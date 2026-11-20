package com.example.reading

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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class ReadingLessonTab {
    LEARN,
    ASSEMBLE,
    WRITE,
    CHALLENGE
}

/**
 * زر طفولي بارز بزوايا دائرية واسعة وحافة ظل سفلية صلبة مع ارتداد نابضي عند الضغط.
 */
@Composable
fun PlayfulBouncyButton(
    onClick: () -> Unit,
    backgroundColor: Color,
    shadowColor: Color,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    lipDepth: Dp = 5.dp,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
    testTagValue: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) lipDepth else 0.dp,
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

    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBg = if (enabled) backgroundColor else Color(0xFFCBD5E1)
    val effectiveShadow = if (enabled) shadowColor else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 52.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(if (testTagValue != null) Modifier.testTag(testTagValue) else Modifier)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick
            )
    ) {
        // الحافة السفلية الصلبة التي تعطي الإحساس البارز الملموس
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = lipDepth)
                .clip(shape)
                .background(effectiveShadow)
        )
        // سطح الزر العلوي القابل للانضغاط
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(bottom = lipDepth)
                .offset(y = pressOffset)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            effectiveBg.copy(alpha = 0.96f),
                            effectiveBg
                        )
                    )
                )
        ) {
            // لمعة خفيفة ناعمة أعلى الزر
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxSize(fraction = 0.25f)
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center,
                content = content
            )
        }
    }
}

/**
 * الشخصية التعليمية المحبوبة "فصيح" — بومة قارئة سماوية وذهبية مرسومة بالكامل على Canvas
 * برموش تطرف وأجنحة ترفرف بلطف وقفزة احتفالية عند التشجيع.
 */
