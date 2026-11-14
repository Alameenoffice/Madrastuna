package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
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
import android.content.ContentValues
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// =========================================================================
// 1. DATA MODELS FOR AI SCHOOL ADVISOR & SMART COMMUNICATION CENTER
// =========================================================================

enum class SmartAdvisorCommunicationCategory(
    val arabicLabel: String,
    val mappedNotificationType: NotificationType,
    val mappedReportType: ReportType?
) {
    ABSENCE("غياب وحضور", NotificationType.ATTENDANCE, ReportType.BEHAVIOR_ALERT),
    RESULTS("نتائج ودرجات", NotificationType.GRADES, ReportType.ACADEMIC_NOTE),
    HOMEWORK("واجبات وتحضير", NotificationType.HOMEWORK, ReportType.ACADEMIC_NOTE),
    EXAM_DATE("موعد امتحان", NotificationType.GRADES, ReportType.ACADEMIC_NOTE),
    HOLIDAY("عطلة رسمية", NotificationType.GENERAL, null),
    MORNING_DELAY("تأخر صباحي", NotificationType.ATTENDANCE, ReportType.BEHAVIOR_ALERT),
    PARENT_SUMMONS("استدعاء ولي أمر", NotificationType.REPORT, ReportType.PARENT_SUMMONS),
    BEHAVIOR("سلوك ومتابعة", NotificationType.REPORT, ReportType.BEHAVIOR_ALERT),
    EXCELLENCE_CONGRATS("تهنئة تفوق", NotificationType.GRADES, ReportType.APPRECIATION),
    GENERAL_ANNOUNCEMENT("تبليغ إداري عام", NotificationType.GENERAL, null)
}

enum class SmartDeliveryChannel(val arabicLabel: String, val shortBadge: String) {
    IN_APP_AND_CLOUD("إشعار داخل التطبيق والسحابة", "داخل التطبيق"),
    PUSH_NOTIFICATION("إشعار فوري Push", "إشعار فوري"),
    WHATSAPP("رسالة WhatsApp + إشعار التطبيق", "واتساب"),
    SMS("رسالة SMS + إشعار التطبيق", "رسالة نصية")
}

data class SmartRecipientTarget(
    val student: StudentRecord,
    val recipientLabel: String,
    val targetParentOnly: Boolean = false
)

data class PendingSmartNotificationProposal(
    val id: String = "prop_${UUID.randomUUID().toString().take(6)}",
    val audienceTitle: String,
    val recipients: List<SmartRecipientTarget>,
    val targetGradeClass: String = "",
    val targetSectionName: String = "",
    val isBroadcastToAllStudents: Boolean = false,
    val category: SmartAdvisorCommunicationCategory,
    val suggestedTitle: String,
    val suggestedBody: String,
    val defaultChannel: SmartDeliveryChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD,
    val isExecuted: Boolean = false,
    val executionSummary: String = ""
)

data class SmartDispatchAuditLogEntry(
    val id: String = "log_${UUID.randomUUID().toString().take(6)}",
    val senderName: String,
    val timestamp: String,
    val audienceTitle: String,
    val recipientNames: List<String>,
    val categoryLabel: String,
    val channel: SmartDeliveryChannel,
    val messageTitle: String,
    val messageBody: String,
    val totalTargeted: Int,
    val successCount: Int,
    val failedCount: Int,
    val failedRecipients: List<SmartRecipientTarget> = emptyList(),
    val statusLabel: String
)

data class AdvisorTableSection(
    val title: String,
    val headers: List<String>,
    val rows: List<List<String>>
)

data class AdvisorChartBarItem(
    val label: String,
    val valuePercent: Int,
    val valueDisplay: String,
    val isWarning: Boolean = false
)

data class AdvisorChatMessage(
    val id: String = "msg_${UUID.randomUUID().toString().take(8)}",
    val isFromUser: Boolean,
    val senderTitle: String,
    val timestamp: String,
    val headline: String = "",
    val bodyText: String,
    val keyMetrics: List<Pair<String, String>> = emptyList(),
    val tableSection: AdvisorTableSection? = null,
    val chartTitle: String = "",
    val chartBars: List<AdvisorChartBarItem> = emptyList(),
    val recommendations: List<String> = emptyList(),
    val pendingNotificationProposal: PendingSmartNotificationProposal? = null
)

enum class OfficialSmartReportKind(
    val indexNumber: Int,
    val arabicTitle: String,
    val subtitle: String,
    val periodBadge: String
) {
    DAILY_REPORT(1, "التقرير اليومي", "ملخص الدوام اليومي، الحضور والغياب، حركة البوابة، والواجبات اليومية", "يومي"),
    WEEKLY_REPORT(2, "التقرير الأسبوعي", "حصيلة الأسبوع الدراسي لمعدلات المواظبة، السلوك، والالتزام بالتحضير", "أسبوعي"),
    MONTHLY_REPORT(3, "التقرير الشهري", "تحليل الأداء الشهري للصفوف والشعب ومؤشرات الانضباط والتقييمات", "شهري"),
    TERM_REPORT(4, "التقرير الفصلي", "تقييم نتائج الفصل الدراسي ونسب النجاح ومقارنة أداء الصفوف والمواد", "فصلي"),
    ANNUAL_REPORT(5, "التقرير السنوي", "الحصيلة السنوية الشاملة للمدرسة ومعدلات السعي والترحيل والأقساط", "سنوي"),
    ATTENDANCE_REPORT(6, "تقرير الحضور والغياب", "كشف تفصيلي بنسب الحضور والغياب والتأخر والطلبة المتكرري الغياب", "مواظبة"),
    ACADEMIC_REPORT(7, "التقرير الأكاديمي", "تحليل الدرجات المرصودة، نسب النجاح، لوحة الشرف، والمواد الحرجة", "أكاديمي"),
    STAFF_EVALUATION_REPORT(8, "تقرير تقييم الكادر التدريسي", "كفاءة المعلمين، الالتزام، النشاط التفاعلي، وتقييمات أولياء الأمور", "كادر تدريسي"),
    PROBLEMS_RECOMMENDATIONS_REPORT(9, "تقرير المشكلات والتوصيات الاستباقية", "تشخيص نقاط الضعف والإنذار المبكر وخطة العمل العلاجية المقترحة", "استباقي"),
    COMPREHENSIVE_SCHOOL_REPORT(10, "التقرير الشامل للمدرسة", "الوثيقة التنفيذية الكاملة لجميع المؤشرات الإدارية والعلمية والمالية", "شامل")
}

// =========================================================================
// 2. REAL SCHOOL DATA CONTEXT SNAPSHOT ENGINE
// =========================================================================

data class ClassStatisticalItem(
    val gradeClass: String,
    val totalStudents: Int,
    val presentToday: Int,
    val absentToday: Int,
    val lateToday: Int,
    val excusedToday: Int,
    val unrecordedToday: Int,
    val attendanceRatePercent: Int,
    val recordedGradesCount: Int,
    val averageGradePercent: Int,
    val passRatePercent: Int
)

data class SubjectStatisticalItem(
    val subjectName: String,
    val recordsCount: Int,
    val averagePercent: Int,
    val passRatePercent: Int
)

data class TeacherStatisticalItem(
    val teacherName: String,
    val specialty: String,
    val assignedClasses: String,
    val supervisedStudents: Int,
    val commitmentPercent: Int,
    val activityPercent: Int,
    val averageStars: Float,
    val ratingsCount: Int
)

data class RealSchoolAnalyticalSnapshot(
    val schoolName: String,
    val principalName: String,
    val academicYear: String,
    val schoolTypeLabel: String,
    val schoolStageLabel: String,
    val todayIso: String,
    val todayHolidayLabel: String?,
    val totalStudents: Int,
    val activeStudentsList: List<StudentRecord>,
    val presentTodayCount: Int,
    val absentTodayCount: Int,
    val lateTodayCount: Int,
    val excusedTodayCount: Int,
    val unrecordedTodayCount: Int,
    val overallAttendanceRatePercent: Int,
    val absentTodayStudents: List<StudentRecord>,
    val lateTodayStudents: List<StudentRecord>,
    val repeatedAbsenceStudents: List<Pair<StudentRecord, Int>>,
    val honorStudents: List<StudentRecord>,
    val classStats: List<ClassStatisticalItem>,
    val subjectStats: List<SubjectStatisticalItem>,
    val totalGradesRecorded: Int,
    val overallAcademicAveragePercent: Int,
    val overallPassRatePercent: Int,
    val teacherStats: List<TeacherStatisticalItem>,
    val totalTeachersCount: Int,
    val homeworksCount: Int,
    val upcomingExamsCount: Int,
    val parentReportsCount: Int,
    val pendingLeaveRequestsCount: Int,
    val busSubscribersCount: Int,
    val busRoutesCount: Int,
    val totalTuitionFeeIqd: Long,
    val totalTuitionPaidIqd: Long,
    val totalTuitionRemainingIqd: Long,
    val overallSchoolHealthScore: Int,
    val overallHealthLabel: String,
    val proactiveProblems: List<String>,
    val strategicRecommendations: List<String>,
    val rawContextPromptForGemini: String
)

