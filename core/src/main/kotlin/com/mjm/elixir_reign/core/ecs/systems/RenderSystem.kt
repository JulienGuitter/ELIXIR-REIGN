package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.SortedIteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.ecs.components.DepthComponent
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
            // Dessiner le sprite
            batch.draw(
                textureRegion.textureRegion,
                position.x,
                position.y,
                sprite.width * sprite.scaleX,
                sprite.height * sprite.scaleY
            )
        }
    }

    /**
     * Comparateur pour trier les entités par profondeur
     * Permet le Y-sorting automatique (plus Y = affiche en dernier = au-dessus)
     */
    private class DepthComparator : Comparator<Entity> {
        override fun compare(e1: Entity, e2: Entity): Int {
            val pos1 = e1.getComponent(PositionComponent::class.java)
            val pos2 = e2.getComponent(PositionComponent::class.java)

            // Récupérer la profondeur de chaque entité
            val depth1 = e1.getComponent(DepthComponent::class.java)?.getDepth(pos1.y) ?: pos1.y
            val depth2 = e2.getComponent(DepthComponent::class.java)?.getDepth(pos2.y) ?: pos2.y

            // Comparer les profondeurs (profondeur basse = affiche en premier)
            return depth1.compareTo(depth2)
        }
    }
}
