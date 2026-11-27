package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EventAvailable
import androidx.compose.material.icons.filled.LocalLibrary
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val COMPACT_DATE_8_REGEX = Regex("^(\\d{4})(\\d{2})(\\d{2})(.*)$")
private val SEPARATED_DATE_REGEX = Regex("^(\\d{4})[\\-/\\.](\\d{1,2})[\\-/\\.](\\d{1,2})(.*)$")
private val EMBEDDED_COMPACT_DATE_REGEX = Regex("\\b(20\\d{2})(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\b")

/**
 * دالة مساعدة موحّدة لتنسيق التواريخ الملتصقة أو غير المنتظمة:
 * تحوّل صيغاً مثل "20261114" أو "20261111" أو "2026/11/6" إلى صيغة نظيفة موحّدة "2026-11-14".
 */
fun formatCleanDate(rawDate: String): String {
    val trimmed = rawDate.trim()
    if (trimmed.isEmpty()) return ""

    val ascii = buildString(trimmed.length) {
        for (ch in trimmed) {
            when (ch) {
                in '٠'..'٩' -> append(('0'.code + (ch.code - '٠'.code)).toChar())
                in '۰'..'۹' -> append(('0'.code + (ch.code - '۰'.code)).toChar())
                else -> append(ch)
            }
        }
    }

    COMPACT_DATE_8_REGEX.matchEntire(ascii)?.let { match ->
        val year = match.groupValues[1]
        val month = match.groupValues[2]
        val day = match.groupValues[3]
        val monthInt = month.toIntOrNull() ?: 0
        val dayInt = day.toIntOrNull() ?: 0
        if (monthInt in 1..12 && dayInt in 1..31) {
            val suffix = match.groupValues[4].trim()
            return if (suffix.isEmpty()) "$year-$month-$day" else "$year-$month-$day $suffix"
        }
    }

    SEPARATED_DATE_REGEX.matchEntire(ascii)?.let { match ->
        val year = match.groupValues[1]
        val month = match.groupValues[2].padStart(2, '0')
        val day = match.groupValues[3].padStart(2, '0')
        val suffix = match.groupValues[4].trim()
        return if (suffix.isEmpty()) "$year-$month-$day" else "$year-$month-$day $suffix"
    }

    return EMBEDDED_COMPACT_DATE_REGEX.replace(ascii) { m ->
        "${m.groupValues[1]}-${m.groupValues[2]}-${m.groupValues[3]}"
    }
}