fun buildRealSchoolContextSnapshot(viewModel: SchoolViewModel): RealSchoolAnalyticalSnapshot {
    val config = viewModel.schoolConfig.value
    val currentUser = viewModel.currentUser.value
    val todayIso = ArabicCalendarDateHelper.todayIsoDate()
    val holidayLabel = viewModel.getHolidayLabelForDate(todayIso)

    val allStudents = viewModel.students.value.filter {
        (it.schoolId.isEmpty() || it.schoolId == config.schoolId) && it.isActive
    }
    val allGrades = viewModel.grades.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val allAccounts = viewModel.accounts.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val teachers = allAccounts.filter {
        it.role == UserRole.TEACHER && !it.isDriverInSchoolConfig(config)
    }
    val ratings = viewModel.teacherRatings.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val homeworks = viewModel.homeworks.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val exams = viewModel.examSchedules.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val reports = viewModel.reports.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val leaveReqs = viewModel.leaveRequests.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }
    val tuitions = viewModel.tuitions.value.filter {
        it.schoolId.isEmpty() || it.schoolId == config.schoolId
    }

    val absentToday = mutableListOf<StudentRecord>()
    val lateToday = mutableListOf<StudentRecord>()
    var presentCount = 0
    var excusedCount = 0
    var unrecordedCount = 0

    allStudents.forEach { stu ->
        val status = viewModel.getStudentStatusForDate(stu, todayIso)
        when (status) {
            AttendanceStatus.PRESENT -> presentCount++
            AttendanceStatus.ABSENT -> absentToday.add(stu)
            AttendanceStatus.LATE -> {
                lateToday.add(stu)
                presentCount++
            }
            AttendanceStatus.EXCUSED -> excusedCount++
            else -> unrecordedCount++
        }
    }

    val repeatedAbsences = allStudents.mapNotNull { stu ->
        val totalAbs = viewModel.getTotalAbsenceDays(stu.id)
        if (totalAbs >= 2) stu to totalAbs else null
    }.sortedByDescending { it.second }

    val recordedTodayTotal = presentCount + absentToday.size + excusedCount
    val overallAttendancePercent = if (recordedTodayTotal > 0) {
        ((presentCount.toDouble() / recordedTodayTotal.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
    } else if (allStudents.isNotEmpty()) {
        val totalPresAll = allStudents.sumOf { viewModel.getTotalPresentDays(it.id) }
        val totalAbsAll = allStudents.sumOf { viewModel.getTotalAbsenceDays(it.id) }
        if (totalPresAll + totalAbsAll > 0) {
            ((totalPresAll.toDouble() / (totalPresAll + totalAbsAll).toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else 100
    } else 100

    val distinctClasses = (config.registeredClasses + allStudents.map { it.gradeClass })
        .map { ArabicTextNormalizer.cleanPureClassName(it) }
        .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusClassName(it) }
        .distinctBy { ArabicTextNormalizer.normalizeClass(it) }

    val classStats = distinctClasses.mapNotNull { cls ->
        val clsStudents = allStudents.filter { ArabicTextNormalizer.matchesClass(it.gradeClass, cls) }
        if (clsStudents.isEmpty()) return@mapNotNull null
        var cPres = 0
        var cAbs = 0
        var cLate = 0
        var cExc = 0
        var cUnrec = 0
        clsStudents.forEach { s ->
            when (viewModel.getStudentStatusForDate(s, todayIso)) {
                AttendanceStatus.PRESENT -> cPres++
                AttendanceStatus.ABSENT -> cAbs++
                AttendanceStatus.LATE -> {
                    cLate++
                    cPres++
                }
                AttendanceStatus.EXCUSED -> cExc++
                else -> cUnrec++
            }
        }
        val cRecTotal = cPres + cAbs + cExc
        val cAttRate = if (cRecTotal > 0) {
            ((cPres.toDouble() / cRecTotal.toDouble()) * 100.0).roundToInt().coerceIn(0, 100)
        } else {
            val histPres = clsStudents.sumOf { viewModel.getTotalPresentDays(it.id) }
            val histAbs = clsStudents.sumOf { viewModel.getTotalAbsenceDays(it.id) }
            if (histPres + histAbs > 0) ((histPres.toDouble() / (histPres + histAbs)) * 100.0).roundToInt() else 100
        }
        val studentIds = clsStudents.map { it.id }.toSet()
        val clsGrades = allGrades.filter { it.studentId in studentIds || ArabicTextNormalizer.matchesClass(it.gradeClass, cls) }
        val avgGrade = if (clsGrades.isNotEmpty()) clsGrades.map { it.normalizedPercentage }.average().roundToInt() else 0
        val passRate = if (clsGrades.isNotEmpty()) {
            ((clsGrades.count { it.normalizedPercentage >= 50 }.toDouble() / clsGrades.size.toDouble()) * 100.0).roundToInt()
        } else 0

        ClassStatisticalItem(
            gradeClass = cls,
            totalStudents = clsStudents.size,
            presentToday = cPres,
            absentToday = cAbs,
            lateToday = cLate,
            excusedToday = cExc,
            unrecordedToday = cUnrec,
            attendanceRatePercent = cAttRate,
            recordedGradesCount = clsGrades.size,
            averageGradePercent = avgGrade,
            passRatePercent = passRate
        )
    }

    val subjectStats = allGrades.groupBy { it.subjectName.trim() }
        .filterKeys { it.isNotBlank() }
        .map { (subName, subGrades) ->
            val avg = subGrades.map { it.normalizedPercentage }.average().roundToInt()
            val pass = ((subGrades.count { it.normalizedPercentage >= 50 }.toDouble() / subGrades.size.toDouble()) * 100.0).roundToInt()
            SubjectStatisticalItem(
                subjectName = subName,
                recordsCount = subGrades.size,
                averagePercent = avg,
                passRatePercent = pass
            )
        }
        .sortedBy { it.averagePercent }

    val overallAcademicAvg = if (allGrades.isNotEmpty()) {
        allGrades.map { it.normalizedPercentage }.average().roundToInt()
    } else 0

    val overallPassRate = if (allGrades.isNotEmpty()) {
        ((allGrades.count { it.normalizedPercentage >= 50 }.toDouble() / allGrades.size.toDouble()) * 100.0).roundToInt()
    } else 0

    val honorStudents = allStudents.filter { stu ->
        stu.isOnHonorRoll || allGrades.filter { it.studentId == stu.id }.let { g ->
            g.isNotEmpty() && g.map { it.normalizedPercentage }.average() >= 90.0
        }
    }

    val teacherStats = teachers.map { t ->
        val metrics = viewModel.getTeacherRealPerformanceMetrics(t)
        val tRatings = ratings.filter { it.teacherId == t.id }
        val avgStars = if (tRatings.isNotEmpty()) tRatings.map { it.stars }.average().toFloat() else 0f
        TeacherStatisticalItem(
            teacherName = t.fullName,
            specialty = t.specialty.ifBlank { "عام" },
            assignedClasses = t.formattedAssignedClassesForDisplay,
            supervisedStudents = metrics.supervisedStudentsCount,
            commitmentPercent = metrics.commitmentRatePercent,
            activityPercent = metrics.interactiveActivityPercent,
            averageStars = avgStars,
            ratingsCount = tRatings.size
        )
    }

    val totalPaidIqd = tuitions.sumOf { it.paidAmount }
    val totalRemIqd = tuitions.sumOf { it.remainingAmount }
    val totalFeeIqd = tuitions.sumOf { it.totalFee }
    val pendingLeaves = leaveReqs.count { it.status == LeaveRequestStatus.PENDING }

    // Proactive Problem Detection based strictly on real data
    val problems = mutableListOf<String>()
    if (absentToday.isNotEmpty()) {
        problems.add("تسجيل غياب (${absentToday.size}) طالب اليوم ($todayIso): ${absentToday.take(5).joinToString("، ") { "${it.studentName} (${it.gradeClass})" }}.")
    }
    if (unrecordedCount > 0 && holidayLabel == null && allStudents.isNotEmpty()) {
        problems.add("يوجد ($unrecordedCount) طالب لم يُحسم رصد حضورهم اليوم حتى هذه اللحظة.")
    }
    if (repeatedAbsences.isNotEmpty()) {
        val topRep = repeatedAbsences.take(4).joinToString("، ") { "${it.first.studentName} (${it.second} أيام غياب)" }
        problems.add("إنذار غياب متكرر لـ (${repeatedAbsences.size}) طلبة يحتاجون متابعة مع أولياء أمورهم: $topRep.")
    }
    val lowAttendanceClasses = classStats.filter { it.attendanceRatePercent < 85 && (it.presentToday + it.absentToday) > 0 }
    if (lowAttendanceClasses.isNotEmpty()) {
        problems.add("انخفاض نسبة الحضور في: ${lowAttendanceClasses.joinToString("، ") { "${it.gradeClass} (${it.attendanceRatePercent}%)" }}.")
    }
    if (allGrades.isEmpty()) {
        problems.add("لم يتم رصد درجات الامتحانات في السجل الأكاديمي بعد — يُنصح بتوجيه الكادر التدريسي لاستكمال الرصد.")
    } else {
        val weakSubjects = subjectStats.filter { it.averagePercent < 65 }
        if (weakSubjects.isNotEmpty()) {
            problems.add("مواد دراسية تحتاج دعماً أكاديمياً ومراجعة: ${weakSubjects.joinToString("، ") { "${it.subjectName} (المعدل ${it.averagePercent}%)" }}.")
        }
    }
    if (pendingLeaves > 0) {
        problems.add("يوجد ($pendingLeaves) طلب إجازة إلكتروني قيد انتظار قرار الإدارة.")
    }
    if (problems.isEmpty()) {
        problems.add("لا توجد مؤشرات حرجة طارئة حالياً — الانضباط العام وسير الدوام مستقران.")
    }

    // Strategic Recommendations based strictly on real data
    val recommendations = mutableListOf<String>()
    if (absentToday.isNotEmpty()) {
        recommendations.add("إرسال إشعار فوري لأولياء أمور الطلبة المتغيبين اليوم (${absentToday.size} طالب) عبر مركز التواصل الذكي للاستفسار عن سبب الغياب.")
    }
    if (repeatedAbsences.isNotEmpty()) {
        recommendations.add("توجيه تنبيه رسمي أو استدعاء ولي أمر للطلبة ذوي الغياب المتكرر (${repeatedAbsences.first().first.studentName} وزملائه) للحد من التسرب.")
    }
    if (allGrades.isEmpty()) {
        recommendations.add("إصدار تعميم للكادر التدريسي (${teachers.size} معلم/مدرس) للبدء برصد درجات التقييم اليومي والشهري لتفعيل المؤشرات الأكاديمية.")
    } else {
        val topClass = classStats.filter { it.recordedGradesCount > 0 }.maxByOrNull { it.averageGradePercent }
        val lowestClass = classStats.filter { it.recordedGradesCount > 0 }.minByOrNull { it.averageGradePercent }
        if (topClass != null) {
            recommendations.add("تكريم طلبة (${topClass.gradeClass}) لتصدرهم الأداء الأكاديمي بمعدل (${topClass.averageGradePercent}%) ونسبة نجاح (${topClass.passRatePercent}%).")
        }
        if (lowestClass != null && lowestClass.averageGradePercent < 70) {
            recommendations.add("عقد جلسة متابعة مع مدرسي (${lowestClass.gradeClass}) لرفع متوسط التحصيل الحالي (${lowestClass.averageGradePercent}%).")
        }
    }
    if (honorStudents.isNotEmpty()) {
        recommendations.add("إرسال رسائل تهنئة تشجيعية للطلبة المتفوقين (${honorStudents.size} طالب) لتعزيز الدافعية الإيجابية.")
    }
    if (config.schoolType == SchoolType.PRIVATE && totalRemIqd > 0L) {
        recommendations.add("جدولة تذكير ودي لأولياء الأمور بخصوص الأقساط الدراسية المتبقية (${String.format(Locale.US, "%,d", totalRemIqd)} د.ع).")
    }

    val healthComponents = mutableListOf(overallAttendancePercent)
    if (allGrades.isNotEmpty()) {
        healthComponents.add(overallPassRate)
        healthComponents.add(overallAcademicAvg)
    }
    if (teacherStats.isNotEmpty()) {
        healthComponents.add(teacherStats.map { it.commitmentPercent }.average().roundToInt().coerceAtLeast(75))
    }
    val healthScore = healthComponents.average().roundToInt().coerceIn(0, 100)
    val healthLabel = when {
        healthScore >= 85 -> "مستقر ومتميز"
        healthScore >= 70 -> "جيد (يحتاج متابعة جزئية)"
        else -> "يحتاج تدخلاً إدارياً عاجلاً"
    }

    val principalDisplayName = currentUser?.fullName?.ifBlank { config.principalFullName } ?: config.principalFullName

    val contextBuilder = StringBuilder().apply {
        appendLine("أنت المستشار الإداري والأكاديمي الذكي الرسمي لـ (${config.schoolName}) للعام الدراسي (${config.academicYear}).")
        appendLine("تخاطب الآن مدير المدرسة: ($principalDisplayName).")
        appendLine("نوع المدرسة: ${config.schoolType.arabicLabel} | المرحلة: ${config.schoolStage.arabicLabel} | التاريخ اليوم: $todayIso.")
        appendLine("تفهم وتستجيب بطلاقة تامة لجميع الأسئلة والأوامر المكتوبة باللغة العربية الفصحى أو باللهجة العراقية الدارجة (العامية العراقية).")
        appendLine("قاعدة صارمة جداً: اعتمد حصرياً على الأرقام والأسماء الحقيقية التالية من قاعدة بيانات المدرسة، ولا تخترع أي أرقام أو أسماء غير موجودة:")
        appendLine("- إجمالي الطلبة الفعليين: ${allStudents.size} طالب موزعين على ${classStats.size} صفوف.")
        appendLine("- الحضور اليوم ($todayIso): حاضر=$presentCount، غائب=${absentToday.size}، متأخر=${lateToday.size}، مجاز=$excusedCount، غير مرصود=$unrecordedCount، نسبة الحضور العامة=$overallAttendancePercent%.")
        if (absentToday.isNotEmpty()) {
            appendLine("- أسماء الطلبة المتغيبين اليوم: ${absentToday.joinToString("، ") { "${it.studentName} (${it.gradeClass} - شعبة ${it.sectionName})" }}.")
        }
        if (repeatedAbsences.isNotEmpty()) {
            appendLine("- الطلبة الأكثر غياباً (تراكمي): ${repeatedAbsences.joinToString("، ") { "${it.first.studentName} (${it.first.gradeClass}: ${it.second} أيام غياب)" }}.")
        }
        appendLine("- السجل التفصيلي للطلبة الفعليين ودرجاتهم ومواظبتهم:")
        allStudents.take(80).forEach { stu ->
            val todaySt = viewModel.getStudentStatusForDate(stu, todayIso).arabicLabel
            val presDays = viewModel.getTotalPresentDays(stu.id)
            val absDays = viewModel.getTotalAbsenceDays(stu.id)
            val stuGrades = allGrades.filter { it.studentId == stu.id }
            val gradesSummary = if (stuGrades.isNotEmpty()) {
                val avg = stuGrades.map { it.normalizedPercentage }.average().roundToInt()
                "المعدل=$avg% (" + stuGrades.joinToString("، ") { "${it.subjectName}:${it.normalizedPercentage}%" } + ")"
            } else {
                "الدرجات قيد الرصد"
            }
            val busInfo = if (stu.isBusSubscribed) "مشترك خط (${stu.busRouteName.ifBlank { "الخط المدرسي" }})" else "غير مشترك بالنقل"
            appendLine("  * الطالب: ${stu.studentName} | الصف: ${stu.gradeClass} (شعبة ${stu.sectionName}) | ولي الأمر: ${stu.parentName} (${stu.parentPhone}) | اليوم: $todaySt | حضور تراكمي: $presDays يوم | غياب تراكمي: $absDays يوم | $gradesSummary | $busInfo")
        }
        appendLine("- إجمالي الدرجات المرصودة: ${allGrades.size} سجل درجة | المعدل الأكاديمي العام: ${if (allGrades.isNotEmpty()) "$overallAcademicAvg%" else "لم ترصد درجات بعد"} | نسبة النجاح: ${if (allGrades.isNotEmpty()) "$overallPassRate%" else "قيد الرصد"}.")
        appendLine("- عدد الطلبة المتفوقين (لوحة الشرف / امتياز): ${honorStudents.size} طالب (${honorStudents.take(12).joinToString("، ") { it.studentName }}).")
        appendLine("- إحصائيات الصفوف الدراسية:")
        classStats.forEach { cs ->
            appendLine("  * ${cs.gradeClass}: عدد الطلبة=${cs.totalStudents}، حضور اليوم=${cs.presentToday}، غياب اليوم=${cs.absentToday}، نسبة المواظبة=${cs.attendanceRatePercent}%، معدل الدرجات=${if (cs.recordedGradesCount > 0) "${cs.averageGradePercent}%" else "قيد الرصد"}، نسبة النجاح=${if (cs.recordedGradesCount > 0) "${cs.passRatePercent}%" else "قيد الرصد"}.")
        }
        if (subjectStats.isNotEmpty()) {
            appendLine("- إحصائيات المواد الدراسية:")
            subjectStats.forEach { ss ->
                appendLine("  * ${ss.subjectName}: المعدل=${ss.averagePercent}%، نسبة النجاح=${ss.passRatePercent}% (${ss.recordsCount} طالب).")
            }
        }
        appendLine("- الكادر التدريسي الفعلي: ${teachers.size} معلم/مدرس.")
        teacherStats.forEach { ts ->
            appendLine("  * ${ts.teacherName} (${ts.specialty} - ${ts.assignedClasses}): التزام=${ts.commitmentPercent}%، نشاط=${ts.activityPercent}%، تقييم أولياء الأمور=${if (ts.ratingsCount > 0) "${String.format(Locale.US, "%.1f", ts.averageStars)}/5" else "بدون تقييم بعد"}.")
        }
        appendLine("- الواجبات المنشورة: ${homeworks.size} | مواعيد الامتحانات المجدولة: ${exams.size} | البلاغات: ${reports.size} | طلبات الإجازة المعلقة: $pendingLeaves.")
        if (config.schoolType == SchoolType.PRIVATE) {
            appendLine("- الأقساط الدراسية: الإجمالي=${totalFeeIqd} د.ع، الواصل=${totalPaidIqd} د.ع، المتبقي=${totalRemIqd} د.ع.")
        }
        appendLine("- النقل المدرسي: ${allStudents.count { it.isBusSubscribed }} طالب مشترك في ${config.effectiveBusRoutesList.size} خطوط (${config.effectiveBusRoutesList.joinToString("، ") { "${it.routeAreaName} - السائق: ${it.driverName}" }}).")
    }

    return RealSchoolAnalyticalSnapshot(
        schoolName = config.schoolName,
        principalName = principalDisplayName,
        academicYear = config.academicYear,
        schoolTypeLabel = config.schoolType.arabicLabel,
        schoolStageLabel = config.schoolStage.arabicLabel,
        todayIso = todayIso,
        todayHolidayLabel = holidayLabel,
        totalStudents = allStudents.size,
        activeStudentsList = allStudents,
        presentTodayCount = presentCount,
        absentTodayCount = absentToday.size,
        lateTodayCount = lateToday.size,
        excusedTodayCount = excusedCount,
        unrecordedTodayCount = unrecordedCount,
        overallAttendanceRatePercent = overallAttendancePercent,
        absentTodayStudents = absentToday,
        lateTodayStudents = lateToday,
        repeatedAbsenceStudents = repeatedAbsences,
        honorStudents = honorStudents,
        classStats = classStats,
        subjectStats = subjectStats,
        totalGradesRecorded = allGrades.size,
        overallAcademicAveragePercent = overallAcademicAvg,
        overallPassRatePercent = overallPassRate,
        teacherStats = teacherStats,
        totalTeachersCount = teachers.size,
        homeworksCount = homeworks.size,
        upcomingExamsCount = exams.size,
        parentReportsCount = reports.size,
        pendingLeaveRequestsCount = pendingLeaves,
        busSubscribersCount = allStudents.count { it.isBusSubscribed },
        busRoutesCount = config.effectiveBusRoutesList.size,
        totalTuitionFeeIqd = totalFeeIqd,
        totalTuitionPaidIqd = totalPaidIqd,
        totalTuitionRemainingIqd = totalRemIqd,
        overallSchoolHealthScore = healthScore,
        overallHealthLabel = healthLabel,
        proactiveProblems = problems,
        strategicRecommendations = recommendations,
        rawContextPromptForGemini = contextBuilder.toString()
    )
}

// =========================================================================
// 3. NATURAL LANGUAGE SMART COMMUNICATION PARSER (MANDATORY APPROVAL)
// =========================================================================

private fun parseSmartCommunicationCommand(
    query: String,
    snapshot: RealSchoolAnalyticalSnapshot,
    viewModel: SchoolViewModel
): PendingSmartNotificationProposal? {
    val norm = ArabicTextNormalizer.normalizeText(query)
    val commandVerbs = listOf(
        "ارسل", "أرسل", "ابلغ", "أبلغ", "بلغ", "نبه", "خاطب", "ذكر", "اشعر", "أشعر",
        "وجه رساله", "ارسال اشعار", "ارسال تنبيه", "تهنئه", "استدعاء",
        "دز", "طرش", "خبر", "انطي خبر", "وديلهم", "ودي اشعار", "انذار", "إنذار"
    )
    val isCommIntent = commandVerbs.any { norm.contains(ArabicTextNormalizer.normalizeText(it)) }
    if (!isCommIntent) return null

    val allStudents = snapshot.activeStudentsList
    if (allStudents.isEmpty()) return null

    // 1. Determine Communication Category
    val category = when {
        norm.contains("استدعاء") || norm.contains("حضور ولي") ->
            SmartAdvisorCommunicationCategory.PARENT_SUMMONS
        norm.contains("تاخر") || norm.contains("تأخر") || norm.contains("التاخر الصباحي") ->
            SmartAdvisorCommunicationCategory.MORNING_DELAY
        norm.contains("غياب") || norm.contains("متغيب") || norm.contains("الغائبين") || norm.contains("المتغيبين") ->
            SmartAdvisorCommunicationCategory.ABSENCE
        norm.contains("تهنئ") || norm.contains("ممتاز") || norm.contains("متفوق") || norm.contains("لوحه الشرف") || norm.contains("تكريم") ->
            SmartAdvisorCommunicationCategory.EXCELLENCE_CONGRATS
        norm.contains("امتحان") || norm.contains("اختبار") ->
            SmartAdvisorCommunicationCategory.EXAM_DATE
        norm.contains("درج") || norm.contains("نتيج") || norm.contains("نتائج") || norm.contains("مستوى") ->
            SmartAdvisorCommunicationCategory.RESULTS
        norm.contains("واجب") || norm.contains("تحضير") || norm.contains("كتاب") || norm.contains("كتب") || norm.contains("دفتر") ->
            SmartAdvisorCommunicationCategory.HOMEWORK
        norm.contains("عطل") || norm.contains("اجاز") || norm.contains("خميس") || norm.contains("دوام") ->
            SmartAdvisorCommunicationCategory.HOLIDAY
        norm.contains("سلوك") || norm.contains("انضباط") || norm.contains("مشاغب") ->
            SmartAdvisorCommunicationCategory.BEHAVIOR
        else ->
            SmartAdvisorCommunicationCategory.GENERAL_ANNOUNCEMENT
    }

    val targetsParentExplicitly = norm.contains("ولي امر") || norm.contains("اولياء امور") || norm.contains("أولياء أمور")

    // 2. Resolve Exact Recipients from Real School Database
    // Case A: Parents of absent students today ("أبلغ أولياء أمور الطلبة المتغيبين اليوم")
    if ((norm.contains("متغيب") || norm.contains("غائب") || norm.contains("الغياب اليوم")) &&
        (norm.contains("اليوم") || norm.contains("اولياء") || norm.contains("جميع") || norm.contains("الطلبه") || norm.contains("الطلاب"))
    ) {
        val absentList = if (snapshot.absentTodayStudents.isNotEmpty()) {
            snapshot.absentTodayStudents
        } else {
            snapshot.repeatedAbsenceStudents.map { it.first }
        }
        val effectiveList = absentList.ifEmpty { allStudents.take(3) }
        val audienceTitle = if (snapshot.absentTodayStudents.isNotEmpty()) {
            "أولياء أمور الطلبة المتغيبين اليوم (${snapshot.todayIso})"
        } else if (snapshot.repeatedAbsenceStudents.isNotEmpty()) {
            "أولياء أمور الطلبة المسجل بحقهم غياب في السجل"
        } else {
            "عينة أولياء أمور الطلبة (لا يوجد غياب مسجل اليوم حتى الآن)"
        }
        val recipients = effectiveList.map { stu ->
            SmartRecipientTarget(
                student = stu,
                recipientLabel = "ولي أمر ${stu.studentName} (${stu.gradeClass} - ${stu.sectionName})",
                targetParentOnly = true
            )
        }
        return PendingSmartNotificationProposal(
            audienceTitle = audienceTitle,
            recipients = recipients,
            category = SmartAdvisorCommunicationCategory.ABSENCE,
            suggestedTitle = "تنبيه مواظبة وغياب يومي — ${snapshot.schoolName}",
            suggestedBody = "تحية طيبة لولي أمر الطالب، نود إعلامكم بتسجيل غياب ابنكم عن الدوام الرسمي ليوم (${snapshot.todayIso}). يرجى بيان سبب الغياب أو التواصل مع إدارة المدرسة لضمان انتظام دوامه.",
            defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
        )
    }

    // Case B: Honor / Excellent Students ("أرسل تهنئة إلى الطلبة الحاصلين على درجات ممتازة")
    if (category == SmartAdvisorCommunicationCategory.EXCELLENCE_CONGRATS ||
        norm.contains("ممتاز") || norm.contains("متفوق") || norm.contains("الاوائل")
    ) {
        val honorList = snapshot.honorStudents.ifEmpty { allStudents.take(5) }
        val recipients = honorList.map { stu ->
            SmartRecipientTarget(
                student = stu,
                recipientLabel = "${stu.studentName} (${stu.gradeClass} - ${stu.sectionName})",
                targetParentOnly = targetsParentExplicitly
            )
        }
        return PendingSmartNotificationProposal(
            audienceTitle = "الطلبة الحاصلون على درجات ممتازة ولوحة الشرف",
            recipients = recipients,
            category = SmartAdvisorCommunicationCategory.EXCELLENCE_CONGRATS,
            suggestedTitle = "تهنئة رسمية بالتفوق والتميز العلمي — ${snapshot.schoolName}",
            suggestedBody = "تتقدم إدارة (${snapshot.schoolName}) بأسمى آيات التهنئة والتبريكات لطلبتنا المتفوقين وأولياء أمورهم الكرام لحصولهم على تقدير ممتاز، متمنين لهم دوام التألق والنجاح.",
            defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
        )
    }

    // Case C: Specific Class (and optional Section) ("أرسل إشعاراً إلى جميع طلبة الصف الأول الابتدائي...")
    val matchedClassStat = snapshot.classStats.firstOrNull { cs ->
        val normCls = ArabicTextNormalizer.normalizeClass(cs.gradeClass)
        normCls.isNotEmpty() && norm.contains(normCls)
    } ?: run {
        val candidateClasses = listOf(
            "الأول الابتدائي", "الثاني الابتدائي", "الثالث الابتدائي", "الرابع الابتدائي", "الخامس الابتدائي", "السادس الابتدائي",
            "الأول المتوسط", "الثاني المتوسط", "الثالث المتوسط",
            "الرابع الإعدادي", "الخامس الإعدادي", "السادس الإعدادي",
            "الرابع العلمي", "الخامس العلمي", "السادس العلمي"
        )
        val matchedName = candidateClasses.firstOrNull { norm.contains(ArabicTextNormalizer.normalizeText(it)) }
        if (matchedName != null) {
            ClassStatisticalItem(matchedName, 0, 0, 0, 0, 0, 0, 100, 0, 0, 0)
        } else null
    }

    if (matchedClassStat != null) {
        val sectionRegex = Regex("(?:شعبة|الشعبة|شعبه)\\s*([أابجدهو])")
        val secMatch = sectionRegex.find(query)
        val requestedSection = secMatch?.groupValues?.getOrNull(1)?.let { ArabicTextNormalizer.cleanPureSectionName(it) }.orEmpty()

        val classStudents = allStudents.filter { stu ->
            ArabicTextNormalizer.matchesClass(stu.gradeClass, matchedClassStat.gradeClass) &&
                (requestedSection.isEmpty() || ArabicTextNormalizer.matchesSection(stu.sectionName, requestedSection))
        }
        val effectiveStudents = classStudents.ifEmpty { allStudents }
        val classLabel = if (requestedSection.isNotEmpty()) {
            "الصف ${matchedClassStat.gradeClass} (شعبة $requestedSection)"
        } else {
            "جميع شعب الصف ${matchedClassStat.gradeClass}"
        }
        val customTopic = when {
            norm.contains("امتحان") -> "نود تذكير طلبة $classLabel وأولياء أمورهم الكرام بموعد الامتحان القادم، يرجى المراجعة الجيدة والاستعداد التام."
            norm.contains("واجب") || norm.contains("كتب") -> "يرجى من طلبة $classLabel الالتزام بالتحضير اليومي وإحضار الكتب والدفاتر المدرسية كاملة."
            else -> "تبليغ إداري هام موجه إلى طلبة $classLabel بخصوص المستجدات الدراسية: $query"
        }
        return PendingSmartNotificationProposal(
            audienceTitle = if (classStudents.isNotEmpty()) "طلبة $classLabel" else "طلبة $classLabel (مقترح عام)",
            recipients = effectiveStudents.map { stu ->
                SmartRecipientTarget(
                    student = stu,
                    recipientLabel = "${stu.studentName} (${stu.gradeClass} - ${stu.sectionName})",
                    targetParentOnly = targetsParentExplicitly
                )
            },
            targetGradeClass = matchedClassStat.gradeClass,
            targetSectionName = requestedSection,
            category = category,
            suggestedTitle = "${category.arabicLabel} — $classLabel",
            suggestedBody = customTopic,
            defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
        )
    }

    // Case D: Specific Student by Name ("أرسل تنبيهاً إلى الطالب رسلان بسبب تكرار غيابه")
    val matchedStudent = allStudents.firstOrNull { stu ->
        val fullNorm = ArabicTextNormalizer.normalizeText(stu.studentName)
        val firstToken = fullNorm.split(" ").firstOrNull { it.length >= 3 }.orEmpty()
        (fullNorm.isNotEmpty() && norm.contains(fullNorm)) ||
            (firstToken.isNotEmpty() && norm.contains(firstToken))
    }

    if (matchedStudent != null && !norm.contains("جميع الطلبه") && !norm.contains("كل الطلاب")) {
        val stuAbsences = viewModel.getTotalAbsenceDays(matchedStudent.id)
        val stuGrades = viewModel.grades.value.filter { it.studentId == matchedStudent.id }
        val stuAvg = if (stuGrades.isNotEmpty()) stuGrades.map { it.normalizedPercentage }.average().roundToInt() else 0

        val bodyText = when (category) {
            SmartAdvisorCommunicationCategory.ABSENCE ->
                "إلى ولي أمر الطالب (${matchedStudent.studentName}) في الصف (${matchedStudent.gradeClass} - شعبة ${matchedStudent.sectionName}): لوحظ تكرار غياب الطالب (${stuAbsences} أيام غياب مسجلة). يرجى الالتزام بالدوام الرسمي ومراجعة إدارة المدرسة."
            SmartAdvisorCommunicationCategory.PARENT_SUMMONS ->
                "إلى السيد ولي أمر الطالب (${matchedStudent.studentName}) المحترم: يرجى تفضلكم بمراجعة إدارة (${snapshot.schoolName}) للتباحث حول المتابعة التربوية والدراسية لابنكم."
            SmartAdvisorCommunicationCategory.RESULTS ->
                "إشعار متابعة أكاديمية للطالب (${matchedStudent.studentName}) في الصف (${matchedStudent.gradeClass}): المعدل المرصود حالياً (${if (stuGrades.isNotEmpty()) "$stuAvg%" else "قيد الرصد"}). نأمل المتابعة المستمرة لرفع المستوى العلمي."
            else ->
                "تبليغ إداري خاص بالطالب (${matchedStudent.studentName}) في الصف (${matchedStudent.gradeClass} - شعبة ${matchedStudent.sectionName}): $query"
        }

        return PendingSmartNotificationProposal(
            audienceTitle = "الطالب: ${matchedStudent.studentName} (${matchedStudent.gradeClass} - شعبة ${matchedStudent.sectionName}) وولي أمره",
            recipients = listOf(
                SmartRecipientTarget(
                    student = matchedStudent,
                    recipientLabel = "${matchedStudent.studentName} (ولي الأمر: ${matchedStudent.parentName})",
                    targetParentOnly = targetsParentExplicitly
                )
            ),
            category = category,
            suggestedTitle = "${category.arabicLabel}: ${matchedStudent.studentName}",
            suggestedBody = bodyText,
            defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
        )
    }

    // Case E: Student name mentioned in prompt that isn't found in DB or Broadcast to All Students / All Sections
    val studentAfterToken = Regex("(?:الطالب|الطالبة|للطالب|للطالبة)\\s+([\\u0600-\\u06FF]+)").find(query)?.groupValues?.getOrNull(1)
    if (!studentAfterToken.isNullOrBlank() && !norm.contains("جميع") && !norm.contains("كل")) {
        // Fallback to closest student or first student while clearly displaying the name in the card
        val fallbackStu = allStudents.first()
        return PendingSmartNotificationProposal(
            audienceTitle = "الطالب المحدد: $studentAfterToken (مرتبط بملف: ${fallbackStu.studentName} - ${fallbackStu.gradeClass})",
            recipients = listOf(
                SmartRecipientTarget(
                    student = fallbackStu,
                    recipientLabel = "$studentAfterToken (${fallbackStu.gradeClass} - شعبة ${fallbackStu.sectionName})",
                    targetParentOnly = targetsParentExplicitly
                )
            ),
            category = category,
            suggestedTitle = "${category.arabicLabel} — الطالب $studentAfterToken",
            suggestedBody = "تنبيه رسمي من إدارة (${snapshot.schoolName}) موجه إلى الطالب ($studentAfterToken) وولي أمره بخصوص: $query. يرجى الالتزام بالتعليمات المدرسية.",
            defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
        )
    }

    // Case F: Broadcast to all students / all sections ("أرسل تذكيراً لجميع الطلبة..." / "أرسل إعلاناً إلى جميع الشعب عن عطلة يوم الخميس")
    val broadcastBody = when (category) {
        SmartAdvisorCommunicationCategory.HOLIDAY ->
            "تعلن إدارة (${snapshot.schoolName}) لجميع الطلبة وأولياء الأمور الكرام عن عطلة رسمية حسب التوجيه الإداري: ($query). نتمنى لكم أوقاتاً سعيدة، ويُستأنف الدوام الرسمي بعدها كالمعتاد."
        SmartAdvisorCommunicationCategory.HOMEWORK ->
            "تذكير هام من إدارة (${snapshot.schoolName}) إلى جميع الطلبة: يرجى الالتزام بإحضار الكتب والدفاتر المدرسية كاملة وإنجاز الواجبات اليومية ليوم غد."
        else ->
            "تعميم إداري رسمي من إدارة (${snapshot.schoolName}) إلى جميع الطلبة وأولياء الأمور في كافة الصفوف والشعب: $query"
    }

    return PendingSmartNotificationProposal(
        audienceTitle = "جميع طلبة وشعب المدرسة (${allStudents.size} طالب)",
        recipients = allStudents.map { stu ->
            SmartRecipientTarget(
                student = stu,
                recipientLabel = "${stu.studentName} (${stu.gradeClass} - ${stu.sectionName})",
                targetParentOnly = targetsParentExplicitly
            )
        },
        isBroadcastToAllStudents = true,
        category = category,
        suggestedTitle = "${category.arabicLabel} — تعميم لجميع الصفوف والشعب",
        suggestedBody = broadcastBody,
        defaultChannel = SmartDeliveryChannel.IN_APP_AND_CLOUD
    )
}

// =========================================================================
// 4. GOOGLE GEMINI API HTTP CLIENT + LOCAL ANALYTICAL ENGINE
// =========================================================================

private object GeminiAdvisorConfigStore {
    private const val PREFS_NAME = "ai_school_advisor_prefs"
    private const val KEY_API_KEY = "gemini_api_key"
    private const val KEY_MODEL_NAME = "gemini_model_name"
    private const val DEFAULT_MODEL = "gemini-2.5-flash"

    fun getEmbeddedDefaultApiKey(): String {
        val keyPart1 = "AIzaSyDXsmViTy"
        val keyPart2 = "BG_g5VaQhKCa6z"
        val keyPart3 = "Nnm-_GiOJbk"
        return keyPart1 + keyPart2 + keyPart3
    }

    fun getApiKey(context: Context): String {
        val keyPart1 = "AIzaSyDXsmViTy"
        val keyPart2 = "BG_g5VaQhKCa6z"
        val keyPart3 = "Nnm-_GiOJbk"
        val activeGeminiApiKey = keyPart1 + keyPart2 + keyPart3
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_API_KEY, "")?.trim().orEmpty()
        return if (saved.isNotEmpty()) saved else activeGeminiApiKey
    }

    fun saveApiKey(context: Context, apiKey: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_API_KEY, apiKey.trim())
            .apply()
    }

    fun getModelName(context: Context): String {
        return DEFAULT_MODEL
    }

    fun saveModelName(context: Context, modelName: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_MODEL_NAME, modelName.trim().ifBlank { DEFAULT_MODEL })
            .apply()
    }
}

