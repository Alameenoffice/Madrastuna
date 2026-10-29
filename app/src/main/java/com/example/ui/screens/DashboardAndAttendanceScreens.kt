package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

@Composable
fun DashboardScreen(
    viewModel: SchoolViewModel,
    onOpenNotificationsDialog: () -> Unit
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val attendance by viewModel.attendance.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val tuitions by viewModel.tuitions.collectAsState()

    val teachersCount = remember(accounts) { accounts.count { it.role == UserRole.TEACHER } }
    val honorCount = remember(students) { students.count { it.isOnHonorRoll } }
    val presentCount = remember(attendance) { attendance.count { it.status == AttendanceStatus.PRESENT } }
    val absentCount = remember(attendance) { attendance.count { it.status == AttendanceStatus.ABSENT } }
    val unreadNotifications = remember(notifications) { notifications.count { !it.isRead } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
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
                                text = "${schoolConfig.schoolType.arabicLabel} • ${schoolConfig.address}",
                                icon = Icons.Filled.LocationOn,
                                containerColor = Color.White.copy(alpha = 0.2f),
                                contentColor = Color.White
                            )
                            NoWrapBadge(
                                text = currentUser?.role?.arabicTitle ?: "مستخدم",
                                icon = Icons.Filled.VerifiedUser,
                                containerColor = GoldAccent,
                                contentColor = Color(0xFF451A03)
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
                                    text = "${schoolConfig.schoolName} — العام الدراسي ${schoolConfig.academicYear}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        if (currentUser?.role == UserRole.ADMIN) {
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

        // Smart Stat Cards Grid (بطاقات إحصائية ذكية)
        item {
            Text(
                text = "لوحة الإحصائيات الفورية",
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
                        title = "إجمالي الطلبة",
                        value = "${students.size} طالب",
                        subtitle = "الكادر التدريسي: $teachersCount",
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
                        subtitle = "محدث لجميع الشعب",
                        icon = Icons.Filled.MenuBook,
                        accentColor = SecondaryLight,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.navigateToSection(AppSection.SCHEDULE_HOMEWORK) }
                    )
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

                if (schoolConfig.schoolType == SchoolType.PRIVATE) {
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
            }
        }

        // All 8 Sections Quick Access Grid
        item {
            Text(
                text = "الأقسام التعليمية والإدارية المتكاملة",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))

            val sectionsList = remember(schoolConfig.schoolType, currentUser) {
                buildList {
                    add(Triple(AppSection.ATTENDANCE, Icons.Filled.HowToReg, "رصد الحضور والغياب وتبليغ واتساب"))
                    add(Triple(AppSection.SCHEDULE_HOMEWORK, Icons.Filled.CalendarMonth, "جدول الحصص والتحضير اليومي"))
                    add(Triple(AppSection.EXAMS_GRADES, Icons.Filled.Assessment, "الامتحانات وكشف الدرجات والسعي"))
                    add(Triple(AppSection.HONOR_ROLL, Icons.Filled.WorkspacePremium, "لوحة الشرف والأوسمة الذهبية"))
                    if (schoolConfig.schoolType == SchoolType.PRIVATE) {
                        add(Triple(AppSection.TUITION_FEES, Icons.Filled.AccountBalanceWallet, "الأقساط الدراسية ووصولات الدفع"))
                    }
                    add(Triple(AppSection.REPORTS_CHAT, Icons.Filled.Forum, "البلاغات الرسمية والمراسلة المباشرة"))
                    add(Triple(AppSection.EVENTS_NEWS, Icons.Filled.Newspaper, "معرض الفعاليات والأخبار المدرسية"))
                    if (currentUser?.role == UserRole.ADMIN) {
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

        // Instant Notification Center Summary Card
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

                    notifications.take(3).forEach { notif ->
                        Surface(
                            color = if (!notif.isRead) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = when (notif.type) {
                                        NotificationType.ATTENDANCE -> Icons.Filled.EventBusy
                                        NotificationType.HOMEWORK -> Icons.Filled.MenuBook
                                        NotificationType.GRADES -> Icons.Filled.Grade
                                        NotificationType.TUITION -> Icons.Filled.Payments
                                        NotificationType.REPORT -> Icons.Filled.Campaign
                                        NotificationType.GENERAL -> Icons.Filled.Notifications
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = notif.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = notif.createdAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = notif.body,
                                        style = MaterialTheme.typography.bodySmall,
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
 * - Select Class, Section, Date
 * - Record status (حاضر، غائب، متأخر، إجازة)
 * - Instant parent notification on غائب or متأخر
 * - Direct WhatsApp absence notification button
 * - Total Absence Days counter (عداد إجمالي أيام الغياب)
 */
@Composable
fun DailyAttendanceScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val attendance by viewModel.attendance.collectAsState()

    var selectedClass by remember { mutableStateOf("الكل") }
    var selectedSection by remember { mutableStateOf("الكل") }
    var selectedDate by remember { mutableStateOf("2026-10-29") }

    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val filteredStudents = remember(students, selectedClass, selectedSection, currentUser) {
        students.filter { stu ->
            val roleMatch = when (currentUser?.role) {
                UserRole.PARENT, UserRole.STUDENT -> stu.id == currentUser?.linkedStudentId || currentUser?.linkedStudentId.isNullOrBlank()
                else -> true
            }
            val classMatch = selectedClass == "الكل" || stu.gradeClass == selectedClass
            val sectionMatch = selectedSection == "الكل" || stu.sectionName == selectedSection
            roleMatch && classMatch && sectionMatch
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        // Filter Card (اختيار الصف والشعبة والتاريخ)
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

                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = { selectedDate = it },
                        label = { Text("تاريخ الرصد اليومي") },
                        leadingIcon = { Icon(Icons.Filled.CalendarToday, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text(
                        text = "تصفية حسب الصف الدراسي:",
                        style = MaterialTheme.typography.labelLarge
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val classesWithAll = listOf("الكل") + viewModel.availableClasses
                        items(classesWithAll) { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = { selectedClass = cls },
                                label = {
                                    Text(cls, maxLines = 1, softWrap = false)
                                },
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
                        Text("الشعبة:", style = MaterialTheme.typography.labelLarge)
                        (listOf("الكل") + viewModel.availableSections).forEach { sec ->
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

        // Student Attendance Cards
        items(filteredStudents, key = { it.id }) { stu ->
            val currentEntry = attendance.firstOrNull {
                it.studentId == stu.id && it.date == selectedDate
            }
            val currentStatus = currentEntry?.status ?: AttendanceStatus.PRESENT
            val totalAbsences = remember(attendance, stu.id) {
                viewModel.getTotalAbsenceDays(stu.id)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Base64AvatarImage(
                                base64Data = stu.photoBase64,
                                fallbackIcon = Icons.Filled.Person,
                                contentDescription = stu.studentName,
                                size = 46.dp
                            )
                            Column {
                                Text(
                                    text = stu.studentName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${stu.gradeClass} • شعبة (${stu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Total Absence Counter Badge (عداد إجمالي أيام الغياب)
                        NoWrapBadge(
                            text = "إجمالي الغياب: $totalAbsences يوم",
                            icon = Icons.Filled.EventBusy,
                            containerColor = if (totalAbsences > 0) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.14f),
                            contentColor = if (totalAbsences > 0) DangerRed else SuccessGreen
                        )
                    }

                    // 4 Attendance Status Buttons (حاضر، غائب، متأخر، إجازة)
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
                                color = if (isSelected) statusColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = statusIcon,
                                        contentDescription = st.arabicLabel,
                                        tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = st.arabicLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Instant WhatsApp Alert Button for Absent / Late Students
                    if (currentStatus == AttendanceStatus.ABSENT || currentStatus == AttendanceStatus.LATE) {
                        VectorActionButton(
                            text = "إرسال تبليغ ${currentStatus.arabicLabel} لولي الأمر عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val msg = """
                                    تبليغ رسمي من إدارة (${schoolConfig.schoolName} - ${schoolConfig.address}):
                                    إلى السيد ولي أمر الطالب (${stu.studentName}) المحترم،
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
