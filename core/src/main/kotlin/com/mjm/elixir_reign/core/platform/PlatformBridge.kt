package com.mjm.elixir_reign.core.platform

import com.badlogic.gdx.Screen
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.navigation.ScreenRoute

/**
 * Interface implémentée par chaque plateforme (Android, Desktop…).
 * Permet à Core d'appeler des comportements spécifiques à la plateforme
 * sans connaître les classes d'écran concrètes.
 */
interface PlatformBridge {

    /**
     * Construit l'écran concret correspondant à la route demandée par le core.
     */
    fun createScreen(route: ScreenRoute, game: Main): Screen

    /**
     * Action à exécuter quand le joueur veut "revenir en arrière"
     * (bouton Back Android, touche Escape sur Desktop, etc.).
     *
     * @param game référence vers [Main] pour permettre les changements d'écran.
     */
    fun onBackPressed(game: Main)
}

