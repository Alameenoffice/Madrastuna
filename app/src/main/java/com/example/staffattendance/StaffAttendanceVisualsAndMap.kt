package com.example.staffattendance

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SatelliteAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Streetview
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ui.components.DailyStatusBadgeSpec
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldAccent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// =============================================================================
// ألوان الحالة الدلالية الموحّدة لمنظومة البصمة والإجازات
// =============================================================================
object StaffAttendancePalette {
    val PresentEmerald = Color(0xFF059669)
    val AbsentCoral = Color(0xFFDC2626)
    val LeaveAmber = Color(0xFFD97706)
    val LateOrange = Color(0xFFEA580C)
    val SoftGold = Color(0xFFD97706)
    val SoftGoldLight = Color(0xFFFDE68A)
}

fun staffStatusArabicLabel(status: String): String = when (status.lowercase(Locale.US)) {
    "present" -> "حاضر"
    "late" -> "متأخر"
    "leave" -> "مجاز"
    "absent" -> "غائب"
    else -> "غير مسجّل"
}

fun staffStatusColor(status: String): Color = when (status.lowercase(Locale.US)) {
    "present" -> StaffAttendancePalette.PresentEmerald
    "late" -> StaffAttendancePalette.LateOrange
    "leave" -> StaffAttendancePalette.LeaveAmber
    "absent" -> StaffAttendancePalette.AbsentCoral
    else -> Color(0xFF64748B)
}

fun leaveStatusArabicLabel(status: String): String = when (status.lowercase(Locale.US)) {
    "approved" -> "موافق عليها"
    "rejected" -> "مرفوضة"
    else -> "قيد المراجعة"
}

fun leaveStatusColor(status: String): Color = when (status.lowercase(Locale.US)) {
    "approved" -> StaffAttendancePalette.PresentEmerald
    "rejected" -> StaffAttendancePalette.AbsentCoral
    else -> StaffAttendancePalette.LeaveAmber
}

fun recordMethodArabicLabel(method: String, isManualOverride: Boolean): String {
    return if (isManualOverride || method == "manual_admin") {
        "تعديل إداري"
    } else {
        "بصمة إلكترونية"
    }
}

/**
 * يبني شارة الحالة اليومية الحية على صورة المستخدم (SmartWelcomeProfileAvatar) في الشاشة الرئيسية
 * للكادر التدريسي والإداري بحيث تتحدث تلقائياً فور تسجيل البصمة أو تعديل الإدارة.
 */
