package com.example.reading

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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * نموذج الشخصية الرمزية الخاصة بواحة القراءة الذكية فقط (معزولة تماماً عن الملف الرسمي للطالب).
 */
data class ReadingAvatar(
    val id: String,
    val name: String,
    val tagline: String,
    val frameColor: Color,
    val frameShadow: Color,
    val bgTop: Color,
    val bgBottom: Color
)

object ReadingAvatarCatalog {
    const val DEFAULT_AVATAR_ID: String = "owl"

    val all: List<ReadingAvatar> = listOf(
        ReadingAvatar(
            id = "owl",
            name = "فصيح",
            tagline = "بومة القراءة الذكية",
            frameColor = Color(0xFF0EA5E9),
            frameShadow = Color(0xFF0369A1),
            bgTop = Color(0xFFE0F2FE),
            bgBottom = Color(0xFFBAE6FD)
        ),
        ReadingAvatar(
            id = "lion",
            name = "شبل الشجاعة",
            tagline = "أسد صغير شجاع ومبتسم",
            frameColor = Color(0xFFF59E0B),
            frameShadow = Color(0xFFB45309),
            bgTop = Color(0xFFFFFBEB),
            bgBottom = Color(0xFFFDE68A)
        ),
        ReadingAvatar(
            id = "bunny",
            name = "أرنوب الذكي",
            tagline = "أرنب لطيف سريع الفهم",
            frameColor = Color(0xFFEC4899),
            frameShadow = Color(0xFFBE185D),
            bgTop = Color(0xFFFDF2F8),
            bgBottom = Color(0xFFFBCFE8)
        ),
        ReadingAvatar(
            id = "butterfly",
            name = "فراشة الألوان",
            tagline = "فراشة مبدعة تحب الحروف",
            frameColor = Color(0xFF8B5CF6),
            frameShadow = Color(0xFF6D28D9),
            bgTop = Color(0xFFF5F3FF),
            bgBottom = Color(0xFFDDD6FE)
        ),
        ReadingAvatar(
            id = "astronaut",
            name = "رائد النجوم",
            tagline = "مستكشف الفضاء والقراءة",
            frameColor = Color(0xFF2563EB),
            frameShadow = Color(0xFF1D4ED8),
            bgTop = Color(0xFFDBEAFE),
            bgBottom = Color(0xFFBFDBFE)
        ),
        ReadingAvatar(
            id = "cat",
            name = "قطوط اللطيف",
            tagline = "قط قارئ بنظارة صغيرة",
            frameColor = Color(0xFFFF7A59),
            frameShadow = Color(0xFFD14B2B),
            bgTop = Color(0xFFFFF7ED),
            bgBottom = Color(0xFFFED7AA)
        )
    )

    fun byId(avatarId: String?): ReadingAvatar {
        val key = avatarId?.trim()?.lowercase().orEmpty()
        return all.firstOrNull { it.id == key } ?: all.first()
    }
}

/**
 * رسم الشخصية الرمزية المختارة داخل إطار دائري مشرق بمنحنيات ناعمة وتدرجات لونية لامعة.
 */
@Composable
fun ReadingAvatarCanvas(
    avatarId: String,
    size: Dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier
) {
    val avatar = ReadingAvatarCatalog.byId(avatarId)

    val phase = if (animated) {
        val infinite = rememberInfiniteTransition(label = "avatar_anim_${avatar.id}")
        val value by infinite.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1750, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "avatar_phase"
        )
        value
    } else {
        0f
    }

    val twinkle = if (animated) {
        val infinite = rememberInfiniteTransition(label = "avatar_twinkle_${avatar.id}")
        val value by infinite.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2400, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "twinkle_phase"
        )
        value
    } else {
        0.5f
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(listOf(avatar.bgTop, avatar.bgBottom))
            )
            .border(
                width = (size * 0.055f).coerceIn(2.dp, 4.dp),
                brush = Brush.linearGradient(listOf(Color.White, avatar.frameColor)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w * 0.5f
            val cy = h * 0.5f

            // لمعة داخلية خلف الشخصية
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = 0.72f), Color.Transparent),
                    center = Offset(cx, h * 0.42f),
                    radius = w * 0.44f
                ),
                radius = w * 0.44f,
                center = Offset(cx, h * 0.42f)
            )

            when (avatar.id) {
                "owl" -> drawOwlAvatar(w, h, phase)
                "lion" -> drawLionAvatar(w, h, phase)
                "bunny" -> drawBunnyAvatar(w, h, phase)
                "butterfly" -> drawButterflyAvatar(w, h, phase)
                "astronaut" -> drawAstronautAvatar(w, h, phase, twinkle)
                "cat" -> drawCatAvatar(w, h, phase)
                else -> drawOwlAvatar(w, h, phase)
            }
        }
    }
}

