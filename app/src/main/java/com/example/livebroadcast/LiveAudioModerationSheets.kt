package com.example.livebroadcast

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolConfig
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.DailyStatusBadgeSpec
import com.example.ui.components.SmartWelcomeProfileAvatar
import com.example.ui.components.resolveSmartAvatarPersona

/**
 * Principal Moderation Sheet inside LiveAudioBroadcastRoomScreen:
 * - Tab 0: Active Participants with Mute/Unmute, Kick, and Ban controls next to every participant.
 * - Tab 1: Banned Users list for this broadcast with one-tap Unban ("فك الحظر").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoomParticipantsModerationSheet(
    room: LiveAudioRoom,
    currentUserId: String,
    allAccounts: List<UserAccount>,
    schoolConfig: SchoolConfig,
    onToggleMute: (LiveRoomParticipant) -> Unit,
    onKick: (LiveRoomParticipant) -> Unit,
    onBan: (LiveRoomParticipant) -> Unit,
    onUnban: (userId: String, displayName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Active, 1 = Banned
    var confirmKickTarget by remember { mutableStateOf<LiveRoomParticipant?>(null) }
    var confirmBanTarget by remember { mutableStateOf<LiveRoomParticipant?>(null) }

    val activeList = remember(room.activeParticipants, room.hostId) {
        room.activeParticipants.values.sortedWith(
            compareByDescending<LiveRoomParticipant> { it.userId == room.hostId }
                .thenBy { it.joinedAt }
        )
    }

    val bannedEntries = remember(room.bannedUserIds, room.bannedUsersInfo, allAccounts) {
        room.bannedUserIds.distinct().map { uid ->
            val resolvedName = room.bannedUsersInfo[uid]
                ?: allAccounts.firstOrNull { it.id == uid }?.fullName
                ?: "مستخدم محظور"
            uid to resolvedName
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("room_moderation_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Filled.Shield,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "إدارة المشاركين والرقابة الفورية",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "التحكم الفوري بالصوت والطرد والحظر وفك الحظر داخل الغرفة",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }

            // Tab Switcher: Active vs Banned
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    onClick = { selectedTab = 0 },
                    shape = RoundedCornerShape(13.dp),
                    color = if (selectedTab == 0) colorScheme.primary else colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == 0) BroadcastGoldLight else colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("moderation_tab_active")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Groups,
                            contentDescription = null,
                            tint = if (selectedTab == 0) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المشاركون الآن (${activeList.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedTab == 0) colorScheme.onPrimary else colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    onClick = { selectedTab = 1 },
                    shape = RoundedCornerShape(13.dp),
                    color = if (selectedTab == 1) BroadcastCoralEnd else colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    border = BorderStroke(
                        1.dp,
                        if (selectedTab == 1) Color.White.copy(alpha = 0.35f) else colorScheme.outline.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                        .testTag("moderation_tab_banned")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Block,
                            contentDescription = null,
                            tint = if (selectedTab == 1) Color.White else colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "المحظورون (${bannedEntries.size})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selectedTab == 1) Color.White else colorScheme.onSurface
                        )
                    }
                }
            }

            if (selectedTab == 0) {
                if (activeList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد مشاركون متصلون حالياً غيرك في الغرفة.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        items(activeList, key = { "active_p_${it.userId}" }) { participant ->
                            val isSelf = participant.userId == currentUserId
                            val isHost = participant.userId == room.hostId
                            val isMuted = room.isUserMuted(participant.userId)
                            val roleEnum = runCatching { UserRole.valueOf(participant.role) }.getOrDefault(UserRole.TEACHER)
                            val persona = remember(participant.userId, participant.gender, participant.fullName) {
                                resolveSmartAvatarPersona(
                                    role = roleEnum,
                                    gender = participant.gender,
                                    schoolGenderType = schoolConfig.schoolGenderType,
                                    fullName = participant.fullName
                                )
                            }
                            val badge = remember(isMuted) {
                                DailyStatusBadgeSpec(
                                    label = if (isMuted) "مكتوم" else "متصل",
                                    ringColors = if (isMuted) listOf(BroadcastCoralEnd, Color(0xFFFCA5A5))
                                    else listOf(BroadcastRichEmerald, BroadcastGoldAccent),
                                    pillBackground = BroadcastDeepEmerald,
                                    pillBorder = Color.White,
                                    pillTextColor = Color.White
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(15.dp),
                                color = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    0.9.dp,
                                    if (isMuted) BroadcastCoralEnd.copy(alpha = 0.45f)
                                    else colorScheme.outlineVariant.copy(alpha = 0.65f)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(11.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        SmartWelcomeProfileAvatar(
                                            photoSource = participant.avatarUrl,
                                            contentDescription = participant.fullName,
                                            persona = persona,
                                            statusBadge = badge,
                                            onClick = {},
                                            avatarSize = 40.dp,
                                            showStatusBadge = false
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = participant.fullName,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = colorScheme.onSurface,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f, fill = false)
                                                )
                                                if (isHost) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = BroadcastGoldAccent.copy(alpha = 0.22f)
                                                    ) {
                                                        Text(
                                                            text = "المشرف",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) BroadcastGoldLight else Color(0xFF784E06),
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                } else if (isMuted) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = BroadcastCoralEnd.copy(alpha = 0.16f)
                                                    ) {
                                                        Text(
                                                            text = "مكتوم",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                                            fontWeight = FontWeight.Bold,
                                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
                                            }
                                            Text(
                                                text = roleEnum.arabicTitle,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    // Moderation Controls Row (Mute/Unmute, Kick, Ban) for non-self/non-admin participants
                                    if (!isSelf && roleEnum != UserRole.ADMIN) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            // 1) Mute / Unmute
                                            Surface(
                                                onClick = { onToggleMute(participant) },
                                                shape = RoundedCornerShape(10.dp),
                                                color = if (isMuted) colorScheme.primary.copy(alpha = 0.14f)
                                                else Color(0xFFD97706).copy(alpha = if (isDark) 0.24f else 0.13f),
                                                border = BorderStroke(
                                                    0.8.dp,
                                                    if (isMuted) colorScheme.primary.copy(alpha = 0.45f)
                                                    else Color(0xFFD97706).copy(alpha = 0.48f)
                                                ),
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .height(34.dp)
                                                    .testTag("mod_mute_${participant.userId}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isMuted) Icons.Filled.Mic else Icons.Filled.MicOff,
                                                        contentDescription = if (isMuted) "إلغاء الكتم" else "كتم الصوت",
                                                        tint = if (isMuted) colorScheme.primary
                                                        else if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (isMuted) "إلغاء الكتم" else "كتم الصوت",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isMuted) colorScheme.primary
                                                        else if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            // 2) Kick
                                            Surface(
                                                onClick = { confirmKickTarget = participant },
                                                shape = RoundedCornerShape(10.dp),
                                                color = Color(0xFF475569).copy(alpha = if (isDark) 0.30f else 0.12f),
                                                border = BorderStroke(0.8.dp, Color(0xFF64748B).copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .weight(0.9f)
                                                    .height(34.dp)
                                                    .testTag("mod_kick_${participant.userId}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.PersonRemove,
                                                        contentDescription = "طرد من البث",
                                                        tint = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "طرد",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDark) Color(0xFFE2E8F0) else Color(0xFF334155),
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }

                                            // 3) Ban
                                            Surface(
                                                onClick = { confirmBanTarget = participant },
                                                shape = RoundedCornerShape(10.dp),
                                                color = BroadcastCoralEnd.copy(alpha = if (isDark) 0.25f else 0.13f),
                                                border = BorderStroke(0.8.dp, BroadcastCoralEnd.copy(alpha = 0.55f)),
                                                modifier = Modifier
                                                    .weight(0.9f)
                                                    .height(34.dp)
                                                    .testTag("mod_ban_${participant.userId}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Block,
                                                        contentDescription = "حظر من البث",
                                                        tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = "حظر",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
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
            } else {
                // Tab 1: Banned Users List with Unban
                if (bannedEntries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "لا يوجد أي مستخدم محظور في هذا البث حالياً.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 340.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(bannedEntries, key = { "banned_${it.first}" }) { (uid, name) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFFEF2F2),
                                border = BorderStroke(0.9.dp, BroadcastCoralEnd.copy(alpha = 0.35f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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
                                            color = BroadcastCoralEnd.copy(alpha = 0.18f),
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = Icons.Filled.Block,
                                                    contentDescription = null,
                                                    tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = colorScheme.onSurface
                                            )
                                            Text(
                                                text = "محظور من دخول هذا البث",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd
                                            )
                                        }
                                    }

                                    Surface(
                                        onClick = { onUnban(uid, name) },
                                        shape = RoundedCornerShape(10.dp),
                                        color = colorScheme.primary,
                                        modifier = Modifier
                                            .height(34.dp)
                                            .testTag("mod_unban_$uid")
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.LockOpen,
                                                contentDescription = "فك الحظر",
                                                tint = colorScheme.onPrimary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = "فك الحظر",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.onPrimary,
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

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    // Confirm Kick
    confirmKickTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmKickTarget = null },
            title = { Text("تأكيد الطرد من البث", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "هل تريد إخراج «${target.fullName}» من هذه الجلسة الآن؟ سيُسمح له بالانضمام إلى البثوث اللاحقة.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = target
                        confirmKickTarget = null
                        onKick(p)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إخراج من البث", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmKickTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirm Ban
    confirmBanTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmBanTarget = null },
            title = { Text("تأكيد حظر المشارك من هذا البث", fontWeight = FontWeight.ExtraBold) },
            text = {
                Text(
                    text = "سيتم إخراج «${target.fullName}» فوراً ومنعه من إعادة الدخول لهذا البث ما لم تقم بفك الحظر عنه.",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val p = target
                        confirmBanTarget = null
                        onBan(p)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حظر من البث", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmBanTarget = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Searchable Multi-Select Sheet allowing the Principal to send an Urgent Join Invite
 * ("تنبيه عضو للدخول") to eligible school members who are not currently inside the broadcast.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UrgentMemberInviteSheet(
    eligibleAbsentUsers: List<UserAccount>,
    schoolConfig: SchoolConfig,
    isSending: Boolean,
    onSendInvites: (List<UserAccount>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val colorScheme = MaterialTheme.colorScheme
    var searchQuery by remember { mutableStateOf("") }
    val selectedIds = remember { mutableStateListOf<String>() }

    val filteredUsers = remember(eligibleAbsentUsers, searchQuery) {
        val q = searchQuery.trim()
        if (q.isBlank()) eligibleAbsentUsers
        else eligibleAbsentUsers.filter { u ->
            u.fullName.contains(q, ignoreCase = true) ||
                u.role.arabicTitle.contains(q, ignoreCase = true) ||
                u.specialty.contains(q, ignoreCase = true) ||
                u.gradeClass.contains(q, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("urgent_member_invite_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = BroadcastGoldAccent
                    )
                    Column {
                        Text(
                            text = "تنبيه عضو للدخول العاجل",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "اختر الأعضاء غير الموجودين حالياً في البث لإرسال تنبيه فوري بالانضمام",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Filled.Close, contentDescription = "إغلاق")
                }
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("ابحث بالاسم أو الصفة أو الصف...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Search,
                        contentDescription = null,
                        tint = colorScheme.primary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "مسح البحث")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("urgent_invite_search_input")
            )

            if (filteredUsers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (eligibleAbsentUsers.isEmpty()) {
                            "جميع الأعضاء المؤهلين موجودون حالياً داخل غرفة البث."
                        } else {
                            "لا توجد نتائج مطابقة للبحث."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 310.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredUsers, key = { "invite_cand_${it.id}" }) { user ->
                        val isChosen = user.id in selectedIds
                        val persona = remember(user.id, user.gender, user.fullName) {
                            resolveSmartAvatarPersona(
                                role = user.role,
                                gender = user.gender,
                                schoolGenderType = schoolConfig.schoolGenderType,
                                fullName = user.fullName
                            )
                        }
                        val badge = remember(isChosen) {
                            DailyStatusBadgeSpec(
                                label = user.role.arabicTitle,
                                ringColors = if (isChosen) listOf(BroadcastGoldAccent, BroadcastRichEmerald)
                                else listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0)),
                                pillBackground = BroadcastDeepEmerald,
                                pillBorder = Color.White,
                                pillTextColor = Color.White
                            )
                        }

                        Surface(
                            onClick = {
                                if (isChosen) selectedIds.remove(user.id)
                                else selectedIds.add(user.id)
                            },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isChosen) colorScheme.primaryContainer.copy(alpha = 0.45f)
                            else colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            border = BorderStroke(
                                width = if (isChosen) 1.2.dp else 0.8.dp,
                                color = if (isChosen) colorScheme.primary else colorScheme.outline.copy(alpha = 0.22f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("invite_candidate_${user.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                SmartWelcomeProfileAvatar(
                                    photoSource = user.photoBase64,
                                    contentDescription = user.fullName,
                                    persona = persona,
                                    statusBadge = badge,
                                    onClick = {
                                        if (isChosen) selectedIds.remove(user.id)
                                        else selectedIds.add(user.id)
                                    },
                                    avatarSize = 38.dp,
                                    showStatusBadge = false
                                )

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = user.fullName,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = buildString {
                                        append(user.role.arabicTitle)
                                        if (user.specialty.isNotBlank()) append(" • ${user.specialty}")
                                        else if (user.gradeClass.isNotBlank()) append(" • ${user.gradeClass}")
                                    }
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Icon(
                                    imageVector = if (isChosen) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = if (isChosen) "محدد للتنبيه" else "تحديد للتنبيه",
                                    tint = if (isChosen) colorScheme.primary else colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(thickness = 0.8.dp, color = colorScheme.outlineVariant.copy(alpha = 0.45f))

            SpringBroadcastButton(
                text = when {
                    isSending -> "جارٍ إرسال التنبيه العاجل..."
                    selectedIds.isEmpty() -> "اختر عضواً واحداً على الأقل"
                    else -> "إرسال تنبيه عاجل للدخول (${selectedIds.size})"
                },
                icon = Icons.Filled.NotificationsActive,
                enabled = !isSending && selectedIds.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                testTagName = "send_urgent_invites_confirm_btn",
                onClick = {
                    val chosen = eligibleAbsentUsers.filter { it.id in selectedIds }
                    if (chosen.isNotEmpty()) {
                        onSendInvites(chosen)
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
