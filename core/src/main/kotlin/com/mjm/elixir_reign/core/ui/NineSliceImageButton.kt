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
    var buttonCheckedColor: Color = Color(0.92f, 0.77f, 0.30f, 1f)
    var iconCheckedColor: Color = Color(1f, 0.98f, 0.88f, 1f)

    init {
        val buttonUp = NinePatchDrawable(
            NinePatch(backgroundTexture, ninePatchBorder, ninePatchBorder, ninePatchBorder, ninePatchBorder)
        ).tint(buttonUpColor)

        val buttonDown = NinePatchDrawable(
            NinePatch(backgroundTexture, ninePatchBorder, ninePatchBorder, ninePatchBorder, ninePatchBorder)
        ).tint(buttonDownColor)

        val buttonChecked = NinePatchDrawable(
            NinePatch(backgroundTexture, ninePatchBorder, ninePatchBorder, ninePatchBorder, ninePatchBorder)
        ).tint(buttonCheckedColor)

        val iconDrawable = TextureRegionDrawable(TextureRegion(iconTexture))
        val iconCheckedDrawable = TextureRegionDrawable(TextureRegion(iconTexture)).tint(iconCheckedColor)

        style = ImageButtonStyle().apply {
            up = buttonUp
            down = buttonDown
            imageUp = iconDrawable
            imageDown = iconDrawable
            checked = buttonChecked
            checkedDown = buttonChecked
            imageChecked = iconCheckedDrawable
        }
    }

    fun setHighlighted(highlighted: Boolean) {
        val previous = programmaticChangeEvents
        programmaticChangeEvents = false
        isChecked = highlighted
        programmaticChangeEvents = previous
    }

    fun onClick(callback: (ChangeEvent, Actor) -> Unit) {
        addListener(object : ChangeListener() {
            override fun changed(event: ChangeEvent, actor: Actor) {
                callback(event, actor)
            }
        })
    }
}
