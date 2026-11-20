package com.example.reading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * الهوية اللونية الطفولية المشرقة لقسم "واحة القراءة الذكية".
 */
object ReadingOasisColors {
    val SkySoft = Color(0xFFECF8FF)
    val SkyHorizon = Color(0xFFCDEEFF)
    val SkyBright = Color(0xFF38BDF8)
    val SunnyYellow = Color(0xFFFFC83D)
    val SunnyShadow = Color(0xFFD99A00)
    val Coral = Color(0xFFFF7A59)
    val CoralShadow = Color(0xFFD65231)
    val Mint = Color(0xFF22C55E)
    val MintShadow = Color(0xFF159445)
    val Purple = Color(0xFF8B5CF6)
    val PurpleShadow = Color(0xFF6634D9)
    val Pink = Color(0xFFEC4899)
    val PinkShadow = Color(0xFFBE2371)
    val RoyalBlue = Color(0xFF2563EB)
    val RoyalBlueShadow = Color(0xFF1A46B0)
    val White = Color(0xFFFFFFFF)
    val CreamPaper = Color(0xFFFFFDF7)
    val InkDark = Color(0xFF1E293B)
    val InkMuted = Color(0xFF475569)
    val StarGoldLight = Color(0xFFFFE57F)
    val StarGold = Color(0xFFFFC83D)
    val StarGoldDeep = Color(0xFFE68A00)
    val StarEmptyFill = Color(0xFFE2EDF7)
    val StarEmptyStroke = Color(0xFFBDD2E6)

    fun darken(color: Color, factor: Float = 0.74f): Color = Color(
        red = (color.red * factor).coerceIn(0f, 1f),
        green = (color.green * factor).coerceIn(0f, 1f),
        blue = (color.blue * factor).coerceIn(0f, 1f),
        alpha = color.alpha
    )
}

/**
 * زر طفولي بارز وتفاعلي بحواف دائرية كبيرة (22.dp) وظل سفلي سميك ثابت (5.dp)
 * ينضغط للأسفل بارتداد مرن عند اللمس.
 */
