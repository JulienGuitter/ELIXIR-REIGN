package com.mjm.elixir_reign.shared.worldgen

class MutableMaterialGrid(
    val width: Int,
    val height: Int,
    initialMaterial: MaterialType = MaterialType.GRASS
) {
    private val cells = IntArray(width * height) { initialMaterial.ordinal }

    init {
        require(width > 0) { "width doit etre strictement positif." }
        require(height > 0) { "height doit etre strictement positif." }
    }

    operator fun get(row: Int, col: Int): MaterialType? {
        if (!inBounds(row, col)) {
            return null
        }

        return MaterialType.entries[cells[index(row, col)]]
    }

    operator fun set(row: Int, col: Int, material: MaterialType) {
        require(inBounds(row, col)) {
            "Coordonnees hors de la grille: row=$row, col=$col, width=$width, height=$height"
        }
        cells[index(row, col)] = material.ordinal
    }

    fun fill(material: MaterialType) {
        cells.fill(material.ordinal)
    }

    fun inBounds(row: Int, col: Int): Boolean {
        return row in 0 until height && col in 0 until width
    }

    fun forEachIndexed(action: (row: Int, col: Int, material: MaterialType) -> Unit) {
        for (row in 0 until height) {
            for (col in 0 until width) {
                action(row, col, MaterialType.entries[cells[index(row, col)]])
            }
        }
    }

    fun count(predicate: (MaterialType) -> Boolean): Int {
        var total = 0
        cells.forEach { ordinal ->
            if (predicate(MaterialType.entries[ordinal])) {
                total++
            }
        }
        return total
    }

    private fun index(row: Int, col: Int): Int {
        return row * width + col
    }
}
