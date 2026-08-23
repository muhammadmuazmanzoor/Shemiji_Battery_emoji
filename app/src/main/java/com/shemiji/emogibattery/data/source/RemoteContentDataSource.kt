package com.shemiji.emogibattery.data.source

import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.model.WallpaperItem
import com.shemiji.emogibattery.data.remote.ApiEnvelope
import com.shemiji.emogibattery.data.remote.ContentApiService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemoteContentDataSource @Inject constructor(
    private val apiService: ContentApiService,
) : ContentDataSource {

    override suspend fun getBatteryEmojis(): List<BatteryEmoji> =
        apiService.getBatteryEmojis().requireData().map { dto ->
            BatteryEmoji(
                id = dto.id,
                name = dto.name,
                imageUrl = dto.imageUrl,
                isPremium = dto.isPremium,
            )
        }

    override suspend fun getToolbarStyles(): List<ToolbarStyle> =
        apiService.getToolbarStyles().requireData().map { dto ->
            ToolbarStyle(
                id = dto.id,
                name = dto.name,
                backgroundColor = dto.backgroundColor,
                contentColor = dto.contentColor,
                accentColor = dto.accentColor,
                isPremium = dto.isPremium,
            )
        }

    override suspend fun getWallpapers(): List<WallpaperItem> =
        apiService.getWallpapers().requireData().map { dto ->
            WallpaperItem(
                id = dto.id,
                name = dto.name,
                category = dto.category,
                imageUrl = dto.imageUrl,
                isPremium = dto.isPremium,
            )
        }

    override suspend fun getShimejiCharacters(): List<ShimejiCharacter> =
        apiService.getShimejiCharacters().requireData().map { dto ->
            ShimejiCharacter(
                id = dto.id,
                name = dto.name,
                imageUrl = dto.imageUrl,
                animationUrl = dto.animationUrl,
                movementSpeed = dto.movementSpeed.coerceIn(0.25f, 3f),
                isPremium = dto.isPremium,
            )
        }

    private fun <T> ApiEnvelope<T>.requireData(): T {
        if (!success) throw IllegalStateException(message ?: "The server rejected the request")
        return data ?: throw IllegalStateException(message ?: "The server returned no data")
    }
}
