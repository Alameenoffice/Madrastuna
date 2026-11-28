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
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.formatCleanDate
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
import androidx.compose.animation.core.Animatable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import androidx.compose.ui.text.style.TextOverflow
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
 * Sleek Edge-to-Edge Full-Width Live Audio Broadcast Ribbon seamlessly docked under the Welcome Hero.
 * - Zero floating box borders, zero rounded card margins, zero harsh black backgrounds.
 * - LIVE state: Bright emerald-mint gradient (Light & Dark harmonious) + 3 interactive oscillating
 *   vector acoustic waves across the full width (reacts with wave boost & luminous sweep on touch)
 *   + compact "انضمام" button.
 * - ENDED state: Calm light mint-gray gradient with soft hairline bottom border + slow breathing
 *   thin vector wave in the background + crisp dark typography + light refined "سجل البثوث" button.
 */
@Composable
fun HomeLiveAudioBroadcastCard(
    room: LiveAudioRoom,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenHistoryClick: () -> Unit = onJoinClick,
    topOverlapPadding: Dp = 0.dp
) {
    val isDark = isSystemInDarkTheme()
    val isLive = room.isLive
    val scope = rememberCoroutineScope()

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val touchBoost by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "broadcast_strip_touch_boost"
    )
    val tapFlashProgress = remember { Animatable(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "home_broadcast_vector_waves")
    val livePhasePrimary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_wave_phase_primary"
    )
    val livePhaseSecondary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "live_wave_phase_secondary"
    )
    val endedBreath by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ended_wave_calm_breath"
    )

    val stripGradientBrush = when {
        isLive && !isDark -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFCFF7E6),
                Color(0xFFE5FCF2),
                Color(0xFFC4F2DE)
            )
        )
        isLive && isDark -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF0A4737),
                Color(0xFF0E5A45),
                Color(0xFF0B3F33)
            )
        )
        !isLive && !isDark -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFFEDF5F2),
                Color(0xFFF5FAF8),
                Color(0xFFE8F2EE)
            )
        )
        else -> Brush.horizontalGradient(
            colors = listOf(
                Color(0xFF1E2F2B),
                Color(0xFF243833),
                Color(0xFF1C2C28)
            )
        )
    }

    val bottomHairlineColor = when {
        isLive && !isDark -> Color(0xFF059669).copy(alpha = 0.24f)
        isLive && isDark -> Color(0xFF34D399).copy(alpha = 0.30f)
        !isLive && !isDark -> Color(0xFFC5D8D0)
        else -> Color(0xFF3A544C)
    }
    val titleColor = when {
        isLive && !isDark -> Color(0xFF044635)
        isLive && isDark -> Color(0xFFF0FDF4)
        !isLive && !isDark -> Color(0xFF16352C)
        else -> Color(0xFFE6F4EF)
    }
    val metaColor = when {
        isLive && !isDark -> Color(0xFF0F5E48)
        isLive && isDark -> Color(0xFFA7F3D0).copy(alpha = 0.92f)
        !isLive && !isDark -> Color(0xFF3E5D53)
        else -> Color(0xFFA8C6BC)
    }

    val wavePrimaryColor = when {
        isLive && !isDark -> Color(0xFF059669).copy(alpha = 0.16f + 0.10f * touchBoost)
        isLive && isDark -> Color(0xFF34D399).copy(alpha = 0.22f + 0.12f * touchBoost)
        !isLive && !isDark -> Color(0xFF059669).copy(alpha = 0.12f + 0.06f * touchBoost)
        else -> Color(0xFF6EE7B7).copy(alpha = 0.15f + 0.08f * touchBoost)
    }
    val waveSecondaryColor = when {
        isLive && !isDark -> Color(0xFF10B981).copy(alpha = 0.13f + 0.08f * touchBoost)
        isLive && isDark -> Color(0xFFA7F3D0).copy(alpha = 0.14f + 0.08f * touchBoost)
        !isLive && !isDark -> Color(0xFF64748B).copy(alpha = 0.10f)
        else -> Color(0xFF94A3B8).copy(alpha = 0.12f)
    }
    val waveGoldColor = if (isDark) {
        BroadcastGoldLight.copy(alpha = 0.19f + 0.12f * touchBoost)
    } else {
        BroadcastGoldAccent.copy(alpha = 0.18f + 0.12f * touchBoost)
    }
    val shimmerSweepColor = if (isDark) {
        Color(0xFFA7F3D0)
    } else {
        Color.White
    }

    val clickAction: () -> Unit = {
        scope.launch {
            tapFlashProgress.snapTo(0f)
            tapFlashProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing)
            )
            tapFlashProgress.snapTo(0f)
        }
        if (isLive) onJoinClick() else onOpenHistoryClick()
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(stripGradientBrush)
            .clickable(
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(
                    color = if (isLive) Color(0xFF059669) else Color(0xFF64748B)
                ),
                onClick = clickAction
            )
            .testTag("home_live_audio_broadcast_card")
    ) {
        // Full-Width Interactive Vector Acoustic Wave Animation Background
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            if (w <= 0f || h <= 0f) return@Canvas
            val topSafe = topOverlapPadding.toPx().coerceAtMost(h * 0.45f)
            val usableH = (h - topSafe).coerceAtLeast(18f)
            val centerY = topSafe + usableH * 0.56f
            val stepPx = (w / 46f).coerceIn(8f, 22f)

            if (isLive) {
                val ampScale = 1f + 0.42f * touchBoost

                // Soft translucent fill wave behind primary vector line
                val fillPath = Path()
                val wave1Path = Path()
                val wave2Path = Path()
                val wave3Path = Path()

                var x = 0f
                var first = true
                while (x <= w + stepPx) {
                    val cx = x.coerceAtMost(w)
                    val normX = cx / w
                    // Envelope tapers gently near far edges while staying vibrant across width
                    val env = 0.55f + 0.45f * sin(normX * PI).toFloat()

                    val y1 = centerY + sin(normX * 4.4 * PI + livePhasePrimary).toFloat() * (usableH * 0.21f * env * ampScale)
                    val y2 = centerY + 2.dp.toPx() + sin(normX * 3.1 * PI - livePhaseSecondary + 1.1).toFloat() * (usableH * 0.16f * env * ampScale)
                    val y3 = centerY - 2.dp.toPx() + sin(normX * 6.0 * PI + livePhasePrimary * 1.35 + 2.3).toFloat() * (usableH * 0.11f * env * ampScale)

                    if (first) {
                        wave1Path.moveTo(cx, y1)
                        wave2Path.moveTo(cx, y2)
                        wave3Path.moveTo(cx, y3)
                        fillPath.moveTo(cx, h)
                        fillPath.lineTo(cx, y1)
                        first = false
                    } else {
                        wave1Path.lineTo(cx, y1)
                        wave2Path.lineTo(cx, y2)
                        wave3Path.lineTo(cx, y3)
                        fillPath.lineTo(cx, y1)
                    }
                    if (cx >= w) break
                    x += stepPx
                }
                fillPath.lineTo(w, h)
                fillPath.close()

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            wavePrimaryColor.copy(alpha = wavePrimaryColor.alpha * 0.36f),
                            Color.Transparent
                        ),
                        startY = topSafe,
                        endY = h
                    )
                )
                drawPath(
                    path = wave2Path,
                    color = waveSecondaryColor,
                    style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = wave3Path,
                    color = waveGoldColor,
                    style = Stroke(width = 1.05.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = wave1Path,
                    color = wavePrimaryColor,
                    style = Stroke(width = 1.65.dp.toPx(), cap = StrokeCap.Round)
                )
            } else {
                // Calm, refined slow-breathing thin vector wave for ENDED broadcast
                val breathShift = (endedBreath - 0.5f) * 1.45f
                val calmAmp = usableH * (0.12f + 0.045f * endedBreath + 0.06f * touchBoost)

                val calmWaveMain = Path()
                val calmWaveEcho = Path()
                var x = 0f
                var first = true
                while (x <= w + stepPx) {
                    val cx = x.coerceAtMost(w)
                    val normX = cx / w
                    val yMain = centerY + sin(normX * 2.6 * PI + breathShift).toFloat() * calmAmp
                    val yEcho = centerY + 3.dp.toPx() + sin(normX * 1.9 * PI - breathShift * 0.8 + 0.9).toFloat() * (calmAmp * 0.68f)
                    if (first) {
                        calmWaveMain.moveTo(cx, yMain)
                        calmWaveEcho.moveTo(cx, yEcho)
                        first = false
                    } else {
                        calmWaveMain.lineTo(cx, yMain)
                        calmWaveEcho.lineTo(cx, yEcho)
                    }
                    if (cx >= w) break
                    x += stepPx
                }

                drawPath(
                    path = calmWaveEcho,
                    color = waveSecondaryColor,
                    style = Stroke(width = 0.95.dp.toPx(), cap = StrokeCap.Round)
                )
                drawPath(
                    path = calmWaveMain,
                    color = wavePrimaryColor,
                    style = Stroke(width = 1.35.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // Soft luminous flash sweep across width on tap interaction
            val flash = tapFlashProgress.value
            if (flash > 0f && flash < 1f) {
                val sweepCenterX = w * flash
                val sweepHalfW = w * 0.28f
                val alphaPeak = sin(flash * PI).toFloat() * (if (isLive) 0.24f else 0.16f)
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            shimmerSweepColor.copy(alpha = alphaPeak),
                            Color.Transparent
                        ),
                        startX = (sweepCenterX - sweepHalfW).coerceAtLeast(0f),
                        endX = (sweepCenterX + sweepHalfW).coerceAtMost(w)
                    )
                )
            }
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 14.dp,
                        top = topOverlapPadding + 7.dp,
                        bottom = 8.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Row 1: Status pill ("بث مباشر" / "بث منتهي") + Broadcast Title in single line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (isLive) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDark) Color(0xFF5A161E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.88f),
                                border = BorderStroke(
                                    0.75.dp,
                                    if (isDark) Color(0xFFF87171).copy(alpha = 0.65f) else Color(0xFFFCA5A5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    PulsingLiveDotCanvas(size = 9.dp)
                                    Text(
                                        text = "بث مباشر",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isDark) Color(0xFFFCA5A5) else Color(0xFFDC2626),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (isDark) Color.White.copy(alpha = 0.11f) else Color(0xFF047857).copy(alpha = 0.09f),
                                border = BorderStroke(
                                    0.7.dp,
                                    if (isDark) Color(0xFF6EE7B7).copy(alpha = 0.28f) else Color(0xFF047857).copy(alpha = 0.20f)
                                )
                            ) {
                                Text(
                                    text = "بث منتهي",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDark) Color(0xFFCBE5DC) else Color(0xFF1F5143),
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = room.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = titleColor,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Row 2: Host + Audience (when live) OR Duration + Host (when ended) in single line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (!isLive) {
                            Icon(
                                imageVector = Icons.Filled.Schedule,
                                contentDescription = null,
                                tint = metaColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "المدة: ${room.formattedDurationArabic()} • ${room.hostName}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = metaColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = "المشرف: ${room.hostName} • ${room.audienceDisplayLabel}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = metaColor,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Compact Action CTA (RTL end): "انضمام" when live, light refined "سجل البثوث" when ended
                if (isLive) {
                    Surface(
                        onClick = onJoinClick,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFF10B981) else Color(0xFF047857),
                        contentColor = if (isDark) Color(0xFF022C22) else Color.White,
                        shadowElevation = 1.dp,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("home_live_audio_join_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "انضمام",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "انضمام للبث",
                                modifier = Modifier.size(13.5.dp)
                            )
                        }
                    }
                } else {
                    Surface(
                        onClick = onOpenHistoryClick,
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.88f),
                        border = BorderStroke(
                            0.85.dp,
                            if (isDark) Color(0xFF6B9386).copy(alpha = 0.55f) else Color(0xFFB7CFC6)
                        ),
                        modifier = Modifier
                            .height(30.dp)
                            .testTag("home_live_audio_history_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "سجل البثوث",
                                tint = if (isDark) Color(0xFFD5EDE4) else Color(0xFF1A4B3E),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "سجل البثوث",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDark) Color(0xFFE6F4EF) else Color(0xFF1A4B3E),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Soft subtle hairline bottom border
            HorizontalDivider(
                thickness = 0.85.dp,
                color = bottomHairlineColor
            )
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

fun formatParticipantsCountArabic(count: Int): String {
    val safe = count.coerceAtLeast(1)
    return when (safe) {
        1 -> "مشارك واحد"
        2 -> "مشاركان"
        in 3..10 -> "$safe مشاركين"
        else -> "$safe مشاركاً"
    }
}

/**
 * Dedicated Past Broadcasts History Modal Dialog ("سجل البثوث السابقة").
 * Displays every ended broadcast with title, host name, date, exact Arabic duration,
 * and real participant count. Grants Principal-only single delete and clear-all with confirmation.
 */
@Composable
fun PastBroadcastsHistoryDialog(
    endedRooms: List<LiveAudioRoom>,
    isPrincipal: Boolean,
    isProcessingDelete: Boolean = false,
    onDeleteSingle: (LiveAudioRoom) -> Unit,
    onClearAll: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()
    var pendingDeleteRoom by remember { mutableStateOf<LiveAudioRoom?>(null) }
    var showClearAllConfirm by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .heightIn(max = 640.dp)
                .testTag("past_broadcasts_history_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 10.dp,
            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = if (isDark) 0.34f else 0.18f))
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Emerald-Gold Header Strip
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BroadcastDeepEmerald, BroadcastRichEmerald, colorScheme.primary)
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
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
                                color = Color.White.copy(alpha = 0.16f),
                                border = BorderStroke(0.8.dp, BroadcastGoldLight.copy(alpha = 0.55f)),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.History,
                                        contentDescription = null,
                                        tint = BroadcastGoldLight,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Column {
                                Text(
                                    text = "سجل البثوث الصوتية السابقة",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (endedRooms.isEmpty()) "لا توجد جلسات محفوظة حالياً"
                                    else "إجمالي الجلسات المؤرشفة: ${endedRooms.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = BroadcastMintSoft
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_past_broadcasts_history_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق سجل البثوث",
                                tint = Color.White
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.2.dp, color = BroadcastGoldAccent.copy(alpha = 0.65f))

                // Principal-Only Clear-All Toolbar
                if (isPrincipal && endedRooms.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.35f) else Color(0xFFF8FAFC))
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "إدارة أرشيف البثوث (صلاحية المدير)",
                            style = MaterialTheme.typography.labelMedium,
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            onClick = { if (!isProcessingDelete) showClearAllConfirm = true },
                            shape = RoundedCornerShape(10.dp),
                            color = BroadcastCoralEnd.copy(alpha = if (isDark) 0.24f else 0.12f),
                            border = BorderStroke(0.9.dp, BroadcastCoralEnd.copy(alpha = 0.55f)),
                            modifier = Modifier.testTag("clear_all_past_broadcasts_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.DeleteSweep,
                                    contentDescription = "مسح السجل بالكامل",
                                    tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "مسح السجل بالكامل",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.8.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                }

                // Ended Broadcasts List or Calm Empty State
                if (endedRooms.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 38.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colorScheme.primary.copy(alpha = 0.10f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.History,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Text(
                            text = "سجل البثوث السابقة فارغ حالياً",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "سيتم حفظ كل بث صوتي منتهٍ تلقائياً هنا مع مدته الدقيقة وعدد المشاركين.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(endedRooms, key = { "history_dlg_${it.id}" }) { room ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.45f) else Color(0xFFF8FAFC),
                                border = BorderStroke(0.9.dp, colorScheme.outlineVariant.copy(alpha = 0.65f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(7.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = room.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = colorScheme.onSurface,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Person,
                                                    contentDescription = null,
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = "المشرف: ${room.hostName} • ${room.audienceDisplayLabel}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        if (isPrincipal) {
                                            IconButton(
                                                onClick = { if (!isProcessingDelete) pendingDeleteRoom = room },
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .testTag("delete_history_room_${room.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "حذف هذا البث من السجل",
                                                    tint = if (isDark) Color(0xFFFCA5A5) else BroadcastCoralEnd,
                                                    modifier = Modifier.size(19.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Metadata Row: Date + Exact Arabic Duration + Real Participants Count
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = colorScheme.primary.copy(alpha = if (isDark) 0.18f else 0.10f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Schedule,
                                                    contentDescription = null,
                                                    tint = colorScheme.primary,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = room.formattedDurationArabic(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = BroadcastGoldAccent.copy(alpha = if (isDark) 0.20f else 0.14f)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Groups,
                                                    contentDescription = null,
                                                    tint = if (isDark) BroadcastGoldLight else Color(0xFF92600A),
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = formatParticipantsCountArabic(room.totalParticipantsCount),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (isDark) BroadcastGoldLight else Color(0xFF784E06),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = "التاريخ: ${formatCleanDate(room.resolvedCreatedDateForDisplay())}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Close Button
                HorizontalDivider(thickness = 0.8.dp, color = colorScheme.outlineVariant.copy(alpha = 0.5f))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("إغلاق", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Confirmation Dialog for Single Broadcast Delete (Principal only)
    pendingDeleteRoom?.let { targetRoom ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRoom = null },
            title = {
                Text(
                    text = "تأكيد حذف البث من السجل",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف جلسة «${targetRoom.title}» نهائياً من سجل البثوث السابقة لدى جميع المستخدمين؟",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val toDelete = targetRoom
                        pendingDeleteRoom = null
                        onDeleteSingle(toDelete)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_delete_single_broadcast_btn")
                ) {
                    Text("حذف نهائي", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRoom = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Confirmation Dialog for Clear-All Past Broadcasts (Principal only)
    if (showClearAllConfirm) {
        AlertDialog(
            onDismissRequest = { showClearAllConfirm = false },
            title = {
                Text(
                    text = "تأكيد مسح سجل البثوث بالكامل",
                    fontWeight = FontWeight.ExtraBold
                )
            },
            text = {
                Text(
                    text = "سيتم حذف جميع جلسات البث المنتهية (${endedRooms.size} جلسات) نهائياً من قاعدة البيانات ولن يمكن استرجاعها. هل تريد المتابعة؟",
                    lineHeight = 22.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showClearAllConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BroadcastCoralEnd),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("confirm_clear_all_broadcasts_btn")
                ) {
                    Text("مسح السجل بالكامل", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearAllConfirm = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

/**
 * Live In-App Vector Acoustic Wave & Microphone Pulse Header (Compose Canvas)
 * matching the exact emerald-mint-gold visual identity of the external BigPictureStyle banner.
 */
@Composable
fun LiveBroadcastAcousticVectorBannerCanvas(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "urgent_invite_live_waves")
    val wavePhasePrimary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "urgent_wave_primary"
    )
    val wavePhaseSecondary by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "urgent_wave_secondary"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urgent_mic_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val centerY = h * 0.68f
        val stepPx = (w / 44f).coerceIn(8f, 20f)
        val fillPath = Path()
        val wave1 = Path()
        val wave2 = Path()
        val wave3 = Path()

        var x = 0f
        var first = true
        while (x <= w + stepPx) {
            val cx = x.coerceAtMost(w)
            val norm = cx / w
            val env = 0.50f + 0.50f * sin(norm * PI).toFloat()
            val y1 = centerY + sin(norm * 4.2 * PI + wavePhasePrimary).toFloat() * (h * 0.16f * env)
            val y2 = centerY - 3.dp.toPx() + sin(norm * 3.0 * PI - wavePhaseSecondary + 1.2).toFloat() * (h * 0.12f * env)
            val y3 = centerY + 4.dp.toPx() + sin(norm * 5.6 * PI + wavePhasePrimary * 1.3 + 2.1).toFloat() * (h * 0.08f * env)
            if (first) {
                fillPath.moveTo(cx, h)
                fillPath.lineTo(cx, y1)
                wave1.moveTo(cx, y1)
                wave2.moveTo(cx, y2)
                wave3.moveTo(cx, y3)
                first = false
            } else {
                fillPath.lineTo(cx, y1)
                wave1.lineTo(cx, y1)
                wave2.lineTo(cx, y2)
                wave3.lineTo(cx, y3)
            }
            if (cx >= w) break
            x += stepPx
        }
        fillPath.lineTo(w, h)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color(0xFF34D399).copy(alpha = 0.22f),
                    Color.Transparent
                ),
                startY = h * 0.35f,
                endY = h
            )
        )
        drawPath(
            path = wave2,
            color = BroadcastMintSoft.copy(alpha = 0.28f),
            style = Stroke(width = 1.25.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = wave3,
            color = BroadcastGoldLight.copy(alpha = 0.38f),
            style = Stroke(width = 1.1.dp.toPx(), cap = StrokeCap.Round)
        )
        drawPath(
            path = wave1,
            color = Color(0xFF34D399).copy(alpha = 0.55f),
            style = Stroke(width = 1.7.dp.toPx(), cap = StrokeCap.Round)
        )

        // Subtle ambient golden pulse glow at top-end
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    BroadcastGoldLight.copy(alpha = 0.22f),
                    Color.Transparent
                ),
                center = Offset(w * 0.16f, h * 0.40f),
                radius = h * 0.55f * pulseScale
            ),
            radius = h * 0.55f * pulseScale,
            center = Offset(w * 0.16f, h * 0.40f)
        )
    }
}

/**
 * Prominent Urgent Broadcast Join Invite Dialog displayed instantly on the recipient's screen
 * when the Principal sends an urgent join alert from inside the live broadcast room.
 * Features live emerald-mint-gold Compose Canvas acoustic wave animation matching the external banner.
 */
@Composable
fun UrgentBroadcastInviteOverlayDialog(
    invite: LiveRoomUrgentInvite,
    onJoinNow: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = isSystemInDarkTheme()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .testTag("urgent_broadcast_invite_dialog"),
            shape = RoundedCornerShape(22.dp),
            color = colorScheme.surface,
            shadowElevation = 14.dp,
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(BroadcastGoldAccent, Color(0xFF059669), BroadcastGoldLight)
                )
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BroadcastDeepEmerald, BroadcastRichEmerald, Color(0xFF0A4E5A))
                            )
                        )
                ) {
                    LiveBroadcastAcousticVectorBannerCanvas(
                        modifier = Modifier.matchParentSize()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Animated live microphone studio glass badge
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                                .border(1.2.dp, BroadcastGoldLight.copy(alpha = 0.72f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Podcasts,
                                contentDescription = null,
                                tint = BroadcastGoldLight,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFEF4444).copy(alpha = 0.28f),
                                border = BorderStroke(0.8.dp, Color(0xFFFCA5A5).copy(alpha = 0.75f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    PulsingLiveDotCanvas(size = 9.dp)
                                    Text(
                                        text = "تنبيه عاجل للدخول للبث المباشر",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Text(
                                text = invite.roomTitle.ifBlank { "جلسة بث صوتي مباشر" },
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                HorizontalDivider(thickness = 1.2.dp, color = BroadcastGoldAccent.copy(alpha = 0.65f))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) colorScheme.surfaceVariant.copy(alpha = 0.55f) else Color(0xFFF0FDF4),
                        border = BorderStroke(0.8.dp, colorScheme.primary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تدعوك ${invite.senderName} للانضمام فوراً إلى غرفة البث الصوتي المباشر الجارية الآن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 22.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    SpringBroadcastButton(
                        text = "انضمام فوري للبث",
                        icon = Icons.Filled.Podcasts,
                        onClick = onJoinNow,
                        height = 48.dp,
                        cornerRadius = 14.dp,
                        modifier = Modifier.fillMaxWidth(),
                        testTagName = "urgent_invite_join_now_btn"
                    )

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .testTag("urgent_invite_dismiss_btn")
                    ) {
                        Text(
                            text = "لاحقاً",
                            color = colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Calm, polite Android 13+ notification permission dialog so users can opt into
 * external status-bar alerts for new live audio broadcasts and urgent join invites.
 */
@Composable
fun LiveNotificationPermissionRationaleDialog(
    onEnableNotifications: () -> Unit,
    onLater: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onLater,
        icon = {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "تفعيل إشعارات البث الصوتي المباشر",
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "لإشعارك فور انطلاق أي بث صوتي مباشر أو وصول دعوة عاجلة من إدارة المدرسة حتى عند استخدام تطبيقات أخرى، يمكنك تفعيل إذن الإشعارات.",
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onEnableNotifications,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("enable_live_notifications_btn")
            ) {
                Text("تفعيل الإشعارات", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onLater,
                modifier = Modifier.testTag("later_live_notifications_btn")
            ) {
                Text("لاحقاً")
            }
        }
    )
}

/**
 * One-time smart prompt guiding the user to exempt "مدرستي الذكية" from Android battery optimization
 * so external notifications for live audio broadcasts and urgent announcements arrive reliably even when closed.
 */
@Composable
fun BatteryOptimizationGuidanceDialog(
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Filled.NotificationsActive,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "ضمان وصول الإشعارات عند إغلاق التطبيق",
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Text(
                text = "لضمان وصول إشعارات البث الصوتي المباشر وتبليغات إدارة المدرسة العاجلة حتى عندما يكون التطبيق مغلقاً، يُنصح باستثناء «مدرستي الذكية» من قيود توفير البطارية في إعدادات الجهاز.",
                lineHeight = 22.sp
            )
        },
        confirmButton = {
            Button(
                onClick = onOpenSettings,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF047857),
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("open_battery_opt_settings_btn")
            ) {
                Text("فتح إعدادات البطارية", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_battery_opt_prompt_btn")
            ) {
                Text("لاحقاً")
            }
        }
    )
}