private fun DrawScope.drawOwlAvatar(w: Float, h: Float, phase: Float) {
    val cx = w * 0.5f
    val bob = phase * h * 0.025f

    // الجناحان الجانبيان
    rotate(degrees = -phase * 8f, pivot = Offset(w * 0.28f, h * 0.58f + bob)) {
        val leftWing = Path().apply {
            moveTo(w * 0.28f, h * 0.48f + bob)
            cubicTo(w * 0.08f, h * 0.52f + bob, w * 0.09f, h * 0.76f + bob, w * 0.29f, h * 0.76f + bob)
            close()
        }
        drawPath(leftWing, brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
    }
    rotate(degrees = phase * 8f, pivot = Offset(w * 0.72f, h * 0.58f + bob)) {
        val rightWing = Path().apply {
            moveTo(w * 0.72f, h * 0.48f + bob)
            cubicTo(w * 0.92f, h * 0.52f + bob, w * 0.91f, h * 0.76f + bob, w * 0.71f, h * 0.76f + bob)
            close()
        }
        drawPath(rightWing, brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))))
    }

    // الجسم السماوي الممتلئ
    val body = Path().apply {
        moveTo(cx, h * 0.20f + bob)
        cubicTo(w * 0.78f, h * 0.20f + bob, w * 0.80f, h * 0.82f + bob, cx, h * 0.86f + bob)
        cubicTo(w * 0.20f, h * 0.82f + bob, w * 0.22f, h * 0.20f + bob, cx, h * 0.20f + bob)
        close()
    }
    drawPath(body, brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9), Color(0xFF0284C7))))

    // أذنا البومة الصغيرتان
    val earL = Path().apply {
        moveTo(w * 0.30f, h * 0.29f + bob)
        quadraticBezierTo(w * 0.24f, h * 0.14f + bob, w * 0.40f, h * 0.22f + bob)
        close()
    }
    val earR = Path().apply {
        moveTo(w * 0.70f, h * 0.29f + bob)
        quadraticBezierTo(w * 0.76f, h * 0.14f + bob, w * 0.60f, h * 0.22f + bob)
        close()
    }
    drawPath(earL, color = Color(0xFF0284C7))
    drawPath(earR, color = Color(0xFF0284C7))

    // البطن الكريمي اللامع
    drawOval(
        brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF3C7))),
        topLeft = Offset(w * 0.32f, h * 0.56f + bob),
        size = Size(w * 0.36f, h * 0.26f)
    )

    // عينان كبيرتان لامعتان
    drawCircle(Color.White, w * 0.135f, Offset(w * 0.38f, h * 0.43f + bob))
    drawCircle(Color.White, w * 0.135f, Offset(w * 0.62f, h * 0.43f + bob))
    drawCircle(ReadingColors.InkDark, w * 0.068f, Offset(w * 0.39f, h * 0.43f + bob))
    drawCircle(ReadingColors.InkDark, w * 0.068f, Offset(w * 0.61f, h * 0.43f + bob))
    drawCircle(Color.White, w * 0.026f, Offset(w * 0.36f, h * 0.40f + bob))
    drawCircle(Color.White, w * 0.026f, Offset(w * 0.58f, h * 0.40f + bob))

    // المنقار الذهبي
    val beak = Path().apply {
        moveTo(cx, h * 0.46f + bob)
        lineTo(w * 0.43f, h * 0.54f + bob)
        quadraticBezierTo(cx, h * 0.61f + bob, w * 0.57f, h * 0.54f + bob)
        close()
    }
    drawPath(beak, brush = Brush.linearGradient(listOf(ReadingColors.SunYellow, ReadingColors.CoralOrange)))
}

