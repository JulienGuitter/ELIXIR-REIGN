package com.mjm.elixir_reign.core.session

import com.mjm.elixir_reign.shared.game.BuildingInstanceState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.network.PacketBuildingRemove
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketGameOver
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
import com.mjm.elixir_reign.shared.network.PlayerConnectionState
import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.type.GameType
import com.mjm.elixir_reign.shared.world.ChunkCoord
import com.mjm.elixir_reign.shared.world.TileGrid
import com.mjm.elixir_reign.shared.world.WorldChunk
import com.mjm.elixir_reign.shared.world.WorldMap

object GameSession {
    private val networkStateLock = Any()
    private val knownChunks = linkedMapOf<ChunkCoord, WorldChunk>()
    private val visibleChunks = linkedSetOf<ChunkCoord>()
    private val visibleTiles = linkedSetOf<Int>()
    private var visibilityMask = BooleanArray(0)
    @Volatile
    private var currentFogSnapshot = FogSnapshot(width = 0, height = 0, alphaByTile = floatArrayOf())
    private val networkUnits = linkedMapOf<Int, UnitState>()
    private val networkBuildings = linkedMapOf<Int, BuildingInstanceState>()
    private val playerNameById = linkedMapOf<Int, String>()
    private val playerStateById = linkedMapOf<Int, PlayerConnectionState>()

    @Volatile
    var mode: GameMode = GameMode.SOLO

    @Volatile
    var gameType: GameType = GameType.SOLO

    @Volatile
    var gold: Int = DEFAULT_GOLD

    @Volatile
    var elixir: Int = DEFAULT_ELIXIR

    @Volatile
    var darkElixir: Int = DEFAULT_DARK_ELIXIR

    @Volatile
    var myPlayerId: Int = 0
        private set

    @Volatile
    var mapWidth: Int = 0
        private set

    @Volatile
    var mapHeight: Int = 0
        private set

    @Volatile
    var chunkSize: Int = 0
        private set

    @Volatile
    var playerNames: List<String> = emptyList()
        private set

    @Volatile
    var mapRevision: Int = 0
        private set

    @Volatile
    var gameOver: Boolean = false
        private set

    @Volatile
    var winnerPlayerId: Int = 0
        private set

    @Volatile
    var eliminatedPlayerIds: Set<Int> = emptySet()
        private set

    fun startSolo() {
        mode = GameMode.SOLO
        gameType = GameType.SOLO
        resetResources()
        clearNetworkState()
    }

    fun startMultiplayer(gameType: GameType) {
        mode = GameMode.MULTI
        this.gameType = gameType
        resetResources()
        clearNetworkState()
    }

    fun applyGameInit(packet: PacketGameInit) {
        synchronized(networkStateLock) {
            myPlayerId = packet.myPlayerId
            mapWidth = packet.mapWidth
            mapHeight = packet.mapHeight
            chunkSize = packet.chunkSize
            playerNames = packet.players.map { it.name }
            playerNameById.clear()
            playerStateById.clear()
            packet.players.forEach { player ->
                playerNameById[player.id] = player.name
                playerStateById[player.id] = player.connectionState
            }
            mapRevision = 0

            packet.players.firstOrNull { it.id == packet.myPlayerId }?.let { player ->
                gold = player.gold
                elixir = player.elixir
                darkElixir = player.darkElixir
            }

            knownChunks.clear()
            visibleChunks.clear()
            visibleTiles.clear()
            visibilityMask = if (mapWidth > 0 && mapHeight > 0) {
                BooleanArray(mapWidth * mapHeight)
            } else {
                BooleanArray(0)
            }
            currentFogSnapshot = FogSnapshot(width = mapWidth, height = mapHeight, alphaByTile = FloatArray(mapWidth * mapHeight) { 1f })
            networkUnits.clear()
            networkBuildings.clear()
            gameOver = false
            winnerPlayerId = 0
            eliminatedPlayerIds = emptySet()
        }
    }

    fun applyMapChunk(packet: PacketMapChunk) {
        synchronized(networkStateLock) {
            if (chunkSize <= 0) return

            val ground = TileGrid.empty<TerrainType>(chunkSize)
            for (row in 0 until chunkSize) {
                for (col in 0 until chunkSize) {
                    val ordinal = packet.terrainOrdinals.getOrNull(row * chunkSize + col) ?: UNKNOWN_TILE
                    ground[row, col] = TerrainType.entries.getOrNull(ordinal)
                }
            }

            val coord = ChunkCoord(packet.chunkX, packet.chunkY)
            knownChunks[coord] = WorldChunk(
                coord = coord,
                ground = ground
            )
            mapRevision += 1
        }
    }

