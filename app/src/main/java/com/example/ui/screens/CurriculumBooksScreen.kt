package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

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
    val fileKey: String
        get() = pdfUrl.substringAfterLast("/").substringBeforeLast(".pdf").ifBlank { id }
}

/**
 * الفهرس النصي الخفيف الكامل لروابط المناهج العراقية الرسمية (Strict On-Demand Only - Zero Pre-loading).
 * جميع العناصر هنا عبارة عن عناوين وروابط نصية فقط ولا يتم جلب أي ملف إلا عند ضغط المستخدم فعلياً.
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

    /**
     * Resolves the student's raw gradeClass (e.g. "الأول المتوسط", "الرابع الإعدادي", "الخامس الابتدائي")
     * to the matching CurriculumStageTab and exact class tab name for instant smart auto-filtering.
     */
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

    fun getCachedPdfFile(cacheDir: File, book: CurriculumBookItem): File {
        val dir = File(cacheDir, "curriculum_pdfs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "book_${book.fileKey}.pdf")
    }
}

@Composable
fun CurriculumBooksScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
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

    // Strict On-Demand Modals (Null until user explicitly taps a specific book!)
    var activeQuickPreviewBook by remember { mutableStateOf<CurriculumBookItem?>(null) }
    var activeInternalReaderBook by remember { mutableStateOf<CurriculumBookItem?>(null) }

    val displayedBooks = remember(selectedStage, selectedClass, searchQuery) {
        val cleanQuery = ArabicTextNormalizer.normalizeText(searchQuery.trim())
        if (cleanQuery.isBlank()) {
            IraqiCurriculumCatalog.allBooks.filter {
                it.stage == selectedStage && it.gradeClass == selectedClass
            }
        } else {
            // Search across the selected class first, or all books matching the query
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

    val cachedFilesSet = remember(cacheRefreshTick, displayedBooks) {
        displayedBooks.filter { book ->
            val f = IraqiCurriculumCatalog.getCachedPdfFile(context.cacheDir, book)
            f.exists() && f.length() > 1024L
        }.map { it.id }.toSet()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_books_screen_list"),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) Zero-Gap Wavy Emerald & Gold Header with Exclusive 3D Illuminated Digital Library Book Animation
        item(key = "curriculum_hero_header") {
            ThreeDHeroHeaderCard(
                title = "المناهج الدراسية الرسمية",
                subtitle = "المكتبة الرقمية الشاملة للمناهج العراقية (الابتدائية، المتوسطة، والإعدادية) بنظام الفتح الفوري عند الطلب.",
                rightPillText = selectedStage.arabicTitle,
                rightPillIcon = Icons.Filled.AutoStories,
                leftPillText = "$selectedClass (${displayedBooks.size} كتاب)",
                leftPillIcon = Icons.Filled.MenuBook,
                illustrationType = ThreeDIllustrationType.DIGITAL_LIBRARY_ILLUMINATED_BOOK_3D,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        // 2) Smart Auto-Filter Notice for Student & Parent Accounts
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFF059669),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "مناهج الطالب: ${activeStudent.fullNameWithFather} (${initialSelection.second})",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF065F46)
                                )
                                Text(
                                    text = "تم فتح مرحلة وصف الطالب تلقائياً للتسهيل، ويمكنك تصفح بقية المراحل والصفوف بحرية كاملة.",
                                    fontSize = 11.sp,
                                    color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857)
                                )
                            }
                        }

                        if (selectedStage != initialSelection.first || selectedClass != initialSelection.second) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF059669),
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
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3) Three Stages Glassmorphic Capsule Tabs + Class Sub-Pills + Instant Search Bar
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
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Stage Capsules Row (المرحلة الابتدائية | المرحلة المتوسطة | المرحلة الإعدادية)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurriculumStageTab.entries.forEach { stageTab ->
                            val isSelected = selectedStage == stageTab
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
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
                                        .fillMaxSize()
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
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stageTab.arabicTitle,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) Color.White else if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // Class Selection Strip for the Active Stage
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
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable {
                                        selectedClass = classTitle
                                    }
                                    .testTag("curriculum_class_pill_$classTitle"),
                                shape = RoundedCornerShape(14.dp),
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
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isClassSelected) Icons.Filled.CheckCircle else Icons.Filled.Class,
                                        contentDescription = null,
                                        tint = if (isClassSelected) {
                                            if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669)
                                        } else {
                                            if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
                                        },
                                        modifier = Modifier.size(16.dp)
                                    )
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

                    // Instant Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("curriculum_search_input"),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = "بحث في المناهج",
                                tint = Color(0xFF059669)
                            )
                        },
                        trailingIcon = {
                            if (searchQuery.isNotBlank()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "مسح البحث",
                                        tint = Color(0xFF64748B)
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

        // 4) Strict On-Demand Info Pill
        item(key = "on_demand_policy_notice") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Bolt,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = if (searchQuery.isBlank()) {
                            "كتب الصف: $selectedClass (${displayedBooks.size} منهج رسمي)"
                        } else {
                            "نتائج البحث الفوري (${displayedBooks.size} كتاب)"
                        },
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                }

                Text(
                    text = "تُفتح عند الطلب فقط",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF059669)
                )
            }
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
                        Icon(
                            imageVector = Icons.Filled.SearchOff,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(40.dp)
                        )
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
                val isCachedLocally = book.id in cachedFilesSet
                CurriculumBookLuxuryCard(
                    book = book,
                    isDarkMode = isDarkMode,
                    isCachedLocally = isCachedLocally,
                    onQuickPreviewClick = {
                        activeQuickPreviewBook = book
                    },
                    onOpenInternalReaderClick = {
                        activeInternalReaderBook = book
                    },
                    onDeleteCachedCopyClick = {
                        val file = IraqiCurriculumCatalog.getCachedPdfFile(context.cacheDir, book)
                        if (file.exists()) {
                            file.delete()
                        }
                        cacheRefreshTick += 1
                        viewModel.showToastAlert(
                            message = "تم حذف النسخة المؤقتة لكتاب (${book.subjectTitle}) وتفريغ المساحة بنجاح.",
                            isSuccess = true
                        )
                    }
                )
            }
        }
    }

    // Modal 1: Instant Cloud Preview (Google Docs Viewer WebView — Zero Download)
    val previewBook = activeQuickPreviewBook
    if (previewBook != null) {
        QuickCloudPdfPreviewModal(
            book = previewBook,
            onDismiss = { activeQuickPreviewBook = null },
            onSwitchToSuperReader = {
                val target = previewBook
                activeQuickPreviewBook = null
                activeInternalReaderBook = target
            }
        )
    }

    // Modal 2: High-Resolution On-Demand Internal PDF Reader (HttpURLConnection + PdfRenderer + Pinch-to-Zoom)
    val readerBook = activeInternalReaderBook
    if (readerBook != null) {
        InternalSuperPdfReaderModal(
            book = readerBook,
            onDismiss = {
                activeInternalReaderBook = null
                cacheRefreshTick += 1
            },
            onCacheDeleted = {
                cacheRefreshTick += 1
                viewModel.showToastAlert(
                    message = "تم حذف النسخة المؤقتة لكتاب (${readerBook.subjectTitle}) وتفريغ المساحة.",
                    isSuccess = true
                )
            }
        )
    }
}

