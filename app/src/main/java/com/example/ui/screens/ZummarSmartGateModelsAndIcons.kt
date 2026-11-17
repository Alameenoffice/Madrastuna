package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Calendar
import java.util.UUID
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// ==========================================
// الهوية البصرية والألوان الملكية لبوابة زمار الذكية
// ==========================================
val ZummarEmerald = Color(0xFF064E3B)
val ZummarEmeraldMid = Color(0xFF059669)
val ZummarEmeraldLight = Color(0xFF10B981)
val ZummarNavy = Color(0xFF0A1128)
val ZummarNavyMid = Color(0xFF162244)
val ZummarAmberGold = Color(0xFFFFB703)
val ZummarAmberWarm = Color(0xFFFB8500)
val ZummarSkyCyan = Color(0xFF00B4D8)
val ZummarCyanLight = Color(0xFF48CAE4)
val ZummarCrimson = Color(0xFFDC2626)
val ZummarSurfaceBg = Color(0xFFF4F7F6)
val ZummarCardWhite = Color(0xFFFFFFFF)

val ZummarTajawalFont = FontFamily(Font(R.font.tajawal))

// ==========================================
// نماذج البيانات الشاملة لبوابة زمار الذكية
// ==========================================
enum class ZummarRole(val arabicTitle: String, val badgeDesc: String) {
    CITIZEN("مواطن", "التصفح، حجز المواعيد، ومراسلة الصيدليات والتجار"),
    HEALTH_PROVIDER("صيدلية / عيادة طبيب / مختبر", "تثبيت الموقع على الخريطة واستقبال المراجعين"),
    MERCHANT("تاجر / صاحب محل", "تثبيت المحل ونشر البضائع والأسعار في سوق زمار"),
    ELECTRICITY_OFFICIAL("مسؤول دائرة الكهرباء", "رسم أحياء زمار والتحكم بالكهرباء والأعطال"),
    WATER_OFFICIAL("مسؤول دائرة الماء", "رسم قواطع ضخ الماء والتحكم بالفتح والصيانة"),
    SUPREME_ADMIN("المطور والإدارة العليا لبوابة زمار", "صلاحية كاملة لاعتماد الطلبات وإدارة كافة الأقسام")
}

data class ZummarUserAccount(
    val id: String = UUID.randomUUID().toString(),
    val fullName: String,
    val usernameOrPhone: String,
    val password: String,
    val role: ZummarRole,
    val isApproved: Boolean = true,
    val specialtyOrCategory: String = "",
    val workingHours: String = "08:00 ص - 10:00 م",
    val mapX: Float = 0.50f,
    val mapY: Float = 0.50f,
    val isOnDutyPharmacy: Boolean = false
)

data class ZummarPoint(
    val x: Float,
    val y: Float
)

enum class PowerStatus(val arabicLabel: String, val colorHex: Long) {
    NATIONAL_ON("تشغيل وطني", 0xFFFFB703),
    GENERATOR_ON("تشغيل مولدة", 0xFF10B981),
    OFF("إطفاء", 0xFF64748B),
    MAINTENANCE("خلل فني وصيانة", 0xFFDC2626)
}

enum class WaterStatus(val arabicLabel: String, val colorHex: Long) {
    PUMPING_ON("ضخ الماء مفتوح", 0xFF00B4D8),
    PUMPING_OFF("الضخ متوقف حالياً", 0xFF64748B),
    PIPE_MAINTENANCE("صيانة وكسر أنبوب", 0xFFDC2626)
}

data class ZummarNeighborhoodZone(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val points: List<ZummarPoint>,
    val powerStatus: PowerStatus = PowerStatus.NATIONAL_ON,
    val faultReason: String = "",
    val expectedRestoreDate: String = "",
    val expectedRestoreTime: String = "",
    val outageDurationHours: String = "",
    val waterStatus: WaterStatus = WaterStatus.PUMPING_ON,
    val waterOpenTime: String = "06:00 صباحاً - 02:00 ظهراً",
    val waterDurationHours: String = "8 ساعات",
    val waterMaintenanceNote: String = ""
)

enum class ZummarMarkerType(val arabicLabel: String) {
    PHARMACY("صيدلية"),
    CLINIC("عيادة طبيب"),
    LAB("مختبر تحليلات"),
    SHOP("محل تجاري"),
    GENERATOR("مولدة أهلية"),
    GOV_OFFICE("دائرة حكومية")
}

data class ZummarLandmarkMarker(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val subtitleOrOwner: String,
    val type: ZummarMarkerType,
    val mapX: Float,
    val mapY: Float,
    val workingHours: String = "08:00 ص - 10:00 م",
    val phoneOrUsername: String = "",
    val isApproved: Boolean = true,
    val isOnDuty: Boolean = false,
    val amperePriceIqd: String = "",
    val specialty: String = ""
)

data class ZummarProduct(
    val id: String = UUID.randomUUID().toString(),
    val shopMarkerId: String,
    val shopName: String,
    val productName: String,
    val priceIqd: String,
    val description: String,
    val imageBase64: String = ""
)

data class ZummarBooking(
    val id: String = UUID.randomUUID().toString(),
    val clinicMarkerId: String,
    val clinicName: String,
    val doctorSpecialty: String,
    val patientName: String,
    val patientPhone: String,
    val appointmentDate: String,
    val appointmentTime: String,
    val notes: String,
    val bookingCode: String = "ZMR-" + (1000..9999).random()
)

data class ZummarChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val targetMarkerId: String,
    val targetTitle: String,
    val senderName: String,
    val senderRole: String,
    val messageText: String,
    val attachmentBase64: String = "",
    val timestampLabel: String
)

data class ZummarFeedbackBanner(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val isSuccess: Boolean = true
)

// ==========================================
// أيقونات مرسومة برمجياً بالكامل عبر المنحنيات الهندسية (Canvas)
// ==========================================
enum class ZummarVectorIconType {
    MAP_COMPASS,
    ELECTRICITY_BOLT,
    WATER_WAVE,
    HEALTH_SHIELD,
    BAZAAR_STORE,
    ADMIN_CROWN,
    USER_BADGE,
    PLUS_DRAW,
    POLYGON_LASSO,
    CHECK_VERIFIED,
    ALERT_WARNING,
    CALENDAR_PICKER,
    CLOCK_TIME,
    CHAT_MESSAGE,
    CAMERA_PHOTO,
    GENERATOR_TURBINE,
    GOV_DOME,
    ZOOM_IN,
    ZOOM_OUT,
    RESET_VIEW,
    LOGOUT_DOOR,
    CLOSE_X
}

