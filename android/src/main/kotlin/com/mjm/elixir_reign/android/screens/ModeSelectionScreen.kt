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
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.core.network.MatchmakingClient
import com.mjm.elixir_reign.core.session.GameSession
import com.mjm.elixir_reign.shared.type.GameType

class ModeSelectionScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch


    override fun show() {
        // Le stage et le SpriteBatch
        stage = Stage(ExtendViewport(UiAssets.screenResolution.x, UiAssets.screenResolution.y))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        // Créer les boutons avec les textes localisés
        val btnSolo = TextButton(Localization.get("modeSelection.solo"), UiAssets.skin)
        val btn1v1 = TextButton("1 " + Localization.get("modeSelection.versus") + " 1", UiAssets.skin)
        val btn1v3 = TextButton("1 " + Localization.get("modeSelection.versus") + " 3", UiAssets.skin)
        val btn2v2 = TextButton("2 " + Localization.get("modeSelection.versus") + " 2", UiAssets.skin)
        val btnReturn = TextButton(Localization.get("global.back"), UiAssets.skin)

        // Ajouter les listeners
        btnSolo.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                MatchmakingClient.startOfflineMode()
                GameSession.startSolo()
                game.navigateTo(ScreenRoute.GAME)
            }
        })

        btn1v1.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                GameSession.startMultiplayer(GameType.G1V1)
                MatchmakingClient.startMatchmaking(GameType.G1V1)
                game.navigateTo(ScreenRoute.LOBBY_WAITING)
            }
        })

        btn1v3.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                GameSession.startMultiplayer(GameType.G1V3)
                MatchmakingClient.startMatchmaking(GameType.G1V3)
                game.navigateTo(ScreenRoute.LOBBY_WAITING)
            }
        })

        btn2v2.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                GameSession.startMultiplayer(GameType.G2V2)
                MatchmakingClient.startMatchmaking(GameType.G2V2)
                game.navigateTo(ScreenRoute.LOBBY_WAITING)
            }
        })

        btnReturn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.navigateTo(ScreenRoute.MENU)
            }
        })

        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.LOGO_TRANSPARENT)))).apply {
            color = Color(1f, 1f, 1f, 0.85f)
        }

        // Créer la table pour organiser les boutons au centre
        val table = Table().apply {
            setFillParent(true)
            add(logoImage).width(220f).height(220f).padBottom(20f).row()
            add(btnSolo).width(300f).height(80f).pad(15f).row()
            add(btn1v1).width(300f).height(80f).pad(15f).row()
            add(btn1v3).width(300f).height(80f).pad(15f).row()
//            add(btn2v2).width(300f).height(80f).pad(15f).row()
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
