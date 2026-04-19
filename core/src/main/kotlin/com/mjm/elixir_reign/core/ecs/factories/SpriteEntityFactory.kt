package com.mjm.elixir_reign.core.ecs.factories

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.core.ecs.components.AnimationComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent
import com.mjm.elixir_reign.core.ecs.components.LayerComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.logic.BuildingState

/**
 * Factory ECS pour créer des entités avec sprites
 * Gère la création des components d'animation et rendu
 *
 * Crée aussi bien :
 * - Des unités (avec animation, vie, etc.)
 * - Des bâtiments (statiques, placés sur la grille)
 */
object SpriteEntityFactory {

    /**
     * Crée une unité avec tous les components (animation + rendu)
     */
    fun createUnit(
        entityType: EntityType,
        x: Float,
        y: Float,
        engine: Engine
    ) {
        val stats = getUnitStats(entityType)

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
        entity.add(EntityTypeComponent(entityType))
        entity.add(AnimationComponent(
            currentActionType = ActionType.RUN,
            currentDirectionType = DirectionType.DOWN,
            isAnimating = true
        ))

        // Créer l'animator (UNE FOIS)
        val animator = SpriteAnimationManager.createUnitAnimator(
            stats = stats,
            actionType = ActionType.RUN,
            directionType = DirectionType.DOWN
        )
        entity.add(SpriteAnimatorComponent(animator))

        // Créer le SpriteComponent avec dimensions et offsets depuis l'animator
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

        // Créer la TextureRegion (UNE FOIS) et la stocker dans le component
        val textureRegion = animator.getCurrentTextureRegion()
            ?: throw RuntimeException("Failed to create TextureRegion for $entityType")
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

        // Ajouter l'entité à l'engine
        engine.addEntity(entity)
    }

    fun createBuilding(
        entityType: EntityType,
        x: Float,
        y: Float,
        engine: Engine
    ) {
        // Récupérer les stats du bâtiment
        val stats = getBuildingStats(entityType)

        val entity = Entity()

        // Position dans le monde
        entity.add(PositionComponent(x, y))
        entity.add(HealthComponent(
            currentHP = stats.maxHP - 45, // For test
            maxHP = stats.maxHP
        ))

        // Component du type de bâtiment
        entity.add(EntityTypeComponent(entityType))

        // Créer l'animator (UNE FOIS) - peut être null si pas d'animation JSON
        val animator = SpriteAnimationManager.createBuildingAnimator(
            stats = stats,
            buildingState = BuildingState.MINING
        )
        entity.add(SpriteAnimatorComponent(animator))

        val spriteSheet = animator.spriteSheet
        // Créer le SpriteComponent avec dimensions depuis les stats
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

        // Créer la TextureRegion (UNE FOIS) et la stocker dans le component (si animator exists)
        val textureRegion = animator.getCurrentTextureRegion()
            ?: throw RuntimeException("Failed to create TextureRegion: currentClip is null for $entityType")
        entity.add(TextureRegionComponent(textureRegion))

        // Barre de vie : position et largeur calculées dynamiquement depuis le collider
        entity.add(HealthBarComponent(barHeight = 5f))

        // Couche de rendu (bâtiments au-dessus du terrain)
        entity.add(LayerComponent(layer = 3))
        entity.add(DepthComponent())

        // Sélectionnable (utile pour interactions)
        entity.add(SelectableComponent(isSelected = false))

        // Ajouter l'entité à l'engine
        engine.addEntity(entity)
    }

    /**
     * Récupère les stats d'une unité
     * Type-safe: retourne directement UnitStats
     */
    private fun getUnitStats(entityType: EntityType): UnitStats {
        return when (entityType) {
            EntityType.BARBARIAN -> UnitStats.BARBARIAN
            EntityType.ARCHER -> UnitStats.ARCHER
            EntityType.GIANT -> UnitStats.GIANT
            else -> throw IllegalArgumentException("$entityType n'est pas une unité")
        }
    }

    /**
     * Récupère les stats d'un bâtiment
     * Type-safe: retourne directement BuildingStats
     */
    private fun getBuildingStats(entityType: EntityType): BuildingStats {
        return when (entityType) {
//            EntityType.BARRACKS -> BuildingStats.BARRACKS
//            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP
            else -> throw IllegalArgumentException("$entityType n'est pas un bâtiment")
        }
    }
}


