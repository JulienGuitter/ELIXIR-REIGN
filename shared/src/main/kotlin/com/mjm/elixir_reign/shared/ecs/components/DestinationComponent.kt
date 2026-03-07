package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

class DestinationComponent : Component {
    var targetX: Float = 0f
    var targetY: Float = 0f
    var isActive: Boolean = false
}

