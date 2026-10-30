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
    val featureSettings by viewModel.featureSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
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

    val visibleReports = remember(reports, scopedStudents) {
        val allowedIds = scopedStudents.map { it.id }.toSet()
        reports.filter { it.studentId in allowedIds }
    }

    // Role-based contacts directory for Direct Messaging
    val allowedChatContacts = remember(accounts, students, currentUser, schoolConfig) {
        viewModel.getAllowedChatContacts(currentUser)
    }
    var chatSearchQuery by remember { mutableStateOf("") }
    var selectedRoleFilter by remember { mutableStateOf("الكل") }
    var selectedClassFilter by remember { mutableStateOf("الكل") }

    val filteredChatContacts = remember(
        allowedChatContacts,
        chatSearchQuery,
        selectedRoleFilter,
        selectedClassFilter
    ) {
        allowedChatContacts.filter { c ->
            val roleMatch = when (selectedRoleFilter) {
                "المدير", "مدير المدرسة" -> c.role == UserRole.ADMIN
                "المعلمون", "معلمو صفي" -> c.role == UserRole.TEACHER
                "الطلاب", "طلابي" -> c.role == UserRole.STUDENT
                "أولياء الأمور" -> c.role == UserRole.PARENT
                else -> true
            }
            val classMatch = selectedClassFilter == "الكل" ||
                c.gradeClass.contains(selectedClassFilter, ignoreCase = true) ||
                c.subtitle.contains(selectedClassFilter, ignoreCase = true)
            val query = chatSearchQuery.trim()
            val queryMatch = query.isEmpty() ||
                c.fullName.contains(query, ignoreCase = true) ||
                c.subtitle.contains(query, ignoreCase = true) ||
                c.gradeClass.contains(query, ignoreCase = true) ||
                c.linkedStudentName.contains(query, ignoreCase = true)
            roleMatch && classMatch && queryMatch
        }
    }

    var selectedContactAccountId by remember(allowedChatContacts) {
        mutableStateOf(allowedChatContacts.firstOrNull()?.accountId ?: "")
    }
    val selectedContact = remember(filteredChatContacts, allowedChatContacts, selectedContactAccountId) {
        filteredChatContacts.firstOrNull { it.accountId == selectedContactAccountId }
            ?: allowedChatContacts.firstOrNull { it.accountId == selectedContactAccountId }
            ?: filteredChatContacts.firstOrNull()
            ?: allowedChatContacts.firstOrNull()
    }

    val activeConversationMessages = remember(chatMessages, currentUser, selectedContact) {
        val user = currentUser
        val contact = selectedContact
        if (user == null || contact == null) {
            emptyList()
        } else {
            chatMessages.filter { msg -> msg.matchesConversation(user, contact) }
        }
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

            if (featureSettings.directChatEnabled) {
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
            // =========================================================================
            // Role-Based Direct Messaging & Smart Search (المراسلة والمحادثات حسب الدور)
            // =========================================================================
            val searchPlaceholder = when (currentUser?.role) {
                UserRole.ADMIN -> "ابحث بالاسم أو الصف عن (معلم، طالب، أو ولي أمر)..."
                UserRole.TEACHER -> "ابحث عن المدير أو طلاب وأولياء أمور صفوفك المسندة..."
                else -> "ابحث عن مدير المدرسة أو معلمي صفك وشعبتك..."
            }

            val roleFilterOptions = remember(currentUser?.role) {
                when (currentUser?.role) {
                    UserRole.ADMIN -> listOf("الكل", "المعلمون", "الطلاب", "أولياء الأمور")
                    UserRole.TEACHER -> listOf("الكل", "مدير المدرسة", "طلابي", "أولياء الأمور")
                    else -> listOf("الكل", "مدير المدرسة", "معلمو صفي")
                }
            }

            OutlinedTextField(
                value = chatSearchQuery,
                onValueChange = { chatSearchQuery = it },
                placeholder = { Text(searchPlaceholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "بحث") },
                trailingIcon = {
                    if (chatSearchQuery.isNotEmpty()) {
                        IconButton(onClick = { chatSearchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("chat_contacts_search_input")
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Role Filter Chips
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(roleFilterOptions) { roleOpt ->
                    FilterChip(
                        selected = selectedRoleFilter == roleOpt,
                        onClick = { selectedRoleFilter = roleOpt },
                        label = { Text(roleOpt, maxLines = 1, softWrap = false) },
                        leadingIcon = {
                            val icon = when (roleOpt) {
                                "المدير", "مدير المدرسة" -> Icons.Filled.AdminPanelSettings
                                "المعلمون", "معلمو صفي" -> Icons.Filled.Badge
                                "الطلاب", "طلابي" -> Icons.Filled.School
                                "أولياء الأمور" -> Icons.Filled.FamilyRestroom
                                else -> Icons.Filled.Groups
                            }
                            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                }

                // Class filter chips for Admin & Teacher
                if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER) {
                    val allowedCls = viewModel.getAllowedClassesForUser(currentUser)
                    items(allowedCls) { cls ->
                        FilterChip(
                            selected = selectedClassFilter == cls,
                            onClick = {
                                selectedClassFilter = if (selectedClassFilter == cls) "الكل" else cls
                            },
                            label = { Text(cls, maxLines = 1, softWrap = false) },
                            leadingIcon = {
                                Icon(Icons.Filled.Class, contentDescription = null, modifier = Modifier.size(15.dp))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Selectable Contacts Strip (One-Tap Direct Conversation Starter)
            if (filteredChatContacts.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "لا توجد جهات اتصال مطابقة للبحث ضمن صلاحيات حسابك.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filteredChatContacts, key = { it.accountId }) { contact ->
                        val isSelected = selectedContact?.accountId == contact.accountId
                        val roleColor = when (contact.role) {
                            UserRole.ADMIN -> GoldDeep
                            UserRole.TEACHER -> InfoBlue
                            UserRole.STUDENT -> SuccessGreen
                            UserRole.PARENT -> MaterialTheme.colorScheme.primary
                        }
                        val roleIcon = when (contact.role) {
                            UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                            UserRole.TEACHER -> Icons.Filled.Badge
                            UserRole.STUDENT -> Icons.Filled.School
                            UserRole.PARENT -> Icons.Filled.FamilyRestroom
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { selectedContactAccountId = contact.accountId }
                                .testTag("chat_contact_${contact.accountId}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Base64AvatarImage(
                                    base64Data = contact.photoBase64,
                                    fallbackIcon = roleIcon,
                                    contentDescription = contact.fullName,
                                    size = 34.dp,
                                    tint = roleColor
                                )
                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = contact.fullName,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                        Text(
                                            text = "• ${contact.role.arabicTitle}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = roleColor,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                    Text(
                                        text = contact.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (selectedContact != null) {
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Base64AvatarImage(
                                    base64Data = selectedContact.photoBase64,
                                    fallbackIcon = Icons.Filled.Person,
                                    contentDescription = selectedContact.fullName,
                                    size = 42.dp,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "محادثة مباشرة: ${selectedContact.fullName}",
                                        style = MaterialTheme.typography.titleSmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = selectedContact.subtitle,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (selectedContact.phone.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WhatsAppGreen,
                                    modifier = Modifier.clickable {
                                        ExternalIntents.sendWhatsAppMessage(
                                            context,
                                            selectedContact.phone,
                                            "السلام عليكم ${selectedContact.fullName}، نتواصل معكم عبر منصة ${schoolConfig.schoolName}."
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
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        if (activeConversationMessages.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Filled.Forum,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "ابدأ المحادثة الفورية مع ${selectedContact.fullName}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "تصل الرسائل والإشعارات لحظياً للمستلم المعني فقط",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(activeConversationMessages, key = { it.id }) { msg ->
                                    val isSentByMe = if (msg.senderId.isNotEmpty()) {
                                        msg.senderId == currentUser?.id
                                    } else {
                                        msg.senderRole == currentUser?.role
                                    }
                                    val msgBitmap = remember(msg.imageBase64) {
                                        ImageCompressor.decodeBase64ToImageBitmap(msg.imageBase64)
                                    }
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isSentByMe) Alignment.Start else Alignment.End
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(14.dp),
                                            color = if (isSentByMe) MaterialTheme.colorScheme.primaryContainer
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
                                                            .clickable {
                                                                GlobalImageLightboxController.open(
                                                                    msg.imageBase64,
                                                                    "صورة مرفقة - ${msg.senderName}"
                                                                )
                                                            }
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
                                placeholder = { Text("اكتب رسالتك إلى ${selectedContact.fullName}...") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("chat_message_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Button(
                                onClick = {
                                    if (draftMessage.isNotBlank() || draftChatImageBase64.isNotBlank()) {
                                        viewModel.sendDirectChatMessage(selectedContact, draftMessage, draftChatImageBase64)
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
            val isLikedByMe = currentUser?.id != null && currentUser?.id in ev.likedByUserIds

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
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        GlobalImageLightboxController.open(ev.imageBase64, ev.title)
                                    }
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
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))

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
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "الناشر: ${ev.authorName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            HonorHeartReactionButton(
                                heartCount = ev.heartCount,
                                isLikedByMe = isLikedByMe,
                                onReact = { viewModel.toggleEventHeartReaction(ev.id) },
                                emptyLabel = "إعجاب",
                                testTag = "event_heart_button_${ev.id}"
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
    val featureSettings by viewModel.featureSettings.collectAsState()
    val privateSuggestions by viewModel.privateSuggestions.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSideDrawerEnabled by viewModel.isSideDrawerEnabled.collectAsState()
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

    // Private suggestion input
    var suggestionDraft by remember { mutableStateOf("") }

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

        // Theme & Display Preferences Card (includes Side Navigation Drawer toggle)
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
                    text = "المظهر والعرض والتنقل",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
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

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.MenuOpen,
                            contentDescription = null,
                            tint = DeepEmerald
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "تفعيل القائمة الجانبية السريعة (Side Navigation Drawer)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isSideDrawerEnabled) "مفعلة (تظهر أيقونة ☰ أعلى الشاشة للانتقال الفوري)" else "مخفية حالياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = isSideDrawerEnabled,
                        onCheckedChange = { viewModel.toggleSideDrawerEnabled() },
                        modifier = Modifier.testTag("toggle_side_drawer_switch")
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

        // Private Principal Suggestions Box (صندوق المقترحات والشكاوى السري للمدير)
        if (featureSettings.suggestionsBoxEnabled) {
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
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Lock, contentDescription = null, tint = DeepEmerald)
                            Column {
                                Text(
                                    text = "صندوق المقترحات والملاحظات السري للمدير",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepEmerald
                                )
                                Text(
                                    text = "سري للغاية — يطلع عليه مدير المدرسة حصراً دون أي طرف آخر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        NoWrapBadge(
                            text = "سري 🔒",
                            icon = Icons.Filled.VerifiedUser,
                            containerColor = DeepEmerald.copy(alpha = 0.14f),
                            contentColor = DeepEmerald
                        )
                    }

                    if (!isAdmin) {
                        OutlinedTextField(
                            value = suggestionDraft,
                            onValueChange = { suggestionDraft = it },
                            label = { Text("اكتب مقترحك أو ملاحظتك السرية لمدير المدرسة...") },
                            minLines = 2,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("private_suggestion_input")
                        )
                        VectorActionButton(
                            text = "إرسال المقترح السري لمدير المدرسة 🔒",
                            icon = Icons.AutoMirrored.Filled.Send,
                            containerColor = DeepEmerald,
                            onClick = {
                                if (suggestionDraft.isNotBlank()) {
                                    viewModel.submitPrivateSuggestionToPrincipal(suggestionDraft)
                                    suggestionDraft = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "submit_private_suggestion_btn"
                        )
                    } else {
                        if (privateSuggestions.isEmpty()) {
                            Text(
                                text = "لا توجد مقترحات أو شكاوى سرية واردة حالياً.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "الرسائل السرية الواردة للمدير (${privateSuggestions.size}):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepEmerald
                            )
                            privateSuggestions.take(10).forEach { sug ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DeepEmerald.copy(alpha = 0.06f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "${sug.senderName} (${sug.senderRoleArabic})",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = DeepEmerald
                                            )
                                            Text(
                                                text = sug.createdAt,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = sug.messageText,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Principal Modular Feature Toggles Card (لوحة تحكم المدير في تفعيل أو إيقاف مزايا مدرسته)
        if (isAdmin) {
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
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Tune, contentDescription = null, tint = DeepEmerald)
                        Column {
                            Text(
                                text = "التحكم في مزايا وأقسام المنصة (لوحة المدير المرنة)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeepEmerald
                            )
                            Text(
                                text = "قم بتفعيل أو إيقاف أي ميزة في مدرستك بضغطة زر؛ تختفي الميزة الموقوفة تلقائياً من حسابات الطلاب والمعلمين.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    val featureRows = listOf(
                        Triple("ميزة الخطوط والنقل المدرسي 🚌", featureSettings.busAlertsEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(busAlertsEnabled = v))
                        },
                        Triple("ميزة لوحة الشرف والتفاعل بالقلب 🏆", featureSettings.honorRollEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(honorRollEnabled = v))
                        },
                        Triple("ميزة المراسلة والمحادثات المباشرة 💬", featureSettings.directChatEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(directChatEnabled = v))
                        },
                        Triple("ميزة المكتبة الرقمية والملخصات 📚", featureSettings.studyLibraryEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(studyLibraryEnabled = v))
                        },
                        Triple("ميزة طلبات الإجازة الإلكترونية 📝", featureSettings.leaveRequestsEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(leaveRequestsEnabled = v))
                        },
                        Triple("ميزة الماسح الضوئي للهويات (QR Attendance) 📷", featureSettings.qrScannerEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(qrScannerEnabled = v))
                        },
                        Triple("ميزة الأقساط الدراسية والوصولات 💰", featureSettings.tuitionEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(tuitionEnabled = v))
                        },
                        Triple("ميزة تسليم الواجبات بالصورة 📸", featureSettings.homeworkSubmissionEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(homeworkSubmissionEnabled = v))
                        },
                        Triple("ميزة دفتر المتابعة اليومي والسلوك 🌟", featureSettings.dailyBehaviorEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(dailyBehaviorEnabled = v))
                        },
                        Triple("ميزة النداء الذكي للانصراف (استلام الأبناء) 🚗", featureSettings.dismissalCallEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(dismissalCallEnabled = v))
                        },
                        Triple("ميزة الاختبارات السريعة المؤقتة ⚡", featureSettings.quickQuizzesEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(quickQuizzesEnabled = v))
                        },
                        Triple("ميزة سجل المفقودات وصندوق المقترحات 🔎", featureSettings.lostAndFoundEnabled) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(lostAndFoundEnabled = v, suggestionsBoxEnabled = v)
                            )
                        }
                    )

                    featureRows.forEachIndexed { idx, (label, isChecked, onToggle) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = isChecked,
                                onCheckedChange = onToggle,
                                modifier = Modifier.testTag("feature_toggle_switch_$idx")
                            )
                        }
                    }
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
