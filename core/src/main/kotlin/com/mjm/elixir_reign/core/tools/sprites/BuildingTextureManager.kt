package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture

object BuildingTextureManager {
    private var barracksTexture: Texture? = null

    fun getBarracksTexture(): Texture {
        return barracksTexture ?: createBarracksTexture().also {
            barracksTexture = it
        }
    }

    private fun createBarracksTexture(): Texture {
        val pixmap = Pixmap(64, 64, Pixmap.Format.RGBA8888)
        pixmap.setBlending(Pixmap.Blending.SourceOver)
        pixmap.setColor(0f, 0f, 0f, 0f)
        pixmap.fill()

        pixmap.setColor(Color(0.18f, 0.13f, 0.10f, 1f))
        pixmap.fillRectangle(12, 28, 40, 24)
        pixmap.setColor(Color(0.52f, 0.32f, 0.16f, 1f))
        pixmap.fillRectangle(16, 32, 32, 16)

        pixmap.setColor(Color(0.62f, 0.10f, 0.08f, 1f))
        for (row in 0 until 18) {
            pixmap.drawLine(32 - row, 27 - row / 2, 32 + row, 27 - row / 2)
        }
        pixmap.setColor(Color(0.82f, 0.72f, 0.48f, 1f))
        pixmap.drawRectangle(24, 36, 16, 16)
        pixmap.setColor(Color(0.12f, 0.09f, 0.07f, 1f))
        pixmap.fillRectangle(28, 40, 8, 12)
        pixmap.setColor(Color(0.05f, 0.04f, 0.03f, 0.55f))
        pixmap.fillRectangle(10, 52, 44, 5)

        return Texture(pixmap).also { texture ->
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
            pixmap.dispose()
        }
    }

    fun dispose() {
        barracksTexture?.dispose()
        barracksTexture = null
    }
}