@Composable
fun ZummarCustomIcon(
    type: ZummarVectorIconType,
    tint: Color,
    modifier: Modifier = Modifier.size(22.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val strokeW = (w * 0.095f).coerceAtLeast(2f)
        val stroke = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (type) {
            ZummarVectorIconType.MAP_COMPASS -> {
                drawCircle(color = tint, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val needle = Path().apply {
                    moveTo(w * 0.5f, h * 0.18f)
                    cubicTo(w * 0.62f, h * 0.38f, w * 0.65f, h * 0.45f, w * 0.5f, h * 0.82f)
                    cubicTo(w * 0.35f, h * 0.45f, w * 0.38f, h * 0.38f, w * 0.5f, h * 0.18f)
                    close()
                }
                drawPath(needle, color = tint.copy(alpha = 0.28f))
                drawPath(needle, color = tint, style = stroke)
                drawCircle(color = tint, radius = w * 0.07f, center = Offset(w * 0.5f, h * 0.5f))
            }

            ZummarVectorIconType.ELECTRICITY_BOLT -> {
                val bolt = Path().apply {
                    moveTo(w * 0.56f, h * 0.10f)
                    lineTo(w * 0.24f, h * 0.52f)
                    lineTo(w * 0.50f, h * 0.52f)
                    lineTo(w * 0.40f, h * 0.90f)
                    lineTo(w * 0.76f, h * 0.44f)
                    lineTo(w * 0.50f, h * 0.44f)
                    close()
                }
                drawPath(bolt, color = tint.copy(alpha = 0.25f))
                drawPath(bolt, color = tint, style = stroke)
            }

            ZummarVectorIconType.WATER_WAVE -> {
                val drop = Path().apply {
                    moveTo(w * 0.5f, h * 0.12f)
                    cubicTo(w * 0.82f, h * 0.48f, w * 0.80f, h * 0.84f, w * 0.5f, h * 0.88f)
                    cubicTo(w * 0.20f, h * 0.84f, w * 0.18f, h * 0.48f, w * 0.5f, h * 0.12f)
                    close()
                }
                drawPath(drop, color = tint.copy(alpha = 0.22f))
                drawPath(drop, color = tint, style = stroke)
                val wave = Path().apply {
                    moveTo(w * 0.28f, h * 0.62f)
                    cubicTo(w * 0.40f, h * 0.52f, w * 0.60f, h * 0.72f, w * 0.72f, h * 0.60f)
                }
                drawPath(wave, color = tint, style = stroke)
            }

            ZummarVectorIconType.HEALTH_SHIELD -> {
                val shield = Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    cubicTo(w * 0.82f, h * 0.16f, w * 0.86f, h * 0.26f, w * 0.84f, h * 0.52f)
                    cubicTo(w * 0.82f, h * 0.74f, w * 0.64f, h * 0.86f, w * 0.5f, h * 0.92f)
                    cubicTo(w * 0.36f, h * 0.86f, w * 0.18f, h * 0.74f, w * 0.16f, h * 0.52f)
                    cubicTo(w * 0.14f, h * 0.26f, w * 0.18f, h * 0.16f, w * 0.5f, h * 0.10f)
                    close()
                }
                drawPath(shield, color = tint.copy(alpha = 0.20f))
                drawPath(shield, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.33f), Offset(w * 0.5f, h * 0.67f), strokeWidth = strokeW * 1.15f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.33f, h * 0.50f), Offset(w * 0.67f, h * 0.50f), strokeWidth = strokeW * 1.15f, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.BAZAAR_STORE -> {
                val awning = Path().apply {
                    moveTo(w * 0.14f, h * 0.38f)
                    lineTo(w * 0.24f, h * 0.16f)
                    lineTo(w * 0.76f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.38f)
                    cubicTo(w * 0.74f, h * 0.48f, w * 0.62f, h * 0.48f, w * 0.50f, h * 0.38f)
                    cubicTo(w * 0.38f, h * 0.48f, w * 0.26f, h * 0.48f, w * 0.14f, h * 0.38f)
                    close()
                }
                drawPath(awning, color = tint.copy(alpha = 0.25f))
                drawPath(awning, color = tint, style = stroke)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.20f, h * 0.40f),
                    size = Size(w * 0.60f, h * 0.46f),
                    cornerRadius = CornerRadius(w * 0.06f, w * 0.06f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.50f, h * 0.54f), Offset(w * 0.50f, h * 0.86f), strokeWidth = strokeW)
            }

            ZummarVectorIconType.ADMIN_CROWN -> {
                val crown = Path().apply {
                    moveTo(w * 0.14f, h * 0.76f)
                    lineTo(w * 0.18f, h * 0.30f)
                    lineTo(w * 0.38f, h * 0.52f)
                    lineTo(w * 0.50f, h * 0.20f)
                    lineTo(w * 0.62f, h * 0.52f)
                    lineTo(w * 0.82f, h * 0.30f)
                    lineTo(w * 0.86f, h * 0.76f)
                    close()
                }
                drawPath(crown, color = tint.copy(alpha = 0.24f))
                drawPath(crown, color = tint, style = stroke)
            }

            ZummarVectorIconType.USER_BADGE -> {
                drawCircle(color = tint, radius = w * 0.18f, center = Offset(w * 0.5f, h * 0.32f), style = stroke)
                val shoulders = Path().apply {
                    moveTo(w * 0.20f, h * 0.84f)
                    cubicTo(w * 0.22f, h * 0.58f, w * 0.78f, h * 0.58f, w * 0.80f, h * 0.84f)
                }
                drawPath(shoulders, color = tint, style = stroke)
            }

            ZummarVectorIconType.PLUS_DRAW -> {
                drawCircle(color = tint.copy(alpha = 0.18f), radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = tint, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.26f), Offset(w * 0.5f, h * 0.74f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.26f, h * 0.5f), Offset(w * 0.74f, h * 0.5f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.POLYGON_LASSO -> {
                val poly = Path().apply {
                    moveTo(w * 0.22f, h * 0.26f)
                    lineTo(w * 0.76f, h * 0.20f)
                    lineTo(w * 0.84f, h * 0.68f)
                    lineTo(w * 0.34f, h * 0.82f)
                    close()
                }
                drawPath(poly, color = tint.copy(alpha = 0.20f))
                drawPath(poly, color = tint, style = stroke)
                listOf(
                    Offset(w * 0.22f, h * 0.26f),
                    Offset(w * 0.76f, h * 0.20f),
                    Offset(w * 0.84f, h * 0.68f),
                    Offset(w * 0.34f, h * 0.82f)
                ).forEach { pt ->
                    drawCircle(color = tint, radius = w * 0.08f, center = pt)
                }
            }

            ZummarVectorIconType.CHECK_VERIFIED -> {
                drawCircle(color = tint.copy(alpha = 0.20f), radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f))
                drawCircle(color = tint, radius = w * 0.44f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                val check = Path().apply {
                    moveTo(w * 0.30f, h * 0.52f)
                    lineTo(w * 0.45f, h * 0.67f)
                    lineTo(w * 0.72f, h * 0.36f)
                }
                drawPath(check, color = tint, style = stroke)
            }

            ZummarVectorIconType.ALERT_WARNING -> {
                val tri = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.88f, h * 0.84f)
                    lineTo(w * 0.12f, h * 0.84f)
                    close()
                }
                drawPath(tri, color = tint.copy(alpha = 0.20f))
                drawPath(tri, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.38f), Offset(w * 0.5f, h * 0.60f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawCircle(color = tint, radius = w * 0.05f, center = Offset(w * 0.5f, h * 0.72f))
            }

            ZummarVectorIconType.CALENDAR_PICKER -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.72f, h * 0.64f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.14f, h * 0.40f), Offset(w * 0.86f, h * 0.40f), strokeWidth = strokeW)
                drawLine(tint, Offset(w * 0.32f, h * 0.12f), Offset(w * 0.32f, h * 0.28f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.68f, h * 0.12f), Offset(w * 0.68f, h * 0.28f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.CLOCK_TIME -> {
                drawCircle(color = tint, radius = w * 0.42f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.5f, h * 0.26f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.5f, h * 0.5f), Offset(w * 0.68f, h * 0.58f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.CHAT_MESSAGE -> {
                val bubble = Path().apply {
                    moveTo(w * 0.16f, h * 0.24f)
                    cubicTo(w * 0.16f, h * 0.14f, w * 0.84f, h * 0.14f, w * 0.84f, h * 0.24f)
                    lineTo(w * 0.84f, h * 0.64f)
                    cubicTo(w * 0.84f, h * 0.74f, w * 0.52f, h * 0.74f, w * 0.42f, h * 0.74f)
                    lineTo(w * 0.22f, h * 0.88f)
                    lineTo(w * 0.26f, h * 0.72f)
                    cubicTo(w * 0.18f, h * 0.70f, w * 0.16f, h * 0.66f, w * 0.16f, h * 0.60f)
                    close()
                }
                drawPath(bubble, color = tint.copy(alpha = 0.20f))
                drawPath(bubble, color = tint, style = stroke)
            }

            ZummarVectorIconType.CAMERA_PHOTO -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.12f, h * 0.28f),
                    size = Size(w * 0.76f, h * 0.54f),
                    cornerRadius = CornerRadius(w * 0.10f, w * 0.10f),
                    style = stroke
                )
                drawCircle(color = tint, radius = w * 0.16f, center = Offset(w * 0.5f, h * 0.55f), style = stroke)
            }

            ZummarVectorIconType.GENERATOR_TURBINE -> {
                drawCircle(color = tint, radius = w * 0.40f, center = Offset(w * 0.5f, h * 0.5f), style = stroke)
                drawCircle(color = tint, radius = w * 0.12f, center = Offset(w * 0.5f, h * 0.5f))
                for (angle in listOf(0f, 120f, 240f)) {
                    val rad = angle * (PI.toFloat() / 180f)
                    drawLine(
                        color = tint,
                        start = Offset(w * 0.5f, h * 0.5f),
                        end = Offset(w * 0.5f + cos(rad) * w * 0.34f, h * 0.5f + sin(rad) * h * 0.34f),
                        strokeWidth = strokeW,
                        cap = StrokeCap.Round
                    )
                }
            }

            ZummarVectorIconType.GOV_DOME -> {
                val dome = Path().apply {
                    moveTo(w * 0.18f, h * 0.46f)
                    cubicTo(w * 0.20f, h * 0.16f, w * 0.80f, h * 0.16f, w * 0.82f, h * 0.46f)
                    close()
                }
                drawPath(dome, color = tint.copy(alpha = 0.24f))
                drawPath(dome, color = tint, style = stroke)
                drawLine(tint, Offset(w * 0.14f, h * 0.82f), Offset(w * 0.86f, h * 0.82f), strokeWidth = strokeW)
                for (colX in listOf(0.28f, 0.50f, 0.72f)) {
                    drawLine(tint, Offset(w * colX, h * 0.46f), Offset(w * colX, h * 0.82f), strokeWidth = strokeW)
                }
            }

            ZummarVectorIconType.ZOOM_IN -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.44f, h * 0.44f), style = stroke)
                drawLine(tint, Offset(w * 0.68f, h * 0.68f), Offset(w * 0.88f, h * 0.88f), strokeWidth = strokeW * 1.2f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.44f, h * 0.28f), Offset(w * 0.44f, h * 0.60f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.28f, h * 0.44f), Offset(w * 0.60f, h * 0.44f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.ZOOM_OUT -> {
                drawCircle(color = tint, radius = w * 0.34f, center = Offset(w * 0.44f, h * 0.44f), style = stroke)
                drawLine(tint, Offset(w * 0.68f, h * 0.68f), Offset(w * 0.88f, h * 0.88f), strokeWidth = strokeW * 1.2f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.28f, h * 0.44f), Offset(w * 0.60f, h * 0.44f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.RESET_VIEW -> {
                drawArc(
                    color = tint,
                    startAngle = 30f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.68f, h * 0.68f),
                    style = stroke
                )
            }

            ZummarVectorIconType.LOGOUT_DOOR -> {
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.16f, h * 0.16f),
                    size = Size(w * 0.44f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.08f, w * 0.08f),
                    style = stroke
                )
                drawLine(tint, Offset(w * 0.44f, h * 0.50f), Offset(w * 0.86f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.34f), Offset(w * 0.86f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.70f, h * 0.66f), Offset(w * 0.86f, h * 0.50f), strokeWidth = strokeW, cap = StrokeCap.Round)
            }

            ZummarVectorIconType.CLOSE_X -> {
                drawLine(tint, Offset(w * 0.24f, h * 0.24f), Offset(w * 0.76f, h * 0.76f), strokeWidth = strokeW * 1.1f, cap = StrokeCap.Round)
                drawLine(tint, Offset(w * 0.76f, h * 0.24f), Offset(w * 0.24f, h * 0.76f), strokeWidth = strokeW * 1.1f, cap = StrokeCap.Round)
            }
        }
    }
}