private fun DrawScope.drawLionAvatar(w: Float, h: Float, phase: Float) {
    val cx = w * 0.5f
    val cy = h * 0.52f + phase * h * 0.02f

    // اللبدة الذهبية المتوهجة (بتلات دائرية متصلة)
    for (i in 0 until 12) {
        val angle = i * (2.0 * PI / 12.0) + phase * 0.04
        val px = cx + (w * 0.28f * cos(angle)).toFloat()
        val py = cy + (h * 0.28f * sin(angle)).toFloat()
        drawCircle(
            brush = Brush.radialGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706))),
            radius = w * 0.135f,
            center = Offset(px, py)
        )
    }

    // الأذنان
    drawCircle(Color(0xFFFBBF24), w * 0.095f, Offset(w * 0.28f, cy - h * 0.22f))
    drawCircle(Color(0xFFFBBF24), w * 0.095f, Offset(w * 0.72f, cy - h * 0.22f))
    drawCircle(Color(0xFFFDE68A), w * 0.055f, Offset(w * 0.28f, cy - h * 0.22f))
    drawCircle(Color(0xFFFDE68A), w * 0.055f, Offset(w * 0.72f, cy - h * 0.22f))

    // الوجه الدافئ المبتسم
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFFEF3C7), Color(0xFFFCD34D), Color(0xFFF59E0B)),
            center = Offset(cx, cy - h * 0.05f),
            radius = w * 0.30f
        ),
        radius = w * 0.27f,
        center = Offset(cx, cy)
    )

    // العينان والخدان
    drawCircle(ReadingColors.InkDark, w * 0.045f, Offset(w * 0.40f, cy - h * 0.04f))
    drawCircle(ReadingColors.InkDark, w * 0.045f, Offset(w * 0.60f, cy - h * 0.04f))
    drawCircle(Color.White, w * 0.018f, Offset(w * 0.385f, cy - h * 0.06f))
    drawCircle(Color.White, w * 0.018f, Offset(w * 0.585f, cy - h * 0.06f))
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.45f), w * 0.05f, Offset(w * 0.32f, cy + h * 0.06f))
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.45f), w * 0.05f, Offset(w * 0.68f, cy + h * 0.06f))

    // الأنف والابتسامة
    val nose = Path().apply {
        moveTo(w * 0.45f, cy + h * 0.02f)
        lineTo(w * 0.55f, cy + h * 0.02f)
        lineTo(cx, cy + h * 0.09f)
        close()
    }
    drawPath(nose, color = Color(0xFF92400E))
    val smile = Path().apply {
        moveTo(w * 0.40f, cy + h * 0.12f)
        quadraticBezierTo(cx, cy + h * 0.21f, w * 0.60f, cy + h * 0.12f)
    }
    drawPath(smile, color = Color(0xFF92400E), style = Stroke(width = w * 0.032f, cap = StrokeCap.Round))
}

