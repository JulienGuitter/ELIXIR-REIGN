package com.mjm.elixir_reign.server.game

import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.game.BuildingInstanceState
import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.game.UnitState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.network.PacketBuildingRemove
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameOver
import com.mjm.elixir_reign.shared.network.PacketGameReady
import com.mjm.elixir_reign.shared.network.PacketMapChunk
import com.mjm.elixir_reign.shared.network.PacketPlaceBuildingResult
import com.mjm.elixir_reign.shared.network.PacketPlayerPresenceUpdate
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketPlayerStatus
import com.mjm.elixir_reign.shared.network.PacketPlayerSummary
import com.mjm.elixir_reign.shared.network.PacketTrainUnitResult
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
    private val lastUnitHealthSentByPlayer = mutableMapOf<Int, MutableMap<Int, Float>>()
    private val lastBuildingHealthSentByPlayer = mutableMapOf<Int, MutableMap<Int, BuildingHealthSnapshot>>()
    private val lastBuildingTrainingSentByPlayer = mutableMapOf<Int, MutableMap<Int, BuildingTrainingSnapshot>>()
    private val eliminatedPlayerIds = linkedSetOf<Int>()
    private val gameOverSentByPlayer = mutableSetOf<Int>()
    private val reconnectDeadlineByPlayer = mutableMapOf<Int, Long>()
    private val connectionStateByPlayer = mutableMapOf<Int, PlayerConnectionState>()
    private var nextUnitId = 1
    private var nextBuildingId = 1
    private var started = false
    private var gameOver = false
    private var winnerPlayerId = 0

    fun addPlayer(playerId: Int, name: String) {
        if (players.containsKey(playerId)) return

        val player = PlayerState(id = playerId, name = name)
        player.buildings += createStartingTownHall(player, STARTING_TOWN_HALL_ROW_OFFSET)
        player.units += createStartingUnit(player, offset = 0)
        player.units += createStartingUnit(player, offset = 1)
        players[playerId] = player
        connectionStateByPlayer[playerId] = PlayerConnectionState.CONNECTED
        reconnectDeadlineByPlayer.remove(playerId)
        sentChunksByPlayer[playerId] = mutableSetOf()
        visibleUnitsByPlayer[playerId] = mutableSetOf()
        visibleBuildingsByPlayer[playerId] = mutableSetOf()
        lastBuildingLevelSentByPlayer[playerId] = mutableMapOf()
        lastBuildingTrainingSentByPlayer[playerId] = mutableMapOf()
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
        lastUnitHealthSentByPlayer.remove(playerId)
        lastBuildingHealthSentByPlayer.remove(playerId)
        lastBuildingTrainingSentByPlayer.remove(playerId)
        gameOverSentByPlayer.remove(playerId)
        eliminatedPlayerIds.remove(playerId)
    }

    fun isStarted(): Boolean {
        return started
    }

    fun isGameOver(): Boolean {
        return gameOver
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
        lastBuildingTrainingSentByPlayer[playerId] = mutableMapOf()
        visibleTilesByPlayer[playerId] = mutableSetOf()
        lastPresenceSentByPlayer.remove(playerId)
        lastMovingStateSentByPlayer.remove(playerId)
        lastUnitHealthSentByPlayer.remove(playerId)
        lastBuildingHealthSentByPlayer.remove(playerId)
        lastBuildingTrainingSentByPlayer.remove(playerId)
        gameOverSentByPlayer.remove(playerId)
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
        if (gameOver || playerId in eliminatedPlayerIds) return
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
                unit.targetUnitId = 0
                unit.targetBuildingId = 0
            }
    }

    fun handlePlaceBuildingRequest(playerId: Int, requestId: Int, entityType: EntityType, row: Int, col: Int): List<Any> {
        val player = players[playerId] ?: return listOf(PacketPlaceBuildingResult(requestId, false, "Joueur introuvable."))
        if (gameOver) return listOf(PacketPlaceBuildingResult(requestId, false, "Partie terminee."))
        if (player.id in eliminatedPlayerIds) return listOf(PacketPlaceBuildingResult(requestId, false, "Joueur elimine."))
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
            level = 1,
            currentHP = stats.maxHP,
            maxHP = stats.maxHP,
            destroyed = false,
            maxFormedUnits = stats.maxFormedTroops
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
        if (gameOver) return listOf(PacketUpgradeBuildingResult(requestId, false, "Partie terminee.", buildingId))
        if (player.id in eliminatedPlayerIds) return listOf(PacketUpgradeBuildingResult(requestId, false, "Joueur elimine.", buildingId))
        val building = player.buildings.firstOrNull { it.id == buildingId }
            ?: return listOf(PacketUpgradeBuildingResult(requestId, false, "Batiment introuvable.", buildingId))
        val stats = buildingStats(building.entityType)
            ?: return listOf(PacketUpgradeBuildingResult(requestId, false, "Type de batiment invalide.", buildingId))
        if (building.destroyed) {
            return listOf(PacketUpgradeBuildingResult(requestId, false, "Batiment detruit.", buildingId, building.level))
        }
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

    fun handleTrainUnitRequest(playerId: Int, requestId: Int, buildingId: Int, entityType: EntityType): List<Any> {
        val player = players[playerId] ?: return listOf(PacketTrainUnitResult(requestId, false, "Joueur introuvable.", buildingId, entityType))
        if (gameOver) return listOf(PacketTrainUnitResult(requestId, false, "Partie terminee.", buildingId, entityType))
        if (player.id in eliminatedPlayerIds) return listOf(PacketTrainUnitResult(requestId, false, "Joueur elimine.", buildingId, entityType))

        val barracks = player.buildings.firstOrNull { it.id == buildingId && it.entityType == EntityType.BARRACKS }
            ?: return listOf(PacketTrainUnitResult(requestId, false, "Caserne introuvable.", buildingId, entityType))
        if (barracks.destroyed) {
            return listOf(PacketTrainUnitResult(requestId, false, "Caserne detruite.", buildingId, entityType))
        }

        val stats = unitStats(entityType)
            ?: return listOf(PacketTrainUnitResult(requestId, false, "Type d'unite invalide.", buildingId, entityType))
        if (plannedBarracksUnitCount(player, barracks) >= barracks.effectiveBarracksCapacity()) {
            return listOf(PacketTrainUnitResult(requestId, false, "Caserne pleine.", buildingId, entityType))
        }
        if (!spend(player, stats.costGold, stats.costElixir, stats.costDarkElixir)) {
            return listOf(PacketTrainUnitResult(requestId, false, "Ressources insuffisantes.", buildingId, entityType))
        }

        barracks.trainingQueue += entityType
        return listOf(
            PacketTrainUnitResult(requestId, true, "", buildingId, entityType),
            player.resourcesPacket(),
            barracks.toPacket()
        )
    }

    fun update(deltaSeconds: Float) {
        expireReconnectWindows(System.currentTimeMillis())
        if (!started || deltaSeconds <= 0f) return
        if (gameOver) return

        players.values
            .flatMap { it.units }
            .forEach { updateUnitMovement(it, deltaSeconds) }

        updateUnitAttacks(deltaSeconds)
        updateArcherTowerAttacks(deltaSeconds)
        removeDeadUnits()
        updateBarracksTraining(deltaSeconds)
        players.values.forEach { updateResourceProduction(it, deltaSeconds) }
        updateEliminations()
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
        val lastUnitHealth = lastUnitHealthSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        players.values
            .flatMap { it.units }
            .filter { it.id in currentlyVisibleUnits }
            .forEach { unit ->
                val lastMoving = movingStateByUnit[unit.id]
                val lastHealth = lastUnitHealth[unit.id]
                val becameVisible = unit.id !in previouslyVisibleUnits
                val movingStateChanged = lastMoving != null && lastMoving != unit.moving
                val healthChanged = lastHealth == null || lastHealth != unit.currentHP
                if (becameVisible || unit.moving || movingStateChanged || healthChanged) {
                    packets += unit.toPacket()
                    movingStateByUnit[unit.id] = unit.moving
                    lastUnitHealth[unit.id] = unit.currentHP
                }
            }

        movingStateByUnit.keys.removeAll { it !in currentlyVisibleUnits }
        lastUnitHealth.keys.removeAll { it !in currentlyVisibleUnits }

        val currentlyVisibleBuildings = players.values
            .flatMap { it.buildings }
            .filter { building -> building.footprintTouches(visibleTiles) }
            .map { it.id }
            .toMutableSet()

        val previouslyVisibleBuildings = visibleBuildingsByPlayer.getOrPut(playerId) { mutableSetOf() }
        previouslyVisibleBuildings
            .filter { it !in currentlyVisibleBuildings }
            .forEach { packets += PacketBuildingRemove(it) }

        val lastBuildingLevels = lastBuildingLevelSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        val lastBuildingHealth = lastBuildingHealthSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        val lastBuildingTraining = lastBuildingTrainingSentByPlayer.getOrPut(playerId) { mutableMapOf() }
        players.values
            .flatMap { it.buildings }
            .filter { it.id in currentlyVisibleBuildings }
            .forEach { building ->
                val lastSentLevel = lastBuildingLevels[building.id]
                val healthSnapshot = BuildingHealthSnapshot(building.currentHP, building.destroyed)
                val lastSentHealth = lastBuildingHealth[building.id]
                val trainingSnapshot = BuildingTrainingSnapshot(
                    maxFormedUnits = building.maxFormedUnits,
                    trainingQueue = building.trainingQueue.toList(),
                    hasActiveTraining = building.hasActiveTraining,
                    activeTrainingUnitType = building.activeTrainingUnitType,
                    activeTrainingElapsedSeconds = building.activeTrainingElapsedSeconds
                )
                val lastSentTraining = lastBuildingTraining[building.id]
                if (
                    building.id !in previouslyVisibleBuildings ||
                    lastSentLevel != building.level ||
                    lastSentHealth != healthSnapshot ||
                    lastSentTraining != trainingSnapshot
                ) {
                    packets += building.toPacket()
                    lastBuildingHealth[building.id] = healthSnapshot
                    lastBuildingTraining[building.id] = trainingSnapshot
                }
                lastBuildingLevels[building.id] = building.level
            }

        lastBuildingLevels.keys.removeAll { it !in currentlyVisibleBuildings }
        lastBuildingHealth.keys.removeAll { it !in currentlyVisibleBuildings }
        lastBuildingTraining.keys.removeAll { it !in currentlyVisibleBuildings }

        previouslyVisibleUnits.clear()
        previouslyVisibleUnits += currentlyVisibleUnits
        previouslyVisibleBuildings.clear()
        previouslyVisibleBuildings += currentlyVisibleBuildings
        previouslyVisibleTiles.clear()
        previouslyVisibleTiles += visibleTiles

        if (gameOver && playerId !in gameOverSentByPlayer) {
            packets += PacketGameOver(
                winnerPlayerId = winnerPlayerId,
                eliminatedPlayerIds = eliminatedPlayerIds.toIntArray()
            )
            gameOverSentByPlayer += playerId
        }

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

    private fun updateUnitAttacks(deltaSeconds: Float) {
        players.values
            .flatMap { it.units }
            .forEach { unit ->
                if (unit.attackCooldown > 0f) {
                    unit.attackCooldown = (unit.attackCooldown - deltaSeconds).coerceAtLeast(0f)
                }
                updateUnitAttack(unit)
            }

        players.values.forEach { player ->
            player.units.removeAll { it.currentHP <= 0f }
        }
    }

    private fun updateUnitAttack(unit: UnitState) {
        if (unit.currentHP <= 0f) return

        val stats = unitStats(unit.entityType) ?: return
        val rangeTiles = unitAttackRangeTiles(stats)
        val target = currentTargetFor(unit)
            ?.takeIf { target -> target.isAlive && isInRange(unit.row, unit.col, target, rangeTiles) }
            ?: findNearestTarget(unit, rangeTiles)

        if (target == null) {
            clearAttackTarget(unit)
            resumeMovementIfNeeded(unit)
            return
        }

        unit.moving = false
        setAttackTarget(unit, target)

        if (unit.attackCooldown > 0f) {
            return
        }

        target.applyDamage(stats.damage)
        unit.attackCooldown = if (stats.attackSpeed <= 0f) 1f else 1f / stats.attackSpeed

        if (!target.isAlive) {
            clearAttackTarget(unit)
            resumeMovementIfNeeded(unit)
        }
    }

    private fun updateArcherTowerAttacks(deltaSeconds: Float) {
        players.values
            .flatMap { it.buildings }
            .filter { it.entityType == EntityType.ARCHER_TOWER && !it.destroyed && it.currentHP > 0f }
            .forEach { tower ->
                if (tower.attackCooldown > 0f) {
                    tower.attackCooldown = (tower.attackCooldown - deltaSeconds).coerceAtLeast(0f)
                }

                if (tower.attackCooldown > 0f) {
                    return@forEach
                }

                val target = findNearestEnemyUnit(tower, ARCHER_TOWER_RANGE_TILES) ?: return@forEach
                target.currentHP = (target.currentHP - ARCHER_TOWER_DAMAGE).coerceAtLeast(0f)
                if (target.currentHP <= 0f) {
                    target.moving = false
                    target.targetUnitId = 0
                    target.targetBuildingId = 0
                }
                tower.attackCooldown = ARCHER_TOWER_ATTACK_COOLDOWN_SECONDS
            }
    }

    private fun findNearestEnemyUnit(tower: BuildingInstanceState, rangeTiles: Float): UnitState? {
        val rangeSquared = rangeTiles * rangeTiles
        return players.values
            .filter { it.id != tower.ownerPlayerId }
            .flatMap { it.units }
            .filter { it.currentHP > 0f }
            .filter { distanceSquaredToBuildingFootprint(it.row, it.col, tower) <= rangeSquared }
            .minByOrNull { distanceSquaredToBuildingFootprint(it.row, it.col, tower) }
    }

    private fun updateBarracksTraining(deltaSeconds: Float) {
        players.values.forEach { player ->
            player.buildings
                .filter { it.entityType == EntityType.BARRACKS && !it.destroyed && it.currentHP > 0f }
                .forEach { barracks ->
                    if (!barracks.hasActiveTraining && barracks.trainingQueue.isNotEmpty()) {
                        barracks.activeTrainingUnitType = barracks.trainingQueue.removeAt(0)
                        barracks.activeTrainingElapsedSeconds = 0f
                        barracks.hasActiveTraining = true
                    }

                    if (!barracks.hasActiveTraining) return@forEach

                    val stats = unitStats(barracks.activeTrainingUnitType) ?: run {
                        barracks.hasActiveTraining = false
                        barracks.activeTrainingElapsedSeconds = 0f
                        return@forEach
                    }

                    barracks.activeTrainingElapsedSeconds += deltaSeconds
                    if (barracks.activeTrainingElapsedSeconds < stats.trainingTimeSeconds) return@forEach

                    if (formedBarracksUnitCount(player, barracks) >= barracks.effectiveBarracksCapacity()) {
                        barracks.activeTrainingElapsedSeconds = stats.trainingTimeSeconds
                        return@forEach
                    }

                    player.units += createTrainedUnit(player, barracks, barracks.activeTrainingUnitType)
                    barracks.hasActiveTraining = false
                    barracks.activeTrainingElapsedSeconds = 0f
                }
        }
    }

    private fun plannedBarracksUnitCount(player: PlayerState, barracks: BuildingInstanceState): Int {
        val activeCount = if (barracks.hasActiveTraining) 1 else 0
        return formedBarracksUnitCount(player, barracks) + activeCount + barracks.trainingQueue.size
    }

    private fun formedBarracksUnitCount(player: PlayerState, barracks: BuildingInstanceState): Int {
        return player.units.count { it.barracksId == barracks.id && it.currentHP > 0f }
    }

    private fun BuildingInstanceState.effectiveBarracksCapacity(): Int {
        return maxFormedUnits.takeIf { it > 0 } ?: (buildingStats(entityType)?.maxFormedTroops ?: 0)
    }

    private fun createTrainedUnit(player: PlayerState, barracks: BuildingInstanceState, entityType: EntityType): UnitState {
        val stats = unitStats(entityType) ?: UnitStats.BARBARIAN
        val spawn = spawnTileNearBuilding(barracks)
        return UnitState(
            id = nextUnitId++,
            ownerPlayerId = player.id,
            entityType = entityType,
            row = spawn.first.toFloat(),
            col = spawn.second.toFloat(),
            targetRow = spawn.first.toFloat(),
            targetCol = spawn.second.toFloat(),
            moving = false,
            currentHP = stats.maxHP,
            maxHP = stats.maxHP,
            barracksId = barracks.id
        )
    }

    private fun spawnTileNearBuilding(building: BuildingInstanceState): Pair<Int, Int> {
        val size = buildingStats(building.entityType)?.footprintSizeTiles ?: 1
        val offset = size / 2 + 1
        return (building.row + offset).coerceIn(0, worldMap.height - 1) to
            building.col.coerceIn(0, worldMap.width - 1)
    }


    private fun currentTargetFor(unit: UnitState): AttackTarget? {
        if (unit.targetUnitId > 0) {
            players.values
                .flatMap { it.units }
                .firstOrNull { it.id == unit.targetUnitId && it.ownerPlayerId != unit.ownerPlayerId && it.currentHP > 0f }
                ?.let { return AttackTarget.UnitTarget(it) }
        }

        if (unit.targetBuildingId > 0) {
            players.values
                .flatMap { it.buildings }
                .firstOrNull { it.id == unit.targetBuildingId && it.ownerPlayerId != unit.ownerPlayerId && !it.destroyed }
                ?.let { return AttackTarget.BuildingTarget(it) }
        }

        return null
    }

    private fun findNearestTarget(unit: UnitState, rangeTiles: Float): AttackTarget? {
        val enemies = mutableListOf<AttackTarget>()
        players.values
            .filter { it.id != unit.ownerPlayerId }
            .forEach { enemy ->
                enemy.units
                    .filter { it.currentHP > 0f }
                    .mapTo(enemies) { AttackTarget.UnitTarget(it) }
                enemy.buildings
                    .filter { !it.destroyed }
                    .mapTo(enemies) { AttackTarget.BuildingTarget(it) }
            }

        return enemies
            .filter { target -> isInRange(unit.row, unit.col, target, rangeTiles) }
            .minByOrNull { target -> distanceSquaredToTarget(unit.row, unit.col, target) }
    }

    private fun setAttackTarget(unit: UnitState, target: AttackTarget) {
        when (target) {
            is AttackTarget.UnitTarget -> {
                unit.targetUnitId = target.unit.id
                unit.targetBuildingId = 0
            }
            is AttackTarget.BuildingTarget -> {
                unit.targetUnitId = 0
                unit.targetBuildingId = target.building.id
            }
        }
    }

    private fun resumeMovementIfNeeded(unit: UnitState) {
        val dRow = unit.targetRow - unit.row
        val dCol = unit.targetCol - unit.col
        if (sqrt(dRow * dRow + dCol * dCol) > ARRIVAL_THRESHOLD_TILES) {
            unit.moving = true
        }
    }

    private fun clearAttackTarget(unit: UnitState) {
        unit.targetUnitId = 0
        unit.targetBuildingId = 0
    }

    private fun updateResourceProduction(player: PlayerState, deltaSeconds: Float) {
        player.buildings.forEach { building ->
            if (building.destroyed) return@forEach
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

    private fun unitStats(entityType: EntityType): UnitStats? {
        return when (entityType) {
            EntityType.BARBARIAN -> UnitStats.BARBARIAN
            EntityType.ARCHER -> UnitStats.ARCHER
            EntityType.GIANT -> UnitStats.GIANT
            else -> null
        }
    }


    private fun distanceSquaredToTile(sourceRow: Float, sourceCol: Float, tileRow: Int, tileCol: Int): Float {
        val nearestRow = sourceRow.coerceIn(tileRow - 0.5f, tileRow + 0.5f)
        val nearestCol = sourceCol.coerceIn(tileCol - 0.5f, tileCol + 0.5f)
        return distanceSquared(sourceRow, sourceCol, nearestRow, nearestCol)
    }

    private fun distanceSquaredToTarget(sourceRow: Float, sourceCol: Float, target: AttackTarget): Float {
        return when (target) {
            is AttackTarget.UnitTarget -> distanceSquared(sourceRow, sourceCol, target.unit.row, target.unit.col)
            is AttackTarget.BuildingTarget -> distanceSquaredToBuildingFootprint(sourceRow, sourceCol, target.building)
        }
    }

    private fun distanceSquaredToBuildingFootprint(
        sourceRow: Float,
        sourceCol: Float,
        building: BuildingInstanceState
    ): Float {
        val size = buildingStats(building.entityType)?.footprintSizeTiles ?: 1
        return footprint(building.row, building.col, size)
            .minOf { (cellRow, cellCol) ->
                distanceSquaredToTile(sourceRow, sourceCol, cellRow, cellCol)
            }
    }

    private fun isInRange(sourceRow: Float, sourceCol: Float, targetRow: Float, targetCol: Float, rangeTiles: Float): Boolean {
        return distanceSquared(sourceRow, sourceCol, targetRow, targetCol) <= rangeTiles * rangeTiles
    }

    private fun distanceSquared(sourceRow: Float, sourceCol: Float, targetRow: Float, targetCol: Float): Float {
        val dRow = targetRow - sourceRow
        val dCol = targetCol - sourceCol
        return dRow * dRow + dCol * dCol
    }

    private fun createStartingUnit(player: PlayerState, offset: Int): UnitState {
        val spawn = spawnTileFor(players.size, offset)
        val stats = UnitStats.BARBARIAN
        return UnitState(
            id = nextUnitId++,
            ownerPlayerId = player.id,
            entityType = EntityType.BARBARIAN,
            row = spawn.first.toFloat(),
            col = spawn.second.toFloat(),
            targetRow = spawn.first.toFloat(),
            targetCol = spawn.second.toFloat(),
            currentHP = stats.maxHP,
            maxHP = stats.maxHP
        )
    }

    private fun createStartingTownHall(player: PlayerState, rowOffset: Int): BuildingInstanceState {
        val spawn = spawnTileFor(players.size, rowOffset)
        val stats = BuildingStats.TOWN_HALL
        return BuildingInstanceState(
            id = nextBuildingId++,
            ownerPlayerId = player.id,
            entityType = EntityType.TOWN_HALL,
            row = spawn.first,
            col = spawn.second,
            level = 1,
            currentHP = stats.maxHP,
            maxHP = stats.maxHP,
            destroyed = false,
            maxFormedUnits = stats.maxFormedTroops
        )
    }

    private fun unitAttackRangeTiles(stats: UnitStats): Float {
        return stats.range.coerceAtLeast(MIN_TROOP_ATTACK_RANGE_TILES)
    }

    private fun isInRange(sourceRow: Float, sourceCol: Float, target: AttackTarget, rangeTiles: Float): Boolean {
        return when (target) {
            is AttackTarget.UnitTarget -> isInRange(sourceRow, sourceCol, target.unit.row, target.unit.col, rangeTiles)
            is AttackTarget.BuildingTarget -> distanceSquaredToBuildingFootprint(sourceRow, sourceCol, target.building) <= rangeTiles * rangeTiles
        }
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
            addVisibleRadius(visible, centerRow, centerCol, radius, radiusSquared)
        }

        for (building in player.buildings) {
            if (building.destroyed) continue
            val size = buildingStats(building.entityType)?.footprintSizeTiles ?: 1
            footprint(building.row, building.col, size).forEach { (row, col) ->
                addVisibleRadius(visible, row, col, radius, radiusSquared)
            }
        }

        return visible
    }

    private fun addVisibleRadius(
        visible: MutableSet<Int>,
        centerRow: Int,
        centerCol: Int,
        radius: Int,
        radiusSquared: Int
    ) {
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
            moving = moving,
            currentHP = currentHP,
            maxHP = maxHP,
            barracksId = barracksId
        )
    }

    private fun BuildingInstanceState.toPacket(): PacketBuildingSnapshot {
        return PacketBuildingSnapshot(
            buildingId = id,
            ownerPlayerId = ownerPlayerId,
            entityType = entityType,
            row = row,
            col = col,
            level = level,
            currentHP = currentHP,
            maxHP = maxHP,
            destroyed = destroyed,
            maxFormedUnits = maxFormedUnits,
            trainingQueue = ArrayList(trainingQueue),
            hasActiveTraining = hasActiveTraining,
            activeTrainingUnitType = activeTrainingUnitType,
            activeTrainingElapsedSeconds = activeTrainingElapsedSeconds
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

    private fun BuildingInstanceState.footprintTouches(tileIndices: Set<Int>): Boolean {
        val size = buildingStats(entityType)?.footprintSizeTiles ?: 1
        return footprint(row, col, size).any { (cellRow, cellCol) ->
            if (cellRow !in 0 until worldMap.height || cellCol !in 0 until worldMap.width) {
                false
            } else {
                cellRow * worldMap.width + cellCol in tileIndices
            }
        }
    }

    private fun chunkColumns(): Int {
        return (worldMap.width + worldMap.chunkSize - 1) / worldMap.chunkSize
    }

    private sealed class AttackTarget {
        abstract val isAlive: Boolean
        abstract fun applyDamage(damage: Float)

        class UnitTarget(val unit: UnitState) : AttackTarget() {
            override val isAlive: Boolean
                get() = unit.currentHP > 0f

            override fun applyDamage(damage: Float) {
                unit.currentHP = (unit.currentHP - damage).coerceAtLeast(0f)
                if (unit.currentHP <= 0f) {
                    unit.moving = false
                    unit.targetUnitId = 0
                    unit.targetBuildingId = 0
                }
            }
        }

        class BuildingTarget(val building: BuildingInstanceState) : AttackTarget() {
            override val isAlive: Boolean
                get() = !building.destroyed && building.currentHP > 0f

            override fun applyDamage(damage: Float) {
                building.currentHP = (building.currentHP - damage).coerceAtLeast(0f)
                if (building.currentHP <= 0f) {
                    building.destroyed = true
                    building.productionAccumulator = 0f
                }
            }
        }
    }

    private data class BuildingHealthSnapshot(
        val currentHP: Float,
        val destroyed: Boolean
    )

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

    private fun removeDeadUnits() {
        players.values.forEach { player ->
            player.units.removeAll { it.currentHP <= 0f }
        }
    }

    private fun updateEliminations() {
        players.values.forEach { player ->
            if (player.id in eliminatedPlayerIds) return@forEach
            val hasLivingBuilding = player.buildings.any { !it.destroyed && it.currentHP > 0f }
            if (!hasLivingBuilding) {
                eliminatedPlayerIds += player.id
                player.units.clear()
            }
        }

        val remainingPlayers = players.values.filter { it.id !in eliminatedPlayerIds }
        if (players.size > 1 && remainingPlayers.size == 1) {
            gameOver = true
            winnerPlayerId = remainingPlayers.first().id
        }
    }

    private data class BuildingTrainingSnapshot(
        val maxFormedUnits: Int,
        val trainingQueue: List<EntityType>,
        val hasActiveTraining: Boolean,
        val activeTrainingUnitType: EntityType,
        val activeTrainingElapsedSeconds: Float
    )

    companion object {
        private const val UNIT_SPEED_TILES_PER_SECOND = 4f
        private const val ARRIVAL_THRESHOLD_TILES = 0.05f
        private const val MIN_TROOP_ATTACK_RANGE_TILES = 2.5f
        private const val ARCHER_TOWER_RANGE_TILES = 5f
        private const val ARCHER_TOWER_DAMAGE = 20f
        private const val ARCHER_TOWER_ATTACK_COOLDOWN_SECONDS = 1f / 1.5f
        private const val UNKNOWN_TILE = -1
        private const val RECONNECT_GRACE_PERIOD_MS = 3 * 60 * 1000L
        private const val STARTING_TOWN_HALL_ROW_OFFSET = 2
    }
}
