package com.mjm.elixir_reign.core.tools.sprites.mapper

import com.mjm.elixir_reign.shared.logic.BuildingState

/**
 * StateMapper mappe les états d'un bâtiment à leurs suffixes de clip d'animation
 * Utilisé par SpriteAnimator pour les bâtiments (qui n'ont pas direction/action)
 */
class StateMapper {
    fun getStateInfo(buildingState: BuildingState): String {
        return when (buildingState) {
            BuildingState.IDLE -> "_idle"
            BuildingState.TRAINING_UNIT -> "_training"
            BuildingState.MINING -> "_lvl1"
            BuildingState.DESTROYED -> "_destroyed"
        }
    }

    fun buildClipName(baseClipName: String, buildingState: BuildingState): String {
        val suffix = getStateInfo(buildingState)
        return baseClipName + suffix
    }
}