@Composable
fun PlayfulBouncyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ReadingOasisColors.SunnyYellow,
    shadowColor: Color = ReadingOasisColors.darken(containerColor, 0.74f),
    contentColor: Color = ReadingOasisColors.White,
    cornerRadius: Dp = 22.dp,
    shadowDepth: Dp = 5.dp,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) shadowDepth - 1.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playful_btn_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playful_btn_scale"
    )

    val effectiveFill = if (enabled) containerColor else containerColor.copy(alpha = 0.45f)
    val effectiveShadow = if (enabled) shadowColor else shadowColor.copy(alpha = 0.35f)
    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .defaultMinSize(minWidth = 48.dp, minHeight = 52.dp)
            .padding(bottom = shadowDepth)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        // الظل السفلي الثابت السميك لإعطاء إحساس ملموس بالعمق
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = shadowDepth)
                .clip(shape)
                .background(effectiveShadow)
        )

        // الوجه العلوي القابل للانضغاط بارتداد مرن
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(y = pressOffset)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            effectiveFill.copy(alpha = 0.96f),
                            effectiveFill
                        )
                    )
                )
                .border(
                    width = 1.5.dp,
                    color = Color.White.copy(alpha = 0.38f),
                    shape = shape
                ),
            contentAlignment = Alignment.Center
        ) {
            // لمعة طفولية ناعمة في أعلى الزر
            Canvas(modifier = Modifier.matchParentSize()) {
                val sheenPath = Path().apply {
                    moveTo(size.width * 0.14f, size.height * 0.16f)
                    quadraticBezierTo(
                        size.width * 0.50f,
                        size.height * 0.04f,
                        size.width * 0.86f,
                        size.height * 0.16f
                    )
                }
                drawPath(
                    path = sheenPath,
                    color = Color.White.copy(alpha = 0.32f),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            CompositionLocalProvider(LocalContentColor provides contentColor) {
                Row(
                    modifier = Modifier.padding(contentPadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                    content = content
                )
            }
        }
    }
}

/**
 * شخصية رفيق القراءة المبتسم "فصيح" (بومة قارئة صغيرة بألوان سماوية وذهبية).
 * مرسومة بالكامل بمنحنيات ناعمة مع عينين ترمشان وجناحين يتحركان وحركة قفز فرحة عند الاحتفال.
 */
@Composable
fun ReadingMascotCanvas(
    modifier: Modifier = Modifier,
    isCelebrating: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mascot_life")

    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isCelebrating) 360 else 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )

    val wingFlap by infiniteTransition.animateFloat(
        initialValue = if (isCelebrating) -28f else -8f,
        targetValue = if (isCelebrating) 18f else 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isCelebrating) 240 else 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_wings"
    )

    val blinkCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_blink"
    )

    val celebrationJump by animateFloatAsState(
        targetValue = if (isCelebrating) -16f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mascot_jump"
    )

    val eyeOpenRatio = when {
        isCelebrating -> 1f
        blinkCycle in 0.91f..0.95f -> 0.12f
        else -> 1f
    }

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "رفيق القراءة المبتسم فصيح"
        }
    ) {
        val w = size.width
        val h = size.height
        val dy = (floatOffset + celebrationJump) * (h / 140f)
        val cx = w * 0.50f
        val cy = h * 0.54f + dy

        // ظل ناعم أسفل الشخصية يتقلص عند القفز
        val shadowScale = (1f - ((-dy) / (h * 0.30f)).coerceIn(0f, 0.35f))
        drawOval(
            color = Color(0xFF0EA5E9).copy(alpha = 0.16f),
            topLeft = Offset(cx - w * 0.28f * shadowScale, h * 0.88f),
            size = Size(w * 0.56f * shadowScale, h * 0.09f)
        )

        // الجناح الأيمن
        rotate(degrees = wingFlap, pivot = Offset(cx + w * 0.25f, cy - h * 0.02f)) {
            val rightWing = Path().apply {
                moveTo(cx + w * 0.22f, cy - h * 0.06f)
                cubicTo(
                    cx + w * 0.45f, cy - h * 0.12f,
                    cx + w * 0.47f, cy + h * 0.18f,
                    cx + w * 0.26f, cy + h * 0.22f
                )
                quadraticBezierTo(
                    cx + w * 0.18f, cy + h * 0.12f,
                    cx + w * 0.22f, cy - h * 0.06f
                )
                close()
            }
            drawPath(
                path = rightWing,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    center = Offset(cx + w * 0.34f, cy + h * 0.04f),
                    radius = w * 0.28f
                )
            )
        }

        // الجناح الأيسر
        rotate(degrees = -wingFlap, pivot = Offset(cx - w * 0.25f, cy - h * 0.02f)) {
            val leftWing = Path().apply {
                moveTo(cx - w * 0.22f, cy - h * 0.06f)
                cubicTo(
                    cx - w * 0.45f, cy - h * 0.12f,
                    cx - w * 0.47f, cy + h * 0.18f,
                    cx - w * 0.26f, cy + h * 0.22f
                )
                quadraticBezierTo(
                    cx - w * 0.18f, cy + h * 0.12f,
                    cx - w * 0.22f, cy - h * 0.06f
                )
                close()
            }
            drawPath(
                path = leftWing,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7)),
                    center = Offset(cx - w * 0.34f, cy + h * 0.04f),
                    radius = w * 0.28f
                )
            )
        }

        // القدمان الذهبيتان
        val leftFoot = Path().apply {
            moveTo(cx - w * 0.15f, cy + h * 0.28f)
            quadraticBezierTo(cx - w * 0.18f, cy + h * 0.36f, cx - w * 0.10f, cy + h * 0.35f)
            quadraticBezierTo(cx - w * 0.05f, cy + h * 0.36f, cx - w * 0.06f, cy + h * 0.28f)
            close()
        }
        val rightFoot = Path().apply {
            moveTo(cx + w * 0.15f, cy + h * 0.28f)
            quadraticBezierTo(cx + w * 0.18f, cy + h * 0.36f, cx + w * 0.10f, cy + h * 0.35f)
            quadraticBezierTo(cx + w * 0.05f, cy + h * 0.36f, cx + w * 0.06f, cy + h * 0.28f)
            close()
        }
        drawPath(leftFoot, color = ReadingOasisColors.Coral)
        drawPath(rightFoot, color = ReadingOasisColors.Coral)

        // الجسم الدائري اللطيف مع أذني البومة المنحنيتين
        val bodyPath = Path().apply {
            moveTo(cx, cy - h * 0.31f)
            // الأذن اليمنى
            cubicTo(
                cx + w * 0.10f, cy - h * 0.33f,
                cx + w * 0.20f, cy - h * 0.42f,
                cx + w * 0.24f, cy - h * 0.34f
            )
            // الجانب الأيمن من الجسم
            cubicTo(
                cx + w * 0.33f, cy - h * 0.14f,
                cx + w * 0.34f, cy + h * 0.25f,
                cx, cy + h * 0.31f
            )
            // الجانب الأيسر من الجسم
            cubicTo(
                cx - w * 0.34f, cy + h * 0.25f,
                cx - w * 0.33f, cy - h * 0.14f,
                cx - w * 0.24f, cy - h * 0.34f
            )
            // الأذن اليسرى
            cubicTo(
                cx - w * 0.20f, cy - h * 0.42f,
                cx - w * 0.10f, cy - h * 0.33f,
                cx, cy - h * 0.31f
            )
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7DD3FC), Color(0xFF0EA5E9), Color(0xFF0369A1)),
                center = Offset(cx - w * 0.08f, cy - h * 0.16f),
                radius = w * 0.56f
            )
        )

        // البطن الذهبي الدافئ
        val bellyPath = Path().apply {
            moveTo(cx, cy + h * 0.01f)
            cubicTo(
                cx + w * 0.22f, cy + h * 0.01f,
                cx + w * 0.23f, cy + h * 0.27f,
                cx, cy + h * 0.28f
            )
            cubicTo(
                cx - w * 0.23f, cy + h * 0.27f,
                cx - w * 0.22f, cy + h * 0.01f,
                cx, cy + h * 0.01f
            )
            close()
        }
        drawPath(
            path = bellyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFFCD34D)),
                center = Offset(cx, cy + h * 0.12f),
                radius = w * 0.28f
            )
        )

        // ريشات لطيفة على البطن
        val featherStroke = Stroke(width = w * 0.018f, cap = StrokeCap.Round)
        for (i in -1..1) {
            val fx = cx + i * w * 0.085f
            val fy = cy + h * 0.14f + (if (i == 0) h * 0.02f else 0f)
            val feather = Path().apply {
                moveTo(fx - w * 0.03f, fy)
                quadraticBezierTo(fx, fy + h * 0.035f, fx + w * 0.03f, fy)
            }
            drawPath(feather, color = Color(0xFFE59B00).copy(alpha = 0.45f), style = featherStroke)
        }

        // هالة الوجه البيضاء حول العينين
        drawCircle(
            color = Color(0xFFF0F9FF),
            radius = w * 0.145f,
            center = Offset(cx - w * 0.115f, cy - h * 0.09f)
        )
        drawCircle(
            color = Color(0xFFF0F9FF),
            radius = w * 0.145f,
            center = Offset(cx + w * 0.115f, cy - h * 0.09f)
        )

        // العينان (مبتسمتان عند الاحتفال أو ترمشان بسلاسة)
        val leftEyeCenter = Offset(cx - w * 0.115f, cy - h * 0.09f)
        val rightEyeCenter = Offset(cx + w * 0.115f, cy - h * 0.09f)

        if (isCelebrating) {
            val happyEyeStroke = Stroke(width = w * 0.035f, cap = StrokeCap.Round)
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                val arc = Path().apply {
                    moveTo(center.x - w * 0.065f, center.y + h * 0.015f)
                    quadraticBezierTo(
                        center.x,
                        center.y - h * 0.065f,
                        center.x + w * 0.065f,
                        center.y + h * 0.015f
                    )
                }
                drawPath(arc, color = ReadingOasisColors.InkDark, style = happyEyeStroke)
            }
        } else {
            val eyeH = (w * 0.19f * eyeOpenRatio).coerceAtLeast(w * 0.025f)
            val eyeW = w * 0.18f
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawOval(
                    color = Color.White,
                    topLeft = Offset(center.x - eyeW / 2f, center.y - eyeH / 2f),
                    size = Size(eyeW, eyeH)
                )
                if (eyeOpenRatio > 0.35f) {
                    drawCircle(
                        color = ReadingOasisColors.InkDark,
                        radius = w * 0.056f,
                        center = Offset(center.x, center.y + h * 0.005f)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = w * 0.020f,
                        center = Offset(center.x - w * 0.02f, center.y - h * 0.018f)
                    )
                }
            }
        }

        // خدود وردية مبهجة
        drawOval(
            color = ReadingOasisColors.Pink.copy(alpha = if (isCelebrating) 0.45f else 0.28f),
            topLeft = Offset(cx - w * 0.25f, cy - h * 0.01f),
            size = Size(w * 0.09f, h * 0.048f)
        )
        drawOval(
            color = ReadingOasisColors.Pink.copy(alpha = if (isCelebrating) 0.45f else 0.28f),
            topLeft = Offset(cx + w * 0.16f, cy - h * 0.01f),
            size = Size(w * 0.09f, h * 0.048f)
        )

        // المنقار الذهبي المنحني المبتسم
        val beakPath = Path().apply {
            moveTo(cx - w * 0.06f, cy - h * 0.025f)
            quadraticBezierTo(cx, cy - h * 0.065f, cx + w * 0.06f, cy - h * 0.025f)
            quadraticBezierTo(cx, cy + h * 0.065f, cx - w * 0.06f, cy - h * 0.025f)
            close()
        }
        drawPath(
            path = beakPath,
            brush = Brush.verticalGradient(
                colors = listOf(ReadingOasisColors.SunnyYellow, ReadingOasisColors.Coral)
            )
        )

        // قبعة/تاج صغير ذهبي لطيف على الرأس
        val miniCrown = Path().apply {
            moveTo(cx - w * 0.11f, cy - h * 0.29f)
            quadraticBezierTo(cx - w * 0.12f, cy - h * 0.40f, cx - w * 0.05f, cy - h * 0.35f)
            quadraticBezierTo(cx, cy - h * 0.44f, cx + w * 0.05f, cy - h * 0.35f)
            quadraticBezierTo(cx + w * 0.12f, cy - h * 0.40f, cx + w * 0.11f, cy - h * 0.29f)
            close()
        }
        drawPath(
            path = miniCrown,
            brush = Brush.linearGradient(
                colors = listOf(ReadingOasisColors.StarGoldLight, ReadingOasisColors.SunnyYellow, ReadingOasisColors.StarGoldDeep)
            )
        )
    }
}

