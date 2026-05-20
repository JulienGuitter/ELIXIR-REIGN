package com.mjm.elixir_reign.server.game

import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.game.BuildingInstanceState
import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.network.PacketBuildingRemove
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketPlaceBuildingResult
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketPlayerStatus
import com.mjm.elixir_reign.shared.network.PacketPlayerSummary
import com.mjm.elixir_reign.shared.network.PacketUpgradeBuildingResult
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
    private val visibleBuildingsByPlayer = mutableMapOf<Int, MutableSet<Int>>()
    private val lastBuildingLevelSentByPlayer = mutableMapOf<Int, MutableMap<Int, Int>>()
    private val visibleTilesByPlayer = mutableMapOf<Int, MutableSet<Int>>()
    private val lastPresenceSentByPlayer = mutableMapOf<Int, Map<Int, PlayerConnectionState>>()
    private val lastMovingStateSentByPlayer = mutableMapOf<Int, MutableMap<Int, Boolean>>()
    private val reconnectDeadlineByPlayer = mutableMapOf<Int, Long>()
    private val connectionStateByPlayer = mutableMapOf<Int, PlayerConnectionState>()
    private var nextUnitId = 1
    private var nextBuildingId = 1
    private var started = false

    fun addPlayer(playerId: Int, name: String) {
        if (players.containsKey(playerId)) return

        val player = PlayerState(id = playerId, name = name)
        if (gameType == GameType.G1V1) {
            player.buildings += createStartingTownHall(player, if (players.isEmpty()) STARTING_TOWN_HALL_ROW_OFFSET else 0)
        }
        player.units += createStartingUnit(player, offset = 0)
        player.units += createStartingUnit(player, offset = 1)
        players[playerId] = player
        connectionStateByPlayer[playerId] = PlayerConnectionState.CONNECTED
        reconnectDeadlineByPlayer.remove(playerId)
        sentChunksByPlayer[playerId] = mutableSetOf()
        visibleUnitsByPlayer[playerId] = mutableSetOf()
        visibleBuildingsByPlayer[playerId] = mutableSetOf()
        lastBuildingLevelSentByPlayer[playerId] = mutableMapOf()
        visibleTilesByPlayer[playerId] = mutableSetOf()
    }

    fun removePlayer(playerId: Int) {
        players.remove(playerId)
        connectionStateByPlayer.remove(playerId)
        reconnectDeadlineByPlayer.remove(playerId)
        sentChunksByPlayer.remove(playerId)
        visibleUnitsByPlayer.remove(playerId)
        visibleBuildingsByPlayer.remove(playerId)
        lastBuildingLevelSentByPlayer.remove(playerId)
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
        visibleBuildingsByPlayer[playerId] = mutableSetOf()
        lastBuildingLevelSentByPlayer[playerId] = mutableMapOf()
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

    fun handlePlaceBuildingRequest(playerId: Int, requestId: Int, entityType: EntityType, row: Int, col: Int): List<Any> {
        val player = players[playerId] ?: return listOf(PacketPlaceBuildingResult(requestId, false, "Joueur introuvable."))
        val stats = buildingStats(entityType)
            ?: return listOf(PacketPlaceBuildingResult(requestId, false, "Type de batiment invalide."))

        val validationError = validateBuildingPlacement(player, entityType, stats, row, col)
        if (validationError != null) {
            return listOf(PacketPlaceBuildingResult(requestId, false, validationError))
        }

        if (!spend(player, stats.costGold, stats.costElixir, stats.costDarkElixir)) {
            return listOf(PacketPlaceBuildingResult(requestId, false, "Ressources insuffisantes."))
        }

        val building = BuildingInstanceState(
            id = nextBuildingId++,
            ownerPlayerId = player.id,
            entityType = entityType,
            row = row,
            col = col,
            level = 1
        )
        player.buildings += building

        return listOf(
            PacketPlaceBuildingResult(requestId, true, "", building.id),
            player.resourcesPacket(),
            building.toPacket()
        )
    }

    fun handleUpgradeBuildingRequest(playerId: Int, requestId: Int, buildingId: Int): List<Any> {
        val player = players[playerId] ?: return listOf(PacketUpgradeBuildingResult(requestId, false, "Joueur introuvable.", buildingId))
        val building = player.buildings.firstOrNull { it.id == buildingId }
            ?: return listOf(PacketUpgradeBuildingResult(requestId, false, "Batiment introuvable.", buildingId))
        val stats = buildingStats(building.entityType)
            ?: return listOf(PacketUpgradeBuildingResult(requestId, false, "Type de batiment invalide.", buildingId))
        if (building.level >= stats.maxLevel) {
            return listOf(PacketUpgradeBuildingResult(requestId, false, "Niveau maximum atteint.", buildingId, building.level))
        }
        val multiplier = building.level + 1
        val goldCost = stats.costGold * multiplier
        val elixirCost = stats.costElixir * multiplier
        val darkElixirCost = stats.costDarkElixir * multiplier
        if (!spend(player, goldCost, elixirCost, darkElixirCost)) {
            return listOf(PacketUpgradeBuildingResult(requestId, false, "Ressources insuffisantes.", buildingId, building.level))
        }

        building.level += 1
        return listOf(
            PacketUpgradeBuildingResult(requestId, true, "", buildingId, building.level),
            player.resourcesPacket(),
            building.toPacket()
        )
    }

    fun update(deltaSeconds: Float) {
        expireReconnectWindows(System.currentTimeMillis())
        if (!started || deltaSeconds <= 0f) return

        players.values
            .flatMap { it.units }
            .forEach { updateUnitMovement(it, deltaSeconds) }

        players.values.forEach { updateResourceProduction(it, deltaSeconds) }
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
        val packets = mutableListOf<Any>()

        val sentChunks = sentChunksByPlayer.getOrPut(playerId) { mutableSetOf() }
        worldMap.allChunks()
            .map { it.coord }
            .filter { it !in sentChunks }
            .sortedWith(compareBy<ChunkCoord> { it.y }.thenBy { it.x })
            .mapNotNull { worldMap.chunkAt(it) }
            .forEach { chunk ->
                packets += chunk.toPacket()
                sentChunks += chunk.coord
            }

        val visibleTiles = computeVisibleTileIndices(player)
        val visibleChunks = computeVisibleChunks(visibleTiles)

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

        packets += player.resourcesPacket()

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

        val currentlyVisibleBuildings = players.values
            .flatMap { it.buildings }
            .filter { building -> building.tileIndex() in visibleTiles }
            .map { it.id }
            .toMutableSet()

        val previouslyVisibleBuildings = visibleBuildingsByPlayer.getOrPut(playerId) { mutableSetOf() }
        previouslyVisibleBuildings
            .filter { it !in currentlyVisibleBuildings }
            .forEach { packets += PacketBuildingRemove(it) }

        val lastBuildingLevels = lastBuildingLevelSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        players.values
            .flatMap { it.buildings }
            .filter { it.id in currentlyVisibleBuildings }
            .forEach { building ->
                val lastSentLevel = lastBuildingLevels[building.id]
                if (building.id !in previouslyVisibleBuildings || lastSentLevel != building.level) {
                    packets += building.toPacket()
                    lastBuildingLevels[building.id] = building.level
                }
            }

        lastBuildingLevels.keys.removeAll { it !in currentlyVisibleBuildings }

        previouslyVisibleUnits.clear()
        previouslyVisibleUnits += currentlyVisibleUnits
        previouslyVisibleBuildings.clear()
        previouslyVisibleBuildings += currentlyVisibleBuildings
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

    private fun updateResourceProduction(player: PlayerState, deltaSeconds: Float) {
        player.buildings.forEach { building ->
            val stats = buildingStats(building.entityType) ?: return@forEach
            if (stats.productionRate <= 0f) return@forEach

            building.productionAccumulator += stats.productionRate * building.level * deltaSeconds
            val produced = floor(building.productionAccumulator).toInt()
            if (produced <= 0) return@forEach

            building.productionAccumulator -= produced
            when (building.entityType) {
                EntityType.GOLD_MINE -> player.gold += produced
                EntityType.ELEXIR_PUMP -> player.elixir += produced
                EntityType.DARCKELEXIR_PUMP -> player.darkElixir += produced
                else -> Unit
            }
        }
    }

    private fun validateBuildingPlacement(
        player: PlayerState,
        entityType: EntityType,
        stats: BuildingStats,
        row: Int,
        col: Int
    ): String? {
        val cells = footprint(row, col, stats.footprintSizeTiles)
        if (cells.isEmpty()) return "Position invalide."

        val requiredMaterial = when (entityType) {
            EntityType.GOLD_MINE -> com.mjm.elixir_reign.shared.terrain.TerrainMaterial.GOLD
            EntityType.ELEXIR_PUMP -> com.mjm.elixir_reign.shared.terrain.TerrainMaterial.ELEXIR
            EntityType.DARCKELEXIR_PUMP -> com.mjm.elixir_reign.shared.terrain.TerrainMaterial.DARK_ELEXIR
            else -> null
        }
        var hasRequiredResource = false

        for ((cellRow, cellCol) in cells) {
            if (cellRow !in 0 until worldMap.height || cellCol !in 0 until worldMap.width) {
                return "Position hors limites."
            }
            val terrain = worldMap[cellRow, cellCol] ?: return "Terrain inconnu."
            val isResourceTile = requiredMaterial != null && terrain.material == requiredMaterial
            if (!isResourceTile && !terrain.canBuildOn) {
                return "Terrain non constructible."
            }
            if (isResourceTile) {
                hasRequiredResource = true
            }
        }
        if (requiredMaterial != null && !hasRequiredResource) {
            return "Ce batiment doit etre place sur sa ressource."
        }

        val occupiedByBuilding = players.values
            .flatMap { it.buildings }
            .flatMap { footprint(it.row, it.col, buildingStats(it.entityType)?.footprintSizeTiles ?: 1) }
            .toSet()
        if (cells.any { it in occupiedByBuilding }) {
            return "Emplacement deja occupe."
        }

        if (!hasOwnedTroopNear(player, row, col)) {
            return "Une troupe alliee doit etre proche."
        }

        return null
    }

    private fun hasOwnedTroopNear(player: PlayerState, row: Int, col: Int): Boolean {
        val maxDistanceSquared = worldMap.chunkSize * worldMap.chunkSize
        return player.units.any { unit ->
            val dRow = floor(unit.row).toInt() - row
            val dCol = floor(unit.col).toInt() - col
            dRow * dRow + dCol * dCol <= maxDistanceSquared
        }
    }

    private fun spend(player: PlayerState, goldCost: Int, elixirCost: Int, darkElixirCost: Int): Boolean {
        if (player.gold < goldCost || player.elixir < elixirCost || player.darkElixir < darkElixirCost) {
            return false
        }
        player.gold -= goldCost
        player.elixir -= elixirCost
        player.darkElixir -= darkElixirCost
        return true
    }

    private fun footprint(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
        val normalizedSize = size.coerceAtLeast(1)
        val startRow = centerRow - normalizedSize / 2
        val startCol = centerCol - normalizedSize / 2
        val cells = ArrayList<Pair<Int, Int>>(normalizedSize * normalizedSize)
        for (row in startRow until startRow + normalizedSize) {
            for (col in startCol until startCol + normalizedSize) {
                cells += row to col
            }
        }
        return cells
    }

    private fun buildingStats(entityType: EntityType): BuildingStats? {
        return when (entityType) {
            EntityType.BARRACKS -> BuildingStats.BARRACKS
            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP
            EntityType.GOLD_MINE -> BuildingStats.GOLD_MINE
            EntityType.ARCHER_TOWER -> BuildingStats.ARCHER_TOWER
            EntityType.TOWN_HALL -> BuildingStats.TOWN_HALL
            else -> null
        }
    }

    private fun createStartingUnit(player: PlayerState, offset: Int): UnitState {
        val spawn = spawnTileFor(players.size, offset)
        return UnitState(
            id = nextUnitId++,
            ownerPlayerId = player.id,
            entityType = EntityType.BARBARIAN,
            row = spawn.first.toFloat(),
            col = spawn.second.toFloat(),
            targetRow = spawn.first.toFloat(),
            targetCol = spawn.second.toFloat()
        )
    }

    private fun createStartingTownHall(player: PlayerState, rowOffset: Int): BuildingInstanceState {
        val spawn = spawnTileFor(players.size, rowOffset)
        return BuildingInstanceState(
            id = nextBuildingId++,
            ownerPlayerId = player.id,
            entityType = EntityType.TOWN_HALL,
            row = spawn.first,
            col = spawn.second,
            level = 1
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
            entityType = entityType,
            row = row,
            col = col,
            targetRow = targetRow,
            targetCol = targetCol,
            moving = moving
        )
    }

    private fun BuildingInstanceState.toPacket(): PacketBuildingSnapshot {
        return PacketBuildingSnapshot(
            buildingId = id,
            ownerPlayerId = ownerPlayerId,
            entityType = entityType,
            row = row,
            col = col,
            level = level
        )
    }

    private fun PlayerState.resourcesPacket(): PacketPlayerResources {
        return PacketPlayerResources(
            gold = gold,
            elixir = elixir,
            darkElixir = darkElixir
        )
    }

    private fun UnitState.tileIndex(): Int {
        val clampedRow = floor(row).toInt().coerceIn(0, worldMap.height - 1)
        val clampedCol = floor(col).toInt().coerceIn(0, worldMap.width - 1)
        return clampedRow * worldMap.width + clampedCol
    }

    private fun BuildingInstanceState.tileIndex(): Int {
        val clampedRow = row.coerceIn(0, worldMap.height - 1)
        val clampedCol = col.coerceIn(0, worldMap.width - 1)
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
        private const val STARTING_TOWN_HALL_ROW_OFFSET = 2
    }
}
