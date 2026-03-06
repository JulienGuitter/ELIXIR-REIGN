package com.mjm.elixir_reign.server.lobby

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.server.ConfigManager
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
        for(gameType in GameType.entries){
            when(gameType){
                GameType.G1V1 -> break

                GameType.G2V2 -> break

                GameType.G1V3 ->
                    // Get first 4 clients in queu
                    if((gameTypeClients[gameType]?.size ?: 0) >= 4){
                        val clientsInGame = ConcurrentHashMap<Int, Client>()
                        for(i in 0 until 4) {
                            val clientId = gameTypeClients[gameType]?.poll() ?: break
                            clientsInGame[clientId] = clients[clientId] ?: continue
                        }
                        createServerInstance(clientsInGame)
                        println("Starting a new G1V3 game with clients : ${clientsInGame.values.joinToString(", ") { it.pseudo }}")
                    }
            }
        }
    }

    private fun createServerInstance(clientsInGame: ConcurrentHashMap<Int, Client>){
        val server = availableServers.entries.firstOrNull { it.value > 0 } ?: run {
            println("No available server for the game !")
            return
        }
        if (server.key == "this"){
            println("Starting game on this server !")
            // TODO : Create instance and send clients to it
            return
        }

        println("Sending clients to server ${server.key} ...")
        val ip = server.key.split(":")
        if(ip.size != 2) {
            println("Invalid server IP : ${server.key}")
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
        } catch (e: Exception) {
            println("Failed to connect to server ${server.key} !")
        } finally {
            client.stop()
        }
    }

    private fun sendClientsToInstance(clientsInGame: ConcurrentHashMap<Int, Client>, instanceUUID: String, instanceIP: String = "", instancePort: String = ""){
        for(client in clientsInGame.values){
            println("Sending client ${client.pseudo} to instance $instanceUUID ...")

            val packet = PacketRedirectToInstance(
                ip = instanceIP,
                port = instancePort.toIntOrNull() ?: 0,
                uuid = instanceUUID
            )
            client.connection?.sendTCP(packet)
        }
    }

    private fun updateAvailableServers(){
        for(server in config.serversIP){
            val ip = server.split(":")
            if(server == "this"){
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
                    client.stop()
                    continue
                }

                println("Server $server is available !")
            } catch (e: Exception) {
                println("Server $server is not available !")
            } finally {
                client.stop()
            }
        }
    }
}
