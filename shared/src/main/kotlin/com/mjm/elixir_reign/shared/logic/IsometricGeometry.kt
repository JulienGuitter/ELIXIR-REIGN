package com.mjm.elixir_reign.shared.logic

import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.shared.world.WorldMap

/**
 * Centralise la géométrie isométrique et les formules de conversion.
 *
 * Cette classe encapsule :
 * - Les dimensions des tuiles (tileWidth, tileHeight, halfTileWidth, halfTileHeight)
 * - Les offsets de centrage (offsetX, offsetY)
 * - Les formules de conversion GRID ↔ WORLD (isométrique)
 *
 * Elle garantit que TOUS les composants (TerrainRenderer, IsometricGridRenderer, IsometricCoordinateConverter)
 * utilisent exactement les mêmes formules et dimensions.
 *
 * Immutable après initialisation.
 */
class IsometricGeometry(
    private val worldMap: WorldMap,
    val scale: Float = 4f
) {
    // Dimensions de la grille logique
    val gridHeight: Int = worldMap.height
    val gridWidth: Int = worldMap.width

    // Dimensions des tuiles (basées sur GroundTileset)
    val tileWidth = 32 * scale     // GroundTileset.TOP_REGION_WIDTH (32) * scale
    val tileHeight = 16 * scale    // GroundTileset.TOP_REGION_HEIGHT (16) * scale
    val halfTileWidth = tileWidth / 2f
    val halfTileHeight = tileHeight / 2f

    // Offsets de centrage (calculés une seule fois, immutable)
    val offsetX: Float
    val offsetY: Float

    init {
        val offsets = calculateOffsets()
        offsetX = offsets.first
        offsetY = offsets.second
    }

    /**
     * Convertit une position grille (row, col) → coordonnées monde (x, y)
     * Formule isométrique classique : les colonnes vont vers la droite, les rangées vers le haut-gauche
     */
    fun gridToWorld(row: Int, col: Int): Vector2 {
        val rawX = (col - row) * halfTileWidth
        val rawY = -(col + row) * halfTileHeight
        return Vector2(rawX + offsetX, rawY + offsetY)
    }

    /**
     * Convertit des coordonnées monde (x, y) → grille (row, col)
     * C'est l'inverse mathématique de la formule isométrique
     *
     * @return Pair(row, col) ou null si la position est hors limites
     */
    fun worldToGrid(worldX: Float, worldY: Float): Pair<Int, Int>? {
        // Soustraire les offsets pour revenir à "rawX, rawY"
        val rawX = worldX - offsetX
        val rawY = worldY - offsetY

        // Inverser les formules isométriques :
        // rawX = (col - row) * halfTileWidth → col - row = A
        // rawY = -(col + row) * halfTileHeight → col + row = B
        // Résoudre : col = (A + B) / 2, row = (B - A) / 2

        val A = rawX / halfTileWidth
        val B = -rawY / halfTileHeight

        // roundToInt pour éviter le biais de toInt() sur coordonnées négatives
        val col = ((A + B) / 2f).toInt()
        val row = ((B - A) / 2f).toInt()

        return if (isGridPositionValid(row, col)) {
            Pair(row, col)
        } else {
            null
        }
    }

    /**
     * Vérifie si une position grille est valide (dans les bounds de la matrice)
     */
    fun isGridPositionValid(row: Int, col: Int): Boolean {
        return BoundsValidator.isWithinBounds(row, col, gridHeight, gridWidth)
    }

    /**
     * Retourne les bounds du monde (utilisé pour la caméra, frustum culling, etc.)
     */
    fun getWorldBounds(): Pair<Float, Float> {
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (row in 0 until worldMap.height) {
            for (col in 0 until worldMap.width) {
                if (worldMap[row, col] == null) continue

                val worldPos = gridToWorld(row, col)
                minX = minOf(minX, worldPos.x)
                maxX = maxOf(maxX, worldPos.x + tileWidth)
                minY = minOf(minY, worldPos.y)
                maxY = maxOf(maxY, worldPos.y + tileHeight)
            }
        }

        return Pair(maxX - minX, maxY - minY)  // Retourne (width, height)
    }

    /**
     * Calcule les offsets de centrage (même logique que TerrainRenderer)
     * Les offsets positionnent le centre de la grille à l'origine (0, 0)
     */
    private fun calculateOffsets(): Pair<Float, Float> {
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE

        for (row in 0 until worldMap.height) {
            for (col in 0 until worldMap.width) {
                val rawX = (col - row) * halfTileWidth
                val rawY = -(col + row) * halfTileHeight

                minX = minOf(minX, rawX)
                maxX = maxOf(maxX, rawX + tileWidth)
                minY = minOf(minY, rawY)
                maxY = maxOf(maxY, rawY + tileHeight)
            }
        }

        val offsetX = -((minX + maxX) / 2f)
        val offsetY = -((minY + maxY) / 2f)

        return Pair(offsetX, offsetY)
    }
}


