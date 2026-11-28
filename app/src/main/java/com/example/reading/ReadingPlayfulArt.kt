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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
 * زر طفولي بارز بزوايا دائرية واسعة وحافة ظل سفلية صلبة مع ارتداد نابضي عند الضغط
 * وقياس طبيعي للمحتوى بدون أي قص للنصوص أو التشكيل.
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
    contentPadding: PaddingValues = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
    testTagValue: String? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val pressOffset by animateDpAsState(
        targetValue = if (isPressed && enabled) lipDepth else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playful_btn_offset"
    )
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "playful_btn_scale"
    )

    val shape = RoundedCornerShape(cornerRadius)
    val effectiveBg = if (enabled) backgroundColor else Color(0xFFCBD5E1)
    val effectiveShadow = if (enabled) shadowColor else Color(0xFF94A3B8)

    Box(
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
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
            ),
        contentAlignment = Alignment.Center,
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(top = lipDepth)
                .clip(shape)
                .background(effectiveShadow)
        )
        Box(
            modifier = Modifier
                .padding(bottom = lipDepth)
                .offset(y = pressOffset)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            effectiveBg.copy(alpha = 0.98f),
                            effectiveBg
                        )
                    )
                )
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

/**
 * الشخصية التعليمية المحبوبة "فصيح" — بومة قارئة سماوية وذهبية مرسومة بالكامل على Canvas
 * بهالة توهج دافئة، طبقات تظليل ناعمة، رموش تطرف، أجنحة ترفرف بلطف، وقفزة احتفالية.
 */
