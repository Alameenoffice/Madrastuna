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
    const val COL_SCHEDULES_PERIODS = "schedules_periods"
    const val COL_EXAMS = "exams"
    const val COL_LEAVE_REQUESTS = "leave_requests"
    const val COL_BUS_TRACKING = "bus_tracking"
    const val COL_STUDY_MATERIALS = "study_materials"
    const val COL_FEATURE_SETTINGS = "feature_settings"
    const val COL_DAILY_BEHAVIOR = "daily_behavior"
    const val COL_HOMEWORK_SUBMISSIONS = "homework_submissions"
    const val COL_QUICK_QUIZZES = "quick_quizzes"
    const val COL_DISMISSAL_CALLS = "dismissal_calls"
    const val COL_LOST_AND_FOUND = "lost_and_found"
    const val COL_PRIVATE_SUGGESTIONS = "private_suggestions"

    // Root collections for Developer Super-Admin Panel
    const val COL_SCHOOL_LICENSES = "school_licenses"
    const val COL_SCHOOLS_REGISTRY = "schools"
    const val COL_DEV_BROADCASTS = "developer_broadcasts"
    const val COL_APP_UPDATE_CONFIG = "global_app_update_config"
    const val COL_OCCASION_THEME = "global_occasion_theme"

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
    EXCUSED("مجاز (غائب بعذر)")
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
    val adminUsername: String = "",
    val subscriptionExpiryDate: String = "2027-10-29"
) {
    fun daysUntilExpiry(): Long = DateCountdownHelper.daysUntilDate(subscriptionExpiryDate) ?: 365L
    fun isWithinRenewalWarningWindow(): Boolean = daysUntilExpiry() <= 14L
}

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
    val phone: String = "07512362567",
    val subscriptionExpiryDate: String = "2027-10-29",
    val featureSettings: SchoolFeatureSettings = SchoolFeatureSettings()
) {
    fun daysUntilExpiry(): Long = DateCountdownHelper.daysUntilDate(subscriptionExpiryDate) ?: 365L
    fun isWithinRenewalWarningWindow(): Boolean = daysUntilExpiry() <= 14L
}

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
    val heartCount: Int = 0,
    val likedByUserIds: List<String> = emptyList(),
    val isBusSubscribed: Boolean = false,
    val busRouteName: String = "الخط الصباحي والمسائي",
    val schoolId: String = ""
) {
    val qrCodeValue: String
        get() = "MADRASATI|${schoolId.ifBlank { "100" }}|$id|$studentName|$gradeClass"

    /**
     * Returns the student's full compound name including their father's name (الاسم الثنائي/الكامل)
     * without duplicating the father's name if it is already part of studentName.
     */
    val fullNameWithFather: String
        get() {
            val cleanStudent = studentName.trim()
            val cleanFather = parentName
                .replace("ولي أمر الطالب", "")
                .replace("ولي أمر", "")
                .replace("السيد", "")
                .replace("الأستاذ", "")
                .trim()
            if (cleanFather.isEmpty()) return cleanStudent
            if (cleanStudent.contains(cleanFather, ignoreCase = true)) return cleanStudent
            val firstFatherToken = cleanFather.split(" ").firstOrNull()?.trim().orEmpty()
            val studentTokens = cleanStudent.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
            return if (studentTokens.size >= 2 && firstFatherToken.isNotEmpty() && studentTokens.drop(1).any { it == firstFatherToken }) {
                cleanStudent
            } else {
                "$cleanStudent $cleanFather"
            }
        }
}

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
) {
    fun daysRemainingUntilExam(): Long? = DateCountdownHelper.daysUntilDate(examDate)
}

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
    // هل اكتملت جميع الامتحانات (شهر 1، شهر 2، نصف السنة، النهائي)؟
    val isAllExamsCompleted: Boolean
        get() = month1Score > 0 && month2Score > 0 && midYearScore > 0 && finalExamScore > 0

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

    // حالة الطالب الأكاديمية: لا يُحكم بالرسوب أو النجاح النهائي إلا بعد اكتمال جميع الامتحانات
    val academicStatusLabel: String
        get() = when {
            !isAllExamsCompleted -> "مستمر"
            normalizedPercentage >= 50 -> "ناجح"
            else -> "راسب"
        }

    // التقدير التلقائي (لا يظهر "راسب" إطلاقاً أثناء العام الدراسي طالما لم تكتمل جميع الامتحانات)
    val ratingLabel: String
        get() = when {
            !isAllExamsCompleted -> "مستمر"
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

data class ChatContactItem(
    val accountId: String,
    val fullName: String,
    val role: UserRole,
    val subtitle: String,
    val gradeClass: String = "",
    val sectionName: String = "",
    val phone: String = "",
    val photoBase64: String = "",
    val linkedStudentId: String = "",
    val linkedStudentName: String = ""
)

data class ChatMessage(
    val id: String,
    val studentId: String,
    val studentName: String,
    val senderId: String = "",
    val senderName: String,
    val senderRole: UserRole,
    val recipientId: String = "",
    val recipientName: String = "",
    val recipientRole: UserRole = UserRole.PARENT,
    val conversationKey: String = "",
    val messageText: String,
    val timestamp: String,
    val imageBase64: String = "",
    val schoolId: String = ""
) {
    companion object {
        fun buildConversationKey(userId1: String, userId2: String): String {
            return listOf(userId1.trim(), userId2.trim()).sorted().joinToString("__")
        }
    }

    fun matchesConversation(currentUser: UserAccount, contact: ChatContactItem): Boolean {
        val expectedKey = buildConversationKey(currentUser.id, contact.accountId)
        if (conversationKey.isNotEmpty()) {
            return conversationKey == expectedKey
        }
        if (senderId.isNotEmpty() && recipientId.isNotEmpty()) {
            return (senderId == currentUser.id && recipientId == contact.accountId) ||
                (senderId == contact.accountId && recipientId == currentUser.id)
        }
        // Fallback for legacy student-scoped messages with Administration
        val targetStuId = contact.linkedStudentId.ifBlank { currentUser.linkedStudentId }
        val involvesAdmin = currentUser.role == UserRole.ADMIN || contact.role == UserRole.ADMIN
        val involvesParentOrStudent = currentUser.role == UserRole.PARENT ||
            currentUser.role == UserRole.STUDENT ||
            contact.role == UserRole.PARENT ||
            contact.role == UserRole.STUDENT
        return involvesAdmin && involvesParentOrStudent && targetStuId.isNotEmpty() && studentId == targetStuId
    }
}

data class SchoolEvent(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val date: String,
    val location: String = "الموصل - زمار",
    val imageBase64: String = "",
    val authorName: String = "إدارة المدرسة",
    val heartCount: Int = 0,
    val likedByUserIds: List<String> = emptyList(),
    val schoolId: String = ""
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetStudentId: String = "", // Specific student & his parent only
    val targetUserId: String = "",    // Specific account ID only
    val targetGradeClass: String = "", // Specific class (e.g. for Homework / Exam)
    val targetSectionName: String = "", // Specific section (e.g. for Homework)
    val targetRole: String = "",       // Optional role filter (e.g. "ADMIN" or "PARENT")
    val senderUserId: String = "",    // Sender user ID (never notify the sender himself)
    val createdAt: String,
    val isRead: Boolean = false,
    val schoolId: String = ""
) {
    /**
     * Strict Notification Targeting Rule:
     * - Never notify the sender who triggered the action.
     * - If targetUserId is set: ONLY that exact user receives it.
     * - If targetRole is set (e.g. "ADMIN" or "PARENT"): user role must match.
     * - If targetStudentId is set (grades, absence/lateness, report, tuition, private message, QR arrival, bus):
     *   ONLY that student (unless targetRole == "PARENT") and that student's parent receive it.
     * - If targetGradeClass is set (homework/daily preparation, study material, or class exam):
     *   ONLY students and parents belonging to that exact gradeClass (and targetSectionName if specified) receive it.
     * - Otherwise (general school announcement with no specific student/class target):
     *   visible to members of the school.
     */
    fun isTargetedForUser(
        user: UserAccount,
        linkedStudent: StudentRecord?,
        allParentChildIds: Set<String> = emptySet()
    ): Boolean {
        if (senderUserId.isNotEmpty() && senderUserId == user.id) return false

        if (targetUserId.isNotEmpty()) {
            return user.id == targetUserId
        }

        if (targetRole.isNotEmpty() && !user.role.name.equals(targetRole.trim(), ignoreCase = true)) {
            return false
        }

        if (targetStudentId.isNotEmpty()) {
            if (user.role != UserRole.STUDENT && user.role != UserRole.PARENT) return false
            val matchesPrimaryLink = user.linkedStudentId == targetStudentId
            val matchesAnyParentChild = user.role == UserRole.PARENT && targetStudentId in allParentChildIds
            return matchesPrimaryLink || matchesAnyParentChild
        }

        if (targetGradeClass.isNotEmpty()) {
            if (user.role != UserRole.STUDENT && user.role != UserRole.PARENT) return false
            val userClass = linkedStudent?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
            val userSection = linkedStudent?.sectionName?.ifBlank { user.sectionName } ?: user.sectionName
            if (!userClass.trim().equals(targetGradeClass.trim(), ignoreCase = true)) return false
            if (targetSectionName.isNotEmpty() &&
                !userSection.trim().equals(targetSectionName.trim(), ignoreCase = true)
            ) {
                return false
            }
            return true
        }

        return true
    }
}

enum class LeaveType(val arabicLabel: String) {
    SICK("إجازة مرضية"),
    TIME("إجازة زمنية"),
    FAMILY("عذر عائلي")
}

enum class LeaveRequestStatus(val arabicLabel: String) {
    PENDING("قيد المراجعة"),
    APPROVED("تمت الموافقة"),
    REJECTED("مرفوضة")
}

data class LeaveRequestItem(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val parentName: String,
    val parentPhone: String,
    val leaveType: LeaveType = LeaveType.SICK,
    val leaveDate: String,
    val durationDays: Int = 1,
    val reason: String,
    val status: LeaveRequestStatus = LeaveRequestStatus.PENDING,
    val adminResponseNote: String = "",
    val createdAt: String,
    val schoolId: String = ""
)

data class StudyMaterialItem(
    val id: String,
    val title: String,
    val subjectName: String,
    val gradeClass: String,
    val sectionName: String = "",
    val teacherName: String,
    val description: String,
    val fileUrl: String = "",
    val imageBase64: String = "",
    val materialType: String = "ملخص دراسي / ملزمة PDF",
    val createdAt: String,
    val schoolId: String = ""
)

enum class BusEventType(
    val arabicLabel: String,
    val notificationTitle: String,
    val notificationBodyTemplate: String
) {
    MORNING_DEPARTURE(
        arabicLabel = "انطلاق الخط صباحاً",
        notificationTitle = "انطلاق خط النقل المدرسي صباحاً",
        notificationBodyTemplate = "انطلق خط النقل المدرسي صباحاً باتجاه منازل الطلبة لنقل ابنكم (%s) إلى المدرسة."
    ),
    ARRIVED_AT_SCHOOL(
        arabicLabel = "وصول للمدرسة",
        notificationTitle = "وصول الخط المدرسي إلى المدرسة",
        notificationBodyTemplate = "وصل خط النقل المدرسي بسلامة الله إلى المدرسة وعلى متنه ابنكم (%s)."
    ),
    RETURN_HOME_DEPARTURE(
        arabicLabel = "انطلاق العودة للمنزل",
        notificationTitle = "انطلاق خط العودة إلى المنزل",
        notificationBodyTemplate = "انطلق خط النقل المدرسي الآن للعودة إلى المنزل وعلى متنه ابنكم (%s)."
    )
}

data class DeveloperBroadcastItem(
    val id: String,
    val title: String,
    val message: String,
    val senderName: String = "المطور أحمد أبو رسلان",
    val createdAt: String,
    val targetSchoolId: String = "ALL"
)

data class StudentAutoBadge(
    val id: String,
    val title: String,
    val subtitle: String,
    val isGoldAttendance: Boolean
)

object DateCountdownHelper {
    fun daysUntilDate(dateStr: String): Long? {
        return try {
            val clean = dateStr.trim().take(10)
            if (clean.length < 8) return null
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
            sdf.isLenient = false
            val targetDate = sdf.parse(clean) ?: return null
            val todayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val targetCal = java.util.Calendar.getInstance().apply {
                time = targetDate
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
            java.util.concurrent.TimeUnit.MILLISECONDS.toDays(diffMillis)
        } catch (_: Exception) {
            null
        }
    }

    fun defaultExpiryDatePlusDays(daysToAdd: Int = 365): String {
        val cal = java.util.Calendar.getInstance().apply {
            add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
        }
        return java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
    }
}

/**
 * Modular Feature Toggles for School Principal (لوحة تحكم المدير في تفعيل أو إيقاف مزايا مدرسته)
 */
data class SchoolFeatureSettings(
    val honorRollEnabled: Boolean = true,
    val eventsGalleryEnabled: Boolean = true,
    val directChatEnabled: Boolean = true,
    val leaveRequestsEnabled: Boolean = true,
    val tuitionEnabled: Boolean = true,
    val busTrackingEnabled: Boolean = true,
    val studyMaterialsEnabled: Boolean = true,
    val quickQuizzesEnabled: Boolean = true,
    val dailyBehaviorEnabled: Boolean = true,
    val dismissalAndLostFoundEnabled: Boolean = true,
    val suggestionsBoxEnabled: Boolean = true,
    val digitalIdEnabled: Boolean = true,
    val busAlertsEnabled: Boolean = busTrackingEnabled,
    val studyLibraryEnabled: Boolean = studyMaterialsEnabled,
    val qrScannerEnabled: Boolean = digitalIdEnabled,
    val qrAttendanceEnabled: Boolean = digitalIdEnabled,
    val homeworkSubmissionEnabled: Boolean = true,
    val dismissalCallEnabled: Boolean = true,
    val lostAndFoundEnabled: Boolean = true
) {
    fun normalized(): SchoolFeatureSettings {
        val bus = busAlertsEnabled && busTrackingEnabled || (busAlertsEnabled != busTrackingEnabled && busAlertsEnabled)
        val study = studyLibraryEnabled && studyMaterialsEnabled || (studyLibraryEnabled != studyMaterialsEnabled && studyLibraryEnabled)
        val qr = qrScannerEnabled && qrAttendanceEnabled && digitalIdEnabled || (qrScannerEnabled != digitalIdEnabled && qrScannerEnabled) || (qrAttendanceEnabled != digitalIdEnabled && qrAttendanceEnabled)
        val lostAndSug = lostAndFoundEnabled
        return copy(
            busTrackingEnabled = bus,
            busAlertsEnabled = bus,
            studyMaterialsEnabled = study,
            studyLibraryEnabled = study,
            digitalIdEnabled = qr,
            qrScannerEnabled = qr,
            qrAttendanceEnabled = qr,
            lostAndFoundEnabled = lostAndSug,
            suggestionsBoxEnabled = lostAndSug,
            dismissalAndLostFoundEnabled = dismissalCallEnabled || lostAndSug
        )
    }
}

/**
 * Smart Parent Auto-Link Match Info (الاكتشاف التلقائي لولي الأمر المسجل مسبقاً وربط الإخوة)
 */
data class SiblingParentMatchInfo(
    val parentName: String,
    val parentPhone: String,
    val parentUsername: String,
    val parentPassword: String,
    val existingChildrenNames: List<String>,
    val address: String
)

/**
 * 7.a Daily Classroom Follow-up & Behavior Evaluation (دفتر المتابعة اليومي والسلوك)
 */
enum class DailyBehaviorType(
    val arabicLabel: String,
    val notificationTitle: String,
    val isPositive: Boolean,
    val emoji: String = ""
) {
    EXCELLENT_PARTICIPATION(
        arabicLabel = "مشارك ممتاز",
        notificationTitle = "إشادة يومية: مشارك ممتاز",
        isPositive = true,
        emoji = ""
    ),
    FORGOT_BOOK(
        arabicLabel = "نسي الكتاب",
        notificationTitle = "ملاحظة متابعة: نسيان الكتاب المدرسي",
        isPositive = false,
        emoji = ""
    ),
    MISSED_HOMEWORK(
        arabicLabel = "لم يحل الواجب",
        notificationTitle = "تنبيه دراسي: عدم حل الواجب البيتي",
        isPositive = false,
        emoji = ""
    ),
    BEHAVIOR_NOTE(
        arabicLabel = "ملاحظة سلوكية",
        notificationTitle = "ملاحظة سلوكية داخل الصف",
        isPositive = false,
        emoji = ""
    );

    val arabicTitle: String
        get() = arabicLabel
}

data class DailyBehaviorLogItem(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val subjectName: String,
    val behaviorType: DailyBehaviorType,
    val note: String,
    val teacherName: String,
    val date: String,
    val schoolId: String = ""
)

/**
 * 7.b Photo Homework Submission & Auto-Graded Quick Quizzes (تسليم الواجبات المصور والاختبارات السريعة)
 */
enum class HomeworkSubmissionStatus(val arabicLabel: String) {
    PENDING_REVIEW("قيد مراجعة المعلم"),
    REVIEWED_CORRECTED("تمت المراجعة والتصحيح")
}

data class HomeworkSubmissionItem(
    val id: String,
    val homeworkId: String,
    val subjectName: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val note: String,
    val photoBase64: String,
    val submittedAt: String,
    val status: HomeworkSubmissionStatus = HomeworkSubmissionStatus.PENDING_REVIEW,
    val teacherFeedback: String = "",
    val homeworkScore: String = "",
    val reviewedByTeacher: String = "",
    val schoolId: String = ""
) {
    val solutionImageBase64: String
        get() = photoBase64

    val isReviewed: Boolean
        get() = status == HomeworkSubmissionStatus.REVIEWED_CORRECTED
}

enum class QuickQuizType(val arabicLabel: String) {
    MULTIPLE_CHOICE("اختيار من متعدد"),
    TRUE_FALSE("صح أو خطأ")
}

data class QuickQuizAnswerView(
    val quizId: String,
    val studentId: String,
    val selectedOptionIndex: Int,
    val isCorrect: Boolean,
    val earnedPoints: Int
)

data class QuickQuizItem(
    val id: String,
    val gradeClass: String,
    val sectionName: String,
    val subjectName: String,
    val teacherName: String,
    val questionText: String,
    val quizType: QuickQuizType = QuickQuizType.MULTIPLE_CHOICE,
    val options: List<String>,
    val correctOptionIndex: Int,
    val createdAt: String,
    val studentAnswers: Map<String, Int> = emptyMap(), // studentId -> chosenIndex
    val points: Int = 5,
    val schoolId: String = ""
)

/**
 * 7.c Smart Dismissal Call + School Lost & Found Registry (الاستدعاء السريع عند الانصراف + سجل المفقودات)
 */
data class DismissalCallItem(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val parentName: String,
    val parentPhone: String,
    val requestedAtTime: String,
    val status: String = "بانتظار خروج الطالب", // or "تم تسليم الطالب لولي الأمر"
    val isHandled: Boolean = false,
    val schoolId: String = ""
) {
    val isAcknowledged: Boolean
        get() = isHandled

    val requestedAt: String
        get() = requestedAtTime
}

enum class LostAndFoundStatus(val arabicLabel: String) {
    AT_ADMINISTRATION("موجود في الإدارة"),
    RETURNED_TO_OWNER("تم التسليم لصاحبه")
}

data class LostAndFoundItem(
    val id: String,
    val title: String,
    val description: String,
    val foundLocation: String,
    val date: String,
    val photoBase64: String = "",
    val status: LostAndFoundStatus = LostAndFoundStatus.AT_ADMINISTRATION,
    val publisherName: String = "إدارة المدرسة",
    val schoolId: String = ""
) {
    val isClaimed: Boolean
        get() = status == LostAndFoundStatus.RETURNED_TO_OWNER
}

/**
 * 7.d Private Principal Suggestions & Complaints Box (صندوق المقترحات والشكاوى السري للمدير)
 */
data class PrivateSuggestionItem(
    val id: String,
    val senderName: String,
    val senderRoleLabel: String,
    val category: String, // مقترح تطويري / ملاحظة إدارية / شكوى سرية
    val title: String,
    val message: String,
    val createdAt: String,
    val adminReply: String = "",
    val schoolId: String = ""
) {
    val senderRoleArabic: String
        get() = senderRoleLabel

    val messageText: String
        get() = message
}

/**
 * 8.a Developer App Updates & Live Occasion Theme / Icon Config (إدارة تحديثات التطبيق وثيم المناسبات المباشر)
 */
data class AppUpdateConfig(
    val latestVersionCode: Int = 2,
    val latestVersionName: String = "2.5.0",
    val apkDownloadUrl: String = "https://github.com/madrasati-smart/releases/latest/download/app-debug.apk",
    val releaseNotes: String = "تحديث شامل يتضمن نظام QR Code الحقيقي، ربط الإخوة الذكي، الماسح الضوئي المباشر، والمزايا الميدانية اليومية.",
    val isForceUpdate: Boolean = false,
    val isUpdateBroadcastActive: Boolean = false,
    val updatedAt: String = "2026-10-29"
) {
    val isUpdateActive: Boolean
        get() = isUpdateBroadcastActive

    val updateNotes: String
        get() = releaseNotes

    val downloadUrl: String
        get() = apkDownloadUrl
}

enum class OccasionThemeType(
    val arabicLabel: String,
    val badgeTitle: String,
    val bannerGreeting: String,
    val launcherAliasName: String = ".MainActivityDefault",
    val defaultPrimaryHex: String = "#064E3B",
    val defaultGradientHex: String = "#059669",
    val emojiSymbol: String = ""
) {
    OFFICIAL(
        arabicLabel = "الوضع الرسمي الفخم",
        badgeTitle = "الهوية الزمردية الملكية الرسمية",
        bannerGreeting = "أهلاً بكم في منصة مدرستي الذكية للتعليم الحديث",
        launcherAliasName = ".MainActivityDefault",
        defaultPrimaryHex = "#064E3B",
        defaultGradientHex = "#059669",
        emojiSymbol = ""
    ),
    RAMADAN_EID(
        arabicLabel = "رمضان المبارك والعيد",
        badgeTitle = "نفحات شهر رمضان المبارك والعيد السعيد",
        bannerGreeting = "رمضان كريم وكل عام وأسرتنا التربوية والطلابية بألف خير",
        launcherAliasName = ".MainActivityRamadan",
        defaultPrimaryHex = "#064E3B",
        defaultGradientHex = "#B45309",
        emojiSymbol = ""
    ),
    NEW_YEAR(
        arabicLabel = "رأس السنة والعام الجديد",
        badgeTitle = "احتفالية العام الميلادي والدراسي الجديد",
        bannerGreeting = "كل عام وأنتم بخير • عام دراسي مليء بالتفوق والنجاح",
        launcherAliasName = ".MainActivityNewYear",
        defaultPrimaryHex = "#1E1B4B",
        defaultGradientHex = "#B45309",
        emojiSymbol = ""
    ),
    ACADEMIC_EXCELLENCE(
        arabicLabel = "موسم التفوق والامتحانات",
        badgeTitle = "موسم الحصاد والتفوق العلمي الذهبي",
        bannerGreeting = "نبارك لطلبتنا الأعزاء موسم التميز والتفوق الدراسي",
        launcherAliasName = ".MainActivityHonor",
        defaultPrimaryHex = "#065F46",
        defaultGradientHex = "#D97706",
        emojiSymbol = ""
    ),
    NATIONAL_DAY(
        arabicLabel = "العيد الوطني / مناسبة وطنية",
        badgeTitle = "احتفالية العيد الوطني والمناسبة الوطنية",
        bannerGreeting = "دام عز الوطن شامخاً بالعلم والمعرفة والبناء",
        launcherAliasName = ".MainActivityNational",
        defaultPrimaryHex = "#047857",
        defaultGradientHex = "#0F766E",
        emojiSymbol = ""
    ),
    CUSTOM_MANUAL(
        arabicLabel = "ثيم يدوي مخصص",
        badgeTitle = "مناسبة خاصة مخصصة",
        bannerGreeting = "تهنئة خاصة لجميع إدارات ومعلمي وطلاب المنصة",
        launcherAliasName = ".MainActivityDefault",
        defaultPrimaryHex = "#064E3B",
        defaultGradientHex = "#D97706",
        emojiSymbol = ""
    );

    val iconEmoji: String
        get() = emojiSymbol

    val badgeEmoji: String
        get() = emojiSymbol

    val arabicTitle: String
        get() = badgeTitle

    val defaultBanner: String
        get() = bannerGreeting

    val greetingBanner: String
        get() = bannerGreeting

    companion object {
        val DEFAULT: OccasionThemeType = OFFICIAL
    }
}

data class OccasionThemeConfig(
    val themeType: OccasionThemeType = OccasionThemeType.OFFICIAL,
    val customTitle: String = "",
    val customBannerText: String = "",
    val customIconBase64: String = "",
    val customIconUrl: String = "",
    val primaryColorHex: String = "#064E3B",
    val gradientColorHex: String = "#059669",
    val launcherAliasName: String = ".MainActivityDefault",
    val updatedAt: String = "2026-10-29"
) {
    val occasionTheme: OccasionThemeType
        get() = themeType

    val customGreeting: String
        get() = customBannerText

    val customIconEmoji: String
        get() = themeType.emojiSymbol

    val effectiveTitle: String
        get() = customTitle.ifBlank { themeType.badgeTitle }

    val effectiveBannerText: String
        get() = customBannerText.ifBlank { themeType.bannerGreeting }

    val effectivePrimaryHex: String
        get() = primaryColorHex.ifBlank { themeType.defaultPrimaryHex }

    val effectiveGradientHex: String
        get() = gradientColorHex.ifBlank { themeType.defaultGradientHex }

    val effectiveAliasName: String
        get() = launcherAliasName.ifBlank { themeType.launcherAliasName }
}




