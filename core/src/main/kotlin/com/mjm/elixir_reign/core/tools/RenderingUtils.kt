package com.mjm.elixir_reign.core.tools

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils

/**
 * RenderingUtils : Utilitaire regroupant les fonctions de rendu réutilisables
 * Contient les fonctions pour dessiner des formes personnalisées avec ShapeRenderer
 */
object RenderingUtils {

    /**
     * Dessine un arc de cercle (portion de cercle)
     *
     * @param renderer ShapeRenderer à utiliser
     * @param centerX Position X du centre du cercle
     * @param centerY Position Y du centre du cercle
     * @param radius Rayon du cercle
     * @param startAngle Angle de départ en degrés (0° = droite, 90° = haut, 180° = gauche, 270° = bas)
     * @param arcDegrees Nombre de degrés à couvrir pour l'arc
     * @param segments Nombre de segments pour dessiner l'arc (plus = plus lisse)
     * @param r Composante rouge (0-1)
     * @param g Composante verte (0-1)
     * @param b Composante bleue (0-1)
     * @param a Composante alpha (0-1)
     * @param dashLength Longueur des tirets en pixels (0 = trait continu)
     * @param gapLength Longueur des espaces entre tirets en pixels
     */
    fun drawArc(
        renderer: ShapeRenderer,
        centerX: Float,
        centerY: Float,
        radius: Float,
        startAngle: Float,
        arcDegrees: Float,
        segments: Int,
        r: Float = 1f,
        g: Float = 1f,
        b: Float = 1f,
        a: Float = 1f,
        dashLength: Float = 0f,
        gapLength: Float = 5f
    ) {
        val angleStep = arcDegrees / segments
        var angle = startAngle
        var distanceTraveled = 0f
        var isDashing = true

        renderer.color.set(r, g, b, a)

        for (i in 0 until segments) {
            val x1 = centerX + radius * MathUtils.cos(MathUtils.degreesToRadians * angle)
            val y1 = centerY + radius * MathUtils.sin(MathUtils.degreesToRadians * angle)

            angle += angleStep
            val x2 = centerX + radius * MathUtils.cos(MathUtils.degreesToRadians * angle)
            val y2 = centerY + radius * MathUtils.sin(MathUtils.degreesToRadians * angle)

            if (dashLength <= 0f) {
                // Trait continu
                renderer.line(x1, y1, x2, y2)
            } else {
                // Trait pointillé
                val dx = x2 - x1
                val dy = y2 - y1
                val segmentLength = kotlin.math.sqrt(dx * dx + dy * dy)
                val steps = (segmentLength / 2f).toInt().coerceAtLeast(1)
                val stepX = dx / steps
                val stepY = dy / steps

                var currentX = x1
                var currentY = y1

                for (j in 0 until steps) {
                    val nextX = currentX + stepX
                    val nextY = currentY + stepY
                    val distance = kotlin.math.sqrt((nextX - currentX) * (nextX - currentX) + (nextY - currentY) * (nextY - currentY))

                    if (isDashing) {
                        distanceTraveled += distance
                        if (distanceTraveled >= dashLength) {
                            isDashing = false
                            distanceTraveled = 0f
                        } else {
                            renderer.line(currentX, currentY, nextX, nextY)
                        }
                    } else {
                        distanceTraveled += distance
                        if (distanceTraveled >= gapLength) {
                            isDashing = true
                            distanceTraveled = 0f
                        }
                    }

                    currentX = nextX
                    currentY = nextY
                }
            }
        }
    }
}

