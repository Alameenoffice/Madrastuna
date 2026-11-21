package com.example.reading.qiraati

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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.reading.ReadingColors
import com.example.reading.ReadingTajawalFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * غيمة الحرف العائمة: سحابة سماوية وبيضاء ناعمة تطفو بلطف
 * وتعرض أشكال الحرف (المنفصل والمتصل) بألوان مبهجة وخط تجوال واضح.
 */
@Composable
fun LetterCloudCanvas(
    cloudForms: List<String>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "letter_cloud_float")
    val floatY by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_bob"
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cloud_glow"
    )

    Box(
        modifier = modifier
            .graphicsLayer { translationY = floatY },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // هالة سماوية مضيئة خلف الغيمة
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF7DD3FC).copy(alpha = 0.42f),
                        Color(0xFFBAE6FD).copy(alpha = 0.16f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.52f),
                    radius = w * 0.52f * glowPulse
                ),
                topLeft = Offset(w * 0.02f, h * 0.08f),
                size = Size(w * 0.96f, h * 0.84f)
            )

            // ظل سفلي ناعم للغيمة
            drawOval(
                color = Color(0xFF0284C7).copy(alpha = 0.14f),
                topLeft = Offset(w * 0.18f, h * 0.84f),
                size = Size(w * 0.64f, h * 0.11f)
            )

            // جسم الغيمة المرسوم بمنحنيات ناعمة متصلة
            val cloudPath = Path().apply {
                moveTo(w * 0.22f, h * 0.78f)
                cubicTo(w * 0.05f, h * 0.78f, w * 0.04f, h * 0.48f, w * 0.20f, h * 0.44f)
                cubicTo(w * 0.18f, h * 0.22f, w * 0.42f, h * 0.12f, w * 0.54f, h * 0.24f)
                cubicTo(w * 0.68f, h * 0.12f, w * 0.88f, h * 0.26f, w * 0.82f, h * 0.46f)
                cubicTo(w * 0.96f, h * 0.50f, w * 0.94f, h * 0.78f, w * 0.76f, h * 0.78f)
                close()
            }

            drawPath(
                path = cloudPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE0F2FE),
                        Color(0xFFBAE6FD)
                    )
                )
            )
            drawPath(
                path = cloudPath,
                color = Color(0xFF38BDF8),
                style = Stroke(width = 4.5f, join = StrokeJoin.Round)
            )

            // لمعة بيضاء ناعمة أعلى الغيمة
            val shine = Path().apply {
                moveTo(w * 0.28f, h * 0.34f)
                quadraticBezierTo(w * 0.42f, h * 0.20f, w * 0.56f, h * 0.30f)
            }
            drawPath(
                path = shine,
                color = Color.White.copy(alpha = 0.9f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 10.dp)
        ) {
            cloudForms.forEachIndexed { idx, form ->
                val pillBg = if (idx == 0) accentColor else Color(0xFF0284C7)
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = pillBg,
                    shadowElevation = 4.dp,
                    modifier = Modifier.border(2.5.dp, Color.White, RoundedCornerShape(20.dp))
                ) {
                    Text(
                        text = form,
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        lineHeight = 46.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * العصفور الأزرق المبتسم (مستوحى من شخصية "أقرأ" في الكتاب العراقي):
 * يرفرف بجناحيه بلطف ويحمل لافتة مبهجة بعنوان "أقرأ".
 */
@Composable
fun ReadingBlueBirdCanvas(
    bannerText: String = "أقرأ",
    isCelebrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "blue_bird_anim")
    val wingFlap by infinite.animateFloat(
        initialValue = -14f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCelebrating) 260 else 820, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bird_wing"
    )
    val bobY by infinite.animateFloat(
        initialValue = -3.5f,
        targetValue = 3.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bird_bob"
    )

    Row(
        modifier = modifier.graphicsLayer { translationY = bobY },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(72.dp)) {
            val w = size.width
            val h = size.height

            // هالة مضيئة ناعمة خلف العصفور
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBAE6FD).copy(alpha = 0.55f), Color.Transparent),
                    center = Offset(w * 0.50f, h * 0.52f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.50f, h * 0.52f)
            )

            // ذيل العصفور المنحني
            val tail = Path().apply {
                moveTo(w * 0.22f, h * 0.62f)
                cubicTo(w * 0.04f, h * 0.58f, w * 0.04f, h * 0.78f, w * 0.24f, h * 0.74f)
                close()
            }
            drawPath(tail, color = Color(0xFF0284C7))

            // الجناح الخلفي
            rotate(degrees = -wingFlap, pivot = Offset(w * 0.38f, h * 0.50f)) {
                val wing = Path().apply {
                    moveTo(w * 0.40f, h * 0.48f)
                    cubicTo(w * 0.12f, h * 0.30f, w * 0.10f, h * 0.64f, w * 0.38f, h * 0.66f)
                    close()
                }
                drawPath(
                    wing,
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0369A1)))
                )
            }

            // جسم العصفور الأزرق الممتلئ
            val body = Path().apply {
                moveTo(w * 0.52f, h * 0.22f)
                cubicTo(w * 0.80f, h * 0.22f, w * 0.86f, h * 0.56f, w * 0.74f, h * 0.74f)
                cubicTo(w * 0.60f, h * 0.88f, w * 0.28f, h * 0.86f, w * 0.22f, h * 0.66f)
                cubicTo(w * 0.18f, h * 0.46f, w * 0.30f, h * 0.22f, w * 0.52f, h * 0.22f)
                close()
            }
            drawPath(
                body,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF7DD3FC), Color(0xFF0EA5E9), Color(0xFF0284C7)),
                    center = Offset(w * 0.46f, h * 0.42f),
                    radius = w * 0.48f
                )
            )

            // بطن العصفور الفاتح
            drawOval(
                brush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBAE6FD))),
                topLeft = Offset(w * 0.38f, h * 0.50f),
                size = Size(w * 0.34f, h * 0.28f)
            )

            // ريشة تاج صغيرة أعلى الرأس
            val crest = Path().apply {
                moveTo(w * 0.48f, h * 0.23f)
                quadraticBezierTo(w * 0.44f, h * 0.08f, w * 0.55f, h * 0.14f)
                quadraticBezierTo(w * 0.62f, h * 0.08f, w * 0.60f, h * 0.23f)
            }
            drawPath(crest, color = Color(0xFF0284C7), style = Stroke(width = 5f, cap = StrokeCap.Round))

            // الجناح الأمامي المرفرف
            rotate(degrees = wingFlap, pivot = Offset(w * 0.46f, h * 0.56f)) {
                val frontWing = Path().apply {
                    moveTo(w * 0.44f, h * 0.52f)
                    cubicTo(w * 0.22f, h * 0.54f, w * 0.22f, h * 0.76f, w * 0.48f, h * 0.72f)
                    close()
                }
                drawPath(
                    frontWing,
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7)))
                )
            }

            // العين المشرقة والوجنة الوردية
            drawCircle(Color.White, w * 0.105f, Offset(w * 0.64f, h * 0.40f))
            drawCircle(ReadingColors.InkDark, w * 0.056f, Offset(w * 0.66f, h * 0.40f))
            drawCircle(Color.White, w * 0.022f, Offset(w * 0.64f, h * 0.37f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.55f), w * 0.052f, Offset(w * 0.56f, h * 0.49f))

            // المنقار الذهبي المبتسم
            val beak = Path().apply {
                moveTo(w * 0.76f, h * 0.39f)
                quadraticBezierTo(w * 0.94f, h * 0.43f, w * 0.77f, h * 0.51f)
                close()
            }
            drawPath(beak, brush = Brush.linearGradient(listOf(ReadingColors.SunYellow, ReadingColors.CoralOrange)))

            // القدمان الصغيرتان
            drawLine(ReadingColors.CoralOrange, Offset(w * 0.44f, h * 0.83f), Offset(w * 0.42f, h * 0.94f), 4f, StrokeCap.Round)
            drawLine(ReadingColors.CoralOrange, Offset(w * 0.58f, h * 0.83f), Offset(w * 0.60f, h * 0.94f), 4f, StrokeCap.Round)
        }

        if (bannerText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ReadingColors.SunYellow,
                shadowElevation = 4.dp,
                modifier = Modifier.border(2.5.dp, Color.White, RoundedCornerShape(20.dp))
            ) {
                Text(
                    text = bannerText,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = ReadingColors.InkDark,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                )
            }
        }
    }
}

/**
 * اتجاه تعشيق قطعة البازل المتشابكة (لسان بارز أو تجويف مطابق).
 */
enum class PuzzleInterlockSide {
    LEFT_TAB,
    RIGHT_SOCKET,
    BOTH_CONNECTED
}

