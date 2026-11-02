package com.example.ui.theme

import android.content.Context
import androidx.compose.material3.Typography
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

val BundledCairoFontFamily = FontFamily(
    Font(R.font.cairo_regular, FontWeight.Light),
    Font(R.font.cairo_regular, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_medium, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
    Font(R.font.cairo_extrabold, FontWeight.Black)
)

fun buildCairoTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 15.5.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 15.5.sp,
        letterSpacing = 0.sp
    )
)

/**
 * Smart Cairo TTF Manager:
 * Ensures `Cairo-Regular.ttf` and `Cairo-Bold.ttf` exist in `context.filesDir`,
 * downloading them from the official Google Fonts GitHub repository if not yet cached
 * (with fallback to bundled `res/font` resources), and builds a real `FontFamily` using
 * `androidx.compose.ui.text.font.Font(file = ...)`.
 */
object CairoFontManager {
    private const val CAIRO_REGULAR_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/static/Cairo-Regular.ttf"
    private const val CAIRO_BOLD_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/static/Cairo-Bold.ttf"

    private const val REGULAR_FILE_NAME = "Cairo-Regular.ttf"
    private const val BOLD_FILE_NAME = "Cairo-Bold.ttf"

    var currentFontFamily: FontFamily by mutableStateOf(BundledCairoFontFamily)
        private set

    var currentTypography: Typography by mutableStateOf(buildCairoTypography(BundledCairoFontFamily))
        private set

    @Volatile
    private var initializedFromDisk = false

    fun initSynchronouslyIfCached(context: Context) {
        if (initializedFromDisk) return
        val regularFile = File(context.filesDir, REGULAR_FILE_NAME)
        val boldFile = File(context.filesDir, BOLD_FILE_NAME)
        if (regularFile.exists() && regularFile.length() > 10_000L &&
            boldFile.exists() && boldFile.length() > 10_000L
        ) {
            applyFontFiles(regularFile, boldFile)
            initializedFromDisk = true
        }
    }

    suspend fun ensureCairoFontsLoaded(context: Context) {
        initSynchronouslyIfCached(context)
        if (initializedFromDisk) return

        val regularFile = File(context.filesDir, REGULAR_FILE_NAME)
        val boldFile = File(context.filesDir, BOLD_FILE_NAME)

        val ready = withContext(Dispatchers.IO) {
            val regOk = ensureSingleFontFile(
                context = context,
                targetFile = regularFile,
                downloadUrl = CAIRO_REGULAR_URL,
                fallbackRawResId = R.font.cairo_regular
            )
            val boldOk = ensureSingleFontFile(
                context = context,
                targetFile = boldFile,
                downloadUrl = CAIRO_BOLD_URL,
                fallbackRawResId = R.font.cairo_bold
            )
            regOk && boldOk
        }

        if (ready && regularFile.exists() && boldFile.exists()) {
            applyFontFiles(regularFile, boldFile)
            initializedFromDisk = true
        }
    }

    private fun applyFontFiles(regularFile: File, boldFile: File) {
        runCatching {
            val family = FontFamily(
                androidx.compose.ui.text.font.Font(file = regularFile, weight = FontWeight.Light),
                androidx.compose.ui.text.font.Font(file = regularFile, weight = FontWeight.Normal),
                androidx.compose.ui.text.font.Font(file = regularFile, weight = FontWeight.Medium),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.SemiBold),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.Bold),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.ExtraBold),
                androidx.compose.ui.text.font.Font(file = boldFile, weight = FontWeight.Black)
            )
            currentFontFamily = family
            currentTypography = buildCairoTypography(family)
        }
    }

    private fun ensureSingleFontFile(
        context: Context,
        targetFile: File,
        downloadUrl: String,
        fallbackRawResId: Int
    ): Boolean {
        if (targetFile.exists() && targetFile.length() > 10_000L) {
            return true
        }
        // 1. Download from official Google Fonts static TTF URL
        val downloaded = runCatching {
            val tempFile = File(context.filesDir, "${targetFile.name}.tmp")
            val connection = (URL(downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            if (tempFile.exists() && tempFile.length() > 10_000L) {
                tempFile.renameTo(targetFile)
                true
            } else {
                tempFile.delete()
                false
            }
        }.getOrDefault(false)

        if (downloaded && targetFile.exists() && targetFile.length() > 10_000L) {
            return true
        }

        // 2. Fallback to bundled res/font resource if device is offline on first run
        return runCatching {
            context.resources.openRawResource(fallbackRawResId).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.exists() && targetFile.length() > 10_000L
        }.getOrDefault(false)
    }
}

val CairoFontFamily: FontFamily
    get() = CairoFontManager.currentFontFamily

val Typography: Typography
    get() = CairoFontManager.currentTypography

