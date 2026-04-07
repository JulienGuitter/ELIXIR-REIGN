package com.mjm.elixir_reign.core.terrain

import com.mjm.elixir_reign.shared.terrain.TerrainType
import com.mjm.elixir_reign.shared.world.WorldMap

object TerrainPresets {
    private const val DEFAULT_CHUNK_SIZE = 16

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

        return WorldMap.fromGroundRows(
            chunkSize = DEFAULT_CHUNK_SIZE,
            rows = listOf(
                listOf(G1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, S1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, W1, W3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S2, G1, G1, G1, G1, G1, G1, G1, G1, W1, W2, G1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, S1, S3, S3, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, S1, S2, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S3, S1, G1, G1, G1, G3, G1, G1, G1, W1, W1, G1, G1, G1, G1, S1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W3, W2, G1, G1, G1, G3, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(W1, W1, W2, W1, W1, W3, W1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, S1, G1, G1, G1, G1, G1, G1, G2, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, W1, W1, W1, W1, W1, W2, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W1, W2, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G3, G1, G1, G1, W1, W1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W1, W1, W3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, G1, G1, G1),
                listOf(G1, G1, G2, G1, G3, G1, G2, G1, G1, G1, S1, S1, W1, W1, W2, W1, W1, G1, G1, G1, G1, G1, W1, W1, W1, W1, W1, G1, W1, W1, W1, S1, S2, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S3, S2, S1, S1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, S1, S3, S1, G1, G1),
                listOf(G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S2, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W2, W1, W1, W1, W1, W1, S1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W2, W1, S1, S1, S3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S3, S1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, S1, S1, S3, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W3, W1, S1, S1, S1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S2, S1, S1, S1, W1, W1, W1, W2, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, W1, W1, W1, W2, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, S1, S3, G1, G1, G1, G1, G1, G2, G1, S1, S1, W1, W1, W1, W1, W1, W3, W1, W1, W1, W1, W2, W1, W1, W1, W1, W1, W1, W1, S1, S2, S1, G1, G1, G1, G3, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, S1, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1),
                listOf(G1, G1, G1, S1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S3, W1, W1, W1, W2, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W3, W1, W1, W1, W1, W1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S2, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W2, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G2, G1, G1, G1, G3, G1, G1, G1, S1, S1, S2, S1, S3, S1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, S1, S1, S1, S1, W1, W1, W1, W1, W1, G1, G1, W1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, W1, W3, G1, G1, G1, G1, G1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W2, W1, G1, G1, G1, G1, G1, S1, S2, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G3, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, G1, G1, G1, G3, G1, G1, G1, G2, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G3, G1, G1, G1, G1, G1, S3, S1, S2, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S3, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, S1, S1, G1, G1, G1, G3, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S2, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, S3, S1, W1, W1, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G3, G1, G1, G3, G1, G1, G1, W2, W1, W1, S1, S1, S1, S1, W1, W1, W2, W1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W1, G1, S1, S1, S2, S2, S1, G1, W1, W1, W2, W1, G1, G1, S3, S1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G3, G1, G1, G1),
                listOf(G1, G1, G1, G3, G1, G1, G2, G1, G1, G1, S3, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, S1, S1, S1, G1, G1, W1, W1, W1, W1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, W1, W3, W1, G1, G1, G1, G1, G1, G1, S1, G1, G3, G1, W1, W1, W1, G1, G1, S1, S1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, S1, S1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, S1, G1, G1, G1, G1, G1, W1, W3, W2, G1, G1, G1, G3, G1, G1, G1, G3, G1, G1, G1, G1, W2, W1, G1, G1, G1, G1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, W1, G1, G2, G1, G1, G1, G1, G1, G1, G1, G1, G2, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1),
                listOf(G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, W1, W3, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1, G1)
            )
        )
    }
}
