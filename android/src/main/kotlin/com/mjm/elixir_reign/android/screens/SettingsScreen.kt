package com.mjm.elixir_reign.android.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.ui.UiAssets
import com.badlogic.gdx.utils.Array

class SettingsScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var backBtn: TextButton

    override fun show() {
        stage = Stage(ExtendViewport(UiAssets.screenResolution.x, UiAssets.screenResolution.y))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        createUI()
    }

    private fun createUI() {
        // Nettoyer le stage avant de recréer l'UI
        stage.clear()

        // Créer la table principale
        val mainTable = Table().apply {
            setFillParent(true)
            pad(50f)
        }

        // Titre "Langues"
        val titleLabel = Label(Localization.get("settings.title"), UiAssets.skin)
        titleLabel.setFontScale(2f)
        mainTable.add(titleLabel).colspan(2).pad(20f).row()

        // Label pour la sélection
        val langLabel = Label(Localization.get("settings.language"), UiAssets.skin)
        langLabel.setFontScale(1.5f)
        mainTable.add(langLabel).colspan(2).pad(10f).row()


        val selectBox = SelectBox<String>(UiAssets.skin)

        // Liste des langues (source centralisée)
        val languageItems = Array<String>()
        for (lang in Localization.availableLanguages) {
            languageItems.add(lang.displayName)
        }
        selectBox.items = languageItems

        val currentLangIndex = Localization.indexOfCurrentLanguage()
        if (currentLangIndex >= 0) {
            selectBox.selectedIndex = currentLangIndex
        }

        selectBox.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Localization.languageCodeAt(selectBox.selectedIndex)?.let { langCode ->
                    Localization.setLanguage(langCode)
                    refreshUI()
                }
            }
        })

        mainTable.add(selectBox).width(400f).height(80f).pad(20f).row()

        // Ajouter une ligne de séparation visuelle
        mainTable.row().padTop(30f)

        // Bouton retour
        backBtn = TextButton(Localization.get("global.back"), UiAssets.skin)
        backBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.navigateTo(ScreenRoute.MENU)
            }
        })

        mainTable.add(backBtn).width(300f).height(80f).pad(20f).colspan(2)

        stage.addActor(mainTable)
        stage.addActor(UiAssets.createVersionTable())
    }

    private fun refreshUI() {
        // Recréer toute l'UI pour mettre à jour les textes et indicateurs
        createUI()
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        // Dessiner le background AVANT le stage
        UiAssets.drawBackground(stage, spriteBatch)

        // Update et draw du stage
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        spriteBatch.dispose()
    }
}
