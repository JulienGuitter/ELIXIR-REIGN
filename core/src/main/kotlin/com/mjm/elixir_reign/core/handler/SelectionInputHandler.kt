package com.mjm.elixir_reign.core.handler

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent

/**
 * SelectionInputHandler : Gère la sélection des entités au clic/double-clic
 *
 * Clic simple = sélectionner 1 entité
 * Double-clic + drag = sélectionner plusieurs entités dans un rectangle
 */
class SelectionInputHandler(private val engine: Engine) {
    private var selectedEntities = mutableSetOf<Entity>()

    private var lastClickTime = 0L
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var dragEndX = 0f
    private var dragEndY = 0f
    private var isDoubleClickActive = false
    private var isDragging = false

    // ============ ÉVÉNEMENTS D'INPUT ============

    fun touchDown(screenX: Int, screenY: Int, camera: Camera) {
        val worldCoords = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

        // Initialiser le drag
        dragStartX = worldCoords.x
        dragStartY = worldCoords.y
        dragEndX = worldCoords.x
        dragEndY = worldCoords.y
        isDragging = false

        // Détecter double-clic (300ms)
        val now = System.currentTimeMillis()
        isDoubleClickActive = (now - lastClickTime) < 300L
        lastClickTime = now

        // Si simple clic : sélectionner l'entité cliquée
        if (!isDoubleClickActive) {
            val clickedEntity = findEntityAt(worldCoords.x, worldCoords.y)
            selectedEntities.clear()
            if (clickedEntity != null) selectedEntities.add(clickedEntity)
            updateSelection()
        }
    }

    fun touchDragged(screenX: Int, screenY: Int, camera: Camera) {
        if (!isDoubleClickActive) return

        val worldCoords = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))
        dragEndX = worldCoords.x
        dragEndY = worldCoords.y

        val distance = kotlin.math.sqrt(
            (dragEndX - dragStartX) * (dragEndX - dragStartX) +
            (dragEndY - dragStartY) * (dragEndY - dragStartY)
        )
        isDragging = distance > 10f
    }

    fun touchUp() {
        // Si double-clic + drag : sélectionner toutes les entités dans le rectangle
        if (isDoubleClickActive && isDragging) {
            val dragRect = getDragRectangle()
            selectedEntities.clear()
            for (entity in engine.entities) {
                if (entityTouchesRectangle(entity, dragRect)) {
                    selectedEntities.add(entity)
                }
            }
        }
        isDragging = false
        isDoubleClickActive = false
        updateSelection()
    }

    // ============ CALCULS DE BOUNDING BOX ============

    /**
     * Récupère la bounding box d'une entité (centré sur sa position)
     * Utilisé PARTOUT pour la cohérence
     */
    private fun getBoundingBox(entity: Entity): Rectangle? {
        val pos = entity.getComponent(PositionComponent::class.java) ?: return null
        val sprite = entity.getComponent(SpriteComponent::class.java) ?: return null

        val fullW = sprite.width * sprite.scaleX
        val fullH = sprite.height * sprite.scaleY

        // Le personnage visible occupe ~50% de la largeur (centré) et ~60% du bas de la cellule
        val boxW = fullW * 0.4f
        val boxH = fullH * 0.5f

        return Rectangle(
            pos.x + (fullW - boxW) / 3f,   // centré horizontalement
            pos.y + (fullH - boxH) / 2.2f,                           // aligné sur le bas de la cellule
            boxW,
            boxH
        )
    }

    private fun findEntityAt(x: Float, y: Float): Entity? {
        for (entity in engine.entities) {
            if (entity.getComponent(SelectableComponent::class.java) != null) {
                val box = getBoundingBox(entity)
                if (box?.contains(x, y) == true) {
                    return entity
                }
            }
        }
        return null
    }

    private fun entityTouchesRectangle(entity: Entity, rect: Rectangle): Boolean {
        if (entity.getComponent(SelectableComponent::class.java) == null) return false
        val box = getBoundingBox(entity) ?: return false
        return rect.overlaps(box)
    }

    fun getDragRectangle(): Rectangle {
        val x1 = kotlin.math.min(dragStartX, dragEndX)
        val y1 = kotlin.math.min(dragStartY, dragEndY)
        val x2 = kotlin.math.max(dragStartX, dragEndX)
        val y2 = kotlin.math.max(dragStartY, dragEndY)
        return Rectangle(x1, y1, x2 - x1, y2 - y1)
    }

    // ============ GESTION SÉLECTION ============

    private fun updateSelection() {
        for (entity in engine.entities) {
            entity.getComponent(SelectableComponent::class.java)?.isSelected =
                entity in selectedEntities
        }
    }

    // ============ ACCESSEURS ============

    fun isDraggingNow(): Boolean = isDragging
    fun isDoubleClickModeActive(): Boolean = isDoubleClickActive
    fun getEntityBoundingBox(entity: Entity): Rectangle? = getBoundingBox(entity)
    fun isEntitySelected(entity: Entity): Boolean = entity in selectedEntities
}

