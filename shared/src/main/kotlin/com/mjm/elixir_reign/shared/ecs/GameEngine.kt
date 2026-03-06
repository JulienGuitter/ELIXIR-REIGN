package com.mjm.elixir_reign.shared.ecs

import com.badlogic.ashley.core.Engine
import com.mjm.elixir_reign.shared.ecs.systems.*

/**
 * GameEngine partagé entre client et serveur
 * Contient seulement la logique métier pure (pas de rendu)
 *
 * Les systems spécifiques au rendu (AnimationSystem, RenderSystem, HealthSystem côté client)
 * sont ajoutés dans le core via CoreGameEngine
 */
open class GameEngine {
    val engine = Engine()

    init {
        // Ajouter uniquement les systems partagés (logique métier pure)
        engine.addSystem(MovementSystem())
        engine.addSystem(AttackSystem())
    }

    fun update(deltaTime: Float) {
        engine.update(deltaTime)
    }

    fun clear() {
        engine.removeAllEntities()
    }
}
