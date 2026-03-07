package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils

/**
 * HealthBarRenderSystem côté client
 * Affiche une barre de vie au-dessus de chaque entité avec santé
 * La barre s'affiche uniquement quand HP < maxHP
 *
 * Suit le même pattern que SelectionRenderSystem :
 * interrompt le SpriteBatch, dessine via ShapeRenderer puis reprend le batch.
 */
class HealthBarRenderSystem(
    private val batch: SpriteBatch,
    private val shapeRenderer: ShapeRenderer,
    private val camera: OrthographicCamera
) : IteratingSystem(
    Family.all(
        HealthComponent::class.java,
        PositionComponent::class.java,
        HealthBarComponent::class.java,
        SpriteComponent::class.java
    ).get()
) {
    // Entités à rendre cette frame (collectées avant le rendu batché)
    private val entitiesToRender = mutableListOf<Entity>()

    override fun update(deltaTime: Float) {
        // Collecter les entités qui nécessitent un rendu de barre de vie
        entitiesToRender.clear()
        for (entity in engine.getEntitiesFor(family)) {
            val health = entity.getComponent(HealthComponent::class.java)
            if (health.currentHP < health.maxHP) {
                entitiesToRender.add(entity)
            }
        }

        if (entitiesToRender.isEmpty()) return

        // Pause le SpriteBatch, dessine toutes les barres, reprend le batch
        batch.end()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (entity in entitiesToRender) {
            val health = entity.getComponent(HealthComponent::class.java)
            val position = entity.getComponent(PositionComponent::class.java)
            val healthBar = entity.getComponent(HealthBarComponent::class.java)
            val sprite = entity.getComponent(SpriteComponent::class.java)
            renderHealthBar(position, health, healthBar, sprite)
        }

        shapeRenderer.end()
        batch.begin()
    }

    /**
     * processEntity() n'est pas utilisé car le rendu est batché dans update()
     */
    override fun processEntity(entity: Entity, deltaTime: Float) {
        // Rendu géré dans update()
    }

    private fun renderHealthBar(
        position: PositionComponent,
        health: HealthComponent,
        healthBar: HealthBarComponent,
        sprite: SpriteComponent
    ) {
        // Utiliser la bounding box du collider comme référence
        val box = BoundingBoxUtils.getBoundingBoxFromComponents(position, sprite)

        // La barre est centrée sur le collider, juste au-dessus
        val barWidth = box.width
        val barX = box.x
        val barY = box.y + box.height + healthBar.barHeight  // juste au-dessus du collider

        val healthPercent = (health.currentHP / health.maxHP).coerceIn(0f, 1f)
        val filledWidth = barWidth * healthPercent

        // Fond gris (barre vide)
        shapeRenderer.color = Color(0.3f, 0.3f, 0.3f, 1f)
        shapeRenderer.rect(barX, barY, barWidth, healthBar.barHeight)

        // Barre de vie colorée (gradient Vert -> Orange -> Rouge)
        shapeRenderer.color = getHealthColor(healthPercent)
        shapeRenderer.rect(barX, barY, filledWidth, healthBar.barHeight)

        // Bordure blanche
        shapeRenderer.color = Color(1f, 1f, 1f, 1f)
        shapeRenderer.rect(barX, barY, barWidth, 0.5f)                          // Bas
        shapeRenderer.rect(barX, barY + healthBar.barHeight - 0.5f, barWidth, 0.5f)  // Haut
        shapeRenderer.rect(barX, barY, 0.5f, healthBar.barHeight)               // Gauche
        shapeRenderer.rect(barX + barWidth - 0.5f, barY, 0.5f, healthBar.barHeight) // Droite
    }

    /**
     * Retourne une couleur basée sur le pourcentage de santé
     * Vert  (> 66%) -> Orange (33-66%) -> Rouge (< 33%)
     */
    private fun getHealthColor(healthPercent: Float): Color {
        return when {
            healthPercent > 0.66f -> Color(0f, 1f, 0f, 1f)
            healthPercent < 0.33f -> Color(1f, 0f, 0f, 1f)
            else -> Color(1f, 0.5f, 0f, 1f)
        }
    }
}