    fun applyVisibilityUpdate(packet: PacketVisibilityUpdate) {
        synchronized(networkStateLock) {
            visibleChunks.clear()
            val chunkColumns = chunkColumns()
            packet.visibleChunkIndices.forEach { chunkIndex ->
                if (chunkColumns <= 0) return@forEach
                val chunkY = chunkIndex / chunkColumns
                val chunkX = chunkIndex % chunkColumns
                if (chunkX >= 0 && chunkY >= 0) {
                    visibleChunks += ChunkCoord(chunkX, chunkY)
                }
            }

            if (packet.fullSync) {
                visibleTiles.clear()
                visibilityMask.fill(false)
            }

            packet.visibleTileIndices.forEach { tileIndex ->
                if (tileIndex !in 0 until visibilityMask.size) return@forEach
                visibleTiles += tileIndex
                visibilityMask[tileIndex] = true
            }

            packet.hiddenTileIndices.forEach { tileIndex ->
                if (tileIndex !in 0 until visibilityMask.size) return@forEach
                visibleTiles.remove(tileIndex)
                visibilityMask[tileIndex] = false
            }

            rebuildFogSnapshotLocked()
        }
    }

    fun applyUnitSnapshot(packet: PacketUnitSnapshot) {
        synchronized(networkStateLock) {
            networkUnits[packet.unitId] = UnitState(
                id = packet.unitId,
                ownerPlayerId = packet.ownerPlayerId,
                entityType = packet.entityType,
                row = packet.row,
                col = packet.col,
                targetRow = packet.targetRow,
                targetCol = packet.targetCol,
                moving = packet.moving,
                currentHP = packet.currentHP,
                maxHP = packet.maxHP,
                barracksId = packet.barracksId
            )
        }
    }

    fun applyUnitRemove(packet: PacketUnitRemove) {
        synchronized(networkStateLock) {
            networkUnits.remove(packet.unitId)
        }
    }

    fun applyBuildingSnapshot(packet: PacketBuildingSnapshot) {
        synchronized(networkStateLock) {
            networkBuildings[packet.buildingId] = BuildingInstanceState(
                id = packet.buildingId,
                ownerPlayerId = packet.ownerPlayerId,
                entityType = packet.entityType,
                row = packet.row,
                col = packet.col,
                level = packet.level,
                currentHP = packet.currentHP,
                maxHP = packet.maxHP,
                destroyed = packet.destroyed,
                maxFormedUnits = packet.maxFormedUnits,
                trainingQueue = packet.trainingQueue.toMutableList(),
                hasActiveTraining = packet.hasActiveTraining,
                activeTrainingUnitType = packet.activeTrainingUnitType,
                activeTrainingElapsedSeconds = packet.activeTrainingElapsedSeconds
            )
        }
    }

    fun applyBuildingRemove(packet: PacketBuildingRemove) {
        synchronized(networkStateLock) {
            networkBuildings.remove(packet.buildingId)
        }
    }

    fun applyPlayerResources(packet: PacketPlayerResources) {
        gold = packet.gold
        elixir = packet.elixir
        darkElixir = packet.darkElixir
    }

    fun applyPlayerPresenceUpdate(packet: PacketPlayerPresenceUpdate) {
        synchronized(networkStateLock) {
            packet.players.forEach { status ->
                if (playerNameById.containsKey(status.id)) {
                    playerStateById[status.id] = status.connectionState
                }
            }
        }
    }

    fun applyGameOver(packet: PacketGameOver) {
        synchronized(networkStateLock) {
            gameOver = true
            winnerPlayerId = packet.winnerPlayerId
            eliminatedPlayerIds = packet.eliminatedPlayerIds.toSet()
        }
    }

    fun getPlayerState(playerName: String): PlayerConnectionState {
        synchronized(networkStateLock) {
            val playerId = playerNameById.entries.firstOrNull { it.value == playerName }?.key
                ?: return PlayerConnectionState.CONNECTED
            return playerStateById[playerId] ?: PlayerConnectionState.CONNECTED
        }
    }

    fun getPlayerName(playerId: Int): String {
        synchronized(networkStateLock) {
            return playerNameById[playerId] ?: "Joueur $playerId"
        }
    }

    fun multiplayerWorldMap(): WorldMap? {
        synchronized(networkStateLock) {
            if (mapWidth <= 0 || mapHeight <= 0 || chunkSize <= 0) return null
            return WorldMap.fromChunks(
                chunkSize = chunkSize,
                width = mapWidth,
                height = mapHeight,
                chunks = knownChunks.toMap()
            )
        }
    }

    fun visibleChunksSnapshot(): Set<ChunkCoord> {
        synchronized(networkStateLock) {
            return visibleChunks.toSet()
        }
    }

    fun fogSnapshot(): FogSnapshot {
        return currentFogSnapshot
    }

    fun hasInitialMultiplayerVisibility(): Boolean {
        synchronized(networkStateLock) {
            return mapWidth > 0 && mapHeight > 0 && chunkSize > 0 && visibleTiles.isNotEmpty()
        }
    }

    fun unitSnapshots(): List<UnitState> {
        synchronized(networkStateLock) {
            return networkUnits.values.map {
                UnitState(
                    id = it.id,
                    ownerPlayerId = it.ownerPlayerId,
                    entityType = it.entityType,
                    row = it.row,
                    col = it.col,
                    targetRow = it.targetRow,
                    targetCol = it.targetCol,
                    moving = it.moving,
                    currentHP = it.currentHP,
                    maxHP = it.maxHP,
                    barracksId = it.barracksId
                )
            }
        }
    }