@Composable
fun ReadingMascotCanvas(
    modifier: Modifier = Modifier,
    isCelebrating: Boolean = false,
    isSpeaking: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "fasih_mascot_anim")

    val floatOffset by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )

    val wingFlap by infinite.animateFloat(
        initialValue = -10f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCelebrating) 320 else 1200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_wing"
    )

    val blinkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "mascot_blink"
    )

    val celebrationBounce by animateFloatAsState(
        targetValue = if (isCelebrating) -16f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "mascot_celebrate_jump"
    )

    val isBlinking = blinkPhase > 0.93f && !isCelebrating

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = floatOffset + celebrationBounce
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.55f

        // ظل ناعم أسفل البومة
        drawOval(
            color = Color(0xFF0284C7).copy(alpha = 0.14f),
            topLeft = Offset(w * 0.24f, h * 0.89f),
            size = Size(w * 0.52f, h * 0.09f)
        )

        // الجناح الأيسر
        rotate(degrees = -wingFlap, pivot = Offset(w * 0.26f, h * 0.52f)) {
            val leftWing = Path().apply {
                moveTo(w * 0.26f, h * 0.44f)
                cubicTo(w * 0.04f, h * 0.46f, w * 0.05f, h * 0.72f, w * 0.24f, h * 0.75f)
                quadraticBezierTo(w * 0.30f, h * 0.62f, w * 0.26f, h * 0.44f)
                close()
            }
            drawPath(
                path = leftWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                    start = Offset(0f, h * 0.42f),
                    end = Offset(w * 0.3f, h * 0.78f)
                )
            )
        }

        // الجناح الأيمن
        rotate(degrees = wingFlap, pivot = Offset(w * 0.74f, h * 0.52f)) {
            val rightWing = Path().apply {
                moveTo(w * 0.74f, h * 0.44f)
                cubicTo(w * 0.96f, h * 0.46f, w * 0.95f, h * 0.72f, w * 0.76f, h * 0.75f)
                quadraticBezierTo(w * 0.70f, h * 0.62f, w * 0.74f, h * 0.44f)
                close()
            }
            drawPath(
                path = rightWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF0EA5E9), Color(0xFF0284C7)),
                    start = Offset(w, h * 0.42f),
                    end = Offset(w * 0.7f, h * 0.78f)
                )
            )
        }

        // القدمان الذهبيتان
        drawOval(
            color = Color(0xFFF59E0B),
            topLeft = Offset(w * 0.34f, h * 0.80f),
            size = Size(w * 0.13f, h * 0.085f)
        )
        drawOval(
            color = Color(0xFFF59E0B),
            topLeft = Offset(w * 0.53f, h * 0.80f),
            size = Size(w * 0.13f, h * 0.085f)
        )

        // الجسم السماوي الممتلئ
        val bodyPath = Path().apply {
            moveTo(cx, h * 0.14f)
            cubicTo(w * 0.82f, h * 0.14f, w * 0.84f, h * 0.84f, cx, h * 0.84f)
            cubicTo(w * 0.16f, h * 0.84f, w * 0.18f, h * 0.14f, cx, h * 0.14f)
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFF0284C7)),
                center = Offset(cx, h * 0.36f),
                radius = w * 0.55f
            )
        )

        // خصلتا الأذن الذهبيتان اللطيفتان
        val leftTuft = Path().apply {
            moveTo(w * 0.25f, h * 0.26f)
            quadraticBezierTo(w * 0.16f, h * 0.10f, w * 0.34f, h * 0.17f)
            close()
        }
        val rightTuft = Path().apply {
            moveTo(w * 0.75f, h * 0.26f)
            quadraticBezierTo(w * 0.84f, h * 0.10f, w * 0.66f, h * 0.17f)
            close()
        }
        drawPath(leftTuft, color = ReadingColors.SunYellow)
        drawPath(rightTuft, color = ReadingColors.SunYellow)

        // تاج صغير ذهبي على رأس فصيح
        val crownPath = Path().apply {
            moveTo(w * 0.39f, h * 0.17f)
            lineTo(w * 0.42f, h * 0.06f)
            lineTo(cx, h * 0.12f)
            lineTo(w * 0.58f, h * 0.06f)
            lineTo(w * 0.61f, h * 0.17f)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFE066), ReadingColors.SunYellow, Color(0xFFF59E0B))
            )
        )

        // البطن الكريمي الدافئ
        drawOval(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))
            ),
            topLeft = Offset(w * 0.29f, h * 0.52f),
            size = Size(w * 0.42f, h * 0.29f)
        )

        // ريش صغير لطيف على البطن
        val featherStroke = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
        for (row in 0..1) {
            val fy = h * (0.62f + row * 0.08f)
            val cols = if (row == 0) listOf(0.41f, 0.50f, 0.59f) else listOf(0.45f, 0.55f)
            cols.forEach { fxFrac ->
                val feather = Path().apply {
                    moveTo(w * (fxFrac - 0.03f), fy)
                    quadraticBezierTo(w * fxFrac, fy + h * 0.032f, w * (fxFrac + 0.03f), fy)
                }
                drawPath(feather, color = Color(0xFFF59E0B).copy(alpha = 0.45f), style = featherStroke)
            }
        }

        // هالتان بيضاوان حول العينين
        val eyeRadius = w * 0.145f
        val leftEyeCenter = Offset(w * 0.37f, h * 0.39f)
        val rightEyeCenter = Offset(w * 0.63f, h * 0.39f)
        drawCircle(color = Color.White, radius = eyeRadius, center = leftEyeCenter)
        drawCircle(color = Color.White, radius = eyeRadius, center = rightEyeCenter)
        drawCircle(
            color = ReadingColors.SunYellow,
            radius = eyeRadius,
            center = leftEyeCenter,
            style = Stroke(width = w * 0.022f)
        )
        drawCircle(
            color = ReadingColors.SunYellow,
            radius = eyeRadius,
            center = rightEyeCenter,
            style = Stroke(width = w * 0.022f)
        )

        // العينان (تطرفان أو تبتسمان عند الاحتفال)
        if (isBlinking || isCelebrating) {
            val smileEyeStroke = Stroke(width = w * 0.032f, cap = StrokeCap.Round)
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                val arc = Path().apply {
                    moveTo(center.x - eyeRadius * 0.55f, center.y + eyeRadius * 0.05f)
                    quadraticBezierTo(
                        center.x,
                        center.y - eyeRadius * 0.55f,
                        center.x + eyeRadius * 0.55f,
                        center.y + eyeRadius * 0.05f
                    )
                }
                drawPath(arc, color = ReadingColors.InkDark, style = smileEyeStroke)
            }
        } else {
            val pupilRadius = eyeRadius * 0.56f
            listOf(leftEyeCenter, rightEyeCenter).forEach { center ->
                drawCircle(color = ReadingColors.InkDark, radius = pupilRadius, center = center)
                drawCircle(
                    color = Color.White,
                    radius = pupilRadius * 0.34f,
                    center = Offset(center.x - pupilRadius * 0.30f, center.y - pupilRadius * 0.30f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = pupilRadius * 0.16f,
                    center = Offset(center.x + pupilRadius * 0.25f, center.y + pupilRadius * 0.24f)
                )
            }
        }

        // الخدان الورديان
        drawOval(
            color = ReadingColors.CandyPink.copy(alpha = 0.32f),
            topLeft = Offset(w * 0.22f, h * 0.48f),
            size = Size(w * 0.11f, h * 0.06f)
        )
        drawOval(
            color = ReadingColors.CandyPink.copy(alpha = 0.32f),
            topLeft = Offset(w * 0.67f, h * 0.48f),
            size = Size(w * 0.11f, h * 0.06f)
        )

        // المنقار الذهبي المبتسم
        val beakOpen = if (isSpeaking || isCelebrating) h * 0.024f else 0f
        val beakPath = Path().apply {
            moveTo(cx, h * 0.44f)
            lineTo(w * 0.56f, h * 0.50f)
            lineTo(cx, h * 0.57f + beakOpen)
            lineTo(w * 0.44f, h * 0.50f)
            close()
        }
        drawPath(
            path = beakPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFD166), Color(0xFFF59E0B))
            )
        )

        // كتاب صغير مفتوح يحمله فصيح
        val bookY = h * 0.72f
        val leftPage = Path().apply {
            moveTo(cx, bookY)
            quadraticBezierTo(w * 0.38f, bookY - h * 0.04f, w * 0.30f, bookY + h * 0.01f)
            lineTo(w * 0.32f, bookY + h * 0.11f)
            quadraticBezierTo(w * 0.40f, bookY + h * 0.07f, cx, bookY + h * 0.10f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(cx, bookY)
            quadraticBezierTo(w * 0.62f, bookY - h * 0.04f, w * 0.70f, bookY + h * 0.01f)
            lineTo(w * 0.68f, bookY + h * 0.11f)
            quadraticBezierTo(w * 0.60f, bookY + h * 0.07f, cx, bookY + h * 0.10f)
            close()
        }
        drawPath(leftPage, color = Color.White)
        drawPath(rightPage, color = Color(0xFFF8FAFC))
        drawPath(leftPage, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.018f))
        drawPath(rightPage, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.018f))
    }
}

