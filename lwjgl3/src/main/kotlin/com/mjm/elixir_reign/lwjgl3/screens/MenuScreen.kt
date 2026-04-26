package com.mjm.elixir_reign.lwjgl3.screens

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.InputEvent
import com.badlogic.gdx.scenes.scene2d.InputListener
import com.badlogic.gdx.scenes.scene2d.Stage
import com.badlogic.gdx.scenes.scene2d.Touchable
import com.badlogic.gdx.scenes.scene2d.actions.Actions
import com.badlogic.gdx.scenes.scene2d.ui.Image
import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.mjm.elixir_reign.core.Main
import com.mjm.elixir_reign.core.i18n.Localization
import com.mjm.elixir_reign.core.navigation.ScreenRoute
import com.mjm.elixir_reign.core.ui.UiAssets
import com.mjm.elixir_reign.core.ui.UiImage
import com.mjm.elixir_reign.core.utils.SettingsManager

class MenuScreen(private val game: Main) : ScreenAdapter() {

    private companion object {
        const val BUTTON_PATCH_INSET = 20
    }

    private lateinit var stage: Stage
    private lateinit var spriteBatch: SpriteBatch
    private lateinit var usernameLabel: Label
    private lateinit var usernameField: TextField
    private lateinit var loginModalRoot: Table
//    private lateinit var overlayTexture: Texture
//    private lateinit var popupTexture: Texture

    override fun show() {
        stage = Stage(ExtendViewport(UiAssets.screenResolution.x, UiAssets.screenResolution.y))
        spriteBatch = SpriteBatch()
        Gdx.input.inputProcessor = stage

        val playBtn = TextButton(Localization.get("menu.play"), UiAssets.skin)
        val settingsBtn = TextButton(Localization.get("menu.settings"), UiAssets.skin)
        val quitBtn = TextButton(Localization.get("menu.quit"), UiAssets.skin)

        playBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.navigateTo(ScreenRoute.MODE_SELECTION)
            }
        })

        settingsBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                game.navigateTo(ScreenRoute.SETTINGS)
            }
        })

        quitBtn.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                Gdx.app.exit()
            }
        })

        val logoImage = Image(TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.LOGO_TRANSPARENT)))).apply {
            color = Color(1f, 1f, 1f, 0.85f)
        }

        val menuTable = Table().apply {
            setFillParent(true)
            color = Color(1f, 1f, 1f, 0f)
            add(logoImage).width(350f).height(350f).padBottom(20f).row()
            add(playBtn).width(300f).height(80f).pad(15f).row()
            add(settingsBtn).width(300f).height(80f).pad(15f).row()
            add(quitBtn).width(300f).height(80f).pad(15f)
        }

        stage.addActor(menuTable)
        stage.addActor(buildProfileTable())
        stage.addActor(UiAssets.createVersionTable())

        menuTable.addAction(Actions.fadeIn(0.5f))

        loginModalRoot = buildLoginModal()
        stage.addActor(loginModalRoot)

        if (SettingsManager.username.isBlank()) {
            showLoginModal()
        }
    }

    override fun resize(width: Int, height: Int) {
        stage.viewport.update(width, height, true)
    }

    override fun render(delta: Float) {
        UiAssets.drawBackground(stage, spriteBatch)
        stage.act(delta)
        stage.draw()
    }

    override fun dispose() {
        stage.dispose()
        spriteBatch.dispose()
//        if (::overlayTexture.isInitialized) overlayTexture.dispose()
//        if (::popupTexture.isInitialized) popupTexture.dispose()
    }

    private fun buildProfileTable(): Table {
        val editButton = TextButton(Localization.get("menu.editUsername"), UiAssets.skin)
        editButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                showLoginModal()
            }
        })

        usernameLabel = Label("", UiAssets.skin)
        refreshUsernameLabel()

        return Table().apply {
            setFillParent(true)
            top().right()
            add(usernameLabel).right().padTop(18f).padRight(18f).row()
            add(editButton).width(190f).height(56f).padTop(10f).padRight(18f)
        }
    }

    private fun buildLoginModal(): Table {
//        overlayTexture = UiAssets.createRoundedRectTexture(8, 8, 0, Color(0f, 0f, 0f, 1f))
//        popupTexture = UiAssets.createRoundedRectTexture(32, 32, 8, Color(0.08f, 0.08f, 0.08f, 0.95f))

        usernameField = TextField("", UiAssets.skin).apply {
            messageText = Localization.get("login.usernamePlaceholder")
        }

        val titleLabel = Label(Localization.get("login.title"), UiAssets.skin)
        val saveButton = TextButton(Localization.get("login.button"), UiAssets.skin)
        saveButton.addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                val trimmedUsername = usernameField.text.trim()
                if (trimmedUsername.isBlank()) return

                SettingsManager.username = trimmedUsername
                refreshUsernameLabel()
                hideLoginModal()
            }
        })

        val popupCard = Table().apply {
            background = NinePatchDrawable(
                NinePatch(
                    UiAssets.texture(UiImage.BUTTON_9PATCH),
                    BUTTON_PATCH_INSET,
                    BUTTON_PATCH_INSET,
                    BUTTON_PATCH_INSET,
                    BUTTON_PATCH_INSET
                )
            )
            pad(26f)
            add(titleLabel).padBottom(16f).row()
            add(usernameField).width(420f).height(62f).padBottom(16f).row()
            add(saveButton).width(220f).height(62f)
        }

        return Table().apply {
            setFillParent(true)
            isVisible = false
            touchable = Touchable.enabled
            background = TextureRegionDrawable(TextureRegion(UiAssets.texture(UiImage.BUTTON_9PATCH))).tint(Color(0f, 0f, 0f, 0.65f))
            addListener(object : InputListener() {
                override fun touchDown(event: InputEvent?, x: Float, y: Float, pointer: Int, button: Int): Boolean = true
            })
            add(popupCard).center().pad(20f)
        }
    }

    private fun showLoginModal() {
        usernameField.text = SettingsManager.username
        loginModalRoot.isVisible = true
        stage.keyboardFocus = usernameField
    }

    private fun hideLoginModal() {
        loginModalRoot.isVisible = false
        stage.keyboardFocus = null
    }

    private fun refreshUsernameLabel() {
        val name = SettingsManager.username.ifBlank { "-" }
        usernameLabel.setText(Localization.get("menu.username", name))
    }
}
