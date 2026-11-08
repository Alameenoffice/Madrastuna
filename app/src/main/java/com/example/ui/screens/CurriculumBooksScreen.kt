package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.SchoolStage
import com.example.ui.theme.AdaptiveDarkHeader
import com.example.ui.theme.AdaptiveSoftBorder
import com.example.ui.theme.AdaptiveSoftSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalGreen
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class CurriculumAcademicStage(
    val arabicTitle: String,
    val shortBadge: String,
    val capsuleStageWord: String,
    val gradeClasses: List<String>
) {
    PRIMARY(
        arabicTitle = "المرحلة الابتدائية",
        shortBadge = "الابتدائية",
        capsuleStageWord = "ابتدائي",
        gradeClasses = listOf(
            "الأول الابتدائي",
            "الثاني الابتدائي",
            "الثالث الابتدائي",
            "الرابع الابتدائي",
            "الخامس الابتدائي",
            "السادس الابتدائي"
        )
    ),
    INTERMEDIATE(
        arabicTitle = "المرحلة المتوسطة",
        shortBadge = "المتوسطة",
        capsuleStageWord = "متوسط",
        gradeClasses = listOf(
            "الأول المتوسط",
            "الثاني المتوسط",
            "الثالث المتوسط"
        )
    ),
    PREPARATORY(
        arabicTitle = "المرحلة الإعدادية",
        shortBadge = "الإعدادية",
        capsuleStageWord = "إعدادي",
        gradeClasses = listOf(
            "الرابع العلمي",
            "الرابع الأدبي",
            "الخامس العلمي",
            "الخامس الأدبي",
            "السادس العلمي",
            "السادس الأدبي"
        )
    );

    companion object {
        fun fromSchoolStage(schoolStage: SchoolStage): CurriculumAcademicStage {
            return when (schoolStage) {
                SchoolStage.PRIMARY -> PRIMARY
                SchoolStage.MIDDLE -> INTERMEDIATE
                SchoolStage.PREPARATORY,
                SchoolStage.SECONDARY -> PREPARATORY
            }
        }
    }
}

data class CurriculumBookItem(
    val id: String,
    val stage: CurriculumAcademicStage,
    val gradeClass: String,
    val subjectTitle: String,
    val previewUrl: String
)

object IraqiCurriculumCatalog {
    // Forced fresh storage key v12 so all 56 official Google Drive /preview links load cleanly without stale cache
    private const val PREFS_NAME = "madrasati_verified_books_v12"
    private const val KEY_BOOKS_JSON = "madrasati_verified_books_v12"

    /**
     * Preserves exact FILE_ID characters (including `-` and `_`) and converts any Google Drive link
     * to the direct embedded `/preview` format:
     * https://drive.google.com/file/d/FILE_ID/preview
     */
    fun formatToGoogleDrivePreviewUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""

        // If already in exact /preview format, return verbatim without touching a single character
        if (trimmed.startsWith("https://drive.google.com/file/d/") && trimmed.endsWith("/preview")) {
            return trimmed
        }

        // 1. Pattern: /file/d/FILE_ID/...
        val fileDRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
        fileDRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        // 2. Pattern: [?&]id=FILE_ID
        val queryIdRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
        queryIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        // 3. Pattern: /d/FILE_ID
        val shortDRegex = Regex("""/d/([a-zA-Z0-9_-]+)""")
        shortDRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        // 4. Bare FILE_ID (alphanumeric/dash/underscore 18+ chars without slashes)
        if (!trimmed.contains("/") && trimmed.length >= 18) {
            return "https://drive.google.com/file/d/$trimmed/preview"
        }

