package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
 * - Expanded Full-Height Chat View on contact selection with auto-collapsed filters & slim single-line composer
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
    var isChatConversationOpen by remember { mutableStateOf(false) }
    var isFullScreenChatModal by remember { mutableStateOf(false) }

    LaunchedEffect(featureSettings.directChatEnabled) {
        if (!featureSettings.directChatEnabled) {
            if (selectedTab == 1) selectedTab = 0
            isChatConversationOpen = false
            isFullScreenChatModal = false
        }
    }

    BackHandler(enabled = selectedTab == 1 && isChatConversationOpen) {
        isChatConversationOpen = false
    }

    val canSendReport = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER
    val isParentOrStudent = currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT

    val scopedStudents = remember(students, currentUser) {
        viewModel.getScopedStudentsForUser(currentUser)
    }

    val visibleReports = remember(reports, scopedStudents) {
        val allowedIds = scopedStudents.map { it.id }.toSet()
        reports.filter { it.studentId in allowedIds }
            .sortedByDescending { it.createdAt }
    }

    // التحديث التلقائي لحالة القراءة فور فتح ولي الأمر أو الطالب لقسم البلاغات
    LaunchedEffect(visibleReports, isParentOrStudent, selectedTab) {
        if (isParentOrStudent && selectedTab == 0) {
            val unreadIds = visibleReports.filter { !it.isReadByParent }.map { it.id }.toSet()
            if (unreadIds.isNotEmpty()) {
                viewModel.markReportsAsReadByParent(unreadIds)
            }
        }
    }

    var reportSearchQuery by remember { mutableStateOf("") }
    var reportIndexMode by remember { mutableIntStateOf(0) } // 0 = مفهرس حسب التاريخ, 1 = مفهرس حسب الطالب, 2 = غير المقروءة بعد, 3 = تمت قراءتها

    val filteredAndIndexedReports = remember(visibleReports, reportSearchQuery, reportIndexMode) {
        val q = reportSearchQuery.trim()
        val matching = visibleReports.filter { rep ->
            val matchesQuery = q.isEmpty() ||
                rep.studentName.contains(q, ignoreCase = true) ||
                rep.gradeClass.contains(q, ignoreCase = true) ||
                rep.title.contains(q, ignoreCase = true) ||
                rep.details.contains(q, ignoreCase = true) ||
                rep.parentName.contains(q, ignoreCase = true)
            val matchesReadFilter = when (reportIndexMode) {
                2 -> !rep.isReadByParent
                3 -> rep.isReadByParent
                else -> true
            }
            matchesQuery && matchesReadFilter
        }
        when (reportIndexMode) {
            1 -> matching.sortedWith(compareBy<ParentReport> { it.studentName }.thenByDescending { it.createdAt })
            else -> matching.sortedByDescending { it.createdAt }
        }
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
            c.accountId.isNotBlank() && roleMatch && classMatch && queryMatch
        }.distinctBy { it.accountId }
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

    val deepLinkTarget by viewModel.deepLinkTarget.collectAsState()
    LaunchedEffect(deepLinkTarget, allowedChatContacts) {
        val target = deepLinkTarget
        if (target != null && target.section == AppSection.REPORTS_CHAT) {
            selectedTab = target.tabIndex.coerceIn(0, 1)
            if (target.tabIndex == 1 && target.itemId.isNotBlank()) {
                val matchingContact = allowedChatContacts.firstOrNull {
                    it.accountId == target.itemId || it.linkedStudentId == target.itemId
                }
                if (matchingContact != null) {
                    selectedContactAccountId = matchingContact.accountId
                    isChatConversationOpen = true
                }
            }
            viewModel.clearDeepLinkTarget()
        }
    }

    val activeConversationMessages = remember(chatMessages, currentUser, selectedContact) {
        val user = currentUser
        val contact = selectedContact
        if (user == null || contact == null) {
            emptyList()
        } else {
            chatMessages
                .filter { msg -> msg.id.isNotBlank() && msg.matchesConversation(user, contact) }
                .distinctBy { it.id }
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

    @Composable
    fun ExpandedConversationContent(
        contact: ChatContactItem,
        isInsideModal: Boolean,
        modifier: Modifier = Modifier
    ) {
        val listState = rememberLazyListState()
        LaunchedEffect(activeConversationMessages.size, contact.accountId) {
            if (activeConversationMessages.isNotEmpty()) {
                listState.animateScrollToItem(activeConversationMessages.size - 1)
            }
        }

        Card(
            modifier = modifier.fillMaxSize(),
            shape = RoundedCornerShape(if (isInsideModal) 22.dp else 18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.22f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                // Sleek Compact Conversation Header with Back Button ("رجوع لقائمة المراسلة")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DeepEmerald.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.30f)),
                        modifier = Modifier
                            .clickable {
                                if (isInsideModal) {
                                    isFullScreenChatModal = false
                                } else {
                                    isChatConversationOpen = false
                                }
                            }
                            .testTag("chat_back_to_contacts_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "رجوع لقائمة المراسلة",
                                tint = DeepEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "رجوع لقائمة المراسلة",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Base64AvatarImage(
                            base64Data = contact.photoBase64,
                            fallbackIcon = Icons.Filled.Person,
                            contentDescription = contact.fullName,
                            size = 34.dp,
                            tint = DeepEmerald
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contact.fullName,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = contact.subtitle,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (contact.phone.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = WhatsAppGreen,
                                modifier = Modifier.clickable {
                                    ExternalIntents.sendWhatsAppMessage(
                                        context,
                                        contact.phone,
                                        "السلام عليكم ${contact.fullName}، نتواصل معكم عبر منصة ${schoolConfig.schoolName}."
                                    )
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Chat,
                                        contentDescription = "واتساب",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "واتساب",
                                        color = Color.White,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (!isInsideModal) {
                            IconButton(
                                onClick = { isFullScreenChatModal = true },
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DeepEmerald.copy(alpha = 0.08f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.OpenInFull,
                                    contentDescription = "تكبير المحادثة لملء الشاشة",
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = DeepEmerald.copy(alpha = 0.14f)
                )

                // Spacious Full-Height Messages Area
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
                                tint = DeepEmerald.copy(alpha = 0.40f),
                                modifier = Modifier.size(46.dp)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "ابدأ المحادثة الفورية مع ${contact.fullName}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
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
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp)
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
                                    shape = RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isSentByMe) 4.dp else 16.dp,
                                        bottomEnd = if (isSentByMe) 16.dp else 4.dp
                                    ),
                                    color = if (isSentByMe) {
                                        DeepEmerald.copy(alpha = 0.12f)
                                    } else {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    },
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSentByMe) DeepEmerald.copy(alpha = 0.28f)
                                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
                                    ),
                                    modifier = Modifier.widthIn(max = 300.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepEmerald,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        if (msgBitmap != null) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Image(
                                                bitmap = msgBitmap,
                                                contentDescription = "صورة مرفقة",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(155.dp)
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
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text(
                                                text = msg.messageText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface
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

                Spacer(modifier = Modifier.height(6.dp))

                if (draftChatImageBase64.isNotBlank()) {
                    Surface(
                        color = DeepEmerald.copy(alpha = 0.10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "تم إرفاق صورة جاهزة للإرسال",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepForestGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { draftChatImageBase64 = "" },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "إزالة الصورة", modifier = Modifier.size(15.dp))
                            }
                        }
                    }
                }

                // Slim, Modern Single-Line Bottom Composer Bar (زر إرفاق صورة + حقل نص مريح + زر الإرسال)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = {
                            chatPhotoLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DeepEmerald.copy(alpha = 0.10f))
                            .testTag("chat_attach_photo_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddPhotoAlternate,
                            contentDescription = "إرفاق صورة",
                            tint = DeepEmerald,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        placeholder = {
                            Text(
                                text = "اكتب رسالتك إلى ${contact.fullName}...",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("chat_message_input"),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeepEmerald,
                            unfocusedBorderColor = DeepEmerald.copy(alpha = 0.30f)
                        )
                    )

                    Button(
                        onClick = {
                            if (draftMessage.isNotBlank() || draftChatImageBase64.isNotBlank()) {
                                viewModel.sendDirectChatMessage(contact, draftMessage, draftChatImageBase64)
                                draftMessage = ""
                                draftChatImageBase64 = ""
                            }
                        },
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("send_chat_button"),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepEmerald,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "إرسال",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "إرسال",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }

    if (isFullScreenChatModal && selectedContact != null) {
        Dialog(
            onDismissRequest = { isFullScreenChatModal = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.96f)
                    .fillMaxHeight(0.92f)
            ) {
                ExpandedConversationContent(
                    contact = selectedContact,
                    isInsideModal = true
                )
            }
        }
    }

    var reportBeingEdited by remember { mutableStateOf<ParentReport?>(null) }
    var reportsVisibleCount by remember(filteredAndIndexedReports.size, reportIndexMode, reportSearchQuery) { mutableStateOf(8) }
    val displayedReports = remember(filteredAndIndexedReports, reportsVisibleCount) {
        filteredAndIndexedReports.take(reportsVisibleCount)
    }
    var contactsVisibleCount by remember(chatSearchQuery, selectedRoleFilter, selectedClassFilter) {
        mutableStateOf(6)
    }
    val displayedChatContacts = remember(filteredChatContacts, contactsVisibleCount) {
        filteredChatContacts.take(contactsVisibleCount)
    }

    if (selectedTab == 1 && isChatConversationOpen && selectedContact != null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            ExpandedConversationContent(
                contact = selectedContact,
                isInsideModal = false,
                modifier = Modifier.fillMaxSize()
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
        ) {
            item {
                ThreeDHeroHeaderCard(
                    title = "البلاغات والمراسلة المباشرة",
                    subtitle = "التواصل التربوي المباشر بين الإدارة والكادر التدريسي وأولياء الأمور والطلبة",
                    rightPillText = if (isParentOrStudent) "بلاغاتي الخاصة" else "البلاغات الرسمية",
                    rightPillIcon = Icons.Filled.Campaign,
                    leftPillText = "مراسلة فورية",
                    leftPillIcon = Icons.Filled.Forum,
                    illustrationType = ThreeDIllustrationType.DIRECT_CHAT_ENVELOPE_3D
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isTab0 = selectedTab == 0
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isTab0) Color(0xFF059669) else Color.White,
                        border = BorderStroke(1.2.dp, if (isTab0) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                        shadowElevation = if (isTab0) 6.dp else 2.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                selectedTab = 0
                                isChatConversationOpen = false
                            }
                            .testTag("tab_official_reports")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = null,
                                tint = if (isTab0) Color.White else Color(0xFF047857),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isParentOrStudent) "بلاغاتي الخاصة" else "البلاغات والتبليغات",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isTab0) Color.White else Color(0xFF0F172A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    if (featureSettings.directChatEnabled) {
                        val isTab1 = selectedTab == 1
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isTab1) Color(0xFF059669) else Color.White,
                            border = BorderStroke(1.2.dp, if (isTab1) Color(0xFFA7F3D0) else Color(0xFFE2E8F0)),
                            shadowElevation = if (isTab1) 6.dp else 2.dp,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedTab = 1 }
                                .testTag("tab_direct_chat")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Forum,
                                    contentDescription = null,
                                    tint = if (isTab1) Color.White else Color(0xFF047857),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "المراسلة المباشرة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isTab1) Color.White else Color(0xFF0F172A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                if (canSendReport) {
                    item {
                        VectorActionButton(
                            text = "إرسال بلاغ رسمي جديد",
                            icon = Icons.Filled.MarkChatUnread,
                            onClick = { showCreateReportDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "create_report_button"
                        )
                    }
                }

                // شريط الفهرسة والبحث الذكي للبلاغات (Search & 4 Indexing/Read-Status Chips)
                item(key = "reports_smart_indexing_and_search_bar") {
                    val unreadCount = remember(visibleReports) { visibleReports.count { !it.isReadByParent } }
                    val readCount = remember(visibleReports) { visibleReports.count { it.isReadByParent } }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.2.dp, Color(0xFF059669).copy(alpha = 0.28f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedTextField(
                                value = reportSearchQuery,
                                onValueChange = { reportSearchQuery = it },
                                placeholder = {
                                    Text(
                                        text = "ابحث باسم الطالب، الصف، أو عنوان ومحتوى البلاغ...",
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "بحث في البلاغات",
                                        tint = Color(0xFF059669)
                                    )
                                },
                                trailingIcon = {
                                    if (reportSearchQuery.isNotEmpty()) {
                                        IconButton(onClick = { reportSearchQuery = "" }) {
                                            Icon(Icons.Filled.Clear, contentDescription = "مسح البحث")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("reports_search_input"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF059669),
                                    unfocusedBorderColor = Color(0xFF059669).copy(alpha = 0.30f)
                                )
                            )

                            val indexTabs = listOf(
                                Triple(0, "مفهرس حسب التاريخ", Icons.Filled.CalendarMonth),
                                Triple(1, "مفهرس حسب الطالب", Icons.Filled.PersonSearch),
                                Triple(2, "غير المقروءة بعد ($unreadCount)", Icons.Filled.Schedule),
                                Triple(3, "تمت قراءتها ($readCount)", Icons.Filled.DoneAll)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(indexTabs, key = { it.first }) { (modeIdx, label, iconVec) ->
                                    val isSelected = reportIndexMode == modeIdx
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = if (isSelected) Color(0xFF059669) else Color(0xFFF8FAFC),
                                        border = BorderStroke(
                                            1.dp,
                                            if (isSelected) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)
                                        ),
                                        shadowElevation = if (isSelected) 4.dp else 1.dp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { reportIndexMode = modeIdx }
                                            .testTag("reports_index_chip_$modeIdx")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = iconVec,
                                                contentDescription = null,
                                                tint = if (isSelected) Color.White else Color(0xFF059669),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = label,
                                                fontSize = 11.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) Color.White else Color(0xFF0F172A),
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

                if (displayedReports.isEmpty()) {
                    item(key = "empty_filtered_reports") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.20f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Campaign,
                                    contentDescription = null,
                                    tint = DeepEmerald.copy(alpha = 0.55f),
                                    modifier = Modifier.size(40.dp)
                                )
                                Text(
                                    text = "لا توجد بلاغات مطابقة للفلتر أو البحث الحالي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DeepForestGreen
                                )
                            }
                        }
                    }
                }

                items(displayedReports, key = { it.id }) { rep ->
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
                    val studentReportsCount = remember(visibleReports, rep.studentId) {
                        visibleReports.count { it.studentId == rep.studentId }
                    }

                    // بطاقة البلاغ الرسمي المفهرسة مع شارة تأكيد قراءة ولي الأمر
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.28f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // شريط الفهرس العلوي (حسب التاريخ أو حسب الطالب)
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
                                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = if (reportIndexMode == 1) Icons.Filled.Person else Icons.Filled.EventNote,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = if (reportIndexMode == 1) {
                                                "فهرس الطالب: ${rep.studentName} • (${rep.gradeClass}) • ($studentReportsCount بلاغ)"
                                            } else {
                                                "فهرس تاريخ البلاغ: ${rep.createdAt}"
                                            },
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = DeepForestGreen,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "المرسل: ${rep.senderName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
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
                                    text = rep.reportType.arabicLabel,
                                    icon = badgeIcon,
                                    containerColor = badgeColor.copy(alpha = 0.15f),
                                    contentColor = badgeColor
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = rep.createdAt,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                }
                            }

                            // شارة زجاجية موحدة ونظيفة لحالة اطلاع ولي الأمر (بدون أيقونة الساعة الرملية وبدون تكرار)
                            val pulseTransition = rememberInfiniteTransition(label = "parent_read_pulse_${rep.id}")
                            val dotAlpha by pulseTransition.animateFloat(
                                initialValue = 0.35f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(950, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "amber_dot_alpha"
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (rep.isReadByParent) {
                                    Color(0xFFECFDF5)
                                } else {
                                    Color(0xFFFFFBEB)
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (rep.isReadByParent) Color(0xFF10B981).copy(alpha = 0.50f) else Color(0xFFF59E0B).copy(alpha = 0.48f)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("report_read_receipt_${rep.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        if (!rep.isReadByParent) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF59E0B).copy(alpha = dotAlpha))
                                            )
                                        }
                                        Text(
                                            text = if (rep.isReadByParent) {
                                                "تمت القراءة من ولي الأمر ✓✓"
                                            } else {
                                                "بانتظار اطلاع ولي الأمر"
                                            },
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (rep.isReadByParent) Color(0xFF065F46) else Color(0xFF92400E)
                                        )
                                    }
                                    if (rep.isReadByParent && rep.readAtTimestamp.isNotBlank()) {
                                        Text(
                                            text = rep.readAtTimestamp,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            }

                            // اسم الطالب والصف وولي الأمر بوضوح تام
                            Surface(
                                color = DeepEmerald.copy(alpha = 0.07f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Person,
                                        contentDescription = null,
                                        tint = DeepEmerald,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "الطالب: ${rep.studentName} (${rep.gradeClass})  •  ولي الأمر: ${rep.parentName}",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepForestGreen
                                    )
                                }
                            }

                            Text(
                                text = rep.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = DeepForestGreen
                            )

                            Surface(
                                color = Color(0xFFF8FAFC),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.14f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = rep.details,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF1E293B),
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
                                            من: ${schoolConfig.schoolName}
                                            إلى السيد ولي أمر الطالب (${rep.studentName} - ${rep.gradeClass}) المحترم:
                                            • الموضوع: ${rep.title}
                                            • التفاصيل: ${rep.details}
                                            • المرسل: ${rep.senderName}
                                        """.trimIndent()
                                        ExternalIntents.sendWhatsAppMessage(context, rep.parentPhone, msg)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )

                                // أزرار "تعديل" و "حذف" البلاغ المرسل
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { reportBeingEdited = rep },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("edit_report_${rep.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald)
                                    ) {
                                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("تعديل البلاغ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.deleteParentReport(rep.id) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("delete_report_${rep.id}"),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حذف البلاغ", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                if (displayedReports.size < filteredAndIndexedReports.size) {
                    item {
                        ProgressiveInfiniteScrollFooter(
                            visibleCount = displayedReports.size,
                            totalCount = filteredAndIndexedReports.size,
                            step = 6,
                            itemLabel = "بلاغ",
                            onLoadMore = {
                                reportsVisibleCount = (reportsVisibleCount + 6).coerceAtMost(filteredAndIndexedReports.size)
                            }
                        )
                    }
                }
            } else {
                // =========================================================================
                // Role-Based Direct Messaging & Smart Search (المراسلة والمحادثات حسب الدور)
                // Scrolls smoothly with the 3D Hero Header & Tabs inside the unified LazyColumn
                // =========================================================================
                item {
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

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
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
                    }
                }

                if (filteredChatContacts.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.12f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = DeepEmerald
                                )
                                Text(
                                    text = "لا توجد جهات اتصال مطابقة للبحث ضمن صلاحيات حسابك.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(displayedChatContacts, key = { it.accountId }) { contact ->
                            val roleColor = when (contact.role) {
                                UserRole.ADMIN -> DeepEmerald
                                UserRole.TEACHER -> InfoBlue
                                UserRole.STUDENT -> SuccessGreen
                                UserRole.PARENT -> DeepEmerald
                            }
                            val roleIcon = when (contact.role) {
                                UserRole.ADMIN -> Icons.Filled.AdminPanelSettings
                                UserRole.TEACHER -> Icons.Filled.Badge
                                UserRole.STUDENT -> Icons.Filled.School
                                UserRole.PARENT -> Icons.Filled.FamilyRestroom
                            }
                            val lastMsgForContact = remember(chatMessages, currentUser, contact) {
                                val user = currentUser
                                if (user == null) null
                                else chatMessages.lastOrNull { it.matchesConversation(user, contact) }
                            }

                            Surface(
                                onClick = {
                                    selectedContactAccountId = contact.accountId
                                    isChatConversationOpen = true
                                },
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.12f)),
                                shadowElevation = 0.dp,
                                tonalElevation = 0.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("chat_contact_${contact.accountId}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Right side (in RTL): Circular Avatar + Name + Soft Role Pill + Class/Section & Last Message
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Base64AvatarImage(
                                            base64Data = contact.photoBase64,
                                            fallbackIcon = roleIcon,
                                            contentDescription = contact.fullName,
                                            size = 44.dp,
                                            tint = roleColor
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = contact.fullName.ifBlank { "مستخدم" },
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = roleColor.copy(alpha = 0.10f),
                                                    shadowElevation = 0.dp
                                                ) {
                                                    Text(
                                                        text = contact.role.arabicTitle,
                                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = roleColor,
                                                        fontWeight = FontWeight.SemiBold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                            Text(
                                                text = contact.subtitle.ifBlank { contact.role.arabicTitle },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (lastMsgForContact != null) {
                                                Text(
                                                    text = lastMsgForContact.messageText.ifBlank { "صورة مرفقة" },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }

                                    // Left side (in RTL): Last message time + subtle clean chat/arrow icon only
                                    Column(
                                        horizontalAlignment = Alignment.End,
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        if (lastMsgForContact != null && lastMsgForContact.timestamp.isNotBlank()) {
                                            Text(
                                                text = lastMsgForContact.timestamp,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Filled.ChatBubbleOutline,
                                            contentDescription = "فتح المحادثة",
                                            tint = DeepEmerald.copy(alpha = 0.75f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                        if (displayedChatContacts.size < filteredChatContacts.size) {
                            item {
                                ProgressiveInfiniteScrollFooter(
                                    visibleCount = displayedChatContacts.size,
                                    totalCount = filteredChatContacts.size,
                                    step = 6,
                                    itemLabel = "جهة اتصال",
                                    onLoadMore = {
                                        contactsVisibleCount = (contactsVisibleCount + 6).coerceAtMost(filteredChatContacts.size)
                                    }
                                )
                            }
                        }
                    }
                }
        }
    }

    reportBeingEdited?.let { editingRep ->
        var editType by remember(editingRep.id) { mutableStateOf(editingRep.reportType) }
        var editTitle by remember(editingRep.id) { mutableStateOf(editingRep.title) }
        var editDetails by remember(editingRep.id) { mutableStateOf(editingRep.details) }

        AlertDialog(
            onDismissRequest = { reportBeingEdited = null },
            title = { Text("تعديل البلاغ الرسمي (${editingRep.studentName})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("نوع البلاغ:", style = MaterialTheme.typography.labelLarge)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(ReportType.entries) { rt ->
                            FilterChip(
                                selected = editType == rt,
                                onClick = { editType = rt },
                                label = { Text(rt.arabicLabel, maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان البلاغ") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editDetails,
                        onValueChange = { editDetails = it },
                        label = { Text("نص البلاغ والتفاصيل") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ التعديلات",
                    icon = Icons.Filled.Save,
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateParentReport(
                                reportId = editingRep.id,
                                reportType = editType,
                                title = editTitle,
                                details = editDetails.ifBlank { editTitle }
                            )
                            reportBeingEdited = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { reportBeingEdited = null }) { Text("إلغاء") }
            }
        )
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
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val rawEvents by viewModel.events.collectAsState()
    val events = remember(rawEvents, schoolConfig.schoolId) {
        val activeSid = schoolConfig.schoolId.trim()
        rawEvents.filter { ev ->
            if (activeSid.isNotEmpty()) {
                ev.schoolId.trim() == activeSid
            } else {
                ev.schoolId.isBlank()
            }
        }
    }

    var showPublishEventDialog by remember { mutableStateOf(false) }
    var eventBeingEdited by remember { mutableStateOf<SchoolEvent?>(null) }
    val canPublish = currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER

    var eventsVisibleCount by remember(events.size) { mutableStateOf(6) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)
    ) {
        item {
            ThreeDHeroHeaderCard(
                title = "معرض الفعاليات والأخبار المدرسية",
                subtitle = "تابع أحدث الفعاليات والأنشطة العلمية والثقافية والأخبار الرسمية للمدرسة.",
                rightPillText = "توثيق الأنشطة والأخبار",
                rightPillIcon = Icons.Filled.PhotoLibrary,
                leftPillText = schoolConfig.academicYear,
                leftPillIcon = Icons.Filled.Newspaper,
                illustrationType = ThreeDIllustrationType.MEGAPHONE,
                bottomActionText = if (canPublish) "نشر فعالية أو خبر مدرسي مع صورة" else null,
                bottomActionIcon = Icons.Filled.AddAPhoto,
                bottomActionTestTag = "publish_event_button",
                onBottomActionClick = if (canPublish) {
                    { showPublishEventDialog = true }
                } else null
            )
        }

        val displayedEvents = events.take(eventsVisibleCount)
        items(displayedEvents, key = { it.id }) { ev ->
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
                                    text = if (ev.location.isNotBlank()) "${ev.category} • ${ev.location}" else ev.category,
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

                        // زرا تحكم صغيرين وأنيقين أسفل كل بطاقة خبر/فعالية للمدير أو الناشر: "تعديل" و "حذف"
                        if (canPublish) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { eventBeingEdited = ev },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("edit_event_${ev.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.45f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("تعديل", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { viewModel.deleteSchoolEvent(ev.id) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("delete_event_${ev.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("حذف", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
        if (displayedEvents.size < events.size) {
            item {
                ProgressiveInfiniteScrollFooter(
                    visibleCount = displayedEvents.size,
                    totalCount = events.size,
                    step = 6,
                    itemLabel = "فعالية",
                    onLoadMore = {
                        eventsVisibleCount = (eventsVisibleCount + 6).coerceAtMost(events.size)
                    }
                )
            }
        }
    }

    eventBeingEdited?.let { editingEv ->
        var editTitle by remember(editingEv.id) { mutableStateOf(editingEv.title) }
        var editCategory by remember(editingEv.id) { mutableStateOf(editingEv.category) }
        var editLocation by remember(editingEv.id) { mutableStateOf(editingEv.location) }
        var editDate by remember(editingEv.id) { mutableStateOf(editingEv.date) }
        var editDescription by remember(editingEv.id) { mutableStateOf(editingEv.description) }
        var editImageBase64 by remember(editingEv.id) { mutableStateOf(editingEv.imageBase64) }

        AlertDialog(
            onDismissRequest = { eventBeingEdited = null },
            title = { Text("تعديل الفعالية أو الخبر المدرسي") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("عنوان الخبر أو الفعالية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("التصنيف") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = { Text("الموقع الجغرافي") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = editDate,
                        label = "تاريخ الفعالية أو الخبر",
                        onDateSelected = { selectedIso, _ -> editDate = selectedIso }
                    )
                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text("تفاصيل الخبر أو الفعالية") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    CompressedPhotoPickerButton(
                        label = "تحديث صورة الفعالية",
                        currentBase64 = editImageBase64,
                        onImageCompressed = { editImageBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ التعديلات",
                    icon = Icons.Filled.Save,
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            viewModel.updateSchoolEvent(
                                eventId = editingEv.id,
                                title = editTitle,
                                category = editCategory,
                                description = editDescription.ifBlank { editTitle },
                                date = editDate,
                                location = editLocation.trim(),
                                imageBase64 = editImageBase64
                            )
                            eventBeingEdited = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { eventBeingEdited = null }) { Text("إلغاء") }
            }
        )
    }

    if (showPublishEventDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("نشاط علمي وثقافي") }
        var location by remember { mutableStateOf("") }
        var date by remember { mutableStateOf(ArabicCalendarDateHelper.todayIsoDate()) }
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
                        placeholder = { Text("أدخل موقع الفعالية (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = date,
                        label = "تاريخ الفعالية أو الخبر",
                        onDateSelected = { selectedIso, _ -> date = selectedIso }
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
                                location = location.trim(),
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
    var schoolPhone by remember(schoolConfig) {
        mutableStateOf(IraqiPhoneCarrierValidator.sanitizeDigits(schoolConfig.phone))
    }
    var schoolPhoneKorekOverride by remember(schoolConfig) {
        mutableStateOf(IraqiPhoneCarrierValidator.isKorekNumber(schoolConfig.phone))
    }
    var selectedType by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolType) }
    var selectedStage by remember(schoolConfig) { mutableStateOf(schoolConfig.schoolStage) }
    var selectedMinistryAffiliation by remember(schoolConfig) {
        mutableStateOf(schoolConfig.ministryAffiliation.ifBlank { "وزارة التربية" })
    }
    var selectedSchoolGenderType by remember(schoolConfig) {
        mutableStateOf(schoolConfig.schoolGenderType.ifBlank { "مدرسة بنين" })
    }
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

    // 3D Categorized Settings Hub state (0..4)
    var selectedSettingsCategory by remember { mutableStateOf(0) }
    val settingsCategories = remember(isAdmin) {
        buildList {
            add(Triple(0, "الحساب والملف الشخصي", Icons.Filled.AccountCircle))
            add(Triple(1, "الأمان والدخول السريع", Icons.Filled.Fingerprint))
            add(Triple(2, "المظهر والتنقل", Icons.Filled.Palette))
            if (isAdmin) {
                add(Triple(3, "إدارة العطل الرسمية وأيام الدوام", Icons.Filled.EventAvailable))
            }
            add(Triple(4, "الإشعارات والنسخ الاحتياطي", Icons.Filled.SettingsBackupRestore))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) Top 3D Account Summary Header Card
        val cleanUserAddr = ArabicTextNormalizer.cleanDefaultAddress(currentUser?.address ?: "")
        ThreeDHeroHeaderCard(
            title = currentUser?.fullName ?: "مستخدم المنصة",
            subtitle = if (cleanUserAddr.isNotBlank()) {
                "الصلاحية: ${currentUser?.role?.arabicTitle ?: ""} • العنوان: $cleanUserAddr"
            } else {
                "الصلاحية: ${currentUser?.role?.arabicTitle ?: ""} • اسم المستخدم: ${currentUser?.username ?: ""}"
            },
            rightPillText = "كود المدرسة: ${schoolConfig.schoolId}",
            rightPillIcon = Icons.Filled.School,
            leftPillText = currentUser?.role?.arabicTitle ?: "إعدادات الحساب",
            leftPillIcon = Icons.Filled.VerifiedUser,
            illustrationType = ThreeDIllustrationType.SETTINGS_ROTATING_GEARS_3D
        )

        // 2) 3D Categorized Settings Hub Selector Grid (5 Organized Categories)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(24.dp),
                    ambientColor = Color(0xFF059669).copy(alpha = 0.16f),
                    spotColor = Color(0xFF0F3D5E).copy(alpha = 0.18f)
                ),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Category,
                        contentDescription = null,
                        tint = Color(0xFF059669),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "أقسام الإعدادات والتحكم المنظمة",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A)
                    )
                }

                // 2-column grid of 3D Category Cards
                settingsCategories.chunked(2).forEach { rowCats ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCats.forEach { (catIdx, catTitle, catIcon) ->
                            val isSelected = selectedSettingsCategory == catIdx
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { selectedSettingsCategory = catIdx }
                                    .testTag("settings_category_tab_$catIdx"),
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color.Transparent else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF059669) else Color(0xFFE2E8F0)
                                ),
                                shadowElevation = if (isSelected) 4.dp else 1.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .then(
                                            if (isSelected) {
                                                Modifier.background(
                                                    Brush.horizontalGradient(
                                                        listOf(Color(0xFF059669), Color(0xFF047857))
                                                    )
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = if (isSelected) Color.White.copy(alpha = 0.22f) else Color(0xFFD1FAE5),
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = catIcon,
                                                contentDescription = catTitle,
                                                tint = if (isSelected) Color.White else Color(0xFF047857),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = catTitle,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Category 0: الحساب والملف الشخصي
        if (selectedSettingsCategory == 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "الصلاحية: ${currentUser?.role?.arabicTitle ?: ""} • المستخدم: ${currentUser?.username ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (cleanUserAddr.isNotBlank()) {
                                    "العنوان: $cleanUserAddr • كود المدرسة: ${schoolConfig.schoolId}"
                                } else {
                                    "كود المدرسة: ${schoolConfig.schoolId}"
                                },
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
        }

        // Category 1: الأمان والدخول السريع (Biometric Fingerprint Login Settings Card)
        val biometricCredentials by viewModel.biometricCredentials.collectAsState()
        if (selectedSettingsCategory == 1 && currentUser != null) {
            BiometricSettingsSectionCard(
                currentAccountUsername = currentUser?.username ?: "",
                currentAccountFullName = currentUser?.fullName ?: "",
                currentAccountRoleLabel = currentUser?.role?.arabicTitle ?: "",
                savedBiometricCredentials = biometricCredentials,
                onEnableBiometricVerified = {
                    viewModel.enableBiometricForCurrentUser()
                },
                onDisableBiometric = {
                    viewModel.disableBiometricLogin()
                },
                onErrorToast = { err ->
                    viewModel.showToastAlert(err, isSuccess = false)
                }
            )
        }

        // Category 2: المظهر والتنقل (Theme & Display Preferences Card)
        if (selectedSettingsCategory == 2) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
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
                                    text = if (isSideDrawerEnabled) "مفعلة (تظهر أيقونة القائمة الجانبية أعلى الشاشة للانتقال الفوري)" else "مخفية حالياً",
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
        }

        // Category 4: الإشعارات والنسخ الاحتياطي (Private Principal Suggestions Box)
        if (selectedSettingsCategory == 4 && featureSettings.suggestionsBoxEnabled) {
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
                            text = "سري للغاية",
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
                            text = "إرسال المقترح السري لمدير المدرسة",
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Lock,
                                    contentDescription = null,
                                    tint = DeepEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "الرسائل السرية الواردة للمدير (${privateSuggestions.size}):",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepEmerald
                                )
                            }
                            privateSuggestions.take(10).forEach { sug ->
                                val cleanSugSender = com.example.util.InputSanitizer.stripEmojis(sug.senderName)
                                val cleanSugMsg = com.example.util.InputSanitizer.stripEmojis(sug.messageText)
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = DeepEmerald.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.18f)),
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
                                            imageVector = Icons.Filled.Lock,
                                            contentDescription = "رسالة سرية",
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(16.dp)
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
                                                    text = "$cleanSugSender (${sug.senderRoleArabic})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = DeepEmerald,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = sug.createdAt,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1
                                                )
                                            }
                                            Text(
                                                text = cleanSugMsg,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        IconButton(
                                            onClick = { viewModel.deletePrivateSuggestion(sug.id) },
                                            modifier = Modifier
                                                .size(26.dp)
                                                .testTag("delete_private_suggestion_${sug.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "حذف الرسالة السرية",
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

        // Category 3: إدارة العطل الرسمية وأيام الدوام + مزايا المدرسة والصفوف والمواد (خاص بالمدير)
        if (selectedSettingsCategory == 3 && isAdmin) {
            // Prominent 3D Official Holidays & Working Days Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                border = BorderStroke(1.5.dp, Color(0xFF059669).copy(alpha = 0.45f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThreeDIllustrationBox(
                            type = ThreeDIllustrationType.CALENDAR_HOLIDAYS,
                            modifier = Modifier.size(56.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إدارة العطل الرسمية وأيام الدوام الأسبوعية",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857)
                            )
                            Text(
                                text = "تحديد أيام العطل الأسبوعية والمناسبات الرسمية؛ يُقفل التحضير تلقائياً ولا يُحتسب غياب في أيام العطل.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF0F172A)
                            )
                        }
                    }

                    VectorActionButton(
                        text = "فتح نافذة إدارة العطل الرسمية والمناسبات",
                        icon = Icons.Filled.EventAvailable,
                        containerColor = Color(0xFF059669),
                        onClick = { viewModel.openHolidaysManagementModal() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "settings_open_holidays_modal_btn"
                    )
                }
            }

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
                        Triple(
                            "ميزة الخطوط والنقل المدرسي" to Icons.Filled.DirectionsBus,
                            featureSettings.busTrackingEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(busTrackingEnabled = v, busAlertsEnabled = v)
                            )
                        },
                        Triple(
                            "ميزة لوحة الشرف والتفاعل بالقلب" to Icons.Filled.EmojiEvents,
                            featureSettings.honorRollEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(honorRollEnabled = v))
                        },
                        Triple(
                            "ميزة المراسلة والمحادثات المباشرة" to Icons.Filled.Forum,
                            featureSettings.directChatEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(directChatEnabled = v))
                        },
                        Triple(
                            "ميزة المكتبة الرقمية والملخصات" to Icons.Filled.MenuBook,
                            featureSettings.studyLibraryEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(studyMaterialsEnabled = v, studyLibraryEnabled = v)
                            )
                        },
                        Triple(
                            "ميزة طلبات الإجازة الإلكترونية" to Icons.Filled.EventNote,
                            featureSettings.leaveRequestsEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(leaveRequestsEnabled = v))
                        },
                        Triple(
                            "ميزة الماسح الضوئي للهويات (QR Attendance)" to Icons.Filled.QrCodeScanner,
                            featureSettings.qrAttendanceEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(
                                    digitalIdEnabled = v,
                                    qrScannerEnabled = v,
                                    qrAttendanceEnabled = v
                                )
                            )
                        },
                        Triple(
                            "ميزة الأقساط الدراسية والوصولات" to Icons.Filled.AccountBalanceWallet,
                            featureSettings.tuitionEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(tuitionEnabled = v))
                        },
                        Triple(
                            "ميزة تسليم الواجبات بالصورة" to Icons.Filled.CameraAlt,
                            featureSettings.homeworkSubmissionEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(homeworkSubmissionEnabled = v))
                        },
                        Triple(
                            "ميزة دفتر المتابعة اليومي والسلوك" to ClipboardCheckVectorIcon,
                            featureSettings.dailyBehaviorEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(dailyBehaviorEnabled = v))
                        },
                        Triple(
                            "ميزة النداء الذكي للانصراف (استلام الأبناء)" to Icons.Filled.DirectionsCar,
                            featureSettings.dismissalCallEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(dismissalCallEnabled = v))
                        },
                        Triple(
                            "ميزة الاختبارات السريعة المؤقتة" to Icons.Filled.Quiz,
                            featureSettings.quickQuizzesEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(featureSettings.copy(quickQuizzesEnabled = v))
                        },
                        Triple(
                            "ميزة سجل المفقودات وصندوق المقترحات" to Icons.Filled.Inventory2,
                            featureSettings.lostAndFoundEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(lostAndFoundEnabled = v, suggestionsBoxEnabled = v)
                            )
                        },
                        Triple(
                            "ميزة مجتمع ونشاطات الطلبة والإبداعات" to StudentActivitiesLinearVector,
                            featureSettings.studentActivitiesEnabled
                        ) { v: Boolean ->
                            viewModel.updateSchoolFeatureSettings(
                                featureSettings.copy(studentActivitiesEnabled = v)
                            )
                        }
                    )

                    featureRows.forEachIndexed { idx, (labelAndIcon, isChecked, onToggle) ->
                        val (label, featureIcon) = labelAndIcon
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = DeepEmerald.copy(alpha = 0.10f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = featureIcon,
                                            contentDescription = label,
                                            tint = DeepEmerald,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
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

        // School Settings & Registered Classes Management (Editable by Director under Category 3)
        if (selectedSettingsCategory == 3 && isAdmin) {
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
                        placeholder = { Text("أدخل العنوان الجغرافي للمدرسة (اختياري)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    SmartIraqiPhoneInputField(
                        value = schoolPhone,
                        onValueChange = { schoolPhone = it },
                        label = "رقم هاتف إدارة المدرسة (11 رقماً — اختياري)",
                        korekOverrideApproved = schoolPhoneKorekOverride,
                        onKorekOverrideChange = { schoolPhoneKorekOverride = it },
                        testTag = "settings_school_phone_input"
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
                        text = "عائدية المدرسة الرسمية:",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("وزارة التربية", "التعليم الإسلامي - الوقف السني").forEach { affiliation ->
                            val isSelected = selectedMinistryAffiliation == affiliation
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedMinistryAffiliation = affiliation },
                                label = {
                                    Text(
                                        text = affiliation,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Text(
                        text = "جنس المدرسة:",
                        style = MaterialTheme.typography.labelLarge
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("مدرسة بنين", "مدرسة بنات", "مدرسة مختلطة").forEach { genderType ->
                            val isSelected = selectedSchoolGenderType == genderType
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedSchoolGenderType = genderType },
                                label = {
                                    Text(
                                        text = genderType,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
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
                            val (phoneOk, phoneErr) = IraqiPhoneCarrierValidator.validateForSave(
                                phone = schoolPhone,
                                korekOverrideApproved = schoolPhoneKorekOverride,
                                allowEmpty = true
                            )
                            if (!phoneOk) {
                                viewModel.showToastAlert(phoneErr, isSuccess = false)
                                return@VectorActionButton
                            }
                            viewModel.updateSchoolInfo(
                                schoolName = schoolName,
                                address = address.trim(),
                                schoolType = selectedType,
                                logoBase64 = logoBase64,
                                schoolStage = selectedStage,
                                registeredClasses = registeredClasses,
                                registeredSections = registeredSections,
                                registeredSubjects = registeredSubjects,
                                phone = schoolPhone,
                                ministryAffiliation = selectedMinistryAffiliation,
                                schoolGenderType = selectedSchoolGenderType
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
        }

        // =========================================================================
        // Category 4: FULL JSON BACKUP & RESTORE (النسخ الاحتياطي والاستعادة)
        // =========================================================================
        if (selectedSettingsCategory == 4 && isAdmin) {
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

        if (isAdmin && (selectedSettingsCategory == 0 || selectedSettingsCategory == 4)) {
            DeveloperCreditFooter()
        }

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
