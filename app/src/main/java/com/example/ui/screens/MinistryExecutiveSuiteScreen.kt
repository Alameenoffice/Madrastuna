package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.*
import com.example.ui.components.ArabicCalendarDateHelper
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

// =========================================================================
// 1. DATA MODELS & PERSISTENT STORE FOR SCHOOL ADMIN & EXAM CONTROL SUITE
// =========================================================================

enum class MinistrySuiteSectionTab(
    val index: Int,
    val arabicTitle: String,
    val badgeText: String
) {
    EXAM_CONTROL(0, "اللجنة الامتحانية والكنترول", "الكنترول والإعفاء"),
    OFFICIAL_RECORDS(1, "السجلات الرسمية وإنذارات الغياب", "القيود والإنذارات"),
    STAFF_PORTFOLIO(2, "شؤون الملاك والإضبارة الإلكترونية", "الملاك والخدمة"),
    STUDENT_WELFARE(3, "الرعاية الطلابية والأيتام والمنحة", "الأيتام والمنحة"),
    YEAR_ARCHIVE_PROMOTION(4, "الأرشفة وختم السنة والترحيل", "الترحيل والخريجون"),
    INVENTORY_EXPENSES(5, "جرد الممتلكات والحسابات والمصروفات", "الجرد والمالية")
}

data class ExamSeatAssignment(
    val hallNumber: Int,
    val hallName: String,
    val seatNumber: Int,
    val sectorCode: String,
    val student: StudentRecord
)

data class ExamQuestionItemDraft(
    val questionNumber: Int,
    val mainPrompt: String = "",
    val branchA: String = "",
    val branchB: String = "",
    val marks: Int = 20,
    val branches: List<String> = emptyList()
) {
    fun effectiveBranches(): List<String> {
        if (branches.isNotEmpty()) return branches
        return listOfNotNull(
            branchA.takeIf { it.isNotBlank() },
            branchB.takeIf { it.isNotBlank() }
        )
    }
}

data class OfficialCorrespondenceRecord(
    val id: String = "cor_${UUID.randomUUID().toString().take(6)}",
    val type: String, // "صادر" | "وارد" | "أمر إداري"
    val bookNumber: String,
    val dateIso: String,
    val entityName: String,
    val subject: String,
    val notes: String,
    val imageBase64: String = ""
)

data class StaffPortfolioRecord(
    val teacherId: String,
    val qualification: String = "",
    val startDateIso: String = "",
    val jobTitle: String = "",
    val jobGradeStage: String = "",
    val adminOrderNumber: String = "",
    val assignedClasses: String = "",
    val specialty: String = ""
)

data class StudentWelfareRecord(
    val studentId: String,
    val orphanStatus: String = "غير يتيم", // "غير يتيم" | "يتيم الأب" | "يتيم الأم" | "يتيم الأبوين"
    val isSocialSecurityGrant: Boolean = false,
    val isSpecialNeedsOrLowIncome: Boolean = false,
    val socialSecurityNumber: String = "",
    val monthlyGrantIqd: Long = 30000L,
    val welfareNotes: String = ""
)

data class ArchivedSchoolYearSnapshot(
    val id: String = "arch_${UUID.randomUUID().toString().take(6)}",
    val academicYear: String,
    val sealedDateIso: String,
    val sealedByPrincipal: String,
    val totalStudents: Int,
    val promotedCount: Int,
    val repeatingCount: Int,
    val graduatedCount: Int,
    val graduatesNames: List<String>,
    val summaryNotes: String
)

data class SchoolInventoryItemRecord(
    val id: String = "inv_${UUID.randomUUID().toString().take(6)}",
    val itemName: String,
    val category: String,
    val quantity: Int,
    val conditionStatus: String,
    val locationRoom: String,
    val notes: String
)

data class SchoolExpenseItemRecord(
    val id: String = "exp_${UUID.randomUUID().toString().take(6)}",
    val amountIqd: Long,
    val category: String,
    val dateIso: String,
    val recipientName: String,
    val receiptNumber: String,
    val notes: String
)

data class ExemptionAndWarningThresholds(
    val generalExemptMinAvg: Int = 85,
    val generalExemptMinSubject: Int = 75,
    val individualExemptMinScore: Int = 90,
    val decisionPointsLimit: Int = 5,
    val firstWarningDays: Int = 7,
    val finalWarningDays: Int = 15,
    val dismissalWarningDays: Int = 21
)

/**
 * مدير الحفظ الدائم والمزامنة الفورية لمركز الإدارة المدرسية واللجنة الامتحانية
 * يحفظ جميع التعديلات فوراً في SharedPreferences الخاصة بالمدرسة (`ministry_suite_data_${schoolCode}`)
 * ويرفعها تلقائياً إلى عقدة المدرسة في Firebase (`schools/${schoolCode}/ministrySuiteData`).
 */
object MinistryDataPersistenceManager {
    private const val LEGACY_PREF_NAME = "ministry_executive_suite_store"
    private val LEGACY_FAKE_COR_IDS = setOf("cor_default_1", "cor_default_2", "cor_default_3")
    private val LEGACY_FAKE_INV_IDS = setOf("inv_1", "inv_2", "inv_3", "inv_4", "inv_5")
    private val LEGACY_FAKE_EXP_IDS = setOf("exp_1", "exp_2")

    private fun cleanId(schoolId: String): String =
        schoolId.trim().ifBlank { "default_school" }

    private fun schoolPref(context: Context, schoolId: String) =
        context.getSharedPreferences("ministry_suite_data_${cleanId(schoolId)}", Context.MODE_PRIVATE)

    private fun legacyPref(context: Context) =
        context.getSharedPreferences(LEGACY_PREF_NAME, Context.MODE_PRIVATE)

    private fun getStringWithFallback(context: Context, schoolId: String, keySuffix: String): String? {
        val sid = cleanId(schoolId)
        val primary = schoolPref(context, sid).getString(keySuffix, null)
        if (!primary.isNullOrBlank()) return primary
        return legacyPref(context).getString("${sid}_$keySuffix", null)
    }

    private fun putStringAndSync(context: Context, schoolId: String, keySuffix: String, value: String) {
        val sid = cleanId(schoolId)
        schoolPref(context, sid).edit().putString(keySuffix, value).apply()
        legacyPref(context).edit().putString("${sid}_$keySuffix", value).apply()
        pushSchoolSuiteToFirebase(context, sid)
    }

    fun pushSchoolSuiteToFirebase(context: Context, schoolId: String) {
        val sid = cleanId(schoolId)
        runCatching {
            val sp = schoolPref(context, sid)
            val lp = legacyPref(context)
            val root = JSONObject().apply {
                put("schoolId", sid)
                put("updatedAtMillis", System.currentTimeMillis())
                put("nextOfficialDocNumber", getNextOfficialDocNumber(context, sid))
                put("smartIdExamPassEnabled", isSmartIdExamPassEnabled(context, sid))
                val t = loadThresholds(context, sid)
                put("thresholds", JSONObject().apply {
                    put("generalExemptMinAvg", t.generalExemptMinAvg)
                    put("generalExemptMinSubject", t.generalExemptMinSubject)
                    put("individualExemptMinScore", t.individualExemptMinScore)
                    put("decisionPointsLimit", t.decisionPointsLimit)
                    put("firstWarningDays", t.firstWarningDays)
                    put("finalWarningDays", t.finalWarningDays)
                    put("dismissalWarningDays", t.dismissalWarningDays)
                })
                put("correspondence", JSONArray(getStringWithFallback(context, sid, "correspondence") ?: "[]"))
                put("staffPortfolios", JSONArray(getStringWithFallback(context, sid, "staff_portfolios") ?: "[]"))
                put("studentWelfare", JSONArray(getStringWithFallback(context, sid, "student_welfare") ?: "[]"))
                put("archivedYears", JSONArray(getStringWithFallback(context, sid, "archived_years") ?: "[]"))
                put("inventory", JSONArray(getStringWithFallback(context, sid, "inventory") ?: "[]"))
                put("expenses", JSONArray(getStringWithFallback(context, sid, "expenses") ?: "[]"))
                put("examQuestions", JSONArray(getStringWithFallback(context, sid, "exam_questions") ?: "[]"))

                // Per-student exam eligibility & hall/seat map
                val examAllowedObj = JSONObject()
                val examSeatObj = JSONObject()
                val examHallNumObj = JSONObject()
                val examSeatNumObj = JSONObject()
                (lp.all.keys + sp.all.keys).forEach { k ->
                    when {
                        k.startsWith("${sid}_exam_allowed_") -> {
                            val stuId = k.removePrefix("${sid}_exam_allowed_")
                            examAllowedObj.put(stuId, isStudentExamAllowed(context, sid, stuId))
                        }
                        k.startsWith("exam_allowed_") -> {
                            val stuId = k.removePrefix("exam_allowed_")
                            examAllowedObj.put(stuId, isStudentExamAllowed(context, sid, stuId))
                        }
                        k.startsWith("${sid}_exam_seat_") -> {
                            val stuId = k.removePrefix("${sid}_exam_seat_")
                            examSeatObj.put(stuId, getStudentExamSeatLabel(context, sid, stuId))
                        }
                        k.startsWith("exam_seat_") -> {
                            val stuId = k.removePrefix("exam_seat_")
                            examSeatObj.put(stuId, getStudentExamSeatLabel(context, sid, stuId))
                        }
                        k.startsWith("exam_hall_num_") -> {
                            val stuId = k.removePrefix("exam_hall_num_")
                            examHallNumObj.put(stuId, sp.getInt(k, 1))
                        }
                        k.startsWith("exam_seat_num_") -> {
                            val stuId = k.removePrefix("exam_seat_num_")
                            examSeatNumObj.put(stuId, sp.getInt(k, 1))
                        }
                    }
                }
                put("examAllowedMap", examAllowedObj)
                put("examSeatMap", examSeatObj)
                put("examHallNumMap", examHallNumObj)
                put("examSeatNumMap", examSeatNumObj)
            }
            com.example.data.FirebaseCloudSync.pushSubCollectionAsync(
                schoolId = sid,
                subCollection = "ministrySuiteData",
                jsonPayload = root.toString()
            )
        }
    }

