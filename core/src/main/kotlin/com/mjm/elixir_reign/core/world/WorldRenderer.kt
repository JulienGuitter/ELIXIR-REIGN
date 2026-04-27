package com.mjm.elixir_reign.core.world

import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Disposable
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.core.terrain.TerrainRenderer
import com.mjm.elixir_reign.shared.world.ChunkCoord
import com.mjm.elixir_reign.shared.world.WorldMap

class WorldRenderer(
    worldMap: WorldMap,
    scale: Float = 4f
) : Disposable {
    private val terrainRenderer = TerrainRenderer(
        worldMap = worldMap,
        scale = scale
    )

    fun renderGround(batch: SpriteBatch) {
        terrainRenderer.render(batch)
    }

    fun renderOverlay(batch: SpriteBatch) {
        // La couche OVERLAY n'est pas encore modelisee.
    }

    fun renderFog(batch: SpriteBatch, fogSnapshot: GameSession.FogSnapshot, elapsedSeconds: Float) {
        terrainRenderer.renderFog(batch, fogSnapshot, elapsedSeconds)
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

    fun tileCenterPosition(row: Int, col: Int): Vector2 {
        return terrainRenderer.tileCenterPosition(row, col)
    }

    fun tileCenterPosition(row: Float, col: Float): Vector2 {
        return terrainRenderer.tileCenterPosition(row, col)
    }

    fun tileAtWorldPosition(x: Float, y: Float): Pair<Int, Int> {
        return terrainRenderer.tileAtWorldPosition(x, y)
    }

    override fun dispose() {
        terrainRenderer.dispose()
    }
}
