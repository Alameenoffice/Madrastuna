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
    const val COL_URGENT_ALERT = "urgent_alert"
    const val COL_STUDENT_ACTIVITIES = "student_activities"
    const val COL_STUDENT_ACTIVITY_COMMENTS = "student_activity_comments"
    const val COL_ACTIVITY_REPORTS = "activity_reports"
    const val COL_USER_PROFILES = "user_profiles"
    const val COL_HOLIDAYS = "holidays"
    const val COL_TEACHER_RATINGS = "teacher_ratings"
    const val COL_ISSUED_REPORT_CARDS = "issued_report_cards"

    // Root collections for Developer Super-Admin Panel
    const val COL_SCHOOL_LICENSES = "school_licenses"
    const val COL_SCHOOLS_REGISTRY = "schools"
    const val COL_DEV_BROADCASTS = "developer_broadcasts"
    const val COL_APP_UPDATE_CONFIG = "global_app_update_config"
    const val COL_OCCASION_THEME = "global_occasion_theme"
    const val COL_SYSTEM_SECURE_CONFIG = "system_secure_config"
    const val DOC_DEVELOPER_VAULT = "developer_vault"

    // Developer Super-Admin Token (resolved dynamically via SecurityShieldHelper — zero plaintext in code)
    val DEV_USERNAME: String
        get() = com.example.util.SecurityShieldHelper.defaultDeveloperIdentifierToken()

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
    );

    val staffTitleSingular: String
        get() = if (this == PRIMARY) "المعلم" else "المدرس"

    val staffTitlePlural: String
        get() = if (this == PRIMARY) "المعلمين" else "المدرسين"

    val staffIndefiniteSingular: String
        get() = if (this == PRIMARY) "معلم" else "مدرس"

    val staffIndefinitePlural: String
        get() = if (this == PRIMARY) "معلمين" else "مدرسين"
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
    EXCUSED("مجاز"),
    HOLIDAY("عطلة رسمية"),
    UNRECORDED("بانتظار الرصد");

    companion object {
        val manualSelectableStatuses: List<AttendanceStatus> = listOf(
            PRESENT,
            ABSENT,
            LATE,
            EXCUSED
        )
    }
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
    LIVE_OPERATIONS_RADAR("موجز الدوام اليومي"),
    ATTENDANCE("الحضور والغياب"),
    SCHEDULE_HOMEWORK("الجدول والواجبات"),
    EXAMS_GRADES("الامتحانات والدرجات"),
    STUDENT_COMPREHENSIVE_ARCHIVE("السجل الأكاديمي والإحصائي الشامل"),
    HONOR_ROLL("لوحة الشرف"),
    TEACHER_EVALUATION("تقييم الكادر التدريسي"),
    STUDENT_ACTIVITIES("نشاطات الطلبة"),
    TUITION_FEES("الأقساط الدراسية"),
    REPORTS_CHAT("البلاغات والتواصل"),
    EVENTS_NEWS("الفعاليات والأخبار"),
    ADMIN_ACCOUNTS("إدارة الحسابات"),
    SETTINGS("الإعدادات")
}

enum class StudentEnrollmentStatus(val key: String, val arabicLabel: String) {
    ACTIVE("active", "مستمر بالدوام"),
    SUSPENDED_TEMP("suspended_temp", "فصل مؤقت"),
    EXPELLED("expelled", "فصل دائمي"),
    DEREGISTERED("deregistered", "ترقين قيد"),
    TRANSFERRED("transferred", "منقول إلى مدرسة أخرى");

    companion object {
        fun fromKeyOrName(raw: String): StudentEnrollmentStatus {
            val clean = raw.trim()
            if (clean.isEmpty()) return ACTIVE
            return entries.firstOrNull {
                it.name.equals(clean, ignoreCase = true) ||
                    it.key.equals(clean, ignoreCase = true) ||
                    it.arabicLabel == clean
            } ?: ACTIVE
        }
    }
}

data class ArchivedYearlySummary(
    val academicYear: String = "",
    val gradeClass: String = "",
    val sectionName: String = "",
    val finalAveragePercentage: Int = 0,
    val ratingLabel: String = "جيد",
    val resultStatus: String = "", // "ناجح ومُرحّل" | "راسب ومُعيد"
    val promotedToClass: String = "",
    val archivedAt: String = "",
    val overallAverage: String = if (finalAveragePercentage > 0) "$finalAveragePercentage%" else "",
    val presentDays: Int = 0,
    val absentDays: Int = 0,
    val excusedDays: Int = 0,
    val subjectsSummary: String = ""
) {
    val finalAverage: Int get() = finalAveragePercentage
    val totalPresentDays: Int get() = presentDays
    val totalAbsentDays: Int get() = absentDays
    val totalExcusedDays: Int get() = excusedDays
    val promotedToGrade: String get() = promotedToClass
}

data class PromotedStudentItem(
    val studentId: String = "",
    val studentName: String = "",
    val previousClass: String = "",
    val newClass: String = "",
    val sectionName: String = "",
    val finalAveragePercentage: Int = 0,
    val isPromoted: Boolean = true,
    val isGraduatedStage: Boolean = false,
    val isPendingGrades: Boolean = false,
    val completedSubjectsCount: Int = 0,
    val requiredSubjectsCount: Int = 0
) {
    val fromGradeClass: String get() = previousClass
    val toGradeClass: String get() = newClass
    val previousGrade: String get() = previousClass
    val newGrade: String get() = newClass
    val finalAverage: Int get() = finalAveragePercentage
    val averageScore: Double get() = finalAveragePercentage.toDouble()
    val passed: Boolean get() = isPromoted && !isPendingGrades
    val graduated: Boolean get() = isGraduatedStage && !isPendingGrades
}

data class ClassPromotionBreakdown(
    val gradeClass: String = "",
    val nextGradeClass: String = "",
    val totalStudents: Int = 0,
    val passedCount: Int = 0,
    val failedCount: Int = 0,
    val pendingGradesCount: Int = 0,
    val passRatePercentage: Int = 0
) {
    val totalCount: Int get() = totalStudents
    val repeatingCount: Int get() = failedCount
    val passRatePercent: Int get() = passRatePercentage
}

