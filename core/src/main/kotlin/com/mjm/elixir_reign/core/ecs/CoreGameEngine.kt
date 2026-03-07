package com.mjm.elixir_reign.core.ecs

import com.badlogic.ashley.core.Family
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.GameEngine
import com.mjm.elixir_reign.core.ecs.systems.AnimationSystem
import com.mjm.elixir_reign.core.ecs.systems.RenderSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthBarRenderSystem
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.HealthBarComponent

/**
 * CoreGameEngine étend GameEngine avec les systems spécifiques au client
 * (rendu graphique, animations visuelles, effets)
 */
class CoreGameEngine(private val batch: SpriteBatch, private val shapeRenderer: ShapeRenderer) : GameEngine() {
    private var animationSystem: AnimationSystem? = null
    private var renderSystem: RenderSystem? = null
    private var healthBarRenderSystem: HealthBarRenderSystem? = null

    init {
        // Ajouter les systems spécifiques au client
        animationSystem = AnimationSystem()
        renderSystem = RenderSystem(batch)
        healthBarRenderSystem = HealthBarRenderSystem(shapeRenderer)

        engine.addSystem(animationSystem)
        engine.addSystem(HealthSystem())  // Affichage visuel seulement
        engine.addSystem(renderSystem)
        // HealthBarRenderSystem est exécuté séparément dans renderHealthBars()
    }

    /**
     * Rend les barres de vie de toutes les entités
     * Doit être appelé entre shapeRenderer.begin() et shapeRenderer.end()
     */
    fun renderHealthBars() {
        val healthBarFamily = Family.all(
            HealthComponent::class.java,
            PositionComponent::class.java,
            HealthBarComponent::class.java
        ).get()

        val entities = engine.getEntitiesFor(healthBarFamily)
        for (entity in entities) {
            healthBarRenderSystem?.render(entity)
        }
    }

    fun dispose() {
        renderSystem?.dispose()
    }
}
