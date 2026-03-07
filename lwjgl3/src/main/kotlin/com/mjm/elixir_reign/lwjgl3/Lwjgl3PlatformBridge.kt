package com.mjm.elixir_reign.lwjgl3

import com.badlogic.gdx.Input
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.platform.GameInputConfig
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.MenuScreen

/**
 * Implémentation Desktop (lwjgl3) de [PlatformBridge].
 */
class Lwjgl3PlatformBridge : PlatformBridge {
    override val gameInputConfig = GameInputConfig(
        touchZoomEnabled = false,
        catchesBackKey = false
    )

    override fun isBackKey(keycode: Int): Boolean {
        return keycode == Input.Keys.ESCAPE || keycode == Input.Keys.BACK
    }

    override fun shouldZoomCameraFromScroll(amountX: Float, amountY: Float, isCtrlPressed: Boolean): Boolean {
        return isCtrlPressed
    }

    override fun onBackPressed(game: Main) {
        game.changeScreen(MenuScreen(game))
    }
}
