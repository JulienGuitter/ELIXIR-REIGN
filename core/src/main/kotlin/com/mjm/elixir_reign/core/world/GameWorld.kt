package com.mjm.elixir_reign.core.world

import com.mjm.elixir_reign.shared.ecs.GameEngine
import com.mjm.elixir_reign.shared.factory.EntityFactory

class GameWorld {
    val gameEngine = GameEngine()

    init {
        // Créer quelques entités de test
        EntityFactory.createBarbarian(100f, 200f, gameEngine.engine)
    }

    // Mettre à jour le monde à chaque frame
    fun update(delta: Float) {
        gameEngine.update(delta)
    }

    // Nettoyer le monde
    fun dispose() {
        gameEngine.clear()
    }
}