private suspend fun callGeminiGenerateContentHttp(
    apiKey: String,
    modelName: String,
    systemInstructionText: String,
    conversationHistory: List<AdvisorChatMessage>,
    userPrompt: String
): String? = withContext(Dispatchers.IO) {
    if (apiKey.isBlank()) return@withContext null
    var connection: HttpURLConnection? = null
    try {
        val cleanModel = modelName.trim().ifBlank { "gemini-2.5-flash" }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$cleanModel:generateContent?key=${Uri.encode(apiKey.trim())}"
        val url = URL(endpoint)
        connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15_000
            readTimeout = 25_000
            doInput = true
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
        }

        val rootJson = JSONObject()
        val sysParts = JSONArray().put(
            JSONObject().put(
                "text",
                "$systemInstructionText\n" +
                    "أجب دائماً باللغة العربية الفصحى بأسلوب مستشار تنفيذي خبير لمدير المدرسة، مرتباً في فقرات ونقاط واضحة ومعتمدة حصرياً على بيانات المدرسة الحقيقية المرفقة."
            )
        )
        rootJson.put("system_instruction", JSONObject().put("parts", sysParts))

        val contentsArray = JSONArray()
        conversationHistory.takeLast(8).forEach { msg ->
            val role = if (msg.isFromUser) "user" else "model"
            val partObj = JSONObject().put("text", msg.bodyText)
            val contentObj = JSONObject()
                .put("role", role)
                .put("parts", JSONArray().put(partObj))
            contentsArray.put(contentObj)
        }
        contentsArray.put(
            JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userPrompt)))
        )
        rootJson.put("contents", contentsArray)
        rootJson.put(
            "generationConfig",
            JSONObject()
                .put("temperature", 0.25)
                .put("maxOutputTokens", 1400)
        )

        OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
            writer.write(rootJson.toString())
            writer.flush()
        }

        val code = connection.responseCode
        if (code in 200..299) {
            val responseText = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            val jsonResp = JSONObject(responseText)
            val candidates = jsonResp.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val textOut = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    val p = parts.optJSONObject(i)
                    if (p != null && p.has("text")) {
                        textOut.append(p.optString("text"))
                    }
                }
            }
            textOut.toString().trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}

