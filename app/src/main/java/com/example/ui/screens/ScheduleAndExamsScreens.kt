package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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
    val schedules by viewModel.schedules.collectAsState()
    val homeworks by viewModel.homeworks.collectAsState()
    val homeworkSubmissions by viewModel.homeworkSubmissions.collectAsState()
    val quickQuizzes by viewModel.quickQuizzes.collectAsState()
    val quickQuizAnswers by viewModel.quickQuizAnswers.collectAsState()
    val studyMaterials by viewModel.studyMaterials.collectAsState()

    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT
    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER
    val activeStudent = remember(currentUser, selectedParentChildId) {
        viewModel.getScopedStudentsForUser(currentUser).firstOrNull()
    }

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
    val allowedSubjects = remember(schoolConfig, selectedClass, currentUser) {
        viewModel.getSubjectsForClass(selectedClass, currentUser)
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = التحضير والواجبات, 1 = جدول الحصص الأسبوعي, 2 = المكتبة والملخصات
    var selectedDay by remember { mutableStateOf("الأحد") }

    var showPublishHomeworkDialog by remember { mutableStateOf(false) }
    var showPublishQuickQuizDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var showPublishStudyMaterialDialog by remember { mutableStateOf(false) }

    val filteredHomeworks = remember(homeworks, selectedClass, selectedSection, currentUser) {
        homeworks.filter { hw ->
            val clsMatch = hw.gradeClass == selectedClass && hw.sectionName == selectedSection
            val subjMatch = if (currentUser?.role == UserRole.TEACHER) {
                currentUser?.canTeacherAccessSubject(hw.subjectName) == true
            } else true
            clsMatch && subjMatch
        }
    }

    val filteredQuickQuizzes = remember(quickQuizzes, selectedClass, selectedSection) {
        quickQuizzes.filter { q ->
            q.gradeClass == selectedClass &&
                (q.sectionName == "الكل" || q.sectionName == selectedSection)
        }
    }

    val filteredPeriods = remember(schedules, selectedClass, selectedSection, selectedDay) {
        schedules.filter {
            it.gradeClass == selectedClass &&
                it.sectionName == selectedSection &&
                it.dayOfWeek == selectedDay
        }.sortedBy { it.periodNumber }
    }

    val filteredStudyMaterials = remember(studyMaterials, selectedClass, selectedSection) {
        studyMaterials.filter { mat ->
            mat.gradeClass == selectedClass &&
                (mat.sectionName == "الكل" || mat.sectionName == selectedSection)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = { selectedTab = 0 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("tab_daily_homework"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.MenuBook, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("الواجبات", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }

            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .testTag("tab_weekly_schedule"),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("جدول الحصص", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
            }

            if (featureSettings.studyLibraryEnabled) {
                Button(
                    onClick = { selectedTab = 2 },
                    modifier = Modifier
                        .weight(1.1f)
                        .heightIn(min = 46.dp)
                        .testTag("tab_study_materials"),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedTab == 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = if (selectedTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(Icons.Filled.LocalLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("المكتبة والملخصات", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                }
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
                            text = "نشر اختبار سريع ⚡",
                            icon = Icons.Filled.Quiz,
                            containerColor = GoldDeep,
                            onClick = { showPublishQuickQuizDialog = true },
                            modifier = Modifier.weight(1f),
                            testTag = "publish_quick_quiz_button"
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // Quick Quizzes Section (الاختبارات السريعة والتصحيح التلقائي)
                if (featureSettings.quickQuizzesEnabled && filteredQuickQuizzes.isNotEmpty()) {
                    item {
                        Text(
                            text = "الاختبارات السريعة المؤقتة والتصحيح الفوري ⚡ (${filteredQuickQuizzes.size}):",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepEmerald
                        )
                    }
                    items(filteredQuickQuizzes, key = { "quiz_${it.id}" }) { quiz ->
                        val quizAnsForStudent = remember(quickQuizAnswers, quiz.id, activeStudent?.id) {
                            quickQuizAnswers.firstOrNull { it.quizId == quiz.id && it.studentId == activeStudent?.id }
                        }
                        val allQuizAnswers = remember(quickQuizAnswers, quiz.id) {
                            quickQuizAnswers.filter { it.quizId == quiz.id }
                        }
                        var selectedOptIndex by remember(quiz.id) { mutableIntStateOf(-1) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = GoldAccent.copy(alpha = 0.10f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldAccent.copy(alpha = 0.45f))
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
                                    NoWrapBadge(
                                        text = "اختبار سريع • ${quiz.subjectName}",
                                        icon = Icons.Filled.Quiz,
                                        containerColor = GoldAccent.copy(alpha = 0.25f),
                                        contentColor = Color(0xFF78350F)
                                    )
                                    NoWrapBadge(
                                        text = "الدرجة: ${quiz.points}",
                                        icon = Icons.Filled.Stars,
                                        containerColor = DeepEmerald.copy(alpha = 0.14f),
                                        contentColor = DeepEmerald
                                    )
                                }

                                Text(
                                    text = quiz.questionText,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold
                                )

                                quiz.options.forEachIndexed { idx, optText ->
                                    val isAnswered = quizAnsForStudent != null
                                    val isCorrectOption = idx == quiz.correctOptionIndex
                                    val isChosenOption = (quizAnsForStudent?.selectedOptionIndex ?: selectedOptIndex) == idx

                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable(enabled = isParentOrStudent && !isAnswered) {
                                                selectedOptIndex = idx
                                            },
                                        shape = RoundedCornerShape(10.dp),
                                        color = when {
                                            isAnswered && isCorrectOption -> SuccessGreen.copy(alpha = 0.18f)
                                            isAnswered && isChosenOption && !isCorrectOption -> DangerRed.copy(alpha = 0.18f)
                                            isChosenOption -> DeepEmerald.copy(alpha = 0.15f)
                                            else -> MaterialTheme.colorScheme.surface
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            when {
                                                isAnswered && isCorrectOption -> SuccessGreen
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
                                                fontWeight = if (isChosenOption || (isAnswered && isCorrectOption)) FontWeight.Bold else FontWeight.Normal
                                            )
                                            if (isAnswered && isCorrectOption) {
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
                                                    "إجابة صحيحة ممتازة! حصلت على (${quizAnsForStudent.earnedPoints} / ${quiz.points}) 🌟"
                                                } else {
                                                    "إجابة غير صحيحة • الإجابة الصحيحة هي: ${quiz.options.getOrNull(quiz.correctOptionIndex) ?: ""}"
                                                },
                                                modifier = Modifier.padding(10.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = if (quizAnsForStudent.isCorrect) DeepEmerald else DangerRed
                                            )
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
                                    val correctCount = allQuizAnswers.count { it.isCorrect }
                                    Text(
                                        text = "إحصائية الإجابات: شارك ${allQuizAnswers.size} طالب • إجابات صحيحة: $correctCount • الإجابة الصحيحة: (${quiz.options.getOrNull(quiz.correctOptionIndex) ?: ""})",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeepEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

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
                        val hwSubs = remember(homeworkSubmissions, hw.id) {
                            homeworkSubmissions.filter { it.homeworkId == hw.id }
                        }
                        val mySubmission = remember(hwSubs, activeStudent?.id) {
                            hwSubs.firstOrNull { it.studentId == activeStudent?.id }
                        }
                        var pendingPhotoBase64 by remember(hw.id) { mutableStateOf("") }
                        var pendingStudentNote by remember(hw.id) { mutableStateOf("") }

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

                                // Photo Homework Submission & Teacher Review (تسليم الواجب بالصورة)
                                if (featureSettings.homeworkSubmissionEnabled) {
                                    if (isParentOrStudent) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = DeepEmerald.copy(alpha = 0.07f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
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
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            modifier = Modifier.weight(1f)
                                                        ) {
                                                            Base64AvatarImage(
                                                                base64Data = mySubmission.solutionImageBase64,
                                                                size = 42.dp,
                                                                fallbackIcon = Icons.Filled.CameraAlt,
                                                                contentDescription = "صورة حل الواجب"
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = if (mySubmission.isReviewed) "تمت المراجعة والتصحيح ✅" else "تم إرسال صورة الحل للمعلم ⏳",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    color = if (mySubmission.isReviewed) SuccessGreen else DeepEmerald
                                                                )
                                                                if (mySubmission.teacherFeedback.isNotBlank()) {
                                                                    Text(
                                                                        text = "ملاحظة المعلم: ${mySubmission.teacherFeedback}",
                                                                        style = MaterialTheme.typography.labelSmall,
                                                                        color = DeepEmerald
                                                                    )
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CompressedPhotoPickerButton(
                                                        label = if (pendingPhotoBase64.isEmpty()) "تصوير/إرفاق دفتر الحل 📸" else "جاهز للإرسال ✅",
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
                                    } else if (canPublish && hwSubs.isNotEmpty()) {
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = DeepEmerald.copy(alpha = 0.07f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "حلول الواجب المرسلة بالصورة من الطلاب (${hwSubs.size}):",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepEmerald
                                                )
                                                hwSubs.forEach { sub ->
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(vertical = 4.dp),
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
                                                                size = 40.dp,
                                                                fallbackIcon = Icons.Filled.Image,
                                                                contentDescription = "حل الطالب ${sub.studentName}"
                                                            )
                                                            Column {
                                                                Text(
                                                                    text = "${sub.studentName} (اضغط على الصورة للتكبير)",
                                                                    style = MaterialTheme.typography.labelMedium,
                                                                    fontWeight = FontWeight.Bold
                                                                )
                                                                Text(
                                                                    text = if (sub.isReviewed) "تمت المراجعة ✅" else "بانتظار التصحيح ⏳ • ${sub.submittedAt}",
                                                                    style = MaterialTheme.typography.labelSmall,
                                                                    color = if (sub.isReviewed) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                                )
                                                            }
                                                        }
                                                        if (!sub.isReviewed) {
                                                            TextButton(
                                                                onClick = {
                                                                    viewModel.reviewHomeworkSubmission(
                                                                        submissionId = sub.id,
                                                                        feedback = "أحسنت بارك الله بك، تمت مراجعة وتصحيح الواجب 🌟"
                                                                    )
                                                                },
                                                                modifier = Modifier.testTag("review_hw_sub_btn_${sub.id}")
                                                            ) {
                                                                Text("تصحيح ✅", fontWeight = FontWeight.Bold, color = DeepEmerald)
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
            }
        } else if (selectedTab == 1) {
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
        } else {
            // =========================================================================
            // TAB 2: المكتبة الرقمية والملخصات الدراسية (study_materials)
            // =========================================================================
            if (canPublish) {
                VectorActionButton(
                    text = "نشر ملخص دراسي / ملزمة / ورقة عمل لـ ($selectedClass - $selectedSection)",
                    icon = Icons.Filled.LibraryAdd,
                    onClick = { showPublishStudyMaterialDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "publish_study_material_button"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
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
            title = { Text("نشر اختبار سريع مؤقت لـ ($selectedClass - $selectedSection) ⚡", fontWeight = FontWeight.ExtraBold) },
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
                    Text("نشر الاختبار للطلاب ⚡", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPublishQuickQuizDialog = false }) { Text("إلغاء") }
            }
        )
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
    val allowedSubjects = remember(schoolConfig, selectedClass, currentUser) {
        viewModel.getSubjectsForClass(selectedClass, currentUser)
    }
    var selectedSubject by remember(allowedSubjects) {
        mutableStateOf(allowedSubjects.firstOrNull() ?: "الرياضيات")
    }
    var selectedMaxScale by remember(selectedClass) {
        mutableIntStateOf(GradingRules.defaultMaxScoreForClass(selectedClass))
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
                    val myBadges = viewModel.getStudentAutoBadges(myStu.id)
                    val studentSubjects = viewModel.getSubjectsForClass(myStu.gradeClass, null)
                    val defaultScaleForStu = GradingRules.defaultMaxScoreForClass(myStu.gradeClass)
                    val allSubjectsReport = studentSubjects.map { subjName ->
                        myGrades.firstOrNull { it.subjectName == subjName } ?: SubjectGrade(
                            id = "temp_$subjName",
                            schoolId = schoolConfig.schoolId,
                            studentId = myStu.id,
                            studentName = myStu.studentName,
                            gradeClass = myStu.gradeClass,
                            subjectName = subjName,
                            maxScoreScale = defaultScaleForStu,
                            dailyScore = 0,
                            month1Score = 0,
                            month2Score = 0,
                            midYearScore = 0,
                            finalExamScore = 0
                        )
                    }
                    val overallAvg = if (allSubjectsReport.isEmpty()) 0 else allSubjectsReport.map { it.normalizedPercentage }.average().toInt()
                    val overallRating = when {
                        overallAvg >= 90 -> "ممتاز"
                        overallAvg >= 80 -> "جيد جداً"
                        overallAvg >= 70 -> "جيد"
                        overallAvg >= 60 -> "متوسط"
                        overallAvg >= 50 -> "مقبول"
                        else -> "غير مرصود / ضعيف"
                    }

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
                            if (myBadges.isNotEmpty()) {
                                StudentAutoBadgesRow(badges = myBadges)
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
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير / طباعة بطاقة الدرجات الرسمية (PDF)")
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
                                            text = "المعدل: ${g.finalAverage}/${g.maxScoreScale} • ${g.ratingLabel}",
                                            icon = Icons.Filled.Stars,
                                            containerColor = if (g.normalizedPercentage >= 50) SuccessGreen.copy(alpha = 0.16f) else DangerRed.copy(alpha = 0.16f),
                                            contentColor = if (g.normalizedPercentage >= 50) SuccessGreen else DangerRed
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

                                // 3. Subject Selector (المادة الدراسية المخصصة للصف المختار)
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

                                // 3.B Flexible Grading Scale Toggle (نظام الدرجة: من 10 / من 100)
                                Surface(
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Tune,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Text(
                                                text = "نظام الدرجة (${if (GradingRules.defaultMaxScoreForClass(selectedClass) == 10) "تلقائي للأول-الرابع الابتدائي" else "قياسي"}):",
                                                style = MaterialTheme.typography.labelLarge,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = selectedMaxScale == 10,
                                                onClick = { selectedMaxScale = 10 },
                                                label = { Text("من 10", maxLines = 1, softWrap = false) },
                                                leadingIcon = {
                                                    if (selectedMaxScale == 10) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                },
                                                modifier = Modifier.testTag("scale_toggle_10")
                                            )
                                            FilterChip(
                                                selected = selectedMaxScale == 100,
                                                onClick = { selectedMaxScale = 100 },
                                                label = { Text("من 100", maxLines = 1, softWrap = false) },
                                                leadingIcon = {
                                                    if (selectedMaxScale == 100) {
                                                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                                    }
                                                },
                                                modifier = Modifier.testTag("scale_toggle_100")
                                            )
                                        }
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
                        items(classSectionStudents, key = { "${it.id}_${selectedSubject}_$selectedMaxScale" }) { stu ->
                            val existingGrade = grades.firstOrNull {
                                it.studentId == stu.id && it.subjectName == selectedSubject
                            }
                            val defaultVal = if (selectedMaxScale == 10) 9 else 90

                            var dailyText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.dailyScore ?: defaultVal).coerceAtMost(selectedMaxScale).toString())
                            }
                            var m1Text by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.month1Score ?: defaultVal).coerceAtMost(selectedMaxScale).toString())
                            }
                            var m2Text by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.month2Score ?: defaultVal).coerceAtMost(selectedMaxScale).toString())
                            }
                            var midText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.midYearScore ?: defaultVal).coerceAtMost(selectedMaxScale).toString())
                            }
                            var finalText by remember(existingGrade, selectedSubject, selectedMaxScale) {
                                mutableStateOf((existingGrade?.finalExamScore ?: defaultVal).coerceAtMost(selectedMaxScale).toString())
                            }
                            var savedToast by remember { mutableStateOf(false) }

                            val dVal = (dailyText.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val m1Val = (m1Text.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
                            val m2Val = (m2Text.toIntOrNull() ?: 0).coerceIn(0, selectedMaxScale)
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
                                                    text = "مادة: $selectedSubject (من $selectedMaxScale) • ${stu.gradeClass} (${stu.sectionName})",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        NoWrapBadge(
                                            text = "السعي: ${previewGrade.annualEffortScore}/$selectedMaxScale | المعدل: ${previewGrade.finalAverage}/$selectedMaxScale (${previewGrade.ratingLabel})",
                                            icon = Icons.Filled.Calculate,
                                            containerColor = if (previewGrade.normalizedPercentage >= 50) SuccessGreen.copy(alpha = 0.15f) else DangerRed.copy(alpha = 0.15f),
                                            contentColor = if (previewGrade.normalizedPercentage >= 50) SuccessGreen else DangerRed
                                        )
                                    }

                                    // Inline Grade Input Fields for this Subject (strictly clamped to selectedMaxScale)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = dailyText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { c -> c.isDigit() }.take(3)
                                                val num = digits.toIntOrNull()
                                                if (digits.isEmpty() || (num != null && num <= selectedMaxScale)) {
                                                    dailyText = digits
                                                    savedToast = false
                                                }
                                            },
                                            label = { Text("يومي (/$selectedMaxScale)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = m1Text,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { c -> c.isDigit() }.take(3)
                                                val num = digits.toIntOrNull()
                                                if (digits.isEmpty() || (num != null && num <= selectedMaxScale)) {
                                                    m1Text = digits
                                                    savedToast = false
                                                }
                                            },
                                            label = { Text("شهر 1 (/$selectedMaxScale)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = m2Text,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { c -> c.isDigit() }.take(3)
                                                val num = digits.toIntOrNull()
                                                if (digits.isEmpty() || (num != null && num <= selectedMaxScale)) {
                                                    m2Text = digits
                                                    savedToast = false
                                                }
                                            },
                                            label = { Text("شهر 2 (/$selectedMaxScale)") },
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
                                            onValueChange = { raw ->
                                                val digits = raw.filter { c -> c.isDigit() }.take(3)
                                                val num = digits.toIntOrNull()
                                                if (digits.isEmpty() || (num != null && num <= selectedMaxScale)) {
                                                    midText = digits
                                                    savedToast = false
                                                }
                                            },
                                            label = { Text("نصف السنة (/$selectedMaxScale)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                        ScoreCell(
                                            label = "السعي (/$selectedMaxScale)",
                                            score = previewGrade.annualEffortScore,
                                            modifier = Modifier.weight(1f),
                                            highlight = true
                                        )
                                        OutlinedTextField(
                                            value = finalText,
                                            onValueChange = { raw ->
                                                val digits = raw.filter { c -> c.isDigit() }.take(3)
                                                val num = digits.toIntOrNull()
                                                if (digits.isEmpty() || (num != null && num <= selectedMaxScale)) {
                                                    finalText = digits
                                                    savedToast = false
                                                }
                                            },
                                            label = { Text("النهائي (/$selectedMaxScale)") },
                                            singleLine = true,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    VectorActionButton(
                                        text = if (savedToast) "تم حفظ درجات ($selectedSubject من $selectedMaxScale) للطالب بنجاح" else "حفظ درجات ($selectedSubject من $selectedMaxScale) للطالب",
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
                                                finalExamScore = finVal,
                                                maxScoreScale = selectedMaxScale
                                            )
                                            savedToast = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )

                                    val stuAutoBadges = viewModel.getStudentAutoBadges(stu.id)
                                    if (stuAutoBadges.isNotEmpty()) {
                                        StudentAutoBadgesRow(badges = stuAutoBadges)
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
                                            Icon(Icons.Filled.PictureAsPdf, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("تصدير بطاقة الدرجات (الشهادة المدرسية PDF)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Exam Schedule Tab with Live Countdown Badge
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
                    val daysRemaining = ex.daysRemainingUntilExam() ?: -1L
                    val countdownText = when {
                        daysRemaining > 1L -> "باقي $daysRemaining يوم"
                        daysRemaining == 1L -> "غداً (باقي يوم واحد)"
                        daysRemaining == 0L -> "الامتحان اليوم!"
                        else -> "منتهي"
                    }
                    val countdownColor = when {
                        daysRemaining in 0L..3L -> DangerRed
                        daysRemaining > 3L -> GoldDeep
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
                                    text = "${ex.subjectName} • ${ex.gradeClass}",
                                    icon = Icons.Filled.School,
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    NoWrapBadge(
                                        text = countdownText,
                                        icon = Icons.Filled.Timer,
                                        containerColor = countdownColor.copy(alpha = 0.16f),
                                        contentColor = countdownColor
                                    )
                                    NoWrapBadge(
                                        text = "${ex.dayName} ${ex.examDate}",
                                        icon = Icons.Filled.Event,
                                        containerColor = GoldAccent.copy(alpha = 0.2f),
                                        contentColor = GoldDeep
                                    )
                                }
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
