package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ActiveUserPresenceRecord
import com.example.model.PlatformLivePresenceSummary
import com.example.ui.theme.EmeraldAccent

/**
 * Platform-Unified Real-Time Activity Card for the Principal Dashboard:
 * Styled natively with MaterialTheme.colorScheme (Light & Dark adaptive) to blend seamlessly
 * with the rest of the school dashboard cards, with zero forced dark overlays.
 */
@Composable
fun RealTimePlatformPresenceCard(
    summary: PlatformLivePresenceSummary,
    onRefreshNow: () -> Unit,
    onOpenDetailsSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val primaryContainer = colorScheme.primaryContainer
    val onPrimaryContainer = colorScheme.onPrimaryContainer
    val tertiaryColor = colorScheme.tertiary
    val tertiaryContainer = colorScheme.tertiaryContainer
    val onTertiaryContainer = colorScheme.onTertiaryContainer
    val surfaceColor = colorScheme.surface
    val onSurface = colorScheme.onSurface
    val onSurfaceVariant = colorScheme.onSurfaceVariant

    var refreshSpinStep by remember { mutableIntStateOf(0) }
    val refreshAngle by animateFloatAsState(
        targetValue = refreshSpinStep * 360f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "presence_refresh_spin"
    )

    val cardInteraction = remember { MutableInteractionSource() }
    val isPressed by cardInteraction.collectIsPressedAsState()
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "presence_card_scale"
    )

    val liveTransition = rememberInfiniteTransition(label = "presence_emerald_pulse")
    val pulseRadiusFactor by liveTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "presence_pulse_ring"
    )
    val pulseAlpha by liveTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "presence_pulse_alpha"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = primaryColor.copy(alpha = 0.14f),
                spotColor = primaryColor.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable(
                interactionSource = cardInteraction,
                indication = null,
                onClick = onOpenDetailsSheet
            )
            .testTag("dashboard_realtime_presence_card"),
        shape = RoundedCornerShape(22.dp),
        color = surfaceColor,
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.45f),
                    tertiaryColor.copy(alpha = 0.35f),
                    primaryColor.copy(alpha = 0.25f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Luminous subtle wave/aura synced with platform colors
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.14f, h * 0.16f),
                        radius = w * 0.44f
                    ),
                    radius = w * 0.44f,
                    center = Offset(w * 0.14f, h * 0.16f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            tertiaryColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.88f, h * 0.86f),
                        radius = w * 0.38f
                    ),
                    radius = w * 0.38f,
                    center = Offset(w * 0.88f, h * 0.86f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier.size(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.size(24.dp)) {
                                val c = Offset(size.width / 2f, size.height / 2f)
                                drawCircle(
                                    color = EmeraldAccent.copy(alpha = pulseAlpha),
                                    radius = 6.dp.toPx() * pulseRadiusFactor,
                                    center = c
                                )
                                drawCircle(
                                    color = primaryColor,
                                    radius = 4.6.dp.toPx(),
                                    center = c
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(
                                text = "المتواجدون في المنصة",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.5.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "متابعة مباشرة للمتصلين داخل المدرسة",
                                style = MaterialTheme.typography.labelSmall,
                                color = onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Instant Manual Refresh Button (Custom Canvas Vector)
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                refreshSpinStep += 1
                                onRefreshNow()
                            }
                            .testTag("realtime_presence_refresh_button"),
                        shape = CircleShape,
                        color = primaryContainer.copy(alpha = 0.65f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PresenceRefreshVectorCanvas(
                                tint = onPrimaryContainer,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer {
                                        rotationZ = refreshAngle
                                    }
                            )
                        }
                    }
                }

                // Two Precision KPI Tiles (Native Theme Tints)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Metric 1: Active Now (Real-Time)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = primaryContainer.copy(alpha = 0.45f),
                        border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.30f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldAccent)
                                )
                                Text(
                                    text = "المتصلون الآن",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = summary.activeNowCount.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onSurface
                                )
                                Text(
                                    text = "متصل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }

                    // Metric 2: Active Today (Last 24h Unique)
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        color = tertiaryContainer.copy(alpha = 0.50f),
                        border = BorderStroke(1.dp, tertiaryColor.copy(alpha = 0.34f))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresenceUsersGroupMiniCanvas(
                                    tint = tertiaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "نشطو اليوم",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = onTertiaryContainer,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = summary.activeTodayCount.toString(),
                                    style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = onSurface
                                )
                                Text(
                                    text = "اليوم",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tertiaryColor,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }
                    }
                }

                // Bottom Action Hint Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "اضغط لعرض توزيع المتواجدين حسب التخصص والأسماء",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    PresenceChevronLeftCanvas(
                        tint = tertiaryColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PresenceUsersGroupMiniCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 1.65.dp.toPx()

        // Primary user (center-left)
        drawCircle(
            color = tint,
            radius = w * 0.18f,
            center = Offset(w * 0.38f, h * 0.30f),
            style = Stroke(width = strokeW)
        )
        val primaryShoulders = Path().apply {
            moveTo(w * 0.12f, h * 0.84f)
            quadraticBezierTo(w * 0.38f, h * 0.56f, w * 0.64f, h * 0.84f)
        }
        drawPath(
            path = primaryShoulders,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        // Secondary user (subtle companion on right)
        drawCircle(
            color = tint.copy(alpha = 0.85f),
            radius = w * 0.14f,
            center = Offset(w * 0.72f, h * 0.36f),
            style = Stroke(width = strokeW * 0.9f)
        )
        val secondaryShoulder = Path().apply {
            moveTo(w * 0.64f, h * 0.78f)
            quadraticBezierTo(w * 0.78f, h * 0.62f, w * 0.90f, h * 0.80f)
        }
        drawPath(
            path = secondaryShoulder,
            color = tint.copy(alpha = 0.85f),
            style = Stroke(width = strokeW * 0.9f, cap = StrokeCap.Round)
        )
    }
}

