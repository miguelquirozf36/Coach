package com.miguel.coach

import android.content.SharedPreferences
import androidx.core.content.edit

interface ThemePreferenceStorage {
    fun readThemeId(): String?
    fun writeThemeId(id: String)
}

class SharedPreferencesThemeStorage(
    private val preferences: SharedPreferences
) : ThemePreferenceStorage {
    override fun readThemeId(): String? = preferences.getString(THEME_ID, null)

    override fun writeThemeId(id: String) {
        preferences.edit { putString(THEME_ID, id) }
    }

    private companion object {
        const val THEME_ID = "selected_theme"
    }
}

class ThemePreferenceRepository(private val storage: ThemePreferenceStorage) {
    fun load(): CoachTheme = CoachTheme.fromId(storage.readThemeId())

    fun save(theme: CoachTheme) = storage.writeThemeId(theme.id)
}
