package com.mjm.elixir_reign.core.handler

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mjm.elixir_reign.core.session.GameMode
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.shared.ecs.components.DestinationComponent
import com.mjm.elixir_reign.shared.ecs.components.NetworkUnitComponent
import com.mjm.elixir_reign.shared.ecs.components.OwnerComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.core.tools.BoundingBoxUtils

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

    fun touchDown(screenX: Int, screenY: Int, camera: Camera): Boolean {
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

            if (clickedEntity != null) {
                // On a cliqué sur une unité. On désélectionne les autres (ou pas, selon logique)
                // mais la sélection change.
                selectedEntities.clear()
                selectedEntities.add(clickedEntity)
                updateSelection()
                return true
            } else {
                // Si on a rien cliqué, on pourrait ordonner un mouvement, mais il faut
                // le faire AVANT de clear la sélection.
                // On délègue l'ordre de mouvement à l'appelant s'il le souhaite,
                // mais l'appelant ne sait pas que selectedEntities = null !
                // Faisons la logique : si on a cliqué dans le vide, on bouge d'abord, puis on désélectionne.
                if (selectedEntities.isNotEmpty()) {
                    moveSelectedEntitiesToTarget(worldCoords.x, worldCoords.y)
                    selectedEntities.clear()
                    updateSelection()
                    return false
                }
            }

            selectedEntities.clear()
            updateSelection()
            return false
        }
        // Si c'est un double-clic, on va potentiellement entamer un drag pour sélectionner.
        // On retourne true pour éviter d'interagir avec les bâtiments en dessous.
        return true
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
                if (canInteractWith(entity) && entityTouchesRectangle(entity, dragRect)) {
                    selectedEntities.add(entity)
                }
            }
        }
        isDragging = false
        isDoubleClickActive = false
        updateSelection()
    }

    // ============ CALCULS DE BOUNDING BOX ============
    // ⚠️ Tous les calculs sont centralisés dans BoundingBoxUtils
    // Ne pas dupliquer le code de positionnement ici!

    private fun findEntityAt(x: Float, y: Float): Entity? {
        for (entity in engine.entities) {
            if (entity.getComponent(SelectableComponent::class.java) != null) {
                if (!canInteractWith(entity)) {
                    continue
                }
                if (BoundingBoxUtils.pointInEntity(entity, x, y)) {
                    return entity
                }
            }
        }
        return null
    }

    private fun entityTouchesRectangle(entity: Entity, rect: Rectangle): Boolean {
        if (entity.getComponent(SelectableComponent::class.java) == null) return false
        return BoundingBoxUtils.entityTouchesRectangle(entity, rect)
    }

    private fun canInteractWith(entity: Entity): Boolean {
        if (GameSession.mode != GameMode.MULTI) return true

        val owner = entity.getComponent(OwnerComponent::class.java) ?: return false
        return owner.playerId == GameSession.myPlayerId
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
    fun getEntityBoundingBox(entity: Entity): Rectangle? = BoundingBoxUtils.getBoundingBox(entity)
    fun isEntitySelected(entity: Entity): Boolean = entity in selectedEntities
    fun selectedEntitiesSnapshot(): List<Entity> = selectedEntities.toList()
    fun selectedNetworkUnitIds(): IntArray {
        return selectedEntities
            .mapNotNull { it.getComponent(NetworkUnitComponent::class.java)?.unitId }
            .filter { it > 0 }
            .toIntArray()
    }

    /**
     * Commande aux entités sélectionnées de se déplacer vers une destination
     * @param targetX Position X mondiale de la destination
     * @param targetY Position Y mondiale de la destination
     */
    fun moveSelectedEntitiesToTarget(targetX: Float, targetY: Float) {
        println("[SelectionInput] Commande de déplacement à (${targetX.toInt()},${targetY.toInt()}) pour ${selectedEntities.size} entité(s)")
        for (entity in selectedEntities) {
            val destination = entity.getComponent(DestinationComponent::class.java)
            if (destination != null) {
                destination.targetX = targetX
                destination.targetY = targetY
                destination.isActive = true
                println("[SelectionInput] ✓ Entité mise à jour: destination.isActive = true")
            }
        }
    }
}
