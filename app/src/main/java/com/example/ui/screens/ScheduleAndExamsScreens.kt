package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DocumentAndIdExporter
import com.example.util.ImageCompressor
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
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val students by viewModel.students.collectAsState()
    val schedules by viewModel.schedules.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()
    val homeworkSubmissions by viewModel.homeworkSubmissions.collectAsState()
    val quickQuizzes by viewModel.quickQuizzes.collectAsState()
    val quickQuizAnswers by viewModel.quickQuizAnswers.collectAsState()
    val studyMaterials by viewModel.studyMaterials.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val staffTitleSingular = schoolConfig.schoolStage.staffTitleSingular

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER
    val activeStudent = remember(currentUser, students, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
    }

    val allowedClasses = remember(schoolConfig, currentUser, students, selectedParentChildId) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(currentUser?.id, allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, students, selectedClass, selectedParentChildId) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(currentUser?.id, selectedClass, allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    val allowedSubjects = remember(schoolConfig, selectedClass, currentUser) {
        viewModel.getSubjectsForClass(selectedClass, currentUser)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = التحضير والواجبات, 1 = جدول الحصص الأسبوعي, 2 = المكتبة والملخصات
    var selectedDay by remember { mutableStateOf("الأحد") }

    var showPublishHomeworkDialog by remember { mutableStateOf(false) }
    var editingHomework by remember { mutableStateOf<HomeworkItem?>(null) }
    var deletingHomework by remember { mutableStateOf<HomeworkItem?>(null) }
    var showPublishQuickQuizDialog by remember { mutableStateOf(false) }
    var editingQuiz by remember { mutableStateOf<QuickQuizItem?>(null) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var showPublishStudyMaterialDialog by remember { mutableStateOf(false) }

    val deepLinkTarget by viewModel.deepLinkTarget.collectAsState()
    LaunchedEffect(deepLinkTarget) {
        val target = deepLinkTarget
        if (target != null && target.section == AppSection.SCHEDULE_HOMEWORK) {
            selectedTab = target.tabIndex.coerceIn(0, 2)
            viewModel.clearDeepLinkTarget()
        }
    }

    LaunchedEffect(featureSettings.studyLibraryEnabled, featureSettings.quickQuizzesEnabled) {
        if (!featureSettings.studyLibraryEnabled) {
            if (selectedTab == 2) selectedTab = 0
            showPublishStudyMaterialDialog = false
        }
        if (!featureSettings.quickQuizzesEnabled) {
            showPublishQuickQuizDialog = false
        }
    }

    val filteredHomeworks = remember(homeworks, selectedClass, selectedSection, currentUser) {
        homeworks.filter { hw ->
            val clsMatch = ArabicTextNormalizer.matchesClass(hw.gradeClass, selectedClass) &&
                ArabicTextNormalizer.matchesSection(hw.sectionName, selectedSection)
            val subjMatch = if (currentUser?.role == UserRole.TEACHER) {
                currentUser?.canTeacherAccessSubject(hw.subjectName) == true
            } else true
            clsMatch && subjMatch
        }.sortedByDescending { it.createdAt.ifBlank { it.dueDate } }
    }
    var selectedHomeworkDateFilter by remember(selectedClass, selectedSection) {
        mutableStateOf("الكل")
    }
    val homeworkDatesIndex = remember(filteredHomeworks) {
        filteredHomeworks
            .groupBy { hw -> hw.createdAt.trim().ifBlank { hw.dueDate.trim() } }
            .entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.key }
            .map { it.key to it.value.size }
    }
    val dateScopedHomeworks = remember(filteredHomeworks, selectedHomeworkDateFilter) {
        if (selectedHomeworkDateFilter == "الكل") {
            filteredHomeworks
        } else {
            filteredHomeworks.filter { hw ->
                val dateKey = hw.createdAt.trim().ifBlank { hw.dueDate.trim() }
                dateKey == selectedHomeworkDateFilter
            }
        }
    }
    var homeworksVisibleCount by remember(selectedClass, selectedSection, selectedHomeworkDateFilter, dateScopedHomeworks.size) {
        mutableStateOf(8)
    }
    val displayedHomeworks = remember(dateScopedHomeworks, homeworksVisibleCount) {
        dateScopedHomeworks.take(homeworksVisibleCount)
    }

    val classScopedQuickQuizzes = remember(quickQuizzes, selectedClass, selectedSection, currentUser) {
        quickQuizzes.filter { q ->
            val clsMatch = ArabicTextNormalizer.matchesClass(q.gradeClass, selectedClass) &&
                (q.sectionName == "الكل" || ArabicTextNormalizer.matchesSection(q.sectionName, selectedSection))
            val subjMatch = if (currentUser?.role == UserRole.TEACHER) {
                currentUser?.canTeacherAccessSubject(q.subjectName) == true
            } else true
            clsMatch && subjMatch
        }
    }

    var quizStatusFilter by remember { mutableIntStateOf(0) } // 0 = الاختبارات النشطة (الجارية), 1 = الاختبارات المنتهية (الأرشيف), 2 = الكل
    var selectedQuizSubject by remember { mutableStateOf("الكل") }
    var selectedQuizDateFilter by remember { mutableStateOf("الكل") }
    var quizSortDescending by remember { mutableStateOf(true) }
    var inspectingQuizParticipants by remember { mutableStateOf<QuickQuizItem?>(null) }

    val availableQuizSubjects = remember(classScopedQuickQuizzes, allowedSubjects) {
        (listOf("الكل") + classScopedQuickQuizzes.map { it.subjectName.trim() } + allowedSubjects.map { it.trim() })
            .filter { it.isNotEmpty() }
            .distinct()
    }
    val availableQuizDates = remember(classScopedQuickQuizzes) {
        listOf("الكل") + classScopedQuickQuizzes.map { it.createdAt.trim() }.filter { it.isNotEmpty() }.distinct().sortedDescending()
    }

    val activeQuizzesCount = remember(classScopedQuickQuizzes) {
        classScopedQuickQuizzes.count { !it.isClosed }
    }
    val closedQuizzesCount = remember(classScopedQuickQuizzes) {
        classScopedQuickQuizzes.count { it.isClosed }
    }

    val filteredQuickQuizzes = remember(
        classScopedQuickQuizzes,
        quizStatusFilter,
        selectedQuizSubject,
        selectedQuizDateFilter,
        quizSortDescending
    ) {
        val filtered = classScopedQuickQuizzes.filter { q ->
            val statusMatches = when (quizStatusFilter) {
                0 -> !q.isClosed
                1 -> q.isClosed
                else -> true
            }
            val subjectMatches = selectedQuizSubject == "الكل" ||
                ArabicTextNormalizer.normalizeText(q.subjectName) == ArabicTextNormalizer.normalizeText(selectedQuizSubject)
            val dateMatches = selectedQuizDateFilter == "الكل" || q.createdAt.trim() == selectedQuizDateFilter
            statusMatches && subjectMatches && dateMatches
        }
        if (quizSortDescending) {
            filtered.sortedByDescending { it.createdAt }
        } else {
            filtered.sortedBy { it.createdAt }
        }
    }

    val classSectionStudents = remember(students, selectedClass, selectedSection, schoolConfig.schoolId) {
        students.filter { stu ->
            (stu.schoolId.isEmpty() || stu.schoolId == schoolConfig.schoolId) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection)
        }
    }
    var selectedSummaryStudentId by remember(activeStudent?.id, classSectionStudents) {
        mutableStateOf(activeStudent?.id ?: classSectionStudents.firstOrNull()?.id.orEmpty())
    }
    val summaryTargetStudent = remember(activeStudent, classSectionStudents, selectedSummaryStudentId, isParentOrStudent) {
        if (isParentOrStudent) activeStudent
        else classSectionStudents.firstOrNull { it.id == selectedSummaryStudentId } ?: classSectionStudents.firstOrNull()
    }

    val filteredPeriods = remember(schedules, selectedClass, selectedSection, selectedDay) {
        schedules.filter {
            ArabicTextNormalizer.matchesClass(it.gradeClass, selectedClass) &&
                ArabicTextNormalizer.matchesSection(it.sectionName, selectedSection) &&
                it.dayOfWeek == selectedDay
        }.sortedBy { it.periodNumber }
    }

    val filteredStudyMaterials = remember(studyMaterials, selectedClass, selectedSection) {
        studyMaterials.filter { mat ->
            ArabicTextNormalizer.matchesClass(mat.gradeClass, selectedClass) &&
                (mat.sectionName == "الكل" || ArabicTextNormalizer.matchesSection(mat.sectionName, selectedSection))
        }
    }

    // Single unified LazyColumn so the 3D Hero Card, 3-Tab Switcher, Class/Section Filters, and Action Buttons scroll smoothly with the content
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp)
    ) {
        item {
            // 3D Wave Hero Header for Schedule, Homework & Study Library
            ThreeDHeroHeaderCard(
                title = "الجدول والواجبات والمكتبة",
                subtitle = "تنظيم جدول الحصص الأسبوعي والواجبات اليومية والملخصات الدراسية",
                rightPillText = "$selectedClass • شعبة $selectedSection",
                rightPillIcon = Icons.Filled.MenuBook,
                leftPillText = "الواجبات: ${filteredHomeworks.size}",
                leftPillIcon = Icons.Filled.CalendarMonth,
                illustrationType = ThreeDIllustrationType.SCHEDULE_BOOKS_3D
            )
        }

        item {
            // Compact 3D Wave Header & Unclipped 3-Tab Pill Switcher (الواجبات | جدول الحصص | المكتبة والملخصات)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isHwTab = selectedTab == 0
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isHwTab) Color(0xFF059669) else Color.White,
                    border = BorderStroke(1.2.dp, if (isHwTab) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                    shadowElevation = if (isHwTab) 5.dp else 2.dp,
                    modifier = Modifier
                        .weight(0.85f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedTab = 0 }
                        .testTag("tab_daily_homework")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = if (isHwTab) Color.White else Color(0xFF047857),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "الواجبات",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isHwTab) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val isSchedTab = selectedTab == 1
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSchedTab) Color(0xFF059669) else Color.White,
                    border = BorderStroke(1.2.dp, if (isSchedTab) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                    shadowElevation = if (isSchedTab) 5.dp else 2.dp,
                    modifier = Modifier
                        .weight(1.0f)
                        .heightIn(min = 44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { selectedTab = 1 }
                        .testTag("tab_weekly_schedule")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = null,
                            tint = if (isSchedTab) Color.White else Color(0xFF047857),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "جدول الحصص",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isSchedTab) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                if (featureSettings.studyLibraryEnabled) {
                    val isLibTab = selectedTab == 2
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isLibTab) Color(0xFF059669) else Color.White,
                        border = BorderStroke(1.2.dp, if (isLibTab) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                        shadowElevation = if (isLibTab) 5.dp else 2.dp,
                        modifier = Modifier
                            .weight(1.2f)
                            .heightIn(min = 44.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { selectedTab = 2 }
                            .testTag("tab_study_materials")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocalLibrary,
                                contentDescription = null,
                                tint = if (isLibTab) Color.White else Color(0xFF047857),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = "المكتبة والملخصات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLibTab) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        item {
            if (!isParentOrStudent) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
        }

        if (selectedTab == 0) {
            if (canPublish) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VectorActionButton(
                            text = "نشر تحضير وواجب",
                            icon = Icons.Filled.PostAdd,
                            onClick = { showPublishHomeworkDialog = true },
                            modifier = Modifier.weight(1f),
                            testTag = "publish_homework_button"
                        )
                        if (featureSettings.quickQuizzesEnabled) {
                            VectorActionButton(
                                text = "نشر اختبار سريع",
                                icon = Icons.Filled.Quiz,
                                containerColor = DeepEmerald,
                                onClick = { showPublishQuickQuizDialog = true },
                                modifier = Modifier.weight(1f),
                                testTag = "publish_quick_quiz_button"
                            )
                        }
                    }
                }
            }
                // Quick Quizzes Section (الاختبارات السريعة والتصحيح التلقائي والفرز الاحترافي)
                if (featureSettings.quickQuizzesEnabled && classScopedQuickQuizzes.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                            imageVector = Icons.Filled.Quiz,
                                            contentDescription = null,
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "مركز الاختبارات السريعة والأرشيف (${classScopedQuickQuizzes.size})",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen
                                        )
                                    }
                                    Surface(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { quizSortDescending = !quizSortDescending },
                                        shape = RoundedCornerShape(10.dp),
                                        color = DeepEmerald.copy(alpha = 0.10f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.FilterList,
                                                contentDescription = null,
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = if (quizSortDescending) "الأحدث أولاً" else "الأقدم أولاً",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepEmerald
                                            )
                                        }
                                    }
                                }

                                // 1. تبويبات الفرز حسب الحالة: النشطة (الجارية) / المنتهية (الأرشيف) / الكل
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Button(
                                        onClick = { quizStatusFilter = 0 },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .testTag("quiz_tab_active"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (quizStatusFilter == 0) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (quizStatusFilter == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(Icons.Filled.PlayCircle, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "الاختبارات النشطة ($activeQuizzesCount)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                    }

                                    Button(
                                        onClick = { quizStatusFilter = 1 },
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 40.dp)
                                            .testTag("quiz_tab_archived"),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (quizStatusFilter == 1) DeepForestGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (quizStatusFilter == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    ) {
                                        Icon(Icons.Filled.Inventory2, contentDescription = null, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "المنتهية والأرشيف ($closedQuizzesCount)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            maxLines = 1
                                        )
                                    }
                                }

                                // 2. الفرز والعزل حسب المادة الدراسية
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "فرز وعزل حسب المادة الدراسية:",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMutedGreen
                                    )
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(availableQuizSubjects) { subj ->
                                            FilterChip(
                                                selected = selectedQuizSubject == subj,
                                                onClick = { selectedQuizSubject = subj },
                                                label = { Text(subj, maxLines = 1) },
                                                leadingIcon = if (selectedQuizSubject == subj) {
                                                    {
                                                        Icon(
                                                            imageVector = Icons.Filled.Check,
                                                            contentDescription = null,
                                                            modifier = Modifier.size(15.dp)
                                                        )
                                                    }
                                                } else null
                                            )
                                        }
                                    }
                                }

                                // الفرز حسب التاريخ
                                if (availableQuizDates.size > 2) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Text(
                                            text = "فلترة حسب تاريخ الاختبار:",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = TextMutedGreen
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            items(availableQuizDates) { dt ->
                                                FilterChip(
                                                    selected = selectedQuizDateFilter == dt,
                                                    onClick = { selectedQuizDateFilter = dt },
                                                    label = { Text(if (dt == "الكل") "كل التواريخ" else dt, maxLines = 1) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // 3. ملخص درجات الطالب حسب المادة والتاريخ (عند اختيار مادة معينة أو للطالب المحدد)
                                if (selectedQuizSubject != "الكل" && summaryTargetStudent != null) {
                                    val subjectQuizzes = classScopedQuickQuizzes
                                        .filter { ArabicTextNormalizer.normalizeText(it.subjectName) == ArabicTextNormalizer.normalizeText(selectedQuizSubject) }
                                        .sortedByDescending { it.createdAt }

                                    if (subjectQuizzes.isNotEmpty()) {
                                        val totalPossible = subjectQuizzes.sumOf { it.points }
                                        val totalEarned = subjectQuizzes.sumOf { q ->
                                            val ans = q.studentAnswers[summaryTargetStudent.id]
                                            if (ans != null && ans == q.correctOptionIndex) q.points else 0
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.45f)),
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
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = "ملخص درجات الاختبارات السريعة • مادة $selectedQuizSubject",
                                                            style = MaterialTheme.typography.labelLarge,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = DeepForestGreen
                                                        )
                                                        Text(
                                                            text = "الطالب: ${summaryTargetStudent.fullNameWithFather} (${subjectQuizzes.size} اختبار)",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = TextMutedGreen
                                                        )
                                                    }
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = DeepEmerald
                                                    ) {
                                                        Text(
                                                            text = "المجموع: $totalEarned من $totalPossible",
                                                            style = MaterialTheme.typography.labelMedium,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color.White,
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                                        )
                                                    }
                                                }

                                                if (!isParentOrStudent && classSectionStudents.size > 1) {
                                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                        items(classSectionStudents, key = { it.id }) { stu ->
                                                            FilterChip(
                                                                selected = stu.id == summaryTargetStudent.id,
                                                                onClick = { selectedSummaryStudentId = stu.id },
                                                                label = { Text(stu.studentName, maxLines = 1) }
                                                            )
                                                        }
                                                    }
                                                }

                                                subjectQuizzes.forEach { sq ->
                                                    val ansIdx = sq.studentAnswers[summaryTargetStudent.id]
                                                    val participated = ansIdx != null
                                                    val isCorrect = ansIdx == sq.correctOptionIndex
                                                    val earned = if (participated && isCorrect) sq.points else 0
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = Color.White,
                                                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    text = sq.questionText,
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = DeepForestGreen,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis
                                                                )
                                                                Text(
                                                                    text = "التاريخ: ${sq.createdAt} • الحالة: ${if (sq.isClosed) "منتهي" else "نشط"}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = TextMutedGreen
                                                                )
                                                            }
                                                            Surface(
                                                                shape = RoundedCornerShape(8.dp),
                                                                color = when {
                                                                    !participated -> WarningAmber.copy(alpha = 0.14f)
                                                                    isCorrect -> SuccessGreen.copy(alpha = 0.15f)
                                                                    else -> DangerRed.copy(alpha = 0.15f)
                                                                }
                                                            ) {
                                                                Text(
                                                                    text = if (!participated) {
                                                                        "لم يشارك (0 من ${sq.points})"
                                                                    } else {
                                                                        "$earned من ${sq.points}"
                                                                    },
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = when {
                                                                        !participated -> WarningAmber
                                                                        isCorrect -> SuccessGreen
                                                                        else -> DangerRed
                                                                    },
                                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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

                    if (filteredQuickQuizzes.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.2f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (quizStatusFilter == 0) {
                                        "لا توجد اختبارات نشطة مطابقة للفلتر المحدد حالياً. يمكنك تصفح تبويب (المنتهية والأرشيف)."
                                    } else {
                                        "لا توجد اختبارات منتهية في الأرشيف مطابقة للفلتر المحدد."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMutedGreen,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    items(filteredQuickQuizzes, key = { "quiz_${it.id}" }) { quiz ->
                        val quizAnsForStudent = remember(quickQuizAnswers, quiz.id, activeStudent?.id, quiz.studentAnswers) {
                            quickQuizAnswers.firstOrNull { it.quizId == quiz.id && it.studentId == activeStudent?.id }
                        }
                        val allQuizAnswers = remember(quickQuizAnswers, quiz.id, quiz.studentAnswers) {
                            quickQuizAnswers.filter { it.quizId == quiz.id }
                        }
                        var selectedOptIndex by remember(quiz.id) { mutableIntStateOf(-1) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (quiz.isClosed) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color(0xFFECFDF5)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (quiz.isClosed) MaterialTheme.colorScheme.outlineVariant else EmeraldAccent.copy(alpha = 0.45f)
                            )
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
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NoWrapBadge(
                                            text = "اختبار سريع • ${quiz.subjectName}",
                                            icon = Icons.Filled.Quiz,
                                            containerColor = DeepEmerald.copy(alpha = 0.14f),
                                            contentColor = DeepEmerald
                                        )
                                        NoWrapBadge(
                                            text = if (quiz.isClosed) "منتهي (مؤرشف)" else "نشط (جاري)",
                                            icon = if (quiz.isClosed) Icons.Filled.Lock else Icons.Filled.PlayCircle,
                                            containerColor = if (quiz.isClosed) DangerRed.copy(alpha = 0.14f) else SuccessGreen.copy(alpha = 0.16f),
                                            contentColor = if (quiz.isClosed) DangerRed else SuccessGreen
                                        )
                                    }
                                    NoWrapBadge(
                                        text = "الدرجة: ${quiz.points}",
                                        icon = Icons.Filled.Star,
                                        containerColor = DeepEmerald.copy(alpha = 0.14f),
                                        contentColor = DeepEmerald
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "الأستاذ: ${quiz.teacherName} • الصف: ${quiz.gradeClass} (${quiz.sectionName})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextMutedGreen
                                    )
                                    Text(
                                        text = "التاريخ: ${quiz.createdAt}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestGreen
                                    )
                                }

                                Text(
                                    text = quiz.questionText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                quiz.options.forEachIndexed { idx, optText ->
                                    val isAnswered = quizAnsForStudent != null
                                    val revealCorrect = isAnswered || quiz.isClosed || !isParentOrStudent
                                    val isCorrectOption = idx == quiz.correctOptionIndex
                                    val isChosenOption = (quizAnsForStudent?.selectedOptionIndex ?: selectedOptIndex) == idx

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(enabled = isParentOrStudent && !isAnswered && !quiz.isClosed) {
                                                selectedOptIndex = idx
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            revealCorrect && isCorrectOption -> SuccessGreen.copy(alpha = 0.18f)
                                            isAnswered && isChosenOption && !isCorrectOption -> DangerRed.copy(alpha = 0.18f)
                                            isChosenOption -> DeepEmerald.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            when {
                                                revealCorrect && isCorrectOption -> SuccessGreen
                                                isAnswered && isChosenOption && !isCorrectOption -> DangerRed
                                                isChosenOption -> DeepEmerald
                                                else -> MaterialTheme.colorScheme.outlineVariant
                                            }
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${idx + 1}. $optText",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = if (isChosenOption || (revealCorrect && isCorrectOption)) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (revealCorrect && isCorrectOption) {
                                                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                            } else if (isAnswered && isChosenOption && !isCorrectOption) {
                                                Icon(Icons.Filled.Cancel, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                            }
                                        }
                                    }
                                }

                                if (isParentOrStudent) {
                                    if (quizAnsForStudent != null) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = if (quizAnsForStudent.isCorrect) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (quizAnsForStudent.isCorrect) {
                                                    "إجابة صحيحة ممتازة! احتُسبت لك الدرجة: (${quizAnsForStudent.earnedPoints} من ${quiz.points})"
                                                } else {
                                                    "إجابة خاطئة (احتُسبت لك: 0 من ${quiz.points}) • الإجابة الصحيحة هي: ${quiz.options.getOrNull(quiz.correctOptionIndex) ?: ""}"
                                                },
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (quizAnsForStudent.isCorrect) DeepEmerald else DangerRed
                                            )
                                        }
                                    } else if (quiz.isClosed) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = WarningAmber.copy(alpha = 0.14f),
                                            border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.45f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Filled.Lock, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                                                Text(
                                                    text = "هذا الاختبار منتهي ومغلق من قِبل الأستاذ • لم يتم تسجيل مشاركة سابقة (الدرجة: 0 من ${quiz.points}) • الإجابة الصحيحة: ${quiz.options.getOrNull(quiz.correctOptionIndex) ?: ""}",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepForestGreen
                                                )
                                            }
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                if (selectedOptIndex >= 0) {
                                                    viewModel.submitQuickQuizAnswer(quiz, selectedOptIndex)
                                                }
                                            },
                                            enabled = selectedOptIndex >= 0,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("submit_quick_quiz_btn_${quiz.id}"),
                                            shape = RoundedCornerShape(10.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
                                        ) {
                                            Icon(Icons.Filled.DoneAll, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تأكيد الإجابة والتصحيح التلقائي الفوري", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else if (canPublish) {
                                    val targetQuizStudents = remember(students, quiz.gradeClass, quiz.sectionName, schoolConfig.schoolId) {
                                        students.filter { stu ->
                                            (stu.schoolId.isEmpty() || stu.schoolId == schoolConfig.schoolId) &&
                                                ArabicTextNormalizer.matchesClass(stu.gradeClass, quiz.gradeClass) &&
                                                (quiz.sectionName == "الكل" || ArabicTextNormalizer.matchesSection(stu.sectionName, quiz.sectionName))
                                        }
                                    }
                                    val participantsCount = quiz.studentAnswers.size
                                    val nonParticipantsCount = (targetQuizStudents.size - participantsCount).coerceAtLeast(0)
                                    val correctCount = allQuizAnswers.count { it.isCorrect }

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { inspectingQuizParticipants = quiz }
                                            .testTag("open_quiz_participants_btn_${quiz.id}"),
                                        shape = RoundedCornerShape(12.dp),
                                        color = DeepEmerald.copy(alpha = 0.12f),
                                        border = BorderStroke(1.2.dp, DeepEmerald.copy(alpha = 0.45f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Groups,
                                                        contentDescription = null,
                                                        tint = DeepEmerald,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Text(
                                                        text = "كشف المشاركين وإجابات الطلبة التفصيلية",
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen
                                                    )
                                                }
                                                Text(
                                                    text = "المشاركون: $participantsCount طالب (صحيحة: $correctCount) • غير المشاركين: $nonParticipantsCount طالب",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = DeepEmerald
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Filled.Visibility,
                                                contentDescription = "عرض الكشف التفصيلي",
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }

                                    // زر تحكم الأستاذ بحالة الاختبار (إنهاء الاختبار وإغلاق الإجابات / إعادة تفعيل الاختبار)
                                    Button(
                                        onClick = { viewModel.toggleQuickQuizClosedStatus(quiz.id) },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 40.dp)
                                            .testTag("toggle_quiz_status_btn_${quiz.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (quiz.isClosed) DeepEmerald else WarningAmber,
                                            contentColor = Color.White
                                        )
                                    ) {
                                        Icon(
                                            imageVector = if (quiz.isClosed) Icons.Filled.PlayCircle else Icons.Filled.Lock,
                                            contentDescription = null,
                                            modifier = Modifier.size(17.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (quiz.isClosed) {
                                                "إعادة تفعيل الاختبار وفتح استقبال الإجابات"
                                            } else {
                                                "إنهاء الاختبار وإغلاق الإجابات"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingQuiz = quiz },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("edit_quick_quiz_${quiz.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "تعديل الاختبار",
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "تعديل الاختبار",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = DeepEmerald
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.deleteQuickQuiz(quiz.id) },
                                            shape = RoundedCornerShape(10.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("delete_quick_quiz_${quiz.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = "حذف الاختبار",
                                                tint = DangerRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "حذف الاختبار",
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

                // فهرس الواجبات الذكي حسب التاريخ مع شارة تنبيه نابضة بالواجبات الجديدة (Interactive 3D Homework Date Index)
                if (filteredHomeworks.isNotEmpty()) {
                    item(key = "homework_smart_date_index_bar") {
                        val pulseTransition = rememberInfiniteTransition(label = "hw_date_badge_pulse")
                        val badgePulseScale by pulseTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "hw_badge_scale"
                        )
                        val badgePulseAlpha by pulseTransition.animateFloat(
                            initialValue = 0.72f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "hw_badge_alpha"
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.28f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            imageVector = Icons.Filled.EventNote,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = "فهرس الواجبات الذكي حسب التاريخ",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = "إجمالي الواجبات: ${filteredHomeworks.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857),
                                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                        )
                                    }
                                }

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    item(key = "hw_date_all") {
                                        val isAllSelected = selectedHomeworkDateFilter == "الكل"
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isAllSelected) Color(0xFF059669) else Color(0xFFF8FAFC),
                                            border = BorderStroke(
                                                1.2.dp,
                                                if (isAllSelected) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)
                                            ),
                                            shadowElevation = if (isAllSelected) 4.dp else 1.dp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedHomeworkDateFilter = "الكل" }
                                                .testTag("hw_date_filter_all")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.CalendarMonth,
                                                    contentDescription = null,
                                                    tint = if (isAllSelected) Color.White else Color(0xFF059669),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Text(
                                                    text = "كل التواريخ (${filteredHomeworks.size})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isAllSelected) Color.White else Color(0xFF0F172A),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    items(homeworkDatesIndex, key = { "hw_date_${it.first}" }) { (dateKey, count) ->
                                        val isDateSelected = selectedHomeworkDateFilter == dateKey
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isDateSelected) Color(0xFF059669) else Color.White,
                                            border = BorderStroke(
                                                1.2.dp,
                                                if (isDateSelected) Color(0xFFFEF08A) else Color(0xFF10B981).copy(alpha = 0.45f)
                                            ),
                                            shadowElevation = if (isDateSelected) 5.dp else 2.dp,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .clickable { selectedHomeworkDateFilter = dateKey }
                                                .testTag("hw_date_filter_$dateKey")
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                // شارة إشعار نابضة وملفتة بلطف تدل على وجود واجبات في هذا التاريخ
                                                Box(
                                                    modifier = Modifier
                                                        .size(9.dp)
                                                        .graphicsLayer {
                                                            scaleX = badgePulseScale
                                                            scaleY = badgePulseScale
                                                            alpha = badgePulseAlpha
                                                        }
                                                        .clip(CircleShape)
                                                        .background(
                                                            if (isDateSelected) Color(0xFFFEF08A) else Color(0xFFF59E0B)
                                                        )
                                                )
                                                Text(
                                                    text = dateKey,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isDateSelected) Color.White else DeepForestGreen,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = if (isDateSelected) {
                                                        Color(0xFFFEF08A).copy(alpha = 0.24f)
                                                    } else {
                                                        Color(0xFFECFDF5)
                                                    }
                                                ) {
                                                    Text(
                                                        text = "($count واجب جديد)",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDateSelected) Color(0xFFFEF08A) else Color(0xFF047857),
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
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

                if (dateScopedHomeworks.isEmpty()) {
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
                    items(displayedHomeworks, key = { it.id }) { hw ->
                        val hwSubs = remember(homeworkSubmissions, hw.id) {
                            homeworkSubmissions.filter { it.homeworkId == hw.id }
                        }
                        val mySubmission = remember(hwSubs, activeStudent?.id) {
                            hwSubs.firstOrNull { it.studentId == activeStudent?.id }
                        }
                        var pendingPhotoBase64 by remember(hw.id) { mutableStateOf("") }
                        var pendingStudentNote by remember(hw.id) { mutableStateOf("") }
                        val hwAttachmentBitmap = remember(hw.attachmentImageBase64) {
                            ImageCompressor.decodeBase64ToImageBitmap(hw.attachmentImageBase64)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // شريط فهرس التاريخ أعلى بطاقة الواجب (Date Index Header Strip)
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.30f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.CalendarToday,
                                                contentDescription = null,
                                                tint = Color(0xFF059669),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "تاريخ النشر: ${hw.createdAt.ifBlank { hw.dueDate }}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                        }
                                        Text(
                                            text = "موعد التسليم: ${hw.dueDate}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }

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
                                    text = "الصف: ${hw.gradeClass} (شعبة ${hw.sectionName}) • $staffTitleSingular: ${hw.teacherName}",
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

                                // عرض الصورة التوضيحية المرفقة مع الواجب من قِبل الأستاذ (إن وُجدت) مع إمكانية التكبير
                                if (hwAttachmentBitmap != null) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF0FDF4),
                                        border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.40f)),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                GlobalImageLightboxController.open(
                                                    hw.attachmentImageBase64,
                                                    "صورة توضيحية للواجب • مادة ${hw.subjectName}"
                                                )
                                            }
                                            .testTag("hw_attachment_preview_${hw.id}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                                        imageVector = Icons.Filled.Image,
                                                        contentDescription = null,
                                                        tint = Color(0xFF059669),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Text(
                                                        text = "صورة الواجب المرفقة",
                                                        style = MaterialTheme.typography.labelMedium,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen
                                                    )
                                                }
                                                Text(
                                                    text = "اضغط للتكبير",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF059669)
                                                )
                                            }
                                            Image(
                                                bitmap = hwAttachmentBitmap,
                                                contentDescription = "صورة توضيحية للواجب",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(175.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                }

                                // Photo Homework Submission & Teacher Review (تسليم الواجب بالصورة وتصحيح المعلم)
                                if (featureSettings.homeworkSubmissionEnabled) {
                                    if (isParentOrStudent) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = DeepEmerald.copy(alpha = 0.06f),
                                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.16f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (mySubmission != null) {
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
                                                                base64Data = mySubmission.solutionImageBase64,
                                                                size = 44.dp,
                                                                fallbackIcon = Icons.Filled.CameraAlt,
                                                                contentDescription = "صورة حل الواجب"
                                                            )
                                                            Column(
                                                                modifier = Modifier.weight(1f),
                                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(50),
                                                                        color = if (mySubmission.isReviewed) {
                                                                            SuccessGreen.copy(alpha = 0.14f)
                                                                        } else {
                                                                            DeepEmerald.copy(alpha = 0.12f)
                                                                        }
                                                                    ) {
                                                                        Row(
                                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                                        ) {
                                                            Icon(
                                                                imageVector = if (mySubmission.isReviewed) Icons.Filled.CheckCircle else Icons.Filled.TaskAlt,
                                                                contentDescription = null,
                                                                tint = if (mySubmission.isReviewed) SuccessGreen else DeepEmerald,
                                                                modifier = Modifier.size(13.dp)
                                                            )
                                                            Text(
                                                                text = if (mySubmission.isReviewed) "تم تسليم الواجب ومراجعته" else "تم تسليم الواجب",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (mySubmission.isReviewed) SuccessGreen else DeepEmerald,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    }

                                                    if (mySubmission.homeworkScore.isNotBlank()) {
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = Color(0xFFECFDF5),
                                                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f))
                                                        ) {
                                                            Row(
                                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Filled.WorkspacePremium,
                                                                    contentDescription = null,
                                                                    tint = DeepEmerald,
                                                                    modifier = Modifier.size(13.dp)
                                                                )
                                                                Text(
                                                                    text = "درجة الواجب: ${mySubmission.homeworkScore}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = DeepForestGreen,
                                                                    maxLines = 1,
                                                                    softWrap = false
                                                                )
                                                            }
                                                        }
                                                    }
                                                }

                                                Text(
                                                    text = "تاريخ التسليم: ${mySubmission.submittedAt}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }

                                    if (mySubmission.teacherFeedback.isNotBlank()) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.RateReview,
                                                        contentDescription = null,
                                                        tint = DeepEmerald,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = if (mySubmission.reviewedByTeacher.isNotBlank()) {
                                                            "ملاحظة المعلم (${mySubmission.reviewedByTeacher}):"
                                                        } else {
                                                            "ملاحظة المعلم:"
                                                        },
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepEmerald
                                                    )
                                                }
                                                Text(
                                                    text = mySubmission.teacherFeedback,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // يظهر زر إرفاق الحل مرة واحدة فقط قبل التسليم ويختفي نهائياً بعد إرسال الواجب أو مراجعته
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompressedPhotoPickerButton(
                                            label = if (pendingPhotoBase64.isEmpty()) "تصوير/إرفاق دفتر الحل" else "جاهز للإرسال",
                                            hasImage = pendingPhotoBase64.isNotEmpty(),
                                            onPhotoSelected = { pendingPhotoBase64 = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (pendingPhotoBase64.isNotEmpty()) {
                                            Button(
                                                onClick = {
                                                    viewModel.submitHomeworkPhotoSolution(
                                                        homework = hw,
                                                        solutionImageBase64 = pendingPhotoBase64,
                                                        studentNote = pendingStudentNote
                                                    )
                                                    pendingPhotoBase64 = ""
                                                },
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                                                modifier = Modifier.testTag("send_hw_photo_btn_${hw.id}")
                                            ) {
                                                Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("إرسال للمعلم", fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                                    } else if (canPublish && hwSubs.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = DeepEmerald.copy(alpha = 0.06f),
                                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.16f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(12.dp),
                                                verticalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Text(
                                                    text = "حلول الواجب المرسلة بالصورة من الطلاب (${hwSubs.size}):",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepEmerald
                                                )
                                                hwSubs.forEach { sub ->
                                                    var isGradingExpanded by remember(sub.id, sub.isReviewed) {
                                                        mutableStateOf(!sub.isReviewed)
                                                    }
                                                    var customFeedbackInput by remember(sub.id, sub.teacherFeedback) {
                                                        mutableStateOf(
                                                            sub.teacherFeedback.ifBlank {
                                                                "أحسنت بارك الله بك، تمت مراجعة وتصحيح الواجب"
                                                            }
                                                        )
                                                    }
                                                    var customScoreInput by remember(sub.id, sub.homeworkScore) {
                                                        mutableStateOf(sub.homeworkScore)
                                                    }

                                                    Surface(
                                                        shape = RoundedCornerShape(12.dp),
                                                        color = MaterialTheme.colorScheme.surface,
                                                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(10.dp),
                                                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                                                    Base64AvatarImage(
                                                                        base64Data = sub.solutionImageBase64,
                                                                        size = 44.dp,
                                                                        fallbackIcon = Icons.Filled.Image,
                                                                        contentDescription = "حل الطالب ${sub.studentName}"
                                                                    )
                                                                    Column(modifier = Modifier.weight(1f)) {
                                                                        Text(
                                                                            text = "${sub.studentName} (اضغط على الصورة للتكبير)",
                                                                            style = MaterialTheme.typography.labelMedium,
                                                                            fontWeight = FontWeight.Bold
                                                                        )
                                                                        Row(
                                                                            verticalAlignment = Alignment.CenterVertically,
                                                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                        ) {
                                                                            Text(
                                                                                text = if (sub.isReviewed) "تم التصحيح" else "بانتظار التصحيح • ${sub.submittedAt}",
                                                                                style = MaterialTheme.typography.labelSmall,
                                                                                color = if (sub.isReviewed) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                                                                fontWeight = if (sub.isReviewed) FontWeight.Bold else FontWeight.Normal
                                                                            )
                                                                            if (sub.homeworkScore.isNotBlank()) {
                                                                                Surface(
                                                                                    shape = RoundedCornerShape(50),
                                                                                    color = Color(0xFFECFDF5),
                                                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.4f))
                                                                                ) {
                                                                                    Text(
                                                                                        text = "درجة الواجب: ${sub.homeworkScore}",
                                                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                                                        style = MaterialTheme.typography.labelSmall,
                                                                                        fontWeight = FontWeight.ExtraBold,
                                                                                        color = DeepEmerald
                                                                                    )
                                                                                }
                                                                            }
                                                                        }
                                                                    }
                                                                }

                                                                TextButton(
                                                                    onClick = { isGradingExpanded = !isGradingExpanded }
                                                                ) {
                                                                    Icon(
                                                                        imageVector = if (sub.isReviewed) Icons.Filled.Edit else Icons.Filled.RateReview,
                                                                        contentDescription = null,
                                                                        tint = DeepEmerald,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text(
                                                                        text = if (sub.isReviewed) "تعديل الرد" else "تصحيح",
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = DeepEmerald
                                                                    )
                                                                }
                                                            }

                                                            if (sub.isReviewed && !isGradingExpanded && sub.teacherFeedback.isNotBlank()) {
                                                                Text(
                                                                    text = "ملاحظة المعلم: ${sub.teacherFeedback}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = DeepEmerald,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )
                                                            }

                                                            if (isGradingExpanded) {
                                                                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.12f))

                                                                val quickReplies = listOf(
                                                                    "أحسنت بارك الله بك، حل نموذجي ومرتب",
                                                                    "ممتاز جداً، إجابات صحيحة وخط واضح",
                                                                    "جيد، يرجى مراجعة الخطوات وتصحيح الأخطاء البسيطة",
                                                                    "يرجى إكمال حل جميع فقرات الواجب"
                                                                )
                                                                LazyRow(
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    items(quickReplies) { preset ->
                                                                        SuggestionChip(
                                                                            onClick = { customFeedbackInput = preset },
                                                                            label = {
                                                                                Text(
                                                                                    text = preset,
                                                                                    style = MaterialTheme.typography.labelSmall,
                                                                                    maxLines = 1
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                }

                                                                OutlinedTextField(
                                                                    value = customFeedbackInput,
                                                                    onValueChange = { customFeedbackInput = it },
                                                                    label = { Text("رد وملاحظة المعلم المخصصة للطالب") },
                                                                    placeholder = { Text("اكتب ملاحظتك التربوية أو توجيهاتك للطالب...") },
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .testTag("hw_feedback_input_${sub.id}"),
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    maxLines = 3
                                                                )

                                                                val quickScores = listOf("10 / 10", "9 / 10", "ممتاز 100%", "جيد جداً")
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                ) {
                                                                    OutlinedTextField(
                                                                        value = customScoreInput,
                                                                        onValueChange = { customScoreInput = it },
                                                                        label = { Text("درجة الواجب (اختياري — مثل: 10 / 10)") },
                                                                        singleLine = true,
                                                                        modifier = Modifier
                                                                            .weight(1f)
                                                                            .testTag("hw_score_input_${sub.id}"),
                                                                        shape = RoundedCornerShape(12.dp)
                                                                    )
                                                                }

                                                                LazyRow(
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    items(quickScores) { sc ->
                                                                        FilterChip(
                                                                            selected = customScoreInput == sc,
                                                                            onClick = {
                                                                                customScoreInput = if (customScoreInput == sc) "" else sc
                                                                            },
                                                                            label = {
                                                                                Text(
                                                                                    text = sc,
                                                                                    style = MaterialTheme.typography.labelSmall
                                                                                )
                                                                            }
                                                                        )
                                                                    }
                                                                }

                                                                Button(
                                                                    onClick = {
                                                                        viewModel.reviewHomeworkSubmission(
                                                                            submissionId = sub.id,
                                                                            feedback = customFeedbackInput,
                                                                            score = customScoreInput
                                                                        )
                                                                        isGradingExpanded = false
                                                                    },
                                                                    modifier = Modifier
                                                                        .fillMaxWidth()
                                                                        .testTag("review_hw_sub_btn_${sub.id}"),
                                                                    shape = RoundedCornerShape(12.dp),
                                                                    colors = ButtonDefaults.buttonColors(
                                                                        containerColor = DeepEmerald,
                                                                        contentColor = Color.White
                                                                    )
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Filled.CheckCircle,
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFA7F3D0),
                                                                        modifier = Modifier.size(17.dp)
                                                                    )
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text(
                                                                        text = "اعتماد التصحيح وإرسال الرد",
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

                                if (canPublish) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { editingHomework = hw },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("edit_homework_${hw.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Edit,
                                                contentDescription = "تعديل الواجب",
                                                tint = DeepEmerald,
                                                modifier = Modifier.size(15.dp)
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
                                            onClick = { deletingHomework = hw },
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(36.dp)
                                                .testTag("delete_homework_${hw.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "حذف الواجب",
                                                tint = DangerRed,
                                                modifier = Modifier.size(15.dp)
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
                    if (displayedHomeworks.size < dateScopedHomeworks.size) {
                        item {
                            ProgressiveInfiniteScrollFooter(
                                visibleCount = displayedHomeworks.size,
                                totalCount = dateScopedHomeworks.size,
                                step = 6,
                                itemLabel = "واجب",
                                onLoadMore = {
                                    homeworksVisibleCount = (homeworksVisibleCount + 6).coerceAtMost(dateScopedHomeworks.size)
                                }
                            )
                        }
                    }
                }
        } else if (selectedTab == 1) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

                    if (canPublish) {
                        VectorActionButton(
                            text = "إضافة أو تعديل حصة في الجدول الأسبوعي",
                            icon = Icons.Filled.AddAlarm,
                            onClick = { showAddPeriodDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

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
                                    text = "$staffTitleSingular: ${period.teacherName}",
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
        } else {
            // =========================================================================
            // TAB 2: المكتبة الرقمية والملخصات الدراسية (study_materials)
            // =========================================================================
            if (canPublish) {
                item {
                    VectorActionButton(
                        text = "نشر ملخص دراسي / ملزمة / ورقة عمل لـ ($selectedClass - $selectedSection)",
                        icon = Icons.Filled.LibraryAdd,
                        onClick = { showPublishStudyMaterialDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "publish_study_material_button"
                    )
                }
            }

            if (filteredStudyMaterials.isEmpty()) {
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
                                    imageVector = Icons.Filled.LocalLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "لا توجد ملخصات أو ملازم منشورة لـ $selectedClass ($selectedSection) حالياً",
                                    style = MaterialTheme.typography.titleMedium,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(filteredStudyMaterials, key = { it.id }) { mat ->
                        val bitmap = remember(mat.imageBase64) {
                            ImageCompressor.decodeBase64ToImageBitmap(mat.imageBase64)
                        }
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
                                        text = "مادة ${mat.subjectName}",
                                        icon = Icons.Filled.MenuBook,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        NoWrapBadge(
                                            text = mat.createdAt,
                                            icon = Icons.Filled.CalendarToday,
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        if (canPublish) {
                                            IconButton(
                                                onClick = { viewModel.deleteStudyMaterial(mat.id) },
                                                modifier = Modifier.size(30.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.DeleteOutline,
                                                    contentDescription = "حذف الملخص",
                                                    tint = DangerRed
                                                )
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = mat.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "الصف: ${mat.gradeClass} (${mat.sectionName}) • الناشر: ${mat.teacherName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )

                                Text(
                                    text = mat.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap,
                                        contentDescription = mat.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(190.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                GlobalImageLightboxController.open(mat.imageBase64, mat.title)
                                            }
                                    )
                                }

                                if (mat.fileUrl.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            try {
                                                val url = if (mat.fileUrl.startsWith("http")) mat.fileUrl else "https://${mat.fileUrl}"
                                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Filled.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("فتح / تحميل ملف الملزمة أو الملخص (PDF)")
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    if (inspectingQuizParticipants != null) {
        val liveQuiz = quickQuizzes.firstOrNull { it.id == inspectingQuizParticipants?.id } ?: inspectingQuizParticipants!!
        val targetStudents = students.filter { stu ->
            (stu.schoolId.isEmpty() || stu.schoolId == schoolConfig.schoolId) &&
                ArabicTextNormalizer.matchesClass(stu.gradeClass, liveQuiz.gradeClass) &&
                (liveQuiz.sectionName == "الكل" || ArabicTextNormalizer.matchesSection(stu.sectionName, liveQuiz.sectionName))
        }
        QuickQuizParticipantsModal(
            quiz = liveQuiz,
            targetStudents = targetStudents,
            onDismiss = { inspectingQuizParticipants = null }
        )
    }

    // Publish Quick Quiz Modal
    if (showPublishQuickQuizDialog) {
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var questionText by remember { mutableStateOf("") }
        var opt1 by remember { mutableStateOf("") }
        var opt2 by remember { mutableStateOf("") }
        var opt3 by remember { mutableStateOf("") }
        var correctIdx by remember { mutableIntStateOf(0) }
        var pointsStr by remember { mutableStateOf("5") }

        AlertDialog(
            onDismissRequest = { showPublishQuickQuizDialog = false },
            title = { Text("نشر اختبار سريع مؤقت لـ ($selectedClass - $selectedSection)", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        value = questionText,
                        onValueChange = { questionText = it },
                        label = { Text("نص السؤال التفاعلي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt1,
                        onValueChange = { opt1 = it },
                        label = { Text("الخيار الأول (1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt2,
                        onValueChange = { opt2 = it },
                        label = { Text("الخيار الثاني (2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = opt3,
                        onValueChange = { opt3 = it },
                        label = { Text("الخيار الثالث (3 - اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجابة الصحيحة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(0 to "خيار 1", 1 to "خيار 2", 2 to "خيار 3").forEach { (idx, lbl) ->
                            FilterChip(
                                selected = correctIdx == idx,
                                onClick = { correctIdx = idx },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = pointsStr,
                        onValueChange = { pointsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("درجة السؤال") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val optionsList = listOfNotNull(
                            opt1.trim().takeIf { it.isNotEmpty() },
                            opt2.trim().takeIf { it.isNotEmpty() },
                            opt3.trim().takeIf { it.isNotEmpty() }
                        )
                        if (questionText.isNotBlank() && optionsList.size >= 2) {
                            viewModel.publishQuickQuiz(
                                gradeClass = selectedClass,
                                sectionName = selectedSection,
                                subjectName = subject,
                                questionText = questionText,
                                options = optionsList,
                                correctOptionIndex = correctIdx.coerceIn(0, optionsList.lastIndex),
                                points = pointsStr.toIntOrNull()?.coerceIn(1, 100) ?: 5
                            )
                            showPublishQuickQuizDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_publish_quick_quiz_btn")
                ) {
                    Text("نشر الاختبار للطلاب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishQuickQuizDialog = false }) { Text("إلغاء") }
            }
        )
    }

    editingQuiz?.let { quizTarget ->
        var editSubject by remember(quizTarget.id) { mutableStateOf(quizTarget.subjectName) }
        var editQuestion by remember(quizTarget.id) { mutableStateOf(quizTarget.questionText) }
        var editOpt1 by remember(quizTarget.id) { mutableStateOf(quizTarget.options.getOrElse(0) { "" }) }
        var editOpt2 by remember(quizTarget.id) { mutableStateOf(quizTarget.options.getOrElse(1) { "" }) }
        var editOpt3 by remember(quizTarget.id) { mutableStateOf(quizTarget.options.getOrElse(2) { "" }) }
        var editCorrectIdx by remember(quizTarget.id) { mutableIntStateOf(quizTarget.correctOptionIndex) }
        var editPointsStr by remember(quizTarget.id) { mutableStateOf(quizTarget.points.toString()) }

        AlertDialog(
            onDismissRequest = { editingQuiz = null },
            title = {
                Text(
                    text = "تعديل الاختبار السريع (${quizTarget.subjectName})",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allowedSubjects) { sOpt ->
                            FilterChip(
                                selected = editSubject == sOpt,
                                onClick = { editSubject = sOpt },
                                label = { Text(sOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editQuestion,
                        onValueChange = { editQuestion = it },
                        label = { Text("نص السؤال التفاعلي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOpt1,
                        onValueChange = { editOpt1 = it },
                        label = { Text("الخيار الأول (1)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOpt2,
                        onValueChange = { editOpt2 = it },
                        label = { Text("الخيار الثاني (2)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editOpt3,
                        onValueChange = { editOpt3 = it },
                        label = { Text("الخيار الثالث (3 - اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("الإجابة الصحيحة:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        listOf(0 to "خيار 1", 1 to "خيار 2", 2 to "خيار 3").forEach { (idx, lbl) ->
                            FilterChip(
                                selected = editCorrectIdx == idx,
                                onClick = { editCorrectIdx = idx },
                                label = { Text(lbl) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editPointsStr,
                        onValueChange = { editPointsStr = it.filter { ch -> ch.isDigit() } },
                        label = { Text("درجة السؤال") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val optionsList = listOfNotNull(
                            editOpt1.trim().takeIf { it.isNotEmpty() },
                            editOpt2.trim().takeIf { it.isNotEmpty() },
                            editOpt3.trim().takeIf { it.isNotEmpty() }
                        )
                        if (editQuestion.isNotBlank() && optionsList.size >= 2) {
                            viewModel.updateQuickQuiz(
                                quizId = quizTarget.id,
                                subjectName = editSubject,
                                questionText = editQuestion,
                                options = optionsList,
                                correctOptionIndex = editCorrectIdx.coerceIn(0, optionsList.lastIndex),
                                points = editPointsStr.toIntOrNull()?.coerceIn(1, 100) ?: quizTarget.points
                            )
                            editingQuiz = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_edit_quick_quiz_btn")
                ) {
                    Text("حفظ التعديل", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingQuiz = null }) { Text("إلغاء") }
            }
        )
    }

    // Publish Homework Modal with Dynamic Registered Subject Selector & Camera/Gallery Illustrative Photo Picker
    if (showPublishHomeworkDialog) {
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var subjExpanded by remember { mutableStateOf(false) }
        var prep by remember { mutableStateOf("") }
        var hwDetails by remember { mutableStateOf("") }
        var dueDate by remember { mutableStateOf("2026-11-06") }
        var attachmentImageBase64 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPublishHomeworkDialog = false },
            title = {
                Text(
                    text = "نشر التحضير والواجب لـ ($selectedClass - $selectedSection)",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepForestGreen
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                    ModernCalendarDatePickerField(
                        selectedDateIso = dueDate,
                        label = "تاريخ التسليم",
                        onDateSelected = { selectedIso, _ -> dueDate = selectedIso }
                    )

                    HomeworkIllustrativePhotoPickerSection(
                        attachmentBase64 = attachmentImageBase64,
                        onAttachmentChanged = { attachmentImageBase64 = it }
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
                                dueDate = dueDate,
                                attachmentImageBase64 = attachmentImageBase64
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

    // Edit Homework Modal
    editingHomework?.let { hwTarget ->
        var editSubject by remember(hwTarget.id) { mutableStateOf(hwTarget.subjectName) }
        var editSubjExpanded by remember { mutableStateOf(false) }
        var editPrep by remember(hwTarget.id) { mutableStateOf(hwTarget.dailyPreparation) }
        var editHwDetails by remember(hwTarget.id) { mutableStateOf(hwTarget.homeworkDetails) }
        var editDueDate by remember(hwTarget.id) { mutableStateOf(hwTarget.dueDate) }
        var editAttachmentBase64 by remember(hwTarget.id) { mutableStateOf(hwTarget.attachmentImageBase64) }

        AlertDialog(
            onDismissRequest = { editingHomework = null },
            title = {
                Text(
                    text = "تعديل التحضير والواجب (${hwTarget.subjectName})",
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepEmerald
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = editSubjExpanded,
                        onExpandedChange = { editSubjExpanded = !editSubjExpanded }
                    ) {
                        OutlinedTextField(
                            value = editSubject,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("المادة الدراسية") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = editSubjExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = editSubjExpanded,
                            onDismissRequest = { editSubjExpanded = false }
                        ) {
                            allowedSubjects.forEach { sOption ->
                                DropdownMenuItem(
                                    text = { Text(sOption) },
                                    onClick = {
                                        editSubject = sOption
                                        editSubjExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = editPrep,
                        onValueChange = { editPrep = it },
                        label = { Text("التحضير اليومي للدرس القادم") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editHwDetails,
                        onValueChange = { editHwDetails = it },
                        label = { Text("تفاصيل الواجب البيتي المطلوب") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = editDueDate,
                        label = "تاريخ التسليم",
                        onDateSelected = { selectedIso, _ -> editDueDate = selectedIso }
                    )

                    HomeworkIllustrativePhotoPickerSection(
                        attachmentBase64 = editAttachmentBase64,
                        onAttachmentChanged = { editAttachmentBase64 = it }
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ التعديلات",
                    icon = Icons.Filled.CheckCircle,
                    onClick = {
                        if (editPrep.isNotBlank()) {
                            viewModel.updateHomework(
                                homeworkId = hwTarget.id,
                                gradeClass = hwTarget.gradeClass,
                                sectionName = hwTarget.sectionName,
                                subjectName = editSubject,
                                teacherName = hwTarget.teacherName,
                                dailyPreparation = editPrep,
                                homeworkDetails = editHwDetails.ifBlank { "مراجعة الدرس وحل أسئلة الفصل" },
                                dueDate = editDueDate,
                                attachmentImageBase64 = editAttachmentBase64
                            )
                            editingHomework = null
                        }
                    },
                    testTag = "confirm_edit_homework_btn"
                )
            },
            dismissButton = {
                TextButton(onClick = { editingHomework = null }) { Text("إلغاء") }
            }
        )
    }

    // Delete Homework Confirmation Modal
    deletingHomework?.let { hwTarget ->
        AlertDialog(
            onDismissRequest = { deletingHomework = null },
            title = {
                Text(
                    text = "تأكيد حذف الواجب الدراسي",
                    fontWeight = FontWeight.ExtraBold,
                    color = DangerRed
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف واجب مادة (${hwTarget.subjectName}) للصف (${hwTarget.gradeClass} - شعبة ${hwTarget.sectionName})؟ سيتم حذفه فوراً من السحابة وجميع الأجهزة.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                VectorActionButton(
                    text = "تأكيد الحذف",
                    icon = Icons.Filled.DeleteOutline,
                    containerColor = DangerRed,
                    onClick = {
                        viewModel.deleteHomework(hwTarget.id)
                        deletingHomework = null
                    },
                    testTag = "confirm_delete_homework_btn"
                )
            },
            dismissButton = {
                TextButton(onClick = { deletingHomework = null }) { Text("إلغاء") }
            }
        )
    }

    if (showAddPeriodDialog) {
        val registeredTeachers = remember(accounts) {
            accounts.filter { it.role == UserRole.TEACHER }
        }
        var periodNo by remember { mutableStateOf("1") }
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var teacher by remember(currentUser, registeredTeachers) {
            val defaultTeacher = if (currentUser?.role == UserRole.TEACHER) {
                currentUser?.fullName
            } else {
                registeredTeachers.firstOrNull()?.fullName
            }
            mutableStateOf(defaultTeacher ?: "")
        }
        var teacherSearchQuery by remember { mutableStateOf("") }
        var teacherDropdownExpanded by remember { mutableStateOf(false) }

        val timeOptions = remember {
            listOf(
                "08:00 ص",
                "08:45 ص",
                "09:30 ص",
                "10:15 ص",
                "11:00 ص",
                "11:45 ص",
                "12:30 م",
                "01:15 م",
                "02:00 م"
            )
        }
        var startTime by remember { mutableStateOf("08:00 ص") }
        var endTime by remember { mutableStateOf("08:45 ص") }
        var startExpanded by remember { mutableStateOf(false) }
        var endExpanded by remember { mutableStateOf(false) }

        val filteredTeachers = remember(registeredTeachers, teacherSearchQuery) {
            if (teacherSearchQuery.isBlank()) {
                registeredTeachers
            } else {
                val q = teacherSearchQuery.trim()
                registeredTeachers.filter { tAcc ->
                    tAcc.fullName.contains(q, ignoreCase = true) ||
                        tAcc.specialty.contains(q, ignoreCase = true) ||
                        tAcc.teacherSubjectsList.any { it.contains(q, ignoreCase = true) }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddPeriodDialog = false },
            title = { Text("إضافة حصة ليوم $selectedDay ($selectedClass - $selectedSection)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = periodNo,
                        onValueChange = { periodNo = it.filter { c -> c.isDigit() } },
                        label = { Text("رقم الحصة (1 - 7)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("المادة الدراسية:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allowedSubjects) { sOpt ->
                            FilterChip(
                                selected = subject == sOpt,
                                onClick = { subject = sOpt },
                                label = { Text(sOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    // Searchable Select Dropdown for Teacher Name
                    Text(
                        text = "اختيار $staffTitleSingular من الكادر المسجل:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                    ExposedDropdownMenuBox(
                        expanded = teacherDropdownExpanded,
                        onExpandedChange = { teacherDropdownExpanded = !teacherDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = teacherSearchQuery.ifBlank { teacher },
                            onValueChange = {
                                teacherSearchQuery = it
                                teacher = it
                                teacherDropdownExpanded = true
                            },
                            label = { Text("ابحث أو اختر اسم $staffTitleSingular...") },
                            leadingIcon = { Icon(Icons.Filled.PersonSearch, contentDescription = null, tint = DeepEmerald) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = teacherDropdownExpanded) },
                            singleLine = true,
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = teacherDropdownExpanded,
                            onDismissRequest = { teacherDropdownExpanded = false }
                        ) {
                            if (filteredTeachers.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("لا يوجد مطابق في الكادر المسجل") },
                                    onClick = { teacherDropdownExpanded = false }
                                )
                            } else {
                                filteredTeachers.forEach { tAcc ->
                                    val subjInfo = tAcc.teacherSubjectsList.joinToString("، ").ifBlank { tAcc.specialty }
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(tAcc.fullName, fontWeight = FontWeight.Bold)
                                                if (subjInfo.isNotBlank()) {
                                                    Text(
                                                        text = "الاختصاص: $subjInfo",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        onClick = {
                                            teacher = tAcc.fullName
                                            teacherSearchQuery = ""
                                            teacherDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Structured Start & End Time Selectors
                    Text(
                        text = "توقيت الحصة الدراسية (من الساعة — إلى الساعة):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExposedDropdownMenuBox(
                            expanded = startExpanded,
                            onExpandedChange = { startExpanded = !startExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = startTime,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("من الساعة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startExpanded) },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = startExpanded,
                                onDismissRequest = { startExpanded = false }
                            ) {
                                timeOptions.forEach { tOpt ->
                                    DropdownMenuItem(
                                        text = { Text(tOpt) },
                                        onClick = {
                                            startTime = tOpt
                                            startExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = endExpanded,
                            onExpandedChange = { endExpanded = !endExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = endTime,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("إلى الساعة") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = endExpanded) },
                                singleLine = true,
                                modifier = Modifier
                                    .menuAnchor()
                                    .fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = endExpanded,
                                onDismissRequest = { endExpanded = false }
                            ) {
                                timeOptions.forEach { tOpt ->
                                    DropdownMenuItem(
                                        text = { Text(tOpt) },
                                        onClick = {
                                            endTime = tOpt
                                            endExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ في الجدول",
                    icon = Icons.Filled.Save,
                    onClick = {
                        val finalTeacher = teacher.trim().ifBlank { currentUser?.fullName ?: staffTitleSingular }
                        val formattedSlot = "$startTime - $endTime"
                        viewModel.addSchedulePeriod(
                            gradeClass = selectedClass,
                            sectionName = selectedSection,
                            dayOfWeek = selectedDay,
                            periodNumber = periodNo.toIntOrNull() ?: 1,
                            subjectName = subject,
                            teacherName = finalTeacher,
                            timeSlot = formattedSlot
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

    if (showPublishStudyMaterialDialog) {
        var subject by remember(allowedSubjects) { mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات") }
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var docLink by remember { mutableStateOf("") }
        var imageBase64 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPublishStudyMaterialDialog = false },
            title = { Text("نشر ملخص أو ملزمة لـ ($selectedClass - $selectedSection)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("المادة الدراسية:", style = MaterialTheme.typography.labelMedium)
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
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الملخص / الملزمة / ورقة العمل") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("شرح ومحتوى الملخص الدراسي") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = docLink,
                        onValueChange = { docLink = it },
                        label = { Text("رابط ملف PDF أو الملزمة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompressedPhotoPickerButton(
                        label = "إرفاق صورة توضيحية أو ورقة عمل",
                        currentBase64 = imageBase64,
                        onImageCompressed = { imageBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "نشر في المكتبة الرقمية",
                    icon = Icons.Filled.CloudUpload,
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.publishStudyMaterial(
                                gradeClass = selectedClass,
                                sectionName = selectedSection,
                                subjectName = subject,
                                title = title,
                                description = description.ifBlank { "ملخص دراسي شامل للمراجعة" },
                                fileUrl = docLink,
                                imageBase64 = imageBase64
                            )
                            showPublishStudyMaterialDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPublishStudyMaterialDialog = false }) { Text("إلغاء") }
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
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val students by viewModel.students.collectAsState()
    val grades by viewModel.grades.collectAsState()
    val examSchedules by viewModel.examSchedules.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val canEdit = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    val allowedClasses = remember(schoolConfig, currentUser, students, selectedParentChildId) {
        viewModel.getAllowedClassesForUser(currentUser)
    }
    var selectedClass by remember(currentUser?.id, allowedClasses) {
        mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
    }
    val allowedSections = remember(schoolConfig, currentUser, students, selectedClass, selectedParentChildId) {
        viewModel.getAllowedSectionsForClass(selectedClass, currentUser)
    }
    var selectedSection by remember(currentUser?.id, selectedClass, allowedSections) {
        mutableStateOf(allowedSections.firstOrNull() ?: "أ")
    }
    val allowedSubjects = remember(schoolConfig, selectedClass, currentUser) {
        viewModel.getSubjectsForClass(selectedClass, currentUser)
    }
    var selectedSubject by remember(currentUser?.id, selectedClass, allowedSubjects) {
        mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات")
    }
    var selectedMaxScale by remember(selectedClass, selectedSubject, schoolConfig, grades) {
        mutableIntStateOf(viewModel.getDefaultMaxScoreForClassAndSubject(selectedClass, selectedSubject))
    }

    var searchStudentName by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = رصد/كشف الدرجات, 1 = مواعيد الامتحانات, 2 = إصدار الكارتات والنتائج
    var selectedReportCardPeriod by remember { mutableStateOf(ReportCardPeriodType.COMPREHENSIVE_ALL) }
    var selectedApprovalPeriod by remember { mutableStateOf(GradeAssessmentPeriod.MONTH_1) }
    var pendingOverrideApprovalSummary by remember { mutableStateOf<PeriodCompletionSummary?>(null) }
    var showAddExamDialog by remember { mutableStateOf(false) }
    var editingExamSchedule by remember { mutableStateOf<ExamScheduleItem?>(null) }

    val issuedReportCards by viewModel.issuedReportCards.collectAsState()
    val activeCelebrationCard by viewModel.activeCelebrationReportCard.collectAsState()

    val deepLinkTarget by viewModel.deepLinkTarget.collectAsState()
    LaunchedEffect(deepLinkTarget) {
        val target = deepLinkTarget
        if (target != null && target.section == AppSection.EXAMS_GRADES) {
            selectedTab = target.tabIndex.coerceIn(0, 2)
            if (target.itemId.startsWith("rc_")) {
                issuedReportCards.firstOrNull { it.id == target.itemId }?.let { card ->
                    viewModel.openCelebrationReportCardModal(card)
                }
            }
            viewModel.clearDeepLinkTarget()
        }
    }

    // Principal Override Warning Modal when some students do not have recorded grades for the selected period
    pendingOverrideApprovalSummary?.let { summary ->
        AlertDialog(
            onDismissRequest = { pendingOverrideApprovalSummary = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = Color(0xFFD97706),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "تنبيه عدم اكتمال رصد درجات (${summary.period.arabicTitle})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF92400E),
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "اكتمال الرصد الحالي: ${summary.recordedStudentsCount} من ${summary.totalStudentsCount} طالب",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = Color(0xFF78350F)
                            )
                            Text(
                                text = "يوجد (${summary.unrecordedCount}) من الطلبة في ($selectedClass - شعبة $selectedSection) لم يقم المعلم برصد درجاتهم في مادة ($selectedSubject) لفترة (${summary.period.arabicTitle}):",
                                fontSize = 12.sp,
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "الطلبة غير المرصودة درجاتهم:",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = Color(0xFF991B1B)
                            )
                            summary.unrecordedStudentNames.forEachIndexed { idx, stuName ->
                                Text(
                                    text = "${idx + 1}. $stuName",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFB91C1C)
                                )
                            }
                        }
                    }

                    // Two explicit options for the Principal (Pure-Text Capsules)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { pendingOverrideApprovalSummary = null },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF047857),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_cancel_approval_wait_teacher")
                        ) {
                            Text(
                                text = "إلغاء والعودة لحين استكمال المعلم لرصد جميع الطلبة",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.setClassSubjectPeriodApproval(
                                    gradeClass = selectedClass,
                                    sectionName = selectedSection,
                                    subjectName = selectedSubject,
                                    period = summary.period,
                                    approved = true
                                )
                                pendingOverrideApprovalSummary = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.4.dp, Color(0xFFD97706)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFB45309)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_override_approval_by_principal")
                        ) {
                            Text(
                                text = "تجاوز التحذير والمصادقة الاستثنائية بأمر المدير",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }

    // Active Celebration / Report Card Preview Modal
    activeCelebrationCard?.let { cardToView ->
        val matchedStu = students.firstOrNull { it.id == cardToView.studentId }
        val stuGrades = grades.filter { it.studentId == cardToView.studentId }
        LuxuryInteractiveReportCardModal(
            reportCard = cardToView,
            schoolConfig = schoolConfig,
            onDismiss = { viewModel.closeCelebrationReportCardModal() },
            onPrintPdf = {
                if (matchedStu != null) {
                    val res = DocumentAndIdExporter.exportStudentReportCardPdf(
                        context = context,
                        student = matchedStu,
                        grades = stuGrades,
                        schoolConfig = schoolConfig,
                        principalName = cardToView.principalName,
                        periodLabel = cardToView.periodType.arabicTitle
                    )
                    viewModel.showBannerMessage(res)
                }
            }
        )
    }

    // Unified Scrollable Top Banner + 3-Tab Switcher Header rendered inside each LazyColumn so nothing gets cut off or squeezed
    val renderTopTabsHeader: @Composable () -> Unit = {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isParentOrStudent) {
                val myStu = viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
                val myLatestCard = remember(issuedReportCards, myStu?.id) {
                    myStu?.id?.let { viewModel.getLatestIssuedReportCardForStudent(it) }
                }
                if (myStu?.isReportCardSentByPrincipal == true && myLatestCard != null) {
                    ParentIssuedReportCardArrivalBanner(
                        reportCard = myLatestCard,
                        onClick = { viewModel.openCelebrationReportCardModal(myLatestCard) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val isGradesTab = selectedTab == 0
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isGradesTab) Color(0xFF059669) else Color.White,
                    border = BorderStroke(1.2.dp, if (isGradesTab) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                    shadowElevation = if (isGradesTab) 6.dp else 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedTab = 0 }
                        .testTag("tab_detailed_grades")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isParentOrStudent) "جدول كشف الدرجات" else "الدرجات حسب المادة",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isGradesTab) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                val isScheduleTab = selectedTab == 1
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isScheduleTab) Color(0xFF059669) else Color.White,
                    border = BorderStroke(1.2.dp, if (isScheduleTab) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                    shadowElevation = if (isScheduleTab) 6.dp else 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedTab = 1 }
                        .testTag("tab_exam_schedule")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مواعيد الامتحانات",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isScheduleTab) Color.White else Color(0xFF0F172A),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 3rd Main Tab: إصدار الكارتات والنتائج (Principal Only) / كارت الدرجات الرسمي (Student & Parent)
            if (currentUser?.role == UserRole.ADMIN || isParentOrStudent) {
                val isReportCardsTab = selectedTab == 2
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isReportCardsTab) Color(0xFF047857) else Color(0xFFFFFBEB),
                    border = BorderStroke(
                        1.4.dp,
                        if (isReportCardsTab) Color(0xFFFDE68A) else Color(0xFFF59E0B).copy(alpha = 0.55f)
                    ),
                    shadowElevation = if (isReportCardsTab) 7.dp else 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 46.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { selectedTab = 2 }
                        .testTag("tab_smart_report_cards_hub")
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (currentUser?.role == UserRole.ADMIN) {
                                "إصدار الكارتات والنتائج"
                            } else {
                                "كارت الدرجات الرسمي"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isReportCardsTab) Color.White else Color(0xFF78350F),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (selectedTab == 0) {
            if (isParentOrStudent) {
                // =============================================================
                // STEP D: STUDENT & PARENT ORGANIZED REPORT CARD TABLE
                // =============================================================
                val myStu = viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
                if (myStu != null) {
                    val myGrades = grades.filter { it.studentId == myStu.id }
                    val myBadges = viewModel.getStudentAutoBadges(myStu.id)
                    val studentSubjects = viewModel.getSubjectsForClass(myStu.gradeClass, null)
                    val allSubjectsReport = studentSubjects.map { subjName ->
                        val dynamicScale = viewModel.getDefaultMaxScoreForClassAndSubject(myStu.gradeClass, subjName)
                        myGrades.firstOrNull { it.subjectName == subjName } ?: SubjectGrade(
                            id = "temp_$subjName",
                            schoolId = schoolConfig.schoolId,
                            studentId = myStu.id,
                            studentName = myStu.studentName,
                            gradeClass = myStu.gradeClass,
                            subjectName = subjName,
                            maxScoreScale = dynamicScale,
                            dailyScore = 0,
                            month1Score = 0,
                            month2Score = 0,
                            midYearScore = 0,
                            finalExamScore = 0
                        )
                    }
                    val allSubjectsCompleted = allSubjectsReport.isNotEmpty() && allSubjectsReport.all { it.isAllExamsCompleted }
                    val completedSubjects = allSubjectsReport.filter { it.isAllExamsCompleted }
                    val overallAvg = when {
                        allSubjectsCompleted -> allSubjectsReport.map { it.normalizedPercentage }.average().toInt()
                        completedSubjects.isNotEmpty() -> completedSubjects.map { it.normalizedPercentage }.average().toInt()
                        allSubjectsReport.isNotEmpty() -> allSubjectsReport.map {
                            if (it.maxScoreScale == 10) (it.annualEffortScore * 10).coerceIn(0, 100) else it.annualEffortScore.coerceIn(0, 100)
                        }.average().toInt()
                        else -> 0
                    }
                    val overallRating = when {
                        !allSubjectsCompleted -> "مستمر / متابع"
                        overallAvg >= 90 -> "ممتاز"
                        overallAvg >= 80 -> "جيد جداً"
                        overallAvg >= 70 -> "جيد"
                        overallAvg >= 60 -> "متوسط"
                        overallAvg >= 50 -> "ناجح (مقبول)"
                        else -> "راسب"
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 110.dp)
                    ) {
                        item(key = "exams_top_tabs_parent_0") {
                            renderTopTabsHeader()
                        }
                        item(key = "student_report_card_header_${myStu.id}") {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "كشف درجات الطالب: ${myStu.fullNameWithFather}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "الصف: ${myStu.gradeClass} • شعبة (${myStu.sectionName})",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            NoWrapBadge(
                                                text = if (allSubjectsCompleted) "المعدل النهائي: $overallAvg%" else "مؤشر المتابعة: $overallAvg%",
                                                icon = Icons.Filled.AutoGraph,
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            NoWrapBadge(
                                                text = "الحالة: $overallRating",
                                                icon = Icons.Filled.WorkspacePremium,
                                                containerColor = GoldAccent,
                                                contentColor = Color(0xFF451A03)
                                            )
                                        }
                                    }
                                    if (myBadges.isNotEmpty()) {
                                        StudentAutoBadgesRow(badges = myBadges)
                                    }
                                    if (myStu.isReportCardSentByPrincipal) {
                                        OutlinedButton(
                                            onClick = {
                                                val msg = DocumentAndIdExporter.exportStudentReportCardPdf(
                                                    context = context,
                                                    student = myStu,
                                                    grades = allSubjectsReport,
                                                    schoolConfig = schoolConfig
                                                )
                                                viewModel.showBannerMessage(msg)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_export_official_report_card_pdf"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تصدير / طباعة بطاقة الدرجات الرسمية (PDF)")
                                        }
                                    } else {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("report_card_pending_principal_approval_card"),
                                            shape = RoundedCornerShape(14.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = "كارت الدرجات الرسمي قيد المراجعة والاعتماد",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF065F46),
                                                    textAlign = TextAlign.Center
                                                )
                                                Text(
                                                    text = "سيظهر كارت النتائج النهائي هنا فور إصداره وإرساله رسمياً من قِبل مدير المدرسة.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF475569),
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        items(allSubjectsReport, key = { it.subjectName }) { g ->
                            val statusColor = when {
                                !g.isAllExamsCompleted -> DeepEmerald
                                g.normalizedPercentage >= 50 -> SuccessGreen
                                else -> DangerRed
                            }
                            val statusText = when {
                                !g.isAllExamsCompleted -> "مستمر (${g.ratingLabel})"
                                g.normalizedPercentage >= 50 -> "ناجح (${g.ratingLabel})"
                                else -> "راسب"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // شريط علوي زمردي هادئ يعرض ملخص المادة والسعي والمعدل في سطرين مريحين بدون أي التفاف عمودي
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = DeepEmerald.copy(alpha = 0.09f),
                                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.24f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                                        imageVector = Icons.Filled.MenuBook,
                                                        contentDescription = null,
                                                        tint = DeepEmerald,
                                                        modifier = Modifier.size(19.dp)
                                                    )
                                                    Text(
                                                        text = "المادة: ${g.subjectName} (الدرجة من ${g.maxScoreScale})",
                                                        style = MaterialTheme.typography.titleSmall,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = DeepForestGreen,
                                                        maxLines = 1,
                                                        softWrap = false,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                                NoWrapBadge(
                                                    text = statusText,
                                                    icon = if (!g.isAllExamsCompleted) Icons.Filled.TrendingUp else Icons.Filled.Star,
                                                    containerColor = statusColor.copy(alpha = 0.15f),
                                                    contentColor = statusColor
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "السعي الفصلي: ${g.annualEffortScore} / ${g.maxScoreScale}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = DeepForestGreen,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "التقدير/الحالة: ${if (g.isAllExamsCompleted) "${g.finalAverage} / ${g.maxScoreScale}" else "قيد الرصد"}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = DeepEmerald,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // شبكة الدرجات النظيفة (Clean Grades Grid 2x3) مع عناوين مستقلة فوق كل خانة
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ScoreCell("يومي 1", g.dailyScore, Modifier.weight(1f), maxScale = g.maxScoreScale)
                                            ScoreCell("شهر 1", g.month1Score, Modifier.weight(1f), maxScale = g.maxScoreScale)
                                            ScoreCell("شهر 2", g.month2Score, Modifier.weight(1f), maxScale = g.maxScoreScale)
                                        }
                                        if (g.extraDailyScores.isNotEmpty() || g.extraMonthlyScores.isNotEmpty()) {
                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                items(g.extraDailyScores.size) { idx ->
                                                    ScoreCell(
                                                        label = "يومي ${idx + 2}",
                                                        score = g.extraDailyScores[idx],
                                                        modifier = Modifier.width(96.dp),
                                                        maxScale = g.maxScoreScale
                                                    )
                                                }
                                                items(g.extraMonthlyScores.size) { idx ->
                                                    ScoreCell(
                                                        label = "شهر ${idx + 3}",
                                                        score = g.extraMonthlyScores[idx],
                                                        modifier = Modifier.width(96.dp),
                                                        maxScale = g.maxScoreScale
                                                    )
                                                }
                                            }
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            ScoreCell("نصف السنة", g.midYearScore, Modifier.weight(1f), maxScale = g.maxScoreScale)
                                            ScoreCell("النهائي", g.finalExamScore, Modifier.weight(1f), maxScale = g.maxScoreScale)
                                            ScoreCell("السعي النهائي", g.annualEffortScore, Modifier.weight(1f), highlight = true, maxScale = g.maxScoreScale)
                                        }
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
                val allTeacherScopedStudents = remember(students, currentUser) {
                    viewModel.getScopedStudentsForUser(currentUser)
                }
                val classSectionStudents = remember(allTeacherScopedStudents, selectedClass, selectedSection, searchStudentName) {
                    allTeacherScopedStudents.filter { stu ->
                        ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                            ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection) &&
                            (searchStudentName.isBlank() ||
                                stu.studentName.contains(searchStudentName, ignoreCase = true) ||
                                stu.fullNameWithFather.contains(searchStudentName, ignoreCase = true))
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    item(key = "exams_top_tabs_staff_0") {
                        renderTopTabsHeader()
                    }

                    // 3D Wave Hero Header for Grades & Exams
                    item {
                        ThreeDHeroHeaderCard(
                            title = "الامتحانات ورصد الدرجات",
                            subtitle = "رصد الدرجات الشهرية والفصلية وحساب السعي والمعدل النهائي آلياً",
                            rightPillText = "$selectedClass • شعبة $selectedSection",
                            rightPillIcon = Icons.Filled.Class,
                            leftPillText = "طلاب الصف: ${classSectionStudents.size}",
                            leftPillIcon = Icons.Filled.Groups,
                            illustrationType = ThreeDIllustrationType.EXAM_CERTIFICATE_PEN
                        )
                    }

                    // Step A & B Filter Card (Removed overlapping green badge next to title)
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(8.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "أولاً: اختر الصف والشعبة والمادة الدراسية",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // 1. Registered Class Selector (Pure-Text Chips)
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
                                            label = {
                                                Text(
                                                    text = cls,
                                                    fontWeight = if (selectedClass == cls) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        )
                                    }
                                }

                                // 2. Registered Section Selector (LazyRow so "شعبة ج" never clips)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "الشعبة:",
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(allowedSections) { sec ->
                                            FilterChip(
                                                selected = selectedSection == sec,
                                                onClick = { selectedSection = sec },
                                                label = {
                                                    Text(
                                                        text = "شعبة $sec",
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            )
                                        }
                                    }
                                }

                                // 3. Subject Selector (المادة الدراسية المخصصة للصف المختار - Pure-Text Chips)
                                Text(
                                    text = "المادة الدراسية المختارة للرصد (${allowedSubjects.size}):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(allowedSubjects) { subj ->
                                        FilterChip(
                                            selected = selectedSubject == subj,
                                            onClick = { selectedSubject = subj },
                                            label = {
                                                Text(
                                                    text = subj,
                                                    fontWeight = if (selectedSubject == subj) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        )
                                    }
                                }

                                // 3.B Flexible Grading Scale Toggle (نظام الدرجة: من 10 / من 100) — Pure-Text Glassmorphic Capsules
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "نظام الدرجة:",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF065F46),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selectedMaxScale == 10) Color(0xFF059669) else Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selectedMaxScale == 10) Color(0xFF047857) else Color(0xFFA7F3D0)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        selectedMaxScale = 10
                                                        viewModel.updateClassSubjectMaxScoreScale(
                                                            gradeClass = selectedClass,
                                                            sectionName = selectedSection,
                                                            subjectName = selectedSubject,
                                                            newScale = 10
                                                        )
                                                    }
                                                    .testTag("scale_toggle_10")
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "من 10",
                                                        fontSize = 11.8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (selectedMaxScale == 10) Color.White else Color(0xFF065F46),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (selectedMaxScale == 100) Color(0xFF059669) else Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (selectedMaxScale == 100) Color(0xFF047857) else Color(0xFFA7F3D0)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        selectedMaxScale = 100
                                                        viewModel.updateClassSubjectMaxScoreScale(
                                                            gradeClass = selectedClass,
                                                            sectionName = selectedSection,
                                                            subjectName = selectedSubject,
                                                            newScale = 100
                                                        )
                                                    }
                                                    .testTag("scale_toggle_100")
                                            ) {
                                                Box(
                                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "من 100",
                                                        fontSize = 11.8.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (selectedMaxScale == 100) Color.White else Color(0xFF065F46),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // 4. Search by Student Name within the selected Class & Section (Pure-Text Search Field)
                                OutlinedTextField(
                                    value = searchStudentName,
                                    onValueChange = { searchStudentName = it },
                                    label = { Text("بحث بالاسم داخل طلاب ($selectedClass - شعبة $selectedSection)...") },
                                    trailingIcon = {
                                        if (searchStudentName.isNotEmpty()) {
                                            Surface(
                                                onClick = { searchStudentName = "" },
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFECFDF5),
                                                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.5f)),
                                                modifier = Modifier.padding(end = 8.dp)
                                            ) {
                                                Text(
                                                    text = "مسح",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF047857),
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("grades_student_search_bar"),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                // 5. Per-Period Principal Grade Approval & Student Completion Check Card
                                val periodSummary = remember(grades, students, selectedClass, selectedSection, selectedSubject, selectedApprovalPeriod) {
                                    viewModel.getPeriodCompletionSummary(
                                        gradeClass = selectedClass,
                                        sectionName = selectedSection,
                                        subjectName = selectedSubject,
                                        period = selectedApprovalPeriod
                                    )
                                }
                                val approvedPeriodsList = remember(grades, students, selectedClass, selectedSection, selectedSubject) {
                                    GradeAssessmentPeriod.entries.filter { p ->
                                        viewModel.isClassSubjectPeriodApproved(selectedClass, selectedSection, selectedSubject, p)
                                    }
                                }

                                if (currentUser?.role == UserRole.ADMIN) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (periodSummary.isPeriodApproved) Color(0xFFECFDF5) else Color(0xFFFFFBEB),
                                        border = BorderStroke(
                                            1.3.dp,
                                            if (periodSummary.isPeriodApproved) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = "نظام مصادقة المدير الذكية حسب الفترة الامتحانية:",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (periodSummary.isPeriodApproved) Color(0xFF065F46) else Color(0xFF92400E)
                                            )

                                            // Horizontal Period Selector Chips (Pure-Text without classic shield/verified icons)
                                            LazyRow(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                items(GradeAssessmentPeriod.entries) { pItem ->
                                                    val isSelectedPeriod = selectedApprovalPeriod == pItem
                                                    val isPApproved = pItem in approvedPeriodsList
                                                    FilterChip(
                                                        selected = isSelectedPeriod,
                                                        onClick = { selectedApprovalPeriod = pItem },
                                                        label = {
                                                            Text(
                                                                text = if (isPApproved) "${pItem.arabicTitle} (معتمد)" else pItem.arabicTitle,
                                                                fontWeight = if (isSelectedPeriod || isPApproved) FontWeight.ExtraBold else FontWeight.SemiBold,
                                                                maxLines = 1,
                                                                softWrap = false
                                                            )
                                                        }
                                                    )
                                                }
                                            }

                                            // Completion Counter & Unrecorded Students Warning (Horizontal unclipped capsule "0 من 1 طالب")
                                            Surface(
                                                shape = RoundedCornerShape(12.dp),
                                                color = Color.White,
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (periodSummary.isFullyRecorded) Color(0xFF6EE7B7) else Color(0xFFFDE68A)
                                                ),
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
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "اكتمال الرصد لفترة (${selectedApprovalPeriod.arabicTitle}):",
                                                            fontSize = 11.8.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF1E293B),
                                                            maxLines = 1,
                                                            softWrap = false,
                                                            modifier = Modifier.weight(1f, fill = false)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Surface(
                                                            shape = RoundedCornerShape(50),
                                                            color = if (periodSummary.isFullyRecorded) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                                            border = BorderStroke(
                                                                1.dp,
                                                                if (periodSummary.isFullyRecorded) Color(0xFF6EE7B7) else Color(0xFFFCD34D)
                                                            )
                                                        ) {
                                                            Text(
                                                                text = "${periodSummary.recordedStudentsCount} من ${periodSummary.totalStudentsCount} طالب",
                                                                fontSize = 11.8.sp,
                                                                fontWeight = FontWeight.ExtraBold,
                                                                color = if (periodSummary.isFullyRecorded) Color(0xFF065F46) else Color(0xFF92400E),
                                                                maxLines = 1,
                                                                softWrap = false,
                                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                            )
                                                        }
                                                    }

                                                    if (periodSummary.unrecordedCount > 0) {
                                                        Text(
                                                            text = "الطلبة غير المرصودة درجاتهم (${periodSummary.unrecordedCount}): ${periodSummary.unrecordedStudentNames.joinToString("، ")}",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = Color(0xFFB91C1C)
                                                        )
                                                    } else if (periodSummary.totalStudentsCount > 0) {
                                                        Text(
                                                            text = "اكتمل رصد درجات جميع طلبة الشعبة لهذه الفترة بالكامل",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF047857)
                                                        )
                                                    }
                                                }
                                            }

                                            // Full Unclipped Single-Line Principal Approval Capsule Button (Zero Icons, Zero Truncation)
                                            Button(
                                                onClick = {
                                                    if (periodSummary.isPeriodApproved) {
                                                        viewModel.setClassSubjectPeriodApproval(
                                                            gradeClass = selectedClass,
                                                            sectionName = selectedSection,
                                                            subjectName = selectedSubject,
                                                            period = selectedApprovalPeriod,
                                                            approved = false
                                                        )
                                                    } else if (periodSummary.unrecordedCount > 0) {
                                                        pendingOverrideApprovalSummary = periodSummary
                                                    } else {
                                                        viewModel.setClassSubjectPeriodApproval(
                                                            gradeClass = selectedClass,
                                                            sectionName = selectedSection,
                                                            subjectName = selectedSubject,
                                                            period = selectedApprovalPeriod,
                                                            approved = true
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (periodSummary.isPeriodApproved) Color(0xFFB45309) else Color(0xFF047857),
                                                    contentColor = Color.White
                                                ),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 11.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("btn_principal_approve_grades")
                                            ) {
                                                Text(
                                                    text = if (periodSummary.isPeriodApproved) {
                                                        "إلغاء مصادقة درجات (${selectedApprovalPeriod.arabicTitle})"
                                                    } else {
                                                        "مصادقة واعتماد درجات (${selectedApprovalPeriod.arabicTitle})"
                                                    },
                                                    fontWeight = FontWeight.ExtraBold,
                                                    fontSize = 13.sp,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                } else if (currentUser?.role == UserRole.TEACHER && approvedPeriodsList.isNotEmpty()) {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFFFBEB),
                                        border = BorderStroke(1.2.dp, Color(0xFFF59E0B))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = "الفترات المصادق عليها والمقفولة من الإدارة: ${approvedPeriodsList.joinToString("، ") { it.arabicTitle }} (بقية الفترات متاحة للرصد)",
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF92400E)
                                            )
                                        }
                                    }
                                }
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
                        items(classSectionStudents, key = { "${it.id}_${selectedSubject}_$selectedMaxScale" }) { stu ->
                            val existingGrade = grades.firstOrNull {
                                it.studentId == stu.id && it.subjectName == selectedSubject
                            }

                            var dailyText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.dailyScore ?: 0).coerceAtMost(selectedMaxScale).toString())
                            }
                            var extraDailyTexts: List<String> by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf<List<String>>(
                                    existingGrade?.extraDailyScores?.map { score ->
                                        score.coerceAtMost(selectedMaxScale).toString()
                                    } ?: emptyList()
                                )
                            }
                            var m1Text by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.month1Score ?: 0).coerceAtMost(selectedMaxScale).toString())
                            }
                            var m2Text by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.month2Score ?: 0).coerceAtMost(selectedMaxScale).toString())
                            }
                            var extraMonthlyTexts: List<String> by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf<List<String>>(
                                    existingGrade?.extraMonthlyScores?.map { score ->
                                        score.coerceAtMost(selectedMaxScale).toString()
                                    } ?: emptyList()
                                )
                            }
                            var midText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.midYearScore ?: 0).coerceAtMost(selectedMaxScale).toString())
                            }
                            var finalText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.finalExamScore ?: 0).coerceAtMost(selectedMaxScale).toString())
                            }
                            var savedToast by remember { mutableStateOf(false) }

                            val dVal = (dailyText.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val extraDailyVals = extraDailyTexts.map {
                                (it.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            }
                            val m1Val = (m1Text.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val m2Val = (m2Text.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val extraMonthlyVals = extraMonthlyTexts.map {
                                (it.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            }
                            val midVal = (midText.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val finVal = (finalText.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)

                            val previewGrade = SubjectGrade(
                                id = "prev",
                                schoolId = schoolConfig.schoolId,
                                studentId = stu.id,
                                studentName = stu.studentName,
                                gradeClass = stu.gradeClass,
                                subjectName = selectedSubject,
                                maxScoreScale = selectedMaxScale,
                                dailyScore = dVal,
                                extraDailyScores = extraDailyVals,
                                month1Score = m1Val,
                                month2Score = m2Val,
                                extraMonthlyScores = extraMonthlyVals,
                                midYearScore = midVal,
                                finalExamScore = finVal
                            )

                            val statusBadgeColor = when {
                                !previewGrade.isAllExamsCompleted -> DeepEmerald
                                previewGrade.normalizedPercentage >= 50 -> SuccessGreen
                                else -> DangerRed
                            }
                            val statusBadgeLabel = when {
                                !previewGrade.isAllExamsCompleted -> "الحالة: مستمر"
                                previewGrade.normalizedPercentage >= 50 -> "الحالة: ناجح (${previewGrade.ratingLabel})"
                                else -> "الحالة: راسب"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    // رأس بطاقة الطالب (الاسم في سطر كامل + الصف والشعبة في سطر مستقل كامل بدون كسر (أ) + كبسولة الحالة بدون أيقونات)
                                    val stuAutoBadges = viewModel.getStudentAutoBadges(stu.id)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        StudentAvatarWithOverlayBadge(
                                            base64Data = stu.photoBase64,
                                            contentDescription = stu.studentName,
                                            badge = stuAutoBadges.firstOrNull(),
                                            avatarSize = 54.dp,
                                            boxWidth = 62.dp,
                                            fallbackIcon = Icons.Filled.Person
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Text(
                                                text = stu.fullNameWithFather,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${stu.gradeClass} • شعبة (${stu.sectionName})",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = statusBadgeColor.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, statusBadgeColor.copy(alpha = 0.45f))
                                            ) {
                                                Text(
                                                    text = statusBadgeLabel,
                                                    fontSize = 11.2.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = statusBadgeColor,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }

                                    // شريط علوي زمردي هادئ يعرض ملخص الطالب (المادة، السعي الفصلي، والتقدير/الحالة) في سطرين مريحين بدون أي انكسار عمودي أو أيقونات كلاسيكية
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        color = DeepEmerald.copy(alpha = 0.09f),
                                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.24f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalArrangement = Arrangement.spacedBy(7.dp)
                                        ) {
                                            Text(
                                                text = "المادة: $selectedSubject (الدرجة من $selectedMaxScale)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "السعي الفصلي: ${previewGrade.annualEffortScore} / $selectedMaxScale",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = DeepForestGreen,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = Color.White,
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "التقدير/الحالة: ${if (previewGrade.isAllExamsCompleted) "${previewGrade.finalAverage} / $selectedMaxScale" else "قيد الرصد"}",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = DeepEmerald,
                                                            maxLines = 1,
                                                            softWrap = false
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Per-period lock checks for the current Teacher
                                    val isTeacherRole = currentUser?.role == UserRole.TEACHER
                                    fun isPeriodApprovedForStu(p: GradeAssessmentPeriod): Boolean {
                                        return existingGrade?.isPeriodApproved(p) == true ||
                                            viewModel.isClassSubjectPeriodApproved(stu.gradeClass, stu.sectionName, selectedSubject, p)
                                    }
                                    val isDailyLocked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.DAILY)
                                    val isMonth1Locked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.MONTH_1)
                                    val isMonth2Locked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.MONTH_2)
                                    val isMonth3Locked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.MONTH_3)
                                    val isMonth4Locked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.MONTH_4)
                                    val isMidYearLocked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.MID_YEAR)
                                    val isFinalLocked = isTeacherRole && isPeriodApprovedForStu(GradeAssessmentPeriod.FINAL_EXAM)
                                    val areAllPeriodsLockedForTeacher = isTeacherRole &&
                                        GradeAssessmentPeriod.entries.all { isPeriodApprovedForStu(it) }

                                    // Only display "مصادق عليها رسمياً" for periods that are BOTH approved AND have a recorded score (> 0)
                                    val studentApprovedAndRecordedPeriods = GradeAssessmentPeriod.entries.filter { p ->
                                        isPeriodApprovedForStu(p) && previewGrade.getScoreForPeriod(p) > 0
                                    }
                                    val studentLockedPeriods = GradeAssessmentPeriod.entries.filter { p ->
                                        isPeriodApprovedForStu(p)
                                    }

                                    if (studentApprovedAndRecordedPeriods.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFECFDF5),
                                            border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "مصادق عليها رسمياً: ${studentApprovedAndRecordedPeriods.joinToString(" • ") { it.arabicTitle }}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF065F46)
                                                )
                                            }
                                        }
                                    } else if (isTeacherRole && studentLockedPeriods.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFFFFFBEB),
                                            border = BorderStroke(1.dp, Color(0xFFFDE68A))
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                                            ) {
                                                Text(
                                                    text = "الفترات المقفولة من الإدارة: ${studentLockedPeriods.joinToString(" • ") { it.arabicTitle }}",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF92400E)
                                                )
                                            }
                                        }
                                    }

                                    // أزرار مرنة لإضافة أو إزالة خانات درجات يومية أو شهرية إضافية حسب الحاجة (كبسولات نصية صافية بدون أيقونات)
                                    if (!areAllPeriodsLockedForTeacher) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (!isDailyLocked) {
                                                OutlinedButton(
                                                    onClick = {
                                                        extraDailyTexts = extraDailyTexts + "0"
                                                        savedToast = false
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.5f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("+ يومي إضافي", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                                }
                                            }

                                            if (!isMonth3Locked || !isMonth4Locked) {
                                                OutlinedButton(
                                                    onClick = {
                                                        extraMonthlyTexts = extraMonthlyTexts + "0"
                                                        savedToast = false
                                                    },
                                                    shape = RoundedCornerShape(10.dp),
                                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.5f)),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Text("+ شهر إضافي", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = DeepEmerald)
                                                }
                                            }

                                            if (extraDailyTexts.isNotEmpty() || extraMonthlyTexts.isNotEmpty()) {
                                                TextButton(
                                                    onClick = {
                                                        if (extraMonthlyTexts.isNotEmpty()) {
                                                            extraMonthlyTexts = extraMonthlyTexts.dropLast(1)
                                                        } else if (extraDailyTexts.isNotEmpty()) {
                                                            extraDailyTexts = extraDailyTexts.dropLast(1)
                                                        }
                                                        savedToast = false
                                                    }
                                                ) {
                                                    Text("إزالة إضافي", style = MaterialTheme.typography.labelSmall, color = DangerRed)
                                                }
                                            }
                                        }
                                    }

                                    // شبكة الدرجات النظيفة (Clean Grades Grid 2x3 + Extra Dynamic Fields) بعناوين مستقلة فوق كل خانة لمنع أي تداخل نصي
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            CleanGradeInputBox(
                                                title = "يومي 1",
                                                maxScale = selectedMaxScale,
                                                value = dailyText,
                                                enabled = !isDailyLocked,
                                                onValueChange = { digits ->
                                                    dailyText = digits
                                                    savedToast = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            CleanGradeInputBox(
                                                title = "شهر 1",
                                                maxScale = selectedMaxScale,
                                                value = m1Text,
                                                enabled = !isMonth1Locked,
                                                onValueChange = { digits ->
                                                    m1Text = digits
                                                    savedToast = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            CleanGradeInputBox(
                                                title = "شهر 2",
                                                maxScale = selectedMaxScale,
                                                value = m2Text,
                                                enabled = !isMonth2Locked,
                                                onValueChange = { digits ->
                                                    m2Text = digits
                                                    savedToast = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                        }

                                        // خانات اليومي الإضافية (يومي 2، يومي 3...)
                                        if (extraDailyTexts.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                extraDailyTexts.forEachIndexed { idx, txtVal ->
                                                    CleanGradeInputBox(
                                                        title = "يومي ${idx + 2}",
                                                        maxScale = selectedMaxScale,
                                                        value = txtVal,
                                                        enabled = !isDailyLocked,
                                                        onValueChange = { digits ->
                                                            extraDailyTexts = extraDailyTexts.toMutableList().also {
                                                                it[idx] = digits
                                                            }
                                                            savedToast = false
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        // خانات الأشهر الإضافية (شهر 3، شهر 4...)
                                        if (extraMonthlyTexts.isNotEmpty()) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                extraMonthlyTexts.forEachIndexed { idx, txtVal ->
                                                    val isThisExtraMonthLocked = when (idx) {
                                                        0 -> isMonth3Locked
                                                        1 -> isMonth4Locked
                                                        else -> false
                                                    }
                                                    CleanGradeInputBox(
                                                        title = "شهر ${idx + 3}",
                                                        maxScale = selectedMaxScale,
                                                        value = txtVal,
                                                        enabled = !isThisExtraMonthLocked,
                                                        onValueChange = { digits ->
                                                            extraMonthlyTexts = extraMonthlyTexts.toMutableList().also {
                                                                it[idx] = digits
                                                            }
                                                            savedToast = false
                                                        },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            CleanGradeInputBox(
                                                title = "نصف السنة",
                                                maxScale = selectedMaxScale,
                                                value = midText,
                                                enabled = !isMidYearLocked,
                                                onValueChange = { digits ->
                                                    midText = digits
                                                    savedToast = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            CleanGradeInputBox(
                                                title = "النهائي",
                                                maxScale = selectedMaxScale,
                                                value = finalText,
                                                enabled = !isFinalLocked,
                                                onValueChange = { digits ->
                                                    finalText = digits
                                                    savedToast = false
                                                },
                                                modifier = Modifier.weight(1f)
                                            )
                                            ScoreCell(
                                                label = "السعي النهائي",
                                                score = previewGrade.annualEffortScore,
                                                modifier = Modifier.weight(1f),
                                                highlight = true,
                                                maxScale = selectedMaxScale
                                            )
                                        }
                                    }

                                    if (!areAllPeriodsLockedForTeacher) {
                                        Button(
                                            onClick = {
                                                val ok = viewModel.saveOrUpdateSubjectGrade(
                                                    student = stu,
                                                    subjectName = selectedSubject,
                                                    dailyScore = dVal,
                                                    month1Score = m1Val,
                                                    month2Score = m2Val,
                                                    midYearScore = midVal,
                                                    finalExamScore = finVal,
                                                    maxScoreScale = selectedMaxScale,
                                                    extraDailyScores = extraDailyVals,
                                                    extraMonthlyScores = extraMonthlyVals
                                                )
                                                if (ok) savedToast = true
                                            },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (savedToast) SuccessGreen else MaterialTheme.colorScheme.primary,
                                                contentColor = Color.White
                                            ),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text(
                                                text = if (savedToast) "تم حفظ درجات ($selectedSubject من $selectedMaxScale) للطالب بنجاح" else "حفظ درجات ($selectedSubject من $selectedMaxScale) للطالب",
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }

                                    if (currentUser?.role == UserRole.ADMIN) {
                                        OutlinedButton(
                                            onClick = {
                                                val stuGrades = grades.filter { it.studentId == stu.id }
                                                val msg = DocumentAndIdExporter.exportStudentReportCardPdf(
                                                    context = context,
                                                    student = stu,
                                                    grades = stuGrades,
                                                    schoolConfig = schoolConfig
                                                )
                                                viewModel.showBannerMessage(msg)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("export_report_card_${stu.id}"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = "تصدير بطاقة الدرجات (الشهادة المدرسية PDF)",
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
        } else if (selectedTab == 1) {
            // Exam Schedule Tab with Live Countdown Badge
            val scopedExams = remember(examSchedules, allowedClasses) {
                examSchedules.filter { ex ->
                    allowedClasses.any { cls -> ArabicTextNormalizer.matchesClass(ex.gradeClass, cls) }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 110.dp)
            ) {
                item(key = "exams_top_tabs_schedule_1") {
                    renderTopTabsHeader()
                }

                item {
                    ThreeDHeroHeaderCard(
                        title = "مواعيد الامتحانات الرسمية",
                        subtitle = "جدول مواعيد الامتحانات الشهرية ونصف السنة وآخر السنة مع العد التنازلي",
                        rightPillText = "الامتحانات المجدولة: ${scopedExams.size}",
                        rightPillIcon = Icons.Filled.EventNote,
                        leftPillText = schoolConfig.academicYear,
                        leftPillIcon = Icons.Filled.CalendarMonth,
                        illustrationType = ThreeDIllustrationType.EXAM_CERTIFICATE_PEN
                    )
                }

                if (canEdit) {
                    item {
                        VectorActionButton(
                            text = "إضافة موعد امتحان جديد للجدول",
                            icon = Icons.Filled.AddCard,
                            onClick = { showAddExamDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                items(scopedExams, key = { it.id }) { ex ->
                    val daysRemaining = ex.daysRemainingUntilExam()
                    val cleanExamIsoDate = ArabicTextNormalizer.normalizeDateIso(ex.examDate.trim())
                    val examScale = viewModel.getDefaultMaxScoreForClassAndSubject(ex.gradeClass, ex.subjectName)
                    val formattedExamDateLabel = listOf(ex.dayName.trim(), cleanExamIsoDate)
                        .filter { it.isNotEmpty() }
                        .joinToString(" • ")
                        .ifBlank { cleanExamIsoDate }
                    val countdownText = when {
                        daysRemaining == null -> formattedExamDateLabel
                        daysRemaining > 0L -> "قادم (بعد $daysRemaining يوم)"
                        daysRemaining == 0L -> "اليوم"
                        else -> "منتهي"
                    }
                    val countdownColor = when {
                        daysRemaining == null || daysRemaining > 0L -> DeepEmerald
                        daysRemaining == 0L -> DangerRed
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }

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
                                    text = "${ex.subjectName} • ${ex.gradeClass} (الدرجة من $examScale)",
                                    icon = Icons.Filled.School,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (daysRemaining != null) {
                                        NoWrapBadge(
                                            text = countdownText,
                                            icon = Icons.Filled.Timer,
                                            containerColor = countdownColor.copy(alpha = 0.16f),
                                            contentColor = countdownColor
                                        )
                                    }
                                    NoWrapBadge(
                                        text = formattedExamDateLabel,
                                        icon = Icons.Filled.Event,
                                        containerColor = GoldAccent.copy(alpha = 0.2f),
                                        contentColor = GoldDeep
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = ex.examType,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (ex.timeSlot.isNotBlank()) {
                                    NoWrapBadge(
                                        text = "الوقت: ${ex.timeSlot}",
                                        icon = Icons.Filled.Schedule,
                                        containerColor = DeepEmerald.copy(alpha = 0.12f),
                                        contentColor = DeepEmerald
                                    )
                                }
                            }
                            Text(
                                text = "المادة الداخلة في الامتحان: ${ex.topicsIncluded}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (canEdit) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = { editingExamSchedule = ex }
                                    ) {
                                        Icon(
                                            Icons.Filled.Edit,
                                            contentDescription = "تعديل موعد الامتحان",
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تعديل", color = DeepEmerald, fontWeight = FontWeight.Bold)
                                    }
                                    TextButton(
                                        onClick = { viewModel.deleteExamSchedule(ex.id) }
                                    ) {
                                        Icon(
                                            Icons.Filled.DeleteOutline,
                                            contentDescription = "حذف موعد الامتحان",
                                            tint = DangerRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("حذف", color = DangerRed, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // =========================================================================
            // TAB 2: SMART STUDENT REPORT CARDS & INTERACTIVE CELEBRATION HUB
            // (إصدار الكارتات والنتائج)
            // =========================================================================
            if (isParentOrStudent) {
                val myStu = viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
                val isReportCardSent = myStu?.isReportCardSentByPrincipal == true
                val myIssuedCards = remember(issuedReportCards, myStu?.id, isReportCardSent) {
                    if (isReportCardSent) {
                        issuedReportCards.filter { it.studentId == myStu?.id }
                    } else {
                        emptyList()
                    }
                }
                val livePreviewCard = remember(myStu, grades, selectedReportCardPeriod, isReportCardSent) {
                    if (isReportCardSent && myStu != null) {
                        viewModel.buildStudentReportCardPreview(myStu, selectedReportCardPeriod)
                    } else {
                        null
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    item(key = "exams_top_tabs_parent_2") {
                        renderTopTabsHeader()
                    }

                    item {
                        ThreeDHeroHeaderCard(
                            title = "الكارت المدرسي الذكي والنتيجة الرسمية",
                            subtitle = "استعراض الشهادات والكارتات المدرسية المعتمدة من إدارة المدرسة مع الطباعة الفورية",
                            rightPillText = myStu?.fullNameWithFather ?: "الطالب",
                            rightPillIcon = Icons.Filled.School,
                            leftPillText = "${myStu?.gradeClass.orEmpty()} (${myStu?.sectionName.orEmpty()})",
                            leftPillIcon = Icons.Filled.WorkspacePremium,
                            illustrationType = ThreeDIllustrationType.EXAM_CERTIFICATE_PEN
                        )
                    }

                    if (!isReportCardSent) {
                        item(key = "student_report_card_pending_notice") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("student_report_card_pending_notice_card"),
                                shape = RoundedCornerShape(22.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.5.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFECFDF5).copy(alpha = 0.85f),
                                                    Color.White
                                                )
                                            )
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 20.dp, vertical = 28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFD1FAE5),
                                            border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                                            modifier = Modifier.size(64.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.WorkspacePremium,
                                                    contentDescription = null,
                                                    tint = Color(0xFF047857),
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "كارت الدرجات الرسمي قيد المراجعة والاعتماد",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF064E3B),
                                            textAlign = TextAlign.Center
                                        )

                                        Text(
                                            text = "سيظهر كارت النتائج النهائي هنا فور إصداره وإرساله رسمياً من قِبل مدير المدرسة.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF475569),
                                            textAlign = TextAlign.Center,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        if (myIssuedCards.isNotEmpty()) {
                            item {
                                Text(
                                    text = "الكارتات المدرسية الرسمية الصادرة من إدارة المدرسة (${myIssuedCards.size}):",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
                                )
                            }
                            items(myIssuedCards, key = { it.id }) { card ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.openCelebrationReportCardModal(card) },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (card.isOverallPassed) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                                    ),
                                    border = BorderStroke(
                                        1.5.dp,
                                        if (card.isOverallPassed) Color(0xFF10B981) else Color(0xFFF59E0B)
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = card.periodType.arabicTitle,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color(0xFF064E3B)
                                                )
                                                Text(
                                                    text = "تاريخ الإصدار: ${card.issuedAt} • مصادقة: ${card.principalName}",
                                                    fontSize = 11.5.sp,
                                                    color = Color(0xFF475569)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (card.isOverallPassed) Color(0xFF059669) else Color(0xFFD97706)
                                            ) {
                                                Text(
                                                    text = if (card.isOverallPassed) "ناجح (${card.averagePercent}%)" else "مكمل / متابعة (${card.averagePercent}%)",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { viewModel.openCelebrationReportCardModal(card) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF047857),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.WorkspacePremium, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "فتح الكارت المدرسي الذكي والنتيجة الكاملة",
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (livePreviewCard != null && myStu != null) {
                            item {
                                val studentSubjects = viewModel.getSubjectsForClass(myStu.gradeClass, null)
                                val myGrades = grades.filter { it.studentId == myStu.id }
                                val allSubjectsReport = studentSubjects.map { subjName ->
                                    val dynamicScale = viewModel.getDefaultMaxScoreForClassAndSubject(myStu.gradeClass, subjName)
                                    myGrades.firstOrNull { it.subjectName == subjName } ?: SubjectGrade(
                                        id = "temp_$subjName",
                                        schoolId = schoolConfig.schoolId,
                                        studentId = myStu.id,
                                        studentName = myStu.studentName,
                                        gradeClass = myStu.gradeClass,
                                        subjectName = subjName,
                                        maxScoreScale = dynamicScale,
                                        dailyScore = 0,
                                        month1Score = 0,
                                        month2Score = 0,
                                        midYearScore = 0,
                                        finalExamScore = 0
                                    )
                                }
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "معاينة الكارت المدرسي المعتمد حسب الفصل:",
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF065F46)
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(ReportCardPeriodType.entries) { pType ->
                                                FilterChip(
                                                    selected = selectedReportCardPeriod == pType,
                                                    onClick = { selectedReportCardPeriod = pType },
                                                    label = { Text(pType.arabicTitle, maxLines = 1, softWrap = false) }
                                                )
                                            }
                                        }
                                        Button(
                                            onClick = { viewModel.openCelebrationReportCardModal(livePreviewCard) },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = Color(0xFF059669),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "استعراض (${selectedReportCardPeriod.arabicTitle}) بتصميم الشهادة الفاخرة",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 12.sp,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = {
                                                val msg = DocumentAndIdExporter.exportStudentReportCardPdf(
                                                    context = context,
                                                    student = myStu,
                                                    grades = allSubjectsReport,
                                                    schoolConfig = schoolConfig
                                                )
                                                viewModel.showBannerMessage(msg)
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("btn_export_official_report_card_pdf_tab2"),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تصدير / طباعة بطاقة الدرجات الرسمية (PDF)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Principal & Teacher Smart Report Card Issuance Hub
                val allScopedStudents = remember(students, currentUser) {
                    viewModel.getScopedStudentsForUser(currentUser)
                }
                val hubClassStudents = remember(allScopedStudents, selectedClass, selectedSection) {
                    allScopedStudents.filter { stu ->
                        ArabicTextNormalizer.matchesClass(stu.gradeClass, selectedClass) &&
                            ArabicTextNormalizer.matchesSection(stu.sectionName, selectedSection)
                    }
                }
                val studentPreviewCards = remember(hubClassStudents, grades, selectedReportCardPeriod, schoolConfig) {
                    hubClassStudents.map { stu ->
                        stu to viewModel.buildStudentReportCardPreview(stu, selectedReportCardPeriod)
                    }
                }
                val totalClassStudentsCount = studentPreviewCards.size
                val passedStudentsCount = studentPreviewCards.count { it.second.isOverallPassed }
                val failedStudentsCount = (totalClassStudentsCount - passedStudentsCount).coerceAtLeast(0)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    item(key = "exams_top_tabs_staff_2") {
                        renderTopTabsHeader()
                    }

                    item {
                        ThreeDHeroHeaderCard(
                            title = "مركز إصدار وإرسال الكارتات المدرسية الذكية",
                            subtitle = "إصدار الشهادات الفصلية والسنوية وحساب النجاح والرسوب آلياً وإرسالها لأولياء الأمور",
                            rightPillText = "$selectedClass • شعبة $selectedSection",
                            rightPillIcon = Icons.Filled.WorkspacePremium,
                            leftPillText = selectedReportCardPeriod.shortLabel,
                            leftPillIcon = Icons.Filled.Verified,
                            illustrationType = ThreeDIllustrationType.EXAM_CERTIFICATE_PEN
                        )
                    }

                    // Selectors: Class, Section, Report Card Type
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(22.dp)),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = "1. اختر الصف والشعبة ونوع الكارت المراد إصداره:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857)
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
                                            label = { Text(cls, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text("الشعبة:", style = MaterialTheme.typography.labelLarge, maxLines = 1, softWrap = false)
                                    LazyRow(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        items(allowedSections) { sec ->
                                            FilterChip(
                                                selected = selectedSection == sec,
                                                onClick = { selectedSection = sec },
                                                label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = "نوع الكارت المدرسي الرسمي:",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF065F46)
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(ReportCardPeriodType.entries) { pType ->
                                        FilterChip(
                                            selected = selectedReportCardPeriod == pType,
                                            onClick = { selectedReportCardPeriod = pType },
                                            label = { Text(pType.arabicTitle, maxLines = 1, softWrap = false) }
                                        )
                                    }
                                }

                                // 3 Top KPI Cards for Class Summary: إجمالي طلاب الصف | عدد الناجحين | الراسبين / المكملين
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFEFF6FF),
                                        border = BorderStroke(1.dp, Color(0xFF93C5FD))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$totalClassStudentsCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1D4ED8)
                                            )
                                            Text(
                                                text = "إجمالي طلاب الصف",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E3A8A),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFECFDF5),
                                        border = BorderStroke(1.dp, Color(0xFF6EE7B7))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$passedStudentsCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF047857)
                                            )
                                            Text(
                                                text = "عدد الناجحين",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF065F46),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                    Surface(
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFFEF2F2),
                                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                text = "$failedStudentsCount",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFFB91C1C)
                                            )
                                            Text(
                                                text = "الراسبين / المكملين",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF991B1B),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }

                                // Bulk Issue Button for Principal
                                if (currentUser?.role == UserRole.ADMIN) {
                                    Button(
                                        onClick = {
                                            viewModel.issueAndSendClassReportCardsBulk(
                                                gradeClass = selectedClass,
                                                sectionName = selectedSection,
                                                periodType = selectedReportCardPeriod
                                            )
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF047857),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(min = 48.dp)
                                            .testTag("btn_bulk_issue_report_cards")
                                    ) {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "إصدار وإرسال الكارتات لجميع طلاب الصف دفعة واحدة",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Student Cards List with Pass/Fail status, Preview & Send to Parent
                    items(studentPreviewCards, key = { "${it.first.id}_${selectedReportCardPeriod.name}" }) { (stu, previewCard) ->
                        val alreadyIssued = stu.isReportCardSentByPrincipal || issuedReportCards.any {
                            it.studentId == stu.id && it.periodType == selectedReportCardPeriod
                        }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = BorderStroke(
                                1.2.dp,
                                if (previewCard.isOverallPassed) Color(0xFF6EE7B7) else Color(0xFFFCA5A5)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stu.fullNameWithFather,
                                            fontSize = 14.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "${stu.gradeClass} (${stu.sectionName}) • المعدل: ${previewCard.averagePercent}% • المجموع: ${previewCard.totalScore}",
                                            fontSize = 11.5.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(50),
                                        color = if (previewCard.isOverallPassed) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                                        border = BorderStroke(
                                            1.dp,
                                            if (previewCard.isOverallPassed) Color(0xFF10B981) else Color(0xFFF87171)
                                        )
                                    ) {
                                        Text(
                                            text = if (previewCard.isOverallPassed) {
                                                "ناجح"
                                            } else {
                                                "مكمل / راسب (${previewCard.failedSubjectsCount} مواد)"
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (previewCard.isOverallPassed) Color(0xFF047857) else Color(0xFFB91C1C),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }

                                // Subject Mini Pills (Red for failed subjects <50 or <5, Green for passed)
                                if (previewCard.subjects.isNotEmpty()) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        items(previewCard.subjects) { subItem ->
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (subItem.isPassed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (subItem.isPassed) Color(0xFFA7F3D0) else Color(0xFFFECACA)
                                                )
                                            ) {
                                                Text(
                                                    text = "${subItem.subjectName}: ${subItem.score} / ${subItem.maxScoreScale}",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (subItem.isPassed) Color(0xFF065F46) else Color(0xFFB91C1C),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { viewModel.openCelebrationReportCardModal(previewCard) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.2.dp, Color(0xFF059669)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(0.95f)
                                    ) {
                                        Icon(Icons.Filled.Visibility, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "معاينة الكارت",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color(0xFF047857),
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }

                                    if (currentUser?.role == UserRole.ADMIN) {
                                        Button(
                                            onClick = {
                                                viewModel.issueAndSendStudentReportCard(stu, selectedReportCardPeriod)
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (alreadyIssued) Color(0xFF047857) else Color(0xFFD97706),
                                                contentColor = Color.White
                                            ),
                                            modifier = Modifier.weight(1.25f)
                                        ) {
                                            Icon(
                                                imageVector = if (alreadyIssued) Icons.Filled.CheckCircle else Icons.AutoMirrored.Filled.Send,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = if (alreadyIssued) "تم الإرسال (تحديث)" else "إرسال الكارت لولي الأمر",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }

                                        if (alreadyIssued) {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.revokeStudentReportCard(stu, selectedReportCardPeriod)
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                border = BorderStroke(1.2.dp, DangerRed),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                                modifier = Modifier.weight(0.85f)
                                            ) {
                                                Text(
                                                    text = "إلغاء الإرسال",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DangerRed,
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

    if (showAddExamDialog || editingExamSchedule != null) {
        val currentEditing = editingExamSchedule
        var cls by remember(currentEditing, allowedClasses) {
            mutableStateOf(currentEditing?.gradeClass ?: allowedClasses.firstOrNull() ?: "الأول المتوسط")
        }
        var subject by remember(currentEditing, allowedSubjects) {
            mutableStateOf(currentEditing?.subjectName ?: allowedSubjects.firstOrNull() ?: "الرياضيات")
        }
        var examType by remember(currentEditing) {
            mutableStateOf(currentEditing?.examType ?: "امتحان الشهر الثاني")
        }
        var date by remember(currentEditing) {
            mutableStateOf(currentEditing?.examDate ?: ArabicCalendarDateHelper.todayIsoDate())
        }
        var dayName by remember(currentEditing, date) {
            mutableStateOf(currentEditing?.dayName?.ifBlank { ArabicCalendarDateHelper.arabicDayOfWeek(date) } ?: ArabicCalendarDateHelper.arabicDayOfWeek(date))
        }
        var timeSlot by remember(currentEditing) {
            mutableStateOf(currentEditing?.timeSlot ?: "")
        }
        var topics by remember(currentEditing) {
            mutableStateOf(currentEditing?.topicsIncluded ?: "الفصل الأول والثاني")
        }
        val presetExamTimes = remember {
            listOf("08:00 صباحاً", "08:30 صباحاً", "09:00 صباحاً", "10:00 صباحاً", "11:00 صباحاً")
        }

        AlertDialog(
            onDismissRequest = {
                showAddExamDialog = false
                editingExamSchedule = null
            },
            title = {
                Text(if (currentEditing != null) "تعديل موعد الامتحان" else "إضافة موعد امتحان جديد")
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    OutlinedTextField(
                        value = examType,
                        onValueChange = { examType = it },
                        label = { Text("نوع الامتحان (الشهر الأول / الشهر الثاني / نصف السنة...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = date,
                        label = "تاريخ ويوم الامتحان",
                        onDateSelected = { selectedIso, selectedArabicDay ->
                            date = selectedIso
                            dayName = selectedArabicDay
                        }
                    )
                    OutlinedTextField(
                        value = timeSlot,
                        onValueChange = { timeSlot = it },
                        label = { Text("وقت الامتحان (اختياري — مثلاً: 08:30 صباحاً)") },
                        leadingIcon = { Icon(Icons.Filled.Schedule, contentDescription = null, tint = DeepEmerald) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(presetExamTimes) { tOpt ->
                            val isSelected = timeSlot == tOpt
                            FilterChip(
                                selected = isSelected,
                                onClick = { timeSlot = if (isSelected) "" else tOpt },
                                label = { Text(tOpt, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = topics,
                        onValueChange = { topics = it },
                        label = { Text("المادة الداخلة بالامتحان") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = if (currentEditing != null) "حفظ التعديلات" else "حفظ وإرسال إشعار",
                    icon = Icons.Filled.NotificationsActive,
                    onClick = {
                        if (currentEditing != null) {
                            viewModel.updateExamSchedule(
                                examId = currentEditing.id,
                                gradeClass = cls,
                                subjectName = subject,
                                examType = examType,
                                examDate = date,
                                dayName = dayName,
                                topicsIncluded = topics,
                                timeSlot = timeSlot
                            )
                        } else {
                            viewModel.addExamSchedule(
                                gradeClass = cls,
                                subjectName = subject,
                                examType = examType,
                                examDate = date,
                                dayName = dayName,
                                topicsIncluded = topics,
                                timeSlot = timeSlot
                            )
                        }
                        showAddExamDialog = false
                        editingExamSchedule = null
                    }
                )
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddExamDialog = false
                        editingExamSchedule = null
                    }
                ) { Text("إلغاء") }
            }
        )
    }
}

@Composable
fun ParentIssuedReportCardArrivalBanner(
    reportCard: IssuedStudentReportCard,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onDismissFromHome: (() -> Unit)? = null
) {
    val infiniteTransition = rememberInfiniteTransition(label = "report_card_banner_pulse")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("banner_parent_issued_report_card"),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF064E3B),
        border = BorderStroke(2.dp, Color(0xFFF59E0B).copy(alpha = glowAlpha))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF065F46))
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFF59E0B).copy(alpha = 0.24f),
                    border = BorderStroke(1.2.dp, Color(0xFFFDE68A)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFDE68A),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "وصل الآن الكارت المدرسي الرسمي للطالب (${reportCard.studentName}) — اضغط لفتح النتيجة",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFFFDE68A)
                    )
                    Text(
                        text = "${reportCard.periodType.arabicTitle} • الصف: ${reportCard.gradeClass} (${reportCard.sectionName}) • المعدل: ${reportCard.averagePercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = null,
                    tint = Color(0xFFFDE68A),
                    modifier = Modifier.size(16.dp)
                )
            }

            if (onDismissFromHome != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.55f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onDismissFromHome() }
                            .testTag("btn_dismiss_report_card_from_home")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VisibilityOff,
                                contentDescription = "إخفاء من الرئيسية",
                                tint = Color(0xFFFDE68A),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "إخفاء من الرئيسية",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFDE68A),
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

@Composable
fun LuxuryInteractiveReportCardModal(
    reportCard: IssuedStudentReportCard,
    schoolConfig: SchoolConfig,
    onDismiss: () -> Unit,
    onPrintPdf: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "celebration_confetti_anim")
    val confettiProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confettiProgress"
    )
    val fireworkPulse by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fireworkPulse"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xD9022C22))
                .padding(horizontal = 14.dp, vertical = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            // Full-Screen Canvas Confetti, Golden Stars & Fireworks Animation when student passed all subjects!
            if (reportCard.isOverallPassed) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val palette = listOf(
                        Color(0xFFF59E0B),
                        Color(0xFFFDE68A),
                        Color(0xFF10B981),
                        Color(0xFF38BDF8),
                        Color(0xFFF43F5E),
                        Color(0xFFA855F7)
                    )
                    // 42 falling confetti ribbons & golden stars
                    for (i in 0 until 42) {
                        val seedX = ((i * 97 + 19) % 100) / 100f
                        val speedFactor = 0.6f + (i % 5) * 0.18f
                        val yNorm = ((confettiProgress * speedFactor + (i * 0.07f)) % 1f)
                        val x = seedX * w + kotlin.math.sin((yNorm * 6.28f) + i) * 24f
                        val y = yNorm * h
                        val color = palette[i % palette.size]
                        if (i % 3 == 0) {
                            drawCircle(
                                color = Color(0xFFFDE68A),
                                radius = 5.5f + (i % 4),
                                center = Offset(x, y)
                            )
                        } else {
                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, y),
                                size = Size(12f, 6.5f),
                                cornerRadius = CornerRadius(3f, 3f)
                            )
                        }
                    }

                    // 3 Firework bursts in the upper sky
                    val fireworkCenters = listOf(
                        Offset(w * 0.2f, h * 0.14f),
                        Offset(w * 0.5f, h * 0.09f),
                        Offset(w * 0.8f, h * 0.15f)
                    )
                    fireworkCenters.forEachIndexed { fIdx, center ->
                        val maxRadius = 56f * fireworkPulse
                        for (ray in 0 until 12) {
                            val angle = (ray * 30.0) * (Math.PI / 180.0)
                            val endX = center.x + (kotlin.math.cos(angle) * maxRadius).toFloat()
                            val endY = center.y + (kotlin.math.sin(angle) * maxRadius).toFloat()
                            val rayColor = palette[(fIdx + ray) % palette.size]
                            drawLine(
                                color = rayColor.copy(alpha = 1f - (fireworkPulse * 0.35f)),
                                start = center,
                                end = Offset(endX, endY),
                                strokeWidth = 2.6f,
                                cap = StrokeCap.Round
                            )
                            drawCircle(
                                color = Color(0xFFFDE68A),
                                radius = 3.5f,
                                center = Offset(endX, endY)
                            )
                        }
                    }
                }
            }

            // Luxury 3D Glass Report Card Container
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 540.dp)
                    .heightIn(max = 760.dp)
                    .shadow(24.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = BorderStroke(
                    2.2.dp,
                    if (reportCard.isOverallPassed) Color(0xFFF59E0B) else Color(0xFF94A3B8)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Celebration or Supportive Educational Banner
                    if (reportCard.isOverallPassed) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.6.dp, Color(0xFFF59E0B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "ألف مبارك النجاح والتفوق للطالب البطل (${reportCard.studentName})",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFDE68A),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "تهنئ إدارة (${reportCard.schoolName.ifBlank { schoolConfig.schoolName }}) الطالب وأسرته الكريمة بهذا التفوق الباهر",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.5.dp, Color(0xFFF59E0B))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "رسالة تربوية تشجيعية لولي أمر الطالب (${reportCard.studentName})",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF92400E),
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "نثق بقدرات الطالب وهمته العالية، ونأمل متابعة المواد المؤشرة باللون الكهرماني/الأحمر (${reportCard.failedSubjectsCount} مواد) لتحقيق التفوق في الامتحانات القادمة بإذن الله.",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF78350F),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Official School Header inside Report Card
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFFD97706).copy(alpha = 0.55f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = reportCard.schoolName.ifBlank { schoolConfig.schoolName },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF064E3B)
                                    )
                                    Text(
                                        text = "نوع الشهادة: ${reportCard.periodType.arabicTitle}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB45309)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFECFDF5),
                                    border = BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "العام ${schoolConfig.academicYear}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = Color(0xFFE2E8F0))
                            Text(
                                text = "اسم الطالب الرباعي: ${reportCard.studentName}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "الصف والشعبة: ${reportCard.gradeClass} — شعبة (${reportCard.sectionName})   •   تاريخ الإصدار: ${reportCard.issuedAt}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF475569)
                            )
                        }
                    }

                    // Glassmorphic Subjects Table
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.2.dp, Color(0xFF065F46).copy(alpha = 0.35f))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Table Header
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF065F46))
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "المادة الدراسية",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    modifier = Modifier.weight(1.4f)
                                )
                                Text(
                                    text = "الدرجة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFDE68A),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.weight(0.8f)
                                )
                                Text(
                                    text = "الحالة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            reportCard.subjects.forEachIndexed { idx, sub ->
                                val rowBg = when {
                                    !sub.isPassed -> Color(0xFFFEF2F2)
                                    idx % 2 == 0 -> Color.White
                                    else -> Color(0xFFF0FDF4)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(rowBg)
                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = sub.subjectName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (sub.isPassed) Color(0xFF0F172A) else Color(0xFFB91C1C),
                                        modifier = Modifier.weight(1.4f)
                                    )
                                    Text(
                                        text = "${sub.score} / ${sub.maxScoreScale}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (sub.isPassed) Color(0xFF047857) else Color(0xFFB91C1C),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(0.8f)
                                    )
                                    Box(
                                        modifier = Modifier.weight(1f),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = if (sub.isPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                                        ) {
                                            Text(
                                                text = if (sub.isPassed) "ناجح" else "يحتاج دعم",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (sub.isPassed) Color(0xFF166534) else Color(0xFF991B1B),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(color = Color(0xFFE2E8F0))
                            }
                        }
                    }

                    // Summary Row: Total Score, Average %, and Electronic Stamp from إدارة المدرسة
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFECFDF5),
                            border = BorderStroke(1.dp, Color(0xFF10B981))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المجموع الكلي", fontSize = 11.sp, color = Color(0xFF065F46), fontWeight = FontWeight.Bold)
                                Text("${reportCard.totalScore}", fontSize = 16.sp, color = Color(0xFF047857), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFFFBEB),
                            border = BorderStroke(1.dp, Color(0xFFF59E0B))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("المعدل العام", fontSize = 11.sp, color = Color(0xFF92400E), fontWeight = FontWeight.Bold)
                                Text("${reportCard.averagePercent}%", fontSize = 16.sp, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Surface(
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.2.dp, Color(0xFFD97706))
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("الختم والمصادقة", fontSize = 10.5.sp, color = Color(0xFF064E3B), fontWeight = FontWeight.ExtraBold)
                                Text("مصادق • إدارة المدرسة", fontSize = 11.sp, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold, maxLines = 1, softWrap = false)
                            }
                        }
                    }

                    // Bottom Action Buttons: Print Official PDF & Close
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onPrintPdf,
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF047857),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1.35f)
                        ) {
                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("طباعة / حفظ الكارت PDF", fontWeight = FontWeight.ExtraBold, fontSize = 11.5.sp, maxLines = 1, softWrap = false)
                        }
                        OutlinedButton(
                            onClick = onDismiss,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.75f)
                        ) {
                            Text("إغلاق", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CleanGradeInputBox(
    title: String,
    maxScale: Int,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$title (من $maxScale)",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = DeepForestGreen,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        OutlinedTextField(
            value = value,
            enabled = enabled,
            readOnly = !enabled,
            onValueChange = { raw ->
                if (!enabled) return@OutlinedTextField
                val digits = raw.filter { c -> c.isDigit() }.take(3)
                val num = digits.toIntOrNull()
                if (digits.isEmpty() || (num != null && num <= maxScale)) {
                    onValueChange(digits)
                }
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            ),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = DeepEmerald,
                unfocusedBorderColor = DeepEmerald.copy(alpha = 0.28f),
                focusedContainerColor = DeepEmerald.copy(alpha = 0.04f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ScoreCell(
    label: String,
    score: Int,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    maxScale: Int = 0
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (highlight) DeepEmerald else DeepForestGreen,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp),
            shape = RoundedCornerShape(12.dp),
            color = if (highlight) DeepEmerald.copy(alpha = 0.12f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(
                width = 1.dp,
                color = if (highlight) DeepEmerald.copy(alpha = 0.45f) else DeepEmerald.copy(alpha = 0.22f)
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = if (maxScale > 0) "$score / $maxScale" else "$score",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (highlight) DeepEmerald else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

@Composable
fun QuickQuizParticipantsModal(
    quiz: QuickQuizItem,
    targetStudents: List<StudentRecord>,
    onDismiss: () -> Unit
) {
    var selectedModalTab by remember { mutableIntStateOf(0) } // 0 = الطلبة المشاركون, 1 = الطلبة غير المشاركين

    data class ParticipantDetail(
        val studentId: String,
        val studentName: String,
        val gradeAndSection: String,
        val chosenOptionText: String,
        val isCorrect: Boolean,
        val earnedPoints: Int,
        val answeredAt: String
    )

    val participantDetails = remember(quiz, targetStudents) {
        val studentMap = targetStudents.associateBy { it.id }
        quiz.studentAnswers.entries.map { (stuId, optIdx) ->
            val stu = studentMap[stuId]
            val chosenText = quiz.options.getOrNull(optIdx) ?: "خيار ${optIdx + 1}"
            val isCorrect = optIdx == quiz.correctOptionIndex
            ParticipantDetail(
                studentId = stuId,
                studentName = stu?.fullNameWithFather ?: "طالب ($stuId)",
                gradeAndSection = if (stu != null) "${stu.gradeClass} - شعبة ${stu.sectionName}" else "${quiz.gradeClass} - ${quiz.sectionName}",
                chosenOptionText = chosenText,
                isCorrect = isCorrect,
                earnedPoints = if (isCorrect) quiz.points else 0,
                answeredAt = quiz.answerTimestamps[stuId] ?: quiz.createdAt
            )
        }
    }

    val nonParticipants = remember(quiz, targetStudents) {
        targetStudents.filter { stu -> !quiz.studentAnswers.containsKey(stu.id) }
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
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = DeepEmerald
                    )
                    Text(
                        text = "كشف المشاركين وإجابات الطلبة التفصيلية",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }
                Text(
                    text = "مادة ${quiz.subjectName} • الصف ${quiz.gradeClass} (${quiz.sectionName}) • الدرجة: ${quiz.points}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMutedGreen
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 470.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DeepEmerald.copy(alpha = 0.08f)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "السؤال: ${quiz.questionText}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                        Text(
                            text = "الإجابة النموذجية الصحيحة: (${quiz.options.getOrNull(quiz.correctOptionIndex) ?: ""})",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }
                }

                // تبويبين: 1. الطلبة المشاركون (X طالب) | 2. الطلبة غير المشاركين (Y طالب)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = { selectedModalTab = 0 },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("modal_tab_participants"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedModalTab == 0) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedModalTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "الطلبة المشاركون (${participantDetails.size} طالب)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { selectedModalTab = 1 },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp)
                            .testTag("modal_tab_non_participants"),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selectedModalTab == 1) WarningAmber else MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = if (selectedModalTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    ) {
                        Text(
                            text = "غير المشاركين (${nonParticipants.size} طالب)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1
                        )
                    }
                }

                HorizontalDivider(color = DeepEmerald.copy(alpha = 0.15f))

                if (selectedModalTab == 0) {
                    if (participantDetails.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "لم يشارك أي طالب في هذا الاختبار حتى الآن.",
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
                            items(participantDetails, key = { it.studentId }) { item ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (item.isCorrect) SuccessGreen.copy(alpha = 0.10f) else DangerRed.copy(alpha = 0.10f),
                                    border = BorderStroke(
                                        1.dp,
                                        if (item.isCorrect) SuccessGreen.copy(alpha = 0.40f) else DangerRed.copy(alpha = 0.40f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.studentName,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (item.isCorrect) SuccessGreen else DangerRed
                                            ) {
                                                Text(
                                                    text = if (item.isCorrect) "إجابة صحيحة" else "إجابة خاطئة",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "الإجابة المختارة: (${item.chosenOptionText})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (item.isCorrect) DeepEmerald else DangerRed
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface
                                            ) {
                                                Text(
                                                    text = "احتُسبت له: ${item.earnedPoints} من ${quiz.points}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (item.isCorrect) SuccessGreen else DangerRed,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = "وقت الإجابة: ${item.answeredAt}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (nonParticipants.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "ممتاز! جميع طلاب الصف والشعبة شاركوا في هذا الاختبار.",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(nonParticipants, key = { it.id }) { stu ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = WarningAmber.copy(alpha = 0.10f),
                                    border = BorderStroke(1.dp, WarningAmber.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stu.fullNameWithFather,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepForestGreen
                                            )
                                            Text(
                                                text = "الصف: ${stu.gradeClass} - شعبة ${stu.sectionName}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TextMutedGreen
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = WarningAmber.copy(alpha = 0.20f)
                                        ) {
                                            Text(
                                                text = "لم يشارك بعد",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = WarningAmber,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
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
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
            ) {
                Text("إغلاق الكشف", fontWeight = FontWeight.Bold)
            }
        }
    )
}

/**
 * مكون إرفاق صورة توضيحية للواجب من الكاميرا أو الاستوديو مع أنيميشن اختيار المصدر ومعاينة مصغرة
 * (Animated Camera / Gallery Illustrative Photo Picker for Homework Publishing)
 */
@Composable
fun HomeworkIllustrativePhotoPickerSection(
    attachmentBase64: String,
    onAttachmentChanged: (String) -> Unit
) {
    val context = LocalContext.current
    var showSourceOptions by remember { mutableStateOf(false) }

    val cameraPreviewLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val encoded = ImageCompressor.compressBitmapToBase64Jpeg(bitmap)
            if (!encoded.isNullOrBlank()) {
                onAttachmentChanged(encoded)
                showSourceOptions = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraPreviewLauncher.launch(null)
        }
    }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCompressor.compressUriToBase64Jpeg(context, uri)
            if (!encoded.isNullOrBlank()) {
                onAttachmentChanged(encoded)
                showSourceOptions = false
            }
        }
    }

    val previewBitmap = remember(attachmentBase64) {
        ImageCompressor.decodeBase64ToImageBitmap(attachmentBase64)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (attachmentBase64.isNotBlank()) Color(0xFFECFDF5) else Color(0xFFF0FDF4),
            border = BorderStroke(
                1.3.dp,
                if (attachmentBase64.isNotBlank()) Color(0xFF059669) else Color(0xFF10B981).copy(alpha = 0.55f)
            ),
            shadowElevation = 4.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50))
                .clickable { showSourceOptions = !showSourceOptions }
                .testTag("toggle_hw_photo_picker_btn")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF059669),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (attachmentBase64.isNotBlank()) Icons.Filled.CheckCircle else Icons.Filled.AddAPhoto,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Text(
                        text = if (attachmentBase64.isNotBlank()) {
                            "تم إرفاق صورة للواجب (اضغط لتغييرها)"
                        } else {
                            "+ إرفاق صورة للواجب (كاميرا / استوديو)"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (showSourceOptions) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF059669)
                )
            }
        }

        AnimatedVisibility(
            visible = showSourceOptions,
            enter = expandVertically(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(tween(220)),
            exit = shrinkVertically(tween(180)) + fadeOut(tween(150))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // خيار 1: التقاط بالكاميرا
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                cameraPreviewLauncher.launch(null)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                        .testTag("hw_pick_camera_option")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFECFDF5), Color.White)
                                )
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF059669),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoCamera,
                                    contentDescription = "التقاط بالكاميرا",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "التقاط بالكاميرا",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // خيار 2: اختيار من الاستوديو
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, Color(0xFF0284C7).copy(alpha = 0.45f)),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            galleryPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .testTag("hw_pick_gallery_option")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFFF0F9FF), Color.White)
                                )
                            )
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF0284C7),
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.PhotoLibrary,
                                    contentDescription = "اختيار من الاستوديو",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "اختيار من الاستوديو",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0C4A6E),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        if (previewBitmap != null) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "معاينة الصورة التوضيحية المرفقة:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepForestGreen
                        )
                        TextButton(
                            onClick = { onAttachmentChanged("") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "حذف الصورة",
                                tint = DangerRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إزالة الصورة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = DangerRed
                            )
                        }
                    }
                    Image(
                        bitmap = previewBitmap,
                        contentDescription = "معاينة مرفق الواجب",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                GlobalImageLightboxController.open(
                                    attachmentBase64,
                                    "معاينة الصورة التوضيحية للواجب"
                                )
                            }
                    )
                }
            }
        }
    }
}



