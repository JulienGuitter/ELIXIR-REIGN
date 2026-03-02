package com.mjm.elixir_reign.shared.network

import com.esotericsoftware.kryo.Kryo

object Network {
    // Le port qu'on utilisera (TCP et UDP)
    const val PORT: Int = 54555

    // La méthode pour tout enregistrer
    fun register(kryo: Kryo?) {

        // Login packets register
        kryo?.register(PacketLogin::class.java)
        kryo?.register(PacketLoginAccepted::class.java)
        kryo?.register(PacketLoginRefused::class.java)
    }
}
