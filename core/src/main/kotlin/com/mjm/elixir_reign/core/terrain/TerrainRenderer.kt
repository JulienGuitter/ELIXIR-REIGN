package com.mjm.elixir_reign.core.terrain

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.mjm.elixir_reign.shared.terrain.TerrainMatrix
import com.mjm.elixir_reign.shared.terrain.TerrainType

class TerrainRenderer(
    private val matrix: TerrainMatrix,
    private val scale: Float = 4f
) : Disposable {

    private val tileset = GroundTileset()
    private val tileWidth = GroundTileset.TOP_REGION_WIDTH * scale
    private val tileHeight = GroundTileset.TOP_REGION_HEIGHT * scale
    private val halfTileWidth = tileWidth / 2f
    private val halfTileHeight = tileHeight / 2f

    private val renderTiles: List<RenderTile> = buildRenderTiles()
    private val terrainContourCommands: List<ContourPreviewCommand> = buildTerrainContourCommands()
    private val grassBottomAngleCommands: List<ContourPreviewCommand> = buildGrassBottomAngleCommands()
    private val terrainBounds: Rectangle = buildTerrainBounds()

    fun render(batch: SpriteBatch) {
        renderTiles.forEach { tile ->
            drawRegion(
                batch = batch,
                region = tileset.top(tile.type.material, tile.type.topVariant),
                x = tile.x,
                y = tile.y,
                width = tileWidth,
                height = tileHeight
            )
        }

        terrainContourCommands.forEach { command ->
            drawContourCommand(batch, command)
        }

        grassBottomAngleCommands.forEach { command ->
            drawContourCommand(batch, command)
        }
    }

    fun worldBounds(): Rectangle {
        return Rectangle(terrainBounds)
    }

    override fun dispose() {
        tileset.dispose()
    }

    private fun buildRenderTiles(): List<RenderTile> {
        val rawTiles = mutableListOf<RawTile>()

        for (row in 0 until matrix.height) {
            for (col in 0 until matrix.width) {
                val type = matrix[row, col] ?: continue

                rawTiles += RawTile(
                    row = row,
                    col = col,
                    type = type,
                    rawX = (col - row) * halfTileWidth,
                    rawY = -(col + row) * halfTileHeight
                )
            }
        }

        val minX = rawTiles.minOf { it.rawX }
        val maxX = rawTiles.maxOf { it.rawX + tileWidth }
        val minY = rawTiles.minOf { it.rawY }
        val maxY = rawTiles.maxOf { it.rawY + tileHeight }

        val offsetX = -((minX + maxX) / 2f)
        val offsetY = -((minY + maxY) / 2f)

        return rawTiles
            .sortedWith(compareBy<RawTile> { it.row + it.col }.thenBy { it.row }.thenBy { it.col })
            .map { tile ->
                RenderTile(
                    row = tile.row,
                    col = tile.col,
                    type = tile.type,
                    x = tile.rawX + offsetX,
                    y = tile.rawY + offsetY
                )
            }
    }

    private fun buildTerrainContourCommands(): List<ContourPreviewCommand> {
        val commands = mutableListOf<ContourPreviewCommand>()

        renderTiles.forEach { tile ->
            if (!tile.type.isBlendTarget) {
                return@forEach
            }

            val hasGrassLeftUp = matrix[tile.row, tile.col - 1]?.isGrass == true
            val hasGrassRightUp = matrix[tile.row - 1, tile.col]?.isGrass == true
            val hasGrassLeftDown = matrix[tile.row + 1, tile.col]?.isGrass == true
            val hasGrassRightDown = matrix[tile.row, tile.col + 1]?.isGrass == true

            if (hasGrassLeftUp) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.LEFT_TOP_SIDE,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }

            if (hasGrassRightUp) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.RIGHT_TOP_SIDE,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }

            if (hasGrassLeftDown) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.LEFT_BOTTOM_SIDE,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }

            if (hasGrassRightDown) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.RIGHT_BOTTOM_SIDE,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }

            if (hasGrassLeftUp && hasGrassRightUp) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.TOP_LEFT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.TOP_RIGHT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }

            if (hasGrassLeftDown && hasGrassRightDown) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.BOTTOM_LEFT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.BOTTOM_RIGHT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight
                )
            }
        }

        return commands
    }

    private fun buildTerrainBounds(): Rectangle {
        val minTileX = renderTiles.minOf { it.x }
        val maxTileX = renderTiles.maxOf { it.x + tileWidth }
        val minTileY = renderTiles.minOf { it.y }
        val maxTileY = renderTiles.maxOf { it.y + tileHeight }

        val allContourCommands = terrainContourCommands + grassBottomAngleCommands
        val minContourX = allContourCommands.minOfOrNull { it.x } ?: minTileX
        val maxContourX = allContourCommands.maxOfOrNull { it.x + it.width } ?: maxTileX
        val minContourY = allContourCommands.minOfOrNull { it.y } ?: minTileY
        val maxContourY = allContourCommands.maxOfOrNull { it.y + it.height } ?: maxTileY

        val minX = minOf(minTileX, minContourX)
        val maxX = maxOf(maxTileX, maxContourX)
        val minY = minOf(minTileY, minContourY)
        val maxY = maxOf(maxTileY, maxContourY)

        return Rectangle(minX, minY, maxX - minX, maxY - minY)
    }

    private fun buildGrassBottomAngleCommands(): List<ContourPreviewCommand> {
        val commands = mutableListOf<ContourPreviewCommand>()

        renderTiles.forEach { tile ->
            if (!tile.type.isGrass) {
                return@forEach
            }

            val hasNonGrassLeftDown = matrix[tile.row + 1, tile.col]?.isGrass == false
            val hasNonGrassRightDown = matrix[tile.row, tile.col + 1]?.isGrass == false

            if (hasNonGrassLeftDown && hasNonGrassRightDown) {
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.TOP_LEFT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight * 2f
                )
                commands += buildContourOverlayCommand(
                    id = ContourSpriteId.TOP_RIGHT_CORNER,
                    overlayCellX = tile.x,
                    overlayCellY = tile.y - tileHeight * 2f
                )
            }
        }

        return commands
    }

    private fun buildContourOverlayCommand(
        id: ContourSpriteId,
        overlayCellX: Float,
        overlayCellY: Float
    ): ContourPreviewCommand {
        val sprite = tileset.contour(id)

        return ContourPreviewCommand(
            sprite = sprite,
            x = overlayCellX + sprite.cellOffsetX * scale,
            y = overlayCellY + sprite.overlayOffsetY * scale,
            width = sprite.region.regionWidth * scale,
            height = sprite.region.regionHeight * scale
        )
    }

    private fun drawContourCommand(batch: SpriteBatch, command: ContourPreviewCommand) {
        drawRegion(
            batch = batch,
            region = command.sprite.region,
            x = command.x,
            y = command.y,
            width = command.width,
            height = command.height
        )
    }

    private fun drawRegion(
        batch: SpriteBatch,
        region: com.badlogic.gdx.graphics.g2d.TextureRegion,
        x: Float,
        y: Float,
        width: Float,
        height: Float
    ) {
        batch.draw(region, x, y, width, height)
    }

    private data class RawTile(
        val row: Int,
        val col: Int,
        val type: TerrainType,
        val rawX: Float,
        val rawY: Float
    )

    private data class RenderTile(
        val row: Int,
        val col: Int,
        val type: TerrainType,
        val x: Float,
        val y: Float
    )

    private data class ContourPreviewCommand(
        val sprite: GroundTileset.ContourSprite,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float
    )
}
