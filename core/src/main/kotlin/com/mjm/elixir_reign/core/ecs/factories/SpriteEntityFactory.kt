package com.mjm.elixir_reign.core.ecs.factories

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.logic.UnitType
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.core.ecs.components.AnimationComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.ecs.components.UnitTypeComponent
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
import com.mjm.elixir_reign.core.ecs.components.LayerComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent

/**
 * Factory ECS-pur pour créer des entités avec sprites
 * Gère toute la création des components d'animation et rendu
 */
object SpriteEntityFactory {

    /**
     * Crée une unité avec tous les components (animation + rendu)
     */
    fun createUnit(
        unitType: UnitType,
        x: Float,
        y: Float,
        engine: Engine
    ) {
        val stats = getUnitStats(unitType)

        val entity = Entity()

        // Components partagés (shared)
        entity.add(PositionComponent(x, y))
        entity.add(MovementComponent(
            directionType = DirectionType.DOWN,
            speed = stats.speed,
            isMoving = false
        ))
        entity.add(HealthComponent(
            currentHP = stats.maxHP - 45, // For test
            maxHP = stats.maxHP
        ))

        // Components client (core) - Animation
        entity.add(UnitTypeComponent(unitType))
        entity.add(AnimationComponent(
            currentActionType = ActionType.RUN,
            currentDirectionType = DirectionType.DOWN,
            isAnimating = true
        ))

        // Créer l'animator (UNE FOIS)
        val animator = SpriteAnimationManager.createAnimator(
            unitType = unitType,
            actionType = ActionType.RUN,
            directionType = DirectionType.DOWN
        )
        entity.add(SpriteAnimatorComponent(animator))

        // Créer le SpriteComponent avec dimensions et offsets depuis l'animator
        val spriteSheet = animator.spriteSheet
        entity.add(SpriteComponent(
            texturePath = SpriteAnimationManager.getTexturePath(unitType),
            width = spriteSheet.cellWidth,
            height = spriteSheet.cellHeight,
            scaleX = 3f,
            scaleY = 3f,
            offsetX = -spriteSheet.footX,
            offsetY = -spriteSheet.footY,
            collider = animator.getCurrentCollider()
        ))

        // Créer la TextureRegion (UNE FOIS) et la stocker dans le component
        val textureRegion = animator.getCurrentTextureRegion()
            ?: throw RuntimeException("Failed to create TextureRegion for $unitType")
        entity.add(TextureRegionComponent(textureRegion))

        // Components de sélection
        entity.add(SelectableComponent(isSelected = false))

        // Barre de vie : position et largeur calculées dynamiquement depuis le collider
        entity.add(HealthBarComponent(barHeight = 5f))

        // Component de profondeur (pour tri automatique par Y-sorting)
        entity.add(DepthComponent())

        // Component de couche (layer 1 = entités principales)
        entity.add(LayerComponent(layer = 1))

        // Ajouter l'entité à l'engine
        engine.addEntity(entity)
    }

    /**
     * Récupère les stats d'une unité
     */
    private fun getUnitStats(unitType: UnitType): UnitStats {
        return when (unitType) {
            UnitType.BARBARIAN -> UnitStats.BARBARIAN
            UnitType.ARCHER -> UnitStats.ARCHER
            UnitType.GIANT -> UnitStats.GIANT
        }
    }
}


