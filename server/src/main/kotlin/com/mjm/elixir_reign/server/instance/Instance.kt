package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.game.GameState
import com.mjm.elixir_reign.shared.network.Client
import com.mjm.elixir_reign.shared.type.GameType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Instance(
    val uuid: String = UUID.randomUUID().toString(),
    var gameType: GameType = GameType.G1V3,
    var active: Boolean = false
) {
    val players = ConcurrentHashMap<Int, Client>()
    private var gameState: GameState? = null
    private var lastSyncAtMs: Long = 0L

    fun start(gameType: GameType) {
        this.gameType = gameType
        this.active = true
        this.gameState = GameState(gameType)
        this.lastSyncAtMs = 0L
        println("Instance $uuid started for $gameType")
    }

    fun stop() {
        this.active = false
        players.clear()
        gameState = null
        println("Instance $uuid stopped")
    }

    fun addPlayer(id: Int, client: Client) {
        players[id] = client
        val state = gameState ?: return
        state.addPlayer(id, client.pseudo)

        if (state.canStart(expectedPlayerCount())) {
            state.markStarted()
            sendInitialGameState(state)
        }
    }

    fun removePlayer(id: Int) {
        players.remove(id)
        gameState?.removePlayer(id)
        if (players.isEmpty()) {
            stop()
        }
    }

    fun containsPlayer(id: Int): Boolean {
        return players.containsKey(id)
    }

    fun handleMoveRequest(playerId: Int, unitIds: IntArray, targetRow: Int, targetCol: Int) {
        gameState?.handleMoveRequest(playerId, unitIds, targetRow, targetCol)
    }

    fun update(deltaSeconds: Float) {
        val state = gameState ?: return
        state.update(deltaSeconds)

        val now = System.currentTimeMillis()
        if (now - lastSyncAtMs < SYNC_INTERVAL_MS) {
            return
        }

        for ((playerId, client) in players) {
            val connection = client.connection ?: continue
            state.syncPacketsFor(playerId).forEach { packet ->
                connection.sendTCP(packet)
            }
        }
        lastSyncAtMs = now
    }

    private fun sendInitialGameState(state: GameState) {
        for ((playerId, client) in players) {
            val connection = client.connection ?: continue
            state.initialPacketsFor(playerId).forEach { packet ->
                connection.sendTCP(packet)
            }
        }
    }

    private fun expectedPlayerCount(): Int {
        return when (gameType) {
            GameType.SOLO -> 1
            GameType.G1V1 -> 2
            GameType.G2V2 -> 4
            GameType.G1V3 -> 4
        }
    }

    companion object {
        private const val SYNC_INTERVAL_MS = 100L
    }
}
