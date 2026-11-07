package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Base64
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.ThreeDIllustrationType
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

private const val NATIVE_CURRICULUM_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"
private const val MIN_VALID_BOOK_BYTES = 50_000L // 50 KB minimum

/**
 * 1) تنظيف وتوحيد الرابط قبل أي عملية:
 * `val cleanUrl = rawUrl.trim().replaceFirst("^http://".toRegex(RegexOption.IGNORE_CASE), "https://").replaceFirst("^Https://".toRegex(), "https://")`
 */
private fun normalizeCurriculumPdfUrl(rawUrl: String): String {
    return rawUrl
        .trim()
        .replaceFirst("^http://".toRegex(RegexOption.IGNORE_CASE), "https://")
        .replaceFirst("^Https://".toRegex(), "https://")
}

/**
 * التحقق من البصمة السحرية لملف الـ PDF (`%PDF` -> `0x25, 0x50, 0x44, 0x46`)
 * لضمان عدم حفظ صفحة HTML أو رد خطأ من Cloudflare كملف كتاب.
 */
private fun hasPdfMagicSignature(file: File): Boolean {
    if (!file.exists() || file.length() <= MIN_VALID_BOOK_BYTES) return false
    return runCatching {
        FileInputStream(file).use { fis ->
            val header = ByteArray(1024)
            val read = fis.read(header)
            if (read < 4) return@use false
            if (header[0] == 0x25.toByte() &&
                header[1] == 0x50.toByte() &&
                header[2] == 0x44.toByte() &&
                header[3] == 0x46.toByte()
            ) {
                return@use true
            }
            for (i in 0..(read - 4)) {
                if (header[i] == 0x25.toByte() &&
                    header[i + 1] == 0x50.toByte() &&
                    header[i + 2] == 0x44.toByte() &&
                    header[i + 3] == 0x46.toByte()
                ) {
                    return@use true
                }
            }
            false
        }
    }.getOrDefault(false)
}

/**
 * Checks whether a local book file exists in `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)/curriculum_${bookId}.pdf`,
 * its size is greater than 50,000 bytes, and its header contains the real `%PDF` magic bytes.
 */
private fun isValidDownloadedCurriculumPdf(file: File): Boolean {
    return file.exists() && file.length() > MIN_VALID_BOOK_BYTES && hasPdfMagicSignature(file)
}

/**
 * Copies the already-downloaded in-app PDF file directly into the phone's public `Downloads` folder
 * without opening any external browser or leaving the app.
 */
private suspend fun saveBookFileToPublicDownloads(
    context: Context,
    sourceFile: File,
    book: CurriculumBookItem
): Boolean = withContext(Dispatchers.IO) {
    if (!isValidDownloadedCurriculumPdf(sourceFile)) return@withContext false
    val safeSubject = book.subjectTitle
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(" ", "_")
    val safeGrade = book.gradeClass
        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
        .replace(" ", "_")
    val fileName = "كتاب_${safeSubject}_${safeGrade}.pdf"

    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@runCatching false
            resolver.openOutputStream(uri)?.use { outStream ->
                FileInputStream(sourceFile).use { inStream ->
                    inStream.copyTo(outStream, bufferSize = 16 * 1024)
                    outStream.flush()
                }
            } ?: return@runCatching false

            val completeValues = ContentValues().apply {
                put(MediaStore.Downloads.IS_PENDING, 0)
            }
            resolver.update(uri, completeValues, null, null)
            true
        } else {
            @Suppress("DEPRECATION")
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val destFile = File(downloadsDir, fileName)
            FileInputStream(sourceFile).use { inStream ->
                FileOutputStream(destFile).use { outStream ->
                    inStream.copyTo(outStream, bufferSize = 16 * 1024)
                    outStream.flush()
                }
            }
            true
        }
    }.getOrDefault(false)
}

enum class CurriculumStageTab(
    val arabicTitle: String,
    val shortBadge: String,
    val classes: List<String>
) {
    PRIMARY(
        arabicTitle = "المرحلة الابتدائية",
        shortBadge = "ابتدائي",
        classes = listOf(
            "الأول الابتدائي",
            "الثاني الابتدائي",
            "الثالث الابتدائي",
            "الرابع الابتدائي",
            "الخامس الابتدائي",
            "السادس الابتدائي"
        )
    ),
    MIDDLE(
        arabicTitle = "المرحلة المتوسطة",
        shortBadge = "متوسط",
        classes = listOf(
            "الأول متوسط",
            "الثاني متوسط",
            "الثالث متوسط"
        )
    ),
    PREPARATORY(
        arabicTitle = "المرحلة الإعدادية",
        shortBadge = "إعدادي",
        classes = listOf(
            "الرابع العلمي",
            "الرابع الأدبي",
            "الخامس العلمي",
            "الخامس الأدبي",
            "السادس العلمي",
            "السادس الأدبي"
        )
    )
}

data class CurriculumBookItem(
    val id: String,
    val subjectTitle: String,
    val stage: CurriculumStageTab,
    val gradeClass: String,
    val pdfUrl: String
) {
    val normalizedPdfUrl: String
        get() = normalizeCurriculumPdfUrl(pdfUrl)
}

/**
 * الفهرس النصي الرسمي الكامل لروابط المناهج العراقية (Strict On-Demand Only - Native In-App Stream & PdfRenderer).
 */
object IraqiCurriculumCatalog {

