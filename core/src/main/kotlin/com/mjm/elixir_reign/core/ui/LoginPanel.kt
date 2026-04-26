package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.scenes.scene2d.ui.Label
import com.badlogic.gdx.scenes.scene2d.ui.Table
import com.badlogic.gdx.scenes.scene2d.ui.TextButton
import com.badlogic.gdx.scenes.scene2d.ui.TextField
import com.badlogic.gdx.utils.Align
import com.mjm.elixir_reign.core.i18n.Localization

data class LoginPanelConfig(
    val backgroundDrawable: String = "loginBackground",
    val titleStyle: String = "loginTitle",
    val inputFieldStyle: String = "loginInputField",
    val buttonStyle: String = "loginButton",
    val containerPadding: Float = 20f,
    val spacing: Float = 15f
)

open class LoginPanel(
    private val visualConfig: LoginPanelConfig = LoginPanelConfig()
) : Table() {



    init {
        setFillParent(true)
        align(Align.center)
        isVisible = false

        background = UiAssets.skin.getDrawable(visualConfig.backgroundDrawable)
        pad(visualConfig.containerPadding)

        val title = Label(Localization.get("login.title"), UiAssets.skin, visualConfig.titleStyle)
        title.setAlignment(Align.center)

        val usernameField = TextField("", UiAssets.skin, visualConfig.inputFieldStyle)
        usernameField.messageText = Localization.get("login.usernamePlaceholder")

//        val passwordField = TextField("", UiAssets.skin, visualConfig.inputFieldStyle)
//        passwordField.messageText = Localization.get("login.passwordPlaceholder")
//        passwordField.passwordMode = true
//        passwordField.passwordCharacter = '*'

        val loginButton = TextButton(Localization.get("login.button"), UiAssets.skin, visualConfig.buttonStyle)

        add(title).expandX().fillX().padBottom(visualConfig.spacing)
        row()
        add(usernameField).expandX().fillX().padBottom(visualConfig.spacing)
//        row()
//        add(passwordField).expandX().fillX().padBottom(visualConfig.spacing)
        row()
        add(loginButton).expandX().fillX()
    }
}
