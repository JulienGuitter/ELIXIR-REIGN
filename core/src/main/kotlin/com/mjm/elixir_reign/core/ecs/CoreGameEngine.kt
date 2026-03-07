package com.mjm.elixir_reign.core.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.GameEngine
import com.mjm.elixir_reign.core.ecs.systems.AnimationSystem
import com.mjm.elixir_reign.core.ecs.systems.RenderSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthSystem
import com.mjm.elixir_reign.core.ecs.systems.SelectionSystem
import com.mjm.elixir_reign.core.ecs.systems.SelectionRenderSystem
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager

/**
 * CoreGameEngine : Ajoute le rendu et l'animation à un GameEngine existant
 *
 * Peut être utilisé de deux façons :
 * 1. Créer sa propre instance GameEngine (ancien comportement)
 * 2. Recevoir un Engine existant (nouveau comportement, pour partage avec GameWorld)
 */
class CoreGameEngine(
    private val batch: SpriteBatch,
    private val camera: OrthographicCamera,
    engineToUse: Engine? = null
) {
    private val gameEngine = GameEngine()
    val engine: Engine = engineToUse ?: gameEngine.engine

    private var animationSystem: AnimationSystem? = null
    private var renderSystem: RenderSystem? = null
    private var selectionSystem: SelectionSystem? = null
    private var selectionRenderSystem: SelectionRenderSystem? = null
    private val shapeRenderer = ShapeRenderer()

    init {
        // Ajouter les systems spécifiques au client
        animationSystem = AnimationSystem()
        renderSystem = RenderSystem(batch)
        selectionSystem = SelectionSystem()
        selectionRenderSystem = SelectionRenderSystem(batch, shapeRenderer, camera)

        engine.addSystem(animationSystem)
        engine.addSystem(HealthSystem())  // Affichage visuel seulement
        engine.addSystem(selectionRenderSystem)  // Rendu du cercle de sélection (AVANT les sprites)
        engine.addSystem(renderSystem)           // Affichage des sprites (par-dessus le cercle)
        engine.addSystem(selectionSystem)  // Logique de sélection
    }

    fun update(deltaTime: Float) {
        engine.update(deltaTime)
    }

    fun dispose() {
        TextureManager.unloadAll()
        SpriteAnimationManager.dispose()
        shapeRenderer.dispose()
    }
}