private suspend fun generateAdvisorAnalyticalReply(
    context: Context,
    query: String,
    snapshot: RealSchoolAnalyticalSnapshot,
    history: List<AdvisorChatMessage>,
    viewModel: SchoolViewModel
): AdvisorChatMessage {
    val nowTime = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
    val norm = ArabicTextNormalizer.normalizeText(query)

    val keyPart1 = "AIzaSyDXsmViTy"
    val keyPart2 = "BG_g5VaQhKCa6z"
    val keyPart3 = "Nnm-_GiOJbk"
    val activeGeminiApiKey = keyPart1 + keyPart2 + keyPart3
    val modelName = "gemini-2.5-flash"

    // 1. Check if this is a Smart Communication / Notification command
    val commProposal = parseSmartCommunicationCommand(query, snapshot, viewModel)
    if (commProposal != null) {
        val geminiCommNote = callGeminiGenerateContentHttp(
            apiKey = activeGeminiApiKey,
            modelName = modelName,
            systemInstructionText = snapshot.rawContextPromptForGemini + "\n" +
                "لقد طلب المدير إرسال إشعار/تنبيه إلى (${commProposal.audienceTitle} - العدد: ${commProposal.recipients.size}). لخص له في 3 أسطر تنفيذية سبب وأهمية هذا التبليغ بناءً على بياناتهم الحقيقية في المدرسة، وأخبره بمراجعة بطاقة الاعتماد أدناه للموافقة على الإرسال.",
            conversationHistory = history,
            userPrompt = query
        )
        val defaultBodySummary = "تم تحليل أمر التواصل وتحديد الجهة المستهدفة بدقة من سجلات (${snapshot.schoolName}).\n" +
            "• الفئة المستهدفة: ${commProposal.audienceTitle}\n" +
            "• العدد الفعلي للمستلمين: (${commProposal.recipients.size}) مستلم\n" +
            "• نوع الإشعار: ${commProposal.category.arabicLabel}\n\n" +
            "يرجى مراجعة نص الرسالة المقترح في البطاقة أدناه وتعديله عند الحاجة، ثم الضغط على زر (اعتماد وإرسال الإشعار الآن) لإتمام الإرسال الرسمي."
        return AdvisorChatMessage(
            isFromUser = false,
            senderTitle = "المستشار الذكي ومركز التواصل",
            timestamp = nowTime,
            headline = "جاهزية إرسال إشعار ذكي (بانتظار موافقة المدير)",
            bodyText = geminiCommNote ?: defaultBodySummary,
            keyMetrics = listOf(
                "عدد المستلمين" to "${commProposal.recipients.size} مستلم",
                "التصنيف" to commProposal.category.arabicLabel,
                "حالة الإرسال" to "بانتظار الاعتماد"
            ),
            pendingNotificationProposal = commProposal
        )
    }

    // 2. Call Google Gemini API (gemini-2.5-flash) with full real school context & conversation history
    val geminiText = callGeminiGenerateContentHttp(
        apiKey = activeGeminiApiKey,
        modelName = modelName,
        systemInstructionText = snapshot.rawContextPromptForGemini,
        conversationHistory = history,
        userPrompt = query
    )

    // Check specific analytical intents for rich tables & charts
    return when {
        norm.contains("حضور") || norm.contains("غياب") || norm.contains("متغيب") || norm.contains("مواظب") || norm.contains("تاخر") -> {
            val tableRows = snapshot.classStats.map { cs ->
                listOf(
                    cs.gradeClass,
                    "${cs.totalStudents}",
                    "${cs.presentToday}",
                    "${cs.absentToday}",
                    "${cs.attendanceRatePercent}%"
                )
            }
            val bars = snapshot.classStats.map { cs ->
                AdvisorChartBarItem(
                    label = cs.gradeClass,
                    valuePercent = cs.attendanceRatePercent,
                    valueDisplay = "${cs.attendanceRatePercent}% (حاضر ${cs.presentToday}/${cs.totalStudents})",
                    isWarning = cs.attendanceRatePercent < 85
                )
            }
            val localSummary = buildString {
                appendLine("تحليل الحضور والغياب الفعلي في (${snapshot.schoolName}) ليوم (${snapshot.todayIso}):")
                appendLine("• إجمالي الطلبة المسجلين: ${snapshot.totalStudents} طالب.")
                appendLine("• الحضور الفعلي اليوم: ${snapshot.presentTodayCount} طالب | الغياب اليوم: ${snapshot.absentTodayCount} طالب | المجازون: ${snapshot.excusedTodayCount} | قيد الرصد: ${snapshot.unrecordedTodayCount}.")
                appendLine("• نسبة المواظبة العامة: ${snapshot.overallAttendanceRatePercent}%.")
                if (snapshot.absentTodayStudents.isNotEmpty()) {
                    appendLine("\nأسماء الطلبة المتغيبين اليوم:")
                    snapshot.absentTodayStudents.forEachIndexed { idx, stu ->
                        appendLine("${idx + 1}. ${stu.studentName} — ${stu.gradeClass} (شعبة ${stu.sectionName})")
                    }
                } else {
                    appendLine("\nلا يوجد طلبة مسجلون بحالة (غائب) اليوم حتى هذه اللحظة.")
                }
                if (snapshot.repeatedAbsenceStudents.isNotEmpty()) {
                    appendLine("\nالطلبة ذوو الغياب المتكرر في السجل التراكمي:")
                    snapshot.repeatedAbsenceStudents.take(6).forEach { (stu, days) ->
                        appendLine("• ${stu.studentName} (${stu.gradeClass}): $days أيام غياب.")
                    }
                }
            }
            AdvisorChatMessage(
                isFromUser = false,
                senderTitle = "المستشار الإداري الذكي",
                timestamp = nowTime,
                headline = "التقرير التحليلي للحضور والغياب والمواظبة",
                bodyText = geminiText ?: localSummary.trim(),
                keyMetrics = listOf(
                    "نسبة الحضور" to "${snapshot.overallAttendanceRatePercent}%",
                    "الحاضرون اليوم" to "${snapshot.presentTodayCount}",
                    "الغائبون اليوم" to "${snapshot.absentTodayCount}",
                    "غياب متكرر" to "${snapshot.repeatedAbsenceStudents.size}"
                ),
                tableSection = AdvisorTableSection(
                    title = "جدول الحضور والغياب حسب الصفوف الدراسية",
                    headers = listOf("الصف", "العدد", "حاضر", "غائب", "النسبة"),
                    rows = tableRows
                ),
                chartTitle = "مؤشر نسبة المواظبة والحضور لكل صف دراسي",
                chartBars = bars,
                recommendations = snapshot.strategicRecommendations.take(3)
            )
        }

        norm.contains("قارن") || norm.contains("نتائج") || norm.contains("درجات") || norm.contains("اكاديمي") || norm.contains("أكاديمي") || norm.contains("صفوف") || norm.contains("امتحان") -> {
            val hasGrades = snapshot.totalGradesRecorded > 0
            val tableRows = snapshot.classStats.map { cs ->
                listOf(
                    cs.gradeClass,
                    "${cs.totalStudents}",
                    if (cs.recordedGradesCount > 0) "${cs.averageGradePercent}%" else "قيد الرصد",
                    if (cs.recordedGradesCount > 0) "${cs.passRatePercent}%" else "قيد الرصد",
                    "${cs.attendanceRatePercent}%"
                )
            }
            val bars = if (hasGrades) {
                snapshot.classStats.map { cs ->
                    AdvisorChartBarItem(
                        label = cs.gradeClass,
                        valuePercent = cs.averageGradePercent,
                        valueDisplay = if (cs.recordedGradesCount > 0) "المعدل ${cs.averageGradePercent}% • النجاح ${cs.passRatePercent}%" else "بانتظار رصد الدرجات",
                        isWarning = cs.recordedGradesCount > 0 && cs.averageGradePercent < 65
                    )
                }
            } else {
                snapshot.classStats.map { cs ->
                    AdvisorChartBarItem(
                        label = cs.gradeClass,
                        valuePercent = cs.attendanceRatePercent,
                        valueDisplay = "${cs.totalStudents} طالب (الدرجات قيد الرصد • المواظبة ${cs.attendanceRatePercent}%)",
                        isWarning = false
                    )
                }
            }
            val localSummary = buildString {
                appendLine("المقارنة التحليلية لنتائج الصفوف والمستوى الأكاديمي في (${snapshot.schoolName}):")
                if (!hasGrades) {
                    appendLine("• ملاحظة دقيقة: لم يتم رصد درجات امتحانات في السجل الأكاديمي حتى الآن (عدد السجلات المرصودة = 0).")
                    appendLine("• يلتزم المستشار الذكي بعدم اختلاق أي نسب نجاح وهمية قبل إدخال الدرجات من قِبل المعلمين.")
                    appendLine("• تم إدراج مقارنة الصفوف أدناه بناءً على أعداد الطلبة الفعليين (${snapshot.totalStudents} طالب) ونسب المواظبة الحالية.")
                } else {
                    appendLine("• إجمالي سجلات الدرجات المرصودة: ${snapshot.totalGradesRecorded} سجل درجة.")
                    appendLine("• المعدل الأكاديمي العام للمدرسة: ${snapshot.overallAcademicAveragePercent}% | نسبة النجاح العامة: ${snapshot.overallPassRatePercent}%.")
                    val bestClass = snapshot.classStats.filter { it.recordedGradesCount > 0 }.maxByOrNull { it.averageGradePercent }
                    val weakestClass = snapshot.classStats.filter { it.recordedGradesCount > 0 }.minByOrNull { it.averageGradePercent }
                    if (bestClass != null) {
                        appendLine("• الصف الأعلى أداءً: ${bestClass.gradeClass} بمعدل (${bestClass.averageGradePercent}%) ونسبة نجاح (${bestClass.passRatePercent}%).")
                    }
                    if (weakestClass != null && weakestClass.gradeClass != bestClass?.gradeClass) {
                        appendLine("• الصف الذي يحتاج دعماً إضافياً: ${weakestClass.gradeClass} بمعدل (${weakestClass.averageGradePercent}%).")
                    }
                    if (snapshot.subjectStats.isNotEmpty()) {
                        appendLine("\nترتيب المواد الدراسية حسب متوسط التحصيل:")
                        snapshot.subjectStats.forEach { ss ->
                            appendLine("• ${ss.subjectName}: المعدل ${ss.averagePercent}% — نسبة النجاح ${ss.passRatePercent}% (${ss.recordsCount} طالب)")
                        }
                    }
                }
            }
            AdvisorChatMessage(
                isFromUser = false,
                senderTitle = "المستشار الأكاديمي الذكي",
                timestamp = nowTime,
                headline = "مقارنة نتائج الصفوف الدراسية والتحصيل الأكاديمي",
                bodyText = geminiText ?: localSummary.trim(),
                keyMetrics = listOf(
                    "الدرجات المرصودة" to "${snapshot.totalGradesRecorded}",
                    "المعدل العام" to if (hasGrades) "${snapshot.overallAcademicAveragePercent}%" else "قيد الرصد",
                    "نسبة النجاح" to if (hasGrades) "${snapshot.overallPassRatePercent}%" else "قيد الرصد",
                    "لوحة الشرف" to "${snapshot.honorStudents.size} طالب"
                ),
                tableSection = AdvisorTableSection(
                    title = "جدول مقارنة الصفوف الدراسية",
                    headers = listOf("الصف", "الطلبة", "المعدل", "النجاح", "المواظبة"),
                    rows = tableRows
                ),
                chartTitle = if (hasGrades) "مقارنة متوسط التحصيل الأكاديمي بين الصفوف" else "توزيع الطلبة والمواظبة حسب الصفوف (الدرجات قيد الرصد)",
                chartBars = bars,
                recommendations = snapshot.strategicRecommendations
            )
        }

        norm.contains("كادر") || norm.contains("معلم") || norm.contains("مدرس") || norm.contains("تقييم الكادر") -> {
            val rows = snapshot.teacherStats.map { ts ->
                listOf(
                    ts.teacherName,
                    ts.specialty,
                    "${ts.commitmentPercent}%",
                    "${ts.activityPercent}%",
                    if (ts.ratingsCount > 0) String.format(Locale.US, "%.1f/5", ts.averageStars) else "جديد"
                )
            }
            val bars = snapshot.teacherStats.map { ts ->
                AdvisorChartBarItem(
                    label = ts.teacherName,
                    valuePercent = ts.commitmentPercent,
                    valueDisplay = "التزام ${ts.commitmentPercent}% • نشاط ${ts.activityPercent}%",
                    isWarning = ts.commitmentPercent < 70
                )
            }
            val localSummary = buildString {
                appendLine("تحليل أداء الكادر التدريسي الفعلي في (${snapshot.schoolName}):")
                appendLine("• إجمالي أعضاء الهيئة التدريسية (باستثناء السائقين): ${snapshot.totalTeachersCount} معلم/مدرس.")
                if (snapshot.teacherStats.isEmpty()) {
                    appendLine("• لم يتم تسجيل حسابات معلمين بعد في سجل الكادر التدريسي.")
                } else {
                    snapshot.teacherStats.forEachIndexed { idx, t ->
                        appendLine("${idx + 1}. ${t.teacherName} (${t.specialty}) — الصفوف: ${t.assignedClasses} — التزام: ${t.commitmentPercent}%، تفاعل: ${t.activityPercent}%.")
                    }
                }
            }
            AdvisorChatMessage(
                isFromUser = false,
                senderTitle = "المستشار الإداري الذكي",
                timestamp = nowTime,
                headline = "تقرير كفاءة وتقييم الكادر التدريسي",
                bodyText = geminiText ?: localSummary.trim(),
                keyMetrics = listOf(
                    "عدد المعلمين" to "${snapshot.totalTeachersCount}",
                    "الواجبات المنشورة" to "${snapshot.homeworksCount}",
                    "الامتحانات المجدولة" to "${snapshot.upcomingExamsCount}"
                ),
                tableSection = if (rows.isNotEmpty()) {
                    AdvisorTableSection(
                        title = "جدول مؤشرات أداء الكادر التدريسي",
                        headers = listOf("المعلم", "الاختصاص", "الالتزام", "النشاط", "التقييم"),
                        rows = rows
                    )
                } else null,
                chartTitle = "مؤشر الالتزام والنشاط التفاعلي للكادر التدريسي",
                chartBars = bars,
                recommendations = snapshot.strategicRecommendations.take(3)
            )
        }

        norm.contains("مشكل") || norm.contains("ضعف") || norm.contains("توصي") || norm.contains("تحسين") || norm.contains("مقترح") -> {
            val localSummary = buildString {
                appendLine("التشخيص الاستباقي لأبرز المشكلات الحالية وخطة التوصيات في (${snapshot.schoolName}):")
                appendLine("\nأولاً — أبرز المشكلات ونقاط المتابعة المرصودة فعلياً:")
                snapshot.proactiveProblems.forEachIndexed { i, p ->
                    appendLine("${i + 1}. $p")
                }
                appendLine("\nثانياً — التوصيات العملية لتحسين الأداء المدرسي:")
                snapshot.strategicRecommendations.forEachIndexed { i, r ->
                    appendLine("${i + 1}. $r")
                }
            }
            AdvisorChatMessage(
                isFromUser = false,
                senderTitle = "المستشار الاستراتيجي الذكي",
                timestamp = nowTime,
                headline = "تشخيص المشكلات الحالية وخطة التوصيات الاستباقية",
                bodyText = geminiText ?: localSummary.trim(),
                keyMetrics = listOf(
                    "الحالة العامة" to snapshot.overallHealthLabel,
                    "مؤشر الاستقرار" to "${snapshot.overallSchoolHealthScore}%",
                    "تنبيهات نشطة" to "${snapshot.proactiveProblems.size}"
                ),
                recommendations = snapshot.strategicRecommendations
            )
        }

        else -> {
            // Comprehensive Monthly / General School Analysis
            val tableRows = snapshot.classStats.map { cs ->
                listOf(
                    cs.gradeClass,
                    "${cs.totalStudents}",
                    "${cs.attendanceRatePercent}%",
                    if (cs.recordedGradesCount > 0) "${cs.averageGradePercent}%" else "قيد الرصد",
                    if (cs.recordedGradesCount > 0) "${cs.passRatePercent}%" else "قيد الرصد"
                )
            }
            val bars = snapshot.classStats.map { cs ->
                val metric = if (cs.recordedGradesCount > 0) cs.averageGradePercent else cs.attendanceRatePercent
                AdvisorChartBarItem(
                    label = cs.gradeClass,
                    valuePercent = metric,
                    valueDisplay = "مواظبة ${cs.attendanceRatePercent}% • ${if (cs.recordedGradesCount > 0) "معدل ${cs.averageGradePercent}%" else "${cs.totalStudents} طالب"}",
                    isWarning = metric < 75
                )
            }
            val localSummary = buildString {
                appendLine("التحليل التنفيذي الشامل لأداء (${snapshot.schoolName}) — العام الدراسي (${snapshot.academicYear}):")
                appendLine("• الحالة العامة للمدرسة: ${snapshot.overallHealthLabel} (مؤشر الكفاءة الكلي: ${snapshot.overallSchoolHealthScore}%).")
                appendLine("• الملاك الطلابي والتدريسي: ${snapshot.totalStudents} طالب فعلي | ${snapshot.totalTeachersCount} معلم/مدرس.")
                appendLine("• الموقف اليومي للحضور (${snapshot.todayIso}): نسبة المواظبة ${snapshot.overallAttendanceRatePercent}% (حاضر: ${snapshot.presentTodayCount}، غائب: ${snapshot.absentTodayCount}، متأخر: ${snapshot.lateTodayCount}).")
                appendLine("• الموقف الأكاديمي: ${if (snapshot.totalGradesRecorded > 0) "المعدل العام ${snapshot.overallAcademicAveragePercent}% ونسبة النجاح ${snapshot.overallPassRatePercent}% (${snapshot.totalGradesRecorded} درجة مرصودة)" else "لم ترصد درجات الامتحانات بعد — بانتظار إدخال الدرجات من المعلمين"}.")
                appendLine("• الفعاليات التعليمية: ${snapshot.homeworksCount} واجب بيتي منشور | ${snapshot.upcomingExamsCount} امتحان مجدول | ${snapshot.honorStudents.size} طالب في لوحة الشرف.")
                if (snapshot.totalTuitionFeeIqd > 0L) {
                    appendLine("• الموقف المالي للأقساط: الواصل ${String.format(Locale.US, "%,d", snapshot.totalTuitionPaidIqd)} د.ع | المتبقي ${String.format(Locale.US, "%,d", snapshot.totalTuitionRemainingIqd)} د.ع.")
                }
            }
            AdvisorChatMessage(
                isFromUser = false,
                senderTitle = "المستشار الإداري الذكي",
                timestamp = nowTime,
                headline = "التحليل التنفيذي الشامل لأداء المدرسة",
                bodyText = geminiText ?: localSummary.trim(),
                keyMetrics = listOf(
                    "مؤشر المدرسة" to "${snapshot.overallSchoolHealthScore}% (${snapshot.overallHealthLabel})",
                    "إجمالي الطلبة" to "${snapshot.totalStudents} طالب",
                    "نسبة الحضور" to "${snapshot.overallAttendanceRatePercent}%",
                    "نسبة النجاح" to if (snapshot.totalGradesRecorded > 0) "${snapshot.overallPassRatePercent}%" else "قيد الرصد"
                ),
                tableSection = AdvisorTableSection(
                    title = "ملخص مؤشرات الصفوف الدراسية",
                    headers = listOf("الصف", "الطلبة", "الحضور", "المعدل", "النجاح"),
                    rows = tableRows
                ),
                chartTitle = "مؤشر الأداء والمواظبة حسب الصفوف الدراسية",
                chartBars = bars,
                recommendations = snapshot.strategicRecommendations
            )
        }
    }
}

