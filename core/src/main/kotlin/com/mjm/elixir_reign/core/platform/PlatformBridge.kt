package com.mjm.elixir_reign.core.platform

import com.mjm.elixir_reign.core.Main

/**
 * Interface implémentée par chaque plateforme (Android, Desktop…).
 * Permet à Core d'appeler des comportements spécifiques à la plateforme
 * sans en dépendre directement.
 */
interface PlatformBridge {

    /**
     * Action à exécuter quand le joueur veut "revenir en arrière"
     * (bouton Back Android, touche Escape sur Desktop, etc.).
     *
     * @param game référence vers [Main] pour permettre les changements d'écran.
     */
    fun onBackPressed(game: Main)
}


