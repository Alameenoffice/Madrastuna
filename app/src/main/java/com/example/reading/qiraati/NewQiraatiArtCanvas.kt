package com.example.reading.qiraati

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.launch
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
    modifier: Modifier = Modifier,
    onTap: (() -> Unit)? = null
) {
    val infinite = rememberInfiniteTransition(label = "blue_bird_anim")
    var tapCount by remember { mutableIntStateOf(0) }
    val tapJump = remember { Animatable(0f) }
    val tapTilt = remember { Animatable(0f) }

    LaunchedEffect(tapCount) {
        if (tapCount > 0) {
            launch {
                tapJump.snapTo(0f)
                tapJump.animateTo(-18f, tween(130, easing = FastOutSlowInEasing))
                tapJump.animateTo(
                    0f,
                    spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                )
            }
            launch {
                tapTilt.animateTo(9f, tween(90))
                tapTilt.animateTo(-9f, tween(100))
                tapTilt.animateTo(0f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
            }
        }
    }

    val isExcited = isCelebrating || tapJump.value < -2f
    val wingFlap by infinite.animateFloat(
        initialValue = -14f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isExcited) 220 else 820, easing = FastOutSlowInEasing),
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
    val breatheScale by infinite.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bird_breathe"
    )
    val blinkPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bird_blink"
    )
    val isBlinking = blinkPhase > 0.93f && !isExcited

    val interactiveModifier = if (onTap != null) {
        val tapSource = remember { MutableInteractionSource() }
        modifier.clickable(interactionSource = tapSource, indication = null) {
            tapCount++
            onTap()
        }
    } else {
        modifier
    }

    Row(
        modifier = interactiveModifier.graphicsLayer {
            translationY = bobY + tapJump.value
            rotationZ = tapTilt.value
            scaleX = breatheScale
            scaleY = breatheScale
        },
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

            // العين المشرقة والوجنة الوردية مع رمش طبيعي وابتسامة فرح عند القفز
            drawCircle(Color.White, w * 0.105f, Offset(w * 0.64f, h * 0.40f))
            if (isBlinking || isExcited) {
                val eyeArc = Path().apply {
                    moveTo(w * 0.59f, h * 0.41f)
                    quadraticBezierTo(w * 0.65f, h * 0.35f, w * 0.70f, h * 0.41f)
                }
                drawPath(eyeArc, color = ReadingColors.InkDark, style = Stroke(width = 3.2f, cap = StrokeCap.Round))
            } else {
                drawCircle(ReadingColors.InkDark, w * 0.056f, Offset(w * 0.66f, h * 0.40f))
                drawCircle(Color.White, w * 0.022f, Offset(w * 0.64f, h * 0.37f))
            }
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
 * شخصية القلم الأزرق المبتسم (مرافق فقرة "أقرأ" في الوحدة الثالثة: وطني):
 * قلم أزرق ودود مرسوم بالكامل على Canvas بمنحنيات ناعمة وحركة تمايل لطيفة وابتسامة مشرقة.
 */
@Composable
fun ReadingBluePenCanvas(
    bannerText: String = "أقرأ",
    isCelebrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "blue_pen_anim")
    val tiltDeg by infinite.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCelebrating) 320 else 1350, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pen_tilt"
    )
    val bobY by infinite.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pen_bob"
    )

    Row(
        modifier = modifier.graphicsLayer { translationY = bobY },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(
            modifier = Modifier
                .size(74.dp)
                .graphicsLayer { rotationZ = tiltDeg }
        ) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f

            // هالة سماوية مضيئة خلف القلم
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFBAE6FD).copy(alpha = 0.60f), Color.Transparent),
                    center = Offset(cx, h * 0.50f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(cx, h * 0.50f)
            )

            // غطاء علوي ذهبي وقوس ممحاة وردية ناعمة
            val cap = Path().apply {
                moveTo(w * 0.34f, h * 0.24f)
                cubicTo(w * 0.34f, h * 0.08f, w * 0.66f, h * 0.08f, w * 0.66f, h * 0.24f)
                close()
            }
            drawPath(cap, brush = Brush.verticalGradient(listOf(Color(0xFFFDA4AF), Color(0xFFEC4899))))
            drawRoundRect(
                brush = Brush.horizontalGradient(listOf(Color(0xFFFEF08A), ReadingColors.SunYellow, Color(0xFFD97706))),
                topLeft = Offset(w * 0.32f, h * 0.22f),
                size = Size(w * 0.36f, h * 0.09f),
                cornerRadius = CornerRadius(6f, 6f)
            )

            // جسم القلم الأزرق المشرق بمنحنيات ناعمة
            val body = Path().apply {
                moveTo(w * 0.32f, h * 0.30f)
                lineTo(w * 0.68f, h * 0.30f)
                lineTo(w * 0.66f, h * 0.72f)
                quadraticBezierTo(cx, h * 0.76f, w * 0.34f, h * 0.72f)
                close()
            }
            drawPath(
                body,
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF7DD3FC), Color(0xFF0EA5E9), Color(0xFF0284C7))
                )
            )

            // لمعة زجاجية جانبية على جسم القلم
            drawLine(
                color = Color.White.copy(alpha = 0.50f),
                start = Offset(w * 0.40f, h * 0.34f),
                end = Offset(w * 0.40f, h * 0.66f),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )

            // مشبك القلم الذهبي الجانبي
            val clipPath = Path().apply {
                moveTo(w * 0.68f, h * 0.26f)
                quadraticBezierTo(w * 0.80f, h * 0.28f, w * 0.77f, h * 0.50f)
            }
            drawPath(clipPath, color = ReadingColors.SunYellow, style = Stroke(width = 5f, cap = StrokeCap.Round))

            // الرأس المخروطي الخشبي وسنّ القلم الأزرق
            val cone = Path().apply {
                moveTo(w * 0.34f, h * 0.71f)
                lineTo(w * 0.66f, h * 0.71f)
                lineTo(cx, h * 0.93f)
                close()
            }
            drawPath(cone, brush = Brush.verticalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))))
            val nib = Path().apply {
                moveTo(w * 0.45f, h * 0.85f)
                lineTo(w * 0.55f, h * 0.85f)
                lineTo(cx, h * 0.94f)
                close()
            }
            drawPath(nib, color = Color(0xFF0369A1))

            // عينان مشرقتان وخدود وردية وابتسامة دافئة على جسم القلم
            drawCircle(Color.White, w * 0.068f, Offset(w * 0.43f, h * 0.47f))
            drawCircle(Color.White, w * 0.068f, Offset(w * 0.57f, h * 0.47f))
            drawCircle(ReadingColors.InkDark, w * 0.036f, Offset(w * 0.43f, h * 0.47f))
            drawCircle(ReadingColors.InkDark, w * 0.036f, Offset(w * 0.57f, h * 0.47f))
            drawCircle(Color.White, w * 0.015f, Offset(w * 0.415f, h * 0.455f))
            drawCircle(Color.White, w * 0.015f, Offset(w * 0.555f, h * 0.455f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.62f), w * 0.042f, Offset(w * 0.38f, h * 0.55f))
            drawCircle(Color(0xFFF472B6).copy(alpha = 0.62f), w * 0.042f, Offset(w * 0.62f, h * 0.55f))
            val smile = Path().apply {
                moveTo(w * 0.43f, h * 0.57f)
                quadraticBezierTo(cx, h * 0.65f, w * 0.57f, h * 0.57f)
            }
            drawPath(smile, color = Color.White, style = Stroke(width = 4f, cap = StrokeCap.Round))
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
 * شخصية "النحلة المبتسمة مع القلم" الخاصة بالوحدة الرابعة (صحتي وبيئتي):
 * ترفرف بجناحيها الشفافين بحركة ناعمة عبر rememberInfiniteTransition وتحمل قلماً ملوّناً.
 */
@Composable
fun ReadingBeeMascotCanvas(
    bannerText: String = "أقرأ",
    isCelebrating: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "bee_mascot_anim")
    val wingFlap by infinite.animateFloat(
        initialValue = -16f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isCelebrating) 180 else 360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bee_wing_flap"
    )
    val hoverY by infinite.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bee_hover_y"
    )

    Row(
        modifier = modifier.graphicsLayer { translationY = hoverY },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Canvas(modifier = Modifier.size(76.dp)) {
            val w = size.width
            val h = size.height
            val cx = w * 0.48f
            val cy = h * 0.56f

            // هالة وردية وفيروزية مشرقة خلف النحلة
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFCE7F3).copy(alpha = 0.72f), Color(0xFFCFFAFE).copy(alpha = 0.30f), Color.Transparent),
                    center = Offset(cx, cy),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(cx, cy)
            )

            // الجناحان الشفافان اللامعان بحركة رفرفة حية
            rotate(degrees = wingFlap, pivot = Offset(w * 0.42f, h * 0.42f)) {
                val leftWing = Path().apply {
                    moveTo(w * 0.42f, h * 0.42f)
                    cubicTo(w * 0.14f, h * 0.10f, w * 0.04f, h * 0.36f, w * 0.36f, h * 0.48f)
                    close()
                }
                drawPath(
                    leftWing,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.92f), Color(0xFF67E8F9).copy(alpha = 0.62f))
                    )
                )
                drawPath(leftWing, color = Color(0xFF06B6D4).copy(alpha = 0.55f), style = Stroke(2.5f))
            }
            rotate(degrees = -wingFlap, pivot = Offset(w * 0.56f, h * 0.42f)) {
                val rightWing = Path().apply {
                    moveTo(w * 0.54f, h * 0.42f)
                    cubicTo(w * 0.80f, h * 0.08f, w * 0.92f, h * 0.34f, w * 0.58f, h * 0.48f)
                    close()
                }
                drawPath(
                    rightWing,
                    brush = Brush.radialGradient(
                        colors = listOf(Color.White.copy(alpha = 0.92f), Color(0xFF67E8F9).copy(alpha = 0.62f))
                    )
                )
                drawPath(rightWing, color = Color(0xFF06B6D4).copy(alpha = 0.55f), style = Stroke(2.5f))
            }

            // ذيل النحلة اللطيف الصغير
            val stinger = Path().apply {
                moveTo(w * 0.22f, h * 0.58f)
                lineTo(w * 0.12f, h * 0.62f)
                lineTo(w * 0.24f, h * 0.66f)
                close()
            }
            drawPath(stinger, color = Color(0xFF451A03))

            // جسم النحلة الذهبي الممتلئ المشرق
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFEF08A), ReadingColors.SunYellow, Color(0xFFF59E0B)),
                    center = Offset(w * 0.46f, h * 0.54f),
                    radius = w * 0.34f
                ),
                topLeft = Offset(w * 0.20f, h * 0.38f),
                size = Size(w * 0.54f, h * 0.40f)
            )

            // خطوط النحلة البنية الدافئة المنحنية
            val stripe1 = Path().apply {
                moveTo(w * 0.36f, h * 0.40f)
                quadraticBezierTo(w * 0.31f, h * 0.58f, w * 0.36f, h * 0.76f)
            }
            val stripe2 = Path().apply {
                moveTo(w * 0.49f, h * 0.39f)
                quadraticBezierTo(w * 0.44f, h * 0.58f, w * 0.49f, h * 0.77f)
            }
            drawPath(stripe1, color = Color(0xFF451A03), style = Stroke(width = 7f, cap = StrokeCap.Round))
            drawPath(stripe2, color = Color(0xFF451A03), style = Stroke(width = 7f, cap = StrokeCap.Round))

            // قرنا الاستشعار الملتويان بنقطتين ورديتين
            val ant1 = Path().apply {
                moveTo(w * 0.58f, h * 0.38f)
                quadraticBezierTo(w * 0.56f, h * 0.20f, w * 0.48f, h * 0.19f)
            }
            val ant2 = Path().apply {
                moveTo(w * 0.66f, h * 0.40f)
                quadraticBezierTo(w * 0.72f, h * 0.22f, w * 0.79f, h * 0.23f)
            }
            drawPath(ant1, color = Color(0xFF451A03), style = Stroke(3.5f, cap = StrokeCap.Round))
            drawPath(ant2, color = Color(0xFF451A03), style = Stroke(3.5f, cap = StrokeCap.Round))
            drawCircle(Color(0xFFEC4899), w * 0.042f, Offset(w * 0.48f, h * 0.19f))
            drawCircle(Color(0xFF06B6D4), w * 0.042f, Offset(w * 0.79f, h * 0.23f))

            // وجه النحلة المبتسمة (عينان لامعتان وخدود وردية)
            drawCircle(Color.White, w * 0.062f, Offset(w * 0.59f, h * 0.52f))
            drawCircle(Color.White, w * 0.062f, Offset(w * 0.69f, h * 0.52f))
            drawCircle(ReadingColors.InkDark, w * 0.032f, Offset(w * 0.60f, h * 0.52f))
            drawCircle(ReadingColors.InkDark, w * 0.032f, Offset(w * 0.70f, h * 0.52f))
            drawCircle(Color.White, w * 0.014f, Offset(w * 0.585f, h * 0.505f))
            drawCircle(Color.White, w * 0.014f, Offset(w * 0.685f, h * 0.505f))
            drawCircle(Color(0xFFEC4899).copy(alpha = 0.52f), w * 0.040f, Offset(w * 0.56f, h * 0.60f))
            drawCircle(Color(0xFFEC4899).copy(alpha = 0.52f), w * 0.040f, Offset(w * 0.72f, h * 0.60f))
            val smile = Path().apply {
                moveTo(w * 0.59f, h * 0.63f)
                quadraticBezierTo(w * 0.65f, h * 0.70f, w * 0.71f, h * 0.62f)
            }
            drawPath(smile, color = Color(0xFF451A03), style = Stroke(3.5f, cap = StrokeCap.Round))

            // قلم ملون صغير تحمله النحلة المبتسمة بفرح
            rotate(degrees = -28f, pivot = Offset(w * 0.78f, h * 0.74f)) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFF06B6D4))),
                    topLeft = Offset(w * 0.64f, h * 0.70f),
                    size = Size(w * 0.28f, h * 0.10f),
                    cornerRadius = CornerRadius(5f, 5f)
                )
                val tip = Path().apply {
                    moveTo(w * 0.92f, h * 0.70f)
                    lineTo(w * 0.99f, h * 0.75f)
                    lineTo(w * 0.92f, h * 0.80f)
                    close()
                }
                drawPath(tip, color = Color(0xFFFDE68A))
                drawCircle(Color(0xFF0E7490), 2.5f, Offset(w * 0.98f, h * 0.75f))
            }
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
    MIDDLE_TAB_AND_SOCKET,
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
    sheenProgress: Float = -1f,
    isErrorFlash: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isHighlighted) 1.05f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
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
                    if (interlockSide == PuzzleInterlockSide.RIGHT_SOCKET ||
                        interlockSide == PuzzleInterlockSide.MIDDLE_TAB_AND_SOCKET
                    ) {
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
                    if (interlockSide == PuzzleInterlockSide.LEFT_TAB ||
                        interlockSide == PuzzleInterlockSide.MIDDLE_TAB_AND_SOCKET
                    ) {
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

            val effectiveColor = if (isErrorFlash) Color(0xFFEF4444) else pieceColor
            val effectiveShadow = if (isErrorFlash) Color(0xFFB91C1C) else shadowColor

            // طبقة العمق السفلية البارزة
            drawPath(
                path = buildPiecePath(offsetY = 8f),
                color = effectiveShadow
            )

            // جسم قطعة البازل بتدرج مشرق
            val mainPath = buildPiecePath(offsetY = 0f)
            drawPath(
                path = mainPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        effectiveColor.copy(alpha = 0.94f),
                        effectiveColor,
                        effectiveShadow.copy(alpha = 0.92f)
                    )
                )
            )

            // ومضة ضوئية مائلة تنساب على القطعة لحظة التعشيق المغناطيسي
            if (sheenProgress in 0f..1f) {
                clipPath(mainPath) {
                    val bandW = w * 0.42f
                    val sweepX = -bandW + (w + bandW * 2f) * sheenProgress
                    drawRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFFFBEB).copy(alpha = 0.72f),
                                ReadingColors.SunYellow.copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            start = Offset(sweepX, 0f),
                            end = Offset(sweepX + bandW, h)
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(w, h)
                    )
                }
            }

            // إطار أبيض لطيف ولمعة علوية
            drawPath(
                path = mainPath,
                color = when {
                    isErrorFlash -> Color(0xFFFEE2E2)
                    isHighlighted -> ReadingColors.SunYellow
                    else -> Color.White.copy(alpha = 0.72f)
                },
                style = Stroke(width = if (isHighlighted || isErrorFlash) 5.5f else 3f, join = StrokeJoin.Round)
            )
            drawLine(
                color = Color.White.copy(alpha = 0.42f),
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
 * تفاحة التاء المربوطة التفاعلية (نشاط حركات ة ، ـة في الوحدة الثانية):
 * تفاحة حمراء لامعة مرسومة بمنحنيات ناعمة مع ورقة خضراء وساق بنية وتمايل لطيف.
 */
@Composable
fun AppleVowelCanvas(
    primaryForm: String,
    secondaryForm: String,
    vowelLabel: String,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "apple_vowel_anim")
    val swayDeg by infinite.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isActive) 650 else 2100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "apple_sway"
    )
    val scale by animateFloatAsState(
        targetValue = if (isActive) 1.07f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "apple_scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            rotationZ = swayDeg
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val cx = w * 0.50f

            // هالة مضيئة حول التفاحة عند التفعيل
            if (isActive) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(ReadingColors.SunYellow.copy(alpha = 0.55f), Color.Transparent),
                        center = Offset(cx, h * 0.56f),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, h * 0.56f)
                )
            }

            // الساق البنية المقوّسة
            val stem = Path().apply {
                moveTo(cx, h * 0.28f)
                quadraticBezierTo(cx - w * 0.04f, h * 0.14f, cx + w * 0.05f, h * 0.08f)
            }
            drawPath(stem, color = Color(0xFF78350F), style = Stroke(width = 7f, cap = StrokeCap.Round))

            // ورقة التفاحة الخضراء المرسومة بمنحنيات ناعمة
            val leaf = Path().apply {
                moveTo(cx + w * 0.02f, h * 0.22f)
                cubicTo(cx + w * 0.24f, h * 0.05f, cx + w * 0.36f, h * 0.24f, cx + w * 0.04f, h * 0.27f)
                close()
            }
            drawPath(
                leaf,
                brush = Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A)))
            )

            // جسم التفاحة الحمراء اللامعة بمنحنيات ناعمة
            val applePath = Path().apply {
                moveTo(cx, h * 0.30f)
                cubicTo(w * 0.72f, h * 0.16f, w * 0.94f, h * 0.42f, w * 0.84f, h * 0.70f)
                cubicTo(w * 0.76f, h * 0.92f, w * 0.58f, h * 0.94f, cx, h * 0.87f)
                cubicTo(w * 0.42f, h * 0.94f, w * 0.24f, h * 0.92f, w * 0.16f, h * 0.70f)
                cubicTo(w * 0.06f, h * 0.42f, w * 0.28f, h * 0.16f, cx, h * 0.30f)
                close()
            }
            drawPath(
                applePath,
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFB91C1C)),
                    center = Offset(w * 0.36f, h * 0.44f),
                    radius = w * 0.56f
                )
            )
            drawPath(
                applePath,
                color = if (isActive) ReadingColors.SunYellow else Color.White.copy(alpha = 0.75f),
                style = Stroke(width = if (isActive) 5f else 3f, join = StrokeJoin.Round)
            )

            // لمعة زجاجية ناعمة أعلى يسار التفاحة
            val shine = Path().apply {
                moveTo(w * 0.26f, h * 0.46f)
                quadraticBezierTo(w * 0.24f, h * 0.35f, w * 0.36f, h * 0.32f)
            }
            drawPath(shine, color = Color.White.copy(alpha = 0.65f), style = Stroke(width = 6f, cap = StrokeCap.Round))
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(top = 24.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = primaryForm,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    lineHeight = 36.sp,
                    color = Color.White
                )
                Text(
                    text = secondaryForm,
                    fontFamily = ReadingTajawalFont,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 27.sp,
                    lineHeight = 36.sp,
                    color = ReadingColors.SunYellow
                )
            }
            Text(
                text = vowelLabel,
                fontFamily = ReadingTajawalFont,
                fontWeight = FontWeight.Bold,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.94f)
            )
        }
    }
}

