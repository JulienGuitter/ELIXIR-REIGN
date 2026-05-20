package com.mjm.elixir_reign.core.grid

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.mjm.elixir_reign.shared.logic.IsometricGeometry

/**
 * Convertit les coordonnées entre 3 systèmes :
 * - GRID (row, col) : système logique entier, c'est la position dans la grille
 * - WORLD (x, y) : coordonnées continues après conversion isométrique
 * - SCREEN (x, y) : pixels bruts de l'écran
 *
 * Cette classe utilise [IsometricGeometry] pour les conversions GRID ↔ WORLD,
 * éliminant la duplication des formules isométriques et des dimensions.
 */
class IsometricCoordinateConverter(
    private val geometry: IsometricGeometry
) {
    /**
     * Convertit une position grille (row, col) → coordonnées monde (x, y)
     * Délègue à IsometricGeometry
     */
    fun gridToWorld(row: Int, col: Int): Vector2 {
        return geometry.gridToWorld(row, col)
    }

    /**
     * Convertit des coordonnées monde (x, y) → grille (row, col)
     * Délègue à IsometricGeometry
     */
    fun worldToGrid(worldX: Float, worldY: Float): Pair<Int, Int>? {
        return geometry.worldToGrid(worldX, worldY)
    }

    /**
     * Convertit des coordonnées écran (pixels bruts) → coordonnées monde
     * Utilise la caméra pour transformer
     */
    fun screenToWorld(screenX: Float, screenY: Float, camera: OrthographicCamera): Vector2 {
        val worldVector = camera.unproject(Vector3(screenX, screenY, 0f))
        return Vector2(worldVector.x, worldVector.y)
    }

    /**
     * Vérifie si une position grille est valide (dans les bounds de la matrice)
     * Délègue à IsometricGeometry
     */
    fun isGridPositionValid(row: Int, col: Int): Boolean {
        return geometry.isGridPositionValid(row, col)
    }
}
