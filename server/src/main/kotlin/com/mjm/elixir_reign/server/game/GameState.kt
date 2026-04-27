package com.mjm.elixir_reign.server.game

import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.logic.UnitType
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketPlayerStatus
import com.mjm.elixir_reign.shared.network.PacketPlayerSummary
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
import com.mjm.elixir_reign.shared.network.PlayerConnectionState
import com.mjm.elixir_reign.shared.type.GameType
import com.mjm.elixir_reign.shared.world.ChunkCoord
import com.mjm.elixir_reign.shared.world.MapGenerator
import com.mjm.elixir_reign.shared.world.WorldChunk
import com.mjm.elixir_reign.shared.world.WorldMap
import kotlin.math.floor
import kotlin.math.sqrt

class GameState(
    private val gameType: GameType
) {
    private val worldMap: WorldMap = MapGenerator.generateGameMap()
    private val players = linkedMapOf<Int, PlayerState>()
    private val sentChunksByPlayer = mutableMapOf<Int, MutableSet<ChunkCoord>>()
    private val visibleUnitsByPlayer = mutableMapOf<Int, MutableSet<Int>>()
    private val visibleTilesByPlayer = mutableMapOf<Int, MutableSet<Int>>()
    private val lastPresenceSentByPlayer = mutableMapOf<Int, Map<Int, PlayerConnectionState>>()
    private val lastMovingStateSentByPlayer = mutableMapOf<Int, MutableMap<Int, Boolean>>()
    private val reconnectDeadlineByPlayer = mutableMapOf<Int, Long>()
    private val connectionStateByPlayer = mutableMapOf<Int, PlayerConnectionState>()
    private var nextUnitId = 1
    private var started = false

    fun addPlayer(playerId: Int, name: String) {
        if (players.containsKey(playerId)) return

        val player = PlayerState(id = playerId, name = name)
        player.units += createStartingUnit(player, offset = 0)
        player.units += createStartingUnit(player, offset = 1)
        players[playerId] = player
        connectionStateByPlayer[playerId] = PlayerConnectionState.CONNECTED
        reconnectDeadlineByPlayer.remove(playerId)
        sentChunksByPlayer[playerId] = mutableSetOf()
        visibleUnitsByPlayer[playerId] = mutableSetOf()
        visibleTilesByPlayer[playerId] = mutableSetOf()
    }

    fun removePlayer(playerId: Int) {
        players.remove(playerId)
        connectionStateByPlayer.remove(playerId)
        reconnectDeadlineByPlayer.remove(playerId)
        sentChunksByPlayer.remove(playerId)
        visibleUnitsByPlayer.remove(playerId)
        visibleTilesByPlayer.remove(playerId)
        lastPresenceSentByPlayer.remove(playerId)
        lastMovingStateSentByPlayer.remove(playerId)
    }

    fun isStarted(): Boolean {
        return started
    }

    fun findReconnectablePlayerId(name: String, nowMs: Long): Int? {
        expireReconnectWindows(nowMs)
        return players.values
            .firstOrNull { player ->
                player.name == name && connectionStateByPlayer[player.id] == PlayerConnectionState.WAITING_RECONNECTION
            }
            ?.id
    }

    fun markPlayerDisconnected(playerId: Int, nowMs: Long) {
        if (!players.containsKey(playerId)) return
        if (!started) {
            removePlayer(playerId)
            return
        }

        connectionStateByPlayer[playerId] = PlayerConnectionState.WAITING_RECONNECTION
        reconnectDeadlineByPlayer[playerId] = nowMs + RECONNECT_GRACE_PERIOD_MS
    }

    fun markPlayerReconnected(playerId: Int) {
        if (!players.containsKey(playerId)) return
        connectionStateByPlayer[playerId] = PlayerConnectionState.CONNECTED
        reconnectDeadlineByPlayer.remove(playerId)
    }

    fun resetSyncStateForPlayer(playerId: Int) {
        sentChunksByPlayer[playerId] = mutableSetOf()
        visibleUnitsByPlayer[playerId] = mutableSetOf()
        visibleTilesByPlayer[playerId] = mutableSetOf()
        lastPresenceSentByPlayer.remove(playerId)
        lastMovingStateSentByPlayer.remove(playerId)
    }

    fun hasMovingUnits(): Boolean {
        return players.values.any { player -> player.units.any { it.moving } }
    }

    fun hasPlayer(playerId: Int): Boolean {
        return players.containsKey(playerId)
    }

    fun canStart(expectedPlayers: Int): Boolean {
        return !started && players.size >= expectedPlayers
    }

    fun markStarted() {
        started = true
    }

    fun handleMoveRequest(playerId: Int, unitIds: IntArray, targetRow: Int, targetCol: Int) {
        val player = players[playerId] ?: return
        val clampedTargetRow = targetRow.coerceIn(0, worldMap.height - 1).toFloat()
        val clampedTargetCol = targetCol.coerceIn(0, worldMap.width - 1).toFloat()
        val requestedIds = unitIds.toSet()

        player.units
            .filter { it.id in requestedIds }
            .forEach { unit ->
                unit.targetRow = clampedTargetRow
                unit.targetCol = clampedTargetCol
                unit.moving = true
            }
    }

    fun update(deltaSeconds: Float) {
        expireReconnectWindows(System.currentTimeMillis())
        if (!started || deltaSeconds <= 0f) return

        players.values
            .flatMap { it.units }
            .forEach { updateUnitMovement(it, deltaSeconds) }
    }

    fun initialPacketsFor(playerId: Int): List<Any> {
        val packets = mutableListOf<Any>()
        packets += PacketGameInit(
            myPlayerId = playerId,
            mapWidth = worldMap.width,
            mapHeight = worldMap.height,
            chunkSize = worldMap.chunkSize,
            players = ArrayList(players.values.map { summary ->
                PacketPlayerSummary(
                    id = summary.id,
                    name = summary.name,
                    gold = summary.gold,
                    elixir = summary.elixir,
                    darkElixir = summary.darkElixir,
                    connectionState = connectionStateByPlayer[summary.id] ?: PlayerConnectionState.CONNECTED
                )
            })
        )
        packets.addAll(syncPacketsFor(playerId))
        packets += PacketGameReady()
        return packets
    }

    fun syncPacketsFor(playerId: Int, forcePresenceHeartbeat: Boolean = false): List<Any> {
        val player = players[playerId] ?: return emptyList()
        val visibleTiles = computeVisibleTileIndices(player)
        val visibleChunks = computeVisibleChunks(visibleTiles)
        val packets = mutableListOf<Any>()

        val sentChunks = sentChunksByPlayer.getOrPut(playerId) { mutableSetOf() }
        visibleChunks
            .filter { it !in sentChunks }
            .sortedWith(compareBy<ChunkCoord> { it.y }.thenBy { it.x })
            .mapNotNull { worldMap.chunkAt(it) }
            .forEach { chunk ->
                packets += chunk.toPacket()
                sentChunks += chunk.coord
            }

        val previouslyVisibleTiles = visibleTilesByPlayer.getOrPut(playerId) { mutableSetOf() }
        val fullSync = previouslyVisibleTiles.isEmpty()
        val addedVisibleTiles = if (fullSync) {
            visibleTiles
        } else {
            visibleTiles.filterTo(linkedSetOf()) { it !in previouslyVisibleTiles }
        }
        val hiddenTiles = if (fullSync) {
            emptySet()
        } else {
            previouslyVisibleTiles.filterTo(linkedSetOf()) { it !in visibleTiles }
        }

        if (fullSync || addedVisibleTiles.isNotEmpty() || hiddenTiles.isNotEmpty()) {
            packets += PacketVisibilityUpdate(
                fullSync = fullSync,
                visibleChunkIndices = visibleChunks
                    .map { chunk -> chunk.y * chunkColumns() + chunk.x }
                    .sorted()
                    .toIntArray(),
                visibleTileIndices = addedVisibleTiles.sorted().toIntArray(),
                hiddenTileIndices = hiddenTiles.sorted().toIntArray()
            )
        }

        val presenceSnapshot = players.values
            .associate { state -> state.id to (connectionStateByPlayer[state.id] ?: PlayerConnectionState.CONNECTED) }
        val lastPresenceSnapshot = lastPresenceSentByPlayer[playerId]
        val shouldSendPresence = forcePresenceHeartbeat || lastPresenceSnapshot != presenceSnapshot
        if (shouldSendPresence) {
            packets += PacketPlayerPresenceUpdate(
                players = ArrayList(
                    players.values
                        .sortedBy { it.id }
                        .map { state ->
                            PacketPlayerStatus(
                                id = state.id,
                                connectionState = connectionStateByPlayer[state.id] ?: PlayerConnectionState.CONNECTED
                            )
                        }
                )
            )
            lastPresenceSentByPlayer[playerId] = presenceSnapshot
        }

        val currentlyVisibleUnits = players.values
            .flatMap { it.units }
            .filter { unit ->
                unit.tileIndex() in visibleTiles
            }
            .map { it.id }
            .toMutableSet()

        val previouslyVisibleUnits = visibleUnitsByPlayer.getOrPut(playerId) { mutableSetOf() }
        previouslyVisibleUnits
            .filter { it !in currentlyVisibleUnits }
            .forEach { packets += PacketUnitRemove(it) }

        val movingStateByUnit = lastMovingStateSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        players.values
            .flatMap { it.units }
            .filter { it.id in currentlyVisibleUnits }
            .forEach { unit ->
                val lastMoving = movingStateByUnit[unit.id]
                val becameVisible = unit.id !in previouslyVisibleUnits
                val movingStateChanged = lastMoving != null && lastMoving != unit.moving
                if (becameVisible || unit.moving || movingStateChanged) {
                    packets += unit.toPacket()
                    movingStateByUnit[unit.id] = unit.moving
                }
            }

        movingStateByUnit.keys.removeAll { it !in currentlyVisibleUnits }

        previouslyVisibleUnits.clear()
        previouslyVisibleUnits += currentlyVisibleUnits
        previouslyVisibleTiles.clear()
        previouslyVisibleTiles += visibleTiles

        return packets
    }

    private fun updateUnitMovement(unit: UnitState, deltaSeconds: Float) {
        if (!unit.moving) return

        val dRow = unit.targetRow - unit.row
        val dCol = unit.targetCol - unit.col
        val distance = sqrt(dRow * dRow + dCol * dCol)
        val step = UNIT_SPEED_TILES_PER_SECOND * deltaSeconds

        if (distance <= step || distance <= ARRIVAL_THRESHOLD_TILES) {
            unit.row = unit.targetRow
            unit.col = unit.targetCol
            unit.moving = false
            return
        }

        unit.row += dRow / distance * step
        unit.col += dCol / distance * step
    }

    private fun createStartingUnit(player: PlayerState, offset: Int): UnitState {
        val spawn = spawnTileFor(players.size, offset)
        return UnitState(
            id = nextUnitId++,
            ownerPlayerId = player.id,
            unitType = UnitType.BARBARIAN,
            row = spawn.first.toFloat(),
            col = spawn.second.toFloat(),
            targetRow = spawn.first.toFloat(),
            targetCol = spawn.second.toFloat()
        )
    }

    private fun spawnTileFor(playerIndex: Int, offset: Int): Pair<Int, Int> {
        val margin = 3 + offset
        if (gameType == GameType.G1V1) {
            return when (playerIndex % 2) {
                0 -> margin to margin
                else -> (worldMap.height - 1 - margin) to (worldMap.width - 1 - margin)
            }
        }

        return when (playerIndex % 4) {
            0 -> margin to margin
            1 -> margin to (worldMap.width - 1 - margin)
            2 -> (worldMap.height - 1 - margin) to margin
            else -> (worldMap.height - 1 - margin) to (worldMap.width - 1 - margin)
        }
    }

    private fun computeVisibleTileIndices(player: PlayerState): Set<Int> {
        val visible = linkedSetOf<Int>()
        val radius = worldMap.chunkSize
        val radiusSquared = radius * radius

        for (unit in player.units) {
            val centerRow = floor(unit.row).toInt()
            val centerCol = floor(unit.col).toInt()
            for (row in (centerRow - radius)..(centerRow + radius)) {
                if (row !in 0 until worldMap.height) continue
                for (col in (centerCol - radius)..(centerCol + radius)) {
                    if (col !in 0 until worldMap.width) continue
                    val dRow = row - centerRow
                    val dCol = col - centerCol
                    if (dRow * dRow + dCol * dCol <= radiusSquared) {
                        visible += row * worldMap.width + col
                    }
                }
            }
        }

        return visible
    }

    private fun computeVisibleChunks(visibleTiles: Set<Int>): Set<ChunkCoord> {
        return visibleTiles.mapNotNullTo(linkedSetOf()) { tileIndex ->
            val row = tileIndex / worldMap.width
            val col = tileIndex % worldMap.width
            worldMap.worldToChunkCoord(row, col)
        }
    }

    private fun WorldChunk.toPacket(): PacketMapChunk {
        val ordinals = IntArray(size * size)
        ground.forEachIndexed { row, col, value ->
            ordinals[row * size + col] = value?.ordinal ?: UNKNOWN_TILE
        }
        return PacketMapChunk(
            chunkX = coord.x,
            chunkY = coord.y,
            terrainOrdinals = ordinals
        )
    }

    private fun UnitState.toPacket(): PacketUnitSnapshot {
        return PacketUnitSnapshot(
            unitId = id,
            ownerPlayerId = ownerPlayerId,
            unitType = unitType,
            row = row,
            col = col,
            targetRow = targetRow,
            targetCol = targetCol,
            moving = moving
        )
    }

    private fun UnitState.tileIndex(): Int {
        val clampedRow = floor(row).toInt().coerceIn(0, worldMap.height - 1)
        val clampedCol = floor(col).toInt().coerceIn(0, worldMap.width - 1)
        return clampedRow * worldMap.width + clampedCol
    }

    private fun chunkColumns(): Int {
        return (worldMap.width + worldMap.chunkSize - 1) / worldMap.chunkSize
    }

    private fun expireReconnectWindows(nowMs: Long) {
        reconnectDeadlineByPlayer
            .filterValues { deadline -> deadline <= nowMs }
            .keys
            .forEach { playerId ->
                if (connectionStateByPlayer[playerId] == PlayerConnectionState.WAITING_RECONNECTION) {
                    connectionStateByPlayer[playerId] = PlayerConnectionState.DISCONNECTED
                }
                reconnectDeadlineByPlayer.remove(playerId)
            }
    }

    companion object {
        private const val UNIT_SPEED_TILES_PER_SECOND = 4f
        private const val ARRIVAL_THRESHOLD_TILES = 0.05f
        private const val UNKNOWN_TILE = -1
        private const val RECONNECT_GRACE_PERIOD_MS = 3 * 60 * 1000L
    }
}
