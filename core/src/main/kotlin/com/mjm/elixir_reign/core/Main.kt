package com.mjm.elixir_reign.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.assets.AssetManager
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.MenuScreen

class Main(val platform: PlatformBridge) : Game() {

    lateinit var batch: SpriteBatch
    lateinit var assets: AssetManager

    override fun create() {
        batch = SpriteBatch()
        assets = AssetManager()

        changeScreen(MenuScreen(this))
    }

    fun changeScreen(newScreen: Screen) {
        // Dispose l'écran actuel avant de changer vers le nouvel écran
        screen?.dispose()
        batch.dispose()
        assets.dispose()

        setScreen(newScreen)
    }
}
