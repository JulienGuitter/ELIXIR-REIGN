package com.mjm.elixir_reign.shared.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * Component de mouvement
 * Garde une direction discrète pour l'animation et un vecteur continu optionnel
 * pour les déplacements vers une destination précise.
 */
class MovementComponent(
    var directionType: DirectionType = DirectionType.DOWN,
    var speed: Float = 100f,
    var isMoving: Boolean = false,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f
) : Component