/**
 * نجمة ذهبية منحنية مرسومة على Canvas (مملوءة أو فارغة).
 */
@Composable
fun PlayfulStarIcon(
    filled: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        drawCurvedStar(
            center = Offset(size.width / 2f, size.height / 2f),
            outerRadius = size.minDimension * 0.46f,
            innerRadius = size.minDimension * 0.23f,
            filled = filled
        )
    }
}

private fun DrawScope.drawCurvedStar(
    center: Offset,
    outerRadius: Float,
    innerRadius: Float,
    filled: Boolean
) {
    val path = Path()
    val points = 5
    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = (i * PI / points) - PI / 2.0
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()

    if (filled) {
        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFEE88), ReadingColors.StarGold, Color(0xFFF59E0B)),
                center = Offset(center.x - outerRadius * 0.2f, center.y - outerRadius * 0.2f),
                radius = outerRadius * 1.4f
            )
        )
        drawPath(
            path = path,
            color = Color(0xFFD97706),
            style = Stroke(width = outerRadius * 0.14f, join = StrokeJoin.Round)
        )
    } else {
        drawPath(
            path = path,
            color = Color(0xFFE2E8F0)
        )
        drawPath(
            path = path,
            color = Color(0xFF94A3B8),
            style = Stroke(width = outerRadius * 0.13f, join = StrokeJoin.Round)
        )
    }
}

/**
 * مكبر صوت نابض بموجات صوتية دائرية مرسومة بـ Canvas.
 */
