package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.MathUtils
import com.mjm.elixir_reign.core.tools.RenderingUtils
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent

/**
 * SelectionRenderSystem : Affiche le cercle de sélection autour des entités sélectionnées
 * Utilise ShapeRenderer pour dessiner un arc avec un effet de pulsation
 *
 * ⚠️ ORDRE D'AFFICHAGE : Ce système s'exécute AVANT RenderSystem (sprites)
 * Cela fait que le cercle s'affiche SOUS les sprites (l'unité passe par-dessus)
 * Crée un effet d'anneau visuel au sol autour de l'entité sélectionnée
 *
 * La caméra est utilisée pour que les cercles se déplacent avec la vue
 */
class SelectionRenderSystem(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera
) : IteratingSystem(
    Family.all(
        SelectableComponent::class.java,
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

        // Calculer le centre et le rayon du cercle
        val centerX = position.x + (sprite.width / 1.2f * sprite.scaleX) / 2f
        val centerY = position.y + (sprite.height * sprite.scaleY) / 2.2f

        // Rayon simple basé sur la largeur du sprite
        val radius = (sprite.width * sprite.scaleX) / 4.5f

        // Effet de pulsation optionnel
        val pulseAlpha = 0.7f + 0.3f * MathUtils.sin(elapsed * pulseSpeed)

        // Arrêter le batch pour dessiner avec ShapeRenderer
        batch.end()

        // Appliquer la projection de la caméra au ShapeRenderer
        shapeRenderer.projectionMatrix = camera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        // Dessiner un arc de cercle (270 degrés, commençant par la gauche et ouverture vers le haut)
        // Avec couleur blanche et pointillés
        RenderingUtils.drawArc(
            shapeRenderer,
            centerX, centerY,
            radius,
            startAngle = 0f,
            arcDegrees = 360f,
            segments = 64,
            a = pulseAlpha,
            dashLength = 10f,
            gapLength = 10f
        )

        shapeRenderer.end()

        batch.begin()
    }
}


