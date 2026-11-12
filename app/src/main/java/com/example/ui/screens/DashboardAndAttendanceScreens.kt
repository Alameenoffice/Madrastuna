package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DocumentAndIdExporter
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

@Composable
fun DashboardScreen(
    viewModel: SchoolViewModel,
    onOpenNotificationsDialog: () -> Unit
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val tuitions by viewModel.tuitions.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val occasionThemeConfig by viewModel.occasionThemeConfig.collectAsState()
    val developerBroadcasts by viewModel.developerBroadcasts.collectAsState()
    val dismissedBroadcastIds by viewModel.dismissedBroadcastIds.collectAsState()
    val dailyBehaviors by viewModel.dailyBehaviors.collectAsState()
    val dismissalCalls by viewModel.dismissalCalls.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val dismissedUrgentAlertSig by viewModel.dismissedUrgentAlertSignature.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val teacherRatings by viewModel.teacherRatings.collectAsState()
    val topTeacherSummary = remember(accounts, teacherRatings, currentUser) {
        viewModel.getTopRatedTeacherSummary(accounts, teacherRatings, currentUser)
    }
    val topRatedTeachers = remember(accounts, teacherRatings, currentUser) {
        viewModel.getTopRatedTeacherSummaries(limit = 3, accountsList = accounts, ratingsList = teacherRatings, currentUserAccount = currentUser)
    }

    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }
    val todayHolidayLabel = remember(holidaysConfig, todayIso) {
        viewModel.getHolidayLabelForDate(todayIso)
    }

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val isAdmin = currentUser?.role == UserRole.ADMIN

    var isEditingUrgentAlertRequested by remember { mutableStateOf(false) }
    var dashboardBehaviorVisibleCount by remember(selectedParentChildId) { mutableStateOf(3) }
    var showCanteenPosDialog by remember { mutableStateOf(false) }
    var walletPassStudent by remember { mutableStateOf<StudentRecord?>(null) }
    val canManageCanteen = remember(currentUser) { viewModel.canUserManageCanteen(currentUser) }

    // Scoped data based on logged-in user role (and active child for Parent)
    val scopedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser)
    }
    val myStudent = remember(scopedStudents, students, currentUser, selectedParentChildId) {
        if (isParentOrStudent) {
            scopedStudents.firstOrNull() ?: students.firstOrNull { it.id == currentUser?.linkedStudentId }
        } else null
    }

    val scopedNotifications = remember(notifications, currentUser, students) {
        viewModel.getScopedNotificationsForUser(currentUser)
    }
    val unreadNotifications = remember(scopedNotifications) {
        scopedNotifications.count { !it.isRead }
    }

    val activeUrgentAlert = schoolConfig.urgentAlert
    val currentUrgentSig = activeUrgentAlert.signatureKey.ifBlank {
        "${activeUrgentAlert.updatedAt}_${activeUrgentAlert.message.hashCode()}"
    }
    val shouldShowUrgentAlertBanner = activeUrgentAlert.isActive &&
        activeUrgentAlert.message.isNotBlank() &&
        dismissedUrgentAlertSig != currentUrgentSig

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 28.dp)
    ) {
        // Zero-Gap Seamless Full-Bleed Wave Stack:
        // 1) FIRST at the very top: 3D Royal Welcome Wave Hero ("مرحباً بك، ...")
        // 2) SECOND (0.dp gap): Developer System Broadcast + Principal School Announcement
        // 3) THIRD (0.dp gap): Top 3 Auto-Rotating Rated Teachers Wave Spotlight
        item {
            val welcomeSubtitle = when {
                isParentOrStudent && myStudent != null ->
                    "${myStudent.studentTitleLabel}: ${myStudent.studentName} • الصف: ${myStudent.gradeClass} (شعبة ${myStudent.sectionName})"
                isTeacher ->
                    "${currentUser?.honorificTeacherTitle ?: "الأستاذ"} • المواد: ${currentUser?.specialty?.ifBlank { "كادر تدريسي" } ?: ""} • الصفوف: ${currentUser?.formattedAssignedClassesForDisplay ?: ""}"
                else -> {
                    val cleanAddr = ArabicTextNormalizer.cleanDefaultAddress(schoolConfig.address)
                    if (cleanAddr.isNotBlank()) {
                        "${schoolConfig.schoolName} ($cleanAddr) — ${schoolConfig.academicYear}"
                    } else {
                        "${schoolConfig.schoolName} — ${schoolConfig.academicYear}"
                    }
                }
            }
            val welcomeGreetingPrefix = when {
                currentUser?.role == UserRole.STUDENT && myStudent != null ->
                    if (myStudent.isFemale) "مرحباً بكِ" else "مرحباً بك"
                else -> currentUser?.welcomeGreeting ?: "مرحباً بك"
            }
            val roleGenderPillText = when {
                currentUser?.role == UserRole.STUDENT && myStudent != null -> myStudent.studentTitleLabel
                else -> currentUser?.roleGenderTitle ?: currentUser?.role?.arabicTitle ?: "مستخدم"
            }

            val edgeToEdgeWaveModifier = Modifier.layout { measurable, constraints ->
                val bleedPx = 32.dp.roundToPx()
                val fullW = (constraints.maxWidth + bleedPx).coerceAtLeast(constraints.minWidth)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = fullW, maxWidth = fullW)
                )
                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }

            fun Modifier.seamlessWaveOverlap(overlapDp: Dp): Modifier = this.layout { measurable, constraints ->
                val bleedPx = 32.dp.roundToPx()
                val overlapPx = overlapDp.roundToPx()
                val fullW = (constraints.maxWidth + bleedPx).coerceAtLeast(constraints.minWidth)
                val placeable = measurable.measure(
                    constraints.copy(minWidth = fullW, maxWidth = fullW)
                )
                val effectiveHeight = (placeable.height - overlapPx).coerceAtLeast(0)
                layout(placeable.width, effectiveHeight) {
                    placeable.place(0, -overlapPx)
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.Top
            ) {
                // 1) FIRST: Welcome Hero Section ("مرحباً بك، ...") edge-to-edge on top layer with welcoming academic character
                ThreeDHeroHeaderCard(
                    title = "$welcomeGreetingPrefix، ${currentUser?.fullName ?: ""}",
                    subtitle = "$welcomeSubtitle • ${schoolConfig.ministryAffiliation} (${schoolConfig.schoolGenderType})",
                    rightPillText = "${schoolConfig.schoolType.arabicLabel} • ${schoolConfig.schoolStage.arabicLabel}",
                    rightPillIcon = Icons.Filled.School,
                    leftPillText = roleGenderPillText,
                    leftPillIcon = Icons.Filled.VerifiedUser,
                    illustrationType = ThreeDIllustrationType.WELCOME_ACADEMIC_CHARACTER,
                    bottomActionText = if (isAdmin) "إدارة الحسابات والمستخدمين" else null,
                    bottomActionIcon = Icons.Filled.ManageAccounts,
                    bottomActionTestTag = "dashboard_admin_accounts_btn",
                    onBottomActionClick = if (isAdmin) {
                        { viewModel.navigateToSection(AppSection.ADMIN_ACCOUNTS) }
                    } else null,
                    modifier = edgeToEdgeWaveModifier.zIndex(4f)
                )

                // 2) SECOND (Directly below the Emerald Welcome Hero Banner at full width with 0.dp gap):
                //    إشعار النظام العام / تعميم المطور + Principal Announcement
                val visibleAdminBroadcasts = remember(developerBroadcasts, dismissedBroadcastIds) {
                    developerBroadcasts.filterNot { bc ->
                        val key = bc.id.ifBlank { "${bc.title}_${bc.createdAt}" }
                        bc.id in dismissedBroadcastIds || key in dismissedBroadcastIds
                    }
                }
                val latestAdminBroadcast = if (isAdmin) visibleAdminBroadcasts.firstOrNull() else null
                val showOccasionBroadcast = occasionThemeConfig.isActive
                val showAdminDirectBroadcast = latestAdminBroadcast != null &&
                    (!showOccasionBroadcast || latestAdminBroadcast.title != occasionThemeConfig.effectiveTitle)

                if (showOccasionBroadcast) {
                    val devIconKey = occasionThemeConfig.resolvedIconKey
                    DeveloperSystemBroadcastBanner(
                        title = occasionThemeConfig.effectiveTitle,
                        subtitle = occasionThemeConfig.effectiveBannerText,
                        badgeText = "إشعار النظام العام / تعميم المطور • ${devIconKey.arabicLabel}",
                        iconKey = devIconKey,
                        illustrationType = devIconKey.toThreeDIllustrationType(),
                        topOverlapPadding = 22.dp,
                        modifier = Modifier
                            .seamlessWaveOverlap(26.dp)
                            .zIndex(3.2f)
                    )
                }

                if (showAdminDirectBroadcast && latestAdminBroadcast != null) {
                    val bcIconKey = OccasionIconKey.fromKey(latestAdminBroadcast.iconKey)
                    val adminBcOverlap = if (showOccasionBroadcast) 18.dp else 26.dp
                    DeveloperSystemBroadcastBanner(
                        title = latestAdminBroadcast.title,
                        subtitle = "${latestAdminBroadcast.message}  •  (${latestAdminBroadcast.createdAt})",
                        badgeText = "إشعار النظام العام / تعميم المطور",
                        iconKey = bcIconKey,
                        illustrationType = bcIconKey.toThreeDIllustrationType(),
                        topOverlapPadding = adminBcOverlap - 4.dp,
                        onDismiss = {
                            developerBroadcasts.forEach { bc ->
                                val key = bc.id.ifBlank { "${bc.title}_${bc.createdAt}" }
                                viewModel.dismissDeveloperBroadcastForAdmin(key)
                                if (bc.id.isNotBlank()) {
                                    viewModel.dismissDeveloperBroadcastForAdmin(bc.id)
                                }
                            }
                        },
                        modifier = Modifier
                            .seamlessWaveOverlap(adminBcOverlap)
                            .zIndex(3f)
                    )
                }

                val hasDeveloperBannerAbove = showOccasionBroadcast || showAdminDirectBroadcast

                if (shouldShowUrgentAlertBanner || todayHolidayLabel != null) {
                    val schoolBannerTitle = if (shouldShowUrgentAlertBanner) {
                        activeUrgentAlert.authorName.ifBlank { "تبليغ إداري هام" }
                    } else {
                        "عطلة رسمية: $todayHolidayLabel"
                    }
                    val schoolBannerSubtitle = if (shouldShowUrgentAlertBanner) {
                        activeUrgentAlert.message
                    } else {
                        "اليوم عطلة رسمية معتمدة في التقويم المدرسي (${schoolConfig.academicYear})"
                    }
                    val overlapWithAbove = if (hasDeveloperBannerAbove) 18.dp else 26.dp

                    ThreeDMegaphoneAnnouncementBanner(
                        title = schoolBannerTitle,
                        subtitle = schoolBannerSubtitle,
                        topOverlapPadding = overlapWithAbove - 4.dp,
                        modifier = Modifier
                            .seamlessWaveOverlap(overlapWithAbove)
                            .zIndex(2f),
                        onClick = when {
                            isAdmin && todayHolidayLabel != null && !shouldShowUrgentAlertBanner -> {
                                { viewModel.openHolidaysManagementModal() }
                            }
                            isAdmin -> {
                                { viewModel.openUrgentSchoolAlertModal() }
                            }
                            else -> null
                        },
                        isUrgentActive = shouldShowUrgentAlertBanner,
                        isAdmin = isAdmin,
                        onEditUrgent = { viewModel.openUrgentSchoolAlertModal() },
                        onStopUrgent = { viewModel.deleteOrStopUrgentSchoolAlert() },
                        onDismissUrgent = { viewModel.dismissUrgentSchoolAlertLocally() }
                    )
                }

                // 3) THIRD (Tucked seamlessly under Announcements/Welcome with 0.dp gap): Top 3 Auto-Rotating Rated Teachers Wave Spotlight
                if (featureSettings.showTopTeacherOnHome && topRatedTeachers.isNotEmpty() && topTeacherSummary != null) {
                    val hasBannerAbove = hasDeveloperBannerAbove || shouldShowUrgentAlertBanner || todayHolidayLabel != null
                    val spotlightOverlap = if (hasBannerAbove) 18.dp else 26.dp
                    SeamlessWaveTeacherSpotlightSection(
                        teacherName = topTeacherSummary.teacher.fullName,
                        teacherSubjects = topTeacherSummary.teacher.specialty,
                        averageRating = topTeacherSummary.averageRating,
                        totalRatingsCount = topTeacherSummary.totalRatingsCount,
                        topTeachers = topRatedTeachers,
                        isAdmin = isAdmin,
                        topOverlapPadding = spotlightOverlap - 4.dp,
                        onOpenEvaluationSection = {
                            viewModel.navigateToSection(AppSection.TEACHER_EVALUATION)
                        },
                        onHideFromHome = {
                            viewModel.toggleShowTopTeacherOnHome(false)
                        },
                        modifier = Modifier
                            .seamlessWaveOverlap(spotlightOverlap)
                            .zIndex(1f)
                    )
                }
            }
        }

        // Strict Privacy Isolation vs Admin/Teacher Stat Cards
        item {
            if (isParentOrStudent && myStudent != null) {
                val issuedReportCards by viewModel.issuedReportCards.collectAsState()
                val activeCelebrationCard by viewModel.activeCelebrationReportCard.collectAsState()
                val myLatestReportCard = remember(issuedReportCards, myStudent.id) {
                    viewModel.getLatestHomeBannerReportCardForStudent(myStudent.id)
                }

                activeCelebrationCard?.let { cardToView ->
                    val stuGrades = grades.filter { it.studentId == cardToView.studentId }
                    LuxuryInteractiveReportCardModal(
                        reportCard = cardToView,
                        schoolConfig = schoolConfig,
                        onDismiss = { viewModel.closeCelebrationReportCardModal() },
                        onPrintPdf = {
                            val res = DocumentAndIdExporter.exportStudentReportCardPdf(
                                context = context,
                                student = myStudent,
                                grades = stuGrades,
                                schoolConfig = schoolConfig,
                                principalName = cardToView.principalName,
                                periodLabel = cardToView.periodType.arabicTitle
                            )
                            viewModel.showBannerMessage(res)
                        }
                    )
                }

                if (myLatestReportCard != null) {
                    ParentIssuedReportCardArrivalBanner(
                        reportCard = myLatestReportCard,
                        onClick = { viewModel.openCelebrationReportCardModal(myLatestReportCard) },
                        onDismissFromHome = { viewModel.dismissIssuedReportCardFromHome(myLatestReportCard.id) },
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                // PRIVATE STUDENT-ONLY DASHBOARD STATS (Strictly NO general school stats!)
                val myTodayStatus = if (todayHolidayLabel != null) {
                    AttendanceStatus.HOLIDAY
                } else {
                    viewModel.getStudentStatusForDate(myStudent, todayIso)
                }
                val myTotalAbsences = viewModel.getTotalAbsenceDays(myStudent.id)
                val myTotalPresent = viewModel.getTotalPresentDays(myStudent.id)
                val myClassHomeworks = homeworks.filter {
                    it.gradeClass == myStudent.gradeClass && it.sectionName == myStudent.sectionName
                }
                val myGrades = grades.filter { it.studentId == myStudent.id }
                val myAvg = if (myGrades.isEmpty()) 0 else myGrades.map { it.finalAverage }.average().toInt()
                val myReportsCount = reports.count { it.studentId == myStudent.id }
                val myTuition = tuitions.firstOrNull { it.studentId == myStudent.id }
                val myBadges = remember(attendance, grades, myStudent.id) {
                    viewModel.getStudentAutoBadges(myStudent.id)
                }
                val allMyBehaviors = remember(dailyBehaviors, myStudent.id) {
                    dailyBehaviors.filter { it.studentId == myStudent.id }
                }
                val myBehaviors = remember(allMyBehaviors, dashboardBehaviorVisibleCount) {
                    allMyBehaviors.take(dashboardBehaviorVisibleCount)
                }

                // Live School Bus Departure/Arrival Alert Banner at Top of Dashboard for Subscribed Student & Parent
                if (myStudent.isBusSubscribed && (featureSettings.busTrackingEnabled || schoolConfig.schoolType == SchoolType.PRIVATE)) {
                    ParentStudentLiveBusAlertBannerCard(
                        studentName = myStudent.fullNameWithFather,
                        routeName = myStudent.busRouteName,
                        activeBusEventName = schoolConfig.activeBusEvent,
                        activeBusTimestamp = schoolConfig.activeBusEventTimestamp,
                        onClick = { viewModel.navigateToSection(AppSection.SCHOOL_TRANSPORT) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                // Live Smart Gate Status Card for Parent & Student (Check-In / Check-Out timestamp)
                val todayGateLog = myStudent.getGateLogForDate(todayIso)
                if (todayGateLog != null && (todayGateLog.checkInTime.isNotBlank() || todayGateLog.checkOutTime.isNotBlank())) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("dashboard_student_gate_status_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = AdaptiveSoftSurface,
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = DeepEmerald
                            ) {
                                Icon(
                                    imageVector = if (todayGateLog.isDeparted) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (todayGateLog.isDeparted) {
                                        "غادر المدرسة الساعة ${todayGateLog.checkOutTime} (وقت الدخول: ${todayGateLog.checkInTime.ifBlank { "—" }})"
                                    } else {
                                        "متواجد داخل المدرسة منذ الساعة ${todayGateLog.checkInTime}"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (todayGateLog.isDeparted) {
                                        "حركة البوابة الذكية اليوم ($todayIso): ${todayGateLog.departureNote.ifBlank { "انصراف رسمي من البوابة" }}"
                                    } else {
                                        "حركة البوابة الذكية اليوم ($todayIso): تم تسجيل الدخول بأمان عبر بوابة المدرسة"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // Smart 1-Tap Dismissal Call for Parents ("أنا أمام المدرسة لاستلام ابني") with Holiday Lock & 3D Car Bezier Radar
                if (currentUser?.role == UserRole.PARENT && featureSettings.dismissalCallEnabled) {
                    val activeCall = dismissalCalls.firstOrNull { it.studentId == myStudent.id && !it.isAcknowledged }
                    val isHolidayToday = todayHolidayLabel != null || myTodayStatus == AttendanceStatus.HOLIDAY
                    InteractiveParentDismissalPickupCard(
                        student = myStudent,
                        activeCall = activeCall,
                        isHoliday = isHolidayToday,
                        holidayLabel = todayHolidayLabel ?: "عطلة رسمية",
                        onTriggerCall = { viewModel.triggerParentDismissalCall(myStudent) },
                        onHolidayLockedClick = {
                            viewModel.showToastAlert(
                                "الاستدعاء السريع يعمل فقط خلال أيام وأوقات الدوام الرسمي — اليوم عطلة رسمية",
                                isSuccess = false
                            )
                        },
                        buttonTestTag = "dashboard_parent_dismissal_call_btn",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                // Recent Daily Follow-Up & Behavior Log for Student/Parent (Compact top 3 + Full Log Modal)
                if (featureSettings.dailyBehaviorEnabled && allMyBehaviors.isNotEmpty()) {
                    CompactDailyBehaviorCard(
                        title = "دفتر المتابعة اليومي والسلوك (${myStudent.studentName}):",
                        studentName = myStudent.fullNameWithFather,
                        behaviors = allMyBehaviors,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = "حالة الحضور اليوم",
                            value = if (todayHolidayLabel != null) "عطلة رسمية" else myTodayStatus.arabicLabel,
                            subtitle = if (todayHolidayLabel != null) todayHolidayLabel else "حضور فعلي: $myTotalPresent يوم • غياب: $myTotalAbsences يوم",
                            icon = Icons.Filled.FactCheck,
                            accentColor = when {
                                todayHolidayLabel != null || myTodayStatus == AttendanceStatus.HOLIDAY -> GoldAccent
                                myTodayStatus == AttendanceStatus.ABSENT -> DangerRed
                                myTodayStatus == AttendanceStatus.UNRECORDED -> Color(0xFF64748B)
                                else -> PrimaryLight
                            },
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.ATTENDANCE) }
                        )
                        SmartStatCard(
                            title = "تحضير وواجبات صفه",
                            value = "${myClassHomeworks.size} واجب",
                            subtitle = "${myStudent.gradeClass} (${myStudent.sectionName})",
                            icon = Icons.Filled.MenuBook,
                            accentColor = SecondaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.SCHEDULE_HOMEWORK) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = "معدل درجات الطالب",
                            value = if (myGrades.isEmpty()) "قيد الرصد" else "$myAvg%",
                            subtitle = "${myGrades.size} مواد مرصودة",
                            icon = Icons.Filled.Assessment,
                            accentColor = InfoBlue,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.EXAMS_GRADES) }
                        )
                        SmartStatCard(
                            title = "البلاغات الخاصة",
                            value = "$myReportsCount تبليغ",
                            subtitle = "مراسلة الإدارة المباشرة",
                            icon = Icons.Filled.Forum,
                            accentColor = GoldAccent,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.REPORTS_CHAT) }
                        )
                    }

                    if (schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.tuitionEnabled && myTuition != null) {
                        SmartStatCard(
                            title = "الأقساط الدراسية الخاصة بالطالب (${myStudent.studentName})",
                            value = "الواصل: ${myTuition.paidAmount} د.ع",
                            subtitle = "المتبقي: ${myTuition.remainingAmount} د.ع • الاستحقاق القادم: ${myTuition.nextDueDate}",
                            icon = Icons.Filled.Payments,
                            accentColor = Color(0xFF0D9488),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.navigateToSection(AppSection.TUITION_FEES) }
                        )
                    }

                    // Smart Canteen E-Wallet Card for Parent & Student
                    if (schoolConfig.isCanteenWalletEnabled && featureSettings.canteenWalletEnabled) {
                        ParentStudentCanteenWalletCard(
                            student = myStudent,
                            todayIso = todayIso,
                            isParentRole = currentUser?.role == UserRole.PARENT,
                            onUpdateDailyLimit = { newLimit, onComplete ->
                                viewModel.updateStudentDailySpendLimit(myStudent.id, newLimit)
                                onComplete(true, "تم حفظ السقف اليومي بنجاح")
                            },
                            onOpenWalletBarcodePass = {
                                walletPassStudent = myStudent
                            }
                        )
                    }
                }
            } else {
                // ADMIN OR SCOPED TEACHER DASHBOARD STATS
                val absentCount = scopedStudents.count {
                    viewModel.getStudentStatusForDate(it, todayIso) == AttendanceStatus.ABSENT
                }
                val excusedCount = scopedStudents.count {
                    viewModel.getStudentStatusForDate(it, todayIso) == AttendanceStatus.EXCUSED
                }
                val lateCount = scopedStudents.count {
                    viewModel.getStudentStatusForDate(it, todayIso) == AttendanceStatus.LATE
                }
                val presentCount = scopedStudents.count {
                    viewModel.getStudentStatusForDate(it, todayIso) == AttendanceStatus.PRESENT
                }
                val unrecordedCount = scopedStudents.count {
                    viewModel.getStudentStatusForDate(it, todayIso) == AttendanceStatus.UNRECORDED
                }
                val scopedHomeworks = remember(homeworks, currentUser, isTeacher) {
                    if (isTeacher && currentUser != null) {
                        homeworks.filter { hw ->
                            currentUser!!.canTeacherAccessClassSection(hw.gradeClass, hw.sectionName) &&
                                currentUser!!.canTeacherAccessSubject(hw.subjectName)
                        }
                    } else {
                        homeworks
                    }
                }
                val honorCount = if (isTeacher) {
                    scopedStudents.count { it.isOnHonorRoll }
                } else {
                    students.count { it.isOnHonorRoll }
                }
                val teachersCount = accounts.count { it.role == UserRole.TEACHER }
                val staffPluralLabel = schoolConfig.schoolStage.staffTitlePlural

                val checkedInToday = scopedStudents.count { stu ->
                    stu.getGateLogForDate(todayIso)?.checkInTime?.isNotBlank() == true
                }
                val checkedOutToday = scopedStudents.count { stu ->
                    stu.getGateLogForDate(todayIso)?.isDeparted == true
                }
                val insideSchoolNow = scopedStudents.count { stu ->
                    val log = stu.getGateLogForDate(todayIso)
                    log != null && log.checkInTime.isNotBlank() && log.checkOutTime.isBlank()
                }

                SectionDividerHeaderWithLines(
                    title = if (isTeacher) "إحصائيات صفوفك وطلابك المسندين" else "لوحة الإحصائيات العامة للمدرسة"
                )
                Spacer(modifier = Modifier.height(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // 3D Daily Attendance & Gate Summary Banner Card (موجز الدوام وحركة البوابة اليومية)
                    ThreeDDailyAttendanceGateSummaryCard(
                        title = "موجز الدوام وحركة البوابة اليومية",
                        subtitle = "متابعة حضور وغياب وإجازات كل صف وحركة الدخول والانصراف في المدرسة",
                        buttonText = "عرض التفاصيل",
                        presentCount = presentCount,
                        absentCount = absentCount,
                        checkedInCount = checkedInToday,
                        checkedOutCount = checkedOutToday,
                        insideNowCount = insideSchoolNow,
                        onClick = { viewModel.navigateToSection(AppSection.LIVE_OPERATIONS_RADAR) },
                        testTag = "dashboard_open_live_operations_radar_card"
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val realTeacherSupervisedCount = remember(students, currentUser, schoolConfig.schoolId) {
                            val activeTeacher = currentUser
                            if (isTeacher && activeTeacher != null) viewModel.getTeacherSupervisedStudentsCount(activeTeacher) else scopedStudents.size
                        }
                        SmartStatCard(
                            title = if (isTeacher) "الطلاب المشرف عليهم" else "إجمالي الطلبة",
                            value = "$realTeacherSupervisedCount طالب",
                            subtitle = if (isTeacher) "العدد الفعلي المسجل في صفوفك وشعبك" else "$staffPluralLabel: $teachersCount",
                            icon = Icons.Filled.Groups,
                            accentColor = PrimaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (isAdmin) viewModel.navigateToSection(AppSection.STUDENTS_DIRECTORY)
                                else viewModel.navigateToSection(AppSection.ATTENDANCE)
                            }
                        )
                        SmartStatCard(
                            title = "سجل الحضور اليومي",
                            value = when {
                                todayHolidayLabel != null -> "عطلة رسمية"
                                presentCount == 0 && unrecordedCount == scopedStudents.size && scopedStudents.isNotEmpty() -> "بانتظار الرصد"
                                else -> "$presentCount حاضر"
                            },
                            subtitle = if (todayHolidayLabel != null) {
                                todayHolidayLabel
                            } else {
                                "الغياب: $absentCount • مجاز: $excusedCount • بانتظار الرصد: $unrecordedCount"
                            },
                            icon = Icons.Filled.FactCheck,
                            accentColor = if (todayHolidayLabel != null) GoldAccent else InfoBlue,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.ATTENDANCE) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = "الواجبات والتحضير",
                            value = "${scopedHomeworks.size} واجب",
                            subtitle = if (isTeacher) "ضمن موادك وصفوفك المسندة" else "محدث للصفوف المسجلة",
                            icon = Icons.Filled.MenuBook,
                            accentColor = SecondaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.SCHEDULE_HOMEWORK) }
                        )
                        if (featureSettings.honorRollEnabled) {
                            SmartStatCard(
                                title = "لوحة الشرف للمتفوقين",
                                value = "$honorCount متفوق",
                                subtitle = if (isTeacher) "ضمن طلابك المسندين" else "أوائل الصفوف",
                                icon = Icons.Filled.EmojiEvents,
                                accentColor = Color(0xFF059669),
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.navigateToSection(AppSection.HONOR_ROLL) }
                            )
                        }
                    }

                    if (isAdmin && schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.tuitionEnabled) {
                        val totalPaid = tuitions.sumOf { it.paidAmount }
                        val totalRemaining = tuitions.sumOf { it.remainingAmount }
                        SmartStatCard(
                            title = "ملخص نظام الأقساط الدراسية (مدرسة أهلية)",
                            value = "الواصل: $totalPaid د.ع",
                            subtitle = "المتبقي الكلي: $totalRemaining د.ع — اضغط لإدارة الدفعات والوصولات",
                            icon = Icons.Filled.Payments,
                            accentColor = Color(0xFF0D9488),
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { viewModel.navigateToSection(AppSection.TUITION_FEES) }
                        )
                    }

                    // School Bus & Transport Executive Telemetry Summary Card
                    val busSubscribersCount = students.count { it.isBusSubscribed && it.isActive }
                    val isParentOrStudentBusSubscribed = (!isAdmin && !isTeacher) && scopedStudents.any { it.isBusSubscribed }
                    if ((featureSettings.busTrackingEnabled || schoolConfig.schoolType == SchoolType.PRIVATE) &&
                        (isAdmin || isTeacher || isParentOrStudentBusSubscribed)
                    ) {
                        SchoolBusQuickAlertsCard(
                            busSubscribersCount = busSubscribersCount,
                            activeBusEventName = schoolConfig.activeBusEvent,
                            activeBusEventTimestamp = schoolConfig.activeBusEventTimestamp,
                            canControlTrip = isAdmin || isTeacher,
                            onSendBusAlert = { eventType -> viewModel.sendBusTrackingAlert(eventType) },
                            onOpenTransportSection = { viewModel.navigateToSection(AppSection.SCHOOL_TRANSPORT) }
                        )
                    }

                    // Smart Canteen POS & Wallet Management Card (for Principal and Authorized Teacher/Cashier)
                    if (canManageCanteen) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCanteenPosDialog = true }
                                .testTag("dashboard_open_canteen_pos_card"),
                            shape = RoundedCornerShape(18.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                DeepEmerald,
                                                RoyalGreen
                                            )
                                        )
                                    )
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color.White.copy(alpha = 0.18f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Storefront,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Column {
                                            Text(
                                                text = "نظام كاشير ومحفظة الحانوت الذكية",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "نقطة بيع الباركود (POS) وشحن أرصدة الطلاب سحابياً",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White.copy(alpha = 0.92f)
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White,
                                        shadowElevation = 2.dp
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.QrCodeScanner,
                                                contentDescription = null,
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "فتح الكاشير",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepEmerald,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // All Sections 3D Wave Quick Access Grid
        item {
            SectionDividerHeaderWithLines(
                title = "الأقسام التعليمية والإدارية",
                modifier = Modifier.padding(top = 4.dp, bottom = 6.dp)
            )

            val sectionsList = remember(schoolConfig.schoolType, featureSettings, currentUser) {
                buildList {
                    if (isParentOrStudent) {
                        add(Triple(AppSection.STUDENT_PERSONAL_PROFILE, Icons.Filled.Badge, "ملف الطالب الشخصي الشامل والمواظبة والدرجات"))
                    }
                    if (isAdmin) {
                        add(Triple(AppSection.TEACHING_STAFF, Icons.Filled.Groups, "سجل الكادر التدريسي المدمج ومؤشرات الإشراف"))
                        add(Triple(AppSection.STUDENTS_DIRECTORY, Icons.Filled.People, "سجل وأسماء الطلاب حسب الصف والشعبة والنقل الفوري"))
                    }
                    add(Triple(AppSection.ATTENDANCE, Icons.Filled.HowToReg, if (isParentOrStudent) "حالة الحضور وعداد الغياب الخاص" else "رصد الحضور والغياب وتبليغ واتساب"))
                    add(Triple(AppSection.SCHEDULE_HOMEWORK, Icons.Filled.CalendarMonth, "جدول الحصص والتحضير اليومي"))
                    add(Triple(AppSection.CURRICULUM_BOOKS, Icons.Filled.AutoStories, "المناهج العراقية الرسمية وقارئ الكتب الفائق"))
                    add(Triple(AppSection.EXAMS_GRADES, Icons.Filled.Assessment, if (isParentOrStudent) "جدول كشف درجات الطالب التفصيلي" else "الامتحانات ورصد الدرجات حسب المادة"))
                    if (featureSettings.honorRollEnabled) {
                        add(Triple(AppSection.HONOR_ROLL, Icons.Filled.WorkspacePremium, "لوحة الشرف للمتفوقين (عام)"))
                    }
                    add(Triple(AppSection.TEACHER_EVALUATION, Icons.Filled.Star, "تقييم وتميّز الكادر التدريسي الموثق"))
                    if (schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.tuitionEnabled && !isTeacher) {
                        add(Triple(AppSection.TUITION_FEES, Icons.Filled.AccountBalanceWallet, if (isParentOrStudent) "أقساط الطالب ووصولات الدفع" else "الأقساط الدراسية ووصولات الدفع"))
                    }
                    add(Triple(AppSection.REPORTS_CHAT, Icons.Filled.Forum, if (isParentOrStudent) "بلاغات الطالب ومراسلة الإدارة" else "البلاغات الرسمية والمراسلة المباشرة"))
                    if (featureSettings.studentActivitiesEnabled) {
                        add(Triple(AppSection.STUDENT_ACTIVITIES, StudentActivitiesLinearVector, "مجتمع ونشاطات الطلبة والإبداعات العلمية"))
                    }
                    if (featureSettings.eventsGalleryEnabled) {
                        add(Triple(AppSection.EVENTS_NEWS, Icons.Filled.Newspaper, "معرض الفعاليات والأخبار (عام)"))
                    }
                    add(Triple(AppSection.STUDENT_COMPREHENSIVE_ARCHIVE, Icons.Filled.FolderSpecial, "الأرشيف الشهري والسنوي الموثق"))
                    if (isAdmin) {
                        add(Triple(AppSection.ADMIN_ACCOUNTS, Icons.Filled.ManageAccounts, "إدارة وتفعيل حسابات المستخدمين"))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sectionsList.chunked(2).forEachIndexed { rowIdx, pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEachIndexed { colIdx, (sec, icon, desc) ->
                            val displayTitle = if (sec == AppSection.STUDENT_COMPREHENSIVE_ARCHIVE) {
                                "السجل الأكاديمي الشامل"
                            } else {
                                sec.arabicTitle
                            }
                            SectionWaveQuickCard3D(
                                title = displayTitle,
                                subtitle = desc,
                                icon = icon,
                                isBlueAccent = (rowIdx + colIdx) % 2 == 1,
                                onClick = { viewModel.navigateToSection(sec) },
                                modifier = Modifier.weight(1f),
                                testTag = "section_card_${sec.name}"
                            )
                        }
                        if (pair.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Scoped Instant Notification Center Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = null,
                                tint = WarningAmber
                            )
                            Text(
                                text = "مركز الإشعارات الفورية",
                                style = MaterialTheme.typography.titleMedium
                            )
                            if (unreadNotifications > 0) {
                                NoWrapBadge(
                                    text = "$unreadNotifications جديد",
                                    icon = Icons.Filled.NewReleases,
                                    containerColor = DangerRed,
                                    contentColor = Color.White
                                )
                            }
                        }

                        TextButton(onClick = onOpenNotificationsDialog) {
                            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("السجل الكامل", maxLines = 1, softWrap = false)
                        }
                    }

                    scopedNotifications.take(3).forEach { notif ->
                        val cleanNotifTitle = com.example.util.InputSanitizer.stripEmojis(notif.title)
                        val cleanNotifBody = com.example.util.InputSanitizer.stripEmojis(notif.body)
                        val isSecretSuggestion = cleanNotifTitle.contains("صندوق المقترحات") || cleanNotifTitle.contains("سرية")
                        Surface(
                            color = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clickable { viewModel.openNotificationDeepLink(notif) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSecretSuggestion) {
                                        Icons.Filled.Lock
                                    } else {
                                        when (notif.type) {
                                            NotificationType.ATTENDANCE -> Icons.Filled.EventBusy
                                            NotificationType.HOMEWORK -> Icons.Filled.MenuBook
                                            NotificationType.GRADES -> Icons.Filled.Grade
                                            NotificationType.TUITION -> Icons.Filled.Payments
                                            NotificationType.REPORT -> Icons.Filled.Campaign
                                            NotificationType.GENERAL -> Icons.Filled.Notifications
                                        }
                                    },
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cleanNotifTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = notif.createdAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = cleanNotifBody,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteNotification(notif.id) },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .testTag("delete_dash_notif_${notif.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "حذف الإشعار",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCanteenPosDialog) {
        val activeSchoolStudents = remember(students, schoolConfig.schoolId) {
            students.filter { it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId }
        }
        SmartCanteenPosAndWalletDialog(
            students = activeSchoolStudents,
            todayIso = todayIso,
            onTopUpWallet = { stuId, amountIqd, note, onComplete ->
                viewModel.topUpStudentWallet(stuId, amountIqd, note, onComplete)
            },
            onProcessPurchase = { stuId, amountIqd, itemSummary, onComplete ->
                viewModel.processCanteenPurchase(stuId, amountIqd, itemSummary) { ok, msg, _ ->
                    onComplete(ok, msg)
                }
            },
            onFindStudentByQr = { rawQr ->
                viewModel.findStudentByWalletOrQrCode(rawQr)
            },
            isCanteenWalletEnabled = schoolConfig.isCanteenWalletEnabled && featureSettings.canteenWalletEnabled,
            isAdminRole = isAdmin,
            onToggleSchoolCanteenWallet = { enabled ->
                viewModel.toggleSchoolCanteenWalletEnabled(enabled)
            },
            onToggleStudentWalletSuspended = { stuId, suspended ->
                viewModel.toggleStudentWalletSuspended(stuId, suspended)
            },
            toastAlert = toastAlert,
            onDismiss = { showCanteenPosDialog = false }
        )
    }

    walletPassStudent?.let { stu ->
        val stuBadges = viewModel.getStudentAutoBadges(stu.id)
        val stuTodayLabel = viewModel.getStudentStatusForDate(stu, todayIso).arabicLabel
        DigitalStudentIdDialog(
            student = stu,
            schoolConfig = schoolConfig,
            badges = stuBadges,
            todayAttendanceLabel = stuTodayLabel,
            todayIso = todayIso,
            initialWalletMode = true,
            toastAlert = toastAlert,
            onSavePng = {
                val res = DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم حفظ الهوية المدرسية بدقة عالية (PNG) في المعرض",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر حفظ الصورة: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم تصدير الهوية المدرسية بصيغة PDF بنجاح",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر تصدير PDF: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { walletPassStudent = null }
        )
    }
}

@Composable
private fun SmartStatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isBlueTheme = title.contains("الحضور") || accentColor == InfoBlue
    WaveStatCard3D(
        title = title,
        value = value,
        subtitle = subtitle,
        icon = icon,
        accentColor = accentColor,
        isBlueTheme = isBlueTheme,
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Section 2: Daily Attendance Screen (سجل الحضور والغياب اليومي)
 * - Strictly isolates Parent/Student so they see ONLY their own student's attendance & total absence days.
 * - Filters Teacher to ONLY their assigned classes & sections.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DailyAttendanceScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val dailyBehaviors by viewModel.dailyBehaviors.collectAsState()
    val dismissalCalls by viewModel.dismissalCalls.collectAsState()
    val lostAndFoundItems by viewModel.lostAndFoundItems.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val allowedClasses = remember(schoolConfig, currentUser, students, selectedParentChildId) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    val vmSelectedClass by viewModel.selectedAttendanceClass.collectAsState()
    val selectedClass = if (vmSelectedClass.isNotBlank() && vmSelectedClass in allowedClasses) {
        vmSelectedClass
    } else {
        allowedClasses.firstOrNull() ?: "الأول المتوسط"
    }

    val allowedSections = remember(schoolConfig, currentUser, students, selectedClass, selectedParentChildId) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    val vmSelectedSection by viewModel.selectedAttendanceSection.collectAsState()
    val selectedSection = if (vmSelectedSection.isNotBlank() && vmSelectedSection in allowedSections) {
        vmSelectedSection
    } else {
        allowedSections.firstOrNull() ?: "أ"
    }

    LaunchedEffect(allowedClasses, vmSelectedClass) {
        if ((vmSelectedClass.isBlank() || vmSelectedClass !in allowedClasses) && allowedClasses.isNotEmpty()) {
            viewModel.setSelectedAttendanceClass(allowedClasses.first())
        }
    }
    LaunchedEffect(allowedSections, vmSelectedSection) {
        if ((vmSelectedSection.isBlank() || vmSelectedSection !in allowedSections) && allowedSections.isNotEmpty()) {
            viewModel.setSelectedAttendanceSection(allowedSections.first())
        }
    }

    val selectedDate by viewModel.selectedAttendanceDate.collectAsState()
    val selectedDateHolidayLabel = remember(holidaysConfig, selectedDate) {
        viewModel.getHolidayLabelForDate(selectedDate)
    }
    var searchName by remember { mutableStateOf("") }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var gateScannerInitialMode by remember { mutableStateOf(GateScanMode.CHECK_IN) }
    var showLeaveRequestDialog by remember { mutableStateOf(false) }
    var showLostAndFoundDialog by remember { mutableStateOf(false) }
    var selectedStudentForIdCard by remember { mutableStateOf<StudentRecord?>(null) }
    var attendanceBehaviorVisibleCount by remember(selectedParentChildId) { mutableStateOf(3) }
    var studentLeaveRequestsVisibleCount by remember(selectedParentChildId) { mutableStateOf(3) }
    var adminLeaveRequestsVisibleCount by remember { mutableStateOf(3) }
    var studentAttendanceRecordsVisibleCount by remember(selectedParentChildId) { mutableStateOf(6) }
    var teacherStudentsVisibleCount by remember(selectedClass, selectedSection, searchName) { mutableStateOf(6) }
    var selectedParentAttendanceSubTab by remember(selectedParentChildId) { mutableIntStateOf(0) }
    var selectedAttendanceSubTab by remember { mutableIntStateOf(0) }

    // Smart Auto-Absence Countdown Ticker & Automatic Trigger after 2 Hours (or configured delay) from First Attendance
    val firstAttendanceTs = remember(attendance, students, selectedDate, holidaysConfig) {
        viewModel.getFirstAttendanceTimestamp(selectedDate)
    }
    var autoAbsenceNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(firstAttendanceTs, featureSettings.autoAbsenceEnabled, featureSettings.autoAbsenceDelayMinutes, selectedDate, selectedDateHolidayLabel) {
        if (featureSettings.autoAbsenceEnabled && selectedDateHolidayLabel == null && firstAttendanceTs > 0L) {
            while (true) {
                autoAbsenceNowMs = System.currentTimeMillis()
                viewModel.checkAndRunAutoAbsenceForDate(selectedDate, forceNow = false)
                kotlinx.coroutines.delay(15_000L)
            }
        }
    }

    LaunchedEffect(featureSettings) {
        if (!featureSettings.qrAttendanceEnabled) {
            showQrScannerDialog = false
            selectedStudentForIdCard = null
        }
        if (!featureSettings.leaveRequestsEnabled) {
            showLeaveRequestDialog = false
        }
        if (!featureSettings.lostAndFoundEnabled) {
            showLostAndFoundDialog = false
        }
    }

    val allTeacherAssignedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser)
    }

    val scopedStudents = remember(allTeacherAssignedStudents, isParentOrStudent, selectedClass, selectedSection, searchName) {
        if (isParentOrStudent) {
            allTeacherAssignedStudents
        } else {
            allTeacherAssignedStudents.filter { stu ->
                ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                    ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection) &&
                    (searchName.isBlank() ||
                        stu.studentName.contains(searchName, ignoreCase = true) ||
                        stu.fullNameWithFather.contains(searchName, ignoreCase = true))
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // 3D Seamless Wave Hero Header for Daily Attendance Section
        item {
            ThreeDHeroHeaderCard(
                title = "الحضور والغياب والعطل الرسمية",
                subtitle = "رصد المواظبة اليومية، حركة البوابة بالباركود، وطلبات الإجازات الإلكترونية",
                rightPillText = selectedDate,
                rightPillIcon = Icons.Filled.Today,
                leftPillText = if (selectedDateHolidayLabel != null) "عطلة رسمية" else "دوام رسمي",
                leftPillIcon = if (selectedDateHolidayLabel != null) Icons.Filled.EventAvailable else Icons.Filled.HowToReg,
                illustrationType = ThreeDIllustrationType.CALENDAR_HOLIDAYS
            )
        }

        if (isParentOrStudent) {
            // Strict Privacy View for Parent & Student: ONLY their own student's attendance & leave requests!
            val myStu = scopedStudents.firstOrNull()
            if (myStu != null) {
                val myTotalAbsences = viewModel.getTotalAbsenceDays(myStu.id)
                val baseList = attendance.filter { it.studentId == myStu.id }
                val existingDates = baseList.map {
                    ArabicTextNormalizer.normalizeDigitsToAscii(it.date.trim())
                }.toSet()
                val extraFromDailyMap = myStu.dailyAttendance.entries.mapNotNull { (rawDate, rawSt) ->
                    val cleanDate = ArabicTextNormalizer.normalizeDigitsToAscii(rawDate.trim())
                    if (cleanDate.isBlank() || cleanDate in existingDates) {
                        null
                    } else {
                        val parsedSt = runCatching { AttendanceStatus.valueOf(rawSt.trim()) }.getOrNull()
                            ?: return@mapNotNull null
                        AttendanceEntry(
                            id = "ATT_DAILY_${myStu.id}_$cleanDate",
                            studentId = myStu.id,
                            studentName = myStu.fullNameWithFather,
                            gradeClass = myStu.gradeClass,
                            sectionName = myStu.sectionName,
                            date = cleanDate,
                            status = parsedSt,
                            schoolId = myStu.schoolId
                        )
                    }
                }
                val rawMyRecords = (baseList + extraFromDailyMap).sortedByDescending { it.date }
                val cleanSel = ArabicTextNormalizer.normalizeDigitsToAscii(selectedDate.trim())
                val myRecords = if (selectedDateHolidayLabel != null && rawMyRecords.none {
                        ArabicTextNormalizer.normalizeDigitsToAscii(it.date.trim()) == cleanSel
                    }
                ) {
                    listOf(
                        AttendanceEntry(
                            id = "ATT_HOLIDAY_${myStu.id}_$cleanSel",
                            studentId = myStu.id,
                            studentName = myStu.fullNameWithFather,
                            gradeClass = myStu.gradeClass,
                            sectionName = myStu.sectionName,
                            date = cleanSel,
                            status = AttendanceStatus.HOLIDAY,
                            note = "يوم عطلة رسمية ($selectedDateHolidayLabel) — موثق في السجل"
                        )
                    ) + rawMyRecords
                } else {
                    rawMyRecords
                }
                val todayStatus = viewModel.getStudentAttendanceStatusForDate(myStu.id, selectedDate)
                val myLeaveRequests = leaveRequests.filter { it.studentId == myStu.id }
                val myBadges = viewModel.getStudentAutoBadges(myStu.id)
                val myBehaviors = dailyBehaviors.filter { it.studentId == myStu.id }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                StudentAvatarWithOverlayBadge(
                                    base64Data = myStu.photoBase64,
                                    contentDescription = myStu.fullNameWithFather,
                                    badge = myBadges.firstOrNull(),
                                    avatarSize = 52.dp,
                                    boxWidth = 58.dp
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "سجل الحضور الخاص: ${myStu.fullNameWithFather}",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestGreen
                                    )
                                    Text(
                                        text = "الصف: ${myStu.gradeClass} (شعبة ${myStu.sectionName}) • حالة اليوم ($selectedDate): ${todayStatus.arabicLabel}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepForestGreen.copy(alpha = 0.88f)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NoWrapBadge(
                                            text = "الغياب: $myTotalAbsences يوم",
                                            icon = Icons.Filled.EventBusy,
                                            containerColor = if (myTotalAbsences > 0) DangerRed else SuccessGreen,
                                            contentColor = Color.White
                                        )
                                        if (myStu.enrollmentStatusEnum != StudentEnrollmentStatus.ACTIVE) {
                                            NoWrapBadge(
                                                text = "القيد: ${myStu.enrollmentStatusEnum.arabicLabel}",
                                                icon = Icons.Filled.Gavel,
                                                containerColor = DangerRed.copy(alpha = 0.14f),
                                                contentColor = DangerRed
                                            )
                                        }
                                    }
                                }
                            }

                            val mySelectedDateGateLog = myStu.getGateLogForDate(selectedDate)
                            if (mySelectedDateGateLog != null && (mySelectedDateGateLog.checkInTime.isNotBlank() || mySelectedDateGateLog.checkOutTime.isNotBlank())) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("attendance_student_gate_status_badge"),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (mySelectedDateGateLog.isDeparted) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = if (mySelectedDateGateLog.isDeparted) {
                                                "غادر المدرسة الساعة ${mySelectedDateGateLog.checkOutTime} (وقت الدخول: ${mySelectedDateGateLog.checkInTime.ifBlank { "—" }})"
                                            } else {
                                                "متواجد داخل المدرسة منذ الساعة ${mySelectedDateGateLog.checkInTime}"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            color = DeepForestGreen,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (featureSettings.leaveRequestsEnabled) {
                                    val todayLeaveReq = remember(leaveRequests, myStu.id, selectedDate) {
                                        viewModel.getStudentLeaveRequestForDate(myStu.id, selectedDate)
                                    }
                                    val isExcusedToday = todayStatus == AttendanceStatus.EXCUSED ||
                                        todayLeaveReq?.status == LeaveRequestStatus.APPROVED
                                    val isPendingToday = todayLeaveReq?.status == LeaveRequestStatus.PENDING
                                    val isRejectedToday = todayLeaveReq?.status == LeaveRequestStatus.REJECTED

                                    when {
                                        isExcusedToday -> {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("student_excused_today_badge"),
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color(0xFF065F46),
                                                border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldFoil)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.CheckCircle,
                                                        contentDescription = null,
                                                        tint = Color(0xFFA7F3D0),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "الطالب مجاز اليوم رسمياً",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color.White,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        isPendingToday -> {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("leave_pending_today_badge"),
                                                shape = RoundedCornerShape(12.dp),
                                                color = WarningAmber.copy(alpha = 0.16f),
                                                border = androidx.compose.foundation.BorderStroke(1.2.dp, WarningAmber)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PendingActions,
                                                        contentDescription = null,
                                                        tint = Color(0xFFB45309),
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "الطلب بانتظار الموافقة",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = Color(0xFF92400E),
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        isRejectedToday -> {
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(48.dp)
                                                    .testTag("leave_rejected_today_badge"),
                                                shape = RoundedCornerShape(12.dp),
                                                color = DangerRed.copy(alpha = 0.14f),
                                                border = androidx.compose.foundation.BorderStroke(1.2.dp, DangerRed)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .padding(horizontal = 10.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Cancel,
                                                        contentDescription = null,
                                                        tint = DangerRed,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "تم رفض طلب إجازة اليوم",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        color = DangerRed,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                        else -> {
                                            Button(
                                                onClick = { showLeaveRequestDialog = true },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .testTag("open_leave_request_dialog_btn"),
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = DeepEmerald,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(Icons.Filled.EventNote, contentDescription = null, tint = GoldFoil, modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("طلب إجازة للطالب", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }

                                if (featureSettings.lostAndFoundEnabled) {
                                    OutlinedButton(
                                        onClick = { showLostAndFoundDialog = true },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("open_lost_and_found_btn_student"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("سجل المفقودات (${lostAndFoundItems.size})", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            if (currentUser?.role == UserRole.PARENT && featureSettings.dismissalCallEnabled) {
                                val activeDismissalCall = dismissalCalls.firstOrNull { it.studentId == myStu.id && !it.isAcknowledged }
                                val isHolidayOnSelectedDate = selectedDateHolidayLabel != null || todayStatus == AttendanceStatus.HOLIDAY
                                InteractiveParentDismissalPickupCard(
                                    student = myStu,
                                    activeCall = activeDismissalCall,
                                    isHoliday = isHolidayOnSelectedDate,
                                    holidayLabel = selectedDateHolidayLabel ?: "عطلة رسمية",
                                    onTriggerCall = { viewModel.triggerParentDismissalCall(myStu) },
                                    onHolidayLockedClick = {
                                        viewModel.showToastAlert(
                                            "الاستدعاء السريع يعمل فقط خلال أيام وأوقات الدوام الرسمي — اليوم عطلة رسمية",
                                            isSuccess = false
                                        )
                                    },
                                    buttonTestTag = "attendance_parent_dismissal_call_btn",
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 3-Tab Organized Sub-Navigation for Parent & Student:
                // 0: سجل الدوام اليومي | 1: دفتر المتابعة والسلوك | 2: طلبات الإجازة
                item {
                    val parentSubTabs = listOf(
                        Triple(0, "سجل الدوام اليومي (${myRecords.size})", Icons.Filled.FactCheck),
                        Triple(1, "دفتر المتابعة والسلوك (${myBehaviors.size})", Icons.Filled.AssignmentTurnedIn),
                        Triple(2, "طلبات الإجازة (${myLeaveRequests.size})", Icons.Filled.EventNote)
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(parentSubTabs, key = { it.first }) { (tabIdx, tabLabel, tabIcon) ->
                            val isSelected = selectedParentAttendanceSubTab == tabIdx
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color(0xFF059669) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedParentAttendanceSubTab = tabIdx }
                                    .testTag("parent_attendance_subtab_$tabIdx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tabLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                // Tab 1: Daily Behavior & Classroom Follow-Up Log for Student/Parent
                if (selectedParentAttendanceSubTab == 1 && featureSettings.dailyBehaviorEnabled) {
                    item {
                        if (myBehaviors.isEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد ملاحظات سلوكية أو تقييمات متابعة يومية مسجلة حالياً.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        } else {
                            CompactDailyBehaviorCard(
                                title = "دفتر المتابعة اليومي والسلوك الصفي (${myBehaviors.size}):",
                                studentName = myStu.fullNameWithFather,
                                behaviors = myBehaviors,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Tab 2: Leave Requests for Student/Parent
                if (selectedParentAttendanceSubTab == 2 && featureSettings.leaveRequestsEnabled) {
                    item {
                        Text(
                            text = "سجل طلبات الإجازة الإلكترونية (${myLeaveRequests.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (myLeaveRequests.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "لا توجد طلبات إجازة مرسلة بعد.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        val visibleMyLeaves = myLeaveRequests.take(studentLeaveRequestsVisibleCount)
                        items(visibleMyLeaves, key = { it.id }) { req ->
                            LeaveRequestCardItem(
                                req = req,
                                isAdmin = false,
                                onApprove = {},
                                onReject = {}
                            )
                        }
                        if (myLeaveRequests.size > 3) {
                            item(key = "student_leaves_load_more") {
                                ProgressiveInfiniteScrollFooter(
                                    visibleCount = visibleMyLeaves.size,
                                    totalCount = myLeaveRequests.size,
                                    step = 3,
                                    itemLabel = "طلب إجازة",
                                    onLoadMore = {
                                        studentLeaveRequestsVisibleCount = (studentLeaveRequestsVisibleCount + 3).coerceAtMost(myLeaveRequests.size)
                                    }
                                )
                            }
                        }
                    }
                }

                // Tab 0: Daily Attendance Log (Cleaned of duplicate holiday text)
                if (selectedParentAttendanceSubTab == 0) {
                    val visibleMyRecords = myRecords.take(studentAttendanceRecordsVisibleCount)
                    items(visibleMyRecords, key = { it.id }) { rec ->
                        val recordHolidayLabel = viewModel.getHolidayLabelForDate(rec.date)
                        val effectiveStatus = if (recordHolidayLabel != null) AttendanceStatus.HOLIDAY else rec.status
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("التاريخ: ${rec.date}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                                    val cleanSubNote = when {
                                        effectiveStatus == AttendanceStatus.HOLIDAY ->
                                            recordHolidayLabel ?: "عطلة رسمية معتمدة في التقويم"
                                        rec.note.isNotBlank() && rec.note != effectiveStatus.arabicLabel ->
                                            rec.note
                                        else -> ""
                                    }
                                    if (cleanSubNote.isNotBlank()) {
                                        Text(
                                            text = cleanSubNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                NoWrapBadge(
                                    text = if (effectiveStatus == AttendanceStatus.HOLIDAY) {
                                        "يوم عطلة رسمية"
                                    } else {
                                        effectiveStatus.arabicLabel
                                    },
                                    icon = if (effectiveStatus == AttendanceStatus.HOLIDAY) Icons.Filled.EventAvailable else Icons.Filled.FactCheck,
                                    containerColor = when (effectiveStatus) {
                                        AttendanceStatus.HOLIDAY -> GoldFoil.copy(alpha = 0.18f)
                                        AttendanceStatus.ABSENT -> DangerRed.copy(alpha = 0.15f)
                                        AttendanceStatus.EXCUSED -> InfoBlue.copy(alpha = 0.15f)
                                        else -> SuccessGreen.copy(alpha = 0.15f)
                                    },
                                    contentColor = when (effectiveStatus) {
                                        AttendanceStatus.HOLIDAY -> DeepForestGreen
                                        AttendanceStatus.ABSENT -> DangerRed
                                        AttendanceStatus.EXCUSED -> InfoBlue
                                        else -> SuccessGreen
                                    }
                                )
                            }
                        }
                    }
                    if (visibleMyRecords.size < myRecords.size) {
                        item {
                            ProgressiveInfiniteScrollFooter(
                                visibleCount = visibleMyRecords.size,
                                totalCount = myRecords.size,
                                step = 6,
                                itemLabel = "سجل حضور",
                                onLoadMore = {
                                    studentAttendanceRecordsVisibleCount = (studentAttendanceRecordsVisibleCount + 6).coerceAtMost(myRecords.size)
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // 4-Tab 3D Sub-Navigation Bar for Admin & Teacher inside DailyAttendanceScreen
            val scopedLeaveRequestsForManager = if (isAdmin) {
                leaveRequests
            } else {
                val allowedIds = scopedStudents.map { it.id.trim() }.toSet()
                leaveRequests.filter { it.studentId.trim() in allowedIds }
            }
            val pendingLeavesCount = scopedLeaveRequestsForManager.count { it.status == LeaveRequestStatus.PENDING }
            val activeDismissalCalls = dismissalCalls.filter { !it.isAcknowledged }

            item {
                val subTabs = listOf(
                    Triple(0, "رصد الدوام والسلوك", Icons.Filled.HowToReg),
                    Triple(1, "البوابة الذكية (الباركود)", Icons.Filled.QrCodeScanner),
                    Triple(2, if (pendingLeavesCount > 0) "طلبات الإجازات الواردة ($pendingLeavesCount)" else "طلبات الإجازات الواردة", Icons.Filled.EventNote),
                    Triple(3, "إدارة العطل الرسمية", Icons.Filled.EventAvailable)
                )

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Scrollable 3D Glassmorphic Sub-Tab Switcher
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(subTabs, key = { it.first }) { (tabIdx, tabLabel, tabIcon) ->
                            val isSelected = selectedAttendanceSubTab == tabIdx
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSelected) Color(0xFF059669) else Color.White,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.2.dp,
                                    if (isSelected) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { selectedAttendanceSubTab = tabIdx }
                                    .testTag("attendance_subtab_$tabIdx")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = tabIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF047857),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = tabLabel,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Sub-Tab 1: Smart Barcode Gate & Dismissal Calls & Bus Alerts
                    if (selectedAttendanceSubTab == 1) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "البوابة الذكية وحركة الدخول والمغادرة بالباركود",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "امسح باركود هوية الطالب لتسجيل وقت الدخول صباحاً أو وقت المغادرة مع إرسال إشعار فوري لولي الأمر.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF475569)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            gateScannerInitialMode = GateScanMode.CHECK_IN
                                            showQrScannerDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF059669),
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("مسح باركود الدخول", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            gateScannerInitialMode = GateScanMode.CHECK_OUT
                                            showQrScannerDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF059669))
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text("مسح باركود المغادرة", fontSize = 12.sp, color = Color(0xFF047857), fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                                    }
                                }
                            }
                        }
                    }

                    // Sub-Tab 2: Electronic Leave Requests
                    if (selectedAttendanceSubTab == 2) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0))
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
                                    Text(
                                        text = "طلبات الإجازة الإلكترونية الواردة (${scopedLeaveRequestsForManager.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    if (pendingLeavesCount > 0) {
                                        NoWrapBadge(
                                            text = "$pendingLeavesCount قيد المراجعة",
                                            icon = Icons.Filled.PendingActions,
                                            containerColor = WarningAmber,
                                            contentColor = Color.White
                                        )
                                    }
                                }
                                if (scopedLeaveRequestsForManager.isEmpty()) {
                                    Text(
                                        text = "لا توجد طلبات إجازة واردة حالياً.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF64748B)
                                    )
                                } else {
                                    val visibleAdminLeaves = scopedLeaveRequestsForManager.take(adminLeaveRequestsVisibleCount)
                                    visibleAdminLeaves.forEach { req ->
                                        LeaveRequestCardItem(
                                            req = req,
                                            isAdmin = canEdit,
                                            onApprove = {
                                                viewModel.requireOnlineAndExecute {
                                                    viewModel.respondToLeaveRequest(req.id, approve = true)
                                                }
                                            },
                                            onReject = {
                                                viewModel.requireOnlineAndExecute {
                                                    viewModel.respondToLeaveRequest(req.id, approve = false)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Sub-Tab 3: Official Holidays & Lost and Found Registry
                    if (selectedAttendanceSubTab == 3) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "إدارة العطل الرسمية وسجل المفقودات المدرسية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                if (isAdmin) {
                                    Button(
                                        onClick = { viewModel.openHolidaysManagementModal() },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F3D5E), contentColor = Color.White)
                                    ) {
                                        Icon(Icons.Filled.EventAvailable, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح نافذة إدارة العطل الرسمية والأسبوعية", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                if (featureSettings.lostAndFoundEnabled) {
                                    OutlinedButton(
                                        onClick = { showLostAndFoundDialog = true },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669))
                                    ) {
                                        Icon(Icons.Filled.Inventory2, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("سجل المفقودات المدرسية المصور (${lostAndFoundItems.size})", fontWeight = FontWeight.ExtraBold, color = Color(0xFF047857))
                                    }
                                }
                            }
                        }
                    }

                    // Main Attendance Filter & Action Card (Always accessible in Tab 0 & keeps all testTags active)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 10.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color(0xFF059669).copy(alpha = 0.18f),
                                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.18f)
                            ),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Header Row with Full Unclipped Date Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFD1FAE5),
                                        modifier = Modifier.size(38.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = Icons.Filled.HowToReg,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "سجل الحضور والغياب",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A),
                                        maxLines = 1
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Today,
                                            contentDescription = null,
                                            tint = Color(0xFF047857),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = selectedDate,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Prominent 3D Official Holiday Lock Banner when selectedDate is a holiday
                            if (selectedDateHolidayLabel != null) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFF059669),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, Color.White.copy(alpha = 0.5f)),
                                    shadowElevation = 4.dp,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EventAvailable,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "يوم عطلة رسمية معتمدة — جميع إجراءات الرصد والسلوك مقفلة في هذا التاريخ",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "المناسبة: $selectedDateHolidayLabel • لا يُحتسب غياب على الطلبة",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 11.sp,
                                                color = Color.White.copy(alpha = 0.92f)
                                            )
                                        }
                                    }
                                }
                            } else {
                                // Smart Auto-Absence Countdown Banner & Principal Controls (after 2 hours default from first attendance)
                                val delayMins = featureSettings.autoAbsenceDelayMinutes.coerceIn(15, 720)
                                val unrecordedInSchoolCount = allTeacherAssignedStudents.count {
                                    viewModel.getStudentStatusForDate(it, selectedDate) == AttendanceStatus.UNRECORDED
                                }
                                val remainingMs = if (firstAttendanceTs > 0L) {
                                    ((firstAttendanceTs + delayMins * 60_000L) - autoAbsenceNowMs).coerceAtLeast(0L)
                                } else -1L
                                val remHours = if (remainingMs > 0L) remainingMs / 3_600_000L else 0L
                                val remMins = if (remainingMs > 0L) (remainingMs % 3_600_000L) / 60_000L else 0L

                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("smart_auto_absence_control_card")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
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
                                                Icon(
                                                    imageVector = Icons.Filled.Timer,
                                                    contentDescription = null,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = "الاحتساب التلقائي للغياب بعد بدء الرصد الصباحي",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF064E3B)
                                                    )
                                                    val timerSubtitle = when {
                                                        !featureSettings.autoAbsenceEnabled ->
                                                            "الاحتساب التلقائي متوقف حالياً بأمر المدير"
                                                        firstAttendanceTs <= 0L ->
                                                            "يبدأ المؤقت تلقائياً (${delayMins / 60} ساعة) فور تسجيل أول طالب حاضر اليوم"
                                                        remainingMs > 0L && unrecordedInSchoolCount > 0 ->
                                                            "متبقي (${remHours} ساعة و ${remMins} دقيقة) لاحتساب الغياب تلقائياً لـ ($unrecordedInSchoolCount) طالب بانتظار الرصد"
                                                        unrecordedInSchoolCount == 0 ->
                                                            "اكتمل رصد جميع الطلاب لهذا اليوم بالكامل"
                                                        else ->
                                                            "انتهت المهلة الصباحية — يتم تثبيت الغياب للطلبة غير المرصودين"
                                                    }
                                                    Text(
                                                        text = timerSubtitle,
                                                        fontSize = 10.8.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = Color(0xFF475569)
                                                    )
                                                }
                                            }
                                            if (isAdmin) {
                                                Switch(
                                                    checked = featureSettings.autoAbsenceEnabled,
                                                    onCheckedChange = { viewModel.toggleAutoAbsenceEnabled(it) },
                                                    modifier = Modifier.testTag("auto_absence_toggle_switch")
                                                )
                                            }
                                        }

                                        if (isAdmin && featureSettings.autoAbsenceEnabled) {
                                            val delayOptions = listOf(
                                                60 to "ساعة واحدة",
                                                120 to "ساعتان",
                                                180 to "3 ساعات",
                                                240 to "4 ساعات"
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                delayOptions.forEach { (mins, label) ->
                                                    val isSel = delayMins == mins
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = if (isSel) Color(0xFF059669) else Color.White,
                                                        border = androidx.compose.foundation.BorderStroke(
                                                            1.dp,
                                                            if (isSel) Color(0xFF059669) else Color(0xFFCBD5E1)
                                                        ),
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(50))
                                                            .clickable { viewModel.updateAutoAbsenceDelayMinutes(mins) }
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = if (isSel) Color.White else Color(0xFF334155),
                                                            textAlign = TextAlign.Center,
                                                            maxLines = 1,
                                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 4.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // 3D Barcode Check-In / Check-Out Buttons (Unclipped single-line text)
                            if (featureSettings.qrAttendanceEnabled) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            gateScannerInitialMode = GateScanMode.CHECK_IN
                                            showQrScannerDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("open_qr_attendance_scanner_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF059669),
                                            contentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "مسح باركود الدخول",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            gateScannerInitialMode = GateScanMode.CHECK_OUT
                                            showQrScannerDialog = true
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(46.dp)
                                            .testTag("open_qr_checkout_scanner_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF059669)),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            containerColor = Color(0xFFECFDF5),
                                            contentColor = Color(0xFF047857)
                                        ),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Logout,
                                            contentDescription = null,
                                            tint = Color(0xFF047857),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "مسح باركود المغادرة",
                                            fontSize = 12.sp,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            // Admin Action Row: Prominent 3D Official Holidays Manager Button + Monthly PDF Report
                            if (isAdmin) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.openHolidaysManagementModal() },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("attendance_open_holidays_modal_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF0F3D5E),
                                            contentColor = Color.White
                                        ),
                                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EventAvailable,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "إدارة العطل الرسمية",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            val monthPrefix = selectedDate.take(7)
                                            val classStudents = students.filter {
                                                ArabicTextNormalizer.matchesClass(it.gradeClass, selectedClass) &&
                                                    ArabicTextNormalizer.matchesSection(it.sectionName, selectedSection)
                                            }
                                            val res = DocumentAndIdExporter.exportMonthlyAttendanceReportPdf(
                                                context = context,
                                                schoolConfig = schoolConfig,
                                                gradeClass = selectedClass,
                                                sectionName = selectedSection,
                                                monthPrefix = monthPrefix,
                                                students = classStudents,
                                                attendanceList = attendance,
                                                principalName = currentUser?.fullName ?: "المدير"
                                            )
                                            if (res.isSuccess) {
                                                viewModel.showToastAlert(
                                                    "تم تصدير كشف الغياب الشهري بصيغة PDF وفتح نافذة الطباعة بنجاح",
                                                    isSuccess = true
                                                )
                                            } else {
                                                viewModel.showToastAlert(
                                                    "تعذر تصدير كشف الغياب الشهري: ${res.exceptionOrNull()?.message}",
                                                    isSuccess = false
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .testTag("export_monthly_attendance_pdf_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.2.dp, Color(0xFF059669)),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PictureAsPdf,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "كشف الغياب الشهري",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }

                            if (featureSettings.lostAndFoundEnabled) {
                                OutlinedButton(
                                    onClick = { showLostAndFoundDialog = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("open_lost_and_found_btn_admin"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Inventory2, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "سجل المفقودات المدرسية المصور (${lostAndFoundItems.size})",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Date Picker Row (Full width comfortable row)
                            ModernCalendarDatePickerField(
                                selectedDateIso = selectedDate,
                                label = "تاريخ رصد الحضور والغياب",
                                onDateSelected = { selectedIso, _ -> viewModel.setSelectedAttendanceDate(selectedIso) },
                                modifier = Modifier.fillMaxWidth()
                            )

                            // Student Search Field in its own full-width comfortable row so "ابحث باسم الطالب..." never clips
                            OutlinedTextField(
                                value = searchName,
                                onValueChange = { searchName = it },
                                placeholder = {
                                    Text(
                                        text = "ابحث باسم الطالب...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                trailingIcon = if (searchName.isNotBlank()) {
                                    {
                                        IconButton(onClick = { searchName = "" }) {
                                            Icon(Icons.Filled.Close, contentDescription = "مسح البحث", modifier = Modifier.size(18.dp))
                                        }
                                    }
                                } else null,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Text(
                                text = "اختر الصف الدراسي المسجل:",
                                style = MaterialTheme.typography.labelLarge
                            )
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(allowedClasses) { cls ->
                                    FilterChip(
                                        selected = selectedClass == cls,
                                        onClick = {
                                            viewModel.setSelectedAttendanceClass(cls)
                                            val nextSecs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                                            if (selectedSection !in nextSecs) {
                                                viewModel.setSelectedAttendanceSection(nextSecs.firstOrNull() ?: "أ")
                                            }
                                        },
                                        label = { Text(cls, maxLines = 1, softWrap = false) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (selectedClass == cls) Icons.Filled.Check else Icons.Filled.Class,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("الشعبة المسجلة:", style = MaterialTheme.typography.labelLarge)
                                allowedSections.forEach { sec ->
                                    FilterChip(
                                        selected = selectedSection == sec,
                                        onClick = { viewModel.setSelectedAttendanceSection(sec) },
                                        label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                    )
                                }
                            }

                            // Scoped Student Count Indicator
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = DeepEmerald.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Groups,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isTeacher) {
                                            "طلابك في ($selectedClass - شعبة $selectedSection): ${scopedStudents.size} طالب (إجمالي طلابك المسندين: ${allTeacherAssignedStudents.size})"
                                        } else {
                                            "عدد طلاب ($selectedClass - شعبة $selectedSection): ${scopedStudents.size} طالب"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Parent Dismissal Calls Card ("الاستدعاء السريع عند الانصراف")
            if (featureSettings.dismissalCallEnabled && activeDismissalCalls.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_active_dismissal_calls_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = WarmGoldLight),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldFoil)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = DeepForestGreen)
                                Text(
                                    text = "طلبات استلام الطلاب عند البوابة الآن (${activeDismissalCalls.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            activeDismissalCalls.forEach { call ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "الطالب: ${call.studentName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = DeepForestGreen,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "${call.gradeClass} (${call.sectionName}) • ولي الأمر: ${call.parentName} (${call.requestedAt})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepEmerald
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.markDismissalCallHandled(call.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = SuccessGreen,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تم التسليم", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // School Bus Executive Telemetry Card in Attendance section
            if ((schoolConfig.schoolType == SchoolType.PRIVATE || featureSettings.busTrackingEnabled) && canEdit) {
                item {
                    val busSubscribersCount = students.count { it.isBusSubscribed && it.isActive }
                    SchoolBusQuickAlertsCard(
                        busSubscribersCount = busSubscribersCount,
                        activeBusEventName = schoolConfig.activeBusEvent,
                        activeBusEventTimestamp = schoolConfig.activeBusEventTimestamp,
                        canControlTrip = true,
                        onSendBusAlert = { eventType -> viewModel.sendBusTrackingAlert(eventType) },
                        onOpenTransportSection = { viewModel.navigateToSection(AppSection.SCHOOL_TRANSPORT) }
                    )
                }
            }

            // Electronic Leave Requests Section for Admin & Authorized Class Teacher (with Approve / Reject buttons)
            if (canEdit && featureSettings.leaveRequestsEnabled && scopedLeaveRequestsForManager.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.EventNote,
                                        contentDescription = null,
                                        tint = DeepEmerald
                                    )
                                    Text(
                                        text = "طلبات الإجازة الواردة",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                if (pendingLeavesCount > 0) {
                                    NoWrapBadge(
                                        text = "$pendingLeavesCount قيد المراجعة",
                                        icon = Icons.Filled.PendingActions,
                                        containerColor = WarningAmber,
                                        contentColor = Color.White
                                    )
                                }
                            }

                            val visibleAdminLeaves = scopedLeaveRequestsForManager.take(adminLeaveRequestsVisibleCount)
                            visibleAdminLeaves.forEach { req ->
                                LeaveRequestCardItem(
                                    req = req,
                                    isAdmin = canEdit,
                                    onApprove = {
                                        viewModel.requireOnlineAndExecute {
                                            viewModel.respondToLeaveRequest(req.id, approve = true)
                                        }
                                    },
                                    onReject = {
                                        viewModel.requireOnlineAndExecute {
                                            viewModel.respondToLeaveRequest(req.id, approve = false)
                                        }
                                    }
                                )
                            }
                            if (scopedLeaveRequestsForManager.size > 3) {
                                ProgressiveInfiniteScrollFooter(
                                    visibleCount = visibleAdminLeaves.size,
                                    totalCount = scopedLeaveRequestsForManager.size,
                                    step = 3,
                                    itemLabel = "طلب إجازة",
                                    onLoadMore = {
                                        adminLeaveRequestsVisibleCount = (adminLeaveRequestsVisibleCount + 3).coerceAtMost(scopedLeaveRequestsForManager.size)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Student Attendance Cards for the selected Class & Section (6 by 6 progressive loading)
            val visibleScopedStudents = scopedStudents.take(teacherStudentsVisibleCount)
            items(visibleScopedStudents, key = { it.id }) { stu ->
                val cleanSelDate = ArabicTextNormalizer.normalizeDateIso(selectedDate.trim())
                val currentEntry = attendance.firstOrNull {
                    it.studentId.trim() == stu.id.trim() &&
                        ArabicTextNormalizer.normalizeDateIso(it.date.trim()) == cleanSelDate
                }
                val currentStatus = viewModel.getStudentStatusForDate(stu, cleanSelDate)
                val totalAbsences = viewModel.getTotalAbsenceDays(stu.id)
                val stuBadges = remember(attendance, students, stu.id, stu.dailyAttendance, holidaysConfig) {
                    viewModel.getStudentAutoBadges(stu.id)
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.20f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. رأس البطاقة: صورة الطالب المدمجة بالوسام (58.dp) + الاسم الثلاثي بعرض كامل + صف أفقي للغيابات وحالة القيد
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentAvatarWithOverlayBadge(
                                base64Data = stu.photoBase64,
                                contentDescription = stu.fullNameWithFather,
                                badge = stuBadges.firstOrNull(),
                                avatarSize = 52.dp,
                                boxWidth = 58.dp
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = stu.fullNameWithFather,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stu.gradeClass} • شعبة (${stu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    softWrap = false
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    NoWrapBadge(
                                        text = "الغياب: $totalAbsences يوم",
                                        icon = Icons.Filled.EventBusy,
                                        containerColor = if (totalAbsences > 0) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.14f),
                                        contentColor = if (totalAbsences > 0) DangerRed else SuccessGreen
                                    )
                                    if (stu.enrollmentStatusEnum != StudentEnrollmentStatus.ACTIVE) {
                                        NoWrapBadge(
                                            text = stu.enrollmentStatusEnum.arabicLabel,
                                            icon = Icons.Filled.Gavel,
                                            containerColor = DangerRed.copy(alpha = 0.14f),
                                            contentColor = DangerRed
                                        )
                                    }
                                }
                            }

                            if (featureSettings.qrAttendanceEnabled) {
                                IconButton(
                                    onClick = { selectedStudentForIdCard = stu },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("att_open_student_id_${stu.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = "بطاقة الهوية والـ QR",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // 1.B: شارة حركة البوابة الذكية (وقت الدخول / وقت المغادرة + زر "تسجيل مغادرة" السريع)
                        val stuGateLog = stu.getGateLogForDate(cleanSelDate)
                        val isStudentPresentOrInside = currentStatus == AttendanceStatus.PRESENT ||
                            currentStatus == AttendanceStatus.LATE ||
                            stuGateLog != null
                        if (selectedDateHolidayLabel == null && currentStatus != AttendanceStatus.HOLIDAY && isStudentPresentOrInside) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_gate_log_row_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.32f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (stuGateLog?.isDeparted == true) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                            contentDescription = null,
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = if (stuGateLog?.isDeparted == true) {
                                                "الدخول: ${stuGateLog.checkInTime.ifBlank { "—" }} | غادر المدرسة: ${stuGateLog.checkOutTime}"
                                            } else {
                                                val cInText = stuGateLog?.checkInTime?.takeIf { it.isNotBlank() } ?: "مرصود اليوم"
                                                "وقت الدخول: $cInText • متواجد بالمدرسة"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepForestGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    if (canEdit && stuGateLog?.isDeparted != true) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .clickable {
                                                    viewModel.requireOnlineAndExecute {
                                                        viewModel.recordStudentGateCheckOut(stu, cleanSelDate)
                                                    }
                                                }
                                                .testTag("quick_gate_checkout_btn_${stu.id}"),
                                            shape = RoundedCornerShape(50),
                                            color = DeepEmerald
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "تسجيل مغادرة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Real-time Pending Leave Alert Banner directly on the Student Card for Principal & Teacher
                        val pendingLeaveForDate = remember(leaveRequests, stu.id, cleanSelDate) {
                            leaveRequests.firstOrNull {
                                it.studentId.trim() == stu.id.trim() &&
                                    it.status == LeaveRequestStatus.PENDING &&
                                    viewModel.isDateCoveredByLeaveRequest(it, cleanSelDate)
                            }
                        }
                        if (pendingLeaveForDate != null && canEdit) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_card_pending_leave_banner_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = WarningAmber.copy(alpha = 0.14f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, WarningAmber)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.PendingActions,
                                            contentDescription = null,
                                            tint = Color(0xFFB45309),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "يوجد طلب إجازة معلق لهذا اليوم (${pendingLeaveForDate.leaveType.arabicLabel})",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF92400E),
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Text(
                                                text = "السبب: ${pendingLeaveForDate.reason} • المدة: ${pendingLeaveForDate.durationDays} يوم",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF78350F)
                                            )
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                viewModel.requireOnlineAndExecute {
                                                    viewModel.respondToLeaveRequest(pendingLeaveForDate.id, approve = true)
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(38.dp)
                                                .testTag("quick_approve_leave_${stu.id}"),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = DeepEmerald,
                                                contentColor = Color.White
                                            )
                                        ) {
                                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Color(0xFFA7F3D0), modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("موافقة (تحويل لمجاز)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.requireOnlineAndExecute {
                                                    viewModel.respondToLeaveRequest(pendingLeaveForDate.id, approve = false)
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(0.75f)
                                                .height(38.dp)
                                                .testTag("quick_reject_leave_${stu.id}"),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Filled.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("رفض الطلب", style = MaterialTheme.typography.labelSmall, color = DangerRed, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }

                        // 2. أزرار تسجيل الحضور (حاضر، غائب، متأخر، مجاز) أو قفل اكتمال الدوام أو قيد الطالب أو شارة يوم عطلة رسمية
                        if (stu.isAttendanceBlockedByEnrollmentStatus(cleanSelDate)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("att_enrollment_locked_badge_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = DangerRed.copy(alpha = 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, DangerRed.copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Block,
                                        contentDescription = null,
                                        tint = DangerRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stu.getEnrollmentBlockBannerText(cleanSelDate),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DangerRed
                                    )
                                }
                            }
                        } else if (selectedDateHolidayLabel != null || currentStatus == AttendanceStatus.HOLIDAY) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("att_holiday_locked_badge_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF064E3B).copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldFoil.copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "البوابة مغلقة (يوم عطلة) — جميع إجراءات الرصد والدخول والمغادرة مقفلة في هذا التاريخ",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else if (stu.isDayAttendanceLocked(cleanSelDate)) {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("att_day_completed_locked_badge_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.55f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "اكتمل دوام الطالب لهذا اليوم (دخول ومغادرة موثقة — مقفل حتى يوم غد)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        } else {
                            if (currentStatus == AttendanceStatus.UNRECORDED) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF1F5F9),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Schedule,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "حالة الحضور ليوم ($cleanSelDate): لم يُرصد بعد (بانتظار الرصد)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                AttendanceStatus.manualSelectableStatuses.forEach { st ->
                                    val isSelected = currentStatus == st
                                    val statusColor = when (st) {
                                        AttendanceStatus.PRESENT -> SuccessGreen
                                        AttendanceStatus.ABSENT -> DangerRed
                                        AttendanceStatus.LATE -> WarningAmber
                                        AttendanceStatus.EXCUSED -> InfoBlue
                                        AttendanceStatus.HOLIDAY -> GoldAccent
                                        AttendanceStatus.UNRECORDED -> Color(0xFF64748B)
                                    }
                                    val statusIcon = when (st) {
                                        AttendanceStatus.PRESENT -> Icons.Filled.CheckCircle
                                        AttendanceStatus.ABSENT -> Icons.Filled.Cancel
                                        AttendanceStatus.LATE -> Icons.Filled.Schedule
                                        AttendanceStatus.EXCUSED -> Icons.Filled.AssignmentLate
                                        AttendanceStatus.HOLIDAY -> Icons.Filled.EventAvailable
                                        AttendanceStatus.UNRECORDED -> Icons.Filled.Schedule
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(enabled = canEdit) {
                                                viewModel.requireOnlineAndExecute {
                                                    viewModel.recordStudentAttendance(stu, cleanSelDate, st)
                                                }
                                            }
                                            .testTag("att_btn_${stu.id}_${st.name}"),
                                        color = if (isSelected) statusColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.16f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = statusIcon,
                                                contentDescription = st.arabicLabel,
                                                tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = st.arabicLabel,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // 3. خط فاصل ناعم وأنيق + 4. أزرار تقييم المتابعة والسلوك الصفي الفوري (تُقفل وتُخفى تلقائياً في أيام العطل الرسمية والأسبوعية!)
                        if (canEdit && featureSettings.dailyBehaviorEnabled && selectedDateHolidayLabel == null && currentStatus != AttendanceStatus.HOLIDAY) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = DeepEmerald.copy(alpha = 0.16f)
                            )

                            val teacherDefaultSubj = currentUser?.specialty?.split("،")?.firstOrNull()?.trim()?.ifBlank { "المتابعة اليومية" } ?: "المتابعة اليومية"
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = ClipboardCheckVectorIcon,
                                        contentDescription = "دفتر المتابعة اليومي والسلوك",
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "تقييم دفتر المتابعة اليومي والسلوك الصفي (يرسل إشعاراً لولي الأمر):",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                var showCustomBehaviorDialog by remember(stu.id) { mutableStateOf(false) }
                                var manualBehaviorNoteText by remember(stu.id) { mutableStateOf("") }

                                DailyBehaviorType.entries.chunked(2).forEach { rowItems ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        rowItems.forEach { behType ->
                                            val behIcon = when (behType) {
                                                DailyBehaviorType.EXCELLENT_PARTICIPATION -> Icons.Filled.EmojiEvents
                                                DailyBehaviorType.FORGOT_BOOK -> Icons.Filled.MenuBook
                                                DailyBehaviorType.MISSED_HOMEWORK -> Icons.Filled.AssignmentLate
                                                DailyBehaviorType.BEHAVIOR_NOTE -> Icons.Filled.EditNote
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        if (behType == DailyBehaviorType.BEHAVIOR_NOTE) {
                                                            showCustomBehaviorDialog = true
                                                        } else {
                                                            viewModel.sendDailyBehaviorEvaluation(
                                                                student = stu,
                                                                behaviorType = behType,
                                                                subjectName = teacherDefaultSubj
                                                            )
                                                        }
                                                    }
                                                    .testTag("behavior_chip_${stu.id}_${behType.name}"),
                                                shape = RoundedCornerShape(12.dp),
                                                color = if (behType.isPositive) {
                                                    SuccessGreen.copy(alpha = 0.12f)
                                                } else {
                                                    DeepEmerald.copy(alpha = 0.08f)
                                                },
                                                border = androidx.compose.foundation.BorderStroke(
                                                    1.dp,
                                                    if (behType.isPositive) SuccessGreen.copy(alpha = 0.45f) else DeepEmerald.copy(alpha = 0.30f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 10.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = behIcon,
                                                        contentDescription = null,
                                                        tint = if (behType.isPositive) SuccessGreen else DeepEmerald,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = behType.arabicTitle,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DeepForestGreen,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // حقل مباشر لكتابة تنبيه أو ملاحظة سلوكية يدوية مخصصة وإرسالها فوراً لولي الأمر
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = manualBehaviorNoteText,
                                        onValueChange = { manualBehaviorNoteText = it },
                                        placeholder = {
                                            Text(
                                                "اكتب تنبيهاً أو ملاحظة سلوكية يدوية مخصصة لولي الأمر...",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Filled.EditNote,
                                                contentDescription = null,
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("custom_behavior_note_input_${stu.id}")
                                    )
                                    Button(
                                        onClick = {
                                            val cleanCustom = manualBehaviorNoteText.trim()
                                            if (cleanCustom.isNotEmpty()) {
                                                viewModel.sendDailyBehaviorEvaluation(
                                                    student = stu,
                                                    behaviorType = DailyBehaviorType.BEHAVIOR_NOTE,
                                                    subjectName = teacherDefaultSubj,
                                                    customNote = cleanCustom
                                                )
                                                manualBehaviorNoteText = ""
                                            } else {
                                                showCustomBehaviorDialog = true
                                            }
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = DeepEmerald,
                                            contentColor = Color.White
                                        ),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("send_custom_behavior_note_btn_${stu.id}")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "إرسال الملاحظة السلوكية",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "إرسال",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }

                                if (showCustomBehaviorDialog) {
                                    var dialogNoteInput by remember { mutableStateOf(manualBehaviorNoteText) }
                                    AlertDialog(
                                        onDismissRequest = { showCustomBehaviorDialog = false },
                                        icon = {
                                            Icon(
                                                imageVector = Icons.Filled.EditNote,
                                                contentDescription = null,
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        },
                                        title = {
                                            Text(
                                                text = "كتابة ملاحظة سلوكية أو تنبيه يدوي",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                        },
                                        text = {
                                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                                Text(
                                                    text = "الطالب: ${stu.fullNameWithFather} (${stu.gradeClass} - ${stu.sectionName})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = DeepEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                OutlinedTextField(
                                                    value = dialogNoteInput,
                                                    onValueChange = { dialogNoteInput = it },
                                                    label = { Text("نص الملاحظة السلوكية أو التنبيه التربوي") },
                                                    placeholder = { Text("اكتب تفاصيل الملاحظة السلوكية ليصل إشعار فوري لولي الأمر...") },
                                                    minLines = 3,
                                                    maxLines = 5,
                                                    shape = RoundedCornerShape(14.dp),
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .testTag("dialog_custom_behavior_note_field_${stu.id}")
                                                )
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    val finalCustom = dialogNoteInput.trim()
                                                    viewModel.sendDailyBehaviorEvaluation(
                                                        student = stu,
                                                        behaviorType = DailyBehaviorType.BEHAVIOR_NOTE,
                                                        subjectName = teacherDefaultSubj,
                                                        customNote = finalCustom
                                                    )
                                                    manualBehaviorNoteText = ""
                                                    showCustomBehaviorDialog = false
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = DeepEmerald,
                                                    contentColor = Color.White
                                                ),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("حفظ وإرسال لولي الأمر", fontWeight = FontWeight.Bold)
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showCustomBehaviorDialog = false }) {
                                                Text("إلغاء")
                                            }
                                        }
                                    )
                                }

                                val studentBehaviors = dailyBehaviors.filter { it.studentId == stu.id }
                                if (studentBehaviors.isNotEmpty()) {
                                    CompactDailyBehaviorCard(
                                        title = "آخر ملاحظات المتابعة والسلوك (${studentBehaviors.size}):",
                                        studentName = stu.fullNameWithFather,
                                        behaviors = studentBehaviors,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (currentStatus == AttendanceStatus.ABSENT || currentStatus == AttendanceStatus.LATE) {
                            VectorActionButton(
                                text = "إرسال تبليغ ${currentStatus.arabicLabel} لولي الأمر عبر واتساب",
                                icon = Icons.Filled.Chat,
                                containerColor = WhatsAppGreen,
                                onClick = {
                                    val msg = """
                                        تبليغ رسمي من إدارة (${schoolConfig.schoolName}):
                                        إلى السيد ولي أمر الطالب (${stu.fullNameWithFather}) المحترم،
                                        نود إعلامكم بتسجيل حالة (${currentStatus.arabicLabel}) لنجلكم في الصف (${stu.gradeClass} - شعبة ${stu.sectionName}) لهذا اليوم الموافق $selectedDate.
                                        • إجمالي أيام الغياب المسجلة: $totalAbsences يوم.
                                        يرجى التواصل مع إدارة المدرسة لبيان السبب.
                                    """.trimIndent()
                                    ExternalIntents.sendWhatsAppMessage(context, stu.parentPhone, msg)
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
            if (visibleScopedStudents.size < scopedStudents.size) {
                item {
                    ProgressiveInfiniteScrollFooter(
                        visibleCount = visibleScopedStudents.size,
                        totalCount = scopedStudents.size,
                        step = 6,
                        itemLabel = "طالب",
                        onLoadMore = {
                            teacherStudentsVisibleCount = (teacherStudentsVisibleCount + 6).coerceAtMost(scopedStudents.size)
                        }
                    )
                }
            }
        }
    }

    if (showQrScannerDialog) {
        val allSchoolStudents = remember(students, schoolConfig.schoolId) {
            students.filter { it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId }
        }
        QrAttendanceScannerDialog(
            students = allSchoolStudents,
            selectedDate = selectedDate,
            initialScanMode = gateScannerInitialMode,
            onScanQrCode = { qrValue ->
                viewModel.recordAttendanceByQrCode(qrValue, selectedDate, gateScannerInitialMode)
            },
            onScanStudentDetailed = { qrValue ->
                viewModel.recordAttendanceByQrCodeDetailed(qrValue, selectedDate, gateScannerInitialMode)
            },
            onScanStudentGateDetailed = { qrValue, scanMode ->
                viewModel.recordAttendanceByQrCodeDetailed(qrValue, selectedDate, scanMode)
            },
            isHoliday = selectedDateHolidayLabel != null,
            holidayLabel = selectedDateHolidayLabel,
            toastAlert = toastAlert,
            onDismiss = { showQrScannerDialog = false }
        )
    }

    if (showLostAndFoundDialog) {
        LostAndFoundRegistryDialog(
            items = lostAndFoundItems,
            canPublish = canEdit,
            onPublishItem = { title, foundLoc, photoBase64 ->
                viewModel.publishLostAndFoundItem(title, foundLoc, photoBase64)
            },
            onUpdateItem = { itemId, title, foundLoc, photoBase64 ->
                viewModel.updateLostAndFoundItem(itemId, title, foundLoc, photoBase64)
            },
            onDeleteItem = { itemId ->
                viewModel.deleteLostAndFoundItem(itemId)
            },
            onToggleStatus = { itemId ->
                viewModel.toggleLostAndFoundStatus(itemId)
            },
            onDismiss = { showLostAndFoundDialog = false }
        )
    }

    if (showLeaveRequestDialog) {
        val targetStudent = scopedStudents.firstOrNull()
        if (targetStudent != null) {
            SubmitLeaveRequestDialog(
                student = targetStudent,
                defaultDate = selectedDate,
                onSubmit = { leaveType, leaveDate, durationDays, reason ->
                    viewModel.submitLeaveRequest(
                        student = targetStudent,
                        leaveType = leaveType,
                        leaveDate = leaveDate,
                        durationDays = durationDays,
                        reason = reason
                    )
                    showLeaveRequestDialog = false
                },
                onDismiss = { showLeaveRequestDialog = false }
            )
        }
    }

    selectedStudentForIdCard?.let { stu ->
        val badges = viewModel.getStudentAutoBadges(stu.id)
        val todayAttLabel = viewModel.getStudentStatusForDate(stu, selectedDate).arabicLabel
        DigitalStudentIdDialog(
            student = stu,
            schoolConfig = schoolConfig,
            badges = badges,
            todayAttendanceLabel = todayAttLabel,
            todayIso = selectedDate,
            toastAlert = toastAlert,
            onSavePng = {
                val res = DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = badges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم حفظ الهوية المدرسية بدقة عالية (PNG) في المعرض جاهزة للطباعة أو الكبس الحراري",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر حفظ الصورة: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = badges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم تصدير وفتح نافذة طباعة الهوية المدرسية بصيغة PDF بنجاح",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر تصدير PDF: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { selectedStudentForIdCard = null }
        )
    }
}

private fun formatBusTimestampPretty(raw: String): String {
    val trimmed = raw.trim()
    if (trimmed.isBlank()) return "تحديث مباشر الآن"
    return Regex("""(\d{4})[-/]?(\d{2})[-/]?(\d{2})""").replace(trimmed) { match ->
        "${match.groupValues[1]}/${match.groupValues[2]}/${match.groupValues[3]}"
    }
}

@Composable
private fun ParentStudentLiveBusAlertBannerCard(
    studentName: String,
    routeName: String,
    activeBusEventName: String,
    activeBusTimestamp: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val resolvedEvent = remember(activeBusEventName) {
        BusEventType.entries.firstOrNull { it.name == activeBusEventName } ?: BusEventType.MORNING_DEPARTURE
    }
    val formattedStamp = remember(activeBusTimestamp) {
        formatBusTimestampPretty(activeBusTimestamp)
    }
    val infiniteTransition = rememberInfiniteTransition(label = "live_bus_alert_banner_anim")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bus_banner_wave_phase"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bus_banner_pulse_glow"
    )

    val alertHeadline = when (resolvedEvent) {
        BusEventType.MORNING_DEPARTURE -> "تنبيه النقل المدرسي: انطلق خط الطالب ($studentName) الآن باتجاه المدرسة"
        BusEventType.ARRIVED_AT_SCHOOL -> "تنبيه النقل المدرسي: وصل خط الطالب ($studentName) بسلام إلى بوابة المدرسة"
        BusEventType.RETURN_HOME_DEPARTURE -> "تنبيه النقل المدرسي: انطلق خط الطالب ($studentName) الآن في رحلة العودة للمنزل"
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .testTag("dashboard_parent_live_bus_alert_banner"),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF064E3B),
        border = BorderStroke(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.78f)),
        shadowElevation = 6.dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Animated Bezier Waves & Glowing Telemetry Stream Background
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.70f)
                    cubicTo(
                        w * 0.30f, h * (0.50f + sin(wavePhase) * 0.12f),
                        w * 0.70f, h * (0.88f - cos(wavePhase) * 0.12f),
                        w, h * 0.64f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wavePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.24f),
                            Color(0xFFFBBF24).copy(alpha = 0.18f),
                            Color(0xFF047857).copy(alpha = 0.28f)
                        )
                    )
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Mini 3D Bezier Bus Badge with Pulsing Aura
                Box(
                    modifier = Modifier.size(46.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width * 0.5f
                        val cy = size.height * 0.5f
                        drawCircle(
                            color = Color(0xFFFDE68A).copy(alpha = pulseGlow * 0.35f),
                            radius = size.minDimension * 0.48f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF047857))
                            ),
                            radius = size.minDimension * 0.40f,
                            center = Offset(cx, cy)
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = Color(0xFFFDE68A),
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = alertHeadline,
                        style = MaterialTheme.typography.labelLarge,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "${routeName.ifBlank { "الخط المدرسي المعتمد" }} • آخر تحديث: $formattedStamp • اضغط لمتابعة المسار",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFA7F3D0)
                    )
                }

                Icon(
                    imageVector = Icons.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun InteractiveParentDismissalPickupCard(
    student: StudentRecord,
    activeCall: DismissalCallItem?,
    isHoliday: Boolean,
    holidayLabel: String,
    onTriggerCall: () -> Unit,
    onHolidayLockedClick: () -> Unit,
    buttonTestTag: String,
    modifier: Modifier = Modifier
) {
    var localPickupTriggered by remember(student.id, isHoliday) { mutableStateOf(false) }
    val isPickupRadarOpen = !isHoliday && (activeCall != null || localPickupTriggered)

    var elapsedSeconds by remember(isPickupRadarOpen) { mutableIntStateOf(0) }
    LaunchedEffect(isPickupRadarOpen) {
        if (isPickupRadarOpen) {
            while (true) {
                kotlinx.coroutines.delay(1000L)
                elapsedSeconds += 1
            }
        } else {
            elapsedSeconds = 0
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pickup_radar_3d_car_anim")
    val sonarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sonar_wave_progress"
    )
    val carFloatY by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "car_float_y"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = when {
            isHoliday -> Color(0xFFF8FAFC)
            isPickupRadarOpen -> Color(0xFF042F24)
            else -> AdaptiveSoftSurface
        },
        border = BorderStroke(
            1.3.dp,
            when {
                isHoliday -> Color(0xFFCBD5E1)
                isPickupRadarOpen -> Color(0xFFFBBF24)
                else -> DeepEmerald.copy(alpha = 0.45f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (isHoliday) {
                // 1) Locked State on Official Holidays & Fridays
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFE2E8F0),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.LockClock,
                                contentDescription = null,
                                tint = Color(0xFF475569),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "الاستدعاء السريع متوقف اليوم (عطلة رسمية)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "اليوم مصنف كـ ($holidayLabel) — يعمل الاستدعاء السريع فقط خلال أيام وأوقات الدوام الرسمي.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    onClick = onHolidayLockedClick,
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFE2E8F0),
                    border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag(buttonTestTag)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "الاستدعاء السريع متوقف اليوم (عطلة رسمية)",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF334155),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            } else {
                // 2) Active School Day: Interactive Pickup Radar & 3D Car Bezier Animation when triggered
                if (isPickupRadarOpen) {
                    val mins = elapsedSeconds / 60
                    val secs = elapsedSeconds % 60
                    val timerFormatted = String.format(java.util.Locale.US, "%02d:%02d", mins, secs)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Radar,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "رادار الاستلام الذكي أمام البوابة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFDE68A).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Text(
                                text = "الوقت المنقضي: $timerFormatted",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE68A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Interactive Pickup Radar & 3D Car Bezier Scene Canvas
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF064E3B), Color(0xFF022C22))
                                )
                            )
                            .testTag("interactive_pickup_radar_3d_car_scene")
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Bezier curved ground track connecting Car (right) to Emerald School Gate (left)
                            val trackPath = Path().apply {
                                moveTo(w * 0.08f, h * 0.78f)
                                cubicTo(
                                    w * 0.35f, h * 0.68f,
                                    w * 0.65f, h * 0.86f,
                                    w * 0.92f, h * 0.76f
                                )
                            }
                            drawPath(
                                path = trackPath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF34D399), Color(0xFFFDE68A), Color(0xFF10B981))
                                ),
                                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                            )

                            // Left: Emerald School Gate Arch (Bezier architectural arch)
                            val gateX = w * 0.20f
                            val gateBaseY = h * 0.78f
                            val gateArchPath = Path().apply {
                                moveTo(gateX - 28.dp.toPx(), gateBaseY)
                                lineTo(gateX - 28.dp.toPx(), gateBaseY - 36.dp.toPx())
                                cubicTo(
                                    gateX - 28.dp.toPx(), gateBaseY - 62.dp.toPx(),
                                    gateX + 28.dp.toPx(), gateBaseY - 62.dp.toPx(),
                                    gateX + 28.dp.toPx(), gateBaseY - 36.dp.toPx()
                                )
                                lineTo(gateX + 28.dp.toPx(), gateBaseY)
                                close()
                            }
                            drawPath(
                                path = gateArchPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFF10B981), Color(0xFF065F46))
                                )
                            )
                            drawPath(
                                path = gateArchPath,
                                color = Color(0xFFFDE68A),
                                style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round)
                            )
                            // Inner golden gate portal
                            val innerPortalPath = Path().apply {
                                moveTo(gateX - 15.dp.toPx(), gateBaseY)
                                lineTo(gateX - 15.dp.toPx(), gateBaseY - 26.dp.toPx())
                                cubicTo(
                                    gateX - 15.dp.toPx(), gateBaseY - 42.dp.toPx(),
                                    gateX + 15.dp.toPx(), gateBaseY - 42.dp.toPx(),
                                    gateX + 15.dp.toPx(), gateBaseY - 26.dp.toPx()
                                )
                                lineTo(gateX + 15.dp.toPx(), gateBaseY)
                                close()
                            }
                            drawPath(
                                path = innerPortalPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFDE68A).copy(alpha = 0.85f), Color(0xFFD97706).copy(alpha = 0.45f))
                                )
                            )

                            // Consecutive Bezier Sonar Waves radiating from the 3D Car toward the School Gate
                            val carX = w * 0.74f
                            val carY = h * 0.72f + carFloatY
                            for (waveIdx in 0..2) {
                                val phase = ((sonarProgress + waveIdx * 0.33f) % 1f)
                                val waveX = carX - 36.dp.toPx() - phase * (carX - gateX - 52.dp.toPx())
                                val waveSpread = (14f + phase * 26f).dp.toPx()
                                val waveAlpha = (1f - phase).coerceIn(0f, 1f)
                                val sonarArc = Path().apply {
                                    moveTo(waveX, carY - waveSpread - 8.dp.toPx())
                                    cubicTo(
                                        waveX - 14.dp.toPx(), carY - waveSpread * 0.35f - 8.dp.toPx(),
                                        waveX - 14.dp.toPx(), carY + waveSpread * 0.35f - 8.dp.toPx(),
                                        waveX, carY + waveSpread - 8.dp.toPx()
                                    )
                                }
                                drawPath(
                                    path = sonarArc,
                                    color = if (waveIdx % 2 == 0) {
                                        Color(0xFFFDE68A).copy(alpha = waveAlpha * 0.90f)
                                    } else {
                                        Color(0xFF34D399).copy(alpha = waveAlpha * 0.90f)
                                    },
                                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }

                            // Right: Elegant 3D Vector Car (Path.cubicTo) stopped in front of the gate
                            val cw = 68.dp.toPx()
                            val ch = 26.dp.toPx()
                            val carBodyPath = Path().apply {
                                moveTo(carX - cw * 0.50f, carY)
                                cubicTo(
                                    carX - cw * 0.52f, carY - ch * 0.48f,
                                    carX - cw * 0.38f, carY - ch * 0.55f,
                                    carX - cw * 0.24f, carY - ch * 0.58f
                                )
                                // Roofline curve
                                cubicTo(
                                    carX - cw * 0.12f, carY - ch * 1.12f,
                                    carX + cw * 0.22f, carY - ch * 1.12f,
                                    carX + cw * 0.34f, carY - ch * 0.56f
                                )
                                cubicTo(
                                    carX + cw * 0.48f, carY - ch * 0.50f,
                                    carX + cw * 0.52f, carY - ch * 0.20f,
                                    carX + cw * 0.48f, carY
                                )
                                close()
                            }
                            drawPath(
                                path = carBodyPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B)),
                                    start = Offset(carX - cw * 0.5f, carY - ch),
                                    end = Offset(carX + cw * 0.5f, carY)
                                )
                            )
                            drawPath(
                                path = carBodyPath,
                                color = Color(0xFFFDE68A),
                                style = Stroke(width = 1.5.dp.toPx(), join = StrokeJoin.Round)
                            )

                            // Car glass cabin window
                            val cabinGlassPath = Path().apply {
                                moveTo(carX - cw * 0.18f, carY - ch * 0.56f)
                                cubicTo(
                                    carX - cw * 0.08f, carY - ch * 0.96f,
                                    carX + cw * 0.18f, carY - ch * 0.96f,
                                    carX + cw * 0.28f, carY - ch * 0.56f
                                )
                                close()
                            }
                            drawPath(
                                path = cabinGlassPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFE0F2FE), Color(0xFF6EE7B7))
                                )
                            )

                            // Headlight beam toward the gate
                            val beamPath = Path().apply {
                                moveTo(carX - cw * 0.50f, carY - ch * 0.32f)
                                lineTo(carX - cw * 0.85f, carY - ch * 0.65f)
                                lineTo(carX - cw * 0.85f, carY - ch * 0.02f)
                                close()
                            }
                            drawPath(
                                path = beamPath,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Color(0xFFFEF08A).copy(alpha = 0.55f))
                                )
                            )

                            // 3D Wheels with Golden Rims
                            listOf(carX - cw * 0.28f, carX + cw * 0.26f).forEach { wx ->
                                drawCircle(
                                    color = Color(0xFF0F172A),
                                    radius = 6.5.dp.toPx(),
                                    center = Offset(wx, carY)
                                )
                                drawCircle(
                                    color = Color(0xFFFBBF24),
                                    radius = 3.dp.toPx(),
                                    center = Offset(wx, carY)
                                )
                            }
                        }
                    }

                    Text(
                        text = "تم إبلاغ الإدارة والمناوب: جاري تجهيز الطالب (${student.fullNameWithFather}) للخروج للبوابة...",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFDE68A),
                        lineHeight = 18.sp
                    )
                } else {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "الاستدعاء السريع عند الانصراف",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "اضغط فور وصولك أمام بوابة المدرسة لإرسال نداء صوتي وضوئي للإدارة لتجهيز خروج (${student.fullNameWithFather})",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepEmerald
                        )
                    }
                }

                Button(
                    onClick = {
                        localPickupTriggered = true
                        onTriggerCall()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPickupRadarOpen) Color(0xFF059669) else DeepEmerald,
                        contentColor = Color.White
                    ),
                    border = if (isPickupRadarOpen) BorderStroke(1.2.dp, Color(0xFFFDE68A)) else null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .testTag(buttonTestTag)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DirectionsCar,
                        contentDescription = null,
                        tint = if (isPickupRadarOpen) Color(0xFFFDE68A) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isPickupRadarOpen) {
                            "إعادة إرسال نبضة تنبيه للإدارة والمناوب"
                        } else {
                            "الاستدعاء السريع: أنا أمام المدرسة لاستلام ابني"
                        },
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun SchoolBusQuickAlertsCard(
    busSubscribersCount: Int,
    activeBusEventName: String = "",
    activeBusEventTimestamp: String = "",
    canControlTrip: Boolean = true,
    onSendBusAlert: (BusEventType) -> Unit,
    onOpenTransportSection: () -> Unit = {}
) {
    val resolvedBusEvent = remember(activeBusEventName) {
        BusEventType.entries.firstOrNull { it.name == activeBusEventName } ?: BusEventType.MORNING_DEPARTURE
    }
    var activeBusState by remember(resolvedBusEvent) { mutableStateOf(resolvedBusEvent) }

    val activeStationIndex = when (activeBusState) {
        BusEventType.MORNING_DEPARTURE -> 1
        BusEventType.ARRIVED_AT_SCHOOL -> 2
        BusEventType.RETURN_HOME_DEPARTURE -> 0
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("school_bus_quick_alerts_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header Row with Open Full Section Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF065F46),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsBus,
                                contentDescription = null,
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "المسار التفاعلي المباشر لحركة الخط",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "الطلبة المشمولون بالخط حالياً: $busSubscribersCount طالب",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF047857),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    onClick = onOpenTransportSection,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFF059669)),
                    modifier = Modifier.testTag("open_full_school_transport_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "قسم النقل الكامل",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            maxLines = 1,
                            softWrap = false
                        )
                        Icon(
                            imageVector = Icons.Filled.ArrowForward,
                            contentDescription = null,
                            tint = Color(0xFF065F46),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Full-Width Panoramic Bezier Wave Road Track with 3D Bus
            FullWidthBezierWaveRoadTrack(
                activeBusState = activeBusState,
                activeStationIndex = activeStationIndex,
                lastUpdatedTimestamp = formatBusTimestampPretty(activeBusEventTimestamp)
            )

            // 3 Full-Width / Unclipped Trip Control Action Buttons for Principal & Supervisor
            if (canControlTrip) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val controlButtons = listOf(
                        Triple(BusEventType.MORNING_DEPARTURE, "بدء الانطلاق الصباحي", Icons.Filled.WbSunny),
                        Triple(BusEventType.ARRIVED_AT_SCHOOL, "تأكيد الوصول للمدرسة", Icons.Filled.Verified),
                        Triple(BusEventType.RETURN_HOME_DEPARTURE, "انطلاق رحلة العودة للمنازل", Icons.Filled.Home)
                    )
                    controlButtons.forEach { (busEvent, fullButtonTitle, btnIcon) ->
                        val isSelected = activeBusState == busEvent
                        Button(
                            onClick = {
                                activeBusState = busEvent
                                onSendBusAlert(busEvent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 46.dp)
                                .testTag("bus_alert_btn_${busEvent.name}"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF047857) else Color(0xFF064E3B),
                                contentColor = Color.White
                            ),
                            border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFFDE68A)) else null
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
                                        imageVector = btnIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color(0xFFFDE68A) else Color(0xFF6EE7B7),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = fullButtonTitle,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (isSelected) {
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFFDE68A).copy(alpha = 0.22f),
                                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                    ) {
                                        Text(
                                            text = "نشط الآن",
                                            fontSize = 10.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFFFDE68A),
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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
    }
}

/**
 * مشهد مسار بانورامي متموج بكامل عرض الشاشة (Full-Width Bezier Wave Road Track)
 * تتحرك عليه مركبة/باص 3D فيكتوري بفيزياء ناعمة بين المحطات الثلاث:
 * (منزل الطالب ⬅️ في الطريق السريع ⬅️ بوابة المدرسة)
 */
@Composable
private fun FullWidthBezierWaveRoadTrack(
    activeBusState: BusEventType,
    activeStationIndex: Int,
    lastUpdatedTimestamp: String
) {
    // Target normalized X along the panoramic Bezier track:
    // Station 0 (منزل الطالب - Right): 0.82f
    // Station 1 (في الطريق السريع - Center): 0.50f
    // Station 2 (بوابة المدرسة - Left): 0.18f
    val targetT = when (activeStationIndex) {
        0 -> 0.82f
        1 -> 0.50f
        else -> 0.18f
    }
    val animatedBusT by animateFloatAsState(
        targetValue = targetT,
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
        label = "panoramic_bus_position_t"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "bezier_wave_road_track_anim")
    val busSuspensionFloat by infiniteTransition.animateFloat(
        initialValue = -2.8f,
        targetValue = 2.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bus_suspension_float"
    )
    val wheelSpinDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bus_wheel_spin_deg"
    )
    val stationPulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "station_node_pulse"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF042F24),
                        Color(0xFF022C22)
                    )
                )
            )
            .padding(vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Top Status Bar with Formatted Slash Date (2026/11/13)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFBBF24),
                    modifier = Modifier.size(10.dp)
                ) {}
                Text(
                    text = when (activeBusState) {
                        BusEventType.MORNING_DEPARTURE -> "حالة الخط الآن: انطلاق صباحي في الطريق السريع نحو المدرسة"
                        BusEventType.ARRIVED_AT_SCHOOL -> "حالة الخط الآن: وصل الباص بأمان إلى بوابة المدرسة ✓"
                        BusEventType.RETURN_HOME_DEPARTURE -> "حالة الخط الآن: انطلاق رحلة العودة نحو منزل الطالب"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }

            Surface(
                shape = RoundedCornerShape(50),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.55f))
            ) {
                Text(
                    text = formatBusTimestampPretty(lastUpdatedTimestamp),
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFDE68A),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }

        // Full-Width Panoramic Bezier Wave Road & 3D Vector Bus Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height

                // Helper function computing Y on the smooth sinusoidal Bezier wave road for any normalized X in [0, 1]
                fun roadYAt(normX: Float): Float {
                    val baseLineY = h * 0.64f
                    val waveAmp = h * 0.11f
                    return baseLineY + sin(normX * Math.PI.toFloat() * 2.0f) * waveAmp
                }

                // 1) Panoramic Wavy Road Ribbon (Full Width 0f..w)
                val roadRibbonPath = Path().apply {
                    moveTo(0f, roadYAt(0f))
                    val steps = 36
                    for (i in 1..steps) {
                        val nx = i.toFloat() / steps.toFloat()
                        lineTo(w * nx, roadYAt(nx))
                    }
                }
                // Outer glowing emerald-gold road bed
                drawPath(
                    path = roadRibbonPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.35f),
                            Color(0xFF059669).copy(alpha = 0.55f),
                            Color(0xFF10B981).copy(alpha = 0.35f)
                        )
                    ),
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Inner dark asphalt track
                drawPath(
                    path = roadRibbonPath,
                    color = Color(0xFF0F172A).copy(alpha = 0.85f),
                    style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Center golden neon guide line
                drawPath(
                    path = roadRibbonPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF34D399), Color(0xFFFDE68A), Color(0xFF34D399))
                    ),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // 2) Three Station Anchors on the Wavy Track:
                // idx 0 = منزل الطالب (0.82f), idx 1 = في الطريق السريع (0.50f), idx 2 = بوابة المدرسة (0.18f)
                val stationNormXs = listOf(0.82f, 0.50f, 0.18f)
                stationNormXs.forEachIndexed { idx, nx ->
                    val sx = w * nx
                    val sy = roadYAt(nx)
                    val isCurrent = idx == activeStationIndex

                    if (isCurrent) {
                        drawCircle(
                            color = Color(0xFFFDE68A).copy(alpha = (1f - stationPulse) * 0.55f),
                            radius = 22.dp.toPx() * (0.55f + stationPulse * 0.65f),
                            center = Offset(sx, sy)
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isCurrent) {
                                listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
                            } else {
                                listOf(Color(0xFFA7F3D0), Color(0xFF10B981), Color(0xFF064E3B))
                            },
                            center = Offset(sx, sy),
                            radius = 11.dp.toPx()
                        ),
                        radius = if (isCurrent) 9.5.dp.toPx() else 7.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.dp.toPx(),
                        center = Offset(sx, sy)
                    )
                }

                // 3) 3D Vector School Bus Gliding Smoothly Along the Bezier Wave Track
                val busX = w * animatedBusT
                val busRoadY = roadYAt(animatedBusT)
                val busY = busRoadY - 12.dp.toPx() + busSuspensionFloat
                val bw = 66.dp.toPx()
                val bh = 30.dp.toPx()
                val facingRight = activeBusState == BusEventType.RETURN_HOME_DEPARTURE

                // Ground shadow under the 3D Bus
                drawOval(
                    color = Color.Black.copy(alpha = 0.35f),
                    topLeft = Offset(busX - bw * 0.46f, busRoadY - 3.dp.toPx()),
                    size = Size(bw * 0.92f, 7.dp.toPx())
                )

                // Headlight volumetric cone
                val beamStartX = if (facingRight) busX + bw * 0.48f else busX - bw * 0.48f
                val beamEndX = if (facingRight) busX + bw * 0.92f else busX - bw * 0.92f
                val headlightCone = Path().apply {
                    moveTo(beamStartX, busY - bh * 0.28f)
                    lineTo(beamEndX, busY - bh * 0.62f)
                    lineTo(beamEndX, busY + bh * 0.05f)
                    close()
                }
                drawPath(
                    path = headlightCone,
                    brush = Brush.horizontalGradient(
                        colors = if (facingRight) {
                            listOf(Color(0xFFFEF08A).copy(alpha = 0.55f), Color.Transparent)
                        } else {
                            listOf(Color.Transparent, Color(0xFFFEF08A).copy(alpha = 0.55f))
                        },
                        startX = minOf(beamStartX, beamEndX),
                        endX = maxOf(beamStartX, beamEndX)
                    )
                )

                // 3D Bus Upper Roof Deck (Isometric depth)
                val roofPath = Path().apply {
                    moveTo(busX - bw * 0.42f, busY - bh * 0.92f)
                    cubicTo(
                        busX - bw * 0.20f, busY - bh * 1.12f,
                        busX + bw * 0.20f, busY - bh * 1.12f,
                        busX + bw * 0.42f, busY - bh * 0.92f
                    )
                    lineTo(busX + bw * 0.46f, busY - bh * 0.78f)
                    lineTo(busX - bw * 0.46f, busY - bh * 0.78f)
                    close()
                }
                drawPath(
                    path = roofPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B))
                    )
                )

                // 3D Sculpted Bus Main Body (Bezier Aerodynamic Coach)
                val busHullPath = Path().apply {
                    moveTo(busX - bw * 0.48f, busY)
                    cubicTo(
                        busX - bw * 0.52f, busY - bh * 0.45f,
                        busX - bw * 0.48f, busY - bh * 0.85f,
                        busX - bw * 0.38f, busY - bh * 0.90f
                    )
                    lineTo(busX + bw * 0.38f, busY - bh * 0.90f)
                    cubicTo(
                        busX + bw * 0.48f, busY - bh * 0.85f,
                        busX + bw * 0.52f, busY - bh * 0.45f,
                        busX + bw * 0.48f, busY
                    )
                    close()
                }
                drawPath(
                    path = busHullPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF064E3B)),
                        startY = busY - bh,
                        endY = busY
                    )
                )
                drawPath(
                    path = busHullPath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFFDE68A), Color(0xFF6EE7B7), Color(0xFFFBBF24))
                    ),
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )

                // Golden Royal Belt Stripe along the Bus Body
                val beltPath = Path().apply {
                    moveTo(busX - bw * 0.48f, busY - bh * 0.34f)
                    quadraticBezierTo(
                        busX, busY - bh * 0.40f,
                        busX + bw * 0.48f, busY - bh * 0.34f
                    )
                }
                drawPath(
                    path = beltPath,
                    color = Color(0xFFFBBF24),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
                )

                // Panoramic Glass Windows Row
                for (winIdx in -1..1) {
                    val wx = busX + winIdx * (bw * 0.25f)
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE0F2FE), Color(0xFF6EE7B7))
                        ),
                        topLeft = Offset(wx - bw * 0.09f, busY - bh * 0.76f),
                        size = Size(bw * 0.18f, bh * 0.32f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                }

                // Rotating 3D Wheels with Spokes
                listOf(busX - bw * 0.28f, busX + bw * 0.28f).forEach { wx ->
                    val wheelCenter = Offset(wx, busY + 1.dp.toPx())
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = 6.5.dp.toPx(),
                        center = wheelCenter
                    )
                    rotate(degrees = wheelSpinDeg, pivot = wheelCenter) {
                        drawLine(
                            color = Color(0xFFFDE68A),
                            start = Offset(wheelCenter.x - 4.dp.toPx(), wheelCenter.y),
                            end = Offset(wheelCenter.x + 4.dp.toPx(), wheelCenter.y),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFFFDE68A),
                            start = Offset(wheelCenter.x, wheelCenter.y - 4.dp.toPx()),
                            end = Offset(wheelCenter.x, wheelCenter.y + 4.dp.toPx()),
                            strokeWidth = 1.5.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }

        // 3 Pure Arabic Station Labels: (منزل الطالب ⬅️ في الطريق السريع ⬅️ بوابة المدرسة)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val stationsLabels = listOf(
                0 to "منزل الطالب",
                1 to "في الطريق السريع",
                2 to "بوابة المدرسة"
            )
            stationsLabels.forEach { (idx, label) ->
                val isCurrent = idx == activeStationIndex
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCurrent) Color(0xFFFBBF24).copy(alpha = 0.20f) else Color.White.copy(alpha = 0.08f),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrent) Color(0xFFFDE68A) else Color(0xFF6EE7B7).copy(alpha = 0.30f)
                    )
                ) {
                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.SemiBold,
                        color = if (isCurrent) Color(0xFFFDE68A) else Color(0xFFD1FAE5),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaveRequestCardItem(
    req: LeaveRequestItem,
    isAdmin: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${req.studentName} • ${req.gradeClass} (${req.sectionName})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${req.leaveType.arabicLabel} • التاريخ: ${req.leaveDate} (${req.durationDays} يوم)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                NoWrapBadge(
                    text = req.status.arabicLabel,
                    icon = when (req.status) {
                        LeaveRequestStatus.APPROVED -> Icons.Filled.CheckCircle
                        LeaveRequestStatus.REJECTED -> Icons.Filled.Cancel
                        LeaveRequestStatus.PENDING -> Icons.Filled.HourglassTop
                    },
                    containerColor = when (req.status) {
                        LeaveRequestStatus.APPROVED -> SuccessGreen.copy(alpha = 0.16f)
                        LeaveRequestStatus.REJECTED -> DangerRed.copy(alpha = 0.16f)
                        LeaveRequestStatus.PENDING -> WarningAmber.copy(alpha = 0.18f)
                    },
                    contentColor = when (req.status) {
                        LeaveRequestStatus.APPROVED -> SuccessGreen
                        LeaveRequestStatus.REJECTED -> DangerRed
                        LeaveRequestStatus.PENDING -> WarningAmber
                    }
                )
            }

            Text(
                text = "السبب: ${req.reason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isAdmin && req.status == LeaveRequestStatus.PENDING) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("approve_leave_btn_${req.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("موافقة (تسجيل مجاز بعذر)", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onReject,
                        modifier = Modifier
                            .weight(0.7f)
                            .testTag("reject_leave_btn_${req.id}"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("رفض", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmitLeaveRequestDialog(
    student: StudentRecord,
    defaultDate: String,
    onSubmit: (LeaveType, String, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf(LeaveType.SICK) }
    var leaveDate by remember { mutableStateOf(defaultDate) }
    var durationStr by remember { mutableStateOf("1") }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "طلب إجازة إلكتروني للطالب: ${student.fullNameWithFather}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "نوع الإجازة المطلوبة:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    LeaveType.entries.forEach { lt ->
                        FilterChip(
                            selected = selectedType == lt,
                            onClick = { selectedType = lt },
                            label = { Text(lt.arabicLabel, maxLines = 1) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ModernCalendarDatePickerField(
                        selectedDateIso = leaveDate,
                        label = "تاريخ الإجازة",
                        onDateSelected = { selectedIso, _ -> leaveDate = selectedIso },
                        modifier = Modifier.weight(1.3f)
                    )
                    OutlinedTextField(
                        value = durationStr,
                        onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("المدة (أيام)") },
                        singleLine = true,
                        modifier = Modifier.weight(0.7f)
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("سبب الإجازة والتفاصيل لمدير المدرسة") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("leave_request_reason_input"),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val days = durationStr.toIntOrNull()?.coerceIn(1, 30) ?: 1
                    onSubmit(selectedType, leaveDate, days, reason)
                },
                enabled = reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                modifier = Modifier.testTag("confirm_submit_leave_request_btn")
            ) {
                Text("إرسال الطلب للمدير", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

@Composable
private fun LostAndFoundRegistryDialog(
    items: List<LostAndFoundItem>,
    canPublish: Boolean,
    onPublishItem: (String, String, String) -> Unit,
    onUpdateItem: (String, String, String, String) -> Unit,
    onDeleteItem: (String) -> Unit,
    onToggleStatus: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var foundLocation by remember { mutableStateOf("") }
    var photoBase64 by remember { mutableStateOf("") }
    var editingItem by remember { mutableStateOf<LostAndFoundItem?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Filled.Inventory2, contentDescription = null, tint = DeepEmerald)
                Text(
                    text = "سجل المفقودات والأمانات المدرسية",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (canPublish) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DeepEmerald.copy(alpha = 0.08f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "إضافة غرض مفقود تم العثور عليه في المدرسة:",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepEmerald
                            )
                            OutlinedTextField(
                                value = title,
                                onValueChange = { title = it },
                                label = { Text("وصف الغرض (مثلاً: سترة شتوية، نظارة، حقيبة)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = foundLocation,
                                onValueChange = { foundLocation = it },
                                label = { Text("مكان الاستلام / العثور عليه (مثلاً: لدى الإدارة)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CompressedPhotoPickerButton(
                                    label = if (photoBase64.isEmpty()) "إرفاق صورة الغرض" else "تغيير الصورة",
                                    hasImage = photoBase64.isNotEmpty(),
                                    onPhotoSelected = { photoBase64 = it },
                                    modifier = Modifier.weight(1f),
                                    isAvatar = false
                                )
                                Button(
                                    onClick = {
                                        onPublishItem(title, foundLocation, photoBase64)
                                        title = ""
                                        foundLocation = ""
                                        photoBase64 = ""
                                    },
                                    enabled = title.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                                    modifier = Modifier.testTag("publish_lost_found_btn")
                                ) {
                                    Text("نشر الغرض", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                if (items.isEmpty()) {
                    Text(
                        text = "لا توجد مفقودات مسجلة حالياً في المدرسة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    items.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.16f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (item.photoBase64.isNotBlank()) {
                                        Base64AvatarImage(
                                            base64Data = item.photoBase64,
                                            size = 48.dp,
                                            fallbackIcon = Icons.Filled.Inventory2,
                                            contentDescription = item.title
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "مكان الاستلام: ${item.foundLocation} • التاريخ: ${item.date}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = if (item.isClaimed) "تم تسليمه لصاحبه" else "موجود لدى الإدارة",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (item.isClaimed) SuccessGreen else DeepEmerald,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    if (canPublish) {
                                        TextButton(onClick = { onToggleStatus(item.id) }) {
                                            Text(
                                                text = if (item.isClaimed) "إعادة كموجود" else "تم التسليم",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                if (canPublish) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingItem = item },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("edit_lost_found_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "تعديل الغرض",
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تعديل",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepEmerald
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { onDeleteItem(item.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .testTag("delete_lost_found_${item.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "حذف الغرض",
                                                tint = DangerRed,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "حذف",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DangerRed
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", fontWeight = FontWeight.Bold)
            }
        }
    )

    editingItem?.let { target ->
        var editTitle by remember(target.id) { mutableStateOf(target.title) }
        var editLocation by remember(target.id) { mutableStateOf(target.foundLocation) }
        var editPhoto by remember(target.id) { mutableStateOf(target.photoBase64) }

        AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = "تعديل بيانات الغرض في سجل المفقودات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("وصف الغرض") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("مكان الاستلام / التواجد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompressedPhotoPickerButton(
                        label = if (editPhoto.isEmpty()) "إرفاق صورة الغرض" else "تغيير صورة الغرض",
                        hasImage = editPhoto.isNotEmpty(),
                        onPhotoSelected = { editPhoto = it },
                        modifier = Modifier.fillMaxWidth(),
                        isAvatar = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateItem(target.id, editTitle, editLocation, editPhoto)
                        editingItem = null
                    },
                    enabled = editTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
                ) {
                    Text("حفظ التعديل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingItem = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactDailyBehaviorCard(
    title: String,
    studentName: String,
    behaviors: List<DailyBehaviorLogItem>,
    modifier: Modifier = Modifier
) {
    var showFullLogModal by remember { mutableStateOf(false) }
    val sortedBehaviors = remember(behaviors) {
        behaviors.sortedByDescending { it.date }
    }
    val topThree = remember(sortedBehaviors) {
        sortedBehaviors.take(3)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = ClipboardCheckVectorIcon,
                        contentDescription = "دفتر المتابعة اليومي والسلوك",
                        tint = DeepEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DeepEmerald.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "آخر ${topThree.size} من ${sortedBehaviors.size}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            topThree.forEach { beh ->
                val cleanNote = beh.note.trim()
                val hasDistinctNote = cleanNote.isNotEmpty()
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF0FDF4),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (beh.behaviorType.isPositive) ClipboardCheckVectorIcon else Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = Color(0xFF059669),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "${beh.behaviorType.arabicTitle} • ${beh.subjectName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                            }
                            Text(
                                text = beh.date,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF059669)
                            )
                        }

                        if (hasDistinctNote) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1FAE5)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = cleanNote,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1E293B),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                                )
                            }
                        }

                        if (beh.teacherName.isNotBlank()) {
                            Text(
                                text = "بواسطة: ${beh.teacherName}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen
                            )
                        }
                    }
                }
            }

            if (sortedBehaviors.size > 3) {
                OutlinedButton(
                    onClick = { showFullLogModal = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("open_full_behavior_log_btn"),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald)
                ) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "عرض السجل الكامل للمتابعة والسلوك (إجمالي ${sortedBehaviors.size} ملاحظة)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }

    if (showFullLogModal) {
        DailyBehaviorFullLogModal(
            studentName = studentName,
            behaviors = sortedBehaviors,
            onDismiss = { showFullLogModal = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyBehaviorFullLogModal(
    studentName: String,
    behaviors: List<DailyBehaviorLogItem>,
    onDismiss: () -> Unit
) {
    var selectedTypeFilter by remember { mutableStateOf<DailyBehaviorType?>(null) }
    var selectedSubjectFilter by remember { mutableStateOf("الكل") }

    val availableSubjects = remember(behaviors) {
        listOf("الكل") + behaviors.map { it.subjectName.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    val filteredBehaviors = remember(behaviors, selectedTypeFilter, selectedSubjectFilter) {
        behaviors
            .sortedByDescending { it.date }
            .filter { item ->
                val matchesType = selectedTypeFilter == null || item.behaviorType == selectedTypeFilter
                val matchesSubject = selectedSubjectFilter == "الكل" || item.subjectName.trim() == selectedSubjectFilter
                matchesType && matchesSubject
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = ClipboardCheckVectorIcon,
                        contentDescription = null,
                        tint = DeepEmerald
                    )
                    Text(
                        text = "السجل الكامل للمتابعة والسلوك",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }
                Text(
                    text = "الطالب: $studentName • إجمالي الملاحظات المطابقة: ${filteredBehaviors.size} من ${behaviors.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "فلترة حسب نوع الملاحظة:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = DeepForestGreen
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedTypeFilter == null,
                            onClick = { selectedTypeFilter = null },
                            label = { Text("الكل (${behaviors.size})") }
                        )
                    }
                    items(DailyBehaviorType.entries) { type ->
                        val count = behaviors.count { it.behaviorType == type }
                        FilterChip(
                            selected = selectedTypeFilter == type,
                            onClick = {
                                selectedTypeFilter = if (selectedTypeFilter == type) null else type
                            },
                            label = { Text("${type.arabicTitle} ($count)") }
                        )
                    }
                }

                if (availableSubjects.size > 2) {
                    Text(
                        text = "فلترة حسب المادة الدراسية:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = DeepForestGreen
                    )
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableSubjects) { subj ->
                            FilterChip(
                                selected = selectedSubjectFilter == subj,
                                onClick = { selectedSubjectFilter = subj },
                                label = { Text(subj) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.15f))

                if (filteredBehaviors.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا توجد ملاحظات مطابقة للفلتر المحدد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedGreen
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredBehaviors, key = { it.id }) { beh ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${beh.behaviorType.arabicTitle} • ${beh.subjectName}",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFD1FAE5)
                                        ) {
                                            Text(
                                                text = beh.date,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    if (beh.note.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1FAE5)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = beh.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E293B),
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = "بواسطة: ${beh.teacherName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedGreen
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
            ) {
                Text("إغلاق السجل", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * Deliverable 4b: Indexed Leave Requests Modal Dialog (نافذة طلبات الإجازة الإلكترونية المفهرسة)
 * - Accessible directly from the Side Navigation Drawer for Principal, Teacher, Parent, and Student.
 * - Includes InlineDialogToastBanner so action feedback is visible above the modal.
 * - Tabs: المعلقة قيد المراجعة | المقبولة | المرفوضة | الكل
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun IndexedLeaveRequestsModalDialog(
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val canManage = isAdmin || isTeacher
    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT

    val scopedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser)
    }
    val scopedLeaveRequests = remember(leaveRequests, isAdmin, scopedStudents) {
        if (isAdmin) {
            leaveRequests
        } else {
            val allowedIds = scopedStudents.map { it.id.trim() }.toSet()
            leaveRequests.filter { it.studentId.trim() in allowedIds }
        }
    }

    var selectedStatusTab by remember { mutableStateOf(0) } // 0 = Pending, 1 = Approved, 2 = Rejected, 3 = All
    var searchQuery by remember { mutableStateOf("") }
    var selectedClassFilter by remember { mutableStateOf("الكل") }
    var showNewRequestSheet by remember { mutableStateOf(false) }

    val availableClasses = remember(scopedLeaveRequests, scopedStudents) {
        (listOf("الكل") + (scopedLeaveRequests.map { it.gradeClass } + scopedStudents.map { it.gradeClass })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct())
    }

    val pendingCount = remember(scopedLeaveRequests) {
        scopedLeaveRequests.count { it.status == LeaveRequestStatus.PENDING }
    }
    val approvedCount = remember(scopedLeaveRequests) {
        scopedLeaveRequests.count { it.status == LeaveRequestStatus.APPROVED }
    }
    val rejectedCount = remember(scopedLeaveRequests) {
        scopedLeaveRequests.count { it.status == LeaveRequestStatus.REJECTED }
    }

    val filteredRequests = remember(scopedLeaveRequests, selectedStatusTab, searchQuery, selectedClassFilter) {
        val q = searchQuery.trim()
        scopedLeaveRequests.filter { req ->
            val matchesStatus = when (selectedStatusTab) {
                0 -> req.status == LeaveRequestStatus.PENDING
                1 -> req.status == LeaveRequestStatus.APPROVED
                2 -> req.status == LeaveRequestStatus.REJECTED
                else -> true
            }
            val matchesClass = selectedClassFilter == "الكل" ||
                ArabicTextNormalizer.matchesClass(req.gradeClass, selectedClassFilter)
            val matchesQuery = q.isEmpty() ||
                req.studentName.contains(q, ignoreCase = true) ||
                req.reason.contains(q, ignoreCase = true) ||
                req.leaveDate.contains(q, ignoreCase = true)
            matchesStatus && matchesClass && matchesQuery
        }.sortedByDescending { it.requestedAt.ifBlank { it.leaveDate } }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 780.dp)
                .testTag("indexed_leave_requests_modal"),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepEmerald.copy(alpha = 0.45f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InlineDialogToastBanner(
                    toastAlert = toastAlert,
                    onDismiss = { viewModel.dismissToastAlert() }
                )

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.3f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventNote,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "مركز طلبات الإجازة الإلكترونية المفهرس",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Text(
                                text = if (canManage) {
                                    "مراجعة واعتماد طلبات الإجازة المدرسية سحابياً (${scopedLeaveRequests.size} طلب)"
                                } else {
                                    "متابعة حالة طلبات الإجازة وإرسال طلب جديد للإدارة"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_indexed_leave_modal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق",
                            tint = DeepForestGreen
                        )
                    }
                }

                // Parent / Student Submit New Leave Request Button
                if (isParentOrStudent && scopedStudents.isNotEmpty()) {
                    Button(
                        onClick = { showNewRequestSheet = !showNewRequestSheet },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("toggle_new_leave_request_form_btn"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (showNewRequestSheet) Icons.Filled.ExpandLess else Icons.Filled.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showNewRequestSheet) "إخفاء نموذج طلب الإجازة" else "إرسال طلب إجازة إلكتروني جديد",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    if (showNewRequestSheet) {
                        val targetStu = scopedStudents.first()
                        var selectedType by remember { mutableStateOf(LeaveType.SICK) }
                        var leaveDate by remember { mutableStateOf(todayIso) }
                        var durationStr by remember { mutableStateOf("1") }
                        var reason by remember { mutableStateOf("") }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "تقديم طلب إجازة للطالب: ${targetStu.fullNameWithFather}",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    LeaveType.entries.forEach { lt ->
                                        FilterChip(
                                            selected = selectedType == lt,
                                            onClick = { selectedType = lt },
                                            label = { Text(lt.arabicLabel, maxLines = 1) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ModernCalendarDatePickerField(
                                        selectedDateIso = leaveDate,
                                        label = "تاريخ بدء الإجازة",
                                        onDateSelected = { iso, _ -> leaveDate = iso },
                                        modifier = Modifier.weight(1.3f)
                                    )
                                    OutlinedTextField(
                                        value = durationStr,
                                        onValueChange = { durationStr = it.filter { ch -> ch.isDigit() } },
                                        label = { Text("المدة (أيام)") },
                                        singleLine = true,
                                        modifier = Modifier.weight(0.7f)
                                    )
                                }
                                OutlinedTextField(
                                    value = reason,
                                    onValueChange = { reason = it },
                                    label = { Text("سبب الإجازة والتفاصيل") },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 2
                                )
                                Button(
                                    onClick = {
                                        val days = durationStr.toIntOrNull()?.coerceIn(1, 30) ?: 1
                                        viewModel.submitLeaveRequest(
                                            student = targetStu,
                                            leaveType = selectedType,
                                            leaveDate = leaveDate,
                                            durationDays = days,
                                            reason = reason
                                        )
                                        reason = ""
                                        showNewRequestSheet = false
                                    },
                                    enabled = reason.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepEmerald,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("اعتماد وإرسال الطلب سحابياً", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                // Status Index Tabs
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tabItems = listOf(
                        Triple(0, "المعلقة قيد المراجعة ($pendingCount)", WarningAmber),
                        Triple(1, "المقبولة ($approvedCount)", SuccessGreen),
                        Triple(2, "المرفوضة ($rejectedCount)", DangerRed),
                        Triple(3, "الكل (${scopedLeaveRequests.size})", DeepEmerald)
                    )
                    items(tabItems) { (idx, label, _) ->
                        val isSel = selectedStatusTab == idx
                        FilterChip(
                            selected = isSel,
                            onClick = { selectedStatusTab = idx },
                            label = {
                                Text(
                                    text = label,
                                    fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        )
                    }
                }

                // Search & Class Filter
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("بحث باسم الطالب أو سبب الإجازة أو التاريخ...") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DeepEmerald) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                if (canManage && availableClasses.size > 2) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(availableClasses) { cls ->
                            FilterChip(
                                selected = selectedClassFilter == cls,
                                onClick = { selectedClassFilter = cls },
                                label = { Text(cls, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }

                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.16f))

                if (filteredRequests.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.EventAvailable,
                                contentDescription = null,
                                tint = DeepEmerald.copy(alpha = 0.5f),
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "لا توجد طلبات إجازة مطابقة للتبويب المحدد حالياً.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextMutedGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredRequests, key = { it.id }) { req ->
                            LeaveRequestCardItem(
                                req = req,
                                isAdmin = canManage,
                                onApprove = {
                                    viewModel.requireOnlineAndExecute {
                                        viewModel.respondToLeaveRequest(req.id, approve = true)
                                    }
                                },
                                onReject = {
                                    viewModel.requireOnlineAndExecute {
                                        viewModel.respondToLeaveRequest(req.id, approve = false)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Deliverable 5: Comprehensive Student Archive Screen (السجل الشامل للطالب — الأرشيف الشهري والسنوي)
 * - Monthly indexing (الشهر بشهره من أيلول إلى تموز + الكل)
 * - 1. Attendance & Gate check-in/out times, Absence dates, Excused Leaves & reasons, Late days
 * - 2. Monthly & Semester Exams & Grades + Evaluations
 * - 3. Daily Behavior & Classroom Follow-Up Log + Notes
 * - 4. Canteen Wallet Transactions + Archived Yearly Promotion Summaries
 * - 5. Export Comprehensive Student Archive Report as PDF
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentComprehensiveArchiveScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val exams by viewModel.examSchedules.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val dailyBehaviors by viewModel.dailyBehaviors.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val latestAnnualPromotionReport by viewModel.latestAnnualPromotionReport.collectAsState()
    var showEnrollmentDecisionModalForStudent by remember { mutableStateOf<StudentRecord?>(null) }

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val scopedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser)
    }

    val allowedClasses = remember(schoolConfig, currentUser, students) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, students, selectedClass) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    var studentSearchQuery by remember { mutableStateOf("") }

    val filteredStudentsList = remember(scopedStudents, isParentOrStudent, selectedClass, selectedSection, studentSearchQuery) {
        if (isParentOrStudent) {
            scopedStudents
        } else {
            val q = studentSearchQuery.trim()
            scopedStudents.filter { stu ->
                ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                    ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection) &&
                    (q.isEmpty() || stu.fullNameWithFather.contains(q, ignoreCase = true) || stu.studentName.contains(q, ignoreCase = true))
            }
        }
    }

    var selectedStudentId by remember(filteredStudentsList, selectedParentChildId) {
        mutableStateOf(
            if (isParentOrStudent && selectedParentChildId.isNotBlank()) {
                selectedParentChildId
            } else {
                filteredStudentsList.firstOrNull()?.id ?: ""
            }
        )
    }
    val selectedStudent = remember(filteredStudentsList, scopedStudents, selectedStudentId) {
        filteredStudentsList.firstOrNull { it.id == selectedStudentId }
            ?: filteredStudentsList.firstOrNull()
            ?: scopedStudents.firstOrNull()
    }

    // Monthly Indexing: "ALL" or month number "09".."08"
    val academicMonths = remember {
        listOf(
            "ALL" to "الكل (السنة كاملة)",
            "09" to "أيلول (9)",
            "10" to "تشرين الأول (10)",
            "11" to "تشرين الثاني (11)",
            "12" to "كانون الأول (12)",
            "01" to "كانون الثاني (1)",
            "02" to "شباط (2)",
            "03" to "آذار (3)",
            "04" to "نيسان (4)",
            "05" to "أيار (5)",
            "06" to "حزيران (6)",
            "07" to "تموز (7)",
            "08" to "آب (8)"
        )
    }
    var selectedMonthCode by remember { mutableStateOf("ALL") }
    val selectedMonthLabel = remember(selectedMonthCode) {
        academicMonths.firstOrNull { it.first == selectedMonthCode }?.second ?: "الكل (السنة كاملة)"
    }

    fun matchesSelectedMonth(dateIso: String): Boolean {
        if (selectedMonthCode == "ALL") return true
        val clean = ArabicTextNormalizer.normalizeDateIso(dateIso.trim())
        val parts = clean.split("-")
        return parts.size >= 2 && parts[1].padStart(2, '0') == selectedMonthCode
    }

    val stuBadges = remember(attendance, grades, selectedStudent?.id, holidaysConfig) {
        selectedStudent?.let { viewModel.getStudentAutoBadges(it.id) } ?: emptyList()
    }
    val stuAttendanceMap = remember(attendance, selectedStudent, holidaysConfig) {
        selectedStudent?.let { viewModel.getStudentAttendanceMap(it.id) } ?: emptyMap()
    }
    val monthFilteredAttendance = remember(stuAttendanceMap, selectedMonthCode) {
        stuAttendanceMap.entries
            .filter { matchesSelectedMonth(it.key) }
            .sortedByDescending { it.key }
    }
    val presentDays = remember(monthFilteredAttendance) {
        monthFilteredAttendance.filter { it.value == AttendanceStatus.PRESENT }
    }
    val absentDays = remember(monthFilteredAttendance) {
        monthFilteredAttendance.filter { it.value == AttendanceStatus.ABSENT }
    }
    val excusedDays = remember(monthFilteredAttendance) {
        monthFilteredAttendance.filter { it.value == AttendanceStatus.EXCUSED }
    }
    val lateDays = remember(monthFilteredAttendance) {
        monthFilteredAttendance.filter { it.value == AttendanceStatus.LATE }
    }
    val stuLeaveRequests = remember(leaveRequests, selectedStudent?.id, selectedMonthCode) {
        val targetId = selectedStudent?.id ?: ""
        if (targetId.isEmpty()) emptyList()
        else leaveRequests.filter { it.studentId == targetId && matchesSelectedMonth(it.leaveDate) }
    }
    val stuGateLogs = remember(selectedStudent?.dailyGateLogs, selectedMonthCode) {
        (selectedStudent?.dailyGateLogs ?: emptyMap()).entries
            .filter { matchesSelectedMonth(it.key) }
            .sortedByDescending { it.key }
    }
    val stuExams = remember(exams, selectedStudent?.gradeClass, selectedStudent?.sectionName, selectedMonthCode) {
        val stu = selectedStudent
        if (stu == null) emptyList()
        else exams.filter {
            ArabicTextNormalizer.matchesClass(it.gradeClass, stu.gradeClass) &&
                (it.sectionName.isBlank() || ArabicTextNormalizer.matchesSection(it.sectionName, stu.sectionName)) &&
                matchesSelectedMonth(it.examDate)
        }
    }
    val stuGrades = remember(grades, selectedStudent?.id, selectedStudent?.gradeClass, schoolConfig) {
        val stuObj = selectedStudent
        if (stuObj == null) {
            emptyList()
        } else {
            val recorded = grades.filter { it.studentId == stuObj.id }
            val configuredSubjects = viewModel.getSubjectsForClass(stuObj.gradeClass, null)
            val allSubNames = (configuredSubjects + recorded.map { it.subjectName })
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .distinctBy { ArabicTextNormalizer.normalizeText(it) }
            allSubNames.map { subName ->
                val found = recorded.firstOrNull {
                    ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(subName)
                }
                val dynamicScale = found?.maxScoreScale ?: viewModel.getDefaultMaxScoreForClassAndSubject(stuObj.gradeClass, subName)
                found?.copy(maxScoreScale = dynamicScale) ?: SubjectGrade(
                    id = "temp_${stuObj.id}_$subName",
                    studentId = stuObj.id,
                    studentName = stuObj.studentName,
                    gradeClass = stuObj.gradeClass,
                    subjectName = subName,
                    dailyScore = 0,
                    month1Score = 0,
                    month2Score = 0,
                    midYearScore = 0,
                    finalExamScore = 0,
                    maxScoreScale = dynamicScale,
                    schoolId = schoolConfig.schoolId
                )
            }
        }
    }
    val stuAverage = remember(stuGrades) {
        val withScores = stuGrades.filter {
            it.dailyScore > 0 || it.month1Score > 0 || it.month2Score > 0 || it.midYearScore > 0 || it.finalExamScore > 0
        }
        if (withScores.isEmpty()) 0 else withScores.map { it.normalizedPercentage }.average().toInt()
    }
    val stuBehaviors = remember(dailyBehaviors, selectedStudent?.id, selectedMonthCode) {
        val targetId = selectedStudent?.id ?: ""
        if (targetId.isEmpty()) emptyList()
        else dailyBehaviors.filter { it.studentId == targetId && matchesSelectedMonth(it.date) }
            .sortedByDescending { it.date }
    }
    val stuWalletTxs = remember(selectedStudent?.walletTransactions, selectedMonthCode) {
        (selectedStudent?.walletTransactions ?: emptyList()).filter { tx ->
            if (selectedMonthCode == "ALL") true
            else {
                val datePart = tx.timestamp.take(10)
                matchesSelectedMonth(datePart)
            }
        }
    }

    // Mode Switcher for Admin & Teacher:
    // 0 = السجل الإحصائي العام للصفوف والشعب
    // 1 = السجل التفصيلي الفردي للطالب
    var archiveViewMode by remember(isParentOrStudent) {
        mutableStateOf(if (isParentOrStudent) 1 else 0)
    }
    var selectedGeneralClassFilter by remember { mutableStateOf("ALL") }

    val classStatisticalReports = remember(
        scopedStudents,
        allowedClasses,
        attendance,
        grades,
        leaveRequests,
        selectedMonthCode,
        selectedGeneralClassFilter,
        schoolConfig
    ) {
        val targetClasses = if (selectedGeneralClassFilter == "ALL") {
            allowedClasses
        } else {
            allowedClasses.filter { ArabicTextNormalizer.matchesClass(it, selectedGeneralClassFilter) }
        }
        val reportsList = mutableListOf<ClassComprehensiveStatisticalReport>()
        val requiredSubjectsCache = targetClasses.associateWith { cls ->
            viewModel.getRequiredSubjectsForClass(cls)
        }
        targetClasses.forEach { cls ->
            val secs = viewModel.getAllowedSectionsForClass(cls, currentUser)
            secs.forEach { sec ->
                val classStudents = scopedStudents.filter { stu ->
                    ArabicTextNormalizer.matchesClass(stu.gradeClass, cls) &&
                        ArabicTextNormalizer.matchesSection(stu.sectionName, sec)
                }
                if (classStudents.isNotEmpty() || secs.size == 1) {
                    val activeCount = classStudents.count { it.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE }
                    val suspendedCount = classStudents.count { it.enrollmentStatusEnum == StudentEnrollmentStatus.SUSPENDED_TEMP }
                    val expelledCount = classStudents.count {
                        it.enrollmentStatusEnum == StudentEnrollmentStatus.EXPELLED ||
                            it.enrollmentStatusEnum == StudentEnrollmentStatus.DEREGISTERED
                    }
                    val transferredCount = classStudents.count { it.enrollmentStatusEnum == StudentEnrollmentStatus.TRANSFERRED }

                    var totalPresent = 0
                    var totalAbsent = 0
                    var totalExcused = 0
                    var totalLate = 0
                    var passedCount = 0
                    var repeatingCount = 0
                    var pendingGradesCount = 0
                    val studentAvgs = mutableListOf<Int>()
                    val reqSubjects: List<String> = requiredSubjectsCache[cls] ?: emptyList()

                    classStudents.forEach { stu ->
                        val attMap = viewModel.getStudentAttendanceMap(stu.id)
                            .filter { matchesSelectedMonth(it.key) }
                        totalPresent += attMap.count { it.value == AttendanceStatus.PRESENT }
                        totalAbsent += attMap.count { it.value == AttendanceStatus.ABSENT }
                        totalExcused += attMap.count { it.value == AttendanceStatus.EXCUSED }
                        totalLate += attMap.count { it.value == AttendanceStatus.LATE }

                        val sGrades = grades.filter { it.studentId == stu.id }
                        if (sGrades.isNotEmpty()) {
                            studentAvgs.add(sGrades.map { it.finalAverage }.average().toInt())
                        }
                        val completedReqCount = reqSubjects.count { reqSub ->
                            sGrades.any { g ->
                                ArabicTextNormalizer.normalizeText(g.subjectName) == ArabicTextNormalizer.normalizeText(reqSub) &&
                                    g.isAllExamsCompleted
                            }
                        }
                        val isGradesComplete = reqSubjects.isNotEmpty() &&
                            completedReqCount >= reqSubjects.size &&
                            sGrades.all { it.isAllExamsCompleted }
                        if (!isGradesComplete) {
                            pendingGradesCount++
                        } else {
                            val avg = sGrades.map { it.finalAverage }.average().toInt()
                            val failedSubs = sGrades.count { g -> g.finalAverage < (g.maxScoreScale / 2) }
                            if (avg >= 50 && failedSubs <= 2) passedCount++ else repeatingCount++
                        }
                    }

                    val totalTrackedDays = totalPresent + totalAbsent + totalExcused + totalLate
                    val attRate = if (totalTrackedDays == 0) 100 else (((totalPresent + totalExcused) * 100.0) / totalTrackedDays).toInt().coerceIn(0, 100)
                    val avgGrade = if (studentAvgs.isEmpty()) 0 else studentAvgs.average().toInt()
                    val evaluatedCount = passedCount + repeatingCount
                    val passRate = if (evaluatedCount == 0) 0 else ((passedCount * 100.0) / evaluatedCount).toInt().coerceIn(0, 100)

                    reportsList.add(
                        ClassComprehensiveStatisticalReport(
                            gradeClass = cls,
                            sectionName = sec,
                            totalStudents = classStudents.size,
                            activeStudentsCount = activeCount,
                            suspendedStudentsCount = suspendedCount,
                            expelledStudentsCount = expelledCount,
                            transferredStudentsCount = transferredCount,
                            presentDaysTotal = totalPresent,
                            absentDaysTotal = totalAbsent,
                            excusedDaysTotal = totalExcused,
                            lateDaysTotal = totalLate,
                            attendanceRatePercent = attRate,
                            averageGradePercent = avgGrade,
                            passRatePercent = passRate,
                            passedStudentsCount = passedCount,
                            repeatingStudentsCount = repeatingCount,
                            pendingGradesCount = pendingGradesCount
                        )
                    )
                }
            }
        }
        reportsList
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Top 3D Hero Header Card & Unclipped Dual Mode Switcher for Admin/Teacher
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThreeDHeroHeaderCard(
                    title = "السجل الأكاديمي والإحصائي الشامل",
                    subtitle = "تقارير الأداء، المواظبة، والنتائج الشهرية والسنوية للصفوف والطلبة",
                    rightPillText = schoolConfig.academicYear,
                    rightPillIcon = Icons.Filled.FolderSpecial,
                    leftPillText = currentUser?.role?.arabicTitle ?: "إدارة السجل",
                    leftPillIcon = Icons.Filled.VerifiedUser,
                    illustrationType = ThreeDIllustrationType.BOOKS_GRADUATION_CAP
                )

                if (!isParentOrStudent) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.25f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Concise Unclipped Dual Mode Switcher: [السجل العام للصفوف] vs [السجل الفردي للطالب]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0xFFF0FDF4))
                                    .padding(4.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    0 to "السجل العام للصفوف",
                                    1 to "السجل الفردي للطالب"
                                ).forEach { (modeIdx, modeTitle) ->
                                    val isSel = archiveViewMode == modeIdx
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { archiveViewMode = modeIdx }
                                            .testTag("archive_mode_tab_$modeIdx"),
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSel) DeepEmerald else Color.Transparent,
                                        shadowElevation = if (isSel) 3.dp else 0.dp
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = modeTitle,
                                                fontSize = 12.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSel) Color.White else DeepForestGreen,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }

                            if (isAdmin) {
                                Button(
                                    onClick = { viewModel.buildAnnualPromotionPreviewReport() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("archive_open_annual_promotion_report_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepEmerald,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "الترحيل السنوي وتقرير النجاح العام",
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            if (archiveViewMode == 1) {
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    items(allowedClasses) { cls ->
                                        FilterChip(
                                            selected = selectedClass == cls,
                                            onClick = {
                                                selectedClass = cls
                                                val nextSecs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                                                if (selectedSection !in nextSecs) {
                                                    selectedSection = nextSecs.firstOrNull() ?: "أ"
                                                }
                                            },
                                            label = { Text(cls, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("الشعبة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    allowedSections.forEach { sec ->
                                        FilterChip(
                                            selected = selectedSection == sec,
                                            onClick = { selectedSection = sec },
                                            label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }
                                OutlinedTextField(
                                    value = studentSearchQuery,
                                    onValueChange = { studentSearchQuery = it },
                                    placeholder = { Text("بحث سريع باسم الطالب في هذا الصف...") },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DeepEmerald) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                if (filteredStudentsList.isNotEmpty()) {
                                    Text(
                                        text = "اختر الطالب لعرض سجله الأكاديمي الشامل (${filteredStudentsList.size} طالب):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(filteredStudentsList, key = { it.id }) { stu ->
                                            val isSel = selectedStudent?.id == stu.id
                                            FilterChip(
                                                selected = isSel,
                                                onClick = { selectedStudentId = stu.id },
                                                label = {
                                                    Text(
                                                        text = stu.fullNameWithFather,
                                                        fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = if (isSel) Icons.Filled.CheckCircle else Icons.Filled.Person,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (!isParentOrStudent && archiveViewMode == 0) {
            // =========================================================================
            // MODE 0: السجل الإحصائي العام للصفوف والشعب (General Class & Section Archive)
            // =========================================================================
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تصفية التقارير الإحصائية حسب الفترة الزمنية والصف الدراسي:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(academicMonths) { (code, label) ->
                                val isSel = selectedMonthCode == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedMonthCode = code },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            item {
                                FilterChip(
                                    selected = selectedGeneralClassFilter == "ALL",
                                    onClick = { selectedGeneralClassFilter = "ALL" },
                                    label = { Text("جميع الصفوف المسجلة (${allowedClasses.size})", fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                                )
                            }
                            items(allowedClasses) { cls ->
                                FilterChip(
                                    selected = selectedGeneralClassFilter == cls,
                                    onClick = { selectedGeneralClassFilter = cls },
                                    label = { Text(cls, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false) }
                                )
                            }
                        }

                        val totalAllStudents = classStatisticalReports.sumOf { it.totalStudents }
                        val avgAttRate = if (classStatisticalReports.isEmpty()) 100 else classStatisticalReports.map { it.attendanceRatePercent }.average().toInt()
                        val avgGradeAll = if (classStatisticalReports.isEmpty()) 0 else classStatisticalReports.map { it.averageGradePercent }.average().toInt()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("الشعب المشمولة", "${classStatisticalReports.size} شعبة", DeepEmerald),
                                Triple("إجمالي الطلبة", "$totalAllStudents طالب", InfoBlue),
                                Triple("معدل المواظبة", "$avgAttRate%", SuccessGreen),
                                Triple("متوسط الدرجات", "$avgGradeAll%", DeepForestGreen)
                            ).forEach { (lbl, valTxt, col) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = col.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.32f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = valTxt,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = col,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestGreen,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val res = DocumentAndIdExporter.exportClassComprehensiveStatisticalReportPdf(
                                    context = context,
                                    schoolConfig = schoolConfig,
                                    reports = classStatisticalReports,
                                    periodLabel = selectedMonthLabel,
                                    principalName = schoolConfig.principalFullName.ifBlank { currentUser?.fullName ?: "مدير المدرسة" }
                                )
                                if (res.isSuccess) {
                                    viewModel.showToastAlert(
                                        "تم تصدير وفتح السجل الإحصائي العام للصفوف والشعب بصيغة PDF بنجاح",
                                        isSuccess = true
                                    )
                                } else {
                                    viewModel.showToastAlert(
                                        "تعذر تصدير التقرير الإحصائي: ${res.exceptionOrNull()?.message}",
                                        isSuccess = false
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_general_class_statistical_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepEmerald,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "طباعة / تصدير السجل الإحصائي العام للصفوف والشعب (PDF)",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            items(classStatisticalReports, key = { "${it.gradeClass}_${it.sectionName}" }) { rep ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.28f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "الصف: ${rep.gradeClass} — شعبة (${rep.sectionName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "إجمالي الطلبة: ${rep.totalStudents} • مستمر بالدوام: ${rep.activeStudentsCount} • الفترة: $selectedMonthLabel",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            NoWrapBadge(
                                text = "المواظبة: ${rep.attendanceRatePercent}%",
                                icon = Icons.Filled.FactCheck,
                                containerColor = DeepEmerald,
                                contentColor = Color.White
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("أيام الحضور", "${rep.presentDaysTotal}", SuccessGreen),
                                Triple("أيام الغياب", "${rep.absentDaysTotal}", DangerRed),
                                Triple("الإجازات", "${rep.excusedDaysTotal}", InfoBlue),
                                Triple("معدل الصف", if (rep.averageGradePercent > 0) "${rep.averageGradePercent}%" else "بانتظار الرصد", DeepEmerald)
                            ).forEach { (lbl, valTxt, col) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    color = col.copy(alpha = 0.09f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.30f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = valTxt,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = col,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepForestGreen,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "نتائج الصف والقرارات الإدارية: ناجح مكتمل (${rep.passedStudentsCount}) • معيد (${rep.repeatingStudentsCount}) • بانتظار استكمال الدرجات (${rep.pendingGradesCount})",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                if (rep.suspendedStudentsCount > 0 || rep.expelledStudentsCount > 0 || rep.transferredStudentsCount > 0) {
                                    Text(
                                        text = "حالات القيد الإدارية: مفصول مؤقتاً (${rep.suspendedStudentsCount}) • مرقن قيده/مفصول (${rep.expelledStudentsCount}) • منقول (${rep.transferredStudentsCount})",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DangerRed
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedClass = rep.gradeClass
                                    selectedSection = rep.sectionName
                                    archiveViewMode = 1
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald)
                            ) {
                                Icon(Icons.Filled.Groups, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "استعراض سجلات طلاب الصف",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                            }
                            Button(
                                onClick = {
                                    val res = DocumentAndIdExporter.exportClassComprehensiveStatisticalReportPdf(
                                        context = context,
                                        schoolConfig = schoolConfig,
                                        reports = listOf(rep),
                                        periodLabel = "${rep.gradeClass} (${rep.sectionName}) - $selectedMonthLabel",
                                        principalName = schoolConfig.principalFullName.ifBlank { currentUser?.fullName ?: "مدير المدرسة" }
                                    )
                                    if (res.isSuccess) {
                                        viewModel.showToastAlert("تم تصدير تقرير الصف (${rep.gradeClass} - ${rep.sectionName}) بصيغة PDF", isSuccess = true)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("تقرير PDF", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        } else if (selectedStudent == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد طالب محدد ضمن هذا الصف حالياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMutedGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        } else {
            val stu = selectedStudent

            // Student Identity & Export PDF Banner Card (Emerald Compact Card without Vertical Distortion or Truncation)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DeepEmerald),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StudentAvatarWithOverlayBadge(
                                base64Data = stu.photoBase64,
                                contentDescription = stu.fullNameWithFather,
                                badge = stuBadges.firstOrNull(),
                                avatarSize = 54.dp,
                                boxWidth = 62.dp,
                                fallbackIcon = Icons.Filled.Person,
                                tint = Color.White,
                                onDarkBackground = true
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = stu.fullNameWithFather,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Text(
                                    text = "الصف: ${stu.gradeClass} • شعبة (${stu.sectionName}) • العام الدراسي: ${viewModel.formatAcademicYearAscending(schoolConfig.academicYear).second}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.94f),
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Surface(
                                        onClick = {
                                            if (isAdmin) {
                                                showEnrollmentDecisionModalForStudent = stu
                                            }
                                        },
                                        enabled = isAdmin,
                                        shape = RoundedCornerShape(50),
                                        color = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) {
                                            Color.White.copy(alpha = 0.22f)
                                        } else {
                                            DangerRed
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Gavel,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "حالة القيد: ${stu.enrollmentStatusEnum.arabicLabel}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color.White,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color.White.copy(alpha = 0.22f)
                                    ) {
                                        Text(
                                            text = "المعدل العام: ${if (stuGrades.isEmpty()) "بانتظار رصد الدرجات" else "$stuAverage%"}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val res = DocumentAndIdExporter.exportComprehensiveStudentArchivePdf(
                                    context = context,
                                    schoolConfig = schoolConfig,
                                    student = stu,
                                    monthLabel = selectedMonthLabel,
                                    attendanceMap = monthFilteredAttendance.associate { it.key to it.value },
                                    leaveRequests = stuLeaveRequests,
                                    exams = stuExams,
                                    grades = stuGrades,
                                    behaviors = stuBehaviors,
                                    principalName = schoolConfig.principalFullName.ifBlank { "مدير المدرسة" }
                                )
                                if (res.isSuccess) {
                                    viewModel.showToastAlert(
                                        "تم تصدير وفتح السجل الشامل للطالب (${stu.studentName}) بصيغة PDF بنجاح",
                                        isSuccess = true
                                    )
                                } else {
                                    viewModel.showToastAlert(
                                        "تعذر تصدير السجل الشامل PDF: ${res.exceptionOrNull()?.message}",
                                        isSuccess = false
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_comprehensive_student_archive_pdf_btn"),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White,
                                contentColor = DeepForestGreen
                            )
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "طباعة / تصدير السجل الشامل للطالب (PDF) — $selectedMonthLabel",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Card: ملف حالة القيد والقرارات الإدارية للطالب (Enrollment Status & Administrative Decisions Panel)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald.copy(alpha = 0.28f) else DangerRed.copy(alpha = 0.45f)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) Color(0xFFECFDF5) else DangerRed.copy(alpha = 0.12f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Gavel,
                                        contentDescription = null,
                                        tint = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald else DangerRed,
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .size(20.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        text = "ملف حالة القيد والقرارات الإدارية للطالب",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen
                                    )
                                    Text(
                                        text = "توثيق حالة الدوام، الفصل المؤقت أو الدائمي، ترقين القيد، والنقل المدرسي",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedGreen
                                    )
                                }
                            }
                            NoWrapBadge(
                                text = stu.enrollmentStatusEnum.arabicLabel,
                                icon = Icons.Filled.VerifiedUser,
                                containerColor = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald else DangerRed,
                                contentColor = Color.White
                            )
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) Color(0xFFECFDF5) else DangerRed.copy(alpha = 0.08f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald.copy(alpha = 0.35f) else DangerRed.copy(alpha = 0.35f)
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "تفاصيل القرار الإداري وحالة القيد الحالية:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepForestGreen else DangerRed
                                )
                                Text(
                                    text = stu.getDetailedEnrollmentSummaryText(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepForestGreen
                                )
                            }
                        }

                        if (stu.enrollmentHistory.isNotEmpty()) {
                            Text(
                                text = "سجل القرارات الإدارية السابقة لهذا الطالب (${stu.enrollmentHistory.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                stu.enrollmentHistory.forEach { hist ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.2f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = hist.status.arabicLabel,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepEmerald
                                                )
                                                Text(
                                                    text = hist.decisionDate,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMutedGreen
                                                )
                                            }
                                            if (hist.reason.isNotBlank()) {
                                                Text(
                                                    text = "السبب: ${hist.reason}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DeepForestGreen
                                                )
                                            }
                                            if (hist.suspensionStartDate.isNotBlank() || hist.suspensionEndDate.isNotBlank()) {
                                                Text(
                                                    text = "الفترة: من ${hist.suspensionStartDate.ifBlank { "—" }} إلى ${hist.suspensionEndDate.ifBlank { "—" }}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepEmerald
                                                )
                                            }
                                            if (hist.transferSchoolName.isNotBlank()) {
                                                Text(
                                                    text = "المدرسة المنقول إليها: ${hist.transferSchoolName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepEmerald
                                                )
                                            }
                                            if (hist.orderNumber.isNotBlank()) {
                                                Text(
                                                    text = "رقم الأمر الإداري: ${hist.orderNumber}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMutedGreen
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = { showEnrollmentDecisionModalForStudent = stu },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("archive_open_enrollment_decision_modal_btn"),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Gavel,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اتخاذ قرار إداري / تعديل حالة قيد الطالب (فصل / ترقين قيد / نقل)",
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Monthly Indexing Selector Row (الشهر بشهره)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "الفهرسة الشهرية للأرشيف (اختر الشهر لعرض تفاصيله الدقيقة أو السنة كاملة):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(academicMonths) { (code, label) ->
                                val isSel = selectedMonthCode == code
                                FilterChip(
                                    selected = isSel,
                                    onClick = { selectedMonthCode = code },
                                    label = {
                                        Text(
                                            text = label,
                                            fontWeight = if (isSel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Attendance, Gate Timestamps, Absences & Leaves for Selected Month
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.HowToReg, contentDescription = null, tint = DeepEmerald)
                            Text(
                                text = "1. سجل الحضور والغياب والإجازات وحركة البوابة ($selectedMonthLabel)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("الحضور", "${presentDays.size} يوم", SuccessGreen),
                                Triple("الغياب", "${absentDays.size} يوم", DangerRed),
                                Triple("الإجازات", "${excusedDays.size} يوم", InfoBlue),
                                Triple("التأخر", "${lateDays.size} يوم", WarningAmber)
                            ).forEach { (lbl, valTxt, col) ->
                                Surface(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    color = col.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.35f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = valTxt,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = col
                                        )
                                        Text(
                                            text = lbl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepForestGreen,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }

                        if (absentDays.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DangerRed.copy(alpha = 0.07f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "تواريخ أيام الغياب المسجلة (${absentDays.size}):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DangerRed
                                    )
                                    Text(
                                        text = absentDays.joinToString(" • ") { it.key },
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1E293B)
                                    )
                                }
                            }
                        }

                        if (stuLeaveRequests.isNotEmpty()) {
                            Text(
                                text = "تفاصيل الإجازات وأسبابها (${stuLeaveRequests.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            stuLeaveRequests.forEach { req ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${req.leaveType.arabicLabel} • ${req.leaveDate} (${req.durationDays} يوم)",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = "السبب: ${req.reason}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextMutedGreen
                                            )
                                        }
                                        NoWrapBadge(
                                            text = req.status.arabicLabel,
                                            icon = Icons.Filled.EventNote,
                                            containerColor = DeepEmerald.copy(alpha = 0.14f),
                                            contentColor = DeepEmerald
                                        )
                                    }
                                }
                            }
                        }

                        if (stuGateLogs.isNotEmpty()) {
                            Text(
                                text = "توقيتات الدخول والمغادرة عبر البوابة الذكية (${stuGateLogs.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            stuGateLogs.take(15).forEach { (dateKey, log) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.2f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = dateKey,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen
                                        )
                                        Text(
                                            text = "دخول: ${log.checkInTime.ifBlank { "—" }} | مغادرة: ${log.checkOutTime.ifBlank { "—" }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Exams & Grades Log
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.Assessment, contentDescription = null, tint = DeepEmerald)
                            Text(
                                text = "2. سجل الامتحانات والدرجات والتقييمات (${stuGrades.size} مادة)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                        }

                        if (stuExams.isNotEmpty()) {
                            Text(
                                text = "الامتحانات المجدولة خلال الفترة (${stuExams.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepEmerald
                            )
                            stuExams.forEach { ex ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${ex.subjectName} (${ex.examType})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            if (ex.topicsIncluded.isNotBlank()) {
                                                Text(
                                                    text = "المادة الداخلة: ${ex.topicsIncluded}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMutedGreen
                                                )
                                            }
                                        }
                                        val cleanExamIso = ArabicTextNormalizer.normalizeDateIso(ex.examDate.trim())
                                        val dynamicExamMax = viewModel.getDefaultMaxScoreForClassAndSubject(ex.gradeClass, ex.subjectName)
                                        Text(
                                            text = "$cleanExamIso • الدرجة من $dynamicExamMax",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepEmerald
                                        )
                                    }
                                }
                            }
                        }

                        if (stuGrades.isEmpty()) {
                            Text(
                                text = "لم يتم رصد درجات للمواد الدراسية لهذا الطالب بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedGreen
                            )
                        } else {
                            stuGrades.forEach { gr ->
                                val minPassForScale = if (gr.maxScoreScale <= 10) 5 else 50
                                val isPassingAvg = gr.finalAverage >= minPassForScale
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${gr.subjectName} (من ${gr.maxScoreScale})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            NoWrapBadge(
                                                text = "المعدل: ${gr.finalAverage} / ${gr.maxScoreScale} (${gr.evaluationText})",
                                                icon = Icons.Filled.Grade,
                                                containerColor = if (isPassingAvg) SuccessGreen.copy(alpha = 0.14f) else DangerRed.copy(alpha = 0.14f),
                                                contentColor = if (isPassingAvg) SuccessGreen else DangerRed
                                            )
                                        }
                                        Text(
                                            text = "ش1: ${gr.month1} / ${gr.maxScoreScale} | ش2: ${gr.month2} / ${gr.maxScoreScale} | نصف السنة: ${gr.midYear} / ${gr.maxScoreScale} | ش3: ${gr.month3} / ${gr.maxScoreScale} | ش4: ${gr.month4} / ${gr.maxScoreScale} | النهائي: ${gr.finalExam} / ${gr.maxScoreScale}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedGreen,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 3: Daily Behavior & Classroom Follow-Up Log
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(ClipboardCheckVectorIcon, contentDescription = null, tint = DeepEmerald)
                            Text(
                                text = "3. دفتر المتابعة اليومي والسلوك الصفي والملاحظات (${stuBehaviors.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                        }

                        if (stuBehaviors.isEmpty()) {
                            Text(
                                text = "لا توجد ملاحظات سلوكية أو تربوية مسجلة في هذه الفترة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMutedGreen
                            )
                        } else {
                            stuBehaviors.forEach { beh ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "${beh.behaviorType.arabicTitle} • ${beh.subjectName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = beh.date,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepEmerald
                                            )
                                        }
                                        if (beh.note.isNotBlank()) {
                                            Text(
                                                text = beh.note,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color(0xFF1E293B),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        if (beh.teacherName.isNotBlank()) {
                                            Text(
                                                text = "بواسطة: ${beh.teacherName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 4: Canteen Wallet Transactions & Archived Yearly Promotion Summaries
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.AccountBalanceWallet, contentDescription = null, tint = DeepEmerald)
                            Text(
                                text = "4. حركات محفظة الحانوت والأرشيف السنوي للسنوات السابقة",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                        }

                        Text(
                            text = "الرصيد الحالي للمحفظة: ${"%,d".format(java.util.Locale.US, stu.walletBalance)} د.ع • عدد الحركات في الفترة: ${stuWalletTxs.size}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = DeepEmerald
                        )

                        stuWalletTxs.take(10).forEach { tx ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF8FAFC),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${tx.type.arabicLabel}: ${"%,d".format(java.util.Locale.US, tx.amount)} د.ع",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen
                                        )
                                        Text(
                                            text = "${tx.note} • ${tx.timestamp}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedGreen
                                        )
                                    }
                                    Text(
                                        text = "الرصيد: ${"%,d".format(java.util.Locale.US, tx.balanceAfter)} د.ع",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                }
                            }
                        }

                        if (stu.archivedYearlySummaries.isNotEmpty()) {
                            HorizontalDivider(color = DeepEmerald.copy(alpha = 0.18f))
                            Text(
                                text = "الأرشيف السنوي للأعوام الدراسية السابقة (${stu.archivedYearlySummaries.size}):",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            stu.archivedYearlySummaries.forEach { arch ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFECFDF5),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
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
                                            Text(
                                                text = "العام الدراسي: ${arch.academicYear} • ${arch.gradeClass} (${arch.sectionName})",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            NoWrapBadge(
                                                text = arch.resultStatus,
                                                icon = Icons.Filled.Verified,
                                                containerColor = DeepEmerald,
                                                contentColor = Color.White
                                            )
                                        }
                                        Text(
                                            text = "المعدل النهائي: ${arch.finalAverage}% • الحضور: ${arch.totalPresentDays} يوم • الغياب: ${arch.totalAbsentDays} يوم • الإجازات: ${arch.totalExcusedDays} يوم",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepEmerald
                                        )
                                        if (arch.promotedToGrade.isNotBlank()) {
                                            Text(
                                                text = "تم الترحيل إلى: ${arch.promotedToGrade} بتاريخ ${arch.archivedAt}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    showEnrollmentDecisionModalForStudent?.let { targetStudent ->
        val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }
        StudentEnrollmentStatusModalDialog(
            student = targetStudent,
            todayIso = todayIso,
            toastAlert = toastAlert,
            onSaveStatus = { status, reason, suspFrom, suspTo ->
                viewModel.requireOnlineAndExecute {
                    viewModel.updateStudentEnrollmentStatus(
                        studentId = targetStudent.id,
                        newStatus = status,
                        reason = reason,
                        suspensionFromDate = suspFrom,
                        suspensionToDate = suspTo
                    )
                    showEnrollmentDecisionModalForStudent = null
                }
            },
            onSaveFullDecision = { status, reason, suspFrom, suspTo, decisionDate, orderNum, transferSchool ->
                viewModel.requireOnlineAndExecute {
                    viewModel.updateStudentEnrollmentStatus(
                        studentId = targetStudent.id,
                        newStatus = status,
                        reason = reason,
                        suspensionFromDate = suspFrom,
                        suspensionToDate = suspTo,
                        decisionDate = decisionDate,
                        orderNumber = orderNum,
                        transferredToSchoolName = transferSchool
                    )
                    showEnrollmentDecisionModalForStudent = null
                }
            },
            onDismiss = { showEnrollmentDecisionModalForStudent = null }
        )
    }

    latestAnnualPromotionReport?.let { report ->
        AnnualPromotionReportModalDialog(
            report = report,
            schoolConfig = schoolConfig,
            toastAlert = toastAlert,
            onExecutePromotion = { nextYear ->
                viewModel.requireOnlineAndExecute {
                    viewModel.executeAnnualPromotionAndGenerateReport(nextYear)
                }
            },
            onManualOverrideStudent = { stuId, newGrade, newSec, newStat ->
                viewModel.requireOnlineAndExecute {
                    viewModel.manualUpdateStudentClassAndStatus(
                        studentId = stuId,
                        newGradeClass = newGrade,
                        newSectionName = newSec,
                        newStatus = newStat
                    )
                }
            },
            onEditStudent = { stuId ->
                selectedStudentId = stuId
                viewModel.dismissAnnualPromotionReportModal()
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportAnnualPromotionReportPdf(
                    context = context,
                    schoolConfig = schoolConfig,
                    report = report,
                    principalName = schoolConfig.principalFullName.ifBlank { currentUser?.fullName ?: "مدير المدرسة" }
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert("تم تصدير وفتح تقرير الترحيل السنوي بصيغة PDF بنجاح", isSuccess = true)
                } else {
                    viewModel.showToastAlert("تعذر تصدير التقرير: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { viewModel.dismissAnnualPromotionReportModal() }
        )
    }
}

private enum class LiveRadarDrillFilter(val arabicTitle: String) {
    ALL("الكل"),
    INSIDE_NOW("متواجدون بالمدرسة"),
    CHECKED_IN("سجلوا الدخول"),
    CHECKED_OUT("غادروا المدرسة"),
    PRESENT("الحاضرون"),
    ABSENT("الغائبون"),
    EXCUSED("المجازون"),
    UNRECORDED("بانتظار الرصد"),
    LATE("المتأخرون")
}

private data class LiveClassroomCardSummary(
    val gradeClass: String,
    val sectionName: String,
    val students: List<StudentRecord>,
    val presentCount: Int,
    val absentCount: Int,
    val excusedCount: Int,
    val unrecordedCount: Int,
    val lateCount: Int,
    val checkedInCount: Int,
    val checkedOutCount: Int,
    val insideNowCount: Int
)

private data class LiveRadarDrillTarget(
    val title: String,
    val gradeClassFilter: String? = null,
    val sectionFilter: String? = null,
    val initialFilter: LiveRadarDrillFilter = LiveRadarDrillFilter.ALL
)

/**
 * موجز الدوام وحركة البوابة اليومية
 * - 1) إحصائية اليوم لحضور الطلبة وحركة بوابة المدرسة
 * - 2) شبكة بطاقات الصفوف والشعب
 * - 3) كشف الأسماء التفصيلي الفوري عند الضغط
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LiveOperationsRadarScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()

    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }
    var selectedDateIso by remember { mutableStateOf(todayIso) }
    var selectedClassFilter by remember { mutableStateOf("ALL") }
    var drillTarget by remember { mutableStateOf<LiveRadarDrillTarget?>(null) }
    val selectedDateHolidayLabel = remember(holidaysConfig, selectedDateIso) {
        viewModel.getHolidayLabelForDate(selectedDateIso)
    }
    val isHolidayToday = selectedDateHolidayLabel != null

    val isTeacher = currentUser?.role == UserRole.TEACHER
    val allowedClasses = remember(schoolConfig, currentUser, students) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    val activeScopedStudents = remember(students, currentUser) {
        viewModel.getScopedStudentsForUser(currentUser).filter { it.isActive }
    }

    fun isStudentPresentOnDate(stu: StudentRecord, dateIso: String): AttendanceStatus {
        return viewModel.getStudentStatusForDate(stu, dateIso)
    }

    fun isStudentCheckedIn(stu: StudentRecord, dateIso: String): Boolean {
        val log = stu.getGateLogForDate(dateIso)
        return log != null && log.checkInTime.isNotBlank()
    }

    fun isStudentCheckedOut(stu: StudentRecord, dateIso: String): Boolean {
        val log = stu.getGateLogForDate(dateIso)
        return log != null && log.isDeparted
    }

    fun isStudentInsideNow(stu: StudentRecord, dateIso: String): Boolean {
        val log = stu.getGateLogForDate(dateIso)
        return log != null && log.checkInTime.isNotBlank() && log.checkOutTime.isBlank()
    }

    val totalStudentsCount = activeScopedStudents.size
    val presentTodayCount = remember(activeScopedStudents, attendance, selectedDateIso, holidaysConfig) {
        activeScopedStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.PRESENT }
    }
    val absentTodayCount = remember(activeScopedStudents, attendance, selectedDateIso, holidaysConfig) {
        activeScopedStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.ABSENT }
    }
    val excusedTodayCount = remember(activeScopedStudents, attendance, selectedDateIso, holidaysConfig) {
        activeScopedStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.EXCUSED }
    }
    val unrecordedTodayCount = remember(activeScopedStudents, attendance, selectedDateIso, holidaysConfig) {
        activeScopedStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.UNRECORDED }
    }
    val checkedInTodayCount = remember(activeScopedStudents, selectedDateIso) {
        activeScopedStudents.count { isStudentCheckedIn(it, selectedDateIso) }
    }
    val checkedOutTodayCount = remember(activeScopedStudents, selectedDateIso) {
        activeScopedStudents.count { isStudentCheckedOut(it, selectedDateIso) }
    }
    val insideNowTodayCount = remember(activeScopedStudents, selectedDateIso) {
        activeScopedStudents.count { isStudentInsideNow(it, selectedDateIso) }
    }

    val classroomCards = remember(
        activeScopedStudents,
        allowedClasses,
        selectedClassFilter,
        attendance,
        selectedDateIso,
        holidaysConfig
    ) {
        val targetClasses = if (selectedClassFilter == "ALL") {
            allowedClasses
        } else {
            allowedClasses.filter { ArabicTextNormalizer.matchesClass(it, selectedClassFilter) }
        }
        val list = mutableListOf<LiveClassroomCardSummary>()
        targetClasses.forEach { cls ->
            val secs = viewModel.getAllowedSectionsForClass(cls, currentUser)
            secs.forEach { sec ->
                val classStudents = activeScopedStudents.filter { stu ->
                    ArabicTextNormalizer.matchesClass(stu.gradeClass, cls) &&
                        ArabicTextNormalizer.matchesSection(stu.sectionName, sec)
                }
                if (classStudents.isNotEmpty() || secs.size == 1) {
                    list.add(
                        LiveClassroomCardSummary(
                            gradeClass = cls,
                            sectionName = sec,
                            students = classStudents,
                            presentCount = classStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.PRESENT },
                            absentCount = classStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.ABSENT },
                            excusedCount = classStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.EXCUSED },
                            unrecordedCount = classStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.UNRECORDED },
                            lateCount = classStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.LATE },
                            checkedInCount = classStudents.count { isStudentCheckedIn(it, selectedDateIso) },
                            checkedOutCount = classStudents.count { isStudentCheckedOut(it, selectedDateIso) },
                            insideNowCount = classStudents.count { isStudentInsideNow(it, selectedDateIso) }
                        )
                    )
                }
            }
        }
        list
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // 1) بطاقة موجز الدوام وحركة البوابة اليومية
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepEmerald.copy(alpha = 0.35f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            ThreeDIllustrationBox(
                                type = ThreeDIllustrationType.ATTENDANCE_GATE_CLOCK,
                                modifier = Modifier.size(58.dp)
                            )
                            Column {
                                Text(
                                    text = "موجز الدوام وحركة البوابة اليومية",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "إحصائية اليوم لحضور الطلبة وحركة الدخول والانصراف في الصفوف ($selectedDateIso)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (isHolidayToday) {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("live_radar_holiday_lock_banner"),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEF2F2),
                            border = androidx.compose.foundation.BorderStroke(1.4.dp, DangerRed.copy(alpha = 0.7f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = DangerRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "نظام البوابة ومسح الباركود مغلق اليوم (${selectedDateHolidayLabel ?: "عطلة رسمية / أسبوعية"}) — لا يمكن تسجيل دخول أو مغادرة",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DangerRed
                                )
                            }
                        }
                    }

                    // إحصائية الحضور والغياب اليوم (الإجمالي | الحاضرون | الغائبون | المجازون | بانتظار الرصد) بدون أي بتر للحروف نهائياً
                    Text(
                        text = "أولاً: إحصائية الحضور والغياب والإجازات اليوم (اضغط لعرض الأسماء):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("الإجمالي", "$totalStudentsCount", DeepEmerald to LiveRadarDrillFilter.ALL),
                                Triple("الحاضرون", "$presentTodayCount", SuccessGreen to LiveRadarDrillFilter.PRESENT),
                                Triple("الغائبون", "$absentTodayCount", DangerRed to LiveRadarDrillFilter.ABSENT)
                            ).forEach { (lbl, valTxt, pair) ->
                                val (col, filterType) = pair
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            drillTarget = LiveRadarDrillTarget(
                                                title = "كشف $lbl في المدرسة ($selectedDateIso)",
                                                initialFilter = filterType
                                            )
                                        }
                                        .testTag("live_radar_counter_${filterType.name}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = col.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, col.copy(alpha = 0.38f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Text(
                                            text = valTxt,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = col,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = lbl,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple("المجازون", "$excusedTodayCount", InfoBlue to LiveRadarDrillFilter.EXCUSED),
                                Triple("بانتظار الرصد", "$unrecordedTodayCount", Color(0xFF64748B) to LiveRadarDrillFilter.UNRECORDED)
                            ).forEach { (lbl, valTxt, pair) ->
                                val (col, filterType) = pair
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            drillTarget = LiveRadarDrillTarget(
                                                title = "كشف $lbl في المدرسة ($selectedDateIso)",
                                                initialFilter = filterType
                                            )
                                        }
                                        .testTag("live_radar_counter_${filterType.name}"),
                                    shape = RoundedCornerShape(14.dp),
                                    color = col.copy(alpha = 0.10f),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, col.copy(alpha = 0.38f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp, horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = lbl,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = col.copy(alpha = 0.18f)
                                        ) {
                                            Text(
                                                text = valTxt,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = col,
                                                maxLines = 1,
                                                softWrap = false,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // بطاقة حركة بوابة المدرسة اليوم (مع كبسولة واسعة غير مقطوعة نهائياً: اضغط على أي مؤشر لعرض الأسماء)
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFF059669),
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isHolidayToday) Icons.Filled.Lock else Icons.Filled.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isHolidayToday) {
                                            "حركة بوابة المدرسة اليوم (البوابة مغلقة - عطلة)"
                                        } else {
                                            "حركة بوابة المدرسة اليوم"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 2
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.22f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "اضغط على أي مؤشر لعرض الأسماء",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("دخلوا صباحاً", "$checkedInTodayCount طالب", LiveRadarDrillFilter.CHECKED_IN),
                                    Triple("غادروا المدرسة", "$checkedOutTodayCount طالب", LiveRadarDrillFilter.CHECKED_OUT),
                                    Triple("متواجدون حالياً", "$insideNowTodayCount طالب", LiveRadarDrillFilter.INSIDE_NOW)
                                ).forEach { (lbl, valTxt, filterType) ->
                                    val isInsideCard = filterType == LiveRadarDrillFilter.INSIDE_NOW
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable {
                                                drillTarget = LiveRadarDrillTarget(
                                                    title = "كشف الطلاب: ${filterType.arabicTitle} ($selectedDateIso)",
                                                    initialFilter = filterType
                                                )
                                            }
                                            .testTag("live_gate_counter_${filterType.name}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isInsideCard) Color.White else Color.White.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color.White.copy(alpha = if (isInsideCard) 0.9f else 0.38f)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 7.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = valTxt,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isInsideCard) Color(0xFF047857) else Color.White,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Text(
                                                text = lbl,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isInsideCard) DeepForestGreen else Color.White.copy(alpha = 0.95f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Filter by Class
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = selectedClassFilter == "ALL",
                                onClick = { selectedClassFilter = "ALL" },
                                label = {
                                    Text(
                                        text = "جميع الصفوف والشعب (${classroomCards.size})",
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                        items(allowedClasses) { cls ->
                            FilterChip(
                                selected = selectedClassFilter == cls,
                                onClick = { selectedClassFilter = cls },
                                label = {
                                    Text(
                                        text = cls,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        // 2) شبكة بطاقات الصفوف والشعب
        items(classroomCards, key = { "${it.gradeClass}_${it.sectionName}" }) { card ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        drillTarget = LiveRadarDrillTarget(
                            title = "الصف: ${card.gradeClass} — شعبة (${card.sectionName})",
                            gradeClassFilter = card.gradeClass,
                            sectionFilter = card.sectionName,
                            initialFilter = LiveRadarDrillFilter.ALL
                        )
                    }
                    .testTag("live_classroom_card_${card.gradeClass}_${card.sectionName}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.28f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Class,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "الصف: ${card.gradeClass} — شعبة (${card.sectionName})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "اضغط على أي مؤشر أدناه لفتح كشف أسماء الطلاب فوراً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen
                                )
                            }
                        }
                        NoWrapBadge(
                            text = "العدد الكلي: ${card.students.size} طالب",
                            icon = Icons.Filled.Groups,
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        )
                    }

                    // Attendance Breakdown Row (حاضر • غائب • مجاز • بانتظار الرصد) بدون أي بتر للحروف
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        listOf(
                            Triple("حاضر", "${card.presentCount}", SuccessGreen to LiveRadarDrillFilter.PRESENT),
                            Triple("غائب", "${card.absentCount}", DangerRed to LiveRadarDrillFilter.ABSENT),
                            Triple("مجاز", "${card.excusedCount}", InfoBlue to LiveRadarDrillFilter.EXCUSED),
                            Triple("بانتظار الرصد", "${card.unrecordedCount}", Color(0xFF64748B) to LiveRadarDrillFilter.UNRECORDED)
                        ).forEach { (lbl, valTxt, pair) ->
                            val (col, filterType) = pair
                            val itemWeight = if (filterType == LiveRadarDrillFilter.UNRECORDED) 1.45f else 0.88f
                            Surface(
                                modifier = Modifier
                                    .weight(itemWeight)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        drillTarget = LiveRadarDrillTarget(
                                            title = "الصف: ${card.gradeClass} (${card.sectionName}) — $lbl",
                                            gradeClassFilter = card.gradeClass,
                                            sectionFilter = card.sectionName,
                                            initialFilter = filterType
                                        )
                                    },
                                shape = RoundedCornerShape(10.dp),
                                color = col.copy(alpha = 0.10f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.35f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 3.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = valTxt,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = col
                                    )
                                    Text(
                                        text = lbl,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // حركة بوابة المدرسة لهذا الصف اليوم (دخلوا صباحاً • غادروا المدرسة • متواجدون حالياً)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "حركة بوابة المدرسة لهذا الصف اليوم:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("دخلوا صباحاً", "${card.checkedInCount}", LiveRadarDrillFilter.CHECKED_IN),
                                    Triple("غادروا المدرسة", "${card.checkedOutCount}", LiveRadarDrillFilter.CHECKED_OUT),
                                    Triple("متواجدون حالياً", "${card.insideNowCount}", LiveRadarDrillFilter.INSIDE_NOW)
                                ).forEach { (lbl, valTxt, filterType) ->
                                    val isStay = filterType == LiveRadarDrillFilter.INSIDE_NOW
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                drillTarget = LiveRadarDrillTarget(
                                                    title = "الصف: ${card.gradeClass} (${card.sectionName}) — $lbl",
                                                    gradeClassFilter = card.gradeClass,
                                                    sectionFilter = card.sectionName,
                                                    initialFilter = filterType
                                                )
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isStay) DeepEmerald else Color.White,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.40f))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = valTxt,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isStay) Color.White else DeepEmerald
                                            )
                                            Text(
                                                text = lbl,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isStay) Color.White else DeepForestGreen,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            drillTarget = LiveRadarDrillTarget(
                                title = "كشف طلاب الصف: ${card.gradeClass} — شعبة (${card.sectionName})",
                                gradeClassFilter = card.gradeClass,
                                sectionFilter = card.sectionName,
                                initialFilter = LiveRadarDrillFilter.ALL
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald)
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "عرض كشف الأسماء التفصيلي وتوقيتات الدخول والمغادرة وأرقام أولياء الأمور",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                    }
                }
            }
        }
    }

    // 3) كشف الأسماء التفصيلي الفوري عند الضغط (Drill-Down Student List Modal)
    drillTarget?.let { target ->
        var activeModalFilter by remember(target) { mutableStateOf(target.initialFilter) }
        var modalSearchQuery by remember(target) { mutableStateOf("") }

        val baseStudentsForModal = remember(activeScopedStudents, target) {
            activeScopedStudents.filter { stu ->
                (target.gradeClassFilter == null || ArabicTextNormalizer.matchesClass(stu.gradeClass, target.gradeClassFilter)) &&
                    (target.sectionFilter == null || ArabicTextNormalizer.matchesSection(stu.sectionName, target.sectionFilter))
            }
        }

        val matchingStudents = remember(
            baseStudentsForModal,
            activeModalFilter,
            modalSearchQuery,
            attendance,
            selectedDateIso,
            holidaysConfig
        ) {
            val q = modalSearchQuery.trim()
            baseStudentsForModal.filter { stu ->
                val matchesCategory = when (activeModalFilter) {
                    LiveRadarDrillFilter.ALL -> true
                    LiveRadarDrillFilter.INSIDE_NOW -> isStudentInsideNow(stu, selectedDateIso)
                    LiveRadarDrillFilter.CHECKED_IN -> isStudentCheckedIn(stu, selectedDateIso)
                    LiveRadarDrillFilter.CHECKED_OUT -> isStudentCheckedOut(stu, selectedDateIso)
                    LiveRadarDrillFilter.PRESENT -> isStudentPresentOnDate(stu, selectedDateIso) == AttendanceStatus.PRESENT
                    LiveRadarDrillFilter.ABSENT -> isStudentPresentOnDate(stu, selectedDateIso) == AttendanceStatus.ABSENT
                    LiveRadarDrillFilter.EXCUSED -> isStudentPresentOnDate(stu, selectedDateIso) == AttendanceStatus.EXCUSED
                    LiveRadarDrillFilter.UNRECORDED -> isStudentPresentOnDate(stu, selectedDateIso) == AttendanceStatus.UNRECORDED
                    LiveRadarDrillFilter.LATE -> isStudentPresentOnDate(stu, selectedDateIso) == AttendanceStatus.LATE
                }
                val matchesQuery = q.isEmpty() ||
                    stu.fullNameWithFather.contains(q, ignoreCase = true) ||
                    stu.studentName.contains(q, ignoreCase = true) ||
                    stu.gradeClass.contains(q, ignoreCase = true)
                matchesCategory && matchesQuery
            }
        }

        androidx.compose.ui.window.Dialog(
            onDismissRequest = { drillTarget = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .heightIn(max = 840.dp)
                    .testTag("live_radar_drill_down_modal"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, DeepEmerald.copy(alpha = 0.5f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InlineDialogToastBanner(toastAlert = toastAlert)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Assessment,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = target.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "العدد المطابق: ${matchingStudents.size} طالب • التاريخ: $selectedDateIso",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        IconButton(onClick = { drillTarget = null }) {
                            Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = DeepForestGreen)
                        }
                    }

                    // Filter Tabs inside Modal
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(LiveRadarDrillFilter.entries) { fItem ->
                            val isSel = activeModalFilter == fItem
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isSel) DeepEmerald else Color(0xFFF0FDF4),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSel) DeepEmerald else DeepEmerald.copy(alpha = 0.35f)
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { activeModalFilter = fItem }
                            ) {
                                Text(
                                    text = fItem.arabicTitle,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) Color.White else DeepForestGreen,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = modalSearchQuery,
                        onValueChange = { modalSearchQuery = it },
                        placeholder = { Text("ابحث باسم الطالب...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DeepEmerald) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    HorizontalDivider(color = DeepEmerald.copy(alpha = 0.16f))

                    if (matchingStudents.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لا يوجد طلاب ضمن هذا التصنيف (${activeModalFilter.arabicTitle}) حالياً.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextMutedGreen
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(matchingStudents, key = { it.id }) { stu ->
                                val attStatus = isStudentPresentOnDate(stu, selectedDateIso)
                                val gateLog = stu.getGateLogForDate(selectedDateIso)
                                val isInside = isStudentInsideNow(stu, selectedDateIso)
                                val isDeparted = isStudentCheckedOut(stu, selectedDateIso)
                                val stuLeaveForDate = remember(leaveRequests, stu.id, selectedDateIso) {
                                    leaveRequests.firstOrNull { req ->
                                        req.studentId.trim() == stu.id.trim() &&
                                            viewModel.isDateCoveredByLeaveRequest(req, selectedDateIso)
                                    }
                                }
                                val validLeaveReason = stuLeaveForDate?.reason?.trim().orEmpty()
                                val stuBadges = remember(attendance, grades, stu.id) {
                                    viewModel.getStudentAutoBadges(stu.id)
                                }

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.28f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                StudentAvatarWithOverlayBadge(
                                                    base64Data = stu.photoBase64,
                                                    contentDescription = stu.fullNameWithFather,
                                                    badge = stuBadges.firstOrNull(),
                                                    avatarSize = 54.dp,
                                                    boxWidth = 62.dp,
                                                    fallbackIcon = Icons.Filled.Person,
                                                    tint = DeepEmerald,
                                                    onDarkBackground = false
                                                )
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stu.fullNameWithFather,
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen
                                                    )
                                                    Text(
                                                        text = "الصف: ${stu.gradeClass} • شعبة (${stu.sectionName})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = TextMutedGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                            NoWrapBadge(
                                                text = attStatus.arabicLabel,
                                                icon = if (attStatus == AttendanceStatus.UNRECORDED) Icons.Filled.Schedule else Icons.Filled.FactCheck,
                                                containerColor = when (attStatus) {
                                                    AttendanceStatus.PRESENT -> SuccessGreen
                                                    AttendanceStatus.ABSENT -> DangerRed
                                                    AttendanceStatus.EXCUSED -> InfoBlue
                                                    AttendanceStatus.UNRECORDED -> Color(0xFF64748B)
                                                    else -> WarningAmber
                                                },
                                                contentColor = Color.White
                                            )
                                        }

                                        // مربع تفاصيل حركة البوابة (الشارة في سطر أفقي مستقل بالأعلى + أوقات الدخول والمغادرة في سطر أفقي مريح بالأسفل)
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFECFDF5),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = "حالة البوابة اليوم:",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = DeepEmerald
                                                    )
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = when {
                                                            isInside -> DeepEmerald
                                                            isDeparted -> InfoBlue
                                                            else -> Color(0xFF64748B)
                                                        }
                                                    ) {
                                                        Text(
                                                            text = when {
                                                                isInside -> "متواجد بالمدرسة حالياً"
                                                                isDeparted -> "غادر المدرسة"
                                                                else -> "لم يسجل دخول"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                                        )
                                                    }
                                                }

                                                Text(
                                                    text = "وقت الدخول: ${gateLog?.checkInTime?.ifBlank { "لم يسجل" } ?: "لم يسجل"}  |  وقت المغادرة: ${gateLog?.checkOutTime?.ifBlank { "لم يغادر بعد" } ?: "لم يغادر بعد"}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.fillMaxWidth()
                                                )

                                                if (attStatus == AttendanceStatus.EXCUSED && validLeaveReason.length >= 2) {
                                                    Text(
                                                        text = "سبب الإجازة: $validLeaveReason",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = InfoBlue
                                                    )
                                                }
                                            }
                                        }

                                        // Parent Contact & Quick Gate Action Row
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (stu.parentPhone.isNotBlank()) {
                                                OutlinedButton(
                                                    onClick = { ExternalIntents.dialPhoneNumber(context, stu.parentPhone) },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Icon(Icons.Filled.Call, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "اتصال (${stu.parentPhone})",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen,
                                                        maxLines = 1
                                                    )
                                                }
                                                OutlinedButton(
                                                    onClick = {
                                                        ExternalIntents.sendWhatsAppMessage(
                                                            context = context,
                                                            rawPhone = stu.parentPhone,
                                                            message = "تحية طيبة من إدارة ${schoolConfig.schoolName} بخصوص الطالب (${stu.fullNameWithFather})..."
                                                        )
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald)
                                                ) {
                                                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("واتساب", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = DeepForestGreen)
                                                }
                                            }

                                            Button(
                                                onClick = {
                                                    if (!isHolidayToday) {
                                                        viewModel.requireOnlineAndExecute {
                                                            val mode = if (isInside) GateScanMode.CHECK_OUT else GateScanMode.CHECK_IN
                                                            viewModel.recordAttendanceByQrCodeDetailed(
                                                                qrOrUsername = stu.id,
                                                                date = selectedDateIso,
                                                                scanMode = mode
                                                            )
                                                        }
                                                    }
                                                },
                                                enabled = !isHolidayToday,
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isInside) InfoBlue else DeepEmerald,
                                                    contentColor = Color.White,
                                                    disabledContainerColor = Color(0xFFCBD5E1),
                                                    disabledContentColor = Color(0xFF475569)
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = when {
                                                        isHolidayToday -> Icons.Filled.Lock
                                                        isInside -> Icons.AutoMirrored.Filled.Logout
                                                        else -> Icons.AutoMirrored.Filled.Login
                                                    },
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = when {
                                                        isHolidayToday -> "البوابة مغلقة (يوم عطلة)"
                                                        isInside -> "تسجيل مغادرة"
                                                        else -> "تسجيل دخول"
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
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
            }
        }
    }
}

/**
 * ثالثاً: قسم "ملف الطالب الشخصي الشامل" (Comprehensive Student Personal Profile Screen)
 * مستقل تماماً عن نافذة بطاقة الهوية الرقمية الحالية (DigitalStudentIdDialog)، ويتكيف 100% مع الهوية اللونية الذكية حسب الجنس.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentPersonalProfileScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val dailyBehaviors by viewModel.dailyBehaviors.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }

    val scopedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser)
    }
    var selectedAdminStudentId by remember { mutableStateOf("") }
    val activeStudent = remember(scopedStudents, students, currentUser, selectedParentChildId, selectedAdminStudentId) {
        when (currentUser?.role) {
            UserRole.STUDENT, UserRole.PARENT -> {
                scopedStudents.firstOrNull()
                    ?: students.firstOrNull { it.id == currentUser?.linkedStudentId }
            }
            else -> {
                scopedStudents.firstOrNull { it.id == selectedAdminStudentId }
                    ?: scopedStudents.firstOrNull()
            }
        }
    }

    var showDigitalIdModal by remember { mutableStateOf(false) }

    if (activeStudent == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ملف الطالب الشخصي الشامل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                    Text(
                        text = "لا يوجد سجل طالب مرتبط بهذا الحساب حالياً.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    // Real-time Attendance Analytics (100% Real Data, Zero Mock Numbers!)
    val realPresentDays = remember(attendance, students, activeStudent.id) {
        viewModel.getTotalPresentDays(activeStudent.id)
    }
    val realAbsentDays = remember(attendance, students, activeStudent.id) {
        viewModel.getTotalAbsenceDays(activeStudent.id)
    }
    val realExcusedDays = remember(attendance, students, activeStudent.id) {
        viewModel.getTotalExcusedDays(activeStudent.id)
    }
    val realLateDays = remember(attendance, students, activeStudent.id) {
        viewModel.getTotalLateDays(activeStudent.id)
    }
    val totalRecordedDays = realPresentDays + realAbsentDays + realExcusedDays + realLateDays
    val attendanceCommitmentPercent = remember(realPresentDays, realExcusedDays, totalRecordedDays) {
        if (totalRecordedDays <= 0) 0 else (((realPresentDays + realExcusedDays) * 100.0) / totalRecordedDays).toInt().coerceIn(0, 100)
    }
    val todayStatus = remember(activeStudent, attendance, todayIso) {
        viewModel.getStudentStatusForDate(activeStudent, todayIso)
    }

    // Academic Grades & Subject Progress
    val studentGrades = remember(grades, activeStudent.id, activeStudent.gradeClass, schoolConfig) {
        val recorded = grades.filter { it.studentId == activeStudent.id }
        val configuredSubjects = viewModel.getSubjectsForClass(activeStudent.gradeClass, null)
        val allSubNames = (configuredSubjects + recorded.map { it.subjectName })
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { ArabicTextNormalizer.normalizeText(it) }
        allSubNames.map { subName ->
            val found = recorded.firstOrNull {
                ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(subName)
            }
            val dynamicScale = found?.maxScoreScale ?: viewModel.getDefaultMaxScoreForClassAndSubject(activeStudent.gradeClass, subName)
            found?.copy(maxScoreScale = dynamicScale) ?: SubjectGrade(
                id = "prof_${activeStudent.id}_$subName",
                studentId = activeStudent.id,
                studentName = activeStudent.studentName,
                gradeClass = activeStudent.gradeClass,
                subjectName = subName,
                dailyScore = 0,
                month1Score = 0,
                month2Score = 0,
                midYearScore = 0,
                finalExamScore = 0,
                maxScoreScale = dynamicScale,
                schoolId = schoolConfig.schoolId
            )
        }
    }
    val scoredSubjects = remember(studentGrades) {
        studentGrades.filter {
            it.dailyScore > 0 || it.month1Score > 0 || it.month2Score > 0 || it.midYearScore > 0 || it.finalExamScore > 0
        }
    }
    val cumulativeAverage = remember(scoredSubjects) {
        if (scoredSubjects.isEmpty()) 0 else scoredSubjects.map { it.normalizedPercentage }.average().toInt()
    }
    val studentBadges = remember(attendance, grades, activeStudent.id) {
        viewModel.getStudentAutoBadges(activeStudent.id)
    }
    val studentBehaviors = remember(dailyBehaviors, activeStudent.id) {
        dailyBehaviors.filter { it.studentId == activeStudent.id }.sortedByDescending { it.date }
    }

    val allNotifications by viewModel.notifications.collectAsState()
    val allReports by viewModel.reports.collectAsState()
    val scopedNotifications = remember(allNotifications, currentUser, activeStudent.id) {
        viewModel.getScopedNotificationsForUser(currentUser).take(6)
    }
    val studentReports = remember(allReports, activeStudent.id, activeStudent.studentName) {
        allReports.filter {
            it.studentName.trim() == activeStudent.studentName.trim() ||
                it.studentName.trim() == activeStudent.fullNameWithFather.trim()
        }.take(5)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .testTag("student_personal_profile_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 32.dp)
    ) {
        // 1. Royal Emerald 3D Hero Header Banner addressing the student by gender
        item {
            ThreeDHeroHeaderCard(
                title = if (activeStudent.isFemale) {
                    "مرحباً بكِ يا ${activeStudent.studentName}"
                } else {
                    "مرحباً بك يا ${activeStudent.studentName}"
                },
                subtitle = "${activeStudent.studentTitleLabel}: ${activeStudent.fullNameWithFather} • الصف: ${activeStudent.gradeClass} (شعبة ${activeStudent.sectionName})",
                rightPillText = if (activeStudent.isFemale) "ملف الطالبة والبيانات الشاملة" else "ملف الطالب والبيانات الشاملة",
                rightPillIcon = Icons.Filled.AccountBox,
                leftPillText = "كود الطالب: ${activeStudent.studentUsername.ifBlank { activeStudent.id }}",
                leftPillIcon = Icons.Filled.Badge,
                illustrationType = ThreeDIllustrationType.ATTENDANCE_GATE_CLOCK,
                bottomActionText = "عرض بطاقة الهوية المدرسية الرقمية والباركود",
                bottomActionIcon = Icons.Filled.QrCode2,
                bottomActionTestTag = "student_profile_open_digital_id_btn",
                onBottomActionClick = { showDigitalIdModal = true }
            )
        }

        // 2. Comprehensive Personal & Academic Identity Card (Unclipped Full-Text Info Rows)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Base64AvatarImage(
                                base64Data = activeStudent.photoBase64,
                                fallbackIcon = Icons.Filled.Person,
                                contentDescription = activeStudent.fullNameWithFather,
                                size = 48.dp,
                                tint = RoyalGreen
                            )
                            Column {
                                Text(
                                    text = "البيانات الشخصية والأكاديمية الشاملة",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "${schoolConfig.schoolName} • العام الدراسي ${schoolConfig.academicYear}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AdaptiveSoftSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                        ) {
                            Text(
                                text = activeStudent.enrollmentStatusEnum.arabicLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    val personalInfoRows = buildList {
                        add("الاسم الرباعي الكامل" to activeStudent.fullNameWithFather)
                        add("الصف الدراسي والشعبة" to "${activeStudent.gradeClass} — شعبة (${activeStudent.sectionName})")
                        add("الكود الأكاديمي / اسم المستخدم" to activeStudent.studentUsername.ifBlank { activeStudent.id })
                        add("الجنس والعمر" to "الجنس: ${activeStudent.normalizedGenderLabel} • العمر: ${formatArabicAgeYearsLabel(activeStudent.effectiveAgeYears)}")
                        add("ولي الأمر ورقم التواصل" to "${activeStudent.parentName.ifBlank { "ولي الأمر" }} • ${activeStudent.parentPhone.ifBlank { "غير مسجل" }}")
                        add("حالة الدوام اليومي (${todayIso})" to todayStatus.arabicLabel)
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        personalInfoRows.forEach { (labelTitle, valueDetail) ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                color = AdaptiveSoftSurface.copy(alpha = 0.65f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = labelTitle,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = valueDetail,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 3. Real-Time Attendance & Discipline Analytics (Unified White & Royal Emerald Cards with Circular Vector Indicators)
        item {
            val animatedSweep by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (totalRecordedDays == 0) 0f else (attendanceCommitmentPercent / 100f) * 360f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 650),
                label = "attendance_ring_sweep"
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "سجل المواظبة والانضباط المدرسي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Text(
                                text = if (totalRecordedDays == 0) {
                                    "تبدأ العدادات من 0 وتُحدث تلقائياً مع كل رصد يومي للحضور"
                                } else {
                                    "إجمالي أيام الدوام المرصودة فعلياً: $totalRecordedDays يوم"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Box(
                            modifier = Modifier.size(64.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val ringTrackColor = AdaptiveSoftContainer
                            val ringActiveColor = RoyalGreen
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokePx = 6.5.dp.toPx()
                                drawArc(
                                    color = ringTrackColor,
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokePx)
                                )
                                drawArc(
                                    color = ringActiveColor,
                                    startAngle = -90f,
                                    sweepAngle = animatedSweep,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = strokePx,
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            }
                            Text(
                                text = if (totalRecordedDays == 0) "0%" else "$attendanceCommitmentPercent%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                        }
                    }

                    val attendanceStatCards = listOf(
                        Triple("أيام الحضور", "$realPresentDays يوم", Icons.Filled.CheckCircle),
                        Triple("أيام الغياب", "$realAbsentDays يوم", Icons.Filled.EventBusy),
                        Triple("الإجازات", "$realExcusedDays يوم", Icons.Filled.EventAvailable),
                        Triple("التأخر الصباحي", "$realLateDays يوم", Icons.Filled.Schedule)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        attendanceStatCards.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { (statLabel, statVal, statIcon) ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(16.dp),
                                        color = AdaptiveSoftSurface.copy(alpha = 0.7f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 12.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Surface(
                                                shape = CircleShape,
                                                color = Color.White,
                                                border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder),
                                                modifier = Modifier.size(36.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        imageVector = statIcon,
                                                        contentDescription = statLabel,
                                                        tint = RoyalGreen,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = statVal,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = statLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepEmerald,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Academic Record & Subject Progress Indicator
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "السجل الأكاديمي ودرجات المواد الدراسية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Text(
                                text = "متابعة مستوى التقدم في المواد الدراسية والمعدل التراكمي العام",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = AdaptiveSoftSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                        ) {
                            Text(
                                text = if (scoredSubjects.isEmpty()) "المعدل: قيد الرصد" else "المعدل العام: $cumulativeAverage%",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    if (studentBadges.isNotEmpty() || cumulativeAverage >= 85 || (realPresentDays > 0 && realAbsentDays == 0)) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val badgesTextList = buildList {
                                if (cumulativeAverage >= 90) add("وسام التفوق الأكاديمي الممتاز (${activeStudent.excellingGenderLabel})")
                                else if (cumulativeAverage >= 80) add("وسام التميز العلمي الجيد جداً")
                                if (realPresentDays > 0 && realAbsentDays == 0) add("وسام الانضباط والمواظبة الكاملة")
                                studentBadges.forEach { add(it.arabicTitle) }
                            }.distinct()
                            badgesTextList.forEach { badgeTitle ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = AdaptiveSoftContainer,
                                    border = androidx.compose.foundation.BorderStroke(1.dp, RoyalGreen.copy(alpha = 0.4f))
                                ) {
                                    Text(
                                        text = badgeTitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (studentGrades.isEmpty()) {
                        Text(
                            text = "لا توجد مواد مرصودة لهذا الصف حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            studentGrades.forEach { grade ->
                                val hasAnyScore = grade.dailyScore > 0 || grade.month1Score > 0 || grade.month2Score > 0 || grade.midYearScore > 0 || grade.finalExamScore > 0
                                val pct = if (hasAnyScore) grade.normalizedPercentage.coerceIn(0, 100) else 0
                                val barFraction by androidx.compose.animation.core.animateFloatAsState(
                                    targetValue = pct / 100f,
                                    animationSpec = androidx.compose.animation.core.tween(500),
                                    label = "subject_progress_${grade.subjectName}"
                                )
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = AdaptiveSoftSurface.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder.copy(alpha = 0.65f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = grade.subjectName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = if (hasAnyScore) {
                                                    "${grade.finalAverage} / ${grade.maxScoreScale} ($pct%)"
                                                } else {
                                                    "بانتظار الرصد"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (hasAnyScore) RoyalGreen else Color(0xFF64748B)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(AdaptiveSoftContainer)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(barFraction)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(50))
                                                    .background(
                                                        Brush.horizontalGradient(
                                                            colors = listOf(RoyalGreen, DeepEmerald)
                                                        )
                                                    )
                                            )
                                        }
                                        if (hasAnyScore) {
                                            Text(
                                                text = "يومي: ${grade.dailyScore} • شهر 1: ${grade.month1Score} • شهر 2: ${grade.month2Score} • نصف السنة: ${grade.midYearScore} • نهائي: ${grade.finalExamScore}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. E-Wallet Balance, Transactions & Behavioral Log Summary Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "محفظة الحانوت المدرسي والسجل السلوكي",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = AdaptiveSoftSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "رصيد محفظة الحانوت",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${activeStudent.walletBalanceIqd} د.ع",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "السقف اليومي: ${activeStudent.dailySpendLimitIqd} د.ع",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = AdaptiveSoftSurface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "الملاحظات التربوية",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${studentBehaviors.size} ملاحظة موثقة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = if (studentBehaviors.isEmpty()) "سجل انضباطي ناصع" else "آخر تحديث: ${studentBehaviors.first().date}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (activeStudent.walletTransactions.isNotEmpty()) {
                        Text(
                            text = "آخر حركات محفظة الحانوت:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            activeStudent.walletTransactions.take(4).forEach { tx ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AdaptiveSoftSurface.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${tx.type.arabicLabel} • ${tx.note.ifBlank { tx.actorName }}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = "${tx.date} ${tx.timestamp}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "${tx.amountIqd} د.ع",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = RoyalGreen
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (studentBehaviors.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            studentBehaviors.take(5).forEach { beh ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AdaptiveSoftSurface.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${beh.subjectName.ifBlank { "متابعة عامة" }} — ${beh.teacherName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = beh.date,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepEmerald,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Text(
                                            text = "التقييم: ${beh.participationLevel.arabicLabel} • ${beh.behaviorNote.ifBlank { "انضباط ومشاركة متميزة" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. Administration Announcements & Official Notifications Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, AdaptiveSoftBorder),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تبليغات الإدارة والإشعارات المدرسية",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )

                    if (schoolConfig.urgentAlert.isActive && schoolConfig.urgentAlert.message.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = AdaptiveSoftSurface,
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, RoyalGreen),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = schoolConfig.urgentAlert.authorName.ifBlank { "تبليغ رسمي من إدارة المدرسة" },
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = schoolConfig.urgentAlert.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (scopedNotifications.isEmpty() && studentReports.isEmpty() && (!schoolConfig.urgentAlert.isActive || schoolConfig.urgentAlert.message.isBlank())) {
                        Text(
                            text = "لا توجد تبليغات إدارية جديدة حالياً.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            scopedNotifications.forEach { notif ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = AdaptiveSoftSurface.copy(alpha = 0.55f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AdaptiveSoftBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = notif.title,
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = notif.createdAt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = DeepEmerald
                                            )
                                        }
                                        Text(
                                            text = notif.body,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDigitalIdModal) {
        DigitalStudentIdDialog(
            student = activeStudent,
            schoolConfig = schoolConfig,
            badges = studentBadges,
            todayAttendanceLabel = todayStatus.arabicLabel,
            todayIso = todayIso,
            toastAlert = toastAlert,
            onDismissToast = { viewModel.dismissToastAlert() },
            onSavePng = {
                val res = DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = activeStudent,
                    schoolConfig = schoolConfig,
                    badges = studentBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert("تم حفظ الهوية المدرسية بدقة عالية (PNG) في المعرض", isSuccess = true)
                } else {
                    viewModel.showToastAlert("تعذر حفظ الصورة: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = activeStudent,
                    schoolConfig = schoolConfig,
                    badges = studentBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert("تم تصدير الهوية المدرسية بصيغة PDF بنجاح", isSuccess = true)
                } else {
                    viewModel.showToastAlert("تعذر تصدير PDF: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { showDigitalIdModal = false }
        )
    }
}

/**
 * قسم "النقل والخطوط المدرسية" المستقل بمواصفات عالمية
 * - بنر علوي بكامل عرض الشاشة مع التموج المعهود (Full-Width Wavy Emerald Hero Banner)
 * - أنميشن فيكتور ثلاثي الأبعاد بمنحنيات بيزيير لباص مدرسي حديث وعجلات تدور بنعومة فوق مسار متموج
 * - مشهد مسار بانورامي متموج بكامل عرض الشاشة (المسار التفاعلي المباشر لحركة الخط)
 * - فصل تام لواجهة ولي الأمر والطالب عن واجهة المدير مع بطاقة كشف هوية السائق المعتمد
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchoolTransportFleetScreen(viewModel: SchoolViewModel) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val canControlFleet = isAdmin || isTeacher

    val scopedStudents = remember(students, currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser).filter { it.isActive }
    }
    val allActiveSchoolStudents = remember(students, schoolConfig.schoolId) {
        students.filter { (it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId) && it.isActive }
    }
    val subscribedStudents = remember(allActiveSchoolStudents) {
        allActiveSchoolStudents.filter { it.isBusSubscribed }
    }
    val unsubscribedStudents = remember(allActiveSchoolStudents) {
        allActiveSchoolStudents.filter { !it.isBusSubscribed }
    }

    var selectedClassFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var showAddStudentToBusDialog by remember { mutableStateOf(false) }
    var showVerifiedDriverIdModal by remember { mutableStateOf(false) }
    var showEditDriverSettingsModal by remember { mutableStateOf(false) }

    val availableClasses = remember(subscribedStudents) {
        subscribedStudents.map { ArabicTextNormalizer.cleanPureClassName(it.gradeClass) }
            .filter { it.isNotBlank() }
            .distinct()
    }

    val filteredSubscribedStudents = remember(subscribedStudents, selectedClassFilter, searchQuery) {
        subscribedStudents.filter { stu ->
            val matchesClass = selectedClassFilter == "الكل" ||
                ArabicTextNormalizer.cleanPureClassName(stu.gradeClass) == selectedClassFilter
            val q = searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                stu.fullNameWithFather.contains(q, ignoreCase = true) ||
                stu.parentPhone.contains(q) ||
                stu.busRouteName.contains(q, ignoreCase = true)
            matchesClass && matchesSearch
        }
    }

    val groupedByClassAndSection = remember(filteredSubscribedStudents) {
        filteredSubscribedStudents.groupBy { stu ->
            val pureCls = ArabicTextNormalizer.cleanPureClassName(stu.gradeClass)
            val pureSec = ArabicTextNormalizer.cleanPureSectionName(stu.sectionName)
            "$pureCls - شعبة $pureSec"
        }
    }

    val resolvedBusEvent = remember(schoolConfig.activeBusEvent) {
        BusEventType.entries.firstOrNull { it.name == schoolConfig.activeBusEvent } ?: BusEventType.MORNING_DEPARTURE
    }
    var activeBusState by remember(resolvedBusEvent) { mutableStateOf(resolvedBusEvent) }

    val activeStationIndex = when (activeBusState) {
        BusEventType.MORNING_DEPARTURE -> 1
        BusEventType.ARRIVED_AT_SCHOOL -> 2
        BusEventType.RETURN_HOME_DEPARTURE -> 0
    }

    // Custom Full-Width Wavy Emerald Hero Banner Shape (Bezier Wave Bottom matching system identity)
    val fullWidthWavyBannerShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val bottomWaveDepth = (h * 0.11f).coerceIn(18f, 34f)
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h - bottomWaveDepth)
            cubicTo(
                w * 0.68f, h + bottomWaveDepth * 0.95f,
                w * 0.32f, h - bottomWaveDepth * 1.45f,
                0f, h - bottomWaveDepth * 0.35f
            )
            close()
        }
    }

    val myChildForParent = remember(scopedStudents, selectedParentChildId) {
        scopedStudents.firstOrNull { it.id == selectedParentChildId } ?: scopedStudents.firstOrNull()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("school_transport_fleet_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) البنر العلوي بكامل عرض الشاشة مع التموج المعهود + مركبة 3D بيزيير حديثة
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 10.dp,
                        shape = fullWidthWavyBannerShape,
                        ambientColor = Color(0xFF047857).copy(alpha = 0.32f),
                        spotColor = Color(0xFF0F3D5E).copy(alpha = 0.38f)
                    )
                    .clip(fullWidthWavyBannerShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF059669),
                                Color(0xFF047857),
                                Color(0xFF0F3D5E)
                            ),
                            start = Offset(0f, 0f),
                            end = Offset(980f, 680f)
                        )
                    )
            ) {
                // Halftone dot matrix & inner Bezier wave sheen
                Canvas(modifier = Modifier.matchParentSize()) {
                    val w = size.width
                    val h = size.height

                    val innerWave = Path().apply {
                        moveTo(0f, h * 0.70f)
                        cubicTo(
                            w * 0.35f, h * 0.56f,
                            w * 0.70f, h * 0.86f,
                            w, h * 0.72f
                        )
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(
                        path = innerWave,
                        color = Color(0xFF0F3D5E).copy(alpha = 0.28f)
                    )

                    val step = 11.dp.toPx()
                    val r = 1.5.dp.toPx()
                    for (row in 0..5) {
                        for (col in 0..7) {
                            val alpha = ((1f - (row + col) / 13f) * 0.22f).coerceIn(0.03f, 0.22f)
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = r,
                                center = Offset(w * 0.04f + col * step, h * 0.10f + row * step)
                            )
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = r,
                                center = Offset(w * 0.96f - col * step, h * 0.84f - row * step)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Frosted Capsules
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DirectionsBus,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (canControlFleet) "إدارة الأسطول والنقل المدرسي" else "متابعة خط النقل المدرسي",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.55f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VerifiedUser,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (canControlFleet) {
                                        "${subscribedStudents.size} طالب مشمول بالخط"
                                    } else {
                                        myChildForParent?.fullNameWithFather ?: "حساب الطالب"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFDE68A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    // Title + 3D Isometric Bezier School Bus & Modern Fleet Animation on Left
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "النقل والخطوط المدرسية",
                                style = MaterialTheme.typography.headlineSmall,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (canControlFleet) {
                                    "إدارة حركة الخطوط المدرسية وإرسال تنبيهات الانطلاق والوصول الآمن لأولياء الأمور"
                                } else {
                                    "متابعة المسار التفاعلي المباشر لرحلة الخط المدرسي وبيانات السائق المعتمد"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.92f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // 3D Isometric Bezier School Bus & Modern Fleet Animation
                        ThreeDIsometricBezierSchoolBusHeroAnimation(
                            modifier = Modifier.size(110.dp)
                        )
                    }
                }
            }
        }

        // 2) المسار التفاعلي المباشر لحركة الخط (مشهد مسار بانورامي متموج بكامل عرض الشاشة بدون إطار ضيق)
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "المسار التفاعلي المباشر لحركة الخط",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46)
                    )
                    Text(
                        text = formatBusTimestampPretty(schoolConfig.activeBusEventTimestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF047857)
                    )
                }

                FullWidthBezierWaveRoadTrack(
                    activeBusState = activeBusState,
                    activeStationIndex = activeStationIndex,
                    lastUpdatedTimestamp = schoolConfig.activeBusEventTimestamp
                )
            }
        }

        // 3) فصل تام بين واجهة ولي الأمر/الطالب وواجهة المدير/المشرف
        if (!canControlFleet) {
            // =========================================================================
            // واجهة الطالب وولي الأمر الخاصة بطفله فقط (بدون قائمة طلاب المدرسة أو شريط البحث)
            // =========================================================================
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (myChildForParent == null) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                        ) {
                            Text(
                                text = "لا توجد بيانات طالب مرتبطة بهذا الحساب حالياً.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.padding(20.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val effectiveRouteName = myChildForParent.busRouteName
                            .takeIf { it.isNotBlank() } ?: "الخط الصباحي والمسائي المعتمد"
                        val effectiveDriverName = myChildForParent.busDriverName
                            .takeIf { it.isNotBlank() } ?: schoolConfig.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" }
                        val effectiveVehicleNumber = myChildForParent.busVehicleNumber
                            .takeIf { it.isNotBlank() } ?: schoolConfig.busVehicleNumber.ifBlank { "مركبة النقل المدرسي الحديثة" }

                        // بطاقة "بيانات خط الطالب والسائق المعتمد"
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("parent_student_bus_route_driver_card"),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            border = BorderStroke(1.2.dp, Color(0xFFA7F3D0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = Color(0xFF065F46),
                                            modifier = Modifier.size(42.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Badge,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFDE68A),
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = "بيانات خط الطالب والسائق المعتمد",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF065F46)
                                            )
                                            Text(
                                                text = "الطالب: ${myChildForParent.fullNameWithFather} (${ArabicTextNormalizer.cleanPureClassName(myChildForParent.gradeClass)} - شعبة ${ArabicTextNormalizer.cleanPureSectionName(myChildForParent.sectionName)})",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (myChildForParent.isBusSubscribed) Color(0xFFECFDF5) else Color(0xFFFEF3C7),
                                        border = BorderStroke(
                                            1.dp,
                                            if (myChildForParent.isBusSubscribed) Color(0xFF059669) else Color(0xFFD97706)
                                        )
                                    ) {
                                        Text(
                                            text = if (myChildForParent.isBusSubscribed) "مشمول بالخط ✓" else "غير مشترك",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (myChildForParent.isBusSubscribed) Color(0xFF065F46) else Color(0xFFB45309),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                HorizontalDivider(color = Color(0xFFD1FAE5))

                                // 3 Structured Info Rows: (اسم الخط والمنطقة، اسم السائق، رقم المركبة)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "اسم الخط والمنطقة:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            Text(
                                                text = effectiveRouteName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF064E3B)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "اسم السائق المعتمد:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            Text(
                                                text = effectiveDriverName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF064E3B)
                                            )
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "نوع ورقم المركبة:",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF047857)
                                            )
                                            Text(
                                                text = effectiveVehicleNumber,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF064E3B)
                                            )
                                        }
                                    }
                                }

                                // زر "كشف هوية وتفاصيل السائق المعتمد" (يظهر إذا وافق المدير allowParentToViewDriverIdentity == true)
                                if (schoolConfig.allowParentToViewDriverIdentity) {
                                    Button(
                                        onClick = { showVerifiedDriverIdModal = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("reveal_verified_driver_identity_btn"),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF064E3B),
                                            contentColor = Color.White
                                        ),
                                        border = BorderStroke(1.5.dp, Color(0xFFFBBF24))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.VerifiedUser,
                                            contentDescription = null,
                                            tint = Color(0xFFFDE68A),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "كشف هوية وتفاصيل السائق المعتمد",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
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
        } else {
            // =========================================================================
            // واجهة المدير والمشرف (أزرار التحكم بالرحلة + إعدادات هوية السائق + سجل الطلاب المشمولين)
            // =========================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "أزرار التحكم الفوري بالرحلة وإشعار أولياء الأمور:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            )
                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = { showEditDriverSettingsModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFF059669)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("open_driver_identity_settings_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Badge,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "هوية السائق",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        val controlButtons = listOf(
                            Triple(BusEventType.MORNING_DEPARTURE, "بدء الانطلاق الصباحي", Icons.Filled.WbSunny),
                            Triple(BusEventType.ARRIVED_AT_SCHOOL, "تأكيد الوصول للمدرسة", Icons.Filled.Verified),
                            Triple(BusEventType.RETURN_HOME_DEPARTURE, "انطلاق رحلة العودة للمنازل", Icons.Filled.Home)
                        )
                        controlButtons.forEach { (busEvent, fullButtonTitle, btnIcon) ->
                            val isSelected = activeBusState == busEvent
                            Button(
                                onClick = {
                                    activeBusState = busEvent
                                    viewModel.sendBusTrackingAlert(busEvent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .testTag("fleet_control_btn_${busEvent.name}"),
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSelected) Color(0xFF047857) else Color(0xFF064E3B),
                                    contentColor = Color.White
                                ),
                                border = if (isSelected) BorderStroke(1.5.dp, Color(0xFFFDE68A)) else null
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = btnIcon,
                                            contentDescription = null,
                                            tint = if (isSelected) Color(0xFFFDE68A) else Color(0xFF6EE7B7),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = fullButtonTitle,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    if (isSelected) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFFFDE68A).copy(alpha = 0.22f),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                        ) {
                                            Text(
                                                text = "المسار النشط الآن",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFFDE68A),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
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

            // سجل الطلاب المشتركين بالخط (للمدير والمشرف فقط!)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "قائمة الطلاب المشمولين بالنقل المدرسي",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "مقسمون حسب الصف والشعبة (${filteredSubscribedStudents.size} طالب مشترك)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Button(
                                onClick = { showAddStudentToBusDialog = true },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("add_student_to_bus_line_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "+ شمول طالب بالخط",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("بحث باسم الطالب أو اسم الخط أو هاتف ولي الأمر...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (availableClasses.isNotEmpty()) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item {
                                    FilterChip(
                                        selected = selectedClassFilter == "الكل",
                                        onClick = { selectedClassFilter = "الكل" },
                                        label = { Text("جميع الصفوف (${subscribedStudents.size})", fontWeight = FontWeight.Bold) }
                                    )
                                }
                                items(availableClasses) { clsName ->
                                    FilterChip(
                                        selected = selectedClassFilter == clsName,
                                        onClick = { selectedClassFilter = clsName },
                                        label = { Text(clsName, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (groupedByClassAndSection.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DirectionsBus,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "لا يوجد طلاب مشمولون بخدمة النقل المدرسي حالياً",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "يمكنك شمول أي طالب بالخط من نافذة إضافة/تعديل الطالب أو عبر زر (+ شمول طالب بالخط) أعلاه.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF047857),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                groupedByClassAndSection.entries.forEach { (classSecTitle, classStudents) ->
                    item(key = "bus_group_header_$classSecTitle") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF064E3B)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = classSecTitle,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFFFDE68A).copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = "${classStudents.size} طالب",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFDE68A),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    items(classStudents, key = { "bus_sub_stu_${it.id}" }) { stu ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            border = BorderStroke(1.dp, Color(0xFFD1FAE5))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Base64AvatarImage(
                                        base64Data = stu.photoBase64,
                                        fallbackIcon = Icons.Filled.DirectionsBus,
                                        contentDescription = stu.fullNameWithFather,
                                        size = 44.dp
                                    )
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = stu.fullNameWithFather,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                                        ) {
                                            Text(
                                                text = "الخط: ${stu.busRouteName.ifBlank { "الخط الصباحي والمسائي" }}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF047857),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                        Text(
                                            text = "ولي الأمر: ${stu.parentName} • هاتف: ${stu.parentPhone.ifBlank { "غير مسجل" }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = {
                                        viewModel.updateStudentBusSubscription(
                                            studentId = stu.id,
                                            isSubscribed = false,
                                            busRouteName = stu.busRouteName
                                        )
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.65f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("cancel_bus_sub_${stu.id}")
                                ) {
                                    Text(
                                        text = "إلغاء الشمول",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFDC2626),
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

    // نافذة بطاقة الهوية الرسمية الموثقة للسائق المعتمد (تفتح لولي الأمر عند الضغط على "كشف هوية وتفاصيل السائق المعتمد")
    if (showVerifiedDriverIdModal) {
        val targetStu = myChildForParent
        val dName = targetStu?.busDriverName?.takeIf { it.isNotBlank() }
            ?: schoolConfig.busDriverName.ifBlank { "السائق المعتمد للخط المدرسي" }
        val dPhone = targetStu?.busDriverPhone?.takeIf { it.isNotBlank() }
            ?: schoolConfig.busDriverPhone.ifBlank { schoolConfig.phone }
        val dVehicle = targetStu?.busVehicleNumber?.takeIf { it.isNotBlank() }
            ?: schoolConfig.busVehicleNumber.ifBlank { "مركبة النقل المدرسي الحديثة" }
        val dRoute = targetStu?.busRouteName?.takeIf { it.isNotBlank() }
            ?: "الخط الصباحي والمسائي المعتمد"

        AlertDialog(
            onDismissRequest = { showVerifiedDriverIdModal = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.VerifiedUser,
                        contentDescription = null,
                        tint = Color(0xFF059669)
                    )
                    Text(
                        text = "بطاقة هوية السائق المعتمد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF064E3B)
                    )
                }
            },
            text = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verified_driver_identity_dialog_card"),
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFF042F24),
                    border = BorderStroke(1.5.dp, Color(0xFFFBBF24))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Driver Avatar / Photo with Golden Ring
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF065F46),
                            border = BorderStroke(2.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.size(72.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Base64AvatarImage(
                                    base64Data = schoolConfig.busDriverPhotoBase64,
                                    fallbackIcon = Icons.Filled.Person,
                                    contentDescription = dName,
                                    size = 68.dp
                                )
                            }
                        }

                        Text(
                            text = dName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        // Official School Administration Verification Stamp
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFFDE68A).copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "ختم توثيق واعتماد إدارة (${schoolConfig.schoolName})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFDE68A)
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFF10B981).copy(alpha = 0.35f))

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "الخط والمنطقة: $dRoute",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA7F3D0)
                            )
                            Text(
                                text = "نوع ورقم المركبة: $dVehicle",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "رقم هاتف السائق: ${dPhone.ifBlank { "متاح عبر إدارة المدرسة" }}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFDE68A)
                            )
                        }

                        if (dPhone.isNotBlank()) {
                            Button(
                                onClick = { ExternalIntents.dialPhoneNumber(context, dPhone) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF10B981),
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Call,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "اتصال مباشر بالسائق المعتمد",
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showVerifiedDriverIdModal = false }) {
                    Text("إغلاق البطاقة", fontWeight = FontWeight.ExtraBold)
                }
            }
        )
    }

    // نافذة إعدادات بيانات السائق المعتمد وصلاحية كشف الهوية لأولياء الأمور (للمدير)
    if (showEditDriverSettingsModal && isAdmin) {
        var editDriverName by remember(schoolConfig) { mutableStateOf(schoolConfig.busDriverName) }
        var editDriverPhone by remember(schoolConfig) { mutableStateOf(schoolConfig.busDriverPhone) }
        var editVehicleNumber by remember(schoolConfig) { mutableStateOf(schoolConfig.busVehicleNumber) }
        var editAllowIdentity by remember(schoolConfig) { mutableStateOf(schoolConfig.allowParentToViewDriverIdentity) }

        AlertDialog(
            onDismissRequest = { showEditDriverSettingsModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.Badge, contentDescription = null, tint = Color(0xFF059669))
                    Text("إعدادات السائق المعتمد وهوية المركبة", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editDriverName,
                        onValueChange = { editDriverName = it },
                        label = { Text("اسم السائق المعتمد الكامل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDriverPhone,
                        onValueChange = { editDriverPhone = it },
                        label = { Text("رقم هاتف السائق للاتصال المباشر") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editVehicleNumber,
                        onValueChange = { editVehicleNumber = it },
                        label = { Text("نوع ورقم المركبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFF059669))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "السماح لولي الأمر بكشف هوية وتفاصيل السائق المعتمد",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF065F46),
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = editAllowIdentity,
                                onCheckedChange = { editAllowIdentity = it }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSchoolBusDriverConfig(
                            driverName = editDriverName,
                            driverPhone = editDriverPhone,
                            vehicleNumber = editVehicleNumber,
                            allowParentToViewDriverIdentity = editAllowIdentity
                        )
                        showEditDriverSettingsModal = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("حفظ واعتماد", fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDriverSettingsModal = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // One-Tap Modal to Add / Subscribe Any Student to the School Bus Line (for Principal / Supervisor)
    if (showAddStudentToBusDialog && canControlFleet) {
        var modalSearch by remember { mutableStateOf("") }
        var modalRouteName by remember { mutableStateOf("") }
        val modalCandidates = remember(unsubscribedStudents, modalSearch) {
            val q = modalSearch.trim()
            if (q.isEmpty()) unsubscribedStudents
            else unsubscribedStudents.filter {
                it.fullNameWithFather.contains(q, ignoreCase = true) ||
                    it.gradeClass.contains(q, ignoreCase = true)
            }
        }

        AlertDialog(
            onDismissRequest = { showAddStudentToBusDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.DirectionsBus, contentDescription = null, tint = Color(0xFF059669))
                    Text("شمول طالب بخدمة النقل المدرسي", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = modalRouteName,
                        onValueChange = { modalRouteName = it },
                        label = { Text("اسم الخط / السائق أو المنطقة (اختياري)") },
                        placeholder = { Text("أدخل اسم الخط والمنطقة ورقم المركبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = modalSearch,
                        onValueChange = { modalSearch = it },
                        label = { Text("ابحث عن اسم الطالب لشموله بالخط...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (modalCandidates.isEmpty()) {
                        Text(
                            text = "جميع الطلاب المسجلين مشمولون بالخط أو لا يوجد طالب مطابق للبحث.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f, fill = false)
                        ) {
                            items(modalCandidates, key = { "enroll_bus_cand_${it.id}" }) { cand ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = cand.fullNameWithFather,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.sp
                                            )
                                            Text(
                                                text = "${ArabicTextNormalizer.cleanPureClassName(cand.gradeClass)} - شعبة ${ArabicTextNormalizer.cleanPureSectionName(cand.sectionName)}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Button(
                                            onClick = {
                                                viewModel.updateStudentBusSubscription(
                                                    studentId = cand.id,
                                                    isSubscribed = true,
                                                    busRouteName = modalRouteName.trim().ifBlank { "الخط الصباحي والمسائي" }
                                                )
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = "شمول بالخط",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
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
            },
            confirmButton = {
                TextButton(onClick = { showAddStudentToBusDialog = false }) {
                    Text("إغلاق", fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

/**
 * أنميشن فيكتور ثلاثي الأبعاد فائق الاحترافية بمنحنيات بيزيير لباص مدرسي حديث
 * (3D Isometric Bezier School Bus & Modern Fleet Animation)
 * بهيكل زمردي وذهبي وإضاءة أمامية حية وعجلات تدور بنعومة فوق مسار متموج.
 */
@Composable
private fun ThreeDIsometricBezierSchoolBusHeroAnimation(
    modifier: Modifier = Modifier.size(110.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "hero_3d_bezier_school_bus_anim")
    val busFloatY by infiniteTransition.animateFloat(
        initialValue = -3.8f,
        targetValue = 3.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bus_float_y"
    )
    val wheelRotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "hero_bus_wheel_rotation"
    )
    val headlightGlowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bus_headlight_glow"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.54f + busFloatY

        // 1) Soft Radial Emerald & Gold Volumetric Backdrop
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF34D399).copy(alpha = 0.35f),
                    Color(0xFFFDE68A).copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, h * 0.54f),
                radius = w * 0.50f
            ),
            radius = w * 0.50f,
            center = Offset(cx, h * 0.54f)
        )

        // 2) Wavy Bezier Track Underneath the 3D Coach
        val waveRoadPath = Path().apply {
            moveTo(w * 0.05f, h * 0.80f)
            cubicTo(
                w * 0.34f, h * 0.70f,
                w * 0.66f, h * 0.88f,
                w * 0.95f, h * 0.76f
            )
        }
        drawPath(
            path = waveRoadPath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFF6EE7B7), Color(0xFFFDE68A), Color(0xFF34D399))
            ),
            style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 3) Live Volumetric Headlight Beams Projecting Forward (Leftward in RTL)
        val beamConePath = Path().apply {
            moveTo(w * 0.19f, cy + h * 0.06f)
            lineTo(w * 0.01f, cy - h * 0.06f)
            lineTo(w * 0.01f, cy + h * 0.20f)
            close()
        }
        drawPath(
            path = beamConePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    Color(0xFFFEF08A).copy(alpha = headlightGlowAlpha)
                ),
                startX = 0f,
                endX = w * 0.20f
            )
        )

        // 4) 3D Isometric Bezier Modern School Bus Body (Emerald & Gold Luxury Coach)
        // Upper 3D Roof Deck
        val roofDeckPath = Path().apply {
            moveTo(w * 0.24f, cy - h * 0.20f)
            cubicTo(
                w * 0.42f, cy - h * 0.28f,
                w * 0.66f, cy - h * 0.28f,
                w * 0.82f, cy - h * 0.18f
            )
            lineTo(w * 0.86f, cy - h * 0.10f)
            lineTo(w * 0.20f, cy - h * 0.10f)
            close()
        }
        drawPath(
            path = roofDeckPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B))
            )
        )

        // Main Aerodynamic 3D Coach Hull (Cubic Bezier Curves)
        val coachHullPath = Path().apply {
            moveTo(w * 0.16f, cy + h * 0.16f)
            cubicTo(
                w * 0.14f, cy + h * 0.02f,
                w * 0.17f, cy - h * 0.12f,
                w * 0.24f, cy - h * 0.16f
            )
            cubicTo(
                w * 0.45f, cy - h * 0.20f,
                w * 0.68f, cy - h * 0.19f,
                w * 0.84f, cy - h * 0.14f
            )
            cubicTo(
                w * 0.90f, cy - h * 0.08f,
                w * 0.90f, cy + h * 0.06f,
                w * 0.86f, cy + h * 0.16f
            )
            close()
        }
        drawPath(
            path = coachHullPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF10B981),
                    Color(0xFF059669),
                    Color(0xFF064E3B)
                ),
                startY = cy - h * 0.20f,
                endY = cy + h * 0.16f
            )
        )
        drawPath(
            path = coachHullPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFF34D399), Color(0xFFF59E0B))
            ),
            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
        )

        // Front 3D Panoramic Windshield + Side Windows
        val frontWindshieldPath = Path().apply {
            moveTo(w * 0.19f, cy + h * 0.01f)
            cubicTo(
                w * 0.19f, cy - h * 0.08f,
                w * 0.23f, cy - h * 0.13f,
                w * 0.32f, cy - h * 0.13f
            )
            lineTo(w * 0.32f, cy + h * 0.01f)
            close()
        }
        drawPath(
            path = frontWindshieldPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFE0F2FE), Color(0xFF6EE7B7))
            )
        )

        for (winIdx in 0..2) {
            val wx = w * (0.36f + winIdx * 0.15f)
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFFE0F2FE), Color(0xFFA7F3D0))
                ),
                topLeft = Offset(wx, cy - h * 0.12f),
                size = Size(w * 0.12f, h * 0.13f),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )
        }

        // Royal Golden Bezier Trim Stripe along the Coach Body
        val goldStripePath = Path().apply {
            moveTo(w * 0.17f, cy + h * 0.06f)
            cubicTo(
                w * 0.40f, cy + h * 0.02f,
                w * 0.65f, cy + h * 0.09f,
                w * 0.86f, cy + h * 0.05f
            )
        }
        drawPath(
            path = goldStripePath,
            brush = Brush.horizontalGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
            ),
            style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
        )

        // Glowing Front LED Headlight
        drawCircle(
            color = Color(0xFFFFFBEB),
            radius = 3.8.dp.toPx(),
            center = Offset(w * 0.18f, cy + h * 0.08f)
        )

        // 5) Smoothly Rotating 3D Wheels with Golden Rims
        listOf(w * 0.32f, w * 0.72f).forEach { wheelX ->
            val wheelCenter = Offset(wheelX, cy + h * 0.17f)
            drawCircle(
                color = Color(0xFF0F172A),
                radius = 8.5.dp.toPx(),
                center = wheelCenter
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = 5.5.dp.toPx(),
                center = wheelCenter,
                style = Stroke(width = 1.6.dp.toPx())
            )
            rotate(degrees = wheelRotationDeg, pivot = wheelCenter) {
                drawLine(
                    color = Color(0xFFFDE68A),
                    start = Offset(wheelCenter.x - 4.5.dp.toPx(), wheelCenter.y),
                    end = Offset(wheelCenter.x + 4.5.dp.toPx(), wheelCenter.y),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFDE68A),
                    start = Offset(wheelCenter.x, wheelCenter.y - 4.5.dp.toPx()),
                    end = Offset(wheelCenter.x, wheelCenter.y + 4.5.dp.toPx()),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }
    }
}


