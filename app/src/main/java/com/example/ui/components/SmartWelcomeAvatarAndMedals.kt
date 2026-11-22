package com.example.ui.components

import android.graphics.BitmapFactory
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AttendanceStatus
import com.example.model.UserRole
import com.example.util.ImageCompressor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import kotlin.math.cos
import kotlin.math.sin

enum class SmartAvatarPersona {
    STUDENT_BOY,
    STUDENT_GIRL,
    STAFF_MALE,
    STAFF_FEMALE
}

data class DailyStatusBadgeSpec(
    val label: String,
    val ringColors: List<Color>,
    val pillBackground: Color,
    val pillBorder: Color,
    val pillTextColor: Color,
    val dotColor: Color? = null,
    val pulseDot: Boolean = false
)

fun resolveSmartAvatarPersona(
    role: UserRole?,
    gender: String?,
    schoolGenderType: String?,
    fullName: String? = null,
    isBusDriver: Boolean = false
): SmartAvatarPersona {
    val g = gender.orEmpty().trim()
    val schoolG = schoolGenderType.orEmpty().trim()
    val name = fullName.orEmpty().trim()

    val isFemale = when {
        isBusDriver -> false
        g == "أنثى" || g == "انثى" || g.equals("FEMALE", ignoreCase = true) ||
            g.contains("بنات") || g.contains("طالبة") || g.contains("معلمة") ||
            g.contains("مديرة") || g.contains("أستاذة") -> true
        g == "ذكر" || g.equals("MALE", ignoreCase = true) ||
            g.contains("بنين") || g.contains("طالب") || g.contains("معلم") ||
            g.contains("مدير") || g.contains("سائق") -> false
        name.startsWith("الست ") || name.startsWith("أستاذة") ||
            name.startsWith("معلمة") || name.startsWith("مديرة") -> true
        schoolG.contains("بنات") -> true
        else -> false
    }

    val isStudentContext = role == UserRole.STUDENT || role == UserRole.PARENT
    return when {
        isStudentContext && isFemale -> SmartAvatarPersona.STUDENT_GIRL
        isStudentContext -> SmartAvatarPersona.STUDENT_BOY
        isFemale -> SmartAvatarPersona.STAFF_FEMALE
        else -> SmartAvatarPersona.STAFF_MALE
    }
}

