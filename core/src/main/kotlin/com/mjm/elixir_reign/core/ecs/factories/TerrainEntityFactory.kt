package com.mjm.elixir_reign.core.ecs.factories

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.ecs.components.TerrainComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.AnimationClip
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent

object TerrainEntityFactory {
    private const val TERRAIN_TEXTURE_PATH = "sprites/terrain/anim_pack/pack_ground.png"
    private const val TERRAIN_JSON_PATH = "sprites/terrain/description/pack_ground.json"
    private val spriteSheet by lazy { SpriteSheetParser().parseJson(TERRAIN_JSON_PATH) }

    fun createIsoTerrain(
        clipName: String,
        gridColumns: Int,
        gridRows: Int,
        engine: Engine,
        margin: Float = 0.25f
    ): Entity {
        require(gridColumns >= 1 && gridRows >= 1) { "Terrain size must be >= 1x1." }

        val clip = getClip(clipName)
        val frame = clip.frames.firstOrNull()
            ?: error("Terrain clip $clipName has no frame.")
        val texture = TextureManager.getTexture(TERRAIN_TEXTURE_PATH)
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        val region = TextureRegion(
            texture,
            frame.x,
            frame.y,
            spriteSheet.cellWidth,
            spriteSheet.cellHeight
        )

        val entity = Entity()
        entity.add(PositionComponent(0f, 0f))
        entity.add(TextureRegionComponent(region))
        entity.add(SpriteComponent(
            texturePath = TERRAIN_TEXTURE_PATH,
            width = computeTerrainWidth(gridColumns, gridRows),
            height = computeTerrainHeight(gridColumns, gridRows)
        ))
        entity.add(TerrainComponent(
            clipName = clipName,
            gridColumns = gridColumns,
            gridRows = gridRows,
            margin = margin
        ))

        engine.addEntity(entity)
        return entity
    }

    private fun getClip(clipName: String): AnimationClip {
        return spriteSheet.clips.firstOrNull { it.name == clipName }
            ?: error("Unknown terrain clip: $clipName")
    }

    private fun computeTerrainWidth(gridColumns: Int, gridRows: Int): Int {
        return ((spriteSheet.cellWidth / 2f) * (gridColumns + gridRows)).toInt()
    }

    private fun computeTerrainHeight(gridColumns: Int, gridRows: Int): Int {
        return ((spriteSheet.cellHeight / 2f) * (gridColumns + gridRows)).toInt()
    }
}
