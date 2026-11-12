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
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Verified
import com.example.model.SchoolStage
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.ThreeDIllustrationType
import com.example.ui.theme.AdaptiveSoftBorder
import com.example.ui.theme.AdaptiveSoftSurface
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalGreen
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

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
        val repo = com.example.data.SchoolRepository(context)
        val cloudJson = repo.loadCloudCurriculumBooksJson()
        if (!cloudJson.isNullOrBlank()) {
            val parsedCloud = parseBooksJson(cloudJson)
            if (parsedCloud.isNotEmpty()) {
                _booksFlow.value = parsedCloud
                return
            }
        }
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
        saveToPrefs(context, defaultPrimaryBooks, pushToCloud = false)
    }

    fun applyCloudBooksJson(context: Context, rawJson: String) {
        if (rawJson.isBlank()) return
        val parsed = parseBooksJson(rawJson)
        if (parsed.isNotEmpty()) {
            _booksFlow.value = parsed
            saveToPrefs(context, parsed, pushToCloud = false)
        }
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

    private fun saveToPrefs(context: Context, list: List<CurriculumBookItem>, pushToCloud: Boolean = true) {
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
            val jsonPayload = arr.toString()
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_BOOKS_JSON, jsonPayload)
                .apply()
            com.example.data.SchoolRepository(context).saveCloudCurriculumBooksJson(jsonPayload, pushToCloud = pushToCloud)
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

/**
 * Custom Animated Canvas Vector Icons for Curriculum Screen (Zero primitive static icons).
 */
@Composable
private fun AnimatedCurriculumCrestCanvasIcon(
    accentColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "curriculum_crest_transition")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "crest_wave_phase"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_glow_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val pageFlutter = sin(wavePhase) * (h * 0.045f)

        // Soft glowing aura ring
        drawCircle(
            color = accentColor.copy(alpha = 0.18f),
            radius = (w * 0.44f) * glowPulse,
            center = Offset(cx, cy)
        )

        // Left open book wing
        val leftWing = Path().apply {
            moveTo(cx, cy + h * 0.24f)
            cubicTo(
                cx - w * 0.16f, cy + h * 0.16f,
                cx - w * 0.34f, cy + h * 0.20f + pageFlutter,
                cx - w * 0.40f, cy + h * 0.12f + pageFlutter
            )
            lineTo(cx - w * 0.38f, cy - h * 0.22f + pageFlutter)
            cubicTo(
                cx - w * 0.26f, cy - h * 0.26f + pageFlutter,
                cx - w * 0.12f, cy - h * 0.18f,
                cx, cy - h * 0.10f
            )
            close()
        }
        drawPath(
            path = leftWing,
            color = accentColor.copy(alpha = 0.24f)
        )
        drawPath(
            path = leftWing,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Right open book wing
        val rightWing = Path().apply {
            moveTo(cx, cy + h * 0.24f)
            cubicTo(
                cx + w * 0.16f, cy + h * 0.16f,
                cx + w * 0.34f, cy + h * 0.20f - pageFlutter,
                cx + w * 0.40f, cy + h * 0.12f - pageFlutter
            )
            lineTo(cx + w * 0.38f, cy - h * 0.22f - pageFlutter)
            cubicTo(
                cx + w * 0.26f, cy - h * 0.26f - pageFlutter,
                cx + w * 0.12f, cy - h * 0.18f,
                cx, cy - h * 0.10f
            )
            close()
        }
        drawPath(
            path = rightWing,
            color = secondaryColor.copy(alpha = 0.24f)
        )
        drawPath(
            path = rightWing,
            color = accentColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Center spine & floating knowledge star above the book
        drawLine(
            color = accentColor,
            start = Offset(cx, cy - h * 0.12f),
            end = Offset(cx, cy + h * 0.26f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        val starY = cy - h * 0.31f + (cos(wavePhase) * h * 0.03f)
        drawCircle(
            color = accentColor,
            radius = 2.6.dp.toPx() * glowPulse,
            center = Offset(cx, starY)
        )
    }
}

@Composable
private fun AnimatedStageSelectorCanvasIcon(
    stage: CurriculumAcademicStage,
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stage_tab_canvas_${stage.name}")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stage_tab_pulse"
    )
    val scale by animateFloatAsState(
        targetValue = if (isSelected) pulse else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "stage_tab_scale"
    )

    Canvas(modifier = modifier.scale(scale)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.8.dp.toPx()

        when (stage) {
            CurriculumAcademicStage.PRIMARY -> {
                // Rising academic star & open book base
                val starPath = Path().apply {
                    moveTo(cx, h * 0.12f)
                    lineTo(cx + w * 0.14f, cy - h * 0.06f)
                    lineTo(w * 0.86f, cy)
                    lineTo(cx + w * 0.14f, cy + h * 0.10f)
                    lineTo(cx, h * 0.86f)
                    lineTo(cx - w * 0.14f, cy + h * 0.10f)
                    lineTo(w * 0.14f, cy)
                    lineTo(cx - w * 0.14f, cy - h * 0.06f)
                    close()
                }
                drawPath(
                    path = starPath,
                    color = tint.copy(alpha = if (isSelected) 0.30f else 0.14f)
                )
                drawPath(
                    path = starPath,
                    color = tint,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
            }
            CurriculumAcademicStage.INTERMEDIATE -> {
                // Compass / geometric hexagon prism
                val hexPath = Path().apply {
                    for (i in 0 until 6) {
                        val angle = (i * 60.0 - 90.0) * (PI / 180.0)
                        val px = cx + (w * 0.38f) * cos(angle).toFloat()
                        val py = cy + (h * 0.38f) * sin(angle).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(
                    path = hexPath,
                    color = tint.copy(alpha = if (isSelected) 0.26f else 0.10f)
                )
                drawPath(
                    path = hexPath,
                    color = tint,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
                drawCircle(color = tint, radius = 2.dp.toPx(), center = Offset(cx, cy))
            }
            CurriculumAcademicStage.PREPARATORY -> {
                // Academic graduation cap crest
                val capPath = Path().apply {
                    moveTo(cx, h * 0.18f)
                    lineTo(w * 0.90f, cy * 0.88f)
                    lineTo(cx, cy * 1.32f)
                    lineTo(w * 0.10f, cy * 0.88f)
                    close()
                }
                drawPath(
                    path = capPath,
                    color = tint.copy(alpha = if (isSelected) 0.28f else 0.12f)
                )
                drawPath(
                    path = capPath,
                    color = tint,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.82f, cy * 0.92f),
                    end = Offset(w * 0.82f, h * 0.82f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun AnimatedBookSubjectCanvasIcon(
    subjectTitle: String,
    accentColor: Color,
    modifier: Modifier = Modifier.size(28.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "subject_icon_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "subject_phase"
    )
    val breath by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subject_breath"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 2.dp.toPx()

        when {
            subjectTitle.contains("الاسلامية") || subjectTitle.contains("الأخلاقية") || subjectTitle.contains("الاخلاقية") -> {
                // Radiant eight-point Islamic geometric star + crescent glow
                drawCircle(
                    color = accentColor.copy(alpha = 0.16f),
                    radius = (w * 0.42f) * breath,
                    center = Offset(cx, cy)
                )
                rotate(degrees = sin(phase) * 8f, pivot = Offset(cx, cy)) {
                    val s = w * 0.46f
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(cx - s / 2f, cy - s / 2f),
                        size = Size(s, s),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )
                    rotate(degrees = 45f, pivot = Offset(cx, cy)) {
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(cx - s / 2f, cy - s / 2f),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = Stroke(width = strokeW)
                        )
                    }
                }
                drawCircle(
                    color = accentColor,
                    radius = 2.4.dp.toPx() * breath,
                    center = Offset(cx, cy)
                )
            }

            subjectTitle.contains("العلوم") -> {
                // Animated atomic orbital rings + glowing nucleus
                drawCircle(
                    color = accentColor,
                    radius = 3.dp.toPx() * breath,
                    center = Offset(cx, cy)
                )
                rotate(degrees = 30f + sin(phase) * 10f, pivot = Offset(cx, cy)) {
                    drawOval(
                        color = accentColor,
                        topLeft = Offset(w * 0.12f, h * 0.32f),
                        size = Size(w * 0.76f, h * 0.36f),
                        style = Stroke(width = 1.7.dp.toPx())
                    )
                }
                rotate(degrees = -30f - sin(phase) * 10f, pivot = Offset(cx, cy)) {
                    drawOval(
                        color = accentColor.copy(alpha = 0.85f),
                        topLeft = Offset(w * 0.12f, h * 0.32f),
                        size = Size(w * 0.76f, h * 0.36f),
                        style = Stroke(width = 1.7.dp.toPx())
                    )
                }
                val electronX = cx + cos(phase) * (w * 0.32f)
                val electronY = cy + sin(phase) * (h * 0.18f)
                drawCircle(
                    color = accentColor,
                    radius = 2.2.dp.toPx(),
                    center = Offset(electronX, electronY)
                )
            }

            subjectTitle.contains("الرياضيات") -> {
                // Animated geometric prism + coordinate grid waves
                val offsetWave = sin(phase) * 2.dp.toPx()
                val triangle = Path().apply {
                    moveTo(cx, h * 0.16f + offsetWave)
                    lineTo(w * 0.84f, h * 0.78f)
                    lineTo(w * 0.16f, h * 0.78f)
                    close()
                }
                drawPath(
                    path = triangle,
                    color = accentColor.copy(alpha = 0.20f)
                )
                drawPath(
                    path = triangle,
                    color = accentColor,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
                drawCircle(
                    color = accentColor,
                    radius = (w * 0.16f) * breath,
                    center = Offset(cx, h * 0.56f),
                    style = Stroke(width = 1.6.dp.toPx())
                )
            }

            subjectTitle.contains("الانكليزي") || subjectTitle.contains("النشاط محلول") -> {
                // Animated global language sphere + meridian arcs
                val r = (w * 0.38f) * breath
                drawCircle(
                    color = accentColor.copy(alpha = 0.15f),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = accentColor,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = strokeW)
                )
                drawOval(
                    color = accentColor,
                    topLeft = Offset(cx - r * 0.52f, cy - r),
                    size = Size(r * 1.04f, r * 2f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawLine(
                    color = accentColor,
                    start = Offset(cx - r, cy),
                    end = Offset(cx + r, cy),
                    strokeWidth = 1.5.dp.toPx()
                )
            }

            subjectTitle.contains("الاجتماعيات") -> {
                // Animated compass rose & heritage meridian
                val r = w * 0.38f
                drawCircle(
                    color = accentColor,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.8.dp.toPx())
                )
                rotate(degrees = sin(phase) * 18f, pivot = Offset(cx, cy)) {
                    val needle = Path().apply {
                        moveTo(cx, cy - r * 0.78f)
                        lineTo(cx + r * 0.22f, cy)
                        lineTo(cx, cy + r * 0.78f)
                        lineTo(cx - r * 0.22f, cy)
                        close()
                    }
                    drawPath(path = needle, color = accentColor.copy(alpha = 0.28f))
                    drawPath(
                        path = needle,
                        color = accentColor,
                        style = Stroke(width = 1.7.dp.toPx(), join = StrokeJoin.Round)
                    )
                }
            }

            else -> {
                // Reading / Arabic Grammar: Open calligraphic scroll book + nib wave
                val flutter = sin(phase) * (h * 0.04f)
                val bookPath = Path().apply {
                    moveTo(cx, h * 0.76f)
                    lineTo(w * 0.16f, h * 0.68f + flutter)
                    lineTo(w * 0.16f, h * 0.24f + flutter)
                    lineTo(cx, h * 0.32f)
                    lineTo(w * 0.84f, h * 0.24f - flutter)
                    lineTo(w * 0.84f, h * 0.68f - flutter)
                    close()
                }
                drawPath(path = bookPath, color = accentColor.copy(alpha = 0.20f))
                drawPath(
                    path = bookPath,
                    color = accentColor,
                    style = Stroke(width = strokeW, join = StrokeJoin.Round)
                )
                drawLine(
                    color = accentColor,
                    start = Offset(cx, h * 0.32f),
                    end = Offset(cx, h * 0.76f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun AnimatedSearchVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_vector_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "search_lens_scale"
    )
    Canvas(modifier = modifier.scale(scale)) {
        val w = size.width
        val h = size.height
        val lensCenter = Offset(w * 0.42f, h * 0.42f)
        val radius = w * 0.28f
        drawCircle(
            color = tint.copy(alpha = 0.16f),
            radius = radius,
            center = lensCenter
        )
        drawCircle(
            color = tint,
            radius = radius,
            center = lensCenter,
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.62f, h * 0.62f),
            end = Offset(w * 0.86f, h * 0.86f),
            strokeWidth = 2.4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun AnimatedClearVectorIcon(
    tint: Color,
    modifier: Modifier = Modifier.size(18.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = tint.copy(alpha = 0.14f),
            radius = w * 0.44f,
            center = Offset(w / 2f, h / 2f)
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.32f, h * 0.32f),
            end = Offset(w * 0.68f, h * 0.68f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.68f, h * 0.32f),
            end = Offset(w * 0.32f, h * 0.68f),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round
        )
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

    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()

    var selectedStage by remember { mutableStateOf(CurriculumAcademicStage.PRIMARY) }
    var selectedGradeClass by remember(selectedStage) {
        mutableStateOf(selectedStage.gradeClasses.first())
    }
    var searchQuery by remember { mutableStateOf("") }

    // Active book opened inside the Full-Screen In-App WebView Modal
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

    val headerInfinite = rememberInfiniteTransition(label = "curriculum_header_mesh")
    val meshPhase by headerInfinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "curriculum_mesh_phase"
    )

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        // 1) التمرير الكامل للشاشة نحو الأعلى (Full Vertical Scroll) داخل LazyColumn واحدة رئيسية
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("curriculum_books_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
        ) {
            // البنر الزمردي العلوي مع مشهد المنصة المعرفية العائمة
            item(key = "curriculum_hero_banner") {
                CurriculumHeroBannerWithOpenBookAnimation(
                    title = "المناهج الدراسية والكتب الرسمية",
                    subtitle = "تصفح وقراءة كافة الكتب المنهجية المعتمدة مباشرة داخل المنصة",
                    rightPillText = "منهج وزارة التربية المعتمد",
                    leftPillText = "${allBooks.size.coerceAtLeast(56)} كتاب منهجي"
                )
            }

            // بطاقة اختيار المرحلة والصف وحقل البحث
            item(key = "curriculum_stage_grade_search_card") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.2.dp, AdaptiveSoftBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Clean 3-Stage Segmented Selector (الابتدائية • المتوسطة • الإعدادية)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isDarkMode) Color(0xFF0F172A) else AdaptiveSoftSurface)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CurriculumAcademicStage.entries.forEach { stage ->
                                val isSelected = selectedStage == stage && searchQuery.isBlank()
                                Surface(
                                    onClick = {
                                        searchQuery = ""
                                        selectedStage = stage
                                        selectedGradeClass = stage.gradeClasses.first()
                                    },
                                    shape = RoundedCornerShape(11.dp),
                                    color = if (isSelected) RoyalGreen else Color.Transparent,
                                    shadowElevation = if (isSelected) 3.dp else 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(42.dp)
                                        .testTag("curriculum_stage_tab_${stage.name}")
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = stage.shortBadge,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isSelected) {
                                                Color.White
                                            } else if (isDarkMode) {
                                                Color(0xFFE2E8F0)
                                            } else {
                                                DeepForestGreen
                                            },
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // 2. Grade Classes Horizontal Filter Chips
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
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isGradeSelected) {
                                        DeepEmerald
                                    } else if (isDarkMode) {
                                        Color(0xFF0F172A)
                                    } else {
                                        AdaptiveSoftSurface
                                    },
                                    border = BorderStroke(
                                        width = if (isGradeSelected) 1.4.dp else 1.dp,
                                        color = if (isGradeSelected) GoldAccent else AdaptiveSoftBorder
                                    ),
                                    shadowElevation = if (isGradeSelected) 2.dp else 0.dp,
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("curriculum_grade_chip_$gradeClass")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(horizontal = 14.dp)
                                    ) {
                                        Text(
                                            text = if (gradeBookCount > 0) {
                                                "$gradeClass ($gradeBookCount)"
                                            } else {
                                                gradeClass
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontSize = 12.5.sp,
                                            fontWeight = if (isGradeSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                            color = if (isGradeSelected) {
                                                Color.White
                                            } else if (isDarkMode) {
                                                Color(0xFFA7F3D0)
                                            } else {
                                                DeepForestGreen
                                            },
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        // 3. Search Bar with Custom Animated Canvas Vector Icons
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
                                AnimatedSearchVectorIcon(
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotBlank()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        AnimatedClearVectorIcon(
                                            tint = if (isDarkMode) Color(0xFF94A3B8) else DeepEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("curriculum_search_input")
                        )
                    }
                }
            }

            // 3) قائمة الكتب أو بطاقة المرحلة الفارغة داخل نفس الـ LazyColumn الرئيسية
            if (displayedBooks.isEmpty()) {
                item(key = "curriculum_empty_stage_notice_${selectedStage.name}_$selectedGradeClass") {
                    InteractiveSmartLibraryStageView(
                        selectedStage = selectedStage,
                        selectedGradeClass = selectedGradeClass,
                        hasActiveSearchQuery = searchQuery.isNotBlank(),
                        isDarkMode = isDarkMode
                    )
                }
            } else {
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
 * 3) بطاقة واضحة للمراحل التي لم تُرفع كتبها بعد:
 * تعرض بطاقة بيضاء أنيقة بداخلها الرسم الفيكتوري للمنصة المعرفية العائمة مع العنوان:
 * "لم يتم رفع المناهج الدراسية لهذه المرحلة بعد"
 * وتحته: "سيتم إدراج الكتب الرسمية لهذا الصف قريباً من قِبل الإدارة المركزية."
 */
@Composable
private fun InteractiveSmartLibraryStageView(
    selectedStage: CurriculumAcademicStage,
    selectedGradeClass: String,
    hasActiveSearchQuery: Boolean,
    isDarkMode: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .testTag("curriculum_stage_pending_notice_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF0F172A) else Color.White
        ),
        border = BorderStroke(
            1.4.dp,
            if (isDarkMode) GoldAccent.copy(alpha = 0.45f) else Color(0xFF10B981).copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = if (isDarkMode) {
                            listOf(
                                Color(0xFF042F24).copy(alpha = 0.65f),
                                Color(0xFF0F172A)
                            )
                        } else {
                            listOf(
                                Color.White,
                                Color(0xFFECFDF5).copy(alpha = 0.55f),
                                Color.White
                            )
                        }
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // مشهد المنصة المعرفية العائمة بالمدارات والرموز التعليمية المضيئة
                OpeningAndClosing3DBookVectorAnimation(
                    modifier = Modifier.size(132.dp),
                    isDarkMode = isDarkMode
                )

                // شارة المرحلة والصف الدراسي
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.75f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Canvas(modifier = Modifier.size(8.dp)) {
                            drawCircle(color = GoldAccent, radius = size.minDimension / 2f)
                        }
                        Text(
                            text = "${selectedStage.arabicTitle} • $selectedGradeClass",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                        )
                    }
                }

                // العنوان الرئيسي المطلوب
                Text(
                    text = "لم يتم رفع المناهج الدراسية لهذه المرحلة بعد",
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else DeepForestGreen,
                    textAlign = TextAlign.Center,
                    lineHeight = 25.sp
                )

                // النص التوضيحي الفرعي المطلوب
                Text(
                    text = if (hasActiveSearchQuery) {
                        "سيتم إدراج الكتب الرسمية لهذا الصف قريباً من قِبل الإدارة المركزية. (لا توجد نتائج مطابقة لعبارة البحث الحالية)"
                    } else {
                        "سيتم إدراج الكتب الرسمية لهذا الصف قريباً من قِبل الإدارة المركزية."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 21.sp
                )
            }
        }
    }
}

/**
 * Clean Interactive Curriculum Book Card:
 * - Contains ONLY:
 *   1. Animated Subject Canvas Vector Icon (`AnimatedBookSubjectCanvasIcon`)
 *   2. Book Title (`book.subjectTitle`)
 *   3. Stage & Grade (`${book.stage.shortBadge} • ${book.gradeClass}`)
 *   4. Full-width green button ("فتح مباشر") that opens the book inside the app.
 */
@Composable
private fun CurriculumBookOfficialCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onDirectOpenBook: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "book_card_pulse_${book.id}")
    val badgeGlow by infiniteTransition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book_badge_glow"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isDarkMode) Color(0xFF334155) else Color(0xFFA7F3D0).copy(alpha = 0.75f)
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
                    shape = RoundedCornerShape(16.dp),
                    color = if (isDarkMode) Color(0xFF064E3B) else Color(0xFFECFDF5),
                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = badgeGlow)),
                    modifier = Modifier.size(52.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        AnimatedBookSubjectCanvasIcon(
                            subjectTitle = book.subjectTitle,
                            accentColor = if (isDarkMode) Color(0xFF34D399) else DeepEmerald,
                            modifier = Modifier.size(30.dp)
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
                AnimatedCurriculumCrestCanvasIcon(
                    accentColor = GoldAccent,
                    secondaryColor = Color.White,
                    modifier = Modifier.size(20.dp)
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
 * - While the book is being prepared, renders a royal interactive 3D flipping-pages Canvas book
 *   animation with glowing light waves and a smooth progress bar that fades out (`fadeOut`)
 *   once `onPageFinished` fires.
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
                                        AnimatedCurriculumCrestCanvasIcon(
                                            accentColor = GoldAccent,
                                            secondaryColor = Color(0xFF34D399),
                                            modifier = Modifier.size(22.dp)
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
                                    Canvas(modifier = Modifier.size(18.dp)) {
                                        val r = size.minDimension * 0.38f
                                        drawArc(
                                            color = Color(0xFFA7F3D0),
                                            startAngle = 35f,
                                            sweepAngle = 285f,
                                            useCenter = false,
                                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
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
                                            return if (targetUrl.startsWith("http://") || targetUrl.startsWith("https://")) {
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

                        // Royal Interactive 3D Digital Book Preparation Screen with Smooth Fade-Out
                        androidx.compose.animation.AnimatedVisibility(
                            visible = isLoadingPage,
                            enter = fadeIn(animationSpec = tween(250)),
                            exit = fadeOut(animationSpec = tween(420)),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            RoyalBookFlippingPreparationOverlay(
                                book = book,
                                isDarkMode = isDarkMode
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Exclusive Curriculum Hero Banner with `OpeningAndClosing3DBookVectorAnimation` on the left side:
 * Replaces the stacked-books illustration used in Schedule & Homework with a dedicated
 * 3D Perspective Opening & Closing Book Vector Animation.
 */
@Composable
private fun CurriculumHeroBannerWithOpenBookAnimation(
    title: String,
    subtitle: String,
    rightPillText: String,
    leftPillText: String,
    modifier: Modifier = Modifier
) {
    val waveHeroShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val topWaveDepth = (h * 0.065f).coerceIn(10f, 22f)
            val bottomWaveDepth = (h * 0.095f).coerceIn(16f, 30f)

            moveTo(0f, topWaveDepth * 0.45f)
            cubicTo(
                w * 0.32f, -topWaveDepth * 0.35f,
                w * 0.68f, topWaveDepth * 1.35f,
                w, topWaveDepth * 0.25f
            )
            lineTo(w, h - bottomWaveDepth)
            quadraticBezierTo(
                w * 0.50f, h + bottomWaveDepth * 0.85f,
                0f, h - bottomWaveDepth * 0.85f
            )
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = waveHeroShape,
                ambientColor = Color(0xFF047857).copy(alpha = 0.32f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.38f)
            )
            .clip(waveHeroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF047857),
                        Color(0xFF0F3D5E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 680f)
                )
            )
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            val innerWave = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(
                    w * 0.35f, h * 0.60f,
                    w * 0.70f, h * 0.88f,
                    w, h * 0.74f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = innerWave,
                color = Color(0xFF0F3D5E).copy(alpha = 0.28f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.34f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * 0.50f),
                    radius = w * 0.44f
                ),
                radius = w * 0.44f,
                center = Offset(w * 0.22f, h * 0.50f)
            )

            val step = 11.dp.toPx()
            val dotRadius = 1.6.dp.toPx()
            for (row in 0..6) {
                for (col in 0..7) {
                    val alpha = ((1f - (row + col) / 14f) * 0.22f).coerceIn(0.03f, 0.22f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.04f + col * step, h * 0.10f + row * step)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.96f - col * step, h * 0.88f - row * step)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.weight(1f, fill = false),
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = rightPillText,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.01f))

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.MenuBook,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = leftPillText,
                            style = MaterialTheme.typography.labelMedium,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Left side of the Emerald Banner: 3D Opening & Closing Book Vector Animation
                OpeningAndClosing3DBookVectorAnimation(
                    modifier = Modifier.size(108.dp),
                    isDarkMode = true
                )
            }
        }
    }
}

/**
 * 2) مشهد المنصة المعرفية العائمة (بديلاً عن أنميشن الكتاب الأصفر القديم):
 * يرسم بـ Canvas منصة معرفية زمردية وذهبية عائمة تحيط بها مدارات ورموز تعليمية مضيئة تسبح بنعومة.
 */
@Composable
fun OpeningAndClosing3DBookVectorAnimation(
    modifier: Modifier = Modifier.size(108.dp),
    isDarkMode: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "floating_knowledge_platform_anim")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "knowledge_orbit_angle"
    )
    val floatWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "platform_float_wave"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.14f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "platform_glow_pulse"
    )

    Canvas(
        modifier = modifier.testTag("opening_closing_3d_book_vector_canvas")
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.54f
        val floatY = sin(floatWave) * (h * 0.032f)
        val crestFloatY = cos(floatWave) * (h * 0.040f)

        // 1. هالة ضوئية زمردية وذهبية محيطة بالمنصة المعرفية العائمة
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.34f else 0.24f),
                    Color(0xFFFDE68A).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(cx, cy - h * 0.04f),
                radius = w * 0.48f * glowPulse
            ),
            radius = w * 0.48f * glowPulse,
            center = Offset(cx, cy - h * 0.04f)
        )

        // ظل ناعم أسفل المنصة العائمة
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF042F24).copy(alpha = if (isDarkMode) 0.45f else 0.22f),
                    Color.Transparent
                ),
                center = Offset(cx, h * 0.88f),
                radius = w * 0.36f
            ),
            topLeft = Offset(cx - w * 0.34f, h * 0.82f),
            size = Size(w * 0.68f, h * 0.10f)
        )

        // 2. المدارات التعليمية المضيئة المحيطة بالمنصة (Tilted Glowing Knowledge Orbits)
        rotate(degrees = -18f, pivot = Offset(cx, cy - h * 0.04f + floatY)) {
            drawOval(
                color = Color(0xFFFDE68A).copy(alpha = 0.55f),
                topLeft = Offset(cx - w * 0.44f, cy - h * 0.22f + floatY),
                size = Size(w * 0.88f, h * 0.34f),
                style = Stroke(width = 1.6.dp.toPx())
            )
        }
        rotate(degrees = 22f, pivot = Offset(cx, cy - h * 0.04f + floatY)) {
            drawOval(
                color = Color(0xFF34D399).copy(alpha = 0.50f),
                topLeft = Offset(cx - w * 0.40f, cy - h * 0.20f + floatY),
                size = Size(w * 0.80f, h * 0.30f),
                style = Stroke(width = 1.4.dp.toPx())
            )
        }

        // 3. القاعدة الهندسية للمنصة المعرفية الزمردية والذهبية العائمة (Floating Emerald & Gold Pedestal)
        val platY = cy + h * 0.14f + floatY
        val platHalfW = w * 0.34f
        val platDepth = h * 0.11f

        // الطبقة السفلية الذهبية للمنصة
        val lowerPedestal = Path().apply {
            moveTo(cx, platY - platDepth * 0.45f)
            lineTo(cx + platHalfW * 0.88f, platY + platDepth * 0.25f)
            lineTo(cx, platY + platDepth * 1.05f)
            lineTo(cx - platHalfW * 0.88f, platY + platDepth * 0.25f)
            close()
        }
        drawPath(
            path = lowerPedestal,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFB45309), Color(0xFFF59E0B), Color(0xFFFDE68A)),
                start = Offset(cx - platHalfW, platY),
                end = Offset(cx + platHalfW, platY + platDepth)
            )
        )

        // السطح الزمردي الملكي للمنصة المعرفية العائمة بمنحنيات بيزيير
        val upperPlatform = Path().apply {
            moveTo(cx, platY - platDepth)
            quadraticBezierTo(
                cx + platHalfW * 0.55f, platY - platDepth * 0.65f,
                cx + platHalfW, platY
            )
            quadraticBezierTo(
                cx + platHalfW * 0.55f, platY + platDepth * 0.65f,
                cx, platY + platDepth * 0.72f
            )
            quadraticBezierTo(
                cx - platHalfW * 0.55f, platY + platDepth * 0.65f,
                cx - platHalfW, platY
            )
            quadraticBezierTo(
                cx - platHalfW * 0.55f, platY - platDepth * 0.65f,
                cx, platY - platDepth
            )
            close()
        }
        drawPath(
            path = upperPlatform,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF064E3B),
                    Color(0xFF0D8A6A),
                    Color(0xFF10B981)
                ),
                start = Offset(cx - platHalfW, platY - platDepth),
                end = Offset(cx + platHalfW, platY + platDepth)
            )
        )
        drawPath(
            path = upperPlatform,
            color = Color(0xFFFDE68A),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // حلقة الطاقة المعرفية وسط المنصة
        drawOval(
            color = Color(0xFFA7F3D0).copy(alpha = 0.75f),
            topLeft = Offset(cx - platHalfW * 0.52f, platY - platDepth * 0.34f),
            size = Size(platHalfW * 1.04f, platDepth * 0.68f),
            style = Stroke(width = 1.3.dp.toPx())
        )

        // 4. البلورة / المنارة المعرفية الزمردية الذهبية العائمة فوق المنصة
        val beaconCenterY = cy - h * 0.12f + crestFloatY
        val crystalW = w * 0.18f
        val crystalH = h * 0.22f

        val crystalPath = Path().apply {
            moveTo(cx, beaconCenterY - crystalH)
            lineTo(cx + crystalW, beaconCenterY)
            lineTo(cx, beaconCenterY + crystalH * 0.78f)
            lineTo(cx - crystalW, beaconCenterY)
            close()
        }
        drawPath(
            path = crystalPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFFDE68A),
                    Color(0xFF10B981),
                    Color(0xFF064E3B)
                ),
                startY = beaconCenterY - crystalH,
                endY = beaconCenterY + crystalH
            )
        )
        drawPath(
            path = crystalPath,
            color = Color.White.copy(alpha = 0.85f),
            style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
        )

        // تاج أكاديمي مصغر مضيء أعلى المنارة المعرفية
        val crownY = beaconCenterY - crystalH - h * 0.04f
        drawCircle(
            color = Color(0xFFFDE68A),
            radius = 3.5.dp.toPx() * glowPulse,
            center = Offset(cx, crownY)
        )

        // 5. رموز وكواكب تعليمية مضيئة تسبح بنعومة في المدارات المحيطة (Swimming Educational Symbols)
        for (idx in 0 until 4) {
            val symbolAngle = orbitAngle + idx * (PI.toFloat() * 0.5f)
            val rx = w * 0.39f
            val ry = h * 0.16f
            val sx = cx + cos(symbolAngle) * rx
            val sy = (cy - h * 0.04f + floatY) + sin(symbolAngle) * ry

            // هالة الرمز التعليمي السابح
            drawCircle(
                color = if (idx % 2 == 0) Color(0xFFFDE68A).copy(alpha = 0.32f) else Color(0xFF34D399).copy(alpha = 0.32f),
                radius = 7.dp.toPx() * glowPulse,
                center = Offset(sx, sy)
            )

            // شكل الرمز التعليمي (نجمة معرفية / ماسة علمية / ذرة مضيئة)
            if (idx % 2 == 0) {
                val starR = 4.2.dp.toPx()
                val diamond = Path().apply {
                    moveTo(sx, sy - starR)
                    lineTo(sx + starR, sy)
                    lineTo(sx, sy + starR)
                    lineTo(sx - starR, sy)
                    close()
                }
                drawPath(path = diamond, color = Color(0xFFFDE68A))
            } else {
                drawCircle(
                    color = Color(0xFF6EE7B7),
                    radius = 3.6.dp.toPx(),
                    center = Offset(sx, sy)
                )
                drawCircle(
                    color = Color.White,
                    radius = 1.6.dp.toPx(),
                    center = Offset(sx, sy)
                )
            }
        }
    }
}

