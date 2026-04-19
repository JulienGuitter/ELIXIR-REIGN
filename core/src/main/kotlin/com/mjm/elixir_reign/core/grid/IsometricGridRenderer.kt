package com.mjm.elixir_reign.core.grid

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.logic.IsometricGeometry

/**
 * Affiche une grille de construction isométrique alignée avec les tuiles du terrain.
 *
 * Utilise [IsometricGeometry] pour garantir que les positions sont identiques au rendu des tuiles.
 */
class IsometricGridRenderer(
    private val geometry: IsometricGeometry
) {
    // Couleurs pour la grille
    private val gridColor = Color(0.5f, 0.5f, 0.5f, 0.2f)  // Gris semi-transparent
    private val highlightColor = Color(0f, 1f, 0f, 0.8f)  // Vert plus opaque

    // Offset pour aligner la grille avec le terrain (ajustable pour debug)
    private val gridOffsetX = geometry.halfTileWidth
    private val gridOffsetY = geometry.halfTileHeight

    /**
     * Render la grille de construction isométrique
     */
    fun render(
        shapeRenderer: ShapeRenderer,
        isCellVisible: (row: Int, col: Int) -> Boolean = { _, _ -> true }
    ) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = gridColor

        // Parcourir toutes les cellules et dessiner les losanges
        for (row in 0 until geometry.gridHeight) {
            for (col in 0 until geometry.gridWidth) {
                if (!geometry.isGridPositionValid(row, col)) {
                    continue
                }
                if (!isCellVisible(row, col)) {
                    continue
                }
                drawTileDiamond(shapeRenderer, row, col)
            }
        }

        shapeRenderer.end()
    }

    private fun drawTileDiamond(shapeRenderer: ShapeRenderer, row: Int, col: Int) {
        val center = geometry.gridToWorld(row, col)
        val centerX = center.x + gridOffsetX
        val centerY = center.y + gridOffsetY

        val topX = centerX
        val topY = centerY + geometry.halfTileHeight
        val rightX = centerX + geometry.halfTileWidth
        val rightY = centerY
        val bottomX = centerX
        val bottomY = centerY - geometry.halfTileHeight
        val leftX = centerX - geometry.halfTileWidth
        val leftY = centerY

        shapeRenderer.line(topX, topY, rightX, rightY)
        shapeRenderer.line(rightX, rightY, bottomX, bottomY)
        shapeRenderer.line(bottomX, bottomY, leftX, leftY)
        shapeRenderer.line(leftX, leftY, topX, topY)
    }

    /**
     * Subrille plusieurs tuiles
     */
    fun highlightTiles(shapeRenderer: ShapeRenderer, cells: Iterable<Pair<Int, Int>>, color: Color = highlightColor) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color = color

        for ((row, col) in cells) {
            if (!geometry.isGridPositionValid(row, col)) {
                continue
            }
            drawTileDiamond(shapeRenderer, row, col)
        }

        shapeRenderer.end()
    }
}