@Composable
private fun CurriculumBookLuxuryCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    isCachedLocally: Boolean,
    onQuickPreviewClick: () -> Unit,
    onOpenInternalReaderClick: () -> Unit,
    onDeleteCachedCopyClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.2.dp,
            if (isCachedLocally) {
                Color(0xFF10B981).copy(alpha = 0.65f)
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Geometric 3D Book Vector Icon Box
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF065F46),
                                        Color(0xFF047857),
                                        Color(0xFF0F3D5E)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(30.dp)) {
                            val w = size.width
                            val h = size.height
                            // Geometric open book vector icon drawn on Canvas
                            val leftPage = Path().apply {
                                moveTo(w * 0.50f, h * 0.82f)
                                lineTo(w * 0.10f, h * 0.68f)
                                lineTo(w * 0.10f, h * 0.20f)
                                lineTo(w * 0.50f, h * 0.34f)
                                close()
                            }
                            val rightPage = Path().apply {
                                moveTo(w * 0.50f, h * 0.82f)
                                lineTo(w * 0.90f, h * 0.68f)
                                lineTo(w * 0.90f, h * 0.20f)
                                lineTo(w * 0.50f, h * 0.34f)
                                close()
                            }
                            drawPath(leftPage, color = Color.White.copy(alpha = 0.92f))
                            drawPath(rightPage, color = Color(0xFFA7F3D0))
                            drawLine(
                                color = Color(0xFFFBBF24),
                                start = Offset(w * 0.50f, h * 0.30f),
                                end = Offset(w * 0.50f, h * 0.86f),
                                strokeWidth = 2.2.dp.toPx()
                            )
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = book.subjectTitle,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                                border = BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                            ) {
                                Text(
                                    text = book.gradeClass,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDarkMode) Color(0xFF1E3A8A).copy(alpha = 0.45f) else Color(0xFFEFF6FF)
                            ) {
                                Text(
                                    text = book.stage.arabicTitle,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF93C5FD) else Color(0xFF1D4ED8),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                if (isCachedLocally) {
                    IconButton(
                        onClick = onDeleteCachedCopyClick,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF2F2))
                            .testTag("delete_cached_book_${book.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = "حذف النسخة المؤقتة",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            if (isCachedLocally) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.45f) else Color(0xFFF0FDF4),
                    border = BorderStroke(0.8.dp, Color(0xFF34D399).copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.OfflinePin,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "محفوظ في الكاش المؤقت للقراءة الفورية الفائقة",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                            )
                        }
                        Text(
                            text = "حذف النسخة المؤقتة",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFDC2626),
                            modifier = Modifier.clickable { onDeleteCachedCopyClick() }
                        )
                    }
                }
            }

            // Two On-Demand Action Buttons:
            // 1) "معاينة سريعة للكتاب" (Cloud Embedded Viewer without downloading)
            // 2) "فتح بالقارئ الداخلي الفائق" (On-Demand Cache + PdfRenderer High-Res Pinch-to-Zoom)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onQuickPreviewClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.2.dp, Color(0xFF059669)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .testTag("btn_quick_preview_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Visibility,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "معاينة سريعة للكتاب",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857),
                        maxLines = 1,
                        softWrap = false
                    )
                }

                Button(
                    onClick = onOpenInternalReaderClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF047857),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.18f)
                        .heightIn(min = 46.dp)
                        .testTag("btn_super_reader_${book.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = null,
                        tint = Color(0xFFFEF08A),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "فتح بالقارئ الداخلي الفائق",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

/**
 * الزر الأول: "معاينة سريعة للكتاب"
 * يفتح شاشة معاينة داخلية تعرض صفحات الكتاب مباشرة عبر عارض المستندات السحابي المدمج
 * (`https://docs.google.com/gview?embedded=true&url=<ENCODED_PDF_URL>`)
 * داخل WebView مهيأ بـ JavaScript و DOMStorage مع مؤشر تحميل أنيق، دون تنزيل الملف على ذاكرة الجهاز.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun QuickCloudPdfPreviewModal(
    book: CurriculumBookItem,
    onDismiss: () -> Unit,
    onSwitchToSuperReader: () -> Unit
) {
    val encodedPdfUrl = remember(book.pdfUrl) {
        Uri.encode(book.pdfUrl)
    }
    val viewerUrl = remember(encodedPdfUrl) {
        "https://docs.google.com/gview?embedded=true&url=$encodedPdfUrl"
    }

    var isLoading by remember { mutableStateOf(true) }
    var loadProgress by remember { mutableIntStateOf(15) }
    var hasLoadError by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Header Bar
                Surface(
                    color = Color(0xFF064E3B),
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = onDismiss,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.16f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق المعاينة",
                                        tint = Color.White,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = book.subjectTitle,
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${book.gradeClass} • معاينة سحابية فورية بدون تنزيل",
                                        fontSize = 11.sp,
                                        color = Color(0xFFA7F3D0),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        hasLoadError = false
                                        isLoading = true
                                        webViewRef?.loadUrl(viewerUrl)
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.14f))
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "إعادة تحميل المعاينة",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSwitchToSuperReader() }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.HighQuality,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "القارئ الفائق",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        AnimatedVisibility(visible = isLoading) {
                            LinearProgressIndicator(
                                progress = { (loadProgress / 100f).coerceIn(0.08f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(50)),
                                color = Color(0xFFFBBF24),
                                trackColor = Color.White.copy(alpha = 0.2f)
                            )
                        }
                    }
                }

                // WebView Container
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewRef = this
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    loadsImagesAutomatically = true
                                    useWideViewPort = true
                                    loadWithOverviewMode = true
                                    builtInZoomControls = true
                                    displayZoomControls = false
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        loadProgress = newProgress
                                        if (newProgress >= 95) {
                                            isLoading = false
                                        }
                                    }
                                }
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            hasLoadError = true
                                            isLoading = false
                                        }
                                    }
                                }
                                loadUrl(viewerUrl)
                            }
                        },
                        onRelease = { webView ->
                            webView.stopLoading()
                            webView.destroy()
                        }
                    )

                    if (isLoading) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF064E3B).copy(alpha = 0.94f),
                            border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.5f)),
                            shadowElevation = 12.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    color = Color(0xFFFBBF24),
                                    strokeWidth = 3.dp,
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "جاري تجهيز المعاينة السحابية لكتاب (${book.subjectTitle})...",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "يتم العرض مباشرة بدون تنزيل الملف على ذاكرة الهاتف ($loadProgress%)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA7F3D0),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    if (hasLoadError) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(24.dp),
                            shape = RoundedCornerShape(22.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.2.dp, Color(0xFFF87171))
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CloudOff,
                                    contentDescription = null,
                                    tint = Color(0xFFF87171),
                                    modifier = Modifier.size(38.dp)
                                )
                                Text(
                                    text = "تعذر تحميل عارض المستندات السحابي حالياً",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "يمكنك إعادة المحاولة أو فتح الكتاب مباشرة عبر القارئ الداخلي الفائق عالي الدقة.",
                                    fontSize = 12.sp,
                                    color = Color(0xFFCBD5E1),
                                    textAlign = TextAlign.Center
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            hasLoadError = false
                                            isLoading = true
                                            webViewRef?.loadUrl(viewerUrl)
                                        },
                                        border = BorderStroke(1.dp, Color.White)
                                    ) {
                                        Text("إعادة المحاولة", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = onSwitchToSuperReader,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                    ) {
                                        Text("فتح بالقارئ الفائق", fontWeight = FontWeight.ExtraBold)
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

/**
 * الزر الثاني: "فتح بالقارئ الداخلي الفائق"
 * عند الضغط عليه فقط، يقوم بجلب هذا الكتاب المحدد فقط عبر HttpURLConnection إلى الكاش المؤقت
 * مع عداد تقدم مئوي أنيق (0% - 100%) وقابلية الإلغاء في أي لحظة،
 * ثم يعرض صفحاته بدقة عالية عبر `android.graphics.pdf.PdfRenderer` داخل `LazyColumn`
 * مع دعم التكبير والتصغير السلس بإصبعين (Pinch-to-Zoom)،
 * مع توفير زر "حذف النسخة المؤقتة" لتفريغ المساحة متى شاء المستخدم.
 */
@Composable
private fun InternalSuperPdfReaderModal(
    book: CurriculumBookItem,
    onDismiss: () -> Unit,
    onCacheDeleted: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgressPercent by remember { mutableIntStateOf(0) }
    var downloadedMegabytesText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pageCount by remember { mutableIntStateOf(0) }
    var pdfRendererHolder by remember { mutableStateOf<Pair<ParcelFileDescriptor, PdfRenderer>?>(null) }
    val renderMutex = remember { Mutex() }
    var activeDownloadJob by remember { mutableStateOf<Job?>(null) }

    // Pinch-to-Zoom & Pan State
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    val listState = rememberLazyListState()

    fun closeRendererSafely() {
        val current = pdfRendererHolder
        pdfRendererHolder = null
        if (current != null) {
            runCatching { current.second.close() }
            runCatching { current.first.close() }
        }
    }

    fun startOnDemandDownloadOrOpen() {
        activeDownloadJob?.cancel()
        activeDownloadJob = coroutineScope.launch {
            errorMessage = null
            val targetFile = IraqiCurriculumCatalog.getCachedPdfFile(context.cacheDir, book)
            if (!targetFile.exists() || targetFile.length() <= 1024L) {
                isDownloading = true
                downloadProgressPercent = 0
                downloadedMegabytesText = "0.0 MB"
                val tempFile = File(targetFile.parentFile, "${targetFile.name}.part")
                try {
                    withContext(Dispatchers.IO) {
                        var currentUrl = book.pdfUrl
                        var redirects = 0
                        var connection: HttpURLConnection? = null
                        while (redirects < 5) {
                            ensureActive()
                            val urlObj = URL(currentUrl)
                            val conn = (urlObj.openConnection() as HttpURLConnection).apply {
                                connectTimeout = 20000
                                readTimeout = 30000
                                instanceFollowRedirects = true
                                requestMethod = "GET"
                                setRequestProperty("User-Agent", "Mozilla/5.0 (Android; MadrasatiSmartPlatform)")
                            }
                            conn.connect()
                            val code = conn.responseCode
                            if (code in 300..399) {
                                val location = conn.getHeaderField("Location")
                                conn.disconnect()
                                if (!location.isNullOrBlank()) {
                                    currentUrl = URL(urlObj, location).toString()
                                    redirects++
                                    continue
                                }
                            }
                            connection = conn
                            break
                        }

                        val finalConn = connection ?: throw IllegalStateException("تعذر الاتصال بخادم المناهج")
                        try {
                            if (finalConn.responseCode !in 200..299) {
                                throw IllegalStateException("رمز استجابة الخادم: ${finalConn.responseCode}")
                            }
                            val totalBytes = finalConn.contentLengthLong
                            var downloadedBytes = 0L
                            finalConn.inputStream.use { input ->
                                FileOutputStream(tempFile).use { output ->
                                    val buffer = ByteArray(8 * 1024)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } != -1) {
                                        if (!isActive) {
                                            throw CancellationException("تم إلغاء التحميل من قبل المستخدم")
                                        }
                                        output.write(buffer, 0, bytesRead)
                                        downloadedBytes += bytesRead
                                        val dlMb = downloadedBytes / (1024.0 * 1024.0)
                                        if (totalBytes > 0L) {
                                            val pct = ((downloadedBytes * 100L) / totalBytes).toInt().coerceIn(0, 100)
                                            val totalMb = totalBytes / (1024.0 * 1024.0)
                                            downloadProgressPercent = pct
                                            downloadedMegabytesText = String.format(
                                                Locale.US,
                                                "%.1f / %.1f MB",
                                                dlMb,
                                                totalMb
                                            )
                                        } else {
                                            downloadProgressPercent = (downloadProgressPercent + 2).coerceAtMost(95)
                                            downloadedMegabytesText = String.format(Locale.US, "%.1f MB", dlMb)
                                        }
                                    }
                                    output.flush()
                                }
                            }
                            if (targetFile.exists()) targetFile.delete()
                            tempFile.renameTo(targetFile)
                        } finally {
                            finalConn.disconnect()
                        }
                    }
                    downloadProgressPercent = 100
                    isDownloading = false
                } catch (ce: CancellationException) {
                    runCatching { if (tempFile.exists()) tempFile.delete() }
                    isDownloading = false
                    onDismiss()
                    return@launch
                } catch (e: Exception) {
                    runCatching { if (tempFile.exists()) tempFile.delete() }
                    isDownloading = false
                    errorMessage = "تعذر جلب ملف الكتاب (${e.localizedMessage ?: "خطأ في الشبكة"}). يمكنك المحاولة مجدداً."
                    return@launch
                }
            }

            // Open PdfRenderer on the cached file
            try {
                val opened = withContext(Dispatchers.IO) {
                    val pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    pfd to renderer
                }
                pdfFile = targetFile
                pdfRendererHolder = opened
                pageCount = opened.second.pageCount
            } catch (e: Exception) {
                runCatching { targetFile.delete() }
                errorMessage = "ملف الكتاب غير مكتمل أو تالف، يرجى إعادة التحميل."
            }
        }
    }

    LaunchedEffect(book.id) {
        startOnDemandDownloadOrOpen()
    }

    DisposableEffect(book.id) {
        onDispose {
            activeDownloadJob?.cancel()
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
            activeDownloadJob?.cancel()
            closeRendererSafely()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Bar with Book Title, Page Indicator, and "حذف النسخة المؤقتة" Button
                Surface(
                    color = Color(0xFF064E3B),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            IconButton(
                                onClick = {
                                    activeDownloadJob?.cancel()
                                    closeRendererSafely()
                                    onDismiss()
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.16f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق القارئ",
                                    tint = Color.White,
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.subjectTitle,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (pageCount > 0) {
                                        "${book.gradeClass} • صفحة $currentVisiblePage من $pageCount"
                                    } else {
                                        "${book.gradeClass} • القارئ الداخلي الفائق"
                                    },
                                    fontSize = 11.sp,
                                    color = Color(0xFFA7F3D0),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (pdfFile != null && !isDownloading) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFDC2626),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable {
                                        activeDownloadJob?.cancel()
                                        closeRendererSafely()
                                        val fileToDelete = IraqiCurriculumCatalog.getCachedPdfFile(context.cacheDir, book)
                                        if (fileToDelete.exists()) {
                                            fileToDelete.delete()
                                        }
                                        onCacheDeleted()
                                        onDismiss()
                                    }
                                    .testTag("reader_delete_cached_copy_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteSweep,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "حذف النسخة المؤقتة",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Main Content Area
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
                                        (panOffsetX + pan.x).coerceIn(-650f, 650f)
                                    } else {
                                        0f
                                    }
                                }
                            }
                        }
                ) {
                    when {
                        isDownloading -> {
                            // Sleek On-Demand Progress Dialog (0% - 100%) + Cancel Button
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(horizontal = 24.dp)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(26.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.4.dp, Color(0xFF10B981).copy(alpha = 0.6f)),
                                shadowElevation = 16.dp
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        CircularProgressIndicator(
                                            progress = { (downloadProgressPercent / 100f).coerceIn(0.04f, 1f) },
                                            modifier = Modifier.size(76.dp),
                                            color = Color(0xFF10B981),
                                            trackColor = Color(0xFF334155),
                                            strokeWidth = 6.dp
                                        )
                                        Text(
                                            text = "$downloadProgressPercent%",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFEF08A)
                                        )
                                    }

                                    Text(
                                        text = "جاري جلب كتاب (${book.subjectTitle}) عند الطلب",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )

                                    if (downloadedMegabytesText.isNotBlank()) {
                                        Text(
                                            text = downloadedMegabytesText,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA7F3D0)
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { (downloadProgressPercent / 100f).coerceIn(0.02f, 1f) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(RoundedCornerShape(50)),
                                        color = Color(0xFF10B981),
                                        trackColor = Color(0xFF334155)
                                    )

                                    Text(
                                        text = "يُحفظ في الكاش المؤقت لعرض الصفحات بدقة فائقة مع دعم التكبير بإصبعين.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFF94A3B8),
                                        textAlign = TextAlign.Center
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            activeDownloadJob?.cancel()
                                            isDownloading = false
                                            onDismiss()
                                        },
                                        shape = RoundedCornerShape(50),
                                        border = BorderStroke(1.2.dp, Color(0xFFF87171)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("cancel_on_demand_download_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Cancel,
                                            contentDescription = null,
                                            tint = Color(0xFFF87171),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "إلغاء التحميل والعودة",
                                            color = Color(0xFFFCA5A5),
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        errorMessage != null -> {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(24.dp),
                                shape = RoundedCornerShape(22.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.2.dp, DangerRed)
                            ) {
                                Column(
                                    modifier = Modifier.padding(22.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFF87171),
                                        modifier = Modifier.size(42.dp)
                                    )
                                    Text(
                                        text = errorMessage.orEmpty(),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = onDismiss,
                                            border = BorderStroke(1.dp, Color.White)
                                        ) {
                                            Text("إغلاق", color = Color.White)
                                        }
                                        Button(
                                            onClick = { startOnDemandDownloadOrOpen() },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                                        ) {
                                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إعادة المحاولة", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        pdfRendererHolder != null && pageCount > 0 -> {
                            val renderer = pdfRendererHolder!!.second
                            LazyColumn(
                                state = listState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = zoomScale
                                        scaleY = zoomScale
                                        translationX = panOffsetX
                                    },
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 14.dp),
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

                            // Floating Bottom Zoom & Page Bar
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .navigationBarsPadding()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(0xFF064E3B).copy(alpha = 0.94f),
                                border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.55f)),
                                shadowElevation = 10.dp
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    IconButton(
                                        onClick = {
                                            zoomScale = (zoomScale - 0.25f).coerceAtLeast(1f)
                                            if (zoomScale <= 1.02f) panOffsetX = 0f
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ZoomOut,
                                            contentDescription = "تصغير",
                                            tint = Color.White
                                        )
                                    }

                                    Text(
                                        text = "${(zoomScale * 100).toInt()}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = GoldAccent
                                    )

                                    IconButton(
                                        onClick = {
                                            zoomScale = (zoomScale + 0.25f).coerceAtMost(3.5f)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.ZoomIn,
                                            contentDescription = "تكبير",
                                            tint = Color.White
                                        )
                                    }

                                    if (zoomScale > 1.05f) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color.White.copy(alpha = 0.18f),
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    zoomScale = 1f
                                                    panOffsetX = 0f
                                                }
                                        ) {
                                            Text(
                                                text = "إعادة ضبط",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "صفحة $currentVisiblePage / $pageCount",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFA7F3D0)
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
                    contentDescription = "صفحة ${pageIndex + 1}",
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
                color = Color(0xFF0F172A).copy(alpha = 0.72f)
            ) {
                Text(
                    text = "${pageIndex + 1} / $totalPages",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
    }
}