/**
 * نوتات موسيقية ملونة ونجوم متلألئة عائمة لنشيد الوحدة ("نشيد مَدْرَسَتِي").
 */
@Composable
fun ChantMusicalCanvas(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infinite = rememberInfiniteTransition(label = "chant_music_anim")
    val floatPhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 1400 else 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "music_phase"
    )
    val twinkle by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isPlaying) 600 else 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "music_twinkle"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        fun drawMusicNote(nx: Float, ny: Float, r: Float, color: Color, beamed: Boolean) {
            // رأس النوتة المائل
            drawOval(color = color, topLeft = Offset(nx - r, ny - r * 0.72f), size = Size(r * 2f, r * 1.45f))
            // ساق النوتة
            drawLine(color, Offset(nx + r * 0.85f, ny), Offset(nx + r * 0.85f, ny - r * 3.4f), r * 0.42f, StrokeCap.Round)
            if (beamed) {
                val nx2 = nx + r * 2.5f
                val ny2 = ny - r * 0.45f
                drawOval(color = color, topLeft = Offset(nx2 - r, ny2 - r * 0.72f), size = Size(r * 2f, r * 1.45f))
                drawLine(color, Offset(nx2 + r * 0.85f, ny2), Offset(nx2 + r * 0.85f, ny2 - r * 3.4f), r * 0.42f, StrokeCap.Round)
                drawLine(
                    color,
                    Offset(nx + r * 0.85f, ny - r * 3.2f),
                    Offset(nx2 + r * 0.85f, ny2 - r * 3.2f),
                    r * 0.62f,
                    StrokeCap.Round
                )
            } else {
                val flag = Path().apply {
                    moveTo(nx + r * 0.85f, ny - r * 3.3f)
                    cubicTo(nx + r * 2.4f, ny - r * 2.8f, nx + r * 2.3f, ny - r * 1.4f, nx + r * 1.5f, ny - r * 1.0f)
                }
                drawPath(flag, color = color, style = Stroke(width = r * 0.48f, cap = StrokeCap.Round))
            }
        }

        fun drawTwinkleStar(sx: Float, sy: Float, r: Float, color: Color) {
            val star = Path().apply {
                for (i in 0 until 8) {
                    val rad = if (i % 2 == 0) r else r * 0.40f
                    val a = i * (PI / 4.0) - PI / 2.0
                    val x = sx + (rad * cos(a)).toFloat()
                    val y = sy + (rad * sin(a)).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
            drawPath(star, color = color)
        }

        val amp = if (isPlaying) h * 0.14f else h * 0.06f
        val noteR = (minOf(w, h) * 0.085f).coerceIn(5.5f, 11f)

        drawMusicNote(
            nx = w * 0.16f,
            ny = h * 0.68f + sin(floatPhase) * amp,
            r = noteR,
            color = Color(0xFFEC4899),
            beamed = true
        )
        drawMusicNote(
            nx = w * 0.50f,
            ny = h * 0.60f + sin(floatPhase + 1.8f) * amp,
            r = noteR * 1.08f,
            color = Color(0xFF0EA5E9),
            beamed = false
        )
        drawMusicNote(
            nx = w * 0.78f,
            ny = h * 0.70f + sin(floatPhase + 3.4f) * amp,
            r = noteR * 0.96f,
            color = Color(0xFF22C55E),
            beamed = true
        )

        drawTwinkleStar(w * 0.34f, h * 0.28f - sin(floatPhase) * amp * 0.4f, noteR * 1.1f * twinkle, ReadingColors.SunYellow)
        drawTwinkleStar(w * 0.67f, h * 0.24f + sin(floatPhase + 1.2f) * amp * 0.4f, noteR * 0.95f * twinkle, ReadingColors.CoralOrange)
        drawTwinkleStar(w * 0.90f, h * 0.36f, noteR * 0.75f * twinkle, ReadingColors.SunYellow)
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

            QiraatiStoryArtKey.MADRASATI_SCHOOL_FLAG -> {
                // مبنى المدرسة العراقية الجميل مع سارية وعلم العراق يرفرف وتلاميذ مبتسمون
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF7DD3FC), Color(0xFFE0F2FE), Color(0xFFDCFCE7))
                    )
                )
                drawCircle(ReadingColors.SunYellow, w * 0.075f, Offset(w * 0.14f, h * 0.19f))
                // غيمة لطيفة متحركة
                val gx = w * 0.74f + sway * 9f
                drawCircle(Color.White.copy(alpha = 0.92f), w * 0.048f, Offset(gx, h * 0.16f))
                drawCircle(Color.White.copy(alpha = 0.92f), w * 0.062f, Offset(gx + w * 0.055f, h * 0.14f))
                drawCircle(Color.White.copy(alpha = 0.92f), w * 0.045f, Offset(gx + w * 0.11f, h * 0.17f))

                // ساحة المدرسة الخضراء
                val yard = Path().apply {
                    moveTo(0f, h * 0.74f)
                    cubicTo(w * 0.35f, h * 0.66f, w * 0.70f, h * 0.76f, w, h * 0.69f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(yard, brush = Brush.verticalGradient(listOf(Color(0xFF84CC16), Color(0xFF22C55E))))

                // مبنى المدرسة المشرق
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
                    topLeft = Offset(w * 0.18f, h * 0.34f),
                    size = Size(w * 0.52f, h * 0.42f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF15803D))),
                    topLeft = Offset(w * 0.15f, h * 0.28f),
                    size = Size(w * 0.58f, h * 0.10f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                // نوافذ وباب المدرسة
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.24f, h * 0.46f), Size(w * 0.10f, h * 0.12f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.54f, h * 0.46f), Size(w * 0.10f, h * 0.12f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.39f, h * 0.50f), Size(w * 0.10f, h * 0.26f), CornerRadius(10f, 10f))

                // سارية وعلم العراق يرفرف بوضوح
                drawIraqiFlag(poleX = w * 0.79f, topY = h * 0.16f, bottomY = h * 0.76f, sway = sway)

                // تلميذ وتلميذة يبتسمان أمام المدرسة
                drawCartoonPerson(w * 0.28f, h * 0.68f, scale = 0.78f, shirtColor = Color(0xFF0284C7))
                drawCartoonPerson(w * 0.60f, h * 0.68f, scale = 0.76f, shirtColor = Color(0xFFEC4899))
            }

            QiraatiStoryArtKey.MADRASATI_GATE_ARRIVAL -> {
                // بوابة المدرسة الصباحية والأطفال يصلون بنشاط مع حقائبهم المدرسية
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFBAE6FD), Color(0xFFECFCCB))))
                drawCircle(ReadingColors.SunYellow, w * 0.08f, Offset(w * 0.84f, h * 0.20f))
                // ممر المدرسة
                drawRoundRect(Color(0xFFFDE68A), Offset(0f, h * 0.70f), Size(w, h * 0.30f), CornerRadius(0f, 0f))
                // قوس بوابة المدرسة
                val gate = Path().apply {
                    moveTo(w * 0.22f, h * 0.74f)
                    lineTo(w * 0.22f, h * 0.34f)
                    cubicTo(w * 0.22f, h * 0.12f, w * 0.78f, h * 0.12f, w * 0.78f, h * 0.34f)
                    lineTo(w * 0.78f, h * 0.74f)
                }
                drawPath(gate, color = Color(0xFF0284C7), style = Stroke(width = 14f, cap = StrokeCap.Round))
                // معلمة ترحب بالتلاميذ مع حقائبهم
                drawCartoonPerson(w * 0.34f, h * 0.56f, scale = 0.96f, shirtColor = Color(0xFF16A34A))
                drawCartoonPerson(w * 0.54f, h * 0.64f, scale = 0.76f, shirtColor = Color(0xFFFF7A59))
                drawCartoonPerson(w * 0.68f, h * 0.65f, scale = 0.74f, shirtColor = Color(0xFF8B5CF6))
                // حقيبة ملونة على ظهر التلميذ
                drawRoundRect(Color(0xFFEF4444), Offset(w * 0.73f, h * 0.66f), Size(w * 0.07f, h * 0.12f), CornerRadius(6f, 6f))
            }

            QiraatiStoryArtKey.MADRASATI_YARD_LINEUP -> {
                // ساحة المدرسة والاصطفاف الصباحي وتحيّة العلم العراقي
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFFFEF9C3))))
                drawRect(Color(0xFF86EFAC), Offset(0f, h * 0.66f), Size(w, h * 0.34f))
                // سارية وعلم العراق في وسط الساحة
                drawIraqiFlag(poleX = w * 0.50f, topY = h * 0.12f, bottomY = h * 0.68f, sway = sway)
                // صفّ منتظم من التلاميذ يحيّون العلم
                drawCartoonPerson(w * 0.22f, h * 0.64f, scale = 0.76f, shirtColor = Color(0xFF0284C7))
                drawCartoonPerson(w * 0.36f, h * 0.66f, scale = 0.74f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.64f, h * 0.66f, scale = 0.74f, shirtColor = Color(0xFF22C55E))
                drawCartoonPerson(w * 0.78f, h * 0.64f, scale = 0.76f, shirtColor = Color(0xFFF59E0B))
            }

            QiraatiStoryArtKey.MADRASATI_CLASSROOM_LESSON -> {
                // غرفة الصف المشرقة: سبورة خضراء، معلمة، ورحلات التلاميذ وكتب مفتوحة
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFEFCE8), Color(0xFFECFCCB))))
                // السبورة الصفية الخضراء
                drawRoundRect(Color(0xFF15803D), Offset(w * 0.20f, h * 0.14f), Size(w * 0.60f, h * 0.38f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.20f, h * 0.14f), Size(w * 0.60f, h * 0.38f), CornerRadius(14f, 14f), style = Stroke(6f))
                // خطوط طبشورية على السبورة
                drawLine(Color.White, Offset(w * 0.30f, h * 0.28f), Offset(w * 0.68f, h * 0.28f), 4.5f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(w * 0.36f, h * 0.39f), Offset(w * 0.62f, h * 0.39f), 4.5f, StrokeCap.Round)
                // المعلمة والتلاميذ أمام الرحلات
                drawCartoonPerson(w * 0.16f, h * 0.46f, scale = 0.90f, shirtColor = Color(0xFF0EA5E9))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.32f, h * 0.72f), Size(w * 0.22f, h * 0.18f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.60f, h * 0.72f), Size(w * 0.22f, h * 0.18f), CornerRadius(10f, 10f))
                drawCartoonPerson(w * 0.43f, h * 0.60f, scale = 0.72f, shirtColor = Color(0xFFFF7A59))
                drawCartoonPerson(w * 0.71f, h * 0.60f, scale = 0.72f, shirtColor = Color(0xFF8B5CF6))
                // كتب مفتوحة على الرحلات
                drawRoundRect(Color.White, Offset(w * 0.37f, h * 0.68f), Size(w * 0.12f, h * 0.06f), CornerRadius(4f, 4f))
                drawRoundRect(Color.White, Offset(w * 0.65f, h * 0.68f), Size(w * 0.12f, h * 0.06f), CornerRadius(4f, 4f))
            }

            QiraatiStoryArtKey.WATANI_CLASSROOM_MAP_FLAG -> {
                // المعلم والتلاميذ في الصف يشيرون إلى خريطة العراق وعلم العراق المرسوم على السبورة
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFFFFBEB))))
                // السبورة الصفية الكبيرة
                drawRoundRect(Color(0xFF15803D), Offset(w * 0.18f, h * 0.10f), Size(w * 0.64f, h * 0.48f), CornerRadius(16f, 16f))
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.18f, h * 0.10f), Size(w * 0.64f, h * 0.48f), CornerRadius(16f, 16f), style = Stroke(6f))
                // خريطة العراق المضيئة وسط السبورة
                drawIraqMapSilhouette(center = Offset(w * 0.40f, h * 0.34f), scale = size.minDimension * 0.24f, fillColor = ReadingColors.SunYellow)
                // علم العراق المرسوم على السبورة
                drawIraqiFlag(poleX = w * 0.56f, topY = h * 0.18f, bottomY = h * 0.50f, sway = sway * 0.7f)
                // المعلم والتلاميذ يشيرون بفخر
                drawCartoonPerson(w * 0.15f, h * 0.52f, scale = 0.94f, shirtColor = Color(0xFF0284C7))
                drawCartoonPerson(w * 0.42f, h * 0.68f, scale = 0.76f, shirtColor = Color(0xFFFF7A59))
                drawCartoonPerson(w * 0.60f, h * 0.68f, scale = 0.76f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.78f, h * 0.66f, scale = 0.78f, shirtColor = Color(0xFF22C55E))
            }

            QiraatiStoryArtKey.WATANI_NATIONAL_DAY_PARTY -> {
                // تلاميذ يحتفلون بالعيد الوطني داخل الصف بالبالونات والأعلام والصور
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFFEF3C7))))
                // شريط زينة ملون مقوّس أعلى الصف
                val garland = Path().apply {
                    moveTo(0f, h * 0.16f)
                    quadraticBezierTo(w * 0.50f, h * 0.34f, w, h * 0.16f)
                }
                drawPath(garland, color = Color(0xFF0284C7), style = Stroke(4f, cap = StrokeCap.Round))
                // بالونات بألوان العلم العراقي ومبهجة تتأرجح
                val bShift = sway * 4f
                drawOval(Color(0xFFDC2626), Offset(w * 0.12f, h * 0.16f + bShift), Size(w * 0.11f, h * 0.20f))
                drawOval(Color.White, Offset(w * 0.24f, h * 0.12f - bShift), Size(w * 0.11f, h * 0.20f))
                drawOval(Color(0xFF16A34A), Offset(w * 0.74f, h * 0.14f + bShift), Size(w * 0.11f, h * 0.20f))
                drawOval(ReadingColors.SunYellow, Offset(w * 0.85f, h * 0.20f - bShift), Size(w * 0.10f, h * 0.18f))
                // علم العراق وسط لوحة الاحتفال
                drawIraqiFlag(poleX = w * 0.44f, topY = h * 0.22f, bottomY = h * 0.60f, sway = sway)
                // تلاميذ مبتسمون يحتفلون
                drawCartoonPerson(w * 0.26f, h * 0.64f, scale = 0.84f, shirtColor = Color(0xFF0EA5E9))
                drawCartoonPerson(w * 0.50f, h * 0.66f, scale = 0.84f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.74f, h * 0.64f, scale = 0.84f, shirtColor = Color(0xFF22C55E))
            }

            QiraatiStoryArtKey.WATANI_THREE_IRAQI_PALMS -> {
                // ثلاث نخلات عراقية باسقة تحت الشمس الذهبية بجانب النهر
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFFFEF9C3), Color(0xFFBBF7D0))))
                drawCircle(ReadingColors.SunYellow, w * 0.085f, Offset(w * 0.15f, h * 0.20f))
                // ضفة النهر الزرقاء والخضراء
                val river = Path().apply {
                    moveTo(0f, h * 0.76f)
                    cubicTo(w * 0.32f, h * 0.70f, w * 0.68f, h * 0.82f, w, h * 0.74f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(river, brush = Brush.horizontalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
                // النخلات العراقية الثلاث الباسقة
                drawIraqiPalmTree(baseX = w * 0.26f, baseY = h * 0.80f, height = h * 0.52f, sway = sway)
                drawIraqiPalmTree(baseX = w * 0.52f, baseY = h * 0.78f, height = h * 0.60f, sway = -sway)
                drawIraqiPalmTree(baseX = w * 0.78f, baseY = h * 0.80f, height = h * 0.50f, sway = sway * 0.8f)
            }

            QiraatiStoryArtKey.WATANI_IRAQ_MAP_AND_FLAG -> {
                // خريطة العراق يلتف حولها العلم العراقي مع هالة ذهبية مشرقة
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFFE0F2FE))))
                // شريط علم العراق المتموّج يلتف خلف الخريطة
                val wave = sway * 6f
                fun ribbon(yBase: Float, col: Color) {
                    val p = Path().apply {
                        moveTo(0f, yBase)
                        cubicTo(w * 0.30f, yBase - 18f + wave, w * 0.70f, yBase + 18f - wave, w, yBase)
                        lineTo(w, yBase + h * 0.14f)
                        cubicTo(w * 0.70f, yBase + h * 0.14f + 18f - wave, w * 0.30f, yBase + h * 0.14f - 18f + wave, 0f, yBase + h * 0.14f)
                        close()
                    }
                    drawPath(p, color = col)
                }
                ribbon(h * 0.28f, Color(0xFFDC2626))
                ribbon(h * 0.42f, Color.White)
                ribbon(h * 0.56f, Color(0xFF0F172A))
                // خريطة العراق الذهبية المضيئة في المنتصف
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFFF7B2).copy(alpha = 0.72f), Color.Transparent)),
                    radius = w * 0.26f,
                    center = Offset(w * 0.50f, h * 0.48f)
                )
                drawIraqMapSilhouette(center = Offset(w * 0.50f, h * 0.49f), scale = size.minDimension * 0.34f, fillColor = Color(0xFF16A34A))
                drawIraqiPalmTree(baseX = w * 0.18f, baseY = h * 0.90f, height = h * 0.38f, sway = sway)
                drawIraqiPalmTree(baseX = w * 0.84f, baseY = h * 0.90f, height = h * 0.38f, sway = -sway)
            }

            QiraatiStoryArtKey.SIHATI_HYGIENE_TOOLS -> {
                // حوار الأم والطفل مع أدوات النظافة الشخصية (مقص، مشط، مقراض أظافر، منشفة، فرشاة ومعجون أسنان)
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFCFFAFE), Color(0xFFFDF2F8))))
                // طاولة بيضاء ناعمة لأدوات النظافة
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.08f, h * 0.58f),
                    size = Size(w * 0.84f, h * 0.34f),
                    cornerRadius = CornerRadius(22f, 22f)
                )
                // الأم والطفل مبتسمان خلف الطاولة
                drawCartoonPerson(w * 0.30f, h * 0.34f, scale = 1.05f, shirtColor = Color(0xFFEC4899))
                drawCartoonPerson(w * 0.70f, h * 0.40f, scale = 0.86f, shirtColor = Color(0xFF06B6D4))
                // منشفة وردية ناعمة مطوية
                drawRoundRect(Color(0xFFF472B6), Offset(w * 0.13f, h * 0.66f), Size(w * 0.18f, h * 0.16f), CornerRadius(10f, 10f))
                drawLine(Color.White.copy(alpha = 0.75f), Offset(w * 0.15f, h * 0.72f), Offset(w * 0.29f, h * 0.72f), 4f, StrokeCap.Round)
                // فرشاة ومعجون أسنان فيروزي
                drawLine(Color(0xFF06B6D4), Offset(w * 0.36f, h * 0.78f), Offset(w * 0.52f, h * 0.68f), 7f, StrokeCap.Round)
                drawRoundRect(Color.White, Offset(w * 0.47f, h * 0.63f), Size(w * 0.08f, h * 0.06f), CornerRadius(4f, 4f))
                drawRoundRect(Color(0xFF22C55E), Offset(w * 0.38f, h * 0.82f), Size(w * 0.16f, h * 0.06f), CornerRadius(6f, 6f))
                // مشط أصفر لطيف
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.58f, h * 0.67f), Size(w * 0.14f, h * 0.06f), CornerRadius(5f, 5f))
                for (t in 0..4) {
                    val tx = w * (0.60f + t * 0.026f)
                    drawLine(Color(0xFFD97706), Offset(tx, h * 0.73f), Offset(tx, h * 0.81f), 3.2f, StrokeCap.Round)
                }
                // مقص ومقراض أظافر صغيرين
                drawCircle(Color(0xFF8B5CF6), 7f, Offset(w * 0.78f, h * 0.78f), style = Stroke(3.5f))
                drawCircle(Color(0xFF8B5CF6), 7f, Offset(w * 0.84f, h * 0.78f), style = Stroke(3.5f))
                drawLine(Color(0xFF64748B), Offset(w * 0.79f, h * 0.74f), Offset(w * 0.85f, h * 0.64f), 3.5f, StrokeCap.Round)
                drawLine(Color(0xFF64748B), Offset(w * 0.83f, h * 0.74f), Offset(w * 0.77f, h * 0.64f), 3.5f, StrokeCap.Round)
            }

            QiraatiStoryArtKey.SIHATI_HAND_WASHING -> {
                // طفل يغسل يديه بالماء والصابون مع فقاعات لامعة
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFE0F2FE), Color(0xFFCFFAFE))))
                // مرآة دائرية مشرقة
                drawCircle(Color.White, w * 0.16f, Offset(w * 0.50f, h * 0.28f))
                drawCircle(Color(0xFF67E8F9), w * 0.16f, Offset(w * 0.50f, h * 0.28f), style = Stroke(5f))
                drawCartoonPerson(w * 0.34f, h * 0.44f, scale = 0.98f, shirtColor = Color(0xFF06B6D4))
                // مغسلة بيضاء وحنفية وتيار ماء لامع
                drawRoundRect(Color.White, Offset(w * 0.46f, h * 0.62f), Size(w * 0.42f, h * 0.22f), CornerRadius(24f, 24f))
                drawLine(Color(0xFF94A3B8), Offset(w * 0.72f, h * 0.46f), Offset(w * 0.64f, h * 0.46f), 8f, StrokeCap.Round)
                val waterStream = Path().apply {
                    moveTo(w * 0.64f, h * 0.48f)
                    quadraticBezierTo(w * 0.62f + sway * 2f, h * 0.56f, w * 0.64f, h * 0.64f)
                }
                drawPath(waterStream, color = Color(0xFF0EA5E9), style = Stroke(8f, cap = StrokeCap.Round))
                // قطعة صابون وردية وفقاعات صابون متطايرة
                drawOval(Color(0xFFEC4899), Offset(w * 0.76f, h * 0.55f), Size(w * 0.11f, h * 0.07f))
                val bubbleOff = sway * 3.5f
                listOf(
                    Offset(w * 0.56f, h * 0.48f - bubbleOff) to 10f,
                    Offset(w * 0.60f, h * 0.36f + bubbleOff) to 7f,
                    Offset(w * 0.72f, h * 0.32f - bubbleOff) to 9f,
                    Offset(w * 0.52f, h * 0.56f + bubbleOff) to 6f
                ).forEach { (pt, r) ->
                    drawCircle(Color.White.copy(alpha = 0.85f), r, pt)
                    drawCircle(Color(0xFF38BDF8), r, pt, style = Stroke(2.2f))
                }
            }

            QiraatiStoryArtKey.SIHATI_HEALTHY_BREAKFAST -> {
                // أسرة تتناول وجبة إفطار صحية معاً
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFDF2F8))))
                drawCircle(ReadingColors.SunYellow.copy(alpha = 0.35f), w * 0.16f, Offset(w * 0.86f, h * 0.18f))
                drawCartoonPerson(w * 0.24f, h * 0.38f, scale = 0.96f, shirtColor = Color(0xFF0284C7))
                drawCartoonPerson(w * 0.50f, h * 0.42f, scale = 0.82f, shirtColor = Color(0xFF10B981))
                drawCartoonPerson(w * 0.76f, h * 0.38f, scale = 0.96f, shirtColor = Color(0xFFEC4899))
                // مائدة الإفطار الصحية
                drawRoundRect(Color(0xFFF59E0B), Offset(w * 0.08f, h * 0.62f), Size(w * 0.84f, h * 0.26f), CornerRadius(20f, 20f))
                // كوب حليب، طبق زيتون وجبن، وتفاحة حمراء وبرتقالة
                drawRoundRect(Color.White, Offset(w * 0.22f, h * 0.52f), Size(w * 0.09f, h * 0.14f), CornerRadius(6f, 6f))
                drawOval(Color.White, Offset(w * 0.40f, h * 0.58f), Size(w * 0.20f, h * 0.11f))
                drawCircle(Color(0xFF16A34A), 5.5f, Offset(w * 0.46f, h * 0.63f))
                drawCircle(Color(0xFF15803D), 5.5f, Offset(w * 0.51f, h * 0.62f))
                drawCircle(ReadingColors.SunYellow, 6f, Offset(w * 0.55f, h * 0.63f))
                drawCircle(Color(0xFFEF4444), 9f, Offset(w * 0.70f, h * 0.60f))
                drawCircle(ReadingColors.CoralOrange, 9f, Offset(w * 0.77f, h * 0.61f))
            }

            QiraatiStoryArtKey.SIHATI_PARK_PLAY_KITE_BIKE -> {
                // أفراد الأسرة يمرحون في الحديقة بالدراجة والطائرة الورقية
                drawRect(brush = Brush.verticalGradient(listOf(Color(0xFF7DD3FC), Color(0xFFE0F2FE))))
                drawCircle(ReadingColors.SunYellow, w * 0.09f, Offset(w * 0.86f, h * 0.20f))
                // تلة حديقة خضراء ناعمة
                val parkHill = Path().apply {
                    moveTo(0f, h * 0.64f)
                    quadraticBezierTo(w * 0.50f, h * 0.50f, w, h * 0.64f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(parkHill, brush = Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))))
                // شجرة خضراء ظليلة على اليسار
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.10f, h * 0.48f), Size(w * 0.05f, h * 0.22f), CornerRadius(4f, 4f))
                drawCircle(Color(0xFF15803D), w * 0.10f, Offset(w * 0.125f, h * 0.42f))
                // طائرة ورقية ملونة تحلّق بخيط متموّج
                val kiteCenter = Offset(w * 0.64f + sway * 5f, h * 0.24f - sway * 2f)
                val kite = Path().apply {
                    moveTo(kiteCenter.x, kiteCenter.y - 22f)
                    lineTo(kiteCenter.x + 18f, kiteCenter.y)
                    lineTo(kiteCenter.x, kiteCenter.y + 24f)
                    lineTo(kiteCenter.x - 18f, kiteCenter.y)
                    close()
                }
                drawPath(kite, brush = Brush.linearGradient(listOf(Color(0xFFEC4899), ReadingColors.SunYellow)))
                val kiteString = Path().apply {
                    moveTo(kiteCenter.x, kiteCenter.y + 24f)
                    quadraticBezierTo(w * 0.56f, h * 0.50f, w * 0.50f, h * 0.68f)
                }
                drawPath(kiteString, color = Color.White, style = Stroke(3f, cap = StrokeCap.Round))
                // دراجة هوائية ملونة وطفل سعيد
                drawCircle(ReadingColors.InkDark, 14f, Offset(w * 0.28f, h * 0.78f), style = Stroke(5f))
                drawCircle(ReadingColors.InkDark, 14f, Offset(w * 0.44f, h * 0.78f), style = Stroke(5f))
                drawLine(Color(0xFFEC4899), Offset(w * 0.28f, h * 0.78f), Offset(w * 0.36f, h * 0.64f), 5f, StrokeCap.Round)
                drawLine(Color(0xFFEC4899), Offset(w * 0.36f, h * 0.64f), Offset(w * 0.44f, h * 0.78f), 5f, StrokeCap.Round)
                drawCartoonPerson(w * 0.52f, h * 0.60f, scale = 0.78f, shirtColor = Color(0xFF06B6D4))
                drawCartoonPerson(w * 0.78f, h * 0.58f, scale = 0.86f, shirtColor = Color(0xFFEC4899))
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

