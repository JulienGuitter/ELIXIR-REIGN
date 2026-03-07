package com.mjm.elixir_reign.core.debug

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch

class FpsCounterOverlay {
    private val camera = OrthographicCamera()
    private val batch = SpriteBatch()
    private val font = BitmapFont().apply {
        color = Color.WHITE
    }

    private val padding = 16f
    private val shadowOffset = 2f

    init {
        resize(Gdx.graphics.width, Gdx.graphics.height)
    }

    fun resize(width: Int, height: Int) {
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()
        font.data.setScale((height / 540f).coerceIn(1.25f, 2.5f))
    }

    fun render() {
        val fpsLabel = "FPS: ${Gdx.graphics.framesPerSecond}"
        val y = camera.viewportHeight - padding

        batch.projectionMatrix = camera.combined
        batch.begin()
        font.color = Color.BLACK
        font.draw(batch, fpsLabel, padding + shadowOffset, y - shadowOffset)
        font.color = Color.WHITE
        font.draw(batch, fpsLabel, padding, y)
        batch.end()
    }

    fun dispose() {
        batch.dispose()
        font.dispose()
    }
}
