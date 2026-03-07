package com.mjm.elixir_reign.core.tools

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

/**
 * RenderingUtils : Utilitaire regroupant les fonctions de rendu réutilisables
 */
object RenderingUtils {

    /**
     * Cache des angles pré-calculés par rayon
     * Clé   : Long encodant (radius, dashLength, gapLength) via leurs bits IEEE 754
     * Valeur: Pair(dashAngle, gapAngle) en radians
     *
     * Utilise un Long au lieu d'un Triple pour éviter une allocation heap à chaque lookup
     */
    private val angleCache = HashMap<Long, Pair<Float, Float>>()

    /** Encode trois Float en un Long sans allocation (16 bits chacun via half-precision) */
    private fun cacheKey(radius: Float, dashLength: Float, gapLength: Float): Long {
        val r = radius.toBits().toLong()          and 0xFFFFF  // 20 bits
        val d = dashLength.toBits().toLong()      and 0xFFFFF  // 20 bits
        val g = gapLength.toBits().toLong()       and 0xFFFFF  // 20 bits
        return (r shl 40) or (d shl 20) or g
    }

    /**
     * Dessine un cercle en pointillé blanc
     *
     * Principe : sur un cercle de rayon r, un arc de longueur L correspond
     * à un angle de L/r radians. On avance donc directement d'angle en angle.
     *
     * @param renderer   ShapeRenderer à utiliser (doit être en mode Line)
     * @param centerX    Position X du centre du cercle
     * @param centerY    Position Y du centre du cercle
     * @param radius     Rayon du cercle
     * @param dashLength Longueur des tirets en unités monde
     * @param gapLength  Longueur des espaces entre tirets en unités monde
     */
    fun drawDashedCircle(
        renderer: ShapeRenderer,
        centerX: Float,
        centerY: Float,
        radius: Float,
        dashLength: Float = 10f,
        gapLength: Float = 10f
    ) {
        if (radius <= 0f) return
        renderer.color.set(1f, 1f, 1f, 1f)

        // Récupère depuis le cache ou calcule une seule fois (clé Long = 0 allocation)
        val key = cacheKey(radius, dashLength, gapLength)
        val (dashAngle, gapAngle) = angleCache.getOrPut(key) {
            Pair(dashLength / radius, gapLength / radius)
        }

        val twoPi = MathUtils.PI2
        var angle = 0f
        while (angle < twoPi) {
            // Début du tiret
            val dashStart = angle
            val dashEnd   = (angle + dashAngle).coerceAtMost(twoPi)

            renderer.line(
                centerX + radius * MathUtils.cos(dashStart),
                centerY + radius * MathUtils.sin(dashStart),
                centerX + radius * MathUtils.cos(dashEnd),
                centerY + radius * MathUtils.sin(dashEnd)
            )

            angle += dashAngle + gapAngle
        }
    }

    /** Vide le cache (à appeler si les entités changent de taille ou au dispose) */
    fun clearCache() {
        angleCache.clear()
    }
}
