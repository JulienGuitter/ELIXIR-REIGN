package com.mjm.elixir_reign.core.screens

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
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.tools.sprites.terrain.TerrainTileManager
import com.mjm.elixir_reign.core.tools.sprites.terrain.IsoTileSlice
import com.mjm.elixir_reign.core.tools.sprites.terrain.ComposedTile
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

const val CAMERA_ZOOM = 0.5f
const val MAP_SIZE = 1
const val TILE_SIZE = 128f

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

        // Charger les tiles de terrain avec le 9-slice isométrique
        TerrainTileManager.load()

        // Initialiser l'engine ECS avec le batch
        ecsEngine = CoreGameEngine(batch)

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

        // Dessiner la face du dessus avec la texture terrain + entités ECS (SpriteBatch)
        batch.begin()
        drawMap()
        ecsEngine.update(delta)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        // Quand la taille de l'écran change, on doit réajuster la caméra pour que les coordonnées restent cohérentes
        val oldX = camera.position.x
        val oldY = camera.position.y
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.position.set(oldX, oldY, 0f)
        camera.update()
    }

    override fun dispose() {
        batch.dispose()
        TerrainTileManager.dispose()
        ecsEngine.dispose()
    }

    /**
     * Dessine la map isométrique avec la texture terrain.
     * Affiche les 9 slices de la tile séparément avec un espacement pour visualiser le découpage.
     */
    private fun drawMap() {
        val slices = TerrainTileManager.getAllSlices(5)
        val spacing = 1f // espacement en pixels entre chaque slice

        drawIsoTile(cubePosition.x, cubePosition.y, slices, spacing)
    }

    /**
     * Dessine une tile isométrique composée de 9 slices à la position donnée.
     *
     * La tile source (114x88) est déjà un losange isométrique rendu dans un rectangle.
     * Les slices sont découpés en grille 3x3 rectangulaire et ré-assemblés de la même
     * manière pour reconstruire le losange original.
     *
     * @param x position X du coin inférieur gauche de la tile
     * @param y position Y du coin inférieur gauche de la tile
     * @param slices les 9 TextureRegion à dessiner
     * @param spacing espacement entre les slices (0 = collés, >0 = séparés pour debug)
     */
    private fun drawIsoTile(
        x: Float,
        y: Float,
        slices: Map<IsoTileSlice, TextureRegion>,
        spacing: Float = 0f
    ) {
        val sliceW = TerrainTileManager.sliceWidth.toFloat()
        val sliceH = TerrainTileManager.sliceHeight.toFloat()

        for (slice in IsoTileSlice.entries) {
            val textureRegion = slices[slice] ?: continue
            // col va de gauche à droite (0,1,2)
            // row 0 = haut de la texture = Y le plus grand dans libGDX (Y-up)
            val posX = x + slice.col * (sliceW + spacing)
            val posY = y + (2 - slice.row) * (sliceH + spacing)
            batch.draw(textureRegion, posX, posY, sliceW, sliceH)
        }
    }
}
