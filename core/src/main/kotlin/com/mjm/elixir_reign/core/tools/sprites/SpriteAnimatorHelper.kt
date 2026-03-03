package com.mjm.elixir_reign.core.tools.sprites

import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheet
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.shared.logic.Direction
import com.mjm.elixir_reign.shared.logic.Action
import com.mjm.elixir_reign.shared.logic.Unit

class SpriteAnimatorHelper(private val unitType: Unit) {
    private val unit = when (unitType) {
        Unit.BARBARIAN -> "barbarian"
        Unit.ARCHER -> "archer"
    }

    private val texturePath = when (unitType) {
        Unit.BARBARIAN -> "sprites/anim_pack_chr_barbarian.png"
        Unit.ARCHER -> "sprites/anim_pack_chr_archer.png"
    }

    private val spriteSheet = loadSpriteSheet()

    private fun loadSpriteSheet(): SpriteSheet {
        val jsonPath = when (unitType) {
            Unit.BARBARIAN -> "anim_pack_chr_barbarian.json"
            Unit.ARCHER -> "anim_pack_chr_archer.json"
        }
        return SpriteSheetParser().parseJson(jsonPath)
    }

    fun getAnimation(action: Action, direction: Direction): SpriteAnimator {
        return SpriteAnimator(
            spriteSheet = spriteSheet,
            texturePath = texturePath,
            baseClipName = unit,
            direction = direction,
            action = action
        )
    }
}
