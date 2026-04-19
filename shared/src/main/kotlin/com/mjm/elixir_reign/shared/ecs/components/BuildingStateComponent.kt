package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.BuildingState

/**
 * BuildingStateComponent : Représente l'état actuel d'un bâtiment
 *
 * Utilisé pour gérer les animations et les actions du bâtiment
 * (IDLE, TRAINING_UNIT, MINING, UPGRADING, DESTROYED)
 *
 * @param state L'état actuel du bâtiment
 */
class BuildingStateComponent(
    var state: BuildingState = BuildingState.IDLE
) : Component