@Composable
fun PlayfulSpeakerIcon(
    isSpeaking: Boolean = false,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "speaker_pulse")
    val waveAlpha by infinite.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "speaker_wave"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val speakerPath = Path().apply {
            moveTo(w * 0.14f, h * 0.36f)
            lineTo(w * 0.34f, h * 0.36f)
            lineTo(w * 0.56f, h * 0.18f)
            lineTo(w * 0.56f, h * 0.82f)
            lineTo(w * 0.34f, h * 0.64f)
            lineTo(w * 0.14f, h * 0.64f)
            close()
        }
        drawPath(speakerPath, color = tint)

        val strokeW = w * 0.085f
        val alpha1 = if (isSpeaking) waveAlpha else 0.85f
        val alpha2 = if (isSpeaking) (1.25f - waveAlpha).coerceIn(0.3f, 1f) else 0.55f

        val wave1 = Path().apply {
            moveTo(w * 0.67f, h * 0.33f)
            quadraticBezierTo(w * 0.78f, h * 0.50f, w * 0.67f, h * 0.67f)
        }
        drawPath(
            wave1,
            color = tint.copy(alpha = alpha1),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        val wave2 = Path().apply {
            moveTo(w * 0.79f, h * 0.22f)
            quadraticBezierTo(w * 0.94f, h * 0.50f, w * 0.79f, h * 0.78f)
        }
        drawPath(
            wave2,
            color = tint.copy(alpha = alpha2),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
    }
}

/**
 * قلم تلوين طفولي مرسوم على Canvas.
 */
@Composable
fun PlayfulCrayonIcon(
    crayonColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        rotate(degrees = -32f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            // جسم القلم
            drawRoundRect(
                color = crayonColor,
                topLeft = Offset(w * 0.34f, h * 0.26f),
                size = Size(w * 0.32f, h * 0.60f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            // شريط الورق الأبيض
            drawRect(
                color = Color.White.copy(alpha = 0.82f),
                topLeft = Offset(w * 0.34f, h * 0.42f),
                size = Size(w * 0.32f, h * 0.24f)
            )
            drawLine(
                color = crayonColor,
                start = Offset(w * 0.34f, h * 0.48f),
                end = Offset(w * 0.66f, h * 0.48f),
                strokeWidth = h * 0.04f
            )
            drawLine(
                color = crayonColor,
                start = Offset(w * 0.34f, h * 0.60f),
                end = Offset(w * 0.66f, h * 0.60f),
                strokeWidth = h * 0.04f
            )
            // رأس القلم المدبب
            val tip = Path().apply {
                moveTo(w * 0.36f, h * 0.26f)
                lineTo(w * 0.50f, h * 0.06f)
                lineTo(w * 0.64f, h * 0.26f)
                close()
            }
            drawPath(tip, color = crayonColor)
        }
    }
}

/**
 * تاج البطل الذهبي المرسوم على Canvas.
 */
@Composable
fun PlayfulCrownIcon(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.12f, h * 0.78f)
            lineTo(w * 0.08f, h * 0.30f)
            lineTo(w * 0.33f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.18f)
            lineTo(w * 0.67f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.78f)
            close()
        }
        drawPath(
            crown,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFEC80), ReadingColors.SunYellow, Color(0xFFF59E0B))
            )
        )
        drawPath(
            crown,
            color = Color(0xFFD97706),
            style = Stroke(width = w * 0.055f, join = StrokeJoin.Round)
        )
        drawCircle(color = ReadingColors.CoralOrange, radius = w * 0.065f, center = Offset(w * 0.50f, h * 0.62f))
        drawCircle(color = ReadingColors.SkyBlue, radius = w * 0.05f, center = Offset(w * 0.30f, h * 0.64f))
        drawCircle(color = ReadingColors.MeadowGreen, radius = w * 0.05f, center = Offset(w * 0.70f, h * 0.64f))
    }
}

/**
 * سهم رجوع ناعم دائري متوافق مع الواجهة العربية (يشير لليمين).
 */
@Composable
fun PlayfulBackArrowIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = w * 0.14f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val arrow = Path().apply {
            moveTo(w * 0.22f, h * 0.50f)
            lineTo(w * 0.76f, h * 0.50f)
            moveTo(w * 0.52f, h * 0.24f)
            lineTo(w * 0.78f, h * 0.50f)
            lineTo(w * 0.52f, h * 0.76f)
        }
        drawPath(arrow, color = tint, style = stroke)
    }
}

/**
 * أيقونة ميكروفون لطيفة للتحدي الصوتي الاختياري.
 */
