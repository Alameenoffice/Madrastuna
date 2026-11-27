package com.example.ui.components

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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private val CLEAN_ISO_DATE_WITH_TIME_REGEX = Regex("^(\\d{4}-\\d{2}-\\d{2})\\s+(.+)$")

/**
 * Formats compact or raw date/time strings via [formatCleanDate] and ensures
 * any trailing time segment is cleanly separated by " • " (e.g., "2026-11-15 • 04:43 ص").
 */
fun formatCleanDateTime(raw: String): String {
    val cleaned = formatCleanDate(raw)
    if (cleaned.isBlank()) return ""
    if (cleaned.contains("•")) return cleaned
    val match = CLEAN_ISO_DATE_WITH_TIME_REGEX.matchEntire(cleaned) ?: return cleaned
    val datePart = match.groupValues[1]
    val timePart = match.groupValues[2].trim().trimStart('-', '|', '•').trim()
    return if (timePart.isEmpty()) datePart else "$datePart • $timePart"
}

/**
 * Refined rank badge specification for SmartWelcomeProfileAvatar in Honor Roll & Teacher Evaluation:
 * #1 = Warm Royal Gold, #2 = Crisp Silver, #3 = Warm Bronze, #4+ = Platform Emerald.
 */
fun rankAvatarBadgeSpec(rank: Int, customLabel: String = "#$rank"): DailyStatusBadgeSpec {
    return when (rank) {
        1 -> DailyStatusBadgeSpec(
            label = customLabel,
            ringColors = listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706), Color(0xFFFEF08A)),
            pillBackground = Color(0xFFB45309),
            pillBorder = Color(0xFFFEF08A),
            pillTextColor = Color(0xFFFFFBEB),
            dotColor = null,
            pulseDot = false
        )
        2 -> DailyStatusBadgeSpec(
            label = customLabel,
            ringColors = listOf(Color(0xFFF1F5F9), Color(0xFF94A3B8), Color(0xFFCBD5E1), Color(0xFFF1F5F9)),
            pillBackground = Color(0xFF475569),
            pillBorder = Color(0xFFE2E8F0),
            pillTextColor = Color.White,
            dotColor = null,
            pulseDot = false
        )
        3 -> DailyStatusBadgeSpec(
            label = customLabel,
            ringColors = listOf(Color(0xFFFED7AA), Color(0xFFEA580C), Color(0xFFC2410C), Color(0xFFFED7AA)),
            pillBackground = Color(0xFF9A3412),
            pillBorder = Color(0xFFFDBA74),
            pillTextColor = Color(0xFFFFF7ED),
            dotColor = null,
            pulseDot = false
        )
        else -> DailyStatusBadgeSpec(
            label = customLabel,
            ringColors = listOf(Color(0xFF6EE7B7), Color(0xFF059669), Color(0xFFFDE68A), Color(0xFF34D399)),
            pillBackground = Color(0xFF065F46),
            pillBorder = Color(0xFF6EE7B7),
            pillTextColor = Color(0xFFECFDF5),
            dotColor = null,
            pulseDot = false
        )
    }
}

enum class DynamicSpotlightIconKind {
    // 1. Honor Roll (لوحة الشرف)
    HONOR_CROWN_TROPHY,
    HONOR_EMERALD_SHIELD,
    HONOR_LAUREL_MEDAL,
    // 2. Teacher Evaluation (تقييم الكادر التدريسي)
    TEACHER_EXCELLENCE_MEDAL,
    TEACHER_GOLDEN_STARS,
    TEACHER_TORCH_WREATH,
    // 3. Reports & Messaging (البلاغات والتواصل)
    REPORTS_DIALOGUE_BUBBLES,
    REPORTS_OFFICIAL_ENVELOPE,
    REPORTS_CARE_SHIELD,
    // 4. Events & News Gallery (معرض الفعاليات والأخبار)
    EVENTS_DOCUMENT_LENS,
    EVENTS_GOLDEN_SPARK,
    EVENTS_NEWS_WAVES
}

