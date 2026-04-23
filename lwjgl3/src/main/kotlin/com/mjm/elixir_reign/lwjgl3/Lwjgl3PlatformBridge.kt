package com.mjm.elixir_reign.lwjgl3

import com.badlogic.gdx.Screen
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.lwjgl3.screens.GameScreen
import com.mjm.elixir_reign.lwjgl3.screens.LoadingScreen
import com.mjm.elixir_reign.lwjgl3.screens.LobbyWaitingMenu
import com.mjm.elixir_reign.lwjgl3.screens.MenuScreen
import com.mjm.elixir_reign.lwjgl3.screens.ModeSelectionScreen
import com.mjm.elixir_reign.lwjgl3.screens.SettingsScreen

/**
 * Implémentation Desktop (lwjgl3) de [PlatformBridge].
 * La touche Escape est capturée dans le GameScreen de la plateforme
 * via Input.Keys.ESCAPE — ce bridge se contente de gérer la navigation.
 */
class Lwjgl3PlatformBridge : PlatformBridge {

    override fun createScreen(route: ScreenRoute, game: Main): Screen =
        when (route) {
            ScreenRoute.LOADING -> LoadingScreen(game)
            ScreenRoute.MENU -> MenuScreen(game)
            ScreenRoute.MODE_SELECTION -> ModeSelectionScreen(game)
            ScreenRoute.SETTINGS -> SettingsScreen(game)
            ScreenRoute.LOBBY_WAITING -> LobbyWaitingMenu(game)
            ScreenRoute.GAME -> GameScreen(game)
        }

    override fun onBackPressed(game: Main) {
        game.navigateTo(ScreenRoute.MENU)
    }
}
