package com.mjm.elixir_reign.core.i18n

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.utils.I18NBundle
import com.mjm.elixir_reign.core.utils.SettingsManager
import java.util.Locale

data class LanguageOption(val code: String, val displayName: String)

object Localization {
    private const val DEFAULT_LANGUAGE = "fr"

    private var bundle: I18NBundle
    private var currentLanguage: String = DEFAULT_LANGUAGE

    val availableLanguages = listOf(
        LanguageOption("fr", "Français"),
        LanguageOption("en", "English")
    )

    fun indexOfCurrentLanguage(): Int =
        availableLanguages.indexOfFirst { it.code == getCurrentLanguage() }

    fun languageCodeAt(index: Int): String? =
        availableLanguages.getOrNull(index)?.code

    init {
        currentLanguage = normalizeLanguage(SettingsManager.language)
        bundle = loadBundle(currentLanguage)
    }

    private fun loadBundle(language: String): I18NBundle {
        val locale = Locale(language)
        val fileHandle = Gdx.files.internal("i18n/strings")
        return I18NBundle.createBundle(fileHandle, locale, "ISO-8859-1")
    }

    fun setLanguage(language: String) {
        currentLanguage = normalizeLanguage(language)
        bundle = loadBundle(currentLanguage)
        SettingsManager.language = currentLanguage
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

    private fun normalizeLanguage(language: String): String {
        return if (availableLanguages.any { it.code == language }) language else DEFAULT_LANGUAGE
    }
}
