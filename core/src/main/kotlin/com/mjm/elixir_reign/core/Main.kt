package com.mjm.elixir_reign.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.assets.AssetManager
import com.mjm.elixir_reign.core.debug.FpsCounterOverlay
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.GameScreen

class Main(val platform: PlatformBridge) : Game() {

    lateinit var batch: SpriteBatch
    lateinit var assets: AssetManager
    private lateinit var fpsCounterOverlay: FpsCounterOverlay

    override fun create() {
        batch = SpriteBatch()
        assets = AssetManager()
        fpsCounterOverlay = FpsCounterOverlay()

        changeScreen(GameScreen(this))
    }

    override fun render() {
        super.render()
        fpsCounterOverlay.render()
    }

    override fun resize(width: Int, height: Int) {
        super.resize(width, height)
        fpsCounterOverlay.resize(width, height)
    }

    fun changeScreen(screen: Screen) {
        val previous = this.screen
        setScreen(screen)      // appelle hide() sur l'ancien, show() sur le nouveau
        previous?.dispose()    // libère les ressources natives de l'ancien écran
    }

    override fun dispose() {
        super.dispose()   // appelle hide() + dispose() sur l'écran courant via Game
        fpsCounterOverlay.dispose()
        batch.dispose()
        assets.dispose()  // libère toutes les ressources natives gérées par l'AssetManager
    }
}
