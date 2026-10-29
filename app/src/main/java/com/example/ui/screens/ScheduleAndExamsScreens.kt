package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

/**
 * Section 3: Weekly Schedule & Daily Preparation / Homework Screen
 * Dynamically scoped to registered classes/sections (and locked to the student's own class/section for Parent/Student).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleAndHomeworkScreen(
    viewModel: SchoolViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val allowedClasses = remember(schoolConfig, currentUser) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, selectedClass) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    val allowedSubjects = remember(schoolConfig, currentUser) {
        viewModel.getAllowedSubjectsForUser(currentUser)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = التحضير والواجبات, 1 = جدول الحصص الأسبوعي
    var selectedDay by remember { mutableStateOf("الأحد") }

    var showPublishHomeworkDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }

    val filteredHomeworks = remember(homeworks, selectedClass, selectedSection, currentUser) {
        homeworks.filter { hw ->
            val clsMatch = hw.gradeClass == selectedClass && hw.sectionName == selectedSection
            val subjMatch = if (currentUser?.role == UserRole.TEACHER) {
                currentUser?.canTeacherAccessSubject(hw.subjectName) == true
            } else true
            clsMatch && subjMatch
        }
    }

    val filteredPeriods = remember(schedules, selectedClass, selectedSection, selectedDay) {
        schedules.filter {
            it.gradeClass == selectedClass &&
                it.sectionName == selectedSection &&
                it.dayOfWeek == selectedDay
        }.sortedBy { it.periodNumber }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_daily_homework"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("التحضير والواجبات", maxLines = 1, softWrap = false)
            }

            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_weekly_schedule"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("جدول الحصص الأسبوعي", maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (!isParentOrStudent) {
            // Class & Section Selector from Registered/Allowed lists
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allowedClasses) { cls ->
                    FilterChip(
                        selected = selectedClass == cls,
                        onClick = {
                            selectedClass = cls
                            val secs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                            if (selectedSection !in secs) selectedSection = secs.firstOrNull() ?: "أ"
                        },
                        label = { Text(cls, maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (selectedClass == cls) Icons.Filled.Check else Icons.Filled.School,
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
                allowedSections.forEach { sec ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                    )
                }
            }
        } else {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Filled.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "جدول وتحضير الصف الخاص بالطالب: $selectedClass (شعبة $selectedSection)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            if (canPublish) {
                VectorActionButton(
                    text = "نشر التحضير اليومي والواجب البيتي لـ ($selectedClass - $selectedSection)",
                    icon = Icons.Filled.PostAdd,
                    onClick = { showPublishHomeworkDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "publish_homework_button"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                if (filteredHomeworks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AssignmentTurnedIn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("لا توجد واجبات منشورة لـ $selectedClass ($selectedSection) حالياً", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } else {
                    items(filteredHomeworks, key = { it.id }) { hw ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    NoWrapBadge(
                                        text = "مادة ${hw.subjectName}",
                                        icon = Icons.Filled.AutoStories,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    NoWrapBadge(
                                        text = "التسليم: ${hw.dueDate}",
                                        icon = Icons.Filled.Event,
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Text(
                                    text = "الصف: ${hw.gradeClass} (شعبة ${hw.sectionName}) • المعلم: ${hw.teacherName}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Surface(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "التحضير اليومي: ${hw.dailyPreparation}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Text(
                                            text = "الواجب البيتي: ${hw.homeworkDetails}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(viewModel.weekDays) { day ->
                    FilterChip(
                        selected = selectedDay == day,
                        onClick = { selectedDay = day },
                        label = { Text("يوم $day", maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            Icon(Icons.Filled.Today, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (canPublish) {
                VectorActionButton(
                    text = "إضافة أو تعديل حصة في الجدول الأسبوعي",
                    icon = Icons.Filled.AddAlarm,
                    onClick = { showAddPeriodDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredPeriods, key = { it.id }) { period ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary
                                ) {
                                    Text(
                                        text = "الحصة ${period.periodNumber}",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                Column {
                                    Text(
                                        text = period.subjectName,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "المعلم: ${period.teacherName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            NoWrapBadge(
                                text = period.timeSlot,
                                icon = Icons.Filled.AccessTime,
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }

    // Publish Homework Modal with Dynamic Registered Subject Selector
    if (showPublishHomeworkDialog) {
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var subjExpanded by remember { mutableStateOf(false) }
        var prep by remember { mutableStateOf("") }
        var hwDetails by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-10-31") }

        AlertDialog(
            onDismissRequest = { showPublishHomeworkDialog = false },
            title = { Text("نشر التحضير لـ ($selectedClass - $selectedSection)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = subjExpanded,
                        onExpandedChange = { subjExpanded = !subjExpanded }
                    ) {
                        OutlinedTextField(
                            value = subject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختر المادة الدراسية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = subjExpanded,
                            onDismissRequest = { subjExpanded = false }
                        ) {
                            allowedSubjects.forEach { sOption ->
                                DropdownMenuItem(
                                    text = { Text(sOption) },
                                    onClick = {
                                        subject = sOption
                                        subjExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = prep,
                        onValueChange = { prep = it },
                        label = { Text("التحضير اليومي للدرس القادم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = hwDetails,
                        onValueChange = { hwDetails = it },
                        label = { Text("تفاصيل الواجب البيتي المطلوب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text("تاريخ التسليم") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "نشر وإشعار الطلاب وأولياء الأمور",
                    icon = Icons.Filled.Send,
                    onClick = {
                        if (prep.isNotBlank()) {
                            viewModel.publishHomework(
                                gradeClass = selectedClass,
                                sectionName = selectedSection,
                                subjectName = subject,
                                teacherName = currentUser?.fullName ?: "معلم المادة",
                                dailyPreparation = prep,
                                homeworkDetails = hwDetails.ifBlank { "مراجعة الدرس وحل أسئلة الفصل" },
                                dueDate = dueDate
                            )
                            showPublishHomeworkDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPublishHomeworkDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showAddPeriodDialog) {
        var periodNo by remember { mutableStateOf("5") }
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var teacher by remember { mutableStateOf(currentUser?.fullName ?: "أ. عمر قاسم الحديثي") }
        var timeSlot by remember { mutableStateOf("11:30 - 12:15") }

        AlertDialog(
            onDismissRequest = { showAddPeriodDialog = false },
            title = { Text("إضافة حصة ليوم $selectedDay ($selectedClass - $selectedSection)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = periodNo,
                        onValueChange = { periodNo = it.filter { c -> c.isDigit() } },
                        label = { Text("رقم الحصة (1 - 7)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allowedSubjects) { sOpt ->
                            FilterChip(
                                selected = subject == sOpt,
                                onClick = { subject = sOpt },
                                label = { Text(sOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = teacher,
                        onValueChange = { teacher = it },
                        label = { Text("اسم مدرس المادة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("الوقت") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ في الجدول",
                    icon = Icons.Filled.Save,
                    onClick = {
                        viewModel.addSchedulePeriod(
                            gradeClass = selectedClass,
                            sectionName = selectedSection,
                            dayOfWeek = selectedDay,
                            periodNumber = periodNo.toIntOrNull() ?: 1,
                            subjectName = subject,
                            teacherName = teacher,
                            timeSlot = timeSlot
                        )
                        showAddPeriodDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddPeriodDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Section 4: Restructured Exams & Detailed Gradebook Screen
 * - For Admin & Teacher:
 *   Step A: Choose Registered Class -> Choose Registered Section -> Choose Subject
 *   Step B: Displays ONLY students in that Class & Section + Instant Search by Name bar
 *   Step C: Inline editable grade fields (يومي، الشهر الأول، الشهر الثاني، نصف السنة، السعي، النهائي) for each student in that selected subject with instant Save!
 * - For Student & Parent:
 *   Step D: Displays a clean, organized Student Report Card Table showing ONLY that student's subjects and detailed grades.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamsAndGradesScreen(
    viewModel: SchoolViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val examSchedules by viewModel.examSchedules.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val allowedClasses = remember(schoolConfig, currentUser) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, selectedClass) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    val allowedSubjects = remember(schoolConfig, currentUser) {
        viewModel.getAllowedSubjectsForUser(currentUser)
    }
    var selectedSubject by remember(allowedSubjects) {
        mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات")
    }

    var searchStudentName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = رصد/كشف الدرجات, 1 = مواعيد الامتحانات
    var showAddExamDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_detailed_grades"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Assessment, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isParentOrStudent) "جدول كشف الدرجات" else "رصد الدرجات حسب المادة",
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_exam_schedule"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.EventNote, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("مواعيد الامتحانات", maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            if (isParentOrStudent) {
                // =============================================================
                // STEP D: STUDENT & PARENT ORGANIZED REPORT CARD TABLE
                // =============================================================
                val myStu = viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
                if (myStu != null) {
                    val myGrades = grades.filter { it.studentId == myStu.id }
                    val allSubjectsReport = schoolConfig.registeredSubjects.map { subjName ->
                        myGrades.firstOrNull { it.subjectName == subjName } ?: SubjectGrade(
                            id = "temp_$subjName",
                            studentId = myStu.id,
                            studentName = myStu.studentName,
                            gradeClass = myStu.gradeClass,
                            subjectName = subjName,
                            dailyScore = 92,
                            month1Score = 90,
                            month2Score = 94,
                            midYearScore = 92,
                            finalExamScore = 95
                        )
                    }
                    val overallAvg = if (allSubjectsReport.isEmpty()) 0 else allSubjectsReport.map { it.finalAverage }.average().toInt()
                    val overallRating = when {
                        overallAvg >= 90 -> "ممتاز"
                        overallAvg >= 80 -> "جيد جداً"
                        overallAvg >= 70 -> "جيد"
                        overallAvg >= 60 -> "متوسط"
                        overallAvg >= 50 -> "مقبول"
                        else -> "راسب"
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "كشف درجات الطالب: ${myStu.studentName}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "الصف: ${myStu.gradeClass} • شعبة (${myStu.sectionName})",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                NoWrapBadge(
                                    text = "المعدل العام: $overallAvg%",
                                    icon = Icons.Filled.AutoGraph,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.White
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                NoWrapBadge(
                                    text = "التقدير: $overallRating",
                                    icon = Icons.Filled.WorkspacePremium,
                                    containerColor = GoldAccent,
                                    contentColor = Color(0xFF451A03)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(allSubjectsReport, key = { it.subjectName }) { g ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(g.subjectName, style = MaterialTheme.typography.titleMedium)
                                        }
                                        NoWrapBadge(
                                            text = "المعدل: ${g.finalAverage} • ${g.ratingLabel}",
                                            icon = Icons.Filled.Stars,
                                            containerColor = if (g.finalAverage >= 50) SuccessGreen.copy(alpha = 0.16f) else DangerRed.copy(alpha = 0.16f),
                                            contentColor = if (g.finalAverage >= 50) SuccessGreen else DangerRed
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        ScoreCell("يومي", g.dailyScore, Modifier.weight(1f))
                                        ScoreCell("شهر 1", g.month1Score, Modifier.weight(1f))
                                        ScoreCell("شهر 2", g.month2Score, Modifier.weight(1f))
                                        ScoreCell("نصف السنة", g.midYearScore, Modifier.weight(1f))
                                        ScoreCell("السعي", g.annualEffortScore, Modifier.weight(1f), highlight = true)
                                        ScoreCell("النهائي", g.finalExamScore, Modifier.weight(1f), highlight = true)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // =============================================================
                // STEPS A, B, C: ADMIN & TEACHER GRADE ENTRY BY
                // (1) الصف -> (2) الشعبة -> (3) المادة الدراسية -> (4) البحث بالاسم -> رصد وحفظ
                // =============================================================
                val classSectionStudents = remember(students, selectedClass, selectedSection, searchStudentName, currentUser) {
                    viewModel.getScopedStudentsForUser(currentUser).filter { stu ->
                        stu.gradeClass == selectedClass &&
                            stu.sectionName == selectedSection &&
                            (searchStudentName.isBlank() || stu.studentName.contains(searchStudentName, ignoreCase = true))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 28.dp)
                ) {
                    // Step A & B Filter Card
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
                                Text(
                                    text = "أولاً: اختر الصف والشعبة والمادة الدراسية",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                // 1. Registered Class Selector
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
                                                Icon(Icons.Filled.Class, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        )
                                    }
                                }

                                // 2. Registered Section Selector
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("الشعبة:", style = MaterialTheme.typography.labelLarge)
                                    allowedSections.forEach { sec ->
                                        FilterChip(
                                            selected = selectedSection == sec,
                                            onClick = { selectedSection = sec },
                                            label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                // 3. Subject Selector (المادة الدراسية)
                                Text(
                                    text = "المادة الدراسية المختارة للرصد:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(allowedSubjects) { subj ->
                                        FilterChip(
                                            selected = selectedSubject == subj,
                                            onClick = { selectedSubject = subj },
                                            label = { Text(subj, maxLines = 1, softWrap = false) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (selectedSubject == subj) Icons.Filled.CheckCircle else Icons.Filled.MenuBook,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        )
                                    }
                                }

                                // 4. Search by Student Name within the selected Class & Section
                                OutlinedTextField(
                                    value = searchStudentName,
                                    onValueChange = { searchStudentName = it },
                                    label = { Text("بحث بالاسم داخل طلاب ($selectedClass - شعبة $selectedSection)...") },
                                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                                    trailingIcon = {
                                        if (searchStudentName.isNotEmpty()) {
                                            IconButton(onClick = { searchStudentName = "" }) {
                                                Icon(Icons.Filled.Clear, contentDescription = "مسح")
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grades_student_search_bar"),
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }
                        }
                    }

                    // Step C: Inline Subject Grade Entry Card for each student in the selected Class & Section
                    if (classSectionStudents.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = "لا يوجد طلاب مطابقون في ($selectedClass - شعبة $selectedSection)",
                                    modifier = Modifier.padding(20.dp),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        items(classSectionStudents, key = { "${it.id}_$selectedSubject" }) { stu ->
                            val existingGrade = grades.firstOrNull {
                                it.studentId == stu.id && it.subjectName == selectedSubject
                            }

                            var dailyText by remember(existingGrade, selectedSubject) {
                                mutableStateOf((existingGrade?.dailyScore ?: 92).toString())
                            }
                            var m1Text by remember(existingGrade, selectedSubject) {
                                mutableStateOf((existingGrade?.month1Score ?: 90).toString())
                            }
                            var m2Text by remember(existingGrade, selectedSubject) {
                                mutableStateOf((existingGrade?.month2Score ?: 94).toString())
                            }
                            var midText by remember(existingGrade, selectedSubject) {
                                mutableStateOf((existingGrade?.midYearScore ?: 92).toString())
                            }
                            var finalText by remember(existingGrade, selectedSubject) {
                                mutableStateOf((existingGrade?.finalExamScore ?: 95).toString())
                            }
                            var savedToast by remember { mutableStateOf(false) }

                            val dVal = (dailyText.toIntOrNull() ?: 0).coerceIn(0, 100)
                            val m1Val = (m1Text.toIntOrNull() ?: 0).coerceIn(0, 100)
                            val m2Val = (m2Text.toIntOrNull() ?: 0).coerceIn(0, 100)
                            val midVal = (midText.toIntOrNull() ?: 0).coerceIn(0, 100)
                            val finVal = (finalText.toIntOrNull() ?: 0).coerceIn(0, 100)

                            val previewGrade = SubjectGrade(
                                id = "prev",
                                studentId = stu.id,
                                studentName = stu.studentName,
                                gradeClass = stu.gradeClass,
                                subjectName = selectedSubject,
                                dailyScore = dVal,
                                month1Score = m1Val,
                                month2Score = m2Val,
                                midYearScore = midVal,
                                finalExamScore = finVal
                            )

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
                                                size = 44.dp
                                            )
                                            Column {
                                                Text(
                                                    text = stu.studentName,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "مادة: $selectedSubject • ${stu.gradeClass} (${stu.sectionName})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        NoWrapBadge(
                                            text = "السعي: ${previewGrade.annualEffortScore} | المعدل: ${previewGrade.finalAverage} (${previewGrade.ratingLabel})",
                                            icon = Icons.Filled.Calculate,
                                            containerColor = if (previewGrade.finalAverage >= 50) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                            contentColor = if (previewGrade.finalAverage >= 50) SuccessGreen else DangerRed
                                        )
                                    }

                                    // Inline Grade Input Fields for this Subject
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = dailyText,
                                            onValueChange = {
                                                dailyText = it.filter { c -> c.isDigit() }.take(3)
                                                savedToast = false
                                            },
                                            label = { Text("يومي") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = m1Text,
                                            onValueChange = {
                                                m1Text = it.filter { c -> c.isDigit() }.take(3)
                                                savedToast = false
                                            },
                                            label = { Text("شهر 1") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = m2Text,
                                            onValueChange = {
                                                m2Text = it.filter { c -> c.isDigit() }.take(3)
                                                savedToast = false
                                            },
                                            label = { Text("شهر 2") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = midText,
                                            onValueChange = {
                                                midText = it.filter { c -> c.isDigit() }.take(3)
                                                savedToast = false
                                            },
                                            label = { Text("نصف السنة") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ScoreCell(
                                            label = "السعي السنوي",
                                            score = previewGrade.annualEffortScore,
                                            modifier = Modifier.weight(1f),
                                            highlight = true
                                        )
                                        OutlinedTextField(
                                            value = finalText,
                                            onValueChange = {
                                                finalText = it.filter { c -> c.isDigit() }.take(3)
                                                savedToast = false
                                            },
                                            label = { Text("النهائي") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    VectorActionButton(
                                        text = if (savedToast) "تم حفظ درجات ($selectedSubject) للطالب بنجاح" else "حفظ درجات ($selectedSubject) للطالب",
                                        icon = if (savedToast) Icons.Filled.CheckCircle else Icons.Filled.Save,
                                        containerColor = if (savedToast) SuccessGreen else MaterialTheme.colorScheme.primary,
                                        onClick = {
                                            viewModel.saveOrUpdateSubjectGrade(
                                                student = stu,
                                                subjectName = selectedSubject,
                                                dailyScore = dVal,
                                                month1Score = m1Val,
                                                month2Score = m2Val,
                                                midYearScore = midVal,
                                                finalExamScore = finVal
                                            )
                                            savedToast = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Exam Schedule Tab
            if (canEdit) {
                VectorActionButton(
                    text = "إضافة موعد امتحان جديد للجدول",
                    icon = Icons.Filled.AddCard,
                    onClick = { showAddExamDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val scopedExams = remember(examSchedules, allowedClasses) {
                examSchedules.filter { it.gradeClass in allowedClasses }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(scopedExams, key = { it.id }) { ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                NoWrapBadge(
                                    text = "${ex.subjectName} • ${ex.gradeClass}",
                                    icon = Icons.Filled.School,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                NoWrapBadge(
                                    text = "${ex.dayName} ${ex.examDate}",
                                    icon = Icons.Filled.Event,
                                    containerColor = GoldAccent.copy(alpha = 0.2f),
                                    contentColor = GoldDeep
                                )
                            }
                            Text(
                                text = "${ex.examType} — الوقت: ${ex.timeSlot} — ${ex.hallNumber}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "المادة الداخلة في الامتحان: ${ex.topicsIncluded}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddExamDialog) {
        var cls by remember(allowedClasses) { mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط") }
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var examType by remember { mutableStateOf("امتحان الشهر الثاني") }
        var date by remember { mutableStateOf("2026-11-12") }
        var dayName by remember { mutableStateOf("الخميس") }
        var topics by remember { mutableStateOf("الفصل الثالث والرابع") }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text("إضافة موعد امتحان جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الصف الدراسي:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allowedClasses) { cOpt ->
                            FilterChip(
                                selected = cls == cOpt,
                                onClick = { cls = cOpt },
                                label = { Text(cOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    Text("المادة الدراسية:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allowedSubjects) { sOpt ->
                            FilterChip(
                                selected = subject == sOpt,
                                onClick = { subject = sOpt },
                                label = { Text(sOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(value = examType, onValueChange = { examType = it }, label = { Text("نوع الامتحان") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = dayName, onValueChange = { dayName = it }, label = { Text("اليوم") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = date, onValueChange = { date = it }, label = { Text("التاريخ") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = topics, onValueChange = { topics = it }, label = { Text("المادة الداخلة") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إضافة للجدول",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        viewModel.addExamSchedule(cls, subject, examType, date, dayName, "08:30 صباحاً", topics, "قاعة 1")
                        showAddExamDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showAddExamDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun ScoreCell(
    label: String,
    score: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = if (highlight) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$score",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (highlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}