/**
 * قطعة بازل حقيقية متشابكة مرسومة بالكامل بـ Path.cubicTo بألسنة وتجاويف متطابقة،
 * مع حافة بارزة ولمعة ناعمة لعرض مقاطع لعبة البازل (نشاط 6).
 */
@Composable
fun JigsawPuzzlePieceCanvas(
    text: String,
    pieceColor: Color,
    shadowColor: Color,
    interlockSide: PuzzleInterlockSide,
    isHighlighted: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.06f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "puzzle_scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.14f
            val padY = h * 0.10f
            val left = padX
            val right = w - padX
            val top = padY
            val bottom = h - padY * 1.45f
            val r = 18f
            val knobSize = minOf(w * 0.135f, h * 0.19f)
            val midY = (top + bottom) * 0.5f

            fun buildPiecePath(offsetY: Float): Path {
                val t = top + offsetY
                val b = bottom + offsetY
                val my = midY + offsetY
                return Path().apply {
                    moveTo(left + r, t)
                    lineTo(right - r, t)
                    quadraticBezierTo(right, t, right, t + r)

                    // الحافة اليمنى: تجويف داخلي مطابق لاستقبال اللسان
                    if (interlockSide == PuzzleInterlockSide.RIGHT_SOCKET) {
                        lineTo(right, my - knobSize * 0.65f)
                        cubicTo(
                            right - knobSize * 1.35f, my - knobSize * 1.15f,
                            right - knobSize * 1.35f, my + knobSize * 1.15f,
                            right, my + knobSize * 0.65f
                        )
                    }
                    lineTo(right, b - r)
                    quadraticBezierTo(right, b, right - r, b)
                    lineTo(left + r, b)
                    quadraticBezierTo(left, b, left, b - r)

                    // الحافة اليسرى: لسان بارز للخارج يتعشّق داخل التجويف
                    if (interlockSide == PuzzleInterlockSide.LEFT_TAB) {
                        lineTo(left, my + knobSize * 0.65f)
                        cubicTo(
                            left - knobSize * 1.35f, my + knobSize * 1.15f,
                            left - knobSize * 1.35f, my - knobSize * 1.15f,
                            left, my - knobSize * 0.65f
                        )
                    }
                    lineTo(left, t + r)
                    quadraticBezierTo(left, t, left + r, t)
                    close()
                }
            }

            // طبقة العمق السفلية البارزة
            drawPath(
                path = buildPiecePath(offsetY = 8f),
                color = shadowColor
            )

            // جسم قطعة البازل بتدرج مشرق
            val mainPath = buildPiecePath(offsetY = 0f)
            drawPath(
                path = mainPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        pieceColor.copy(alpha = 0.94f),
                        pieceColor,
                        shadowColor.copy(alpha = 0.92f)
                    )
                )
            )

            // إطار أبيض لطيف ولمعة علوية
            drawPath(
                path = mainPath,
                color = if (isHighlighted) ReadingColors.SunYellow else Color.White.copy(alpha = 0.72f),
                style = Stroke(width = if (isHighlighted) 5f else 3f, join = StrokeJoin.Round)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.40f),
                start = Offset(left + 20f, top + 10f),
                end = Offset(right - 20f, top + 10f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
        }

        Text(
            text = text,
            fontFamily = ReadingTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = if (text.length > 3) 24.sp else 30.sp,
            lineHeight = 40.sp,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 6.dp)
        )
    }
}

/**
 * دائرة التحليل الصوتي ذات الأرجل الكرتونية (نشاط 5):
 * دائرة خضراء مبهجة تتمايل قدماها بفرح عند وضع المقطع الصحيح بداخلها.
 */
@Composable
fun WalkingSyllableBubbleCanvas(
    syllableText: String?,
    isFilled: Boolean,
    isCelebrating: Boolean = false,
    accentColor: Color = ReadingColors.MeadowGreen,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "walking_bubble_anim")
    val legSwing by infinite.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCelebrating || isFilled) 420 else 1300, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "legs_swing"
    )
    val hopOffset by animateFloatAsState(
        targetValue = if (isFilled) -7f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bubble_hop"
    )

    Box(
        modifier = modifier.graphicsLayer { translationY = hopOffset },
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val circleCenter = Offset(w * 0.50f, h * 0.42f)
            val radius = minOf(w * 0.42f, h * 0.36f)

            // الأرجل الكرتونية المرحة أسفل الدائرة
            val leftHip = Offset(w * 0.39f, circleCenter.y + radius * 0.85f)
            val rightHip = Offset(w * 0.61f, circleCenter.y + radius * 0.85f)
            val swing = if (isFilled || isCelebrating) legSwing else legSwing * 0.25f

            rotate(degrees = swing, pivot = leftHip) {
                val leftLeg = Path().apply {
                    moveTo(leftHip.x, leftHip.y)
                    quadraticBezierTo(leftHip.x - 5f, h * 0.82f, leftHip.x - 3f, h * 0.87f)
                }
                drawPath(leftLeg, color = ReadingColors.InkDark, style = Stroke(width = 6f, cap = StrokeCap.Round))
                drawOval(
                    color = ReadingColors.CoralOrange,
                    topLeft = Offset(leftHip.x - 16f, h * 0.84f),
                    size = Size(22f, 13f)
                )
            }

            rotate(degrees = -swing, pivot = rightHip) {
                val rightLeg = Path().apply {
                    moveTo(rightHip.x, rightHip.y)
                    quadraticBezierTo(rightHip.x + 5f, h * 0.82f, rightHip.x + 3f, h * 0.87f)
                }
                drawPath(rightLeg, color = ReadingColors.InkDark, style = Stroke(width = 6f, cap = StrokeCap.Round))
                drawOval(
                    color = ReadingColors.SunYellow,
                    topLeft = Offset(rightHip.x - 6f, h * 0.84f),
                    size = Size(22f, 13f)
                )
            }

            // هالة خفيفة حول الدائرة عند الامتلاء
            if (isFilled) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(accentColor.copy(alpha = 0.35f), Color.Transparent),
                        center = circleCenter,
                        radius = radius * 1.24f
                    ),
                    radius = radius * 1.24f,
                    center = circleCenter
                )
            }

            // جسم الدائرة
            drawCircle(
                brush = if (isFilled) {
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF86EFAC), accentColor, Color(0xFF15803D)),
                        center = Offset(circleCenter.x - radius * 0.24f, circleCenter.y - radius * 0.24f),
                        radius = radius * 1.35f
                    )
                } else {
                    Brush.radialGradient(
                        colors = listOf(Color.White, Color(0xFFF0FDF4)),
                        center = circleCenter,
                        radius = radius
                    )
                },
                radius = radius,
                center = circleCenter
            )

            drawCircle(
                color = if (isFilled) Color.White else accentColor.copy(alpha = 0.65f),
                radius = radius,
                center = circleCenter,
                style = Stroke(width = if (isFilled) 4.5f else 3.5f)
            )

            // لمعة زجاجية لطيفة أعلى الدائرة
            val shine = Path().apply {
                moveTo(circleCenter.x - radius * 0.48f, circleCenter.y - radius * 0.44f)
                quadraticBezierTo(circleCenter.x, circleCenter.y - radius * 0.72f, circleCenter.x + radius * 0.38f, circleCenter.y - radius * 0.44f)
            }
            drawPath(
                shine,
                color = Color.White.copy(alpha = if (isFilled) 0.55f else 0.7f),
                style = Stroke(width = 4.5f, cap = StrokeCap.Round)
            )
        }

        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = syllableText ?: "؟",
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = if ((syllableText?.length ?: 0) >= 3) 22.sp else 26.sp,
                lineHeight = 36.sp,
                color = if (isFilled) Color.White else accentColor.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * بطاقة الكلمة المثبّتة بدبوس أحمر لطيف أعلى دوائر التحليل (نشاط 5).
 */
