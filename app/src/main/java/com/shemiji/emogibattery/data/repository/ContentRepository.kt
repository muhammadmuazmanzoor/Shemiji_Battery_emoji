package com.shemiji.emogibattery.data.repository

import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.model.WallpaperItem

sealed interface ContentResult<out T> {
    data class Success<T>(val value: T) : ContentResult<T>
    data class Error(val message: String, val cause: Throwable? = null) : ContentResult<Nothing>
}

interface ContentRepository {
    val sourceMode: ContentSourceMode

    suspend fun getBatteryEmojis(): ContentResult<List<BatteryEmoji>>
    suspend fun getToolbarStyles(): ContentResult<List<ToolbarStyle>>
    suspend fun getWallpapers(): ContentResult<List<WallpaperItem>>
    suspend fun getShimejiCharacters(): ContentResult<List<ShimejiCharacter>>
}
