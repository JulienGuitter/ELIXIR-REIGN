package com.mjm.elixir_reign.core.screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.ecs.CoreGameEngine
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.ecs.factories.TerrainEntityFactory
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Écran de jeu principal.
 * La navigation "retour" et les capacités d'input sont déléguées à [Main.platform]
 * afin de rester indépendantes de la plateforme.
 */

const val CAMERA_INITIAL_ZOOM = 0.5f
const val MAP_SIZE = 10 // 4x4 tiles

class GameScreen(private val game: Main) : ScreenAdapter() {
    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var ecsEngine: CoreGameEngine
    private lateinit var inputController: GameScreenInputController

    override fun show() {
        camera = OrthographicCamera()
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 0f)
        camera.zoom = CAMERA_INITIAL_ZOOM
        camera.update()

        batch = SpriteBatch()

        // Initialiser l'engine ECS avec le batch
        ecsEngine = CoreGameEngine(batch)

        val terrain = TerrainEntityFactory.createIsoTerrain(
            clipName = "ground_5",
            gridColumns = MAP_SIZE,
            gridRows = MAP_SIZE,
            engine = ecsEngine.engine
        )
        centerEntity(terrain)
        inputController = GameScreenInputController(game, camera, buildCameraDragBounds(terrain))

        inputController.activate()

        val nbr = 10;
        for (i in nbr downTo 1) {
            for (j in nbr downTo 1) {
                SpriteEntityFactory.createUnit(
                    unitType = UnitType.BARBARIAN,
                    x = i * 20f - 100f,
                    y = j * 20f - 100f,
                    engine = ecsEngine.engine
                )
            }
        }
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        batch.projectionMatrix = camera.combined

        // Tout est rendu par le RenderSystem de l'ECS
        batch.begin()
        ecsEngine.update(delta)
        batch.end()
    }

    override fun hide() {
        inputController.deactivate()
    }

    override fun resize(width: Int, height: Int) {
        val oldX = camera.position.x
        val oldY = camera.position.y
        val oldZoom = camera.zoom
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.position.set(oldX, oldY, 0f)
        camera.zoom = oldZoom
        inputController.onViewportChanged()
    }

    override fun dispose() {
        if (::inputController.isInitialized) {
            inputController.deactivate()
        }
        batch.dispose()
        ecsEngine.dispose()
    }

    private fun centerEntity(entity: Entity) {
        val position = entity.getComponent(PositionComponent::class.java)
        val sprite = entity.getComponent(SpriteComponent::class.java)
        position.x = -(sprite.width / 2f)
        position.y = -(sprite.height / 2f)
    }

    private fun buildCameraDragBounds(entity: Entity): CameraDragBounds {
        val position = entity.getComponent(PositionComponent::class.java)
        val sprite = entity.getComponent(SpriteComponent::class.java)
        val width = sprite.width * sprite.scaleX
        val height = sprite.height * sprite.scaleY

        return CameraDragBounds(
            left = position.x,
            right = position.x + width,
            bottom = position.y,
            top = position.y + height
        )
    }
}
