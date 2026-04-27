package com.mjm.elixir_reign.lwjgl3.screens

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
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.Array
import com.badlogic.gdx.utils.viewport.ScreenViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.world.GameWorld
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.core.terrain.TerrainPresets
import com.mjm.elixir_reign.core.ui.NineSliceImageButton
import com.mjm.elixir_reign.lwjgl3.ui.Shop
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.shared.GameConfiguration
import com.mjm.elixir_reign.core.world.WorldRenderer
import com.mjm.elixir_reign.shared.logic.UnitType
import com.mjm.elixir_reign.core.network.MatchmakingClient
import com.mjm.elixir_reign.core.session.GameMode
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.ecs.components.MovementComponent
import com.mjm.elixir_reign.shared.ecs.components.NetworkUnitComponent
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
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
    private lateinit var gameWorld: GameWorld
    private lateinit var selectionInputHandler: SelectionInputHandler
    private lateinit var terrainBounds: Rectangle
    private lateinit var uiStage: Stage
    private var uiDebugEnabled = false
    private var mapDebugEnabled = false

    private val activeTouches = mutableMapOf<Int, Vector2>()
    private var pinchState: PinchState? = null

    private lateinit var goldLabel: Label
    private lateinit var elixirLabel: Label
    private lateinit var darkElixirLabel: Label

    private lateinit var pauseMenuTable: Table
    private lateinit var settingsMenuTable: Table
    private lateinit var connectionErrorTable: Table
    private lateinit var connectionErrorLabel: Label
    private var fogElapsedSeconds = 0f
    private var renderedMapRevision = -1
    private val entitiesByUnitId = mutableMapOf<Int, Entity>()

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
            (this::connectionErrorTable.isInitialized && connectionErrorTable.isVisible)
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

            val worldCoords = camera.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
            if (GameSession.mode == GameMode.MULTI) {
                val selectedUnitIds = selectionInputHandler.selectedNetworkUnitIds()
                val (targetRow, targetCol) = worldRenderer.tileAtWorldPosition(worldCoords.x, worldCoords.y)
                MatchmakingClient.sendMoveUnitsRequest(
                    unitIds = selectedUnitIds,
                    targetRow = targetRow,
                    targetCol = targetCol
                )
            }
            selectionInputHandler.moveSelectedEntitiesToTarget(worldCoords.x, worldCoords.y)
            if (GameSession.mode == GameMode.MULTI) {
                val selectedUnitIds = selectionInputHandler.selectedNetworkUnitIds()
                val (targetRow, targetCol) = worldRenderer.tileAtWorldPosition(worldCoords.x, worldCoords.y)
                applyPredictedMove(selectedUnitIds, targetRow, targetCol)
            }
