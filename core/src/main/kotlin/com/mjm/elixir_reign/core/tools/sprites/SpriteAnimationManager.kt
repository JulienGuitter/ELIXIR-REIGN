package com.mjm.elixir_reign.core.tools.sprites

import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.shared.logic.DirectionType
import com.mjm.elixir_reign.shared.logic.ActionType
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Manager centralisé pour tous les SpriteSheets et création d'animateurs
 * Remplace SpriteAnimatorHelper
 * Charge les sprite sheets une seule fois et les cache
 */
object SpriteAnimationManager {
    private val spriteSheetCache = mutableMapOf<UnitType, SpriteSheet>()

    // Métadonnées pour chaque unité
    private val unitTypeMetadata = mapOf(
        UnitType.BARBARIAN to UnitMetadata(
            baseClipName = "barbarian",
            texturePath = "sprites/anim_pack_chr_barbarian.png",
            jsonPath = "sprites/anim_pack_chr_barbarian.json"
        ),
        UnitType.ARCHER to UnitMetadata(
            baseClipName = "archer",
            texturePath = "sprites/anim_pack_chr_archer.png",
            jsonPath = "sprites/anim_pack_chr_archer.json"
        ),
        UnitType.GIANT to UnitMetadata(
            baseClipName = "giant",
            texturePath = "sprites/anim_pack_chr_giant.png",
            jsonPath = "sprites/anim_pack_chr_giant.json"
        )
    )

    /**
     * Crée un SpriteAnimator pour une unité avec une action et direction
     * Le SpriteSheet est chargé une seule fois et mis en cache
     */
    fun createAnimator(
        unitType: UnitType,
        actionType: ActionType,
        directionType: DirectionType
    ): SpriteAnimator {
        val metadata = unitTypeMetadata[unitType] ?: throw IllegalArgumentException("Unknown unit type: $unitType")
        val spriteSheet = getSpriteSheet(unitType, metadata)

        return SpriteAnimator(
            spriteSheet = spriteSheet,
            texturePath = metadata.texturePath,
            baseClipName = metadata.baseClipName,
            directionType = directionType,
            actionType = actionType
        )
    }

    /**
     * Récupère la texture path pour une unité
     */
    fun getTexturePath(unitType: UnitType): String {
        return unitTypeMetadata[unitType]?.texturePath ?: throw IllegalArgumentException("Unknown unit type: $unitType")
    }

    /**
     * Récupère le base clip name pour une unité
     */
    fun getBaseClipName(unitType: UnitType): String {
        return unitTypeMetadata[unitType]?.baseClipName ?: throw IllegalArgumentException("Unknown unit type: $unitType")
    }

    /**
     * Récupère le SpriteSheet (avec cache)
     */
    private fun getSpriteSheet(unitType: UnitType, metadata: UnitMetadata): SpriteSheet {
        return spriteSheetCache.getOrPut(unitType) {
            SpriteSheetParser().parseJson(metadata.jsonPath)
        }
    }

    /**
     * Décharge tous les sprite sheets du cache
     */
    fun dispose() {
        spriteSheetCache.clear()
    }

    /**
     * Métadonnées d'une unité
     */
    private data class UnitMetadata(
        val baseClipName: String,
        val texturePath: String,
        val jsonPath: String
    )
}