@Composable
private fun PresenceRefreshVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.dp.toPx()
        val inset = strokeW * 1.1f
        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, h - inset * 2f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        val arrowPath = Path().apply {
            moveTo(w * 0.72f, h * 0.14f)
            lineTo(w * 0.90f, h * 0.30f)
            lineTo(w * 0.66f, h * 0.36f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun PresenceCloseVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val pad = w * 0.24f
        val strokeW = 2.dp.toPx()
        drawLine(
            color = tint,
            start = Offset(pad, pad),
            end = Offset(w - pad, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w - pad, pad),
            end = Offset(pad, h - pad),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun PresenceChevronLeftCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.64f, h * 0.22f)
            lineTo(w * 0.34f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.78f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * Theme-Adaptive ModalBottomSheet displaying live active user breakdown by role category
 * and the real-time directory of online users with their last activity timing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RealTimePresenceBottomSheet(
    summary: PlatformLivePresenceSummary,
    onRefreshNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sheet Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(EmeraldAccent)
                    )
                    Column {
                        Text(
                            text = "المتواجدون الآن في المنصة",
                            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "المتصلون الآن: ${summary.activeNowCount} • نشطو اليوم: ${summary.activeTodayCount}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onRefreshNow)
                            .testTag("presence_sheet_refresh_button"),
                        shape = CircleShape,
                        color = colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PresenceRefreshVectorCanvas(
                                tint = colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onDismiss)
                            .testTag("presence_sheet_close_button"),
                        shape = CircleShape,
                        color = colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            PresenceCloseVectorCanvas(
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Role Category Distribution Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PresenceCategoryStatChip(
                    label = "معلمون",
                    count = summary.teachersOnlineCount,
                    accentColor = colorScheme.secondary,
                    containerColor = colorScheme.secondaryContainer.copy(alpha = 0.50f),
                    modifier = Modifier.weight(1f)
                )
                PresenceCategoryStatChip(
                    label = "طلبة وأولياء أمور",
                    count = summary.studentsAndParentsOnlineCount,
                    accentColor = colorScheme.primary,
                    containerColor = colorScheme.primaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                )
                PresenceCategoryStatChip(
                    label = "إدارة ونقل",
                    count = summary.adminAndTransportOnlineCount,
                    accentColor = colorScheme.tertiary,
                    containerColor = colorScheme.tertiaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.weight(1f)
                )
            }

            // Active Users Directory List
            if (summary.activeNowUsers.isEmpty()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "لا يوجد مستخدمون آخرون نشطون في هذه اللحظة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "تتحدث القائمة تلقائياً كل 20 ثانية فور دخول أي مستخدم للمنصة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(summary.activeNowUsers, key = { it.userId }) { userRec ->
                        PresenceUserRowItem(record = userRec)
                    }
                }
            }
        }
    }
}

@Composable
private fun PresenceCategoryStatChip(
    label: String,
    count: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = containerColor,
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.32f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                fontWeight = FontWeight.ExtraBold,
                color = accentColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun PresenceUserRowItem(
    record: ActiveUserPresenceRecord
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = when (record.categoryBucket()) {
        "TEACHERS" -> colorScheme.secondary
        "STUDENTS_PARENTS" -> colorScheme.primary
        else -> colorScheme.tertiary
    }
    val lastSeenLabel = remember(record.lastActiveTimestamp) {
        formatArabicPresenceRelativeTime(record.lastActiveTimestamp)
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceVariant.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Initials Circle with Live Emerald Status Dot
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(accentColor.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    val initialChar = record.name.trim().firstOrNull()?.toString() ?: "م"
                    Text(
                        text = initialChar,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = accentColor
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(11.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surface)
                        .padding(2.dp)
                        .clip(CircleShape)
                        .background(EmeraldAccent)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = record.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = accentColor.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = record.role.ifBlank { "مستخدم" },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Text(
                text = lastSeenLabel,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

private fun formatArabicPresenceRelativeTime(timestampMs: Long): String {
    if (timestampMs <= 0L) return "متصل الآن"
    val diffSec = ((System.currentTimeMillis() - timestampMs) / 1000L).coerceAtLeast(0L)
    return when {
        diffSec < 15L -> "متصل الآن"
        diffSec < 60L -> "منذ $diffSec ثانية"
        diffSec < 120L -> "منذ دقيقة"
        else -> "منذ ${diffSec / 60L} دقائق"
    }
}
