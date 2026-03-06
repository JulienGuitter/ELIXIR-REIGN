package com.mjm.elixir_reign.server

import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.esotericsoftware.kryonet.Server
import com.mjm.elixir_reign.server.instance.InstanceManager
import com.mjm.elixir_reign.server.lobby.LobbyManager
import com.mjm.elixir_reign.shared.GameVersion
import com.mjm.elixir_reign.shared.network.*
import java.util.concurrent.ConcurrentHashMap

fun main(args: Array<String>) {
    val launcher = ServerLauncher()
    launcher.start()
}

class ServerLauncher {

    fun start() {
        var config = ConfigManager.getConfig()

        if(config.lobby){
            LobbyManager.init()
        }
        if(config.instance){
            InstanceManager.init()
        }

        val server = Server()
        Network.register(server.kryo)

        val clients = ConcurrentHashMap<Int, Client>()

        server.addListener(object : Listener {
            override fun received(connection: Connection, message : Any) {
                when(message){
                    is PacketLogin -> {
                        println("Le client ${message.pseudo} vient de se connecter !")

                        if(message.version != GameVersion.VERSION){
                            println("Le client ${message.pseudo} n'a pas la bonne version !")
                            connection.sendTCP(PacketLoginRefused("Version incompatible ! Requis : ${GameVersion.VERSION}, CLient : ${message.version}"))
                            connection.close()
                            return
                        }

                        var newClient = Client(pseudo = message.pseudo, gameType = message.gameType)
                        clients[connection.id] = newClient

                        var accepted = PacketLoginAccepted(
                            myId = connection.id
                        )
                        connection.sendTCP(accepted)
                    }

                    is PacketServerInfo -> {
                        var disponibility = InstanceManager.getAvailableInstances()
                        connection.sendTCP(PacketServerInfo(disponibility))
                    }

                    // Define all packets.
                }
            }

            override fun disconnected(connection: Connection?) {
                println("Le client ${clients[connection?.id]?.pseudo} vient de se deconnecter !")
                clients.remove(connection?.id)
            }
        })

        server.start()
        server.bind(config.port, config.port)
        println("Serveur started sur le port ${config.port}")

        Thread {
            while(true){
                // Add serveur game loop

                Thread.sleep(50)
            }
        }.start()
    }
}