private fun DrawScope.drawBunnyAvatar(w: Float, h: Float, phase: Float) {
    val cx = w * 0.5f
    val earSway = phase * 9f

    // الأذن اليسرى المتحركة بلطف
    rotate(degrees = -10f + earSway, pivot = Offset(w * 0.38f, h * 0.42f)) {
        drawOval(Color.White, Offset(w * 0.28f, h * 0.10f), Size(w * 0.18f, h * 0.42f))
        drawOval(Color(0xFFF9A8D4), Offset(w * 0.32f, h * 0.16f), Size(w * 0.10f, h * 0.30f))
    }
    // الأذن اليمنى المتحركة بلطف
    rotate(degrees = 10f - earSway, pivot = Offset(w * 0.62f, h * 0.42f)) {
        drawOval(Color.White, Offset(w * 0.54f, h * 0.10f), Size(w * 0.18f, h * 0.42f))
        drawOval(Color(0xFFF9A8D4), Offset(w * 0.58f, h * 0.16f), Size(w * 0.10f, h * 0.30f))
    }

    // الرأس الأبيض الناعم
    drawOval(
        brush = Brush.radialGradient(listOf(Color.White, Color(0xFFF8FAFC), Color(0xFFFCE7F3))),
        topLeft = Offset(w * 0.21f, h * 0.36f),
        size = Size(w * 0.58f, h * 0.48f)
    )

    // العينان اللامعتان
    drawCircle(ReadingColors.InkDark, w * 0.048f, Offset(w * 0.39f, h * 0.55f))
    drawCircle(ReadingColors.InkDark, w * 0.048f, Offset(w * 0.61f, h * 0.55f))
    drawCircle(Color.White, w * 0.020f, Offset(w * 0.375f, h * 0.53f))
    drawCircle(Color.White, w * 0.020f, Offset(w * 0.595f, h * 0.53f))

    // الخدان الورديان والأنف المبتسم
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.52f), w * 0.058f, Offset(w * 0.30f, h * 0.64f))
    drawCircle(Color(0xFFF472B6).copy(alpha = 0.52f), w * 0.058f, Offset(w * 0.70f, h * 0.64f))
    drawOval(Color(0xFFEC4899), Offset(w * 0.45f, h * 0.61f), Size(w * 0.10f, h * 0.07f))

    val smile = Path().apply {
        moveTo(w * 0.41f, h * 0.71f)
        quadraticBezierTo(cx, h * 0.79f, w * 0.59f, h * 0.71f)
    }
    drawPath(smile, color = Color(0xFFBE185D), style = Stroke(width = w * 0.03f, cap = StrokeCap.Round))
}

