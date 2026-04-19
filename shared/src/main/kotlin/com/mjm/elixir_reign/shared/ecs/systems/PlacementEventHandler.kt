package com.mjm.elixir_reign.shared.ecs.systems

import com.mjm.elixir_reign.shared.events.EventBus
import com.mjm.elixir_reign.shared.events.PlacementRequestEvent

class PlacementEventHandler(
    private val eventBus: EventBus,
    private val placementSystem: PlacementSystem
) {
    private val unsubscribe = eventBus.subscribe<PlacementRequestEvent> { event ->
        event.accepted = placementSystem.place(event.row, event.col, event.building)
    }

    fun dispose() {
        unsubscribe()
    }
}