    val allBooks: List<CurriculumBookItem> = listOf(
        // =========================================================================
        // أولاً: المرحلة الابتدائية
        // =========================================================================
        // 1) الصف الأول الابتدائي
        CurriculumBookItem("pri_1_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1881.pdf"),
        CurriculumBookItem("pri_1_reading", "القراءة العربية (قراءتي)", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1880.pdf"),
        CurriculumBookItem("pri_1_math", "الرياضيات", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1882.pdf"),
        CurriculumBookItem("pri_1_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1878.pdf"),
        CurriculumBookItem("pri_1_science", "العلوم", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1879.pdf"),
        CurriculumBookItem("pri_1_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1877.pdf"),
        CurriculumBookItem("pri_1_ethics", "التربية الأخلاقية", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1876.pdf"),

        // 2) الصف الثاني الابتدائي
        CurriculumBookItem("pri_2_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1872.pdf"),
        CurriculumBookItem("pri_2_reading", "القراءة العربية (قراءتي)", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1871.pdf"),
        CurriculumBookItem("pri_2_math", "الرياضيات", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1874.pdf"),
        CurriculumBookItem("pri_2_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1873.pdf"),
        CurriculumBookItem("pri_2_science", "العلوم", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1869.pdf"),
        CurriculumBookItem("pri_2_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1875.pdf"),
        CurriculumBookItem("pri_2_ethics", "التربية الأخلاقية", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1870.pdf"),

        // 3) الصف الثالث الابتدائي
        CurriculumBookItem("pri_3_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1867.pdf"),
        CurriculumBookItem("pri_3_reading", "القراءة العربية (قراءتي)", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1866.pdf"),
        CurriculumBookItem("pri_3_math", "الرياضيات", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1864.pdf"),
        CurriculumBookItem("pri_3_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1863.pdf"),
        CurriculumBookItem("pri_3_science", "العلوم", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1865.pdf"),
        CurriculumBookItem("pri_3_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1868.pdf"),

        // 4) الصف الرابع الابتدائي
        CurriculumBookItem("pri_4_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1857.pdf"),
        CurriculumBookItem("pri_4_reading", "القراءة العربية", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1860.pdf"),
        CurriculumBookItem("pri_4_math", "الرياضيات", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1914.pdf"),
        CurriculumBookItem("pri_4_grammar", "قواعد اللغة العربية", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1861.pdf"),
        CurriculumBookItem("pri_4_science", "العلوم", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1859.pdf"),
        CurriculumBookItem("pri_4_social", "الاجتماعيات", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1856.pdf"),
        CurriculumBookItem("pri_4_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1862.pdf"),
        CurriculumBookItem("pri_4_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1858.pdf"),

        // 5) الصف الخامس الابتدائي
        CurriculumBookItem("pri_5_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1847.pdf"),
        CurriculumBookItem("pri_5_reading", "القراءة العربية", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1848.pdf"),
        CurriculumBookItem("pri_5_math", "الرياضيات", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1850.pdf"),
        CurriculumBookItem("pri_5_grammar", "قواعد اللغة العربية", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1849.pdf"),
        CurriculumBookItem("pri_5_science", "العلوم", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1854.pdf"),
        CurriculumBookItem("pri_5_social", "الاجتماعيات", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1852.pdf"),
        CurriculumBookItem("pri_5_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1855.pdf"),
        CurriculumBookItem("pri_5_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1853.pdf"),

        // 6) الصف السادس الابتدائي
        CurriculumBookItem("pri_6_islamic", "التربية الإسلامية", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1773.pdf"),
        CurriculumBookItem("pri_6_reading", "القراءة العربية", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1780.pdf"),
        CurriculumBookItem("pri_6_math", "الرياضيات", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1778.pdf"),
        CurriculumBookItem("pri_6_grammar", "قواعد اللغة العربية", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1774.pdf"),
        CurriculumBookItem("pri_6_science", "العلوم", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1775.pdf"),
        CurriculumBookItem("pri_6_social", "الاجتماعيات", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1777.pdf"),
        CurriculumBookItem("pri_6_sci_act", "نشاط العلوم", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1776.pdf"),
        CurriculumBookItem("pri_6_math_act", "التمارين والأنشطة", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://manahj.edu.iq/upload/upfile/ar/1779.pdf"),

        // =========================================================================
        // ثانياً: المرحلة المتوسطة
        // =========================================================================
        // 1) الأول متوسط
        CurriculumBookItem("mid_1_islamic", "التربية الإسلامية", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1837.pdf"),
        CurriculumBookItem("mid_1_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1838.pdf"),
        CurriculumBookItem("mid_1_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1839.pdf"),
        CurriculumBookItem("mid_1_math", "الرياضيات", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1840.pdf"),
        CurriculumBookItem("mid_1_physics", "الفيزياء", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1841.pdf"),
        CurriculumBookItem("mid_1_biology", "الأحياء", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1842.pdf"),
        CurriculumBookItem("mid_1_computer", "الحاسوب", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1843.pdf"),
        CurriculumBookItem("mid_1_social", "الاجتماعيات", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1844.pdf"),
        CurriculumBookItem("mid_1_chemistry", "الكيمياء", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1845.pdf"),
        CurriculumBookItem("mid_1_french", "اللغة الفرنسية", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1910.pdf"),
        CurriculumBookItem("mid_1_french_act", "نشاط اللغة الفرنسية", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1912.pdf"),
        CurriculumBookItem("mid_1_ethics", "التربية الأخلاقية", CurriculumStageTab.MIDDLE, "الأول متوسط", "https://manahj.edu.iq/upload/upfile/ar/1846.pdf"),

        // 2) الثاني متوسط
        CurriculumBookItem("mid_2_islamic", "التربية الإسلامية", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1828.pdf"),
        CurriculumBookItem("mid_2_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1831.pdf"),
        CurriculumBookItem("mid_2_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1832.pdf"),
        CurriculumBookItem("mid_2_math", "الرياضيات", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1834.pdf"),
        CurriculumBookItem("mid_2_physics", "الفيزياء", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1835.pdf"),
        CurriculumBookItem("mid_2_biology", "الأحياء", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1827.pdf"),
        CurriculumBookItem("mid_2_computer", "الحاسوب", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1836.pdf"),
        CurriculumBookItem("mid_2_social", "الاجتماعيات", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1829.pdf"),
        CurriculumBookItem("mid_2_chemistry", "الكيمياء", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1830.pdf"),
        CurriculumBookItem("mid_2_french", "اللغة الفرنسية", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1911.pdf"),
        CurriculumBookItem("mid_2_french_act", "نشاط اللغة الفرنسية", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1913.pdf"),
        CurriculumBookItem("mid_2_ethics", "التربية الأخلاقية", CurriculumStageTab.MIDDLE, "الثاني متوسط", "https://manahj.edu.iq/upload/upfile/ar/1833.pdf"),

        // 3) الثالث متوسط
        CurriculumBookItem("mid_3_islamic", "التربية الإسلامية", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1787.pdf"),
        CurriculumBookItem("mid_3_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1783.pdf"),
        CurriculumBookItem("mid_3_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1784.pdf"),
        CurriculumBookItem("mid_3_math", "الرياضيات", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1785.pdf"),
        CurriculumBookItem("mid_3_physics", "الفيزياء", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1786.pdf"),
        CurriculumBookItem("mid_3_biology", "الأحياء", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1782.pdf"),
        CurriculumBookItem("mid_3_social", "الاجتماعيات", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1781.pdf"),
        CurriculumBookItem("mid_3_chemistry", "الكيمياء", CurriculumStageTab.MIDDLE, "الثالث متوسط", "https://manahj.edu.iq/upload/upfile/ar/1788.pdf"),

        // =========================================================================
        // ثالثاً: المرحلة الإعدادية
        // =========================================================================
        // 1) الرابع العلمي
        CurriculumBookItem("prep_4s_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1824.pdf"),
        CurriculumBookItem("prep_4s_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1814.pdf"),
        CurriculumBookItem("prep_4s_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1815.pdf"),
        CurriculumBookItem("prep_4s_computer", "الحاسوب", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1818.pdf"),
        CurriculumBookItem("prep_4s_biology", "الأحياء", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1812.pdf"),
        CurriculumBookItem("prep_4s_physics", "الفيزياء", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1822.pdf"),
        CurriculumBookItem("prep_4s_chemistry", "الكيمياء", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1823.pdf"),
        CurriculumBookItem("prep_4s_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1820.pdf"),
        CurriculumBookItem("prep_4s_crimes", "جرائم حزب البعث", CurriculumStageTab.PREPARATORY, "الرابع العلمي", "https://manahj.edu.iq/upload/upfile/ar/1817.pdf"),

        // 2) الرابع الأدبي
        CurriculumBookItem("prep_4l_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1824.pdf"),
        CurriculumBookItem("prep_4l_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1814.pdf"),
        CurriculumBookItem("prep_4l_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1815.pdf"),
        CurriculumBookItem("prep_4l_computer", "الحاسوب", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1818.pdf"),
        CurriculumBookItem("prep_4l_geo", "أساسيات الجغرافيا", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1825.pdf"),
        CurriculumBookItem("prep_4l_history", "التاريخ", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1816.pdf"),
        CurriculumBookItem("prep_4l_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1819.pdf"),
        CurriculumBookItem("prep_4l_sociology", "علم الاجتماع", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1821.pdf"),
        CurriculumBookItem("prep_4l_crimes", "جرائم حزب البعث", CurriculumStageTab.PREPARATORY, "الرابع الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1817.pdf"),

        // 3) الخامس العلمي
        CurriculumBookItem("prep_5s_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1800.pdf"),
        CurriculumBookItem("prep_5s_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1801.pdf"),
        CurriculumBookItem("prep_5s_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1802.pdf"),
        CurriculumBookItem("prep_5s_computer", "الحاسوب", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1803.pdf"),
        CurriculumBookItem("prep_5s_biology", "الأحياء", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1808.pdf"),
        CurriculumBookItem("prep_5s_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1811.pdf"),
        CurriculumBookItem("prep_5s_physics", "الفيزياء", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1915.pdf"),
        CurriculumBookItem("prep_5s_chemistry", "الكيمياء", CurriculumStageTab.PREPARATORY, "الخامس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1826.pdf"),

        // 4) الخامس الأدبي
        CurriculumBookItem("prep_5l_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1800.pdf"),
        CurriculumBookItem("prep_5l_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1801.pdf"),
        CurriculumBookItem("prep_5l_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1802.pdf"),
        CurriculumBookItem("prep_5l_computer", "الحاسوب", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1803.pdf"),
        CurriculumBookItem("prep_5l_history", "التاريخ", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1804.pdf"),
        CurriculumBookItem("prep_5l_geo", "الجغرافيا", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1810.pdf"),
        CurriculumBookItem("prep_5l_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1805.pdf"),
        CurriculumBookItem("prep_5l_philosophy", "الفلسفة وعلم النفس", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1806.pdf"),
        CurriculumBookItem("prep_5l_economics", "الاقتصاد", CurriculumStageTab.PREPARATORY, "الخامس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1807.pdf"),

        // 5) السادس العلمي
        CurriculumBookItem("prep_6s_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1789.pdf"),
        CurriculumBookItem("prep_6s_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1790.pdf"),
        CurriculumBookItem("prep_6s_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1791.pdf"),
        CurriculumBookItem("prep_6s_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1796.pdf"),
        CurriculumBookItem("prep_6s_biology", "الأحياء", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1799.pdf"),
        CurriculumBookItem("prep_6s_physics", "الفيزياء", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1797.pdf"),
        CurriculumBookItem("prep_6s_chemistry", "الكيمياء", CurriculumStageTab.PREPARATORY, "السادس العلمي", "https://manahj.edu.iq/upload/upfile/ar/1798.pdf"),

        // 6) السادس الأدبي
        CurriculumBookItem("prep_6l_islamic", "التربية الإسلامية", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1789.pdf"),
        CurriculumBookItem("prep_6l_arabic1", "اللغة العربية (الجزء الأول)", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1790.pdf"),
        CurriculumBookItem("prep_6l_arabic2", "اللغة العربية (الجزء الثاني)", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1791.pdf"),
        CurriculumBookItem("prep_6l_math", "الرياضيات", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1795.pdf"),
        CurriculumBookItem("prep_6l_history", "التاريخ", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1793.pdf"),
        CurriculumBookItem("prep_6l_geo", "الجغرافيا", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1794.pdf"),
        CurriculumBookItem("prep_6l_economics", "الاقتصاد", CurriculumStageTab.PREPARATORY, "السادس الأدبي", "https://manahj.edu.iq/upload/upfile/ar/1792.pdf")
    )

    fun resolveStageAndClass(rawStudentClass: String?, schoolStage: SchoolStage): Pair<CurriculumStageTab, String> {
        val norm = ArabicTextNormalizer.normalizeText(rawStudentClass.orEmpty())
        if (norm.isNotBlank()) {
            when {
                norm.contains("ابتدا") -> {
                    val matched = when {
                        norm.contains("اول") -> "الأول الابتدائي"
                        norm.contains("ثاني") -> "الثاني الابتدائي"
                        norm.contains("ثالث") -> "الثالث الابتدائي"
                        norm.contains("رابع") -> "الرابع الابتدائي"
                        norm.contains("خامس") -> "الخامس الابتدائي"
                        norm.contains("سادس") -> "السادس الابتدائي"
                        else -> "الأول الابتدائي"
                    }
                    return CurriculumStageTab.PRIMARY to matched
                }
                norm.contains("متوسط") -> {
                    val matched = when {
                        norm.contains("اول") -> "الأول متوسط"
                        norm.contains("ثاني") -> "الثاني متوسط"
                        norm.contains("ثالث") -> "الثالث متوسط"
                        else -> "الأول متوسط"
                    }
                    return CurriculumStageTab.MIDDLE to matched
                }
                norm.contains("اعدادي") || norm.contains("علمي") || norm.contains("ادبي") -> {
                    val isLiterary = norm.contains("ادبي")
                    val matched = when {
                        norm.contains("رابع") -> if (isLiterary) "الرابع الأدبي" else "الرابع العلمي"
                        norm.contains("خامس") -> if (isLiterary) "الخامس الأدبي" else "الخامس العلمي"
                        norm.contains("سادس") -> if (isLiterary) "السادس الأدبي" else "السادس العلمي"
                        else -> "الرابع العلمي"
                    }
                    return CurriculumStageTab.PREPARATORY to matched
                }
            }
        }
        return when (schoolStage) {
            SchoolStage.PRIMARY -> CurriculumStageTab.PRIMARY to "الأول الابتدائي"
            SchoolStage.MIDDLE -> CurriculumStageTab.MIDDLE to "الأول متوسط"
            SchoolStage.PREPARATORY -> CurriculumStageTab.PREPARATORY to "الرابع العلمي"
            SchoolStage.SECONDARY -> CurriculumStageTab.MIDDLE to "الأول متوسط"
        }
    }

    /**
     * Local book file path in app external downloads storage:
     * `File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "curriculum_${bookId}.pdf")`
     */
    fun getLocalCurriculumBookFile(context: Context, bookId: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "curriculums")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "curriculum_${bookId}.pdf")
    }
}

private data class ActiveReaderSessionRequest(
    val book: CurriculumBookItem,
    val forceRedownload: Boolean = false,
    val autoSaveToDownloadsOnOpen: Boolean = false
)

@Composable
fun CurriculumBooksScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT

    val activeStudent = remember(allStudents, currentUser, selectedParentChildId) {
        when (currentUser?.role) {
            UserRole.STUDENT -> allStudents.firstOrNull {
                it.id == currentUser?.linkedStudentId ||
                    it.studentUsername.equals(currentUser?.username ?: "", ignoreCase = true)
            }
            UserRole.PARENT -> {
                val children = viewModel.getChildrenForParent(currentUser)
                children.firstOrNull { it.id == selectedParentChildId }
                    ?: children.firstOrNull { it.id == currentUser?.linkedStudentId }
                    ?: children.firstOrNull()
            }
            else -> null
        }
    }

    val initialSelection = remember(activeStudent?.gradeClass, schoolConfig.schoolStage) {
        IraqiCurriculumCatalog.resolveStageAndClass(
            rawStudentClass = activeStudent?.gradeClass,
            schoolStage = schoolConfig.schoolStage
        )
    }

    var selectedStage by remember(initialSelection) { mutableStateOf(initialSelection.first) }
    var selectedClass by remember(initialSelection) { mutableStateOf(initialSelection.second) }
    var searchQuery by remember { mutableStateOf("") }
    var cacheRefreshTick by remember { mutableIntStateOf(0) }

    // Active In-App Native PDF Reader Session (Zero WebView, Zero External Browser)
    var activeReaderSession by remember { mutableStateOf<ActiveReaderSessionRequest?>(null) }

    val displayedBooks = remember(selectedStage, selectedClass, searchQuery) {
        val cleanQuery = ArabicTextNormalizer.normalizeText(searchQuery.trim())
        if (cleanQuery.isBlank()) {
            IraqiCurriculumCatalog.allBooks.filter {
                it.stage == selectedStage && it.gradeClass == selectedClass
            }
        } else {
            val inClass = IraqiCurriculumCatalog.allBooks.filter {
                it.stage == selectedStage &&
                    it.gradeClass == selectedClass &&
                    (ArabicTextNormalizer.normalizeText(it.subjectTitle).contains(cleanQuery) ||
                        ArabicTextNormalizer.normalizeText(it.gradeClass).contains(cleanQuery))
            }
            if (inClass.isNotEmpty()) {
                inClass
            } else {
                IraqiCurriculumCatalog.allBooks.filter {
                    ArabicTextNormalizer.normalizeText(it.subjectTitle).contains(cleanQuery) ||
                        ArabicTextNormalizer.normalizeText(it.gradeClass).contains(cleanQuery)
                }
            }
        }
    }

    val localSavedBooksMap = remember(cacheRefreshTick, displayedBooks) {
        displayedBooks.associate { book ->
            val f = IraqiCurriculumCatalog.getLocalCurriculumBookFile(context, book.id)
            val valid = isValidDownloadedCurriculumPdf(f)
            val sizeMb = if (valid) {
                String.format(Locale.US, "%.1f MB", f.length() / (1024.0 * 1024.0))
            } else {
                ""
            }
            book.id to (valid to sizeMb)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_books_screen_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) Wavy Emerald & Gold Header with Exclusive 3D Illuminated Digital Library Book Animation
        item(key = "curriculum_hero_header") {
            ThreeDHeroHeaderCard(
                title = "المناهج الدراسية الرسمية",
                subtitle = "المكتبة الرقمية المدمجة للمناهج العراقية بنظام التنزيل والقراءة الأصلي عالي الدقة داخل التطبيق.",
                rightPillText = selectedStage.arabicTitle,
                rightPillIcon = Icons.Filled.AutoStories,
                leftPillText = "$selectedClass (${displayedBooks.size} كتاب)",
                leftPillIcon = Icons.Filled.MenuBook,
                illustrationType = ThreeDIllustrationType.DIGITAL_LIBRARY_ILLUMINATED_BOOK_3D,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // 2) Smart Auto-Filter Notice for Student & Parent Accounts (Pure Glassmorphic Text Capsules)
        if (isParentOrStudent && activeStudent != null) {
            item(key = "student_auto_filter_banner") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.65f) else Color(0xFFECFDF5),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF34D399).copy(alpha = 0.45f) else Color(0xFF6EE7B7)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "مناهج الطالب: ${activeStudent.fullNameWithFather} (${initialSelection.second})",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else Color(0xFF065F46)
                            )
                            Text(
                                text = "تم فتح مرحلة وصف الطالب تلقائياً، ويمكنك تصفح بقية المراحل والصفوف بحرية كاملة.",
                                fontSize = 11.sp,
                                color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857)
                            )
                        }

                        if (selectedStage != initialSelection.first || selectedClass != initialSelection.second) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF059669),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        selectedStage = initialSelection.first
                                        selectedClass = initialSelection.second
                                        searchQuery = ""
                                    }
                                    .testTag("reset_to_student_class_btn")
                            ) {
                                Text(
                                    text = "العودة لصفي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3) Three Stages Glassmorphic Capsule Tabs + Class Sub-Pills + Instant Search Bar (Zero Classic Icons)
        item(key = "curriculum_stages_and_classes_selector") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stage Capsules Row (Pure Text Glassmorphic Capsules — Full Unclipped Text)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CurriculumStageTab.entries.forEach { stageTab ->
                            val isSelected = selectedStage == stageTab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        selectedStage = stageTab
                                        if (selectedClass !in stageTab.classes) {
                                            selectedClass = stageTab.classes.first()
                                        }
                                    }
                                    .testTag("curriculum_stage_tab_${stageTab.name}"),
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color.Transparent else if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSelected) Color(0xFF10B981) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 5.dp else 0.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF0F3D5E))
                                                    )
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(PaddingValues(horizontal = 6.dp, vertical = 10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stageTab.arabicTitle,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) Color.White else if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Class Selection Strip for the Active Stage (Pure Text Glassmorphic Capsules)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(selectedStage.classes, key = { it }) { classTitle ->
                            val isClassSelected = selectedClass == classTitle
                            val classBooksCount = remember(classTitle) {
                                IraqiCurriculumCatalog.allBooks.count { it.gradeClass == classTitle }
                            }
                            Surface(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        selectedClass = classTitle
                                    }
                                    .testTag("curriculum_class_pill_$classTitle"),
                                shape = RoundedCornerShape(50),
                                color = if (isClassSelected) {
                                    if (isDarkMode) Color(0xFF065F46) else Color(0xFFECFDF5)
                                } else {
                                    if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                },
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isClassSelected) Color(0xFF059669) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = classTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (isClassSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        color = if (isClassSelected) {
                                            if (isDarkMode) Color.White else Color(0xFF065F46)
                                        } else {
                                            if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
                                        },
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (isClassSelected) Color(0xFF059669) else Color(0xFFE2E8F0).copy(alpha = if (isDarkMode) 0.15f else 0.85f)
                                    ) {
                                        Text(
                                            text = "$classBooksCount",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isClassSelected) Color.White else if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Instant Search Bar (Pure Text Capsule Clear Button)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("curriculum_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0),
                                    modifier = Modifier
                                        .padding(end = 8.dp)
                                        .clip(RoundedCornerShape(50))
                                        .clickable { searchQuery = "" }
                                ) {
                                    Text(
                                        text = "مسح",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDarkMode) Color.White else Color(0xFF1E293B),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        },
                        placeholder = {
                            Text(
                                text = "ابحث باسم المادة أو الكتاب المنهجي...",
                                fontSize = 12.5.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    )
                }
            }
        }

        // 4) Full-Width Class Curriculum Section Title (Zero Squished Side Capsules)
        item(key = "on_demand_policy_notice") {
            Text(
                text = if (searchQuery.isBlank()) {
                    "كتب الصف: $selectedClass (${displayedBooks.size} منهج رسمي)"
                } else {
                    "نتائج البحث الفوري (${displayedBooks.size} كتاب)"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 5) Books Cards List
        if (displayedBooks.isEmpty()) {
            item(key = "empty_curriculum_search") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "لا يوجد كتاب مطابق لعبارة البحث الحالية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                        )
                        Text(
                            text = "جرب كتابة اسم المادة مثل: الرياضيات، الفيزياء، القراءة، أو التربية الإسلامية.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedBooks, key = { it.id }) { book ->
                val savedState = localSavedBooksMap[book.id]
                val isSavedLocally = savedState?.first == true
                val fileSizeLabel = savedState?.second.orEmpty()

                CurriculumBookLuxuryCard(
                    book = book,
                    isDarkMode = isDarkMode,
                    isSavedLocally = isSavedLocally,
                    fileSizeLabel = fileSizeLabel,
                    onOpenBookInAppClick = {
                        activeReaderSession = ActiveReaderSessionRequest(
                            book = book,
                            forceRedownload = false,
                            autoSaveToDownloadsOnOpen = false
                        )
                    },
                    onSaveToPhoneDownloadsClick = {
                        val localFile = IraqiCurriculumCatalog.getLocalCurriculumBookFile(context, book.id)
                        if (isValidDownloadedCurriculumPdf(localFile)) {
                            coroutineScope.launch {
                                val savedOk = saveBookFileToPublicDownloads(context, localFile, book)
                                if (savedOk) {
                                    viewModel.showToastAlert(
                                        message = "تم حفظ كتاب ${book.subjectTitle} في مجلد التنزيلات بنجاح",
                                        isSuccess = true
                                    )
                                } else {
                                    viewModel.showToastAlert(
                                        message = "تعذر نسخ الكتاب إلى مجلد التنزيلات، يرجى المحاولة من داخل القارئ",
                                        isSuccess = false
                                    )
                                }
                            }
                        } else {
                            // Stream download inside the app first and automatically save to Downloads upon completion
                            activeReaderSession = ActiveReaderSessionRequest(
                                book = book,
                                forceRedownload = false,
                                autoSaveToDownloadsOnOpen = true
                            )
                        }
                    },
                    onRedownloadBookClick = {
                        activeReaderSession = ActiveReaderSessionRequest(
                            book = book,
                            forceRedownload = true,
                            autoSaveToDownloadsOnOpen = false
                        )
                    }
                )
            }
        }
    }

    // Native In-App PDF Downloader & Android PdfRenderer Engine Modal (100% In-App, Zero WebView, Zero External Browser)
    val currentSession = activeReaderSession
    if (currentSession != null) {
        NativeInAppPdfReaderModal(
            sessionRequest = currentSession,
            onDismiss = {
                activeReaderSession = null
                cacheRefreshTick += 1
            },
            onBookDownloadedReady = {
                cacheRefreshTick += 1
            },
            onShowToast = { msg, success ->
                viewModel.showToastAlert(message = msg, isSuccess = success)
            }
        )
    }
}

@Composable
private fun CurriculumBookLuxuryCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    isSavedLocally: Boolean,
    fileSizeLabel: String,
    onOpenBookInAppClick: () -> Unit,
    onSaveToPhoneDownloadsClick: () -> Unit,
    onRedownloadBookClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onOpenBookInAppClick() }
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.2.dp,
            if (isSavedLocally) {
                Color(0xFF10B981).copy(alpha = 0.70f)
            } else if (isDarkMode) {
                Color(0xFF334155)
            } else {
                Color(0xFFE2E8F0)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Clean Spacious Header: Large Subject Title + Unclipped Row of Grade Badge & Instant Browse Badge
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = book.subjectTitle,
                    fontSize = 16.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                        border = BorderStroke(0.9.dp, Color(0xFF10B981).copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = book.gradeClass,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isSavedLocally) {
                            if (isDarkMode) Color(0xFF065F46) else Color(0xFFDCFCE7)
                        } else {
                            if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                        },
                        border = BorderStroke(
                            0.9.dp,
                            if (isSavedLocally) Color(0xFF10B981) else Color(0xFF94A3B8).copy(alpha = 0.45f)
                        )
                    ) {
                        Text(
                            text = if (isSavedLocally) {
                                if (fileSizeLabel.isNotBlank()) "جاهز محلياً • $fileSizeLabel" else "جاهز محلياً"
                            } else {
                                "تصفح فوري داخل التطبيق"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSavedLocally) {
                                if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                            } else {
                                if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
                            },
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // Primary Glassmorphic Pure-Text Capsule Button: Open & Read In-App
            Surface(
                onClick = onOpenBookInAppClick,
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.7f)),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
                    .testTag("btn_super_reader_${book.id}")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF059669),
                                    Color(0xFF047857),
                                    Color(0xFF0F3D5E)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSavedLocally) {
                            "فتح وقراءة الكتاب فوراً (بدون إنترنت)"
                        } else {
                            "فتح وقراءة الكتاب داخل التطبيق"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // Secondary Glassmorphic Pure-Text Capsule Buttons Row:
            // 1) "حفظ نسخة في تنزيلات الهاتف"
            // 2) "تحديث / إعادة تنزيل الكتاب"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onSaveToPhoneDownloadsClick,
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFECFDF5),
                    border = BorderStroke(
                        1.1.dp,
                        if (isDarkMode) Color(0xFF34D399).copy(alpha = 0.6f) else Color(0xFF10B981)
                    ),
                    modifier = Modifier
                        .weight(1.1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("btn_save_downloads_${book.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "حفظ نسخة في تنزيلات الهاتف",
                            fontSize = 10.8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Surface(
                    onClick = onRedownloadBookClick,
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .weight(0.95f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("btn_redownload_book_${book.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "تحديث / إعادة تنزيل الكتاب",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

private data class DownloadPollResult(
    val status: Int,
    val bytesSoFar: Long,
    val totalBytes: Long,
    val reasonCode: Int
)

private data class CapturedWebViewDownloadRequest(
    val url: String,
    val userAgent: String,
    val cookies: String?,
    val contentLength: Long
)

/**
 * جسر JavaScript (`@JavascriptInterface` باسم `PdfBridge`)
 * يستقبل قطع البيانات (`Base64 Chunks`) من `Headless Chromium WebView` ويكتبها مباشرة في `FileOutputStream`
 * لملف الكتاب المحلي مع تحديث شريط التقدم المئوي الحي (`0% ... 100%`) والتحقق من توقيع `%PDF`.
 */
private class ChromiumPdfStreamBridge(
    private val tempFile: File,
    private val finalFile: File,
    private val mainHandler: Handler,
    private val onProgressUpdate: (Int, String, String) -> Unit,
    private val onStreamSuccess: () -> Unit,
    private val onStreamFallbackNeeded: (String) -> Unit
) {
    private var outputStream: BufferedOutputStream? = null
    private var totalWrittenBytes: Long = 0L
    private var reportedTotalBytes: Long = -1L

    @JavascriptInterface
    fun onStart(totalBytes: Long) {
        runCatching {
            outputStream?.close()
            tempFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
            if (tempFile.exists()) tempFile.delete()
            outputStream = BufferedOutputStream(FileOutputStream(tempFile), 64 * 1024)
            totalWrittenBytes = 0L
            reportedTotalBytes = totalBytes
        }.onFailure { err ->
            mainHandler.post {
                onStreamFallbackNeeded(err.localizedMessage ?: "Stream init error")
            }
        }
    }

    @JavascriptInterface
    fun onChunk(base64Chunk: String, loadedBytes: Long, totalBytes: Long) {
        runCatching {
            val decoded = Base64.decode(base64Chunk, Base64.DEFAULT)
            if (outputStream == null) {
                tempFile.parentFile?.let { if (!it.exists()) it.mkdirs() }
                outputStream = BufferedOutputStream(FileOutputStream(tempFile), 64 * 1024)
            }
            outputStream?.write(decoded)
            totalWrittenBytes += decoded.size
            val effectiveLoaded = if (loadedBytes > 0L) loadedBytes else totalWrittenBytes
            val effectiveTotal = if (totalBytes > 0L) totalBytes else reportedTotalBytes

            val dlMb = effectiveLoaded / (1024.0 * 1024.0)
            val (pct, mbFormatted) = if (effectiveTotal > 0L) {
                val p = ((effectiveLoaded * 100L) / effectiveTotal).toInt().coerceIn(1, 99)
                val totalMb = effectiveTotal / (1024.0 * 1024.0)
                p to String.format(Locale.US, "%.1f MB / %.1f MB", dlMb, totalMb)
            } else {
                val approx = (8 + (effectiveLoaded / (180 * 1024)).toInt()).coerceIn(8, 95)
                approx to String.format(Locale.US, "%.1f MB", dlMb)
            }

            mainHandler.post {
                onProgressUpdate(pct, mbFormatted, "جاري سحب الكتاب... $pct% ($mbFormatted)")
            }
        }.onFailure { err ->
            runCatching { outputStream?.close() }
            outputStream = null
            mainHandler.post {
                onStreamFallbackNeeded(err.localizedMessage ?: "Chunk decode error")
            }
        }
    }

    @JavascriptInterface
    fun onComplete() {
        runCatching {
            outputStream?.flush()
            outputStream?.close()
            outputStream = null

            if (isValidDownloadedCurriculumPdf(tempFile)) {
                if (finalFile.exists()) finalFile.delete()
                val moved = tempFile.renameTo(finalFile)
                if (!moved) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
                val finalMb = finalFile.length() / (1024.0 * 1024.0)
                val finalMbFormatted = String.format(Locale.US, "%.1f MB / %.1f MB", finalMb, finalMb)
                mainHandler.post {
                    onProgressUpdate(100, finalMbFormatted, "اكتمل سحب الكتاب 100% ($finalMbFormatted)")
                    onStreamSuccess()
                }
            } else {
                if (tempFile.exists()) tempFile.delete()
                mainHandler.post {
                    onStreamFallbackNeeded("Invalid PDF signature from JS stream")
                }
            }
        }.onFailure { err ->
            runCatching { outputStream?.close() }
            outputStream = null
            mainHandler.post {
                onStreamFallbackNeeded(err.localizedMessage ?: "Stream finalize error")
            }
        }
    }

    @JavascriptInterface
    fun onFetchFailed(reason: String) {
        runCatching { outputStream?.close() }
        outputStream = null
        if (tempFile.exists()) runCatching { tempFile.delete() }
        mainHandler.post {
            onStreamFallbackNeeded(reason)
        }
    }

    fun closeQuietly() {
        runCatching { outputStream?.close() }
        outputStream = null
    }
}

/**
 * سحب مباشر للملف مع التحقق الصارم من أول 4 بايتات (`%PDF` = `0x25, 0x50, 0x44, 0x46`)
 * وتمرير الكوكيز النظيفة (`CookieManager`) و `User-Agent` الخالي من `; wv` مع دعم التحويل بين HTTP/HTTPS.
 */
private suspend fun streamUrlToPdfFileVerified(
    targetUrl: String,
    refererUrl: String,
    userAgent: String,
    cookieHeader: String?,
    tempFile: File,
    finalFile: File,
    onProgress: (Int, String, String) -> Unit
): Boolean = withContext(Dispatchers.IO) {
    runCatching {
        if (tempFile.exists()) tempFile.delete()
        tempFile.parentFile?.let { if (!it.exists()) it.mkdirs() }

        var currentUrl = targetUrl
        var redirects = 0
        var connection: HttpURLConnection? = null

        while (redirects < 6) {
            val urlObj = URL(currentUrl)
            val conn = (urlObj.openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = false
                connectTimeout = 20_000
                readTimeout = 30_000
                requestMethod = "GET"
                useCaches = false
                setRequestProperty("User-Agent", userAgent)
                setRequestProperty("Accept", "application/pdf,application/octet-stream,*/*;q=0.9")
                setRequestProperty("Accept-Language", "ar-IQ,ar;q=0.9,en-US;q=0.8,en;q=0.7")
                setRequestProperty("Referer", refererUrl)
                setRequestProperty("Connection", "keep-alive")
                setRequestProperty("Upgrade-Insecure-Requests", "1")
                if (!cookieHeader.isNullOrBlank()) {
                    setRequestProperty("Cookie", cookieHeader)
                }
            }
            conn.connect()
            val code = conn.responseCode
            if (code in listOf(
                    HttpURLConnection.HTTP_MOVED_PERM,
                    HttpURLConnection.HTTP_MOVED_TEMP,
                    HttpURLConnection.HTTP_SEE_OTHER,
                    307,
                    308
                )
            ) {
                val loc = conn.getHeaderField("Location")
                conn.disconnect()
                if (loc.isNullOrBlank()) return@runCatching false
                currentUrl = URL(urlObj, loc).toString()
                redirects++
                continue
            }
            connection = conn
            break
        }

        val activeConn = connection ?: return@runCatching false
        try {
            val responseCode = activeConn.responseCode
            if (responseCode !in 200..299) {
                return@runCatching false
            }

            val totalBytes = activeConn.contentLengthLong
            activeConn.inputStream.use { input ->
                BufferedOutputStream(FileOutputStream(tempFile), 64 * 1024).use { output ->
                    val buffer = ByteArray(32 * 1024)
                    var bytesReadTotal = 0L
                    var headerChecked = false
                    val headerCheckBuf = ByteArray(1024)
                    var headerCheckLen = 0
                    var lastReportTime = 0L

                    while (true) {
                        val read = input.read(buffer)
                        if (read == -1) break

                        if (!headerChecked) {
                            val toCopy = minOf(read, headerCheckBuf.size - headerCheckLen)
                            if (toCopy > 0) {
                                System.arraycopy(buffer, 0, headerCheckBuf, headerCheckLen, toCopy)
                                headerCheckLen += toCopy
                            }
                            if (headerCheckLen >= 4) {
                                var foundPdfMagic = false
                                for (i in 0..(headerCheckLen - 4)) {
                                    if (headerCheckBuf[i] == 0x25.toByte() &&
                                        headerCheckBuf[i + 1] == 0x50.toByte() &&
                                        headerCheckBuf[i + 2] == 0x44.toByte() &&
                                        headerCheckBuf[i + 3] == 0x46.toByte()
                                    ) {
                                        foundPdfMagic = true
                                        break
                                    }
                                }
                                if (foundPdfMagic) {
                                    headerChecked = true
                                } else if (headerCheckLen >= 512) {
                                    return@runCatching false
                                }
                            }
                        }

                        output.write(buffer, 0, read)
                        bytesReadTotal += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime >= 150L) {
                            lastReportTime = now
                            val dlMb = bytesReadTotal / (1024.0 * 1024.0)
                            if (totalBytes > 0L) {
                                val pct = ((bytesReadTotal * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                val totalMb = totalBytes / (1024.0 * 1024.0)
                                val mbFormatted = String.format(Locale.US, "%.1f MB / %.1f MB", dlMb, totalMb)
                                onProgress(pct, mbFormatted, "جاري سحب الكتاب... $pct% ($mbFormatted)")
                            } else {
                                val approx = (10 + (bytesReadTotal / (180 * 1024)).toInt()).coerceIn(10, 95)
                                val mbFormatted = String.format(Locale.US, "%.1f MB", dlMb)
                                onProgress(approx, mbFormatted, "جاري سحب الكتاب... $approx% ($mbFormatted)")
                            }
                        }
                    }
                    output.flush()
                }
            }

            if (isValidDownloadedCurriculumPdf(tempFile)) {
                if (finalFile.exists()) finalFile.delete()
                val renamed = tempFile.renameTo(finalFile)
                if (!renamed) {
                    tempFile.copyTo(finalFile, overwrite = true)
                    tempFile.delete()
                }
                val finalMb = finalFile.length() / (1024.0 * 1024.0)
                val finalMbFormatted = String.format(Locale.US, "%.1f MB / %.1f MB", finalMb, finalMb)
                onProgress(100, finalMbFormatted, "اكتمل سحب الكتاب 100% ($finalMbFormatted)")
                true
            } else {
                if (tempFile.exists()) tempFile.delete()
                false
            }
        } finally {
            activeConn.disconnect()
        }
    }.getOrDefault(false)
}

/**
 * محرك التنزيل وقارئ كتب الـ PDF الأصلي المدمج داخل التطبيق حصراً
 * (`Headless Chromium WebView Blob/Stream Bridge` + `Multi-Source Proxy Fallback` -> `android.graphics.pdf.PdfRenderer`)
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun NativeInAppPdfReaderModal(
    sessionRequest: ActiveReaderSessionRequest,
    onDismiss: () -> Unit,
    onBookDownloadedReady: () -> Unit,
    onShowToast: (String, Boolean) -> Unit
) {
    val book = sessionRequest.book
    val bookId = book.id
    val bookTitle = book.subjectTitle
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val renderMutex = remember { Mutex() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    var isDownloading by remember(bookId) { mutableStateOf(false) }
    var downloadProgressPercent by remember(bookId) { mutableIntStateOf(0) }
    var downloadedMegabytesText by remember(bookId) { mutableStateOf("0.0 MB") }
    var downloadStatusLineText by remember(bookId) { mutableStateOf("جاري سحب الكتاب... 0%") }
    var downloadErrorMessage by remember(bookId) { mutableStateOf<String?>(null) }
    var inAppBannerMessage by remember(bookId) { mutableStateOf<String?>(null) }

    var pdfFile by remember(bookId) { mutableStateOf<File?>(null) }
    var pdfRendererHolder by remember(bookId) { mutableStateOf<Pair<ParcelFileDescriptor, PdfRenderer>?>(null) }
    var pageCount by remember(bookId) { mutableIntStateOf(0) }

    var zoomScale by remember(bookId) { mutableFloatStateOf(1f) }
    var panOffsetX by remember(bookId) { mutableFloatStateOf(0f) }
    var showJumpToPageBar by remember(bookId) { mutableStateOf(false) }
    var jumpPageInput by remember(bookId) { mutableStateOf("") }

    var retryTrigger by remember(bookId) { mutableIntStateOf(0) }
    var forceFreshDownload by remember(sessionRequest) { mutableStateOf(sessionRequest.forceRedownload) }
    var saveToDownloadsWhenOpened by remember(sessionRequest) { mutableStateOf(sessionRequest.autoSaveToDownloadsOnOpen) }
    var activeDownloadId by remember(bookId) { mutableStateOf<Long?>(null) }

    var useHeadlessWebViewExtractor by remember(bookId, retryTrigger) { mutableStateOf(false) }
    var webViewDirectLoadUrl by remember(bookId, retryTrigger) { mutableStateOf<String?>(null) }
    var webViewStreamResult by remember(bookId, retryTrigger) {
        mutableStateOf<CompletableDeferred<Boolean>?>(null)
    }
    var capturedDownloadRequest by remember(bookId, retryTrigger) {
        mutableStateOf<CompletableDeferred<CapturedWebViewDownloadRequest?>?>(null)
    }

    val cleanUrl = remember(book.pdfUrl) { normalizeCurriculumPdfUrl(book.pdfUrl) }
    val localFile = remember(bookId) {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "curriculum_${bookId}.pdf")
    }
    val tempStreamFile = remember(bookId) {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "curriculum_${bookId}.part.pdf")
    }

    LaunchedEffect(inAppBannerMessage) {
        if (inAppBannerMessage != null) {
            delay(4000L)
            inAppBannerMessage = null
        }
    }

    fun closeRendererSafely() {
        val holder = pdfRendererHolder
        pdfRendererHolder = null
        if (holder != null) {
            runCatching { holder.second.close() }
            runCatching { holder.first.close() }
        }
    }

    fun cancelActiveDownloadIfAny() {
        useHeadlessWebViewExtractor = false
        webViewDirectLoadUrl = null
        webViewStreamResult?.complete(false)
        capturedDownloadRequest?.complete(null)
        val currentId = activeDownloadId
        activeDownloadId = null
        if (currentId != null) {
            runCatching {
                val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                dm?.remove(currentId)
            }
        }
        runCatching {
            if (tempStreamFile.exists()) tempStreamFile.delete()
        }
    }

    fun triggerReaderReload(forceRedownload: Boolean, saveToPublicDownloads: Boolean) {
        cancelActiveDownloadIfAny()
        forceFreshDownload = forceRedownload
        saveToDownloadsWhenOpened = saveToPublicDownloads
        retryTrigger += 1
    }

    LaunchedEffect(bookId, retryTrigger) {
        downloadErrorMessage = null

        val alreadyExistsValid = !forceFreshDownload && isValidDownloadedCurriculumPdf(localFile)

        if (!alreadyExistsValid) {
            closeRendererSafely()
            pdfFile = null
            pageCount = 0
            isDownloading = true
            downloadProgressPercent = 2
            downloadedMegabytesText = "0.0 MB"
            downloadStatusLineText = "جاري تهيئة قناة السحب المباشر للكتاب... 2%"

            withContext(Dispatchers.IO) {
                localFile.parentFile?.let { parent ->
                    if (!parent.exists()) parent.mkdirs()
                }
                if (localFile.exists()) localFile.delete()
                if (tempStreamFile.exists()) tempStreamFile.delete()
            }

            // المرحلة 1: تشغيل محرك سحب البايتات الخفي عبر كروميوم (Headless Chromium WebView Stream Extractor)
            val jsBridgeDeferred = CompletableDeferred<Boolean>()
            val downloadListenerDeferred = CompletableDeferred<CapturedWebViewDownloadRequest?>()
            webViewStreamResult = jsBridgeDeferred
            capturedDownloadRequest = downloadListenerDeferred
            webViewDirectLoadUrl = null
            useHeadlessWebViewExtractor = true

            val streamedViaJsBridge = withContext(Dispatchers.IO) {
                var elapsed = 0L
                while (isActive && !jsBridgeDeferred.isCompleted && elapsed < 45_000L) {
                    delay(200L)
                    elapsed += 200L
                }
                if (jsBridgeDeferred.isCompleted) jsBridgeDeferred.await() else false
            }

            var finalSuccess = streamedViaJsBridge && isValidDownloadedCurriculumPdf(localFile)

            // المرحلة 2: إذا واجه الـ fetch داخل الـ WebView تحدي Cloudflare أو قيود CORS،
            // نوجه الـ WebView الخفي مباشرة للرابط ليلتقط الكوكيز الشرعية عبر CookieManager و setDownloadListener
            // ثم نسحب الملف عبر البروتوكول البديل أو البروكسيات المفتوحة مع فحص %PDF
            if (!finalSuccess && isActive) {
                downloadStatusLineText = "جاري تجاوز حماية الخادم وسحب ملف الكتاب..."
                webViewDirectLoadUrl = cleanUrl

                val capturedReq = withContext(Dispatchers.IO) {
                    var waited = 0L
                    while (isActive && !downloadListenerDeferred.isCompleted && waited < 4_500L) {
                        if (isValidDownloadedCurriculumPdf(localFile)) break
                        delay(250L)
                        waited += 250L
                    }
                    if (downloadListenerDeferred.isCompleted) downloadListenerDeferred.await() else null
                }

                if (isValidDownloadedCurriculumPdf(localFile)) {
                    finalSuccess = true
                } else {
                    val cleanUa = runCatching {
                        WebSettings.getDefaultUserAgent(context)
                            .replace("; wv", "")
                            .replace("Version/4.0 ", "")
                    }.getOrDefault(NATIVE_CURRICULUM_USER_AGENT)

                    val effectiveUa = capturedReq?.userAgent
                        ?.replace("; wv", "")
                        ?.replace("Version/4.0 ", "")
                        ?.takeIf { it.isNotBlank() }
                        ?: cleanUa

                    val cookieFromManager = runCatching {
                        val cm = CookieManager.getInstance()
                        cm.getCookie(capturedReq?.url ?: cleanUrl)
                            ?: cm.getCookie("https://manahj.edu.iq")
                            ?: cm.getCookie("http://manahj.edu.iq")
                    }.getOrNull() ?: capturedReq?.cookies

                    val httpFallbackUrl = cleanUrl.replaceFirst("^https://".toRegex(RegexOption.IGNORE_CASE), "http://")
                    val encodedHttpsUrl = withContext(Dispatchers.IO) {
                        URLEncoder.encode(cleanUrl, "UTF-8")
                    }
                    val encodedHttpUrl = withContext(Dispatchers.IO) {
                        URLEncoder.encode(httpFallbackUrl, "UTF-8")
                    }

                    val candidateSources = buildList {
                        if (capturedReq != null && capturedReq.url.isNotBlank()) {
                            add(capturedReq.url to "https://manahj.edu.iq/")
                        }
                        add(httpFallbackUrl to "http://manahj.edu.iq/")
                        add(cleanUrl to "https://manahj.edu.iq/")
                        add("https://corsproxy.io/?$encodedHttpsUrl" to "https://manahj.edu.iq/")
                        add("https://api.allorigins.win/raw?url=$encodedHttpsUrl" to "https://manahj.edu.iq/")
                        add("https://api.allorigins.win/raw?url=$encodedHttpUrl" to "http://manahj.edu.iq/")
                    }.distinctBy { it.first }

                    for ((candidateUrl, referer) in candidateSources) {
                        if (!isActive || finalSuccess) break
                        val ok = streamUrlToPdfFileVerified(
                            targetUrl = candidateUrl,
                            refererUrl = referer,
                            userAgent = effectiveUa,
                            cookieHeader = cookieFromManager,
                            tempFile = tempStreamFile,
                            finalFile = localFile,
                            onProgress = { pct, mbText, statusLine ->
                                mainHandler.post {
                                    downloadProgressPercent = pct
                                    downloadedMegabytesText = mbText
                                    downloadStatusLineText = statusLine
                                }
                            }
                        )
                        if (ok && isValidDownloadedCurriculumPdf(localFile)) {
                            finalSuccess = true
                            break
                        }
                    }

                    // المرحلة 3: تمرير الكوكيز الشرعية الملتقطة من WebView إلى DownloadManager كخيار احتياطي إضافي
                    if (!finalSuccess && isActive) {
                        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                        if (downloadManager != null) {
                            val dmTargetUrl = capturedReq?.url?.takeIf { it.isNotBlank() } ?: cleanUrl
                            val dmIdResult = withContext(Dispatchers.IO) {
                                runCatching {
                                    if (localFile.exists()) localFile.delete()
                                    val req = DownloadManager.Request(Uri.parse(dmTargetUrl))
                                        .setTitle(bookTitle)
                                        .setDescription("جاري تحميل الكتاب المدرسي...")
                                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                                        .setAllowedOverMetered(true)
                                        .setAllowedOverRoaming(true)
                                        .addRequestHeader("User-Agent", effectiveUa)
                                        .addRequestHeader("Referer", "https://manahj.edu.iq/")
                                        .addRequestHeader("Accept", "application/pdf,application/octet-stream,*/*")
                                        .apply {
                                            if (!cookieFromManager.isNullOrBlank()) {
                                                addRequestHeader("Cookie", cookieFromManager)
                                            }
                                        }
                                        .setDestinationInExternalFilesDir(
                                            context,
                                            Environment.DIRECTORY_DOWNLOADS,
                                            "curriculum_${bookId}.pdf"
                                        )
                                    downloadManager.enqueue(req)
                                }
                            }

                            if (dmIdResult.isSuccess) {
                                val downloadId = dmIdResult.getOrThrow()
                                activeDownloadId = downloadId
                                while (isActive && !finalSuccess) {
                                    val pollStep = withContext(Dispatchers.IO) {
                                        val query = DownloadManager.Query().setFilterById(downloadId)
                                        var cursor: Cursor? = null
                                        try {
                                            cursor = downloadManager.query(query)
                                            if (cursor != null && cursor.moveToFirst()) {
                                                val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                                                val bytesIdx = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                                                val totalIdx = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)
                                                val reasonIdx = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                                                val status = if (statusIdx >= 0) cursor.getInt(statusIdx) else DownloadManager.STATUS_RUNNING
                                                val bytesSoFar = if (bytesIdx >= 0) cursor.getLong(bytesIdx) else 0L
                                                val totalBytes = if (totalIdx >= 0) cursor.getLong(totalIdx) else -1L
                                                val reasonCode = if (reasonIdx >= 0) cursor.getInt(reasonIdx) else 0
                                                DownloadPollResult(status, bytesSoFar, totalBytes, reasonCode)
                                            } else null
                                        } finally {
                                            cursor?.close()
                                        }
                                    } ?: break

                                    val (status, bytesSoFar, totalBytes, _) = pollStep
                                    if (bytesSoFar > 0L) {
                                        val dlMb = bytesSoFar / (1024.0 * 1024.0)
                                        if (totalBytes > 0L) {
                                            val pct = ((bytesSoFar * 100L) / totalBytes).toInt().coerceIn(1, 99)
                                            val totalMb = totalBytes / (1024.0 * 1024.0)
                                            val mbFormatted = String.format(Locale.US, "%.1f MB / %.1f MB", dlMb, totalMb)
                                            downloadProgressPercent = pct
                                            downloadedMegabytesText = mbFormatted
                                            downloadStatusLineText = "جاري سحب الكتاب... $pct% ($mbFormatted)"
                                        } else {
                                            val approxPct = (10 + (bytesSoFar / (180 * 1024)).toInt()).coerceIn(10, 95)
                                            val mbFormatted = String.format(Locale.US, "%.1f MB", dlMb)
                                            downloadProgressPercent = approxPct
                                            downloadedMegabytesText = mbFormatted
                                            downloadStatusLineText = "جاري سحب الكتاب... $approxPct% ($mbFormatted)"
                                        }
                                    }

                                    when (status) {
                                        DownloadManager.STATUS_SUCCESSFUL -> {
                                            activeDownloadId = null
                                            if (isValidDownloadedCurriculumPdf(localFile)) {
                                                val finalMb = localFile.length() / (1024.0 * 1024.0)
                                                val finalMbFormatted = String.format(Locale.US, "%.1f MB / %.1f MB", finalMb, finalMb)
                                                downloadProgressPercent = 100
                                                downloadedMegabytesText = finalMbFormatted
                                                downloadStatusLineText = "اكتمل سحب الكتاب 100% ($finalMbFormatted)"
                                                finalSuccess = true
                                            }
                                            break
                                        }
                                        DownloadManager.STATUS_FAILED -> {
                                            activeDownloadId = null
                                            runCatching { downloadManager.remove(downloadId) }
                                            break
                                        }
                                        else -> delay(400L)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            useHeadlessWebViewExtractor = false
            webViewDirectLoadUrl = null
            isDownloading = false

            if (!finalSuccess || !isValidDownloadedCurriculumPdf(localFile)) {
                runCatching { if (localFile.exists()) localFile.delete() }
                runCatching { if (tempStreamFile.exists()) tempStreamFile.delete() }
                downloadErrorMessage = "تعذر سحب ملف الكتاب من المصدر حالياً، يرجى التحقق من اتصال الإنترنت والضغط على إعادة المحاولة."
                return@LaunchedEffect
            }

            forceFreshDownload = false
            onBookDownloadedReady()
        }

        // Open localFile immediately inside android.graphics.pdf.PdfRenderer
        try {
            val opened = withContext(Dispatchers.IO) {
                if (!isValidDownloadedCurriculumPdf(localFile)) {
                    throw IllegalStateException("ملف الكتاب غير مكتمل أو غير صالح")
                }
                val pfd = ParcelFileDescriptor.open(localFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pfd to renderer
            }
            closeRendererSafely()
            pdfFile = localFile
            pdfRendererHolder = opened
            pageCount = opened.second.pageCount

            if (saveToDownloadsWhenOpened) {
                saveToDownloadsWhenOpened = false
                val savedOk = saveBookFileToPublicDownloads(context, localFile, book)
                if (savedOk) {
                    val confirmMsg = "تم حفظ كتاب ${book.subjectTitle} في مجلد التنزيلات بنجاح"
                    inAppBannerMessage = confirmMsg
                    onShowToast(confirmMsg, true)
                } else {
                    val failMsg = "تعذر حفظ نسخة الكتاب في مجلد التنزيلات"
                    inAppBannerMessage = failMsg
                    onShowToast(failMsg, false)
                }
            }
        } catch (_: CancellationException) {
            return@LaunchedEffect
        } catch (_: Exception) {
            runCatching { if (localFile.exists()) localFile.delete() }
            downloadErrorMessage = "تعذر قراءة صفحات ملف الـ PDF، يرجى الضغط على (تحديث / إعادة تنزيل الكتاب)."
        }
    }

    DisposableEffect(bookId) {
        onDispose {
            cancelActiveDownloadIfAny()
            closeRendererSafely()
        }
    }

    val currentVisiblePage by remember {
        derivedStateOf {
            (listState.firstVisibleItemIndex + 1).coerceAtLeast(1)
        }
    }

    Dialog(
        onDismissRequest = {
            cancelActiveDownloadIfAny()
            closeRendererSafely()
            onDismiss()
        },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Glassmorphic Header Bar with Safe Status Bar Padding (statusBarsPadding + top = 36.dp)
                // Strictly Pure-Text Glassmorphic Capsules (Zero Classic Icons)
                Surface(
                    color = Color(0xFF064E3B),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 36.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = book.subjectTitle,
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (pageCount > 0) {
                                        "${book.stage.arabicTitle} • ${book.gradeClass} • صفحة $currentVisiblePage من $pageCount"
                                    } else {
                                        "${book.stage.arabicTitle} • ${book.gradeClass}"
                                    },
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Pure-Text Glassmorphic Close Capsule Button
                            Surface(
                                onClick = {
                                    cancelActiveDownloadIfAny()
                                    closeRendererSafely()
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                modifier = Modifier.testTag("reader_close_capsule_btn")
                            ) {
                                Text(
                                    text = "إغلاق القارئ",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Action Capsules Row Inside Reader Header:
                        // 1) "حفظ نسخة في تنزيلات الهاتف"
                        // 2) "تحديث / إعادة تنزيل الكتاب"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    val currentLocal = pdfFile ?: IraqiCurriculumCatalog.getLocalCurriculumBookFile(context, book.id)
                                    if (isValidDownloadedCurriculumPdf(currentLocal) && !isDownloading) {
                                        coroutineScope.launch {
                                            val savedOk = saveBookFileToPublicDownloads(context, currentLocal, book)
                                            if (savedOk) {
                                                val msg = "تم حفظ كتاب ${book.subjectTitle} في مجلد التنزيلات بنجاح"
                                                inAppBannerMessage = msg
                                                onShowToast(msg, true)
                                            } else {
                                                val errMsg = "تعذر حفظ نسخة الكتاب في مجلد التنزيلات"
                                                inAppBannerMessage = errMsg
                                                onShowToast(errMsg, false)
                                            }
                                        }
                                    } else if (!isDownloading) {
                                        triggerReaderReload(
                                            forceRedownload = false,
                                            saveToPublicDownloads = true
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF059669),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .testTag("reader_save_to_downloads_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "حفظ نسخة في تنزيلات الهاتف",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    if (!isDownloading) {
                                        triggerReaderReload(
                                            forceRedownload = true,
                                            saveToPublicDownloads = false
                                        )
                                    }
                                },
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFFFEF08A).copy(alpha = 0.55f)),
                                modifier = Modifier
                                    .weight(0.95f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .testTag("reader_redownload_book_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "تحديث / إعادة تنزيل الكتاب",
                                        fontSize = 10.8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFEF08A),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        // Instant In-App Glassmorphic Confirmation Banner
                        AnimatedVisibility(
                            visible = !inAppBannerMessage.isNullOrBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF047857),
                                border = BorderStroke(1.dp, Color(0xFFFEF08A)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = inAppBannerMessage.orEmpty(),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }

                        // Quick Jump to Any Page Bar (Collapsible Pure-Text Glassmorphic Bar)
                        AnimatedVisibility(visible = showJumpToPageBar && pageCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = jumpPageInput,
                                            onValueChange = { raw ->
                                                jumpPageInput = raw.filter { it.isDigit() }.take(4)
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(50.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            placeholder = {
                                                Text(
                                                    text = "اكتب رقم الصفحة (1 - $pageCount)",
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF94A3B8)
                                                )
                                            },
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = Color(0xFF34D399),
                                                unfocusedBorderColor = Color(0xFF475569),
                                                focusedTextColor = Color.White,
                                                unfocusedTextColor = Color.White
                                            )
                                        )

                                        Surface(
                                            onClick = {
                                                val target = jumpPageInput.toIntOrNull()
                                                if (target != null && pageCount > 0) {
                                                    val index = (target - 1).coerceIn(0, pageCount - 1)
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(index)
                                                    }
                                                    showJumpToPageBar = false
                                                }
                                            },
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFF10B981),
                                            modifier = Modifier.height(44.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "انتقال فوري",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    // Quick Jump Capsules (الأولى | -10 | +10 | الأخيرة)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val jumpActions = listOf(
                                            "الصفحة الأولى" to 0,
                                            "السابق 10-" to (currentVisiblePage - 11).coerceAtLeast(0),
                                            "التالي 10+" to (currentVisiblePage + 9).coerceAtMost((pageCount - 1).coerceAtLeast(0)),
                                            "الصفحة الأخيرة" to (pageCount - 1).coerceAtLeast(0)
                                        )
                                        jumpActions.forEach { (label, targetIdx) ->
                                            Surface(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        listState.scrollToItem(targetIdx)
                                                    }
                                                },
                                                shape = RoundedCornerShape(50),
                                                color = Color.White.copy(alpha = 0.12f),
                                                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.3f)),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White,
                                                    textAlign = TextAlign.Center,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Main Content Area: Live In-App Download Progress UI OR Native PdfRenderer Pages
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .pointerInput(pageCount) {
                            if (pageCount > 0) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 3.5f)
                                    zoomScale = newScale
                                    panOffsetX = if (newScale > 1.02f) {
                                        (panOffsetX + pan.x).coerceIn(-700f, 700f)
                                    } else {
                                        0f
                                    }
                                }
                            }
                        }
                ) {
                    // محرك سحب البايتات الخفي عبر كروميوم (Headless Chromium WebView Stream Extractor)
                    if (useHeadlessWebViewExtractor) {
                        val bridge = remember(bookId, retryTrigger) {
                            ChromiumPdfStreamBridge(
                                tempFile = tempStreamFile,
                                finalFile = localFile,
                                mainHandler = mainHandler,
                                onProgressUpdate = { pct, mbText, statusLine ->
                                    downloadProgressPercent = pct
                                    downloadedMegabytesText = mbText
                                    downloadStatusLineText = statusLine
                                },
                                onStreamSuccess = {
                                    webViewStreamResult?.complete(true)
                                },
                                onStreamFallbackNeeded = {
                                    webViewStreamResult?.complete(false)
                                }
                            )
                        }

                        DisposableEffect(bridge) {
                            onDispose {
                                bridge.closeQuietly()
                            }
                        }

                        AndroidView(
                            modifier = Modifier
                                .size(1.dp)
                                .alpha(0.01f),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    val cleanBrowserUa = runCatching {
                                        WebSettings.getDefaultUserAgent(ctx)
                                            .replace("; wv", "")
                                            .replace("Version/4.0 ", "")
                                    }.getOrDefault(NATIVE_CURRICULUM_USER_AGENT)

                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.databaseEnabled = true
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    settings.userAgentString = cleanBrowserUa
                                    settings.allowFileAccess = false
                                    settings.allowContentAccess = false
                                    settings.cacheMode = WebSettings.LOAD_NO_CACHE

                                    val cookieManager = CookieManager.getInstance()
                                    cookieManager.setAcceptCookie(true)
                                    cookieManager.setAcceptThirdPartyCookies(this, true)

                                    addJavascriptInterface(bridge, "PdfBridge")

                                    webViewClient = object : WebViewClient() {
                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            return false
                                        }
                                    }

                                    setDownloadListener { url, userAgent, _, _, contentLength ->
                                        val cookies = runCatching {
                                            CookieManager.getInstance().getCookie(url)
                                                ?: CookieManager.getInstance().getCookie("https://manahj.edu.iq")
                                        }.getOrNull()
                                        capturedDownloadRequest?.complete(
                                            CapturedWebViewDownloadRequest(
                                                url = url,
                                                userAgent = userAgent,
                                                cookies = cookies,
                                                contentLength = contentLength
                                            )
                                        )
                                        webViewStreamResult?.complete(false)
                                    }

                                    val httpAltUrl = cleanUrl.replaceFirst(
                                        "^https://".toRegex(RegexOption.IGNORE_CASE),
                                        "http://"
                                    )
                                    val jsEscapedClean = cleanUrl.replace("\\", "\\\\").replace("'", "\\'")
                                    val jsEscapedHttp = httpAltUrl.replace("\\", "\\\\").replace("'", "\\'")

                                    val htmlFetcherScript = """
                                        <!DOCTYPE html>
                                        <html>
                                        <head><meta charset="utf-8"></head>
                                        <body>
                                        <script>
                                        (async function() {
                                            const primaryUrl = '$jsEscapedClean';
                                            const httpUrl = '$jsEscapedHttp';
                                            const candidates = [
                                                { url: primaryUrl, opts: { method: 'GET', credentials: 'include', mode: 'cors' } },
                                                { url: 'https://corsproxy.io/?' + encodeURIComponent(primaryUrl), opts: { method: 'GET', mode: 'cors' } },
                                                { url: 'https://api.allorigins.win/raw?url=' + encodeURIComponent(primaryUrl), opts: { method: 'GET', mode: 'cors' } },
                                                { url: 'https://api.allorigins.win/raw?url=' + encodeURIComponent(httpUrl), opts: { method: 'GET', mode: 'cors' } }
                                            ];

                                            function uint8ToBase64(bytes) {
                                                let binary = '';
                                                const chunkSize = 0x8000;
                                                for (let i = 0; i < bytes.length; i += chunkSize) {
                                                    binary += String.fromCharCode.apply(null, bytes.subarray(i, i + chunkSize));
                                                }
                                                return btoa(binary);
                                            }

                                            function hasPdfSignature(bytes) {
                                                const limit = Math.min(bytes.length - 4, 512);
                                                for (let i = 0; i <= limit; i++) {
                                                    if (bytes[i] === 0x25 && bytes[i + 1] === 0x50 && bytes[i + 2] === 0x44 && bytes[i + 3] === 0x46) {
                                                        return true;
                                                    }
                                                }
                                                return false;
                                            }

                                            for (let c = 0; c < candidates.length; c++) {
                                                try {
                                                    const resp = await fetch(candidates[c].url, candidates[c].opts);
                                                    if (!resp.ok || !resp.body) continue;
                                                    const totalHeader = resp.headers.get('content-length');
                                                    const totalBytes = totalHeader ? parseInt(totalHeader, 10) : -1;
                                                    const reader = resp.body.getReader();

                                                    let verifiedHeader = false;
                                                    let loadedBytes = 0;
                                                    let pendingBuffer = new Uint8Array(0);
                                                    let startedBridge = false;

                                                    while (true) {
                                                        const { done, value } = await reader.read();
                                                        if (done) break;
                                                        if (!value || value.length === 0) continue;

                                                        const merged = new Uint8Array(pendingBuffer.length + value.length);
                                                        merged.set(pendingBuffer, 0);
                                                        merged.set(value, pendingBuffer.length);
                                                        pendingBuffer = merged;

                                                        if (!verifiedHeader) {
                                                            if (pendingBuffer.length < 16) continue;
                                                            if (!hasPdfSignature(pendingBuffer)) {
                                                                break;
                                                            }
                                                            verifiedHeader = true;
                                                            PdfBridge.onStart(totalBytes);
                                                            startedBridge = true;
                                                        }

                                                        if (pendingBuffer.length >= 65536) {
                                                            loadedBytes += pendingBuffer.length;
                                                            PdfBridge.onChunk(uint8ToBase64(pendingBuffer), loadedBytes, totalBytes);
                                                            pendingBuffer = new Uint8Array(0);
                                                        }
                                                    }

                                                    if (verifiedHeader && startedBridge) {
                                                        if (pendingBuffer.length > 0) {
                                                            loadedBytes += pendingBuffer.length;
                                                            PdfBridge.onChunk(uint8ToBase64(pendingBuffer), loadedBytes, totalBytes);
                                                            pendingBuffer = new Uint8Array(0);
                                                        }
                                                        if (loadedBytes > 50000) {
                                                            PdfBridge.onComplete();
                                                            return;
                                                        }
                                                    }
                                                } catch (e) {
                                                    // Try next candidate
                                                }
                                            }
                                            PdfBridge.onFetchFailed('All JS fetch candidates failed');
                                        })();
                                        </script>
                                        </body>
                                        </html>
                                    """.trimIndent()

                                    loadDataWithBaseURL(
                                        "https://manahj.edu.iq/",
                                        htmlFetcherScript,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
                                }
                            },
                            update = { webView ->
                                val directUrl = webViewDirectLoadUrl
                                if (!directUrl.isNullOrBlank() && webView.tag != directUrl) {
                                    webView.tag = directUrl
                                    webView.loadUrl(
                                        directUrl,
                                        mapOf("Referer" to "https://manahj.edu.iq/")
                                    )
                                }
                            }
                        )
                    }

                    when {
                        isDownloading -> {
                            // 2) شاشة تقدم تنزيل زجاجية حية داخل التطبيق (Live In-App Download Progress UI)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 22.dp)
                                    .fillMaxWidth()
                                    .testTag("live_in_app_download_progress_card"),
                                shape = RoundedCornerShape(28.dp),
                                color = Color(0xFF1E293B).copy(alpha = 0.96f),
                                border = BorderStroke(
                                    1.5.dp,
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF34D399),
                                            Color(0xFFFBBF24),
                                            Color(0xFF10B981)
                                        )
                                    )
                                ),
                                shadowElevation = 18.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 24.dp, vertical = 28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    // Stage & Grade Pill
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF064E3B),
                                        border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f))
                                    ) {
                                        Text(
                                            text = "${book.stage.arabicTitle} • ${book.gradeClass}",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFA7F3D0),
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                        )
                                    }

                                    // Book Name
                                    Text(
                                        text = "كتاب ${book.subjectTitle}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    // Circular Glowing Percentage Ring
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            progress = { (downloadProgressPercent / 100f).coerceIn(0.03f, 1f) },
                                            modifier = Modifier.size(92.dp),
                                            color = Color(0xFF10B981),
                                            trackColor = Color(0xFF334155),
                                            strokeWidth = 7.dp
                                        )
                                        Text(
                                            text = "$downloadProgressPercent%",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFEF08A)
                                        )
                                    }

                                    // Glowing Glassmorphic Linear Progress Bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(14.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(Color(0xFF0F172A))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .fillMaxWidth(fraction = (downloadProgressPercent / 100f).coerceIn(0.04f, 1f))
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    Brush.horizontalGradient(
                                                        colors = listOf(
                                                            Color(0xFF059669),
                                                            Color(0xFF10B981),
                                                            Color(0xFFFBBF24)
                                                        )
                                                    )
                                                )
                                        )
                                    }

                                    // Live Status Line: e.g. "جاري سحب الكتاب... 64% (10.2 MB / 15.8 MB)"
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF0F172A),
                                        border = BorderStroke(1.dp, Color(0xFFFEF08A).copy(alpha = 0.45f))
                                    ) {
                                        Text(
                                            text = downloadStatusLineText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFEF08A),
                                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    Text(
                                        text = "جاري سحب ملف الكتاب مباشرة عبر مدير التنزيل المدمج...\nسيتم فتح صفحات الكتاب تلقائياً فور اكتمال التحميل 100%",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFCBD5E1),
                                        textAlign = TextAlign.Center
                                    )

                                    // Pure-Text Glassmorphic Cancel Capsule
                                    Surface(
                                        onClick = {
                                            cancelActiveDownloadIfAny()
                                            isDownloading = false
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF7F1D1D).copy(alpha = 0.45f),
                                        border = BorderStroke(1.2.dp, Color(0xFFF87171)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(50))
                                            .testTag("cancel_on_demand_download_btn")
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "إلغاء التحميل والعودة",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFCA5A5)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        downloadErrorMessage != null -> {
                            // In-App Retry Card (Zero External Browser)
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 22.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.4.dp, Color(0xFFF87171).copy(alpha = 0.6f)),
                                shadowElevation = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "تعذر إكمال سحب ملف الكتاب (${book.subjectTitle})",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = downloadErrorMessage.orEmpty(),
                                        fontSize = 12.sp,
                                        color = Color(0xFFFCA5A5),
                                        textAlign = TextAlign.Center
                                    )

                                    Surface(
                                        onClick = {
                                            triggerReaderReload(
                                                forceRedownload = true,
                                                saveToPublicDownloads = sessionRequest.autoSaveToDownloadsOnOpen
                                            )
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF059669),
                                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(50))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "تحديث / إعادة تنزيل الكتاب الآن",
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = onDismiss,
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(50))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = "العودة لقائمة الكتب",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        pdfRendererHolder != null && pageCount > 0 -> {
                            // 3) عارض صفحات PDF أصلي فائق الوضوح عبر android.graphics.pdf.PdfRenderer
                            val renderer = pdfRendererHolder!!.second
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        translationX = panOffsetX
                                    }
                                    .testTag("native_pdf_pages_lazy_column"),
                                contentPadding = PaddingValues(
                                    start = 10.dp,
                                    end = 10.dp,
                                    top = 14.dp,
                                    bottom = 96.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(count = pageCount, key = { pageIndex -> "pdf_page_$pageIndex" }) { pageIndex ->
                                    PdfRendererPageCard(
                                        renderer = renderer,
                                        renderMutex = renderMutex,
                                        pageIndex = pageIndex,
                                        totalPages = pageCount
                                    )
                                }
                            }

                            // Floating Bottom Pure-Text Glassmorphic Zoom & Page Counter Bar
                            // ("تكبير +" | "تصغير -" | "ملاءمة الشاشة" | "صفحة X من Y")
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF064E3B).copy(alpha = 0.95f),
                                border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.65f)),
                                shadowElevation = 12.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Pure-Text Zoom In Capsule ("تكبير +")
                                    Surface(
                                        onClick = {
                                            zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.16f),
                                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .testTag("pdf_zoom_in_btn")
                                    ) {
                                        Text(
                                            text = "تكبير +",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    // Pure-Text Zoom Out Capsule ("تصغير -")
                                    Surface(
                                        onClick = {
                                            zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                            if (zoomScale <= 1.02f) panOffsetX = 0f
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.16f),
                                        border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .testTag("pdf_zoom_out_btn")
                                    ) {
                                        Text(
                                            text = "تصغير -",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    // Pure-Text Fit Screen Capsule ("ملاءمة الشاشة")
                                    Surface(
                                        onClick = {
                                            zoomScale = 1f
                                            panOffsetX = 0f
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = if (zoomScale > 1.02f) Color(0xFFF59E0B) else Color.White.copy(alpha = 0.12f),
                                        border = BorderStroke(0.8.dp, Color(0xFFFEF08A).copy(alpha = 0.5f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .testTag("pdf_fit_screen_btn")
                                    ) {
                                        Text(
                                            text = "ملاءمة الشاشة",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    // Page Counter & Quick Jump Trigger Capsule ("صفحة X من Y")
                                    Surface(
                                        onClick = {
                                            jumpPageInput = currentVisiblePage.toString()
                                            showJumpToPageBar = !showJumpToPageBar
                                        },
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFF0F172A).copy(alpha = 0.75f),
                                        border = BorderStroke(0.9.dp, GoldAccent.copy(alpha = 0.7f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .testTag("pdf_page_counter_jump_btn")
                                    ) {
                                        Text(
                                            text = "صفحة $currentVisiblePage من $pageCount",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFEF08A),
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PdfRendererPageCard(
    renderer: PdfRenderer,
    renderMutex: Mutex,
    pageIndex: Int,
    totalPages: Int
) {
    var pageBitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(null) }
    var aspectRatio by remember(pageIndex) { mutableFloatStateOf(0.707f) }

    LaunchedEffect(renderer, pageIndex) {
        withContext(Dispatchers.IO) {
            renderMutex.withLock {
                runCatching {
                    renderer.openPage(pageIndex).use { page ->
                        val w = (page.width * 2).coerceIn(720, 1600)
                        val h = (page.height * 2).coerceIn(960, 2260)
                        if (page.height > 0) {
                            aspectRatio = page.width.toFloat() / page.height.toFloat()
                        }
                        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                        bmp.eraseColor(android.graphics.Color.WHITE)
                        page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        pageBitmap = bmp
                    }
                }
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio.coerceIn(0.55f, 0.90f))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            val bmp = pageBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "صفحة ${pageIndex + 1} من $totalPages",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CircularProgressIndicator(
                        color = DeepEmerald,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "جاري عرض صفحة ${pageIndex + 1} من $totalPages...",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFF0F172A).copy(alpha = 0.76f)
            ) {
                Text(
                    text = "صفحة ${pageIndex + 1} من $totalPages",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

@Composable
fun DeveloperCurriculumManagerSection(
    viewModel: SchoolViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    var showFullCurriculumModal by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "فهرس المناهج الدراسية العراقية الموحد",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "يضم ${IraqiCurriculumCatalog.allBooks.size} كتاباً منهجياً رسمياً بمحرك تنزيل وقارئ PDF أصلي مدمج داخل التطبيق حصراً",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                onClick = { showFullCurriculumModal = true },
                shape = RoundedCornerShape(50),
                color = DeepEmerald,
                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(50))
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "استعراض وفحص مكتبة المناهج الرسمية",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }

    if (showFullCurriculumModal) {
        Dialog(
            onDismissRequest = { showFullCurriculumModal = false },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF064E3B),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(top = 36.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "مكتبة المناهج الدراسية العراقية",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                            Surface(
                                onClick = { showFullCurriculumModal = false },
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "إغلاق",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        CurriculumBooksScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
