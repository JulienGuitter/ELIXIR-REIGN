package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.EntityType

class BarracksComponent(
    var barracksId: Int,
    var teamId: Int = 0,
    var maxFormedUnits: Int = 6,
    val trainingQueue: MutableList<EntityType> = mutableListOf(),
    val readyToSpawn: MutableList<EntityType> = mutableListOf(),
    var activeTraining: BarracksTrainingProgress? = null
) : Component

class BarracksTrainingProgress(
    var unitType: EntityType,
    var elapsedSeconds: Float = 0f
)
