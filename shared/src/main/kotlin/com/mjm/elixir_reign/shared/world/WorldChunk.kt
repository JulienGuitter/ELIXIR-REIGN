package com.mjm.elixir_reign.shared.world

import com.mjm.elixir_reign.shared.terrain.TerrainType

class WorldChunk(
    val coord: ChunkCoord,
    val ground: TileGrid<TerrainType>,
    val entities: MutableList<Any> = mutableListOf(),
    val overlay: MutableList<Any> = mutableListOf()
) {
    val size: Int
        get() = ground.size

    val originRow: Int
        get() = coord.y * size

    val originCol: Int
        get() = coord.x * size
}