data class AnnualPromotionReport(
    val academicYear: String = "",
    val academicYearFrom: String = academicYear,
    val academicYearTo: String = academicYear,
    val executedAt: String = "",
    val totalEvaluatedStudents: Int = 0,
    val totalPromotedCount: Int = 0,
    val totalRepeatingCount: Int = 0,
    val totalPendingGradesCount: Int = 0,
    val graduatedCount: Int = 0,
    val overallPassRatePercentage: Int = 0,
    val classBreakdowns: List<ClassPromotionBreakdown> = emptyList(),
    val studentResults: List<PromotedStudentItem> = emptyList(),
    val promotedStudents: List<PromotedStudentItem> = studentResults.filter { it.isPromoted && !it.isPendingGrades },
    val repeatingStudents: List<PromotedStudentItem> = studentResults.filter { !it.isPromoted && !it.isPendingGrades },
    val pendingGradesStudents: List<PromotedStudentItem> = studentResults.filter { it.isPendingGrades }
) {
    val previousAcademicYear: String get() = academicYearFrom.ifBlank { academicYear }
    val newAcademicYear: String get() = academicYearTo.ifBlank { academicYear }
    val overallPassRatePercent: Int get() = overallPassRatePercentage
    val promotedCount: Int get() = totalPromotedCount
    val repeatingCount: Int get() = totalRepeatingCount
    val pendingGradesCount: Int get() = totalPendingGradesCount
    val totalEvaluated: Int get() = totalEvaluatedStudents
    val totalStudentsEvaluated: Int get() = totalEvaluatedStudents
}

data class ClassComprehensiveStatisticalReport(
    val gradeClass: String = "",
    val sectionName: String = "",
    val periodLabel: String = "",
    val totalStudents: Int = 0,
    val activeStudentsCount: Int = 0,
    val suspendedStudentsCount: Int = 0,
    val expelledStudentsCount: Int = 0,
    val transferredStudentsCount: Int = 0,
    val presentDaysTotal: Int = 0,
    val absentDaysTotal: Int = 0,
    val excusedDaysTotal: Int = 0,
    val lateDaysTotal: Int = 0,
    val attendanceRatePercent: Int = 100,
    val averageGradePercent: Int = 0,
    val passRatePercent: Int = 0,
    val passedStudentsCount: Int = 0,
    val repeatingStudentsCount: Int = 0,
    val pendingGradesCount: Int = 0
)

data class DeveloperVaultConfig(
    val usernameHash: String = "",
    val passwordHash: String = "",
    val encryptedVaultPayload: String = "",
    val encryptedUsernameCipher: String = "",
    val encryptedPasswordCipher: String = "",
    val maskedDisplayLabel: String = "مطور النظام العام (مشفر سحابياً)",
    val updatedAt: String = ""
)

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
    val address: String = "",
    val schoolStage: String = "ثانوية",
    val schoolType: String = "مدرسة أهلية",
    val ministryAffiliation: String = "وزارة التربية",
    val schoolGenderType: String = "مدرسة بنين",
    val createdAt: String = "2026-10-29",
    val isActive: Boolean = true,
    val adminUsername: String = "",
    val subscriptionExpiryDate: String = "2027-10-29"
) {
    fun daysUntilExpiry(): Long = DateCountdownHelper.daysUntilDate(subscriptionExpiryDate) ?: 365L
    fun isWithinRenewalWarningWindow(): Boolean = daysUntilExpiry() <= 14L
}

data class UrgentSchoolAlert(
    val id: String = "urgent_alert",
    val schoolId: String = "",
    val message: String = "",
    val updatedAt: String = "",
    val authorName: String = "إدارة المدرسة",
    val isActive: Boolean = false,
    val signatureKey: String = ""
)

data class OfficialHolidayItem(
    val id: String,
    val dateIso: String,
    val title: String = "عطلة رسمية",
    val createdAt: String = "",
    val schoolId: String = ""
)

data class SchoolHolidaysConfig(
    val weeklyDaysOff: List<String> = listOf("الجمعة", "السبت"),
    val officialHolidays: List<OfficialHolidayItem> = emptyList(),
    val updatedAt: Long = 0L,
    val schoolId: String = ""
)