/**
 * نجمة ذهبية ممتلئة أو مفرغة بحواف منحنية ناعمة.
 */
@Composable
fun PlayfulStarCanvas(
    filled: Boolean,
    modifier: Modifier = Modifier,
    glowing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "star_glow")
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star_pulse"
    )

    Canvas(
        modifier = modifier.semantics {
            contentDescription = if (filled) "نجمة ذهبية مكتسبة" else "نجمة بانتظار الإنجاز"
        }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerR = minOf(size.width, size.height) * 0.44f
        val innerR = outerR * 0.48f

        if (filled && glowing) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ReadingOasisColors.SunnyYellow.copy(alpha = 0.42f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = outerR * 1.25f * glowPulse
                ),
                radius = outerR * 1.25f * glowPulse,
                center = center
            )
        }

        val starPath = buildSmoothStarPath(center, outerR, innerR)

        if (filled) {
            drawPath(
                path = starPath,
                brush = Brush.radialGradient(
                    colors = listOf(
                        ReadingOasisColors.StarGoldLight,
                        ReadingOasisColors.StarGold,
                        ReadingOasisColors.StarGoldDeep
                    ),
                    center = Offset(center.x - outerR * 0.2f, center.y - outerR * 0.2f),
                    radius = outerR * 1.3f
                )
            )
            drawPath(
                path = starPath,
                color = ReadingOasisColors.StarGoldDeep,
                style = Stroke(width = outerR * 0.11f, join = StrokeJoin.Round)
            )
            // لمعة داخلية صغيرة
            drawCircle(
                color = Color.White.copy(alpha = 0.65f),
                radius = outerR * 0.12f,
                center = Offset(center.x - outerR * 0.16f, center.y - outerR * 0.18f)
            )
        } else {
            drawPath(path = starPath, color = ReadingOasisColors.StarEmptyFill)
            drawPath(
                path = starPath,
                color = ReadingOasisColors.StarEmptyStroke,
                style = Stroke(width = outerR * 0.12f, join = StrokeJoin.Round)
            )
        }
    }
}