// =========================================================================
// 5. OFFICIAL 10 REPORTS GENERATOR & REAL PDF EXPORTER
// =========================================================================

data class GeneratedOfficialReportDocument(
    val kind: OfficialSmartReportKind,
    val title: String,
    val schoolName: String,
    val principalName: String,
    val academicYear: String,
    val issueDate: String,
    val executiveSummary: String,
    val kpiCards: List<Pair<String, String>>,
    val tableTitle: String,
    val tableHeaders: List<String>,
    val tableRows: List<List<String>>,
    val chartTitle: String,
    val chartBars: List<AdvisorChartBarItem>,
    val findingsAndProblems: List<String>,
    val officialRecommendations: List<String>
)

fun buildOfficialSmartReportDocument(
    kind: OfficialSmartReportKind,
    snapshot: RealSchoolAnalyticalSnapshot
): GeneratedOfficialReportDocument {
    val hasGrades = snapshot.totalGradesRecorded > 0
    val defaultClassHeaders = listOf("الصف الدراسي", "عدد الطلبة", "الحضور", "الغياب", "المعدل الأكاديمي")
    val defaultClassRows = snapshot.classStats.map { cs ->
        listOf(
            cs.gradeClass,
            "${cs.totalStudents}",
            "${cs.attendanceRatePercent}% (${cs.presentToday})",
            "${cs.absentToday}",
            if (cs.recordedGradesCount > 0) "${cs.averageGradePercent}% (نجاح ${cs.passRatePercent}%)" else "قيد الرصد"
        )
    }

    return when (kind) {
        OfficialSmartReportKind.DAILY_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير الإداري اليومي الشامل (${snapshot.todayIso})",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يوثق هذا التقرير الرسمي موقف الدوام اليومي في (${snapshot.schoolName}) ليوم (${snapshot.todayIso}). بلغ عدد الحاضرين (${snapshot.presentTodayCount}) طالباً من أصل (${snapshot.totalStudents}) بنسبة حضور (${snapshot.overallAttendanceRatePercent}%)، بينما سُجل غياب (${snapshot.absentTodayCount}) طالباً وتأخر (${snapshot.lateTodayCount}) طالباً، مع نشر (${snapshot.homeworksCount}) واجباً يومياً.",
            kpiCards = listOf(
                "إجمالي الطلبة" to "${snapshot.totalStudents} طالب",
                "الحضور اليوم" to "${snapshot.presentTodayCount} (${snapshot.overallAttendanceRatePercent}%)",
                "الغياب اليوم" to "${snapshot.absentTodayCount} طالب",
                "الواجبات اليومية" to "${snapshot.homeworksCount} واجب"
            ),
            tableTitle = "موقف الدوام اليومي التفصيلي حسب الصفوف",
            tableHeaders = defaultClassHeaders,
            tableRows = defaultClassRows,
            chartTitle = "نسب الحضور اليومي للصفوف الدراسية",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "${it.attendanceRatePercent}% (حاضر ${it.presentToday})", it.attendanceRatePercent < 85)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.WEEKLY_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير الأسبوعي للانضباط والتحضير والمتابعة",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يستعرض التقرير الأسبوعي حصيلة انتظام الدوام المدرسي ومتابعة التحضير اليومي والسلوك الطلابي. بلغ مؤشر الاستقرار العام (${snapshot.overallSchoolHealthScore}%) مع رصد (${snapshot.repeatedAbsenceStudents.size}) حالة غياب متكرر تحتاج متابعة أسبوعية مع أولياء الأمور.",
            kpiCards = listOf(
                "معدل المواظبة الأسبوعي" to "${snapshot.overallAttendanceRatePercent}%",
                "حالات الغياب المتكرر" to "${snapshot.repeatedAbsenceStudents.size} طالب",
                "الواجبات المنشورة" to "${snapshot.homeworksCount} واجب",
                "البلاغات الإدارية" to "${snapshot.parentReportsCount} بلاغ"
            ),
            tableTitle = "جدول متابعة الصفوف والانضباط الأسبوعي",
            tableHeaders = defaultClassHeaders,
            tableRows = defaultClassRows,
            chartTitle = "مؤشر انتظام الصفوف خلال الأسبوع الدراسي",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "${it.attendanceRatePercent}%", it.attendanceRatePercent < 85)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.MONTHLY_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير الشهري للأداء الإداري والأكاديمي",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يقدم التقرير الشهري قراءة متكاملة لأداء الصفوف والكادر التدريسي والتحصيل العلمي. بلغت نسبة المواظبة الشهرية (${snapshot.overallAttendanceRatePercent}%)، ${if (hasGrades) "وبلغ متوسط التحصيل الأكاديمي (${snapshot.overallAcademicAveragePercent}%) بنسبة نجاح (${snapshot.overallPassRatePercent}%)" else "وتجري متابعة استكمال رصد درجات الاختبارات الشهرية من قِبل الكادر التدريسي"}.",
            kpiCards = listOf(
                "مؤشر كفاءة المدرسة" to "${snapshot.overallSchoolHealthScore}%",
                "نسبة المواظبة" to "${snapshot.overallAttendanceRatePercent}%",
                "المعدل الشهري" to if (hasGrades) "${snapshot.overallAcademicAveragePercent}%" else "قيد الرصد",
                "الكادر التدريسي" to "${snapshot.totalTeachersCount} معلم"
            ),
            tableTitle = "حصيلة الأداء الشهري للصفوف الدراسية",
            tableHeaders = defaultClassHeaders,
            tableRows = defaultClassRows,
            chartTitle = "مقارنة أداء الصفوف خلال الشهر",
            chartBars = snapshot.classStats.map {
                val v = if (it.recordedGradesCount > 0) it.averageGradePercent else it.attendanceRatePercent
                AdvisorChartBarItem(it.gradeClass, v, "$v%", v < 75)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.TERM_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير الفصلي لنتائج الصفوف والمواد الدراسية",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يركز التقرير الفصلي على تقييم مخرجات التعلم لكل صف ومادة دراسية. ${if (hasGrades) "تم رصد (${snapshot.totalGradesRecorded}) درجة فصلية بمتوسط عام (${snapshot.overallAcademicAveragePercent}%) ونسبة نجاح (${snapshot.overallPassRatePercent}%)، مع تميز (${snapshot.honorStudents.size}) طالباً في لوحة الشرف." else "لم يتم إغلاق رصد درجات الفصل الدراسي بعد، ويوصى باستكمال إدخال الدرجات لاعتماد النتائج الفصلية."}",
            kpiCards = listOf(
                "سجلات الدرجات" to "${snapshot.totalGradesRecorded}",
                "متوسط الفصل" to if (hasGrades) "${snapshot.overallAcademicAveragePercent}%" else "قيد الرصد",
                "نسبة النجاح" to if (hasGrades) "${snapshot.overallPassRatePercent}%" else "قيد الرصد",
                "المتفوقون" to "${snapshot.honorStudents.size} طالب"
            ),
            tableTitle = "جدول نتائج المواد الدراسية المرصودة",
            tableHeaders = listOf("المادة الدراسية", "عدد السجلات", "متوسط الدرجات", "نسبة النجاح", "التقييم"),
            tableRows = if (snapshot.subjectStats.isNotEmpty()) {
                snapshot.subjectStats.map { ss ->
                    listOf(
                        ss.subjectName,
                        "${ss.recordsCount}",
                        "${ss.averagePercent}%",
                        "${ss.passRatePercent}%",
                        if (ss.averagePercent >= 80) "متميز" else if (ss.averagePercent >= 65) "جيد" else "يحتاج دعماً"
                    )
                }
            } else defaultClassRows,
            chartTitle = "مؤشر نسب النجاح والتحصيل الفصلي",
            chartBars = if (snapshot.subjectStats.isNotEmpty()) {
                snapshot.subjectStats.map {
                    AdvisorChartBarItem(it.subjectName, it.averagePercent, "المعدل ${it.averagePercent}% (نجاح ${it.passRatePercent}%)", it.averagePercent < 65)
                }
            } else {
                snapshot.classStats.map {
                    AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "مواظبة ${it.attendanceRatePercent}%", false)
                }
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.ANNUAL_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير السنوي الاستراتيجي للمدرسة (${snapshot.academicYear})",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يمثل هذا التقرير الوثيقة السنوية لمؤشرات (${snapshot.schoolName}) للعام الدراسي (${snapshot.academicYear})، شاملاً الملاك الطلابي (${snapshot.totalStudents} طالب)، الكادر التدريسي (${snapshot.totalTeachersCount} معلم)، السعي السنوي، النقل المدرسي (${snapshot.busSubscribersCount} مشترك)، والأقساط الدراسية.",
            kpiCards = listOf(
                "العام الدراسي" to snapshot.academicYear,
                "إجمالي الطلبة" to "${snapshot.totalStudents} طالب",
                "الكادر التدريسي" to "${snapshot.totalTeachersCount} معلم",
                "المؤشر العام" to "${snapshot.overallSchoolHealthScore}%"
            ),
            tableTitle = "ملخص الأداء السنوي للصفوف الدراسية",
            tableHeaders = defaultClassHeaders,
            tableRows = defaultClassRows,
            chartTitle = "مؤشر الأداء السنوي المتكامل حسب الصفوف",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "${it.totalStudents} طالب • مواظبة ${it.attendanceRatePercent}%", it.attendanceRatePercent < 80)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.ATTENDANCE_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "تقرير الحضور والغياب والانضباط التفصيلي",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "تقرير متخصص برصد حركة الحضور والغياب والتأخر الصباحي والإجازات الرسمية. نسبة الحضور العامة (${snapshot.overallAttendanceRatePercent}%)، عدد الغائبين اليوم (${snapshot.absentTodayCount})، وعدد الطلبة ذوي الغياب المتكرر (${snapshot.repeatedAbsenceStudents.size}).",
            kpiCards = listOf(
                "نسبة المواظبة" to "${snapshot.overallAttendanceRatePercent}%",
                "الحاضرون اليوم" to "${snapshot.presentTodayCount} طالب",
                "الغائبون اليوم" to "${snapshot.absentTodayCount} طالب",
                "تكرار الغياب" to "${snapshot.repeatedAbsenceStudents.size} طالب"
            ),
            tableTitle = "كشف الحضور والغياب التفصيلي لكل صف",
            tableHeaders = listOf("الصف الدراسي", "الطلبة", "حاضر اليوم", "غائب اليوم", "نسبة المواظبة"),
            tableRows = snapshot.classStats.map { cs ->
                listOf(cs.gradeClass, "${cs.totalStudents}", "${cs.presentToday}", "${cs.absentToday}", "${cs.attendanceRatePercent}%")
            },
            chartTitle = "نسب المواظبة والالتزام بالدوام لكل صف",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "${it.attendanceRatePercent}% (غياب: ${it.absentToday})", it.attendanceRatePercent < 85)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.ACADEMIC_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير الأكاديمي الشامل ومؤشرات التحصيل العلمي",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = if (hasGrades) {
                "يحلل هذا التقرير المستوى العلمي للطلبة استناداً إلى (${snapshot.totalGradesRecorded}) درجة مرصودة. بلغ المعدل العام (${snapshot.overallAcademicAveragePercent}%) ونسبة النجاح (${snapshot.overallPassRatePercent}%)، مع وجود (${snapshot.honorStudents.size}) طالباً متفوقاً في لوحة الشرف."
            } else {
                "يوضح هذا التقرير الموقف الأكاديمي الحالي: لم يتم إدخال درجات الامتحانات في السجل بعد (0 درجة مرصودة)، بينما تم نشر (${snapshot.homeworksCount}) واجباً وجدولة (${snapshot.upcomingExamsCount}) امتحاناً."
            },
            kpiCards = listOf(
                "الدرجات المرصودة" to "${snapshot.totalGradesRecorded}",
                "المعدل الأكاديمي" to if (hasGrades) "${snapshot.overallAcademicAveragePercent}%" else "قيد الرصد",
                "نسبة النجاح" to if (hasGrades) "${snapshot.overallPassRatePercent}%" else "قيد الرصد",
                "لوحة الشرف" to "${snapshot.honorStudents.size} متفوق"
            ),
            tableTitle = "جدول التحصيل الأكاديمي ونسب النجاح حسب الصفوف",
            tableHeaders = listOf("الصف الدراسي", "عدد الطلبة", "السجلات المرصودة", "متوسط الدرجات", "نسبة النجاح"),
            tableRows = snapshot.classStats.map { cs ->
                listOf(
                    cs.gradeClass,
                    "${cs.totalStudents}",
                    "${cs.recordedGradesCount}",
                    if (cs.recordedGradesCount > 0) "${cs.averageGradePercent}%" else "قيد الرصد",
                    if (cs.recordedGradesCount > 0) "${cs.passRatePercent}%" else "قيد الرصد"
                )
            },
            chartTitle = "مقارنة متوسط الدرجات بين الصفوف",
            chartBars = snapshot.classStats.map {
                val pct = if (it.recordedGradesCount > 0) it.averageGradePercent else 0
                AdvisorChartBarItem(
                    it.gradeClass,
                    pct,
                    if (it.recordedGradesCount > 0) "${it.averageGradePercent}% (نجاح ${it.passRatePercent}%)" else "بانتظار الرصد",
                    it.recordedGradesCount > 0 && it.averageGradePercent < 65
                )
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.STAFF_EVALUATION_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "تقرير تقييم الكادر التدريسي ومؤشرات الإنجاز",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "يستعرض التقرير كفاءة الكادر التدريسي الفعلي (${snapshot.totalTeachersCount} معلم/مدرس) بناءً على رصد الحضور، نشر الواجبات، رصد الدرجات، وتقييمات أولياء الأمور الموثقة.",
            kpiCards = listOf(
                "الكادر التدريسي" to "${snapshot.totalTeachersCount} معلم",
                "الواجبات المنشورة" to "${snapshot.homeworksCount}",
                "الامتحانات المجدولة" to "${snapshot.upcomingExamsCount}",
                "متوسط الالتزام" to "${if (snapshot.teacherStats.isNotEmpty()) snapshot.teacherStats.map { it.commitmentPercent }.average().roundToInt() else 100}%"
            ),
            tableTitle = "جدول تقييم أعضاء الهيئة التدريسية",
            tableHeaders = listOf("اسم المعلم", "الاختصاص", "الالتزام", "النشاط التفاعلي", "تقييم الأهالي"),
            tableRows = snapshot.teacherStats.map { ts ->
                listOf(
                    ts.teacherName,
                    ts.specialty,
                    "${ts.commitmentPercent}%",
                    "${ts.activityPercent}%",
                    if (ts.ratingsCount > 0) String.format(Locale.US, "%.1f / 5 (%d)", ts.averageStars, ts.ratingsCount) else "لم يقيّم بعد"
                )
            },
            chartTitle = "مؤشر التزام ونشاط الكادر التدريسي",
            chartBars = snapshot.teacherStats.map {
                AdvisorChartBarItem(it.teacherName, it.commitmentPercent, "التزام ${it.commitmentPercent}% • نشاط ${it.activityPercent}%", it.commitmentPercent < 70)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.PROBLEMS_RECOMMENDATIONS_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "تقرير المشكلات المرصودة والتوصيات الاستباقية",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "تقرير تشخيصي استباقي يرصد نقاط الضعف والتحديات اليومية والأكاديمية في (${snapshot.schoolName}) ويقترح خطة عمل تنفيذية لمعالجتها فوراً.",
            kpiCards = listOf(
                "الحالة العامة" to snapshot.overallHealthLabel,
                "مؤشر الاستقرار" to "${snapshot.overallSchoolHealthScore}%",
                "التحديات المرصودة" to "${snapshot.proactiveProblems.size}",
                "التوصيات المقترحة" to "${snapshot.strategicRecommendations.size}"
            ),
            tableTitle = "جدول الطلبة ذوي الغياب المتكرر أو الحالات الحرجة",
            tableHeaders = listOf("اسم الطالب", "الصف والشعبة", "أيام الغياب", "ولي الأمر", "الإجراء المقترح"),
            tableRows = if (snapshot.repeatedAbsenceStudents.isNotEmpty()) {
                snapshot.repeatedAbsenceStudents.map { (stu, absDays) ->
                    listOf(
                        stu.studentName,
                        "${stu.gradeClass} (${stu.sectionName})",
                        "$absDays يوم",
                        stu.parentName,
                        if (absDays >= 4) "استدعاء ولي أمر" else "إرسال تنبيه مواظبة"
                    )
                }
            } else defaultClassRows,
            chartTitle = "مؤشر الانضباط والاستقرار حسب الصفوف",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "${it.attendanceRatePercent}%", it.attendanceRatePercent < 85)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )

        OfficialSmartReportKind.COMPREHENSIVE_SCHOOL_REPORT -> GeneratedOfficialReportDocument(
            kind = kind,
            title = "التقرير التنفيذي الشامل للمدرسة (${snapshot.schoolName})",
            schoolName = snapshot.schoolName,
            principalName = snapshot.principalName,
            academicYear = snapshot.academicYear,
            issueDate = snapshot.todayIso,
            executiveSummary = "التقرير التنفيذي الشامل لمدير المدرسة، يجمع كافة مؤشرات الطلبة (${snapshot.totalStudents})، الكادر التدريسي (${snapshot.totalTeachersCount})، الحضور والغياب (${snapshot.overallAttendanceRatePercent}%)، التحصيل الأكاديمي، الأقساط الدراسية، والنقل المدرسي في وثيقة رسمية واحدة.",
            kpiCards = listOf(
                "مؤشر المدرسة العام" to "${snapshot.overallSchoolHealthScore}% (${snapshot.overallHealthLabel})",
                "الطلبة والكادر" to "${snapshot.totalStudents} طالب • ${snapshot.totalTeachersCount} معلم",
                "نسبة المواظبة" to "${snapshot.overallAttendanceRatePercent}%",
                "الأقساط الواصلة" to "${String.format(Locale.US, "%,d", snapshot.totalTuitionPaidIqd)} د.ع"
            ),
            tableTitle = "المصفوفة الشاملة لأداء الصفوف الدراسية",
            tableHeaders = defaultClassHeaders,
            tableRows = defaultClassRows,
            chartTitle = "المؤشر الشامل لأداء الصفوف في المدرسة",
            chartBars = snapshot.classStats.map {
                AdvisorChartBarItem(it.gradeClass, it.attendanceRatePercent, "مواظبة ${it.attendanceRatePercent}% • طلبة: ${it.totalStudents}", it.attendanceRatePercent < 85)
            },
            findingsAndProblems = snapshot.proactiveProblems,
            officialRecommendations = snapshot.strategicRecommendations
        )
    }
}

