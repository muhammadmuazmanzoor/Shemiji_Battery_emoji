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
        ShimejiCharacter("shimeji_01", "Shimeji 01", drawableRes = R.drawable.img_1),
        ShimejiCharacter("shimeji_02", "Shimeji 02", drawableRes = R.drawable.img2),
        ShimejiCharacter("shimeji_03", "Shimeji 03", drawableRes = R.drawable.img3),
        ShimejiCharacter("shimeji_04", "Shimeji 04", drawableRes = R.drawable.img4),
        ShimejiCharacter("shimeji_05", "Shimeji 05", drawableRes = R.drawable.img5),
        ShimejiCharacter("shimeji_06", "Shimeji 06", drawableRes = R.drawable.img6),
        ShimejiCharacter("shimeji_07", "Shimeji 07", drawableRes = R.drawable.img7),
        ShimejiCharacter("shimeji_08", "Shimeji 08", drawableRes = R.drawable.img8),
        ShimejiCharacter("shimeji_09", "Shimeji 09", drawableRes = R.drawable.img9),
        ShimejiCharacter("shimeji_10", "Shimeji 10", drawableRes = R.drawable.img10),
        ShimejiCharacter("shimeji_12", "Shimeji 12", drawableRes = R.drawable.img12),
        ShimejiCharacter("shimeji_13", "Shimeji 13", drawableRes = R.drawable.img13),
        ShimejiCharacter("shimeji_14", "Shimeji 14", drawableRes = R.drawable.img14),
        ShimejiCharacter("shimeji_15", "Shimeji 15", drawableRes = R.drawable.img15),
        ShimejiCharacter("shimeji_17", "Shimeji 17", drawableRes = R.drawable.img17),
        ShimejiCharacter("shimeji_18", "Shimeji 18", drawableRes = R.drawable.img18),
        ShimejiCharacter("shimeji_19", "Shimeji 19", drawableRes = R.drawable.img19),
        ShimejiCharacter("shimeji_21", "Shimeji 21", drawableRes = R.drawable.img21),
        ShimejiCharacter("shimeji_22", "Shimeji 22", drawableRes = R.drawable.img22),
        ShimejiCharacter("shimeji_24", "Shimeji 24", drawableRes = R.drawable.img24),
        ShimejiCharacter("shimeji_25", "Shimeji 25", drawableRes = R.drawable.img25),
        ShimejiCharacter("shimeji_26", "Shimeji 26", drawableRes = R.drawable.img26),
        ShimejiCharacter("shimeji_27", "Shimeji 27", drawableRes = R.drawable.img27),
    )
}