@Composable
fun RedPinWordCard(
    word: String,
    subtitle: String,
    isSolved: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.TopCenter
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = if (isSolved) Color(0xFFDCFCE7) else Color.White,
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .border(
                    width = 2.5.dp,
                    color = if (isSolved) ReadingColors.MeadowGreen else Color(0xFFFCA5A5),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 20.dp, bottom = 14.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = word,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 40.sp,
                    lineHeight = 52.sp,
                    color = if (isSolved) ReadingColors.MeadowShadow else ReadingColors.InkDark,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = subtitle,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.5.sp,
                    color = if (isSolved) ReadingColors.MeadowShadow else ReadingColors.InkSoft,
                    textAlign = TextAlign.Center
                )
            }
        }

        // الدبوس الأحمر اللامع المرسوم على Canvas
        Canvas(modifier = Modifier.size(38.dp)) {
            val w = size.width
            val h = size.height
            // ظل الدبوس
            drawOval(
                color = Color.Black.copy(alpha = 0.16f),
                topLeft = Offset(w * 0.34f, h * 0.72f),
                size = Size(w * 0.36f, h * 0.18f)
            )
            // إبرة الدبوس المعدنية
            drawLine(
                color = Color(0xFF64748B),
                start = Offset(w * 0.50f, h * 0.56f),
                end = Offset(w * 0.50f, h * 0.86f),
                strokeWidth = 4.5f,
                cap = StrokeCap.Round
            )
            // عنق الدبوس الأحمر
            drawRoundRect(
                color = Color(0xFFB91C1C),
                topLeft = Offset(w * 0.35f, h * 0.36f),
                size = Size(w * 0.30f, h * 0.24f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // رأس الدبوس الأحمر الكروي اللامع
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFB91C1C)),
                    center = Offset(w * 0.42f, h * 0.24f),
                    radius = w * 0.34f
                ),
                radius = w * 0.30f,
                center = Offset(w * 0.50f, h * 0.28f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = w * 0.08f,
                center = Offset(w * 0.40f, h * 0.20f)
            )
        }
    }
}

/**
 * مؤشر موجة الصوت البصرية للمد القصير مقابل المد الطويل (نشاط 2):
 * عند المد القصير تنبض موجة قصيرة، وعند المد الطويل تمتد موجة ذهبية طويلة لتعليم الطفل إطالة الصوت.
 */
@Composable
fun VowelWaveIndicatorCanvas(
    isLongVowel: Boolean,
    isActive: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "vowel_wave_anim")
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isLongVowel) 1100 else 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    val stretchFactor by animateFloatAsState(
        targetValue = if (isActive) 1f else 0.42f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "wave_stretch"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h * 0.5f

        if (!isLongVowel) {
            // موجة قصيرة نابضة للمد القصير
            val barCount = 5
            val centerBar = 2
            val totalSpan = w * (0.34f + 0.14f * stretchFactor)
            val startX = (w - totalSpan) * 0.5f
            val step = totalSpan / (barCount - 1)
            for (i in 0 until barCount) {
                val dist = kotlin.math.abs(i - centerBar)
                val pulse = if (isActive) (0.72f + 0.28f * sin(wavePhase + i * 0.9f)) else 0.48f
                val barH = h * (0.68f - dist * 0.16f) * pulse
                val x = startX + i * step
                drawLine(
                    color = if (isActive) accentColor else accentColor.copy(alpha = 0.45f),
                    start = Offset(x, cy - barH * 0.5f),
                    end = Offset(x, cy + barH * 0.5f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
            }
        } else {
            // موجة ذهبية ممتدة طويلة للمد الطويل
            val activeWidth = w * (0.45f + 0.47f * stretchFactor)
            val startX = (w - activeWidth) * 0.5f
            val endX = startX + activeWidth
            val wavePath = Path().apply {
                moveTo(startX, cy)
                val segments = 24
                for (i in 1..segments) {
                    val t = i.toFloat() / segments.toFloat()
                    val x = startX + activeWidth * t
                    val envelope = sin(t * PI).toFloat()
                    val amp = h * (if (isActive) 0.36f else 0.16f) * envelope
                    val y = cy + sin(t * 4f * PI.toFloat() - wavePhase) * amp
                    lineTo(x, y)
                }
            }
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFF59E0B),
                        ReadingColors.SunYellow,
                        Color(0xFFFF7A59),
                        ReadingColors.SunYellow
                    )
                ),
                style = Stroke(width = if (isActive) 7f else 4.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // نقطة متوهجة عند نهاية المد الطويل
            drawCircle(
                color = if (isActive) ReadingColors.SunYellow else ReadingColors.SunYellow.copy(alpha = 0.45f),
                radius = if (isActive) 7f else 4.5f,
                center = Offset(endX, cy)
            )
        }
    }
}

/**
 * أيقونة كتاب قراءتي الجديدة المبهجة لبطاقة الدخول وترويسة المكتبة.
 */
@Composable
fun QiraatiBookIconCanvas(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "qiraati_book_glow")
    val pulse by infinite.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "book_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFFF7B2).copy(alpha = 0.72f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.48f),
                radius = w * 0.48f * pulse
            ),
            radius = w * 0.48f * pulse,
            center = Offset(w * 0.5f, h * 0.48f)
        )

        // غلاف الكتاب البرتقالي الذهبي
        val cover = Path().apply {
            moveTo(w * 0.10f, h * 0.34f)
            quadraticBezierTo(w * 0.50f, h * 0.24f, w * 0.90f, h * 0.34f)
            lineTo(w * 0.88f, h * 0.84f)
            quadraticBezierTo(w * 0.50f, h * 0.74f, w * 0.12f, h * 0.84f)
            close()
        }
        drawPath(
            cover,
            brush = Brush.linearGradient(listOf(ReadingColors.CoralOrange, Color(0xFFD97706)))
        )

        // الصفحتان المفتوحتان
        val leftPage = Path().apply {
            moveTo(w * 0.50f, h * 0.34f)
            cubicTo(w * 0.36f, h * 0.26f, w * 0.20f, h * 0.28f, w * 0.14f, h * 0.36f)
            lineTo(w * 0.16f, h * 0.76f)
            cubicTo(w * 0.28f, h * 0.68f, w * 0.40f, h * 0.70f, w * 0.50f, h * 0.78f)
            close()
        }
        val rightPage = Path().apply {
            moveTo(w * 0.50f, h * 0.34f)
            cubicTo(w * 0.64f, h * 0.26f, w * 0.80f, h * 0.28f, w * 0.86f, h * 0.36f)
            lineTo(w * 0.84f, h * 0.76f)
            cubicTo(w * 0.72f, h * 0.68f, w * 0.60f, h * 0.70f, w * 0.50f, h * 0.78f)
            close()
        }
        drawPath(leftPage, color = Color.White)
        drawPath(rightPage, color = Color(0xFFFFFBEB))

        // أسطر ملونة لطيفة على الصفحات
        drawLine(ReadingColors.SkyBlue, Offset(w * 0.23f, h * 0.46f), Offset(w * 0.42f, h * 0.46f), 4f, StrokeCap.Round)
        drawLine(ReadingColors.MeadowGreen, Offset(w * 0.23f, h * 0.56f), Offset(w * 0.40f, h * 0.56f), 4f, StrokeCap.Round)
        drawLine(ReadingColors.CoralOrange, Offset(w * 0.58f, h * 0.46f), Offset(w * 0.77f, h * 0.46f), 4f, StrokeCap.Round)
        drawLine(ReadingColors.BerryPurple, Offset(w * 0.60f, h * 0.56f), Offset(w * 0.77f, h * 0.56f), 4f, StrokeCap.Round)

        // شمس صغيرة مبتسمة فوق الكتاب
        drawCircle(ReadingColors.SunYellow, w * 0.09f, Offset(w * 0.50f, h * 0.18f))
    }
}

/**
 * مشاهد قصصية كرتونية مرسومة بمنحنيات ناعمة لقسم الاستماع والمحادثة (أنا أتحدث).
 */
