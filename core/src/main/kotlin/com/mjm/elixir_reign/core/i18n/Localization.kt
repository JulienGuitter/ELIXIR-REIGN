package com.mjm.elixir_reign.core.i18n

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import java.util.Locale

object Localization {
    private var bundle: I18NBundle
    private var currentLanguage: String = "fr"

    init {
        // Par défaut, on charge le français
        bundle = loadBundle(currentLanguage)
    }

    private fun loadBundle(language: String): I18NBundle {
        val locale = Locale(language)
        val fileHandle = Gdx.files.internal("i18n/strings")
        return I18NBundle.createBundle(fileHandle, locale, "ISO-8859-1")
    }

    fun setLanguage(language: String) {
        currentLanguage = language
        bundle = loadBundle(language)
    }

    fun getCurrentLanguage(): String {
        return currentLanguage
    }

    fun get(key: String): String {
        return bundle.get(key)
    }

    fun get(key: String, vararg args: Any): String {
        return bundle.format(key, *args)
    }
}


