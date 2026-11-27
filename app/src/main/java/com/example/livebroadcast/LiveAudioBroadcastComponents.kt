package com.example.livebroadcast

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

internal val BroadcastGoldAccent = Color(0xFFD9A441)
internal val BroadcastGoldLight = Color(0xFFF3D58D)
internal val BroadcastMintSoft = Color(0xFFA7F3D0)
internal val BroadcastDeepEmerald = Color(0xFF044E3B)
internal val BroadcastRichEmerald = Color(0xFF066A4E)
internal val BroadcastCoralEnd = Color(0xFFDC524D)

/**
 * Tactile Spring-bounce button adhering to the Madrasati visual language.
 */
@Composable
fun SpringBroadcastButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    height: Dp = 48.dp,
    cornerRadius: Dp = 15.dp,
    enabled: Boolean = true,
    testTagName: String = "spring_broadcast_btn"
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "spring_broadcast_btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(cornerRadius),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.45f),
            disabledContentColor = contentColor.copy(alpha = 0.7f)
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        modifier = modifier
            .height(height)
            .scale(scale)
            .testTag(testTagName)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            softWrap = false
        )
    }
}

/**
 * Pulsing geometric broadcast dot drawn with Compose Canvas (zero emojis).
 */
@Composable
fun PulsingLiveDotCanvas(
    modifier: Modifier = Modifier,
    dotColor: Color = Color(0xFFEF4444),
    ringColor: Color = Color(0xFFFCA5A5),
    size: Dp = 14.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "live_dot_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_dot_ring_progress"
    )

    Canvas(modifier = modifier.size(size)) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val maxRadius = this.size.minDimension / 2f
        val animatedRadius = maxRadius * (0.42f + 0.56f * pulseProgress)
        val ringAlpha = (1f - pulseProgress).coerceIn(0f, 1f) * 0.72f

        drawCircle(
            color = ringColor.copy(alpha = ringAlpha),
            radius = animatedRadius,
            center = center
        )
        drawCircle(
            color = dotColor,
            radius = maxRadius * 0.42f,
            center = center
        )
    }
}

/**
 * Signature Kinetic Header: "وحدة الاستوديو الصوتي الزجاجي" (Glass Acoustic Spectrum Unit).
 * - Emerald gradient header with 22.dp rounded corners and translucent gold/white border.
 * - Left side (RTL end): Glass studio unit drawn with Compose Canvas showing 5 rounded-cap
 *   Acoustic Equalizer Bars in emerald & gold oscillating smoothly inside a gently pulsing
 *   circular broadcast ring via rememberInfiniteTransition.
 * - Subtitle strip cycling via Crossfade across the 3 exact Arabic phrases.
 */
