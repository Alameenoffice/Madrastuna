package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent

/**
 * Platform-Unified Offline / No Internet Screen:
 * Fully synchronized with MaterialTheme.colorScheme (Light & Dark modes) using the school platform's
 * Royal Emerald & Gold visual identity — zero forced dark overlays, custom Canvas radar & icons.
 */
@Composable
fun ExecutiveOfflineScreen(
    isCheckingNow: Boolean,
    isUserLoggedIn: Boolean,
    onRetryConnection: () -> Unit,
    onOpenOfflineReadingOasis: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val primaryColor = colorScheme.primary
    val primaryContainerColor = colorScheme.primaryContainer
    val onPrimaryContainerColor = colorScheme.onPrimaryContainer
    val tertiaryColor = colorScheme.tertiary
    val backgroundColor = colorScheme.background
    val surfaceColor = colorScheme.surface
    val onSurfaceColor = colorScheme.onSurface
    val onSurfaceVariantColor = colorScheme.onSurfaceVariant
    val outlineColor = colorScheme.outline

    val haptic = LocalHapticFeedback.current
    var retrySpinCount by remember { mutableIntStateOf(0) }
    val retryRotation by animateFloatAsState(
        targetValue = retrySpinCount * 360f,
        animationSpec = tween(durationMillis = 720, easing = FastOutSlowInEasing),
        label = "offline_exec_retry_spin"
    )

    val retryInteraction = remember { MutableInteractionSource() }
    val isRetryPressed by retryInteraction.collectIsPressedAsState()
    val retryScale by animateFloatAsState(
        targetValue = if (isRetryPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offline_retry_spring"
    )

    val oasisInteraction = remember { MutableInteractionSource() }
    val isOasisPressed by oasisInteraction.collectIsPressedAsState()
    val oasisScale by animateFloatAsState(
        targetValue = if (isOasisPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offline_oasis_spring"
    )

    val statusTransition = rememberInfiniteTransition(label = "offline_probe_status")
    val probeDotAlpha by statusTransition.animateFloat(
        initialValue = 0.38f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offline_probe_dot"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor)
            .pointerInput(Unit) {
                detectTapGestures { /* Intercept touches cleanly while offline */ }
            }
            .testTag("network_connectivity_guard_banner")
    ) {
        // Luminous subtle geometric grid + soft primary/gold atmospheric glow synced with active theme
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val step = 38.dp.toPx()
            val gridColor = primaryColor.copy(alpha = 0.055f)

            var x = 0f
            while (x <= w) {
                drawLine(
                    color = gridColor,
                    start = Offset(x, 0f),
                    end = Offset(x, h),
                    strokeWidth = 1f
                )
                x += step
            }
            var y = 0f
            while (y <= h) {
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(w, y),
                    strokeWidth = 1f
                )
                y += step
            }

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.12f),
                        tertiaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.30f),
                    radius = w * 0.72f
                ),
                radius = w * 0.72f,
                center = Offset(w * 0.5f, h * 0.30f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.09f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.86f),
                    radius = w * 0.52f
                ),
                radius = w * 0.52f,
                center = Offset(w * 0.85f, h * 0.86f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Interactive Theme-Synced Radar & Signal Pulse Graphic
                OfflineRadarSignalCanvas(
                    isCheckingNow = isCheckingNow,
                    primaryColor = primaryColor,
                    accentGoldColor = tertiaryColor,
                    coreSurfaceColor = surfaceColor,
                    modifier = Modifier.size(214.dp)
                )

                // Live Automatic Probe Status Pill
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isCheckingNow) {
                        colorScheme.tertiaryContainer.copy(alpha = 0.78f)
                    } else {
                        primaryContainerColor.copy(alpha = 0.72f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (isCheckingNow) tertiaryColor.copy(alpha = 0.45f) else primaryColor.copy(alpha = 0.35f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(
                                    (if (isCheckingNow) tertiaryColor else primaryColor).copy(alpha = probeDotAlpha)
                                )
                        )
                        Text(
                            text = if (isCheckingNow) {
                                "جارٍ فحص الاتصال بالشبكة الآن..."
                            } else {
                                "الفحص التلقائي للشبكة قيد التشغيل"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (isCheckingNow) colorScheme.onTertiaryContainer else onPrimaryContainerColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Clean Platform-Surface Info Card
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = primaryColor.copy(alpha = 0.14f),
                            spotColor = primaryColor.copy(alpha = 0.12f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = surfaceColor,
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.36f),
                                tertiaryColor.copy(alpha = 0.28f),
                                outlineColor.copy(alpha = 0.35f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "تعذّر الاتصال بالشبكة حالياً",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 21.sp,
                                lineHeight = 30.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = onSurfaceColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "يجري فحص الاتصال تلقائياً؛ ستعود جميع خدمات المنصة فور استقرار الإنترنت.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 24.sp
                            ),
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariantColor,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Primary Emerald Gradient Retry Button with Spring Bounce & Custom Refresh Vector
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .graphicsLayer {
                            scaleX = retryScale
                            scaleY = retryScale
                        }
                        .shadow(
                            elevation = 10.dp,
                            shape = RoundedCornerShape(18.dp),
                            ambientColor = primaryColor.copy(alpha = 0.28f),
                            spotColor = primaryColor.copy(alpha = 0.22f)
                        )
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(
                            interactionSource = retryInteraction,
                            indication = null
                        ) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            retrySpinCount += 1
                            onRetryConnection()
                        }
                        .testTag("network_retry_sync_button"),
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(
                        width = 1.2.dp,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                EmeraldAccent.copy(alpha = 0.85f),
                                GoldAccent.copy(alpha = 0.72f),
                                EmeraldAccent.copy(alpha = 0.85f)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        primaryColor,
                                        DeepEmerald,
                                        primaryColor
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OfflineRefreshVectorCanvas(
                                tint = Color.White,
                                modifier = Modifier
                                    .size(21.dp)
                                    .graphicsLayer {
                                        rotationZ = retryRotation
                                    }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "إعادة المحاولة الآن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Smart Touch: Direct Access to Offline Reading Oasis for Signed-In Users
                if (isUserLoggedIn) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .graphicsLayer {
                                scaleX = oasisScale
                                scaleY = oasisScale
                            }
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(16.dp),
                                ambientColor = tertiaryColor.copy(alpha = 0.14f)
                            )
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(
                                interactionSource = oasisInteraction,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenOfflineReadingOasis()
                            }
                            .testTag("offline_open_reading_oasis_button"),
                        shape = RoundedCornerShape(16.dp),
                        color = surfaceColor,
                        border = BorderStroke(
                            width = 1.3.dp,
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.55f),
                                    tertiaryColor.copy(alpha = 0.65f)
                                )
                            )
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OfflineOasisBookBadgeCanvas(
                                primaryColor = primaryColor,
                                goldColor = tertiaryColor,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "الدخول إلى واحة القراءة (تعمل بدون إنترنت)",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = onSurfaceColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OfflineRefreshVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.2.dp.toPx()
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
private fun OfflineRadarSignalCanvas(
    isCheckingNow: Boolean,
    primaryColor: Color,
    accentGoldColor: Color,
    coreSurfaceColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "offline_radar_signal")

    val sweepAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isCheckingNow) 1400 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_sweep_angle"
    )

    val waveProgress1 by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave_1"
    )

    val waveProgress2 by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar_wave_2"
    )

    val corePulse by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "radar_core_pulse"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension * 0.47f

        // 1. Soft translucent circular backing halo
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.14f),
                    primaryColor.copy(alpha = 0.04f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = maxR
            ),
            radius = maxR,
            center = Offset(cx, cy)
        )

        // 2. Crisp calibration rings
        listOf(0.96f, 0.72f, 0.48f).forEachIndexed { idx, factor ->
            drawCircle(
                color = primaryColor.copy(alpha = if (idx == 0) 0.24f else 0.16f),
                radius = maxR * factor,
                center = Offset(cx, cy),
                style = Stroke(width = if (idx == 0) 1.5.dp.toPx() else 1.dp.toPx())
            )
        }

        // Axis guides
        drawLine(
            color = primaryColor.copy(alpha = 0.12f),
            start = Offset(cx - maxR, cy),
            end = Offset(cx + maxR, cy),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = primaryColor.copy(alpha = 0.12f),
            start = Offset(cx, cy - maxR),
            end = Offset(cx, cy + maxR),
            strokeWidth = 1.dp.toPx()
        )

        // 3. Expanding radar pulse waves
        val p1 = waveProgress1.coerceIn(0f, 1f)
        val p2 = (waveProgress2 % 1f).coerceIn(0f, 1f)
        listOf(p1, p2).forEach { p ->
            val waveR = maxR * (0.24f + p * 0.74f)
            val alpha = (1f - p) * 0.58f
            drawCircle(
                color = primaryColor.copy(alpha = alpha),
                radius = waveR,
                center = Offset(cx, cy),
                style = Stroke(width = (2.3f * (1f - p * 0.5f)).dp.toPx())
            )
        }

        // 4. Rotating radar sweep cone & needle
        rotate(degrees = sweepAngle, pivot = Offset(cx, cy)) {
            drawArc(
                brush = Brush.sweepGradient(
                    0.0f to Color.Transparent,
                    0.72f to Color.Transparent,
                    0.92f to primaryColor.copy(alpha = 0.20f),
                    1.0f to primaryColor.copy(alpha = 0.52f),
                    center = Offset(cx, cy)
                ),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = true,
                topLeft = Offset(cx - maxR * 0.94f, cy - maxR * 0.94f),
                size = Size(maxR * 1.88f, maxR * 1.88f)
            )
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.35f), accentGoldColor.copy(alpha = 0.95f)),
                    start = Offset(cx, cy),
                    end = Offset(cx + maxR * 0.94f, cy)
                ),
                start = Offset(cx, cy),
                end = Offset(cx + maxR * 0.94f, cy),
                strokeWidth = 2.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // 5. Curved signal search arcs
        val arcStroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
        listOf(0.36f to 0.88f, 0.52f to 0.56f).forEach { (factor, baseAlpha) ->
            val r = maxR * factor
            val pulsedAlpha = (baseAlpha * (0.65f + 0.35f * (corePulse - 0.88f) / 0.24f)).coerceIn(0.24f, 1f)
            drawArc(
                color = primaryColor.copy(alpha = pulsedAlpha),
                startAngle = -132f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = arcStroke
            )
            drawArc(
                color = accentGoldColor.copy(alpha = pulsedAlpha * 0.85f),
                startAngle = 48f,
                sweepAngle = 84f,
                useCenter = false,
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2f, r * 2f),
                style = arcStroke
            )
        }

        // 6. Luminous center beacon in Platform Surface + Emerald/Gold ring
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.30f),
                    accentGoldColor.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = maxR * 0.34f * corePulse
            ),
            radius = maxR * 0.34f * corePulse,
            center = Offset(cx, cy)
        )

        drawCircle(
            color = coreSurfaceColor,
            radius = maxR * 0.21f,
            center = Offset(cx, cy)
        )

        drawCircle(
            brush = Brush.sweepGradient(
                colors = listOf(primaryColor, accentGoldColor, primaryColor),
                center = Offset(cx, cy)
            ),
            radius = maxR * 0.21f,
            center = Offset(cx, cy),
            style = Stroke(width = 2.5.dp.toPx())
        )

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color.White, accentGoldColor, primaryColor),
                center = Offset(cx, cy),
                radius = maxR * 0.09f * corePulse
            ),
            radius = maxR * 0.09f * corePulse,
            center = Offset(cx, cy)
        )
    }
}