    fun buildingSnapshots(): List<BuildingInstanceState> {
        synchronized(networkStateLock) {
            return networkBuildings.values.map {
                BuildingInstanceState(
                    id = it.id,
                    ownerPlayerId = it.ownerPlayerId,
                    entityType = it.entityType,
                    row = it.row,
                    col = it.col,
                    level = it.level,
                    currentHP = it.currentHP,
                    maxHP = it.maxHP,
                    destroyed = it.destroyed,
                    maxFormedUnits = it.maxFormedUnits,
                    trainingQueue = it.trainingQueue.toMutableList(),
                    hasActiveTraining = it.hasActiveTraining,
                    activeTrainingUnitType = it.activeTrainingUnitType,
                    activeTrainingElapsedSeconds = it.activeTrainingElapsedSeconds
                )
            }
        }
    }

    fun spendResources(goldCost: Int, elixirCost: Int, darkElixirCost: Int): Boolean {
        synchronized(networkStateLock) {
            if (gold < goldCost || elixir < elixirCost || darkElixir < darkElixirCost) {
                return false
            }
            gold -= goldCost
            elixir -= elixirCost
            darkElixir -= darkElixirCost
            return true
        }
    }

    fun addResources(goldAmount: Int = 0, elixirAmount: Int = 0, darkElixirAmount: Int = 0) {
        synchronized(networkStateLock) {
            gold += goldAmount
            elixir += elixirAmount
            darkElixir += darkElixirAmount
        }
    }

    private fun resetResources() {
        gold = DEFAULT_GOLD
        elixir = DEFAULT_ELIXIR
        darkElixir = DEFAULT_DARK_ELIXIR
    }

    private fun clearNetworkState() {
        synchronized(networkStateLock) {
            myPlayerId = 0
            mapWidth = 0
            mapHeight = 0
            chunkSize = 0
            mapRevision = 0
            playerNames = emptyList()
            playerNameById.clear()
            playerStateById.clear()
            knownChunks.clear()
            visibleChunks.clear()
            visibleTiles.clear()
            visibilityMask = BooleanArray(0)
            currentFogSnapshot = FogSnapshot(width = 0, height = 0, alphaByTile = floatArrayOf())
            networkUnits.clear()
            networkBuildings.clear()
            gameOver = false
            winnerPlayerId = 0
            eliminatedPlayerIds = emptySet()
        }
    }

    private fun rebuildFogSnapshotLocked() {
        if (mapWidth <= 0 || mapHeight <= 0) {
            currentFogSnapshot = FogSnapshot(width = 0, height = 0, alphaByTile = floatArrayOf())
            return
        }

        val alphaByTile = FloatArray(mapWidth * mapHeight)
        for (row in 0 until mapHeight) {
            for (col in 0 until mapWidth) {
                val tileIndex = row * mapWidth + col
                if (visibilityMask.getOrNull(tileIndex) == true) {
                    alphaByTile[tileIndex] = 0f
                    continue
                }

                alphaByTile[tileIndex] = when {
                    hasVisibleTileAround(row, col, maxDistanceSquared = FIRST_RING_DISTANCE_SQUARED) -> 0.4f
                    hasVisibleTileAround(row, col, maxDistanceSquared = SECOND_RING_DISTANCE_SQUARED) -> 0.7f
                    else -> 1f
                }
            }
        }
        currentFogSnapshot = FogSnapshot(width = mapWidth, height = mapHeight, alphaByTile = alphaByTile)
    }

    private fun hasVisibleTileAround(row: Int, col: Int, maxDistanceSquared: Int): Boolean {
        val radius = if (maxDistanceSquared <= FIRST_RING_DISTANCE_SQUARED) FIRST_RING_RADIUS else SECOND_RING_RADIUS
        for (testRow in (row - radius)..(row + radius)) {
            if (testRow !in 0 until mapHeight) continue
            for (testCol in (col - radius)..(col + radius)) {
                if (testCol !in 0 until mapWidth) continue
                val dRow = testRow - row
                val dCol = testCol - col
                if (dRow * dRow + dCol * dCol > maxDistanceSquared) continue
                val testIndex = testRow * mapWidth + testCol
                if (visibilityMask.getOrNull(testIndex) == true) {
                    return true
                }
            }
        }
        return false
    }

    private fun chunkColumns(): Int {
        if (chunkSize <= 0 || mapWidth <= 0) return 0
        return (mapWidth + chunkSize - 1) / chunkSize
    }

    data class FogSnapshot(
        val width: Int,
        val height: Int,
        val alphaByTile: FloatArray
    )

    private const val DEFAULT_GOLD = 1200
    private const val DEFAULT_ELIXIR = 1200
    private const val DEFAULT_DARK_ELIXIR = 80
    private const val UNKNOWN_TILE = -1
    private const val FIRST_RING_DISTANCE_SQUARED = 2
    private const val SECOND_RING_DISTANCE_SQUARED = 8
    private const val FIRST_RING_RADIUS = 1
    private const val SECOND_RING_RADIUS = 2
}