@Composable
fun ReadingMascotCanvas(
    modifier: Modifier = Modifier,
    isCelebrating: Boolean = false,
    isSpeaking: Boolean = false,
    onTap: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "fasih_mascot_anim")
    var tapCount by remember { mutableIntStateOf(0) }
    val tapJump = remember { Animatable(0f) }
    val tapTilt = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            launch {
                tapJump.snapTo(0f)
                tapJump.animateTo(
                    targetValue = -22f,
                    animationSpec = tween(durationMillis = 135, easing = FastOutSlowInEasing)
                )
                tapJump.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
            launch {
                tapTilt.animateTo(-10f, tween(90))
                tapTilt.animateTo(10f, tween(110))
                tapTilt.animateTo(-5f, tween(90))
                tapTilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }

    val floatOffset by infinite.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_float"
    )

    val breatheScale by infinite.animateFloat(
        initialValue = 0.975f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_breathe"
    )

    val wingFlap by infinite.animateFloat(
        initialValue = -11f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (isCelebrating || tapJump.value < -2f) 260 else 1150,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_wing"
    )

    val haloPulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "mascot_halo"
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

    val isHappyJump = isCelebrating || tapJump.value < -3f
    val isBlinking = blinkPhase > 0.93f && !isHappyJump

    val interactiveModifier = if (onTap != null) {
        val tapSource = remember { MutableInteractionSource() }
        modifier.clickable(
            interactionSource = tapSource,
            indication = null
        ) {
            tapCount++
            onTap()
        }
    } else {
        modifier
    }

    Canvas(
        modifier = interactiveModifier.graphicsLayer {
            translationY = floatOffset + celebrationBounce + tapJump.value
            rotationZ = tapTilt.value
            scaleX = breatheScale
            scaleY = breatheScale
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // هالة توهج دافئة ناعمة خلف فصيح
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFFF3A1).copy(alpha = 0.45f),
                    Color(0xFF7DD3FC).copy(alpha = 0.24f),
                    Color.Transparent
                ),
                center = Offset(cx, h * 0.48f),
                radius = w * 0.50f * haloPulse
            ),
            radius = w * 0.50f * haloPulse,
            center = Offset(cx, h * 0.48f)
        )

        // ظل ناعم أسفل البومة
        drawOval(
            color = Color(0xFF0284C7).copy(alpha = 0.16f),
            topLeft = Offset(w * 0.22f, h * 0.89f),
            size = Size(w * 0.56f, h * 0.09f)
        )

        // الجناح الأيسر المجسّم بطبقة لمعان
        rotate(degrees = -wingFlap, pivot = Offset(w * 0.26f, h * 0.52f)) {
            val leftWing = Path().apply {
                moveTo(w * 0.26f, h * 0.43f)
                cubicTo(w * 0.03f, h * 0.46f, w * 0.04f, h * 0.74f, w * 0.24f, h * 0.76f)
                quadraticBezierTo(w * 0.31f, h * 0.62f, w * 0.26f, h * 0.43f)
                close()
            }
            drawPath(
                path = leftWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1)),
                    start = Offset(0f, h * 0.40f),
                    end = Offset(w * 0.30f, h * 0.78f)
                )
            )
            drawPath(
                path = leftWing,
                color = Color(0xFFBAE6FD).copy(alpha = 0.55f),
                style = Stroke(width = w * 0.015f)
            )
        }

        // الجناح الأيمن المجسّم بطبقة لمعان
        rotate(degrees = wingFlap, pivot = Offset(w * 0.74f, h * 0.52f)) {
            val rightWing = Path().apply {
                moveTo(w * 0.74f, h * 0.43f)
                cubicTo(w * 0.97f, h * 0.46f, w * 0.96f, h * 0.74f, w * 0.76f, h * 0.76f)
                quadraticBezierTo(w * 0.69f, h * 0.62f, w * 0.74f, h * 0.43f)
                close()
            }
            drawPath(
                path = rightWing,
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1)),
                    start = Offset(w, h * 0.40f),
                    end = Offset(w * 0.70f, h * 0.78f)
                )
            )
            drawPath(
                path = rightWing,
                color = Color(0xFFBAE6FD).copy(alpha = 0.55f),
                style = Stroke(width = w * 0.015f)
            )
        }

        // القدمان الذهبيتان بتدرج دافئ
        val footBrush = Brush.verticalGradient(listOf(Color(0xFFFDE047), Color(0xFFF59E0B)))
        drawOval(brush = footBrush, topLeft = Offset(w * 0.33f, h * 0.80f), size = Size(w * 0.14f, h * 0.09f))
        drawOval(brush = footBrush, topLeft = Offset(w * 0.53f, h * 0.80f), size = Size(w * 0.14f, h * 0.09f))

        // الجسم السماوي الممتلئ بتدرج إشعاعي ولمعة علوية
        val bodyPath = Path().apply {
            moveTo(cx, h * 0.14f)
            cubicTo(w * 0.83f, h * 0.14f, w * 0.85f, h * 0.85f, cx, h * 0.85f)
            cubicTo(w * 0.15f, h * 0.85f, w * 0.17f, h * 0.14f, cx, h * 0.14f)
            close()
        }
        drawPath(
            path = bodyPath,
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF0284C7), Color(0xFF0369A1)),
                center = Offset(cx, h * 0.34f),
                radius = w * 0.56f
            )
        )
        drawPath(
            path = bodyPath,
            color = Color(0xFFE0F2FE).copy(alpha = 0.45f),
            style = Stroke(width = w * 0.014f)
        )

        // خصلتا الأذن الذهبيتان
        val tuftBrush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706)))
        val leftTuft = Path().apply {
            moveTo(w * 0.24f, h * 0.26f)
            quadraticBezierTo(w * 0.15f, h * 0.09f, w * 0.34f, h * 0.17f)
            close()
        }
        val rightTuft = Path().apply {
            moveTo(w * 0.76f, h * 0.26f)
            quadraticBezierTo(w * 0.85f, h * 0.09f, w * 0.66f, h * 0.17f)
            close()
        }
        drawPath(leftTuft, brush = tuftBrush)
        drawPath(rightTuft, brush = tuftBrush)

        // التاج الملكي الصغير فوق رأس فصيح
        val crownPath = Path().apply {
            moveTo(w * 0.38f, h * 0.17f)
            lineTo(w * 0.41f, h * 0.05f)
            lineTo(cx, h * 0.11f)
            lineTo(w * 0.59f, h * 0.05f)
            lineTo(w * 0.62f, h * 0.17f)
            close()
        }
        drawPath(
            path = crownPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706))
            )
        )
        drawCircle(color = ReadingColors.CoralOrange, radius = w * 0.024f, center = Offset(cx, h * 0.135f))

        // البطن الكريمي الناعم
        drawOval(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFFBEB), Color(0xFFFDE68A)),
                center = Offset(cx, h * 0.60f),
                radius = w * 0.26f
            ),
            topLeft = Offset(w * 0.28f, h * 0.51f),
            size = Size(w * 0.44f, h * 0.30f)
        )

        val featherStroke = Stroke(width = w * 0.022f, cap = StrokeCap.Round)
        for (row in 0..1) {
            val fy = h * (0.61f + row * 0.08f)
            val cols = if (row == 0) listOf(0.41f, 0.50f, 0.59f) else listOf(0.45f, 0.55f)
            cols.forEach { fxFrac ->
                val feather = Path().apply {
                    moveTo(w * (fxFrac - 0.03f), fy)
                    quadraticBezierTo(w * fxFrac, fy + h * 0.034f, w * (fxFrac + 0.03f), fy)
                }
                drawPath(feather, color = Color(0xFFF59E0B).copy(alpha = 0.48f), style = featherStroke)
            }
        }

        // هالتا العينين مع إطار ذهبي ناعم
        val eyeRadius = w * 0.148f
        val leftEyeCenter = Offset(w * 0.37f, h * 0.39f)
        val rightEyeCenter = Offset(w * 0.63f, h * 0.39f)
        listOf(leftEyeCenter, rightEyeCenter).forEach { c ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White, Color(0xFFF1F5F9)),
                    center = Offset(c.x - eyeRadius * 0.2f, c.y - eyeRadius * 0.2f),
                    radius = eyeRadius * 1.2f
                ),
                radius = eyeRadius,
                center = c
            )
            drawCircle(
                color = ReadingColors.SunYellow,
                radius = eyeRadius,
                center = c,
                style = Stroke(width = w * 0.024f)
            )
        }

        if (isBlinking || isHappyJump) {
            val smileEyeStroke = Stroke(width = w * 0.034f, cap = StrokeCap.Round)
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
                    radius = pupilRadius * 0.35f,
                    center = Offset(center.x - pupilRadius * 0.28f, center.y - pupilRadius * 0.28f)
                )
                drawCircle(
                    color = Color(0xFFBAE6FD).copy(alpha = 0.8f),
                    radius = pupilRadius * 0.16f,
                    center = Offset(center.x + pupilRadius * 0.24f, center.y + pupilRadius * 0.24f)
                )
            }
        }

        // الخدان الورديان المتوردان
        drawOval(
            color = ReadingColors.CandyPink.copy(alpha = 0.34f),
            topLeft = Offset(w * 0.21f, h * 0.48f),
            size = Size(w * 0.12f, h * 0.065f)
        )
        drawOval(
            color = ReadingColors.CandyPink.copy(alpha = 0.34f),
            topLeft = Offset(w * 0.67f, h * 0.48f),
            size = Size(w * 0.12f, h * 0.065f)
        )

        // المنقار الذهبي اللامع
        val beakOpen = if (isSpeaking || isCelebrating) h * 0.025f else 0f
        val beakPath = Path().apply {
            moveTo(cx, h * 0.44f)
            lineTo(w * 0.565f, h * 0.50f)
            lineTo(cx, h * 0.575f + beakOpen)
            lineTo(w * 0.435f, h * 0.50f)
            close()
        }
        drawPath(
            path = beakPath,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF3A1), Color(0xFFFBBF24), Color(0xFFD97706))
            )
        )

        // كتاب القراءة المفتوح بلمعة ذهبية
        val bookY = h * 0.72f
        val leftPage = Path().apply {
            moveTo(cx, bookY)
            quadraticBezierTo(w * 0.38f, bookY - h * 0.045f, w * 0.29f, bookY + h * 0.01f)
            lineTo(w * 0.31f, bookY + h * 0.12f)
            quadraticBezierTo(w * 0.40f, bookY + h * 0.075f, cx, bookY + h * 0.11f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(cx, bookY)
            quadraticBezierTo(w * 0.62f, bookY - h * 0.045f, w * 0.71f, bookY + h * 0.01f)
            lineTo(w * 0.69f, bookY + h * 0.12f)
            quadraticBezierTo(w * 0.60f, bookY + h * 0.075f, cx, bookY + h * 0.11f)
            close()
        }
        drawPath(leftPage, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFEF3C7))))
        drawPath(rightPage, brush = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A))))
        drawPath(leftPage, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.018f))
        drawPath(rightPage, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.018f))
    }
}

