package com.mjm.elixir_reign.core.ecs.components

import com.badlogic.ashley.core.Component
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimator
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.DirectionType

/**
 * Component ECS qui stocke l'instance persistante de SpriteAnimator
 * Responsable de maintenir l'état d'animation entre les frames
 *
 * L'animator est créé UNE FOIS lors de la création de l'entité
 * et est mis à jour par AnimationSystem à chaque frame
 */
class SpriteAnimatorComponent(
    var spriteAnimator: SpriteAnimator,
    // Tracking pour détecter les changements d'action/direction
    var lastActionType: ActionType = spriteAnimator.currentAction,
    var lastDirectionType: DirectionType = spriteAnimator.currentDirection
) : Component



