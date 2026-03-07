package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.core.tools.RenderingUtils
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent

/**
 * SelectionRenderSystem : Affiche les contours de sélection (cercles en pointillés)
 *
 * Optimisé pour batches tous les contours de sélection en une seule passe ShapeRenderer
 * par frame, indépendante du SpriteBatch. Cela évite les multiples batch.end/begin
 * et améliore les performances.
 */
class SelectionRenderSystem(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera,
    private val selectionInputHandler: SelectionInputHandler
) : IteratingSystem(
    Family.all(
        SelectableComponent::class.java,
        PositionComponent::class.java,
        SpriteComponent::class.java
    ).get()
) {
    // Collecte des entités sélectionnées pour rendu batché
    private val selectedEntitiesThisFrame = mutableListOf<Entity>()

    override fun update(deltaTime: Float) {
        // Collecter les entités sélectionnées
        selectedEntitiesThisFrame.clear()
        for (entity in engine.getEntitiesFor(family)) {
            val selectable = entity.getComponent(SelectableComponent::class.java)
            if (selectable.isSelected) {
                selectedEntitiesThisFrame.add(entity)
            }
        }

        // Dessiner tous les contours en une seule passe ShapeRenderer
        if (selectedEntitiesThisFrame.isNotEmpty()) {
            renderSelectionContours()
        }
    }

    /**
     * Dessine tous les contours de sélection en une seule passe
     * Appelé une fois par frame depuis update(), pas depuis processEntity()
     */
    private fun renderSelectionContours() {
        // Pause le batch, dessine les contours, reprend le batch
        batch.end()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        // Dessiner tous les contours
        for (entity in selectedEntitiesThisFrame) {
            val pos = entity.getComponent(PositionComponent::class.java) ?: continue
            val sprite = entity.getComponent(SpriteComponent::class.java) ?: continue

            val (centerX, centerY) = BoundingBoxUtils.getSpriteCenter(pos, sprite)
            val radius = BoundingBoxUtils.getSelectionRadius(sprite)

            RenderingUtils.drawDashedCircle(
                shapeRenderer,
                centerX, centerY,
                radius,
                dashLength = 10f,
                gapLength = 10f
            )
        }

        shapeRenderer.end()
        batch.begin()
    }

    /**
     * processEntity() n'est pas utilisé car le rendu est batché dans update()
     */
    override fun processEntity(entity: Entity, deltaTime: Float) {
        // Ne rien faire ici - le rendu est géré dans renderSelectionContours()
    }
}



