package com.example.ui.screens

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.viewinterop.AndroidView
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.model.*
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.ThreeDIllustrationType
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

private const val MIN_VALID_BOOK_BYTES = 50_000L // 50 KB minimum

/**
 * تنظيف وتوحيد رابط الكتاب:
 * يحول أي بادئة `http://` أو `Https://` إلى `https://`
 */
private fun normalizeCurriculumPdfUrl(rawUrl: String): String {
    return rawUrl
        .trim()
        .replaceFirst("^http://".toRegex(RegexOption.IGNORE_CASE), "https://")
        .replaceFirst("^Https://".toRegex(), "https://")
}

/**
 * التحقق من البصمة السحرية لملف الـ PDF (`%PDF` -> `0x25, 0x50, 0x44, 0x46`)
 * وحجم الملف أكبر من 50 KB.
 */
private fun hasPdfMagicSignature(file: File): Boolean {
    if (!file.exists() || file.length() <= MIN_VALID_BOOK_BYTES) return false
    return runCatching {
        FileInputStream(file).use { fis ->
            val header = ByteArray(1024)
            val read = fis.read(header)
            if (read < 4) return@use false
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

private fun isValidDownloadedCurriculumPdf(file: File): Boolean {
    return file.exists() && file.length() > MIN_VALID_BOOK_BYTES && hasPdfMagicSignature(file)
}

/**
 * يستورد ملف PDF تم اختياره عبر منتقي الملفات (`OpenDocument`) ويحفظه داخل مجلد التطبيق الداخلي للأبد:
 * `File(context.filesDir, "books/book_${book.id}.pdf")`
 */
private suspend fun importPdfUriToInternalBookStorage(
    context: Context,
    sourceUri: Uri,
    book: CurriculumBookItem
): File? = withContext(Dispatchers.IO) {
    val destFile = IraqiCurriculumCatalog.getInternalBookFile(context, book.id)
    val tempFile = File(destFile.parentFile, "temp_import_${book.id}.pdf")
    runCatching {
        if (tempFile.exists()) tempFile.delete()
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output, bufferSize = 32 * 1024)
                output.flush()
            }
        } ?: return@withContext null

        if (isValidDownloadedCurriculumPdf(tempFile)) {
            if (destFile.exists()) destFile.delete()
            val moved = tempFile.renameTo(destFile)
            if (!moved) {
                tempFile.copyTo(destFile, overwrite = true)
                tempFile.delete()
            }
            destFile
        } else {
            if (tempFile.exists()) tempFile.delete()
            null
        }
    }.getOrNull()
}

/**
 * يقوم بجدولة تنزيل مباشر للكتاب داخل التطبيق عبر `android.app.DownloadManager`
 * مع تمرير كوكيز الجلسة الشرعية (`Cookie`) و (`User-Agent`) المستخلصة من `WebView` المدمج،
 * وحفظه مباشرة في: `setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, "book_${bookId}.pdf")`.
 */
private fun enqueueDirectBookDownloadWithSessionCookies(
    context: Context,
    book: CurriculumBookItem,
    downloadUrl: String,
    cookies: String?,
    userAgent: String?
): Long? {
    return runCatching {
        val cleanUrl = normalizeCurriculumPdfUrl(downloadUrl.ifBlank { book.normalizedPdfUrl })
        val appDownloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (appDownloadsDir != null) {
            val targetFile = File(appDownloadsDir, "book_${book.id}.pdf")
            if (targetFile.exists() && !isValidDownloadedCurriculumPdf(targetFile)) {
                targetFile.delete()
            }
        }

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            ?: return@runCatching null

        val effectiveUa = userAgent?.takeIf { it.isNotBlank() }
            ?: "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"

        val request = DownloadManager.Request(Uri.parse(cleanUrl)).apply {
            addRequestHeader("User-Agent", effectiveUa)
            addRequestHeader("Referer", "https://manahj.edu.iq/")
            addRequestHeader("Accept", "application/pdf,application/octet-stream,*/*;q=0.8")
            if (!cookies.isNullOrBlank()) {
                addRequestHeader("Cookie", cookies)
            }
            setTitle("كتاب ${book.subjectTitle} - ${book.gradeClass}")
            setDescription("جاري تنزيل الكتاب المنهجي لفتحه تلقائياً داخل التطبيق")
            setMimeType("application/pdf")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            setAllowedOverMetered(true)
            setAllowedOverRoaming(true)
            setDestinationInExternalFilesDir(
                context,
                Environment.DIRECTORY_DOWNLOADS,
                "book_${book.id}.pdf"
            )
        }
        dm.enqueue(request)
    }.getOrNull()
}

