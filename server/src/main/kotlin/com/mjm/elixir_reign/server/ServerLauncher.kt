package com.mjm.elixir_reign.server

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.mjm.elixir_reign.server.instance.InstanceManager
import com.mjm.elixir_reign.server.lobby.LobbyManager
import com.mjm.elixir_reign.server.logging.ServerLog
import com.mjm.elixir_reign.shared.GameConfiguration
import com.mjm.elixir_reign.shared.network.*
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>) {
    val launcher = ServerLauncher()
    launcher.start()
}

class ServerLauncher {

    fun start() {
        val config = ConfigManager.getConfig()
        ServerLog.configure(config)

        if(config.instance){
            InstanceManager.init()
        }
        if(config.lobby){
            LobbyManager.init()
        }

        val server = Server(Network.WRITE_BUFFER_SIZE, Network.OBJECT_BUFFER_SIZE)
        Network.register(server.kryo)

        val clients = ConcurrentHashMap<Int, Client>()

        server.addListener(object : Listener {
            override fun received(connection: Connection, message : Any) {
                ServerLog.inbound(connection.id, message)
                when(message){
                    is PacketLogin -> {
                        ServerLog.info("Le client ${message.pseudo} vient de se connecter !")

                        if(message.version != GameConfiguration.VERSION){
                            ServerLog.info("Le client ${message.pseudo} n'a pas la bonne version !")
                            ServerLog.sendTcp(connection, PacketLoginRefused("Version incompatible ! Requis : ${GameConfiguration.VERSION}, Client : ${message.version}"))
                            connection.close()
                            return
                        }

                        val requestedInstanceUuid = message.instanceUuid.trim()
                        val targetInstance = if (requestedInstanceUuid.isNotBlank()) {
                            if (!config.instance || !InstanceManager.isInit) {
                                ServerLog.sendTcp(connection, PacketLoginRefused("Serveur d'instance indisponible."))
                                connection.close()
                                return
                            }
                            val foundInstance = InstanceManager.findByUUID(requestedInstanceUuid)
                            if (foundInstance == null) {
                                ServerLog.info("Instance $requestedInstanceUuid non trouvee !")
                                ServerLog.sendTcp(connection, PacketLoginRefused("Instance invalide ou expiree."))
                                connection.close()
                                return
                            }
                            foundInstance
                        } else {
                            null
                        }

                        val newClient = Client(pseudo = message.pseudo, gameType = message.gameType, connection = connection)
                        clients[connection.id] = newClient

                        val accepted = PacketLoginAccepted(
                            myId = connection.id
                        )
                        ServerLog.sendTcp(connection, accepted)

                        if(targetInstance != null){
                            targetInstance.addPlayer(connection.id, newClient)
                            ServerLog.info("Client ${message.pseudo} connecte a l'instance $requestedInstanceUuid")
                        }

                        // Ajouter le client au lobby s'il est actif
                        if(message.instanceUuid.isBlank() && config.lobby && LobbyManager.isInit){
                            LobbyManager.addClient(connection.id, newClient)
                            ServerLog.info("Client ${message.pseudo} ajouté au lobby (${message.gameType})")
                        }
                    }

                    is PacketServerInfo -> {
                        val disponibility = InstanceManager.getAvailableInstances()
                        ServerLog.sendTcp(connection, PacketServerInfo(disponibility))
                    }

                    is PacketCreateInstance -> {
                        // Un autre serveur (lobby) demande de créer une instance
                        if(config.instance && InstanceManager.isInit){
                            val instance = InstanceManager.createInstance(message.gameType)
                            if(instance != null){
                                ServerLog.info("Instance créée : ${instance.uuid} pour ${message.gameType}")
                                ServerLog.sendTcp(connection, PacketCreateInstance(gameType = message.gameType, uuid = instance.uuid))
                            } else {
                                ServerLog.info("Pas d'instance disponible !")
                                ServerLog.sendTcp(connection, PacketCreateInstance(gameType = message.gameType, uuid = ""))
                            }
                        }
                    }

                    is PacketConnectToInstance -> {
                        // Un client veut se connecter à une instance de jeu
                        if(config.instance && InstanceManager.isInit){
                            val instance = InstanceManager.findByUUID(message.uuid)
                            if(instance != null){
                                val client = clients[connection.id]
                                if(client != null && !instance.containsConnection(connection.id)){
                                    instance.addPlayer(connection.id, client)
                                    ServerLog.info("Client ${client.pseudo} connecté à l'instance ${message.uuid}")
                                }
                            } else {
                                ServerLog.info("Instance ${message.uuid} non trouvée !")
                            }
                        }
                    }

                    is PacketGameplayTick -> {
                        // Placeholder for authoritative multiplayer simulation hooks.
                    }

                    is PacketMoveUnitsRequest -> {
                        if(config.instance && InstanceManager.isInit){
                            InstanceManager.handleMoveRequest(
                                playerId = connection.id,
                                unitIds = message.unitIds,
                                targetRow = message.targetRow,
                                targetCol = message.targetCol
                            )
                        }
                    }
                }
            }

            override fun disconnected(connection: Connection?) {
                ServerLog.info("Le client ${clients[connection?.id]?.pseudo} vient de se deconnecter !")
                val id = connection?.id ?: return
                clients.remove(id)

                // Retirer le client du lobby
                if(config.lobby && LobbyManager.isInit){
                    LobbyManager.removeClient(id)
                }
                if(config.instance && InstanceManager.isInit){
                    InstanceManager.removePlayer(id)
                }
            }
        })

        server.start()
        server.bind(config.port, config.port)
        ServerLog.info("Serveur started sur le port ${config.port}")

        Thread {
            var lastTickAt = System.nanoTime()
            while(true){
                val now = System.nanoTime()
                val deltaSeconds = (now - lastTickAt) / 1_000_000_000f
                lastTickAt = now

                if(config.instance && InstanceManager.isInit){
                    InstanceManager.update(deltaSeconds)
                }

                Thread.sleep(50)
            }
        }.start()
    }
}
