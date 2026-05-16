package com.mjm.elixir_reign.shared.worldgen

import kotlin.math.max

data class MapGenConfig(
    val width: Int = 50,
    val height: Int = 50,
    val chunkSize: Int = 16,
    val seed: Long = DEFAULT_SEED,
    val spawnZoneSize: Int = 5,
    val lakeRadiusFraction: Float = 0.17f,
    val lakeRoughness: Float = 0.22f,
    val lakePointCount: Int = 32,
    val islandRadiusFraction: Float = 0.07f,
    val islandRoughness: Float = 0.18f,
    val islandPointCount: Int = 24,
    val riverWidthMin: Int = 2,
    val riverWidthMax: Int = 2,
    val riverCurviness: Float = 0.28f,
    val sandCoverage: Float = 0.09f,
    val goldRarity: Float = 0.52f,
    val elixirRarity: Float = 0.68f,
    val darkElixirPatchSize: Int = 5,
    val maxGenerationAttempts: Int = 24
) {
    init {
        require(width > 0) { "width doit etre strictement positif." }
        require(height > 0) { "height doit etre strictement positif." }
        require(chunkSize > 0) { "chunkSize doit etre strictement positif." }
        require(spawnZoneSize > 0) { "spawnZoneSize doit etre strictement positif." }
        require(riverWidthMin in 1..2) { "riverWidthMin doit etre entre 1 et 2." }
        require(riverWidthMax in 1..2) { "riverWidthMax doit etre entre 1 et 2." }
        require(riverWidthMin <= riverWidthMax) { "riverWidthMin ne peut pas exceder riverWidthMax." }
        require(sandCoverage in 0f..0.45f) { "sandCoverage doit etre entre 0 et 0.45." }
        require(goldRarity in 0f..1f) { "goldRarity doit etre entre 0 et 1." }
        require(elixirRarity in 0f..1f) { "elixirRarity doit etre entre 0 et 1." }
        require(lakeRadiusFraction in 0.08f..0.35f) { "lakeRadiusFraction doit rester raisonnable." }
        require(islandRadiusFraction in 0.02f..0.16f) { "islandRadiusFraction doit rester raisonnable." }
        require(lakeRoughness in 0f..0.45f) { "lakeRoughness doit etre entre 0 et 0.45." }
        require(islandRoughness in 0f..0.45f) { "islandRoughness doit etre entre 0 et 0.45." }
        require(lakePointCount >= 12) { "lakePointCount doit etre >= 12." }
        require(islandPointCount >= 12) { "islandPointCount doit etre >= 12." }
        require(darkElixirPatchSize > 0) { "darkElixirPatchSize doit etre strictement positif." }
        require(maxGenerationAttempts > 0) { "maxGenerationAttempts doit etre strictement positif." }

        val minRequiredDimension = max(spawnZoneSize * 2 + 12, 24)
        require(width >= minRequiredDimension && height >= minRequiredDimension) {
            "La map est trop petite pour conserver les coins, le croisement central et les rivieres."
        }
    }

    companion object {
        const val DEFAULT_SEED: Long = 20260423L
    }
}
