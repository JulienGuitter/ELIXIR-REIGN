package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
import com.mjm.elixir_reign.core.ecs.components.LayerComponent
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils
import java.util.Comparator

/**
 * RenderSystem côté client (ECS-pur)
 * Affiche les TextureRegion à l'écran, triées par profondeur (ordre d'affichage)
 *
 * Components requis:
 * - PositionComponent (où afficher)
 * - TextureRegionComponent (quoi afficher - déjà chargé!)
 * - SpriteComponent (métadonnées: dimensions, scale)
 * - DepthComponent (optionnel, pour contrôler l'ordre d'affichage)
 *
 * Tri automatique:
 * - Si DepthComponent présent: utilise zOrder ou Y-sorting
 * - Sinon: Y-sorting automatique basé sur PositionComponent.y
 */
class RenderSystem(private val batch: SpriteBatch) : SortedIteratingSystem(
    Family.all(
        PositionComponent::class.java,
        TextureRegionComponent::class.java,
        SpriteComponent::class.java
    ).get(),
    DepthComparator()
) {

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val position = entity.getComponent(PositionComponent::class.java)
        val textureRegion = entity.getComponent(TextureRegionComponent::class.java)
        val sprite = entity.getComponent(SpriteComponent::class.java)

        // Vérifier que la TextureRegion est valide avant de dessiner
        if (textureRegion.textureRegion.texture != null) {
            // Récupérer la position de dessin depuis BoundingBoxUtils (source unique de vérité)
            val (drawX, drawY) = BoundingBoxUtils.getDrawPosition(position, sprite)
            val spriteWidth = sprite.width * sprite.scaleX
            val spriteHeight = sprite.height * sprite.scaleY


            // Dessiner le sprite
            batch.draw(
                textureRegion.textureRegion,
                drawX,
                drawY,
                spriteWidth,
                spriteHeight
            )
        }
    }

    /**
     * Comparateur pour trier les entités par layer, puis par profondeur
     * Ordre de tri :
     * 1. Layer (plus élevé = au-dessus)
     * 2. Profondeur/Y-position (Y-sorting dans chaque layer)
     */
    private class DepthComparator : Comparator<Entity> {
        override fun compare(e1: Entity, e2: Entity): Int {
            // Comparaison des layers d'abord (plus élevé = au-dessus)
            val layer1 = e1.getComponent(LayerComponent::class.java)?.layer ?: 0
            val layer2 = e2.getComponent(LayerComponent::class.java)?.layer ?: 0
            val layerComparison = layer2.compareTo(layer1)  // Inversé : plus grand = premier
            if (layerComparison != 0) return layerComparison

            // Même layer → comparer par profondeur/Y-position
            val pos1 = e1.getComponent(PositionComponent::class.java)
            val pos2 = e2.getComponent(PositionComponent::class.java)

            val depth1 = e1.getComponent(DepthComponent::class.java)?.getDepth(pos1.y) ?: pos1.y
            val depth2 = e2.getComponent(DepthComponent::class.java)?.getDepth(pos2.y) ?: pos2.y

            return depth1.compareTo(depth2)
        }
    }
}