    suspend fun syncFromFirebaseIfAvailable(context: Context, schoolId: String): Boolean {
        val sid = cleanId(schoolId)
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val payload = com.example.data.FirebaseCloudSync.fetchSubCollectionPayload(sid, "ministrySuiteData")
                if (payload.isNullOrBlank()) return@withContext false
                val root = JSONObject(payload)
                val cloudUpdated = root.optLong("updatedAtMillis", 0L)
                val sp = schoolPref(context, sid)
                val localUpdated = sp.getLong("updatedAtMillis", 0L)
                if (cloudUpdated <= 0L && localUpdated > 0L) return@withContext false

                val spEdit = sp.edit()
                val lpEdit = legacyPref(context).edit()
                spEdit.putLong("updatedAtMillis", cloudUpdated.coerceAtLeast(System.currentTimeMillis()))

                if (root.has("nextOfficialDocNumber")) {
                    val nextNum = root.optInt("nextOfficialDocNumber", 1).coerceAtLeast(1)
                    spEdit.putInt("next_official_doc_number", nextNum)
                    lpEdit.putInt("${sid}_next_official_doc_number", nextNum)
                }
                if (root.has("smartIdExamPassEnabled")) {
                    val enabled = root.optBoolean("smartIdExamPassEnabled", true)
                    spEdit.putBoolean("smart_id_exam_pass_enabled", enabled)
                    lpEdit.putBoolean("${sid}_smart_id_exam_pass_enabled", enabled)
                }
                root.optJSONObject("thresholds")?.let { tObj ->
                    val th = ExemptionAndWarningThresholds(
                        generalExemptMinAvg = tObj.optInt("generalExemptMinAvg", 85),
                        generalExemptMinSubject = tObj.optInt("generalExemptMinSubject", 75),
                        individualExemptMinScore = tObj.optInt("individualExemptMinScore", 90),
                        decisionPointsLimit = tObj.optInt("decisionPointsLimit", 5),
                        firstWarningDays = tObj.optInt("firstWarningDays", 7),
                        finalWarningDays = tObj.optInt("finalWarningDays", 15),
                        dismissalWarningDays = tObj.optInt("dismissalWarningDays", 21)
                    )
                    spEdit.putInt("gen_exempt_avg", th.generalExemptMinAvg)
                    spEdit.putInt("gen_exempt_min_sub", th.generalExemptMinSubject)
                    spEdit.putInt("ind_exempt_score", th.individualExemptMinScore)
                    spEdit.putInt("decision_pts_limit", th.decisionPointsLimit)
                    spEdit.putInt("warn_first_days", th.firstWarningDays)
                    spEdit.putInt("warn_final_days", th.finalWarningDays)
                    spEdit.putInt("warn_dismiss_days", th.dismissalWarningDays)

                    lpEdit.putInt("${sid}_gen_exempt_avg", th.generalExemptMinAvg)
                    lpEdit.putInt("${sid}_gen_exempt_min_sub", th.generalExemptMinSubject)
                    lpEdit.putInt("${sid}_ind_exempt_score", th.individualExemptMinScore)
                    lpEdit.putInt("${sid}_decision_pts_limit", th.decisionPointsLimit)
                    lpEdit.putInt("${sid}_warn_first_days", th.firstWarningDays)
                    lpEdit.putInt("${sid}_warn_final_days", th.finalWarningDays)
                    lpEdit.putInt("${sid}_warn_dismiss_days", th.dismissalWarningDays)
                }

                listOf(
                    "correspondence" to "correspondence",
                    "staffPortfolios" to "staff_portfolios",
                    "studentWelfare" to "student_welfare",
                    "archivedYears" to "archived_years",
                    "inventory" to "inventory",
                    "expenses" to "expenses",
                    "examQuestions" to "exam_questions"
                ).forEach { (jsonKey, prefKey) ->
                    root.optJSONArray(jsonKey)?.let { arr ->
                        val localStr = getStringWithFallback(context, sid, prefKey)
                        if (arr.length() > 0 || localStr.isNullOrBlank()) {
                            spEdit.putString(prefKey, arr.toString())
                            lpEdit.putString("${sid}_$prefKey", arr.toString())
                        }
                    }
                }

                root.optJSONObject("examAllowedMap")?.let { obj ->
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val stuId = keys.next()
                        val allowed = obj.optBoolean(stuId, true)
                        spEdit.putBoolean("exam_allowed_$stuId", allowed)
                        lpEdit.putBoolean("${sid}_exam_allowed_$stuId", allowed)
                    }
                }
                root.optJSONObject("examSeatMap")?.let { obj ->
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val stuId = keys.next()
                        val label = obj.optString(stuId, "")
                        if (label.isNotBlank()) {
                            spEdit.putString("exam_seat_$stuId", label)
                            lpEdit.putString("${sid}_exam_seat_$stuId", label)
                        }
                    }
                }
                root.optJSONObject("examHallNumMap")?.let { obj ->
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val stuId = keys.next()
                        val hNum = obj.optInt(stuId, 1)
                        spEdit.putInt("exam_hall_num_$stuId", hNum)
                        lpEdit.putInt("${sid}_exam_hall_num_$stuId", hNum)
                    }
                }
                root.optJSONObject("examSeatNumMap")?.let { obj ->
                    val keys = obj.keys()
                    while (keys.hasNext()) {
                        val stuId = keys.next()
                        val sNum = obj.optInt(stuId, 1)
                        spEdit.putInt("exam_seat_num_$stuId", sNum)
                        lpEdit.putInt("${sid}_exam_seat_num_$stuId", sNum)
                    }
                }

                spEdit.apply()
                lpEdit.apply()
                true
            }.getOrDefault(false)
        }
    }

    fun getNextOfficialDocNumber(context: Context, schoolId: String): Int {
        val sid = cleanId(schoolId)
        val sp = schoolPref(context, sid)
        if (sp.contains("next_official_doc_number")) {
            return sp.getInt("next_official_doc_number", 1).coerceAtLeast(1)
        }
        val existingCount = loadCorrespondence(context, sid).size
        return (existingCount + 1).coerceAtLeast(1)
    }

    fun peekNextDocNumber(context: Context, schoolId: String): Int =
        getNextOfficialDocNumber(context, schoolId)

    fun peekNextOfficialBookNumberFormatted(context: Context, schoolId: String): String {
        val num = getNextOfficialDocNumber(context, schoolId)
        return "إداري / $num"
    }

    fun nextAndIncrementDocNumber(context: Context, schoolId: String): String {
        val sid = cleanId(schoolId)
        val current = getNextOfficialDocNumber(context, sid)
        val next = current + 1
        schoolPref(context, sid).edit()
            .putInt("next_official_doc_number", next)
            .putLong("updatedAtMillis", System.currentTimeMillis())
            .apply()
        legacyPref(context).edit()
            .putInt("${sid}_next_official_doc_number", next)
            .apply()
        pushSchoolSuiteToFirebase(context, sid)
        return "إداري / $current"
    }

    fun consumeNextDocNumberFormatted(context: Context, schoolId: String): String =
        nextAndIncrementDocNumber(context, schoolId)

    fun archiveOfficialDocumentAutomatically(
        context: Context,
        schoolId: String,
        bookNumber: String,
        docType: String = "صادر",
        entityName: String = "إدارة المدرسة / أولياء الأمور",
        subject: String,
        notes: String = "",
        imageBase64: String = ""
    ): List<OfficialCorrespondenceRecord> {
        val sid = cleanId(schoolId)
        val currentList = loadCorrespondence(context, sid)
        val todayIso = ArabicCalendarDateHelper.todayIsoDate()
        val alreadyExists = currentList.any {
            it.bookNumber == bookNumber && it.subject == subject && it.dateIso == todayIso
        }
        if (alreadyExists) return currentList
        val record = OfficialCorrespondenceRecord(
            type = docType,
            bookNumber = bookNumber,
            dateIso = todayIso,
            entityName = entityName,
            subject = subject,
            notes = notes.ifBlank { "تم إصداره وأرشفته تلقائياً من مركز الإدارة المدرسية" },
            imageBase64 = imageBase64
        )
        val updated = listOf(record) + currentList
        saveCorrespondence(context, sid, updated)
        return updated
    }

    fun loadThresholds(context: Context, schoolId: String): ExemptionAndWarningThresholds {
        val sid = cleanId(schoolId)
        val sp = schoolPref(context, sid)
        val lp = legacyPref(context)
        return ExemptionAndWarningThresholds(
            generalExemptMinAvg = (if (sp.contains("gen_exempt_avg")) sp.getInt("gen_exempt_avg", 85) else lp.getInt("${sid}_gen_exempt_avg", 85)).coerceIn(50, 100),
            generalExemptMinSubject = (if (sp.contains("gen_exempt_min_sub")) sp.getInt("gen_exempt_min_sub", 75) else lp.getInt("${sid}_gen_exempt_min_sub", 75)).coerceIn(50, 100),
            individualExemptMinScore = (if (sp.contains("ind_exempt_score")) sp.getInt("ind_exempt_score", 90) else lp.getInt("${sid}_ind_exempt_score", 90)).coerceIn(50, 100),
            decisionPointsLimit = (if (sp.contains("decision_pts_limit")) sp.getInt("decision_pts_limit", 5) else lp.getInt("${sid}_decision_pts_limit", 5)).coerceIn(1, 25),
            firstWarningDays = (if (sp.contains("warn_first_days")) sp.getInt("warn_first_days", 7) else lp.getInt("${sid}_warn_first_days", 7)).coerceIn(1, 60),
            finalWarningDays = (if (sp.contains("warn_final_days")) sp.getInt("warn_final_days", 15) else lp.getInt("${sid}_warn_final_days", 15)).coerceIn(2, 90),
            dismissalWarningDays = (if (sp.contains("warn_dismiss_days")) sp.getInt("warn_dismiss_days", 21) else lp.getInt("${sid}_warn_dismiss_days", 21)).coerceIn(3, 120)
        )
    }

    fun saveThresholds(context: Context, schoolId: String, thresholds: ExemptionAndWarningThresholds) {
        val sid = cleanId(schoolId)
        schoolPref(context, sid).edit()
            .putInt("gen_exempt_avg", thresholds.generalExemptMinAvg)
            .putInt("gen_exempt_min_sub", thresholds.generalExemptMinSubject)
            .putInt("ind_exempt_score", thresholds.individualExemptMinScore)
            .putInt("decision_pts_limit", thresholds.decisionPointsLimit)
            .putInt("warn_first_days", thresholds.firstWarningDays)
            .putInt("warn_final_days", thresholds.finalWarningDays)
            .putInt("warn_dismiss_days", thresholds.dismissalWarningDays)
            .putLong("updatedAtMillis", System.currentTimeMillis())
            .apply()
        legacyPref(context).edit()
            .putInt("${sid}_gen_exempt_avg", thresholds.generalExemptMinAvg)
            .putInt("${sid}_gen_exempt_min_sub", thresholds.generalExemptMinSubject)
            .putInt("${sid}_ind_exempt_score", thresholds.individualExemptMinScore)
            .putInt("${sid}_decision_pts_limit", thresholds.decisionPointsLimit)
            .putInt("${sid}_warn_first_days", thresholds.firstWarningDays)
            .putInt("${sid}_warn_final_days", thresholds.finalWarningDays)
            .putInt("${sid}_warn_dismiss_days", thresholds.dismissalWarningDays)
            .apply()
        pushSchoolSuiteToFirebase(context, sid)
    }

    fun isSmartIdExamPassEnabled(context: Context, schoolId: String): Boolean {
        val sid = cleanId(schoolId)
        val sp = schoolPref(context, sid)
        if (sp.contains("smart_id_exam_pass_enabled")) {
            return sp.getBoolean("smart_id_exam_pass_enabled", true)
        }
        return legacyPref(context).getBoolean("${sid}_smart_id_exam_pass_enabled", true)
    }

    fun setSmartIdExamPassEnabled(context: Context, schoolId: String, enabled: Boolean) {
        val sid = cleanId(schoolId)
        schoolPref(context, sid).edit()
            .putBoolean("smart_id_exam_pass_enabled", enabled)
            .putLong("updatedAtMillis", System.currentTimeMillis())
            .apply()
        legacyPref(context).edit()
            .putBoolean("${sid}_smart_id_exam_pass_enabled", enabled)
            .apply()
        pushSchoolSuiteToFirebase(context, sid)
    }

    fun isStudentExamAllowed(context: Context, schoolId: String, studentId: String): Boolean {
        val sid = cleanId(schoolId)
        val sp = schoolPref(context, sid)
        if (sp.contains("exam_allowed_$studentId")) {
            return sp.getBoolean("exam_allowed_$studentId", true)
        }
        return legacyPref(context).getBoolean("${sid}_exam_allowed_$studentId", true)
    }

    fun setStudentExamAllowed(context: Context, schoolId: String, studentId: String, allowed: Boolean) {
        val sid = cleanId(schoolId)
        schoolPref(context, sid).edit()
            .putBoolean("exam_allowed_$studentId", allowed)
            .putLong("updatedAtMillis", System.currentTimeMillis())
            .apply()
        legacyPref(context).edit()
            .putBoolean("${sid}_exam_allowed_$studentId", allowed)
            .apply()
        pushSchoolSuiteToFirebase(context, sid)
    }

    fun saveStudentSeatLabel(
        context: Context,
        schoolId: String,
        studentId: String,
        seatLabel: String,
        hallNumber: Int = 0,
        seatNumber: Int = 0
    ) {
        val sid = cleanId(schoolId)
        val spEdit = schoolPref(context, sid).edit()
            .putString("exam_seat_$studentId", seatLabel)
            .putLong("updatedAtMillis", System.currentTimeMillis())
        val lpEdit = legacyPref(context).edit()
            .putString("${sid}_exam_seat_$studentId", seatLabel)
        if (hallNumber > 0) {
            spEdit.putInt("exam_hall_num_$studentId", hallNumber)
            lpEdit.putInt("${sid}_exam_hall_num_$studentId", hallNumber)
        }
        if (seatNumber > 0) {
            spEdit.putInt("exam_seat_num_$studentId", seatNumber)
            lpEdit.putInt("${sid}_exam_seat_num_$studentId", seatNumber)
        }
        spEdit.apply()
        lpEdit.apply()
        pushSchoolSuiteToFirebase(context, sid)
    }

    fun getStudentExamHallAndSeatNumbers(
        context: Context,
        schoolId: String,
        studentId: String,
        defaultHall: Int = 1,
        defaultSeat: Int = 1
    ): Pair<Int, Int> {
        val sid = cleanId(schoolId)
        val sp = schoolPref(context, sid)
        val lp = legacyPref(context)
        val savedHall = if (sp.contains("exam_hall_num_$studentId")) {
            sp.getInt("exam_hall_num_$studentId", 0)
        } else {
            lp.getInt("${sid}_exam_hall_num_$studentId", 0)
        }
        val savedSeat = if (sp.contains("exam_seat_num_$studentId")) {
            sp.getInt("exam_seat_num_$studentId", 0)
        } else {
            lp.getInt("${sid}_exam_seat_num_$studentId", 0)
        }
        if (savedHall > 0 && savedSeat > 0) return savedHall to savedSeat

        // Parse from saved label if present
        val savedLabel = sp.getString("exam_seat_$studentId", null)
            ?: lp.getString("${sid}_exam_seat_$studentId", null)
        if (!savedLabel.isNullOrBlank()) {
            val nums = Regex("\\d+").findAll(savedLabel).mapNotNull { it.value.toIntOrNull() }.toList()
            if (nums.size >= 2) return nums[0] to nums[1]
        }

        val absHash = studentId.hashCode().let { if (it < 0) -it else it }
        val fallbackHall = if (defaultHall > 0) ((absHash % 3) + 1) else defaultHall.coerceAtLeast(1)
        val fallbackSeat = if (defaultSeat > 0) ((absHash % 20) + 1) else defaultSeat.coerceAtLeast(1)
        return fallbackHall to fallbackSeat
    }

    fun getStudentExamSeatLabel(context: Context, schoolId: String, studentId: String, gradeClass: String = ""): String {
        val (hallNum, seatNum) = getStudentExamHallAndSeatNumbers(context, schoolId, studentId)
        return "قاعة ($hallNum) - مقعد ($seatNum)"
    }

    fun loadCorrespondence(context: Context, schoolId: String): List<OfficialCorrespondenceRecord> {
        val raw = getStringWithFallback(context, schoolId, "correspondence")
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id !in LEGACY_FAKE_COR_IDS) {
                        add(
                            OfficialCorrespondenceRecord(
                                id = id,
                                type = o.optString("type", "صادر"),
                                bookNumber = o.optString("bookNumber"),
                                dateIso = o.optString("dateIso"),
                                entityName = o.optString("entityName"),
                                subject = o.optString("subject"),
                                notes = o.optString("notes"),
                                imageBase64 = o.optString("imageBase64", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveCorrespondence(context: Context, schoolId: String, list: List<OfficialCorrespondenceRecord>) {
        val arr = JSONArray()
        list.filter { it.id !in LEGACY_FAKE_COR_IDS }.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("bookNumber", item.bookNumber)
                    put("dateIso", item.dateIso)
                    put("entityName", item.entityName)
                    put("subject", item.subject)
                    put("notes", item.notes)
                    put("imageBase64", item.imageBase64)
                }
            )
        }
        putStringAndSync(context, schoolId, "correspondence", arr.toString())
    }

    fun loadStaffPortfolios(context: Context, schoolId: String): Map<String, StaffPortfolioRecord> {
        val raw = getStringWithFallback(context, schoolId, "staff_portfolios") ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val tid = o.optString("teacherId")
                    if (tid.isNotBlank()) {
                        val qual = o.optString("qualification", "").let {
                            if (it == "بكالوريوس تربية" && !o.optBoolean("userEdited", false)) "" else it
                        }
                        val startIso = o.optString("startDateIso", "").let {
                            if (it == "2016-09-01" && !o.optBoolean("userEdited", false)) "" else it
                        }
                        val jTitle = o.optString("jobTitle", "").let {
                            if (it == "معلم جامعي" && !o.optBoolean("userEdited", false)) "" else it
                        }
                        val jGrade = o.optString("jobGradeStage", "").let {
                            if (it == "الدرجة الخامسة - المرحلة الثانية" && !o.optBoolean("userEdited", false)) "" else it
                        }
                        val ordNum = o.optString("adminOrderNumber", "").let {
                            if (it == "إداري / 104" && !o.optBoolean("userEdited", false)) "" else it
                        }
                        put(
                            tid,
                            StaffPortfolioRecord(
                                teacherId = tid,
                                qualification = qual,
                                startDateIso = startIso,
                                jobTitle = jTitle,
                                jobGradeStage = jGrade,
                                adminOrderNumber = ordNum,
                                assignedClasses = o.optString("assignedClasses", ""),
                                specialty = o.optString("specialty", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveStaffPortfolios(context: Context, schoolId: String, map: Map<String, StaffPortfolioRecord>) {
        val arr = JSONArray()
        map.values.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("teacherId", item.teacherId)
                    put("qualification", item.qualification)
                    put("startDateIso", item.startDateIso)
                    put("jobTitle", item.jobTitle)
                    put("jobGradeStage", item.jobGradeStage)
                    put("adminOrderNumber", item.adminOrderNumber)
                    put("assignedClasses", item.assignedClasses)
                    put("specialty", item.specialty)
                    put("userEdited", true)
                }
            )
        }
        putStringAndSync(context, schoolId, "staff_portfolios", arr.toString())
    }

    fun loadStudentWelfare(context: Context, schoolId: String): Map<String, StudentWelfareRecord> {
        val raw = getStringWithFallback(context, schoolId, "student_welfare") ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val sid = o.optString("studentId")
                    if (sid.isNotBlank()) {
                        put(
                            sid,
                            StudentWelfareRecord(
                                studentId = sid,
                                orphanStatus = o.optString("orphanStatus", "غير يتيم"),
                                isSocialSecurityGrant = o.optBoolean("isSocialSecurityGrant", false),
                                isSpecialNeedsOrLowIncome = o.optBoolean("isSpecialNeedsOrLowIncome", false),
                                socialSecurityNumber = o.optString("socialSecurityNumber", ""),
                                monthlyGrantIqd = o.optLong("monthlyGrantIqd", 30000L),
                                welfareNotes = o.optString("welfareNotes", "")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveStudentWelfare(context: Context, schoolId: String, map: Map<String, StudentWelfareRecord>) {
        val arr = JSONArray()
        map.values.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("studentId", item.studentId)
                    put("orphanStatus", item.orphanStatus)
                    put("isSocialSecurityGrant", item.isSocialSecurityGrant)
                    put("isSpecialNeedsOrLowIncome", item.isSpecialNeedsOrLowIncome)
                    put("socialSecurityNumber", item.socialSecurityNumber)
                    put("monthlyGrantIqd", item.monthlyGrantIqd)
                    put("welfareNotes", item.welfareNotes)
                }
            )
        }
        putStringAndSync(context, schoolId, "student_welfare", arr.toString())
    }

    fun loadArchivedYears(context: Context, schoolId: String): List<ArchivedSchoolYearSnapshot> {
        val raw = getStringWithFallback(context, schoolId, "archived_years") ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val gArr = o.optJSONArray("graduatesNames") ?: JSONArray()
                    val grads = buildList {
                        for (j in 0 until gArr.length()) add(gArr.optString(j))
                    }
                    add(
                        ArchivedSchoolYearSnapshot(
                            id = o.optString("id"),
                            academicYear = o.optString("academicYear"),
                            sealedDateIso = o.optString("sealedDateIso"),
                            sealedByPrincipal = o.optString("sealedByPrincipal"),
                            totalStudents = o.optInt("totalStudents"),
                            promotedCount = o.optInt("promotedCount"),
                            repeatingCount = o.optInt("repeatingCount"),
                            graduatedCount = o.optInt("graduatedCount"),
                            graduatesNames = grads,
                            summaryNotes = o.optString("summaryNotes")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveArchivedYears(context: Context, schoolId: String, list: List<ArchivedSchoolYearSnapshot>) {
        val arr = JSONArray()
        list.forEach { item ->
            val gArr = JSONArray()
            item.graduatesNames.forEach { gArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("academicYear", item.academicYear)
                    put("sealedDateIso", item.sealedDateIso)
                    put("sealedByPrincipal", item.sealedByPrincipal)
                    put("totalStudents", item.totalStudents)
                    put("promotedCount", item.promotedCount)
                    put("repeatingCount", item.repeatingCount)
                    put("graduatedCount", item.graduatedCount)
                    put("graduatesNames", gArr)
                    put("summaryNotes", item.summaryNotes)
                }
            )
        }
        putStringAndSync(context, schoolId, "archived_years", arr.toString())
    }

    fun loadInventory(context: Context, schoolId: String): List<SchoolInventoryItemRecord> {
        val raw = getStringWithFallback(context, schoolId, "inventory")
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id !in LEGACY_FAKE_INV_IDS) {
                        add(
                            SchoolInventoryItemRecord(
                                id = id,
                                itemName = o.optString("itemName"),
                                category = o.optString("category"),
                                quantity = o.optInt("quantity", 1),
                                conditionStatus = o.optString("conditionStatus", "صالح"),
                                locationRoom = o.optString("locationRoom"),
                                notes = o.optString("notes")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveInventory(context: Context, schoolId: String, list: List<SchoolInventoryItemRecord>) {
        val arr = JSONArray()
        list.filter { it.id !in LEGACY_FAKE_INV_IDS }.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("itemName", item.itemName)
                    put("category", item.category)
                    put("quantity", item.quantity)
                    put("conditionStatus", item.conditionStatus)
                    put("locationRoom", item.locationRoom)
                    put("notes", item.notes)
                }
            )
        }
        putStringAndSync(context, schoolId, "inventory", arr.toString())
    }

    fun loadExpenses(context: Context, schoolId: String): List<SchoolExpenseItemRecord> {
        val raw = getStringWithFallback(context, schoolId, "expenses")
        if (raw.isNullOrBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val id = o.optString("id")
                    if (id !in LEGACY_FAKE_EXP_IDS) {
                        add(
                            SchoolExpenseItemRecord(
                                id = id,
                                amountIqd = o.optLong("amountIqd", 0L),
                                category = o.optString("category"),
                                dateIso = o.optString("dateIso"),
                                recipientName = o.optString("recipientName"),
                                receiptNumber = o.optString("receiptNumber"),
                                notes = o.optString("notes")
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveExpenses(context: Context, schoolId: String, list: List<SchoolExpenseItemRecord>) {
        val arr = JSONArray()
        list.filter { it.id !in LEGACY_FAKE_EXP_IDS }.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("amountIqd", item.amountIqd)
                    put("category", item.category)
                    put("dateIso", item.dateIso)
                    put("recipientName", item.recipientName)
                    put("receiptNumber", item.receiptNumber)
                    put("notes", item.notes)
                }
            )
        }
        putStringAndSync(context, schoolId, "expenses", arr.toString())
    }

    fun loadExamQuestions(context: Context, schoolId: String): List<ExamQuestionItemDraft> {
        val raw = getStringWithFallback(context, schoolId, "exam_questions")
        if (raw.isNullOrBlank()) {
            return listOf(
                ExamQuestionItemDraft(questionNumber = 1, mainPrompt = "", marks = 20, branches = emptyList()),
                ExamQuestionItemDraft(questionNumber = 2, mainPrompt = "", marks = 20, branches = emptyList())
            )
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val bArr = o.optJSONArray("branches")
                    val branchList = buildList {
                        if (bArr != null) {
                            for (j in 0 until bArr.length()) {
                                add(bArr.optString(j, ""))
                            }
                        } else {
                            val bA = o.optString("branchA", "")
                            val bB = o.optString("branchB", "")
                            if (bA.isNotBlank()) add(bA)
                            if (bB.isNotBlank()) add(bB)
                        }
                    }
                    add(
                        ExamQuestionItemDraft(
                            questionNumber = i + 1,
                            mainPrompt = o.optString("mainPrompt", ""),
                            branchA = branchList.getOrNull(0) ?: "",
                            branchB = branchList.getOrNull(1) ?: "",
                            marks = o.optInt("marks", 20).coerceIn(1, 100),
                            branches = branchList
                        )
                    )
                }
            }.ifEmpty {
                listOf(
                    ExamQuestionItemDraft(questionNumber = 1, mainPrompt = "", marks = 20, branches = emptyList())
                )
            }
        }.getOrDefault(
            listOf(
                ExamQuestionItemDraft(questionNumber = 1, mainPrompt = "", marks = 20, branches = emptyList())
            )
        )
    }

    fun saveExamQuestions(context: Context, schoolId: String, questions: List<ExamQuestionItemDraft>) {
        val arr = JSONArray()
        questions.forEachIndexed { idx, q ->
            val bArr = JSONArray()
            q.branches.forEach { bArr.put(it) }
            arr.put(
                JSONObject().apply {
                    put("questionNumber", idx + 1)
                    put("mainPrompt", q.mainPrompt)
                    put("branchA", q.branches.getOrNull(0) ?: q.branchA)
                    put("branchB", q.branches.getOrNull(1) ?: q.branchB)
                    put("marks", q.marks)
                    put("branches", bArr)
                }
            )
        }
        putStringAndSync(context, schoolId, "exam_questions", arr.toString())
    }
}

internal object MinistrySuiteLocalStore {
    fun loadThresholds(context: Context, schoolId: String): ExemptionAndWarningThresholds =
        MinistryDataPersistenceManager.loadThresholds(context, schoolId)

    fun saveThresholds(context: Context, schoolId: String, thresholds: ExemptionAndWarningThresholds) =
        MinistryDataPersistenceManager.saveThresholds(context, schoolId, thresholds)

    fun isSmartIdExamPassEnabled(context: Context, schoolId: String): Boolean =
        MinistryDataPersistenceManager.isSmartIdExamPassEnabled(context, schoolId)

    fun setSmartIdExamPassEnabled(context: Context, schoolId: String, enabled: Boolean) =
        MinistryDataPersistenceManager.setSmartIdExamPassEnabled(context, schoolId, enabled)

    fun isStudentExamAllowed(context: Context, schoolId: String, studentId: String): Boolean =
        MinistryDataPersistenceManager.isStudentExamAllowed(context, schoolId, studentId)

    fun setStudentExamAllowed(context: Context, schoolId: String, studentId: String, allowed: Boolean) =
        MinistryDataPersistenceManager.setStudentExamAllowed(context, schoolId, studentId, allowed)

    fun saveStudentSeatLabel(
        context: Context,
        schoolId: String,
        studentId: String,
        seatLabel: String,
        hallNumber: Int = 0,
        seatNumber: Int = 0
    ) = MinistryDataPersistenceManager.saveStudentSeatLabel(context, schoolId, studentId, seatLabel, hallNumber, seatNumber)

    fun getStudentExamHallAndSeatNumbers(context: Context, schoolId: String, studentId: String): Pair<Int, Int> =
        MinistryDataPersistenceManager.getStudentExamHallAndSeatNumbers(context, schoolId, studentId)

    fun getStudentExamSeatLabel(context: Context, schoolId: String, studentId: String, gradeClass: String = ""): String =
        MinistryDataPersistenceManager.getStudentExamSeatLabel(context, schoolId, studentId, gradeClass)

    fun loadCorrespondence(context: Context, schoolId: String): List<OfficialCorrespondenceRecord> =
        MinistryDataPersistenceManager.loadCorrespondence(context, schoolId)

    fun saveCorrespondence(context: Context, schoolId: String, list: List<OfficialCorrespondenceRecord>) =
        MinistryDataPersistenceManager.saveCorrespondence(context, schoolId, list)

    fun loadStaffPortfolios(context: Context, schoolId: String): Map<String, StaffPortfolioRecord> =
        MinistryDataPersistenceManager.loadStaffPortfolios(context, schoolId)

    fun saveStaffPortfolios(context: Context, schoolId: String, map: Map<String, StaffPortfolioRecord>) =
        MinistryDataPersistenceManager.saveStaffPortfolios(context, schoolId, map)

    fun loadStudentWelfare(context: Context, schoolId: String): Map<String, StudentWelfareRecord> =
        MinistryDataPersistenceManager.loadStudentWelfare(context, schoolId)

    fun saveStudentWelfare(context: Context, schoolId: String, map: Map<String, StudentWelfareRecord>) =
        MinistryDataPersistenceManager.saveStudentWelfare(context, schoolId, map)

    fun loadArchivedYears(context: Context, schoolId: String): List<ArchivedSchoolYearSnapshot> =
        MinistryDataPersistenceManager.loadArchivedYears(context, schoolId)

    fun saveArchivedYears(context: Context, schoolId: String, list: List<ArchivedSchoolYearSnapshot>) =
        MinistryDataPersistenceManager.saveArchivedYears(context, schoolId, list)

    fun loadInventory(context: Context, schoolId: String): List<SchoolInventoryItemRecord> =
        MinistryDataPersistenceManager.loadInventory(context, schoolId)

    fun saveInventory(context: Context, schoolId: String, list: List<SchoolInventoryItemRecord>) =
        MinistryDataPersistenceManager.saveInventory(context, schoolId, list)

    fun loadExpenses(context: Context, schoolId: String): List<SchoolExpenseItemRecord> =
        MinistryDataPersistenceManager.loadExpenses(context, schoolId)

    fun saveExpenses(context: Context, schoolId: String, list: List<SchoolExpenseItemRecord>) =
        MinistryDataPersistenceManager.saveExpenses(context, schoolId, list)
}

// =========================================================================
// 2. OFFICIAL IRAQI A4 PAPER DOCUMENT PDF & EXCEL CSV EXPORT ENGINE
// =========================================================================

private fun decodeBase64ToBitmapSafe(base64Str: String): Bitmap? {
    if (base64Str.isBlank()) return null
    return runCatching {
        val clean = base64Str.substringAfter(",")
        val bytes = Base64.decode(clean, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()
}

private fun exportMinistryOfficialTablePdf(
    context: Context,
    schoolName: String,
    principalName: String,
    academicYear: String,
    documentTitle: String,
    subtitleInfo: String,
    summaryPills: List<Pair<String, String>>,
    headers: List<String>,
    rows: List<List<String>>,
    footerNote: String = "تم إصدار هذه الوثيقة رسمياً من مركز الإدارة المدرسية واللجنة الامتحانية",
    officialLetterBody: String = "",
    bookNumberLabel: String = "",
    attachedImageBase64: String = "",
    schoolId: String = "default_school",
    autoArchiveToCorrespondence: Boolean = true,
    onArchivedRecordCreated: ((List<OfficialCorrespondenceRecord>) -> Unit)? = null,
    onResult: (Boolean, String) -> Unit
) {
    try {
        val resolvedBookNumber = if (bookNumberLabel.isBlank() || bookNumberLabel == "AUTO") {
            MinistryDataPersistenceManager.nextAndIncrementDocNumber(context, schoolId)
        } else {
            bookNumberLabel
        }

        if (autoArchiveToCorrespondence) {
            val updatedCor = MinistryDataPersistenceManager.archiveOfficialDocumentAutomatically(
                context = context,
                schoolId = schoolId,
                bookNumber = resolvedBookNumber,
                docType = if (documentTitle.contains("أمر إداري")) "أمر إداري" else "صادر",
                entityName = subtitleInfo.takeIf { it.isNotBlank() }?.take(45) ?: "إدارة المدرسة / أولياء الأمور",
                subject = documentTitle,
                notes = footerNote,
                imageBase64 = attachedImageBase64
            )
            onArchivedRecordCreated?.invoke(updatedCor)
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val whiteBgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val headerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val headerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val centerBasmalaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#042F24")
            textSize = 13f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val subjectPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 13f
            isUnderlineText = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val bodyBoldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val tableHeaderFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 1.1f
        }

        // 1. Crisp White A4 Paper & Double Official Border
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whiteBgPaint)
        canvas.drawRect(18f, 18f, pageWidth - 18f, pageHeight - 18f, outerBorderPaint)
        canvas.drawRect(22f, 22f, pageWidth - 22f, pageHeight - 22f, innerBorderPaint)

        val rightMargin = pageWidth - 36f
        val leftMargin = 36f
        val centerX = pageWidth / 2f
        val totalWidth = rightMargin - leftMargin
        val todayIso = ArabicCalendarDateHelper.todayIsoDate()

        // 2. Tri-Part Official Iraqi Header (Right / Center / Left)
        canvas.drawText("جمهورية العراق", rightMargin, 48f, headerRightPaint)
        canvas.drawText("وزارة التربية", rightMargin, 65f, headerRightPaint)
        canvas.drawText("إدارة مدرسة ($schoolName)", rightMargin, 82f, headerRightPaint)
        canvas.drawText("مركز الإدارة المدرسية واللجنة الامتحانية", rightMargin, 98f, Paint(headerRightPaint).apply {
            textSize = 9f
            color = android.graphics.Color.parseColor("#065F46")
        })

        // Center: ONLY "بسم الله الرحمن الرحيم" in elegant official font (No circle or crossed lines underneath)
        canvas.drawText("بسم الله الرحمن الرحيم", centerX, 58f, centerBasmalaPaint)

        // Left: Sequential Number (العدد), Date, Academic Year
        canvas.drawText("العدد: $resolvedBookNumber", leftMargin, 48f, headerLeftPaint)
        canvas.drawText("التاريخ: $todayIso", leftMargin, 65f, headerLeftPaint)
        canvas.drawText("العام الدراسي: $academicYear", leftMargin, 82f, headerLeftPaint)
        canvas.drawText("المرفقات: وثيقة رسمية مصدقة", leftMargin, 98f, Paint(headerLeftPaint).apply {
            textSize = 9f
            color = android.graphics.Color.parseColor("#334155")
        })

        // Separator Line under Tri-Part Header
        canvas.drawLine(leftMargin, 112f, rightMargin, 112f, outerBorderPaint)
        canvas.drawLine(leftMargin, 115f, rightMargin, 115f, innerBorderPaint)

        // 3. Centered Bold Underlined Subject Title (م / ...)
        var currentY = 138f
        val formattedSubject = if (documentTitle.startsWith("م /") || documentTitle.startsWith("م/")) {
            documentTitle
        } else {
            "م / $documentTitle"
        }
        canvas.drawText(formattedSubject, centerX, currentY, subjectPaint)
        currentY += 22f

        if (subtitleInfo.isNotBlank()) {
            canvas.drawText(subtitleInfo, rightMargin, currentY, bodyBoldPaint)
            currentY += 18f
        }

        // 4. Formal Salutation & Letter Body Paragraph
        val effectiveLetterBody = officialLetterBody.ifBlank {
            "تحية طيبة.. استناداً إلى الضوابط والتعليمات المدرسية والامتحانية النافذة للعام الدراسي ($academicYear)، ندرج لكم أدناه التفاصيل والبيانات الرسمية المعتمدة في سجلات مدرسة ($schoolName) لاتخاذ ما يلزم أصولياً:"
        }
        val words = effectiveLetterBody.split(" ")
        var lineBuffer = StringBuilder()
        for (word in words) {
            val testLine = if (lineBuffer.isEmpty()) word else "$lineBuffer $word"
            if (bodyPaint.measureText(testLine) > totalWidth - 12f) {
                canvas.drawText(lineBuffer.toString(), rightMargin, currentY, bodyPaint)
                currentY += 15f
                lineBuffer = StringBuilder(word)
            } else {
                lineBuffer = StringBuilder(testLine)
            }
        }
        if (lineBuffer.isNotEmpty()) {
            canvas.drawText(lineBuffer.toString(), rightMargin, currentY, bodyPaint)
            currentY += 18f
        }

        // 5. Summary Boxes (Official Paper Style)
        if (summaryPills.isNotEmpty()) {
            val count = summaryPills.size.coerceAtMost(4)
            val gap = 8f
            val boxW = (totalWidth - gap * (count - 1)) / count
            summaryPills.take(4).forEachIndexed { idx, (k, v) ->
                val boxLeft = rightMargin - ((idx + 1) * boxW) - (idx * gap)
                val boxRight = boxLeft + boxW
                canvas.drawRect(boxLeft, currentY, boxRight, currentY + 36f, tableHeaderFillPaint)
                canvas.drawRect(boxLeft, currentY, boxRight, currentY + 36f, tableBorderPaint)
                val cx = (boxLeft + boxRight) / 2f
                canvas.drawText(k, cx, currentY + 14f, Paint(bodyPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 8.5f
                })
                canvas.drawText(v, cx, currentY + 29f, Paint(bodyBoldPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 9.5f
                })
            }
            currentY += 48f
        }

        // 6. Crisp Black-Bordered Official Table
        if (headers.isNotEmpty()) {
            val colCount = headers.size
            val colW = totalWidth / colCount
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 24f, tableHeaderFillPaint)
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 24f, tableBorderPaint)
            val thPaint = Paint(bodyBoldPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 9f
            }
            headers.forEachIndexed { cIdx, hText ->
                val colRight = rightMargin - (cIdx * colW)
                val colLeft = colRight - colW
                if (cIdx > 0) {
                    canvas.drawLine(colRight, currentY, colRight, currentY + 24f, tableBorderPaint)
                }
                val colCenter = (colRight + colLeft) / 2f
                canvas.drawText(hText.take(24), colCenter, currentY + 16f, thPaint)
            }
            currentY += 24f

            val tdPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 8.5f
            }
            rows.take(19).forEachIndexed { rIdx, row ->
                if (currentY + 21f < pageHeight - 135f) {
                    if (rIdx % 2 == 1) {
                        canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 21f, Paint().apply {
                            color = android.graphics.Color.parseColor("#F8FAFC")
                            style = Paint.Style.FILL
                        })
                    }
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 21f, tableBorderPaint)
                    row.forEachIndexed { cIdx, cell ->
                        if (cIdx < colCount) {
                            val colRight = rightMargin - (cIdx * colW)
                            val colLeft = colRight - colW
                            if (cIdx > 0) {
                                canvas.drawLine(colRight, currentY, colRight, currentY + 21f, tableBorderPaint)
                            }
                            val colCenter = (colRight + colLeft) / 2f
                            canvas.drawText(cell.take(28), colCenter, currentY + 14f, tdPaint)
                        }
                    }
                    currentY += 21f
                }
            }
        }

        // 7. Official Iraqi Document Footer (Principal Signature & Real Official School Stamp on Left, Real Scannable QR on Center-Right)
        val footerTopY = pageHeight - 122f
        canvas.drawLine(leftMargin, footerTopY, rightMargin, footerTopY, innerBorderPaint)

        // Right side of footer: Exam Committee / Administration
        canvas.drawText("اللجنة الامتحانية / الإدارة المدرسية", rightMargin, footerTopY + 20f, bodyBoldPaint)
        canvas.drawText(footerNote.take(64), rightMargin, footerTopY + 36f, Paint(bodyPaint).apply { textSize = 8.5f })
        canvas.drawText("العدد الصادر: ($resolvedBookNumber) • مؤرشف في سجل الصادرة والواردة", rightMargin, footerTopY + 52f, Paint(bodyPaint).apply { textSize = 8.5f })

        // Real 100% Scannable QR Code Bitmap in Footer Center
        val qrSize = 56f
        val qrLeft = centerX - (qrSize / 2f)
        val qrTop = footerTopY + 10f
        val qrPayload = "DOC:${schoolId.ifBlank { schoolName }}:$resolvedBookNumber:$todayIso"
        val realQrBmp = com.example.util.DocumentAndIdExporter.generateQrCodeBitmap(qrPayload, 160)
        val qrRect = RectF(qrLeft, qrTop, qrLeft + qrSize, qrTop + qrSize)
        canvas.drawBitmap(realQrBmp, null, qrRect, null)
        canvas.drawRect(qrRect, tableBorderPaint)
        canvas.drawText("باركود التحقق الرسمي", centerX, qrTop + qrSize + 11f, Paint(bodyBoldPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 7.5f
        })

        // Left side of footer: Principal Signature & Large Double-Bordered Official School Stamp
        val leftSigCenterX = leftMargin + 105f
        canvas.drawText("مدير المدرسة: $principalName", leftSigCenterX, footerTopY + 16f, Paint(bodyBoldPaint).apply {
            textAlign = Paint.Align.CENTER
            textSize = 9.5f
        })

        val stampCenterY = footerTopY + 56f
        val stampRx = 72f
        val stampRy = 30f
        val stampOuterRect = RectF(leftSigCenterX - stampRx, stampCenterY - stampRy, leftSigCenterX + stampRx, stampCenterY + stampRy)
        val stampInnerRect = RectF(leftSigCenterX - stampRx + 3.5f, stampCenterY - stampRy + 3.5f, leftSigCenterX + stampRx - 3.5f, stampCenterY + stampRy - 3.5f)
        val stampPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E3A8A")
            style = Paint.Style.STROKE
            strokeWidth = 1.8f
        }
        val stampInnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E3A8A")
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        canvas.drawRoundRect(stampOuterRect, 28f, 28f, stampPaint)
        canvas.drawRoundRect(stampInnerRect, 24f, 24f, stampInnerPaint)

        val stampTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E3A8A")
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 8f
        }
        canvas.drawText("إدارة مدرسة ${schoolName.take(22)}", leftSigCenterX, stampCenterY - 10f, stampTextPaint)
        canvas.drawText("التاريخ: $todayIso", leftSigCenterX, stampCenterY + 3f, Paint(stampTextPaint).apply {
            textSize = 8.2f
            color = android.graphics.Color.parseColor("#065F46")
        })
        canvas.drawText("الختم الإداري المعتمد", leftSigCenterX, stampCenterY + 16f, stampTextPaint)

        pdfDocument.finishPage(page)

        // Optional Page 2: Attached Official Scanned Document Image
        val attachedBitmap = decodeBase64ToBitmapSafe(attachedImageBase64)
        var totalPageCount = 1
        if (attachedBitmap != null) {
            totalPageCount = 2
            val page2Info = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 2).create()
            val page2 = pdfDocument.startPage(page2Info)
            val canvas2 = page2.canvas
            canvas2.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whiteBgPaint)
            canvas2.drawRect(18f, 18f, pageWidth - 18f, pageHeight - 18f, outerBorderPaint)
            canvas2.drawRect(22f, 22f, pageWidth - 22f, pageHeight - 22f, innerBorderPaint)
            canvas2.drawText("المرفق الضوئي الرسمي للكتاب ($resolvedBookNumber) — $documentTitle", centerX, 52f, subjectPaint)
            val targetRect = RectF(44f, 75f, pageWidth - 44f, pageHeight - 85f)
            canvas2.drawBitmap(attachedBitmap, null, targetRect, null)
            canvas2.drawRect(targetRect, tableBorderPaint)
            canvas2.drawText("مصدق طبق الأصل — مدير المدرسة: $principalName", centerX, pageHeight - 45f, Paint(bodyBoldPaint).apply {
                textAlign = Paint.Align.CENTER
            })
            pdfDocument.finishPage(page2)
        }

        val safeName = "School_Official_${System.currentTimeMillis()}.pdf"
        val cacheFile = File(context.cacheDir, safeName)
        FileOutputStream(cacheFile).use { out -> pdfDocument.writeTo(out) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(cacheFile).use { inStream -> inStream.copyTo(outStream) }
                    }
                }
            }
        }
        pdfDocument.close()

        runCatching {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            printManager?.print(
                "$documentTitle - $schoolName",
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(safeName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(totalPageCount)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            if (destination != null) {
                                FileInputStream(cacheFile).use { input ->
                                    FileOutputStream(destination.fileDescriptor).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                },
                null
            )
        }

        onResult(true, "تم تصدير الوثيقة الرسمية ($documentTitle) بالعدد ($resolvedBookNumber) وأرشفتها تلقائياً ✓")
    } catch (e: Exception) {
        onResult(false, "تعذر تصدير PDF: ${e.localizedMessage ?: "خطأ غير متوقع"}")
    }
}