//             Clic gauche = sélectionner
            selectionInputHandler.touchDown(screenX, screenY, camera)

            activeTouches[pointer] = Vector2(screenX.toFloat(), screenY.toFloat())

            if (activeTouches.size >= 2) {
                beginPinch()
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            if (isPauseOverlayVisible()) return false

            // Si mode double-clic actif, faire le drag selection
             if (selectionInputHandler.isDoubleClickModeActive()) {
                 selectionInputHandler.touchDragged(screenX, screenY, camera)
                 return true
             }

            val previousTouch = activeTouches[pointer] ?: return false

            if (pinchState != null) {
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
            if (isPauseOverlayVisible()) return false

            // Finaliser la sélection/drag selection
             selectionInputHandler.touchUp()

            activeTouches.remove(pointer)

            if (activeTouches.size >= 2) {
                beginPinch()
            } else {
                endPinch()
            }
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
        worldRenderer = WorldRenderer(worldMap)
        renderedMapRevision = GameSession.mapRevision

        // Initialiser le monde du jeu (encapsule CoreGameEngine)
        gameWorld = GameWorld(batch, camera)

        // Récupérer le selectionInputHandler depuis le CoreGameEngine
        selectionInputHandler = gameWorld.coreEngine.selectionInputHandler
        terrainBounds = worldRenderer.worldBounds()

        // Créer une entité barbare au centre de la scène
        spawnInitialUnits()

        configureCamera(resetView = true)

        show_UI()
    }

    override fun render(delta: Float) {
        refreshWorldRendererIfNeeded()

        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // IMPORTANT : la caméra bouge => il faut réassigner camera.combined à chaque frame
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        val soloPausedByMenu = GameSession.mode == GameMode.SOLO && isPauseOverlayVisible()
        fogElapsedSeconds += delta

        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.sendGameplayTick(delta)
            syncNetworkUnits()
        }
        updateConnectionErrorOverlay()

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
        gameWorld.dispose()
        worldRenderer.dispose()
        uiStage.dispose()
    }

    private fun show_UI() {
        uiStage = Stage(ScreenViewport())

        uiStage.addActor(Shop)

        val btnBuildMenu = NineSliceImageButton(UiAssets.texture(UiImage.BUTTON_9PATCH), UiAssets.texture(UiImage.ICON_HAMMER)).apply {
            onClick { _, _ ->
                Shop.show()
            }
        }

        val resourceBarTable = Table().apply {
            background = NinePatchDrawable(
                NinePatch(UiAssets.texture(UiImage.BUTTON_9PATCH), 20, 20, 20, 20)
            ).tint(Color(0.12f, 0.12f, 0.14f, 0.92f))
            pad(10f, 14f, 10f, 14f)
            goldLabel = addResourceEntry(this, UiImage.ICON_GOLD)
            elixirLabel = addResourceEntry(this, UiImage.ICON_ELIXIR)
            darkElixirLabel = addResourceEntry(this, UiImage.ICON_DARK_ELIXIR)
            addPlayerNamesRow(this)
        }

        val hudTopTable = Table().apply {
            setFillParent(true)
            top().right()
            padTop(16f)
            padRight(16f)
            add(resourceBarTable)
        }

        val hudTable = Table().apply {
            setFillParent(true)
            bottom().left()
            add(btnBuildMenu).size(96f).pad(24f)
        }

        pauseMenuTable = buildPauseMenuTable()
        settingsMenuTable = buildSettingsMenuTable()
        connectionErrorTable = buildConnectionErrorTable()

        uiStage.addActor(hudTopTable)
        uiStage.addActor(hudTable)
        uiStage.addActor(pauseMenuTable)
        uiStage.addActor(settingsMenuTable)
        uiStage.addActor(connectionErrorTable)

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

    private fun addPlayerNamesRow(row: Table) {
        if (GameSession.playerNames.isEmpty()) return

        row.row()
        row.add(Label(GameSession.playerNames.joinToString("  |  "), UiAssets.skin).apply {
            setFontScale(0.72f)
            color = Color(1f, 1f, 1f, 0.82f)
        }).colspan(6).padTop(6f).center()
    }

    private fun spawnInitialUnits() {
        if (GameSession.mode != GameMode.MULTI) {
            SpriteEntityFactory.createUnit(
                unitType = UnitType.BARBARIAN,
                x = 0f,
                y = 0f,
                engine = gameWorld.coreEngine.engine
            )
            return
        }

        GameSession.unitSnapshots().forEach { unit ->
            val position = worldRenderer.tileCenterPosition(unit.row, unit.col)
            SpriteEntityFactory.createUnit(
                unitType = unit.unitType,
                x = position.x,
                y = position.y,
                engine = gameWorld.coreEngine.engine,
                networkUnitId = unit.id,
                ownerPlayerId = unit.ownerPlayerId,
                selectable = unit.ownerPlayerId == GameSession.myPlayerId
            )
            gameWorld.coreEngine.engine.entities.firstOrNull {
                it.getComponent(NetworkUnitComponent::class.java)?.unitId == unit.id
            }?.let { entitiesByUnitId[unit.id] = it }
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
                    unitType = unit.unitType,
                    x = position.x,
                    y = position.y,
                    engine = gameWorld.coreEngine.engine,
                    networkUnitId = unit.id,
                    ownerPlayerId = unit.ownerPlayerId,
                    selectable = unit.ownerPlayerId == GameSession.myPlayerId
                )
                val created = gameWorld.coreEngine.engine.entities.firstOrNull {
                    it.getComponent(NetworkUnitComponent::class.java)?.unitId == unit.id
                }
                if (created != null) {
                    entitiesByUnitId[unit.id] = created
                    applyServerUnitState(created, unit.row, unit.col, unit.targetRow, unit.targetCol, unit.moving)
                }
            } else {
                applyServerUnitState(existing, unit.row, unit.col, unit.targetRow, unit.targetCol, unit.moving)
            }
        }
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
        terrainBounds = worldRenderer.worldBounds()
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

    private fun formatResource(value: Int): String {
        return String.format(Locale.US, "%,d", value).replace(',', ' ')
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
        private val DEBUG_CHUNK_COLOR = Color(0.16f, 0.85f, 1f, 0.95f)
        private val DEBUG_LABEL_COLOR = Color(1f, 1f, 1f, 1f)
    }

    private data class PinchState(
        val initialDistance: Float,
        val initialZoom: Float
    )
}
