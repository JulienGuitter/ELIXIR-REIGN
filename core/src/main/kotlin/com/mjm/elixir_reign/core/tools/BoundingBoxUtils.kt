package com.mjm.elixir_reign.core.tools

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.math.Rectangle
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent

/**
 * BoundingBoxUtils : Wrapper convenable pour accéder aux calculs de positionnement via SpritePositionCalculator
 *
 * ⚠️ SOURCE DE VÉRITÉ UNIQUE : SpritePositionCalculator (voir ce fichier pour les calculs purs)
 * BoundingBoxUtils fournit une API pratique pour les entités ECS
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
        return SpritePositionCalculator.calculateBoundingBox(pos, sprite)
    }

    /**
     * Calcule la position de dessin du sprite
     */
    fun getDrawPosition(pos: PositionComponent, sprite: SpriteComponent): Pair<Float, Float> {
        return SpritePositionCalculator.calculateDrawPosition(pos, sprite)
    }

    /**
     * Calcule le centre du sprite pour les cercles de sélection
     */
    fun getSpriteCenter(pos: PositionComponent, sprite: SpriteComponent): Pair<Float, Float> {
        return SpritePositionCalculator.calculateSpriteCenter(pos, sprite)
    }

    /**
     * Calcule le rayon du cercle de sélection
     */
    fun getSelectionRadius(sprite: SpriteComponent): Float {
        return SpritePositionCalculator.calculateSelectionRadius(sprite)
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

    // ============ Vérifications géométriques ============
}
