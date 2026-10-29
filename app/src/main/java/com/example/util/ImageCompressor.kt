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

    private const val MAX_WIDTH = 800
    private const val JPEG_QUALITY = 70 // 0.7 quality

    private val bitmapCache = object : LruCache<Int, ImageBitmap>(32) {}

    fun compressUriToBase64Jpeg(context: Context, uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return null

            val targetWidth: Int
            val targetHeight: Int
            if (originalBitmap.width > MAX_WIDTH) {
                val ratio = MAX_WIDTH.toFloat() / originalBitmap.width.toFloat()
                targetWidth = MAX_WIDTH
                targetHeight = (originalBitmap.height * ratio).roundToInt().coerceAtLeast(1)
            } else {
                targetWidth = originalBitmap.width.coerceAtLeast(1)
                targetHeight = originalBitmap.height.coerceAtLeast(1)
            }

            // Render onto a clean RGB_565 / ARGB_8888 Canvas (matching HTML5 Canvas 800px 0.7 JPEG logic)
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
        } catch (e: Exception) {
            null
        }
    }

    fun decodeBase64ToImageBitmap(base64Data: String): ImageBitmap? {
        if (base64Data.isBlank()) return null
        val cacheKey = base64Data.hashCode()
        bitmapCache.get(cacheKey)?.let { return it }

        return try {
            val cleanBase64 = base64Data.substringAfter("base64,", base64Data)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
            val imageBitmap = bitmap.asImageBitmap()
            bitmapCache.put(cacheKey, imageBitmap)
            imageBitmap
        } catch (e: Exception) {
            null
        }
    }
}
