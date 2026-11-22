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
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.RoyalGreen

/**
 * أيقونة هادئة وأنيقة لبطاقة "المستشار الذكي":
 * - حاوية زوايا دائرية ناعمة (16dp) بتدرج زمردي فاتح متناسق مع هوية المنصة
 * - نجمة إرشاد ذكية صافية مع هالة ضوئية ناعمة وتنفس بطيء مريح للعين (0.96f..1.04f)
 */
@Composable
fun SmartAdvisorCalmEmblem(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "smart_advisor_calm_emblem")
    val breatheScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "advisor_breathe_scale"
    )
    val auraAlpha by transition.animateFloat(
        initialValue = 0.24f,
        targetValue = 0.52f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "advisor_aura_alpha"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.34f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFECFDF5),
                            Color(0xFFD1FAE5)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // Soft breathing radial aura
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = auraAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.46f
                    ),
                    radius = w * 0.46f,
                    center = Offset(cx, cy)
                )

                // Clean 4-point guidance star (Smart Advisor)
                val starR = w * 0.27f * breatheScale
                val innerR = starR * 0.34f
                val mainStar = Path().apply {
                    moveTo(cx, cy - starR)
                    quadraticBezierTo(cx, cy, cx + starR, cy)
                    quadraticBezierTo(cx, cy, cx, cy + starR)
                    quadraticBezierTo(cx, cy, cx - starR, cy)
                    quadraticBezierTo(cx, cy, cx, cy - starR)
                    close()
                }
                drawPath(
                    path = mainStar,
                    brush = Brush.linearGradient(
                        colors = listOf(RoyalGreen, DeepEmerald),
                        start = Offset(cx - starR, cy - starR),
                        end = Offset(cx + starR, cy + starR)
                    )
                )

                // Crisp small accent sparkle (top-left) in warm gold
                val miniCx = cx - w * 0.22f
                val miniCy = cy - h * 0.21f
                val miniR = w * 0.085f * (2f - breatheScale)
                val miniStar = Path().apply {
                    moveTo(miniCx, miniCy - miniR)
                    lineTo(miniCx + miniR * 0.32f, miniCy - miniR * 0.32f)
                    lineTo(miniCx + miniR, miniCy)
                    lineTo(miniCx + miniR * 0.32f, miniCy + miniR * 0.32f)
                    lineTo(miniCx, miniCy + miniR)
                    lineTo(miniCx - miniR * 0.32f, miniCy + miniR * 0.32f)
                    lineTo(miniCx - miniR, miniCy)
                    lineTo(miniCx - miniR * 0.32f, miniCy - miniR * 0.32f)
                    close()
                }
                drawPath(path = miniStar, color = GoldAccent)

                // Subtle luminous core dot
                drawCircle(
                    color = Color(0xFFFEF9C3),
                    radius = innerR * 0.34f,
                    center = Offset(cx, cy)
                )
            }
        }
    }
}

/**
 * أيقونة هادئة وأنيقة لبطاقة "مركز الإدارة":
 * - حاوية زوايا دائرية ناعمة (16dp) بتدرج زمردي-ذهبي فاتح هادئ
 * - درع قيادي وتقارير منظمة بخطوط فيكتور واضحة
 * - حركة تنفس هادئة ولمعة ضوئية ناعمة بدون دوائر مظلمة أو خطوط متشابكة
 */
@Composable
fun ManagementCenterAnimatedEmblemCanvas(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "management_center_calm_emblem")

    val breatheScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mgmt_breathe_scale"
    )

    val sheenProgress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mgmt_calm_sheen"
    )

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, GoldAccent.copy(alpha = 0.42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFFBEB),
                            Color(0xFFECFDF5)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val cx = w / 2f
                val cy = h / 2f

                // Clip subtle sheen to container bounds
                val containerClip = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = 0f,
                            top = 0f,
                            right = w,
                            bottom = h,
                            cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                        )
                    )
                }
                clipPath(containerClip) {
                    val sheenX = w * sheenProgress
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.White.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            start = Offset(sheenX - w * 0.25f, 0f),
                            end = Offset(sheenX + w * 0.25f, h)
                        ),
                        start = Offset(sheenX - w * 0.14f, 0f),
                        end = Offset(sheenX + w * 0.14f, h),
                        strokeWidth = w * 0.24f
                    )
                }

                // Clean Executive Shield Outline (Leadership & Reports)
                val sw = w * 0.26f * breatheScale
                val sh = h * 0.29f * breatheScale
                val shieldPath = Path().apply {
                    moveTo(cx, cy - sh)
                    lineTo(cx + sw, cy - sh * 0.62f)
                    lineTo(cx + sw * 0.92f, cy + sh * 0.12f)
                    quadraticBezierTo(cx + sw * 0.62f, cy + sh * 0.78f, cx, cy + sh)
                    quadraticBezierTo(cx - sw * 0.62f, cy + sh * 0.78f, cx - sw * 0.92f, cy + sh * 0.12f)
                    lineTo(cx - sw, cy - sh * 0.62f)
                    close()
                }
                drawPath(
                    path = shieldPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF059669).copy(alpha = 0.16f),
                            GoldLight.copy(alpha = 0.30f)
                        )
                    )
                )
                drawPath(
                    path = shieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(DeepEmerald, Color(0xFFD97706)),
                        start = Offset(cx - sw, cy - sh),
                        end = Offset(cx + sw, cy + sh)
                    ),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                // Structured Mini Executive Bars inside the shield (crisp, static & balanced)
                val barBottom = cy + sh * 0.44f
                val barW = w * 0.068f
                val barGap = w * 0.045f
                val heights = listOf(sh * 0.42f, sh * 0.68f, sh * 0.54f)
                val totalBarsW = heights.size * barW + (heights.size - 1) * barGap
                val startX = cx - totalBarsW / 2f

                heights.forEachIndexed { idx, barH ->
                    val bx = startX + idx * (barW + barGap)
                    drawRoundRect(
                        color = if (idx == 1) Color(0xFFD97706) else DeepEmerald,
                        topLeft = Offset(bx, barBottom - barH),
                        size = Size(barW, barH),
                        cornerRadius = CornerRadius(barW * 0.45f, barW * 0.45f)
                    )
                }
            }
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
                            SmartAdvisorCalmEmblem(
                                modifier = Modifier.size(50.dp)
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
                            modifier = Modifier.size(50.dp)
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
