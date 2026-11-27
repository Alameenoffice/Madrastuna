package com.example.livebroadcast

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.ViewGroup
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import android.webkit.JavascriptInterface
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SupervisorAccount
import com.example.util.SystemNotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.model.AppSection
import com.example.model.NotificationType
import com.example.model.SchoolConfig
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.DailyStatusBadgeSpec
import com.example.ui.components.SmartWelcomeProfileAvatar
import com.example.ui.components.formatCleanDate
import com.example.ui.components.resolveSmartAvatarPersona
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject

/**
 * Main Live Audio Broadcast Hub Screen ("مركز البث الصوتي المباشر").
 * - Displays GlassAcousticSpectrumHeader
 * - Shows active live rooms strictly scoped to the current user's audience
 * - Shows calm Canvas empty state when no live session is active
 * - Allows Principal (or delegated co-host supervisor) to launch a new broadcast via ModalBottomSheet
 * - Displays organized past sessions archive formatted with formatCleanDate
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveAudioBroadcastHubScreen(
    viewModel: SchoolViewModel,
    onEnterRoom: (LiveAudioRoom) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val students by viewModel.students.collectAsState()
    val allRooms by LiveAudioBroadcastCloudStore.rooms.collectAsState()
    val isSyncing by LiveAudioBroadcastCloudStore.isSyncing.collectAsState()

    val cleanSchoolId = schoolConfig.schoolId.ifBlank { "default_school" }
    LaunchedEffect(cleanSchoolId) {
        LiveAudioBroadcastCloudStore.startObservingSchool(context, cleanSchoolId)
        LiveAudioBroadcastCloudStore.fetchZegoConfig(context)
    }

    val linkedStudent = remember(currentUser, students) {
        val uid = currentUser?.linkedStudentId.orEmpty()
        if (uid.isNotBlank()) students.firstOrNull { it.id == uid } else null
    }
    val parentChildren = remember(currentUser, students) {
        val user = currentUser
        if (user?.role == UserRole.PARENT) viewModel.getChildrenForParent(user) else emptyList()
    }

    val visibleRooms = remember(allRooms, currentUser, linkedStudent, parentChildren) {
        allRooms.filter { room ->
            isUserIncludedInRoom(room, currentUser, linkedStudent, parentChildren)
        }
    }
    val activeLiveRooms = remember(visibleRooms) {
        visibleRooms.filter { it.isLive }
    }
    val pastEndedRooms = remember(visibleRooms) {
        visibleRooms.filter { !it.isLive }.take(25)
    }

    val teachingStaff = remember(accounts) {
        accounts.filter {
            it.role == UserRole.TEACHER && it.isActive && !it.isEffectiveBusDriver
        }
    }

    val canLaunchNewBroadcast = remember(currentUser, allRooms) {
        val user = currentUser ?: return@remember false
        when (user.role) {
            UserRole.ADMIN -> true
            UserRole.TEACHER -> user.canModerateActivities || allRooms.any { it.coHosts.contains(user.id) }
            else -> false
        }
    }

    val isPrincipal = currentUser?.role == UserRole.ADMIN
    var showCreateSheet by remember { mutableStateOf(false) }
    var showPastHistoryDialog by remember { mutableStateOf(false) }
    var isDeletingHistory by remember { mutableStateOf(false) }
    var pendingDeletePastRoom by remember { mutableStateOf<LiveAudioRoom?>(null) }
    var bannedEntryRoomTitle by remember { mutableStateOf<String?>(null) }

    fun attemptEnterRoom(room: LiveAudioRoom) {
        val uid = currentUser?.id.orEmpty()
        if (!isPrincipal && uid.isNotBlank() && room.isUserBanned(uid)) {
            bannedEntryRoomTitle = room.title
            return
        }
        onEnterRoom(room)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("live_audio_broadcast_hub_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) Signature Glass Acoustic Spectrum Header
        item(key = "header_glass_studio") {
            GlassAcousticSpectrumHeader(
                schoolName = schoolConfig.schoolName,
                activeLiveCount = activeLiveRooms.size
            )
        }

        // 2) Principal / Authorized Supervisor Launch Action Bar + Quick Archive Button
        item(key = "launch_broadcast_action") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canLaunchNewBroadcast) {
                    SpringBroadcastButton(
                        text = "بدء بث صوتي جديد",
                        icon = Icons.Filled.Podcasts,
                        onClick = { showCreateSheet = true },
                        height = 48.dp,
                        cornerRadius = 15.dp,
                        modifier = Modifier.weight(1f),
                        testTagName = "start_new_broadcast_button"
                    )
                } else {
                    Surface(
                        onClick = { showPastHistoryDialog = true },
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("hub_open_history_dialog_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "سجل البثوث السابقة",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(7.dp))
                            Text(
                                text = "سجل البثوث السابقة (${pastEndedRooms.size})",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                if (canLaunchNewBroadcast) {
                    Surface(
                        onClick = { showPastHistoryDialog = true },
                        shape = RoundedCornerShape(15.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("hub_open_history_dialog_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "سجل البثوث",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "السجل",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Surface(
                    onClick = {
                        scope.launch {
                            LiveAudioBroadcastCloudStore.refreshRoomsFromCloud(context, cleanSchoolId)
                        }
                    },
                    shape = RoundedCornerShape(15.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("refresh_live_rooms_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "تحديث جلسات البث",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // 3) Active Live Room(s) OR Calm Acoustic Empty State
        if (activeLiveRooms.isNotEmpty()) {
            item(key = "active_rooms_section_label") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PulsingLiveDotCanvas(size = 12.dp)
                    Text(
                        text = "الجلسات المباشرة الآن",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
            items(activeLiveRooms, key = { "active_${it.id}" }) { room ->
                ActiveLiveRoomHubCard(
                    room = room,
                    currentUser = currentUser,
                    schoolConfig = schoolConfig,
                    onEnterClick = { attemptEnterRoom(room) }
                )
            }
        } else {
            item(key = "calm_empty_state") {
                CalmAcousticEmptyStateCard()
            }
        }

        // 4) Organized Archive of Past Sessions
        if (pastEndedRooms.isNotEmpty()) {
            item(key = "past_sessions_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "سجل الجلسات السابقة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }

                    TextButton(
                        onClick = { showPastHistoryDialog = true },
                        modifier = Modifier.testTag("hub_manage_past_sessions_btn")
                    ) {
                        Text(
                            text = if (isPrincipal) "إدارة السجل" else "عرض الكل",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            items(pastEndedRooms, key = { "ended_${it.id}" }) { pastRoom ->
                PastBroadcastSessionCard(
                    room = pastRoom,
                    schoolConfig = schoolConfig,
                    isPrincipal = isPrincipal,
                    onRequestDelete = { pendingDeletePastRoom = pastRoom }
                )
            }
        }

        item(key = "hub_bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    if (showPastHistoryDialog) {
        PastBroadcastsHistoryDialog(
            endedRooms = pastEndedRooms,
            isPrincipal = isPrincipal,
            isProcessingDelete = isDeletingHistory,
            onDeleteSingle = { roomToDelete ->
                isDeletingHistory = true
                scope.launch {
                    LiveAudioBroadcastCloudStore.deleteRoomFromHistory(context, cleanSchoolId, roomToDelete.id)
                    isDeletingHistory = false
                    viewModel.showToastAlert("تم حذف البث من السجل بنجاح", isSuccess = true)
                }
            },
            onClearAll = {
                isDeletingHistory = true
                scope.launch {
                    val count = LiveAudioBroadcastCloudStore.clearAllEndedHistory(context, cleanSchoolId)
                    isDeletingHistory = false
                    viewModel.showToastAlert("تم مسح $count من سجل البثوث السابقة بنجاح", isSuccess = true)
                }
            },
            onDismiss = { showPastHistoryDialog = false }
        )
    }

    pendingDeletePastRoom?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDeletePastRoom = null },
            title = { Text("تأكيد حذف البث من السجل", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "هل تريد حذف جلسة «${target.title}» نهائياً من سجل البثوث السابقة؟",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDel = target
                        pendingDeletePastRoom = null
                        scope.launch {
                            LiveAudioBroadcastCloudStore.deleteRoomFromHistory(context, cleanSchoolId, toDel.id)
                            viewModel.showToastAlert("تم حذف البث من السجل", isSuccess = true)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePastRoom = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    bannedEntryRoomTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { bannedEntryRoomTitle = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = BroadcastCoralEnd
                )
            },
            title = { Text("تعذّر الدخول لهذا البث", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "تم حظرك من الدخول إلى جلسة «$title» من قِبل إدارة المدرسة. يرجى مراجعة الإدارة لفك الحظر.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { bannedEntryRoomTitle = null },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حسناً", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showCreateSheet && currentUser != null) {
        CreateLiveBroadcastModalSheet(
            schoolConfig = schoolConfig,
            teachingStaff = teachingStaff,
            onDismiss = { showCreateSheet = false },
            onLaunchBroadcast = { title, audienceType, targetGrade, targetSection, selectedCoHosts ->
                scope.launch {
                    val host = currentUser ?: return@launch
                    val createdRoom = LiveAudioBroadcastCloudStore.createLiveRoom(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        title = title,
                        hostUser = host,
                        audienceType = audienceType,
                        targetGrade = targetGrade,
                        targetSection = targetSection,
                        coHosts = selectedCoHosts
                    )
                    showCreateSheet = false
                    if (createdRoom != null) {
                        dispatchScopedLiveBroadcastNotification(
                            viewModel = viewModel,
                            room = createdRoom
                        )
                        viewModel.showToastAlert("تم إطلاق البث الصوتي المباشر بنجاح", isSuccess = true)
                        onEnterRoom(createdRoom)
                    } else {
                        viewModel.showToastAlert("تعذّر إنشاء غرفة البث الصوتي، حاول مجدداً", isSuccess = false)
                    }
                }
            }
        )
    }
}

@Composable
private fun ActiveLiveRoomHubCard(
    room: LiveAudioRoom,
    currentUser: UserAccount?,
    schoolConfig: SchoolConfig,
    onEnterClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val isHostOrCoHost = remember(room, currentUser) {
        isUserHostOrCoHost(room, currentUser)
    }
    val hostRoleEnum = remember(room.hostRole) {
        runCatching { UserRole.valueOf(room.hostRole) }.getOrDefault(UserRole.ADMIN)
    }
    val hostPersona = remember(hostRoleEnum, room.hostGender, schoolConfig.schoolGenderType, room.hostName) {
        resolveSmartAvatarPersona(
            role = hostRoleEnum,
            gender = room.hostGender,
            schoolGenderType = schoolConfig.schoolGenderType,
            fullName = room.hostName
        )
    }
    val hostRingBadge = remember {
        DailyStatusBadgeSpec(
            label = "مباشر",
            ringColors = listOf(BroadcastGoldAccent, BroadcastRichEmerald, BroadcastMintSoft),
            pillBackground = BroadcastDeepEmerald,
            pillBorder = BroadcastGoldLight,
            pillTextColor = Color.White,
            dotColor = Color(0xFFEF4444),
            pulseDot = true
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("active_live_room_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.72f) else Color.White
        ),
        border = BorderStroke(
            1.2.dp,
            Brush.linearGradient(
                colors = listOf(
                    BroadcastGoldAccent.copy(alpha = 0.6f),
                    colorScheme.primary.copy(alpha = 0.5f)
                )
            )
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SmartWelcomeProfileAvatar(
                    photoSource = room.hostAvatarUrl,
                    contentDescription = room.hostName,
                    persona = hostPersona,
                    statusBadge = hostRingBadge,
                    onClick = onEnterClick,
                    avatarSize = 56.dp,
                    showStatusBadge = false
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEF4444).copy(alpha = if (isDark) 0.25f else 0.12f),
                            border = BorderStroke(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PulsingLiveDotCanvas(size = 9.dp)
                                Text(
                                    text = "مباشر الآن",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colorScheme.primary.copy(alpha = 0.12f),
                            border = BorderStroke(0.8.dp, colorScheme.primary.copy(alpha = 0.35f))
                        ) {
                            Text(
                                text = room.audienceDisplayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Text(
                        text = room.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface
                    )

                    Text(
                        text = "بإشراف: ${room.hostName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = formatCleanDate(room.resolvedCreatedDateForDisplay()),
                        style = MaterialTheme.typography.labelMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }

                if (room.coHosts.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.SupervisorAccount,
                            contentDescription = null,
                            tint = BroadcastGoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "مشرفون مساعدون: ${room.coHosts.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            SpringBroadcastButton(
                text = if (isHostOrCoHost) "دخول كمشرف" else "انضمام للاستماع",
                icon = if (isHostOrCoHost) Icons.Filled.Mic else Icons.Filled.Headphones,
                onClick = onEnterClick,
                height = 48.dp,
                cornerRadius = 14.dp,
                modifier = Modifier.fillMaxWidth(),
                testTagName = if (isHostOrCoHost) "enter_as_host_btn" else "join_listen_btn"
            )
        }
    }
}

@Composable
private fun PastBroadcastSessionCard(
    room: LiveAudioRoom,
    schoolConfig: SchoolConfig,
    isPrincipal: Boolean = false,
    onRequestDelete: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val hostRoleEnum = remember(room.hostRole) {
        runCatching { UserRole.valueOf(room.hostRole) }.getOrDefault(UserRole.ADMIN)
    }
    val hostPersona = remember(hostRoleEnum, room.hostGender, schoolConfig.schoolGenderType, room.hostName) {
        resolveSmartAvatarPersona(
            role = hostRoleEnum,
            gender = room.hostGender,
            schoolGenderType = schoolConfig.schoolGenderType,
            fullName = room.hostName
        )
    }
    val calmBadge = remember {
        DailyStatusBadgeSpec(
            label = "منتهي",
            ringColors = listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1)),
            pillBackground = Color(0xFF64748B),
            pillBorder = Color.White,
            pillTextColor = Color.White
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.45f) else colorScheme.surface
        ),
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SmartWelcomeProfileAvatar(
                photoSource = room.hostAvatarUrl,
                contentDescription = room.hostName,
                persona = hostPersona,
                statusBadge = calmBadge,
                onClick = {},
                avatarSize = 44.dp,
                showStatusBadge = false
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = room.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "${room.hostName} • ${room.audienceDisplayLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "المدة: ${room.formattedDurationArabic()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "• ${formatParticipantsCountArabic(room.totalParticipantsCount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) BroadcastGoldLight else Color(0xFF92600A),
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = formatCleanDate(room.resolvedCreatedDateForDisplay()),
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }

            if (isPrincipal) {
                IconButton(
                    onClick = onRequestDelete,
                    modifier = Modifier.testTag("hub_delete_past_room_${room.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "حذف الجلسة من السجل",
                        tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "منتهية",
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Create Broadcast ModalBottomSheet (24.dp top corners).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateLiveBroadcastModalSheet(
    schoolConfig: SchoolConfig,
    teachingStaff: List<UserAccount>,
    onDismiss: () -> Unit,
    onLaunchBroadcast: (
        title: String,
        audienceType: LiveRoomAudienceType,
        targetGrade: String,
        targetSection: String,
        coHosts: List<String>
    ) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val availableGrades = remember(schoolConfig) {
        schoolConfig.registeredClasses.ifEmpty {
            schoolConfig.schoolStage.defaultClasses
        }
    }
    val availableSections = remember(schoolConfig) {
        val base = schoolConfig.registeredSections.ifEmpty { listOf("أ", "ب", "ج", "د") }
        listOf("الكل") + base.distinct()
    }

    var title by remember { mutableStateOf("") }
    var selectedAudience by remember { mutableStateOf(LiveRoomAudienceType.ALL) }
    var selectedGrade by remember(availableGrades) { mutableStateOf(availableGrades.firstOrNull().orEmpty()) }
    var selectedSection by remember { mutableStateOf("الكل") }
    val selectedCoHosts = remember { mutableStateListOf<String>() }
    var validationError by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Podcasts,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "إطلاق بث صوتي مباشر جديد",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق"
                    )
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it
                    validationError = ""
                },
                label = { Text("عنوان البث الصوتي") },
                placeholder = { Text("مثال: الاجتماع الدوري لأولياء الأمور") },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("create_broadcast_title_input")
            )

            // Uniform-height audience chips (Modifier.weight(1f))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "تحديد الفئة المستهدفة للبث",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LiveRoomAudienceType.entries.forEach { audience ->
                        val isSelected = selectedAudience == audience
                        Surface(
                            onClick = { selectedAudience = audience },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) BroadcastGoldLight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("audience_chip_${audience.key}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            ) {
                                Text(
                                    text = audience.arabicLabel,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // Conditional Grade & Section Pickers when "صف محدّد" is selected
            AnimatedVisibility(
                visible = selectedAudience == LiveRoomAudienceType.CLASS,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "اختيار الصف المستهدف",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableGrades) { grade ->
                            val picked = selectedGrade == grade
                            Surface(
                                onClick = { selectedGrade = grade },
                                shape = RoundedCornerShape(12.dp),
                                color = if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (picked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Text(
                                    text = grade,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "اختيار الشعبة",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(availableSections) { sec ->
                            val picked = selectedSection == sec
                            Surface(
                                onClick = { selectedSection = sec },
                                shape = RoundedCornerShape(12.dp),
                                color = if (picked) BroadcastGoldAccent else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(
                                    1.dp,
                                    if (picked) BroadcastGoldAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                            ) {
                                Text(
                                    text = if (sec == "الكل") "جميع الشعب" else "شعبة $sec",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (picked) Color.White else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Optional Co-Hosts Multi-Select with SmartWelcomeProfileAvatar
            if (teachingStaff.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "تخويل مشرفين مساعدين من الكادر (اختياري)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 190.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        teachingStaff.forEach { staff ->
                            val isChosen = selectedCoHosts.contains(staff.id)
                            StaffCoHostSelectRow(
                                staff = staff,
                                schoolGenderLabel = schoolConfig.schoolGenderType,
                                isSelected = isChosen,
                                onToggle = {
                                    if (isChosen) selectedCoHosts.remove(staff.id)
                                    else selectedCoHosts.add(staff.id)
                                }
                            )
                        }
                    }
                }
            }

            if (validationError.isNotBlank()) {
                Text(
                    text = validationError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold
                )
            }

            SpringBroadcastButton(
                text = "إطلاق البث المباشر الآن",
                icon = Icons.Filled.Podcasts,
                height = 50.dp,
                cornerRadius = 16.dp,
                modifier = Modifier.fillMaxWidth(),
                testTagName = "confirm_launch_broadcast_button",
                onClick = {
                    val cleanTitle = title.trim()
                    if (cleanTitle.length < 3) {
                        validationError = "يرجى كتابة عنوان واضح للبث الصوتي"
                        return@SpringBroadcastButton
                    }
                    if (selectedAudience == LiveRoomAudienceType.CLASS && selectedGrade.isBlank()) {
                        validationError = "يرجى اختيار الصف المستهدف"
                        return@SpringBroadcastButton
                    }
                    onLaunchBroadcast(
                        cleanTitle,
                        selectedAudience,
                        selectedGrade,
                        if (selectedSection == "الكل") "" else selectedSection,
                        selectedCoHosts.toList()
                    )
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
private fun StaffCoHostSelectRow(
    staff: UserAccount,
    schoolGenderLabel: String,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val persona = remember(staff.id, staff.gender, schoolGenderLabel, staff.fullName) {
        resolveSmartAvatarPersona(
            role = staff.role,
            gender = staff.gender,
            schoolGenderType = schoolGenderLabel,
            fullName = staff.fullName
        )
    }
    val badgeSpec = remember(isSelected) {
        DailyStatusBadgeSpec(
            label = if (isSelected) "مشرف" else "كادر",
            ringColors = if (isSelected) {
                listOf(BroadcastGoldAccent, BroadcastRichEmerald)
            } else {
                listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0))
            },
            pillBackground = BroadcastDeepEmerald,
            pillBorder = Color.White,
            pillTextColor = Color.White
        )
    }

    Surface(
        onClick = onToggle,
        shape = RoundedCornerShape(14.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
        },
        border = BorderStroke(
            width = if (isSelected) 1.2.dp else 0.8.dp,
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.22f)
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SmartWelcomeProfileAvatar(
                photoSource = staff.photoBase64,
                contentDescription = staff.fullName,
                persona = persona,
                statusBadge = badgeSpec,
                onClick = onToggle,
                avatarSize = 40.dp,
                showStatusBadge = false
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = staff.fullName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (staff.specialty.isNotBlank()) {
                    Text(
                        text = staff.specialty,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Icon(
                imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                contentDescription = if (isSelected) "مختار كمشرف" else "اختيار كمشرف",
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Safe JavaScript bridge exposed inside the Live Audio Broadcast WebView so web-layer events
 * can dispatch deduplicated Android native system notifications in addition to browser Notification API.
 */
