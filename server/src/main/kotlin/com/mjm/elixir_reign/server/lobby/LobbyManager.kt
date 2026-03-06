package com.mjm.elixir_reign.server.lobby

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.server.ConfigManager
import com.mjm.elixir_reign.server.instance.InstanceManager
import com.mjm.elixir_reign.shared.network.Client
import com.mjm.elixir_reign.shared.network.Network
import com.mjm.elixir_reign.shared.network.PacketCreateInstance
import com.mjm.elixir_reign.shared.network.PacketRedirectToInstance
import com.mjm.elixir_reign.shared.network.PacketServerInfo
import type.GameType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.esotericsoftware.kryonet.Client as KryoClient

object LobbyManager {
    private var config = ConfigManager.getConfig()

    private val clients = ConcurrentHashMap<Int, Client>()
    private val gameTypeClients = ConcurrentHashMap<GameType, ConcurrentLinkedQueue<Int>>()

    private lateinit var lobbyThread : Thread

    private var availableServers = ConcurrentHashMap<String, Int>()
    private lateinit var availableServersThread : Thread

    // Cooldown quand aucun serveur n'est disponible (évite le spam)
    private var noServerCooldownUntil: Long = 0L
    private const val NO_SERVER_COOLDOWN_MS = 10_000L // 10 secondes

    var isInit = false
        private set

    fun init(){
        for (gameType in GameType.entries){
            gameTypeClients[gameType] = ConcurrentLinkedQueue()
        }

        availableServersThread = Thread {
            while(true){
                updateAvailableServers()
                Thread.sleep(1000L * 60L * 5L) // Check every 5 minute
            }
        }
        availableServersThread.isDaemon = true
        availableServersThread.start()

        // Si ce serveur a aussi le mode instance, l'ajouter comme disponible
        if(config.instance && config.serversIP.contains("this")){
            availableServers["this"] = InstanceManager.getAvailableInstances()
        }

        lobbyThread = Thread {
            while(true){
                update()
                Thread.sleep(1000L / 20L) // 20 ticks per second
            }
        }
        lobbyThread.start()

        isInit = true
    }

    fun addClient(id: Int, client: Client){
        val type = client.gameType
        clients.put(id, client)
        gameTypeClients[type]?.add(id)
    }

    fun removeClient(id: Int){
        val client = clients.remove(id) ?: return
        val type = client.gameType
        gameTypeClients[type]?.remove(id)
    }

    fun update(){
        // Si on est en cooldown, ne rien faire
        if(System.currentTimeMillis() < noServerCooldownUntil) return

        for(gameType in GameType.entries){
            var userNeeded = 0
            when(gameType){
                GameType.G1V1 -> {
                    userNeeded = 2
                }
                GameType.G2V2 -> {
                    userNeeded = 4
                }
                GameType.G1V3 -> {
                    userNeeded = 4
                }
            }


            // Get first clients in queue according to userNeeded
            if((gameTypeClients[gameType]?.size ?: 0) >= userNeeded){
                val clientsInGame = ConcurrentHashMap<Int, Client>()
                while (clientsInGame.size < userNeeded) {
                    val clientId = gameTypeClients[gameType]?.poll() ?: break
                    val client = clients[clientId] ?: continue
                    clientsInGame[clientId] = client
                }
                if(clientsInGame.size == userNeeded){
                    println("Starting a new G1V3 game with clients : ${clientsInGame.values.joinToString(", ") { it.pseudo }}")
                    createServerInstance(clientsInGame)
                }
            }
        }
    }