private fun DrawScope.drawButterflyAvatar(w: Float, h: Float, phase: Float) {
    val cx = w * 0.5f
    val cy = h * 0.52f
    val flap = phase * 10f

    // الجناحان العلويان والسفليان اليساريان
    rotate(degrees = flap, pivot = Offset(cx, cy)) {
        val leftUpper = Path().apply {
            moveTo(cx, cy)
            cubicTo(w * 0.22f, h * 0.14f, w * 0.06f, h * 0.38f, w * 0.24f, h * 0.56f)
            quadraticBezierTo(w * 0.36f, h * 0.60f, cx, cy)
            close()
        }
        val leftLower = Path().apply {
            moveTo(cx, cy)
            cubicTo(w * 0.22f, h * 0.58f, w * 0.18f, h * 0.84f, w * 0.40f, h * 0.78f)
            quadraticBezierTo(w * 0.46f, h * 0.70f, cx, cy)
            close()
        }
        drawPath(leftUpper, brush = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))))
        drawPath(leftLower, brush = Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFF8B5CF6))))
        drawCircle(ReadingColors.SunYellow, w * 0.055f, Offset(w * 0.27f, h * 0.39f))
    }

    // الجناحان العلويان والسفليان اليمينيان
    rotate(degrees = -flap, pivot = Offset(cx, cy)) {
        val rightUpper = Path().apply {
            moveTo(cx, cy)
            cubicTo(w * 0.78f, h * 0.14f, w * 0.94f, h * 0.38f, w * 0.76f, h * 0.56f)
            quadraticBezierTo(w * 0.64f, h * 0.60f, cx, cy)
            close()
        }
        val rightLower = Path().apply {
            moveTo(cx, cy)
            cubicTo(w * 0.78f, h * 0.58f, w * 0.82f, h * 0.84f, w * 0.60f, h * 0.78f)
            quadraticBezierTo(w * 0.54f, h * 0.70f, cx, cy)
            close()
        }
        drawPath(rightUpper, brush = Brush.linearGradient(listOf(Color(0xFFA855F7), Color(0xFFEC4899))))
        drawPath(rightLower, brush = Brush.linearGradient(listOf(Color(0xFFF472B6), Color(0xFF8B5CF6))))
        drawCircle(ReadingColors.SunYellow, w * 0.055f, Offset(w * 0.73f, h * 0.39f))
    }

    // جسم الفراشة وقرنا الاستشعار
    val antL = Path().apply {
        moveTo(cx, h * 0.34f)
        quadraticBezierTo(w * 0.38f, h * 0.18f, w * 0.34f, h * 0.22f)
    }
    val antR = Path().apply {
        moveTo(cx, h * 0.34f)
        quadraticBezierTo(w * 0.62f, h * 0.18f, w * 0.66f, h * 0.22f)
    }
    drawPath(antL, color = Color(0xFF581C87), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
    drawPath(antR, color = Color(0xFF581C87), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
    drawCircle(ReadingColors.SunYellow, w * 0.035f, Offset(w * 0.34f, h * 0.22f))
    drawCircle(ReadingColors.SunYellow, w * 0.035f, Offset(w * 0.66f, h * 0.22f))

    drawRoundRect(
        brush = Brush.verticalGradient(listOf(Color(0xFF7C3AED), Color(0xFF4C1D95))),
        topLeft = Offset(cx - w * 0.07f, h * 0.32f),
        size = Size(w * 0.14f, h * 0.46f),
        cornerRadius = CornerRadius(w * 0.07f, w * 0.07f)
    )
    drawCircle(Color.White, w * 0.024f, Offset(cx - w * 0.03f, h * 0.39f))
    drawCircle(Color.White, w * 0.024f, Offset(cx + w * 0.03f, h * 0.39f))
}

private fun DrawScope.drawAstronautAvatar(w: Float, h: Float, phase: Float, twinkle: Float) {
    val cx = w * 0.5f
    val bob = phase * h * 0.025f

    // نجوم فضائية صغيرة متلألئة
    val starAlpha = (0.45f + 0.55f * sin(twinkle * 2.0 * PI).toFloat().coerceAtLeast(0f))
    drawCircle(ReadingColors.SunYellow.copy(alpha = starAlpha), w * 0.04f, Offset(w * 0.18f, h * 0.24f))
    drawCircle(Color.White.copy(alpha = starAlpha), w * 0.032f, Offset(w * 0.82f, h * 0.22f))

    // هوائي الخوذة
    drawLine(Color(0xFF64748B), Offset(cx, h * 0.16f + bob), Offset(cx, h * 0.26f + bob), w * 0.035f, StrokeCap.Round)
    drawCircle(ReadingColors.SunYellow, w * 0.055f, Offset(cx, h * 0.15f + bob))

    // ياقة بدلة الفضاء
    drawRoundRect(
        brush = Brush.horizontalGradient(listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0284C7))),
        topLeft = Offset(w * 0.22f, h * 0.72f + bob),
        size = Size(w * 0.56f, h * 0.20f),
        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
    )

    // الخوذة البيضاء اللامعة
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White, Color(0xFFF1F5F9), Color(0xFFCBD5E1)),
            center = Offset(w * 0.42f, h * 0.42f + bob),
            radius = w * 0.36f
        ),
        radius = w * 0.31f,
        center = Offset(cx, h * 0.50f + bob)
    )

    // الزجاج الأمامي الداكن المتلألئ بالنجوم
    drawOval(
        brush = Brush.linearGradient(
            colors = listOf(Color(0xFF1E3A8A), Color(0xFF312E81), Color(0xFF0F172A)),
            start = Offset(w * 0.25f, h * 0.34f + bob),
            end = Offset(w * 0.75f, h * 0.66f + bob)
        ),
        topLeft = Offset(w * 0.27f, h * 0.36f + bob),
        size = Size(w * 0.46f, h * 0.30f)
    )

    // لمعة منحنية على زجاج الخوذة + نجوم منعكسة
    val sheen = Path().apply {
        moveTo(w * 0.34f, h * 0.44f + bob)
        quadraticBezierTo(w * 0.44f, h * 0.39f + bob, w * 0.56f, h * 0.43f + bob)
    }
    drawPath(sheen, color = Color.White.copy(alpha = 0.72f), style = Stroke(w * 0.036f, cap = StrokeCap.Round))
    drawCircle(ReadingColors.SunYellow, w * 0.036f, Offset(w * 0.62f, h * 0.54f + bob))
    drawCircle(Color(0xFF7DD3FC), w * 0.025f, Offset(w * 0.40f, h * 0.56f + bob))
}