data class SchoolConfig(
    val schoolId: String = "",
    val isSetupCompleted: Boolean = false,
    val isActive: Boolean = true,
    val createdAt: String = "2026-10-29",
    val schoolName: String = "مدرستي النموذجية",
    val address: String = "",
    val schoolType: SchoolType = SchoolType.PRIVATE,
    val schoolStage: SchoolStage = SchoolStage.SECONDARY,
    val ministryAffiliation: String = "وزارة التربية",
    val schoolGenderType: String = "مدرسة بنين",
    val registeredClasses: List<String> = SchoolStage.SECONDARY.defaultClasses,
    val registeredSections: List<String> = listOf("أ", "ب", "ج"),
    val registeredSubjects: List<String> = SchoolStage.SECONDARY.defaultSubjects,
    val subjectConfigs: List<SubjectConfigItem> = emptyList(),
    val logoBase64: String = "",
    val academicYear: String = "2026 - 2027",
    val phone: String = "",
    val subscriptionExpiryDate: String = "2027-10-29",
    val featureSettings: SchoolFeatureSettings = SchoolFeatureSettings(),
    val urgentAlert: UrgentSchoolAlert = UrgentSchoolAlert(),
    val holidaysConfig: SchoolHolidaysConfig = SchoolHolidaysConfig(),
    val isCanteenWalletEnabled: Boolean = true,
    val principalFullName: String = "مدير المدرسة"
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
    val address: String = "",
    val photoBase64: String = "",
    val schoolId: String = "",
    val canModerateActivities: Boolean = false,
    val canManageCanteen: Boolean = false
) {
    val teacherSubjectsList: List<String>
        get() = specialty.split("،", ",", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    val teacherClassSectionPairs: List<String>
        get() = assignedClasses.split("،", ",", "|")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    /**
     * Parses each assigned item into a clean (pureClass, optionalPureSection) pair.
     */
    val parsedClassAndSectionEntries: List<Pair<String, String>>
        get() {
            val rawTokens = teacherClassSectionPairs
            val explicitPairs = mutableListOf<Pair<String, String>>()
            val standaloneClasses = mutableListOf<String>()
            val standaloneSections = mutableListOf<String>()

            rawTokens.forEach { token ->
                val trimmed = token.trim()
                if (trimmed.isEmpty()) return@forEach
                val cleanedSecCheck = trimmed
                    .replace("شعبة", "")
                    .replace("الشعبة", "")
                    .replace("(", "")
                    .replace(")", "")
                    .trim()
                val isStandaloneSec = !trimmed.contains("-") &&
                    (trimmed.startsWith("شعبة") || trimmed.startsWith("الشعبة") || cleanedSecCheck.length == 1)
                if (isStandaloneSec) {
                    val pureSec = ArabicTextNormalizer.cleanPureSectionName(cleanedSecCheck)
                    if (pureSec.isNotEmpty() && !ArabicTextNormalizer.isBogusSectionName(pureSec)) {
                        standaloneSections.add(pureSec)
                    }
                } else {
                    val (pureClass, extractedSec) = ArabicTextNormalizer.normalizeAndExtractClass(trimmed, fallbackSection = "")
                    if (pureClass.isNotEmpty() && !ArabicTextNormalizer.isBogusClassName(pureClass)) {
                        if (extractedSec.isNotEmpty() && !ArabicTextNormalizer.isBogusSectionName(extractedSec)) {
                            explicitPairs.add(pureClass to extractedSec)
                        } else {
                            standaloneClasses.add(pureClass)
                        }
                    }
                }
            }

            if (standaloneClasses.isNotEmpty()) {
                if (standaloneSections.isNotEmpty()) {
                    standaloneClasses.forEach { cls ->
                        standaloneSections.forEach { sec ->
                            explicitPairs.add(cls to sec)
                        }
                    }
                } else {
                    standaloneClasses.forEach { cls ->
                        explicitPairs.add(cls to "")
                    }
                }
            }
            return explicitPairs.distinctBy { (c, s) ->
                "${ArabicTextNormalizer.normalizeClass(c)}__${ArabicTextNormalizer.normalizeSection(s)}"
            }
        }

    val teacherAssignedClassesOnly: List<String>
        get() = parsedClassAndSectionEntries
            .map { it.first }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusClassName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeClass(it) }

    val teacherAssignedSectionsOnly: List<String>
        get() = parsedClassAndSectionEntries
            .map { it.second }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusSectionName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeSection(it) }

    /**
     * Clean, human-readable summary of the teacher's assigned classes & sections for the green welcome card
     * (e.g., "الثاني الابتدائي (أ، ب)" or "الأول المتوسط (أ) • الثاني المتوسط (ب)").
     */
    val formattedAssignedClassesForDisplay: String
        get() {
            val entries = parsedClassAndSectionEntries
            if (entries.isEmpty()) {
                return assignedClasses.trim().ifBlank { "جميع الصفوف المسندة" }
            }
            val grouped = linkedMapOf<String, MutableList<String>>()
            entries.forEach { (cls, sec) ->
                val existingKey = grouped.keys.firstOrNull { ArabicTextNormalizer.matchesClass(it, cls) } ?: cls
                val secList = grouped.getOrPut(existingKey) { mutableListOf() }
                if (sec.isNotBlank() && secList.none { ArabicTextNormalizer.matchesSection(it, sec) }) {
                    secList.add(sec)
                }
            }
            return grouped.entries.joinToString(" • ") { (cls, secs) ->
                if (secs.isEmpty()) cls else "$cls (${secs.joinToString("، ")})"
            }
        }

    fun canTeacherAccessClassSection(gradeClass: String, sectionName: String? = null): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role != UserRole.TEACHER) return false
        val pairs = teacherClassSectionPairs
        if (pairs.isEmpty()) return false
        if (pairs.any { ArabicTextNormalizer.isAllClassesToken(it) }) return true

        val (pureTargetClass, extractedFromTargetClass) = ArabicTextNormalizer.normalizeAndExtractClass(
            rawInput = gradeClass,
            fallbackSection = sectionName.orEmpty()
        )
        val normTargetClass = ArabicTextNormalizer.normalizeClass(pureTargetClass)
        val effectiveTargetSec = sectionName?.takeIf { it.isNotBlank() } ?: extractedFromTargetClass
        val normTargetSec = ArabicTextNormalizer.normalizeSection(effectiveTargetSec)

        val entries = parsedClassAndSectionEntries
        if (entries.isEmpty()) return false

        return entries.any { (assignedCls, assignedSec) ->
            val clsMatch = ArabicTextNormalizer.isAllClassesToken(assignedCls) ||
                ArabicTextNormalizer.normalizeClass(assignedCls) == normTargetClass
            if (!clsMatch) return@any false
            if (sectionName == null || normTargetSec.isEmpty() || assignedSec.isBlank()) return@any true
            val normAssignedSec = ArabicTextNormalizer.normalizeSection(assignedSec)
            normAssignedSec.isEmpty() || normAssignedSec == normTargetSec || ArabicTextNormalizer.isAllClassesToken(assignedSec)
        }
    }

    fun canTeacherAccessSubject(subjectName: String): Boolean {
        if (role == UserRole.ADMIN) return true
        if (role != UserRole.TEACHER) return false
        val subs = teacherSubjectsList
        if (subs.isEmpty()) return true
        if (subs.any { ArabicTextNormalizer.isAllClassesToken(it) }) return true
        val normSub = ArabicTextNormalizer.normalizeText(subjectName)
        return subs.any {
            val normAssigned = ArabicTextNormalizer.normalizeText(it)
            normAssigned == normSub || normSub.contains(normAssigned) || normAssigned.contains(normSub)
        }
    }
}

object ArabicTextNormalizer {
    private val validSectionLetters = setOf("أ", "ا", "ب", "ج", "د", "هـ", "ه", "و", "a", "b", "c", "d")

    private val officialBaseClasses = listOf(
        "الأول الابتدائي",
        "الثاني الابتدائي",
        "الثالث الابتدائي",
        "الرابع الابتدائي",
        "الخامس الابتدائي",
        "السادس الابتدائي",
        "الأول المتوسط",
        "الثاني المتوسط",
        "الثالث المتوسط",
        "الرابع الإعدادي",
        "الخامس الإعدادي",
        "السادس الإعدادي",
        "الرابع العلمي",
        "الرابع الأدبي",
        "الخامس العلمي",
        "الخامس الأدبي",
        "السادس العلمي",
        "السادس الأدبي"
    )

    fun normalizeText(input: String): String {
        return input.trim()
            .replace(Regex("[أإآٱ]"), "ا")
            .replace("ة", "ه")
            .replace("ى", "ي")
            .replace("ؤ", "و")
            .replace("ئ", "ي")
            .replace("ـ", "")
            .replace(Regex("\\s+"), " ")
            .lowercase()
    }

