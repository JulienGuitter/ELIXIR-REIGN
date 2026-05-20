package com.mjm.elixir_reign.core.ecs

import com.badlogic.ashley.core.Engine
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.core.ecs.systems.AnimationSystem
import com.mjm.elixir_reign.core.ecs.systems.BarracksProductionSystem
import com.mjm.elixir_reign.core.ecs.systems.RenderSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthSystem
import com.mjm.elixir_reign.core.ecs.systems.HealthBarRenderSystem
import com.mjm.elixir_reign.core.ecs.systems.SelectionRenderSystem
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.core.tools.sprites.BuildingTextureManager
import com.mjm.elixir_reign.core.tools.RenderingUtils

class CoreGameEngine(
    private val batch: SpriteBatch,
    private val camera: OrthographicCamera,
    val engine: Engine
) {
    val selectionInputHandler = SelectionInputHandler(engine)

    private val shapeRenderer = ShapeRenderer()

    init {
        engine.addSystem(AnimationSystem())
        engine.addSystem(HealthSystem())
        engine.addSystem(BarracksProductionSystem(engine))
        // engine.addSystem(SelectionRenderSystem(batch, shapeRenderer, camera, selectionInputHandler))
        engine.addSystem(RenderSystem(batch))
        engine.addSystem(HealthBarRenderSystem(batch, shapeRenderer, camera))
    }

    fun update(deltaTime: Float) {
        engine.update(deltaTime)
    }

    fun dispose() {
        TextureManager.unloadAll()
        SpriteAnimationManager.dispose()
        BuildingTextureManager.dispose()
        RenderingUtils.clearCache()
        shapeRenderer.dispose()
    }
}
