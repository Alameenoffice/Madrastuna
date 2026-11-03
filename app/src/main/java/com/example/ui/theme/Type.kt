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

val BundledArabicFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Light),
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.cairo_medium, FontWeight.Medium),
    Font(R.font.cairo_medium, FontWeight.SemiBold),
    Font(R.font.cairo_bold, FontWeight.Bold),
    Font(R.font.cairo_extrabold, FontWeight.ExtraBold),
    Font(R.font.cairo_extrabold, FontWeight.Black)
)

private val BaseAppTypography = Typography(
    displayLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 30.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 23.sp,
        lineHeight = 33.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 29.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 15.5.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    titleSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 23.sp,
        letterSpacing = 0.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 13.5.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.sp
    ),
    bodySmall = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.5.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.5.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 10.5.sp,
        lineHeight = 15.5.sp,
        letterSpacing = 0.sp
    )
)

fun buildAppTypography(fontFamily: FontFamily): Typography = Typography(
    displayLarge = BaseAppTypography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = BaseAppTypography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = BaseAppTypography.displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = BaseAppTypography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = BaseAppTypography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = BaseAppTypography.headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = BaseAppTypography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = BaseAppTypography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = BaseAppTypography.titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = BaseAppTypography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = BaseAppTypography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = BaseAppTypography.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = BaseAppTypography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = BaseAppTypography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = BaseAppTypography.labelSmall.copy(fontFamily = fontFamily)
)

/**
 * Smart Arabic Geometric Font Loader (Tajawal -> Almarai -> Cairo Variable -> Bundled Resources).
 * Downloads and caches `.ttf` files in `context.filesDir` on `Dispatchers.IO`, validating
 * `file.exists() && file.length() > 10_000` with `HttpURLConnection.instanceFollowRedirects = true`.
 */
object ArabicFontLoader {
    // Primary Option: Tajawal Geometric Arabic Font (100% live direct links)
    private const val TAJAWAL_REGULAR_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Regular.ttf"
    private const val TAJAWAL_MEDIUM_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Medium.ttf"
    private const val TAJAWAL_BOLD_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-Bold.ttf"
    private const val TAJAWAL_EXTRABOLD_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/tajawal/Tajawal-ExtraBold.ttf"

    // Fallback Option 1: Almarai Geometric Arabic Font
    private const val ALMARAI_REGULAR_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/almarai/Almarai-Regular.ttf"
    private const val ALMARAI_BOLD_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/almarai/Almarai-Bold.ttf"

    // Fallback Option 2: Cairo Variable TTF
    private const val CAIRO_VARIABLE_URL =
        "https://raw.githubusercontent.com/google/fonts/main/ofl/cairo/Cairo%5Bslnt%2Cwght%5D.ttf"

    private const val TAJAWAL_REGULAR_FILE = "Tajawal-Regular.ttf"
    private const val TAJAWAL_MEDIUM_FILE = "Tajawal-Medium.ttf"
    private const val TAJAWAL_BOLD_FILE = "Tajawal-Bold.ttf"
    private const val TAJAWAL_EXTRABOLD_FILE = "Tajawal-ExtraBold.ttf"

    private const val ALMARAI_REGULAR_FILE = "Almarai-Regular.ttf"
    private const val ALMARAI_BOLD_FILE = "Almarai-Bold.ttf"

    private const val CAIRO_VARIABLE_FILE = "Cairo-Variable.ttf"

    var cachedFontFamily: FontFamily by mutableStateOf(BundledArabicFontFamily)
        private set

    fun getInitialFontFamily(context: Context): FontFamily {
        readCachedFromDisk(context)?.let { family ->
            cachedFontFamily = family
            return family
        }
        return cachedFontFamily
    }

    private fun isValidFontFile(file: File): Boolean {
        return file.exists() && file.length() > 10_000L
    }