    /**
     * Separates any attached section letter or "شعبة ..." from a class string and returns:
     * Pair(pureClassName, pureSectionName)
     * Example:
     *   "الثاني الابتدائي أ - شعبة أ" -> Pair("الثاني الابتدائي", "أ")
     *   "الصف الثاني الابتدائي (ب)" -> Pair("الثاني الابتدائي", "ب")
     *   "الثاني الابتدائي أ" -> Pair("الثاني الابتدائي", "أ")
     */
    fun normalizeAndExtractClass(rawInput: String, fallbackSection: String = "أ"): Pair<String, String> {
        var working = rawInput.trim()
            .replace("الصف", "")
            .replace(Regex("\\s+"), " ")
            .trim()

        var extractedSection = ""

        // 1. Check if there is a dash separator: "الثاني الابتدائي - شعبة أ" or "الثاني الابتدائي أ - أ"
        if (working.contains("-")) {
            val beforeDash = working.substringBefore("-").trim()
            val afterDash = working.substringAfter("-").trim()
            val secCandidate = cleanPureSectionName(afterDash)
            if (secCandidate.isNotEmpty()) {
                extractedSection = secCandidate
            }
            working = beforeDash
        }

        // 2. Check if there is a parenthesized section: "الثاني الابتدائي (أ)" or "(شعبة ب)"
        val parenRegex = Regex("\\(([^)]+)\\)")
        val parenMatch = parenRegex.find(working)
        if (parenMatch != null) {
            val inside = parenMatch.groupValues[1].trim()
            val secCandidate = cleanPureSectionName(inside)
            if (secCandidate.isNotEmpty() && extractedSection.isEmpty()) {
                extractedSection = secCandidate
            }
            working = working.replace(parenMatch.value, "").trim()
        }

        // 3. Check if "شعبة X" is attached at the end of working
        val shubaRegex = Regex("(?:شعبة|الشعبة|شعبه|الشعبه)\\s*([أابجدهوA-Da-d])\\s*$")
        val shubaMatch = shubaRegex.find(working)
        if (shubaMatch != null) {
            val secCandidate = cleanPureSectionName(shubaMatch.groupValues[1])
            if (secCandidate.isNotEmpty() && extractedSection.isEmpty()) {
                extractedSection = secCandidate
            }
            working = working.removeRange(shubaMatch.range).trim()
        }

        // 4. Check if a trailing single section letter (أ، ب، ج، د) is attached after the stage word
        val tokens = working.split(" ").map { it.trim() }.filter { it.isNotEmpty() }
        if (tokens.size >= 3) {
            val lastToken = tokens.last()
            val normLast = normalizeText(lastToken)
            if (lastToken.length == 1 && (lastToken in validSectionLetters || normLast in validSectionLetters)) {
                if (extractedSection.isEmpty()) {
                    extractedSection = cleanPureSectionName(lastToken)
                }
                working = tokens.dropLast(1).joinToString(" ").trim()
            }
        }

        // 5. Canonicalize against official Iraqi school class names if it matches
        val normWorking = normalizeText(working)
        val canonicalClass = officialBaseClasses.firstOrNull { normalizeText(it) == normWorking }
            ?: working.trim()

        val finalSection = extractedSection.ifBlank { cleanPureSectionName(fallbackSection) }
        return canonicalClass to finalSection
    }

    fun cleanPureClassName(rawClass: String): String {
        return normalizeAndExtractClass(rawClass, fallbackSection = "").first
    }

    fun cleanPureSectionName(rawSection: String): String {
        val cleaned = rawSection.trim()
            .replace("شعبة", "")
            .replace("الشعبة", "")
            .replace("شعبه", "")
            .replace("الشعبه", "")
            .replace("(", "")
            .replace(")", "")
            .replace("-", "")
            .trim()
        if (cleaned.isEmpty()) return ""
        val firstToken = cleaned.split(" ").firstOrNull()?.trim().orEmpty()
        return when (normalizeText(firstToken)) {
            "ا" -> "أ"
            "ب" -> "ب"
            "ج" -> "ج"
            "د" -> "د"
            "ه" -> "هـ"
            else -> firstToken
        }
    }

    fun isBogusClassName(className: String): Boolean {
        val norm = normalizeClass(className)
        if (norm.isEmpty()) return true
        // Filter out non-Iraqi or bogus legacy test strings like "الصف العاشر", "الحادي عشر", "الثاني عشر", "شعبة ط"
        return norm.contains("عاشر") ||
            norm.contains("حادي عشر") ||
            norm.contains("ثاني عشر") ||
            norm == "ط" ||
            norm.startsWith("شعبه")
    }

    fun isBogusSectionName(sectionName: String): Boolean {
        val pure = cleanPureSectionName(sectionName)
        if (pure.isEmpty()) return true
        val norm = normalizeText(pure)
        // Filter out bogus section letters like "ط" or long non-section strings
        if (norm == "ط" || pure.length > 3) return true
        return false
    }

    fun normalizeClass(gradeClass: String): String {
        val pure = cleanPureClassName(gradeClass)
        return normalizeText(pure)
    }

    fun normalizeSection(sectionName: String): String {
        val pure = cleanPureSectionName(sectionName)
        return normalizeText(pure)
    }

    fun matchesClass(c1: String, c2: String): Boolean {
        if (isAllClassesToken(c1) || isAllClassesToken(c2)) return true
        val n1 = normalizeClass(c1)
        val n2 = normalizeClass(c2)
        return n1.isNotEmpty() && n1 == n2
    }

    fun matchesSection(s1: String, s2: String): Boolean {
        if (isAllClassesToken(s1) || isAllClassesToken(s2)) return true
        val n1 = normalizeSection(s1)
        val n2 = normalizeSection(s2)
        return n1.isNotEmpty() && n1 == n2
    }

    fun isAllClassesToken(token: String): Boolean {
        val norm = normalizeText(token)
        return norm == "الكل" ||
            norm == "جميع الصفوف" ||
            norm == "كل الصفوف" ||
            norm == "جميع الشعب" ||
            norm == "كل الشعب" ||
            norm == "عام" ||
            norm == "all"
    }