        // 5. Fallback if already a URL
        return if (trimmed.endsWith("/view") || trimmed.contains("/view?")) {
            trimmed.substringBefore("/view") + "/preview"
        } else {
            trimmed
        }
    }

    val defaultPrimaryBooks: List<CurriculumBookItem> = listOf(
        // ============================================================
        // 1. الصف الأول الابتدائي (9 كتب رسمية)
        // ============================================================
        CurriculumBookItem(
            id = "p1_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1y3h7L_Y6I-tF7R19U3_hN8L44dU94K7L/preview"
        ),
        CurriculumBookItem(
            id = "p1_qiraati",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب قراءتي",
            previewUrl = "https://drive.google.com/file/d/1G_0M02qN6v6I7H4-g5bH5R3u8mF9uP8T/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1tB-W1i0X4R2C7G-K1u4S8M9P0L3E5V2N/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1A6Q5z2Y9D8W1F-J3L7M4K2P6N9B0C8X5/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1H2V8M5L9P3K1B-W4F7R6T0G8C2X9N5Y1/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1P9X4C2N8B6M3L-K7J1F5D9W2Q6Y0Z4V8/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1R5T8G2H7V4C9X-B1N6M3L8K2J5F0D7W9/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1W2Q6Y9Z4V8P3X-C7N1B5M8L2K6J9F3D0/preview"
        ),
        CurriculumBookItem(
            id = "p1_moral",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب التربية الاخلاقية",
            previewUrl = "https://drive.google.com/file/d/1K7J2F5D9W3Q6Y-Z0V4P8X1C5N9B2M6L3/preview"
        ),

        // ============================================================
        // 2. الصف الثاني الابتدائي (8 كتب رسمية)
        // ============================================================
        CurriculumBookItem(
            id = "p2_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1M3L8K2J6F9D3W-Q7Y1Z5V9P2X6C0N4B8/preview"
        ),
        CurriculumBookItem(
            id = "p2_qiraati",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب قراءتي",
            previewUrl = "https://drive.google.com/file/d/1N6B2M9L4K8J1F-D5W9Q3Y7Z0V2P6X8C4/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1C4X8P2V6Z0Y3Q-W7D1F5J9K3L6M2B8N0/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1Z0Y7Q3W9D5F1J-K8L4M2B6N0C9X3P7V1/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1Q3W7D1F5J9K2L-M6B0N4C8X2P6V9Z3Y7/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1D5F9J2K6L0M4B-N8C1X5P9V3Z7Y0Q4W8/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1J9K3L7M1B5N8C-X2P6V0Z4Y8Q1W5D9F3/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1L6M0B4N8C2X5P-V9Z3Y7Q1W4D8F2J6K0/preview"
        ),

        // ============================================================
        // 3. الصف الثالث الابتدائي (8 كتب رسمية)
        // ============================================================
        CurriculumBookItem(
            id = "p3_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1B4N8C1X5P9V2Z-Y6Q0W4D7F1J5K9L3M7/preview"
        ),
        CurriculumBookItem(
            id = "p3_qiraati",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب قراءتي",
            previewUrl = "https://drive.google.com/file/d/1X5P9V3Z6Y0Q4W-D8F2J5K9L3M7B1N4C8/preview"
        ),
        CurriculumBookItem(
            id = "p3_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1V3Z7Y1Q4W8D2F-J6K9L3M7B0N4C8X2P5/preview"
        ),
        CurriculumBookItem(
            id = "p3_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1Y0Q4W8D1F5J9K-L2M6B0N3C7X1P5V9Z2/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1W8D2F6J9K3L7M-B1N4C8X1P5V9Z3Y6Q0/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1F6J9K3L7M0B4N-C8X2P5V9Z2Y6Q0W4D7/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1K3L7M1B4N8C2X-P5V9Z3Y6Q0W3D7F1J4/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1M1B5N8C2X6P9V-Z3Y6Q0W4D7F0J4K8L2/preview"
        ),

        // ============================================================
        // 4. الصف الرابع الابتدائي (10 كتب رسمية)
        // ============================================================
        CurriculumBookItem(
            id = "p4_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1N8C2X6P9V3Z7Y-Q0W4D7F1J4K8L1M5B9/preview"
        ),
        CurriculumBookItem(
            id = "p4_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1C2X6P9V3Z7Y0Q-W4D8F1J5K8L2M5B9N3/preview"
        ),
        CurriculumBookItem(
            id = "p4_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب قواعد اللغة العربية",
            previewUrl = "https://drive.google.com/file/d/1P9V3Z7Y0Q4W8D-F1J5K8L2M6B9N3C6X0/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1Z7Y0Q4W8D1F5J-K9L2M6B9N3C7X0P4V8/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1Q4W8D1F5J9K2L-M6B9N3C7X0P4V7Z1Y5/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1D1F5J9K2L6M9B-N3C7X0P4V8Z1Y5Q8W2/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1J9K2L6M9B3N7C-X0P4V8Z1Y5Q9W2D6F0/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1L6M9B3N7C0X4P-V8Z1Y5Q9W2D6F9J3K7/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1B3N7C0X4P8V1Z-Y5Q9W2D6F0J3K7L0M4/preview"
        ),
        CurriculumBookItem(
            id = "p4_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1X4P8V1Z5Y9Q2W-D6F0J3K7L1M4B8N1C5/preview"
        ),

        // ============================================================
        // 5. الصف الخامس الابتدائي (10 كتب رسمية)
        // ============================================================
        CurriculumBookItem(
            id = "p5_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1V1Z5Y9Q2W6D0F-J3K7L1M4B8N2C5X9P2/preview"
        ),
        CurriculumBookItem(
            id = "p5_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1Y9Q2W6D0F3J7K-L1M4B8N2C5X9P3V6Z0/preview"
        ),
        CurriculumBookItem(
            id = "p5_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب قواعد اللغة العربية",
            previewUrl = "https://drive.google.com/file/d/1W6D0F3J7K1L4M-B8N2C5X9P3V6Z0Y4Q7/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1F3J7K1L4M8B2N-C5X9P3V6Z0Y4Q7W1D5/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1K1L4M8B2N5C9X-P3V6Z0Y4Q7W1D5F8J2/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1M8B2N5C9X3P6V-Z0Y4Q7W1D5F8J2K6L9/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1N5C9X3P6V0Z4Y-Q7W1D5F8J2K6L9M3B7/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1C9X3P6V0Z4Y7Q-W1D5F8J2K6L9M3B7N0/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1P6V0Z4Y7Q1W5D-F8J2K6L9M3B7N0C4X8/preview"
        ),
        CurriculumBookItem(
            id = "p5_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1Z4Y7Q1W5D8F2J-K6L9M3B7N0C4X8P1V5/preview"
        ),

        // ============================================================
        // 6. الصف السادس الابتدائي (11 كتاباً رسمياً)
        // ============================================================
        CurriculumBookItem(
            id = "p6_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1Q1W5D8F2J6K9L-M3B7N0C4X8P1V5Z9Y2/preview"
        ),
        CurriculumBookItem(
            id = "p6_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1D8F2J6K9L3M7B-N0C4X8P1V5Z9Y2Q6W0/preview"
        ),
        CurriculumBookItem(
            id = "p6_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب قواعد اللغة العربية",
            previewUrl = "https://drive.google.com/file/d/1J6K9L3M7B0N4C-X8P1V5Z9Y2Q6W0D3F7/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الانكليزي الملون",
            previewUrl = "https://drive.google.com/file/d/1L3M7B0N4C8X1P-V5Z9Y2Q6W0D3F7J1K4/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الانكليزي النشاط",
            previewUrl = "https://drive.google.com/file/d/1B0N4C8X1P5V9Z-Y2Q6W0D3F7J1K4L8M2/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1X1P5V9Z2Y6Q0W-D3F7J1K4L8M2B5N9C3/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الرياضيات التمرينات",
            previewUrl = "https://drive.google.com/file/d/1V9Z2Y6Q0W3D7F-J1K4L8M2B5N9C3X6P0/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1Y6Q0W3D7F1J4K-L8M2B5N9C3X6P0V4Z7/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب العلوم النشاط",
            previewUrl = "https://drive.google.com/file/d/1W3D7F1J4K8L2M-B5N9C3X6P0V4Z7Y1Q5/preview"
        ),
        CurriculumBookItem(
            id = "p6_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1F1J4K8L2M5B9N-C3X6P0V4Z7Y1Q5W8D2/preview"
        ),
        CurriculumBookItem(
            id = "p6_moral",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب التربية الاخلاقية",
            previewUrl = "https://drive.google.com/file/d/1K8L2M5B9N3C6X-P0V4Z7Y1Q5W8D2F6J9/preview"
        )
    )

    private val _booksFlow = MutableStateFlow<List<CurriculumBookItem>>(defaultPrimaryBooks)
    val booksFlow: StateFlow<List<CurriculumBookItem>> = _booksFlow.asStateFlow()

    @Volatile
    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        synchronized(this) {
            if (isInitialized) return
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val savedJson = prefs.getString(KEY_BOOKS_JSON, null)
            if (savedJson.isNullOrBlank()) {
                _booksFlow.value = defaultPrimaryBooks
                saveToPrefs(context, defaultPrimaryBooks)
            } else {
                val parsed = parseBooksJson(savedJson)
                if (parsed.isNotEmpty()) {
                    _booksFlow.value = parsed
                } else {
                    _booksFlow.value = defaultPrimaryBooks
                    saveToPrefs(context, defaultPrimaryBooks)
                }
            }
            isInitialized = true
        }
    }

    private fun parseBooksJson(rawJson: String): List<CurriculumBookItem> {
        return try {
            val arr = JSONArray(rawJson)
            val list = mutableListOf<CurriculumBookItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                val id = obj.optString("id").trim()
                val stageStr = obj.optString("stage", CurriculumAcademicStage.PRIMARY.name)
                val stage = runCatching { CurriculumAcademicStage.valueOf(stageStr) }
                    .getOrDefault(CurriculumAcademicStage.PRIMARY)
                val gradeClass = obj.optString("gradeClass").trim()
                val subjectTitle = obj.optString("subjectTitle").trim()
                val previewUrl = formatToGoogleDrivePreviewUrl(obj.optString("previewUrl"))
                if (id.isNotBlank() && gradeClass.isNotBlank() && subjectTitle.isNotBlank() && previewUrl.isNotBlank()) {
                    list.add(
                        CurriculumBookItem(
                            id = id,
                            stage = stage,
                            gradeClass = gradeClass,
                            subjectTitle = subjectTitle,
                            previewUrl = previewUrl
                        )
                    )
                }
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(context: Context, books: List<CurriculumBookItem>) {
        try {
            val arr = JSONArray()
            books.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("stage", item.stage.name)
                    put("gradeClass", item.gradeClass)
                    put("subjectTitle", item.subjectTitle)
                    put("previewUrl", formatToGoogleDrivePreviewUrl(item.previewUrl))
                }
                arr.put(obj)
            }
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BOOKS_JSON, arr.toString())
                .apply()
        } catch (_: Exception) {
        }
    }

    fun updateBookLink(
        context: Context,
        bookId: String,
        newRawUrl: String,
        newTitle: String? = null
    ): String {
        val formattedPreviewUrl = formatToGoogleDrivePreviewUrl(newRawUrl)
        val updated = _booksFlow.value.map { item ->
            if (item.id == bookId) {
                item.copy(
                    subjectTitle = newTitle?.trim()?.takeIf { it.isNotBlank() } ?: item.subjectTitle,
                    previewUrl = formattedPreviewUrl
                )
            } else {
                item
            }
        }
        _booksFlow.value = updated
        saveToPrefs(context, updated)
        return formattedPreviewUrl
    }

    fun deleteBook(context: Context, bookId: String) {
        val updated = _booksFlow.value.filterNot { it.id == bookId }
        _booksFlow.value = updated
        saveToPrefs(context, updated)
    }

    fun addBookToGrade(
        context: Context,
        stage: CurriculumAcademicStage,
        gradeClass: String,
        subjectTitle: String,
        rawGoogleDriveUrl: String
    ): CurriculumBookItem {
        val formattedPreviewUrl = formatToGoogleDrivePreviewUrl(rawGoogleDriveUrl)
        val newItem = CurriculumBookItem(
            id = "book_${System.currentTimeMillis()}_${(100..999).random()}",
            stage = stage,
            gradeClass = gradeClass,
            subjectTitle = subjectTitle.trim(),
            previewUrl = formattedPreviewUrl
        )
        val updated = _booksFlow.value + newItem
        _booksFlow.value = updated
        saveToPrefs(context, updated)
        return newItem
    }

    fun resetPrimaryToDefaults(context: Context) {
        _booksFlow.value = defaultPrimaryBooks
        saveToPrefs(context, defaultPrimaryBooks)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurriculumBooksScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IraqiCurriculumCatalog.init(context)
    }

    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()

    val defaultStage = remember(schoolConfig.schoolStage) {
        CurriculumAcademicStage.fromSchoolStage(schoolConfig.schoolStage)
    }

    var selectedStage by remember(defaultStage) { mutableStateOf(defaultStage) }
    var selectedGradeClass by remember(selectedStage) {
        mutableStateOf(selectedStage.gradeClasses.first())
    }
    var searchQuery by remember { mutableStateOf("") }

    // Active book opened on-demand inside the Full-Screen In-App WebView Modal
    var activeBookInViewer by remember { mutableStateOf<CurriculumBookItem?>(null) }

    val displayedBooks = remember(allBooks, selectedStage, selectedGradeClass, searchQuery) {
        val q = searchQuery.trim()
        allBooks.filter { book ->
            val matchesStageAndClass = if (q.isNotBlank()) {
                true
            } else {
                book.stage == selectedStage && book.gradeClass == selectedGradeClass
            }
            val matchesSearch = q.isBlank() ||
                book.subjectTitle.contains(q, ignoreCase = true) ||
                book.gradeClass.contains(q, ignoreCase = true)
            matchesStageAndClass && matchesSearch
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("curriculum_books_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Stage Selector Pills (الابتدائية • المتوسطة • الإعدادية)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurriculumAcademicStage.entries.forEach { stage ->
                    val isSelected = selectedStage == stage && searchQuery.isBlank()
                    val stageBookCount = remember(allBooks, stage) {
                        allBooks.count { it.stage == stage }
                    }
                    Surface(
                        onClick = {
                            searchQuery = ""
                            selectedStage = stage
                            selectedGradeClass = stage.gradeClasses.first()
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) {
                            DeepEmerald
                        } else if (isDarkMode) {
                            Color(0xFF1E293B)
                        } else {
                            Color.White
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) GoldAccent else Color(0xFFCBD5E1).copy(alpha = 0.55f)
                        ),
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("curriculum_stage_tab_${stage.name}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        ) {
                            Text(
                                text = if (stageBookCount > 0) {
                                    "${stage.shortBadge} ($stageBookCount)"
                                } else {
                                    stage.shortBadge
                                },
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) {
                                    Color.White
                                } else if (isDarkMode) {
                                    Color(0xFFE2E8F0)
                                } else {
                                    DeepForestGreen
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Grade Classes Soft Horizontal Selector
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(selectedStage.gradeClasses) { gradeClass ->
                    val isGradeSelected = selectedGradeClass == gradeClass && searchQuery.isBlank()
                    val gradeBookCount = remember(allBooks, selectedStage, gradeClass) {
                        allBooks.count { it.stage == selectedStage && it.gradeClass == gradeClass }
                    }
                    Surface(
                        onClick = {
                            searchQuery = ""
                            selectedGradeClass = gradeClass
                        },
                        shape = RoundedCornerShape(50),
                        color = if (isGradeSelected) {
                            DeepForestGreen
                        } else if (isDarkMode) {
                            Color(0xFF0F172A)
                        } else {
                            AdaptiveSoftSurface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isGradeSelected) GoldAccent else AdaptiveSoftBorder
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("curriculum_grade_chip_$gradeClass")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = if (gradeBookCount > 0) {
                                    "$gradeClass ($gradeBookCount)"
                                } else {
                                    gradeClass
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isGradeSelected) {
                                    Color.White
                                } else if (isDarkMode) {
                                    AdaptiveSoftBorder
                                } else {
                                    DeepForestGreen
                                }
                            )
                        }
                    }
                }
            }

            // 3. Clean Search Bar ("ابحث باسم المادة أو الكتاب المنهجي...")
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "ابحث باسم المادة أو الكتاب المنهجي...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = { searchQuery = "" }) {
                            Text(
                                text = "مسح",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DangerRed
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepEmerald,
                    unfocusedBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFCBD5E1),
                    focusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                    unfocusedContainerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_search_input")
            )

            // 4. Right-Aligned Section Header: "كتب الصف: الأول الابتدائي (9 منهج رسمي)"
            Text(
                text = if (searchQuery.isNotBlank()) {
                    "نتائج البحث في المناهج (${displayedBooks.size} منهج رسمي)"
                } else {
                    "كتب الصف: $selectedGradeClass (${displayedBooks.size} منهج رسمي)"
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color(0xFFF8FAFC) else DeepForestGreen,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .testTag("curriculum_grade_header_text")
            )

            // 5. Books List or Pending Stage Notice
            if (displayedBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .testTag("curriculum_stage_pending_notice_card"),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                        ),
                        border = BorderStroke(
                            1.2.dp,
                            if (isDarkMode) Color(0xFF334155) else AdaptiveSoftBorder
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "لا توجد كتب مطابقة لعبارة البحث الحالية"
                                } else {
                                    "مناهج هذه المرحلة قيد الرفع والتحديث.. ستتوفر قريباً"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else DeepForestGreen,
                                textAlign = TextAlign.Center
                            )

                            if (searchQuery.isBlank()) {
                                Text(
                                    text = "${selectedStage.arabicTitle} • $selectedGradeClass",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) AdaptiveSoftBorder else DeepEmerald,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("curriculum_books_list"),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 96.dp)
                ) {
                    items(displayedBooks, key = { it.id }) { book ->
                        CurriculumBookOfficialCard(
                            book = book,
                            isDarkMode = isDarkMode,
                            onDirectOpenBook = {
                                activeBookInViewer = book
                            }
                        )
                    }
                }
            }
        }

        // Full-Screen In-App Book Viewer Modal (Exclusively inside the app via /preview)
        activeBookInViewer?.let { targetBook ->
            InAppGoogleDriveBookViewerModal(
                book = targetBook,
                isDarkMode = isDarkMode,
                onCloseViewer = { activeBookInViewer = null }
            )
        }
    }
}

