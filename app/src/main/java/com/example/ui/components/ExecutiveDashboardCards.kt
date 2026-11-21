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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.ExecutiveAnalyticsRadarVectorCanvas
import com.example.ui.theme.AdaptiveHighlightGlow
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalGreen

/**
 * Interactive animated emblem for "مركز الإدارة" rendered with Compose Canvas + rememberInfiniteTransition:
 * - Royal Emerald orb matching the platform identity
 * - Counter-rotating Gold & Luminous Mint leadership rings
 * - Breathing KPI performance bars oscillating smoothly
 * - Periodic soft light sheen sweeping diagonally across the emblem
 */
@Composable
fun ManagementCenterAnimatedEmblemCanvas(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "management_center_exec_emblem")

    val clockwiseAngle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 7200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mgmt_outer_ring_cw"
    )

    val counterClockwiseAngle by transition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mgmt_inner_ring_ccw"
    )

    val kpiPhaseA by transition.animateFloat(
        initialValue = 0.42f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mgmt_kpi_bar_a"
    )

    val kpiPhaseB by transition.animateFloat(
        initialValue = 0.88f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1850, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mgmt_kpi_bar_b"
    )

    val sheenProgress by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mgmt_light_sheen"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val radius = (w.coerceAtMost(h) / 2f) * 0.94f

        // 1. Royal Emerald orb backdrop matching platform's signature crest
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    RoyalGreen,
                    DeepEmerald,
                    DeepForestGreen
                ),
                center = Offset(cx, cy),
                radius = radius
            ),
            radius = radius,
            center = Offset(cx, cy)
        )

        // 2. Outer clockwise rotating Gold & Mint leadership arcs
        rotate(degrees = clockwiseAngle, pivot = Offset(cx, cy)) {
            val outerStroke = Stroke(width = (radius * 0.085f).coerceAtLeast(1.8f), cap = StrokeCap.Round)
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        GoldAccent,
                        GoldLight,
                        GoldAccent.copy(alpha = 0.25f),
                        GoldAccent
                    ),
                    center = Offset(cx, cy)
                ),
                startAngle = 0f,
                sweepAngle = 215f,
                useCenter = false,
                topLeft = Offset(cx - radius * 0.94f, cy - radius * 0.94f),
                size = Size(radius * 1.88f, radius * 1.88f),
                style = outerStroke
            )
            drawArc(
                color = AdaptiveHighlightGlow.copy(alpha = 0.85f),
                startAngle = 235f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(cx - radius * 0.94f, cy - radius * 0.94f),
                size = Size(radius * 1.88f, radius * 1.88f),
                style = outerStroke
            )
        }

        // 3. Inner counter-clockwise rotating luminous ring
        rotate(degrees = counterClockwiseAngle, pivot = Offset(cx, cy)) {
            val innerR = radius * 0.76f
            val innerStroke = Stroke(width = (radius * 0.055f).coerceAtLeast(1.3f), cap = StrokeCap.Round)
            drawArc(
                color = AdaptiveHighlightGlow.copy(alpha = 0.78f),
                startAngle = 20f,
                sweepAngle = 135f,
                useCenter = false,
                topLeft = Offset(cx - innerR, cy - innerR),
                size = Size(innerR * 2f, innerR * 2f),
                style = innerStroke
            )
            drawArc(
                color = GoldLight.copy(alpha = 0.78f),
                startAngle = 200f,
                sweepAngle = 125f,
                useCenter = false,
                topLeft = Offset(cx - innerR, cy - innerR),
                size = Size(innerR * 2f, innerR * 2f),
                style = innerStroke
            )
        }

        // 4. Breathing KPI Performance Bars inside the core
        val baseY = cy + radius * 0.34f
        val maxBarH = radius * 0.68f
        val barWidth = radius * 0.14f
        val gap = radius * 0.08f
        val barFactors = listOf(
            0.46f + (kpiPhaseA - 0.42f) * 0.75f,
            0.55f + (kpiPhaseB - 0.36f) * 0.70f,
            0.62f + (kpiPhaseA - 0.42f) * 0.65f,
            0.48f + (kpiPhaseB - 0.36f) * 0.80f
        )
        val totalBarsW = barFactors.size * barWidth + (barFactors.size - 1) * gap
        val startX = cx - totalBarsW / 2f

        barFactors.forEachIndexed { index, factor ->
            val clampedH = (maxBarH * factor.coerceIn(0.32f, 0.96f))
            val left = startX + index * (barWidth + gap)
            val top = baseY - clampedH
            val barBrush = if (index % 2 == 0) {
                Brush.verticalGradient(colors = listOf(GoldLight, GoldAccent))
            } else {
                Brush.verticalGradient(colors = listOf(Color(0xFFA7F3D0), AdaptiveHighlightGlow))
            }
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(left, top),
                size = Size(barWidth, clampedH),
                cornerRadius = CornerRadius(barWidth * 0.48f, barWidth * 0.48f)
            )
        }

        drawLine(
            color = Color.White.copy(alpha = 0.42f),
            start = Offset(startX - radius * 0.06f, baseY + radius * 0.05f),
            end = Offset(startX + totalBarsW + radius * 0.06f, baseY + radius * 0.05f),
            strokeWidth = (radius * 0.045f).coerceAtLeast(1.2f),
            cap = StrokeCap.Round
        )

        // 5. Soft diagonal periodic light sheen clipped to circle
        val clipCirclePath = Path().apply {
            addOval(
                androidx.compose.ui.geometry.Rect(
                    left = cx - radius,
                    top = cy - radius,
                    right = cx + radius,
                    bottom = cy + radius
                )
            )
        }
        clipPath(clipCirclePath) {
            val sheenX = (cx - radius) + (radius * 2f) * sheenProgress
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.28f),
                        Color.Transparent
                    ),
                    start = Offset(sheenX - radius * 0.35f, cy - radius),
                    end = Offset(sheenX + radius * 0.35f, cy + radius)
                ),
                start = Offset(sheenX - radius * 0.22f, cy - radius),
                end = Offset(sheenX + radius * 0.22f, cy + radius),
                strokeWidth = radius * 0.34f
            )
        }
    }
}