    fun normalizeDigitsToAscii(raw: String): String {
        val arabicIndic = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        val easternArabic = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder(raw.length)
        for (ch in raw) {
            val idx1 = arabicIndic.indexOf(ch)
            val idx2 = easternArabic.indexOf(ch)
            when {
                idx1 >= 0 -> sb.append(('0'.code + idx1).toChar())
                idx2 >= 0 -> sb.append(('0'.code + idx2).toChar())
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Normalizes any date string into canonical ISO format YYYY-MM-DD (e.g. "20261101" -> "2026-11-01").
     */
    fun normalizeDateIso(raw: String): String {
        val ascii = normalizeDigitsToAscii(raw.trim())
        if (ascii.isEmpty()) return ""
        if (ascii.length == 8 && ascii.all { it.isDigit() }) {
            return "${ascii.substring(0, 4)}-${ascii.substring(4, 6)}-${ascii.substring(6, 8)}"
        }
        val sepMatch = Regex("^(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})$").find(ascii)
        if (sepMatch != null) {
            val y = sepMatch.groupValues[1]
            val m = sepMatch.groupValues[2].padStart(2, '0')
            val d = sepMatch.groupValues[3].padStart(2, '0')
            return "$y-$m-$d"
        }
        return ascii
    }

    /**
     * Strips legacy default city/district placeholders so school and student addresses stay clean.
     */
    fun cleanDefaultAddress(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return ""
        val legacyCity = "\u0627\u0644\u0645\u0648\u0635\u0644"
        val legacyDistrict = "\u0632\u0645\u0627\u0631"
        val cleaned = trimmed
            .replace("($legacyCity - $legacyDistrict)", "")
            .replace("($legacyCity $legacyDistrict)", "")
            .replace("$legacyCity - $legacyDistrict", "")
            .replace("$legacyCity $legacyDistrict", "")
            .trim()
        if (cleaned == legacyCity || cleaned == legacyDistrict || cleaned == "-" || cleaned == "—") return ""
        return cleaned
    }
}

enum class CanteenTransactionType(val arabicLabel: String) {
    TOP_UP("شحن رصيد"),
    PURCHASE_APPROVED("شراء من الحانوت"),
    PURCHASE_REJECTED("محاولة شراء مرفوضة")
}

data class CanteenWalletTransaction(
    val id: String,
    val studentId: String,
    val studentName: String,
    val type: CanteenTransactionType,
    val amount: Int,
    val balanceAfter: Int,
    val date: String,
    val timestamp: String,
    val actorName: String = "كاشير الحانوت",
    val note: String = ""
) {
    val amountIqd: Int get() = amount
    val itemsSummary: String get() = note
}

data class EnrollmentHistoryEntry(
    val id: String = "",
    val status: StudentEnrollmentStatus = StudentEnrollmentStatus.ACTIVE,
    val decisionDate: String = "",
    val suspensionStartDate: String = "",
    val suspensionEndDate: String = "",
    val reason: String = "",
    val orderNumber: String = "",
    val transferSchoolName: String = "",
    val actorName: String = "المدير"
)

data class StudentRecord(
    val id: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val parentName: String,
    val parentPhone: String,
    val address: String = "",
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
    val schoolId: String = "",
    val dailyAttendance: Map<String, String> = emptyMap(),
    val todayStatus: String = "",
    val todayStatusDate: String = "",
    val absenceDays: Int = 0,
    val excusedDays: Int = 0,
    val lateDays: Int = 0,
    val walletBalance: Int = 0,
    val dailySpendLimit: Int = 0,
    val dailySpentMap: Map<String, Int> = emptyMap(),
    val walletTransactions: List<CanteenWalletTransaction> = emptyList(),
    val dailyGateLog: Map<String, DailyGateLogEntry> = emptyMap(),
    val isWalletSuspended: Boolean = false,
    val enrollmentStatus: StudentEnrollmentStatus = StudentEnrollmentStatus.ACTIVE,
    val suspensionStartDate: String = "",
    val suspensionEndDate: String = "",
    val enrollmentStatusReason: String = "",
    val transferredToSchoolName: String = "",
    val enrollmentDecisionDate: String = "",
    val enrollmentOrderNumber: String = "",
    val enrollmentHistory: List<EnrollmentHistoryEntry> = emptyList(),
    val archivedYearlySummaries: List<ArchivedYearlySummary> = emptyList()
) {
    val enrollmentStatusEnum: StudentEnrollmentStatus get() = enrollmentStatus
    val suspensionFromDate: String get() = suspensionStartDate
    val suspensionToDate: String get() = suspensionEndDate
    val statusReason: String get() = enrollmentStatusReason
    val transferSchoolName: String get() = transferredToSchoolName
    val dailyGateLogs: Map<String, DailyGateLogEntry> get() = dailyGateLog
    val walletBalanceIqd: Int get() = walletBalance
    val overallAverage: String get() = "$excellencePoints%"

    val qrCodeValue: String
        get() = "MADRASATI|${schoolId.ifBlank { "100" }}|$id|$studentName|$gradeClass"

    val walletBarcode: String
        get() = "WALLET-${schoolId.ifBlank { "100" }}-$id"

    fun getSpentToday(todayIso: String): Int {
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(todayIso.trim())
        if (cleanDate.isEmpty()) return 0
        return dailySpentMap[cleanDate]
            ?: dailySpentMap.entries.firstOrNull { (k, _) ->
                ArabicTextNormalizer.normalizeDateIso(k.trim()) == cleanDate
            }?.value
            ?: 0
    }

    fun getGateLogForDate(dateIso: String): DailyGateLogEntry? {
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(dateIso.trim())
        if (cleanDate.isEmpty()) return null
        return dailyGateLog[cleanDate]
            ?: dailyGateLog.entries.firstOrNull { (k, _) ->
                ArabicTextNormalizer.normalizeDateIso(k.trim()) == cleanDate
            }?.value
    }

    /**
     * Returns true when the student has completed both Check-In and Check-Out on `dateIso`.
     * Once true, attendance and gate buttons for that date are locked until the next day.
     */
    fun isDayAttendanceLocked(dateIso: String): Boolean {
        val gateLog = getGateLogForDate(dateIso) ?: return false
        return gateLog.checkInTime.isNotBlank() && gateLog.checkOutTime.isNotBlank() && gateLog.isDeparted
    }

    /**
     * Checks if the student is under an active temporary suspension (`SUSPENDED_TEMP`) on `dateIso`.
     */
    fun isTemporarilySuspendedOnDate(dateIso: String): Boolean {
        if (enrollmentStatus != StudentEnrollmentStatus.SUSPENDED_TEMP) return false
        val targetDate = ArabicTextNormalizer.normalizeDateIso(dateIso.trim())
        if (targetDate.isEmpty()) return true
        val startIso = ArabicTextNormalizer.normalizeDateIso(suspensionStartDate.trim())
        val endIso = ArabicTextNormalizer.normalizeDateIso(suspensionEndDate.trim())
        if (startIso.isNotEmpty() && targetDate < startIso) return false
        if (endIso.isNotEmpty() && targetDate > endIso) return false
        return true
    }

    /**
     * Checks if attendance recording is blocked for this student on `dateIso` due to official enrollment status
     * (Temporary Suspension within date range, Expelled, Deregistered, or Transferred).
     */
    fun isAttendanceBlockedByEnrollmentStatus(dateIso: String): Boolean {
        return when (enrollmentStatus) {
            StudentEnrollmentStatus.ACTIVE -> false
            StudentEnrollmentStatus.SUSPENDED_TEMP -> isTemporarilySuspendedOnDate(dateIso)
            StudentEnrollmentStatus.EXPELLED,
            StudentEnrollmentStatus.DEREGISTERED,
            StudentEnrollmentStatus.TRANSFERRED -> true
        }
    }

    fun getEnrollmentBlockBannerText(dateIso: String): String {
        if (!isAttendanceBlockedByEnrollmentStatus(dateIso)) return ""
        val reasonSuffix = enrollmentStatusReason.takeIf { it.isNotBlank() }?.let { " — السبب: $it" }.orEmpty()
        return when (enrollmentStatus) {
            StudentEnrollmentStatus.ACTIVE -> ""
            StudentEnrollmentStatus.SUSPENDED_TEMP -> {
                val rangeText = if (suspensionStartDate.isNotBlank() || suspensionEndDate.isNotBlank()) {
                    " (من ${suspensionStartDate.ifBlank { "اليوم" }} إلى ${suspensionEndDate.ifBlank { "إشعار آخر" }})"
                } else ""
                "الطالب في حالة فصل مؤقت$rangeText — رصد الحضور موقوف حتى انتهاء المدة$reasonSuffix"
            }
            StudentEnrollmentStatus.EXPELLED ->
                "قيد الطالب: فصل دائمي — رصد الحضور موقوف رسمياً$reasonSuffix"
            StudentEnrollmentStatus.DEREGISTERED ->
                "قيد الطالب: ترقين قيد — رصد الحضور موقوف رسمياً$reasonSuffix"
            StudentEnrollmentStatus.TRANSFERRED -> {
                val schoolTarget = transferredToSchoolName.takeIf { it.isNotBlank() }?.let { " إلى ($it)" }.orEmpty()
                "قيد الطالب: منقول$schoolTarget — رصد الحضور موقوف رسمياً$reasonSuffix"
            }
        }
    }

    fun getDetailedEnrollmentSummaryText(): String {
        return when (enrollmentStatus) {
            StudentEnrollmentStatus.ACTIVE ->
                "مستمر بالدوام الرسمي — مسموح برصد الحضور والغياب والامتحانات بشكل اعتيادي."
            StudentEnrollmentStatus.SUSPENDED_TEMP -> buildString {
                append("فصل مؤقت من تاريخ (${suspensionStartDate.ifBlank { "غير محدد" }}) إلى تاريخ (${suspensionEndDate.ifBlank { "غير محدد" }})")
                if (enrollmentStatusReason.isNotBlank()) append(" • السبب: $enrollmentStatusReason")
                if (enrollmentOrderNumber.isNotBlank()) append(" • رقم الأمر: $enrollmentOrderNumber")
            }
            StudentEnrollmentStatus.EXPELLED -> buildString {
                append("فصل دائمي من المدرسة")
                if (enrollmentDecisionDate.isNotBlank()) append(" • تاريخ القرار: $enrollmentDecisionDate")
                if (enrollmentOrderNumber.isNotBlank()) append(" • رقم الأمر الإداري: $enrollmentOrderNumber")
                if (enrollmentStatusReason.isNotBlank()) append(" • السبب: $enrollmentStatusReason")
            }
            StudentEnrollmentStatus.DEREGISTERED -> buildString {
                append("مرقّن القيد رسمياً")
                if (enrollmentDecisionDate.isNotBlank()) append(" • تاريخ ترقين القيد: $enrollmentDecisionDate")
                if (enrollmentOrderNumber.isNotBlank()) append(" • رقم الأمر الإداري: $enrollmentOrderNumber")
                if (enrollmentStatusReason.isNotBlank()) append(" • السبب: $enrollmentStatusReason")
            }
            StudentEnrollmentStatus.TRANSFERRED -> buildString {
                append("منقول إلى مدرسة: (${transferredToSchoolName.ifBlank { "مدرسة أخرى" }})")
                if (enrollmentDecisionDate.isNotBlank()) append(" • تاريخ النقل: $enrollmentDecisionDate")
                if (enrollmentOrderNumber.isNotBlank()) append(" • رقم كتاب النقل: $enrollmentOrderNumber")
                if (enrollmentStatusReason.isNotBlank()) append(" • الملاحظات: $enrollmentStatusReason")
            }
        }
    }

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

enum class GateScanMode(val arabicTitle: String, val shortLabel: String) {
    CHECK_IN("تسجيل دخول البوابة (حضور)", "تسجيل دخول (حضور)"),
    CHECK_OUT("تسجيل مغادرة البوابة (انصراف)", "تسجيل مغادرة (انصراف)")
}

data class DailyGateLogEntry(
    val checkInTime: String = "",
    val checkOutTime: String = "",
    val gateStatus: String = "inside", // "inside" | "departed"
    val departureNote: String = "انصراف رسمي / مغادرة البوابة"
) {
    val isDeparted: Boolean
        get() = gateStatus.equals("departed", ignoreCase = true) || checkOutTime.isNotBlank()
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
    val schoolId: String = "",
    val attachmentImageBase64: String = ""
)

data class ExamScheduleItem(
    val id: String,
    val gradeClass: String,
    val subjectName: String,
    val examType: String,
    val examDate: String,
    val dayName: String,
    val timeSlot: String = "",
    val topicsIncluded: String = "",
    val hallNumber: String = "",
    val schoolId: String = "",
    val sectionName: String = "",
    val maxScore: Int = GradingRules.defaultMaxScoreForClass(gradeClass)
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
    val extraDailyScores: List<Int> = emptyList(),
    val extraMonthlyScores: List<Int> = emptyList(),
    val isApprovedByPrincipal: Boolean = false,
    val schoolId: String = ""
) {
    // هل اكتملت جميع الامتحانات (شهر 1، شهر 2، والأشهر الإضافية، نصف السنة، النهائي)؟
    val isAllExamsCompleted: Boolean
        get() = month1Score > 0 &&
            month2Score > 0 &&
            extraMonthlyScores.all { it > 0 } &&
            midYearScore > 0 &&
            finalExamScore > 0

    // متوسط درجات اليومي (يومي 1 + أي درجات يومية إضافية)
    val effectiveDailyAverage: Double
        get() {
            val allDaily = listOf(dailyScore) + extraDailyScores
            return allDaily.average()
        }

    // السعي السنوي (يحسب بدقة متناهية مع أي حقول يومية أو شهرية إضافية)
    val annualEffortScore: Int
        get() {
            val components = mutableListOf(effectiveDailyAverage, month1Score.toDouble(), month2Score.toDouble())
            extraMonthlyScores.forEach { components.add(it.toDouble()) }
            val courseworkAvg = components.average()
            return ((courseworkAvg * 0.5) + (midYearScore * 0.5))
                .roundToInt()
                .coerceIn(0, maxScoreScale)
        }

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

    val evaluationText: String get() = ratingLabel
    val month1: Int get() = month1Score
    val month2: Int get() = month2Score
    val midYear: Int get() = midYearScore
    val month3: Int get() = extraMonthlyScores.getOrNull(0) ?: month1Score
    val month4: Int get() = extraMonthlyScores.getOrNull(1) ?: month2Score
    val finalExam: Int get() = finalExamScore
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
    val schoolId: String = "",
    val isReadByParent: Boolean = false,
    val readAtTimestamp: String = ""
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
    val location: String = "",
    val imageBase64: String = "",
    val authorName: String = "إدارة المدرسة",
    val heartCount: Int = 0,
    val likedByUserIds: List<String> = emptyList(),
    val schoolId: String = "",
    val schoolName: String = ""
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
    val targetSection: String = "",   // Deep-link section (e.g. AppSection.name)
    val targetTab: Int = 0,           // Deep-link sub-tab index
    val targetItemId: String = "",    // Deep-link specific item/post/contact/homework ID
    val createdAt: String,
    val isRead: Boolean = false,
    val schoolId: String = "",
    val isUrgent: Boolean = false,
    val priority: String = "normal"
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
) {
    val requestedAt: String get() = createdAt
}

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
    val senderName: String = "إدارة المنصة التقنية",
    val createdAt: String,
    val targetSchoolId: String = "ALL",
    val iconKey: String = OccasionIconKey.BELL.name,
    val imageBase64: String = "",
    val imageUrl: String = ""
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
            val ascii = ArabicTextNormalizer.normalizeDigitsToAscii(dateStr)
                .trim()
                .replace("/", "-")
                .replace(".", "-")
            // Extract YYYY-M-D or D-M-YYYY token
            val regexYmd = Regex("(\\d{4})-(\\d{1,2})-(\\d{1,2})")
            val regexDmy = Regex("(\\d{1,2})-(\\d{1,2})-(\\d{4})")
            val ymdMatch = regexYmd.find(ascii)
            val dmyMatch = if (ymdMatch == null) regexDmy.find(ascii) else null
            val year: Int
            val month: Int
            val day: Int
            when {
                ymdMatch != null -> {
                    year = ymdMatch.groupValues[1].toInt()
                    month = ymdMatch.groupValues[2].toInt()
                    day = ymdMatch.groupValues[3].toInt()
                }
                dmyMatch != null -> {
                    day = dmyMatch.groupValues[1].toInt()
                    month = dmyMatch.groupValues[2].toInt()
                    year = dmyMatch.groupValues[3].toInt()
                }
                else -> return null
            }
            if (month !in 1..12 || day !in 1..31 || year < 2000) return null
            val todayCal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val targetCal = java.util.Calendar.getInstance().apply {
                clear()
                set(year, month - 1, day, 0, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val diffMillis = targetCal.timeInMillis - todayCal.timeInMillis
            kotlin.math.round(diffMillis / 86_400_000.0).toLong()
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
    val lostAndFoundEnabled: Boolean = true,
    val studentActivitiesEnabled: Boolean = true,
    val canteenWalletEnabled: Boolean = true,
    val teacherEvaluationEnabled: Boolean = true,
    val showTopTeacherOnHome: Boolean = true,
    val autoAbsenceEnabled: Boolean = true,
    val autoAbsenceDelayMinutes: Int = 120
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
            dismissalAndLostFoundEnabled = dismissalCallEnabled || lostAndSug,
            canteenWalletEnabled = canteenWalletEnabled,
            teacherEvaluationEnabled = teacherEvaluationEnabled,
            showTopTeacherOnHome = showTopTeacherOnHome,
            autoAbsenceEnabled = autoAbsenceEnabled,
            autoAbsenceDelayMinutes = autoAbsenceDelayMinutes.coerceIn(15, 480)
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
    val earnedPoints: Int,
    val answeredAt: String = ""
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
    val answerTimestamps: Map<String, String> = emptyMap(), // studentId -> timestamp
    val points: Int = 5,
    val isClosed: Boolean = false,
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

enum class OccasionIconKey(
    val arabicLabel: String,
    val launcherAliasName: String
) {
    BELL("مكبر صوت إعلاني", ".MainActivityDefault"),
    GRADUATION_CAP("تهنئة وعام دراسي", ".MainActivityNewYear"),
    CRESCENT("مناسبة دينية / هلال", ".MainActivityRamadan"),
    TROPHY("تفوق وتكريم", ".MainActivityHonor"),
    CALENDAR("عطلة رسمية / تقويم", ".MainActivityNewYear"),
    STAR("تحديث وميزة جديدة", ".MainActivityDefault"),
    SHIELD("تنبيه أمني / هام", ".MainActivityNational"),
    CROWN("تاج التميز", ".MainActivityHonor"),
    FLAG("علم وطني", ".MainActivityNational"),
    BOOK("كتاب أكاديمي", ".MainActivityDefault"),
    ROSE("إشراقة تربوية", ".MainActivityDefault");

    companion object {
        val primary3DBroadcastChoices: List<OccasionIconKey> = listOf(
            BELL,
            GRADUATION_CAP,
            CRESCENT,
            TROPHY,
            CALENDAR,
            STAR,
            SHIELD
        )

        fun fromKey(key: String): OccasionIconKey {
            return entries.firstOrNull { it.name.equals(key.trim(), ignoreCase = true) } ?: BELL
        }
    }
}

data class OccasionThemeConfig(
    val isActive: Boolean = false,
    val themeType: OccasionThemeType = OccasionThemeType.CUSTOM_MANUAL,
    val customTitle: String = "",
    val customBannerText: String = "",
    val selectedIconKey: String = OccasionIconKey.BELL.name,
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
        get() = ""

    val resolvedIconKey: OccasionIconKey
        get() = OccasionIconKey.fromKey(selectedIconKey)

    val effectiveTitle: String
        get() = customTitle.ifBlank { "مناسبة خاصة" }

    val effectiveBannerText: String
        get() = customBannerText.ifBlank { "كل عام وأنتم بخير من أسرة منصة مدرستي الذكية" }

    val effectivePrimaryHex: String
        get() = primaryColorHex.ifBlank { "#064E3B" }

    val effectiveGradientHex: String
        get() = gradientColorHex.ifBlank { "#059669" }

    val effectiveAliasName: String
        get() = if (!isActive) {
            ".MainActivityDefault"
        } else {
            launcherAliasName.ifBlank { resolvedIconKey.launcherAliasName }
        }
}

/**
 * 8.b Biometric Fingerprint Saved Credentials (الدخول البايومتري السريع ببصمة الإصبع)
 */
data class BiometricSavedCredentials(
    val username: String = "",
    val password: String = "",
    val fullName: String = "",
    val roleArabic: String = "",
    val schoolId: String = "",
    val schoolName: String = "",
    val isDeveloper: Boolean = false,
    val enabledAt: String = ""
) {
    val isValid: Boolean
        get() = username.isNotBlank() && password.isNotBlank()
}

/**
 * 9. Student Creative Community & Activities ("مجتمع ونشاطات الطلبة") + Full Admin Moderation & Sanctions
 */
enum class StudentActivityStatus(val arabicLabel: String) {
    PENDING("قيد المراجعة"),
    APPROVED("معتمد ومنشور"),
    REJECTED("مرفوض")
}

data class StudentActivityReply(
    val id: String,
    val commentId: String,
    val postId: String,
    val authorUserId: String,
    val authorName: String,
    val authorRole: UserRole,
    val authorGradeClass: String = "",
    val authorPhotoBase64: String = "",
    val text: String,
    val createdAt: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val syncStatus: String = "synced"
)

data class StudentActivityComment(
    val id: String,
    val postId: String,
    val authorUserId: String,
    val authorName: String,
    val authorRole: UserRole,
    val authorGradeClass: String = "",
    val authorPhotoBase64: String = "",
    val text: String,
    val createdAt: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val replies: List<StudentActivityReply> = emptyList(),
    val syncStatus: String = "synced"
)

data class StudentActivityPost(
    val id: String,
    val schoolId: String = "",
    val schoolName: String = "",
    val authorUserId: String,
    val authorStudentId: String = "",
    val authorName: String,
    val authorRole: UserRole,
    val authorGradeClass: String = "",
    val authorSectionName: String = "",
    val authorPhotoBase64: String = "",
    val category: String = "موهبة وإبداع علمي",
    val title: String,
    val content: String,
    val imageBase64: String = "",
    val status: StudentActivityStatus = StudentActivityStatus.PENDING,
    val reviewedByName: String = "",
    val createdAt: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val heartCount: Int = 0,
    val likedByUserIds: List<String> = emptyList(),
    val comments: List<StudentActivityComment> = emptyList()
) {
    val totalCommentsAndRepliesCount: Int
        get() = comments.size + comments.sumOf { it.replies.size }

    fun engagementScore(isAuthorFollowed: Boolean): Double {
        val likesWeight = heartCount * 3.0
        val commentsWeight = totalCommentsAndRepliesCount * 4.5
        val followBoost = if (isAuthorFollowed) 12.0 else 0.0
        val ageHours = ((System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L) / 3_600_000.0)
        val recencyBonus = (48.0 - ageHours).coerceAtLeast(0.0) * 0.35
        return likesWeight + commentsWeight + followBoost + recencyBonus
    }
}

enum class ViolationTargetType(val arabicLabel: String) {
    POST("منشور نشاط"),
    COMMENT("تعليق"),
    REPLY("رد على تعليق")
}

data class StudentActivityViolationReport(
    val id: String,
    val schoolId: String = "",
    val postId: String,
    val commentId: String = "",
    val replyId: String = "",
    val targetType: ViolationTargetType = ViolationTargetType.POST,
    val reportedUserId: String,
    val reportedStudentId: String = "",
    val reportedUserName: String,
    val reportedUserRole: UserRole,
    val reportedContentExcerpt: String,
    val reporterUserId: String,
    val reporterName: String,
    val reason: String,
    val createdAt: String,
    val isResolved: Boolean = false,
    val resolutionActionNote: String = ""
)

enum class SanctionDurationOption(val arabicLabel: String, val durationMillis: Long, val isPermanent: Boolean = false) {
    HOURS_24("24 ساعة", 24L * 3600L * 1000L, false),
    DAYS_3("3 أيام", 3L * 24L * 3600L * 1000L, false),
    DAYS_7("7 أيام", 7L * 24L * 3600L * 1000L, false),
    PERMANENT("حظر دائم", Long.MAX_VALUE / 2, true)
}

data class UserActivityProfileAndSanctions(
    val userId: String,
    val schoolId: String = "",
    val bio: String = "",
    val followingUserIds: List<String> = emptyList(),
    val mutePostingUntilMillis: Long = 0L,
    val isPermanentPostingBan: Boolean = false,
    val mutePostingReason: String = "",
    val muteInteractionUntilMillis: Long = 0L,
    val isPermanentInteractionBan: Boolean = false,
    val muteInteractionReason: String = "",
    val warningCount: Int = 0,
    val lastWarningText: String = "",
    val updatedAt: String = ""
) {
    fun isPostingMuted(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return isPermanentPostingBan || mutePostingUntilMillis > nowMillis
    }

    fun isInteractionMuted(nowMillis: Long = System.currentTimeMillis()): Boolean {
        return isPermanentInteractionBan || muteInteractionUntilMillis > nowMillis
    }

    fun remainingPostingBanLabel(nowMillis: Long = System.currentTimeMillis()): String {
        if (isPermanentPostingBan) return "حظر دائم من النشر"
        val diff = mutePostingUntilMillis - nowMillis
        if (diff <= 0L) return ""
        val hours = (diff / (3600L * 1000L)).coerceAtLeast(1L)
        return if (hours >= 24L) {
            val days = (hours / 24L).coerceAtLeast(1L)
            "متبقي $days يوم ($hours ساعة)"
        } else {
            "متبقي $hours ساعة"
        }
    }

    fun remainingInteractionBanLabel(nowMillis: Long = System.currentTimeMillis()): String {
        if (isPermanentInteractionBan) return "حظر دائم من التفاعل"
        val diff = muteInteractionUntilMillis - nowMillis
        if (diff <= 0L) return ""
        val hours = (diff / (3600L * 1000L)).coerceAtLeast(1L)
        return if (hours >= 24L) {
            val days = (hours / 24L).coerceAtLeast(1L)
            "متبقي $days يوم ($hours ساعة)"
        } else {
            "متبقي $hours ساعة"
        }
    }
}

/**
 * 10. Deep-Link Notification Navigation Target (التوجيه الذكي الفوري عند النقر على الإشعارات)
 */
data class DeepLinkNavigationTarget(
    val section: AppSection,
    val tabIndex: Int = 0,
    val itemId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 11. Teacher Evaluation & Excellence System (نظام تقييم وتميّز الكادر التدريسي)
 */
data class TeacherRatingRecord(
    val id: String,
    val schoolId: String,
    val teacherId: String,
    val teacherName: String,
    val parentId: String,
    val parentName: String,
    val stars: Int, // 1..5
    val praiseTag: String = "",
    val note: String = "",
    val createdAt: String = ""
)

data class TeacherEvaluationSummary(
    val teacher: UserAccount,
    val averageRating: Float,
    val totalRatingsCount: Int,
    val starsCountMap: Map<Int, Int>,
    val topPraiseTags: List<String>,
    val myParentRating: TeacherRatingRecord?,
    val recentParentNotes: List<Pair<String, String>> = emptyList()
)

/**
 * 12. Smart Student Report Cards & Interactive Celebration Hub (مركز إصدار وإرسال الكارتات المدرسية الذكية والتهنئة التفاعلية)
 */
enum class ReportCardPeriodType(val arabicTitle: String, val shortLabel: String) {
    MONTH_1("كارت الشهر الأول", "الشهر الأول"),
    MONTH_2("كارت الشهر الثاني", "الشهر الثاني"),
    MID_YEAR("كارت نصف السنة", "نصف السنة"),
    ANNUAL_EFFORT("كارت السعي السنوي", "السعي السنوي"),
    FINAL_EXAM("كارت الامتحان النهائي", "الامتحان النهائي"),
    COMPREHENSIVE_ALL("الكارت الشامل لجميع الفصول", "الكارت الشامل")
}

data class IssuedReportCardSubjectItem(
    val subjectName: String,
    val score: Int,
    val maxScoreScale: Int = 100,
    val isPassed: Boolean = true,
    val dailyScore: Int = 0,
    val month1Score: Int = 0,
    val month2Score: Int = 0,
    val midYearScore: Int = 0,
    val annualEffortScore: Int = 0,
    val finalExamScore: Int = 0,
    val finalAverage: Int = 0
)

data class IssuedStudentReportCard(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val periodType: ReportCardPeriodType = ReportCardPeriodType.COMPREHENSIVE_ALL,
    val subjects: List<IssuedReportCardSubjectItem> = emptyList(),
    val totalScore: Int = 0,
    val averagePercent: Int = 0,
    val isOverallPassed: Boolean = true,
    val failedSubjectsCount: Int = 0,
    val issuedAt: String = "",
    val principalName: String = "إدارة المدرسة",
    val schoolName: String = "",
    val isViewedByParent: Boolean = false,
    val schoolId: String = ""
)

