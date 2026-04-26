package com.mjm.elixir_reign.server.game

import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.logic.UnitType
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketPlayerSummary
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
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
    private var nextUnitId = 1
    private var started = false

    fun addPlayer(playerId: Int, name: String) {
        if (players.containsKey(playerId)) return

        val player = PlayerState(id = playerId, name = name)
        player.units += createStartingUnit(player, offset = 0)
        player.units += createStartingUnit(player, offset = 1)
        players[playerId] = player
        sentChunksByPlayer[playerId] = mutableSetOf()
        visibleUnitsByPlayer[playerId] = mutableSetOf()
    }

    fun removePlayer(playerId: Int) {
        players.remove(playerId)
        sentChunksByPlayer.remove(playerId)
        visibleUnitsByPlayer.remove(playerId)
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
                    darkElixir = summary.darkElixir
                )
            })
        )
        packets.addAll(syncPacketsFor(playerId))
        packets += PacketGameReady()
        return packets
    }

    fun syncPacketsFor(playerId: Int): List<Any> {
        val player = players[playerId] ?: return emptyList()
        val visibleTiles = computeVisibleTiles(player)
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

        packets += PacketVisibilityUpdate(
            visibleChunkKeys = ArrayList(visibleChunks.map { it.toKey() }),
            visibleTileKeys = ArrayList(visibleTiles.map { it.toKey() })
        )

        val currentlyVisibleUnits = players.values
            .flatMap { it.units }
            .filter { unit ->
                unit.tileKey() in visibleTiles
            }
            .map { it.id }
            .toMutableSet()

        val previouslyVisibleUnits = visibleUnitsByPlayer.getOrPut(playerId) { mutableSetOf() }
        previouslyVisibleUnits
            .filter { it !in currentlyVisibleUnits }
            .forEach { packets += PacketUnitRemove(it) }

        players.values
            .flatMap { it.units }
            .filter { it.id in currentlyVisibleUnits }
            .forEach { unit -> packets += unit.toPacket() }

        previouslyVisibleUnits.clear()
        previouslyVisibleUnits += currentlyVisibleUnits

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

    private fun computeVisibleTiles(player: PlayerState): Set<Pair<Int, Int>> {
        val visible = linkedSetOf<Pair<Int, Int>>()
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
                        visible += row to col
                    }
                }
            }
        }

        return visible
    }

    private fun computeVisibleChunks(visibleTiles: Set<Pair<Int, Int>>): Set<ChunkCoord> {
        return visibleTiles.mapNotNullTo(linkedSetOf()) { (row, col) ->
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

    private fun UnitState.tileKey(): Pair<Int, Int> {
        return floor(row).toInt() to floor(col).toInt()
    }

    private fun ChunkCoord.toKey(): String {
        return "$x:$y"
    }

    private fun Pair<Int, Int>.toKey(): String {
        return "$first:$second"
    }

    companion object {
        private const val UNIT_SPEED_TILES_PER_SECOND = 4f
        private const val ARRIVAL_THRESHOLD_TILES = 0.05f
        private const val UNKNOWN_TILE = -1
    }
}
