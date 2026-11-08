package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.webkit.CookieManager
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.model.ArabicTextNormalizer
import com.example.model.SchoolStage
import com.example.model.UserRole
import com.example.ui.theme.DeepEmerald
import com.example.viewmodel.SchoolViewModel
import java.io.File

private const val MIN_VALID_BOOK_BYTES = 50_000L

private fun normalizeCurriculumPdfUrl(rawUrl: String): String {
    return rawUrl
        .trim()
        .replaceFirst("^http://".toRegex(RegexOption.IGNORE_CASE), "https://")
        .replaceFirst("^Https://".toRegex(), "https://")
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
    val id: String = "",
    val subjectTitle: String = "",
    val stage: CurriculumStageTab = CurriculumStageTab.PRIMARY,
    val gradeClass: String = "الأول الابتدائي",
    val pdfUrl: String = ""
) {
    val normalizedPdfUrl: String
        get() = normalizeCurriculumPdfUrl(pdfUrl)

    val remoteFileName: String
        get() = normalizedPdfUrl.substringAfterLast("/").substringBefore("?").trim()

    val remoteFileNumber: String
        get() = remoteFileName.substringBeforeLast(".").trim()
}

/**
 * الفهرس الرسمي المعتمد لروابط المناهج الدراسية العراقية (56 كتاباً منهجياً رسمياً بصيغة /preview).
 */
object IraqiCurriculumCatalog {