fun resolveDailyStatusBadgeSpec(
    isParentOrStudent: Boolean,
    attendanceStatus: AttendanceStatus?,
    isHolidayToday: Boolean,
    staffDailyStatusText: String = "متصل الآن"
): DailyStatusBadgeSpec {
    if (!isParentOrStudent) {
        return if (isHolidayToday) {
            DailyStatusBadgeSpec(
                label = "عطلة",
                ringColors = listOf(Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFBAE6FD)),
                pillBackground = Color(0xFF0C4A6E),
                pillBorder = Color(0xFF7DD3FC),
                pillTextColor = Color(0xFFE0F2FE),
                dotColor = Color(0xFF38BDF8),
                pulseDot = false
            )
        } else {
            DailyStatusBadgeSpec(
                label = staffDailyStatusText.ifBlank { "متصل الآن" },
                ringColors = listOf(Color(0xFF34D399), Color(0xFFFDE68A), Color(0xFF10B981)),
                pillBackground = Color(0xFF064E3B),
                pillBorder = Color(0xFF6EE7B7),
                pillTextColor = Color(0xFFECFDF5),
                dotColor = Color(0xFF34D399),
                pulseDot = true
            )
        }
    }

    val effectiveStatus = if (isHolidayToday) AttendanceStatus.HOLIDAY else (attendanceStatus ?: AttendanceStatus.UNRECORDED)
    return when (effectiveStatus) {
        AttendanceStatus.PRESENT -> DailyStatusBadgeSpec(
            label = "حاضر",
            ringColors = listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF34D399)),
            pillBackground = Color(0xFF065F46),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color.White,
            dotColor = Color(0xFF34D399),
            pulseDot = true
        )
        AttendanceStatus.ABSENT -> DailyStatusBadgeSpec(
            label = "غائب",
            ringColors = listOf(Color(0xFFFCA5A5), Color(0xFFEF4444), Color(0xFFF87171)),
            pillBackground = Color(0xFF7F1D1D),
            pillBorder = Color(0xFFFCA5A5),
            pillTextColor = Color(0xFFFEF2F2),
            dotColor = Color(0xFFF87171),
            pulseDot = false
        )
        AttendanceStatus.EXCUSED -> DailyStatusBadgeSpec(
            label = "إجازة",
            ringColors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFFBBF24)),
            pillBackground = Color(0xFF78350F),
            pillBorder = Color(0xFFFDE68A),
            pillTextColor = Color(0xFFFFFBEB),
            dotColor = Color(0xFFFBBF24),
            pulseDot = false
        )
        AttendanceStatus.LATE -> DailyStatusBadgeSpec(
            label = "متأخر",
            ringColors = listOf(Color(0xFFFDBA74), Color(0xFFF97316), Color(0xFFFB923C)),
            pillBackground = Color(0xFF7C2D12),
            pillBorder = Color(0xFFFDBA74),
            pillTextColor = Color(0xFFFFF7ED),
            dotColor = Color(0xFFFB923C),
            pulseDot = false
        )
        AttendanceStatus.HOLIDAY -> DailyStatusBadgeSpec(
            label = "عطلة",
            ringColors = listOf(Color(0xFFBAE6FD), Color(0xFF38BDF8), Color(0xFF7DD3FC)),
            pillBackground = Color(0xFF0C4A6E),
            pillBorder = Color(0xFF7DD3FC),
            pillTextColor = Color(0xFFE0F2FE),
            dotColor = Color(0xFF38BDF8),
            pulseDot = false
        )
        AttendanceStatus.UNRECORDED -> DailyStatusBadgeSpec(
            label = "بانتظار الرصد",
            ringColors = listOf(Color.White.copy(alpha = 0.78f), Color(0xFFA7F3D0).copy(alpha = 0.65f), Color.White.copy(alpha = 0.78f)),
            pillBackground = Color(0xFF0F3D5E).copy(alpha = 0.92f),
            pillBorder = Color.White.copy(alpha = 0.55f),
            pillTextColor = Color.White,
            dotColor = null,
            pulseDot = false
        )
    }
}

/**
 * Loads an ImageBitmap cleanly from either a Base64 string or an HTTP/HTTPS URL.
 */
@Composable
fun rememberSmartProfileImageBitmap(
    photoSource: String?,
    maxDimension: Int = 360
): ImageBitmap? {
    val cleanSource = photoSource.orEmpty().trim()
    val isRemoteUrl = cleanSource.startsWith("http://", ignoreCase = true) ||
        cleanSource.startsWith("https://", ignoreCase = true)

    val base64Bitmap = remember(cleanSource, isRemoteUrl) {
        if (cleanSource.isBlank() || isRemoteUrl) {
            null
        } else {
            ImageCompressor.decodeBase64ToImageBitmap(cleanSource, maxDimension = maxDimension)
        }
    }

    var remoteBitmap by remember(cleanSource) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(cleanSource, isRemoteUrl) {
        if (!isRemoteUrl) {
            remoteBitmap = null
            return@LaunchedEffect
        }
        remoteBitmap = withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(cleanSource).openConnection() as? HttpURLConnection)?.apply {
                    connectTimeout = 6000
                    readTimeout = 6000
                    doInput = true
                } ?: return@runCatching null
                try {
                    conn.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                } finally {
                    conn.disconnect()
                }
            }.getOrNull()
        }
    }

    return if (isRemoteUrl) remoteBitmap else base64Bitmap
}

/**
 * صورة المستخدم الذكية التفاعلية للترويسة الترحيبية الرئيسية مع حلقة وشارة الحالة اليومية:
 * - الأولوية 1: الصورة الشخصية الحقيقية (Base64 أو URL) بدقة عالية.
 * - الأولوية 2: بورتريه هندسي متجه أنيق حسب الجنس والدور مع حركة هادئة جداً.
 * - مدمج بها حلقة لونية للحالة اليومية + كبسولة صغيرة متداخلة بنعومة أسفل الإطار.
 */
