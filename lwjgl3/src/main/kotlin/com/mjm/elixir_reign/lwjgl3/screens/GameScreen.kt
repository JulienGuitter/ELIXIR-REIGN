package com.mjm.elixir_reign.lwjgl3.screens

import com.badlogic.gdx.Application
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Group
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
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

    private fun onBackRequested() {
        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.cancelMatchmaking()
        }
        game.platform.onBackPressed(game)
    }

    private val input = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
             val worldCoords = camera.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
             selectionInputHandler.moveSelectedEntitiesToTarget(worldCoords.x, worldCoords.y)
//             Clic gauche = sélectionner
             selectionInputHandler.touchDown(screenX, screenY, camera)

            activeTouches[pointer] = Vector2(screenX.toFloat(), screenY.toFloat())

            if (activeTouches.size >= 2) {
                beginPinch()
            }
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
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
                onBackRequested()
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

        worldRenderer = WorldRenderer(TerrainPresets.map())

        // Initialiser le monde du jeu (encapsule CoreGameEngine)
        gameWorld = GameWorld(batch, camera)

        // Récupérer le selectionInputHandler depuis le CoreGameEngine
        selectionInputHandler = gameWorld.coreEngine.selectionInputHandler
        terrainBounds = worldRenderer.worldBounds()

        // Créer une entité barbare au centre de la scène
        SpriteEntityFactory.createUnit(
            unitType = UnitType.BARBARIAN,
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

        if (GameSession.mode == GameMode.MULTI) {
            MatchmakingClient.sendGameplayTick(delta)
        }

        // Mise à jour + rendu des entités ECS (SpriteBatch géré par RenderSystem)
        batch.begin()
        worldRenderer.renderGround(batch)
        gameWorld.update(delta)
        worldRenderer.renderOverlay(batch)
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

        val hudTable = Table().apply {
            setFillParent(true)
            bottom().left()
            add(btnBuildMenu).size(96f).pad(24f)
        }

        uiStage.addActor(hudTable)
        applyUiDebugRecursively(uiStage.root, uiDebugEnabled)
        Gdx.input.inputProcessor = InputMultiplexer(uiStage, input)
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

    companion object {
        private const val MAX_ZOOM_IN = 0.6f
        private const val SCROLL_ZOOM_STEP = 0.1f
        private const val MIN_ZOOM_PADDING_X = 96f
        private const val MIN_ZOOM_PADDING_Y = 96f
        private const val DRAG_PADDING_X = 48f
        private const val DRAG_PADDING_Y = 96f
        private const val DEBUG_LABEL_SCALE = 1.2f
        private val DEBUG_CHUNK_COLOR = Color(0.16f, 0.85f, 1f, 0.95f)
        private val DEBUG_LABEL_COLOR = Color(1f, 1f, 1f, 1f)
    }

    private data class PinchState(
        val initialDistance: Float,
        val initialZoom: Float
    )
}
