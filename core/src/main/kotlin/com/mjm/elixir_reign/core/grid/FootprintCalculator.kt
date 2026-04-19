package com.mjm.elixir_reign.core.grid

import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.data.BuildingStats

/**
 * Centralise tous les calculs de footprint (empreinte) des entités.
 *
 * SOURCE UNIQUE DE VÉRITÉ pour :
 * - Calcul des cellules occupées par un bâtiment/unité
 * - Récupération de la taille du footprint par EntityType
 *
 * Élimine la duplication entre BuildPlacementHandler et BuildingPlacementCalculator
 */
object FootprintCalculator {

    /**
     * Calcule les cellules occupées par une entité centrée à (centerRow, centerCol)
     * avec une taille de footprint donnée
     *
     * @param centerRow Ligne du centre
     * @param centerCol Colonne du centre
     * @param size Taille du footprint (doit être >= 1)
     * @return Liste des cellules (row, col) occupées
     */
    fun getFootprintCells(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
        val normalizedSize = size.coerceAtLeast(1)
        val startRow = centerRow - normalizedSize / 2
        val startCol = centerCol - normalizedSize / 2
        val cells = ArrayList<Pair<Int, Int>>(normalizedSize * normalizedSize)

        for (row in startRow until startRow + normalizedSize) {
            for (col in startCol until startCol + normalizedSize) {
                cells += Pair(row, col)
            }
        }

        return cells
    }

    /**
     * Récupère la taille du footprint pour une EntityType donnée
     *
     * @param entityType Type d'entité (bâtiment ou unité)
     * @return Taille du footprint en tuiles
     */
    fun getFootprintSize(entityType: EntityType): Int {
        return when (entityType) {
            EntityType.BARRACKS -> BuildingStats.BARRACKS.footprintSizeTiles
            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP.footprintSizeTiles
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP.footprintSizeTiles
            // Units: 1x1
            EntityType.BARBARIAN, EntityType.ARCHER, EntityType.GIANT -> 1
            else -> 1
        }
    }

    /**
     * Récupère le footprint d'une entité spécifique par type et étape
     *
     * @param entityType Type d'entité
     * @param centerRow Ligne du centre
     * @param centerCol Colonne du centre
     * @return Liste des cellules occupées
     */
    fun getFootprintCellsForEntity(entityType: EntityType, centerRow: Int, centerCol: Int): List<Pair<Int, Int>> {
        val size = getFootprintSize(entityType)
        return getFootprintCells(centerRow, centerCol, size)
    }

    /**
     * Vérifie si un type d'entité est un bâtiment
     */
    fun isBuildingType(entityType: EntityType): Boolean {
        return when (entityType) {
            EntityType.BARRACKS,
            EntityType.ELEXIR_PUMP,
            EntityType.DARCKELEXIR_PUMP -> true
            else -> false
        }
    }
}

