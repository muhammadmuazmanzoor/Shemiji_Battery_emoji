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
        BatteryEmoji("battery_fury", "Fury", drawableRes = R.drawable.battery1, isPremium = true),
        BatteryEmoji("battery_rage", "Rage", drawableRes = R.drawable.battery2, isPremium = true),
        BatteryEmoji("battery_glam", "Glam", drawableRes = R.drawable.battery3),

        BatteryEmoji("battery_happy", "Happy", drawableRes = R.drawable.demo_battery_happy),
        BatteryEmoji("battery_love", "Love", drawableRes = R.drawable.demo_battery_love),

        BatteryEmoji("battery_power", "Power", drawableRes = R.drawable.battery_01),
        BatteryEmoji("battery_sleepy", "Sleepy", drawableRes = R.drawable.battery_02),
        BatteryEmoji("battery_smile", "Smile", drawableRes = R.drawable.battery_03),
        BatteryEmoji("battery_cute", "Cute", drawableRes = R.drawable.battery_04),
        BatteryEmoji("battery_cool", "Cool", drawableRes = R.drawable.battery_05),
        BatteryEmoji("battery_wink", "Wink", drawableRes = R.drawable.battery_06),
        BatteryEmoji("battery_joy", "Joy", drawableRes = R.drawable.battery_07),
        BatteryEmoji("battery_blush", "Blush", drawableRes = R.drawable.battery_08),
        BatteryEmoji("battery_star", "Star", drawableRes = R.drawable.battery_09),
        BatteryEmoji("battery_dream", "Dream", drawableRes = R.drawable.battery_10),
        BatteryEmoji("battery_spark", "Spark", drawableRes = R.drawable.battery_11),
        BatteryEmoji("battery_magic", "Magic", drawableRes = R.drawable.battery_12),
        BatteryEmoji("battery_sweet", "Sweet", drawableRes = R.drawable.battery_13),
        BatteryEmoji("battery_chill", "Chill", drawableRes = R.drawable.battery_14),
        BatteryEmoji("battery_party", "Party", drawableRes = R.drawable.battery_15),
        BatteryEmoji("battery_flame", "Flame", drawableRes = R.drawable.battery_16),
        BatteryEmoji("battery_ghost", "Ghost", drawableRes = R.drawable.battery_17),
        BatteryEmoji("battery_panda", "Panda", drawableRes = R.drawable.battery_18),
        BatteryEmoji("battery_bunny", "Bunny", drawableRes = R.drawable.battery_19),
        BatteryEmoji("battery_kitty", "Kitty", drawableRes = R.drawable.battery_20),
        BatteryEmoji("battery_bear", "Bear", drawableRes = R.drawable.battery_21),
        BatteryEmoji("battery_angel", "Angel", drawableRes = R.drawable.battery_22),
        BatteryEmoji("battery_devil", "Devil", drawableRes = R.drawable.battery_23),
        BatteryEmoji("battery_heart", "Heart", drawableRes = R.drawable.battery_24),
        BatteryEmoji("battery_neon", "Neon", drawableRes = R.drawable.battery_25),
    )

    override suspend fun getToolbarStyles(): List<ToolbarStyle> = listOf(
        ToolbarStyle("toolbar_midnight", "Midnight", "#16182B", "#FFFFFF", "#8B8FFF"),
        ToolbarStyle("toolbar_candy", "Candy", "#FFF0F5", "#382B35", "#FF5C93"),
        ToolbarStyle("toolbar_ocean", "Ocean", "#DFF7FF", "#083746", "#00A9CE"),
        ToolbarStyle("toolbar_sunset", "Sunset", "#FFF0DE", "#482513", "#FF7A3D"),
    )

    override suspend fun getWallpapers(): List<WallpaperItem> = listOf(
        WallpaperItem("wallpaper_wall1", "Dreamy Space", "Space", drawableRes = R.drawable.img_2,isPremium = true),
        WallpaperItem("wallpaper_wall2", "Dreamy Space", "Space", drawableRes = R.drawable.img_3,isPremium = true),
        WallpaperItem("wallpaper_wal43", "Dreamy Space", "Space", drawableRes = R.drawable.img_4,isPremium = true),
        WallpaperItem("wallpaper_wall5", "Dreamy Space", "Space", drawableRes = R.drawable.img_5,isPremium = true),
        WallpaperItem("wallpaper_wall6", "Dreamy Space", "Space", drawableRes = R.drawable.img_6,isPremium = true),
        WallpaperItem("wallpaper_wall7", "Dreamy Space", "Space", drawableRes = R.drawable.img_7,isPremium = true),
        WallpaperItem("wallpaper_wall8", "Dreamy Space", "Space", drawableRes = R.drawable.img_8,isPremium = true),
        WallpaperItem("wallpaper_wall9", "Dreamy Space", "Space", drawableRes = R.drawable.img_9,isPremium = true),
        WallpaperItem("wallpaper_wall10", "Dreamy Space", "Space", drawableRes = R.drawable.wall1,isPremium = true),
        WallpaperItem("wallpaper_wall2", "Dreamy Space", "Space", drawableRes = R.drawable.wall2, isPremium = true),
        WallpaperItem("wallpaper_wall3", "Dreamy Space", "Space", drawableRes = R.drawable.wall3),
        WallpaperItem("wallpaper_space", "Dreamy Space", "Space", drawableRes = R.drawable.demo_wallpaper_space),
        WallpaperItem("wallpaper_sunset", "Soft Sunset", "Nature", drawableRes = R.drawable.demo_wallpaper_sunset),
        WallpaperItem("wallpaper_ocean", "Blue Waves", "Nature", drawableRes = R.drawable.demo_wallpaper_ocean),
        WallpaperItem("wallpaper_blossom", "Pink Blossom", "Cute", drawableRes = R.drawable.demo_wallpaper_blossom),
    )

    override suspend fun getShimejiCharacters(): List<ShimejiCharacter> = listOf(
        ShimejiCharacter("shimeji_02", "Pikachu", drawableRes = R.drawable.img2),
        ShimejiCharacter("shimeji_03", "Kitty", drawableRes = R.drawable.img3),
        ShimejiCharacter("shimeji_05", "Doraemon", drawableRes = R.drawable.img5 ,isPremium = true),
        ShimejiCharacter("shimeji_06", "Sonic", drawableRes = R.drawable.img6),
        ShimejiCharacter("shimeji_08", "Goku", drawableRes = R.drawable.img8 ,isPremium = true),
        ShimejiCharacter("shimeji_10", "Gojo", drawableRes = R.drawable.img10),

        ShimejiCharacter("shimeji_12", "Totoro", drawableRes = R.drawable.img12 ,isPremium = true),
        ShimejiCharacter("shimeji_13", "Kuromi", drawableRes = R.drawable.img13),
        ShimejiCharacter("shimeji_15", "Cinnamoroll", drawableRes = R.drawable.img15),

        ShimejiCharacter("shimeji_17", "Tom", drawableRes = R.drawable.img17),
        ShimejiCharacter("shimeji_18", "Jerry", drawableRes = R.drawable.img18 ,isPremium = true),
        ShimejiCharacter("shimeji_19", "Garfield", drawableRes = R.drawable.img19),

        ShimejiCharacter("shimeji_21", "Shinchan", drawableRes = R.drawable.img21),
        ShimejiCharacter("shimeji_22", "Scooby", drawableRes = R.drawable.img22 ,isPremium = true),

        ShimejiCharacter("shimeji_24", "Batman", drawableRes = R.drawable.img24 ,isPremium = true),
        ShimejiCharacter("shimeji_25", "Deadpool", drawableRes = R.drawable.img25 ,isPremium = true),
        ShimejiCharacter("shimeji_26", "Man", drawableRes = R.drawable.img26),
        ShimejiCharacter("shimeji_27", "Hulk", drawableRes = R.drawable.img27 ,isPremium = true),
        ShimejiCharacter("shimeji_28", "Venom", drawableRes = R.drawable.img28),
        ShimejiCharacter("shimeji_29", "Minion", drawableRes = R.drawable.img29),
        ShimejiCharacter("shimeji_30", "SpongeBob", drawableRes = R.drawable.img30),
        ShimejiCharacter("shimeji_31", "Patrick Star", drawableRes = R.drawable.img31),
        ShimejiCharacter("shimeji_33", "Cute", drawableRes = R.drawable.img33),
        ShimejiCharacter("shimeji_34", "Puppy", drawableRes = R.drawable.img34),
        ShimejiCharacter("shimeji_35", "Baby", drawableRes = R.drawable.img35 ,isPremium = true),
        ShimejiCharacter("shimeji_36", "Baby", drawableRes = R.drawable.img36 ,isPremium = true),
        ShimejiCharacter("shimeji_37", "Baby", drawableRes = R.drawable.img37 ,isPremium = true),
    )
}