fun buildStaffDailyStatusBadgeSpec(
    record: StaffAttendanceRecord?,
    isHolidayToday: Boolean
): DailyStatusBadgeSpec {
    if (isHolidayToday) {
        return DailyStatusBadgeSpec(
            label = "عطلة رسمية",
            ringColors = listOf(Color(0xFF7DD3FC), Color(0xFF38BDF8), Color(0xFFBAE6FD)),
            pillBackground = Color(0xFF0C4A6E),
            pillBorder = Color(0xFF7DD3FC),
            pillTextColor = Color(0xFFE0F2FE),
            dotColor = Color(0xFF38BDF8),
            pulseDot = false
        )
    }

    if (record == null) {
        return DailyStatusBadgeSpec(
            label = "بانتظار البصمة",
            ringColors = listOf(Color(0xFF34D399), Color(0xFFFDE68A), Color(0xFF10B981)),
            pillBackground = Color(0xFF064E3B),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color(0xFFECFDF5),
            dotColor = Color(0xFFFDE68A),
            pulseDot = true
        )
    }

    if (record.hasCheckedOut) {
        return DailyStatusBadgeSpec(
            label = "انصرف • ${record.checkOutTime}",
            ringColors = listOf(Color(0xFF6EE7B7), Color(0xFF059669), Color(0xFF34D399)),
            pillBackground = Color(0xFF065F46),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color.White,
            dotColor = Color(0xFF34D399),
            pulseDot = false
        )
    }

    return when (record.status.lowercase(Locale.US)) {
        "present" -> DailyStatusBadgeSpec(
            label = if (record.checkInTime.isNotBlank()) "حاضر • ${record.checkInTime}" else "حاضر اليوم",
            ringColors = listOf(Color(0xFF6EE7B7), Color(0xFF059669), Color(0xFF34D399)),
            pillBackground = Color(0xFF065F46),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color.White,
            dotColor = Color(0xFF34D399),
            pulseDot = true
        )
        "late" -> DailyStatusBadgeSpec(
            label = if (record.checkInTime.isNotBlank()) "متأخر • ${record.checkInTime}" else "متأخر اليوم",
            ringColors = listOf(Color(0xFFFDBA74), Color(0xFFEA580C), Color(0xFFFED7AA)),
            pillBackground = Color(0xFF7C2D12),
            pillBorder = Color(0xFFFDBA74),
            pillTextColor = Color(0xFFFFEDD5),
            dotColor = Color(0xFFFB923C),
            pulseDot = true
        )
        "leave" -> DailyStatusBadgeSpec(
            label = "إجازة معتمدة",
            ringColors = listOf(Color(0xFFFDE68A), Color(0xFFD97706), Color(0xFFFCD34D)),
            pillBackground = Color(0xFF78350F),
            pillBorder = Color(0xFFFDE68A),
            pillTextColor = Color(0xFFFEF3C7),
            dotColor = Color(0xFFFBBF24),
            pulseDot = false
        )
        "absent" -> DailyStatusBadgeSpec(
            label = "غائب اليوم",
            ringColors = listOf(Color(0xFFFCA5A5), Color(0xFFDC2626), Color(0xFFFECACA)),
            pillBackground = Color(0xFF7F1D1D),
            pillBorder = Color(0xFFFCA5A5),
            pillTextColor = Color(0xFFFEE2E2),
            dotColor = Color(0xFFF87171),
            pulseDot = false
        )
        else -> DailyStatusBadgeSpec(
            label = "متصل الآن",
            ringColors = listOf(Color(0xFF34D399), Color(0xFFFDE68A), Color(0xFF10B981)),
            pillBackground = Color(0xFF064E3B),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color(0xFFECFDF5),
            dotColor = Color(0xFF34D399),
            pulseDot = true
        )
    }
}

// =============================================================================
// 1. بطاقة النطاق الجغرافي الحية في الترويسة الزمردية لشاشة المعلم (Canvas)
// =============================================================================
@Composable
fun StaffZoneLiveIndicatorHeaderWidget(
    probe: ZoneProbeResult,
    onRefreshClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "zone_live_pulse")
    val pulseProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "zone_ring_expand"
    )

    val accentColor by animateColorAsState(
        targetValue = when {
            probe.isHoliday -> Color(0xFF7DD3FC)
            probe.isMockLocationDetected -> Color(0xFFFCA5A5)
            probe.insideZone -> Color(0xFF34D399)
            else -> Color(0xFFFDE68A)
        },
        animationSpec = tween(350),
        label = "zone_accent_color"
    )

    Surface(
        modifier = modifier.width(126.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension * 0.48f

                    // حلقة نبض جغرافي متوسعة
                    drawCircle(
                        color = accentColor.copy(alpha = (1f - pulseProgress) * 0.45f),
                        radius = maxR * (0.42f + 0.56f * pulseProgress),
                        center = c,
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // حلقة النطاق الثابتة
                    drawCircle(
                        color = accentColor.copy(alpha = 0.32f),
                        radius = maxR * 0.72f,
                        center = c,
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    // قلب الدائرة الزجاجية
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accentColor.copy(alpha = 0.92f),
                                accentColor.copy(alpha = 0.42f)
                            ),
                            center = c,
                            radius = maxR * 0.44f
                        ),
                        radius = maxR * 0.38f,
                        center = c
                    )
                    // رمز دبوس الموقع/البصمة المرسوم بـ Canvas
                    drawCircle(
                        color = Color.White,
                        radius = maxR * 0.13f,
                        center = c
                    )
                }

                if (probe.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(44.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                }
            }

            Text(
                text = when {
                    probe.isLoading -> "فحص الموقع..."
                    probe.isHoliday -> "عطلة رسمية"
                    probe.isMockLocationDetected -> "موقع وهمي"
                    probe.insideZone -> "داخل النطاق"
                    else -> "خارج النطاق"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )

            Text(
                text = when {
                    probe.matchedByWifi -> "شبكة المدرسة"
                    probe.distanceMeters != null -> "${probe.distanceMeters} م / ${probe.allowedRadiusMeters} م"
                    else -> "النطاق ${probe.allowedRadiusMeters} م"
                },
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = !probe.isLoading, onClick = onRefreshClick)
                    .testTag("staff_zone_refresh_btn"),
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.18f),
                border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = "تحديث الموقع",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "تحديث",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// =============================================================================
