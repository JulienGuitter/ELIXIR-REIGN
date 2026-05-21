package com.mjm.elixir_reign.android.screens

import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils
import com.mjm.elixir_reign.core.world.GameWorld
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.grid.IsometricCoordinateConverter
import com.mjm.elixir_reign.core.grid.IsometricGridRenderer
import com.mjm.elixir_reign.core.handler.BuildPlacementHandler
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.core.terrain.TerrainPresets
import com.mjm.elixir_reign.core.ui.BarracksPanel
import com.mjm.elixir_reign.core.ui.NineSliceImageButton
import com.mjm.elixir_reign.android.ui.Shop
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.shared.GameConfiguration
import com.mjm.elixir_reign.core.world.WorldRenderer
import com.mjm.elixir_reign.shared.data.BuildingDefinition
import com.mjm.elixir_reign.shared.data.BuildingStats
import com.mjm.elixir_reign.shared.ecs.components.BarracksComponent
import com.mjm.elixir_reign.shared.ecs.components.BarracksTrainingProgress
import com.mjm.elixir_reign.shared.ecs.components.BuildingLevelComponent
import com.mjm.elixir_reign.shared.ecs.components.BuildingStateComponent
import com.mjm.elixir_reign.shared.ecs.components.EntityTypeComponent
import com.mjm.elixir_reign.shared.ecs.components.HealthComponent
import com.mjm.elixir_reign.shared.logic.BuildingState
import com.mjm.elixir_reign.shared.ecs.components.NetworkBuildingComponent
import com.mjm.elixir_reign.shared.ecs.components.OwnerComponent
import com.mjm.elixir_reign.shared.ecs.systems.PlacementEventHandler
import com.mjm.elixir_reign.shared.ecs.systems.PlacementSystem
import com.mjm.elixir_reign.shared.events.EventBus
import com.mjm.elixir_reign.shared.game.BuildingInstanceState
import com.mjm.elixir_reign.shared.logic.EntityType
import com.mjm.elixir_reign.shared.logic.IsometricGeometry
import com.mjm.elixir_reign.shared.network.PlayerConnectionState
import com.mjm.elixir_reign.core.network.MatchmakingClient
import com.mjm.elixir_reign.core.session.GameMode
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.ecs.components.NetworkUnitComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.world.GridOccupancyData
import com.mjm.elixir_reign.shared.world.WorldMap
import java.util.Locale
import kotlin.math.sqrt

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

class GameScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var debugFont: BitmapFont
    private lateinit var worldRenderer: WorldRenderer
    private lateinit var worldMap: WorldMap
    private lateinit var gameWorld: GameWorld
    private lateinit var selectionInputHandler: SelectionInputHandler
    private var isSelectionMode = false

    private val activeTouches = mutableMapOf<Int, Vector2>()
    private var isConstructionGridVisible = false
    private var pinchState: PinchState? = null
    private var cameraFocusAnimation: CameraFocusAnimation? = null
    private var placementDragPointer: Int? = null
    private lateinit var buildPlacementHandler: BuildPlacementHandler
    private lateinit var placementEventHandler: PlacementEventHandler
    private lateinit var eventBus: EventBus
    private lateinit var gridRenderer: IsometricGridRenderer
    private lateinit var gridOccupancy: GridOccupancyData
    private lateinit var coordinateConverter: IsometricCoordinateConverter
    private lateinit var terrainBounds: Rectangle
    private lateinit var uiStage: Stage
    private lateinit var btnSelectTroops: NineSliceImageButton
    private lateinit var placementControlsTable: Table
    private lateinit var btnConfirmPlacement: TextButton
    private lateinit var btnCancelPlacement: TextButton
    private lateinit var barracksPanel: BarracksPanel
    private var uiDebugEnabled = false
    private var mapDebugEnabled = false

    private lateinit var goldLabel: Label
    private lateinit var elixirLabel: Label
    private lateinit var darkElixirLabel: Label

    private lateinit var pauseMenuTable: Table
    private lateinit var settingsMenuTable: Table
    private lateinit var connectionErrorTable: Table
    private lateinit var connectionErrorLabel: Label
    private lateinit var gameOverTable: Table
    private lateinit var gameOverLabel: Label
    private lateinit var buildingPanelTable: Table
    private lateinit var buildingPanelTitle: Label
    private lateinit var buildingPanelLevel: Label
    private lateinit var buildingPanelAction: TextButton
    private lateinit var playerNameTable: Table
    private var playerStatusSignature: String = ""
    private var fogElapsedSeconds = 0f
    private var renderedMapRevision = -1
    private var constructionGridVisible = false
    private var localNextBuildingId = -1
    private var localProductionElapsed = 0f
    private var selectedBuildingEntity: Entity? = null
    private var pendingPlacementRequestId = 0
    private val pendingTrainingRequests = mutableMapOf<Int, PendingTrainingRequest>()
    private val entitiesByUnitId = mutableMapOf<Int, Entity>()
    private val entitiesByBuildingId = mutableMapOf<Int, Entity>()
    private val localBuildings = mutableListOf<BuildingInstanceState>()

    private fun leaveGameToMainMenu() {
        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.cancelMatchmaking()
        }
        game.navigateTo(ScreenRoute.MENU)
    }

    private fun isPauseOverlayVisible(): Boolean {
        if (!this::pauseMenuTable.isInitialized) return false
        return pauseMenuTable.isVisible ||
            settingsMenuTable.isVisible ||
            (this::connectionErrorTable.isInitialized && connectionErrorTable.isVisible) ||
            (this::gameOverTable.isInitialized && gameOverTable.isVisible)
    }

    private fun openPauseMenu() {
        pauseMenuTable.isVisible = true
        settingsMenuTable.isVisible = false
    }

    private fun openSettingsOverlay() {
        pauseMenuTable.isVisible = false
        settingsMenuTable.isVisible = true
    }

    private fun closeOverlays() {
        pauseMenuTable.isVisible = false
        settingsMenuTable.isVisible = false
    }

    private fun refreshInGameMenus(showSettings: Boolean) {
        pauseMenuTable.remove()
        settingsMenuTable.remove()

        pauseMenuTable = buildPauseMenuTable()
        settingsMenuTable = buildSettingsMenuTable()

        uiStage.addActor(pauseMenuTable)
        uiStage.addActor(settingsMenuTable)
        if (showSettings) {
            openSettingsOverlay()
        } else {
            openPauseMenu()
        }
        applyUiDebugRecursively(uiStage.root, uiDebugEnabled)
    }

    private val input = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            if (isPauseOverlayVisible()) return false

            if (handlePlacementControlsTouch(screenX, screenY)) {
                return true
            }
            cameraFocusAnimation = null

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
                    disableSelectionMode()
                    beginPinch()
                }
                return true
            }

            val worldCoords = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
            activeTouches[pointer] = Vector2(screenX.toFloat(), screenY.toFloat())

            if (activeTouches.size >= 2) {
                beginPinch()
                return true
            }

            val clickedBarracks = findBarracksAt(worldCoords.x, worldCoords.y)
            if (clickedBarracks != null) {
                barracksPanel.showFor(clickedBarracks)
                return true
            }

            // Clic gauche = tenter de sélectionner
            val hasSelectedEntity = selectionInputHandler.touchDown(screenX, screenY, camera)

            if (isSelectionMode) {
                selectionInputHandler.touchDown(screenX, screenY, camera)
                return true
            }

            if (hasSelectedEntity) {
                return true
            }

            if (!commandSelectedUnitsTo(worldCoords.x, worldCoords.y)) {
                selectionInputHandler.clearSelection()
            }

            if (activeTouches.size >= 2) {
                disableSelectionMode()
                beginPinch()
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (isPauseOverlayVisible()) return false

            if (placementDragPointer == pointer) {
                buildPlacementHandler.updateHover(camera, screenX.toFloat(), screenY.toFloat())
                return true
            }

            val previousTouch = activeTouches[pointer] ?: return false

            if (pinchState != null || activeTouches.size >= 2) {
                disableSelectionMode()
                previousTouch.set(screenX.toFloat(), screenY.toFloat())
                if (pinchState == null) {
                    beginPinch()
                }
                updatePinchZoom()
                return true
            }

            // Sur Android, la sélection rectangle ne s'active que via le bouton dédié.
            if (!buildPlacementHandler.isPlacementModeActive() && isSelectionMode) {
                selectionInputHandler.touchDragged(screenX, screenY, camera)
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
            if (isPauseOverlayVisible()) return false

            if (placementDragPointer == pointer) {
                placementDragPointer = null
                activeTouches.remove(pointer)
                if (activeTouches.size >= 2) {
                    beginPinch()
                } else {
                    endPinch()
                }
                return true
            }

            if (buildPlacementHandler.isPlacementModeActive()) {
                activeTouches.remove(pointer)
                if (activeTouches.size >= 2) {
                    beginPinch()
                } else {
                    endPinch()
                }
                return true
            }

            // Finaliser la sélection/drag selection
             selectionInputHandler.touchUp()
            isSelectionMode = false
            updateSelectionButtonState()
            updateSelectedBuildingPanel()

            activeTouches.remove(pointer)

            if (activeTouches.size >= 2) {
                disableSelectionMode()
                beginPinch()
            } else {
                endPinch()
            }
            return true
        }

        override fun mouseMoved(screenX: Int, screenY: Int): Boolean {
            if (!this@GameScreen::buildPlacementHandler.isInitialized || !buildPlacementHandler.isPlacementModeActive()) {
                return false
            }
            buildPlacementHandler.updateHover(camera, screenX.toFloat(), screenY.toFloat())
            return true
        }

        override fun scrolled(amountX: Float, amountY: Float): Boolean {
            if (isPauseOverlayVisible()) return false

            if (amountY == 0f) {
                return false
            }

            applyZoom(camera.zoom * (1f + amountY * SCROLL_ZOOM_STEP))
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if(GameConfiguration.DEBUG){
                if ((keycode == Input.Keys.B && Gdx.input.isKeyPressed(Input.Keys.F3)) ||
                    (keycode == Input.Keys.F3 && Gdx.input.isKeyPressed(Input.Keys.B))) {
                    toggleUiDebug()
                    return true
                }
                if ((keycode == Input.Keys.G && Gdx.input.isKeyPressed(Input.Keys.F3)) ||
                    (keycode == Input.Keys.F3 && Gdx.input.isKeyPressed(Input.Keys.G))) {
                    toggleMapDebug()
                    return true
                }
            }

            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                when {
                    settingsMenuTable.isVisible -> openPauseMenu()
                    pauseMenuTable.isVisible -> closeOverlays()
                    else -> openPauseMenu()
                }
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
        batch = SpriteBatch()
        debugFont = BitmapFont().apply {
            data.setScale(DEBUG_LABEL_SCALE)
            color = DEBUG_LABEL_COLOR
        }

        val worldMap = if (GameSession.mode == GameMode.MULTI) {
            GameSession.multiplayerWorldMap() ?: TerrainPresets.map()
        } else {
            TerrainPresets.map()
        }
        this.worldMap = worldMap
        worldRenderer = WorldRenderer(worldMap)
        renderedMapRevision = GameSession.mapRevision

        // Initialiser le monde du jeu (encapsule CoreGameEngine)
        gameWorld = GameWorld(batch, camera)

        // Récupérer le selectionInputHandler depuis le CoreGameEngine
        selectionInputHandler = gameWorld.coreEngine.selectionInputHandler
        terrainBounds = worldRenderer.worldBounds()

        setupPlacement(worldMap)
        spawnInitialUnits()

        configureCamera(resetView = true)

        show_UI()
    }

    override fun render(delta: Float) {
        refreshWorldRendererIfNeeded()

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        updateCameraFocusAnimation(delta)

        // IMPORTANT : la caméra bouge => il faut réassigner camera.combined à chaque frame
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        val soloPausedByMenu = GameSession.mode == GameMode.SOLO && isPauseOverlayVisible()
        fogElapsedSeconds += delta

        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.sendGameplayTick(delta)
            syncNetworkUnits()
            syncNetworkBuildings()
            consumeNetworkBuildingResults()
        } else if (!soloPausedByMenu) {
            updateSoloBuildingProduction(delta)
        }
        updateConnectionErrorOverlay()
        updateGameOverOverlay()
        refreshPlayerConnectionTable()

        batch.begin()
        worldRenderer.renderGround(batch)
        if (!soloPausedByMenu) {
            gameWorld.update(delta)
        }
        worldRenderer.renderOverlay(batch)
        if (GameSession.mode == GameMode.MULTI) {
            worldRenderer.renderFog(batch, GameSession.fogSnapshot(), fogElapsedSeconds)
        }
        batch.end()

        if (constructionGridVisible) {
            gridRenderer.render(shapeRenderer) { row, col ->
                val terrain = worldMap[row, col] ?: return@render false
                terrain.canBuildOn && !gridOccupancy.isOccupied(row, col)
            }
        }

        if (buildPlacementHandler.isPlacementModeActive()) {
            buildPlacementHandler.renderPreview(delta, batch, shapeRenderer)
        }
        updatePlacementControls()

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        if (selectionInputHandler.isDraggingNow() || uiDebugEnabled) {
            if (selectionInputHandler.isDraggingNow()) {
                val dragRect = selectionInputHandler.getDragRectangle()
                shapeRenderer.color.set(0.5f, 1f, 0.5f, 0.8f)
                shapeRenderer.rect(dragRect.x, dragRect.y, dragRect.width, dragRect.height)
            }
        }
        if (mapDebugEnabled) {
            shapeRenderer.color.set(DEBUG_CHUNK_COLOR)
            worldRenderer.renderChunkDebug(shapeRenderer)
        }
        shapeRenderer.end()

        updateResourceLabels()
        updateSelectedBuildingPanel()

        // L'UI est dessinée en dernier pour rester au-dessus du terrain.
        uiStage.act(delta)
        uiStage.draw()
        if (mapDebugEnabled) {
            batch.begin()
            debugFont.color.set(DEBUG_LABEL_COLOR)
            worldRenderer.renderChunkDebugLabels(batch, debugFont)
            batch.end()
        }
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
        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.cancelMatchmaking()
        }

        shapeRenderer.dispose()
        batch.dispose()
        debugFont.dispose()
        if (this::placementEventHandler.isInitialized) {
            placementEventHandler.dispose()
        }
        gameWorld.dispose()
        worldRenderer.dispose()
        uiStage.dispose()
    }

    private fun show_UI() {
        uiStage = Stage(ScreenViewport())

        uiStage.addActor(Shop)
        Shop.setOnShopShown { constructionGridVisible = true }
        Shop.setOnShopHidden { constructionGridVisible = false }

        val btnBuildMenu = NineSliceImageButton(UiAssets.texture(UiImage.BUTTON_9PATCH), UiAssets.texture(UiImage.ICON_HAMMER)).apply {
            onClick { _, _ ->
                Shop.show()
            }
        }

        val btnPauseMenu = NineSliceImageButton(UiAssets.texture(UiImage.BUTTON_9PATCH), UiAssets.texture(UiImage.ICON_SETTING)).apply {
            onClick { _, _ ->
                openPauseMenu()
            }
        }

        btnSelectTroops = NineSliceImageButton(
            UiAssets.texture(UiImage.BUTTON_9PATCH),
            UiAssets.texture(UiImage.ICON_SELECT),
            toggleVisuals = true
        ).apply {
            onClick { _, _ ->
                if (buildPlacementHandler.isPlacementModeActive()) {
                    return@onClick
                }
                isSelectionMode = !isSelectionMode
                updateSelectionButtonState()
            }
        }
        updateSelectionButtonState()

        btnConfirmPlacement = TextButton("Valider", UiAssets.skin).apply {
            isDisabled = true
            addListener(object : ClickListener() {
                override fun clicked(event: InputEvent?, x: Float, y: Float) {
                    if (!isDisabled && buildPlacementHandler.confirmPlacement()) {
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
            add(btnConfirmPlacement).width(180f).height(64f).padRight(8f)
            add(btnCancelPlacement).width(180f).height(64f)
            pack()
        }

        val resourceBarTable = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.12f, 0.12f, 0.14f, 0.92f))
            pad(10f, 14f, 10f, 14f)
            goldLabel = addResourceEntry(this, UiImage.ICON_GOLD)
            elixirLabel = addResourceEntry(this, UiImage.ICON_ELIXIR)
            darkElixirLabel = addResourceEntry(this, UiImage.ICON_DARK_ELIXIR)
        }

        // vertical align elements
        val hudTopTable = Table().apply {
            setFillParent(true)
            top().right()
            padTop(16f)
            padRight(16f)
            add(resourceBarTable)
            row()
            playerNameTable = playerConnectionTable()
            add(playerNameTable).right()
        }

        val hudTopLeftTable = Table().apply {
            setFillParent(true)
            top().left()
            padTop(16f)
            padLeft(16f)
            add(btnPauseMenu).size(96f).pad(24f)
        }

        val hudLeftTable = Table().apply {
            setFillParent(true)
            bottom().left()
            add(btnBuildMenu).size(96f).pad(24f)
        }

        val hudRightTable = Table().apply {
            setFillParent(true)
            bottom().right()
            add(btnSelectTroops).size(96f).pad(24f)
        }

        pauseMenuTable = buildPauseMenuTable()
        settingsMenuTable = buildSettingsMenuTable()
        connectionErrorTable = buildConnectionErrorTable()
        gameOverTable = buildGameOverTable()
        buildingPanelTable = buildBuildingPanelTable()
        barracksPanel = BarracksPanel(
            barracksProvider = {
                gameWorld.coreEngine.engine.entities
                    .filter { entity -> entity.getComponent(BarracksComponent::class.java) != null }
                    .toList()
            },
            allEntitiesProvider = { gameWorld.coreEngine.engine.entities },
            removeEntity = { entity -> gameWorld.coreEngine.engine.removeEntity(entity) },
            onBarracksFocused = { barracks -> focusCameraOnBarracks(barracks) },
            onTrainUnitRequested = { barracks, unitType -> requestUnitTraining(barracks, unitType) },
            canMutateLocally = { GameSession.mode != GameMode.MULTI }
        )

        uiStage.addActor(hudTopTable)
        uiStage.addActor(hudTopLeftTable)
        uiStage.addActor(hudLeftTable)
        uiStage.addActor(hudRightTable)
        uiStage.addActor(placementControlsTable)
        uiStage.addActor(buildingPanelTable)
        uiStage.addActor(barracksPanel)
        uiStage.addActor(pauseMenuTable)
        uiStage.addActor(settingsMenuTable)
        uiStage.addActor(connectionErrorTable)
        uiStage.addActor(gameOverTable)

        applyUiDebugRecursively(uiStage.root, uiDebugEnabled)
        Gdx.input.inputProcessor = InputMultiplexer(uiStage, input)
    }

    private fun addResourceEntry(row: Table, icon: UiImage): Label {
        val valueLabel = Label("0", UiAssets.skin)

        val valueBox = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.SHOP_CARD_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.18f, 0.18f, 0.2f, 0.95f))
            pad(6f, 12f, 6f, 12f)
            add(valueLabel).minWidth(96f).right()
        }

        row.add(Image(UiAssets.texture(icon))).size(30f).padRight(8f)
        row.add(valueBox).width(132f).padRight(14f)

        return valueLabel
    }

    private fun playerConnectionTable() : Table {
        val playerNameTable = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.12f, 0.12f, 0.14f, 0.92f))
            pad(6f, 12f, 6f, 12f)
        }
        rebuildPlayerConnectionTable(playerNameTable)
        return playerNameTable
    }

    private fun refreshPlayerConnectionTable() {
        if (!this::playerNameTable.isInitialized) return
        val signature = GameSession.playerNames
            .joinToString("|") { name -> "$name:${GameSession.getPlayerState(name).name}" }
        if (signature == playerStatusSignature) return

        playerStatusSignature = signature
        rebuildPlayerConnectionTable(playerNameTable)
    }

    private fun rebuildPlayerConnectionTable(table: Table) {
        table.clear()
        if (GameSession.playerNames.isEmpty()) return

        GameSession.playerNames.forEach { playerName ->
            val color = when (GameSession.getPlayerState(playerName)) {
                PlayerConnectionState.CONNECTED -> Color.GREEN
                PlayerConnectionState.WAITING_RECONNECTION -> Color.WHITE
                PlayerConnectionState.DISCONNECTED -> Color.GRAY
            }

            table.add(Label(playerName, UiAssets.skin).apply {
                setFontScale(0.72f)
                this.color = color
            }).padTop(6f).center()
            table.row()
        }
    }

    private fun setupPlacement(worldMap: WorldMap) {
        val geometry = worldRenderer.geometry()
        val coordinateConverter = IsometricCoordinateConverter(geometry)
        gridRenderer = IsometricGridRenderer(geometry)
        gridOccupancy = GridOccupancyData(rows = worldMap.height, cols = worldMap.width)
        eventBus = EventBus()

        val placementSystem = PlacementSystem(
            worldMap = worldMap,
            geometry = geometry,
            occupancy = gridOccupancy,
            spawnBuilding = { entityType, x, y, row, col, footprintSize ->
                val localId = localNextBuildingId--
                SpriteEntityFactory.createBuilding(
                    entityType = entityType,
                    x = x,
                    y = y,
                    engine = gameWorld.coreEngine.engine,
                    networkBuildingId = localId,
                    ownerPlayerId = localPlayerId(),
                    selectable = true,
                    gridRow = row,
                    gridCol = col,
                    footprintSizeTiles = footprintSize
                )
                localBuildings += BuildingInstanceState(
                    id = localId,
                    ownerPlayerId = localPlayerId(),
                    entityType = entityType,
                    row = row,
                    col = col,
                    level = 1
                )
            }
        )
        placementEventHandler = PlacementEventHandler(eventBus, placementSystem)

        buildPlacementHandler = BuildPlacementHandler(
            worldMap = worldMap,
            coordinateConverter = coordinateConverter,
            gridRenderer = gridRenderer,
            placementSystem = placementSystem,
            eventBus = eventBus,
            extraCanPlaceValidator = { row, col, _ -> hasOwnedTroopNear(row, col) },
            placementRequestHandler = { row, col, building ->
                requestBuildingPlacement(row, col, building)
            }
        )

        Shop.setOnBuildingSelected { selection: BuildingDefinition ->
            buildPlacementHandler.selectBuilding(selection.entityType, selection.stats, activatePlacement = true)
            pendingPlacementRequestId = 0
            centerPlacementPreviewOnScreen()
        }
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
        val x = (screenAnchor.x - placementControlsTable.width / 2f)
            .coerceIn(12f, Gdx.graphics.width - placementControlsTable.width - 12f)
        val y = (screenAnchor.y + padding)
            .coerceAtMost(Gdx.graphics.height - placementControlsTable.height - 12f)

        placementControlsTable.setPosition(x, y)
        placementControlsTable.isVisible = true
        placementControlsTable.toFront()
        btnConfirmPlacement.isDisabled = pendingPlacementRequestId != 0 || !buildPlacementHandler.canConfirmPlacement()
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

    private fun requestBuildingPlacement(row: Int, col: Int, building: PlacementSystem.BuildingToPlace): Boolean {
        if (GameSession.mode == GameMode.MULTI) {
            if (pendingPlacementRequestId != 0) {
                Gdx.app.log("GameScreen", "Placement deja en attente de validation serveur.")
                return false
            }
            if (!hasResources(building.stats.costGold, building.stats.costElixir, building.stats.costDarkElixir)) {
                Gdx.app.log("GameScreen", "Ressources insuffisantes pour placer ${building.entityType}.")
                return false
            }
            val requestId = MatchmakingClient.sendPlaceBuildingRequest(building.entityType, row, col)
            if (requestId <= 0) {
                Gdx.app.log("GameScreen", "Impossible d'envoyer la demande de placement au serveur.")
                return false
            }
            pendingPlacementRequestId = requestId
            return false
        }

        if (!GameSession.spendResources(
                goldCost = building.stats.costGold,
                elixirCost = building.stats.costElixir,
                darkElixirCost = building.stats.costDarkElixir
            )
        ) {
            Gdx.app.log("GameScreen", "Ressources insuffisantes pour placer ${building.entityType}.")
            return false
        }

        val cells = footprint(row, col, building.stats.footprintSizeTiles)
        val accepted = gridOccupancy.canOccupy(cells)
        if (accepted) {
            val world = buildingWorldPosition(row, col)
            val localId = localNextBuildingId--
            SpriteEntityFactory.createBuilding(
                entityType = building.entityType,
                x = world.x,
                y = world.y,
                engine = gameWorld.coreEngine.engine,
                networkBuildingId = localId,
                ownerPlayerId = localPlayerId(),
                selectable = true
            )
            gridOccupancy.occupy(cells)
            localBuildings += BuildingInstanceState(
                id = localId,
                ownerPlayerId = localPlayerId(),
                entityType = building.entityType,
                row = row,
                col = col,
                level = 1
            )
        }
        if (!accepted) {
            GameSession.addResources(building.stats.costGold, building.stats.costElixir, building.stats.costDarkElixir)
            Gdx.app.log("GameScreen", "Emplacement invalide pour ${building.entityType}.")
        }
        return accepted
    }

    private fun hasOwnedTroopNear(row: Int, col: Int): Boolean {
        val maxDistanceSquared = worldMap.chunkSize * worldMap.chunkSize
        val playerId = localPlayerId()
        return gameWorld.coreEngine.engine.entities.any { entity ->
            if (entity.getComponent(NetworkBuildingComponent::class.java) != null) return@any false
            val owner = entity.getComponent(OwnerComponent::class.java)
            if (GameSession.mode == GameMode.MULTI && owner?.playerId != playerId) return@any false
            val position = entity.getComponent(PositionComponent::class.java) ?: return@any false
            val (unitRow, unitCol) = worldRenderer.tileAtWorldPosition(position.x, position.y)
            val dRow = unitRow - row
            val dCol = unitCol - col
            dRow * dRow + dCol * dCol <= maxDistanceSquared
        }
    }

    private fun localPlayerId(): Int {
        return if (GameSession.mode == GameMode.MULTI) GameSession.myPlayerId else 0
    }

    private fun footprint(centerRow: Int, centerCol: Int, size: Int): List<Pair<Int, Int>> {
        val normalizedSize = size.coerceAtLeast(1)
        val startRow = centerRow - normalizedSize / 2
        val startCol = centerCol - normalizedSize / 2
        return buildList {
            for (row in startRow until startRow + normalizedSize) {
                for (col in startCol until startCol + normalizedSize) {
                    add(row to col)
                }
            }
        }
    }

    private fun spawnInitialUnits() {
        if (GameSession.mode != GameMode.MULTI) {
            val spawn = worldRenderer.tileCenterPosition(worldMap.height / 2, worldMap.width / 2)
            SpriteEntityFactory.createUnit(
                entityType = EntityType.BARBARIAN,
                x = spawn.x,
                y = spawn.y,
                engine = gameWorld.coreEngine.engine
            )
            val secondSpawn = worldRenderer.tileCenterPosition(worldMap.height / 2, worldMap.width / 2 + 1)
            SpriteEntityFactory.createUnit(
                entityType = EntityType.BARBARIAN,
                x = secondSpawn.x,
                y = secondSpawn.y,
                engine = gameWorld.coreEngine.engine
            )
            return
        }

        GameSession.unitSnapshots().forEach { unit ->
            val position = worldRenderer.tileCenterPosition(unit.row, unit.col)
            SpriteEntityFactory.createUnit(
                entityType = unit.entityType,
                x = position.x,
                y = position.y,
                engine = gameWorld.coreEngine.engine,
                networkUnitId = unit.id,
                ownerPlayerId = unit.ownerPlayerId,
                selectable = unit.ownerPlayerId == GameSession.myPlayerId,
                currentHP = unit.currentHP,
                barracksId = unit.barracksId.takeIf { it > 0 },
                teamId = unit.ownerPlayerId
            )
            gameWorld.coreEngine.engine.entities.firstOrNull {
                it.getComponent(NetworkUnitComponent::class.java)?.unitId == unit.id
            }?.let {
                entitiesByUnitId[unit.id] = it
                applyNetworkHealth(it, unit.currentHP, unit.maxHP)
            }
        }
    }

    private fun syncNetworkUnits() {
        val snapshots = GameSession.unitSnapshots()
        val visibleIds = snapshots.mapTo(mutableSetOf()) { it.id }

        entitiesByUnitId
            .filterKeys { it !in visibleIds }
            .values
            .forEach { gameWorld.coreEngine.engine.removeEntity(it) }
        entitiesByUnitId.keys.removeAll { it !in visibleIds }

        snapshots.forEach { unit ->
            val position = worldRenderer.tileCenterPosition(unit.row, unit.col)
            val existing = entitiesByUnitId[unit.id]
            if (existing == null) {
                SpriteEntityFactory.createUnit(
                    entityType = unit.entityType,
                    x = position.x,
                    y = position.y,
                    engine = gameWorld.coreEngine.engine,
                    networkUnitId = unit.id,
                    ownerPlayerId = unit.ownerPlayerId,
                    selectable = unit.ownerPlayerId == GameSession.myPlayerId,
                    currentHP = unit.currentHP,
                    barracksId = unit.barracksId.takeIf { it > 0 },
                    teamId = unit.ownerPlayerId
                )
                val created = gameWorld.coreEngine.engine.entities.firstOrNull {
                    it.getComponent(NetworkUnitComponent::class.java)?.unitId == unit.id
                }
                if (created != null) {
                    entitiesByUnitId[unit.id] = created
                    applyNetworkHealth(created, unit.currentHP, unit.maxHP)
                    applyServerUnitState(created, unit.row, unit.col, unit.targetRow, unit.targetCol, unit.moving)
                }
            } else {
                applyNetworkHealth(existing, unit.currentHP, unit.maxHP)
                applyServerUnitState(existing, unit.row, unit.col, unit.targetRow, unit.targetCol, unit.moving)
            }
        }
    }

    private fun syncNetworkBuildings() {
        val snapshots = GameSession.buildingSnapshots()
        val visibleIds = snapshots.mapTo(mutableSetOf()) { it.id }
        gridOccupancy.clear()

        entitiesByBuildingId
            .filterKeys { it !in visibleIds }
            .values
            .forEach { gameWorld.coreEngine.engine.removeEntity(it) }
        entitiesByBuildingId.keys.removeAll { it !in visibleIds }

        snapshots.forEach { building ->
            val footprint = footprint(building.row, building.col, buildingStats(building.entityType).footprintSizeTiles)
            gridOccupancy.occupy(footprint)
            val existing = entitiesByBuildingId[building.id]
            if (existing != null) {
                existing.getComponent(BuildingLevelComponent::class.java)?.level = building.level
                applyNetworkHealth(existing, building.currentHP, building.maxHP)
                applyBuildingDestroyedState(existing, building.destroyed)
                applyBarracksSnapshot(existing, building)
                return@forEach
            }
            val position = buildingWorldPosition(building.row, building.col)
            SpriteEntityFactory.createBuilding(
                entityType = building.entityType,
                x = position.x,
                y = position.y,
                engine = gameWorld.coreEngine.engine,
                networkBuildingId = building.id,
                ownerPlayerId = building.ownerPlayerId,
                level = building.level,
                selectable = building.ownerPlayerId == GameSession.myPlayerId,
                gridRow = building.row,
                gridCol = building.col,
                footprintSizeTiles = buildingStats(building.entityType).footprintSizeTiles
            )
            val created = gameWorld.coreEngine.engine.entities.firstOrNull {
                it.getComponent(NetworkBuildingComponent::class.java)?.buildingId == building.id
            }
            if (created != null) {
                entitiesByBuildingId[building.id] = created
                applyNetworkHealth(created, building.currentHP, building.maxHP)
                applyBuildingDestroyedState(created, building.destroyed)
                applyBarracksSnapshot(created, building)
            }
        }
    }

    private fun applyBarracksSnapshot(entity: Entity, building: BuildingInstanceState) {
        val barracks = entity.getComponent(BarracksComponent::class.java) ?: return
        barracks.maxFormedUnits = building.maxFormedUnits
        barracks.trainingQueue.clear()
        barracks.trainingQueue.addAll(building.trainingQueue)
        barracks.readyToSpawn.clear()
        barracks.activeTraining = if (building.hasActiveTraining) {
            BarracksTrainingProgress(
                unitType = building.activeTrainingUnitType,
                elapsedSeconds = building.activeTrainingElapsedSeconds
            )
        } else {
            null
        }
    }

    private fun applyNetworkHealth(entity: Entity, currentHP: Float, maxHP: Float) {
        val health = entity.getComponent(HealthComponent::class.java) ?: return
        health.maxHP = maxHP
        health.currentHP = currentHP.coerceIn(0f, maxHP)
    }

    private fun applyBuildingDestroyedState(entity: Entity, destroyed: Boolean) {
        val state = entity.getComponent(BuildingStateComponent::class.java) ?: return
        if (destroyed) {
            state.state = BuildingState.DESTROYED
        }
    }

    private fun consumeNetworkBuildingResults() {
        MatchmakingClient.consumePlacementResult()?.let { result ->
            if (pendingPlacementRequestId != 0 && (result.requestId == pendingPlacementRequestId || result.requestId > 0)) {
                pendingPlacementRequestId = 0
            }
            if (result.accepted) {
                buildPlacementHandler.cancelPlacement()
                Shop.hide()
            } else {
                Gdx.app.log("GameScreen", "Placement refuse: ${result.reason}")
            }
        }
        MatchmakingClient.consumeUpgradeResult()?.let { result ->
            if (!result.accepted) {
                Gdx.app.log("GameScreen", "Amelioration refusee: ${result.reason}")
            }
        }
        MatchmakingClient.consumeTrainUnitResult()?.let { result ->
            pendingTrainingRequests.remove(result.requestId)
            if (!result.accepted) {
                Gdx.app.log("GameScreen", "Formation refusee: ${result.reason}")
            }
        }
    }

    private fun requestUnitTraining(barracksEntity: Entity, unitType: EntityType): Boolean {
        if (GameSession.mode != GameMode.MULTI) return false
        val buildingId = barracksEntity.getComponent(NetworkBuildingComponent::class.java)?.buildingId ?: return false
        val stats = SpriteEntityFactory.getUnitStats(unitType)
        if (!hasResources(stats.costGold, stats.costElixir, stats.costDarkElixir)) {
            Gdx.app.log("GameScreen", "Ressources insuffisantes pour former $unitType.")
            return false
        }
        val requestId = MatchmakingClient.sendTrainUnitRequest(buildingId, unitType)
        if (requestId <= 0) {
            return false
        }
        pendingTrainingRequests[requestId] = PendingTrainingRequest(buildingId, unitType)
        return true
    }

    private fun hasResources(goldCost: Int, elixirCost: Int, darkElixirCost: Int): Boolean {
        return GameSession.gold >= goldCost &&
            GameSession.elixir >= elixirCost &&
            GameSession.darkElixir >= darkElixirCost
    }

    private fun updateSoloBuildingProduction(delta: Float) {
        localProductionElapsed += delta
        if (localProductionElapsed < SOLO_PRODUCTION_INTERVAL_SECONDS) return
        val elapsed = localProductionElapsed
        localProductionElapsed = 0f

        localBuildings.forEach { building ->
            val stats = buildingStats(building.entityType)
            val produced = (stats.productionRate * building.level * elapsed).toInt()
            if (produced <= 0) return@forEach
            when (building.entityType) {
                EntityType.GOLD_MINE -> GameSession.addResources(goldAmount = produced)
                EntityType.ELEXIR_PUMP -> GameSession.addResources(elixirAmount = produced)
                EntityType.DARCKELEXIR_PUMP -> GameSession.addResources(darkElixirAmount = produced)
                else -> Unit
            }
        }
    }

    private fun buildingStats(entityType: EntityType): BuildingStats {
        return when (entityType) {
            EntityType.BARRACKS -> BuildingStats.BARRACKS
            EntityType.ELEXIR_PUMP -> BuildingStats.ELEXIR_PUMP
            EntityType.DARCKELEXIR_PUMP -> BuildingStats.DARCKELEXIR_PUMP
            EntityType.GOLD_MINE -> BuildingStats.GOLD_MINE
            EntityType.ARCHER_TOWER -> BuildingStats.ARCHER_TOWER
            EntityType.TOWN_HALL -> BuildingStats.TOWN_HALL
            else -> BuildingStats.BARRACKS
        }
    }

    private fun buildingWorldPosition(row: Int, col: Int): Vector2 {
        val geometry = worldRenderer.geometry()
        val center = geometry.gridToWorld(row, col + 1)
        return Vector2(center.x, center.y - geometry.halfTileHeight)
    }

    private fun applyPredictedMove(unitIds: IntArray, targetRow: Int, targetCol: Int) {
        unitIds.forEach { unitId ->
            val entity = entitiesByUnitId[unitId] ?: return@forEach
            val position = entity.getComponent(PositionComponent::class.java) ?: return@forEach
            val sourceTile = worldRenderer.tileAtWorldPosition(position.x, position.y)
            applyDestinationForTiles(
                entity = entity,
                sourceRow = sourceTile.first.toFloat(),
                sourceCol = sourceTile.second.toFloat(),
                targetRow = targetRow.toFloat(),
                targetCol = targetCol.toFloat(),
                moving = true
            )
        }
    }

    private fun applyServerUnitState(
        entity: Entity,
        row: Float,
        col: Float,
        targetRow: Float,
        targetCol: Float,
        moving: Boolean
    ) {
        val serverPosition = worldRenderer.tileCenterPosition(row, col)
        val position = entity.getComponent(PositionComponent::class.java)
        if (position != null) {
            val dx = serverPosition.x - position.x
            val dy = serverPosition.y - position.y
            if (dx * dx + dy * dy > SERVER_SNAP_DISTANCE_SQUARED || !moving) {
                position.x = serverPosition.x
                position.y = serverPosition.y
            }
        }

        applyDestinationForTiles(entity, row, col, targetRow, targetCol, moving)
    }

    private fun applyDestinationForTiles(
        entity: Entity,
        sourceRow: Float,
        sourceCol: Float,
        targetRow: Float,
        targetCol: Float,
        moving: Boolean
    ) {
        val destination = entity.getComponent(DestinationComponent::class.java) ?: return
        if (!moving) {
            destination.isActive = false
            entity.getComponent(MovementComponent::class.java)?.isMoving = false
            return
        }

        val target = worldRenderer.tileCenterPosition(targetRow, targetCol)
        destination.targetX = target.x
        destination.targetY = target.y
        destination.isActive = true

        entity.getComponent(MovementComponent::class.java)?.speed = computePredictedWorldSpeed(
            sourceRow = sourceRow,
            sourceCol = sourceCol,
            targetRow = targetRow,
            targetCol = targetCol
        )
    }

    private fun computePredictedWorldSpeed(
        sourceRow: Float,
        sourceCol: Float,
        targetRow: Float,
        targetCol: Float
    ): Float {
        val tileDistance = sqrt((targetRow - sourceRow) * (targetRow - sourceRow) + (targetCol - sourceCol) * (targetCol - sourceCol))
        if (tileDistance <= 0.0001f) return 0f

        val source = worldRenderer.tileCenterPosition(sourceRow, sourceCol)
        val target = worldRenderer.tileCenterPosition(targetRow, targetCol)
        val worldDistance = source.dst(target)
        return worldDistance / tileDistance * SERVER_UNIT_SPEED_TILES_PER_SECOND
    }

    private fun refreshWorldRendererIfNeeded() {
        if (GameSession.mode != GameMode.MULTI || renderedMapRevision == GameSession.mapRevision) {
            return
        }

        val worldMap = GameSession.multiplayerWorldMap() ?: return
        worldRenderer.dispose()
        worldRenderer = WorldRenderer(worldMap)
        this.worldMap = worldMap
        terrainBounds = worldRenderer.worldBounds()
        setupPlacement(worldMap)
        renderedMapRevision = GameSession.mapRevision
    }

    private fun updateResourceLabels() {
        if (!this::goldLabel.isInitialized) return

        goldLabel.setText(formatResource(GameSession.gold))
        elixirLabel.setText(formatResource(GameSession.elixir))
        darkElixirLabel.setText(formatResource(GameSession.darkElixir))
    }

    private fun updateConnectionErrorOverlay() {
        val error = MatchmakingClient.getErrorText()
        if (!this::connectionErrorTable.isInitialized || error.isNullOrBlank()) {
            return
        }

        connectionErrorLabel.setText(error)
        pauseMenuTable.isVisible = false
        settingsMenuTable.isVisible = false
        connectionErrorTable.isVisible = true
    }

    private fun updateGameOverOverlay() {
        if (!this::gameOverTable.isInitialized || !GameSession.gameOver) {
            return
        }

        val winnerName = GameSession.getPlayerName(GameSession.winnerPlayerId)
        gameOverLabel.setText(
            if (GameSession.winnerPlayerId == GameSession.myPlayerId) {
                "Victoire - gagnant: $winnerName"
            } else {
                "Defaite - gagnant: $winnerName"
            }
        )
        pauseMenuTable.isVisible = false
        settingsMenuTable.isVisible = false
        connectionErrorTable.isVisible = false
        gameOverTable.isVisible = true
        gameOverTable.toFront()
    }

    private fun formatResource(value: Int): String {
        return String.format(Locale.US, "%,d", value).replace(',', ' ')
    }

    private fun updateSelectionButtonState() {
        if (!this::btnSelectTroops.isInitialized) {
            return
        }
        btnSelectTroops.setHighlighted(isSelectionMode)
    }

    private fun disableSelectionMode() {
        if (!isSelectionMode) {
            return
        }
        isSelectionMode = false
        selectionInputHandler.touchUp()
        updateSelectionButtonState()
    }

    private fun toggleUiDebug() {
        uiDebugEnabled = !uiDebugEnabled
        applyUiDebugRecursively(uiStage.root, uiDebugEnabled)
        Gdx.app.log("GameScreen", "UI debug: ${if (uiDebugEnabled) "ON" else "OFF"}")
    }

    private fun toggleMapDebug() {
        mapDebugEnabled = !mapDebugEnabled
        Gdx.app.log("GameScreen", "Map debug: ${if (mapDebugEnabled) "ON" else "OFF"}")
    }

    private fun applyUiDebugRecursively(actor: Actor, enabled: Boolean) {
        actor.setDebug(enabled)
        if (actor is Group) {
            for (i in 0 until actor.children.size) {
                applyUiDebugRecursively(actor.children[i], enabled)
            }
        }
    }

    private fun barracksEntities(): List<Entity> {
        return gameWorld.coreEngine.engine.entities
            .filter {
                it.getComponent(BarracksComponent::class.java) != null &&
                    canOpenBarracks(it)
            }
            .sortedBy { it.getComponent(BarracksComponent::class.java).barracksId }
    }

    private fun findBarracksAt(worldX: Float, worldY: Float): Entity? {
        return barracksEntities().firstOrNull { barracks ->
            BoundingBoxUtils.pointInEntity(barracks, worldX, worldY)
        }
    }

    private fun canOpenBarracks(entity: Entity): Boolean {
        if (GameSession.mode != GameMode.MULTI) return true
        val owner = entity.getComponent(OwnerComponent::class.java) ?: return false
        return owner.playerId == GameSession.myPlayerId
    }

    private fun focusCameraOnBarracks(barracks: Entity) {
        val position = barracks.getComponent(PositionComponent::class.java) ?: return
        startCameraFocusAnimation(position.x, position.y)
    }

    private fun startCameraFocusAnimation(targetX: Float, targetY: Float) {
        val startX = camera.position.x
        val startY = camera.position.y

        camera.position.set(targetX, targetY, 0f)
        clampCameraPosition()
        val clampedTargetX = camera.position.x
        val clampedTargetY = camera.position.y

        camera.position.set(startX, startY, 0f)
        camera.update()

        cameraFocusAnimation = CameraFocusAnimation(
            startX = startX,
            startY = startY,
            targetX = clampedTargetX,
            targetY = clampedTargetY
        )
    }

    private fun updateCameraFocusAnimation(delta: Float) {
        val animation = cameraFocusAnimation ?: return
        animation.elapsed += delta

        val progress = (animation.elapsed / CAMERA_FOCUS_DURATION_SECONDS).coerceIn(0f, 1f)
        val easedProgress = progress * progress * (3f - 2f * progress)

        camera.position.set(
            animation.startX + (animation.targetX - animation.startX) * easedProgress,
            animation.startY + (animation.targetY - animation.startY) * easedProgress,
            0f
        )
        clampCameraPosition()
        camera.update()

        if (progress >= 1f) {
            cameraFocusAnimation = null
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

    private fun buildPauseMenuTable(): Table {
        val btnBack = TextButton(Localization.get("global.back"), UiAssets.skin)
        val btnSettings = TextButton(Localization.get("menu.settings"), UiAssets.skin)
        val btnQuit = TextButton(Localization.get("menu.quit"), UiAssets.skin)

        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                closeOverlays()
            }
        })
        btnSettings.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                openSettingsOverlay()
            }
        })
        btnQuit.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                leaveGameToMainMenu()
            }
        })

        val panel = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.08f, 0.08f, 0.1f, 0.96f))
            pad(16f)
            add(btnBack).width(280f).height(72f).padBottom(10f).row()
            add(btnSettings).width(280f).height(72f).padBottom(10f).row()
            add(btnQuit).width(280f).height(72f)
        }

        return Table().apply {
            setFillParent(true)
            background = TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.BUTTON_9PATCH))).tint(Color(0f, 0f, 0f, 0.58f))
            touchable = Touchable.enabled
            isVisible = false
            add(panel).center()
        }
    }

    private fun buildSettingsMenuTable(): Table {
        val title = Label(Localization.get("settings.title"), UiAssets.skin).apply { setFontScale(1.4f) }
        val langLabel = Label(Localization.get("settings.language"), UiAssets.skin)

        val selectBox = SelectBox<String>(UiAssets.skin)
        val languageItems = Array<String>()
        for (lang in Localization.availableLanguages) {
            languageItems.add(lang.displayName)
        }
        selectBox.items = languageItems
        val currentLangIndex = Localization.indexOfCurrentLanguage()
        if (currentLangIndex >= 0) {
            selectBox.selectedIndex = currentLangIndex
        }
        selectBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val languageCode = Localization.languageCodeAt(selectBox.selectedIndex) ?: return
                if (languageCode == Localization.getCurrentLanguage()) return

                Localization.setLanguage(languageCode)
                Gdx.app.postRunnable {
                    refreshInGameMenus(showSettings = true)
                }
            }
        })

        val btnBack = TextButton(Localization.get("global.back"), UiAssets.skin)
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                openPauseMenu()
            }
        })

        val panel = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.08f, 0.08f, 0.1f, 0.96f))
            pad(18f)
            add(title).padBottom(12f).row()
            add(langLabel).left().padBottom(8f).row()
            add(selectBox).width(320f).height(68f).padBottom(14f).row()
            add(btnBack).width(260f).height(70f)
        }

        return Table().apply {
            setFillParent(true)
            background = TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.BUTTON_9PATCH))).tint(Color(0f, 0f, 0f, 0.58f))
            touchable = Touchable.enabled
            isVisible = false
            add(panel).center()
        }
    }

    private fun buildConnectionErrorTable(): Table {
        connectionErrorLabel = Label("", UiAssets.skin).apply {
            setWrap(true)
        }

        val btnReconnect = TextButton(Localization.get("network.action.reconnect"), UiAssets.skin)
        btnReconnect.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                if (!MatchmakingClient.reconnectToLastInstance()) {
                    game.navigateTo(ScreenRoute.MENU)
                    return
                }
                game.navigateTo(ScreenRoute.LOBBY_WAITING)
            }
        })

        val btnBack = TextButton(Localization.get("global.back"), UiAssets.skin)
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                MatchmakingClient.cancelMatchmaking()
                game.navigateTo(ScreenRoute.MENU)
            }
        })

        val panel = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.08f, 0.08f, 0.1f, 0.98f))
            pad(18f)
            add(connectionErrorLabel).width(420f).padBottom(16f).row()
            add(btnReconnect).width(280f).height(72f).padBottom(10f).row()
            add(btnBack).width(280f).height(72f)
        }

        return Table().apply {
            setFillParent(true)
            background = TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.BUTTON_9PATCH))).tint(Color(0f, 0f, 0f, 0.68f))
            touchable = Touchable.enabled
            isVisible = false
            add(panel).center()
        }
    }

    private fun buildGameOverTable(): Table {
        gameOverLabel = Label("", UiAssets.skin).apply {
            setWrap(true)
            setFontScale(1f)
        }

        val btnBack = TextButton(Localization.get("global.back"), UiAssets.skin)
        btnBack.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                leaveGameToMainMenu()
            }
        })

        val panel = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.08f, 0.08f, 0.1f, 0.98f))
            pad(18f)
            add(gameOverLabel).width(420f).padBottom(16f).row()
            add(btnBack).width(280f).height(72f)
        }

        return Table().apply {
            setFillParent(true)
            background = TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.BUTTON_9PATCH))).tint(Color(0f, 0f, 0f, 0.68f))
            touchable = Touchable.enabled
            isVisible = false
            add(panel).center()
        }
    }

    private fun buildBuildingPanelTable(): Table {
        buildingPanelTitle = Label("", UiAssets.skin).apply { setFontScale(0.9f) }
        buildingPanelLevel = Label("", UiAssets.skin).apply { setFontScale(0.75f) }
        buildingPanelAction = TextButton("Ameliorer", UiAssets.skin)
        buildingPanelAction.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val entity = selectedBuildingEntity ?: return
                val buildingId = entity.getComponent(NetworkBuildingComponent::class.java)?.buildingId ?: return
                if (GameSession.mode == GameMode.MULTI) {
                    MatchmakingClient.sendUpgradeBuildingRequest(buildingId)
                    return
                }

                val type = entity.getComponent(EntityTypeComponent::class.java)?.entityType ?: return
                val building = localBuildings.firstOrNull { it.id == buildingId } ?: return
                val stats = buildingStats(type)
                val multiplier = building.level + 1
                if (GameSession.spendResources(
                        goldCost = stats.costGold * multiplier,
                        elixirCost = stats.costElixir * multiplier,
                        darkElixirCost = stats.costDarkElixir * multiplier
                    )
                ) {
                    building.level += 1
                    entitiesByBuildingId[buildingId]?.getComponent(BuildingLevelComponent::class.java)?.level = building.level
                }
            }
        })

        val panel = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.08f, 0.08f, 0.1f, 0.96f))
            pad(12f)
            add(buildingPanelTitle).width(BUILDING_PANEL_WIDTH - 24f).left().row()
            add(buildingPanelLevel).width(BUILDING_PANEL_WIDTH - 24f).left().padTop(4f).row()
            add(buildingPanelAction).width(BUILDING_PANEL_WIDTH - 48f).height(52f).padTop(10f)
        }

        return Table().apply {
            isVisible = false
            add(panel)
            setSize(BUILDING_PANEL_WIDTH, BUILDING_PANEL_HEIGHT)
        }
    }

    private fun updateSelectedBuildingPanel() {
        if (!this::buildingPanelTable.isInitialized) return
        val selected = selectionInputHandler.selectedEntitiesSnapshot().firstOrNull { entity ->
            entity.getComponent(NetworkBuildingComponent::class.java) != null &&
                entity.getComponent(EntityTypeComponent::class.java) != null &&
                canInteractWithSelectedBuilding(entity)
        }

        selectedBuildingEntity = selected
        if (selected == null) {
            buildingPanelTable.isVisible = false
            return
        }

        val type = selected.getComponent(EntityTypeComponent::class.java).entityType
        val buildingId = selected.getComponent(NetworkBuildingComponent::class.java).buildingId
        val level = if (GameSession.mode == GameMode.MULTI) {
            GameSession.buildingSnapshots().firstOrNull { it.id == buildingId }?.level ?: 1
        } else {
            localBuildings.firstOrNull { it.id == buildingId }?.level ?: 1
        }
        val stats = buildingStats(type)
        val multiplier = level + 1
        buildingPanelTitle.setText(stats.name)
        val atMaxLevel = level >= stats.maxLevel
        buildingPanelLevel.setText(
            if (atMaxLevel) {
                "Niveau $level / ${stats.maxLevel} - niveau max"
            } else {
                "Niveau $level / ${stats.maxLevel} - cout: ${formatUpgradeCost(stats, multiplier)}"
            }
        )
        buildingPanelAction.isDisabled = atMaxLevel
        buildingPanelTable.isVisible = true
        positionBuildingPanelAbove(selected)
    }

    private fun positionBuildingPanelAbove(entity: Entity) {
        val position = entity.getComponent(PositionComponent::class.java) ?: return
        val bounds = BoundingBoxUtils.getBoundingBox(entity)
        buildingPanelTable.setSize(BUILDING_PANEL_WIDTH, BUILDING_PANEL_HEIGHT)
        val anchorX = bounds?.let { it.x + it.width / 2f } ?: position.x
        val anchorY = bounds?.let { it.y + it.height } ?: position.y
        val screen = camera.project(Vector3(anchorX, anchorY, 0f))
        val padding = 10f
        val x = (screen.x - BUILDING_PANEL_WIDTH / 2f)
            .coerceIn(12f, Gdx.graphics.width - buildingPanelTable.width - 12f)
        val y = (screen.y + padding)
            .coerceIn(12f, Gdx.graphics.height - buildingPanelTable.height - 12f)
        buildingPanelTable.setPosition(x, y)
        buildingPanelTable.toFront()
    }

    private fun commandSelectedUnitsTo(worldX: Float, worldY: Float): Boolean {
        val selectedNetworkUnitIds = selectionInputHandler.selectedNetworkUnitIds()
        if (GameSession.mode == GameMode.MULTI) {
            if (selectedNetworkUnitIds.isEmpty()) return false
            val (targetRow, targetCol) = worldRenderer.tileAtWorldPosition(worldX, worldY)
            MatchmakingClient.sendMoveUnitsRequest(selectedNetworkUnitIds, targetRow, targetCol)
            selectionInputHandler.clearSelection()
            return true
        }

        if (selectionInputHandler.selectedEntitiesSnapshot().isEmpty()) return false
        selectionInputHandler.moveSelectedEntitiesToTarget(worldX, worldY)
        selectionInputHandler.clearSelection()
        return true
    }

    private fun formatUpgradeCost(stats: BuildingStats, multiplier: Int): String {
        val costs = mutableListOf<String>()
        val gold = stats.costGold * multiplier
        val elixir = stats.costElixir * multiplier
        val darkElixir = stats.costDarkElixir * multiplier
        if (gold > 0) costs += "$gold or"
        if (elixir > 0) costs += "$elixir elixir"
        if (darkElixir > 0) costs += "$darkElixir elixir noir"
        return if (costs.isEmpty()) "aucun cout" else costs.joinToString(", ")
    }

    private fun canInteractWithSelectedBuilding(entity: Entity): Boolean {
        if (GameSession.mode != GameMode.MULTI) return true
        val owner = entity.getComponent(OwnerComponent::class.java) ?: return false
        return owner.playerId == GameSession.myPlayerId
    }

    companion object {
        private const val MAX_ZOOM_IN = 0.6f
        private const val SCROLL_ZOOM_STEP = 0.1f
        private const val MIN_ZOOM_PADDING_X = 96f
        private const val MIN_ZOOM_PADDING_Y = 96f
        private const val DRAG_PADDING_X = 48f
        private const val DRAG_PADDING_Y = 96f
        private const val DEBUG_LABEL_SCALE = 1.2f
        private const val SERVER_UNIT_SPEED_TILES_PER_SECOND = 4f
        private const val SERVER_SNAP_DISTANCE_SQUARED = 96f * 96f
        private const val SOLO_PRODUCTION_INTERVAL_SECONDS = 1f
        private const val BUILDING_PANEL_WIDTH = 292f
        private const val BUILDING_PANEL_HEIGHT = 154f
        private val DEBUG_CHUNK_COLOR = Color(0.16f, 0.85f, 1f, 0.95f)
        private val DEBUG_LABEL_COLOR = Color(1f, 1f, 1f, 1f)
        private const val CAMERA_FOCUS_DURATION_SECONDS = 0.35f
    }

    private data class PinchState(
        val initialDistance: Float,
        val initialZoom: Float
    )

    private data class CameraFocusAnimation(
        val startX: Float,
        val startY: Float,
        val targetX: Float,
        val targetY: Float,
        var elapsed: Float = 0f
    )

    private data class PendingTrainingRequest(
        val buildingId: Int,
        val unitType: EntityType
    )
}
