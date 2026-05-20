package com.mjm.elixir_reign.core.ecs.factories

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.mjm.elixir_reign.core.ecs.components.AnimationComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent
import com.mjm.elixir_reign.core.ecs.components.LayerComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.ecs.components.BarracksComponent
import com.mjm.elixir_reign.shared.ecs.components.BuildingStateComponent
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.shared.ecs.components.TrainedUnitComponent
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.BuildingState
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.EntityType

/**
 * Factory ECS pour créer des entités avec sprites.
 */
object SpriteEntityFactory {
    private var nextBarracksId = 1

    fun createUnit(
        entityType: EntityType,
        x: Float,
        y: Float,
        engine: Engine,
        barracksId: Int? = null,
        teamId: Int = 0,
        currentHP: Float? = null
    ): Entity {
        val stats = getUnitStats(entityType)
        val entity = Entity()

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

        entity.add(EntityTypeComponent(entityType))
        entity.add(AnimationComponent(
            currentActionType = ActionType.RUN,
            currentDirectionType = DirectionType.DOWN,
            isAnimating = true
        ))

        val animator = SpriteAnimationManager.createUnitAnimator(
            stats = stats,
            actionType = ActionType.RUN,
            directionType = DirectionType.DOWN
        )
        entity.add(SpriteAnimatorComponent(animator))

        val spriteSheet = animator.spriteSheet
        entity.add(SpriteComponent(
            texturePath = stats.texturePath,
            width = spriteSheet.cellWidth,
            height = spriteSheet.cellHeight,
            scaleX = 3f,
            scaleY = 3f,
            offsetX = -spriteSheet.footX,
            offsetY = -spriteSheet.footY,
            collider = animator.getCurrentCollider()
        ))

        val textureRegion = animator.getCurrentTextureRegion()
            ?: throw RuntimeException("Failed to create TextureRegion for $entityType")
        entity.add(TextureRegionComponent(textureRegion))

        entity.add(SelectableComponent(isSelected = false))
        entity.add(DestinationComponent())
        entity.add(HealthBarComponent(barHeight = 5f))
        entity.add(DepthComponent())
        entity.add(LayerComponent(layer = 1))

        if (barracksId != null) {
            entity.add(TrainedUnitComponent(barracksId = barracksId, teamId = teamId))
        }

        engine.addEntity(entity)
        return entity
    }

    fun createBuilding(
        entityType: EntityType,
        x: Float,
        y: Float,
        engine: Engine
    ): Entity {
        val stats = getBuildingStats(entityType)
        val entity = Entity()
        val initialState = if (entityType == EntityType.BARRACKS) {
            BuildingState.IDLE
        } else {
            BuildingState.MINING
        }

        entity.add(PositionComponent(x, y))
        entity.add(HealthComponent(
            currentHP = stats.maxHP,
            maxHP = stats.maxHP
        ))
        entity.add(EntityTypeComponent(entityType))
        entity.add(BuildingStateComponent(initialState))

        if (entityType == EntityType.BARRACKS) {
            entity.add(BarracksComponent(
                barracksId = nextBarracksId++,
                teamId = 0,
                maxFormedUnits = stats.maxFormedTroops
            ))
        }

        val animator = SpriteAnimationManager.createBuildingAnimator(
            stats = stats,
            buildingState = initialState
        )
        entity.add(SpriteAnimatorComponent(animator))

        val spriteSheet = animator.spriteSheet
        entity.add(SpriteComponent(
            texturePath = stats.texturePath,
            width = spriteSheet.cellWidth,
            height = spriteSheet.cellHeight,
            scaleX = 2.5f,
            scaleY = 2.5f,
            offsetX = -spriteSheet.footX,
            offsetY = -spriteSheet.footY,
            collider = animator.getCurrentCollider()
        ))

        val textureRegion = animator.getCurrentTextureRegion()
            ?: throw RuntimeException("Failed to create TextureRegion: currentClip is null for $entityType")
        entity.add(TextureRegionComponent(textureRegion))

        entity.add(HealthBarComponent(barHeight = 5f))
        entity.add(LayerComponent(layer = 3))
        entity.add(DepthComponent())
        entity.add(SelectableComponent(isSelected = false))

        engine.addEntity(entity)
        return entity
    }

    fun getUnitStats(entityType: EntityType): UnitStats {
        return when (entityType) {
            EntityType.BARBARIAN -> UnitStats.BARBARIAN
            EntityType.ARCHER -> UnitStats.ARCHER
            EntityType.GIANT -> UnitStats.GIANT
            else -> throw IllegalArgumentException("$entityType n'est pas une unite")
        }
    }

    fun getBuildingStats(entityType: EntityType): BuildingStats {
        return when (entityType) {
            EntityType.BARRACKS -> BuildingStats.BARRACKS
            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP
            EntityType.GOLD_MINE -> BuildingStats.GOLD_MINE
            EntityType.ARCHER_TOWER -> BuildingStats.ARCHER_TOWER
            EntityType.TOWN_HALL -> BuildingStats.TOWN_HALL
            else -> throw IllegalArgumentException("$entityType n'est pas un batiment")
        }
    }
}