    val allBooks: List<CurriculumBookItem> = listOf(
        // 1. الأول الابتدائي (9 كتب)
        CurriculumBookItem("pri_1_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1nVM1dhoZJJ2kwYwMLRMlFfo3a70rOKrD/preview"),
        CurriculumBookItem("pri_1_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1zM3D1AUpzxzhiytq3vVmyYOnSGJqM4lP/preview"),
        CurriculumBookItem("pri_1_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1AXlhEx5zeA2PUfiDsxJGQXaksFyQiVsc/preview"),
        CurriculumBookItem("pri_1_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1-FLrmZmDfOLoxh47eYCPnWCK1f4CcDR3/preview"),
        CurriculumBookItem("pri_1_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1w0_8y7osEzGCST5Kj_b8HtL9gRnHqBcu/preview"),
        CurriculumBookItem("pri_1_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1e1TGmQX5RVQgyEeb7gHslt328UEpwJFX/preview"),
        CurriculumBookItem("pri_1_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1ohJZlWvvnZ-d_fQSVL7iTjdEY5EeaQU6/preview"),
        CurriculumBookItem("pri_1_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1SSmMfZYvWCkEDfk-lfCWT3ni4LbrZl-a/preview"),
        CurriculumBookItem("pri_1_ethics", "كتاب الاخلاقية", CurriculumStageTab.PRIMARY, "الأول الابتدائي", "https://drive.google.com/file/d/1ugtWInqi8S-reTElSAd5r2AXmFUoEQQ2/preview"),

        // 2. الثاني الابتدائي (8 كتب)
        CurriculumBookItem("pri_2_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1vPIz7JbjlfZBAXJQe95jBAypa_YuSxr8/preview"),
        CurriculumBookItem("pri_2_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1JIa29yJ_Ld9A_Oe31n7lgScUSiCAEhYG/preview"),
        CurriculumBookItem("pri_2_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1fLZBPEG9bgczvcZvxN2jBSuNR0pMMne9/preview"),
        CurriculumBookItem("pri_2_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1qcYh1Vx3W5Qs6VAnDo0biC2Pf3Fl66em/preview"),
        CurriculumBookItem("pri_2_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/10h-L5LKKVQM9XvqknVtzhPPUXIjhqviF/preview"),
        CurriculumBookItem("pri_2_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/117MuokgO4X-sri5OP69GX4hQXSxawFDZ/preview"),
        CurriculumBookItem("pri_2_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1VQy3aIsgXYrP9TBibfV6TKM570AmqFtj/preview"),
        CurriculumBookItem("pri_2_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "الثاني الابتدائي", "https://drive.google.com/file/d/1SFE2frCT3DtwuNgp2vyIhEV5-VM836YT/preview"),

        // 3. الثالث الابتدائي (8 كتب)
        CurriculumBookItem("pri_3_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1iIoCA9vUMjsYLGcYB3bG9ghH1yJTtA3j/preview"),
        CurriculumBookItem("pri_3_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1hX263Pr8NUNHLUokm_dxklapZNbsWwey/preview"),
        CurriculumBookItem("pri_3_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1gjmF4BkLbfDNQ4aSBps2YLwkJ8Whlb6A/preview"),
        CurriculumBookItem("pri_3_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1uUqy4egLScXQ618bc0ynoi-UQdSdrvWO/preview"),
        CurriculumBookItem("pri_3_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1L_LHQS1Q8S-1R4J7h3-SlDl4IvZMv0ry/preview"),
        CurriculumBookItem("pri_3_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1gNkRMv0ToIJEeEfLpdpQZ4Aex1P4r-bV/preview"),
        CurriculumBookItem("pri_3_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1x2XtEK97fBrP-Fuw1fzRtbXVI5mHQfw9/preview"),
        CurriculumBookItem("pri_3_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "الثالث الابتدائي", "https://drive.google.com/file/d/1rMFsFuJSbDLYO17R-4_wzx5Dj_01dJvD/preview"),

        // 4. الرابع الابتدائي (10 كتب)
        CurriculumBookItem("pri_4_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1Qa6FOlInBlFlafEon5rtHvU4DWbQEdhn/preview"),
        CurriculumBookItem("pri_4_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1Ea8lcedAR_7JPhiwVGLu1J4QizhxXDIR/preview"),
        CurriculumBookItem("pri_4_grammar", "كتاب القواعد", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1D0H2HFkgFtrUkfu9_jfqPlDSPuqV8gHv/preview"),
        CurriculumBookItem("pri_4_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1_Rs159GJB3UKzjHjAoDmDfx3nAHEYcC0/preview"),
        CurriculumBookItem("pri_4_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1Ph31Q_l9dSpq50dUQgOAVFYQxyBLIzZf/preview"),
        CurriculumBookItem("pri_4_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/10N1acdhtc1HPkvPKjyeKfsy-WQ_q4GUk/preview"),
        CurriculumBookItem("pri_4_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1rJ-2jjQM5LdQNmWMq0kjpJfZW9IuoEQj/preview"),
        CurriculumBookItem("pri_4_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1ciJBefTfDhpQ5aScaHmd218ME7Wpkb_q/preview"),
        CurriculumBookItem("pri_4_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1g0_IfvcDt8OUi5u_66B_CzIgbuCobbog/preview"),
        CurriculumBookItem("pri_4_social", "كتاب الاجتماعيات", CurriculumStageTab.PRIMARY, "الرابع الابتدائي", "https://drive.google.com/file/d/1XxlWljt1fhXPBtoWg4wYdm_pQtI4z6Tc/preview"),

        // 5. الخامس الابتدائي (10 كتب)
        CurriculumBookItem("pri_5_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1QD4MKjg-JRNzmyDUOvAXsArKqU1FWSoD/preview"),
        CurriculumBookItem("pri_5_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1S9P_4htwstxFyk5RKoccW4bnN9DPlwo6/preview"),
        CurriculumBookItem("pri_5_grammar", "كتاب القواعد", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1KLKUU-6NqSLk68txNZOvrjXZQMYAsI92/preview"),
        CurriculumBookItem("pri_5_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1sbFi8uAP35zFSjav93bXoCDoEjOYZa0h/preview"),
        CurriculumBookItem("pri_5_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1WClbsxm9zoFvFm_g7tp8O8kNDzMNOdB0/preview"),
        CurriculumBookItem("pri_5_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/19GKZ3aOWyBZlPipgK7tVdIWXuHNEbM4u/preview"),
        CurriculumBookItem("pri_5_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1cSZp8Uszg1hKZb7te8FbNZY0r6eZyqSM/preview"),
        CurriculumBookItem("pri_5_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1z43l3wERXzT-egGjs_2puQM54lVaAQ30/preview"),
        CurriculumBookItem("pri_5_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1T7b4sdD0-3TQvub0M-HH3gCDNnGtNm3z/preview"),
        CurriculumBookItem("pri_5_social", "كتاب الاجتماعيات", CurriculumStageTab.PRIMARY, "الخامس الابتدائي", "https://drive.google.com/file/d/1RVz091kMA7w-XEn0f19WjTxMKzY4WE1g/preview"),

        // 6. السادس الابتدائي (11 كتاباً)
        CurriculumBookItem("pri_6_islamic", "كتاب الاسلامية", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1a_VcmloZrQIJv9D7dmOXKOubK3sSI5Zb/preview"),
        CurriculumBookItem("pri_6_reading", "كتاب القراءة", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1YsbcKHGfzCO6d1d0aXykdh1SOFsTnnH6/preview"),
        CurriculumBookItem("pri_6_grammar", "كتاب القواعد", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1POyExJU-KWzrWTNtXBNb4V5QaXKTnIZJ/preview"),
        CurriculumBookItem("pri_6_english", "كتاب الانكليزي", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1nGn8_KxVs2y-xWFqqUBcaHE3Z9k5AzzE/preview"),
        CurriculumBookItem("pri_6_english_act", "كتاب نشاط الانكليزي", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1yEWy4uW6sjY-SPJB2cpE4S4_YiCiNXZ0/preview"),
        CurriculumBookItem("pri_6_english_solved", "كتاب النشاط محلول", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1pjX1iSOA0QX9p-36KhJYdBIIDeak35Jy/preview"),
        CurriculumBookItem("pri_6_science", "كتاب العلوم", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/13zLlyffzFlZn6B5Z-qsZWleBp1c3KwGd/preview"),
        CurriculumBookItem("pri_6_sci_act", "كتاب نشاط العلوم", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1t410bKRHPExvnBgs9iPu1JjHhEoOOHmq/preview"),
        CurriculumBookItem("pri_6_math", "كتاب الرياضيات", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1qvYJnF__-cQg47cagZAXgIXV1EoZo_27/preview"),
        CurriculumBookItem("pri_6_math_act", "كتاب تمارين الرياضيات", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1eWnIMdPE901ff1owadlzwuHNH_rElBYg/preview"),
        CurriculumBookItem("pri_6_social", "كتاب الاجتماعيات", CurriculumStageTab.PRIMARY, "السادس الابتدائي", "https://drive.google.com/file/d/1qB5kJuih2TrN-4IL4pIM8MploPnOX0BD/preview")
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
            SchoolStage.SECONDARY -> CurriculumStageTab.PRIMARY to "الأول الابتدائي"
        }
    }

    fun getInternalBookFile(context: Context, bookId: String): File {
        val dir = File(context.filesDir, "books")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "book_${bookId}.pdf")
    }

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

@Composable
fun CurriculumScreen(
    viewModel: SchoolViewModel
) {
    CurriculumBooksScreen(viewModel = viewModel)
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

    // تجاهل وحذف أي بيانات مناهج قديمة مخزنة في SharedPreferences للاعتماد حصرياً على الروابط الـ 56 الرسمية
    LaunchedEffect(Unit) {
        runCatching {
            val prefs = context.getSharedPreferences("madrasati_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .remove("global_curriculum_config")
                .remove("curriculum_books_override")
                .remove("custom_curriculum_links")
                .apply()
        }
    }

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
    var activeWebViewBook by remember { mutableStateOf<CurriculumBookItem?>(null) }

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
                    it.stage == selectedStage &&
                        (ArabicTextNormalizer.normalizeText(it.subjectTitle).contains(cleanQuery) ||
                            ArabicTextNormalizer.normalizeText(it.gradeClass).contains(cleanQuery))
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("curriculum_books_screen_list"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1) محدد اختيار المرحلة والصف بتصميم ناعم + حقل البحث
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
                elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // محدد اختيار المرحلة بتصميم ناعم
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
                                color = if (isSelected) {
                                    Color(0xFF059669)
                                } else {
                                    if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) Color(0xFF059669) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stageTab.arabicTitle,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isSelected) Color.White else if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF334155),
                                        maxLines = 1,
                                        softWrap = false,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }

                    // محدد اختيار الصف بتصميم ناعم
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(selectedStage.classes, key = { it }) { classTitle ->
                            val isClassSelected = selectedClass == classTitle
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
                                    1.dp,
                                    if (isClassSelected) Color(0xFF34D399) else if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                )
                            ) {
                                Text(
                                    text = classTitle,
                                    fontSize = 12.sp,
                                    fontWeight = if (isClassSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    color = if (isClassSelected) {
                                        if (isDarkMode) Color.White else Color(0xFF065F46)
                                    } else {
                                        if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF475569)
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // حقل البحث (ابحث باسم المادة أو الكتاب المنهجي...)
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("curriculum_search_input"),
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(20.dp)
                            )
                        },
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

        // 2) عنوان نصي على اليمين مثل: كتب الصف: الأول الابتدائي (9 منهج رسمي)
        item(key = "on_demand_policy_notice") {
            Text(
                text = "كتب الصف: $selectedClass (${displayedBooks.size} منهج رسمي)",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // 3) عرض بطاقات الكتب النظيفة
        if (selectedStage == CurriculumStageTab.MIDDLE || selectedStage == CurriculumStageTab.PREPARATORY) {
            item(key = "stage_under_upload_notice_card") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("stage_coming_soon_card"),
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
                                .padding(horizontal = 24.dp, vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = "أيقونة الكتاب",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "مناهج هذه المرحلة قيد الرفع والتحديث.. ستتوفر قريباً",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        } else if (displayedBooks.isEmpty()) {
            item(key = "empty_curriculum_search") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
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
                            text = "جرب كتابة اسم المادة مثل: الرياضيات، العلوم، القراءة، أو الاسلامية.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(displayedBooks, key = { it.id }) { book ->
                CurriculumBookCleanCard(
                    book = book,
                    isDarkMode = isDarkMode,
                    onDirectOpenClick = {
                        activeWebViewBook = book
                    }
                )
            }
        }
    }

    // نافذة القراءة المدمجة بكامل الشاشة داخل التطبيق عبر AndroidView(factory = { WebView(it) })
    val targetBook = activeWebViewBook
    if (targetBook != null) {
        InAppGoogleDrivePreviewWebViewDialog(
            book = targetBook,
            onDismiss = { activeWebViewBook = null }
        )
    }
}

/**
 * تصميم بطاقة الكتاب النظيفة (Clean Card):
 * بطاقة بيضاء نظيفة بحواف دائرية ناعمة تحتوي فقط على 3 عناصر:
 * 1. اسم الكتاب بخط عريض وواضح في اليمين.
 * 2. تحته شارة كبسولة خضراء فاتحة ناعمة مكتوب بداخلها: `ابتدائي • الأول الابتدائي`.
 * 3. في أسفل البطاقة: زر أخضر عريض سادة بكامل عرض البطاقة مكتوب فيه نص فقط: `فتح مباشر` (بدون أيقونة).
 */
@Composable
private fun CurriculumBookCleanCard(
    book: CurriculumBookItem,
    isDarkMode: Boolean,
    onDirectOpenClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp)
            .clickable { onDirectOpenClick() }
            .testTag("curriculum_book_card_${book.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. اسم الكتاب بخط عريض وواضح في اليمين
            Text(
                text = book.subjectTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isDarkMode) Color.White else Color(0xFF0F172A),
                textAlign = TextAlign.Start,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            // 2. تحته شارة كبسولة خضراء فاتحة ناعمة مكتوب بداخلها: ابتدائي • الأول الابتدائي
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.65f) else Color(0xFFECFDF5),
                border = BorderStroke(
                    0.8.dp,
                    if (isDarkMode) Color(0xFF34D399).copy(alpha = 0.4f) else Color(0xFFA7F3D0)
                )
            ) {
                Text(
                    text = "${book.stage.shortBadge} • ${book.gradeClass}",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }

            // 3. في أسفل البطاقة: زر أخضر عريض سادة بكامل عرض البطاقة مكتوب فيه نص فقط: فتح مباشر (بدون أيقونة)
            Surface(
                onClick = onDirectOpenClick,
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF059669),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .testTag("btn_super_reader_${book.id}")
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "فتح مباشر",
                        fontSize = 13.5.sp,
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

/**
 * قارئ الكتب المدمج بكامل الشاشة داخل التطبيق باستخدام AndroidView(factory = { WebView(it) })
 * مع تفعيل settings.javaScriptEnabled = true و settings.domStorageEnabled = true
 * ليفتح رابط Google Drive بصيغة /preview مباشرة داخل التطبيق دون الخروج للمتصفح.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun InAppGoogleDrivePreviewWebViewDialog(
    book: CurriculumBookItem,
    onDismiss: () -> Unit
) {
    var isLoading by remember(book.id) { mutableStateOf(true) }

    BackHandler {
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
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
                // الشريط العلوي لنافذة القراءة المدمجة
                Surface(
                    color = Color(0xFF064E3B),
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
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
                                text = "${book.stage.shortBadge} • ${book.gradeClass}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Surface(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                            modifier = Modifier.testTag("reader_close_capsule_btn")
                        ) {
                            Text(
                                text = "إغلاق",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF10B981),
                        trackColor = Color(0xFF064E3B)
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("curriculum_in_app_webview"),
                        factory = { WebView(it) },
                        update = { webView ->
                            webView.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = true
                                displayZoomControls = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            }
                            runCatching {
                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(webView, true)
                            }
                            webView.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val url = request?.url?.toString().orEmpty()
                                    // إبقاء التصفح داخل WebView المدمج ومنع فتح المتصفح الخارجي
                                    if (url.startsWith("https://") || url.startsWith("http://")) {
                                        return false
                                    }
                                    return true
                                }
                            }
                            val targetUrl = book.normalizedPdfUrl
                            if (webView.url != targetUrl && webView.originalUrl != targetUrl) {
                                webView.loadUrl(targetUrl)
                            }
                        }
                    )

                    androidx.compose.animation.AnimatedVisibility(
                        visible = isLoading,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF0F172A).copy(alpha = 0.85f),
                            border = BorderStroke(1.dp, Color(0xFF34D399).copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.5.dp,
                                    color = Color(0xFF10B981)
                                )
                                Text(
                                    text = "جاري فتح ${book.subjectTitle}...",
                                    fontSize = 12.5.sp,
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

/**
 * قسم إدارة واستعراض روابط المناهج الدراسية داخل "إعدادات المطور" فقط (DeveloperControlPanelScreen).
 */
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
                    text = "مدير روابط المناهج الدراسية (${IraqiCurriculumCatalog.allBooks.size} منهج رسمي)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "إدارة وفحص روابط المناهج الدراسية العراقية الرسمية (Google Drive /preview) داخل إعدادات المطور فقط",
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
                        text = "فتح مدير روابط المناهج الدراسية",
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
                                text = "مدير روابط المناهج الدراسية (${IraqiCurriculumCatalog.allBooks.size} كتاب)",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.5.sp
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