private class MadrasatiBroadcastJsBridge(private val appContext: android.content.Context) {
    @JavascriptInterface
    fun notifyExternal(title: String?, body: String?, dedupKey: String?) {
        SystemNotificationHelper.showLiveBroadcastNotificationOnce(
            context = appContext,
            dedupKey = dedupKey.orEmpty().trim(),
            title = title.orEmpty().ifBlank { "بث صوتي مباشر" },
            body = body.orEmpty(),
            isUrgentInvite = false
        )
    }
}

private val detachedPresenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Interactive Live Audio Broadcast Room Screen with Real-Time Moderation & Urgent Invites.
 * - Tracks participant presence in Firebase on join & exit for real participant counts and live roster.
 * - Enforces Principal Mute/Unmute (real mic track mute + UI lock in WebView + Compose banner),
 *   Kick (immediate disconnect + clear message, allowed to join future broadcasts),
 *   and Ban (immediate disconnect + blocked from re-entering this broadcast until unbanned).
 * - Provides Principal controls inside the room: Participants & Moderation, Urgent Member Invite,
 *   Co-Host Delegation, and End Broadcast.
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LiveAudioBroadcastRoomScreen(
    initialRoom: LiveAudioRoom,
    viewModel: SchoolViewModel,
    onExitRoom: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentUser by viewModel.currentUser.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val students by viewModel.students.collectAsState()
    val allRooms by LiveAudioBroadcastCloudStore.rooms.collectAsState()
    val zegoConfig by LiveAudioBroadcastCloudStore.zegoConfig.collectAsState()

    val cleanSchoolId = schoolConfig.schoolId.ifBlank { initialRoom.schoolId.ifBlank { "default_school" } }
    val liveRoom = remember(allRooms, initialRoom) {
        allRooms.firstOrNull { it.id == initialRoom.id } ?: initialRoom
    }
    val roomSessionEnteredAt = remember(initialRoom.id) { System.currentTimeMillis() }

    val isHostRole = remember(liveRoom, currentUser) {
        isUserHostOrCoHost(liveRoom, currentUser)
    }
    val isPrincipalOrCreator = remember(liveRoom, currentUser) {
        val user = currentUser ?: return@remember false
        user.id == liveRoom.hostId || user.role == UserRole.ADMIN
    }
    val currentUserId = currentUser?.id.orEmpty()
    val isMutedByAdmin = remember(liveRoom.mutedUserIds, currentUserId, isPrincipalOrCreator) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.isUserMuted(currentUserId)
    }
    val isBannedByAdmin = remember(liveRoom.bannedUserIds, currentUserId, isPrincipalOrCreator) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.isUserBanned(currentUserId)
    }
    val isKickedByAdmin = remember(liveRoom.kickedUsers, currentUserId, isPrincipalOrCreator, roomSessionEnteredAt) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.kickedAtForUser(currentUserId) > roomSessionEnteredAt
    }

    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    var permissionDeniedOnce by remember { mutableStateOf(false) }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (!granted) {
            permissionDeniedOnce = true
        }
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewDestroyed by remember { mutableStateOf(false) }
    var showEndBroadcastConfirm by remember { mutableStateOf(false) }
    var showCoHostManagerSheet by remember { mutableStateOf(false) }
    var showModerationSheet by remember { mutableStateOf(false) }
    var showUrgentInviteSheet by remember { mutableStateOf(false) }
    var isSendingUrgentInvites by remember { mutableStateOf(false) }
    var hasAutoClosedForEnded by remember { mutableStateOf(false) }
    var removalReasonDialog by remember { mutableStateOf<Pair<String, String>?>(null) } // title to message

    fun teardownWebViewSafely() {
        if (isWebViewDestroyed) return
        isWebViewDestroyed = true
        val wv = webViewRef
        webViewRef = null
        if (wv != null) {
            runCatching {
                wv.evaluateJavascript("window.destroyZegoRoom && window.destroyZegoRoom();", null)
            }
            runCatching {
                wv.stopLoading()
                wv.loadUrl("about:blank")
                wv.onPause()
                wv.removeAllViews()
                wv.destroy()
            }
        }
    }

    fun leaveRoomCleanly() {
        teardownWebViewSafely()
        if (currentUserId.isNotBlank()) {
            val appCtx = context.applicationContext
            val rid = liveRoom.id
            detachedPresenceScope.launch {
                LiveAudioBroadcastCloudStore.recordParticipantLeft(appCtx, cleanSchoolId, rid, currentUserId)
            }
        }
        onExitRoom()
    }

    LaunchedEffect(Unit) {
        LiveAudioBroadcastCloudStore.fetchZegoConfig(context)
        if (!hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Register participant presence in DB on join (and refresh if room reloads while live)
    LaunchedEffect(liveRoom.id, currentUser?.id, isBannedByAdmin, isKickedByAdmin) {
        val user = currentUser
        if (user != null && liveRoom.isLive && !isBannedByAdmin && !isKickedByAdmin) {
            LiveAudioBroadcastCloudStore.recordParticipantJoined(context, cleanSchoolId, liveRoom.id, user)
        }
    }

    // Real-time Admin Mute Enforcement inside WebView
    LaunchedEffect(isMutedByAdmin, webViewRef, isWebViewDestroyed) {
        val wv = webViewRef
        if (wv != null && !isWebViewDestroyed) {
            runCatching {
                wv.evaluateJavascript(
                    "window.setAdminForceMute && window.setAdminForceMute(${if (isMutedByAdmin) "true" else "false"});",
                    null
                )
            }
        }
    }

    // Real-time Admin Ban Enforcement
    LaunchedEffect(isBannedByAdmin) {
        if (isBannedByAdmin && removalReasonDialog == null) {
            teardownWebViewSafely()
            removalReasonDialog = "تم حظرك من هذا البث" to
                "قامت إدارة المدرسة بحظرك من دخول غرفة البث الحالية. لا يمكنك إعادة الدخول لهذا البث إلا في حال فك الحظر من قِبل الإدارة."
        }
    }

    // Real-time Admin Kick Enforcement
    LaunchedEffect(isKickedByAdmin) {
        if (isKickedByAdmin && removalReasonDialog == null) {
            teardownWebViewSafely()
            removalReasonDialog = "تم إخراجك من غرفة البث" to
                "قامت إدارة المدرسة بإخراجك من هذه الجلسة الصوتية. يمكنك الانضمام إلى البثوث الصوتية القادمة لاحقاً."
        }
    }

    // Real-time termination watcher: immediately close room when status becomes "ended"
    LaunchedEffect(liveRoom.status) {
        if (!liveRoom.isLive && !hasAutoClosedForEnded && removalReasonDialog == null) {
            hasAutoClosedForEnded = true
            teardownWebViewSafely()
            viewModel.showToastAlert("انتهى البث الصوتي المباشر", isSuccess = true)
            onExitRoom()
        }
    }

    // Guaranteed cleanup + presence unregister when leaving composable
    DisposableEffect(liveRoom.id, currentUserId) {
        onDispose {
            teardownWebViewSafely()
            if (currentUserId.isNotBlank()) {
                val appCtx = context.applicationContext
                val rid = liveRoom.id
                detachedPresenceScope.launch {
                    LiveAudioBroadcastCloudStore.recordParticipantLeft(appCtx, cleanSchoolId, rid, currentUserId)
                }
            }
        }
    }

    BackHandler {
        if (removalReasonDialog != null) {
            removalReasonDialog = null
            leaveRoomCleanly()
        } else if (isPrincipalOrCreator) {
            showEndBroadcastConfirm = true
        } else {
            leaveRoomCleanly()
        }
    }

    val teachingStaff = remember(accounts) {
        accounts.filter { it.role == UserRole.TEACHER && it.isActive && !it.isEffectiveBusDriver }
    }

    // Eligible school members NOT currently present in the broadcast (for "تنبيه عضو للدخول")
    val eligibleAbsentUsers = remember(accounts, students, liveRoom.activeParticipants, liveRoom.bannedUserIds, currentUserId, liveRoom.targetAudience, liveRoom.targetGrade, liveRoom.targetSection) {
        accounts.filter { user ->
            if (!user.isActive || user.id.isBlank() || user.id == currentUserId) return@filter false
            if (liveRoom.activeParticipants.containsKey(user.id)) return@filter false
            if (liveRoom.isUserBanned(user.id)) return@filter false
            val linked = user.linkedStudentId.takeIf { it.isNotBlank() }?.let { lid ->
                students.firstOrNull { it.id == lid }
            }
            val children = if (user.role == UserRole.PARENT) viewModel.getChildrenForParent(user) else emptyList()
            isUserIncludedInRoom(liveRoom, user, linked, children)
        }.sortedWith(
            compareBy<UserAccount> {
                when (it.role) {
                    UserRole.ADMIN -> 0
                    UserRole.TEACHER -> 1
                    UserRole.PARENT -> 2
                    UserRole.STUDENT -> 3
                }
            }.thenBy { it.fullName }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("live_audio_broadcast_room_screen")
    ) {
        // Native Compose Emerald Top Bar with Principal Moderation & Invite Toolbar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(BroadcastDeepEmerald, BroadcastRichEmerald, MaterialTheme.colorScheme.primary)
                    )
                )
                .statusBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Right (RTL start): Pulsing Canvas "مباشر" badge + title + audience pill
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEF4444).copy(alpha = 0.28f),
                            border = BorderStroke(0.8.dp, Color(0xFFFCA5A5).copy(alpha = 0.75f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                PulsingLiveDotCanvas(size = 9.dp)
                                Text(
                                    text = "مباشر",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(0.75.dp, BroadcastGoldLight.copy(alpha = 0.55f))
                        ) {
                            Text(
                                text = liveRoom.audienceDisplayLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = BroadcastGoldLight,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = liveRoom.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                // Left (RTL end): End Broadcast (Principal/Creator) OR Leave (Listener)
                if (isPrincipalOrCreator) {
                    Surface(
                        onClick = { showEndBroadcastConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        color = BroadcastCoralEnd,
                        border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.28f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("room_end_broadcast_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "إنهاء البث",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "إنهاء البث",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = { leaveRoomCleanly() },
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("room_leave_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "مغادرة",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "مغادرة",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Compact Principal Moderation & Urgent Invite Action Bar inside Room Header
            if (isPrincipalOrCreator) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(start = 12.dp, end = 12.dp, bottom = 9.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1) Participants & Moderation ("المشاركون والرقابة")
                    Surface(
                        onClick = { showModerationSheet = true },
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(0.85.dp, BroadcastGoldLight.copy(alpha = 0.65f)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("room_participants_moderation_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Shield,
                                contentDescription = "المشاركون والرقابة",
                                tint = BroadcastGoldLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "المشاركون والرقابة (${liveRoom.activeParticipants.size})",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }

                    // 2) Urgent Member Invite ("تنبيه عضو للدخول")
                    Surface(
                        onClick = { showUrgentInviteSheet = true },
                        shape = RoundedCornerShape(11.dp),
                        color = BroadcastGoldAccent.copy(alpha = 0.28f),
                        border = BorderStroke(0.85.dp, BroadcastGoldLight.copy(alpha = 0.78f)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("room_urgent_invite_member_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.NotificationsActive,
                                contentDescription = "تنبيه عضو للدخول",
                                tint = BroadcastGoldLight,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "تنبيه عضو للدخول",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }

                    // 3) Delegate Co-Host ("تخويل مشرف")
                    Surface(
                        onClick = { showCoHostManagerSheet = true },
                        shape = RoundedCornerShape(11.dp),
                        color = Color.White.copy(alpha = 0.14f),
                        border = BorderStroke(0.85.dp, Color.White.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .height(34.dp)
                            .testTag("room_delegate_cohost_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PersonAddAlt1,
                                contentDescription = "تخويل مشرف",
                                tint = BroadcastMintSoft,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "تخويل مشرف",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Thin warm gold divider line
            HorizontalDivider(
                thickness = 1.2.dp,
                color = BroadcastGoldAccent.copy(alpha = 0.72f)
            )
        }

        // Recipient Admin-Mute Lock Banner
        AnimatedVisibility(
            visible = isMutedByAdmin,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Surface(
                color = BroadcastCoralEnd,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_muted_lock_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.MicOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "تم كتم صوتك وقفل الميكروفون من قِبل الإدارة حتى إلغاء الكتم.",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }

        // Room Body: Either Microphone Permission Rationale OR ZEGOCLOUD WebView Engine
        if (removalReasonDialog != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            )
        } else if (!hasAudioPermission) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }

                        Text(
                            text = "إذن المايكروفون مطلوب لغرفة البث الصوتي",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "لتشغيل البث الصوتي المباشر والسماح بالتحدث عند الإذن أو إدارة الجلسة بنقاء عالٍ، يرجى السماح للتطبيق باستخدام المايكروفون.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 22.sp
                        )

                        SpringBroadcastButton(
                            text = "السماح بالمايكروفون والمتابعة",
                            icon = Icons.Filled.Mic,
                            onClick = {
                                audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTagName = "request_audio_permission_retry_button"
                        )

                        if (permissionDeniedOnce) {
                            OutlinedButton(
                                onClick = {
                                    runCatching {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    }
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("فتح إعدادات صلاحيات التطبيق", fontWeight = FontWeight.Bold)
                            }
                        }

                        TextButton(
                            onClick = { leaveRoomCleanly() }
                        ) {
                            Text("العودة إلى مركز البث")
                        }
                    }
                }
            }
        } else {
            val isDark = isSystemInDarkTheme()
            val effectiveAppId = zegoConfig.appId.takeIf { it > 0L } ?: DEFAULT_ZEGO_APP_ID
            val effectiveSecret = zegoConfig.serverSecret.ifBlank { DEFAULT_ZEGO_SERVER_SECRET }
            val zegoRoomId = remember(liveRoom.id, liveRoom.schoolId) { liveRoom.buildZegoRoomId() }
            val cleanUserId = remember(currentUser?.id, currentUser?.username) { buildCleanZegoUserId(currentUser) }
            val displayName = remember(currentUser?.fullName, currentUser?.role) {
                currentUser?.fullName?.ifBlank { currentUser?.role?.arabicTitle } ?: "مشارك"
            }

            val htmlContent = remember(
                effectiveAppId,
                effectiveSecret,
                zegoRoomId,
                cleanUserId,
                displayName,
                isHostRole,
                isDark
            ) {
                buildZegoLiveAudioRoomHtml(
                    appId = effectiveAppId,
                    serverSecret = effectiveSecret,
                    roomId = zegoRoomId,
                    cleanUserId = cleanUserId,
                    userDisplayName = displayName,
                    isHostRole = isHostRole,
                    isDarkMode = isDark,
                    initialAdminMuted = isMutedByAdmin
                )
            }

            var loadedHtmlSignature by remember { mutableStateOf("") }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("zego_live_audio_webview"),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            setBackgroundColor(if (isDark) 0xFF0A1612.toInt() else 0xFFF4FAF7.toInt())
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                allowContentAccess = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                cacheMode = WebSettings.LOAD_DEFAULT
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            }
                            addJavascriptInterface(
                                MadrasatiBroadcastJsBridge(ctx.applicationContext),
                                "MadrasatiBroadcastBridge"
                            )
                            webViewClient = object : WebViewClient() {
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (isMutedByAdmin) {
                                        view?.evaluateJavascript(
                                            "window.setAdminForceMute && window.setAdminForceMute(true);",
                                            null
                                        )
                                    }
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onPermissionRequest(request: PermissionRequest?) {
                                    request?.let { req ->
                                        val grantedResources = req.resources.filter { res ->
                                            res == PermissionRequest.RESOURCE_AUDIO_CAPTURE ||
                                                res == PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID
                                        }.toTypedArray()
                                        if (grantedResources.isNotEmpty()) {
                                            req.grant(grantedResources)
                                        } else {
                                            req.grant(req.resources)
                                        }
                                    }
                                }
                            }
                            webViewRef = this
                            loadedHtmlSignature = htmlContent
                            loadDataWithBaseURL(
                                "https://madrasati.smart.app/",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    },
                    update = { webView ->
                        if (!isWebViewDestroyed && loadedHtmlSignature != htmlContent) {
                            loadedHtmlSignature = htmlContent
                            runCatching {
                                webView.evaluateJavascript("window.destroyZegoRoom && window.destroyZegoRoom();", null)
                            }
                            webView.loadDataWithBaseURL(
                                "https://madrasati.smart.app/",
                                htmlContent,
                                "text/html",
                                "UTF-8",
                                null
                            )
                        }
                    }
                )
            }
        }
    }

    // Recipient Kick / Ban Explanation Dialog (blocking until acknowledged)
    removalReasonDialog?.let { (dialogTitle, dialogBody) ->
        AlertDialog(
            onDismissRequest = {
                removalReasonDialog = null
                leaveRoomCleanly()
            },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Block,
                    contentDescription = null,
                    tint = BroadcastCoralEnd
                )
            },
            title = { Text(text = dialogTitle, fontWeight = FontWeight.ExtraBold) },
            text = { Text(text = dialogBody, lineHeight = 22.sp) },
            confirmButton = {
                Button(
                    onClick = {
                        removalReasonDialog = null
                        leaveRoomCleanly()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("acknowledge_kicked_or_banned_btn")
                ) {
                    Text("العودة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // End Broadcast Confirmation Dialog (Principal / Host)
    if (showEndBroadcastConfirm) {
        AlertDialog(
            onDismissRequest = { showEndBroadcastConfirm = false },
            title = {
                Text(
                    text = "إنهاء البث الصوتي المباشر",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "هل تريد إنهاء هذا البث الصوتي للجميع الآن؟ سيتم إغلاق الغرفة تلقائياً لدى جميع المستمعين والمشرفين وحفظ الجلسة مع مدتها في السجل.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEndBroadcastConfirm = false
                        hasAutoClosedForEnded = true
                        teardownWebViewSafely()
                        scope.launch {
                            LiveAudioBroadcastCloudStore.endLiveRoom(context, cleanSchoolId, liveRoom.id)
                            viewModel.showToastAlert("انتهى البث الصوتي المباشر وتم حفظه في السجل", isSuccess = true)
                            onExitRoom()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_end_broadcast_dialog_btn")
                ) {
                    Text("إنهاء للجميع", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            showEndBroadcastConfirm = false
                            leaveRoomCleanly()
                        }
                    ) {
                        Text("مغادرة فقط")
                    }
                    TextButton(onClick = { showEndBroadcastConfirm = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    // Participants & Moderation Sheet (Principal / Creator)
    if (showModerationSheet && isPrincipalOrCreator) {
        RoomParticipantsModerationSheet(
            room = liveRoom,
            currentUserId = currentUserId,
            allAccounts = accounts,
            schoolConfig = schoolConfig,
            onToggleMute = { participant ->
                scope.launch {
                    val wasMuted = liveRoom.isUserMuted(participant.userId)
                    LiveAudioBroadcastCloudStore.toggleMuteParticipant(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId
                    )
                    viewModel.showToastAlert(
                        if (wasMuted) "تم إلغاء كتم صوت ${participant.fullName}"
                        else "تم كتم صوت ${participant.fullName} فوراً",
                        isSuccess = true
                    )
                }
            },
            onKick = { participant ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.kickParticipant(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId
                    )
                    viewModel.showToastAlert("تم إخراج ${participant.fullName} من غرفة البث", isSuccess = true)
                }
            },
            onBan = { participant ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.banParticipant(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId,
                        targetFullName = participant.fullName
                    )
                    viewModel.showToastAlert("تم حظر ${participant.fullName} من هذا البث", isSuccess = true)
                }
            },
            onUnban = { uid, displayName ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.unbanParticipant(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = uid
                    )
                    viewModel.showToastAlert("تم فك الحظر عن $displayName بنجاح", isSuccess = true)
                }
            },
            onDismiss = { showModerationSheet = false }
        )
    }

    // Urgent Member Invite Sheet ("تنبيه عضو للدخول")
    if (showUrgentInviteSheet && isPrincipalOrCreator) {
        UrgentMemberInviteSheet(
            eligibleAbsentUsers = eligibleAbsentUsers,
            schoolConfig = schoolConfig,
            isSending = isSendingUrgentInvites,
            onSendInvites = { chosenUsers ->
                isSendingUrgentInvites = true
                scope.launch {
                    val senderDisplay = currentUser?.fullName?.ifBlank { "إدارة المدرسة" } ?: "إدارة المدرسة"
                    val sent = LiveAudioBroadcastCloudStore.sendUrgentInvites(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targets = chosenUsers,
                        senderName = senderDisplay
                    )
                    chosenUsers.forEach { target ->
                        viewModel.pushNotification(
                            title = "تنبيه عاجل: مطلوب حضورك في البث المباشر",
                            body = "تدعوك $senderDisplay للانضمام فوراً إلى بث «${liveRoom.title}»",
                            type = NotificationType.REPORT,
                            targetUserId = target.id,
                            targetSection = AppSection.LIVE_AUDIO_BROADCAST.name,
                            isUrgent = true,
                            priority = "high"
                        )
                    }
                    isSendingUrgentInvites = false
                    showUrgentInviteSheet = false
                    viewModel.showToastAlert(
                        "تم إرسال تنبيه عاجل للدخول إلى ${sent.size} من الأعضاء",
                        isSuccess = true
                    )
                }
            },
            onDismiss = { showUrgentInviteSheet = false }
        )
    }

    // Co-Host Management Sheet (Principal / Host)
    if (showCoHostManagerSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val workingCoHosts = remember(liveRoom.coHosts) {
            mutableStateListOf<String>().apply { addAll(liveRoom.coHosts) }
        }
        var isUpdatingCoHosts by remember { mutableStateOf(false) }

        ModalBottomSheet(
            onDismissRequest = { showCoHostManagerSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "إدارة وتخويل المشرفين المساعدين",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "أي كادر يتم تخويله هنا يحصل فوراً على صلاحيات إدارة المتحدثين والمايكروفون داخل الغرفة.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (teachingStaff.isEmpty()) {
                    Text(
                        text = "لا توجد حسابات كادر تدريسي نشطة حالياً في المدرسة.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(teachingStaff, key = { it.id }) { staff ->
                            val isChosen = workingCoHosts.contains(staff.id)
                            StaffCoHostSelectRow(
                                staff = staff,
                                schoolGenderLabel = schoolConfig.schoolGenderType,
                                isSelected = isChosen,
                                onToggle = {
                                    if (isChosen) workingCoHosts.remove(staff.id)
                                    else workingCoHosts.add(staff.id)
                                }
                            )
                        }
                    }
                }

                SpringBroadcastButton(
                    text = if (isUpdatingCoHosts) "جارٍ الحفظ..." else "حفظ صلاحيات المشرفين",
                    icon = Icons.Filled.SupervisorAccount,
                    enabled = !isUpdatingCoHosts,
                    modifier = Modifier.fillMaxWidth(),
                    testTagName = "save_cohosts_sheet_button",
                    onClick = {
                        isUpdatingCoHosts = true
                        scope.launch {
                            LiveAudioBroadcastCloudStore.updateRoomCoHosts(
                                context = context,
                                rawSchoolId = cleanSchoolId,
                                roomId = liveRoom.id,
                                newCoHosts = workingCoHosts.toList()
                            )
                            isUpdatingCoHosts = false
                            showCoHostManagerSheet = false
                            viewModel.showToastAlert("تم تحديث قائمة المشرفين المخوّلين فوراً", isSuccess = true)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Dispatches an instant in-app + system notification scoped strictly to the broadcast's targetAudience.
 */
private fun dispatchScopedLiveBroadcastNotification(
    viewModel: SchoolViewModel,
    room: LiveAudioRoom
) {
    val notifTitle = "بث صوتي مباشر الآن"
    val notifBody = "${room.title} • بإشراف ${room.hostName} (${room.audienceDisplayLabel})"
    when (room.audienceType) {
        LiveRoomAudienceType.STAFF -> {
            viewModel.pushNotification(
                title = notifTitle,
                body = notifBody,
                type = NotificationType.REPORT,
                targetRole = UserRole.TEACHER.name,
                targetSection = AppSection.LIVE_AUDIO_BROADCAST.name,
                isUrgent = true,
                priority = "high"
            )
        }
        LiveRoomAudienceType.CLASS -> {
            viewModel.pushNotification(
                title = notifTitle,
                body = notifBody,
                type = NotificationType.REPORT,
                targetGradeClass = room.targetGrade,
                targetSectionName = room.targetSection,
                targetSection = AppSection.LIVE_AUDIO_BROADCAST.name,
                isUrgent = true,
                priority = "high"
            )
        }
        LiveRoomAudienceType.ALL -> {
            viewModel.pushNotification(
                title = notifTitle,
                body = notifBody,
                type = NotificationType.REPORT,
                targetSection = AppSection.LIVE_AUDIO_BROADCAST.name,
                isUrgent = true,
                priority = "high"
            )
        }
    }
}

/**
 * Builds the ZEGOCLOUD UIKit Prebuilt Live Audio Room HTML page with:
 * - Client-side token generation via ZegoUIKitPrebuilt.generateKitTokenForTest (zero manual token!)
 * - Automatic Host vs Audience role
 * - Custom RTL emerald-mint-gold Madrasati CSS styling
 * - window.destroyZegoRoom() teardown hook calling zp.destroy()
 */
private fun buildZegoLiveAudioRoomHtml(
    appId: Long,
    serverSecret: String,
    roomId: String,
    cleanUserId: String,
    userDisplayName: String,
    isHostRole: Boolean,
    isDarkMode: Boolean,
    initialAdminMuted: Boolean = false
): String {
    val jsSecret = JSONObject.quote(serverSecret)
    val jsRoomId = JSONObject.quote(roomId)
    val jsUserId = JSONObject.quote(cleanUserId)
    val jsUserName = JSONObject.quote(userDisplayName)
    val bgHex = if (isDarkMode) "#0A1612" else "#F4FAF7"
    val surfaceHex = if (isDarkMode) "#102720" else "#FFFFFF"
    val textHex = if (isDarkMode) "#ECFDF5" else "#052E24"
    val roleLabel = if (isHostRole) "مشرف الجلسة" else "مستمع"

    return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <title>غرفة البث الصوتي المباشر</title>
  <style>
    * {
      box-sizing: border-box;
      -webkit-tap-highlight-color: transparent;
    }
    html, body {
      margin: 0;
      padding: 0;
      width: 100%;
      height: 100%;
      overflow: hidden;
      background: $bgHex;
      color: $textHex;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
      direction: rtl;
    }
    #zego-root {
      width: 100vw;
      height: 100vh;
      background: radial-gradient(circle at top right, rgba(6, 106, 78, 0.22), $bgHex 68%);
    }
    #loading-overlay {
      position: fixed;
      inset: 0;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 14px;
      background: $bgHex;
      z-index: 999;
      padding: 24px;
      text-align: center;
      transition: opacity 0.35s ease;
    }
    .loader-ring {
      width: 52px;
      height: 52px;
      border-radius: 50%;
      border: 3.5px solid rgba(6, 106, 78, 0.20);
      border-top-color: #059669;
      border-right-color: #D9A441;
      animation: spin 1s linear infinite;
    }
    @keyframes spin {
      to { transform: rotate(360deg); }
    }
    .loader-title {
      font-size: 16px;
      font-weight: 700;
      color: $textHex;
    }
    .loader-sub {
      font-size: 13px;
      color: ${if (isDarkMode) "#A7F3D0" else "#047857"};
      background: ${if (isDarkMode) "rgba(6,106,78,0.28)" else "rgba(5,150,105,0.10)"};
      border: 1px solid rgba(217, 164, 65, 0.38);
      border-radius: 999px;
      padding: 5px 14px;
    }
    .error-box {
      display: none;
      max-width: 320px;
      background: $surfaceHex;
      border: 1px solid rgba(220, 82, 77, 0.45);
      border-radius: 16px;
      padding: 16px;
      color: $textHex;
      font-size: 13px;
      line-height: 1.6;
    }
    /* Harmonize ZEGOCLOUD Prebuilt containers with Madrasati Emerald-Gold aesthetic */
    #zego-root [class*="zego"],
    #zego-root [class*="Zego"] {
      direction: rtl;
    }
    /* When locked by Principal mute, disable mic toggle affordances visually & interactively */
    body.admin-mic-locked [class*="Mic"],
    body.admin-mic-locked [class*="mic"],
    body.admin-mic-locked [class*="Audio"],
    body.admin-mic-locked [aria-label*="mic" i],
    body.admin-mic-locked [title*="mic" i] {
      pointer-events: none !important;
      opacity: 0.42 !important;
      filter: grayscale(0.8);
    }
  </style>
</head>
<body class="${if (initialAdminMuted) "admin-mic-locked" else ""}">
  <div id="loading-overlay">
    <div class="loader-ring"></div>
    <div class="loader-title">جارٍ تهيئة الاستوديو الصوتي المباشر...</div>
    <div class="loader-sub">الدخول بصفة: $roleLabel</div>
    <div id="error-box" class="error-box"></div>
  </div>
  <div id="zego-root"></div>

  <script>
    (function () {
      window.__madrasatiAudioTracks = [];
      window.__madrasatiAdminMuted = ${if (initialAdminMuted) "true" else "false"};

      // Wrap getUserMedia early so every captured microphone MediaStreamTrack can be strictly muted/unmuted by Principal
      if (navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === "function") {
        var origGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
        navigator.mediaDevices.getUserMedia = function (constraints) {
          return origGetUserMedia(constraints).then(function (stream) {
            try {
              var tracks = stream.getAudioTracks ? stream.getAudioTracks() : [];
              tracks.forEach(function (t) {
                window.__madrasatiAudioTracks.push(t);
                if (window.__madrasatiAdminMuted) {
                  t.enabled = false;
                }
              });
            } catch (e) {}
            return stream;
          });
        };
      }

      function enforceAudioTracksMuteState() {
        try {
          var muted = !!window.__madrasatiAdminMuted;
          window.__madrasatiAudioTracks = window.__madrasatiAudioTracks.filter(function (t) {
            return t && t.readyState !== "ended";
          });
          window.__madrasatiAudioTracks.forEach(function (t) {
            t.enabled = !muted;
          });
        } catch (e) {}
      }

      window.setAdminForceMute = function (isMuted) {
        window.__madrasatiAdminMuted = !!isMuted;
        if (window.__madrasatiAdminMuted) {
          document.body.classList.add("admin-mic-locked");
        } else {
          document.body.classList.remove("admin-mic-locked");
        }
        enforceAudioTracksMuteState();
      };

      // Dual-path external notification helper: Android native bridge first, Browser Notification API fallback
      window.madrasatiNotifyExternal = function (title, body, dedupKey) {
        try {
          if (window.MadrasatiBroadcastBridge && typeof window.MadrasatiBroadcastBridge.notifyExternal === "function") {
            window.MadrasatiBroadcastBridge.notifyExternal(String(title || ""), String(body || ""), String(dedupKey || ""));
            return;
          }
          if ("Notification" in window) {
            if (Notification.permission === "granted") {
              new Notification(String(title || "بث صوتي مباشر"), { body: String(body || ""), tag: String(dedupKey || "") });
            } else if (Notification.permission === "default") {
              Notification.requestPermission().then(function (perm) {
                if (perm === "granted") {
                  new Notification(String(title || "بث صوتي مباشر"), { body: String(body || ""), tag: String(dedupKey || "") });
                }
              });
            }
          }
        } catch (e) {}
      };

      setInterval(function () {
        if (window.__madrasatiAdminMuted) {
          enforceAudioTracksMuteState();
        }
      }, 900);
    })();
  </script>

  <script src="https://unpkg.com/@zegocloud/zego-uikit-prebuilt/zego-uikit-prebuilt.js"></script>
  <script>
    (function () {
      var zpInstance = null;

      window.destroyZegoRoom = function () {
        try {
          if (window.__madrasatiAudioTracks) {
            window.__madrasatiAudioTracks.forEach(function (t) {
              try { t.stop(); } catch (e) {}
            });
            window.__madrasatiAudioTracks = [];
          }
          if (zpInstance && typeof zpInstance.destroy === "function") {
            zpInstance.destroy();
          }
        } catch (e) {}
        zpInstance = null;
      };

      function showError(msg) {
        var box = document.getElementById("error-box");
        if (box) {
          box.style.display = "block";
          box.textContent = msg;
        }
      }

      function hideLoader() {
        var el = document.getElementById("loading-overlay");
        if (el) {
          el.style.opacity = "0";
          setTimeout(function () {
            if (el && el.parentNode) el.parentNode.removeChild(el);
          }, 360);
        }
      }

      function initRoom() {
        try {
          if (typeof ZegoUIKitPrebuilt === "undefined") {
            showError("تعذّر تحميل محرك البث الصوتي. يرجى التحقق من اتصال الإنترنت ثم إعادة المحاولة.");
            return;
          }

          var appID = Number($appId);
          var serverSecret = $jsSecret;
          var roomID = $jsRoomId;
          var userID = $jsUserId;
          var userName = $jsUserName;
          var isHost = ${if (isHostRole) "true" else "false"};

          var kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(
            appID,
            serverSecret,
            roomID,
            userID,
            userName
          );

          var role = isHost ? ZegoUIKitPrebuilt.Host : ZegoUIKitPrebuilt.Audience;
          zpInstance = ZegoUIKitPrebuilt.create(kitToken);

          zpInstance.joinRoom({
            container: document.getElementById("zego-root"),
            scenario: {
              mode: ZegoUIKitPrebuilt.LiveAudioRoom,
              config: {
                role: role
              }
            },
            showPreJoinView: false,
            showLeavingView: false,
            showRoomTimer: true,
            showUserList: true,
            showAudioVideoSettingsButton: false,
            showTextChat: true,
            turnOnMicrophoneWhenJoining: isHost && !window.__madrasatiAdminMuted,
            turnOnCameraWhenJoining: false,
            showMyCameraToggleButton: false,
            showScreenSharingButton: false,
            lowerLeftNotification: {
              showUserJoinAndLeave: true,
              showTextChat: true
            },
            onJoinRoom: function () {
              hideLoader();
              if (window.__madrasatiAdminMuted) {
                window.setAdminForceMute(true);
              }
            },
            onLeaveRoom: function () {
              window.destroyZegoRoom();
            }
          });

          setTimeout(hideLoader, 2400);
        } catch (err) {
          showError("حدث خطأ أثناء تشغيل البث الصوتي المباشر. تأكد من الاتصال بالإنترنت.");
        }
      }

      window.addEventListener("beforeunload", function () {
        window.destroyZegoRoom();
      });

      if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initRoom);
      } else {
        initRoom();
      }
    })();
  </script>
</body>
</html>
""".trimIndent()
}
