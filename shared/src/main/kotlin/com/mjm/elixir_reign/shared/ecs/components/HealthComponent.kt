package com.mjm.elixir_reign.shared.ecs.components


import com.badlogic.ashley.core.Component

class HealthComponent(
    var currentHP: Float = 100f,
    var maxHP: Float = 100f
) : Component
