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
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
import com.mjm.elixir_reign.core.ecs.components.LayerComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent
import com.mjm.elixir_reign.core.tools.sprites.BuildingTextureManager
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.ecs.components.BarracksComponent
import com.mjm.elixir_reign.shared.ecs.components.BuildingTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.TrainedUnitComponent
import com.mjm.elixir_reign.shared.logic.BuildingType
import com.badlogic.gdx.graphics.g2d.TextureRegion

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
        engine: Engine,
        barracksId: Int? = null,
        teamId: Int = 0,
        currentHP: Float? = null
    ): Entity {
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
            currentHP = currentHP ?: stats.maxHP,
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
        entity.add(DestinationComponent())

        // Barre de vie : position et largeur calculées dynamiquement depuis le collider
        entity.add(HealthBarComponent(barHeight = 5f))

        // Component de profondeur (pour tri automatique par Y-sorting)
        entity.add(DepthComponent())

        // Component de couche (layer 1 = entités principales)
        entity.add(LayerComponent(layer = 1))

        if (barracksId != null) {
            entity.add(TrainedUnitComponent(barracksId = barracksId, teamId = teamId))
        }

        // Ajouter l'entité à l'engine
        engine.addEntity(entity)
        return entity
    }

    fun createBarracks(
        x: Float,
        y: Float,
        barracksId: Int,
        engine: Engine,
        teamId: Int = 0
    ): Entity {
        val stats = BuildingStats.BARRACKS
        val entity = Entity()

        entity.add(PositionComponent(x, y))
        entity.add(HealthComponent(currentHP = stats.maxHP, maxHP = stats.maxHP))
        entity.add(SelectableComponent(isSelected = false))
        entity.add(BuildingTypeComponent(BuildingType.BARRACKS))
        entity.add(BarracksComponent(
            barracksId = barracksId,
            teamId = teamId,
            maxFormedUnits = stats.maxFormedTroops
        ))
        entity.add(SpriteComponent(
            texturePath = stats.texturePath,
            width = stats.width,
            height = stats.height,
            scaleX = 1.8f,
            scaleY = 1.8f,
            offsetX = -0.5f,
            offsetY = -0.35f
        ))
        entity.add(TextureRegionComponent(TextureRegion(BuildingTextureManager.getBarracksTexture())))
        entity.add(HealthBarComponent(barHeight = 5f))
        entity.add(DepthComponent())
        entity.add(LayerComponent(layer = 1))

        engine.addEntity(entity)
        return entity
    }

    /**
     * Récupère les stats d'une unité
     */
    fun getUnitStats(unitType: UnitType): UnitStats {
        return when (unitType) {
            UnitType.BARBARIAN -> UnitStats.BARBARIAN
            UnitType.ARCHER -> UnitStats.ARCHER
            UnitType.GIANT -> UnitStats.GIANT
        }
    }
}

