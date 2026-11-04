package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdminAccountsScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val students by viewModel.students.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val latestAnnualPromotionReport by viewModel.latestAnnualPromotionReport.collectAsState()
    val todayIso = remember { ArabicCalendarDateHelper.todayIsoDate() }

    val staffSingular = schoolConfig.schoolStage.staffTitleSingular
    val staffPlural = schoolConfig.schoolStage.staffTitlePlural
    val staffIndefinite = schoolConfig.schoolStage.staffIndefiniteSingular

    val registeredClasses = remember(schoolConfig, students) {
        (schoolConfig.schoolStage.defaultClasses + schoolConfig.registeredClasses + students.map { it.gradeClass })
            .map { ArabicTextNormalizer.cleanPureClassName(it) }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusClassName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeClass(it) }
    }
    val registeredSections = remember(schoolConfig, students) {
        (schoolConfig.registeredSections.ifEmpty { listOf("أ", "ب", "ج") } + students.map { it.sectionName })
            .map { ArabicTextNormalizer.cleanPureSectionName(it) }
            .filter { it.isNotBlank() && !ArabicTextNormalizer.isBogusSectionName(it) }
            .distinctBy { ArabicTextNormalizer.normalizeSection(it) }
    }
    val registeredSubjects = remember(schoolConfig) {
        (schoolConfig.schoolStage.defaultSubjects + schoolConfig.registeredSubjects)
            .filter { it.isNotBlank() }
            .distinctBy { ArabicTextNormalizer.normalizeText(it) }
    }

    // Search & Filters (شريط بحث فوري + فلتر حسب الدور + فلتر حسب الصف)
    var searchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("الكل") }
    var selectedClassFilter by remember { mutableStateOf("الكل") }

    var showTeacherDialog by remember { mutableStateOf(false) }
    var editingTeacher by remember { mutableStateOf<UserAccount?>(null) }

    var showStudentDialog by remember { mutableStateOf(false) }
    var editingStudent by remember { mutableStateOf<StudentRecord?>(null) }
    var studentForDigitalIdCard by remember { mutableStateOf<StudentRecord?>(null) }
    var studentForEnrollmentStatus by remember { mutableStateOf<StudentRecord?>(null) }
    var showAnnualPromotionConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(featureSettings.qrAttendanceEnabled) {
        if (!featureSettings.qrAttendanceEnabled && studentForDigitalIdCard != null) {
            studentForDigitalIdCard = null
        }
    }

    val teachers = remember(accounts, searchQuery, selectedClassFilter) {
        accounts.filter { acc ->
            acc.role == UserRole.TEACHER &&
                (searchQuery.isBlank() ||
                    acc.fullName.contains(searchQuery, ignoreCase = true) ||
                    acc.username.contains(searchQuery, ignoreCase = true) ||
                    acc.specialty.contains(searchQuery, ignoreCase = true)) &&
                (selectedClassFilter == "الكل" || acc.canTeacherAccessClassSection(selectedClassFilter, null))
        }
    }

    val filteredStudents = remember(students, searchQuery, selectedClassFilter) {
        students.filter { stu ->
            val qMatch = searchQuery.isBlank() ||
                stu.studentName.contains(searchQuery, ignoreCase = true) ||
                stu.parentName.contains(searchQuery, ignoreCase = true) ||
                stu.studentUsername.contains(searchQuery, ignoreCase = true) ||
                stu.parentUsername.contains(searchQuery, ignoreCase = true)
            val clsMatch = selectedClassFilter == "الكل" || ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClassFilter)
            qMatch && clsMatch
        }
    }

    val showTeachersList = selectedRoleFilter == "الكل" || selectedRoleFilter == "معلم" || selectedRoleFilter == "مدرس" || selectedRoleFilter == staffIndefinite
    val showStudentsParentsList = selectedRoleFilter == "الكل" || selectedRoleFilter == "ولي أمر" || selectedRoleFilter == "طالب"

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)
    ) {
        // 3D Seamless Wave Hero Header & Unclipped Action Buttons (No overlapping green badge)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ThreeDHeroHeaderCard(
                    title = "إدارة الحسابات والمستخدمين",
                    subtitle = "مرتبط ديناميكياً بالصفوف والشعب المسجلة (${registeredClasses.size} صفوف). اختر الصفوف والمواد لـ$staffPlural وأولياء الأمور والطلاب.",
                    rightPillText = schoolConfig.schoolStage.arabicLabel,
                    rightPillIcon = Icons.Filled.School,
                    leftPillText = "${registeredClasses.size} صفوف مسجلة",
                    leftPillIcon = Icons.Filled.Groups,
                    illustrationType = ThreeDIllustrationType.USER_MANAGEMENT_AVATARS_3D
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(8.dp, RoundedCornerShape(22.dp)),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    editingTeacher = null
                                    showTeacherDialog = true
                                },
                                modifier = Modifier
                                    .weight(0.92f)
                                    .height(46.dp)
                                    .testTag("add_teacher_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF059669),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.PersonAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ إضافة $staffIndefinite",
                                    fontSize = 11.2.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Button(
                                onClick = {
                                    editingStudent = null
                                    showStudentDialog = true
                                },
                                modifier = Modifier
                                    .weight(1.15f)
                                    .height(46.dp)
                                    .testTag("add_student_parent_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF0F3D5E),
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.GroupAdd,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "+ إضافة طالب وولي أمر",
                                    fontSize = 11.2.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        // Deliverable 6b: Automatic Annual Promotion & End-of-Year Principal Report Tool
                        Button(
                            onClick = {
                                viewModel.buildAnnualPromotionPreviewReport()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("open_annual_promotion_engine_btn"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DeepEmerald,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "الترحيل السنوي وتقرير النجاح العام",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Instant Search Bar + Role Filter + Class Filter
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
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("بحث فوري بالاسم أو اسم المستخدم...") },
                        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                }
                            }
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("accounts_search_bar"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Role Filter (الكل، معلم، ولي أمر، طالب)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FilterList,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("الدور:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("الكل", staffIndefinite, "ولي أمر", "طالب")) { roleOption ->
                                FilterChip(
                                    selected = selectedRoleFilter == roleOption,
                                    onClick = { selectedRoleFilter = roleOption },
                                    label = { Text(roleOption, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    // Registered Class Filter (حسب الصف المسجل)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Class,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text("الصف:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(listOf("الكل") + registeredClasses) { clsOption ->
                                FilterChip(
                                    selected = selectedClassFilter == clsOption,
                                    onClick = { selectedClassFilter = clsOption },
                                    label = { Text(clsOption, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Teachers List Section
        if (showTeachersList) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "حسابات $staffPlural (${teachers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            items(teachers, key = { it.id }) { teacher ->
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
                                    base64Data = teacher.photoBase64,
                                    fallbackIcon = Icons.Filled.CoPresent,
                                    contentDescription = teacher.fullName,
                                    size = 48.dp
                                )
                                Column {
                                    Text(
                                        text = teacher.fullName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المواد المسندة: ${teacher.specialty}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "الصفوف والشعب المسندة: ${teacher.formattedAssignedClassesForDisplay}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (teacher.canModerateActivities) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NoWrapBadge(
                                            text = "مخول بمراجعة نشاطات الطلبة",
                                            icon = Icons.Filled.VerifiedUser,
                                            containerColor = DeepEmerald.copy(alpha = 0.14f),
                                            contentColor = DeepEmerald
                                        )
                                    }
                                    if (teacher.canManageCanteen) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        NoWrapBadge(
                                            text = "مخول بإدارة كاشير الحانوت وشحن المحفظة",
                                            icon = Icons.Filled.Storefront,
                                            containerColor = DeepEmerald.copy(alpha = 0.14f),
                                            contentColor = DeepEmerald
                                        )
                                    }
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        editingTeacher = teacher
                                        showTeacherDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "تعديل صفوف ومواد المعلم",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = teacher.isActive,
                                    onCheckedChange = { viewModel.toggleAccountActive(teacher.id) }
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المستخدم: ${teacher.username}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Text(
                                    text = "كلمة المرور: ${teacher.password}",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }

                        VectorActionButton(
                            text = "إرسال بيانات الدخول للمعلم عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val msg = """
                                    السلام عليكم أستاذنا الفاضل ${teacher.fullName}
                                    إدارة (${schoolConfig.schoolName}) ترحب بكم في منصة مدرستي.
                                    بيانات الدخول الخاصة بحساب المعلم:
                                    • كود المدرسة: ${schoolConfig.schoolId}
                                    • المواد المسندة: ${teacher.specialty}
                                    • الصفوف والشعب المسندة: ${teacher.assignedClasses}
                                    • اسم المستخدم الكامل: ${teacher.username}
                                    • كلمة المرور: ${teacher.password}
                                """.trimIndent()
                                ExternalIntents.sendWhatsAppMessage(context, teacher.phone, msg)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Parents & Students List Section
        if (showStudentsParentsList) {
            item {
                Text(
                    text = "حسابات أولياء الأمور والطلاب (${filteredStudents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            items(filteredStudents, key = { it.id }) { stu ->
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
                        val stuAutoBadges = viewModel.getStudentAutoBadges(stu.id)
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
                                StudentAvatarWithOverlayBadge(
                                    base64Data = stu.photoBase64,
                                    contentDescription = stu.studentName,
                                    badge = stuAutoBadges.firstOrNull(),
                                    fallbackHonorTitle = stu.honorTitle,
                                    isOnHonorRoll = stu.isOnHonorRoll,
                                    fallbackIcon = Icons.Filled.School
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = stu.fullNameWithFather,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "الصف المسجل: ${stu.gradeClass} (شعبة ${stu.sectionName})",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "ولي الأمر: ${stu.parentName} | هاتف: ${stu.parentPhone}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    NoWrapBadge(
                                        text = "حالة القيد: ${stu.enrollmentStatusEnum.arabicLabel}",
                                        icon = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) Icons.Filled.VerifiedUser else Icons.Filled.Gavel,
                                        containerColor = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald.copy(alpha = 0.14f) else DangerRed.copy(alpha = 0.15f),
                                        contentColor = if (stu.enrollmentStatusEnum == StudentEnrollmentStatus.ACTIVE) DeepEmerald else DangerRed
                                    )
                                }
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = {
                                        editingStudent = stu
                                        showStudentDialog = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Edit,
                                        contentDescription = "تعديل بيانات الطالب وولي الأمر",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Switch(
                                    checked = stu.isActive,
                                    onCheckedChange = { viewModel.toggleStudentAndParentActive(stu.id) }
                                )
                            }
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedRoleFilter != "طالب") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "ولي الأمر: ${stu.parentUsername}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "كلمة المرور: ${stu.parentPassword}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                                if (selectedRoleFilter == "الكل") {
                                    HorizontalDivider()
                                }
                                if (selectedRoleFilter != "ولي أمر") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "الطالب: ${stu.studentUsername}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                        Text(
                                            text = "كلمة المرور: ${stu.studentPassword}",
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }
                            }
                        }

                        if (stu.enrollmentStatusEnum != StudentEnrollmentStatus.ACTIVE) {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                color = DangerRed.copy(alpha = 0.09f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                    Text(
                                        text = stu.getEnrollmentBlockBannerText(todayIso).ifBlank {
                                            "حالة القيد: ${stu.enrollmentStatusEnum.arabicLabel}${if (stu.enrollmentStatusReason.isNotBlank()) " — ${stu.enrollmentStatusReason}" else ""}"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerRed,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { studentForEnrollmentStatus = stu },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_student_enrollment_status_btn_${stu.id}"),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald)
                            ) {
                                Icon(Icons.Filled.Gavel, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "تحديث حالة قيد الطالب",
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (featureSettings.qrAttendanceEnabled) {
                                OutlinedButton(
                                    onClick = { studentForDigitalIdCard = stu },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("admin_open_student_id_${stu.id}"),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Filled.Badge, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text("بطاقة الهوية والـ QR", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }

                        if (schoolConfig.schoolType == SchoolType.PRIVATE && featureSettings.busTrackingEnabled) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilterChip(
                                    selected = stu.isBusSubscribed,
                                    onClick = { viewModel.toggleStudentBusSubscription(stu.id) },
                                    label = {
                                        Text(
                                            text = if (stu.isBusSubscribed) "مشترك بالخط المدرسي" else "غير مشترك بالخط المدرسي",
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.DirectionsBus,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }

                        VectorActionButton(
                            text = "إرسال بيانات الحساب لولي الأمر عبر واتساب",
                            icon = Icons.Filled.Chat,
                            containerColor = WhatsAppGreen,
                            onClick = {
                                val msg = """
                                    السلام عليكم السيد ولي أمر الطالب (${stu.studentName}) المحترم،
                                    تهديكم إدارة (${schoolConfig.schoolName}) أطيب التحيات.
                                    تم تفعيل حسابات المتابعة اليومية في منصة مدرستي (كود المدرسة: ${schoolConfig.schoolId}):
                                    • الصف والشعبة: ${stu.gradeClass} - شعبة (${stu.sectionName})
                                    • حساب ولي الأمر: المستخدم (${stu.parentUsername}) | الرمز (${stu.parentPassword})
                                    • حساب الطالب: المستخدم (${stu.studentUsername}) | الرمز (${stu.studentPassword})
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

    studentForDigitalIdCard?.let { stu ->
        DigitalStudentIdDialog(
            student = stu,
            schoolConfig = schoolConfig,
            badges = viewModel.getStudentAutoBadges(stu.id),
            toastAlert = toastAlert,
            onSavePng = {
                val res = com.example.util.DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = viewModel.getStudentAutoBadges(stu.id)
                )
                viewModel.showBannerMessage(res.fold({ it }, { it.message ?: "تعذر حفظ الهوية" }))
            },
            onExportPdf = {
                val msg = com.example.util.DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = stu,
                    schoolConfig = schoolConfig,
                    badges = viewModel.getStudentAutoBadges(stu.id)
                )
                viewModel.showBannerMessage(msg)
            },
            onDismiss = { studentForDigitalIdCard = null }
        )
    }

    studentForEnrollmentStatus?.let { stu ->
        StudentEnrollmentStatusModalDialog(
            student = stu,
            todayIso = todayIso,
            toastAlert = toastAlert,
            onSaveStatus = { status, reason, suspFrom, suspTo ->
                viewModel.requireOnlineAndExecute {
                    viewModel.updateStudentEnrollmentStatus(
                        studentId = stu.id,
                        newStatus = status,
                        reason = reason,
                        suspensionFromDate = suspFrom,
                        suspensionToDate = suspTo
                    )
                    studentForEnrollmentStatus = null
                }
            },
            onSaveFullDecision = { status, reason, suspFrom, suspTo, decisionDate, orderNum, transferSchool ->
                viewModel.requireOnlineAndExecute {
                    viewModel.updateStudentEnrollmentStatus(
                        studentId = stu.id,
                        newStatus = status,
                        reason = reason,
                        suspensionFromDate = suspFrom,
                        suspensionToDate = suspTo,
                        decisionDate = decisionDate,
                        orderNumber = orderNum,
                        transferredToSchoolName = transferSchool
                    )
                    studentForEnrollmentStatus = null
                }
            },
            onDismiss = { studentForEnrollmentStatus = null }
        )
    }

    if (showAnnualPromotionConfirmDialog) {
        var newYearInput by remember { mutableStateOf("2026 - 2027") }
        AlertDialog(
            onDismissRequest = { showAnnualPromotionConfirmDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.WorkspacePremium, contentDescription = null, tint = DeepEmerald)
                    Text(
                        text = "الترحيل السنوي واعتماد نتائج نهاية العام",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "سيقوم النظام بحساب المعدل السنوي النهائي لجميع الطلاب المستمرين بالدوام، وترحيل الطلاب الناجحين (معدل 50% فأعلى) تلقائياً إلى الصف الأعلى مع أرشفة سجل عامهم الدراسي الحالي، وإبقاء الطلاب المعيدين في صفوفهم، وإصدار التقرير السنوي الشامل لمدير المدرسة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMutedGreen,
                        fontWeight = FontWeight.SemiBold
                    )
                    OutlinedTextField(
                        value = newYearInput,
                        onValueChange = { newYearInput = it },
                        label = { Text("العام الدراسي الجديد بعد الترحيل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showAnnualPromotionConfirmDialog = false
                        viewModel.executeAnnualPromotionAndGenerateReport(
                            newAcademicYear = newYearInput.ifBlank { "2026 - 2027" }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DeepEmerald,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_execute_annual_promotion_btn")
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اعتماد الترحيل وإصدار التقرير السنوي", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAnnualPromotionConfirmDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    latestAnnualPromotionReport?.let { report ->
        AnnualPromotionReportModalDialog(
            report = report,
            schoolConfig = schoolConfig,
            toastAlert = toastAlert,
            onExecutePromotion = { targetYear ->
                viewModel.requireOnlineAndExecute {
                    viewModel.executeAnnualPromotionAndGenerateReport(targetYear)
                }
            },
            onManualOverrideStudent = { stuId, newClass, newSec, newStatus ->
                viewModel.requireOnlineAndExecute {
                    viewModel.manualUpdateStudentClassAndStatus(stuId, newClass, newSec, newStatus)
                }
            },
            onEditStudent = { stuId ->
                val found = students.firstOrNull { it.id == stuId }
                if (found != null) {
                    viewModel.dismissAnnualPromotionReportModal()
                    editingStudent = found
                    showStudentDialog = true
                }
            },
            onExportPdf = {
                val res = com.example.util.DocumentAndIdExporter.exportAnnualPromotionReportPdf(
                    context = context,
                    schoolConfig = schoolConfig,
                    report = report,
                    principalName = schoolConfig.principalFullName.ifBlank { "مدير المدرسة" }
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم تصدير وفتح التقرير السنوي الشامل للترحيل والنتائج بصيغة PDF بنجاح",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert(
                        "تعذر تصدير التقرير السنوي PDF: ${res.exceptionOrNull()?.message}",
                        isSuccess = false
                    )
                }
            },
            onDismiss = { viewModel.dismissAnnualPromotionReportModal() }
        )
    }

    // Add / Edit Teacher Dialog with Multi-Select Checkboxes for Registered Classes/Sections & Subjects
    if (showTeacherDialog) {
        val currentEditing = editingTeacher
        var name by remember(currentEditing) { mutableStateOf(currentEditing?.fullName ?: "") }
        var phone by remember(currentEditing) {
            mutableStateOf(IraqiPhoneCarrierValidator.sanitizeDigits(currentEditing?.phone ?: ""))
        }
        var teacherKorekOverride by remember(currentEditing) {
            mutableStateOf(IraqiPhoneCarrierValidator.isKorekNumber(currentEditing?.phone ?: ""))
        }
        var username by remember(currentEditing) {
            mutableStateOf(
                currentEditing?.username ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "t${teachers.size + 1}"
                )
            )
        }
        var password by remember(currentEditing) { mutableStateOf(currentEditing?.password ?: (1000..9999).random().toString()) }
        var teacherPhotoBase64 by remember(currentEditing) { mutableStateOf(currentEditing?.photoBase64 ?: "") }
        var canModerateActivities by remember(currentEditing) { mutableStateOf(currentEditing?.canModerateActivities ?: false) }
        var canManageCanteen by remember(currentEditing) { mutableStateOf(currentEditing?.canManageCanteen ?: false) }

        // Multi-select subjects from registeredSubjects
        val initialSubjects = remember(currentEditing) {
            currentEditing?.teacherSubjectsList?.toSet() ?: emptySet()
        }
        var selectedSubjects by remember(currentEditing) { mutableStateOf(initialSubjects) }

        // Separate multi-select for Official Classes and Official Sections (matching Attendance & Exams 100%)
        val initialTeacherClasses = remember(currentEditing, registeredClasses) {
            val existingCls = currentEditing?.teacherAssignedClassesOnly.orEmpty()
            registeredClasses.filter { regCls ->
                existingCls.any { ArabicTextNormalizer.matchesClass(it, regCls) }
            }.toSet()
        }
        var selectedTeacherClasses by remember(currentEditing) { mutableStateOf(initialTeacherClasses) }

        val initialTeacherSections = remember(currentEditing, registeredSections) {
            val existingSecs = currentEditing?.teacherAssignedSectionsOnly.orEmpty()
            val matchedSecs = registeredSections.filter { regSec ->
                existingSecs.any { ArabicTextNormalizer.matchesSection(it, regSec) }
            }.toSet()
            if (matchedSecs.isEmpty() && currentEditing != null && currentEditing.teacherAssignedClassesOnly.isNotEmpty()) {
                registeredSections.toSet()
            } else {
                matchedSecs
            }
        }
        var selectedTeacherSections by remember(currentEditing) { mutableStateOf(initialTeacherSections) }

        AlertDialog(
            onDismissRequest = { showTeacherDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentEditing == null) "إضافة $staffIndefinite وتحديد صفوفه ومادته" else "تعديل بيانات وصلاحيات $staffSingular")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("اسم $staffSingular الثلاثي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختر المواد الدراسية المسندة للمعلم:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = {
                                    selectedSubjects = if (selectedSubjects.size == registeredSubjects.size) {
                                        emptySet()
                                    } else {
                                        registeredSubjects.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedSubjects.size == registeredSubjects.size) "إلغاء تحديد الكل" else "تحديد الكل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepEmerald
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            registeredSubjects.forEach { subj ->
                                val isChecked = subj in selectedSubjects
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedSubjects = if (isChecked) {
                                            selectedSubjects - subj
                                        } else {
                                            selectedSubjects + subj
                                        }
                                    },
                                    label = { Text(subj, maxLines = 1, softWrap = false) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختر الصفوف الدراسية المسندة للمعلم:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            TextButton(
                                onClick = {
                                    selectedTeacherClasses = if (selectedTeacherClasses.size == registeredClasses.size) {
                                        emptySet()
                                    } else {
                                        registeredClasses.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedTeacherClasses.size == registeredClasses.size) "إلغاء تحديد الكل" else "تحديد الكل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepEmerald
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            registeredClasses.forEach { clsLabel ->
                                val isChecked = clsLabel in selectedTeacherClasses
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedTeacherClasses = if (isChecked) {
                                            selectedTeacherClasses - clsLabel
                                        } else {
                                            selectedTeacherClasses + clsLabel
                                        }
                                    },
                                    label = { Text(clsLabel, maxLines = 1, softWrap = false) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "اختر الشعب المسندة للمعلم:",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            TextButton(
                                onClick = {
                                    selectedTeacherSections = if (selectedTeacherSections.size == registeredSections.size) {
                                        emptySet()
                                    } else {
                                        registeredSections.toSet()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (selectedTeacherSections.size == registeredSections.size) "إلغاء تحديد الكل" else "تحديد الكل",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepEmerald
                                )
                            }
                        }
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            registeredSections.forEach { secLabel ->
                                val isChecked = secLabel in selectedTeacherSections
                                FilterChip(
                                    selected = isChecked,
                                    onClick = {
                                        selectedTeacherSections = if (isChecked) {
                                            selectedTeacherSections - secLabel
                                        } else {
                                            selectedTeacherSections + secLabel
                                        }
                                    },
                                    label = { Text("شعبة $secLabel", maxLines = 1, softWrap = false) },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    item {
                        SmartIraqiPhoneInputField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = "رقم هاتف / واتساب $staffSingular (11 رقماً)",
                            korekOverrideApproved = teacherKorekOverride,
                            onKorekOverrideChange = { teacherKorekOverride = it },
                            testTag = "teacher_phone_input_field"
                        )
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Base64AvatarImage(
                                base64Data = teacherPhotoBase64,
                                fallbackIcon = Icons.Filled.CoPresent,
                                contentDescription = name.ifBlank { "صورة المعلم" },
                                size = 48.dp
                            )
                            CompressedPhotoPickerButton(
                                label = "اختر صورة المعلم",
                                currentBase64 = teacherPhotoBase64,
                                onImageCompressed = { teacherPhotoBase64 = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.VerifiedUser,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "صلاحية مراجعة واعتماد نشاطات الطلبة",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "السماح لـ$staffSingular برؤية وإدارة (قائمة المراجعة) في قسم نشاطات الطلبة",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = canModerateActivities,
                                    onCheckedChange = { canModerateActivities = it },
                                    modifier = Modifier.testTag("teacher_moderate_activities_switch")
                                )
                            }
                        }
                    }
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Storefront,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            text = "إدارة كاشير الحانوت وشحن المحفظة",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "السماح لـ$staffSingular بمسح باركود محفظة الطالب للشراء وشحن الأرصدة في الحانوت",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Switch(
                                    checked = canManageCanteen,
                                    onCheckedChange = { canManageCanteen = it },
                                    modifier = Modifier.testTag("teacher_manage_canteen_switch")
                                )
                            }
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("اسم المستخدم") },
                                placeholder = { Text("مثال: 1 أو ali") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("كلمة المرور") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "اسم المستخدم المعتمد مع كود المدرسة: ${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, username)}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = if (currentEditing == null) "اعتماد وتفعيل الحساب" else "حفظ التعديلات",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (name.isBlank()) {
                            viewModel.showToastAlert("يرجى إدخال اسم $staffSingular الثلاثي", isSuccess = false)
                            return@VectorActionButton
                        }
                        val (phoneValid, phoneErr) = IraqiPhoneCarrierValidator.validateForSave(
                            phone = phone,
                            korekOverrideApproved = teacherKorekOverride,
                            allowEmpty = true
                        )
                        if (!phoneValid) {
                            viewModel.showToastAlert(phoneErr, isSuccess = false)
                            return@VectorActionButton
                        }
                        if (name.isNotBlank()) {
                            viewModel.requireOnlineAndExecute {
                                val specialtyStr = selectedSubjects.joinToString("، ")
                                val targetSecs = selectedTeacherSections.ifEmpty { registeredSections.toSet() }
                                val classesStr = selectedTeacherClasses.joinToString(" | ") { cls ->
                                    "${ArabicTextNormalizer.cleanPureClassName(cls)} (${targetSecs.map { ArabicTextNormalizer.cleanPureSectionName(it) }.joinToString("، ")})"
                                }
                                if (currentEditing == null) {
                                    viewModel.addTeacherAccount(
                                        fullName = name,
                                        specialty = specialtyStr,
                                        assignedClasses = classesStr,
                                        phone = phone,
                                        username = username,
                                        password = password,
                                        photoBase64 = teacherPhotoBase64,
                                        canModerateActivities = canModerateActivities,
                                        canManageCanteen = canManageCanteen
                                    )
                                } else {
                                    viewModel.updateTeacherAccount(
                                        teacherId = currentEditing.id,
                                        fullName = name,
                                        specialty = specialtyStr,
                                        assignedClasses = classesStr,
                                        phone = phone,
                                        username = username,
                                        password = password,
                                        photoBase64 = teacherPhotoBase64,
                                        canModerateActivities = canModerateActivities,
                                        canManageCanteen = canManageCanteen
                                    )
                                }
                                showTeacherDialog = false
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showTeacherDialog = false }) { Text("إلغاء") }
            }
        )
    }

    // Add / Edit Student & Parent Dialog with Dynamic Registered Class & Section Dropdowns
    if (showStudentDialog) {
        val currentStu = editingStudent
        var studentName by remember(currentStu) { mutableStateOf(currentStu?.studentName ?: "") }
        var gradeClass by remember(currentStu, registeredClasses) {
            mutableStateOf(currentStu?.gradeClass?.takeIf { it in registeredClasses } ?: registeredClasses.first())
        }
        var sectionName by remember(currentStu, registeredSections) {
            mutableStateOf(currentStu?.sectionName?.takeIf { it in registeredSections } ?: registeredSections.first())
        }
        var parentName by remember(currentStu) { mutableStateOf(currentStu?.parentName ?: "") }
        var parentPhone by remember(currentStu) {
            mutableStateOf(IraqiPhoneCarrierValidator.sanitizeDigits(currentStu?.parentPhone ?: ""))
        }
        var parentKorekOverride by remember(currentStu) {
            mutableStateOf(IraqiPhoneCarrierValidator.isKorekNumber(currentStu?.parentPhone ?: ""))
        }
        var address by remember(currentStu) { mutableStateOf(currentStu?.address ?: "") }
        var totalFeeText by remember { mutableStateOf("1500000") }
        var photoBase64 by remember(currentStu) { mutableStateOf(currentStu?.photoBase64 ?: "") }
        var sUser by remember(currentStu) {
            mutableStateOf(
                currentStu?.studentUsername ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "${students.size + 1}"
                )
            )
        }
        var sPass by remember(currentStu) { mutableStateOf(currentStu?.studentPassword ?: (1000..9999).random().toString()) }
        var pUser by remember(currentStu) {
            mutableStateOf(
                currentStu?.parentUsername ?: FirebaseConfigConstants.applySchoolPrefix(
                    schoolConfig.schoolId,
                    "p${students.size + 1}"
                )
            )
        }
        var pPass by remember(currentStu) { mutableStateOf(currentStu?.parentPassword ?: (1000..9999).random().toString()) }

        var classDropdownExpanded by remember { mutableStateOf(false) }
        var sectionDropdownExpanded by remember { mutableStateOf(false) }
        var isLinkedToExistingParent by remember(currentStu) { mutableStateOf(false) }

        val matchedParent = remember(studentName, parentName, parentPhone, students, currentStu) {
            viewModel.findExistingParentMatch(
                studentNameInput = studentName,
                parentNameInput = parentName,
                parentPhoneInput = parentPhone,
                excludeStudentId = currentStu?.id
            )
        }

        AlertDialog(
            onDismissRequest = { showStudentDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.GroupAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (currentStu == null) "إضافة طالب وولي أمره" else "تعديل بيانات الطالب وولي الأمر")
                }
            },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 460.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = studentName,
                            onValueChange = {
                                studentName = it
                                // Only link when full 4-part name is entered and confirmed via the banner button
                            },
                            label = { Text("اسم الطالب الرباعي الكامل") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Smart Parent Auto-Link Interactive Banner (الاكتشاف التلقائي لولي الأمر وربط الإخوة بعد اكتمال الاسم الرباعي)
                    if (matchedParent != null) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("smart_parent_autolink_banner"),
                                shape = RoundedCornerShape(14.dp),
                                color = if (isLinkedToExistingParent) {
                                    Color(0xFF065F46).copy(alpha = 0.14f)
                                } else {
                                    GoldAccent.copy(alpha = 0.18f)
                                },
                                border = androidx.compose.foundation.BorderStroke(
                                    1.5.dp,
                                    if (isLinkedToExistingParent) Color(0xFF059669) else DeepEmerald
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isLinkedToExistingParent) Icons.Filled.CheckCircle else Icons.Filled.FamilyRestroom,
                                            contentDescription = null,
                                            tint = if (isLinkedToExistingParent) Color(0xFF059669) else DeepEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = if (isLinkedToExistingParent) {
                                                "تم الربط الذكي بنجاح مع حساب ولي الأمر (${matchedParent.parentName})"
                                            } else {
                                                "وجدنا ولي الأمر [${matchedParent.parentName}] مسجلاً مسبقاً (لديه الطالب: ${matchedParent.existingChildrenNames.joinToString("، ")}) — هل تريد ربط الطالب الجديد بنفس حساب ولي الأمر؟"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    if (!isLinkedToExistingParent) {
                                        Button(
                                            onClick = {
                                                parentName = matchedParent.parentName
                                                if (matchedParent.parentPhone.isNotBlank()) {
                                                    parentPhone = matchedParent.parentPhone
                                                }
                                                if (matchedParent.address.isNotBlank()) {
                                                    address = matchedParent.address
                                                }
                                                pUser = matchedParent.parentUsername
                                                pPass = matchedParent.parentPassword
                                                isLinkedToExistingParent = true
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("confirm_smart_parent_link_button"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Link,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "(نعم، اربط بنفس الحساب)",
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "سيظهر الطالب الجديد تلقائياً بجانب إخوته (${matchedParent.existingChildrenNames.joinToString("، ")}) في حساب ولي الأمر الموحد (${matchedParent.parentUsername}).",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF047857),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "اختر الصف والشعبة من الصفوف المسجلة:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        // Dynamic Registered Class Dropdown
                        ExposedDropdownMenuBox(
                            expanded = classDropdownExpanded,
                            onExpandedChange = { classDropdownExpanded = !classDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = gradeClass,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الصف الدراسي المسجل") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = classDropdownExpanded,
                                onDismissRequest = { classDropdownExpanded = false }
                            ) {
                                registeredClasses.forEach { clsOption ->
                                    DropdownMenuItem(
                                        text = { Text(clsOption) },
                                        onClick = {
                                            gradeClass = clsOption
                                            classDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        // Dynamic Registered Section Dropdown
                        ExposedDropdownMenuBox(
                            expanded = sectionDropdownExpanded,
                            onExpandedChange = { sectionDropdownExpanded = !sectionDropdownExpanded }
                        ) {
                            OutlinedTextField(
                                value = "شعبة $sectionName",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("الشعبة المسجلة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionDropdownExpanded) },
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sectionDropdownExpanded,
                                onDismissRequest = { sectionDropdownExpanded = false }
                            ) {
                                registeredSections.forEach { secOption ->
                                    DropdownMenuItem(
                                        text = { Text("شعبة $secOption") },
                                        onClick = {
                                            sectionName = secOption
                                            sectionDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = parentName,
                            onValueChange = {
                                parentName = it
                                isLinkedToExistingParent = false
                            },
                            label = { Text("اسم ولي الأمر") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item {
                        SmartIraqiPhoneInputField(
                            value = parentPhone,
                            onValueChange = {
                                parentPhone = it
                                isLinkedToExistingParent = false
                            },
                            label = "رقم هاتف / واتساب ولي الأمر (11 رقماً)",
                            korekOverrideApproved = parentKorekOverride,
                            onKorekOverrideChange = { parentKorekOverride = it },
                            testTag = "parent_phone_input_field"
                        )
                    }
                    item {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            label = { Text("العنوان السكني") },
                            placeholder = { Text("أدخل العنوان السكني (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (schoolConfig.schoolType == SchoolType.PRIVATE && currentStu == null) {
                        item {
                            OutlinedTextField(
                                value = totalFeeText,
                                onValueChange = { totalFeeText = it.filter { c -> c.isDigit() } },
                                label = { Text("القسط السنوي الكلي (د.ع)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Base64AvatarImage(
                                base64Data = photoBase64,
                                fallbackIcon = Icons.Filled.School,
                                contentDescription = studentName.ifBlank { "صورة الطالب" },
                                size = 48.dp
                            )
                            CompressedPhotoPickerButton(
                                label = "اختر صورة الطالب",
                                currentBase64 = photoBase64,
                                onImageCompressed = { photoBase64 = it },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = pUser,
                                onValueChange = { pUser = it },
                                label = { Text("مستخدم ولي الأمر") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pPass,
                                onValueChange = { pPass = it },
                                label = { Text("الرمز") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedTextField(
                                value = sUser,
                                onValueChange = { sUser = it },
                                label = { Text("مستخدم الطالب") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = sPass,
                                onValueChange = { sPass = it },
                                label = { Text("الرمز") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Text(
                            text = "البادئة التلقائية: ولي الأمر (${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, pUser)}) • الطالب (${FirebaseConfigConstants.applySchoolPrefix(schoolConfig.schoolId, sUser)})",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = if (currentStu == null) "إنشاء وتفعيل الحسابين" else "حفظ التعديلات",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (studentName.isBlank() || parentName.isBlank()) {
                            viewModel.showToastAlert("يرجى إدخال اسم الطالب واسم ولي الأمر بالكامل", isSuccess = false)
                            return@VectorActionButton
                        }
                        val (phoneValid, phoneErr) = IraqiPhoneCarrierValidator.validateForSave(
                            phone = parentPhone,
                            korekOverrideApproved = parentKorekOverride,
                            allowEmpty = true
                        )
                        if (!phoneValid) {
                            viewModel.showToastAlert(phoneErr, isSuccess = false)
                            return@VectorActionButton
                        }
                        if (studentName.isNotBlank() && parentName.isNotBlank()) {
                            viewModel.requireOnlineAndExecute {
                                if (currentStu == null) {
                                    viewModel.addStudentAndParentAccounts(
                                        studentName = studentName,
                                        gradeClass = gradeClass,
                                        sectionName = sectionName,
                                        parentName = parentName,
                                        parentPhone = parentPhone,
                                        address = address.trim(),
                                        totalFee = totalFeeText.toLongOrNull() ?: 1500000L,
                                        photoBase64 = photoBase64,
                                        studentUsername = sUser,
                                        studentPassword = sPass,
                                        parentUsername = pUser,
                                        parentPassword = pPass
                                    )
                                } else {
                                    viewModel.updateStudentAndParentAccounts(
                                        studentId = currentStu.id,
                                        studentName = studentName,
                                        gradeClass = gradeClass,
                                        sectionName = sectionName,
                                        parentName = parentName,
                                        parentPhone = parentPhone,
                                        address = address,
                                        studentUsername = sUser,
                                        studentPassword = sPass,
                                        parentUsername = pUser,
                                        parentPassword = pPass,
                                        photoBase64 = photoBase64
                                    )
                                }
                                showStudentDialog = false
                            }
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showStudentDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Deliverable 6a: Official Student Enrollment Status Modal Dialog
 * Allows the Principal to set and archive in Firestore:
 * - ACTIVE ("مستمر بالدوام")
 * - SUSPENDED_TEMP ("فصل مؤقت من تاريخ - إلى تاريخ" + سبب الفصل المؤقت)
 * - EXPELLED ("فصل دائمي" + سبب وتاريخ القرار ورقم الأمر الإداري)
 * - DEREGISTERED ("ترقين قيد" + سبب وتاريخ ترقين القيد ورقم الأمر الإداري)
 * - TRANSFERRED ("منقول" + اسم المدرسة المنقول إليها وتاريخ النقل)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentEnrollmentStatusModalDialog(
    student: StudentRecord,
    todayIso: String,
    toastAlert: ToastAlertMessage? = null,
    onSaveStatus: (StudentEnrollmentStatus, String, String, String) -> Unit = { _, _, _, _ -> },
    onSaveFullDecision: ((StudentEnrollmentStatus, String, String, String, String, String, String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var selectedStatus by remember(student.id) { mutableStateOf(student.enrollmentStatusEnum) }
    var reason by remember(student.id) { mutableStateOf(student.enrollmentStatusReason) }
    var suspensionFrom by remember(student.id) {
        mutableStateOf(student.suspensionFromDate.ifBlank { todayIso })
    }
    var suspensionTo by remember(student.id) {
        mutableStateOf(student.suspensionToDate.ifBlank { todayIso })
    }
    var decisionDate by remember(student.id) {
        mutableStateOf(student.enrollmentDecisionDate.ifBlank { todayIso })
    }
    var orderNumber by remember(student.id) {
        mutableStateOf(student.enrollmentOrderNumber)
    }
    var transferSchoolName by remember(student.id) {
        mutableStateOf(student.transferredToSchoolName)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                InlineDialogToastBanner(toastAlert = toastAlert)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.Gavel, contentDescription = null, tint = DeepEmerald)
                    Text(
                        text = "اتخاذ قرار إداري بحالة قيد الطالب",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }
                Text(
                    text = "الطالب: ${student.fullNameWithFather} • ${student.gradeClass} (شعبة ${student.sectionName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedGreen,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "اختر القرار الإداري وحالة قيد الطالب الرسمية:",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepForestGreen
                )

                StudentEnrollmentStatus.entries.forEach { st ->
                    val isSel = selectedStatus == st
                    val statusCol = if (st == StudentEnrollmentStatus.ACTIVE) DeepEmerald else DangerRed
                    val subDesc = when (st) {
                        StudentEnrollmentStatus.ACTIVE -> "إعادة تفعيل القيد الرسمي والسماح بالحضور والامتحانات"
                        StudentEnrollmentStatus.SUSPENDED_TEMP -> "قفل الحضور تلقائياً من تاريخ البدء إلى تاريخ الانتهاء وإشعار ولي الأمر"
                        StudentEnrollmentStatus.EXPELLED -> "فصل دائمي من المدرسة مع توثيق السبب وتاريخ القرار"
                        StudentEnrollmentStatus.DEREGISTERED -> "ترقين قيد الطالب رسمياً مع توثيق رقم الأمر الإداري والتاريخ"
                        StudentEnrollmentStatus.TRANSFERRED -> "نقل قيد الطالب إلى مدرسة أخرى مع توثيق اسم المدرسة وتاريخ النقل"
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { selectedStatus = st }
                            .testTag("select_enrollment_status_${st.name}"),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSel) statusCol.copy(alpha = 0.12f) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSel) 1.5.dp else 1.dp,
                            if (isSel) statusCol else DeepEmerald.copy(alpha = 0.2f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = st.arabicLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSel) statusCol else DeepForestGreen
                                )
                                Text(
                                    text = subDesc,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMutedGreen
                                )
                            }
                            Icon(
                                imageVector = if (isSel) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                                contentDescription = null,
                                tint = if (isSel) statusCol else TextMutedGreen,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                when (selectedStatus) {
                    StudentEnrollmentStatus.ACTIVE -> {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFECFDF5),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "سيتم اعتماد قيد الطالب (مستمر بالدوام) وإلغاء أي إيقاف سابق عن سجل الحضور والامتحانات.",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepForestGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                    StudentEnrollmentStatus.SUSPENDED_TEMP -> {
                        Text(
                            text = "فترة الفصل المؤقت (يُقفل حضور الطالب تلقائياً خلالها ويُشعر ولي الأمر):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DangerRed
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ModernCalendarDatePickerField(
                                selectedDateIso = suspensionFrom,
                                label = "من تاريخ (YYYY-MM-DD)",
                                onDateSelected = { iso, _ -> suspensionFrom = iso },
                                modifier = Modifier.weight(1f)
                            )
                            ModernCalendarDatePickerField(
                                selectedDateIso = suspensionTo,
                                label = "إلى تاريخ (YYYY-MM-DD)",
                                onDateSelected = { iso, _ -> suspensionTo = iso },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("سبب الفصل المؤقت") },
                            placeholder = { Text("اكتب سبب الفصل المؤقت ليتم إشعار ولي الأمر وتوثيقه في السجل...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                        OutlinedTextField(
                            value = orderNumber,
                            onValueChange = { orderNumber = it },
                            label = { Text("رقم الأمر الإداري (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    StudentEnrollmentStatus.EXPELLED -> {
                        ModernCalendarDatePickerField(
                            selectedDateIso = decisionDate,
                            label = "تاريخ قرار الفصل الدائمي (YYYY-MM-DD)",
                            onDateSelected = { iso, _ -> decisionDate = iso },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = orderNumber,
                            onValueChange = { orderNumber = it },
                            label = { Text("رقم الأمر الإداري للفصل الدائمي") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("سبب الفصل الدائمي") },
                            placeholder = { Text("اكتب سبب الفصل الدائمي لتوثيقه في أرشيف الطالب...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    StudentEnrollmentStatus.DEREGISTERED -> {
                        ModernCalendarDatePickerField(
                            selectedDateIso = decisionDate,
                            label = "تاريخ ترقين القيد (YYYY-MM-DD)",
                            onDateSelected = { iso, _ -> decisionDate = iso },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = orderNumber,
                            onValueChange = { orderNumber = it },
                            label = { Text("رقم الأمر الإداري لترقين القيد") },
                            placeholder = { Text("مثال: الأمر الإداري المرقم 145 في ...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("سبب ترقين القيد") },
                            placeholder = { Text("مثال: تجاوز الحد القانوني للغياب / انقطاع عن الدوام...") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                    StudentEnrollmentStatus.TRANSFERRED -> {
                        OutlinedTextField(
                            value = transferSchoolName,
                            onValueChange = { transferSchoolName = it },
                            label = { Text("اسم المدرسة المنقول إليها الطالب") },
                            placeholder = { Text("اكتب اسم المدرسة التي انتقل إليها الطالب...") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        ModernCalendarDatePickerField(
                            selectedDateIso = decisionDate,
                            label = "تاريخ النقل (YYYY-MM-DD)",
                            onDateSelected = { iso, _ -> decisionDate = iso },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = orderNumber,
                            onValueChange = { orderNumber = it },
                            label = { Text("رقم وكتاب النقل (اختياري)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = reason,
                            onValueChange = { reason = it },
                            label = { Text("سبب النقل / ملاحظات إدارية") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanReason = reason.trim()
                    val cleanFrom = if (selectedStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) suspensionFrom else ""
                    val cleanTo = if (selectedStatus == StudentEnrollmentStatus.SUSPENDED_TEMP) suspensionTo else ""
                    if (onSaveFullDecision != null) {
                        onSaveFullDecision(
                            selectedStatus,
                            cleanReason,
                            cleanFrom,
                            cleanTo,
                            decisionDate.trim(),
                            orderNumber.trim(),
                            transferSchoolName.trim()
                        )
                    } else {
                        onSaveStatus(selectedStatus, cleanReason, cleanFrom, cleanTo)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedStatus == StudentEnrollmentStatus.ACTIVE) DeepEmerald else DangerRed,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_save_enrollment_status_btn")
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("اعتماد وتوثيق القرار سحابياً", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Deliverable 6b: Interactive Annual Promotion & End-of-Year Principal Report Modal
 * Shows:
 * - Overall school pass rate, total promoted, graduated, and repeating students
 * - Per-class pass rate breakdown
 * - One-click button to execute automatic annual promotion of all passing students to their new classes
 * - Tabs for Promoted / Graduated vs Repeating students with direct manual class/status editing
 * - Button to export the official PDF report
 */
@Composable
fun AnnualPromotionReportModalDialog(
    report: AnnualPromotionReport,
    schoolConfig: SchoolConfig,
    toastAlert: ToastAlertMessage? = null,
    onExecutePromotion: ((String) -> Unit)? = null,
    onManualOverrideStudent: ((String, String, String, StudentEnrollmentStatus) -> Unit)? = null,
    onEditStudent: (String) -> Unit = {},
    onExportPdf: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Class Breakdown, 1 = Promoted, 2 = Repeating, 3 = Pending Grades
    fun formatYearLtr(raw: String): String {
        val nums = Regex("\\d{4}").findAll(raw).mapNotNull { it.value.toIntOrNull() }.toList().sorted()
        return if (nums.size >= 2) "\u200E${nums[0]} - ${nums[1]}\u200E" else "\u200E${raw.trim()}\u200E"
    }
    val displayPrevYear = remember(report.previousAcademicYear) {
        formatYearLtr(report.previousAcademicYear.ifBlank { "2026 - 2027" })
    }
    var targetNewAcademicYear by remember(report.newAcademicYear) {
        mutableStateOf(formatYearLtr(report.newAcademicYear.ifBlank { "2027 - 2028" }))
    }
    var studentForQuickManualOverride by remember { mutableStateOf<PromotedStudentItem?>(null) }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .heightIn(max = 840.dp)
                .testTag("annual_promotion_report_modal"),
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
                                imageVector = Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = DeepEmerald,
                                modifier = Modifier
                                    .padding(9.dp)
                                    .size(22.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "تقرير الترحيل السنوي ونتائج النجاح العام",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepForestGreen
                            )
                            Text(
                                text = "${schoolConfig.schoolName} • من العام ($displayPrevYear) إلى العام (${formatYearLtr(targetNewAcademicYear)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMutedGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "إغلاق", tint = DeepForestGreen)
                    }
                }

                // Summary Metrics Row (including Pending Grades count)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    listOf(
                        Triple("نسبة النجاح", "${report.overallPassRatePercent}%", DeepEmerald),
                        Triple("الناجحون", "${report.promotedCount}", SuccessGreen),
                        Triple("المعيدون", "${report.repeatingCount}", DangerRed),
                        Triple("قيد الرصد", "${report.pendingGradesCount}", InfoBlue)
                    ).forEach { (lbl, valTxt, col) ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = col.copy(alpha = 0.10f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, col.copy(alpha = 0.35f))
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 7.dp, horizontal = 3.dp),
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
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Automatic Promotion Execution Box
                if (onExecutePromotion != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = targetNewAcademicYear,
                                    onValueChange = { targetNewAcademicYear = it },
                                    label = { Text("العام الدراسي الجديد بعد الترحيل (مثال: 2027 - 2028)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                            Button(
                                onClick = {
                                    onExecutePromotion(formatYearLtr(targetNewAcademicYear.ifBlank { "2027 - 2028" }))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("execute_auto_annual_promotion_now_btn"),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = DeepEmerald,
                                    contentColor = Color.White
                                )
                            ) {
                                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اعتماد وترحيل الطلاب الناجحين المكتملة درجاتهم إلى صفوفهم الأعلى",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }

                // Export Official PDF Button
                OutlinedButton(
                    onClick = onExportPdf,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("export_annual_promotion_pdf_btn"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 9.dp),
                    border = androidx.compose.foundation.BorderStroke(1.2.dp, DeepEmerald)
                ) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = DeepEmerald, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "طباعة / تصدير التقرير السنوي الشامل للمدير (PDF)",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepForestGreen,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Tabs (4 clear full-text tabs)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val tabs = listOf(
                        0 to "نسب النجاح حسب الصف (${report.classBreakdowns.size})",
                        1 to "الناجحون المرحّلون (${report.promotedStudents.size})",
                        2 to "المعيدون (${report.repeatingStudents.size})",
                        3 to "بانتظار استكمال الدرجات (${report.pendingGradesStudents.size})"
                    )
                    items(tabs) { (idx, title) ->
                        val isSel = selectedTab == idx
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isSel) DeepEmerald else Color(0xFFF0FDF4),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSel) DeepEmerald else DeepEmerald.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { selectedTab = idx }
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSel) Color.White else DeepForestGreen,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.16f))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            items(report.classBreakdowns, key = { it.gradeClass }) { cb ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFF0FDF4),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "من (${cb.gradeClass})  ←  إلى (${cb.nextGradeClass})",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = "الإجمالي: ${cb.totalStudents} • ناجح ومرحّل: ${cb.passedCount} • معيد: ${cb.repeatingCount} • بانتظار رصد الدرجات: ${cb.pendingGradesCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                        NoWrapBadge(
                                            text = "النجاح: ${cb.passRatePercent}%",
                                            icon = Icons.Filled.TrendingUp,
                                            containerColor = DeepEmerald,
                                            contentColor = Color.White
                                        )
                                    }
                                }
                            }
                        }
                        1 -> {
                            if (report.promotedStudents.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "لا يوجد طلاب مكتملو الدرجات النهائية وناجحون للترحيل حالياً. يرجى استكمال رصد الدرجات أولاً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestGreen,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            } else {
                                items(report.promotedStudents, key = { it.studentId }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.studentName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen
                                                )
                                                Text(
                                                    text = "يرحّل من (${item.previousGrade}) إلى (${item.newGrade}) • المعدل: ${item.finalAverage}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DeepEmerald,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    if (onManualOverrideStudent != null) {
                                                        studentForQuickManualOverride = item
                                                    } else {
                                                        onEditStudent(item.studentId)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تعديل الصف/الحالة", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        2 -> {
                            if (report.repeatingStudents.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "لا يوجد طلاب معيدون مكتملو الدرجات حالياً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestGreen,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            } else {
                                items(report.repeatingStudents, key = { it.studentId }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = DangerRed.copy(alpha = 0.06f),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.studentName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen
                                                )
                                                Text(
                                                    text = "باقٍ للإعادة في (${item.previousGrade} - شعبة ${item.sectionName}) • المعدل: ${item.finalAverage}%",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = DangerRed,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    if (onManualOverrideStudent != null) {
                                                        studentForQuickManualOverride = item
                                                    } else {
                                                        onEditStudent(item.studentId)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تعديل الصف/الحالة", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        3 -> {
                            if (report.pendingGradesStudents.isEmpty()) {
                                item {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFECFDF5),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "ممتاز! جميع طلاب المدرسة مكتملو الدرجات النهائية وجاهزون للترحيل.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = DeepForestGreen,
                                            modifier = Modifier.padding(14.dp)
                                        )
                                    }
                                }
                            } else {
                                items(report.pendingGradesStudents, key = { it.studentId }) { item ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFF8FAFC),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, InfoBlue.copy(alpha = 0.38f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.studentName,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepForestGreen
                                                )
                                                Text(
                                                    text = "الصف الحالي: (${item.previousGrade} - شعبة ${item.sectionName}) • بانتظار استكمال الدرجات (${item.completedSubjectsCount}/${item.requiredSubjectsCount} مادة مرصودة)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = InfoBlue,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    if (onManualOverrideStudent != null) {
                                                        studentForQuickManualOverride = item
                                                    } else {
                                                        onEditStudent(item.studentId)
                                                    }
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("ترحيل يدوي", style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
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

    studentForQuickManualOverride?.let { targetItem ->
        var selClass by remember(targetItem.studentId) { mutableStateOf(targetItem.newGrade.ifBlank { targetItem.previousGrade }) }
        var selSec by remember(targetItem.studentId) { mutableStateOf(targetItem.sectionName.ifBlank { "أ" }) }
        var selStatus by remember(targetItem.studentId) { mutableStateOf(StudentEnrollmentStatus.ACTIVE) }
        val classChoices = remember(schoolConfig.registeredClasses) {
            (schoolConfig.registeredClasses + schoolConfig.schoolStage.defaultClasses).distinct()
        }

        AlertDialog(
            onDismissRequest = { studentForQuickManualOverride = null },
            title = {
                Text(
                    text = "تعديل يدوي لصف أو حالة الطالب: ${targetItem.studentName}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepForestGreen
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر الصف الدراسي للطالب:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(classChoices) { cls ->
                            FilterChip(
                                selected = selClass == cls,
                                onClick = { selClass = cls },
                                label = { Text(cls, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = selSec,
                        onValueChange = { selSec = it },
                        label = { Text("الشعبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("حالة القيد:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(StudentEnrollmentStatus.entries) { st ->
                            FilterChip(
                                selected = selStatus == st,
                                onClick = { selStatus = st },
                                label = { Text(st.arabicLabel, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onManualOverrideStudent?.invoke(targetItem.studentId, selClass, selSec, selStatus)
                        studentForQuickManualOverride = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
                ) {
                    Text("حفظ التعديل سحابياً", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { studentForQuickManualOverride = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}


