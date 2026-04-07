package com.mjm.elixir_reign.shared.world

class TileGrid<T> private constructor(
    val size: Int,
    private val cells: Array<Any?>
) {
    init {
        require(size > 0) { "La grille doit avoir une taille strictement positive." }
        require(cells.size == size * size) { "La grille doit contenir size * size cellules." }
    }

    operator fun get(row: Int, col: Int): T? {
        if (row !in 0 until size || col !in 0 until size) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        return cells[index(row, col)] as T?
    }

    operator fun set(row: Int, col: Int, value: T?) {
        require(row in 0 until size && col in 0 until size) {
            "Coordonnees locales hors de la grille: row=$row, col=$col, size=$size"
        }
        cells[index(row, col)] = value
    }

    fun forEachIndexed(action: (row: Int, col: Int, value: T?) -> Unit) {
        for (row in 0 until size) {
            for (col in 0 until size) {
                action(row, col, get(row, col))
            }
        }
    }

    private fun index(row: Int, col: Int): Int {
        return row * size + col
    }

    companion object {
        fun <T> empty(size: Int): TileGrid<T> {
            return TileGrid(size = size, cells = arrayOfNulls(size * size))
        }
    }
}