private fun DrawScope.drawIraqiPalmTree(baseX: Float, baseY: Float, height: Float, sway: Float) {
    val topX = baseX + sway * 5f
    val topY = baseY - height
    // جذع النخلة المقوّس
    val trunk = Path().apply {
        moveTo(baseX - 7f, baseY)
        quadraticBezierTo(baseX - 2f + sway * 2f, baseY - height * 0.55f, topX - 4f, topY)
        lineTo(topX + 4f, topY)
        quadraticBezierTo(baseX + 4f + sway * 2f, baseY - height * 0.55f, baseX + 7f, baseY)
        close()
    }
    drawPath(trunk, brush = Brush.verticalGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))))
    // عذوق التمر الذهبية
    drawCircle(ReadingColors.SunYellow, 5.5f, Offset(topX - 6f, topY + 8f))
    drawCircle(Color(0xFFF59E0B), 5.5f, Offset(topX + 6f, topY + 8f))
    // سعف النخيل الأخضر المنحني
    val angles = listOf(-155.0, -120.0, -75.0, -35.0, 5.0, 200.0)
    val frondLen = height * 0.44f
    angles.forEach { deg ->
        val rad = deg * PI / 180.0
        val endX = topX + (frondLen * cos(rad)).toFloat() + sway * 3f
        val endY = topY + (frondLen * sin(rad)).toFloat() + frondLen * 0.22f
        val ctrlX = topX + (frondLen * 0.55f * cos(rad)).toFloat()
        val ctrlY = topY + (frondLen * 0.55f * sin(rad)).toFloat() - 10f
        val frond = Path().apply {
            moveTo(topX, topY)
            quadraticBezierTo(ctrlX, ctrlY, endX, endY)
        }
        drawPath(frond, color = Color(0xFF16A34A), style = Stroke(width = 7.5f, cap = StrokeCap.Round))
    }
}

private fun DrawScope.drawIraqMapSilhouette(center: Offset, scale: Float, fillColor: Color) {
    // شكل مبسّط أنيق ومحبّب لخريطة العراق بمنحنيات ناعمة
    val mapPath = Path().apply {
        moveTo(center.x - scale * 0.35f, center.y - scale * 0.78f)
        cubicTo(center.x + scale * 0.05f, center.y - scale * 0.92f, center.x + scale * 0.42f, center.y - scale * 0.65f, center.x + scale * 0.40f, center.y - scale * 0.25f)
        cubicTo(center.x + scale * 0.62f, center.y + scale * 0.08f, center.x + scale * 0.74f, center.y + scale * 0.52f, center.x + scale * 0.52f, center.y + scale * 0.80f)
        cubicTo(center.x + scale * 0.16f, center.y + scale * 0.88f, center.x - scale * 0.28f, center.y + scale * 0.58f, center.x - scale * 0.64f, center.y + scale * 0.18f)
        cubicTo(center.x - scale * 0.80f, center.y - scale * 0.10f, center.x - scale * 0.60f, center.y - scale * 0.55f, center.x - scale * 0.35f, center.y - scale * 0.78f)
        close()
    }
    drawPath(mapPath, color = fillColor)
    drawPath(mapPath, color = Color.White, style = Stroke(width = 3.5f, join = StrokeJoin.Round))
    // نهر دجلة والفرات كخطين سماويين لطيفين داخل الخريطة
    val tigris = Path().apply {
        moveTo(center.x - scale * 0.08f, center.y - scale * 0.65f)
        quadraticBezierTo(center.x + scale * 0.14f, center.y, center.x + scale * 0.42f, center.y + scale * 0.66f)
    }
    val euphrates = Path().apply {
        moveTo(center.x - scale * 0.40f, center.y - scale * 0.24f)
        quadraticBezierTo(center.x - scale * 0.06f, center.y + scale * 0.25f, center.x + scale * 0.40f, center.y + scale * 0.66f)
    }
    drawPath(tigris, color = Color(0xFF7DD3FC), style = Stroke(width = 3f, cap = StrokeCap.Round))
    drawPath(euphrates, color = Color(0xFF7DD3FC), style = Stroke(width = 3f, cap = StrokeCap.Round))
}

