package com.mjm.elixir_reign.core.tools.sprites

import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.data.UnitStats
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.logic.BuildingState

/**
 * Manager centralisé pour tous les SpriteSheets et création d'animateurs
 * Récupère les métadonnées directement depuis les stats objects
 * Charge les sprite sheets une seule fois et les cache
 */
object SpriteAnimationManager {
    // Caches séparés pour units et buildings (clés: String = stats.name)
    private val unitSpriteSheetCache = mutableMapOf<String, SpriteSheet>()
    private val buildingSpriteSheetCache = mutableMapOf<String, SpriteSheet>()

    /** True once preloadAll() has completed */
    var isReady: Boolean = false
        private set

    /**
     * Crée un SpriteAnimator pour une unité avec une action et direction
     * Récupère les métadonnées depuis UnitStats
     */
    fun createUnitAnimator(
        stats: UnitStats,
        actionType: ActionType,
        directionType: DirectionType
    ): SpriteAnimator {
        val spriteSheet = getUnitSpriteSheet(stats)

        return SpriteAnimator(
            spriteSheet = spriteSheet,
            texturePath = stats.texturePath,
            baseClipName = stats.spriteBaseClipName,
            directionType = directionType,
            actionType = actionType
        )
    }

    /**
     * Récupère un SpriteAnimator pour un bâtiment
     * Récupère les métadonnées depuis BuildingStats
     * Les bâtiments peuvent ne pas avoir de JSON d'animation
     */
    fun createBuildingAnimator(
        stats: BuildingStats,
        buildingState: BuildingState
    ): SpriteAnimator {
        val spriteSheet = getBuildingSpriteSheet(stats)

        // Pour les bâtiments: pas de direction/action, passer null
        return SpriteAnimator(
            spriteSheet = spriteSheet,
            texturePath = stats.texturePath,
            baseClipName = stats.spriteBaseClipName,
            directionType = null,
            actionType = null,
            buildingState = buildingState
        )
    }

    /**
     * Récupère le base clip name pour une entité (unit ou building)
     * Utilisé par AnimationSystem
     */
    fun getBaseClipName(entityType: EntityType): String {
        return when (entityType) {
            // Units
            EntityType.BARBARIAN -> UnitStats.BARBARIAN.spriteBaseClipName
            EntityType.ARCHER -> UnitStats.ARCHER.spriteBaseClipName
            EntityType.GIANT -> UnitStats.GIANT.spriteBaseClipName
            // Buildings
            EntityType.BARRACKS -> BuildingStats.BARRACKS.spriteBaseClipName
            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP.spriteBaseClipName
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP.spriteBaseClipName
        }
    }

    /**
     * Récupère le SpriteSheet pour une unit (avec cache)
     */
    private fun getUnitSpriteSheet(stats: UnitStats): SpriteSheet {
        return unitSpriteSheetCache.getOrPut(stats.name) {
            SpriteSheetParser().parseJson(stats.spriteSheetJsonPath)
        }
    }

    /**
     * Récupère le SpriteSheet pour un building (avec cache)
     */
    private fun getBuildingSpriteSheet(stats: BuildingStats): SpriteSheet {
        return buildingSpriteSheetCache.getOrPut(stats.name) {
            SpriteSheetParser().parseJson(stats.spriteSheetJsonPath)
        }
    }

    /**
     * Pré-parse tous les JSON de sprite sheets et remplit les caches
     * À appeler une fois les textures chargées (dans Main.onAssetsLoaded)
     * Après cet appel, aucune I/O n'est effectuée
     */
    fun preloadAll() {
        val parser = SpriteSheetParser()

        // Preload units
        listOf(UnitStats.BARBARIAN, UnitStats.ARCHER, UnitStats.GIANT).forEach { stats ->
            loadSpriteSheet(parser, stats.name, stats.spriteSheetJsonPath, unitSpriteSheetCache)
        }

        // Preload buildings
        listOf(BuildingStats.BARRACKS, BuildingStats.ELEXIR_PUMP, BuildingStats.DARCKELEXIR_PUMP).forEach { stats ->
            if (stats.spriteSheetJsonPath.isNotEmpty()) {
                loadSpriteSheet(parser, stats.name, stats.spriteSheetJsonPath, buildingSpriteSheetCache)
            }
        }

        isReady = true
    }

    /**
     * Helper générique pour charger un sprite sheet avec gestion d'erreur
     */
    private fun loadSpriteSheet(
        parser: SpriteSheetParser,
        cacheKey: String,
        spriteSheetPath: String,
        cache: MutableMap<String, SpriteSheet>
    ) {
        try {
            cache.getOrPut(cacheKey) {
                parser.parseJson(spriteSheetPath)
            }
        } catch (e: Exception) {
            println("Warning: Failed to load sprite sheet for $cacheKey: ${e.message}")
        }
    }

    /**
     * Décharge tous les sprite sheets du cache
     */
    fun dispose() {
        unitSpriteSheetCache.clear()
        buildingSpriteSheetCache.clear()
        isReady = false
    }
}

