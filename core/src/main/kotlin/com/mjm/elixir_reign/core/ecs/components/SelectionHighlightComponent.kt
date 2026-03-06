package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

class SelectionHighlightComponent(
    var borderColor: Int = 0xFFFFFFFF.toInt(), // Blanc
    var borderWidth: Float = 2f,
    var glowIntensity: Float = 1.5f
) : Component

