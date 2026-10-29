package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.FirebaseCloudSync
import com.example.data.SchoolRepository
import com.example.model.*
import com.example.util.InputSanitizer
import com.example.util.SystemNotificationHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SchoolViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repo = SchoolRepository(appContext)

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

    private val _activeSession = MutableStateFlow<UserSession?>(repo.loadSession())
    val activeSession: StateFlow<UserSession?> = _activeSession.asStateFlow()

    private val _activeSection = MutableStateFlow(AppSection.DASHBOARD)
    val activeSection: StateFlow<AppSection> = _activeSection.asStateFlow()

    private val _latestBannerAlert = MutableStateFlow<AppNotification?>(null)
    val latestBannerAlert: StateFlow<AppNotification?> = _latestBannerAlert.asStateFlow()

    private val _toastAlert = MutableStateFlow<ToastAlertMessage?>(null)
    val toastAlert: StateFlow<ToastAlertMessage?> = _toastAlert.asStateFlow()

    val availableClasses: List<String>
        get() = _schoolConfig.value.registeredClasses.ifEmpty { SchoolStage.SECONDARY.defaultClasses }

    val availableSections: List<String>
        get() = _schoolConfig.value.registeredSections.ifEmpty { listOf("أ", "ب", "ج") }

    val availableSubjects: List<String>
        get() = _schoolConfig.value.registeredSubjects.ifEmpty { SchoolStage.SECONDARY.defaultSubjects }

    val weekDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")

    init {
        SystemNotificationHelper.ensureChannelCreated(appContext)
        restoreSavedSessionIfValid()
        startRealtimeCloudMonitor()
    }

    // =========================================================================
    // SESSION VALIDATION & MULTI-TENANT SCHOOL ISOLATION
    // =========================================================================

    private fun restoreSavedSessionIfValid() {
        val savedSession = repo.loadSession() ?: return
        val currentSchoolId = _schoolConfig.value.schoolId
        if (currentSchoolId.isNotEmpty() && savedSession.schoolId == currentSchoolId) {
            val matchedUser = _accounts.value.firstOrNull {
                it.id == savedSession.userId && it.isActive && (it.schoolId.isEmpty() || it.schoolId == currentSchoolId)
            }
            if (matchedUser != null) {
                _currentUser.value = matchedUser
            } else {
                repo.clearSession()
                _activeSession.value = null
            }
        }
    }

    fun validateSessionAndSchoolAccess(
        allowedRoles: Set<UserRole> = UserRole.entries.toSet()
    ): Boolean {
        val user = _currentUser.value
        val schoolId = _schoolConfig.value.schoolId
        if (user == null || !user.isActive) {
            showToastAlert("انتهت صلاحية الجلسة أو الحساب غير مفعل، يرجى تسجيل الدخول", isSuccess = false)
            return false
        }
        if (schoolId.isNotEmpty() && user.schoolId.isNotEmpty() && user.schoolId != schoolId) {
            showToastAlert("خطأ أمني: الحساب لا ينتمي إلى معرّف هذه المدرسة ($schoolId)", isSuccess = false)
            return false
        }
        if (user.role !in allowedRoles) {
            showToastAlert("عذراً، لا تملك الصلاحية لتنفيذ هذه العملية", isSuccess = false)
            return false
        }
        return true
    }

    fun showToastAlert(message: String, isSuccess: Boolean = true) {
        val alert = ToastAlertMessage(
            id = UUID.randomUUID().toString(),
            message = message,
            isSuccess = isSuccess
        )
        _toastAlert.value = alert
    }

    fun dismissToastAlert() {
        _toastAlert.value = null
    }

    /**
     * Real-time monitor for Firebase collections:
     * notifications, messages, schedules_homework, and reports under schools/{schoolId}/...
     * Triggers external phone push notifications with sound and vibration when new items arrive.
     */
    private fun startRealtimeCloudMonitor() {
        viewModelScope.launch {
            while (isActive) {
                delay(18000L)
                val sid = _schoolConfig.value.schoolId
                val user = _currentUser.value
                if (sid.isNotBlank() && user != null) {
                    try {
                        // 1. Check notifications
                        val remoteNotifsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                            sid,
                            FirebaseConfigConstants.COL_NOTIFICATIONS
                        )
                        if (!remoteNotifsRaw.isNullOrBlank()) {
                            val remoteNotifs = repo.parseNotificationsJson(remoteNotifsRaw, sid)
                            val currentIds = _notifications.value.map { it.id }.toSet()
                            val newNotifs = remoteNotifs.filter { it.id !in currentIds }
                            if (newNotifs.isNotEmpty()) {
                                _notifications.value = remoteNotifs
                                val scopedNew = getScopedNotificationsForUser(user).filter { it.id in newNotifs.map { n -> n.id } }
                                scopedNew.firstOrNull()?.let { latest ->
                                    _latestBannerAlert.value = latest
                                    SystemNotificationHelper.showSystemPushNotification(
                                        appContext,
                                        latest.title,
                                        latest.body,
                                        latest.id
                                    )
                                }
                            }
                        }

                        // 2. Check messages
                        val remoteMsgsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                            sid,
                            FirebaseConfigConstants.COL_MESSAGES
                        )
                        if (!remoteMsgsRaw.isNullOrBlank()) {
                            val remoteMsgs = repo.parseChatMessagesJson(remoteMsgsRaw, sid)
                            val currentIds = _chatMessages.value.map { it.id }.toSet()
                            val newMsgs = remoteMsgs.filter { it.id !in currentIds }
                            if (newMsgs.isNotEmpty()) {
                                _chatMessages.value = remoteMsgs
                                val newest = newMsgs.last()
                                if (!newest.senderName.contains(user.fullName)) {
                                    SystemNotificationHelper.showSystemPushNotification(
                                        appContext,
                                        "رسالة جديدة من ${newest.senderName}",
                                        newest.messageText.ifBlank { "صورة مرفقة في المحادثة" },
                                        newest.id
                                    )
                                }
                            }
                        }

                        // 3. Check schedules_homework
                        val remoteHwRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                            sid,
                            FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK
                        )
                        if (!remoteHwRaw.isNullOrBlank()) {
                            val remoteHws = repo.parseHomeworksJson(remoteHwRaw, sid)
                            val currentIds = _homeworks.value.map { it.id }.toSet()
                            val newHws = remoteHws.filter { it.id !in currentIds }
                            if (newHws.isNotEmpty()) {
                                _homeworks.value = remoteHws
                                val newest = newHws.first()
                                SystemNotificationHelper.showSystemPushNotification(
                                    appContext,
                                    "واجب وتحضير جديد: ${newest.subjectName}",
                                    "الصف ${newest.gradeClass} (${newest.sectionName}): ${newest.dailyPreparation}",
                                    newest.id
                                )
                            }
                        }

                        // 4. Check reports
                        val remoteRepRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                            sid,
                            FirebaseConfigConstants.COL_REPORTS
                        )
                        if (!remoteRepRaw.isNullOrBlank()) {
                            val remoteReps = repo.parseReportsJson(remoteRepRaw, sid)
                            val currentIds = _reports.value.map { it.id }.toSet()
                            val newReps = remoteReps.filter { it.id !in currentIds }
                            if (newReps.isNotEmpty()) {
                                _reports.value = remoteReps
                                val newest = newReps.first()
                                if (user.role == UserRole.ADMIN || user.linkedStudentId == newest.studentId) {
                                    SystemNotificationHelper.showSystemPushNotification(
                                        appContext,
                                        "بلاغ رسمي (${newest.reportType.arabicLabel})",
                                        "${newest.title} — الطالب: ${newest.studentName}",
                                        newest.id
                                    )
                                }
                            }
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    // =========================================================================
    // ROLE-BASED & CLASS-BASED DYNAMIC SCOPING HELPERS
    // =========================================================================

    fun getAllowedClassesForUser(user: UserAccount? = _currentUser.value): List<String> {
        val regClasses = availableClasses
        if (user == null || user.role == UserRole.ADMIN) return regClasses
        return when (user.role) {
            UserRole.TEACHER -> {
                val filtered = regClasses.filter { cls -> user.canTeacherAccessClassSection(cls, null) }
                filtered.ifEmpty { regClasses.take(1) }
            }
            UserRole.PARENT, UserRole.STUDENT -> {
                val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
                if (linkedStu != null) listOf(linkedStu.gradeClass) else listOf(user.gradeClass)
            }
            else -> regClasses
        }
    }

    fun getAllowedSectionsForClass(gradeClass: String, user: UserAccount? = _currentUser.value): List<String> {
        val regSections = availableSections
        if (user == null || user.role == UserRole.ADMIN) return regSections
        return when (user.role) {
            UserRole.TEACHER -> {
                val filtered = regSections.filter { sec -> user.canTeacherAccessClassSection(gradeClass, sec) }
                filtered.ifEmpty { regSections }
            }
            UserRole.PARENT, UserRole.STUDENT -> {
                val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
                if (linkedStu != null) listOf(linkedStu.sectionName) else listOf(user.sectionName)
            }
            else -> regSections
        }
    }

    fun getSubjectsForClass(
        gradeClass: String,
        user: UserAccount? = _currentUser.value
    ): List<String> {
        val configs = _schoolConfig.value.subjectConfigs
        val classSubjects = if (configs.isNotEmpty()) {
            configs
                .filter { it.assignedClasses.isEmpty() || gradeClass in it.assignedClasses }
                .map { it.name }
                .ifEmpty { availableSubjects }
        } else {
            availableSubjects
        }

        if (user != null && user.role == UserRole.TEACHER) {
            val teacherSubs = user.teacherSubjectsList
            if (teacherSubs.isNotEmpty()) {
                val intersection = classSubjects.filter { sub -> user.canTeacherAccessSubject(sub) }
                return intersection.ifEmpty { teacherSubs }
            }
        }
        return classSubjects
    }

    fun getAllowedSubjectsForUser(user: UserAccount? = _currentUser.value): List<String> {
        val regSubjects = availableSubjects
        if (user == null || user.role == UserRole.ADMIN) return regSubjects
        return when (user.role) {
            UserRole.TEACHER -> {
                val teacherSubs = user.teacherSubjectsList
                if (teacherSubs.isNotEmpty()) teacherSubs else regSubjects
            }
            else -> regSubjects
        }
    }

    fun getDefaultMaxScoreForClassAndSubject(gradeClass: String, subjectName: String): Int {
        if (gradeClass.trim() in GradingRules.tenPointPrimaryClasses) {
            return 10
        }
        val subjCfg = _schoolConfig.value.subjectConfigs.firstOrNull {
            it.name.equals(subjectName.trim(), ignoreCase = true)
        }
        return subjCfg?.defaultMaxScore ?: GradingRules.defaultMaxScoreForClass(gradeClass)
    }

    fun getScopedStudentsForUser(user: UserAccount? = _currentUser.value): List<StudentRecord> {
        val schoolId = _schoolConfig.value.schoolId
        val all = _students.value.filter { it.schoolId.isEmpty() || it.schoolId == schoolId }
        if (user == null) return emptyList()
        return when (user.role) {
            UserRole.ADMIN -> all
            UserRole.TEACHER -> all.filter { stu ->
                user.canTeacherAccessClassSection(stu.gradeClass, stu.sectionName)
            }
            UserRole.PARENT, UserRole.STUDENT -> all.filter { stu ->
                stu.id == user.linkedStudentId
            }
        }
    }

    fun getScopedNotificationsForUser(user: UserAccount? = _currentUser.value): List<AppNotification> {
        val schoolId = _schoolConfig.value.schoolId
        val all = _notifications.value.filter { it.schoolId.isEmpty() || it.schoolId == schoolId }
        if (user == null) return emptyList()
        return when (user.role) {
            UserRole.ADMIN -> all
            UserRole.TEACHER -> {
                val allowedStuIds = getScopedStudentsForUser(user).map { it.id }.toSet()
                all.filter { it.targetStudentId.isEmpty() || it.targetStudentId in allowedStuIds }
            }
            UserRole.PARENT, UserRole.STUDENT -> {
                all.filter { it.targetStudentId.isEmpty() || it.targetStudentId == user.linkedStudentId }
            }
        }
    }

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

    // =========================================================================
    // 1. MULTI-TENANT SCHOOL FOUNDING (CLEAN STATE + ISOLATED schoolId)
    // =========================================================================

    fun setupSchoolAndAdmin(
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String,
        schoolStage: SchoolStage = SchoolStage.SECONDARY,
        customSections: List<String> = listOf("أ", "ب", "ج"),
        customSchoolId: String = ""
    ) {
        val cleanName = InputSanitizer.sanitize(schoolName)
        val cleanUser = InputSanitizer.sanitizeUsername(adminUsername)
        val cleanPass = InputSanitizer.sanitize(adminPassword)
        if (cleanName.isBlank() || cleanUser.isBlank() || cleanPass.isBlank()) {
            showToastAlert("يرجى إدخال اسم المدرسة واسم المستخدم وكلمة المرور للمدير", isSuccess = false)
            return
        }

        val generatedSchoolId = InputSanitizer.sanitizeSchoolId(customSchoolId).ifBlank {
            "SCH-${(100000..999999).random()}"
        }
        val finalAddress = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" }
        val generatedClasses = schoolStage.defaultClasses
        val generatedSubjects = schoolStage.defaultSubjects
        val finalSections = customSections.ifEmpty { listOf("أ", "ب", "ج") }

        val initialSubjectConfigs = generatedSubjects.mapIndexed { idx, subName ->
            SubjectConfigItem(
                id = "sub_${idx + 1}",
                name = subName,
                assignedClasses = generatedClasses,
                defaultMaxScore = if (schoolStage == SchoolStage.PRIMARY) 10 else 100,
                schoolId = generatedSchoolId
            )
        }

        val config = SchoolConfig(
            schoolId = generatedSchoolId,
            isSetupCompleted = true,
            schoolName = cleanName,
            address = finalAddress,
            schoolType = schoolType,
            schoolStage = schoolStage,
            registeredClasses = generatedClasses,
            registeredSections = finalSections,
            registeredSubjects = generatedSubjects,
            subjectConfigs = initialSubjectConfigs,
            logoBase64 = logoBase64,
            academicYear = "2026 - 2027",
            phone = "07512362567"
        )
        repo.setActiveSchoolId(generatedSchoolId)
        repo.saveSchoolConfig(config)
        _schoolConfig.value = config

        // Job title for administration is exclusively "المدير"
        val adminAccount = UserAccount(
            id = "admin_${generatedSchoolId.lowercase()}",
            username = cleanUser,
            password = cleanPass,
            fullName = InputSanitizer.sanitize(adminFullName).ifBlank { "المدير" },
            role = UserRole.ADMIN,
            isActive = true,
            phone = "07512362567",
            specialty = "المدير",
            assignedClasses = "جميع الصفوف والشعب",
            address = finalAddress,
            photoBase64 = logoBase64,
            schoolId = generatedSchoolId
        )

        // Clean State: Start completely empty without any pre-existing demo students or teachers
        val cleanAccounts = listOf(adminAccount)
        repo.saveAccounts(cleanAccounts, generatedSchoolId)
        _accounts.value = cleanAccounts

        _students.value = emptyList()
        repo.saveStudents(emptyList(), generatedSchoolId)

        _attendance.value = emptyList()
        repo.saveAttendance(emptyList(), generatedSchoolId)

        _schedules.value = emptyList()
        repo.saveSchedules(emptyList(), generatedSchoolId)

        _homeworks.value = emptyList()
        repo.saveHomeworks(emptyList(), generatedSchoolId)

        _examSchedules.value = emptyList()
        repo.saveExamSchedules(emptyList(), generatedSchoolId)

        _grades.value = emptyList()
        repo.saveGrades(emptyList(), generatedSchoolId)

        _tuitions.value = emptyList()
        repo.saveTuitions(emptyList(), generatedSchoolId)

        _reports.value = emptyList()
        repo.saveReports(emptyList(), generatedSchoolId)

        _chatMessages.value = emptyList()
        repo.saveChatMessages(emptyList(), generatedSchoolId)

        _events.value = emptyList()
        repo.saveEvents(emptyList(), generatedSchoolId)

        _notifications.value = emptyList()
        repo.saveNotifications(emptyList(), generatedSchoolId)

        val session = UserSession(
            userId = adminAccount.id,
            username = adminAccount.username,
            role = UserRole.ADMIN,
            schoolId = generatedSchoolId,
            sessionToken = UUID.randomUUID().toString()
        )
        repo.saveSession(session)
        _activeSession.value = session
        _currentUser.value = adminAccount
        _activeSection.value = AppSection.DASHBOARD

        showToastAlert("تم تأسيس منصة المدرسة ($cleanName) بنجاح • كود المدرسة: $generatedSchoolId", isSuccess = true)
    }

    fun updateSchoolInfo(
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        schoolStage: SchoolStage = _schoolConfig.value.schoolStage,
        registeredClasses: List<String> = _schoolConfig.value.registeredClasses,
        registeredSections: List<String> = _schoolConfig.value.registeredSections,
        registeredSubjects: List<String> = _schoolConfig.value.registeredSubjects
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val cleanName = InputSanitizer.sanitize(schoolName)
        if (cleanName.isBlank()) {
            showToastAlert("لا يمكن ترك اسم المدرسة فارغاً", isSuccess = false)
            return
        }

        val finalClasses = registeredClasses.map { InputSanitizer.sanitize(it) }.filter { it.isNotEmpty() }
            .ifEmpty { schoolStage.defaultClasses }
        val finalSections = registeredSections.map { InputSanitizer.sanitize(it) }.filter { it.isNotEmpty() }
            .ifEmpty { listOf("أ", "ب", "ج") }
        val finalSubjects = registeredSubjects.map { InputSanitizer.sanitize(it) }.filter { it.isNotEmpty() }
            .ifEmpty { schoolStage.defaultSubjects }

        val updatedConfigs = finalSubjects.mapIndexed { idx, subName ->
            val existing = _schoolConfig.value.subjectConfigs.firstOrNull { it.name == subName }
            existing?.copy(
                assignedClasses = existing.assignedClasses.filter { it in finalClasses }.ifEmpty { finalClasses }
            ) ?: SubjectConfigItem(
                id = "sub_${idx + 1}_${UUID.randomUUID().toString().take(4)}",
                name = subName,
                assignedClasses = finalClasses,
                defaultMaxScore = if (schoolStage == SchoolStage.PRIMARY) 10 else 100,
                schoolId = _schoolConfig.value.schoolId
            )
        }

        val updated = _schoolConfig.value.copy(
            schoolName = cleanName,
            address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
            schoolType = schoolType,
            schoolStage = schoolStage,
            registeredClasses = finalClasses,
            registeredSections = finalSections,
            registeredSubjects = finalSubjects,
            subjectConfigs = updatedConfigs,
            logoBase64 = logoBase64.ifBlank { _schoolConfig.value.logoBase64 }
        )
        repo.saveSchoolConfig(updated)
        _schoolConfig.value = updated
        if (schoolType == SchoolType.PUBLIC && _activeSection.value == AppSection.TUITION_FEES) {
            _activeSection.value = AppSection.DASHBOARD
        }
        showToastAlert("تم حفظ إعدادات المدرسة وتحديث الصفوف والشعب بنجاح", isSuccess = true)
    }

    // =========================================================================
    // SUBJECTS MANAGEMENT & CLASS CUSTOMIZATION (إدارة وتخصيص المواد الدراسية)
    // =========================================================================

    fun addOrUpdateSubjectConfig(
        subjectId: String?,
        subjectName: String,
        assignedClasses: List<String>,
        defaultMaxScore: Int = 100
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val cleanName = InputSanitizer.sanitize(subjectName)
        if (cleanName.isBlank()) {
            showToastAlert("يرجى إدخال اسم المادة الدراسية", isSuccess = false)
            return
        }
        val targetClasses = assignedClasses.ifEmpty { availableClasses }
        val sid = _schoolConfig.value.schoolId
        val currentConfigs = _schoolConfig.value.subjectConfigs.toMutableList()

        val existingIndex = if (subjectId != null) {
            currentConfigs.indexOfFirst { it.id == subjectId }
        } else {
            currentConfigs.indexOfFirst { it.name.equals(cleanName, ignoreCase = true) }
        }

        val item = SubjectConfigItem(
            id = if (existingIndex >= 0) currentConfigs[existingIndex].id else "sub_${UUID.randomUUID().toString().take(6)}",
            name = cleanName,
            assignedClasses = targetClasses,
            defaultMaxScore = if (defaultMaxScore == 10) 10 else 100,
            schoolId = sid
        )

        if (existingIndex >= 0) {
            currentConfigs[existingIndex] = item
        } else {
            currentConfigs.add(item)
        }

        val updatedSubjectNames = currentConfigs.map { it.name }.distinct()
        val updatedConfig = _schoolConfig.value.copy(
            registeredSubjects = updatedSubjectNames,
            subjectConfigs = currentConfigs
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        repo.saveSubjectConfigs(currentConfigs, sid)
        showToastAlert("تم حفظ وتخصيص مادة ($cleanName) للصفوف المختارة بنجاح", isSuccess = true)
    }

    fun deleteSubjectConfig(subjectName: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        if (_schoolConfig.value.registeredSubjects.size <= 1) {
            showToastAlert("يجب بقاء مادة دراسية واحدة على الأقل في المنصة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val updatedConfigs = _schoolConfig.value.subjectConfigs.filterNot {
            it.name.equals(subjectName, ignoreCase = true)
        }
        val updatedNames = _schoolConfig.value.registeredSubjects.filterNot {
            it.equals(subjectName, ignoreCase = true)
        }
        val updatedConfig = _schoolConfig.value.copy(
            registeredSubjects = updatedNames,
            subjectConfigs = updatedConfigs
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        repo.saveSubjectConfigs(updatedConfigs, sid)
        showToastAlert("تم حذف المادة الدراسية ($subjectName) بنجاح", isSuccess = true)
    }

    fun addCustomRegisteredClass(newClassName: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val clean = InputSanitizer.sanitize(newClassName)
        if (clean.isBlank() || clean in _schoolConfig.value.registeredClasses) {
            showToastAlert("يرجى إدخال اسم صف جديد غير مكرر", isSuccess = false)
            return
        }
        val updated = _schoolConfig.value.copy(
            registeredClasses = _schoolConfig.value.registeredClasses + clean
        )
        _schoolConfig.value = updated
        repo.saveSchoolConfig(updated)
        showToastAlert("تمت إضافة الصف ($clean) إلى قائمة الصفوف المسجلة", isSuccess = true)
    }

    fun removeRegisteredClass(className: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        if (_schoolConfig.value.registeredClasses.size <= 1) {
            showToastAlert("لا يمكن حذف جميع الصفوف المسجلة", isSuccess = false)
            return
        }
        val updated = _schoolConfig.value.copy(
            registeredClasses = _schoolConfig.value.registeredClasses.filterNot { it == className }
        )
        _schoolConfig.value = updated
        repo.saveSchoolConfig(updated)
        showToastAlert("تم حذف الصف ($className)", isSuccess = true)
    }

    fun addCustomRegisteredSection(newSection: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val clean = InputSanitizer.sanitize(newSection)
        if (clean.isBlank() || clean in _schoolConfig.value.registeredSections) {
            showToastAlert("يرجى إدخال اسم شعبة جديد غير مكرر", isSuccess = false)
            return
        }
        val updated = _schoolConfig.value.copy(
            registeredSections = _schoolConfig.value.registeredSections + clean
        )
        _schoolConfig.value = updated
        repo.saveSchoolConfig(updated)
        showToastAlert("تمت إضافة شعبة ($clean) بنجاح", isSuccess = true)
    }

    fun addCustomRegisteredSubject(newSubject: String) {
        addOrUpdateSubjectConfig(
            subjectId = null,
            subjectName = newSubject,
            assignedClasses = availableClasses,
            defaultMaxScore = 100
        )
    }

    // =========================================================================
    // LOGIN & LOGOUT WITH SESSION VALIDATION
    // =========================================================================

    fun login(usernameInput: String, passwordInput: String): Result<UserAccount> {
        val cleanUser = InputSanitizer.sanitizeUsername(usernameInput)
        val cleanPass = InputSanitizer.sanitize(passwordInput)
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            showToastAlert("يرجى إدخال اسم المستخدم وكلمة المرور", isSuccess = false)
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور"))
        }

        val currentSchoolId = _schoolConfig.value.schoolId
        val matched = _accounts.value.firstOrNull {
            it.username.equals(cleanUser, ignoreCase = true) &&
                it.password == cleanPass &&
                (currentSchoolId.isEmpty() || it.schoolId.isEmpty() || it.schoolId == currentSchoolId)
        } ?: run {
            showToastAlert("بيانات الدخول غير صحيحة، تأكد من اسم المستخدم وكلمة المرور", isSuccess = false)
            return Result.failure(IllegalArgumentException("بيانات الدخول غير صحيحة، تأكد من اسم المستخدم وكلمة المرور"))
        }

        if (!matched.isActive) {
            showToastAlert("هذا الحساب موقوف حالياً من قبل إدارة المدرسة", isSuccess = false)
            return Result.failure(IllegalStateException("هذا الحساب موقوف حالياً من قبل إدارة المدرسة"))
        }

        val resolvedSchoolId = matched.schoolId.ifBlank { currentSchoolId }
        val session = UserSession(
            userId = matched.id,
            username = matched.username,
            role = matched.role,
            schoolId = resolvedSchoolId,
            sessionToken = UUID.randomUUID().toString()
        )
        repo.saveSession(session)
        _activeSession.value = session
        _currentUser.value = matched
        _activeSection.value = AppSection.DASHBOARD

        showToastAlert("مرحباً بك ${matched.fullName} (${matched.role.arabicTitle})", isSuccess = true)
        return Result.success(matched)
    }

    fun logout() {
        repo.clearSession()
        _activeSession.value = null
        _currentUser.value = null
        _activeSection.value = AppSection.DASHBOARD
        showToastAlert("تم تسجيل الخروج من الجلسة بأمان", isSuccess = true)
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
    ): UserAccount? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val cleanName = InputSanitizer.sanitize(fullName)
        if (cleanName.isBlank()) {
            showToastAlert("يرجى إدخال اسم المعلم الثلاثي", isSuccess = false)
            return null
        }

        val sid = _schoolConfig.value.schoolId
        val cleanUsername = InputSanitizer.sanitizeUsername(username).ifBlank { "tch_${(100..999).random()}" }
        val cleanPassword = InputSanitizer.sanitize(password).ifBlank { (1000..9999).random().toString() }

        if (_accounts.value.any { it.username.equals(cleanUsername, ignoreCase = true) }) {
            showToastAlert("اسم المستخدم ($cleanUsername) مستخدم مسبقاً، اختر اسماً آخر", isSuccess = false)
            return null
        }

        val newTeacher = UserAccount(
            id = "tch_${UUID.randomUUID().toString().take(6)}",
            username = cleanUsername,
            password = cleanPassword,
            fullName = cleanName,
            role = UserRole.TEACHER,
            isActive = true,
            phone = InputSanitizer.sanitize(phone).ifBlank { "07512362567" },
            specialty = InputSanitizer.sanitize(specialty),
            assignedClasses = InputSanitizer.sanitize(assignedClasses),
            address = "الموصل - زمار",
            schoolId = sid
        )
        val updated = listOf(newTeacher) + _accounts.value
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        showToastAlert("تم إنشاء حساب المعلم ($cleanName) وربطه بالمدرسة بنجاح", isSuccess = true)
        return newTeacher
    }

    fun updateTeacherAccount(
        teacherId: String,
        fullName: String,
        specialty: String,
        assignedClasses: String,
        phone: String,
        username: String,
        password: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _accounts.value.map { acc ->
            if (acc.id == teacherId && acc.role == UserRole.TEACHER) {
                acc.copy(
                    fullName = InputSanitizer.sanitize(fullName).ifBlank { acc.fullName },
                    specialty = InputSanitizer.sanitize(specialty),
                    assignedClasses = InputSanitizer.sanitize(assignedClasses),
                    phone = InputSanitizer.sanitize(phone).ifBlank { acc.phone },
                    username = InputSanitizer.sanitizeUsername(username).ifBlank { acc.username },
                    password = InputSanitizer.sanitize(password).ifBlank { acc.password },
                    schoolId = sid
                )
            } else acc
        }
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        showToastAlert("تم تحديث بيانات وصلاحيات المعلم بنجاح", isSuccess = true)
    }

    fun toggleAccountActive(accountId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _accounts.value.map { acc ->
            if (acc.id == accountId && acc.role != UserRole.ADMIN) {
                acc.copy(isActive = !acc.isActive)
            } else acc
        }
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        showToastAlert("تم تحديث حالة تفعيل الحساب بنجاح", isSuccess = true)
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
    ): StudentRecord? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val cleanStuName = InputSanitizer.sanitize(studentName)
        val cleanParName = InputSanitizer.sanitize(parentName)
        if (cleanStuName.isBlank() || cleanParName.isBlank()) {
            showToastAlert("يرجى إدخال اسم الطالب واسم ولي الأمر بالكامل", isSuccess = false)
            return null
        }

        val sid = _schoolConfig.value.schoolId
        val stuId = "stu_${UUID.randomUUID().toString().take(6)}"
        val finalAddress = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" }
        val finalPhone = InputSanitizer.sanitize(parentPhone).ifBlank { "07512362567" }
        val sUser = InputSanitizer.sanitizeUsername(studentUsername).ifBlank { "stu_${(100..999).random()}" }
        val sPass = InputSanitizer.sanitize(studentPassword).ifBlank { (1000..9999).random().toString() }
        val pUser = InputSanitizer.sanitizeUsername(parentUsername).ifBlank { "par_${(100..999).random()}" }
        val pPass = InputSanitizer.sanitize(parentPassword).ifBlank { (1000..9999).random().toString() }

        val newStudent = StudentRecord(
            id = stuId,
            studentName = cleanStuName,
            gradeClass = gradeClass,
            sectionName = sectionName,
            parentName = cleanParName,
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
            honorTitle = "طالب مجتهد",
            schoolId = sid
        )

        val updatedStudents = listOf(newStudent) + _students.value
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val parentAcc = UserAccount(
            id = "acc_parent_$stuId",
            username = pUser,
            password = pPass,
            fullName = cleanParName,
            role = UserRole.PARENT,
            isActive = true,
            phone = finalPhone,
            linkedStudentId = stuId,
            gradeClass = gradeClass,
            sectionName = sectionName,
            address = finalAddress,
            schoolId = sid
        )
        val studentAcc = UserAccount(
            id = "acc_stu_$stuId",
            username = sUser,
            password = sPass,
            fullName = cleanStuName,
            role = UserRole.STUDENT,
            isActive = true,
            phone = finalPhone,
            linkedStudentId = stuId,
            gradeClass = gradeClass,
            sectionName = sectionName,
            address = finalAddress,
            photoBase64 = photoBase64,
            schoolId = sid
        )

        val updatedAccounts = listOf(parentAcc, studentAcc) + _accounts.value
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)

        val newTuition = TuitionRecord(
            studentId = stuId,
            studentName = newStudent.studentName,
            gradeClass = gradeClass,
            sectionName = sectionName,
            parentName = newStudent.parentName,
            parentPhone = finalPhone,
            totalFee = totalFee.coerceAtLeast(500000L),
            nextDueDate = "2026-11-15",
            installments = emptyList(),
            schoolId = sid
        )
        val updatedTuitions = listOf(newTuition) + _tuitions.value
        _tuitions.value = updatedTuitions
        repo.saveTuitions(updatedTuitions, sid)

        showToastAlert("تمت إضافة الطالب ($cleanStuName) وحساب ولي أمره في ($gradeClass - $sectionName)", isSuccess = true)
        return newStudent
    }

    fun updateStudentAndParentAccounts(
        studentId: String,
        studentName: String,
        gradeClass: String,
        sectionName: String,
        parentName: String,
        parentPhone: String,
        address: String,
        studentUsername: String,
        studentPassword: String,
        parentUsername: String,
        parentPassword: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updatedStudents = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    studentName = InputSanitizer.sanitize(studentName).ifBlank { s.studentName },
                    gradeClass = gradeClass,
                    sectionName = sectionName,
                    parentName = InputSanitizer.sanitize(parentName).ifBlank { s.parentName },
                    parentPhone = InputSanitizer.sanitize(parentPhone).ifBlank { s.parentPhone },
                    address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                    studentUsername = InputSanitizer.sanitizeUsername(studentUsername).ifBlank { s.studentUsername },
                    studentPassword = InputSanitizer.sanitize(studentPassword).ifBlank { s.studentPassword },
                    parentUsername = InputSanitizer.sanitizeUsername(parentUsername).ifBlank { s.parentUsername },
                    parentPassword = InputSanitizer.sanitize(parentPassword).ifBlank { s.parentPassword },
                    schoolId = sid
                )
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val updatedAccounts = _accounts.value.map { acc ->
            if (acc.linkedStudentId == studentId) {
                if (acc.role == UserRole.PARENT) {
                    acc.copy(
                        fullName = InputSanitizer.sanitize(parentName).ifBlank { acc.fullName },
                        username = InputSanitizer.sanitizeUsername(parentUsername).ifBlank { acc.username },
                        password = InputSanitizer.sanitize(parentPassword).ifBlank { acc.password },
                        phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                        gradeClass = gradeClass,
                        sectionName = sectionName,
                        address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                        schoolId = sid
                    )
                } else {
                    acc.copy(
                        fullName = InputSanitizer.sanitize(studentName).ifBlank { acc.fullName },
                        username = InputSanitizer.sanitizeUsername(studentUsername).ifBlank { acc.username },
                        password = InputSanitizer.sanitize(studentPassword).ifBlank { acc.password },
                        phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                        gradeClass = gradeClass,
                        sectionName = sectionName,
                        address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                        schoolId = sid
                    )
                }
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)
        showToastAlert("تم تحديث بيانات الطالب وولي الأمر بنجاح", isSuccess = true)
    }

    fun toggleStudentAndParentActive(studentId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val target = _students.value.firstOrNull { it.id == studentId } ?: return
        val nextState = !target.isActive

        val updatedStudents = _students.value.map {
            if (it.id == studentId) it.copy(isActive = nextState) else it
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val updatedAccounts = _accounts.value.map {
            if (it.linkedStudentId == studentId) it.copy(isActive = nextState) else it
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)
        showToastAlert(
            if (nextState) "تم تفعيل حساب الطالب وولي الأمر" else "تم إيقاف حساب الطالب وولي الأمر",
            isSuccess = true
        )
    }

    // =========================================================================
    // SECTION 1: NOTIFICATIONS (Internal Bell + System Push Notifications)
    // =========================================================================

    fun pushNotification(
        title: String,
        body: String,
        type: NotificationType,
        targetStudentId: String = ""
    ) {
        val sid = _schoolConfig.value.schoolId
        val timeLabel = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val notif = AppNotification(
            id = "notif_${UUID.randomUUID().toString().take(6)}",
            title = InputSanitizer.sanitize(title),
            body = InputSanitizer.sanitize(body),
            type = type,
            targetStudentId = targetStudentId,
            createdAt = "الآن ($timeLabel)",
            isRead = false,
            schoolId = sid
        )
        val updated = listOf(notif) + _notifications.value
        _notifications.value = updated
        repo.saveNotifications(updated, sid)
        _latestBannerAlert.value = notif

        // Fire external Android System Push Notification with sound and vibration
        SystemNotificationHelper.showSystemPushNotification(
            context = appContext,
            title = notif.title,
            body = notif.body,
            tagId = notif.id
        )
    }

    fun markAllNotificationsRead() {
        val sid = _schoolConfig.value.schoolId
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        repo.saveNotifications(updated, sid)
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val existingIndex = _attendance.value.indexOfFirst {
            it.studentId == student.id && it.date == date
        }
        val entry = AttendanceEntry(
            id = if (existingIndex >= 0) _attendance.value[existingIndex].id else "att_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            date = InputSanitizer.sanitize(date),
            status = status,
            note = InputSanitizer.sanitize(note).ifBlank { status.arabicLabel },
            schoolId = sid
        )

        val updated = _attendance.value.toMutableList()
        if (existingIndex >= 0) {
            updated[existingIndex] = entry
        } else {
            updated.add(0, entry)
        }
        _attendance.value = updated
        repo.saveAttendance(updated, sid)

        showToastAlert("تم حفظ حالة (${status.arabicLabel}) للطالب ${student.studentName}", isSuccess = true)

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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val period = SchedulePeriod(
            id = "sch_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            dayOfWeek = dayOfWeek,
            periodNumber = periodNumber,
            subjectName = InputSanitizer.sanitize(subjectName),
            teacherName = InputSanitizer.sanitize(teacherName),
            timeSlot = InputSanitizer.sanitize(timeSlot),
            schoolId = sid
        )
        val updated = _schedules.value.filterNot {
            it.gradeClass == gradeClass &&
                it.sectionName == sectionName &&
                it.dayOfWeek == dayOfWeek &&
                it.periodNumber == periodNumber
        } + period
        _schedules.value = updated.sortedBy { it.periodNumber }
        repo.saveSchedules(_schedules.value, sid)
        showToastAlert("تم حفظ الحصة الدراسية ($subjectName) في الجدول الأسبوعي", isSuccess = true)
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanPrep = InputSanitizer.sanitize(dailyPreparation)
        if (cleanPrep.isBlank()) {
            showToastAlert("يرجى إدخال تفاصيل التحضير اليومي للدرس", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val hw = HomeworkItem(
            id = "hw_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            subjectName = InputSanitizer.sanitize(subjectName),
            teacherName = InputSanitizer.sanitize(teacherName),
            dailyPreparation = cleanPrep,
            homeworkDetails = InputSanitizer.sanitize(homeworkDetails),
            dueDate = InputSanitizer.sanitize(dueDate),
            createdAt = "اليوم",
            schoolId = sid
        )
        val updated = listOf(hw) + _homeworks.value
        _homeworks.value = updated
        repo.saveHomeworks(updated, sid)

        showToastAlert("تم نشر التحضير والواجب لمادة ($subjectName) بنجاح", isSuccess = true)

        pushNotification(
            title = "تحضير وواجب جديد: $subjectName",
            body = "للصف $gradeClass ($sectionName): $cleanPrep — الواجب: $homeworkDetails",
            type = NotificationType.HOMEWORK
        )
    }

    // =========================================================================
    // SECTION 4: EXAMS & FLEXIBLE GRADING (10 OR 100)
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val item = ExamScheduleItem(
            id = "ex_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            subjectName = InputSanitizer.sanitize(subjectName),
            examType = InputSanitizer.sanitize(examType),
            examDate = InputSanitizer.sanitize(examDate),
            dayName = InputSanitizer.sanitize(dayName),
            timeSlot = InputSanitizer.sanitize(timeSlot),
            topicsIncluded = InputSanitizer.sanitize(topicsIncluded),
            hallNumber = InputSanitizer.sanitize(hallNumber),
            schoolId = sid
        )
        val updated = listOf(item) + _examSchedules.value
        _examSchedules.value = updated
        repo.saveExamSchedules(updated, sid)

        showToastAlert("تمت إضافة موعد امتحان ($subjectName) للصف ($gradeClass)", isSuccess = true)

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
        finalExamScore: Int,
        maxScoreScale: Int = GradingRules.defaultMaxScoreForClass(student.gradeClass)
    ): Boolean {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return false
        val validScale = if (maxScoreScale == 10) 10 else 100
        val scores = listOf(dailyScore, month1Score, month2Score, midYearScore, finalExamScore)
        if (scores.any { it < 0 || it > validScale }) {
            showToastAlert(
                "خطأ في الإدخال: لا يمكن إدخال درجة أكبر من الحد الأقصى المعتمد ($validScale)",
                isSuccess = false
            )
            return false
        }

        val sid = _schoolConfig.value.schoolId
        val existingIdx = _grades.value.indexOfFirst {
            it.studentId == student.id && it.subjectName.equals(subjectName.trim(), ignoreCase = true)
        }
        val grade = SubjectGrade(
            id = if (existingIdx >= 0) _grades.value[existingIdx].id else "grd_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            subjectName = InputSanitizer.sanitize(subjectName),
            dailyScore = dailyScore.coerceIn(0, validScale),
            month1Score = month1Score.coerceIn(0, validScale),
            month2Score = month2Score.coerceIn(0, validScale),
            midYearScore = midYearScore.coerceIn(0, validScale),
            finalExamScore = finalExamScore.coerceIn(0, validScale),
            maxScoreScale = validScale,
            schoolId = sid
        )

        val updated = _grades.value.toMutableList()
        if (existingIdx >= 0) {
            updated[existingIdx] = grade
        } else {
            updated.add(0, grade)
        }
        _grades.value = updated
        repo.saveGrades(updated, sid)

        showToastAlert(
            "تم رصد درجة ($subjectName) للطالب ${student.studentName} بنجاح (من $validScale)",
            isSuccess = true
        )

        pushNotification(
            title = "رصد درجات مادة $subjectName",
            body = "تم تحديث درجات الطالب ${student.studentName} في مادة $subjectName (السعي: ${grade.annualEffortScore}/$validScale | المعدل: ${grade.finalAverage}/$validScale - ${grade.ratingLabel}).",
            type = NotificationType.GRADES,
            targetStudentId = student.id
        )
        return true
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    isOnHonorRoll = isOnHonorRoll,
                    excellencePoints = excellencePoints.coerceIn(50, 100),
                    honorTitle = InputSanitizer.sanitize(honorTitle).ifBlank { "وسام التفوق الذهبي" }
                )
            } else s
        }
        _students.value = updated
        repo.saveStudents(updated, sid)
        showToastAlert("تم تحديث لوحة الشرف للطالب بنجاح", isSuccess = true)
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        if (amount <= 0L) {
            showToastAlert("يرجى إدخال مبلغ قسط صحيح أكبر من الصفر", isSuccess = false)
            return null
        }
        val sid = _schoolConfig.value.schoolId
        val receiptNo = "REC-${(1000..9999).random()}"
        val installment = TuitionInstallment(
            id = "inst_${UUID.randomUUID().toString().take(6)}",
            amount = amount,
            paymentDate = InputSanitizer.sanitize(paymentDate).ifBlank { "2026-10-29" },
            receiptNumber = receiptNo,
            note = InputSanitizer.sanitize(note).ifBlank { "دفعة قسط دراسي" }
        )

        var targetStudentName = ""
        var remainingAfter = 0L

        val updated = _tuitions.value.map { rec ->
            if (rec.studentId == studentId) {
                val newList = listOf(installment) + rec.installments
                val copyRec = rec.copy(
                    installments = newList,
                    nextDueDate = InputSanitizer.sanitize(nextDueDate).ifBlank { rec.nextDueDate }
                )
                targetStudentName = copyRec.studentName
                remainingAfter = copyRec.remainingAmount
                copyRec
            } else rec
        }
        _tuitions.value = updated
        repo.saveTuitions(updated, sid)

        showToastAlert("تم تسجيل دفعة القسط ($receiptNo) للطالب $targetStudentName بنجاح", isSuccess = true)

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
    // SECTION 7: PARENT REPORTS & DIRECT MESSAGING (WITH PHOTO SUPPORT)
    // =========================================================================

    fun sendParentReport(
        student: StudentRecord,
        reportType: ReportType,
        title: String,
        details: String,
        senderName: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى كتابة عنوان البلاغ الرسمي", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val rep = ParentReport(
            id = "rep_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            parentName = student.parentName,
            parentPhone = student.parentPhone,
            reportType = reportType,
            title = cleanTitle,
            details = InputSanitizer.sanitize(details).ifBlank { cleanTitle },
            senderName = InputSanitizer.sanitize(senderName),
            createdAt = "اليوم",
            schoolId = sid
        )
        val updated = listOf(rep) + _reports.value
        _reports.value = updated
        repo.saveReports(updated, sid)

        showToastAlert("تم إرسال البلاغ الرسمي لولي أمر الطالب (${student.studentName}) بنجاح", isSuccess = true)

        pushNotification(
            title = "${reportType.arabicLabel}: ${student.studentName}",
            body = "$cleanTitle — ${rep.details}",
            type = NotificationType.REPORT,
            targetStudentId = student.id
        )
    }

    fun sendChatMessage(
        student: StudentRecord,
        messageText: String,
        imageBase64: String = ""
    ) {
        if (!validateSessionAndSchoolAccess()) return
        val user = _currentUser.value ?: return
        val cleanText = InputSanitizer.sanitize(messageText)
        if (cleanText.isBlank() && imageBase64.isBlank()) {
            showToastAlert("يرجى كتابة نص الرسالة أو إرفاق صورة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val timeLabel = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val msg = ChatMessage(
            id = "msg_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            senderName = "${user.fullName} (${user.role.arabicTitle})",
            senderRole = user.role,
            messageText = cleanText,
            timestamp = timeLabel,
            imageBase64 = imageBase64,
            schoolId = sid
        )
        val updated = _chatMessages.value + msg
        _chatMessages.value = updated
        repo.saveChatMessages(updated, sid)

        showToastAlert("تم إرسال الرسالة في المحادثة المباشرة", isSuccess = true)

        SystemNotificationHelper.showSystemPushNotification(
            context = appContext,
            title = "رسالة جديدة: ${msg.senderName}",
            body = cleanText.ifBlank { "تم إرفاق صورة في محادثة الطالب ${student.studentName}" },
            tagId = msg.id
        )
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
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى إدخال عنوان الفعالية أو الخبر المدرسي", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val author = _currentUser.value?.fullName ?: "إدارة المدرسة"
        val ev = SchoolEvent(
            id = "ev_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            category = InputSanitizer.sanitize(category).ifBlank { "فعالية مدرسية" },
            description = InputSanitizer.sanitize(description).ifBlank { cleanTitle },
            date = InputSanitizer.sanitize(date).ifBlank { "2026-10-29" },
            location = InputSanitizer.sanitize(location).ifBlank { "الموصل - زمار" },
            imageBase64 = imageBase64,
            authorName = author,
            schoolId = sid
        )
        val updated = listOf(ev) + _events.value
        _events.value = updated
        repo.saveEvents(updated, sid)

        showToastAlert("تم نشر الفعالية في معرض أخبار المدرسة بنجاح", isSuccess = true)

        pushNotification(
            title = "خبر جديد: ${ev.title}",
            body = ev.description,
            type = NotificationType.GENERAL
        )
    }

    // =========================================================================
    // SECTION 9: BACKUP & RESTORE (ADMIN ONLY)
    // =========================================================================

    fun exportSchoolBackupJson(): String? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val json = repo.exportFullSchoolBackupJson(_schoolConfig.value.schoolId)
        showToastAlert("تم تجهيز النسخة الاحتياطية الكاملة للمدرسة بصيغة JSON بنجاح", isSuccess = true)
        return json
    }

    fun importSchoolBackupJson(rawJson: String): Boolean {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return false
        if (rawJson.isBlank()) {
            showToastAlert("يرجى لصق محتوى النسخة الاحتياطية بصيغة JSON أولاً", isSuccess = false)
            return false
        }
        val res = repo.importFullSchoolBackupJson(rawJson, _schoolConfig.value.schoolId)
        return if (res.isSuccess) {
            val sid = res.getOrNull() ?: _schoolConfig.value.schoolId
            _schoolConfig.value = repo.loadSchoolConfig(sid)
            _accounts.value = repo.loadAccounts(sid)
            _students.value = repo.loadStudents(sid)
            _attendance.value = repo.loadAttendance(sid)
            _schedules.value = repo.loadSchedules(sid)
            _homeworks.value = repo.loadHomeworks(sid)
            _examSchedules.value = repo.loadExamSchedules(sid)
            _grades.value = repo.loadGrades(sid)
            _tuitions.value = repo.loadTuitions(sid)
            _reports.value = repo.loadReports(sid)
            _chatMessages.value = repo.loadChatMessages(sid)
            _events.value = repo.loadEvents(sid)
            _notifications.value = repo.loadNotifications(sid)
            showToastAlert("تمت استعادة النسخة الاحتياطية الكاملة للمدرسة ($sid) بنجاح", isSuccess = true)
            true
        } else {
            showToastAlert(res.exceptionOrNull()?.message ?: "فشل استيراد النسخة الاحتياطية", isSuccess = false)
            false
        }
    }
}