private fun buildSmoothStarPath(center: Offset, outerR: Float, innerR: Float): Path {
    val points = mutableListOf<Offset>()
    for (i in 0 until 10) {
        val angle = (-90.0 + i * 36.0) * PI / 180.0
        val r = if (i % 2 == 0) outerR else innerR
        points.add(
            Offset(
                x = center.x + (cos(angle) * r).toFloat(),
                y = center.y + (sin(angle) * r).toFloat()
            )
        )
    }
    val path = Path()
    for (i in points.indices) {
        val prev = points[(i - 1 + points.size) % points.size]
        val curr = points[i]
        val next = points[(i + 1) % points.size]
        val midStart = Offset((prev.x + curr.x) / 2f, (prev.y + curr.y) / 2f)
        val midEnd = Offset((curr.x + next.x) / 2f, (curr.y + next.y) / 2f)
        if (i == 0) {
            path.moveTo(midStart.x, midStart.y)
        }
        path.quadraticBezierTo(curr.x, curr.y, midEnd.x, midEnd.y)
    }
    path.close()
    return path
}

/**
 * مكبر صوت كرتوني مع موجات صوتية نابضة.
 */
@Composable
fun PlayfulSpeakerCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White,
    isPulsing: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "speaker_waves")
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaker_wave_alpha"
    )

    Canvas(
        modifier = modifier.semantics {
            contentDescription = "استماع للصوت"
        }
    ) {
        val w = size.width
        val h = size.height

        // جسم السماعة المنحني
        val hornPath = Path().apply {
            moveTo(w * 0.14f, h * 0.38f)
            lineTo(w * 0.32f, h * 0.38f)
            quadraticBezierTo(w * 0.48f, h * 0.22f, w * 0.56f, h * 0.20f)
            cubicTo(w * 0.60f, h * 0.20f, w * 0.60f, h * 0.80f, w * 0.56f, h * 0.80f)
            quadraticBezierTo(w * 0.48f, h * 0.78f, w * 0.32f, h * 0.62f)
            lineTo(w * 0.14f, h * 0.62f)
            quadraticBezierTo(w * 0.08f, h * 0.50f, w * 0.14f, h * 0.38f)
            close()
        }
        drawPath(hornPath, color = color)

        val strokeW = w * 0.085f
        val wave1Alpha = if (isPulsing) waveAlpha else 0.88f
        val wave2Alpha = if (isPulsing) (1.25f - waveAlpha).coerceIn(0.35f, 1f) else 0.65f

        val wave1 = Path().apply {
            moveTo(w * 0.68f, h * 0.35f)
            quadraticBezierTo(w * 0.80f, h * 0.50f, w * 0.68f, h * 0.65f)
        }
        drawPath(
            path = wave1,
            color = color.copy(alpha = wave1Alpha),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        val wave2 = Path().apply {
            moveTo(w * 0.78f, h * 0.24f)
            quadraticBezierTo(w * 0.96f, h * 0.50f, w * 0.78f, h * 0.76f)
        }
        drawPath(
            path = wave2,
            color = color.copy(alpha = wave2Alpha),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

/**
 * قلم تلوين طفولي مرسوم بـ Canvas.
 */
@Composable
fun PlayfulCrayonCanvas(
    color: Color,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "قلم تلوين"
        }
    ) {
        val w = size.width
        val h = size.height
        rotate(degrees = -35f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            val bodyLeft = w * 0.35f
            val bodyRight = w * 0.65f
            val tipY = h * 0.12f
            val neckY = h * 0.34f
            val bottomY = h * 0.88f

            // رأس القلم المنحني
            val tipPath = Path().apply {
                moveTo(w * 0.50f, tipY)
                quadraticBezierTo(bodyRight, neckY - h * 0.04f, bodyRight, neckY)
                lineTo(bodyLeft, neckY)
                quadraticBezierTo(bodyLeft, neckY - h * 0.04f, w * 0.50f, tipY)
                close()
            }
            drawPath(tipPath, color = ReadingOasisColors.darken(color, 0.85f))

            // جسم القلم
            drawRoundRect(
                color = color,
                topLeft = Offset(bodyLeft, neckY),
                size = Size(bodyRight - bodyLeft, bottomY - neckY),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )

            // شريط الورق الأبيض المزخرف
            drawRect(
                color = Color.White.copy(alpha = 0.82f),
                topLeft = Offset(bodyLeft, h * 0.44f),
                size = Size(bodyRight - bodyLeft, h * 0.28f)
            )

            // خط موجة طفولي داخل الشريط
            val wave = Path().apply {
                moveTo(bodyLeft + w * 0.04f, h * 0.58f)
                quadraticBezierTo(w * 0.50f, h * 0.50f, bodyRight - w * 0.04f, h * 0.58f)
            }
            drawPath(wave, color = color, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
        }
    }
}

/**
 * تاج بطل ذهبي بلمسة جواهر ملونة.
 */
@Composable
fun PlayfulCrownCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.SunnyYellow
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "تاج البطل"
        }
    ) {
        val w = size.width
        val h = size.height
        val crownPath = Path().apply {
            moveTo(w * 0.14f, h * 0.76f)
            cubicTo(w * 0.10f, h * 0.54f, w * 0.12f, h * 0.34f, w * 0.16f, h * 0.32f)
            quadraticBezierTo(w * 0.28f, h * 0.50f, w * 0.34f, h * 0.50f)
            quadraticBezierTo(w * 0.44f, h * 0.22f, w * 0.50f, h * 0.18f)
            quadraticBezierTo(w * 0.56f, h * 0.22f, w * 0.66f, h * 0.50f)
            quadraticBezierTo(w * 0.72f, h * 0.50f, w * 0.84f, h * 0.32f)
            cubicTo(w * 0.88f, h * 0.34f, w * 0.90f, h * 0.54f, w * 0.86f, h * 0.76f)
            quadraticBezierTo(w * 0.50f, h * 0.84f, w * 0.14f, h * 0.76f)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF1A8), color, ReadingOasisColors.darken(color, 0.82f))
            )
        )
        // جواهر صغيرة على القمم
        drawCircle(color = ReadingOasisColors.Pink, radius = w * 0.06f, center = Offset(w * 0.50f, h * 0.18f))
        drawCircle(color = ReadingOasisColors.SkyBright, radius = w * 0.05f, center = Offset(w * 0.16f, h * 0.31f))
        drawCircle(color = ReadingOasisColors.Mint, radius = w * 0.05f, center = Offset(w * 0.84f, h * 0.31f))
    }
}

