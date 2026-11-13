package com.atiempo.wear.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.atiempo.wear.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/** Preferencias de la persona, guardadas en DataStore. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.settingsStore

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_MODE]?.let { saved -> ThemeMode.entries.firstOrNull { it.name == saved } } ?: ThemeMode.LIGHT
    }
    val vibrationsEnabled: Flow<Boolean> = store.data.map { it[VIBRATIONS] ?: true }
    val demoMode: Flow<Boolean> = store.data.map { it[DEMO_MODE] ?: false }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[THEME_MODE] = mode.name }
    suspend fun setVibrationsEnabled(enabled: Boolean) = store.edit { it[VIBRATIONS] = enabled }
    suspend fun setDemoMode(enabled: Boolean) = store.edit { it[DEMO_MODE] = enabled }

    /** Indica si los datos de ejemplo ya se sembraron (tarea 2.5). */
    suspend fun isSeeded(): Boolean = store.data.first()[SEEDED] ?: false
    suspend fun markSeeded() = store.edit { it[SEEDED] = true }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val VIBRATIONS = booleanPreferencesKey("vibrations_enabled")
        val DEMO_MODE = booleanPreferencesKey("demo_mode")
        val SEEDED = booleanPreferencesKey("seeded")
    }
}
