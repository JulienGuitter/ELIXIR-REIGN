package com.mjm.elixir_reign.core.ecs.systems

import com.badlogic.ashley.core.Engine
import com.badlogic.ashley.core.Entity
import com.badlogic.ashley.core.EntitySystem
import com.badlogic.ashley.core.Family
import com.badlogic.gdx.Gdx
import com.mjm.elixir_reign.shared.ecs.components.PositionComponent
import com.mjm.elixir_reign.core.ecs.components.SpriteComponent
import com.mjm.elixir_reign.shared.ecs.components.SelectableComponent

/**
 * SelectionSystem : Gère la logique de sélection des entités
 * - Détecte les clics/taps
 * - Détermine quelle entité est sélectionnée
 * - Met à jour l'état de sélection
 */
class SelectionSystem : EntitySystem() {
    private var selectedEntity: Entity? = null
    private val selectableFamily = Family.all(SelectableComponent::class.java).get()

    override fun update(deltaTime: Float) {
        // Détection du clic/tap
        if (Gdx.input.justTouched()) {
            val mouseX = Gdx.input.x.toFloat()
            val mouseY = Gdx.input.y.toFloat()

            trySelectEntity(mouseX, mouseY)
        }
    }

    /**
     * Essaie de sélectionner une entité aux coordonnées données
     */
    private fun trySelectEntity(screenX: Float, screenY: Float) {
        val entities = engine.getEntitiesFor(selectableFamily)
        var newSelection: Entity? = null

        // Parcourir les entités du haut vers le bas pour déterminer la sélection
        for (entity in entities.reversed()) {
            if (isPointInEntity(entity, screenX, screenY)) {
                newSelection = entity
                break
            }
        }

        // Désélectionner l'ancienne entité
        selectedEntity?.getComponent(SelectableComponent::class.java)?.isSelected = false

        // Sélectionner la nouvelle entité
        selectedEntity = newSelection
        newSelection?.getComponent(SelectableComponent::class.java)?.isSelected = true
    }

    /**
     * Vérifie si un point (écran) se trouve dans les limites d'une entité
     */
    private fun isPointInEntity(entity: Entity, screenX: Float, screenY: Float): Boolean {
        val position = entity.getComponent(PositionComponent::class.java) ?: return false
        val sprite = entity.getComponent(SpriteComponent::class.java) ?: return false

        val x1 = position.x
        val y1 = position.y
        val x2 = x1 + sprite.width * sprite.scaleX
        val y2 = y1 + sprite.height * sprite.scaleY

        return screenX >= x1 && screenX <= x2 && screenY >= y1 && screenY <= y2
    }

    /**
     * Retourne l'entité actuellement sélectionnée
     */
    fun getSelectedEntity(): Entity? = selectedEntity
}