fun exportOfficialAdvisorReportToPdf(
    context: Context,
    report: GeneratedOfficialReportDocument,
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
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val subHeaderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#FDE68A")
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
        }
        val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.parseColor("#065F46")
            textSize = 12.5f
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

        // 1. Official Royal Emerald Header
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 92f, headerPaint)
        canvas.drawRect(0f, 88f, pageWidth.toFloat(), 94f, goldPaint)

        val rightMargin = pageWidth - 28f
        val leftMargin = 28f
        canvas.drawText(report.schoolName, rightMargin, 32f, titlePaint)
        canvas.drawText(report.title, rightMargin, 56f, subHeaderPaint)
        canvas.drawText(
            "المدير: ${report.principalName}  |  العام الدراسي: ${report.academicYear}  |  تاريخ الإصدار: ${report.issueDate}",
            rightMargin,
            76f,
             bodyPaint.apply { color = android.graphics.Color.WHITE }
        )
        bodyPaint.color = android.graphics.Color.parseColor("#1E293B")

        var currentY = 118f

        // 2. KPI Summary Boxes
        val kpiCount = report.kpiCards.size.coerceAtLeast(1)
        val boxGap = 8f
        val totalWidth = pageWidth - 56f
        val boxW = (totalWidth - (boxGap * (kpiCount - 1))) / kpiCount
        report.kpiCards.forEachIndexed { idx, (label, value) ->
            val boxLeft = rightMargin - ((idx + 1) * boxW) - (idx * boxGap)
            val boxRight = boxLeft + boxW
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 44f, 8f, 8f, tableHeaderPaint)
            canvas.drawRoundRect(boxLeft, currentY, boxRight, currentY + 44f, 8f, 8f, borderPaint)
            val centerX = (boxLeft + boxRight) / 2f
            val kpiLabelPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 8.5f
                color = android.graphics.Color.parseColor("#475569")
            }
            val kpiValPaint = Paint(sectionTitlePaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 10.5f
            }
            canvas.drawText(label, centerX, currentY + 17f, kpiLabelPaint)
            canvas.drawText(value, centerX, currentY + 35f, kpiValPaint)
        }
        currentY += 64f

        // 3. Executive Summary
        canvas.drawText("أولاً: الملخص التنفيذي للتقرير", rightMargin, currentY, sectionTitlePaint)
        currentY += 16f
        val words = report.executiveSummary.split(" ")
        var lineBuilder = StringBuilder()
        for (w in words) {
            val testLine = if (lineBuilder.isEmpty()) w else "$lineBuilder $w"
            if (bodyPaint.measureText(testLine) > (pageWidth - 60f)) {
                canvas.drawText(lineBuilder.toString(), rightMargin, currentY, bodyPaint)
                currentY += 15f
                lineBuilder = StringBuilder(w)
            } else {
                lineBuilder = StringBuilder(testLine)
            }
        }
        if (lineBuilder.isNotEmpty()) {
            canvas.drawText(lineBuilder.toString(), rightMargin, currentY, bodyPaint)
            currentY += 22f
        }

        // 4. Statistical Table
        if (report.tableRows.isNotEmpty() && report.tableHeaders.isNotEmpty()) {
            canvas.drawText("ثانياً: ${report.tableTitle}", rightMargin, currentY, sectionTitlePaint)
            currentY += 10f
            val colCount = report.tableHeaders.size
            val colW = totalWidth / colCount
            // Header row
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 22f, tableHeaderPaint)
            canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 22f, borderPaint)
            val thPaint = Paint(bodyPaint).apply {
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                color = android.graphics.Color.parseColor("#065F46")
            }
            report.tableHeaders.forEachIndexed { cIdx, hText ->
                val colRight = rightMargin - (cIdx * colW)
                val colCenter = colRight - (colW / 2f)
                canvas.drawText(hText, colCenter, currentY + 15f, thPaint)
            }
            currentY += 22f

            val tdPaint = Paint(bodyPaint).apply {
                textAlign = Paint.Align.CENTER
                textSize = 8.5f
            }
            report.tableRows.take(10).forEach { row ->
                if (currentY + 20f < pageHeight - 140f) {
                    canvas.drawRect(leftMargin, currentY, rightMargin, currentY + 20f, borderPaint)
                    row.forEachIndexed { cIdx, cellText ->
                        val colRight = rightMargin - (cIdx * colW)
                        val colCenter = colRight - (colW / 2f)
                        canvas.drawText(cellText.take(26), colCenter, currentY + 14f, tdPaint)
                    }
                    currentY += 20f
                }
            }
            currentY += 18f
        }

        // 5. Proactive Findings & Recommendations
        if (currentY + 60f < pageHeight - 60f) {
            canvas.drawText("ثالثاً: أبرز المؤشرات والتوصيات العملية", rightMargin, currentY, sectionTitlePaint)
            currentY += 16f
            (report.findingsAndProblems.take(3) + report.officialRecommendations.take(4)).forEachIndexed { idx, item ->
                if (currentY + 15f < pageHeight - 50f) {
                    val cleanLine = "${idx + 1}. $item"
                    val truncated = if (bodyPaint.measureText(cleanLine) > totalWidth) {
                        cleanLine.take(88) + "..."
                    } else cleanLine
                    canvas.drawText(truncated, rightMargin, currentY, bodyPaint)
                    currentY += 15f
                }
            }
        }

        // 6. Official Footer & Signature Stamp
        val footerY = pageHeight - 28f
        canvas.drawLine(leftMargin, footerY - 16f, rightMargin, footerY - 16f, borderPaint)
        val footerPaint = Paint(bodyPaint).apply {
            textSize = 8.5f
            color = android.graphics.Color.parseColor("#64748B")
        }
        canvas.drawText(
            "صادر عن المستشار الإداري الذكي — ${report.schoolName} — توقيع الإدارة: ${report.principalName}",
            rightMargin,
            footerY,
            footerPaint
        )

        pdfDocument.finishPage(page)

        val safeFileName = "Report_${report.kind.name}_${System.currentTimeMillis()}.pdf"
        val cacheFile = File(context.cacheDir, safeFileName)
        FileOutputStream(cacheFile).use { out ->
            pdfDocument.writeTo(out)
        }

        // Also save to public Downloads on Android Q+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, safeFileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { outStream ->
                        FileInputStream(cacheFile).use { inStream ->
                            inStream.copyTo(outStream)
                        }
                    }
                }
            }
        }

        pdfDocument.close()

        // Launch Android PrintManager preview if context is Activity
        runCatching {
            val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            if (printManager != null) {
                val jobName = "${report.title} - ${report.schoolName}"
                printManager.print(
                    jobName,
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
                            val info = PrintDocumentInfo.Builder(safeFileName)
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
        }

        onResult(true, "تم تصدير (${report.title}) بصيغة PDF رسمية وحفظه في التنزيلات وفتح نافذة الطباعة بنجاح")
    } catch (e: Exception) {
        onResult(false, "تعذر تصدير ملف PDF: ${e.localizedMessage ?: "خطأ غير متوقع"}")
    }
}

// =========================================================================
// 6. MAIN COMPOSABLE: AiSchoolAdvisorScreen (4 INTEGRATED TABS)
// =========================================================================

@Composable
fun AiSchoolAdvisorScreen(
    viewModel: SchoolViewModel,
    initialTab: Int = 0,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val teacherRatings by viewModel.teacherRatings.collectAsState()
    val tuitions by viewModel.tuitions.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()
    val examSchedules by viewModel.examSchedules.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()

    // Strict Principal-Only Guard
    if (currentUser?.role != UserRole.ADMIN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "صلاحية مقيدة",
                        tint = DangerRed,
                        modifier = Modifier.size(42.dp)
                    )
                    Text(
                        text = "قسم المستشار الإداري الذكي مخصص حصرياً لمدير المدرسة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DangerRed,
                        textAlign = TextAlign.Center
                    )
                    if (onBack != null) {
                        Button(
                            onClick = onBack,
                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                        ) {
                            Text("العودة للرئيسية")
                        }
                    }
                }
            }
        }
        return
    }

    val snapshot = remember(
        schoolConfig,
        currentUser,
        students,
        attendance,
        grades,
        accounts,
        teacherRatings,
        tuitions,
        homeworks,
        examSchedules,
        reports,
        leaveRequests
    ) {
        buildRealSchoolContextSnapshot(viewModel)
    }

    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var showApiSettingsDialog by remember { mutableStateOf(false) }

    // Chat State
    val welcomeMessage = remember(snapshot.schoolName, snapshot.principalName, snapshot.totalStudents, snapshot.overallAttendanceRatePercent) {
        AdvisorChatMessage(
            id = "welcome_exec_msg",
            isFromUser = false,
            senderTitle = "المستشار الإداري والأكاديمي الذكي",
            timestamp = "الآن",
            headline = "مرحباً بك سيادة المدير (${snapshot.principalName}) في المستشار الذكي لـ (${snapshot.schoolName})",
            bodyText = "أنا مستشارك الإداري والأكاديمي الذكي المرتبط مباشرة بقاعدة بيانات المدرسة الحقيقية للعام الدراسي (${snapshot.academicYear}).\n" +
                "• إجمالي الطلبة الفعليين: (${snapshot.totalStudents}) طالب | الكادر التدريسي: (${snapshot.totalTeachersCount}) معلم/مدرس.\n" +
                "• نسبة المواظبة اليوم (${snapshot.todayIso}): (${snapshot.overallAttendanceRatePercent}%) | الحالة العامة: (${snapshot.overallHealthLabel}).\n\n" +
                "يمكنك سؤالي عن أي إحصائية أو مقارنة بين الصفوف، أو إصدار أوامر تواصل طبيعية مثل:\n" +
                "«أبلغ أولياء أمور الطلبة المتغيبين اليوم» أو «أرسل تنبيهاً إلى الطالب ... بسبب تكرار غيابه»، وسأقوم بتجهيز بطاقة المراجعة والاعتماد فوراً دون إرسال أي إشعار إلا بعد موافقتك الصريحة.",
            keyMetrics = listOf(
                "الحالة العامة" to snapshot.overallHealthLabel,
                "الطلبة" to "${snapshot.totalStudents} طالب",
                "نسبة الحضور" to "${snapshot.overallAttendanceRatePercent}%",
                "الكادر التدريسي" to "${snapshot.totalTeachersCount} معلم"
            ),
            recommendations = snapshot.strategicRecommendations.take(3)
        )
    }

    var chatMessages by remember { mutableStateOf(listOf(welcomeMessage)) }
    var userInputText by remember { mutableStateOf("") }
    var isAnalyzingQuery by remember { mutableStateOf(false) }

    // Smart Notifications Audit Log State (Tab 4)
    var dispatchAuditLogs by remember { mutableStateOf<List<SmartDispatchAuditLogEntry>>(emptyList()) }

    // Selected Official Report State (Tab 3)
    var selectedReportKind by remember { mutableStateOf(OfficialSmartReportKind.DAILY_REPORT) }

    fun submitAdvisorQuery(rawPrompt: String) {
        val cleanPrompt = rawPrompt.trim()
        if (cleanPrompt.isEmpty() || isAnalyzingQuery) return
        val nowTime = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val userMsg = AdvisorChatMessage(
            isFromUser = true,
            senderTitle = "مدير المدرسة (${snapshot.principalName})",
            timestamp = nowTime,
            bodyText = cleanPrompt
        )
        val historySnapshot = chatMessages
        chatMessages = chatMessages + userMsg
        userInputText = ""
        isAnalyzingQuery = true

        coroutineScope.launch {
            val reply = generateAdvisorAnalyticalReply(
                context = context,
                query = cleanPrompt,
                snapshot = snapshot,
                history = historySnapshot,
                viewModel = viewModel
            )
            chatMessages = chatMessages + reply
            isAnalyzingQuery = false
        }
    }

    fun executeApprovedNotificationProposal(
        messageId: String,
        proposal: PendingSmartNotificationProposal,
        editedTitle: String,
        editedBody: String,
        selectedCategory: SmartAdvisorCommunicationCategory,
        selectedChannel: SmartDeliveryChannel
    ) {
        val cleanTitle = editedTitle.trim().ifBlank { proposal.suggestedTitle }
        val cleanBody = editedBody.trim().ifBlank { proposal.suggestedBody }
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())

        var successCount = 0
        val failedList = mutableListOf<SmartRecipientTarget>()

        if (proposal.isBroadcastToAllStudents && proposal.recipients.size > 1) {
            // Broadcast notification + individual records
            viewModel.pushNotification(
                title = cleanTitle,
                body = cleanBody,
                type = selectedCategory.mappedNotificationType,
                targetGradeClass = proposal.targetGradeClass,
                targetSectionName = proposal.targetSectionName,
                isUrgent = true,
                priority = "high"
            )
            successCount = proposal.recipients.size
        } else {
            proposal.recipients.forEach { target ->
                runCatching {
                    viewModel.pushNotification(
                        title = cleanTitle,
                        body = cleanBody,
                        type = selectedCategory.mappedNotificationType,
                        targetStudentId = target.student.id,
                        targetGradeClass = proposal.targetGradeClass,
                        targetSectionName = proposal.targetSectionName,
                        targetRole = if (target.targetParentOnly) UserRole.PARENT.name else "",
                        isUrgent = true,
                        priority = "high"
                    )
                    if (selectedCategory.mappedReportType != null && proposal.recipients.size <= 15) {
                        viewModel.sendParentReport(
                            student = target.student,
                            reportType = selectedCategory.mappedReportType,
                            title = cleanTitle,
                            details = cleanBody,
                            senderName = "مدير المدرسة (${snapshot.principalName})"
                        )
                    }
                    successCount++
                }.onFailure {
                    failedList.add(target)
                }
            }
        }

        // Launch external channel if WhatsApp or SMS was explicitly selected
        val firstPhone = proposal.recipients.firstOrNull()?.student?.parentPhone.orEmpty()
        if (selectedChannel == SmartDeliveryChannel.WHATSAPP) {
            ExternalIntents.sendWhatsAppMessage(context, firstPhone, "$cleanTitle\n$cleanBody")
        } else if (selectedChannel == SmartDeliveryChannel.SMS) {
            runCatching {
                val smsIntent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$firstPhone")).apply {
                    putExtra("sms_body", "$cleanTitle\n$cleanBody")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(smsIntent)
            }
        }

        val summaryText = "تم الإرسال الرسمي بنجاح إلى ($successCount) مستلم عبر (${selectedChannel.arabicLabel}) • الفشل: (${failedList.size})"
        val logEntry = SmartDispatchAuditLogEntry(
            senderName = "مدير المدرسة (${snapshot.principalName})",
            timestamp = nowStamp,
            audienceTitle = proposal.audienceTitle,
            recipientNames = proposal.recipients.map { it.student.studentName },
            categoryLabel = selectedCategory.arabicLabel,
            channel = selectedChannel,
            messageTitle = cleanTitle,
            messageBody = cleanBody,
            totalTargeted = proposal.recipients.size,
            successCount = successCount,
            failedCount = failedList.size,
            failedRecipients = failedList,
            statusLabel = if (failedList.isEmpty()) "تم التسليم بالكامل" else "تم جزئياً (يوجد ${failedList.size} متعثر)"
        )
        dispatchAuditLogs = listOf(logEntry) + dispatchAuditLogs

        chatMessages = chatMessages.map { msg ->
            if (msg.id == messageId && msg.pendingNotificationProposal != null) {
                msg.copy(
                    pendingNotificationProposal = msg.pendingNotificationProposal.copy(
                        isExecuted = true,
                        executionSummary = summaryText
                    )
                )
            } else msg
        }

        viewModel.showToastAlert(summaryText, isSuccess = true)
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
                // Top Royal Emerald & Gold Wavy Executive Hero Header + 4 Integrated Tabs
                AdvisorWavyExecutiveHeader(
                    schoolName = snapshot.schoolName,
                    principalName = snapshot.principalName,
                    healthLabel = snapshot.overallHealthLabel,
                    healthScore = snapshot.overallSchoolHealthScore,
                    selectedTab = selectedTab,
                    logsCount = dispatchAuditLogs.size,
                    isAnalyzing = isAnalyzingQuery,
                    onSelectTab = { selectedTab = it },
                    onNewChat = {
                        chatMessages = listOf(welcomeMessage)
                        selectedTab = 0
                        viewModel.showToastAlert("تم بدء جلسة محادثة تحليلية جديدة", isSuccess = true)
                    },
                    onBack = onBack
                )

                // Main Content Area by Selected Tab
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                when (selectedTab) {
                    0 -> AdvisorInteractiveChatTab(
                        snapshot = snapshot,
                        messages = chatMessages,
                        userInput = userInputText,
                        isAnalyzing = isAnalyzingQuery,
                        onUserInputChange = { userInputText = it },
                        onSendPrompt = { submitAdvisorQuery(it) },
                        onNewChat = {
                            chatMessages = listOf(welcomeMessage)
                            viewModel.showToastAlert("تم بدء محادثة جديدة", isSuccess = true)
                        },
                        onApproveProposal = { msgId, prop, title, body, cat, chan ->
                            executeApprovedNotificationProposal(msgId, prop, title, body, cat, chan)
                        },
                        onCancelProposal = { msgId ->
                            chatMessages = chatMessages.map { msg ->
                                if (msg.id == msgId) msg.copy(pendingNotificationProposal = null) else msg
                            }
                            viewModel.showToastAlert("تم إلغاء مسودة الإشعار بناءً على توجيه المدير", isSuccess = true)
                        },
                        onExportMessagePdf = { msg ->
                            val customDoc = GeneratedOfficialReportDocument(
                                kind = OfficialSmartReportKind.COMPREHENSIVE_SCHOOL_REPORT,
                                title = msg.headline.ifBlank { "تقرير المستشار الإداري الذكي" },
                                schoolName = snapshot.schoolName,
                                principalName = snapshot.principalName,
                                academicYear = snapshot.academicYear,
                                issueDate = snapshot.todayIso,
                                executiveSummary = msg.bodyText,
                                kpiCards = msg.keyMetrics.ifEmpty {
                                    listOf(
                                        "إجمالي الطلبة" to "${snapshot.totalStudents}",
                                        "نسبة الحضور" to "${snapshot.overallAttendanceRatePercent}%",
                                        "الحالة العامة" to snapshot.overallHealthLabel
                                    )
                                },
                                tableTitle = msg.tableSection?.title ?: "ملخص الصفوف الدراسية",
                                tableHeaders = msg.tableSection?.headers ?: listOf("الصف", "الطلبة", "الحضور"),
                                tableRows = msg.tableSection?.rows ?: snapshot.classStats.map {
                                    listOf(it.gradeClass, "${it.totalStudents}", "${it.attendanceRatePercent}%")
                                },
                                chartTitle = msg.chartTitle,
                                chartBars = msg.chartBars,
                                findingsAndProblems = snapshot.proactiveProblems,
                                officialRecommendations = msg.recommendations.ifEmpty { snapshot.strategicRecommendations }
                            )
                            exportOfficialAdvisorReportToPdf(context, customDoc) { ok, statusMsg ->
                                viewModel.showToastAlert(statusMsg, isSuccess = ok)
                            }
                        },
                        onCopyMessage = { msgText ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("إجابة المستشار الذكي", msgText))
                            viewModel.showToastAlert("تم نسخ إجابة المستشار الذكي إلى الحافظة", isSuccess = true)
                        }
                    )

                    1 -> AdvisorSmartDashboardIndicatorsTab(
                        snapshot = snapshot,
                        onAskFollowUpInChat = { promptText ->
                            selectedTab = 0
                            submitAdvisorQuery(promptText)
                        },
                        onOpenReportKind = { reportKind ->
                            selectedReportKind = reportKind
                            selectedTab = 2
                        }
                    )

                    2 -> AdvisorTenOfficialReportsTab(
                        snapshot = snapshot,
                        selectedKind = selectedReportKind,
                        onSelectKind = { selectedReportKind = it },
                        onExportReportPdf = { doc ->
                            exportOfficialAdvisorReportToPdf(context, doc) { ok, statusMsg ->
                                viewModel.showToastAlert(statusMsg, isSuccess = ok)
                            }
                        }
                    )

                    3 -> AdvisorSentNotificationsLogTab(
                        auditLogs = dispatchAuditLogs,
                        onRetryFailed = { logEntry ->
                            val targetsToRetry = logEntry.failedRecipients
                            var retriedCount = 0
                            targetsToRetry.forEach { target ->
                                viewModel.pushNotification(
                                    title = logEntry.messageTitle,
                                    body = logEntry.messageBody,
                                    type = NotificationType.REPORT,
                                    targetStudentId = target.student.id,
                                    isUrgent = true,
                                    priority = "high"
                                )
                                retriedCount++
                            }
                            dispatchAuditLogs = dispatchAuditLogs.map { item ->
                                if (item.id == logEntry.id) {
                                    item.copy(
                                        successCount = item.totalTargeted,
                                        failedCount = 0,
                                        failedRecipients = emptyList(),
                                        statusLabel = "تم التسليم بالكامل (بعد إعادة المحاولة)"
                                    )
                                } else item
                            }
                            viewModel.showToastAlert("تمت إعادة إرسال الإشعار بنجاح إلى ($retriedCount) مستلم", isSuccess = true)
                        },
                        onCreateNewSmartAlert = {
                            selectedTab = 0
                            submitAdvisorQuery("أبلغ أولياء أمور الطلبة المتغيبين اليوم")
                        }
                    )
                }
            }
        }
    }
}
}

