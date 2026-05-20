package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimator
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.BuildingState
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * Component ECS qui stocke l'instance persistante de SpriteAnimator
 * Compatible avec Units ET Buildings
 *
 * Pour UNITS (direction + action):
 * - Tracker lastActionType et lastDirectionType
 * - buildingState reste null
 *
 * Pour BUILDINGS (state):
 * - Tracker lastBuildingState
 * - lastActionType et lastDirectionType restent null
 *
 * L'animator est créé UNE FOIS lors de la création de l'entité
 * et est mis à jour par AnimationSystem à chaque frame
 */
class SpriteAnimatorComponent(
    var spriteAnimator: SpriteAnimator,
    // Tracking pour UNITS: détecte les changements d'action/direction
    var lastActionType: ActionType? = spriteAnimator.currentAction,
    var lastDirectionType: DirectionType? = spriteAnimator.currentDirection,
    // Tracking pour BUILDINGS: détecte les changements de state
    var lastBuildingState: BuildingState? = null,
    var lastBuildingLevel: Int = 1
) : Component
