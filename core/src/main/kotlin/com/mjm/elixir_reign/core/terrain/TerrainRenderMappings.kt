package com.mjm.elixir_reign.core.terrain

import com.mjm.elixir_reign.shared.terrain.TerrainMaterial
import com.mjm.elixir_reign.shared.terrain.TerrainType

internal val TerrainMaterial.sheetColumn: Int
    get() = when (this) {
        TerrainMaterial.GRASS -> 0
        TerrainMaterial.SAND -> 1
        TerrainMaterial.WATER -> 2
    }

internal val TerrainType.topVariant: GroundTopVariant
    get() = when (this) {
        TerrainType.GRASS_1,
        TerrainType.SAND_1,
        TerrainType.WATER_1 -> GroundTopVariant.VARIANT_0

        TerrainType.GRASS_2,
        TerrainType.SAND_2,
        TerrainType.WATER_2 -> GroundTopVariant.VARIANT_1

        TerrainType.GRASS_3,
        TerrainType.SAND_3,
        TerrainType.WATER_3 -> GroundTopVariant.VARIANT_2
    }