/**
 * سهم رجوع ناعم مناسب لاتجاه العربية (يشير نحو اليمين).
 */
@Composable
fun PlayfulBackArrowCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "رجوع"
        }
    ) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arrow = Path().apply {
            moveTo(w * 0.22f, h * 0.50f)
            quadraticBezierTo(w * 0.52f, h * 0.50f, w * 0.78f, h * 0.50f)
            moveTo(w * 0.54f, h * 0.24f)
            quadraticBezierTo(w * 0.68f, h * 0.38f, w * 0.78f, h * 0.50f)
            quadraticBezierTo(w * 0.68f, h * 0.62f, w * 0.54f, h * 0.76f)
        }
        drawPath(arrow, color = color, style = stroke)
    }
}

/**
 * أيقونة كتاب مفتوح ناعمة لتبويب "تعلّم".
 */
@Composable
fun PlayfulBookIconCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White
) {
    Canvas(modifier = modifier.semantics { contentDescription = "تعلّم" }) {
        val w = size.width
        val h = size.height
        val bookPath = Path().apply {
            moveTo(w * 0.50f, h * 0.30f)
            cubicTo(w * 0.38f, h * 0.20f, w * 0.18f, h * 0.22f, w * 0.12f, h * 0.30f)
            lineTo(w * 0.12f, h * 0.76f)
            cubicTo(w * 0.22f, h * 0.70f, w * 0.40f, h * 0.70f, w * 0.50f, h * 0.80f)
            cubicTo(w * 0.60f, h * 0.70f, w * 0.78f, h * 0.70f, w * 0.88f, h * 0.76f)
            lineTo(w * 0.88f, h * 0.30f)
            cubicTo(w * 0.82f, h * 0.22f, w * 0.62f, h * 0.20f, w * 0.50f, h * 0.30f)
            close()
        }
        drawPath(bookPath, color = color.copy(alpha = 0.28f))
        drawPath(
            bookPath,
            color = color,
            style = Stroke(width = w * 0.085f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.30f),
            end = Offset(w * 0.50f, h * 0.79f),
            strokeWidth = w * 0.08f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * أيقونة قطع تركيب لطيفة لتبويب "ركّب".
 */
@Composable
fun PlayfulPuzzleIconCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White
) {
    Canvas(modifier = modifier.semantics { contentDescription = "ركّب" }) {
        val w = size.width
        val h = size.height
        val piece = Path().apply {
            moveTo(w * 0.20f, h * 0.28f)
            lineTo(w * 0.42f, h * 0.28f)
            cubicTo(w * 0.42f, h * 0.12f, w * 0.60f, h * 0.12f, w * 0.60f, h * 0.28f)
            lineTo(w * 0.74f, h * 0.28f)
            lineTo(w * 0.74f, h * 0.46f)
            cubicTo(w * 0.90f, h * 0.46f, w * 0.90f, h * 0.64f, w * 0.74f, h * 0.64f)
            lineTo(w * 0.74f, h * 0.82f)
            lineTo(w * 0.20f, h * 0.82f)
            close()
        }
        drawPath(piece, color = color)
    }
}

/**
 * أيقونة ميكروفون طفولية نابضة لزر "اقرأها بصوتك".
 */
@Composable
fun PlayfulMicCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White,
    isListening: Boolean = false
) {
    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val ringScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(650, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mic_ring_scale"
    )

    Canvas(modifier = modifier.semantics { contentDescription = "اقرأ بصوتك" }) {
        val w = size.width
        val h = size.height
        if (isListening) {
            drawCircle(
                color = color.copy(alpha = 0.28f),
                radius = w * 0.44f * ringScale,
                center = Offset(w * 0.50f, h * 0.42f)
            )
        }
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.35f, h * 0.16f),
            size = Size(w * 0.30f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.15f, w * 0.15f)
        )
        val cradle = Path().apply {
            moveTo(w * 0.24f, h * 0.46f)
            cubicTo(w * 0.24f, h * 0.74f, w * 0.76f, h * 0.74f, w * 0.76f, h * 0.46f)
        }
        drawPath(cradle, color = color, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))
        drawLine(
            color = color,
            start = Offset(w * 0.50f, h * 0.68f),
            end = Offset(w * 0.50f, h * 0.86f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
        drawLine(
            color = color,
            start = Offset(w * 0.34f, h * 0.86f),
            end = Offset(w * 0.66f, h * 0.86f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * أيقونة ممحاة مدرسية لطيفة لزر "مسح".
 */
@Composable
fun PlayfulEraserCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White
) {
    Canvas(modifier = modifier.semantics { contentDescription = "مسح السبورة" }) {
        val w = size.width
        val h = size.height
        rotate(degrees = -28f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawRoundRect(
                color = color,
                topLeft = Offset(w * 0.20f, h * 0.30f),
                size = Size(w * 0.60f, h * 0.40f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
            )
            drawRoundRect(
                color = color.copy(alpha = 0.45f),
                topLeft = Offset(w * 0.54f, h * 0.30f),
                size = Size(w * 0.26f, h * 0.40f),
                cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
            )
        }
    }
}

/**
 * أيقونة علامة صح ناعمة ومنحنية.
 */
@Composable
fun PlayfulCheckCanvas(
    modifier: Modifier = Modifier,
    color: Color = ReadingOasisColors.White
) {
    Canvas(modifier = modifier.semantics { contentDescription = "تم بنجاح" }) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.20f, h * 0.54f)
            quadraticBezierTo(w * 0.34f, h * 0.66f, w * 0.43f, h * 0.74f)
            quadraticBezierTo(w * 0.62f, h * 0.50f, w * 0.82f, h * 0.26f)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * حلقات تموّج صوتية تنبض حول بطاقة الحرف عند النطق.
 */
@Composable
fun SoundRippleRingsCanvas(
    isActive: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "sound_ripples")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_phase"
    )

    Canvas(modifier = modifier) {
        if (!isActive) return@Canvas
        val center = Offset(size.width / 2f, size.height / 2f)
        val maxR = minOf(size.width, size.height) * 0.49f
        for (ring in 0 until 3) {
            val p = (phase + ring * 0.33f) % 1f
            val radius = maxR * (0.45f + p * 0.55f)
            val alpha = (1f - p).coerceIn(0f, 1f) * 0.55f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = center,
                style = Stroke(width = 4.dp.toPx())
            )
        }
    }
}

/**
 * رسوم مصغّرة معبّرة بمنحنيات ناعمة وتدرجات لونية لكل درس من الدروس الثمانية.
 */
@Composable
fun LessonIllustrationCanvas(
    lessonId: String,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier.semantics {
            contentDescription = "رسمة توضيحية للدرس"
        }
    ) {
        when (lessonId) {
            "lesson_01_dada" -> drawDadaBabyRattle()
            "lesson_02_dar" -> drawCozyHouse()
            "lesson_03_door" -> drawTwoHouses()
            "lesson_04_baba" -> drawColorfulDoor()
            "lesson_05_mama" -> drawSmilingFlower()
            "lesson_06_noor_noori" -> drawRadiantSunLamp()
            "lesson_07_booq_nadeem" -> drawGoldenTrumpet()
            "lesson_08_waz" -> drawCuteGooseOnWater()
            else -> drawCozyHouse()
        }
    }
}

