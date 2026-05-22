package com.mjm.elixir_reign.server.lobby

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.server.ConfigManager
import com.mjm.elixir_reign.server.instance.InstanceManager
import com.mjm.elixir_reign.server.logging.ServerLog
import com.mjm.elixir_reign.shared.network.Client
import com.mjm.elixir_reign.shared.network.Network
import com.mjm.elixir_reign.shared.network.PacketCreateInstance
import com.mjm.elixir_reign.shared.network.PacketLoginRefused
import com.mjm.elixir_reign.shared.network.PacketRedirectToInstance
import com.mjm.elixir_reign.shared.network.PacketServerInfo
import com.mjm.elixir_reign.shared.type.GameType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import com.esotericsoftware.kryonet.Client as KryoClient

object LobbyManager {
    private var config = ConfigManager.getConfig()

    private val clients = ConcurrentHashMap<Int, Client>()
    private val gameTypeClients = ConcurrentHashMap<GameType, ConcurrentLinkedQueue<Int>>()
    private val pendingRedirects = ConcurrentHashMap<Int, PendingRedirect>()

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
        pendingRedirects.remove(id)
        val client = clients.remove(id) ?: return
        val type = client.gameType
        gameTypeClients[type]?.remove(id)
    }

    fun acknowledgeRedirect(id: Int, uuid: String) {
        val pendingRedirect = pendingRedirects[id] ?: return
        if (pendingRedirect.packet.uuid != uuid) return

        pendingRedirects.remove(id)
        clients.remove(id)
        ServerLog.info("Client $id acknowledged redirect to instance $uuid")
    }

    fun update(){
        processPendingRedirects()

        // Si on est en cooldown, ne rien faire
        if(System.currentTimeMillis() < noServerCooldownUntil) return

        for(gameType in GameType.entries){
            val userNeeded = when(gameType){
                GameType.SOLO -> 1
                GameType.G1V1 -> 2
                GameType.G2V2 -> 4
                GameType.G1V3 -> 4
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
                    ServerLog.info("Starting a new $gameType game with clients: ${clientsInGame.values.joinToString(", ") { it.pseudo }}")
                    createServerInstance(clientsInGame)
                }
            }
        }
    }

    private fun createServerInstance(clientsInGame: ConcurrentHashMap<Int, Client>){
        // 1) Essayer d'abord le serveur local ("this")
        val thisCount = availableServers["this"]
        if(thisCount != null && thisCount > 0){
            ServerLog.info("Starting game on this server !")
            val instance = InstanceManager.createInstance(clientsInGame.values.first().gameType)
            if(instance != null){
                // Mettre à jour le compteur
                availableServers["this"] = InstanceManager.getAvailableInstances()

                // Envoyer les clients vers l'instance locale
                sendClientsToInstance(clientsInGame, instance.uuid, "this", config.port.toString())
                return
            } else {
                ServerLog.info("No available instance on this server !")
                availableServers["this"] = 0
            }
        }

        // 2) Essayer les serveurs externes
        val server = availableServers.entries.firstOrNull { it.key != "this" && it.value > 0 }
        if(server == null){
            ServerLog.info("No available server for the game !")
            notifyMatchFailure(clientsInGame, "Aucun serveur disponible pour lancer la partie.")
            noServerCooldownUntil = System.currentTimeMillis() + NO_SERVER_COOLDOWN_MS
            return
        }

        ServerLog.info("Sending clients to server ${server.key} ...")
        val ip = server.key.split(":")
        if(ip.size != 2) {
            ServerLog.info("Invalid server IP : ${server.key}")
            notifyMatchFailure(clientsInGame, "Serveur de partie invalide.")
            noServerCooldownUntil = System.currentTimeMillis() + NO_SERVER_COOLDOWN_MS
            return
        }

        val client = KryoClient(Network.WRITE_BUFFER_SIZE, Network.OBJECT_BUFFER_SIZE)
        Network.register(client.kryo)
        var latch = CountDownLatch(1)

        // Connect to server and send create instance request
        try {
            client.start()

            client.addListener(object : Listener {
                override fun received(connection: Connection, message: Any) {
                    ServerLog.inbound(connection.id, message)
                    when (message) {
                        is PacketCreateInstance -> {
                            ServerLog.info("Instance created with UUID : ${message.uuid}")
                            connection.close()
                            latch.countDown()

                            // Only send clients to instance if the UUID is not blank.
                            if (message.uuid.isBlank()) {
                                ServerLog.info("Failed to create instance: received blank UUID from server, not redirecting clients.")
                                notifyMatchFailure(clientsInGame, "Le serveur est plein. Reessayez dans quelques instants.")
                            } else {
                                // Send clients to instance
                                sendClientsToInstance(clientsInGame, message.uuid, ip[0], ip[1])
                            }
                        }
                    }
                }

                override fun disconnected(connection: Connection) {
                    ServerLog.info("Deconnecte du serveur")
                }
            })

            client.connect(5000, ip[0], ip[1].toInt(), ip[1].toInt())

            val request = PacketCreateInstance(gameType = clientsInGame.values.first().gameType)
            ServerLog.outbound(null, request)
            client.sendTCP(request)

            val ok = latch.await(10, TimeUnit.SECONDS)
            if(!ok){
                ServerLog.info("Server ${server.key} did not respond to create instance in time !")
                notifyMatchFailure(clientsInGame, "Le serveur de partie ne repond pas.")
                updateAvailableServers()
            }
        } catch (e: Exception) {
            ServerLog.info("Failed to connect to server ${server.key} : ${e.message}")
            notifyMatchFailure(clientsInGame, "Impossible de contacter le serveur de partie.")
            updateAvailableServers()
        } finally {
            client.stop()
        }
    }

    private fun notifyMatchFailure(clientsInGame: ConcurrentHashMap<Int, Client>, reason: String) {
        for ((id, client) in clientsInGame) {
            ServerLog.sendTcp(client.connection, PacketLoginRefused(reason))
            clients.remove(id)
        }
    }

    private fun sendClientsToInstance(clientsInGame: ConcurrentHashMap<Int, Client>, instanceUUID: String, instanceIP: String = "", instancePort: String = ""){
        for((id, client) in clientsInGame){
            ServerLog.info("Sending client ${client.pseudo} to instance $instanceUUID ...")

            val packet = PacketRedirectToInstance(
                ip = instanceIP,
                port = instancePort.toIntOrNull() ?: 0,
                uuid = instanceUUID
            )
            val pendingRedirect = PendingRedirect(packet = packet)
            pendingRedirects[id] = pendingRedirect
            sendPendingRedirect(id, client, pendingRedirect, force = true)
        }
    }

    private fun processPendingRedirects() {
        for ((id, pendingRedirect) in pendingRedirects) {
            val client = clients[id]
            if (client == null) {
                pendingRedirects.remove(id)
                continue
            }

            if (pendingRedirect.attempts >= REDIRECT_MAX_ATTEMPTS) {
                ServerLog.info("Client ${client.pseudo} did not acknowledge redirect to ${pendingRedirect.packet.uuid}")
                ServerLog.sendTcp(client.connection, PacketLoginRefused("Impossible de rejoindre la partie. Reessayez."))
                pendingRedirects.remove(id)
                clients.remove(id)
                client.connection?.close()
                continue
            }

            sendPendingRedirect(id, client, pendingRedirect, force = false)
        }
    }

    private fun sendPendingRedirect(id: Int, client: Client, pendingRedirect: PendingRedirect, force: Boolean) {
        val now = System.currentTimeMillis()
        if (!force && now - pendingRedirect.lastSentAtMs < REDIRECT_RETRY_INTERVAL_MS) {
            return
        }

        pendingRedirect.attempts++
        pendingRedirect.lastSentAtMs = now
        ServerLog.info("Sending redirect attempt ${pendingRedirect.attempts} to client ${client.pseudo} ($id)")
        ServerLog.sendTcp(client.connection, pendingRedirect.packet)
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
                ServerLog.info("Invalid server IP : $server")
                continue
            }

            val client = KryoClient(Network.WRITE_BUFFER_SIZE, Network.OBJECT_BUFFER_SIZE)
            Network.register(client.kryo)

            var latch = CountDownLatch(1)

            try {
                client.start()

                client.addListener(object : Listener {
                    override fun received(connection: Connection, message: Any) {
                        ServerLog.inbound(connection.id, message)
                        when (message) {
                            is PacketServerInfo -> {
                                ServerLog.info("[${server}] instance available : ${message.disponibilityCount}")
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
                        ServerLog.info("Deconnecte du serveur")
                    }
                })

                client.connect(5000, ip[0], ip[1].toInt(), ip[1].toInt())

                val request = PacketServerInfo()
                ServerLog.outbound(null, request)
                client.sendTCP(request)

                val ok = latch.await(5, TimeUnit.SECONDS)
                if(!ok){
                    ServerLog.info("Server $server did not respond in time !")
                    availableServers.remove(server)
                    client.stop()
                    continue
                }

                ServerLog.info("Server $server is available !")
            } catch (e: Exception) {
                ServerLog.info("Server $server is not available !")
                availableServers.remove(server)
            } finally {
                client.stop()
            }
        }
    }

    private data class PendingRedirect(
        val packet: PacketRedirectToInstance,
        var attempts: Int = 0,
        var lastSentAtMs: Long = 0L
    )

    private const val REDIRECT_RETRY_INTERVAL_MS = 1000L
    private const val REDIRECT_MAX_ATTEMPTS = 5
}
