package com.mjm.elixir_reign.android.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.world.GameWorld
import com.mjm.elixir_reign.core.world.WorldRenderer
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.grid.IsometricCoordinateConverter
import com.mjm.elixir_reign.core.grid.IsometricGridRenderer
import com.mjm.elixir_reign.core.handler.BuildPlacementHandler
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.core.terrain.TerrainPresets
import com.mjm.elixir_reign.core.ui.NineSliceImageButton
import com.mjm.elixir_reign.android.ui.Shop
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.shared.data.BuildingDefinition
import com.mjm.elixir_reign.shared.ecs.systems.PlacementEventHandler
import com.mjm.elixir_reign.shared.ecs.systems.PlacementSystem
import com.mjm.elixir_reign.shared.events.EventBus
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.logic.IsometricGeometry
import com.mjm.elixir_reign.shared.world.GridOccupancyData
import com.mjm.elixir_reign.shared.world.WorldMap

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

class GameScreen(@Suppress("UNUSED_PARAMETER") game: Main) : ScreenAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var gameWorld: GameWorld
    private lateinit var selectionInputHandler: SelectionInputHandler
    private lateinit var terrainBounds: Rectangle
    private lateinit var uiStage: Stage
    private lateinit var btnSelectTroops: NineSliceImageButton
    private lateinit var placementControlsTable: Table
    private lateinit var btnConfirmPlacement: TextButton
    private lateinit var btnCancelPlacement: TextButton
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var worldMap: WorldMap
    private var isSelectionMode = false

    private val activeTouches = mutableMapOf<Int, Vector2>()
    private var isConstructionGridVisible = false
    private var pinchState: PinchState? = null
    private var placementDragPointer: Int? = null

    private lateinit var buildPlacementHandler: BuildPlacementHandler
    private lateinit var placementEventHandler: PlacementEventHandler
    private lateinit var eventBus: EventBus
    private lateinit var gridRenderer: IsometricGridRenderer
    private lateinit var gridOccupancy: GridOccupancyData
    private lateinit var coordinateConverter: IsometricCoordinateConverter

    private val input = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (handlePlacementControlsTouch(screenX, screenY)) {
                return true
            }

            if (buildPlacementHandler.isPlacementModeActive()) {
                val screenXf = screenX.toFloat()
                val screenYf = screenY.toFloat()
                if (buildPlacementHandler.isTouchOnPreview(screenXf, screenYf, camera)) {
                    placementDragPointer = pointer
                    buildPlacementHandler.updateHover(camera, screenXf, screenYf)
                    return true
                }

                activeTouches[pointer] = Vector2(screenXf, screenYf)
                if (activeTouches.size >= 2) {
                    beginPinch()
                }
                return true
            }

            val worldCoords = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
            selectionInputHandler.moveSelectedEntitiesToTarget(worldCoords.x, worldCoords.y)
            // emulate double click
            selectionInputHandler.touchDown(screenX, screenY, camera)
            if(isSelectionMode) {
                selectionInputHandler.touchDown(screenX, screenY, camera)
            }

            activeTouches[pointer] = Vector2(screenX.toFloat(), screenY.toFloat())

            if (activeTouches.size >= 2) {
                beginPinch()
            }

            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
			if (placementDragPointer == pointer) {
				buildPlacementHandler.updateHover(camera, screenX.toFloat(), screenY.toFloat())
				return true
			}

            // Si mode double-clic actif, faire le drag selection
             if (isSelectionMode) {
                 selectionInputHandler.touchDragged(screenX, screenY, camera)
                 return true
             }

            val previousTouch = activeTouches[pointer] ?: return false

            if (pinchState != null && !isSelectionMode) {
                previousTouch.set(screenX.toFloat(), screenY.toFloat())
                updatePinchZoom()
                return true
            }

            if (activeTouches.size != 1) {
                return false
            }

            val deltaX = screenX.toFloat() - previousTouch.x
            val deltaY = screenY.toFloat() - previousTouch.y

            // Sinon, bouger la caméra normalement
            camera.translate(-deltaX * camera.zoom, deltaY * camera.zoom, 0f)
            clampCameraPosition()
            camera.update()
            previousTouch.set(screenX.toFloat(), screenY.toFloat())
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
			if (placementDragPointer == pointer) {
				placementDragPointer = null
				return true
			}

            // Finaliser la sélection/drag selection
            if (!buildPlacementHandler.isPlacementModeActive()) {
                selectionInputHandler.touchUp()
                isSelectionMode = false
                updateSelectionButtonState()
            }

            activeTouches.remove(pointer)

            if (activeTouches.size >= 2) {
                beginPinch()
            } else {
                endPinch()
            }
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (amountY == 0f) {
                return false
            }

            applyZoom(camera.zoom * (1f + amountY * SCROLL_ZOOM_STEP))
            return true
        }
    }

    override fun show() {
        camera = OrthographicCamera()
        camera.setToOrtho(false, Gdx.graphics.width.toFloat(), Gdx.graphics.height.toFloat())
        camera.position.set(0f, 0f, 0f) // centre caméra = (0,0)
        camera.update()

        shapeRenderer = ShapeRenderer()
        batch = SpriteBatch()

        val worldMap = TerrainPresets.map()
        worldRenderer = WorldRenderer(worldMap)
        this.worldMap = worldMap

        // Initialiser le monde du jeu (encapsule CoreGameEngine)
        gameWorld = GameWorld(batch, camera)

        // Récupérer le selectionInputHandler depuis le CoreGameEngine
        selectionInputHandler = gameWorld.coreEngine.selectionInputHandler
        terrainBounds = worldRenderer.worldBounds()

        val isometricGeometry = IsometricGeometry(worldMap, scale = 4f)
        val coordinateConverter = IsometricCoordinateConverter(isometricGeometry)
        val gridRenderer = IsometricGridRenderer(isometricGeometry)
        val gridOccupancy = GridOccupancyData(rows = worldMap.height, cols = worldMap.width)
        this.gridOccupancy = gridOccupancy

        val placementSystem = PlacementSystem(
            worldMap = worldMap,
            geometry = isometricGeometry,
            occupancy = gridOccupancy,
            spawnBuilding = { entityType, x, y ->
                SpriteEntityFactory.createBuilding(
                    entityType = entityType,
                    x = x,
                    y = y,
                    engine = gameWorld.coreEngine.engine
                )
            }
        )

        eventBus = EventBus()
        placementEventHandler = PlacementEventHandler(eventBus, placementSystem)

        buildPlacementHandler = BuildPlacementHandler(
            worldMap = worldMap,
            coordinateConverter = coordinateConverter,
            gridRenderer = gridRenderer,
            placementSystem = placementSystem,
            eventBus = eventBus
        )

        Shop.setOnBuildingSelected { selection: BuildingDefinition ->
            buildPlacementHandler.selectBuilding(selection.entityType, selection.stats, activatePlacement = true)
            centerPlacementPreviewOnScreen()
        }

        this.gridRenderer = gridRenderer
        this.coordinateConverter = coordinateConverter

        SpriteEntityFactory.createUnit(
            entityType = EntityType.BARBARIAN,
            x = 0f,
            y = 0f,
            engine = gameWorld.coreEngine.engine
        )

        configureCamera(resetView = true)

        show_UI()
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // IMPORTANT : la caméra bouge => il faut réassigner camera.combined à chaque frame
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        // Mise à jour + rendu des entités ECS (SpriteBatch géré par RenderSystem)
        batch.begin()
        worldRenderer.renderGround(batch)
        batch.end()

        if (isConstructionGridVisible) {
            gridRenderer.render(shapeRenderer) { row, col ->
                val terrain = worldMap[row, col] ?: return@render false
                terrain.canBuildOn && !gridOccupancy.isOccupied(row, col)
            }
        }

        batch.begin()
        gameWorld.update(delta)
        worldRenderer.renderOverlay(batch)
        batch.end()

        if (buildPlacementHandler.isPlacementModeActive()) {
            buildPlacementHandler.renderPreview(delta, batch, shapeRenderer)
        }

        updatePlacementControls()

        if (selectionInputHandler.isDraggingNow()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)

            if (selectionInputHandler.isDraggingNow()) {
                val dragRect = selectionInputHandler.getDragRectangle()
                shapeRenderer.color.set(0.5f, 1f, 0.5f, 0.8f)
                shapeRenderer.rect(dragRect.x, dragRect.y, dragRect.width, dragRect.height)
            }
            shapeRenderer.end()
        }

        uiStage.act(delta)
        uiStage.draw()
    }

    override fun resize(width: Int, height: Int) {
        // Quand la taille de l'écran change, on doit réajuster la caméra pour que les coordonnées restent cohérentes
        val oldX = camera.position.x
        val oldY = camera.position.y
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.position.set(oldX, oldY, 0f)
        configureCamera(resetView = false)
        uiStage.viewport.update(width, height, true)
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        gameWorld.dispose()
        uiStage.dispose()
        placementEventHandler.dispose()
        worldRenderer.dispose()
    }

    private fun show_UI() {
        uiStage = Stage(ScreenViewport())

        uiStage.addActor(Shop)

        Shop.setOnShopShown { isConstructionGridVisible = true }
        Shop.setOnShopHidden { isConstructionGridVisible = false }

        val btnBuildMenu = NineSliceImageButton(UiAssets.texture(UiImage.BUTTON_9PATCH), UiAssets.texture(UiImage.ICON_HAMMER)).apply {
            onClick { _, _ ->
                Shop.show()
            }
        }

        btnSelectTroops = NineSliceImageButton(UiAssets.texture(UiImage.BUTTON_9PATCH), UiAssets.texture(UiImage.ICON_SELECT), toggleVisuals = true).apply {
            onClick { _, _ ->
                isSelectionMode = !isSelectionMode
                updateSelectionButtonState()
            }
        }
        updateSelectionButtonState()

        btnConfirmPlacement = TextButton("Valider", UiAssets.skin).apply {
            isDisabled = true
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (buildPlacementHandler.confirmPlacement()) {
                        Shop.hide()
                    }
                }
            })
        }

        btnCancelPlacement = TextButton("Annuler", UiAssets.skin).apply {
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    buildPlacementHandler.cancelPlacement()
                }
            })
        }

        placementControlsTable = Table().apply {
            isVisible = false
            add(btnConfirmPlacement).padRight(8f)
            add(btnCancelPlacement)
            pack()
        }

        // TODO: Review this methode of drawing multiple elements
        val hudLeftTable = Table().apply {
            setFillParent(true)
            bottom().left()
            add(btnBuildMenu).size(96f).pad(24f)
        }

        val hudRightTable = Table().apply{
            setFillParent(true)
            bottom().right()
            add(btnSelectTroops).size(96f).pad(24f)
        }

        uiStage.addActor(hudLeftTable)
        uiStage.addActor(hudRightTable)
        uiStage.addActor(placementControlsTable)
        Gdx.input.inputProcessor = InputMultiplexer(uiStage, input)
    }

    private fun updatePlacementControls() {
        if (!this::placementControlsTable.isInitialized || !buildPlacementHandler.isPlacementModeActive()) {
            if (this::placementControlsTable.isInitialized) {
                placementControlsTable.isVisible = false
            }
            return
        }

        val previewAnchor = buildPlacementHandler.getPreviewAnchorWorldPosition()
        if (previewAnchor == null) {
            placementControlsTable.isVisible = false
            return
        }

        val screenAnchor = camera.project(Vector3(previewAnchor.x, previewAnchor.y, 0f))
        val padding = 18f
        val x = (screenAnchor.x - placementControlsTable.width / 2f).coerceIn(12f, Gdx.graphics.width - placementControlsTable.width - 12f)
        val y = (screenAnchor.y + padding).coerceAtMost(Gdx.graphics.height - placementControlsTable.height - 12f)

        placementControlsTable.setPosition(x, y)
        placementControlsTable.isVisible = true
        placementControlsTable.toFront()
        btnConfirmPlacement.isDisabled = !buildPlacementHandler.canConfirmPlacement()
    }

    private fun handlePlacementControlsTouch(screenX: Int, screenY: Int): Boolean {
        if (!this::placementControlsTable.isInitialized || !placementControlsTable.isVisible) {
            return false
        }

        val stageCoords = uiStage.screenToStageCoordinates(Vector2(screenX.toFloat(), screenY.toFloat()))
        val localCoords = placementControlsTable.stageToLocalCoordinates(stageCoords)
        val hitActor = placementControlsTable.hit(localCoords.x, localCoords.y, true) ?: return false

        if (hitActor.isDescendantOf(btnConfirmPlacement)) {
            if (!btnConfirmPlacement.isDisabled && buildPlacementHandler.confirmPlacement()) {
                Shop.hide()
            }
            return true
        }

        if (hitActor.isDescendantOf(btnCancelPlacement)) {
            buildPlacementHandler.cancelPlacement()
            return true
        }

        return false
    }

    private fun centerPlacementPreviewOnScreen() {
        buildPlacementHandler.updateHover(
            camera = camera,
            screenX = Gdx.graphics.width / 2f,
            screenY = Gdx.graphics.height / 2f
        )
    }

    private fun updateSelectionButtonState() {
        if (!this::btnSelectTroops.isInitialized) {
            return
        }
        btnSelectTroops.setHighlighted(isSelectionMode)
    }

    private fun applyUiDebugRecursively(actor: Actor, enabled: Boolean) {
        actor.setDebug(enabled)
        if (actor is Group) {
            for (i in 0 until actor.children.size) {
                applyUiDebugRecursively(actor.children[i], enabled)
            }
        }
    }

    private fun beginPinch() {
        if (activeTouches.size < 2) {
            return
        }

        val touches = activeTouches.values.toList()
        pinchState = PinchState(
            initialDistance = touches[0].dst(touches[1]).coerceAtLeast(1f),
            initialZoom = camera.zoom
        )
    }

    private fun endPinch() {
        pinchState = null
    }

    private fun updatePinchZoom() {
        val currentPinchState = pinchState ?: return
        if (activeTouches.size < 2) {
            return
        }

        val touches = activeTouches.values.toList()
        val currentDistance = touches[0].dst(touches[1])

        if (currentDistance <= 0f) {
            return
        }

        applyZoom(currentPinchState.initialZoom * (currentPinchState.initialDistance / currentDistance))
    }

    private fun applyZoom(requestedZoom: Float) {
        val minZoomToFit = computeMinZoomToFit().coerceAtLeast(MAX_ZOOM_IN)
        camera.zoom = requestedZoom.coerceIn(MAX_ZOOM_IN, minZoomToFit)
        clampCameraPosition()
        camera.update()
    }

    private fun configureCamera(resetView: Boolean) {
        val minZoomToFit = computeMinZoomToFit().coerceAtLeast(MAX_ZOOM_IN)

        if (resetView) {
            camera.zoom = minZoomToFit
            camera.position.set(terrainBounds.x + terrainBounds.width / 2f, terrainBounds.y + terrainBounds.height / 2f, 0f)
        } else {
            camera.zoom = camera.zoom.coerceIn(MAX_ZOOM_IN, minZoomToFit)
        }

        clampCameraPosition()
        camera.update()
    }

    private fun computeMinZoomToFit(): Float {
        val fitZoomX = (terrainBounds.width + MIN_ZOOM_PADDING_X * 2f) / camera.viewportWidth
        val fitZoomY = (terrainBounds.height + MIN_ZOOM_PADDING_Y * 2f) / camera.viewportHeight
        return maxOf(1f, fitZoomX, fitZoomY)
    }

    private fun clampCameraPosition() {
        val visibleHalfWidth = camera.viewportWidth * camera.zoom / 2f
        val visibleHalfHeight = camera.viewportHeight * camera.zoom / 2f

        val minCameraX = terrainBounds.x - DRAG_PADDING_X + visibleHalfWidth
        val maxCameraX = terrainBounds.x + terrainBounds.width + DRAG_PADDING_X - visibleHalfWidth
        val minCameraY = terrainBounds.y - DRAG_PADDING_Y + visibleHalfHeight
        val maxCameraY = terrainBounds.y + terrainBounds.height + DRAG_PADDING_Y - visibleHalfHeight

        camera.position.x = if (minCameraX <= maxCameraX) {
            camera.position.x.coerceIn(minCameraX, maxCameraX)
        } else {
            terrainBounds.x + terrainBounds.width / 2f
        }

        camera.position.y = if (minCameraY <= maxCameraY) {
            camera.position.y.coerceIn(minCameraY, maxCameraY)
        } else {
            terrainBounds.y + terrainBounds.height / 2f
        }
    }

    companion object {
        private const val MAX_ZOOM_IN = 0.6f
        private const val SCROLL_ZOOM_STEP = 0.1f
        private const val MIN_ZOOM_PADDING_X = 96f
        private const val MIN_ZOOM_PADDING_Y = 96f
        private const val DRAG_PADDING_X = 48f
        private const val DRAG_PADDING_Y = 96f
    }

    private data class PinchState(
        val initialDistance: Float,
        val initialZoom: Float
    )
}
