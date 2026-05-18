package com.mjm.elixir_reign.shared.world

import com.mjm.elixir_reign.shared.terrain.TerrainType

class WorldMap private constructor(
    val chunkSize: Int,
    val width: Int,
    val height: Int,
    val chunks: Map<ChunkCoord, WorldChunk>
) {
    init {
        require(chunkSize > 0) { "La taille d'un chunk doit etre strictement positive." }
        require(width > 0) { "La map doit avoir au moins une colonne." }
        require(height > 0) { "La map doit avoir au moins une ligne." }
    }

    val chunkWidth: Int = (width + chunkSize - 1) / chunkSize
    val chunkHeight: Int = (height + chunkSize - 1) / chunkSize

    operator fun get(row: Int, col: Int): TerrainType? {
        val coord = worldToChunkCoord(row = row, col = col) ?: return null
        val chunk = chunks[coord] ?: return null
        return chunk.ground[row % chunkSize, col % chunkSize]
    }

    fun chunkAt(coord: ChunkCoord): WorldChunk? {
        return chunks[coord]
    }

    fun allChunks(): Collection<WorldChunk> {
        return chunks.values
    }

    fun worldToChunkCoord(row: Int, col: Int): ChunkCoord? {
        if (row !in 0 until height || col !in 0 until width) {
            return null
        }

        return ChunkCoord(
            x = col / chunkSize,
            y = row / chunkSize
        )
    }

    companion object {
        fun build(
            width: Int,
            height: Int,
            chunkSize: Int,
            tileProvider: (row: Int, col: Int) -> TerrainType
        ): WorldMap {
            require(width > 0) { "La map doit avoir au moins une colonne." }
            require(height > 0) { "La map doit avoir au moins une ligne." }
            require(chunkSize > 0) { "La taille d'un chunk doit etre strictement positive." }

            val chunkWidth = (width + chunkSize - 1) / chunkSize
            val chunkHeight = (height + chunkSize - 1) / chunkSize
            val chunks = linkedMapOf<ChunkCoord, WorldChunk>()

            for (chunkY in 0 until chunkHeight) {
                for (chunkX in 0 until chunkWidth) {
                    val ground = TileGrid.empty<TerrainType>(size = chunkSize)

                    for (localRow in 0 until chunkSize) {
                        for (localCol in 0 until chunkSize) {
                            val worldRow = chunkY * chunkSize + localRow
                            val worldCol = chunkX * chunkSize + localCol

                            val tile = if (worldRow in 0 until height && worldCol in 0 until width) {
                                tileProvider(worldRow, worldCol)
                            } else {
                                null
                            }

                            ground[localRow, localCol] = tile
                        }
                    }

                    val coord = ChunkCoord(x = chunkX, y = chunkY)
                    chunks[coord] = WorldChunk(
                        coord = coord,
                        ground = ground
                    )
                }
            }

            return WorldMap(
                chunkSize = chunkSize,
                width = width,
                height = height,
                chunks = chunks
            )
        }

        fun fromGroundRows(
            rows: List<List<TerrainType>>,
            chunkSize: Int
        ): WorldMap {
            require(rows.isNotEmpty()) { "La map ne peut pas etre vide." }
            require(chunkSize > 0) { "La taille d'un chunk doit etre strictement positive." }

            val height = rows.size
            val width = rows.firstOrNull()?.size ?: 0

            require(width > 0) { "La map doit avoir au moins une colonne." }
            require(rows.all { it.size == width }) { "Toutes les lignes doivent avoir la meme largeur." }
            return build(
                width = width,
                height = height,
                chunkSize = chunkSize
            ) { row, col ->
                rows[row][col]
            }
        }
    }
}