private fun DrawScope.drawCatAvatar(w: Float, h: Float, phase: Float) {
    val cx = w * 0.5f
    val cy = h * 0.55f + phase * h * 0.02f

    // الأذنان المثلثتان المنحنيتان
    val leftEar = Path().apply {
        moveTo(w * 0.24f, cy - h * 0.08f)
        quadraticBezierTo(w * 0.20f, h * 0.16f, w * 0.44f, cy - h * 0.22f)
        close()
    }
    val rightEar = Path().apply {
        moveTo(w * 0.76f, cy - h * 0.08f)
        quadraticBezierTo(w * 0.80f, h * 0.16f, w * 0.56f, cy - h * 0.22f)
        close()
    }
    drawPath(leftEar, color = Color(0xFFF97316))
    drawPath(rightEar, color = Color(0xFFF97316))

    // الوجه البرتقالي الدافئ
    drawOval(
        brush = Brush.radialGradient(listOf(Color(0xFFFDBA74), Color(0xFFFF7A59), Color(0xFFEA580C))),
        topLeft = Offset(w * 0.20f, cy - h * 0.24f),
        size = Size(w * 0.60f, h * 0.48f)
    )

    // خطوط جبهة القطة
    drawLine(Color(0xFFC2410C), Offset(cx, cy - h * 0.22f), Offset(cx, cy - h * 0.14f), w * 0.03f, StrokeCap.Round)
    drawLine(Color(0xFFC2410C), Offset(w * 0.42f, cy - h * 0.20f), Offset(w * 0.44f, cy - h * 0.13f), w * 0.026f, StrokeCap.Round)
    drawLine(Color(0xFFC2410C), Offset(w * 0.58f, cy - h * 0.20f), Offset(w * 0.56f, cy - h * 0.13f), w * 0.026f, StrokeCap.Round)

    // العينان + نظارة القراءة الصغيرة
    drawCircle(Color.White, w * 0.085f, Offset(w * 0.39f, cy - h * 0.02f))
    drawCircle(Color.White, w * 0.085f, Offset(w * 0.61f, cy - h * 0.02f))
    drawCircle(ReadingColors.InkDark, w * 0.042f, Offset(w * 0.39f, cy - h * 0.02f))
    drawCircle(ReadingColors.InkDark, w * 0.042f, Offset(w * 0.61f, cy - h * 0.02f))
    drawCircle(Color(0xFF0284C7), w * 0.105f, Offset(w * 0.39f, cy - h * 0.02f), style = Stroke(w * 0.032f))
    drawCircle(Color(0xFF0284C7), w * 0.105f, Offset(w * 0.61f, cy - h * 0.02f), style = Stroke(w * 0.032f))
    drawLine(Color(0xFF0284C7), Offset(w * 0.495f, cy - h * 0.02f), Offset(w * 0.505f, cy - h * 0.02f), w * 0.032f)

    // الأنف الوردي والابتسامة
    drawOval(Color(0xFFF43F5E), Offset(w * 0.46f, cy + h * 0.06f), Size(w * 0.08f, h * 0.055f))
    val smile = Path().apply {
        moveTo(w * 0.41f, cy + h * 0.13f)
        quadraticBezierTo(cx, cy + h * 0.20f, w * 0.59f, cy + h * 0.13f)
    }
    drawPath(smile, color = Color(0xFF7C2D12), style = Stroke(w * 0.03f, cap = StrokeCap.Round))
}

/**
 * كأس ذهبي لامع ينبض بلطف لزر "أبطال الصف" ولوحة المنافسة.
 */
