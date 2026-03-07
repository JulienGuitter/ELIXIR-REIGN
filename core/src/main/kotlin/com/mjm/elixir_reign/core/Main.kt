package com.mjm.elixir_reign.core

import com.badlogic.gdx.Game
import com.badlogic.gdx.Screen
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.assets.AssetManager
import com.mjm.elixir_reign.core.platform.PlatformBridge
import com.mjm.elixir_reign.core.screens.LoadingScreen
import com.mjm.elixir_reign.core.screens.MenuScreen
import com.mjm.elixir_reign.core.tools.sprites.SpriteAnimationManager
import com.mjm.elixir_reign.core.tools.sprites.TextureManager
import com.mjm.elixir_reign.core.ui.UiAssets

class Main(val platform: PlatformBridge) : Game() {

    lateinit var batch: SpriteBatch
    lateinit var assets: AssetManager

    override fun create() {
        batch = SpriteBatch()
        assets = AssetManager()

        // Charge uniquement le logo pour l'afficher dans le LoadingScreen
        UiAssets.loadMinimal()

        changeScreen(LoadingScreen(this))
    }

    fun changeScreen(screen: Screen) {
        val previous = this.screen
        setScreen(screen)
        previous?.dispose()
    }

    /** Appelé par LoadingScreen une fois que l'AssetManager a tout chargé */
    fun onAssetsLoaded() {
        TextureManager.init(assets)
        UiAssets.finishLoading(assets)   // construit le skin avec la vraie font
        SpriteAnimationManager.preloadAll() // parse tous les JSON d'animation upfront
    }

    override fun dispose() {
        super.dispose()   // appelle hide() + dispose() sur l'écran courant via Game
        batch.dispose()
        assets.dispose()  // libère toutes les ressources natives gérées par l'AssetManager
        // Libérer les assets UI ici, pas dans les screens
        UiAssets.dispose()
    }
}
