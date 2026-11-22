package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reading.ReadingMascotCanvas
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import kotlinx.coroutines.delay

/**
 * شاشة انقطاع الإنترنت بهوية المنصة الزمردية الزجاجية:
 * البصمة الخاصة: موجات اتصال زمردية هادئة (3 أقواس إشارة متدرجة) + كبسولة حالة حية
 * + بطاقة واحة القراءة الذكية الحية التي تعرض أنيميشن الواحة الحي بـ Canvas وتفتحها بدون إنترنت.
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
        targetValue = if (isRetryPressed) 0.94f else 1f,
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

    val statusPhrases = remember {
        listOf(
            "جارٍ فحص الاتصال تلقائياً",
            "وضع العمل بدون إنترنت جاهز"
        )
    }
    var statusPhraseIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(isCheckingNow) {
        if (isCheckingNow) {
            statusPhraseIdx = 0
        }
        while (true) {
            delay(2800L)
            statusPhraseIdx = if (isCheckingNow) 0 else (statusPhraseIdx + 1) % statusPhrases.size
        }
    }

    val statusDotTransition = rememberInfiniteTransition(label = "offline_probe_dot")
    val probeDotAlpha by statusDotTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offline_probe_dot_alpha"
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
        // Subtle luminous atmospheric background synced with active platform theme
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.13f),
                        tertiaryColor.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.24f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.5f, h * 0.24f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.85f, h * 0.86f),
                    radius = w * 0.56f
                ),
                radius = w * 0.56f,
                center = Offset(w * 0.85f, h * 0.86f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1) Top Frosted Glass Signal Wave Ripple Unit (3 smooth graduated emerald signal arcs)
                Surface(
                    modifier = Modifier
                        .size(164.dp)
                        .shadow(
                            elevation = 14.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = primaryColor.copy(alpha = 0.22f),
                            spotColor = primaryColor.copy(alpha = 0.18f)
                        ),
                    shape = RoundedCornerShape(36.dp),
                    color = surfaceColor.copy(alpha = 0.94f),
                    border = BorderStroke(
                        width = 1.4.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.45f),
                                GoldAccent.copy(alpha = 0.48f),
                                primaryColor.copy(alpha = 0.25f)
                            )
                        )
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        primaryContainerColor.copy(alpha = 0.45f),
                                        surfaceColor.copy(alpha = 0.95f)
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        OfflineCalmSignalWavesCanvas(
                            isCheckingNow = isCheckingNow,
                            primaryColor = primaryColor,
                            goldColor = GoldAccent,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp)
                        )
                    }
                }

                // 2) Live Animated Status Capsule ("جارٍ فحص الاتصال تلقائياً" <-> "وضع العمل بدون إنترنت جاهز")
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (statusPhraseIdx == 0) {
                        primaryContainerColor.copy(alpha = 0.80f)
                    } else {
                        colorScheme.tertiaryContainer.copy(alpha = 0.78f)
                    },
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (statusPhraseIdx == 0) {
                            primaryColor.copy(alpha = 0.38f)
                        } else {
                            tertiaryColor.copy(alpha = 0.46f)
                        }
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
                                    (if (statusPhraseIdx == 0) primaryColor else tertiaryColor)
                                        .copy(alpha = probeDotAlpha)
                                )
                        )
                        AnimatedContent(
                            targetState = statusPhraseIdx,
                            transitionSpec = {
                                (slideInVertically(tween(380, easing = FastOutSlowInEasing)) { it / 2 } + fadeIn(tween(320))) togetherWith
                                    (slideOutVertically(tween(300)) { -it / 2 } + fadeOut(tween(240)))
                            },
                            label = "offline_status_pill_text"
                        ) { idx ->
                            Text(
                                text = statusPhrases[idx],
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (idx == 0) onPrimaryContainerColor else colorScheme.onTertiaryContainer,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // 3) Clear Title & Concise Message + Spring Retry Button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = primaryColor.copy(alpha = 0.14f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    color = surfaceColor,
                    border = BorderStroke(
                        width = 1.1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.30f),
                                GoldAccent.copy(alpha = 0.28f),
                                outlineColor.copy(alpha = 0.30f)
                            )
                        )
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "انقطع الاتصال بالإنترنت",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontSize = 22.sp,
                                lineHeight = 30.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = onSurfaceColor,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "ستعود خدمات المنصة تلقائياً فور استقرار الشبكة",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.5.sp,
                                lineHeight = 23.sp
                            ),
                            fontWeight = FontWeight.Medium,
                            color = onSurfaceVariantColor,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Primary Emerald Gradient Retry Button with Spring Bounce
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .graphicsLayer {
                                    scaleX = retryScale
                                    scaleY = retryScale
                                }
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(16.dp),
                                    ambientColor = primaryColor.copy(alpha = 0.28f)
                                )
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(
                                    interactionSource = retryInteraction,
                                    indication = null
                                ) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    retrySpinCount += 1
                                    onRetryConnection()
                                }
                                .testTag("network_retry_sync_button"),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            border = BorderStroke(
                                width = 1.2.dp,
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        EmeraldAccent.copy(alpha = 0.80f),
                                        GoldAccent.copy(alpha = 0.72f),
                                        EmeraldAccent.copy(alpha = 0.80f)
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
                                                Color(0xFF059669),
                                                DeepEmerald,
                                                Color(0xFF065F46)
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
                                            .size(20.dp)
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
                    }
                }

                // 4) Creative Bottom Interactive Card: Live Reading Oasis Card with Real Reading Oasis Canvas Animation
                if (isUserLoggedIn) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = oasisScale
                                scaleY = oasisScale
                            }
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(24.dp),
                                ambientColor = Color(0xFF047857).copy(alpha = 0.30f),
                                spotColor = Color(0xFF064E3B).copy(alpha = 0.35f)
                            )
                            .clip(RoundedCornerShape(24.dp))
                            .clickable(
                                interactionSource = oasisInteraction,
                                indication = null
                            ) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onOpenOfflineReadingOasis()
                            }
                            .testTag("offline_open_reading_oasis_button"),
                        shape = RoundedCornerShape(24.dp),
                        color = Color.Transparent,
                        border = BorderStroke(
                            width = 1.4.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF6EE7B7).copy(alpha = 0.78f),
                                    GoldAccent.copy(alpha = 0.85f),
                                    Color(0xFF34D399).copy(alpha = 0.70f)
                                )
                            )
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF059669),
                                            Color(0xFF047857),
                                            Color(0xFF064E3B)
                                        )
                                    )
                                )
                        ) {
                            // Subtle emerald & gold internal light wave
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val w = size.width
                                val h = size.height
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFFFEF08A).copy(alpha = 0.22f),
                                            Color.Transparent
                                        ),
                                        center = Offset(w * 0.86f, h * 0.45f),
                                        radius = w * 0.42f
                                    ),
                                    radius = w * 0.42f,
                                    center = Offset(w * 0.86f, h * 0.45f)
                                )
                                val wavePath = Path().apply {
                                    moveTo(0f, h * 0.74f)
                                    cubicTo(w * 0.32f, h * 0.56f, w * 0.68f, h * 0.92f, w, h * 0.68f)
                                    lineTo(w, h)
                                    lineTo(0f, h)
                                    close()
                                }
                                drawPath(
                                    path = wavePath,
                                    color = Color(0xFF34D399).copy(alpha = 0.14f)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Live Reading Oasis Canvas Animation (Exact mascot & floating book animation from Reading Oasis)
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.55f)),
                                    modifier = Modifier.size(66.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        ReadingMascotCanvas(
                                            modifier = Modifier.fillMaxSize(),
                                            isCelebrating = true
                                        )
                                    }
                                }

                                // Completely Unclipped Title & Subtitle
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "واحة القراءة الذكية",
                                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "تعلّم وتدرّب الآن • تعمل بدون إنترنت",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontSize = 12.5.sp,
                                            lineHeight = 18.sp
                                        ),
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFEF9C3)
                                    )
                                }

                                // Sleek Frosted Entry Arrow
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.18f),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A).copy(alpha = 0.65f)),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                            contentDescription = "دخول واحة القراءة الذكية",
                                            tint = Color(0xFFFEF08A),
                                            modifier = Modifier.size(20.dp)
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
}

/**
 * 3 أقواس إشارة/موجات زمردية متدرجة تنبض بانسيابية هادئة (Calm Wave Ripple via rememberInfiniteTransition).
 */