@Composable
fun PlayfulMicIcon(
    isListening: Boolean = false,
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = w * 0.095f
        if (isListening) {
            drawCircle(
                color = tint.copy(alpha = 0.24f),
                radius = w * 0.46f,
                center = Offset(w * 0.5f, h * 0.45f)
            )
        }
        drawRoundRect(
            color = tint,
            topLeft = Offset(w * 0.35f, h * 0.12f),
            size = Size(w * 0.30f, h * 0.44f),
            cornerRadius = CornerRadius(w * 0.16f, w * 0.16f)
        )
        val holder = Path().apply {
            moveTo(w * 0.24f, h * 0.44f)
            cubicTo(w * 0.24f, h * 0.74f, w * 0.76f, h * 0.74f, w * 0.76f, h * 0.44f)
        }
        drawPath(holder, color = tint, style = Stroke(width = strokeW, cap = StrokeCap.Round))
        drawLine(
            color = tint,
            start = Offset(w * 0.50f, h * 0.68f),
            end = Offset(w * 0.50f, h * 0.86f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.34f, h * 0.86f),
            end = Offset(w * 0.66f, h * 0.86f),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )
    }
}

/**
 * أيقونة ممحاة مدرسية لطيفة.
 */
@Composable
fun PlayfulEraserIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        rotate(degrees = -28f, pivot = Offset(w * 0.5f, h * 0.5f)) {
            drawRoundRect(
                color = tint,
                topLeft = Offset(w * 0.18f, h * 0.28f),
                size = Size(w * 0.64f, h * 0.42f),
                cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
            )
            drawRect(
                color = ReadingColors.CandyPink.copy(alpha = 0.55f),
                topLeft = Offset(w * 0.18f, h * 0.28f),
                size = Size(w * 0.28f, h * 0.42f)
            )
        }
        drawLine(
            color = tint,
            start = Offset(w * 0.18f, h * 0.84f),
            end = Offset(w * 0.84f, h * 0.84f),
            strokeWidth = h * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

/**
 * علامة صح ناعمة دائرية.
 */
@Composable
fun PlayfulCheckIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.18f, h * 0.54f)
            lineTo(w * 0.42f, h * 0.76f)
            lineTo(w * 0.84f, h * 0.26f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = w * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

/**
 * أيقونات مخصصة بالكامل لتبويبات الدرس الأربعة (تعلّم، ركّب، اكتب، تحدّي).
 */
@Composable
fun PlayfulTabIcon(
    tab: ReadingLessonTab,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (tab) {
        ReadingLessonTab.LEARN -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                val leftPage = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    quadraticBezierTo(w * 0.26f, h * 0.14f, w * 0.10f, h * 0.26f)
                    lineTo(w * 0.10f, h * 0.78f)
                    quadraticBezierTo(w * 0.28f, h * 0.68f, w * 0.50f, h * 0.80f)
                    close()
                }
                val rightPage = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    quadraticBezierTo(w * 0.74f, h * 0.14f, w * 0.90f, h * 0.26f)
                    lineTo(w * 0.90f, h * 0.78f)
                    quadraticBezierTo(w * 0.72f, h * 0.68f, w * 0.50f, h * 0.80f)
                    close()
                }
                drawPath(leftPage, color = tint)
                drawPath(rightPage, color = tint.copy(alpha = 0.82f))
            }
        }
        ReadingLessonTab.ASSEMBLE -> {
            Canvas(modifier = modifier) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.10f, h * 0.34f),
                    size = Size(w * 0.38f, h * 0.42f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                )
                drawRoundRect(
                    color = tint.copy(alpha = 0.84f),
                    topLeft = Offset(w * 0.54f, h * 0.24f),
                    size = Size(w * 0.36f, h * 0.52f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f)
                )
                drawCircle(color = tint, radius = w * 0.10f, center = Offset(w * 0.51f, h * 0.52f))
            }
        }
        ReadingLessonTab.WRITE -> {
            PlayfulCrayonIcon(crayonColor = tint, modifier = modifier)
        }
        ReadingLessonTab.CHALLENGE -> {
            PlayfulStarIcon(filled = true, modifier = modifier)
        }
    }
}

/**
 * قاطرة صغيرة ملونة مرسومة بـ Canvas تتقدم "قطار المقاطع".
 */
