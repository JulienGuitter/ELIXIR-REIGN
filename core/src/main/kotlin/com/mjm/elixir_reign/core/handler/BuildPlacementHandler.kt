package com.mjm.elixir_reign.core.handler

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.core.grid.IsometricCoordinateConverter
import com.mjm.elixir_reign.core.grid.IsometricGridRenderer
import com.mjm.elixir_reign.core.tools.SpritePositionCalculator
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimator
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.ecs.systems.PlacementSystem
import com.mjm.elixir_reign.shared.events.EventBus
import com.mjm.elixir_reign.shared.events.PlacementRequestEvent
import com.mjm.elixir_reign.shared.logic.BuildingState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.world.WorldMap

class BuildPlacementHandler(
    private val worldMap: WorldMap,
    private val coordinateConverter: IsometricCoordinateConverter,
    private val gridRenderer: IsometricGridRenderer,
    private val placementSystem: PlacementSystem,
    private val eventBus: EventBus,
    private val placementBuildingType: EntityType = EntityType.DARCKELEXIR_PUMP,
    private val placementBuildingStats: BuildingStats = BuildingStats.DARCKELEXIR_PUMP
) {
    private val placementAnimator: SpriteAnimator = SpriteAnimationManager.createBuildingAnimator(
        stats = placementBuildingStats,
        buildingState = BuildingState.IDLE
    )

    private var isPlacementMode = false
    private var hoveredPlacementCell: Pair<Int, Int>? = null
    private var hoveredPlacementFootprintCells: List<Pair<Int, Int>> = emptyList()
    private val hoveredPlacementWorld = Vector2()
    private var canPlaceAtHoveredCell = false

    fun togglePlacementMode() {
        isPlacementMode = !isPlacementMode
        if (!isPlacementMode) {
            clearPreviewState()
        }
    }

    fun isPlacementModeActive(): Boolean = isPlacementMode

    fun updateHover(camera: OrthographicCamera, screenX: Float = Gdx.input.x.toFloat(), screenY: Float = Gdx.input.y.toFloat()) {
        if (!isPlacementMode) {
            return
        }

        val world = coordinateConverter.screenToWorld(screenX, screenY, camera)
        val gridPos = coordinateConverter.worldToGrid(world.x, world.y)

        if (gridPos == null) {
            clearPreviewState()
            return
        }

        val (row, col) = gridPos

        if (!coordinateConverter.isGridPositionValid(row, col) || worldMap[row, col] == null) {
            clearPreviewState()
            return
        }

        hoveredPlacementCell = Pair(row, col)
        hoveredPlacementFootprintCells = getFootprintCells(
            centerRow = row,
            centerCol = col,
            size = placementBuildingStats.footprintSizeTiles
        )

        val worldPos = placementSystem.computePlacementWorldPosition(row, col)
        hoveredPlacementWorld.set(worldPos.x, worldPos.y)
        canPlaceAtHoveredCell = placementSystem.canPlace(
            row = row,
            col = col,
            building = buildingToPlace()
        )
    }

    fun tryPlaceFromTap(screenX: Float, screenY: Float, camera: OrthographicCamera): Boolean {
        if (!isPlacementMode) {
            return false
        }

        updateHover(camera, screenX, screenY)

        if (!canPlaceAtHoveredCell) {
            return false
        }

        val hoveredCell = hoveredPlacementCell ?: return false
        val event = PlacementRequestEvent(
            row = hoveredCell.first,
            col = hoveredCell.second,
            building = buildingToPlace()
        )
        eventBus.publish(event)
        return event.accepted
    }

    fun renderPreview(delta: Float, batch: SpriteBatch, shapeRenderer: ShapeRenderer) {
        if (!isPlacementMode) {
            return
        }
        hoveredPlacementCell ?: return

        placementAnimator.update(delta)
        val region = placementAnimator.getCurrentTextureRegion() ?: return
        val previewColor = if (canPlaceAtHoveredCell) VALID_PREVIEW_COLOR else INVALID_PREVIEW_COLOR
        val gridColor = if (canPlaceAtHoveredCell) VALID_GRID_HIGHLIGHT else INVALID_GRID_HIGHLIGHT

        // Utiliser le calculator pour obtenir la position exactement comme RenderSystem
        val spriteWidth = placementAnimator.spriteSheet.cellWidth
        val spriteHeight = placementAnimator.spriteSheet.cellHeight
        val (drawX, drawY) = SpritePositionCalculator.calculateDrawPosition(
            worldX = hoveredPlacementWorld.x,
            worldY = hoveredPlacementWorld.y,
            spriteWidth = spriteWidth,
            spriteHeight = spriteHeight,
            scaleX = BUILDING_PREVIEW_SCALE,
            scaleY = BUILDING_PREVIEW_SCALE,
            offsetX = -placementAnimator.spriteSheet.footX,
            offsetY = -placementAnimator.spriteSheet.footY
        )

        val scaledWidth = spriteWidth * BUILDING_PREVIEW_SCALE
        val scaledHeight = spriteHeight * BUILDING_PREVIEW_SCALE

        batch.begin()
        batch.setColor(previewColor)
        batch.draw(region, drawX, drawY, scaledWidth, scaledHeight)
        batch.setColor(1f, 1f, 1f, 1f)
        batch.end()

        // === DEBUG VISUAL : Afficher les positions du preview ===
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

        // Point de placement théorique (vert)
        shapeRenderer.color.set(0f, 1f, 0f, 1f)
        shapeRenderer.circle(hoveredPlacementWorld.x, hoveredPlacementWorld.y, 5f)
        shapeRenderer.line(
            hoveredPlacementWorld.x - 15f, hoveredPlacementWorld.y,
            hoveredPlacementWorld.x + 15f, hoveredPlacementWorld.y
        )
        shapeRenderer.line(
            hoveredPlacementWorld.x, hoveredPlacementWorld.y - 15f,
            hoveredPlacementWorld.x, hoveredPlacementWorld.y + 15f
        )

        // Rectangle du preview (avec les offsets appliqués)
        shapeRenderer.color.set(0.2f, 1f, 0.2f, 0.7f)
        shapeRenderer.rect(drawX, drawY, scaledWidth, scaledHeight)

        // Vecteur montrant l'offset appliqué
        val offsetX = scaledWidth * (-placementAnimator.spriteSheet.footX)
        val offsetY = scaledHeight * (-placementAnimator.spriteSheet.footY)
        shapeRenderer.color.set(1f, 1f, 0f, 1f)
        shapeRenderer.line(
            hoveredPlacementWorld.x, hoveredPlacementWorld.y,
            hoveredPlacementWorld.x + offsetX, hoveredPlacementWorld.y + offsetY
        )

        shapeRenderer.end()

        gridRenderer.highlightTiles(shapeRenderer, hoveredPlacementFootprintCells, gridColor)
    }

    private fun clearPreviewState() {
        hoveredPlacementCell = null
        hoveredPlacementFootprintCells = emptyList()
        canPlaceAtHoveredCell = false
    }

    private fun buildingToPlace(): PlacementSystem.BuildingToPlace {
        return PlacementSystem.BuildingToPlace(
            entityType = placementBuildingType,
            stats = placementBuildingStats
        )
    }

    private fun getFootprintCells(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
        val normalizedSize = size.coerceAtLeast(1)
        val startRow = centerRow - normalizedSize / 2
        val startCol = centerCol - normalizedSize / 2
        val cells = ArrayList<Pair<Int, Int>>(normalizedSize * normalizedSize)
        for (row in startRow until startRow + normalizedSize) {
            for (col in startCol until startCol + normalizedSize) {
                cells += Pair(row, col)
            }
        }
        return cells
    }

    companion object {
        private const val BUILDING_PREVIEW_SCALE = 3f
        private val VALID_PREVIEW_COLOR = Color(0.65f, 1f, 0.65f, 0.8f)
        private val INVALID_PREVIEW_COLOR = Color(1f, 0.45f, 0.45f, 0.7f)
        private val VALID_GRID_HIGHLIGHT = Color(0.2f, 1f, 0.2f, 0.95f)
        private val INVALID_GRID_HIGHLIGHT = Color(1f, 0.2f, 0.2f, 0.95f)
    }
}
