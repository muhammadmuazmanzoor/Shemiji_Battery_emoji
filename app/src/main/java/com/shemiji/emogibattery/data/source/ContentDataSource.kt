package com.shemiji.emogibattery.data.source

import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.model.WallpaperItem

interface ContentDataSource {
    suspend fun getBatteryEmojis(): List<BatteryEmoji>
    suspend fun getToolbarStyles(): List<ToolbarStyle>
    suspend fun getWallpapers(): List<WallpaperItem>
    suspend fun getShimejiCharacters(): List<ShimejiCharacter>
}
