package com.mjm.elixir_reign.core.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.scenes.scene2d.Actor
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener.ChangeEvent
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable

class NineSliceImageButton(
    backgroundTexture: Texture,
    iconTexture: Texture
) : ImageButton(ImageButtonStyle()) {

    var ninePatchBorder: Int = 20
    var buttonUpColor: Color = Color(0.95f, 0.95f, 0.95f, 1f)
    var buttonDownColor: Color = Color(0.75f, 0.75f, 0.75f, 1f)

    init {
        val buttonUp = NinePatchDrawable(
            NinePatch(backgroundTexture, ninePatchBorder, ninePatchBorder, ninePatchBorder, ninePatchBorder)
        ).tint(buttonUpColor)

        val buttonDown = NinePatchDrawable(
            NinePatch(backgroundTexture, ninePatchBorder, ninePatchBorder, ninePatchBorder, ninePatchBorder)
        ).tint(buttonDownColor)

        val iconDrawable = TextureRegionDrawable(TextureRegion(iconTexture))

        style = ImageButtonStyle().apply {
            up = buttonUp
            down = buttonDown
            imageUp = iconDrawable
            imageDown = iconDrawable
        }
    }

    fun onClick(callback: (ChangeEvent, Actor) -> Unit) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                callback(event, actor)
            }
        })
    }
}
