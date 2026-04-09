package com.mjm.elixir_reign.lwjgl3.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.ui.UiAssets

class LobbyWaitingMenu(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch


    override fun show() {
        // Le stage et le SpriteBatch
        stage = Stage(ExtendViewport(1920f, 1080f))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        // Créer les boutons avec les textes localisés
        val btnReturn = TextButton(Localization.get("global.back"), UiAssets.skin)

        btnReturn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.navigateTo(ScreenRoute.MENU)
            }
        })

        // Waiting label
        val titleLabel = Label(Localization.get("lobbyWaiting.title"), UiAssets.skin)
        titleLabel.setFontScale(2f)

        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.logoTransparent))).apply {
            color = Color(1f, 1f, 1f, 0.85f)
        }

        // Créer la table pour organiser les boutons au centre
        val table = Table().apply {
            setFillParent(true)
            add(logoImage).width(220f).height(220f).padBottom(20f).row()
            add(titleLabel).colspan(2).pad(20f).row()
            add(btnReturn).width(300f).height(80f).pad(15f).row()
        }

        stage.addActor(table)
        stage.addActor(UiAssets.createVersionTable())
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        UiAssets.drawBackground(stage, spriteBatch)

        // Update et draw du stage pour les boutons
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        spriteBatch.dispose()
        // UiAssets est géré par Main, ne pas disposer ici
    }
}
