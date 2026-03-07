package com.mjm.elixir_reign.shared.world

import com.badlogic.ashley.core.Engine
import com.mjm.elixir_reign.shared.ecs.GameEngine

/**
 * GameWorld (Shared) : Gère la logique métier du monde
 *
 * Responsabilités :
 * - Encapsuler GameEngine
 * - Gérer le cycle de vie (update, dispose)
 * - Logique métier pure (pas de rendu)
 *
 * Cette classe est PARTAGÉE entre client et serveur
 */
open class GameWorld {
    protected val gameEngine = GameEngine()

    val engine: Engine
        get() = gameEngine.engine

    /**
     * Mettre à jour le monde à chaque frame
     */
    open fun update(delta: Float) {
        gameEngine.update(delta)
    }

    /**
     * Nettoyer les ressources du monde
     */
    open fun dispose() {
        gameEngine.clear()
    }
}


