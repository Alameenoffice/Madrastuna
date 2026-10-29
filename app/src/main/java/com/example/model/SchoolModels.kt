package com.example.model

import kotlin.math.roundToInt

/**
 * Official Firebase Configuration for Multi-Tenant School Isolation (schools/{schoolId}/...)
 */
object FirebaseConfigConstants {
    const val API_KEY = "AIzaSyD0cqrHd0A4q1VPnzjQoN7IP6Qnf4aNtG0"
    const val AUTH_DOMAIN = "madrasati-app-d51dd.firebaseapp.com"
    const val DATABASE_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com"
    const val PROJECT_ID = "madrasati-app-d51dd"
    const val STORAGE_BUCKET = "madrasati-app-d51dd.firebasestorage.app"
    const val MESSAGING_SENDER_ID = "125981370793"
    const val APP_ID = "1:125981370793:web:3e78e653e43bfbc791fcb8"

    // Sub-collections isolated under schools/{schoolId}/...
    const val COL_SETTINGS = "settings"
    const val COL_USERS = "users"
    const val COL_SUBJECTS = "subjects"
    const val COL_ATTENDANCE = "attendance"
    const val COL_SCHEDULES_HOMEWORK = "schedules_homework"
    const val COL_GRADES = "grades"
    const val COL_HONOR_ROLL = "honor_roll"
    const val COL_TUITION_FEES = "tuition_fees"
    const val COL_REPORTS = "reports"
    const val COL_MESSAGES = "messages"
    const val COL_EVENTS_GALLERY = "events_gallery"
    const val COL_NOTIFICATIONS = "notifications"

    // Root collections for Developer Super-Admin Panel
    const val COL_SCHOOL_LICENSES = "school_licenses"
    const val COL_SCHOOLS_REGISTRY = "schools"

    // Developer Super-Admin Credentials
    const val DEV_USERNAME = "ABU-RASLAN"
    const val DEV_PASSWORD = "07512362567"

    fun schoolCollectionPath(schoolId: String, subCollection: String): String {
        return "schools/$schoolId/$subCollection"
    }

    /**
     * Automatically prepends the 3-digit school code (e.g. "105") to a username (e.g. "1" -> "105-1")
     * without duplicating the prefix if it is already present.
     */
    fun applySchoolPrefix(schoolCode: String, rawUsername: String): String {
        val cleanCode = schoolCode.trim()
        val cleanUser = rawUsername.trim()
        if (cleanUser.isEmpty()) return ""
        if (cleanCode.isEmpty()) return cleanUser
        val prefix = "$cleanCode-"
        return if (cleanUser.startsWith(prefix, ignoreCase = true)) {
            prefix + cleanUser.substring(prefix.length)
        } else {
            "$prefix$cleanUser"
        }
    }

    /**
     * Extracts the school code prefix (e.g. "105" from "105-1" or "105-ali") when logging in.
     */
    fun extractSchoolPrefix(username: String): String? {
        val trimmed = username.trim()
        if (!trimmed.contains("-")) return null
        val prefix = trimmed.substringBefore("-").trim()
        return if (prefix.length in 2..12 && prefix.all { it.isLetterOrDigit() }) prefix else null
    }
}

enum class SchoolType(val arabicLabel: String) {
    PUBLIC("مدرسة حكومية"),
    PRIVATE("مدرسة أهلية")
}