@Composable
fun QiraatiStorySceneCanvas(
    artKey: QiraatiStoryArtKey,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "story_scene_anim")
    val sway by infinite.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scene_sway"
    )

    Canvas(modifier = modifier.clip(RoundedCornerShape(22.dp))) {
        val w = size.width
        val h = size.height

        when (artKey) {
            QiraatiStoryArtKey.USRATY_FAMILY_HOME -> {
                // سماء وحديقة وبيت الأسرة الجميل مع الأب والأم والولد والبنت
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF7DD3FC), Color(0xFFDCFCE7), Color(0xFF86EFAC))
                    )
                )
                drawCircle(ReadingColors.SunYellow, w * 0.075f, Offset(w * 0.14f, h * 0.20f))
                // غيمة تتحرك بلطف
                val cx = w * 0.74f + sway * 10f
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.055f, Offset(cx, h * 0.18f))
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.068f, Offset(cx + w * 0.06f, h * 0.16f))
                drawCircle(Color.White.copy(alpha = 0.9f), w * 0.05f, Offset(cx + w * 0.12f, h * 0.19f))

                // حديقة منحنية
                val hill = Path().apply {
                    moveTo(0f, h * 0.74f)
                    cubicTo(w * 0.30f, h * 0.64f, w * 0.70f, h * 0.78f, w, h * 0.68f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(hill, brush = Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF22C55E))))

                // البيت العائلي الدافئ
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A))),
                    topLeft = Offset(w * 0.28f, h * 0.34f),
                    size = Size(w * 0.44f, h * 0.42f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                val roof = Path().apply {
                    moveTo(w * 0.22f, h * 0.38f)
                    quadraticBezierTo(w * 0.50f, h * 0.12f, w * 0.78f, h * 0.38f)
                    close()
                }
                drawPath(roof, brush = Brush.linearGradient(listOf(Color(0xFFFF7A59), Color(0xFFD14B2B))))
                // باب ونافذة
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.46f, h * 0.54f), Size(w * 0.09f, h * 0.22f), CornerRadius(12f, 12f))
                drawCircle(Color(0xFF38BDF8), w * 0.036f, Offset(w * 0.36f, h * 0.49f))
                drawCircle(Color(0xFF38BDF8), w * 0.036f, Offset(w * 0.64f, h * 0.49f))

                // أفراد الأسرة الأربعة مبتسمين أمام البيت
                drawCartoonPerson(w * 0.26f, h * 0.64f, scale = 1.05f, shirtColor = Color(0xFF0284C7))
                drawCartoonPerson(w * 0.40f, h * 0.66f, scale = 0.98f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.58f, h * 0.72f, scale = 0.76f, shirtColor = Color(0xFFF59E0B))
                drawCartoonPerson(w * 0.72f, h * 0.73f, scale = 0.72f, shirtColor = Color(0xFF8B5CF6))

                // قلب دافئ نابض فوق الأسرة
                drawWarmHeart(Offset(w * 0.50f, h * 0.25f + sway * 2.5f), w * 0.038f, Color(0xFFE11D48))
            }

            QiraatiStoryArtKey.USRATY_KITCHEN_HELP -> {
                // مطبخ مشرق دافئ مع طاولة وطعام وأم وأبناء يتعاونون
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFFF7ED), Color(0xFFFED7AA))))
                // نافذة المطبخ المشرقة
                drawRoundRect(Color(0xFFBAE6FD), Offset(w * 0.36f, h * 0.12f), Size(w * 0.28f, h * 0.28f), CornerRadius(14f, 14f))
                drawRoundRect(Color.White, Offset(w * 0.36f, h * 0.12f), Size(w * 0.28f, h * 0.28f), CornerRadius(14f, 14f), style = Stroke(4f))

                // طاولة المطبخ
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFB45309))),
                    topLeft = Offset(w * 0.16f, h * 0.64f),
                    size = Size(w * 0.68f, h * 0.24f),
                    cornerRadius = CornerRadius(16f, 16f)
                )
                // وعاء فواكه وقدر ساخن ببخار متصاعد
                drawArc(Color(0xFFEF4444), 0f, 180f, true, Offset(w * 0.28f, h * 0.50f), Size(w * 0.18f, h * 0.18f))
                drawCircle(ReadingColors.SunYellow, w * 0.03f, Offset(w * 0.35f, h * 0.55f))
                drawCircle(ReadingColors.MeadowGreen, w * 0.028f, Offset(w * 0.40f, h * 0.55f))

                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.54f, h * 0.52f), Size(w * 0.16f, h * 0.14f), CornerRadius(10f, 10f))
                val steam = Path().apply {
                    moveTo(w * 0.62f, h * 0.49f)
                    cubicTo(w * 0.58f + sway * 5f, h * 0.42f, w * 0.66f - sway * 5f, h * 0.36f, w * 0.61f, h * 0.30f)
                }
                drawPath(steam, color = Color.White.copy(alpha = 0.85f), style = Stroke(4f, cap = StrokeCap.Round))

                drawCartoonPerson(w * 0.24f, h * 0.48f, scale = 0.95f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.78f, h * 0.54f, scale = 0.75f, shirtColor = Color(0xFF22C55E))
            }

            QiraatiStoryArtKey.USRATY_FAMILY_READING -> {
                // غرفة جلوس دافئة مع مصباح وكتاب مفتوح وجلسة قراءة عائلية
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFEDE9FE), Color(0xFFDDD6FE))))
                // أريكة مريحة
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    topLeft = Offset(w * 0.14f, h * 0.50f),
                    size = Size(w * 0.72f, h * 0.34f),
                    cornerRadius = CornerRadius(24f, 24f)
                )
                // مصباح دافئ متوهج
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFF7B2).copy(alpha = 0.75f), Color.Transparent)),
                    radius = w * 0.18f,
                    center = Offset(w * 0.82f, h * 0.24f)
                )
                drawCircle(ReadingColors.SunYellow, w * 0.06f, Offset(w * 0.82f, h * 0.24f))

                // الأسرة تقرأ معاً
                drawCartoonPerson(w * 0.30f, h * 0.46f, scale = 0.92f, shirtColor = Color(0xFF6366F1))
                drawCartoonPerson(w * 0.50f, h * 0.52f, scale = 0.76f, shirtColor = Color(0xFFFF7A59))
                drawCartoonPerson(w * 0.68f, h * 0.50f, scale = 0.84f, shirtColor = Color(0xFFEC4899))

                // كتاب مفتوح مضيء في المنتصف
                val bookLeft = Path().apply {
                    moveTo(w * 0.50f, h * 0.68f)
                    quadraticBezierTo(w * 0.40f, h * 0.62f, w * 0.34f, h * 0.69f)
                    lineTo(w * 0.36f, h * 0.82f)
                    quadraticBezierTo(w * 0.43f, h * 0.78f, w * 0.50f, h * 0.83f)
                    close()
                }
                val bookRight = Path().apply {
                    moveTo(w * 0.50f, h * 0.68f)
                    quadraticBezierTo(w * 0.60f, h * 0.62f, w * 0.66f, h * 0.69f)
                    lineTo(w * 0.64f, h * 0.82f)
                    quadraticBezierTo(w * 0.57f, h * 0.78f, w * 0.50f, h * 0.83f)
                    close()
                }
                drawPath(bookLeft, color = Color.White)
                drawPath(bookRight, color = Color(0xFFFFFBEB))
                drawWarmHeart(Offset(w * 0.50f, h * 0.24f + sway * 3f), w * 0.036f, Color(0xFFE11D48))
            }

            QiraatiStoryArtKey.USRATY_HELPING_GRANDMA -> {
                // باب المنزل المقوّس والطفل يساعد جدته بحنان
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFFDE68A))))
                // قوس باب خشبي مزخرف
                val archDoor = Path().apply {
                    moveTo(w * 0.24f, h * 0.88f)
                    lineTo(w * 0.24f, h * 0.42f)
                    cubicTo(w * 0.24f, h * 0.14f, w * 0.76f, h * 0.14f, w * 0.76f, h * 0.42f)
                    lineTo(w * 0.76f, h * 0.88f)
                    close()
                }
                drawPath(archDoor, brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))))
                drawCircle(ReadingColors.SunYellow, w * 0.025f, Offset(w * 0.68f, h * 0.56f))

                // الجدة اللطيفة + الطفل يساعدها
                drawCartoonPerson(w * 0.42f, h * 0.54f, scale = 0.96f, shirtColor = Color(0xFF8B5CF6), hairColor = Color(0xFFE2E8F0))
                drawCartoonPerson(w * 0.60f, h * 0.62f, scale = 0.78f, shirtColor = Color(0xFF22C55E))
                // سلة لطيفة يحملها الطفل
                drawArc(Color(0xFFF59E0B), 0f, 180f, true, Offset(w * 0.64f, h * 0.64f), Size(w * 0.14f, h * 0.16f))
                drawWarmHeart(Offset(w * 0.51f, h * 0.30f + sway * 2.5f), w * 0.038f, Color(0xFFE11D48))
            }

            else -> {
                // غلاف مشوّق للوحدات القادمة
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))
                    )
                )
                drawCircle(Color.White.copy(alpha = 0.18f), w * 0.26f, Offset(w * 0.22f, h * 0.28f))
                drawCircle(ReadingColors.SunYellow.copy(alpha = 0.30f), w * 0.20f, Offset(w * 0.80f, h * 0.72f))
            }
        }
    }
}