    private fun createServerInstance(clientsInGame: ConcurrentHashMap<Int, Client>){
        // 1) Essayer d'abord le serveur local ("this")
        val thisCount = availableServers["this"]
        if(thisCount != null && thisCount > 0){
            println("Starting game on this server !")
            val instance = InstanceManager.createInstance(clientsInGame.values.first().gameType)
            if(instance != null){
                // Mettre à jour le compteur
                availableServers["this"] = InstanceManager.getAvailableInstances()

                // Envoyer les clients vers l'instance locale
                sendClientsToInstance(clientsInGame, instance.uuid, "this", config.port.toString())
                return
            } else {
                println("No available instance on this server !")
                availableServers["this"] = 0
            }
        }

        // 2) Essayer les serveurs externes
        val server = availableServers.entries.firstOrNull { it.key != "this" && it.value > 0 }
        if(server == null){
            println("No available server for the game ! Retry in ${NO_SERVER_COOLDOWN_MS / 1000}s...")
            noServerCooldownUntil = System.currentTimeMillis() + NO_SERVER_COOLDOWN_MS
            // Remettre les clients dans la queue
            for((id, client) in clientsInGame){
                gameTypeClients[client.gameType]?.add(id)
            }
            return
        }

        println("Sending clients to server ${server.key} ...")
        val ip = server.key.split(":")
        if(ip.size != 2) {
            println("Invalid server IP : ${server.key}")
            // Re-queue clients to avoid dropping them from matchmaking
            for((id, client) in clientsInGame){
                gameTypeClients[client.gameType]?.add(id)
            }
            // Apply a cooldown similar to the no-available-server case
            noServerCooldownUntil = System.currentTimeMillis() + NO_SERVER_COOLDOWN_MS
            return
        }

        val client = KryoClient()
        Network.register(client.kryo)
        var latch = CountDownLatch(1)

        // Connect to server and send create instance request
        try {
            client.start()

            client.addListener(object : Listener {
                override fun received(connection: Connection, message: Any) {
                    when (message) {
                        is PacketCreateInstance -> {
                            println("Instance created with UUID : ${message.uuid}")
                            connection.close()
                            latch.countDown()

                            // Send clients to instance
                            sendClientsToInstance(clientsInGame, message.uuid, ip[0], ip[1])
                        }
                    }
                }

                override fun disconnected(connection: Connection) {
                    println("Déconnecté du serveur")
                }
            })

            client.connect(5000, ip[0], ip[1].toInt(), ip[1].toInt())

            val request = PacketCreateInstance(gameType = clientsInGame.values.first().gameType)
            client.sendTCP(request)

            val ok = latch.await(10, TimeUnit.SECONDS)
            if(!ok){
                println("Server ${server.key} did not respond to create instance in time !")
                // Re-queue clients so they are not lost if the instance server does not respond.
                requeueClientsInGame(clientsInGame)
                // Optionally refresh available servers to avoid using an unresponsive one.
                updateAvailableServers()
            }
        } catch (e: Exception) {
            println("Failed to connect to server ${server.key} : ${e.message}")
            // Re-queue clients so they are not lost if contacting the instance server fails.
            requeueClientsInGame(clientsInGame)
            // Optionally refresh available servers to avoid using an unresponsive one.
            updateAvailableServers()
        } finally {
            client.stop()
        }
    }

    private fun requeueClientsInGame(clientsInGame: ConcurrentHashMap<Int, Client>) {
        if (clientsInGame.isEmpty()) {
            return
        }

        // All clients in this map should share the same game type.
        val gameType = clientsInGame.values.first().gameType

        // Ensure there is a queue for this game type and re-add clients to it.
        val queue = gameTypeClients.computeIfAbsent(gameType) { ConcurrentLinkedQueue() }
        for (client in clientsInGame.values) {
            queue.add(client)
        }
    }
    private fun sendClientsToInstance(clientsInGame: ConcurrentHashMap<Int, Client>, instanceUUID: String, instanceIP: String = "", instancePort: String = ""){
        for((id, client) in clientsInGame){
            println("Sending client ${client.pseudo} to instance $instanceUUID ...")

            val packet = PacketRedirectToInstance(
                ip = instanceIP,
                port = instancePort.toIntOrNull() ?: 0,
                uuid = instanceUUID
            )
            client.connection?.sendTCP(packet)

            // Retirer le client du lobby (il est maintenant dans une instance)
            clients.remove(id)
        }
    }

    private fun updateAvailableServers(){
        for(server in config.serversIP){
            val ip = server.split(":")
            if(server == "this"){
                // Rafraîchir le compteur local
                if(config.instance && InstanceManager.isInit){
                    availableServers["this"] = InstanceManager.getAvailableInstances()
                }
                continue
            }

            if(ip.size != 2){
                println("Invalid server IP : $server")
                continue
            }

            val client = KryoClient()
            Network.register(client.kryo)

            var latch = CountDownLatch(1)

            try {
                client.start()

                client.addListener(object : Listener {
                    override fun received(connection: Connection, message: Any) {
                        when (message) {
                            is PacketServerInfo -> {
                                println("[${server}] instance available : ${message.disponibilityCount}")
                                if(message.disponibilityCount > 0){
                                    availableServers[server] = message.disponibilityCount
                                } else {
                                    availableServers.remove(server)
                                }

                                // On ferme dès qu'on a la réponse
                                connection.close()
                                latch.countDown()
                            }
                        }
                    }

                    override fun disconnected(connection: Connection) {
                        println("Déconnecté du serveur")
                    }
                })

                client.connect(5000, ip[0], ip[1].toInt(), ip[1].toInt())

                val request = PacketServerInfo()
                client.sendTCP(request)

                val ok = latch.await(5, TimeUnit.SECONDS)
                if(!ok){
                    println("Server $server did not respond in time !")
                    availableServers.remove(server)
                    client.stop()
                    continue
                }

                println("Server $server is available !")
            } catch (e: Exception) {
                println("Server $server is not available !")
                availableServers.remove(server)
            } finally {
                client.stop()
            }
        }
    }
}