enum class SchoolStage(
    val arabicLabel: String,
    val defaultClasses: List<String>,
    val defaultSubjects: List<String>
) {
    PRIMARY(
        arabicLabel = "مدرسة ابتدائية",
        defaultClasses = listOf(
            "الأول الابتدائي",
            "الثاني الابتدائي",
            "الثالث الابتدائي",
            "الرابع الابتدائي",
            "الخامس الابتدائي",
            "السادس الابتدائي"
        ),
        defaultSubjects = listOf(
            "التربية الإسلامية",
            "اللغة العربية",
            "اللغة الإنكليزية",
            "الرياضيات",
            "العلوم",
            "الاجتماعيات",
            "التربية الفنية والرياضية"
        )
    ),
    MIDDLE(
        arabicLabel = "مدرسة متوسطة",
        defaultClasses = listOf(
            "الأول المتوسط",
            "الثاني المتوسط",
            "الثالث المتوسط"
        ),
        defaultSubjects = listOf(
            "التربية الإسلامية",
            "اللغة العربية",
            "اللغة الإنكليزية",
            "الرياضيات",
            "الاجتماعيات",
            "الفيزياء",
            "الكيمياء",
            "الأحياء",
            "الحاسوب"
        )
    ),
    PREPARATORY(
        arabicLabel = "مدرسة إعدادية",
        defaultClasses = listOf(
            "الرابع الإعدادي",
            "الخامس الإعدادي",
            "السادس الإعدادي"
        ),
        defaultSubjects = listOf(
            "التربية الإسلامية",
            "اللغة العربية",
            "اللغة الإنكليزية",
            "الرياضيات",
            "الفيزياء",
            "الكيمياء",
            "الأحياء",
            "الحاسوب"
        )
    ),
    SECONDARY(
        arabicLabel = "ثانوية",
        defaultClasses = listOf(
            "الأول المتوسط",
            "الثاني المتوسط",
            "الثالث المتوسط",
            "الرابع الإعدادي",
            "الخامس الإعدادي",
            "السادس الإعدادي"
        ),
        defaultSubjects = listOf(
            "التربية الإسلامية",
            "اللغة العربية",
            "اللغة الإنكليزية",
            "الرياضيات",
            "الاجتماعيات",
            "الفيزياء",
            "الكيمياء",
            "الأحياء",
            "الحاسوب"
        )
    )
}

/**
 * Job title for the school administrator is exclusively "المدير".
 */
enum class UserRole(val arabicTitle: String) {
    ADMIN("المدير"),
    TEACHER("كادر تدريسي"),
    PARENT("ولي أمر"),
    STUDENT("طالب")
}

enum class AttendanceStatus(val arabicLabel: String) {
    PRESENT("حاضر"),
    ABSENT("غائب"),
    LATE("متأخر"),
    EXCUSED("إجازة")
}

enum class ReportType(val arabicLabel: String) {
    APPRECIATION("شكر وتقدير"),
    ACADEMIC_NOTE("ملاحظة علمية"),
    BEHAVIOR_ALERT("تنبيه سلوكي"),
    PARENT_SUMMONS("استدعاء ولي أمر")
}

enum class NotificationType(val arabicLabel: String) {
    ATTENDANCE("غياب وحضور"),
    HOMEWORK("واجب وتحضير"),
    GRADES("درجات وامتحانات"),
    REPORT("تبليغ إداري"),
    TUITION("قسط دراسي"),
    GENERAL("إشعار عام")
}

enum class AppSection(val arabicTitle: String) {
    DASHBOARD("الرئيسية"),
    ATTENDANCE("الحضور والغياب"),
    SCHEDULE_HOMEWORK("الجدول والواجبات"),
    EXAMS_GRADES("الامتحانات والدرجات"),
    HONOR_ROLL("لوحة الشرف"),
    TUITION_FEES("الأقساط الدراسية"),
    REPORTS_CHAT("البلاغات والتواصل"),
    EVENTS_NEWS("الفعاليات والأخبار"),
    ADMIN_ACCOUNTS("إدارة الحسابات"),
    SETTINGS("الإعدادات")
}

/**
 * Subject configuration item assignable to specific registered classes inside schools/{schoolId}/subjects
 */
data class SubjectConfigItem(
    val id: String,
    val name: String,
    val assignedClasses: List<String>,
    val defaultMaxScore: Int = 100,
    val schoolId: String = ""
)

data class SchoolLicense(
    val code: String,
    val used: Boolean = false,
    val createdAt: String = "2026-10-29",
    val usedBySchoolId: String = "",
    val usedBySchoolName: String = "",
    val usedAt: String = ""
)

