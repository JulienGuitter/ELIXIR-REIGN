package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiAssets.createRoundedRectTexture
import java.awt.Font

class LoadingScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var progressBar: ProgressBar
    private lateinit var loadingLabel: Label
    private lateinit var contentTable: Table

    // Textures créées localement pour la progress bar (avant que UiAssets soit dispo)
    private lateinit var barBgTex: Texture
    private lateinit var barFillTex: Texture
    private lateinit var labelFont: BitmapFont
    private lateinit var blackTex: Texture

    private var loadingStarted = false
    private var loadingDone = false

    override fun show() {
        stage = Stage(ExtendViewport(1920f, 1080f))
        spriteBatch = SpriteBatch()

        barBgTex = createRoundedRectTexture(
            width = 64,
            height = 16,
            radius = 8,
            color = Color(0.20f, 0.16f, 0.10f, 1f)
        )

        barFillTex = createRoundedRectTexture(
            width = 64,
            height = 16,
            radius = 8,
            color = Color(0.90f, 0.72f, 0.18f, 1f)
        )

        val bgPatch = NinePatch(barBgTex, 8, 8, 8, 8)
        val fillPatch = NinePatch(barFillTex, 8, 8, 8, 8)

        val bgDrawable = NinePatchDrawable(bgPatch).apply {
            leftWidth = 0f
            rightWidth = 0f
            topHeight = 0f
            bottomHeight = 0f
        }

        val fillDrawable = NinePatchDrawable(fillPatch).apply {
            minWidth = 0f
            leftWidth = 0f
            rightWidth = 0f
            topHeight = 0f
            bottomHeight = 0f
        }

        val pbStyle = ProgressBar.ProgressBarStyle().apply {
            background = bgDrawable
            knobBefore = fillDrawable
        }

        progressBar = ProgressBar(0f, 1f, 0.01f, false, pbStyle).apply {
            value = 0f
        }

        labelFont = com.badlogic.gdx.graphics.g2d.BitmapFont()

        // -- Label "Chargement…" avec police de base (avant skin complet) --
        val labelStyle = Label.LabelStyle(
            labelFont,
            Color.WHITE
        )
        loadingLabel = Label("Chargement...", labelStyle)

        // -- Logo --
        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.logoTransparent))).apply {
            color = Color(1f, 1f, 1f, 0.9f)
        }

        // -- Table de contenu : commence invisible, fade-in au show --
        contentTable = Table().apply {
            setFillParent(true)
            center()
            color = Color(1f, 1f, 1f, 0f)   // invisible au départ
            add(logoImage).size(450f).padBottom(40f).row()
            add(progressBar).width(500f).height(16f).padBottom(20f).row()
            add(loadingLabel).padBottom(10f)
        }
        stage.addActor(contentTable)
        // Fade-in du contenu dès l'affichage
        contentTable.addAction(Actions.fadeIn(0.4f))

        // -- Overlay noir pour le fade-out final (couvre uniquement le contenu, pas le bg) --
        val blackPixmap = Pixmap(1, 1, Pixmap.Format.RGBA8888).apply {
            setColor(Color.BLACK); fill()
        }
        blackTex = Texture(blackPixmap)
        blackPixmap.dispose()

        // -- Démarrer le chargement asynchrone --
        UiAssets.queueLoading(game.assets)
        loadingStarted = true
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        // Background toujours disponible (chargé synchroniquement dans loadMinimal)
        UiAssets.drawBackground(stage, spriteBatch)

        if (loadingStarted && !loadingDone) {
            val finished = game.assets.update()
            val progress = UiAssets.getProgress(game.assets)
            progressBar.value = progress

            val pct = (progress * 100).toInt()
            loadingLabel.setText("Chargement... $pct%")

            if (finished && progress >= 1f) {
                loadingDone = true
                loadingLabel.setText("Initialisation des animations...")
                progressBar.value = 1f

                // Fade-out du contenu seulement, le background reste visible
                contentTable.addAction(
                    Actions.sequence(
                        Actions.delay(0.5f),
                        Actions.fadeOut(0.5f),
                        Actions.run {
                            game.onAssetsLoaded()
                            game.changeScreen(MenuScreen(game))
                        }
                    )
                )
            }
        }

        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        spriteBatch.dispose()
        barBgTex.dispose()
        barFillTex.dispose()
        labelFont.dispose()
        blackTex.dispose()
    }
}
