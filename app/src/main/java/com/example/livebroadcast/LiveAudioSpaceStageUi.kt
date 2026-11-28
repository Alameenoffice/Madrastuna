package com.example.livebroadcast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SchoolConfig
import com.example.model.UserRole
import com.example.ui.components.DailyStatusBadgeSpec
import com.example.ui.components.SmartWelcomeProfileAvatar
import com.example.ui.components.resolveSmartAvatarPersona
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin

internal enum class AudioEngineConnectionState {
    CONNECTING,
    CONNECTED,
    ERROR
}

/**
 * Native Arabic Spaces-style Live Audio Broadcast Room body:
 * - Subtle studio acoustic wave backdrop (Light & Dark emerald identity)
 * - Live duration & audio connection pill
 * - Optional speaker mic permission prompt (shown ONLY to Host / Authorized Speaker if permission not yet granted)
 * - Principal inline hand-raise request approvals strip
 * - Top Stage: Speakers ONLY (Host + explicitly authorized speakers) with pulsing emerald soundwave ring & live mic icon
 * - Middle Grid: Listeners with clear "يطلب التحدث" badge for raised hands and zero mic icons
 */
@Composable
internal fun LiveAudioSpaceStageBody(
    room: LiveAudioRoom,
    currentUserId: String,
    schoolConfig: SchoolConfig,
    isPrincipalOrCreator: Boolean,
    canSpeakNow: Boolean,
    hasAudioPermission: Boolean,
    permissionDeniedOnce: Boolean,
    isLocalMicOn: Boolean,
    isLocalSpeaking: Boolean,
    isRemoteAudioActive: Boolean,
    connectionState: AudioEngineConnectionState,
    connectionErrorText: String?,
    onRetryConnection: () -> Unit,
    onRequestMicPermission: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onGrantSpeak: (LiveRoomParticipant) -> Unit,
    onDeclineSpeakRequest: (LiveRoomParticipant) -> Unit,
    onOpenModerationForParticipant: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    // Live ticking elapsed timer from broadcast start
    var elapsedSeconds by remember(room.id, room.effectiveStartedAt) {
        mutableLongStateOf(((System.currentTimeMillis() - room.effectiveStartedAt) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(room.id, room.effectiveStartedAt, room.isLive) {
        while (room.isLive) {
            elapsedSeconds = ((System.currentTimeMillis() - room.effectiveStartedAt) / 1000L).coerceAtLeast(0L)
            delay(1_000L)
        }
    }

    // Build authoritative Speakers list (Host + Admin + explicitly authorized non-muted Co-Hosts)
    val speakers = remember(room.activeParticipants, room.hostId, room.hostName, room.coHosts, room.mutedUserIds) {
        val list = mutableListOf<LiveRoomParticipant>()
        val hostEntry = room.activeParticipants[room.hostId] ?: LiveRoomParticipant(
            userId = room.hostId,
            fullName = room.hostName.ifBlank { "مشرف البث" },
            role = room.hostRole.ifBlank { UserRole.ADMIN.name },
            gender = room.hostGender,
            avatarUrl = room.hostAvatarUrl,
            joinedAt = room.effectiveStartedAt,
            isMicOn = true
        )
        list.add(hostEntry)

        room.activeParticipants.values
            .filter { p ->
                !p.userId.equals(room.hostId, ignoreCase = true) &&
                    room.canParticipantSpeak(p.userId, p.role)
            }
            .sortedWith(
                compareByDescending<LiveRoomParticipant> { it.role.equals(UserRole.ADMIN.name, ignoreCase = true) }
                    .thenBy { it.joinedAt }
            )
            .forEach { list.add(it) }

        list.distinctBy { it.userId }
    }

    // Build Listeners list (everyone active who is NOT an authorized speaker)
    val listeners = remember(room.activeParticipants, room.hostId, room.coHosts, room.mutedUserIds, room.handRaisedUserIds) {
        room.activeParticipants.values
            .filter { p -> !room.canParticipantSpeak(p.userId, p.role) }
            .sortedWith(
                compareByDescending<LiveRoomParticipant> { room.hasRaisedHand(it.userId) }
                    .thenBy { it.joinedAt }
            )
    }

    val handRaisedListeners = remember(listeners, room.handRaisedUserIds) {
        listeners.filter { room.hasRaisedHand(it.userId) }
    }

    val speakerRows = remember(speakers) { speakers.chunked(2) }
    val listenerRows = remember(listeners) { listeners.chunked(3) }

    val studioBackdrop = remember(isDark) {
        if (isDark) {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF071A14),
                    Color(0xFF0A211A),
                    Color(0xFF081712)
                )
            )
        } else {
            Brush.verticalGradient(
                colors = listOf(
                    Color(0xFFEEF9F4),
                    Color(0xFFFAFDFB),
                    Color(0xFFF1F9F5)
                )
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(studioBackdrop)
    ) {
        // Subtle ambient acoustic waves floating gently in studio background
        AmbientStudioAcousticWavesBackdrop(
            isDark = isDark,
            isVoiceActive = isLocalSpeaking || isRemoteAudioActive,
            modifier = Modifier
                .fillMaxWidth()
                .height(170.dp)
                .align(Alignment.TopCenter)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("live_audio_arabic_space_body"),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1) Compact Studio Telemetry Pill (Timer + Connection State + Counts)
            item(key = "space_telemetry_strip") {
                StudioTelemetryStrip(
                    elapsedSeconds = elapsedSeconds,
                    speakersCount = speakers.size,
                    listenersCount = listeners.size,
                    connectionState = connectionState,
                    connectionErrorText = connectionErrorText,
                    onRetryConnection = onRetryConnection,
                    isDark = isDark
                )
            }

            // 2) Speaker-only Mic Permission Prompt (NEVER shown to Listeners!)
            if (canSpeakNow && !hasAudioPermission) {
                item(key = "speaker_mic_permission_card") {
                    SpeakerMicPermissionBanner(
                        permissionDeniedOnce = permissionDeniedOnce,
                        isDark = isDark,
                        onRequestPermission = onRequestMicPermission,
                        onOpenSettings = onOpenAppSettings
                    )
                }
            }

            // 3) Principal Inline Hand-Raise Approvals Strip
            if (isPrincipalOrCreator && handRaisedListeners.isNotEmpty()) {
                item(key = "principal_hand_raise_queue") {
                    HandRaiseRequestsInlineBanner(
                        requests = handRaisedListeners,
                        isDark = isDark,
                        onAccept = onGrantSpeak,
                        onDecline = onDeclineSpeakRequest
                    )
                }
            }

            // 4) Top Section: Speakers Stage ("منصة المتحدثين")
            item(key = "speakers_stage_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary.copy(alpha = if (isDark) 0.25f else 0.13f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Podcasts,
                                    contentDescription = null,
                                    tint = if (isDark) BroadcastMintSoft else BroadcastRichEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "منصة المتحدثين",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onBackground
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.11f),
                        border = BorderStroke(0.75.dp, colorScheme.primary.copy(alpha = 0.30f))
                    ) {
                        Text(
                            text = "${speakers.size} متحدث",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) BroadcastMintSoft else BroadcastDeepEmerald,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            items(speakerRows, key = { row -> "spk_row_${row.joinToString("_") { it.userId }}" }) { rowSpeakers ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowSpeakers.forEach { speaker ->
                        val isSelf = speaker.userId == currentUserId
                        val isHost = speaker.userId == room.hostId ||
                            speaker.role.equals(UserRole.ADMIN.name, ignoreCase = true)
                        val effectiveMicOn = if (isSelf) {
                            canSpeakNow && hasAudioPermission && isLocalMicOn
                        } else {
                            speaker.isMicOn
                        }
                        val remoteAudioFallbackForSoleSpeaker =
                            !isSelf && isRemoteAudioActive && effectiveMicOn &&
                                speakers.count { it.userId != currentUserId && it.isMicOn } <= 1
                        val isActivelySpeaking = if (isSelf) {
                            effectiveMicOn && isLocalSpeaking
                        } else {
                            effectiveMicOn && (speaker.isSpeaking || remoteAudioFallbackForSoleSpeaker)
                        }

                        SpeakerStageCard(
                            participant = speaker,
                            isHost = isHost,
                            isSelf = isSelf,
                            isMicOn = effectiveMicOn,
                            isSpeaking = isActivelySpeaking,
                            schoolConfig = schoolConfig,
                            isDark = isDark,
                            onClick = {
                                if (isPrincipalOrCreator && !isSelf) {
                                    onOpenModerationForParticipant()
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (rowSpeakers.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }

            // Soft divider between Stage and Audience
            item(key = "stage_audience_divider") {
                HorizontalDivider(
                    thickness = 0.85.dp,
                    color = if (isDark) Color.White.copy(alpha = 0.08f) else BroadcastDeepEmerald.copy(alpha = 0.10f),
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            // 5) Middle Section: Listeners Grid ("المستمعون")
            item(key = "listeners_section_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isDark) Color.White.copy(alpha = 0.09f) else Color(0xFFE2F2EB),
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Headphones,
                                    contentDescription = null,
                                    tint = if (isDark) Color(0xFFA7F3D0) else BroadcastRichEmerald,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Text(
                            text = "المستمعون",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onBackground
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (handRaisedListeners.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = BroadcastGoldAccent.copy(alpha = if (isDark) 0.28f else 0.18f),
                                border = BorderStroke(0.8.dp, BroadcastGoldAccent.copy(alpha = 0.65f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PanTool,
                                        contentDescription = null,
                                        tint = if (isDark) BroadcastGoldLight else Color(0xFF92400E),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Text(
                                        text = "${handRaisedListeners.size} طلب تحدث",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) BroadcastGoldLight else Color(0xFF92400E)
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.85f),
                            border = BorderStroke(0.75.dp, colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = "${listeners.size} مستمع",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }

            if (listeners.isEmpty()) {
                item(key = "listeners_empty_card") {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.04f) else Color.White.copy(alpha = 0.72f),
                        border = BorderStroke(
                            0.85.dp,
                            if (isDark) Color.White.copy(alpha = 0.08f) else BroadcastRichEmerald.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Groups,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بانتظار انضمام المستمعين إلى غرفة البث المباشر...",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                items(listenerRows, key = { row -> "lst_row_${row.joinToString("_") { it.userId }}" }) { rowListeners ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowListeners.forEach { listener ->
                            val hasHand = room.hasRaisedHand(listener.userId)
                            val isMuted = room.isUserMuted(listener.userId)
                            ListenerGridCard(
                                participant = listener,
                                isSelf = listener.userId == currentUserId,
                                hasRaisedHand = hasHand,
                                isMuted = isMuted,
                                schoolConfig = schoolConfig,
                                isDark = isDark,
                                onClick = {
                                    if (isPrincipalOrCreator) {
                                        onOpenModerationForParticipant()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat(3 - rowListeners.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            item(key = "space_bottom_clearance") {
                Spacer(modifier = Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun StudioTelemetryStrip(
    elapsedSeconds: Long,
    speakersCount: Int,
    listenersCount: Int,
    connectionState: AudioEngineConnectionState,
    connectionErrorText: String?,
    onRetryConnection: () -> Unit,
    isDark: Boolean
) {
    val formattedTime = remember(elapsedSeconds) {
        val h = elapsedSeconds / 3600L
        val m = (elapsedSeconds % 3600L) / 60L
        val s = elapsedSeconds % 60L
        if (h > 0L) String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%02d:%02d", m, s)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF0E2920).copy(alpha = 0.90f) else Color.White.copy(alpha = 0.88f),
        border = BorderStroke(
            0.9.dp,
            when (connectionState) {
                AudioEngineConnectionState.CONNECTED -> if (isDark) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF059669).copy(alpha = 0.22f)
                AudioEngineConnectionState.CONNECTING -> BroadcastGoldAccent.copy(alpha = 0.45f)
                AudioEngineConnectionState.ERROR -> BroadcastCoralEnd.copy(alpha = 0.55f)
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    when (connectionState) {
                        AudioEngineConnectionState.CONNECTING -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                strokeWidth = 2.dp,
                                color = BroadcastGoldAccent
                            )
                            Text(
                                text = "جارٍ تهيئة الاتصال الصوتي...",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) BroadcastGoldLight else Color(0xFF92400E),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AudioEngineConnectionState.CONNECTED -> {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF34D399) else Color(0xFF059669),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "متصل بالبث الصوتي المباشر",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFA7F3D0) else BroadcastDeepEmerald,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        AudioEngineConnectionState.ERROR -> {
                            Text(
                                text = connectionErrorText ?: "تعذّر الاتصال بمحرك الصوت",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (connectionState == AudioEngineConnectionState.ERROR) {
                    Surface(
                        onClick = onRetryConnection,
                        shape = RoundedCornerShape(10.dp),
                        color = BroadcastCoralEnd.copy(alpha = 0.16f),
                        modifier = Modifier.testTag("retry_audio_engine_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = "إعادة المحاولة",
                                tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "إعادة الاتصال",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd
                            )
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFECFDF5)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = if (isDark) BroadcastGoldLight else BroadcastRichEmerald,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = formattedTime,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color.White else BroadcastDeepEmerald
                            )
                        }
                    }
                }
            }

            Text(
                text = "المتواجدون الآن: ${speakersCount + listenersCount} • المتحدثون: $speakersCount • المستمعون: $listenersCount",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SpeakerMicPermissionBanner(
    permissionDeniedOnce: Boolean,
    isDark: Boolean,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF282012) else Color(0xFFFFFBEB),
        border = BorderStroke(1.dp, BroadcastGoldAccent.copy(alpha = 0.65f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("speaker_mic_permission_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = BroadcastGoldAccent.copy(alpha = 0.22f),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = null,
                            tint = if (isDark) BroadcastGoldLight else Color(0xFF92400E),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "أنت مخوّل للتحدث — يرجى تفعيل إذن الميكروفون",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color(0xFFFEF3C7) else Color(0xFF78350F)
                    )
                    Text(
                        text = "اسمح للتطبيق بالوصول إلى الميكروفون لبدء التحدث بوضوح في الغرفة.",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDark) Color(0xFFFDE68A).copy(alpha = 0.85f) else Color(0xFF92400E)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SpringBroadcastButton(
                    text = "السماح بالميكروفون",
                    icon = Icons.Filled.Mic,
                    onClick = onRequestPermission,
                    height = 40.dp,
                    cornerRadius = 12.dp,
                    modifier = Modifier.weight(1f),
                    testTagName = "request_audio_permission_retry_button"
                )
                if (permissionDeniedOnce) {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("الإعدادات", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun HandRaiseRequestsInlineBanner(
    requests: List<LiveRoomParticipant>,
    isDark: Boolean,
    onAccept: (LiveRoomParticipant) -> Unit,
    onDecline: (LiveRoomParticipant) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDark) Color(0xFF282211) else Color(0xFFFFFBEB),
        border = BorderStroke(1.1.dp, BroadcastGoldAccent.copy(alpha = 0.72f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("hand_raise_requests_banner")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.PanTool,
                    contentDescription = null,
                    tint = if (isDark) BroadcastGoldLight else Color(0xFFB45309),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "طلبات التحدث الواردة (${requests.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) BroadcastGoldLight else Color(0xFF78350F)
                )
            }

            requests.take(3).forEach { requester ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = requester.fullName,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Surface(
                        onClick = { onAccept(requester) },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("inline_accept_speak_${requester.userId}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "قبول التحدث",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "قبول وتخويل",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onPrimary,
                                softWrap = false
                            )
                        }
                    }

                    Surface(
                        onClick = { onDecline(requester) },
                        shape = RoundedCornerShape(10.dp),
                        color = BroadcastCoralEnd.copy(alpha = if (isDark) 0.25f else 0.14f),
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("inline_decline_speak_${requester.userId}")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "رفض الطلب",
                                tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Elegant Spaces-style Speaker Card on the Top Stage with pulsing green soundwave ring
 * around the avatar while talking and a crisp live microphone state badge.
 */
@Composable
private fun SpeakerStageCard(
    participant: LiveRoomParticipant,
    isHost: Boolean,
    isSelf: Boolean,
    isMicOn: Boolean,
    isSpeaking: Boolean,
    schoolConfig: SchoolConfig,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val roleEnum = remember(participant.role) {
        runCatching { UserRole.valueOf(participant.role) }.getOrDefault(UserRole.TEACHER)
    }
    val persona = remember(participant.userId, participant.gender, participant.fullName, roleEnum) {
        resolveSmartAvatarPersona(
            role = roleEnum,
            gender = participant.gender,
            schoolGenderType = schoolConfig.schoolGenderType,
            fullName = participant.fullName
        )
    }
    val staticBadge = remember(isHost, isMicOn) {
        DailyStatusBadgeSpec(
            label = if (isHost) "مشرف" else "متحدث",
            ringColors = if (isHost) listOf(BroadcastGoldAccent, BroadcastRichEmerald)
            else listOf(Color(0xFF10B981), BroadcastMintSoft),
            pillBackground = BroadcastDeepEmerald,
            pillBorder = Color.White,
            pillTextColor = Color.White
        )
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF0E2920).copy(alpha = 0.92f) else Color.White,
        border = BorderStroke(
            width = if (isSpeaking) 1.5.dp else 1.dp,
            color = when {
                isSpeaking -> Color(0xFF10B981)
                isHost -> BroadcastGoldAccent.copy(alpha = if (isDark) 0.55f else 0.45f)
                else -> colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.22f)
            }
        ),
        shadowElevation = if (isSpeaking) 5.dp else 1.5.dp,
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("speaker_stage_card_${participant.userId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Avatar + Animated Pulsing Emerald Soundwave Ring + Live Mic Corner Badge
            Box(
                modifier = Modifier.size(86.dp),
                contentAlignment = Alignment.Center
            ) {
                SpeakingAcousticRingCanvas(
                    isSpeaking = isSpeaking,
                    isMicOn = isMicOn,
                    isHost = isHost,
                    modifier = Modifier.fillMaxSize()
                )

                SmartWelcomeProfileAvatar(
                    photoSource = participant.avatarUrl,
                    contentDescription = participant.fullName,
                    persona = persona,
                    statusBadge = staticBadge,
                    onClick = onClick,
                    avatarSize = 62.dp,
                    showStatusBadge = false
                )

                // Crisp Mic Status Indicator Badge
                Surface(
                    shape = CircleShape,
                    color = if (isMicOn) Color(0xFF059669) else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0),
                    border = BorderStroke(1.8.dp, if (isDark) Color(0xFF0E2920) else Color.White),
                    modifier = Modifier
                        .size(26.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isMicOn) Icons.Filled.Mic else Icons.Filled.MicOff,
                            contentDescription = if (isMicOn) "الميكروفون مفتوح" else "الميكروفون مكتوم",
                            tint = if (isMicOn) Color.White else if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Speaker Name
            Text(
                text = if (isSelf) "${participant.fullName} (أنت)" else participant.fullName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.ExtraBold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            // Role Pill ("مشرف البث" / "متحدث مخوّل")
            Surface(
                shape = RoundedCornerShape(50),
                color = if (isHost) {
                    BroadcastGoldAccent.copy(alpha = if (isDark) 0.24f else 0.16f)
                } else {
                    colorScheme.primary.copy(alpha = if (isDark) 0.22f else 0.12f)
                },
                border = BorderStroke(
                    0.75.dp,
                    if (isHost) BroadcastGoldAccent.copy(alpha = 0.55f)
                    else colorScheme.primary.copy(alpha = 0.38f)
                )
            ) {
                Text(
                    text = if (isHost) "مشرف البث" else "متحدث مخوّل",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isHost) {
                        if (isDark) BroadcastGoldLight else Color(0xFF784E06)
                    } else {
                        if (isDark) BroadcastMintSoft else BroadcastDeepEmerald
                    },
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                )
            }
        }
    }
}

/**
 * Compact Listener Tile in the Audience Grid — strictly NO microphone controls or icons.
 * Highlights raised hand requests with a golden ring and "يطلب التحدث" badge.
 */
@Composable
private fun ListenerGridCard(
    participant: LiveRoomParticipant,
    isSelf: Boolean,
    hasRaisedHand: Boolean,
    isMuted: Boolean,
    schoolConfig: SchoolConfig,
    isDark: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val roleEnum = remember(participant.role) {
        runCatching { UserRole.valueOf(participant.role) }.getOrDefault(UserRole.STUDENT)
    }
    val persona = remember(participant.userId, participant.gender, participant.fullName, roleEnum) {
        resolveSmartAvatarPersona(
            role = roleEnum,
            gender = participant.gender,
            schoolGenderType = schoolConfig.schoolGenderType,
            fullName = participant.fullName
        )
    }
    val ringBadge = remember(hasRaisedHand, isMuted) {
        DailyStatusBadgeSpec(
            label = if (hasRaisedHand) "يطلب التحدث" else "مستمع",
            ringColors = when {
                hasRaisedHand -> listOf(BroadcastGoldAccent, BroadcastGoldLight)
                isMuted -> listOf(BroadcastCoralEnd, Color(0xFFFCA5A5))
                else -> listOf(Color(0xFF94A3B8), Color(0xFFCBD5E1))
            },
            pillBackground = BroadcastDeepEmerald,
            pillBorder = Color.White,
            pillTextColor = Color.White
        )
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when {
            hasRaisedHand -> if (isDark) Color(0xFF282111) else Color(0xFFFFFBEB)
            isDark -> Color.White.copy(alpha = 0.05f)
            else -> Color.White.copy(alpha = 0.86f)
        },
        border = BorderStroke(
            width = if (hasRaisedHand) 1.2.dp else 0.75.dp,
            color = when {
                hasRaisedHand -> BroadcastGoldAccent.copy(alpha = 0.78f)
                isMuted -> BroadcastCoralEnd.copy(alpha = 0.40f)
                else -> colorScheme.outlineVariant.copy(alpha = 0.45f)
            }
        ),
        modifier = modifier
            .clickable(onClick = onClick)
            .testTag("listener_grid_card_${participant.userId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                SmartWelcomeProfileAvatar(
                    photoSource = participant.avatarUrl,
                    contentDescription = participant.fullName,
                    persona = persona,
                    statusBadge = ringBadge,
                    onClick = onClick,
                    avatarSize = 46.dp,
                    showStatusBadge = false
                )

                if (hasRaisedHand) {
                    Surface(
                        shape = CircleShape,
                        color = BroadcastGoldAccent,
                        border = BorderStroke(1.5.dp, if (isDark) Color(0xFF282111) else Color.White),
                        modifier = Modifier
                            .size(22.dp)
                            .align(Alignment.BottomEnd)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.PanTool,
                                contentDescription = "يطلب التحدث",
                                tint = Color(0xFF1F1604),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = if (isSelf) "${participant.fullName} (أنت)" else participant.fullName,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            if (hasRaisedHand) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = BroadcastGoldAccent.copy(alpha = if (isDark) 0.28f else 0.18f)
                ) {
                    Text(
                        text = "يطلب التحدث",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) BroadcastGoldLight else Color(0xFF92400E),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            } else {
                Text(
                    text = if (isMuted) "مكتوم" else roleEnum.arabicTitle,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isMuted) {
                        if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd
                    } else {
                        colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Custom Compose Canvas pulsing emerald soundwave ring animated around an active speaker's avatar.
 */
@Composable
private fun SpeakingAcousticRingCanvas(
    isSpeaking: Boolean,
    isMicOn: Boolean,
    isHost: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "speaker_voice_wave_ring")
    val ripple1 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaker_ripple_1"
    )
    val ripple2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1350, delayMillis = 650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "speaker_ripple_2"
    )
    val gentleBreath by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaker_idle_breath"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val baseRadius = size.minDimension * 0.375f
        val maxExpand = size.minDimension * 0.115f

        if (isSpeaking && isMicOn) {
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = (1f - ripple1) * 0.55f),
                radius = baseRadius + maxExpand * ripple1,
                center = center,
                style = Stroke(width = 2.4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF34D399).copy(alpha = (1f - ripple2) * 0.42f),
                radius = baseRadius + maxExpand * ripple2,
                center = center,
                style = Stroke(width = 1.8.dp.toPx())
            )
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.85f),
                radius = baseRadius + 1.5.dp.toPx(),
                center = center,
                style = Stroke(width = 2.6.dp.toPx())
            )
        } else if (isMicOn) {
            drawCircle(
                color = Color(0xFF10B981).copy(alpha = 0.35f * gentleBreath),
                radius = baseRadius + 2.dp.toPx(),
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
        } else {
            drawCircle(
                color = if (isHost) BroadcastGoldAccent.copy(alpha = 0.38f) else Color(0xFF94A3B8).copy(alpha = 0.32f),
                radius = baseRadius + 1.5.dp.toPx(),
                center = center,
                style = Stroke(width = 1.6.dp.toPx())
            )
        }
    }
}

@Composable
private fun AmbientStudioAcousticWavesBackdrop(
    isDark: Boolean,
    isVoiceActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "studio_ambient_waves")
    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isVoiceActive) 3400 else 6200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "studio_wave_phase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val waveColor1 = if (isDark) Color(0xFF34D399).copy(alpha = if (isVoiceActive) 0.14f else 0.07f)
        else Color(0xFF059669).copy(alpha = if (isVoiceActive) 0.11f else 0.06f)
        val waveColor2 = if (isDark) BroadcastGoldLight.copy(alpha = 0.08f)
        else BroadcastGoldAccent.copy(alpha = 0.08f)

        fun drawSoftSine(color: Color, baseY: Float, amp: Float, freq: Float, offset: Float, strokeDp: Dp) {
            val path = Path()
            val steps = 42
            for (i in 0..steps) {
                val frac = i / steps.toFloat()
                val x = frac * w
                val y = baseY + sin((frac * freq * 2.0 * PI + phase + offset)).toFloat() * amp
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(width = strokeDp.toPx(), cap = StrokeCap.Round)
            )
        }

        drawSoftSine(waveColor1, h * 0.36f, if (isVoiceActive) 13f else 7f, 1.5f, 0f, 1.6.dp)
        drawSoftSine(waveColor2, h * 0.56f, if (isVoiceActive) 10f else 5f, 2.0f, 1.8f, 1.2.dp)
    }
}

/**
 * Custom 100% Arabic Bottom Control Dock replacing the hidden English ZEGOCLOUD bar:
 * - Authorized Speakers (Host / Admin / Co-Host): Clear Arabic Mic button ("تحدث" / "كتم")
 *   + Loudspeaker toggle ("السماعة" / "الأذن") + "إنهاء البث" (for creator/principal) or "مغادرة".
 * - Listeners: ZERO microphone button! Shows ONLY "طلب التحدث (رفع اليد)" / "إلغاء رفع اليد"
 *   + Loudspeaker toggle + "مغادرة البث".
 */
@Composable
internal fun LiveAudioArabicBottomDock(
    canSpeakNow: Boolean,
    isPrincipalOrCreator: Boolean,
    isMutedByAdmin: Boolean,
    hasAudioPermission: Boolean,
    isLocalMicOn: Boolean,
    hasSelfRaisedHand: Boolean,
    isSpeakerphoneOn: Boolean,
    onToggleMic: () -> Unit,
    onToggleSpeakerphone: () -> Unit,
    onToggleRaiseHand: () -> Unit,
    onRequestEndOrLeave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    Surface(
        color = if (isDark) Color(0xFF0A1F18) else Color.White,
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_audio_arabic_bottom_dock")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                thickness = 0.9.dp,
                color = if (isDark) Color(0xFF10B981).copy(alpha = 0.25f) else BroadcastDeepEmerald.copy(alpha = 0.12f)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (canSpeakNow) {
                    // 1) Primary Arabic Mic Button ("تحدث / كتم") — ONLY for Host & Authorized Speakers
                    val micActive = hasAudioPermission && isLocalMicOn
                    Surface(
                        onClick = onToggleMic,
                        shape = RoundedCornerShape(16.dp),
                        color = if (micActive) BroadcastRichEmerald
                        else if (isDark) Color(0xFF1A332A) else Color(0xFFECFDF5),
                        border = BorderStroke(
                            width = 1.2.dp,
                            color = if (micActive) BroadcastGoldLight.copy(alpha = 0.72f)
                            else colorScheme.primary.copy(alpha = 0.42f)
                        ),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(50.dp)
                            .testTag("arabic_dock_mic_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (micActive) Icons.Filled.Mic else Icons.Filled.MicOff,
                                contentDescription = if (micActive) "كتم الميكروفون" else "فتح الميكروفون للتحدث",
                                tint = if (micActive) Color.White else if (isDark) BroadcastMintSoft else BroadcastDeepEmerald,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    !hasAudioPermission -> "تفعيل الميكروفون"
                                    micActive -> "كتم"
                                    else -> "تحدث"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (micActive) Color.White else if (isDark) Color.White else BroadcastDeepEmerald,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 2) Loudspeaker / Earpiece Switch Button
                    Surface(
                        onClick = onToggleSpeakerphone,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSpeakerphoneOn) {
                            if (isDark) Color(0xFF12362B) else Color(0xFFE6F7F0)
                        } else {
                            colorScheme.surfaceVariant.copy(alpha = 0.75f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSpeakerphoneOn) colorScheme.primary.copy(alpha = 0.45f)
                            else colorScheme.outline.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(50.dp)
                            .testTag("arabic_dock_speaker_toggle_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isSpeakerphoneOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Hearing,
                                contentDescription = if (isSpeakerphoneOn) "السماعة الخارجية مفعّلة" else "سماعة الأذن",
                                tint = if (isSpeakerphoneOn) {
                                    if (isDark) BroadcastMintSoft else BroadcastRichEmerald
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(19.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSpeakerphoneOn) "السماعة" else "الأذن",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSpeakerphoneOn) {
                                    if (isDark) BroadcastMintSoft else BroadcastDeepEmerald
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 3) End Broadcast (Principal/Creator) OR Leave (Authorized Speaker)
                    Surface(
                        onClick = onRequestEndOrLeave,
                        shape = RoundedCornerShape(16.dp),
                        color = BroadcastCoralEnd,
                        modifier = Modifier
                            .weight(1.0f)
                            .height(50.dp)
                            .testTag(if (isPrincipalOrCreator) "arabic_dock_end_btn" else "arabic_dock_leave_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isPrincipalOrCreator) Icons.Filled.CallEnd else Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = if (isPrincipalOrCreator) "إنهاء البث" else "مغادرة",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isPrincipalOrCreator) "إنهاء البث" else "مغادرة",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                } else {
                    // LISTENER MODE: STRICTLY NO MICROPHONE BUTTON!
                    // 1) Raise Hand / Request to Speak Button
                    val canTapRaiseHand = !isMutedByAdmin
                    Surface(
                        onClick = { if (canTapRaiseHand) onToggleRaiseHand() },
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            isMutedByAdmin -> colorScheme.surfaceVariant.copy(alpha = 0.7f)
                            hasSelfRaisedHand -> BroadcastGoldAccent
                            else -> BroadcastRichEmerald
                        },
                        border = BorderStroke(
                            1.1.dp,
                            when {
                                isMutedByAdmin -> BroadcastCoralEnd.copy(alpha = 0.4f)
                                hasSelfRaisedHand -> BroadcastGoldLight
                                else -> BroadcastGoldLight.copy(alpha = 0.55f)
                            }
                        ),
                        modifier = Modifier
                            .weight(1.45f)
                            .height(50.dp)
                            .testTag("arabic_dock_raise_hand_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = if (isMutedByAdmin) Icons.Filled.MicOff else Icons.Filled.PanTool,
                                contentDescription = "طلب التحدث (رفع اليد)",
                                tint = when {
                                    isMutedByAdmin -> if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd
                                    hasSelfRaisedHand -> Color(0xFF1F1604)
                                    else -> Color.White
                                },
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when {
                                    isMutedByAdmin -> "مكتوم من الإدارة"
                                    hasSelfRaisedHand -> "إلغاء طلب التحدث"
                                    else -> "طلب التحدث (رفع اليد)"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    isMutedByAdmin -> if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd
                                    hasSelfRaisedHand -> Color(0xFF1F1604)
                                    else -> Color.White
                                },
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 2) Compact Loudspeaker Toggle for Listeners
                    Surface(
                        onClick = onToggleSpeakerphone,
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSpeakerphoneOn) {
                            if (isDark) Color(0xFF12362B) else Color(0xFFE6F7F0)
                        } else {
                            colorScheme.surfaceVariant.copy(alpha = 0.75f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (isSpeakerphoneOn) colorScheme.primary.copy(alpha = 0.45f)
                            else colorScheme.outline.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier
                            .size(50.dp)
                            .testTag("arabic_dock_listener_speaker_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isSpeakerphoneOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Filled.Hearing,
                                contentDescription = if (isSpeakerphoneOn) "السماعة الخارجية" else "سماعة الأذن",
                                tint = if (isSpeakerphoneOn) {
                                    if (isDark) BroadcastMintSoft else BroadcastRichEmerald
                                } else {
                                    colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }

                    // 3) Leave Broadcast Button ("مغادرة البث")
                    Surface(
                        onClick = onRequestEndOrLeave,
                        shape = RoundedCornerShape(16.dp),
                        color = BroadcastCoralEnd.copy(alpha = if (isDark) 0.90f else 0.94f),
                        modifier = Modifier
                            .weight(0.95f)
                            .height(50.dp)
                            .testTag("arabic_dock_listener_leave_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "مغادرة البث",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "مغادرة البث",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
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