@Composable
private fun OfflineOasisBookBadgeCanvas(
    primaryColor: Color,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        val leftPage = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.36f, h * 0.20f, w * 0.20f, h * 0.22f, w * 0.12f, h * 0.30f)
            lineTo(w * 0.12f, h * 0.78f)
            cubicTo(w * 0.22f, h * 0.72f, w * 0.36f, h * 0.72f, w * 0.50f, h * 0.80f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.50f, h * 0.28f)
            cubicTo(w * 0.64f, h * 0.20f, w * 0.80f, h * 0.22f, w * 0.88f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.78f)
            cubicTo(w * 0.78f, h * 0.72f, w * 0.64f, h * 0.72f, w * 0.50f, h * 0.80f)
            close()
        }

        drawPath(path = leftPage, color = primaryColor.copy(alpha = 0.16f))
        drawPath(path = rightPage, color = goldColor.copy(alpha = 0.18f))
        drawPath(path = leftPage, color = primaryColor, style = stroke)
        drawPath(path = rightPage, color = goldColor, style = stroke)

        drawRoundRect(
            color = primaryColor,
            topLeft = Offset(w * 0.45f, h * 0.08f),
            size = Size(w * 0.10f, h * 0.10f),
            cornerRadius = CornerRadius(4f, 4f)
        )
    }
}