/**
 * تصدير ورقة الأسئلة الامتحانية العراقية الرسمية (تخطيط عمودي حر بدون جداول وبدون نص كتاب إداري)
 */
private fun exportOfficialExamQuestionsPaperPdf(
    context: Context,
    schoolId: String,
    schoolName: String,
    principalName: String,
    subjectTeacherName: String,
    academicYear: String,
    examSubject: String,
    examGradeClass: String,
    examRound: String,
    examDuration: String,
    examDate: String,
    examInstructionNote: String,
    questions: List<ExamQuestionItemDraft>,
    onResult: (Boolean, String) -> Unit
) {
    try {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val whiteBgPaint = Paint().apply {
            color = android.graphics.Color.WHITE
            style = Paint.Style.FILL
        }
        val outerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            style = Paint.Style.STROKE
            strokeWidth = 2.2f
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#475569")
            style = Paint.Style.STROKE
            strokeWidth = 0.9f
        }
        val headerRightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val headerLeftPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.LEFT
        }
        val headerCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#042F24")
            textSize = 12.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val questionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#0F172A")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val branchPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E293B")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }

        // 1. White A4 Page & Double Frame
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), whiteBgPaint)
        canvas.drawRect(18f, 18f, pageWidth - 18f, pageHeight - 18f, outerBorderPaint)
        canvas.drawRect(22f, 22f, pageWidth - 22f, pageHeight - 22f, innerBorderPaint)

        val rightMargin = pageWidth - 36f
        val leftMargin = 36f
        val centerX = pageWidth / 2f
        val totalWidth = rightMargin - leftMargin
        val totalMarks = questions.sumOf { it.marks }.let { if (it > 0) it else 100 }

        // 2. Top Framed Iraqi Exam Header Box (Right / Center / Left)
        val headerBoxRect = RectF(leftMargin, 32f, rightMargin, 114f)
        canvas.drawRoundRect(headerBoxRect, 8f, 8f, Paint().apply {
            color = android.graphics.Color.parseColor("#F8FAFC")
            style = Paint.Style.FILL
        })
        canvas.drawRoundRect(headerBoxRect, 8f, 8f, outerBorderPaint)

        // Right Column of Header
        canvas.drawText("إدارة مدرسة $schoolName", rightMargin - 10f, 54f, headerRightPaint)
        canvas.drawText("المادة: $examSubject", rightMargin - 10f, 76f, headerRightPaint)
        canvas.drawText("الصف: $examGradeClass", rightMargin - 10f, 98f, headerRightPaint)

        // Center Column of Header
        canvas.drawText("بسم الله الرحمن الرحيم", centerX, 52f, headerCenterPaint)
        canvas.drawText(examRound, centerX, 75f, Paint(headerCenterPaint).apply { textSize = 11f })
        canvas.drawText("للعام الدراسي ($academicYear)", centerX, 97f, Paint(headerCenterPaint).apply {
            textSize = 10f
            color = android.graphics.Color.parseColor("#065F46")
        })

        // Left Column of Header
        canvas.drawText("التاريخ: $examDate", leftMargin + 10f, 54f, headerLeftPaint)
        canvas.drawText("الزمن: $examDuration", leftMargin + 10f, 76f, headerLeftPaint)
        canvas.drawText("الدرجة الكلية: ($totalMarks) درجة", leftMargin + 10f, 98f, headerLeftPaint)

        var currentY = 132f

        // 3. Exam Note Line under Header
        if (examInstructionNote.isNotBlank()) {
            val noteRect = RectF(leftMargin, currentY - 14f, rightMargin, currentY + 10f)
            canvas.drawRoundRect(noteRect, 6f, 6f, Paint().apply {
                color = android.graphics.Color.parseColor("#FFFBEB")
                style = Paint.Style.FILL
            })
            canvas.drawRoundRect(noteRect, 6f, 6f, innerBorderPaint)
            canvas.drawText(examInstructionNote, centerX, currentY + 2f, Paint(headerCenterPaint).apply {
                textSize = 10.5f
                color = android.graphics.Color.parseColor("#92400E")
            })
            currentY += 32f
        } else {
            currentY += 10f
        }

        // 4. Vertical Questions Layout (Full Width, Right-to-Left, Zero Tables)
        val arabicBranchLetters = listOf("أ", "ب", "ج", "د", "هـ", "و")
        val nonBlankQuestions = questions.filter {
            it.mainPrompt.isNotBlank() || it.effectiveBranches().any { b -> b.isNotBlank() }
        }.ifEmpty { questions }

        nonBlankQuestions.forEachIndexed { idx, q ->
            if (currentY < pageHeight - 135f) {
                val qNum = idx + 1
                val promptText = q.mainPrompt.ifBlank { "أجب عما يأتي:" }
                val fullQuestionLine = "س$qNum / $promptText   (${q.marks} درجة)"

                // Wrap main question line if long
                val words = fullQuestionLine.split(" ")
                var lineBuf = StringBuilder()
                for (w in words) {
                    val candidate = if (lineBuf.isEmpty()) w else "$lineBuf $w"
                    if (questionTitlePaint.measureText(candidate) > totalWidth - 16f) {
                        canvas.drawText(lineBuf.toString(), rightMargin, currentY, questionTitlePaint)
                        currentY += 18f
                        lineBuf = StringBuilder(w)
                    } else {
                        lineBuf = StringBuilder(candidate)
                    }
                }
                if (lineBuf.isNotEmpty()) {
                    canvas.drawText(lineBuf.toString(), rightMargin, currentY, questionTitlePaint)
                    currentY += 19f
                }

                // Branches indented underneath
                val branches = q.effectiveBranches().filter { it.isNotBlank() }
                branches.forEachIndexed { bIdx, bText ->
                    if (currentY < pageHeight - 130f) {
                        val bLetter = arabicBranchLetters.getOrElse(bIdx) { "${bIdx + 1}" }
                        val branchLine = if (bText.startsWith("فرع") || bText.startsWith("$bLetter)") || bText.startsWith("($bLetter)")) {
                            bText
                        } else {
                            "$bLetter) $bText"
                        }
                        val bWords = branchLine.split(" ")
                        var bBuf = StringBuilder()
                        for (bw in bWords) {
                            val cand = if (bBuf.isEmpty()) bw else "$bBuf $bw"
                            if (branchPaint.measureText(cand) > totalWidth - 36f) {
                                canvas.drawText(bBuf.toString(), rightMargin - 16f, currentY, branchPaint)
                                currentY += 16f
                                bBuf = StringBuilder(bw)
                            } else {
                                bBuf = StringBuilder(cand)
                            }
                        }
                        if (bBuf.isNotEmpty()) {
                            canvas.drawText(bBuf.toString(), rightMargin - 16f, currentY, branchPaint)
                            currentY += 17f
                        }
                    }
                }

                currentY += 6f
                if (idx < nonBlankQuestions.lastIndex && currentY < pageHeight - 140f) {
                    canvas.drawLine(leftMargin + 14f, currentY, rightMargin - 14f, currentY, dividerPaint)
                    currentY += 18f
                }
            }
        }

        // 5. Bottom Exam Paper Footer: Best Wishes + Subject Teacher & Principal
        val footerTopY = pageHeight - 102f
        canvas.drawLine(leftMargin, footerTopY, rightMargin, footerTopY, outerBorderPaint)
        canvas.drawText(
            "مع تمنياتنا لجميع طلبتنا الأعزاء بالنجاح والتوفيق",
            centerX,
            footerTopY + 24f,
            Paint(headerCenterPaint).apply { textSize = 11.5f }
        )
        canvas.drawText(
            "معلم / مدرس المادة: ${subjectTeacherName.ifBlank { "مدرّس المادة" }}",
            rightMargin - 10f,
            footerTopY + 52f,
            headerRightPaint
        )
        canvas.drawText(
            "مدير المدرسة: $principalName",
            leftMargin + 10f,
            footerTopY + 52f,
            headerLeftPaint
        )

        pdfDocument.finishPage(page)

        val safeName = "Exam_Questions_${System.currentTimeMillis()}.pdf"
        val cacheFile = File(context.cacheDir, safeName)
        FileOutputStream(cacheFile).use { out -> pdfDocument.writeTo(out) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(cacheFile).use { inStream -> inStream.copyTo(outStream) }
                    }
                }
            }
        }
        pdfDocument.close()

        runCatching {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            printManager?.print(
                "أسئلة امتحان $examSubject - $examGradeClass",
                object : PrintDocumentAdapter() {
                    override fun onLayout(
                        oldAttributes: PrintAttributes?,
                        newAttributes: PrintAttributes?,
                        cancellationSignal: CancellationSignal?,
                        callback: LayoutResultCallback?,
                        extras: Bundle?
                    ) {
                        if (cancellationSignal?.isCanceled == true) {
                            callback?.onLayoutCancelled()
                            return
                        }
                        val info = PrintDocumentInfo.Builder(safeName)
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .setPageCount(1)
                            .build()
                        callback?.onLayoutFinished(info, true)
                    }

                    override fun onWrite(
                        pages: Array<out PageRange>?,
                        destination: ParcelFileDescriptor?,
                        cancellationSignal: CancellationSignal?,
                        callback: WriteResultCallback?
                    ) {
                        try {
                            if (destination != null) {
                                FileInputStream(cacheFile).use { input ->
                                    FileOutputStream(destination.fileDescriptor).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                            callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                        } catch (e: Exception) {
                            callback?.onWriteFailed(e.message)
                        }
                    }
                },
                null
            )
        }

        onResult(true, "تم تصدير ورقة الأسئلة الامتحانية الرسمية لمادة ($examSubject) بالتنسيق العمودي المعتمد ✓")
    } catch (e: Exception) {
        onResult(false, "تعذر تصدير ورقة الأسئلة: ${e.localizedMessage ?: "خطأ غير متوقع"}")
    }
}

private fun exportMinistryOfficialCsv(
    context: Context,
    filePrefix: String,
    headers: List<String>,
    rows: List<List<String>>,
    onResult: (Boolean, String) -> Unit
) {
    try {
        val sb = StringBuilder()
        sb.append('\uFEFF')
        sb.appendLine(headers.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        rows.forEach { r ->
            sb.appendLine(r.joinToString(",") { "\"${it.replace("\"", "\"\"")}\"" })
        }
        val safeFileName = "${filePrefix}_${System.currentTimeMillis()}.csv"
        val cacheFile = File(context.cacheDir, safeFileName)
        cacheFile.writeText(sb.toString(), Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(sb.toString().toByteArray(Charsets.UTF_8))
                    }
                }
            }
        }
        onResult(true, "تم تصدير جدول Excel (CSV) بالترميز العربي المعتمد وحفظه في مجلد التنزيلات ($safeFileName)")
    } catch (e: Exception) {
        onResult(false, "تعذر تصدير ملف CSV: ${e.localizedMessage ?: "خطأ"}")
    }
}

// =========================================================================
// 3. MAIN COMPOSABLE: MinistryExecutiveSuiteScreen (FULL-SCREEN DIALOG)
// =========================================================================