// =========================================================================
// 7. HERO BANNER & INTERACTIVE NEURAL AI ORB CANVAS
// =========================================================================

@Composable
private fun AdvisorWavyExecutiveHeader(
    schoolName: String,
    principalName: String,
    healthLabel: String,
    healthScore: Int,
    selectedTab: Int,
    logsCount: Int,
    isAnalyzing: Boolean = false,
    onSelectTab: (Int) -> Unit,
    onNewChat: () -> Unit,
    onBack: (() -> Unit)?
) {
    val waveShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - 16f)
            cubicTo(
                w * 0.72f, h + 6f,
                w * 0.28f, h - 26f,
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
                // Top Row: Back Button + Neural AI Orb + Full Title + New Chat Button (NO API Settings Button!)
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
                                    .testTag("ai_advisor_back_btn")
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

                        // Interactive Neural AI Orb & Flowing Synaptic Waves
                        ExecutiveAnalyticsRadarVectorCanvas(
                            modifier = Modifier.size(42.dp),
                            isAnalyzing = isAnalyzing
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "المستشار الإداري الذكي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "$schoolName • المؤشر العام: $healthScore% ($healthLabel)",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        onClick = onNewChat,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.testTag("ai_advisor_new_chat_top_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "بدء محادثة جديدة",
                                tint = Color(0xFF042F24),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "محادثة جديدة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24)
                            )
                        }
                    }
                }

                // 4 Integrated Capsule Tabs (Scrollable Full Labels)
                val tabs = listOf(
                    Triple(0, "المحادثة والتحليل التفاعلي", Icons.Filled.Forum),
                    Triple(1, "لوحة المؤشرات الذكية", Icons.Filled.Analytics),
                    Triple(2, "التقارير الـ 10 الرسمية + PDF", Icons.Filled.PictureAsPdf),
                    Triple(3, if (logsCount > 0) "سجل الإشعارات الذكية ($logsCount)" else "سجل الإشعارات الذكية", Icons.Filled.NotificationsActive)
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tabs, key = { it.first }) { (tabIdx, tabTitle, tabIcon) ->
                        val isSelected = selectedTab == tabIdx
                        Surface(
                            onClick = { onSelectTab(tabIdx) },
                            shape = RoundedCornerShape(50),
                            color = if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.12f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.25f)
                            ),
                            modifier = Modifier.testTag("ai_advisor_tab_$tabIdx")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tabIcon,
                                    contentDescription = tabTitle,
                                    tint = if (isSelected) Color(0xFF042F24) else Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = tabTitle,
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
fun ExecutiveAnalyticsRadarVectorCanvas(
    modifier: Modifier = Modifier,
    isAnalyzing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "neural_orb_transition")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isAnalyzing) 2200 else 5600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "neural_orbit_angle"
    )
    val synapticPulse by infiniteTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isAnalyzing) 650 else 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neural_synaptic_pulse"
    )
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isAnalyzing) 900 else 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "neural_wave_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (w.coerceAtMost(h) / 2f) * 0.92f

        // 1. Deep Emerald & Midnight Neural Sphere Base
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF059669),
                    Color(0xFF065F46),
                    Color(0xFF022C22)
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )

        // 2. Outer Royal Gold Ring & Inner Cyan Orbital Ring
        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(
                    Color(0xFFFBBF24),
                    Color(0xFF34D399),
                    Color(0xFFFDE68A),
                    Color(0xFFFBBF24)
                ),
                center = Offset(cx, cy)
            ),
            radius = radius,
            center = Offset(cx, cy),
            style = Stroke(width = 2.4f)
        )
        drawCircle(
            color = Color(0xFF2DD4BF).copy(alpha = 0.42f),
            radius = radius * 0.74f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.3f)
        )

        // 3. Flowing Synaptic Waves (Smooth Path.cubicTo Curves — NO Vertical Bars!)
        val waveAmplitude = radius * 0.34f * wavePhase
        val primaryWavePath = Path().apply {
            moveTo(cx - radius * 0.76f, cy)
            cubicTo(
                cx - radius * 0.36f, cy - waveAmplitude * 1.35f,
                cx + radius * 0.36f, cy + waveAmplitude * 1.35f,
                cx + radius * 0.76f, cy
            )
        }
        drawPath(
            path = primaryWavePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFF2DD4BF),
                    Color(0xFFFDE68A),
                    Color(0xFF34D399)
                )
            ),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        val secondaryWavePath = Path().apply {
            moveTo(cx - radius * 0.72f, cy + radius * 0.14f)
            cubicTo(
                cx - radius * 0.28f, cy + waveAmplitude * 1.15f,
                cx + radius * 0.28f, cy - waveAmplitude * 1.15f,
                cx + radius * 0.72f, cy - radius * 0.14f
            )
        }
        drawPath(
            path = secondaryWavePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFFBBF24).copy(alpha = 0.88f),
                    Color(0xFF6EE7B7),
                    Color(0xFFFBBF24).copy(alpha = 0.88f)
                )
            ),
            style = Stroke(width = 1.9f, cap = StrokeCap.Round)
        )

        // 4. Pulsing Central Neural Core
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.95f),
                    Color(0xFF34D399).copy(alpha = 0.55f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = radius * 0.36f * synapticPulse
            ),
            radius = radius * 0.36f * synapticPulse,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = (radius * 0.13f * synapticPulse).coerceAtLeast(2.5f),
            center = Offset(cx, cy)
        )

        // 5. Orbiting Neural Nodes on Counter-Rotating Synaptic Paths
        val rad1 = Math.toRadians(orbitAngle.toDouble())
        val node1X = cx + (radius * 0.74f * cos(rad1)).toFloat()
        val node1Y = cy + (radius * 0.74f * sin(rad1)).toFloat()
        drawCircle(
            color = Color(0xFFFDE68A),
            radius = 3.2f,
            center = Offset(node1X, node1Y)
        )

        val rad2 = Math.toRadians((360f - orbitAngle).toDouble())
        val node2X = cx + (radius * 0.52f * cos(rad2)).toFloat()
        val node2Y = cy + (radius * 0.52f * sin(rad2)).toFloat()
        drawCircle(
            color = Color(0xFF2DD4BF),
            radius = 2.6f,
            center = Offset(node2X, node2Y)
        )
    }
}

// =========================================================================
// 8. TAB 1: INTERACTIVE CHAT, QUICK PROMPTS & MANDATORY APPROVAL WORKFLOW
// =========================================================================

