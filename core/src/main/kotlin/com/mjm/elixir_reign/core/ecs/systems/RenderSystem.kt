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
import com.mjm.elixir_reign.core.ecs.components.SpriteAnimatorComponent
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils
import com.mjm.elixir_reign.shared.ecs.components.GridPlacementComponent
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

    override fun update(deltaTime: Float) {
        forceSort()
        super.update(deltaTime)
    }

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

            val animatorComp = entity.getComponent(SpriteAnimatorComponent::class.java)
            val isFlipped = animatorComp?.spriteAnimator?.shouldFlip() ?: false
            val finalDrawX = if (isFlipped) {
                // Use scaled width so flip offset stays consistent with BoundingBoxUtils and other normalized offsets
                drawX - sprite.width * animatorComp.spriteAnimator.spriteSheet.footX
            } else {
                drawX
            }

            // Dessiner le sprite
            batch.draw(
                textureRegion.textureRegion,
                finalDrawX,
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
            val layerComparison = layer1.compareTo(layer2)
            if (layerComparison != 0) return layerComparison

            // Même layer -> comparer par profondeur isometrique.
            val pos1 = e1.getComponent(PositionComponent::class.java)
            val pos2 = e2.getComponent(PositionComponent::class.java)
            val grid1 = e1.getComponent(GridPlacementComponent::class.java)
            val grid2 = e2.getComponent(GridPlacementComponent::class.java)

            val depth1 = e1.getComponent(DepthComponent::class.java)?.zOrder ?: grid1?.frontDepth?.toFloat() ?: -pos1.y
            val depth2 = e2.getComponent(DepthComponent::class.java)?.zOrder ?: grid2?.frontDepth?.toFloat() ?: -pos2.y
            val depthComparison = depth1.compareTo(depth2)
            if (depthComparison != 0) return depthComparison

            if (grid1 != null && grid2 != null) {
                val rowComparison = grid1.row.compareTo(grid2.row)
                if (rowComparison != 0) return rowComparison

                val colComparison = grid1.col.compareTo(grid2.col)
                if (colComparison != 0) return colComparison
            }

            return pos1.x.compareTo(pos2.x)
        }
    }
}
