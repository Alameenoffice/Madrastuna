package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AdaptiveSoftBorder
import com.example.ui.theme.AdaptiveSoftSurface
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.RoyalGreen
import kotlin.math.PI
import kotlin.math.sin

fun formatArabicAgeYearsLabel(ageYears: Int): String {
    val cleanAge = ageYears.coerceAtLeast(1)
    return if (cleanAge in 3..10) {
        "$cleanAge سنوات"
    } else {
        "$cleanAge سنة"
    }
}

/**
 * Compact, modern Segmented Selector for Gender (ذكر / أنثى) and Age (العمر):
 * - Gender: 44.dp Segmented Toggle Pill with two clean side-by-side options (ذكر | أنثى).
 * - Age: Single balanced horizontal row with (-) button on the left, "X سنوات / سنة" in the center,
 *   and (+) button on the right, without duplicate labels or clipped capsules.
 */
@Composable
fun GenderAndAgeSegmentedSelectorCard(
    selectedGender: String,
    onGenderChange: (String) -> Unit,
    ageYears: Int,
    onAgeYearsChange: (Int) -> Unit,
    title: String = "الجنس والعمر"
) {
    val isFemaleSelected: Boolean = selectedGender.trim() == "أنثى" ||
        selectedGender.trim() == "انثى" ||
        selectedGender.equals("FEMALE", ignoreCase = true)

    val maleSelected: Boolean = !isFemaleSelected
    val effectiveDisplayedAge: Int = if (ageYears > 0) ageYears else 9

    val maleBgColor by animateColorAsState(
        targetValue = if (maleSelected) RoyalGreen else Color.Transparent,
        animationSpec = tween(220),
        label = "male_seg_bg"
    )
    val femaleBgColor by animateColorAsState(
        targetValue = if (isFemaleSelected) Color(0xFFE11D48) else Color.Transparent,
        animationSpec = tween(220),
        label = "female_seg_bg"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, AdaptiveSoftBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepForestGreen,
                    maxLines = 1
                )
            }

            // 1. الجنس: شريط تبديل ثنائي عصري ومضغوط بارتفاع 44.dp فقط
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdaptiveSoftSurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onGenderChange("ذكر") },
                    shape = RoundedCornerShape(9.dp),
                    color = maleBgColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ذكر",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (maleSelected) Color.White else DeepForestGreen,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(9.dp))
                        .clickable { onGenderChange("أنثى") },
                    shape = RoundedCornerShape(9.dp),
                    color = femaleBgColor
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "أنثى",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFemaleSelected) Color.White else Color(0xFFBE185D),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            // 2. العمر: عداد رقمي أفقي أنيق وموزون في سطر واحد (زر + في اليمين، العمر في الوسط، زر - في اليسار)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdaptiveSoftSurface.copy(alpha = 0.65f))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // في اليمين (بداية السطر في RTL): زر زائد دائري (+)
                Surface(
                    onClick = {
                        val current = if (ageYears <= 0) 9 else ageYears
                        val next: Int = (current + 1).coerceAtMost(75)
                        onAgeYearsChange(next)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, AdaptiveSoftBorder),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val accent = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald
                        Canvas(modifier = Modifier.size(13.dp)) {
                            drawLine(
                                color = accent,
                                start = Offset(size.width * 0.18f, size.height / 2f),
                                end = Offset(size.width * 0.82f, size.height / 2f),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                            drawLine(
                                color = accent,
                                start = Offset(size.width / 2f, size.height * 0.18f),
                                end = Offset(size.width / 2f, size.height * 0.82f),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // في الوسط: الرقم مع كلمة سنة/سنوات بخط واضح بدون تكرار
                Text(
                    text = formatArabicAgeYearsLabel(effectiveDisplayedAge),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isFemaleSelected) Color(0xFFBE185D) else DeepForestGreen,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )

                // في اليسار (نهاية السطر في RTL): زر ناقص دائري (-)
                Surface(
                    onClick = {
                        val current = if (ageYears <= 0) 9 else ageYears
                        val next: Int = (current - 1).coerceAtLeast(5)
                        onAgeYearsChange(next)
                    },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, AdaptiveSoftBorder),
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        val accent = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald
                        Canvas(modifier = Modifier.size(13.dp)) {
                            drawLine(
                                color = accent,
                                start = Offset(size.width * 0.18f, size.height / 2f),
                                end = Offset(size.width * 0.82f, size.height / 2f),
                                strokeWidth = 2.2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable Animated Canvas Vector Icon for Teacher Specialties in Teaching Staff Hub
 * (`AppSection.TEACHING_STAFF`).
 */
@Composable
fun AnimatedSpecialtyCanvasIcon(
    specialty: String,
    accentColor: Color,
    modifier: Modifier = Modifier.size(24.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "specialty_canvas_anim")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "specialty_phase"
    )
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "specialty_pulse"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.9.dp.toPx()

        drawCircle(
            color = accentColor.copy(alpha = 0.14f),
            radius = (w * 0.44f) * pulse,
            center = Offset(cx, cy)
        )

        when {
            specialty.contains("رياضيات") || specialty.contains("فيزياء") -> {
                rotate(degrees = sin(phase) * 12f, pivot = Offset(cx, cy)) {
                    val tri = Path().apply {
                        moveTo(cx, h * 0.18f)
                        lineTo(w * 0.82f, h * 0.78f)
                        lineTo(w * 0.18f, h * 0.78f)
                        close()
                    }
                    drawPath(path = tri, color = accentColor.copy(alpha = 0.22f))
                    drawPath(path = tri, color = accentColor, style = Stroke(width = strokeW, join = StrokeJoin.Round))
                }
            }
            specialty.contains("علوم") || specialty.contains("أحياء") || specialty.contains("كيمياء") -> {
                drawCircle(color = accentColor, radius = 2.6.dp.toPx() * pulse, center = Offset(cx, cy))
                rotate(degrees = 32f + sin(phase) * 10f, pivot = Offset(cx, cy)) {
                    drawOval(
                        color = accentColor,
                        topLeft = Offset(w * 0.14f, h * 0.34f),
                        size = Size(w * 0.72f, h * 0.32f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
                rotate(degrees = -32f - sin(phase) * 10f, pivot = Offset(cx, cy)) {
                    drawOval(
                        color = accentColor,
                        topLeft = Offset(w * 0.14f, h * 0.34f),
                        size = Size(w * 0.72f, h * 0.32f),
                        style = Stroke(width = 1.6.dp.toPx())
                    )
                }
            }
            specialty.contains("اسلام") || specialty.contains("إسلام") || specialty.contains("قرآن") -> {
                rotate(degrees = sin(phase) * 8f, pivot = Offset(cx, cy)) {
                    val s = w * 0.44f
                    drawRoundRect(
                        color = accentColor,
                        topLeft = Offset(cx - s / 2f, cy - s / 2f),
                        size = Size(s, s),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                        style = Stroke(width = strokeW)
                    )
                    rotate(degrees = 45f, pivot = Offset(cx, cy)) {
                        drawRoundRect(
                            color = accentColor,
                            topLeft = Offset(cx - s / 2f, cy - s / 2f),
                            size = Size(s, s),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                            style = Stroke(width = strokeW)
                        )
                    }
                }
            }
            else -> {
                val flutter = sin(phase) * (h * 0.03f)
                val book = Path().apply {
                    moveTo(cx, h * 0.76f)
                    lineTo(w * 0.18f, h * 0.68f + flutter)
                    lineTo(w * 0.18f, h * 0.26f + flutter)
                    lineTo(cx, h * 0.34f)
                    lineTo(w * 0.82f, h * 0.26f - flutter)
                    lineTo(w * 0.82f, h * 0.68f - flutter)
                    close()
                }
                drawPath(path = book, color = accentColor.copy(alpha = 0.20f))
                drawPath(path = book, color = accentColor, style = Stroke(width = strokeW, join = StrokeJoin.Round))
                drawLine(
                    color = accentColor,
                    start = Offset(cx, h * 0.34f),
                    end = Offset(cx, h * 0.76f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}