// ==========================================
// 1. البوابة المعمارية المزدوجة المتحركة التي تنفتح بانسيابية
// ==========================================
@Composable
fun InteractiveOpeningSmartGateCanvas(
    isGateOpen: Boolean,
    onToggleGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val openFraction by animateFloatAsState(
        targetValue = if (isGateOpen) 1f else 0.18f,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "zummar_gate_open_fraction"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "zummar_gate_glow")
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zummar_light_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZummarNavy, Color(0xFF072F29), ZummarEmerald)
                )
            )
            .border(1.5.dp, ZummarAmberGold.copy(alpha = 0.65f), RoundedCornerShape(26.dp))
            .clickable { onToggleGate() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f

                // الهالة النورانية خلف مصراعي البوابة المعمارية
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZummarAmberGold.copy(alpha = 0.48f * openFraction * lightPulse.coerceAtMost(1f)),
                            ZummarSkyCyan.copy(alpha = 0.22f * openFraction),
                            Color.Transparent
                        ),
                        center = Offset(cx, h * 0.56f),
                        radius = w * 0.42f * lightPulse
                    ),
                    center = Offset(cx, h * 0.56f),
                    radius = w * 0.42f * lightPulse
                )

                // نهر دجلة وأفق زمار خلف البوابة المفتوحة
                val horizonWave = Path().apply {
                    moveTo(w * 0.22f, h * 0.78f)
                    cubicTo(w * 0.38f, h * 0.68f, w * 0.62f, h * 0.86f, w * 0.78f, h * 0.74f)
                }
                drawPath(
                    path = horizonWave,
                    color = ZummarSkyCyan.copy(alpha = 0.65f * openFraction),
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                )

                // القوس المعماري الإسلامي/الحضاري لبوابة زمار
                val archOuter = Path().apply {
                    moveTo(w * 0.14f, h * 0.94f)
                    lineTo(w * 0.14f, h * 0.36f)
                    cubicTo(w * 0.14f, h * 0.12f, w * 0.36f, h * 0.06f, cx, h * 0.04f)
                    cubicTo(w * 0.64f, h * 0.06f, w * 0.86f, h * 0.12f, w * 0.86f, h * 0.36f)
                    lineTo(w * 0.86f, h * 0.94f)
                }
                drawPath(
                    path = archOuter,
                    color = ZummarAmberGold,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // المصراع الأيمن والأيسر للبوابة ينفتحان تدريجياً
                val leafMaxSpan = w * 0.33f
                val currentLeafSpan = leafMaxSpan * (1f - openFraction * 0.76f)

                // المصراع الأيسر
                val leftLeaf = Path().apply {
                    moveTo(w * 0.16f, h * 0.92f)
                    lineTo(w * 0.16f, h * 0.38f)
                    cubicTo(
                        w * 0.16f + currentLeafSpan * 0.35f,
                        h * 0.20f,
                        w * 0.16f + currentLeafSpan * 0.75f,
                        h * 0.16f,
                        w * 0.16f + currentLeafSpan,
                        h * 0.24f
                    )
                    lineTo(w * 0.16f + currentLeafSpan, h * 0.92f)
                    close()
                }
                drawPath(
                    path = leftLeaf,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF043927), Color(0xFF0A6847))
                    )
                )
                drawPath(
                    path = leftLeaf,
                    color = ZummarAmberGold,
                    style = Stroke(width = 2.dp.toPx())
                )

                // المصراع الأيمن
                val rightLeaf = Path().apply {
                    moveTo(w * 0.84f, h * 0.92f)
                    lineTo(w * 0.84f, h * 0.38f)
                    cubicTo(
                        w * 0.84f - currentLeafSpan * 0.35f,
                        h * 0.20f,
                        w * 0.84f - currentLeafSpan * 0.75f,
                        h * 0.16f,
                        w * 0.84f - currentLeafSpan,
                        h * 0.24f
                    )
                    lineTo(w * 0.84f - currentLeafSpan, h * 0.92f)
                    close()
                }
                drawPath(
                    path = rightLeaf,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF0A6847), Color(0xFF043927))
                    )
                )
                drawPath(
                    path = rightLeaf,
                    color = ZummarAmberGold,
                    style = Stroke(width = 2.dp.toPx())
                )

                // مقابض ذهبية مزخرفة على مصراعي البوابة
                drawCircle(
                    color = ZummarAmberGold,
                    radius = 4.5.dp.toPx(),
                    center = Offset(w * 0.16f + currentLeafSpan * 0.78f, h * 0.58f)
                )
                drawCircle(
                    color = ZummarAmberGold,
                    radius = 4.5.dp.toPx(),
                    center = Offset(w * 0.84f - currentLeafSpan * 0.78f, h * 0.58f)
                )
            }

            Text(
                text = "أهلاً بك في بوابة زمار الذكية — نحن هنا لخدمتك وتسهيل حياتك اليومية، اختر وجهتك الخدمية الآن",
                fontFamily = ZummarTajawalFont,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ==========================================
// 2. واجهة المتجر التفاعلية التي تنفتح أبوابها عند النقر على المحل
// ==========================================
@Composable
fun InteractiveOpeningStorefrontCanvas(
    shopName: String,
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val doorOpenProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing),
        label = "storefront_door_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZummarNavy, ZummarEmerald)
                )
            )
            .border(1.2.dp, ZummarAmberGold.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            .padding(12.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
        ) {
            val w = size.width
            val h = size.height

            // المظلة التجارية المزخرفة
            val awning = Path().apply {
                moveTo(w * 0.12f, h * 0.34f)
                lineTo(w * 0.20f, h * 0.10f)
                lineTo(w * 0.80f, h * 0.10f)
                lineTo(w * 0.88f, h * 0.34f)
                cubicTo(w * 0.75f, h * 0.44f, w * 0.62f, h * 0.44f, w * 0.50f, h * 0.34f)
                cubicTo(w * 0.38f, h * 0.44f, w * 0.25f, h * 0.44f, w * 0.12f, h * 0.34f)
                close()
            }
            drawPath(awning, color = ZummarAmberGold)

            // إضاءة داخل المتجر عند انفتاح الأبواب
            drawRoundRect(
                color = Color(0xFFFFFBEB).copy(alpha = 0.25f + 0.55f * doorOpenProgress),
                topLeft = Offset(w * 0.22f, h * 0.36f),
                size = Size(w * 0.56f, h * 0.56f),
                cornerRadius = CornerRadius(10f, 10f)
            )

            val doorWidth = (w * 0.28f) * (1f - 0.72f * doorOpenProgress)
            // باب المتجر الأيسر
            drawRect(
                color = Color(0xFF065F46),
                topLeft = Offset(w * 0.22f, h * 0.36f),
                size = Size(doorWidth, h * 0.56f)
            )
            // باب المتجر الأيمن
            drawRect(
                color = Color(0xFF065F46),
                topLeft = Offset(w * 0.78f - doorWidth, h * 0.36f),
                size = Size(doorWidth, h * 0.56f)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "واجهة $shopName الرقمية المفتوحة",
            fontFamily = ZummarTajawalFont,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 14.sp,
            color = ZummarAmberGold
        )
    }
}

