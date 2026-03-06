package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.ActionType

/**
 * Component d'animation (côté client uniquement)
 * Stocke l'état d'animation de l'entité (action + direction)
 * Correspond directement aux noms des clips dans le sprite sheet
 */
class AnimationComponent(
    var currentActionType: ActionType = ActionType.RUN,
    var currentDirectionType: DirectionType = DirectionType.DOWN,
    var isAnimating: Boolean = true
) : Component