private fun DrawScope.drawDadaBabyRattle() {
    val w = size.width
    val h = size.height
    // قلب دافئ مبتسم يرمز للطفل "دادا"
    val heart = Path().apply {
        moveTo(w * 0.50f, h * 0.80f)
        cubicTo(w * 0.14f, h * 0.56f, w * 0.14f, h * 0.22f, w * 0.36f, h * 0.24f)
        cubicTo(w * 0.44f, h * 0.24f, w * 0.48f, h * 0.32f, w * 0.50f, h * 0.36f)
        cubicTo(w * 0.52f, h * 0.32f, w * 0.56f, h * 0.24f, w * 0.64f, h * 0.24f)
        cubicTo(w * 0.86f, h * 0.22f, w * 0.86f, h * 0.56f, w * 0.50f, h * 0.80f)
        close()
    }
    drawPath(
        path = heart,
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFF9EB8), ReadingOasisColors.Pink),
            center = Offset(w * 0.42f, h * 0.38f),
            radius = w * 0.46f
        )
    )
    // عيون مبتسمة لطيفة
    drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.40f, h * 0.46f))
    drawCircle(Color.White, radius = w * 0.045f, center = Offset(w * 0.60f, h * 0.46f))
    val smile = Path().apply {
        moveTo(w * 0.42f, h * 0.57f)
        quadraticBezierTo(w * 0.50f, h * 0.65f, w * 0.58f, h * 0.57f)
    }
    drawPath(smile, color = Color.White, style = Stroke(width = w * 0.045f, cap = StrokeCap.Round))
}

private fun DrawScope.drawCozyHouse() {
    val w = size.width
    val h = size.height
    // جدران البيت الدافئة
    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFF8DC), Color(0xFFFDE68A))),
        topLeft = Offset(w * 0.22f, h * 0.44f),
        size = Size(w * 0.56f, h * 0.42f),
        cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
    )
    // سقف منحني مرجاني
    val roof = Path().apply {
        moveTo(w * 0.14f, h * 0.48f)
        quadraticBezierTo(w * 0.50f, h * 0.12f, w * 0.86f, h * 0.48f)
        close()
    }
    drawPath(
        path = roof,
        brush = Brush.linearGradient(listOf(ReadingOasisColors.Coral, Color(0xFFE11D48)))
    )
    // باب مقوس لطيف
    val door = Path().apply {
        moveTo(w * 0.42f, h * 0.86f)
        lineTo(w * 0.42f, h * 0.64f)
        quadraticBezierTo(w * 0.50f, h * 0.56f, w * 0.58f, h * 0.64f)
        lineTo(w * 0.58f, h * 0.86f)
        close()
    }
    drawPath(door, color = Color(0xFF92400E))
    // نافذة دائرية
    drawCircle(color = Color(0xFF38BDF8), radius = w * 0.07f, center = Offset(w * 0.67f, h * 0.58f))
    drawCircle(color = Color.White, radius = w * 0.07f, center = Offset(w * 0.67f, h * 0.58f), style = Stroke(w * 0.025f))
}

