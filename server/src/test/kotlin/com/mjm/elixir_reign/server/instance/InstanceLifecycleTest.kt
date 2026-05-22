package com.mjm.elixir_reign.server.instance

import com.mjm.elixir_reign.server.game.GameState
import com.mjm.elixir_reign.shared.network.Client
import com.mjm.elixir_reign.shared.type.GameType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstanceLifecycleTest {
    @Test
    fun `started instance stops after every player misses reconnect window`() {
        val instance = Instance()
        instance.start(GameType.G1V1)
        instance.addPlayer(1, Client(pseudo = "Alice", gameType = GameType.G1V1))
        instance.addPlayer(2, Client(pseudo = "Bob", gameType = GameType.G1V1))

        assertTrue(instance.active)

        instance.removePlayer(1)
        instance.removePlayer(2)
        instance.expireReconnectDeadlines()

        instance.update(0.05f)

        assertFalse(instance.active)
    }

    @Suppress("UNCHECKED_CAST")
    private fun Instance.expireReconnectDeadlines() {
        val stateField = Instance::class.java.getDeclaredField("gameState")
        stateField.isAccessible = true
        val state = stateField.get(this) as GameState

        val deadlinesField = GameState::class.java.getDeclaredField("reconnectDeadlineByPlayer")
        deadlinesField.isAccessible = true
        val deadlines = deadlinesField.get(state) as MutableMap<Int, Long>
        deadlines.keys.toList().forEach { playerId ->
            deadlines[playerId] = 0L
        }
    }
}