@Composable
fun SmartWelcomeProfileAvatar(
    photoSource: String?,
    contentDescription: String,
    persona: SmartAvatarPersona,
    statusBadge: DailyStatusBadgeSpec,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 86.dp
) {
    val imageBitmap = rememberSmartProfileImageBitmap(photoSource = photoSource, maxDimension = 360)
    val transition = rememberInfiniteTransition(label = "smart_welcome_avatar_anim")
    val dotPulseAlpha by transition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "status_dot_pulse_alpha"
    )

    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("welcome_smart_profile_avatar"),
        contentAlignment = Alignment.TopCenter
    ) {
        // Outer circular frosted frame + daily status ring
        Box(
            modifier = Modifier
                .padding(bottom = 12.dp)
                .size(avatarSize)
                .shadow(
                    elevation = 10.dp,
                    shape = CircleShape,
                    ambientColor = Color(0xFF022C22).copy(alpha = 0.40f),
                    spotColor = Color(0xFF022C22).copy(alpha = 0.45f)
                )
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .border(
                    width = 2.6.dp,
                    brush = Brush.sweepGradient(statusBadge.ringColors),
                    shape = CircleShape
                )
                .padding(4.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF065F46), Color(0xFF022C22))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Crop,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                SmartVectorPortraitCanvas(
                    persona = persona,
                    animateCalmly = true,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        // Overlapping bottom daily status capsule
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = (-1).dp)
                .testTag("welcome_avatar_daily_status_badge"),
            shape = RoundedCornerShape(50),
            color = statusBadge.pillBackground,
            border = BorderStroke(1.dp, statusBadge.pillBorder),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (statusBadge.dotColor != null) {
                    val effectiveAlpha = if (statusBadge.pulseDot) dotPulseAlpha else 1f
                    Box(
                        modifier = Modifier
                            .size(6.5.dp)
                            .clip(CircleShape)
                            .background(statusBadge.dotColor.copy(alpha = effectiveAlpha))
                    )
                }
                Text(
                    text = statusBadge.label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = statusBadge.pillTextColor,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * بورتريه هندسي متجه ومصقول بألوان المنصة (زمردي، أبيض، ولمسة ذهبية هادئة)
 * يختلف بدقة بين: طالب (ولد)، طالبة (بنت)، كادر ذكر، وكادر أنثى.
 */
@Composable
fun SmartVectorPortraitCanvas(
    persona: SmartAvatarPersona,
    animateCalmly: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "smart_vector_portrait_calm")
    val breatheScale by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portrait_breathe"
    )
    val glowAlpha by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.36f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "portrait_aura"
    )
    val sheenProgress by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "portrait_sheen"
    )

    val effScale = if (animateCalmly) breatheScale else 1f
    val effGlow = if (animateCalmly) glowAlpha else 0.24f

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.50f
        val cy = h * 0.50f
        val r = minOf(w, h) * 0.50f

        // 1) Calm platform backdrop gradient
        drawCircle(
            brush = Brush.verticalGradient(
                colors = listOf(Color(0xFFECFDF5), Color(0xFFD1FAE5))
            ),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF34D399).copy(alpha = effGlow), Color.Transparent),
                center = Offset(cx, h * 0.34f),
                radius = r * 0.85f
            ),
            radius = r * 0.85f,
            center = Offset(cx, h * 0.34f)
        )

        val circleClip = Path().apply {
            addOval(Rect(cx - r, cy - r, cx + r, cy + r))
        }

        clipPath(circleClip) {
            val headR = r * 0.29f * effScale
            val headCy = h * 0.39f
            val neckW = headR * 0.58f
            val shoulderTop = h * 0.63f

            when (persona) {
                SmartAvatarPersona.STUDENT_BOY -> {
                    // Uniform shoulders (Clean emerald blazer + crisp white collar)
                    val shoulders = Path().apply {
                        moveTo(cx - r * 0.72f, h * 1.04f)
                        quadraticBezierTo(cx - r * 0.62f, shoulderTop, cx - neckW * 0.7f, shoulderTop - r * 0.03f)
                        lineTo(cx + neckW * 0.7f, shoulderTop - r * 0.03f)
                        quadraticBezierTo(cx + r * 0.62f, shoulderTop, cx + r * 0.72f, h * 1.04f)
                        close()
                    }
                    drawPath(
                        path = shoulders,
                        brush = Brush.verticalGradient(listOf(Color(0xFF059669), Color(0xFF064E3B)))
                    )
                    // Crisp white student V-collar
                    val collar = Path().apply {
                        moveTo(cx - neckW * 0.9f, shoulderTop - r * 0.02f)
                        lineTo(cx, shoulderTop + r * 0.16f)
                        lineTo(cx + neckW * 0.9f, shoulderTop - r * 0.02f)
                        close()
                    }
                    drawPath(path = collar, color = Color.White)

                    // Neck
                    drawRoundRect(
                        color = Color(0xFFE7C4A8),
                        topLeft = Offset(cx - neckW * 0.45f, headCy + headR * 0.72f),
                        size = Size(neckW * 0.9f, r * 0.18f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Head circle
                    drawCircle(
                        color = Color(0xFFF3D5BC),
                        radius = headR,
                        center = Offset(cx, headCy)
                    )
                    // Neat geometric boy hairstyle
                    val hair = Path().apply {
                        moveTo(cx - headR * 1.02f, headCy - headR * 0.08f)
                        cubicTo(
                            cx - headR * 1.06f, headCy - headR * 1.18f,
                            cx + headR * 1.06f, headCy - headR * 1.18f,
                            cx + headR * 1.02f, headCy - headR * 0.08f
                        )
                        quadraticBezierTo(cx + headR * 0.55f, headCy - headR * 0.56f, cx - headR * 0.12f, headCy - headR * 0.44f)
                        quadraticBezierTo(cx - headR * 0.68f, headCy - headR * 0.36f, cx - headR * 1.02f, headCy - headR * 0.08f)
                        close()
                    }
                    drawPath(path = hair, color = Color(0xFF0F2940))
                }

                SmartAvatarPersona.STUDENT_GIRL -> {
                    // Refined emerald school pinafore shoulders
                    val shoulders = Path().apply {
                        moveTo(cx - r * 0.72f, h * 1.04f)
                        quadraticBezierTo(cx - r * 0.60f, shoulderTop, cx, shoulderTop - r * 0.04f)
                        quadraticBezierTo(cx + r * 0.60f, shoulderTop, cx + r * 0.72f, h * 1.04f)
                        close()
                    }
                    drawPath(
                        path = shoulders,
                        brush = Brush.verticalGradient(listOf(Color(0xFF059669), Color(0xFF064E3B)))
                    )
                    // Soft mint-white school hijab / drape framing face cleanly
                    val drapeOuter = Path().apply {
                        moveTo(cx - headR * 1.30f, headCy + headR * 0.10f)
                        cubicTo(
                            cx - headR * 1.32f, headCy - headR * 1.25f,
                            cx + headR * 1.32f, headCy - headR * 1.25f,
                            cx + headR * 1.30f, headCy + headR * 0.10f
                        )
                        quadraticBezierTo(cx + headR * 1.22f, shoulderTop + r * 0.18f, cx, shoulderTop + r * 0.24f)
                        quadraticBezierTo(cx - headR * 1.22f, shoulderTop + r * 0.18f, cx - headR * 1.30f, headCy + headR * 0.10f)
                        close()
                    }
                    drawPath(
                        path = drapeOuter,
                        brush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFECFDF5)))
                    )
                    drawPath(
                        path = drapeOuter,
                        color = Color(0xFF10B981).copy(alpha = 0.40f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    // Subtle golden trim curve on headscarf
                    drawArc(
                        color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                        startAngle = 210f,
                        sweepAngle = 120f,
                        useCenter = false,
                        topLeft = Offset(cx - headR * 1.06f, headCy - headR * 1.02f),
                        size = Size(headR * 2.12f, headR * 1.96f),
                        style = Stroke(width = 1.3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Face oval
                    drawCircle(
                        color = Color(0xFFF5D8C0),
                        radius = headR * 0.90f,
                        center = Offset(cx, headCy + headR * 0.04f)
                    )
                }

                SmartAvatarPersona.STAFF_MALE -> {
                    // Executive/Educator formal jacket shoulders
                    val suit = Path().apply {
                        moveTo(cx - r * 0.78f, h * 1.04f)
                        quadraticBezierTo(cx - r * 0.66f, shoulderTop - r * 0.02f, cx - neckW * 0.75f, shoulderTop - r * 0.05f)
                        lineTo(cx + neckW * 0.75f, shoulderTop - r * 0.05f)
                        quadraticBezierTo(cx + r * 0.66f, shoulderTop - r * 0.02f, cx + r * 0.78f, h * 1.04f)
                        close()
                    }
                    drawPath(
                        path = suit,
                        brush = Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF0F2940)))
                    )
                    // Crisp white shirt triangle + subtle gold-emerald tie
                    val shirtV = Path().apply {
                        moveTo(cx - neckW * 0.85f, shoulderTop - r * 0.04f)
                        lineTo(cx, shoulderTop + r * 0.28f)
                        lineTo(cx + neckW * 0.85f, shoulderTop - r * 0.04f)
                        close()
                    }
                    drawPath(path = shirtV, color = Color.White)
                    val tie = Path().apply {
                        moveTo(cx - r * 0.045f, shoulderTop + r * 0.01f)
                        lineTo(cx + r * 0.045f, shoulderTop + r * 0.01f)
                        lineTo(cx + r * 0.06f, shoulderTop + r * 0.23f)
                        lineTo(cx, shoulderTop + r * 0.29f)
                        lineTo(cx - r * 0.06f, shoulderTop + r * 0.23f)
                        close()
                    }
                    drawPath(
                        path = tie,
                        brush = Brush.verticalGradient(listOf(Color(0xFFF59E0B), Color(0xFFD97706)))
                    )
                    // Neck & composed head
                    drawRoundRect(
                        color = Color(0xFFE4BF9F),
                        topLeft = Offset(cx - neckW * 0.45f, headCy + headR * 0.70f),
                        size = Size(neckW * 0.9f, r * 0.18f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFF0CEB2),
                        radius = headR,
                        center = Offset(cx, headCy)
                    )
                    // Composed professional hair contour
                    val staffHair = Path().apply {
                        moveTo(cx - headR * 0.98f, headCy - headR * 0.15f)
                        cubicTo(
                            cx - headR * 1.02f, headCy - headR * 1.12f,
                            cx + headR * 1.02f, headCy - headR * 1.12f,
                            cx + headR * 0.98f, headCy - headR * 0.15f
                        )
                        quadraticBezierTo(cx, headCy - headR * 0.55f, cx - headR * 0.98f, headCy - headR * 0.15f)
                        close()
                    }
                    drawPath(path = staffHair, color = Color(0xFF1E293B))
                }

                SmartAvatarPersona.STAFF_FEMALE -> {
                    // Formal emerald executive blazer shoulders
                    val blazer = Path().apply {
                        moveTo(cx - r * 0.76f, h * 1.04f)
                        quadraticBezierTo(cx - r * 0.64f, shoulderTop - r * 0.02f, cx, shoulderTop - r * 0.05f)
                        quadraticBezierTo(cx + r * 0.64f, shoulderTop - r * 0.02f, cx + r * 0.76f, h * 1.04f)
                        close()
                    }
                    drawPath(
                        path = blazer,
                        brush = Brush.verticalGradient(listOf(Color(0xFF065F46), Color(0xFF022C22)))
                    )
                    // Dignified deep-mint & warm-gold layered scarf drape
                    val scarf = Path().apply {
                        moveTo(cx - headR * 1.32f, headCy + headR * 0.08f)
                        cubicTo(
                            cx - headR * 1.34f, headCy - headR * 1.24f,
                            cx + headR * 1.34f, headCy - headR * 1.24f,
                            cx + headR * 1.32f, headCy + headR * 0.08f
                        )
                        quadraticBezierTo(cx + headR * 1.26f, shoulderTop + r * 0.22f, cx, shoulderTop + r * 0.28f)
                        quadraticBezierTo(cx - headR * 1.26f, shoulderTop + r * 0.22f, cx - headR * 1.32f, headCy + headR * 0.08f)
                        close()
                    }
                    drawPath(
                        path = scarf,
                        brush = Brush.verticalGradient(listOf(Color(0xFF047857), Color(0xFF065F46)))
                    )
                    drawArc(
                        color = Color(0xFFFDE68A).copy(alpha = 0.75f),
                        startAngle = 205f,
                        sweepAngle = 130f,
                        useCenter = false,
                        topLeft = Offset(cx - headR * 1.08f, headCy - headR * 1.02f),
                        size = Size(headR * 2.16f, headR * 1.98f),
                        style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                    )
                    // Face oval
                    drawCircle(
                        color = Color(0xFFF4D5BC),
                        radius = headR * 0.88f,
                        center = Offset(cx, headCy + headR * 0.04f)
                    )
                }
            }

            // Minimalist calm facial accents (subtle eyes & serene gentle smile — geometric, never cartoonish)
            val eyeY = headCy + headR * 0.04f
            val eyeOffset = headR * 0.30f
            val eyeR = (headR * 0.095f).coerceAtLeast(1.6f)
            drawCircle(color = Color(0xFF1E293B), radius = eyeR, center = Offset(cx - eyeOffset, eyeY))
            drawCircle(color = Color(0xFF1E293B), radius = eyeR, center = Offset(cx + eyeOffset, eyeY))

            val smilePath = Path().apply {
                moveTo(cx - headR * 0.22f, headCy + headR * 0.36f)
                quadraticBezierTo(cx, headCy + headR * 0.50f, cx + headR * 0.22f, headCy + headR * 0.36f)
            }
            drawPath(
                path = smilePath,
                color = Color(0xFF7C4A33).copy(alpha = 0.70f),
                style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
            )

            // Slow subtle diagonal light sheen across the portrait
            if (animateCalmly) {
                val sheenX = w * sheenProgress
                drawLine(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.26f), Color.Transparent),
                        start = Offset(sheenX - w * 0.24f, 0f),
                        end = Offset(sheenX + w * 0.24f, h)
                    ),
                    start = Offset(sheenX - w * 0.14f, 0f),
                    end = Offset(sheenX + w * 0.14f, h),
                    strokeWidth = w * 0.22f
                )
            }
        }
    }
}