data class DynamicSectionSpotlightScene(
    val title: String,
    val subtitle: String,
    val iconKind: DynamicSpotlightIconKind
)

/**
 * الوحدة البصرية الزجاجية الديناميكية الموحّدة للترويسات الزمردية:
 * مطابقة لروح وسلاسة بطاقة "مجتمع ونشاطات الطلبة" — تبدّل كل 2.6 ثانية بانزلاق عمودي ناعم وتلاشي
 * عبر AnimatedContent (fadeIn + slideInVertically مع tween(500, FastOutSlowInEasing))
 * مع 3 شرائط مؤشر تتمدّد بانسيابية وهالة زمردية/ذهبية هادئة وأيقونات متجهة نظيفة بـ Canvas.
 */
@Composable
fun DynamicSectionSpotlightCard(
    scenes: List<DynamicSectionSpotlightScene>,
    modifier: Modifier = Modifier,
    testTagName: String = "dynamic_section_spotlight_card"
) {
    if (scenes.isEmpty()) return

    var activeSceneIndex by remember(scenes) { mutableIntStateOf(0) }
    LaunchedEffect(scenes) {
        while (true) {
            delay(2600L)
            activeSceneIndex = (activeSceneIndex + 1) % scenes.size
        }
    }

    val auraTransition = rememberInfiniteTransition(label = "section_spotlight_aura")
    val auraPulse by auraTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "section_spotlight_aura_pulse"
    )

    Surface(
        modifier = modifier
            .width(148.dp)
            .height(116.dp)
            .testTag(testTagName),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.30f))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Calm emerald & warm-gold backdrop halo
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFEF08A).copy(alpha = auraPulse),
                            Color(0xFF34D399).copy(alpha = auraPulse * 0.72f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.30f),
                        radius = size.width * 0.64f
                    ),
                    radius = size.width * 0.64f,
                    center = Offset(size.width * 0.5f, size.height * 0.30f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 9.dp, vertical = 9.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = scenes[activeSceneIndex.coerceIn(scenes.indices)],
                    transitionSpec = {
                        (fadeIn(tween(500, easing = FastOutSlowInEasing)) +
                            slideInVertically(tween(500, easing = FastOutSlowInEasing)) { it / 3 }) togetherWith
                            (fadeOut(tween(420, easing = FastOutSlowInEasing)) +
                                slideOutVertically(tween(420, easing = FastOutSlowInEasing)) { -it / 3 })
                    },
                    label = "section_spotlight_scene_anim",
                    modifier = Modifier.weight(1f)
                ) { scene ->
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (scene.iconKind == DynamicSpotlightIconKind.TEACHER_EXCELLENCE_MEDAL) {
                            EliteTeacherExcellenceMedalCanvas(
                                modifier = Modifier.size(38.dp)
                            )
                        } else {
                            DynamicSpotlightCanvasIcon(
                                kind = scene.iconKind,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = scene.title,
                            fontSize = 12.2.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = scene.subtitle,
                            fontSize = 9.2.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFD1FAE5),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // 3 Smoothly Expanding Indicator Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    scenes.indices.forEach { idx ->
                        val isSelected = idx == activeSceneIndex
                        val pillWidth by animateDpAsState(
                            targetValue = if (isSelected) 18.dp else 6.dp,
                            animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing),
                            label = "section_spotlight_ind_$idx"
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
fun HonorRollSpotlightCard(modifier: Modifier = Modifier) {
    val scenes = remember {
        listOf(
            DynamicSectionSpotlightScene(
                title = "تفوّق وتميّز",
                subtitle = "نحتفي بأبطال العلم والاجتهاد",
                iconKind = DynamicSpotlightIconKind.HONOR_CROWN_TROPHY
            ),
            DynamicSectionSpotlightScene(
                title = "التزام وقدوة",
                subtitle = "سلوك راقٍ وحضورٌ مثالي",
                iconKind = DynamicSpotlightIconKind.HONOR_EMERALD_SHIELD
            ),
            DynamicSectionSpotlightScene(
                title = "كوكبة الأوائل",
                subtitle = "فخر المدرسة وأولياء الأمور",
                iconKind = DynamicSpotlightIconKind.HONOR_LAUREL_MEDAL
            )
        )
    }
    DynamicSectionSpotlightCard(
        scenes = scenes,
        modifier = modifier,
        testTagName = "honor_roll_spotlight_card"
    )
}

@Composable
fun TeacherEvaluationSpotlightCard(modifier: Modifier = Modifier) {
    val scenes = remember {
        listOf(
            DynamicSectionSpotlightScene(
                title = "عطاءٌ يُقدَّر",
                subtitle = "تكريم لجهود كادرنا التربوي",
                iconKind = DynamicSpotlightIconKind.TEACHER_EXCELLENCE_MEDAL
            ),
            DynamicSectionSpotlightScene(
                title = "تقييمٌ موثوق",
                subtitle = "شراكة شفافة مع أولياء الأمور",
                iconKind = DynamicSpotlightIconKind.TEACHER_GOLDEN_STARS
            ),
            DynamicSectionSpotlightScene(
                title = "إلهامٌ وتميّز",
                subtitle = "صنّاع أجيال المستقبل",
                iconKind = DynamicSpotlightIconKind.TEACHER_TORCH_WREATH
            )
        )
    }
    DynamicSectionSpotlightCard(
        scenes = scenes,
        modifier = modifier,
        testTagName = "teacher_evaluation_spotlight_card"
    )
}

@Composable
fun ReportsAndChatSpotlightCard(modifier: Modifier = Modifier) {
    val scenes = remember {
        listOf(
            DynamicSectionSpotlightScene(
                title = "تواصل مباشر",
                subtitle = "جسر موثوق بين البيت والمدرسة",
                iconKind = DynamicSpotlightIconKind.REPORTS_DIALOGUE_BUBBLES
            ),
            DynamicSectionSpotlightScene(
                title = "إشعارات دقيقة",
                subtitle = "متابعة رسمية فورية لكل بلاغ",
                iconKind = DynamicSpotlightIconKind.REPORTS_OFFICIAL_ENVELOPE
            ),
            DynamicSectionSpotlightScene(
                title = "شراكة تربوية",
                subtitle = "معاً لرعاية مسيرة أبنائنا",
                iconKind = DynamicSpotlightIconKind.REPORTS_CARE_SHIELD
            )
        )
    }
    DynamicSectionSpotlightCard(
        scenes = scenes,
        modifier = modifier,
        testTagName = "reports_chat_spotlight_card"
    )
}

@Composable
fun EventsAndNewsSpotlightCard(modifier: Modifier = Modifier) {
    val scenes = remember {
        listOf(
            DynamicSectionSpotlightScene(
                title = "لحظاتٌ تُوثَّق",
                subtitle = "أجمل المحطات والأنشطة المدرسية",
                iconKind = DynamicSpotlightIconKind.EVENTS_DOCUMENT_LENS
            ),
            DynamicSectionSpotlightScene(
                title = "إنجازاتٌ تُروى",
                subtitle = "فعاليات علمية وثقافية ورياضية",
                iconKind = DynamicSpotlightIconKind.EVENTS_GOLDEN_SPARK
            ),
            DynamicSectionSpotlightScene(
                title = "نبض المدرسة",
                subtitle = "أخبار رسمية وتغطيات متجددة",
                iconKind = DynamicSpotlightIconKind.EVENTS_NEWS_WAVES
            )
        )
    }
    DynamicSectionSpotlightCard(
        scenes = scenes,
        modifier = modifier,
        testTagName = "events_news_spotlight_card"
    )
}

@Composable
private fun DynamicSpotlightCanvasIcon(
    kind: DynamicSpotlightIconKind,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val strokeW = 1.9.dp.toPx()
        val goldLight = Color(0xFFFEF08A)
        val goldMid = Color(0xFFF59E0B)
        val mintBright = Color(0xFF6EE7B7)

        when (kind) {
            DynamicSpotlightIconKind.HONOR_CROWN_TROPHY -> {
                // Geometric Golden Crown / Trophy Emblem
                val crown = Path().apply {
                    moveTo(w * 0.16f, h * 0.68f)
                    lineTo(w * 0.22f, h * 0.28f)
                    lineTo(w * 0.38f, h * 0.48f)
                    lineTo(cx, h * 0.18f)
                    lineTo(w * 0.62f, h * 0.48f)
                    lineTo(w * 0.78f, h * 0.28f)
                    lineTo(w * 0.84f, h * 0.68f)
                    close()
                }
                drawPath(
                    path = crown,
                    brush = Brush.verticalGradient(listOf(goldLight.copy(alpha = 0.32f), goldMid.copy(alpha = 0.18f)))
                )
                drawPath(
                    path = crown,
                    brush = Brush.linearGradient(listOf(goldLight, goldMid)),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawLine(
                    color = mintBright,
                    start = Offset(w * 0.20f, h * 0.80f),
                    end = Offset(w * 0.80f, h * 0.80f),
                    strokeWidth = strokeW,
                    cap = StrokeCap.Round
                )
                drawCircle(color = goldLight, radius = 2.2.dp.toPx(), center = Offset(cx, h * 0.16f))
            }

            DynamicSpotlightIconKind.HONOR_EMERALD_SHIELD -> {
                // Emerald & Gold Commitment Shield with Check
                val shield = Path().apply {
                    moveTo(cx, h * 0.10f)
                    lineTo(w * 0.84f, h * 0.24f)
                    lineTo(w * 0.84f, h * 0.52f)
                    cubicTo(w * 0.84f, h * 0.74f, w * 0.66f, h * 0.86f, cx, h * 0.92f)
                    cubicTo(w * 0.34f, h * 0.86f, w * 0.16f, h * 0.74f, w * 0.16f, h * 0.52f)
                    lineTo(w * 0.16f, h * 0.24f)
                    close()
                }
                drawPath(
                    path = shield,
                    brush = Brush.verticalGradient(listOf(mintBright.copy(alpha = 0.26f), Color(0xFF065F46).copy(alpha = 0.45f)))
                )
                drawPath(
                    path = shield,
                    color = mintBright,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                val check = Path().apply {
                    moveTo(w * 0.35f, h * 0.52f)
                    lineTo(w * 0.47f, h * 0.64f)
                    lineTo(w * 0.68f, h * 0.40f)
                }
                drawPath(
                    path = check,
                    color = goldLight,
                    style = Stroke(width = strokeW * 1.05f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            DynamicSpotlightIconKind.HONOR_LAUREL_MEDAL,
            DynamicSpotlightIconKind.TEACHER_EXCELLENCE_MEDAL -> {
                // Glowing Laurel Medal with 5-Point Star
                val r = w * 0.34f
                drawArc(
                    color = goldLight,
                    startAngle = 128f,
                    sweepAngle = 104f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.43f, cy - h * 0.43f),
                    size = Size(w * 0.86f, h * 0.86f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = goldLight,
                    startAngle = -52f,
                    sweepAngle = 104f,
                    useCenter = false,
                    topLeft = Offset(cx - w * 0.43f, cy - h * 0.43f),
                    size = Size(w * 0.86f, h * 0.86f),
                    style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = r,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = mintBright,
                    radius = r,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawStarPath(cx = cx, cy = cy, outerR = r * 0.62f, brush = Brush.verticalGradient(listOf(goldLight, goldMid)))
            }

            DynamicSpotlightIconKind.TEACHER_GOLDEN_STARS -> {
                // Balanced Geometric Golden Rating Stars (Center big + 2 flanking)
                drawStarPath(
                    cx = cx,
                    cy = h * 0.40f,
                    outerR = w * 0.24f,
                    brush = Brush.verticalGradient(listOf(goldLight, goldMid))
                )
                drawStarPath(
                    cx = w * 0.22f,
                    cy = h * 0.62f,
                    outerR = w * 0.13f,
                    brush = Brush.verticalGradient(listOf(mintBright, Color(0xFF10B981)))
                )
                drawStarPath(
                    cx = w * 0.78f,
                    cy = h * 0.62f,
                    outerR = w * 0.13f,
                    brush = Brush.verticalGradient(listOf(mintBright, Color(0xFF10B981)))
                )
                drawLine(
                    color = goldLight.copy(alpha = 0.75f),
                    start = Offset(w * 0.26f, h * 0.84f),
                    end = Offset(w * 0.74f, h * 0.84f),
                    strokeWidth = 1.7.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            DynamicSpotlightIconKind.TEACHER_TORCH_WREATH -> {
                // Calm Knowledge Torch & Excellence Arc
                val flame = Path().apply {
                    moveTo(cx, h * 0.10f)
                    cubicTo(w * 0.66f, h * 0.26f, w * 0.66f, h * 0.44f, cx, h * 0.48f)
                    cubicTo(w * 0.34f, h * 0.44f, w * 0.34f, h * 0.26f, cx, h * 0.10f)
                    close()
                }
                drawPath(
                    path = flame,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), goldLight, goldMid))
                )
                // Torch cup & stem
                val handle = Path().apply {
                    moveTo(w * 0.38f, h * 0.52f)
                    lineTo(w * 0.62f, h * 0.52f)
                    lineTo(w * 0.55f, h * 0.86f)
                    lineTo(w * 0.45f, h * 0.86f)
                    close()
                }
                drawPath(path = handle, color = mintBright.copy(alpha = 0.28f))
                drawPath(
                    path = handle,
                    color = mintBright,
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )
                // Subtle wreath arcs around torch
                drawArc(
                    color = goldLight.copy(alpha = 0.70f),
                    startAngle = 140f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.72f, h * 0.68f),
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = goldLight.copy(alpha = 0.70f),
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.72f, h * 0.68f),
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )
            }

            DynamicSpotlightIconKind.REPORTS_DIALOGUE_BUBBLES -> {
                // Two Harmonious Geometric Dialogue Bubbles
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = mintBright,
                    topLeft = Offset(w * 0.10f, h * 0.16f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = strokeW * 0.9f)
                )
                drawRoundRect(
                    color = Color(0xFF064E3B).copy(alpha = 0.70f),
                    topLeft = Offset(w * 0.36f, h * 0.42f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = goldLight,
                    topLeft = Offset(w * 0.36f, h * 0.42f),
                    size = Size(w * 0.54f, h * 0.40f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = strokeW * 0.95f)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.90f),
                    start = Offset(w * 0.48f, h * 0.58f),
                    end = Offset(w * 0.78f, h * 0.58f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = goldLight.copy(alpha = 0.85f),
                    start = Offset(w * 0.54f, h * 0.68f),
                    end = Offset(w * 0.78f, h * 0.68f),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            DynamicSpotlightIconKind.REPORTS_OFFICIAL_ENVELOPE -> {
                // Polished Official Notification Envelope + Seal Dot
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.76f, h * 0.54f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    color = goldLight,
                    topLeft = Offset(w * 0.12f, h * 0.24f),
                    size = Size(w * 0.76f, h * 0.54f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = strokeW)
                )
                val flap = Path().apply {
                    moveTo(w * 0.15f, h * 0.28f)
                    lineTo(cx, h * 0.56f)
                    lineTo(w * 0.85f, h * 0.28f)
                }
                drawPath(
                    path = flap,
                    color = mintBright,
                    style = Stroke(width = strokeW * 0.92f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                drawCircle(
                    color = goldLight,
                    radius = 3.2.dp.toPx(),
                    center = Offset(w * 0.82f, h * 0.22f)
                )
            }

            DynamicSpotlightIconKind.REPORTS_CARE_SHIELD -> {
                // Educational Care & Integration Shield with Inner Heart/Star Accent
                val shield = Path().apply {
                    moveTo(cx, h * 0.10f)
                    lineTo(w * 0.84f, h * 0.24f)
                    lineTo(w * 0.84f, h * 0.52f)
                    cubicTo(w * 0.84f, h * 0.74f, w * 0.66f, h * 0.86f, cx, h * 0.92f)
                    cubicTo(w * 0.34f, h * 0.86f, w * 0.16f, h * 0.74f, w * 0.16f, h * 0.52f)
                    lineTo(w * 0.16f, h * 0.24f)
                    close()
                }
                drawPath(path = shield, color = Color.White.copy(alpha = 0.13f))
                drawPath(
                    path = shield,
                    color = goldLight,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
                // Two connected partnership arcs inside shield
                drawCircle(
                    color = mintBright,
                    radius = w * 0.11f,
                    center = Offset(cx - w * 0.08f, cy + h * 0.02f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawCircle(
                    color = goldLight,
                    radius = w * 0.11f,
                    center = Offset(cx + w * 0.08f, cy + h * 0.02f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }

            DynamicSpotlightIconKind.EVENTS_DOCUMENT_LENS -> {
                // Polished Documentation Frame & Camera Aperture Lens
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.14f),
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = mintBright,
                    topLeft = Offset(w * 0.12f, h * 0.22f),
                    size = Size(w * 0.76f, h * 0.58f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = goldLight,
                    radius = w * 0.17f,
                    center = Offset(cx, h * 0.51f),
                    style = Stroke(width = strokeW)
                )
                drawCircle(
                    color = goldLight.copy(alpha = 0.85f),
                    radius = w * 0.06f,
                    center = Offset(cx, h * 0.51f)
                )
                drawCircle(
                    color = goldLight,
                    radius = 2.dp.toPx(),
                    center = Offset(w * 0.74f, h * 0.34f)
                )
            }

            DynamicSpotlightIconKind.EVENTS_GOLDEN_SPARK -> {
                // Golden Event Star / Spark with Delicate Halo
                drawCircle(
                    color = mintBright.copy(alpha = 0.45f),
                    radius = w * 0.38f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                val sparkR = w * 0.32f
                val spark = Path().apply {
                    moveTo(cx, cy - sparkR)
                    lineTo(cx + sparkR * 0.28f, cy - sparkR * 0.28f)
                    lineTo(cx + sparkR, cy)
                    lineTo(cx + sparkR * 0.28f, cy + sparkR * 0.28f)
                    lineTo(cx, cy + sparkR)
                    lineTo(cx - sparkR * 0.28f, cy + sparkR * 0.28f)
                    lineTo(cx - sparkR, cy)
                    lineTo(cx - sparkR * 0.28f, cy - sparkR * 0.28f)
                    close()
                }
                drawPath(
                    path = spark,
                    brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), goldLight, goldMid))
                )
                drawCircle(color = mintBright, radius = 2.2.dp.toPx(), center = Offset(w * 0.80f, h * 0.22f))
                drawCircle(color = goldLight, radius = 1.8.dp.toPx(), center = Offset(w * 0.20f, h * 0.78f))
            }

            DynamicSpotlightIconKind.EVENTS_NEWS_WAVES -> {
                // Calm Official News Broadcast Waves
                drawCircle(
                    color = goldLight,
                    radius = w * 0.11f,
                    center = Offset(w * 0.26f, h * 0.74f)
                )
                drawArc(
                    color = mintBright,
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.26f - w * 0.28f, h * 0.74f - w * 0.28f),
                    size = Size(w * 0.56f, w * 0.56f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = goldLight,
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.26f - w * 0.48f, h * 0.74f - w * 0.48f),
                    size = Size(w * 0.96f, w * 0.96f),
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = Color.White.copy(alpha = 0.55f),
                    startAngle = -90f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.26f - w * 0.66f, h * 0.74f - w * 0.66f),
                    size = Size(w * 1.32f, w * 1.32f),
                    style = Stroke(width = 1.4.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStarPath(
    cx: Float,
    cy: Float,
    outerR: Float,
    brush: Brush
) {
    val innerR = outerR * 0.45f
    val path = Path().apply {
        for (i in 0 until 10) {
            val rad = Math.toRadians(-90.0 + i * 36.0)
            val r = if (i % 2 == 0) outerR else innerR
            val px = cx + (r * cos(rad)).toFloat()
            val py = cy + (r * sin(rad)).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
        }
        close()
    }
    drawPath(path = path, brush = brush)
}

/**
 * غلاف هندسي زمردي-نعناعي راقٍ لبطاقة الفعالية/الخبر عند غياب الصورة المرفقة:
 * يستبدل التدرج الأزرق/البنفسجي وأيقونة البوق بتكوين تحريري متناغم مع هوية المنصة.
 */
@Composable
fun EditorialEventFallbackCoverCanvas(
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.46f

        // 1) Rich harmonious emerald-mint editorial gradient
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF064E3B),
                    Color(0xFF047857),
                    Color(0xFF059669),
                    Color(0xFF065F46)
                ),
                start = Offset(0f, 0f),
                end = Offset(w, h)
            )
        )

        // 2) Soft radial emerald-gold editorial halo behind center emblem
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFFFEF08A).copy(alpha = 0.22f),
                    Color(0xFF34D399).copy(alpha = 0.16f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.42f
            ),
            radius = w * 0.42f,
            center = Offset(cx, cy)
        )

        // 3) Subtle geometric architectural diagonal lattice
        val lineColor = Color.White.copy(alpha = 0.07f)
        val step = 36.dp.toPx()
        var x = -h
        while (x < w + h) {
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x + h * 0.65f, h),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = lineColor,
                start = Offset(x + h * 0.65f, 0f),
                end = Offset(x, h),
                strokeWidth = 1.dp.toPx()
            )
            x += step
        }

        // 4) Delicate bottom golden thread wave
        val wave = Path().apply {
            moveTo(0f, h * 0.84f)
            cubicTo(w * 0.30f, h * 0.72f, w * 0.70f, h * 0.96f, w, h * 0.80f)
        }
        drawPath(
            path = wave,
            brush = Brush.horizontalGradient(
                listOf(
                    Color.Transparent,
                    Color(0xFFFEF08A).copy(alpha = 0.55f),
                    Color(0xFF6EE7B7).copy(alpha = 0.45f),
                    Color.Transparent
                )
            ),
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
        )

        // 5) Refined Geometric Vector Event Emblem in center (clean diamond frame + spark)
        val emblemR = minOf(w, h) * 0.23f
        drawCircle(
            color = Color.White.copy(alpha = 0.12f),
            radius = emblemR * 1.15f,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = Color(0xFFFEF08A).copy(alpha = 0.45f),
            radius = emblemR * 1.15f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2.dp.toPx())
        )
        drawCircle(
            color = Color(0xFF6EE7B7).copy(alpha = 0.40f),
            radius = emblemR * 0.84f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.dp.toPx())
        )

        val sparkR = emblemR * 0.58f
        val spark = Path().apply {
            moveTo(cx, cy - sparkR)
            lineTo(cx + sparkR * 0.28f, cy - sparkR * 0.28f)
            lineTo(cx + sparkR, cy)
            lineTo(cx + sparkR * 0.28f, cy + sparkR * 0.28f)
            lineTo(cx, cy + sparkR)
            lineTo(cx - sparkR * 0.28f, cy + sparkR * 0.28f)
            lineTo(cx - sparkR, cy)
            lineTo(cx - sparkR * 0.28f, cy - sparkR * 0.28f)
            close()
        }
        drawPath(
            path = spark,
            brush = Brush.verticalGradient(listOf(Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFFF59E0B)))
        )
    }
}