@Composable
fun PlayfulTrainEngineCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // بخار أبيض لطيف
        drawCircle(color = Color.White.copy(alpha = 0.92f), radius = w * 0.08f, center = Offset(w * 0.76f, h * 0.16f))
        drawCircle(color = Color.White.copy(alpha = 0.72f), radius = w * 0.06f, center = Offset(w * 0.86f, h * 0.09f))

        // مدخنة القطار
        drawRoundRect(
            color = ReadingColors.CoralOrange,
            topLeft = Offset(w * 0.66f, h * 0.22f),
            size = Size(w * 0.16f, h * 0.26f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        // كابينة السائق
        drawRoundRect(
            color = ReadingColors.BerryPurple,
            topLeft = Offset(w * 0.14f, h * 0.20f),
            size = Size(w * 0.36f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // نافذة الكابينة
        drawRoundRect(
            color = Color(0xFFE0F2FE),
            topLeft = Offset(w * 0.20f, h * 0.28f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        // جسم القاطرة الأمامي
        drawRoundRect(
            color = ReadingColors.SunYellow,
            topLeft = Offset(w * 0.44f, h * 0.42f),
            size = Size(w * 0.44f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        // مقدمة القطار
        val nose = Path().apply {
            moveTo(w * 0.86f, h * 0.54f)
            lineTo(w * 0.96f, h * 0.74f)
            lineTo(w * 0.86f, h * 0.74f)
            close()
        }
        drawPath(nose, color = ReadingColors.CoralOrange)

        // عجلات القطار
        listOf(0.28f, 0.56f, 0.78f).forEach { cxFrac ->
            drawCircle(color = ReadingColors.InkDark, radius = h * 0.14f, center = Offset(w * cxFrac, h * 0.80f))
            drawCircle(color = Color.White, radius = h * 0.06f, center = Offset(w * cxFrac, h * 0.80f))
        }
    }
}

/**
 * رسوم مصغرة معبرة لكل درس مرسومة بالكامل بـ Canvas (بيت، باب، شمس، بوق، بطة، إلخ).
 */
@Composable
fun LessonMiniIllustrationCanvas(
    artType: LessonArtType,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        when (artType) {
            LessonArtType.DADA -> {
                // وجه طفل مبتسم لطيف
                drawCircle(color = Color(0xFFFFE4C4), radius = w * 0.36f, center = Offset(w * 0.5f, h * 0.54f))
                // خصلة شعر
                val curl = Path().apply {
                    moveTo(w * 0.48f, h * 0.18f)
                    cubicTo(w * 0.38f, h * 0.06f, w * 0.62f, h * 0.06f, w * 0.54f, h * 0.20f)
                }
                drawPath(curl, color = Color(0xFF92400E), style = Stroke(width = w * 0.07f, cap = StrokeCap.Round))
                // عينان وخدان
                drawCircle(color = ReadingColors.InkDark, radius = w * 0.05f, center = Offset(w * 0.38f, h * 0.48f))
                drawCircle(color = ReadingColors.InkDark, radius = w * 0.05f, center = Offset(w * 0.62f, h * 0.48f))
                drawCircle(color = ReadingColors.CandyPink.copy(alpha = 0.4f), radius = w * 0.07f, center = Offset(w * 0.28f, h * 0.58f))
                drawCircle(color = ReadingColors.CandyPink.copy(alpha = 0.4f), radius = w * 0.07f, center = Offset(w * 0.72f, h * 0.58f))
                val smile = Path().apply {
                    moveTo(w * 0.38f, h * 0.64f)
                    quadraticBezierTo(w * 0.50f, h * 0.76f, w * 0.62f, h * 0.64f)
                }
                drawPath(smile, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.06f, cap = StrokeCap.Round))
            }

            LessonArtType.DAR -> {
                // بيت لطيف دافئ
                val roof = Path().apply {
                    moveTo(w * 0.12f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.88f, h * 0.48f)
                    close()
                }
                drawPath(roof, color = ReadingColors.CoralOrange)
                drawRoundRect(
                    color = Color(0xFFFFFBEB),
                    topLeft = Offset(w * 0.20f, h * 0.46f),
                    size = Size(w * 0.60f, h * 0.40f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
                drawRoundRect(
                    color = Color(0xFFD97706),
                    topLeft = Offset(w * 0.42f, h * 0.60f),
                    size = Size(w * 0.16f, h * 0.26f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )
                drawCircle(color = ReadingColors.SkyBlue, radius = w * 0.07f, center = Offset(w * 0.30f, h * 0.60f))
                drawCircle(color = ReadingColors.SkyBlue, radius = w * 0.07f, center = Offset(w * 0.70f, h * 0.60f))
            }

            LessonArtType.DOOR -> {
                // بيتان متجاوران (دور)
                listOf(0.08f to ReadingColors.CoralOrange, 0.50f to ReadingColors.BerryPurple).forEach { (startX, roofColor) ->
                    val r = Path().apply {
                        moveTo(w * startX, h * 0.52f)
                        lineTo(w * (startX + 0.21f), h * 0.24f)
                        lineTo(w * (startX + 0.42f), h * 0.52f)
                        close()
                    }
                    drawPath(r, color = roofColor)
                    drawRoundRect(
                        color = Color(0xFFFFFBEB),
                        topLeft = Offset(w * (startX + 0.05f), h * 0.50f),
                        size = Size(w * 0.32f, h * 0.34f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                    drawRoundRect(
                        color = ReadingColors.SunYellow,
                        topLeft = Offset(w * (startX + 0.15f), h * 0.62f),
                        size = Size(w * 0.12f, h * 0.22f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                }
            }

            LessonArtType.BABA -> {
                // باب ملون مقوس
                drawRoundRect(
                    color = ReadingColors.SunYellow,
                    topLeft = Offset(w * 0.22f, h * 0.14f),
                    size = Size(w * 0.56f, h * 0.74f),
                    cornerRadius = CornerRadius(w * 0.26f, w * 0.26f)
                )
                drawRoundRect(
                    color = Color(0xFFB45309),
                    topLeft = Offset(w * 0.29f, h * 0.21f),
                    size = Size(w * 0.42f, h * 0.67f),
                    cornerRadius = CornerRadius(w * 0.20f, w * 0.20f)
                )
                drawCircle(color = ReadingColors.SunYellow, radius = w * 0.06f, center = Offset(w * 0.60f, h * 0.58f))
            }

            LessonArtType.MAMA -> {
                // زهرة مبهجة مع قلب دافئ
                val petalColors = listOf(
                    ReadingColors.CandyPink,
                    ReadingColors.CoralOrange,
                    ReadingColors.SunYellow,
                    ReadingColors.BerryPurple,
                    ReadingColors.SkyBlue,
                    ReadingColors.MeadowGreen
                )
                for (i in 0 until 6) {
                    val angle = i * PI / 3.0
                    val px = w * 0.50f + (w * 0.24f * cos(angle)).toFloat()
                    val py = h * 0.46f + (h * 0.24f * sin(angle)).toFloat()
                    drawCircle(color = petalColors[i], radius = w * 0.15f, center = Offset(px, py))
                }
                drawCircle(color = Color.White, radius = w * 0.15f, center = Offset(w * 0.50f, h * 0.46f))
                drawCircle(color = ReadingColors.SunYellow, radius = w * 0.10f, center = Offset(w * 0.50f, h * 0.46f))
                drawLine(
                    color = ReadingColors.MeadowGreen,
                    start = Offset(w * 0.50f, h * 0.68f),
                    end = Offset(w * 0.50f, h * 0.90f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round
                )
            }

            LessonArtType.NOOR -> {
                // شمس مشرقة ساطعة
                val center = Offset(w * 0.50f, h * 0.50f)
                for (i in 0 until 8) {
                    val angle = i * PI / 4.0
                    val sx = center.x + (w * 0.30f * cos(angle)).toFloat()
                    val sy = center.y + (h * 0.30f * sin(angle)).toFloat()
                    val ex = center.x + (w * 0.44f * cos(angle)).toFloat()
                    val ey = center.y + (h * 0.44f * sin(angle)).toFloat()
                    drawLine(
                        color = ReadingColors.CoralOrange,
                        start = Offset(sx, sy),
                        end = Offset(ex, ey),
                        strokeWidth = w * 0.075f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFF59E0B))
                    ),
                    radius = w * 0.25f,
                    center = center
                )
            }

            LessonArtType.BOOQ -> {
                // بوق ذهبي لامع
                val horn = Path().apply {
                    moveTo(w * 0.14f, h * 0.54f)
                    lineTo(w * 0.58f, h * 0.40f)
                    quadraticBezierTo(w * 0.78f, h * 0.30f, w * 0.84f, h * 0.18f)
                    lineTo(w * 0.88f, h * 0.74f)
                    quadraticBezierTo(w * 0.72f, h * 0.60f, w * 0.56f, h * 0.56f)
                    lineTo(w * 0.14f, h * 0.62f)
                    close()
                }
                drawPath(
                    horn,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFEC80), ReadingColors.SunYellow, Color(0xFFD97706))
                    )
                )
                drawCircle(color = ReadingColors.CoralOrange, radius = w * 0.06f, center = Offset(w * 0.44f, h * 0.34f))
                drawCircle(color = ReadingColors.SkyBlue, radius = w * 0.05f, center = Offset(w * 0.56f, h * 0.26f))
            }

            LessonArtType.WAZ -> {
                // بطة/وزة بيضاء جميلة تسبح على الماء
                drawOval(
                    color = ReadingColors.SkyBlue.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.10f, h * 0.68f),
                    size = Size(w * 0.80f, h * 0.20f)
                )
                // جسم البطة
                drawOval(
                    color = Color.White,
                    topLeft = Offset(w * 0.20f, h * 0.46f),
                    size = Size(w * 0.54f, h * 0.32f)
                )
                // رقبة ورأس البطة
                drawCircle(color = Color.White, radius = w * 0.16f, center = Offset(w * 0.66f, h * 0.36f))
                // المنقار البرتقالي
                val beak = Path().apply {
                    moveTo(w * 0.78f, h * 0.34f)
                    lineTo(w * 0.94f, h * 0.40f)
                    lineTo(w * 0.78f, h * 0.44f)
                    close()
                }
                drawPath(beak, color = ReadingColors.CoralOrange)
                drawCircle(color = ReadingColors.InkDark, radius = w * 0.04f, center = Offset(w * 0.70f, h * 0.33f))
                // جناح صغير
                val wing = Path().apply {
                    moveTo(w * 0.32f, h * 0.56f)
                    quadraticBezierTo(w * 0.48f, h * 0.72f, w * 0.60f, h * 0.58f)
                }
                drawPath(wing, color = Color(0xFFCBD5E1), style = Stroke(width = w * 0.055f, cap = StrokeCap.Round))
            }
        }
    }
}

/**
 * طبقة احتفال مبهجة: بالونات صغيرة ملونة تتصاعد مع شرارات ونجوم ذهبية متناثرة.
 */
@Composable
fun KidsCelebrationCanvas(
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    if (!visible) return
    val infinite = rememberInfiniteTransition(label = "kids_celebration")
    val progress by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "celebration_phase"
    )

    val colors = remember {
        listOf(
            ReadingColors.SunYellow,
            ReadingColors.CoralOrange,
            ReadingColors.MeadowGreen,
            ReadingColors.BerryPurple,
            ReadingColors.CandyPink,
            ReadingColors.SkyBlue
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // بالونات صغيرة متصاعدة للأعلى
        for (i in 0 until 7) {
            val baseX = w * (0.12f + i * 0.125f)
            val sway = sin((progress * 2f * PI + i).toDouble()).toFloat() * 18f
            val localP = (progress + i * 0.14f) % 1f
            val y = h * (1.08f - localP * 1.25f)
            val alpha = (1f - localP).coerceIn(0.15f, 0.92f)
            val balloonColor = colors[i % colors.size].copy(alpha = alpha)

            drawOval(
                color = balloonColor,
                topLeft = Offset(baseX + sway - 16f, y - 22f),
                size = Size(32f, 40f)
            )
            drawCircle(
                color = Color.White.copy(alpha = alpha * 0.55f),
                radius = 5f,
                center = Offset(baseX + sway - 5f, y - 10f)
            )
            drawLine(
                color = Color(0xFF64748B).copy(alpha = alpha * 0.5f),
                start = Offset(baseX + sway, y + 18f),
                end = Offset(baseX + sway + 4f, y + 44f),
                strokeWidth = 2.5f
            )
        }

        // شرارات ونجوم ذهبية متناثرة
        for (j in 0 until 14) {
            val angle = (j * 2.0 * PI / 14.0) + progress * 1.8
            val dist = (0.15f + (progress * 0.72f + j * 0.07f) % 0.65f) * size.minDimension * 0.55f
            val sx = w * 0.5f + (cos(angle) * dist).toFloat()
            val sy = h * 0.44f + (sin(angle) * dist).toFloat()
            val starSize = 9f + (j % 3) * 4f
            if (j % 2 == 0) {
                drawCurvedStar(
                    center = Offset(sx, sy),
                    outerRadius = starSize,
                    innerRadius = starSize * 0.48f,
                    filled = true
                )
            } else {
                drawCircle(
                    color = colors[j % colors.size].copy(alpha = 0.85f),
                    radius = starSize * 0.55f,
                    center = Offset(sx, sy)
                )
            }
        }
    }
}
