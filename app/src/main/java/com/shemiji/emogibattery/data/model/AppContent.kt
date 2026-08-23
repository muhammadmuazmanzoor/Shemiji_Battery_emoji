package com.shemiji.emogibattery.data.model

import androidx.annotation.DrawableRes

enum class ContentSourceMode {
    LOCAL_DRAWABLES,
    REMOTE_API,
}

data class BatteryEmoji(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val isPremium: Boolean = false,
)

data class ToolbarStyle(
    val id: String,
    val name: String,
    val backgroundColor: String,
    val contentColor: String,
    val accentColor: String,
    val isPremium: Boolean = false,
)

data class WallpaperItem(
    val id: String,
    val name: String,
    val category: String,
    val imageUrl: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val isPremium: Boolean = false,
)

data class ShimejiCharacter(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val animationUrl: String? = null,
    @DrawableRes val drawableRes: Int? = null,
    val movementSpeed: Float = 1f,
    val isPremium: Boolean = false,
)

fun BatteryEmoji.imageModel(): Any? = imageUrl?.takeIf(String::isNotBlank) ?: drawableRes

fun WallpaperItem.imageModel(): Any? = imageUrl?.takeIf(String::isNotBlank) ?: drawableRes

fun ShimejiCharacter.imageModel(): Any? =
    animationUrl?.takeIf(String::isNotBlank)
        ?: imageUrl?.takeIf(String::isNotBlank)
        ?: drawableRes
