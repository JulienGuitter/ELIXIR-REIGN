package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.OwnerComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.session.GameMode
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils

/**
 * HealthBarRenderSystem côté client
 * Affiche une barre de vie au-dessus de chaque entité avec santé
 * La barre s'affiche quand HP < maxHP. Le marqueur joueur peut rester visible seul en multijoueur.
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
    private val entitiesToRender = mutableListOf<HealthBarRenderItem>()

    override fun update(deltaTime: Float) {
        // Collecter les entités qui nécessitent un rendu de barre de vie
        entitiesToRender.clear()
        for (entity in engine.getEntitiesFor(family)) {
            val health = entity.getComponent(HealthComponent::class.java)
            val renderPlayerMarker = shouldRenderPlayerMarker(entity)
            if (health.currentHP < health.maxHP || renderPlayerMarker) {
                entitiesToRender.add(HealthBarRenderItem(entity, renderPlayerMarker))
            }
        }

        if (entitiesToRender.isEmpty()) return

        // Pause le SpriteBatch, dessine toutes les barres, reprend le batch
        batch.end()

        shapeRenderer.projectionMatrix = camera.combined
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)

        for (item in entitiesToRender) {
            val entity = item.entity
            val health = entity.getComponent(HealthComponent::class.java)
            val position = entity.getComponent(PositionComponent::class.java)
            val healthBar = entity.getComponent(HealthBarComponent::class.java)
            val sprite = entity.getComponent(SpriteComponent::class.java)
            val owner = entity.getComponent(OwnerComponent::class.java)
            renderHealthBar(position, health, healthBar, sprite, owner, item.renderPlayerMarker)
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
        sprite: SpriteComponent,
        owner: OwnerComponent?,
        renderPlayerMarker: Boolean
    ) {
        // Utiliser la bounding box du collider comme référence
        val box = BoundingBoxUtils.getBoundingBoxFromComponents(position, sprite)

        // La barre est centrée sur le collider, juste au-dessus
        val barWidth = box.width
        val barX = box.x
        val barY = box.y + box.height + healthBar.barHeight  // juste au-dessus du collider

        val healthPercent = (health.currentHP / health.maxHP).coerceIn(0f, 1f)
        val filledWidth = barWidth * healthPercent
        val renderHealthBar = health.currentHP < health.maxHP

        if (renderHealthBar) {
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

        if (renderPlayerMarker && owner != null) {
            renderPlayerMarker(barX, barY, barWidth, healthBar.barHeight, owner.playerId, renderHealthBar)
        }
    }

    private fun renderPlayerMarker(
        barX: Float,
        barY: Float,
        barWidth: Float,
        barHeight: Float,
        playerId: Int,
        healthBarVisible: Boolean
    ) {
        val markerSize = (barWidth * MARKER_SIZE_RATIO).coerceIn(MIN_MARKER_SIZE, MAX_MARKER_SIZE)
        val markerX = if (healthBarVisible) {
            barX - markerSize - MARKER_GAP
        } else {
            barX + (barWidth - markerSize) / 2f
        }
        val markerY = barY + (barHeight - markerSize) / 2f

        shapeRenderer.color = Color(0.05f, 0.05f, 0.05f, 1f)
        shapeRenderer.rect(markerX, markerY, markerSize, markerSize)

        shapeRenderer.color = playerColor(playerId)
        shapeRenderer.rect(markerX + 1f, markerY + 1f, markerSize - 2f, markerSize - 2f)
    }

    private fun shouldRenderPlayerMarker(entity: Entity): Boolean {
        if (GameSession.mode != GameMode.MULTI) return false
        if (entity.getComponent(OwnerComponent::class.java) == null) return false

        return entity.getComponent(EntityTypeComponent::class.java) != null
    }

    private fun playerColor(playerId: Int): Color {
        val slot = GameSession.getPlayerColorSlot(playerId)
        val index = Math.floorMod(slot, PLAYER_COLORS.size)
        return PLAYER_COLORS[index]
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

    companion object {
        private const val MARKER_GAP = 4f
        private const val MARKER_SIZE_RATIO = 0.18f
        private const val MIN_MARKER_SIZE = 22f
        private const val MAX_MARKER_SIZE = 34f

        private val PLAYER_COLORS = arrayOf(
            Color(0.13f, 0.58f, 1f, 1f),
            Color(1f, 0.78f, 0.12f, 1f),
            Color(0.36f, 0.82f, 0.34f, 1f),
            Color(0.94f, 0.29f, 0.72f, 1f)
        )
    }

    private data class HealthBarRenderItem(
        val entity: Entity,
        val renderPlayerMarker: Boolean
    )
}
