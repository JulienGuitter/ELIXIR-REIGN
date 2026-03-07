package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
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
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.shared.GameVersion.VERSION

class MenuScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch


    override fun show() {
        // Le stage et le SpriteBatch
        stage = Stage(ExtendViewport(1920f, 1080f))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        // Créer les boutons avec les textes localisés
        val playBtn = TextButton(Localization.get("menu.play"), UiAssets.skin)
        val settingsBtn = TextButton(Localization.get("menu.settings"), UiAssets.skin)
        val quitBtn = TextButton(Localization.get("menu.quit"), UiAssets.skin)

        // Ajouter les listeners
        playBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.changeScreen(ModeSelectionScreen(game))
            }
        })

        settingsBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.changeScreen(SettingsScreen(game))
            }
        })

        quitBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Gdx.app.exit()
            }
        })

        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.logoTransparent))).apply {
            color = Color(1f, 1f, 1f, 0.85f)
        }

        // Créer la table pour organiser les boutons au centre
        val table = Table().apply {
            setFillParent(true)
            color = Color(1f, 1f, 1f, 0f)   // invisible au départ
            add(logoImage).width(350f).height(350f).padBottom(20f).row()
            add(playBtn).width(300f).height(80f).pad(15f).row()
            add(settingsBtn).width(300f).height(80f).pad(15f).row()
            add(quitBtn).width(300f).height(80f).pad(15f)
        }

        stage.addActor(table)
        stage.addActor(UiAssets.createVersionTable())

        // Fade-in du contenu uniquement — le background reste visible en continu
        table.addAction(Actions.fadeIn(0.5f))
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
