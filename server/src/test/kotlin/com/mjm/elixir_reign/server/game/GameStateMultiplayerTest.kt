package com.mjm.elixir_reign.server.game

import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.game.PlayerState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketGameOver
import com.mjm.elixir_reign.shared.network.PacketPlaceBuildingResult
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketTrainUnitResult
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.network.PacketUpgradeBuildingResult
import com.mjm.elixir_reign.shared.terrain.TerrainMaterial
import com.mjm.elixir_reign.shared.type.GameType
import com.mjm.elixir_reign.shared.world.WorldMap
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GameStateMultiplayerTest {
    @Test
    fun `building placement is authoritative and returns resource and building packets`() {
        val state = startedState()
        val player = state.player(1)
        player.gold = BuildingStats.BARRACKS.costGold
        val beforeGold = player.gold
        val (row, col) = state.findValidPlacement(player, EntityType.BARRACKS, BuildingStats.BARRACKS)

        val packets = state.handlePlaceBuildingRequest(1, 10, EntityType.BARRACKS, row, col)

        val result = packets.singleOfType<PacketPlaceBuildingResult>()
        assertTrue(result.accepted, result.reason)
        assertEquals(EntityType.BARRACKS, player.buildings.single { it.id == result.buildingId }.entityType)
        assertEquals(beforeGold - BuildingStats.BARRACKS.costGold, packets.singleOfType<PacketPlayerResources>().gold)
        assertEquals(result.buildingId, packets.singleOfType<PacketBuildingSnapshot>().buildingId)
    }

    @Test
    fun `resource building placed on matching resource produces for owner`() {
        val state = startedState()
        val player = state.player(1)
        val (row, col) = state.findValidPlacement(
            player = player,
            entityType = EntityType.GOLD_MINE,
            stats = BuildingStats.GOLD_MINE,
            requiredMaterial = TerrainMaterial.GOLD
        )
        val beforeGold = player.gold

        val placement = state.handlePlaceBuildingRequest(1, 11, EntityType.GOLD_MINE, row, col)
            .singleOfType<PacketPlaceBuildingResult>()
        assertTrue(placement.accepted, placement.reason)

        state.update(3f)

        assertTrue(player.gold > beforeGold, "Gold mine should produce gold after being placed on gold.")
    }

    @Test
    fun `barracks queues trains and syncs formed troops`() {
        val state = startedState()
        val player = state.player(1)
        player.gold = BuildingStats.BARRACKS.costGold + UnitStats.ARCHER.costGold
        player.elixir = UnitStats.ARCHER.costElixir
        val (row, col) = state.findValidPlacement(player, EntityType.BARRACKS, BuildingStats.BARRACKS)
        val barracksId = state.handlePlaceBuildingRequest(1, 12, EntityType.BARRACKS, row, col)
            .singleOfType<PacketPlaceBuildingResult>()
            .also { assertTrue(it.accepted, it.reason) }
            .buildingId

        val trainResult = state.handleTrainUnitRequest(1, 13, barracksId, EntityType.ARCHER)
            .singleOfType<PacketTrainUnitResult>()
        assertTrue(trainResult.accepted, trainResult.reason)
        assertEquals(0, player.gold)
        assertEquals(0, player.elixir)

        state.update(UnitStats.ARCHER.trainingTimeSeconds)

        assertTrue(player.units.any { it.barracksId == barracksId && it.entityType == EntityType.ARCHER })
        assertTrue(
            state.syncPacketsFor(1).filterIsInstance<PacketUnitSnapshot>()
                .any { it.barracksId == barracksId && it.entityType == EntityType.ARCHER }
        )
    }

    @Test
    fun `building upgrade spends resources and syncs upgraded level`() {
        val state = startedState()
        val player = state.player(1)
        player.gold = BuildingStats.TOWN_HALL.costGold * 2
        val beforeGold = player.gold
        val townHallId = player.buildings.first { it.entityType == EntityType.TOWN_HALL }.id

        val packets = state.handleUpgradeBuildingRequest(1, 14, townHallId)

        val result = packets.singleOfType<PacketUpgradeBuildingResult>()
        assertTrue(result.accepted, result.reason)
        assertEquals(2, result.level)
        assertEquals(beforeGold - BuildingStats.TOWN_HALL.costGold * 2, packets.singleOfType<PacketPlayerResources>().gold)
        assertEquals(2, packets.singleOfType<PacketBuildingSnapshot>().level)
    }

    @Test
    fun `enemy troops damage and remove each other`() {
        val state = startedState()
        val attacker = state.player(1).units.first()
        val defenderPlayer = state.player(2)
        val defender = defenderPlayer.units.first()
        defenderPlayer.units.retainAll(listOf(defender))

        attacker.row = 20f
        attacker.col = 20f
        defender.row = 20f
        defender.col = 20f
        defender.currentHP = 5f

        state.update(1f)

        assertFalse(defenderPlayer.units.any { it.id == defender.id })
    }

    @Test
    fun `troop destroys last enemy building then game over is sent`() {
        val state = startedState()
        val attackerPlayer = state.player(1)
        val defenderPlayer = state.player(2)
        val attacker = attackerPlayer.units.first()
        val targetBuilding = defenderPlayer.buildings.first()
        defenderPlayer.units.clear()
        defenderPlayer.buildings.retainAll(listOf(targetBuilding))

        attacker.row = targetBuilding.row.toFloat()
        attacker.col = targetBuilding.col.toFloat()
        targetBuilding.currentHP = 5f

        state.update(1f)

        assertTrue(targetBuilding.destroyed)
        val gameOver = state.syncPacketsFor(1).singleOfType<PacketGameOver>()
        assertEquals(1, gameOver.winnerPlayerId)
        assertTrue(2 in gameOver.eliminatedPlayerIds.toSet())
    }

    @Test
    fun `archer tower attacks enemy troops at range`() {
        val state = startedState()
        val tower = state.player(1).buildings.first()
        val enemyPlayer = state.player(2)
        val enemy = enemyPlayer.units.first()
        enemyPlayer.units.retainAll(listOf(enemy))

        tower.entityType = EntityType.ARCHER_TOWER
        tower.currentHP = BuildingStats.ARCHER_TOWER.maxHP
        tower.maxHP = BuildingStats.ARCHER_TOWER.maxHP
        enemy.row = tower.row.toFloat()
        enemy.col = tower.col.toFloat()
        enemy.currentHP = 10f

        state.update(1f)

        assertFalse(enemyPlayer.units.any { it.id == enemy.id })
    }

    private fun startedState(): GameState {
        return GameState(GameType.G1V1).apply {
            addPlayer(1, "Alice")
            addPlayer(2, "Bob")
            markStarted()
        }
    }

    private fun GameState.findValidPlacement(
        player: PlayerState,
        entityType: EntityType,
        stats: BuildingStats,
        requiredMaterial: TerrainMaterial? = null
    ): Pair<Int, Int> {
        val map = worldMap()
        for (row in 0 until map.height) {
            for (col in 0 until map.width) {
                if (requiredMaterial != null && footprint(row, col, stats.footprintSizeTiles).none { (r, c) ->
                        map[r, c]?.material == requiredMaterial
                    }
                ) {
                    continue
                }
                player.units.first().apply {
                    this.row = row.toFloat()
                    this.col = col.toFloat()
                }
                if (validatePlacement(player, entityType, stats, row, col) == null) {
                    return row to col
                }
            }
        }
        error("No valid placement found for $entityType.")
    }

    private fun GameState.validatePlacement(
        player: PlayerState,
        entityType: EntityType,
        stats: BuildingStats,
        row: Int,
        col: Int
    ): String? {
        val method = GameState::class.java.getDeclaredMethod(
            "validateBuildingPlacement",
            PlayerState::class.java,
            EntityType::class.java,
            BuildingStats::class.java,
            Integer.TYPE,
            Integer.TYPE
        )
        method.isAccessible = true
        return method.invoke(this, player, entityType, stats, row, col) as String?
    }

    private fun GameState.player(id: Int): PlayerState {
        return players().getValue(id)
    }

    @Suppress("UNCHECKED_CAST")
    private fun GameState.players(): LinkedHashMap<Int, PlayerState> {
        val field = GameState::class.java.getDeclaredField("players")
        field.isAccessible = true
        return field.get(this) as LinkedHashMap<Int, PlayerState>
    }

    private fun GameState.worldMap(): WorldMap {
        val field = GameState::class.java.getDeclaredField("worldMap")
        field.isAccessible = true
        return field.get(this) as WorldMap
    }

    private fun footprint(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
        val normalizedSize = size.coerceAtLeast(1)
        val startRow = centerRow - normalizedSize / 2
        val startCol = centerCol - normalizedSize / 2
        return buildList {
            for (row in startRow until startRow + normalizedSize) {
                for (col in startCol until startCol + normalizedSize) {
                    add(row to col)
                }
            }
        }
    }

    private inline fun <reified T> List<Any>.singleOfType(): T {
        return filterIsInstance<T>().singleOrNull()
            ?: error("Expected one packet of type ${T::class.simpleName}, got ${map { it::class.simpleName }}")
    }
}