private fun DrawScope.drawIraqiFlag(poleX: Float, topY: Float, bottomY: Float, sway: Float) {
    // السارية الذهبية
    drawLine(Color(0xFF64748B), Offset(poleX, topY), Offset(poleX, bottomY), 5.5f, StrokeCap.Round)
    drawCircle(ReadingColors.SunYellow, 6.5f, Offset(poleX, topY))

    val fw = size.width * 0.22f
    val fh = size.height * 0.26f
    val stripeH = fh / 3f
    val wave = sway * 5.5f

    fun stripePath(y0: Float): Path = Path().apply {
        moveTo(poleX, y0)
        cubicTo(poleX + fw * 0.35f, y0 - wave, poleX + fw * 0.68f, y0 + wave, poleX + fw, y0 - wave * 0.5f)
        lineTo(poleX + fw, y0 + stripeH - wave * 0.5f)
        cubicTo(poleX + fw * 0.68f, y0 + stripeH + wave, poleX + fw * 0.35f, y0 + stripeH - wave, poleX, y0 + stripeH)
        close()
    }

    // الأشرطة الثلاثة: أحمر، أبيض، أسود + شارة خضراء وسط الأبيض
    drawPath(stripePath(topY + 4f), color = Color(0xFFDC2626))
    drawPath(stripePath(topY + 4f + stripeH), color = Color.White)
    drawPath(stripePath(topY + 4f + stripeH * 2f), color = Color(0xFF0F172A))
    drawLine(
        color = Color(0xFF15803D),
        start = Offset(poleX + fw * 0.26f, topY + 4f + stripeH * 1.5f - wave * 0.2f),
        end = Offset(poleX + fw * 0.74f, topY + 4f + stripeH * 1.5f + wave * 0.2f),
        strokeWidth = 4.5f,
        cap = StrokeCap.Round
    )
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

            // ================================================================
            // رسوم كلمات الوحدة الثانية (مدرستي): ر، س، ن، ت، ة، ـٌ
            // ================================================================
            QiraatiWordArtKey.FOOT -> {
                val foot = Path().apply {
                    moveTo(w * 0.30f, h * 0.22f)
                    lineTo(w * 0.50f, h * 0.22f)
                    lineTo(w * 0.52f, h * 0.58f)
                    cubicTo(w * 0.78f, h * 0.58f, w * 0.86f, h * 0.78f, w * 0.76f, h * 0.82f)
                    lineTo(w * 0.26f, h * 0.82f)
                    quadraticBezierTo(w * 0.22f, h * 0.64f, w * 0.30f, h * 0.58f)
                    close()
                }
                drawPath(foot, color = Color(0xFF0EA5E9))
                drawRoundRect(Color.White, Offset(w * 0.24f, h * 0.76f), Size(w * 0.56f, h * 0.09f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.CARROT -> {
                val carrot = Path().apply {
                    moveTo(w * 0.32f, h * 0.30f)
                    lineTo(w * 0.68f, h * 0.30f)
                    cubicTo(w * 0.64f, h * 0.58f, w * 0.56f, h * 0.86f, cx, h * 0.88f)
                    cubicTo(w * 0.44f, h * 0.86f, w * 0.36f, h * 0.58f, w * 0.32f, h * 0.30f)
                    close()
                }
                drawPath(carrot, brush = Brush.verticalGradient(listOf(Color(0xFFFB923C), Color(0xFFEA580C))))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.30f), Offset(w * 0.38f, h * 0.12f), 5.5f, StrokeCap.Round)
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.30f), Offset(cx, h * 0.10f), 5.5f, StrokeCap.Round)
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.30f), Offset(w * 0.62f, h * 0.12f), 5.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.MONKEY -> {
                drawCircle(Color(0xFF92400E), w * 0.13f, Offset(w * 0.20f, h * 0.48f))
                drawCircle(Color(0xFF92400E), w * 0.13f, Offset(w * 0.80f, h * 0.48f))
                drawCircle(Color(0xFFB45309), w * 0.30f, Offset(cx, h * 0.52f))
                drawOval(Color(0xFFFDE68A), Offset(w * 0.30f, h * 0.48f), Size(w * 0.40f, h * 0.30f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.41f, h * 0.44f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.59f, h * 0.44f))
                val mSmile = Path().apply {
                    moveTo(w * 0.39f, h * 0.64f)
                    quadraticBezierTo(cx, h * 0.74f, w * 0.61f, h * 0.64f)
                }
                drawPath(mSmile, color = Color(0xFF78350F), style = Stroke(3.5f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.SHARPENER -> {
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    topLeft = Offset(w * 0.18f, h * 0.26f),
                    size = Size(w * 0.64f, h * 0.50f),
                    cornerRadius = CornerRadius(14f, 14f)
                )
                drawRoundRect(Color(0xFFE2E8F0), Offset(w * 0.34f, h * 0.36f), Size(w * 0.34f, h * 0.30f), CornerRadius(6f, 6f))
                drawCircle(Color(0xFF475569), w * 0.055f, Offset(w * 0.56f, h * 0.51f))
            }

            QiraatiWordArtKey.SHEEP -> {
                drawCircle(Color(0xFFF8FAFC), w * 0.17f, Offset(w * 0.32f, h * 0.48f))
                drawCircle(Color(0xFFF8FAFC), w * 0.20f, Offset(w * 0.50f, h * 0.42f))
                drawCircle(Color(0xFFF8FAFC), w * 0.17f, Offset(w * 0.68f, h * 0.48f))
                drawCircle(Color(0xFFF8FAFC), w * 0.17f, Offset(w * 0.42f, h * 0.62f))
                drawCircle(Color(0xFFF8FAFC), w * 0.17f, Offset(w * 0.58f, h * 0.62f))
                drawOval(Color(0xFF334155), Offset(w * 0.66f, h * 0.42f), Size(w * 0.22f, h * 0.26f))
                drawCircle(Color.White, 3.5f, Offset(w * 0.78f, h * 0.50f))
            }

            QiraatiWordArtKey.BED -> {
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.14f, h * 0.34f), Size(w * 0.10f, h * 0.46f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.76f, h * 0.48f), Size(w * 0.10f, h * 0.32f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.22f, h * 0.52f), Size(w * 0.56f, h * 0.20f), CornerRadius(10f, 10f))
                drawRoundRect(Color.White, Offset(w * 0.25f, h * 0.44f), Size(w * 0.20f, h * 0.12f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.WHISTLE -> {
                drawCircle(Color(0xFF0EA5E9), w * 0.22f, Offset(w * 0.38f, h * 0.58f))
                drawRoundRect(Color(0xFF0284C7), Offset(w * 0.48f, h * 0.42f), Size(w * 0.38f, h * 0.18f), CornerRadius(8f, 8f))
                drawCircle(ReadingColors.SunYellow, w * 0.08f, Offset(w * 0.38f, h * 0.58f))
            }

            QiraatiWordArtKey.BOAT, QiraatiWordArtKey.SHIP -> {
                val hull = Path().apply {
                    moveTo(w * 0.14f, h * 0.62f)
                    lineTo(w * 0.86f, h * 0.62f)
                    lineTo(w * 0.74f, h * 0.82f)
                    lineTo(w * 0.26f, h * 0.82f)
                    close()
                }
                drawPath(hull, brush = Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFF92400E))))
                drawLine(Color(0xFF78350F), Offset(cx, h * 0.20f), Offset(cx, h * 0.62f), 5f, StrokeCap.Round)
                val sail = Path().apply {
                    moveTo(cx, h * 0.22f)
                    quadraticBezierTo(w * 0.76f, h * 0.36f, w * 0.74f, h * 0.56f)
                    lineTo(cx, h * 0.56f)
                    close()
                }
                drawPath(sail, color = Color(0xFF38BDF8))
            }

            QiraatiWordArtKey.RICE -> {
                drawArc(Color(0xFF0EA5E9), 0f, 180f, true, Offset(w * 0.16f, h * 0.42f), Size(w * 0.68f, h * 0.42f))
                drawOval(Color.White, Offset(w * 0.22f, h * 0.36f), Size(w * 0.56f, h * 0.28f))
                drawOval(Color(0xFFF1F5F9), Offset(w * 0.30f, h * 0.30f), Size(w * 0.40f, h * 0.20f))
            }

            QiraatiWordArtKey.FEATHER -> {
                val feather = Path().apply {
                    moveTo(w * 0.22f, h * 0.82f)
                    cubicTo(w * 0.34f, h * 0.32f, w * 0.62f, h * 0.14f, w * 0.82f, h * 0.18f)
                    cubicTo(w * 0.76f, h * 0.48f, w * 0.54f, h * 0.72f, w * 0.22f, h * 0.82f)
                    close()
                }
                drawPath(feather, brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF8B5CF6))))
                drawLine(Color.White, Offset(w * 0.20f, h * 0.84f), Offset(w * 0.76f, h * 0.24f), 3.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.BRUSH -> {
                drawLine(Color(0xFFD97706), Offset(w * 0.22f, h * 0.82f), Offset(w * 0.64f, h * 0.40f), 10f, StrokeCap.Round)
                val bristles = Path().apply {
                    moveTo(w * 0.60f, h * 0.36f)
                    cubicTo(w * 0.76f, h * 0.16f, w * 0.88f, h * 0.24f, w * 0.70f, h * 0.46f)
                    close()
                }
                drawPath(bristles, color = ReadingColors.CoralOrange)
            }

            QiraatiWordArtKey.POND -> {
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFF7DD3FC), Color(0xFF0284C7))),
                    topLeft = Offset(w * 0.10f, h * 0.42f),
                    size = Size(w * 0.80f, h * 0.40f)
                )
                drawCircle(ReadingColors.MeadowGreen, w * 0.10f, Offset(w * 0.34f, h * 0.58f))
                drawCircle(Color(0xFFEC4899), w * 0.06f, Offset(w * 0.34f, h * 0.56f))
            }

            QiraatiWordArtKey.BASKET -> {
                drawArc(Color(0xFFD97706), 0f, 180f, true, Offset(w * 0.16f, h * 0.38f), Size(w * 0.68f, h * 0.48f))
                val handle = Path().apply {
                    moveTo(w * 0.24f, h * 0.56f)
                    cubicTo(w * 0.28f, h * 0.16f, w * 0.72f, h * 0.16f, w * 0.76f, h * 0.56f)
                }
                drawPath(handle, color = Color(0xFF92400E), style = Stroke(6f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.TURTLE -> {
                drawArc(Color(0xFF16A34A), 180f, 180f, true, Offset(w * 0.18f, h * 0.32f), Size(w * 0.58f, h * 0.48f))
                drawCircle(Color(0xFF4ADE80), w * 0.12f, Offset(w * 0.80f, h * 0.54f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.83f, h * 0.52f))
                drawRoundRect(Color(0xFF4ADE80), Offset(w * 0.24f, h * 0.64f), Size(w * 0.12f, h * 0.14f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF4ADE80), Offset(w * 0.58f, h * 0.64f), Size(w * 0.12f, h * 0.14f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.CURTAIN -> {
                drawLine(Color(0xFF78350F), Offset(w * 0.12f, h * 0.20f), Offset(w * 0.88f, h * 0.20f), 6f, StrokeCap.Round)
                val leftDrape = Path().apply {
                    moveTo(w * 0.18f, h * 0.22f)
                    lineTo(w * 0.48f, h * 0.22f)
                    quadraticBezierTo(w * 0.36f, h * 0.54f, w * 0.22f, h * 0.82f)
                    lineTo(w * 0.18f, h * 0.82f)
                    close()
                }
                val rightDrape = Path().apply {
                    moveTo(w * 0.82f, h * 0.22f)
                    lineTo(w * 0.52f, h * 0.22f)
                    quadraticBezierTo(w * 0.64f, h * 0.54f, w * 0.78f, h * 0.82f)
                    lineTo(w * 0.82f, h * 0.82f)
                    close()
                }
                drawPath(leftDrape, color = Color(0xFFEC4899))
                drawPath(rightDrape, color = Color(0xFFEC4899))
            }

            QiraatiWordArtKey.NAIL -> {
                drawRoundRect(Color(0xFF64748B), Offset(w * 0.28f, h * 0.18f), Size(w * 0.44f, h * 0.12f), CornerRadius(6f, 6f))
                drawLine(Color(0xFF94A3B8), Offset(cx, h * 0.30f), Offset(cx, h * 0.84f), 10f, StrokeCap.Round)
            }

            QiraatiWordArtKey.CLOCK -> {
                drawCircle(Color(0xFF0EA5E9), w * 0.36f, Offset(cx, cy))
                drawCircle(Color.White, w * 0.29f, Offset(cx, cy))
                drawLine(ReadingColors.InkDark, Offset(cx, cy), Offset(cx, h * 0.30f), 5f, StrokeCap.Round)
                drawLine(ReadingColors.CoralOrange, Offset(cx, cy), Offset(w * 0.66f, cy), 4.5f, StrokeCap.Round)
                drawCircle(ReadingColors.InkDark, 4f, Offset(cx, cy))
            }

            QiraatiWordArtKey.LAPTOP -> {
                drawRoundRect(Color(0xFF334155), Offset(w * 0.20f, h * 0.22f), Size(w * 0.60f, h * 0.42f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFF7DD3FC), Offset(w * 0.26f, h * 0.28f), Size(w * 0.48f, h * 0.30f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.12f, h * 0.66f), Size(w * 0.76f, h * 0.12f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.CROCODILE -> {
                drawRoundRect(Color(0xFF22C55E), Offset(w * 0.14f, h * 0.46f), Size(w * 0.72f, h * 0.26f), CornerRadius(16f, 16f))
                drawCircle(Color(0xFF16A34A), w * 0.10f, Offset(w * 0.64f, h * 0.42f))
                drawCircle(Color.White, w * 0.055f, Offset(w * 0.64f, h * 0.42f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.65f, h * 0.42f))
                drawLine(Color.White, Offset(w * 0.58f, h * 0.64f), Offset(w * 0.82f, h * 0.64f), 4f, StrokeCap.Round)
            }

            QiraatiWordArtKey.BLACKBOARD, QiraatiWordArtKey.BOARD -> {
                drawRoundRect(Color(0xFF15803D), Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.56f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.56f), CornerRadius(12f, 12f), style = Stroke(6f))
                drawLine(Color.White, Offset(w * 0.26f, h * 0.40f), Offset(w * 0.70f, h * 0.40f), 4.5f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(w * 0.30f, h * 0.56f), Offset(w * 0.62f, h * 0.56f), 4.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.SUGAR -> {
                drawRoundRect(Color(0xFFF8FAFC), Offset(w * 0.20f, h * 0.46f), Size(w * 0.28f, h * 0.28f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFE0F2FE), Offset(w * 0.50f, h * 0.46f), Size(w * 0.28f, h * 0.28f), CornerRadius(6f, 6f))
                drawRoundRect(Color.White, Offset(w * 0.35f, h * 0.22f), Size(w * 0.28f, h * 0.28f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFCBD5E1), Offset(w * 0.35f, h * 0.22f), Size(w * 0.28f, h * 0.28f), CornerRadius(6f, 6f), style = Stroke(3f))
            }

            QiraatiWordArtKey.SQUIRREL -> {
                val tail = Path().apply {
                    moveTo(w * 0.40f, h * 0.68f)
                    cubicTo(w * 0.10f, h * 0.66f, w * 0.10f, h * 0.20f, w * 0.36f, h * 0.24f)
                    cubicTo(w * 0.48f, h * 0.28f, w * 0.44f, h * 0.52f, w * 0.46f, h * 0.68f)
                    close()
                }
                drawPath(tail, color = Color(0xFFD97706))
                drawOval(Color(0xFFB45309), Offset(w * 0.44f, h * 0.42f), Size(w * 0.30f, h * 0.36f))
                drawCircle(Color(0xFFD97706), w * 0.14f, Offset(w * 0.64f, h * 0.38f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.68f, h * 0.36f))
            }

            QiraatiWordArtKey.MARKET -> {
                drawRoundRect(Color(0xFFFEF3C7), Offset(w * 0.18f, h * 0.42f), Size(w * 0.64f, h * 0.42f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFFEF4444), Offset(w * 0.14f, h * 0.24f), Size(w * 0.72f, h * 0.20f), CornerRadius(10f, 10f))
                drawRoundRect(Color.White, Offset(w * 0.28f, h * 0.24f), Size(w * 0.14f, h * 0.20f), CornerRadius(4f, 4f))
                drawRoundRect(Color.White, Offset(w * 0.56f, h * 0.24f), Size(w * 0.14f, h * 0.20f), CornerRadius(4f, 4f))
            }

            QiraatiWordArtKey.RULER -> {
                rotate(degrees = -25f, pivot = Offset(cx, cy)) {
                    drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.12f, h * 0.36f), Size(w * 0.76f, h * 0.28f), CornerRadius(8f, 8f))
                    for (i in 0..5) {
                        val x = w * (0.20f + i * 0.12f)
                        drawLine(Color(0xFF92400E), Offset(x, h * 0.36f), Offset(x, h * 0.48f), 3.5f, StrokeCap.Round)
                    }
                }
            }

            QiraatiWordArtKey.DRESS -> {
                val gown = Path().apply {
                    moveTo(w * 0.38f, h * 0.20f)
                    lineTo(w * 0.62f, h * 0.20f)
                    lineTo(w * 0.58f, h * 0.44f)
                    lineTo(w * 0.80f, h * 0.84f)
                    lineTo(w * 0.20f, h * 0.84f)
                    lineTo(w * 0.42f, h * 0.44f)
                    close()
                }
                drawPath(gown, brush = Brush.verticalGradient(listOf(Color(0xFFF472B6), Color(0xFFEC4899))))
                drawLine(ReadingColors.SunYellow, Offset(w * 0.40f, h * 0.44f), Offset(w * 0.60f, h * 0.44f), 6f, StrokeCap.Round)
            }

            QiraatiWordArtKey.EAGLE -> {
                val wings = Path().apply {
                    moveTo(w * 0.10f, h * 0.42f)
                    quadraticBezierTo(w * 0.32f, h * 0.22f, cx, h * 0.46f)
                    quadraticBezierTo(w * 0.68f, h * 0.22f, w * 0.90f, h * 0.42f)
                    lineTo(w * 0.72f, h * 0.62f)
                    lineTo(cx, h * 0.54f)
                    lineTo(w * 0.28f, h * 0.62f)
                    close()
                }
                drawPath(wings, color = Color(0xFF78350F))
                drawCircle(Color.White, w * 0.11f, Offset(cx, h * 0.36f))
                drawCircle(ReadingColors.SunYellow, w * 0.055f, Offset(cx, h * 0.42f))
            }

            QiraatiWordArtKey.DOTS -> {
                drawCircle(ReadingColors.CoralOrange, w * 0.13f, Offset(w * 0.32f, h * 0.38f))
                drawCircle(ReadingColors.SkyBlue, w * 0.13f, Offset(w * 0.68f, h * 0.38f))
                drawCircle(ReadingColors.MeadowGreen, w * 0.13f, Offset(cx, h * 0.68f))
            }

            QiraatiWordArtKey.STARS -> {
                fun miniStar(sx: Float, sy: Float, r: Float, col: Color) {
                    val s = Path().apply {
                        for (i in 0 until 10) {
                            val rad = if (i % 2 == 0) r else r * 0.44f
                            val a = i * (PI / 5.0) - PI / 2.0
                            val x = sx + (rad * cos(a)).toFloat()
                            val y = sy + (rad * sin(a)).toFloat()
                            if (i == 0) moveTo(x, y) else lineTo(x, y)
                        }
                        close()
                    }
                    drawPath(s, color = col)
                }
                miniStar(w * 0.34f, h * 0.42f, w * 0.20f, ReadingColors.SunYellow)
                miniStar(w * 0.68f, h * 0.36f, w * 0.15f, ReadingColors.CoralOrange)
                miniStar(w * 0.56f, h * 0.70f, w * 0.16f, Color(0xFF38BDF8))
            }

            QiraatiWordArtKey.HORSE -> {
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.20f, h * 0.44f), Size(w * 0.48f, h * 0.28f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.56f, h * 0.24f), Size(w * 0.24f, h * 0.28f), CornerRadius(12f, 12f))
                drawLine(Color(0xFF78350F), Offset(w * 0.28f, h * 0.70f), Offset(w * 0.28f, h * 0.86f), 7f, StrokeCap.Round)
                drawLine(Color(0xFF78350F), Offset(w * 0.60f, h * 0.70f), Offset(w * 0.60f, h * 0.86f), 7f, StrokeCap.Round)
                drawCircle(Color.White, 3.5f, Offset(w * 0.70f, h * 0.34f))
            }

            QiraatiWordArtKey.FIRE -> {
                val outerFlame = Path().apply {
                    moveTo(cx, h * 0.14f)
                    cubicTo(w * 0.82f, h * 0.44f, w * 0.82f, h * 0.84f, cx, h * 0.86f)
                    cubicTo(w * 0.18f, h * 0.84f, w * 0.18f, h * 0.44f, cx, h * 0.14f)
                    close()
                }
                drawPath(outerFlame, brush = Brush.verticalGradient(listOf(ReadingColors.SunYellow, Color(0xFFEF4444))))
                drawCircle(Color(0xFFFEF08A), w * 0.15f, Offset(cx, h * 0.66f))
            }

            QiraatiWordArtKey.SOLDIERS -> {
                drawCartoonPerson(w * 0.34f, h * 0.42f, scale = 0.86f, shirtColor = Color(0xFF15803D))
                drawCartoonPerson(w * 0.66f, h * 0.42f, scale = 0.86f, shirtColor = Color(0xFF16A34A))
            }

            QiraatiWordArtKey.TIGERS -> {
                drawCircle(Color(0xFFF97316), w * 0.32f, Offset(cx, h * 0.54f))
                drawCircle(Color(0xFFEA580C), w * 0.10f, Offset(w * 0.26f, h * 0.28f))
                drawCircle(Color(0xFFEA580C), w * 0.10f, Offset(w * 0.74f, h * 0.28f))
                drawLine(ReadingColors.InkDark, Offset(w * 0.42f, h * 0.26f), Offset(w * 0.42f, h * 0.38f), 4f, StrokeCap.Round)
                drawLine(ReadingColors.InkDark, Offset(cx, h * 0.24f), Offset(cx, h * 0.38f), 4f, StrokeCap.Round)
                drawLine(ReadingColors.InkDark, Offset(w * 0.58f, h * 0.26f), Offset(w * 0.58f, h * 0.38f), 4f, StrokeCap.Round)
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.40f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.60f, h * 0.50f))
            }

            QiraatiWordArtKey.GIRL, QiraatiWordArtKey.SCHOOLGIRL -> {
                drawCartoonPerson(cx, h * 0.40f, scale = 1.15f, shirtColor = Color(0xFFEC4899))
                drawCircle(ReadingColors.SunYellow, w * 0.08f, Offset(w * 0.30f, h * 0.28f))
                drawCircle(ReadingColors.SunYellow, w * 0.08f, Offset(w * 0.70f, h * 0.28f))
            }

            QiraatiWordArtKey.HAZELNUT -> {
                val nut = Path().apply {
                    moveTo(cx, h * 0.84f)
                    cubicTo(w * 0.18f, h * 0.72f, w * 0.20f, h * 0.34f, cx, h * 0.26f)
                    cubicTo(w * 0.80f, h * 0.34f, w * 0.82f, h * 0.72f, cx, h * 0.84f)
                    close()
                }
                drawPath(nut, brush = Brush.radialGradient(listOf(Color(0xFFD97706), Color(0xFF78350F))))
                drawArc(Color(0xFFFDE68A), 180f, 180f, true, Offset(w * 0.24f, h * 0.20f), Size(w * 0.52f, h * 0.30f))
            }

            QiraatiWordArtKey.SICKLE -> {
                drawLine(Color(0xFF92400E), Offset(w * 0.26f, h * 0.82f), Offset(w * 0.44f, h * 0.56f), 11f, StrokeCap.Round)
                val blade = Path().apply {
                    moveTo(w * 0.44f, h * 0.56f)
                    cubicTo(w * 0.24f, h * 0.22f, w * 0.72f, h * 0.10f, w * 0.82f, h * 0.44f)
                }
                drawPath(blade, color = Color(0xFF64748B), style = Stroke(10f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.DATES -> {
                drawOval(Color(0xFF92400E), Offset(w * 0.18f, h * 0.40f), Size(w * 0.32f, h * 0.40f))
                drawOval(Color(0xFFB45309), Offset(w * 0.48f, h * 0.36f), Size(w * 0.32f, h * 0.40f))
                drawOval(Color(0xFF78350F), Offset(w * 0.34f, h * 0.22f), Size(w * 0.32f, h * 0.38f))
            }

            QiraatiWordArtKey.APPLE -> {
                val apple = Path().apply {
                    moveTo(cx, h * 0.32f)
                    cubicTo(w * 0.76f, h * 0.18f, w * 0.90f, h * 0.48f, w * 0.80f, h * 0.74f)
                    cubicTo(w * 0.72f, h * 0.90f, w * 0.56f, h * 0.90f, cx, h * 0.84f)
                    cubicTo(w * 0.44f, h * 0.90f, w * 0.28f, h * 0.90f, w * 0.20f, h * 0.74f)
                    cubicTo(w * 0.10f, h * 0.48f, w * 0.24f, h * 0.18f, cx, h * 0.32f)
                    close()
                }
                drawPath(apple, brush = Brush.radialGradient(listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFB91C1C))))
                drawLine(Color(0xFF78350F), Offset(cx, h * 0.32f), Offset(cx, h * 0.14f), 5f, StrokeCap.Round)
                drawOval(ReadingColors.MeadowGreen, Offset(cx + 2f, h * 0.16f), Size(w * 0.20f, h * 0.11f))
            }

            QiraatiWordArtKey.SPIDER -> {
                drawCircle(Color(0xFF334155), w * 0.18f, Offset(cx, h * 0.52f))
                for (side in listOf(-1f, 1f)) {
                    for (i in 0..2) {
                        val yOff = h * (0.40f + i * 0.12f)
                        drawLine(Color(0xFF334155), Offset(cx + side * w * 0.14f, yOff), Offset(cx + side * w * 0.38f, yOff - 6f + i * 8f), 4f, StrokeCap.Round)
                    }
                }
                drawCircle(Color.White, 3.5f, Offset(w * 0.44f, h * 0.48f))
                drawCircle(Color.White, 3.5f, Offset(w * 0.56f, h * 0.48f))
            }

            QiraatiWordArtKey.JACKET -> {
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.26f, h * 0.22f), Size(w * 0.48f, h * 0.62f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFF0284C7), Offset(w * 0.12f, h * 0.26f), Size(w * 0.18f, h * 0.44f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFF0284C7), Offset(w * 0.70f, h * 0.26f), Size(w * 0.18f, h * 0.44f), CornerRadius(10f, 10f))
                drawLine(ReadingColors.SunYellow, Offset(cx, h * 0.24f), Offset(cx, h * 0.82f), 4.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.CROWN -> {
                val crown = Path().apply {
                    moveTo(w * 0.16f, h * 0.76f)
                    lineTo(w * 0.12f, h * 0.34f)
                    lineTo(w * 0.34f, h * 0.52f)
                    lineTo(cx, h * 0.22f)
                    lineTo(w * 0.66f, h * 0.52f)
                    lineTo(w * 0.88f, h * 0.34f)
                    lineTo(w * 0.84f, h * 0.76f)
                    close()
                }
                drawPath(crown, brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), ReadingColors.SunYellow, Color(0xFFD97706))))
                drawCircle(Color(0xFFEF4444), w * 0.055f, Offset(cx, h * 0.62f))
            }

            QiraatiWordArtKey.BERRY -> {
                val pts = listOf(
                    Offset(w * 0.38f, h * 0.42f), Offset(w * 0.62f, h * 0.42f),
                    Offset(w * 0.34f, h * 0.58f), Offset(cx, h * 0.54f), Offset(w * 0.66f, h * 0.58f),
                    Offset(w * 0.44f, h * 0.72f), Offset(w * 0.56f, h * 0.72f)
                )
                pts.forEach { drawCircle(Color(0xFF7E22CE), w * 0.11f, it) }
                drawOval(ReadingColors.MeadowGreen, Offset(w * 0.36f, h * 0.20f), Size(w * 0.28f, h * 0.14f))
            }

            QiraatiWordArtKey.FIG -> {
                val fig = Path().apply {
                    moveTo(cx, h * 0.18f)
                    cubicTo(w * 0.74f, h * 0.34f, w * 0.84f, h * 0.78f, cx, h * 0.84f)
                    cubicTo(w * 0.16f, h * 0.78f, w * 0.26f, h * 0.34f, cx, h * 0.18f)
                    close()
                }
                drawPath(fig, brush = Brush.verticalGradient(listOf(Color(0xFFA855F7), Color(0xFF6B21A8))))
            }

            QiraatiWordArtKey.STAMP -> {
                drawCircle(Color(0xFFB45309), w * 0.14f, Offset(cx, h * 0.28f))
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.42f, h * 0.36f), Size(w * 0.16f, h * 0.24f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.18f, h * 0.58f), Size(w * 0.64f, h * 0.22f), CornerRadius(10f, 10f))
            }

            QiraatiWordArtKey.MUSEUM -> {
                val ped = Path().apply {
                    moveTo(w * 0.12f, h * 0.36f)
                    lineTo(cx, h * 0.16f)
                    lineTo(w * 0.88f, h * 0.36f)
                    close()
                }
                drawPath(ped, color = Color(0xFFD97706))
                for (i in 0..2) {
                    val x = w * (0.24f + i * 0.22f)
                    drawRoundRect(Color(0xFFFDE68A), Offset(x, h * 0.38f), Size(w * 0.10f, h * 0.36f), CornerRadius(4f, 4f))
                }
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.14f, h * 0.74f), Size(w * 0.72f, h * 0.10f), CornerRadius(4f, 4f))
            }

            QiraatiWordArtKey.SEEDLINGS -> {
                drawArc(Color(0xFFB45309), 0f, 180f, true, Offset(w * 0.16f, h * 0.60f), Size(w * 0.68f, h * 0.30f))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.68f), Offset(cx, h * 0.32f), 6f, StrokeCap.Round)
                drawOval(Color(0xFF4ADE80), Offset(w * 0.24f, h * 0.26f), Size(w * 0.26f, h * 0.16f))
                drawOval(Color(0xFF22C55E), Offset(w * 0.50f, h * 0.26f), Size(w * 0.26f, h * 0.16f))
            }

            QiraatiWordArtKey.ERASER -> {
                rotate(degrees = -22f, pivot = Offset(cx, cy)) {
                    drawRoundRect(Color(0xFFEC4899), Offset(w * 0.16f, h * 0.34f), Size(w * 0.44f, h * 0.32f), CornerRadius(10f, 10f))
                    drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.54f, h * 0.34f), Size(w * 0.30f, h * 0.32f), CornerRadius(10f, 10f))
                }
            }

            QiraatiWordArtKey.BACKPACK -> {
                drawRoundRect(Color(0xFFEF4444), Offset(w * 0.20f, h * 0.24f), Size(w * 0.60f, h * 0.62f), CornerRadius(20f, 20f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.28f, h * 0.54f), Size(w * 0.44f, h * 0.24f), CornerRadius(12f, 12f))
                drawArc(Color(0xFFB91C1C), 180f, 180f, false, Offset(w * 0.34f, h * 0.12f), Size(w * 0.32f, h * 0.20f), style = Stroke(6f))
            }

            QiraatiWordArtKey.SCHOOL -> {
                drawRoundRect(Color(0xFFFEF3C7), Offset(w * 0.16f, h * 0.38f), Size(w * 0.68f, h * 0.46f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFF22C55E), Offset(w * 0.12f, h * 0.30f), Size(w * 0.76f, h * 0.12f), CornerRadius(8f, 8f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.42f, h * 0.56f), Size(w * 0.16f, h * 0.28f), CornerRadius(6f, 6f))
                drawCircle(Color(0xFF38BDF8), w * 0.065f, Offset(w * 0.28f, h * 0.56f))
                drawCircle(Color(0xFF38BDF8), w * 0.065f, Offset(w * 0.72f, h * 0.56f))
            }

            QiraatiWordArtKey.BOOK -> {
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.16f, h * 0.24f), Size(w * 0.68f, h * 0.56f), CornerRadius(12f, 12f))
                drawRoundRect(Color.White, Offset(w * 0.22f, h * 0.30f), Size(w * 0.26f, h * 0.44f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFFFFBEB), Offset(w * 0.52f, h * 0.30f), Size(w * 0.26f, h * 0.44f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.MEAT -> {
                drawOval(Color(0xFFDC2626), Offset(w * 0.14f, h * 0.26f), Size(w * 0.68f, h * 0.50f))
                drawOval(Color(0xFFFCA5A5), Offset(w * 0.22f, h * 0.34f), Size(w * 0.50f, h * 0.34f))
                drawCircle(Color.White, w * 0.10f, Offset(w * 0.40f, h * 0.50f))
            }

            QiraatiWordArtKey.KNIGHT -> {
                drawCartoonPerson(cx, h * 0.42f, scale = 1.05f, shirtColor = Color(0xFF0284C7))
                val helmet = Path().apply {
                    moveTo(w * 0.32f, h * 0.32f)
                    quadraticBezierTo(cx, h * 0.10f, w * 0.68f, h * 0.32f)
                    close()
                }
                drawPath(helmet, color = ReadingColors.SunYellow)
            }

            QiraatiWordArtKey.FISH -> {
                drawOval(Color(0xFF0EA5E9), Offset(w * 0.26f, h * 0.30f), Size(w * 0.56f, h * 0.40f))
                val tail = Path().apply {
                    moveTo(w * 0.30f, cy)
                    lineTo(w * 0.12f, h * 0.28f)
                    lineTo(w * 0.12f, h * 0.72f)
                    close()
                }
                drawPath(tail, color = ReadingColors.SunYellow)
                drawCircle(Color.White, w * 0.06f, Offset(w * 0.68f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.69f, h * 0.46f))
            }

            QiraatiWordArtKey.ROSE -> {
                drawCircle(Color(0xFFE11D48), w * 0.22f, Offset(cx, h * 0.38f))
                drawCircle(Color(0xFFFB7185), w * 0.14f, Offset(cx, h * 0.38f))
                drawCircle(Color(0xFFBE123C), w * 0.07f, Offset(cx, h * 0.38f))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.58f), Offset(cx, h * 0.88f), 6.5f, StrokeCap.Round)
                drawOval(Color(0xFF4ADE80), Offset(cx + 4f, h * 0.66f), Size(w * 0.22f, h * 0.12f))
            }

            QiraatiWordArtKey.PAINTING -> {
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.14f, h * 0.20f), Size(w * 0.72f, h * 0.58f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFFBAE6FD), Offset(w * 0.22f, h * 0.28f), Size(w * 0.56f, h * 0.42f), CornerRadius(6f, 6f))
                val hill = Path().apply {
                    moveTo(w * 0.22f, h * 0.70f)
                    lineTo(w * 0.44f, h * 0.46f)
                    lineTo(w * 0.62f, h * 0.60f)
                    lineTo(w * 0.78f, h * 0.70f)
                    close()
                }
                drawPath(hill, color = ReadingColors.MeadowGreen)
                drawCircle(ReadingColors.SunYellow, w * 0.065f, Offset(w * 0.64f, h * 0.38f))
            }

            // ================================================================
            // رسوم كلمات الوحدة الثالثة (وطني): و، ي، ق، ل، ط، ع، تنوين الفتح
            // ================================================================
            QiraatiWordArtKey.FACES -> {
                drawCircle(Color(0xFFFDE68A), w * 0.20f, Offset(w * 0.34f, h * 0.50f))
                drawCircle(Color(0xFFFED7AA), w * 0.20f, Offset(w * 0.66f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.28f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.40f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.60f, h * 0.46f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.72f, h * 0.46f))
            }

            QiraatiWordArtKey.PILLOW -> {
                drawRoundRect(Color(0xFF7DD3FC), Offset(w * 0.14f, h * 0.28f), Size(w * 0.72f, h * 0.46f), CornerRadius(22f, 22f))
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.22f, h * 0.36f), Size(w * 0.56f, h * 0.30f), CornerRadius(14f, 14f), style = Stroke(3.5f))
            }

            QiraatiWordArtKey.BUCKET -> {
                val b = Path().apply {
                    moveTo(w * 0.24f, h * 0.36f)
                    lineTo(w * 0.76f, h * 0.36f)
                    lineTo(w * 0.68f, h * 0.84f)
                    lineTo(w * 0.32f, h * 0.84f)
                    close()
                }
                drawPath(b, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
                drawArc(Color(0xFF64748B), 180f, 180f, false, Offset(w * 0.26f, h * 0.14f), Size(w * 0.48f, h * 0.40f), style = Stroke(5f))
            }

            QiraatiWordArtKey.WAVE, QiraatiWordArtKey.WATER_SPRING -> {
                val wavePath = Path().apply {
                    moveTo(w * 0.10f, h * 0.64f)
                    cubicTo(w * 0.28f, h * 0.22f, w * 0.56f, h * 0.22f, w * 0.68f, h * 0.50f)
                    cubicTo(w * 0.76f, h * 0.66f, w * 0.84f, h * 0.62f, w * 0.90f, h * 0.54f)
                    lineTo(w * 0.90f, h * 0.82f)
                    lineTo(w * 0.10f, h * 0.82f)
                    close()
                }
                drawPath(wavePath, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
            }

            QiraatiWordArtKey.FRUITS -> {
                drawCircle(Color(0xFFEF4444), w * 0.18f, Offset(w * 0.36f, h * 0.58f))
                drawCircle(Color(0xFFF97316), w * 0.18f, Offset(w * 0.64f, h * 0.58f))
                drawCircle(ReadingColors.MeadowGreen, w * 0.14f, Offset(cx, h * 0.38f))
            }

            QiraatiWordArtKey.PEACOCK -> {
                val fanCols = listOf(Color(0xFF0D9488), Color(0xFF0EA5E9), Color(0xFF22C55E), Color(0xFF0EA5E9), Color(0xFF0D9488))
                for (i in 0..4) {
                    val a = (-150.0 + i * 30.0) * PI / 180.0
                    val fx = cx + (w * 0.30f * cos(a)).toFloat()
                    val fy = h * 0.58f + (h * 0.30f * sin(a)).toFloat()
                    drawCircle(fanCols[i], w * 0.12f, Offset(fx, fy))
                    drawCircle(ReadingColors.SunYellow, w * 0.04f, Offset(fx, fy))
                }
                drawOval(Color(0xFF0284C7), Offset(w * 0.38f, h * 0.44f), Size(w * 0.24f, h * 0.38f))
            }

            QiraatiWordArtKey.COLORING -> {
                val cols = listOf(Color(0xFFEF4444), ReadingColors.SunYellow, ReadingColors.MeadowGreen, Color(0xFF0EA5E9))
                cols.forEachIndexed { idx, col ->
                    val x = w * (0.18f + idx * 0.17f)
                    drawRoundRect(col, Offset(x, h * 0.32f), Size(w * 0.12f, h * 0.50f), CornerRadius(6f, 6f))
                    val tip = Path().apply {
                        moveTo(x, h * 0.32f)
                        lineTo(x + w * 0.06f, h * 0.18f)
                        lineTo(x + w * 0.12f, h * 0.32f)
                        close()
                    }
                    drawPath(tip, color = Color(0xFFFDE68A))
                }
            }

            QiraatiWordArtKey.MEDICINE -> {
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.28f, h * 0.30f), Size(w * 0.44f, h * 0.54f), CornerRadius(12f, 12f))
                drawRoundRect(Color.White, Offset(w * 0.34f, h * 0.18f), Size(w * 0.32f, h * 0.14f), CornerRadius(6f, 6f))
                drawLine(Color.White, Offset(cx, h * 0.46f), Offset(cx, h * 0.68f), 7f, StrokeCap.Round)
                drawLine(Color.White, Offset(w * 0.39f, h * 0.57f), Offset(w * 0.61f, h * 0.57f), 7f, StrokeCap.Round)
            }

            QiraatiWordArtKey.BOW -> {
                val arc = Path().apply {
                    moveTo(w * 0.30f, h * 0.16f)
                    cubicTo(w * 0.78f, h * 0.28f, w * 0.78f, h * 0.72f, w * 0.30f, h * 0.84f)
                }
                drawPath(arc, color = Color(0xFFB45309), style = Stroke(7f, cap = StrokeCap.Round))
                drawLine(Color(0xFF64748B), Offset(w * 0.30f, h * 0.16f), Offset(w * 0.30f, h * 0.84f), 3f)
                drawLine(Color(0xFFEF4444), Offset(w * 0.20f, cy), Offset(w * 0.82f, cy), 5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.PEACH -> {
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFED7AA), Color(0xFFFB7185), Color(0xFFEA580C))),
                    radius = w * 0.33f,
                    center = Offset(cx, h * 0.56f)
                )
                drawOval(ReadingColors.MeadowGreen, Offset(cx, h * 0.16f), Size(w * 0.24f, h * 0.14f))
            }

            QiraatiWordArtKey.PUMPKIN -> {
                drawOval(Color(0xFFEA580C), Offset(w * 0.14f, h * 0.34f), Size(w * 0.36f, h * 0.48f))
                drawOval(Color(0xFFEA580C), Offset(w * 0.50f, h * 0.34f), Size(w * 0.36f, h * 0.48f))
                drawOval(Color(0xFFF97316), Offset(w * 0.28f, h * 0.32f), Size(w * 0.44f, h * 0.52f))
                drawLine(ReadingColors.MeadowGreen, Offset(cx, h * 0.32f), Offset(w * 0.56f, h * 0.16f), 7f, StrokeCap.Round)
            }

            QiraatiWordArtKey.PRAYING -> {
                drawRoundRect(Color(0xFF0D9488), Offset(w * 0.18f, h * 0.22f), Size(w * 0.64f, h * 0.62f), CornerRadius(12f, 12f))
                val arch = Path().apply {
                    moveTo(w * 0.30f, h * 0.74f)
                    lineTo(w * 0.30f, h * 0.44f)
                    quadraticBezierTo(cx, h * 0.22f, w * 0.70f, h * 0.44f)
                    lineTo(w * 0.70f, h * 0.74f)
                    close()
                }
                drawPath(arch, color = ReadingColors.SunYellow, style = Stroke(4.5f))
            }

            QiraatiWordArtKey.HAND, QiraatiWordArtKey.CLAPPING, QiraatiWordArtKey.FINGER -> {
                drawCircle(Color(0xFFFDE2C4), w * 0.22f, Offset(cx, h * 0.62f))
                for (i in 0..3) {
                    val fx = w * (0.30f + i * 0.13f)
                    drawLine(Color(0xFFFDE2C4), Offset(fx, h * 0.54f), Offset(fx, h * 0.24f), 11f, StrokeCap.Round)
                }
                drawLine(Color(0xFFFDE2C4), Offset(w * 0.28f, h * 0.62f), Offset(w * 0.16f, h * 0.48f), 11f, StrokeCap.Round)
            }

            QiraatiWordArtKey.FLUTE, QiraatiWordArtKey.STICKS -> {
                rotate(degrees = -30f, pivot = Offset(cx, cy)) {
                    drawRoundRect(Color(0xFFD97706), Offset(w * 0.12f, h * 0.43f), Size(w * 0.76f, h * 0.14f), CornerRadius(8f, 8f))
                    for (i in 0..3) {
                        drawCircle(Color(0xFF78350F), 4f, Offset(w * (0.30f + i * 0.14f), cy))
                    }
                }
            }

            QiraatiWordArtKey.LEMON -> {
                drawOval(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), ReadingColors.SunYellow, Color(0xFFEAB308))),
                    topLeft = Offset(w * 0.16f, h * 0.28f),
                    size = Size(w * 0.68f, h * 0.46f)
                )
                drawOval(ReadingColors.MeadowGreen, Offset(w * 0.58f, h * 0.18f), Size(w * 0.20f, h * 0.12f))
            }

            QiraatiWordArtKey.CUCUMBER, QiraatiWordArtKey.PEPPER -> {
                rotate(degrees = -28f, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF16A34A), Color(0xFF22C55E), Color(0xFF15803D))),
                        topLeft = Offset(w * 0.14f, h * 0.38f),
                        size = Size(w * 0.72f, h * 0.24f),
                        cornerRadius = CornerRadius(18f, 18f)
                    )
                }
            }

            QiraatiWordArtKey.BIRDS -> {
                drawCircle(Color(0xFF38BDF8), w * 0.16f, Offset(w * 0.34f, h * 0.46f))
                drawCircle(Color(0xFFFF7A59), w * 0.16f, Offset(w * 0.66f, h * 0.56f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.39f, h * 0.43f))
                drawCircle(ReadingColors.InkDark, 3f, Offset(w * 0.71f, h * 0.53f))
            }

            QiraatiWordArtKey.MILLIONS, QiraatiWordArtKey.COINS -> {
                drawCircle(ReadingColors.SunYellow, w * 0.22f, Offset(w * 0.36f, h * 0.58f))
                drawCircle(Color(0xFFF59E0B), w * 0.22f, Offset(w * 0.64f, h * 0.44f))
                drawCircle(Color.White.copy(alpha = 0.7f), w * 0.14f, Offset(w * 0.64f, h * 0.44f), style = Stroke(3f))
            }

            QiraatiWordArtKey.RESORTS, QiraatiWordArtKey.FIELD -> {
                val mtn = Path().apply {
                    moveTo(w * 0.08f, h * 0.80f)
                    lineTo(w * 0.38f, h * 0.26f)
                    lineTo(w * 0.64f, h * 0.62f)
                    lineTo(w * 0.78f, h * 0.38f)
                    lineTo(w * 0.92f, h * 0.80f)
                    close()
                }
                drawPath(mtn, brush = Brush.verticalGradient(listOf(Color(0xFF4ADE80), Color(0xFF15803D))))
                drawCircle(ReadingColors.SunYellow, w * 0.10f, Offset(w * 0.20f, h * 0.24f))
            }

            QiraatiWordArtKey.JASMINE -> {
                for (i in 0 until 6) {
                    val a = i * (2.0 * PI / 6.0)
                    drawCircle(Color.White, w * 0.14f, Offset(cx + (w * 0.20f * cos(a)).toFloat(), cy + (h * 0.20f * sin(a)).toFloat()))
                }
                drawCircle(ReadingColors.SunYellow, w * 0.12f, Offset(cx, cy))
            }

            QiraatiWordArtKey.EGGS -> {
                drawOval(Color(0xFFFFFBEB), Offset(w * 0.20f, h * 0.32f), Size(w * 0.32f, h * 0.44f))
                drawOval(Color(0xFFFEF3C7), Offset(w * 0.48f, h * 0.36f), Size(w * 0.32f, h * 0.44f))
            }

            QiraatiWordArtKey.CARDAMOM, QiraatiWordArtKey.LEAF, QiraatiWordArtKey.MINT -> {
                val leaf = Path().apply {
                    moveTo(w * 0.18f, h * 0.78f)
                    cubicTo(w * 0.20f, h * 0.26f, w * 0.66f, h * 0.14f, w * 0.84f, h * 0.22f)
                    cubicTo(w * 0.80f, h * 0.66f, w * 0.44f, h * 0.82f, w * 0.18f, h * 0.78f)
                    close()
                }
                drawPath(leaf, brush = Brush.linearGradient(listOf(Color(0xFF4ADE80), Color(0xFF16A34A))))
                drawLine(Color.White.copy(alpha = 0.65f), Offset(w * 0.22f, h * 0.74f), Offset(w * 0.76f, h * 0.28f), 3.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.OIL -> {
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.30f, h * 0.34f), Size(w * 0.40f, h * 0.50f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFFD97706), Offset(w * 0.40f, h * 0.16f), Size(w * 0.20f, h * 0.20f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.HAT -> {
                drawArc(Color(0xFF0EA5E9), 180f, 180f, true, Offset(w * 0.22f, h * 0.26f), Size(w * 0.56f, h * 0.56f))
                drawRoundRect(Color(0xFF0284C7), Offset(w * 0.12f, h * 0.52f), Size(w * 0.76f, h * 0.14f), CornerRadius(10f, 10f))
            }

            QiraatiWordArtKey.SHARK -> {
                drawOval(Color(0xFF64748B), Offset(w * 0.16f, h * 0.36f), Size(w * 0.68f, h * 0.34f))
                val fin = Path().apply {
                    moveTo(w * 0.42f, h * 0.38f)
                    lineTo(w * 0.52f, h * 0.16f)
                    lineTo(w * 0.62f, h * 0.38f)
                    close()
                }
                drawPath(fin, color = Color(0xFF475569))
                drawCircle(Color.White, 4f, Offset(w * 0.72f, h * 0.48f))
            }

            QiraatiWordArtKey.PENGUIN -> {
                drawOval(Color(0xFF1E293B), Offset(w * 0.26f, h * 0.18f), Size(w * 0.48f, h * 0.68f))
                drawOval(Color.White, Offset(w * 0.34f, h * 0.38f), Size(w * 0.32f, h * 0.42f))
                drawCircle(Color.White, 4f, Offset(w * 0.42f, h * 0.32f))
                drawCircle(Color.White, 4f, Offset(w * 0.58f, h * 0.32f))
                drawCircle(ReadingColors.CoralOrange, w * 0.06f, Offset(cx, h * 0.38f))
            }

            QiraatiWordArtKey.FALCON -> {
                drawCircle(Color(0xFF78350F), w * 0.24f, Offset(cx, h * 0.46f))
                val beak = Path().apply {
                    moveTo(w * 0.68f, h * 0.42f)
                    quadraticBezierTo(w * 0.90f, h * 0.46f, w * 0.74f, h * 0.58f)
                    close()
                }
                drawPath(beak, color = ReadingColors.SunYellow)
                drawCircle(Color.White, 4.5f, Offset(w * 0.58f, h * 0.40f))
            }

            QiraatiWordArtKey.FLOUR, QiraatiWordArtKey.COTTON -> {
                drawCircle(Color.White, w * 0.20f, Offset(w * 0.36f, h * 0.54f))
                drawCircle(Color.White, w * 0.22f, Offset(cx, h * 0.44f))
                drawCircle(Color.White, w * 0.20f, Offset(w * 0.64f, h * 0.54f))
                drawCircle(Color(0xFFCBD5E1), w * 0.22f, Offset(cx, h * 0.44f), style = Stroke(2.5f))
            }

            QiraatiWordArtKey.STORY -> {
                drawRoundRect(Color(0xFF8B5CF6), Offset(w * 0.18f, h * 0.22f), Size(w * 0.64f, h * 0.58f), CornerRadius(12f, 12f))
                drawRoundRect(Color.White, Offset(w * 0.26f, h * 0.30f), Size(w * 0.48f, h * 0.42f), CornerRadius(6f, 6f))
                drawCircle(ReadingColors.SunYellow, w * 0.10f, Offset(cx, h * 0.50f))
            }

            QiraatiWordArtKey.SCORPION -> {
                drawOval(Color(0xFFB45309), Offset(w * 0.28f, h * 0.44f), Size(w * 0.42f, h * 0.28f))
                val tail = Path().apply {
                    moveTo(w * 0.30f, h * 0.56f)
                    cubicTo(w * 0.10f, h * 0.44f, w * 0.18f, h * 0.18f, w * 0.42f, h * 0.24f)
                }
                drawPath(tail, color = Color(0xFF92400E), style = Stroke(8f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.TOY -> {
                drawRoundRect(Color(0xFFEF4444), Offset(w * 0.18f, h * 0.48f), Size(w * 0.30f, h * 0.30f), CornerRadius(8f, 8f))
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.52f, h * 0.48f), Size(w * 0.30f, h * 0.30f), CornerRadius(8f, 8f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.35f, h * 0.20f), Size(w * 0.30f, h * 0.30f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.TONGUE -> {
                val t = Path().apply {
                    moveTo(w * 0.26f, h * 0.30f)
                    lineTo(w * 0.74f, h * 0.30f)
                    cubicTo(w * 0.74f, h * 0.78f, w * 0.26f, h * 0.78f, w * 0.26f, h * 0.30f)
                    close()
                }
                drawPath(t, color = Color(0xFFFB7185))
                drawLine(Color(0xFFE11D48), Offset(cx, h * 0.34f), Offset(cx, h * 0.64f), 4f, StrokeCap.Round)
            }

            QiraatiWordArtKey.GAZELLE, QiraatiWordArtKey.DOG, QiraatiWordArtKey.FOX -> {
                val bodyCol = when (artKey) {
                    QiraatiWordArtKey.FOX -> Color(0xFFEA580C)
                    QiraatiWordArtKey.DOG -> Color(0xFF92400E)
                    else -> Color(0xFFD97706)
                }
                drawCircle(bodyCol, w * 0.28f, Offset(cx, h * 0.54f))
                drawOval(bodyCol, Offset(w * 0.24f, h * 0.16f), Size(w * 0.16f, h * 0.28f))
                drawOval(bodyCol, Offset(w * 0.60f, h * 0.16f), Size(w * 0.16f, h * 0.28f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.42f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.58f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 4.5f, Offset(cx, h * 0.62f))
            }

            QiraatiWordArtKey.PLAYER, QiraatiWordArtKey.SCIENTIST, QiraatiWordArtKey.DOCTOR, QiraatiWordArtKey.TAILOR, QiraatiWordArtKey.CAPTAIN -> {
                val shirtCol = when (artKey) {
                    QiraatiWordArtKey.DOCTOR -> Color(0xFFE0F2FE)
                    QiraatiWordArtKey.CAPTAIN -> Color(0xFF0369A1)
                    QiraatiWordArtKey.PLAYER -> Color(0xFF16A34A)
                    QiraatiWordArtKey.TAILOR -> Color(0xFFEC4899)
                    else -> Color(0xFF8B5CF6)
                }
                drawCartoonPerson(cx, h * 0.40f, scale = 1.15f, shirtColor = shirtCol)
            }

            QiraatiWordArtKey.HEARTS -> {
                drawWarmHeart(Offset(w * 0.36f, h * 0.46f), w * 0.20f, Color(0xFFE11D48))
                drawWarmHeart(Offset(w * 0.66f, h * 0.58f), w * 0.18f, Color(0xFFEC4899))
            }

            QiraatiWordArtKey.THREE -> {
                drawCircle(ReadingColors.CoralOrange, w * 0.14f, Offset(w * 0.26f, h * 0.58f))
                drawCircle(ReadingColors.SunYellow, w * 0.14f, Offset(cx, h * 0.36f))
                drawCircle(ReadingColors.SkyBlue, w * 0.14f, Offset(w * 0.74f, h * 0.58f))
            }

            QiraatiWordArtKey.BALLOON -> {
                drawOval(Color(0xFFEC4899), Offset(w * 0.26f, h * 0.14f), Size(w * 0.48f, h * 0.54f))
                drawLine(Color(0xFF64748B), Offset(cx, h * 0.68f), Offset(cx, h * 0.88f), 3.5f, StrokeCap.Round)
                drawCircle(Color.White.copy(alpha = 0.55f), w * 0.06f, Offset(w * 0.40f, h * 0.28f))
            }

            QiraatiWordArtKey.PLAYGROUND -> {
                drawRoundRect(Color(0xFF22C55E), Offset(w * 0.12f, h * 0.24f), Size(w * 0.76f, h * 0.52f), CornerRadius(12f, 12f))
                drawRoundRect(Color.White, Offset(w * 0.18f, h * 0.30f), Size(w * 0.64f, h * 0.40f), CornerRadius(8f, 8f), style = Stroke(3.5f))
                drawCircle(Color.White, w * 0.10f, Offset(cx, cy), style = Stroke(3.5f))
            }

            QiraatiWordArtKey.PERFUMES -> {
                drawRoundRect(Color(0xFFA855F7), Offset(w * 0.26f, h * 0.36f), Size(w * 0.48f, h * 0.46f), CornerRadius(14f, 14f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.38f, h * 0.20f), Size(w * 0.24f, h * 0.16f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.TRAIN -> {
                drawRoundRect(Color(0xFFEF4444), Offset(w * 0.14f, h * 0.38f), Size(w * 0.72f, h * 0.34f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFFBAE6FD), Offset(w * 0.24f, h * 0.44f), Size(w * 0.20f, h * 0.16f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFBAE6FD), Offset(w * 0.54f, h * 0.44f), Size(w * 0.20f, h * 0.16f), CornerRadius(6f, 6f))
                drawCircle(ReadingColors.InkDark, w * 0.09f, Offset(w * 0.30f, h * 0.76f))
                drawCircle(ReadingColors.InkDark, w * 0.09f, Offset(w * 0.70f, h * 0.76f))
            }

            QiraatiWordArtKey.RECTANGLE -> {
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.12f, h * 0.28f), Size(w * 0.76f, h * 0.44f), CornerRadius(10f, 10f))
                drawRoundRect(Color.White, Offset(w * 0.20f, h * 0.36f), Size(w * 0.60f, h * 0.28f), CornerRadius(6f, 6f), style = Stroke(3.5f))
            }

            QiraatiWordArtKey.HAMMER -> {
                drawLine(Color(0xFFB45309), Offset(cx, h * 0.40f), Offset(cx, h * 0.86f), 11f, StrokeCap.Round)
                drawRoundRect(Color(0xFF475569), Offset(w * 0.20f, h * 0.20f), Size(w * 0.60f, h * 0.22f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.KITCHEN -> {
                drawRoundRect(Color(0xFFFED7AA), Offset(w * 0.16f, h * 0.26f), Size(w * 0.68f, h * 0.56f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFF0EA5E9), Offset(w * 0.24f, h * 0.54f), Size(w * 0.52f, h * 0.22f), CornerRadius(8f, 8f))
                drawCircle(Color(0xFFEF4444), w * 0.09f, Offset(cx, h * 0.40f))
            }

            QiraatiWordArtKey.IRAQ_FLAG -> {
                drawLine(Color(0xFF64748B), Offset(w * 0.20f, h * 0.16f), Offset(w * 0.20f, h * 0.86f), 6f, StrokeCap.Round)
                drawRect(Color(0xFFDC2626), Offset(w * 0.22f, h * 0.20f), Size(w * 0.62f, h * 0.14f))
                drawRect(Color.White, Offset(w * 0.22f, h * 0.34f), Size(w * 0.62f, h * 0.14f))
                drawRect(Color(0xFF0F172A), Offset(w * 0.22f, h * 0.48f), Size(w * 0.62f, h * 0.14f))
                drawLine(Color(0xFF15803D), Offset(w * 0.38f, h * 0.41f), Offset(w * 0.68f, h * 0.41f), 5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.BONES -> {
                drawLine(Color(0xFFE2E8F0), Offset(w * 0.26f, cy), Offset(w * 0.74f, cy), 14f, StrokeCap.Round)
                drawCircle(Color(0xFFE2E8F0), w * 0.10f, Offset(w * 0.24f, h * 0.42f))
                drawCircle(Color(0xFFE2E8F0), w * 0.10f, Offset(w * 0.24f, h * 0.58f))
                drawCircle(Color(0xFFE2E8F0), w * 0.10f, Offset(w * 0.76f, h * 0.42f))
                drawCircle(Color(0xFFE2E8F0), w * 0.10f, Offset(w * 0.76f, h * 0.58f))
            }

            QiraatiWordArtKey.TORCH -> {
                drawLine(Color(0xFF92400E), Offset(cx, h * 0.46f), Offset(cx, h * 0.86f), 11f, StrokeCap.Round)
                val fl = Path().apply {
                    moveTo(cx, h * 0.14f)
                    cubicTo(w * 0.72f, h * 0.34f, w * 0.68f, h * 0.52f, cx, h * 0.52f)
                    cubicTo(w * 0.32f, h * 0.52f, w * 0.28f, h * 0.34f, cx, h * 0.14f)
                    close()
                }
                drawPath(fl, brush = Brush.verticalGradient(listOf(ReadingColors.SunYellow, Color(0xFFEF4444))))
            }

            QiraatiWordArtKey.OUD -> {
                drawOval(Color(0xFFB45309), Offset(w * 0.24f, h * 0.38f), Size(w * 0.52f, h * 0.46f))
                drawLine(Color(0xFF78350F), Offset(cx, h * 0.14f), Offset(cx, h * 0.44f), 9f, StrokeCap.Round)
                drawCircle(Color(0xFFFDE68A), w * 0.10f, Offset(cx, h * 0.58f))
            }

            QiraatiWordArtKey.GRAPES -> {
                val pts = listOf(
                    Offset(w * 0.36f, h * 0.38f), Offset(cx, h * 0.38f), Offset(w * 0.64f, h * 0.38f),
                    Offset(w * 0.42f, h * 0.54f), Offset(w * 0.58f, h * 0.54f),
                    Offset(cx, h * 0.70f)
                )
                pts.forEach { drawCircle(Color(0xFF7E22CE), w * 0.11f, it) }
                drawOval(ReadingColors.MeadowGreen, Offset(w * 0.42f, h * 0.18f), Size(w * 0.24f, h * 0.12f))
            }

            QiraatiWordArtKey.OSTRICH -> {
                drawOval(Color(0xFF334155), Offset(w * 0.22f, h * 0.48f), Size(w * 0.46f, h * 0.28f))
                drawLine(Color(0xFFFDA4AF), Offset(w * 0.62f, h * 0.52f), Offset(w * 0.70f, h * 0.24f), 7f, StrokeCap.Round)
                drawCircle(Color(0xFFFDA4AF), w * 0.09f, Offset(w * 0.70f, h * 0.24f))
                drawLine(Color(0xFFFDA4AF), Offset(w * 0.40f, h * 0.74f), Offset(w * 0.40f, h * 0.88f), 5f, StrokeCap.Round)
                drawLine(Color(0xFFFDA4AF), Offset(w * 0.54f, h * 0.74f), Offset(w * 0.54f, h * 0.88f), 5f, StrokeCap.Round)
            }

            // ===== رسوم الوحدة الرابعة (صحتي وبيئتي: ح، ش، ز) =====
            QiraatiWordArtKey.DOVE -> {
                // حمامة بيضاء ناعمة بجناح مقوّس وغصن زيتون أخضر
                val body = Path().apply {
                    moveTo(w * 0.20f, h * 0.62f)
                    cubicTo(w * 0.24f, h * 0.36f, w * 0.56f, h * 0.34f, w * 0.70f, h * 0.42f)
                    cubicTo(w * 0.78f, h * 0.56f, w * 0.64f, h * 0.74f, w * 0.38f, h * 0.72f)
                    close()
                }
                drawPath(body, color = Color.White)
                drawPath(body, color = Color(0xFF94A3B8), style = Stroke(2.5f))
                val wing = Path().apply {
                    moveTo(w * 0.36f, h * 0.52f)
                    cubicTo(w * 0.26f, h * 0.22f, w * 0.54f, h * 0.18f, w * 0.58f, h * 0.48f)
                    close()
                }
                drawPath(wing, brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE0F2FE))))
                drawCircle(Color.White, w * 0.12f, Offset(w * 0.70f, h * 0.38f))
                drawCircle(ReadingColors.InkDark, 3.5f, Offset(w * 0.73f, h * 0.36f))
                val beak = Path().apply {
                    moveTo(w * 0.80f, h * 0.36f)
                    lineTo(w * 0.90f, h * 0.40f)
                    lineTo(w * 0.80f, h * 0.43f)
                    close()
                }
                drawPath(beak, color = ReadingColors.CoralOrange)
                drawLine(Color(0xFF16A34A), Offset(w * 0.84f, h * 0.41f), Offset(w * 0.92f, h * 0.54f), 3f, StrokeCap.Round)
            }

            QiraatiWordArtKey.WHALE -> {
                // حوت أزرق مبتسم ينفث قطرات ماء
                val whale = Path().apply {
                    moveTo(w * 0.18f, h * 0.54f)
                    cubicTo(w * 0.20f, h * 0.26f, w * 0.76f, h * 0.26f, w * 0.82f, h * 0.58f)
                    cubicTo(w * 0.82f, h * 0.76f, w * 0.34f, h * 0.78f, w * 0.24f, h * 0.66f)
                    close()
                }
                drawPath(whale, brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
                val tail = Path().apply {
                    moveTo(w * 0.24f, h * 0.58f)
                    cubicTo(w * 0.08f, h * 0.44f, w * 0.08f, h * 0.62f, w * 0.16f, h * 0.68f)
                    close()
                }
                drawPath(tail, color = Color(0xFF0369A1))
                drawArc(Color(0xFFE0F2FE), 0f, 180f, true, Offset(w * 0.42f, h * 0.56f), Size(w * 0.34f, h * 0.18f))
                drawCircle(Color.White, 4.5f, Offset(w * 0.68f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 2.5f, Offset(w * 0.69f, h * 0.50f))
                // نافورة ماء لطيفة أعلى الحوت
                val spray1 = Path().apply {
                    moveTo(w * 0.52f, h * 0.32f)
                    quadraticBezierTo(w * 0.46f, h * 0.14f, w * 0.38f, h * 0.20f)
                }
                val spray2 = Path().apply {
                    moveTo(w * 0.52f, h * 0.32f)
                    quadraticBezierTo(w * 0.58f, h * 0.14f, w * 0.66f, h * 0.20f)
                }
                drawPath(spray1, color = Color(0xFF67E8F9), style = Stroke(3.5f, cap = StrokeCap.Round))
                drawPath(spray2, color = Color(0xFF67E8F9), style = Stroke(3.5f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.CANDLE -> {
                // شمعة وردية دافئة بشعلة ذهبية ناعمة
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFF9A8D4), Color(0xFFEC4899))),
                    topLeft = Offset(w * 0.34f, h * 0.40f),
                    size = Size(w * 0.32f, h * 0.46f),
                    cornerRadius = CornerRadius(10f, 10f)
                )
                drawLine(ReadingColors.InkDark, Offset(cx, h * 0.34f), Offset(cx, h * 0.41f), 3f, StrokeCap.Round)
                val flame = Path().apply {
                    moveTo(cx, h * 0.12f)
                    cubicTo(w * 0.64f, h * 0.24f, w * 0.60f, h * 0.36f, cx, h * 0.36f)
                    cubicTo(w * 0.40f, h * 0.36f, w * 0.36f, h * 0.24f, cx, h * 0.12f)
                    close()
                }
                drawPath(flame, brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF97316))))
            }

            QiraatiWordArtKey.OLIVES -> {
                // حبات زيتون خضراء مع أوراق نضرة
                val leaf = Path().apply {
                    moveTo(w * 0.24f, h * 0.36f)
                    quadraticBezierTo(w * 0.50f, h * 0.14f, w * 0.78f, h * 0.30f)
                    quadraticBezierTo(w * 0.52f, h * 0.42f, w * 0.24f, h * 0.36f)
                    close()
                }
                drawPath(leaf, color = Color(0xFF16A34A))
                drawOval(Color(0xFF65A30D), Offset(w * 0.24f, h * 0.44f), Size(w * 0.26f, h * 0.34f))
                drawOval(Color(0xFF4D7C0F), Offset(w * 0.50f, h * 0.46f), Size(w * 0.26f, h * 0.34f))
                drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(w * 0.33f, h * 0.54f))
                drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(w * 0.59f, h * 0.56f))
            }

            QiraatiWordArtKey.TOOTHBRUSH -> {
                // فرشاة أسنان فيروزية مع معجون نعناعي لامع
                drawLine(Color(0xFF06B6D4), Offset(w * 0.18f, h * 0.78f), Offset(w * 0.78f, h * 0.24f), 11f, StrokeCap.Round)
                drawRoundRect(Color.White, Offset(w * 0.60f, h * 0.18f), Size(w * 0.22f, h * 0.14f), CornerRadius(6f, 6f))
                val paste = Path().apply {
                    moveTo(w * 0.58f, h * 0.20f)
                    quadraticBezierTo(w * 0.72f, h * 0.08f, w * 0.84f, h * 0.20f)
                }
                drawPath(paste, color = Color(0xFFEC4899), style = Stroke(7f, cap = StrokeCap.Round))
            }

            QiraatiWordArtKey.GRAINS -> {
                drawArc(Color(0xFFD97706), 0f, 180f, true, Offset(w * 0.16f, h * 0.42f), Size(w * 0.68f, h * 0.42f))
                listOf(
                    Offset(w * 0.34f, h * 0.54f), Offset(w * 0.46f, h * 0.48f),
                    Offset(w * 0.58f, h * 0.52f), Offset(w * 0.68f, h * 0.56f),
                    Offset(w * 0.50f, h * 0.58f)
                ).forEach { drawCircle(ReadingColors.SunYellow, w * 0.07f, it) }
            }

            QiraatiWordArtKey.SHOE -> {
                val shoe = Path().apply {
                    moveTo(w * 0.16f, h * 0.66f)
                    lineTo(w * 0.24f, h * 0.36f)
                    lineTo(w * 0.46f, h * 0.36f)
                    quadraticBezierTo(w * 0.60f, h * 0.52f, w * 0.84f, h * 0.56f)
                    lineTo(w * 0.84f, h * 0.72f)
                    lineTo(w * 0.16f, h * 0.72f)
                    close()
                }
                drawPath(shoe, brush = Brush.horizontalGradient(listOf(Color(0xFFEC4899), Color(0xFFDB2777))))
                drawRoundRect(Color.White, Offset(w * 0.16f, h * 0.68f), Size(w * 0.68f, h * 0.08f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.WALL -> {
                drawRoundRect(Color(0xFFEA580C), Offset(w * 0.14f, h * 0.24f), Size(w * 0.72f, h * 0.54f), CornerRadius(10f, 10f))
                drawLine(Color.White, Offset(w * 0.14f, h * 0.42f), Offset(w * 0.86f, h * 0.42f), 3.5f)
                drawLine(Color.White, Offset(w * 0.14f, h * 0.60f), Offset(w * 0.86f, h * 0.60f), 3.5f)
                drawLine(Color.White, Offset(w * 0.50f, h * 0.24f), Offset(w * 0.50f, h * 0.42f), 3.5f)
                drawLine(Color.White, Offset(w * 0.34f, h * 0.42f), Offset(w * 0.34f, h * 0.60f), 3.5f)
                drawLine(Color.White, Offset(w * 0.66f, h * 0.42f), Offset(w * 0.66f, h * 0.60f), 3.5f)
            }

            QiraatiWordArtKey.SWEETS -> {
                drawCircle(Color(0xFFEC4899), w * 0.22f, Offset(cx, cy))
                drawCircle(Color.White, w * 0.14f, Offset(cx, cy), style = Stroke(4f))
                val wrapL = Path().apply {
                    moveTo(w * 0.28f, cy)
                    lineTo(w * 0.12f, h * 0.36f)
                    lineTo(w * 0.12f, h * 0.64f)
                    close()
                }
                val wrapR = Path().apply {
                    moveTo(w * 0.72f, cy)
                    lineTo(w * 0.88f, h * 0.36f)
                    lineTo(w * 0.88f, h * 0.64f)
                    close()
                }
                drawPath(wrapL, color = ReadingColors.SunYellow)
                drawPath(wrapR, color = ReadingColors.SunYellow)
            }

            QiraatiWordArtKey.BELT -> {
                drawRoundRect(Color(0xFF92400E), Offset(w * 0.12f, h * 0.40f), Size(w * 0.76f, h * 0.20f), CornerRadius(8f, 8f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.38f, h * 0.34f), Size(w * 0.24f, h * 0.32f), CornerRadius(8f, 8f), style = Stroke(6f))
            }

            QiraatiWordArtKey.WALLET -> {
                drawRoundRect(Color(0xFF06B6D4), Offset(w * 0.16f, h * 0.28f), Size(w * 0.68f, h * 0.46f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFF0E7490), Offset(w * 0.56f, h * 0.42f), Size(w * 0.28f, h * 0.18f), CornerRadius(8f, 8f))
                drawCircle(ReadingColors.SunYellow, w * 0.05f, Offset(w * 0.66f, h * 0.51f))
            }

            QiraatiWordArtKey.COAL -> {
                drawCircle(Color(0xFF334155), w * 0.20f, Offset(w * 0.38f, h * 0.58f))
                drawCircle(Color(0xFF1E293B), w * 0.22f, Offset(w * 0.58f, h * 0.56f))
                drawCircle(Color(0xFF475569), w * 0.18f, Offset(cx, h * 0.42f))
            }

            QiraatiWordArtKey.THERMOMETER -> {
                drawRoundRect(Color.White, Offset(w * 0.40f, h * 0.14f), Size(w * 0.20f, h * 0.58f), CornerRadius(12f, 12f))
                drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.40f, h * 0.14f), Size(w * 0.20f, h * 0.58f), CornerRadius(12f, 12f), style = Stroke(3.5f))
                drawCircle(Color(0xFFEF4444), w * 0.14f, Offset(cx, h * 0.74f))
                drawLine(Color(0xFFEF4444), Offset(cx, h * 0.34f), Offset(cx, h * 0.74f), 8f, StrokeCap.Round)
            }

            QiraatiWordArtKey.WINDOW -> {
                drawRoundRect(Color(0xFF7DD3FC), Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), CornerRadius(10f, 10f))
                drawRoundRect(Color(0xFFB45309), Offset(w * 0.18f, h * 0.18f), Size(w * 0.64f, h * 0.64f), CornerRadius(10f, 10f), style = Stroke(7f))
                drawLine(Color(0xFFB45309), Offset(cx, h * 0.18f), Offset(cx, h * 0.82f), 5f)
                drawLine(Color(0xFFB45309), Offset(w * 0.18f, cy), Offset(w * 0.82f, cy), 5f)
            }

            QiraatiWordArtKey.SAIL -> {
                val sail = Path().apply {
                    moveTo(cx, h * 0.14f)
                    quadraticBezierTo(w * 0.82f, h * 0.42f, w * 0.76f, h * 0.68f)
                    lineTo(cx, h * 0.68f)
                    close()
                }
                drawPath(sail, brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFBAE6FD))))
                drawLine(Color(0xFF92400E), Offset(cx, h * 0.14f), Offset(cx, h * 0.82f), 6f, StrokeCap.Round)
                drawArc(Color(0xFF0284C7), 0f, 180f, true, Offset(w * 0.22f, h * 0.66f), Size(w * 0.56f, h * 0.22f))
            }

            QiraatiWordArtKey.APRICOT -> {
                drawCircle(Color(0xFFFB923C), w * 0.24f, Offset(w * 0.42f, h * 0.56f))
                drawCircle(Color(0xFFF97316), w * 0.22f, Offset(w * 0.62f, h * 0.56f))
                drawOval(Color(0xFF16A34A), Offset(w * 0.44f, h * 0.20f), Size(w * 0.24f, h * 0.12f))
            }

            QiraatiWordArtKey.TREES -> {
                drawLine(Color(0xFF92400E), Offset(w * 0.34f, h * 0.52f), Offset(w * 0.34f, h * 0.84f), 9f, StrokeCap.Round)
                drawLine(Color(0xFF92400E), Offset(w * 0.66f, h * 0.56f), Offset(w * 0.66f, h * 0.84f), 9f, StrokeCap.Round)
                drawCircle(Color(0xFF16A34A), w * 0.20f, Offset(w * 0.34f, h * 0.40f))
                drawCircle(Color(0xFF22C55E), w * 0.18f, Offset(w * 0.66f, h * 0.46f))
            }

            QiraatiWordArtKey.STREET -> {
                val road = Path().apply {
                    moveTo(w * 0.36f, h * 0.18f)
                    lineTo(w * 0.64f, h * 0.18f)
                    lineTo(w * 0.86f, h * 0.84f)
                    lineTo(w * 0.14f, h * 0.84f)
                    close()
                }
                drawPath(road, color = Color(0xFF475569))
                drawLine(ReadingColors.SunYellow, Offset(cx, h * 0.26f), Offset(cx, h * 0.40f), 5f, StrokeCap.Round)
                drawLine(ReadingColors.SunYellow, Offset(cx, h * 0.52f), Offset(cx, h * 0.68f), 5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.PEELS -> {
                val peel = Path().apply {
                    moveTo(w * 0.20f, h * 0.66f)
                    quadraticBezierTo(cx, h * 0.24f, w * 0.80f, h * 0.66f)
                    quadraticBezierTo(cx, h * 0.52f, w * 0.20f, h * 0.66f)
                    close()
                }
                drawPath(peel, color = ReadingColors.SunYellow)
                drawPath(peel, color = Color(0xFFD97706), style = Stroke(3f))
            }

            QiraatiWordArtKey.SAWS -> {
                drawRoundRect(Color(0xFF94A3B8), Offset(w * 0.16f, h * 0.40f), Size(w * 0.52f, h * 0.20f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFFEA580C), Offset(w * 0.64f, h * 0.34f), Size(w * 0.22f, h * 0.32f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.CHALK -> {
                drawRoundRect(Color(0xFFEC4899), Offset(w * 0.20f, h * 0.30f), Size(w * 0.16f, h * 0.48f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF06B6D4), Offset(w * 0.42f, h * 0.24f), Size(w * 0.16f, h * 0.54f), CornerRadius(6f, 6f))
                drawRoundRect(ReadingColors.SunYellow, Offset(w * 0.64f, h * 0.34f), Size(w * 0.16f, h * 0.44f), CornerRadius(6f, 6f))
            }

            QiraatiWordArtKey.GRASS -> {
                for (i in 0..4) {
                    val bx = w * (0.20f + i * 0.15f)
                    val blade = Path().apply {
                        moveTo(bx, h * 0.80f)
                        quadraticBezierTo(bx + (if (i % 2 == 0) -8f else 8f), h * 0.48f, bx, h * 0.26f)
                    }
                    drawPath(blade, color = Color(0xFF16A34A), style = Stroke(8f, cap = StrokeCap.Round))
                }
            }

            QiraatiWordArtKey.COMB -> {
                drawRoundRect(Color(0xFFEC4899), Offset(w * 0.16f, h * 0.28f), Size(w * 0.68f, h * 0.16f), CornerRadius(8f, 8f))
                for (i in 0..5) {
                    val tx = w * (0.22f + i * 0.11f)
                    drawLine(Color(0xFFDB2777), Offset(tx, h * 0.44f), Offset(tx, h * 0.74f), 6f, StrokeCap.Round)
                }
            }

            QiraatiWordArtKey.WATER_BOTTLE -> {
                drawRoundRect(Color(0xFFBAE6FD), Offset(w * 0.30f, h * 0.30f), Size(w * 0.40f, h * 0.56f), CornerRadius(14f, 14f))
                drawRoundRect(Color(0xFF06B6D4), Offset(w * 0.38f, h * 0.16f), Size(w * 0.24f, h * 0.14f), CornerRadius(6f, 6f))
                drawRoundRect(Color(0xFF38BDF8), Offset(w * 0.34f, h * 0.48f), Size(w * 0.32f, h * 0.22f), CornerRadius(8f, 8f))
            }

            QiraatiWordArtKey.BUTTON -> {
                drawCircle(Color(0xFF06B6D4), w * 0.32f, Offset(cx, cy))
                drawCircle(Color.White, w * 0.26f, Offset(cx, cy), style = Stroke(3.5f))
                drawCircle(Color.White, w * 0.05f, Offset(w * 0.42f, h * 0.42f))
                drawCircle(Color.White, w * 0.05f, Offset(w * 0.58f, h * 0.42f))
                drawCircle(Color.White, w * 0.05f, Offset(w * 0.42f, h * 0.58f))
                drawCircle(Color.White, w * 0.05f, Offset(w * 0.58f, h * 0.58f))
            }

            QiraatiWordArtKey.GOAT -> {
                drawCircle(Color(0xFFF5F5F4), w * 0.26f, Offset(cx, h * 0.54f))
                drawCircle(Color(0xFF94A3B8), w * 0.26f, Offset(cx, h * 0.54f), style = Stroke(3f))
                drawLine(Color(0xFF92400E), Offset(w * 0.38f, h * 0.30f), Offset(w * 0.30f, h * 0.16f), 6f, StrokeCap.Round)
                drawLine(Color(0xFF92400E), Offset(w * 0.62f, h * 0.30f), Offset(w * 0.70f, h * 0.16f), 6f, StrokeCap.Round)
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.42f, h * 0.50f))
                drawCircle(ReadingColors.InkDark, 4f, Offset(w * 0.58f, h * 0.50f))
            }

            QiraatiWordArtKey.SNAIL -> {
                drawOval(Color(0xFFFDE68A), Offset(w * 0.18f, h * 0.58f), Size(w * 0.64f, h * 0.20f))
                drawCircle(Color(0xFFD97706), w * 0.22f, Offset(w * 0.44f, h * 0.48f))
                drawCircle(Color(0xFFFEF3C7), w * 0.12f, Offset(w * 0.44f, h * 0.48f), style = Stroke(4f))
                drawLine(Color(0xFFB45309), Offset(w * 0.74f, h * 0.60f), Offset(w * 0.80f, h * 0.44f), 3.5f, StrokeCap.Round)
            }

            QiraatiWordArtKey.ISLAND -> {
                drawRect(Color(0xFF38BDF8), Offset(w * 0.10f, h * 0.64f), Size(w * 0.80f, h * 0.20f))
                drawArc(Color(0xFFFCD34D), 180f, 180f, true, Offset(w * 0.22f, h * 0.48f), Size(w * 0.56f, h * 0.34f))
                drawLine(Color(0xFF92400E), Offset(cx, h * 0.32f), Offset(cx, h * 0.54f), 6f, StrokeCap.Round)
                drawCircle(Color(0xFF16A34A), w * 0.14f, Offset(cx, h * 0.28f))
            }

            QiraatiWordArtKey.DECORATION -> {
                drawLine(Color(0xFF64748B), Offset(w * 0.14f, h * 0.26f), Offset(w * 0.86f, h * 0.26f), 4f, StrokeCap.Round)
                val cols = listOf(Color(0xFFEC4899), ReadingColors.SunYellow, Color(0xFF06B6D4))
                cols.forEachIndexed { idx, c ->
                    val fx = w * (0.24f + idx * 0.26f)
                    val flag = Path().apply {
                        moveTo(fx - 12f, h * 0.28f)
                        lineTo(fx + 12f, h * 0.28f)
                        lineTo(fx, h * 0.62f)
                        close()
                    }
                    drawPath(flag, color = c)
                }
            }

            QiraatiWordArtKey.FLOWERS -> {
                listOf(
                    Offset(w * 0.32f, h * 0.44f) to Color(0xFFEC4899),
                    Offset(w * 0.68f, h * 0.46f) to Color(0xFF06B6D4),
                    Offset(cx, h * 0.32f) to ReadingColors.SunYellow
                ).forEach { (pt, col) ->
                    drawLine(Color(0xFF16A34A), pt, Offset(pt.x, h * 0.82f), 4.5f, StrokeCap.Round)
                    drawCircle(col, w * 0.14f, pt)
                    drawCircle(Color.White, w * 0.05f, pt)
                }
            }

            QiraatiWordArtKey.VASE -> {
                val vase = Path().apply {
                    moveTo(w * 0.36f, h * 0.44f)
                    lineTo(w * 0.64f, h * 0.44f)
                    cubicTo(w * 0.76f, h * 0.64f, w * 0.70f, h * 0.84f, cx, h * 0.84f)
                    cubicTo(w * 0.30f, h * 0.84f, w * 0.24f, h * 0.64f, w * 0.36f, h * 0.44f)
                    close()
                }
                drawPath(vase, brush = Brush.verticalGradient(listOf(Color(0xFF67E8F9), Color(0xFF06B6D4))))
                drawCircle(Color(0xFFEC4899), w * 0.11f, Offset(w * 0.42f, h * 0.30f))
                drawCircle(ReadingColors.SunYellow, w * 0.11f, Offset(w * 0.58f, h * 0.28f))
            }

            QiraatiWordArtKey.GENERIC_WORD -> {
                drawCircle(ReadingColors.SunYellow, w * 0.32f, Offset(cx, cy))
                drawCircle(Color.White, w * 0.16f, Offset(cx, cy))
            }
        }
    }
}
