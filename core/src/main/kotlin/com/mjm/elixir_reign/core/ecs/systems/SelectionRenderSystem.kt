package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.core.tools.RenderingUtils
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.input.SelectionInputHandler
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent

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
    private var elapsed: Float = 0f
    private val pulseSpeed: Float = 3f

    override fun update(deltaTime: Float) {
        elapsed += deltaTime
        super.update(deltaTime)
    }

    override fun processEntity(entity: Entity, deltaTime: Float) {
        val selectable = entity.getComponent(SelectableComponent::class.java)
        if (!selectable.isSelected) return

        // Même bounding box que SelectionInputHandler → toujours cohérent
        val box = selectionInputHandler.getEntityBoundingBox(entity) ?: return

        val centerX = box.x + box.width / 2f
        val centerY = box.y + box.height / 3f
        val radius = box.width / 2f

        batch.end()
        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        RenderingUtils.drawArc(
            shapeRenderer,
            centerX, centerY,
            radius,
            startAngle = 0f,
            arcDegrees = 360f,
            segments = 64,
            dashLength = 10f,
            gapLength = 10f
        )

        shapeRenderer.end()

        batch.begin()
    }
}