data class RegisteredSchoolSummary(
    val schoolId: String,
    val schoolName: String,
    val address: String = "الموصل - زمار",
    val schoolStage: String = "ثانوية",
    val schoolType: String = "مدرسة أهلية",
    val createdAt: String = "2026-10-29",
    val isActive: Boolean = true,
    val adminUsername: String = ""
)

data class SchoolConfig(
    val schoolId: String = "",
    val isSetupCompleted: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String = "2026-10-29",
    val schoolName: String = "مدرستي النموذجية",
    val address: String = "الموصل - زمار",
    val schoolType: SchoolType = SchoolType.PRIVATE,
    val schoolStage: SchoolStage = SchoolStage.SECONDARY,
    val registeredClasses: List<String> = SchoolStage.SECONDARY.defaultClasses,
    val registeredSections: List<String> = listOf("أ", "ب", "ج"),
    val registeredSubjects: List<String> = SchoolStage.SECONDARY.defaultSubjects,
    val subjectConfigs: List<SubjectConfigItem> = emptyList(),
    val logoBase64: String = "",
    val academicYear: String = "2026 - 2027",
    val phone: String = "07512362567"
)

data class UserSession(
    val userId: String,
    val username: String,
    val role: UserRole,
    val schoolId: String,
    val sessionToken: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class ToastAlertMessage(
    val id: String,
    val message: String,
    val isSuccess: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

data class UserAccount(
    val id: String,
    val username: String,
    val password: String,
    val fullName: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val phone: String = "",
    val specialty: String = "", // Comma-separated subjects for TEACHER
    val assignedClasses: String = "", // Comma-separated "Class - Section" or "Class" for TEACHER
    val linkedStudentId: String = "",
    val gradeClass: String = "الأول المتوسط",
    val sectionName: String = "أ",
    val address: String = "الموصل - زمار",
    val photoBase64: String = "",
    val schoolId: String = ""
) {
    val teacherSubjectsList: List<String>
        get() = specialty.split("،", ",", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val teacherClassSectionPairs: List<String>
        get() = assignedClasses.split("،", ",", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    fun canTeacherAccessClassSection(gradeClass: String, sectionName: String? = null): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role != UserRole.TEACHER) return false
        val pairs = teacherClassSectionPairs
        if (pairs.isEmpty()) return true
        return pairs.any { item ->
            if (item.contains("-")) {
                val parts = item.split("-").map { it.trim() }
                val cls = parts.getOrNull(0) ?: ""
                val sec = parts.getOrNull(1)?.replace("شعبة", "")?.trim() ?: ""
                cls == gradeClass && (sectionName == null || sec.isEmpty() || sec == sectionName)
            } else {
                item == gradeClass
            }
        }
    }

    fun canTeacherAccessSubject(subjectName: String): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role != UserRole.TEACHER) return false
        val subs = teacherSubjectsList
        if (subs.isEmpty()) return true
        return subs.any { it.equals(subjectName, ignoreCase = true) || subjectName.contains(it) || it.contains(subjectName) }
    }
}

data class StudentRecord(
    val id: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val parentName: String,
    val parentPhone: String,
    val address: String = "الموصل - زمار",
    val studentUsername: String,
    val studentPassword: String,
    val parentUsername: String,
    val parentPassword: String,
    val isActive: Boolean = true,
    val photoBase64: String = "",
    val excellencePoints: Int = 95,
    val isOnHonorRoll: Boolean = false,
    val honorTitle: String = "وسام التفوق الذهبي",
    val schoolId: String = ""
)

data class AttendanceEntry(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val date: String,
    val status: AttendanceStatus,
    val note: String = "",
    val schoolId: String = ""
)

data class SchedulePeriod(
    val id: String,
    val gradeClass: String,
    val sectionName: String,
    val dayOfWeek: String,
    val periodNumber: Int,
    val subjectName: String,
    val teacherName: String,
    val timeSlot: String,
    val schoolId: String = ""
)