private fun DrawScope.drawTwoHouses() {
    val w = size.width
    val h = size.height
    // تلة خضراء صغيرة
    val hill = Path().apply {
        moveTo(w * 0.08f, h * 0.86f)
        quadraticBezierTo(w * 0.50f, h * 0.72f, w * 0.92f, h * 0.86f)
        close()
    }
    drawPath(hill, color = ReadingOasisColors.Mint)

    // البيت الأول (أيسر)
    drawRoundRect(
        color = Color(0xFFFEF08A),
        topLeft = Offset(w * 0.14f, h * 0.48f),
        size = Size(w * 0.34f, h * 0.32f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    val roof1 = Path().apply {
        moveTo(w * 0.10f, h * 0.50f)
        quadraticBezierTo(w * 0.31f, h * 0.26f, w * 0.52f, h * 0.50f)
        close()
    }
    drawPath(roof1, color = ReadingOasisColors.Coral)

    // البيت الثاني (أيمن)
    drawRoundRect(
        color = Color(0xFFE0F2FE),
        topLeft = Offset(w * 0.52f, h * 0.40f),
        size = Size(w * 0.34f, h * 0.40f),
        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
    )
    val roof2 = Path().apply {
        moveTo(w * 0.48f, h * 0.43f)
        quadraticBezierTo(w * 0.69f, h * 0.18f, w * 0.90f, h * 0.43f)
        close()
    }
    drawPath(roof2, color = ReadingOasisColors.Purple)
}

private fun DrawScope.drawColorfulDoor() {
    val w = size.width
    val h = size.height
    // إطار باب ملون مقوّس
    val frame = Path().apply {
        moveTo(w * 0.24f, h * 0.88f)
        lineTo(w * 0.24f, h * 0.38f)
        cubicTo(w * 0.24f, h * 0.14f, w * 0.76f, h * 0.14f, w * 0.76f, h * 0.38f)
        lineTo(w * 0.76f, h * 0.88f)
        close()
    }
    drawPath(
        path = frame,
        brush = Brush.verticalGradient(listOf(Color(0xFFA78BFA), ReadingOasisColors.Purple))
    )
    // قلب الباب الداخلي
    val inner = Path().apply {
        moveTo(w * 0.31f, h * 0.86f)
        lineTo(w * 0.31f, h * 0.40f)
        cubicTo(w * 0.31f, h * 0.23f, w * 0.69f, h * 0.23f, w * 0.69f, h * 0.40f)
        lineTo(w * 0.69f, h * 0.86f)
        close()
    }
    drawPath(
        path = inner,
        brush = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706)))
    )
    // مقبض ذهبي لامع
    drawCircle(color = Color.White, radius = w * 0.06f, center = Offset(w * 0.39f, h * 0.60f))
}

private fun DrawScope.drawSmilingFlower() {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val cy = h * 0.48f
    // بتلات وردية ناعمة
    for (i in 0 until 6) {
        val angle = (i * 60.0) * PI / 180.0
        val px = cx + (cos(angle) * w * 0.22f).toFloat()
        val py = cy + (sin(angle) * h * 0.22f).toFloat()
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFF9A8D4), ReadingOasisColors.Pink),
                center = Offset(px, py),
                radius = w * 0.16f
            ),
            radius = w * 0.15f,
            center = Offset(px, py)
        )
    }
    // قلب الزهرة المشمس المبتسم
    drawCircle(color = ReadingOasisColors.SunnyYellow, radius = w * 0.18f, center = Offset(cx, cy))
    drawCircle(color = ReadingOasisColors.InkDark, radius = w * 0.025f, center = Offset(cx - w * 0.06f, cy - h * 0.03f))
    drawCircle(color = ReadingOasisColors.InkDark, radius = w * 0.025f, center = Offset(cx + w * 0.06f, cy - h * 0.03f))
    val smile = Path().apply {
        moveTo(cx - w * 0.06f, cy + h * 0.04f)
        quadraticBezierTo(cx, cy + h * 0.10f, cx + w * 0.06f, cy + h * 0.04f)
    }
    drawPath(smile, color = ReadingOasisColors.InkDark, style = Stroke(w * 0.03f, cap = StrokeCap.Round))
}

private fun DrawScope.drawRadiantSunLamp() {
    val w = size.width
    val h = size.height
    val cx = w * 0.50f
    val cy = h * 0.50f
    // أشعة مضيئة حول الشمس
    for (i in 0 until 8) {
        val angle = (i * 45.0) * PI / 180.0
        val start = Offset(cx + (cos(angle) * w * 0.28f).toFloat(), cy + (sin(angle) * h * 0.28f).toFloat())
        val end = Offset(cx + (cos(angle) * w * 0.42f).toFloat(), cy + (sin(angle) * h * 0.42f).toFloat())
        drawLine(
            color = ReadingOasisColors.SunnyYellow,
            start = start,
            end = end,
            strokeWidth = w * 0.07f,
            cap = StrokeCap.Round
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFFF59D), ReadingOasisColors.SunnyYellow, ReadingOasisColors.Coral),
            center = Offset(cx - w * 0.06f, cy - h * 0.06f),
            radius = w * 0.30f
        ),
        radius = w * 0.24f,
        center = Offset(cx, cy)
    )
}

private fun DrawScope.drawGoldenTrumpet() {
    val w = size.width
    val h = size.height
    // جسم البوق الذهبي المنحني
    val horn = Path().apply {
        moveTo(w * 0.18f, h * 0.62f)
        quadraticBezierTo(w * 0.46f, h * 0.54f, w * 0.72f, h * 0.24f)
        cubicTo(w * 0.84f, h * 0.32f, w * 0.88f, h * 0.58f, w * 0.78f, h * 0.72f)
        quadraticBezierTo(w * 0.52f, h * 0.60f, w * 0.22f, h * 0.70f)
        close()
    }
    drawPath(
        path = horn,
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFFFFF1A8), ReadingOasisColors.SunnyYellow, Color(0xFFD97706))
        )
    )
    // حلقة البوق السفلية
    drawOval(
        color = Color(0xFFD97706),
        topLeft = Offset(w * 0.36f, h * 0.58f),
        size = Size(w * 0.22f, h * 0.14f),
        style = Stroke(width = w * 0.055f)
    )
    // نغمتان صغيرتان منطلقتان
    drawCircle(color = ReadingOasisColors.Pink, radius = w * 0.055f, center = Offset(w * 0.86f, h * 0.22f))
    drawCircle(color = ReadingOasisColors.SkyBright, radius = w * 0.045f, center = Offset(w * 0.90f, h * 0.38f))
}

