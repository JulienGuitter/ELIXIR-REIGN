package com.mjm.elixir_reign.android.screens

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
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.core.network.MatchmakingClient

class LobbyWaitingMenu(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var titleLabel: Label
    private lateinit var errorLabel: Label
    private lateinit var reconnectButton: TextButton

    override fun show() {
        // Le stage et le SpriteBatch
        stage = Stage(ExtendViewport(UiAssets.screenResolution.x, UiAssets.screenResolution.y))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        // Créer les boutons avec les textes localisés
        val btnReturn = TextButton(Localization.get("global.back"), UiAssets.skin)
        reconnectButton = TextButton(Localization.get("network.action.reconnect"), UiAssets.skin)

        btnReturn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                MatchmakingClient.cancelMatchmaking()
                game.navigateTo(ScreenRoute.MENU)
            }
        })

        reconnectButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                MatchmakingClient.reconnectToLastInstance()
            }
        })

        // Waiting label
        titleLabel = Label(MatchmakingClient.getStatusText(), UiAssets.skin)
        titleLabel.setFontScale(2f)

        errorLabel = Label("", UiAssets.skin)
        errorLabel.setWrap(true)

        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.LOGO_TRANSPARENT)))).apply {
            color = Color(1f, 1f, 1f, 0.85f)
        }

        // Créer la table pour organiser les boutons au centre
        val table = Table().apply {
            setFillParent(true)
            add(logoImage).width(220f).height(220f).padBottom(20f).row()
            add(titleLabel).width(560f).pad(20f).row()
            add(errorLabel).width(560f).padBottom(10f).row()
            add(reconnectButton).width(300f).height(80f).padBottom(8f).row()
            add(btnReturn).width(300f).height(80f).pad(15f).row()
        }

        stage.addActor(table)
        stage.addActor(UiAssets.createVersionTable())
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        if (MatchmakingClient.consumeGameReadyWhenWorldReady()) {
            game.navigateTo(ScreenRoute.GAME)
            return
        }

        titleLabel.setText(MatchmakingClient.getStatusText())
        val errorText = MatchmakingClient.getErrorText().orEmpty()
        errorLabel.setText(errorText)
        reconnectButton.isVisible = errorText.isNotBlank() && MatchmakingClient.canReconnectToLastInstance()

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
