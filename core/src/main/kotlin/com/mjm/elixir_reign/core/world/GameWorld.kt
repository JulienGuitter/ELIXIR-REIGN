package com.mjm.elixir_reign.core.world

import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.mjm.elixir_reign.shared.world.GameWorld as SharedGameWorld
import com.mjm.elixir_reign.core.ecs.CoreGameEngine

/**
 * GameWorld (Core) : Étend GameWorld (shared) avec le rendu et l'animation
 *
 * Responsabilités :
 * - Étendre le GameWorld partagé
 * - Créer CoreGameEngine qui ajoute le rendu à l'engine partagé
 * - Ajouter l'animation et les effets visuels
 * - Gérer le cycle de vie du rendu
 *
 * Cette classe est SPÉCIFIQUE au client
 */
class GameWorld(
    private val batch: SpriteBatch,
    private val camera: OrthographicCamera
) : SharedGameWorld() {

    // CoreGameEngine utilise l'engine du parent (SharedGameWorld)
    // et ajoute les systems de rendu par-dessus
    val coreEngine: CoreGameEngine = CoreGameEngine(
        batch = batch,
        camera = camera,
        engine = engine  // Utilise l'engine partagé
    )

    /**
     * Mise à jour : utilise le CoreGameEngine qui ajoute les systems de rendu
     */
    override fun update(delta: Float) {
        coreEngine.update(delta)
    }

    /**
     * Nettoyage : dispose le CoreGameEngine
     */
    override fun dispose() {
        coreEngine.dispose()
        super.dispose()
    }
}

