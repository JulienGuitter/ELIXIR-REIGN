package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.mjm.elixir_reign.core.ecs.components.AnimationComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.ecs.components.UnitTypeComponent
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
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
 * - UnitTypeComponent (type d'unité pour baseClipName)
 */
class AnimationSystem : IteratingSystem(
    Family.all(
        AnimationComponent::class.java,
        SpriteAnimatorComponent::class.java,
        TextureRegionComponent::class.java,
        UnitTypeComponent::class.java,
        MovementComponent::class.java
    ).get()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val animationComp = entity.getComponent(AnimationComponent::class.java)
        val spriteAnimatorComp = entity.getComponent(SpriteAnimatorComponent::class.java)
        val textureRegionComp = entity.getComponent(TextureRegionComponent::class.java)
        val unitTypeComp = entity.getComponent(UnitTypeComponent::class.java)
        val spriteComp = entity.getComponent(SpriteComponent::class.java)
        val movementComp = entity.getComponent(MovementComponent::class.java)

        val spriteAnimator = spriteAnimatorComp.spriteAnimator

        // Synchro de la direction d'animation avec le mouvement
        // Le personnage regarde toujours dans la direction où il se déplace
        animationComp.currentDirectionType = movementComp.directionType

        // Vérifier si l'action ou la direction a changé
        val actionChanged = animationComp.currentActionType != spriteAnimatorComp.lastActionType
        val directionChanged = animationComp.currentDirectionType != spriteAnimatorComp.lastDirectionType

        // Si changement détecté, mettre à jour le clip
        if (actionChanged || directionChanged) {
            val baseClipName = SpriteAnimationManager.getBaseClipName(unitTypeComp.unitType)
            spriteAnimator.setClip(
                baseClipName = baseClipName,
                directionType = animationComp.currentDirectionType,
                actionType = animationComp.currentActionType
            )

            // Tracker les derniers changements
            spriteAnimatorComp.lastActionType = animationComp.currentActionType
            spriteAnimatorComp.lastDirectionType = animationComp.currentDirectionType

            // Mettre à jour le collider du SpriteComponent avec celui du nouveau clip
            spriteComp?.collider = spriteAnimator.getCurrentCollider()
        }

        // Mettre à jour l'animation (l'état persiste entre les frames!)
        if (animationComp.isAnimating) {
            spriteAnimator.update(deltaTime)

            // Mettre à jour le TextureRegionComponent avec la frame actuelle
            spriteAnimator.getCurrentTextureRegion()?.let { newRegion ->
                textureRegionComp.textureRegion = newRegion
            }
        }
    }
}
