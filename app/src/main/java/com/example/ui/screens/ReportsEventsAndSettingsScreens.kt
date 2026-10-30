package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ExternalIntents
import com.example.util.ImageCompressor
import com.example.viewmodel.SchoolViewModel

/**
 * Section 7: Reports & Direct Messaging Screen (نظام البلاغات والتواصل المباشر)
 * - Structured Report Creation: Step A (Select Class & Section) -> Step B (Search by Name & Select Student) -> Send Report
 * - Strict Privacy Isolation for Parent/Student (sees only their own reports & chat)
 * - Scoped to assigned classes/sections for Teacher
 */
@Composable
fun ReportsAndChatScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val students by viewModel.students.collectAsState()
    val reports by viewModel.reports.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = البلاغات الرسمية, 1 = صندوق المراسلة المباشر
    var showCreateReportDialog by remember { mutableStateOf(false) }

    val canSendReport = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER
    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT

    val scopedStudents = remember(students, currentUser) {
        viewModel.getScopedStudentsForUser(currentUser)
    }

    var selectedStudentId by remember(scopedStudents) {
        mutableStateOf(scopedStudents.firstOrNull()?.id ?: "")
    }
    val selectedStudent = remember(scopedStudents, selectedStudentId) {
        scopedStudents.firstOrNull { it.id == selectedStudentId } ?: scopedStudents.firstOrNull()
    }

    val visibleReports = remember(reports, scopedStudents) {
        val allowedIds = scopedStudents.map { it.id }.toSet()
        reports.filter { it.studentId in allowedIds }
    }

    val studentChats = remember(chatMessages, selectedStudent) {
        chatMessages.filter { it.studentId == selectedStudent?.id }
    }

    var draftMessage by remember { mutableStateOf("") }
    var draftChatImageBase64 by remember { mutableStateOf("") }
    val chatPhotoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageCompressor.compressUriToBase64Jpeg(context, uri)
            if (encoded != null) {
                draftChatImageBase64 = encoded
            }
        }
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
                    .testTag("tab_official_reports"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isParentOrStudent) "بلاغاتي الخاصة" else "البلاغات والتبليغات",
                    maxLines = 1,
                    softWrap = false
                )
            }

            Button(
                onClick = { selectedTab = 1 },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("tab_direct_chat"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Forum, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("صندوق المراسلة المباشر", maxLines = 1, softWrap = false)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (selectedTab == 0) {
            if (canSendReport) {
                VectorActionButton(
                    text = "إرسال بلاغ رسمي (اختيار الصف والشعبة ⬅️ البحث واختيار الطالب)",
                    icon = Icons.Filled.MarkChatUnread,
                    onClick = { showCreateReportDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "create_report_button"
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visibleReports, key = { it.id }) { rep ->
                    val badgeColor = when (rep.reportType) {
                        ReportType.APPRECIATION -> SuccessGreen
                        ReportType.ACADEMIC_NOTE -> InfoBlue
                        ReportType.BEHAVIOR_ALERT -> WarningAmber
                        ReportType.PARENT_SUMMONS -> DangerRed
                    }
                    val badgeIcon = when (rep.reportType) {
                        ReportType.APPRECIATION -> Icons.Filled.EmojiEvents
                        ReportType.ACADEMIC_NOTE -> Icons.Filled.MenuBook
                        ReportType.BEHAVIOR_ALERT -> Icons.Filled.Warning
                        ReportType.PARENT_SUMMONS -> Icons.Filled.NotificationImportant
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
                                    text = rep.reportType.arabicLabel,
                                    icon = badgeIcon,
                                    containerColor = badgeColor.copy(alpha = 0.16f),
                                    contentColor = badgeColor
                                )
                                Text(
                                    text = rep.createdAt,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Text(
                                text = rep.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "الطالب: ${rep.studentName} (${rep.gradeClass}) • ولي الأمر: ${rep.parentName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = rep.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }

                            if (canSendReport) {
                                VectorActionButton(
                                    text = "إرسال البلاغ لولي الأمر عبر واتساب",
                                    icon = Icons.Filled.Chat,
                                    containerColor = WhatsAppGreen,
                                    onClick = {
                                        val msg = """
                                            تبليغ رسمي (${rep.reportType.arabicLabel})
                                            من: ${schoolConfig.schoolName} (${schoolConfig.address})
                                            إلى السيد ولي أمر الطالب (${rep.studentName} - ${rep.gradeClass}) المحترم:
                                            • الموضوع: ${rep.title}
                                            • التفاصيل: ${rep.details}
                                            • المرسل: ${rep.senderName}
                                        """.trimIndent()
                                        ExternalIntents.sendWhatsAppMessage(context, rep.parentPhone, msg)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Direct Messaging Inbox between Parent & Administration
            if (!isParentOrStudent) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(scopedStudents, key = { it.id }) { stu ->
                        FilterChip(
                            selected = selectedStudent?.id == stu.id,
                            onClick = { selectedStudentId = stu.id },
                            label = { Text("محادثة: ${stu.studentName}", maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(Icons.Filled.Person, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedStudent != null) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تواصل مباشر: ${selectedStudent.parentName}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = "ولي أمر الطالب: ${selectedStudent.studentName} (${selectedStudent.gradeClass})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppGreen,
                                modifier = Modifier.clickable {
                                    ExternalIntents.sendWhatsAppMessage(
                                        context,
                                        selectedStudent.parentPhone,
                                        "السلام عليكم السيد ${selectedStudent.parentName} ولي أمر الطالب ${selectedStudent.studentName}، نتواصل معكم من إدارة ${schoolConfig.schoolName}."
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "محادثة واتساب",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentChats, key = { it.id }) { msg ->
                                val isFromAdminOrTeacher = msg.senderRole == UserRole.ADMIN || msg.senderRole == UserRole.TEACHER
                                val msgBitmap = remember(msg.imageBase64) {
                                    ImageCompressor.decodeBase64ToImageBitmap(msg.imageBase64)
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalAlignment = if (isFromAdminOrTeacher) Alignment.Start else Alignment.End
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (isFromAdminOrTeacher) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.secondaryContainer,
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = msg.senderName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            if (msgBitmap != null) {
                                                Spacer(modifier = Modifier.height(6.dp))
                                                Image(
                                                    bitmap = msgBitmap,
                                                    contentDescription = "صورة مرفقة",
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(150.dp)
                                                        .clip(RoundedCornerShape(10.dp))
                                                )
                                            }
                                            if (msg.messageText.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = msg.messageText,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (draftChatImageBase64.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.Image,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "تم إرفاق صورة جاهزة للإرسال",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    IconButton(
                                        onClick = { draftChatImageBase64 = "" },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Filled.Close, contentDescription = "إزالة الصورة", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    chatPhotoLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .testTag("chat_attach_photo_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AddPhotoAlternate,
                                    contentDescription = "إرفاق صورة",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            OutlinedTextField(
                                value = draftMessage,
                                onValueChange = { draftMessage = it },
                                placeholder = { Text("اكتب رسالتك للإدارة أو ولي الأمر...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Button(
                                onClick = {
                                    if (draftMessage.isNotBlank() || draftChatImageBase64.isNotBlank()) {
                                        viewModel.sendChatMessage(selectedStudent, draftMessage, draftChatImageBase64)
                                        draftMessage = ""
                                        draftChatImageBase64 = ""
                                    }
                                },
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("send_chat_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "إرسال")
                            }
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // REQUIREMENT 5: STRUCTURED OFFICIAL REPORT MODAL
    // Step A: Select Class & Section -> Step B: Search by Name & Select Student -> Send Report
    // =========================================================================
    if (showCreateReportDialog) {
        val allowedClasses = remember(schoolConfig, currentUser) {
            viewModel.getAllowedClassesForUser(currentUser)
        }
        var reportClass by remember(allowedClasses) {
            mutableStateOf(allowedClasses.firstOrNull() ?: "الأول المتوسط")
        }
        val allowedSections = remember(schoolConfig, currentUser, reportClass) {
            viewModel.getAllowedSectionsForClass(reportClass, currentUser)
        }
        var reportSection by remember(allowedSections) {
            mutableStateOf(allowedSections.firstOrNull() ?: "أ")
        }
        var studentSearchQuery by remember { mutableStateOf("") }

        val matchingStudents = remember(scopedStudents, reportClass, reportSection, studentSearchQuery) {
            scopedStudents.filter { s ->
                s.gradeClass == reportClass &&
                    s.sectionName == reportSection &&
                    (studentSearchQuery.isBlank() || s.studentName.contains(studentSearchQuery, ignoreCase = true))
            }
        }

        var chosenStudent by remember(matchingStudents) {
            mutableStateOf(matchingStudents.firstOrNull())
        }
        var selectedType by remember { mutableStateOf(ReportType.APPRECIATION) }
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateReportDialog = false },
            title = { Text("إرسال بلاغ رسمي لولي الأمر") },
            text = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 440.dp)
                ) {
                    item {
                        Text(
                            text = "أولاً: اختر الصف والشعبة:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(allowedClasses) { cls ->
                                FilterChip(
                                    selected = reportClass == cls,
                                    onClick = {
                                        reportClass = cls
                                        val nextSecs = viewModel.getAllowedSectionsForClass(cls, currentUser)
                                        if (reportSection !in nextSecs) {
                                            reportSection = nextSecs.firstOrNull() ?: "أ"
                                        }
                                    },
                                    label = { Text(cls, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("الشعبة:", style = MaterialTheme.typography.labelLarge)
                            allowedSections.forEach { sec ->
                                FilterChip(
                                    selected = reportSection == sec,
                                    onClick = { reportSection = sec },
                                    label = { Text("شعبة $sec", maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    item {
                        Text(
                            text = "ثانياً: ابحث بالاسم واختر الطالب من ($reportClass - شعبة $reportSection):",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        OutlinedTextField(
                            value = studentSearchQuery,
                            onValueChange = { studentSearchQuery = it },
                            label = { Text("بحث باسم الطالب...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        if (matchingStudents.isEmpty()) {
                            Text(
                                text = "لا يوجد طلاب في ($reportClass - شعبة $reportSection) بهذا الاسم",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(matchingStudents, key = { it.id }) { s ->
                                    FilterChip(
                                        selected = chosenStudent?.id == s.id,
                                        onClick = { chosenStudent = s },
                                        label = { Text(s.studentName, maxLines = 1, softWrap = false) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = if (chosenStudent?.id == s.id) Icons.Filled.CheckCircle else Icons.Filled.Person,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text("ثالثاً: نوع البلاغ وتفاصيله:", style = MaterialTheme.typography.labelLarge)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(ReportType.entries) { rt ->
                                FilterChip(
                                    selected = selectedType == rt,
                                    onClick = { selectedType = rt },
                                    label = { Text(rt.arabicLabel, maxLines = 1, softWrap = false) }
                                )
                            }
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = title,
                            onValueChange = { title = it },
                            label = { Text("عنوان البلاغ") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = details,
                            onValueChange = { details = it },
                            label = { Text("نص البلاغ والتفاصيل") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إرسال البلاغ وإشعار ولي الأمر",
                    icon = Icons.AutoMirrored.Filled.Send,
                    onClick = {
                        val target = chosenStudent
                        if (target != null && title.isNotBlank()) {
                            viewModel.sendParentReport(
                                student = target,
                                reportType = selectedType,
                                title = title,
                                details = details.ifBlank { title },
                                senderName = currentUser?.fullName ?: "إدارة المدرسة"
                            )
                            showCreateReportDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showCreateReportDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Section 8: Events & News Gallery with Automatic 800px / 0.7 JPEG Image Compression
 */
@Composable
fun EventsAndNewsScreen(
    viewModel: SchoolViewModel
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val events by viewModel.events.collectAsState()

    var showPublishEventDialog by remember { mutableStateOf(false) }
    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                )
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "معرض الفعاليات والأخبار المدرسية",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        NoWrapBadge(
                            text = "توثيق الأنشطة والأخبار",
                            icon = Icons.Filled.PhotoLibrary,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    }

                    Text(
                        text = "تابع أحدث الفعاليات والأنشطة العلمية والثقافية والأخبار الرسمية للمدرسة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    if (canPublish) {
                        VectorActionButton(
                            text = "نشر فعالية أو خبر مدرسي مع صورة",
                            icon = Icons.Filled.AddAPhoto,
                            onClick = { showPublishEventDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "publish_event_button"
                        )
                    }
                }
            }
        }

        items(events, key = { it.id }) { ev ->
            val decodedBitmap = remember(ev.imageBase64) {
                ImageCompressor.decodeBase64ToImageBitmap(ev.imageBase64)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(175.dp)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF064E3B),
                                        Color(0xFF059669),
                                        Color(0xFF1E3A8A)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (decodedBitmap != null) {
                            Image(
                                bitmap = decodedBitmap,
                                contentDescription = ev.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Filled.Celebration,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(52.dp)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                NoWrapBadge(
                                    text = "${ev.category} • ${ev.location}",
                                    icon = Icons.Filled.LocationOn,
                                    containerColor = Color.Black.copy(alpha = 0.3f),
                                    contentColor = Color.White
                                )
                            }
                        }
                    }

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
                                text = ev.category,
                                icon = Icons.Filled.Label,
                                containerColor = MaterialTheme.colorScheme.primaryContainer,
                                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            NoWrapBadge(
                                text = "${ev.date} • ${ev.location}",
                                icon = Icons.Filled.Event,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = ev.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            text = ev.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "الناشر: ${ev.authorName}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showPublishEventDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("نشاط علمي وثقافي") }
        var location by remember { mutableStateOf("الموصل - زمار") }
        var date by remember { mutableStateOf("2026-10-29") }
        var description by remember { mutableStateOf("") }
        var imageBase64 by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showPublishEventDialog = false },
            title = { Text("نشر فعالية أو خبر مدرسي جديد") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان الخبر أو الفعالية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("التصنيف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("الموقع الجغرافي") },
                        placeholder = { Text("الموصل - زمار") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("تفاصيل الخبر أو الفعالية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompressedPhotoPickerButton(
                        label = "إرفاق صورة الفعالية",
                        currentBase64 = imageBase64,
                        onImageCompressed = { imageBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "نشر الخبر في المعرض",
                    icon = Icons.Filled.Publish,
                    onClick = {
                        if (title.isNotBlank()) {
                            viewModel.publishSchoolEvent(
                                title = title,
                                category = category,
                                description = description.ifBlank { title },
                                date = date,
                                location = location.ifBlank { "الموصل - زمار" },
                                imageBase64 = imageBase64
                            )
                            showPublishEventDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showPublishEventDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

/**
 * Settings Screen (صفحة الإعدادات) with School Type, School Stage (ابتدائية/متوسطة/إعدادية/ثانوية),
 * dynamic Registered Classes & Sections management, Theme Toggle, Logout Button, and Developer Credit Footer Bar.
 */
@Composable
fun SettingsScreen(
    viewModel: SchoolViewModel
) {
    val clipboardManager = LocalClipboardManager.current
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val customSubjects = schoolConfig.subjectConfigs

    var schoolName by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolName) }
    var address by remember(schoolConfig) { mutableStateOf(schoolConfig.address) }
    var selectedType by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolType) }
    var selectedStage by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolStage) }
    var registeredClasses by remember(schoolConfig) { mutableStateOf(schoolConfig.registeredClasses) }
    var registeredSections by remember(schoolConfig) { mutableStateOf(schoolConfig.registeredSections) }
    var registeredSubjects by remember(schoolConfig) { mutableStateOf(schoolConfig.registeredSubjects) }
    var logoBase64 by remember(schoolConfig) { mutableStateOf(schoolConfig.logoBase64) }

    var newCustomClass by remember { mutableStateOf("") }
    var newCustomSection by remember { mutableStateOf("") }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    // Subject Customization State
    var newSubjectName by remember { mutableStateOf("") }
    var newSubjectClasses by remember(registeredClasses) { mutableStateOf(registeredClasses) }

    // Backup & Restore State
    var backupJsonText by remember { mutableStateOf("") }
    var showRestoreDialog by remember { mutableStateOf(false) }

    val isAdmin = currentUser?.role == UserRole.ADMIN

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Current User Profile Card + Logout Button
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Base64AvatarImage(
                        base64Data = currentUser?.photoBase64 ?: "",
                        fallbackIcon = Icons.Filled.AccountCircle,
                        contentDescription = currentUser?.fullName ?: "",
                        size = 56.dp
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser?.fullName ?: "مستخدم المنصة",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "الصلاحية: ${currentUser?.role?.arabicTitle ?: ""} • المستخدم: ${currentUser?.username ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "العنوان: ${currentUser?.address ?: "الموصل - زمار"} • كود المدرسة: ${schoolConfig.schoolId}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                VectorActionButton(
                    text = "تسجيل الخروج من الحساب",
                    icon = Icons.AutoMirrored.Filled.Logout,
                    containerColor = DangerRed,
                    onClick = { viewModel.logout() },
                    modifier = Modifier.fillMaxWidth(),
                    testTag = "settings_logout_button"
                )
            }
        }

        // Theme & Display Preferences Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "المظهر والعرض",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.DarkMode else Icons.Filled.LightMode,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("الوضع الليلي والنهاري (Dark / Light Mode)", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = if (isDarkMode) "الوضع الليلي مفعل حالياً" else "الوضع النهاري مفعل حالياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { viewModel.toggleDarkMode() }
                    )
                }

                OutlinedButton(
                    onClick = { viewModel.reopenOnboarding() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Filled.PlayCircleOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("استعراض شاشة الترحيب التفاعلية (Onboarding)", maxLines = 1, softWrap = false)
                }
            }
        }

        // School Settings & Registered Classes Management (Editable by Director)
        if (isAdmin) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إعدادات المدرسة والمرحلة والصفوف المسجلة (خاص بالمدير)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Filled.CloudSync, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "مسار المدرسة المعزول في Firebase: schools/${schoolConfig.schoolId}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("اسم المدرسة") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الجغرافي") },
                        placeholder = { Text("الموصل - زمار") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "نوع المدرسة (يُظهر الأقساط للأهلية ويخفيها للحكومية تلقائياً):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SchoolType.entries.forEach { type ->
                            val selected = selectedType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedType = type },
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (type == SchoolType.PRIVATE) Icons.Filled.AccountBalance else Icons.Filled.AssuredWorkload,
                                        contentDescription = null,
                                        tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type.arabicLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "المرحلة الدراسية (توليد الصفوف المسجلة تلقائياً):",
                        style = MaterialTheme.typography.labelLarge
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(SchoolStage.entries) { stage ->
                            FilterChip(
                                selected = selectedStage == stage,
                                onClick = {
                                    selectedStage = stage
                                    registeredClasses = stage.defaultClasses
                                    registeredSubjects = stage.defaultSubjects
                                },
                                label = { Text(stage.arabicLabel, maxLines = 1, softWrap = false) },
                                leadingIcon = {
                                    Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }

                    // Manage Registered Classes
                    Text(
                        text = "قائمة الصفوف المسجلة المعتمدة (${registeredClasses.size}):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(registeredClasses) { cls ->
                            InputChip(
                                selected = true,
                                onClick = {
                                    if (registeredClasses.size > 1) {
                                        registeredClasses = registeredClasses.filterNot { it == cls }
                                    }
                                },
                                label = { Text(cls, maxLines = 1, softWrap = false) },
                                trailingIcon = {
                                    Icon(Icons.Filled.Close, contentDescription = "حذف الصف", modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomClass,
                            onValueChange = { newCustomClass = it },
                            label = { Text("إضافة صف جديد للمدرسة") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newCustomClass.trim()
                                if (clean.isNotEmpty() && clean !in registeredClasses) {
                                    registeredClasses = registeredClasses + clean
                                    newCustomClass = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة صف", maxLines = 1, softWrap = false)
                        }
                    }

                    // Manage Registered Sections
                    Text(
                        text = "الشعب المسجلة (${registeredSections.joinToString("، ")}):",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = newCustomSection,
                            onValueChange = { newCustomSection = it },
                            label = { Text("إضافة شعبة (مثال: د)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newCustomSection.trim()
                                if (clean.isNotEmpty() && clean !in registeredSections) {
                                    registeredSections = registeredSections + clean
                                    newCustomSection = ""
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إضافة شعبة", maxLines = 1, softWrap = false)
                        }
                    }

                    CompressedPhotoPickerButton(
                        label = "تحديث شعار المدرسة",
                        currentBase64 = logoBase64,
                        onImageCompressed = { logoBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (savedMessage != null) {
                        Text(
                            text = savedMessage!!,
                            style = MaterialTheme.typography.labelLarge,
                            color = SuccessGreen
                        )
                    }

                    VectorActionButton(
                        text = "حفظ إعدادات المدرسة والصفوف المسجلة",
                        icon = Icons.Filled.Save,
                        onClick = {
                            viewModel.updateSchoolInfo(
                                schoolName = schoolName,
                                address = address.ifBlank { "الموصل - زمار" },
                                schoolType = selectedType,
                                logoBase64 = logoBase64,
                                schoolStage = selectedStage,
                                registeredClasses = registeredClasses,
                                registeredSections = registeredSections,
                                registeredSubjects = registeredSubjects
                            )
                            savedMessage = "تم حفظ إعدادات المدرسة وتحديث الصفوف والشعب المسجلة بنجاح"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // =========================================================================
            // REQUIREMENT 6.A: SUBJECT MANAGEMENT & CLASS ASSIGNMENT (إدارة وتخصيص المواد الدراسية)
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "إدارة وتخصيص المواد الدراسية للصفوف",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "أضف أو عدل المواد الدراسية وخصص كل مادة للصفوف المسجلة المعنية بها:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = newSubjectName,
                        onValueChange = { newSubjectName = it },
                        label = { Text("اسم المادة الدراسية (مثال: القراءة، العلوم، الفيزياء)") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("subject_name_input")
                    )

                    Text(
                        text = "تخصيص المادة للصفوف المسجلة:",
                        style = MaterialTheme.typography.labelLarge
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(schoolConfig.registeredClasses) { cls ->
                            val isSelected = cls in newSubjectClasses
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    newSubjectClasses = if (isSelected) {
                                        newSubjectClasses.filterNot { it == cls }
                                    } else {
                                        newSubjectClasses + cls
                                    }
                                },
                                label = { Text(cls, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    VectorActionButton(
                        text = "إضافة / تحديث المادة وتخصيصها للصفوف المختارة",
                        icon = Icons.Filled.BookmarkAdd,
                        onClick = {
                            viewModel.addOrUpdateSubjectConfig(
                                subjectId = null,
                                subjectName = newSubjectName,
                                assignedClasses = newSubjectClasses.ifEmpty { schoolConfig.registeredClasses }
                            )
                            newSubjectName = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "save_subject_config_button"
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    val displaySubjects = if (customSubjects.isNotEmpty()) {
                        customSubjects
                    } else {
                        schoolConfig.registeredSubjects.map { sName ->
                            SubjectConfigItem(
                                id = sName,
                                name = sName,
                                assignedClasses = schoolConfig.registeredClasses,
                                schoolId = schoolConfig.schoolId
                            )
                        }
                    }

                    displaySubjects.forEach { subjItem ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
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
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = subjItem.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "الصفوف: ${subjItem.assignedClasses.joinToString("، ").ifBlank { "جميع الصفوف" }}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Row {
                                    IconButton(
                                        onClick = {
                                            newSubjectName = subjItem.name
                                            newSubjectClasses = subjItem.assignedClasses
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Edit,
                                            contentDescription = "تعديل المادة",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteSubjectConfig(subjItem.name) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "حذف المادة",
                                            tint = DangerRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // REQUIREMENT 6.C: FULL JSON BACKUP & RESTORE (النسخ الاحتياطي والاستعادة)
            // =========================================================================
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.SettingsBackupRestore, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = "النسخ الاحتياطي والاستعادة الشاملة (JSON)",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = "يمكن للمدير تصدير نسخة احتياطية كاملة لقاعدة بيانات المدرسة (${schoolConfig.schoolId}) بصيغة JSON أو استعادتها بضغطة زر:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val exported = viewModel.exportSchoolBackupJson()
                                if (!exported.isNullOrBlank()) {
                                    backupJsonText = exported
                                    clipboardManager.setText(AnnotatedString(exported))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_backup_json_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تصدير ونسخ JSON", maxLines = 1, softWrap = false)
                        }

                        OutlinedButton(
                            onClick = { showRestoreDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("open_restore_json_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعادة من JSON", maxLines = 1, softWrap = false)
                        }
                    }

                    if (backupJsonText.isNotBlank()) {
                        OutlinedTextField(
                            value = backupJsonText,
                            onValueChange = { backupJsonText = it },
                            label = { Text("بيانات النسخة الاحتياطية (JSON) - تم نسخها للحافظة") },
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        DeveloperCreditFooter()

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showRestoreDialog) {
        var restoreInput by remember { mutableStateOf(backupJsonText) }
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("استعادة النسخة الاحتياطية لقاعدة بيانات المدرسة") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "الصق محتوى ملف النسخة الاحتياطية (JSON) لاستعادة كافة الحسابات، الصفوف، المواد، الدرجات، والسجلات فوراً:",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = restoreInput,
                        onValueChange = { restoreInput = it },
                        label = { Text("كود النسخة الاحتياطية JSON") },
                        maxLines = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "استعادة البيانات الآن",
                    icon = Icons.Filled.Restore,
                    onClick = {
                        if (viewModel.importSchoolBackupJson(restoreInput)) {
                            showRestoreDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) { Text("إلغاء") }
            }
        )
    }
}
