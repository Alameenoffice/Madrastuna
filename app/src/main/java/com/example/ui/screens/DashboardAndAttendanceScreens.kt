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

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val isTeacher = currentUser?.role == UserRole.TEACHER
    val isAdmin = currentUser?.role == UserRole.ADMIN

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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // Live Occasion Theme Greeting Banner (Managed by Developer ABU-RASLAN)
        if (occasionThemeConfig.themeType != OccasionThemeType.OFFICIAL ||
            occasionThemeConfig.customBannerText.isNotBlank() ||
            occasionThemeConfig.customIconBase64.isNotBlank()
        ) {
            item {
                LiveOccasionBannerCard(
                    config = occasionThemeConfig,
                    modifier = Modifier.testTag("dashboard_occasion_theme_banner")
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
                            Base64AvatarImage(
                                base64Data = schoolConfig.logoBase64,
                                fallbackIcon = Icons.Filled.School,
                                contentDescription = schoolConfig.schoolName,
                                size = 60.dp,
                                tint = Color.White
                            )
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
                                            "المواد: ${currentUser?.specialty ?: ""} • الصفوف: ${currentUser?.assignedClasses ?: ""}"
                                        else ->
                                            "${schoolConfig.schoolName} (${schoolConfig.address}) — ${schoolConfig.academicYear}"
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
                val myTodayStatus = attendance.firstOrNull { it.studentId == myStudent.id }?.status ?: AttendanceStatus.PRESENT
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
                val myBehaviors = remember(dailyBehaviors, myStudent.id) {
                    dailyBehaviors.filter { it.studentId == myStudent.id }.take(3)
                }

                // Automatic Motivational Badges (وسام الالتزام الذهبي + وسام نجم المادة)
                if (myBadges.isNotEmpty()) {
                    StudentAutoBadgesRow(
                        badges = myBadges,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Smart 1-Tap Dismissal Call for Parents ("أنا أمام المدرسة لاستلام ابني")
                if (currentUser?.role == UserRole.PARENT && featureSettings.dismissalCallEnabled) {
                    val activeCall = dismissalCalls.firstOrNull { it.studentId == myStudent.id && !it.isAcknowledged }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = if (activeCall != null) SuccessGreen.copy(alpha = 0.14f) else DeepForestGreen,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (activeCall != null) SuccessGreen else EmeraldAccent.copy(alpha = 0.45f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (activeCall != null) {
                                        "تم إبلاغ الإدارة بوصولك (${activeCall.requestedAt})"
                                    } else {
                                        "الاستدعاء السريع عند الانصراف"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (activeCall != null) DeepForestGreen else Color(0xFFA7F3D0),
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (activeCall != null) {
                                        "الإدارة تجهز خروج الطالب (${myStudent.studentName}) الآن للبوابة"
                                    } else {
                                        "اضغط لإبلاغ الإدارة والمعلمين فور وصولك أمام المدرسة لاستلام ابنك"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (activeCall != null) DeepEmerald else Color.White.copy(alpha = 0.85f)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = { viewModel.triggerParentDismissalCall(myStudent) },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                ),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.testTag("dashboard_parent_dismissal_call_btn")
                            ) {
                                Icon(Icons.Filled.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (activeCall != null) "إعادة تنبيه" else "أنا أمام المدرسة",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Recent Daily Follow-Up & Behavior Log for Student/Parent
                if (featureSettings.dailyBehaviorEnabled && myBehaviors.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                                Text(
                                    text = "دفتر المتابعة اليومي والسلوك (${myStudent.studentName}):",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                            }
                            myBehaviors.forEach { beh ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (beh.behaviorType.isPositive) {
                                        SuccessGreen.copy(alpha = 0.12f)
                                    } else {
                                        DeepEmerald.copy(alpha = 0.08f)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = if (beh.behaviorType.isPositive) Icons.Filled.AutoAwesome else Icons.Filled.Info,
                                                contentDescription = null,
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = "${beh.behaviorType.arabicTitle} • ${beh.subjectName}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepForestGreen
                                            )
                                        }
                                        Text(
                                            text = beh.date,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMutedGreen
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = "حالة الحضور اليوم",
                            value = myTodayStatus.arabicLabel,
                            subtitle = "إجمالي أيام غيابه: $myTotalAbsences يوم",
                            icon = Icons.Filled.FactCheck,
                            accentColor = if (myTodayStatus == AttendanceStatus.ABSENT) DangerRed else PrimaryLight,
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
                }
            } else {
                // ADMIN OR SCOPED TEACHER DASHBOARD STATS
                val scopedStudentIds = scopedStudents.map { it.id }.toSet()
                val scopedAttendance = attendance.filter { it.studentId in scopedStudentIds }
                val presentCount = scopedAttendance.count { it.status == AttendanceStatus.PRESENT }
                val absentCount = scopedAttendance.count { it.status == AttendanceStatus.ABSENT }
                val honorCount = students.count { it.isOnHonorRoll }
                val teachersCount = accounts.count { it.role == UserRole.TEACHER }

                Text(
                    text = if (isTeacher) "إحصائيات صفوفك وطلابك المسندين" else "لوحة الإحصائيات العامة للمدرسة",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SmartStatCard(
                            title = if (isTeacher) "طلابي المسندون" else "إجمالي الطلبة",
                            value = "${scopedStudents.size} طالب",
                            subtitle = if (isTeacher) "ضمن شعبك المسندة" else "الكادر التدريسي: $teachersCount",
                            icon = Icons.Filled.Groups,
                            accentColor = PrimaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.ATTENDANCE) }
                        )
                        SmartStatCard(
                            title = "سجل الحضور اليومي",
                            value = "$presentCount حاضر",
                            subtitle = "حالات الغياب: $absentCount",
                            icon = Icons.Filled.FactCheck,
                            accentColor = InfoBlue,
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
                            value = "${homeworks.size} واجب",
                            subtitle = "محدث للصفوف المسجلة",
                            icon = Icons.Filled.MenuBook,
                            accentColor = SecondaryLight,
                            modifier = Modifier.weight(1f),
                            onClick = { viewModel.navigateToSection(AppSection.SCHEDULE_HOMEWORK) }
                        )
                        if (featureSettings.honorRollEnabled) {
                            SmartStatCard(
                                title = "لوحة الشرف الذهبية",
                                value = "$honorCount متفوق",
                                subtitle = "أوائل الصفوف",
                                icon = Icons.Filled.EmojiEvents,
                                accentColor = GoldAccent,
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
                    if (featureSettings.eventsGalleryEnabled) {
                        add(Triple(AppSection.EVENTS_NEWS, Icons.Filled.Newspaper, "معرض الفعاليات والأخبار (عام)"))
                    }
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

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val isAdmin = currentUser?.role == UserRole.ADMIN
    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val allowedClasses = remember(schoolConfig, currentUser, selectedParentChildId) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, selectedClass, selectedParentChildId) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }

    var selectedDate by remember { mutableStateOf("2026-10-29") }
    var searchName by remember { mutableStateOf("") }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var showLeaveRequestDialog by remember { mutableStateOf(false) }
    var showLostAndFoundDialog by remember { mutableStateOf(false) }
    var selectedStudentForIdCard by remember { mutableStateOf<StudentRecord?>(null) }

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

    val scopedStudents = remember(students, currentUser, selectedClass, selectedSection, searchName, selectedParentChildId) {
        val base = viewModel.getScopedStudentsForUser(currentUser)
        if (isParentOrStudent) {
            base
        } else {
            base.filter { stu ->
                stu.gradeClass == selectedClass &&
                    stu.sectionName == selectedSection &&
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
        if (isParentOrStudent) {
            // Strict Privacy View for Parent & Student: ONLY their own student's attendance & leave requests!
            val myStu = scopedStudents.firstOrNull()
            if (myStu != null) {
                val myTotalAbsences = viewModel.getTotalAbsenceDays(myStu.id)
                val myRecords = attendance.filter { it.studentId == myStu.id }
                val todayStatus = myRecords.firstOrNull { it.date == selectedDate }?.status ?: AttendanceStatus.PRESENT
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "سجل الحضور الخاص: ${myStu.fullNameWithFather}",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                NoWrapBadge(
                                    text = "إجمالي أيام الغياب: $myTotalAbsences يوم",
                                    icon = Icons.Filled.EventBusy,
                                    containerColor = if (myTotalAbsences > 0) DangerRed else SuccessGreen,
                                    contentColor = Color.White
                                )
                            }
                            Text(
                                text = "الصف: ${myStu.gradeClass} (شعبة ${myStu.sectionName}) • حالة اليوم ($selectedDate): ${todayStatus.arabicLabel}",
                                style = MaterialTheme.typography.bodyMedium
                            )

                            if (myBadges.isNotEmpty()) {
                                StudentAutoBadgesRow(badges = myBadges)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (featureSettings.leaveRequestsEnabled) {
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

                // Daily Behavior & Classroom Follow-Up Log for Student/Parent
                if (featureSettings.dailyBehaviorEnabled && myBehaviors.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = DeepEmerald)
                                    Text(
                                        text = "دفتر المتابعة اليومي والسلوك الصفي (${myBehaviors.size}):",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                myBehaviors.forEach { beh ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (beh.behaviorType.isPositive) {
                                            SuccessGreen.copy(alpha = 0.12f)
                                        } else {
                                            DeepEmerald.copy(alpha = 0.08f)
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = beh.behaviorType.arabicTitle,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen
                                                )
                                                Text(
                                                    text = "المادة: ${beh.subjectName} • بواسطة: ${beh.teacherName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMutedGreen
                                                )
                                                if (beh.note.isNotBlank()) {
                                                    Text(
                                                        text = beh.note,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = DeepEmerald
                                                    )
                                                }
                                            }
                                            Text(
                                                text = beh.date,
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

                if (featureSettings.leaveRequestsEnabled && myLeaveRequests.isNotEmpty()) {
                    item {
                        Text(
                            text = "سجل طلبات الإجازة الإلكترونية (${myLeaveRequests.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    items(myLeaveRequests, key = { it.id }) { req ->
                        LeaveRequestCardItem(
                            req = req,
                            isAdmin = false,
                            onApprove = {},
                            onReject = {}
                        )
                    }
                }

                items(myRecords, key = { it.id }) { rec ->
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
                            NoWrapBadge(
                                text = rec.status.arabicLabel,
                                icon = Icons.Filled.FactCheck,
                                containerColor = when (rec.status) {
                                    AttendanceStatus.ABSENT -> DangerRed.copy(alpha = 0.15f)
                                    AttendanceStatus.EXCUSED -> InfoBlue.copy(alpha = 0.15f)
                                    else -> SuccessGreen.copy(alpha = 0.15f)
                                },
                                contentColor = when (rec.status) {
                                    AttendanceStatus.ABSENT -> DangerRed
                                    AttendanceStatus.EXCUSED -> InfoBlue
                                    else -> SuccessGreen
                                }
                            )
                        }
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

                        // Action Buttons: QR Code Attendance Scan + Monthly Attendance PDF Export + Lost & Found
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (featureSettings.qrAttendanceEnabled) {
                                Button(
                                    onClick = { showQrScannerDialog = true },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("open_qr_attendance_scanner_btn"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = DeepEmerald,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.QrCodeScanner,
                                        contentDescription = null,
                                        tint = GoldFoil,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مسح الهوية بالـ QR", fontWeight = FontWeight.Bold, maxLines = 1)
                                }
                            }

                            if (isAdmin) {
                                OutlinedButton(
                                    onClick = {
                                        val monthPrefix = selectedDate.take(7)
                                        val classStudents = students.filter {
                                            it.gradeClass == selectedClass && it.sectionName == selectedSection
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

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = selectedDate,
                                onValueChange = { selectedDate = it },
                                label = { Text("تاريخ الرصد") },
                                leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            )
                            OutlinedTextField(
                                value = searchName,
                                onValueChange = { searchName = it },
                                label = { Text("بحث باسم الطالب...") },
                                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                singleLine = true,
                                modifier = Modifier.weight(1.2f),
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
                                        selectedClass = cls
                                        val nextSecs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                                        if (selectedSection !in nextSecs) {
                                            selectedSection = nextSecs.firstOrNull() ?: "أ"
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
                                    onClick = { selectedSection = sec },
                                    label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                )
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

            // Electronic Leave Requests Section for Admin (with Approve / Reject buttons)
            if (isAdmin && featureSettings.leaveRequestsEnabled && leaveRequests.isNotEmpty()) {
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
                                        text = "طلبات الإجازات الإلكترونية من أولياء الأمور",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                val pendingCount = leaveRequests.count { it.status == LeaveRequestStatus.PENDING }
                                if (pendingCount > 0) {
                                    NoWrapBadge(
                                        text = "$pendingCount قيد المراجعة",
                                        icon = Icons.Filled.PendingActions,
                                        containerColor = WarningAmber,
                                        contentColor = Color.White
                                    )
                                }
                            }

                            leaveRequests.take(6).forEach { req ->
                                LeaveRequestCardItem(
                                    req = req,
                                    isAdmin = true,
                                    onApprove = {
                                        viewModel.respondToLeaveRequest(req.id, approve = true)
                                    },
                                    onReject = {
                                        viewModel.respondToLeaveRequest(req.id, approve = false)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Student Attendance Cards for the selected Class & Section
            items(scopedStudents, key = { it.id }) { stu ->
                val currentEntry = attendance.firstOrNull {
                    it.studentId == stu.id && it.date == selectedDate
                }
                val currentStatus = currentEntry?.status ?: AttendanceStatus.PRESENT
                val totalAbsences = remember(attendance, stu.id) {
                    viewModel.getTotalAbsenceDays(stu.id)
                }
                val stuBadges = remember(attendance, stu.id) {
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
                        // 1. رأس البطاقة: صورة الطالب + الاسم الثلاثي + الصف والشعبة + صف أفقي مدمج لوسام الالتزام الذهبي وعدد الغيابات
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
                                Base64AvatarImage(
                                    base64Data = stu.photoBase64,
                                    fallbackIcon = Icons.Filled.Person,
                                    contentDescription = stu.fullNameWithFather,
                                    size = 46.dp
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stu.fullNameWithFather,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${stu.gradeClass} • شعبة (${stu.sectionName})",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        if (stuBadges.isNotEmpty()) {
                                            StudentAutoBadgesRow(badges = stuBadges.take(1))
                                        }
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (featureSettings.qrAttendanceEnabled) {
                                    IconButton(
                                        onClick = { selectedStudentForIdCard = stu },
                                        modifier = Modifier
                                            .size(34.dp)
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
                                NoWrapBadge(
                                    text = "الغياب: $totalAbsences يوم",
                                    icon = Icons.Filled.EventBusy,
                                    containerColor = if (totalAbsences > 0) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.14f),
                                    contentColor = if (totalAbsences > 0) DangerRed else SuccessGreen
                                )
                            }
                        }

                        // 2. أزرار تسجيل الحضور (حاضر، غائب، متأخر، غائب بعذر): صف أفقي مدمج ومنسق بأيقونات واضحة لا تتداخل
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AttendanceStatus.entries.forEach { st ->
                                val isSelected = currentStatus == st
                                val statusColor = when (st) {
                                    AttendanceStatus.PRESENT -> SuccessGreen
                                    AttendanceStatus.ABSENT -> DangerRed
                                    AttendanceStatus.LATE -> WarningAmber
                                    AttendanceStatus.EXCUSED -> InfoBlue
                                }
                                val statusIcon = when (st) {
                                    AttendanceStatus.PRESENT -> Icons.Filled.CheckCircle
                                    AttendanceStatus.ABSENT -> Icons.Filled.Cancel
                                    AttendanceStatus.LATE -> Icons.Filled.Schedule
                                    AttendanceStatus.EXCUSED -> Icons.Filled.AssignmentLate
                                }

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(enabled = canEdit) {
                                            viewModel.recordStudentAttendance(stu, selectedDate, st)
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

                        // 3. خط فاصل ناعم وأنيق + 4. أزرار تقييم المتابعة والسلوك الصفي الفوري في صفين متوازيين (2×2) بأيقونات Vector نظيفة
                        if (canEdit && featureSettings.dailyBehaviorEnabled) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                thickness = 1.dp,
                                color = DeepEmerald.copy(alpha = 0.16f)
                            )

                            val teacherDefaultSubj = currentUser?.specialty?.split("،")?.firstOrNull()?.trim()?.ifBlank { "المتابعة اليومية" } ?: "المتابعة اليومية"
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "تقييم المتابعة والسلوك الصفي الفوري (يرسل إشعاراً لولي الأمر):",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.Bold
                                )
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
                                                DailyBehaviorType.BEHAVIOR_NOTE -> Icons.Filled.Info
                                            }
                                            Surface(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .clickable {
                                                        viewModel.sendDailyBehaviorEvaluation(
                                                            student = stu,
                                                            behaviorType = behType,
                                                            subjectName = teacherDefaultSubj
                                                        )
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
                            }
                        }

                        if (currentStatus == AttendanceStatus.ABSENT || currentStatus == AttendanceStatus.LATE) {
                            VectorActionButton(
                                text = "إرسال تبليغ ${currentStatus.arabicLabel} لولي الأمر عبر واتساب",
                                icon = Icons.Filled.Chat,
                                containerColor = WhatsAppGreen,
                                onClick = {
                                    val msg = """
                                        تبليغ رسمي من إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}):
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
        }
    }

    if (showQrScannerDialog) {
        val allSchoolStudents = remember(students, schoolConfig.schoolId) {
            students.filter { it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId }
        }
        QrAttendanceScannerDialog(
            students = allSchoolStudents,
            selectedDate = selectedDate,
            onScanQrCode = { qrValue ->
                viewModel.recordAttendanceByQrCode(qrValue, selectedDate)
            },
            onScanStudentDetailed = { qrValue ->
                viewModel.recordAttendanceByQrCodeDetailed(qrValue, selectedDate)
            },
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
        DigitalStudentIdDialog(
            student = stu,
            schoolConfig = schoolConfig,
            badges = badges,
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

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = leaveDate,
                        onValueChange = { leaveDate = it },
                        label = { Text("تاريخ الإجازة (YYYY-MM-DD)") },
                        singleLine = true,
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