@Composable
fun GlassAcousticSpectrumHeader(
    schoolName: String,
    activeLiveCount: Int,
    modifier: Modifier = Modifier
) {
    val phrases = remember {
        listOf(
            "بثٌ صوتي نقي ومباشر",
            "تواصلٌ منظّم للكوادر والأهالي",
            "إدارةٌ ذكية للمتحدثين"
        )
    }
    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3400L)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "acoustic_studio_spectrum")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "acoustic_wave_phase"
    )
    val ringBreath by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "acoustic_ring_breath"
    )

    val headerShape = RoundedCornerShape(22.dp)
    val emeraldGradient = Brush.linearGradient(
        colors = listOf(
            BroadcastDeepEmerald,
            BroadcastRichEmerald,
            colorScheme.primary
        )
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_audio_glass_studio_header"),
        shape = headerShape,
        color = Color.Transparent,
        shadowElevation = 6.dp,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    BroadcastGoldLight.copy(alpha = 0.65f),
                    Color.White.copy(alpha = 0.32f),
                    BroadcastGoldAccent.copy(alpha = 0.50f)
                )
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(emeraldGradient)
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Right side (RTL start): Title + School + Crossfade animated strip
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (activeLiveCount > 0) {
                                Color(0xFFEF4444).copy(alpha = 0.24f)
                            } else {
                                Color.White.copy(alpha = 0.16f)
                            },
                            border = BorderStroke(
                                0.8.dp,
                                if (activeLiveCount > 0) Color(0xFFFCA5A5).copy(alpha = 0.7f)
                                else BroadcastGoldLight.copy(alpha = 0.45f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (activeLiveCount > 0) {
                                    PulsingLiveDotCanvas(size = 10.dp)
                                    Text(
                                        text = "بث مباشر الآن",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                } else {
                                    Text(
                                        text = "استوديو المدرسة",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = BroadcastGoldLight,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "مركز البث الصوتي المباشر",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )

                    if (schoolName.isNotBlank()) {
                        Text(
                            text = schoolName,
                            style = MaterialTheme.typography.labelMedium,
                            color = BroadcastMintSoft.copy(alpha = 0.92f),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Smooth Crossfade kinetic text strip
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.11f),
                        border = BorderStroke(0.75.dp, Color.White.copy(alpha = 0.20f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Crossfade(
                                targetState = phrases[phraseIndex],
                                animationSpec = tween(durationMillis = 520),
                                label = "acoustic_phrase_crossfade"
                            ) { currentPhrase ->
                                Text(
                                    text = currentPhrase,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                // Left side (RTL end): Glass Acoustic Spectrum Unit drawn with Compose Canvas
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.20f),
                                    Color.White.copy(alpha = 0.06f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    BroadcastGoldLight.copy(alpha = 0.72f),
                                    Color.White.copy(alpha = 0.22f)
                                )
                            ),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val outerRadius = (size.minDimension / 2f) * ringBreath

                        // Outer softly pulsing broadcast ring
                        drawCircle(
                            color = BroadcastGoldLight.copy(alpha = 0.24f),
                            radius = outerRadius,
                            center = center,
                            style = Stroke(width = 1.6.dp.toPx())
                        )
                        // Inner halo circle
                        drawCircle(
                            color = BroadcastMintSoft.copy(alpha = 0.16f),
                            radius = outerRadius * 0.78f,
                            center = center
                        )

                        // 5 geometric rounded-cap Acoustic Equalizer Bars alternating emerald & gold
                        val barCount = 5
                        val barWidth = 4.2.dp.toPx()
                        val gap = 4.5.dp.toPx()
                        val totalWidth = barCount * barWidth + (barCount - 1) * gap
                        val startX = center.x - totalWidth / 2f + barWidth / 2f
                        val maxBarHalfHeight = size.height * 0.27f

                        val baseProfiles = floatArrayOf(0.46f, 0.78f, 1.0f, 0.74f, 0.44f)
                        val phaseOffsets = floatArrayOf(0.0f, 1.15f, 2.25f, 3.4f, 4.55f)
                        val barColors = listOf(
                            BroadcastMintSoft,
                            BroadcastGoldLight,
                            Color.White,
                            BroadcastGoldAccent,
                            BroadcastMintSoft
                        )

                        for (i in 0 until barCount) {
                            val osc = 0.5f + 0.5f * sin((wavePhase + phaseOffsets[i]).toDouble()).toFloat()
                            val dynamicFraction = (0.34f + 0.66f * osc) * baseProfiles[i]
                            val halfH = (maxBarHalfHeight * dynamicFraction).coerceAtLeast(3.5.dp.toPx())
                            val x = startX + i * (barWidth + gap)
                            drawLine(
                                color = barColors[i],
                                start = Offset(x, center.y - halfH),
                                end = Offset(x, center.y + halfH),
                                strokeWidth = barWidth,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Calm Empty State Card for the Live Broadcast Hub when no active room targets this user.
 * Drawn with Compose Canvas sound waves (zero cartoon drawings or emojis).
 */
@Composable
fun CalmAcousticEmptyStateCard(
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val infiniteTransition = rememberInfiniteTransition(label = "empty_wave_calm")
    val calmBreath by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "empty_wave_breath"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_audio_empty_state_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.42f) else colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            colorScheme.primary.copy(alpha = if (isDark) 0.30f else 0.16f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(colorScheme.primary.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val maxR = (size.minDimension / 2f) * calmBreath

                    drawCircle(
                        color = colorScheme.primary.copy(alpha = 0.18f),
                        radius = maxR,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawCircle(
                        color = BroadcastGoldAccent.copy(alpha = 0.28f),
                        radius = maxR * 0.68f,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // Calm horizontal acoustic wave bars
                    val barW = 3.2.dp.toPx()
                    val gap = 4.2.dp.toPx()
                    val heights = floatArrayOf(0.22f, 0.46f, 0.68f, 0.46f, 0.22f)
                    val totalW = heights.size * barW + (heights.size - 1) * gap
                    val startX = center.x - totalW / 2f + barW / 2f
                    heights.forEachIndexed { idx, frac ->
                        val halfH = (size.height * 0.26f) * frac
                        val x = startX + idx * (barW + gap)
                        drawLine(
                            color = if (idx == 2) BroadcastGoldAccent else colorScheme.primary,
                            start = Offset(x, center.y - halfH),
                            end = Offset(x, center.y + halfH),
                            strokeWidth = barW,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Text(
                text = "لا يوجد بث صوتي مباشر حالياً • ستظهر الجلسات هنا فور إطلاقها من الإدارة",
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * Refined Live Broadcast Banner Card for the Home Dashboard.
 * Shown ONLY to users who match the active live room's targetAudience while status == "live".
 * - 18.dp rounded glass card with thin emerald-gold vertical edge accent on the right (RTL).
 * - Pulsing Canvas broadcast dot + "بث صوتي مباشر الآن" + audience badge.
 * - Broadcast title + host name.
 * - Compact left button "انضمام الآن" with directional arrow opening the live room immediately.
 */
@Composable
fun HomeLiveAudioBroadcastCard(
    room: LiveAudioRoom,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) {
        colorScheme.surfaceVariant.copy(alpha = 0.78f)
    } else {
        Color(0xFFF3FBF7)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onJoinClick)
            .testTag("home_live_audio_broadcast_card"),
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        shadowElevation = 4.dp,
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    BroadcastGoldAccent.copy(alpha = 0.55f),
                    colorScheme.primary.copy(alpha = 0.45f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Sleek emerald-gold right vertical stripe (start in RTL)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(5.dp)
                    .matchParentSize()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Right-side accent bar (first child in RTL layout)
                Canvas(
                    modifier = Modifier
                        .width(5.dp)
                        .height(86.dp)
                ) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                BroadcastGoldAccent,
                                colorScheme.primary,
                                BroadcastDeepEmerald
                            )
                        ),
                        topLeft = Offset.Zero,
                        size = Size(size.width, size.height),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            PulsingLiveDotCanvas(size = 12.dp)
                            Text(
                                text = "بث صوتي مباشر الآن",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isDark) BroadcastMintSoft else BroadcastDeepEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = BroadcastGoldAccent.copy(alpha = if (isDark) 0.22f else 0.16f),
                                border = BorderStroke(0.75.dp, BroadcastGoldAccent.copy(alpha = 0.55f))
                            ) {
                                Text(
                                    text = room.audienceDisplayLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) BroadcastGoldLight else Color(0xFF7A530D),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = room.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold
                        )

                        Text(
                            text = "المشرف: ${room.hostName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Compact left-side Join CTA with clean directional arrow
                    Surface(
                        onClick = onJoinClick,
                        shape = RoundedCornerShape(13.dp),
                        color = colorScheme.primary,
                        contentColor = colorScheme.onPrimary,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("home_live_audio_join_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "انضمام الآن",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "انضمام الآن",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean ZEGOCLOUD Platform Settings Card embedded inside the secret ABU-RASLAN Developer Control Panel.
 * Reads & updates platform_settings/zego (appId, serverSecret) globally for all schools without rebuilding APK.
 */
@Composable
fun ZegoCloudPlatformSettingsCard(
    onStatusMessage: (String, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val zegoConfig by LiveAudioBroadcastCloudStore.zegoConfig.collectAsState()

    var appIdInput by remember(zegoConfig.appId) { mutableStateOf(zegoConfig.appId.toString()) }
    var secretInput by remember(zegoConfig.serverSecret) { mutableStateOf(zegoConfig.serverSecret) }
    var showSecret by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        LiveAudioBroadcastCloudStore.fetchZegoConfig(context)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dev_zego_broadcast_settings_card"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.GraphicEq,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "إعدادات البث الصوتي المباشر (ZEGOCLOUD)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "مفاتيح البث الموحدة لكافة المدارس سحابياً دون إعادة بناء التطبيق",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            ) {
                Text(
                    text = if (zegoConfig.isCustomFromCloud) {
                        "الحالة: يعمل حالياً بالمفاتيح السحابية المخصّصة من المسار الموحّد"
                    } else {
                        "الحالة: يعمل حالياً بالمفاتيح الافتراضية الجاهزة للمنصة"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                )
            }

            OutlinedTextField(
                value = appIdInput,
                onValueChange = { value -> appIdInput = value.filter { it.isDigit() } },
                label = { Text("معرّف التطبيق (App ID)") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Filled.Key,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_zego_app_id_input")
            )

            OutlinedTextField(
                value = secretInput,
                onValueChange = { secretInput = it.trim() },
                label = { Text("المفتاح السري للخادم (Server Secret)") },
                visualTransformation = if (showSecret) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showSecret = !showSecret }) {
                        Icon(
                            imageVector = if (showSecret) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showSecret) "إخفاء المفتاح" else "إظهار المفتاح"
                        )
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dev_zego_server_secret_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val parsedAppId = appIdInput.trim().toLongOrNull()
                        val cleanSecret = secretInput.trim()
                        if (parsedAppId == null || parsedAppId <= 0L || cleanSecret.length < 16) {
                            onStatusMessage("يرجى إدخال App ID رقمي صحيح و Server Secret صالح", false)
                            return@Button
                        }
                        isSaving = true
                        scope.launch {
                            val ok = LiveAudioBroadcastCloudStore.saveZegoConfig(context, parsedAppId, cleanSecret)
                            isSaving = false
                            if (ok) {
                                onStatusMessage("تم تحديث إعدادات البث الصوتي سحابياً لجميع المدارس بنجاح", true)
                            } else {
                                onStatusMessage("تم الحفظ محلياً وتعذّر الاتصال بالسحابة حالياً", false)
                            }
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("dev_zego_save_button")
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ وتعميم سحابياً",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        appIdInput = DEFAULT_ZEGO_APP_ID.toString()
                        secretInput = DEFAULT_ZEGO_SERVER_SECRET
                        isSaving = true
                        scope.launch {
                            LiveAudioBroadcastCloudStore.saveZegoConfig(
                                context,
                                DEFAULT_ZEGO_APP_ID,
                                DEFAULT_ZEGO_SERVER_SECRET
                            )
                            isSaving = false
                            onStatusMessage("تمت استعادة إعدادات البث الافتراضية الجاهزة", true)
                        }
                    },
                    enabled = !isSaving,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .height(46.dp)
                        .testTag("dev_zego_reset_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = "استعادة الافتراضي",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("افتراضي", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
