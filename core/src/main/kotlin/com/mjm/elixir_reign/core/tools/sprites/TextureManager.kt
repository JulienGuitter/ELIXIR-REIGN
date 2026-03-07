package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.graphics.Texture

object TextureManager {
    private val textureCache = mutableMapOf<String, Texture>()

    fun getTexture(path: String): Texture {
        return textureCache.getOrPut(path) {
            Texture(path).also { tex ->
                // Linear = interpolation bilinéaire → rendu smooth au lieu de pixelisé
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
    }

    fun unloadTexture(path: String) {
        textureCache[path]?.dispose()
        textureCache.remove(path)
    }

    fun unloadAll() {
        textureCache.values.forEach { it.dispose() }
        textureCache.clear()
    }

    fun getCacheSize(): Int = textureCache.size
}