@Composable
fun MinistryExecutiveSuiteScreen(
    viewModel: SchoolViewModel,
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val allGrades by viewModel.grades.collectAsState()
    val allAccounts by viewModel.accounts.collectAsState()
    val allTuitions by viewModel.tuitions.collectAsState()
    val examSchedules by viewModel.examSchedules.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val schoolId = schoolConfig.schoolId.ifBlank { "default_school" }
    val principalName = currentUser?.fullName?.ifBlank { schoolConfig.principalFullName } ?: schoolConfig.principalFullName

    val activeStudents = remember(allStudents, schoolId) {
        allStudents.filter { (it.schoolId.isEmpty() || it.schoolId == schoolId) && it.isActive }
    }
    val realTeachers = remember(allAccounts, schoolConfig) {
        allAccounts.filter {
            (it.schoolId.isEmpty() || it.schoolId == schoolId) &&
                it.role == UserRole.TEACHER &&
                !it.isDriverInSchoolConfig(schoolConfig)
        }
    }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 5)) }

    // Action Feedback Banner State (Package 8)
    var feedbackBannerMessage by remember { mutableStateOf<String?>(null) }
    var feedbackBannerIsSuccess by remember { mutableStateOf(true) }
    var feedbackBannerTrigger by remember { mutableIntStateOf(0) }

    val triggerActionBanner: (String, Boolean) -> Unit = { msg, isSuccess ->
        feedbackBannerMessage = msg
        feedbackBannerIsSuccess = isSuccess
        feedbackBannerTrigger++
        viewModel.showToastAlert(msg, isSuccess = isSuccess)
    }

    LaunchedEffect(feedbackBannerTrigger) {
        if (feedbackBannerMessage != null) {
            delay(4200L)
            feedbackBannerMessage = null
        }
    }

    // Persistent Suite States (SharedPreferences + Firebase Realtime Sync)
    var thresholdsState by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadThresholds(context, schoolId))
    }
    var correspondenceList by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadCorrespondence(context, schoolId))
    }
    var staffPortfoliosMap by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadStaffPortfolios(context, schoolId))
    }
    var studentWelfareMap by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadStudentWelfare(context, schoolId))
    }
    var archivedYearsList by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadArchivedYears(context, schoolId))
    }
    var inventoryList by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadInventory(context, schoolId))
    }
    var expensesList by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.loadExpenses(context, schoolId))
    }

    LaunchedEffect(schoolId) {
        val synced = MinistryDataPersistenceManager.syncFromFirebaseIfAvailable(context, schoolId)
        if (synced) {
            thresholdsState = MinistrySuiteLocalStore.loadThresholds(context, schoolId)
            correspondenceList = MinistrySuiteLocalStore.loadCorrespondence(context, schoolId)
            staffPortfoliosMap = MinistrySuiteLocalStore.loadStaffPortfolios(context, schoolId)
            studentWelfareMap = MinistrySuiteLocalStore.loadStudentWelfare(context, schoolId)
            archivedYearsList = MinistrySuiteLocalStore.loadArchivedYears(context, schoolId)
            inventoryList = MinistrySuiteLocalStore.loadInventory(context, schoolId)
            expensesList = MinistrySuiteLocalStore.loadExpenses(context, schoolId)
        }
    }

    val refreshCorrespondenceState: (List<OfficialCorrespondenceRecord>) -> Unit = { updatedCor ->
        correspondenceList = updatedCor
    }

    if (!isAdmin) {
        return
    }

    Dialog(
        onDismissRequest = { onBack?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF8FAFC))
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Top Royal Emerald & Gold Header with Official Document Crest & 6 Horizontal Scrollable Tabs
                MinistrySuiteExecutiveHeader(
                    schoolName = schoolConfig.schoolName,
                    principalName = principalName,
                    academicYear = schoolConfig.academicYear,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = onBack
                )

                // Package 8: Animated Action Feedback Banner
                AnimatedVisibility(
                    visible = feedbackBannerMessage != null,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    val bannerMsg = feedbackBannerMessage.orEmpty()
                    val bannerBg = if (feedbackBannerIsSuccess) Color(0xFF065F46) else Color(0xFF991B1B)
                    val bannerBorder = if (feedbackBannerIsSuccess) Color(0xFF34D399) else Color(0xFFF87171)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                            .testTag("ministry_action_feedback_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = bannerBg,
                        border = BorderStroke(1.5.dp, bannerBorder),
                        shadowElevation = 6.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = if (feedbackBannerIsSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = bannerMsg,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { feedbackBannerMessage = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Active Section Content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when (selectedTab) {
                        0 -> ExamControlCenterSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            students = activeStudents,
                            grades = allGrades,
                            examSchedules = examSchedules,
                            thresholds = thresholdsState,
                            onSaveThresholds = { updated ->
                                thresholdsState = updated
                                MinistrySuiteLocalStore.saveThresholds(context, schoolId, updated)
                                triggerActionBanner("تم حفظ واعتماد الضوابط في قاعدة البيانات بنجاح ✓", true)
                            },
                            onCorrespondenceAutoUpdated = refreshCorrespondenceState,
                            onTriggerBanner = triggerActionBanner
                        )

                        1 -> OfficialMinistryRecordsSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            students = activeStudents,
                            grades = allGrades,
                            thresholds = thresholdsState,
                            onSaveThresholds = { updated ->
                                thresholdsState = updated
                                MinistrySuiteLocalStore.saveThresholds(context, schoolId, updated)
                                triggerActionBanner("تم حفظ واعتماد الضوابط في قاعدة البيانات بنجاح ✓", true)
                            },
                            correspondenceList = correspondenceList,
                            onSaveCorrespondence = { updated ->
                                correspondenceList = updated
                                MinistrySuiteLocalStore.saveCorrespondence(context, schoolId, updated)
                            },
                            onTriggerBanner = triggerActionBanner
                        )

                        2 -> StaffHrAndPortfolioSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            teachers = realTeachers,
                            portfoliosMap = staffPortfoliosMap,
                            onSavePortfolios = { updated ->
                                staffPortfoliosMap = updated
                                MinistrySuiteLocalStore.saveStaffPortfolios(context, schoolId, updated)
                                triggerActionBanner("تم حفظ الإضبارة الوظيفية للمعلم في قاعدة البيانات بنجاح ✓", true)
                            },
                            onCorrespondenceAutoUpdated = refreshCorrespondenceState,
                            onTriggerBanner = triggerActionBanner
                        )

                        3 -> StudentWelfareAndOrphansSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            students = activeStudents,
                            welfareMap = studentWelfareMap,
                            onSaveWelfare = { updated ->
                                studentWelfareMap = updated
                                MinistrySuiteLocalStore.saveStudentWelfare(context, schoolId, updated)
                                triggerActionBanner("تم حفظ تصنيف الرعاية والمنحة للطالب بشكل دائم ✓", true)
                            },
                            onCorrespondenceAutoUpdated = refreshCorrespondenceState,
                            onTriggerBanner = triggerActionBanner
                        )

                        4 -> YearEndArchivingAndPromotionSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            students = activeStudents,
                            grades = allGrades,
                            archivedYears = archivedYearsList,
                            onSaveArchivedYears = { updated ->
                                archivedYearsList = updated
                                MinistrySuiteLocalStore.saveArchivedYears(context, schoolId, updated)
                                triggerActionBanner("تم حفظ وأرشفة السنة الدراسية في قاعدة البيانات بنجاح ✓", true)
                            },
                            onCorrespondenceAutoUpdated = refreshCorrespondenceState,
                            onTriggerBanner = triggerActionBanner
                        )

                        5 -> SchoolInventoryAndExpensesSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            tuitions = allTuitions,
                            inventoryList = inventoryList,
                            onSaveInventory = { updated ->
                                inventoryList = updated
                                MinistrySuiteLocalStore.saveInventory(context, schoolId, updated)
                                triggerActionBanner("تم حفظ سجل الجرد في قاعدة البيانات بنجاح ✓", true)
                            },
                            expensesList = expensesList,
                            onSaveExpenses = { updated ->
                                expensesList = updated
                                MinistrySuiteLocalStore.saveExpenses(context, schoolId, updated)
                                triggerActionBanner("تم حفظ وصل الصرف في قاعدة البيانات بنجاح ✓", true)
                            },
                            onCorrespondenceAutoUpdated = refreshCorrespondenceState,
                            onTriggerBanner = triggerActionBanner
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. EXECUTIVE HEADER & OFFICIAL DOCUMENT BOOK VECTOR CREST (NO CROSSED LINES)
// =========================================================================

