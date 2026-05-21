package com.mjm.elixir_reign.core.utils

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Preferences

object SettingsManager {
    private const val PREFERENCES_NAME = "elixir-reign-settings"
    private const val LEGACY_PREFERENCES_NAME = "elixir_reign_settings"

    private enum class SettingKey(val storageKey: String, val defaultValue: Any) {
        USERNAME("username", ""),
        LANGUAGE("language", "fr")
    }

    private var loaded = false
    private val values: MutableMap<SettingKey, Any> = mutableMapOf(
        SettingKey.USERNAME to SettingKey.USERNAME.defaultValue,
        SettingKey.LANGUAGE to SettingKey.LANGUAGE.defaultValue
    )

    var username: String
        get() = get(SettingKey.USERNAME)
        set(value) = set(SettingKey.USERNAME, value)

    var language: String
        get() = get(SettingKey.LANGUAGE)
        set(value) = set(SettingKey.LANGUAGE, value)

    fun load() {
        val prefs = preferencesOrNull() ?: return

        SettingKey.entries.forEach { key ->
            values[key] = readValue(prefs, key)
        }

        migrateLegacyLanguageIfNeeded(prefs)
        loaded = true
    }

    fun save() {
        val prefs = preferencesOrNull() ?: return

        SettingKey.entries.forEach { key ->
            writeValue(prefs, key, values.getValue(key))
        }

        prefs.flush()
        loaded = true
    }

    private fun <T : Any> get(key: SettingKey): T {
        ensureLoaded()

        @Suppress("UNCHECKED_CAST")
        return values.getValue(key) as T
    }

    private fun set(key: SettingKey, value: Any) {
        values[key] = value

        // Persist immediately once LibGDX is ready.
        if (loaded) {
            save()
        } else {
            ensureLoaded()
            if (loaded) save()
        }
    }

    private fun ensureLoaded() {
        if (!loaded) {
            load()
        }
    }

    private fun migrateLegacyLanguageIfNeeded(prefs: Preferences) {
        if (prefs.contains(SettingKey.LANGUAGE.storageKey)) return

        val legacyPrefs = legacyPreferencesOrNull() ?: return
        if (!legacyPrefs.contains(SettingKey.LANGUAGE.storageKey)) return

        val legacyLanguage = legacyPrefs.getString(
            SettingKey.LANGUAGE.storageKey,
            SettingKey.LANGUAGE.defaultValue as String
        )

        values[SettingKey.LANGUAGE] = legacyLanguage
        prefs.putString(SettingKey.LANGUAGE.storageKey, legacyLanguage)
        prefs.flush()
    }

    private fun preferencesOrNull(): Preferences? =
        if (Gdx.app != null) Gdx.app.getPreferences(PREFERENCES_NAME) else null

    private fun legacyPreferencesOrNull(): Preferences? =
        if (Gdx.app != null) Gdx.app.getPreferences(LEGACY_PREFERENCES_NAME) else null

    private fun readValue(prefs: Preferences, key: SettingKey): Any = when (val defaultValue = key.defaultValue) {
        is String -> prefs.getString(key.storageKey, defaultValue)
        is Int -> prefs.getInteger(key.storageKey, defaultValue)
        is Long -> prefs.getLong(key.storageKey, defaultValue)
        is Float -> prefs.getFloat(key.storageKey, defaultValue)
        is Boolean -> prefs.getBoolean(key.storageKey, defaultValue)
        else -> error("Unsupported setting type for key: ${key.storageKey}")
    }

    private fun writeValue(prefs: Preferences, key: SettingKey, value: Any) {
        when (value) {
            is String -> prefs.putString(key.storageKey, value)
            is Int -> prefs.putInteger(key.storageKey, value)
            is Long -> prefs.putLong(key.storageKey, value)
            is Float -> prefs.putFloat(key.storageKey, value)
            is Boolean -> prefs.putBoolean(key.storageKey, value)
            else -> error("Unsupported setting type for key: ${key.storageKey}")
        }
    }
}
