package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.TubeThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tubeStoreDataStore: DataStore<Preferences> by preferencesDataStore(name = "tube_store_prefs")

data class LauncherThemeSettings(
    val themePreset: TubeThemePreset = TubeThemePreset.CRIMSON,
    val darkMode: Boolean = true
)

class PreferencesManager(private val context: Context) {

    private object Keys {
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val themeSettings: Flow<LauncherThemeSettings> = context.tubeStoreDataStore.data.map { prefs ->
        val rawPreset = prefs[Keys.THEME_PRESET]
        val preset = enumValues<TubeThemePreset>().firstOrNull { it.name == rawPreset }
            ?: TubeThemePreset.CRIMSON
        LauncherThemeSettings(
            themePreset = preset,
            darkMode = prefs[Keys.DARK_MODE] ?: true
        )
    }

    suspend fun setThemePreset(preset: TubeThemePreset) {
        context.tubeStoreDataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = preset.name
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.tubeStoreDataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }
}