/**
 * نجمة ذهبية منحنية مجسّمة الإحساس مرسومة على Canvas.
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

internal fun DrawScope.drawCurvedStar(
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
                colors = listOf(Color(0xFFFFF7B2), ReadingColors.StarGold, Color(0xFFD97706)),
                center = Offset(center.x - outerRadius * 0.22f, center.y - outerRadius * 0.22f),
                radius = outerRadius * 1.45f
            )
        )
        drawPath(
            path = path,
            color = Color(0xFFB45309),
            style = Stroke(width = outerRadius * 0.13f, join = StrokeJoin.Round)
        )
    } else {
        drawPath(path = path, color = Color(0xFFE2E8F0))
        drawPath(
            path = path,
            color = Color(0xFF94A3B8),
            style = Stroke(width = outerRadius * 0.12f, join = StrokeJoin.Round)
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
        drawPath(wave1, color = tint.copy(alpha = alpha1), style = Stroke(width = strokeW, cap = StrokeCap.Round))

        val wave2 = Path().apply {
            moveTo(w * 0.79f, h * 0.22f)
            quadraticBezierTo(w * 0.94f, h * 0.50f, w * 0.79f, h * 0.78f)
        }
        drawPath(wave2, color = tint.copy(alpha = alpha2), style = Stroke(width = strokeW, cap = StrokeCap.Round))
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
            drawRoundRect(
                color = crayonColor,
                topLeft = Offset(w * 0.34f, h * 0.26f),
                size = Size(w * 0.32f, h * 0.60f),
                cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
            )
            drawRect(
                color = Color.White.copy(alpha = 0.84f),
                topLeft = Offset(w * 0.34f, h * 0.42f),
                size = Size(w * 0.32f, h * 0.24f)
            )
            drawLine(crayonColor, Offset(w * 0.34f, h * 0.48f), Offset(w * 0.66f, h * 0.48f), h * 0.04f)
            drawLine(crayonColor, Offset(w * 0.34f, h * 0.60f), Offset(w * 0.66f, h * 0.60f), h * 0.04f)
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
 * تاج البطل الذهبي المتوهج المرسوم على Canvas.
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
            lineTo(w * 0.50f, h * 0.16f)
            lineTo(w * 0.67f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.30f)
            lineTo(w * 0.88f, h * 0.78f)
            close()
        }
        drawPath(
            crown,
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706))
            )
        )
        drawPath(crown, color = Color(0xFFB45309), style = Stroke(width = w * 0.055f, join = StrokeJoin.Round))
        drawCircle(color = ReadingColors.CoralOrange, radius = w * 0.068f, center = Offset(w * 0.50f, h * 0.62f))
        drawCircle(color = ReadingColors.SkyBlue, radius = w * 0.052f, center = Offset(w * 0.30f, h * 0.64f))
        drawCircle(color = ReadingColors.MeadowGreen, radius = w * 0.052f, center = Offset(w * 0.70f, h * 0.64f))
    }
}

/**
 * سهم رجوع ناعم دائري متوافق مع الواجهة العربية.
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
            drawCircle(color = tint.copy(alpha = 0.24f), radius = w * 0.46f, center = Offset(w * 0.5f, h * 0.45f))
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
        drawLine(tint, Offset(w * 0.50f, h * 0.68f), Offset(w * 0.50f, h * 0.86f), strokeW, cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.34f, h * 0.86f), Offset(w * 0.66f, h * 0.86f), strokeW, cap = StrokeCap.Round)
    }
}

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
        drawLine(tint, Offset(w * 0.18f, h * 0.84f), Offset(w * 0.84f, h * 0.84f), h * 0.09f, cap = StrokeCap.Round)
    }
}

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
        drawPath(path, color = tint, style = Stroke(width = w * 0.15f, cap = StrokeCap.Round, join = StrokeJoin.Round))
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
 * قاطرة ملونة متحركة البخار مرسومة على Canvas تتقدم "قطار المقاطع".
 */
