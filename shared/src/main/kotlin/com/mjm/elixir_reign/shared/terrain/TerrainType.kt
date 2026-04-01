package com.mjm.elixir_reign.shared.terrain

enum class TerrainMaterial {
    GRASS,
    SAND,
    WATER
}

enum class TerrainType(
    val material: TerrainMaterial
) {
    GRASS_1(material = TerrainMaterial.GRASS),
    GRASS_2(material = TerrainMaterial.GRASS),
    GRASS_3(material = TerrainMaterial.GRASS),
    SAND_1(material = TerrainMaterial.SAND),
    SAND_2(material = TerrainMaterial.SAND),
    SAND_3(material = TerrainMaterial.SAND),
    WATER_1(material = TerrainMaterial.WATER),
    WATER_2(material = TerrainMaterial.WATER),
    WATER_3(material = TerrainMaterial.WATER);

    val isGrass: Boolean
        get() = material == TerrainMaterial.GRASS

    val isBlendTarget: Boolean
        get() = material != TerrainMaterial.GRASS
}