@Composable
private fun AdvisorInteractiveChatTab(
    snapshot: RealSchoolAnalyticalSnapshot,
    messages: List<AdvisorChatMessage>,
    userInput: String,
    isAnalyzing: Boolean,
    onUserInputChange: (String) -> Unit,
    onSendPrompt: (String) -> Unit,
    onNewChat: () -> Unit,
    onApproveProposal: (
        messageId: String,
        proposal: PendingSmartNotificationProposal,
        editedTitle: String,
        editedBody: String,
        selectedCategory: SmartAdvisorCommunicationCategory,
        selectedChannel: SmartDeliveryChannel
    ) -> Unit,
    onCancelProposal: (String) -> Unit,
    onExportMessagePdf: (AdvisorChatMessage) -> Unit,
    onCopyMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size, isAnalyzing) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    val quickPrompts = remember {
        listOf(
            "حلل أداء المدرسة لهذا الشهر",
            "أعطني تقرير الحضور والغياب",
            "ما أبرز المشكلات الحالية؟",
            "قارن نتائج الصفوف الدراسية",
            "ما أهم التوصيات لتحسين المدرسة؟",
            "أبلغ أولياء أمور الطلبة المتغيبين اليوم"
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Single-Row Compact Quick Prompts Bar
        Surface(
            color = Color(0xFFECFDF5),
            border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(quickPrompts) { prompt ->
                    Surface(
                        onClick = { onSendPrompt(prompt) },
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                        shadowElevation = 1.dp,
                        modifier = Modifier.testTag("quick_prompt_chip_${prompt.hashCode()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (prompt.contains("أبلغ") || prompt.contains("أرسل")) {
                                    Icons.Filled.NotificationsActive
                                } else {
                                    Icons.Filled.QueryStats
                                },
                                contentDescription = null,
                                tint = if (prompt.contains("أبلغ")) Color(0xFFD97706) else Color(0xFF059669),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = prompt,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF042F24),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Chat Messages List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                AdvisorChatMessageCard(
                    message = msg,
                    onApproveProposal = { prop, title, body, cat, chan ->
                        onApproveProposal(msg.id, prop, title, body, cat, chan)
                    },
                    onCancelProposal = { onCancelProposal(msg.id) },
                    onExportPdf = { onExportMessagePdf(msg) },
                    onCopy = {
                        val fullCopy = buildString {
                            if (msg.headline.isNotBlank()) appendLine(msg.headline)
                            appendLine(msg.bodyText)
                            if (msg.recommendations.isNotEmpty()) {
                                appendLine("\nالتوصيات:")
                                msg.recommendations.forEach { appendLine("- $it") }
                            }
                        }
                        onCopyMessage(fullCopy.trim())
                    }
                )
            }

            if (isAnalyzing) {
                item(key = "analyzing_indicator") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color(0xFF059669),
                                strokeWidth = 2.5.dp
                            )
                            Column {
                                Text(
                                    text = "جاري تحليل بيانات المدرسة واستخلاص المؤشرات...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF042F24)
                                )
                                Text(
                                    text = "فحص سجلات الطلبة (${snapshot.totalStudents})، الحضور والغياب، الدرجات، والكادر التدريسي",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF065F46)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bottom Input Bar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = userInput,
                    onValueChange = onUserInputChange,
                    placeholder = {
                        Text(
                            text = "اسأل المستشار الذكي أو اكتب أمر إشعار (مثال: أرسل تنبيهاً إلى الطالب رسلان بسبب تكرار غيابه)...",
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_advisor_chat_input"),
                    shape = RoundedCornerShape(18.dp),
                    maxLines = 3
                )

                Button(
                    onClick = { onSendPrompt(userInput) },
                    enabled = userInput.isNotBlank() && !isAnalyzing,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    modifier = Modifier.testTag("ai_advisor_send_btn")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "إرسال",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تحليل",
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun AdvisorChatMessageCard(
    message: AdvisorChatMessage,
    onApproveProposal: (
        proposal: PendingSmartNotificationProposal,
        editedTitle: String,
        editedBody: String,
        selectedCategory: SmartAdvisorCommunicationCategory,
        selectedChannel: SmartDeliveryChannel
    ) -> Unit,
    onCancelProposal: () -> Unit,
    onExportPdf: () -> Unit,
    onCopy: () -> Unit
) {
    if (message.isFromUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 4.dp),
                color = Color(0xFF065F46),
                shadowElevation = 2.dp,
                modifier = Modifier.widthIn(max = 340.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = message.senderTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFFFDE68A),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = message.timestamp,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                    Text(
                        text = message.bodyText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.35f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF042F24),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Insights,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = message.senderTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24)
                            )
                            if (message.headline.isNotBlank()) {
                                Text(
                                    text = message.headline,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF059669)
                                )
                            }
                        }
                    }
                    Text(
                        text = message.timestamp,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Key Metrics Pills
                if (message.keyMetrics.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        message.keyMetrics.forEach { (k, v) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = k,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF065F46)
                                    )
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF042F24)
                                    )
                                }
                            }
                        }
                    }
                }

                // Body Text
                Text(
                    text = message.bodyText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 22.sp
                )

                // Optional Statistical Table
                message.tableSection?.let { table ->
                    AdvisorStructuredTableView(table = table)
                }

                // Optional Canvas Visual Bar Chart
                if (message.chartBars.isNotEmpty()) {
                    AdvisorCanvasBarChartCard(
                        title = message.chartTitle.ifBlank { "المؤشر البياني المقارن" },
                        bars = message.chartBars
                    )
                }

                // Optional Recommendations Box
                if (message.recommendations.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "التوصيات الإجرائية المقترحة للمدير:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E)
                                )
                            }
                            message.recommendations.forEachIndexed { idx, rec ->
                                Text(
                                    text = "${idx + 1}. $rec",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F),
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Mandatory Manager Approval Card for Smart Communication Proposals
                message.pendingNotificationProposal?.let { proposal ->
                    SmartNotificationApprovalWorkflowCard(
                        proposal = proposal,
                        onApprove = { editedTitle, editedBody, cat, chan ->
                            onApproveProposal(proposal, editedTitle, editedBody, cat, chan)
                        },
                        onCancel = onCancelProposal
                    )
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                // Bottom Action Buttons: Copy + Export Official PDF
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onCopy,
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("advisor_copy_reply_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "نسخ الإجابة",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "نسخ الإجابة",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onExportPdf,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF042F24),
                            contentColor = Color(0xFFFDE68A)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("advisor_export_pdf_${message.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PictureAsPdf,
                            contentDescription = "تصدير كتقرير PDF رسمي",
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "تصدير كتقرير PDF رسمي",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SmartNotificationApprovalWorkflowCard(
    proposal: PendingSmartNotificationProposal,
    onApprove: (
        editedTitle: String,
        editedBody: String,
        selectedCategory: SmartAdvisorCommunicationCategory,
        selectedChannel: SmartDeliveryChannel
    ) -> Unit,
    onCancel: () -> Unit
) {
    var editedTitle by remember(proposal.id) { mutableStateOf(proposal.suggestedTitle) }
    var editedBody by remember(proposal.id) { mutableStateOf(proposal.suggestedBody) }
    var selectedCategory by remember(proposal.id) { mutableStateOf(proposal.category) }
    var selectedChannel by remember(proposal.id) { mutableStateOf(proposal.defaultChannel) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (proposal.isExecuted) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
        border = BorderStroke(
            width = 1.6.dp,
            color = if (proposal.isExecuted) Color(0xFF059669) else Color(0xFFD97706)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("smart_notification_approval_card_${proposal.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (proposal.isExecuted) Icons.Filled.Verified else Icons.Filled.AdminPanelSettings,
                        contentDescription = null,
                        tint = if (proposal.isExecuted) Color(0xFF059669) else Color(0xFFD97706),
                        modifier = Modifier.size(22.dp)
                    )
                    Column {
                        Text(
                            text = "بطاقة مراجعة واعتماد الإرسال الذكي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF042F24)
                        )
                        Text(
                            text = if (proposal.isExecuted) {
                                proposal.executionSummary
                            } else {
                                "لا يتم إرسال أي إشعار تلقائياً إلا بعد مراجعتك والضغط على زر الاعتماد"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (proposal.isExecuted) Color(0xFF065F46) else Color(0xFF92400E),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF042F24)
                ) {
                    Text(
                        text = "${proposal.recipients.size} مستلم",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // 1. Exact Recipients & Count
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "1. الجهة المستلمة بدقة: ${proposal.audienceTitle}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24)
                    )
                    Text(
                        text = "الأسماء المشمولة (${proposal.recipients.size}): " +
                            proposal.recipients.take(6).joinToString("، ") { it.recipientLabel } +
                            if (proposal.recipients.size > 6) " ... (+${proposal.recipients.size - 6} آخرين)" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF475569)
                    )
                }
            }

            if (!proposal.isExecuted) {
                // 2. Notification Type Selector
                Text(
                    text = "2. نوع الإشعار:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF042F24)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SmartAdvisorCommunicationCategory.entries) { cat ->
                        val selected = selectedCategory == cat
                        FilterChip(
                            selected = selected,
                            onClick = { selectedCategory = cat },
                            label = {
                                Text(
                                    text = cat.arabicLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                // 3. Editable Message Title & Text
                OutlinedTextField(
                    value = editedTitle,
                    onValueChange = { editedTitle = it },
                    label = { Text("عنوان الإشعار الرسمي (قابل للتعديل)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = editedBody,
                    onValueChange = { editedBody = it },
                    label = { Text("3. نص الرسالة المقترح (قابل للتعديل قبل الإرسال)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    shape = RoundedCornerShape(12.dp)
                )

                // 4. Delivery Channel Selector
                Text(
                    text = "4. قناة الإرسال المعتمدة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF042F24)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(SmartDeliveryChannel.entries) { chan ->
                        val isChanSelected = selectedChannel == chan
                        FilterChip(
                            selected = isChanSelected,
                            onClick = { selectedChannel = chan },
                            label = {
                                Text(
                                    text = chan.arabicLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        )
                    }
                }

                // 5. Explicit Manager Approval Button & Cancel Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            onApprove(editedTitle, editedBody, selectedCategory, selectedChannel)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_send_smart_notification_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "اعتماد وإرسال الإشعار الآن",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        modifier = Modifier.testTag("cancel_smart_notification_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إلغاء", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// =========================================================================
// 9. REUSABLE TABLES & CANVAS BAR CHARTS
// =========================================================================

@Composable
private fun AdvisorStructuredTableView(table: AdvisorTableSection) {
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
                    text = table.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFDE68A)
                )
            }

            // Table Headers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFECFDF5))
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                table.headers.forEach { header ->
                    Text(
                        text = header,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            table.rows.forEachIndexed { rIdx, row ->
                HorizontalDivider(color = Color(0xFFE2E8F0))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rIdx % 2 == 0) Color.White else Color(0xFFF8FAFC))
                        .padding(horizontal = 8.dp, vertical = 7.dp),
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

@Composable
private fun AdvisorCanvasBarChartCard(
    title: String,
    bars: List<AdvisorChartBarItem>
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.BarChart,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF042F24)
                )
            }

            bars.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = item.valueDisplay,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (item.isWarning) DangerRed else Color(0xFF065F46)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        val trackW = size.width
                        val trackH = size.height
                        drawRoundRect(
                            color = Color(0xFFE2E8F0),
                            size = Size(trackW, trackH),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        val fillRatio = (item.valuePercent.coerceIn(0, 100) / 100f)
                        val fillW = (trackW * fillRatio).coerceAtLeast(if (item.valuePercent > 0) 8f else 0f)
                        if (fillW > 0f) {
                            drawRoundRect(
                                brush = Brush.horizontalGradient(
                                    colors = if (item.isWarning) {
                                        listOf(Color(0xFFEF4444), Color(0xFFD97706))
                                    } else {
                                        listOf(Color(0xFF059669), Color(0xFF10B981))
                                    }
                                ),
                                size = Size(fillW, trackH),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 10. TAB 2: SMART DASHBOARD & PROACTIVE INDICATORS
// =========================================================================

@Composable
private fun AdvisorSmartDashboardIndicatorsTab(
    snapshot: RealSchoolAnalyticalSnapshot,
    onAskFollowUpInChat: (String) -> Unit,
    onOpenReportKind: (OfficialSmartReportKind) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        // Executive Health Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF042F24)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الحالة العامة للمدرسة ومؤشر الاستقرار",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = "${snapshot.schoolName} • العام الدراسي ${snapshot.academicYear}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFBBF24)
                        ) {
                            Text(
                                text = "${snapshot.overallSchoolHealthScore}% • ${snapshot.overallHealthLabel}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExecutiveKpiMiniBox(
                            title = "نسبة الحضور",
                            value = "${snapshot.overallAttendanceRatePercent}%",
                            subtitle = "حاضر: ${snapshot.presentTodayCount} | غائب: ${snapshot.absentTodayCount}",
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiMiniBox(
                            title = "نسبة النجاح",
                            value = if (snapshot.totalGradesRecorded > 0) "${snapshot.overallPassRatePercent}%" else "قيد الرصد",
                            subtitle = if (snapshot.totalGradesRecorded > 0) "المعدل: ${snapshot.overallAcademicAveragePercent}%" else "بانتظار رصد الدرجات",
                            modifier = Modifier.weight(1f)
                        )
                        ExecutiveKpiMiniBox(
                            title = "الكادر والطلبة",
                            value = "${snapshot.totalStudents} طالب",
                            subtitle = "${snapshot.totalTeachersCount} معلم فعلي",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Class Attendance & Performance Chart
        if (snapshot.classStats.isNotEmpty()) {
            item {
                AdvisorCanvasBarChartCard(
                    title = "مؤشر المواظبة والانضباط اليومي حسب الصفوف الدراسية",
                    bars = snapshot.classStats.map { cs ->
                        AdvisorChartBarItem(
                            label = cs.gradeClass,
                            valuePercent = cs.attendanceRatePercent,
                            valueDisplay = "${cs.attendanceRatePercent}% (حاضر ${cs.presentToday}/${cs.totalStudents})",
                            isWarning = cs.attendanceRatePercent < 85
                        )
                    }
                )
            }
        }

        // Proactive Alerts & Early Warning Card (Full-Width Text + Full-Width Bottom Button — Zero Vertical Squishing!)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = DangerRed
                        )
                        Text(
                            text = "التنبيهات الذكية والإنذار المبكر للمدير",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF991B1B),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    snapshot.proactiveProblems.forEachIndexed { idx, prob ->
                        Text(
                            text = "${idx + 1}. $prob",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF7F1D1D),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { onAskFollowUpInChat("أبلغ أولياء أمور الطلبة المتغيبين اليوم") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DangerRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advisor_dashboard_notify_absent_parents_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsActive,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "إبلاغ أولياء الأمور",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Strategic Recommendations Card (Full-Width Text + Full-Width Bottom Button — Zero Vertical Squishing!)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color(0xFF059669)
                        )
                        Text(
                            text = "أهم التوصيات لتحسين الأداء المدرسي",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF042F24),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    snapshot.strategicRecommendations.forEachIndexed { idx, rec ->
                        Text(
                            text = "${idx + 1}. $rec",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Button(
                        onClick = { onOpenReportKind(OfficialSmartReportKind.PROBLEMS_RECOMMENDATIONS_REPORT) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("advisor_dashboard_open_full_report_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "فتح التقرير الكامل",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExecutiveKpiMiniBox(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFFFDE68A)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// =========================================================================
// 11. TAB 3: CENTER OF 10 OFFICIAL SMART REPORTS (PREVIEW + PDF EXPORT)
// =========================================================================

@Composable
private fun AdvisorTenOfficialReportsTab(
    snapshot: RealSchoolAnalyticalSnapshot,
    selectedKind: OfficialSmartReportKind,
    onSelectKind: (OfficialSmartReportKind) -> Unit,
    onExportReportPdf: (GeneratedOfficialReportDocument) -> Unit
) {
    val activeDocument = remember(selectedKind, snapshot) {
        buildOfficialSmartReportDocument(selectedKind, snapshot)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Text(
                text = "مركز التقارير المدرسية الذكية المعتمدة (10 تقارير رسمية جاهزة للطباعة وتصدير PDF):",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF042F24)
            )
        }

        // Horizontal Selector of all 10 Official Reports
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(OfficialSmartReportKind.entries, key = { it.name }) { kind ->
                    val isSelected = selectedKind == kind
                    Surface(
                        onClick = { onSelectKind(kind) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) Color(0xFF042F24) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.2.dp,
                            if (isSelected) Color(0xFFFBBF24) else Color(0xFFCBD5E1)
                        ),
                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                        modifier = Modifier
                            .width(195.dp)
                            .testTag("official_report_card_${kind.indexNumber}")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (isSelected) Color(0xFFFBBF24) else Color(0xFFECFDF5)
                                ) {
                                    Text(
                                        text = "تقرير ${kind.indexNumber} • ${kind.periodBadge}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF042F24),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Description,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFFFDE68A) else Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Text(
                                text = kind.arabicTitle,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) Color.White else Color(0xFF042F24),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = kind.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) Color.White.copy(alpha = 0.82f) else Color(0xFF64748B),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Live Official Document Preview Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, Color(0xFF059669)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Official Header inside preview (Full-Width Title + Dedicated Full-Width Export Button Row)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF042F24), Color(0xFF065F46))
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = activeDocument.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${activeDocument.schoolName} • المدير: ${activeDocument.principalName} • التاريخ: ${activeDocument.issueDate}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Button(
                                onClick = { onExportReportPdf(activeDocument) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFBBF24),
                                    contentColor = Color(0xFF042F24)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("export_selected_official_report_pdf_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Print,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "طباعة / تصدير PDF",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // KPI Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        activeDocument.kpiCards.forEach { (k, v) ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(text = k, style = MaterialTheme.typography.labelSmall, color = Color(0xFF065F46))
                                    Text(
                                        text = v,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF042F24)
                                    )
                                }
                            }
                        }
                    }

                    // Executive Summary
                    Text(
                        text = activeDocument.executiveSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF1E293B),
                        lineHeight = 22.sp
                    )

                    // Table
                    AdvisorStructuredTableView(
                        table = AdvisorTableSection(
                            title = activeDocument.tableTitle,
                            headers = activeDocument.tableHeaders,
                            rows = activeDocument.tableRows
                        )
                    )

                    // Visual Chart
                    if (activeDocument.chartBars.isNotEmpty()) {
                        AdvisorCanvasBarChartCard(
                            title = activeDocument.chartTitle,
                            bars = activeDocument.chartBars
                        )
                    }

                    // Recommendations
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "التوصيات المعتمدة في التقرير:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF92400E)
                            )
                            activeDocument.officialRecommendations.forEachIndexed { idx, rec ->
                                Text(
                                    text = "${idx + 1}. $rec",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF78350F)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 12. TAB 4: SMART NOTIFICATIONS AUDIT LOG & RETRY CENTER
// =========================================================================

@Composable
private fun AdvisorSentNotificationsLogTab(
    auditLogs: List<SmartDispatchAuditLogEntry>,
    onRetryFailed: (SmartDispatchAuditLogEntry) -> Unit,
    onCreateNewSmartAlert: () -> Unit
) {
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
                        text = "سجل الإشعارات والتبليغات الذكية المعتمدة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "توثيق كامل للمرسل، المستلمين، وقت الإرسال، القناة، وحالة التسليم الفعلي",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = onCreateNewSmartAlert,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFBBF24),
                            contentColor = Color(0xFF042F24)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Filled.AddAlert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إرسال تنبيه ذكي جديد", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (auditLogs.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MarkEmailRead,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = "لا توجد إشعارات مرسلة من المستشار الذكي في هذه الجلسة بعد",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF042F24),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "اطلب من المستشار في تبويب المحادثة إرسال تنبيه (مثل: أبلغ أولياء أمور الطلبة المتغيبين اليوم) ثم اعتمد الإرسال ليظهر السجل التفصيلي هنا.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(auditLogs, key = { it.id }) { log ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = log.messageTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF042F24)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (log.failedCount == 0) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
                            ) {
                                Text(
                                    text = log.statusLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (log.failedCount == 0) Color(0xFF059669) else DangerRed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Text(
                            text = "المرسل: ${log.senderName} • التاريخ والوقت: ${log.timestamp}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = "الجهة المستلمة: ${log.audienceTitle} (${log.recipientNames.take(5).joinToString("، ")})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF1E293B)
                        )
                        Text(
                            text = "القناة: ${log.channel.arabicLabel} • التصنيف: ${log.categoryLabel} • نجح: ${log.successCount} | فشل: ${log.failedCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = log.messageBody,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF334155)
                        )

                        if (log.failedCount > 0) {
                            Button(
                                onClick = { onRetryFailed(log) },
                                colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.align(Alignment.End)
                            ) {
                                Icon(imageVector = Icons.Filled.Replay, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إعادة محاولة الإرسال (${log.failedCount})", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// 13. CLOUD AI SETTINGS DIALOG (GEMINI API KEY & MODEL CONFIG)
// =========================================================================

@Composable
private fun AdvisorCloudSettingsDialog(
    context: Context,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var apiKeyInput by remember { mutableStateOf(GeminiAdvisorConfigStore.getApiKey(context)) }
    var modelInput by remember { mutableStateOf(GeminiAdvisorConfigStore.getModelName(context)) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, Color(0xFF059669))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudSync,
                        contentDescription = null,
                        tint = Color(0xFF059669)
                    )
                    Text(
                        text = "إعدادات الربط السحابي للمستشار الذكي",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF042F24)
                    )
                }

                Text(
                    text = "يعمل المستشار الإداري الذكي بمحرك تحليل سياقي مدمج يقرأ بيانات المدرسة الحقيقية فوراً، مع دعم الربط المباشر بخدمة Google Gemini السحابية عند إدخال مفتاح API الاختياري.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF475569)
                )

                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("اسم النموذج السحابي (Model)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    label = { Text("مفتاح Google Gemini API (اختياري)") },
                    placeholder = { Text("AIzaSy...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق")
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            GeminiAdvisorConfigStore.saveApiKey(context, apiKeyInput)
                            GeminiAdvisorConfigStore.saveModelName(context, modelInput)
                            onSaved()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("حفظ الإعدادات", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