@Composable
fun PlayfulTrainEngineCanvas(
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "train_steam")
    val puff by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "steam_puff"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // بخار أبيض متصاعد ناعم
        drawCircle(
            color = Color.White.copy(alpha = (1f - puff * 0.6f).coerceIn(0.2f, 0.95f)),
            radius = w * (0.07f + puff * 0.03f),
            center = Offset(w * (0.75f + puff * 0.06f), h * (0.19f - puff * 0.10f))
        )
        drawCircle(
            color = Color.White.copy(alpha = (0.8f - puff * 0.6f).coerceIn(0.15f, 0.8f)),
            radius = w * 0.06f,
            center = Offset(w * (0.85f + puff * 0.04f), h * (0.10f - puff * 0.05f))
        )

        drawRoundRect(
            brush = Brush.verticalGradient(listOf(ReadingColors.CoralOrange, ReadingColors.CoralShadow)),
            topLeft = Offset(w * 0.66f, h * 0.22f),
            size = Size(w * 0.16f, h * 0.26f),
            cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFA78BFA), ReadingColors.BerryPurple, ReadingColors.BerryShadow)),
            topLeft = Offset(w * 0.14f, h * 0.20f),
            size = Size(w * 0.36f, h * 0.52f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        drawRoundRect(
            brush = Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFF7DD3FC))),
            topLeft = Offset(w * 0.20f, h * 0.28f),
            size = Size(w * 0.22f, h * 0.20f),
            cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
        )
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(Color(0xFFFFE57F), ReadingColors.SunYellow, Color(0xFFD97706))),
            topLeft = Offset(w * 0.44f, h * 0.42f),
            size = Size(w * 0.44f, h * 0.32f),
            cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
        )
        val nose = Path().apply {
            moveTo(w * 0.86f, h * 0.54f)
            lineTo(w * 0.96f, h * 0.74f)
            lineTo(w * 0.86f, h * 0.74f)
            close()
        }
        drawPath(nose, color = ReadingColors.CoralOrange)

        listOf(0.28f, 0.56f, 0.78f).forEach { cxFrac ->
            drawCircle(color = ReadingColors.InkDark, radius = h * 0.14f, center = Offset(w * cxFrac, h * 0.80f))
            drawCircle(color = Color.White, radius = h * 0.06f, center = Offset(w * cxFrac, h * 0.80f))
        }
    }
}

/**
 * رسوم فنية مجسّمة الإحساس ومتحركة لكل جزيرة درس مرسومة بالكامل بـ Compose Canvas
 * باستخدام منحنيات ناعمة وطبقات تظليل ولمعان وحركة حية مستمرة.
 */
