package com.shemiji.emogibattery.data.source

import com.shemiji.emogibattery.R
import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.model.WallpaperItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalContentDataSource @Inject constructor() : ContentDataSource {

    override suspend fun getBatteryEmojis(): List<BatteryEmoji> = listOf(
        BatteryEmoji("battery_angry", "Angry", drawableRes = R.drawable.battery1,isPremium = true),
        BatteryEmoji("angry", "Angry", drawableRes = R.drawable.battery2, isPremium = true),
        BatteryEmoji("beauty", "Angry", drawableRes = R.drawable.battery3),
        BatteryEmoji("battery_happy", "Happy", drawableRes = R.drawable.demo_battery_happy),
        BatteryEmoji("battery_love", "Love", drawableRes = R.drawable.demo_battery_love),
        BatteryEmoji("battery_charge", "Power", drawableRes = R.drawable.demo_battery_power),
        BatteryEmoji("battery_sleep", "Sleepy", drawableRes = R.drawable.demo_battery_sleepy),
    )

    override suspend fun getToolbarStyles(): List<ToolbarStyle> = listOf(
        ToolbarStyle("toolbar_midnight", "Midnight", "#16182B", "#FFFFFF", "#8B8FFF"),
        ToolbarStyle("toolbar_candy", "Candy", "#FFF0F5", "#382B35", "#FF5C93"),
        ToolbarStyle("toolbar_ocean", "Ocean", "#DFF7FF", "#083746", "#00A9CE"),
        ToolbarStyle("toolbar_sunset", "Sunset", "#FFF0DE", "#482513", "#FF7A3D"),
    )

    override suspend fun getWallpapers(): List<WallpaperItem> = listOf(
        WallpaperItem("wallpaper_wall1", "Dreamy Space", "Space", drawableRes = R.drawable.wall1,isPremium = true),
        WallpaperItem("wallpaper_wall2", "Dreamy Space", "Space", drawableRes = R.drawable.wall2, isPremium = true),
        WallpaperItem("wallpaper_wall3", "Dreamy Space", "Space", drawableRes = R.drawable.wall3),
        WallpaperItem("wallpaper_space", "Dreamy Space", "Space", drawableRes = R.drawable.demo_wallpaper_space),
        WallpaperItem("wallpaper_sunset", "Soft Sunset", "Nature", drawableRes = R.drawable.demo_wallpaper_sunset),
        WallpaperItem("wallpaper_ocean", "Blue Waves", "Nature", drawableRes = R.drawable.demo_wallpaper_ocean),
        WallpaperItem("wallpaper_blossom", "Pink Blossom", "Cute", drawableRes = R.drawable.demo_wallpaper_blossom),
    )

    override suspend fun getShimejiCharacters(): List<ShimejiCharacter> = listOf(
        ShimejiCharacter("shimeji_girl", "girl", drawableRes = R.drawable.shemiji1, movementSpeed = 1f, isPremium = true),
        ShimejiCharacter("shimeji_angry_girl", "angry", drawableRes = R.drawable.shemiji3, movementSpeed = 1f),
        ShimejiCharacter("shimeji_beauty", "beauty", drawableRes = R.drawable.shemiji2, movementSpeed = 1f,isPremium = true),
        ShimejiCharacter("shimeji_mochi", "Mochi", drawableRes = R.drawable.demo_shimeji_mochi, movementSpeed = 0.85f),
        ShimejiCharacter("shimeji_bunny", "Bunny", drawableRes = R.drawable.demo_shimeji_bunny, movementSpeed = 1.1f),
        ShimejiCharacter("shimeji_cat", "Milo", drawableRes = R.drawable.demo_shimeji_cat, movementSpeed = 1f),
        ShimejiCharacter("shimeji_bear", "Coco", drawableRes = R.drawable.demo_shimeji_bear, movementSpeed = 0.75f),
    )
}
