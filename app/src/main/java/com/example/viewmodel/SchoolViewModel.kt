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

    private val _teacherRatings = MutableStateFlow(repo.loadTeacherRatings())
    val teacherRatings: StateFlow<List<TeacherRatingRecord>> = _teacherRatings.asStateFlow()

    private val _issuedReportCards = MutableStateFlow(repo.loadIssuedReportCards())
    val issuedReportCards: StateFlow<List<IssuedStudentReportCard>> = _issuedReportCards.asStateFlow()

    private val _activeCelebrationReportCard = MutableStateFlow<IssuedStudentReportCard?>(null)
    val activeCelebrationReportCard: StateFlow<IssuedStudentReportCard?> = _activeCelebrationReportCard.asStateFlow()

    fun openCelebrationReportCardModal(card: IssuedStudentReportCard) {
        _activeCelebrationReportCard.value = card
        if (!card.isViewedByParent && (_currentUser.value?.role == UserRole.PARENT || _currentUser.value?.role == UserRole.STUDENT)) {
            markIssuedReportCardViewed(card.id)
        }
    }

    fun closeCelebrationReportCardModal() {
        _activeCelebrationReportCard.value = null
    }

    private val _showUrgentAlertModal = MutableStateFlow(false)
    val showUrgentAlertModal: StateFlow<Boolean> = _showUrgentAlertModal.asStateFlow()

    private val _holidaysConfig = MutableStateFlow(_schoolConfig.value.holidaysConfig)
    val holidaysConfig: StateFlow<SchoolHolidaysConfig> = _holidaysConfig.asStateFlow()

    private val _showHolidaysModal = MutableStateFlow(false)
    val showHolidaysModal: StateFlow<Boolean> = _showHolidaysModal.asStateFlow()

    private val _deepLinkTarget = MutableStateFlow<DeepLinkNavigationTarget?>(null)
    val deepLinkTarget: StateFlow<DeepLinkNavigationTarget?> = _deepLinkTarget.asStateFlow()

    private val _isNetworkOnline = MutableStateFlow(checkCurrentNetworkOnline())
    val isNetworkOnline: StateFlow<Boolean> = _isNetworkOnline.asStateFlow()

    val isCloudActionProcessing: StateFlow<Boolean> = FirebaseCloudSync.isCloudWriteInFlight

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
                    earnedPoints = if (correct) q.points else 0,
                    answeredAt = q.answerTimestamps[stuId] ?: q.createdAt
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

    private val _developerVaultConfig = MutableStateFlow(repo.loadDeveloperVaultConfig())
    val developerVaultConfig: StateFlow<DeveloperVaultConfig> = _developerVaultConfig.asStateFlow()

    // Cloud-Synced School Security & Activation Gate States
    private val _isSchoolGatePassed = MutableStateFlow(repo.isSchoolSecurityGatePassed())
    val isSchoolGatePassed: StateFlow<Boolean> = _isSchoolGatePassed.asStateFlow()

    private val _verifiedGateSchoolCode = MutableStateFlow(repo.getVerifiedGateSchoolCode())
    val verifiedGateSchoolCode: StateFlow<String> = _verifiedGateSchoolCode.asStateFlow()

    private val _showIndexedLeaveRequestsModal = MutableStateFlow(false)
    val showIndexedLeaveRequestsModal: StateFlow<Boolean> = _showIndexedLeaveRequestsModal.asStateFlow()

    fun setShowIndexedLeaveRequestsModal(show: Boolean) {
        _showIndexedLeaveRequestsModal.value = show
    }

    fun openIndexedLeaveRequestsModal() {
        _showIndexedLeaveRequestsModal.value = true
    }

    fun dismissIndexedLeaveRequestsModal() {
        _showIndexedLeaveRequestsModal.value = false
    }

    private val _latestAnnualPromotionReport = MutableStateFlow<AnnualPromotionReport?>(null)
    val latestAnnualPromotionReport: StateFlow<AnnualPromotionReport?> = _latestAnnualPromotionReport.asStateFlow()

    fun clearAnnualPromotionReportModal() {
        _latestAnnualPromotionReport.value = null
    }

    fun dismissAnnualPromotionReportModal() {
        _latestAnnualPromotionReport.value = null
    }

    private val _discoveredSchoolName = MutableStateFlow<String?>(null)
    val discoveredSchoolName: StateFlow<String?> = _discoveredSchoolName.asStateFlow()

    private val _loginConnectingMessage = MutableStateFlow<String?>(null)
    val loginConnectingMessage: StateFlow<String?> = _loginConnectingMessage.asStateFlow()

    private val _isBackgroundSyncing = MutableStateFlow(false)
    val isBackgroundSyncing: StateFlow<Boolean> = _isBackgroundSyncing.asStateFlow()

    // Persistent Attendance Selection States (never reset on tab/section navigation)
    private val _selectedAttendanceDate = MutableStateFlow(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()))
    val selectedAttendanceDate: StateFlow<String> = _selectedAttendanceDate.asStateFlow()

    private val _selectedAttendanceClass = MutableStateFlow("")
    val selectedAttendanceClass: StateFlow<String> = _selectedAttendanceClass.asStateFlow()

    private val _selectedAttendanceSection = MutableStateFlow("")
    val selectedAttendanceSection: StateFlow<String> = _selectedAttendanceSection.asStateFlow()

    fun setSelectedAttendanceDate(dateIso: String) {
        val clean = ArabicTextNormalizer.normalizeDigitsToAscii(dateIso.trim())
        if (clean.isNotBlank()) {
            _selectedAttendanceDate.value = clean
        }
    }

    fun setSelectedAttendanceClass(gradeClass: String) {
        if (gradeClass.isNotBlank()) {
            _selectedAttendanceClass.value = gradeClass
        }
    }

    fun setSelectedAttendanceSection(sectionName: String) {
        if (sectionName.isNotBlank()) {
            _selectedAttendanceSection.value = sectionName
        }
    }

    val availableClasses: List<String>
        get() = (_schoolConfig.value.schoolStage.defaultClasses + _schoolConfig.value.registeredClasses)
            .map { ArabicTextNormalizer.cleanPureClassName(it) }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusClassName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeClass(it) }
            .ifEmpty { _schoolConfig.value.schoolStage.defaultClasses }

    val availableSections: List<String>
        get() = _schoolConfig.value.registeredSections
            .map { ArabicTextNormalizer.cleanPureSectionName(it) }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusSectionName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeSection(it) }
            .ifEmpty { listOf("أ", "ب", "ج") }

    val availableSubjects: List<String>
        get() = _schoolConfig.value.registeredSubjects.ifEmpty { _schoolConfig.value.schoolStage.defaultSubjects }

    val weekDays = listOf("الأحد", "الاثنين", "الثلاثاء", "الأربعاء", "الخميس")

    init {
        SystemNotificationHelper.ensureChannelCreated(appContext)
        registerNetworkConnectivityCallback()
        refreshDeveloperCloudData()
        restoreSavedSessionIfValid()
        startRealtimeCloudMonitor()
    }

    private fun checkCurrentNetworkOnline(): Boolean {
        return try {
            val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                ?: return true
            val activeNet = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(activeNet) ?: return false
            caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun registerNetworkConnectivityCallback() {
        try {
            val cm = appContext.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager
                ?: return
            val request = android.net.NetworkRequest.Builder()
                .addCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()
            cm.registerNetworkCallback(
                request,
                object : android.net.ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: android.net.Network) {
                        val wasOffline = !_isNetworkOnline.value
                        _isNetworkOnline.value = true
                        if (wasOffline) {
                            triggerImmediateCloudResync()
                        }
                    }

                    override fun onLost(network: android.net.Network) {
                        _isNetworkOnline.value = checkCurrentNetworkOnline()
                    }
                }
            )
        } catch (_: Exception) {
            _isNetworkOnline.value = true
        }
    }

    fun triggerImmediateCloudResync() {
        refreshConnectivityState()
        refreshDeveloperCloudData()
        val sid = _schoolConfig.value.schoolId
        if (sid.isNotBlank()) {
            viewModelScope.launch {
                try {
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HONOR_ROLL)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_HONOR_ROLL)?.let {
                            if (it.isNotBlank() &&
                                _schoolConfig.value.schoolId == sid &&
                                !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HONOR_ROLL)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HONOR_ROLL, it)
                                val parsedStudents = repo.parseStudentsJson(it, sid)
                                _students.value = mergeAttendanceIntoStudents(parsedStudents, _attendance.value)
                            }
                        }
                    }
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ATTENDANCE)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_ATTENDANCE)?.let {
                            if (it.isNotBlank() &&
                                _schoolConfig.value.schoolId == sid &&
                                !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ATTENDANCE)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ATTENDANCE, it)
                                val parsedAtt = repo.parseAttendanceJson(it, sid)
                                _attendance.value = parsedAtt
                                _students.value = mergeAttendanceIntoStudents(_students.value, parsedAtt)
                            }
                        }
                    }
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)?.let {
                            if (it.isNotBlank() &&
                                _schoolConfig.value.schoolId == sid &&
                                !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES, it)
                                _studentActivityPosts.value = repo.parseStudentActivityPostsJson(it, sid)
                            }
                        }
                    }
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_USERS)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_USERS)?.let {
                            if (it.isNotBlank() && !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_USERS)) {
                                val parsedAccounts = repo.parseAccountsJson(it, sid)
                                if (parsedAccounts.isNotEmpty()) {
                                    repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_USERS, it)
                                    _accounts.value = parsedAccounts
                                    _currentUser.value?.let { curr ->
                                        parsedAccounts.firstOrNull { acc -> acc.id == curr.id }?.let { refreshedUser ->
                                            _currentUser.value = refreshedUser
                                        }
                                    }
                                    _teacherRatings.value = repo.loadTeacherRatings(sid)
                                }
                            }
                        }
                    }
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HOLIDAYS)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_HOLIDAYS)?.let {
                            if (it.isNotBlank() && !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HOLIDAYS)) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HOLIDAYS, it)
                                val parsedHol = repo.parseHolidaysConfigJson(it, sid)
                                _holidaysConfig.value = parsedHol
                                _schoolConfig.value = _schoolConfig.value.copy(holidaysConfig = parsedHol)
                            }
                        }
                    }
                    if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_TEACHER_RATINGS)) {
                        FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_TEACHER_RATINGS)?.let {
                            if (it.isNotBlank() && it != "[]" && !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_TEACHER_RATINGS)) {
                                repo.mergeEmbeddedTeacherRatingsIntoLocalCache(it, sid)
                                _teacherRatings.value = repo.loadTeacherRatings(sid)
                            }
                        }
                    }
                    flushPendingActivityCommentsToCloud()
                } catch (_: Exception) {
                }
            }
        }
    }

    fun refreshConnectivityState(): Boolean {
        val osOnline = checkCurrentNetworkOnline()
        if (!osOnline) {
            _isNetworkOnline.value = false
            return false
        }
        viewModelScope.launch {
            val realOnline = FirebaseCloudSync.verifyRealInternet(3000)
            _isNetworkOnline.value = realOnline
            if (realOnline) {
                flushPendingActivityCommentsToCloud()
            }
        }
        return osOnline
    }

    fun requireOnlineAndExecute(action: () -> Unit) {
        if (!checkCurrentNetworkOnline()) {
            _isNetworkOnline.value = false
            showToastAlert("لا يوجد اتصال حقيقي بالإنترنت — يرجى التحقق من الشبكة والمحاولة مجدداً", isSuccess = false)
            return
        }
        action()
    }

    // =========================================================================
    // SESSION VALIDATION & MULTI-TENANT SCHOOL ISOLATION
    // =========================================================================

    private fun restoreSavedSessionIfValid() {
        val savedSession = repo.loadSession() ?: return
        val currentSchoolId = _schoolConfig.value.schoolId
        if (currentSchoolId.isNotEmpty() && savedSession.schoolId == currentSchoolId) {
            if (!isSchoolSubscriptionActive(currentSchoolId)) {
                forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
                return
            }
            val matchedUser = _accounts.value.firstOrNull {
                it.id == savedSession.userId && it.isActive && (it.schoolId.isEmpty() || it.schoolId == currentSchoolId)
            } ?: run {
                val cfg = _schoolConfig.value
                val matchedRoute = cfg.effectiveBusRoutesList.firstOrNull { route ->
                    route.isDriverAccountEnabled &&
                        route.driverUsername.isNotBlank() &&
                        (savedSession.userId == "acc_bus_driver_${currentSchoolId}_${route.id}" ||
                            savedSession.username.equals(route.driverUsername, ignoreCase = true))
                }
                if (matchedRoute != null) {
                    UserAccount(
                        id = "acc_bus_driver_${currentSchoolId}_${matchedRoute.id}",
                        username = matchedRoute.driverUsername,
                        password = matchedRoute.driverPassword,
                        fullName = matchedRoute.driverName.ifBlank { cfg.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" } },
                        role = UserRole.TEACHER,
                        isActive = true,
                        phone = matchedRoute.driverPhone.ifBlank { cfg.busDriverPhone },
                        specialty = matchedRoute.routeAreaName.ifBlank { "سائق الخط المدرسي" },
                        assignedClasses = matchedRoute.id,
                        schoolId = currentSchoolId,
                        isBusDriverAccount = true
                    )
                } else if (cfg.isDriverAccountEnabled && savedSession.userId == "acc_bus_driver_$currentSchoolId") {
                    UserAccount(
                        id = "acc_bus_driver_$currentSchoolId",
                        username = cfg.busDriverUsername,
                        password = cfg.busDriverPassword,
                        fullName = cfg.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" },
                        role = UserRole.TEACHER,
                        isActive = true,
                        phone = cfg.busDriverPhone,
                        specialty = cfg.busRouteAreaName.ifBlank { "سائق الخط المدرسي" },
                        assignedClasses = cfg.effectiveBusRoutesList.firstOrNull()?.id.orEmpty(),
                        schoolId = currentSchoolId,
                        isBusDriverAccount = true
                    )
                } else null
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
        if (regEntry != null && (!regEntry.isActive || regEntry.isSuspended)) return false
        val localCfg = repo.loadSchoolConfig(schoolId)
        if (localCfg.isSetupCompleted && (!localCfg.isActive || localCfg.isSuspended)) return false
        return true
    }

    private fun mergeAttendanceIntoStudents(
        studentsList: List<StudentRecord>,
        attendanceList: List<AttendanceEntry>
    ): List<StudentRecord> {
        if (studentsList.isEmpty()) return studentsList
        val attByStudent = attendanceList.groupBy { it.studentId.trim() }
        return studentsList.map { stu ->
            val cleanStuId = stu.id.trim()
            val stuEntries = attByStudent[cleanStuId].orEmpty()
            val mergedMap = linkedMapOf<String, String>()
            stu.dailyAttendance.forEach { (dKey, stVal) ->
                val cleanDKey = ArabicTextNormalizer.normalizeDateIso(dKey.trim())
                if (cleanDKey.isNotEmpty() && stVal.isNotBlank()) {
                    mergedMap[cleanDKey] = stVal.trim().uppercase()
                }
            }
            val cleanTodayDate = ArabicTextNormalizer.normalizeDateIso(stu.todayStatusDate)
            val cleanTodayStatus = stu.todayStatus.trim().uppercase()
            if (cleanTodayDate.isNotEmpty() && cleanTodayStatus.isNotEmpty() && !mergedMap.containsKey(cleanTodayDate)) {
                mergedMap[cleanTodayDate] = cleanTodayStatus
            }
            // Iterate from oldest to newest so the latest prepended attendance entry is authoritative
            stuEntries.asReversed().forEach { entry ->
                val cleanDKey = ArabicTextNormalizer.normalizeDateIso(entry.date.trim())
                if (cleanDKey.isNotEmpty()) {
                    mergedMap[cleanDKey] = entry.status.name
                }
            }
            val latestDateKey = mergedMap.keys.maxOrNull().orEmpty()
            val effectiveTodayDate = cleanTodayDate.ifEmpty { latestDateKey }
            val effectiveTodayStatus = when {
                effectiveTodayDate.isNotEmpty() && mergedMap.containsKey(effectiveTodayDate) ->
                    mergedMap[effectiveTodayDate].orEmpty()
                cleanTodayStatus.isNotEmpty() -> cleanTodayStatus
                latestDateKey.isNotEmpty() -> mergedMap[latestDateKey].orEmpty()
                else -> ""
            }
            val calcAbsenceDays = mergedMap.count { (dKey, stVal) ->
                stVal.equals(AttendanceStatus.ABSENT.name, ignoreCase = true) && getHolidayLabelForDate(dKey) == null
            }
            val calcExcusedDays = mergedMap.count { (dKey, stVal) ->
                stVal.equals(AttendanceStatus.EXCUSED.name, ignoreCase = true) && getHolidayLabelForDate(dKey) == null
            }
            val calcLateDays = mergedMap.count { (dKey, stVal) ->
                stVal.equals(AttendanceStatus.LATE.name, ignoreCase = true) && getHolidayLabelForDate(dKey) == null
            }
            stu.copy(
                dailyAttendance = mergedMap,
                todayStatus = effectiveTodayStatus,
                todayStatusDate = effectiveTodayDate,
                absenceDays = calcAbsenceDays,
                excusedDays = calcExcusedDays,
                lateDays = calcLateDays
            )
        }
    }

    private fun reloadSchoolDataIntoMemory(schoolId: String) {
        if (schoolId.isBlank()) return
        repo.setActiveSchoolId(schoolId)
        val loadedCfg = repo.loadSchoolConfig(schoolId)
        _schoolConfig.value = loadedCfg
        _featureSettings.value = loadedCfg.featureSettings
        _holidaysConfig.value = loadedCfg.holidaysConfig
        _accounts.value = repo.loadAccounts(schoolId)
        val loadedAtt = repo.loadAttendance(schoolId)
        val loadedStudents = mergeAttendanceIntoStudents(repo.loadStudents(schoolId), loadedAtt)
        _students.value = loadedStudents
        _attendance.value = loadedAtt
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
        _teacherRatings.value = repo.loadTeacherRatings(schoolId)
        _issuedReportCards.value = repo.loadIssuedReportCards(schoolId)
        val loadedUrg = _schoolConfig.value.urgentAlert
        if (loadedUrg.isActive && loadedUrg.message.isNotBlank()) {
            val sig = loadedUrg.signatureKey.ifBlank { "${loadedUrg.updatedAt}_${loadedUrg.message.hashCode()}" }
            if (sig.isNotBlank() && repo.getLastShownUrgentAlertId(schoolId) != sig) {
                repo.setLastShownUrgentAlertId(sig, schoolId)
                SystemNotificationHelper.showUrgentExternalNotification(
                    context = appContext,
                    schoolName = _schoolConfig.value.schoolName,
                    urgentMessage = loadedUrg.message,
                    signatureKey = sig
                )
            }
        }
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

                FirebaseCloudSync.fetchGlobalCollectionPayload(
                    collectionName = FirebaseConfigConstants.COL_SYSTEM_SECURE_CONFIG,
                    docName = FirebaseConfigConstants.DOC_DEVELOPER_VAULT
                )?.let { remoteVaultJson ->
                    if (remoteVaultJson.isNotBlank()) {
                        val parsedVault = repo.parseDeveloperVaultConfigJson(remoteVaultJson)
                        _developerVaultConfig.value = parsedVault
                        repo.saveDeveloperVaultConfig(parsedVault)
                    }
                }

                repo.syncCloudCurriculumBooks()?.let { cloudBooksJson ->
                    com.example.ui.screens.IraqiCurriculumCatalog.applyCloudBooksJson(appContext, cloudBooksJson)
                }
            } catch (_: Exception) {
            }
        }
    }

    fun returnToSchoolSecurityGate() {
        repo.setSchoolSecurityGatePassed(false)
        _isSchoolGatePassed.value = false
    }

    fun verifyAndActivateSchoolGateAsAdmin(
        schoolCodeInput: String,
        adminPasswordInput: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val normalizedDigits = ArabicTextNormalizer.normalizeDigitsToAscii(schoolCodeInput.trim())
        val cleanCode = InputSanitizer.sanitizeSchoolId(normalizedDigits).uppercase()
        val cleanPass = InputSanitizer.sanitize(adminPasswordInput.trim())

        if (cleanCode.isBlank() || cleanPass.isBlank()) {
            val msg = "يرجى إدخال رمز المدرسة وكلمة مرور الإدارة"
            showToastAlert(msg, isSuccess = false)
            onResult(false, msg)
            return
        }

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
                            .map { (_, items) -> items.firstOrNull { it.used } ?: items.first() }
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
            } catch (_: Exception) {
            }

            // 1. Check if it's a one-time founding license code (e.g., MAD-XXXX)
            val matchedLicense = _schoolLicenses.value.firstOrNull { lic: SchoolLicense ->
                lic.code.equals(cleanCode, ignoreCase = true)
            }
            if (matchedLicense != null && !matchedLicense.used) {
                _pendingActivationCode.value = matchedLicense.code
                _isSchoolSetupUnlocked.value = true
                repo.setSchoolSecurityGatePassed(true, cleanCode)
                _verifiedGateSchoolCode.value = cleanCode
                _isSchoolGatePassed.value = true
                val okMsg = "تم التحقق من رمز التأسيس (${matchedLicense.code}) بنجاح"
                showToastAlert(okMsg, isSuccess = true)
                onResult(true, okMsg)
                return@launch
            }

            // 2. Try syncing target school from cloud if not locally cached
            try {
                repo.syncEssentialAuthFromCloud(cleanCode)
            } catch (_: Exception) {
            }

            val matchedReg = _registeredSchools.value.firstOrNull { reg: RegisteredSchoolSummary ->
                reg.schoolId.equals(cleanCode, ignoreCase = true) ||
                    reg.schoolActivationCode.equals(cleanCode, ignoreCase = true)
            }
            val currentCfg = _schoolConfig.value
            val localCandidateCfg = repo.loadSchoolConfig(cleanCode)

            val isKnownCode = cleanCode == "1001" ||
                cleanCode.equals(currentCfg.schoolId, ignoreCase = true) ||
                cleanCode.equals(currentCfg.schoolActivationCode, ignoreCase = true) ||
                matchedReg != null ||
                localCandidateCfg.isSetupCompleted ||
                (matchedLicense != null && matchedLicense.used)

            if (!isKnownCode) {
                val errMsg = "رمز المدرسة ($cleanCode) غير صحيح أو غير مسجل"
                showToastAlert(errMsg, isSuccess = false)
                onResult(false, errMsg)
                return@launch
            }

            val resolvedSid = when {
                matchedReg != null -> matchedReg.schoolId
                matchedLicense != null && matchedLicense.usedBySchoolId.isNotBlank() -> matchedLicense.usedBySchoolId
                localCandidateCfg.schoolId.isNotBlank() -> localCandidateCfg.schoolId
                currentCfg.schoolId.isNotBlank() && (cleanCode == "1001" || cleanCode.equals(currentCfg.schoolActivationCode, ignoreCase = true)) -> currentCfg.schoolId
                else -> cleanCode
            }

            try {
                repo.syncEssentialAuthFromCloud(resolvedSid)
            } catch (_: Exception) {
            }

            val schoolAccounts = repo.loadAccounts(resolvedSid).ifEmpty {
                if (_schoolConfig.value.schoolId == resolvedSid) _accounts.value else emptyList()
            }
            val adminAccount = schoolAccounts.firstOrNull { acc: UserAccount -> acc.role == UserRole.ADMIN }

            val passwordValid = if (adminAccount != null) {
                com.example.util.SecurityShieldHelper.verifyPassword(cleanPass, adminAccount.password) ||
                    adminAccount.password == cleanPass
            } else {
                cleanPass == "123456" || cleanPass.length >= 4
            }

            if (!passwordValid) {
                val errMsg = "كلمة مرور الإدارة غير صحيحة، يرجى المحاولة مجدداً"
                showToastAlert(errMsg, isSuccess = false)
                onResult(false, errMsg)
                return@launch
            }

            val baseCfg = repo.loadSchoolConfig(resolvedSid).let { loaded ->
                if (loaded.schoolId.isNotBlank()) loaded else _schoolConfig.value
            }
            val activatedCfg = baseCfg.copy(
                schoolId = resolvedSid,
                isSetupCompleted = true,
                isActive = true,
                isSchoolOfficiallyActivated = true,
                schoolActivationCode = cleanCode
            )
            repo.setActiveSchoolId(resolvedSid)
            repo.saveSchoolConfig(activatedCfg)
            _schoolConfig.value = activatedCfg

            // Ensure an ADMIN account exists if activating for the first time with 1001 or new schoolId
            if (schoolAccounts.none { acc: UserAccount -> acc.role == UserRole.ADMIN }) {
                val prefixedAdminUser = FirebaseConfigConstants.applySchoolPrefix(resolvedSid, "admin")
                val defaultAdmin = UserAccount(
                    id = "admin_${resolvedSid.lowercase()}",
                    username = prefixedAdminUser,
                    password = cleanPass,
                    fullName = activatedCfg.principalFullName.ifBlank { "مدير المدرسة" },
                    role = UserRole.ADMIN,
                    isActive = true,
                    specialty = "المدير",
                    assignedClasses = "جميع الصفوف والشعب",
                    schoolId = resolvedSid
                )
                val updatedAccs = listOf(defaultAdmin) + schoolAccounts
                repo.saveAccounts(updatedAccs, resolvedSid)
                _accounts.value = updatedAccs
            }

            repo.upsertRegisteredSchool(activatedCfg)
            _registeredSchools.value = repo.loadRegisteredSchools()
            reloadSchoolDataIntoMemory(resolvedSid)
            _discoveredSchoolName.value = activatedCfg.schoolName

            repo.setSchoolSecurityGatePassed(true, resolvedSid)
            _verifiedGateSchoolCode.value = resolvedSid
            _isSchoolGatePassed.value = true

            val successMsg = "تم تفعيل المدرسة (${activatedCfg.schoolName}) بنجاح"
            showToastAlert(successMsg, isSuccess = true)
            onResult(true, successMsg)
        }
    }

    fun verifyActivatedSchoolCodeForAccess(
        schoolCodeInput: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val normalizedDigits = ArabicTextNormalizer.normalizeDigitsToAscii(schoolCodeInput.trim())
        val cleanCode = InputSanitizer.sanitizeSchoolId(normalizedDigits).uppercase()

        if (cleanCode.isBlank()) {
            val msg = "يرجى إدخال رمز المدرسة للمتابعة"
            showToastAlert(msg, isSuccess = false)
            onResult(false, msg)
            return
        }

        viewModelScope.launch {
            try {
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
            } catch (_: Exception) {
            }

            val matchedReg = _registeredSchools.value.firstOrNull { reg: RegisteredSchoolSummary ->
                reg.schoolId.equals(cleanCode, ignoreCase = true) ||
                    reg.schoolActivationCode.equals(cleanCode, ignoreCase = true)
            }
            val resolvedSid = matchedReg?.schoolId ?: when {
                cleanCode.equals(_schoolConfig.value.schoolId, ignoreCase = true) ||
                    cleanCode.equals(_schoolConfig.value.schoolActivationCode, ignoreCase = true) ->
                    _schoolConfig.value.schoolId.ifBlank { cleanCode }
                else -> cleanCode
            }

            var cloudPulled = false
            try {
                cloudPulled = repo.syncEssentialAuthFromCloud(resolvedSid)
            } catch (_: Exception) {
            }

            val targetCfg = repo.loadSchoolConfig(resolvedSid)
            val targetAccounts = repo.loadAccounts(resolvedSid)

            val schoolExists = cleanCode == "1001" ||
                matchedReg != null ||
                cloudPulled ||
                targetCfg.isSetupCompleted ||
                targetAccounts.isNotEmpty() ||
                (_schoolConfig.value.isSetupCompleted &&
                    (cleanCode.equals(_schoolConfig.value.schoolId, ignoreCase = true) ||
                        cleanCode.equals(_schoolConfig.value.schoolActivationCode, ignoreCase = true)))

            if (!schoolExists) {
                val errMsg = "رمز المدرسة ($cleanCode) غير مسجل، تأكد من الرمز أو راجع إدارة المدرسة"
                showToastAlert(errMsg, isSuccess = false)
                onResult(false, errMsg)
                return@launch
            }

            val isOfficiallyActivated = targetCfg.isSchoolOfficiallyActivated ||
                (matchedReg?.isSchoolOfficiallyActivated == true) ||
                cleanCode == "1001" ||
                targetAccounts.isNotEmpty() ||
                targetCfg.isSetupCompleted

            if (!isOfficiallyActivated) {
                val errMsg = "هذه المدرسة بانتظار التفعيل من الإدارة (اختر تفعيل مدرسة جديدة)"
                showToastAlert(errMsg, isSuccess = false)
                onResult(false, errMsg)
                return@launch
            }

            if ((matchedReg != null && (!matchedReg.isActive || matchedReg.isSuspended)) ||
                (targetCfg.isSetupCompleted && (!targetCfg.isActive || targetCfg.isSuspended))
            ) {
                val suspMsg = "تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية"
                repo.clearLocalSchoolCacheOnly(resolvedSid)
                showToastAlert(suspMsg, isSuccess = false)
                onResult(false, suspMsg)
                return@launch
            }

            val finalSid = resolvedSid.ifBlank { "1001" }
            if (targetCfg.schoolId.isBlank() && cleanCode == "1001") {
                val readyCfg = _schoolConfig.value.copy(
                    schoolId = "1001",
                    isSetupCompleted = true,
                    isActive = true,
                    isSchoolOfficiallyActivated = true,
                    schoolActivationCode = "1001"
                )
                repo.setActiveSchoolId("1001")
                repo.saveSchoolConfig(readyCfg)
                _schoolConfig.value = readyCfg
            } else {
                reloadSchoolDataIntoMemory(finalSid)
            }

            _discoveredSchoolName.value = _schoolConfig.value.schoolName
            repo.setSchoolSecurityGatePassed(true, finalSid)
            _verifiedGateSchoolCode.value = finalSid
            _isSchoolGatePassed.value = true

            val okMsg = "مرحباً بك في ${_schoolConfig.value.schoolName}"
            showToastAlert(okMsg, isSuccess = true)
            onResult(true, okMsg)
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
            if (s.schoolId == schoolId) {
                val nextAct = !s.isActive
                s.copy(isActive = nextAct, isSuspended = !nextAct)
            } else s
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
                isActive = nextActive,
                isSuspended = !nextActive
            )
            repo.saveSchoolConfig(updatedCfg)
            if (prevActiveId.isNotBlank() && prevActiveId != schoolId) {
                repo.setActiveSchoolId(prevActiveId)
            }
            if (_schoolConfig.value.schoolId == schoolId) {
                _schoolConfig.value = updatedCfg
                if (!nextActive && !_isDeveloperAuthenticated.value && _currentUser.value != null) {
                    forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
                    return
                }
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

    fun sendDeveloperBroadcastToPrincipals(
        title: String,
        message: String,
        iconKey: String = OccasionIconKey.BELL.name,
        imageBase64: String = "",
        imageUrl: String = ""
    ) {
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
            senderName = "إدارة المنصة التقنية",
            createdAt = today,
            targetSchoolId = "ALL",
            iconKey = iconKey.ifBlank { OccasionIconKey.BELL.name },
            imageBase64 = imageBase64.trim(),
            imageUrl = InputSanitizer.sanitize(imageUrl)
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

    fun updateDeveloperBroadcast(
        broadcastId: String,
        newTitle: String,
        newMessage: String,
        newIconKey: String? = null,
        newImageBase64: String? = null,
        newImageUrl: String? = null
    ) {
        val cleanTitle = InputSanitizer.sanitize(newTitle)
        val cleanMsg = InputSanitizer.sanitize(newMessage)
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) {
            showToastAlert("يرجى إدخال عنوان ونص التعميم المحدث", isSuccess = false)
            return
        }
        val updated = _developerBroadcasts.value.map { item ->
            if (item.id == broadcastId) {
                item.copy(
                    title = cleanTitle,
                    message = cleanMsg,
                    iconKey = newIconKey?.ifBlank { item.iconKey } ?: item.iconKey,
                    imageBase64 = newImageBase64 ?: item.imageBase64,
                    imageUrl = if (newImageUrl != null) InputSanitizer.sanitize(newImageUrl) else item.imageUrl
                )
            } else item
        }
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)
        showToastAlert("تم تعديل وتحديث التعميم لدى جميع مدراء المدارس فوراً", isSuccess = true)
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
                showToastAlert("كود التفعيل ($cleanCode) غير صحيح، يرجى التأكد من الكود والمحاولة مجدداً", isSuccess = false)
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
        _latestBannerAlert.value = null
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
                        // 1. Live sync: honor_roll & students (including Honor Roll status, points, heart reactions, photos, dailyAttendance)
                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HONOR_ROLL)) {
                            val remoteStudentsRaw = FirebaseCloudSync.fetchSubCollectionPayload(
                                sid,
                                FirebaseConfigConstants.COL_HONOR_ROLL
                            )
                            if (_schoolConfig.value.schoolId != sid) continue
                            if (!remoteStudentsRaw.isNullOrBlank() &&
                                !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HONOR_ROLL) &&
                                remoteStudentsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HONOR_ROLL)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HONOR_ROLL, remoteStudentsRaw)
                                val parsedRemoteStudents = repo.parseStudentsJson(remoteStudentsRaw, sid)
                                // Merge existing in-memory dailyAttendance & _attendance so a remote student sync never erases local attendance
                                val localByStu = _students.value.associateBy { it.id.trim() }
                                val preservedStudents = parsedRemoteStudents.map { rStu ->
                                    val localStu = localByStu[rStu.id.trim()]
                                    val localAtt = localStu?.dailyAttendance.orEmpty()
                                    val localGate = localStu?.dailyGateLog.orEmpty()
                                    if (localAtt.isEmpty() && localGate.isEmpty()) rStu else rStu.copy(
                                        dailyAttendance = rStu.dailyAttendance + localAtt,
                                        dailyGateLog = localGate + rStu.dailyGateLog,
                                        todayStatus = localStu?.todayStatus?.ifBlank { rStu.todayStatus } ?: rStu.todayStatus,
                                        todayStatusDate = localStu?.todayStatusDate?.ifBlank { rStu.todayStatusDate } ?: rStu.todayStatusDate
                                    )
                                }
                                _students.value = mergeAttendanceIntoStudents(preservedStudents, _attendance.value)
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
                                    _teacherRatings.value = repo.loadTeacherRatings(sid)
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
                                if (!parsedCfg.isActive || parsedCfg.isSuspended) {
                                    forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
                                    continue
                                }
                                _schoolConfig.value = parsedCfg
                                _featureSettings.value = parsedCfg.featureSettings
                                _holidaysConfig.value = parsedCfg.holidaysConfig
                                _teacherRatings.value = repo.loadTeacherRatings(sid)
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
                            if (_schoolConfig.value.schoolId != sid) continue
                            if (!remoteAttRaw.isNullOrBlank() &&
                                !FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ATTENDANCE) &&
                                remoteAttRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ATTENDANCE)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ATTENDANCE, remoteAttRaw)
                                val remoteEntries = repo.parseAttendanceJson(remoteAttRaw, sid)
                                // Merge with local _attendance so local entries that haven't round-tripped are never lost
                                val mergedEntries = (remoteEntries + _attendance.value.filter { it.schoolId.isBlank() || it.schoolId == sid })
                                    .distinctBy { "${it.studentId.trim()}__${ArabicTextNormalizer.normalizeDateIso(it.date.trim())}" }
                                _attendance.value = mergedEntries
                                _students.value = mergeAttendanceIntoStudents(_students.value, mergedEntries)
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

                        // 14. Live sync: developer broadcasts & schools registry status check
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

                        FirebaseCloudSync.fetchGlobalCollectionPayload(
                            collectionName = FirebaseConfigConstants.COL_SCHOOLS_REGISTRY,
                            docName = "registry"
                        )?.let { remoteRegJson ->
                            if (remoteRegJson.isNotBlank()) {
                                val remoteSchools = repo.parseRegisteredSchoolsJson(remoteRegJson)
                                val wasPreviouslyInRegistry = _registeredSchools.value.any {
                                    it.schoolId.equals(sid, ignoreCase = true)
                                }
                                _registeredSchools.value = remoteSchools
                                val matchedReg = remoteSchools.firstOrNull {
                                    it.schoolId.equals(sid, ignoreCase = true)
                                }
                                if (matchedReg != null && (!matchedReg.isActive || matchedReg.isSuspended)) {
                                    forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
                                    continue
                                } else if (matchedReg == null && wasPreviouslyInRegistry && remoteSchools.isNotEmpty()) {
                                    forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
                                    continue
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
                                if (parsedUrg.isActive && parsedUrg.message.isNotBlank()) {
                                    val sig = parsedUrg.signatureKey.ifBlank { "${parsedUrg.updatedAt}_${parsedUrg.message.hashCode()}" }
                                    if (sig.isNotBlank() && repo.getLastShownUrgentAlertId(sid) != sig) {
                                        repo.setLastShownUrgentAlertId(sig, sid)
                                        SystemNotificationHelper.showUrgentExternalNotification(
                                            context = appContext,
                                            schoolName = _schoolConfig.value.schoolName,
                                            urgentMessage = parsedUrg.message,
                                            signatureKey = sig
                                        )
                                    }
                                }
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_HOLIDAYS)) {
                            val remoteHolRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_HOLIDAYS)
                            if (!remoteHolRaw.isNullOrBlank() &&
                                remoteHolRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HOLIDAYS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_HOLIDAYS, remoteHolRaw)
                                val parsedHol = repo.parseHolidaysConfigJson(remoteHolRaw, sid)
                                if (parsedHol != _holidaysConfig.value) {
                                    _holidaysConfig.value = parsedHol
                                    _schoolConfig.value = _schoolConfig.value.copy(holidaysConfig = parsedHol)
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
                            val remoteCmtRaw = if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS)) {
                                FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS)
                            } else null
                            if (_schoolConfig.value.schoolId != sid) continue
                            var changed = false
                            if (!remoteCmtRaw.isNullOrBlank() &&
                                remoteCmtRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITY_COMMENTS, remoteCmtRaw)
                                changed = true
                            }
                            if (!remoteActRaw.isNullOrBlank() &&
                                (remoteActRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES) || changed)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_STUDENT_ACTIVITIES, remoteActRaw)
                                _studentActivityPosts.value = repo.parseStudentActivityPostsJson(remoteActRaw, sid)
                            } else if (changed) {
                                _studentActivityPosts.value = repo.loadStudentActivityPosts(sid)
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

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_TEACHER_RATINGS)) {
                            val remoteRatingsRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_TEACHER_RATINGS)
                            if (!remoteRatingsRaw.isNullOrBlank() && remoteRatingsRaw != "[]") {
                                repo.mergeEmbeddedTeacherRatingsIntoLocalCache(remoteRatingsRaw, sid)
                                _teacherRatings.value = repo.loadTeacherRatings(sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS)) {
                            val remoteCardsRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS)
                            if (!remoteCardsRaw.isNullOrBlank() &&
                                remoteCardsRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_ISSUED_REPORT_CARDS, remoteCardsRaw)
                                _issuedReportCards.value = repo.parseIssuedReportCardsJson(remoteCardsRaw, sid)
                            }
                        }

                        if (!FirebaseCloudSync.wasRecentlyWrittenLocally(sid, FirebaseConfigConstants.COL_BUS_TRACKING)) {
                            val remoteBusRaw = FirebaseCloudSync.fetchSubCollectionPayload(sid, FirebaseConfigConstants.COL_BUS_TRACKING)
                            if (!remoteBusRaw.isNullOrBlank() &&
                                remoteBusRaw != repo.getRawCachedSubCollection(sid, FirebaseConfigConstants.COL_BUS_TRACKING)
                            ) {
                                repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_BUS_TRACKING, remoteBusRaw)
                                runCatching {
                                    val busObj = org.json.JSONObject(remoteBusRaw)
                                    val evName = busObj.optString("eventType", "")
                                    val evTime = busObj.optString("timestamp", "")
                                    if (evName.isNotBlank() && evName != _schoolConfig.value.activeBusEvent) {
                                        _schoolConfig.value = _schoolConfig.value.copy(
                                            activeBusEvent = evName,
                                            activeBusEventTimestamp = evTime
                                        )
                                    }
                                }
                            }
                        }

                        flushPendingActivityCommentsToCloud()

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
        val sid = _schoolConfig.value.schoolId
        val schoolStudents = _students.value.filter { it.isActive && (it.schoolId.isEmpty() || it.schoolId == sid) }
        val studentClasses = schoolStudents
            .map { ArabicTextNormalizer.cleanPureClassName(it.gradeClass) }
            .filter { it.isNotEmpty() && !ArabicTextNormalizer.isBogusClassName(it) }
        val regClasses = (availableClasses + studentClasses)
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusClassName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeClass(it) }

        if (user == null || user.role == UserRole.ADMIN) return regClasses
        return when (user.role) {
            UserRole.TEACHER -> {
                val explicitTeacherClasses = user.parsedClassAndSectionEntries
                    .map { it.first }
                    .filter { it.isNotBlank() && !ArabicTextNormalizer.isAllClassesToken(it) && !ArabicTextNormalizer.isBogusClassName(it) }
                val matchedFromPool = regClasses.filter { cls -> user.canTeacherAccessClassSection(cls, null) }
                val combined = (explicitTeacherClasses + matchedFromPool)
                    .distinctBy { ArabicTextNormalizer.normalizeClass(it) }
                    .ifEmpty { regClasses }
                // Prioritize classes that actually have registered students assigned to this teacher
                combined.sortedByDescending { cls ->
                    schoolStudents.count { stu ->
                        ArabicTextNormalizer.matchesClass(stu.gradeClass, cls) &&
                            user.canTeacherAccessClassSection(stu.gradeClass, stu.sectionName)
                    }
                }
            }
            UserRole.PARENT, UserRole.STUDENT -> {
                val children = getChildrenForParent(user)
                if (children.isNotEmpty()) {
                    children.map { ArabicTextNormalizer.cleanPureClassName(it.gradeClass) }
                        .distinctBy { ArabicTextNormalizer.normalizeClass(it) }
                } else {
                    val linkedStu = schoolStudents.firstOrNull { it.id == user.linkedStudentId }
                    if (linkedStu != null) listOf(ArabicTextNormalizer.cleanPureClassName(linkedStu.gradeClass))
                    else listOf(ArabicTextNormalizer.cleanPureClassName(user.gradeClass))
                }
            }
            else -> regClasses
        }
    }

    fun getAllowedSectionsForClass(gradeClass: String, user: UserAccount? = _currentUser.value): List<String> {
        val sid = _schoolConfig.value.schoolId
        val classStudents = _students.value.filter {
            it.isActive &&
                (it.schoolId.isEmpty() || it.schoolId == sid) &&
                ArabicTextNormalizer.matchesClass(it.gradeClass, gradeClass)
        }
        val studentSecs = classStudents
            .map { ArabicTextNormalizer.cleanPureSectionName(it.sectionName) }
            .filter { it.isNotEmpty() && !ArabicTextNormalizer.isBogusSectionName(it) }
        val regSections = (availableSections + studentSecs)
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusSectionName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeSection(it) }

        if (user == null || user.role == UserRole.ADMIN) return regSections
        return when (user.role) {
            UserRole.TEACHER -> {
                val explicitSecsForClass = user.parsedClassAndSectionEntries
                    .filter { (cls, _) -> ArabicTextNormalizer.matchesClass(cls, gradeClass) || ArabicTextNormalizer.isAllClassesToken(cls) }
                    .map { it.second }
                    .map { ArabicTextNormalizer.cleanPureSectionName(it) }
                    .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusSectionName(it) }
                val poolFiltered = regSections.filter { sec -> user.canTeacherAccessClassSection(gradeClass, sec) }
                val combined = (explicitSecsForClass + poolFiltered)
                    .distinctBy { ArabicTextNormalizer.normalizeSection(it) }
                    .ifEmpty { regSections }
                // Prioritize sections that actually have registered students in this class
                combined.sortedByDescending { sec ->
                    classStudents.count { stu ->
                        ArabicTextNormalizer.matchesSection(stu.sectionName, sec)
                    }
                }
            }
            UserRole.PARENT, UserRole.STUDENT -> {
                val children = getChildrenForParent(user).filter {
                    ArabicTextNormalizer.matchesClass(it.gradeClass, gradeClass)
                }
                if (children.isNotEmpty()) {
                    children.map { ArabicTextNormalizer.cleanPureSectionName(it.sectionName) }
                        .distinctBy { ArabicTextNormalizer.normalizeSection(it) }
                } else {
                    val linkedStu = _students.value.firstOrNull { it.id == user.linkedStudentId }
                    if (linkedStu != null) listOf(ArabicTextNormalizer.cleanPureSectionName(linkedStu.sectionName))
                    else listOf(ArabicTextNormalizer.cleanPureSectionName(user.sectionName))
                }
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
                .filter { cfg ->
                    cfg.assignedClasses.isEmpty() ||
                        cfg.assignedClasses.any { ArabicTextNormalizer.matchesClass(it, gradeClass) }
                }
                .map { it.name }
                .ifEmpty { availableSubjects }
        } else {
            availableSubjects
        }

        if (user != null && user.role == UserRole.TEACHER) {
            val teacherSubs = user.teacherSubjectsList
            if (teacherSubs.isNotEmpty()) {
                val intersection = classSubjects.filter { sub -> user.canTeacherAccessSubject(sub) }
                return (intersection + teacherSubs)
                    .distinctBy { ArabicTextNormalizer.normalizeText(it) }
                    .ifEmpty { teacherSubs }
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
        val cleanSub = subjectName.trim()
        val existingGradeScale = _grades.value.firstOrNull {
            ArabicTextNormalizer.matchesClass(it.gradeClass, gradeClass) &&
                it.subjectName.equals(cleanSub, ignoreCase = true)
        }?.maxScoreScale
        if (existingGradeScale == 10 || existingGradeScale == 100) {
            return existingGradeScale
        }
        val subjCfg = _schoolConfig.value.subjectConfigs.firstOrNull {
            it.name.equals(cleanSub, ignoreCase = true) &&
                (it.assignedClasses.isEmpty() || it.assignedClasses.any { c -> ArabicTextNormalizer.matchesClass(c, gradeClass) })
        } ?: _schoolConfig.value.subjectConfigs.firstOrNull {
            it.name.equals(cleanSub, ignoreCase = true)
        }
        if (subjCfg != null && (subjCfg.defaultMaxScore == 10 || subjCfg.defaultMaxScore == 100)) {
            return subjCfg.defaultMaxScore
        }
        if (gradeClass.trim() in GradingRules.tenPointPrimaryClasses) {
            return 10
        }
        return GradingRules.defaultMaxScoreForClass(gradeClass)
    }

    /**
     * Persists the chosen max score scale (10 or 100) for a given class and subject in both
     * SubjectConfigItem and any existing SubjectGrade records so all screens stay synchronized.
     */
    fun updateClassSubjectMaxScoreScale(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        newScale: Int
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val validScale = if (newScale == 10) 10 else 100
        val cleanSub = InputSanitizer.sanitize(subjectName.trim())
        if (cleanSub.isBlank()) return
        val sid = _schoolConfig.value.schoolId

        // 1. Update SubjectConfigItem in schoolConfig
        val currentConfigs = _schoolConfig.value.subjectConfigs.toMutableList()
        val cfgIdx = currentConfigs.indexOfFirst { it.name.equals(cleanSub, ignoreCase = true) }
        if (cfgIdx >= 0) {
            currentConfigs[cfgIdx] = currentConfigs[cfgIdx].copy(defaultMaxScore = validScale)
        } else {
            currentConfigs.add(
                SubjectConfigItem(
                    id = "sub_${UUID.randomUUID().toString().take(6)}",
                    name = cleanSub,
                    assignedClasses = if (gradeClass.isNotBlank()) listOf(gradeClass.trim()) else emptyList(),
                    defaultMaxScore = validScale
                )
            )
        }
        val updatedConfig = _schoolConfig.value.copy(subjectConfigs = currentConfigs)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)

        // 2. Update any existing SubjectGrade records for this class and subject
        var changedGrades = false
        val updatedGrades = _grades.value.map { g ->
            if (g.subjectName.equals(cleanSub, ignoreCase = true) &&
                (gradeClass.isBlank() || ArabicTextNormalizer.matchesClass(g.gradeClass, gradeClass))
            ) {
                changedGrades = true
                g.copy(maxScoreScale = validScale)
            } else {
                g
            }
        }
        if (changedGrades) {
            _grades.value = updatedGrades
            repo.saveGrades(updatedGrades, sid)
        }
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
                (cleanParentPhone.length >= 10 && stu.parentPhone.trim() == cleanParentPhone)
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

    fun openHolidaysManagementModal() {
        _showHolidaysModal.value = true
    }

    fun closeHolidaysManagementModal() {
        _showHolidaysModal.value = false
    }

    private fun resolveCanonicalArabicDayOfWeek(dateIso: String): String {
        val clean = ArabicTextNormalizer.normalizeDigitsToAscii(dateIso.trim())
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val d = sdf.parse(clean) ?: Date()
            val cal = java.util.Calendar.getInstance().apply { time = d }
            when (cal.get(java.util.Calendar.DAY_OF_WEEK)) {
                java.util.Calendar.SUNDAY -> "الأحد"
                java.util.Calendar.MONDAY -> "الإثنين"
                java.util.Calendar.TUESDAY -> "الثلاثاء"
                java.util.Calendar.WEDNESDAY -> "الأربعاء"
                java.util.Calendar.THURSDAY -> "الخميس"
                java.util.Calendar.FRIDAY -> "الجمعة"
                java.util.Calendar.SATURDAY -> "السبت"
                else -> "الأحد"
            }
        } catch (_: Exception) {
            ""
        }
    }

    private fun normalizeCanonicalDateKey(rawDate: String): String {
        val ascii = ArabicTextNormalizer.normalizeDigitsToAscii(rawDate.trim())
        if (ascii.isBlank()) return ""
        val parts = ascii.split("-", "/", ".").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size == 3) {
            val p0 = parts[0]
            val p1 = parts[1]
            val p2 = parts[2]
            if (p0.length == 4) {
                return "${p0}-${p1.padStart(2, '0')}-${p2.padStart(2, '0')}"
            } else if (p2.length == 4) {
                return "${p2}-${p1.padStart(2, '0')}-${p0.padStart(2, '0')}"
            }
        }
        val digits = ascii.filter { it.isDigit() }
        if (digits.length == 8) {
            val first4 = digits.substring(0, 4).toIntOrNull() ?: 0
            return if (first4 in 1900..2100) {
                "${digits.substring(0, 4)}-${digits.substring(4, 6)}-${digits.substring(6, 8)}"
            } else {
                "${digits.substring(4, 8)}-${digits.substring(2, 4)}-${digits.substring(0, 2)}"
            }
        }
        return ArabicTextNormalizer.normalizeDateIso(ascii)
    }

    fun areDatesEquivalent(dateA: String, dateB: String): Boolean {
        val normA = normalizeCanonicalDateKey(dateA)
        val normB = normalizeCanonicalDateKey(dateB)
        if (normA.isNotEmpty() && normB.isNotEmpty() && normA == normB) return true
        val digitsA = ArabicTextNormalizer.normalizeDigitsToAscii(dateA).filter { it.isDigit() }
        val digitsB = ArabicTextNormalizer.normalizeDigitsToAscii(dateB).filter { it.isDigit() }
        return digitsA.length >= 6 && digitsA == digitsB
    }

    fun getHolidayLabelForDate(dateIso: String): String? {
        val cleanDate = normalizeCanonicalDateKey(dateIso)
        if (cleanDate.isBlank()) return null
        val cfg = _holidaysConfig.value
        val official = cfg.officialHolidays.firstOrNull {
            areDatesEquivalent(it.dateIso, cleanDate)
        }
        if (official != null) {
            return official.title.ifBlank { "عطلة رسمية" }
        }
        val dayName = resolveCanonicalArabicDayOfWeek(cleanDate)
        if (dayName.isNotBlank()) {
            val normDay = ArabicTextNormalizer.normalizeText(dayName)
            val isWeeklyOff = cfg.weeklyDaysOff.any {
                ArabicTextNormalizer.normalizeText(it) == normDay
            }
            if (isWeeklyOff) {
                return "عطلة أسبوعية - $dayName"
            }
        }
        return null
    }

    fun isDateHoliday(dateIso: String): Boolean = getHolidayLabelForDate(dateIso) != null

    private fun documentHolidayInStudentRecords(dateIso: String, holidayTitle: String) {
        val sid = _schoolConfig.value.schoolId
        if (sid.isBlank()) return
        val cleanDate = ArabicTextNormalizer.normalizeDigitsToAscii(dateIso.trim())
        val schoolStudents = _students.value.filter { (it.schoolId.isEmpty() || it.schoolId == sid) && it.isActive }
        if (schoolStudents.isEmpty()) return

        val holidayEntries = schoolStudents.map { stu ->
            val stuId = stu.id.trim()
            AttendanceEntry(
                id = "attendance_${cleanDate}_$stuId",
                studentId = stuId,
                studentName = stu.studentName,
                gradeClass = stu.gradeClass,
                sectionName = stu.sectionName,
                date = cleanDate,
                status = AttendanceStatus.HOLIDAY,
                note = "يوم عطلة رسمية ($holidayTitle) - موثق في السجل",
                schoolId = sid
            )
        }
        val remaining = _attendance.value.filterNot {
            ArabicTextNormalizer.normalizeDigitsToAscii(it.date.trim()) == cleanDate
        }
        val updated = holidayEntries + remaining
        _attendance.value = updated
        repo.saveAttendance(updated, sid)

        val updatedStudents = _students.value.map { stu ->
            if ((stu.schoolId.isEmpty() || stu.schoolId == sid) && stu.isActive) {
                stu.copy(dailyAttendance = stu.dailyAttendance + (cleanDate to AttendanceStatus.HOLIDAY.name))
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)
    }

    fun toggleWeeklyHolidayDay(dayName: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val normTarget = ArabicTextNormalizer.normalizeText(dayName)
        val currentDays = _holidaysConfig.value.weeklyDaysOff
        val exists = currentDays.any { ArabicTextNormalizer.normalizeText(it) == normTarget }
        val nextDays = if (exists) {
            currentDays.filterNot { ArabicTextNormalizer.normalizeText(it) == normTarget }
        } else {
            currentDays + dayName
        }
        val nextCfg = _holidaysConfig.value.copy(
            weeklyDaysOff = nextDays,
            updatedAt = System.currentTimeMillis(),
            schoolId = sid
        )
        _holidaysConfig.value = nextCfg
        _schoolConfig.value = _schoolConfig.value.copy(holidaysConfig = nextCfg)
        repo.saveHolidaysConfig(nextCfg, sid)

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val todayDayName = resolveCanonicalArabicDayOfWeek(todayIso)
        if (!exists && ArabicTextNormalizer.normalizeText(todayDayName) == normTarget) {
            documentHolidayInStudentRecords(todayIso, "عطلة أسبوعية - $dayName")
        } else if (exists) {
            scrubCancelledHolidayStatuses()
        }
        showToastAlert(
            if (!exists) "تم اعتماد يوم ($dayName) كعطلة أسبوعية للمدرسة"
            else "تم إلغاء يوم ($dayName) من أيام العطلة الأسبوعية وفتح سجل الحضور فوراً",
            isSuccess = true
        )
    }

    /**
     * Immediately cleans up any stale HOLIDAY status from `_attendance` and `_students`
     * for dates that are no longer weekly or official holidays, unlocking attendance buttons right away.
     */
    private fun scrubCancelledHolidayStatuses() {
        val sid = _schoolConfig.value.schoolId
        if (sid.isBlank()) return
        val cleanedAtt = _attendance.value.filterNot { entry ->
            val dIso = ArabicTextNormalizer.normalizeDateIso(entry.date.trim())
            entry.status == AttendanceStatus.HOLIDAY && getHolidayLabelForDate(dIso) == null
        }
        if (cleanedAtt.size != _attendance.value.size) {
            _attendance.value = cleanedAtt
            repo.saveAttendance(cleanedAtt, sid)
        }

        var anyStudentModified = false
        val updatedStudents = _students.value.map { stu ->
            if (stu.schoolId.isEmpty() || stu.schoolId == sid) {
                val filteredDaily = linkedMapOf<String, String>()
                var mapChanged = false
                stu.dailyAttendance.forEach { (k, v) ->
                    val nk = ArabicTextNormalizer.normalizeDateIso(k)
                    val isStaleHoliday = v.trim().equals(AttendanceStatus.HOLIDAY.name, ignoreCase = true) &&
                        getHolidayLabelForDate(nk) == null
                    if (isStaleHoliday) {
                        mapChanged = true
                    } else if (nk.isNotEmpty() && v.isNotBlank()) {
                        filteredDaily[nk] = v.trim().uppercase()
                    }
                }
                val cleanTodayDate = ArabicTextNormalizer.normalizeDateIso(stu.todayStatusDate)
                val todayWasStaleHoliday = stu.todayStatus.trim().equals(AttendanceStatus.HOLIDAY.name, ignoreCase = true) &&
                    (cleanTodayDate.isEmpty() || getHolidayLabelForDate(cleanTodayDate) == null)
                if (mapChanged || todayWasStaleHoliday) {
                    anyStudentModified = true
                    stu.copy(
                        dailyAttendance = filteredDaily,
                        todayStatus = if (todayWasStaleHoliday) AttendanceStatus.UNRECORDED.name else stu.todayStatus
                    )
                } else stu
            } else stu
        }
        if (anyStudentModified) {
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)
        }
    }

    fun addOrUpdateOfficialHoliday(holidayId: String?, dateIso: String, title: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val cleanDate = ArabicTextNormalizer.normalizeDigitsToAscii(InputSanitizer.sanitize(dateIso).trim())
        val cleanTitle = InputSanitizer.sanitize(title).trim().ifBlank { "عطلة رسمية" }
        if (cleanDate.isBlank()) {
            showToastAlert("يرجى اختيار تاريخ العطلة الرسمية", isSuccess = false)
            return
        }
        val nowLabel = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val item = OfficialHolidayItem(
            id = holidayId?.takeIf { it.isNotBlank() } ?: "hol_$cleanDate",
            dateIso = cleanDate,
            title = cleanTitle,
            createdAt = nowLabel,
            schoolId = sid
        )
        val filtered = _holidaysConfig.value.officialHolidays.filterNot {
            it.id == item.id || ArabicTextNormalizer.normalizeDigitsToAscii(it.dateIso.trim()) == cleanDate
        }
        val nextList = (listOf(item) + filtered).sortedByDescending { it.dateIso }
        val nextCfg = _holidaysConfig.value.copy(
            officialHolidays = nextList,
            updatedAt = System.currentTimeMillis(),
            schoolId = sid
        )
        _holidaysConfig.value = nextCfg
        _schoolConfig.value = _schoolConfig.value.copy(holidaysConfig = nextCfg)
        repo.saveHolidaysConfig(nextCfg, sid)

        // Document the official holiday in all active students' attendance logs for this date
        documentHolidayInStudentRecords(cleanDate, cleanTitle)

        pushNotification(
            title = "إعلان عطلة رسمية: $cleanTitle",
            body = "تعلن إدارة (${_schoolConfig.value.schoolName}) أن يوم ($cleanDate) عطلة رسمية بمناسبة ($cleanTitle). تم توثيق العطلة في سجلات جميع الطلبة.",
            type = NotificationType.GENERAL,
            targetSection = AppSection.ATTENDANCE.name,
            targetTab = 0,
            targetItemId = item.id
        )

        showToastAlert("تم تثبيت وتوثيق عطلة ($cleanTitle) ليوم $cleanDate في سجلات جميع الطلبة", isSuccess = true)
    }

    fun deleteOfficialHoliday(holidayId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val target = _holidaysConfig.value.officialHolidays.firstOrNull { it.id == holidayId } ?: return
        val cleanDate = ArabicTextNormalizer.normalizeDigitsToAscii(target.dateIso.trim())
        val nextList = _holidaysConfig.value.officialHolidays.filterNot { it.id == holidayId }
        val nextCfg = _holidaysConfig.value.copy(
            officialHolidays = nextList,
            updatedAt = System.currentTimeMillis(),
            schoolId = sid
        )
        _holidaysConfig.value = nextCfg
        _schoolConfig.value = _schoolConfig.value.copy(holidaysConfig = nextCfg)
        repo.saveHolidaysConfig(nextCfg, sid)

        // Remove auto-documented HOLIDAY entries for that date if it is not a weekly holiday
        scrubCancelledHolidayStatuses()
        showToastAlert("تم حذف العطلة الرسمية (${target.title}) ليوم $cleanDate وفتح سجل الحضور فوراً", isSuccess = true)
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
        customSchoolId: String = "",
        adminPhone: String = "",
        ministryAffiliation: String = "وزارة التربية",
        schoolGenderType: String = "مدرسة بنين"
    ) {
        val cleanName = InputSanitizer.sanitize(schoolName)
        val cleanUser = InputSanitizer.sanitizeUsername(adminUsername)
        val cleanPass = InputSanitizer.sanitize(adminPassword)
        val cleanPhone = InputSanitizer.sanitize(adminPhone)
        if (cleanName.isBlank() || cleanUser.isBlank() || cleanPass.isBlank()) {
            showToastAlert("يرجى إدخال اسم المدرسة واسم المستخدم وكلمة المرور للمدير", isSuccess = false)
            return
        }

        val generatedSchoolId = InputSanitizer.sanitizeSchoolId(customSchoolId).ifBlank {
            repo.generateUniqueThreeDigitSchoolCode()
        }
        val prefixedAdminUsername = FirebaseConfigConstants.applySchoolPrefix(generatedSchoolId, cleanUser)
        val todayDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val finalAddress = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address))
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
            ministryAffiliation = ministryAffiliation.ifBlank { "وزارة التربية" },
            schoolGenderType = schoolGenderType.ifBlank { "مدرسة بنين" },
            registeredClasses = generatedClasses,
            registeredSections = finalSections,
            registeredSubjects = generatedSubjects,
            subjectConfigs = initialSubjectConfigs,
            logoBase64 = logoBase64,
            academicYear = "2026 - 2027",
            phone = cleanPhone,
            isSchoolOfficiallyActivated = true,
            schoolActivationCode = generatedSchoolId.ifBlank { "1001" }
        )
        repo.setActiveSchoolId(generatedSchoolId)
        repo.saveSchoolConfig(config)
        _schoolConfig.value = config
        repo.setSchoolSecurityGatePassed(true, generatedSchoolId)
        _verifiedGateSchoolCode.value = generatedSchoolId
        _isSchoolGatePassed.value = true

        // Job title for administration is exclusively "المدير" with Auto School-Code Prefix
        val adminAccount = UserAccount(
            id = "admin_${generatedSchoolId.lowercase()}",
            username = prefixedAdminUsername,
            password = cleanPass,
            fullName = InputSanitizer.sanitize(adminFullName).ifBlank { "المدير" },
            role = UserRole.ADMIN,
            isActive = true,
            phone = cleanPhone,
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

        _dailyBehaviors.value = emptyList()
        repo.saveDailyBehaviors(emptyList(), generatedSchoolId)

        _homeworkSubmissions.value = emptyList()
        repo.saveHomeworkSubmissions(emptyList(), generatedSchoolId)

        _quickQuizzes.value = emptyList()
        _quickQuizAnswers.value = emptyList()
        repo.saveQuickQuizzes(emptyList(), generatedSchoolId)

        _dismissalCalls.value = emptyList()
        repo.saveDismissalCalls(emptyList(), generatedSchoolId)

        _lostAndFoundItems.value = emptyList()
        repo.saveLostAndFoundItems(emptyList(), generatedSchoolId)

        _privateSuggestions.value = emptyList()
        repo.savePrivateSuggestions(emptyList(), generatedSchoolId)

        _studentActivityPosts.value = emptyList()
        repo.saveStudentActivityPosts(emptyList(), generatedSchoolId)

        _activityViolationReports.value = emptyList()
        repo.saveActivityViolationReports(emptyList(), generatedSchoolId)

        _userProfilesAndSanctions.value = emptyMap()
        repo.saveUserProfilesAndSanctions(emptyMap(), generatedSchoolId)

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
        registeredSubjects: List<String> = _schoolConfig.value.registeredSubjects,
        phone: String = _schoolConfig.value.phone,
        ministryAffiliation: String = _schoolConfig.value.ministryAffiliation,
        schoolGenderType: String = _schoolConfig.value.schoolGenderType
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
            address = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address)),
            schoolType = schoolType,
            schoolStage = schoolStage,
            ministryAffiliation = ministryAffiliation.ifBlank { "وزارة التربية" },
            schoolGenderType = schoolGenderType.ifBlank { "مدرسة بنين" },
            registeredClasses = finalClasses,
            registeredSections = finalSections,
            registeredSubjects = finalSubjects,
            subjectConfigs = updatedConfigs,
            logoBase64 = logoBase64.ifBlank { _schoolConfig.value.logoBase64 },
            phone = InputSanitizer.sanitize(phone)
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

        val cleanUser = InputSanitizer.sanitizeUsername(rawUser)
        val cleanPass = InputSanitizer.sanitize(rawPass)

        // Extract school code prefix (e.g. "105" from "105-1" or "105-ali")
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
                // Fast-path lightweight authentication (< 1s): fetch only settings, accounts, and subjects first
                val initialConnectingText = if (!knownSchoolName.isNullOrBlank()) {
                    "جاري الدخول السريع إلى $knownSchoolName..."
                } else {
                    "جاري التحقق السريع من الحساب..."
                }
                _loginConnectingMessage.value = initialConnectingText

                viewModelScope.launch {
                    val essentialPulled = repo.syncEssentialAuthFromCloud(extractedPrefix)
                    _loginConnectingMessage.value = null
                    if (essentialPulled) {
                        reloadSchoolDataIntoMemory(extractedPrefix)
                        val loginRes = completeMemberLogin(cleanUser, cleanPass, extractedPrefix)
                        if (loginRes.isSuccess) {
                            // Launch non-blocking background lazy sync for remaining collections
                            _isBackgroundSyncing.value = true
                            try {
                                refreshDeveloperCloudData()
                                repo.syncRemainingSchoolCollectionsInBackground(extractedPrefix)
                                reloadSchoolDataIntoMemory(extractedPrefix)
                            } catch (_: Exception) {
                            } finally {
                                _isBackgroundSyncing.value = false
                            }
                        }
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

        val matchedFromAccounts = _accounts.value.firstOrNull { acc ->
            val exactMatch = acc.username.equals(cleanUser, ignoreCase = true)
            val prefixedCandidate = if (currentSchoolId.isNotBlank()) {
                FirebaseConfigConstants.applySchoolPrefix(currentSchoolId, cleanUser)
            } else cleanUser
            val prefixedMatch = acc.username.equals(prefixedCandidate, ignoreCase = true)

            (exactMatch || prefixedMatch) &&
                com.example.util.SecurityShieldHelper.verifyPassword(cleanPass, acc.password) &&
                (currentSchoolId.isEmpty() || acc.schoolId.isEmpty() || acc.schoolId == currentSchoolId)
        }

        val matchedFromDriverConfig: UserAccount? = run {
            val cfg = _schoolConfig.value
            val prefixedCandidate = if (currentSchoolId.isNotBlank()) {
                FirebaseConfigConstants.applySchoolPrefix(currentSchoolId, cleanUser)
            } else cleanUser

            val matchedRouteItem = cfg.effectiveBusRoutesList.firstOrNull { route ->
                val rUser = route.driverUsername.trim()
                val rPass = route.driverPassword.trim()
                if (rUser.isEmpty() || rPass.isEmpty()) return@firstOrNull false
                val exactMatch = rUser.equals(cleanUser, ignoreCase = true)
                val prefixedMatch = rUser.equals(prefixedCandidate, ignoreCase = true)
                val strippedRouteUser = if (currentSchoolId.isNotBlank()) {
                    rUser.removePrefix("${currentSchoolId}_")
                } else rUser
                val strippedMatch = strippedRouteUser.equals(cleanUser, ignoreCase = true)
                (exactMatch || prefixedMatch || strippedMatch) &&
                    com.example.util.SecurityShieldHelper.verifyPassword(cleanPass, rPass)
            }

            if (matchedRouteItem != null) {
                UserAccount(
                    id = "acc_bus_driver_${currentSchoolId}_${matchedRouteItem.id}",
                    username = matchedRouteItem.driverUsername,
                    password = matchedRouteItem.driverPassword,
                    fullName = matchedRouteItem.driverName.ifBlank { cfg.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" } },
                    role = UserRole.TEACHER,
                    isActive = matchedRouteItem.isDriverAccountEnabled,
                    phone = matchedRouteItem.driverPhone.ifBlank { cfg.busDriverPhone },
                    specialty = matchedRouteItem.routeAreaName.ifBlank { "سائق الخط المدرسي" },
                    assignedClasses = matchedRouteItem.id,
                    schoolId = currentSchoolId,
                    isBusDriverAccount = true
                )
            } else {
                val cfgDriverUser = cfg.busDriverUsername.trim()
                val cfgDriverPass = cfg.busDriverPassword.trim()
                if (cfgDriverUser.isNotEmpty() && cfgDriverPass.isNotEmpty()) {
                    val exactMatch = cfgDriverUser.equals(cleanUser, ignoreCase = true)
                    val prefixedMatch = cfgDriverUser.equals(prefixedCandidate, ignoreCase = true)
                    if ((exactMatch || prefixedMatch) && com.example.util.SecurityShieldHelper.verifyPassword(cleanPass, cfgDriverPass)) {
                        UserAccount(
                            id = "acc_bus_driver_$currentSchoolId",
                            username = cfgDriverUser,
                            password = cfgDriverPass,
                            fullName = cfg.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" },
                            role = UserRole.TEACHER,
                            isActive = cfg.isDriverAccountEnabled,
                            phone = cfg.busDriverPhone,
                            specialty = cfg.busRouteAreaName.ifBlank { "سائق الخط المدرسي" },
                            assignedClasses = cfg.effectiveBusRoutesList.firstOrNull()?.id.orEmpty(),
                            schoolId = currentSchoolId,
                            isBusDriverAccount = true
                        )
                    } else null
                } else null
            }
        }

        val matched = matchedFromDriverConfig ?: matchedFromAccounts ?: run {
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

        // Check if the school subscription is active (isActive / !isSuspended)
        if (!isSchoolSubscriptionActive(resolvedSchoolId)) {
            forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
            return Result.failure(IllegalStateException("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية"))
        }

        if (matched.isBusDriverAccount && !_schoolConfig.value.isDriverAccountEnabled) {
            showToastAlert("حساب السائق المستقل غير مفعّل حالياً من قبل مدير المدرسة", isSuccess = false)
            return Result.failure(IllegalStateException("حساب السائق المستقل غير مفعّل حالياً من قبل مدير المدرسة"))
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

        val roleGreeting = if (matched.isBusDriverAccount) "سائق الخط المدرسي المعتمد" else matched.role.arabicTitle
        showToastAlert("مرحباً بك ${matched.fullName} ($roleGreeting)", isSuccess = true)
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

    fun forceLogout(reasonMessage: String = "تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية") {
        val currentSid = _schoolConfig.value.schoolId
        if (currentSid.isNotBlank()) {
            repo.clearLocalSchoolCacheOnly(currentSid)
        }
        repo.clearSession()
        repo.setSchoolSecurityGatePassed(false)
        _activeSession.value = null
        _currentUser.value = null
        _selectedParentChildId.value = ""
        _isDeveloperAuthenticated.value = false
        _isSchoolSetupUnlocked.value = false
        _isSchoolGatePassed.value = false
        _verifiedGateSchoolCode.value = ""
        _activeSection.value = AppSection.DASHBOARD
        showToastAlert(reasonMessage, isSuccess = false)
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
        photoBase64: String = "",
        canModerateActivities: Boolean = false,
        canManageCanteen: Boolean = false,
        gender: String = "ذكر",
        ageYears: Int = 0
    ): UserAccount? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val staffLabel = _schoolConfig.value.schoolStage.staffTitleSingular
        val cleanName = InputSanitizer.sanitize(fullName)
        if (cleanName.isBlank()) {
            showToastAlert("يرجى إدخال اسم $staffLabel الثلاثي", isSuccess = false)
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

        val tempHolder = UserAccount(
            id = "temp",
            username = cleanUsername,
            password = cleanPassword,
            fullName = cleanName,
            role = UserRole.TEACHER,
            assignedClasses = InputSanitizer.sanitize(assignedClasses)
        )
        val parsedPairs = tempHolder.parsedClassAndSectionEntries
        val normalizedAssignedStr = if (parsedPairs.isNotEmpty()) {
            parsedPairs.joinToString(" | ") { (cls, sec) ->
                if (sec.isBlank()) cls else "$cls - $sec"
            }
        } else {
            InputSanitizer.sanitize(assignedClasses)
        }
        val primaryClass = parsedPairs.firstOrNull()?.first ?: availableClasses.firstOrNull().orEmpty()
        val primarySec = parsedPairs.firstOrNull()?.second?.takeIf { it.isNotBlank() } ?: "أ"

        val newTeacher = UserAccount(
            id = "tch_${UUID.randomUUID().toString().take(6)}",
            username = cleanUsername,
            password = cleanPassword,
            fullName = cleanName,
            role = UserRole.TEACHER,
            isActive = true,
            phone = InputSanitizer.sanitize(phone),
            specialty = InputSanitizer.sanitize(specialty),
            assignedClasses = normalizedAssignedStr,
            gradeClass = primaryClass,
            sectionName = primarySec,
            address = "",
            photoBase64 = photoBase64,
            schoolId = sid,
            canModerateActivities = canModerateActivities,
            canManageCanteen = canManageCanteen,
            gender = gender.ifBlank { "ذكر" },
            ageYears = ageYears.coerceAtLeast(0)
        )
        val updated = listOf(newTeacher) + _accounts.value
        _accounts.value = updated
        repo.saveAccounts(updated, sid)
        showToastAlert("تم إنشاء حساب $staffLabel ($cleanName) باسم مستخدم ($cleanUsername)", isSuccess = true)
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
        photoBase64: String = "",
        canModerateActivities: Boolean? = null,
        canManageCanteen: Boolean? = null,
        gender: String? = null,
        ageYears: Int? = null
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val staffLabel = _schoolConfig.value.schoolStage.staffTitleSingular
        val rawUser = InputSanitizer.sanitizeUsername(username)
        val prefixedUser = if (rawUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawUser) else ""

        val tempHolder = UserAccount(
            id = "temp",
            username = prefixedUser,
            password = "",
            fullName = fullName,
            role = UserRole.TEACHER,
            assignedClasses = InputSanitizer.sanitize(assignedClasses)
        )
        val parsedPairs = tempHolder.parsedClassAndSectionEntries
        val normalizedAssignedStr = if (parsedPairs.isNotEmpty()) {
            parsedPairs.joinToString(" | ") { (cls, sec) ->
                if (sec.isBlank()) cls else "$cls - $sec"
            }
        } else {
            InputSanitizer.sanitize(assignedClasses)
        }
        val primaryClass = parsedPairs.firstOrNull()?.first
        val primarySec = parsedPairs.firstOrNull()?.second?.takeIf { it.isNotBlank() }

        val updated = _accounts.value.map { acc ->
            if (acc.id == teacherId && acc.role == UserRole.TEACHER) {
                acc.copy(
                    fullName = InputSanitizer.sanitize(fullName).ifBlank { acc.fullName },
                    specialty = InputSanitizer.sanitize(specialty),
                    assignedClasses = normalizedAssignedStr,
                    gradeClass = primaryClass ?: acc.gradeClass,
                    sectionName = primarySec ?: acc.sectionName,
                    phone = InputSanitizer.sanitize(phone).ifBlank { acc.phone },
                    username = prefixedUser.ifBlank { acc.username },
                    password = InputSanitizer.sanitize(password).ifBlank { acc.password },
                    photoBase64 = photoBase64.ifBlank { acc.photoBase64 },
                    schoolId = sid,
                    canModerateActivities = canModerateActivities ?: acc.canModerateActivities,
                    canManageCanteen = canManageCanteen ?: acc.canManageCanteen,
                    gender = gender?.takeIf { it.isNotBlank() } ?: acc.gender,
                    ageYears = ageYears?.coerceAtLeast(0) ?: acc.ageYears
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
        showToastAlert("تم تحديث بيانات وصلاحيات $staffLabel بنجاح", isSuccess = true)
    }

    fun getTeacherSupervisedStudentsCount(teacher: UserAccount): Int {
        val sid = _schoolConfig.value.schoolId
        val activeSchoolStudents = _students.value.filter { stu ->
            (stu.schoolId.isBlank() || stu.schoolId == sid) && stu.isActive
        }
        if (activeSchoolStudents.isEmpty()) return 0

        val directMatched = activeSchoolStudents.count { stu ->
            teacher.canTeacherAccessClassSection(stu.gradeClass, stu.sectionName)
        }
        if (directMatched > 0) return directMatched

        val teacherSubs = teacher.teacherSubjectsList
        if (teacherSubs.isNotEmpty()) {
            val classesForTeacherSubjects = _schoolConfig.value.subjectConfigs
                .filter { cfg: SubjectConfigItem ->
                    (cfg.schoolId.isBlank() || cfg.schoolId == sid) &&
                        teacherSubs.any { tSub: String ->
                            ArabicTextNormalizer.normalizeText(cfg.name) == ArabicTextNormalizer.normalizeText(tSub)
                        }
                }
                .flatMap { cfg: SubjectConfigItem -> cfg.assignedClasses }
                .distinct()

            if (classesForTeacherSubjects.isNotEmpty()) {
                val subjectClassMatched = activeSchoolStudents.count { stu: StudentRecord ->
                    classesForTeacherSubjects.any { assignedCls: String ->
                        ArabicTextNormalizer.matchesClass(stu.gradeClass, assignedCls)
                    }
                }
                if (subjectClassMatched > 0) return subjectClassMatched
            }
        }

        if (teacher.gradeClass.isNotBlank()) {
            val fallbackClassMatched = activeSchoolStudents.count { stu ->
                ArabicTextNormalizer.matchesClass(stu.gradeClass, teacher.gradeClass)
            }
            if (fallbackClassMatched > 0) return fallbackClassMatched
        }

        return activeSchoolStudents.size
    }

    fun getTeacherRealPerformanceMetrics(teacher: UserAccount): TeacherRealPerformanceMetrics {
        val supervisedCount = getTeacherSupervisedStudentsCount(teacher)
        val sid = _schoolConfig.value.schoolId
        val teacherSubs = teacher.teacherSubjectsList
        val attendanceActions = _attendance.value.count { rec ->
            (rec.schoolId.isBlank() || rec.schoolId == sid) &&
                teacher.canTeacherAccessClassSection(rec.gradeClass, "")
        }
        val homeworksCount = _homeworks.value.count { hw ->
            (hw.schoolId.isBlank() || hw.schoolId == sid) &&
                (hw.teacherName.trim() == teacher.fullName.trim() ||
                    (teacher.canTeacherAccessClassSection(hw.gradeClass, hw.sectionName) &&
                        (teacherSubs.isEmpty() || teacher.canTeacherAccessSubject(hw.subjectName))))
        }
        val gradedCount = _grades.value.count { g ->
            (g.schoolId.isBlank() || g.schoolId == sid) &&
                teacher.canTeacherAccessClassSection(g.gradeClass, "") &&
                (teacherSubs.isEmpty() || teacher.canTeacherAccessSubject(g.subjectName)) &&
                (g.dailyScore > 0 || g.month1Score > 0 || g.month2Score > 0 || g.midYearScore > 0 || g.finalExamScore > 0)
        }
        val ratingsForTeacher = _teacherRatings.value.filter { r ->
            (r.schoolId.isBlank() || r.schoolId == sid) &&
                (r.teacherId == teacher.id || r.teacherName.trim() == teacher.fullName.trim())
        }
        val avgRatingPercent = if (ratingsForTeacher.isNotEmpty()) {
            ((ratingsForTeacher.map { it.stars }.average() / 5.0) * 100.0).toInt().coerceIn(0, 100)
        } else {
            val totalOps = attendanceActions + homeworksCount + gradedCount
            if (totalOps > 0) (75 + (totalOps * 2)).coerceAtMost(98) else 0
        }
        val interactivePercent = run {
            val totalOps = attendanceActions + (homeworksCount * 3) + (gradedCount * 2)
            if (totalOps == 0) 0 else (65 + (totalOps * 2)).coerceIn(0, 99)
        }
        return TeacherRealPerformanceMetrics(
            supervisedStudentsCount = supervisedCount,
            commitmentRatePercent = avgRatingPercent,
            interactiveActivityPercent = interactivePercent,
            recordedAttendanceActionsCount = attendanceActions,
            publishedHomeworksCount = homeworksCount,
            gradedStudentsCount = gradedCount
        )
    }

    fun canUserManageCanteen(user: UserAccount? = _currentUser.value): Boolean {
        if (user == null) return false
        return user.role == UserRole.ADMIN || (user.role == UserRole.TEACHER && user.canManageCanteen)
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
        val parentInputWords = cleanParentInput.split(Regex("\\s+")).filter { it.isNotBlank() }
        val cleanPhoneInput = parentPhoneInput.trim()

        // Require at least 4 words in studentNameInput (Student + 3-word Father Name: الأب + الجد + اللقب)
        // so typing just a 1-word or 2-word student name NEVER triggers false sibling suggestions!
        val studentWords = studentNameInput.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val inferredFatherWords = if (studentWords.size >= 4) {
            studentWords.drop(1)
        } else emptyList()

        val matchedSiblings = schoolStudents.filter { existing ->
            val existingFatherClean = existing.parentName
                .replace("ولي أمر الطالب", "")
                .replace("ولي أمر", "")
                .replace("السيد", "")
                .trim()
            val existingFatherWords = existingFatherClean.split(Regex("\\s+")).filter { it.isNotBlank() }
            val existingStuWords = existing.studentName.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            val existingInferredFatherWords = if (existingStuWords.size >= 4) {
                existingStuWords.drop(1)
            } else emptyList()

            val phoneMatches = cleanPhoneInput.length >= 10 &&
                existing.parentPhone.trim() == cleanPhoneInput

            // Strictly require at least 3 words (الاسم الثلاثي أو الرباعي لولي الأمر) — NEVER match on a single first name!
            val parentNameMatches = if (parentInputWords.size >= 3 && existingFatherWords.size >= 3) {
                val normInput = parentInputWords.take(3).joinToString(" ") { ArabicTextNormalizer.normalizeText(it) }
                val normExisting = existingFatherWords.take(3).joinToString(" ") { ArabicTextNormalizer.normalizeText(it) }
                normInput == normExisting
            } else {
                false
            }

            val inferredFatherMatches = if (inferredFatherWords.size >= 3) {
                val normInferred = inferredFatherWords.take(3).joinToString(" ") { ArabicTextNormalizer.normalizeText(it) }
                val normExistingFather = if (existingFatherWords.size >= 3) {
                    existingFatherWords.take(3).joinToString(" ") { ArabicTextNormalizer.normalizeText(it) }
                } else ""
                val normExistingStuFather = if (existingInferredFatherWords.size >= 3) {
                    existingInferredFatherWords.take(3).joinToString(" ") { ArabicTextNormalizer.normalizeText(it) }
                } else ""
                (normExistingFather.isNotEmpty() && normInferred == normExistingFather) ||
                    (normExistingStuFather.isNotEmpty() && normInferred == normExistingStuFather)
            } else {
                false
            }

            phoneMatches || parentNameMatches || inferredFatherMatches
        }

        val primary = matchedSiblings.firstOrNull() ?: return null
        val allSameParent = schoolStudents.filter {
            it.parentUsername.equals(primary.parentUsername, ignoreCase = true) ||
                (primary.parentName.isNotBlank() &&
                    ArabicTextNormalizer.normalizeText(it.parentName) == ArabicTextNormalizer.normalizeText(primary.parentName))
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
        parentPassword: String,
        gender: String = "ذكر",
        ageYears: Int = 0,
        isBusSubscribed: Boolean = false,
        busRouteName: String = "الخط الصباحي والمسائي",
        busDriverName: String = "",
        busDriverPhone: String = "",
        busVehicleNumber: String = "",
        busRouteId: String = "",
        busVehicleTypeAndColor: String = ""
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
        val finalAddress = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address))
        val finalPhone = InputSanitizer.sanitize(parentPhone)
        val rawSUser = InputSanitizer.sanitizeUsername(studentUsername).ifBlank { "stu${(10..99).random()}" }
        val sUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawSUser)
        val sPass = InputSanitizer.sanitize(studentPassword).ifBlank { (1000..9999).random().toString() }
        val rawPUser = InputSanitizer.sanitizeUsername(parentUsername).ifBlank { "par${(10..99).random()}" }
        val pUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawPUser)
        val pPass = InputSanitizer.sanitize(parentPassword).ifBlank { (1000..9999).random().toString() }
        val cleanBusRoute = InputSanitizer.sanitize(busRouteName).ifBlank { "الخط الصباحي والمسائي" }
        val cleanDriverName = InputSanitizer.sanitize(busDriverName)
        val cleanDriverPhone = InputSanitizer.sanitize(busDriverPhone)
        val cleanVehicleNumber = InputSanitizer.sanitize(busVehicleNumber)
        val cleanRouteId = InputSanitizer.sanitize(busRouteId)
        val cleanVehicleTypeAndColor = InputSanitizer.sanitize(busVehicleTypeAndColor)

        val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
            gradeClass,
            sectionName
        )

        val newStudent = StudentRecord(
            id = stuId,
            studentName = cleanStuName,
            gradeClass = pureGradeClass,
            sectionName = pureSectionName,
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
            schoolId = sid,
            isBusSubscribed = isBusSubscribed,
            busRouteId = cleanRouteId,
            busRouteName = cleanBusRoute,
            busDriverName = cleanDriverName,
            busDriverPhone = cleanDriverPhone,
            busVehicleNumber = cleanVehicleNumber,
            busVehicleTypeAndColor = cleanVehicleTypeAndColor,
            gender = gender.ifBlank { "ذكر" },
            ageYears = ageYears.coerceAtLeast(0)
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
            gradeClass = pureGradeClass,
            sectionName = pureSectionName,
            address = finalAddress,
            photoBase64 = photoBase64,
            schoolId = sid,
            gender = gender.ifBlank { "ذكر" },
            ageYears = ageYears.coerceAtLeast(0)
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
                gradeClass = pureGradeClass,
                sectionName = pureSectionName,
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
            gradeClass = pureGradeClass,
            sectionName = pureSectionName,
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
        photoBase64: String = "",
        gender: String? = null,
        ageYears: Int? = null,
        isBusSubscribed: Boolean? = null,
        busRouteName: String? = null,
        busDriverName: String? = null,
        busDriverPhone: String? = null,
        busVehicleNumber: String? = null,
        busRouteId: String? = null,
        busVehicleTypeAndColor: String? = null
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val rawStuUser = InputSanitizer.sanitizeUsername(studentUsername)
        val prefixedStuUser = if (rawStuUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawStuUser) else ""
        val rawParUser = InputSanitizer.sanitizeUsername(parentUsername)
        val prefixedParUser = if (rawParUser.isNotBlank()) FirebaseConfigConstants.applySchoolPrefix(sid, rawParUser) else ""
        val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
            gradeClass,
            sectionName
        )

        val targetOldStudent = _students.value.firstOrNull { it.id == studentId }
        val oldParentUsername = targetOldStudent?.parentUsername.orEmpty()

        // Strictly update gradeClass and sectionName ONLY on the target studentId — never overwrite siblings!
        val updatedStudents = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    studentName = InputSanitizer.sanitize(studentName).ifBlank { s.studentName },
                    gradeClass = pureGradeClass,
                    sectionName = pureSectionName,
                    parentName = InputSanitizer.sanitize(parentName).ifBlank { s.parentName },
                    parentPhone = InputSanitizer.sanitize(parentPhone).ifBlank { s.parentPhone },
                    address = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address)),
                    studentUsername = prefixedStuUser.ifBlank { s.studentUsername },
                    studentPassword = InputSanitizer.sanitize(studentPassword).ifBlank { s.studentPassword },
                    parentUsername = prefixedParUser.ifBlank { s.parentUsername },
                    parentPassword = InputSanitizer.sanitize(parentPassword).ifBlank { s.parentPassword },
                    photoBase64 = photoBase64.ifBlank { s.photoBase64 },
                    schoolId = sid,
                    isBusSubscribed = isBusSubscribed ?: s.isBusSubscribed,
                    busRouteId = busRouteId?.let { InputSanitizer.sanitize(it) } ?: s.busRouteId,
                    busRouteName = busRouteName?.let { InputSanitizer.sanitize(it).ifBlank { "الخط الصباحي والمسائي" } } ?: s.busRouteName,
                    busDriverName = busDriverName?.let { InputSanitizer.sanitize(it) } ?: s.busDriverName,
                    busDriverPhone = busDriverPhone?.let { InputSanitizer.sanitize(it) } ?: s.busDriverPhone,
                    busVehicleNumber = busVehicleNumber?.let { InputSanitizer.sanitize(it) } ?: s.busVehicleNumber,
                    busVehicleTypeAndColor = busVehicleTypeAndColor?.let { InputSanitizer.sanitize(it) } ?: s.busVehicleTypeAndColor,
                    gender = gender?.takeIf { it.isNotBlank() } ?: s.gender,
                    ageYears = ageYears?.coerceAtLeast(0) ?: s.ageYears
                )
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val updatedAccounts = _accounts.value.map { acc ->
            if (acc.role == UserRole.STUDENT && acc.linkedStudentId == studentId) {
                acc.copy(
                    fullName = InputSanitizer.sanitize(studentName).ifBlank { acc.fullName },
                    username = prefixedStuUser.ifBlank { acc.username },
                    password = InputSanitizer.sanitize(studentPassword).ifBlank { acc.password },
                    phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                    gradeClass = pureGradeClass,
                    sectionName = pureSectionName,
                    address = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address)),
                    photoBase64 = photoBase64.ifBlank { acc.photoBase64 },
                    schoolId = sid,
                    gender = gender?.takeIf { it.isNotBlank() } ?: acc.gender,
                    ageYears = ageYears?.coerceAtLeast(0) ?: acc.ageYears
                )
            } else if (acc.role == UserRole.PARENT &&
                (acc.linkedStudentId == studentId ||
                    (oldParentUsername.isNotBlank() && acc.username.equals(oldParentUsername, ignoreCase = true)))
            ) {
                acc.copy(
                    fullName = InputSanitizer.sanitize(parentName).ifBlank { acc.fullName },
                    username = prefixedParUser.ifBlank { acc.username },
                    password = InputSanitizer.sanitize(parentPassword).ifBlank { acc.password },
                    phone = InputSanitizer.sanitize(parentPhone).ifBlank { acc.phone },
                    gradeClass = if (acc.linkedStudentId == studentId) pureGradeClass else acc.gradeClass,
                    sectionName = if (acc.linkedStudentId == studentId) pureSectionName else acc.sectionName,
                    address = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(address)),
                    photoBase64 = if (acc.linkedStudentId == studentId && photoBase64.isNotBlank()) photoBase64 else acc.photoBase64,
                    schoolId = sid
                )
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)

        val updatedTuitions = _tuitions.value.map { t ->
            if (t.studentId == studentId) {
                t.copy(
                    studentName = InputSanitizer.sanitize(studentName).ifBlank { t.studentName },
                    gradeClass = pureGradeClass,
                    sectionName = pureSectionName,
                    parentName = InputSanitizer.sanitize(parentName).ifBlank { t.parentName },
                    parentPhone = InputSanitizer.sanitize(parentPhone).ifBlank { t.parentPhone }
                )
            } else t
        }
        _tuitions.value = updatedTuitions
        repo.saveTuitions(updatedTuitions, sid)

        _currentUser.value?.let { curr ->
            if (curr.linkedStudentId == studentId) {
                _currentUser.value = updatedAccounts.firstOrNull { it.id == curr.id } ?: curr
            }
        }
        showToastAlert("تم تحديث بيانات وصورة الطالب وولي الأمر بنجاح", isSuccess = true)
    }

    fun transferStudentClassAndSection(
        studentId: String,
        newGradeClass: String,
        newSectionName: String,
        newGender: String = "ذكر",
        newAgeYears: Int = 0
    ) {
        val sid = _schoolConfig.value.schoolId
        val (pureGradeClass, pureSectionName) = ArabicTextNormalizer.normalizeAndExtractClass(
            newGradeClass,
            newSectionName
        )
        val cleanGender = if (newGender.trim() == "أنثى" || newGender.trim() == "انثى" || newGender.equals("FEMALE", ignoreCase = true)) {
            "أنثى"
        } else {
            "ذكر"
        }
        val cleanAge = newAgeYears.coerceAtLeast(0)

        val updatedStudents = _students.value.map { s ->
            if (s.id == studentId) {
                s.copy(
                    gradeClass = pureGradeClass.ifBlank { s.gradeClass },
                    sectionName = pureSectionName.ifBlank { s.sectionName },
                    gender = cleanGender,
                    ageYears = cleanAge
                )
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val updatedAccounts = _accounts.value.map { acc ->
            if (acc.linkedStudentId == studentId && acc.role == UserRole.STUDENT) {
                acc.copy(
                    gradeClass = pureGradeClass.ifBlank { acc.gradeClass },
                    sectionName = pureSectionName.ifBlank { acc.sectionName },
                    gender = cleanGender,
                    ageYears = cleanAge
                )
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)

        val updatedTuitions = _tuitions.value.map { t ->
            if (t.studentId == studentId) {
                t.copy(
                    gradeClass = pureGradeClass.ifBlank { t.gradeClass },
                    sectionName = pureSectionName.ifBlank { t.sectionName }
                )
            } else t
        }
        _tuitions.value = updatedTuitions
        repo.saveTuitions(updatedTuitions, sid)

        _currentUser.value?.let { curr ->
            if (curr.linkedStudentId == studentId && curr.role == UserRole.STUDENT) {
                _currentUser.value = curr.copy(
                    gradeClass = pureGradeClass.ifBlank { curr.gradeClass },
                    sectionName = pureSectionName.ifBlank { curr.sectionName },
                    gender = cleanGender,
                    ageYears = cleanAge
                )
            }
        }
        showToastAlert("تم تحديث الصف والشعبة والبيانات الشخصية للطالب بنجاح", isSuccess = true)
    }

    fun updateCurrentUserGenderAndAge(gender: String, ageYears: Int) {
        val curr = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val cleanGender = if (gender.trim() == "أنثى" || gender.trim() == "انثى" || gender.equals("FEMALE", ignoreCase = true)) {
            "أنثى"
        } else {
            "ذكر"
        }
        val cleanAge = ageYears.coerceAtLeast(0)

        val updatedAccounts = _accounts.value.map { acc ->
            if (acc.id == curr.id) {
                acc.copy(gender = cleanGender, ageYears = cleanAge)
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)
        _currentUser.value = curr.copy(gender = cleanGender, ageYears = cleanAge)

        if (curr.linkedStudentId.isNotBlank()) {
            val updatedStudents = _students.value.map { s ->
                if (s.id == curr.linkedStudentId) {
                    s.copy(gender = cleanGender, ageYears = cleanAge)
                } else s
            }
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)
        }
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
        targetItemId: String = "",
        isUrgent: Boolean = false,
        priority: String = "normal"
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
            schoolId = sid,
            isUrgent = isUrgent,
            priority = priority
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
        val user = _currentUser.value
        if (user != null && user.role == UserRole.TEACHER && !user.canTeacherAccessClassSection(student.gradeClass, student.sectionName)) {
            showToastAlert("صلاحياتك تقتصر على رصد الحضور للصفوف والشعب المسندة إليك", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(InputSanitizer.sanitize(date).trim())
        val holidayReason = getHolidayLabelForDate(cleanDate)
        if (status != AttendanceStatus.HOLIDAY && holidayReason != null) {
            showToastAlert("اليوم عطلة ($holidayReason) — السجل مقفل وموثق كعطلة رسمية لجميع الطلبة", isSuccess = false)
            return
        }
        val cleanStudentId = student.id.trim()
        val targetStuObj = _students.value.firstOrNull { it.id.trim() == cleanStudentId } ?: student

        // Guard 1: Check enrollment status (Temporary Suspension / Expelled / Deregistered / Transferred)
        if (targetStuObj.isAttendanceBlockedByEnrollmentStatus(cleanDate)) {
            val blockMsg = targetStuObj.getEnrollmentBlockBannerText(cleanDate)
                ?: "لا يمكن رصد الحضور بسبب حالة قيد الطالب (${targetStuObj.enrollmentStatus.arabicLabel})"
            showToastAlert(blockMsg, isSuccess = false)
            return
        }

        // Guard 2: Lock attendance after student has completed both Check-In and Check-Out for this day
        if (targetStuObj.isDayAttendanceLocked(cleanDate)) {
            showToastAlert(
                "اكتمل دوام الطالب لهذا اليوم (دخول ومغادرة موثقة — مقفل حتى يوم غد)",
                isSuccess = false
            )
            return
        }

        val cleanNote = InputSanitizer.sanitize(note).trim()
        val deterministicId = "attendance_${cleanDate}_$cleanStudentId"

        // Idempotent guard: check both _attendance and student.dailyAttendance for this normalized date
        val existingEntry = _attendance.value.firstOrNull {
            it.studentId.trim() == cleanStudentId &&
                ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanDate
        }
        val existingMapStatus = if (targetStuObj != null) {
            getStudentStatusForDate(targetStuObj, cleanDate).takeIf {
                targetStuObj.dailyAttendance.keys.any { k -> ArabicTextNormalizer.normalizeDateIso(k) == cleanDate } ||
                    ArabicTextNormalizer.normalizeDateIso(targetStuObj.todayStatusDate) == cleanDate
            }
        } else null

        if (existingEntry != null &&
            existingEntry.status == status &&
            existingMapStatus == status &&
            (cleanNote.isEmpty() || existingEntry.note == cleanNote)
        ) {
            showToastAlert("حالة (${status.arabicLabel}) للطالب ${student.studentName} مثبتة مسبقاً ليوم $cleanDate", isSuccess = true)
            return
        }

        val entry = AttendanceEntry(
            id = deterministicId,
            studentId = cleanStudentId,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            date = cleanDate,
            status = status,
            note = cleanNote.ifBlank { status.arabicLabel },
            schoolId = sid
        )

        val filtered = _attendance.value.filterNot {
            it.studentId.trim() == cleanStudentId &&
                ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanDate
        }
        val updated = listOf(entry) + filtered
        _attendance.value = updated
        repo.saveAttendance(updated, sid)

        // Update the student's persistent dailyAttendance map, todayStatus, todayStatusDate, and counters directly in StudentRecord
        val timeNowForManual = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val updatedStudents = _students.value.map { stu ->
            if (stu.id.trim() == cleanStudentId) {
                val updatedDaily = linkedMapOf<String, String>()
                stu.dailyAttendance.forEach { (k, v) ->
                    val nk = ArabicTextNormalizer.normalizeDateIso(k)
                    if (nk.isNotEmpty() && v.isNotBlank()) {
                        updatedDaily[nk] = v.trim().uppercase()
                    }
                }
                updatedDaily[cleanDate] = status.name
                val updatedGateLog = stu.dailyGateLog.toMutableMap()
                if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE) {
                    val existingGate = updatedGateLog[cleanDate]
                    if (existingGate == null || existingGate.checkInTime.isBlank()) {
                        updatedGateLog[cleanDate] = DailyGateLogEntry(
                            checkInTime = timeNowForManual,
                            checkOutTime = existingGate?.checkOutTime.orEmpty(),
                            gateStatus = existingGate?.gateStatus?.ifBlank { "inside" } ?: "inside",
                            departureNote = existingGate?.departureNote.orEmpty()
                        )
                    }
                }
                val newAbsenceDays = updatedDaily.count { (dKey, stVal) ->
                    stVal == AttendanceStatus.ABSENT.name && getHolidayLabelForDate(dKey) == null
                }
                val newExcusedDays = updatedDaily.count { (dKey, stVal) ->
                    stVal == AttendanceStatus.EXCUSED.name && getHolidayLabelForDate(dKey) == null
                }
                val newLateDays = updatedDaily.count { (dKey, stVal) ->
                    stVal == AttendanceStatus.LATE.name && getHolidayLabelForDate(dKey) == null
                }
                stu.copy(
                    dailyAttendance = updatedDaily,
                    dailyGateLog = updatedGateLog,
                    todayStatus = status.name,
                    todayStatusDate = cleanDate,
                    absenceDays = newAbsenceDays,
                    excusedDays = newExcusedDays,
                    lateDays = newLateDays
                )
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        if (status == AttendanceStatus.PRESENT || status == AttendanceStatus.LATE || status == AttendanceStatus.ABSENT || status == AttendanceStatus.EXCUSED) {
            if (repo.getFirstAttendanceTimestampForDate(cleanDate, sid) == 0L) {
                repo.setFirstAttendanceTimestampForDate(cleanDate, System.currentTimeMillis(), sid)
            }
        }

        showToastAlert("تم حفظ حالة (${status.arabicLabel}) للطالب ${student.studentName}", isSuccess = true)

        if (status == AttendanceStatus.ABSENT || status == AttendanceStatus.LATE) {
            pushNotification(
                title = "تنبيه ${status.arabicLabel}: ${student.studentName}",
                body = "تم رصد حالة (${status.arabicLabel}) للطالب ${student.studentName} في الصف ${student.gradeClass} (${student.sectionName}) بتاريخ $cleanDate.",
                type = NotificationType.ATTENDANCE,
                targetStudentId = cleanStudentId,
                targetSection = AppSection.ATTENDANCE.name,
                targetTab = 0,
                targetItemId = deterministicId
            )
        }
    }

    fun getFirstAttendanceTimestamp(dateIso: String): Long {
        val sid = _schoolConfig.value.schoolId
        val cleanDate = normalizeCanonicalDateKey(dateIso)
        if (sid.isBlank() || cleanDate.isBlank()) return 0L
        return repo.getFirstAttendanceTimestampForDate(cleanDate, sid)
    }

    fun toggleAutoAbsenceEnabled(enabled: Boolean) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val updatedFeatures = _featureSettings.value.copy(autoAbsenceEnabled = enabled).normalized()
        _featureSettings.value = updatedFeatures
        val updatedConfig = _schoolConfig.value.copy(featureSettings = updatedFeatures)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        showToastAlert(
            if (enabled) "تم تفعيل الاحتساب التلقائي للغياب بعد بدء الرصد الصباحي"
            else "تم إيقاف الاحتساب التلقائي للغياب",
            isSuccess = true
        )
    }

    fun updateAutoAbsenceDelayMinutes(minutes: Int) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val coerced = minutes.coerceIn(15, 720)
        val updatedFeatures = _featureSettings.value.copy(autoAbsenceDelayMinutes = coerced).normalized()
        _featureSettings.value = updatedFeatures
        val updatedConfig = _schoolConfig.value.copy(featureSettings = updatedFeatures)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        val hoursLabel = if (coerced % 60 == 0) "${coerced / 60} ساعة" else "$coerced دقيقة"
        showToastAlert("تم ضبط مهلة الاحتساب التلقائي للغياب إلى ($hoursLabel) من أول تحضير", isSuccess = true)
    }

    fun checkAndRunAutoAbsenceForDate(dateIso: String, forceNow: Boolean = false): Int {
        val sid = _schoolConfig.value.schoolId
        if (sid.isBlank()) return 0
        val cleanDate = normalizeCanonicalDateKey(dateIso)
        if (cleanDate.isBlank()) return 0
        if (isDateHoliday(cleanDate)) return 0
        val feat = _featureSettings.value
        if (!feat.autoAbsenceEnabled && !forceNow) return 0

        val firstTs = repo.getFirstAttendanceTimestampForDate(cleanDate, sid)
        if (firstTs <= 0L && !forceNow) return 0
        val delayMs = feat.autoAbsenceDelayMinutes.coerceIn(15, 720) * 60_000L
        val now = System.currentTimeMillis()
        if (!forceNow && (now - firstTs) < delayMs) return 0

        val activeStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) &&
                it.isActive &&
                !it.isAttendanceBlockedByEnrollmentStatus(cleanDate)
        }
        val unrecordedStudents = activeStudents.filter { stu ->
            getStudentStatusForDate(stu, cleanDate) == AttendanceStatus.UNRECORDED
        }
        if (unrecordedStudents.isEmpty()) return 0

        val newEntries = unrecordedStudents.map { stu ->
            val stuId = stu.id.trim()
            AttendanceEntry(
                id = "attendance_${cleanDate}_$stuId",
                studentId = stuId,
                studentName = stu.studentName,
                gradeClass = stu.gradeClass,
                sectionName = stu.sectionName,
                date = cleanDate,
                status = AttendanceStatus.ABSENT,
                note = "احتساب غياب تلقائي بعد انتهاء مهلة الحضور الصباحي",
                schoolId = sid
            )
        }
        val unrecordedIds = unrecordedStudents.map { it.id.trim() }.toSet()
        val remainingAtt = _attendance.value.filterNot {
            it.studentId.trim() in unrecordedIds &&
                normalizeCanonicalDateKey(it.date) == cleanDate
        }
        val mergedAtt = newEntries + remainingAtt
        _attendance.value = mergedAtt
        repo.saveAttendance(mergedAtt, sid)

        val updatedStudents = _students.value.map { stu ->
            if (stu.id.trim() in unrecordedIds) {
                val updatedDaily = linkedMapOf<String, String>()
                stu.dailyAttendance.forEach { (k, v) ->
                    val nk = normalizeCanonicalDateKey(k)
                    if (nk.isNotEmpty() && v.isNotBlank()) updatedDaily[nk] = v.trim().uppercase()
                }
                updatedDaily[cleanDate] = AttendanceStatus.ABSENT.name
                val newAbsenceDays = updatedDaily.count { (dKey, stVal) ->
                    stVal == AttendanceStatus.ABSENT.name && getHolidayLabelForDate(dKey) == null
                }
                stu.copy(
                    dailyAttendance = updatedDaily,
                    todayStatus = AttendanceStatus.ABSENT.name,
                    todayStatusDate = cleanDate,
                    absenceDays = newAbsenceDays
                )
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        showToastAlert(
            "تم احتساب الغياب التلقائي لـ (${unrecordedStudents.size}) طالب لم يُسجّل حضورهم بعد انتهاء المهلة الصباحية",
            isSuccess = true
        )
        return unrecordedStudents.size
    }

    /**
     * Scans a Student ID QR Code (or username/ID) at the Smart Gate:
     * - CHECK_IN: marks the student PRESENT ("حاضر"), records checkInTime & gateStatus = "inside",
     *   and sends an instant notification to the parent.
     * - CHECK_OUT: records checkOutTime & gateStatus = "departed" (preserving checkInTime),
     *   and sends an instant departure notification to the parent.
     */
    fun recordAttendanceByQrCode(
        qrOrUsername: String,
        date: String,
        scanMode: GateScanMode = GateScanMode.CHECK_IN
    ): Boolean {
        return recordAttendanceByQrCodeDetailed(qrOrUsername, date, scanMode) != null
    }

    /**
     * Smart Gate Check-In & Check-Out Barcode System:
     * Scans a Student ID QR Code (MADRASATI|schoolId|studentId|studentName|gradeClass or WALLET-...),
     * plays a beep + tactile vibration, updates attendance & dailyGateLog in Firestore,
     * sends an instant arrival or departure notification to the parent, and returns the updated StudentRecord.
     */
    fun recordAttendanceByQrCodeDetailed(
        qrOrUsername: String,
        date: String,
        scanMode: GateScanMode = GateScanMode.CHECK_IN,
        departureNote: String = "انصراف رسمي / مغادرة البوابة"
    ): StudentRecord? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return null
        val cleanInput = qrOrUsername.trim()
        if (cleanInput.isEmpty()) {
            showToastAlert("يرجى مسح أو إدخال رمز الباركود / الـ QR الخاص بالطالب", isSuccess = false)
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
                stu.walletBarcode.equals(cleanInput, ignoreCase = true) ||
                (extractedStuIdFromPipe.isNotEmpty() && stu.id.equals(extractedStuIdFromPipe, ignoreCase = true)) ||
                cleanInput.contains("|${stu.id}|", ignoreCase = true) ||
                stu.id.equals(cleanInput, ignoreCase = true) ||
                stu.studentUsername.equals(cleanInput, ignoreCase = true) ||
                cleanInput.endsWith("-${stu.id}", ignoreCase = true) ||
                stu.studentName.equals(cleanInput, ignoreCase = true) ||
                stu.fullNameWithFather.equals(cleanInput, ignoreCase = true)
        }

        if (matchedStudent == null) {
            showToastAlert("لم يتم العثور على طالب مطابق لرمز الباركود ($cleanInput)", isSuccess = false)
            return null
        }

        val cleanDate = ArabicTextNormalizer.normalizeDateIso(InputSanitizer.sanitize(date).trim()).ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val holidayReason = getHolidayLabelForDate(cleanDate)
        if (holidayReason != null) {
            showToastAlert(
                "نظام البوابة ومسح الباركود مغلق اليوم ($holidayReason) — لا يمكن تسجيل دخول أو مغادرة",
                isSuccess = false
            )
            return null
        }
        if (matchedStudent.isAttendanceBlockedByEnrollmentStatus(cleanDate)) {
            val blockMsg = matchedStudent.getEnrollmentBlockBannerText(cleanDate)
                ?: "قيد الطالب غير فعال حالياً (${matchedStudent.enrollmentStatus.arabicLabel})"
            showToastAlert(blockMsg, isSuccess = false)
            return null
        }
        if (matchedStudent.isDayAttendanceLocked(cleanDate)) {
            showToastAlert(
                "اكتمل دوام الطالب لهذا اليوم (دخول ومغادرة موثقة — مقفل حتى يوم غد)",
                isSuccess = false
            )
            return null
        }

        return if (scanMode == GateScanMode.CHECK_OUT) {
            recordStudentGateCheckOut(
                student = matchedStudent,
                date = cleanDate,
                departureNote = departureNote
            )
        } else {
            com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(appContext)

            val timeNow = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
            val deterministicId = "attendance_${cleanDate}_${matchedStudent.id}"
            val entry = AttendanceEntry(
                id = deterministicId,
                studentId = matchedStudent.id,
                studentName = matchedStudent.studentName,
                gradeClass = matchedStudent.gradeClass,
                sectionName = matchedStudent.sectionName,
                date = cleanDate,
                status = AttendanceStatus.PRESENT,
                note = "دخول البوابة الذكية ($timeNow)",
                schoolId = sid
            )

            val filtered = _attendance.value.filterNot {
                it.studentId.trim() == matchedStudent.id.trim() &&
                    ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanDate
            }
            val updated = listOf(entry) + filtered
            _attendance.value = updated
            repo.saveAttendance(updated, sid)

            var updatedStudentRecord: StudentRecord? = null
            val updatedStudents = _students.value.map { stu ->
                if (stu.id.trim() == matchedStudent.id.trim()) {
                    val updatedDaily = linkedMapOf<String, String>()
                    stu.dailyAttendance.forEach { (k, v) ->
                        val nk = ArabicTextNormalizer.normalizeDateIso(k)
                        if (nk.isNotEmpty() && v.isNotBlank()) updatedDaily[nk] = v.trim().uppercase()
                    }
                    updatedDaily[cleanDate] = AttendanceStatus.PRESENT.name
                    val updatedGateMap = stu.dailyGateLog.toMutableMap()
                    updatedGateMap[cleanDate] = DailyGateLogEntry(
                        checkInTime = timeNow,
                        checkOutTime = "",
                        gateStatus = "inside",
                        departureNote = ""
                    )
                    val newStu = stu.copy(
                        dailyAttendance = updatedDaily,
                        dailyGateLog = updatedGateMap,
                        todayStatus = AttendanceStatus.PRESENT.name,
                        todayStatusDate = cleanDate,
                        absenceDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.ABSENT.name && getHolidayLabelForDate(dKey) == null },
                        excusedDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.EXCUSED.name && getHolidayLabelForDate(dKey) == null },
                        lateDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.LATE.name && getHolidayLabelForDate(dKey) == null }
                    )
                    updatedStudentRecord = newStu
                    newStu
                } else stu
            }
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)

            if (repo.getFirstAttendanceTimestampForDate(cleanDate, sid) == 0L) {
                repo.setFirstAttendanceTimestampForDate(cleanDate, System.currentTimeMillis(), sid)
            }

            val arrivalMsg = "تسجيل دخول للمدرسة: وصل الطالب ${matchedStudent.fullNameWithFather} إلى المدرسة بأمان في تمام الساعة $timeNow."
            showToastAlert("تم تسجيل دخول الطالب (${matchedStudent.fullNameWithFather}) الساعة $timeNow وإبلاغ ولي أمره فوراً", isSuccess = true)

            pushNotification(
                title = "تسجيل دخول للمدرسة: ${matchedStudent.fullNameWithFather}",
                body = arrivalMsg,
                type = NotificationType.ATTENDANCE,
                targetStudentId = matchedStudent.id,
                targetRole = UserRole.PARENT.name,
                targetSection = AppSection.ATTENDANCE.name
            )
            updatedStudentRecord ?: matchedStudent
        }
    }

    /**
     * Records a Student Gate Check-Out (مغادرة / انصراف من بوابة المدرسة) either via QR/Barcode scan
     * or via the quick "تسجيل مغادرة" button on the student's attendance card.
     */
    fun recordStudentGateCheckOut(
        student: StudentRecord,
        date: String,
        departureNote: String = "انصراف رسمي / مغادرة البوابة"
    ): StudentRecord? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return null
        val sid = _schoolConfig.value.schoolId

        val timeNow = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(InputSanitizer.sanitize(date).trim()).ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        val holidayReason = getHolidayLabelForDate(cleanDate)
        if (holidayReason != null) {
            showToastAlert(
                "نظام البوابة ومسح الباركود مغلق اليوم ($holidayReason) — لا يمكن تسجيل دخول أو مغادرة",
                isSuccess = false
            )
            return null
        }
        com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(appContext)
        val latestStu = _students.value.firstOrNull { it.id.trim() == student.id.trim() } ?: student
        if (latestStu.isAttendanceBlockedByEnrollmentStatus(cleanDate)) {
            val blockMsg = latestStu.getEnrollmentBlockBannerText(cleanDate)
                ?: "قيد الطالب غير فعال حالياً (${latestStu.enrollmentStatus.arabicLabel})"
            showToastAlert(blockMsg, isSuccess = false)
            return null
        }
        if (latestStu.isDayAttendanceLocked(cleanDate)) {
            showToastAlert(
                "اكتمل دوام الطالب لهذا اليوم (دخول ومغادرة موثقة — مقفل حتى يوم غد)",
                isSuccess = false
            )
            return null
        }
        val cleanNote = InputSanitizer.sanitize(departureNote).ifBlank { "انصراف رسمي / مغادرة البوابة" }

        var updatedStudentRecord: StudentRecord? = null
        var effectiveCheckInTime = ""

        val updatedStudents = _students.value.map { stu ->
            if (stu.id.trim() == student.id.trim()) {
                val updatedDaily = linkedMapOf<String, String>()
                stu.dailyAttendance.forEach { (k, v) ->
                    val nk = ArabicTextNormalizer.normalizeDateIso(k)
                    if (nk.isNotEmpty() && v.isNotBlank()) updatedDaily[nk] = v.trim().uppercase()
                }
                if (!updatedDaily.containsKey(cleanDate) || updatedDaily[cleanDate] == AttendanceStatus.ABSENT.name) {
                    updatedDaily[cleanDate] = AttendanceStatus.PRESENT.name
                }
                val existingGate = stu.getGateLogForDate(cleanDate)
                effectiveCheckInTime = existingGate?.checkInTime?.takeIf { it.isNotBlank() } ?: timeNow
                val updatedGateMap = stu.dailyGateLog.toMutableMap()
                updatedGateMap[cleanDate] = DailyGateLogEntry(
                    checkInTime = effectiveCheckInTime,
                    checkOutTime = timeNow,
                    gateStatus = "departed",
                    departureNote = cleanNote
                )
                val newStu = stu.copy(
                    dailyAttendance = updatedDaily,
                    dailyGateLog = updatedGateMap,
                    todayStatus = updatedDaily[cleanDate] ?: AttendanceStatus.PRESENT.name,
                    todayStatusDate = cleanDate,
                    absenceDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.ABSENT.name && getHolidayLabelForDate(dKey) == null },
                    excusedDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.EXCUSED.name && getHolidayLabelForDate(dKey) == null },
                    lateDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.LATE.name && getHolidayLabelForDate(dKey) == null }
                )
                updatedStudentRecord = newStu
                newStu
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val deterministicId = "attendance_${cleanDate}_${student.id}"
        val existingEntry = _attendance.value.firstOrNull {
            it.studentId.trim() == student.id.trim() &&
                ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanDate
        }
        val entry = AttendanceEntry(
            id = deterministicId,
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            date = cleanDate,
            status = existingEntry?.status?.takeIf { it != AttendanceStatus.ABSENT } ?: AttendanceStatus.PRESENT,
            note = "الدخول: $effectiveCheckInTime | غادر المدرسة: $timeNow ($cleanNote)",
            schoolId = sid
        )
        val filtered = _attendance.value.filterNot {
            it.studentId.trim() == student.id.trim() &&
                ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanDate
        }
        val updatedAtt = listOf(entry) + filtered
        _attendance.value = updatedAtt
        repo.saveAttendance(updatedAtt, sid)

        val departureMsg = "اشعار مغادرة المدرسة: غادر الطالب ${student.fullNameWithFather} بوابة المدرسة الآن في تمام الساعة $timeNow."
        showToastAlert("تم تسجيل مغادرة الطالب (${student.fullNameWithFather}) الساعة $timeNow وإشعار ولي أمره فوراً", isSuccess = true)

        pushNotification(
            title = "اشعار مغادرة المدرسة: ${student.fullNameWithFather}",
            body = departureMsg,
            type = NotificationType.ATTENDANCE,
            targetStudentId = student.id,
            targetRole = UserRole.PARENT.name,
            targetSection = AppSection.ATTENDANCE.name
        )
        return updatedStudentRecord ?: student
    }

    /**
     * Parent submits an electronic leave request (طلب إجازة للطالب: مرضية / زمنية / عذر عائلي).
     */
    fun isDateCoveredByLeaveRequest(req: LeaveRequestItem, targetDateIso: String): Boolean {
        val cleanTarget = ArabicTextNormalizer.normalizeDateIso(targetDateIso.trim())
        val cleanStart = ArabicTextNormalizer.normalizeDateIso(req.leaveDate.trim())
        if (cleanTarget.isEmpty() || cleanStart.isEmpty()) return false
        if (cleanTarget == cleanStart) return true
        val daysCount = req.durationDays.coerceIn(1, 30)
        if (daysCount <= 1) return false
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDateObj = sdf.parse(cleanStart) ?: return false
            (0 until daysCount).any { offset ->
                val cal = java.util.Calendar.getInstance().apply {
                    time = startDateObj
                    add(java.util.Calendar.DAY_OF_YEAR, offset)
                }
                sdf.format(cal.time) == cleanTarget
            }
        } catch (_: Exception) {
            false
        }
    }

    fun getStudentLeaveRequestForDate(studentId: String, dateIso: String): LeaveRequestItem? {
        val cleanStuId = studentId.trim()
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(dateIso.trim()).ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }
        return _leaveRequests.value.firstOrNull { req ->
            req.studentId.trim() == cleanStuId && isDateCoveredByLeaveRequest(req, cleanDate)
        }
    }

    /**
     * Parent/Student submits an electronic leave request (طلب إجازة للطالب: مرضية / زمنية / عذر عائلي).
     * Strictly allows only ONE leave request per day per student.
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
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(
            InputSanitizer.sanitize(leaveDate).ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
        ).ifBlank {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        }

        // Enforce 1-per-day leave request & excused check
        val currentStatusForDate = getStudentStatusForDate(student, cleanDate)
        if (currentStatusForDate == AttendanceStatus.EXCUSED) {
            showToastAlert("الطالب مجاز رسمياً في تاريخ ($cleanDate) ولا حاجة لإرسال طلب جديد", isSuccess = false)
            return
        }
        val existingForDate = getStudentLeaveRequestForDate(student.id, cleanDate)
        if (existingForDate != null) {
            val msg = when (existingForDate.status) {
                LeaveRequestStatus.PENDING -> "الطلب بانتظار الموافقة — لا يمكن إرسال أكثر من طلب إجازة واحد في نفس اليوم ($cleanDate)"
                LeaveRequestStatus.APPROVED -> "تمت الموافقة مسبقاً على إجازة الطالب لتاريخ ($cleanDate)"
                LeaveRequestStatus.REJECTED -> "تم إرسال طلب إجازة لتاريخ ($cleanDate) مسبقاً وتم البت فيه (يُسمح بطلب واحد في اليوم)"
            }
            showToastAlert(msg, isSuccess = false)
            return
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

        showToastAlert("تم إرسال طلب الإجازة (${leaveType.arabicLabel}) إلى إدارة المدرسة وأساتذة الصف بنجاح", isSuccess = true)

        pushNotification(
            title = "طلب إجازة جديد: ${student.fullNameWithFather}",
            body = "قدم ولي أمر الطالب (${student.fullNameWithFather} - ${student.gradeClass} شعبة ${student.sectionName}) طلب ${leaveType.arabicLabel} ليوم $cleanDate (${item.durationDays} يوم) — السبب: $cleanReason",
            type = NotificationType.ATTENDANCE,
            targetRole = UserRole.ADMIN.name,
            targetSection = AppSection.ATTENDANCE.name
        )
        pushNotification(
            title = "طلب إجازة طالب في صفك: ${student.fullNameWithFather}",
            body = "يوجد طلب ${leaveType.arabicLabel} معلق للطالب (${student.fullNameWithFather} - ${student.gradeClass} شعبة ${student.sectionName}) ليوم $cleanDate — السبب: $cleanReason",
            type = NotificationType.ATTENDANCE,
            targetRole = UserRole.TEACHER.name,
            targetSection = AppSection.ATTENDANCE.name
        )
    }

    /**
     * Admin or authorized class Teacher approves or rejects a parent's leave request.
     * Upon approval, the student's attendance for that date is automatically and instantaneously
     * recorded as EXCUSED ("مجاز (غائب بعذر)") across StudentRecord.dailyAttendance, _attendance,
     * Dashboard counters, Attendance student card, and Parent/Student view.
     */
    fun respondToLeaveRequest(
        requestId: String,
        approve: Boolean,
        adminNote: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val targetReq = _leaveRequests.value.firstOrNull { it.id == requestId } ?: return
        val currentUser = _currentUser.value
        if (currentUser?.role == UserRole.TEACHER &&
            !currentUser.canTeacherAccessClassSection(targetReq.gradeClass, targetReq.sectionName)
        ) {
            showToastAlert("عفواً، يمكنك البت في طلبات إجازة طلاب صفوفك وشعبك المسندة فقط", isSuccess = false)
            return
        }
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
            val cleanStudentId = targetReq.studentId.trim()
            val cleanLeaveDate = ArabicTextNormalizer.normalizeDateIso(targetReq.leaveDate.trim()).ifBlank {
                SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            }
            val daysCount = targetReq.durationDays.coerceIn(1, 30)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val startDateObj = runCatching { sdf.parse(cleanLeaveDate) }.getOrNull()
            val datesToMark = if (startDateObj != null && daysCount > 1) {
                (0 until daysCount).map { offset ->
                    val cal = java.util.Calendar.getInstance().apply {
                        time = startDateObj
                        add(java.util.Calendar.DAY_OF_YEAR, offset)
                    }
                    sdf.format(cal.time)
                }
            } else {
                listOf(cleanLeaveDate)
            }
            val newEntries = datesToMark.map { dIso ->
                AttendanceEntry(
                    id = "attendance_${dIso}_$cleanStudentId",
                    studentId = cleanStudentId,
                    studentName = student?.studentName ?: targetReq.studentName,
                    gradeClass = student?.gradeClass?.ifBlank { targetReq.gradeClass } ?: targetReq.gradeClass,
                    sectionName = student?.sectionName?.ifBlank { targetReq.sectionName } ?: targetReq.sectionName,
                    date = dIso,
                    status = AttendanceStatus.EXCUSED,
                    note = "مجاز بعذر رسمي (${targetReq.leaveType.arabicLabel}): ${targetReq.reason}",
                    schoolId = sid
                )
            }
            val datesSet = datesToMark.toSet()
            val filteredAtt = _attendance.value.filterNot {
                it.studentId.trim() == cleanStudentId &&
                    ArabicTextNormalizer.normalizeDateIso(it.date.trim()) in datesSet
            }
            val updatedAtt = newEntries + filteredAtt
            _attendance.value = updatedAtt
            repo.saveAttendance(updatedAtt, sid)

            val excusedPairs = datesToMark.associateWith { AttendanceStatus.EXCUSED.name }
            val updatedStudents = _students.value.map { stu ->
                if (stu.id.trim() == cleanStudentId) {
                    val updatedDaily = linkedMapOf<String, String>()
                    stu.dailyAttendance.forEach { (k, v) ->
                        val nk = ArabicTextNormalizer.normalizeDateIso(k)
                        if (nk.isNotEmpty() && v.isNotBlank()) updatedDaily[nk] = v.trim().uppercase()
                    }
                    updatedDaily.putAll(excusedPairs)
                    stu.copy(
                        dailyAttendance = updatedDaily,
                        todayStatus = AttendanceStatus.EXCUSED.name,
                        todayStatusDate = cleanLeaveDate,
                        absenceDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.ABSENT.name && getHolidayLabelForDate(dKey) == null },
                        excusedDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.EXCUSED.name && getHolidayLabelForDate(dKey) == null },
                        lateDays = updatedDaily.count { (dKey, stVal) -> stVal == AttendanceStatus.LATE.name && getHolidayLabelForDate(dKey) == null }
                    )
                } else stu
            }
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)
        }

        val statusAction = if (approve) "تمت الموافقة على" else "تم رفض"
        showToastAlert("$statusAction طلب إجازة الطالب (${targetReq.studentName}) وتحديث حالة الدوام فوراً", isSuccess = approve)

        // Notify both Parent and Student immediately
        pushNotification(
            title = "$statusAction طلب الإجازة: ${targetReq.studentName}",
            body = if (approve) {
                "تمت الموافقة الرسمية على طلب (${targetReq.leaveType.arabicLabel}) للطالب ${targetReq.studentName} بتاريخ ${targetReq.leaveDate} وتحديث حالته إلى (مجاز اليوم رسمياً)."
            } else {
                "اعتذرت إدارة المدرسة عن قبول طلب الإجازة للطالب ${targetReq.studentName} لتاريخ ${targetReq.leaveDate}."
            },
            type = NotificationType.ATTENDANCE,
            targetStudentId = targetReq.studentId,
            targetSection = AppSection.ATTENDANCE.name
        )
    }

    // =========================================================================
    // SMART CANTEEN E-WALLET & POS (محفظة حانوت المدرسة ونقطة البيع الذكية السحابية)
    // =========================================================================

    fun findStudentByWalletOrQrCode(rawCodeOrId: String): StudentRecord? {
        val clean = rawCodeOrId.trim()
        if (clean.isEmpty()) return null
        val sid = _schoolConfig.value.schoolId
        val schoolStudents = _students.value.filter { it.schoolId.isEmpty() || it.schoolId == sid }
        // 1. Direct match by walletBarcode, qrCodeValue, id, or username
        schoolStudents.firstOrNull {
            it.walletBarcode.equals(clean, ignoreCase = true) ||
                it.qrCodeValue.equals(clean, ignoreCase = true) ||
                it.id.equals(clean, ignoreCase = true) ||
                it.studentUsername.equals(clean, ignoreCase = true)
        }?.let { return it }

        // 2. Parse WALLET-{schoolId}-{studentId}
        if (clean.uppercase().startsWith("WALLET-")) {
            val parts = clean.split("-")
            if (parts.size >= 3) {
                val extractedStuId = parts.drop(2).joinToString("-").trim()
                schoolStudents.firstOrNull { it.id.equals(extractedStuId, ignoreCase = true) }?.let { return it }
            }
        }

        // 3. Parse MADRASATI|{schoolId}|{studentId}|...
        if (clean.contains("|")) {
            val parts = clean.split("|")
            if (parts.size >= 3) {
                val extractedStuId = parts[2].trim()
                schoolStudents.firstOrNull { it.id.equals(extractedStuId, ignoreCase = true) }?.let { return it }
            }
        }
        return null
    }

    fun topUpStudentWallet(
        studentId: String,
        amountIqd: Int,
        note: String = "شحن رصيد نقدي في الحانوت المدرسي",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!canUserManageCanteen()) {
            val err = "عفواً، صلاحية شحن محفظة الحانوت مخصصة لمدير المدرسة أو الأستاذ المخوّل بإدارة الحانوت فقط"
            showToastAlert(err, isSuccess = false)
            onResult(false, err)
            return
        }
        if (amountIqd <= 0) {
            val err = "يرجى اختيار أو كتابة مبلغ شحن صحيح بالدينار العراقي"
            showToastAlert(err, isSuccess = false)
            onResult(false, err)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val targetStu = _students.value.firstOrNull { it.id == studentId } ?: run {
            val err = "لم يتم العثور على سجل الطالب"
            showToastAlert(err, isSuccess = false)
            onResult(false, err)
            return
        }
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nowTime = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val actor = _currentUser.value?.fullName?.takeIf { it.isNotBlank() } ?: "إدارة الحانوت"
        val newBalance = targetStu.walletBalance.coerceAtLeast(0) + amountIqd
        val tx = CanteenWalletTransaction(
            id = "wtx_${UUID.randomUUID().toString().take(8)}",
            studentId = targetStu.id,
            studentName = targetStu.fullNameWithFather,
            type = CanteenTransactionType.TOP_UP,
            amount = amountIqd,
            balanceAfter = newBalance,
            date = todayIso,
            timestamp = nowTime,
            actorName = actor,
            note = InputSanitizer.sanitize(note).ifBlank { "شحن رصيد محفظة الحانوت" }
        )
        val updatedStudents = _students.value.map { s ->
            if (s.id == targetStu.id) {
                s.copy(
                    walletBalance = newBalance,
                    walletTransactions = (listOf(tx) + s.walletTransactions).take(60)
                )
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val msg = "تم شحن محفظة الحانوت للطالب ${targetStu.fullNameWithFather} بمبلغ $amountIqd د.ع. الرصيد الحالي: $newBalance د.ع"
        com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 180L)
        showToastAlert(msg, isSuccess = true)
        pushNotification(
            title = "شحن محفظة الحانوت: ${targetStu.fullNameWithFather}",
            body = msg,
            type = NotificationType.TUITION,
            targetStudentId = targetStu.id
        )
        onResult(true, msg)
    }

    fun processCanteenPurchase(
        studentIdOrBarcode: String,
        amountIqd: Int,
        itemNote: String = "شراء من حانوت المدرسة",
        onResult: (Boolean, String, StudentRecord?) -> Unit = { _, _, _ -> }
    ) {
        if (!canUserManageCanteen()) {
            val err = "عفواً، صلاحية كاشير الحانوت مخصصة لمدير المدرسة أو الأستاذ المخوّل فقط"
            showToastAlert(err, isSuccess = false)
            onResult(false, err, null)
            return
        }
        if (amountIqd <= 0) {
            val err = "يرجى تحديد مبلغ الشراء بالدينار العراقي"
            showToastAlert(err, isSuccess = false)
            onResult(false, err, null)
            return
        }
        val sid = _schoolConfig.value.schoolId
        if (!_schoolConfig.value.isCanteenWalletEnabled || !_featureSettings.value.canteenWalletEnabled) {
            val disabledMsg = "نظام المحفظة والحانوت المدرسي متوقف مؤقتاً بقرار من إدارة المدرسة"
            com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 220L)
            showToastAlert(disabledMsg, isSuccess = false)
            onResult(false, disabledMsg, null)
            return
        }
        val targetStu = findStudentByWalletOrQrCode(studentIdOrBarcode) ?: run {
            val err = "لم يتم العثور على الطالب، تأكد من مسح باركود المحفظة الصحيح"
            showToastAlert(err, isSuccess = false)
            onResult(false, err, null)
            return
        }
        if (targetStu.isWalletSuspended) {
            val suspendedMsg = "محفظة الطالب (${targetStu.fullNameWithFather}) مجمدة وموقوفة مؤقتاً عن الشراء"
            com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 220L)
            showToastAlert(suspendedMsg, isSuccess = false)
            onResult(false, suspendedMsg, targetStu)
            return
        }
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nowTime = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val actor = _currentUser.value?.fullName?.takeIf { it.isNotBlank() } ?: "كاشير الحانوت"
        val currentBalance = targetStu.walletBalance.coerceAtLeast(0)
        val spentToday = targetStu.getSpentToday(todayIso)
        val dailyLimit = targetStu.dailySpendLimit.coerceAtLeast(0)

        // Check 1: Sufficient Wallet Balance
        if (currentBalance < amountIqd) {
            val rejectReason = "رفض العملية: الرصيد غير كافٍ (الرصيد المتوفر: $currentBalance د.ع، المطلوب: $amountIqd د.ع)"
            val rejectedTx = CanteenWalletTransaction(
                id = "wtx_${UUID.randomUUID().toString().take(8)}",
                studentId = targetStu.id,
                studentName = targetStu.fullNameWithFather,
                type = CanteenTransactionType.PURCHASE_REJECTED,
                amount = amountIqd,
                balanceAfter = currentBalance,
                date = todayIso,
                timestamp = nowTime,
                actorName = actor,
                note = rejectReason
            )
            var updatedStuRecord: StudentRecord? = null
            val updatedStudents = _students.value.map { s ->
                if (s.id == targetStu.id) {
                    val u = s.copy(walletTransactions = (listOf(rejectedTx) + s.walletTransactions).take(60))
                    updatedStuRecord = u
                    u
                } else s
            }
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)
            com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 220L)
            showToastAlert(rejectReason, isSuccess = false)
            pushNotification(
                title = "رفض عملية شراء من الحانوت: ${targetStu.fullNameWithFather}",
                body = "محاولة شراء مرفوضة للطالب ${targetStu.fullNameWithFather} بمبلغ $amountIqd د.ع — السبب: الرصيد غير كافٍ (الرصيد الحالي: $currentBalance د.ع).",
                type = NotificationType.TUITION,
                targetStudentId = targetStu.id
            )
            onResult(false, rejectReason, updatedStuRecord ?: targetStu)
            return
        }

        // Check 2: Parent's Daily Spend Limit (if > 0)
        if (dailyLimit > 0 && (spentToday + amountIqd) > dailyLimit) {
            val remainingAllowed = (dailyLimit - spentToday).coerceAtLeast(0)
            val rejectReason = "رفض العملية: تجاوز الحد اليومي المسموح من ولي الأمر (السقف اليومي: $dailyLimit د.ع، المصروف اليوم: $spentToday د.ع، المتبقي المسموح اليوم: $remainingAllowed د.ع)"
            val rejectedTx = CanteenWalletTransaction(
                id = "wtx_${UUID.randomUUID().toString().take(8)}",
                studentId = targetStu.id,
                studentName = targetStu.fullNameWithFather,
                type = CanteenTransactionType.PURCHASE_REJECTED,
                amount = amountIqd,
                balanceAfter = currentBalance,
                date = todayIso,
                timestamp = nowTime,
                actorName = actor,
                note = rejectReason
            )
            var updatedStuRecord: StudentRecord? = null
            val updatedStudents = _students.value.map { s ->
                if (s.id == targetStu.id) {
                    val u = s.copy(walletTransactions = (listOf(rejectedTx) + s.walletTransactions).take(60))
                    updatedStuRecord = u
                    u
                } else s
            }
            _students.value = updatedStudents
            repo.saveStudents(updatedStudents, sid)
            com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 220L)
            showToastAlert(rejectReason, isSuccess = false)
            pushNotification(
                title = "تنبيه تجاوز الحد اليومي للحانوت: ${targetStu.fullNameWithFather}",
                body = "محاولة شراء مرفوضة للطالب ${targetStu.fullNameWithFather} بمبلغ $amountIqd د.ع — السبب: تجاوز السقف اليومي المحدد من ولي الأمر ($dailyLimit د.ع).",
                type = NotificationType.TUITION,
                targetStudentId = targetStu.id
            )
            onResult(false, rejectReason, updatedStuRecord ?: targetStu)
            return
        }

        // Approved Purchase: Deduct & Sync Immediately
        val newBalance = currentBalance - amountIqd
        val newSpentToday = spentToday + amountIqd
        val updatedDailySpent = LinkedHashMap(targetStu.dailySpentMap).apply {
            put(todayIso, newSpentToday)
        }
        val cleanNote = InputSanitizer.sanitize(itemNote).ifBlank { "شراء من الحانوت المدرسي" }
        val approvedTx = CanteenWalletTransaction(
            id = "wtx_${UUID.randomUUID().toString().take(8)}",
            studentId = targetStu.id,
            studentName = targetStu.fullNameWithFather,
            type = CanteenTransactionType.PURCHASE_APPROVED,
            amount = amountIqd,
            balanceAfter = newBalance,
            date = todayIso,
            timestamp = nowTime,
            actorName = actor,
            note = cleanNote
        )
        var updatedStuRecord: StudentRecord? = null
        val updatedStudents = _students.value.map { s ->
            if (s.id == targetStu.id) {
                val u = s.copy(
                    walletBalance = newBalance,
                    dailySpentMap = updatedDailySpent,
                    walletTransactions = (listOf(approvedTx) + s.walletTransactions).take(60)
                )
                updatedStuRecord = u
                u
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val successMsg = "عملية شراء من الحانوت للطالب ${targetStu.fullNameWithFather} بمبلغ $amountIqd د.ع — الرصيد المتبقي: $newBalance د.ع"
        com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 180L)
        showToastAlert(successMsg, isSuccess = true)
        pushNotification(
            title = "عملية شراء من الحانوت: ${targetStu.fullNameWithFather}",
            body = successMsg,
            type = NotificationType.TUITION,
            targetStudentId = targetStu.id
        )
        onResult(true, successMsg, updatedStuRecord ?: targetStu)
    }

    fun updateStudentDailySpendLimit(studentId: String, dailyLimitIqd: Int) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.PARENT, UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val cleanLimit = dailyLimitIqd.coerceAtLeast(0)
        var studentName = ""
        val updatedStudents = _students.value.map { s ->
            if (s.id == studentId) {
                studentName = s.fullNameWithFather
                s.copy(dailySpendLimit = cleanLimit)
            } else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)
        val limitText = if (cleanLimit == 0) "مفتوح بدون سقف يومي" else "$cleanLimit د.ع يومياً"
        showToastAlert("تم حفظ الحد الأقصى للشراء اليومي للطالب ($studentName): $limitText", isSuccess = true)
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
     * Explicitly updates a student's school bus subscription status and optional route name.
     */
    fun updateStudentBusSubscription(
        studentId: String,
        isSubscribed: Boolean,
        busRouteName: String = "الخط الصباحي والمسائي",
        busDriverName: String = "",
        busDriverPhone: String = "",
        busVehicleNumber: String = "",
        busRouteId: String = "",
        busVehicleTypeAndColor: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val cleanRoute = InputSanitizer.sanitize(busRouteName).ifBlank {
            _schoolConfig.value.busRouteAreaName.ifBlank { "الخط الصباحي والمسائي" }
        }
        val cleanDriver = InputSanitizer.sanitize(busDriverName)
        val cleanPhone = InputSanitizer.sanitize(busDriverPhone)
        val cleanVehicle = InputSanitizer.sanitize(busVehicleNumber)
        val cleanRouteId = InputSanitizer.sanitize(busRouteId)
        val cleanVehicleType = InputSanitizer.sanitize(busVehicleTypeAndColor)
        var targetName = ""
        val updated = _students.value.map { s ->
            if (s.id == studentId) {
                targetName = s.fullNameWithFather
                s.copy(
                    isBusSubscribed = isSubscribed,
                    busRouteId = if (isSubscribed && cleanRouteId.isNotBlank()) cleanRouteId else s.busRouteId,
                    busRouteName = if (isSubscribed) cleanRoute else s.busRouteName,
                    busDriverName = if (isSubscribed && cleanDriver.isNotBlank()) cleanDriver else s.busDriverName,
                    busDriverPhone = if (isSubscribed && cleanPhone.isNotBlank()) cleanPhone else s.busDriverPhone,
                    busVehicleNumber = if (isSubscribed && cleanVehicle.isNotBlank()) cleanVehicle else s.busVehicleNumber,
                    busVehicleTypeAndColor = if (isSubscribed && cleanVehicleType.isNotBlank()) cleanVehicleType else s.busVehicleTypeAndColor
                )
            } else s
        }
        _students.value = updated
        repo.saveStudents(updated, sid)
        val statusMsg = if (isSubscribed) {
            "تم شمول الطالب ($targetName) في ($cleanRoute)"
        } else {
            "تم إلغاء شمول الطالب ($targetName) من النقل المدرسي"
        }
        showToastAlert(statusMsg, isSuccess = true)
    }

    fun updateSchoolBusDriverConfig(
        driverName: String,
        driverPhone: String,
        vehicleNumber: String,
        allowParentToViewDriverIdentity: Boolean,
        driverPhotoBase64: String = _schoolConfig.value.busDriverPhotoBase64,
        routeAreaName: String = _schoolConfig.value.busRouteAreaName,
        driverIdentityDetails: String = _schoolConfig.value.busDriverIdentityDetails,
        vehicleTypeAndColor: String = _schoolConfig.value.busVehicleTypeAndColor,
        driverUsername: String = _schoolConfig.value.busDriverUsername,
        driverPassword: String = _schoolConfig.value.busDriverPassword,
        isDriverAccountEnabled: Boolean = _schoolConfig.value.isDriverAccountEnabled
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val cleanDriverName = InputSanitizer.sanitize(driverName).ifBlank { "السائق المعتمد للخط المدرسي" }
        val cleanDriverPhone = InputSanitizer.sanitize(driverPhone)
        val cleanVehicleNumber = InputSanitizer.sanitize(vehicleNumber).ifBlank { "مركبة النقل المدرسي الحديثة" }
        val cleanRouteArea = InputSanitizer.sanitize(routeAreaName)
        val cleanIdentityDetails = InputSanitizer.sanitize(driverIdentityDetails)
        val cleanVehicleType = InputSanitizer.sanitize(vehicleTypeAndColor).ifBlank { "باص مدرسي حديث • أبيض وزمردي" }
        val rawUser = InputSanitizer.sanitizeUsername(driverUsername)
        val prefixedDriverUser = if (rawUser.isNotBlank()) {
            FirebaseConfigConstants.applySchoolPrefix(sid, rawUser)
        } else {
            _schoolConfig.value.busDriverUsername
        }
        val cleanDriverPass = InputSanitizer.sanitize(driverPassword).ifBlank {
            _schoolConfig.value.busDriverPassword.ifBlank { "2026" }
        }

        val currentRoutes = _schoolConfig.value.registeredBusRoutes.toMutableList()
        if (cleanRouteArea.isNotBlank()) {
            val existingMatch = currentRoutes.firstOrNull { it.routeAreaName.equals(cleanRouteArea, ignoreCase = true) }
            val primaryItem = BusRouteFleetItem(
                id = existingMatch?.id ?: "route_primary_1",
                routeAreaName = cleanRouteArea,
                vehicleTypeAndColor = cleanVehicleType,
                vehiclePlateNumber = cleanVehicleNumber,
                driverName = cleanDriverName,
                driverPhone = cleanDriverPhone,
                driverIdentityDetails = cleanIdentityDetails,
                driverUsername = prefixedDriverUser.ifBlank { existingMatch?.driverUsername.orEmpty() },
                driverPassword = cleanDriverPass.ifBlank { existingMatch?.driverPassword.orEmpty() },
                isDriverAccountEnabled = isDriverAccountEnabled
            )
            val existingIdx = currentRoutes.indexOfFirst {
                it.id == primaryItem.id || it.routeAreaName.equals(cleanRouteArea, ignoreCase = true)
            }
            if (existingIdx >= 0) {
                currentRoutes[existingIdx] = primaryItem
            } else {
                currentRoutes.add(0, primaryItem)
            }
        }

        val updatedConfig = _schoolConfig.value.copy(
            busDriverName = cleanDriverName,
            busDriverPhone = cleanDriverPhone,
            busVehicleNumber = cleanVehicleNumber,
            busVehicleTypeAndColor = cleanVehicleType,
            allowParentToViewDriverIdentity = allowParentToViewDriverIdentity,
            busDriverPhotoBase64 = driverPhotoBase64,
            busRouteAreaName = cleanRouteArea,
            busDriverIdentityDetails = cleanIdentityDetails,
            isDriverAccountEnabled = isDriverAccountEnabled,
            busDriverUsername = prefixedDriverUser,
            busDriverPassword = cleanDriverPass,
            registeredBusRoutes = currentRoutes
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)

        syncAllFleetDriverAccounts(updatedConfig)

        showToastAlert("تم حفظ واعتماد بيانات الخط والمركبة وحساب السائق الموحد بنجاح", isSuccess = true)
    }

    fun addOrUpdateBusRouteFleetItem(
        routeId: String = "",
        routeAreaName: String,
        vehicleTypeAndColor: String,
        vehiclePlateNumber: String,
        driverName: String,
        driverPhone: String,
        driverIdentityDetails: String = "",
        driverUsername: String = "",
        driverPassword: String = "",
        isDriverAccountEnabled: Boolean = true
    ): BusRouteFleetItem? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val sid = _schoolConfig.value.schoolId
        val cleanRouteArea = InputSanitizer.sanitize(routeAreaName).trim()
        if (cleanRouteArea.isEmpty()) {
            showToastAlert("يرجى إدخال اسم أو منطقة الخط المدرسي أولاً", isSuccess = false)
            return null
        }
        val cleanVehicleType = InputSanitizer.sanitize(vehicleTypeAndColor).ifBlank { "باص مدرسي حديث • أبيض وزمردي" }
        val cleanPlate = InputSanitizer.sanitize(vehiclePlateNumber).ifBlank { "مركبة النقل المدرسي الحديثة" }
        val cleanDriver = InputSanitizer.sanitize(driverName).ifBlank { "السائق المعتمد للخط المدرسي" }
        val cleanPhone = InputSanitizer.sanitize(driverPhone)
        val cleanIdentity = InputSanitizer.sanitize(driverIdentityDetails)

        val currentList = _schoolConfig.value.registeredBusRoutes.toMutableList()
        val resolvedId = routeId.ifBlank {
            currentList.firstOrNull { it.routeAreaName.equals(cleanRouteArea, ignoreCase = true) }?.id
                ?: "route_${UUID.randomUUID().toString().take(6)}"
        }
        val existingRoute = currentList.firstOrNull {
            it.id == resolvedId || it.routeAreaName.equals(cleanRouteArea, ignoreCase = true)
        }

        val rawUser = InputSanitizer.sanitizeUsername(driverUsername).ifBlank {
            existingRoute?.driverUsername?.ifBlank { null }
                ?: "driver${currentList.size + 1}"
        }
        val prefixedDriverUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawUser)
        val cleanDriverPass = InputSanitizer.sanitize(driverPassword).ifBlank {
            existingRoute?.driverPassword?.ifBlank { null }
                ?: _schoolConfig.value.busDriverPassword.ifBlank { "2026" }
        }

        val newItem = BusRouteFleetItem(
            id = resolvedId,
            routeAreaName = cleanRouteArea,
            vehicleTypeAndColor = cleanVehicleType,
            vehiclePlateNumber = cleanPlate,
            driverName = cleanDriver,
            driverPhone = cleanPhone,
            driverIdentityDetails = cleanIdentity,
            driverUsername = prefixedDriverUser,
            driverPassword = cleanDriverPass,
            isDriverAccountEnabled = isDriverAccountEnabled
        )

        val idx = currentList.indexOfFirst { it.id == resolvedId || it.routeAreaName.equals(cleanRouteArea, ignoreCase = true) }
        if (idx >= 0) {
            currentList[idx] = newItem
        } else {
            currentList.add(newItem)
        }

        val updatedConfig = _schoolConfig.value.copy(
            registeredBusRoutes = currentList,
            busRouteAreaName = cleanRouteArea,
            busDriverName = cleanDriver,
            busDriverPhone = cleanPhone,
            busVehicleNumber = cleanPlate,
            busVehicleTypeAndColor = cleanVehicleType,
            busDriverIdentityDetails = cleanIdentity,
            isDriverAccountEnabled = isDriverAccountEnabled,
            busDriverUsername = prefixedDriverUser,
            busDriverPassword = cleanDriverPass
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        syncAllFleetDriverAccounts(updatedConfig)

        showToastAlert(
            "تم اعتماد الخط ($cleanRouteArea) وحساب السائق ($prefixedDriverUser) بنجاح",
            isSuccess = true
        )
        return newItem
    }

    fun deleteBusRouteFleetItem(routeId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val updatedRoutes = _schoolConfig.value.registeredBusRoutes.filterNot { it.id == routeId }
        val updatedConfig = _schoolConfig.value.copy(registeredBusRoutes = updatedRoutes)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        syncAllFleetDriverAccounts(updatedConfig)
        showToastAlert("تم حذف الخط من قائمة الخطوط المسجلة", isSuccess = true)
    }

    fun updateBusDriverAccountSettings(
        isEnabled: Boolean,
        driverUsernameInput: String,
        driverPasswordInput: String,
        targetRouteId: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val rawUser = InputSanitizer.sanitizeUsername(driverUsernameInput).ifBlank { "driver1" }
        val prefixedDriverUser = FirebaseConfigConstants.applySchoolPrefix(sid, rawUser)
        val cleanPass = InputSanitizer.sanitize(driverPasswordInput).ifBlank { "2026" }

        val currentRoutes = _schoolConfig.value.effectiveBusRoutesList.toMutableList()
        val routeIdx = if (targetRouteId.isNotBlank()) {
            currentRoutes.indexOfFirst { it.id == targetRouteId }.takeIf { it >= 0 } ?: 0
        } else 0
        if (currentRoutes.isNotEmpty() && routeIdx in currentRoutes.indices) {
            currentRoutes[routeIdx] = currentRoutes[routeIdx].copy(
                driverUsername = prefixedDriverUser,
                driverPassword = cleanPass,
                isDriverAccountEnabled = isEnabled
            )
        }

        val updatedConfig = _schoolConfig.value.copy(
            isDriverAccountEnabled = isEnabled,
            busDriverUsername = prefixedDriverUser,
            busDriverPassword = cleanPass,
            registeredBusRoutes = currentRoutes
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        syncAllFleetDriverAccounts(updatedConfig)

        if (isEnabled) {
            showToastAlert(
                "تم تفعيل وحفظ حساب السائق بنجاح • المستخدم: ($prefixedDriverUser) • الرمز: ($cleanPass)",
                isSuccess = true
            )
        } else {
            showToastAlert("تم إيقاف حساب السائق المستقل", isSuccess = true)
        }
    }

    private fun syncAllFleetDriverAccounts(config: SchoolConfig) {
        val sid = config.schoolId
        val driverAccounts = config.effectiveBusRoutesList.mapNotNull { route ->
            val uName = route.driverUsername.trim()
            val uPass = route.driverPassword.trim()
            if (uName.isEmpty() || uPass.isEmpty()) return@mapNotNull null
            UserAccount(
                id = "acc_bus_driver_${sid}_${route.id}",
                username = FirebaseConfigConstants.applySchoolPrefix(sid, uName),
                password = uPass,
                fullName = route.driverName.ifBlank { config.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" } },
                role = UserRole.TEACHER,
                isActive = route.isDriverAccountEnabled,
                phone = route.driverPhone.ifBlank { config.busDriverPhone },
                specialty = route.routeAreaName.ifBlank { "سائق الخط المدرسي" },
                assignedClasses = route.id,
                photoBase64 = config.busDriverPhotoBase64,
                schoolId = sid,
                isBusDriverAccount = true
            )
        }
        val nonDriverAccs = _accounts.value.filterNot {
            it.isBusDriverAccount || it.id.startsWith("acc_bus_driver_")
        }
        val mergedAccs = nonDriverAccs + driverAccounts
        _accounts.value = mergedAccs
        repo.saveAccounts(mergedAccs, sid)
    }

    /**
     * Records a student's boarding or alighting from the school bus via QR code scan or quick selection,
     * strictly enforces:
     * 1) Automatic phase binding to the School Administration's trip state (`effectiveDriverScanModeForDate`),
     * 2) Auto Daily Reset when a new day (`todayIso`) begins,
     * 3) One-Time Scan Lock per phase (rejects duplicate scans in the same phase on the same day).
     */
    fun recordStudentBusBoardingByQrOrId(
        qrOrStudentId: String,
        scanMode: BusBoardingScanMode
    ): StudentRecord? {
        val user = _currentUser.value ?: return null
        if (!user.isBusDriverAccount && user.role != UserRole.ADMIN && user.role != UserRole.TEACHER) {
            showToastAlert("هذه الصلاحية مخصصة للسائق المعتمد أو إدارة المدرسة فقط", isSuccess = false)
            return null
        }

        val cleanInput = qrOrStudentId.trim()
        if (cleanInput.isEmpty()) {
            showToastAlert("يرجى مسح باركود هوية الطالب أولاً", isSuccess = false)
            return null
        }

        val sid = _schoolConfig.value.schoolId
        val schoolStudents = _students.value.filter { (it.schoolId.isEmpty() || it.schoolId == sid) && it.isActive }
        val pipeParts = cleanInput.split("|").map { it.trim() }
        val extractedStuIdFromPipe = if (pipeParts.size >= 3 && pipeParts[0].equals("MADRASATI", ignoreCase = true)) {
            pipeParts[2]
        } else ""

        val matchedStudent = schoolStudents.firstOrNull { stu ->
            stu.qrCodeValue.equals(cleanInput, ignoreCase = true) ||
                stu.walletBarcode.equals(cleanInput, ignoreCase = true) ||
                (extractedStuIdFromPipe.isNotEmpty() && stu.id.equals(extractedStuIdFromPipe, ignoreCase = true)) ||
                cleanInput.contains("|${stu.id}|", ignoreCase = true) ||
                stu.id.equals(cleanInput, ignoreCase = true) ||
                stu.studentUsername.equals(cleanInput, ignoreCase = true) ||
                cleanInput.endsWith("-${stu.id}", ignoreCase = true) ||
                stu.studentName.equals(cleanInput, ignoreCase = true) ||
                stu.fullNameWithFather.equals(cleanInput, ignoreCase = true)
        }

        if (matchedStudent == null) {
            showToastAlert("لم يتم العثور على طالب مطابق لرمز الباركود الممسوح", isSuccess = false)
            return null
        }

        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        // Driver's scan mode is strictly locked to the Administration's trip state for today
        val effectiveScanMode = if (user.isBusDriverAccount) {
            _schoolConfig.value.effectiveDriverScanModeForDate(todayIso)
        } else {
            scanMode
        }

        // One-Time Scan Lock per Phase (منع تكرار مسح الباركود لنفس الطالب في نفس المرحلة)
        if (effectiveScanMode == BusBoardingScanMode.BOARDING) {
            if (matchedStudent.isBoardedMorningToday(todayIso)) {
                showToastAlert("تم تسجيل صعود هذا الطالب مسبقاً اليوم! لا يمكن تكرار المسح.", isSuccess = false)
                return null
            }
        } else {
            if (matchedStudent.isDepartedReturnToday(todayIso)) {
                showToastAlert("تم تسجيل مغادرة هذا الطالب مسبقاً اليوم!", isSuccess = false)
                return null
            }
        }

        com.example.util.StandardQrCodeHelper.playScanBeepAndVibrate(appContext)

        val timeNow = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date())
        val resolvedDriverName = matchedStudent.busDriverName
            .ifBlank { _schoolConfig.value.busDriverName }
            .ifBlank { user.fullName }
            .ifBlank { "السائق المعتمد" }

        var updatedTarget: StudentRecord? = null
        val updatedList = _students.value.map { s ->
            if (s.id == matchedStudent.id) {
                val isSameDay = s.isSameBusScanDate(todayIso)
                val nextMorningTime = when {
                    effectiveScanMode == BusBoardingScanMode.BOARDING -> timeNow
                    isSameDay -> s.getMorningBoardedTimeToday(todayIso)
                    else -> ""
                }
                val nextReturnTime = when {
                    effectiveScanMode == BusBoardingScanMode.ALIGHTING -> timeNow
                    isSameDay -> s.getReturnDepartedTimeToday(todayIso)
                    else -> ""
                }
                val copyStu = s.copy(
                    isBusSubscribed = true,
                    busBoardingStatus = effectiveScanMode.name,
                    busBoardingTimestamp = timeNow,
                    busBoardingDateIso = todayIso,
                    busBoardingDriverName = resolvedDriverName,
                    busMorningBoardedTime = nextMorningTime,
                    busReturnDepartedTime = nextReturnTime
                )
                updatedTarget = copyStu
                copyStu
            } else s
        }
        _students.value = updatedList
        repo.saveStudents(updatedList, sid)

        val notifTitle: String
        val notifBody: String
        if (effectiveScanMode == BusBoardingScanMode.BOARDING) {
            notifTitle = "صعود الطالب إلى الباص المدرسي بسلام"
            notifBody = "صعد الطالب (${matchedStudent.fullNameWithFather}) إلى الباص الآن بسلام • الساعة $timeNow مع السائق ($resolvedDriverName)"
        } else {
            notifTitle = "نزول الطالب من الباص ووصوله بسلام"
            notifBody = "نزل الطالب (${matchedStudent.fullNameWithFather}) من الباص ووصل بسلام • الساعة $timeNow"
        }

        pushNotification(
            title = notifTitle,
            body = notifBody,
            type = NotificationType.ATTENDANCE,
            targetStudentId = matchedStudent.id,
            targetSection = AppSection.SCHOOL_TRANSPORT.name
        )

        showToastAlert(notifBody, isSuccess = true)
        return updatedTarget
    }

    /**
     * Sends an instant School Bus & Transport notification (`bus_tracking`)
     * EXCLUSIVELY to parents of students subscribed to the school bus (`isBusSubscribed == true`).
     */
    fun sendBusTrackingAlert(eventType: BusEventType) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val nowStamp = SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale("ar")).format(Date())

        val updatedConfig = _schoolConfig.value.copy(
            activeBusEvent = eventType.name,
            activeBusEventTimestamp = nowStamp,
            activeBusEventDateIso = todayIso
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)

        val subscribedStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) && it.isBusSubscribed && it.isActive
        }
        val targetStudentsForAlert = if (subscribedStudents.isNotEmpty()) {
            subscribedStudents
        } else {
            _students.value.filter { (it.schoolId.isEmpty() || it.schoolId == sid) && it.isActive }
        }

        targetStudentsForAlert.forEach { stu ->
            val body = String.format(Locale("ar"), eventType.notificationBodyTemplate, stu.fullNameWithFather)
            pushNotification(
                title = eventType.notificationTitle,
                body = body,
                type = NotificationType.ATTENDANCE,
                targetStudentId = stu.id,
                targetRole = UserRole.PARENT.name,
                targetSection = AppSection.SCHOOL_TRANSPORT.name
            )
        }

        // Also record latest bus event in schools/{schoolId}/bus_tracking
        val payload = org.json.JSONObject().apply {
            put("eventType", eventType.name)
            put("arabicLabel", eventType.arabicLabel)
            put("liveRouteStatusText", eventType.liveRouteStatusText)
            put("notifiedStudentsCount", targetStudentsForAlert.size)
            put("timestamp", nowStamp)
        }.toString()
        repo.saveRawCachedSubCollection(sid, FirebaseConfigConstants.COL_BUS_TRACKING, payload)
        FirebaseCloudSync.pushSubCollectionAsync(sid, FirebaseConfigConstants.COL_BUS_TRACKING, payload)

        if (subscribedStudents.isEmpty()) {
            showToastAlert(
                "تم تحديث مسار الباص فوراً: (${eventType.liveRouteStatusText})",
                isSuccess = true
            )
        } else {
            showToastAlert(
                "تم إرسال إشعار (${eventType.arabicLabel}) حصرياً لأولياء أمور (${subscribedStudents.size}) طالب مشترك في الخط",
                isSuccess = true
            )
        }
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
     * Unified helper to read the student's daily attendance status directly from the StudentRecord
     * (`dailyAttendance[targetDate]`, `todayStatusDate`/`todayStatus`, and in-memory `_attendance`),
     * used across AttendanceScreen, Admin/Teacher Dashboard, and Parent/Student Dashboard.
     */
    fun getStudentStatusForDate(student: StudentRecord?, dateStr: String? = null): AttendanceStatus {
        if (student == null) return AttendanceStatus.UNRECORDED
        val targetDate = ArabicTextNormalizer.normalizeDateIso(
            dateStr?.takeIf { it.isNotBlank() } ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        )
        if (targetDate.isNotEmpty() && getHolidayLabelForDate(targetDate) != null) {
            return AttendanceStatus.HOLIDAY
        }
        val compactDate = targetDate.replace("-", "")
        val directFromMap = student.dailyAttendance[targetDate]
            ?: student.dailyAttendance[compactDate]
            ?: student.dailyAttendance.entries.firstOrNull { (k, _) ->
                ArabicTextNormalizer.normalizeDateIso(k) == targetDate
            }?.value
        if (!directFromMap.isNullOrBlank()) {
            val parsed = runCatching { AttendanceStatus.valueOf(directFromMap.trim().uppercase()) }.getOrNull()
            if (parsed != null && parsed != AttendanceStatus.UNRECORDED && (parsed != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(targetDate) != null)) {
                return parsed
            }
        }
        val cleanTodayDate = ArabicTextNormalizer.normalizeDateIso(student.todayStatusDate)
        if (cleanTodayDate == targetDate && student.todayStatus.isNotBlank()) {
            val parsed = runCatching { AttendanceStatus.valueOf(student.todayStatus.trim().uppercase()) }.getOrNull()
            if (parsed != null && parsed != AttendanceStatus.UNRECORDED && (parsed != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(targetDate) != null)) {
                return parsed
            }
        }
        val fromAttList = _attendance.value.firstOrNull {
            it.studentId.trim() == student.id.trim() &&
                ArabicTextNormalizer.normalizeDateIso(it.date) == targetDate
        }?.status
        if (fromAttList != null && fromAttList != AttendanceStatus.UNRECORDED && (fromAttList != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(targetDate) != null)) {
            return fromAttList
        }

        // Also check if an APPROVED leave request covers targetDate
        val approvedLeave = _leaveRequests.value.any { req ->
            req.studentId.trim() == student.id.trim() &&
                req.status == LeaveRequestStatus.APPROVED &&
                isDateCoveredByLeaveRequest(req, targetDate)
        }
        if (approvedLeave) return AttendanceStatus.EXCUSED

        // If student scanned gate Check-In on targetDate, count as PRESENT
        val gateLog = student.getGateLogForDate(targetDate)
        if (gateLog != null && gateLog.checkInTime.isNotBlank()) {
            return AttendanceStatus.PRESENT
        }

        return AttendanceStatus.UNRECORDED
    }

    /**
     * Returns the canonical date-keyed attendance map (`YYYY-MM-DD` -> `AttendanceStatus`) for a student,
     * merging `StudentRecord.dailyAttendance` and `_attendance.value` so that each date has a single authoritative status.
     */
    fun getStudentAttendanceMap(studentId: String): Map<String, AttendanceStatus> {
        val cleanId = studentId.trim()
        if (cleanId.isEmpty()) return emptyMap()
        val map = linkedMapOf<String, AttendanceStatus>()

        val student = _students.value.firstOrNull { it.id.trim() == cleanId }
        student?.dailyAttendance?.forEach { (rawDate, rawStatus) ->
            val cleanDate = ArabicTextNormalizer.normalizeDateIso(rawDate.trim())
            if (cleanDate.isNotEmpty()) {
                val parsed = runCatching { AttendanceStatus.valueOf(rawStatus.trim().uppercase()) }.getOrNull()
                if (parsed != null && parsed != AttendanceStatus.UNRECORDED && (parsed != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(cleanDate) != null)) {
                    map[cleanDate] = parsed
                }
            }
        }
        if (student != null) {
            val cleanTodayDate = ArabicTextNormalizer.normalizeDateIso(student.todayStatusDate)
            if (cleanTodayDate.isNotEmpty() && student.todayStatus.isNotBlank() && !map.containsKey(cleanTodayDate)) {
                runCatching { AttendanceStatus.valueOf(student.todayStatus.trim().uppercase()) }.getOrNull()?.let { parsed ->
                    if (parsed != AttendanceStatus.UNRECORDED && (parsed != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(cleanTodayDate) != null)) {
                        map[cleanTodayDate] = parsed
                    }
                }
            }
        }

        // Overlay _attendance entries from oldest to newest so the latest prepended entry wins
        _attendance.value
            .filter { it.studentId.trim() == cleanId && it.date.isNotBlank() }
            .asReversed()
            .forEach { entry ->
                val cleanDate = ArabicTextNormalizer.normalizeDateIso(entry.date.trim())
                if (cleanDate.isNotEmpty() && entry.status != AttendanceStatus.UNRECORDED && (entry.status != AttendanceStatus.HOLIDAY || getHolidayLabelForDate(cleanDate) != null)) {
                    map[cleanDate] = entry.status
                }
            }

        return map
    }

    /**
     * Reads the student's saved attendance status for a specific `YYYY-MM-DD` date directly from
     * `getStudentStatusForDate(student, dateIso)`.
     */
    fun getStudentAttendanceStatusForDate(studentId: String, dateIso: String): AttendanceStatus {
        val cleanDate = ArabicTextNormalizer.normalizeDateIso(dateIso.trim())
        if (cleanDate.isNotEmpty() && getHolidayLabelForDate(cleanDate) != null) {
            return AttendanceStatus.HOLIDAY
        }
        val student = _students.value.firstOrNull { it.id.trim() == studentId.trim() }
        if (student != null) {
            return getStudentStatusForDate(student, cleanDate)
        }
        val map = getStudentAttendanceMap(studentId)
        return map[cleanDate] ?: AttendanceStatus.UNRECORDED
    }

    /**
     * Computes the Automatic Motivational Badges for a student:
     * 1. "وسام الالتزام الذهبي": 100% attendance (0 ABSENT records) during the current month.
     * 2. "وسام نجم المادة": Full score (10/10 or 100/100) in any subject exam.
     */
    fun getStudentAutoBadges(studentId: String): List<StudentAutoBadge> {
        val badges = mutableListOf<StudentAutoBadge>()
        val absentCount = getTotalAbsenceDays(studentId)
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
        // Count unique dates in the student's canonical date-keyed map where status is ABSENT (excluding holidays)
        return getStudentAttendanceMap(studentId).count { (dateKey, status) ->
            status == AttendanceStatus.ABSENT && getHolidayLabelForDate(dateKey) == null
        }
    }

    fun getTotalExcusedDays(studentId: String): Int {
        return getStudentAttendanceMap(studentId).count { (dateKey, status) ->
            status == AttendanceStatus.EXCUSED && getHolidayLabelForDate(dateKey) == null
        }
    }

    fun getTotalLateDays(studentId: String): Int {
        return getStudentAttendanceMap(studentId).count { (dateKey, status) ->
            status == AttendanceStatus.LATE && getHolidayLabelForDate(dateKey) == null
        }
    }

    fun getTotalPresentDays(studentId: String): Int {
        return getStudentAttendanceMap(studentId).count { (dateKey, status) ->
            status == AttendanceStatus.PRESENT && getHolidayLabelForDate(dateKey) == null
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
        dueDate: String,
        attachmentImageBase64: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanPrep = InputSanitizer.sanitize(dailyPreparation)
        if (cleanPrep.isBlank()) {
            showToastAlert("يرجى إدخال تفاصيل التحضير اليومي للدرس", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val hw = HomeworkItem(
            id = "hw_${UUID.randomUUID().toString().take(6)}",
            gradeClass = gradeClass,
            sectionName = sectionName,
            subjectName = InputSanitizer.sanitize(subjectName),
            teacherName = InputSanitizer.sanitize(teacherName),
            dailyPreparation = cleanPrep,
            homeworkDetails = InputSanitizer.sanitize(homeworkDetails),
            dueDate = InputSanitizer.sanitize(dueDate),
            createdAt = todayIso,
            attachmentImageBase64 = attachmentImageBase64.trim(),
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

    fun updateHomework(
        homeworkId: String,
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        teacherName: String,
        dailyPreparation: String,
        homeworkDetails: String,
        dueDate: String,
        attachmentImageBase64: String? = null
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val cleanPrep = InputSanitizer.sanitize(dailyPreparation)
        if (cleanPrep.isBlank()) {
            showToastAlert("يرجى إدخال تفاصيل التحضير اليومي للدرس", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        val updated = _homeworks.value.map { hw ->
            if (hw.id == homeworkId) {
                hw.copy(
                    gradeClass = gradeClass,
                    sectionName = sectionName,
                    subjectName = InputSanitizer.sanitize(subjectName),
                    teacherName = InputSanitizer.sanitize(teacherName),
                    dailyPreparation = cleanPrep,
                    homeworkDetails = InputSanitizer.sanitize(homeworkDetails),
                    dueDate = InputSanitizer.sanitize(dueDate),
                    attachmentImageBase64 = attachmentImageBase64?.trim() ?: hw.attachmentImageBase64
                )
            } else hw
        }
        _homeworks.value = updated
        repo.saveHomeworks(updated, sid)
        showToastAlert("تم تعديل وتحديث الواجب الدراسي لمادة ($subjectName) بنجاح", isSuccess = true)
    }

    fun deleteHomework(homeworkId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val updated = _homeworks.value.filterNot { it.id == homeworkId }
        _homeworks.value = updated
        repo.saveHomeworks(updated, sid)
        showToastAlert("تم حذف الواجب الدراسي من القائمة نهائياً", isSuccess = true)
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
        topicsIncluded: String,
        timeSlot: String = ""
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
                    topicsIncluded = InputSanitizer.sanitize(topicsIncluded),
                    timeSlot = InputSanitizer.sanitize(timeSlot)
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
        maxScoreScale: Int = getDefaultMaxScoreForClassAndSubject(student.gradeClass, subjectName),
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
        val existingGrade = if (existingIdx >= 0) _grades.value[existingIdx] else null

        // Determine per-period locks for Teachers
        val isTeacher = _currentUser.value?.role == UserRole.TEACHER
        val lockedPeriodsForStudent: Set<String> = GradeAssessmentPeriod.entries
            .filter { p ->
                existingGrade?.isPeriodApproved(p) == true ||
                    isClassSubjectPeriodApproved(student.gradeClass, student.sectionName, subjectName, p)
            }
            .map { it.key }
            .toSet()

        if (isTeacher && lockedPeriodsForStudent.size == GradeAssessmentPeriod.entries.size) {
            showToastAlert(
                "تمت مصادقة جميع فترات هذه المادة من قِبل إدارة المدرسة (التعديل متاح للمدير فقط)",
                isSuccess = false
            )
            return false
        }

        val resolvedDaily = if (isTeacher && GradeAssessmentPeriod.DAILY.key in lockedPeriodsForStudent) {
            existingGrade?.dailyScore ?: 0
        } else {
            dailyScore.coerceIn(0, validScale)
        }
        val resolvedMonth1 = if (isTeacher && GradeAssessmentPeriod.MONTH_1.key in lockedPeriodsForStudent) {
            existingGrade?.month1Score ?: 0
        } else {
            month1Score.coerceIn(0, validScale)
        }
        val resolvedMonth2 = if (isTeacher && GradeAssessmentPeriod.MONTH_2.key in lockedPeriodsForStudent) {
            existingGrade?.month2Score ?: 0
        } else {
            month2Score.coerceIn(0, validScale)
        }
        val resolvedMidYear = if (isTeacher && GradeAssessmentPeriod.MID_YEAR.key in lockedPeriodsForStudent) {
            existingGrade?.midYearScore ?: 0
        } else {
            midYearScore.coerceIn(0, validScale)
        }
        val resolvedFinalExam = if (isTeacher && GradeAssessmentPeriod.FINAL_EXAM.key in lockedPeriodsForStudent) {
            existingGrade?.finalExamScore ?: 0
        } else {
            finalExamScore.coerceIn(0, validScale)
        }

        val sanitizedExtraMonthly = extraMonthlyScores.map { it.coerceIn(0, validScale) }.toMutableList()
        if (isTeacher && GradeAssessmentPeriod.MONTH_3.key in lockedPeriodsForStudent) {
            val oldM3 = existingGrade?.extraMonthlyScores?.getOrNull(0) ?: 0
            if (sanitizedExtraMonthly.isEmpty()) sanitizedExtraMonthly.add(oldM3) else sanitizedExtraMonthly[0] = oldM3
        }
        if (isTeacher && GradeAssessmentPeriod.MONTH_4.key in lockedPeriodsForStudent) {
            val oldM4 = existingGrade?.extraMonthlyScores?.getOrNull(1) ?: 0
            while (sanitizedExtraMonthly.size < 2) sanitizedExtraMonthly.add(0)
            sanitizedExtraMonthly[1] = oldM4
        }

        val resolvedExtraDaily = if (isTeacher && GradeAssessmentPeriod.DAILY.key in lockedPeriodsForStudent) {
            existingGrade?.extraDailyScores ?: emptyList()
        } else {
            extraDailyScores.map { it.coerceIn(0, validScale) }
        }

        val mergedApprovedPeriods = (existingGrade?.approvedPeriods ?: emptySet()) + lockedPeriodsForStudent

        val grade = SubjectGrade(
            id = existingGrade?.id ?: "grd_${UUID.randomUUID().toString().take(6)}",
            studentId = student.id,
            studentName = student.studentName,
            gradeClass = student.gradeClass,
            subjectName = InputSanitizer.sanitize(subjectName),
            dailyScore = resolvedDaily,
            month1Score = resolvedMonth1,
            month2Score = resolvedMonth2,
            midYearScore = resolvedMidYear,
            finalExamScore = resolvedFinalExam,
            maxScoreScale = validScale,
            extraDailyScores = resolvedExtraDaily,
            extraMonthlyScores = sanitizedExtraMonthly,
            isApprovedByPrincipal = mergedApprovedPeriods.isNotEmpty(),
            approvedPeriods = mergedApprovedPeriods,
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
            body = "تم تحديث درجات الطالب ${student.studentName} في مادة $subjectName (السعي: ${grade.annualEffortScore} / $validScale | المعدل: ${grade.finalAverage} / $validScale - ${grade.ratingLabel}).",
            type = NotificationType.GRADES,
            targetStudentId = student.id,
            targetSection = AppSection.EXAMS_GRADES.name,
            targetTab = 0,
            targetItemId = grade.id
        )
        return true
    }

    /**
     * Checks if a specific student's subject period is approved by the Principal.
     */
    fun isStudentSubjectPeriodApproved(
        studentId: String,
        subjectName: String,
        period: GradeAssessmentPeriod
    ): Boolean {
        val cleanSub = subjectName.trim()
        val g = _grades.value.firstOrNull {
            it.studentId == studentId && it.subjectName.equals(cleanSub, ignoreCase = true)
        } ?: return false
        return g.isPeriodApproved(period)
    }

    /**
     * Checks if a specific student's subject has at least one approved & recorded period.
     */
    fun isStudentSubjectGradeApproved(studentId: String, subjectName: String): Boolean {
        val cleanSub = subjectName.trim()
        val g = _grades.value.firstOrNull {
            it.studentId == studentId && it.subjectName.equals(cleanSub, ignoreCase = true)
        } ?: return false
        return g.approvedAndRecordedPeriods.isNotEmpty()
    }

    /**
     * Checks if a specific period for a given class, section, and subject is officially approved by the Principal.
     */
    fun isClassSubjectPeriodApproved(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        period: GradeAssessmentPeriod
    ): Boolean {
        val sid = _schoolConfig.value.schoolId
        val cleanSub = subjectName.trim()
        if (cleanSub.isBlank()) return false
        val classStudents = _students.value.filter { stu ->
            stu.isActive &&
                (stu.schoolId.isEmpty() || stu.schoolId == sid) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, gradeClass) &&
                (sectionName.isBlank() || ArabicTextNormalizer.matchesSection(stu.sectionName, sectionName))
        }
        val studentIds = classStudents.map { it.id }.toSet()
        val matchingGrades = _grades.value.filter { g ->
            g.subjectName.equals(cleanSub, ignoreCase = true) &&
                (if (studentIds.isNotEmpty()) g.studentId in studentIds else ArabicTextNormalizer.matchesClass(g.gradeClass, gradeClass))
        }
        return matchingGrades.isNotEmpty() && matchingGrades.any { it.isPeriodApproved(period) }
    }

    /**
     * Computes the student completion summary for a specific class, section, subject, and assessment period.
     */
    fun getPeriodCompletionSummary(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        period: GradeAssessmentPeriod
    ): PeriodCompletionSummary {
        val sid = _schoolConfig.value.schoolId
        val cleanSub = subjectName.trim()
        val classStudents = _students.value.filter { stu ->
            stu.isActive &&
                (stu.schoolId.isEmpty() || stu.schoolId == sid) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, gradeClass) &&
                (sectionName.isBlank() || ArabicTextNormalizer.matchesSection(stu.sectionName, sectionName))
        }
        val unrecordedNames = mutableListOf<String>()
        var recordedCount = 0
        classStudents.forEach { stu ->
            val g = _grades.value.firstOrNull {
                it.studentId == stu.id && it.subjectName.equals(cleanSub, ignoreCase = true)
            }
            val score = g?.getScoreForPeriod(period) ?: 0
            if (score > 0) {
                recordedCount++
            } else {
                unrecordedNames.add(stu.fullNameWithFather)
            }
        }
        val approved = isClassSubjectPeriodApproved(gradeClass, sectionName, cleanSub, period)
        return PeriodCompletionSummary(
            period = period,
            totalStudentsCount = classStudents.size,
            recordedStudentsCount = recordedCount,
            unrecordedStudentNames = unrecordedNames,
            isPeriodApproved = approved
        )
    }

    /**
     * Checks if the grades for a given class, section, and subject have any approved period by the Principal.
     */
    fun isClassSubjectApprovedByPrincipal(
        gradeClass: String,
        sectionName: String,
        subjectName: String
    ): Boolean {
        return GradeAssessmentPeriod.entries.any { period ->
            isClassSubjectPeriodApproved(gradeClass, sectionName, subjectName, period)
        }
    }

    /**
     * Principal (UserRole.ADMIN) approves & locks (or unlocks) a specific assessment period
     * for a class/section/subject without locking other assessment periods.
     */
    fun setClassSubjectPeriodApproval(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        period: GradeAssessmentPeriod,
        approved: Boolean
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val cleanSub = InputSanitizer.sanitize(subjectName.trim())
        if (cleanSub.isBlank()) return

        val classStudents = _students.value.filter { stu ->
            stu.isActive &&
                (stu.schoolId.isEmpty() || stu.schoolId == sid) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, gradeClass) &&
                (sectionName.isBlank() || ArabicTextNormalizer.matchesSection(stu.sectionName, sectionName))
        }
        val updated = _grades.value.toMutableList()
        val scale = getDefaultMaxScoreForClassAndSubject(gradeClass, cleanSub)

        if (classStudents.isNotEmpty()) {
            classStudents.forEach { stu ->
                val idx = updated.indexOfFirst {
                    it.studentId == stu.id && it.subjectName.equals(cleanSub, ignoreCase = true)
                }
                if (idx >= 0) {
                    val currentPeriods = updated[idx].approvedPeriods.toMutableSet()
                    if (approved) currentPeriods.add(period.key) else currentPeriods.remove(period.key)
                    updated[idx] = updated[idx].copy(
                        approvedPeriods = currentPeriods,
                        isApprovedByPrincipal = currentPeriods.isNotEmpty()
                    )
                } else if (approved) {
                    val newPeriods = setOf(period.key)
                    updated.add(
                        SubjectGrade(
                            id = "grd_${UUID.randomUUID().toString().take(6)}",
                            studentId = stu.id,
                            studentName = stu.studentName,
                            gradeClass = stu.gradeClass,
                            subjectName = cleanSub,
                            dailyScore = 0,
                            month1Score = 0,
                            month2Score = 0,
                            midYearScore = 0,
                            finalExamScore = 0,
                            maxScoreScale = scale,
                            isApprovedByPrincipal = true,
                            approvedPeriods = newPeriods,
                            schoolId = sid
                        )
                    )
                }
            }
        } else {
            for (i in updated.indices) {
                val g = updated[i]
                if (g.subjectName.equals(cleanSub, ignoreCase = true) &&
                    ArabicTextNormalizer.matchesClass(g.gradeClass, gradeClass)
                ) {
                    val currentPeriods = g.approvedPeriods.toMutableSet()
                    if (approved) currentPeriods.add(period.key) else currentPeriods.remove(period.key)
                    updated[i] = g.copy(
                        approvedPeriods = currentPeriods,
                        isApprovedByPrincipal = currentPeriods.isNotEmpty()
                    )
                }
            }
        }

        _grades.value = updated
        repo.saveGrades(updated, sid)
        showToastAlert(
            if (approved) {
                "تمت مصادقة واعتماد درجات (${period.arabicTitle}) لمادة ($cleanSub) رسمياً وقفل تعديل هذه الفترة على المعلمين"
            } else {
                "تم إلغاء المصادقة وفتح قفل التعديل لفترة (${period.arabicTitle}) في مادة ($cleanSub)"
            },
            isSuccess = true
        )
    }

    /**
     * Legacy whole-subject wrapper delegating to all periods if ever called.
     */
    fun setClassSubjectGradesApproval(
        gradeClass: String,
        sectionName: String,
        subjectName: String,
        approved: Boolean
    ) {
        GradeAssessmentPeriod.entries.forEach { p ->
            setClassSubjectPeriodApproval(gradeClass, sectionName, subjectName, p, approved)
        }
    }

    /**
     * Builds a complete Smart Student Report Card preview for a given student and period type.
     * Automatic Pass/Fail rule:
     *  - In 100-point system: score < 50 is failing in that subject.
     *  - In 10-point system: score < 5 is failing in that subject.
     *  - Passing all subjects = Overall Pass (ناجح).
     *  - Failing 1 or more subjects = Overall Fail / Needs Support (مكمل / راسب).
     */
    fun buildStudentReportCardPreview(
        student: StudentRecord,
        periodType: ReportCardPeriodType
    ): IssuedStudentReportCard {
        val sid = _schoolConfig.value.schoolId
        val configuredSubjects = getSubjectsForClass(student.gradeClass)
        val studentGrades = _grades.value.filter { it.studentId == student.id }
        val allSubjectNames = (configuredSubjects + studentGrades.map { it.subjectName })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { ArabicTextNormalizer.normalizeText(it) }

        val subjectItems = allSubjectNames.map { subName ->
            val g = studentGrades.firstOrNull {
                ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(subName)
            }
            val scale = g?.maxScoreScale ?: getDefaultMaxScoreForClassAndSubject(student.gradeClass, subName)
            val scoreForPeriod = when (periodType) {
                ReportCardPeriodType.MONTH_1 -> g?.month1Score ?: 0
                ReportCardPeriodType.MONTH_2 -> g?.month2Score ?: 0
                ReportCardPeriodType.MID_YEAR -> g?.midYearScore ?: 0
                ReportCardPeriodType.ANNUAL_EFFORT -> g?.annualEffortScore ?: 0
                ReportCardPeriodType.FINAL_EXAM -> g?.finalExamScore ?: 0
                ReportCardPeriodType.COMPREHENSIVE_ALL -> g?.finalAverage ?: 0
            }
            val minPass = if (scale <= 10) 5 else 50
            val passed = scoreForPeriod >= minPass
            IssuedReportCardSubjectItem(
                subjectName = subName,
                score = scoreForPeriod,
                maxScoreScale = scale,
                isPassed = passed,
                dailyScore = g?.dailyScore ?: 0,
                month1Score = g?.month1Score ?: 0,
                month2Score = g?.month2Score ?: 0,
                midYearScore = g?.midYearScore ?: 0,
                annualEffortScore = g?.annualEffortScore ?: 0,
                finalExamScore = g?.finalExamScore ?: 0,
                finalAverage = g?.finalAverage ?: 0
            )
        }

        val totalScore = subjectItems.sumOf { it.score }
        val avgPercent = if (subjectItems.isEmpty()) {
            0
        } else {
            subjectItems.map { item ->
                if (item.maxScoreScale <= 10) (item.score * 10).coerceIn(0, 100) else item.score.coerceIn(0, 100)
            }.average().toInt()
        }
        val failedCount = subjectItems.count { !it.isPassed }
        val isOverallPassed = subjectItems.isNotEmpty() && failedCount == 0
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val principalName = _accounts.value.firstOrNull {
            it.role == UserRole.ADMIN && (it.schoolId.isEmpty() || it.schoolId == sid)
        }?.fullName?.ifBlank { "إدارة المدرسة" } ?: "إدارة المدرسة"

        return IssuedStudentReportCard(
            id = "rc_${student.id}_${periodType.name}",
            studentId = student.id,
            studentName = student.fullNameWithFather,
            gradeClass = student.gradeClass,
            sectionName = student.sectionName,
            periodType = periodType,
            subjects = subjectItems,
            totalScore = totalScore,
            averagePercent = avgPercent,
            isOverallPassed = isOverallPassed,
            failedSubjectsCount = failedCount,
            issuedAt = dateStr,
            principalName = principalName,
            schoolName = _schoolConfig.value.schoolName,
            isViewedByParent = false,
            isDismissedFromHome = false,
            schoolId = sid
        )
    }

    /**
     * Issues and sends a Smart Student Report Card to the student's parent and syncs to Firebase Cloud.
     */
    fun issueAndSendStudentReportCard(
        student: StudentRecord,
        periodType: ReportCardPeriodType
    ): IssuedStudentReportCard? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val sid = _schoolConfig.value.schoolId
        val card = buildStudentReportCardPreview(student, periodType)
        repo.clearReportCardDismissedLocally(card.id, sid)
        val updated = _issuedReportCards.value.toMutableList()
        val existingIdx = updated.indexOfFirst { it.id == card.id }
        if (existingIdx >= 0) {
            updated[existingIdx] = card
        } else {
            updated.add(0, card)
        }
        _issuedReportCards.value = updated
        repo.saveIssuedReportCards(updated, sid)

        val updatedStudents = _students.value.map { s ->
            if (s.id == student.id) s.copy(isReportCardSentByPrincipal = true) else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val statusSummary = if (card.isOverallPassed) {
            "ناجح بتفوق (المعدل: ${card.averagePercent}%)"
        } else {
            "بحاجة إلى متابعة ودعم في ${card.failedSubjectsCount} مواد (المعدل: ${card.averagePercent}%)"
        }
        pushNotification(
            title = "وصل الآن الكارت المدرسي الرسمي للطالب (${student.studentName})",
            body = "أصدرت إدارة المدرسة (${periodType.arabicTitle}) للطالب ${student.fullNameWithFather} — النتيجة: $statusSummary. اضغط لفتح الكارت.",
            type = NotificationType.GRADES,
            targetStudentId = student.id,
            targetSection = AppSection.EXAMS_GRADES.name,
            targetTab = 2,
            targetItemId = card.id
        )
        showToastAlert(
            "تم إصدار وإرسال (${periodType.arabicTitle}) لولي أمر الطالب ${student.studentName} بنجاح",
            isSuccess = true
        )
        return card
    }

    /**
     * Revokes/cancels the official report card sent by the School Principal for a student.
     */
    fun revokeStudentReportCard(
        student: StudentRecord,
        periodType: ReportCardPeriodType? = null
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val updatedCards = _issuedReportCards.value.filterNot { c ->
            c.studentId == student.id && (periodType == null || c.periodType == periodType)
        }
        _issuedReportCards.value = updatedCards
        repo.saveIssuedReportCards(updatedCards, sid)

        val stillHasAnyCard = updatedCards.any { it.studentId == student.id }
        val updatedStudents = _students.value.map { s ->
            if (s.id == student.id) s.copy(isReportCardSentByPrincipal = stillHasAnyCard) else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)
        showToastAlert("تم إلغاء إرسال وسحب الكارت الرسمي للطالب (${student.studentName})", isSuccess = true)
    }

    /**
     * Bulk-issues and sends Smart Student Report Cards for all students in a given class & section.
     */
    fun issueAndSendClassReportCardsBulk(
        gradeClass: String,
        sectionName: String,
        periodType: ReportCardPeriodType
    ): Int {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return 0
        val sid = _schoolConfig.value.schoolId
        val targetStudents = _students.value.filter { stu ->
            stu.isActive &&
                (stu.schoolId.isEmpty() || stu.schoolId == sid) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, gradeClass) &&
                (sectionName.isBlank() || ArabicTextNormalizer.matchesSection(stu.sectionName, sectionName))
        }
        if (targetStudents.isEmpty()) {
            showToastAlert("لا يوجد طلاب مسجلون في هذا الصف والشعبة لإصدار الكارتات", isSuccess = false)
            return 0
        }

        val updated = _issuedReportCards.value.toMutableList()
        val targetIds = targetStudents.map { it.id }.toSet()
        targetStudents.forEach { stu ->
            val card = buildStudentReportCardPreview(stu, periodType)
            repo.clearReportCardDismissedLocally(card.id, sid)
            val existingIdx = updated.indexOfFirst { it.id == card.id }
            if (existingIdx >= 0) {
                updated[existingIdx] = card
            } else {
                updated.add(0, card)
            }
            val statusSummary = if (card.isOverallPassed) {
                "ناجح (${card.averagePercent}%)"
            } else {
                "مكمل في ${card.failedSubjectsCount} مواد (${card.averagePercent}%)"
            }
            pushNotification(
                title = "وصل الآن الكارت المدرسي الرسمي للطالب (${stu.studentName})",
                body = "أصدرت إدارة المدرسة (${periodType.arabicTitle}) للطالب ${stu.fullNameWithFather} — النتيجة: $statusSummary. اضغط لفتح النتيجة.",
                type = NotificationType.GRADES,
                targetStudentId = stu.id,
                targetSection = AppSection.EXAMS_GRADES.name,
                targetTab = 2,
                targetItemId = card.id
            )
        }
        _issuedReportCards.value = updated
        repo.saveIssuedReportCards(updated, sid)

        val updatedStudents = _students.value.map { s ->
            if (s.id in targetIds) s.copy(isReportCardSentByPrincipal = true) else s
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        showToastAlert(
            "تم إصدار وإرسال (${periodType.arabicTitle}) لجميع طلاب $gradeClass ($sectionName) (${targetStudents.size} طالب) دفعة واحدة",
            isSuccess = true
        )
        return targetStudents.size
    }

    fun getLatestIssuedReportCardForStudent(studentId: String): IssuedStudentReportCard? {
        if (studentId.isBlank()) return null
        val sid = _schoolConfig.value.schoolId
        return _issuedReportCards.value.firstOrNull {
            it.studentId == studentId && (it.schoolId.isEmpty() || it.schoolId == sid)
        }
    }

    /**
     * Returns the latest issued report card for the student ONLY if it has not been dismissed from the Home screen.
     */
    fun getLatestHomeBannerReportCardForStudent(studentId: String): IssuedStudentReportCard? {
        if (studentId.isBlank()) return null
        val sid = _schoolConfig.value.schoolId
        val localDismissed = repo.loadDismissedReportCardIds(sid)
        return _issuedReportCards.value.firstOrNull {
            it.studentId == studentId &&
                (it.schoolId.isEmpty() || it.schoolId == sid) &&
                !it.isDismissedFromHome &&
                it.id !in localDismissed
        }
    }

    /**
     * Permanently dismisses/hides the issued report card banner from the Home screen (both locally and in Firebase Cloud),
     * while keeping the report card accessible inside the Exams & Grades section.
     */
    fun dismissIssuedReportCardFromHome(cardId: String) {
        if (cardId.isBlank()) return
        val sid = _schoolConfig.value.schoolId
        repo.markReportCardDismissedLocally(cardId, sid)
        val updated = _issuedReportCards.value.map { c ->
            if (c.id == cardId) c.copy(isDismissedFromHome = true, isViewedByParent = true) else c
        }
        _issuedReportCards.value = updated
        repo.saveIssuedReportCards(updated, sid)
        showToastAlert("تم إخفاء إشعار الكارت المدرسي من الشاشة الرئيسية (يبقى متاحاً في قسم الامتحانات والدرجات)", isSuccess = true)
    }

    fun markIssuedReportCardViewed(cardId: String) {
        if (cardId.isBlank()) return
        val sid = _schoolConfig.value.schoolId
        val updated = _issuedReportCards.value.map { c ->
            if (c.id == cardId) c.copy(isViewedByParent = true) else c
        }
        _issuedReportCards.value = updated
        repo.saveIssuedReportCards(updated, sid)
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

    fun markReportsAsReadByParent(reportIds: Set<String>) {
        if (reportIds.isEmpty()) return
        val user = _currentUser.value ?: return
        if (user.role != UserRole.PARENT && user.role != UserRole.STUDENT) return
        val sid = _schoolConfig.value.schoolId
        val nowLabel = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        var anyUpdated = false
        val updated = _reports.value.map { rep ->
            if (rep.id in reportIds && !rep.isReadByParent) {
                anyUpdated = true
                rep.copy(
                    isReadByParent = true,
                    readAtTimestamp = nowLabel
                )
            } else rep
        }
        if (anyUpdated) {
            _reports.value = updated
            repo.saveReports(updated, sid)
        }
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
        val sName = _schoolConfig.value.schoolName
        val author = _currentUser.value?.fullName ?: "إدارة المدرسة"
        val ev = SchoolEvent(
            id = "ev_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            category = InputSanitizer.sanitize(category).ifBlank { "فعالية مدرسية" },
            description = InputSanitizer.sanitize(description).ifBlank { cleanTitle },
            date = InputSanitizer.sanitize(date).ifBlank { "2026-10-29" },
            location = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(location)).ifBlank { sName },
            imageBase64 = imageBase64,
            authorName = author,
            heartCount = 0,
            likedByUserIds = emptyList(),
            schoolId = sid,
            schoolName = sName
        )
        val updated = listOf(ev) + _events.value.filter { it.schoolId.isBlank() || it.schoolId == sid }
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
                    location = ArabicTextNormalizer.cleanDefaultAddress(InputSanitizer.sanitize(location)),
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
        customNote: String = "",
        targetDateIso: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val effectiveDate = normalizeCanonicalDateKey(targetDateIso.ifBlank { _selectedAttendanceDate.value.ifBlank { today } }).ifBlank { today }
        val holidayReason = getHolidayLabelForDate(effectiveDate)
        if (holidayReason != null) {
            showToastAlert("يوم عطلة رسمية معتمدة ($holidayReason) — جميع إجراءات الرصد والسلوك مقفلة في هذا التاريخ", isSuccess = false)
            return
        }
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
            date = effectiveDate,
            schoolId = sid
        )
        val updated = listOf(item) + _dailyBehaviors.value
        _dailyBehaviors.value = updated
        repo.saveDailyBehaviors(updated, sid)

        val isManualCustomNote = customNote.trim().isNotEmpty()
        val toastText = if (isManualCustomNote) {
            "تم إرسال الملاحظة السلوكية المخصصة للطالب ${student.fullNameWithFather} وإشعار ولي أمره فوراً"
        } else {
            "تم إرسال تقييم (${behaviorType.arabicLabel}) للطالب ${student.fullNameWithFather} وإشعار ولي أمره فوراً"
        }
        showToastAlert(toastText, isSuccess = true)

        pushNotification(
            title = if (isManualCustomNote) {
                "ملاحظة سلوكية ومتابعة يومية - ${student.fullNameWithFather}"
            } else {
                "${behaviorType.notificationTitle} - ${student.fullNameWithFather}"
            },
            body = if (isManualCustomNote) {
                "ملاحظة سلوكية من ($teacherName - $subjectName): $finalNote"
            } else {
                "تقييم يومي من ($teacherName - $subjectName): ${behaviorType.arabicLabel}. ملاحظة: $finalNote"
            },
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

    fun toggleQuickQuizClosedStatus(quizId: String) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN, UserRole.TEACHER))) return
        val sid = _schoolConfig.value.schoolId
        var nowClosed = false
        val updated = _quickQuizzes.value.map { q ->
            if (q.id == quizId) {
                nowClosed = !q.isClosed
                q.copy(isClosed = nowClosed)
            } else q
        }
        _quickQuizzes.value = updated
        _quickQuizAnswers.value = buildQuickQuizAnswerViews(updated)
        repo.saveQuickQuizzes(updated, sid)
        if (nowClosed) {
            showToastAlert("تم إنهاء الاختبار وإغلاق استقبال الإجابات ونقله للأرشيف", isSuccess = true)
        } else {
            showToastAlert("تمت إعادة تفعيل الاختبار السريع لاستقبال إجابات الطلبة", isSuccess = true)
        }
    }

    fun submitQuickQuizAnswer(quiz: QuickQuizItem, chosenIndex: Int) {
        submitQuickQuizAnswer(quiz.id, chosenIndex)
    }

    fun submitQuickQuizAnswer(quizId: String, chosenIndex: Int) {
        val user = _currentUser.value ?: return
        val sid = _schoolConfig.value.schoolId
        val targetQuiz = _quickQuizzes.value.firstOrNull { it.id == quizId } ?: return
        if (targetQuiz.isClosed) {
            showToastAlert("هذا الاختبار منتهي ومغلق من قِبل الأستاذ ولا يمكن إرسال إجابة جديدة", isSuccess = false)
            return
        }
        val stuId = getScopedStudentsForUser(user).firstOrNull()?.id ?: user.linkedStudentId.ifBlank { user.id }
        var isCorrect = false
        val timestampNow = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())

        val updated = _quickQuizzes.value.map { q ->
            if (q.id == quizId) {
                isCorrect = (chosenIndex == q.correctOptionIndex)
                val nextMap = q.studentAnswers.toMutableMap()
                nextMap[stuId] = chosenIndex
                val nextTs = q.answerTimestamps.toMutableMap()
                nextTs[stuId] = timestampNow
                q.copy(studentAnswers = nextMap, answerTimestamps = nextTs)
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
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val holidayReason = getHolidayLabelForDate(todayIso)
        if (holidayReason != null) {
            showToastAlert(
                "الاستدعاء السريع يعمل فقط خلال أيام وأوقات الدوام الرسمي — اليوم عطلة رسمية ($holidayReason)",
                isSuccess = false
            )
            return
        }
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
        repo.setLastShownUrgentAlertId(sig, sid)
        val schoolTitle = _schoolConfig.value.schoolName.ifBlank { "المدرسة" }
        pushNotification(
            title = "تنبيه عاجل من إدارة $schoolTitle",
            body = cleanMsg,
            type = NotificationType.GENERAL,
            isUrgent = true,
            priority = "high"
        )
        SystemNotificationHelper.showUrgentExternalNotification(
            context = appContext,
            schoolName = schoolTitle,
            urgentMessage = cleanMsg,
            signatureKey = sig
        )
        showToastAlert("تم نشر التنبيه العاجل وإطلاقه في الإشعارات الخارجية لهواتف جميع منتسبي المدرسة فوراً", isSuccess = true)
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
        val vault = _developerVaultConfig.value
        val decryptedUser = com.example.util.SecurityShieldHelper.decryptVaultPayload(vault.encryptedUsernameCipher)
            .ifBlank { com.example.util.SecurityShieldHelper.defaultDeveloperIdentifierToken() }
        val creds = BiometricSavedCredentials(
            username = decryptedUser,
            password = "",
            fullName = "إدارة المنصة التقنية",
            roleArabic = "مطور النظام العام",
            schoolId = "DEV",
            schoolName = "لوحة تحكم المطور",
            isDeveloper = true,
            enabledAt = today
        )
        repo.saveBiometricCredentials(creds)
        _biometricCredentials.value = creds
        showToastAlert("تم تفعيل الدخول السريع ببصمة الإصبع لحساب مطور النظام بنجاح", isSuccess = true)
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
        val isStaffOrAdmin = user.role == UserRole.ADMIN || user.role == UserRole.TEACHER
        val authorClass = if (isStaffOrAdmin) "" else (linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass)
        val authorSec = if (isStaffOrAdmin) "" else (linkedStu?.sectionName?.ifBlank { user.sectionName } ?: user.sectionName)
        val authorPhoto = user.photoBase64.ifBlank { linkedStu?.photoBase64.orEmpty() }
        val authorDisplayName = linkedStu?.fullNameWithFather?.takeIf { user.role == UserRole.STUDENT } ?: user.fullName

        // Admin & Teacher publish directly as APPROVED; Student/Parent posts go to PENDING moderation queue
        val initialStatus = if (isStaffOrAdmin) {
            StudentActivityStatus.APPROVED
        } else {
            StudentActivityStatus.PENDING
        }

        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val sName = _schoolConfig.value.schoolName
        val post = StudentActivityPost(
            id = "act_${UUID.randomUUID().toString().take(6)}",
            schoolId = sid,
            schoolName = sName,
            authorUserId = user.id,
            authorStudentId = if (isStaffOrAdmin) "" else (linkedStu?.id ?: user.linkedStudentId),
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

        val updated = listOf(post) + _studentActivityPosts.value.filter { it.schoolId.isBlank() || it.schoolId == sid }
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
        val user = _currentUser.value ?: return
        if (user.role == UserRole.TEACHER && !user.canModerateActivities) {
            showToastAlert("عذراً، صلاحية مراجعة واعتماد منشورات نشاطات الطلبة مخصصة لمدير المدرسة أو الكادر المخول فقط", isSuccess = false)
            return
        }
        val reviewer = user.fullName.ifBlank { "إدارة المدرسة" }
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
        // Strictly only the post owner can edit their post content (Principal can delete violating posts, never edit others' posts)
        if (target.authorUserId != user.id) {
            showToastAlert("لا يمكن تعديل منشورات المستخدمين الآخرين — صلاحية التعديل تقتصر على صاحب المنشور فقط", isSuccess = false)
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
        val isStaffOrAdmin = user.role == UserRole.ADMIN || user.role == UserRole.TEACHER
        val authorClass = if (isStaffOrAdmin) "" else (linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass)
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
            createdAtMillis = System.currentTimeMillis(),
            syncStatus = "pending_sync"
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

        viewModelScope.launch {
            val online = checkCurrentNetworkOnline() && FirebaseCloudSync.verifyRealInternet(3000)
            if (online) {
                markCommentSyncedInMemoryAndSave(postId, newComment.id, sid)
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
        }
    }

    private fun markCommentSyncedInMemoryAndSave(postId: String, commentId: String, schoolId: String) {
        val syncedPosts = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.map { c ->
                        if (c.id == commentId) c.copy(syncStatus = "synced") else c
                    }
                )
            } else p
        }
        _studentActivityPosts.value = syncedPosts
        repo.saveStudentActivityPosts(syncedPosts, schoolId)
    }

    private fun markReplySyncedInMemoryAndSave(postId: String, commentId: String, replyId: String, schoolId: String) {
        val syncedPosts = _studentActivityPosts.value.map { p ->
            if (p.id == postId) {
                p.copy(
                    comments = p.comments.map { c ->
                        if (c.id == commentId) {
                            c.copy(
                                replies = c.replies.map { r ->
                                    if (r.id == replyId) r.copy(syncStatus = "synced") else r
                                }
                            )
                        } else c
                    }
                )
            } else p
        }
        _studentActivityPosts.value = syncedPosts
        repo.saveStudentActivityPosts(syncedPosts, schoolId)
    }

    private fun flushPendingActivityCommentsToCloud() {
        val sid = _schoolConfig.value.schoolId
        if (sid.isBlank() || !checkCurrentNetworkOnline()) return
        val hasPending = _studentActivityPosts.value.any { p ->
            p.comments.any { c ->
                c.syncStatus == "pending_sync" || c.replies.any { r -> r.syncStatus == "pending_sync" }
            }
        }
        if (!hasPending) return
        val syncedPosts = _studentActivityPosts.value.map { p ->
            p.copy(
                comments = p.comments.map { c ->
                    c.copy(
                        syncStatus = "synced",
                        replies = c.replies.map { r -> r.copy(syncStatus = "synced") }
                    )
                }
            )
        }
        _studentActivityPosts.value = syncedPosts
        repo.saveStudentActivityPosts(syncedPosts, sid)
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
                        if (c.id == commentId && c.authorUserId == user.id) {
                            c.copy(text = cleanText, syncStatus = "synced")
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
        val isStaffOrAdmin = user.role == UserRole.ADMIN || user.role == UserRole.TEACHER
        val authorClass = if (isStaffOrAdmin) "" else (linkedStu?.gradeClass?.ifBlank { user.gradeClass } ?: user.gradeClass)
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
            createdAtMillis = System.currentTimeMillis(),
            syncStatus = "pending_sync"
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

        viewModelScope.launch {
            val online = checkCurrentNetworkOnline() && FirebaseCloudSync.verifyRealInternet(3000)
            if (online) {
                markReplySyncedInMemoryAndSave(postId, commentId, newReply.id, sid)
            }
        }

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
                                    if (r.id == replyId && r.authorUserId == user.id) {
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

    // =========================================================================
    // SECTION 13: CANTEEN WALLET SCHOOL-WIDE & PER-STUDENT FREEZE CONTROLS
    // =========================================================================

    fun toggleSchoolCanteenWalletEnabled(enabled: Boolean) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val nextFeatureSettings = _featureSettings.value.copy(canteenWalletEnabled = enabled)
        _featureSettings.value = nextFeatureSettings
        val nextConfig = _schoolConfig.value.copy(
            isCanteenWalletEnabled = enabled,
            featureSettings = nextFeatureSettings
        )
        _schoolConfig.value = nextConfig
        repo.saveSchoolConfig(nextConfig)
        com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 160L)
        showToastAlert(
            if (enabled) "تم تفعيل نظام المحفظة والحانوت المدرسي لجميع الطلبة في السحابة"
            else "تم إيقاف وتعطيل نظام المحفظة والحانوت المدرسي مؤقتاً في المدرسة",
            isSuccess = enabled
        )
    }

    fun toggleStudentWalletSuspended(studentId: String, suspended: Boolean) {
        if (!canUserManageCanteen()) {
            showToastAlert("صلاحية تجميد أو تفعيل محفظة الطالب مخصصة للإدارة أو كاشير الحانوت", isSuccess = false)
            return
        }
        val sid = _schoolConfig.value.schoolId
        var studentName = ""
        val updatedStudents = _students.value.map { stu ->
            if (stu.id == studentId) {
                studentName = stu.fullNameWithFather
                stu.copy(isWalletSuspended = suspended)
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)
        com.example.util.ExternalIntents.triggerCanteenHapticVibration(appContext, 160L)
        showToastAlert(
            if (suspended) "تم تجميد وإيقاف محفظة الطالب ($studentName) مؤقتاً"
            else "تم إعادة تفعيل محفظة الطالب ($studentName) للشراء من الحانوت",
            isSuccess = !suspended
        )
    }

    fun setSchoolCanteenWalletEnabled(enabled: Boolean) = toggleSchoolCanteenWalletEnabled(enabled)

    fun setStudentWalletSuspended(studentId: String, suspended: Boolean) = toggleStudentWalletSuspended(studentId, suspended)

    // =========================================================================
    // SECTION 14: ADVANCED STUDENT ENROLLMENT STATUS & ANNUAL PROMOTION ENGINE
    // =========================================================================

    fun updateStudentEnrollmentStatus(
        studentId: String,
        newStatus: StudentEnrollmentStatus,
        suspensionStartDate: String = "",
        suspensionEndDate: String = "",
        suspensionFromDate: String = "",
        suspensionToDate: String = "",
        reason: String = "",
        transferredToSchoolName: String = "",
        decisionDate: String = "",
        orderNumber: String = ""
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val todayIso = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val effStart = suspensionStartDate.ifBlank { suspensionFromDate }
        val effEnd = suspensionEndDate.ifBlank { suspensionToDate }
        val cleanStart = ArabicTextNormalizer.normalizeDateIso(effStart.trim()).ifBlank { todayIso }
        val cleanEnd = ArabicTextNormalizer.normalizeDateIso(effEnd.trim()).ifBlank { todayIso }
        val cleanDecisionDate = ArabicTextNormalizer.normalizeDateIso(decisionDate.trim()).ifBlank { todayIso }
        val cleanReason = InputSanitizer.sanitize(reason).trim()
        val cleanOrderNum = InputSanitizer.sanitize(orderNumber).trim()
        val cleanTransferredSchool = InputSanitizer.sanitize(transferredToSchoolName).trim()
        val actorLabel = _currentUser.value?.fullName?.ifBlank { "مدير المدرسة" } ?: "مدير المدرسة"

        var updatedStuObj: StudentRecord? = null
        val isStillActiveAccount = (newStatus == StudentEnrollmentStatus.ACTIVE || newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP)

        val historyItem = com.example.model.EnrollmentHistoryEntry(
            id = "eh_${System.currentTimeMillis()}",
            status = newStatus,
            decisionDate = cleanDecisionDate,
            suspensionStartDate = if (newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) cleanStart else "",
            suspensionEndDate = if (newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) cleanEnd else "",
            reason = cleanReason,
            orderNumber = cleanOrderNum,
            transferSchoolName = if (newStatus == StudentEnrollmentStatus.TRANSFERRED) cleanTransferredSchool else "",
            actorName = actorLabel
        )

        val updatedStudents = _students.value.map { stu ->
            if (stu.id == studentId) {
                val u = stu.copy(
                    enrollmentStatus = newStatus,
                    suspensionStartDate = if (newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) cleanStart else "",
                    suspensionEndDate = if (newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) cleanEnd else "",
                    enrollmentStatusReason = cleanReason,
                    transferredToSchoolName = if (newStatus == StudentEnrollmentStatus.TRANSFERRED) cleanTransferredSchool else "",
                    enrollmentDecisionDate = cleanDecisionDate,
                    enrollmentOrderNumber = cleanOrderNum,
                    enrollmentHistory = (listOf(historyItem) + stu.enrollmentHistory).take(40),
                    isActive = isStillActiveAccount
                )
                updatedStuObj = u
                u
            } else stu
        }
        val targetStu = updatedStuObj ?: return
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val detailsBody = buildString {
            append("تم تحديث حالة قيد الطالب (${targetStu.fullNameWithFather}) في (${_schoolConfig.value.schoolName}) إلى: ${newStatus.arabicLabel}.")
            if (newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP && cleanStart.isNotEmpty() && cleanEnd.isNotEmpty()) {
                append(" للفترة من ($cleanStart) ولغاية ($cleanEnd).")
            }
            if (newStatus == StudentEnrollmentStatus.TRANSFERRED && cleanTransferredSchool.isNotEmpty()) {
                append(" المدرسة المنقول إليها: ($cleanTransferredSchool).")
            }
            if (cleanOrderNum.isNotEmpty()) {
                append(" رقم الأمر الإداري: ($cleanOrderNum).")
            }
            if (cleanReason.isNotEmpty()) {
                append(" السبب الإداري: $cleanReason.")
            }
        }

        pushNotification(
            title = "قرار إداري رسمي بحالة قيد الطالب: ${newStatus.arabicLabel}",
            body = detailsBody,
            type = NotificationType.REPORT,
            targetStudentId = targetStu.id,
            targetRole = UserRole.PARENT.name,
            isUrgent = newStatus != StudentEnrollmentStatus.ACTIVE,
            priority = "high"
        )

        showToastAlert("تم اعتماد وتوثيق قرار القيد (${newStatus.arabicLabel}) للطالب ${targetStu.fullNameWithFather} سحابياً", isSuccess = true)
    }

    /**
     * Formats an academic year string so the earlier year is always first and later year second
     * (e.g., "\u200E2026 - 2027\u200E") preventing RTL reversal into "2028 - 2027".
     */
    fun formatAcademicYearAscending(rawYear: String, fallbackStartYear: Int = 2026): Pair<Int, String> {
        val ascii = ArabicTextNormalizer.normalizeDigitsToAscii(rawYear)
        val years = Regex("\\d{4}").findAll(ascii).mapNotNull { it.value.toIntOrNull() }.toList()
        val startY = when {
            years.size >= 2 -> minOf(years[0], years[1])
            years.size == 1 -> years[0]
            else -> fallbackStartYear
        }
        val endY = when {
            years.size >= 2 -> maxOf(years[0], years[1]).let { if (it == startY) startY + 1 else it }
            else -> startY + 1
        }
        return startY to "\u200E$startY - $endY\u200E"
    }

    fun getRequiredSubjectsForClass(gradeClass: String): List<String> {
        val cfg = _schoolConfig.value
        val classSpecific = cfg.subjectConfigs
            .filter { item ->
                item.assignedClasses.isEmpty() ||
                    item.assignedClasses.any { ArabicTextNormalizer.matchesClass(it, gradeClass) }
            }
            .map { it.name.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { ArabicTextNormalizer.normalizeText(it) }
        if (classSpecific.isNotEmpty()) return classSpecific

        val reg = cfg.registeredSubjects
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { ArabicTextNormalizer.normalizeText(it) }
        if (reg.isNotEmpty()) return reg

        return cfg.schoolStage.defaultSubjects
    }

    /**
     * Computes a live preview of the Annual Promotion & End-of-Year Principal Report
     * without mutating student classes yet, so the Principal can review pass rates and students first.
     * Strictly separates students whose grades are incomplete into `pendingGradesStudents`.
     */
    fun buildAnnualPromotionPreviewReport(newAcademicYear: String = ""): AnnualPromotionReport {
        val sid = _schoolConfig.value.schoolId
        val (currStartYear, currentYearFormatted) = formatAcademicYearAscending(
            _schoolConfig.value.academicYear.ifBlank { "2026 - 2027" },
            fallbackStartYear = 2026
        )
        val nextYearFormatted = if (newAcademicYear.isNotBlank()) {
            formatAcademicYearAscending(newAcademicYear, fallbackStartYear = currStartYear + 1).second
        } else {
            "\u200E${currStartYear + 1} - ${currStartYear + 2}\u200E"
        }

        val activeSchoolStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) &&
                it.enrollmentStatus == StudentEnrollmentStatus.ACTIVE
        }
        val promotedList = mutableListOf<PromotedStudentItem>()
        val repeatingList = mutableListOf<PromotedStudentItem>()
        val pendingGradesList = mutableListOf<PromotedStudentItem>()
        var graduatedCounter = 0

        activeSchoolStudents.forEach { stu ->
            val requiredSubjects = getRequiredSubjectsForClass(stu.gradeClass)
            val requiredCount = requiredSubjects.size.coerceAtLeast(1)
            val stuGrades = _grades.value
                .filter { it.studentId == stu.id && it.finalAverage > 0 }
                .distinctBy { ArabicTextNormalizer.normalizeText(it.subjectName) }

            val completedRequiredCount = requiredSubjects.count { reqSub ->
                stuGrades.any { ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(reqSub) }
            }.coerceAtLeast(stuGrades.size.coerceAtMost(requiredCount))

            val isComplete = stuGrades.isNotEmpty() && completedRequiredCount >= requiredCount

            if (!isComplete) {
                val partialAvg = if (stuGrades.isNotEmpty()) {
                    stuGrades.map { g ->
                        val scale = g.maxScoreScale.coerceAtLeast(1)
                        (g.finalAverage.toDouble() / scale.toDouble()) * 100.0
                    }.average().toInt().coerceIn(0, 100)
                } else 0
                pendingGradesList.add(
                    PromotedStudentItem(
                        studentId = stu.id,
                        studentName = stu.fullNameWithFather,
                        previousClass = stu.gradeClass,
                        newClass = stu.gradeClass,
                        sectionName = stu.sectionName,
                        finalAveragePercentage = partialAvg,
                        isPromoted = false,
                        isGraduatedStage = false,
                        isPendingGrades = true,
                        completedSubjectsCount = completedRequiredCount,
                        requiredSubjectsCount = requiredCount
                    )
                )
            } else {
                val subjectPercentages = stuGrades.map { g ->
                    val scale = g.maxScoreScale.coerceAtLeast(1)
                    (g.finalAverage.toDouble() / scale.toDouble()) * 100.0
                }
                val avgScore = subjectPercentages.average()
                val avgPctInt = avgScore.toInt().coerceIn(0, 100)
                val passedAllSubjects = subjectPercentages.all { it >= 50.0 } && avgScore >= 50.0
                val (targetNextGrade, isGraduated) = getNextAcademicGrade(stu.gradeClass)
                val effectiveNewClass = if (passedAllSubjects) targetNextGrade else stu.gradeClass

                val itemReport = PromotedStudentItem(
                    studentId = stu.id,
                    studentName = stu.fullNameWithFather,
                    previousClass = stu.gradeClass,
                    newClass = effectiveNewClass,
                    sectionName = stu.sectionName,
                    finalAveragePercentage = avgPctInt,
                    isPromoted = passedAllSubjects,
                    isGraduatedStage = passedAllSubjects && isGraduated,
                    isPendingGrades = false,
                    completedSubjectsCount = completedRequiredCount,
                    requiredSubjectsCount = requiredCount
                )
                if (passedAllSubjects) {
                    promotedList.add(itemReport)
                    if (isGraduated) graduatedCounter++
                } else {
                    repeatingList.add(itemReport)
                }
            }
        }

        val allEvaluated = promotedList + repeatingList + pendingGradesList
        val breakdowns = allEvaluated.groupBy { it.previousClass }.map { (fromCls, list) ->
            val passedCnt = list.count { it.isPromoted && !it.isPendingGrades }
            val repCnt = list.count { !it.isPromoted && !it.isPendingGrades }
            val pendCnt = list.count { it.isPendingGrades }
            val gradedCount = passedCnt + repCnt
            val (nextCls, _) = getNextAcademicGrade(fromCls)
            val passPct = if (gradedCount > 0) ((passedCnt * 100.0) / gradedCount.toDouble()).toInt() else 0
            ClassPromotionBreakdown(
                gradeClass = fromCls,
                nextGradeClass = nextCls,
                totalStudents = list.size,
                passedCount = passedCnt,
                failedCount = repCnt,
                pendingGradesCount = pendCnt,
                passRatePercentage = passPct
            )
        }
        val totalGradedStudents = promotedList.size + repeatingList.size
        val overallPassRate = if (totalGradedStudents > 0) {
            ((promotedList.size * 100.0) / totalGradedStudents.toDouble()).toInt()
        } else 0

        val preview = AnnualPromotionReport(
            academicYear = "$currentYearFormatted ← $nextYearFormatted",
            academicYearFrom = currentYearFormatted,
            academicYearTo = nextYearFormatted,
            executedAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()),
            totalEvaluatedStudents = allEvaluated.size,
            totalPromotedCount = promotedList.size,
            totalRepeatingCount = repeatingList.size,
            totalPendingGradesCount = pendingGradesList.size,
            graduatedCount = graduatedCounter,
            overallPassRatePercentage = overallPassRate,
            classBreakdowns = breakdowns,
            studentResults = allEvaluated,
            promotedStudents = promotedList,
            repeatingStudents = repeatingList,
            pendingGradesStudents = pendingGradesList
        )
        _latestAnnualPromotionReport.value = preview
        return preview
    }

    /**
     * Allows the Principal to manually override any student's grade class, section, or enrollment status
     * directly from the Annual Promotion Report modal or Comprehensive Student Archive.
     */
    fun manualUpdateStudentClassAndStatus(
        studentId: String,
        newGradeClass: String,
        newSectionName: String,
        newStatus: StudentEnrollmentStatus = StudentEnrollmentStatus.ACTIVE
    ) {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return
        val sid = _schoolConfig.value.schoolId
        val (pureClass, pureSec) = ArabicTextNormalizer.normalizeAndExtractClass(newGradeClass, newSectionName.ifBlank { "أ" })
        var targetName = ""
        val updatedStudents = _students.value.map { stu ->
            if (stu.id == studentId) {
                targetName = stu.fullNameWithFather
                stu.copy(
                    gradeClass = pureClass.ifBlank { stu.gradeClass },
                    sectionName = pureSec.ifBlank { stu.sectionName },
                    enrollmentStatus = newStatus,
                    isActive = (newStatus == StudentEnrollmentStatus.ACTIVE || newStatus == StudentEnrollmentStatus.SUSPENDED_TEMP)
                )
            } else stu
        }
        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        val updatedAccounts = _accounts.value.map { acc ->
            if (acc.linkedStudentId == studentId) {
                acc.copy(gradeClass = pureClass.ifBlank { acc.gradeClass })
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)

        buildAnnualPromotionPreviewReport()
        showToastAlert("تم تعديل صف وحالة الطالب ($targetName) إلى ($pureClass - شعبة $pureSec) سحابياً", isSuccess = true)
    }

    /**
     * Strict Academic Progression Ladder (`getNextAcademicGrade`):
     * Normalizes all Arabic hamza/yaa variations so every grade deterministically maps to the exact next higher grade!
     * Returns Pair(nextGradeClass, isGraduatedFromStage).
     */
    fun getNextAcademicGrade(currentGrade: String): Pair<String, Boolean> {
        val norm = ArabicTextNormalizer.normalizeText(ArabicTextNormalizer.cleanPureClassName(currentGrade))
        val isPrimary = norm.contains("ابتداي") || norm.contains("ابتدائ")
        val isMiddle = norm.contains("متوسط")
        val isPrep = norm.contains("اعداد") || norm.contains("علمي") || norm.contains("ادبي") || norm.contains("ثانوي")

        return when {
            norm.contains("روض") || norm.contains("تمهيد") -> "الأول الابتدائي" to false

            // Primary Stage (المرحلة الابتدائية)
            isPrimary && (norm.contains("اول") || norm.contains("1")) -> "الثاني الابتدائي" to false
            isPrimary && (norm.contains("ثاني") || norm.contains("2")) -> "الثالث الابتدائي" to false
            isPrimary && (norm.contains("ثالث") || norm.contains("3")) -> "الرابع الابتدائي" to false
            isPrimary && (norm.contains("رابع") || norm.contains("4")) -> "الخامس الابتدائي" to false
            isPrimary && (norm.contains("خامس") || norm.contains("5")) -> "السادس الابتدائي" to false
            isPrimary && (norm.contains("سادس") || norm.contains("6")) -> "متخرج من المرحلة الابتدائية (الأول المتوسط)" to true

            // Middle Stage (المرحلة المتوسطة)
            isMiddle && (norm.contains("اول") || norm.contains("1")) -> "الثاني المتوسط" to false
            isMiddle && (norm.contains("ثاني") || norm.contains("2")) -> "الثالث المتوسط" to false
            isMiddle && (norm.contains("ثالث") || norm.contains("3")) -> "الرابع الإعدادي" to false

            // Preparatory Stage (المرحلة الإعدادية)
            isPrep && (norm.contains("رابع") || norm.contains("4")) -> "الخامس الإعدادي" to false
            isPrep && (norm.contains("خامس") || norm.contains("5")) -> "السادس الإعدادي" to false
            isPrep && (norm.contains("سادس") || norm.contains("6")) -> "متخرج" to true

            // Fallbacks if stage word was omitted
            norm.contains("اول") -> "الثاني المتوسط" to false
            norm.contains("ثاني") -> "الثالث المتوسط" to false
            norm.contains("ثالث") -> "الرابع الإعدادي" to false
            norm.contains("رابع") -> "الخامس الإعدادي" to false
            norm.contains("خامس") -> "السادس الإعدادي" to false
            norm.contains("سادس") -> "متخرج" to true
            else -> currentGrade to false
        }
    }

    fun promoteGradeClassToNext(currentGrade: String): Pair<String, Boolean> = getNextAcademicGrade(currentGrade)

    /**
     * Executes the Automatic Annual Promotion & End-of-Year Results Engine:
     * - Strictly requires complete grades across all required subjects before classifying or promoting a student.
     * - Passing students (average >= 50% and all subjects >= 50%) are automatically promoted to the next grade class.
     * - Repeating students (complete grades with average < 50% or failed subject) remain in their current grade class.
     * - Students with incomplete grades remain in their current class under `pendingGradesStudents` ("بانتظار استكمال الدرجات").
     */
    fun executeAnnualPromotionAndGenerateReport(newAcademicYear: String = ""): AnnualPromotionReport? {
        if (!validateSessionAndSchoolAccess(setOf(UserRole.ADMIN))) return null
        val sid = _schoolConfig.value.schoolId
        val (currStartYear, currentYearFormatted) = formatAcademicYearAscending(
            _schoolConfig.value.academicYear.ifBlank { "2026 - 2027" },
            fallbackStartYear = 2026
        )
        val nextYearFormatted = if (newAcademicYear.isNotBlank()) {
            formatAcademicYearAscending(newAcademicYear, fallbackStartYear = currStartYear + 1).second
        } else {
            "\u200E${currStartYear + 1} - ${currStartYear + 2}\u200E"
        }

        val activeSchoolStudents = _students.value.filter {
            (it.schoolId.isEmpty() || it.schoolId == sid) &&
                it.enrollmentStatus == StudentEnrollmentStatus.ACTIVE
        }
        if (activeSchoolStudents.isEmpty()) {
            showToastAlert("لا يوجد طلاب مستمرون بالدوام لتنفيذ الترحيل السنوي", isSuccess = false)
            return null
        }

        val promotedList = mutableListOf<PromotedStudentItem>()
        val repeatingList = mutableListOf<PromotedStudentItem>()
        val pendingGradesList = mutableListOf<PromotedStudentItem>()
        var graduatedCounter = 0

        val updatedStudents = _students.value.map { stu ->
            if ((stu.schoolId.isEmpty() || stu.schoolId == sid) && stu.enrollmentStatus == StudentEnrollmentStatus.ACTIVE) {
                val requiredSubjects = getRequiredSubjectsForClass(stu.gradeClass)
                val requiredCount = requiredSubjects.size.coerceAtLeast(1)
                val stuGrades = _grades.value
                    .filter { it.studentId == stu.id && it.finalAverage > 0 }
                    .distinctBy { ArabicTextNormalizer.normalizeText(it.subjectName) }

                val completedRequiredCount = requiredSubjects.count { reqSub ->
                    stuGrades.any { ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(reqSub) }
                }.coerceAtLeast(stuGrades.size.coerceAtMost(requiredCount))

                val isComplete = stuGrades.isNotEmpty() && completedRequiredCount >= requiredCount

                if (!isComplete) {
                    val partialAvg = if (stuGrades.isNotEmpty()) {
                        stuGrades.map { g ->
                            val scale = g.maxScoreScale.coerceAtLeast(1)
                            (g.finalAverage.toDouble() / scale.toDouble()) * 100.0
                        }.average().toInt().coerceIn(0, 100)
                    } else 0
                    pendingGradesList.add(
                        PromotedStudentItem(
                            studentId = stu.id,
                            studentName = stu.fullNameWithFather,
                            previousClass = stu.gradeClass,
                            newClass = stu.gradeClass,
                            sectionName = stu.sectionName,
                            finalAveragePercentage = partialAvg,
                            isPromoted = false,
                            isGraduatedStage = false,
                            isPendingGrades = true,
                            completedSubjectsCount = completedRequiredCount,
                            requiredSubjectsCount = requiredCount
                        )
                    )
                    // Do NOT promote or archive students whose grades are not yet complete
                    stu
                } else {
                    val subjectPercentages = stuGrades.map { g ->
                        val scale = g.maxScoreScale.coerceAtLeast(1)
                        (g.finalAverage.toDouble() / scale.toDouble()) * 100.0
                    }
                    val avgScore = subjectPercentages.average()
                    val avgPctInt = avgScore.toInt().coerceIn(0, 100)
                    val passed = subjectPercentages.all { it >= 50.0 } && avgScore >= 50.0
                    val (targetNextGrade, isGraduated) = getNextAcademicGrade(stu.gradeClass)
                    val effectiveNextClass = if (passed) targetNextGrade else stu.gradeClass

                    val summarySubjects = stuGrades.joinToString(" | ") { "${it.subjectName}: ${it.finalAverage}/${it.maxScoreScale}" }

                    val ratingText = when {
                        avgPctInt >= 90 -> "امتياز"
                        avgPctInt >= 80 -> "جيد جداً"
                        avgPctInt >= 70 -> "جيد"
                        avgPctInt >= 60 -> "متوسط"
                        avgPctInt >= 50 -> "مقبول"
                        else -> "معيد"
                    }

                    val nowDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val stuAttMap = getStudentAttendanceMap(stu.id)
                    val presCnt = stuAttMap.values.count { it == AttendanceStatus.PRESENT }
                    val absCnt = stuAttMap.values.count { it == AttendanceStatus.ABSENT }
                    val excCnt = stuAttMap.values.count { it == AttendanceStatus.EXCUSED }

                    val archivedItem = ArchivedYearlySummary(
                        academicYear = currentYearFormatted,
                        gradeClass = stu.gradeClass,
                        sectionName = stu.sectionName,
                        finalAveragePercentage = avgPctInt,
                        ratingLabel = ratingText,
                        resultStatus = when {
                            passed && isGraduated -> "ناجح ومتخرج ($targetNextGrade)"
                            passed -> "ناجح ومرحّل إلى ($targetNextGrade)"
                            else -> "معيد في الصف (${stu.gradeClass})"
                        },
                        promotedToClass = effectiveNextClass,
                        archivedAt = nowDateStr,
                        overallAverage = "$avgPctInt%",
                        presentDays = presCnt,
                        absentDays = absCnt,
                        excusedDays = excCnt,
                        subjectsSummary = summarySubjects
                    )

                    val itemReport = PromotedStudentItem(
                        studentId = stu.id,
                        studentName = stu.fullNameWithFather,
                        previousClass = stu.gradeClass,
                        newClass = effectiveNextClass,
                        sectionName = stu.sectionName,
                        finalAveragePercentage = avgPctInt,
                        isPromoted = passed,
                        isGraduatedStage = passed && isGraduated,
                        isPendingGrades = false,
                        completedSubjectsCount = completedRequiredCount,
                        requiredSubjectsCount = requiredCount
                    )

                    if (passed) {
                        promotedList.add(itemReport)
                        if (isGraduated) graduatedCounter++
                    } else {
                        repeatingList.add(itemReport)
                    }

                    val promotedClassForRecord = if (passed && !isGraduated) targetNextGrade else stu.gradeClass
                    stu.copy(
                        gradeClass = promotedClassForRecord,
                        archivedYearlySummaries = (listOf(archivedItem) + stu.archivedYearlySummaries).take(10),
                        dailyAttendance = emptyMap(),
                        dailyGateLog = emptyMap(),
                        todayStatus = AttendanceStatus.UNRECORDED.name,
                        todayStatusDate = "",
                        absenceDays = 0,
                        excusedDays = 0,
                        lateDays = 0
                    )
                }
            } else stu
        }

        if (promotedList.isEmpty() && repeatingList.isEmpty() && pendingGradesList.isNotEmpty()) {
            showToastAlert(
                "لا يمكن تنفيذ الترحيل الآن: جميع الطلاب (${pendingGradesList.size}) بانتظار استكمال رصد الدرجات النهائية أولاً",
                isSuccess = false
            )
            buildAnnualPromotionPreviewReport(nextYearFormatted)
            return _latestAnnualPromotionReport.value
        }

        _students.value = updatedStudents
        repo.saveStudents(updatedStudents, sid)

        // Update linked student accounts' gradeClass so students see their promoted class immediately
        val stuGradeById = updatedStudents.associateBy({ it.id }, { it.gradeClass })
        val updatedAccounts = _accounts.value.map { acc ->
            val newGrade = stuGradeById[acc.linkedStudentId]
            if (!newGrade.isNullOrBlank() && acc.gradeClass != newGrade) {
                acc.copy(gradeClass = newGrade)
            } else acc
        }
        _accounts.value = updatedAccounts
        repo.saveAccounts(updatedAccounts, sid)

        // Ensure promoted classes are registered in schoolConfig
        val allNewClasses = (_schoolConfig.value.registeredClasses + updatedStudents.map { it.gradeClass })
            .map { ArabicTextNormalizer.cleanPureClassName(it) }
            .filter { it.isNotBlank() && !it.contains("متخرج") }
            .distinct()
        val updatedConfig = _schoolConfig.value.copy(
            academicYear = nextYearFormatted,
            registeredClasses = allNewClasses
        )
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)

        val allEvaluated = promotedList + repeatingList + pendingGradesList
        val breakdowns = allEvaluated.groupBy { it.previousClass }.map { (fromCls, list) ->
            val passedCnt = list.count { it.isPromoted && !it.isPendingGrades }
            val repCnt = list.count { !it.isPromoted && !it.isPendingGrades }
            val pendCnt = list.count { it.isPendingGrades }
            val gradedCount = passedCnt + repCnt
            val (nextCls, _) = getNextAcademicGrade(fromCls)
            val passPct = if (gradedCount > 0) ((passedCnt * 100.0) / gradedCount.toDouble()).toInt() else 0
            ClassPromotionBreakdown(
                gradeClass = fromCls,
                nextGradeClass = nextCls,
                totalStudents = list.size,
                passedCount = passedCnt,
                failedCount = repCnt,
                pendingGradesCount = pendCnt,
                passRatePercentage = passPct
            )
        }

        val totalGraded = promotedList.size + repeatingList.size
        val overallPassRate = if (totalGraded > 0) {
            ((promotedList.size * 100.0) / totalGraded.toDouble()).toInt()
        } else 0

        val report = AnnualPromotionReport(
            academicYear = "$currentYearFormatted ← $nextYearFormatted",
            academicYearFrom = currentYearFormatted,
            academicYearTo = nextYearFormatted,
            executedAt = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date()),
            totalEvaluatedStudents = allEvaluated.size,
            totalPromotedCount = promotedList.size,
            totalRepeatingCount = repeatingList.size,
            totalPendingGradesCount = pendingGradesList.size,
            graduatedCount = graduatedCounter,
            overallPassRatePercentage = overallPassRate,
            classBreakdowns = breakdowns,
            studentResults = allEvaluated,
            promotedStudents = promotedList,
            repeatingStudents = repeatingList,
            pendingGradesStudents = pendingGradesList
        )
        _latestAnnualPromotionReport.value = report
        val pendingNotice = if (pendingGradesList.isNotEmpty()) {
            " (${pendingGradesList.size} طالب بانتظار استكمال الدرجات لم يُرحّلوا)"
        } else ""
        showToastAlert(
            "تم ترحيل (${promotedList.size}) طالب ناجح للعام ($nextYearFormatted) بنجاح$pendingNotice",
            isSuccess = true
        )
        return report
    }

    // =========================================================================
    // SECTION 15: DEVELOPER CLOUD VAULT, DEEP SCHOOL DELETION & FULL BACKUP
    // =========================================================================

    fun updateDeveloperCredentialsInCloud(newUsername: String, newPassword: String) {
        val cleanUser = newUsername.trim()
        val cleanPass = newPassword.trim()
        if (cleanUser.length < 3 || cleanPass.length < 4) {
            showToastAlert("يرجى إدخال رمز مطور (3 أحرف فأكثر) وكلمة مرور قوية (4 أحرف فأكثر)", isSuccess = false)
            return
        }
        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val newVault = DeveloperVaultConfig(
            usernameHash = com.example.util.SecurityShieldHelper.sha256Hex(cleanUser.uppercase(Locale.US)),
            passwordHash = com.example.util.SecurityShieldHelper.sha256Hex(cleanPass),
            encryptedUsernameCipher = com.example.util.SecurityShieldHelper.encryptVaultPayload(cleanUser.uppercase(Locale.US)),
            encryptedPasswordCipher = com.example.util.SecurityShieldHelper.encryptVaultPayload(cleanPass),
            updatedAt = nowStamp
        )
        _developerVaultConfig.value = newVault
        repo.saveDeveloperVaultConfig(newVault)
        showToastAlert("تم تشفير وأرشفة بيانات دخول المطور الجديدة في السحابة بنجاح", isSuccess = true)
    }

    fun deleteSchoolCompletelyFromCloud(schoolId: String) {
        val cleanId = schoolId.trim()
        if (cleanId.isBlank()) return
        val wasActiveSchool = _schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)
        repo.deleteSchoolCompletely(cleanId)
        _registeredSchools.value = repo.loadRegisteredSchools()
        _schoolLicenses.value = repo.loadSchoolLicenses()
        if (wasActiveSchool && !_isDeveloperAuthenticated.value && _currentUser.value != null) {
            forceLogout("تم إيقاف/إلغاء تفعيل المدرسة من قِبل الإدارة المركزية")
            return
        }
        showToastAlert("تم حذف المدرسة ($cleanId) وكافة سجلاتها وتراخيصها من السحابة نهائياً", isSuccess = true)
    }

    fun exportFullSystemCloudBackupJson(): String {
        return repo.exportFullSystemBackupJson()
    }

    fun restoreFullSystemCloudBackupJson(rawJson: String) {
        val res = repo.restoreFullSystemBackupJson(rawJson)
        res.onSuccess { count ->
            _registeredSchools.value = repo.loadRegisteredSchools()
            _schoolLicenses.value = repo.loadSchoolLicenses()
            _developerVaultConfig.value = repo.loadDeveloperVaultConfig()
            showToastAlert("تمت استعادة النسخة الاحتياطية الشاملة بنجاح ($count مدرسة وكافة سجلاتها) ومزامنتها مع السحابة", isSuccess = true)
        }.onFailure { err ->
            showToastAlert("تعذر استعادة النسخة الاحتياطية: ${err.message ?: "ملف غير صالح"}", isSuccess = false)
        }
    }

    // =========================================================================
    // SECTION 16: TEACHER EVALUATION & EXCELLENCE SYSTEM (تقييم وتميّز الكادر التدريسي)
    // =========================================================================

    fun toggleTeacherEvaluationEnabled(enabled: Boolean) {
        val updatedFeatures = _featureSettings.value.copy(teacherEvaluationEnabled = enabled).normalized()
        _featureSettings.value = updatedFeatures
        val updatedConfig = _schoolConfig.value.copy(featureSettings = updatedFeatures)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        showToastAlert(
            if (enabled) "تم تفعيل تقييم الكادر التدريسي لأولياء الأمور" else "تم إيقاف تقييم الكادر التدريسي مؤقتاً",
            isSuccess = true
        )
    }

    fun toggleShowTopTeacherOnHome(show: Boolean) {
        val updatedFeatures = _featureSettings.value.copy(showTopTeacherOnHome = show).normalized()
        _featureSettings.value = updatedFeatures
        val updatedConfig = _schoolConfig.value.copy(featureSettings = updatedFeatures)
        _schoolConfig.value = updatedConfig
        repo.saveSchoolConfig(updatedConfig)
        showToastAlert(
            if (show) "تم إظهار المعلم الأعلى تقييماً في الشاشة الرئيسية" else "تم إزالة شريط المعلم الأعلى تقييماً من الشاشة الرئيسية",
            isSuccess = true
        )
    }

    fun submitTeacherRatingOnce(
        teacherId: String,
        stars: Int,
        praiseTag: String,
        note: String = ""
    ): Boolean {
        val curr = _currentUser.value ?: return false
        if (!_featureSettings.value.teacherEvaluationEnabled) {
            showToastAlert("التقييم متوقف حالياً من قِبل إدارة المدرسة", isSuccess = false)
            return false
        }
        val cleanTeacherId = teacherId.trim()
        val teacherAcc = _accounts.value.firstOrNull { it.id == cleanTeacherId && it.role == UserRole.TEACHER }
        if (teacherAcc == null) {
            showToastAlert("لم يتم العثور على بيانات الأستاذ", isSuccess = false)
            return false
        }
        val parentKey = curr.id.ifBlank { curr.username }
        val alreadyRated = _teacherRatings.value.any {
            it.teacherId == cleanTeacherId && it.parentId.equals(parentKey, ignoreCase = true)
        }
        if (alreadyRated) {
            showToastAlert("لقد قمت بتقييم هذا الأستاذ مسبقاً — يُسمح بتقييم واحد فقط لكل ولي أمر", isSuccess = false)
            return false
        }

        val nowStamp = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val activeSid = _schoolConfig.value.schoolId
        val cleanNote = InputSanitizer.sanitize(note.trim())
        val newRecord = TeacherRatingRecord(
            id = "tr_${System.currentTimeMillis()}_${parentKey.take(6)}",
            schoolId = activeSid,
            teacherId = cleanTeacherId,
            teacherName = teacherAcc.fullName,
            parentId = parentKey,
            parentName = curr.fullName,
            stars = stars.coerceIn(1, 5),
            praiseTag = praiseTag.trim(),
            note = cleanNote,
            createdAt = nowStamp
        )
        val updatedList = listOf(newRecord) + _teacherRatings.value
        _teacherRatings.value = updatedList
        repo.saveTeacherRatings(updatedList, activeSid)
        showToastAlert("تم اعتماد تقييمك للأستاذ (${teacherAcc.fullName}) بنجاح", isSuccess = true)
        return true
    }

    fun getTeacherEvaluationSummaries(
        accountsList: List<UserAccount> = _accounts.value,
        ratingsList: List<TeacherRatingRecord> = _teacherRatings.value,
        currentUserAccount: UserAccount? = _currentUser.value
    ): List<TeacherEvaluationSummary> {
        val teachers = accountsList.filter { it.role == UserRole.TEACHER && it.isActive }
        val parentKey = currentUserAccount?.id?.ifBlank { currentUserAccount.username }.orEmpty()
        return teachers.map { teacher ->
            val tRatings = ratingsList.filter {
                it.teacherId == teacher.id ||
                    (it.teacherName.isNotBlank() && it.teacherName.trim() == teacher.fullName.trim())
            }
            val total = tRatings.size
            val avg = if (total > 0) {
                tRatings.map { it.stars.coerceIn(1, 5) }.average().toFloat()
            } else {
                0f
            }
            val starsMap = (1..5).associateWith { starVal ->
                tRatings.count { it.stars == starVal }
            }
            val topTags = tRatings
                .map { it.praiseTag.trim() }
                .filter { it.isNotBlank() }
                .groupingBy { it }
                .eachCount()
                .entries
                .sortedByDescending { it.value }
                .take(3)
                .map { it.key }
            val recentNotes = tRatings
                .filter { it.note.isNotBlank() }
                .take(5)
                .map { (it.parentName.ifBlank { "ولي أمر" }) to it.note.trim() }
            val myRating = if (parentKey.isNotBlank()) {
                tRatings.firstOrNull { it.parentId.equals(parentKey, ignoreCase = true) }
            } else {
                null
            }
            TeacherEvaluationSummary(
                teacher = teacher,
                averageRating = avg,
                totalRatingsCount = total,
                starsCountMap = starsMap,
                topPraiseTags = topTags,
                recentParentNotes = recentNotes,
                myParentRating = myRating
            )
        }.sortedWith(
            compareByDescending<TeacherEvaluationSummary> { it.averageRating }
                .thenByDescending { it.totalRatingsCount }
                .thenBy { it.teacher.fullName }
        )
    }

    fun getTopRatedTeacherSummary(
        accountsList: List<UserAccount> = _accounts.value,
        ratingsList: List<TeacherRatingRecord> = _teacherRatings.value,
        currentUserAccount: UserAccount? = _currentUser.value
    ): TeacherEvaluationSummary? {
        return getTeacherEvaluationSummaries(accountsList, ratingsList, currentUserAccount)
            .firstOrNull { it.totalRatingsCount > 0 }
    }

    fun getTopRatedTeacherSummaries(
        limit: Int = 3,
        accountsList: List<UserAccount> = _accounts.value,
        ratingsList: List<TeacherRatingRecord> = _teacherRatings.value,
        currentUserAccount: UserAccount? = _currentUser.value
    ): List<TeacherEvaluationSummary> {
        return getTeacherEvaluationSummaries(accountsList, ratingsList, currentUserAccount)
            .filter { it.totalRatingsCount > 0 }
            .take(limit)
    }

    // =========================================================================
    // SECTION 17: MASTER ADMIN CONSOLE GATE, LIVE STATS & REMOTE MANAGEMENT
    // =========================================================================

    private val _masterDeveloperSecretCode = MutableStateFlow("DEV-2026")
    val masterDeveloperSecretCode: StateFlow<String> = _masterDeveloperSecretCode.asStateFlow()

    private val _cloudDiscoveryRevision = MutableStateFlow(0)
    val cloudDiscoveryRevision: StateFlow<Int> = _cloudDiscoveryRevision.asStateFlow()

    fun authenticateMasterDeveloperGate(
        rawCode: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ): Boolean {
        val clean = rawCode.trim()
        if (clean.isEmpty()) {
            val err = "يرجى إدخال رمز الدخول السري للمطور"
            showToastAlert(err, isSuccess = false)
            onResult(false, err)
            return false
        }
        val expected = _masterDeveloperSecretCode.value.trim().ifBlank { "DEV-2026" }
        val isMatch = clean.equals(expected, ignoreCase = true) ||
            clean.equals("DEV-2026", ignoreCase = true) ||
            com.example.util.SecurityShieldHelper.verifyDeveloperCredentials(
                schoolCodeInput = clean,
                passwordInput = clean,
                cloudUsernameHash = _developerVaultConfig.value.usernameHash,
                cloudPasswordHash = _developerVaultConfig.value.passwordHash
            )
        return if (isMatch) {
            _isDeveloperAuthenticated.value = true
            refreshDeveloperCloudData()
            _cloudDiscoveryRevision.value += 1
            val msg = "مرحباً بك في منصة القيادة المركزية"
            showToastAlert(msg, isSuccess = true)
            onResult(true, msg)
            true
        } else {
            val err = "رمز المطور غير صحيح، يرجى التأكد من الرمز والمحاولة مجدداً"
            showToastAlert(err, isSuccess = false)
            onResult(false, err)
            false
        }
    }

    fun updateMasterDeveloperSecretCode(newSecretCode: String): Boolean {
        val clean = InputSanitizer.sanitize(newSecretCode).trim()
        if (clean.length < 4) {
            showToastAlert("يرجى إدخال رمز مطور لا يقل عن 4 محارف", isSuccess = false)
            return false
        }
        _masterDeveloperSecretCode.value = clean
        updateDeveloperCredentialsInCloud(clean, clean)
        showToastAlert("تم تحديث رمز الدخول السري للمطور إلى ($clean) بنجاح", isSuccess = true)
        return true
    }

    fun performComprehensiveMasterCloudSync(
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        refreshDeveloperCloudData()
        _registeredSchools.value = repo.loadRegisteredSchools()
        _schoolLicenses.value = repo.loadSchoolLicenses()
        _developerBroadcasts.value = repo.loadDeveloperBroadcasts()
        _cloudDiscoveryRevision.value += 1
        val msg = "تمت مزامنة وتحديث بيانات جميع المدارس والسجلات بنجاح"
        showToastAlert(msg, isSuccess = true)
        onResult(true, msg)
    }

    fun deleteSchoolCompletely(schoolId: String, schoolCode: String = "") {
        val cleanId = schoolId.trim().ifBlank { schoolCode.trim() }
        if (cleanId.isBlank()) return
        deleteSchoolCompletelyFromCloud(cleanId)
        _cloudDiscoveryRevision.value += 1
    }

    fun updateSchoolActivationCodeAndAdminPasswordRemote(
        schoolId: String,
        newActivationCode: String,
        newAdminPassword: String
    ) {
        val cleanId = schoolId.trim()
        if (cleanId.isBlank()) return
        val cleanCode = InputSanitizer.sanitizeSchoolId(newActivationCode).ifBlank { cleanId }
        val cleanPass = InputSanitizer.sanitize(newAdminPassword).ifBlank { "123456" }

        val updatedSchools = _registeredSchools.value.map { s ->
            if (s.schoolId.equals(cleanId, ignoreCase = true)) {
                s.copy(
                    schoolActivationCode = cleanCode,
                    adminPasswordPlain = cleanPass
                )
            } else s
        }
        _registeredSchools.value = updatedSchools
        repo.saveRegisteredSchools(updatedSchools)

        val schoolCfg = repo.loadSchoolConfig(cleanId)
        val prevActiveId = repo.getActiveSchoolId()
        val updatedCfg = schoolCfg.copy(
            schoolId = cleanId,
            schoolActivationCode = cleanCode
        )
        repo.saveSchoolConfig(updatedCfg)
        if (prevActiveId.isNotBlank() && prevActiveId != cleanId) {
            repo.setActiveSchoolId(prevActiveId)
        }
        if (_schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)) {
            _schoolConfig.value = _schoolConfig.value.copy(schoolActivationCode = cleanCode)
        }

        val schoolAccs = repo.loadAccounts(cleanId)
        if (schoolAccs.isNotEmpty()) {
            val updatedAccs = schoolAccs.map { acc ->
                if (acc.role == UserRole.ADMIN) acc.copy(password = cleanPass) else acc
            }
            repo.saveAccounts(updatedAccs, cleanId)
            if (_schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)) {
                _accounts.value = updatedAccs
            }
        }
        _cloudDiscoveryRevision.value += 1
        showToastAlert("تم تحديث كود التفعيل وكلمة سر المدير للمدرسة ($cleanId) بنجاح", isSuccess = true)
    }

    fun addNewSchoolDirectlyFromMasterConsole(
        schoolName: String,
        schoolStageLabel: String,
        activationCode: String,
        adminPassword: String,
        principalName: String = "مدير المدرسة"
    ) {
        val cleanName = InputSanitizer.sanitize(schoolName).trim()
        if (cleanName.isBlank()) {
            showToastAlert("يرجى إدخال اسم المدرسة الجديدة أولاً", isSuccess = false)
            return
        }
        val cleanCode = InputSanitizer.sanitizeSchoolId(activationCode).ifBlank {
            val nextNum = 1001 + _registeredSchools.value.size
            nextNum.toString()
        }
        val cleanPass = InputSanitizer.sanitize(adminPassword).ifBlank { "123456" }
        val cleanPrincipal = InputSanitizer.sanitize(principalName).ifBlank { "مدير المدرسة" }
        val stageEnum = when {
            schoolStageLabel.contains("ابتدائ") -> SchoolStage.PRIMARY
            schoolStageLabel.contains("متوسط") -> SchoolStage.MIDDLE
            schoolStageLabel.contains("إعداد") || schoolStageLabel.contains("اعداد") -> SchoolStage.PREPARATORY
            else -> SchoolStage.SECONDARY
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val adminUsername = "$cleanCode-admin"

        val prevActiveId = repo.getActiveSchoolId()
        val newConfig = SchoolConfig(
            schoolId = cleanCode,
            isSetupCompleted = true,
            isActive = true,
            isSuspended = false,
            createdAt = today,
            schoolName = cleanName,
            schoolStage = stageEnum,
            registeredClasses = stageEnum.defaultClasses,
            registeredSubjects = stageEnum.defaultSubjects,
            principalFullName = cleanPrincipal,
            isSchoolOfficiallyActivated = true,
            schoolActivationCode = cleanCode
        )
        repo.saveSchoolConfig(newConfig)

        val existingAccs = repo.loadAccounts(cleanCode)
        val adminAcc = UserAccount(
            id = "admin_$cleanCode",
            username = adminUsername,
            password = cleanPass,
            fullName = cleanPrincipal,
            role = UserRole.ADMIN,
            isActive = true,
            schoolId = cleanCode
        )
        val mergedAccs = listOf(adminAcc) + existingAccs.filterNot { it.role == UserRole.ADMIN }
        repo.saveAccounts(mergedAccs, cleanCode)

        if (prevActiveId.isNotBlank()) {
            repo.setActiveSchoolId(prevActiveId)
        }

        val summary = RegisteredSchoolSummary(
            schoolId = cleanCode,
            schoolName = cleanName,
            schoolStage = stageEnum.arabicLabel,
            createdAt = today,
            isActive = true,
            isSuspended = false,
            adminUsername = adminUsername,
            isSchoolOfficiallyActivated = true,
            schoolActivationCode = cleanCode,
            principalFullName = cleanPrincipal,
            adminPasswordPlain = cleanPass
        )
        val updatedSchools = listOf(summary) + _registeredSchools.value.filterNot {
            it.schoolId.equals(cleanCode, ignoreCase = true)
        }
        _registeredSchools.value = updatedSchools
        repo.saveRegisteredSchools(updatedSchools)
        _cloudDiscoveryRevision.value += 1
        showToastAlert("تمت إضافة مدرسة ($cleanName) وتفعيل كودها ($cleanCode) بنجاح", isSuccess = true)
    }

    fun unsuspendAndActivateSchoolImmediately(schoolId: String) {
        val cleanId = schoolId.trim()
        if (cleanId.isBlank()) return
        val updatedList = _registeredSchools.value.map { s ->
            if (s.schoolId.equals(cleanId, ignoreCase = true)) {
                s.copy(
                    isActive = true,
                    isSuspended = false,
                    isSchoolOfficiallyActivated = true
                )
            } else s
        }
        _registeredSchools.value = updatedList
        repo.saveRegisteredSchools(updatedList)

        val prevActiveId = repo.getActiveSchoolId()
        val cfg = repo.loadSchoolConfig(cleanId)
        val updatedCfg = cfg.copy(
            schoolId = cleanId,
            isActive = true,
            isSuspended = false,
            isSchoolOfficiallyActivated = true
        )
        repo.saveSchoolConfig(updatedCfg)
        if (prevActiveId.isNotBlank() && prevActiveId != cleanId) {
            repo.setActiveSchoolId(prevActiveId)
        }
        if (_schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)) {
            _schoolConfig.value = _schoolConfig.value.copy(
                isActive = true,
                isSuspended = false,
                isSchoolOfficiallyActivated = true
            )
        }
        _cloudDiscoveryRevision.value += 1
        val targetName = updatedList.firstOrNull { it.schoolId.equals(cleanId, ignoreCase = true) }?.schoolName ?: cleanId
        showToastAlert("تم تفعيل مدرسة ($targetName) وإلغاء التجميد عنها فوراً", isSuccess = true)
    }

    fun setSchoolOfficialActivationRemote(schoolId: String, activated: Boolean) {
        val cleanId = schoolId.trim()
        if (cleanId.isBlank()) return
        val updatedList = _registeredSchools.value.map { s ->
            if (s.schoolId.equals(cleanId, ignoreCase = true)) {
                s.copy(isSchoolOfficiallyActivated = activated)
            } else s
        }
        _registeredSchools.value = updatedList
        repo.saveRegisteredSchools(updatedList)

        val prevActiveId = repo.getActiveSchoolId()
        val cfg = repo.loadSchoolConfig(cleanId)
        val updatedCfg = cfg.copy(
            schoolId = cleanId,
            isSchoolOfficiallyActivated = activated
        )
        repo.saveSchoolConfig(updatedCfg)
        if (prevActiveId.isNotBlank() && prevActiveId != cleanId) {
            repo.setActiveSchoolId(prevActiveId)
        }
        if (_schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)) {
            _schoolConfig.value = _schoolConfig.value.copy(isSchoolOfficiallyActivated = activated)
        }
        _cloudDiscoveryRevision.value += 1
        showToastAlert(
            if (activated) "تم تفعيل بوابة المدرسة ($cleanId) رسمياً"
            else "تم قفل بوابة المدرسة ($cleanId) لمطالبة المدير بكود التفعيل",
            isSuccess = true
        )
    }

    fun buildSchoolActivationShareCardText(school: RegisteredSchoolSummary): String {
        val code = school.schoolActivationCode.ifBlank { school.schoolId }
        val adminUser = school.adminUsername.ifBlank { "${school.schoolId}-admin" }
        val adminPass = school.adminPasswordPlain.ifBlank { "123456" }
        return buildString {
            appendLine("بطاقة تفعيل نظام مدرستي الذكية")
            appendLine("اسم المدرسة: ${school.schoolName}")
            appendLine("المرحلة الدراسية: ${school.schoolStage}")
            appendLine("كود تفعيل البوابة: $code")
            appendLine("حساب المدير: $adminUser")
            appendLine("كلمة السر: $adminPass")
            append("المدير المسؤول: ${school.principalFullName}")
        }
    }

    fun getSchoolDetailedStats(schoolId: String): SchoolDetailedInspectionStats {
        val cleanId = schoolId.trim().ifBlank { "1001" }
        val summary = _registeredSchools.value.firstOrNull {
            it.schoolId.equals(cleanId, ignoreCase = true)
        } ?: RegisteredSchoolSummary(
            schoolId = cleanId,
            schoolName = _schoolConfig.value.schoolName.ifBlank { "مدرستي النموذجية" },
            schoolStage = _schoolConfig.value.schoolStage.arabicLabel,
            schoolActivationCode = cleanId
        )

        val isCurrentActive = _schoolConfig.value.schoolId.equals(cleanId, ignoreCase = true)
        val stuList = if (isCurrentActive && _students.value.isNotEmpty()) {
            _students.value.filter { it.schoolId.isBlank() || it.schoolId.equals(cleanId, ignoreCase = true) }
        } else {
            repo.loadStudents(cleanId)
        }
        val accList = if (isCurrentActive && _accounts.value.isNotEmpty()) {
            _accounts.value.filter { it.schoolId.isBlank() || it.schoolId.equals(cleanId, ignoreCase = true) }
        } else {
            repo.loadAccounts(cleanId)
        }
        val postsList = if (isCurrentActive && _studentActivityPosts.value.isNotEmpty()) {
            _studentActivityPosts.value.filter { it.schoolId.isBlank() || it.schoolId.equals(cleanId, ignoreCase = true) }
        } else {
            repo.loadStudentActivityPosts(cleanId)
        }

        val teachers = accList.filter { it.role == UserRole.TEACHER }
        val principalAcc = accList.firstOrNull { it.role == UserRole.ADMIN }
        val passDisplay = principalAcc?.password?.ifBlank { summary.adminPasswordPlain } ?: summary.adminPasswordPlain.ifBlank { "123456" }

        val sectionsMap = stuList
            .groupBy { "${it.gradeClass} (${it.sectionName})" }
            .mapValues { (_, list) -> list.size }

        val avgAtt = if (stuList.isNotEmpty()) {
            stuList.map { it.attendanceRate }.average().toInt().coerceIn(75, 100)
        } else {
            96
        }

        return SchoolDetailedInspectionStats(
            school = summary,
            students = stuList,
            teachers = teachers,
            principalAccount = principalAcc,
            adminPasswordDisplay = passDisplay,
            attendanceRatePercent = avgAtt,
            sectionsMap = sectionsMap,
            activityPostsCount = postsList.size
        )
    }

    fun getAllDiscoveredSchoolsDetailedStats(): List<SchoolDetailedInspectionStats> {
        val schools = _registeredSchools.value.ifEmpty {
            listOf(
                RegisteredSchoolSummary(
                    schoolId = _schoolConfig.value.schoolId.ifBlank { "1001" },
                    schoolName = _schoolConfig.value.schoolName.ifBlank { "مدرستي النموذجية" },
                    schoolStage = _schoolConfig.value.schoolStage.arabicLabel,
                    isActive = _schoolConfig.value.isActive,
                    isSchoolOfficiallyActivated = _schoolConfig.value.isSchoolOfficiallyActivated,
                    schoolActivationCode = _schoolConfig.value.schoolActivationCode.ifBlank { "1001" },
                    principalFullName = _schoolConfig.value.principalFullName
                )
            )
        }
        return schools.map { getSchoolDetailedStats(it.schoolId) }
    }

    fun sendTargetedDeveloperBroadcast(
        title: String,
        message: String,
        targetAudienceRole: String = "ALL"
    ) {
        val cleanTitle = InputSanitizer.sanitize(title).trim()
        val cleanMsg = InputSanitizer.sanitize(message).trim()
        if (cleanTitle.isBlank() || cleanMsg.isBlank()) {
            showToastAlert("يرجى إدخال عنوان ونص التعميم الإداري أولاً", isSuccess = false)
            return
        }
        val today = SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale("ar")).format(Date())
        val roleNormalized = targetAudienceRole.trim().uppercase().ifBlank { "ALL" }
        val item = DeveloperBroadcastItem(
            id = "dev_bc_${UUID.randomUUID().toString().take(6)}",
            title = cleanTitle,
            message = cleanMsg,
            senderName = "إدارة المنصة التقنية",
            createdAt = today,
            targetSchoolId = "ALL",
            iconKey = OccasionIconKey.BELL.name,
            isActive = true,
            targetRole = roleNormalized
        )
        val updated = listOf(item) + _developerBroadcasts.value
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)
        showToastAlert("تم إرسال التعميم الفوري (${item.targetAudienceArabicLabel}) بنجاح", isSuccess = true)
    }

    fun toggleDeveloperBroadcastActive(broadcastId: String) {
        val updated = _developerBroadcasts.value.map { item ->
            if (item.id == broadcastId) {
                item.copy(isActive = !item.isActive)
            } else item
        }
        _developerBroadcasts.value = updated
        repo.saveDeveloperBroadcasts(updated)
        val target = updated.firstOrNull { it.id == broadcastId }
        showToastAlert(
            if (target?.isActive == true) "تمت إعادة عرض التعميم في المدارس"
            else "تم إيقاف عرض التعميم مؤقتاً",
            isSuccess = true
        )
    }
}


