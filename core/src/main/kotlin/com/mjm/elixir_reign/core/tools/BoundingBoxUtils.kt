package com.mjm.elixir_reign.core.tools

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Rectangle
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent

/**
 * BoundingBoxUtils : Centralize tous les calculs de bounding box et positionnement
 *
 * ⚠️ SOURCE DE VÉRITÉ UNIQUE pour les calculs de positionnement
 * Tous les calculs partagent la même logique de base : calculateAdjustedPosition()
 */
object BoundingBoxUtils {

    /**
     * Récupère la bounding box d'une entité
     */
    fun getBoundingBox(entity: Entity): Rectangle? {
        val pos = entity.getComponent(PositionComponent::class.java) ?: return null
        val sprite = entity.getComponent(SpriteComponent::class.java) ?: return null
        return getBoundingBoxFromComponents(pos, sprite)
    }

    /**
     * Calcule la bounding box à partir des components.
     * Si un collider est défini sur le SpriteComponent, il est utilisé à la place
     * de la cellule entière. Les coordonnées sont en "normalized_bottom_left" :
     *   (0,0) = bas-gauche de la cellule, (1,1) = haut-droite de la cellule
     */
    fun getBoundingBoxFromComponents(pos: PositionComponent, sprite: SpriteComponent): Rectangle {
        val fullW = sprite.width * sprite.scaleX
        val fullH = sprite.height * sprite.scaleY
        val (drawX, drawY) = calculateAdjustedPosition(pos, sprite)

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
     * Calcule la position de dessin du sprite
     */
    fun getDrawPosition(pos: PositionComponent, sprite: SpriteComponent): Pair<Float, Float> {
        return calculateAdjustedPosition(pos, sprite)
    }

    /**
     * Calcule le centre du sprite pour les cercles de sélection
     */
    fun getSpriteCenter(pos: PositionComponent, sprite: SpriteComponent): Pair<Float, Float> {
        return Pair(pos.x, pos.y + sprite.height * sprite.scaleY * 0.1f)
    }

    /**
     * Calcule le rayon du cercle de sélection
     */
    fun getSelectionRadius(sprite: SpriteComponent): Float {
        return (sprite.width * sprite.scaleX) / 5f
    }

    /**
     * Vérifie si un point est à l'intérieur de la bounding box
     */
    fun pointInEntity(entity: Entity, x: Float, y: Float): Boolean {
        val box = getBoundingBox(entity) ?: return false
        return box.contains(x, y)
    }

    /**
     * Vérifie si une rectangle chevauche la bounding box
     */
    fun entityTouchesRectangle(entity: Entity, rect: Rectangle): Boolean {
        val box = getBoundingBox(entity) ?: return false
        return rect.overlaps(box)
    }

    // ============ CALCULS INTERNES (Source unique de vérité) ============

    /**
     * Logique UNIQUE de calcul de positionnement ajustée
     * Tous les autres calculs l'utilisent pour éviter la duplication
     *
     * Calcule la position (X, Y) en tenant compte de:
     * - La largeur du sprite (fullW) pour le centrage horizontal
     * - Les offsets d'animation du SpriteComponent (offsetX, offsetY)
     */
    private fun calculateAdjustedPosition(pos: PositionComponent, sprite: SpriteComponent): Pair<Float, Float> {
        val adjustedX = pos.x + sprite.width * sprite.scaleX * sprite.offsetX
        val adjustedY = pos.y + sprite.height * sprite.scaleY * sprite.offsetY

        return Pair(adjustedX, adjustedY)
    }
}
