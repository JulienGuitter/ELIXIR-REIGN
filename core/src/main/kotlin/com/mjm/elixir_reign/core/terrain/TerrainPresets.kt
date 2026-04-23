package com.mjm.elixir_reign.core.terrain

import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.world.WorldMap

object TerrainPresets {
    private const val DElFAULT_CHUNK_SIZE = 16

    fun map(): WorldMap {
        val G1 = TerrainType.GRASS_1
        val G2 = TerrainType.GRASS_2
        val G3 = TerrainType.GRASS_3
        val S1 = TerrainType.SAND_1
        val S2 = TerrainType.SAND_2
        val S3 = TerrainType.SAND_3
        val W1 = TerrainType.WATER_1
        val W2 = TerrainType.WATER_2
        val W3 = TerrainType.WATER_3
        val Go = TerrainType.GOLD
        val El = TerrainType.ELEXIR
        val DEl = TerrainType.DARK_ELEXIR

        return WorldMap.fromGroundRows(
            chunkSize = DElFAULT_CHUNK_SIZE,
            rows = listOf(
                listOf(S1, S1, G1, G1, G1, G1, G3, G3, G2, G3, G1, G1, G1, G1, G1, G1, G1, G1, G2, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W3, G2, G1, G1, G1, G1, G1, G1, G2, G3, G1, G1, G1, G1, G3, G1, G1, G3),
                listOf(S1, S1, S1, G1, G1, G1, G1, G3, G1, G1, G1, G3, Go, G1, G1, G3, G1, G1, G1, G3, G1, G1, G3, G1, G1, G1, G1, G1, G2, W2, W1, W1, G2, G1, G1, G1, G1, G1, G2, G3, G1, G1, G2, G1, G1, G3, G1, G2, G1, G1),
                listOf(S1, S2, S2, S1, G2, G3, G1, G1, G1, Go, Go, Go, Go, G3, G1, G3, G1, G3, G1, G1, G1, G1, Go, Go, El, El, G1, G3, W3, W1, W1, W1, G1, G1, G1, G2, G1, G2, G1, G1, G3, G1, El, G1, G1, G1, G1, G3, G1, G1),
                listOf(G1, S1, S1, S1, G3, G1, G1, G1, G3, G1, Go, Go, Go, G1, G1, G3, G1, G2, G2, G1, G1, G1, Go, Go, Go, El, G1, G1, W1, W1, W1, G1, G1, G1, G3, G3, G1, G1, G1, G2, G3, G1, El, El, El, Go, Go, Go, G1, G2),
                listOf(G1, S1, S2, S1, S1, G1, G1, G3, G1, Go, Go, G2, G2, G1, G3, G2, G1, G1, G1, G1, G2, G1, G1, G2, El, El, G3, G1, W1, W1, W3, G1, G1, G1, G1, G1, G1, G3, G1, G3, G1, G2, G1, El, El, Go, Go, Go, G1, G1),
                listOf(G1, G1, S1, S3, G2, G3, G3, G1, G1, G2, G1, G1, G2, G1, G1, G2, G1, G1, G3, S2, S1, G3, G1, G1, G1, G1, G1, G1, W1, W1, W1, G3, G1, G1, G1, G2, G1, G1, G2, G3, G3, G3, G1, G2, G3, G3, Go, Go, G1, G1),
                listOf(G1, G1, G1, G1, G2, G1, G2, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G3, S3, S1, S1, S1, G2, G3, G1, G3, G1, G1, G1, W3, W1, W1, G1, G3, G1, G1, G1, G2, G3, G1, S2, G1, G1, G2, G2, G1, Go, Go, G1, G1),
                listOf(G1, G1, G1, G2, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G3, G1, G1, S1, S1, S1, G1, G1, G1, G1, G3, G2, G3, G3, W2, W3, W3, G1, G1, G2, G3, G3, G3, G1, S1, S1, S1, G2, G1, G1, G1, G1, G3, G2, G1),
                listOf(G2, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G2, S1, G1, G2, G1, G1, G2, G1, G3, G1, G1, W1, W2, W1, G2, G1, G3, G3, G1, G3, G1, S2, S1, S1, G1, G1, G2, G2, G1, G1, G1, G1),
                listOf(G1, G1, G1, G3, G1, G1, G1, G1, G1, G3, G1, G2, G1, G1, G1, G1, G1, G3, G2, G1, G1, G2, G1, G1, G3, G1, G2, G1, G3, G1, W1, W1, W1, G2, G1, G1, G1, G1, G3, S3, S1, S1, G1, G1, G1, G3, G1, G2, G1, G1),
                listOf(G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G2, G1, G1, G1, G1, G1, G2, G1, G3, G1, G1, G1, G1, G1, G1, G1, W1, W2, W1, G3, G1, G1, G1, G1, G1, G2, S3, S2, S3, G1, G1, G2, G1, G1, G1, G2),
                listOf(G2, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, El, El, G1, G1, G1, G1, G1, G1, G1, G2, G3, G2, G1, G3, G1, G1, G2, G1, G1, W1, W2, W3, W3, G1, G1, G1, G3, G3, G1, G1, S1, G1, G1, G1, G1, G1, G1, G2, G1),
                listOf(G1, G1, G1, G2, G1, G1, G1, G1, G2, G3, El, El, El, El, G2, G1, G3, G2, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, W1, W2, W1, G1, G1, G3, G2, G1, G3, G1, G2, G1, G1, G1, G3, G3, G1, G1, G1),
                listOf(G1, G1, G3, G1, G1, G1, G1, G2, G3, G1, El, G2, El, El, G1, G1, G1, G1, G2, G1, G1, G2, G2, G1, G1, G3, S3, S3, G1, G2, G1, W1, W1, W3, G1, G3, G1, G1, G1, G1, G1, G1, G2, G1, G3, G2, El, El, G3, G1),
                listOf(G1, G1, G1, G1, G3, G3, G1, G2, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G2, S2, S1, S1, S1, S1, S2, S3, G1, G1, G1, W3, G1, G1, G1, G2, G1, G1, G3, G3, G1, G3, G1, G1, G2, El, El, G1, G1),
                listOf(G1, G1, G1, G1, Go, G1, G1, G3, G1, G1, G1, G1, G3, G2, G3, G1, G1, G1, W1, W3, W2, S1, S1, S2, S2, S3, W1, W1, W1, W1, W1, W1, W3, W1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G2, G1, G1, El, El, G1, G3),
                listOf(G1, G1, Go, Go, Go, G1, G1, G1, W1, W1, W1, W1, W3, W2, W3, W1, W3, W1, W1, W1, W1, W2, S1, S1, W1, W1, W2, W1, W3, W2, W1, W1, W3, W1, S1, G1, G3, G1, G2, G1, G1, G1, G1, G3, G3, G1, El, G1, G1, G1),
                listOf(G1, G1, Go, Go, G1, G1, W1, W3, W1, G1, S1, S2, S2, S1, S1, W3, W2, W2, W1, W1, W1, W1, W1, W1, W2, W2, W1, W1, W1, W1, W1, W1, W1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, El, G1, G1, G1),
                listOf(G1, G1, G3, G1, G1, W1, W1, W1, G1, G3, G1, G3, S2, S1, S1, S3, W1, W2, W1, W1, W1, W2, W1, S1, S1, W1, W1, W1, W2, W1, W3, W3, W2, S1, S1, S1, G1, G2, G1, G1, G1, G1, G3, G1, G3, G2, G3, G1, G1, G1),
                listOf(G1, G1, W1, W1, W3, W3, W1, G1, G3, G1, G1, G2, G1, G1, S1, S2, G1, W2, W2, W1, W1, W1, S1, DEl, DEl, S1, W1, W2, W1, W1, W1, W1, W1, W1, S1, S1, G1, G1, G2, G3, G3, G2, G1, G1, G3, G1, G2, G1, G1, G3),
                listOf(W1, W1, W3, W1, W1, G2, G1, G1, G1, G1, G2, G3, G1, G1, G1, G2, G1, W1, W3, W1, W1, W1, S1, DEl, G2, G1, S1, W1, W1, W3, W1, W2, W3, W1, W1, G2, G3, G1, G1, G3, G1, G3, G3, G1, G3, G1, G3, G2, G3, G1),
                listOf(W1, W1, W1, G2, G1, G1, G3, G1, G3, G2, G1, G3, G2, G2, G1, G1, G1, G1, W1, W1, W1, S1, G2, S1, G1, G1, S1, W1, W3, W1, W1, W1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G3),
                listOf(W3, G1, G1, G3, G3, G1, G1, G1, G2, G1, G2, G1, G1, G1, G3, G1, G1, G1, W1, W1, W2, W1, S1, W1, S2, S1, W2, W1, W2, W2, W1, W1, W1, W2, W1, G1, G1, G1, G1, G1, Go, Go, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G2, G2, G1, G2, G1, G2, G2, G3, G1, G1, G1, G3, G1, G3, G2, G1, G1, G3, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S1, W1, W1, G1, G1, G3, G1, G3, G3, G3, Go, Go, G1, G2, G1, G1, G1, G1, G1),
                listOf(G1, G3, G1, G1, G1, G3, G1, G1, G1, G1, G2, G2, G1, G2, G3, G1, G2, G1, G1, W1, W1, W1, W3, W1, W2, W1, W1, W1, W1, W1, S3, S1, G1, G1, G1, G1, G1, G1, G1, G3, Go, Go, Go, Go, G3, G1, G3, G1, G1, G1),
                listOf(G1, G1, El, El, G3, G3, G3, G2, G3, G1, G1, G2, G1, G2, G1, G1, G1, G1, W2, W3, W2, W1, W2, W1, W1, W1, W1, W1, S1, S2, S1, G1, G1, G2, G1, G1, G3, G1, G1, G1, Go, Go, G1, G1, G1, G1, G1, G1, G3, G1),
                listOf(G2, El, El, El, G2, G1, G1, G3, G2, G3, G3, G1, G1, G1, G1, Go, Go, W1, W1, W1, W1, W1, W2, W1, S3, S3, S2, S1, S1, S1, S2, G1, G3, G2, G2, G1, G1, G3, G1, G1, Go, Go, G1, G1, G1, G1, G1, G3, G1, G1),
                listOf(G1, El, El, El, G1, G1, G1, G3, G1, G1, G1, G3, G3, G1, Go, Go, Go, W1, W2, W1, S1, S1, S1, S3, S3, S2, S1, S2, S3, S1, G1, G3, G1, G1, G2, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1),
                listOf(G1, G1, El, El, G1, G2, G1, G3, G1, S1, S3, G1, G3, G3, Go, Go, W2, W1, W3, S1, S1, S1, S3, S3, S1, S1, S1, S2, S1, G1, G1, G1, G3, G1, G2, G2, G3, G3, G1, G1, G1, G1, G1, G1, G2, G3, G1, G3, G1, G1),
                listOf(G3, G1, G1, G2, G1, G2, G1, G1, S2, S1, S2, S1, G1, G2, G2, Go, W1, W1, W1, G1, S1, S3, S1, S1, S1, G1, G1, G3, G1, G2, G2, G2, G1, G1, G1, G1, G3, G1, G1, G3, G1, G1, G3, G1, G1, G1, G1, G1, G2, G1),
                listOf(G1, G1, G3, G1, G2, G1, G1, S1, S1, S1, S1, S1, G2, G1, G1, G1, W1, W3, W1, G1, G1, G1, G1, G1, G1, G3, G1, G3, G1, G3, G1, G2, G1, G3, G1, G2, G1, G1, G1, G1, G2, G3, G2, G2, G1, G1, G2, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, S3, S2, S1, S2, G1, G1, G1, G3, W1, W1, W1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G3, G1, G1, G2, G3, G1, G1, G2, G1, G2, G2, G1, G3, G1, G1, G3, G1, G1, G1, G1, G2),
                listOf(G1, G3, G2, G1, G1, G1, G2, G3, S1, S1, S1, G2, G1, G1, G1, G1, W1, W1, W3, G1, G1, G1, G3, G1, G2, G3, G1, G1, G1, El, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G2, G2, G1, G1, G1, G3, G1, G2),
                listOf(G1, G1, G1, G2, G3, G1, G1, G1, S1, S1, S1, G2, G2, G1, G2, G1, W2, W2, W2, W1, G1, G3, G1, G1, G3, G1, G1, G1, El, El, El, G1, G2, G1, G3, G1, G1, G3, G1, G3, G2, G1, G1, G1, G1, G3, G1, G2, G2, G2),
                listOf(G2, G1, G1, G1, G1, G1, S3, S1, S1, S1, G2, G1, G1, G1, G3, G1, G1, W1, W1, W2, G3, G2, G1, G2, G1, G1, G3, G1, El, El, El, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G3, G2, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G2, G3, G1, S1, S1, S1, S2, G1, G1, G1, G1, G1, G2, G1, G1, G1, W1, W1, W1, G1, G3, G2, G1, G1, G1, G1, G3, G1, G1, G1, G2, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G2, El, G1, G3),
                listOf(G1, G2, G1, G3, G1, S2, S1, S1, G3, G1, G2, G1, G1, G1, G1, G2, G2, G2, W1, W2, W1, W1, G1, G1, G1, G2, G1, G1, G3, G2, G1, G1, G2, G1, G1, S1, G3, G1, G2, G1, G1, G2, G3, G1, G1, G2, El, El, El, G1),
                listOf(G1, G3, G1, G2, G1, S2, S1, S1, G1, G1, G1, G1, G2, G3, G3, G1, G3, G1, G2, W1, W1, W1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, S1, S3, S1, G1, G1, G1, G3, G3, G1, G1, G1, G1, El, El, El, G1),
                listOf(G1, G1, G1, G1, G1, G1, S2, G3, G3, G1, G1, G1, El, El, G1, G1, G1, G3, G1, G1, W1, W1, W1, G1, G1, G1, G1, G3, G3, G1, G1, G2, G1, G2, S1, S1, S2, S1, S1, S1, S1, G1, G3, G3, G2, G1, El, El, El, G1),
                listOf(G2, G3, G1, G2, G1, G1, G1, G2, G1, G1, G2, G1, El, El, G2, G2, G1, G1, G3, G1, W1, W1, W1, G1, G3, G1, G1, G1, G1, G3, G1, G1, G3, G1, G3, S1, S2, S1, S3, S1, S1, S3, G1, G1, G3, G1, G1, G3, G2, G1),
                listOf(G2, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, El, El, El, G1, G2, G1, G3, G1, G3, W1, W1, W1, G1, G1, G1, G1, G1, G3, G1, G3, G1, G1, G3, G1, G2, G1, S1, S2, S3, S1, S1, G1, G1, G2, G1, G3, G1, G2, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G2, G3, G1, G3, G1, G3, G1, G1, G1, G2, G2, G1, G1, W3, W3, W3, G2, G3, G1, G1, G3, G2, G2, G1, G1, G1, G1, G1, G1, G1, G2, G1, S1, S1, S1, G1, G3, G3, G1, G1, G1, G1, G1),
                listOf(G3, G1, G2, G3, G3, G3, G1, G1, G3, G1, G1, G1, G1, G3, G1, G1, G1, G1, G3, W1, W2, W1, W1, G1, G1, G1, G3, G1, G1, G1, G2, G1, G2, G1, G1, G1, G2, G1, G1, S1, S1, S1, G3, G1, G1, G1, G1, G2, G1, G3),
                listOf(G1, G1, G3, G2, G3, G3, G1, G1, G1, G1, G1, G3, G2, G1, G2, G1, G1, G1, W1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G2, G1, G1, Go, Go, G1, G1, G1, G3, G1, G2, G3, S2, G2, G1, G3, G1, G1, G3, G3, G1, G1),
                listOf(G1, G1, G1, G1, G3, G1, G3, G2, G3, G3, G2, G1, G1, G1, G1, G2, W1, W1, W1, W1, W2, G1, G2, G2, G1, G2, G1, G1, G1, Go, Go, Go, Go, G1, G1, G3, G1, G1, G3, G1, G2, G1, G1, G2, G1, G1, G1, G1, G1, G1),
                listOf(G2, Go, Go, Go, Go, G1, G1, G1, G1, G3, G3, G1, G1, G1, G1, W1, W2, W1, W1, W1, G1, G1, G1, G3, G1, G1, G1, G1, G1, Go, G1, G1, G2, G2, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, Go, Go, G2, G1),
                listOf(G1, G1, G1, Go, Go, G1, G3, G1, G1, G3, G1, G3, G1, G1, G1, W1, W1, W2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, Go, G1, G2, G1, G3, G1, G1, G3, G2, G2, G1, G3, G1, G1, G1, G1, Go, Go, Go, Go, G1),
                listOf(G1, G1, G1, Go, Go, G1, G1, G1, G1, G2, G1, G1, G1, G1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G3, G1, G2, G3, G1, G1, G1, G1, G1, G1, G1, G1, G3, G3, Go, Go, Go, Go, G2, G1),
                listOf(G1, G1, G1, G1, G1, Go, G1, G3, G1, G1, G3, G3, G1, W2, W1, W1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G3, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, Go, Go, Go, Go, G1, G1, G1),
                listOf(G2, G1, G3, G1, G1, G1, G3, G1, G1, G3, G1, G1, W1, W1, W1, W1, G1, G1, G2, G1, G3, G1, G1, G1, G2, G2, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1)
            )
        )
    }
}
