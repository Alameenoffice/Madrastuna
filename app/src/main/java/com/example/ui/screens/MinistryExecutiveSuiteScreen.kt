package com.example.ui.screens

import android.content.ContentValues
import android.content.Context
import android.graphics.Paint
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.drawscope.Stroke
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
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================
// 1. DATA MODELS & PERSISTENT STORE FOR MINISTRY EXECUTIVE SUITE
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
    val mainPrompt: String,
    val branchA: String,
    val branchB: String,
    val marks: Int = 20
)

data class OfficialCorrespondenceRecord(
    val id: String = "cor_${UUID.randomUUID().toString().take(6)}",
    val type: String, // "صادر" | "وارد" | "أمر إداري"
    val bookNumber: String,
    val dateIso: String,
    val entityName: String,
    val subject: String,
    val notes: String
)

data class StaffPortfolioRecord(
    val teacherId: String,
    val qualification: String = "بكالوريوس تربية",
    val startDateIso: String = "2016-09-01",
    val jobTitle: String = "معلم جامعي",
    val jobGradeStage: String = "الدرجة الخامسة - المرحلة الثانية",
    val adminOrderNumber: String = "إداري / 104"
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
    val category: String, // "مقاعد دراسية" | "سبورات" | "أجهزة حاسوب" | "مختبرات" | "مبردات/مكيفات" | "أثاث إداري"
    val quantity: Int,
    val conditionStatus: String, // "صالح" | "يحتاج صيانة" | "مستهلك"
    val locationRoom: String,
    val notes: String
)

data class SchoolExpenseItemRecord(
    val id: String = "exp_${UUID.randomUUID().toString().take(6)}",
    val amountIqd: Long,
    val category: String, // "قرطاسية ومطبوعات" | "صيانة وترميم" | "كهرباء ووقود" | "أنشطة ومختبرات" | "نثريات عامة"
    val dateIso: String,
    val recipientName: String,
    val receiptNumber: String,
    val notes: String
)

private object MinistrySuiteLocalStore {
    private const val PREF_NAME = "ministry_executive_suite_store"

    private fun pref(context: Context) = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun loadCorrespondence(context: Context, schoolId: String): List<OfficialCorrespondenceRecord> {
        val raw = pref(context).getString("${schoolId}_correspondence", null)
        if (raw.isNullOrBlank()) {
            val today = ArabicCalendarDateHelper.todayIsoDate()
            return listOf(
                OfficialCorrespondenceRecord(
                    id = "cor_default_1",
                    type = "وارد",
                    bookNumber = "ت ع / 4820",
                    dateIso = today,
                    entityName = "المديرية العامة للتربية",
                    subject = "ضوابط وتعليمات اللجنة الامتحانية والقرار الوزاري للعام الدراسي",
                    notes = "عمم على أعضاء اللجنة الامتحانية للحفظ والعمل بموجبه"
                ),
                OfficialCorrespondenceRecord(
                    id = "cor_default_2",
                    type = "صادر",
                    bookNumber = "ص / 115",
                    dateIso = today,
                    entityName = "قسم الإشراف التربوي",
                    subject = "الموقف الإحصائي لملاك المدرسة وأعداد الطلبة الفعليين",
                    notes = "تم إرسال نسخة ورقية وإلكترونية"
                ),
                OfficialCorrespondenceRecord(
                    id = "cor_default_3",
                    type = "أمر إداري",
                    bookNumber = "إداري / 29",
                    dateIso = today,
                    entityName = "إدارة المدرسة",
                    subject = "تشكيل لجنة الجرد السنوي ولجنة التدقيق الامتحاني",
                    notes = "نافذ من تاريخ صدوره"
                )
            )
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        OfficialCorrespondenceRecord(
                            id = o.optString("id"),
                            type = o.optString("type", "صادر"),
                            bookNumber = o.optString("bookNumber"),
                            dateIso = o.optString("dateIso"),
                            entityName = o.optString("entityName"),
                            subject = o.optString("subject"),
                            notes = o.optString("notes")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveCorrespondence(context: Context, schoolId: String, list: List<OfficialCorrespondenceRecord>) {
        val arr = JSONArray()
        list.forEach { item ->
            arr.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("bookNumber", item.bookNumber)
                    put("dateIso", item.dateIso)
                    put("entityName", item.entityName)
                    put("subject", item.subject)
                    put("notes", item.notes)
                }
            )
        }
        pref(context).edit().putString("${schoolId}_correspondence", arr.toString()).apply()
    }

    fun loadStaffPortfolios(context: Context, schoolId: String): Map<String, StaffPortfolioRecord> {
        val raw = pref(context).getString("${schoolId}_staff_portfolios", null) ?: return emptyMap()
        return runCatching {
            val arr = JSONArray(raw)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val tid = o.optString("teacherId")
                    if (tid.isNotBlank()) {
                        put(
                            tid,
                            StaffPortfolioRecord(
                                teacherId = tid,
                                qualification = o.optString("qualification", "بكالوريوس تربية"),
                                startDateIso = o.optString("startDateIso", "2016-09-01"),
                                jobTitle = o.optString("jobTitle", "معلم جامعي"),
                                jobGradeStage = o.optString("jobGradeStage", "الدرجة الخامسة - المرحلة الثانية"),
                                adminOrderNumber = o.optString("adminOrderNumber", "إداري / 104")
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
                }
            )
        }
        pref(context).edit().putString("${schoolId}_staff_portfolios", arr.toString()).apply()
    }

    fun loadStudentWelfare(context: Context, schoolId: String): Map<String, StudentWelfareRecord> {
        val raw = pref(context).getString("${schoolId}_student_welfare", null) ?: return emptyMap()
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
        pref(context).edit().putString("${schoolId}_student_welfare", arr.toString()).apply()
    }

    fun loadArchivedYears(context: Context, schoolId: String): List<ArchivedSchoolYearSnapshot> {
        val raw = pref(context).getString("${schoolId}_archived_years", null) ?: return emptyList()
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
        pref(context).edit().putString("${schoolId}_archived_years", arr.toString()).apply()
    }

    fun loadInventory(context: Context, schoolId: String): List<SchoolInventoryItemRecord> {
        val raw = pref(context).getString("${schoolId}_inventory", null)
        if (raw.isNullOrBlank()) {
            return listOf(
                SchoolInventoryItemRecord("inv_1", "رحلات ومقاعد دراسية مزدوجة", "مقاعد دراسية", 120, "صالح", "القاعات الدراسية (1 - 6)", "جرد سنوي مطابق"),
                SchoolInventoryItemRecord("inv_2", "سبورات بيضاء قياسية", "سبورات", 12, "صالح", "الصفوف الدراسية", "بحالة ممتازة"),
                SchoolInventoryItemRecord("inv_3", "حاسبات مكتبية وطابعات ليزرية", "أجهزة حاسوب", 8, "صالح", "الإدارة ومختبر الحاسوب", "مع منظومة UPS"),
                SchoolInventoryItemRecord("inv_4", "مجهرات وأدوات مختبر العلوم", "مختبرات", 15, "صالح", "المختبر العلمي", "كامل التجهيز"),
                SchoolInventoryItemRecord("inv_5", "سبالت ومبردات هواء", "مبردات/مكيفات", 14, "يحتاج صيانة", "القاعات والإدارة", "2 منها تحتاج صيانة موسمية")
            )
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SchoolInventoryItemRecord(
                            id = o.optString("id"),
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
        }.getOrDefault(emptyList())
    }

    fun saveInventory(context: Context, schoolId: String, list: List<SchoolInventoryItemRecord>) {
        val arr = JSONArray()
        list.forEach { item ->
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
        pref(context).edit().putString("${schoolId}_inventory", arr.toString()).apply()
    }

    fun loadExpenses(context: Context, schoolId: String): List<SchoolExpenseItemRecord> {
        val raw = pref(context).getString("${schoolId}_expenses", null)
        if (raw.isNullOrBlank()) {
            val today = ArabicCalendarDateHelper.todayIsoDate()
            return listOf(
                SchoolExpenseItemRecord("exp_1", 250000L, "قرطاسية ومطبوعات", today, "مطبعة ومكتبة النور", "وصل / 841", "أوراق امتحانية وسجلات رسمية وأحبار"),
                SchoolExpenseItemRecord("exp_2", 180000L, "صيانة وترميم", today, "ورشة الصيانة الفنية", "وصل / 842", "صيانة منظومة الإنارة والمبردات")
            )
        }
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        SchoolExpenseItemRecord(
                            id = o.optString("id"),
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
        }.getOrDefault(emptyList())
    }

    fun saveExpenses(context: Context, schoolId: String, list: List<SchoolExpenseItemRecord>) {
        val arr = JSONArray()
        list.forEach { item ->
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
        pref(context).edit().putString("${schoolId}_expenses", arr.toString()).apply()
    }
}

// =========================================================================
// 2. OFFICIAL PDF & EXCEL CSV EXPORT ENGINE (STANDARD ANDROID SDK ONLY)
// =========================================================================

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
    footerNote: String = "تم إصدار هذه الوثيقة رسمياً من المنظومة الوزارية واللجنة الامتحانية المعتمدة",
    onResult: (Boolean, String) -> Unit
) {
    try {
        val pdfDocument = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#042F24")
            style = Paint.Style.FILL
        }
        val goldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#D97706")
            style = Paint.Style.FILL
        }
        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FDE68A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#1E293B")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.RIGHT
        }
        val tableHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#ECFDF5")
            style = Paint.Style.FILL
        }
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        // 1. Official Ministry Header
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 94f, headerPaint)
        canvas.drawRect(0f, 90f, pageWidth.toFloat(), 96f, goldPaint)