// 2. زر البصمة الإلكترونية الذكية الدائري التفاعلي (Compose Canvas + Scan Ring)
// =============================================================================
enum class FingerprintVisualStage {
    READY_CHECK_IN,
    READY_CHECK_OUT,
    COMPLETED_DAY,
    BLOCKED_OUTSIDE_OR_HOLIDAY,
    SCANNING
}

@Composable
fun ElectronicFingerprintScanButton(
    stage: FingerprintVisualStage,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fingerprint_scan_ring")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (stage == FingerprintVisualStage.SCANNING) 1100 else 3600,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scan_sweep"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val scanLineY by infiniteTransition.animateFloat(
        initialValue = 0.22f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1250, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scan_beam_y"
    )

    val ringPrimary by animateColorAsState(
        targetValue = when (stage) {
            FingerprintVisualStage.READY_CHECK_IN -> StaffAttendancePalette.PresentEmerald
            FingerprintVisualStage.READY_CHECK_OUT -> Color(0xFF0F766E)
            FingerprintVisualStage.COMPLETED_DAY -> StaffAttendancePalette.PresentEmerald
            FingerprintVisualStage.SCANNING -> EmeraldAccent
            FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY -> Color(0xFF94A3B8)
        },
        animationSpec = tween(350),
        label = "fp_ring_primary"
    )

    val ringSecondary by animateColorAsState(
        targetValue = when (stage) {
            FingerprintVisualStage.READY_CHECK_IN -> Color(0xFFF59E0B)
            FingerprintVisualStage.READY_CHECK_OUT -> Color(0xFF34D399)
            FingerprintVisualStage.COMPLETED_DAY -> Color(0xFF6EE7B7)
            FingerprintVisualStage.SCANNING -> Color(0xFFFDE68A)
            FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY -> Color(0xFFCBD5E1)
        },
        animationSpec = tween(350),
        label = "fp_ring_secondary"
    )

    val buttonScale by animateFloatAsState(
        targetValue = if (stage == FingerprintVisualStage.SCANNING) 1.03f else 1f,
        animationSpec = tween(250),
        label = "fp_btn_scale"
    )

    Box(
        modifier = modifier
            .size(144.dp)
            .scale(buttonScale)
            .clip(CircleShape)
            .clickable(enabled = enabled && stage != FingerprintVisualStage.SCANNING, onClick = onClick)
            .testTag("electronic_fingerprint_action_btn"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val c = Offset(w / 2f, h / 2f)
            val outerR = (minOf(w, h) / 2f) - 4.dp.toPx()

            // هالة خارجية ناعمة
            val haloFactor = if (stage == FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY) 0.98f else pulseScale
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ringPrimary.copy(alpha = 0.22f),
                        ringPrimary.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = outerR * haloFactor
                ),
                radius = outerR * haloFactor,
                center = c
            )

            // مسار حلقة المسح الخلفي
            drawCircle(
                color = ringPrimary.copy(alpha = 0.16f),
                radius = outerR * 0.88f,
                center = c,
                style = Stroke(width = 5.dp.toPx())
            )

            // حلقة المسح الدوّارة (Scan Ring)
            if (stage != FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY) {
                val arcDiameter = outerR * 1.76f
                val topLeft = Offset(c.x - arcDiameter / 2f, c.y - arcDiameter / 2f)
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            ringSecondary.copy(alpha = 0.7f),
                            ringPrimary
                        ),
                        center = c
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = if (stage == FingerprintVisualStage.SCANNING) 240f else 130f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcDiameter, arcDiameter),
                    style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            // الدائرة الداخلية المتدرجة للزر
            val innerR = outerR * 0.76f
            drawCircle(
                brush = Brush.linearGradient(
                    colors = when (stage) {
                        FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY -> listOf(
                            Color(0xFF64748B),
                            Color(0xFF475569)
                        )
                        FingerprintVisualStage.READY_CHECK_OUT -> listOf(
                            Color(0xFF0F766E),
                            DeepEmerald
                        )
                        else -> listOf(
                            StaffAttendancePalette.PresentEmerald,
                            DeepEmerald
                        )
                    },
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                radius = innerR,
                center = c
            )

            // حلقة داخلية ذهبية رفيعة
            drawCircle(
                color = ringSecondary.copy(alpha = 0.45f),
                radius = innerR - 3.dp.toPx(),
                center = c,
                style = Stroke(width = 1.2.dp.toPx())
            )

            // رسم خطوط البصمة الإلكترونية بـ Compose Canvas (بدون أيقونات افتراضية)
            val ridgeColor = Color.White.copy(alpha = if (stage == FingerprintVisualStage.BLOCKED_OUTSIDE_OR_HOLIDAY) 0.65f else 0.95f)
            val ridgeStroke = 2.6.dp.toPx()

            if (stage == FingerprintVisualStage.COMPLETED_DAY) {
                // علامة اكتمال أنيقة داخل حلقة البصمة
                val checkStart = Offset(c.x - innerR * 0.34f, c.y + innerR * 0.02f)
                val checkMid = Offset(c.x - innerR * 0.08f, c.y + innerR * 0.28f)
                val checkEnd = Offset(c.x + innerR * 0.36f, c.y - innerR * 0.24f)
                drawLine(
                    color = Color.White,
                    start = checkStart,
                    end = checkMid,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White,
                    start = checkMid,
                    end = checkEnd,
                    strokeWidth = 5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            } else {
                // 5 أقواس بصمة متحدة المركز تحاكي البصمة الحيوية
                val radii = listOf(0.54f, 0.42f, 0.30f, 0.19f)
                radii.forEachIndexed { idx, factor ->
                    val r = innerR * factor
                    val ovalW = r * 1.68f
                    val ovalH = r * 2.08f
                    val tl = Offset(c.x - ovalW / 2f, c.y - ovalH / 2f)
                    val startA = when (idx) {
                        0 -> 205f
                        1 -> 185f
                        2 -> 165f
                        else -> 210f
                    }
                    val sweepA = when (idx) {
                        0 -> 130f
                        1 -> 215f
                        2 -> 255f
                        else -> 290f
                    }
                    drawArc(
                        color = ridgeColor,
                        startAngle = startA,
                        sweepAngle = sweepA,
                        useCenter = false,
                        topLeft = tl,
                        size = Size(ovalW, ovalH),
                        style = Stroke(width = ridgeStroke, cap = StrokeCap.Round)
                    )
                    if (idx < 2) {
                        drawArc(
                            color = ridgeColor.copy(alpha = 0.8f),
                            startAngle = 25f + idx * 18f,
                            sweepAngle = 105f - idx * 14f,
                            useCenter = false,
                            topLeft = tl,
                            size = Size(ovalW, ovalH),
                            style = Stroke(width = ridgeStroke, cap = StrokeCap.Round)
                        )
                    }
                }
                // خط لبّ البصمة المركزي
                drawLine(
                    color = ridgeColor,
                    start = Offset(c.x, c.y - innerR * 0.10f),
                    end = Offset(c.x, c.y + innerR * 0.22f),
                    strokeWidth = ridgeStroke,
                    cap = StrokeCap.Round
                )

                // شعاع المسح المتحرك أثناء الفحص
                if (stage == FingerprintVisualStage.SCANNING) {
                    val beamY = (c.y - innerR) + (innerR * 2f * scanLineY)
                    drawLine(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFFFDE68A),
                                Color.White,
                                Color(0xFFFDE68A),
                                Color.Transparent
                            )
                        ),
                        start = Offset(c.x - innerR * 0.72f, beamY),
                        end = Offset(c.x + innerR * 0.72f, beamY),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

// =============================================================================
// 3. الوحدة الزجاجية التفاعلية في الترويسة الزمردية لشاشة المدير (Crossfade)
// =============================================================================
@Composable
fun AdminStaffFingerprintHeaderWidget(
    modifier: Modifier = Modifier
) {
    val phrases = remember {
        listOf(
            "بصمة إلكترونية موثّقة",
            "تحقق فعلي بالموقع",
            "إشراف وسجل دقيق"
        )
    }
    var phraseIndex by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2800L)
            phraseIndex = (phraseIndex + 1) % phrases.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "admin_geo_fp")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "admin_geo_ring"
    )

    Surface(
        modifier = modifier.width(136.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier.size(56.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    val maxR = size.minDimension * 0.48f

                    // حلقة النطاق الجغرافي النابضة بهدوء
                    drawCircle(
                        color = Color(0xFFFDE68A).copy(alpha = (1f - pulse) * 0.55f),
                        radius = maxR * (0.45f + 0.54f * pulse),
                        center = c,
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFF6EE7B7).copy(alpha = 0.40f),
                        radius = maxR * 0.78f,
                        center = c,
                        style = Stroke(width = 1.3.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.16f),
                        radius = maxR * 0.62f,
                        center = c
                    )

                    // بصمة إلكترونية مرسومة بـ Canvas في المركز
                    val fpR = maxR * 0.46f
                    val strokeW = 1.8.dp.toPx()
                    listOf(0.95f, 0.68f, 0.42f).forEachIndexed { i, f ->
                        val r = fpR * f
                        val ow = r * 1.65f
                        val oh = r * 2.0f
                        drawArc(
                            color = Color.White,
                            startAngle = 195f - i * 12f,
                            sweepAngle = 210f + i * 24f,
                            useCenter = false,
                            topLeft = Offset(c.x - ow / 2f, c.y - oh / 2f),
                            size = Size(ow, oh),
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                    drawLine(
                        color = Color(0xFFFDE68A),
                        start = Offset(c.x, c.y - fpR * 0.16f),
                        end = Offset(c.x, c.y + fpR * 0.32f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            Crossfade(
                targetState = phrases[phraseIndex],
                animationSpec = tween(420),
                label = "admin_fp_phrase_crossfade"
            ) { phrase ->
                Text(
                    text = phrase,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// =============================================================================
// 4. خريطة تحديد النطاق الجغرافي التفاعلية (Leaflet.js داخل WebView بدون API Key)
// =============================================================================
private data class NominatimPlaceResult(
    val displayName: String,
    val lat: Double,
    val lon: Double
)

@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun SchoolGeofenceLeafletMap(
    latitude: Double,
    longitude: Double,
    radiusMeters: Int,
    onCoordinatesChanged: (lat: Double, lon: Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var isSatelliteMode by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<NominatimPlaceResult>>(emptyList()) }
    var searchError by remember { mutableStateOf<String?>(null) }
    var mapReady by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    // مزامنة الإحداثيات ونصف القطر والطبقة مع Leaflet عند تغيّرها من Kotlin (السلايدر أو زر موقعي الحالي أو اللصق)
    LaunchedEffect(latitude, longitude, radiusMeters, isSatelliteMode, mapReady) {
        if (mapReady) {
            val js = String.format(
                Locale.US,
                "if(window.updateSchoolZone){window.updateSchoolZone(%.6f, %.6f, %d, %s);}",
                latitude,
                longitude,
                radiusMeters.coerceIn(50, 300),
                if (isSatelliteMode) "true" else "false"
            )
            webViewRef?.evaluateJavascript(js, null)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // شريط البحث عن اسم المنطقة أو المدينة (Nominatim المجاني)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    searchError = null
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("geofence_map_search_input"),
                placeholder = {
                    Text(
                        text = "ابحث عن المدينة أو الحي (مثل: الموصل، حي الزهور...)",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DeepEmerald,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                )
            )
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(enabled = !isSearching && searchQuery.isNotBlank()) {
                        scope.launch {
                            isSearching = true
                            searchError = null
                            val found = searchNominatimPlaces(searchQuery)
                            isSearching = false
                            if (found.isEmpty()) {
                                searchError = "لم يتم العثور على نتائج لهذا الموقع"
                            }
                            searchResults = found
                        }
                    }
                    .testTag("geofence_map_search_btn"),
                shape = RoundedCornerShape(12.dp),
                color = DeepEmerald
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSearching) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = "بحث في الخريطة",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        if (searchError != null) {
            Text(
                text = searchError.orEmpty(),
                style = MaterialTheme.typography.labelSmall,
                color = StaffAttendancePalette.AbsentCoral
            )
        }

        if (searchResults.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, DeepEmerald.copy(alpha = 0.25f))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    searchResults.forEach { place ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onCoordinatesChanged(place.lat, place.lon)
                                    searchResults = emptyList()
                                    searchQuery = place.displayName.substringBefore(",")
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MyLocation,
                                contentDescription = "اختيار الموقع",
                                tint = DeepEmerald,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = place.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // زرّان للتبديل السريع بين "قمر صناعي" و"خريطة شوارع"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isSatelliteMode = true }
                    .testTag("map_mode_satellite_btn"),
                shape = RoundedCornerShape(12.dp),
                color = if (isSatelliteMode) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(
                    1.dp,
                    if (isSatelliteMode) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.SatelliteAlt,
                        contentDescription = "قمر صناعي",
                        tint = if (isSatelliteMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "قمر صناعي",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSatelliteMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { isSatelliteMode = false }
                    .testTag("map_mode_streets_btn"),
                shape = RoundedCornerShape(12.dp),
                color = if (!isSatelliteMode) DeepEmerald else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(
                    1.dp,
                    if (!isSatelliteMode) DeepEmerald else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.Streetview,
                        contentDescription = "خريطة شوارع",
                        tint = if (!isSatelliteMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "خريطة شوارع",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (!isSatelliteMode) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // حاوية الخريطة التفاعلية بارتفاع 260.dp وحواف 16.dp مع حماية اللمس من التمرير الأب
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(16.dp))
                .border(1.2.dp, DeepEmerald.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        webViewRef = this
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = WebSettings.LOAD_DEFAULT
                        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        webChromeClient = WebChromeClient()
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                mapReady = true
                            }
                        }

                        // منع القائمة الأم (LazyColumn/Scrollable) من خطف سحب الخريطة
                        setOnTouchListener { v, event ->
                            when (event.actionMasked) {
                                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                }
                                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            }
                            false
                        }

                        addJavascriptInterface(
                            object {
                                @JavascriptInterface
                                fun onMapCoordsPicked(lat: Double, lng: Double) {
                                    post {
                                        onCoordinatesChanged(lat, lng)
                                    }
                                }
                            },
                            "AndroidMapBridge"
                        )

                        val html = buildLeafletGeofenceHtml(latitude, longitude, radiusMeters, isSatelliteMode)
                        loadDataWithBaseURL("https://leafletjs.com/", html, "text/html", "UTF-8", null)
                    }
                }
            )
        }

        Text(
            text = "انقر على أي نقطة في الخريطة أو اسحب الدبوس الزمردي لتثبيت مركز المدرسة بدقة.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private suspend fun searchNominatimPlaces(query: String): List<NominatimPlaceResult> =
    withContext(Dispatchers.IO) {
        var conn: HttpURLConnection? = null
        try {
            val encoded = URLEncoder.encode(query.trim(), "UTF-8")
            val urlStr = "https://nominatim.openstreetmap.org/search?format=json&limit=5&accept-language=ar&q=$encoded"
            conn = (URL(urlStr).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 7000
                readTimeout = 8000
                setRequestProperty("User-Agent", "MadrasatiSmartAndroidApp/1.0")
                setRequestProperty("Accept", "application/json")
            }
            if (conn.responseCode !in 200..299) return@withContext emptyList()
            val body = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8)).use { it.readText() }
            val arr = JSONArray(body)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val lat = obj.optString("lat").toDoubleOrNull() ?: continue
                    val lon = obj.optString("lon").toDoubleOrNull() ?: continue
                    val name = obj.optString("display_name", "").ifBlank { "$lat, $lon" }
                    add(NominatimPlaceResult(name, lat, lon))
                }
            }
        } catch (_: Exception) {
            emptyList()
        } finally {
            conn?.disconnect()
        }
    }

private fun buildLeafletGeofenceHtml(
    initLat: Double,
    initLng: Double,
    initRadius: Int,
    initSatellite: Boolean
): String {
    val latStr = String.format(Locale.US, "%.6f", initLat)
    val lngStr = String.format(Locale.US, "%.6f", initLng)
    val satFlag = if (initSatellite) "true" else "false"
    return """
<!DOCTYPE html>
<html lang="ar" dir="rtl">
<head>
  <meta charset="utf-8" />
  <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <style>
    html, body, #map { margin: 0; padding: 0; width: 100%; height: 100%; background: #e2e8f0; }
    .school-pin {
      width: 24px; height: 24px; border-radius: 50%;
      background: #059669; border: 3px solid #ffffff;
      box-shadow: 0 3px 10px rgba(6, 78, 59, 0.45);
    }
  </style>
</head>
<body>
  <div id="map"></div>
  <script>
    var currentLat = $latStr;
    var currentLng = $lngStr;
    var currentRadius = $initRadius;
    var isSat = $satFlag;

    var map = L.map('map', { zoomControl: true, attributionControl: false })
      .setView([currentLat, currentLng], 17);

    var satelliteLayer = L.tileLayer(
      'https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}',
      { maxZoom: 19 }
    );
    var streetLayer = L.tileLayer(
      'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
      { maxZoom: 19 }
    );

    if (isSat) { satelliteLayer.addTo(map); } else { streetLayer.addTo(map); }

    var pinIcon = L.divIcon({
      className: '',
      html: '<div class="school-pin"></div>',
      iconSize: [24, 24],
      iconAnchor: [12, 12]
    });

    var marker = L.marker([currentLat, currentLng], { draggable: true, icon: pinIcon }).addTo(map);
    var circle = L.circle([currentLat, currentLng], {
      radius: currentRadius,
      color: '#059669',
      weight: 2.5,
      fillColor: '#10b981',
      fillOpacity: 0.24
    }).addTo(map);

    function emitCoords(lat, lng) {
      currentLat = lat;
      currentLng = lng;
      marker.setLatLng([lat, lng]);
      circle.setLatLng([lat, lng]);
      if (window.AndroidMapBridge && window.AndroidMapBridge.onMapCoordsPicked) {
        window.AndroidMapBridge.onMapCoordsPicked(lat, lng);
      }
    }

    map.on('click', function(e) {
      emitCoords(e.latlng.lat, e.latlng.lng);
    });

    marker.on('dragend', function(e) {
      var pos = marker.getLatLng();
      emitCoords(pos.lat, pos.lng);
    });

    window.updateSchoolZone = function(lat, lng, radius, satellite) {
      var moved = Math.abs(lat - currentLat) > 0.00001 || Math.abs(lng - currentLng) > 0.00001;
      currentLat = lat;
      currentLng = lng;
      currentRadius = radius;
      marker.setLatLng([lat, lng]);
      circle.setLatLng([lat, lng]);
      circle.setRadius(radius);
      if (moved) {
        map.panTo([lat, lng], { animate: true });
      }
      if (satellite !== isSat) {
        isSat = satellite;
        if (isSat) {
          if (map.hasLayer(streetLayer)) map.removeLayer(streetLayer);
          if (!map.hasLayer(satelliteLayer)) satelliteLayer.addTo(map);
        } else {
          if (map.hasLayer(satelliteLayer)) map.removeLayer(satelliteLayer);
          if (!map.hasLayer(streetLayer)) streetLayer.addTo(map);
        }
      }
    };
  </script>
</body>
</html>
    """.trimIndent()
}
