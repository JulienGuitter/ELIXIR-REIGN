package com.mjm.elixir_reign.shared.ecs.systems

import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.logic.IsometricGeometry
import com.mjm.elixir_reign.shared.world.GridOccupancyData
import com.mjm.elixir_reign.shared.world.WorldMap

/**
 * Système de placement minimaliste :
 * - valide bounds + terrain + collisions
 * - crée l'entité via callback injecté
 * - met à jour l'occupation de grille
 */
class PlacementSystem(
	private val worldMap: WorldMap,
	private val geometry: IsometricGeometry,
	private val occupancy: GridOccupancyData,
	private val spawnBuilding: (EntityType, Float, Float) -> Unit
) {

	data class BuildingToPlace(
		val entityType: EntityType,
		val stats: BuildingStats
	)

	fun canPlace(row: Int, col: Int, building: BuildingToPlace): Boolean {
		val cells = footprint(row, col, building.stats.footprintSizeTiles)
		if (cells.isEmpty()) {
			return false
		}

		for ((r, c) in cells) {
			if (!occupancy.isInBounds(r, c)) {
				return false
			}

			val terrain = worldMap[r, c] ?: return false
			if (!terrain.canBuildOn) {
				return false
			}
		}

		return occupancy.canOccupy(cells)
	}

	fun place(row: Int, col: Int, building: BuildingToPlace): Boolean {
		if (!canPlace(row, col, building)) {
			return false
		}

		val cells = footprint(row, col, building.stats.footprintSizeTiles)
		val world = computePlacementWorldPosition(row, col)

		return try {
			spawnBuilding(building.entityType, world.x, world.y)
			occupancy.occupy(cells)
		} catch (_: Exception) {
			false
		}
	}

	fun computePlacementWorldPosition(row: Int, col: Int): Vector2 {
		val center = geometry.gridToWorld(row, col + 1)
		return Vector2(center.x, center.y - geometry.halfTileHeight)
	}

	private fun footprint(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
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
}