/**
 * دالة الفحص والبحث التلقائي الذكي في الهاتف (`autoFindAndImportBookPdf`):
 * تبحث عن ملف الكتاب تلقائياً في:
 * 1. مجلد التطبيق الداخلي (`File(context.filesDir, "books/book_${bookId}.pdf")`) ومجلد `getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)`.
 * 2. مجلد تنزيلات الهاتف العام `Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)`
 *    للبحث عن أي ملف PDF يطابق رقم الكتاب في الرابط (مثل `1881.pdf` أو `1881 (1).pdf`) أو أحدث ملف PDF تم تنزيله للتو.
 * 3. استعلام `MediaStore.Downloads` (`contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, ...)`)
 *    للبحث عن أحدث ملف PDF في التنزيلات ونسخه بصمت عبر `contentResolver.openInputStream(uri)` إلى مجلد التطبيق الداخلي!
 */
private suspend fun autoFindAndImportBookPdf(
    context: Context,
    book: CurriculumBookItem,
    acceptRecentPdfSinceMs: Long = 0L
): File? = withContext(Dispatchers.IO) {
    val internalFile = IraqiCurriculumCatalog.getInternalBookFile(context, book.id)

    // 1. فحص مجلد التطبيق الداخلي أولاً: File(context.filesDir, "books/book_${bookId}.pdf")
    if (isValidDownloadedCurriculumPdf(internalFile)) {
        return@withContext internalFile
    }

    // 1-ب. فحص مسار DownloadManager المدمج داخل التطبيق: getExternalFilesDir(DIRECTORY_DOWNLOADS)/book_${book.id}.pdf
    runCatching {
        val appExtDownloads = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        if (appExtDownloads != null) {
            val dmFile = File(appExtDownloads, "book_${book.id}.pdf")
            if (isValidDownloadedCurriculumPdf(dmFile)) {
                dmFile.copyTo(internalFile, overwrite = true)
                if (isValidDownloadedCurriculumPdf(internalFile)) {
                    return@withContext internalFile
                }
                return@withContext dmFile
            }
        }
    }

    // فحص المسار المحلي الرديف داخل التطبيق في حال كان محفوظاً من جلسة سابقة
    val legacyAppExternalFile = IraqiCurriculumCatalog.getLegacyExternalBookFile(context, book.id)
    if (isValidDownloadedCurriculumPdf(legacyAppExternalFile)) {
        runCatching {
            legacyAppExternalFile.copyTo(internalFile, overwrite = true)
        }
        return@withContext if (isValidDownloadedCurriculumPdf(internalFile)) internalFile else legacyAppExternalFile
    }

    val fileNum = book.remoteFileNumber // e.g., "1881"
    val exactFileName = book.remoteFileName.lowercase(Locale.ROOT) // e.g., "1881.pdf"
    val cleanSubject = book.subjectTitle.trim()

    fun matchesBookFileName(candidateName: String): Boolean {
        val lower = candidateName.trim().lowercase(Locale.ROOT)
        if (!lower.endsWith(".pdf")) return false
        val baseName = lower.removeSuffix(".pdf").trim()
        if (fileNum.isNotBlank()) {
            // يطابق 1881.pdf أو 1881 (1).pdf أو 1881 (2).pdf أو 1881-1.pdf أو 1881_1.pdf أو يبدأ برقم الكتاب
            if (lower == exactFileName) return true
            if (baseName == fileNum) return true
            if (baseName.startsWith("$fileNum ") ||
                baseName.startsWith("$fileNum(") ||
                baseName.startsWith("${fileNum}_") ||
                baseName.startsWith("$fileNum-") ||
                baseName.contains(fileNum)
            ) {
                return true
            }
        }
        if (cleanSubject.length >= 4 && candidateName.contains(cleanSubject)) {
            return true
        }
        return false
    }

    // 2. فحص مجلد تنزيلات الهاتف العام المباشر Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    runCatching {
        @Suppress("DEPRECATION")
        val publicDownloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (publicDownloadsDir != null && publicDownloadsDir.exists() && publicDownloadsDir.isDirectory) {
            val allValidPdfs = publicDownloadsDir
                .listFiles()
                ?.filter { file ->
                    file.isFile &&
                        file.name.lowercase(Locale.ROOT).endsWith(".pdf") &&
                        file.length() > MIN_VALID_BOOK_BYTES &&
                        hasPdfMagicSignature(file)
                }
                .orEmpty()

            val matchedFile = allValidPdfs
                .filter { file -> matchesBookFileName(file.name) }
                .maxByOrNull { it.lastModified() }
                ?: if (acceptRecentPdfSinceMs > 0L) {
                    // يلتقط أحدث ملف PDF نزل في مجلد Downloads خلال جلسة التنزيل الحالية
                    allValidPdfs
                        .filter { file -> file.lastModified() >= (acceptRecentPdfSinceMs - 20_000L) }
                        .maxByOrNull { it.lastModified() }
                } else {
                    null
                }

            if (matchedFile != null) {
                val tempCopy = File(internalFile.parentFile, "scan_copy_${book.id}.pdf")
                if (tempCopy.exists()) tempCopy.delete()
                matchedFile.copyTo(tempCopy, overwrite = true)
                if (isValidDownloadedCurriculumPdf(tempCopy)) {
                    if (internalFile.exists()) internalFile.delete()
                    if (!tempCopy.renameTo(internalFile)) {
                        tempCopy.copyTo(internalFile, overwrite = true)
                        tempCopy.delete()
                    }
                    return@withContext internalFile
                } else {
                    tempCopy.delete()
                }
            }
        }
    }

    // 3. استعلام MediaStore.Downloads للبحث عن أحدث ملف PDF في التنزيلات
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        runCatching {
            val resolver = context.contentResolver
            val projection = arrayOf(
                MediaStore.Downloads._ID,
                MediaStore.Downloads.DISPLAY_NAME,
                MediaStore.Downloads.SIZE,
                MediaStore.Downloads.DATE_MODIFIED
            )
            val selection = "${MediaStore.Downloads.SIZE} > ?"
            val selectionArgs = arrayOf(MIN_VALID_BOOK_BYTES.toString())
            val sortOrder = "${MediaStore.Downloads.DATE_MODIFIED} DESC"

            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                var recentFallbackId: Long? = null

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val displayName = cursor.getString(nameCol).orEmpty()
                    val dateModifiedMs = cursor.getLong(dateCol) * 1000L
                    val isPdfName = displayName.lowercase(Locale.ROOT).endsWith(".pdf")

                    if (matchesBookFileName(displayName) || (fileNum.isNotBlank() && displayName.startsWith(fileNum))) {
                        val contentUri = ContentUris.withAppendedId(
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            id
                        )
                        val imported = importPdfUriToInternalBookStorage(context, contentUri, book)
                        if (imported != null && isValidDownloadedCurriculumPdf(imported)) {
                            return@withContext imported
                        }
                    } else if (acceptRecentPdfSinceMs > 0L && isPdfName && recentFallbackId == null &&
                        dateModifiedMs >= (acceptRecentPdfSinceMs - 25_000L)
                    ) {
                        recentFallbackId = id
                    }
                }

                if (recentFallbackId != null) {
                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        recentFallbackId
                    )
                    val imported = importPdfUriToInternalBookStorage(context, contentUri, book)
                    if (imported != null && isValidDownloadedCurriculumPdf(imported)) {
                        return@withContext imported
                    }
                }
            }
        }
    }

    null
}

