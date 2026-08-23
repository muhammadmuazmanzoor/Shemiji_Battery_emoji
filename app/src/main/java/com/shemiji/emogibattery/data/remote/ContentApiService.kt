package com.shemiji.emogibattery.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface ContentApiService {

    // Replace only these paths if the backend uses different endpoint names.
    @GET("v1/battery-emojis")
    suspend fun getBatteryEmojis(): ApiEnvelope<List<BatteryEmojiDto>>

    @GET("v1/toolbar-styles")
    suspend fun getToolbarStyles(): ApiEnvelope<List<ToolbarStyleDto>>

    @GET("v1/wallpapers")
    suspend fun getWallpapers(): ApiEnvelope<List<WallpaperDto>>

    @GET("v1/shimeji-characters")
    suspend fun getShimejiCharacters(): ApiEnvelope<List<ShimejiCharacterDto>>
}

data class ApiEnvelope<T>(
    @SerializedName("success") val success: Boolean = true,
    @SerializedName("message") val message: String? = null,
    @SerializedName("data") val data: T? = null,
)

data class BatteryEmojiDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_premium") val isPremium: Boolean = false,
)

data class ToolbarStyleDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("background_color") val backgroundColor: String,
    @SerializedName("content_color") val contentColor: String,
    @SerializedName("accent_color") val accentColor: String,
    @SerializedName("is_premium") val isPremium: Boolean = false,
)

data class WallpaperDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("category") val category: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("is_premium") val isPremium: Boolean = false,
)

data class ShimejiCharacterDto(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("image_url") val imageUrl: String?,
    @SerializedName("animation_url") val animationUrl: String?,
    @SerializedName("movement_speed") val movementSpeed: Float = 1f,
    @SerializedName("is_premium") val isPremium: Boolean = false,
)
