package com.shemiji.emogibattery.ui.components

import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

@Composable
fun SpriteSheetPose(
    @DrawableRes drawableRes: Int,
    row: Int,
    column: Int,
    modifier: Modifier = Modifier,
    columns: Int = 4,
    rows: Int = 8,
) {
    val resources = LocalContext.current.resources
    val safeColumn = column.coerceIn(0, columns - 1)
    val safeRow = row.coerceIn(0, rows - 1)
    val frame = remember(resources, drawableRes, safeRow, safeColumn, columns, rows) {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resources.openRawResource(drawableRes).use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }
        val left = safeColumn * bounds.outWidth / columns
        val right = (safeColumn + 1) * bounds.outWidth / columns
        val top = safeRow * bounds.outHeight / rows
        val bottom = (safeRow + 1) * bounds.outHeight / rows
        val decoder = requireNotNull(resources.openRawResource(drawableRes).use { stream ->
            @Suppress("DEPRECATION")
            BitmapRegionDecoder.newInstance(stream, false)
        }) { "Unable to decode sprite sheet resource $drawableRes" }
        try {
            requireNotNull(
                decoder.decodeRegion(
                    Rect(left, top, right, bottom),
                    BitmapFactory.Options().apply {
                        inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    },
                ),
            ) { "Unable to decode sprite frame from resource $drawableRes" }.asImageBitmap()
        } finally {
            @Suppress("DEPRECATION")
            decoder.recycle()
        }
    }

    Canvas(modifier) {
        val scale = minOf(size.width / frame.width, size.height / frame.height)
        val destinationWidth = (frame.width * scale).roundToInt()
        val destinationHeight = (frame.height * scale).roundToInt()
        drawImage(
            image = frame,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(frame.width, frame.height),
            dstOffset = IntOffset(
                ((size.width - destinationWidth) / 2f).roundToInt(),
                ((size.height - destinationHeight) / 2f).roundToInt(),
            ),
            dstSize = IntSize(destinationWidth, destinationHeight),
        )
    }
}
