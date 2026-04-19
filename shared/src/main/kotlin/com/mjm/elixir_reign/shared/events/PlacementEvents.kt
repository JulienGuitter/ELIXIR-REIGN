package com.mjm.elixir_reign.shared.events

import com.mjm.elixir_reign.shared.ecs.systems.PlacementSystem

data class PlacementRequestEvent(
    val row: Int,
    val col: Int,
    val building: PlacementSystem.BuildingToPlace,
    var accepted: Boolean = false
)

