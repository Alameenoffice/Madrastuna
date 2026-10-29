package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.data.SchoolRepository
import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = SchoolRepository(application.applicationContext)

    private val _isDarkMode = MutableStateFlow(repo.isDarkMode())
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _onboardingSeen = MutableStateFlow(repo.isOnboardingSeen())
    val onboardingSeen: StateFlow<Boolean> = _onboardingSeen.asStateFlow()

    private val _schoolConfig = MutableStateFlow(repo.loadSchoolConfig())
    val schoolConfig: StateFlow<SchoolConfig> = _schoolConfig.asStateFlow()

    private val _accounts = MutableStateFlow(repo.loadAccounts())
    val accounts: StateFlow<List<UserAccount>> = _accounts.asStateFlow()

    private val _students = MutableStateFlow(repo.loadStudents())
    val students: StateFlow<List<StudentRecord>> = _students.asStateFlow()

    private val _attendance = MutableStateFlow(repo.loadAttendance())
    val attendance: StateFlow<List<AttendanceEntry>> = _attendance.asStateFlow()

    private val _schedules = MutableStateFlow(repo.loadSchedules())
    val schedules: StateFlow<List<SchedulePeriod>> = _schedules.asStateFlow()

    private val _homeworks = MutableStateFlow(repo.loadHomeworks())
    val homeworks: StateFlow<List<HomeworkItem>> = _homeworks.asStateFlow()

    private val _examSchedules = MutableStateFlow(repo.loadExamSchedules())
    val examSchedules: StateFlow<List<ExamScheduleItem>> = _examSchedules.asStateFlow()

    private val _grades = MutableStateFlow(repo.loadGrades())
    val grades: StateFlow<List<SubjectGrade>> = _grades.asStateFlow()

    private val _tuitions = MutableStateFlow(repo.loadTuitions())
    val tuitions: StateFlow<List<TuitionRecord>> = _tuitions.asStateFlow()

    private val _reports = MutableStateFlow(repo.loadReports())
    val reports: StateFlow<List<ParentReport>> = _reports.asStateFlow()

    private val _chatMessages = MutableStateFlow(repo.loadChatMessages())
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _events = MutableStateFlow(repo.loadEvents())
    val events: StateFlow<List<SchoolEvent>> = _events.asStateFlow()

    private val _notifications = MutableStateFlow(repo.loadNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    private val _activeSection = MutableStateFlow(AppSection.DASHBOARD)
    val activeSection: StateFlow<AppSection> = _activeSection.asStateFlow()

    private val _latestBannerAlert = MutableStateFlow<AppNotification?>(null)
    val latestBannerAlert: StateFlow<AppNotification?> = _latestBannerAlert.asStateFlow()

    val availableClasses = listOf(
        "الأول المتوسط",
        "الثاني المتوسط",
        "الثالث المتوسط",
        "الرابع الإعدادي",
        "الخامس الإعدادي",
        "السادس الإعدادي"
    )
    val availableSections = listOf("أ", "ب", "ج")
    val weekDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")

    fun toggleDarkMode() {
        val next = !_isDarkMode.value
        _isDarkMode.value = next
        repo.setDarkMode(next)
    }

    fun completeOnboarding() {
        _onboardingSeen.value = true
        repo.setOnboardingSeen(true)
    }

    fun reopenOnboarding() {
        _onboardingSeen.value = false
        repo.setOnboardingSeen(false)
    }

    fun navigateToSection(section: AppSection) {
        if (section == AppSection.TUITION_FEES && _schoolConfig.value.schoolType == SchoolType.PUBLIC) {
            _activeSection.value = AppSection.DASHBOARD
            return
        }
        _activeSection.value = section
    }

    fun dismissBannerAlert() {
        _latestBannerAlert.value = null
    }

    /**
     * First-time platform initialization by the General Director.
     */
    fun setupSchoolAndAdmin(
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String
    ) {
        val finalAddress = address.trim().ifBlank { "الموصل - زمار" }
        val config = SchoolConfig(
            isSetupCompleted = true,
            schoolName = schoolName.trim().ifBlank { "مدرستي النموذجية" },
            address = finalAddress,
            schoolType = schoolType,
            logoBase64 = logoBase64,
            academicYear = "2026 - 2027",
            phone = "07512362567"
        )
        repo.saveSchoolConfig(config)
        _schoolConfig.value = config

        val adminAccount = UserAccount(
            id = "admin_main",
            username = adminUsername.trim(),
            password = adminPassword.trim(),
            fullName = adminFullName.trim().ifBlank { "المدير العام" },
            role = UserRole.ADMIN,
            isActive = true,
            phone = "07512362567",
            specialty = "الإدارة العامة",
            assignedClasses = "جميع الصفوف والشعب",
            address = finalAddress,
            photoBase64 = logoBase64
        )

        // Build synced accounts list from default teachers + default students/parents
        val seededAccounts = mutableListOf<UserAccount>()
        seededAccounts.add(adminAccount)
        seededAccounts.addAll(repo.defaultTeachers())

        val currentStudents = _students.value.ifEmpty { repo.defaultStudents() }
        repo.saveStudents(currentStudents)
        _students.value = currentStudents

        currentStudents.forEach { stu ->
            seededAccounts.add(
                UserAccount(
                    id = "acc_parent_${stu.id}",
                    username = stu.parentUsername,
                    password = stu.parentPassword,
                    fullName = stu.parentName,
                    role = UserRole.PARENT,
                    isActive = stu.isActive,
                    phone = stu.parentPhone,
                    linkedStudentId = stu.id,
                    gradeClass = stu.gradeClass,
                    sectionName = stu.sectionName,
                    address = stu.address
                )
            )
            seededAccounts.add(
                UserAccount(
                    id = "acc_stu_${stu.id}",
                    username = stu.studentUsername,
                    password = stu.studentPassword,
                    fullName = stu.studentName,
                    role = UserRole.STUDENT,
                    isActive = stu.isActive,
                    phone = stu.parentPhone,
                    linkedStudentId = stu.id,
                    gradeClass = stu.gradeClass,
                    sectionName = stu.sectionName,
                    address = stu.address,
                    photoBase64 = stu.photoBase64
                )
            )
        }

        repo.saveAccounts(seededAccounts)
        _accounts.value = seededAccounts

        // Persist default records so everything is saved cleanly
        repo.saveAttendance(_attendance.value)
        repo.saveSchedules(_schedules.value)
        repo.saveHomeworks(_homeworks.value)
        repo.saveExamSchedules(_examSchedules.value)
        repo.saveGrades(_grades.value)
        repo.saveTuitions(_tuitions.value)
        repo.saveReports(_reports.value)
        repo.saveChatMessages(_chatMessages.value)
        repo.saveEvents(_events.value)
        repo.saveNotifications(_notifications.value)

        // Log in as the newly created General Director
        _currentUser.value = adminAccount
        _activeSection.value = AppSection.DASHBOARD
    }

    fun updateSchoolInfo(
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String
    ) {
        val updated = _schoolConfig.value.copy(
            schoolName = schoolName.trim().ifBlank { _schoolConfig.value.schoolName },
            address = address.trim().ifBlank { "الموصل - زمار" },
            schoolType = schoolType,
            logoBase64 = logoBase64.ifBlank { _schoolConfig.value.logoBase64 }
        )
        repo.saveSchoolConfig(updated)
        _schoolConfig.value = updated
        if (schoolType == SchoolType.PUBLIC && _activeSection.value == AppSection.TUITION_FEES) {
            _activeSection.value = AppSection.DASHBOARD
        }
    }

    fun login(usernameInput: String, passwordInput: String): Result<UserAccount> {
        val cleanUser = usernameInput.trim()
        val cleanPass = passwordInput.trim()
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور"))
        }
        val matched = _accounts.value.firstOrNull {
            it.username.equals(cleanUser, ignoreCase = true) && it.password == cleanPass
        } ?: return Result.failure(IllegalArgumentException("بيانات الدخول غير صحيحة، تأكد من اسم المستخدم وكلمة المرور"))

        if (!matched.isActive) {
            return Result.failure(IllegalStateException("هذا الحساب موقوف حالياً من قبل إدارة المدرسة"))
        }

        _currentUser.value = matched
        _activeSection.value = AppSection.DASHBOARD
        return Result.success(matched)
    }

    fun logout() {
        _currentUser.value = null
        _activeSection.value = AppSection.DASHBOARD
    }

    // =========================================================================
    // ADMIN ACCOUNT MANAGEMENT (Teachers, Parents & Students)
    // =========================================================================

    fun addTeacherAccount(
        fullName: String,
        specialty: String,
        assignedClasses: String,
        phone: String,
        username: String,
        password: String
    ): UserAccount {
        val cleanUsername = username.trim().ifBlank {
            "tch_${(100..999).random()}"
        }
        val cleanPassword = password.trim().ifBlank {
            (1000..9999).random().toString()
        }
        val newTeacher = UserAccount(
            id = "tch_${UUID.randomUUID().toString().take(6)}",
            username = cleanUsername,
            password = cleanPassword,
            fullName = fullName.trim(),
            role = UserRole.TEACHER,
            isActive = true,
            phone = phone.trim().ifBlank { "07512362567" },
            specialty = specialty.trim(),
            assignedClasses = assignedClasses.trim(),
            address = "الموصل - زمار"
        )
        val updated = listOf(newTeacher) + _accounts.value
        _accounts.value = updated
        repo.saveAccounts(updated)
        return newTeacher
    }

    fun toggleAccountActive(accountId: String) {
        val updated = _accounts.value.map { acc ->
            if (acc.id == accountId && acc.role != UserRole.ADMIN) {
                acc.copy(isActive = !acc.isActive)
            } else acc
        }
        _accounts.value = updated
        repo.saveAccounts(updated)
    }

    fun addStudentAndParentAccounts(
        studentName: String,
        gradeClass: String,
        sectionName: String,
        parentName: String,
        parentPhone: String,
        address: String,
        totalFee: Long,
        photoBase64: String,
        studentUsername: String,
        studentPassword: String,
        parentUsername: String,
        parentPassword: String
    ): StudentRecord {
        val stuId = "stu_${UUID.randomUUID().toString().take(6)}"
        val finalAddress = address.trim().ifBlank { "الموصل - زمار" }
        val finalPhone = parentPhone.trim().ifBlank { "07512362567" }
        val sUser = studentUsername.trim().ifBlank { "stu_${(100..999).random()}" }
        val sPass = studentPassword.trim().ifBlank { (1000..9999).random().toString() }
        val pUser = parentUsername.trim().ifBlank { "par_${(100..999).random()}" }
        val pPass = parentPassword.trim().ifBlank { (1000..9999).random().toString() }

        val newStudent = StudentRecord(
            id = stuId,
            studentName = studentName.trim(),
            gradeClass = gradeClass,
            sectionName = sectionName,
            parentName = parentName.trim(),
            parentPhone = finalPhone,
            address = finalAddress,
            studentUsername = sUser,
            studentPassword = sPass,
            parentUsername = pUser,
            parentPassword = pPass,
            isActive = true,
            photoBase64 = photoBase64,
            excellencePoints = 92,
            isOnHonorRoll = false,
            honorTitle = "طالب مجتهد"
        )

        val updatedStudents = listOf(newStudent) + _students.value
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents)

        val parentAcc = UserAccount(
            id = "acc_parent_$stuId",
            username = pUser,
            password = pPass,
            fullName = parentName.trim(),
            role = UserRole.PARENT,
            isActive = true,
            phone = finalPhone,
            linkedStudentId = stuId,
            gradeClass = gradeClass,
            sectionName = sectionName,
            address = finalAddress
        )
        val studentAcc = UserAccount(
            id = "acc_stu_$stuId",
            username = sUser,
            password = sPass,
            fullName = studentName.trim(),
            role = UserRole.STUDENT,
            isActive = true,
            phone = finalPhone,
            linkedStudentId = stuId,
            gradeClass = gradeClass,
            sectionName = sectionName,
            address = finalAddress,
            photoBase64 = photoBase64
        )

        val updatedAccounts = listOf(parentAcc, studentAcc) + _accounts.value
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts)

        val newTuition = TuitionRecord(
            studentId = stuId,
            studentName = newStudent.studentName,
            gradeClass = gradeClass,
            sectionName = sectionName,
            parentName = newStudent.parentName,
            parentPhone = finalPhone,
            totalFee = totalFee.coerceAtLeast(500000L),
            nextDueDate = "2026-11-15",
            installments = emptyList()
        )
        val updatedTuitions = listOf(newTuition) + _tuitions.value
        _tuitions.value = updatedTuitions
        repo.saveTuitions(updatedTuitions)

        return newStudent
    }

    fun toggleStudentAndParentActive(studentId: String) {
        val target = _students.value.firstOrNull { it.id == studentId } ?: return
        val nextState = !target.isActive

        val updatedStudents = _students.value.map {
            if (it.id == studentId) it.copy(isActive = nextState) else it
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents)

        val updatedAccounts = _accounts.value.map {
            if (it.linkedStudentId == studentId) it.copy(isActive = nextState) else it
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts)
    }

    // =========================================================================
    // SECTION 1: NOTIFICATIONS
    // =========================================================================

    fun pushNotification(
        title: String,
        body: String,
        type: NotificationType,
        targetStudentId: String = ""
    ) {
        val timeLabel = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val notif = AppNotification(
            id = "notif_${UUID.randomUUID().toString().take(6)}",
            title = title,
            body = body,
            type = type,
            targetStudentId = targetStudentId,
            createdAt = "الآن ($timeLabel)",
            isRead = false
        )
        val updated = listOf(notif) + _notifications.value
        _notifications.value = updated
        repo.saveNotifications(updated)
        _latestBannerAlert.value = notif
    }

    fun markAllNotificationsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        repo.saveNotifications(updated)
    }

    // =========================================================================
    // SECTION 2: DAILY ATTENDANCE
    // =========================================================================

    fun recordStudentAttendance(
        student: StudentRecord,
        date: String,
        status: AttendanceStatus,
        note: String = ""
    ) {
        val existingIndex = _attendance.value.indexOfFirst {
            it.studentId == student.id && it.date == date
        }
        val entry = AttendanceEntry(
            id = if (existingIndex >= 0) _attendance.value[existingIndex].id else "att_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            date = date,
            status = status,
            note = note.ifBlank { status.arabicLabel }
        )

        val updated = _attendance.value.toMutableList()
        if (existingIndex >= 0) {
            updated[existingIndex] = entry
        } else {
            updated.add(0, entry)
        }
        _attendance.value = updated
        repo.saveAttendance(updated)

        if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LATE) {
            pushNotification(
                title = "تنبيه ${status.arabicLabel}: ${student.studentName}",
                body = "تم رصد حالة (${status.arabicLabel}) للطالب ${student.studentName} في الصف ${student.gradeClass} (${student.sectionName}) بتاريخ $date.",
                type = NotificationType.ATTENDANCE,
                targetStudentId = student.id
            )
        }
    }

    fun getTotalAbsenceDays(studentId: String): Int {
        return _attendance.value.count {
            it.studentId == studentId && it.status == AttendanceStatus.ABSENT
        }
    }

    // =========================================================================
    // SECTION 3: WEEKLY SCHEDULE & DAILY HOMEWORK
    // =========================================================================

    fun addSchedulePeriod(
        gradeClass: String,
        sectionName: String,
        dayOfWeek: String,
        periodNumber: Int,
        subjectName: String,
        teacherName: String,
        timeSlot: String
    ) {
        val period = SchedulePeriod(
            id = "sch_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            dayOfWeek = dayOfWeek,
            periodNumber = periodNumber,
            subjectName = subjectName.trim(),
            teacherName = teacherName.trim(),
            timeSlot = timeSlot.trim()
        )
        val updated = _schedules.value.filterNot {
            it.gradeClass == gradeClass &&
                it.sectionName == sectionName &&
                it.dayOfWeek == dayOfWeek &&
                it.periodNumber == periodNumber
        } + period
        _schedules.value = updated.sortedBy { it.periodNumber }
        repo.saveSchedules(_schedules.value)
    }

    fun publishHomework(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        teacherName: String,
        dailyPreparation: String,
        homeworkDetails: String,
        dueDate: String
    ) {
        val hw = HomeworkItem(
            id = "hw_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            subjectName = subjectName.trim(),
            teacherName = teacherName.trim(),
            dailyPreparation = dailyPreparation.trim(),
            homeworkDetails = homeworkDetails.trim(),
            dueDate = dueDate.trim(),
            createdAt = "اليوم"
        )
        val updated = listOf(hw) + _homeworks.value
        _homeworks.value = updated
        repo.saveHomeworks(updated)

        pushNotification(
            title = "تحضير وواجب جديد: $subjectName",
            body = "للصف $gradeClass ($sectionName): $dailyPreparation — الواجب: $homeworkDetails",
            type = NotificationType.HOMEWORK
        )
    }

    // =========================================================================
    // SECTION 4: EXAMS & DETAILED GRADES
    // =========================================================================

    fun addExamSchedule(
        gradeClass: String,
        subjectName: String,
        examType: String,
        examDate: String,
        dayName: String,
        timeSlot: String,
        topicsIncluded: String,
        hallNumber: String
    ) {
        val item = ExamScheduleItem(
            id = "ex_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            subjectName = subjectName.trim(),
            examType = examType.trim(),
            examDate = examDate.trim(),
            dayName = dayName.trim(),
            timeSlot = timeSlot.trim(),
            topicsIncluded = topicsIncluded.trim(),
            hallNumber = hallNumber.trim()
        )
        val updated = listOf(item) + _examSchedules.value
        _examSchedules.value = updated
        repo.saveExamSchedules(updated)

        pushNotification(
            title = "موعد امتحان جديد: $subjectName",
            body = "تم تحديد موعد $examType للصف $gradeClass يوم $dayName الموافق $examDate.",
            type = NotificationType.GRADES
        )
    }

    fun saveOrUpdateSubjectGrade(
        student: StudentRecord,
        subjectName: String,
        dailyScore: Int,
        month1Score: Int,
        month2Score: Int,
        midYearScore: Int,
        finalExamScore: Int
    ) {
        val existingIdx = _grades.value.indexOfFirst {
            it.studentId == student.id && it.subjectName.equals(subjectName.trim(), ignoreCase = true)
        }
        val grade = SubjectGrade(
            id = if (existingIdx >= 0) _grades.value[existingIdx].id else "grd_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            subjectName = subjectName.trim(),
            dailyScore = dailyScore.coerceIn(0, 100),
            month1Score = month1Score.coerceIn(0, 100),
            month2Score = month2Score.coerceIn(0, 100),
            midYearScore = midYearScore.coerceIn(0, 100),
            finalExamScore = finalExamScore.coerceIn(0, 100)
        )

        val updated = _grades.value.toMutableList()
        if (existingIdx >= 0) {
            updated[existingIdx] = grade
        } else {
            updated.add(0, grade)
        }
        _grades.value = updated
        repo.saveGrades(updated)

        pushNotification(
            title = "رصد درجات مادة $subjectName",
            body = "تم تحديث درجات الطالب ${student.studentName} في مادة $subjectName (السعي السنوي: ${grade.annualEffortScore} | المعدل النهائي: ${grade.finalAverage} - ${grade.ratingLabel}).",
            type = NotificationType.GRADES,
            targetStudentId = student.id
        )
    }

    // =========================================================================
    // SECTION 5: HONOR ROLL
    // =========================================================================

    fun updateStudentHonorRoll(
        studentId: String,
        isOnHonorRoll: Boolean,
        excellencePoints: Int,
        honorTitle: String
    ) {
        val updated = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    isOnHonorRoll = isOnHonorRoll,
                    excellencePoints = excellencePoints.coerceIn(50, 100),
                    honorTitle = honorTitle.trim().ifBlank { "وسام التفوق الذهبي" }
                )
            } else s
        }
        _students.value = updated
        repo.saveStudents(updated)
    }

    // =========================================================================
    // SECTION 6: TUITION FEES (PRIVATE SCHOOL)
    // =========================================================================

    fun addTuitionPayment(
        studentId: String,
        amount: Long,
        paymentDate: String,
        note: String,
        nextDueDate: String
    ): TuitionInstallment? {
        if (amount <= 0L) return null
        val receiptNo = "REC-${(1000..9999).random()}"
        val installment = TuitionInstallment(
            id = "inst_${UUID.randomUUID().toString().take(6)}",
            amount = amount,
            paymentDate = paymentDate.ifBlank { "2026-10-29" },
            receiptNumber = receiptNo,
            note = note.ifBlank { "دفعة قسط دراسي" }
        )

        var targetStudentName = ""
        var remainingAfter = 0L

        val updated = _tuitions.value.map { rec ->
            if (rec.studentId == studentId) {
                val newList = listOf(installment) + rec.installments
                val copyRec = rec.copy(
                    installments = newList,
                    nextDueDate = nextDueDate.ifBlank { rec.nextDueDate }
                )
                targetStudentName = copyRec.studentName
                remainingAfter = copyRec.remainingAmount
                copyRec
            } else rec
        }
        _tuitions.value = updated
        repo.saveTuitions(updated)

        if (targetStudentName.isNotEmpty()) {
            pushNotification(
                title = "وصل استلام قسط دراسي ($receiptNo)",
                body = "تم استلام مبلغ $amount د.ع للطالب $targetStudentName. المبلغ المتبقي: $remainingAfter د.ع.",
                type = NotificationType.TUITION,
                targetStudentId = studentId
            )
        }
        return installment
    }

    // =========================================================================
    // SECTION 7: PARENT REPORTS & DIRECT MESSAGING
    // =========================================================================

    fun sendParentReport(
        student: StudentRecord,
        reportType: ReportType,
        title: String,
        details: String,
        senderName: String
    ) {
        val rep = ParentReport(
            id = "rep_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            parentName = student.parentName,
            parentPhone = student.parentPhone,
            reportType = reportType,
            title = title.trim(),
            details = details.trim(),
            senderName = senderName,
            createdAt = "اليوم"
        )
        val updated = listOf(rep) + _reports.value
        _reports.value = updated
        repo.saveReports(updated)

        pushNotification(
            title = "${reportType.arabicLabel}: ${student.studentName}",
            body = "${title.trim()} — ${details.trim()}",
            type = NotificationType.REPORT,
            targetStudentId = student.id
        )
    }

    fun sendChatMessage(
        student: StudentRecord,
        messageText: String
    ) {
        val user = _currentUser.value ?: return
        if (messageText.isBlank()) return
        val timeLabel = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val msg = ChatMessage(
            id = "msg_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            senderName = "${user.fullName} (${user.role.arabicTitle})",
            senderRole = user.role,
            messageText = messageText.trim(),
            timestamp = timeLabel
        )
        val updated = _chatMessages.value + msg
        _chatMessages.value = updated
        repo.saveChatMessages(updated)
    }

    // =========================================================================
    // SECTION 8: SCHOOL EVENTS & NEWS GALLERY
    // =========================================================================

    fun publishSchoolEvent(
        title: String,
        category: String,
        description: String,
        date: String,
        location: String,
        imageBase64: String
    ) {
        val author = _currentUser.value?.fullName ?: "إدارة المدرسة"
        val ev = SchoolEvent(
            id = "ev_${UUID.randomUUID().toString().take(6)}",
            title = title.trim(),
            category = category.trim().ifBlank { "فعالية مدرسية" },
            description = description.trim(),
            date = date.trim().ifBlank { "2026-10-29" },
            location = location.trim().ifBlank { "الموصل - زمار" },
            imageBase64 = imageBase64,
            authorName = author
        )
        val updated = listOf(ev) + _events.value
        _events.value = updated
        repo.saveEvents(updated)

        pushNotification(
            title = "خبر جديد: ${ev.title}",
            body = ev.description,
            type = NotificationType.GENERAL
        )
    }
}
