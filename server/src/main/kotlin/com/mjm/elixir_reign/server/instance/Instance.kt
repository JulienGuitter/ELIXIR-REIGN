package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.shared.network.Client
import type.GameType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Instance(
    val uuid: String = UUID.randomUUID().toString(),
    var gameType: GameType = GameType.G1V3,
    var active: Boolean = false
) {
    val players = ConcurrentHashMap<Int, Client>()

    fun start(gameType: GameType) {
        this.gameType = gameType
        this.active = true
        println("Instance $uuid started for $gameType")
    }

    fun stop() {
        this.active = false
        players.clear()
        println("Instance $uuid stopped")
    }

    fun addPlayer(id: Int, client: Client) {
        players[id] = client
    }

    fun removePlayer(id: Int) {
        players.remove(id)
    }
}
