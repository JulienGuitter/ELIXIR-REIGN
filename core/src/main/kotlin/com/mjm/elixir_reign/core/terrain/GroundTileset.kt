package com.mjm.elixir_reign.core.terrain

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.Disposable

internal enum class GroundTopVariant(val row: Int) {
    VARIANT_0(row = 0),
    VARIANT_1(row = 1),
    VARIANT_2(row = 2)
}

internal enum class ContourSpriteId {
    TOP_LEFT_CORNER,
    TOP_RIGHT_CORNER,
    RIGHT_TOP_SIDE,
    RIGHT_BOTTOM_SIDE,
    BOTTOM_RIGHT_CORNER,
    BOTTOM_LEFT_CORNER,
    LEFT_BOTTOM_SIDE,
    LEFT_TOP_SIDE
}

internal class GroundTileset : Disposable {
    data class ContourSprite(
        val region: TextureRegion,
        val cellOffsetX: Int,
        val overlayOffsetY: Int
    )

    private val texture = Texture(Gdx.files.internal(TEXTURE_PATH)).also {
        it.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
    }

    private val topRegions: Map<TerrainMaterial, Map<GroundTopVariant, TextureRegion>> =
        TerrainMaterial.entries.associateWith { material ->
            GroundTopVariant.entries.associateWith { variant ->
                TextureRegion(
                    texture,
                    material.sheetColumn * TOP_REGION_WIDTH,
                    variant.row * TOP_REGION_HEIGHT,
                    TOP_REGION_WIDTH,
                    TOP_REGION_HEIGHT
                )
            }
        }

    private val contourSprites: Map<ContourSpriteId, ContourSprite> = mapOf(
        ContourSpriteId.TOP_LEFT_CORNER to cropContourSprite(
            cellY = 112,
            localX = 13,
            localY = 0,
            width = 8,
            height = 5,
            flipX = true
        ),
        ContourSpriteId.TOP_RIGHT_CORNER to cropContourSprite(
            cellY = 112,
            localX = 13,
            localY = 0,
            width = 8,
            height = 5
        ),
        ContourSpriteId.RIGHT_TOP_SIDE to cropContourSprite(
            cellY = 48,
            localX = 15,
            localY = 0,
            width = 17,
            height = 10
        ),
        ContourSpriteId.RIGHT_BOTTOM_SIDE to cropContourSprite(
            cellY = 48,
            localX = 15,
            localY = 22,
            width = 17,
            height = 10
        ),
        ContourSpriteId.BOTTOM_RIGHT_CORNER to cropContourSprite(
            cellY = 144,
            localX = 14,
            localY = 27,
            width = 7,
            height = 5
        ),
        ContourSpriteId.BOTTOM_LEFT_CORNER to cropContourSprite(
            cellY = 144,
            localX = 14,
            localY = 27,
            width = 7,
            height = 5,
            flipX = true
        ),
        ContourSpriteId.LEFT_BOTTOM_SIDE to cropContourSprite(
            cellY = 80,
            localX = 0,
            localY = 22,
            width = 19,
            height = 10
        ),
        ContourSpriteId.LEFT_TOP_SIDE to cropContourSprite(
            cellY = 80,
            localX = 0,
            localY = 0,
            width = 19,
            height = 10
        )
    )

    fun top(material: TerrainMaterial, variant: GroundTopVariant): TextureRegion {
        return topRegions.getValue(material).getValue(variant)
    }

    fun contour(id: ContourSpriteId): ContourSprite {
        return contourSprites.getValue(id)
    }

    override fun dispose() {
        texture.dispose()
    }

    private fun cropContourSprite(
        cellY: Int,
        localX: Int,
        localY: Int,
        width: Int,
        height: Int,
        flipX: Boolean = false
    ): ContourSprite {
        val region = TextureRegion(texture, localX, cellY + localY, width, height).also { contour ->
            if (flipX) {
                contour.flip(true, false)
            }
        }
        val cellOffsetX = if (flipX) OVERLAY_REGION_SIZE - localX - width else localX
        val overlayOffsetY = (OVERLAY_REGION_SIZE - localY - height) +
            if (localY >= TOP_REGION_HEIGHT) TOP_REGION_HEIGHT else 0

        return ContourSprite(
            region = region,
            cellOffsetX = cellOffsetX,
            overlayOffsetY = overlayOffsetY
        )
    }

    companion object {
        const val TEXTURE_PATH = "sprites/ground/GroundTileSet.png"
        const val TOP_REGION_WIDTH = 32
        const val TOP_REGION_HEIGHT = 16
        const val OVERLAY_REGION_SIZE = 32
    }
}
