package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

class GridPlacementComponent(
    var row: Int,
    var col: Int,
    var footprintSizeTiles: Int = 1
) : Component {
    val frontDepth: Int
        get() = row + col + (footprintSizeTiles.coerceAtLeast(1) - 1)
}