/**
 * وسام التميّز الهادئ والراقي لقسم "نخبة الكادر التدريسي":
 * ميدالية دائرية زجاجية بإطار ذهبي مزدوج، محاطة بقوسي غار رفيعين،
 * وفي قلبها نجمة خماسية مصقولة بتدرج ذهبي دافئ مع لمعة ضوئية مائلة بطيئة وتنفس هادئ (0.97..1.03).
 */
@Composable
fun EliteTeacherExcellenceMedalCanvas(
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "elite_teacher_medal_anim")
    val breatheScale by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "medal_breathe_scale"
    )
    val sheenProgress by transition.animateFloat(
        initialValue = -0.5f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "medal_sheen"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val baseR = minOf(w, h) * 0.44f
        val medalR = baseR * 0.78f * breatheScale

        // 1) Soft warm golden ambient glow behind medal
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.24f),
                    Color(0xFF34D399).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = baseR * 1.12f
            ),
            radius = baseR * 1.12f,
            center = Offset(cx, cy)
        )

        // 2) Delicate Left & Right Curved Golden Laurel Wreath Arcs (قوسي غار رفيعين)
        val laurelR = baseR * 0.98f
        val laurelStroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        drawArc(
            brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
            startAngle = 128f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = Offset(cx - laurelR, cy - laurelR),
            size = Size(laurelR * 2f, laurelR * 2f),
            style = laurelStroke
        )
        drawArc(
            brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
            startAngle = -52f,
            sweepAngle = 104f,
            useCenter = false,
            topLeft = Offset(cx - laurelR, cy - laurelR),
            size = Size(laurelR * 2f, laurelR * 2f),
            style = laurelStroke
        )
        // Subtle tiny laurel leaves along both arcs
        val leafAnglesDeg = listOf(145f, 168f, 192f, 215f, -35f, -12f, 12f, 35f)
        leafAnglesDeg.forEach { deg ->
            val rad = Math.toRadians(deg.toDouble())
            val lx = cx + (laurelR * cos(rad)).toFloat()
            val ly = cy + (laurelR * sin(rad)).toFloat()
            drawCircle(
                color = Color(0xFFFDE68A).copy(alpha = 0.85f),
                radius = 1.8.dp.toPx(),
                center = Offset(lx, ly)
            )
        }

        // 3) Frosted-glass circular medallion body
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.26f),
                    Color(0xFF065F46).copy(alpha = 0.65f),
                    Color(0xFF022C22).copy(alpha = 0.80f)
                ),
                center = Offset(cx - medalR * 0.25f, cy - medalR * 0.25f),
                radius = medalR * 1.25f
            ),
            radius = medalR,
            center = Offset(cx, cy)
        )

        // 4) Double Golden Rim (إطار ذهبي مزدوج)
        drawCircle(
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFFDE68A)),
                start = Offset(cx - medalR, cy - medalR),
                end = Offset(cx + medalR, cy + medalR)
            ),
            radius = medalR,
            center = Offset(cx, cy),
            style = Stroke(width = 2.2.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.50f),
            radius = medalR * 0.83f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        // 5) Polished 5-Point Star in Warm Golden Gradient inside the center
        val starOuterR = medalR * 0.56f
        val starInnerR = starOuterR * 0.44f
        val starPath = Path().apply {
            for (i in 0 until 10) {
                val angleRad = Math.toRadians((-90.0 + i * 36.0))
                val curR = if (i % 2 == 0) starOuterR else starInnerR
                val px = cx + (curR * cos(angleRad)).toFloat()
                val py = cy + (curR * sin(angleRad)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(
            path = starPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFD97706)),
                start = Offset(cx - starOuterR, cy - starOuterR),
                end = Offset(cx + starOuterR, cy + starOuterR)
            )
        )

        // 6) Slow diagonal light sheen clipped neatly inside the medallion circle
        val clipCircle = Path().apply {
            addOval(Rect(cx - medalR, cy - medalR, cx + medalR, cy + medalR))
        }
        clipPath(clipCircle) {
            val sheenX = (cx - medalR) + (medalR * 2f) * sheenProgress
            drawLine(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.38f), Color.Transparent),
                    start = Offset(sheenX - medalR * 0.35f, cy - medalR),
                    end = Offset(sheenX + medalR * 0.35f, cy + medalR)
                ),
                start = Offset(sheenX - medalR * 0.20f, cy - medalR),
                end = Offset(sheenX + medalR * 0.20f, cy + medalR),
                strokeWidth = medalR * 0.38f
            )
        }
    }
}

