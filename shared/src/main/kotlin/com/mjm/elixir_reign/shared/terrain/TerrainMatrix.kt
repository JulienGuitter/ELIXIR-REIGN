package com.mjm.elixir_reign.shared.terrain

class TerrainMatrix(
    private val rows: List<List<TerrainType>>
) {
    val height: Int = rows.size
    val width: Int = rows.firstOrNull()?.size ?: 0

    init {
        require(rows.isNotEmpty()) { "La matrice de terrain ne peut pas etre vide." }
        require(width > 0) { "La matrice de terrain doit avoir au moins une colonne." }
        require(rows.all { it.size == width }) { "Toutes les lignes doivent avoir la meme largeur." }
    }

    operator fun get(row: Int, col: Int): TerrainType? {
        if (row !in 0 until height || col !in 0 until width) {
            return null
        }
        return rows[row][col]
    }
}
