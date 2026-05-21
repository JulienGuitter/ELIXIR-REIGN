package com.mjm.elixir_reign.core.session

import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.network.PacketBuildingSnapshot
import com.mjm.elixir_reign.shared.network.PacketGameInit
import com.mjm.elixir_reign.shared.network.PacketGameOver
import com.mjm.elixir_reign.shared.network.PacketPlayerResources
import com.mjm.elixir_reign.shared.network.PacketPlayerSummary
import com.mjm.elixir_reign.shared.network.PacketUnitSnapshot
import com.mjm.elixir_reign.shared.type.GameType
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameSessionPacketSyncTest {
    @BeforeTest
    fun resetSession() {
        GameSession.startMultiplayer(GameType.G1V1)
    }

    @Test
    fun `client session applies authoritative resources buildings units and game over packets`() {
        GameSession.applyGameInit(
            PacketGameInit(
                myPlayerId = 1,
                mapWidth = 32,
                mapHeight = 32,
                chunkSize = 16,
                players = arrayListOf(
                    PacketPlayerSummary(id = 1, name = "Alice", gold = 1200, elixir = 1200, darkElixir = 80),
                    PacketPlayerSummary(id = 2, name = "Bob", gold = 1200, elixir = 1200, darkElixir = 80)
                )
            )
        )
        GameSession.applyPlayerResources(PacketPlayerResources(gold = 900, elixir = 700, darkElixir = 60))
        GameSession.applyBuildingSnapshot(
            PacketBuildingSnapshot(
                buildingId = 42,
                ownerPlayerId = 1,
                entityType = EntityType.BARRACKS,
                row = 8,
                col = 9,
                level = 2,
                maxFormedUnits = 6,
                trainingQueue = arrayListOf(EntityType.ARCHER),
                hasActiveTraining = true,
                activeTrainingUnitType = EntityType.BARBARIAN,
                activeTrainingElapsedSeconds = 1.5f
            )
        )
        GameSession.applyUnitSnapshot(
            PacketUnitSnapshot(
                unitId = 77,
                ownerPlayerId = 1,
                entityType = EntityType.ARCHER,
                row = 10f,
                col = 11f,
                currentHP = 25f,
                maxHP = 50f,
                barracksId = 42
            )
        )
        GameSession.applyGameOver(PacketGameOver(winnerPlayerId = 1, eliminatedPlayerIds = intArrayOf(2)))

        assertEquals(900, GameSession.gold)
        assertEquals(700, GameSession.elixir)
        assertEquals(60, GameSession.darkElixir)
        assertEquals(2, GameSession.buildingSnapshots().single { it.id == 42 }.level)
        assertEquals(listOf(EntityType.ARCHER), GameSession.buildingSnapshots().single { it.id == 42 }.trainingQueue)
        assertEquals(42, GameSession.unitSnapshots().single { it.id == 77 }.barracksId)
        assertTrue(GameSession.gameOver)
        assertEquals(1, GameSession.winnerPlayerId)
        assertTrue(2 in GameSession.eliminatedPlayerIds)
    }
}