private fun DrawScope.drawCartoonPerson(
    cx: Float,
    cy: Float,
    scale: Float,
    shirtColor: Color,
    hairColor: Color = Color(0xFF334155)
) {
    val headR = size.minDimension * 0.105f * scale
    // جسم القميص
    val torso = Path().apply {
        moveTo(cx - headR * 1.1f, cy + headR * 2.2f)
        cubicTo(cx - headR * 1.1f, cy + headR * 0.7f, cx + headR * 1.1f, cy + headR * 0.7f, cx + headR * 1.1f, cy + headR * 2.2f)
        close()
    }
    drawPath(torso, color = shirtColor)
    // الرأس
    drawCircle(color = Color(0xFFFDE2C4), radius = headR, center = Offset(cx, cy))
    // الشعر
    drawArc(
        color = hairColor,
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - headR, cy - headR * 1.04f),
        size = Size(headR * 2f, headR * 1.3f)
    )
    // عينان مبتسمتان
    drawCircle(ReadingColors.InkDark, headR * 0.13f, Offset(cx - headR * 0.32f, cy + headR * 0.04f))
    drawCircle(ReadingColors.InkDark, headR * 0.13f, Offset(cx + headR * 0.32f, cy + headR * 0.04f))
    val smile = Path().apply {
        moveTo(cx - headR * 0.30f, cy + headR * 0.36f)
        quadraticBezierTo(cx, cy + headR * 0.62f, cx + headR * 0.30f, cy + headR * 0.36f)
    }
    drawPath(smile, color = Color(0xFFE11D48), style = Stroke(width = 2.5f, cap = StrokeCap.Round))
}

private fun DrawScope.drawWarmHeart(center: Offset, r: Float, color: Color) {
    val heart = Path().apply {
        moveTo(center.x, center.y + r * 0.9f)
        cubicTo(center.x - r * 1.4f, center.y, center.x - r * 0.8f, center.y - r * 1.1f, center.x, center.y - r * 0.3f)
        cubicTo(center.x + r * 0.8f, center.y - r * 1.1f, center.x + r * 1.4f, center.y, center.x, center.y + r * 0.9f)
        close()
    }
    drawPath(heart, color = color)
}

/**
 * رسوم كرتونية مصغّرة معبّرة بمنحنيات ناعمة لكل كلمة من كلمات الوحدة الأولى
 * (بقرة، برتقال، باب، بوق، طبل، شمس، بومة، دلة، ديك، بيت، بدر، هدهد، فانوس، عصفور، فيل، ضفدع وغيرها).
 */