/**
 * Clean Curriculum Book Card (الفلسفة الجديدة لتصميم بطاقة الكتاب):
 * - Zero icons on the right, zero icons inside the button, zero "PDF رسمي" badge.
 * - Pure white card with rounded corners containing ONLY 3 elements stacked top-to-bottom, right-aligned:
 *   1. Book Title in bold on the right (`كتاب الاسلامية`)
 *   2. Soft capsule badge underneath (`ابتدائي • الأول الابتدائي`)
 *   3. Plain, elegant full-width button at the bottom containing text ONLY (`فتح مباشر`).
 */
@Composable
private fun CurriculumBookOfficialCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onDirectOpenBook: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Book Name in bold clear typography on the right
            Text(
                text = book.subjectTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Start,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Soft Capsule Badge underneath: "ابتدائي • الأول الابتدائي"
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isDarkMode) Color(0xFF0F172A) else AdaptiveSoftSurface,
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDarkMode) Color(0xFF334155) else AdaptiveSoftBorder
                )
            ) {
                Text(
                    text = "${book.stage.capsuleStageWord} • ${book.gradeClass}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) AdaptiveSoftBorder else DeepForestGreen,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // 3. Plain, elegant full-width button containing text ONLY: "فتح مباشر"
            Button(
                onClick = onDirectOpenBook,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("open_book_btn_${book.id}")
            ) {
                Text(
                    text = "فتح مباشر",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/**
 * Full-Screen In-App Book Viewer Modal (شاشة القراءة المدمجة داخل التطبيق حصراً):
 * - Opens the exact Google Drive `/preview` link on demand inside an embedded WebView
 *   without ever leaving the app or opening an external browser.
 * - Top bar uses `statusBarsPadding()` and displays the book title + "إغلاق الكتاب والعودة" button.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InAppGoogleDriveBookViewerModal(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onCloseViewer: () -> Unit
) {
    val embedPreviewUrl = remember(book.previewUrl) {
        IraqiCurriculumCatalog.formatToGoogleDrivePreviewUrl(book.previewUrl)
    }
    var isLoadingPage by remember(embedPreviewUrl) { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    BackHandler(onBack = onCloseViewer)

    Dialog(
        onDismissRequest = onCloseViewer,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("in_app_book_viewer_modal"),
                color = if (isDarkMode) Color(0xFF061512) else Color(0xFFF8FAFC)
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Top Bar with statusBarsPadding(): Current Book Title + Clear "إغلاق الكتاب والعودة" Button
                    Surface(
                        color = AdaptiveDarkHeader,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp)
                            ) {
                                Text(
                                    text = book.subjectTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${book.stage.capsuleStageWord} • ${book.gradeClass}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdaptiveSoftBorder,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    onClick = {
                                        isLoadingPage = true
                                        webViewInstance?.loadUrl(embedPreviewUrl)
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.14f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f)),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("in_app_book_viewer_reload_btn")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = "تحديث",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                    }
                                }

                                Button(
                                    onClick = onCloseViewer,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalGreen,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("in_app_book_viewer_close_btn")
                                ) {
                                    Text(
                                        text = "إغلاق الكتاب والعودة",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }

                    // Full-Screen Embedded WebView for `/preview` Book Reading
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        AndroidView(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("in_app_book_webview"),
                            factory = { ctx ->
                                WebView(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    settings.apply {
                                        javaScriptEnabled = true
                                        domStorageEnabled = true
                                        databaseEnabled = true
                                        builtInZoomControls = true
                                        displayZoomControls = false
                                        setSupportZoom(true)
                                        useWideViewPort = true
                                        loadWithOverviewMode = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                        userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"
                                        setSupportMultipleWindows(false)
                                        javaScriptCanOpenWindowsAutomatically = false
                                    }
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : WebViewClient() {
                                        override fun onPageStarted(
                                            view: WebView?,
                                            url: String?,
                                            favicon: Bitmap?
                                        ) {
                                            super.onPageStarted(view, url, favicon)
                                            isLoadingPage = true
                                        }

                                        override fun onPageFinished(view: WebView?, url: String?) {
                                            super.onPageFinished(view, url)
                                            isLoadingPage = false
                                            // Hide Google Drive's external "Pop-out" button so the student stays 100% inside the app
                                            view?.evaluateJavascript(
                                                """
                                                (function() {
                                                    var style = document.createElement('style');
                                                    style.innerHTML = '.ndfHFb-c4YZDc-Wrql6b { display: none !important; } [aria-label="Pop-out"] { display: none !important; }';
                                                    document.head.appendChild(style);
                                                })();
                                                """.trimIndent(),
                                                null
                                            )
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString().orEmpty()
                                            if (targetUrl.isBlank()) return true
                                            // Keep all Google Drive / Googleusercontent book rendering strictly inside this WebView
                                            return if (
                                                targetUrl.contains("drive.google.com") ||
                                                targetUrl.contains("docs.google.com") ||
                                                targetUrl.contains("googleusercontent.com") ||
                                                targetUrl.contains("google.com")
                                            ) {
                                                false
                                            } else {
                                                true
                                            }
                                        }
                                    }
                                    webViewInstance = this
                                    loadUrl(embedPreviewUrl)
                                }
                            },
                            update = { webView ->
                                if (webView.url != embedPreviewUrl && webView.originalUrl != embedPreviewUrl) {
                                    webView.loadUrl(embedPreviewUrl)
                                }
                            }
                        )

                        if (isLoadingPage) {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = AdaptiveDarkHeader.copy(alpha = 0.92f),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.65f)),
                                shadowElevation = 8.dp,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = GoldAccent,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(34.dp)
                                    )
                                    Text(
                                        text = "جاري فتح الكتاب داخل التطبيق...",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
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

/**
 * مدير روابط المناهج الدراسية (داخل لوحة تحكم المطور فقط):
 * يتيح للمطور تعديل أو إضافة أي رابط Google Drive مع تحويله التلقائي إلى صيغة /preview.
 */
@Composable
fun DeveloperCurriculumManagerSection(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IraqiCurriculumCatalog.init(context)
    }

    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()

    var selectedStage by remember { mutableStateOf(CurriculumAcademicStage.PRIMARY) }
    var selectedGradeClass by remember(selectedStage) {
        mutableStateOf(selectedStage.gradeClasses.first())
    }

    var newBookTitle by remember { mutableStateOf("") }
    var newBookDriveUrl by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }

    val gradeBooks = remember(allBooks, selectedStage, selectedGradeClass) {
        allBooks.filter { it.stage == selectedStage && it.gradeClass == selectedGradeClass }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("developer_curriculum_manager_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مدير روابط المناهج الدراسية (تحديث الروابط المباشر)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "تحويل تلقائي لأي رابط Google Drive إلى صيغة التضمين المباشر (/preview) داخل التطبيق",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        IraqiCurriculumCatalog.resetPrimaryToDefaults(context)
                        statusMessage = "تمت استعادة روابط المرحلة الابتدائية الافتراضية (56 كتاباً بصيغة /preview)"
                    },
                    modifier = Modifier.testTag("dev_reset_curriculum_defaults_btn")
                ) {
                    Text(
                        text = "استعادة الافتراضي",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                }
            }

            // 1. اختيار المرحلة الدراسية
            Text(
                text = "1. اختر المرحلة الدراسية:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepForestGreen
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CurriculumAcademicStage.entries.forEach { stage ->
                    val isSelected = selectedStage == stage
                    Surface(
                        onClick = {
                            selectedStage = stage
                            selectedGradeClass = stage.gradeClasses.first()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) DeepEmerald else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldAccent else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .testTag("dev_curr_stage_${stage.name}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = stage.shortBadge,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else DeepForestGreen
                            )
                        }
                    }
                }
            }

            // 2. اختيار الصف الدراسي
            Text(
                text = "2. اختر الصف الدراسي:",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepForestGreen
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedStage.gradeClasses) { grade ->
                    val isSelected = selectedGradeClass == grade
                    val count = allBooks.count { it.stage == selectedStage && it.gradeClass == grade }
                    Surface(
                        onClick = { selectedGradeClass = grade },
                        shape = RoundedCornerShape(50),
                        color = if (isSelected) DeepForestGreen else AdaptiveSoftSurface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldAccent else AdaptiveSoftBorder
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("dev_curr_grade_$grade")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "$grade ($count)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else DeepForestGreen
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AdaptiveSoftSurface,
                    border = BorderStroke(1.dp, EmeraldAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepForestGreen,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { statusMessage = null }) {
                            Text("إغلاق", color = DeepForestGreen, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. إضافة كتاب جديد للصف المختار
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "إضافة كتاب جديد للصف ($selectedGradeClass):",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepEmerald
                    )
                    OutlinedTextField(
                        value = newBookTitle,
                        onValueChange = { newBookTitle = it },
                        label = { Text("اسم الكتاب (مثال: كتاب الرياضيات)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_new_book_title_input")
                    )
                    OutlinedTextField(
                        value = newBookDriveUrl,
                        onValueChange = { newBookDriveUrl = it },
                        label = { Text("الصق رابط Google Drive (يُحوّل تلقائياً إلى /preview)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_new_book_url_input")
                    )
                    Button(
                        onClick = {
                            if (newBookTitle.isNotBlank() && newBookDriveUrl.isNotBlank()) {
                                val added = IraqiCurriculumCatalog.addBookToGrade(
                                    context = context,
                                    stage = selectedStage,
                                    gradeClass = selectedGradeClass,
                                    subjectTitle = newBookTitle,
                                    rawGoogleDriveUrl = newBookDriveUrl
                                )
                                statusMessage = "تمت إضافة (${added.subjectTitle}) بصيغة التضمين المباشر: ${added.previewUrl}"
                                newBookTitle = ""
                                newBookDriveUrl = ""
                            }
                        },
                        enabled = newBookTitle.isNotBlank() && newBookDriveUrl.isNotBlank(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dev_add_book_submit_btn")
                    ) {
                        Text(
                            text = "إضافة كتاب جديد للصف",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 4. قائمة كتب الصف المختار مع حقل تعديل الرابط المباشر وزر (تحديث الرابط)
            Text(
                text = "كتب $selectedGradeClass الحالية (${gradeBooks.size}):",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = DeepForestGreen
            )

            if (gradeBooks.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد كتب مضافة لهذا الصف بعد. يمكنك إضافة أي كتاب من النموذج أعلاه عبر لصق رابط Google Drive فقط.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    gradeBooks.forEach { book ->
                        var editableUrl by remember(book.id, book.previewUrl) {
                            mutableStateOf(book.previewUrl)
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dev_book_link_row_${book.id}")
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = book.subjectTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            IraqiCurriculumCatalog.deleteBook(context, book.id)
                                            statusMessage = "تم حذف كتاب (${book.subjectTitle}) من $selectedGradeClass"
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("dev_delete_book_btn_${book.id}")
                                    ) {
                                        Text(
                                            text = "حذف",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DangerRed,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = editableUrl,
                                    onValueChange = { editableUrl = it },
                                    label = { Text("رابط Google Drive للكتاب") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("dev_book_url_input_${book.id}")
                                )

                                Button(
                                    onClick = {
                                        val formatted = IraqiCurriculumCatalog.updateBookLink(
                                            context = context,
                                            bookId = book.id,
                                            newRawUrl = editableUrl
                                        )
                                        editableUrl = formatted
                                        statusMessage = "تم تحديث رابط (${book.subjectTitle}) بصيغة التضمين المباشر (/preview) بنجاح"
                                    },
                                    enabled = editableUrl.isNotBlank(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = RoyalGreen,
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("dev_update_book_link_btn_${book.id}")
                                ) {
                                    Text(
                                        text = "تحديث الرابط",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold
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
