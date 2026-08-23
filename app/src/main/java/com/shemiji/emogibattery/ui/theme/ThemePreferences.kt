package com.shemiji.emogibattery.ui.theme

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("app_theme")
    }

    val themeFlow: Flow<AppTheme> = context.dataStore.data.map { prefs ->
        when (prefs[THEME_KEY]) {
            "LIGHT" -> AppTheme.LIGHT
            "DARK"  -> AppTheme.DARK
            else    -> AppTheme.SYSTEM
        }
    }

    fun getThemeSync(): AppTheme {
        val prefs = context.getSharedPreferences("theme_sync_prefs", Context.MODE_PRIVATE)
        return when (prefs.getString("app_theme", "SYSTEM")) {
            "LIGHT" -> AppTheme.LIGHT
            "DARK"  -> AppTheme.DARK
            else    -> AppTheme.SYSTEM
        }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { prefs -> prefs[THEME_KEY] = theme.name }
        context.getSharedPreferences("theme_sync_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_theme", theme.name).apply()
    }
}