@Composable
private fun MinistrySuiteExecutiveHeader(
    schoolName: String,
    principalName: String,
    academicYear: String,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    onBack: (() -> Unit)?
) {
    val waveShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - 14f)
            cubicTo(
                w * 0.70f, h + 6f,
                w * 0.30f, h - 24f,
                0f, h - 8f
            )
            close()
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, waveShape),
        shape = waveShape,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF042F24),
                            Color(0xFF065F46),
                            Color(0xFF059669)
                        )
                    )
                )
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        if (onBack != null) {
                            Surface(
                                onClick = onBack,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .size(34.dp)
                                    .testTag("ministry_suite_back_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "رجوع",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        MinistryExecutiveCrestVectorCanvas(
                            modifier = Modifier.size(46.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "مركز الإدارة المدرسية واللجنة الامتحانية",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "$schoolName • المدير: $principalName • العام الدراسي: $academicYear",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // 6 Official Sections Horizontal Scrollable Tab Bar
                val tabIcons = listOf(
                    Icons.Filled.VerifiedUser,
                    Icons.Filled.Gavel,
                    Icons.Filled.Badge,
                    Icons.Filled.VolunteerActivism,
                    Icons.Filled.School,
                    Icons.Filled.Inventory
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(MinistrySuiteSectionTab.entries, key = { it.index }) { tab ->
                        val isSelected = selectedTab == tab.index
                        Surface(
                            onClick = { onSelectTab(tab.index) },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.28f)
                            ),
                            modifier = Modifier.testTag("ministry_suite_tab_${tab.index}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tabIcons[tab.index],
                                    contentDescription = tab.arabicTitle,
                                    tint = if (isSelected) Color(0xFF042F24) else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${tab.index + 1}. ${tab.arabicTitle}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) Color(0xFF042F24) else Color.White,
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

@Composable
fun MinistryExecutiveCrestVectorCanvas(
    modifier: Modifier = Modifier
) {
    com.example.ui.components.ManagementCenterAnimatedEmblemCanvas(
        modifier = modifier
    )
}

// =========================================================================
// 5. SECTION 1: EXAM CONTROL CENTER (اللجنة الامتحانية والكنترول)
// =========================================================================

@Composable
private fun ExamControlCenterSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    students: List<StudentRecord>,
    grades: List<SubjectGrade>,
    examSchedules: List<ExamScheduleItem>,
    thresholds: ExemptionAndWarningThresholds,
    onSaveThresholds: (ExemptionAndWarningThresholds) -> Unit,
    onCorrespondenceAutoUpdated: (List<OfficialCorrespondenceRecord>) -> Unit = {},
    onTriggerBanner: (String, Boolean) -> Unit
) {
    val schoolId = schoolConfig.schoolId.ifBlank { "default_school" }
    var activeSubTool by remember { mutableIntStateOf(0) }
    val subTools = remember {
        listOf(
            0 to "1. محرك الإعفاء ودرجة القرار",
            1 to "2. مولّد خرائط الجلوس والقطاعات",
            2 to "3. إصدار البطاقات الامتحانية",
            3 to "4. مُنشئ الأسئلة الرسمية (PDF)"
        )
    }

    // Customizable Exemption & Decision Thresholds (Package 3)
    var generalExemptMinAvg by remember(thresholds) { mutableIntStateOf(thresholds.generalExemptMinAvg) }
    var generalExemptMinSubject by remember(thresholds) { mutableIntStateOf(thresholds.generalExemptMinSubject) }
    var individualExemptMinScore by remember(thresholds) { mutableIntStateOf(thresholds.individualExemptMinScore) }
    var decisionPointsLimit by remember(thresholds) { mutableIntStateOf(thresholds.decisionPointsLimit) }

    val exemptionAnalysis = remember(
        students,
        grades,
        generalExemptMinAvg,
        generalExemptMinSubject,
        individualExemptMinScore,
        decisionPointsLimit
    ) {
        students.map { stu ->
            val stuGrades = grades.filter { it.studentId == stu.id }
            val overallAvg = if (stuGrades.isNotEmpty()) {
                stuGrades.map { it.normalizedPercentage }.average().roundToInt()
            } else 0
            val minSubjectScore = stuGrades.minOfOrNull { it.normalizedPercentage } ?: 0
            val isGeneralExempt = stuGrades.isNotEmpty() &&
                overallAvg >= generalExemptMinAvg &&
                minSubjectScore >= generalExemptMinSubject
            val individualExemptSubjects = stuGrades
                .filter { it.normalizedPercentage >= individualExemptMinScore }
                .map { "${it.subjectName} (${it.normalizedPercentage}%)" }
            val failingGrades = stuGrades.filter { it.normalizedPercentage in 1 until 50 }
            val pointsNeededToPass = failingGrades.sumOf { (50 - it.normalizedPercentage).coerceAtLeast(0) }
            val canPassByDecision = failingGrades.isNotEmpty() && pointsNeededToPass in 1..decisionPointsLimit

            Triple(stu, isGeneralExempt to individualExemptSubjects, failingGrades to pointsNeededToPass) to (overallAvg to canPassByDecision)
        }
    }
    val generalExemptList = remember(exemptionAnalysis) { exemptionAnalysis.filter { it.first.second.first } }
    val individualExemptList = remember(exemptionAnalysis) { exemptionAnalysis.filter { !it.first.second.first && it.first.second.second.isNotEmpty() } }
    val decisionEligibleList = remember(exemptionAnalysis) { exemptionAnalysis.filter { it.second.second } }

    // Tool 2 State: Seating Plan Generator (Persisted per student)
    var hallsCount by remember { mutableIntStateOf(3) }
    var hallCapacity by remember { mutableIntStateOf(20) }
    var selectedHallPreview by remember { mutableIntStateOf(1) }
    var generatedSeats by remember(students, hallsCount, hallCapacity) {
        mutableStateOf(
            generateSmartInterleavedSeatingPlan(students, hallsCount, hallCapacity).map { defaultSeat ->
                val savedPair = MinistrySuiteLocalStore.getStudentExamHallAndSeatNumbers(context, schoolId, defaultSeat.student.id)
                if (savedPair.first > 0 && savedPair.second > 0) {
                    defaultSeat.copy(
                        hallNumber = savedPair.first,
                        hallName = "القاعة الامتحانية (${savedPair.first})",
                        seatNumber = savedPair.second
                    )
                } else {
                    defaultSeat
                }
            }
        )
    }

    // Tool 3 State: Exam Cards Filter & Smart ID Exam Pass Link (Package 6)
    var selectedClassForCards by remember { mutableStateOf("الكل") }
    var isSmartIdExamPassEnabled by remember(schoolId) {
        mutableStateOf(MinistrySuiteLocalStore.isSmartIdExamPassEnabled(context, schoolId))
    }
    var examEligibilityMap by remember(students, schoolId) {
        mutableStateOf(
            students.associate { stu ->
                stu.id to MinistrySuiteLocalStore.isStudentExamAllowed(context, schoolId, stu.id)
            }
        )
    }

    // Tool 4 State: Dynamic Official Exam Questions Paper Builder (Persisted & Zero Fake Texts)
    var examSubject by remember { mutableStateOf(schoolConfig.registeredSubjects.firstOrNull() ?: "اللغة العربية") }
    var examGradeClass by remember { mutableStateOf(schoolConfig.registeredClasses.firstOrNull() ?: "الأول المتوسط") }
    var examRound by remember { mutableStateOf("امتحانات نهاية السنة — الدور الأول") }
    var examDuration by remember { mutableStateOf("ساعتان") }
    var examDateIso by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
    var subjectTeacherName by remember { mutableStateOf("") }
    var examInstructionNote by remember { mutableStateOf("ملاحظة: الإجابة عن خمسة أسئلة فقط، ولكل سؤال (20) درجة.") }
    var questionDrafts by remember(schoolId) {
        mutableStateOf(MinistryDataPersistenceManager.loadExamQuestions(context, schoolId))
    }

    val saveQuestionDraftsState: (List<ExamQuestionItemDraft>) -> Unit = { newList ->
        val renumbered = newList.mapIndexed { i, item ->
            item.copy(
                questionNumber = i + 1,
                branchA = item.branches.getOrNull(0) ?: "",
                branchB = item.branches.getOrNull(1) ?: ""
            )
        }
        questionDrafts = renumbered
        MinistryDataPersistenceManager.saveExamQuestions(context, schoolId, renumbered)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Sub-Tool Selector Bar
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subTools, key = { it.first }) { (idx, title) ->
                    val isSel = activeSubTool == idx
                    FilterChip(
                        selected = isSel,
                        onClick = { activeSubTool = idx },
                        label = {
                            Text(
                                text = title,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF042F24),
                            selectedLabelColor = Color(0xFFFDE68A)
                        ),
                        modifier = Modifier.testTag("exam_control_subtool_$idx")
                    )
                }
            }
        }

        when (activeSubTool) {
            // -----------------------------------------------------------------
            // SUB-TOOL 0: محرك درجة القرار والإعفاء التلقائي (مع تحكم مرن للمدير)
            // -----------------------------------------------------------------
            0 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.5.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "أولاً: محرك الإعفاء العام والفردي التلقائي (قابل للتخصيص من مدير المدرسة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "تخصيص نسب وضوابط الإعفاء ودرجة القرار:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF042F24)
                                    )

                                    ThresholdStepperRow(
                                        label = "معدل الإعفاء العام الشامل:",
                                        valueText = "$generalExemptMinAvg%",
                                        onDecrement = { generalExemptMinAvg = (generalExemptMinAvg - 1).coerceAtLeast(50) },
                                        onIncrement = { generalExemptMinAvg = (generalExemptMinAvg + 1).coerceAtMost(100) }
                                    )
                                    ThresholdStepperRow(
                                        label = "الحد الأدنى للدرجة في الإعفاء العام:",
                                        valueText = "$generalExemptMinSubject%",
                                        onDecrement = { generalExemptMinSubject = (generalExemptMinSubject - 1).coerceAtLeast(50) },
                                        onIncrement = { generalExemptMinSubject = (generalExemptMinSubject + 1).coerceAtMost(100) }
                                    )
                                    ThresholdStepperRow(
                                        label = "نسبة الإعفاء الفردي بالمادة:",
                                        valueText = "$individualExemptMinScore%",
                                        onDecrement = { individualExemptMinScore = (individualExemptMinScore - 1).coerceAtLeast(50) },
                                        onIncrement = { individualExemptMinScore = (individualExemptMinScore + 1).coerceAtMost(100) }
                                    )
                                    ThresholdStepperRow(
                                        label = "سقف درجات القرار المسموحة:",
                                        valueText = "$decisionPointsLimit درجات",
                                        onDecrement = { decisionPointsLimit = (decisionPointsLimit - 1).coerceAtLeast(1) },
                                        onIncrement = { decisionPointsLimit = (decisionPointsLimit + 1).coerceAtMost(25) }
                                    )

                                    Button(
                                        onClick = {
                                            onSaveThresholds(
                                                thresholds.copy(
                                                    generalExemptMinAvg = generalExemptMinAvg,
                                                    generalExemptMinSubject = generalExemptMinSubject,
                                                    individualExemptMinScore = individualExemptMinScore,
                                                    decisionPointsLimit = decisionPointsLimit
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_exemption_thresholds_btn")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حفظ واعتماد ضوابط الإعفاء والقرار", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MinistryMetricPill("إعفاء عام شامل", "${generalExemptList.size} طالب", Modifier.weight(1f))
                                MinistryMetricPill("إعفاء فردي بالمواد", "${individualExemptList.size} طالب", Modifier.weight(1f))
                                MinistryMetricPill("مستحقو القرار", "${decisionEligibleList.size} طالب", Modifier.weight(1f))
                            }

                            if (generalExemptList.isEmpty() && individualExemptList.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "يتم فرز الطلبة المعفوين تلقائياً فور استكمال رصد درجات السعي السنوي ($individualExemptMinScore% للإعفاء الفردي أو $generalExemptMinAvg% للإعفاء العام).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF047857),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                (generalExemptList + individualExemptList).take(15).forEach { (triple, avgPair) ->
                                    val stu = triple.first
                                    val isGen = triple.second.first
                                    val indSubs = triple.second.second
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "${stu.fullNameWithFather} — ${stu.gradeClass} (شعبة ${stu.sectionName})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF042F24),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = if (isGen) {
                                                    "التصنيف المعتمد: إعفاء عام شامل بجميع المواد (المعدل العام: ${avgPair.first}%)"
                                                } else {
                                                    "التصنيف المعتمد: إعفاء فردي في المواد: ${indSubs.joinToString("، ")}"
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF059669),
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val rows = (generalExemptList + individualExemptList).ifEmpty { exemptionAnalysis }.mapIndexed { idx, (triple, avgPair) ->
                                        val stu = triple.first
                                        val isGen = triple.second.first
                                        val indSubs = triple.second.second
                                        listOf(
                                            "${idx + 1}",
                                            stu.fullNameWithFather,
                                            "${stu.gradeClass} (${stu.sectionName})",
                                            "${avgPair.first}%",
                                            when {
                                                isGen -> "إعفاء عام شامل"
                                                indSubs.isNotEmpty() -> "إعفاء فردي (${indSubs.size} مواد)"
                                                else -> "غير مشمول بالإعفاء"
                                            }
                                        )
                                    }
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "قائمة الشرف الرسمية للطلبة المعفوين (الإعفاء العام والفردي)",
                                        subtitleInfo = "مفرزة تلقائياً وفق الضوابط المعتمدة (العام $generalExemptMinAvg% • الفردي $individualExemptMinScore%)",
                                        summaryPills = listOf(
                                            "إعفاء عام" to "${generalExemptList.size}",
                                            "إعفاء فردي" to "${individualExemptList.size}",
                                            "إجمالي الطلبة" to "${students.size}"
                                        ),
                                        headers = listOf("ت", "اسم الطالب", "الصف والشعبة", "المعدل العام", "نوع الإعفاء"),
                                        rows = rows,
                                        schoolId = schoolId,
                                        onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_exempt_honor_pdf_btn")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة وتصدير قائمة الشرف للمعفوين (PDF)", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Decision Points Engine
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ثانياً: معالج درجة القرار ($decisionPointsLimit درجات)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "يحدد تلقائياً الطلبة المكملين أو الراسبين الذين ترفعهم درجة القرار المحددة ($decisionPointsLimit درجات) إلى درجة النجاح الصغرى (50%).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.fillMaxWidth()
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf(5 to "قرار (5) درجات", 10 to "قرار (10) درجات", 15 to "قرار (15) درجة")) { (pts, label) ->
                                    FilterChip(
                                        selected = decisionPointsLimit == pts,
                                        onClick = { decisionPointsLimit = pts },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        modifier = Modifier.widthIn(min = 115.dp)
                                    )
                                }
                            }

                            if (decisionEligibleList.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "لا يوجد حالياً طلبة بحاجة إلى ($decisionPointsLimit) درجات قرار للوصول إلى درجة النجاح (50%).",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF92400E),
                                        modifier = Modifier.padding(12.dp)
                                    )
                                }
                            } else {
                                decisionEligibleList.forEach { (triple, _) ->
                                    val stu = triple.first
                                    val failingList = triple.third.first
                                    val totalNeeded = triple.third.second
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White,
                                        border = BorderStroke(1.dp, Color(0xFFD97706)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${stu.fullNameWithFather} (${stu.gradeClass} - شعبة ${stu.sectionName})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF042F24),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Text(
                                                text = "المواد المشمولة بالقرار: " + failingList.joinToString("، ") { "${it.subjectName} (${it.normalizedPercentage}% ← 50%)" } + " • مجموع درجات القرار المطلوبة: ($totalNeeded) من ($decisionPointsLimit)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.SemiBold,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            Button(
                                                onClick = {
                                                    failingList.forEach { fg ->
                                                        val needed = (50 - fg.normalizedPercentage).coerceAtLeast(1)
                                                        viewModel.applyMinistryDecisionGradeBoost(stu.id, fg.subjectName, needed)
                                                    }
                                                    onTriggerBanner("تم تطبيق درجة القرار واعتماد النجاح ✓", true)
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("تطبيق درجة القرار واعتماد النجاح", fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    decisionEligibleList.forEach { (triple, _) ->
                                        val stu = triple.first
                                        val failingList = triple.third.first
                                        failingList.forEach { fg ->
                                            val needed = (50 - fg.normalizedPercentage).coerceAtLeast(1)
                                            viewModel.applyMinistryDecisionGradeBoost(stu.id, fg.subjectName, needed)
                                        }
                                    }
                                    onSaveThresholds(
                                        thresholds.copy(
                                            generalExemptMinAvg = generalExemptMinAvg,
                                            generalExemptMinSubject = generalExemptMinSubject,
                                            individualExemptMinScore = individualExemptMinScore,
                                            decisionPointsLimit = decisionPointsLimit
                                        )
                                    )
                                    onTriggerBanner("تم حفظ واعتماد الضوابط في قاعدة البيانات بنجاح ✓", true)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFD97706),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("apply_all_decision_points_btn")
                            ) {
                                Icon(Icons.Filled.TaskAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تطبيق درجة القرار واعتماد النجاح للجميع", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // SUB-TOOL 1: مولّد خرائط الجلوس والقطاعات الامتحانية (Package 4)
            // -----------------------------------------------------------------
            1 -> {
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "مولّد خرائط الجلوس والقطاعات الامتحانية الذكي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "يوزع الطلبة (${students.size} طالب) تلقائياً على القاعات والمقاعد مع دمج الصفوف المختلفة بالتناوب في كل قطاع لمنع الغش.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF475569),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Text(
                                text = "عدد القاعات الامتحانية: ($hallsCount قاعات)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F24)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(listOf(2, 3, 4, 5, 6, 8)) { hc ->
                                    FilterChip(
                                        selected = hallsCount == hc,
                                        onClick = {
                                            hallsCount = hc
                                            if (selectedHallPreview > hc) selectedHallPreview = 1
                                        },
                                        label = {
                                            Text(
                                                text = "$hc قاعات",
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.widthIn(min = 95.dp)
                                    )
                                }
                            }

                            Text(
                                text = "سعة كل قاعة امتحانية: ($hallCapacity مقعد)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F24)
                            )
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(listOf(15, 20, 24, 30, 36)) { cap ->
                                    FilterChip(
                                        selected = hallCapacity == cap,
                                        onClick = { hallCapacity = cap },
                                        label = {
                                            Text(
                                                text = "$cap مقعد",
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        },
                                        modifier = Modifier.widthIn(min = 95.dp)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    generatedSeats = generateSmartInterleavedSeatingPlan(students, hallsCount, hallCapacity)
                                    generatedSeats.forEach { seat ->
                                        MinistrySuiteLocalStore.saveStudentSeatLabel(
                                            context = context,
                                            schoolId = schoolId,
                                            studentId = seat.student.id,
                                            seatLabel = "قاعة (${seat.hallNumber}) - مقعد (${seat.seatNumber})",
                                            hallNumber = seat.hallNumber,
                                            seatNumber = seat.seatNumber
                                        )
                                    }
                                    onTriggerBanner("تم توزيع الطلبة وحفظ القاعات والمقاعد في قاعدة البيانات ✓", true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_seating_plan_btn")
                            ) {
                                Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد وحفظ خرائط الجلوس الذكية", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                // Hall Preview Selector & Map
                item {
                    val hallStudents = generatedSeats.filter { it.hallNumber == selectedHallPreview }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "معاينة خريطة القاعة الامتحانية وتوزيع القطاعات:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24)
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items((1..hallsCount).toList()) { hNum ->
                                    val cnt = generatedSeats.count { it.hallNumber == hNum }
                                    FilterChip(
                                        selected = selectedHallPreview == hNum,
                                        onClick = { selectedHallPreview = hNum },
                                        label = {
                                            Text(
                                                text = "القاعة ($hNum) • $cnt طالب",
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        },
                                        modifier = Modifier.widthIn(min = 120.dp)
                                    )
                                }
                            }

                            hallStudents.chunked(2).forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    pair.forEach { seat ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Text(
                                                    text = "مقعد (${seat.seatNumber}) • ${seat.sectorCode}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFFD97706)
                                                )
                                                Text(
                                                    text = seat.student.studentName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF042F24),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${seat.student.gradeClass} (${seat.student.sectionName})",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    }
                                    if (pair.size == 1) Spacer(modifier = Modifier.weight(1f))
                                }
                            }

                            Button(
                                onClick = {
                                    val rows = hallStudents.map { s ->
                                        listOf(
                                            "مقعد ${s.seatNumber}",
                                            s.sectorCode,
                                            s.student.fullNameWithFather,
                                            s.student.gradeClass,
                                            s.student.sectionName
                                        )
                                    }
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "خريطة الجلوس الرسمية — القاعة الامتحانية رقم ($selectedHallPreview)",
                                        subtitleInfo = "توزيع القطاعات المدمجة بين الصفوف لمنع الغش • سعة القاعة: ($hallCapacity) مقعد",
                                        summaryPills = listOf(
                                            "رقم القاعة" to "قاعة $selectedHallPreview",
                                            "عدد الممتحنين" to "${hallStudents.size} طالب",
                                            "إجمالي القاعات" to "$hallsCount قاعات"
                                        ),
                                        headers = listOf("رقم المقعد", "القطاع", "اسم الطالب الثلاثي", "الصف الدراسي", "الشعبة"),
                                        rows = rows,
                                        schoolId = schoolId,
                                        onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_seating_hall_pdf_btn")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة وتصدير خريطة القاعة ($selectedHallPreview) بصيغة PDF", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // SUB-TOOL 2: إصدار بطاقات دخول الامتحان المدرسي وربطها بالهوية الذكية (Package 6)
            // -----------------------------------------------------------------
            2 -> {
                val distinctClasses = listOf("الكل") + students.map { it.gradeClass }.distinct()
                val filteredSeatCards = generatedSeats.filter {
                    selectedClassForCards == "الكل" || it.student.gradeClass == selectedClassForCards
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "إصدار بطاقات دخول الامتحان المدرسي وربطها بهوية الطالب الذكية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "اعتماد بطاقة هوية الطالب الذكية كهوية دخول رسمية للامتحانات",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF042F24)
                                        )
                                        Text(
                                            text = "تظهر شارة التخويل ورقم القاعة والمقعد تلقائياً على هوية الطالب المحفوظة والمطبوعة وباركود QR",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF065F46)
                                        )
                                    }
                                    Switch(
                                        checked = isSmartIdExamPassEnabled,
                                        onCheckedChange = { enabled ->
                                            isSmartIdExamPassEnabled = enabled
                                            MinistrySuiteLocalStore.setSmartIdExamPassEnabled(context, schoolId, enabled)
                                            onTriggerBanner("تم حفظ واعتماد الضوابط في قاعدة البيانات بنجاح ✓", true)
                                        },
                                        modifier = Modifier.testTag("toggle_smart_id_exam_pass")
                                    )
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(distinctClasses) { cls ->
                                    FilterChip(
                                        selected = selectedClassForCards == cls,
                                        onClick = { selectedClassForCards = cls },
                                        label = { Text(cls, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val rows = filteredSeatCards.map { item ->
                                        val isAllowed = examEligibilityMap[item.student.id] ?: true
                                        listOf(
                                            item.student.fullNameWithFather,
                                            "${item.student.gradeClass} (${item.student.sectionName})",
                                            item.hallName,
                                            "مقعد ${item.seatNumber} (${item.sectorCode})",
                                            if (isAllowed) "مشمول بدخول الامتحان ✓" else "محجوب ✗"
                                        )
                                    }
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "كشف بطاقات دخول الامتحان المدرسي ($selectedClassForCards)",
                                        subtitleInfo = "يتضمن أرقام القاعات والمقاعد وحالة الشمول الامتحاني",
                                        summaryPills = listOf(
                                            "البطاقات الصادرة" to "${filteredSeatCards.size} بطاقة",
                                            "مشمول بالامتحان" to "${filteredSeatCards.count { examEligibilityMap[it.student.id] != false }}",
                                            "العام الدراسي" to schoolConfig.academicYear
                                        ),
                                        headers = listOf("اسم الطالب", "الصف والشعبة", "القاعة", "رقم المقعد", "حالة الدخول"),
                                        rows = rows,
                                        schoolId = schoolId,
                                        onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_all_exam_cards_pdf_btn")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة بطاقات دخول الامتحان المدرسي (${filteredSeatCards.size} بطاقة) PDF", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                items(filteredSeatCards.take(25), key = { it.student.id }) { seatItem ->
                    val isAllowed = examEligibilityMap[seatItem.student.id] ?: true
                    val stuExams = examSchedules.filter {
                        ArabicTextNormalizer.matchesClass(it.gradeClass, seatItem.student.gradeClass)
                    }.take(4)

                    LaunchedEffect(seatItem.hallNumber, seatItem.seatNumber) {
                        MinistrySuiteLocalStore.saveStudentSeatLabel(
                            context = context,
                            schoolId = schoolId,
                            studentId = seatItem.student.id,
                            seatLabel = "قاعة (${seatItem.hallNumber}) - مقعد (${seatItem.seatNumber})",
                            hallNumber = seatItem.hallNumber,
                            seatNumber = seatItem.seatNumber
                        )
                    }

                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(
                            1.4.dp,
                            if (isAllowed) Color(0xFF059669) else DangerRed
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "بطاقة دخول الامتحان المدرسي — ${schoolConfig.schoolName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAllowed) Color(0xFF059669) else DangerRed,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = seatItem.student.fullNameWithFather,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF042F24)
                                    )
                                    Text(
                                        text = "الصف: ${seatItem.student.gradeClass} (شعبة ${seatItem.student.sectionName}) • ${seatItem.hallName} • المقعد: (${seatItem.seatNumber}) • ${seatItem.sectorCode}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF334155),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                ExamVerificationQrMiniCanvas(
                                    seed = "${seatItem.student.qrCodeValue}:EXAM:${if (isAllowed) "ALLOWED" else "BLOCKED"}:H${seatItem.hallNumber}:S${seatItem.seatNumber}",
                                    modifier = Modifier.size(54.dp)
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isAllowed) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                border = BorderStroke(
                                    1.dp,
                                    if (isAllowed) Color(0xFF10B981) else Color(0xFFF87171)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isAllowed) {
                                                "مشمول بدخول الامتحان ✓"
                                            } else {
                                                "غير مشمول / محجوب عن الامتحان ✗"
                                            },
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isAllowed) Color(0xFF065F46) else Color(0xFF991B1B)
                                        )
                                        Text(
                                            text = if (isAllowed) {
                                                "مخول بدخول الامتحان • قاعة (${seatItem.hallNumber}) - مقعد (${seatItem.seatNumber})"
                                            } else {
                                                "تم حجب تخويل الدخول في الهوية المدرسية الذكية والباركود"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isAllowed) Color(0xFF047857) else Color(0xFFB91C1C),
                                            maxLines = 1
                                        )
                                    }
                                    Switch(
                                        checked = isAllowed,
                                        onCheckedChange = { newAllowed ->
                                            examEligibilityMap = examEligibilityMap.toMutableMap().apply {
                                                put(seatItem.student.id, newAllowed)
                                            }
                                            MinistrySuiteLocalStore.setStudentExamAllowed(
                                                context = context,
                                                schoolId = schoolId,
                                                studentId = seatItem.student.id,
                                                allowed = newAllowed
                                            )
                                            MinistrySuiteLocalStore.saveStudentSeatLabel(
                                                context = context,
                                                schoolId = schoolId,
                                                studentId = seatItem.student.id,
                                                seatLabel = "قاعة (${seatItem.hallNumber}) - مقعد (${seatItem.seatNumber})",
                                                hallNumber = seatItem.hallNumber,
                                                seatNumber = seatItem.seatNumber
                                            )
                                            val statusTitle = if (newAllowed) {
                                                "تخويل دخول الامتحان المدرسي — ${seatItem.student.studentName}"
                                            } else {
                                                "تنبيه حجب بطاقة الامتحان المدرسي — ${seatItem.student.studentName}"
                                            }
                                            val statusBody = if (newAllowed) {
                                                "تم اعتماد شمول الطالب (${seatItem.student.fullNameWithFather}) بدخول الامتحانات المدرسية في (${seatItem.hallName} - مقعد ${seatItem.seatNumber}) وربطه بهوية الطالب الذكية."
                                            } else {
                                                "يرجى مراجعة إدارة المدرسة بخصوص بطاقة دخول الامتحان المدرسي للطالب (${seatItem.student.fullNameWithFather})."
                                            }
                                            viewModel.pushNotification(
                                                title = statusTitle,
                                                body = statusBody,
                                                type = NotificationType.ATTENDANCE,
                                                targetStudentId = seatItem.student.id,
                                                targetRole = UserRole.PARENT.name,
                                                isUrgent = !newAllowed,
                                                priority = if (newAllowed) "normal" else "high"
                                            )
                                            onTriggerBanner("تم تحديث الحالة وحفظها وإشعار ولي الأمر بنجاح ✓", newAllowed)
                                        }
                                    )
                                }
                            }

                            if (stuExams.isNotEmpty()) {
                                Text(
                                    text = "جدول الامتحانات المثبت على البطاقة: " + stuExams.joinToString(" | ") { "${it.subjectName} (${it.examDate})" },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                            }

                            OutlinedButton(
                                onClick = {
                                    val qrStatus = if (isAllowed) "EXAM:ALLOWED:H${seatItem.hallNumber}:S${seatItem.seatNumber}" else "EXAM:BLOCKED"
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "بطاقة دخول الامتحان المدرسي — الطالب: ${seatItem.student.fullNameWithFather}",
                                        subtitleInfo = "الصف: ${seatItem.student.gradeClass} (${seatItem.student.sectionName}) | ${seatItem.hallName} | مقعد (${seatItem.seatNumber}) | الحالة: ${if (isAllowed) "مشمول بدخول الامتحان ✓" else "غير مشمول ✗"}",
                                        summaryPills = listOf(
                                            "القاعة الامتحانية" to seatItem.hallName,
                                            "رقم المقعد" to "${seatItem.seatNumber}",
                                            "حالة الدخول" to if (isAllowed) "مخول ✓" else "محجوب ✗"
                                        ),
                                        headers = listOf("المادة الامتحانية", "تاريخ الامتحان", "اليوم / الوقت", "القاعة والمقعد"),
                                        rows = if (stuExams.isNotEmpty()) {
                                            stuExams.map { ex ->
                                                listOf(ex.subjectName, ex.examDate, ex.timeSlot.ifBlank { "${ex.dayName} • 8:30 صباحاً" }, "${seatItem.hallName} - مقعد ${seatItem.seatNumber}")
                                            }
                                        } else {
                                            (schoolConfig.registeredSubjects.ifEmpty { listOf("اللغة العربية", "الرياضيات", "اللغة الإنجليزية", "العلوم") }).take(6).map { sub ->
                                                listOf(sub, "حسب الجدول المعلن", "8:30 صباحاً", "${seatItem.hallName} - مقعد ${seatItem.seatNumber}")
                                            }
                                        },
                                        footerNote = "باركود التحقق الامتحاني المعتمد: ${seatItem.student.qrCodeValue}:$qrStatus",
                                        schoolId = schoolId,
                                        onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة بطاقة دخول الامتحان المدرسي الفردية (PDF)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // SUB-TOOL 3: مُنشئ وورقة الأسئلة الامتحانية الرسمية (ديناميكي 100% + عمودي حر بدون جداول)
            // -----------------------------------------------------------------
            3 -> {
                val arabicBranchLetters = listOf("أ", "ب", "ج", "د", "هـ", "و")
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.5.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "مُنشئ الأسئلة الامتحانية الرسمي (تخطيط عمودي وزاري/مدرسي حر بدون جداول)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = examSubject,
                                    onValueChange = { examSubject = it },
                                    label = { Text("المادة الدراسية") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = examGradeClass,
                                    onValueChange = { examGradeClass = it },
                                    label = { Text("الصف الدراسي") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = examRound,
                                    onValueChange = { examRound = it },
                                    label = { Text("الدور / الفصل الامتحاني") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = examDuration,
                                    onValueChange = { examDuration = it },
                                    label = { Text("الزمن المحدد") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = examDateIso,
                                    onValueChange = { examDateIso = it },
                                    label = { Text("تاريخ الامتحان") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = subjectTeacherName,
                                    onValueChange = { subjectTeacherName = it },
                                    label = { Text("معلم / مدرس المادة") },
                                    placeholder = { Text("اكتب اسم معلم المادة...") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            OutlinedTextField(
                                value = examInstructionNote,
                                onValueChange = { examInstructionNote = it },
                                label = { Text("ملاحظة اللجنة الامتحانية تحت الترويسة") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            questionDrafts.forEachIndexed { idx, q ->
                                val currentBranches = q.effectiveBranches()
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "س${idx + 1} / السؤال (${idx + 1}) — (${q.marks} درجة)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF042F24),
                                                modifier = Modifier.weight(1f)
                                            )

                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Surface(
                                                    onClick = {
                                                        val updated = questionDrafts.toMutableList()
                                                        updated[idx] = q.copy(marks = (q.marks - 5).coerceAtLeast(5))
                                                        saveQuestionDraftsState(updated)
                                                    },
                                                    shape = CircleShape,
                                                    color = Color(0xFFE2E8F0),
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Filled.Remove, contentDescription = "إنقاص الدرجة", modifier = Modifier.size(14.dp))
                                                    }
                                                }
                                                Text(
                                                    text = "${q.marks}د",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF065F46)
                                                )
                                                Surface(
                                                    onClick = {
                                                        val updated = questionDrafts.toMutableList()
                                                        updated[idx] = q.copy(marks = (q.marks + 5).coerceAtMost(100))
                                                        saveQuestionDraftsState(updated)
                                                    },
                                                    shape = CircleShape,
                                                    color = Color(0xFFD1FAE5),
                                                    modifier = Modifier.size(26.dp)
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Icon(Icons.Filled.Add, contentDescription = "زيادة الدرجة", modifier = Modifier.size(14.dp))
                                                    }
                                                }

                                                if (questionDrafts.size > 1) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            val updated = questionDrafts.toMutableList().apply { removeAt(idx) }
                                                            saveQuestionDraftsState(updated)
                                                        },
                                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.55f)),
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                        shape = RoundedCornerShape(8.dp),
                                                        modifier = Modifier
                                                            .height(30.dp)
                                                            .testTag("delete_exam_question_btn_${idx + 1}")
                                                    ) {
                                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("حذف السؤال", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = q.mainPrompt,
                                            onValueChange = { newVal ->
                                                val updated = questionDrafts.toMutableList()
                                                updated[idx] = q.copy(mainPrompt = newVal, branches = currentBranches)
                                                saveQuestionDraftsState(updated)
                                            },
                                            label = { Text("منطوق س${idx + 1} الرئيسي") },
                                            placeholder = { Text("اكتب منطوق السؤال هنا (مثال: أجب عن فرعين مما يأتي)...") },
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        currentBranches.forEachIndexed { bIdx, bText ->
                                            val bLetter = arabicBranchLetters.getOrElse(bIdx) { "${bIdx + 1}" }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                OutlinedTextField(
                                                    value = bText,
                                                    onValueChange = { newBranchVal ->
                                                        val updatedBranches = currentBranches.toMutableList().apply {
                                                            set(bIdx, newBranchVal)
                                                        }
                                                        val updated = questionDrafts.toMutableList()
                                                        updated[idx] = q.copy(branches = updatedBranches)
                                                        saveQuestionDraftsState(updated)
                                                    },
                                                    label = { Text("فرع ($bLetter)") },
                                                    placeholder = { Text("اكتب نص فرع ($bLetter)...") },
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        val updatedBranches = currentBranches.toMutableList().apply {
                                                            removeAt(bIdx)
                                                        }
                                                        val updated = questionDrafts.toMutableList()
                                                        updated[idx] = q.copy(branches = updatedBranches, branchA = "", branchB = "")
                                                        saveQuestionDraftsState(updated)
                                                    }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "حذف الفرع",
                                                        tint = DangerRed
                                                    )
                                                }
                                            }
                                        }

                                        if (currentBranches.size < 6) {
                                            val nextBranchLetter = arabicBranchLetters.getOrElse(currentBranches.size) { "${currentBranches.size + 1}" }
                                            OutlinedButton(
                                                onClick = {
                                                    val updatedBranches = currentBranches + listOf("")
                                                    val updated = questionDrafts.toMutableList()
                                                    updated[idx] = q.copy(branches = updatedBranches)
                                                    saveQuestionDraftsState(updated)
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("+ إضافة فرع ($nextBranchLetter)", fontWeight = FontWeight.ExtraBold)
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val updated = questionDrafts + listOf(
                                        ExamQuestionItemDraft(
                                            questionNumber = questionDrafts.size + 1,
                                            mainPrompt = "",
                                            marks = 20,
                                            branches = emptyList()
                                        )
                                    )
                                    saveQuestionDraftsState(updated)
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF042F24)),
                                border = BorderStroke(1.5.dp, Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_new_exam_question_btn")
                            ) {
                                Icon(Icons.Filled.AddCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("+ إضافة سؤال جديد (س${questionDrafts.size + 1})", fontWeight = FontWeight.ExtraBold)
                            }

                            Button(
                                onClick = {
                                    exportOfficialExamQuestionsPaperPdf(
                                        context = context,
                                        schoolId = schoolId,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        subjectTeacherName = subjectTeacherName.ifBlank { "مدرّس المادة" },
                                        academicYear = schoolConfig.academicYear,
                                        examSubject = examSubject,
                                        examGradeClass = examGradeClass,
                                        examRound = examRound,
                                        examDuration = examDuration,
                                        examDate = examDateIso,
                                        examInstructionNote = examInstructionNote,
                                        questions = questionDrafts
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_official_exam_questions_pdf_btn")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير وطباعة ورقة الأسئلة الرسمية (PDF)", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateSmartInterleavedSeatingPlan(
    students: List<StudentRecord>,
    hallsCount: Int,
    hallCapacity: Int
): List<ExamSeatAssignment> {
    if (students.isEmpty()) return emptyList()
    val validHalls = hallsCount.coerceAtLeast(1)
    val validCap = hallCapacity.coerceAtLeast(5)

    val byClass = students.groupBy { it.gradeClass }.values.map { it.toMutableList() }
    val interleaved = mutableListOf<StudentRecord>()
    var remaining = true
    while (remaining) {
        remaining = false
        for (bucket in byClass) {
            if (bucket.isNotEmpty()) {
                interleaved.add(bucket.removeAt(0))
                if (bucket.isNotEmpty()) remaining = true
            }
        }
    }

    return interleaved.mapIndexed { idx, stu ->
        val hallIdx = (idx / validCap).coerceAtMost(validHalls - 1)
        val hallNum = hallIdx + 1
        val seatNum = (idx % validCap) + 1
        val sector = if (seatNum % 2 == 1) "القطاع (أ)" else "القطاع (ب)"
        ExamSeatAssignment(
            hallNumber = hallNum,
            hallName = "القاعة الامتحانية ($hallNum)",
            seatNumber = seatNum,
            sectorCode = sector,
            student = stu
        )
    }
}

@Composable
private fun ExamVerificationQrMiniCanvas(
    seed: String,
    modifier: Modifier = Modifier
) {
    val qrBmp = remember(seed) {
        com.example.util.DocumentAndIdExporter.generateQrCodeBitmap(seed, 160).asImageBitmap()
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFF059669))
    ) {
        Image(
            bitmap = qrBmp,
            contentDescription = "باركود التحقق",
            modifier = Modifier
                .fillMaxSize()
                .padding(3.dp),
            contentScale = ContentScale.Fit
        )
    }
}

// =========================================================================
// 6. SECTION 2: OFFICIAL MINISTRY RECORDS, ABSENCE WARNINGS & CORRESPONDENCE
// =========================================================================

@Composable
private fun OfficialMinistryRecordsSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    students: List<StudentRecord>,
    grades: List<SubjectGrade>,
    thresholds: ExemptionAndWarningThresholds,
    onSaveThresholds: (ExemptionAndWarningThresholds) -> Unit,
    correspondenceList: List<OfficialCorrespondenceRecord>,
    onSaveCorrespondence: (List<OfficialCorrespondenceRecord>) -> Unit,
    onTriggerBanner: (String, Boolean) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        0 to "1. السجلات الرسمية (القيد العام والوسطي)",
        1 to "2. إنذارات الغياب القانونية",
        2 to "3. سجل الصادرة والواردة والأوامر الإدارية"
    )

    var selectedRegisterKind by remember { mutableStateOf("سجل القيد العام") }
    var registerSearchQuery by remember { mutableStateOf("") }

    // Customizable Absence Warning Thresholds (Package 3)
    var firstWarningDays by remember(thresholds) { mutableIntStateOf(thresholds.firstWarningDays) }
    var finalWarningDays by remember(thresholds) { mutableIntStateOf(thresholds.finalWarningDays) }
    var dismissalWarningDays by remember(thresholds) { mutableIntStateOf(thresholds.dismissalWarningDays) }
    var showOnlyStudentsWithWarnings by remember { mutableStateOf(true) }

    // Correspondence Form State (Packages 3 & 5)
    var corType by remember { mutableStateOf("صادر") }
    var corNumber by remember { mutableStateOf("") }
    var corDate by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
    var corEntity by remember { mutableStateOf("") }
    var corSubject by remember { mutableStateOf("") }
    var corNotes by remember { mutableStateOf("") }
    var corImageBase64 by remember { mutableStateOf("") }
    var corSearch by remember { mutableStateOf("") }

    var showCorDatePickerDialog by remember { mutableStateOf(false) }
    var lightboxImageBase64 by remember { mutableStateOf<String?>(null) }
    var lightboxTitle by remember { mutableStateOf("") }

    // Image Picker for Official Correspondence Scanned Document (Package 5)
    val correspondenceImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    val rawBmp = BitmapFactory.decodeStream(inStream)
                    if (rawBmp != null) {
                        val maxDim = 1100
                        val ratio = minOf(
                            maxDim.toFloat() / rawBmp.width.coerceAtLeast(1),
                            maxDim.toFloat() / rawBmp.height.coerceAtLeast(1),
                            1f
                        )
                        val scaled = Bitmap.createScaledBitmap(
                            rawBmp,
                            (rawBmp.width * ratio).roundToInt().coerceAtLeast(1),
                            (rawBmp.height * ratio).roundToInt().coerceAtLeast(1),
                            true
                        )
                        val bos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.JPEG, 82, bos)
                        corImageBase64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP)
                    }
                }
            }
        }
    }

    if (showCorDatePickerDialog) {
        MinistryCalendarDatePickerDialog(
            initialDateIso = corDate,
            onDismiss = { showCorDatePickerDialog = false },
            onDateSelected = { selectedIso ->
                corDate = selectedIso
                showCorDatePickerDialog = false
            }
        )
    }

    if (!lightboxImageBase64.isNullOrBlank()) {
        OfficialDocumentLightboxDialog(
            imageBase64 = lightboxImageBase64.orEmpty(),
            title = lightboxTitle,
            onDismiss = { lightboxImageBase64 = null }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(subTabs, key = { it.first }) { (idx, label) ->
                    FilterChip(
                        selected = activeSubTab == idx,
                        onClick = { activeSubTab = idx },
                        label = {
                            Text(
                                text = label,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF042F24),
                            selectedLabelColor = Color(0xFFFDE68A)
                        ),
                        modifier = Modifier.testTag("official_records_subtab_$idx")
                    )
                }
            }
        }

        when (activeSubTab) {
            // -----------------------------------------------------------------
            // 0: السجلات الرسمية (القيد العام + السجل الوسطي + سجل المستويات)
            // -----------------------------------------------------------------
            0 -> {
                val filteredStudents = students.filter {
                    registerSearchQuery.isBlank() ||
                        it.fullNameWithFather.contains(registerSearchQuery, ignoreCase = true) ||
                        it.gradeClass.contains(registerSearchQuery, ignoreCase = true)
                }
                val tableHeaders = when (selectedRegisterKind) {
                    "سجل القيد العام" -> listOf("رقم القيد", "اسم الطالب الثلاثي", "الصف والشعبة", "ولي الأمر والهاتف", "حالة القيد")
                    "السجل الوسطي للدرجات" -> listOf("اسم الطالب", "الصف", "المواد المرصودة", "السعي السنوي", "النتيجة")
                    else -> listOf("اسم الطالب", "الصف والشعبة", "أيام الحضور", "أيام الغياب", "المستوى العام")
                }
                val tableRows = filteredStudents.mapIndexed { idx, stu ->
                    val stuGrades = grades.filter { it.studentId == stu.id }
                    val avg = if (stuGrades.isNotEmpty()) stuGrades.map { it.normalizedPercentage }.average().roundToInt() else 0
                    when (selectedRegisterKind) {
                        "سجل القيد العام" -> listOf(
                            "${1001 + idx}",
                            stu.fullNameWithFather,
                            "${stu.gradeClass} (${stu.sectionName})",
                            "${stu.parentName} (${stu.parentPhone})",
                            stu.enrollmentStatus.arabicLabel
                        )
                        "السجل الوسطي للدرجات" -> listOf(
                            stu.fullNameWithFather,
                            "${stu.gradeClass} (${stu.sectionName})",
                            "${stuGrades.size} مواد",
                            if (stuGrades.isNotEmpty()) "$avg%" else "قيد الرصد",
                            if (stuGrades.isEmpty()) "قيد الرصد" else if (avg >= 50) "ناجح" else "مكمل / معيد"
                        )
                        else -> listOf(
                            stu.fullNameWithFather,
                            "${stu.gradeClass} (${stu.sectionName})",
                            "${viewModel.getTotalPresentDays(stu.id)} يوم",
                            "${viewModel.getTotalAbsenceDays(stu.id)} يوم",
                            if (avg >= 85) "متميز" else if (avg >= 50) "جيد" else "قيد المتابعة"
                        )
                    }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "السجلات المدرسية الرسمية المعتمدة (القيد العام • السجل الوسطي • سجل المستويات)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("سجل القيد العام", "السجل الوسطي للدرجات", "سجل المستويات الشهرية")) { kind ->
                                    FilterChip(
                                        selected = selectedRegisterKind == kind,
                                        onClick = { selectedRegisterKind = kind },
                                        label = { Text(kind, fontWeight = FontWeight.ExtraBold) }
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = registerSearchQuery,
                                onValueChange = { registerSearchQuery = it },
                                label = { Text("بحث في $selectedRegisterKind (الاسم أو الصف)...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            MinistryOfficialPreviewTable(
                                title = "$selectedRegisterKind — (${filteredStudents.size} قيد)",
                                headers = tableHeaders,
                                rows = tableRows.take(20)
                            )

                            Button(
                                onClick = {
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "$selectedRegisterKind الرسمي المعتمد",
                                        subtitleInfo = "إجمالي القيود المسجلة: (${filteredStudents.size}) طالب",
                                        summaryPills = listOf(
                                            "نوع السجل" to selectedRegisterKind,
                                            "عدد القيود" to "${filteredStudents.size}",
                                            "العام الدراسي" to schoolConfig.academicYear
                                        ),
                                        headers = tableHeaders,
                                        rows = tableRows,
                                        schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                        onArchivedRecordCreated = onSaveCorrespondence
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_official_register_pdf_btn")
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("معاينة وتصدير ($selectedRegisterKind) بصيغة PDF", fontWeight = FontWeight.ExtraBold)
                            }

                            OutlinedButton(
                                onClick = {
                                    exportMinistryOfficialCsv(
                                        context = context,
                                        filePrefix = "School_Register",
                                        headers = tableHeaders,
                                        rows = tableRows
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_official_register_csv_btn")
                            ) {
                                Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير السجل بصيغة جدول (Excel CSV)", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 1: نظام إنذارات الغياب القانونية (مع منع ظهور إنذار لمن غيابه 0 + تحكم مرن للمدير)
            // -----------------------------------------------------------------
            1 -> {
                val studentsReachingWarning = students.filter {
                    viewModel.getTotalAbsenceDays(it.id) >= firstWarningDays
                }.sortedByDescending { viewModel.getTotalAbsenceDays(it.id) }

                val displayedWarningStudents = if (showOnlyStudentsWithWarnings) {
                    studentsReachingWarning
                } else {
                    students.sortedByDescending { viewModel.getTotalAbsenceDays(it.id) }
                }

                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.5.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "نظام إنذارات الغياب القانونية (قابل للتخصيص من مدير المدرسة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Flexible Principal Controls (+/-) for Absence Warning Thresholds (Package 3)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFFECACA)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "تخصيص حدود أيام الغياب لإصدار الإنذارات الرسمية:",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF991B1B)
                                    )
                                    ThresholdStepperRow(
                                        label = "عتبة الإنذار الأولي:",
                                        valueText = "$firstWarningDays أيام غياب",
                                        onDecrement = { firstWarningDays = (firstWarningDays - 1).coerceAtLeast(1) },
                                        onIncrement = {
                                            firstWarningDays = (firstWarningDays + 1).coerceAtMost(finalWarningDays - 1)
                                        }
                                    )
                                    ThresholdStepperRow(
                                        label = "عتبة الإنذار النهائي:",
                                        valueText = "$finalWarningDays يوم غياب",
                                        onDecrement = {
                                            finalWarningDays = (finalWarningDays - 1).coerceAtLeast(firstWarningDays + 1)
                                        },
                                        onIncrement = {
                                            finalWarningDays = (finalWarningDays + 1).coerceAtMost(dismissalWarningDays - 1)
                                        }
                                    )
                                    ThresholdStepperRow(
                                        label = "الحد القانوني (فصل بالغياب):",
                                        valueText = "$dismissalWarningDays يوم غياب",
                                        onDecrement = {
                                            dismissalWarningDays = (dismissalWarningDays - 1).coerceAtLeast(finalWarningDays + 1)
                                        },
                                        onIncrement = { dismissalWarningDays = (dismissalWarningDays + 1).coerceAtMost(90) }
                                    )

                                    Button(
                                        onClick = {
                                            onSaveThresholds(
                                                thresholds.copy(
                                                    firstWarningDays = firstWarningDays,
                                                    finalWarningDays = finalWarningDays,
                                                    dismissalWarningDays = dismissalWarningDays
                                                )
                                            )
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("save_absence_warning_thresholds_btn")
                                    ) {
                                        Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حفظ واعتماد ضوابط الغياب والإنذارات", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilterChip(
                                    selected = showOnlyStudentsWithWarnings,
                                    onClick = { showOnlyStudentsWithWarnings = true },
                                    label = {
                                        Text(
                                            "المستحقون للإنذار فقط (${studentsReachingWarning.size})",
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = !showOnlyStudentsWithWarnings,
                                    onClick = { showOnlyStudentsWithWarnings = false },
                                    label = {
                                        Text(
                                            "جميع الطلبة (${students.size})",
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                if (displayedWarningStudents.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(34.dp)
                                )
                                Text(
                                    text = "جميع الطلبة ملتزمون بالدوام الرسمي — لا يوجد أي طالب تجاوز عتبة الإنذار الأولي ($firstWarningDays أيام)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF042F24),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "اضغط على (جميع الطلبة) أعلاه لعرض سجل الانضباط الكامل للطلبة الملتزمين.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(
                        displayedWarningStudents,
                        key = { it.id }
                    ) { stu ->
                        val absDays = viewModel.getTotalAbsenceDays(stu.id)
                        val hasWarning = absDays >= firstWarningDays
                        val warningLevel = when {
                            absDays >= dismissalWarningDays -> "تجاوز الحد القانوني (فصل بالغياب)"
                            absDays >= finalWarningDays -> "إنذار غياب نهائي"
                            absDays >= firstWarningDays -> "إنذار غياب أولي"
                            absDays == 0 -> "ملتزم بالدوام - 0 غياب - لا يوجد إنذار"
                            else -> "ملتزم بالدوام ($absDays غياب فقط — دون حد الإنذار)"
                        }
                        val badgeColor = when {
                            absDays >= dismissalWarningDays -> DangerRed
                            absDays >= finalWarningDays -> Color(0xFFD97706)
                            absDays >= firstWarningDays -> Color(0xFFB45309)
                            else -> Color(0xFF059669)
                        }

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.55f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${stu.fullNameWithFather} — ${stu.gradeClass} (شعبة ${stu.sectionName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF042F24),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Text(
                                    text = if (hasWarning) {
                                        "مجموع الغيابات المسجلة: ($absDays) يوم • الحالة القانونية: $warningLevel"
                                    } else {
                                        warningLevel
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeColor,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // Only show Warning Dispatch & Official Warning Print buttons if the student actually reached the warning threshold!
                                if (hasWarning) {
                                    Button(
                                        onClick = {
                                            val msgTitle = "كتاب رسمي: $warningLevel — ${stu.studentName}"
                                            val msgBody = "إلى / ولي أمر الطالب (${stu.fullNameWithFather}) المحترم — الصف والشعبة (${stu.gradeClass} - ${stu.sectionName}).. تحية طيبة، نود إعلامكم بأن ولدكم قد تغيب عن الدوام الرسمي لمدة ($absDays) أيام، وبذلك يصدر بحقه ($warningLevel) وفق تعليمات الانضباط المدرسي. يرجى مراجعة إدارة المدرسة فوراً."
                                            viewModel.pushNotification(
                                                title = msgTitle,
                                                body = msgBody,
                                                type = NotificationType.ATTENDANCE,
                                                targetStudentId = stu.id,
                                                targetRole = UserRole.PARENT.name,
                                                isUrgent = true,
                                                priority = "high"
                                            )
                                            viewModel.sendParentReport(
                                                student = stu,
                                                reportType = ReportType.PARENT_SUMMONS,
                                                title = msgTitle,
                                                details = msgBody,
                                                senderName = "اللجنة الانضباطية — المدير ($principalName)"
                                            )
                                            onTriggerBanner("تم تحديث الحالة وإشعار ولي الأمر بنجاح ✓", true)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed, contentColor = Color.White),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("إرسال الإنذار لولي الأمر فوراً", fontWeight = FontWeight.ExtraBold)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val letterParagraph = "إلى / ولي أمر الطالب (${stu.fullNameWithFather}) المحترم — الصف والشعبة (${stu.gradeClass} / ${stu.sectionName}).. تحية طيبة، نود إعلامكم بأن ولدكم قد تغيب عن الدوام الرسمي لمدة ($absDays) أيام بدون عذر مشروع، وعليه تقرر توجيه ($warningLevel) استناداً إلى ضوابط الانضباط المدرسي. يرجى حضوركم إلى إدارة المدرسة خلال (48) ساعة."
                                            exportMinistryOfficialTablePdf(
                                                context = context,
                                                schoolName = schoolConfig.schoolName,
                                                principalName = principalName,
                                                academicYear = schoolConfig.academicYear,
                                                documentTitle = "إنذار غياب رسمي ($warningLevel)",
                                                subtitleInfo = "موجه إلى السيد ولي أمر الطالب (${stu.fullNameWithFather}) المحترم",
                                                officialLetterBody = letterParagraph,
                                                summaryPills = listOf(
                                                    "أيام الغياب" to "$absDays يوم",
                                                    "نوع الإنذار" to warningLevel,
                                                    "ولي الأمر" to stu.parentName.ifBlank { "ولي الأمر" }
                                                ),
                                                headers = listOf("اسم الطالب الثلاثي", "الصف والشعبة", "أيام الغياب", "الإجراء القانوني المترتب"),
                                                rows = listOf(
                                                    listOf(
                                                        stu.fullNameWithFather,
                                                        "${stu.gradeClass} (${stu.sectionName})",
                                                        "$absDays يوم",
                                                        warningLevel
                                                    )
                                                ),
                                                footerNote = "يرجى حضور ولي الأمر إلى إدارة المدرسة خلال 48 ساعة لضمان عدم تعرض الطالب للفصل بالغياب",
                                                schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                                onArchivedRecordCreated = onSaveCorrespondence
                                            ) { ok, msg ->
                                                onTriggerBanner(msg, ok)
                                            }
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("طباعة كتاب الإنذار الرسمي الورقي A4 (PDF)", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2: سجل الصادرة والواردة والأوامر الإدارية (مع تقويم منبثق + إرفاق صورة كتاب رسمي + ترقيم تسلسلي تلقائي)
            // -----------------------------------------------------------------
            2 -> {
                val activeSchoolId = schoolConfig.schoolId.ifBlank { "default_school" }
                item {
                    val nextSeqPreview = remember(correspondenceList) {
                        MinistryDataPersistenceManager.peekNextDocNumber(context, activeSchoolId)
                    }
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "سجل الصادرة والواردة والأوامر الإدارية المدرسية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("صادر", "وارد", "أمر إداري").forEach { t ->
                                    FilterChip(
                                        selected = corType == t,
                                        onClick = { corType = t },
                                        label = { Text(t, fontWeight = FontWeight.ExtraBold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = corNumber,
                                    onValueChange = { corNumber = it },
                                    label = { Text("رقم الكتاب (التلقائي: إداري / $nextSeqPreview)") },
                                    placeholder = { Text("اتركه فارغاً للترقيم التسلسلي (إداري / $nextSeqPreview)") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )

                                // Package 5: Interactive Calendar Date Picker Field
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { showCorDatePickerDialog = true }
                                        .testTag("correspondence_date_picker_field")
                                ) {
                                    OutlinedTextField(
                                        value = corDate,
                                        onValueChange = {},
                                        readOnly = true,
                                        enabled = false,
                                        label = { Text("التاريخ (اضغط للاختيار)") },
                                        trailingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarMonth,
                                                contentDescription = "اختيار التاريخ من التقويم",
                                                tint = Color(0xFF059669)
                                            )
                                        },
                                        colors = OutlinedTextFieldDefaults.colors(
                                            disabledTextColor = Color(0xFF042F24),
                                            disabledBorderColor = Color(0xFF059669),
                                            disabledLabelColor = Color(0xFF065F46),
                                            disabledTrailingIconColor = Color(0xFF059669)
                                        ),
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = corEntity,
                                onValueChange = { corEntity = it },
                                label = { Text("الجهة الصادر إليها / الوارد منها") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = corSubject,
                                onValueChange = { corSubject = it },
                                label = { Text("موضوع الكتاب الرسمي") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = corNotes,
                                onValueChange = { corNotes = it },
                                label = { Text("الملاحظات والهامش الإداري") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Package 5: Attach Scanned Official Document Image
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF0FDF4),
                                border = BorderStroke(1.dp, Color(0xFF10B981)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "إرفاق صورة ضوئية للكتاب الرسمي (اختياري)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF042F24)
                                            )
                                            Text(
                                                text = "حفظ صورة الكتاب الوارد أو الصادر في الأرشيف وطباعتها في PDF",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF065F46)
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { correspondenceImageLauncher.launch("image/*") },
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.testTag("attach_correspondence_image_btn")
                                        ) {
                                            Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(5.dp))
                                            Text(
                                                text = if (corImageBase64.isBlank()) "اختيار صورة" else "تغيير الصورة",
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }

                                    val previewBmp = remember(corImageBase64) { decodeBase64ToBitmapSafe(corImageBase64) }
                                    if (previewBmp != null) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    lightboxTitle = corSubject.ifBlank { "معاينة الصورة الضوئية للكتاب الرسمي" }
                                                    lightboxImageBase64 = corImageBase64
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Image(
                                                    bitmap = previewBmp.asImageBitmap(),
                                                    contentDescription = "معاينة صورة الكتاب الرسمي",
                                                    modifier = Modifier
                                                        .size(68.dp)
                                                        .clip(RoundedCornerShape(8.dp)),
                                                    contentScale = ContentScale.Crop
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "تم إرفاق الصورة الضوئية للكتاب الرسمي ✓",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF065F46)
                                                    )
                                                    Text(
                                                        text = "اضغط على المصغّر لفتح العارض المكبّر بملء الشاشة",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color(0xFF047857)
                                                    )
                                                }
                                                IconButton(onClick = { corImageBase64 = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "حذف المرفق",
                                                        tint = DangerRed
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    if (corSubject.isBlank()) {
                                        onTriggerBanner("يرجى إدخال موضوع الكتاب لحفظه في الأرشيف الرسمي", false)
                                        return@Button
                                    }
                                    val finalNumber = corNumber.trim().ifBlank {
                                        MinistryDataPersistenceManager.consumeNextDocNumberFormatted(context, activeSchoolId)
                                    }
                                    val newRec = OfficialCorrespondenceRecord(
                                        type = corType,
                                        bookNumber = finalNumber,
                                        dateIso = corDate.trim().ifBlank { ArabicCalendarDateHelper.todayIsoDate() },
                                        entityName = corEntity.trim().ifBlank { "المديرية العامة للتربية" },
                                        subject = corSubject.trim(),
                                        notes = corNotes.trim(),
                                        imageBase64 = corImageBase64
                                    )
                                    onSaveCorrespondence(listOf(newRec) + correspondenceList)
                                    corNumber = ""
                                    corSubject = ""
                                    corNotes = ""
                                    corImageBase64 = ""
                                    onTriggerBanner("تم تسجيل وأرشفة الكتاب الرسمي برقم ($finalNumber) في السجل بنجاح ✓", true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("add_correspondence_record_btn")
                            ) {
                                Icon(Icons.Filled.PostAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تسجيل وأرشفة الكتاب الرسمي", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                item {
                    val filteredCor = correspondenceList.filter {
                        corSearch.isBlank() ||
                            it.bookNumber.contains(corSearch, ignoreCase = true) ||
                            it.subject.contains(corSearch, ignoreCase = true) ||
                            it.entityName.contains(corSearch, ignoreCase = true) ||
                            it.dateIso.contains(corSearch, ignoreCase = true)
                    }

                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = corSearch,
                                onValueChange = { corSearch = it },
                                label = { Text("بحث فوري برقم الكتاب أو الموضوع أو التاريخ أو الجهة...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            if (filteredCor.isEmpty()) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "سجل الصادرة والواردة فارغ حالياً — يتم أرشفة كل كتاب رسمي صادر أو مطبوع تلقائياً هنا مع رقمه الإداري المتسلسل.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF475569),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(14.dp)
                                    )
                                }
                            } else {
                                filteredCor.forEach { item ->
                                    val itemBmp = remember(item.imageBase64) { decodeBase64ToBitmapSafe(item.imageBase64) }
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "[${item.type}] رقم الكتاب: ${item.bookNumber} • التاريخ: ${item.dateIso}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF042F24),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        onSaveCorrespondence(correspondenceList.filterNot { it.id == item.id })
                                                    },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteOutline,
                                                        contentDescription = "حذف القيد",
                                                        tint = DangerRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "الجهة: ${item.entityName} • الموضوع: ${item.subject}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (item.notes.isNotBlank()) {
                                                Text(
                                                    text = "الهامش الإداري: ${item.notes}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF065F46),
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                            if (itemBmp != null) {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            lightboxTitle = "الكتاب الرسمي (${item.bookNumber}) — ${item.subject}"
                                                            lightboxImageBase64 = item.imageBase64
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                    ) {
                                                        Image(
                                                            bitmap = itemBmp.asImageBitmap(),
                                                            contentDescription = "نسخة ضوئية للكتاب",
                                                            modifier = Modifier
                                                                .size(56.dp)
                                                                .clip(RoundedCornerShape(8.dp)),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "نسخة ضوئية مرفقة للكتاب الرسمي",
                                                                style = MaterialTheme.typography.labelMedium,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = Color(0xFF042F24)
                                                            )
                                                            Text(
                                                                text = "اضغط لمعاينة الوثيقة بملء الشاشة",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = Color(0xFF059669)
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            OutlinedButton(
                                                onClick = {
                                                    exportMinistryOfficialTablePdf(
                                                        context = context,
                                                        schoolName = schoolConfig.schoolName,
                                                        principalName = principalName,
                                                        academicYear = schoolConfig.academicYear,
                                                        documentTitle = item.subject,
                                                        subtitleInfo = "نوع الوثيقة: كتاب (${item.type}) — الجهة المعنية: ${item.entityName}",
                                                        bookNumberLabel = item.bookNumber,
                                                        officialLetterBody = "إلى / ${item.entityName}.. تحية طيبة، إشارة إلى كتابكم / مذكرتنا الإدارية المرقمة (${item.bookNumber}) بتاريخ (${item.dateIso}) والخاصة بموضوع (${item.subject})، ندرج لكم أدناه خلاصة القيد والهامش الإداري المعتمد: ${item.notes.ifBlank { "للتفضل بالاطلاع واتخاذ ما يلزم أصولياً." }}",
                                                        summaryPills = listOf(
                                                            "نوع الكتاب" to item.type,
                                                            "العدد" to item.bookNumber,
                                                            "التاريخ" to item.dateIso
                                                        ),
                                                        headers = listOf("النوع", "رقم الكتاب", "التاريخ", "الجهة", "الموضوع والهامش"),
                                                        rows = listOf(
                                                            listOf(item.type, item.bookNumber, item.dateIso, item.entityName, "${item.subject} (${item.notes})")
                                                        ),
                                                        attachedImageBase64 = item.imageBase64,
                                                        schoolId = activeSchoolId,
                                                        autoArchiveToCorrespondence = false
                                                    ) { ok, msg ->
                                                        onTriggerBanner(msg, ok)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = if (item.imageBase64.isNotBlank()) {
                                                        "طباعة الكتاب الرسمي مع الصورة المرفقة (PDF)"
                                                    } else {
                                                        "طباعة الكتاب الرسمي الورقي A4 (PDF)"
                                                    },
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "سجل الصادرة والواردة والأوامر الإدارية المدرسية",
                                        subtitleInfo = "إجمالي الكتب المؤرشفة: (${filteredCor.size}) كتاب رسمي",
                                        summaryPills = listOf(
                                            "الكتب الصادرة" to "${filteredCor.count { it.type == "صادر" }}",
                                            "الكتب الواردة" to "${filteredCor.count { it.type == "وارد" }}",
                                            "الأوامر الإدارية" to "${filteredCor.count { it.type == "أمر إداري" }}"
                                        ),
                                        headers = listOf("النوع", "رقم الكتاب", "التاريخ", "الجهة", "الموضوع"),
                                        rows = filteredCor.map {
                                            listOf(it.type, it.bookNumber, it.dateIso, it.entityName, it.subject)
                                        },
                                        attachedImageBase64 = filteredCor.firstOrNull { it.imageBase64.isNotBlank() }?.imageBase64.orEmpty(),
                                        schoolId = activeSchoolId,
                                        autoArchiveToCorrespondence = false
                                    ) { ok, msg ->
                                        onTriggerBanner(msg, ok)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF042F24),
                                    contentColor = Color(0xFFFDE68A)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة سجل الصادرة والواردة الرسمي (PDF)", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 7. SECTION 3: STAFF HR & PORTFOLIO (شؤون الملاك والإضبارة الإلكترونية)
// =========================================================================

@Composable
private fun StaffHrAndPortfolioSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    teachers: List<UserAccount>,
    portfoliosMap: Map<String, StaffPortfolioRecord>,
    onSavePortfolios: (Map<String, StaffPortfolioRecord>) -> Unit,
    onCorrespondenceAutoUpdated: (List<OfficialCorrespondenceRecord>) -> Unit = {},
    onTriggerBanner: (String, Boolean) -> Unit
) {
    val currentYearInt = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var editingTeacher by remember { mutableStateOf<UserAccount?>(null) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var staffSearchQuery by remember { mutableStateOf("") }

    val filteredTeachers = remember(teachers, portfoliosMap, staffSearchQuery) {
        val q = staffSearchQuery.trim()
        if (q.isBlank()) {
            teachers
        } else {
            teachers.filter { t ->
                val p = portfoliosMap[t.id]
                t.fullName.contains(q, ignoreCase = true) ||
                    t.specialty.contains(q, ignoreCase = true) ||
                    (p?.jobTitle?.contains(q, ignoreCase = true) == true) ||
                    (p?.qualification?.contains(q, ignoreCase = true) == true) ||
                    (p?.adminOrderNumber?.contains(q, ignoreCase = true) == true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF042F24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "شؤون الملاك والإضبارة الوظيفية وحاسبة الخدمة التربوية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "إجمالي الكادر التدريسي الفعلي: (${teachers.size}) معلم/مدرس",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val rows = filteredTeachers.mapIndexed { idx, t ->
                                val p = portfoliosMap[t.id] ?: StaffPortfolioRecord(t.id)
                                val startYear = p.startDateIso.take(4).toIntOrNull()
                                val yearsServedText = if (startYear != null) {
                                    "${(currentYearInt - startYear).coerceAtLeast(1)} سنة (${p.startDateIso})"
                                } else {
                                    "غير محدد"
                                }
                                listOf(
                                    "${idx + 1}",
                                    t.fullName,
                                    t.specialty.ifBlank { "غير محدد" },
                                    p.qualification.ifBlank { "غير محدد" },
                                    p.jobTitle.ifBlank { "غير محدد" },
                                    yearsServedText
                                )
                            }
                            exportMinistryOfficialTablePdf(
                                context = context,
                                schoolName = schoolConfig.schoolName,
                                principalName = principalName,
                                academicYear = schoolConfig.academicYear,
                                documentTitle = "خلاصة الخدمة التربوية والإضبارة الوظيفية للملاك التدريسي",
                                subtitleInfo = "إجمالي الكادر التدريسي الفعلي: (${filteredTeachers.size}) معلم/مدرس",
                                summaryPills = listOf(
                                    "إجمالي الملاك" to "${filteredTeachers.size} معلم",
                                    "العام الدراسي" to schoolConfig.academicYear
                                ),
                                headers = listOf("ت", "الاسم الكامل", "الاختصاص", "الشهادة", "العنوان الوظيفي", "سنوات الخدمة"),
                                rows = rows,
                                schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                onArchivedRecordCreated = onCorrespondenceAutoUpdated
                            ) { ok, msg ->
                                onTriggerBanner(msg, ok)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color(0xFF042F24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_staff_service_summary_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير خلاصة الخدمة للملاك (PDF)", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = staffSearchQuery,
                onValueChange = { staffSearchQuery = it },
                label = { Text("بحث سريع باسم المعلم أو الاختصاص أو العنوان الوظيفي...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (staffSearchQuery.isNotBlank()) {
                        IconButton(onClick = { staffSearchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("staff_hr_search_input")
            )
        }

        items(filteredTeachers, key = { it.id }) { teacher ->
            val portfolio = portfoliosMap[teacher.id] ?: StaffPortfolioRecord(teacherId = teacher.id)
            val unsetLabel = "غير محدد — اضغط على تعديل الإضبارة لإدخاله"
            val qualificationText = portfolio.qualification.ifBlank { unsetLabel }
            val jobTitleText = portfolio.jobTitle.ifBlank { unsetLabel }
            val jobGradeText = portfolio.jobGradeStage.ifBlank { unsetLabel }
            val startDateText = portfolio.startDateIso.ifBlank { unsetLabel }
            val adminOrderText = portfolio.adminOrderNumber.ifBlank { unsetLabel }

            val startYear = portfolio.startDateIso.take(4).toIntOrNull()
            val serviceYearsLabel = if (startYear != null) {
                "${(currentYearInt - startYear).coerceAtLeast(1)} سنة"
            } else {
                "غير محدد"
            }
            val nextAllowanceDate = if (portfolio.startDateIso.length >= 10) {
                "${currentYearInt + 1}-${portfolio.startDateIso.drop(5)}"
            } else {
                "يُحسب بعد إدخال تاريخ المباشرة"
            }
            val remainingToRetirement = if (teacher.ageYears > 0) {
                "${(60 - teacher.ageYears).coerceAtLeast(1)} سنة"
            } else {
                "حسب السن القانوني"
            }
            val assignedClassesDisplay = teacher.assignedClasses.trim().ifBlank { unsetLabel }

            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${teacher.fullName} — الاختصاص: ${teacher.specialty.ifBlank { unsetLabel }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "الشهادة: $qualificationText • العنوان الوظيفي: $jobTitleText",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "الدرجة والمرحلة: $jobGradeText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "تاريخ المباشرة: $startDateText • الأمر الإداري: $adminOrderText",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "حاسبة الخدمة التربوية ومصفوفة الصلاحيات:",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24)
                            )
                            Text(
                                text = "• الخدمة الفعلية: ($serviceYearsLabel)  |  العلاوة السنوية القادمة: ($nextAllowanceDate)  |  المتبقي للتقاعد: ($remainingToRetirement)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "• الصفوف والشعب المخولة لاستلام الدرجات والواجبات: ${if (assignedClassesDisplay == unsetLabel) unsetLabel else teacher.formattedAssignedClassesForDisplay}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF047857)
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { editingTeacher = teacher },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تعديل الإضبارة الوظيفية ومصفوفة صلاحيات المعلم", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    editingTeacher?.let { teacher ->
        val existingPortfolio = portfoliosMap[teacher.id] ?: StaffPortfolioRecord(teacher.id)
        var specialtyDraft by remember(teacher.id) { mutableStateOf(teacher.specialty) }
        var assignedClassesDraft by remember(teacher.id) { mutableStateOf(teacher.assignedClasses) }
        var qualificationDraft by remember(teacher.id) { mutableStateOf(existingPortfolio.qualification) }
        var startDateDraft by remember(teacher.id) { mutableStateOf(existingPortfolio.startDateIso) }
        var jobTitleDraft by remember(teacher.id) { mutableStateOf(existingPortfolio.jobTitle) }
        var gradeStageDraft by remember(teacher.id) { mutableStateOf(existingPortfolio.jobGradeStage) }
        var orderNumDraft by remember(teacher.id) { mutableStateOf(existingPortfolio.adminOrderNumber) }

        if (showStartDatePicker) {
            MinistryCalendarDatePickerDialog(
                initialDateIso = startDateDraft.ifBlank { ArabicCalendarDateHelper.todayIsoDate() },
                onDismiss = { showStartDatePicker = false },
                onDateSelected = { selectedIso ->
                    startDateDraft = selectedIso
                    showStartDatePicker = false
                }
            )
        }

        AlertDialog(
            onDismissRequest = { editingTeacher = null },
            title = {
                Text(
                    text = "إضبارة وصلاحيات: ${teacher.fullName}",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = specialtyDraft,
                        onValueChange = { specialtyDraft = it },
                        label = { Text("المواد والاختصاص المسموح رصد درجاته") },
                        placeholder = { Text("مثال: اللغة العربية والتربية الإسلامية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = assignedClassesDraft,
                        onValueChange = { assignedClassesDraft = it },
                        label = { Text("الصفوف والشعب المخصصة (مفصولة بـ |)") },
                        placeholder = { Text("مثال: الأول الابتدائي|الثاني الابتدائي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = qualificationDraft,
                        onValueChange = { qualificationDraft = it },
                        label = { Text("الشهادة والتحصيل الدراسي") },
                        placeholder = { Text("مثال: بكالوريوس تربية / دبلوم معلمين") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showStartDatePicker = true }
                    ) {
                        OutlinedTextField(
                            value = startDateDraft,
                            onValueChange = {},
                            readOnly = true,
                            enabled = false,
                            label = { Text("تاريخ المباشرة بالخدمة (اضغط للتقويم)") },
                            placeholder = { Text("اضغط لاختيار تاريخ المباشرة") },
                            trailingIcon = {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF059669))
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = Color(0xFF042F24),
                                disabledBorderColor = Color(0xFF059669),
                                disabledLabelColor = Color(0xFF065F46)
                            ),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    OutlinedTextField(
                        value = jobTitleDraft,
                        onValueChange = { jobTitleDraft = it },
                        label = { Text("العنوان الوظيفي") },
                        placeholder = { Text("مثال: معلم جامعي / مدرس أول") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gradeStageDraft,
                        onValueChange = { gradeStageDraft = it },
                        label = { Text("الدرجة والمرحلة الوظيفية") },
                        placeholder = { Text("مثال: الدرجة الخامسة / المرحلة الثانية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = orderNumDraft,
                        onValueChange = { orderNumDraft = it },
                        label = { Text("رقم الأمر الإداري") },
                        placeholder = { Text("مثال: أمر إداري / 215") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateTeacherAccount(
                            teacherId = teacher.id,
                            fullName = teacher.fullName,
                            specialty = specialtyDraft.trim(),
                            assignedClasses = assignedClassesDraft.trim(),
                            phone = teacher.phone,
                            username = teacher.username,
                            password = teacher.password
                        )
                        val updatedMap = portfoliosMap.toMutableMap().apply {
                            put(
                                teacher.id,
                                StaffPortfolioRecord(
                                    teacherId = teacher.id,
                                    qualification = qualificationDraft.trim(),
                                    startDateIso = startDateDraft.trim(),
                                    jobTitle = jobTitleDraft.trim(),
                                    jobGradeStage = gradeStageDraft.trim(),
                                    adminOrderNumber = orderNumDraft.trim()
                                )
                            )
                        }
                        onSavePortfolios(updatedMap)
                        editingTeacher = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("حفظ الإضبارة والصلاحيات", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTeacher = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// =========================================================================
// 8. SECTION 4: STUDENT WELFARE, ORPHANS & SOCIAL GRANT
// =========================================================================

@Composable
private fun StudentWelfareAndOrphansSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    students: List<StudentRecord>,
    welfareMap: Map<String, StudentWelfareRecord>,
    onSaveWelfare: (Map<String, StudentWelfareRecord>) -> Unit,
    onCorrespondenceAutoUpdated: (List<OfficialCorrespondenceRecord>) -> Unit = {},
    onTriggerBanner: (String, Boolean) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("الكل") }
    var editingStudent by remember { mutableStateOf<StudentRecord?>(null) }

    val orphansCount = students.count { (welfareMap[it.id]?.orphanStatus ?: "غير يتيم") != "غير يتيم" }
    val grantCount = students.count { welfareMap[it.id]?.isSocialSecurityGrant == true }
    val specialCount = students.count { welfareMap[it.id]?.isSpecialNeedsOrLowIncome == true }

    val filteredStudents = students.filter { stu ->
        val w = welfareMap[stu.id]
        when (selectedFilter) {
            "الأيتام" -> (w?.orphanStatus ?: "غير يتيم") != "غير يتيم"
            "مشمولو المنحة والرعاية" -> w?.isSocialSecurityGrant == true
            "ذوو الاحتياجات والمتعففون" -> w?.isSpecialNeedsOrLowIncome == true
            else -> true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF042F24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "منظومة الرعاية الطلابية والأيتام والمنحة الحكومية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinistryMetricPill("الأيتام المسجلون", "$orphansCount طالب", Modifier.weight(1f))
                        MinistryMetricPill("مشمولو المنحة", "$grantCount طالب", Modifier.weight(1f))
                        MinistryMetricPill("احتياجات ومتعففون", "$specialCount طالب", Modifier.weight(1f))
                    }

                    Button(
                        onClick = {
                            val rows = filteredStudents.mapIndexed { idx, stu ->
                                val w = welfareMap[stu.id] ?: StudentWelfareRecord(stu.id)
                                listOf(
                                    "${idx + 1}",
                                    stu.fullNameWithFather,
                                    "${stu.gradeClass} (${stu.sectionName})",
                                    w.orphanStatus,
                                    if (w.isSocialSecurityGrant) "مشمول (${w.socialSecurityNumber.ifBlank { "منحة" }})" else "غير مشمول",
                                    if (w.isSpecialNeedsOrLowIncome) "نعم" else "—"
                                )
                            }
                            exportMinistryOfficialTablePdf(
                                context = context,
                                schoolName = schoolConfig.schoolName,
                                principalName = principalName,
                                academicYear = schoolConfig.academicYear,
                                documentTitle = "القوائم الرسمية للأيتام ومشمولي المنحة والرعاية الاجتماعية",
                                subtitleInfo = "معدة للرفع الرسمي إلى المديرية العامة للتربية / قسم الرعاية الاجتماعية",
                                summaryPills = listOf(
                                    "الأيتام" to "$orphansCount",
                                    "مشمولو المنحة" to "$grantCount",
                                    "المتعففون" to "$specialCount"
                                ),
                                headers = listOf("ت", "اسم الطالب", "الصف والشعبة", "حالة اليتيم", "المنحة والرعاية", "احتياجات خاصة"),
                                rows = rows,
                                schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                onArchivedRecordCreated = onCorrespondenceAutoUpdated
                            ) { ok, msg ->
                                onTriggerBanner(msg, ok)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color(0xFF042F24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_welfare_orphans_pdf_btn")
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير قوائم الأيتام ومشمولي المنحة لوزارة التربية (PDF)", fontWeight = FontWeight.ExtraBold)
                    }

                    OutlinedButton(
                        onClick = {
                            val rows = filteredStudents.mapIndexed { idx, stu ->
                                val w = welfareMap[stu.id] ?: StudentWelfareRecord(stu.id)
                                listOf(
                                    "${idx + 1}",
                                    stu.fullNameWithFather,
                                    stu.gradeClass,
                                    stu.sectionName,
                                    w.orphanStatus,
                                    if (w.isSocialSecurityGrant) "مشمول" else "غير مشمول",
                                    w.socialSecurityNumber,
                                    if (w.isSpecialNeedsOrLowIncome) "نعم" else "لا",
                                    stu.parentPhone
                                )
                            }
                            exportMinistryOfficialCsv(
                                context = context,
                                filePrefix = "School_Orphans_And_Grant",
                                headers = listOf("ت", "اسم الطالب", "الصف", "الشعبة", "حالة اليتيم", "المنحة الطلابية", "رقم البطاقة الذكية/الرعاية", "ذوي احتياجات/متعفف", "هاتف ولي الأمر"),
                                rows = rows
                            ) { ok, msg ->
                                onTriggerBanner(msg, ok)
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("export_welfare_orphans_csv_btn")
                    ) {
                        Icon(Icons.Filled.TableView, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تصدير قوائم الأيتام والمنحة (Excel CSV)", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("الكل", "الأيتام", "مشمولو المنحة والرعاية", "ذوو الاحتياجات والمتعففون")) { f ->
                    FilterChip(
                        selected = selectedFilter == f,
                        onClick = { selectedFilter = f },
                        label = { Text(f, fontWeight = FontWeight.Bold) }
                    )
                }
            }
        }

        items(filteredStudents, key = { it.id }) { stu ->
            val w = welfareMap[stu.id] ?: StudentWelfareRecord(stu.id)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${stu.fullNameWithFather} — ${stu.gradeClass} (شعبة ${stu.sectionName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "حالة اليُتم: ${w.orphanStatus}  |  المنحة الاجتماعية: ${if (w.isSocialSecurityGrant) "مشمول (${w.monthlyGrantIqd} د.ع)" else "غير مشمول"}  |  احتياجات خاصة/متعفف: ${if (w.isSpecialNeedsOrLowIncome) "نعم" else "لا"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedButton(
                        onClick = { editingStudent = stu },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تأشير وتحديث بيانات الرعاية والمنحة للطالب", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    editingStudent?.let { stu ->
        val existing = welfareMap[stu.id] ?: StudentWelfareRecord(stu.id)
        var orphanDraft by remember(stu.id) { mutableStateOf(existing.orphanStatus) }
        var grantDraft by remember(stu.id) { mutableStateOf(existing.isSocialSecurityGrant) }
        var specialDraft by remember(stu.id) { mutableStateOf(existing.isSpecialNeedsOrLowIncome) }
        var ssnDraft by remember(stu.id) { mutableStateOf(existing.socialSecurityNumber) }
        var notesDraft by remember(stu.id) { mutableStateOf(existing.welfareNotes) }

        AlertDialog(
            onDismissRequest = { editingStudent = null },
            title = { Text("تصنيف الرعاية والمنحة: ${stu.studentName}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("1. حالة اليُتم:", fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(listOf("غير يتيم", "يتيم الأب", "يتيم الأم", "يتيم الأبوين")) { st ->
                            FilterChip(
                                selected = orphanDraft == st,
                                onClick = { orphanDraft = st },
                                label = { Text(st) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("مشمول بالرعاية الاجتماعية (المنحة الطلابية)", modifier = Modifier.weight(1f))
                        Switch(checked = grantDraft, onCheckedChange = { grantDraft = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ذوي الاحتياجات الخاصة / المتعففين", modifier = Modifier.weight(1f))
                        Switch(checked = specialDraft, onCheckedChange = { specialDraft = it })
                    }

                    OutlinedTextField(
                        value = ssnDraft,
                        onValueChange = { ssnDraft = it },
                        label = { Text("رقم البطاقة الذكية / كتاب الرعاية الاجتماعية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notesDraft,
                        onValueChange = { notesDraft = it },
                        label = { Text("ملاحظات الباحث الاجتماعي / الإدارة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = welfareMap.toMutableMap().apply {
                            put(
                                stu.id,
                                StudentWelfareRecord(
                                    studentId = stu.id,
                                    orphanStatus = orphanDraft,
                                    isSocialSecurityGrant = grantDraft,
                                    isSpecialNeedsOrLowIncome = specialDraft,
                                    socialSecurityNumber = ssnDraft.trim(),
                                    welfareNotes = notesDraft.trim()
                                )
                            )
                        }
                        onSaveWelfare(updated)
                        editingStudent = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("حفظ التصنيف", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStudent = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

// =========================================================================
// 9. SECTION 5: YEAR-END ARCHIVING & STUDENT PROMOTION ENGINE
// =========================================================================

@Composable
private fun YearEndArchivingAndPromotionSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    students: List<StudentRecord>,
    grades: List<SubjectGrade>,
    archivedYears: List<ArchivedSchoolYearSnapshot>,
    onSaveArchivedYears: (List<ArchivedSchoolYearSnapshot>) -> Unit,
    onCorrespondenceAutoUpdated: (List<OfficialCorrespondenceRecord>) -> Unit = {},
    onTriggerBanner: (String, Boolean) -> Unit
) {
    val promotionPreview = remember(students, grades, schoolConfig.academicYear) {
        viewModel.buildAnnualPromotionPreviewReport()
    }
    var showStrictPromotionConfirmCard by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = BorderStroke(1.5.dp, Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "أولاً: محرك ترحيل الطلاب الذكي ونقل الخريجين",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "ينقل الطلاب الناجحين تلقائياً إلى الصف الدراسي الأعلى، ويبقي المعيدين في صفوفهم، ويرحّل طلاب المرحلة المنتهية إلى (سجل الخريجين).",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF065F46),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MinistryMetricPill("ناجح ومرحّل", "${promotionPreview.totalPromotedCount}", Modifier.weight(1f))
                        MinistryMetricPill("خريجو المرحلة", "${promotionPreview.graduatedCount}", Modifier.weight(1f))
                        MinistryMetricPill("معيد / قيد الرصد", "${promotionPreview.totalRepeatingCount + promotionPreview.totalPendingGradesCount}", Modifier.weight(1f))
                    }

                    if (promotionPreview.classBreakdowns.isNotEmpty()) {
                        MinistryOfficialPreviewTable(
                            title = "ملخص الترحيل حسب الصفوف الدراسية (${schoolConfig.academicYear})",
                            headers = listOf("الصف الحالي", "الصف القادم", "العدد", "ناجح", "معيد/قيد الرصد"),
                            rows = promotionPreview.classBreakdowns.map { cb ->
                                listOf(
                                    cb.gradeClass,
                                    cb.nextGradeClass,
                                    "${cb.totalStudents}",
                                    "${cb.passedCount}",
                                    "${cb.failedCount + cb.pendingGradesCount}"
                                )
                            }
                        )
                    }

                    Button(
                        onClick = { showStrictPromotionConfirmCard = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF042F24),
                            contentColor = Color(0xFFFDE68A)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_strict_promotion_confirm_btn")
                    ) {
                        Icon(Icons.Filled.Upgrade, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("معاينة وتنفيذ ترحيل العام الدراسي", fontWeight = FontWeight.ExtraBold)
                    }

                    if (showStrictPromotionConfirmCard) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.5.dp, Color(0xFFD97706)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "بطاقة التأكيد الصارم للمدير قبل تنفيذ ترحيل العام الدراسي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "سيتم ترحيل (${promotionPreview.totalPromotedCount}) طالب ناجح إلى الصف الأعلى، وتخريج (${promotionPreview.graduatedCount}) طالب إلى سجل الخريجين، وإبقاء (${promotionPreview.totalRepeatingCount}) في صفوفهم.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                                Button(
                                    onClick = {
                                        val report = viewModel.executeAnnualPromotionAndGenerateReport()
                                        showStrictPromotionConfirmCard = false
                                        if (report != null) {
                                            val snap = ArchivedSchoolYearSnapshot(
                                                academicYear = schoolConfig.academicYear,
                                                sealedDateIso = ArabicCalendarDateHelper.todayIsoDate(),
                                                sealedByPrincipal = principalName,
                                                totalStudents = report.totalEvaluatedStudents,
                                                promotedCount = report.totalPromotedCount,
                                                repeatingCount = report.totalRepeatingCount,
                                                graduatedCount = report.graduatedCount,
                                                graduatesNames = report.promotedStudents.filter { it.isGraduatedStage }.map { it.studentName },
                                                summaryNotes = "تم تنفيذ الترحيل السنوي الرسمي وختم السجلات"
                                            )
                                            onSaveArchivedYears(listOf(snap) + archivedYears)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("تأكيد واعتماد الترحيل الرسمي الآن", fontWeight = FontWeight.ExtraBold)
                                }
                                OutlinedButton(
                                    onClick = { showStrictPromotionConfirmCard = false },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("إلغاء الترحيل")
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. Seal & Archive Current Academic Year
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.2.dp, Color(0xFF059669)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ثانياً: ختم وأرشفة السنة الدراسية وسجل الخريجين (${schoolConfig.academicYear})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "حفظ نسخة أرشيفية كاملة ومختومة من بيانات العام الدراسي الحالي (${schoolConfig.academicYear}) للرجوع إليها في أي وقت.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val grads = students.filter {
                                viewModel.getNextAcademicGrade(it.gradeClass).second
                            }.map { it.fullNameWithFather }
                            val snapshot = ArchivedSchoolYearSnapshot(
                                academicYear = schoolConfig.academicYear,
                                sealedDateIso = ArabicCalendarDateHelper.todayIsoDate(),
                                sealedByPrincipal = principalName,
                                totalStudents = students.size,
                                promotedCount = promotionPreview.totalPromotedCount,
                                repeatingCount = promotionPreview.totalRepeatingCount,
                                graduatedCount = grads.size,
                                graduatesNames = grads,
                                summaryNotes = "نسخة أرشيفية مختومة للعام الدراسي ${schoolConfig.academicYear}"
                            )
                            onSaveArchivedYears(listOf(snapshot) + archivedYears)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("seal_current_academic_year_btn")
                    ) {
                        Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("ختم وأرشفة السنة الدراسية (${schoolConfig.academicYear})", fontWeight = FontWeight.ExtraBold)
                    }

                    archivedYears.forEach { arch ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "أرشيف العام الدراسي: ${arch.academicYear} • تاريخ الختم: ${arch.sealedDateIso}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF042F24)
                                )
                                Text(
                                    text = "الطلبة: ${arch.totalStudents} | الناجحون: ${arch.promotedCount} | الخريجون: ${arch.graduatedCount} | المدير المصادق: ${arch.sealedByPrincipal}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF065F46)
                                )
                                if (arch.graduatesNames.isNotEmpty()) {
                                    Text(
                                        text = "سجل الخريجين: ${arch.graduatesNames.take(8).joinToString("، ")}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF334155)
                                    )
                                }
                                OutlinedButton(
                                    onClick = {
                                        exportMinistryOfficialTablePdf(
                                            context = context,
                                            schoolName = schoolConfig.schoolName,
                                            principalName = arch.sealedByPrincipal,
                                            academicYear = arch.academicYear,
                                            documentTitle = "وثيقة الأرشيف السنوي المختوم وسجل الخريجين (${arch.academicYear})",
                                            subtitleInfo = arch.summaryNotes,
                                            summaryPills = listOf(
                                                "العام المؤرشف" to arch.academicYear,
                                                "الطلبة" to "${arch.totalStudents}",
                                                "الخريجون" to "${arch.graduatedCount}"
                                            ),
                                            headers = listOf("ت", "اسم الخريج / الطالب", "العام الدراسي", "حالة التخرج"),
                                            rows = arch.graduatesNames.ifEmpty { students.map { it.fullNameWithFather } }.mapIndexed { i, name ->
                                                listOf("${i + 1}", name, arch.academicYear, "مؤرشف ومصدق")
                                            },
                                            schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                            onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                        ) { ok, msg ->
                                            onTriggerBanner(msg, ok)
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("طباعة وثيقة الأرشيف وسجل الخريجين (PDF)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 10. SECTION 6: SCHOOL INVENTORY & EXPENSES (الجرد والحسابات والمصروفات)
// =========================================================================

@Composable
private fun SchoolInventoryAndExpensesSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    tuitions: List<TuitionRecord>,
    inventoryList: List<SchoolInventoryItemRecord>,
    onSaveInventory: (List<SchoolInventoryItemRecord>) -> Unit,
    expensesList: List<SchoolExpenseItemRecord>,
    onSaveExpenses: (List<SchoolExpenseItemRecord>) -> Unit,
    onCorrespondenceAutoUpdated: (List<OfficialCorrespondenceRecord>) -> Unit = {},
    onTriggerBanner: (String, Boolean) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }

    // Inventory Form State
    var invName by remember { mutableStateOf("") }
    var invCategory by remember { mutableStateOf("مقاعد دراسية") }
    var invQty by remember { mutableStateOf("10") }
    var invCondition by remember { mutableStateOf("صالح") }
    var invLocation by remember { mutableStateOf("القاعات الدراسية") }

    // Expense Form State
    var expAmount by remember { mutableStateOf("") }
    var expCategory by remember { mutableStateOf("قرطاسية ومطبوعات") }
    var expDate by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
    var expRecipient by remember { mutableStateOf("") }
    var expReceiptNo by remember { mutableStateOf("") }
    var expNotes by remember { mutableStateOf("") }
    var showExpDatePicker by remember { mutableStateOf(false) }

    if (showExpDatePicker) {
        MinistryCalendarDatePickerDialog(
            initialDateIso = expDate,
            onDismiss = { showExpDatePicker = false },
            onDateSelected = { selectedIso ->
                expDate = selectedIso
                showExpDatePicker = false
            }
        )
    }

    val totalTuitionRevenuesIqd = remember(tuitions) { tuitions.sumOf { it.paidAmount } }
    val totalExpensesIqd = remember(expensesList) { expensesList.sumOf { it.amountIqd } }
    val netBalanceIqd = totalTuitionRevenuesIqd - totalExpensesIqd

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = activeSubTab == 0,
                    onClick = { activeSubTab = 0 },
                    label = { Text("1. سجل جرد الأثاث والممتلكات", fontWeight = FontWeight.ExtraBold) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = activeSubTab == 1,
                    onClick = { activeSubTab = 1 },
                    label = { Text("2. سجل المصروفات والحسابات", fontWeight = FontWeight.ExtraBold) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (activeSubTab == 0) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.2.dp, Color(0xFF059669)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "سجل جرد أثاث وممتلكات المدرسة السنوي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF042F24),
                            modifier = Modifier.fillMaxWidth()
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("مقاعد دراسية", "سبورات", "أجهزة حاسوب", "مختبرات", "مبردات/مكيفات", "أثاث إداري")) { cat ->
                                FilterChip(
                                    selected = invCategory == cat,
                                    onClick = { invCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = invName,
                                onValueChange = { invName = it },
                                label = { Text("اسم المادة / الممتلكات") },
                                singleLine = true,
                                modifier = Modifier.weight(1.4f)
                            )
                            OutlinedTextField(
                                value = invQty,
                                onValueChange = { invQty = it },
                                label = { Text("العدد") },
                                singleLine = true,
                                modifier = Modifier.weight(0.6f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("صالح", "يحتاج صيانة", "مستهلك").forEach { cond ->
                                FilterChip(
                                    selected = invCondition == cond,
                                    onClick = { invCondition = cond },
                                    label = { Text(cond, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = invLocation,
                            onValueChange = { invLocation = it },
                            label = { Text("الموقع / القاعة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                if (invName.isBlank()) {
                                    onTriggerBanner("يرجى إدخال اسم المادة لإضافتها في سجل الجرد", false)
                                    return@Button
                                }
                                val item = SchoolInventoryItemRecord(
                                    itemName = invName.trim(),
                                    category = invCategory,
                                    quantity = invQty.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                                    conditionStatus = invCondition,
                                    locationRoom = invLocation.trim(),
                                    notes = "مسجل في الجرد السنوي"
                                )
                                onSaveInventory(listOf(item) + inventoryList)
                                invName = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة المادة إلى سجل الجرد", fontWeight = FontWeight.ExtraBold)
                        }

                        if (inventoryList.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "سجل الجرد فارغ حالياً — لا توجد مواد افتراضية مسبقة. أضف ممتلكات المدرسة الفعلية من النموذج أعلاه.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            MinistryOfficialPreviewTable(
                                title = "كشف الجرد السنوي لممتلكات المدرسة (${inventoryList.size} مادة)",
                                headers = listOf("المادة", "التصنيف", "العدد", "الحالة", "الموقع"),
                                rows = inventoryList.map {
                                    listOf(it.itemName, it.category, "${it.quantity}", it.conditionStatus, it.locationRoom)
                                }
                            )
                        }

                        Button(
                            onClick = {
                                exportMinistryOfficialTablePdf(
                                    context = context,
                                    schoolName = schoolConfig.schoolName,
                                    principalName = principalName,
                                    academicYear = schoolConfig.academicYear,
                                    documentTitle = "كشف الجرد السنوي الرسمي لأثاث وممتلكات المدرسة",
                                    subtitleInfo = "إجمالي الفقرات المجرودة: (${inventoryList.size}) مادة",
                                    summaryPills = listOf(
                                        "صالح للعمل" to "${inventoryList.filter { it.conditionStatus == "صالح" }.sumOf { it.quantity }}",
                                        "يحتاج صيانة" to "${inventoryList.filter { it.conditionStatus == "يحتاج صيانة" }.sumOf { it.quantity }}",
                                        "مستهلك" to "${inventoryList.filter { it.conditionStatus == "مستهلك" }.sumOf { it.quantity }}"
                                    ),
                                    headers = listOf("ت", "اسم المادة", "التصنيف", "العدد", "الحالة الفنية", "الموقع"),
                                    rows = inventoryList.mapIndexed { idx, it ->
                                        listOf("${idx + 1}", it.itemName, it.category, "${it.quantity}", it.conditionStatus, it.locationRoom)
                                    },
                                    schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                    onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                ) { ok, msg ->
                                    onTriggerBanner(msg, ok)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF042F24),
                                contentColor = Color(0xFFFDE68A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_annual_inventory_pdf_btn")
                        ) {
                            Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير كشف الجرد السنوي (PDF)", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF042F24)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "الموقف المالي العام للمدرسة (مقارنة الواردات والمصروفات)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MinistryMetricPill(
                                "إجمالي الواردات",
                                "${String.format(Locale.US, "%,d", totalTuitionRevenuesIqd)} د.ع",
                                Modifier.weight(1f)
                            )
                            MinistryMetricPill(
                                "إجمالي المصروفات",
                                "${String.format(Locale.US, "%,d", totalExpensesIqd)} د.ع",
                                Modifier.weight(1f)
                            )
                            MinistryMetricPill(
                                "الصافي المالي",
                                "${String.format(Locale.US, "%,d", netBalanceIqd)} د.ع",
                                Modifier.weight(1f)
                            )
                        }

                        Button(
                            onClick = {
                                exportMinistryOfficialTablePdf(
                                    context = context,
                                    schoolName = schoolConfig.schoolName,
                                    principalName = principalName,
                                    academicYear = schoolConfig.academicYear,
                                    documentTitle = "التقرير المالي الرسمي للمدرسة (الواردات والمصروفات التشغيلية)",
                                    subtitleInfo = "الواردات المقبوضة: ${String.format(Locale.US, "%,d", totalTuitionRevenuesIqd)} د.ع | المصروفات: ${String.format(Locale.US, "%,d", totalExpensesIqd)} د.ع | الصافي: ${String.format(Locale.US, "%,d", netBalanceIqd)} د.ع",
                                    summaryPills = listOf(
                                        "إجمالي الواردات" to "${String.format(Locale.US, "%,d", totalTuitionRevenuesIqd)} د.ع",
                                        "إجمالي المصروفات" to "${String.format(Locale.US, "%,d", totalExpensesIqd)} د.ع",
                                        "الرصيد الصافي" to "${String.format(Locale.US, "%,d", netBalanceIqd)} د.ع"
                                    ),
                                    headers = listOf("ت", "المبلغ (د.ع)", "التبويب", "التاريخ", "المستلم", "رقم الوصل"),
                                    rows = expensesList.mapIndexed { idx, ex ->
                                        listOf(
                                            "${idx + 1}",
                                            String.format(Locale.US, "%,d", ex.amountIqd),
                                            ex.category,
                                            ex.dateIso,
                                            ex.recipientName,
                                            ex.receiptNumber
                                        )
                                    },
                                    schoolId = schoolConfig.schoolId.ifBlank { "default_school" },
                                    onArchivedRecordCreated = onCorrespondenceAutoUpdated
                                ) { ok, msg ->
                                    onTriggerBanner(msg, ok)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFBBF24),
                                contentColor = Color(0xFF042F24)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_school_financial_report_pdf_btn")
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استخراج التقرير المالي للمدرسة (PDF)", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تسجيل وصل صرف تشغيلي / نثريات جديدة:",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF042F24)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("قرطاسية ومطبوعات", "صيانة وترميم", "كهرباء ووقود", "أنشطة ومختبرات", "نثريات عامة")) { cat ->
                                FilterChip(
                                    selected = expCategory == cat,
                                    onClick = { expCategory = cat },
                                    label = { Text(cat) }
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = expAmount,
                                onValueChange = { expAmount = it },
                                label = { Text("المبلغ (د.ع)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = expReceiptNo,
                                onValueChange = { expReceiptNo = it },
                                label = { Text("رقم الوصل") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showExpDatePicker = true }
                        ) {
                            OutlinedTextField(
                                value = expDate,
                                onValueChange = {},
                                readOnly = true,
                                enabled = false,
                                label = { Text("تاريخ الوصل (اضغط لاختيار التاريخ من التقويم)") },
                                trailingIcon = {
                                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = Color(0xFF059669))
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = Color(0xFF042F24),
                                    disabledBorderColor = Color(0xFF059669),
                                    disabledLabelColor = Color(0xFF065F46)
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        OutlinedTextField(
                            value = expRecipient,
                            onValueChange = { expRecipient = it },
                            label = { Text("اسم المستلم / الجهة المجهزة") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = expNotes,
                            onValueChange = { expNotes = it },
                            label = { Text("تفاصيل الصرف والغرض") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Button(
                            onClick = {
                                val amt = expAmount.replace(",", "").trim().toLongOrNull() ?: 0L
                                if (amt <= 0L) {
                                    onTriggerBanner("يرجى إدخال مبلغ صحيح بالدينار العراقي", false)
                                    return@Button
                                }
                                val rec = SchoolExpenseItemRecord(
                                    amountIqd = amt,
                                    category = expCategory,
                                    dateIso = expDate,
                                    recipientName = expRecipient.trim().ifBlank { "مجهز معتمد" },
                                    receiptNumber = expReceiptNo.trim().ifBlank { "وصل / ${100 + expensesList.size}" },
                                    notes = expNotes.trim()
                                )
                                onSaveExpenses(listOf(rec) + expensesList)
                                expAmount = ""
                                expRecipient = ""
                                expReceiptNo = ""
                                expNotes = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وصل الصرف في السجل المالي", fontWeight = FontWeight.ExtraBold)
                        }

                        if (expensesList.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "سجل المصروفات فارغ حالياً — لا توجد وصولات وهمية مسجلة مسبقاً. أضف وصولات الصرف الفعلية من النموذج أعلاه.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            MinistryOfficialPreviewTable(
                                title = "سجل المصروفات والنثريات المسجلة (${expensesList.size} وصل)",
                                headers = listOf("المبلغ", "التبويب", "التاريخ", "المستلم", "رقم الوصل"),
                                rows = expensesList.map {
                                    listOf(
                                        "${String.format(Locale.US, "%,d", it.amountIqd)} د.ع",
                                        it.category,
                                        it.dateIso,
                                        it.recipientName,
                                        it.receiptNumber
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 11. REUSABLE MINISTRY UI COMPONENTS, STEPPERS, CALENDAR & LIGHTBOX
// =========================================================================

@Composable
private fun ThresholdStepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B),
            modifier = Modifier.weight(1f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                onClick = onDecrement,
                shape = CircleShape,
                color = Color(0xFFF1F5F9),
                border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp), tint = Color(0xFF0F172A))
                }
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFF059669)),
                modifier = Modifier.widthIn(min = 84.dp)
            ) {
                Text(
                    text = valueText,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F24),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
            Surface(
                onClick = onIncrement,
                shape = CircleShape,
                color = Color(0xFFECFDF5),
                border = BorderStroke(1.dp, Color(0xFF059669)),
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp), tint = Color(0xFF042F24))
                }
            }
        }
    }
}

@Composable
private fun MinistryCalendarDatePickerDialog(
    initialDateIso: String,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }
    val parts = remember(initialDateIso) {
        val sp = initialDateIso.split("-")
        val y = sp.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.YEAR)
        val m = sp.getOrNull(1)?.toIntOrNull() ?: (Calendar.getInstance().get(Calendar.MONTH) + 1)
        val d = sp.getOrNull(2)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
        Triple(y.coerceIn(2000, 2035), m.coerceIn(1, 12), d.coerceIn(1, 31))
    }

    var selectedYear by remember { mutableIntStateOf(parts.first) }
    var selectedMonth by remember { mutableIntStateOf(parts.second) }
    var selectedDay by remember { mutableIntStateOf(parts.third) }

    val arabicMonths = remember {
        listOf(
            1 to "1 - كانون الثاني",
            2 to "2 - شباط",
            3 to "3 - آذار",
            4 to "4 - نيسان",
            5 to "5 - أيار",
            6 to "6 - حزيران",
            7 to "7 - تموز",
            8 to "8 - آب",
            9 to "9 - أيلول",
            10 to "10 - تشرين الأول",
            11 to "11 - تشرين الثاني",
            12 to "12 - كانون الأول"
        )
    }
    val maxDaysInMonth = remember(selectedYear, selectedMonth) {
        when (selectedMonth) {
            2 -> if (selectedYear % 4 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }
    if (selectedDay > maxDaysInMonth) selectedDay = maxDaysInMonth
    val previewIso = String.format(Locale.US, "%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "اختيار التاريخ من التقويم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F24)
                )
                TextButton(
                    onClick = { onDateSelected(todayIso) }
                ) {
                    Text("تاريخ اليوم", fontWeight = FontWeight.ExtraBold, color = Color(0xFF059669))
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "التاريخ المحدد: $previewIso",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(10.dp)
                    )
                }

                ThresholdStepperRow(
                    label = "السنة:",
                    valueText = "$selectedYear",
                    onDecrement = { selectedYear = (selectedYear - 1).coerceAtLeast(1990) },
                    onIncrement = { selectedYear = (selectedYear + 1).coerceAtMost(2035) }
                )

                Text(
                    text = "الشهر:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F24)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(arabicMonths) { (mNum, mLabel) ->
                        FilterChip(
                            selected = selectedMonth == mNum,
                            onClick = { selectedMonth = mNum },
                            label = {
                                Text(
                                    text = mLabel,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                Text(
                    text = "اليوم من الشهر:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F24)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..maxDaysInMonth).toList().chunked(7).forEach { weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            weekDays.forEach { dNum ->
                                val isSel = selectedDay == dNum
                                Surface(
                                    onClick = { selectedDay = dNum },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSel) Color(0xFF042F24) else Color(0xFFF1F5F9),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSel) Color(0xFFFBBF24) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "$dNum",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSel) Color(0xFFFDE68A) else Color(0xFF0F172A),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 7.dp)
                                    )
                                }
                            }
                            repeat(7 - weekDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onDateSelected(previewIso) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
            ) {
                Text("اعتماد التاريخ ($previewIso)", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun OfficialDocumentLightboxDialog(
    imageBase64: String,
    title: String,
    onDismiss: () -> Unit
) {
    val bmp = remember(imageBase64) { decodeBase64ToBitmapSafe(imageBase64) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.ifBlank { "معاينة الصورة الضوئية للكتاب الرسمي" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = Color.White)
                    }
                }

                if (bmp != null) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = title,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFBBF24),
                        contentColor = Color(0xFF042F24)
                    ),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("إغلاق معاينة الوثيقة", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MinistryMetricPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF065F46),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF042F24),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun MinistryOfficialPreviewTable(
    title: String,
    headers: List<String>,
    rows: List<List<String>>
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF8FAFC),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF042F24))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFDE68A)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFECFDF5))
                    .padding(horizontal = 6.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEach { h ->
                    Text(
                        text = h,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            rows.forEachIndexed { rIdx, row ->
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rIdx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                        .padding(horizontal = 6.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    row.forEachIndexed { cIdx, cell ->
                        Text(
                            text = cell,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (cIdx == 0) FontWeight.Bold else FontWeight.Medium,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
