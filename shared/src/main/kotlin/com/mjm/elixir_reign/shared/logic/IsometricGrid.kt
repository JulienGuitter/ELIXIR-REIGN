package com.mjm.elixir_reign.shared.logic

/**
 * Grille isométrique qui track l'occupation des tuiles par les bâtiments
 *
 * C'est essentiellement une "map" :
 * - Clé : (row, col) = une tuile
 * - Valeur : BuildingOccupancy (quel bâtiment l'occupe) ou null (vide)
 *
 * @param terrainWidth Largeur du terrain en tuiles
 * @param terrainHeight Hauteur du terrain en tuiles
 */
class IsometricGrid(
    val terrainWidth: Int,
    val terrainHeight: Int
) {
    // Map qui track : (row, col) → BuildingOccupancy
    // null = tuile vide
    private val occupancyMap = mutableMapOf<Pair<Int, Int>, BuildingOccupancy>()

    /**
     * Ajoute un bâtiment à la grille
     * Occupe toutes ses cellules
     *
     * @return true si succès, false si collision avec bâtiment existant
     */
    fun placeBuilding(building: BuildingOccupancy): Boolean {
        val occupiedCells = building.getOccupiedCells()

        // Valider toutes les cellules (bounds + collision)
        if (!validateCells(occupiedCells)) {
            return false
        }

        // OK : occuper toutes les cellules
        for (cell in occupiedCells) {
            occupancyMap[cell] = building
        }

        return true
    }

    /**
     * Retire un bâtiment à une position donnée
     *
     * @return true si le bâtiment a été retiré, false s'il n'existe pas
     */
    fun removeBuildingAt(row: Int, col: Int): Boolean {
        val building = occupancyMap[Pair(row, col)] ?: return false

        val cellsToRemove = building.getOccupiedCells()
        cellsToRemove.forEach { occupancyMap.remove(it) }
        return true
    }

    /**
     * Récupère le bâtiment à une position donnée
     *
     * @return BuildingOccupancy si occupé, null sinon
     */
    fun getBuilding(row: Int, col: Int): BuildingOccupancy? {
        return occupancyMap[Pair(row, col)]
    }

    /**
     * Vérifie si une cellule est libre
     */
    fun isCellFree(row: Int, col: Int): Boolean {
        return !occupancyMap.containsKey(Pair(row, col))
    }

    /**
     * Récupère tous les bâtiments placés
     */
    fun getAllBuildings(): List<BuildingOccupancy> {
        return occupancyMap.values.toList()
    }

    /**
     * Vérifie si on peut placer un bâtiment à une position donnée
     * Sans l'ajouter réellement
     */
    fun canPlaceBuilding(building: BuildingOccupancy): Boolean {
        return validateCells(building.getOccupiedCells())
    }

    /**
     * Valide que toutes les cellules sont valides (dans les bounds et non occupées)
     * SOURCE UNIQUE pour la validation des cellules
     */
    private fun validateCells(cells: Collection<Pair<Int, Int>>): Boolean {
        // Vérifier les bounds via BoundsValidator
        if (!BoundsValidator.validateCellBounds(cells, terrainHeight, terrainWidth)) {
            return false
        }

        // Vérifier les collisions
        for (cell in cells) {
            if (occupancyMap.containsKey(cell)) {
                return false
            }
        }

        return true
    }

    /**
     * Réinitialise la grille (supprime tous les bâtiments)
     */
    fun clear() {
        occupancyMap.clear()
    }
}
