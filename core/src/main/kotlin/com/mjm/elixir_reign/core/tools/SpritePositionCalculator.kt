package com.mjm.elixir_reign.core.tools

import com.badlogic.gdx.math.Rectangle
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent

/**
 * SpritePositionCalculator : Centralise TOUS les calculs de positionnement de sprite
 *
 * SOURCE UNIQUE DE VÉRITÉ pour les transformations :
 * - Position monde → Position de dessin (draw position)
 * - Calcul des bounding boxes avec offsets
 * - Positionnement unifié pour RenderSystem et BuildPlacementHandler
 *
 * Cette classe est agnostique au context : elle fournit des calculs purs
 * que chaque client (RenderSystem, BuildPlacementHandler, etc.) peut utiliser
 * selon ses besoins.
 */
object SpritePositionCalculator {

    /**
     * Calcule la position de dessin d'un sprite basée sur :
     * - La position monde (pos.x, pos.y)
     * - Les dimensions du sprite (width, height)
     * - Les échelles (scaleX, scaleY)
     * - Les offsets de centrage (offsetX, offsetY)
     *
     * La formule est:
     *   drawX = pos.x + (width * scaleX * offsetX)
     *   drawY = pos.y + (height * scaleY * offsetY)
     *
     * @return Pair(drawX, drawY)
     */
    fun calculateDrawPosition(
        worldX: Float,
        worldY: Float,
        spriteWidth: Int,
        spriteHeight: Int,
        scaleX: Float,
        scaleY: Float,
        offsetX: Float,
        offsetY: Float
    ): Pair<Float, Float> {
        val adjustedX = worldX + spriteWidth * scaleX * offsetX
        val adjustedY = worldY + spriteHeight * scaleY * offsetY
        return Pair(adjustedX, adjustedY)
    }

    /**
     * Calcule la position de dessin à partir de components (cas ECS)
     */
    fun calculateDrawPosition(
        pos: PositionComponent,
        sprite: SpriteComponent
    ): Pair<Float, Float> {
        return calculateDrawPosition(
            worldX = pos.x,
            worldY = pos.y,
            spriteWidth = sprite.width,
            spriteHeight = sprite.height,
            scaleX = sprite.scaleX,
            scaleY = sprite.scaleY,
            offsetX = sprite.offsetX,
            offsetY = sprite.offsetY
        )
    }

    /**
     * Calcule une bounding box complète incluant les colliders optionnels
     *
     * La bounding box est calculée en fonction de :
     * - La position de dessin du sprite
     * - Les dimensions du sprite
     * - Un collider optionnel (si défini, la bounding box ne couvre que la zone de collision)
     */
    fun calculateBoundingBox(
        pos: PositionComponent,
        sprite: SpriteComponent
    ): Rectangle {
        val fullW = sprite.width * sprite.scaleX
        val fullH = sprite.height * sprite.scaleY
        val (drawX, drawY) = calculateDrawPosition(pos, sprite)

        val collider = sprite.collider
        return if (collider != null) {
            val colX = drawX + collider.bottomLeftX * fullW
            val colY = drawY + collider.bottomLeftY * fullH
            val width = (collider.topRightX - collider.bottomLeftX) * fullW
            val height = (collider.topRightY - collider.bottomLeftY) * fullH
            Rectangle(colX, colY, width, height)
        } else {
            Rectangle(drawX, drawY, fullW, fullH)
        }
    }

    /**
     * Calcule le centre du sprite (pour les cercles de sélection)
     */
    fun calculateSpriteCenter(
        pos: PositionComponent,
        sprite: SpriteComponent
    ): Pair<Float, Float> {
        return Pair(pos.x, pos.y + sprite.height * sprite.scaleY * 0.1f)
    }

    /**
     * Calcule le rayon du cercle de sélection basé sur les dimensions du sprite
     */
    fun calculateSelectionRadius(sprite: SpriteComponent): Float {
        return (sprite.width * sprite.scaleX) / 5f
    }
}
