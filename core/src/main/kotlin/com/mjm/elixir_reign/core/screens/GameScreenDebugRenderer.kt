package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mjm.elixir_reign.core.ecs.CoreGameEngine
import com.mjm.elixir_reign.shared.logic.IsometricGeometry

class GameScreenDebugRenderer(
    private val coreEngine: CoreGameEngine,
    private val geometry: IsometricGeometry
) {
    private var enabled = false

    fun toggleDebug() {
        enabled = !enabled
    }

    fun renderEntityDebugVisuals(shapeRenderer: ShapeRenderer) {
        if (!enabled) {
            return
        }
        // Intentionally left minimal; wire specific debug visuals later.
    }

    fun renderSelectionCircles(shapeRenderer: ShapeRenderer) {
        if (!enabled) {
            return
        }
        // Intentionally left minimal; wire selection overlays later.
    }

    fun renderOffsetVectors(shapeRenderer: ShapeRenderer) {
        if (!enabled) {
            return
        }
        // Intentionally left minimal; wire offset vectors later.
    }
}

