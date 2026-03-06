package com.mjm.elixir_reign.core.network

import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.shared.GameVersion
import com.mjm.elixir_reign.shared.network.Network
import com.mjm.elixir_reign.shared.network.PacketConnectToInstance
import com.mjm.elixir_reign.shared.network.PacketLogin
import com.mjm.elixir_reign.shared.network.PacketLoginAccepted
import com.mjm.elixir_reign.shared.network.PacketLoginRefused
import com.mjm.elixir_reign.shared.network.PacketRedirectToInstance
import type.GameType
import kotlin.concurrent.thread

class temporary {

    fun testConnection(){
        //Test network connection
        val client = Client()
        Network.register(client.kryo)

        client.addListener(object : Listener {
            override fun received(connection: Connection, message: Any) {
                when(message){
                    is PacketLoginAccepted -> {
                        println("Connected avec succes ! Mon ID reseau est : ${message.myId}")
                    }
                    is PacketLoginRefused -> {
                        println("Connection refuser : ${message.reason}")
                    }
                    is PacketRedirectToInstance -> {
                        Gdx.app.log("NET", "Redirection vers instance ${message.uuid} à ${message.ip}:${message.port}")

                        if(message.ip == "this"){
                            // L'instance est sur le même serveur, réutiliser la connexion existante
                            Gdx.app.log("NET", "Instance locale, réutilisation de la connexion lobby")
                            connection.sendTCP(PacketConnectToInstance(uuid = message.uuid))
                            Gdx.app.log("NET", "Connecté à l'instance ${message.uuid}")
                        } else {
                            // Serveur distant : déconnecter du lobby et se reconnecter à l'instance
                            Gdx.app.log("NET", "Déconnexion du lobby...")
                            client.stop()

                            thread(name = "kryonet-instance") {
                                try {
                                    val instanceClient = Client()
                                    Network.register(instanceClient.kryo)

                                    instanceClient.addListener(object : Listener {
                                        override fun received(conn: Connection, msg: Any) {
                                            // TODO: handle instance game packets
                                            Gdx.app.log("NET", "Instance message reçu: $msg")
                                        }

                                        override fun disconnected(conn: Connection) {
                                            Gdx.app.log("NET", "Déconnecté de l'instance")
                                        }
                                    })

                                    instanceClient.start()
                                    instanceClient.connect(5000, message.ip, message.port, message.port)

                                    instanceClient.sendTCP(PacketConnectToInstance(uuid = message.uuid))
                                    Gdx.app.log("NET", "Connecté à l'instance ${message.uuid}")
                                } catch (e: Exception) {
                                    Gdx.app.error("NET", "Connexion à l'instance échouée", e)
                                }
                            }
                        }
                    }
                }
            }
        })

        client.start()
        // IMPORTANT: évite de bloquer le thread principal (render thread)
        thread(name = "kryonet-connect") {
            try {
                Gdx.app.log("NET", "Connecting to 10.0.2.2:${Network.PORT} ...")
                client.connect(5000, "10.0.2.2", Network.PORT, Network.PORT)

                val login = PacketLogin(pseudo = "CompteTest", version = GameVersion.VERSION, gameType = GameType.G1V1)
                client.sendTCP(login)

                Gdx.app.log("NET", "Login packet sent")
            } catch (e: Exception) {
                // Tu verras enfin la vraie raison du crash
                Gdx.app.error("NET", "Connection failed", e)
            }
        }
    }
}
