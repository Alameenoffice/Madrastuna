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
    GENERATOR_OWNER("صاحب مولدة أهلية", "تثبيت موقع المولدة وتحديث ساعات التشغيل وسعر الأمبير"),
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
// 1. البوابة المعمارية المهيبة لناحية زمار (4 طبقات فنية + إضاءة ليلية/نهارية + أمواج دجلة)
// ==========================================
@Composable
fun InteractiveOpeningSmartGateCanvas(
    isGateOpen: Boolean,
    onToggleGate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isNightIllumination by remember { mutableStateOf(true) }
    val openFraction by animateFloatAsState(
        targetValue = if (isGateOpen) 1f else 0.08f,
        animationSpec = tween(durationMillis = 1350, easing = FastOutSlowInEasing),
        label = "zummar_gate_open_fraction"
    )
    val infiniteTransition = rememberInfiniteTransition(label = "zummar_gate_masterpiece")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "tigris_wave_phase"
    )
    val lightPulse by infiniteTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1.18f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "zummar_light_pulse"
    )

    val skyColors = if (isNightIllumination) {
        listOf(Color(0xFF041411), Color(0xFF06372A), Color(0xFF064E3B))
    } else {
        listOf(Color(0xFF064E3B), Color(0xFF047857), Color(0xFF0D9488))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(Brush.verticalGradient(colors = skyColors))
            .border(1.8.dp, ZummarAmberGold.copy(alpha = 0.8f), RoundedCornerShape(26.dp))
            .padding(horizontal = 14.dp, vertical = 14.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // شريط أزرار التفاعل المباشر مع صرح بوابة زمار
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.32f),
                    border = BorderStroke(1.dp, ZummarAmberGold.copy(alpha = 0.6f)),
                    modifier = Modifier.clickable { isNightIllumination = !isNightIllumination }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.GOV_DOME, ZummarAmberGold, Modifier.size(14.dp))
                        Text(
                            text = if (isNightIllumination) "إضاءة ليلية ذهبية" else "إشراقة نهار دجلة",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = ZummarAmberGold.copy(alpha = 0.22f),
                    border = BorderStroke(1.dp, ZummarAmberGold),
                    modifier = Modifier.clickable { onToggleGate() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ZummarCustomIcon(ZummarVectorIconType.RESET_VIEW, ZummarAmberGold, Modifier.size(14.dp))
                        Text(
                            text = if (isGateOpen) "إعادة حركة فتح البوابة" else "فتح مصراعي البوابة",
                            fontFamily = ZummarTajawalFont,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = ZummarAmberGold
                        )
                    }
                }
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(186.dp)
                    .clickable { onToggleGate() }
            ) {
                val w = size.width
                val h = size.height
                val cx = w * 0.5f
                val horizonY = h * 0.72f

                // الطبقة 1: الهالة النورانية الذهبية خلف القوس + شبكة الأرضية المنظورية
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            ZummarAmberGold.copy(alpha = (0.45f + 0.25f * openFraction) * lightPulse.coerceAtMost(1.1f)),
                            ZummarSkyCyan.copy(alpha = 0.22f * openFraction),
                            Color.Transparent
                        ),
                        center = Offset(cx, h * 0.48f),
                        radius = w * 0.46f * lightPulse
                    ),
                    center = Offset(cx, h * 0.48f),
                    radius = w * 0.46f * lightPulse
                )

                // خطوط المنظور المعماري على أرضية البوابة
                for (i in -4..4) {
                    val topX = cx + i * (w * 0.045f)
                    val botX = cx + i * (w * 0.12f)
                    drawLine(
                        color = ZummarAmberGold.copy(alpha = 0.16f),
                        start = Offset(topX, horizonY),
                        end = Offset(botX, h),
                        strokeWidth = 1.2.dp.toPx()
                    )
                }

                // الطبقة 2: 4 طبقات متداخلة لأمواج نهر دجلة وسد الموصل مع انعكاس الأنوار
                for (layer in 0..3) {
                    val baseWaveY = horizonY + layer * (h * 0.055f)
                    val amp = (3.5f + layer * 1.4f).dp.toPx()
                    val wavePath = Path().apply {
                        moveTo(w * 0.06f, baseWaveY)
                        val segments = 6
                        val segW = (w * 0.88f) / segments
                        for (s in 0 until segments) {
                            val sx = w * 0.06f + s * segW
                            val ex = sx + segW
                            val phaseShift = wavePhase + layer * 0.9f + s * 0.7f
                            val cy1 = baseWaveY + sin(phaseShift) * amp
                            val cy2 = baseWaveY - cos(phaseShift) * amp
                            cubicTo(sx + segW * 0.35f, cy1, sx + segW * 0.68f, cy2, ex, baseWaveY)
                        }
                    }
                    drawPath(
                        path = wavePath,
                        color = if (layer % 2 == 0) ZummarSkyCyan.copy(alpha = 0.45f + layer * 0.08f)
                        else ZummarCyanLight.copy(alpha = 0.38f + layer * 0.08f),
                        style = Stroke(width = (2.2f + layer * 0.4f).dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // الطبقة 3: البرجان الجانبيان المعماريان بتاجين ذهبيين والقوس الملكي متعدد الطبقات
                val leftTowerX = w * 0.11f
                val rightTowerX = w * 0.77f
                val towerW = w * 0.12f
                val towerTopY = h * 0.16f
                val towerBottomY = h * 0.92f

                val towerBrush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF042F24), Color(0xFF0A5C44), Color(0xFF042F24))
                )

                listOf(leftTowerX, rightTowerX).forEach { tx ->
                    // جسم البرج المعماري
                    drawRoundRect(
                        brush = towerBrush,
                        topLeft = Offset(tx, towerTopY),
                        size = Size(towerW, towerBottomY - towerTopY),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                    drawRoundRect(
                        color = ZummarAmberGold,
                        topLeft = Offset(tx, towerTopY),
                        size = Size(towerW, towerBottomY - towerTopY),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    // التاج الذهبي أعلى البرج (Golden Crown)
                    val crownPath = Path().apply {
                        moveTo(tx - w * 0.01f, towerTopY)
                        lineTo(tx + towerW * 0.2f, towerTopY - h * 0.08f)
                        lineTo(tx + towerW * 0.5f, towerTopY - h * 0.03f)
                        lineTo(tx + towerW * 0.8f, towerTopY - h * 0.08f)
                        lineTo(tx + towerW + w * 0.01f, towerTopY)
                        close()
                    }
                    drawPath(crownPath, color = ZummarAmberGold)

                    // نوافذ البرج المضيئة
                    for (winRow in 0..2) {
                        val wy = towerTopY + h * (0.12f + winRow * 0.19f)
                        drawRoundRect(
                            color = if (isNightIllumination) ZummarAmberGold.copy(alpha = 0.85f) else ZummarCyanLight.copy(alpha = 0.75f),
                            topLeft = Offset(tx + towerW * 0.32f, wy),
                            size = Size(towerW * 0.36f, h * 0.11f),
                            cornerRadius = CornerRadius(6f, 6f)
                        )
                    }
                }

                // القوس الملكي المعماري المتدرج بين البرجين (Outer Arch + Inner Arch + Keystone)
                val archLeft = leftTowerX + towerW
                val archRight = rightTowerX
                val archOuter = Path().apply {
                    moveTo(archLeft, towerBottomY)
                    lineTo(archLeft, h * 0.36f)
                    cubicTo(archLeft, h * 0.10f, cx - w * 0.12f, h * 0.04f, cx, h * 0.02f)
                    cubicTo(cx + w * 0.12f, h * 0.04f, archRight, h * 0.10f, archRight, h * 0.36f)
                    lineTo(archRight, towerBottomY)
                }
                drawPath(
                    path = archOuter,
                    brush = Brush.linearGradient(
                        colors = listOf(ZummarAmberGold, Color(0xFFFFE082), ZummarAmberWarm)
                    ),
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )

                val archInner = Path().apply {
                    moveTo(archLeft + w * 0.02f, towerBottomY)
                    lineTo(archLeft + w * 0.02f, h * 0.38f)
                    cubicTo(archLeft + w * 0.02f, h * 0.16f, cx - w * 0.10f, h * 0.10f, cx, h * 0.08f)
                    cubicTo(cx + w * 0.10f, h * 0.10f, archRight - w * 0.02f, h * 0.16f, archRight - w * 0.02f, h * 0.38f)
                    lineTo(archRight - w * 0.02f, towerBottomY)
                }
                drawPath(
                    path = archInner,
                    color = ZummarAmberGold.copy(alpha = 0.65f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )

                // حجر القمة الذهبي (Keystone) في ذروة القوس الملكي
                val keystone = Path().apply {
                    moveTo(cx - w * 0.03f, h * 0.02f)
                    lineTo(cx + w * 0.03f, h * 0.02f)
                    lineTo(cx + w * 0.02f, h * 0.11f)
                    lineTo(cx - w * 0.02f, h * 0.11f)
                    close()
                }
                drawPath(keystone, color = ZummarAmberGold)

                // الطبقة 4: البوابتان الذهبيتان المتحركتان (Double Golden Doors) بالمنظور الهندسي
                val doorAreaLeft = archLeft + w * 0.025f
                val doorAreaRight = archRight - w * 0.025f
                val halfSpan = (doorAreaRight - doorAreaLeft) * 0.5f
                val currentLeafSpan = halfSpan * (1f - openFraction * 0.78f)
                val perspectiveLift = openFraction * (h * 0.05f)

                // المصراع الأيسر
                val leftLeaf = Path().apply {
                    moveTo(doorAreaLeft, towerBottomY)
                    lineTo(doorAreaLeft, h * 0.36f)
                    cubicTo(
                        doorAreaLeft + currentLeafSpan * 0.4f,
                        h * 0.22f - perspectiveLift,
                        doorAreaLeft + currentLeafSpan * 0.8f,
                        h * 0.18f - perspectiveLift,
                        doorAreaLeft + currentLeafSpan,
                        h * 0.24f - perspectiveLift
                    )
                    lineTo(doorAreaLeft + currentLeafSpan, towerBottomY - perspectiveLift)
                    close()
                }
                drawPath(
                    path = leftLeaf,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF043927), Color(0xFF0A6847), Color(0xFF064E3B))
                    )
                )
                drawPath(leftLeaf, color = ZummarAmberGold, style = Stroke(width = 2.2.dp.toPx()))

                // المصراع الأيمن
                val rightLeaf = Path().apply {
                    moveTo(doorAreaRight, towerBottomY)
                    lineTo(doorAreaRight, h * 0.36f)
                    cubicTo(
                        doorAreaRight - currentLeafSpan * 0.4f,
                        h * 0.22f - perspectiveLift,
                        doorAreaRight - currentLeafSpan * 0.8f,
                        h * 0.18f - perspectiveLift,
                        doorAreaRight - currentLeafSpan,
                        h * 0.24f - perspectiveLift
                    )
                    lineTo(doorAreaRight - currentLeafSpan, towerBottomY - perspectiveLift)
                    close()
                }
                drawPath(
                    path = rightLeaf,
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color(0xFF064E3B), Color(0xFF0A6847), Color(0xFF043927))
                    )
                )
                drawPath(rightLeaf, color = ZummarAmberGold, style = Stroke(width = 2.2.dp.toPx()))

                // حلقات ومقابض البوابة الذهبية المزدوجة
                drawCircle(
                    color = ZummarAmberGold,
                    radius = 5.dp.toPx(),
                    center = Offset(doorAreaLeft + currentLeafSpan * 0.76f, h * 0.58f - perspectiveLift * 0.5f)
                )
                drawCircle(
                    color = ZummarAmberGold,
                    radius = 5.dp.toPx(),
                    center = Offset(doorAreaRight - currentLeafSpan * 0.76f, h * 0.58f - perspectiveLift * 0.5f)
                )
            }

            Text(
                text = "أهلاً بك في بوابة زمار الذكية — صرح الخدمات الرقمية المتكاملة لأهلنا في ناحية زمار",
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
// 2. واجهة المتجر التفاعلية ثلاثية العمق التي تنفتح أبوابها عند النقر على المحل
// ==========================================
@Composable
fun InteractiveOpeningStorefrontCanvas(
    shopName: String,
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    val doorOpenProgress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "storefront_door_progress"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(ZummarNavy, Color(0xFF063F30), ZummarEmerald)
                )
            )
            .border(1.4.dp, ZummarAmberGold.copy(alpha = 0.75f), RoundedCornerShape(22.dp))
            .padding(14.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
        ) {
            val w = size.width
            val h = size.height

            // إفريز السقف المعماري للمتجر
            drawRoundRect(
                color = ZummarAmberGold,
                topLeft = Offset(w * 0.10f, h * 0.04f),
                size = Size(w * 0.80f, h * 0.08f),
                cornerRadius = CornerRadius(8f, 8f)
            )

            // المظلة التجارية المقلمة بأقواس متموجة (Striped Awning)
            val stripeCount = 6
            val awningLeft = w * 0.12f
            val awningWidth = w * 0.76f
            val stripeW = awningWidth / stripeCount
            for (i in 0 until stripeCount) {
                val sx = awningLeft + i * stripeW
                val stripePath = Path().apply {
                    moveTo(sx, h * 0.12f)
                    lineTo(sx + stripeW, h * 0.12f)
                    lineTo(sx + stripeW, h * 0.34f)
                    cubicTo(
                        sx + stripeW * 0.75f, h * 0.42f,
                        sx + stripeW * 0.25f, h * 0.42f,
                        sx, h * 0.34f
                    )
                    close()
                }
                drawPath(
                    path = stripePath,
                    color = if (i % 2 == 0) ZummarAmberGold else Color(0xFF047857)
                )
            }

            // عمق المتجر الداخلي ورفوف العرض المضاءة
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFF8E1).copy(alpha = 0.35f + 0.55f * doorOpenProgress),
                        Color(0xFFFFECB3).copy(alpha = 0.25f + 0.45f * doorOpenProgress)
                    )
                ),
                topLeft = Offset(w * 0.18f, h * 0.36f),
                size = Size(w * 0.64f, h * 0.58f),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // رفوف البضائع داخل المتجر
            for (shelfIdx in 1..2) {
                val sy = h * (0.48f + shelfIdx * 0.16f)
                drawLine(
                    color = ZummarAmberWarm.copy(alpha = 0.55f * doorOpenProgress),
                    start = Offset(w * 0.24f, sy),
                    end = Offset(w * 0.76f, sy),
                    strokeWidth = 2.5.dp.toPx()
                )
            }

            // الأبواب الزجاجية المزدوجة التي تنفتح للجانبين
            val maxLeafW = w * 0.31f
            val currentLeafW = maxLeafW * (1f - 0.76f * doorOpenProgress)

            // الباب الأيسر
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF064E3B), Color(0xFF0D9488))
                ),
                topLeft = Offset(w * 0.18f, h * 0.36f),
                size = Size(currentLeafW, h * 0.58f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = ZummarAmberGold,
                topLeft = Offset(w * 0.18f, h * 0.36f),
                size = Size(currentLeafW, h * 0.58f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.8.dp.toPx())
            )

            // الباب الأيمن
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0D9488), Color(0xFF064E3B))
                ),
                topLeft = Offset(w * 0.82f - currentLeafW, h * 0.36f),
                size = Size(currentLeafW, h * 0.58f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawRoundRect(
                color = ZummarAmberGold,
                topLeft = Offset(w * 0.82f - currentLeafW, h * 0.36f),
                size = Size(currentLeafW, h * 0.58f),
                cornerRadius = CornerRadius(6f, 6f),
                style = Stroke(width = 1.8.dp.toPx())
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
// 2-B. اللوحات الفنية الهندسية المرسومة للقطاعات الأربعة (بدون إيموجي نصي)
// ==========================================
@Composable
fun ZummarElectricitySectorArtCanvas(modifier: Modifier = Modifier.size(64.dp)) {
    val infinite = rememberInfiniteTransition(label = "elec_sector_art")
    val pulse by infinite.animateFloat(
        initialValue = 0.7f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "elec_pulse"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(ZummarAmberGold.copy(alpha = 0.42f), Color.Transparent),
                center = Offset(cx, cy),
                radius = w * 0.48f * pulse
            ),
            radius = w * 0.48f * pulse,
            center = Offset(cx, cy)
        )

        // برج الطاقة الشبكي
        val tower = Path().apply {
            moveTo(cx, h * 0.10f)
            lineTo(w * 0.74f, h * 0.88f)
            lineTo(w * 0.26f, h * 0.88f)
            close()
        }
        drawPath(tower, color = ZummarAmberGold.copy(alpha = 0.18f))
        drawPath(tower, color = ZummarAmberWarm, style = Stroke(width = 2.2.dp.toPx(), join = StrokeJoin.Round))
        drawLine(ZummarAmberWarm, Offset(w * 0.22f, h * 0.34f), Offset(w * 0.78f, h * 0.34f), strokeWidth = 2.2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(ZummarAmberWarm, Offset(w * 0.28f, h * 0.54f), Offset(w * 0.72f, h * 0.54f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)

        // صاعقة الطاقة الذهبية وسط البرج
        val bolt = Path().apply {
            moveTo(cx + w * 0.06f, h * 0.18f)
            lineTo(cx - w * 0.12f, h * 0.50f)
            lineTo(cx + w * 0.02f, h * 0.50f)
            lineTo(cx - w * 0.06f, h * 0.82f)
            lineTo(cx + w * 0.14f, h * 0.46f)
            lineTo(cx, h * 0.46f)
            close()
        }
        drawPath(bolt, brush = Brush.verticalGradient(listOf( Color(0xFFFFF59D), ZummarAmberGold, ZummarAmberWarm)))
    }
}

@Composable
fun ZummarWaterSectorArtCanvas(modifier: Modifier = Modifier.size(64.dp)) {
    val infinite = rememberInfiniteTransition(label = "water_sector_art")
    val waveShift by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "water_shift"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dropPath = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.86f, h * 0.46f, w * 0.84f, h * 0.86f, w * 0.5f, h * 0.92f)
            cubicTo(w * 0.16f, h * 0.86f, w * 0.14f, h * 0.46f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(
            path = dropPath,
            brush = Brush.verticalGradient(
                colors = listOf(ZummarCyanLight, ZummarSkyCyan, Color(0xFF0284C7))
            )
        )
        clipPath(dropPath) {
            val wave = Path().apply {
                val baseY = h * 0.62f
                moveTo(0f, baseY)
                cubicTo(
                    w * 0.33f, baseY + sin(waveShift) * (h * 0.09f),
                    w * 0.66f, baseY - cos(waveShift) * (h * 0.09f),
                    w, baseY
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(wave, color = Color(0xFF0369A1).copy(alpha = 0.65f))
        }
        // لمعة بلورية داخل قطرة الماء
        val highlight = Path().apply {
            moveTo(w * 0.36f, h * 0.30f)
            cubicTo(w * 0.28f, h * 0.46f, w * 0.28f, h * 0.62f, w * 0.38f, h * 0.72f)
        }
        drawPath(highlight, color = Color.White.copy(alpha = 0.75f), style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
fun ZummarHealthEcgSectorArtCanvas(modifier: Modifier = Modifier.size(64.dp)) {
    val infinite = rememberInfiniteTransition(label = "health_sector_art")
    val radarScale by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "health_radar"
    )
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        drawCircle(
            color = ZummarEmeraldLight.copy(alpha = 0.22f),
            radius = w * 0.46f * radarScale,
            center = Offset(w * 0.5f, h * 0.5f)
        )
        val shield = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            cubicTo(w * 0.84f, h * 0.14f, w * 0.88f, h * 0.26f, w * 0.86f, h * 0.54f)
            cubicTo(w * 0.82f, h * 0.76f, w * 0.64f, h * 0.88f, w * 0.5f, h * 0.94f)
            cubicTo(w * 0.36f, h * 0.88f, w * 0.18f, h * 0.76f, w * 0.14f, h * 0.54f)
            cubicTo(w * 0.12f, h * 0.26f, w * 0.16f, h * 0.14f, w * 0.5f, h * 0.08f)
            close()
        }
        drawPath(
            path = shield,
            brush = Brush.verticalGradient(listOf(ZummarEmeraldLight, ZummarEmerald))
        )
        drawPath(shield, color = ZummarAmberGold, style = Stroke(width = 2.dp.toPx()))

        // تخطيط نبض القلب الحي (ECG)
        val ecg = Path().apply {
            moveTo(w * 0.22f, h * 0.52f)
            lineTo(w * 0.36f, h * 0.52f)
            lineTo(w * 0.43f, h * 0.30f)
            lineTo(w * 0.51f, h * 0.72f)
            lineTo(w * 0.58f, h * 0.42f)
            lineTo(w * 0.64f, h * 0.52f)
            lineTo(w * 0.78f, h * 0.52f)
        }
        drawPath(ecg, color = Color.White, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun ZummarMarketStoreSectorArtCanvas(modifier: Modifier = Modifier.size(64.dp)) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // واجهة متجر مصغرة بمظلة ذهبية/زمردية وأبواب مفتوحة
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(ZummarNavy, ZummarEmerald)),
            topLeft = Offset(w * 0.14f, h * 0.32f),
            size = Size(w * 0.72f, h * 0.58f),
            cornerRadius = CornerRadius(10f, 10f)
        )
        val awning = Path().apply {
            moveTo(w * 0.08f, h * 0.36f)
            lineTo(w * 0.18f, h * 0.14f)
            lineTo(w * 0.82f, h * 0.14f)
            lineTo(w * 0.92f, h * 0.36f)
            cubicTo(w * 0.76f, h * 0.46f, w * 0.62f, h * 0.46f, w * 0.50f, h * 0.36f)
            cubicTo(w * 0.38f, h * 0.46f, w * 0.24f, h * 0.46f, w * 0.08f, h * 0.36f)
            close()
        }
        drawPath(awning, brush = Brush.horizontalGradient(listOf(ZummarAmberGold, ZummarAmberWarm, ZummarAmberGold)))
        drawRoundRect(
            color = Color(0xFFFFF8E1),
            topLeft = Offset(w * 0.32f, h * 0.46f),
            size = Size(w * 0.36f, h * 0.44f),
            cornerRadius = CornerRadius(6f, 6f)
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
    private const val PREFS_NAME = "zummar_smart_gate_storage_v2_clean"
    private const val KEY_STATE_JSON = "zummar_full_state_json_v2"
    private const val FIREBASE_URL = "https://madrasati-app-d51dd-default-rtdb.firebaseio.com/zummar_smart_gate_v2_clean.json"

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

    fun defaultZones(): List<ZummarNeighborhoodZone> = emptyList()

    fun defaultMarkers(): List<ZummarLandmarkMarker> = emptyList()

    fun defaultProducts(): List<ZummarProduct> = emptyList()

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
