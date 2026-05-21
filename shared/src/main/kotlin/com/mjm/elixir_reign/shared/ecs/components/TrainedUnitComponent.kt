package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component

class TrainedUnitComponent(
    var barracksId: Int,
    var teamId: Int = 0
) : Component