/**
 * Balanced Side-by-Side Executive Pair for Principal Dashboard:
 * "المستشار الذكي" & "مركز الإدارة" in a symmetrical Row with equal weight(1f) and 144.dp height,
 * styled natively with MaterialTheme.colorScheme (bright surface, soft themed wave tint & crisp border).
 */
@Composable
fun PrincipalExecutiveToolsPairRow(
    showSmartAdvisor: Boolean,
    onOpenSmartAdvisor: () -> Unit,
    onOpenManagementCenter: () -> Unit,
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

    val advisorInteraction = remember { MutableInteractionSource() }
    val isAdvisorPressed by advisorInteraction.collectIsPressedAsState()
    val advisorScale by animateFloatAsState(
        targetValue = if (isAdvisorPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "advisor_card_spring"
    )

    val mgmtInteraction = remember { MutableInteractionSource() }
    val isMgmtPressed by mgmtInteraction.collectIsPressedAsState()
    val mgmtScale by animateFloatAsState(
        targetValue = if (isMgmtPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "mgmt_card_spring"
    )

    val bgTransition = rememberInfiniteTransition(label = "exec_pair_bg_glow")
    val glowDrift by bgTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "exec_pair_glow_drift"
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showSmartAdvisor) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(144.dp)
                    .graphicsLayer {
                        scaleX = advisorScale
                        scaleY = advisorScale
                    }
                    .shadow(
                        elevation = 7.dp,
                        shape = RoundedCornerShape(22.dp),
                        ambientColor = primaryColor.copy(alpha = 0.14f),
                        spotColor = primaryColor.copy(alpha = 0.14f)
                    )
                    .clip(RoundedCornerShape(22.dp))
                    .clickable(
                        interactionSource = advisorInteraction,
                        indication = null,
                        onClick = onOpenSmartAdvisor
                    )
                    .testTag("dashboard_open_ai_school_advisor_card"),
                shape = RoundedCornerShape(22.dp),
                color = surfaceColor,
                border = BorderStroke(
                    width = 1.25.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            primaryColor.copy(alpha = 0.45f),
                            tertiaryColor.copy(alpha = 0.25f)
                        )
                    )
                )
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Subtle luminous wave + radial tint matching WaveStatCard3D aesthetic
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.10f),
                                    Color.Transparent
                                ),
                                center = Offset(w * glowDrift, h * 0.20f),
                                radius = w * 0.65f
                            ),
                            radius = w * 0.65f,
                            center = Offset(w * glowDrift, h * 0.20f)
                        )
                        val wave = Path().apply {
                            moveTo(0f, h * 0.78f)
                            cubicTo(w * 0.34f, h * 0.68f, w * 0.68f, h * 0.88f, w, h * 0.76f)
                            lineTo(w, h)
                            lineTo(0f, h)
                            close()
                        }
                        drawPath(path = wave, color = primaryContainer.copy(alpha = 0.42f))
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 13.dp, vertical = 13.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ExecutiveAnalyticsRadarVectorCanvas(
                                modifier = Modifier.size(46.dp)
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = primaryContainer.copy(alpha = 0.78f),
                                border = BorderStroke(0.8.dp, primaryColor.copy(alpha = 0.35f))
                            ) {
                                Text(
                                    text = "تحليل فوري",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    fontWeight = FontWeight.Bold,
                                    color = onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(
                                text = "المستشار الذكي",
                                style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "التحليل الإداري والتوصيات",
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                fontWeight = FontWeight.SemiBold,
                                color = onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Card 2: Management Center ("مركز الإدارة") with Interactive Animated Canvas Emblem
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(144.dp)
                .graphicsLayer {
                    scaleX = mgmtScale
                    scaleY = mgmtScale
                }
                .shadow(
                    elevation = 7.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = tertiaryColor.copy(alpha = 0.14f),
                    spotColor = primaryColor.copy(alpha = 0.12f)
                )
                .clip(RoundedCornerShape(22.dp))
                .clickable(
                    interactionSource = mgmtInteraction,
                    indication = null,
                    onClick = onOpenManagementCenter
                )
                .testTag("dashboard_open_ministry_executive_suite_card"),
            shape = RoundedCornerShape(22.dp),
            color = surfaceColor,
            border = BorderStroke(
                width = 1.25.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        tertiaryColor.copy(alpha = 0.48f),
                        primaryColor.copy(alpha = 0.35f)
                    )
                )
            )
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                tertiaryColor.copy(alpha = 0.11f),
                                Color.Transparent
                            ),
                            center = Offset(w * (1f - glowDrift), h * 0.22f),
                            radius = w * 0.65f
                        ),
                        radius = w * 0.65f,
                        center = Offset(w * (1f - glowDrift), h * 0.22f)
                    )
                    val wave = Path().apply {
                        moveTo(0f, h * 0.78f)
                        cubicTo(w * 0.32f, h * 0.88f, w * 0.68f, h * 0.68f, w, h * 0.76f)
                        lineTo(w, h)
                        lineTo(0f, h)
                        close()
                    }
                    drawPath(path = wave, color = tertiaryContainer.copy(alpha = 0.44f))
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 13.dp, vertical = 13.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ManagementCenterAnimatedEmblemCanvas(
                            modifier = Modifier.size(46.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = tertiaryContainer.copy(alpha = 0.80f),
                            border = BorderStroke(0.8.dp, tertiaryColor.copy(alpha = 0.40f))
                        ) {
                            Text(
                                text = "قيادة تنفيذية",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                fontWeight = FontWeight.Bold,
                                color = onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "مركز الإدارة",
                            style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "التحليلات والتقارير التنفيذية",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