// =================================================================================================
// 2. البصمة الخاصة بقسم "ملفي الشخصي والبيانات الشاملة":
//    بطاقة الهوية الهولوغرافية المضيئة (Holographic Identity Header Card)
// =================================================================================================
@Composable
fun HolographicIdentityHeaderCard(
    photoSource: String?,
    persona: SmartAvatarPersona,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    val labels = remember {
        listOf(
            "هوية رقمية موثّقة",
            "كود الطالب النشط",
            "ملف أكاديمي محدّث"
        )
    }
    var activeIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            activeIndex = (activeIndex + 1) % labels.size
        }
    }

    val holoTransition = rememberInfiniteTransition(label = "holographic_id_sheen")
    val sheenProgress by holoTransition.animateFloat(
        initialValue = -0.55f,
        targetValue = 1.55f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "holographic_sheen_sweep"
    )
    val rimGlow by holoTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holographic_rim_glow"
    )

    val imageBitmap = rememberSmartProfileImageBitmap(photoSource)

    Surface(
        modifier = modifier
            .width(126.dp)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color(0xFF022C22).copy(alpha = 0.35f),
                spotColor = Color(0xFF022C22).copy(alpha = 0.45f)
            ),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF033D2E).copy(alpha = 0.62f),
        border = BorderStroke(
            width = 1.25.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = rimGlow),
                    Color.White.copy(alpha = 0.42f),
                    Color(0xFF34D399).copy(alpha = 0.72f)
                )
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Diagonal Holographic Sheen Canvas Overlay
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val sweepX = sheenProgress * w
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFFFEF08A).copy(alpha = 0.34f),
                            Color.White.copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        start = Offset(sweepX - w * 0.42f, 0f),
                        end = Offset(sweepX + w * 0.28f, h)
                    ),
                    start = Offset(sweepX - w * 0.28f, -h * 0.15f),
                    end = Offset(sweepX + w * 0.32f, h * 1.15f),
                    strokeWidth = w * 0.34f
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                // Circular Frosted Photo / Portrait Frame
                Box(
                    modifier = Modifier
                        .size(62.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.32f),
                                    Color(0xFF064E3B).copy(alpha = 0.90f)
                                )
                            )
                        )
                        .padding(3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFDE68A),
                                    Color(0xFF34D399),
                                    Color.White.copy(alpha = 0.85f),
                                    Color(0xFFFDE68A)
                                )
                            ),
                            radius = size.minDimension / 2f,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                            .clip(CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            androidx.compose.foundation.Image(
                                bitmap = imageBitmap,
                                contentDescription = contentDescription,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                filterQuality = androidx.compose.ui.graphics.FilterQuality.High,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            SmartVectorPortraitCanvas(
                                persona = persona,
                                animateCalmly = true,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Horizontal Slide+Fade Verified Identity Strip
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(0.8.dp, Color(0xFFFDE68A).copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AnimatedContent(
                            targetState = activeIndex,
                            transitionSpec = {
                                (slideInHorizontally(
                                    animationSpec = tween(420, easing = FastOutSlowInEasing)
                                ) { width -> width / 2 } + fadeIn(tween(360))) togetherWith
                                    (slideOutHorizontally(
                                        animationSpec = tween(360, easing = FastOutSlowInEasing)
                                    ) { width -> -width / 2 } + fadeOut(tween(280)))
                            },
                            label = "holographic_id_label_ticker"
                        ) { idx ->
                            Text(
                                text = labels[idx],
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFEF9C3),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

// =================================================================================================
// 3. البصمة الخاصة بقسم "الحضور والغياب والعطل الرسمية":
//    حلقة الالتزام الدائرية الحية (Live Attendance Ring)
// =================================================================================================
private data class AttendanceDialState(
    val label: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun LiveAttendanceRingHeaderWidget(
    modifier: Modifier = Modifier
) {
    val states = remember {
        listOf(
            AttendanceDialState("حضورٌ منتظم", Icons.Filled.Verified, Color(0xFF6EE7B7)),
            AttendanceDialState("بوابة ذكية", Icons.Filled.QrCodeScanner, Color(0xFFFDE68A)),
            AttendanceDialState("إجازات فورية", Icons.Filled.EventAvailable, Color(0xFFA7F3D0))
        )
    }
    var stateIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            stateIndex = (stateIndex + 1) % states.size
        }
    }

    val ringTransition = rememberInfiniteTransition(label = "live_attendance_ring")
    val orbitAngleDeg by ringTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dial_orbit_dot"
    )
    val glowPulse by ringTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dial_glow_pulse"
    )

    Box(
        modifier = modifier.size(116.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val outerR = size.minDimension * 0.45f

            // Ambient soft emerald-gold glass disc
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF043F2E).copy(alpha = 0.78f),
                        Color(0xFF065F46).copy(alpha = 0.48f),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = outerR * 1.10f
                ),
                radius = outerR * 1.10f,
                center = Offset(cx, cy)
            )

            // Frosted glass face
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = outerR * 0.90f,
                center = Offset(cx, cy)
            )

            // Subtle clock/dial calibration ticks (12 marks)
            for (i in 0 until 12) {
                val rad = (i * 30.0 - 90.0) * (PI / 180.0)
                val innerTick = outerR * if (i % 3 == 0) 0.76f else 0.81f
                val outerTick = outerR * 0.88f
                drawLine(
                    color = Color.White.copy(alpha = if (i % 3 == 0) 0.50f else 0.24f),
                    start = Offset(cx + innerTick * cos(rad).toFloat(), cy + innerTick * sin(rad).toFloat()),
                    end = Offset(cx + outerTick * cos(rad).toFloat(), cy + outerTick * sin(rad).toFloat()),
                    strokeWidth = if (i % 3 == 0) 1.5.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            // Luminous emerald + gold commitment ring
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.85f),
                        Color(0xFFFDE68A).copy(alpha = 0.92f),
                        Color(0xFF10B981).copy(alpha = 0.55f),
                        Color(0xFF34D399).copy(alpha = 0.85f)
                    ),
                    center = Offset(cx, cy)
                ),
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(width = 2.6.dp.toPx())
            )

            // Smoothly orbiting golden time marker dot
            val dotRad = (orbitAngleDeg - 90f) * (PI / 180.0)
            val dotX = cx + outerR * cos(dotRad).toFloat()
            val dotY = cy + outerR * sin(dotRad).toFloat()
            drawCircle(
                color = Color(0xFFFDE68A).copy(alpha = 0.42f * glowPulse),
                radius = 7.dp.toPx(),
                center = Offset(dotX, dotY)
            )
            drawCircle(
                color = Color(0xFFFEF08A),
                radius = 3.5.dp.toPx(),
                center = Offset(dotX, dotY)
            )
        }

        // Animated Center ScaleIn + FadeIn Icon & Label
        AnimatedContent(
            targetState = stateIndex,
            transitionSpec = {
                (scaleIn(initialScale = 0.82f, animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                    fadeIn(tween(380))) togetherWith
                    (scaleOut(targetScale = 0.86f, animationSpec = tween(300)) + fadeOut(tween(260)))
            },
            label = "attendance_dial_state"
        ) { idx ->
            val st = states[idx]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = st.accent.copy(alpha = 0.18f),
                    border = BorderStroke(0.9.dp, st.accent.copy(alpha = 0.65f)),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = st.icon,
                            contentDescription = st.label,
                            tint = st.accent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
                Text(
                    text = st.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

// =================================================================================================
// 4. البصمة الخاصة بقسم "السجل الأكاديمي والإحصائي الشامل":
//    بطاقة المؤشر التحليلي الحي (Live Spark-Chart Card)
// =================================================================================================
@Composable
fun LiveSparkChartHeaderCard(
    modifier: Modifier = Modifier
) {
    val captions = remember {
        listOf(
            "توثيق أكاديمي شامل",
            "متابعة دقيقة للدرجات",
            "تقرير سنوي متكامل"
        )
    }
    var captionIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            captionIndex = (captionIndex + 1) % captions.size
        }
    }

    val chartTransition = rememberInfiniteTransition(label = "live_spark_chart")
    val glowSweep by chartTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spark_glow_sweep"
    )
    val nodePulse by chartTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spark_node_pulse"
    )

    Surface(
        modifier = modifier
            .width(132.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF022C22).copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF033A2C).copy(alpha = 0.64f),
        border = BorderStroke(
            width = 1.1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF6EE7B7).copy(alpha = 0.65f),
                    Color(0xFFFDE68A).copy(alpha = 0.72f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                val w = size.width
                val h = size.height

                // Faint analytical guide grid lines
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
                listOf(0.25f, 0.55f, 0.85f).forEach { frac ->
                    drawLine(
                        color = Color.White.copy(alpha = 0.14f),
                        start = Offset(0f, h * frac),
                        end = Offset(w, h * frac),
                        strokeWidth = 0.9.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Growth curve points (left-to-right upward academic progression)
                val p0 = Offset(w * 0.06f, h * 0.80f)
                val p1 = Offset(w * 0.34f, h * 0.62f)
                val p2 = Offset(w * 0.64f, h * 0.44f)
                val p3 = Offset(w * 0.92f, h * 0.18f)

                val areaPath = Path().apply {
                    moveTo(p0.x, h * 0.92f)
                    lineTo(p0.x, p0.y)
                    cubicTo(w * 0.18f, h * 0.78f, w * 0.24f, h * 0.64f, p1.x, p1.y)
                    cubicTo(w * 0.46f, h * 0.60f, w * 0.52f, h * 0.48f, p2.x, p2.y)
                    cubicTo(w * 0.75f, h * 0.40f, w * 0.82f, h * 0.22f, p3.x, p3.y)
                    lineTo(p3.x, h * 0.92f)
                    close()
                }
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = 0.36f),
                            Color(0xFF047857).copy(alpha = 0.04f)
                        )
                    )
                )

                val curvePath = Path().apply {
                    moveTo(p0.x, p0.y)
                    cubicTo(w * 0.18f, h * 0.78f, w * 0.24f, h * 0.64f, p1.x, p1.y)
                    cubicTo(w * 0.46f, h * 0.60f, w * 0.52f, h * 0.48f, p2.x, p2.y)
                    cubicTo(w * 0.75f, h * 0.40f, w * 0.82f, h * 0.22f, p3.x, p3.y)
                }

                // Base glowing emerald-gold curve
                drawPath(
                    path = curvePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFF34D399),
                            Color(0xFF6EE7B7),
                            Color(0xFFFDE68A),
                            Color(0xFFF59E0B)
                        )
                    ),
                    style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Progressive glowing traveling highlight along the curve
                val nodes = listOf(p0, p1, p2, p3)
                val highlightIdx = glowSweep * (nodes.size - 1)
                nodes.forEachIndexed { idx, pt ->
                    val dist = kotlin.math.abs(highlightIdx - idx).coerceIn(0f, 1.4f)
                    val closeness = (1f - dist / 1.4f).coerceIn(0f, 1f)
                    val isPeak = idx == nodes.lastIndex
                    drawCircle(
                        color = Color(0xFFFDE68A).copy(alpha = (0.20f + 0.45f * closeness) * if (isPeak) nodePulse else 1f),
                        radius = (if (isPeak) 6.5.dp else 5.dp).toPx() * (0.85f + 0.28f * closeness),
                        center = pt
                    )
                    drawCircle(
                        color = if (isPeak || closeness > 0.55f) Color(0xFFFEF08A) else Color(0xFF6EE7B7),
                        radius = (if (isPeak) 3.2.dp else 2.5.dp).toPx(),
                        center = pt
                    )
                }
            }

            // Soft Crossfade Caption
            Crossfade(
                targetState = captionIndex,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                label = "spark_chart_caption"
            ) { idx ->
                Text(
                    text = captions[idx],
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFEF9C3),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =================================================================================================
// 5. البصمة الخاصة بقسم "الجدول والواجبات والمكتبة":
//    بطاقات المهام المتراكبة (Layered Task Deck)
// =================================================================================================
private data class TaskDeckCardSpec(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun LayeredTaskDeckHeaderWidget(
    modifier: Modifier = Modifier
) {
    val cards = remember {
        listOf(
            TaskDeckCardSpec("جدولك اليومي", "الحصص المنظمة", Icons.Filled.CalendarMonth, Color(0xFF6EE7B7)),
            TaskDeckCardSpec("واجبات وتحضير", "متابعة فورية", Icons.Filled.Assignment, Color(0xFFFDE68A)),
            TaskDeckCardSpec("مكتبة وملخصات", "مصادر معتمدة", Icons.Filled.LocalLibrary, Color(0xFFA7F3D0))
        )
    }
    var cardIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            cardIndex = (cardIndex + 1) % cards.size
        }
    }

    Box(
        modifier = modifier
            .width(134.dp)
            .height(98.dp),
        contentAlignment = Alignment.Center
    ) {
        // Back-most stacked glass card (depth layer 3)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(68.dp)
                .offset(y = (-10).dp)
                .rotate(-3.5f),
            shape = RoundedCornerShape(16.dp),
            color = Color.White.copy(alpha = 0.10f),
            border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.22f))
        ) {}

        // Middle stacked glass card (depth layer 2)
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.93f)
                .height(72.dp)
                .offset(y = (-5).dp)
                .rotate(2.2f),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF044635).copy(alpha = 0.50f),
            border = BorderStroke(0.9.dp, Color(0xFFFDE68A).copy(alpha = 0.34f))
        ) {}

        // Front animated sliding glass card (depth layer 1)
        AnimatedContent(
            targetState = cardIndex,
            transitionSpec = {
                (slideInHorizontally(
                    animationSpec = tween(480, easing = FastOutSlowInEasing)
                ) { fullW -> fullW / 2 } + fadeIn(tween(380)) + scaleIn(initialScale = 0.94f, animationSpec = tween(420))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(420, easing = FastOutSlowInEasing)
                    ) { fullW -> -fullW / 2 } + fadeOut(tween(300)) + scaleOut(targetScale = 0.92f, animationSpec = tween(320)))
            },
            label = "layered_task_deck_slide",
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 12.dp)
        ) { idx ->
            val spec = cards[idx]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0xFF022C22).copy(alpha = 0.40f)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF033B2D).copy(alpha = 0.80f),
                border = BorderStroke(
                    width = 1.15.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            spec.accent.copy(alpha = 0.85f),
                            Color.White.copy(alpha = 0.35f)
                        )
                    )
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = spec.accent.copy(alpha = 0.18f),
                        border = BorderStroke(0.8.dp, spec.accent.copy(alpha = 0.55f)),
                        modifier = Modifier.size(30.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = spec.icon,
                                contentDescription = spec.title,
                                tint = spec.accent,
                                modifier = Modifier.size(17.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = spec.title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = spec.subtitle,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.78f),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Bottom deck progress indicators
        Row(
            modifier = Modifier.align(Alignment.BottomCenter),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            cards.indices.forEach { idx ->
                val isCurrent = idx == cardIndex
                val dotW by animateDpAsState(
                    targetValue = if (isCurrent) 16.dp else 5.dp,
                    animationSpec = tween(320, easing = FastOutSlowInEasing),
                    label = "deck_indicator_$idx"
                )
                Box(
                    modifier = Modifier
                        .height(4.dp)
                        .width(dotW)
                        .clip(RoundedCornerShape(50))
                        .background(
                            if (isCurrent) Color(0xFFFDE68A) else Color.White.copy(alpha = 0.35f)
                        )
                )
            }
        }
    }
}