/**
 * فتح رابط الكتاب مباشرة في متصفح الهاتف الرسمي لتنزيله بسرعة وسهولة.
 */
private fun launchBrowserForBookDownload(
    context: Context,
    book: CurriculumBookItem,
    onError: (String) -> Unit
) {
    runCatching {
        val cleanUrl = book.normalizedPdfUrl
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cleanUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }.onFailure {
        onError("تعذر فتح المتصفح على هذا الجهاز")
    }
}

/**
 * حفظ نسخة من ملف الكتاب الموجود داخل التطبيق إلى مجلد التنزيلات العام في الهاتف.
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
    val fileName = "كتاب_${safeSubject}_${safeGrade}_${book.remoteFileNumber}.pdf"

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

    /**
     * اسم ملف الكتاب الأصلي في نهاية الرابط (مثال: `1881.pdf`)
     */
    val remoteFileName: String
        get() = normalizedPdfUrl.substringAfterLast("/").substringBefore("?").trim()

    /**
     * رقم ملف الكتاب الأصلي في الرابط (مثال: `1881`)
     */
    val remoteFileNumber: String
        get() = remoteFileName.substringBeforeLast(".").trim()
}

/**
 * الفهرس النصي الرسمي الكامل لروابط المناهج العراقية.
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
     * المسار الداخلي الرسمي لحفظ ملف الكتاب داخل التطبيق:
     * `File(context.filesDir, "books/book_${bookId}.pdf")`
     */
    fun getInternalBookFile(context: Context, bookId: String): File {
        val dir = File(context.filesDir, "books")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "book_${bookId}.pdf")
    }

    /**
     * المسار المحلي الرديف لفحص أي كتب محفوظة مسبقاً:
     */
    fun getLegacyExternalBookFile(context: Context, bookId: String): File {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: File(context.filesDir, "books")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "curriculum_${bookId}.pdf")
    }

    fun getLocalCurriculumBookFile(context: Context, bookId: String): File {
        val internal = getInternalBookFile(context, bookId)
        if (internal.exists() && internal.length() > MIN_VALID_BOOK_BYTES) {
            return internal
        }
        val legacy = getLegacyExternalBookFile(context, bookId)
        if (legacy.exists() && legacy.length() > MIN_VALID_BOOK_BYTES) {
            return legacy
        }
        return internal
    }
}

