package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.core.ecs.components.SelectionHighlightComponent
import kotlin.math.sqrt

/**
 * SelectionRenderSystem : Affiche le cercle de sélection autour des entités sélectionnées
 * Utilise ShapeRenderer pour dessiner un cercle avec un effet de pulsation optionnel
 * La caméra est utilisée pour que les cercles se déplacent avec la vue
 */
class SelectionRenderSystem(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera
) : IteratingSystem(
    Family.all(
        SelectableComponent::class.java,
        SelectionHighlightComponent::class.java,
        PositionComponent::class.java,
        SpriteComponent::class.java
    ).get()
) {
    private var elapsed: Float = 0f
    private val pulseSpeed: Float = 3f // Vitesse de pulsation

    override fun update(deltaTime: Float) {
        elapsed += deltaTime
        super.update(deltaTime)
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val selectable = entity.getComponent(SelectableComponent::class.java)

        if (!selectable.isSelected) return

        val position = entity.getComponent(PositionComponent::class.java)
        val sprite = entity.getComponent(SpriteComponent::class.java)
        val highlight = entity.getComponent(SelectionHighlightComponent::class.java)

        // Calculer le centre et le rayon du cercle
        val centerX = position.x + (sprite.width * sprite.scaleX) / 2f
        val centerY = position.y + (sprite.height * sprite.scaleY) / 2f

        // Rayon basé sur la taille du sprite
        val baseRadius = sqrt(
            (sprite.width * sprite.scaleX / 2f) * (sprite.width * sprite.scaleX / 2f) +
            (sprite.height * sprite.scaleY / 2f) * (sprite.height * sprite.scaleY / 2f)
        ) + highlight.borderWidth * 2

        // Effet de pulsation optionnel
        val pulseAlpha = 0.7f + 0.3f * MathUtils.sin(elapsed * pulseSpeed)

        // Configurer la couleur avec l'effet de pulsation
        val color = highlight.borderColor
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f

        // Arrêter le batch pour dessiner avec ShapeRenderer
        batch.end()

        // Appliquer la projection de la caméra au ShapeRenderer
        shapeRenderer.projectionMatrix = camera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        shapeRenderer.color.set(r, g, b, pulseAlpha)
        shapeRenderer.circle(centerX, centerY, baseRadius, 64)
        shapeRenderer.end()

        batch.begin()
    }
}


