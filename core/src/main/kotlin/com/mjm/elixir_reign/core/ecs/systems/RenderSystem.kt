package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent

/**
 * RenderSystem côté client (ECS-pur)
 * Affiche les TextureRegion à l'écran
 *
 * Components requis:
 * - PositionComponent (où afficher)
 * - TextureRegionComponent (quoi afficher - déjà chargé!)
 * - SpriteComponent (métadonnées: dimensions, scale)
 */
class RenderSystem(private val batch: SpriteBatch) : IteratingSystem(
    Family.all(
        PositionComponent::class.java,
        TextureRegionComponent::class.java,
        SpriteComponent::class.java
    ).get()
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
}
