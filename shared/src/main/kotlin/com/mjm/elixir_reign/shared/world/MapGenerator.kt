package com.mjm.elixir_reign.shared.world

import com.mjm.elixir_reign.shared.terrain.TerrainType

object MapGenerator {
    private const val DEFAULT_CHUNK_SIZE = 16
    private const val DEFAULT_SIZE = 64

    fun generateGameMap(): WorldMap {
        // TODO: remplacer par le generateur de map de l'autre branche.
        val rows = List(DEFAULT_SIZE) { row ->
            List(DEFAULT_SIZE) { col ->
                when {
                    row in 28..35 && col in 28..35 -> TerrainType.WATER_1
                    (row + col) % 23 == 0 -> TerrainType.SAND_2
                    (row * 31 + col * 17) % 47 == 0 -> TerrainType.GOLD
                    (row * 13 + col * 19) % 53 == 0 -> TerrainType.ELEXIR
                    (row + col) % 3 == 0 -> TerrainType.GRASS_2
                    (row + col) % 5 == 0 -> TerrainType.GRASS_3
                    else -> TerrainType.GRASS_1
                }
            }
        }

        return WorldMap.fromGroundRows(
            rows = rows,
            chunkSize = DEFAULT_CHUNK_SIZE
        )
    }
}
