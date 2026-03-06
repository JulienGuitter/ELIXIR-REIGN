package com.mjm.elixir_reign.shared.logic

/**
 * Utilitaire pré-calculé pour mapper chaque direction à un vecteur normalisé
 * Évite les calculs de sqrt() et normalisation à chaque frame
 * Optimisé pour les 8 directions isométriques
 */
object DirectionVectors {
    private val SQRT_2_OVER_2 = 0.7071067811865476f  // cos(45°) = sin(45°)

    private val vectors = mapOf(
        DirectionType.UP to Pair(0f, 1f),
        DirectionType.UP_RIGHT to Pair(SQRT_2_OVER_2, SQRT_2_OVER_2),
        DirectionType.RIGHT to Pair(1f, 0f),
        DirectionType.DOWN_RIGHT to Pair(SQRT_2_OVER_2, -SQRT_2_OVER_2),
        DirectionType.DOWN to Pair(0f, -1f),
        DirectionType.DOWN_LEFT to Pair(-SQRT_2_OVER_2, -SQRT_2_OVER_2),
        DirectionType.LEFT to Pair(-1f, 0f),
        DirectionType.UP_LEFT to Pair(-SQRT_2_OVER_2, SQRT_2_OVER_2)
    )

    /**
     * Récupère le vecteur normalisé pour une direction
     * @param directionType la direction (8 directions possibles)
     * @return Pair(vx, vy) normalisé
     */
    fun getVector(directionType: DirectionType): Pair<Float, Float> {
        return vectors[directionType] ?: Pair(0f, 0f)
    }
}

