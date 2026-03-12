package com.mjm.elixir_reign.core.terrain

enum class TerrainMaterial(val sheetColumn: Int) {
    GRASS(sheetColumn = 0),
    SAND(sheetColumn = 1),
    WATER(sheetColumn = 2)
}

enum class TerrainType(
    val id: Int,
    val material: TerrainMaterial,
    internal val topVariant: GroundTopVariant
) {
    GRASS_1(id = 1, material = TerrainMaterial.GRASS, topVariant = GroundTopVariant.VARIANT_0),
    GRASS_2(id = 2, material = TerrainMaterial.GRASS, topVariant = GroundTopVariant.VARIANT_1),
    GRASS_3(id = 3, material = TerrainMaterial.GRASS, topVariant = GroundTopVariant.VARIANT_2),
    SAND_1(id = 4, material = TerrainMaterial.SAND, topVariant = GroundTopVariant.VARIANT_0),
    SAND_2(id = 5, material = TerrainMaterial.SAND, topVariant = GroundTopVariant.VARIANT_1),
    SAND_3(id = 6, material = TerrainMaterial.SAND, topVariant = GroundTopVariant.VARIANT_2),
    WATER_1(id = 7, material = TerrainMaterial.WATER, topVariant = GroundTopVariant.VARIANT_0),
    WATER_2(id = 8, material = TerrainMaterial.WATER, topVariant = GroundTopVariant.VARIANT_1),
    WATER_3(id = 9, material = TerrainMaterial.WATER, topVariant = GroundTopVariant.VARIANT_2);

    val isGrass: Boolean
        get() = material == TerrainMaterial.GRASS

    val isBlendTarget: Boolean
        get() = material != TerrainMaterial.GRASS

    companion object {
        fun fromId(id: Int): TerrainType {
            return entries.firstOrNull { it.id == id }
                ?: error("ID de tile inconnu: $id")
        }
    }
}
