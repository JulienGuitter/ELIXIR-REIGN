package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.game.GameState
import com.mjm.elixir_reign.server.logging.ServerLog
import com.mjm.elixir_reign.shared.network.Client
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.type.GameType
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class Instance(
    val uuid: String = UUID.randomUUID().toString(),
    var gameType: GameType = GameType.G1V3,
    var active: Boolean = false
) {
    val players = ConcurrentHashMap<Int, Client>()
    private val playerIdByConnectionId = ConcurrentHashMap<Int, Int>()
    private var gameState: GameState? = null
    private var lastSyncAtMs: Long = 0L

    fun start(gameType: GameType) {
        this.gameType = gameType
        this.active = true
        this.gameState = GameState(gameType)
        this.lastSyncAtMs = 0L
        ServerLog.info("Instance $uuid started for $gameType")
    }

    fun stop() {
        this.active = false
        players.clear()
        playerIdByConnectionId.clear()
        gameState = null
        ServerLog.info("Instance $uuid stopped")
    }

    fun addPlayer(connectionId: Int, client: Client) {
        val state = gameState ?: return
        val reconnectPlayerId = if (state.isStarted()) {
            state.findReconnectablePlayerId(client.pseudo, System.currentTimeMillis())
        } else {
            null
        }

        if (reconnectPlayerId != null) {
            players[reconnectPlayerId] = client
            playerIdByConnectionId[connectionId] = reconnectPlayerId
            state.markPlayerReconnected(reconnectPlayerId)
            state.resetSyncStateForPlayer(reconnectPlayerId)
            client.connection?.let { connection ->
                state.initialPacketsFor(reconnectPlayerId).forEach { packet ->
                    ServerLog.sendTcp(connection, packet)
                }
            }
            return
        }

        if (state.isStarted()) {
            return
        }

        players[connectionId] = client
        playerIdByConnectionId[connectionId] = connectionId
        state.addPlayer(connectionId, client.pseudo)

        if (state.canStart(expectedPlayerCount())) {
            state.markStarted()
            sendInitialGameState(state)
        }
    }

    fun removePlayer(connectionId: Int) {
        val playerId = playerIdByConnectionId.remove(connectionId) ?: return
        val state = gameState ?: return
        val playerClient = players[playerId] ?: return

        if (playerClient.connection?.id != connectionId && playerClient.connection != null) {
            return
        }

        playerClient.connection = null
        state.markPlayerDisconnected(playerId, System.currentTimeMillis())

        if (!state.isStarted()) {
            players.remove(playerId)
            if (players.isEmpty()) {
                stop()
            }
            return
        }

        // Keep the instance running while the reconnect window is open.
    }

    fun containsConnection(connectionId: Int): Boolean {
        return playerIdByConnectionId.containsKey(connectionId)
    }

    fun playerIdForConnection(connectionId: Int): Int? {
        return playerIdByConnectionId[connectionId]
    }

    fun handleMoveRequest(playerId: Int, unitIds: IntArray, targetRow: Int, targetCol: Int) {
        gameState?.handleMoveRequest(playerId, unitIds, targetRow, targetCol)
        lastSyncAtMs = 0L
    }

    fun handlePlaceBuildingRequest(connectionId: Int, playerId: Int, requestId: Int, entityType: EntityType, row: Int, col: Int) {
        val connection = players[playerId]?.connection ?: return
        gameState
            ?.handlePlaceBuildingRequest(playerId, requestId, entityType, row, col)
            ?.forEach { packet -> ServerLog.sendTcp(connection, packet) }
        flushSyncToPlayers(forcePresenceHeartbeat = false)
    }

    fun handleUpgradeBuildingRequest(connectionId: Int, playerId: Int, requestId: Int, buildingId: Int) {
        val connection = players[playerId]?.connection ?: return
        gameState
            ?.handleUpgradeBuildingRequest(playerId, requestId, buildingId)
            ?.forEach { packet -> ServerLog.sendTcp(connection, packet) }
        flushSyncToPlayers(forcePresenceHeartbeat = false)
    }

    fun handleTrainUnitRequest(connectionId: Int, playerId: Int, requestId: Int, buildingId: Int, entityType: EntityType) {
        val connection = players[playerId]?.connection ?: return
        gameState
            ?.handleTrainUnitRequest(playerId, requestId, buildingId, entityType)
            ?.forEach { packet -> ServerLog.sendTcp(connection, packet) }
        flushSyncToPlayers(forcePresenceHeartbeat = false)
    }

    fun update(deltaSeconds: Float) {
        val state = gameState ?: return
        val wasGameOver = state.isGameOver()
        state.update(deltaSeconds)

        if (!wasGameOver && state.isGameOver()) {
            flushSyncToPlayers(forcePresenceHeartbeat = false)
            stop()
            return
        }

        val hasMovement = state.hasMovingUnits()
        val syncInterval = if (hasMovement) ACTIVE_SYNC_INTERVAL_MS else IDLE_HEARTBEAT_INTERVAL_MS

        val now = System.currentTimeMillis()
        if (now - lastSyncAtMs < syncInterval) {
            return
        }

        flushSyncToPlayers(forcePresenceHeartbeat = !hasMovement)
        lastSyncAtMs = now
    }

    private fun flushSyncToPlayers(forcePresenceHeartbeat: Boolean) {
        val state = gameState ?: return
        for ((playerId, client) in players) {
            val connection = client.connection ?: continue
            state.syncPacketsFor(
                playerId = playerId,
                forcePresenceHeartbeat = forcePresenceHeartbeat
            ).forEach { packet ->
                ServerLog.sendTcp(connection, packet)
            }
        }
        lastSyncAtMs = System.currentTimeMillis()
    }

    private fun sendInitialGameState(state: GameState) {
        for ((playerId, client) in players) {
            val connection = client.connection ?: continue
            state.initialPacketsFor(playerId).forEach { packet ->
                ServerLog.sendTcp(connection, packet)
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
        private const val ACTIVE_SYNC_INTERVAL_MS = 100L
        private const val IDLE_HEARTBEAT_INTERVAL_MS = 500L
    }
}
