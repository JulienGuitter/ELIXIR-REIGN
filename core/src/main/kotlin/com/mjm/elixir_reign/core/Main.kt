package com.mjm.elixir_reign.core

import com.badlogic.gdx.ApplicationAdapter
import com.badlogic.gdx.Gdx
import com.esotericsoftware.kryonet.Client
import com.esotericsoftware.kryonet.Connection
import com.esotericsoftware.kryonet.Listener
import com.mjm.elixir_reign.shared.GameVersion
import com.mjm.elixir_reign.shared.network.*
import type.GameType
import kotlin.concurrent.thread

class Main : ApplicationAdapter() {
    override fun create() {

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
                }
            }
        })

        client.start()
        // IMPORTANT: évite de bloquer le thread principal (render thread)
        thread(name = "kryonet-connect") {
            try {
                Gdx.app.log("NET", "Connecting to 10.0.2.2:${Network.PORT} ...")
                client.connect(5000, "10.0.2.2", Network.PORT, Network.PORT)

                val login = PacketLogin(pseudo = "CompteTest", version = GameVersion.VERSION, gameType = GameType.G1V3)
                client.sendTCP(login)

                Gdx.app.log("NET", "Login packet sent")
            } catch (e: Exception) {
                // Tu verras enfin la vraie raison du crash
                Gdx.app.error("NET", "Connection failed", e)
            }
        }

    }

    override fun resize(width: Int, height: Int) {
    }

    override fun render() {
    }

    override fun pause() {
    }

    override fun resume() {
    }

    override fun dispose() {
    }
}
