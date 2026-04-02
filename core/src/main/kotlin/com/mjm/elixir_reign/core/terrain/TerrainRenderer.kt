package com.mjm.elixir_reign.core.terrain

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.world.ChunkCoord
import com.mjm.elixir_reign.shared.world.WorldMap

class TerrainRenderer(
    private val worldMap: WorldMap,
    private val scale: Float = 4f
) : Disposable {

    private val tileset = GroundTileset()
    private val tileWidth = GroundTileset.TOP_REGION_WIDTH * scale
    private val tileHeight = GroundTileset.TOP_REGION_HEIGHT * scale
    private val halfTileWidth = tileWidth / 2f
    private val halfTileHeight = tileHeight / 2f

    private val rawTiles: List<RawTile> = buildRawTiles()
    private val renderOffset: RenderOffset = computeRenderOffset(rawTiles)
    private val renderTiles: List<RenderTile> = buildRenderTiles()
    private val terrainContourCommands: List<ContourPreviewCommand> = buildTerrainContourCommands()
    private val grassBottomAngleCommands: List<ContourPreviewCommand> = buildGrassBottomAngleCommands()
    private val chunkDebugOutlines: List<ChunkDebugOutline> = buildChunkDebugOutlines()
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

    fun renderChunkDebug(shapeRenderer: ShapeRenderer) {
        chunkDebugOutlines.forEach { outline ->
            shapeRenderer.line(outline.topX, outline.topY, outline.rightX, outline.rightY)
            shapeRenderer.line(outline.rightX, outline.rightY, outline.bottomX, outline.bottomY)
            shapeRenderer.line(outline.bottomX, outline.bottomY, outline.leftX, outline.leftY)
            shapeRenderer.line(outline.leftX, outline.leftY, outline.topX, outline.topY)
        }
    }

    fun renderChunkDebugLabels(batch: SpriteBatch, font: BitmapFont) {
        chunkDebugOutlines.forEach { outline ->
            font.draw(
                batch,
                "(${outline.coord.x},${outline.coord.y})",
                outline.labelX,
                outline.labelY
            )
        }
    }

    override fun dispose() {
        tileset.dispose()
    }

    private fun buildRawTiles(): List<RawTile> {
        val rawTiles = mutableListOf<RawTile>()

        worldMap.groundChunks().forEach { chunk ->
            chunk.ground.forEachIndexed { localRow, localCol, type ->
                if (type == null) {
                    return@forEachIndexed
                }

                val row = chunk.originRow + localRow
                val col = chunk.originCol + localCol

                rawTiles += RawTile(
                    row = row,
                    col = col,
                    type = type,
                    rawX = (col - row) * halfTileWidth,
                    rawY = -(col + row) * halfTileHeight
                )
            }
        }

        return rawTiles
    }

    private fun computeRenderOffset(rawTiles: List<RawTile>): RenderOffset {
        if (rawTiles.isEmpty()) {
            return RenderOffset(x = 0f, y = 0f)
        }

        val minX = rawTiles.minOf { it.rawX }
        val maxX = rawTiles.maxOf { it.rawX + tileWidth }
        val minY = rawTiles.minOf { it.rawY }
        val maxY = rawTiles.maxOf { it.rawY + tileHeight }

        return RenderOffset(
            x = -((minX + maxX) / 2f),
            y = -((minY + maxY) / 2f)
        )
    }

    private fun buildRenderTiles(): List<RenderTile> {
        if (rawTiles.isEmpty()) {
            return emptyList()
        }

        return rawTiles
            .sortedWith(compareBy<RawTile> { it.row + it.col }.thenBy { it.row }.thenBy { it.col })
            .map { tile ->
                RenderTile(
                    row = tile.row,
                    col = tile.col,
                    type = tile.type,
                    x = tile.rawX + renderOffset.x,
                    y = tile.rawY + renderOffset.y
                )
            }
    }

    private fun buildTerrainContourCommands(): List<ContourPreviewCommand> {
        val commands = mutableListOf<ContourPreviewCommand>()

        renderTiles.forEach { tile ->
            if (!tile.type.isBlendTarget) {
                return@forEach
            }

            val hasGrassLeftUp = worldMap[tile.row, tile.col - 1]?.isGrass == true
            val hasGrassRightUp = worldMap[tile.row - 1, tile.col]?.isGrass == true
            val hasGrassLeftDown = worldMap[tile.row + 1, tile.col]?.isGrass == true
            val hasGrassRightDown = worldMap[tile.row, tile.col + 1]?.isGrass == true

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
        if (renderTiles.isEmpty()) {
            return Rectangle()
        }

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

    private fun buildChunkDebugOutlines(): List<ChunkDebugOutline> {
        return worldMap.groundChunks().mapNotNull { chunk ->
            val chunkRows = minOf(chunk.size, worldMap.height - chunk.originRow)
            val chunkCols = minOf(chunk.size, worldMap.width - chunk.originCol)

            if (chunkRows <= 0 || chunkCols <= 0) {
                return@mapNotNull null
            }

            val topLeft = tileRenderPosition(row = chunk.originRow, col = chunk.originCol)
            val topRight = tileRenderPosition(row = chunk.originRow, col = chunk.originCol + chunkCols - 1)
            val bottomRight = tileRenderPosition(
                row = chunk.originRow + chunkRows - 1,
                col = chunk.originCol + chunkCols - 1
            )
            val bottomLeft = tileRenderPosition(
                row = chunk.originRow + chunkRows - 1,
                col = chunk.originCol
            )

            val topX = topLeft.x + halfTileWidth
            val topY = topLeft.y + tileHeight
            val rightX = topRight.x + tileWidth
            val rightY = topRight.y + halfTileHeight
            val bottomX = bottomRight.x + halfTileWidth
            val bottomY = bottomRight.y
            val leftX = bottomLeft.x
            val leftY = bottomLeft.y + halfTileHeight

            ChunkDebugOutline(
                coord = chunk.coord,
                topX = topX,
                topY = topY,
                rightX = rightX,
                rightY = rightY,
                bottomX = bottomX,
                bottomY = bottomY,
                leftX = leftX,
                leftY = leftY,
                labelX = (topX + rightX + bottomX + leftX) / 4f,
                labelY = (topY + rightY + bottomY + leftY) / 4f
            )
        }
    }

    private fun buildGrassBottomAngleCommands(): List<ContourPreviewCommand> {
        val commands = mutableListOf<ContourPreviewCommand>()

        renderTiles.forEach { tile ->
            if (!tile.type.isGrass) {
                return@forEach
            }

            val hasNonGrassLeftDown = worldMap[tile.row + 1, tile.col]?.isGrass == false
            val hasNonGrassRightDown = worldMap[tile.row, tile.col + 1]?.isGrass == false

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

    private fun tileRenderPosition(row: Int, col: Int): Point {
        return Point(
            x = (col - row) * halfTileWidth + renderOffset.x,
            y = -(col + row) * halfTileHeight + renderOffset.y
        )
    }

    private data class RenderOffset(
        val x: Float,
        val y: Float
    )

    private data class Point(
        val x: Float,
        val y: Float
    )

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

    private data class ChunkDebugOutline(
        val coord: ChunkCoord,
        val topX: Float,
        val topY: Float,
        val rightX: Float,
        val rightY: Float,
        val bottomX: Float,
        val bottomY: Float,
        val leftX: Float,
        val leftY: Float,
        val labelX: Float,
        val labelY: Float
    )
}