@Composable
fun QiraatiWordArtCanvas(
    artKey: QiraatiWordArtKey,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        when (artKey) {
            QiraatiWordArtKey.COW -> {
                // بقرة لطيفة بيضاء ببقع بنية وقرنين ذهبيين
                drawOval(Color.White, Offset(w * 0.16f, h * 0.28f), Size(w * 0.68f, h * 0.54f))
                drawOval(Color(0xFF475569), Offset(w * 0.22f, h * 0.34f), Size(w * 0.20f, h * 0.18f))
                drawOval(Color(0xFFFDA4AF), Offset(w * 0.24f, h * 0.56f), Size(w * 0.52f, h * 0.26f))
                drawCircle(ReadingColors.InkDark, w * 0.045f, Offset(w * 0.38f, h * 0.44f))
                drawCircle(ReadingColors.InkDark, w * 0.045f, Offset(w * 0.62f, h * 0.44f))
                drawCircle(Color(0xFF9F1239), w * 0.03f, Offset(w * 0.42f, h * 0.68f))
                drawCircle(Color(0xFF9F1239), w * 0.03f, Offset(w * 0.58f, h * 0.68f))
                val leftHorn = Path().apply {
                    moveTo(w * 0.26f, h * 0.30f)
                    quadraticBezierTo(w * 0.16f, h * 0.12f, w * 0.32f, h * 0.16f)
                }
                val rightHorn = Path().apply {
                    moveTo(w * 0.74f, h * 0.30f)
                    quadraticBezierTo(w * 0.84f, h * 0.12f, w * 0.68f, h * 0.16f)
                }
                drawPath(leftHorn, color = ReadingColors.SunYellow, style = Stroke(6f, cap = StrokeCap.Round))
                drawPath(rightHorn, color = ReadingColors.SunYellow, style = Stroke(6f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.ORANGE -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFDBA74), Color(0xFFF97316), Color(0xFFEA580C))),
                    radius = w * 0.34f,
                    center = Offset(cx, h * 0.56f)
                )
                val leaf = Path().apply {
                    moveTo(cx, h * 0.22f)
                    cubicTo(cx + w * 0.20f, h * 0.08f, cx + w * 0.26f, h * 0.28f, cx, h * 0.26f)
                    close()
                }
                drawPath(leaf, color = ReadingColors.MeadowGreen)
                drawCircle(Color.White.copy(alpha = 0.45f), w * 0.06f, Offset(w * 0.38f, h * 0.44f))
            }

            QiraatiWordArtKey.CARPET -> {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFE11D48), Color(0xFFBE123C))),
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.72f, h * 0.56f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = ReadingColors.SunYellow,
                    topLeft = Offset(w * 0.22f, h * 0.30f),
                    size = Size(w * 0.56f, h * 0.40f),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(4f)
                )
                val diamond = Path().apply {
                    moveTo(cx, h * 0.36f)
                    lineTo(w * 0.64f, cy)
                    lineTo(cx, h * 0.64f)
                    lineTo(w * 0.36f, cy)
                    close()
                }
                drawPath(diamond, color = Color(0xFF38BDF8))
            }

            QiraatiWordArtKey.MILK -> {
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color.White)),
                    topLeft = Offset(w * 0.28f, h * 0.26f),
                    size = Size(w * 0.44f, h * 0.60f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.32f, h * 0.16f), Size(w * 0.36f, h * 0.12f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF38BDF8).copy(alpha = 0.25f), Offset(w * 0.30f, h * 0.46f), Size(w * 0.40f, h * 0.22f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.BREAD -> {
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFB45309))),
                    topLeft = Offset(w * 0.12f, h * 0.26f),
                    size = Size(w * 0.76f, h * 0.52f)
                )
                drawLine(Color(0xFF92400E), Offset(w * 0.30f, h * 0.40f), Offset(w * 0.38f, h * 0.60f), 4f, StrokeCap.Round)
                drawLine(Color(0xFF92400E), Offset(w * 0.48f, h * 0.36f), Offset(w * 0.56f, h * 0.62f), 4f, StrokeCap.Round)
                drawLine(Color(0xFF92400E), Offset(w * 0.64f, h * 0.40f), Offset(w * 0.70f, h * 0.58f), 4f, StrokeCap.Round)
            }

            QiraatiWordArtKey.DOOR -> {
                val door = Path().apply {
                    moveTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.22f, h * 0.34f)
                    cubicTo(w * 0.22f, h * 0.12f, w * 0.78f, h * 0.12f, w * 0.78f, h * 0.34f)
                    lineTo(w * 0.78f, h * 0.88f)
                    close()
                }
                drawPath(door, brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))))
                drawCircle(ReadingColors.SunYellow, w * 0.065f, Offset(w * 0.66f, h * 0.56f))
            }

            QiraatiWordArtKey.BUGLE -> {
                val horn = Path().apply {
                    moveTo(w * 0.12f, h * 0.56f)
                    cubicTo(w * 0.38f, h * 0.48f, w * 0.62f, h * 0.38f, w * 0.82f, h * 0.20f)
                    lineTo(w * 0.88f, h * 0.76f)
                    cubicTo(w * 0.64f, h * 0.60f, w * 0.38f, h * 0.60f, w * 0.12f, h * 0.64f)
                    close()
                }
                drawPath(horn, brush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFD97706))))
                drawOval(Color(0xFF92400E), Offset(w * 0.78f, h * 0.24f), Size(w * 0.12f, h * 0.48f))
            }

            QiraatiWordArtKey.SPRING -> {
                // زهرة ربيع مبهجة
                val petalColors = listOf(Color(0xFFEC4899), Color(0xFFFF7A59), Color(0xFF8B5CF6), Color(0xFF38BDF8), Color(0xFFF43F5E))
                for (i in 0 until 5) {
                    val a = i * (2.0 * PI / 5.0)
                    drawCircle(
                        color = petalColors[i],
                        radius = w * 0.15f,
                        center = Offset(cx + (w * 0.19f * cos(a)).toFloat(), h * 0.42f + (h * 0.19f * sin(a)).toFloat())
                    )
                }
                drawCircle(ReadingColors.SunYellow, w * 0.13f, Offset(cx, h * 0.42f))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.56f), Offset(cx, h * 0.88f), 7f, StrokeCap.Round)
            }

            QiraatiWordArtKey.ONION -> {
                val bulb = Path().apply {
                    moveTo(cx, h * 0.26f)
                    cubicTo(w * 0.82f, h * 0.42f, w * 0.84f, h * 0.82f, cx, h * 0.86f)
                    cubicTo(w * 0.16f, h * 0.82f, w * 0.18f, h * 0.42f, cx, h * 0.26f)
                    close()
                }
                drawPath(bulb, brush = Brush.radialGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706))))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.28f), Offset(w * 0.44f, h * 0.12f), 5f, StrokeCap.Round)
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.28f), Offset(w * 0.58f, h * 0.12f), 5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.WATERMELON -> {
                drawArc(ReadingColors.MeadowGreen, 0f, 180f, true, Offset(w * 0.10f, h * 0.26f), Size(w * 0.80f, h * 0.62f))
                drawArc(Color(0xFFEF4444), 0f, 180f, true, Offset(w * 0.17f, h * 0.32f), Size(w * 0.66f, h * 0.48f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.38f, h * 0.62f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.50f, h * 0.66f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.62f, h * 0.62f))
            }

            QiraatiWordArtKey.DRUM -> {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFEF4444), ReadingColors.CoralOrange, Color(0xFFDC2626))),
                    topLeft = Offset(w * 0.16f, h * 0.40f),
                    size = Size(w * 0.68f, h * 0.44f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawOval(Color(0xFFFFFBEB), Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.18f))
                drawLine(Color(0xFF78350F), Offset(w * 0.22f, h * 0.16f), Offset(w * 0.48f, h * 0.40f), 5f, StrokeCap.Round)
                drawLine(Color(0xFF78350F), Offset(w * 0.78f, h * 0.16f), Offset(w * 0.52f, h * 0.40f), 5f, StrokeCap.Round)
                drawCircle(ReadingColors.SunYellow, 6f, Offset(w * 0.48f, h * 0.40f))
                drawCircle(ReadingColors.SunYellow, 6f, Offset(w * 0.52f, h * 0.40f))
            }

            QiraatiWordArtKey.CHEESE -> {
                val wedge = Path().apply {
                    moveTo(w * 0.14f, h * 0.74f)
                    lineTo(w * 0.86f, h * 0.74f)
                    lineTo(w * 0.78f, h * 0.28f)
                    close()
                }
                drawPath(wedge, brush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow)))
                drawCircle(Color(0xFFD97706), w * 0.06f, Offset(w * 0.54f, h * 0.60f))
                drawCircle(Color(0xFFD97706), w * 0.045f, Offset(w * 0.68f, h * 0.52f))
                drawCircle(Color(0xFFD97706), w * 0.04f, Offset(w * 0.38f, h * 0.66f))
            }

            QiraatiWordArtKey.LION_CUB, QiraatiWordArtKey.LION -> {
                drawCircle(Color(0xFFD97706), w * 0.36f, Offset(cx, cy))
                drawCircle(Color(0xFFFDE68A), w * 0.25f, Offset(cx, cy + h * 0.02f))
                drawCircle(ReadingColors.InkDark, w * 0.04f, Offset(w * 0.42f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, w * 0.04f, Offset(w * 0.58f, h * 0.46f))
                drawCircle(Color(0xFF92400E), w * 0.045f, Offset(cx, h * 0.56f))
            }

            QiraatiWordArtKey.MAMA -> {
                drawWarmHeart(Offset(cx, cy), w * 0.32f, Color(0xFFEC4899))
                drawWarmHeart(Offset(cx, cy), w * 0.18f, Color(0xFFFCE7F3))
            }

            QiraatiWordArtKey.BANANA -> {
                val banana = Path().apply {
                    moveTo(w * 0.22f, h * 0.24f)
                    cubicTo(w * 0.14f, h * 0.68f, w * 0.56f, h * 0.88f, w * 0.84f, h * 0.64f)
                    cubicTo(w * 0.56f, h * 0.68f, w * 0.36f, h * 0.52f, w * 0.32f, h * 0.22f)
                    close()
                }
                drawPath(banana, brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), ReadingColors.SunYellow)))
            }

            QiraatiWordArtKey.SCISSORS -> {
                drawLine(Color(0xFF94A3B8), Offset(w * 0.34f, h * 0.66f), Offset(w * 0.78f, h * 0.20f), 7f, StrokeCap.Round)
                drawLine(Color(0xFF64748B), Offset(w * 0.66f, h * 0.66f), Offset(w * 0.22f, h * 0.20f), 7f, StrokeCap.Round)
                drawCircle(Color(0xFFEC4899), w * 0.12f, Offset(w * 0.30f, h * 0.72f), style = Stroke(6f))
                drawCircle(Color(0xFF0EA5E9), w * 0.12f, Offset(w * 0.70f, h * 0.72f), style = Stroke(6f))
            }

            QiraatiWordArtKey.PEN -> {
                rotate(degrees = -35f, pivot = Offset(cx, cy)) {
                    drawRoundRect(ReadingColors.SkyBlue, Offset(w * 0.20f, h * 0.42f), Size(w * 0.54f, h * 0.16f), CornerRadius(8f, 8f))
                    val tip = Path().apply {
                        moveTo(w * 0.74f, h * 0.42f)
                        lineTo(w * 0.90f, h * 0.50f)
                        lineTo(w * 0.74f, h * 0.58f)
                        close()
                    }
                    drawPath(tip, color = ReadingColors.SunYellow)
                }
            }

            QiraatiWordArtKey.SUN -> {
                for (i in 0 until 8) {
                    val a = i * (2.0 * PI / 8.0)
                    drawLine(
                        ReadingColors.SunYellow,
                        Offset(cx + (w * 0.28f * cos(a)).toFloat(), cy + (h * 0.28f * sin(a)).toFloat()),
                        Offset(cx + (w * 0.44f * cos(a)).toFloat(), cy + (h * 0.44f * sin(a)).toFloat()),
                        5f,
                        StrokeCap.Round
                    )
                }
                drawCircle(ReadingColors.SunYellow, w * 0.24f, Offset(cx, cy))
            }

            QiraatiWordArtKey.WATER -> {
                val drop = Path().apply {
                    moveTo(cx, h * 0.14f)
                    cubicTo(w * 0.82f, h * 0.52f, w * 0.80f, h * 0.86f, cx, h * 0.86f)
                    cubicTo(w * 0.20f, h * 0.86f, w * 0.18f, h * 0.52f, cx, h * 0.14f)
                    close()
                }
                drawPath(drop, brush = Brush.radialGradient(listOf(Color(0xFFBAE6FD), Color(0xFF0EA5E9), Color(0xFF0284C7))))
                drawCircle(Color.White.copy(alpha = 0.65f), w * 0.055f, Offset(w * 0.42f, h * 0.56f))
            }

            QiraatiWordArtKey.OWL -> {
                drawOval(Color(0xFF8B5CF6), Offset(w * 0.22f, h * 0.20f), Size(w * 0.56f, h * 0.66f))
                drawCircle(Color.White, w * 0.12f, Offset(w * 0.39f, h * 0.42f))
                drawCircle(Color.White, w * 0.12f, Offset(w * 0.61f, h * 0.42f))
                drawCircle(ReadingColors.InkDark, w * 0.055f, Offset(w * 0.39f, h * 0.42f))
                drawCircle(ReadingColors.InkDark, w * 0.055f, Offset(w * 0.61f, h * 0.42f))
                val beak = Path().apply {
                    moveTo(cx, h * 0.48f)
                    lineTo(w * 0.44f, h * 0.58f)
                    lineTo(w * 0.56f, h * 0.58f)
                    close()
                }
                drawPath(beak, color = ReadingColors.SunYellow)
            }

            QiraatiWordArtKey.POMEGRANATE -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFDA4AF), Color(0xFFE11D48), Color(0xFF9F1239))),
                    radius = w * 0.32f,
                    center = Offset(cx, h * 0.58f)
                )
                val crown = Path().apply {
                    moveTo(w * 0.38f, h * 0.30f)
                    lineTo(w * 0.42f, h * 0.16f)
                    lineTo(cx, h * 0.26f)
                    lineTo(w * 0.58f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.30f)
                    close()
                }
                drawPath(crown, color = Color(0xFFBE123C))
            }

            QiraatiWordArtKey.RAIN -> {
                drawCircle(Color(0xFF93C5FD), w * 0.17f, Offset(w * 0.34f, h * 0.38f))
                drawCircle(Color(0xFF60A5FA), w * 0.21f, Offset(w * 0.52f, h * 0.32f))
                drawCircle(Color(0xFF93C5FD), w * 0.16f, Offset(w * 0.68f, h * 0.38f))
                drawLine(Color(0xFF0284C7), Offset(w * 0.34f, h * 0.60f), Offset(w * 0.30f, h * 0.78f), 5f, StrokeCap.Round)
                drawLine(Color(0xFF0284C7), Offset(w * 0.50f, h * 0.62f), Offset(w * 0.46f, h * 0.80f), 5f, StrokeCap.Round)
                drawLine(Color(0xFF0284C7), Offset(w * 0.66f, h * 0.60f), Offset(w * 0.62f, h * 0.78f), 5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.TEACHER -> {
                drawRoundRect(Color(0xFF15803D), Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.54f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.54f), CornerRadius(12f, 12f), style = Stroke(5f))
                drawLine(Color.White, Offset(w * 0.26f, h * 0.38f), Offset(w * 0.64f, h * 0.38f), 4f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(w * 0.26f, h * 0.54f), Offset(w * 0.56f, h * 0.54f), 4f, StrokeCap.Round)
            }

            QiraatiWordArtKey.KEY -> {
                drawCircle(ReadingColors.SunYellow, w * 0.18f, Offset(w * 0.32f, cy), style = Stroke(8f))
                drawLine(ReadingColors.SunYellow, Offset(w * 0.48f, cy), Offset(w * 0.86f, cy), 8f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(w * 0.72f, cy), Offset(w * 0.72f, h * 0.66f), 7f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(w * 0.84f, cy), Offset(w * 0.84f, h * 0.64f), 7f, StrokeCap.Round)
            }

            QiraatiWordArtKey.SCALE -> {
                drawLine(Color(0xFF0284C7), Offset(cx, h * 0.22f), Offset(cx, h * 0.82f), 6f, StrokeCap.Round)
                drawLine(Color(0xFF0284C7), Offset(w * 0.20f, h * 0.34f), Offset(w * 0.80f, h * 0.34f), 6f, StrokeCap.Round)
                drawArc(ReadingColors.SunYellow, 0f, 180f, true, Offset(w * 0.12f, h * 0.44f), Size(w * 0.24f, h * 0.18f))
                drawArc(ReadingColors.SunYellow, 0f, 180f, true, Offset(w * 0.64f, h * 0.44f), Size(w * 0.24f, h * 0.18f))
                drawLine(Color(0xFF0284C7), Offset(w * 0.32f, h * 0.82f), Offset(w * 0.68f, h * 0.82f), 7f, StrokeCap.Round)
            }

            QiraatiWordArtKey.SAND -> {
                val dune = Path().apply {
                    moveTo(w * 0.08f, h * 0.82f)
                    cubicTo(w * 0.30f, h * 0.36f, w * 0.66f, h * 0.36f, w * 0.92f, h * 0.82f)
                    close()
                }
                drawPath(dune, brush = Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))))
            }

            QiraatiWordArtKey.ANT -> {
                drawCircle(Color(0xFF78350F), w * 0.12f, Offset(w * 0.28f, h * 0.56f))
                drawCircle(Color(0xFF92400E), w * 0.10f, Offset(w * 0.50f, h * 0.54f))
                drawCircle(Color(0xFF78350F), w * 0.13f, Offset(w * 0.72f, h * 0.52f))
                drawCircle(Color.White, 3f, Offset(w * 0.76f, h * 0.48f))
            }

            QiraatiWordArtKey.DALLAH -> {
                // دلة قهوة عربية ذهبية أنيقة
                drawOval(
                    brush = Brush.linearGradient(listOf(Color(0xFFFFF3A1), ReadingColors.SunYellow, Color(0xFFB45309))),
                    topLeft = Offset(w * 0.28f, h * 0.40f),
                    size = Size(w * 0.44f, h * 0.44f)
                )
                val spout = Path().apply {
                    moveTo(w * 0.68f, h * 0.54f)
                    quadraticBezierTo(w * 0.92f, h * 0.42f, w * 0.86f, h * 0.22f)
                }
                drawPath(spout, color = Color(0xFFD97706), style = Stroke(7f, cap = StrokeCap.Round))
                val lid = Path().apply {
                    moveTo(w * 0.36f, h * 0.40f)
                    lineTo(cx, h * 0.14f)
                    lineTo(w * 0.64f, h * 0.40f)
                    close()
                }
                drawPath(lid, color = Color(0xFFD97706))
            }

            QiraatiWordArtKey.BEAR -> {
                drawCircle(Color(0xFF92400E), w * 0.11f, Offset(w * 0.26f, h * 0.28f))
                drawCircle(Color(0xFF92400E), w * 0.11f, Offset(w * 0.74f, h * 0.28f))
                drawCircle(Color(0xFFB45309), w * 0.32f, Offset(cx, h * 0.54f))
                drawOval(Color(0xFFFDE68A), Offset(w * 0.34f, h * 0.52f), Size(w * 0.32f, h * 0.22f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.40f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.60f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 4.5f, Offset(cx, h * 0.58f))
            }

            QiraatiWordArtKey.DIRHAM -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706))),
                    radius = w * 0.34f,
                    center = Offset(cx, cy)
                )
                drawCircle(Color.White.copy(alpha = 0.7f), w * 0.25f, Offset(cx, cy), style = Stroke(3.5f))
            }

            QiraatiWordArtKey.POT -> {
                drawRoundRect(Color(0xFF64748B), Offset(w * 0.20f, h * 0.40f), Size(w * 0.60f, h * 0.42f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.16f, h * 0.32f), Size(w * 0.68f, h * 0.10f), CornerRadius(8f, 8f))
                drawCircle(Color(0xFF475569), w * 0.06f, Offset(cx, h * 0.28f))
            }

            QiraatiWordArtKey.HOUSE, QiraatiWordArtKey.HOUSES -> {
                drawRoundRect(Color(0xFFFEF3C7), Offset(w * 0.20f, h * 0.44f), Size(w * 0.60f, h * 0.42f), CornerRadius(10f, 10f))
                val roof = Path().apply {
                    moveTo(w * 0.12f, h * 0.48f)
                    lineTo(cx, h * 0.16f)
                    lineTo(w * 0.88f, h * 0.48f)
                    close()
                }
                drawPath(roof, color = ReadingColors.CoralOrange)
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.42f, h * 0.60f), Size(w * 0.16f, h * 0.26f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.WORM -> {
                drawCircle(Color(0xFF22C55E), w * 0.13f, Offset(w * 0.24f, h * 0.60f))
                drawCircle(Color(0xFF4ADE80), w * 0.14f, Offset(w * 0.42f, h * 0.52f))
                drawCircle(Color(0xFF22C55E), w * 0.14f, Offset(w * 0.60f, h * 0.56f))
                drawCircle(Color(0xFF86EFAC), w * 0.15f, Offset(w * 0.76f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.80f, h * 0.44f))
            }

            QiraatiWordArtKey.ROOSTER -> {
                drawOval(Color(0xFFF59E0B), Offset(w * 0.22f, h * 0.42f), Size(w * 0.52f, h * 0.36f))
                drawCircle(Color(0xFFEF4444), w * 0.08f, Offset(w * 0.62f, h * 0.22f))
                drawCircle(Color(0xFFEF4444), w * 0.07f, Offset(w * 0.72f, h * 0.25f))
                drawCircle(Color(0xFFFDE68A), w * 0.15f, Offset(w * 0.68f, h * 0.38f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.72f, h * 0.36f))
            }

            QiraatiWordArtKey.DOLL -> {
                drawCircle(Color(0xFFFDE2C4), w * 0.18f, Offset(cx, h * 0.32f))
                val dress = Path().apply {
                    moveTo(cx, h * 0.48f)
                    lineTo(w * 0.24f, h * 0.84f)
                    lineTo(w * 0.76f, h * 0.84f)
                    close()
                }
                drawPath(dress, color = Color(0xFFEC4899))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.44f, h * 0.32f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.56f, h * 0.32f))
            }

            QiraatiWordArtKey.STAIRS -> {
                val steps = Path().apply {
                    moveTo(w * 0.16f, h * 0.82f)
                    lineTo(w * 0.16f, h * 0.66f)
                    lineTo(w * 0.36f, h * 0.66f)
                    lineTo(w * 0.36f, h * 0.50f)
                    lineTo(w * 0.56f, h * 0.50f)
                    lineTo(w * 0.56f, h * 0.34f)
                    lineTo(w * 0.84f, h * 0.34f)
                    lineTo(w * 0.84f, h * 0.82f)
                    close()
                }
                drawPath(steps, brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
            }

            QiraatiWordArtKey.DATE_SYRUP -> {
                drawRoundRect(Color(0xFF78350F), Offset(w * 0.26f, h * 0.32f), Size(w * 0.48f, h * 0.54f), CornerRadius(14f, 14f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.32f, h * 0.20f), Size(w * 0.36f, h * 0.12f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFFDE68A), Offset(w * 0.32f, h * 0.48f), Size(w * 0.36f, h * 0.20f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.FULL_MOON -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFFACC15))),
                    radius = w * 0.35f,
                    center = Offset(cx, cy)
                )
                drawCircle(Color(0xFFEAB308).copy(alpha = 0.35f), w * 0.08f, Offset(w * 0.38f, h * 0.40f))
                drawCircle(Color(0xFFEAB308).copy(alpha = 0.30f), w * 0.06f, Offset(w * 0.60f, h * 0.58f))
            }

            QiraatiWordArtKey.HOOPOE -> {
                drawOval(Color(0xFFF59E0B), Offset(w * 0.20f, h * 0.44f), Size(w * 0.50f, h * 0.34f))
                drawCircle(Color(0xFFD97706), w * 0.14f, Offset(w * 0.66f, h * 0.42f))
                // تاج الهدهد المميز
                for (i in 0..3) {
                    val x = w * (0.54f + i * 0.06f)
                    drawLine(Color(0xFFEA580C), Offset(w * 0.64f, h * 0.30f), Offset(x, h * 0.14f), 4f, StrokeCap.Round)
                    drawCircle(ReadingColors.InkDark, 3f, Offset(x, h * 0.14f))
                }
                drawLine(Color(0xFF78350F), Offset(w * 0.78f, h * 0.42f), Offset(w * 0.94f, h * 0.46f), 4f, StrokeCap.Round)
            }

            QiraatiWordArtKey.CHEETAH -> {
                drawCircle(Color(0xFFF59E0B), w * 0.32f, Offset(cx, h * 0.54f))
                drawCircle(Color(0xFFD97706), w * 0.10f, Offset(w * 0.26f, h * 0.28f))
                drawCircle(Color(0xFFD97706), w * 0.10f, Offset(w * 0.74f, h * 0.28f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.36f, h * 0.36f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.64f, h * 0.36f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.42f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.58f, h * 0.50f))
            }

            QiraatiWordArtKey.OVEN -> {
                drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.16f, h * 0.22f), Size(w * 0.68f, h * 0.64f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFF1E293B), Offset(w * 0.26f, h * 0.44f), Size(w * 0.48f, h * 0.32f), CornerRadius(10f, 10f))
                drawCircle(ReadingColors.CoralOrange, 4.5f, Offset(w * 0.34f, h * 0.32f))
                drawCircle(ReadingColors.SunYellow, 4.5f, Offset(w * 0.50f, h * 0.32f))
                drawCircle(ReadingColors.MeadowGreen, 4.5f, Offset(w * 0.66f, h * 0.32f))
            }

            QiraatiWordArtKey.CUP -> {
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.22f, h * 0.38f), Size(w * 0.46f, h * 0.38f), CornerRadius(14f, 14f))
                drawCircle(Color(0xFF0284C7), w * 0.10f, Offset(w * 0.72f, h * 0.56f), style = Stroke(6f))
                drawLine(Color(0xFF0284C7), Offset(w * 0.18f, h * 0.80f), Offset(w * 0.76f, h * 0.80f), 6f, StrokeCap.Round)
            }

            QiraatiWordArtKey.LANTERN -> {
                // فانوس مضيء
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFF3A1), Color.Transparent)),
                    radius = w * 0.42f,
                    center = Offset(cx, h * 0.54f)
                )
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.30f, h * 0.34f), Size(w * 0.40f, h * 0.44f), CornerRadius(12f, 12f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.38f, h * 0.42f), Size(w * 0.24f, h * 0.28f), CornerRadius(8f, 8f))
                val cap = Path().apply {
                    moveTo(w * 0.26f, h * 0.34f)
                    lineTo(cx, h * 0.16f)
                    lineTo(w * 0.74f, h * 0.34f)
                    close()
                }
                drawPath(cap, color = Color(0xFFD97706))
            }

            QiraatiWordArtKey.SPARROW -> {
                drawOval(Color(0xFF38BDF8), Offset(w * 0.22f, h * 0.36f), Size(w * 0.52f, h * 0.38f))
                drawCircle(Color(0xFF0EA5E9), w * 0.15f, Offset(w * 0.66f, h * 0.38f))
                val beak = Path().apply {
                    moveTo(w * 0.78f, h * 0.36f)
                    lineTo(w * 0.92f, h * 0.42f)
                    lineTo(w * 0.78f, h * 0.46f)
                    close()
                }
                drawPath(beak, color = ReadingColors.SunYellow)
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.70f, h * 0.36f))
            }

            QiraatiWordArtKey.ELEPHANT -> {
                // فيل لطيف بأذنين كبيرتين وخرطوم منحني
                drawCircle(Color(0xFF94A3B8), w * 0.18f, Offset(w * 0.24f, h * 0.48f))
                drawCircle(Color(0xFF94A3B8), w * 0.18f, Offset(w * 0.76f, h * 0.48f))
                drawCircle(Color(0xFF64748B), w * 0.28f, Offset(cx, h * 0.48f))
                val trunk = Path().apply {
                    moveTo(cx, h * 0.58f)
                    quadraticBezierTo(cx, h * 0.86f, w * 0.68f, h * 0.82f)
                }
                drawPath(trunk, color = Color(0xFF64748B), style = Stroke(11f, cap = StrokeCap.Round))
                drawCircle(Color.White, 4.5f, Offset(w * 0.40f, h * 0.44f))
                drawCircle(Color.White, 4.5f, Offset(w * 0.60f, h * 0.44f))
                drawCircle(ReadingColors.InkDark, 2.5f, Offset(w * 0.40f, h * 0.44f))
                drawCircle(ReadingColors.InkDark, 2.5f, Offset(w * 0.60f, h * 0.44f))
            }

            QiraatiWordArtKey.MOUTH -> {
                val smile = Path().apply {
                    moveTo(w * 0.18f, h * 0.48f)
                    quadraticBezierTo(cx, h * 0.32f, w * 0.82f, h * 0.48f)
                    quadraticBezierTo(cx, h * 0.82f, w * 0.18f, h * 0.48f)
                    close()
                }
                drawPath(smile, color = Color(0xFFE11D48))
                drawRoundRect(Color.White, Offset(w * 0.34f, h * 0.48f), Size(w * 0.32f, h * 0.10f), CornerRadius(4f, 4f))
            }

            QiraatiWordArtKey.TAMBOURINES -> {
                drawCircle(Color(0xFFFDE68A), w * 0.30f, Offset(cx, cy))
                drawCircle(Color(0xFFD97706), w * 0.30f, Offset(cx, cy), style = Stroke(8f))
                for (i in 0 until 6) {
                    val a = i * (2.0 * PI / 6.0)
                    drawCircle(
                        ReadingColors.SunYellow,
                        w * 0.055f,
                        Offset(cx + (w * 0.34f * cos(a)).toFloat(), cy + (h * 0.34f * sin(a)).toFloat())
                    )
                }
            }

            QiraatiWordArtKey.FROG -> {
                // ضفدع أخضر مبتسم لطيف
                drawCircle(Color(0xFF22C55E), w * 0.12f, Offset(w * 0.32f, h * 0.30f))
                drawCircle(Color(0xFF22C55E), w * 0.12f, Offset(w * 0.68f, h * 0.30f))
                drawOval(Color(0xFF22C55E), Offset(w * 0.16f, h * 0.34f), Size(w * 0.68f, h * 0.46f))
                drawCircle(Color.White, w * 0.07f, Offset(w * 0.32f, h * 0.30f))
                drawCircle(Color.White, w * 0.07f, Offset(w * 0.68f, h * 0.30f))
                drawCircle(ReadingColors.InkDark, w * 0.035f, Offset(w * 0.32f, h * 0.30f))
                drawCircle(ReadingColors.InkDark, w * 0.035f, Offset(w * 0.68f, h * 0.30f))
                val frogSmile = Path().apply {
                    moveTo(w * 0.30f, h * 0.58f)
                    quadraticBezierTo(cx, h * 0.72f, w * 0.70f, h * 0.58f)
                }
                drawPath(frogSmile, color = Color(0xFF14532D), style = Stroke(4.5f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.PARTY -> {
                drawOval(ReadingColors.CoralOrange, Offset(w * 0.16f, h * 0.16f), Size(w * 0.30f, h * 0.38f))
                drawOval(ReadingColors.SkyBlue, Offset(w * 0.52f, h * 0.14f), Size(w * 0.30f, h * 0.38f))
                drawOval(ReadingColors.SunYellow, Offset(w * 0.34f, h * 0.28f), Size(w * 0.30f, h * 0.38f))
                drawLine(Color(0xFF64748B), Offset(w * 0.31f, h * 0.54f), Offset(cx, h * 0.86f), 3f)
                drawLine(Color(0xFF64748B), Offset(w * 0.67f, h * 0.52f), Offset(cx, h * 0.86f), 3f)
            }

            QiraatiWordArtKey.CHILD -> {
                drawCartoonPerson(cx, h * 0.38f, scale = 1.25f, shirtColor = ReadingColors.SkyBlue)
            }

            QiraatiWordArtKey.GENERIC_WORD -> {
                drawCircle(ReadingColors.SunYellow, w * 0.32f, Offset(cx, cy))
                drawCircle(Color.White, w * 0.16f, Offset(cx, cy))
            }
        }
    }
}
