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
    // Force reset storage key v12 so any legacy cached links are immediately ignored
    private const val PREFS_NAME = "madrasati_verified_books_v12"
    private const val KEY_BOOKS_JSON = "madrasati_verified_books_v12"

    /**
     * Extracts FILE_ID from any Google Drive link (preserving every '-' and '_')
     * and converts it to the exact embedded /preview format:
     * https://drive.google.com/file/d/FILE_ID/preview
     */
    fun formatToGoogleDrivePreviewUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) return ""

        val fileDRegex = Regex("""/file/d/([a-zA-Z0-9_-]+)""")
        fileDRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        val queryIdRegex = Regex("""[?&]id=([a-zA-Z0-9_-]+)""")
        queryIdRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        val shortDRegex = Regex("""/d/([a-zA-Z0-9_-]+)""")
        shortDRegex.find(trimmed)?.groupValues?.getOrNull(1)?.let { fileId ->
            if (fileId.isNotBlank()) {
                return "https://drive.google.com/file/d/$fileId/preview"
            }
        }

        if (!trimmed.contains("/") && trimmed.length >= 18) {
            return "https://drive.google.com/file/d/$trimmed/preview"
        }

        return if (trimmed.endsWith("/view") || trimmed.contains("/view?")) {
            trimmed.substringBefore("/view") + "/preview"
        } else {
            trimmed
        }
    }

    val defaultPrimaryBooks: List<CurriculumBookItem> = listOf(
        // ============================================================
        // 1. الصف الأول الابتدائي
        // ============================================================
        CurriculumBookItem(
            id = "p1_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1CwXiZoTtixhcU1LYKvv5DKVvXo2Lqgzo/preview"
        ),
        CurriculumBookItem(
            id = "p1_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1SBZuYDeNnZA-vKImP7e7eF_MS1_fvqKH/preview"
        ),
        CurriculumBookItem(
            id = "p1_qiraa_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب نشاط القراءة",
            previewUrl = "https://drive.google.com/file/d/1d_GNbjjNfEVHyKI280-HfCwA5kzZenjY/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1unzZit-bzu0V1Ny09D3lSlyM51DrveY4/preview"
        ),
        CurriculumBookItem(
            id = "p1_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1Wmq-bnBuQ_g1NPCVbl0Nsqh5CdMdrNPB/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1OUj0igQwRx59GcRie0aWUyb4pQE5wEnq/preview"
        ),
        CurriculumBookItem(
            id = "p1_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1SMeNQfs5DWrlNVOVdjRoW-jkWiFBL2j7/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/15-J2FgKIlPSj7wJivAWAvDXFdUjmLCz2/preview"
        ),
        CurriculumBookItem(
            id = "p1_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1gLHO-fm4dhkkueJGw5XLlT1bj1zoNU0Z/preview"
        ),
        CurriculumBookItem(
            id = "p1_moral",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الأول الابتدائي",
            subjectTitle = "كتاب الاخلاقية",
            previewUrl = "https://drive.google.com/file/d/1vHLP9ScCf71VlfEuRSgUIFt1ohILZwHQ/preview"
        ),

        // ============================================================
        // 2. الصف الثاني الابتدائي
        // ============================================================
        CurriculumBookItem(
            id = "p2_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1tqXQbPZUcaxFlOujjAcgEnFaVLzn3TDN/preview"
        ),
        CurriculumBookItem(
            id = "p2_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1_hNiwMamcGRvHc-LWbIXTeQkXvbH84Z6/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1cKHbMM7vNc8eqcQ4J_GAG0lsva2Sp3hF/preview"
        ),
        CurriculumBookItem(
            id = "p2_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/15cJj1eGTHwJKabekweNAbqbAAvJlFlyM/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/19bt2LoEvw_V5-KoUzmh4XCCplHn2AhiK/preview"
        ),
        CurriculumBookItem(
            id = "p2_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1Xyh4H2DIXMkF6ujpe5mXdkr7EohfkK4K/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1jRHrNCVBOwhea-eL_wwPHxQZF3Vbb4uw/preview"
        ),
        CurriculumBookItem(
            id = "p2_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1ku1QwAD6lRuRJ-_soK4Fp4t9xrMdzJbq/preview"
        ),
        CurriculumBookItem(
            id = "p2_moral",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثاني الابتدائي",
            subjectTitle = "كتاب الاخلاقية",
            previewUrl = "https://drive.google.com/file/d/1Yg0LTjViam_KtL2rq5BgJJKC0ofm4bCe/preview"
        ),

        // ============================================================
        // 3. الصف الثالث الابتدائي
        // ============================================================
        CurriculumBookItem(
            id = "p3_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/17y-N39QiawgjuP7Cwe4cVQMB_IDaJ0Gm/preview"
        ),
        CurriculumBookItem(
            id = "p3_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1AcNuzBMToNiaZBMt6oFtaoD3KbIICxO9/preview"
        ),
        CurriculumBookItem(
            id = "p3_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1iqpDEJoUimDI1dbkR4X6lBIrZHJKJB5Z/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1u2h3i53amPXlc-_bIAv2EfULWOfgrz8t/preview"
        ),
        CurriculumBookItem(
            id = "p3_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1ctC0ZGiLIZbT-SuCK5OYS8vO2unXmArv/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1kBCR4fP0D9uvEVpXdM1CQb_KkzeqM2uC/preview"
        ),
        CurriculumBookItem(
            id = "p3_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الثالث الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1dBAuIuxmOaknT7B6V7lEH0HOlpFiMZrZ/preview"
        ),

        // ============================================================
        // 4. الصف الرابع الابتدائي (10 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p4_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/18nnctYHawWZMAvT6PtzidsXf0P6kPEvh/preview"
        ),
        CurriculumBookItem(
            id = "p4_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1kDacm2LZS9NJI2Gmg8qRS2eEN3OFOQWK/preview"
        ),
        CurriculumBookItem(
            id = "p4_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1i7ZTlozV1UZj5Q7VZVyuj4tRQL4ytNSC/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1hM1RSOGBmh0uGmtBt0FQMN_hc1z79myD/preview"
        ),
        CurriculumBookItem(
            id = "p4_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1-WTOo7uXmibGMg-E4o1Of7XSkrM3nTmB/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/175Hkdms5gmfM7QJjZRyGVIiGrg-uUILy/preview"
        ),
        CurriculumBookItem(
            id = "p4_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/16PtyMCCG3EuIRTe02_PEnPdTPvZ-KHkD/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1Ll_dwNKWgLfTUJk7eHxJrfTbkU9xxRsS/preview"
        ),
        CurriculumBookItem(
            id = "p4_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1PbmUFiYH1HCAu6RK5hhyT7BRxWBMEYz7/preview"
        ),
        CurriculumBookItem(
            id = "p4_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الرابع الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1ck0PDkGhCJBF2Bi6S49nAaBLhxqvIUom/preview"
        ),

        // ============================================================
        // 5. الصف الخامس الابتدائي (10 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p5_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/14kd8vicBKBeGU7RpdGaR3q-7LDbMyB1E/preview"
        ),
        CurriculumBookItem(
            id = "p5_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1nItax-W99MyE9CnKvkOWeDNVZC5fSAiW/preview"
        ),
        CurriculumBookItem(
            id = "p5_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1LSWC6mFrTH_DHFNQ8rVvYPCf3FHxhx3c/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1v9Q7FyE7tMddNZdVR2SSfDxu2ATHyS0f/preview"
        ),
        CurriculumBookItem(
            id = "p5_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1Q-s6t74OCk8jEip2NLsd-RbU_q0Yz25j/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1OEC1v11aSeb660yzQ83Tpu8C95BCnLyM/preview"
        ),
        CurriculumBookItem(
            id = "p5_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1BeziQKPCbVwXU-DZMze_E_efAJL2LHiz/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/1d_NaL3ZXWPg916TR2fl9PW1E_kWFz5-N/preview"
        ),
        CurriculumBookItem(
            id = "p5_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1WiOgm0jZ1AIUrD3Yh_8c3p8cRmW1MTuc/preview"
        ),
        CurriculumBookItem(
            id = "p5_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "الخامس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/18QTrNWPMTJPdE1KRppEK_RGnzZlCrkpz/preview"
        ),

        // ============================================================
        // 6. الصف السادس الابتدائي (10 كتب)
        // ============================================================
        CurriculumBookItem(
            id = "p6_islamic",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاسلامية",
            previewUrl = "https://drive.google.com/file/d/1eBGERuNCc9tN2nuDoX2-RxettdZEsMEB/preview"
        ),
        CurriculumBookItem(
            id = "p6_qiraa",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب القراءة",
            previewUrl = "https://drive.google.com/file/d/1UUOQ-2L3fm7WTXn8BbvrJvMfl65sy-wA/preview"
        ),
        CurriculumBookItem(
            id = "p6_grammar",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب القواعد",
            previewUrl = "https://drive.google.com/file/d/1hFA-4oXpZlx00j-73VA4Uk1b4fbGp-Wn/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1Q5STI-8XJVg_0de2iFVoHi1vEN6ghn3S/preview"
        ),
        CurriculumBookItem(
            id = "p6_english_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب نشاط الانكليزي",
            previewUrl = "https://drive.google.com/file/d/1UhdM7oBvmc6nU6YIrR0iLGEFPhvNyzZf/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1XKUwiS0p2SvSvtUyQDu0xERdbFW382S7/preview"
        ),
        CurriculumBookItem(
            id = "p6_math_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب تمرينات الرياضيات",
            previewUrl = "https://drive.google.com/file/d/1u4uxLEp5an4YT7ELlud36mEn4alDXQEC/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_sb",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب العلوم",
            previewUrl = "https://drive.google.com/file/d/13VFMpKRCNDvX9RmRmvD7kdYUcyURtLkj/preview"
        ),
        CurriculumBookItem(
            id = "p6_science_ab",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب نشاط العلوم",
            previewUrl = "https://drive.google.com/file/d/1-BEJUHYEPjs_7hB7pvAzvfxGoYRGb7AU/preview"
        ),
        CurriculumBookItem(
            id = "p6_social",
            stage = CurriculumAcademicStage.PRIMARY,
            gradeClass = "السادس الابتدائي",
            subjectTitle = "كتاب الاجتماعيات",
            previewUrl = "https://drive.google.com/file/d/1Lrs9Pp_1rNwSIKHS7oVjG8AoHRxNdbbm/preview"
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

    val primaryAccent = MaterialTheme.colorScheme.primary

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .testTag("curriculum_books_screen"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.height(6.dp))

            // 1. Clean Stage Selector Pills (الابتدائية • المتوسطة • الإعدادية) — Zero Icons
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
                            primaryAccent
                        } else if (isDarkMode) {
                            Color(0xFF1E293B)
                        } else {
                            Color.White
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) primaryAccent else Color(0xFFCBD5E1).copy(alpha = 0.65f)
                        ),
                        shadowElevation = if (isSelected) 3.dp else 1.dp,
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("curriculum_stage_tab_${stage.name}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
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
                                    Color(0xFF0F172A)
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // 2. Clean Grade Selector Pills — Zero Icons
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
                            primaryAccent
                        } else if (isDarkMode) {
                            Color(0xFF0F172A)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isGradeSelected) primaryAccent else primaryAccent.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("curriculum_grade_chip_$gradeClass")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
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
                                    Color(0xFFE2E8F0)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                        }
                    }
                }
            }

            // 3. Clean Search Bar — Typography-only clear affordance, zero classic icons
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
                        Text(
                            text = "مسح",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { searchQuery = "" }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("curriculum_search_input")
            )

            // 4. Right-aligned Clean Section Header below Search Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "كتب الصف: $selectedGradeClass (${displayedBooks.size} منهج رسمي)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                )
            }

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
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = if (searchQuery.isNotBlank()) {
                                    "لا توجد كتب مطابقة لعبارة البحث الحالية"
                                } else {
                                    "مناهج هذه المرحلة قيد الرفع والتحديث.. ستتوفر قريباً"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            if (searchQuery.isBlank()) {
                                Text(
                                    text = "${selectedStage.capsuleStageWord} • $selectedGradeClass",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = primaryAccent,
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
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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

        // Full-Screen In-App Book Viewer Modal (Exclusively inside the app via /preview iframe)
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
 * Clean White Rounded Book Card (Zero Classic Icons Policy):
 * Contains ONLY 3 elements ordered right-to-left, top-to-bottom:
 *   1. Book Name in bold, clear text on the right (e.g., "كتاب الاسلامية").
 *   2. Below it: a soft light-green capsule badge with a thin border containing: "ابتدائي • الأول الابتدائي".
 *   3. At the bottom of the card: a solid, sleek full-width green button containing text ONLY: "فتح مباشر".
 */
@Composable
private fun CurriculumBookOfficialCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onDirectOpenBook: () -> Unit
) {
    val primaryAccent = MaterialTheme.colorScheme.primary
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
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Book Name in bold clear text on the right
            Text(
                text = book.subjectTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. Soft light-green capsule badge with thin border: "ابتدائي • الأول الابتدائي"
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.55f) else Color(0xFFECFDF5),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isDarkMode) Color(0xFF34D399).copy(alpha = 0.45f) else Color(0xFFA7F3D0)
                )
            ) {
                Text(
                    text = "${book.stage.capsuleStageWord} • ${book.gradeClass}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // 3. Solid sleek full-width green button containing text ONLY: "فتح مباشر"
            Button(
                onClick = onDirectOpenBook,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryAccent,
                    contentColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
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
 * - Embeds the exact `/preview` link inside an HTML `<iframe>` (`<iframe src="https://drive.google.com/file/d/FILE_ID/preview" width="100%" height="100%" allow="autoplay" style="border:none;"></iframe>`)
 *   so Google Drive renders the book cleanly inside the app without any external redirects.
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
    val iframeHtml = remember(embedPreviewUrl) {
        """
        <!DOCTYPE html>
        <html dir="rtl" lang="ar">
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=5.0, user-scalable=yes" />
            <style>
                html, body {
                    margin: 0;
                    padding: 0;
                    width: 100%;
                    height: 100%;
                    overflow: hidden;
                    background-color: #0F172A;
                }
                iframe {
                    width: 100%;
                    height: 100%;
                    border: none;
                    display: block;
                }
            </style>
        </head>
        <body>
            <iframe src="$embedPreviewUrl" width="100%" height="100%" allow="autoplay" style="border:none;"></iframe>
        </body>
        </html>
        """.trimIndent()
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
                    // Clean Typography Top Bar (Zero Classic Icons)
                    Surface(
                        color = Color(0xFF042F24),
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
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
                                    color = Color(0xFFA7F3D0),
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
                                        webViewInstance?.loadDataWithBaseURL(
                                            "https://drive.google.com",
                                            iframeHtml,
                                            "text/html",
                                            "UTF-8",
                                            null
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0).copy(alpha = 0.45f)),
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
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFA7F3D0)
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
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

                    // Full-Screen Embedded WebView with `/preview` iframe
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
                                        }

                                        override fun shouldOverrideUrlLoading(
                                            view: WebView?,
                                            request: WebResourceRequest?
                                        ): Boolean {
                                            val targetUrl = request?.url?.toString().orEmpty()
                                            if (targetUrl.isBlank()) return true
                                            return if (
                                                targetUrl.contains("drive.google.com") ||
                                                targetUrl.contains("docs.google.com") ||
                                                targetUrl.contains("googleusercontent.com")
                                            ) {
                                                false
                                            } else {
                                                true
                                            }
                                        }
                                    }
                                    webViewInstance = this
                                    loadDataWithBaseURL(
                                        "https://drive.google.com",
                                        iframeHtml,
                                        "text/html",
                                        "UTF-8",
                                        null
                                    )
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
 * مدير روابط المناهج الدراسية (محفوظ حصراً داخل إعدادات المطور)
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
                        Text(
                            text = "×",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            modifier = Modifier
                                .clickable { statusMessage = null }
                                .padding(horizontal = 8.dp)
                        )
                    }
                }
            }

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
