package com.mjm.elixir_reign.core.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.mjm.elixir_reign.core.Main

class MenuScreen(private val game: Main) : ScreenAdapter() {

    private lateinit var stage: Stage
    private lateinit var buttonFont: BitmapFont
    private lateinit var upTexture: Texture
    private lateinit var downTexture: Texture

    override fun show() {
        stage = Stage(ExtendViewport(1920f, 1080f))

        // 1) Font pour les textes des boutons
        buttonFont = BitmapFont().apply {
            data.setScale(2f)  // taille plus grande
        }

        // 2) Pixmaps de couleur simple pour les boutons
        val upPixmap = Pixmap(200, 60, Pixmap.Format.RGBA8888).apply {
            setColor(Color.DARK_GRAY)
            fill()
        }
        val downPixmap = Pixmap(200, 60, Pixmap.Format.RGBA8888).apply {
            setColor(Color.GRAY)
            fill()
        }

        upTexture = Texture(upPixmap)
        downTexture = Texture(downPixmap)
        val upDrawable = TextureRegionDrawable(TextureRegion(upTexture))
        val downDrawable = TextureRegionDrawable(TextureRegion(downTexture))

        // IMPORTANT: on peut maintenant libérer les Pixmaps
        upPixmap.dispose()
        downPixmap.dispose()

        // 3) Style de bouton minimal
        val style = TextButton.TextButtonStyle().apply {
            up = upDrawable
            down = downDrawable
            font = buttonFont
            fontColor = Color.WHITE
        }

        val playBtn = TextButton("Play", style)
        val quitBtn = TextButton("Quit", style)

        playBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.changeScreen(GameScreen(game))
            }
        })
        quitBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Gdx.app.exit()
            }
        })

        val table = Table().apply {
            setFillParent(true)
            add(playBtn).pad(10f).row()
            add(quitBtn).pad(10f)
        }

        stage.addActor(table)

        Gdx.input.inputProcessor = stage
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)

        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        buttonFont.dispose()
        upTexture.dispose()
        downTexture.dispose()
    }
}
