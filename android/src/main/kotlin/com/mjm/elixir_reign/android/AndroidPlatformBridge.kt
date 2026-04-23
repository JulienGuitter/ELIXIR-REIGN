package com.mjm.elixir_reign.android

import com.badlogic.gdx.Screen
import com.mjm.elixir_reign.android.screens.GameScreen
import com.mjm.elixir_reign.android.screens.LoadingScreen
import com.mjm.elixir_reign.android.screens.LobbyWaitingMenu
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.android.screens.MenuScreen
import com.mjm.elixir_reign.android.screens.ModeSelectionScreen
import com.mjm.elixir_reign.android.screens.SettingsScreen

/**
 * Implémentation Android de [PlatformBridge].
 */
class AndroidPlatformBridge : PlatformBridge {

    override fun createScreen(route: ScreenRoute, game: Main): Screen =
        when (route) {
            ScreenRoute.LOADING -> LoadingScreen(game)
            ScreenRoute.MENU -> MenuScreen(game)
            ScreenRoute.MODE_SELECTION -> ModeSelectionScreen(game)
            ScreenRoute.SETTINGS -> SettingsScreen(game)
            ScreenRoute.LOBBY_WAITING -> LobbyWaitingMenu(game)
            ScreenRoute.GAME -> GameScreen(game)
        }

    /**
     * Sur Android, "revenir en arrière" depuis le GameScreen renvoie vers le MenuScreen.
     */
    override fun onBackPressed(game: Main) {
        game.navigateTo(ScreenRoute.MENU)
    }
}

