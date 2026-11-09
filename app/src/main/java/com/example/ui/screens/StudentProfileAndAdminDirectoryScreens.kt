package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import androidx.compose.ui.unit.dp
import com.example.ui.theme.AdaptiveSoftBorder
import com.example.ui.theme.AdaptiveSoftSurface
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.RoyalGreen
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Animated Segmented Selector Card for Gender (ذكر / أنثى) and Age (العمر الزمني)
 * built exclusively with custom animated `Canvas` vector icons (`InfiniteTransition` & `animateFloatAsState`)
 * and explicit lambda types `(String) -> Unit` and `(Int) -> Unit`.
 */
@Composable
fun GenderAndAgeSegmentedSelectorCard(
    selectedGender: String,
    onGenderChange: (String) -> Unit,
    ageYears: Int,
    onAgeYearsChange: (Int) -> Unit,
    title: String = "تحديد الجنس والعمر الزمني"
) {
    val isFemaleSelected: Boolean = selectedGender.trim() == "أنثى" ||
        selectedGender.trim() == "انثى" ||
        selectedGender.equals("FEMALE", ignoreCase = true)

    val maleSelected: Boolean = !isFemaleSelected
    val effectiveDisplayedAge: Int = if (ageYears > 0) ageYears else 12

    val maleBgColor by animateColorAsState(
        targetValue = if (maleSelected) Color(0xFF047857) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(280),
        label = "male_seg_bg"
    )
    val femaleBgColor by animateColorAsState(
        targetValue = if (isFemaleSelected) Color(0xFFE11D48) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(280),
        label = "female_seg_bg"
    )
    val maleScale by animateFloatAsState(
        targetValue = if (maleSelected) 1.02f else 0.98f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "male_seg_scale"
    )
    val femaleScale by animateFloatAsState(
        targetValue = if (isFemaleSelected) 1.02f else 0.98f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "female_seg_scale"
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = AdaptiveSoftSurface.copy(alpha = 0.72f),
        border = BorderStroke(
            width = 1.2.dp,
            color = if (isFemaleSelected) Color(0xFFF43F5E).copy(alpha = 0.45f) else AdaptiveSoftBorder
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedGenderCrestCanvasIcon(
                        isFemale = isFemaleSelected,
                        tint = if (isFemaleSelected) Color(0xFFE11D48) else RoyalGreen,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeepForestGreen
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isFemaleSelected) {
                        Color(0xFFFFE4E6)
                    } else {
                        Color(0xFFECFDF5)
                    },
                    border = BorderStroke(
                        1.dp,
                        if (isFemaleSelected) Color(0xFFF43F5E).copy(alpha = 0.55f) else RoyalGreen.copy(alpha = 0.45f)
                    )
                ) {
                    Text(
                        text = if (isFemaleSelected) "ثيم أنثوي ملكي" else "ثيم زمردي ملكي",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isFemaleSelected) Color(0xFFBE123C) else Color(0xFF047857),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Segmented Selector for Gender (ذكر / أنثى) with Animated Canvas Vector Icons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .scale(maleScale)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onGenderChange("ذكر") },
                    shape = RoundedCornerShape(14.dp),
                    color = maleBgColor,
                    border = BorderStroke(
                        width = if (maleSelected) 1.5.dp else 1.dp,
                        color = if (maleSelected) GoldAccent else AdaptiveSoftBorder
                    ),
                    shadowElevation = if (maleSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AnimatedGenderOptionCanvasIcon(
                            isFemaleOption = false,
                            isSelected = maleSelected,
                            tint = if (maleSelected) Color.White else Color(0xFF047857),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "ذكر",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (maleSelected) Color.White else DeepForestGreen
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .scale(femaleScale)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onGenderChange("أنثى") },
                    shape = RoundedCornerShape(14.dp),
                    color = femaleBgColor,
                    border = BorderStroke(
                        width = if (isFemaleSelected) 1.5.dp else 1.dp,
                        color = if (isFemaleSelected) Color(0xFFFDA4AF) else AdaptiveSoftBorder
                    ),
                    shadowElevation = if (isFemaleSelected) 4.dp else 0.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        AnimatedGenderOptionCanvasIcon(
                            isFemaleOption = true,
                            isSelected = isFemaleSelected,
                            tint = if (isFemaleSelected) Color.White else Color(0xFFE11D48),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "أنثى",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFemaleSelected) Color.White else Color(0xFFBE185D)
                        )
                    }
                }
            }

            // Interactive Age Selector Row with Animated Canvas Plus/Minus & Age Dial
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedAgeDialCanvasIcon(
                        ageYears = effectiveDisplayedAge,
                        tint = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "العمر المعتمد: $effectiveDisplayedAge سنة",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        onClick = {
                            val current = if (ageYears <= 0) 12 else ageYears
                            val next: Int = (current - 1).coerceAtLeast(5)
                            onAgeYearsChange(next)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, AdaptiveSoftBorder),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                drawLine(
                                    color = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald,
                                    start = Offset(size.width * 0.2f, size.height / 2f),
                                    end = Offset(size.width * 0.8f, size.height / 2f),
                                    strokeWidth = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.2.dp,
                            if (isFemaleSelected) Color(0xFFF43F5E).copy(alpha = 0.55f) else AdaptiveSoftBorder
                        )
                    ) {
                        Text(
                            text = "$effectiveDisplayedAge سنة",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }

                    Surface(
                        onClick = {
                            val current = if (ageYears <= 0) 12 else ageYears
                            val next: Int = (current + 1).coerceAtMost(75)
                            onAgeYearsChange(next)
                        },
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, AdaptiveSoftBorder),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.size(14.dp)) {
                                val accent = if (isFemaleSelected) Color(0xFFE11D48) else DeepEmerald
                                drawLine(
                                    color = accent,
                                    start = Offset(size.width * 0.2f, size.height / 2f),
                                    end = Offset(size.width * 0.8f, size.height / 2f),
                                    strokeWidth = 2.2.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                drawLine(
                                    color = accent,
                                    start = Offset(size.width / 2f, size.height * 0.2f),
                                    end = Offset(size.width / 2f, size.height * 0.8f),
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
}

@Composable
fun AnimatedGenderCrestCanvasIcon(
    isFemale: Boolean,
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gender_crest_anim")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gender_crest_pulse"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        drawCircle(
            color = tint.copy(alpha = 0.16f),
            radius = (w * 0.44f) * pulse,
            center = Offset(cx, cy)
        )
        // Head circle
        drawCircle(
            color = tint,
            radius = w * 0.18f,
            center = Offset(cx, h * 0.34f),
            style = Stroke(width = 1.8.dp.toPx())
        )
        // Shoulders arc
        drawArc(
            color = tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.54f),
            size = Size(w * 0.56f, h * 0.38f),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
        if (isFemale) {
            // Subtle rose crown dot above head
            drawCircle(
                color = Color(0xFFF43F5E),
                radius = 2.dp.toPx() * pulse,
                center = Offset(cx, h * 0.11f)
            )
        }
    }
}

@Composable
fun AnimatedGenderOptionCanvasIcon(
    isFemaleOption: Boolean,
    isSelected: Boolean,
    tint: Color,
    modifier: Modifier = Modifier.size(20.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "gender_opt_canvas_$isFemaleOption")
    val haloScale by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "gender_opt_halo"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val strokeW = 1.9.dp.toPx()

        if (isSelected) {
            drawCircle(
                color = tint.copy(alpha = 0.22f),
                radius = (w * 0.44f) * haloScale,
                center = Offset(cx, cy)
            )
        }

        if (isFemaleOption) {
            // Elegant feminine silhouette crest + crown petal
            drawCircle(
                color = tint,
                radius = w * 0.20f,
                center = Offset(cx, h * 0.34f),
                style = Stroke(width = strokeW)
            )
            val dressPath = Path().apply {
                moveTo(cx - w * 0.16f, h * 0.56f)
                lineTo(cx + w * 0.16f, h * 0.56f)
                lineTo(cx + w * 0.28f, h * 0.84f)
                lineTo(cx - w * 0.28f, h * 0.84f)
                close()
            }
            drawPath(
                path = dressPath,
                color = tint.copy(alpha = 0.24f)
            )
            drawPath(
                path = dressPath,
                color = tint,
                style = Stroke(width = strokeW, join = StrokeJoin.Round)
            )
        } else {
            // Noble masculine academic shield + shoulders
            drawCircle(
                color = tint,
                radius = w * 0.20f,
                center = Offset(cx, h * 0.34f),
                style = Stroke(width = strokeW)
            )
            val shoulders = Path().apply {
                moveTo(cx - w * 0.30f, h * 0.84f)
                cubicTo(
                    cx - w * 0.28f, h * 0.56f,
                    cx + w * 0.28f, h * 0.56f,
                    cx + w * 0.30f, h * 0.84f
                )
            }
            drawPath(
                path = shoulders,
                color = tint,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun AnimatedAgeDialCanvasIcon(
    ageYears: Int,
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "age_dial_canvas")
    val sweepGlow by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "age_dial_glow"
    )
    val normalizedSweep: Float = ((ageYears.coerceIn(5, 75) - 5) / 70f * 260f + 45f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = 2.dp.toPx()
        drawCircle(
            color = tint.copy(alpha = 0.14f),
            radius = (w * 0.42f) * sweepGlow,
            center = Offset(w / 2f, h / 2f)
        )
        drawArc(
            color = tint.copy(alpha = 0.28f),
            startAngle = 135f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawArc(
            color = tint,
            startAngle = 135f,
            sweepAngle = normalizedSweep.coerceAtMost(270f),
            useCenter = false,
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        drawCircle(
            color = tint,
            radius = 2.2.dp.toPx(),
            center = Offset(w / 2f, h / 2f)
        )
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
    modifier: Modifier = Modifier.size(26.dp)
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
                // Academic Open Book + Knowledge Star
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