/**
 * Royal Interactive Preparation Screen:
 * Displays `OpeningAndClosing3DBookVectorAnimation` in a large, elegant size (`148.dp`)
 * in the center of the screen while the book loads inside the app, fading out smoothly once ready.
 */
@Composable
private fun RoyalBookFlippingPreparationOverlay(
    book: CurriculumBookItem,
    isDarkMode: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "royal_3d_book_prep")
    val shimmerProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_page_shimmer_progress"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_light_wave_phase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(
                            Color(0xFF042F24),
                            Color(0xFF064E3B),
                            Color(0xFF0F172A)
                        )
                    } else {
                        listOf(
                            Color(0xFFECFDF5),
                            Color(0xFFD1FAE5),
                            Color(0xFFF8FAFC)
                        )
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Ambient light waves in the background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            for (band in 0..2) {
                val path = Path().apply {
                    val baseY = h * (0.28f + band * 0.22f)
                    moveTo(0f, baseY)
                    var x = 0f
                    while (x <= w) {
                        val y = baseY + sin((x / w) * 2f * PI + wavePhase + band).toFloat() * 24.dp.toPx()
                        lineTo(x, y)
                        x += 24f
                    }
                }
                drawPath(
                    path = path,
                    color = GoldAccent.copy(alpha = if (isDarkMode) 0.12f else 0.18f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.88f) else Color.White.copy(alpha = 0.95f)
            ),
            border = BorderStroke(1.5.dp, GoldAccent.copy(alpha = 0.75f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large 3D Opening & Closing Book Vector Animation in the center of the preparation overlay
                OpeningAndClosing3DBookVectorAnimation(
                    modifier = Modifier.size(148.dp),
                    isDarkMode = isDarkMode
                )

                Text(
                    text = book.subjectTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else DeepForestGreen,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "${book.stage.arabicTitle} • ${book.gradeClass}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFFA7F3D0) else DeepEmerald,
                    textAlign = TextAlign.Center
                )

                // Smooth Animated Shimmer Progress Bar
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                ) {
                    val barW = size.width
                    val barH = size.height
                    drawRoundRect(
                        color = if (isDarkMode) Color(0xFF042F24) else Color(0xFFD1FAE5),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barH / 2f)
                    )
                    val activeSegmentW = barW * 0.48f
                    val startX = (barW + activeSegmentW) * shimmerProgress - activeSegmentW
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                DeepEmerald,
                                GoldAccent,
                                DeepEmerald
                            )
                        ),
                        topLeft = Offset(startX.coerceIn(0f, barW), 0f),
                        size = Size(activeSegmentW.coerceAtMost(barW - startX.coerceAtLeast(0f)), barH),
                        cornerRadius = CornerRadius(barH / 2f)
                    )
                }
            }
        }
    }
}

/**
 * 1. مدير روابط المناهج الدراسية (حصرياً داخل إعدادات المطور Developer Settings فقط):
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
                    AnimatedCurriculumCrestCanvasIcon(
                        accentColor = DeepEmerald,
                        secondaryColor = GoldAccent,
                        modifier = Modifier.size(26.dp)
                    )
                    Column {
                        Text(
                            text = "إدارة وتحديث روابط الكتب الدراسية (حصرياً للمطور)",
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
                            AnimatedClearVectorIcon(
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
                                        containerColor = Color(0xFF059669),
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
