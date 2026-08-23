package com.shemiji.emogibattery.system

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.appcompat.content.res.AppCompatResources
import com.shemiji.emogibattery.data.model.WallpaperItem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WallpaperApplier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
) {
    suspend fun apply(wallpaper: WallpaperItem): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val wallpaperManager = WallpaperManager.getInstance(context)
            
            // 1. Determine the EXACT dimensions the system launcher expects
            // Providing exactly what it asks for prevents "rounded" or "padded" artifacts.
            var targetWidth = wallpaperManager.desiredMinimumWidth
            var targetHeight = wallpaperManager.desiredMinimumHeight
            
            if (targetWidth <= 0 || targetHeight <= 0) {
                val metrics = context.resources.displayMetrics
                targetWidth = metrics.widthPixels
                targetHeight = metrics.heightPixels
            }

            // 2. Load the source image
            val sourceBitmap = when {
                !wallpaper.imageUrl.isNullOrBlank() -> loadRemoteBitmap(wallpaper.imageUrl)
                wallpaper.drawableRes != null -> renderDrawableToBitmap(wallpaper.drawableRes)
                else -> error("This wallpaper does not contain a usable image")
            }

            // 3. Scale and Crop the image to fit the target dimensions perfectly
            val finalBitmap = scaleToFit(sourceBitmap, targetWidth, targetHeight)

            try {
                // 4. Set the bitmap. This single call is usually most reliable for full coverage.
                wallpaperManager.setBitmap(finalBitmap)
            } finally {
                if (finalBitmap != sourceBitmap) {
                    finalBitmap.recycle()
                }
                sourceBitmap.recycle()
            }
            Unit
        }
    }

    private fun loadRemoteBitmap(url: String): Bitmap {
        val request = Request.Builder().url(url).get().build()
        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("Wallpaper download failed (${response.code})")
            val bytes = response.body?.bytes() ?: error("Wallpaper response was empty")
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                ?: error("Wallpaper image could not be decoded")
        }
    }

    private fun renderDrawableToBitmap(drawableRes: Int): Bitmap {
        val drawable = AppCompatResources.getDrawable(context, drawableRes)
            ?: error("Wallpaper drawable was not found")
        
        // Render at its intrinsic size first to preserve quality
        val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1080
        val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1920
        
        return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.BLACK) // Ensure no transparency
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
        }
    }

    private fun scaleToFit(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val sourceWidth = source.width
        val sourceHeight = source.height

        // Calculate scale to fill the entire target area (center crop style)
        val scale = Math.max(
            targetWidth.toFloat() / sourceWidth,
            targetHeight.toFloat() / sourceHeight
        )

        val scaledWidth = (scale * sourceWidth).toInt()
        val scaledHeight = (scale * sourceHeight).toInt()

        val left = (targetWidth - scaledWidth) / 2
        val top = (targetHeight - scaledHeight) / 2

        val result = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        canvas.drawColor(Color.BLACK)
        
        val paint = Paint().apply {
            isFilterBitmap = true
            isAntiAlias = true
        }
        
        val destRect = android.graphics.Rect(left, top, left + scaledWidth, top + scaledHeight)
        canvas.drawBitmap(source, null, destRect, paint)
        
        return result
    }
}