@Composable
fun PlayfulTrophyIcon(
    modifier: Modifier = Modifier,
    animated: Boolean = true
) {
    val scale = if (animated) {
        val inf = rememberInfiniteTransition(label = "trophy_pulse")
        val v by inf.animateFloat(
            initialValue = 0.94f,
            targetValue = 1.08f,
            animationSpec = infiniteRepeatable(
                animation = tween(1150, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "trophy_scale"
        )
        v
    } else {
        1f
    }

    Canvas(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f

        // مقبضا الكأس المنحنيان
        val leftHandle = Path().apply {
            moveTo(w * 0.24f, h * 0.22f)
            cubicTo(w * 0.04f, h * 0.22f, w * 0.06f, h * 0.54f, w * 0.28f, h * 0.50f)
        }
        val rightHandle = Path().apply {
            moveTo(w * 0.76f, h * 0.22f)
            cubicTo(w * 0.96f, h * 0.22f, w * 0.94f, h * 0.54f, w * 0.72f, h * 0.50f)
        }
        drawPath(leftHandle, color = Color(0xFFD97706), style = Stroke(w * 0.08f, cap = StrokeCap.Round))
        drawPath(rightHandle, color = Color(0xFFD97706), style = Stroke(w * 0.08f, cap = StrokeCap.Round))

        // جسم الكأس الذهبي
        val cup = Path().apply {
            moveTo(w * 0.22f, h * 0.14f)
            lineTo(w * 0.78f, h * 0.14f)
            cubicTo(w * 0.76f, h * 0.54f, w * 0.64f, h * 0.64f, cx, h * 0.64f)
            cubicTo(w * 0.36f, h * 0.64f, w * 0.24f, h * 0.54f, w * 0.22f, h * 0.14f)
            close()
        }
        drawPath(
            cup,
            brush = Brush.linearGradient(listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706)))
        )

        // الساق والقاعدة
        drawRoundRect(Color(0xFFD97706), Offset(w * 0.43f, h * 0.62f), Size(w * 0.14f, h * 0.16f), CornerRadius(4f, 4f))
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFFB45309), ReadingColors.SunYellow, Color(0xFFB45309))),
            topLeft = Offset(w * 0.26f, h * 0.76f),
            size = Size(w * 0.48f, h * 0.14f),
            cornerRadius = CornerRadius(6f, 6f)
        )
        // لمعة صغيرة
        drawLine(Color.White.copy(alpha = 0.75f), Offset(w * 0.34f, h * 0.24f), Offset(w * 0.37f, h * 0.46f), w * 0.055f, StrokeCap.Round)
    }
}

/**
 * تاج ذهبي متوهج يطفو فوق بطل المركز الأول في منصة التتويج.
 */
@Composable
fun FloatingGoldenCrownCanvas(modifier: Modifier = Modifier) {
    val inf = rememberInfiniteTransition(label = "floating_crown")
    val floatY by inf.animateFloat(
        initialValue = -5f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "crown_y"
    )
    Canvas(modifier = modifier.graphicsLayer { translationY = floatY }) {
        val w = size.width
        val h = size.height
        val crown = Path().apply {
            moveTo(w * 0.12f, h * 0.80f)
            lineTo(w * 0.08f, h * 0.28f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.50f, h * 0.16f)
            lineTo(w * 0.68f, h * 0.50f)
            lineTo(w * 0.92f, h * 0.28f)
            lineTo(w * 0.88f, h * 0.80f)
            close()
        }
        drawPath(
            crown,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFF7B2), ReadingColors.SunYellow, Color(0xFFD97706)))
        )
        drawPath(crown, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.5f, join = StrokeJoin.Round))
        drawCircle(Color(0xFFE11D48), w * 0.065f, Offset(w * 0.50f, h * 0.62f))
        drawCircle(Color(0xFF0EA5E9), w * 0.05f, Offset(w * 0.28f, h * 0.64f))
        drawCircle(Color(0xFF22C55E), w * 0.05f, Offset(w * 0.72f, h * 0.64f))
    }
}