private data class ActiveReaderSessionRequest(
    val book: CurriculumBookItem,
    val launchBrowserOnOpenIfMissing: Boolean = false
)

@Composable
fun CurriculumBooksScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    // الكتاب الذي ينتظر التطبيق اكتمال تنزيله من المتصفح ليفتحه تلقائياً فور العودة
    var pendingAutoOpenBook by remember { mutableStateOf<CurriculumBookItem?>(null) }
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

    var localSavedBooksMap by remember {
        mutableStateOf<Map<String, Pair<Boolean, String>>>(emptyMap())
    }

    // فحص تلقائي لكتب الصف المعروض في الخلفية لتحديث شارة "متوفر للقراءة الفورية"
    LaunchedEffect(displayedBooks, cacheRefreshTick) {
        val updated = withContext(Dispatchers.IO) {
            displayedBooks.associate { book ->
                val found = autoFindAndImportBookPdf(context, book)
                val valid = found != null && isValidDownloadedCurriculumPdf(found)
                val sizeMb = if (valid && found != null) {
                    String.format(Locale.US, "%.1f MB", found.length() / (1024.0 * 1024.0))
                } else {
                    ""
                }
                book.id to (valid to sizeMb)
            }
        }
        localSavedBooksMap = updated
    }

    // مراقب تلقائي فور عودة المستخدم للتطبيق من المتصفح عبر Lifecycle.Event.ON_RESUME
    DisposableEffect(lifecycleOwner, pendingAutoOpenBook, displayedBooks) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    val targetBook = pendingAutoOpenBook
                    if (targetBook != null) {
                        val imported = autoFindAndImportBookPdf(context, targetBook)
                        if (imported != null && isValidDownloadedCurriculumPdf(imported)) {
                            pendingAutoOpenBook = null
                            cacheRefreshTick += 1
                            activeReaderSession = ActiveReaderSessionRequest(
                                book = targetBook,
                                launchBrowserOnOpenIfMissing = false
                            )
                            return@launch
                        }
                    }
                    cacheRefreshTick += 1
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // فحص دوري كل ثانية عندما يكون هناك كتاب قيد التنزيل عبر المتصفح لالتقاطه وفتحه تلقائياً
    LaunchedEffect(pendingAutoOpenBook) {
        val target = pendingAutoOpenBook ?: return@LaunchedEffect
        while (isActive && pendingAutoOpenBook?.id == target.id) {
            val found = autoFindAndImportBookPdf(context, target)
            if (found != null && isValidDownloadedCurriculumPdf(found)) {
                pendingAutoOpenBook = null
                cacheRefreshTick += 1
                activeReaderSession = ActiveReaderSessionRequest(
                    book = target,
                    launchBrowserOnOpenIfMissing = false
                )
                break
            }
            delay(1000L)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_books_screen_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) ترويسة قسم المناهج الدراسية الرسمية
        item(key = "curriculum_hero_header") {
            ThreeDHeroHeaderCard(
                title = "المناهج الدراسية الرسمية",
                subtitle = "المكتبة المنهجية الرسمية لوزارة التربية مع ميزة الالتقاط التلقائي والقراءة الفورية داخل التطبيق.",
                rightPillText = selectedStage.arabicTitle,
                rightPillIcon = Icons.Filled.AutoStories,
                leftPillText = "$selectedClass (${displayedBooks.size} كتاب)",
                leftPillIcon = Icons.Filled.MenuBook,
                illustrationType = ThreeDIllustrationType.DIGITAL_LIBRARY_ILLUMINATED_BOOK_3D,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // 2) إشعار الفلترة التلقائية لحساب الطالب وولي الأمر
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

        // 3) شريط اختيار المراحل الدراسية والصفوف والبحث الفوري (Zero Classic Icons)
        item(key = "curriculum_stages_and_classes_selector") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                border = BorderStroke(
                    1.dp,
                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                                    1.1.dp,
                                    if (isSelected) Color(0xFF10B981) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
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
                                    1.1.dp,
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

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("curriculum_search_input"),
                        shape = RoundedCornerShape(14.dp),
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

        // 4) عنوان قائمة كتب الصف
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

        // 5) قائمة بطاقات الكتب بتصميم مدرسي رسمي هادئ ونظيف
        if (displayedBooks.isEmpty()) {
            item(key = "empty_curriculum_search") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
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
                val isAvailableLocally = savedState?.first == true
                val fileSizeLabel = savedState?.second.orEmpty()

                CurriculumBookOfficialCard(
                    book = book,
                    isDarkMode = isDarkMode,
                    isAvailableLocally = isAvailableLocally,
                    fileSizeLabel = fileSizeLabel,
                    onPrimaryBookClick = {
                        coroutineScope.launch {
                            val existingFile = autoFindAndImportBookPdf(context, book)
                            if (existingFile != null && isValidDownloadedCurriculumPdf(existingFile)) {
                                cacheRefreshTick += 1
                                activeReaderSession = ActiveReaderSessionRequest(
                                    book = book,
                                    launchBrowserOnOpenIfMissing = false
                                )
                            } else {
                                // الحالة (2): إذا لم يكن الكتاب نازلاً بعد، يفتح رابط الكتاب مباشرة في المتصفح لتنزيله،
                                // ويشغّل نافذة الكتاب ومراقب ON_RESUME لالتقاطه وفتحه تلقائياً فور اكتمال تنزيله!
                                pendingAutoOpenBook = book
                                activeReaderSession = ActiveReaderSessionRequest(
                                    book = book,
                                    launchBrowserOnOpenIfMissing = true
                                )
                            }
                        }
                    },
                    onBrowserDownloadClick = {
                        pendingAutoOpenBook = book
                        activeReaderSession = ActiveReaderSessionRequest(
                            book = book,
                            launchBrowserOnOpenIfMissing = true
                        )
                    }
                )
            }
        }
    }

    // شاشة قارئ الكتاب المدمج داخل التطبيق (PdfRenderer + التقاط تلقائي من التنزيلات + اختيار يدوي مساند)
    val currentSession = activeReaderSession
    if (currentSession != null) {
        NativeInAppPdfReaderModal(
            sessionRequest = currentSession,
            onDismiss = {
                activeReaderSession = null
                cacheRefreshTick += 1
            },
            onBookDownloadedReady = {
                pendingAutoOpenBook = null
                cacheRefreshTick += 1
            },
            onShowToast = { msg, success ->
                viewModel.showToastAlert(message = msg, isSuccess = success)
            }
        )
    }
}

/**
 * بطاقة الكتاب المنهجي بتصميم مدرسي رسمي هادئ، نظيف، ومرتب (Zero Classic Icons Policy).
 * - تعرض شارة خضراء أنيقة `متوفر للقراءة الفورية` إذا كان الكتاب محفوظاً أو تم التقاطه من التنزيلات.
 * - تعرض رقم ملف الكتاب الرسمي (`ملف رقم 1881.pdf`) لتسهيل معرفته.
 */
@Composable
private fun CurriculumBookOfficialCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    isAvailableLocally: Boolean,
    fileSizeLabel: String,
    onPrimaryBookClick: () -> Unit,
    onBrowserDownloadClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onPrimaryBookClick() }
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isAvailableLocally) {
                Color(0xFF10B981).copy(alpha = 0.65f)
            } else if (isDarkMode) {
                Color(0xFF334155)
            } else {
                Color(0xFFE2E8F0)
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = book.subjectTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )

                // شارة الحالة الهادئة والواضحة
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isAvailableLocally) {
                        if (isDarkMode) Color(0xFF065F46) else Color(0xFFDCFCE7)
                    } else {
                        if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                    },
                    border = BorderStroke(
                        0.9.dp,
                        if (isAvailableLocally) Color(0xFF10B981) else Color(0xFF94A3B8).copy(alpha = 0.4f)
                    )
                ) {
                    Text(
                        text = if (isAvailableLocally) {
                            "متوفر للقراءة الفورية"
                        } else {
                            "ملف ${book.remoteFileName}"
                        },
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isAvailableLocally) {
                            if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        } else {
                            if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.6f) else Color(0xFFECFDF5),
                    border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "${book.stage.shortBadge} • ${book.gradeClass}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                if (isAvailableLocally && fileSizeLabel.isNotBlank()) {
                    Text(
                        text = "الحجم: $fileSizeLabel",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                    )
                }
            }

            // أزرار التحكم النصية الصافية (Zero Classic Icons)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = onPrimaryBookClick,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF059669),
                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                    modifier = Modifier
                        .weight(1.3f)
                        .height(42.dp)
                        .clip(RoundedCornerShape(50))
                        .testTag("btn_super_reader_${book.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isAvailableLocally) {
                                "فتح وقراءة الكتاب الآن"
                            } else {
                                "تنزيل وفتح الكتاب تلقائياً"
                            },
                            fontSize = 11.8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    onClick = onBrowserDownloadClick,
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF475569) else Color(0xFFCBD5E1)
                    ),
                    modifier = Modifier
                        .weight(0.85f)
                        .height(42.dp)
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
                            text = if (isAvailableLocally) "إعادة تنزيل / تحديث" else "فتح في المتصفح",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * شاشة قارئ كتب الـ PDF الأصلية المدمجة داخل التطبيق (`android.graphics.pdf.PdfRenderer`):
 * - بتصميم مدرسي رسمي هادئ ونظيف بدون أي شاشات مبالغ فيها أو نصوص مصطنعة.
 * - إذا كان الكتاب نازلاً في مجلد التطبيق أو في مجلد التنزيلات `Downloads` (باسم رقمه مثل `1881.pdf` أو `1881 (1).pdf`)،
 *   يلتقطه فوراً ويفتحه بـ `PdfRenderer`.
 * - إذا لم يكن نازلاً بعد، يفتح الرابط في المتصفح لتنزيله ويشغّل مراقب `Lifecycle.Event.ON_RESUME` + فحص دوري كل ثانية
 *   لالتقاط الملف تلقائياً فور نزوله وعرضه مباشرة داخل القارئ!
 * - يوفّر زر `اختيار ملف الكتاب من التنزيلات` (`ActivityResultContracts.OpenDocument`) كخيار مساند يحفظ الكتاب داخل التطبيق للأبد.
 */