private data class ActivitySpotlightScene(
    val title: String,
    val subtitle: String,
    val sceneIndex: Int
)

/**
 * الوحدة التفاعلية الزجاجية الديناميكية لبطاقة "مجتمع ونشاطات الطلبة":
 * تتبدل بسلاسة كل 2.6 ثانية عبر AnimatedContent بحركة انزلاق عمودي ناعم وتلاشي
 * بين 3 مشاهد تربوية متناغمة (انشر إبداعك • علّق وشجّع • تفاعل وتألّق) مع مؤشر نقاط متمدد.
 */
@Composable
fun DynamicActivitySpotlightCard(
    modifier: Modifier = Modifier
) {
    val scenes = remember {
        listOf(
            ActivitySpotlightScene(
                title = "انشر إبداعك",
                subtitle = "شارك موهبتك ومشروعك",
                sceneIndex = 0
            ),
            ActivitySpotlightScene(
                title = "علّق وشجّع",
                subtitle = "تبادل الآراء البنّاءة",
                sceneIndex = 1
            ),
            ActivitySpotlightScene(
                title = "تفاعل وتألّق",
                subtitle = "ادعم إبداعات زملائك",
                sceneIndex = 2
            )
        )
    }

    var activeSceneIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600L)
            activeSceneIndex = (activeSceneIndex + 1) % scenes.size
        }
    }

    val auraTransition = rememberInfiniteTransition(label = "activity_spotlight_aura")
    val auraPulse by auraTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "spotlight_aura_pulse"
    )

    Surface(
        modifier = modifier
            .width(146.dp)
            .height(114.dp)
            .testTag("dynamic_activity_spotlight_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Subtle emerald & warm-gold backdrop glow (strictly no pink/orange)
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = auraPulse),
                            Color(0xFF34D399).copy(alpha = auraPulse * 0.7f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.30f),
                        radius = size.width * 0.62f
                    ),
                    radius = size.width * 0.62f,
                    center = Offset(size.width * 0.5f, size.height * 0.30f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = scenes[activeSceneIndex],
                    transitionSpec = {
                        (fadeIn(tween(500, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 }) togetherWith
                            (fadeOut(tween(420, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { -it / 3 })
                    },
                    label = "activity_spotlight_scene_anim",
                    modifier = Modifier.weight(1f)
                ) { scene ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        SpotlightSceneVectorIcon(
                            sceneIndex = scene.sceneIndex,
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = scene.title,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = scene.subtitle,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1FAE5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3 Smoothly Expanding Phase Indicator Pills (انشر • علّق • تفاعل)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    scenes.indices.forEach { idx ->
                        val isSelected = idx == activeSceneIndex
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 18.dp else 6.dp,
                            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                            label = "spotlight_indicator_w_$idx"
                        )
                        Box(
                            modifier = Modifier
                                .height(4.dp)
                                .width(pillWidth)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) Color(0xFFFEF08A) else Color.White.copy(alpha = 0.36f)
                                )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpotlightSceneVectorIcon(
    sceneIndex: Int,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.dp.toPx()

        when (sceneIndex) {
            0 -> {
                // Scene 1: Creative pen nib & golden sparkle
                val penBody = Path().apply {
                    moveTo(w * 0.24f, h * 0.78f)
                    lineTo(w * 0.34f, h * 0.56f)
                    lineTo(w * 0.68f, h * 0.22f)
                    lineTo(w * 0.80f, h * 0.34f)
                    lineTo(w * 0.46f, h * 0.68f)
                    close()
                }
                drawPath(
                    path = penBody,
                    color = Color.White.copy(alpha = 0.18f)
                )
                drawPath(
                    path = penBody,
                    color = Color(0xFFFEF08A),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawLine(
                    color = Color(0xFF6EE7B7),
                    start = Offset(w * 0.22f, h * 0.84f),
                    end = Offset(w * 0.76f, h * 0.84f),
                    strokeWidth = strokeW * 0.85f,
                    cap = StrokeCap.Round
                )
                // Golden spark top-left
                val sx = w * 0.26f
                val sy = h * 0.24f
                val sr = w * 0.14f
                val spark = Path().apply {
                    moveTo(sx, sy - sr)
                    lineTo(sx + sr * 0.32f, sy - sr * 0.32f)
                    lineTo(sx + sr, sy)
                    lineTo(sx + sr * 0.32f, sy + sr * 0.32f)
                    lineTo(sx, sy + sr)
                    lineTo(sx - sr * 0.32f, sy + sr * 0.32f)
                    lineTo(sx - sr, sy)
                    lineTo(sx - sr * 0.32f, sy - sr * 0.32f)
                    close()
                }
                drawPath(path = spark, color = Color(0xFFFEF08A))
            }

            1 -> {
                // Scene 2: Two clean overlapping dialogue bubbles (emerald & gold borders)
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF6EE7B7),
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = strokeW * 0.9f)
                )
                drawRoundRect(
                    color = Color(0xFF064E3B).copy(alpha = 0.65f),
                    topLeft = Offset(w * 0.36f, h * 0.42f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFFEF08A),
                    topLeft = Offset(w * 0.36f, h * 0.42f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = strokeW * 0.95f)
                )
                // Clean dialogue lines inside foreground bubble
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(w * 0.48f, h * 0.58f),
                    end = Offset(w * 0.78f, h * 0.58f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFEF08A).copy(alpha = 0.85f),
                    start = Offset(w * 0.54f, h * 0.68f),
                    end = Offset(w * 0.78f, h * 0.68f),
                    strokeWidth = 1.6.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            else -> {
                // Scene 3: Excellence star + calm concentric emerald interaction wave
                val cx = w * 0.50f
                val cy = h * 0.50f
                drawCircle(
                    color = Color(0xFF6EE7B7).copy(alpha = 0.48f),
                    radius = w * 0.43f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawCircle(
                    color = Color(0xFFFEF08A).copy(alpha = 0.32f),
                    radius = w * 0.33f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.1.dp.toPx())
                )
                val starOuterR = w * 0.26f
                val starInnerR = starOuterR * 0.44f
                val starPath = Path().apply {
                    for (i in 0 until 10) {
                        val rad = Math.toRadians(-90.0 + i * 36.0)
                        val r = if (i % 2 == 0) starOuterR else starInnerR
                        val px = cx + (r * cos(rad)).toFloat()
                        val py = cy + (r * sin(rad)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(
                    path = starPath,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24)))
                )
            }
        }
    }
}
