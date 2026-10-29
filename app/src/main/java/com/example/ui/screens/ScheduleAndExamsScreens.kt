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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SchoolViewModel

/**
 * Section 3: Weekly Schedule & Daily Preparation / Homework Screen
 * (جدول الدروس والتحضير اليومي والواجبات)
 */
@Composable
fun ScheduleAndHomeworkScreen(
    viewModel: SchoolViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = التحضير اليومي والواجبات, 1 = جدول الحصص الأسبوعي
    var selectedClass by remember { mutableStateOf("الأول المتوسط") }
    var selectedSection by remember { mutableStateOf("أ") }
    var selectedDay by remember { mutableStateOf("الأحد") }

    var showPublishHomeworkDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }

    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val filteredHomeworks = remember(homeworks, selectedClass) {
        homeworks.filter { it.gradeClass == selectedClass }
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
        // Sub-Tabs
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

        Spacer(modifier = Modifier.height(10.dp))

        // Class Filter Row
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(viewModel.availableClasses) { cls ->
                FilterChip(
                    selected = selectedClass == cls,
                    onClick = { selectedClass = cls },
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

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedTab == 0) {
            // Daily Preparation & Homework Tab
            if (canPublish) {
                VectorActionButton(
                    text = "نشر التحضير اليومي والواجب البيتي",
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
                                Text("لا توجد واجبات منشورة لهذا الصف حالياً", style = MaterialTheme.typography.titleMedium)
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
                                        text = "موعد التسليم: ${hw.dueDate}",
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.BookmarkAdded,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "التحضير اليومي:",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = hw.dailyPreparation,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Filled.EditNote,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "الواجب البيتي المطلوب:",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                        }
                                        Text(
                                            text = hw.homeworkDetails,
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
            // Weekly Timetable Tab
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("الشعبة:", style = MaterialTheme.typography.labelLarge)
                viewModel.availableSections.forEach { sec ->
                    FilterChip(
                        selected = selectedSection == sec,
                        onClick = { selectedSection = sec },
                        label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                    )
                }
            }

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

    // Publish Homework Modal
    if (showPublishHomeworkDialog) {
        var subject by remember { mutableStateOf("الرياضيات") }
        var section by remember { mutableStateOf(selectedSection) }
        var prep by remember { mutableStateOf("") }
        var hwDetails by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-10-31") }

        AlertDialog(
            onDismissRequest = { showPublishHomeworkDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PostAdd, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("نشر التحضير والواجب البيتي")
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("اسم المادة الدراسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("الشعبة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                        if (subject.isNotBlank() && prep.isNotBlank()) {
                            viewModel.publishHomework(
                                gradeClass = selectedClass,
                                sectionName = section,
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
                TextButton(onClick = { showPublishHomeworkDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Schedule Period Modal
    if (showAddPeriodDialog) {
        var periodNo by remember { mutableStateOf("5") }
        var subject by remember { mutableStateOf("الكيمياء") }
        var teacher by remember { mutableStateOf(currentUser?.fullName ?: "أ. عمر قاسم الحديثي") }
        var timeSlot by remember { mutableStateOf("11:30 - 12:15") }

        AlertDialog(
            onDismissRequest = { showAddPeriodDialog = false },
            title = { Text("إضافة حصة ليوم $selectedDay") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = periodNo,
                        onValueChange = { periodNo = it.filter { c -> c.isDigit() } },
                        label = { Text("رقم الحصة (1 - 7)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("المادة الدراسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
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
                TextButton(onClick = { showAddPeriodDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Section 4: Exams & Detailed Gradebook Screen
 * (الامتحانات وكشف الدرجات التفصيلي: يومي، الشهر الأول، الشهر الثاني، نصف السنة، السعي السنوي، الامتحان النهائي، المعدل والتقدير التلقائي)
 */
@Composable
fun ExamsAndGradesScreen(
    viewModel: SchoolViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val examSchedules by viewModel.examSchedules.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = كشف الدرجات التفصيلي, 1 = جدول مواعيد الامتحانات
    val visibleStudents = remember(students, currentUser) {
        when (currentUser?.role) {
            UserRole.PARENT, UserRole.STUDENT -> students.filter {
                it.id == currentUser?.linkedStudentId || currentUser?.linkedStudentId.isNullOrBlank()
            }
            else -> students
        }
    }

    var selectedStudentId by remember(visibleStudents) {
        mutableStateOf(visibleStudents.firstOrNull()?.id ?: "")
    }
    val selectedStudent = remember(visibleStudents, selectedStudentId) {
        visibleStudents.firstOrNull { it.id == selectedStudentId } ?: visibleStudents.firstOrNull()
    }

    val studentGrades = remember(grades, selectedStudent) {
        grades.filter { it.studentId == selectedStudent?.id }
    }

    val overallAverage = remember(studentGrades) {
        if (studentGrades.isEmpty()) 0 else studentGrades.map { it.finalAverage }.average().toInt()
    }
    val overallRating = remember(overallAverage) {
        when {
            overallAverage >= 90 -> "ممتاز"
            overallAverage >= 80 -> "جيد جداً"
            overallAverage >= 70 -> "جيد"
            overallAverage >= 60 -> "متوسط"
            overallAverage >= 50 -> "مقبول"
            else -> "راسب"
        }
    }

    var showRecordGradeDialog by remember { mutableStateOf(false) }
    var showAddExamDialog by remember { mutableStateOf(false) }

    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

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
                Text("كشف الدرجات التفصيلي", maxLines = 1, softWrap = false)
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
            // Student Selector Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleStudents, key = { it.id }) { stu ->
                    FilterChip(
                        selected = selectedStudent?.id == stu.id,
                        onClick = { selectedStudentId = stu.id },
                        label = { Text(stu.studentName, maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Automatic Average & Rating Summary Banner
            if (selectedStudent != null) {
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
                                text = "كشف درجات: ${selectedStudent.studentName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "الصف: ${selectedStudent.gradeClass} (شعبة ${selectedStudent.sectionName})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            NoWrapBadge(
                                text = "المعدل العام: $overallAverage%",
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
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (canEdit && selectedStudent != null) {
                VectorActionButton(
                    text = "رصد أو تحديث درجات مادة للطالب",
                    icon = Icons.Filled.Grading,
                    onClick = { showRecordGradeDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "record_grade_button"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(studentGrades, key = { it.id }) { g ->
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
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.MenuBook,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = g.subjectName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                                NoWrapBadge(
                                    text = "المعدل: ${g.finalAverage} • ${g.ratingLabel}",
                                    icon = Icons.Filled.Stars,
                                    containerColor = if (g.finalAverage >= 50) SuccessGreen.copy(alpha = 0.16f) else DangerRed.copy(alpha = 0.16f),
                                    contentColor = if (g.finalAverage >= 50) SuccessGreen else DangerRed
                                )
                            }

                            // 6 Detailed Score Columns (يومي، الشهر الأول، الشهر الثاني، نصف السنة، السعي السنوي، الامتحان النهائي)
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

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(examSchedules, key = { it.id }) { ex ->
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

    // Record Grade Dialog
    if (showRecordGradeDialog && selectedStudent != null) {
        var subject by remember { mutableStateOf("الرياضيات") }
        var daily by remember { mutableStateOf("95") }
        var m1 by remember { mutableStateOf("92") }
        var m2 by remember { mutableStateOf("96") }
        var mid by remember { mutableStateOf("94") }
        var finalScore by remember { mutableStateOf("98") }

        AlertDialog(
            onDismissRequest = { showRecordGradeDialog = false },
            title = { Text("رصد درجات: ${selectedStudent.studentName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("اسم المادة الدراسية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = daily,
                            onValueChange = { daily = it.filter { c -> c.isDigit() } },
                            label = { Text("يومي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = m1,
                            onValueChange = { m1 = it.filter { c -> c.isDigit() } },
                            label = { Text("شهر أول") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = m2,
                            onValueChange = { m2 = it.filter { c -> c.isDigit() } },
                            label = { Text("شهر ثاني") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = mid,
                            onValueChange = { mid = it.filter { c -> c.isDigit() } },
                            label = { Text("نصف السنة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = finalScore,
                            onValueChange = { finalScore = it.filter { c -> c.isDigit() } },
                            label = { Text("الامتحان النهائي") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ وحساب السعي والمعدل تلقائياً",
                    icon = Icons.Filled.Calculate,
                    onClick = {
                        viewModel.saveOrUpdateSubjectGrade(
                            student = selectedStudent,
                            subjectName = subject,
                            dailyScore = daily.toIntOrNull() ?: 90,
                            month1Score = m1.toIntOrNull() ?: 90,
                            month2Score = m2.toIntOrNull() ?: 90,
                            midYearScore = mid.toIntOrNull() ?: 90,
                            finalExamScore = finalScore.toIntOrNull() ?: 90
                        )
                        showRecordGradeDialog = false
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRecordGradeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Add Exam Schedule Dialog
    if (showAddExamDialog) {
        var cls by remember { mutableStateOf("الأول المتوسط") }
        var subject by remember { mutableStateOf("العلوم العامة") }
        var examType by remember { mutableStateOf("امتحان الشهر الثاني") }
        var date by remember { mutableStateOf("2026-11-12") }
        var dayName by remember { mutableStateOf("الخميس") }
        var topics by remember { mutableStateOf("الفصل الثالث والرابع") }

        AlertDialog(
            onDismissRequest = { showAddExamDialog = false },
            title = { Text("إضافة موعد امتحان جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = cls, onValueChange = { cls = it }, label = { Text("الصف") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("المادة") }, singleLine = true, modifier = Modifier.fillMaxWidth())
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
                overflow = TextOverflow.Ellipsis
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
