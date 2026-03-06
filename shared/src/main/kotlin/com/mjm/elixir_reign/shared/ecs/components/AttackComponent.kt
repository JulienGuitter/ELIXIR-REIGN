package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

class AttackComponent(
    var damage: Float = 10f,
    var attackSpeed: Float = 1f,
    var range: Float = 50f,
    var attackCooldown: Float = 0f
) : Component
