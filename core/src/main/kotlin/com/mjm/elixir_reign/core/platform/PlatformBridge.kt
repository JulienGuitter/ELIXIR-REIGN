package com.mjm.elixir_reign.core.platform

import com.mjm.elixir_reign.core.Main

data class GameInputConfig(
    val minCameraZoom: Float = 0.25f,
    val maxCameraZoom: Float = 2f,
    val touchZoomEnabled: Boolean = true,
    val catchesBackKey: Boolean = false
)

/**
 * Interface implémentée par chaque plateforme (Android, Desktop…).
 * Permet à Core d'appeler des comportements spécifiques à la plateforme
 * sans en dépendre directement.
 */
interface PlatformBridge {
    val gameInputConfig: GameInputConfig

    /**
     * Indique si la touche reçue doit être traitée comme une action "retour".
     */
    fun isBackKey(keycode: Int): Boolean

    /**
     * Indique si un événement de scroll doit être traité comme un zoom caméra.
     */
    fun shouldZoomCameraFromScroll(amountX: Float, amountY: Float, isCtrlPressed: Boolean): Boolean

    /**
     * Action à exécuter quand le joueur veut "revenir en arrière"
     * (bouton Back Android, touche Escape sur Desktop, etc.).
     *
     * @param game référence vers [Main] pour permettre les changements d'écran.
     */
    fun onBackPressed(game: Main)
}

