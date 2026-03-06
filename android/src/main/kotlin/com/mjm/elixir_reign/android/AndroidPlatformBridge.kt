package com.mjm.elixir_reign.android

import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.MenuScreen

/**
 * Implémentation Android de [PlatformBridge].
 */
class AndroidPlatformBridge : PlatformBridge {

    /**
     * Sur Android, "revenir en arrière" depuis le GameScreen renvoie vers le MenuScreen.
     */
    override fun onBackPressed(game: Main) {
        game.changeScreen(MenuScreen(game))
    }
}


