package com.mjm.elixir_reign.shared.logic

/**
 * Représente un bâtiment qui occupe des tuiles sur la grille isométrique
 *
 * @param gridRow Position en grille (ligne/row)
 * @param gridCol Position en grille (colonne/col)
 * @param widthInTiles Largeur occupée en tuiles
 * @param heightInTiles Hauteur occupée en tuiles
 */
data class BuildingOccupancy(
    val gridRow: Int,
    val gridCol: Int,
    val widthInTiles: Int,
    val heightInTiles: Int
) {
    /**
     * Calcule toutes les tuiles occupées par ce bâtiment
     * Retourne un Set de pairs (row, col)
     *
     * Exemple : bâtiment 2x2 à (0,0) occupe :
     * - (0, 0), (0, 1), (1, 0), (1, 1)
     */
    fun getOccupiedCells(): Set<Pair<Int, Int>> {
        val occupied = mutableSetOf<Pair<Int, Int>>()

        for (r in gridRow until gridRow + heightInTiles) {
            for (c in gridCol until gridCol + widthInTiles) {
                occupied.add(Pair(r, c))
            }
        }

        return occupied
    }
}
