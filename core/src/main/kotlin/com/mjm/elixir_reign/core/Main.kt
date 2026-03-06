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

    fun changeScreen(screen: Screen) {
        val previous = this.screen
        setScreen(screen)      // appelle hide() sur l'ancien, show() sur le nouveau
        previous?.dispose()    // libère les ressources natives de l'ancien écran
    }

    override fun dispose() {
        super.dispose()   // appelle hide() + dispose() sur l'écran courant via Game
        batch.dispose()
        assets.dispose()  // libère toutes les ressources natives gérées par l'AssetManager
    }
}
