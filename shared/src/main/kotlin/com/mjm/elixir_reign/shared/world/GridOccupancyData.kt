package com.mjm.elixir_reign.shared.world

/**
 * Source de vérité minimale pour l'occupation de la grille.
 *
 * Cette structure est volontairement simple : uniquement des cellules bloquées.
 */
class GridOccupancyData(
	private val rows: Int,
	private val cols: Int
) {
	private val occupied = mutableSetOf<Pair<Int, Int>>()

	fun isInBounds(row: Int, col: Int): Boolean {
		return row in 0 until rows && col in 0 until cols
	}

	fun isOccupied(row: Int, col: Int): Boolean {
		return Pair(row, col) in occupied
	}

	fun canOccupy(cells: Iterable<Pair<Int, Int>>): Boolean {
		for ((row, col) in cells) {
			if (!isInBounds(row, col) || isOccupied(row, col)) {
				return false
			}
		}
		return true
	}

	fun occupy(cells: Iterable<Pair<Int, Int>>): Boolean {
		if (!canOccupy(cells)) {
			return false
		}
		occupied.addAll(cells)
		return true
	}

	fun free(cells: Iterable<Pair<Int, Int>>) {
		for (cell in cells) {
			occupied.remove(cell)
		}
	}

	fun clear() {
		occupied.clear()
	}
}
