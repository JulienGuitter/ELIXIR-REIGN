package com.mjm.elixir_reign.core.tools.sprites

import com.badlogic.gdx.assets.AssetManager
import com.badlogic.gdx.graphics.Texture

object TextureManager {

    // Chemins de tous les sprites du jeu — source unique de vérité
    val ALL_SPRITE_PATHS = listOf(
        // Units
        "sprites/units/anim_pack_chr_archer.png",
        "sprites/units/anim_pack_chr_barbarian.png",
        "sprites/units/anim_pack_chr_giant.png",
        // Buildings
        "sprites/buildings/dark_elixir_pack.png"
    )

    // AssetManager injecté depuis Main une fois le chargement terminé
    private var assetManager: AssetManager? = null

    // Cache de fallback pour les textures hors AssetManager (ui, etc.)
    private val fallbackCache = mutableMapOf<String, Texture>()

    /** À appeler depuis Main après que l'AssetManager a fini de charger */
    fun init(assets: AssetManager) {
        assetManager = assets
        // Appliquer le filtre Linear sur toutes les textures déjà chargées par l'AssetManager
        ALL_SPRITE_PATHS.forEach { path ->
            if (assets.isLoaded(path, Texture::class.java)) {
                assets.get(path, Texture::class.java)
                    .setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
    }

    /** Enregistre tous les sprites dans l'AssetManager pour le chargement async */
    fun queueAll(assets: AssetManager) {
        ALL_SPRITE_PATHS.forEach { path ->
            if (!assets.isLoaded(path, Texture::class.java)) {
                assets.load(path, Texture::class.java)
            }
        }
    }

    fun getTexture(path: String): Texture {
        // Si l'AssetManager a déjà chargé ce chemin, on l'utilise directement
        val am = assetManager
        if (am != null && am.isLoaded(path, Texture::class.java)) {
            return am.get(path, Texture::class.java)
        }
        // Fallback : chargement synchrone avec cache local (ex. logo, background)
        return fallbackCache.getOrPut(path) {
            Texture(path).also { tex ->
                tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }
    }

    fun unloadTexture(path: String) {
        fallbackCache[path]?.dispose()
        fallbackCache.remove(path)
    }

    fun unloadAll() {
        fallbackCache.values.forEach { it.dispose() }
        fallbackCache.clear()
        assetManager = null
    }

    fun getCacheSize(): Int = fallbackCache.size
}

