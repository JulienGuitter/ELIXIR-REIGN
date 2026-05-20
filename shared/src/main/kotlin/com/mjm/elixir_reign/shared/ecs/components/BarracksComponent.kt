package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.UnitType

class BarracksComponent(
    var barracksId: Int,
    var teamId: Int = 0,
    var maxFormedUnits: Int = 6,
    val trainingQueue: MutableList<UnitType> = mutableListOf(),
    val readyToSpawn: MutableList<UnitType> = mutableListOf(),
    var activeTraining: BarracksTrainingProgress? = null
) : Component

class BarracksTrainingProgress(
    var unitType: UnitType,
    var elapsedSeconds: Float = 0f
)
