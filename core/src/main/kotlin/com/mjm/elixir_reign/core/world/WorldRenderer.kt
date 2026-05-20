package com.mjm.elixir_reign.core.world

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Disposable
import com.mjm.elixir_reign.core.terrain.TerrainRenderer
import com.mjm.elixir_reign.shared.logic.IsometricGeometry
import com.mjm.elixir_reign.shared.world.WorldMap

class WorldRenderer(
    worldMap: WorldMap,
    scale: Float = 4f
) : Disposable {
    private val geometry = IsometricGeometry(worldMap, scale)
    private val terrainRenderer = TerrainRenderer(
        geometry = geometry,
        worldMap = worldMap,
        scale = scale
    )

    fun renderGround(batch: SpriteBatch) {
        terrainRenderer.render(batch)
    }

    fun renderOverlay(batch: SpriteBatch) {
        // La couche OVERLAY n'est pas encore modelisee.
    }

    fun renderChunkDebug(shapeRenderer: ShapeRenderer) {
        terrainRenderer.renderChunkDebug(shapeRenderer)
    }

    fun renderChunkDebugLabels(batch: SpriteBatch, font: BitmapFont) {
        terrainRenderer.renderChunkDebugLabels(batch, font)
    }

    fun worldBounds(): Rectangle {
        return terrainRenderer.worldBounds()
    }

    override fun dispose() {
        terrainRenderer.dispose()
    }
}
