package com.mjm.elixir_reign.android

import com.badlogic.gdx.Input
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.platform.GameInputConfig
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.MenuScreen

/**
 * Implémentation Android de [PlatformBridge].
 */
class AndroidPlatformBridge : PlatformBridge {
    override val gameInputConfig = GameInputConfig(
        touchZoomEnabled = true,
        catchesBackKey = true
    )

    override fun isBackKey(keycode: Int): Boolean {
        return keycode == Input.Keys.BACK
    }

    override fun shouldZoomCameraFromScroll(amountX: Float, amountY: Float, isCtrlPressed: Boolean): Boolean {
        return false
    }

    /**
     * Sur Android, "revenir en arrière" depuis le GameScreen renvoie vers le MenuScreen.
     */
    override fun onBackPressed(game: Main) {
        game.changeScreen(MenuScreen(game))
    }
}

