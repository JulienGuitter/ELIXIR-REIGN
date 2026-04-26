package com.mjm.elixir_reign.core.session

import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketUnitRemove
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketVisibilityUpdate
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
    private val visibleTiles = linkedSetOf<Pair<Int, Int>>()
    private val networkUnits = linkedMapOf<Int, UnitState>()

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
            mapRevision = 0

            packet.players.firstOrNull { it.id == packet.myPlayerId }?.let { player ->
                gold = player.gold
                elixir = player.elixir
                darkElixir = player.darkElixir
            }

            knownChunks.clear()
            visibleChunks.clear()
            visibleTiles.clear()
            networkUnits.clear()
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
            visibleTiles.clear()
            packet.visibleChunkKeys.mapNotNullTo(visibleChunks) { key ->
                val parts = key.split(":")
                val x = parts.getOrNull(0)?.toIntOrNull()
                val y = parts.getOrNull(1)?.toIntOrNull()
                if (x == null || y == null) null else ChunkCoord(x, y)
            }
            packet.visibleTileKeys.mapNotNullTo(visibleTiles) { key ->
                val parts = key.split(":")
                val row = parts.getOrNull(0)?.toIntOrNull()
                val col = parts.getOrNull(1)?.toIntOrNull()
                if (row == null || col == null) null else row to col
            }
        }
    }

    fun applyUnitSnapshot(packet: PacketUnitSnapshot) {
        synchronized(networkStateLock) {
            networkUnits[packet.unitId] = UnitState(
                id = packet.unitId,
                ownerPlayerId = packet.ownerPlayerId,
                unitType = packet.unitType,
                row = packet.row,
                col = packet.col,
                targetRow = packet.targetRow,
                targetCol = packet.targetCol,
                moving = packet.moving
            )
        }
    }

    fun applyUnitRemove(packet: PacketUnitRemove) {
        synchronized(networkStateLock) {
            networkUnits.remove(packet.unitId)
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

    fun visibleTilesSnapshot(): Set<Pair<Int, Int>> {
        synchronized(networkStateLock) {
            return visibleTiles.toSet()
        }
    }

    fun unitSnapshots(): List<UnitState> {
        synchronized(networkStateLock) {
            return networkUnits.values.map {
                UnitState(
                    id = it.id,
                    ownerPlayerId = it.ownerPlayerId,
                    unitType = it.unitType,
                    row = it.row,
                    col = it.col,
                    targetRow = it.targetRow,
                    targetCol = it.targetCol,
                    moving = it.moving
                )
            }
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
            knownChunks.clear()
            visibleChunks.clear()
            visibleTiles.clear()
            networkUnits.clear()
        }
    }

    private const val DEFAULT_GOLD = 1200
    private const val DEFAULT_ELIXIR = 1200
    private const val DEFAULT_DARK_ELIXIR = 80
    private const val UNKNOWN_TILE = -1
}
