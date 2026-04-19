package com.mjm.elixir_reign.shared.logic

/**
 * Centralise toutes les vérifications de bounds pour la grille.
 *
 * SOURCE UNIQUE DE VÉRITÉ pour :
 * - Validation de positions (row, col) dans une grille donnée
 * - Élimination de la duplication des vérifications aux quatre coins
 *
 * Utilisée par IsometricGeometry, IsometricGrid, BuildPlacementHandler et autres composants.
 */
object BoundsValidator {

    /**
     * Vérifie si une position (row, col) est dans les limites de la grille
     *
     * @param row Ligne (0-indexed)
     * @param col Colonne (0-indexed)
     * @param gridHeight Hauteur de la grille
     * @param gridWidth Largeur de la grille
     * @return true si (row, col) est valide
     */
    fun isWithinBounds(row: Int, col: Int, gridHeight: Int, gridWidth: Int): Boolean {
        return row >= 0 && row < gridHeight && col >= 0 && col < gridWidth
    }

    /**
     * Vérifie si une position (row, col) est hors limites
     *
     * @return true si (row, col) est hors limites
     */
    fun isOutOfBounds(row: Int, col: Int, gridHeight: Int, gridWidth: Int): Boolean {
        return !isWithinBounds(row, col, gridHeight, gridWidth)
    }

    /**
     * Valide une collection de cellules par rapport aux bounds de la grille
     *
     * @param cells Collection de (row, col) à valider (List ou Set)
     * @param gridHeight Hauteur de la grille
     * @param gridWidth Largeur de la grille
     * @return true si toutes les cellules sont dans les bounds
     */
    fun validateCellBounds(cells: Collection<Pair<Int, Int>>, gridHeight: Int, gridWidth: Int): Boolean {
        return cells.all { (row, col) ->
            isWithinBounds(row, col, gridHeight, gridWidth)
        }
    }
}


