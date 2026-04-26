package com.mjm.elixir_reign.shared.network

import com.esotericsoftware.kryo.Kryo
import com.mjm.elixir_reign.shared.type.GameType

object Network {
    // Le port qu'on utilisera (TCP et UDP)
    const val PORT: Int = 54555

    // La méthode pour tout enregistrer
    fun register(kryo: Kryo?) {

        // Common types
        kryo?.register(GameType::class.java)

        // Lobby/Login packets register
        kryo?.register(PacketLogin::class.java)
        kryo?.register(PacketLoginAccepted::class.java)
        kryo?.register(PacketLoginRefused::class.java)
        kryo?.register(PacketServerInfo::class.java)
        kryo?.register(PacketCreateInstance::class.java)
        kryo?.register(PacketRedirectToInstance::class.java)
        kryo?.register(PacketConnectToInstance::class.java)
        kryo?.register(PacketGameplayTick::class.java)
    }
}
