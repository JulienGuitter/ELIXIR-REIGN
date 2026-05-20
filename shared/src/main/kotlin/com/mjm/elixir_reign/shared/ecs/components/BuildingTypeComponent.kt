package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.BuildingType

class BuildingTypeComponent(
    var buildingType: BuildingType
) : Component