// =================================================================================================
// 6. البصمة الخاصة بقسم "المناهج الدراسية":
//    وحدة الكتاب المتوهّج بلمعة تقليب الصفحات الذهبية (Glowing Book Page Shimmer Widget)
// =================================================================================================
@Composable
fun GlowingCurriculumBookHeaderWidget(
    modifier: Modifier = Modifier
) {
    val phrases = remember {
        listOf(
            "مناهج وزارية معتمدة",
            "تصفّح فوري وسريع",
            "مكتبة صفّك الذكية"
        )
    }
    var phraseIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            phraseIdx = (phraseIdx + 1) % phrases.size
        }
    }

    val bookTransition = rememberInfiniteTransition(label = "glowing_book_shimmer")
    val pageFlip by bookTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_page_flip"
    )
    val shimmerSweep by bookTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "book_golden_shimmer"
    )

    Surface(
        modifier = modifier
            .width(132.dp)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF022C22).copy(alpha = 0.35f)
            ),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF033B2D).copy(alpha = 0.62f),
        border = BorderStroke(
            width = 1.15.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFFDE68A).copy(alpha = 0.72f),
                    Color(0xFF34D399).copy(alpha = 0.65f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 9.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
            ) {
                val w = size.width
                val h = size.height
                val cx = w * 0.50f
                val baseY = h * 0.78f

                // Soft warm golden light behind the open book
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = 0.34f),
                            Color(0xFF34D399).copy(alpha = 0.16f),
                            Color.Transparent
                        ),
                        center = Offset(cx, h * 0.44f),
                        radius = w * 0.44f
                    ),
                    radius = w * 0.44f,
                    center = Offset(cx, h * 0.44f)
                )

                // Golden hardcover base
                val basePath = Path().apply {
                    moveTo(w * 0.10f, baseY)
                    quadraticBezierTo(w * 0.30f, baseY - h * 0.08f, cx, baseY + h * 0.06f)
                    quadraticBezierTo(w * 0.70f, baseY - h * 0.08f, w * 0.90f, baseY)
                    lineTo(w * 0.87f, baseY + h * 0.10f)
                    quadraticBezierTo(w * 0.70f, baseY + h * 0.02f, cx, baseY + h * 0.14f)
                    quadraticBezierTo(w * 0.30f, baseY + h * 0.02f, w * 0.13f, baseY + h * 0.10f)
                    close()
                }
                drawPath(
                    path = basePath,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFFD97706), Color(0xFFFDE68A), Color(0xFFD97706))
                    )
                )

                // Left & right crisp book pages
                val leftPage = Path().apply {
                    moveTo(cx, baseY + h * 0.04f)
                    quadraticBezierTo(w * 0.30f, baseY - h * 0.10f, w * 0.14f, baseY - h * 0.02f)
                    lineTo(w * 0.18f, h * 0.24f)
                    quadraticBezierTo(w * 0.34f, h * 0.18f, cx, h * 0.34f)
                    close()
                }
                val rightPage = Path().apply {
                    moveTo(cx, baseY + h * 0.04f)
                    quadraticBezierTo(w * 0.70f, baseY - h * 0.10f, w * 0.86f, baseY - h * 0.02f)
                    lineTo(w * 0.82f, h * 0.24f)
                    quadraticBezierTo(w * 0.66f, h * 0.18f, cx, h * 0.34f)
                    close()
                }
                drawPath(path = leftPage, color = Color.White.copy(alpha = 0.94f))
                drawPath(path = rightPage, color = Color(0xFFECFDF5))
                drawPath(path = leftPage, color = Color(0xFF059669).copy(alpha = 0.35f), style = Stroke(width = 1.dp.toPx()))
                drawPath(path = rightPage, color = Color(0xFF059669).copy(alpha = 0.35f), style = Stroke(width = 1.dp.toPx()))

                // Turning translucent golden-shimmering page arc
                val flipAngle = pageFlip * PI.toFloat()
                val tipX = cx + cos(flipAngle) * (w * 0.34f)
                val tipY = h * 0.28f - sin(flipAngle) * (h * 0.20f)
                val turningPage = Path().apply {
                    moveTo(cx, baseY + h * 0.03f)
                    lineTo(cx, h * 0.34f)
                    quadraticBezierTo((cx + tipX) * 0.5f, tipY - h * 0.08f, tipX, tipY)
                    lineTo(cx + cos(flipAngle) * (w * 0.32f), baseY - h * 0.04f - sin(flipAngle) * h * 0.08f)
                    close()
                }
                drawPath(
                    path = turningPage,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFEF9C3).copy(alpha = 0.86f),
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFFDE68A).copy(alpha = 0.72f)
                        ),
                        start = Offset(w * shimmerSweep - w * 0.2f, 0f),
                        end = Offset(w * shimmerSweep + w * 0.2f, h)
                    )
                )
                drawPath(
                    path = turningPage,
                    color = Color(0xFFF59E0B).copy(alpha = 0.55f),
                    style = Stroke(width = 1.1.dp.toPx(), join = StrokeJoin.Round)
                )

                // Spine line
                drawLine(
                    color = Color(0xFFD97706),
                    start = Offset(cx, h * 0.34f),
                    end = Offset(cx, baseY + h * 0.06f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Subtle golden shimmer dots above pages
                val shimmerAlpha = (sin(pageFlip * PI).toFloat()).coerceIn(0.25f, 1f)
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = shimmerAlpha),
                    radius = 2.4.dp.toPx(),
                    center = Offset(cx - w * 0.14f, h * 0.12f)
                )
                drawCircle(
                    color = Color(0xFF6EE7B7).copy(alpha = 0.85f * shimmerAlpha),
                    radius = 2.dp.toPx(),
                    center = Offset(cx + w * 0.16f, h * 0.10f)
                )
            }

            // Gentle Horizontal Reveal Phrase Strip
            AnimatedContent(
                targetState = phraseIdx,
                transitionSpec = {
                    (slideInHorizontally(
                        animationSpec = tween(440, easing = FastOutSlowInEasing)
                    ) { w -> w / 3 } + fadeIn(tween(380))) togetherWith
                        (slideOutHorizontally(
                            animationSpec = tween(360, easing = FastOutSlowInEasing)
                        ) { w -> -w / 3 } + fadeOut(tween(260)))
                },
                label = "curriculum_book_phrase"
            ) { idx ->
                Text(
                    text = phrases[idx],
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFEF9C3),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