@Composable
fun LessonMiniIllustrationCanvas(
    artType: LessonArtType,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "lesson_art_${artType.name}")

    val phase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "art_phase"
    )
    val sway by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "art_sway"
    )
    val spin by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "art_spin"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (artType) {
            // 1) طفل مبتسم متورد الخدين بخصلة متحركة ونجوم لطيفة
            LessonArtType.DADA -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF1E0), Color(0xFFFFDAB9), Color(0xFFF4B68A)),
                        center = Offset(w * 0.44f, h * 0.46f),
                        radius = w * 0.44f
                    ),
                    radius = w * 0.36f,
                    center = Offset(w * 0.50f, h * 0.55f)
                )
                // خصلة شعر متمايلة بلطف
                val curl = Path().apply {
                    moveTo(w * 0.47f, h * 0.21f)
                    cubicTo(
                        w * (0.35f + sway * 0.04f), h * 0.05f,
                        w * (0.65f + sway * 0.04f), h * 0.06f,
                        w * 0.54f, h * 0.21f
                    )
                }
                drawPath(curl, color = Color(0xFF78350F), style = Stroke(width = w * 0.075f, cap = StrokeCap.Round))

                // عينان لامعتان وخدان ورديان
                listOf(0.38f, 0.62f).forEach { ex ->
                    drawCircle(color = ReadingColors.InkDark, radius = w * 0.055f, center = Offset(w * ex, h * 0.49f))
                    drawCircle(color = Color.White, radius = w * 0.02f, center = Offset(w * (ex - 0.015f), h * 0.47f))
                }
                drawOval(
                    color = ReadingColors.CandyPink.copy(alpha = 0.42f),
                    topLeft = Offset(w * 0.21f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.09f)
                )
                drawOval(
                    color = ReadingColors.CandyPink.copy(alpha = 0.42f),
                    topLeft = Offset(w * 0.65f, h * 0.55f),
                    size = Size(w * 0.14f, h * 0.09f)
                )
                val smile = Path().apply {
                    moveTo(w * 0.37f, h * 0.64f)
                    quadraticBezierTo(w * 0.50f, h * 0.78f, w * 0.63f, h * 0.64f)
                }
                drawPath(smile, color = ReadingColors.CoralOrange, style = Stroke(width = w * 0.065f, cap = StrokeCap.Round))
            }

            // 2) بيت ثلاثي الطبقات بدخان متحرك متصاعد من المدخنة ونافذة مضيئة
            LessonArtType.DAR -> {
                // مدخنة البيت ودخان متحرك متصاعد
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFB45309), Color(0xFF78350F))),
                    topLeft = Offset(w * 0.65f, h * 0.16f),
                    size = Size(w * 0.13f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                )
                val smokeY1 = h * (0.14f - phase * 0.10f)
                val smokeY2 = h * (0.10f - ((phase + 0.5f) % 1f) * 0.10f)
                drawCircle(
                    color = Color.White.copy(alpha = (1f - phase) * 0.85f),
                    radius = w * (0.06f + phase * 0.04f),
                    center = Offset(w * (0.72f + sway * 0.04f), smokeY1)
                )
                drawCircle(
                    color = Color.White.copy(alpha = (1f - ((phase + 0.5f) % 1f)) * 0.75f),
                    radius = w * 0.055f,
                    center = Offset(w * (0.76f - sway * 0.03f), smokeY2)
                )

                // جدار جانبي مظلل (الطبقة الثانية لإعطاء العمق)
                val sideWall = Path().apply {
                    moveTo(w * 0.72f, h * 0.45f)
                    lineTo(w * 0.86f, h * 0.49f)
                    lineTo(w * 0.86f, h * 0.86f)
                    lineTo(w * 0.72f, h * 0.88f)
                    close()
                }
                drawPath(sideWall, brush = Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706))))

                // الواجهة الأمامية الدافئة
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFEF3C7))),
                    topLeft = Offset(w * 0.16f, h * 0.44f),
                    size = Size(w * 0.58f, h * 0.44f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                )

                // السقف المنحني ذو الطبقتين
                val roof = Path().apply {
                    moveTo(w * 0.08f, h * 0.48f)
                    quadraticBezierTo(w * 0.45f, h * 0.12f, w * 0.50f, h * 0.14f)
                    quadraticBezierTo(w * 0.68f, h * 0.24f, w * 0.90f, h * 0.49f)
                    close()
                }
                drawPath(
                    roof,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFF9A76), ReadingColors.CoralOrange, Color(0xFFB91C1C)),
                        start = Offset(w * 0.1f, h * 0.15f),
                        end = Offset(w * 0.9f, h * 0.50f)
                    )
                )

                // باب مقوّس ونافذة متوهجة
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))),
                    topLeft = Offset(w * 0.38f, h * 0.60f),
                    size = Size(w * 0.17f, h * 0.28f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFF7B2), Color(0xFF38BDF8))),
                    radius = w * 0.075f,
                    center = Offset(w * 0.27f, h * 0.61f)
                )
            }

            // 3) بيتان متجاوران (دور) بعمق لوني وشجرة صغيرة متمايلة
            LessonArtType.DOOR -> {
                val houseConfigs = listOf(
                    Triple(0.06f, Color(0xFFFF7A59), Color(0xFFB91C1C)),
                    Triple(0.48f, Color(0xFFA78BFA), Color(0xFF5B21B6))
                )
                houseConfigs.forEach { (startX, topRoof, botRoof) ->
                    val r = Path().apply {
                        moveTo(w * startX, h * 0.50f)
                        quadraticBezierTo(w * (startX + 0.22f), h * 0.18f, w * (startX + 0.44f), h * 0.50f)
                        close()
                    }
                    drawPath(r, brush = Brush.verticalGradient(listOf(topRoof, botRoof)))
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFFEF3C7))),
                        topLeft = Offset(w * (startX + 0.05f), h * 0.48f),
                        size = Size(w * 0.34f, h * 0.36f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                    drawRoundRect(
                        color = ReadingColors.SunYellow,
                        topLeft = Offset(w * (startX + 0.16f), h * 0.61f),
                        size = Size(w * 0.12f, h * 0.23f),
                        cornerRadius = CornerRadius(w * 0.05f, w * 0.05f)
                    )
                }
                // شجرة صغيرة متمايلة بين البيتين
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFF86EFAC), ReadingColors.MeadowGreen)),
                    radius = w * 0.10f,
                    center = Offset(w * (0.48f + sway * 0.02f), h * 0.72f)
                )
            }

            // 4) باب خشبي مزخرف بمقبض ذهبي لامع متوهج
            LessonArtType.BABA -> {
                // إطار القوس الخارجي الذهبي
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFB45309))),
                    topLeft = Offset(w * 0.18f, h * 0.10f),
                    size = Size(w * 0.64f, h * 0.80f),
                    cornerRadius = CornerRadius(w * 0.32f, w * 0.32f)
                )
                // جسم الباب الخشبي المزخرف
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E), Color(0xFF78350F))),
                    topLeft = Offset(w * 0.25f, h * 0.17f),
                    size = Size(w * 0.50f, h * 0.73f),
                    cornerRadius = CornerRadius(w * 0.25f, w * 0.25f)
                )
                // زخرفة قوس داخلي محفور
                drawRoundRect(
                    color = Color(0xFFFDE68A).copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.32f, h * 0.25f),
                    size = Size(w * 0.36f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.18f, w * 0.18f),
                    style = Stroke(width = w * 0.025f)
                )
                // مقبض ذهبي لامع مع وميض حي
                val knobCenter = Offset(w * 0.62f, h * 0.58f)
                drawCircle(
                    color = Color(0xFFFFF3A1).copy(alpha = 0.35f + 0.25f * ((sway + 1f) * 0.5f)),
                    radius = w * 0.105f,
                    center = knobCenter
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color.White, ReadingColors.SunYellow, Color(0xFFB45309))),
                    radius = w * 0.062f,
                    center = knobCenter
                )
            }

            // 5) زهرة مبهجة متعددة الطبقات متمايلة برقة
            LessonArtType.MAMA -> {
                val petalColors = listOf(
                    ReadingColors.CandyPink,
                    ReadingColors.CoralOrange,
                    ReadingColors.SunYellow,
                    ReadingColors.BerryPurple,
                    ReadingColors.SkyBlue,
                    ReadingColors.MeadowGreen
                )
                drawLine(
                    color = ReadingColors.MeadowShadow,
                    start = Offset(w * 0.50f, h * 0.64f),
                    end = Offset(w * 0.50f, h * 0.92f),
                    strokeWidth = w * 0.08f,
                    cap = StrokeCap.Round
                )
                rotate(degrees = sway * 6f, pivot = Offset(w * 0.50f, h * 0.44f)) {
                    for (i in 0 until 6) {
                        val angle = i * PI / 3.0
                        val px = w * 0.50f + (w * 0.23f * cos(angle)).toFloat()
                        val py = h * 0.44f + (h * 0.23f * sin(angle)).toFloat()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(Color.White.copy(alpha = 0.55f), petalColors[i]),
                                center = Offset(px - w * 0.04f, py - h * 0.04f),
                                radius = w * 0.18f
                            ),
                            radius = w * 0.155f,
                            center = Offset(px, py)
                        )
                    }
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706))),
                        radius = w * 0.15f,
                        center = Offset(w * 0.50f, h * 0.44f)
                    )
                }
            }

            // 6) شمس/مصباح متوهج بأشعة دوّارة وهالة إشعاعية
            LessonArtType.NOOR -> {
                val center = Offset(w * 0.50f, h * 0.50f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFF7B2).copy(alpha = 0.72f), Color(0xFFFFC83D).copy(alpha = 0.25f), Color.Transparent),
                        center = center,
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = center
                )
                rotate(degrees = spin, pivot = center) {
                    for (i in 0 until 10) {
                        val angle = i * (2.0 * PI / 10.0)
                        val sx = center.x + (w * 0.28f * cos(angle)).toFloat()
                        val sy = center.y + (h * 0.28f * sin(angle)).toFloat()
                        val ex = center.x + (w * 0.45f * cos(angle)).toFloat()
                        val ey = center.y + (h * 0.45f * sin(angle)).toFloat()
                        drawLine(
                            brush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.CoralOrange)),
                            start = Offset(sx, sy),
                            end = Offset(ex, ey),
                            strokeWidth = w * 0.07f,
                            cap = StrokeCap.Round
                        )
                    }
                }
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFFFFFFF), Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706)),
                        center = Offset(center.x - w * 0.06f, center.y - h * 0.06f),
                        radius = w * 0.30f
                    ),
                    radius = w * 0.25f,
                    center = center
                )
            }

            // 7) بوق ذهبي لامع بنوتات موسيقية عائمة ومتحركة
            LessonArtType.BOOQ -> {
                val horn = Path().apply {
                    moveTo(w * 0.10f, h * 0.56f)
                    cubicTo(w * 0.36f, h * 0.50f, w * 0.58f, h * 0.40f, w * 0.80f, h * 0.20f)
                    lineTo(w * 0.88f, h * 0.76f)
                    cubicTo(w * 0.64f, h * 0.60f, w * 0.40f, h * 0.58f, w * 0.12f, h * 0.64f)
                    close()
                }
                drawPath(
                    horn,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706), Color(0xFF92400E)),
                        start = Offset(w * 0.15f, h * 0.25f),
                        end = Offset(w * 0.85f, h * 0.75f)
                    )
                )
                // فوهة البوق المجسّمة
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFF78350F), Color(0xFFB45309))),
                    topLeft = Offset(w * 0.76f, h * 0.24f),
                    size = Size(w * 0.14f, h * 0.48f)
                )
                // نوتتان موسيقيتان عائمتان للأعلى
                val note1Y = h * (0.32f - phase * 0.18f)
                val note2Y = h * (0.28f - ((phase + 0.5f) % 1f) * 0.18f)
                val alpha1 = (1f - phase).coerceIn(0.2f, 1f)
                val alpha2 = (1f - ((phase + 0.5f) % 1f)).coerceIn(0.2f, 1f)

                drawCircle(ReadingColors.CoralOrange.copy(alpha = alpha1), w * 0.055f, Offset(w * 0.36f, note1Y))
                drawLine(ReadingColors.CoralOrange.copy(alpha = alpha1), Offset(w * 0.41f, note1Y), Offset(w * 0.41f, note1Y - h * 0.14f), w * 0.035f, cap = StrokeCap.Round)

                drawCircle(ReadingColors.SkyBlue.copy(alpha = alpha2), w * 0.05f, Offset(w * 0.54f, note2Y))
                drawLine(ReadingColors.SkyBlue.copy(alpha = alpha2), Offset(w * 0.59f, note2Y), Offset(w * 0.59f, note2Y - h * 0.13f), w * 0.035f, cap = StrokeCap.Round)
            }

            // 8) وزّة لطيفة تطفو بحركة ناعمة على أمواج منحنية
            LessonArtType.WAZ -> {
                val bobY = sway * h * 0.035f
                // أمواج منحنية متحركة
                val wave1 = Path().apply {
                    moveTo(w * 0.06f, h * 0.78f)
                    cubicTo(w * 0.28f, h * (0.68f + sway * 0.03f), w * 0.54f, h * (0.86f - sway * 0.03f), w * 0.94f, h * 0.75f)
                }
                drawPath(wave1, color = Color(0xFF7DD3FC), style = Stroke(width = w * 0.085f, cap = StrokeCap.Round))

                // جسم الوزة الأبيض اللامع
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFF1F5F9), Color(0xFFCBD5E1)),
                        center = Offset(w * 0.42f, h * 0.54f + bobY),
                        radius = w * 0.34f
                    ),
                    topLeft = Offset(w * 0.18f, h * 0.45f + bobY),
                    size = Size(w * 0.54f, h * 0.32f)
                )
                // عنق ورأس الوزة
                val neck = Path().apply {
                    moveTo(w * 0.56f, h * 0.52f + bobY)
                    quadraticBezierTo(w * 0.64f, h * 0.40f + bobY, w * 0.66f, h * 0.34f + bobY)
                }
                drawPath(neck, color = Color.White, style = Stroke(width = w * 0.15f, cap = StrokeCap.Round))
                drawCircle(color = Color.White, radius = w * 0.145f, center = Offset(w * 0.66f, h * 0.33f + bobY))
                val beak = Path().apply {
                    moveTo(w * 0.77f, h * 0.30f + bobY)
                    lineTo(w * 0.94f, h * 0.37f + bobY)
                    lineTo(w * 0.77f, h * 0.41f + bobY)
                    close()
                }
                drawPath(beak, brush = Brush.linearGradient(listOf(ReadingColors.SunYellow, ReadingColors.CoralOrange)))
                drawCircle(ReadingColors.InkDark, w * 0.036f, Offset(w * 0.70f, h * 0.30f + bobY))
                val wing = Path().apply {
                    moveTo(w * 0.30f, h * 0.56f + bobY)
                    cubicTo(w * 0.42f, h * 0.70f + bobY, w * 0.54f, h * 0.68f + bobY, w * 0.60f, h * 0.57f + bobY)
                }
                drawPath(wing, color = Color(0xFF94A3B8).copy(alpha = 0.7f), style = Stroke(width = w * 0.05f, cap = StrokeCap.Round))
            }

            // 9) كتاب مفتوح تتطاير منه نجوم ذهبية لامعة
            LessonArtType.OPEN_BOOK_STARS -> {
                val leftPage = Path().apply {
                    moveTo(w * 0.50f, h * 0.50f)
                    cubicTo(w * 0.36f, h * 0.42f, w * 0.18f, h * 0.44f, w * 0.10f, h * 0.52f)
                    lineTo(w * 0.14f, h * 0.84f)
                    cubicTo(w * 0.26f, h * 0.76f, w * 0.40f, h * 0.78f, w * 0.50f, h * 0.86f)
                    close()
                }
                val rightPage = Path().apply {
                    moveTo(w * 0.50f, h * 0.50f)
                    cubicTo(w * 0.64f, h * 0.42f, w * 0.82f, h * 0.44f, w * 0.90f, h * 0.52f)
                    lineTo(w * 0.86f, h * 0.84f)
                    cubicTo(w * 0.74f, h * 0.76f, w * 0.60f, h * 0.78f, w * 0.50f, h * 0.86f)
                    close()
                }
                drawPath(leftPage, brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFEF3C7))))
                drawPath(rightPage, brush = Brush.linearGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A))))
                drawPath(leftPage, color = ReadingColors.SunShadow, style = Stroke(width = w * 0.032f, join = StrokeJoin.Round))
                drawPath(rightPage, color = ReadingColors.SunShadow, style = Stroke(width = w * 0.032f, join = StrokeJoin.Round))

                // ثلاث نجوم تتطاير للأعلى من صفحات الكتاب
                val s1Y = h * (0.40f - phase * 0.24f)
                val s2Y = h * (0.36f - ((phase + 0.35f) % 1f) * 0.22f)
                val s3Y = h * (0.42f - ((phase + 0.7f) % 1f) * 0.22f)
                drawCurvedStar(Offset(w * 0.34f, s1Y), w * 0.09f, w * 0.045f, filled = true)
                drawCurvedStar(Offset(w * 0.52f, s2Y), w * 0.11f, w * 0.055f, filled = true)
                drawCurvedStar(Offset(w * 0.70f, s3Y), w * 0.08f, w * 0.04f, filled = true)
            }

            // 10) ريشة قلم مضيئة ترسم خطاً متوهجاً
            LessonArtType.GLOWING_QUILL -> {
                // خط كتابة مضيء أسفل الريشة
                val inkSwirl = Path().apply {
                    moveTo(w * 0.16f, h * 0.82f)
                    cubicTo(w * 0.34f, h * 0.70f, w * 0.56f, h * 0.90f, w * 0.82f, h * 0.78f)
                }
                drawPath(
                    inkSwirl,
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, ReadingColors.CoralOrange)),
                    style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
                )
                rotate(degrees = sway * 7f, pivot = Offset(w * 0.32f, h * 0.74f)) {
                    val feather = Path().apply {
                        moveTo(w * 0.30f, h * 0.74f)
                        cubicTo(w * 0.34f, h * 0.32f, w * 0.60f, h * 0.10f, w * 0.86f, h * 0.14f)
                        cubicTo(w * 0.84f, h * 0.44f, w * 0.58f, h * 0.64f, w * 0.30f, h * 0.74f)
                        close()
                    }
                    drawPath(
                        feather,
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFFFFF3A1), Color(0xFF38BDF8), ReadingColors.BerryPurple),
                            start = Offset(w * 0.85f, h * 0.12f),
                            end = Offset(w * 0.28f, h * 0.74f)
                        )
                    )
                    // عمود الريشة المضيء
                    drawLine(
                        color = Color.White,
                        start = Offset(w * 0.28f, h * 0.76f),
                        end = Offset(w * 0.78f, h * 0.22f),
                        strokeWidth = w * 0.045f,
                        cap = StrokeCap.Round
                    )
                }
            }

            // 11) منظار استكشاف ذهبي وأزرق مع عدسة لامعة ونجوم متلألئة
            LessonArtType.TELESCOPE -> {
                rotate(degrees = -26f + sway * 4f, pivot = Offset(w * 0.50f, h * 0.54f)) {
                    // الأنبوب الخلفي
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFB45309))),
                        topLeft = Offset(w * 0.12f, h * 0.44f),
                        size = Size(w * 0.30f, h * 0.18f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    // الأنبوب الأوسط الملكي
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF60A5FA), Color(0xFF2563EB), Color(0xFF1E3A8A))),
                        topLeft = Offset(w * 0.36f, h * 0.40f),
                        size = Size(w * 0.30f, h * 0.26f),
                        cornerRadius = CornerRadius(w * 0.06f, w * 0.06f)
                    )
                    // الأنبوب الأمامي الذهبي الواسع
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706))),
                        topLeft = Offset(w * 0.62f, h * 0.35f),
                        size = Size(w * 0.24f, h * 0.36f),
                        cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
                    )
                    // العدسة الأمامية المتلألئة
                    drawOval(
                        brush = Brush.radialGradient(listOf(Color.White, Color(0xFFBAE6FD), Color(0xFF0284C7))),
                        topLeft = Offset(w * 0.80f, h * 0.37f),
                        size = Size(w * 0.11f, h * 0.32f)
                    )
                }
                // نجوم استكشاف صغيرة متلألئة
                drawCurvedStar(Offset(w * 0.80f, h * 0.20f), w * (0.08f + 0.02f * sway), w * 0.04f, filled = true)
                drawCircle(Color.White.copy(alpha = 0.85f), w * 0.035f, Offset(w * 0.22f, h * 0.24f))
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