/**
 * أيقونة تحديث دائرية مرسومة بالكامل على Canvas (بدون Icons.Default.*).
 */
@Composable
fun PlayfulRefreshIcon(
    tint: Color = Color.White,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawArc(
            color = tint,
            startAngle = -55f,
            sweepAngle = 295f,
            useCenter = false,
            topLeft = Offset(w * 0.16f, h * 0.16f),
            size = Size(w * 0.68f, h * 0.68f),
            style = Stroke(width = w * 0.12f, cap = StrokeCap.Round)
        )
        val head = Path().apply {
            moveTo(w * 0.72f, h * 0.12f)
            lineTo(w * 0.88f, h * 0.34f)
            lineTo(w * 0.62f, h * 0.34f)
            close()
        }
        drawPath(head, color = tint)
    }
}

/**
 * نافذة اختيار الشخصية الرمزية (شبكة من عمودين تضم 6 شخصيات طفولية لطيفة مع إطار ذهبي متوهج).
 */
@Composable
fun ReadingAvatarPickerDialog(
    currentAvatarId: String,
    onSelectAvatar: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                color = Color(0xFFF8FCFF),
                shadowElevation = 12.dp,
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .border(3.dp, ReadingColors.SunYellow, RoundedCornerShape(30.dp))
                    .testTag("reading_avatar_picker_dialog")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlayfulCrownIcon(modifier = Modifier.size(28.dp))
                        Text(
                            text = "اختر شخصيتك في واحة القراءة",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = ReadingColors.InkDark
                        )
                    }

                    Text(
                        text = "شخصيتك الرمزية تظهر لك ولأصدقائك في ساحة أبطال الصف",
                        fontFamily = ReadingTajawalFont,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = ReadingColors.InkSoft,
                        textAlign = TextAlign.Center
                    )

                    ReadingAvatarCatalog.all.chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { avatar ->
                                val isSelected = avatar.id == ReadingAvatarCatalog.byId(currentAvatarId).id
                                val cardScale by animateFloatAsState(
                                    targetValue = if (isSelected) 1.04f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "avatar_pick_scale_${avatar.id}"
                                )

                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = if (isSelected) Color(0xFFFFFBEB) else Color.White,
                                    shadowElevation = if (isSelected) 8.dp else 3.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .graphicsLayer {
                                            scaleX = cardScale
                                            scaleY = cardScale
                                        }
                                        .border(
                                            width = if (isSelected) 3.dp else 1.5.dp,
                                            color = if (isSelected) ReadingColors.SunYellow else Color(0xFFE2E8F0),
                                            shape = RoundedCornerShape(24.dp)
                                        )
                                        .clickable { onSelectAvatar(avatar.id) }
                                        .testTag("reading_avatar_option_${avatar.id}")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.TopEnd) {
                                            ReadingAvatarCanvas(
                                                avatarId = avatar.id,
                                                size = 76.dp,
                                                animated = true
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .clip(CircleShape)
                                                        .background(ReadingColors.MeadowGreen)
                                                        .border(2.dp, Color.White, CircleShape),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    PlayfulCheckIcon(tint = Color.White, modifier = Modifier.size(15.dp))
                                                }
                                            }
                                        }

                                        Text(
                                            text = avatar.name,
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 15.5.sp,
                                            color = ReadingColors.InkDark,
                                            textAlign = TextAlign.Center
                                        )
                                        Text(
                                            text = avatar.tagline,
                                            fontFamily = ReadingTajawalFont,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.5.sp,
                                            color = ReadingColors.InkSoft,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    PlayfulBouncyButton(
                        onClick = onDismiss,
                        backgroundColor = ReadingColors.MeadowGreen,
                        shadowColor = ReadingColors.MeadowShadow,
                        cornerRadius = 20.dp,
                        lipDepth = 4.dp,
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 11.dp),
                        modifier = Modifier.fillMaxWidth(),
                        testTagValue = "reading_avatar_picker_close_btn"
                    ) {
                        Text(
                            text = "حفظ وإغلاق",
                            fontFamily = ReadingTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
