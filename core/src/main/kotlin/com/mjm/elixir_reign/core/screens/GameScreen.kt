package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.world.GameWorld
import com.mjm.elixir_reign.core.ecs.factories.SpriteEntityFactory
import com.mjm.elixir_reign.core.handler.SelectionInputHandler
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.logic.UnitType

/**
 * Écran de jeu principal.
 * La navigation "retour" est déléguée à [Main.platform] afin de rester
 * indépendante de la plateforme (Android : bouton Back / BACK key ;
 * Desktop : touche Escape gérée dans le launcher Desktop).
 */

class GameScreen(private val game: Main) : ScreenAdapter() {

    private val tileSize = 256f
    private lateinit var shapeRenderer: ShapeRenderer
    private lateinit var camera: OrthographicCamera
    private lateinit var batch: SpriteBatch
    private lateinit var gameWorld: GameWorld
    private lateinit var selectionInputHandler: SelectionInputHandler

    private val lastTouch = Vector2()
    private val cubePosition = Vector2(0f, 0f)

    private val input = object : InputAdapter() {

        override fun touchDown(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            lastTouch.set(screenX.toFloat(), screenY.toFloat())

            val worldCoords = camera.unproject(com.badlogic.gdx.math.Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

            selectionInputHandler.moveSelectedEntitiesToTarget(worldCoords.x, worldCoords.y)

            // Clic gauche = sélectionner
            selectionInputHandler.touchDown(screenX, screenY, camera)
            return true
        }

        override fun touchDragged(screenX: Int, screenY: Int, pointer: Int): Boolean {
            val deltaX = screenX - lastTouch.x
            val deltaY = screenY - lastTouch.y

            // Si mode double-clic actif, faire le drag selection
            if (selectionInputHandler.isDoubleClickModeActive()) {
                selectionInputHandler.touchDragged(screenX, screenY, camera)
            } else {
                // Sinon, bouger la caméra normalement
                camera.translate(-deltaX, deltaY)
                camera.update()
            }

            lastTouch.set(screenX.toFloat(), screenY.toFloat())
            return true
        }

        override fun touchUp(screenX: Int, screenY: Int, pointer: Int, button: Int): Boolean {
            // Finaliser la sélection/drag selection
            selectionInputHandler.touchUp()
            return true
        }

        override fun keyDown(keycode: Int): Boolean {
            if (keycode == Input.Keys.BACK || keycode == Input.Keys.ESCAPE) {
                game.platform.onBackPressed(game)
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

        // Initialiser le monde du jeu (encapsule CoreGameEngine)
        gameWorld = GameWorld(batch, camera)

        // Récupérer le selectionInputHandler depuis le CoreGameEngine
        selectionInputHandler = gameWorld.coreEngine.selectionInputHandler

        // Créer une entité barbare au centre de la scène
        SpriteEntityFactory.createUnit(
            unitType = UnitType.BARBARIAN,
            x = 0f,
            y = 0f,
            engine = gameWorld.coreEngine.engine
        )
        SpriteEntityFactory.createUnit(
            unitType = UnitType.ARCHER,
            x = 150f,
            y = 150f,
            engine = gameWorld.coreEngine.engine
        )

        SpriteEntityFactory.createUnit(
            unitType = UnitType.GIANT,
            x = -150f,
            y = 150f,
            engine = gameWorld.coreEngine.engine
        )

        Gdx.input.inputProcessor = input
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        // IMPORTANT : la caméra bouge => il faut réassigner camera.combined à chaque frame
        shapeRenderer.projectionMatrix = camera.combined
        batch.projectionMatrix = camera.combined

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        drawIsoCube(cubePosition.x, cubePosition.y)
        shapeRenderer.end()

        // Dessiner le rectangle de drag selection si actif
        if (selectionInputHandler.isDraggingNow()) {
            val dragRect = selectionInputHandler.getDragRectangle()
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
            shapeRenderer.color.set(0.5f, 1f, 0.5f, 0.8f)  // Vert semi-transparent
            shapeRenderer.rect(dragRect.x, dragRect.y, dragRect.width, dragRect.height)
            shapeRenderer.end()
        }

        // DEBUG : Afficher les bounding boxes de sélection pour toutes les entités
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        for (entity in gameWorld.coreEngine.engine.entities) {
            val boundingBox = selectionInputHandler.getEntityBoundingBox(entity)
            if (boundingBox != null) {
                // Rouge pour les entités non sélectionnées, bleu pour les sélectionnées
                if (selectionInputHandler.isEntitySelected(entity)) {
                    shapeRenderer.color.set(0f, 0.5f, 1f, 0.6f)  // Bleu
                } else {
                    shapeRenderer.color.set(1f, 0f, 0f, 0.3f)  // Rouge transparent
                }

                shapeRenderer.rect(boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height)
            }
        }
        shapeRenderer.end()

        // DEBUG : Afficher les cercles de sélection pour toutes les entités
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color.set(1f, 1f, 1f, 0.8f)
        for (entity in gameWorld.coreEngine.engine.entities) {
            val position = entity.getComponent(PositionComponent::class.java)
            if (position != null) {
                // Dessiner un cercle blanc rempli autour de chaque entité
                shapeRenderer.circle(position.x, position.y, 5f)
            }
        }
        shapeRenderer.end()

        // Mise à jour + rendu des entités ECS (SpriteBatch géré par RenderSystem)
        batch.begin()
        gameWorld.update(delta)
        batch.end()
    }

    override fun resize(width: Int, height: Int) {
        // Quand la taille de l'écran change, on doit réajuster la caméra pour que les coordonnées restent cohérentes
        val oldX = camera.position.x
        val oldY = camera.position.y
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.position.set(oldX, oldY, 0f)
        camera.update()
    }

    override fun dispose() {
        shapeRenderer.dispose()
        batch.dispose()
        gameWorld.dispose()
    }

    private fun drawIsoCube(x: Float, y: Float) {
        val cubeHeight = tileSize / 2f
        val halfW = tileSize / 2f
        val halfH = tileSize / 4f

        // TOP
        shapeRenderer.color = Color.LIGHT_GRAY
        shapeRenderer.triangle(x, y + halfH, x - halfW, y, x + halfW, y)
        shapeRenderer.triangle(x, y - halfH, x - halfW, y, x + halfW, y)

        // LEFT
        shapeRenderer.color = Color.GRAY
        shapeRenderer.triangle(x - halfW, y, x - halfW, y - cubeHeight, x, y - halfH)
        shapeRenderer.triangle(x - halfW, y - cubeHeight, x, y - cubeHeight - halfH, x, y - halfH)

        // RIGHT
        shapeRenderer.color = Color.DARK_GRAY
        shapeRenderer.triangle(x + halfW, y, x + halfW, y - cubeHeight, x, y - halfH)
        shapeRenderer.triangle(x + halfW, y - cubeHeight, x, y - cubeHeight - halfH, x, y - halfH)
    }
}