data class HomeworkItem(
    val id: String,
    val gradeClass: String,
    val sectionName: String,
    val subjectName: String,
    val teacherName: String,
    val dailyPreparation: String,
    val homeworkDetails: String,
    val dueDate: String,
    val createdAt: String,
    val schoolId: String = ""
)

data class ExamScheduleItem(
    val id: String,
    val gradeClass: String,
    val subjectName: String,
    val examType: String,
    val examDate: String,
    val dayName: String,
    val timeSlot: String = "08:30 صباحاً",
    val topicsIncluded: String = "",
    val hallNumber: String = "قاعة 1",
    val schoolId: String = ""
)

object GradingRules {
    val tenPointPrimaryClasses = setOf(
        "الأول الابتدائي",
        "الثاني الابتدائي",
        "الثالث الابتدائي",
        "الرابع الابتدائي"
    )

    fun defaultMaxScoreForClass(gradeClass: String): Int {
        return if (gradeClass.trim() in tenPointPrimaryClasses) 10 else 100
    }
}

data class SubjectGrade(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val subjectName: String,
    val dailyScore: Int,
    val month1Score: Int,
    val month2Score: Int,
    val midYearScore: Int,
    val finalExamScore: Int,
    val maxScoreScale: Int = GradingRules.defaultMaxScoreForClass(gradeClass),
    val schoolId: String = ""
) {
    // السعي السنوي (calculated according to maxScoreScale: 10 or 100)
    val annualEffortScore: Int
        get() = (((dailyScore + month1Score + month2Score) / 3.0 * 0.5) + (midYearScore * 0.5))
            .roundToInt()
            .coerceIn(0, maxScoreScale)

    // المعدل النهائي
    val finalAverage: Int
        get() = ((annualEffortScore + finalExamScore) / 2.0)
            .roundToInt()
            .coerceIn(0, maxScoreScale)

    // النسبة المئوية المكافئة للتقدير (0 - 100)
    val normalizedPercentage: Int
        get() = if (maxScoreScale == 10) (finalAverage * 10).coerceIn(0, 100) else finalAverage.coerceIn(0, 100)

    // التقدير التلقائي
    val ratingLabel: String
        get() = when {
            normalizedPercentage >= 90 -> "ممتاز"
            normalizedPercentage >= 80 -> "جيد جداً"
            normalizedPercentage >= 70 -> "جيد"
            normalizedPercentage >= 60 -> "متوسط"
            normalizedPercentage >= 50 -> "مقبول"
            else -> "راسب"
        }
}

data class TuitionInstallment(
    val id: String,
    val amount: Long,
    val paymentDate: String,
    val receiptNumber: String,
    val note: String = "دفعة قسط دراسي"
)

data class TuitionRecord(
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val parentName: String,
    val parentPhone: String,
    val totalFee: Long,
    val nextDueDate: String,
    val installments: List<TuitionInstallment> = emptyList(),
    val schoolId: String = ""
) {
    val paidAmount: Long
        get() = installments.sumOf { it.amount }

    val remainingAmount: Long
        get() = (totalFee - paidAmount).coerceAtLeast(0L)
}

data class ParentReport(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val parentName: String,
    val parentPhone: String,
    val reportType: ReportType,
    val title: String,
    val details: String,
    val senderName: String,
    val createdAt: String,
    val schoolId: String = ""
)

data class ChatMessage(
    val id: String,
    val studentId: String,
    val studentName: String,
    val senderName: String,
    val senderRole: UserRole,
    val messageText: String,
    val timestamp: String,
    val imageBase64: String = "",
    val schoolId: String = ""
)

data class SchoolEvent(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val date: String,
    val location: String = "الموصل - زمار",
    val imageBase64: String = "",
    val authorName: String = "إدارة المدرسة",
    val schoolId: String = ""
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetStudentId: String = "", // empty means broadcast to school
    val createdAt: String,
    val isRead: Boolean = false,
    val schoolId: String = ""
)