@Composable
private fun NativeInAppPdfReaderModal(
    sessionRequest: ActiveReaderSessionRequest,
    onDismiss: () -> Unit,
    onBookDownloadedReady: () -> Unit,
    onShowToast: (String, Boolean) -> Unit
) {
    val book = sessionRequest.book
    val bookId = book.id
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val renderMutex = remember { Mutex() }

    var isScanningOrWaitingForBrowser by remember(bookId) { mutableStateOf(true) }
    var statusNoticeMessage by remember(bookId) { mutableStateOf<String?>(null) }
    var inAppBannerMessage by remember(bookId) { mutableStateOf<String?>(null) }

    var pdfFile by remember(bookId) { mutableStateOf<File?>(null) }
    var pdfRendererHolder by remember(bookId) { mutableStateOf<Pair<ParcelFileDescriptor, PdfRenderer>?>(null) }
    var pageCount by remember(bookId) { mutableIntStateOf(0) }

    var zoomScale by remember(bookId) { mutableFloatStateOf(1f) }
    var panOffsetX by remember(bookId) { mutableFloatStateOf(0f) }
    var showJumpToPageBar by remember(bookId) { mutableStateOf(false) }
    var jumpPageInput by remember(bookId) { mutableStateOf("") }
    val sessionStartTimeMs = remember(sessionRequest) { System.currentTimeMillis() }
    var activeDownloadId by remember(sessionRequest) { mutableStateOf<Long?>(null) }

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

    suspend fun openBookFileInRenderer(targetFile: File): Boolean {
        return try {
            val opened = withContext(Dispatchers.IO) {
                if (!isValidDownloadedCurriculumPdf(targetFile)) {
                    throw IllegalStateException("Invalid PDF file")
                }
                val pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                pfd to renderer
            }
            closeRendererSafely()
            pdfFile = targetFile
            pdfRendererHolder = opened
            pageCount = opened.second.pageCount
            isScanningOrWaitingForBrowser = false
            statusNoticeMessage = null
            onBookDownloadedReady()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            false
        }
    }

    // منتقي الملفات الرسمي (ActivityResultContracts.OpenDocument) لزر "اختيار ملف الكتاب من التنزيلات"
    val openPdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { selectedUri: Uri? ->
        if (selectedUri != null) {
            coroutineScope.launch {
                val importedFile = importPdfUriToInternalBookStorage(context, selectedUri, book)
                if (importedFile != null && openBookFileInRenderer(importedFile)) {
                    val msg = "تم استيراد وحفظ كتاب ${book.subjectTitle} داخل التطبيق بنجاح"
                    inAppBannerMessage = msg
                    onShowToast(msg, true)
                } else {
                    val err = "الملف المختار ليس ملف PDF صالحاً"
                    inAppBannerMessage = err
                    onShowToast(err, false)
                }
            }
        }
    }

    // WebView مدمج خفي لتوليد كوكيز الجلسة الشرعية لموقع manahj.edu.iq وتمريرها فوراً إلى DownloadManager
    if (pdfRendererHolder == null) {
        @SuppressLint("SetJavaScriptEnabled")
        AndroidView(
            modifier = Modifier.size(1.dp),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                    val cookieManager = CookieManager.getInstance()
                    cookieManager.setAcceptCookie(true)
                    cookieManager.setAcceptThirdPartyCookies(this, true)

                    setDownloadListener { url, userAgent, _, _, _ ->
                        if (activeDownloadId == null) {
                            val cookies = cookieManager.getCookie(url)
                                ?: cookieManager.getCookie("https://manahj.edu.iq")
                            val dlId = enqueueDirectBookDownloadWithSessionCookies(
                                context = ctx,
                                book = book,
                                downloadUrl = url,
                                cookies = cookies,
                                userAgent = userAgent
                            )
                            if (dlId != null) {
                                activeDownloadId = dlId
                            }
                        }
                    }

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView?,
                            request: WebResourceRequest?
                        ): Boolean {
                            val targetUrl = request?.url?.toString().orEmpty()
                            if (targetUrl.lowercase(Locale.ROOT).endsWith(".pdf") && activeDownloadId == null) {
                                val cookies = cookieManager.getCookie(targetUrl)
                                    ?: cookieManager.getCookie("https://manahj.edu.iq")
                                val dlId = enqueueDirectBookDownloadWithSessionCookies(
                                    context = ctx,
                                    book = book,
                                    downloadUrl = targetUrl,
                                    cookies = cookies,
                                    userAgent = view?.settings?.userAgentString
                                )
                                if (dlId != null) {
                                    activeDownloadId = dlId
                                    return true
                                }
                            }
                            return false
                        }

                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            if (activeDownloadId == null && !url.isNullOrBlank()) {
                                val cookies = cookieManager.getCookie(url)
                                    ?: cookieManager.getCookie("https://manahj.edu.iq")
                                val dlId = enqueueDirectBookDownloadWithSessionCookies(
                                    context = ctx,
                                    book = book,
                                    downloadUrl = book.normalizedPdfUrl,
                                    cookies = cookies,
                                    userAgent = view?.settings?.userAgentString
                                )
                                if (dlId != null) {
                                    activeDownloadId = dlId
                                }
                            }
                        }
                    }

                    loadUrl(
                        book.normalizedPdfUrl,
                        mapOf("Referer" to "https://manahj.edu.iq/")
                    )
                }
            }
        )
    }

    // الفحص الأولي + الفحص الدوري كل 800ms لالتقاط الكتاب من DownloadManager أو من التنزيلات تلقائياً
    LaunchedEffect(sessionRequest) {
        val existing = autoFindAndImportBookPdf(context, book)
        if (existing != null && openBookFileInRenderer(existing)) {
            return@LaunchedEffect
        }

        isScanningOrWaitingForBrowser = true

        // إذا لم ينطلق DownloadManager خلال أول ثانيتين من الـ WebView، نقوم بجدولة التنزيل المباشر تلقائياً
        delay(1200L)
        if (pdfRendererHolder == null && activeDownloadId == null) {
            val cookies = runCatching {
                CookieManager.getInstance().getCookie(book.normalizedPdfUrl)
                    ?: CookieManager.getInstance().getCookie("https://manahj.edu.iq")
            }.getOrNull()
            activeDownloadId = enqueueDirectBookDownloadWithSessionCookies(
                context = context,
                book = book,
                downloadUrl = book.normalizedPdfUrl,
                cookies = cookies,
                userAgent = null
            )
        }

        while (isActive && pdfRendererHolder == null) {
            val scanned = autoFindAndImportBookPdf(
                context = context,
                book = book,
                acceptRecentPdfSinceMs = sessionStartTimeMs
            )
            if (scanned != null && openBookFileInRenderer(scanned)) {
                val msg = "تم تنزيل وفتح كتاب ${book.subjectTitle} تلقائياً"
                inAppBannerMessage = msg
                break
            }
            delay(800L)
        }
    }

    // مراقب ContentObserver حي على MediaStore.Downloads + مراقب حدث ON_RESUME لفتح الكتاب فوراً
    DisposableEffect(lifecycleOwner, bookId) {
        val downloadsObserver = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            object : ContentObserver(Handler(Looper.getMainLooper())) {
                override fun onChange(selfChange: Boolean) {
                    if (pdfRendererHolder == null) {
                        coroutineScope.launch {
                            val scanned = autoFindAndImportBookPdf(
                                context = context,
                                book = book,
                                acceptRecentPdfSinceMs = sessionStartTimeMs
                            )
                            if (scanned != null && openBookFileInRenderer(scanned)) {
                                inAppBannerMessage = "تم التقاط كتاب ${book.subjectTitle} من التنزيلات وفتحه تلقائياً"
                            }
                        }
                    }
                }
            }.also { obs ->
                runCatching {
                    context.contentResolver.registerContentObserver(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                        true,
                        obs
                    )
                }
            }
        } else {
            null
        }

        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pdfRendererHolder == null) {
                coroutineScope.launch {
                    val scanned = autoFindAndImportBookPdf(
                        context = context,
                        book = book,
                        acceptRecentPdfSinceMs = sessionStartTimeMs
                    )
                    if (scanned != null && openBookFileInRenderer(scanned)) {
                        inAppBannerMessage = "تم التقاط كتاب ${book.subjectTitle} وفتحه تلقائياً"
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            if (downloadsObserver != null) {
                runCatching {
                    context.contentResolver.unregisterContentObserver(downloadsObserver)
                }
            }
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
                // الشريط العلوي الرسمي للقارئ (Zero Classic Icons)
                Surface(
                    color = Color(0xFF064E3B),
                    shadowElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 24.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
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
                                        "${book.stage.arabicTitle} • ${book.gradeClass} • ملف (${book.remoteFileName})"
                                    },
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Surface(
                                onClick = {
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

                        // صف الأزرار المساندة النظيفة في رأس القارئ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                onClick = {
                                    openPdfDocumentLauncher.launch(arrayOf("application/pdf"))
                                },
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF059669),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.7f)),
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(50))
                                    .testTag("reader_pick_from_downloads_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "اختيار ملف الكتاب من التنزيلات",
                                        fontSize = 10.8.sp,
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
                                    val currentLocal = pdfFile
                                    if (currentLocal != null && isValidDownloadedCurriculumPdf(currentLocal)) {
                                        coroutineScope.launch {
                                            val savedOk = saveBookFileToPublicDownloads(context, currentLocal, book)
                                            if (savedOk) {
                                                val msg = "تم حفظ نسخة من كتاب ${book.subjectTitle} في التنزيلات"
                                                inAppBannerMessage = msg
                                                onShowToast(msg, true)
                                            }
                                        }
                                    } else {
                                        launchBrowserForBookDownload(
                                            context = context,
                                            book = book,
                                            onError = { err -> statusNoticeMessage = err }
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
                                    .testTag("reader_browser_or_save_btn")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (pdfFile != null) "حفظ نسخة في التنزيلات" else "تنزيل الكتاب عبر المتصفح",
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

                        AnimatedVisibility(
                            visible = !inAppBannerMessage.isNullOrBlank(),
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
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

                        // شريط الانتقال السريع لأي صفحة
                        AnimatedVisibility(visible = showJumpToPageBar && pageCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFF0F172A).copy(alpha = 0.82f),
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
                                                    text = "انتقال",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        val jumpActions = listOf(
                                            "الأولى" to 0,
                                            "السابق 10-" to (currentVisiblePage - 11).coerceAtLeast(0),
                                            "التالي 10+" to (currentVisiblePage + 9).coerceAtMost((pageCount - 1).coerceAtLeast(0)),
                                            "الأخيرة" to (pageCount - 1).coerceAtLeast(0)
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

                // منطقة عرض صفحات الكتاب أو بطاقة الالتقاط التلقائي النظيفة والهادئة
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
                    if (pdfRendererHolder != null && pageCount > 0) {
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

                        // شريط التكبير والتصغير السفلي
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .navigationBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF064E3B).copy(alpha = 0.95f),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.6f)),
                            shadowElevation = 10.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
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
                    } else {
                        // بطاقة رسمية هادئة ونظيفة أثناء تنزيل الملف من المتصفح أو اختياره من التنزيلات
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 22.dp)
                                .fillMaxWidth()
                                .testTag("browser_auto_scan_book_card"),
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            shadowElevation = 8.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF064E3B),
                                    border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                                ) {
                                    Text(
                                        text = "${book.stage.arabicTitle} • ${book.gradeClass}",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFA7F3D0),
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                    )
                                }

                                Text(
                                    text = "كتاب ${book.subjectTitle}",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )

                                if (isScanningOrWaitingForBrowser) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF10B981),
                                        strokeWidth = 3.dp,
                                        modifier = Modifier
                                            .padding(vertical = 4.dp)
                                            .size(36.dp)
                                    )
                                }

                                Text(
                                    text = statusNoticeMessage
                                        ?: "يتم تنزيل ملف الكتاب (${book.remoteFileName}) عبر المتصفح.\nبمجرد اكتمال التنزيل والعودة للتطبيق، سيقوم التطبيق بالتقاط الملف وفتحه تلقائياً هنا للقراءة الدائمة.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )

                                // زر إعادة فتح رابط الكتاب في المتصفح
                                Surface(
                                    onClick = {
                                        launchBrowserForBookDownload(
                                            context = context,
                                            book = book,
                                            onError = { err -> statusNoticeMessage = err }
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF059669),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .testTag("open_book_in_browser_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "تنزيل الكتاب السريع عبر المتصفح (${book.remoteFileName})",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // زر مساند: اختيار ملف الكتاب من التنزيلات (ActivityResultContracts.OpenDocument)
                                Surface(
                                    onClick = {
                                        openPdfDocumentLauncher.launch(arrayOf("application/pdf"))
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFFFEF08A).copy(alpha = 0.65f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(50))
                                        .testTag("pick_book_pdf_from_downloads_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "اختيار ملف الكتاب من التنزيلات",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFEF08A)
                                        )
                                    }
                                }

                                Surface(
                                    onClick = {
                                        closeRendererSafely()
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .clip(RoundedCornerShape(50))
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "العودة لقائمة الكتب",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
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
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                        modifier = Modifier.size(26.dp)
                    )
                    Text(
                        text = "صفحة ${pageIndex + 1} من $totalPages",
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
                    text = "يضم ${IraqiCurriculumCatalog.allBooks.size} كتاباً منهجياً رسمياً مع ميزة الالتقاط التلقائي من التنزيلات والقارئ المدمج داخل التطبيق",
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
                                .padding(top = 24.dp, bottom = 12.dp, start = 14.dp, end = 14.dp),
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
