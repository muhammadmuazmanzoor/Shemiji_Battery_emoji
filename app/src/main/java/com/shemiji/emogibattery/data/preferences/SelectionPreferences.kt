package com.shemiji.emogibattery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.selectionDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "content_selections",
)

data class UserSelections(
    val batteryEmojiId: String? = null,
    val toolbarStyleId: String? = null,
    val wallpaperId: String? = null,
    val shimejiCharacterId: String? = null,
    val batteryToolbarEnabled: Boolean = false,
    val shimejiEnabled: Boolean = false,
)

@Singleton
class SelectionPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val batteryEmojiId = stringPreferencesKey("battery_emoji_id")
        val toolbarStyleId = stringPreferencesKey("toolbar_style_id")
        val wallpaperId = stringPreferencesKey("wallpaper_id")
        val shimejiCharacterId = stringPreferencesKey("shimeji_character_id")
        val batteryToolbarEnabled = booleanPreferencesKey("battery_toolbar_enabled")
        val shimejiEnabled = booleanPreferencesKey("shimeji_enabled")
    }

    val selections: Flow<UserSelections> = context.selectionDataStore.data
        .catch { throwable ->
            if (throwable is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw throwable
        }
        .map { preferences ->
            UserSelections(
                batteryEmojiId = preferences[Keys.batteryEmojiId],
                toolbarStyleId = preferences[Keys.toolbarStyleId],
                wallpaperId = preferences[Keys.wallpaperId],
                shimejiCharacterId = preferences[Keys.shimejiCharacterId],
                batteryToolbarEnabled = preferences[Keys.batteryToolbarEnabled] ?: false,
                shimejiEnabled = preferences[Keys.shimejiEnabled] ?: false,
            )
        }

    suspend fun setBatteryCustomization(
        batteryEmojiId: String,
        toolbarStyleId: String,
        enabled: Boolean,
    ) {
        context.selectionDataStore.edit { preferences ->
            preferences[Keys.batteryEmojiId] = batteryEmojiId
            preferences[Keys.toolbarStyleId] = toolbarStyleId
            preferences[Keys.batteryToolbarEnabled] = enabled
        }
    }

    suspend fun setBatteryToolbarEnabled(enabled: Boolean) {
        context.selectionDataStore.edit { it[Keys.batteryToolbarEnabled] = enabled }
    }

    suspend fun setWallpaper(wallpaperId: String) {
        context.selectionDataStore.edit { it[Keys.wallpaperId] = wallpaperId }
    }

    suspend fun setShimeji(characterId: String, enabled: Boolean) {
        context.selectionDataStore.edit { preferences ->
            preferences[Keys.shimejiCharacterId] = characterId
            preferences[Keys.shimejiEnabled] = enabled
        }
    }

    suspend fun setShimejiEnabled(enabled: Boolean) {
        context.selectionDataStore.edit { it[Keys.shimejiEnabled] = enabled }
    }
}
