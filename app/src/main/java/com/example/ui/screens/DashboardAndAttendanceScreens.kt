package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val dailyBehaviors by viewModel.dailyBehaviors.collectAsState()
    val dismissalCalls by viewModel.dismissalCalls.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val dismissedUrgentAlertSig by viewModel.dismissedUrgentAlertSignature.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()

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
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Urgent School Alert Banner (for all members of this school: Teachers, Students, Parents, Admin)
        if (shouldShowUrgentAlertBanner) {
            item {
                UrgentSchoolAlertBannerCard(
                    alert = activeUrgentAlert,
                    isAdmin = isAdmin,
                    onDismissForUser = { viewModel.dismissUrgentSchoolAlertLocally() },
                    onEditRequest = { viewModel.openUrgentSchoolAlertModal() },
                    onDeleteAlert = { viewModel.deleteOrStopUrgentSchoolAlert() }
                )
            }
        }

        // Live Occasion Theme Greeting Banner (Hidden by default unless explicitly activated by Developer)
        if (occasionThemeConfig.isActive) {
            item {
                LiveOccasionBannerCard(
                    config = occasionThemeConfig,
                    modifier = Modifier.testTag("dashboard_occasion_theme_banner")
                )
            }
        }

        // Official / Weekly Holiday Banner when today is a holiday
        if (todayHolidayLabel != null) {
            item {
                OfficialHolidayBannerCard(
                    holidayLabel = todayHolidayLabel,
                    dateIso = todayIso,
                    isAdmin = isAdmin,
                    onManageHolidaysClick = { viewModel.openHolidaysManagementModal() }
                )
            }
        }

        // Welcome Hero Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF064E3B),
                                    Color(0xFF059669),
                                    Color(0xFF1E3A8A)
                                )
                            )
                        )
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NoWrapBadge(
                                text = "${schoolConfig.schoolType.arabicLabel} • ${schoolConfig.schoolStage.arabicLabel}",
                                icon = Icons.Filled.School,
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            )
                            NoWrapBadge(
                                text = currentUser?.role?.arabicTitle ?: "مستخدم",
                                icon = Icons.Filled.VerifiedUser,
                                containerColor = Color.White.copy(alpha = 0.22f),
                                contentColor = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            if (isParentOrStudent && myStudent != null) {
                                val myHeaderBadges = remember(attendance, grades, myStudent.id) {
                                    viewModel.getStudentAutoBadges(myStudent.id)
                                }
                                StudentAvatarWithOverlayBadge(
                                    base64Data = myStudent.photoBase64.ifBlank { schoolConfig.logoBase64 },
                                    contentDescription = myStudent.fullNameWithFather,
                                    badge = myHeaderBadges.firstOrNull(),
                                    avatarSize = 54.dp,
                                    boxWidth = 62.dp,
                                    fallbackIcon = Icons.Filled.Person,
                                    tint = Color.White,
                                    onDarkBackground = true
                                )
                            } else {
                                Base64AvatarImage(
                                    base64Data = schoolConfig.logoBase64,
                                    fallbackIcon = Icons.Filled.School,
                                    contentDescription = schoolConfig.schoolName,
                                    size = 56.dp,
                                    tint = Color.White
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "مرحباً بك، ${currentUser?.fullName ?: ""}",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White
                                )
                                Text(
                                    text = when {
                                        isParentOrStudent && myStudent != null ->
                                            "الطالب: ${myStudent.studentName} • الصف: ${myStudent.gradeClass} (شعبة ${myStudent.sectionName})"
                                        isTeacher ->
                                            "المواد: ${currentUser?.specialty?.ifBlank { "كادر تدريسي" } ?: ""} • الصفوف: ${currentUser?.formattedAssignedClassesForDisplay ?: ""}"
                                        else -> {
                                            val cleanAddr = ArabicTextNormalizer.cleanDefaultAddress(schoolConfig.address)
                                            if (cleanAddr.isNotBlank()) {
                                                "${schoolConfig.schoolName} ($cleanAddr) — ${schoolConfig.academicYear}"
                                            } else {
                                                "${schoolConfig.schoolName} — ${schoolConfig.academicYear}"
                                            }
                                        }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }

                        if (isAdmin) {
                            Button(
                                onClick = { viewModel.navigateToSection(AppSection.ADMIN_ACCOUNTS) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF064E3B)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("dashboard_admin_accounts_btn")
                            ) {
                                Icon(Icons.Filled.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "إدارة الحسابات والمستخدمين (معلمين • أولياء أمور • طلاب)",
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Strict Privacy Isolation vs Admin/Teacher Stat Cards
        item {
            if (isParentOrStudent && myStudent != null) {
                // PRIVATE STUDENT-ONLY DASHBOARD STATS (Strictly NO general school stats!)
                val myTodayStatus = if (todayHolidayLabel != null) {
                    AttendanceStatus.HOLIDAY
                } else {
                    viewModel.getStudentStatusForDate(myStudent, todayIso)
                }
                val myTotalAbsences = viewModel.getTotalAbsenceDays(myStudent.id)
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

                // Live Smart Gate Status Card for Parent & Student (Check-In / Check-Out timestamp)
                val todayGateLog = myStudent.getGateLogForDate(todayIso)
                if (todayGateLog != null && (todayGateLog.checkInTime.isNotBlank() || todayGateLog.checkOutTime.isNotBlank())) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                            .testTag("dashboard_student_gate_status_card"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFECFDF5),
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

                // Smart 1-Tap Dismissal Call for Parents ("أنا أمام المدرسة لاستلام ابني")
                if (currentUser?.role == UserRole.PARENT && featureSettings.dismissalCallEnabled) {
                    val activeCall = dismissalCalls.firstOrNull { it.studentId == myStudent.id && !it.isAcknowledged }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeCall != null) SuccessGreen.copy(alpha = 0.14f) else Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(
                            1.2.dp,
                            if (activeCall != null) SuccessGreen else DeepEmerald.copy(alpha = 0.45f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    text = if (activeCall != null) {
                                        "تم إبلاغ الإدارة بوصولك (${activeCall.requestedAt})"
                                    } else {
                                        "الاستدعاء السريع عند الانصراف"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (activeCall != null) {
                                        "الإدارة تجهز خروج الطالب (${myStudent.studentName}) الآن للبوابة"
                                    } else {
                                        "اضغط لإبلاغ الإدارة والمعلمين فور وصولك أمام المدرسة لاستلام ابنك"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepEmerald
                                )
                            }
                            Button(
                                onClick = { viewModel.triggerParentDismissalCall(myStudent) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("dashboard_parent_dismissal_call_btn")
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (activeCall != null) "إعادة تنبيه الإدارة بوصولي" else "أنا أمام المدرسة لاستلام ابني",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
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
                            subtitle = if (todayHolidayLabel != null) todayHolidayLabel else "إجمالي أيام غيابه: $myTotalAbsences يوم",
                            icon = Icons.Filled.FactCheck,
                            accentColor = when {
                                todayHolidayLabel != null || myTodayStatus == AttendanceStatus.HOLIDAY -> GoldAccent
                                myTodayStatus == AttendanceStatus.ABSENT -> DangerRed
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

                Text(
                    text = if (isTeacher) "إحصائيات صفوفك وطلابك المسندين فقط" else "لوحة الإحصائيات العامة للمدرسة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Prominent Live Operations Radar Banner Card (المتابعة الحية المباشرة للصفوف والبوابة)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.navigateToSection(AppSection.LIVE_OPERATIONS_RADAR) }
                            .testTag("dashboard_open_live_operations_radar_card"),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF059669)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(Color(0xFF047857), Color(0xFF059669))
                                    )
                                )
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color.White.copy(alpha = 0.18f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.45f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Radar,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier
                                                .padding(8.dp)
                                                .size(22.dp)
                                        )
                                    }
                                    Column {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF4ADE80))
                                            )
                                            Text(
                                                text = "المتابعة الحية المباشرة للصفوف والبوابة (Live)",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                        }
                                        Text(
                                            text = "مراقبة لحظية لحضور وغياب وإجازات كل صف • من دخل صباحاً • من غادر • ومن بقي داخل المدرسة الآن",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.92f)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White,
                                    contentColor = Color(0xFF047857)
                                ) {
                                    Text(
                                        text = "فتح الرادار",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "حاضرون: $presentCount" to Color.White.copy(alpha = 0.20f),
                                    "غائبون: $absentCount" to Color.White.copy(alpha = 0.20f),
                                    "دخلوا: $checkedInToday" to Color.White.copy(alpha = 0.20f),
                                    "غادروا: $checkedOutToday" to Color.White.copy(alpha = 0.20f),
                                    "داخل المدرسة: $insideSchoolNow" to Color.White.copy(alpha = 0.30f)
                                ).forEach { (txt, bg) ->
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(10.dp),
                                        color = bg,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 5.dp, horizontal = 2.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = txt,
                                                style = MaterialTheme.typography.labelSmall,
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

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = if (isTeacher) "طلابي المسندون" else "إجمالي الطلبة",
                            value = "${scopedStudents.size} طالب",
                            subtitle = if (isTeacher) "المسجلون في صفوفك وشعبك" else "$staffPluralLabel: $teachersCount",
                            icon = Icons.Filled.Groups,
                            accentColor = PrimaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.ATTENDANCE) }
                        )
                        SmartStatCard(
                            title = "سجل الحضور اليومي",
                            value = if (todayHolidayLabel != null) "عطلة رسمية" else "$presentCount حاضر",
                            subtitle = if (todayHolidayLabel != null) {
                                todayHolidayLabel
                            } else {
                                "الغياب: $absentCount • مجاز: $excusedCount${if (lateCount > 0) " • متأخر: $lateCount" else ""}"
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

                    // School Bus & Transport Quick Notifications Card (for Private Schools)
                    if (schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.busTrackingEnabled && (isAdmin || isTeacher)) {
                        val busSubscribersCount = students.count { it.isBusSubscribed && it.isActive }
                        SchoolBusQuickAlertsCard(
                            busSubscribersCount = busSubscribersCount,
                            onSendBusAlert = { eventType -> viewModel.sendBusTrackingAlert(eventType) }
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
                                                Color(0xFF047857),
                                                Color(0xFF059669)
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
                                                tint = Color(0xFF047857),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "فتح الكاشير",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF047857),
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

        // All Sections Quick Access Grid
        item {
            Text(
                text = "الأقسام التعليمية والإدارية",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            val sectionsList = remember(schoolConfig.schoolType, featureSettings, currentUser) {
                buildList {
                    add(Triple(AppSection.ATTENDANCE, Icons.Filled.HowToReg, if (isParentOrStudent) "حالة الحضور وعداد الغياب الخاص" else "رصد الحضور والغياب وتبليغ واتساب"))
                    add(Triple(AppSection.SCHEDULE_HOMEWORK, Icons.Filled.CalendarMonth, "جدول الحصص والتحضير اليومي"))
                    add(Triple(AppSection.EXAMS_GRADES, Icons.Filled.Assessment, if (isParentOrStudent) "جدول كشف درجات الطالب التفصيلي" else "الامتحانات ورصد الدرجات حسب المادة"))
                    if (featureSettings.honorRollEnabled) {
                        add(Triple(AppSection.HONOR_ROLL, Icons.Filled.WorkspacePremium, "لوحة الشرف للمتفوقين (عام)"))
                    }
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
                    add(Triple(AppSection.STUDENT_COMPREHENSIVE_ARCHIVE, Icons.Filled.FolderSpecial, "السجل الشامل للطالب (الأرشيف الشهري والسنوي)"))
                    if (isAdmin) {
                        add(Triple(AppSection.ADMIN_ACCOUNTS, Icons.Filled.ManageAccounts, "إدارة وتفعيل حسابات المستخدمين"))
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                sectionsList.chunked(2).forEach { pair ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        pair.forEach { (sec, icon, desc) ->
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { viewModel.navigateToSection(sec) }
                                    .testTag("section_card_${sec.name}"),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = sec.arabicTitle,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                    Text(
                                        text = sec.arabicTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = desc,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Icon(
                    imageVector = Icons.Filled.TrendingUp,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = accentColor,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
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
        if (selectedDateHolidayLabel != null) {
            item {
                OfficialHolidayBannerCard(
                    holidayLabel = selectedDateHolidayLabel,
                    dateIso = selectedDate,
                    isAdmin = isAdmin,
                    onManageHolidaysClick = { viewModel.openHolidaysManagementModal() }
                )
            }
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
                                Button(
                                    onClick = { viewModel.triggerParentDismissalCall(myStu) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("attendance_parent_dismissal_call_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepEmerald,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("الاستدعاء السريع: أنا أمام المدرسة لاستلام ابني", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }

                // Daily Behavior & Classroom Follow-Up Log for Student/Parent (Compact top 3 + Full Log Modal)
                if (featureSettings.dailyBehaviorEnabled && myBehaviors.isNotEmpty()) {
                    item {
                        CompactDailyBehaviorCard(
                            title = "دفتر المتابعة اليومي والسلوك الصفي (${myBehaviors.size}):",
                            studentName = myStu.fullNameWithFather,
                            behaviors = myBehaviors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                if (featureSettings.leaveRequestsEnabled && myLeaveRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل طلبات الإجازة الإلكترونية (${myLeaveRequests.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
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

                val visibleMyRecords = myRecords.take(studentAttendanceRecordsVisibleCount)
                items(visibleMyRecords, key = { it.id }) { rec ->
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
                                Text("التاريخ: ${rec.date}", style = MaterialTheme.typography.titleSmall)
                                if (rec.note.isNotBlank()) {
                                    Text(rec.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            val recordHolidayLabel = viewModel.getHolidayLabelForDate(rec.date)
                            val effectiveStatus = if (recordHolidayLabel != null) AttendanceStatus.HOLIDAY else rec.status
                            NoWrapBadge(
                                text = if (effectiveStatus == AttendanceStatus.HOLIDAY) {
                                    "يوم عطلة رسمية (موثق في السجل)"
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
        } else {
            // Admin & Scoped Teacher Attendance Filter Card + QR Scanner + Monthly PDF Export
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
                                    imageVector = Icons.Filled.HowToReg,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "سجل الحضور والغياب اليومي",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                            NoWrapBadge(
                                text = "التاريخ: $selectedDate",
                                icon = Icons.Filled.Today,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Action Buttons: Smart Gate QR Check-In / Check-Out + Monthly Attendance PDF Export + Lost & Found
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
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepEmerald,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Login,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "باركود الدخول (حضور)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
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
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "باركود المغادرة (انصراف)",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DeepForestGreen,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            if (isAdmin) {
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
                                        .testTag("export_monthly_attendance_pdf_btn"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PictureAsPdf,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تصدير كشف الغياب الشهري", fontWeight = FontWeight.Bold, maxLines = 1)
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

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModernCalendarDatePickerField(
                                selectedDateIso = selectedDate,
                                label = "تاريخ الرصد",
                                onDateSelected = { selectedIso, _ -> viewModel.setSelectedAttendanceDate(selectedIso) },
                                modifier = Modifier.weight(0.95f)
                            )
                            OutlinedTextField(
                                value = searchName,
                                onValueChange = { searchName = it },
                                placeholder = {
                                    Text(
                                        text = "بحث باسم الطالب...",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                singleLine = true,
                                modifier = Modifier.weight(1.15f),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }

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

                        // Scoped Student Count Indicator + Admin Holidays Management Shortcut
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
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
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
                                if (isAdmin) {
                                    TextButton(
                                        onClick = { viewModel.openHolidaysManagementModal() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("attendance_open_holidays_modal_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.EventAvailable,
                                            contentDescription = null,
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "إدارة العطل الرسمية",
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

            // Live Parent Dismissal Calls Card ("الاستدعاء السريع عند الانصراف")
            val activeDismissalCalls = dismissalCalls.filter { !it.isAcknowledged }
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

            // School Bus Quick Alert Buttons in Attendance section for Private Schools
            if (schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.busTrackingEnabled && canEdit) {
                item {
                    val busSubscribersCount = students.count { it.isBusSubscribed && it.isActive }
                    SchoolBusQuickAlertsCard(
                        busSubscribersCount = busSubscribersCount,
                        onSendBusAlert = { eventType -> viewModel.sendBusTrackingAlert(eventType) }
                    )
                }
            }

            // Electronic Leave Requests Section for Admin & Authorized Class Teacher (with Approve / Reject buttons)
            val scopedLeaveRequestsForManager = if (isAdmin) {
                leaveRequests
            } else {
                val allowedIds = scopedStudents.map { it.id.trim() }.toSet()
                leaveRequests.filter { it.studentId.trim() in allowedIds }
            }
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
                                val pendingCount = scopedLeaveRequestsForManager.count { it.status == LeaveRequestStatus.PENDING }
                                if (pendingCount > 0) {
                                    NoWrapBadge(
                                        text = "$pendingCount قيد المراجعة",
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
                            val holidayNoteText = selectedDateHolidayLabel
                                ?: currentEntry?.note?.takeIf { it.isNotBlank() }
                                ?: "عطلة رسمية"
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
                                        imageVector = Icons.Filled.EventAvailable,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "يوم عطلة رسمية (موثق في السجل) • $holidayNoteText",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        maxLines = 1,
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
                                    }
                                    val statusIcon = when (st) {
                                        AttendanceStatus.PRESENT -> Icons.Filled.CheckCircle
                                        AttendanceStatus.ABSENT -> Icons.Filled.Cancel
                                        AttendanceStatus.LATE -> Icons.Filled.Schedule
                                        AttendanceStatus.EXCUSED -> Icons.Filled.AssignmentLate
                                        AttendanceStatus.HOLIDAY -> Icons.Filled.EventAvailable
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

                        // 3. خط فاصل ناعم وأنيق + 4. أزرار تقييم المتابعة والسلوك الصفي الفوري في صفين متوازيين (2×2) بأيقونات Vector نظيفة
                        if (canEdit && featureSettings.dailyBehaviorEnabled) {
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

@Composable
private fun SchoolBusQuickAlertsCard(
    busSubscribersCount: Int,
    onSendBusAlert: (BusEventType) -> Unit
) {
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
                        imageVector = Icons.Filled.DirectionsBus,
                        contentDescription = null,
                        tint = DeepEmerald
                    )
                    Column {
                        Text(
                            text = "إشعارات الخطوط والنقل المدرسي الفورية",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "ترسل إشعاراً فورياً حصرياً لأولياء أمور الطلبة المشتركين بالخط ($busSubscribersCount طالب)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BusEventType.entries.forEach { busEvent ->
                    Button(
                        onClick = { onSendBusAlert(busEvent) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("bus_alert_btn_${busEvent.name}"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = busEvent.arabicLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
    val stuGrades = remember(grades, selectedStudent?.id) {
        val targetId = selectedStudent?.id ?: ""
        if (targetId.isEmpty()) emptyList() else grades.filter { it.studentId == targetId }
    }
    val stuAverage = remember(stuGrades) {
        if (stuGrades.isEmpty()) 0 else stuGrades.map { it.finalAverage }.average().toInt()
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
        // Top Header Card & Dual Mode Switcher for Admin/Teacher
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.25f)),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FolderSpecial,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "السجل الأكاديمي والإحصائي الشامل",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                                Text(
                                    text = "تقارير الأداء، المواظبة، والنتائج الشهرية والسنوية للصفوف والطلبة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Dual Mode Switcher for Principal & Teachers:
                    // [السجل الإحصائي العام للصفوف والشعب] vs [السجل التفصيلي الفردي للطالب]
                    if (!isParentOrStudent) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFF0FDF4))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                0 to "السجل الإحصائي العام للصفوف والشعب",
                                1 to "السجل التفصيلي الفردي للطالب"
                            ).forEach { (modeIdx, modeTitle) ->
                                val isSel = archiveViewMode == modeIdx
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { archiveViewMode = modeIdx }
                                        .testTag("archive_mode_tab_$modeIdx"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSel) DeepEmerald else Color.Transparent
                                ) {
                                    Box(
                                        modifier = Modifier.padding(vertical = 9.dp, horizontal = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = modeTitle,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSel) Color.White else DeepForestGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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

                    if (!isParentOrStudent && archiveViewMode == 1) {
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
                                        Text(
                                            text = "${ex.examDate} • من ${ex.maxScore}",
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
                                                text = gr.subjectName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            NoWrapBadge(
                                                text = "المعدل: ${gr.finalAverage} / 100 (${gr.evaluationText})",
                                                icon = Icons.Filled.Grade,
                                                containerColor = if (gr.finalAverage >= 50) SuccessGreen.copy(alpha = 0.14f) else DangerRed.copy(alpha = 0.14f),
                                                contentColor = if (gr.finalAverage >= 50) SuccessGreen else DangerRed
                                            )
                                        }
                                        Text(
                                            text = "ش1: ${gr.month1} | ش2: ${gr.month2} | نصف السنة: ${gr.midYear} | ش3: ${gr.month3} | ش4: ${gr.month4} | النهائي: ${gr.finalExam}",
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
    INSIDE_NOW("متبقون داخل المدرسة الآن"),
    CHECKED_IN("دخلوا صباحاً (Check-In)"),
    CHECKED_OUT("غادروا المدرسة (Check-Out)"),
    PRESENT("الحاضرون اليوم"),
    ABSENT("الغائبون اليوم"),
    EXCUSED("المجازون اليوم"),
    LATE("المتأخرون اليوم")
}

private data class LiveClassroomCardSummary(
    val gradeClass: String,
    val sectionName: String,
    val students: List<StudentRecord>,
    val presentCount: Int,
    val absentCount: Int,
    val excusedCount: Int,
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
 * Deliverable 1: غرفة المتابعة الحية المباشرة للصفوف والبوابة (Live Operations Radar)
 * - 1) شريط المؤشرات الحية الشامل للمدرسة (Top Live School Radar + Smart Gate Live Radar)
 * - 2) شبكة بطاقات الصفوف والشعب الحية (Live Classrooms Cards)
 * - 3) كشف الأسماء التفصيلي الفوري عند الضغط (Drill-Down Student List Modal)
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
    val lateTodayCount = remember(activeScopedStudents, attendance, selectedDateIso, holidaysConfig) {
        activeScopedStudents.count { isStudentPresentOnDate(it, selectedDateIso) == AttendanceStatus.LATE }
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
        // 1) شريط المؤشرات الحية الشامل للمدرسة (Top Live School Radar + Smart Gate Live Radar)
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
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.4f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Radar,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier
                                        .padding(9.dp)
                                        .size(22.dp)
                                )
                            }
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF10B981))
                                    )
                                    Text(
                                        text = "غرفة المتابعة الحية المباشرة للصفوف والبوابة",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen
                                    )
                                }
                                Text(
                                    text = if (isTeacher) {
                                        "مراقبة حية فورية لصفوفك المسندة • اضغط على أي مؤشر أو صف لعرض كشف الأسماء والتوقيتات"
                                    } else {
                                        "رادار مركزي شامل للمدرسة ($selectedDateIso) • اضغط على أي مؤشر أو صف لعرض كشف الأسماء والتوقيتات"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Live Attendance Radar Row (إجمالي الطلبة | الحاضرون اليوم | الغائبون | المجازون)
                    Text(
                        text = "أولاً: المؤشرات الحية للحضور والغياب والإجازات اليوم (اضغط لعرض الأسماء):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("إجمالي الطلبة", "$totalStudentsCount", DeepEmerald to LiveRadarDrillFilter.ALL),
                            Triple("الحاضرون اليوم", "$presentTodayCount", SuccessGreen to LiveRadarDrillFilter.PRESENT),
                            Triple("الغائبون", "$absentTodayCount", DangerRed to LiveRadarDrillFilter.ABSENT),
                            Triple("المجازون", "$excusedTodayCount", InfoBlue to LiveRadarDrillFilter.EXCUSED)
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
                                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = valTxt,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = col
                                    )
                                    Text(
                                        text = lbl,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DeepForestGreen,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Smart Gate Live Radar Row (دخلوا صباحاً | غادروا المدرسة | متبقون داخل المدرسة الآن)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF059669),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
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
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "رادار البوابة الذكية الحي (Smart Gate Live Radar)",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = "اضغط لكشف الأسماء",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
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
                                    Triple("متبقون بالمدرسة الآن", "$insideNowTodayCount طالب", LiveRadarDrillFilter.INSIDE_NOW)
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
                                            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = valTxt,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isInsideCard) Color(0xFF047857) else Color.White,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = lbl,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isInsideCard) DeepForestGreen else Color.White.copy(alpha = 0.95f),
                                                maxLines = 1
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

        // 2) شبكة بطاقات الصفوف والشعب الحية (Live Classrooms Cards)
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

                    // Attendance Breakdown Row (حاضر • غائب • مجاز • متأخر)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("حاضر", "${card.presentCount}", SuccessGreen to LiveRadarDrillFilter.PRESENT),
                            Triple("غائب", "${card.absentCount}", DangerRed to LiveRadarDrillFilter.ABSENT),
                            Triple("مجاز", "${card.excusedCount}", InfoBlue to LiveRadarDrillFilter.EXCUSED),
                            Triple("متأخر", "${card.lateCount}", WarningAmber to LiveRadarDrillFilter.LATE)
                        ).forEach { (lbl, valTxt, pair) ->
                            val (col, filterType) = pair
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
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestGreen,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    // Smart Gate Live Breakdown for this Classroom (دخلوا بداية الدوام • غادروا • متبقون داخل المدرسة الآن)
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
                                text = "حركة البوابة الذكية لهذا الصف اليوم:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    Triple("دخلوا بداية الدوام", "${card.checkedInCount}", LiveRadarDrillFilter.CHECKED_IN),
                                    Triple("غادروا المدرسة", "${card.checkedOutCount}", LiveRadarDrillFilter.CHECKED_OUT),
                                    Triple("متبقون بالمدرسة الآن", "${card.insideNowCount}", LiveRadarDrillFilter.INSIDE_NOW)
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
                                                maxLines = 1
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
                                    imageVector = Icons.Filled.Radar,
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
                                val stuLeave = leaveRequests.firstOrNull {
                                    it.studentId == stu.id && it.leaveDate == selectedDateIso
                                } ?: leaveRequests.firstOrNull { it.studentId == stu.id }
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
                                                icon = Icons.Filled.FactCheck,
                                                containerColor = when (attStatus) {
                                                    AttendanceStatus.PRESENT -> SuccessGreen
                                                    AttendanceStatus.ABSENT -> DangerRed
                                                    AttendanceStatus.EXCUSED -> InfoBlue
                                                    else -> WarningAmber
                                                },
                                                contentColor = Color.White
                                            )
                                        }

                                        // Smart Gate Timestamps Box
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFECFDF5),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = "وقت الدخول: ${gateLog?.checkInTime?.ifBlank { "لم يسجل" } ?: "لم يسجل"}  |  وقت المغادرة: ${gateLog?.checkOutTime?.ifBlank { "لم يغادر بعد" } ?: "لم يغادر بعد"}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen
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
                                                                isInside -> "متواجد بالمدرسة الآن"
                                                                isDeparted -> "غادر المدرسة"
                                                                else -> "خارج البوابة"
                                                            },
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }

                                                if (attStatus == AttendanceStatus.EXCUSED || stuLeave != null) {
                                                    Text(
                                                        text = "سبب الإجازة: ${stuLeave?.reason?.ifBlank { "إجازة رسمية معتمدة" } ?: "إجازة رسمية معتمدة"}",
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
                                                    viewModel.requireOnlineAndExecute {
                                                        val mode = if (isInside) GateScanMode.CHECK_OUT else GateScanMode.CHECK_IN
                                                        viewModel.recordAttendanceByQrCodeDetailed(
                                                            qrOrUsername = stu.id,
                                                            date = selectedDateIso,
                                                            scanMode = mode
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isInside) InfoBlue else DeepEmerald,
                                                    contentColor = Color.White
                                                )
                                            ) {
                                                Icon(
                                                    imageVector = if (isInside) Icons.AutoMirrored.Filled.Logout else Icons.AutoMirrored.Filled.Login,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = if (isInside) "تسجيل مغادرة" else "تسجيل دخول",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
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
    }
}





