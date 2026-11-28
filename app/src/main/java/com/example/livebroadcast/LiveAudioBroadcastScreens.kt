package com.example.livebroadcast

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.delay
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
 * Safe JavaScript bridge exposed inside the headless Live Audio Broadcast WebView.
 * Dispatches all callbacks cleanly onto the Android main thread for Compose state updates
 * and deduplicated native notifications.
 */
private class MadrasatiBroadcastJsBridge(
    private val appContext: Context,
    private val onEngineConnected: () -> Unit,
    private val onEngineError: (String) -> Unit,
    private val onLocalSpeaking: (Boolean) -> Unit,
    private val onRemoteAudio: (Boolean) -> Unit,
    private val onMicChanged: (Boolean) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())

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

    @JavascriptInterface
    fun onRoomConnected() {
        mainHandler.post { onEngineConnected() }
    }

    @JavascriptInterface
    fun onRoomError(message: String?) {
        val clean = message.orEmpty().trim().ifBlank { "تعذّر الاتصال بمحرك البث الصوتي" }
        mainHandler.post { onEngineError(clean) }
    }

    @JavascriptInterface
    fun onLocalSpeakingChanged(isSpeaking: Boolean) {
        mainHandler.post { onLocalSpeaking(isSpeaking) }
    }

    @JavascriptInterface
    fun onRemoteAudioStateChanged(isActive: Boolean) {
        mainHandler.post { onRemoteAudio(isActive) }
    }

    @JavascriptInterface
    fun onMicStateChanged(isMicOn: Boolean) {
        mainHandler.post { onMicChanged(isMicOn) }
    }
}

private fun applyBroadcastAudioOutputRoute(audioManager: AudioManager, useLoudspeaker: Boolean) {
    runCatching {
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val targetType = if (useLoudspeaker) {
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER
            } else {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
            val matched = audioManager.availableCommunicationDevices.firstOrNull { it.type == targetType }
            if (matched != null) {
                audioManager.setCommunicationDevice(matched)
            } else if (!useLoudspeaker) {
                audioManager.clearCommunicationDevice()
            }
        }
        @Suppress("DEPRECATION")
        audioManager.isSpeakerphoneOn = useLoudspeaker
    }
}

private val detachedPresenceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

