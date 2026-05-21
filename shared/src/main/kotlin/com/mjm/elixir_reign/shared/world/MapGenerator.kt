package com.mjm.elixir_reign.shared.world

import com.mjm.elixir_reign.shared.worldgen.MapGenConfig
import com.mjm.elixir_reign.shared.worldgen.ProceduralTerrainGenerator

object MapGenerator {
    private const val DEFAULT_CHUNK_SIZE = 16
    private const val DEFAULT_SIZE = 100

    fun generateGameMap(): WorldMap {
        return ProceduralTerrainGenerator.generate(
            MapGenConfig(
                width = DEFAULT_SIZE,
                height = DEFAULT_SIZE,
                chunkSize = DEFAULT_CHUNK_SIZE
            )
        )
    }
}
