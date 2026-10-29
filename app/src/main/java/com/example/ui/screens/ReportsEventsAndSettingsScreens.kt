package com.example.ui.screens

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

    val visibleReports = remember(reports, currentUser) {
        when (currentUser?.role) {
            UserRole.PARENT, UserRole.STUDENT -> reports.filter {
                it.studentId == currentUser?.linkedStudentId || currentUser?.linkedStudentId.isNullOrBlank()
            }
            else -> reports
        }
    }

    val studentChats = remember(chatMessages, selectedStudent) {
        chatMessages.filter { it.studentId == selectedStudent?.id }
    }

    var draftMessage by remember { mutableStateOf("") }

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
                    .testTag("tab_official_reports"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (selectedTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Icon(Icons.Filled.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("البلاغات والتبليغات", maxLines = 1, softWrap = false)
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
            // Official Reports Section
            if (canSendReport) {
                VectorActionButton(
                    text = "إرسال بلاغ جديد لولي أمر طالب",
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
        } else {
            // Direct Messaging Inbox between Parent & Administration
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(visibleStudents, key = { it.id }) { stu ->
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
                        // Chat Header with Direct WhatsApp Button
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
                                    text = "ولي أمر الطالب: ${selectedStudent.studentName}",
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

                        // Messages List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(studentChats, key = { it.id }) { msg ->
                                val isFromAdminOrTeacher = msg.senderRole == UserRole.ADMIN || msg.senderRole == UserRole.TEACHER
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
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = msg.messageText,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
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

                        // Input Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
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
                                    if (draftMessage.isNotBlank()) {
                                        viewModel.sendChatMessage(selectedStudent, draftMessage)
                                        draftMessage = ""
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

    // Create Official Parent Report Dialog
    if (showCreateReportDialog) {
        var chosenStudent by remember { mutableStateOf(students.firstOrNull()) }
        var selectedType by remember { mutableStateOf(ReportType.APPRECIATION) }
        var title by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateReportDialog = false },
            title = { Text("إرسال بلاغ رسمي لولي الأمر") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("اختر الطالب:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(students, key = { it.id }) { s ->
                            FilterChip(
                                selected = chosenStudent?.id == s.id,
                                onClick = { chosenStudent = s },
                                label = { Text(s.studentName, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    Text("نوع البلاغ:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ReportType.entries) { rt ->
                            FilterChip(
                                selected = selectedType == rt,
                                onClick = { selectedType = rt },
                                label = { Text(rt.arabicLabel, maxLines = 1, softWrap = false) }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("عنوان البلاغ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = details,
                        onValueChange = { details = it },
                        label = { Text("نص البلاغ والتفاصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
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
 * (معرض الفعاليات والأخبار مع ضغط الصور التلقائي)
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
                            text = "ضغط تلقائي 800px • JPEG 0.7",
                            icon = Icons.Filled.HighQuality,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    }

                    Text(
                        text = "يتم ضغط أي صورة مرفوعة من الهاتف تلقائياً عبر Canvas (بحد أقصى عرض 800px وجودة 0.7 بصيغة Base64 JPEG) لتعمل فوراً وبأعلى كفاءة.",
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
                    // Event Image Banner (or Vector Decorative Banner)
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
                        label = "إرفاق صورة الفعالية (ضغط 800px • 0.7 JPEG)",
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
 * Settings Screen (صفحة الإعدادات) with School Configuration, Theme Toggle, Logout Button,
 * and the required Developer Credit Footer Bar.
 */
@Composable
fun SettingsScreen(
    viewModel: SchoolViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()

    var schoolName by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolName) }
    var address by remember(schoolConfig) { mutableStateOf(schoolConfig.address) }
    var selectedType by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolType) }
    var logoBase64 by remember(schoolConfig) { mutableStateOf(schoolConfig.logoBase64) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

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
                            text = "العنوان: ${currentUser?.address ?: "الموصل - زمار"}",
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

        // School Settings (Editable by General Director)
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
                        text = "إعدادات المدرسة ونوع النظام (خاص بالمدير العام)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

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

                    CompressedPhotoPickerButton(
                        label = "تحديث شعار المدرسة (ضغط تلقائي 800px)",
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
                        text = "حفظ إعدادات المدرسة",
                        icon = Icons.Filled.Save,
                        onClick = {
                            viewModel.updateSchoolInfo(
                                schoolName = schoolName,
                                address = address.ifBlank { "الموصل - زمار" },
                                schoolType = selectedType,
                                logoBase64 = logoBase64
                            )
                            savedMessage = "تم حفظ إعدادات المدرسة وتحديث الأقسام بنجاح"
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Developer Credit Bar at the bottom of Settings
        DeveloperCreditFooter()

        Spacer(modifier = Modifier.height(16.dp))
    }
}
