package com.mjm.elixir_reign.shared.logic

import com.mjm.elixir_reign.shared.world.WorldMap

/**
 * Valide si un bâtiment peut être placé sur la grille
 *
 * Responsabilités :
 * - Vérifier que le terrain autorise la construction (grass/sand = OK, water = NON)
 * - Déléguer l'occupation à IsometricGrid
 *
 * Cette classe contient la LOGIQUE MÉTIER du jeu et doit être partagée
 * entre le client (core) et le serveur (server)
 */
class PlacementValidator(
    private val worldMap: WorldMap,
    private val isometricGrid: IsometricGrid
) {

    /**
     * Valide si un bâtiment peut être placé
     *
     * Délègue les vérifications de bounds/occupation à IsometricGrid
     * Ajoute la vérification du terrain (grass/sand autorisés, water refusé)
     *
     * @param building Le bâtiment à placer
     * @return Pair(isValid, reason) - raison du refus si invalide
     */
    fun canPlaceBuilding(building: BuildingOccupancy): Pair<Boolean, String?> {
        // 1. Vérifier d'abord le terrain (validation métier spécifique au jeu)
        val occupiedCells = building.getOccupiedCells()
        for ((row, col) in occupiedCells) {
            val terrainType = worldMap[row, col] ?: return Pair(false, "Type de terrain introuvable")
            if (!terrainType.canBuildOn) {
                return Pair(false, "Impossible de construire sur $terrainType")
            }
        }

        // 2. Déléguer à IsometricGrid pour bounds + occupation
        if (!isometricGrid.canPlaceBuilding(building)) {
            return Pair(false, "Position invalide (hors limites ou déjà occupée)")
        }

        return Pair(true, null)
    }

    /**
     * Valide et place le bâtiment en une seule opération
     *
     * @param building Le bâtiment à placer
     * @return true si succès, false si refusé
     */
    fun validateAndPlace(building: BuildingOccupancy): Boolean {
        val (isValid) = canPlaceBuilding(building)
        if (!isValid) return false

        return isometricGrid.placeBuilding(building)
    }
}
