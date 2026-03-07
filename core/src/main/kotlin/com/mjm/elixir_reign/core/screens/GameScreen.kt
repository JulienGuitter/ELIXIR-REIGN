package com.mjm.elixir_reign.core.screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.ecs.CoreGameEngine
import com.mjm.elixir_reign.core.ecs.components.TextureRegionComponent
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.tools.sprites.sprite_sheet.SpriteSheetParser
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

const val CAMERA_ZOOM = 0.5f

class GameScreen(private val game: Main) : ScreenAdapter() {
    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var ecsEngine: CoreGameEngine

    private val lastTouch = Vector2()
    private val cubePosition = Vector2(0f, 0f)

    private val input = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            lastTouch.set(screenX.toFloat(), screenY.toFloat())
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val deltaX = screenX - lastTouch.x
            val deltaY = screenY - lastTouch.y

            camera.translate(-deltaX, deltaY)
            camera.update()

            lastTouch.set(screenX.toFloat(), screenY.toFloat())
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                game.platform.onBackPressed(game)
                return true
            }
            return false
        }
    }

    override fun show() {
        camera = OrthographicCamera()
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 0f)
        camera.zoom = CAMERA_ZOOM
        camera.update()

        batch = SpriteBatch()

        // Initialiser l'engine ECS avec le batch
        ecsEngine = CoreGameEngine(batch)

        // Créer une tile de terrain (ground_1) via ECS
        createTerrainTile("ground_1", cubePosition.x, cubePosition.y)

        // Créer une entité barbare au centre de la scène
        SpriteEntityFactory.createUnit(
            unitType = UnitType.BARBARIAN,
            x = 0f,
            y = 0f,
            engine = ecsEngine.engine
        )
        SpriteEntityFactory.createUnit(
            unitType = UnitType.ARCHER,
            x = 150f,
            y = 150f,
            engine = ecsEngine.engine
        )

        Gdx.input.inputProcessor = input
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

    override fun resize(width: Int, height: Int) {
        val oldX = camera.position.x
        val oldY = camera.position.y
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.position.set(oldX, oldY, 0f)
        camera.update()
    }

    override fun dispose() {
        batch.dispose()
        ecsEngine.dispose()
    }

    /**
     * Crée une entité ECS pour une tile de terrain.
     * Utilise le SpriteSheetParser existant pour lire le JSON terrain
     * et TextureManager pour charger la texture.
     *
     * @param clipName nom du clip dans le JSON (ground_1 … ground_7)
     * @param x position X en pixels
     * @param y position Y en pixels
     */
    private fun createTerrainTile(clipName: String, x: Float, y: Float) {
        val texturePath = "sprites/terrain/anim_pack/pack_ground.png"
        val jsonPath = "sprites/terrain/description/pack_ground.json"

        val spriteSheet = SpriteSheetParser().parseJson(jsonPath)
        val clip = spriteSheet.clips.first { it.name == clipName }
        val frame = clip.frames[0]

        val texture = TextureManager.getTexture(texturePath)
        val region = TextureRegion(
            texture,
            frame.x,
            frame.y,
            spriteSheet.cellWidth,
            spriteSheet.cellHeight
        )

        val entity = Entity()
        entity.add(PositionComponent(x, y))
        entity.add(TextureRegionComponent(region))
        entity.add(SpriteComponent(
            texturePath = texturePath,
            width = spriteSheet.cellWidth,
            height = spriteSheet.cellHeight
        ))

        ecsEngine.engine.addEntity(entity)
    }
}