    private fun readCachedFromDisk(context: Context): FontFamily? {
        val dir = context.filesDir
        val tajReg = File(dir, TAJAWAL_REGULAR_FILE)
        val tajMed = File(dir, TAJAWAL_MEDIUM_FILE)
        val tajBold = File(dir, TAJAWAL_BOLD_FILE)
        val tajExtra = File(dir, TAJAWAL_EXTRABOLD_FILE)

        if (isValidFontFile(tajReg) && isValidFontFile(tajBold)) {
            val medFile = if (isValidFontFile(tajMed)) tajMed else tajReg
            val extraFile = if (isValidFontFile(tajExtra)) tajExtra else tajBold
            return runCatching {
                FontFamily(
                    Font(file = tajReg, weight = FontWeight.Light),
                    Font(file = tajReg, weight = FontWeight.Normal),
                    Font(file = medFile, weight = FontWeight.Medium),
                    Font(file = medFile, weight = FontWeight.SemiBold),
                    Font(file = tajBold, weight = FontWeight.Bold),
                    Font(file = extraFile, weight = FontWeight.ExtraBold),
                    Font(file = extraFile, weight = FontWeight.Black)
                )
            }.getOrNull()
        }

        val almReg = File(dir, ALMARAI_REGULAR_FILE)
        val almBold = File(dir, ALMARAI_BOLD_FILE)
        if (isValidFontFile(almReg) && isValidFontFile(almBold)) {
            return runCatching {
                FontFamily(
                    Font(file = almReg, weight = FontWeight.Light),
                    Font(file = almReg, weight = FontWeight.Normal),
                    Font(file = almReg, weight = FontWeight.Medium),
                    Font(file = almBold, weight = FontWeight.SemiBold),
                    Font(file = almBold, weight = FontWeight.Bold),
                    Font(file = almBold, weight = FontWeight.ExtraBold),
                    Font(file = almBold, weight = FontWeight.Black)
                )
            }.getOrNull()
        }

        val cairoVar = File(dir, CAIRO_VARIABLE_FILE)
        if (isValidFontFile(cairoVar)) {
            return runCatching {
                FontFamily(
                    Font(file = cairoVar, weight = FontWeight.Light),
                    Font(file = cairoVar, weight = FontWeight.Normal),
                    Font(file = cairoVar, weight = FontWeight.Medium),
                    Font(file = cairoVar, weight = FontWeight.SemiBold),
                    Font(file = cairoVar, weight = FontWeight.Bold),
                    Font(file = cairoVar, weight = FontWeight.ExtraBold),
                    Font(file = cairoVar, weight = FontWeight.Black)
                )
            }.getOrNull()
        }

        return null
    }

