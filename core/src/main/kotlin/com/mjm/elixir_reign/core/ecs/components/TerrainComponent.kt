package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component

class TerrainComponent(
    var clipName: String = "",
    var gridColumns: Int = 1,
    var gridRows: Int = 1,
    var margin: Float = 0.25f
) : Component
