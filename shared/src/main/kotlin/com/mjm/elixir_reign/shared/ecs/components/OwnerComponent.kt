package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

class OwnerComponent(
    var playerId: Int = 0
) : Component