/**
 * Interactive 100% Arabic Spaces-Style Live Audio Broadcast Room Screen:
 * - Preserves the ZEGOCLOUD WebRTC audio engine completely hidden in the background (zero English UI).
 * - Enforces strict Listener-by-default policy: only the Host/Principal or explicitly authorized speakers
 *   can access the microphone. Listeners see only "طلب التحدث (رفع اليد)" and "مغادرة البث".
 * - Instant real-time enforcement of Principal moderation: Authorize/Revoke Speak, Mute (forces back to Listener),
 *   Kick, Ban/Unban, Urgent Member Invite, and End Broadcast.
 * - Routes WebRTC audio cleanly to the external phone loudspeaker via Android AudioManager.
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

    val currentUserId = currentUser?.id.orEmpty()
    val isPrincipalOrCreator = remember(liveRoom.hostId, currentUser) {
        val user = currentUser ?: return@remember false
        user.id == liveRoom.hostId || user.role == UserRole.ADMIN
    }
    val isMutedByAdmin = remember(liveRoom.mutedUserIds, currentUserId, isPrincipalOrCreator) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.isUserMuted(currentUserId)
    }
    val isBannedByAdmin = remember(liveRoom.bannedUserIds, currentUserId, isPrincipalOrCreator) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.isUserBanned(currentUserId)
    }
    val isKickedByAdmin = remember(liveRoom.kickedUsers, currentUserId, isPrincipalOrCreator, roomSessionEnteredAt) {
        !isPrincipalOrCreator && currentUserId.isNotBlank() && liveRoom.kickedAtForUser(currentUserId) > roomSessionEnteredAt
    }

    // Strict Speaking Policy: ONLY Host/Admin or explicitly authorized non-muted Co-Host can speak
    val canSpeakNow = remember(liveRoom.hostId, liveRoom.coHosts, liveRoom.mutedUserIds, currentUser, isMutedByAdmin) {
        val u = currentUser ?: return@remember false
        !isMutedByAdmin && liveRoom.canParticipantSpeak(u.id, u.role.name)
    }
    val hasSelfRaisedHand = remember(liveRoom.handRaisedUserIds, currentUserId) {
        currentUserId.isNotBlank() && liveRoom.hasRaisedHand(currentUserId)
    }
    val pendingHandRaiseCount = remember(liveRoom.handRaisedUserIds, liveRoom.activeParticipants) {
        liveRoom.handRaisedUserIds.count { uid -> liveRoom.activeParticipants.containsKey(uid) }
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

    // Audio & UI state
    val audioManager = remember(context) {
        context.applicationContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    }
    var isSpeakerphoneOn by remember { mutableStateOf(true) }
    var isLocalMicOn by remember(initialRoom.id) { mutableStateOf(canSpeakNow) }
    var isLocalSpeaking by remember { mutableStateOf(false) }
    var isRemoteAudioActive by remember { mutableStateOf(false) }
    var connectionState by remember { mutableStateOf(AudioEngineConnectionState.CONNECTING) }
    var connectionErrorText by remember { mutableStateOf<String?>(null) }
    var engineReloadNonce by remember { mutableIntStateOf(0) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var isWebViewDestroyed by remember { mutableStateOf(false) }
    var showEndBroadcastConfirm by remember { mutableStateOf(false) }
    var showLeaveBroadcastConfirm by remember { mutableStateOf(false) }
    var showCoHostManagerSheet by remember { mutableStateOf(false) }
    var showModerationSheet by remember { mutableStateOf(false) }
    var showUrgentInviteSheet by remember { mutableStateOf(false) }
    var isSendingUrgentInvites by remember { mutableStateOf(false) }
    var hasAutoClosedForEnded by remember { mutableStateOf(false) }
    var removalReasonDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Effective speaker mode inside WebView (true ONLY when authorized AND RECORD_AUDIO is granted)
    val isSpeakerEngineMode = canSpeakNow && hasAudioPermission
    val latestSpeakerEngineMode by rememberUpdatedState(isSpeakerEngineMode)
    val latestHasAudioPermission by rememberUpdatedState(hasAudioPermission)

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

    // Configure Android AudioManager for clear WebRTC loudspeaker output and restore on exit
    DisposableEffect(audioManager) {
        val am = audioManager
        val prevMode = am?.mode ?: AudioManager.MODE_NORMAL
        @Suppress("DEPRECATION")
        val prevSpeaker = am?.isSpeakerphoneOn ?: false
        if (am != null) {
            applyBroadcastAudioOutputRoute(am, isSpeakerphoneOn)
        }
        onDispose {
            if (am != null) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        am.clearCommunicationDevice()
                    }
                    @Suppress("DEPRECATION")
                    am.isSpeakerphoneOn = prevSpeaker
                    am.mode = prevMode
                }
            }
        }
    }

    LaunchedEffect(isSpeakerphoneOn, connectionState, isSpeakerEngineMode) {
        val am = audioManager ?: return@LaunchedEffect
        applyBroadcastAudioOutputRoute(am, isSpeakerphoneOn)
        if (connectionState == AudioEngineConnectionState.CONNECTED) {
            delay(650L)
            applyBroadcastAudioOutputRoute(am, isSpeakerphoneOn)
        }
    }

    // Fetch latest Zego config & request RECORD_AUDIO ONLY for Host / Authorized Speaker (never blocks Listeners!)
    LaunchedEffect(Unit) {
        LiveAudioBroadcastCloudStore.fetchZegoConfig(context)
    }

    LaunchedEffect(canSpeakNow, hasAudioPermission) {
        if (canSpeakNow && !hasAudioPermission) {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // React instantaneously when speaker authorization is granted, revoked, or muted by Principal
    var previousCanSpeak by remember(initialRoom.id) { mutableStateOf(canSpeakNow) }
    LaunchedEffect(canSpeakNow, isMutedByAdmin) {
        if (canSpeakNow != previousCanSpeak) {
            if (canSpeakNow) {
                isLocalMicOn = true
                viewModel.showToastAlert("تم تخويلك للتحدث الآن — يمكنك استخدام الميكروفون", isSuccess = true)
            } else {
                isLocalMicOn = false
                isLocalSpeaking = false
                runCatching {
                    webViewRef?.evaluateJavascript(
                        "window.forceHardMuteAndStopMic && window.forceHardMuteAndStopMic();",
                        null
                    )
                }
                if (currentUserId.isNotBlank()) {
                    LiveAudioBroadcastCloudStore.updateParticipantMicState(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        userId = currentUserId,
                        isMicOn = false,
                        isSpeaking = false
                    )
                }
                viewModel.showToastAlert(
                    if (isMutedByAdmin) "تم كتم صوتك وإعادتك لوضع المستمع من قِبل الإدارة"
                    else "تم سحب صلاحية التحدث وإعادتك لوضع المستمع",
                    isSuccess = false
                )
            }
            previousCanSpeak = canSpeakNow
        }
    }

    // Register participant presence in DB on join
    LaunchedEffect(liveRoom.id, currentUser?.id, isBannedByAdmin, isKickedByAdmin) {
        val user = currentUser
        if (user != null && liveRoom.isLive && !isBannedByAdmin && !isKickedByAdmin) {
            LiveAudioBroadcastCloudStore.recordParticipantJoined(
                context = context,
                rawSchoolId = cleanSchoolId,
                roomId = liveRoom.id,
                user = user,
                initialMicOn = isSpeakerEngineMode && isLocalMicOn
            )
        }
    }

    // Sync local mic ON/OFF state to Firebase so every participant sees accurate mic icon on Stage
    LaunchedEffect(liveRoom.id, currentUserId, isSpeakerEngineMode, isLocalMicOn, liveRoom.isLive) {
        if (currentUserId.isNotBlank() && liveRoom.isLive && !isBannedByAdmin && !isKickedByAdmin) {
            val effectiveMic = isSpeakerEngineMode && isLocalMicOn
            LiveAudioBroadcastCloudStore.updateParticipantMicState(
                context = context,
                rawSchoolId = cleanSchoolId,
                roomId = liveRoom.id,
                userId = currentUserId,
                isMicOn = effectiveMic,
                isSpeaking = effectiveMic && isLocalSpeaking
            )
        }
    }

    // Throttled lightweight speaking wave sync to RTDB for remote pulsing rings
    var lastPublishedSpeaking by remember { mutableStateOf(false) }
    var lastSpeakingPublishMs by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isLocalSpeaking, isSpeakerEngineMode, isLocalMicOn) {
        val desired = isSpeakerEngineMode && isLocalMicOn && isLocalSpeaking
        if (desired == lastPublishedSpeaking || currentUserId.isBlank() || !liveRoom.isLive) return@LaunchedEffect
        val now = System.currentTimeMillis()
        val elapsed = now - lastSpeakingPublishMs
        if (elapsed < 900L) {
            delay(900L - elapsed)
        }
        lastPublishedSpeaking = desired
        lastSpeakingPublishMs = System.currentTimeMillis()
        LiveAudioBroadcastCloudStore.patchParticipantSpeakingFast(
            rawSchoolId = cleanSchoolId,
            roomId = liveRoom.id,
            userId = currentUserId,
            isSpeaking = desired
        )
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
            showLeaveBroadcastConfirm = true
        }
    }

    val teachingStaff = remember(accounts) {
        accounts.filter { it.role == UserRole.TEACHER && it.isActive && !it.isEffectiveBusDriver }
    }

    // Eligible school members NOT currently present in the broadcast (for "تنبيه عضو للدخول")
    val eligibleAbsentUsers = remember(
        accounts,
        students,
        liveRoom.activeParticipants,
        liveRoom.bannedUserIds,
        currentUserId,
        liveRoom.targetAudience,
        liveRoom.targetGrade,
        liveRoom.targetSection
    ) {
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

    // Build headless Zego audio engine HTML (reloads cleanly only when role/speaker-permission or retry changes)
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
        isSpeakerEngineMode,
        engineReloadNonce
    ) {
        buildHeadlessZegoAudioEngineHtml(
            appId = effectiveAppId,
            serverSecret = effectiveSecret,
            roomId = zegoRoomId,
            cleanUserId = cleanUserId,
            userDisplayName = displayName,
            isSpeakerAllowed = isSpeakerEngineMode,
            initialMicEnabled = isSpeakerEngineMode && isLocalMicOn
        )
    }
    var loadedHtmlSignature by remember { mutableStateOf("") }

    // Root container: Layer 0 = 1dp invisible WebRTC audio engine; Layer 1 = 100% Native Arabic Spaces UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("live_audio_broadcast_room_screen")
    ) {
        // Layer 0: Hidden Background ZEGOCLOUD WebRTC Audio Engine (ZERO visible English UI!)
        if (removalReasonDialog == null && liveRoom.isLive) {
            AndroidView(
                modifier = Modifier
                    .size(1.dp)
                    .alpha(0.01f)
                    .testTag("zego_live_audio_webview"),
                factory = { ctx ->
                    WebView(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(2, 2)
                        setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            allowContentAccess = true
                            useWideViewPort = false
                            loadWithOverviewMode = false
                            cacheMode = WebSettings.LOAD_DEFAULT
                            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        }
                        addJavascriptInterface(
                            MadrasatiBroadcastJsBridge(
                                appContext = ctx.applicationContext,
                                onEngineConnected = {
                                    connectionState = AudioEngineConnectionState.CONNECTED
                                    connectionErrorText = null
                                },
                                onEngineError = { errMsg ->
                                    connectionState = AudioEngineConnectionState.ERROR
                                    connectionErrorText = errMsg
                                },
                                onLocalSpeaking = { speaking ->
                                    isLocalSpeaking = speaking
                                },
                                onRemoteAudio = { active ->
                                    isRemoteAudioActive = active
                                },
                                onMicChanged = { micOn ->
                                    if (!latestSpeakerEngineMode) {
                                        isLocalMicOn = false
                                    } else {
                                        isLocalMicOn = micOn
                                    }
                                }
                            ),
                            "MadrasatiBroadcastBridge"
                        )
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                super.onPageFinished(view, url)
                                if (!latestSpeakerEngineMode) {
                                    view?.evaluateJavascript(
                                        "window.forceHardMuteAndStopMic && window.forceHardMuteAndStopMic();",
                                        null
                                    )
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onPermissionRequest(request: PermissionRequest?) {
                                request?.let { req ->
                                    val allowCapture = latestSpeakerEngineMode && latestHasAudioPermission
                                    val approved = req.resources.filter { res ->
                                        when (res) {
                                            PermissionRequest.RESOURCE_AUDIO_CAPTURE -> allowCapture
                                            PermissionRequest.RESOURCE_PROTECTED_MEDIA_ID -> true
                                            else -> false
                                        }
                                    }.toTypedArray()
                                    runCatching {
                                        if (approved.isNotEmpty()) {
                                            req.grant(approved)
                                        } else {
                                            req.deny()
                                        }
                                    }
                                }
                            }
                        }
                        webViewRef = this
                        loadedHtmlSignature = htmlContent
                        connectionState = AudioEngineConnectionState.CONNECTING
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
                        connectionState = AudioEngineConnectionState.CONNECTING
                        connectionErrorText = null
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

        // Layer 1: Full-Screen Native Arabic Emerald Broadcast Room UI
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // 1) Emerald Studio Top Header with Principal Moderation & Invite Controls
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
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (canSpeakNow) Color(0xFF10B981).copy(alpha = 0.30f) else Color.White.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = when {
                                        isPrincipalOrCreator -> "مشرف البث"
                                        canSpeakNow -> "متحدث مخوّل"
                                        else -> "مستمع"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
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

                    // Compact header End/Leave button
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
                                    text = "إنهاء",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    softWrap = false
                                )
                            }
                        }
                    } else {
                        Surface(
                            onClick = { showLeaveBroadcastConfirm = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(0.9.dp, Color.White.copy(alpha = 0.35f)),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("room_leave_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
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

                // Principal Moderation, Hand-Raise Alerts, Urgent Invite & Co-Host Bar
                if (isPrincipalOrCreator) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(start = 12.dp, end = 12.dp, bottom = 9.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1) Participants & Moderation ("المشاركون والرقابة") + Hand Raise count highlight
                        Surface(
                            onClick = { showModerationSheet = true },
                            shape = RoundedCornerShape(11.dp),
                            color = if (pendingHandRaiseCount > 0) BroadcastGoldAccent.copy(alpha = 0.36f)
                            else Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(
                                if (pendingHandRaiseCount > 0) 1.1.dp else 0.85.dp,
                                BroadcastGoldLight.copy(alpha = if (pendingHandRaiseCount > 0) 0.95f else 0.65f)
                            ),
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
                                    text = if (pendingHandRaiseCount > 0) {
                                        "المشاركون (${liveRoom.activeParticipants.size}) • $pendingHandRaiseCount طلب"
                                    } else {
                                        "المشاركون والرقابة (${liveRoom.activeParticipants.size})"
                                    },
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

                        // 3) Delegate Co-Host ("تخويل متحدث")
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
                                    contentDescription = "تخويل متحدث",
                                    tint = BroadcastMintSoft,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "تخويل متحدث",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

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
                            text = "تم كتم صوتك وإعادتك لوضع المستمع من قِبل الإدارة حتى إلغاء الكتم.",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // 2) Bespoke Native Arabic Spaces Room Body (Speakers Stage + Listeners Grid)
            if (removalReasonDialog != null) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                )
            } else {
                LiveAudioSpaceStageBody(
                    room = liveRoom,
                    currentUserId = currentUserId,
                    schoolConfig = schoolConfig,
                    isPrincipalOrCreator = isPrincipalOrCreator,
                    canSpeakNow = canSpeakNow,
                    hasAudioPermission = hasAudioPermission,
                    permissionDeniedOnce = permissionDeniedOnce,
                    isLocalMicOn = isLocalMicOn,
                    isLocalSpeaking = isLocalSpeaking,
                    isRemoteAudioActive = isRemoteAudioActive,
                    connectionState = connectionState,
                    connectionErrorText = connectionErrorText,
                    onRetryConnection = {
                        engineReloadNonce += 1
                    },
                    onRequestMicPermission = {
                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    onOpenAppSettings = {
                        runCatching {
                            val intent = Intent(
                                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", context.packageName, null)
                            )
                            context.startActivity(intent)
                        }
                    },
                    onGrantSpeak = { participant ->
                        scope.launch {
                            LiveAudioBroadcastCloudStore.grantSpeakerPermission(
                                context = context,
                                rawSchoolId = cleanSchoolId,
                                roomId = liveRoom.id,
                                targetUserId = participant.userId
                            )
                            viewModel.showToastAlert(
                                "تم تخويل ${participant.fullName} للتحدث ومنحه الميكروفون",
                                isSuccess = true
                            )
                        }
                    },
                    onDeclineSpeakRequest = { participant ->
                        scope.launch {
                            LiveAudioBroadcastCloudStore.declineSpeakRequest(
                                context = context,
                                rawSchoolId = cleanSchoolId,
                                roomId = liveRoom.id,
                                targetUserId = participant.userId
                            )
                            viewModel.showToastAlert(
                                "تم رفض طلب التحدث من ${participant.fullName}",
                                isSuccess = true
                            )
                        }
                    },
                    onOpenModerationForParticipant = {
                        if (isPrincipalOrCreator) {
                            showModerationSheet = true
                        }
                    },
                    modifier = Modifier.weight(1f)
                )

                // 3) Custom 100% Arabic Bottom Dock (Speakers vs Listeners strictly separated!)
                LiveAudioArabicBottomDock(
                    canSpeakNow = canSpeakNow,
                    isPrincipalOrCreator = isPrincipalOrCreator,
                    isMutedByAdmin = isMutedByAdmin,
                    hasAudioPermission = hasAudioPermission,
                    isLocalMicOn = isLocalMicOn,
                    hasSelfRaisedHand = hasSelfRaisedHand,
                    isSpeakerphoneOn = isSpeakerphoneOn,
                    onToggleMic = {
                        if (!canSpeakNow) return@LiveAudioArabicBottomDock
                        if (!hasAudioPermission) {
                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            return@LiveAudioArabicBottomDock
                        }
                        val nextMic = !isLocalMicOn
                        isLocalMicOn = nextMic
                        if (!nextMic) isLocalSpeaking = false
                        runCatching {
                            webViewRef?.evaluateJavascript(
                                "window.setLocalMicEnabled && window.setLocalMicEnabled(${if (nextMic) "true" else "false"});",
                                null
                            )
                        }
                    },
                    onToggleSpeakerphone = {
                        val nextSpeaker = !isSpeakerphoneOn
                        isSpeakerphoneOn = nextSpeaker
                        audioManager?.let { applyBroadcastAudioOutputRoute(it, nextSpeaker) }
                        viewModel.showToastAlert(
                            if (nextSpeaker) "تم التحويل إلى السماعة الخارجية" else "تم التحويل إلى سماعة الأذن",
                            isSuccess = true
                        )
                    },
                    onToggleRaiseHand = {
                        if (currentUserId.isBlank() || canSpeakNow || isMutedByAdmin) return@LiveAudioArabicBottomDock
                        val wasRaised = hasSelfRaisedHand
                        scope.launch {
                            LiveAudioBroadcastCloudStore.toggleRaiseHand(
                                context = context,
                                rawSchoolId = cleanSchoolId,
                                roomId = liveRoom.id,
                                userId = currentUserId
                            )
                            viewModel.showToastAlert(
                                if (wasRaised) "تم إلغاء طلب التحدث"
                                else "تم إرسال طلب التحدث (رفع اليد) إلى مشرف البث",
                                isSuccess = true
                            )
                        }
                    },
                    onRequestEndOrLeave = {
                        if (isPrincipalOrCreator) {
                            showEndBroadcastConfirm = true
                        } else {
                            showLeaveBroadcastConfirm = true
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

    // Leave Broadcast Arabic Confirmation Dialog (Listeners & Co-Host Speakers)
    if (showLeaveBroadcastConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveBroadcastConfirm = false },
            title = {
                Text(
                    text = "مغادرة غرفة البث الصوتي",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "هل تريد مغادرة البث الصوتي المباشر الآن؟ يمكنك العودة للاستماع ما دامت الجلسة مستمرة.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveBroadcastConfirm = false
                        leaveRoomCleanly()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_leave_broadcast_dialog_btn")
                ) {
                    Text("نعم، مغادرة البث", fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveBroadcastConfirm = false }) {
                    Text("البقاء في البث", fontWeight = FontWeight.Bold)
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
            onGrantSpeak = { participant ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.grantSpeakerPermission(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId
                    )
                    viewModel.showToastAlert("تم تخويل ${participant.fullName} للتحدث فوراً", isSuccess = true)
                }
            },
            onRevokeSpeak = { participant ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.revokeSpeakerPermission(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId
                    )
                    viewModel.showToastAlert("تم سحب صلاحية التحدث من ${participant.fullName} وإعادته لمستمع", isSuccess = true)
                }
            },
            onDeclineSpeakRequest = { participant ->
                scope.launch {
                    LiveAudioBroadcastCloudStore.declineSpeakRequest(
                        context = context,
                        rawSchoolId = cleanSchoolId,
                        roomId = liveRoom.id,
                        targetUserId = participant.userId
                    )
                    viewModel.showToastAlert("تم رفض طلب التحدث من ${participant.fullName}", isSuccess = true)
                }
            },
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
                        else "تم كتم صوت ${participant.fullName} وإعادته لوضع المستمع فوراً",
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

    // Co-Host / Authorized Speakers Management Sheet (Principal / Host)
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
                    text = "إدارة وتخويل المتحدثين والمشرفين المساعدين",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "أي كادر يتم تخويله هنا يحصل فوراً على صلاحية التحدث وفتح الميكروفون، وفور إزالة التحديد يُغلق الميكروفون لديه ويعود لمستمع.",
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
                    text = if (isUpdatingCoHosts) "جارٍ الحفظ..." else "حفظ صلاحيات التحدث",
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
                            viewModel.showToastAlert("تم تحديث قائمة المتحدثين المخوّلين فوراً", isSuccess = true)
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
 * Builds the headless background ZEGOCLOUD WebRTC audio engine HTML page:
 * - Zero visible English UI (all Zego chrome disabled and hidden via CSS)
 * - Strict speaker vs listener hardware mic enforcement inside getUserMedia wrapper
 * - Pre-warms audio input device ONLY for authorized speakers to eliminate WebView "no mic/speaker detected" errors
 * - Keeps all remote <audio> elements unmuted at full volume and playing continuously
 * - Exposes window.setLocalMicEnabled(bool), window.forceHardMuteAndStopMic(), and window.destroyZegoRoom()
 * - Streams real-time local speaking detection via AudioContext AnalyserNode back to Compose
 */
