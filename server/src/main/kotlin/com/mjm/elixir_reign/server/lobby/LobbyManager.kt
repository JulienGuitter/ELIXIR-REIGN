package com.mjm.elixir_reign.server.lobby

import com.mjm.elixir_reign.server.ConfigManager
import com.mjm.elixir_reign.shared.network.Client
import java.util.concurrent.ConcurrentHashMap

object LobbyManager {
    private var config = ConfigManager.getConfig()

    private val clients = ConcurrentHashMap<Int, Client>()

    fun init(){

    }

    fun addClient(id: Int, client: Client){

    }
}
