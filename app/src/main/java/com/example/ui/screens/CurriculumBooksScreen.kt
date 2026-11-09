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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
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
import com.example.model.UserRole
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class CurriculumAcademicStage(
    val arabicTitle: String,
    val shortBadge: String,
    val gradeClasses: List<String>
) {
    PRIMARY(
        arabicTitle = "المرحلة الابتدائية",
        shortBadge = "الابتدائية",
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
        gradeClasses = listOf(
            "الأول المتوسط",
            "الثاني المتوسط",
            "الثالث المتوسط"
        )
    ),
    PREPARATORY(
        arabicTitle = "المرحلة الإعدادية",
        shortBadge = "الإعدادية",
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
    private const val PREFS_NAME = "madrasati_curriculum_catalog_v3_official_prefs"
    private const val KEY_BOOKS_JSON = "curriculum_books_json_v3_official"

    /**
     * Extracts FILE_ID from any Google Drive link (even if it ends with /view?usp=sharing, /view, id=..., etc.)
     * and converts it automatically to the embedded /preview format:
     * https://drive.google.com/file/d/FILE_ID/preview
     */
    fun formatToGoogleDrivePreviewUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""

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

        // 4. Bare FILE_ID (alphanumeric/dash/underscore 20+ chars without slashes)
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
        // 1. الأول الابتدائي (9 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p1_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1nVM1dhoZJJ2kwYwMLRMlFfo3a70rOKrD/preview"
        ),
        CurriculumBookItem(
            id = "p1_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1zM3D1AUpzxzhiytq3vVmyYOnSGJqM4lP/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1AXlhEx5zeA2PUfiDsxJGQXaksFyQiVsc/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1-FLrmZmDfOLoxh47eYCPnWCK1f4CcDR3/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1w0_8y7osEzGCST5Kj_b8HtL9gRnHqBcu/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1e1TGmQX5RVQgyEeb7gHslt328UEpwJFX/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1ohJZlWvvnZ-d_fQSVL7iTjdEY5EeaQU6/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1SSmMfZYvWCkEDfk-lfCWT3ni4LbrZl-a/preview"
        ),
        CurriculumBookItem(
            id = "p1_moral",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الاخلاقية",
            previewUrl = "https://drive.google.com/file/d/1ugtWInqi8S-reTElSAd5r2AXmFUoEQQ2/preview"
        ),

        // ============================================================
        // 2. الثاني الابتدائي (8 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p2_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1vPIz7JbjlfZBAXJQe95jBAypa_YuSxr8/preview"
        ),
        CurriculumBookItem(
            id = "p2_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1JIa29yJ_Ld9A_Oe31n7lgScUSiCAEhYG/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1fLZBPEG9bgczvcZvxN2jBSuNR0pMMne9/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1qcYh1Vx3W5Qs6VAnDo0biC2Pf3Fl66em/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/10h-L5LKKVQM9XvqknVtzhPPUXIjhqviF/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/117MuokgO4X-sri5OP69GX4hQXSxawFDZ/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1VQy3aIsgXYrP9TBibfV6TKM570AmqFtj/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1SFE2frCT3DtwuNgp2vyIhEV5-VM836YT/preview"
        ),

        // ============================================================
        // 3. الثالث الابتدائي (8 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p3_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1iIoCA9vUMjsYLGcYB3bG9ghH1yJTtA3j/preview"
        ),
        CurriculumBookItem(
            id = "p3_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1hX263Pr8NUNHLUokm_dxklapZNbsWwey/preview"
        ),
        CurriculumBookItem(
            id = "p3_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1gjmF4BkLbfDNQ4aSBps2YLwkJ8Whlb6A/preview"
        ),
        CurriculumBookItem(
            id = "p3_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1uUqy4egLScXQ618bc0ynoi-UQdSdrvWO/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1L_LHQS1Q8S-1R4J7h3-SlDl4IvZMv0ry/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1gNkRMv0ToIJEeEfLpdpQZ4Aex1P4r-bV/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1x2XtEK97fBrP-Fuw1fzRtbXVI5mHQfw9/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1rMFsFuJSbDLYO17R-4_wzx5Dj_01dJvD/preview"
        ),

        // ============================================================
        // 4. الرابع الابتدائي (10 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p4_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1Qa6FOlInBlFlafEon5rtHvU4DWbQEdhn/preview"
        ),
        CurriculumBookItem(
            id = "p4_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1Ea8lcedAR_7JPhiwVGLu1J4QizhxXDIR/preview"
        ),
        CurriculumBookItem(
            id = "p4_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1D0H2HFkgFtrUkfu9_jfqPlDSPuqV8gHv/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1_Rs159GJB3UKzjHjAoDmDfx3nAHEYcC0/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1Ph31Q_l9dSpq50dUQgOAVFYQxyBLIzZf/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/10N1acdhtc1HPkvPKjyeKfsy-WQ_q4GUk/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1rJ-2jjQM5LdQNmWMq0kjpJfZW9IuoEQj/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1ciJBefTfDhpQ5aScaHmd218ME7Wpkb_q/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1g0_IfvcDt8OUi5u_66B_CzIgbuCobbog/preview"
        ),
        CurriculumBookItem(
            id = "p4_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1XxlWljt1fhXPBtoWg4wYdm_pQtI4z6Tc/preview"
        ),

        // ============================================================
        // 5. الخامس الابتدائي (10 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p5_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1QD4MKjg-JRNzmyDUOvAXsArKqU1FWSoD/preview"
        ),
        CurriculumBookItem(
            id = "p5_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1S9P_4htwstxFyk5RKoccW4bnN9DPlwo6/preview"
        ),
        CurriculumBookItem(
            id = "p5_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1KLKUU-6NqSLk68txNZOvrjXZQMYAsI92/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1sbFi8uAP35zFSjav93bXoCDoEjOYZa0h/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1WClbsxm9zoFvFm_g7tp8O8kNDzMNOdB0/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/19GKZ3aOWyBZlPipgK7tVdIWXuHNEbM4u/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1cSZp8Uszg1hKZb7te8FbNZY0r6eZyqSM/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1z43l3wERXzT-egGjs_2puQM54lVaAQ30/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1T7b4sdD0-3TQvub0M-HH3gCDNnGtNm3z/preview"
        ),
        CurriculumBookItem(
            id = "p5_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1RVz091kMA7w-XEn0f19WjTxMKzY4WE1g/preview"
        ),

        // ============================================================
        // 6. السادس الابتدائي (11 كتاباً)
        // ============================================================
        CurriculumBookItem(
            id = "p6_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1a_VcmloZrQIJv9D7dmOXKOubK3sSI5Zb/preview"
        ),
        CurriculumBookItem(
            id = "p6_reading",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1YsbcKHGfzCO6d1d0aXykdh1SOFsTnnH6/preview"
        ),
        CurriculumBookItem(
            id = "p6_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1POyExJU-KWzrWTNtXBNb4V5QaXKTnIZJ/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1nGn8_KxVs2y-xWFqqUBcaHE3Z9k5AzzE/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1yEWy4uW6sjY-SPJB2cpE4S4_YiCiNXZ0/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_ab_solved",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب النشاط محلول",
            previewUrl = "https://drive.google.com/file/d/1pjX1iSOA0QX9p-36KhJYdBIIDeak35Jy/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/13zLlyffzFlZn6B5Z-qsZWleBp1c3KwGd/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1t410bKRHPExvnBgs9iPu1JjHhEoOOHmq/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1qvYJnF__-cQg47cagZAXgIXV1EoZo_27/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب تمارين الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1eWnIMdPE901ff1owadlzwuHNH_rElBYg/preview"
        ),
        CurriculumBookItem(
            id = "p6_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1qB5kJuih2TrN-4IL4pIM8MploPnOX0BD/preview"
        )
    )

    private val _booksFlow = MutableStateFlow<List<CurriculumBookItem>>(defaultPrimaryBooks)
    val booksFlow: StateFlow<List<CurriculumBookItem>> = _booksFlow.asStateFlow()

    val books: List<CurriculumBookItem>
        get() = _booksFlow.value

    private var isInitialized = false

    fun init(context: Context) {
        if (isInitialized) return
        isInitialized = true
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedJson = prefs.getString(KEY_BOOKS_JSON, null)
        if (!savedJson.isNullOrBlank()) {
            val parsed = parseBooksJson(savedJson)
            if (parsed.isNotEmpty()) {
                _booksFlow.value = parsed
                return
            }
        }
        _booksFlow.value = defaultPrimaryBooks
        saveToPrefs(context, defaultPrimaryBooks)
    }

    private fun parseBooksJson(jsonStr: String): List<CurriculumBookItem> {
        return try {
            val arr = JSONArray(jsonStr)
            val result = mutableListOf<CurriculumBookItem>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val id = obj.optString("id")
                val stageName = obj.optString("stage", CurriculumAcademicStage.PRIMARY.name)
                val stage = CurriculumAcademicStage.entries.firstOrNull { it.name == stageName }
                    ?: CurriculumAcademicStage.PRIMARY
                val gradeClass = obj.optString("gradeClass")
                val subjectTitle = obj.optString("subjectTitle")
                val rawUrl = obj.optString("previewUrl")
                if (id.isNotBlank() && gradeClass.isNotBlank() && subjectTitle.isNotBlank()) {
                    result.add(
                        CurriculumBookItem(
                            id = id,
                            stage = stage,
                            gradeClass = gradeClass,
                            subjectTitle = subjectTitle,
                            previewUrl = formatToGoogleDrivePreviewUrl(rawUrl)
                        )
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun saveToPrefs(context: Context, list: List<CurriculumBookItem>) {
        try {
            val arr = JSONArray()
            list.forEach { item ->
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

@Composable
fun CurriculumScreen(
    viewModel: SchoolViewModel
) {
    CurriculumBooksScreen(viewModel = viewModel)
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

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()
    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()

    var selectedStage by remember { mutableStateOf(CurriculumAcademicStage.PRIMARY) }
    var selectedGradeClass by remember(selectedStage) {
        mutableStateOf(selectedStage.gradeClasses.first())
    }
    var searchQuery by remember { mutableStateOf("") }

    // Active book opened inside the Full-Screen In-App WebView Modal
    var activeBookInViewer by remember { mutableStateOf<CurriculumBookItem?>(null) }

    // Optional quick link manager dialog for Developer
    var showDeveloperQuickManagerDialog by remember { mutableStateOf(false) }

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
            Spacer(modifier = Modifier.height(4.dp))

            // Royal Emerald Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF042F24),
                                    Color(0xFF065F46),
                                    Color(0xFF0F3D5E)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            color = GoldAccent.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GoldAccent.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.65f)),
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoStories,
                                            contentDescription = "المناهج الدراسية والكتب",
                                            tint = GoldAccent,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = "المناهج الدراسية الرسمية في العراق",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "تصفح وقراءة جميع كتب المناهج مباشرة داخل التطبيق بدقة كاملة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFD1FAE5)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = "${displayedBooks.size} كتاب",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = GoldAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }

                        if (isDeveloperAuthenticated || currentUser?.role == UserRole.ADMIN) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = GoldAccent.copy(alpha = 0.20f),
                                    border = BorderStroke(1.dp, GoldAccent),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { showDeveloperQuickManagerDialog = true }
                                        .testTag("open_curriculum_links_manager_btn")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Link,
                                            contentDescription = null,
                                            tint = Color(0xFFFFF3B0),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "مدير روابط المناهج الدراسية",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFFF3B0)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Stage Selector Pills (الابتدائية • المتوسطة • الإعدادية)
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
                            color = if (isSelected) GoldAccent else Color(0xFFCBD5E1).copy(alpha = 0.5f)
                        ),
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
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

            // Grade Classes Horizontal Selector
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
                            Color(0xFF065F46)
                        } else if (isDarkMode) {
                            Color(0xFF0F172A)
                        } else {
                            Color(0xFFECFDF5)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isGradeSelected) GoldAccent else Color(0xFF10B981).copy(alpha = 0.35f)
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
                                    Color(0xFFA7F3D0)
                                } else {
                                    Color(0xFF065F46)
                                }
                            )
                        }
                    }
                }
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = "ابحث عن اسم الكتاب أو الصف الدراسي...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = "بحث",
                        tint = DeepEmerald
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Filled.Clear,
                                contentDescription = "مسح البحث"
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_search_input")
            )

            // Books List or Pending Stage Notice
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
                            if (isDarkMode) Color(0xFF334155) else Color(0xFFA7F3D0)
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                                border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.65f)),
                                modifier = Modifier.size(68.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = "مناهج قيد الرفع والتحديث",
                                        tint = if (isDarkMode) GoldAccent else DeepEmerald,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }

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
                                    color = if (isDarkMode) Color(0xFFA7F3D0) else DeepEmerald,
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

        // Quick Links Manager Modal when opened from Curriculum Screen by Developer/Admin
        if (showDeveloperQuickManagerDialog) {
            Dialog(
                onDismissRequest = { showDeveloperQuickManagerDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    shape = RoundedCornerShape(22.dp),
                    color = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدير روابط المناهج الدراسية المباشر",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else DeepForestGreen
                            )
                            TextButton(onClick = { showDeveloperQuickManagerDialog = false }) {
                                Text("إغلاق", fontWeight = FontWeight.ExtraBold, color = DangerRed)
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 24.dp)
                        ) {
                            item {
                                DeveloperCurriculumManagerSection(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean Curriculum Book Card:
 * - NO "PDF رسمي" badge, NO file size badge, NO replacement badge on the left.
 * - Contains ONLY:
 *   1. Book Title (`book.subjectTitle`)
 *   2. Stage & Grade (`${book.stage.shortBadge} • ${book.gradeClass}`)
 *   3. Full-width green button ("فتح مباشر") that opens the book inside the app.
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
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.40f)),
                    modifier = Modifier.size(48.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = book.subjectTitle,
                            tint = if (isDarkMode) Color(0xFF34D399) else DeepEmerald,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = book.subjectTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                    )
                    Text(
                        text = "${book.stage.shortBadge} • ${book.gradeClass}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)
                    )
                }
            }

            // Wide Green Direct Open Button ("فتح مباشر") — Opens exclusively inside the app
            Button(
                onClick = onDirectOpenBook,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DeepEmerald,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("open_book_btn_${book.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoStories,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
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
 * - Opens the Google Drive link in `/preview` format (`https://drive.google.com/file/d/FILE_ID/preview`)
 *   inside an embedded full-screen WebView without ever leaving the app or opening an external browser.
 * - Top bar shows the current book name (`book.subjectTitle`) and a clear button
 *   ("إغلاق / رجوع للمناهج") to return to the books list immediately.
 * - Supports pinch-to-zoom, smooth scrolling, and blocks any pop-out / external browser navigation.
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
                    // Top Bar: Current Book Title + Clear "إغلاق / رجوع للمناهج" Button
                    Surface(
                        color = Color(0xFF042F24),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = GoldAccent.copy(alpha = 0.20f),
                                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.70f)),
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoStories,
                                            contentDescription = null,
                                            tint = GoldAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = book.subjectTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${book.stage.shortBadge} • ${book.gradeClass}",
                                        style = MaterialTheme.typography.labelSmall,
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
                                        isLoadingPage = true
                                        webViewInstance?.loadUrl(embedPreviewUrl)
                                    },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("in_app_book_viewer_reload_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "إعادة تحميل الكتاب",
                                        tint = Color(0xFFA7F3D0),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Button(
                                    onClick = onCloseViewer,
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF059669),
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("in_app_book_viewer_close_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "إغلاق / رجوع للمناهج",
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
                                    settings.javaScriptEnabled = true
                                    settings.domStorageEnabled = true
                                    settings.setSupportZoom(true)
                                    settings.builtInZoomControls = true
                                    settings.displayZoomControls = false
                                    settings.useWideViewPort = true
                                    settings.loadWithOverviewMode = true
                                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                    settings.setSupportMultipleWindows(false)
                                    settings.javaScriptCanOpenWindowsAutomatically = false
                                    webChromeClient = WebChromeClient()
                                    webViewClient = object : android.webkit.WebViewClient() {
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
                                            return if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
                                                // Load all HTTP/HTTPS links directly inside the embedded WebView without opening an external browser
                                                false
                                            } else {
                                                // Block external intent:// or custom scheme links
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
                                color = Color(0xFF042F24).copy(alpha = 0.90f),
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
                                        text = "جاري تحميل الكتاب داخل التطبيق...",
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
 * 1. مدير روابط المناهج الدراسية (تحديث الروابط المباشر بدون رفع ملفات):
 * - يتيح للمطور اختيار (المرحلة الدراسية) ثم (الصف الدراسي).
 * - يعرض قائمة كتب ذلك الصف، وبجانب كل كتاب حقل نصي يحتوي على رابط Google Drive الخاص به
 *   مع زر (تحديث الرابط) وزر (حذف).
 * - يتيح ميزة (إضافة كتاب جديد للصف) عبر إدخال (اسم الكتاب + لصق رابط Google Drive فقط).
 * - المعالجة الذكية التلقائية: يستخرج النظام تلقائياً معرّف الملف (FILE_ID) من أي رابط Google Drive
 *   يلصقه المطور (حتى لو كان ينتهي بـ /view?usp=sharing) ويحوله برمجياً إلى صيغة التضمين المباشر:
 *   https://drive.google.com/file/d/FILE_ID/preview
 *   ثم يحفظه فوراً في قاعدة البيانات ليظهر التحديث لجميع المستخدمين.
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoStories,
                        contentDescription = null,
                        tint = DeepEmerald
                    )
                    Column {
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
                        color = if (isSelected) Color(0xFF065F46) else Color(0xFFECFDF5),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) GoldAccent else Color(0xFF10B981).copy(alpha = 0.4f)
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
                                color = if (isSelected) Color.White else Color(0xFF065F46)
                            )
                        }
                    }
                }
            }

            statusMessage?.let { msg ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
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
                            color = Color(0xFF065F46),
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { statusMessage = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق",
                                tint = Color(0xFF065F46),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 3. إضافة كتاب جديد للصف المختار (اسم الكتاب + لصق رابط Google Drive فقط)
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
                        label = { Text("اسم الكتاب (مثال: الرياضيات)") },
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
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
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
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "حذف الكتاب",
                                            tint = DangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
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
                                        containerColor = Color(0xFF059669),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(40.dp)
                                        .testTag("dev_update_book_link_btn_${book.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
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
