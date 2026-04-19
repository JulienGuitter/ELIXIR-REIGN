package com.mjm.elixir_reign.core.grid

import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.shared.logic.IsometricGeometry

/**
 * Calcule les positions spécifiques aux bâtiments
 *
 * ARCHITECTURE:
 * - La GRILLE (losanges isométriques) → serve pour VALIDATION du placement
 * - La POSITION MONDE (continue) → used pour RENDU et PositionComponent
 * - Les foot_x/foot_y du JSON → APPLIQUÉS par SpritePositionCalculator au rendu
 *
 * IMPORTANT: Le sprite n'est PAS snapé à la case!
 * Il est placé à une position monde pure, les offsets foot_x/foot_y calibrent l'alignement visuel
 */
class BuildingPlacementCalculator(
    private val geometry: IsometricGeometry,
    private val converter: IsometricCoordinateConverter
) {

    /**
     * Calcule la position WORLD du centre de la case grille
     * (servant de référence pour le placement)
     */
    fun calculateGridCellCenterWorldPosition(
        gridRow: Int,
        gridCol: Int,
        buildingWidthTiles: Int = 1,
        buildingHeightTiles: Int = 1
    ): Vector2 {
        // Si le bâtiment fait 1x1, utiliser simplement gridToWorld
        if (buildingWidthTiles == 1 && buildingHeightTiles == 1) {
            return converter.gridToWorld(gridRow, gridCol)
        }

        // Si le bâtiment fait plus de 1x1, calculer le centre du footprint
        val startRow = gridRow - buildingHeightTiles / 2
        val startCol = gridCol - buildingWidthTiles / 2

        // Obtenir les 4 coins du footprint
        val topLeft = converter.gridToWorld(startRow, startCol)
        val bottomRight = converter.gridToWorld(
            startRow + buildingHeightTiles - 1,
            startCol + buildingWidthTiles - 1
        )

        // Retourner le centre
        return Vector2(
            (topLeft.x + bottomRight.x) / 2f,
            (topLeft.y + bottomRight.y) / 2f
        )
    }

    /**
     * Calcule la position WORLD PURE pour le placement du sprite
     *
     * Cette position doit être utilisée DIRECTEMENT dans PositionComponent.
     *
     * IMPORTANT:
     * - PAS de snap à la grille
     * - PAS d'application des offsets foot_x/foot_y ici
     * - Les offsets foot_x/foot_y seront appliqués par SpritePositionCalculator
     *   lors du rendu (drawX = worldX + (width × scale × offsetX))
     *
     * @param gridRow Ligne de la grille (pour validation)
     * @param gridCol Colonne de la grille (pour validation)
     * @param buildingWidthTiles Largeur du footprint
     * @param buildingHeightTiles Hauteur du footprint
     * @return Position monde PURE (pour PositionComponent)
     */
    fun calculateBuildingWorldPosition(
        gridRow: Int,
        gridCol: Int,
        buildingWidthTiles: Int = 1,
        buildingHeightTiles: Int = 1
    ): Vector2 {
        // ✅ Retourner la position du centre de la case
        // Les foot_x/foot_y du sprite JSON seront appliqués au rendu
        return calculateGridCellCenterWorldPosition(gridRow, gridCol, buildingWidthTiles, buildingHeightTiles)
    }

    /**
     * Obtient la liste des cellules occupées par un bâtiment
     * (utilisé pour la validation du placement)
     */
    fun getBuildingFootprint(
        gridRow: Int,
        gridCol: Int,
        buildingWidthTiles: Int = 1,
        buildingHeightTiles: Int = 1
    ): List<Pair<Int, Int>> {
        val startRow = gridRow - buildingHeightTiles / 2
        val startCol = gridCol - buildingWidthTiles / 2

        val footprint = mutableListOf<Pair<Int, Int>>()
        for (r in startRow until startRow + buildingHeightTiles) {
            for (c in startCol until startCol + buildingWidthTiles) {
                footprint.add(Pair(r, c))
            }
        }
        return footprint
    }

    /**
     * Vérifie si toutes les cellules du footprint sont valides
     */
    fun isValidPlacement(
        gridRow: Int,
        gridCol: Int,
        buildingWidthTiles: Int = 1,
        buildingHeightTiles: Int = 1
    ): Boolean {
        val footprint = getBuildingFootprint(gridRow, gridCol, buildingWidthTiles, buildingHeightTiles)
        return footprint.all { (r, c) ->
            converter.isGridPositionValid(r, c)
        }
    }
}
