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

    private val _leaveRequests = MutableStateFlow(repo.loadLeaveRequests())
    val leaveRequests: StateFlow<List<LeaveRequestItem>> = _leaveRequests.asStateFlow()

    private val _studyMaterials = MutableStateFlow(repo.loadStudyMaterials())
    val studyMaterials: StateFlow<List<StudyMaterialItem>> = _studyMaterials.asStateFlow()

    private val _developerBroadcasts = MutableStateFlow(repo.loadDeveloperBroadcasts())
    val developerBroadcasts: StateFlow<List<DeveloperBroadcastItem>> = _developerBroadcasts.asStateFlow()

    private val _dismissedBroadcastIds = MutableStateFlow(repo.loadDismissedBroadcastIds())
    val dismissedBroadcastIds: StateFlow<Set<String>> = _dismissedBroadcastIds.asStateFlow()

    private val _isSideDrawerEnabled = MutableStateFlow(repo.isSideDrawerEnabled())
    val isSideDrawerEnabled: StateFlow<Boolean> = _isSideDrawerEnabled.asStateFlow()

    private val _dailyBehaviors = MutableStateFlow(repo.loadDailyBehaviors())
    val dailyBehaviors: StateFlow<List<DailyBehaviorLogItem>> = _dailyBehaviors.asStateFlow()

    private val _homeworkSubmissions = MutableStateFlow(repo.loadHomeworkSubmissions())
    val homeworkSubmissions: StateFlow<List<HomeworkSubmissionItem>> = _homeworkSubmissions.asStateFlow()

    private val _quickQuizzes = MutableStateFlow(repo.loadQuickQuizzes())
    val quickQuizzes: StateFlow<List<QuickQuizItem>> = _quickQuizzes.asStateFlow()

    private val _quickQuizAnswers = MutableStateFlow(buildQuickQuizAnswerViews(_quickQuizzes.value))
    val quickQuizAnswers: StateFlow<List<QuickQuizAnswerView>> = _quickQuizAnswers.asStateFlow()

    private val _featureSettings = MutableStateFlow(_schoolConfig.value.featureSettings)
    val featureSettings: StateFlow<SchoolFeatureSettings> = _featureSettings.asStateFlow()

    private val _dismissalCalls = MutableStateFlow(repo.loadDismissalCalls())
    val dismissalCalls: StateFlow<List<DismissalCallItem>> = _dismissalCalls.asStateFlow()

    private val _lostAndFoundItems = MutableStateFlow(repo.loadLostAndFoundItems())
    val lostAndFoundItems: StateFlow<List<LostAndFoundItem>> = _lostAndFoundItems.asStateFlow()

    private val _privateSuggestions = MutableStateFlow(repo.loadPrivateSuggestions())
    val privateSuggestions: StateFlow<List<PrivateSuggestionItem>> = _privateSuggestions.asStateFlow()

    private val _appUpdateConfig = MutableStateFlow(repo.loadAppUpdateConfig())
    val appUpdateConfig: StateFlow<AppUpdateConfig> = _appUpdateConfig.asStateFlow()

    private val _dismissedUpdateVersion = MutableStateFlow("")
    val dismissedUpdateVersion: StateFlow<String> = _dismissedUpdateVersion.asStateFlow()

    private val _occasionThemeConfig = MutableStateFlow(repo.loadOccasionThemeConfig())
    val occasionThemeConfig: StateFlow<OccasionThemeConfig> = _occasionThemeConfig.asStateFlow()

    private val _biometricCredentials = MutableStateFlow(repo.loadBiometricCredentials())
    val biometricCredentials: StateFlow<BiometricSavedCredentials?> = _biometricCredentials.asStateFlow()

    private val _studentActivityPosts = MutableStateFlow(repo.loadStudentActivityPosts())
    val studentActivityPosts: StateFlow<List<StudentActivityPost>> = _studentActivityPosts.asStateFlow()

    private val _activityViolationReports = MutableStateFlow(repo.loadActivityViolationReports())
    val activityViolationReports: StateFlow<List<StudentActivityViolationReport>> = _activityViolationReports.asStateFlow()

    private val _userProfilesAndSanctions = MutableStateFlow(repo.loadUserProfilesAndSanctions())
    val userProfilesAndSanctions: StateFlow<Map<String, UserActivityProfileAndSanctions>> = _userProfilesAndSanctions.asStateFlow()

    private val _showUrgentAlertModal = MutableStateFlow(false)
    val showUrgentAlertModal: StateFlow<Boolean> = _showUrgentAlertModal.asStateFlow()

    private val _deepLinkTarget = MutableStateFlow<DeepLinkNavigationTarget?>(null)
    val deepLinkTarget: StateFlow<DeepLinkNavigationTarget?> = _deepLinkTarget.asStateFlow()

    // Security Shield Brute-Force Lockout States
    private val _failedLoginAttempts = MutableStateFlow(0)
    val failedLoginAttempts: StateFlow<Int> = _failedLoginAttempts.asStateFlow()
    val failedLoginAttemptsCount: StateFlow<Int> = _failedLoginAttempts.asStateFlow()

    private val _loginLockoutUntilMs = MutableStateFlow(0L)
    val loginLockoutUntilMs: StateFlow<Long> = _loginLockoutUntilMs.asStateFlow()

    private fun buildQuickQuizAnswerViews(quizzes: List<QuickQuizItem>): List<QuickQuizAnswerView> {
        return quizzes.flatMap { q ->
            q.studentAnswers.map { (stuId, chosenIdx) ->
                val correct = chosenIdx == q.correctOptionIndex
                QuickQuizAnswerView(
                    quizId = q.id,
                    studentId = stuId,
                    selectedOptionIndex = chosenIdx,
                    isCorrect = correct,
                    earnedPoints = if (correct) q.points else 0
                )
            }
        }
    }

    private val _selectedParentChildId = MutableStateFlow("")
    val selectedParentChildId: StateFlow<String> = _selectedParentChildId.asStateFlow()

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

    // Developer Super-Admin & Activation Code States
    private val _isDeveloperAuthenticated = MutableStateFlow(false)
    val isDeveloperAuthenticated: StateFlow<Boolean> = _isDeveloperAuthenticated.asStateFlow()

    private val _isSchoolSetupUnlocked = MutableStateFlow(false)
    val isSchoolSetupUnlocked: StateFlow<Boolean> = _isSchoolSetupUnlocked.asStateFlow()

    private val _pendingActivationCode = MutableStateFlow<String?>(null)
    val pendingActivationCode: StateFlow<String?> = _pendingActivationCode.asStateFlow()

    private val _schoolLicenses = MutableStateFlow(repo.loadSchoolLicenses())
    val schoolLicenses: StateFlow<List<SchoolLicense>> = _schoolLicenses.asStateFlow()

    private val _registeredSchools = MutableStateFlow(repo.loadRegisteredSchools())
    val registeredSchools: StateFlow<List<RegisteredSchoolSummary>> = _registeredSchools.asStateFlow()

    private val _discoveredSchoolName = MutableStateFlow<String?>(null)
    val discoveredSchoolName: StateFlow<String?> = _discoveredSchoolName.asStateFlow()

    private val _loginConnectingMessage = MutableStateFlow<String?>(null)
    val loginConnectingMessage: StateFlow<String?> = _loginConnectingMessage.asStateFlow()

    val availableClasses: List<String>
        get() = _schoolConfig.value.registeredClasses.ifEmpty { SchoolStage.SECONDARY.defaultClasses }

    val availableSections: List<String>
        get() = _schoolConfig.value.registeredSections.ifEmpty { listOf("أ", "ب", "ج") }

    val availableSubjects: List<String>
        get() = _schoolConfig.value.registeredSubjects.ifEmpty { SchoolStage.SECONDARY.defaultSubjects }

    val weekDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")

    init {
        SystemNotificationHelper.ensureChannelCreated(appContext)
        refreshDeveloperCloudData()
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
            if (!isSchoolSubscriptionActive(currentSchoolId)) {
                repo.clearSession()
                _activeSession.value = null
                _currentUser.value = null
                showToastAlert("اشتراك المدرسة متوقف، يرجى مراجعة المطور أحمد أبو رسلان", isSuccess = false)
                return
            }
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

    private fun isSchoolSubscriptionActive(schoolId: String): Boolean {
        if (schoolId.isBlank()) return true
        val regEntry = _registeredSchools.value.firstOrNull { it.schoolId.equals(schoolId, ignoreCase = true) }
        if (regEntry != null && !regEntry.isActive) return false
        val localCfg = repo.loadSchoolConfig(schoolId)
        if (localCfg.isSetupCompleted && !localCfg.isActive) return false
        return true
    }

    private fun reloadSchoolDataIntoMemory(schoolId: String) {
        if (schoolId.isBlank()) return
        repo.setActiveSchoolId(schoolId)
        val loadedCfg = repo.loadSchoolConfig(schoolId)
        _schoolConfig.value = loadedCfg
        _featureSettings.value = loadedCfg.featureSettings
        _accounts.value = repo.loadAccounts(schoolId)
        _students.value = repo.loadStudents(schoolId)
        _attendance.value = repo.loadAttendance(schoolId)
        _schedules.value = repo.loadSchedules(schoolId)
        _homeworks.value = repo.loadHomeworks(schoolId)
        _examSchedules.value = repo.loadExamSchedules(schoolId)
        _grades.value = repo.loadGrades(schoolId)
        _tuitions.value = repo.loadTuitions(schoolId)
        _reports.value = repo.loadReports(schoolId)
        _chatMessages.value = repo.loadChatMessages(schoolId)
        _events.value = repo.loadEvents(schoolId)
        _notifications.value = repo.loadNotifications(schoolId)
        _leaveRequests.value = repo.loadLeaveRequests(schoolId)
        _studyMaterials.value = repo.loadStudyMaterials(schoolId)
        _dailyBehaviors.value = repo.loadDailyBehaviors(schoolId)
        _homeworkSubmissions.value = repo.loadHomeworkSubmissions(schoolId)
        val loadedQuizzes = repo.loadQuickQuizzes(schoolId)
        _quickQuizzes.value = loadedQuizzes
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(loadedQuizzes)
        _dismissalCalls.value = repo.loadDismissalCalls(schoolId)
        _lostAndFoundItems.value = repo.loadLostAndFoundItems(schoolId)
        _privateSuggestions.value = repo.loadPrivateSuggestions(schoolId)
        _studentActivityPosts.value = repo.loadStudentActivityPosts(schoolId)
        _activityViolationReports.value = repo.loadActivityViolationReports(schoolId)
        _userProfilesAndSanctions.value = repo.loadUserProfilesAndSanctions(schoolId)
    }

    // =========================================================================
    // DEVELOPER SUPER-ADMIN PANEL & 5-TAP ACTIVATION CODE FLOW
    // =========================================================================

    fun refreshDeveloperCloudData() {
        viewModelScope.launch {
            try {
                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_SCHOOL_LICENSES,
                    docName = "registry"
                )?.let { remoteJson ->
                    val remoteList = repo.parseSchoolLicensesJson(remoteJson)
                    if (remoteList.isNotEmpty()) {
                        val merged = (remoteList + _schoolLicenses.value)
                            .groupBy { it.code.uppercase() }
                            .map { (_, items) ->
                                items.firstOrNull { it.used } ?: items.first()
                            }
                        _schoolLicenses.value = merged
                        repo.saveSchoolLicenses(merged)
                    }
                }

                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_SCHOOLS_REGISTRY,
                    docName = "registry"
                )?.let { remoteJson ->
                    val remoteSchools = repo.parseRegisteredSchoolsJson(remoteJson)
                    if (remoteSchools.isNotEmpty()) {
                        val merged = (remoteSchools + _registeredSchools.value)
                            .distinctBy { it.schoolId }
                        _registeredSchools.value = merged
                        repo.saveRegisteredSchools(merged)
                    }
                }

                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_DEV_BROADCASTS,
                    docName = "registry"
                )?.let { remoteJson ->
                    if (remoteJson.isNotBlank()) {
                        val remoteBroadcasts = repo.parseDeveloperBroadcastsJson(remoteJson)
                        _developerBroadcasts.value = remoteBroadcasts
                        repo.saveDeveloperBroadcastsLocalOnly(remoteBroadcasts)
                    }
                }

                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_APP_UPDATE_CONFIG,
                    docName = "registry"
                )?.let { remoteUpdateJson ->
                    val parsed = repo.parseAppUpdateConfigJson(remoteUpdateJson)
                    _appUpdateConfig.value = parsed
                }

                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_OCCASION_THEME,
                    docName = "registry"
                )?.let { remoteThemeJson ->
                    val parsed = repo.parseOccasionThemeConfigJson(remoteThemeJson)
                    _occasionThemeConfig.value = parsed
                }
            } catch (_: Exception) {
            }
        }
    }

    fun generateNewSchoolLicense(): SchoolLicense {
        val existingCodes = _schoolLicenses.value.map { it.code.uppercase() }.toSet()
        var newCode = "MAD-${(1000..9999).random()}"
        while (newCode in existingCodes) {
            newCode = "MAD-${(1000..9999).random()}"
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val license = SchoolLicense(
            code = newCode,
            used = false,
            createdAt = today
        )
        val updated = listOf(license) + _schoolLicenses.value
        _schoolLicenses.value = updated
        repo.saveSchoolLicenses(updated)
        showToastAlert("تم توليد كود التفعيل ($newCode) وحفظه في السحابة بنجاح", isSuccess = true)
        return license
    }

    fun openSchoolSetupFromDeveloper() {
        _pendingActivationCode.value = "DEV-DIRECT"
        _isSchoolSetupUnlocked.value = true
        _isDeveloperAuthenticated.value = false
    }

    fun cancelSchoolSetupAndReturn() {
        val wasFromDev = _pendingActivationCode.value == "DEV-DIRECT"
        _isSchoolSetupUnlocked.value = false
        _pendingActivationCode.value = null
        if (wasFromDev) {
            _isDeveloperAuthenticated.value = true
        }
    }

    fun exitDeveloperPanel() {
        _isDeveloperAuthenticated.value = false
        showToastAlert("تم الخروج من لوحة تحكم المطور", isSuccess = true)
    }

    fun toggleSchoolSubscriptionActive(schoolId: String) {
        val updatedList = _registeredSchools.value.map { s ->
            if (s.schoolId == schoolId) s.copy(isActive = !s.isActive) else s
        }
        _registeredSchools.value = updatedList
        repo.saveRegisteredSchools(updatedList)

        val targetSummary = updatedList.firstOrNull { it.schoolId == schoolId }
        val nextActive = targetSummary?.isActive ?: true

        // Update the school's own settings document in local storage & Firebase
        val cfg = repo.loadSchoolConfig(schoolId)
        if (cfg.schoolId.isNotBlank() || schoolId == _schoolConfig.value.schoolId) {
            val prevActiveId = repo.getActiveSchoolId()
            val updatedCfg = (if (cfg.schoolId.isNotBlank()) cfg else _schoolConfig.value).copy(
                schoolId = schoolId,
                isActive = nextActive
            )
            repo.saveSchoolConfig(updatedCfg)
            if (prevActiveId.isNotBlank() && prevActiveId != schoolId) {
                repo.setActiveSchoolId(prevActiveId)
            }
            if (_schoolConfig.value.schoolId == schoolId) {
                _schoolConfig.value = updatedCfg
            }
        }

        val statusLabel = if (nextActive) "مفعّل" else "متوقف"
        showToastAlert("تم تحديث اشتراك مدرسة (${targetSummary?.schoolName ?: schoolId}) إلى: $statusLabel", isSuccess = nextActive)
    }

    fun updateSchoolSubscriptionExpiryDate(schoolId: String, newExpiryDate: String) {
        val cleanDate = InputSanitizer.sanitize(newExpiryDate).trim()
        if (cleanDate.length < 8) {
            showToastAlert("يرجى إدخال تاريخ انتهاء صالح بصيغة YYYY-MM-DD", isSuccess = false)
            return
        }
        val updatedList = _registeredSchools.value.map { s ->
            if (s.schoolId == schoolId) s.copy(subscriptionExpiryDate = cleanDate) else s
        }
        _registeredSchools.value = updatedList
        repo.saveRegisteredSchools(updatedList)

        val cfg = repo.loadSchoolConfig(schoolId)
        if (cfg.schoolId.isNotBlank() || schoolId == _schoolConfig.value.schoolId) {
            val prevActiveId = repo.getActiveSchoolId()
            val updatedCfg = (if (cfg.schoolId.isNotBlank()) cfg else _schoolConfig.value).copy(
                schoolId = schoolId,
                subscriptionExpiryDate = cleanDate
            )
            repo.saveSchoolConfig(updatedCfg)
            if (prevActiveId.isNotBlank() && prevActiveId != schoolId) {
                repo.setActiveSchoolId(prevActiveId)
            }
            if (_schoolConfig.value.schoolId == schoolId) {
                _schoolConfig.value = updatedCfg
            }
        }
        val targetSummary = updatedList.firstOrNull { it.schoolId == schoolId }
        val remDays = targetSummary?.daysUntilExpiry() ?: 365L
        showToastAlert(
            "تم تحديث تاريخ انتهاء اشتراك (${targetSummary?.schoolName ?: schoolId}) إلى $cleanDate (متبقي $remDays يوم)",
            isSuccess = true
        )
    }

    fun sendDeveloperBroadcastToPrincipals(title: String, message: String) {
        val cleanTitle = InputSanitizer.sanitize(title)
        val cleanMsg = InputSanitizer.sanitize(message)
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) {
            showToastAlert("يرجى إدخال عنوان ونص التعميم الموجه لمدراء المدارس", isSuccess = false)
            return
        }
        val today = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val item = DeveloperBroadcastItem(
            id = "dev_bc_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            message = cleanMsg,
            senderName = "المطور أحمد أبو رسلان",
            createdAt = today,
            targetSchoolId = "ALL"
        )
        val updated = listOf(item) + _developerBroadcasts.value
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)

        // Also push an ADMIN-targeted notification to each registered school
        _registeredSchools.value.forEach { school ->
            val sid = school.schoolId
            if (sid.isNotBlank()) {
                val existingNotifs = repo.loadNotifications(sid)
                val adminNotif = AppNotification(
                    id = "notif_dev_${UUID.randomUUID().toString().take(6)}",
                    title = "تعميم من المطور: $cleanTitle",
                    body = cleanMsg,
                    type = NotificationType.GENERAL,
                    targetRole = UserRole.ADMIN.name,
                    createdAt = today,
                    isRead = false,
                    schoolId = sid
                )
                val updatedSchoolNotifs = listOf(adminNotif) + existingNotifs
                repo.saveNotifications(updatedSchoolNotifs, sid)
                if (sid == _schoolConfig.value.schoolId) {
                    _notifications.value = updatedSchoolNotifs
                }
            }
        }

        showToastAlert("تم إرسال التعميم الفوري لجميع مدراء المدارس المشتركة بنجاح", isSuccess = true)
    }

    fun updateDeveloperBroadcast(broadcastId: String, newTitle: String, newMessage: String) {
        val cleanTitle = InputSanitizer.sanitize(newTitle)
        val cleanMsg = InputSanitizer.sanitize(newMessage)
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) {
            showToastAlert("يرجى إدخال عنوان ونص التعميم المحدث", isSuccess = false)
            return
        }
        val updated = _developerBroadcasts.value.map { item ->
            if (item.id == broadcastId) {
                item.copy(title = cleanTitle, message = cleanMsg)
            } else item
        }
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)
        showToastAlert("تم تعديل وتحديث نص التعميم لدى جميع مدراء المدارس فوراً", isSuccess = true)
    }

    fun deleteDeveloperBroadcast(broadcastId: String) {
        val updated = _developerBroadcasts.value.filterNot { it.id == broadcastId }
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)
        showToastAlert("تم حذف التعميم نهائياً وإزالته من شاشات جميع المدراء", isSuccess = true)
    }

    fun dismissDeveloperBroadcastForAdmin(broadcastId: String) {
        if (broadcastId.isBlank()) return
        val nextSet = _dismissedBroadcastIds.value + broadcastId
        _dismissedBroadcastIds.value = nextSet
        repo.saveDismissedBroadcastIds(nextSet)
    }

    fun verifyActivationCodeAndUnlockSetup(
        rawCode: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val cleanCode = InputSanitizer.sanitizeSchoolId(rawCode).uppercase()
        if (cleanCode.isBlank()) {
            showToastAlert("يرجى إدخال كود تفعيل تأسيس المدرسة", isSuccess = false)
            onResult(false)
            return
        }

        viewModelScope.launch {
            // Pull latest licenses from cloud first if online
            try {
                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_SCHOOL_LICENSES,
                    docName = "registry"
                )?.let { remoteJson ->
                    val remoteList = repo.parseSchoolLicensesJson(remoteJson)
                    if (remoteList.isNotEmpty()) {
                        val merged = (remoteList + _schoolLicenses.value)
                            .groupBy { it.code.uppercase() }
                            .map { (_, items) -> items.firstOrNull { it.used } ?: items.first() }
                        _schoolLicenses.value = merged
                        repo.saveSchoolLicenses(merged)
                    }
                }
            } catch (_: Exception) {
            }

            val matched = _schoolLicenses.value.firstOrNull {
                it.code.equals(cleanCode, ignoreCase = true)
            }

            if (matched == null) {
                showToastAlert("كود التفعيل ($cleanCode) غير صحيح، يرجى مراجعة المطور أحمد أبو رسلان", isSuccess = false)
                onResult(false)
                return@launch
            }

            if (matched.used) {
                val usedSchoolLabel = matched.usedBySchoolName.ifBlank { matched.usedBySchoolId }
                showToastAlert("هذا الكود مستخدم مسبقاً لتأسيس مدرسة ($usedSchoolLabel) ولا يصلح للاستخدام مرة أخرى", isSuccess = false)
                onResult(false)
                return@launch
            }

            _pendingActivationCode.value = matched.code
            _isSchoolSetupUnlocked.value = true
            showToastAlert("تم التحقق من كود التفعيل (${matched.code}) بنجاح! يمكنك الآن تأسيس المدرسة", isSuccess = true)
            onResult(true)
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
     * Real-time live cloud listener for ALL school sub-collections under schools/{schoolId}/...
     * Automatically updates UI StateFlows in real time without logging out or manual refresh,
     * and dispatches system push notifications ONLY to the strictly targeted user.
     */
    private fun startRealtimeCloudMonitor() {
        viewModelScope.launch {
            while (isActive) {
                delay(1500L)
                val sid = _schoolConfig.value.schoolId
                val user = _currentUser.value
                if (sid.isNotBlank() && user != null) {
                    try {
                        // 1. Live sync: honor_roll & students (including Honor Roll status, points, heart reactions, photos)
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HONOR_ROLL)) {
                            val remoteStudentsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_HONOR_ROLL
                            )
                            if (!remoteStudentsRaw.isNullOrBlank() &&
                                remoteStudentsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HONOR_ROLL)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HONOR_ROLL, remoteStudentsRaw)
                                _students.value = repo.parseStudentsJson(remoteStudentsRaw, sid)
                            }
                        }

                        // 2. Live sync: users (Teachers, Parents, Students, Admin accounts & photos)
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_USERS)) {
                            val remoteUsersRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_USERS
                            )
                            if (!remoteUsersRaw.isNullOrBlank() &&
                                remoteUsersRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_USERS)
                            ) {
                                val parsedAccounts = repo.parseAccountsJson(remoteUsersRaw, sid)
                                if (parsedAccounts.isNotEmpty()) {
                                    repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_USERS, remoteUsersRaw)
                                    _accounts.value = parsedAccounts
                                    _currentUser.value?.let { curr ->
                                        parsedAccounts.firstOrNull { it.id == curr.id }?.let { refreshedUser ->
                                            _currentUser.value = refreshedUser
                                        }
                                    }
                                }
                            }
                        }

                        // 3. Live sync: settings & subjects
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_SUBJECTS)) {
                            val remoteSubjRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_SUBJECTS
                            )
                            if (!remoteSubjRaw.isNullOrBlank() &&
                                remoteSubjRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SUBJECTS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SUBJECTS, remoteSubjRaw)
                                val loadedCfg = repo.loadSchoolConfig(sid)
                                _schoolConfig.value = loadedCfg
                                _featureSettings.value = loadedCfg.featureSettings
                            }
                        }
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_SETTINGS)) {
                            val remoteCfgRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_SETTINGS
                            )
                            if (!remoteCfgRaw.isNullOrBlank() &&
                                remoteCfgRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SETTINGS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SETTINGS, remoteCfgRaw)
                                val parsedCfg = repo.parseSchoolConfigJson(remoteCfgRaw, sid)
                                _schoolConfig.value = parsedCfg
                                _featureSettings.value = parsedCfg.featureSettings
                                enforceActiveSectionFeatureVisibility(parsedCfg.featureSettings)
                            }
                        }

                        // 4. Live sync: events_gallery
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY)) {
                            val remoteEventsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_EVENTS_GALLERY
                            )
                            if (!remoteEventsRaw.isNullOrBlank() &&
                                remoteEventsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_EVENTS_GALLERY, remoteEventsRaw)
                                _events.value = repo.parseEventsJson(remoteEventsRaw, sid)
                            }
                        }

                        // 5. Live sync: reports
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_REPORTS)) {
                            val remoteRepRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_REPORTS
                            )
                            if (!remoteRepRaw.isNullOrBlank() &&
                                remoteRepRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_REPORTS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_REPORTS, remoteRepRaw)
                                _reports.value = repo.parseReportsJson(remoteRepRaw, sid)
                            }
                        }

                        // 6. Live sync: messages (Direct Chat)
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_MESSAGES)) {
                            val remoteMsgsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_MESSAGES
                            )
                            if (!remoteMsgsRaw.isNullOrBlank() &&
                                remoteMsgsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_MESSAGES)
                            ) {
                                val remoteMsgs = repo.parseChatMessagesJson(remoteMsgsRaw, sid)
                                val currentIds = _chatMessages.value.map { it.id }.toSet()
                                val newMsgs = remoteMsgs.filter { it.id !in currentIds }
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_MESSAGES, remoteMsgsRaw)
                                _chatMessages.value = remoteMsgs

                                if (newMsgs.isNotEmpty()) {
                                    val newest = newMsgs.last()
                                    val isSentByMe = newest.senderName.contains(user.fullName)
                                    val isTargetedRecipient = when {
                                        isSentByMe -> false
                                        newest.senderRole == UserRole.ADMIN || newest.senderRole == UserRole.TEACHER ->
                                            (user.role == UserRole.PARENT || user.role == UserRole.STUDENT) &&
                                                user.linkedStudentId == newest.studentId
                                        newest.senderRole == UserRole.PARENT || newest.senderRole == UserRole.STUDENT ->
                                            user.role == UserRole.ADMIN
                                        else -> false
                                    }
                                    if (isTargetedRecipient) {
                                        SystemNotificationHelper.showSystemPushNotification(
                                            appContext,
                                            "رسالة جديدة من ${newest.senderName}",
                                            newest.messageText.ifBlank { "صورة مرفقة في المحادثة" },
                                            newest.id
                                        )
                                    }
                                }
                            }
                        }

                        // 7. Live sync: attendance
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ATTENDANCE)) {
                            val remoteAttRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_ATTENDANCE
                            )
                            if (!remoteAttRaw.isNullOrBlank() &&
                                remoteAttRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ATTENDANCE)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ATTENDANCE, remoteAttRaw)
                                _attendance.value = repo.parseAttendanceJson(remoteAttRaw, sid)
                            }
                        }

                        // 8. Live sync: grades & exams
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_GRADES)) {
                            val remoteGradesRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_GRADES
                            )
                            if (!remoteGradesRaw.isNullOrBlank() &&
                                remoteGradesRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_GRADES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_GRADES, remoteGradesRaw)
                                _grades.value = repo.parseGradesJson(remoteGradesRaw, sid)
                            }
                        }
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_EXAMS)) {
                            val remoteExamsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_EXAMS
                            )
                            if (!remoteExamsRaw.isNullOrBlank() &&
                                remoteExamsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_EXAMS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_EXAMS, remoteExamsRaw)
                                _examSchedules.value = repo.parseExamSchedulesJson(remoteExamsRaw, sid)
                            }
                        }

                        // 9. Live sync: schedules_homework & schedules_periods
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)) {
                            val remoteHwRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK
                            )
                            if (!remoteHwRaw.isNullOrBlank() &&
                                remoteHwRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SCHEDULES_HOMEWORK, remoteHwRaw)
                                _homeworks.value = repo.parseHomeworksJson(remoteHwRaw, sid)
                            }
                        }
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS)) {
                            val remotePeriodsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_SCHEDULES_PERIODS
                            )
                            if (!remotePeriodsRaw.isNullOrBlank() &&
                                remotePeriodsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_SCHEDULES_PERIODS, remotePeriodsRaw)
                                _schedules.value = repo.parseSchedulesJson(remotePeriodsRaw, sid)
                            }
                        }

                        // 10. Live sync: tuition_fees
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_TUITION_FEES)) {
                            val remoteTuitionRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_TUITION_FEES
                            )
                            if (!remoteTuitionRaw.isNullOrBlank() &&
                                remoteTuitionRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_TUITION_FEES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_TUITION_FEES, remoteTuitionRaw)
                                _tuitions.value = repo.parseTuitionsJson(remoteTuitionRaw, sid)
                            }
                        }

                        // 11. Live sync: notifications (strictly filtered for the target recipient only)
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_NOTIFICATIONS)) {
                            val remoteNotifsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_NOTIFICATIONS
                            )
                            if (!remoteNotifsRaw.isNullOrBlank() &&
                                remoteNotifsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_NOTIFICATIONS)
                            ) {
                                val remoteNotifs = repo.parseNotificationsJson(remoteNotifsRaw, sid)
                                val currentIds = _notifications.value.map { it.id }.toSet()
                                val newNotifIds = remoteNotifs.filter { it.id !in currentIds }.map { it.id }.toSet()
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_NOTIFICATIONS, remoteNotifsRaw)
                                _notifications.value = remoteNotifs

                                if (newNotifIds.isNotEmpty()) {
                                    val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
                                    val parentChildIds = getChildrenForParent(user).map { it.id }.toSet()
                                    val targetedNew = remoteNotifs.filter { n ->
                                        n.id in newNotifIds && n.isTargetedForUser(user, linkedStu, parentChildIds)
                                    }
                                    targetedNew.firstOrNull()?.let { latest ->
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
                        }

                        // 12. Live sync: leave_requests
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_LEAVE_REQUESTS)) {
                            val remoteLeavesRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_LEAVE_REQUESTS
                            )
                            if (!remoteLeavesRaw.isNullOrBlank() &&
                                remoteLeavesRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_LEAVE_REQUESTS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_LEAVE_REQUESTS, remoteLeavesRaw)
                                _leaveRequests.value = repo.parseLeaveRequestsJson(remoteLeavesRaw, sid)
                            }
                        }

                        // 13. Live sync: study_materials
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_STUDY_MATERIALS)) {
                            val remoteMatsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_STUDY_MATERIALS
                            )
                            if (!remoteMatsRaw.isNullOrBlank() &&
                                remoteMatsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDY_MATERIALS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDY_MATERIALS, remoteMatsRaw)
                                _studyMaterials.value = repo.parseStudyMaterialsJson(remoteMatsRaw, sid)
                            }
                        }

                        // 14. Live sync: developer broadcasts for School Principal (ADMIN)
                        if (user.role == UserRole.ADMIN) {
                            FirebaseCloudSync.fetchGlobalCollectionPayload(
                                collectionName = FirebaseConfigConstants.COL_DEV_BROADCASTS,
                                docName = "registry"
                            )?.let { remoteBcJson ->
                                if (remoteBcJson.isNotBlank()) {
                                    val parsedBc = repo.parseDeveloperBroadcastsJson(remoteBcJson)
                                    if (parsedBc != _developerBroadcasts.value) {
                                        _developerBroadcasts.value = parsedBc
                                        repo.saveDeveloperBroadcastsLocalOnly(parsedBc)
                                    }
                                }
                            }
                        }

                        // 15. Live sync: feature_settings, urgent_alert, daily_behavior, homework_submissions, quick_quizzes, dismissal_calls, lost_and_found, private_suggestions
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS)) {
                            val remoteFeatRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS)
                            if (!remoteFeatRaw.isNullOrBlank() &&
                                remoteFeatRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_FEATURE_SETTINGS, remoteFeatRaw)
                                val parsedFeat = repo.parseFeatureSettingsJson(remoteFeatRaw)
                                _schoolConfig.value = _schoolConfig.value.copy(featureSettings = parsedFeat)
                                _featureSettings.value = parsedFeat
                                enforceActiveSectionFeatureVisibility(parsedFeat)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_URGENT_ALERT)) {
                            val remoteUrgRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_URGENT_ALERT)
                            if (!remoteUrgRaw.isNullOrBlank() &&
                                remoteUrgRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_URGENT_ALERT)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_URGENT_ALERT, remoteUrgRaw)
                                val parsedUrg = repo.parseUrgentSchoolAlertJson(remoteUrgRaw, sid)
                                if (parsedUrg != _schoolConfig.value.urgentAlert) {
                                    _schoolConfig.value = _schoolConfig.value.copy(urgentAlert = parsedUrg)
                                }
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR)) {
                            val remoteBehRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR)
                            if (!remoteBehRaw.isNullOrBlank() &&
                                remoteBehRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_DAILY_BEHAVIOR, remoteBehRaw)
                                _dailyBehaviors.value = repo.parseDailyBehaviorsJson(remoteBehRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS)) {
                            val remoteSubRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS)
                            if (!remoteSubRaw.isNullOrBlank() &&
                                remoteSubRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HOMEWORK_SUBMISSIONS, remoteSubRaw)
                                _homeworkSubmissions.value = repo.parseHomeworkSubmissionsJson(remoteSubRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_QUICK_QUIZZES)) {
                            val remoteQuizRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_QUICK_QUIZZES)
                            if (!remoteQuizRaw.isNullOrBlank() &&
                                remoteQuizRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_QUICK_QUIZZES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_QUICK_QUIZZES, remoteQuizRaw)
                                val parsedQ = repo.parseQuickQuizzesJson(remoteQuizRaw, sid)
                                _quickQuizzes.value = parsedQ
                                _quickQuizAnswers.value = buildQuickQuizAnswerViews(parsedQ)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_DISMISSAL_CALLS)) {
                            val remoteDisRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_DISMISSAL_CALLS)
                            if (!remoteDisRaw.isNullOrBlank() &&
                                remoteDisRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_DISMISSAL_CALLS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_DISMISSAL_CALLS, remoteDisRaw)
                                _dismissalCalls.value = repo.parseDismissalCallsJson(remoteDisRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_LOST_AND_FOUND)) {
                            val remoteLfRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_LOST_AND_FOUND)
                            if (!remoteLfRaw.isNullOrBlank() &&
                                remoteLfRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_LOST_AND_FOUND)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_LOST_AND_FOUND, remoteLfRaw)
                                _lostAndFoundItems.value = repo.parseLostAndFoundJson(remoteLfRaw, sid)
                            }
                        }

                        if (user.role == UserRole.ADMIN &&
                            !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS)
                        ) {
                            val remoteSugRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS)
                            if (!remoteSugRaw.isNullOrBlank() &&
                                remoteSugRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_PRIVATE_SUGGESTIONS, remoteSugRaw)
                                _privateSuggestions.value = repo.parsePrivateSuggestionsJson(remoteSugRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)) {
                            val remoteActRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)
                            if (!remoteActRaw.isNullOrBlank() &&
                                remoteActRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES, remoteActRaw)
                                _studentActivityPosts.value = repo.parseStudentActivityPostsJson(remoteActRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ACTIVITY_REPORTS)) {
                            val remoteRepActRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_ACTIVITY_REPORTS)
                            if (!remoteRepActRaw.isNullOrBlank() &&
                                remoteRepActRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ACTIVITY_REPORTS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ACTIVITY_REPORTS, remoteRepActRaw)
                                _activityViolationReports.value = repo.parseActivityViolationReportsJson(remoteRepActRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_USER_PROFILES)) {
                            val remoteProfRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_USER_PROFILES)
                            if (!remoteProfRaw.isNullOrBlank() &&
                                remoteProfRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_USER_PROFILES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_USER_PROFILES, remoteProfRaw)
                                _userProfilesAndSanctions.value = repo.parseUserProfilesAndSanctionsJson(remoteProfRaw, sid)
                            }
                        }

                    } catch (_: Exception) {
                    }
                }

                // 16. Global App Update & Occasion Theme live sync (active for all users including Login/Onboarding)
                try {
                    FirebaseCloudSync.fetchGlobalCollectionPayload(
                        collectionName = FirebaseConfigConstants.COL_APP_UPDATE_CONFIG,
                        docName = "registry"
                    )?.let { remoteUpd ->
                        val parsedUpd = repo.parseAppUpdateConfigJson(remoteUpd)
                        if (parsedUpd != _appUpdateConfig.value) {
                            _appUpdateConfig.value = parsedUpd
                        }
                    }
                    FirebaseCloudSync.fetchGlobalCollectionPayload(
                        collectionName = FirebaseConfigConstants.COL_OCCASION_THEME,
                        docName = "registry"
                    )?.let { remoteThm ->
                        val parsedThm = repo.parseOccasionThemeConfigJson(remoteThm)
                        if (parsedThm != _occasionThemeConfig.value) {
                            _occasionThemeConfig.value = parsedThm
                            val targetAlias = if (parsedThm.isActive) {
                                parsedThm.effectiveAliasName.ifBlank { parsedThm.themeType.name }
                            } else {
                                "default"
                            }
                            com.example.util.AppIconSwitcherHelper.changeLauncherIcon(
                                appContext,
                                targetAlias
                            )
                        }
                    }
                } catch (_: Exception) {
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

    /**
     * Returns all children belonging to a parent in the current school (linked by ID, parent username,
     * parent full name, or non-default parent phone number).
     */
    fun getChildrenForParent(user: UserAccount? = _currentUser.value): List<StudentRecord> {
        if (user == null || user.role != UserRole.PARENT) return emptyList()
        val schoolId = _schoolConfig.value.schoolId
        val all = _students.value.filter { it.schoolId.isEmpty() || it.schoolId == schoolId }
        val cleanParentName = user.fullName.trim()
        val cleanParentPhone = user.phone.trim()
        return all.filter { stu ->
            stu.id == user.linkedStudentId ||
                stu.parentUsername.equals(user.username, ignoreCase = true) ||
                (cleanParentName.isNotEmpty() && stu.parentName.trim().equals(cleanParentName, ignoreCase = true)) ||
                (cleanParentPhone.length >= 10 && cleanParentPhone != "07512362567" && stu.parentPhone.trim() == cleanParentPhone)
        }.distinctBy { it.id }
    }

    /**
     * Switches the active child in the Parent interface so that all screens (Attendance, Grades,
     * Schedule, Digital ID Card, Tuition, Leave Requests) immediately reflect the chosen child.
     */
    fun selectParentChild(studentId: String) {
        val user = _currentUser.value ?: return
        if (user.role != UserRole.PARENT) return
        val child = _students.value.firstOrNull { it.id == studentId } ?: return
        _selectedParentChildId.value = child.id
        _currentUser.value = user.copy(
            linkedStudentId = child.id,
            gradeClass = child.gradeClass,
            sectionName = child.sectionName
        )
        showToastAlert("تم التبديل إلى ملف الابن: ${child.fullNameWithFather} (${child.gradeClass})", isSuccess = true)
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
            UserRole.PARENT -> {
                val activeId = _selectedParentChildId.value.ifBlank { user.linkedStudentId }
                val activeMatch = all.filter { it.id == activeId }
                if (activeMatch.isNotEmpty()) activeMatch else getChildrenForParent(user).take(1)
            }
            UserRole.STUDENT -> all.filter { stu ->
                stu.id == user.linkedStudentId
            }
        }
    }

    fun getScopedNotificationsForUser(user: UserAccount? = _currentUser.value): List<AppNotification> {
        val schoolId = _schoolConfig.value.schoolId
        val all = _notifications.value.filter { it.schoolId.isEmpty() || it.schoolId == schoolId }
        if (user == null) return emptyList()
        val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
        val parentChildIds = getChildrenForParent(user).map { it.id }.toSet()
        return all.filter { notif -> notif.isTargetedForUser(user, linkedStu, parentChildIds) }
    }

    /**
     * Role-Based Messaging Contacts Directory:
     * - ADMIN: All Teachers, all Students, and all Parents in the school.
     * - TEACHER: School Admin + ONLY Students and Parents registered in the classes & sections assigned to this Teacher.
     * - STUDENT / PARENT: ONLY the School Admin + Teachers assigned to this student's specific class & section.
     */
    fun getAllowedChatContacts(user: UserAccount? = _currentUser.value): List<ChatContactItem> {
        if (user == null) return emptyList()
        val sid = _schoolConfig.value.schoolId
        val schoolAccounts = _accounts.value.filter { it.schoolId.isEmpty() || it.schoolId == sid }
        val schoolStudents = _students.value.filter { it.schoolId.isEmpty() || it.schoolId == sid }

        val adminAccount = schoolAccounts.firstOrNull { it.role == UserRole.ADMIN }
        val adminContact = ChatContactItem(
            accountId = adminAccount?.id ?: "admin_${sid.lowercase()}",
            fullName = adminAccount?.fullName?.ifBlank { "مدير المدرسة" } ?: "مدير المدرسة",
            role = UserRole.ADMIN,
            subtitle = "مدير المدرسة • إدارة ${_schoolConfig.value.schoolName}",
            phone = adminAccount?.phone?.ifBlank { _schoolConfig.value.phone } ?: _schoolConfig.value.phone,
            photoBase64 = adminAccount?.photoBase64?.ifBlank { _schoolConfig.value.logoBase64 } ?: _schoolConfig.value.logoBase64
        )

        val teacherContacts = schoolAccounts
            .filter { it.role == UserRole.TEACHER && it.id != user.id }
            .map { t ->
                val spec = t.specialty.ifBlank { "كادر تدريسي" }
                val assigned = t.assignedClasses.ifBlank { "الصفوف المسندة" }
                ChatContactItem(
                    accountId = t.id,
                    fullName = t.fullName,
                    role = UserRole.TEACHER,
                    subtitle = "معلم ($spec) • $assigned",
                    gradeClass = assigned,
                    phone = t.phone,
                    photoBase64 = t.photoBase64
                )
            }

        fun buildStudentAndParentContacts(studentsSubset: List<StudentRecord>): List<ChatContactItem> {
            val result = mutableListOf<ChatContactItem>()
            studentsSubset.forEach { stu ->
                val stuAcc = schoolAccounts.firstOrNull {
                    it.role == UserRole.STUDENT &&
                        (it.linkedStudentId == stu.id || it.username.equals(stu.studentUsername, ignoreCase = true))
                }
                val parAcc = schoolAccounts.firstOrNull {
                    it.role == UserRole.PARENT &&
                        (it.linkedStudentId == stu.id || it.username.equals(stu.parentUsername, ignoreCase = true))
                }
                val stuAccId = stuAcc?.id ?: "acc_stu_${stu.id}"
                val parAccId = parAcc?.id ?: "acc_par_${stu.id}"

                if (stuAccId != user.id) {
                    result.add(
                        ChatContactItem(
                            accountId = stuAccId,
                            fullName = stu.studentName,
                            role = UserRole.STUDENT,
                            subtitle = "طالب • ${stu.gradeClass} - شعبة (${stu.sectionName})",
                            gradeClass = stu.gradeClass,
                            sectionName = stu.sectionName,
                            phone = stu.parentPhone,
                            photoBase64 = stu.photoBase64.ifBlank { stuAcc?.photoBase64.orEmpty() },
                            linkedStudentId = stu.id,
                            linkedStudentName = stu.studentName
                        )
                    )
                }
                if (parAccId != user.id) {
                    val existingIdx = result.indexOfFirst { it.accountId == parAccId }
                    if (existingIdx >= 0) {
                        val existing = result[existingIdx]
                        val combinedSubtitle = "ولي أمر الطالب: ${existing.linkedStudentName} و ${stu.studentName} • ${stu.gradeClass}"
                        result[existingIdx] = existing.copy(subtitle = combinedSubtitle)
                    } else {
                        result.add(
                            ChatContactItem(
                                accountId = parAccId,
                                fullName = stu.parentName.ifBlank { "ولي أمر الطالب" },
                                role = UserRole.PARENT,
                                subtitle = "ولي أمر الطالب: ${stu.studentName} • ${stu.gradeClass} (${stu.sectionName})",
                                gradeClass = stu.gradeClass,
                                sectionName = stu.sectionName,
                                phone = stu.parentPhone,
                                photoBase64 = parAcc?.photoBase64?.ifBlank { stu.photoBase64 } ?: stu.photoBase64,
                                linkedStudentId = stu.id,
                                linkedStudentName = stu.studentName
                            )
                        )
                    }
                }
            }
            return result.distinctBy { it.accountId }
        }

        val rawContacts = when (user.role) {
            UserRole.ADMIN -> {
                teacherContacts + buildStudentAndParentContacts(schoolStudents)
            }
            UserRole.TEACHER -> {
                val assignedStudents = schoolStudents.filter { stu ->
                    user.canTeacherAccessClassSection(stu.gradeClass, stu.sectionName)
                }
                listOf(adminContact) + buildStudentAndParentContacts(assignedStudents)
            }
            UserRole.STUDENT, UserRole.PARENT -> {
                val linkedStu = schoolStudents.firstOrNull { it.id == user.linkedStudentId }
                val stuClass = linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
                val stuSection = linkedStu?.sectionName?.ifBlank { user.sectionName } ?: user.sectionName
                val myClassTeachers = schoolAccounts
                    .filter { t ->
                        t.role == UserRole.TEACHER && t.canTeacherAccessClassSection(stuClass, stuSection)
                    }
                    .map { t ->
                        val spec = t.specialty.ifBlank { "معلم المادة" }
                        ChatContactItem(
                            accountId = t.id,
                            fullName = t.fullName.ifBlank { "معلم المادة" },
                            role = UserRole.TEACHER,
                            subtitle = "معلم ($spec) • $stuClass - شعبة ($stuSection)",
                            gradeClass = stuClass,
                            sectionName = stuSection,
                            phone = t.phone,
                            photoBase64 = t.photoBase64
                        )
                    }
                listOf(adminContact) + myClassTeachers
            }
        }
        return rawContacts
            .filter { it.accountId.isNotBlank() }
            .distinctBy { it.accountId }
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

    fun toggleSideDrawerEnabled() {
        val next = !_isSideDrawerEnabled.value
        _isSideDrawerEnabled.value = next
        repo.setSideDrawerEnabled(next)
        showToastAlert(
            if (next) "تم تفعيل القائمة الجانبية الشاملة بنجاح" else "تم إخفاء القائمة الجانبية والاكتفاء بالبطاقات والشريط السفلي",
            isSuccess = true
        )
    }

    private fun enforceActiveSectionFeatureVisibility(feat: SchoolFeatureSettings) {
        val curr = _activeSection.value
        if (curr == AppSection.HONOR_ROLL && !feat.honorRollEnabled) {
            _activeSection.value = AppSection.DASHBOARD
        } else if (curr == AppSection.STUDENT_ACTIVITIES && !feat.studentActivitiesEnabled) {
            _activeSection.value = AppSection.DASHBOARD
        } else if (curr == AppSection.TUITION_FEES && (_schoolConfig.value.schoolType == SchoolType.PUBLIC || !feat.tuitionEnabled)) {
            _activeSection.value = AppSection.DASHBOARD
        } else if (curr == AppSection.EVENTS_NEWS && !feat.eventsGalleryEnabled) {
            _activeSection.value = AppSection.DASHBOARD
        }
    }

    fun updateSchoolFeatureSettings(newSettings: SchoolFeatureSettings) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val prev = _featureSettings.value
        val bus = if (newSettings.busAlertsEnabled != prev.busAlertsEnabled) newSettings.busAlertsEnabled else newSettings.busTrackingEnabled
        val study = if (newSettings.studyLibraryEnabled != prev.studyLibraryEnabled) newSettings.studyLibraryEnabled else newSettings.studyMaterialsEnabled
        val qr = when {
            newSettings.qrScannerEnabled != prev.qrScannerEnabled -> newSettings.qrScannerEnabled
            newSettings.qrAttendanceEnabled != prev.qrAttendanceEnabled -> newSettings.qrAttendanceEnabled
            else -> newSettings.digitalIdEnabled
        }
        val lostAndSug = if (newSettings.lostAndFoundEnabled != prev.lostAndFoundEnabled) newSettings.lostAndFoundEnabled else newSettings.suggestionsBoxEnabled

        val normalized = newSettings.copy(
            busTrackingEnabled = bus,
            busAlertsEnabled = bus,
            studyMaterialsEnabled = study,
            studyLibraryEnabled = study,
            digitalIdEnabled = qr,
            qrScannerEnabled = qr,
            qrAttendanceEnabled = qr,
            lostAndFoundEnabled = lostAndSug,
            suggestionsBoxEnabled = lostAndSug,
            dismissalAndLostFoundEnabled = newSettings.dismissalCallEnabled || lostAndSug
        )
        val updated = _schoolConfig.value.copy(featureSettings = normalized)
        _schoolConfig.value = updated
        _featureSettings.value = normalized
        enforceActiveSectionFeatureVisibility(normalized)
        repo.saveSchoolConfig(updated)
        showToastAlert("تم تحديث مزايا وأقسام المنصة وحفظها في قاعدة بيانات المدرسة وتعميمها لجميع الحسابات فوراً", isSuccess = true)
    }

    fun navigateToSection(section: AppSection) {
        val feat = _schoolConfig.value.featureSettings
        if (section == AppSection.TUITION_FEES && (_schoolConfig.value.schoolType == SchoolType.PUBLIC || !feat.tuitionEnabled)) {
            _activeSection.value = AppSection.DASHBOARD
            return
        }
        if (section == AppSection.HONOR_ROLL && !feat.honorRollEnabled) {
            _activeSection.value = AppSection.DASHBOARD
            return
        }
        if (section == AppSection.STUDENT_ACTIVITIES && !feat.studentActivitiesEnabled) {
            _activeSection.value = AppSection.DASHBOARD
            return
        }
        if (section == AppSection.EVENTS_NEWS && !feat.eventsGalleryEnabled) {
            _activeSection.value = AppSection.DASHBOARD
            return
        }
        _activeSection.value = section
    }

    fun openUrgentSchoolAlertModal() {
        _showUrgentAlertModal.value = true
    }

    fun closeUrgentSchoolAlertModal() {
        _showUrgentAlertModal.value = false
    }

    fun clearDeepLinkTarget() {
        _deepLinkTarget.value = null
    }

    /**
     * Section 7: Smart Instant Deep-Link Navigation on Notification Click
     * Marks the notification as read and routes immediately to the exact Section, Tab, and Item.
     */
    fun openNotificationDeepLink(notif: AppNotification) {
        val sid = _schoolConfig.value.schoolId
        if (!notif.isRead) {
            val updated = _notifications.value.map {
                if (it.id == notif.id) it.copy(isRead = true) else it
            }
            _notifications.value = updated
            repo.saveNotifications(updated, sid)
        }

        val resolvedSection: AppSection = runCatching {
            if (notif.targetSection.isNotBlank()) {
                AppSection.valueOf(notif.targetSection)
            } else {
                inferSectionFromNotification(notif)
            }
        }.getOrElse { inferSectionFromNotification(notif) }

        val resolvedTab: Int = if (notif.targetSection.isNotBlank()) {
            notif.targetTab
        } else {
            inferTabFromNotification(notif, resolvedSection)
        }

        val resolvedItemId: String = notif.targetItemId.ifBlank {
            notif.targetStudentId
        }

        navigateToSection(resolvedSection)
        _deepLinkTarget.value = DeepLinkNavigationTarget(
            section = _activeSection.value,
            tabIndex = resolvedTab,
            itemId = resolvedItemId,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun inferSectionFromNotification(notif: AppNotification): AppSection {
        val titleAndBody = "${notif.title} ${notif.body}"
        return when {
            titleAndBody.contains("نشاط") || titleAndBody.contains("منشور") || titleAndBody.contains("تعليق") || titleAndBody.contains("إعجاب") || titleAndBody.contains("مخالفة") ->
                AppSection.STUDENT_ACTIVITIES
            titleAndBody.contains("رسالة جديدة") || titleAndBody.contains("محادثة") ->
                AppSection.REPORTS_CHAT
            titleAndBody.contains("صندوق المقترحات") || titleAndBody.contains("رسالة سرية") || titleAndBody.contains("مفقودات") ->
                AppSection.DASHBOARD
            notif.type == NotificationType.ATTENDANCE -> AppSection.ATTENDANCE
            notif.type == NotificationType.HOMEWORK -> AppSection.SCHEDULE_HOMEWORK
            notif.type == NotificationType.GRADES -> AppSection.EXAMS_GRADES
            notif.type == NotificationType.TUITION -> AppSection.TUITION_FEES
            notif.type == NotificationType.REPORT -> {
                if (titleAndBody.contains("تقييم يومي") || titleAndBody.contains("متابعة")) {
                    AppSection.ATTENDANCE
                } else {
                    AppSection.REPORTS_CHAT
                }
            }
            notif.type == NotificationType.GENERAL -> {
                if (titleAndBody.contains("خبر جديد") || titleAndBody.contains("فعالية")) {
                    AppSection.EVENTS_NEWS
                } else {
                    AppSection.DASHBOARD
                }
            }
            else -> AppSection.DASHBOARD
        }
    }

    private fun inferTabFromNotification(notif: AppNotification, section: AppSection): Int {
        val titleAndBody = "${notif.title} ${notif.body}"
        return when (section) {
            AppSection.REPORTS_CHAT -> if (titleAndBody.contains("رسالة جديدة") || titleAndBody.contains("محادثة")) 1 else 0
            AppSection.SCHEDULE_HOMEWORK -> when {
                titleAndBody.contains("مكتبة الملخصات") || titleAndBody.contains("ملخص") -> 2
                else -> 0
            }
            AppSection.EXAMS_GRADES -> if (titleAndBody.contains("موعد امتحان")) 1 else 0
            AppSection.STUDENT_ACTIVITIES -> if (titleAndBody.contains("بلاغ مخالفة")) 2 else 0
            else -> 0
        }
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
            repo.generateUniqueThreeDigitSchoolCode()
        }
        val prefixedAdminUsername = FirebaseConfigConstants.applySchoolPrefix(generatedSchoolId, cleanUser)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
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
            isActive = true,
            createdAt = todayDate,
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

        // Job title for administration is exclusively "المدير" with Auto School-Code Prefix
        val adminAccount = UserAccount(
            id = "admin_${generatedSchoolId.lowercase()}",
            username = prefixedAdminUsername,
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

        // Register school in global schools registry for Developer Super-Admin Panel
        repo.upsertRegisteredSchool(config, prefixedAdminUsername)
        _registeredSchools.value = repo.loadRegisteredSchools()

        // Mark activation code as used = true in school_licenses so it can never be reused
        val usedCode = _pendingActivationCode.value
        if (!usedCode.isNullOrBlank() && usedCode != "DEV-DIRECT") {
            val updatedLicenses = _schoolLicenses.value.map { lic ->
                if (lic.code.equals(usedCode, ignoreCase = true)) {
                    lic.copy(
                        used = true,
                        usedBySchoolId = generatedSchoolId,
                        usedBySchoolName = cleanName,
                        usedAt = todayDate
                    )
                } else lic
            }
            _schoolLicenses.value = updatedLicenses
            repo.saveSchoolLicenses(updatedLicenses)
        }
        _pendingActivationCode.value = null
        _isSchoolSetupUnlocked.value = false
        _isDeveloperAuthenticated.value = false

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

        _leaveRequests.value = emptyList()
        repo.saveLeaveRequests(emptyList(), generatedSchoolId)

        _studyMaterials.value = emptyList()
        repo.saveStudyMaterials(emptyList(), generatedSchoolId)

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

        showToastAlert("تم تأسيس المدرسة ($cleanName) • كود المدرسة: $generatedSchoolId • حساب المدير: $prefixedAdminUsername", isSuccess = true)
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
        repo.upsertRegisteredSchool(updated)
        _registeredSchools.value = repo.loadRegisteredSchools()
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
    // LOGIN & LOGOUT WITH DEVELOPER SUPER-ADMIN & AUTO SCHOOL-CODE PREFIX
    // =========================================================================

    /**
     * Proactively discovers and caches the real school name from `schools/{schoolId}/settings` -> `schoolName`
     * as soon as the user enters a username starting with a school code prefix (e.g. "105-1").
     */
    fun previewSchoolNameByUsername(usernameInput: String) {
        val cleanUser = InputSanitizer.sanitizeUsername(usernameInput.trim())
        val extractedPrefix = FirebaseConfigConstants.extractSchoolPrefix(cleanUser)
        if (extractedPrefix.isNullOrBlank()) {
            _discoveredSchoolName.value = null
            _loginConnectingMessage.value = null
            return
        }
        val localName = repo.resolveSchoolNameLocally(extractedPrefix)
            ?: _registeredSchools.value.firstOrNull { it.schoolId == extractedPrefix }?.schoolName
        if (!localName.isNullOrBlank()) {
            _discoveredSchoolName.value = localName
        }
        viewModelScope.launch {
            val fetchedName = repo.fetchSchoolNameFromSettings(extractedPrefix)
            if (!fetchedName.isNullOrBlank()) {
                _discoveredSchoolName.value = fetchedName
            }
        }
    }

    fun login(usernameInput: String, passwordInput: String): Result<UserAccount> {
        val nowMs = System.currentTimeMillis()
        val lockUntil = _loginLockoutUntilMs.value
        if (lockUntil > nowMs) {
            val remainingFormatted = com.example.util.SecurityShieldHelper.formatRemainingLockout(lockUntil - nowMs)
            val lockMsg = "الدرع الأمني: تم قفل المحاولات مؤقتاً لمدة 5 دقائق بسبب 5 محاولات خاطئة. المتبقي: ($remainingFormatted)"
            showToastAlert(lockMsg, isSuccess = false)
            return Result.failure(IllegalStateException(lockMsg))
        }

        val rawUser = usernameInput.trim()
        val rawPass = passwordInput.trim()
        if (rawUser.isEmpty() || rawPass.isEmpty()) {
            showToastAlert("يرجى إدخال اسم المستخدم وكلمة المرور", isSuccess = false)
            return Result.failure(IllegalArgumentException("يرجى إدخال اسم المستخدم وكلمة المرور"))
        }

        // 1. Check Developer Super-Admin Credentials using SHA-256 verification ("ABU-RASLAN" / "07512362567")
        if (com.example.util.SecurityShieldHelper.verifyDeveloperCredentials(rawUser, rawPass)) {
            _failedLoginAttempts.value = 0
            _loginLockoutUntilMs.value = 0L
            _isDeveloperAuthenticated.value = true
            _isSchoolSetupUnlocked.value = false
            _loginConnectingMessage.value = null
            refreshDeveloperCloudData()
            showToastAlert("مرحباً بك في لوحة تحكم المطور - أحمد أبو رسلان", isSuccess = true)
            val devDummy = UserAccount(
                id = "dev_super_admin",
                username = FirebaseConfigConstants.DEV_USERNAME,
                password = "",
                fullName = "المطور أحمد أبو رسلان",
                role = UserRole.ADMIN,
                isActive = true
            )
            return Result.success(devDummy)
        }

        val cleanUser = InputSanitizer.sanitizeUsername(rawUser)
        val cleanPass = InputSanitizer.sanitize(rawPass)

        // 2. Extract school code prefix (e.g. "105" from "105-1" or "105-ali")
        val extractedPrefix = FirebaseConfigConstants.extractSchoolPrefix(cleanUser)
        if (!extractedPrefix.isNullOrBlank()) {
            val knownSchoolName = repo.resolveSchoolNameLocally(extractedPrefix)
                ?: _registeredSchools.value.firstOrNull { it.schoolId == extractedPrefix }?.schoolName
                ?: _discoveredSchoolName.value

            // Check if this school is already cached locally; if not, switch to it
            val localAccountsForSchool = repo.loadAccounts(extractedPrefix)
            if (localAccountsForSchool.isNotEmpty() && _schoolConfig.value.schoolId != extractedPrefix) {
                reloadSchoolDataIntoMemory(extractedPrefix)
            } else if (localAccountsForSchool.isEmpty()) {
                // Attempt immediate cloud fetch for cross-device login (e.g. logging into 105-1 on a new phone)
                val initialConnectingText = if (!knownSchoolName.isNullOrBlank()) {
                    "جاري الاتصال بـ $knownSchoolName..."
                } else {
                    "جاري الاتصال بالمدرسة وجلب البيانات..."
                }
                _loginConnectingMessage.value = initialConnectingText

                viewModelScope.launch {
                    val cloudSchoolName = repo.fetchSchoolNameFromSettings(extractedPrefix) ?: knownSchoolName
                    val displaySchoolName = cloudSchoolName?.takeIf { it.isNotBlank() } ?: "منصة المدرسة"
                    _discoveredSchoolName.value = cloudSchoolName
                    val liveMessage = "جاري الاتصال بـ $displaySchoolName..."
                    _loginConnectingMessage.value = liveMessage
                    showToastAlert(liveMessage, isSuccess = true)

                    refreshDeveloperCloudData()
                    val pulled = repo.syncFullSchoolFromCloud(extractedPrefix)
                    _loginConnectingMessage.value = null
                    if (pulled) {
                        reloadSchoolDataIntoMemory(extractedPrefix)
                        completeMemberLogin(cleanUser, cleanPass, extractedPrefix)
                    } else {
                        registerFailedLoginAttempt()
                        showToastAlert("بيانات الدخول غير صحيحة أو لم يتم العثور على المدرسة المطلوبة", isSuccess = false)
                    }
                }
                return Result.failure(IllegalStateException(initialConnectingText))
            }
        }

        _loginConnectingMessage.value = null
        return completeMemberLogin(cleanUser, cleanPass, extractedPrefix ?: _schoolConfig.value.schoolId)
    }

    private fun registerFailedLoginAttempt() {
        val nextCount = _failedLoginAttempts.value + 1
        _failedLoginAttempts.value = nextCount
        if (nextCount >= com.example.util.SecurityShieldHelper.MAX_FAILED_ATTEMPTS) {
            _loginLockoutUntilMs.value = System.currentTimeMillis() + com.example.util.SecurityShieldHelper.LOCKOUT_DURATION_MS
            _failedLoginAttempts.value = 0
        }
    }

    private fun completeMemberLogin(
        cleanUser: String,
        cleanPass: String,
        targetSchoolId: String
    ): Result<UserAccount> {
        val currentSchoolId = targetSchoolId.ifBlank { _schoolConfig.value.schoolId }

        val matched = _accounts.value.firstOrNull { acc ->
            val exactMatch = acc.username.equals(cleanUser, ignoreCase = true)
            val prefixedCandidate = if (currentSchoolId.isNotBlank()) {
                FirebaseConfigConstants.applySchoolPrefix(currentSchoolId, cleanUser)
            } else cleanUser
            val prefixedMatch = acc.username.equals(prefixedCandidate, ignoreCase = true)

            (exactMatch || prefixedMatch) &&
                com.example.util.SecurityShieldHelper.verifyPassword(cleanPass, acc.password) &&
                (currentSchoolId.isEmpty() || acc.schoolId.isEmpty() || acc.schoolId == currentSchoolId)
        } ?: run {
            registerFailedLoginAttempt()
            val lockUntil = _loginLockoutUntilMs.value
            val errMsg = if (lockUntil > System.currentTimeMillis()) {
                "تم حظر المحاولات لمدة 5 دقائق بعد 5 محاولات خاطئة متتالية حفاظاً على أمان الحسابات"
            } else {
                val rem = (com.example.util.SecurityShieldHelper.MAX_FAILED_ATTEMPTS - _failedLoginAttempts.value).coerceAtLeast(1)
                "بيانات الدخول غير صحيحة، تأكد من اسم المستخدم وكلمة المرور (المحاولات المتبقية: $rem)"
            }
            showToastAlert(errMsg, isSuccess = false)
            return Result.failure(IllegalArgumentException(errMsg))
        }

        _failedLoginAttempts.value = 0
        _loginLockoutUntilMs.value = 0L

        val resolvedSchoolId = matched.schoolId.ifBlank { currentSchoolId }

        // Check if the school subscription is active (isActive)
        if (!isSchoolSubscriptionActive(resolvedSchoolId)) {
            showToastAlert("اشتراك المدرسة متوقف، يرجى مراجعة المطور أحمد أبو رسلان", isSuccess = false)
            return Result.failure(IllegalStateException("اشتراك المدرسة متوقف، يرجى مراجعة المطور أحمد أبو رسلان"))
        }

        if (!matched.isActive) {
            showToastAlert("هذا الحساب موقوف حالياً من قبل إدارة المدرسة", isSuccess = false)
            return Result.failure(IllegalStateException("هذا الحساب موقوف حالياً من قبل إدارة المدرسة"))
        }

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
        _selectedParentChildId.value = matched.linkedStudentId
        _isDeveloperAuthenticated.value = false
        _isSchoolSetupUnlocked.value = false
        _activeSection.value = AppSection.DASHBOARD

        showToastAlert("مرحباً بك ${matched.fullName} (${matched.role.arabicTitle})", isSuccess = true)
        return Result.success(matched)
    }

    fun logout() {
        repo.clearSession()
        _activeSession.value = null
        _currentUser.value = null
        _selectedParentChildId.value = ""
        _isDeveloperAuthenticated.value = false
        _isSchoolSetupUnlocked.value = false
        _activeSection.value = AppSection.DASHBOARD
        showToastAlert("تم تسجيل الخروج من الجلسة بأمان", isSuccess = true)
    }

    // =========================================================================
    // ADMIN ACCOUNT MANAGEMENT (Teachers, Parents & Students) WITH AUTO SCHOOL-CODE PREFIX
    // =========================================================================

    fun addTeacherAccount(
        fullName: String,
        specialty: String,
        assignedClasses: String,
        phone: String,
        username: String,
        password: String,
        photoBase64: String = ""
    ): UserAccount? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val cleanName = InputSanitizer.sanitize(fullName)
        if (cleanName.isBlank()) {
            showToastAlert("يرجى إدخال اسم المعلم الثلاثي", isSuccess = false)
            return null
        }

        val sid = _schoolConfig.value.schoolId
        val rawUsername = InputSanitizer.sanitizeUsername(username).ifBlank { "tch${(10..99).random()}" }
        val cleanUsername = FirebaseConfigConstants.applySchoolPrefix(sid, rawUsername)
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
            photoBase64 = photoBase64,
            schoolId = sid
        )
        val updated = listOf(newTeacher) + _accounts.value
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        showToastAlert("تم إنشاء حساب المعلم ($cleanName) باسم مستخدم ($cleanUsername)", isSuccess = true)
        return newTeacher
    }

    fun updateTeacherAccount(
        teacherId: String,
        fullName: String,
        specialty: String,
        assignedClasses: String,
        phone: String,
        username: String,
        password: String,
        photoBase64: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val rawUser = InputSanitizer.sanitizeUsername(username)
        val prefixedUser = if (rawUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawUser) else ""
        val updated = _accounts.value.map { acc ->
            if (acc.id == teacherId && acc.role == UserRole.TEACHER) {
                acc.copy(
                    fullName = InputSanitizer.sanitize(fullName).ifBlank { acc.fullName },
                    specialty = InputSanitizer.sanitize(specialty),
                    assignedClasses = InputSanitizer.sanitize(assignedClasses),
                    phone = InputSanitizer.sanitize(phone).ifBlank { acc.phone },
                    username = prefixedUser.ifBlank { acc.username },
                    password = InputSanitizer.sanitize(password).ifBlank { acc.password },
                    photoBase64 = photoBase64.ifBlank { acc.photoBase64 },
                    schoolId = sid
                )
            } else acc
        }
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        _currentUser.value?.let { curr ->
            if (curr.id == teacherId) {
                _currentUser.value = updated.firstOrNull { it.id == teacherId } ?: curr
            }
        }
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

    /**
     * Deliverable 1: Smart Parent Auto-Link (الاكتشاف التلقائي لولي الأمر المسجل مسبقاً وربط الإخوة)
     * Searches existing students in the same school as the admin types the student's name, father's name, or phone.
     */
    fun findExistingParentMatch(
        studentNameInput: String,
        parentNameInput: String,
        parentPhoneInput: String,
        excludeStudentId: String? = null
    ): SiblingParentMatchInfo? {
        val sid = _schoolConfig.value.schoolId
        val schoolStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) && it.id != excludeStudentId
        }
        if (schoolStudents.isEmpty()) return null

        val cleanParentInput = parentNameInput
            .replace("ولي أمر الطالب", "")
            .replace("ولي أمر", "")
            .replace("السيد", "")
            .trim()
        val cleanPhoneInput = parentPhoneInput.trim()

        // Extract probable father name from studentNameInput if it has 2+ words (e.g., "محمد أحمد" -> "أحمد")
        val studentWords = studentNameInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val inferredFatherFromStudent = if (studentWords.size >= 2) {
            studentWords.drop(1).joinToString(" ").trim()
        } else ""

        val matchedSiblings = schoolStudents.filter { existing ->
            val existingFatherClean = existing.parentName
                .replace("ولي أمر الطالب", "")
                .replace("ولي أمر", "")
                .replace("السيد", "")
                .trim()

            val phoneMatches = cleanPhoneInput.length >= 10 &&
                cleanPhoneInput != "07512362567" &&
                existing.parentPhone.trim() == cleanPhoneInput

            val parentNameMatches = cleanParentInput.length >= 3 &&
                existingFatherClean.isNotEmpty() &&
                (existingFatherClean.equals(cleanParentInput, ignoreCase = true) ||
                    existingFatherClean.contains(cleanParentInput, ignoreCase = true) ||
                    cleanParentInput.contains(existingFatherClean, ignoreCase = true))

            val inferredFatherMatches = inferredFatherFromStudent.length >= 3 &&
                existingFatherClean.isNotEmpty() &&
                (existingFatherClean.equals(inferredFatherMatchesOr(inferredFatherFromStudent), ignoreCase = true) ||
                    existingFatherClean.contains(inferredFatherFromStudent, ignoreCase = true))

            phoneMatches || parentNameMatches || inferredFatherMatches
        }

        val primary = matchedSiblings.firstOrNull() ?: return null
        val allSameParent = schoolStudents.filter {
            it.parentUsername.equals(primary.parentUsername, ignoreCase = true) ||
                (primary.parentName.isNotBlank() && it.parentName.equals(primary.parentName, ignoreCase = true))
        }.ifEmpty { listOf(primary) }

        return SiblingParentMatchInfo(
            parentName = primary.parentName,
            parentPhone = primary.parentPhone,
            parentUsername = primary.parentUsername,
            parentPassword = primary.parentPassword,
            existingChildrenNames = allSameParent.map { "${it.studentName} (${it.gradeClass})" }.distinct(),
            address = primary.address
        )
    }

    private fun inferredFatherMatchesOr(s: String): String = s

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
        val rawSUser = InputSanitizer.sanitizeUsername(studentUsername).ifBlank { "stu${(10..99).random()}" }
        val sUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawSUser)
        val sPass = InputSanitizer.sanitize(studentPassword).ifBlank { (1000..9999).random().toString() }
        val rawPUser = InputSanitizer.sanitizeUsername(parentUsername).ifBlank { "par${(10..99).random()}" }
        val pUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawPUser)
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

        // Reuse existing parent account if linking siblings to the same parent username!
        val existingParentAcc = _accounts.value.firstOrNull {
            it.role == UserRole.PARENT &&
                it.username.equals(pUser, ignoreCase = true) &&
                (it.schoolId.isEmpty() || it.schoolId == sid)
        }

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

        val updatedAccounts = if (existingParentAcc != null) {
            listOf(studentAcc) + _accounts.value
        } else {
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
                photoBase64 = photoBase64,
                schoolId = sid
            )
            listOf(parentAcc, studentAcc) + _accounts.value
        }
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

        val msg = if (existingParentAcc != null) {
            "تمت إضافة الطالب ($cleanStuName) وربطه تلقائياً بحساب ولي الأمر الحالي ($pUser) مع إخوته بنجاح"
        } else {
            "تمت إضافة الطالب ($cleanStuName) • المستخدم: ($sUser) وولي الأمر: ($pUser)"
        }
        showToastAlert(msg, isSuccess = true)
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
        parentPassword: String,
        photoBase64: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val rawStuUser = InputSanitizer.sanitizeUsername(studentUsername)
        val prefixedStuUser = if (rawStuUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawStuUser) else ""
        val rawParUser = InputSanitizer.sanitizeUsername(parentUsername)
        val prefixedParUser = if (rawParUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawParUser) else ""

        val updatedStudents = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    studentName = InputSanitizer.sanitize(studentName).ifBlank { s.studentName },
                    gradeClass = gradeClass,
                    sectionName = sectionName,
                    parentName = InputSanitizer.sanitize(parentName).ifBlank { s.parentName },
                    parentPhone = InputSanitizer.sanitize(parentPhone).ifBlank { s.parentPhone },
                    address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                    studentUsername = prefixedStuUser.ifBlank { s.studentUsername },
                    studentPassword = InputSanitizer.sanitize(studentPassword).ifBlank { s.studentPassword },
                    parentUsername = prefixedParUser.ifBlank { s.parentUsername },
                    parentPassword = InputSanitizer.sanitize(parentPassword).ifBlank { s.parentPassword },
                    photoBase64 = photoBase64.ifBlank { s.photoBase64 },
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
                        username = prefixedParUser.ifBlank { acc.username },
                        password = InputSanitizer.sanitize(parentPassword).ifBlank { acc.password },
                        phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                        gradeClass = gradeClass,
                        sectionName = sectionName,
                        address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                        photoBase64 = photoBase64.ifBlank { acc.photoBase64 },
                        schoolId = sid
                    )
                } else {
                    acc.copy(
                        fullName = InputSanitizer.sanitize(studentName).ifBlank { acc.fullName },
                        username = prefixedStuUser.ifBlank { acc.username },
                        password = InputSanitizer.sanitize(studentPassword).ifBlank { acc.password },
                        phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                        gradeClass = gradeClass,
                        sectionName = sectionName,
                        address = InputSanitizer.sanitize(address).ifBlank { "الموصل - زمار" },
                        photoBase64 = photoBase64.ifBlank { acc.photoBase64 },
                        schoolId = sid
                    )
                }
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)
        _currentUser.value?.let { curr ->
            if (curr.linkedStudentId == studentId) {
                _currentUser.value = updatedAccounts.firstOrNull { it.id == curr.id } ?: curr
            }
        }
        showToastAlert("تم تحديث بيانات وصورة الطالب وولي الأمر بنجاح", isSuccess = true)
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
        targetStudentId: String = "",
        targetUserId: String = "",
        targetGradeClass: String = "",
        targetSectionName: String = "",
        targetRole: String = "",
        targetSection: String = "",
        targetTab: Int = 0,
        targetItemId: String = ""
    ) {
        val sid = _schoolConfig.value.schoolId
        val senderId = _currentUser.value?.id.orEmpty()
        val timeLabel = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val tempNotif = AppNotification(
            id = "temp",
            title = title,
            body = body,
            type = type,
            createdAt = ""
        )
        val resolvedSectionName = targetSection.ifBlank {
            inferSectionFromNotification(tempNotif).name
        }
        val resolvedTabIndex = if (targetSection.isNotBlank()) {
            targetTab
        } else {
            val secEnum = runCatching { AppSection.valueOf(resolvedSectionName) }.getOrDefault(AppSection.DASHBOARD)
            inferTabFromNotification(tempNotif, secEnum)
        }
        val notif = AppNotification(
            id = "notif_${UUID.randomUUID().toString().take(6)}",
            title = InputSanitizer.sanitize(title),
            body = InputSanitizer.sanitize(body),
            type = type,
            targetStudentId = targetStudentId,
            targetUserId = targetUserId,
            targetGradeClass = targetGradeClass,
            targetSectionName = targetSectionName,
            targetRole = targetRole,
            senderUserId = senderId,
            targetSection = resolvedSectionName,
            targetTab = resolvedTabIndex,
            targetItemId = targetItemId,
            createdAt = "الآن ($timeLabel)",
            isRead = false,
            schoolId = sid
        )
        val updated = listOf(notif) + _notifications.value
        _notifications.value = updated
        repo.saveNotifications(updated, sid)

        // Only display banner & system push notification on this device if the logged-in user IS the target recipient
        val currUser = _currentUser.value
        if (currUser != null) {
            val linkedStu = _students.value.firstOrNull { it.id == currUser.linkedStudentId }
            val parentChildIds = getChildrenForParent(currUser).map { it.id }.toSet()
            if (notif.isTargetedForUser(currUser, linkedStu, parentChildIds)) {
                _latestBannerAlert.value = notif
                SystemNotificationHelper.showSystemPushNotification(
                    context = appContext,
                    title = notif.title,
                    body = notif.body,
                    tagId = notif.id
                )
            }
        }
    }

    fun markAllNotificationsRead() {
        val sid = _schoolConfig.value.schoolId
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        repo.saveNotifications(updated, sid)
    }

    fun deleteNotification(notificationId: String) {
        val sid = _schoolConfig.value.schoolId
        val updated = _notifications.value.filterNot { it.id == notificationId }
        _notifications.value = updated
        repo.saveNotifications(updated, sid)
    }

    // =========================================================================
    // SECTION 2: DAILY ATTENDANCE, QR ATTENDANCE, LEAVE REQUESTS & BUS TRACKING
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

    /**
     * Scans a Student ID QR Code (or username/ID), immediately marks the student PRESENT ("حاضر")
     * for the given date, and sends an instant notification exclusively to the student's parent:
     * "وصل ابنكم [اسم الطالب] إلى المدرسة الآن".
     */
    fun recordAttendanceByQrCode(qrOrUsername: String, date: String): Boolean {
        return recordAttendanceByQrCodeDetailed(qrOrUsername, date) != null
    }

    /**
     * Scans a Student ID QR Code (MADRASATI|schoolId|studentId|studentName|gradeClass or legacy MADRASATI-STU-...),
     * plays a beep + tactile vibration, marks the student PRESENT ("حاضر") in Firebase,
     * sends an instant arrival notification to the parent, and returns the matched StudentRecord.
     */
    fun recordAttendanceByQrCodeDetailed(qrOrUsername: String, date: String): StudentRecord? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return null
        val cleanInput = qrOrUsername.trim()
        if (cleanInput.isEmpty()) {
            showToastAlert("يرجى مسح أو إدخال رمز الـ QR الخاص بالطالب", isSuccess = false)
            return null
        }

        val sid = _schoolConfig.value.schoolId
        val schoolStudents = _students.value.filter { it.schoolId.isEmpty() || it.schoolId == sid }
        val pipeParts = cleanInput.split("|").map { it.trim() }
        val extractedStuIdFromPipe = if (pipeParts.size >= 3 && pipeParts[0].equals("MADRASATI", ignoreCase = true)) {
            pipeParts[2]
        } else ""

        val matchedStudent = schoolStudents.firstOrNull { stu ->
            stu.qrCodeValue.equals(cleanInput, ignoreCase = true) ||
                (extractedStuIdFromPipe.isNotEmpty() && stu.id.equals(extractedStuIdFromPipe, ignoreCase = true)) ||
                cleanInput.contains("|${stu.id}|", ignoreCase = true) ||
                stu.id.equals(cleanInput, ignoreCase = true) ||
                stu.studentUsername.equals(cleanInput, ignoreCase = true) ||
                cleanInput.endsWith("-${stu.id}", ignoreCase = true) ||
                stu.studentName.equals(cleanInput, ignoreCase = true) ||
                stu.fullNameWithFather.equals(cleanInput, ignoreCase = true)
        }

        if (matchedStudent == null) {
            showToastAlert("لم يتم العثور على طالب مطابق لرمز الـ QR ($cleanInput)", isSuccess = false)
            return null
        }

        com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(appContext)

        val timeNow = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val existingIndex = _attendance.value.indexOfFirst {
            it.studentId == matchedStudent.id && it.date == date
        }
        val entry = AttendanceEntry(
            id = if (existingIndex >= 0) _attendance.value[existingIndex].id else "att_${UUID.randomUUID().toString().take(6)}",
            studentId = matchedStudent.id,
            studentName = matchedStudent.studentName,
            gradeClass = matchedStudent.gradeClass,
            sectionName = matchedStudent.sectionName,
            date = InputSanitizer.sanitize(date),
            status = AttendanceStatus.PRESENT,
            note = "حضور بالـ QR ($timeNow)",
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

        val arrivalMsg = "وصل ابنكم ${matchedStudent.fullNameWithFather} (${matchedStudent.gradeClass} - شعبة ${matchedStudent.sectionName}) إلى المدرسة الآن الساعة $timeNow"
        showToastAlert("تم التعرف على الطالب (${matchedStudent.fullNameWithFather}) وتسجيل حضوره بالـ QR وإبلاغ ولي أمره فوراً", isSuccess = true)

        pushNotification(
            title = "بوابة الحضور الذكي QR: ${matchedStudent.fullNameWithFather}",
            body = arrivalMsg,
            type = NotificationType.ATTENDANCE,
            targetStudentId = matchedStudent.id,
            targetRole = UserRole.PARENT.name
        )
        return matchedStudent
    }

    /**
     * Parent submits an electronic leave request (طلب إجازة للطالب: مرضية / زمنية / عذر عائلي).
     */
    fun submitLeaveRequest(
        student: StudentRecord,
        leaveType: LeaveType,
        leaveDate: String,
        durationDays: Int,
        reason: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.PARENT, UserRole.STUDENT, UserRole.ADMIN))) return
        val cleanReason = InputSanitizer.sanitize(reason)
        if (cleanReason.isBlank()) {
            showToastAlert("يرجى كتابة سبب طلب الإجازة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val cleanDate = InputSanitizer.sanitize(leaveDate).ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val item = LeaveRequestItem(
            id = "lv_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.fullNameWithFather,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            parentName = student.parentName,
            parentPhone = student.parentPhone,
            leaveType = leaveType,
            leaveDate = cleanDate,
            durationDays = durationDays.coerceIn(1, 30),
            reason = cleanReason,
            status = LeaveRequestStatus.PENDING,
            createdAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()),
            schoolId = sid
        )
        val updated = listOf(item) + _leaveRequests.value
        _leaveRequests.value = updated
        repo.saveLeaveRequests(updated, sid)

        showToastAlert("تم إرسال طلب الإجازة (${leaveType.arabicLabel}) إلى إدارة المدرسة بنجاح", isSuccess = true)

        pushNotification(
            title = "طلب إجازة جديد: ${student.fullNameWithFather}",
            body = "قدم ولي أمر الطالب (${student.fullNameWithFather}) طلب ${leaveType.arabicLabel} ليوم $cleanDate (${item.durationDays} يوم) — السبب: $cleanReason",
            type = NotificationType.ATTENDANCE,
            targetRole = UserRole.ADMIN.name
        )
    }

    /**
     * Admin approves or rejects a parent's leave request.
     * Upon approval, the student's attendance for that date is automatically recorded as EXCUSED ("مجاز (غائب بعذر)").
     */
    fun respondToLeaveRequest(
        requestId: String,
        approve: Boolean,
        adminNote: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val targetReq = _leaveRequests.value.firstOrNull { it.id == requestId } ?: return
        val nextStatus = if (approve) LeaveRequestStatus.APPROVED else LeaveRequestStatus.REJECTED

        val updatedLeaves = _leaveRequests.value.map { req ->
            if (req.id == requestId) {
                req.copy(
                    status = nextStatus,
                    adminResponseNote = InputSanitizer.sanitize(adminNote).ifBlank { nextStatus.arabicLabel }
                )
            } else req
        }
        _leaveRequests.value = updatedLeaves
        repo.saveLeaveRequests(updatedLeaves, sid)

        if (approve) {
            val student = _students.value.firstOrNull { it.id == targetReq.studentId }
            val existingIndex = _attendance.value.indexOfFirst {
                it.studentId == targetReq.studentId && it.date == targetReq.leaveDate
            }
            val entry = AttendanceEntry(
                id = if (existingIndex >= 0) _attendance.value[existingIndex].id else "att_${UUID.randomUUID().toString().take(6)}",
                studentId = targetReq.studentId,
                studentName = student?.studentName ?: targetReq.studentName,
                gradeClass = targetReq.gradeClass,
                sectionName = targetReq.sectionName,
                date = targetReq.leaveDate,
                status = AttendanceStatus.EXCUSED,
                note = "مجاز بعذر رسمي (${targetReq.leaveType.arabicLabel}): ${targetReq.reason}",
                schoolId = sid
            )
            val updatedAtt = _attendance.value.toMutableList()
            if (existingIndex >= 0) {
                updatedAtt[existingIndex] = entry
            } else {
                updatedAtt.add(0, entry)
            }
            _attendance.value = updatedAtt
            repo.saveAttendance(updatedAtt, sid)
        }

        val statusAction = if (approve) "تمت الموافقة على" else "تم رفض"
        showToastAlert("$statusAction طلب إجازة الطالب (${targetReq.studentName})", isSuccess = approve)

        pushNotification(
            title = "$statusAction طلب الإجازة: ${targetReq.studentName}",
            body = if (approve) {
                "وافقت إدارة المدرسة على طلب (${targetReq.leaveType.arabicLabel}) للطالب ${targetReq.studentName} بتاريخ ${targetReq.leaveDate} وتم تسجيله (مجاز بعذر)."
            } else {
                "اعتذرت إدارة المدرسة عن قبول طلب الإجازة للطالب ${targetReq.studentName} لتاريخ ${targetReq.leaveDate}."
            },
            type = NotificationType.ATTENDANCE,
            targetStudentId = targetReq.studentId,
            targetRole = UserRole.PARENT.name
        )
    }

    /**
     * Toggles whether a student is subscribed to the school bus (`isBusSubscribed`).
     */
    fun toggleStudentBusSubscription(studentId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        var targetName = ""
        var nextSubscribed = false
        val updated = _students.value.map { s ->
            if (s.id == studentId) {
                nextSubscribed = !s.isBusSubscribed
                targetName = s.fullNameWithFather
                s.copy(isBusSubscribed = nextSubscribed)
            } else s
        }
        _students.value = updated
        repo.saveStudents(updated, sid)
        val statusMsg = if (nextSubscribed) "تم تفعيل اشتراك الخط المدرسي" else "تم إلغاء اشتراك الخط المدرسي"
        showToastAlert("$statusMsg للطالب ($targetName)", isSuccess = true)
    }

    /**
     * Sends an instant School Bus & Transport notification (`bus_tracking`)
     * EXCLUSIVELY to parents of students subscribed to the school bus (`isBusSubscribed == true`).
     */
    fun sendBusTrackingAlert(eventType: BusEventType) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val subscribedStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) && it.isBusSubscribed && it.isActive
        }
        if (subscribedStudents.isEmpty()) {
            showToastAlert("لا يوجد طلاب مشتركين في الخط المدرسي حالياً (يمكنك تفعيل اشتراك الخط للطالب من إدارة الحسابات)", isSuccess = false)
            return
        }

        subscribedStudents.forEach { stu ->
            val body = String.format(Locale("ar"), eventType.notificationBodyTemplate, stu.fullNameWithFather)
            pushNotification(
                title = eventType.notificationTitle,
                body = body,
                type = NotificationType.ATTENDANCE,
                targetStudentId = stu.id,
                targetRole = UserRole.PARENT.name
            )
        }

        // Also record latest bus event in schools/{schoolId}/bus_tracking
        val payload = org.json.JSONObject().apply {
            put("eventType", eventType.name)
            put("arabicLabel", eventType.arabicLabel)
            put("notifiedStudentsCount", subscribedStudents.size)
            put("timestamp", SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()))
        }.toString()
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_BUS_TRACKING, payload)

        showToastAlert(
            "تم إرسال إشعار (${eventType.arabicLabel}) حصرياً لأولياء أمور (${subscribedStudents.size}) طالب مشترك في الخط",
            isSuccess = true
        )
    }

    fun showBannerMessage(message: String) {
        showToastAlert(message, isSuccess = true)
    }

    fun showBannerMessage(result: Result<String>) {
        result.fold(
            onSuccess = { showToastAlert(it, isSuccess = true) },
            onFailure = { showToastAlert(it.message ?: "تعذر إتمام العملية", isSuccess = false) }
        )
    }


    /**
     * Computes the Automatic Motivational Badges for a student:
     * 1. "وسام الالتزام الذهبي": 100% attendance (0 ABSENT records) during the current month.
     * 2. "وسام نجم المادة": Full score (10/10 or 100/100) in any subject exam.
     */
    fun getStudentAutoBadges(studentId: String): List<StudentAutoBadge> {
        val badges = mutableListOf<StudentAutoBadge>()
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

        val studentAtt = _attendance.value.filter {
            it.studentId == studentId && (it.date.startsWith(currentMonthPrefix) || it.date.isNotBlank())
        }
        val absentCount = studentAtt.count { it.status == AttendanceStatus.ABSENT }
        if (absentCount == 0) {
            badges.add(
                StudentAutoBadge(
                    id = "gold_attendance_$studentId",
                    title = "وسام الالتزام الذهبي",
                    subtitle = "حضور 100% بدون غياب",
                    isGoldAttendance = true
                )
            )
        }

        val studentGrades = _grades.value.filter { it.studentId == studentId }
        val fullMarkSubjects = studentGrades.filter { g ->
            val max = g.maxScoreScale
            g.finalExamScore == max ||
                g.midYearScore == max ||
                g.month1Score == max ||
                g.month2Score == max ||
                g.finalAverage == max
        }.map { it.subjectName }.distinct()

        fullMarkSubjects.forEach { subj ->
            badges.add(
                StudentAutoBadge(
                    id = "star_${studentId}_$subj",
                    title = "وسام نجم المادة",
                    subtitle = subj,
                    isGoldAttendance = false
                )
            )
        }

        return badges
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
            type = NotificationType.HOMEWORK,
            targetGradeClass = gradeClass,
            targetSectionName = sectionName,
            targetSection = AppSection.SCHEDULE_HOMEWORK.name,
            targetTab = 1,
            targetItemId = hw.id
        )
    }

    // =========================================================================
    // SECTION 4: EXAMS & FLEXIBLE GRADING (10 OR 100 + EXTRA DAILY & MONTHLY)
    // =========================================================================

    fun addExamSchedule(
        gradeClass: String,
        subjectName: String,
        examType: String,
        examDate: String,
        dayName: String,
        timeSlot: String = "",
        topicsIncluded: String,
        hallNumber: String = ""
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
            type = NotificationType.GRADES,
            targetGradeClass = gradeClass,
            targetSection = AppSection.EXAMS_GRADES.name,
            targetTab = 1,
            targetItemId = item.id
        )
    }

    fun updateExamSchedule(
        examId: String,
        gradeClass: String,
        subjectName: String,
        examType: String,
        examDate: String,
        dayName: String,
        topicsIncluded: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _examSchedules.value.map { ex ->
            if (ex.id == examId) {
                ex.copy(
                    gradeClass = gradeClass,
                    subjectName = InputSanitizer.sanitize(subjectName),
                    examType = InputSanitizer.sanitize(examType),
                    examDate = InputSanitizer.sanitize(examDate),
                    dayName = InputSanitizer.sanitize(dayName),
                    topicsIncluded = InputSanitizer.sanitize(topicsIncluded)
                )
            } else ex
        }
        _examSchedules.value = updated
        repo.saveExamSchedules(updated, sid)
        showToastAlert("تم تعديل موعد امتحان ($subjectName) وحفظه بنجاح", isSuccess = true)
    }

    fun deleteExamSchedule(examId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _examSchedules.value.filterNot { it.id == examId }
        _examSchedules.value = updated
        repo.saveExamSchedules(updated, sid)
        showToastAlert("تم حذف موعد الامتحان من الجدول نهائياً", isSuccess = true)
    }

    fun saveOrUpdateSubjectGrade(
        student: StudentRecord,
        subjectName: String,
        dailyScore: Int,
        month1Score: Int,
        month2Score: Int,
        midYearScore: Int,
        finalExamScore: Int,
        maxScoreScale: Int = GradingRules.defaultMaxScoreForClass(student.gradeClass),
        extraDailyScores: List<Int> = emptyList(),
        extraMonthlyScores: List<Int> = emptyList()
    ): Boolean {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return false
        val validScale = if (maxScoreScale == 10) 10 else 100
        val scores = listOf(dailyScore, month1Score, month2Score, midYearScore, finalExamScore) + extraDailyScores + extraMonthlyScores
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
            extraDailyScores = extraDailyScores.map { it.coerceIn(0, validScale) },
            extraMonthlyScores = extraMonthlyScores.map { it.coerceIn(0, validScale) },
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
            targetStudentId = student.id,
            targetSection = AppSection.EXAMS_GRADES.name,
            targetTab = 0,
            targetItemId = grade.id
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

    /**
     * Interactive Heart Reaction on the Golden Honor Roll.
     * Restricted to ONE reaction per user (Toggle Like/Unlike via `likedBy` array containing `currentUser.id`),
     * synced immediately in real time to schools/{schoolId}/honor_roll in Firestore.
     */
    fun reactWithHeartToHonorStudent(studentId: String) {
        if (!validateSessionAndSchoolAccess()) return
        val sid = _schoolConfig.value.schoolId
        val userId = _currentUser.value?.id.orEmpty()
        if (userId.isEmpty()) return

        val updated = _students.value.map { s ->
            if (s.id == studentId) {
                val alreadyLiked = userId in s.likedByUserIds
                val updatedLikers = if (alreadyLiked) {
                    s.likedByUserIds.filterNot { it == userId }
                } else {
                    (s.likedByUserIds + userId).distinct()
                }
                s.copy(
                    heartCount = updatedLikers.size,
                    likedByUserIds = updatedLikers
                )
            } else s
        }
        _students.value = updated
        repo.saveStudents(updated, sid)
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

    fun updateParentReport(
        reportId: String,
        reportType: ReportType,
        title: String,
        details: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى كتابة عنوان البلاغ الرسمي", isSuccess = false)
            return
        }
        val cleanDetails = InputSanitizer.sanitize(details).ifBlank { cleanTitle }
        val sid = _schoolConfig.value.schoolId
        val updated = _reports.value.map { rep ->
            if (rep.id == reportId) {
                rep.copy(
                    reportType = reportType,
                    title = cleanTitle,
                    details = cleanDetails
                )
            } else rep
        }
        _reports.value = updated
        repo.saveReports(updated, sid)
        showToastAlert("تم تحديث البلاغ الرسمي وحفظه في السحابة بنجاح", isSuccess = true)
    }

    fun deleteParentReport(reportId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _reports.value.filterNot { it.id == reportId }
        _reports.value = updated
        repo.saveReports(updated, sid)
        showToastAlert("تم حذف البلاغ الرسمي نهائياً", isSuccess = true)
    }

    fun sendDirectChatMessage(
        contact: ChatContactItem,
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
        val relatedStudentId = contact.linkedStudentId.ifBlank { user.linkedStudentId }
        val relatedStudentName = contact.linkedStudentName.ifBlank {
            _students.value.firstOrNull { it.id == relatedStudentId }?.studentName ?: contact.fullName
        }

        val msg = ChatMessage(
            id = "msg_${UUID.randomUUID().toString().take(6)}",
            studentId = relatedStudentId,
            studentName = relatedStudentName,
            senderId = user.id,
            senderName = "${user.fullName} (${user.role.arabicTitle})",
            senderRole = user.role,
            recipientId = contact.accountId,
            recipientName = contact.fullName,
            recipientRole = contact.role,
            conversationKey = ChatMessage.buildConversationKey(user.id, contact.accountId),
            messageText = cleanText,
            timestamp = timeLabel,
            imageBase64 = imageBase64,
            schoolId = sid
        )
        val updated = _chatMessages.value + msg
        _chatMessages.value = updated
        repo.saveChatMessages(updated, sid)

        showToastAlert("تم إرسال الرسالة إلى ${contact.fullName}", isSuccess = true)

        // Strictly notify ONLY the exact recipient accountId with deep-link to Direct Chat and sender's accountId
        pushNotification(
            title = "رسالة جديدة من ${msg.senderName}",
            body = cleanText.ifBlank { "تم إرفاق صورة في المحادثة المباشرة" },
            type = NotificationType.REPORT,
            targetUserId = contact.accountId,
            targetSection = AppSection.REPORTS_CHAT.name,
            targetTab = 1,
            targetItemId = user.id
        )
    }

    fun sendChatMessage(
        student: StudentRecord,
        messageText: String,
        imageBase64: String = ""
    ) {
        val parentAcc = _accounts.value.firstOrNull {
            it.role == UserRole.PARENT && it.linkedStudentId == student.id
        }
        val contact = ChatContactItem(
            accountId = parentAcc?.id ?: "acc_par_${student.id}",
            fullName = student.parentName,
            role = UserRole.PARENT,
            subtitle = "ولي أمر الطالب: ${student.studentName} (${student.gradeClass})",
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            phone = student.parentPhone,
            photoBase64 = student.photoBase64,
            linkedStudentId = student.id,
            linkedStudentName = student.studentName
        )
        sendDirectChatMessage(contact, messageText, imageBase64)
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
            heartCount = 0,
            likedByUserIds = emptyList(),
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

    fun updateSchoolEvent(
        eventId: String,
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
        val updated = _events.value.map { ev ->
            if (ev.id == eventId) {
                ev.copy(
                    title = cleanTitle,
                    category = InputSanitizer.sanitize(category).ifBlank { "فعالية مدرسية" },
                    description = InputSanitizer.sanitize(description).ifBlank { cleanTitle },
                    date = InputSanitizer.sanitize(date).ifBlank { ev.date },
                    location = InputSanitizer.sanitize(location).ifBlank { "الموصل - زمار" },
                    imageBase64 = imageBase64
                )
            } else ev
        }
        _events.value = updated
        repo.saveEvents(updated, sid)
        showToastAlert("تم تعديل الخبر / الفعالية وحفظ التغييرات بنجاح", isSuccess = true)
    }

    fun deleteSchoolEvent(eventId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _events.value.filterNot { it.id == eventId }
        _events.value = updated
        repo.saveEvents(updated, sid)
        showToastAlert("تم حذف الفعالية / الخبر نهائياً", isSuccess = true)
    }

    /**
     * Interactive Heart Reaction on Events & News Gallery (`events_gallery`).
     * Restricted to ONE reaction per user (`likedBy` contains `currentUser.id`),
     * synced immediately in real time to schools/{schoolId}/events_gallery in Firestore.
     */
    fun toggleEventHeartReaction(eventId: String) {
        if (!validateSessionAndSchoolAccess()) return
        val sid = _schoolConfig.value.schoolId
        val userId = _currentUser.value?.id.orEmpty()
        if (userId.isEmpty()) return

        val updated = _events.value.map { ev ->
            if (ev.id == eventId) {
                val alreadyLiked = userId in ev.likedByUserIds
                val updatedLikers = if (alreadyLiked) {
                    ev.likedByUserIds.filterNot { it == userId }
                } else {
                    (ev.likedByUserIds + userId).distinct()
                }
                ev.copy(
                    heartCount = updatedLikers.size,
                    likedByUserIds = updatedLikers
                )
            } else ev
        }
        _events.value = updated
        repo.saveEvents(updated, sid)
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
            _leaveRequests.value = repo.loadLeaveRequests(sid)
            _studyMaterials.value = repo.loadStudyMaterials(sid)
            showToastAlert("تمت استعادة النسخة الاحتياطية الكاملة للمدرسة ($sid) بنجاح", isSuccess = true)
            true
        } else {
            showToastAlert(res.exceptionOrNull()?.message ?: "فشل استيراد النسخة الاحتياطية", isSuccess = false)
            false
        }
    }

    // =========================================================================
    // SECTION 10: DIGITAL LIBRARY & STUDY SUMMARIES (study_materials)
    // =========================================================================

    fun publishStudyMaterial(
        title: String,
        subjectName: String,
        gradeClass: String,
        sectionName: String,
        description: String,
        fileUrl: String,
        imageBase64: String,
        materialType: String = "ملخص دراسي / ملزمة PDF"
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى إدخال عنوان الملخص الدراسي أو الملزمة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val teacherName = _currentUser.value?.fullName ?: "إدارة المدرسة"
        val item = StudyMaterialItem(
            id = "mat_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            subjectName = InputSanitizer.sanitize(subjectName),
            gradeClass = InputSanitizer.sanitize(gradeClass),
            sectionName = InputSanitizer.sanitize(sectionName),
            teacherName = InputSanitizer.sanitize(teacherName),
            description = InputSanitizer.sanitize(description).ifBlank { cleanTitle },
            fileUrl = fileUrl.trim(),
            imageBase64 = imageBase64,
            materialType = InputSanitizer.sanitize(materialType).ifBlank { "ملخص دراسي / ملزمة PDF" },
            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            schoolId = sid
        )
        val updated = listOf(item) + _studyMaterials.value
        _studyMaterials.value = updated
        repo.saveStudyMaterials(updated, sid)

        showToastAlert("تم نشر الملخص الدراسي ($cleanTitle) لصف $gradeClass بنجاح", isSuccess = true)

        pushNotification(
            title = "مكتبة الملخصات: $subjectName ($cleanTitle)",
            body = "تم نشر ${item.materialType} لمادة $subjectName للصف $gradeClass بواسطة $teacherName.",
            type = NotificationType.HOMEWORK,
            targetGradeClass = gradeClass,
            targetSectionName = sectionName
        )
    }

    fun deleteStudyMaterial(materialId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _studyMaterials.value.filterNot { it.id == materialId }
        _studyMaterials.value = updated
        repo.saveStudyMaterials(updated, sid)
        showToastAlert("تم حذف الملخص الدراسي من المكتبة الإلكترونية", isSuccess = true)
    }

    // =========================================================================
    // SECTION 11: DAILY BEHAVIOR LOG, HOMEWORK SUBMISSIONS, QUICK QUIZZES,
    // SMART DISMISSAL CALL, LOST & FOUND, PRIVATE SUGGESTIONS, AND DEV UPDATES
    // =========================================================================

    fun sendDailyBehaviorEvaluation(
        student: StudentRecord,
        behaviorType: DailyBehaviorType,
        subjectName: String = "المتابعة اليومية",
        customNote: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val teacherName = _currentUser.value?.fullName ?: "كادر المدرسة"
        val finalNote = InputSanitizer.sanitize(customNote).ifBlank { behaviorType.arabicLabel }

        val item = DailyBehaviorLogItem(
            id = "beh_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.fullNameWithFather,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            subjectName = InputSanitizer.sanitize(subjectName).ifBlank { "المتابعة اليومية" },
            behaviorType = behaviorType,
            note = finalNote,
            teacherName = teacherName,
            date = today,
            schoolId = sid
        )
        val updated = listOf(item) + _dailyBehaviors.value
        _dailyBehaviors.value = updated
        repo.saveDailyBehaviors(updated, sid)

        showToastAlert("تم إرسال تقييم (${behaviorType.arabicLabel}) للطالب ${student.fullNameWithFather} وإشعار ولي أمره فوراً", isSuccess = true)

        pushNotification(
            title = "${behaviorType.notificationTitle} - ${student.fullNameWithFather}",
            body = "تقييم يومي من ($teacherName - $subjectName): ${behaviorType.arabicLabel}. ملاحظة: $finalNote",
            type = NotificationType.REPORT,
            targetStudentId = student.id
        )
    }

    fun submitHomeworkPhotoSolution(
        homework: HomeworkItem,
        solutionImageBase64: String = "",
        studentNote: String = "",
        note: String = studentNote,
        photoBase64: String = solutionImageBase64
    ) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val targetStu = getScopedStudentsForUser(user).firstOrNull()
        val stuId = targetStu?.id ?: user.linkedStudentId.ifBlank { user.id }
        val stuName = targetStu?.fullNameWithFather ?: user.fullName

        val item = HomeworkSubmissionItem(
            id = "hwsub_${UUID.randomUUID().toString().take(6)}",
            homeworkId = homework.id,
            subjectName = homework.subjectName,
            studentId = stuId,
            studentName = stuName,
            gradeClass = homework.gradeClass,
            sectionName = homework.sectionName,
            note = InputSanitizer.sanitize(note).ifBlank { "تسليم حل الواجب البيتي لمادة ${homework.subjectName}" },
            photoBase64 = photoBase64,
            submittedAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()),
            status = HomeworkSubmissionStatus.PENDING_REVIEW,
            schoolId = sid
        )
        val updated = listOf(item) + _homeworkSubmissions.value
        _homeworkSubmissions.value = updated
        repo.saveHomeworkSubmissions(updated, sid)

        showToastAlert("تم إرسال حل الواجب المصور لمادة (${homework.subjectName}) إلى معلم المادة بنجاح", isSuccess = true)

        pushNotification(
            title = "تسليم واجب مصور: $stuName (${homework.subjectName})",
            body = "قام الطالب $stuName (${homework.gradeClass}) بإرفاق صورة حل الواجب البيتي للمراجعة.",
            type = NotificationType.HOMEWORK,
            targetRole = UserRole.TEACHER.name,
            targetSection = AppSection.SCHEDULE_HOMEWORK.name,
            targetTab = 1,
            targetItemId = homework.id
        )
    }

    fun reviewHomeworkSubmission(
        submissionId: String,
        feedback: String = "أحسنت، تمت المراجعة والتصحيح",
        score: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val reviewer = _currentUser.value?.fullName ?: "معلم المادة"
        val cleanFeedback = InputSanitizer.sanitize(feedback).ifBlank { "تمت المراجعة والتصحيح" }
        val cleanScore = InputSanitizer.sanitize(score)
        var targetStudentId = ""
        var subjectName = ""
        var targetHwId = ""

        val updated = _homeworkSubmissions.value.map { sub ->
            if (sub.id == submissionId) {
                targetStudentId = sub.studentId
                subjectName = sub.subjectName
                targetHwId = sub.homeworkId
                sub.copy(
                    status = HomeworkSubmissionStatus.REVIEWED_CORRECTED,
                    teacherFeedback = cleanFeedback,
                    homeworkScore = cleanScore,
                    reviewedByTeacher = reviewer
                )
            } else sub
        }
        _homeworkSubmissions.value = updated
        repo.saveHomeworkSubmissions(updated, sid)
        showToastAlert("تم اعتماد التصحيح وإرسال الرد للطالب وولي أمره فوراً", isSuccess = true)

        if (targetStudentId.isNotBlank()) {
            val notifTitle = if (cleanScore.isNotBlank()) {
                "تم تصحيح الواجب ($subjectName) • درجة الواجب: $cleanScore"
            } else {
                "تم تصحيح ومراجعة الواجب ($subjectName)"
            }
            val notifBody = buildString {
                append("ملاحظة المعلم ($reviewer): $cleanFeedback")
                if (cleanScore.isNotBlank()) {
                    append(" • درجة الواجب: $cleanScore")
                }
            }
            pushNotification(
                title = notifTitle,
                body = notifBody,
                type = NotificationType.HOMEWORK,
                targetStudentId = targetStudentId,
                targetSection = AppSection.SCHEDULE_HOMEWORK.name,
                targetTab = 1,
                targetItemId = targetHwId
            )
        }
    }

    fun publishQuickQuiz(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        questionText: String,
        quizType: QuickQuizType,
        options: List<String>,
        correctOptionIndex: Int
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanQuestion = InputSanitizer.sanitize(questionText)
        if (cleanQuestion.isBlank() || options.size < 2) {
            showToastAlert("يرجى إدخال نص السؤال وخيارات الإجابة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val teacherName = _currentUser.value?.fullName ?: "معلم المادة"
        val item = QuickQuizItem(
            id = "quiz_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            subjectName = subjectName,
            teacherName = teacherName,
            questionText = cleanQuestion,
            quizType = quizType,
            options = options.map { InputSanitizer.sanitize(it) },
            correctOptionIndex = correctOptionIndex.coerceIn(0, options.lastIndex),
            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            studentAnswers = emptyMap(),
            schoolId = sid
        )
        val updated = listOf(item) + _quickQuizzes.value
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)
        showToastAlert("تم نشر الاختبار السريع لمادة ($subjectName) لصف $gradeClass ($sectionName)", isSuccess = true)

        pushNotification(
            title = "اختبار سريع جديد: $subjectName ($gradeClass)",
            body = "نشر $teacherName سؤالاً تفاعلياً سريعاً: $cleanQuestion",
            type = NotificationType.HOMEWORK,
            targetGradeClass = gradeClass,
            targetSectionName = sectionName
        )
    }

    fun publishQuickQuiz(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        questionText: String,
        options: List<String>,
        correctOptionIndex: Int,
        points: Int = 5
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanQuestion = InputSanitizer.sanitize(questionText)
        if (cleanQuestion.isBlank() || options.size < 2) {
            showToastAlert("يرجى إدخال نص السؤال وخيارات الإجابة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val teacherName = _currentUser.value?.fullName ?: "معلم المادة"
        val item = QuickQuizItem(
            id = "quiz_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            subjectName = subjectName,
            teacherName = teacherName,
            questionText = cleanQuestion,
            quizType = QuickQuizType.MULTIPLE_CHOICE,
            options = options.map { InputSanitizer.sanitize(it) },
            correctOptionIndex = correctOptionIndex.coerceIn(0, options.lastIndex),
            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            studentAnswers = emptyMap(),
            points = points.coerceIn(1, 100),
            schoolId = sid
        )
        val updated = listOf(item) + _quickQuizzes.value
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)
        showToastAlert("تم نشر الاختبار السريع لمادة ($subjectName) لصف $gradeClass ($sectionName)", isSuccess = true)

        pushNotification(
            title = "اختبار سريع جديد: $subjectName ($gradeClass)",
            body = "نشر $teacherName سؤالاً تفاعلياً سريعاً: $cleanQuestion",
            type = NotificationType.HOMEWORK,
            targetGradeClass = gradeClass,
            targetSectionName = sectionName
        )
    }

    fun updateQuickQuiz(
        quizId: String,
        subjectName: String,
        questionText: String,
        options: List<String>,
        correctOptionIndex: Int,
        points: Int = 5
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanQuestion = InputSanitizer.sanitize(questionText)
        val cleanOptions = options.map { InputSanitizer.sanitize(it) }.filter { it.isNotBlank() }
        if (cleanQuestion.isBlank() || cleanOptions.size < 2) {
            showToastAlert("يرجى إدخال نص السؤال وخيارات الإجابة بشكل صحيح", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val updated = _quickQuizzes.value.map { q ->
            if (q.id == quizId) {
                q.copy(
                    subjectName = InputSanitizer.sanitize(subjectName).ifBlank { q.subjectName },
                    questionText = cleanQuestion,
                    options = cleanOptions,
                    correctOptionIndex = correctOptionIndex.coerceIn(0, cleanOptions.lastIndex),
                    points = points.coerceIn(1, 100)
                )
            } else q
        }
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)
        showToastAlert("تم تعديل الاختبار السريع وحفظ التغييرات في السحابة بنجاح", isSuccess = true)
    }

    fun deleteQuickQuiz(quizId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _quickQuizzes.value.filterNot { it.id == quizId }
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)
        showToastAlert("تم حذف الاختبار السريع نهائياً", isSuccess = true)
    }

    fun submitQuickQuizAnswer(quiz: QuickQuizItem, chosenIndex: Int) {
        submitQuickQuizAnswer(quiz.id, chosenIndex)
    }

    fun submitQuickQuizAnswer(quizId: String, chosenIndex: Int) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val stuId = getScopedStudentsForUser(user).firstOrNull()?.id ?: user.linkedStudentId.ifBlank { user.id }
        var isCorrect = false

        val updated = _quickQuizzes.value.map { q ->
            if (q.id == quizId) {
                isCorrect = (chosenIndex == q.correctOptionIndex)
                val nextMap = q.studentAnswers.toMutableMap()
                nextMap[stuId] = chosenIndex
                q.copy(studentAnswers = nextMap)
            } else q
        }
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)

        if (isCorrect) {
            showToastAlert("إجابة صحيحة وممتازة! أحسنت التفوق", isSuccess = true)
        } else {
            showToastAlert("تم تسجيل إجابتك وتصحيحها تلقائياً — راجع الإجابة النموذجية", isSuccess = false)
        }
    }

    fun triggerParentDismissalCall(student: StudentRecord) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val timeNow = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(appContext)

        val item = DismissalCallItem(
            id = "dis_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.fullNameWithFather,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            parentName = student.parentName.ifBlank { user.fullName },
            parentPhone = student.parentPhone.ifBlank { user.phone },
            requestedAtTime = timeNow,
            status = "ولي الأمر أمام البوابة بانتظار خروج الطالب",
            isHandled = false,
            schoolId = sid
        )
        val updated = listOf(item) + _dismissalCalls.value
        _dismissalCalls.value = updated
        repo.saveDismissalCalls(updated, sid)

        showToastAlert("تم إرسال نداء الانصراف للإدارة والمدرسين: جاري تجهيز (${student.fullNameWithFather}) للخروج", isSuccess = true)

        pushNotification(
            title = "نداء انصراف فوري: ${student.fullNameWithFather}",
            body = "ولي أمر الطالب (${student.fullNameWithFather} - ${student.gradeClass} شعبة ${student.sectionName}) متواجد الآن أمام باب المدرسة لاستلامه.",
            type = NotificationType.ATTENDANCE,
            targetRole = UserRole.ADMIN.name
        )
    }

    fun markDismissalCallHandled(callId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        var targetStuId = ""
        var stuName = ""
        val updated = _dismissalCalls.value.map { c ->
            if (c.id == callId) {
                targetStuId = c.studentId
                stuName = c.studentName
                c.copy(status = "تم خروج وتسليم الطالب لولي الأمر بسلامة الله", isHandled = true)
            } else c
        }
        _dismissalCalls.value = updated
        repo.saveDismissalCalls(updated, sid)
        showToastAlert("تم تأكيد تسليم الطالب ($stuName) لولي أمره بنجاح", isSuccess = true)

        if (targetStuId.isNotBlank()) {
            pushNotification(
                title = "تم توجيه الطالب للبوابة: $stuName",
                body = "تم إخراج الطالب ($stuName) لتسليمه إلى ولي أمره بسلامة الله.",
                type = NotificationType.ATTENDANCE,
                targetStudentId = targetStuId
            )
        }
    }

    fun publishLostAndFoundItem(
        title: String,
        foundLocation: String,
        photoBase64: String
    ) {
        publishLostAndFoundItem(
            title = title,
            description = title,
            foundLocation = foundLocation,
            photoBase64 = photoBase64
        )
    }

    fun publishLostAndFoundItem(
        title: String,
        description: String,
        foundLocation: String,
        photoBase64: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى إدخال اسم الغرض المفقود أو المعثور عليه", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val item = LostAndFoundItem(
            id = "lf_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            description = InputSanitizer.sanitize(description).ifBlank { cleanTitle },
            foundLocation = InputSanitizer.sanitize(foundLocation).ifBlank { "ساحة المدرسة / الإدارة" },
            date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
            photoBase64 = photoBase64,
            status = LostAndFoundStatus.AT_ADMINISTRATION,
            publisherName = _currentUser.value?.fullName ?: "إدارة المدرسة",
            schoolId = sid
        )
        val updated = listOf(item) + _lostAndFoundItems.value
        _lostAndFoundItems.value = updated
        repo.saveLostAndFoundItems(updated, sid)
        showToastAlert("تمت إضافة ($cleanTitle) إلى سجل المفقودات المدرسية", isSuccess = true)
    }

    fun toggleLostAndFoundStatus(itemId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _lostAndFoundItems.value.map { item ->
            if (item.id == itemId) {
                val nextStatus = if (item.status == LostAndFoundStatus.AT_ADMINISTRATION) {
                    LostAndFoundStatus.RETURNED_TO_OWNER
                } else {
                    LostAndFoundStatus.AT_ADMINISTRATION
                }
                item.copy(status = nextStatus)
            } else item
        }
        _lostAndFoundItems.value = updated
        repo.saveLostAndFoundItems(updated, sid)
        showToastAlert("تم تحديث حالة الغرض في سجل المفقودات", isSuccess = true)
    }

    fun updateLostAndFoundItem(
        itemId: String,
        title: String,
        foundLocation: String,
        photoBase64: String
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanTitle = InputSanitizer.sanitize(title)
        if (cleanTitle.isBlank()) {
            showToastAlert("يرجى إدخال وصف الغرض المفقود أو المعثور عليه", isSuccess = false)
            return
        }
        val cleanLocation = InputSanitizer.sanitize(foundLocation).ifBlank { "الإدارة المدرسية" }
        val sid = _schoolConfig.value.schoolId
        val updated = _lostAndFoundItems.value.map { item ->
            if (item.id == itemId) {
                item.copy(
                    title = cleanTitle,
                    description = cleanTitle,
                    foundLocation = cleanLocation,
                    photoBase64 = photoBase64
                )
            } else item
        }
        _lostAndFoundItems.value = updated
        repo.saveLostAndFoundItems(updated, sid)
        showToastAlert("تم تعديل بيانات الغرض في سجل المفقودات وحفظها بنجاح", isSuccess = true)
    }

    fun deleteLostAndFoundItem(itemId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _lostAndFoundItems.value.filterNot { it.id == itemId }
        _lostAndFoundItems.value = updated
        repo.saveLostAndFoundItems(updated, sid)
        showToastAlert("تم حذف الغرض من سجل المفقودات نهائياً", isSuccess = true)
    }

    fun submitPrivateSuggestionToPrincipal(message: String) {
        submitPrivateSuggestionToPrincipal(
            category = "مقترح وملاحظة سرية",
            title = "رسالة سرية لمدير المدرسة",
            message = message
        )
    }

    fun submitPrivateSuggestionToPrincipal(
        category: String,
        title: String,
        message: String
    ) {
        val user = _currentUser.value ?: return
        val cleanTitle = InputSanitizer.sanitize(title)
        val cleanMsg = InputSanitizer.sanitize(message)
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) {
            showToastAlert("يرجى كتابة عنوان وتفاصيل الرسالة السرية للمدير", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val item = PrivateSuggestionItem(
            id = "sug_${UUID.randomUUID().toString().take(6)}",
            senderName = user.fullName,
            senderRoleLabel = user.role.arabicTitle,
            category = InputSanitizer.sanitize(category).ifBlank { "مقترح تطويري" },
            title = cleanTitle,
            message = cleanMsg,
            createdAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()),
            schoolId = sid
        )
        val updated = listOf(item) + _privateSuggestions.value
        _privateSuggestions.value = updated
        repo.savePrivateSuggestions(updated, sid)
        showToastAlert("تم إرسال رسالتك إلى صندوق المدير السري بنجاح (لا يطلع عليها سوى المدير حصراً)", isSuccess = true)

        pushNotification(
            title = "صندوق المقترحات السري: $cleanTitle",
            body = "رسالة سرية جديدة من (${user.fullName} - ${user.role.arabicTitle}): $cleanMsg",
            type = NotificationType.REPORT,
            targetRole = UserRole.ADMIN.name
        )
    }

    fun deletePrivateSuggestion(suggestionId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _privateSuggestions.value.filterNot { it.id == suggestionId }
        _privateSuggestions.value = updated
        repo.savePrivateSuggestions(updated, sid)
        showToastAlert("تم حذف الرسالة من صندوق المقترحات السري", isSuccess = true)
    }

    fun updateGlobalAppUpdateConfig(
        latestVersionCode: Int,
        latestVersionName: String,
        updateNotes: String,
        downloadUrl: String,
        isForceUpdate: Boolean,
        isUpdateBroadcastActive: Boolean = true
    ) {
        val updated = AppUpdateConfig(
            latestVersionCode = latestVersionCode,
            latestVersionName = InputSanitizer.sanitize(latestVersionName).ifBlank { "2.5.0" },
            apkDownloadUrl = downloadUrl.trim().ifBlank { "https://github.com/madrasati-smart/releases/latest/download/app-debug.apk" },
            releaseNotes = InputSanitizer.sanitize(updateNotes),
            isForceUpdate = isForceUpdate,
            isUpdateBroadcastActive = isUpdateBroadcastActive,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _appUpdateConfig.value = updated
        _dismissedUpdateVersion.value = ""
        repo.saveAppUpdateConfig(updated)
        val statusMsg = if (isUpdateBroadcastActive) {
            "تم نشر وتفعيل التحديث (الإصدار ${updated.latestVersionName}) لجميع المستخدمين فوراً"
        } else {
            "تم حفظ بيانات التحديث (الإصدار ${updated.latestVersionName}) مع إيقاف ظهوره حالياً"
        }
        showToastAlert(statusMsg, isSuccess = true)
    }

    fun updateGlobalAppUpdateConfig(
        versionName: String,
        apkUrl: String,
        releaseNotes: String,
        isForceUpdate: Boolean,
        isBroadcastActive: Boolean
    ) {
        val updated = AppUpdateConfig(
            latestVersionCode = _appUpdateConfig.value.latestVersionCode,
            latestVersionName = InputSanitizer.sanitize(versionName).ifBlank { "2.5.0" },
            apkDownloadUrl = apkUrl.trim().ifBlank { "https://github.com/madrasati-smart/releases/latest/download/app-debug.apk" },
            releaseNotes = InputSanitizer.sanitize(releaseNotes),
            isForceUpdate = isForceUpdate,
            isUpdateBroadcastActive = isBroadcastActive,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _appUpdateConfig.value = updated
        if (isBroadcastActive) {
            _dismissedUpdateVersion.value = ""
        }
        repo.saveAppUpdateConfig(updated)
        showToastAlert("تم حفظ وبث إعدادات تحديث التطبيق (الإصدار ${updated.latestVersionName}) لجميع المستخدمين فوراً", isSuccess = true)
    }

    fun toggleGlobalAppUpdateActive(isActive: Boolean) {
        val updated = _appUpdateConfig.value.copy(
            isUpdateBroadcastActive = isActive,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _appUpdateConfig.value = updated
        if (isActive) {
            _dismissedUpdateVersion.value = ""
        }
        repo.saveAppUpdateConfig(updated)
        showToastAlert(
            if (isActive) "تم تفعيل ظهور إشعار التحديث لجميع المستخدمين" else "تم إيقاف ظهور إشعار التحديث عن المستخدمين",
            isSuccess = isActive
        )
    }

    fun stopAndWithdrawGlobalAppUpdate() {
        val updated = _appUpdateConfig.value.copy(
            isUpdateBroadcastActive = false,
            isForceUpdate = false,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _appUpdateConfig.value = updated
        repo.saveAppUpdateConfig(updated)
        showToastAlert("تم إيقاف وسحب التحديث الحالي فوراً وإخفاؤه من شاشات جميع المستخدمين", isSuccess = true)
    }

    fun dismissAppUpdateBannerLocally() {
        _dismissedUpdateVersion.value = _appUpdateConfig.value.latestVersionName
        if (!_appUpdateConfig.value.isForceUpdate) {
            _appUpdateConfig.value = _appUpdateConfig.value.copy(isUpdateBroadcastActive = false)
        }
    }

    fun updateGlobalOccasionTheme(
        occasionTheme: OccasionThemeType,
        customGreeting: String
    ) {
        updateGlobalOccasionTheme(
            themeType = occasionTheme,
            customTitle = occasionTheme.badgeTitle,
            customBannerText = customGreeting,
            customIconBase64 = if (occasionTheme == OccasionThemeType.OFFICIAL) "" else _occasionThemeConfig.value.customIconBase64,
            customIconUrl = if (occasionTheme == OccasionThemeType.OFFICIAL) "" else _occasionThemeConfig.value.customIconUrl,
            primaryColorHex = occasionTheme.defaultPrimaryHex,
            gradientColorHex = occasionTheme.defaultGradientHex,
            launcherAliasName = occasionTheme.launcherAliasName
        )
    }

    fun updateGlobalOccasionTheme(
        themeType: OccasionThemeType = OccasionThemeType.CUSTOM_MANUAL,
        customTitle: String = "",
        customBannerText: String = "",
        customIconBase64: String = _occasionThemeConfig.value.customIconBase64,
        customIconUrl: String = _occasionThemeConfig.value.customIconUrl,
        primaryColorHex: String = themeType.defaultPrimaryHex,
        gradientColorHex: String = themeType.defaultGradientHex,
        launcherAliasName: String = themeType.launcherAliasName,
        selectedIconKey: String = _occasionThemeConfig.value.selectedIconKey,
        isActive: Boolean = true
    ) {
        val iconKey = OccasionIconKey.fromKey(selectedIconKey)
        val cleanTitle = InputSanitizer.sanitize(customTitle).ifBlank { "تهنئة ومناسبة خاصة" }
        val cleanGreeting = InputSanitizer.sanitize(customBannerText).ifBlank { "نتمنى لجميع منتسبي المدرسة دوام التوفيق والتميز" }
        val resolvedAlias = launcherAliasName.ifBlank { iconKey.launcherAliasName }
        val updated = OccasionThemeConfig(
            isActive = isActive,
            themeType = themeType,
            selectedIconKey = iconKey.name,
            customTitle = cleanTitle,
            customBannerText = cleanGreeting,
            customIconBase64 = customIconBase64,
            customIconUrl = customIconUrl.trim(),
            primaryColorHex = primaryColorHex.ifBlank { "#064E3B" },
            gradientColorHex = gradientColorHex.ifBlank { "#047857" },
            launcherAliasName = resolvedAlias,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _occasionThemeConfig.value = updated
        repo.saveOccasionThemeConfig(updated)
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(
            appContext,
            if (isActive) updated.effectiveAliasName.ifBlank { iconKey.launcherAliasName } else "default"
        )
        if (isActive) {
            showToastAlert("تم تفعيل وإظهار شريط المناسبة (${cleanTitle}) في جميع المدارس فوراً", isSuccess = true)
        } else {
            showToastAlert("تم حفظ بيانات المناسبة مع إبقاء الشريط مخفياً في المدارس", isSuccess = true)
        }
    }

    fun toggleGlobalOccasionThemeActive(isActive: Boolean) {
        val current = _occasionThemeConfig.value
        val iconKey = OccasionIconKey.fromKey(current.selectedIconKey)
        val updated = current.copy(
            isActive = isActive,
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _occasionThemeConfig.value = updated
        repo.saveOccasionThemeConfig(updated)
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(
            appContext,
            if (isActive) updated.effectiveAliasName.ifBlank { iconKey.launcherAliasName } else "default"
        )
        if (isActive) {
            showToastAlert("تم إظهار شريط المناسبة في واجهات جميع المدارس فوراً", isSuccess = true)
        } else {
            showToastAlert("تم إخفاء شريط المناسبة من جميع المدارس والعودة للوضع الافتراضي النظيف", isSuccess = true)
        }
    }

    fun resetGlobalOccasionThemeToOfficial() {
        val official = OccasionThemeConfig(
            isActive = false,
            themeType = OccasionThemeType.OFFICIAL,
            selectedIconKey = OccasionIconKey.CROWN.name,
            customTitle = "",
            customBannerText = "",
            customIconBase64 = "",
            customIconUrl = "",
            primaryColorHex = OccasionThemeType.OFFICIAL.defaultPrimaryHex,
            gradientColorHex = OccasionThemeType.OFFICIAL.defaultGradientHex,
            launcherAliasName = "default",
            updatedAt = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        _occasionThemeConfig.value = official
        repo.saveOccasionThemeConfig(official)
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(appContext, "default")
        showToastAlert("تم إيقاف وإخفاء شريط المناسبة من جميع المدارس والعودة للوضع الرسمي الزمردي", isSuccess = true)
    }

    // =========================================================================
    // URGENT SCHOOL ALERT (Principal Publish / Edit / Stop per School)
    // =========================================================================

    private val _dismissedUrgentAlertSignature = MutableStateFlow("")
    val dismissedUrgentAlertSignature: StateFlow<String> = _dismissedUrgentAlertSignature.asStateFlow()

    fun dismissUrgentSchoolAlertLocally() {
        val currentAlert = _schoolConfig.value.urgentAlert
        val sig = currentAlert.signatureKey.ifBlank { "${currentAlert.updatedAt}_${currentAlert.message.hashCode()}" }
        _dismissedUrgentAlertSignature.value = sig
    }

    fun publishOrUpdateUrgentSchoolAlert(message: String) {
        val cleanMsg = InputSanitizer.sanitize(message).trim()
        if (cleanMsg.isBlank()) {
            showToastAlert("يرجى كتابة نص التنبيه العاجل أولاً", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId.ifBlank { repo.getActiveSchoolId() }
        if (sid.isBlank()) return
        val author = _currentUser.value?.fullName?.takeIf { it.isNotBlank() } ?: "مدير المدرسة"
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.US).format(Date())
        val sig = "${System.currentTimeMillis()}_${cleanMsg.hashCode()}"
        val newAlert = UrgentSchoolAlert(
            id = "urgent_alert",
            schoolId = sid,
            message = cleanMsg,
            updatedAt = nowStamp,
            authorName = author,
            isActive = true,
            signatureKey = sig
        )
        _dismissedUrgentAlertSignature.value = ""
        _schoolConfig.value = _schoolConfig.value.copy(urgentAlert = newAlert)
        repo.saveUrgentSchoolAlert(newAlert, sid)
        showToastAlert("تم نشر/تحديث التنبيه العاجل أعلى الشاشة لجميع حسابات مدرستك فوراً", isSuccess = true)
    }

    fun deleteOrStopUrgentSchoolAlert() {
        val sid = _schoolConfig.value.schoolId.ifBlank { repo.getActiveSchoolId() }
        if (sid.isBlank()) return
        val clearedAlert = UrgentSchoolAlert(
            id = "urgent_alert",
            schoolId = sid,
            message = "",
            updatedAt = "",
            authorName = "",
            isActive = false,
            signatureKey = ""
        )
        _dismissedUrgentAlertSignature.value = ""
        _schoolConfig.value = _schoolConfig.value.copy(urgentAlert = clearedAlert)
        repo.saveUrgentSchoolAlert(clearedAlert, sid)
        showToastAlert("تم حذف وإيقاف التنبيه العاجل من شاشات جميع منتسبي المدرسة فوراً", isSuccess = true)
    }

    // =========================================================================
    // BIOMETRIC FINGERPRINT AUTHENTICATION & SAVED CREDENTIALS FLOW
    // =========================================================================

    fun enableBiometricForCurrentUser() {
        val user = _currentUser.value ?: return
        val cfg = _schoolConfig.value
        val today = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val creds = BiometricSavedCredentials(
            username = user.username,
            password = user.password,
            fullName = user.fullName,
            roleArabic = user.role.arabicTitle,
            schoolId = cfg.schoolId,
            schoolName = cfg.schoolName,
            isDeveloper = false,
            enabledAt = today
        )
        repo.saveBiometricCredentials(creds)
        _biometricCredentials.value = creds
        showToastAlert("تم تفعيل الدخول السريع ببصمة الإصبع لحساب (${user.fullName}) بنجاح", isSuccess = true)
    }

    fun enableBiometricForDeveloper() {
        val today = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val creds = BiometricSavedCredentials(
            username = "ABU-RASLAN",
            password = "202699",
            fullName = "المطور أحمد أبو رسلان",
            roleArabic = "مطور النظام العام",
            schoolId = "DEV",
            schoolName = "لوحة تحكم المطور",
            isDeveloper = true,
            enabledAt = today
        )
        repo.saveBiometricCredentials(creds)
        _biometricCredentials.value = creds
        showToastAlert("تم تفعيل الدخول السريع ببصمة الإصبع لحساب المطور (أحمد أبو رسلان) بنجاح", isSuccess = true)
    }

    fun disableBiometricLogin() {
        repo.clearBiometricCredentials()
        _biometricCredentials.value = null
        showToastAlert("تم إلغاء تفعيل الدخول ببصمة الإصبع ومسح الجلسة المحفوظة", isSuccess = true)
    }

    fun loginWithSavedBiometrics(): Result<UserAccount?> {
        val creds = _biometricCredentials.value ?: repo.loadBiometricCredentials()
        if (creds == null || !creds.isValid) {
            showToastAlert("لا توجد بيانات بصمة محفوظة على هذا الجهاز", isSuccess = false)
            return Result.failure(IllegalStateException("لا توجد بصمة محفوظة"))
        }
        return login(creds.username, creds.password)
    }

    // =========================================================================
    // SECTION 12: STUDENT CREATIVE COMMUNITY ("مجتمع ونشاطات الطلبة")
    // & FULL ADMIN MODERATION & SANCTIONS SYSTEM
    // =========================================================================

    fun getUserActivityProfile(userId: String): UserActivityProfileAndSanctions {
        val sid = _schoolConfig.value.schoolId
        return _userProfilesAndSanctions.value[userId] ?: UserActivityProfileAndSanctions(
            userId = userId,
            schoolId = sid
        )
    }

    fun getFollowersCount(targetUserId: String): Int {
        return _userProfilesAndSanctions.value.values.count { targetUserId in it.followingUserIds }
    }

    fun updateMyActivityBio(newBio: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val cleanBio = InputSanitizer.sanitize(newBio).take(220)
        val currentMap = _userProfilesAndSanctions.value.toMutableMap()
        val existing = currentMap[user.id] ?: UserActivityProfileAndSanctions(userId = user.id, schoolId = sid)
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        currentMap[user.id] = existing.copy(bio = cleanBio, updatedAt = nowStamp)
        _userProfilesAndSanctions.value = currentMap
        repo.saveUserProfilesAndSanctions(currentMap, sid)
        showToastAlert("تم حفظ وتحديث النبذة التعريفية (Bio) في ملفك الإبداعي بنجاح", isSuccess = true)
    }

    fun toggleFollowUser(targetUserId: String) {
        val user = _currentUser.value ?: return
        if (targetUserId.isBlank() || targetUserId == user.id) return
        val sid = _schoolConfig.value.schoolId
        val currentMap = _userProfilesAndSanctions.value.toMutableMap()
        val myProfile = currentMap[user.id] ?: UserActivityProfileAndSanctions(userId = user.id, schoolId = sid)
        val alreadyFollowing = targetUserId in myProfile.followingUserIds
        val updatedFollowing = if (alreadyFollowing) {
            myProfile.followingUserIds.filterNot { it == targetUserId }
        } else {
            (myProfile.followingUserIds + targetUserId).distinct()
        }
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        currentMap[user.id] = myProfile.copy(followingUserIds = updatedFollowing, updatedAt = nowStamp)
        _userProfilesAndSanctions.value = currentMap
        repo.saveUserProfilesAndSanctions(currentMap, sid)

        val targetAccName = _accounts.value.firstOrNull { it.id == targetUserId }?.fullName ?: "العضو"
        if (!alreadyFollowing) {
            showToastAlert("أصبحت تتابع ($targetAccName) في مجتمع نشاطات الطلبة", isSuccess = true)
        } else {
            showToastAlert("تم إلغاء متابعة ($targetAccName)", isSuccess = true)
        }
    }

    fun publishStudentActivityPost(
        category: String,
        title: String,
        content: String,
        imageBase64: String
    ): Boolean {
        if (!validateSessionAndSchoolAccess()) return false
        val user = _currentUser.value ?: return false
        val sid = _schoolConfig.value.schoolId

        // Check if user is restricted from posting (Mute Posting sanction)
        val mySanctions = getUserActivityProfile(user.id)
        if (user.role != UserRole.ADMIN && mySanctions.isPostingMuted()) {
            val rem = mySanctions.remainingPostingBanLabel()
            val reason = mySanctions.mutePostingReason.ifBlank { "مخالفة ضوابط النشر في المنصة" }
            showToastAlert(
                "عذراً، حسابك موقوف مؤقتاً عن نشر النشاطات ($rem) — السبب: $reason",
                isSuccess = false
            )
            return false
        }

        val cleanTitle = InputSanitizer.sanitize(title)
        val cleanContent = InputSanitizer.sanitize(content)
        if (cleanTitle.isBlank() && cleanContent.isBlank() && imageBase64.isBlank()) {
            showToastAlert("يرجى كتابة عنوان أو محتوى النشاط الإبداعي أو إرفاق صورة", isSuccess = false)
            return false
        }

        val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
        val authorClass = linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
        val authorSec = linkedStu?.sectionName?.ifBlank { user.sectionName } ?: user.sectionName
        val authorPhoto = user.photoBase64.ifBlank { linkedStu?.photoBase64.orEmpty() }
        val authorDisplayName = linkedStu?.fullNameWithFather?.takeIf { user.role == UserRole.STUDENT } ?: user.fullName

        // Admin & Teacher publish directly as APPROVED; Student/Parent posts go to PENDING moderation queue
        val initialStatus = if (user.role == UserRole.ADMIN || user.role == UserRole.TEACHER) {
            StudentActivityStatus.APPROVED
        } else {
            StudentActivityStatus.PENDING
        }

        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val post = StudentActivityPost(
            id = "act_${UUID.randomUUID().toString().take(6)}",
            schoolId = sid,
            authorUserId = user.id,
            authorStudentId = linkedStu?.id ?: user.linkedStudentId,
            authorName = authorDisplayName,
            authorRole = user.role,
            authorGradeClass = authorClass,
            authorSectionName = authorSec,
            authorPhotoBase64 = authorPhoto,
            category = InputSanitizer.sanitize(category).ifBlank { "موهبة وإبداع علمي" },
            title = cleanTitle.ifBlank { "مشاركة إبداعية" },
            content = cleanContent,
            imageBase64 = imageBase64,
            status = initialStatus,
            reviewedByName = if (initialStatus == StudentActivityStatus.APPROVED) user.fullName else "",
            createdAt = nowStamp,
            createdAtMillis = System.currentTimeMillis()
        )

        val updated = listOf(post) + _studentActivityPosts.value
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)

        if (initialStatus == StudentActivityStatus.PENDING) {
            showToastAlert(
                "تم إرسال مشاركتك الإبداعية إلى قائمة المراجعة لدى الإدارة والمعلمين، وستُنشر فور الموافقة عليها",
                isSuccess = true
            )
            pushNotification(
                title = "مشاركة طالب بانتظار المراجعة: $authorDisplayName",
                body = "أرسل ($authorDisplayName - $authorClass) مشاركة إبداعية بعنوان (${post.title}) في قسم نشاطات الطلبة للمراجعة والاعتماد.",
                type = NotificationType.GENERAL,
                targetRole = UserRole.ADMIN.name,
                targetSection = AppSection.STUDENT_ACTIVITIES.name,
                targetTab = 1,
                targetItemId = post.id
            )
        } else {
            showToastAlert("تم نشر المشاركة في مجتمع نشاطات الطلبة فوراً", isSuccess = true)
        }
        return true
    }

    fun reviewStudentActivityPost(postId: String, approve: Boolean) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val reviewer = _currentUser.value?.fullName ?: "إدارة المدرسة"
        val sid = _schoolConfig.value.schoolId
        var targetPost: StudentActivityPost? = null

        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                val nextStatus = if (approve) StudentActivityStatus.APPROVED else StudentActivityStatus.REJECTED
                val copyPost = p.copy(status = nextStatus, reviewedByName = reviewer)
                targetPost = copyPost
                copyPost
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)

        val reviewed = targetPost ?: return
        if (approve) {
            showToastAlert("تمت الموافقة على نشر نشاط (${reviewed.authorName}) في ساحة الإبداع", isSuccess = true)
            pushNotification(
                title = "تم اعتماد ونشر مشاركتك الإبداعية!",
                body = "وافقت إدارة المدرسة ($reviewer) على نشر مشاركتك (${reviewed.title}) في قسم نشاطات الطلبة.",
                type = NotificationType.GENERAL,
                targetUserId = reviewed.authorUserId,
                targetSection = AppSection.STUDENT_ACTIVITIES.name,
                targetTab = 0,
                targetItemId = reviewed.id
            )
        } else {
            showToastAlert("تم رفض المشاركة وإخفاؤها من القائمة", isSuccess = false)
            pushNotification(
                title = "ملاحظة حول مشاركتك في نشاطات الطلبة",
                body = "اعتذرت الإدارة عن نشر مشاركتك (${reviewed.title}). يرجى مراجعة ضوابط النشر التربوي والمحاولة مجدداً.",
                type = NotificationType.GENERAL,
                targetUserId = reviewed.authorUserId,
                targetSection = AppSection.STUDENT_ACTIVITIES.name,
                targetTab = 0,
                targetItemId = reviewed.id
            )
        }
    }

    fun updateStudentActivityPost(
        postId: String,
        category: String,
        title: String,
        content: String,
        imageBase64: String
    ) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val target = _studentActivityPosts.value.firstOrNull { it.id == postId } ?: return
        if (user.role != UserRole.ADMIN && target.authorUserId != user.id) {
            showToastAlert("لا تملك صلاحية تعديل هذا المنشور", isSuccess = false)
            return
        }
        val cleanTitle = InputSanitizer.sanitize(title).ifBlank { target.title }
        val cleanContent = InputSanitizer.sanitize(content)
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    category = InputSanitizer.sanitize(category).ifBlank { p.category },
                    title = cleanTitle,
                    content = cleanContent,
                    imageBase64 = imageBase64
                )
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم حفظ تعديلات المنشور بنجاح", isSuccess = true)
    }

    fun deleteStudentActivityPost(postId: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val target = _studentActivityPosts.value.firstOrNull { it.id == postId } ?: return
        if (user.role != UserRole.ADMIN && target.authorUserId != user.id) {
            showToastAlert("لا تملك صلاحية حذف هذا المنشور", isSuccess = false)
            return
        }
        val updated = _studentActivityPosts.value.filterNot { it.id == postId }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم حذف المنشور من مجتمع نشاطات الطلبة نهائياً", isSuccess = true)
    }

    fun toggleStudentActivityHeart(postId: String) {
        if (!validateSessionAndSchoolAccess()) return
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId

        val mySanctions = getUserActivityProfile(user.id)
        if (user.role != UserRole.ADMIN && mySanctions.isInteractionMuted()) {
            val rem = mySanctions.remainingInteractionBanLabel()
            val reason = mySanctions.muteInteractionReason.ifBlank { "مخالفة آداب التفاعل" }
            showToastAlert(
                "عذراً، حسابك مقيد مؤقتاً من التفاعل والإعجاب ($rem) — السبب: $reason",
                isSuccess = false
            )
            return
        }

        var newlyLikedPost: StudentActivityPost? = null
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                val alreadyLiked = user.id in p.likedByUserIds
                val nextLikers = if (alreadyLiked) {
                    p.likedByUserIds.filterNot { it == user.id }
                } else {
                    (p.likedByUserIds + user.id).distinct()
                }
                val copyP = p.copy(
                    heartCount = nextLikers.size,
                    likedByUserIds = nextLikers
                )
                if (!alreadyLiked) {
                    newlyLikedPost = copyP
                }
                copyP
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)

        // Strictly notify ONLY the post owner when someone else likes their post
        newlyLikedPost?.let { likedPost ->
            if (likedPost.authorUserId.isNotBlank() && likedPost.authorUserId != user.id) {
                pushNotification(
                    title = "إعجاب جديد بمشاركتك الإبداعية",
                    body = "أعجب (${user.fullName}) بمنشورك (${likedPost.title}) في قسم نشاطات الطلبة.",
                    type = NotificationType.GENERAL,
                    targetUserId = likedPost.authorUserId,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0,
                    targetItemId = likedPost.id
                )
            }
        }
    }

    fun addStudentActivityComment(postId: String, commentText: String) {
        if (!validateSessionAndSchoolAccess()) return
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId

        val mySanctions = getUserActivityProfile(user.id)
        if (user.role != UserRole.ADMIN && mySanctions.isInteractionMuted()) {
            val rem = mySanctions.remainingInteractionBanLabel()
            val reason = mySanctions.muteInteractionReason.ifBlank { "مخالفة آداب التعليقات" }
            showToastAlert(
                "عذراً، حسابك موقوف مؤقتاً عن كتابة التعليقات ($rem) — السبب: $reason",
                isSuccess = false
            )
            return
        }

        val cleanText = InputSanitizer.sanitize(commentText)
        if (cleanText.isBlank()) {
            showToastAlert("يرجى كتابة نص التعليق أولاً", isSuccess = false)
            return
        }

        val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
        val authorClass = linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
        val authorPhoto = user.photoBase64.ifBlank { linkedStu?.photoBase64.orEmpty() }
        val authorDisplayName = linkedStu?.fullNameWithFather?.takeIf { user.role == UserRole.STUDENT } ?: user.fullName
        val nowStamp = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())

        val newComment = StudentActivityComment(
            id = "cmt_${UUID.randomUUID().toString().take(6)}",
            postId = postId,
            authorUserId = user.id,
            authorName = authorDisplayName,
            authorRole = user.role,
            authorGradeClass = authorClass,
            authorPhotoBase64 = authorPhoto,
            text = cleanText,
            createdAt = nowStamp,
            createdAtMillis = System.currentTimeMillis()
        )

        var targetPost: StudentActivityPost? = null
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                val copyP = p.copy(comments = p.comments + newComment)
                targetPost = copyP
                copyP
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)

        // Strictly notify ONLY the post owner
        targetPost?.let { post ->
            if (post.authorUserId.isNotBlank() && post.authorUserId != user.id) {
                pushNotification(
                    title = "تعليق جديد على منشورك (${post.title})",
                    body = "علّق ($authorDisplayName) على مشاركتك: \"$cleanText\"",
                    type = NotificationType.GENERAL,
                    targetUserId = post.authorUserId,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0,
                    targetItemId = post.id
                )
            }
        }
    }

    fun updateStudentActivityComment(postId: String, commentId: String, newText: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val cleanText = InputSanitizer.sanitize(newText)
        if (cleanText.isBlank()) return

        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.map { c ->
                        if (c.id == commentId && (user.role == UserRole.ADMIN || c.authorUserId == user.id)) {
                            c.copy(text = cleanText)
                        } else c
                    }
                )
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم تعديل التعليق بنجاح", isSuccess = true)
    }

    fun deleteStudentActivityComment(postId: String, commentId: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.filterNot { c ->
                        c.id == commentId && (user.role == UserRole.ADMIN || c.authorUserId == user.id || p.authorUserId == user.id)
                    }
                )
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم حذف التعليق بنجاح", isSuccess = true)
    }

    fun addStudentActivityReply(postId: String, commentId: String, replyText: String) {
        if (!validateSessionAndSchoolAccess()) return
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId

        val mySanctions = getUserActivityProfile(user.id)
        if (user.role != UserRole.ADMIN && mySanctions.isInteractionMuted()) {
            val rem = mySanctions.remainingInteractionBanLabel()
            val reason = mySanctions.muteInteractionReason.ifBlank { "مخالفة آداب التفاعل" }
            showToastAlert(
                "عذراً، حسابك موقوف مؤقتاً عن كتابة الردود ($rem) — السبب: $reason",
                isSuccess = false
            )
            return
        }

        val cleanText = InputSanitizer.sanitize(replyText)
        if (cleanText.isBlank()) return

        val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
        val authorClass = linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass
        val authorPhoto = user.photoBase64.ifBlank { linkedStu?.photoBase64.orEmpty() }
        val authorDisplayName = linkedStu?.fullNameWithFather?.takeIf { user.role == UserRole.STUDENT } ?: user.fullName
        val nowStamp = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())

        val newReply = StudentActivityReply(
            id = "rpl_${UUID.randomUUID().toString().take(6)}",
            commentId = commentId,
            postId = postId,
            authorUserId = user.id,
            authorName = authorDisplayName,
            authorRole = user.role,
            authorGradeClass = authorClass,
            authorPhotoBase64 = authorPhoto,
            text = cleanText,
            createdAt = nowStamp,
            createdAtMillis = System.currentTimeMillis()
        )

        var targetPost: StudentActivityPost? = null
        var parentCommentAuthorId = ""
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                val nextComments = p.comments.map { c ->
                    if (c.id == commentId) {
                        parentCommentAuthorId = c.authorUserId
                        c.copy(replies = c.replies + newReply)
                    } else c
                }
                val copyP = p.copy(comments = nextComments)
                targetPost = copyP
                copyP
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)

        // Notify post owner (and comment owner if different)
        targetPost?.let { post ->
            if (post.authorUserId.isNotBlank() && post.authorUserId != user.id) {
                pushNotification(
                    title = "رد جديد على منشورك (${post.title})",
                    body = "كتب ($authorDisplayName) رداً على تعليق في منشورك: \"$cleanText\"",
                    type = NotificationType.GENERAL,
                    targetUserId = post.authorUserId,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0,
                    targetItemId = post.id
                )
            }
            if (parentCommentAuthorId.isNotBlank() &&
                parentCommentAuthorId != user.id &&
                parentCommentAuthorId != post.authorUserId
            ) {
                pushNotification(
                    title = "رد جديد على تعليقك في نشاطات الطلبة",
                    body = "ردّ ($authorDisplayName) على تعليقك: \"$cleanText\"",
                    type = NotificationType.GENERAL,
                    targetUserId = parentCommentAuthorId,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0,
                    targetItemId = post.id
                )
            }
        }
    }

    fun updateStudentActivityReply(postId: String, commentId: String, replyId: String, newText: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val cleanText = InputSanitizer.sanitize(newText)
        if (cleanText.isBlank()) return
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.map { c ->
                        if (c.id == commentId) {
                            c.copy(
                                replies = c.replies.map { r ->
                                    if (r.id == replyId && (user.role == UserRole.ADMIN || r.authorUserId == user.id)) {
                                        r.copy(text = cleanText)
                                    } else r
                                }
                            )
                        } else c
                    }
                )
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم تعديل الرد بنجاح", isSuccess = true)
    }

    fun deleteStudentActivityReply(postId: String, commentId: String, replyId: String) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val updated = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.map { c ->
                        if (c.id == commentId) {
                            c.copy(
                                replies = c.replies.filterNot { r ->
                                    r.id == replyId && (user.role == UserRole.ADMIN || r.authorUserId == user.id || p.authorUserId == user.id)
                                }
                            )
                        } else c
                    }
                )
            } else p
        }
        _studentActivityPosts.value = updated
        repo.saveStudentActivityPosts(updated, sid)
        showToastAlert("تم حذف الرد بنجاح", isSuccess = true)
    }

    fun reportStudentActivityViolation(
        postId: String,
        commentId: String = "",
        replyId: String = "",
        targetType: ViolationTargetType = ViolationTargetType.POST,
        reportedUserId: String,
        reportedStudentId: String = "",
        reportedUserName: String,
        reportedUserRole: UserRole,
        contentExcerpt: String,
        reason: String
    ) {
        val user = _currentUser.value ?: return
        val cleanReason = InputSanitizer.sanitize(reason)
        if (cleanReason.isBlank()) {
            showToastAlert("يرجى توضيح سبب الإبلاغ عن المخالفة", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val rep = StudentActivityViolationReport(
            id = "vrep_${UUID.randomUUID().toString().take(6)}",
            schoolId = sid,
            postId = postId,
            commentId = commentId,
            replyId = replyId,
            targetType = targetType,
            reportedUserId = reportedUserId,
            reportedStudentId = reportedStudentId,
            reportedUserName = InputSanitizer.sanitize(reportedUserName),
            reportedUserRole = reportedUserRole,
            reportedContentExcerpt = InputSanitizer.sanitize(contentExcerpt).take(160),
            reporterUserId = user.id,
            reporterName = user.fullName,
            reason = cleanReason,
            createdAt = nowStamp,
            isResolved = false
        )
        val updated = listOf(rep) + _activityViolationReports.value
        _activityViolationReports.value = updated
        repo.saveActivityViolationReports(updated, sid)

        showToastAlert("تم إرسال بلاغ المخالفة إلى صندوق بلاغات مدير المدرسة للمراجعة الفورية", isSuccess = true)

        pushNotification(
            title = "بلاغ مخالفة في نشاطات الطلبة (${targetType.arabicLabel})",
            body = "أبلغ (${user.fullName}) عن ${targetType.arabicLabel} للمستخدم ($reportedUserName) — السبب: $cleanReason",
            type = NotificationType.REPORT,
            targetRole = UserRole.ADMIN.name,
            targetSection = AppSection.STUDENT_ACTIVITIES.name,
            targetTab = 2,
            targetItemId = postId
        )
    }

    /**
     * Principal's Full Moderation & Sanctions Execution:
     * 1) Mute Posting (24h, 3d, 7d, Permanent)
     * 2) Mute Interaction / Likes & Comments (24h, 3d, 7d, Permanent)
     * 3) Official Administrative Warning Notification to the violating user AND their parent (if student).
     */
    fun applyAdminUserSanction(
        targetUserId: String,
        targetStudentId: String = "",
        targetUserName: String,
        mutePostingDuration: SanctionDurationOption?,
        muteInteractionDuration: SanctionDurationOption?,
        sendOfficialWarning: Boolean,
        reasonAndViolationText: String,
        relatedReportId: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val cleanReason = InputSanitizer.sanitize(reasonAndViolationText).ifBlank {
            "مخالفة ضوابط وآداب النشر والتفاعل في مجتمع المدرسة"
        }
        val nowMs = System.currentTimeMillis()
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())

        val currentMap = _userProfilesAndSanctions.value.toMutableMap()
        val existing = currentMap[targetUserId] ?: UserActivityProfileAndSanctions(
            userId = targetUserId,
            schoolId = sid
        )

        val nextPostUntil = if (mutePostingDuration != null) {
            if (mutePostingDuration.isPermanent) Long.MAX_VALUE / 2 else nowMs + mutePostingDuration.durationMillis
        } else existing.mutePostingUntilMillis
        val nextPostPerm = mutePostingDuration?.isPermanent ?: existing.isPermanentPostingBan

        val nextInterUntil = if (muteInteractionDuration != null) {
            if (muteInteractionDuration.isPermanent) Long.MAX_VALUE / 2 else nowMs + muteInteractionDuration.durationMillis
        } else existing.muteInteractionUntilMillis
        val nextInterPerm = muteInteractionDuration?.isPermanent ?: existing.isPermanentInteractionBan

        val updatedProfile = existing.copy(
            mutePostingUntilMillis = nextPostUntil,
            isPermanentPostingBan = nextPostPerm,
            mutePostingReason = if (mutePostingDuration != null) cleanReason else existing.mutePostingReason,
            muteInteractionUntilMillis = nextInterUntil,
            isPermanentInteractionBan = nextInterPerm,
            muteInteractionReason = if (muteInteractionDuration != null) cleanReason else existing.muteInteractionReason,
            warningCount = if (sendOfficialWarning) existing.warningCount + 1 else existing.warningCount,
            lastWarningText = if (sendOfficialWarning) cleanReason else existing.lastWarningText,
            updatedAt = nowStamp
        )
        currentMap[targetUserId] = updatedProfile
        _userProfilesAndSanctions.value = currentMap
        repo.saveUserProfilesAndSanctions(currentMap, sid)

        val actionSummaryParts = mutableListOf<String>()
        if (mutePostingDuration != null) {
            actionSummaryParts.add("تعليق النشر (${mutePostingDuration.arabicLabel})")
        }
        if (muteInteractionDuration != null) {
            actionSummaryParts.add("تعليق التفاعل والتعليقات (${muteInteractionDuration.arabicLabel})")
        }
        if (sendOfficialWarning) {
            actionSummaryParts.add("إرسال إنذار إداري رسمي")
        }
        val actionSummary = actionSummaryParts.joinToString(" + ").ifBlank { "تنبيه رقابي" }

        if (relatedReportId.isNotBlank()) {
            val updatedReps = _activityViolationReports.value.map { r ->
                if (r.id == relatedReportId) {
                    r.copy(isResolved = true, resolutionActionNote = "$actionSummary — $cleanReason")
                } else r
            }
            _activityViolationReports.value = updatedReps
            repo.saveActivityViolationReports(updatedReps, sid)
        }

        // Send private official warning notification to the violating user AND copy to their parent if student
        if (sendOfficialWarning || mutePostingDuration != null || muteInteractionDuration != null) {
            val warningTitle = "إنذار وإجراء رقابي رسمي من إدارة المدرسة"
            val warningBody = "تنبيه للمستخدم ($targetUserName): تم اتخاذ إجراء ($actionSummary) بسبب: ($cleanReason). يرجى الالتزام التام بآداب وضوابط المنصة التربوية."

            if (targetUserId.isNotBlank()) {
                pushNotification(
                    title = warningTitle,
                    body = warningBody,
                    type = NotificationType.REPORT,
                    targetUserId = targetUserId,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0
                )
            }

            val resolvedStuId = targetStudentId.ifBlank {
                _accounts.value.firstOrNull { it.id == targetUserId }?.linkedStudentId.orEmpty()
            }
            if (resolvedStuId.isNotBlank()) {
                pushNotification(
                    title = "إشعار لولي الأمر • إجراء رقابي: $targetUserName",
                    body = "نود إعلامكم بتوجيه ($actionSummary) لحساب الطالب ($targetUserName) في قسم نشاطات الطلبة بسبب: ($cleanReason).",
                    type = NotificationType.REPORT,
                    targetStudentId = resolvedStuId,
                    targetRole = UserRole.PARENT.name,
                    targetSection = AppSection.STUDENT_ACTIVITIES.name,
                    targetTab = 0
                )
            }
        }

        showToastAlert("تم تنفيذ الإجراء الرقابي ($actionSummary) بحق ($targetUserName) وحفظه في السحابة فوراً", isSuccess = true)
    }

    fun liftAdminUserSanctions(targetUserId: String, targetUserName: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val currentMap = _userProfilesAndSanctions.value.toMutableMap()
        val existing = currentMap[targetUserId] ?: return
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        currentMap[targetUserId] = existing.copy(
            mutePostingUntilMillis = 0L,
            isPermanentPostingBan = false,
            mutePostingReason = "",
            muteInteractionUntilMillis = 0L,
            isPermanentInteractionBan = false,
            muteInteractionReason = "",
            updatedAt = nowStamp
        )
        _userProfilesAndSanctions.value = currentMap
        repo.saveUserProfilesAndSanctions(currentMap, sid)
        showToastAlert("تم رفع جميع قيود النشر والتفاعل عن حساب ($targetUserName) بنجاح", isSuccess = true)

        pushNotification(
            title = "تم رفع القيود عن حسابك في نشاطات الطلبة",
            body = "قامت إدارة المدرسة برفع تعليق النشر والتفاعل عن حسابك ($targetUserName). يمكنك الآن المشاركة والتفاعل مجدداً.",
            type = NotificationType.GENERAL,
            targetUserId = targetUserId,
            targetSection = AppSection.STUDENT_ACTIVITIES.name,
            targetTab = 0
        )
    }

    fun deleteActivityViolationReport(reportId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _activityViolationReports.value.filterNot { it.id == reportId }
        _activityViolationReports.value = updated
        repo.saveActivityViolationReports(updated, sid)
        showToastAlert("تم حذف البلاغ من صندوق بلاغات النشاطات", isSuccess = true)
    }
}

