package com.shemiji.emogibattery.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
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
    val shimejiSizeDp: Int = 112,
    val shimejiSpeed: Float = 1f,
    val toolbarHeight: Int = 34,
    val toolbarLeftMargin: Int = 16,
    val toolbarRightMargin: Int = 16,
    val toolbarIconColor: String? = null,
    val toolbarBackgroundColor: String? = null,
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
        val shimejiSizeDp = intPreferencesKey("shimeji_size_dp")
        val shimejiSpeed = floatPreferencesKey("shimeji_speed")
        val toolbarHeight = intPreferencesKey("toolbar_height")
        val toolbarLeftMargin = intPreferencesKey("toolbar_left_margin")
        val toolbarRightMargin = intPreferencesKey("toolbar_right_margin")
        val toolbarIconColor = stringPreferencesKey("toolbar_icon_color")
        val toolbarBackgroundColor = stringPreferencesKey("toolbar_background_color")
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
                shimejiSizeDp = preferences[Keys.shimejiSizeDp] ?: 112,
                shimejiSpeed = preferences[Keys.shimejiSpeed] ?: 1f,
                toolbarHeight = preferences[Keys.toolbarHeight] ?: 34,
                toolbarLeftMargin = preferences[Keys.toolbarLeftMargin] ?: 16,
                toolbarRightMargin = preferences[Keys.toolbarRightMargin] ?: 16,
                toolbarIconColor = preferences[Keys.toolbarIconColor],
                toolbarBackgroundColor = preferences[Keys.toolbarBackgroundColor],
            )
        }

    suspend fun setBatteryCustomization(
        batteryEmojiId: String,
        toolbarStyleId: String,
        enabled: Boolean,
        height: Int = 34,
        leftMargin: Int = 16,
        rightMargin: Int = 16,
        iconColor: String? = null,
        backgroundColor: String? = null,
    ) {
        context.selectionDataStore.edit { preferences ->
            preferences[Keys.batteryEmojiId] = batteryEmojiId
            preferences[Keys.toolbarStyleId] = toolbarStyleId
            preferences[Keys.batteryToolbarEnabled] = enabled
            preferences[Keys.toolbarHeight] = height
            preferences[Keys.toolbarLeftMargin] = leftMargin
            preferences[Keys.toolbarRightMargin] = rightMargin
            iconColor?.let { preferences[Keys.toolbarIconColor] = it }
            backgroundColor?.let { preferences[Keys.toolbarBackgroundColor] = it }
        }
    }

    suspend fun setToolbarAppearance(
        height: Int,
        leftMargin: Int,
        rightMargin: Int,
        iconColor: String,
        backgroundColor: String,
    ) {
        context.selectionDataStore.edit { preferences ->
            preferences[Keys.toolbarHeight] = height
            preferences[Keys.toolbarLeftMargin] = leftMargin
            preferences[Keys.toolbarRightMargin] = rightMargin
            preferences[Keys.toolbarIconColor] = iconColor
            preferences[Keys.toolbarBackgroundColor] = backgroundColor
        }
    }

    suspend fun setBatteryToolbarEnabled(enabled: Boolean) {
        context.selectionDataStore.edit { it[Keys.batteryToolbarEnabled] = enabled }
    }

    suspend fun setWallpaper(wallpaperId: String) {
        context.selectionDataStore.edit { it[Keys.wallpaperId] = wallpaperId }
    }

    suspend fun setShimeji(
        characterId: String,
        enabled: Boolean,
        sizeDp: Int = 112,
        speed: Float = 1f,
    ) {
        context.selectionDataStore.edit { preferences ->
            preferences[Keys.shimejiCharacterId] = characterId
            preferences[Keys.shimejiEnabled] = enabled
            preferences[Keys.shimejiSizeDp] = sizeDp.coerceIn(72, 176)
            preferences[Keys.shimejiSpeed] = speed.coerceIn(0.5f, 3f)
        }
    }

    suspend fun setShimejiEnabled(enabled: Boolean) {
        context.selectionDataStore.edit { it[Keys.shimejiEnabled] = enabled }
    }
}
