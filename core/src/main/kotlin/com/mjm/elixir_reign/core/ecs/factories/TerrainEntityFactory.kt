package com.mjm.elixir_reign.core.ecs.factories

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.ecs.components.TerrainComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.core.tools.terrain.IsoNineSlice
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent

object TerrainEntityFactory {
    private const val TERRAIN_TEXTURE_PATH = "sprites/terrain/anim_pack/pack_ground.png"
    private const val TERRAIN_JSON_PATH = "sprites/terrain/description/pack_ground.json"

    fun createIsoTerrain(
        clipName: String,
        gridColumns: Int,
        gridRows: Int,
        engine: Engine,
        margin: Float = 0.25f
    ): Entity {
        require(gridColumns >= 1 && gridRows >= 1) { "Terrain size must be >= 1x1." }

        val cacheKey = buildCacheKey(clipName, gridColumns, gridRows, margin)
        val texture = TextureManager.getTexture(cacheKey) {
            buildTerrainTexture(clipName, gridColumns, gridRows, margin)
        }
        val region = TextureRegion(texture)

        val entity = Entity()
        entity.add(PositionComponent(0f, 0f))
        entity.add(TextureRegionComponent(region))
        entity.add(SpriteComponent(
            texturePath = cacheKey,
            width = region.regionWidth,
            height = region.regionHeight
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

    private fun buildTerrainTexture(
        clipName: String,
        gridColumns: Int,
        gridRows: Int,
        margin: Float
    ): Texture {
        val spriteSheet = SpriteSheetParser().parseJson(TERRAIN_JSON_PATH)
        val clip = spriteSheet.clips.firstOrNull { it.name == clipName }
            ?: error("Unknown terrain clip: $clipName")

        val sheetPixmap = Pixmap(Gdx.files.internal(TERRAIN_TEXTURE_PATH))
        try {
            val frames = clip.frames.map { frame ->
                extractFrame(
                    sheetPixmap = sheetPixmap,
                    sourceX = frame.x,
                    sourceY = frame.y,
                    width = spriteSheet.cellWidth,
                    height = spriteSheet.cellHeight
                )
            }
            try {
                val terrainPixmap = IsoNineSlice.generate(
                    frames = frames,
                    frameIndex = 0,
                    cols = gridColumns.toFloat(),
                    rows = gridRows.toFloat(),
                    margin = margin
                )
                val trimmedPixmap = IsoNineSlice.trimTransparent(terrainPixmap)
                terrainPixmap.dispose()

                return Texture(trimmedPixmap).also { texture ->
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                    trimmedPixmap.dispose()
                }
            } finally {
                frames.forEach(Pixmap::dispose)
            }
        } finally {
            sheetPixmap.dispose()
        }
    }

    private fun extractFrame(
        sheetPixmap: Pixmap,
        sourceX: Int,
        sourceY: Int,
        width: Int,
        height: Int
    ): Pixmap {
        return Pixmap(width, height, Pixmap.Format.RGBA8888).also { frame ->
            frame.drawPixmap(
                sheetPixmap,
                0,
                0,
                sourceX,
                sourceY,
                width,
                height
            )
        }
    }

    private fun buildCacheKey(
        clipName: String,
        gridColumns: Int,
        gridRows: Int,
        margin: Float
    ): String {
        val marginKey = (margin * 1000f).toInt()
        return "generated/terrain/$clipName/${gridColumns}x$gridRows/m$marginKey"
    }
}