        val rightMargin = pageWidth - 26f
        val leftMargin = 26f
        val totalWidth = rightMargin - leftMargin
        val todayIso = ArabicCalendarDateHelper.todayIsoDate()

        canvas.drawText("جمهورية العراق — وزارة التربية — إدارة ($schoolName)", rightMargin, 28f, titlePaint)
        canvas.drawText(documentTitle, rightMargin, 52f, subHeaderPaint)
        canvas.drawText(
            "مدير المدرسة: $principalName  |  العام الدراسي: $academicYear  |  التاريخ: $todayIso",
            rightMargin,
            76f,
            Paint(bodyPaint).apply { color = android.graphics.Color.WHITE }
        )

        var currentY = 116f

        if (subtitleInfo.isNotBlank()) {
            canvas.drawText(subtitleInfo, rightMargin, currentY, Paint(bodyPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#065F46")
            })
            currentY += 20f
        }

        // 2. Summary KPI Boxes
        if (summaryPills.isNotEmpty()) {
            val count = summaryPills.size.coerceAtMost(4)
            val gap = 8f
            val boxW = (totalWidth - gap * (count - 1)) / count
            summaryPills.take(4).forEachIndexed { idx, (k, v) ->
                val boxLeft = rightMargin - ((idx + 1) * boxW) - (idx * gap)
                val boxRight = boxLeft + boxW
                canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 42f, 8f, 8f, tableHeaderPaint)
                canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 42f, 8f, 8f, borderPaint)
                val cx = (boxLeft + boxRight) / 2f
                canvas.drawText(k, cx, currentY + 16f, Paint(bodyPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 8.5f
                })
                canvas.drawText(v, cx, currentY + 33f, Paint(bodyPaint).apply {
                    textAlign = Paint.Align.CENTER
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    color = android.graphics.Color.parseColor("#042F24")
                })
            }
            currentY += 58f
        }

        // 3. Table Headers & Rows
        if (headers.isNotEmpty()) {
            val colCount = headers.size
            val colW = totalWidth / colCount
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 24f, tableHeaderPaint)
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 24f, borderPaint)
            val thPaint = Paint(bodyPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                color = android.graphics.Color.parseColor("#042F24")
                textSize = 9f
            }
            headers.forEachIndexed { cIdx, hText ->
                val colRight = rightMargin - (cIdx * colW)
                val colCenter = colRight - (colW / 2f)
                canvas.drawText(hText.take(22), colCenter, currentY + 16f, thPaint)
            }
            currentY += 24f

            val tdPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 8.5f
            }
            rows.take(24).forEach { row ->
                if (currentY + 21f < pageHeight - 70f) {
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 21f, borderPaint)
                    row.forEachIndexed { cIdx, cell ->
                        if (cIdx < colCount) {
                            val colRight = rightMargin - (cIdx * colW)
                            val colCenter = colRight - (colW / 2f)
                            canvas.drawText(cell.take(26), colCenter, currentY + 14f, tdPaint)
                        }
                    }
                    currentY += 21f
                }
            }
        }

        // 4. Official Signatures Footer
        val footerY = pageHeight - 36f
        canvas.drawLine(leftMargin, footerY - 22f, rightMargin, footerY - 22f, borderPaint)
        canvas.drawText(
            "$footerNote  |  ختم وتوقيع مدير المدرسة: $principalName",
            rightMargin,
            footerY,
            Paint(bodyPaint).apply {
                textSize = 9f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                color = android.graphics.Color.parseColor("#065F46")
            }
        )

        pdfDocument.finishPage(page)

        val safeName = "Ministry_${System.currentTimeMillis()}.pdf"
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

        onResult(true, "تم تصدير ($documentTitle) بصيغة PDF رسمية وحفظه في التنزيلات وفتح المعاينة للطباعة")
    } catch (e: Exception) {
        onResult(false, "تعذر تصدير PDF: ${e.localizedMessage ?: "خطأ غير متوقع"}")
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
        // UTF-8 BOM so Microsoft Excel opens Arabic text cleanly
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
    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }

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

    // Persistent Suite States
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
                // Top Royal Emerald & Gold Header with Vector Crest & 6 Horizontal Scrollable Tabs
                MinistrySuiteExecutiveHeader(
                    schoolName = schoolConfig.schoolName,
                    principalName = principalName,
                    academicYear = schoolConfig.academicYear,
                    selectedTab = selectedTab,
                    onSelectTab = { selectedTab = it },
                    onBack = onBack
                )

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
                            examSchedules = examSchedules
                        )

                        1 -> OfficialMinistryRecordsSection(
                            context = context,
                            viewModel = viewModel,
                            schoolConfig = schoolConfig,
                            principalName = principalName,
                            students = activeStudents,
                            grades = allGrades,
                            correspondenceList = correspondenceList,
                            onSaveCorrespondence = { updated ->
                                correspondenceList = updated
                                MinistrySuiteLocalStore.saveCorrespondence(context, schoolId, updated)
                            }
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
                            }
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
                            }
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
                            }
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
                            },
                            expensesList = expensesList,
                            onSaveExpenses = { updated ->
                                expensesList = updated
                                MinistrySuiteLocalStore.saveExpenses(context, schoolId, updated)
                            }
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// 4. EXECUTIVE HEADER & VECTOR CREST (Path.cubicTo)
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
                            modifier = Modifier.size(44.dp)
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المنظومة الوزارية واللجنة الامتحانية",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "$schoolName • المدير: $principalName • العام الدراسي: $academicYear",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
    val infiniteTransition = rememberInfiniteTransition(label = "ministry_crest_anim")
    val ringPulse by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crest_ring_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val r = (w.coerceAtMost(h) / 2f) * 0.90f

        // 1. Royal Emerald & Gold Outer Seal
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF065F46), Color(0xFF022C22)),
                center = Offset(cx, cy),
                radius = r
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFBBF24),
            radius = r * ringPulse.coerceAtMost(1.02f),
            center = Offset(cx, cy),
            style = Stroke(width = 2.4f)
        )

        // 2. Shield Crest using Path.cubicTo
        val shieldPath = Path().apply {
            moveTo(cx, cy - r * 0.62f)
            cubicTo(
                cx + r * 0.45f, cy - r * 0.60f,
                cx + r * 0.58f, cy - r * 0.15f,
                cx, cy + r * 0.64f
            )
            cubicTo(
                cx - r * 0.58f, cy - r * 0.15f,
                cx - r * 0.45f, cy - r * 0.60f,
                cx, cy - r * 0.62f
            )
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFF10B981).copy(alpha = 0.35f), Color(0xFF042F24))
            )
        )
        drawPath(
            path = shieldPath,
            color = Color(0xFFFDE68A),
            style = Stroke(width = 1.8f, cap = StrokeCap.Round)
        )

        // 3. Open Book & Balanced Scales of Exam Control inside Shield
        val bookPath = Path().apply {
            moveTo(cx, cy + r * 0.18f)
            cubicTo(
                cx - r * 0.18f, cy + r * 0.08f,
                cx - r * 0.34f, cy + r * 0.12f,
                cx - r * 0.38f, cy + r * 0.24f
            )
            lineTo(cx - r * 0.38f, cy - r * 0.16f)
            cubicTo(
                cx - r * 0.26f, cy - r * 0.24f,
                cx - r * 0.12f, cy - r * 0.20f,
                cx, cy - r * 0.10f
            )
            cubicTo(
                cx + r * 0.12f, cy - r * 0.20f,
                cx + r * 0.26f, cy - r * 0.24f,
                cx + r * 0.38f, cy - r * 0.16f
            )
            lineTo(cx + r * 0.38f, cy + r * 0.24f)
            cubicTo(
                cx + r * 0.34f, cy + r * 0.12f,
                cx + r * 0.18f, cy + r * 0.08f,
                cx, cy + r * 0.18f
            )
            close()
        }
        drawPath(
            path = bookPath,
            color = Color(0xFFFBBF24),
            style = Stroke(width = 2.0f, cap = StrokeCap.Round)
        )

        // Central pillar
        drawLine(
            color = Color(0xFFFDE68A),
            start = Offset(cx, cy - r * 0.38f),
            end = Offset(cx, cy + r * 0.22f),
            strokeWidth = 2.0f,
            cap = StrokeCap.Round
        )
    }
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
    examSchedules: List<ExamScheduleItem>
) {
    var activeSubTool by remember { mutableIntStateOf(0) }
    val subTools = remember {
        listOf(
            0 to "1. محرك الإعفاء ودرجة القرار",
            1 to "2. مولّد خرائط الجلوس والقطاعات",
            2 to "3. إصدار البطاقات الامتحانية",
            3 to "4. مُنشئ الأسئلة الرسمية (PDF)"
        )
    }

    // Tool 1 State: Decision Points (5 or 10)
    var decisionPointsLimit by remember { mutableIntStateOf(5) }
    val exemptionAnalysis = remember(students, grades, decisionPointsLimit) {
        students.map { stu ->
            val stuGrades = grades.filter { it.studentId == stu.id }
            val overallAvg = if (stuGrades.isNotEmpty()) {
                stuGrades.map { it.normalizedPercentage }.average().roundToInt()
            } else 0
            val minSubjectScore = stuGrades.minOfOrNull { it.normalizedPercentage } ?: 0
            val isGeneralExempt = stuGrades.isNotEmpty() && overallAvg >= 85 && minSubjectScore >= 75
            val individualExemptSubjects = stuGrades.filter { it.normalizedPercentage >= 90 }.map { "${it.subjectName} (${it.normalizedPercentage}%)" }
            val failingGrades = stuGrades.filter { it.normalizedPercentage in 1 until 50 }
            val pointsNeededToPass = failingGrades.sumOf { (50 - it.normalizedPercentage).coerceAtLeast(0) }
            val canPassByDecision = failingGrades.isNotEmpty() && pointsNeededToPass in 1..decisionPointsLimit

            Triple(stu, isGeneralExempt to individualExemptSubjects, failingGrades to pointsNeededToPass) to (overallAvg to canPassByDecision)
        }
    }
    val generalExemptList = remember(exemptionAnalysis) { exemptionAnalysis.filter { it.first.second.first } }
    val individualExemptList = remember(exemptionAnalysis) { exemptionAnalysis.filter { !it.first.second.first && it.first.second.second.isNotEmpty() } }
    val decisionEligibleList = remember(exemptionAnalysis) { exemptionAnalysis.filter { it.second.second } }

    // Tool 2 State: Seating Plan Generator
    var hallsCount by remember { mutableIntStateOf(3) }
    var hallCapacity by remember { mutableIntStateOf(20) }
    var selectedHallPreview by remember { mutableIntStateOf(1) }
    var generatedSeats by remember(students, hallsCount, hallCapacity) {
        mutableStateOf(generateSmartInterleavedSeatingPlan(students, hallsCount, hallCapacity))
    }

    // Tool 3 State: Exam Cards Filter
    var selectedClassForCards by remember { mutableStateOf("الكل") }

    // Tool 4 State: Official Exam Questions Paper Builder
    var examSubject by remember { mutableStateOf(schoolConfig.registeredSubjects.firstOrNull() ?: "اللغة العربية") }
    var examGradeClass by remember { mutableStateOf(schoolConfig.registeredClasses.firstOrNull() ?: "الأول المتوسط") }
    var examRound by remember { mutableStateOf("امتحانات نهاية السنة — الدور الأول") }
    var examDuration by remember { mutableStateOf("ساعتان") }
    var examInstructionNote by remember { mutableStateOf("ملاحظة: الإجابة عن خمسة أسئلة فقط، ولكل سؤال (20) درجة.") }
    var questionDrafts by remember {
        mutableStateOf(
            listOf(
                ExamQuestionItemDraft(1, "أجب عن الفرعين الآتيين بدقة مع ذكر الشواهد:", "فرع (أ): عرف المصطلحات الأساسية ومثّل لها في جمل مفيدة.", "فرع (ب): استخرج القواعد المطلوبة وبين السبب.", 20),
                ExamQuestionItemDraft(2, "علل خمساً مما يأتي تعليلاً علمياً دقيقاً:", "فرع (أ): أذكر ثلاثة أسباب رئيسية مع الشرح الموجز.", "فرع (ب): قارن في جدول بين الحالتين الأولى والثانية.", 20),
                ExamQuestionItemDraft(3, "املا الفراغات الآتية بما يناسبها في خمسٍ فقط:", "فرع (أ): أكمل النصوص أو المعادلات المقررة في المنهج.", "فرع (ب): صحح الخطأ إن وجد في العبارات الآتية.", 20),
                ExamQuestionItemDraft(4, "حل المسألة أو التطبيق العملي الآتي بخطوات متسلسلة:", "فرع (أ): اكتب القانون الأساسي وعوض القيم المعطاة.", "فرع (ب): فسر النتيجة النهائية وحدد وحدات القياس.", 20),
                ExamQuestionItemDraft(5, "اختر الإجابة الصحيحة من بين الأقواس لخمس فقرات:", "فرع (أ): اختر الجواب الأصح مع بيان خطوات الحل.", "فرع (ب): اربط عناصر القائمة (أ) بما يناسبها في القائمة (ب).", 20),
                ExamQuestionItemDraft(6, "اشرح بإيجاز أحد الموضوعين الآتيين:", "فرع (أ): الموضوع الأول ومميزاته وتطبيقاته.", "فرع (ب): الموضوع الثاني وأهميته العلمية.", 20)
            )
        )
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
            // SUB-TOOL 0: محرك درجة القرار والإعفاء التلقائي
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
                                text = "أولاً: محرك الإعفاء العام والفردي التلقائي (ضوابط وزارة التربية العراقية)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "• الإعفاء العام: معدل عام (85%) فأعلى بشرط ألا تقل أقل درجة عن (75%).\n" +
                                    "• الإعفاء الفردي: الحصول على السعي السنوي (90%) فأعلى في المادة الدراسية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF065F46),
                                modifier = Modifier.fillMaxWidth()
                            )

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
                                        text = "يتم فرز الطلبة المعفوين تلقائياً فور استكمال رصد درجات السعي السنوي (90% للإعفاء الفردي أو 85% للإعفاء العام).",
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
                                                    "التصنيف الوزاري: إعفاء عام شامل بجميع المواد (المعدل العام: ${avgPair.first}%)"
                                                } else {
                                                    "التصنيف الوزاري: إعفاء فردي في المواد: ${indSubs.joinToString("، ")}"
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
                                        subtitleInfo = "مفرزة تلقائياً وفق ضوابط وزارة التربية العراقية للسعي السنوي",
                                        summaryPills = listOf(
                                            "إعفاء عام" to "${generalExemptList.size}",
                                            "إعفاء فردي" to "${individualExemptList.size}",
                                            "إجمالي الطلبة" to "${students.size}"
                                        ),
                                        headers = listOf("ت", "اسم الطالب", "الصف والشعبة", "المعدل العام", "نوع الإعفاء"),
                                        rows = rows
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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

                // Decision Points Engine (5 or 10 points)
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
                                text = "ثانياً: معالج درجة القرار الوزاري (5 أو 10 درجات)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "يحدد تلقائياً الطلبة المكملين أو الراسبين الذين ترفعهم درجة القرار المحددة إلى درجة النجاح الصغرى (50%).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF78350F),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                listOf(5 to "قرار (5) درجات اعتيادي", 10 to "قرار (10) درجات استثنائي").forEach { (pts, label) ->
                                    FilterChip(
                                        selected = decisionPointsLimit == pts,
                                        onClick = { decisionPointsLimit = pts },
                                        label = { Text(label, fontWeight = FontWeight.ExtraBold) },
                                        modifier = Modifier.weight(1f)
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
                                        text = "لا يوجد حالياً طلبة بحاجة إلى (${decisionPointsLimit}) درجات قرار للوصول إلى درجة النجاح (50%).",
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
                                                    viewModel.showToastAlert("تم تطبيق ($totalNeeded) درجات قرار للطالب ${stu.studentName} واعتماد نجاحه رسمياً", isSuccess = true)
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
                                    var appliedCount = 0
                                    decisionEligibleList.forEach { (triple, _) ->
                                        val stu = triple.first
                                        val failingList = triple.third.first
                                        failingList.forEach { fg ->
                                            val needed = (50 - fg.normalizedPercentage).coerceAtLeast(1)
                                            if (viewModel.applyMinistryDecisionGradeBoost(stu.id, fg.subjectName, needed)) {
                                                appliedCount++
                                            }
                                        }
                                    }
                                    if (appliedCount > 0) {
                                        viewModel.showToastAlert("تم تطبيق درجة القرار الجماعي واعتماد نجاح ($appliedCount) مادة دراسية بنجاح", isSuccess = true)
                                    } else {
                                        viewModel.showToastAlert("جميع الطلبة المشمولين بدرجة القرار معتمدون مسبقاً أو لا توجد درجات حرجة ضمن سقف ($decisionPointsLimit) درجات", isSuccess = true)
                                    }
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
            // SUB-TOOL 1: مولّد خرائط الجلوس والقطاعات الامتحانية
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
                                text = "مولّد خرائط الجلوس والقطاعات الامتحانية الذكي (Seating Plan Generator)",
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(2, 3, 4, 5, 6, 8).forEach { hc ->
                                    FilterChip(
                                        selected = hallsCount == hc,
                                        onClick = {
                                            hallsCount = hc
                                            if (selectedHallPreview > hc) selectedHallPreview = 1
                                        },
                                        label = { Text("$hc قاعات", fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text(
                                text = "سعة كل قاعة امتحانية: ($hallCapacity مقعد)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F24)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(15, 20, 24, 30, 36).forEach { cap ->
                                    FilterChip(
                                        selected = hallCapacity == cap,
                                        onClick = { hallCapacity = cap },
                                        label = { Text("$cap مقعد", fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    generatedSeats = generateSmartInterleavedSeatingPlan(students, hallsCount, hallCapacity)
                                    viewModel.showToastAlert("تم توليد خرائط الجلوس الذكية لـ (${generatedSeats.size}) طالب عبر ($hallsCount) قاعات مع دمج الصفوف", isSuccess = true)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("generate_seating_plan_btn")
                            ) {
                                Icon(Icons.Filled.GridView, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("توليد خرائط الجلوس الذكية", fontWeight = FontWeight.ExtraBold)
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
                                            Text("القاعة ($hNum) • $cnt طالب", fontWeight = FontWeight.Bold)
                                        }
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
                                        rows = rows
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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
            // SUB-TOOL 2: إصدار البطاقات الامتحانية للطلبة
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
                                text = "إصدار البطاقات الامتحانية الرسمية للطلبة (مع باركود التحقق ورقم القاعة والمقعد)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.fillMaxWidth()
                            )

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
                                        listOf(
                                            item.student.fullNameWithFather,
                                            "${item.student.gradeClass} (${item.student.sectionName})",
                                            item.hallName,
                                            "مقعد ${item.seatNumber} (${item.sectorCode})",
                                            "EXAM_QR:${item.student.id}"
                                        )
                                    }
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "كشف البطاقات الامتحانية الرسمية للطلبة ($selectedClassForCards)",
                                        subtitleInfo = "يتضمن أرقام القاعات والمقاعد ورموز التحقق الامتحانية المعتمدة",
                                        summaryPills = listOf(
                                            "البطاقات الصادرة" to "${filteredSeatCards.size} بطاقة",
                                            "الصف الدراسي" to selectedClassForCards,
                                            "العام الدراسي" to schoolConfig.academicYear
                                        ),
                                        headers = listOf("اسم الطالب", "الصف والشعبة", "القاعة", "رقم المقعد", "رمز الباركود"),
                                        rows = rows
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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
                                Text("طباعة البطاقات الامتحانية الجماعية (${filteredSeatCards.size} بطاقة) PDF", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }

                items(filteredSeatCards.take(15), key = { it.student.id }) { seatItem ->
                    val stuExams = examSchedules.filter {
                        ArabicTextNormalizer.matchesClass(it.gradeClass, seatItem.student.gradeClass)
                    }.take(4)
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
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
                                        text = "بطاقة دخول الامتحان الوزاري / المدرسي — ${schoolConfig.schoolName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF059669),
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

                                // Drawn QR Matrix Emblem
                                ExamVerificationQrMiniCanvas(
                                    seed = seatItem.student.id,
                                    modifier = Modifier.size(52.dp)
                                )
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
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "بطاقة امتحانية رسمية — الطالب: ${seatItem.student.fullNameWithFather}",
                                        subtitleInfo = "الصف: ${seatItem.student.gradeClass} (${seatItem.student.sectionName}) | ${seatItem.hallName} | مقعد رقم (${seatItem.seatNumber}) | الباركود: EXAM_QR:${seatItem.student.id}",
                                        summaryPills = listOf(
                                            "القاعة الامتحانية" to seatItem.hallName,
                                            "رقم المقعد" to "${seatItem.seatNumber}",
                                            "القطاع" to seatItem.sectorCode
                                        ),
                                        headers = listOf("المادة الامتحانية", "تاريخ الامتحان", "اليوم / الوقت", "القاعة والمقعد"),
                                        rows = if (stuExams.isNotEmpty()) {
                                            stuExams.map { ex ->
                                                listOf(ex.subjectName, ex.examDate, ex.timeSlot.ifBlank { "${ex.dayName} • 8:30 صباحاً" }, "${seatItem.hallName} - مقعد ${seatItem.seatNumber}")
                                            }
                                        } else {
                                            (schoolConfig.registeredSubjects.ifEmpty { listOf("اللغة العربية", "الرياضيات", "اللغة الإنجليزية", "العلوم") }).take(6).mapIndexed { i, sub ->
                                                listOf(sub, "حسب الجدول المعلن", "8:30 صباحاً", "${seatItem.hallName} - مقعد ${seatItem.seatNumber}")
                                            }
                                        }
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة البطاقة الامتحانية الفردية (PDF)", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // SUB-TOOL 3: مُنشئ الأسئلة الامتحانية الرسمي (بدون الحاجة لـ Word)
            // -----------------------------------------------------------------
            3 -> {
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
                                text = "مُنشئ الأسئلة الامتحانية الرسمي (تنسيق وزاري فوري بدون الحاجة لـ Word)",
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
                                    label = { Text("الدور / الفصل") },
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

                            OutlinedTextField(
                                value = examInstructionNote,
                                onValueChange = { examInstructionNote = it },
                                label = { Text("ملاحظة اللجنة الامتحانية أعلى الورقة") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            questionDrafts.forEachIndexed { idx, q ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "السؤال (${q.questionNumber}) — (${q.marks} درجة):",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF042F24)
                                        )
                                        OutlinedTextField(
                                            value = q.mainPrompt,
                                            onValueChange = { newVal ->
                                                questionDrafts = questionDrafts.toMutableList().also {
                                                    it[idx] = q.copy(mainPrompt = newVal)
                                                }
                                            },
                                            label = { Text("منطوق س${q.questionNumber} الرئيسي") },
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = q.branchA,
                                            onValueChange = { newVal ->
                                                questionDrafts = questionDrafts.toMutableList().also {
                                                    it[idx] = q.copy(branchA = newVal)
                                                }
                                            },
                                            label = { Text("فرع (أ)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        OutlinedTextField(
                                            value = q.branchB,
                                            onValueChange = { newVal ->
                                                questionDrafts = questionDrafts.toMutableList().also {
                                                    it[idx] = q.copy(branchB = newVal)
                                                }
                                            },
                                            label = { Text("فرع (ب)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val qRows = questionDrafts.map { q ->
                                        listOf(
                                            "س${q.questionNumber} (${q.marks}د)",
                                            q.mainPrompt,
                                            q.branchA,
                                            q.branchB
                                        )
                                    }
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "ورقة الأسئلة الامتحانية الرسمية — مادة: $examSubject ($examGradeClass)",
                                        subtitleInfo = "$examRound  |  الزمن: $examDuration  |  $examInstructionNote",
                                        summaryPills = listOf(
                                            "المادة" to examSubject,
                                            "الصف" to examGradeClass,
                                            "الزمن" to examDuration,
                                            "الدرجة الكلية" to "100 درجة"
                                        ),
                                        headers = listOf("السؤال والدرجة", "منطوق السؤال الرئيسي", "الفرع الأول (أ)", "الفرع الثاني (ب)"),
                                        rows = qRows,
                                        footerNote = "مع تمنيات اللجنة الامتحانية لجميع طلبتنا الأعزاء بالنجاح والتفوق"
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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

    // Group students by class and interleave them so adjacent seats never share the same class
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
    Canvas(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .padding(4.dp)
    ) {
        val grid = 7
        val cellW = size.width / grid
        val cellH = size.height / grid
        val hash = seed.hashCode()
        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isCorner = (r < 2 && c < 2) || (r < 2 && c >= grid - 2) || (r >= grid - 2 && c < 2)
                val bit = ((hash shr ((r * grid + c) % 28)) and 1) == 1
                if (isCorner || bit) {
                    drawRect(
                        color = Color(0xFF042F24),
                        topLeft = Offset(c * cellW, r * cellH),
                        size = Size(cellW * 0.88f, cellH * 0.88f)
                    )
                }
            }
        }
    }
}

// =========================================================================
// 6. SECTION 2: OFFICIAL MINISTRY RECORDS & ABSENCE WARNINGS
// =========================================================================

@Composable
private fun OfficialMinistryRecordsSection(
    context: Context,
    viewModel: SchoolViewModel,
    schoolConfig: SchoolConfig,
    principalName: String,
    students: List<StudentRecord>,
    grades: List<SubjectGrade>,
    correspondenceList: List<OfficialCorrespondenceRecord>,
    onSaveCorrespondence: (List<OfficialCorrespondenceRecord>) -> Unit
) {
    var activeSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf(
        0 to "1. السجلات الرسمية (القيد العام والوسطي)",
        1 to "2. إنذارات الغياب القانونية (7 / 15 / 21 يوم)",
        2 to "3. سجل الصادرة والواردة والأوامر الإدارية"
    )

    var selectedRegisterKind by remember { mutableStateOf("سجل القيد العام") }
    var registerSearchQuery by remember { mutableStateOf("") }

    // Correspondence Form State
    var corType by remember { mutableStateOf("صادر") }
    var corNumber by remember { mutableStateOf("") }
    var corDate by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
    var corEntity by remember { mutableStateOf("") }
    var corSubject by remember { mutableStateOf("") }
    var corNotes by remember { mutableStateOf("") }
    var corSearch by remember { mutableStateOf("") }

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
                        label = { Text(label, fontWeight = FontWeight.ExtraBold) },
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
                                        rows = tableRows
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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
                                        filePrefix = "Ministry_Register",
                                        headers = tableHeaders,
                                        rows = tableRows
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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
            // 1: نظام إنذارات الغياب القانونية التلقائي (7 / 15 / 21 يوم)
            // -----------------------------------------------------------------
            1 -> {
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
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "نظام إنذارات الغياب القانونية التلقائي (ضوابط الانضباط المدرسي)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "• عند بلوغ (7) أيام غياب: يصدر تنبيه (إنذار غياب أولي).\n" +
                                    "• عند بلوغ (15) يوماً: يصدر تنبيه (إنذار غياب نهائي).\n" +
                                    "• عند تجاوز (21) يوماً: يصدر تنبيه (تجاوز الحد القانوني — فصل بالغياب).",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF7F1D1D),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                items(
                    students.sortedByDescending { viewModel.getTotalAbsenceDays(it.id) },
                    key = { it.id }
                ) { stu ->
                    val absDays = viewModel.getTotalAbsenceDays(stu.id)
                    val warningLevel = when {
                        absDays >= 21 -> "تجاوز الحد القانوني (فصل بالغياب)"
                        absDays >= 15 -> "إنذار غياب نهائي"
                        absDays >= 7 -> "إنذار غياب أولي"
                        else -> "إنذار غياب أولي / وقائي ($absDays أيام مسجلة)"
                    }
                    val badgeColor = when {
                        absDays >= 21 -> DangerRed
                        absDays >= 15 -> Color(0xFFD97706)
                        absDays >= 7 -> Color(0xFFB45309)
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
                                text = "مجموع الغيابات المسجلة: ($absDays) يوم • الحالة القانونية: $warningLevel",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    val msgTitle = "كتاب رسمي: $warningLevel — ${stu.studentName}"
                                    val msgBody = "إلى ولي أمر الطالب (${stu.fullNameWithFather}) في الصف (${stu.gradeClass}): نود إعلامكم بأن مجموع غيابات ابنكم بلغ ($absDays) يوماً، وبذلك يصدر بحقه ($warningLevel) وفق تعليمات وزارة التربية. يرجى مراجعة إدارة المدرسة فوراً."
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
                                    viewModel.showToastAlert("تم إرسال ($warningLevel) رسمياً إلى ولي أمر الطالب ${stu.studentName}", isSuccess = true)
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
                                    exportMinistryOfficialTablePdf(
                                        context = context,
                                        schoolName = schoolConfig.schoolName,
                                        principalName = principalName,
                                        academicYear = schoolConfig.academicYear,
                                        documentTitle = "كتاب رسمي: $warningLevel",
                                        subtitleInfo = "موجه إلى السيد ولي أمر الطالب (${stu.fullNameWithFather}) المحترم — الصف: ${stu.gradeClass} (${stu.sectionName})",
                                        summaryPills = listOf(
                                            "أيام الغياب" to "$absDays يوم",
                                            "نوع الإنذار" to warningLevel,
                                            "ولي الأمر" to stu.parentName
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
                                        footerNote = "يرجى حضور ولي الأمر إلى إدارة المدرسة خلال 48 ساعة لضمان عدم تعرض الطالب للفصل بالغياب"
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("طباعة كتاب الإنذار الرسمي (PDF)", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // -----------------------------------------------------------------
            // 2: سجل الصادرة والواردة والأوامر الإدارية
            // -----------------------------------------------------------------
            2 -> {
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = corNumber,
                                    onValueChange = { corNumber = it },
                                    label = { Text("رقم الكتاب / الأمر") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = corDate,
                                    onValueChange = { corDate = it },
                                    label = { Text("التاريخ") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
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

                            Button(
                                onClick = {
                                    if (corNumber.isBlank() || corSubject.isBlank()) {
                                        viewModel.showToastAlert("يرجى إدخال رقم الكتاب وموضوعه لحفظه في الأرشيف", isSuccess = false)
                                        return@Button
                                    }
                                    val newRec = OfficialCorrespondenceRecord(
                                        type = corType,
                                        bookNumber = corNumber.trim(),
                                        dateIso = corDate.trim().ifBlank { ArabicCalendarDateHelper.todayIsoDate() },
                                        entityName = corEntity.trim().ifBlank { "المديرية العامة للتربية" },
                                        subject = corSubject.trim(),
                                        notes = corNotes.trim()
                                    )
                                    onSaveCorrespondence(listOf(newRec) + correspondenceList)
                                    corNumber = ""
                                    corSubject = ""
                                    corNotes = ""
                                    viewModel.showToastAlert("تم قيد وأرشفة الكتاب الرسمي في سجل الصادرة والواردة بنجاح", isSuccess = true)
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
                            it.entityName.contains(corSearch, ignoreCase = true)
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
                                label = { Text("بحث فوري برقم الكتاب أو الموضوع أو الجهة...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            filteredCor.forEach { item ->
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
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "[${item.type}] رقم الكتاب: ${item.bookNumber} • التاريخ: ${item.dateIso}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF042F24),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Text(
                                            text = "الجهة: ${item.entityName} • الموضوع: ${item.subject}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1E293B),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        if (item.notes.isNotBlank()) {
                                            Text(
                                                text = "الهامش: ${item.notes}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF065F46),
                                                modifier = Modifier.fillMaxWidth()
                                            )
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
                                        }
                                    ) { ok, msg ->
                                        viewModel.showToastAlert(msg, isSuccess = ok)
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
    onSavePortfolios: (Map<String, StaffPortfolioRecord>) -> Unit
) {
    val currentYearInt = remember { Calendar.getInstance().get(Calendar.YEAR) }
    var editingTeacher by remember { mutableStateOf<UserAccount?>(null) }

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
                        text = "إجمالي الكادر التدريسي الفعلي (مع العزل التام لسائقي النقل): (${teachers.size}) معلم/مدرس",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            val rows = teachers.mapIndexed { idx, t ->
                                val p = portfoliosMap[t.id] ?: StaffPortfolioRecord(t.id)
                                val startYear = p.startDateIso.take(4).toIntOrNull() ?: 2016
                                val yearsServed = (currentYearInt - startYear).coerceAtLeast(1)
                                listOf(
                                    "${idx + 1}",
                                    t.fullName,
                                    t.specialty.ifBlank { "عام" },
                                    p.qualification,
                                    p.jobTitle,
                                    "$yearsServed سنة (${p.startDateIso})"
                                )
                            }
                            exportMinistryOfficialTablePdf(
                                context = context,
                                schoolName = schoolConfig.schoolName,
                                principalName = principalName,
                                academicYear = schoolConfig.academicYear,
                                documentTitle = "خلاصة الخدمة التربوية والإضبارة الوظيفية للملاك التدريسي",
                                subtitleInfo = "ملاك المدرسة الفعلي المعتمد (باستثناء سائقي الخطوط)",
                                summaryPills = listOf(
                                    "إجمالي الملاك" to "${teachers.size} معلم",
                                    "العام الدراسي" to schoolConfig.academicYear
                                ),
                                headers = listOf("ت", "الاسم الكامل", "الاختصاص", "الشهادة", "العنوان الوظيفي", "سنوات الخدمة"),
                                rows = rows
                            ) { ok, msg ->
                                viewModel.showToastAlert(msg, isSuccess = ok)
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

        items(teachers, key = { it.id }) { teacher ->
            val portfolio = portfoliosMap[teacher.id] ?: StaffPortfolioRecord(teacherId = teacher.id)
            val startYear = portfolio.startDateIso.take(4).toIntOrNull() ?: 2016
            val serviceYears = (currentYearInt - startYear).coerceAtLeast(1)
            val nextAllowanceDate = "${currentYearInt + 1}-${portfolio.startDateIso.drop(5).ifBlank { "09-01" }}"
            val teacherAge = if (teacher.ageYears > 0) teacher.ageYears else 35
            val remainingToRetirement = (60 - teacherAge).coerceAtLeast(1)

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
                        text = "${teacher.fullName} — الاختصاص: ${teacher.specialty.ifBlank { "عام" }}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "الشهادة: ${portfolio.qualification} • العنوان الوظيفي: ${portfolio.jobTitle} • ${portfolio.jobGradeStage}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "تاريخ المباشرة: ${portfolio.startDateIso} • الأمر الإداري: ${portfolio.adminOrderNumber}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Service Calculator Summary Box
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
                                text = "• الخدمة الفعلية: ($serviceYears) سنة  |  العلاوة السنوية القادمة: ($nextAllowanceDate)  |  المتبقي للتقاعد: ($remainingToRetirement) سنة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "• الصفوف والشعب المخولة لاستلام الدرجات والواجبات: ${teacher.formattedAssignedClassesForDisplay}",
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
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = assignedClassesDraft,
                        onValueChange = { assignedClassesDraft = it },
                        label = { Text("الصفوف والشعب المخصصة (مفصولة بـ |)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = qualificationDraft,
                        onValueChange = { qualificationDraft = it },
                        label = { Text("الشهادة والتحصيل الدراسي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = startDateDraft,
                        onValueChange = { startDateDraft = it },
                        label = { Text("تاريخ المباشرة بالخدمة (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = jobTitleDraft,
                        onValueChange = { jobTitleDraft = it },
                        label = { Text("العنوان الوظيفي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = gradeStageDraft,
                        onValueChange = { gradeStageDraft = it },
                        label = { Text("الدرجة والمرحلة الوظيفية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = orderNumDraft,
                        onValueChange = { orderNumDraft = it },
                        label = { Text("رقم الأمر الإداري") },
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
                            specialty = specialtyDraft,
                            assignedClasses = assignedClassesDraft,
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
                        viewModel.showToastAlert("تم حفظ الإضبارة الوظيفية وتحديث صلاحيات المعلم (${teacher.fullName})", isSuccess = true)
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
    onSaveWelfare: (Map<String, StudentWelfareRecord>) -> Unit
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
                                rows = rows
                            ) { ok, msg ->
                                viewModel.showToastAlert(msg, isSuccess = ok)
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
                                filePrefix = "Ministry_Orphans_And_Grant",
                                headers = listOf("ت", "اسم الطالب", "الصف", "الشعبة", "حالة اليتيم", "المنحة الطلابية", "رقم البطاقة الذكية/الرعاية", "ذوي احتياجات/متعفف", "هاتف ولي الأمر"),
                                rows = rows
                            ) { ok, msg ->
                                viewModel.showToastAlert(msg, isSuccess = ok)
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
                        viewModel.showToastAlert("تم تحديث تصنيف الرعاية والمنحة للطالب (${stu.studentName})", isSuccess = true)
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
    onSaveArchivedYears: (List<ArchivedSchoolYearSnapshot>) -> Unit
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
        // 1. Student Promotion Engine Card
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
                        text = "أولاً: محرك ترحيل الطلاب الذكي ونقل الخريجين (Student Promotion Engine)",
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
                            viewModel.showToastAlert("تم ختم وأرشفة بيانات العام الدراسي (${schoolConfig.academicYear}) في الأرشيف الدائم بنجاح", isSuccess = true)
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
                                            }
                                        ) { ok, msg ->
                                            viewModel.showToastAlert(msg, isSuccess = ok)
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
    onSaveExpenses: (List<SchoolExpenseItemRecord>) -> Unit
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
    var expRecipient by remember { mutableStateOf("") }
    var expReceiptNo by remember { mutableStateOf("") }
    var expNotes by remember { mutableStateOf("") }

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
                                    viewModel.showToastAlert("يرجى إدخال اسم المادة لإضافتها في سجل الجرد", isSuccess = false)
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
                                viewModel.showToastAlert("تمت إضافة المادة إلى سجل جرد ممتلكات المدرسة بنجاح", isSuccess = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.AddBox, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("إضافة المادة إلى سجل الجرد", fontWeight = FontWeight.ExtraBold)
                        }

                        MinistryOfficialPreviewTable(
                            title = "كشف الجرد السنوي لممتلكات المدرسة (${inventoryList.size} مادة)",
                            headers = listOf("المادة", "التصنيف", "العدد", "الحالة", "الموقع"),
                            rows = inventoryList.map {
                                listOf(it.itemName, it.category, "${it.quantity}", it.conditionStatus, it.locationRoom)
                            }
                        )

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
                                    }
                                ) { ok, msg ->
                                    viewModel.showToastAlert(msg, isSuccess = ok)
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
                                    }
                                ) { ok, msg ->
                                    viewModel.showToastAlert(msg, isSuccess = ok)
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
                                    viewModel.showToastAlert("يرجى إدخال مبلغ صحيح بالدينار العراقي", isSuccess = false)
                                    return@Button
                                }
                                val rec = SchoolExpenseItemRecord(
                                    amountIqd = amt,
                                    category = expCategory,
                                    dateIso = ArabicCalendarDateHelper.todayIsoDate(),
                                    recipientName = expRecipient.trim().ifBlank { "مجهز معتمد" },
                                    receiptNumber = expReceiptNo.trim().ifBlank { "وصل / ${100 + expensesList.size}" },
                                    notes = expNotes.trim()
                                )
                                onSaveExpenses(listOf(rec) + expensesList)
                                expAmount = ""
                                expRecipient = ""
                                expReceiptNo = ""
                                expNotes = ""
                                viewModel.showToastAlert("تم تقييد وصل الصرف في السجل المالي للمدرسة بنجاح", isSuccess = true)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Payments, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ وصل الصرف في السجل المالي", fontWeight = FontWeight.ExtraBold)
                        }

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

// =========================================================================
// 11. REUSABLE MINISTRY UI COMPONENTS
// =========================================================================

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
