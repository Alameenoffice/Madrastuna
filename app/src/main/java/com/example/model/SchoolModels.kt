package com.example.model

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

enum class UserRole(val arabicTitle: String) {
    ADMIN("المدير العام"),
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

data class SchoolConfig(
    val isSetupCompleted: Boolean = false,
    val schoolName: String = "مدرستي النموذجية",
    val address: String = "الموصل - زمار",
    val schoolType: SchoolType = SchoolType.PRIVATE,
    val schoolStage: SchoolStage = SchoolStage.SECONDARY,
    val registeredClasses: List<String> = SchoolStage.SECONDARY.defaultClasses,
    val registeredSections: List<String> = listOf("أ", "ب", "ج"),
    val registeredSubjects: List<String> = SchoolStage.SECONDARY.defaultSubjects,
    val logoBase64: String = "",
    val academicYear: String = "2026 - 2027",
    val phone: String = "07512362567"
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
    val photoBase64: String = ""
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
    val honorTitle: String = "وسام التفوق الذهبي"
)

data class AttendanceEntry(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val sectionName: String,
    val date: String,
    val status: AttendanceStatus,
    val note: String = ""
)

data class SchedulePeriod(
    val id: String,
    val gradeClass: String,
    val sectionName: String,
    val dayOfWeek: String,
    val periodNumber: Int,
    val subjectName: String,
    val teacherName: String,
    val timeSlot: String
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
    val createdAt: String
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
    val hallNumber: String = "قاعة 1"
)

data class SubjectGrade(
    val id: String,
    val studentId: String,
    val studentName: String,
    val gradeClass: String,
    val subjectName: String,
    val dailyScore: Int,       // يومي (out of 100)
    val month1Score: Int,      // الشهر الأول (out of 100)
    val month2Score: Int,      // الشهر الثاني (out of 100)
    val midYearScore: Int,     // نصف السنة (out of 100)
    val finalExamScore: Int    // الامتحان النهائي (out of 100)
) {
    // السعي السنوي
    val annualEffortScore: Int
        get() = ((dailyScore + month1Score + month2Score) / 3.0 * 0.5 + midYearScore * 0.5).toInt()

    // المعدل النهائي
    val finalAverage: Int
        get() = ((annualEffortScore + finalExamScore) / 2.0).toInt()

    // التقدير التلقائي
    val ratingLabel: String
        get() = when {
            finalAverage >= 90 -> "ممتاز"
            finalAverage >= 80 -> "جيد جداً"
            finalAverage >= 70 -> "جيد"
            finalAverage >= 60 -> "متوسط"
            finalAverage >= 50 -> "مقبول"
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
    val installments: List<TuitionInstallment> = emptyList()
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
    val createdAt: String
)

data class ChatMessage(
    val id: String,
    val studentId: String,
    val studentName: String,
    val senderName: String,
    val senderRole: UserRole,
    val messageText: String,
    val timestamp: String
)

data class SchoolEvent(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    val date: String,
    val location: String = "الموصل - زمار",
    val imageBase64: String = "",
    val authorName: String = "إدارة المدرسة"
)

data class AppNotification(
    val id: String,
    val title: String,
    val body: String,
    val type: NotificationType,
    val targetStudentId: String = "", // empty means broadcast to all
    val createdAt: String,
    val isRead: Boolean = false
)