private fun DrawScope.drawCuteGooseOnWater() {
    val w = size.width
    val h = size.height
    // موجات ماء زرقاء ناعمة
    val water = Path().apply {
        moveTo(w * 0.10f, h * 0.78f)
        quadraticBezierTo(w * 0.30f, h * 0.70f, w * 0.50f, h * 0.78f)
        quadraticBezierTo(w * 0.70f, h * 0.86f, w * 0.90f, h * 0.78f)
    }
    drawPath(water, color = ReadingOasisColors.SkyBright, style = Stroke(w * 0.07f, cap = StrokeCap.Round))

    // جسم البطة/الوزة الأبيض اللطيف
    val gooseBody = Path().apply {
        moveTo(w * 0.22f, h * 0.64f)
        cubicTo(w * 0.20f, h * 0.46f, w * 0.54f, h * 0.44f, w * 0.62f, h * 0.54f)
        cubicTo(w * 0.64f, h * 0.40f, w * 0.56f, h * 0.24f, w * 0.68f, h * 0.24f)
        cubicTo(w * 0.78f, h * 0.24f, w * 0.78f, h * 0.42f, w * 0.74f, h * 0.58f)
        cubicTo(w * 0.74f, h * 0.74f, w * 0.28f, h * 0.76f, w * 0.22f, h * 0.64f)
        close()
    }
    drawPath(gooseBody, color = Color.White)
    drawPath(gooseBody, color = Color(0xFFCBD5E1), style = Stroke(width = w * 0.025f))

    // المنقار البرتقالي
    val beak = Path().apply {
        moveTo(w * 0.75f, h * 0.31f)
        quadraticBezierTo(w * 0.89f, h * 0.33f, w * 0.75f, h * 0.39f)
        close()
    }
    drawPath(beak, color = ReadingOasisColors.Coral)
    drawCircle(color = ReadingOasisColors.InkDark, radius = w * 0.03f, center = Offset(w * 0.69f, h * 0.31f))
}

private data class CelebrationParticle(
    val xFraction: Float,
    val angleRad: Float,
    val speed: Float,
    val radiusDp: Float,
    val color: Color,
    val isBalloon: Boolean,
    val isStar: Boolean
)

/**
 * مؤثر احتفالي طفولي يطلق نجوماً وشرارات وبالونات ملونة بسلاسة عند الفوز.
 */
@Composable
fun KidsCelebrationCanvas(
    triggerKey: Int,
    modifier: Modifier = Modifier
) {
    if (triggerKey <= 0) return

    val progress = remember(triggerKey) { Animatable(0f) }
    val particles = remember(triggerKey) {
        val palette = listOf(
            ReadingOasisColors.SunnyYellow,
            ReadingOasisColors.Coral,
            ReadingOasisColors.Mint,
            ReadingOasisColors.Purple,
            ReadingOasisColors.Pink,
            ReadingOasisColors.SkyBright
        )
        List(26) { index ->
            val fraction = (index + 0.5f) / 26f
            val angle = (-155f + (index * 19 % 130)) * (PI / 180.0).toFloat()
            CelebrationParticle(
                xFraction = fraction,
                angleRad = angle,
                speed = 0.55f + (index % 5) * 0.14f,
                radiusDp = if (index % 4 == 0) 14f else 8f + (index % 3) * 3f,
                color = palette[index % palette.size],
                isBalloon = index % 4 == 0,
                isStar = index % 4 == 1 || index % 4 == 2
            )
        }
    }

    LaunchedEffect(triggerKey) {
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1650, easing = FastOutSlowInEasing)
        )
    }

    val p = progress.value
    if (p >= 0.99f) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val alpha = when {
            p < 0.72f -> 1f
            else -> ((1f - p) / 0.28f).coerceIn(0f, 1f)
        }

        particles.forEachIndexed { idx, item ->
            val r = item.radiusDp.dp.toPx()
            if (item.isBalloon) {
                val bx = size.width * item.xFraction + sin(p * 6f + idx) * 16.dp.toPx()
                val by = size.height * (1.05f - p * (0.95f + (idx % 3) * 0.12f))
                // خيط البالون المتعرج
                val stringPath = Path().apply {
                    moveTo(bx, by + r * 1.15f)
                    quadraticBezierTo(bx + r * 0.45f, by + r * 1.85f, bx - r * 0.25f, by + r * 2.5f)
                }
                drawPath(
                    path = stringPath,
                    color = Color(0xFF94A3B8).copy(alpha = alpha * 0.7f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                // جسم البالون المنحني
                val balloon = Path().apply {
                    moveTo(bx, by - r * 1.15f)
                    cubicTo(bx + r * 1.1f, by - r * 1.15f, bx + r * 1.1f, by + r * 0.7f, bx, by + r * 1.12f)
                    cubicTo(bx - r * 1.1f, by + r * 0.7f, bx - r * 1.1f, by - r * 1.15f, bx, by - r * 1.15f)
                    close()
                }
                drawPath(balloon, color = item.color.copy(alpha = alpha))
                drawCircle(
                    color = Color.White.copy(alpha = alpha * 0.55f),
                    radius = r * 0.25f,
                    center = Offset(bx - r * 0.30f, by - r * 0.38f)
                )
            } else {
                val originX = size.width * 0.50f
                val originY = size.height * 0.52f
                val dist = minOf(size.width, size.height) * 0.62f * item.speed * p
                val gravity = size.height * 0.24f * p * p
                val px = originX + cos(item.angleRad) * dist
                val py = originY + sin(item.angleRad) * dist + gravity
                if (item.isStar) {
                    val star = buildSmoothStarPath(Offset(px, py), r, r * 0.48f)
                    drawPath(star, color = item.color.copy(alpha = alpha))
                } else {
                    drawCircle(color = item.color.copy(alpha = alpha), radius = r * 0.65f, center = Offset(px, py))
                }
            }
        }
    }
}