// ==========================================
// 3. شريط الإشعارات والتغذية الراجعة العائم (Action Feedback Banner)
// ==========================================
@Composable
fun ZummarFloatingFeedbackBanner(
    banner: ZummarFeedbackBanner?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = banner != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        if (banner != null) {
            val bg = if (banner.isSuccess) ZummarEmerald else ZummarCrimson
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = bg,
                border = BorderStroke(1.2.dp, ZummarAmberGold.copy(alpha = 0.75f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        ZummarCustomIcon(
                            type = if (banner.isSuccess) ZummarVectorIconType.CHECK_VERIFIED else ZummarVectorIconType.ALERT_WARNING,
                            tint = ZummarAmberGold,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = banner.message,
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        ZummarCustomIcon(
                            type = ZummarVectorIconType.CLOSE_X,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. منتقي التقويم التفاعلي (Calendar Date Picker) ومنتقي الوقت
// ==========================================
@Composable
fun ZummarCalendarDatePickerDialog(
    title: String = "اختر التاريخ من التقويم التفاعلي",
    onDateSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val calendar = remember { Calendar.getInstance() }
    var selectedYear by remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }
    var selectedMonth by remember { mutableIntStateOf(calendar.get(Calendar.MONTH) + 1) }
    var selectedDay by remember { mutableIntStateOf(calendar.get(Calendar.DAY_OF_MONTH)) }

    val arabicMonths = listOf(
        "كانون الثاني (1)", "شباط (2)", "آذار (3)", "نيسان (4)",
        "أيار (5)", "حزيران (6)", "تموز (7)", "آب (8)",
        "أيلول (9)", "تشرين الأول (10)", "تشرين الثاني (11)", "كانون الأول (12)"
    )

    val daysInMonth = remember(selectedYear, selectedMonth) {
        when (selectedMonth) {
            2 -> if (selectedYear % 4 == 0) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = ZummarCardWhite,
            border = BorderStroke(1.5.dp, ZummarEmerald.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ZummarCustomIcon(ZummarVectorIconType.CALENDAR_PICKER, ZummarEmerald, Modifier.size(22.dp))
                    Text(
                        text = title,
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = ZummarNavy
                    )
                }

                // شريط اختيار الشهر والسنة
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = {
                            if (selectedMonth == 1) {
                                selectedMonth = 12
                                selectedYear -= 1
                            } else {
                                selectedMonth -= 1
                            }
                        }
                    ) {
                        Text("السابق", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "${arabicMonths[selectedMonth - 1]} $selectedYear",
                        fontFamily = ZummarTajawalFont,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = ZummarEmerald
                    )
                    OutlinedButton(
                        onClick = {
                            if (selectedMonth == 12) {
                                selectedMonth = 1
                                selectedYear += 1
                            } else {
                                selectedMonth += 1
                            }
                        }
                    ) {
                        Text("التالي", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold)
                    }
                }

                // شبكة الأيام التفاعلية
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val rows = (1..daysInMonth).chunked(7)
                    rows.forEach { weekDays ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            weekDays.forEach { day ->
                                val isSelected = day == selectedDay
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) ZummarEmerald else Color(0xFFE2E8F0))
                                        .clickable { selectedDay = day },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        fontFamily = ZummarTajawalFont,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else ZummarNavy,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            repeat(7 - weekDays.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("إلغاء", fontFamily = ZummarTajawalFont, color = ZummarCrimson)
                    }
                    Button(
                        onClick = {
                            val formatted = String.format("%04d-%02d-%02d", selectedYear, selectedMonth, selectedDay)
                            onDateSelected(formatted)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ZummarEmerald),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("اعتماد التاريخ", fontFamily = ZummarTajawalFont, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. مستودع البيانات الدائم (SharedPreferences + Firebase REST Sync)
// ==========================================
object ZummarRepository {
    private const val PREFS_NAME = "zummar_smart_gate_storage_v1"
    private const val KEY_STATE_JSON = "zummar_full_state_json"
    private const val FIREBASE_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com/zummar_smart_gate.json"

    fun encodeUriToBase64(context: Context, uri: Uri): String {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return ""
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return ""
            val maxDim = 480
            val ratio = minOf(maxDim.toFloat() / original.width, maxDim.toFloat() / original.height, 1f)
            val scaled = android.graphics.Bitmap.createScaledBitmap(
                original,
                (original.width * ratio).toInt().coerceAtLeast(1),
                (original.height * ratio).toInt().coerceAtLeast(1),
                true
            )
            val out = ByteArrayOutputStream()
            scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            ""
        }
    }

    fun decodeBase64ToImageBitmap(base64: String): ImageBitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            bmp?.asImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    fun defaultUsers(): List<ZummarUserAccount> = listOf(
        ZummarUserAccount(
            id = "admin-master",
            fullName = "المطور والإدارة العليا لبوابة زمار",
            usernameOrPhone = "admin",
            password = "1234",
            role = ZummarRole.SUPREME_ADMIN,
            isApproved = true,
            specialtyOrCategory = "الإدارة العامة لبوابة زمار الذكية"
        ),
        ZummarUserAccount(
            id = "elec-official-1",
            fullName = "مهندس صيانة كهرباء زمار",
            usernameOrPhone = "elec",
            password = "1234",
            role = ZummarRole.ELECTRICITY_OFFICIAL,
            isApproved = true,
            specialtyOrCategory = "دائرة كهرباء ناحية زمار"
        ),
        ZummarUserAccount(
            id = "water-official-1",
            fullName = "مسؤول مشروع ماء زمار",
            usernameOrPhone = "water",
            password = "1234",
            role = ZummarRole.WATER_OFFICIAL,
            isApproved = true,
            specialtyOrCategory = "مديرية ماء ناحية زمار"
        )
    )

    fun defaultZones(): List<ZummarNeighborhoodZone> = listOf(
        ZummarNeighborhoodZone(
            id = "zone-center",
            name = "مركز زمار التجاري",
            points = listOf(
                ZummarPoint(0.36f, 0.34f),
                ZummarPoint(0.58f, 0.32f),
                ZummarPoint(0.60f, 0.52f),
                ZummarPoint(0.35f, 0.54f)
            ),
            powerStatus = PowerStatus.NATIONAL_ON,
            waterStatus = WaterStatus.PUMPING_ON,
            waterOpenTime = "06:00 صباحاً - 02:00 ظهراً",
            waterDurationHours = "8 ساعات"
        ),
        ZummarNeighborhoodZone(
            id = "zone-shuhada",
            name = "حي الشهداء",
            points = listOf(
                ZummarPoint(0.12f, 0.22f),
                ZummarPoint(0.33f, 0.22f),
                ZummarPoint(0.33f, 0.46f),
                ZummarPoint(0.10f, 0.44f)
            ),
            powerStatus = PowerStatus.GENERATOR_ON,
            waterStatus = WaterStatus.PUMPING_ON,
            waterOpenTime = "08:00 صباحاً - 04:00 عصراً",
            waterDurationHours = "8 ساعات"
        ),
        ZummarNeighborhoodZone(
            id = "zone-muallimeen",
            name = "حي المعلمين",
            points = listOf(
                ZummarPoint(0.62f, 0.24f),
                ZummarPoint(0.88f, 0.26f),
                ZummarPoint(0.86f, 0.50f),
                ZummarPoint(0.63f, 0.48f)
            ),
            powerStatus = PowerStatus.MAINTENANCE,
            faultReason = "صيانة محولة الشبكة الرئيسية وتبديل القابلوات",
            expectedRestoreDate = "2026-11-17",
            expectedRestoreTime = "05:30 مساءً",
            outageDurationHours = "3 ساعات",
            waterStatus = WaterStatus.PUMPING_OFF,
            waterOpenTime = "05:00 عصراً - 11:00 ليلاً",
            waterDurationHours = "6 ساعات"
        ),
        ZummarNeighborhoodZone(
            id = "zone-ziraee",
            name = "الحي الزراعي وعين حلوة",
            points = listOf(
                ZummarPoint(0.20f, 0.58f),
                ZummarPoint(0.74f, 0.58f),
                ZummarPoint(0.78f, 0.84f),
                ZummarPoint(0.18f, 0.82f)
            ),
            powerStatus = PowerStatus.NATIONAL_ON,
            waterStatus = WaterStatus.PUMPING_ON,
            waterOpenTime = "07:00 صباحاً - 03:00 عصراً",
            waterDurationHours = "8 ساعات"
        )
    )

    fun defaultMarkers(): List<ZummarLandmarkMarker> = listOf(
        ZummarLandmarkMarker(
            id = "marker-pharm-1",
            title = "صيدلية زمار المركزية الخافرة",
            subtitleOrOwner = "د. أحمد الزماري — خافرة 24 ساعة",
            type = ZummarMarkerType.PHARMACY,
            mapX = 0.46f,
            mapY = 0.42f,
            workingHours = "مفتوحة 24 ساعة (خافرة اليوم)",
            phoneOrUsername = "07701234567",
            isApproved = true,
            isOnDuty = true,
            specialty = "الأدوية المزمنة والطوارئ والمستلزمات الطبية"
        ),
        ZummarLandmarkMarker(
            id = "marker-pharm-2",
            title = "صيدلية النور الحديث",
            subtitleOrOwner = "د. سارة الموصلي",
            type = ZummarMarkerType.PHARMACY,
            mapX = 0.24f,
            mapY = 0.34f,
            workingHours = "08:00 ص - 11:00 م",
            phoneOrUsername = "07501122334",
            isApproved = true,
            isOnDuty = true,
            specialty = "صرف الوصفات الطبية وأدوية الأطفال"
        ),
        ZummarLandmarkMarker(
            id = "marker-clinic-1",
            title = "عيادة د. عمر الجبوري التخصصية",
            subtitleOrOwner = "اختصاص باطنية وقلبية وأمراض مزمنة",
            type = ZummarMarkerType.CLINIC,
            mapX = 0.52f,
            mapY = 0.38f,
            workingHours = "04:00 عصراً - 09:00 مساءً",
            phoneOrUsername = "07719988776",
            isApproved = true,
            specialty = "باطنية وقلبية وتخطيط القلب"
        ),
        ZummarLandmarkMarker(
            id = "marker-lab-1",
            title = "مختبر دجلة للتحليلات المرضية",
            subtitleOrOwner = "تحليلات شاملة وفحوصات هرمونية",
            type = ZummarMarkerType.LAB,
            mapX = 0.40f,
            mapY = 0.48f,
            workingHours = "07:30 ص - 09:00 م",
            phoneOrUsername = "07723344556",
            isApproved = true,
            specialty = "فحوصات الدم الشاملة والفيتامينات"
        ),
        ZummarLandmarkMarker(
            id = "marker-shop-1",
            title = "أسواق ومجمع بركة زمار التجاري",
            subtitleOrOwner = "الحاج محمود — مواد غذائية ومنزلية",
            type = ZummarMarkerType.SHOP,
            mapX = 0.48f,
            mapY = 0.66f,
            workingHours = "07:00 ص - 11:30 م",
            phoneOrUsername = "07705566778",
            isApproved = true,
            specialty = "مواد غذائية بالجملة والمفرد"
        ),
        ZummarLandmarkMarker(
            id = "marker-shop-2",
            title = "معرض زمار للأجهزة الكهربائية والطاقة",
            subtitleOrOwner = "مؤسسة الفراتين للتجارة",
            type = ZummarMarkerType.SHOP,
            mapX = 0.68f,
            mapY = 0.64f,
            workingHours = "09:00 ص - 09:30 م",
            phoneOrUsername = "07514433221",
            isApproved = true,
            specialty = "أجهزة منزلية ومنظومات طاقة شمسية"
        ),
        ZummarLandmarkMarker(
            id = "marker-gen-1",
            title = "مولدة حي الشهداء النموذجية",
            subtitleOrOwner = "إدارة أبو مصطفى",
            type = ZummarMarkerType.GENERATOR,
            mapX = 0.22f,
            mapY = 0.42f,
            workingHours = "التشغيل التناوبي الذهبي 24 ساعة",
            phoneOrUsername = "07708899001",
            isApproved = true,
            amperePriceIqd = "12,000 د.ع للأمبير"
        ),
        ZummarLandmarkMarker(
            id = "marker-gov-1",
            title = "مديرية ناحية زمار والبلدية",
            subtitleOrOwner = "المجمع الحكومي الرسمي — مركز زمار",
            type = ZummarMarkerType.GOV_OFFICE,
            mapX = 0.56f,
            mapY = 0.46f,
            workingHours = "08:00 ص - 02:30 ظهراً",
            phoneOrUsername = "الاستعلامات الحكومية",
            isApproved = true
        )
    )

    fun defaultProducts(): List<ZummarProduct> = listOf(
        ZummarProduct(
            id = "prod-1",
            shopMarkerId = "marker-shop-1",
            shopName = "أسواق ومجمع بركة زمار التجاري",
            productName = "كيس رز عنبر درجة أولى (25 كغم)",
            priceIqd = "42,000 د.ع",
            description = "رز عنبر عراقي فاخر مفحوص ومكفول مع توصيل مجاني داخل أحياء زمار."
        ),
        ZummarProduct(
            id = "prod-2",
            shopMarkerId = "marker-shop-1",
            shopName = "أسواق ومجمع بركة زمار التجاري",
            productName = "زيت طعام نباتي نقي (صندوق 4 عبوات)",
            priceIqd = "28,500 د.ع",
            description = "عرض خاص للعوائل في ناحية زمار."
        ),
        ZummarProduct(
            id = "prod-3",
            shopMarkerId = "marker-shop-2",
            shopName = "معرض زمار للأجهزة الكهربائية والطاقة",
            productName = "عاكسة شحن ذكية مع بطارية ليثيوم منزلية",
            priceIqd = "385,000 د.ع",
            description = "تشغيل الإنارة والشاشات والراوتر طوال فترة انقطاع الكهرباء مع ضمان سنة."
        )
    )

    fun saveFullState(
        context: Context,
        scope: CoroutineScope,
        users: List<ZummarUserAccount>,
        zones: List<ZummarNeighborhoodZone>,
        markers: List<ZummarLandmarkMarker>,
        products: List<ZummarProduct>,
        bookings: List<ZummarBooking>,
        messages: List<ZummarChatMessage>
    ) {
        val root = JSONObject()
        val usersArr = JSONArray()
        users.forEach { u ->
            usersArr.put(JSONObject().apply {
                put("id", u.id)
                put("fullName", u.fullName)
                put("usernameOrPhone", u.usernameOrPhone)
                put("password", u.password)
                put("role", u.role.name)
                put("isApproved", u.isApproved)
                put("specialtyOrCategory", u.specialtyOrCategory)
                put("workingHours", u.workingHours)
                put("mapX", u.mapX.toDouble())
                put("mapY", u.mapY.toDouble())
                put("isOnDutyPharmacy", u.isOnDutyPharmacy)
            })
        }
        root.put("users", usersArr)

        val zonesArr = JSONArray()
        zones.forEach { z ->
            val ptsArr = JSONArray()
            z.points.forEach { pt ->
                ptsArr.put(JSONObject().apply {
                    put("x", pt.x.toDouble())
                    put("y", pt.y.toDouble())
                })
            }
            zonesArr.put(JSONObject().apply {
                put("id", z.id)
                put("name", z.name)
                put("points", ptsArr)
                put("powerStatus", z.powerStatus.name)
                put("faultReason", z.faultReason)
                put("expectedRestoreDate", z.expectedRestoreDate)
                put("expectedRestoreTime", z.expectedRestoreTime)
                put("outageDurationHours", z.outageDurationHours)
                put("waterStatus", z.waterStatus.name)
                put("waterOpenTime", z.waterOpenTime)
                put("waterDurationHours", z.waterDurationHours)
                put("waterMaintenanceNote", z.waterMaintenanceNote)
            })
        }
        root.put("zones", zonesArr)

        val markersArr = JSONArray()
        markers.forEach { m ->
            markersArr.put(JSONObject().apply {
                put("id", m.id)
                put("title", m.title)
                put("subtitleOrOwner", m.subtitleOrOwner)
                put("type", m.type.name)
                put("mapX", m.mapX.toDouble())
                put("mapY", m.mapY.toDouble())
                put("workingHours", m.workingHours)
                put("phoneOrUsername", m.phoneOrUsername)
                put("isApproved", m.isApproved)
                put("isOnDuty", m.isOnDuty)
                put("amperePriceIqd", m.amperePriceIqd)
                put("specialty", m.specialty)
            })
        }
        root.put("markers", markersArr)

        val prodsArr = JSONArray()
        products.forEach { p ->
            prodsArr.put(JSONObject().apply {
                put("id", p.id)
                put("shopMarkerId", p.shopMarkerId)
                put("shopName", p.shopName)
                put("productName", p.productName)
                put("priceIqd", p.priceIqd)
                put("description", p.description)
                put("imageBase64", p.imageBase64)
            })
        }
        root.put("products", prodsArr)

        val bookingsArr = JSONArray()
        bookings.forEach { b ->
            bookingsArr.put(JSONObject().apply {
                put("id", b.id)
                put("clinicMarkerId", b.clinicMarkerId)
                put("clinicName", b.clinicName)
                put("doctorSpecialty", b.doctorSpecialty)
                put("patientName", b.patientName)
                put("patientPhone", b.patientPhone)
                put("appointmentDate", b.appointmentDate)
                put("appointmentTime", b.appointmentTime)
                put("notes", b.notes)
                put("bookingCode", b.bookingCode)
            })
        }
        root.put("bookings", bookingsArr)

        val msgsArr = JSONArray()
        messages.forEach { msg ->
            msgsArr.put(JSONObject().apply {
                put("id", msg.id)
                put("targetMarkerId", msg.targetMarkerId)
                put("targetTitle", msg.targetTitle)
                put("senderName", msg.senderName)
                put("senderRole", msg.senderRole)
                put("messageText", msg.messageText)
                put("attachmentBase64", msg.attachmentBase64)
                put("timestampLabel", msg.timestampLabel)
            })
        }
        root.put("messages", msgsArr)

        val jsonStr = root.toString()
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATE_JSON, jsonStr)
            .apply()

        scope.launch(Dispatchers.IO) {
            try {
                val conn = (URL(FIREBASE_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    connectTimeout = 7000
                    readTimeout = 7000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(jsonStr) }
                conn.responseCode
                conn.disconnect()
            } catch (_: Exception) {
            }
        }
    }

    data class LoadedState(
        val users: List<ZummarUserAccount>,
        val zones: List<ZummarNeighborhoodZone>,
        val markers: List<ZummarLandmarkMarker>,
        val products: List<ZummarProduct>,
        val bookings: List<ZummarBooking>,
        val messages: List<ZummarChatMessage>
    )

    fun parseJsonState(jsonStr: String): LoadedState? {
        return try {
            val root = JSONObject(jsonStr)
            val users = mutableListOf<ZummarUserAccount>()
            root.optJSONArray("users")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    users.add(
                        ZummarUserAccount(
                            id = o.optString("id"),
                            fullName = o.optString("fullName"),
                            usernameOrPhone = o.optString("usernameOrPhone"),
                            password = o.optString("password"),
                            role = runCatching { ZummarRole.valueOf(o.optString("role")) }.getOrDefault(ZummarRole.CITIZEN),
                            isApproved = o.optBoolean("isApproved", true),
                            specialtyOrCategory = o.optString("specialtyOrCategory"),
                            workingHours = o.optString("workingHours", "08:00 ص - 10:00 م"),
                            mapX = o.optDouble("mapX", 0.5).toFloat(),
                            mapY = o.optDouble("mapY", 0.5).toFloat(),
                            isOnDutyPharmacy = o.optBoolean("isOnDutyPharmacy", false)
                        )
                    )
                }
            }
            if (users.none { it.usernameOrPhone == "admin" }) {
                users.addAll(0, defaultUsers())
            }

            val zones = mutableListOf<ZummarNeighborhoodZone>()
            root.optJSONArray("zones")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val pts = mutableListOf<ZummarPoint>()
                    o.optJSONArray("points")?.let { pArr ->
                        for (j in 0 until pArr.length()) {
                            val po = pArr.getJSONObject(j)
                            pts.add(ZummarPoint(po.optDouble("x", 0.5).toFloat(), po.optDouble("y", 0.5).toFloat()))
                        }
                    }
                    if (pts.size >= 3) {
                        zones.add(
                            ZummarNeighborhoodZone(
                                id = o.optString("id"),
                                name = o.optString("name"),
                                points = pts,
                                powerStatus = runCatching { PowerStatus.valueOf(o.optString("powerStatus")) }.getOrDefault(PowerStatus.NATIONAL_ON),
                                faultReason = o.optString("faultReason"),
                                expectedRestoreDate = o.optString("expectedRestoreDate"),
                                expectedRestoreTime = o.optString("expectedRestoreTime"),
                                outageDurationHours = o.optString("outageDurationHours"),
                                waterStatus = runCatching { WaterStatus.valueOf(o.optString("waterStatus")) }.getOrDefault(WaterStatus.PUMPING_ON),
                                waterOpenTime = o.optString("waterOpenTime"),
                                waterDurationHours = o.optString("waterDurationHours"),
                                waterMaintenanceNote = o.optString("waterMaintenanceNote")
                            )
                        )
                    }
                }
            }

            val markers = mutableListOf<ZummarLandmarkMarker>()
            root.optJSONArray("markers")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    markers.add(
                        ZummarLandmarkMarker(
                            id = o.optString("id"),
                            title = o.optString("title"),
                            subtitleOrOwner = o.optString("subtitleOrOwner"),
                            type = runCatching { ZummarMarkerType.valueOf(o.optString("type")) }.getOrDefault(ZummarMarkerType.SHOP),
                            mapX = o.optDouble("mapX", 0.5).toFloat(),
                            mapY = o.optDouble("mapY", 0.5).toFloat(),
                            workingHours = o.optString("workingHours"),
                            phoneOrUsername = o.optString("phoneOrUsername"),
                            isApproved = o.optBoolean("isApproved", true),
                            isOnDuty = o.optBoolean("isOnDuty", false),
                            amperePriceIqd = o.optString("amperePriceIqd"),
                            specialty = o.optString("specialty")
                        )
                    )
                }
            }

            val products = mutableListOf<ZummarProduct>()
            root.optJSONArray("products")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    products.add(
                        ZummarProduct(
                            id = o.optString("id"),
                            shopMarkerId = o.optString("shopMarkerId"),
                            shopName = o.optString("shopName"),
                            productName = o.optString("productName"),
                            priceIqd = o.optString("priceIqd"),
                            description = o.optString("description"),
                            imageBase64 = o.optString("imageBase64")
                        )
                    )
                }
            }

            val bookings = mutableListOf<ZummarBooking>()
            root.optJSONArray("bookings")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    bookings.add(
                        ZummarBooking(
                            id = o.optString("id"),
                            clinicMarkerId = o.optString("clinicMarkerId"),
                            clinicName = o.optString("clinicName"),
                            doctorSpecialty = o.optString("doctorSpecialty"),
                            patientName = o.optString("patientName"),
                            patientPhone = o.optString("patientPhone"),
                            appointmentDate = o.optString("appointmentDate"),
                            appointmentTime = o.optString("appointmentTime"),
                            notes = o.optString("notes"),
                            bookingCode = o.optString("bookingCode")
                        )
                    )
                }
            }

            val messages = mutableListOf<ZummarChatMessage>()
            root.optJSONArray("messages")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    messages.add(
                        ZummarChatMessage(
                            id = o.optString("id"),
                            targetMarkerId = o.optString("targetMarkerId"),
                            targetTitle = o.optString("targetTitle"),
                            senderName = o.optString("senderName"),
                            senderRole = o.optString("senderRole"),
                            messageText = o.optString("messageText"),
                            attachmentBase64 = o.optString("attachmentBase64"),
                            timestampLabel = o.optString("timestampLabel")
                        )
                    )
                }
            }

            LoadedState(
                users = users,
                zones = zones.ifEmpty { defaultZones() },
                markers = markers.ifEmpty { defaultMarkers() },
                products = products.ifEmpty { defaultProducts() },
                bookings = bookings,
                messages = messages
            )
        } catch (_: Exception) {
            null
        }
    }

    fun loadLocalState(context: Context): LoadedState {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_STATE_JSON, null)
        if (!raw.isNullOrBlank()) {
            parseJsonState(raw)?.let { return it }
        }
        return LoadedState(
            users = defaultUsers(),
            zones = defaultZones(),
            markers = defaultMarkers(),
            products = defaultProducts(),
            bookings = emptyList(),
            messages = emptyList()
        )
    }

    suspend fun fetchRemoteStateIfAvailable(context: Context): LoadedState? = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(FIREBASE_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 6000
                readTimeout = 6000
            }
            if (conn.responseCode in 200..299) {
                val body = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                conn.disconnect()
                if (body.isNotBlank() && body != "null") {
                    val parsed = parseJsonState(body)
                    if (parsed != null) {
                        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                            .edit()
                            .putString(KEY_STATE_JSON, body)
                            .apply()
                        return@withContext parsed
                    }
                }
            }
            conn.disconnect()
            null
        } catch (_: Exception) {
            null
        }
    }
}
