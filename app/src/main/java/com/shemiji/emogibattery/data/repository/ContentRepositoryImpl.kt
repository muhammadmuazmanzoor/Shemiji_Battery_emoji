package com.shemiji.emogibattery.data.repository

import com.shemiji.emogibattery.data.model.BatteryEmoji
import com.shemiji.emogibattery.data.model.ContentSourceMode
import com.shemiji.emogibattery.data.model.ShimejiCharacter
import com.shemiji.emogibattery.data.model.ToolbarStyle
import com.shemiji.emogibattery.data.model.WallpaperItem
import com.shemiji.emogibattery.data.source.LocalContentDataSource
import com.shemiji.emogibattery.data.source.RemoteContentDataSource
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

class ContentRepositoryImpl @Inject constructor(
    private val localDataSource: LocalContentDataSource,
    private val remoteDataSource: RemoteContentDataSource,
    override val sourceMode: ContentSourceMode,
) : ContentRepository {

    private val activeDataSource
        get() = when (sourceMode) {
            ContentSourceMode.LOCAL_DRAWABLES -> localDataSource
            ContentSourceMode.REMOTE_API -> remoteDataSource
        }

    override suspend fun getBatteryEmojis(): ContentResult<List<BatteryEmoji>> =
        safeRequest("battery emojis") { activeDataSource.getBatteryEmojis() }

    override suspend fun getToolbarStyles(): ContentResult<List<ToolbarStyle>> =
        safeRequest("toolbar styles") { activeDataSource.getToolbarStyles() }

    override suspend fun getWallpapers(): ContentResult<List<WallpaperItem>> =
        safeRequest("wallpapers") { activeDataSource.getWallpapers() }

    override suspend fun getShimejiCharacters(): ContentResult<List<ShimejiCharacter>> =
        safeRequest("Shimeji characters") { activeDataSource.getShimejiCharacters() }

    private suspend fun <T> safeRequest(
        contentName: String,
        request: suspend () -> T,
    ): ContentResult<T> = try {
        ContentResult.Success(request())
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (throwable: Throwable) {
        ContentResult.Error(
            message = throwable.message?.takeIf(String::isNotBlank)
                ?: "Unable to load $contentName. Check your connection and try again.",
            cause = throwable,
        )
    }
}
