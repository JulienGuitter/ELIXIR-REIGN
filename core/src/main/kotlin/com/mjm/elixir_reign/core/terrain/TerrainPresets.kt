package com.mjm.elixir_reign.core.terrain

import com.mjm.elixir_reign.shared.world.WorldMap
import com.mjm.elixir_reign.shared.worldgen.MapGenConfig
import com.mjm.elixir_reign.shared.worldgen.ProceduralTerrainGenerator

object TerrainPresets {
    private const val DEFAULT_CHUNK_SIZE = 16
    private const val DEFAULT_MAP_SIZE = 100

    fun map(
        width: Int = DEFAULT_MAP_SIZE,
        height: Int = DEFAULT_MAP_SIZE,
        seed: Long = MapGenConfig.DEFAULT_SEED
    ): WorldMap {
        return ProceduralTerrainGenerator.generate(
            MapGenConfig(
                width = width,
                height = height,
                chunkSize = DEFAULT_CHUNK_SIZE,
                seed = seed
            )
        )
    }
}