/**
 * غلاف دخول متتابع نابض ومرح لبطاقات الحروف والمقاطع عند فتح المحطة أو الدرس.
 */
@Composable
fun StaggeredSpringEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    delayPerItemMs: Long = 55L,
    content: @Composable () -> Unit
) {
    val progress = remember(index) { Animatable(0f) }

    LaunchedEffect(index) {
        delay((index.coerceIn(0, 12) * delayPerItemMs))
        progress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )
    }

    Box(
        modifier = modifier.graphicsLayer {
            val p = progress.value
            alpha = p.coerceIn(0f, 1f)
            val s = 0.78f + p * 0.22f
            scaleX = s
            scaleY = s
            translationY = (1f - p) * 24f
        }
    ) {
        content()
    }
}

/**
 * انفجار نجوم ولمعان احتفالي موضعي خفيف حول قطع البازل المتعشّقة لحظة نجاح الدمج.
 */
@Composable
fun PuzzleCelebrationBurstCanvas(
    burstTrigger: Int,
    modifier: Modifier = Modifier
) {
    val anim = remember { Animatable(0f) }

    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            anim.snapTo(0f)
            anim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 820, easing = FastOutSlowInEasing)
            )
        }
    }

    val p = anim.value
    if (burstTrigger <= 0 || p <= 0.005f || p >= 0.995f) return

    val particleColors = remember {
        listOf(
            ReadingColors.SunYellow,
            ReadingColors.CoralOrange,
            ReadingColors.SkyBlue,
            ReadingColors.MeadowGreen,
            ReadingColors.CandyPink,
            ReadingColors.BerryPurple
        )
    }

    Canvas(modifier = modifier) {
        val cx = size.width * 0.5f
        val cy = size.height * 0.5f
        val maxDist = size.minDimension * 0.56f
        val fade = (1f - ((p - 0.55f) / 0.45f).coerceIn(0f, 1f)).coerceIn(0f, 1f)

        // حلقة توهج ذهبية تتوسع بهدوء
        drawCircle(
            color = ReadingColors.SunYellow.copy(alpha = fade * 0.42f),
            radius = maxDist * (0.28f + p * 0.74f),
            center = Offset(cx, cy),
            style = Stroke(width = (8f * (1f - p * 0.65f)).coerceAtLeast(1.5f))
        )

        val count = 12
        for (i in 0 until count) {
            val angle = (i * 2.0 * PI / count) + p * 0.45
            val dist = maxDist * (0.18f + p * (0.72f + (i % 3) * 0.10f))
            val px = cx + (cos(angle) * dist).toFloat()
            val py = cy + (sin(angle) * dist).toFloat()
            val starR = (7f + (i % 3) * 3f) * (0.65f + (1f - p) * 0.55f)

            if (i % 2 == 0) {
                drawCurvedStar(
                    center = Offset(px, py),
                    outerRadius = starR,
                    innerRadius = starR * 0.46f,
                    filled = true
                )
            } else {
                drawCircle(
                    color = particleColors[i % particleColors.size].copy(alpha = fade * 0.92f),
                    radius = starR * 0.55f,
                    center = Offset(px, py)
                )
            }
        }
    }
}

