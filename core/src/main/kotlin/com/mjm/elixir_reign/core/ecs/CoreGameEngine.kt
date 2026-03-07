package com.mjm.elixir_reign.core.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.shared.ecs.GameEngine
import com.mjm.elixir_reign.core.ecs.systems.AnimationSystem
import com.mjm.elixir_reign.core.ecs.systems.RenderSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthSystem
import com.mjm.elixir_reign.core.ecs.systems.SelectionRenderSystem
import com.mjm.elixir_reign.core.input.SelectionInputHandler
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager

class CoreGameEngine(
    private val batch: SpriteBatch,
    private val camera: OrthographicCamera,
    engineToUse: Engine? = null
) {
    private val gameEngine = GameEngine()
    val engine: Engine = engineToUse ?: gameEngine.engine
    val selectionInputHandler = SelectionInputHandler(engine)

    private val shapeRenderer = ShapeRenderer()

    init {
        engine.addSystem(AnimationSystem())
        engine.addSystem(HealthSystem())
        engine.addSystem(SelectionRenderSystem(batch, shapeRenderer, camera, selectionInputHandler))
        engine.addSystem(RenderSystem(batch))
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
