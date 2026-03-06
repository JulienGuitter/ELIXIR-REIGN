package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.core.Main

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

class GameScreen(private val game: Main) : ScreenAdapter() {

    private val tileSize = 256f
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera

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
        camera.position.set(0f, 0f, 0f) // centre caméra = (0,0)
        camera.update()

        shapeRenderer = ShapeRenderer()

        Gdx.input.inputProcessor = input
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // IMPORTANT : la caméra bouge => il faut réassigner camera.combined à chaque frame
        shapeRenderer.projectionMatrix = camera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        drawIsoCube(cubePosition.x, cubePosition.y)
        shapeRenderer.end()
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
        // Libérer les ressources graphiques
        shapeRenderer.dispose()
    }

    private fun drawIsoCube(x: Float, y: Float) {
        val cubeHeight = tileSize / 2f
        val halfW = tileSize / 2f
        val halfH = tileSize / 4f

        // TOP
        shapeRenderer.color = Color.LIGHT_GRAY
        shapeRenderer.triangle(x, y + halfH, x - halfW, y, x + halfW, y)
        shapeRenderer.triangle(x, y - halfH, x - halfW, y, x + halfW, y)

        // LEFT
        shapeRenderer.color = Color.GRAY
        shapeRenderer.triangle(x - halfW, y, x - halfW, y - cubeHeight, x, y - halfH)
        shapeRenderer.triangle(x - halfW, y - cubeHeight, x, y - cubeHeight - halfH, x, y - halfH)

        // RIGHT
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.triangle(x + halfW, y, x + halfW, y - cubeHeight, x, y - halfH)
        shapeRenderer.triangle(x + halfW, y - cubeHeight, x, y - cubeHeight - halfH, x, y - halfH)
    }
}
