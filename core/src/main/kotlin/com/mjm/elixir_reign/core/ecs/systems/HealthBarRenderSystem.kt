package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.Family
import com.badlogic.ashley.systems.IteratingSystem
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent

/**
 * HealthBarRenderSystem côté client
 * Affiche une barre de vie au-dessus de chaque entité avec santé
 * La barre se fade out après un temps sans dégâts
 */
class HealthBarRenderSystem(private val shapeRenderer: ShapeRenderer) : IteratingSystem(
    Family.all(
        HealthComponent::class.java,
        PositionComponent::class.java,
        HealthBarComponent::class.java
    ).get()
) {
    override fun processEntity(entity: Entity, deltaTime: Float) {
        val health = entity.getComponent(HealthComponent::class.java)
        val position = entity.getComponent(PositionComponent::class.java)
        val healthBar = entity.getComponent(HealthBarComponent::class.java)

        if (health.currentHP < health.maxHP) {
            renderHealthBar(position, health, healthBar)
        } // Else nothing to render if at full health
    }

    /**
     * Rend une barre de vie
     * Doit être appelé entre shapeRenderer.begin() et shapeRenderer.end()
     */
    fun render(entity: Entity) {
        processEntity(entity, 0f)
    }

    private fun renderHealthBar(
        position: PositionComponent,
        health: HealthComponent,
        healthBar: HealthBarComponent
    ) {
        val barX = position.x + healthBar.offsetX - healthBar.barWidth / 2f
        val barY = position.y + healthBar.offsetY

        val healthPercent = (health.currentHP / health.maxHP).coerceIn(0f, 1f)
        val filledWidth = healthBar.barWidth * healthPercent

        // Barre de vie colorée (gradient Vert -> Orange -> Rouge)
        val barColor = getHealthColor(healthPercent)
        shapeRenderer.color = Color(barColor.r, barColor.g, barColor.b, 1f)
        shapeRenderer.rect(barX, barY, filledWidth, healthBar.barHeight)

        // Bordure blanche (4 côtés)
        shapeRenderer.color = Color(1f, 1f, 1f, 1f)
        // Bas
        shapeRenderer.rect(barX, barY, healthBar.barWidth, 0.5f)
        // Droite
        shapeRenderer.rect(barX + healthBar.barWidth - 0.5f, barY, 0.5f, healthBar.barHeight)
    }

    /**
     * Retourne une couleur basée sur le pourcentage de santé
     * Vert (>75%) -> Orange (25-75%) -> Rouge (<25%)
     */
    private fun getHealthColor(healthPercent: Float): Color {
        return when {
            healthPercent > 0.75f -> {
                Color(0f, 1f, 0f, 1f)
            }
            healthPercent > 0.25f -> {
                val ratio = (healthPercent - 0.25f) / 0.5f
                val red = ratio
                val green = 1f - (ratio * 0.5f)
                Color(red, green, 0f, 1f)
            }
            else -> {
                Color(1f, 0f, 0f, 1f)
            }
        }
    }
}