    suspend fun loadArabicFontFamily(context: Context): FontFamily {
        readCachedFromDisk(context)?.let { family ->
            cachedFontFamily = family
            return family
        }

        val loaded = withContext(Dispatchers.IO) {
            val dir = context.filesDir

            // 1. Primary Option: Tajawal (Regular, Medium, Bold, ExtraBold)
            val tajReg = File(dir, TAJAWAL_REGULAR_FILE)
            val tajMed = File(dir, TAJAWAL_MEDIUM_FILE)
            val tajBold = File(dir, TAJAWAL_BOLD_FILE)
            val tajExtra = File(dir, TAJAWAL_EXTRABOLD_FILE)

            val regOk = downloadFontFile(context, tajReg, TAJAWAL_REGULAR_URL)
            val medOk = downloadFontFile(context, tajMed, TAJAWAL_MEDIUM_URL)
            val boldOk = downloadFontFile(context, tajBold, TAJAWAL_BOLD_URL)
            val extraOk = downloadFontFile(context, tajExtra, TAJAWAL_EXTRABOLD_URL)

            if (regOk && boldOk) {
                val medFile = if (medOk && isValidFontFile(tajMed)) tajMed else tajReg
                val extraFile = if (extraOk && isValidFontFile(tajExtra)) tajExtra else tajBold
                val tajFamily = runCatching {
                    FontFamily(
                        Font(file = tajReg, weight = FontWeight.Light),
                        Font(file = tajReg, weight = FontWeight.Normal),
                        Font(file = medFile, weight = FontWeight.Medium),
                        Font(file = medFile, weight = FontWeight.SemiBold),
                        Font(file = tajBold, weight = FontWeight.Bold),
                        Font(file = extraFile, weight = FontWeight.ExtraBold),
                        Font(file = extraFile, weight = FontWeight.Black)
                    )
                }.getOrNull()
                if (tajFamily != null) return@withContext tajFamily
            }

            // 2. Fallback Option A: Almarai (Regular + Bold)
            val almReg = File(dir, ALMARAI_REGULAR_FILE)
            val almBold = File(dir, ALMARAI_BOLD_FILE)
            val almRegOk = downloadFontFile(context, almReg, ALMARAI_REGULAR_URL)
            val almBoldOk = downloadFontFile(context, almBold, ALMARAI_BOLD_URL)
            if (almRegOk && almBoldOk) {
                val almFamily = runCatching {
                    FontFamily(
                        Font(file = almReg, weight = FontWeight.Light),
                        Font(file = almReg, weight = FontWeight.Normal),
                        Font(file = almReg, weight = FontWeight.Medium),
                        Font(file = almBold, weight = FontWeight.SemiBold),
                        Font(file = almBold, weight = FontWeight.Bold),
                        Font(file = almBold, weight = FontWeight.ExtraBold),
                        Font(file = almBold, weight = FontWeight.Black)
                    )
                }.getOrNull()
                if (almFamily != null) return@withContext almFamily
            }

            // 3. Fallback Option B: Cairo Variable TTF
            val cairoVar = File(dir, CAIRO_VARIABLE_FILE)
            if (downloadFontFile(context, cairoVar, CAIRO_VARIABLE_URL)) {
                val cairoFamily = runCatching {
                    FontFamily(
                        Font(file = cairoVar, weight = FontWeight.Light),
                        Font(file = cairoVar, weight = FontWeight.Normal),
                        Font(file = cairoVar, weight = FontWeight.Medium),
                        Font(file = cairoVar, weight = FontWeight.SemiBold),
                        Font(file = cairoVar, weight = FontWeight.Bold),
                        Font(file = cairoVar, weight = FontWeight.ExtraBold),
                        Font(file = cairoVar, weight = FontWeight.Black)
                    )
                }.getOrNull()
                if (cairoFamily != null) return@withContext cairoFamily
            }

            // 4. Offline Bundled Fallback: copy bundled res/font TTFs into filesDir and build FontFamily
            copyBundledResToFile(context, R.font.tajawal, tajReg)
            copyBundledResToFile(context, R.font.cairo_medium, tajMed)
            copyBundledResToFile(context, R.font.cairo_bold, tajBold)
            copyBundledResToFile(context, R.font.cairo_extrabold, tajExtra)
            if (isValidFontFile(tajReg) && isValidFontFile(tajBold)) {
                runCatching {
                    FontFamily(
                        Font(file = tajReg, weight = FontWeight.Light),
                        Font(file = tajReg, weight = FontWeight.Normal),
                        Font(file = if (isValidFontFile(tajMed)) tajMed else tajReg, weight = FontWeight.Medium),
                        Font(file = if (isValidFontFile(tajMed)) tajMed else tajReg, weight = FontWeight.SemiBold),
                        Font(file = tajBold, weight = FontWeight.Bold),
                        Font(file = if (isValidFontFile(tajExtra)) tajExtra else tajBold, weight = FontWeight.ExtraBold),
                        Font(file = if (isValidFontFile(tajExtra)) tajExtra else tajBold, weight = FontWeight.Black)
                    )
                }.getOrNull()
            } else {
                BundledArabicFontFamily
            }
        } ?: BundledArabicFontFamily

        cachedFontFamily = loaded
        return loaded
    }

    private fun downloadFontFile(context: Context, targetFile: File, urlStr: String): Boolean {
        if (isValidFontFile(targetFile)) return true
        return runCatching {
            val tempFile = File(context.filesDir, "${targetFile.name}.tmp")
            val conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 8000
                instanceFollowRedirects = true
                requestMethod = "GET"
            }
            if (conn.responseCode in 200..299) {
                conn.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }
            conn.disconnect()
            if (tempFile.exists() && tempFile.length() > 10_000L) {
                if (targetFile.exists()) targetFile.delete()
                tempFile.renameTo(targetFile)
                isValidFontFile(targetFile)
            } else {
                tempFile.delete()
                false
            }
        }.getOrDefault(false)
    }

    private fun copyBundledResToFile(context: Context, resId: Int, targetFile: File): Boolean {
        if (isValidFontFile(targetFile)) return true
        return runCatching {
            context.resources.openRawResource(resId).use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
            isValidFontFile(targetFile)
        }.getOrDefault(false)
    }
}

val Typography: Typography
    get() = buildAppTypography(ArabicFontLoader.cachedFontFamily)
