package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

class SpriteComponent(
    var texturePath: String = "",
    var width: Int = 32,
    var height: Int = 32,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f
) : Component
