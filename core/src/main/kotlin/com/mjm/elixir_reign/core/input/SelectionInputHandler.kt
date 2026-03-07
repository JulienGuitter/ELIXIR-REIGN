package com.mjm.elixir_reign.core.input

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.gdx.graphics.Camera
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent

/**
 * SelectionInputHandler : Gère la sélection des entités via les inputs
 *
 * Gestionnaire d'input (pas un ECS System)
 * Traite les événements discrets (clic/toucher), pas les entités en continu
 *
 * Responsabilités:
 * - Convertir les coordonnées écran → world
 * - Tester la collision du clic avec les entités
 * - Mettre à jour les flags isSelected des SelectableComponent
 * - Gérer la déselection de l'ancienne entité
 */
class SelectionInputHandler(private val engine: Engine) {
    private var selectedEntity: Entity? = null

    /**
     * Traite un clic/toucher à des coordonnées écran
     * Déselectionne l'ancienne entité et en sélectionne une nouvelle si trouvée
     *
     * @param screenX Coordonnée X en pixels écran (0 = gauche)
     * @param screenY Coordonnée Y en pixels écran (0 = haut)
     * @param camera Camera de la scène pour convertir écran → world
     */
    fun selectEntityAtScreenCoords(screenX: Int, screenY: Int, camera: Camera) {
        // Convertir les coordonnées écran → world
        val worldCoords = camera.unproject(Vector3(screenX.toFloat(), screenY.toFloat(), 0f))

        // Déselectionner TOUTES les entités
        deselectAll()

        // Chercher une entité à cette position
        var clickedEntity: Entity? = null
        for (entity in engine.entities) {
            if (isEntityAtPosition(entity, worldCoords.x, worldCoords.y)) {
                clickedEntity = entity
                break
            }
        }

        // Sélectionner la nouvelle entité si trouvée
        if (clickedEntity != null) {
            clickedEntity.getComponent(SelectableComponent::class.java)?.let { selectable ->
                selectable.isSelected = true
                selectedEntity = clickedEntity
            }
        }
    }

    /**
     * Vérifie si une entité est à une position donnée
     * Teste la bounding box du sprite
     */
    private fun isEntityAtPosition(entity: Entity, worldX: Float, worldY: Float): Boolean {
        entity.getComponent(SelectableComponent::class.java) ?: return false
        val position = entity.getComponent(PositionComponent::class.java) ?: return false
        val sprite = entity.getComponent(SpriteComponent::class.java) ?: return false

        // Créer la bounding box du sprite (centré)
        val spriteX = position.x - (sprite.width * sprite.scaleX) / 2f
        val spriteY = position.y - (sprite.height * sprite.scaleY) / 2f
        val boundingBox = Rectangle(
            spriteX,
            spriteY,
            sprite.width * sprite.scaleX,
            sprite.height * sprite.scaleY
        )

        return boundingBox.contains(worldX, worldY)
    }

    /**
     * Retourne l'entité actuellement sélectionnée
     */
    fun getSelectedEntity(): Entity? = selectedEntity

    /**
     * Déselectionne l'entité actuelle
     */
    fun deselectAll() {
        selectedEntity?.let { entity ->
            entity.getComponent(SelectableComponent::class.java)?.let { selectable ->
                selectable.isSelected = false
            }
        }
        selectedEntity = null
    }

    /**
     * Vérifie si une entité est sélectionnée
     */
    fun isEntitySelected(entity: Entity): Boolean {
        return entity === selectedEntity
    }
}

