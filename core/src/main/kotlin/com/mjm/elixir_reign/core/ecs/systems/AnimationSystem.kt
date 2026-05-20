package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.core.ecs.components.AnimationComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent

/**
 * AnimationSystem côté client (ECS-pur)
 * Responsable de:
 * 1. Détecter les changements d'action/direction
 * 2. Mettre à jour le clip si changement détecté
 * 3. Mettre à jour l'animator (deltaTime)
 * 4. Mettre à jour le TextureRegionComponent avec la frame actuelle
 *
 * Components requis:
 * - AnimationComponent (état: action, direction)
 * - SpriteAnimatorComponent (animator persistant)
 * - TextureRegionComponent (texture à afficher)
 * - EntityTypeComponent (type d'entité pour baseClipName)
 * - MovementComponent (pour synchro direction)
 */
class AnimationSystem : IteratingSystem(
    Family.all(
        SpriteAnimatorComponent::class.java,
        TextureRegionComponent::class.java,
        EntityTypeComponent::class.java
    ).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val spriteAnimatorComp = entity.getComponent(SpriteAnimatorComponent::class.java)
        val textureRegionComp = entity.getComponent(TextureRegionComponent::class.java)
        val animationComp = entity.getComponent(AnimationComponent::class.java)
        val movementComp = entity.getComponent(MovementComponent::class.java)
        val entityTypeComp = entity.getComponent(EntityTypeComponent::class.java)
        val spriteComp = entity.getComponent(SpriteComponent::class.java)

        val spriteAnimator = spriteAnimatorComp.spriteAnimator

        val isUnit = animationComp != null && movementComp != null

        if (isUnit) {
            val unitAnimation = animationComp
            val unitMovement = movementComp

            // Synchro de la direction d'animation avec le mouvement
            unitAnimation.currentDirectionType = unitMovement.directionType

            // Vérifier si l'action ou la direction a changé
            val actionChanged = unitAnimation.currentActionType != spriteAnimatorComp.lastActionType
            val directionChanged = unitAnimation.currentDirectionType != spriteAnimatorComp.lastDirectionType

            if (actionChanged || directionChanged) {
                val baseClipName = SpriteAnimationManager.getBaseClipName(entityTypeComp.entityType)
                spriteAnimator.setUnitClip(
                    baseClipName = baseClipName,
                    directionType = unitAnimation.currentDirectionType,
                    actionType = unitAnimation.currentActionType
                )

                spriteAnimatorComp.lastActionType = unitAnimation.currentActionType
                spriteAnimatorComp.lastDirectionType = unitAnimation.currentDirectionType
                spriteComp?.collider = spriteAnimator.getCurrentCollider()
            }

            if (!unitAnimation.isAnimating) {
                return
            }
        }

        // Units animées + bâtiments (toujours animés si clip multi-frames)
        spriteAnimator.update(deltaTime)
        spriteAnimator.getCurrentTextureRegion()?.let { newRegion ->
            textureRegionComp.textureRegion = newRegion
        }
    }
}
