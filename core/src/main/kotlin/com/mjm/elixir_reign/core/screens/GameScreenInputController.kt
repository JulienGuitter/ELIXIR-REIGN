package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.input.GestureDetector
import com.mjm.elixir_reign.core.Main
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val SCROLL_ZOOM_STEP = 0.08f

internal data class CameraDragBounds(
    val left: Float,
    val right: Float,
    val bottom: Float,
    val top: Float
) {
    val centerX: Float = (left + right) / 2f
    val centerY: Float = (bottom + top) / 2f
}

internal class GameScreenInputController(
    private val game: Main,
    private val camera: OrthographicCamera,
    private val dragBounds: CameraDragBounds
) {
    private val config = game.platform.gameInputConfig
    private var pinchZoomStart = camera.zoom
    private var pinchZoomActive = false

    private val gestureDetector = GestureDetector(object : GestureDetector.GestureAdapter() {
        override fun touchDown(x: Float, y: Float, pointer: Int, button: Int): Boolean {
            return true
        }

        override fun pan(x: Float, y: Float, deltaX: Float, deltaY: Float): Boolean {
            camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom)
            clampCameraPosition()
            return true
        }

        override fun zoom(initialDistance: Float, distance: Float): Boolean {
            if (!config.touchZoomEnabled || initialDistance <= 0f || distance <= 0f) {
                return false
            }

            if (!pinchZoomActive) {
                pinchZoomStart = camera.zoom
                pinchZoomActive = true
            }

            setCameraZoom(pinchZoomStart * (initialDistance / distance))
            return true
        }

        override fun pinchStop() {
            pinchZoomActive = false
        }
    })

    private val keyboardAndScrollInput = object : InputAdapter() {
        override fun keyDown(keycode: Int): Boolean {
            if (!game.platform.isBackKey(keycode)) {
                return false
            }

            game.platform.onBackPressed(game)
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (!game.platform.shouldZoomCameraFromScroll(amountX, amountY, isCtrlPressed())) {
                return false
            }

            setCameraZoom(camera.zoom + (amountY * SCROLL_ZOOM_STEP))
            return true
        }
    }

    private val inputProcessor: InputProcessor = InputMultiplexer(keyboardAndScrollInput, gestureDetector)

    init {
        setCameraZoom(camera.zoom)
    }

    fun activate() {
        if (config.catchesBackKey) {
            Gdx.input.setCatchKey(Input.Keys.BACK, true)
        }

        clampCameraPosition()
        Gdx.input.inputProcessor = inputProcessor
    }

    fun deactivate() {
        if (config.catchesBackKey) {
            Gdx.input.setCatchKey(Input.Keys.BACK, false)
        }

        if (Gdx.input.inputProcessor === inputProcessor) {
            Gdx.input.inputProcessor = null
        }

        pinchZoomActive = false
    }

    private fun isCtrlPressed(): Boolean {
        return Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) ||
            Gdx.input.isKeyPressed(Input.Keys.CONTROL_RIGHT)
    }

    private fun setCameraZoom(nextZoom: Float) {
        camera.zoom = max(config.minCameraZoom, min(config.maxCameraZoom, nextZoom))
        clampCameraPosition()
    }

    fun onViewportChanged() {
        clampCameraPosition()
    }

    private fun clampCameraPosition() {
        val halfViewportWidth = camera.viewportWidth * camera.zoom / 2f
        val halfViewportHeight = camera.viewportHeight * camera.zoom / 2f

        val clampPadding = 50f

        camera.position.x = clampAxis(
            value = camera.position.x,
            minValue = dragBounds.left + halfViewportWidth - clampPadding,
            maxValue = dragBounds.right - halfViewportWidth + clampPadding,
            fallback = dragBounds.centerX
        )
        camera.position.y = clampAxis(
            value = camera.position.y,
            minValue = dragBounds.bottom + halfViewportHeight - clampPadding,
            maxValue = dragBounds.top - halfViewportHeight + clampPadding,
            fallback = dragBounds.centerY
        )
        camera.update()
    }

    private fun clampAxis(value: Float, minValue: Float, maxValue: Float, fallback: Float): Float {
        if (minValue > maxValue || abs(minValue - maxValue) < 0.001f) {
            return fallback
        }

        return max(minValue, min(maxValue, value))
    }
}
