package com.mjm.elixir_reign.lwjgl3

import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.MenuScreen

/**
 * Implémentation Desktop (lwjgl3) de [PlatformBridge].
 * La touche Escape est déjà capturée dans [com.mjm.elixir_reign.core.screens.GameScreen]
 * via Input.Keys.ESCAPE — ce bridge se contente de gérer la navigation.
 */
class Lwjgl3PlatformBridge : PlatformBridge {

    override fun onBackPressed(game: Main) {
        game.changeScreen(MenuScreen(game))
    }
}