private fun buildHeadlessZegoAudioEngineHtml(
    appId: Long,
    serverSecret: String,
    roomId: String,
    cleanUserId: String,
    userDisplayName: String,
    isSpeakerAllowed: Boolean,
    initialMicEnabled: Boolean
): String {
    val jsSecret = JSONObject.quote(serverSecret)
    val jsRoomId = JSONObject.quote(roomId)
    val jsUserId = JSONObject.quote(cleanUserId)
    val jsUserName = JSONObject.quote(userDisplayName)

    return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0" />
  <style>
    html, body, #zego-root {
      margin: 0;
      padding: 0;
      width: 1px;
      height: 1px;
      overflow: hidden;
      opacity: 0 !important;
      visibility: hidden !important;
      pointer-events: none !important;
      background: transparent !important;
    }
  </style>
</head>
<body>
  <div id="zego-root"></div>

  <script>
    (function () {
      window.__madrasatiCanSpeak = ${if (isSpeakerAllowed) "true" else "false"};
      window.__madrasatiMicDesired = ${if (isSpeakerAllowed && initialMicEnabled) "true" else "false"};
      window.__madrasatiAudioTracks = [];
      var localAudioCtx = null;
      var localAnalyser = null;
      var lastLocalSpeaking = false;
      var lastSpeechPeakMs = 0;
      var lastRemoteActive = false;

      function notifyBridge(fnName, arg) {
        try {
          if (window.MadrasatiBroadcastBridge && typeof window.MadrasatiBroadcastBridge[fnName] === "function") {
            if (typeof arg === "undefined") {
              window.MadrasatiBroadcastBridge[fnName]();
            } else {
              window.MadrasatiBroadcastBridge[fnName](arg);
            }
          }
        } catch (e) {}
      }

      function attachLocalVoiceMeter(stream) {
        try {
          var AudioCtx = window.AudioContext || window.webkitAudioContext;
          if (!AudioCtx || !stream) return;
          if (!localAudioCtx) {
            localAudioCtx = new AudioCtx();
          }
          if (localAudioCtx.state === "suspended") {
            localAudioCtx.resume().catch(function () {});
          }
          var source = localAudioCtx.createMediaStreamSource(stream);
          localAnalyser = localAudioCtx.createAnalyser();
          localAnalyser.fftSize = 256;
          source.connect(localAnalyser);
        } catch (e) {}
      }

      // Strictly wrap getUserMedia so Listeners can NEVER open hardware microphone
      // and Authorized Speakers get instant, zero-latency hardware mic track control + voice level detection.
      if (navigator.mediaDevices && typeof navigator.mediaDevices.getUserMedia === "function") {
        var origGetUserMedia = navigator.mediaDevices.getUserMedia.bind(navigator.mediaDevices);
        window.__origGetUserMedia = origGetUserMedia;

        navigator.mediaDevices.getUserMedia = function (constraints) {
          if (!window.__madrasatiCanSpeak) {
            return Promise.reject(new DOMException("Listener mode: microphone locked", "NotAllowedError"));
          }
          var safeConstraints = constraints || { audio: true, video: false };
          if (safeConstraints && typeof safeConstraints === "object") {
            safeConstraints = Object.assign({}, safeConstraints, {
              video: false,
              audio: safeConstraints.audio === false ? false : {
                echoCancellation: true,
                noiseSuppression: true,
                autoGainControl: true
              }
            });
          }
          return origGetUserMedia(safeConstraints).then(function (stream) {
            try {
              var tracks = stream.getAudioTracks ? stream.getAudioTracks() : [];
              tracks.forEach(function (t) {
                t.enabled = !!(window.__madrasatiCanSpeak && window.__madrasatiMicDesired);
                window.__madrasatiAudioTracks.push(t);
              });
              if (tracks.length > 0) {
                attachLocalVoiceMeter(stream);
              }
            } catch (e) {}
            return stream;
          });
        };
      }

      function syncAllLocalTracks() {
        try {
          window.__madrasatiAudioTracks = (window.__madrasatiAudioTracks || []).filter(function (t) {
            return t && t.readyState !== "ended";
          });
          var shouldBeOn = !!(window.__madrasatiCanSpeak && window.__madrasatiMicDesired);
          window.__madrasatiAudioTracks.forEach(function (t) {
            if (!window.__madrasatiCanSpeak) {
              t.enabled = false;
              try { t.stop(); } catch (e) {}
            } else {
              t.enabled = shouldBeOn;
            }
          });
          if (!window.__madrasatiCanSpeak) {
            window.__madrasatiAudioTracks = [];
          }
        } catch (e) {}
      }

      window.setLocalMicEnabled = function (enabled) {
        if (!window.__madrasatiCanSpeak) {
          window.__madrasatiMicDesired = false;
          syncAllLocalTracks();
          notifyBridge("onMicStateChanged", false);
          return;
        }
        window.__madrasatiMicDesired = !!enabled;
        syncAllLocalTracks();
        if (!window.__madrasatiMicDesired && lastLocalSpeaking) {
          lastLocalSpeaking = false;
          notifyBridge("onLocalSpeakingChanged", false);
        }
        notifyBridge("onMicStateChanged", window.__madrasatiMicDesired);
      };

      window.forceHardMuteAndStopMic = function () {
        window.__madrasatiCanSpeak = false;
        window.__madrasatiMicDesired = false;
        syncAllLocalTracks();
        if (lastLocalSpeaking) {
          lastLocalSpeaking = false;
          notifyBridge("onLocalSpeakingChanged", false);
        }
        notifyBridge("onMicStateChanged", false);
      };

      // Local voice activity meter (150ms interval) for live emerald soundwave ring
      var meterData = new Uint8Array(128);
      setInterval(function () {
        syncAllLocalTracks();
        if (window.__madrasatiCanSpeak && window.__madrasatiMicDesired && localAnalyser) {
          try {
            if (localAudioCtx && localAudioCtx.state === "suspended") {
              localAudioCtx.resume().catch(function () {});
            }
            localAnalyser.getByteTimeDomainData(meterData);
            var sumSq = 0;
            for (var i = 0; i < meterData.length; i++) {
              var norm = (meterData[i] - 128) / 128;
              sumSq += norm * norm;
            }
            var rms = Math.sqrt(sumSq / meterData.length);
            var now = Date.now();
            if (rms > 0.022) {
              lastSpeechPeakMs = now;
              if (!lastLocalSpeaking) {
                lastLocalSpeaking = true;
                notifyBridge("onLocalSpeakingChanged", true);
              }
            } else if (lastLocalSpeaking && (now - lastSpeechPeakMs > 550)) {
              lastLocalSpeaking = false;
              notifyBridge("onLocalSpeakingChanged", false);
            }
          } catch (e) {}
        } else if (lastLocalSpeaking) {
          lastLocalSpeaking = false;
          notifyBridge("onLocalSpeakingChanged", false);
        }
      }, 150);

      // Keep all remote WebRTC <audio>/<video> streams unmuted, full volume, and playing cleanly
      setInterval(function () {
        try {
          var mediaEls = document.querySelectorAll("audio, video");
          var anyRemotePlaying = false;
          for (var i = 0; i < mediaEls.length; i++) {
            var el = mediaEls[i];
            if (!el) continue;
            if (el.srcObject && el.srcObject.getAudioTracks) {
              var remoteTracks = el.srcObject.getAudioTracks();
              var isLocal = remoteTracks.some(function (rt) {
                return window.__madrasatiAudioTracks.indexOf(rt) !== -1;
              });
              if (!isLocal && remoteTracks.length > 0) {
                if (el.muted) el.muted = false;
                if (el.volume < 1.0) el.volume = 1.0;
                if (el.paused) {
                  el.play().catch(function () {});
                }
                if (!el.paused && remoteTracks.some(function (rt) { return rt.readyState === "live" && rt.enabled; })) {
                  anyRemotePlaying = true;
                }
              }
            }
          }
          if (anyRemotePlaying !== lastRemoteActive) {
            lastRemoteActive = anyRemotePlaying;
            notifyBridge("onRemoteAudioStateChanged", anyRemotePlaying);
          }
        } catch (e) {}
      }, 550);

      window.madrasatiNotifyExternal = function (title, body, dedupKey) {
        try {
          if (window.MadrasatiBroadcastBridge && typeof window.MadrasatiBroadcastBridge.notifyExternal === "function") {
            window.MadrasatiBroadcastBridge.notifyExternal(String(title || ""), String(body || ""), String(dedupKey || ""));
          }
        } catch (e) {}
      };
    })();
  </script>

  <script src="https://unpkg.com/@zegocloud/zego-uikit-prebuilt/zego-uikit-prebuilt.js"></script>
  <script>
    (function () {
      var zpInstance = null;

      window.destroyZegoRoom = function () {
        try {
          if (window.forceHardMuteAndStopMic) {
            window.forceHardMuteAndStopMic();
          }
          if (zpInstance && typeof zpInstance.destroy === "function") {
            zpInstance.destroy();
          }
        } catch (e) {}
        zpInstance = null;
      };

      function notifyErr(msg) {
        try {
          if (window.MadrasatiBroadcastBridge && window.MadrasatiBroadcastBridge.onRoomError) {
            window.MadrasatiBroadcastBridge.onRoomError(msg);
          }
        } catch (e) {}
      }

      function notifyConnected() {
        try {
          if (window.MadrasatiBroadcastBridge && window.MadrasatiBroadcastBridge.onRoomConnected) {
            window.MadrasatiBroadcastBridge.onRoomConnected();
          }
        } catch (e) {}
      }

      function joinZegoAudioRoom() {
        try {
          if (typeof ZegoUIKitPrebuilt === "undefined") {
            notifyErr("تعذّر تحميل محرك البث الصوتي. يرجى التحقق من اتصال الإنترنت.");
            return;
          }

          var appID = Number($appId);
          var serverSecret = $jsSecret;
          var roomID = $jsRoomId;
          var userID = $jsUserId;
          var userName = $jsUserName;
          var isSpeaker = !!window.__madrasatiCanSpeak;

          var kitToken = ZegoUIKitPrebuilt.generateKitTokenForTest(
            appID,
            serverSecret,
            roomID,
            userID,
            userName
          );

          var role = isSpeaker ? ZegoUIKitPrebuilt.Host : ZegoUIKitPrebuilt.Audience;
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
            showRoomTimer: false,
            showUserList: false,
            showTextChat: false,
            showAudioVideoSettingsButton: false,
            showMyCameraToggleButton: false,
            showMyMicrophoneToggleButton: false,
            showScreenSharingButton: false,
            showLayoutButton: false,
            showMoreButton: false,
            showRoomDetailsButton: false,
            showTurnOffRemoteCameraButton: false,
            showTurnOffRemoteMicrophoneButton: false,
            showRemoveUserButton: false,
            turnOnMicrophoneWhenJoining: isSpeaker && !!window.__madrasatiMicDesired,
            turnOnCameraWhenJoining: false,
            lowerLeftNotification: {
              showUserJoinAndLeave: false,
              showTextChat: false
            },
            onJoinRoom: function () {
              notifyConnected();
              if (!window.__madrasatiCanSpeak && window.forceHardMuteAndStopMic) {
                window.forceHardMuteAndStopMic();
              }
            },
            onLeaveRoom: function () {
              window.destroyZegoRoom();
            }
          });

          setTimeout(notifyConnected, 2200);
        } catch (err) {
          notifyErr("حدث خطأ أثناء تشغيل محرك البث الصوتي. تأكد من الاتصال بالإنترنت.");
        }
      }

      function initEngine() {
        // Pre-warm audio hardware ONLY for authorized speakers so Chromium WebView immediately detects
        // phone microphone & speaker devices before Zego enumerates devices.
        if (window.__madrasatiCanSpeak && window.__origGetUserMedia) {
          window.__origGetUserMedia({
            audio: {
              echoCancellation: true,
              noiseSuppression: true,
              autoGainControl: true
            },
            video: false
          }).then(function (probeStream) {
            try {
              probeStream.getTracks().forEach(function (t) { t.stop(); });
            } catch (e) {}
            joinZegoAudioRoom();
          }).catch(function () {
            joinZegoAudioRoom();
          });
        } else {
          joinZegoAudioRoom();
        }
      }

      window.addEventListener("beforeunload", function () {
        window.destroyZegoRoom();
      });

      if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", initEngine);
      } else {
        initEngine();
      }
    })();
  </script>
</body>
</html>
""".trimIndent()
}
