package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * Component de mouvement
 * Utilise une Direction discrète (8 directions) pour optimisation
 * plutôt que des vecteurs continus
 */
class MovementComponent(
    var directionType: DirectionType = DirectionType.DOWN,
    var speed: Float = 100f,
    var isMoving: Boolean = false
) : Component