@Composable
private fun OfflineCalmSignalWavesCanvas(
    isCheckingNow: Boolean,
    primaryColor: Color,
    goldColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "offline_calm_signal_waves")
    val wavePhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isCheckingNow) 1500 else 2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "signal_wave_phase"
    )
    val coreBreath by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "signal_core_breath"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val originY = h * 0.78f
        val maxR = size.minDimension * 0.70f

        // Soft glowing aura behind arcs
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    primaryColor.copy(alpha = 0.18f * coreBreath),
                    goldColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                center = Offset(cx, originY - maxR * 0.25f),
                radius = maxR * 0.95f
            ),
            radius = maxR * 0.95f,
            center = Offset(cx, originY - maxR * 0.25f)
        )

        // 3 graduated emerald signal arcs pulsing sequentially from inner -> middle -> outer
        val arcRadii = listOf(maxR * 0.34f, maxR * 0.58f, maxR * 0.82f)
        arcRadii.forEachIndexed { idx, r ->
            val phaseWindow = ((wavePhase * 3f) - idx).let { delta ->
                val wrapped = ((delta % 3f) + 3f) % 3f
                when {
                    wrapped < 1f -> kotlin.math.sin(wrapped * Math.PI).toFloat()
                    else -> 0f
                }
            }
            val arcAlpha = (0.28f + 0.68f * phaseWindow).coerceIn(0.24f, 0.96f)
            val arcWidth = (3.4.dp.toPx() + 1.4.dp.toPx() * phaseWindow)
            val arcBrush = Brush.horizontalGradient(
                colors = listOf(
                    primaryColor.copy(alpha = arcAlpha * 0.85f),
                    if (idx == 2) goldColor.copy(alpha = arcAlpha) else EmeraldAccent.copy(alpha = arcAlpha),
                    primaryColor.copy(alpha = arcAlpha * 0.85f)
                )
            )
            drawArc(
                brush = arcBrush,
                startAngle = -140f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - r, originY - r),
                size = Size(r * 2f, r * 2f),
                style = Stroke(width = arcWidth, cap = StrokeCap.Round)
            )
        }

        // Luminous emerald & gold signal anchor node at base
        drawCircle(
            color = primaryColor.copy(alpha = 0.24f),
            radius = 12.dp.toPx() * coreBreath,
            center = Offset(cx, originY)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFEF08A), primaryColor, DeepEmerald),
                center = Offset(cx, originY),
                radius = 7.dp.toPx()
            ),
            radius = 7.dp.toPx(),
            center = Offset(cx, originY)
        )
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
