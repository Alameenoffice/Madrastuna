package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.util.Base64
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

/**
 * Automatic Image Compressor:
 * Any image uploaded from the phone (School Logo, Student Photo, Event Photo)
 * is resized using Canvas to a maximum width of 800px and compressed as JPEG
 * at 0.7 (70%) quality into a Base64 string so it stores cleanly inside the database.
 */
object ImageCompressor {

    private const val MAX_GENERAL_DIMENSION = 800
    private const val MAX_AVATAR_DIMENSION = 400
    private const val JPEG_QUALITY = 75 // 0.75 HD balanced quality (keeps every image under 80KB-150KB)

    private val bitmapCache = object : LruCache<Int, ImageBitmap>(48) {}

    fun compressUriToBase64Jpeg(
        context: Context,
        uri: Uri,
        isAvatar: Boolean = false,
        maxDimensionOverride: Int? = null
    ): String? {
        return try {
            val maxDim = maxDimensionOverride ?: if (isAvatar) MAX_AVATAR_DIMENSION else MAX_GENERAL_DIMENSION

            // 1. First pass: decode bounds only to prevent OOM on high-megapixel camera photos
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOptions)
            }
            val rawW = boundsOptions.outWidth
            val rawH = boundsOptions.outHeight
            if (rawW <= 0 || rawH <= 0) return null

            var sampleSize = 1
            while (rawW / (sampleSize * 2) >= maxDim && rawH / (sampleSize * 2) >= maxDim) {
                sampleSize *= 2
            }

            // 2. Second pass: decode subsampled bitmap safely
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val originalBitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            } ?: return null

            val targetWidth: Int
            val targetHeight: Int
            val maxSide = maxOf(originalBitmap.width, originalBitmap.height)
            if (maxSide > maxDim) {
                val ratio = maxDim.toFloat() / maxSide.toFloat()
                targetWidth = (originalBitmap.width * ratio).roundToInt().coerceAtLeast(1)
                targetHeight = (originalBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            } else {
                targetWidth = originalBitmap.width.coerceAtLeast(1)
                targetHeight = originalBitmap.height.coerceAtLeast(1)
            }

            // 3. Render onto a clean Canvas (matching HTML5 Canvas 800px/400px 0.75 JPEG compression)
            val canvasBitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(canvasBitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
            canvas.drawBitmap(
                originalBitmap,
                Rect(0, 0, originalBitmap.width, originalBitmap.height),
                Rect(0, 0, targetWidth, targetHeight),
                paint
            )

            val outputStream = ByteArrayOutputStream()
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            val jpegBytes = outputStream.toByteArray()
            outputStream.close()

            if (originalBitmap != canvasBitmap) {
                originalBitmap.recycle()
            }
            canvasBitmap.recycle()

            val encoded = Base64.encodeToString(jpegBytes, Base64.NO_WRAP)
            "data:image/jpeg;base64,$encoded"
        } catch (t: Throwable) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(
        base64Data: String?,
        maxDimension: Int = MAX_GENERAL_DIMENSION
    ): ImageBitmap? {
        if (base64Data.isNullOrBlank()) return null
        val safeMaxDim = maxDimension.coerceIn(96, 1200)
        val cacheKey = 31 * base64Data.hashCode() + safeMaxDim
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val cleanBase64 = base64Data.substringAfter("base64,", base64Data).trim()
            if (cleanBase64.isBlank()) return null
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            if (bytes.isEmpty()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)
            val outW = boundsOptions.outWidth
            val outH = boundsOptions.outHeight
            if (outW <= 0 || outH <= 0) return null

            var sampleSize = 1
            val longestSide = maxOf(outW, outH)
            while (longestSide / (sampleSize * 2) >= safeMaxDim) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOptions) ?: return null
            val imageBitmap = bitmap.asImageBitmap()
            bitmapCache.put(cacheKey, imageBitmap)
            imageBitmap
        } catch (t: Throwable) {
            null
        }
    }
}

