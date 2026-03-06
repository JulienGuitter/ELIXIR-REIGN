package com.mjm.elixir_reign.core.ecs

import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.shared.ecs.GameEngine
import com.mjm.elixir_reign.core.ecs.systems.AnimationSystem
import com.mjm.elixir_reign.core.ecs.systems.RenderSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthSystem
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager

/**
 * CoreGameEngine étend GameEngine avec les systems spécifiques au client
 * (rendu graphique, animations visuelles, effets)
 */
class CoreGameEngine(private val batch: SpriteBatch) : GameEngine() {
    private var animationSystem: AnimationSystem? = null
    private var renderSystem: RenderSystem? = null

    init {
        // Ajouter les systems spécifiques au client
        animationSystem = AnimationSystem()
        renderSystem = RenderSystem(batch)

        engine.addSystem(animationSystem)
        engine.addSystem(HealthSystem())  // Affichage visuel seulement
        engine.addSystem(renderSystem)
    }

    fun dispose() {
        TextureManager.unloadAll()
        SpriteAnimationManager.dispose()
    }
}

