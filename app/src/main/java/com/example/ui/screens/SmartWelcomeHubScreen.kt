package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppSection
import com.example.model.PlatformLivePresenceSummary
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.reading.ReadingMascotCanvas
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldLight
import com.example.ui.theme.InfoBlue
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val HERO_READING_OASIS_ID = "hero_reading_oasis"

private enum class BubbleArtKind {
    READING_OASIS,
    MANAGEMENT_CENTER,
    SMART_ADVISOR,
    LIVE_PRESENCE,
    STAFF_STUDENTS,
    CANTEEN_WALLET,
    SCHOOL_TRANSPORT,
    ATTENDANCE_CHECK,
    HOMEWORK_SCHEDULE,
    EXAMS_GRADES,
    PARENT_CHAT,
    INSTANT_ALERTS,
    READING_CHAMPIONS
}

private enum class BubbleTone {
    EMERALD,
    GOLD,
    SKY
}

private data class InteractiveBubbleNode(
    val id: String,
    val bubbleLabel: String,
    val dockTitle: String,
    val dockDescription: String,
    val artKind: BubbleArtKind,
    val tone: BubbleTone,
    val sizeScale: Float = 1f,
    val onOpen: () -> Unit
)

/**
 * Interactive Floating Bubbles Hub ("الواجهة التفاعلية"):
 * - Full-screen organic floating bubble constellation (zero LazyColumn / vertical card stacks).
 * - Platform-native colors from MaterialTheme.colorScheme (Royal Emerald, luminous white/ivory, gold & sky accents).
 * - Zero text emojis, zero Material Icons; 100% Compose Canvas vectors + live ReadingMascotCanvas in center hero bubble.
 * - Single tap selects bubble with spring pop + haptic feedback & updates bottom dock; double tap opens directly.
 */
@Composable
fun SmartWelcomeHubScreen(
    currentUser: UserAccount,
    schoolName: String,
    presenceSummary: PlatformLivePresenceSummary,
    canManageCanteen: Boolean,
    isStudentSubscribedToTransport: Boolean,
    isSmartHubPinned: Boolean,
    onOpenReadingOasis: () -> Unit,
    onNavigateSection: (AppSection) -> Unit,
    onOpenSmartAdvisor: () -> Unit,
    onOpenManagementCenter: () -> Unit,
    onOpenLivePresence: () -> Unit,
    onOpenCanteenPos: () -> Unit,
    onOpenStudentWallet: () -> Unit,
    onOpenNotifications: () -> Unit,
    onTogglePinAsDefault: (Boolean) -> Unit,
    onGoToClassicHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    val displaySchoolName = schoolName.trim().ifBlank { "مدرستي الذكية" }
    val userFullName = currentUser.fullName.trim().ifBlank { currentUser.username.trim().ifBlank { "عضو المنصة" } }

    val isBusDriver = currentUser.isBusDriverAccount
    val isCanteenStaffOnly = canManageCanteen && currentUser.role != UserRole.ADMIN && !isBusDriver

    val roleGateLabel = when {
        isBusDriver -> "النقل المدرسي"
        currentUser.role == UserRole.ADMIN -> "المدير"
        isCanteenStaffOnly -> "الحانوت المدرسي"
        currentUser.role == UserRole.TEACHER -> "المعلم"
        currentUser.role == UserRole.STUDENT -> "الطالب"
        currentUser.role == UserRole.PARENT -> "ولي الأمر"
        else -> "الكادر"
    }

    // Center Hero Bubble — Exclusively "واحة القراءة الذكية" (never duplicated in outer ring)
    val heroOasisNode = remember(onOpenReadingOasis) {
        InteractiveBubbleNode(
            id = HERO_READING_OASIS_ID,
            bubbleLabel = "واحة القراءة",
            dockTitle = "واحة القراءة الذكية",
            dockDescription = "رحلة القراءة المقطعية، كتاب قراءتي الجديدة التفاعلي، ودوري أبطال الصف",
            artKind = BubbleArtKind.READING_OASIS,
            tone = BubbleTone.EMERALD,
            onOpen = onOpenReadingOasis
        )
    }

    // Role-based surrounding floating bubbles (each appears once, short crisp 1-2 word labels)
    val outerBubbles = remember(
        currentUser.id,
        currentUser.role,
        isBusDriver,
        isCanteenStaffOnly,
        isStudentSubscribedToTransport,
        presenceSummary.activeNowCount,
        presenceSummary.activeTodayCount
    ) {
        when {
            currentUser.role == UserRole.ADMIN -> listOf(
                InteractiveBubbleNode(
                    id = "admin_mgmt",
                    bubbleLabel = "مركز الإدارة",
                    dockTitle = "مركز الإدارة",
                    dockDescription = "التحليلات والتقارير التنفيذية ومؤشرات الأداء المدرسي الشاملة",
                    artKind = BubbleArtKind.MANAGEMENT_CENTER,
                    tone = BubbleTone.GOLD,
                    sizeScale = 1.04f,
                    onOpen = onOpenManagementCenter
                ),
                InteractiveBubbleNode(
                    id = "admin_advisor",
                    bubbleLabel = "المستشار الذكي",
                    dockTitle = "المستشار الذكي",
                    dockDescription = "قراءة فورية لمؤشرات المدرسة وتوصيات إدارية وتربوية عملية",
                    artKind = BubbleArtKind.SMART_ADVISOR,
                    tone = BubbleTone.SKY,
                    sizeScale = 1.02f,
                    onOpen = onOpenSmartAdvisor
                ),
                InteractiveBubbleNode(
                    id = "admin_presence",
                    bubbleLabel = "النشاط اللحظي",
                    dockTitle = "النشاط اللحظي للمنصة",
                    dockDescription = "متواجدون الآن: ${presenceSummary.activeNowCount} متصل • الفعّالون اليوم: ${presenceSummary.activeTodayCount}",
                    artKind = BubbleArtKind.LIVE_PRESENCE,
                    tone = BubbleTone.EMERALD,
                    sizeScale = 0.98f,
                    onOpen = onOpenLivePresence
                ),
                InteractiveBubbleNode(
                    id = "admin_staff",
                    bubbleLabel = "الكوادر والطلبة",
                    dockTitle = "الكوادر والطلبة",
                    dockDescription = "إدارة سجلات الكادر التدريسي وحسابات الطلبة والصفوف والشعب",
                    artKind = BubbleArtKind.STAFF_STUDENTS,
                    tone = BubbleTone.SKY,
                    sizeScale = 1.00f,
                    onOpen = { onNavigateSection(AppSection.ADMIN_ACCOUNTS) }
                ),
                InteractiveBubbleNode(
                    id = "admin_canteen",
                    bubbleLabel = "الحانوت والمحفظة",
                    dockTitle = "الحانوت والمحفظة الذكية",
                    dockDescription = "نقطة البيع السريعة وشحن ومتابعة محافظ الطلبة الإلكترونية",
                    artKind = BubbleArtKind.CANTEEN_WALLET,
                    tone = BubbleTone.GOLD,
                    sizeScale = 0.98f,
                    onOpen = onOpenCanteenPos
                ),
                InteractiveBubbleNode(
                    id = "admin_transport",
                    bubbleLabel = "النقل المدرسي",
                    dockTitle = "النقل المدرسي",
                    dockDescription = "تنظيم خطوط النقل والمركبات والطلبة المشتركين بمتابعة دقيقة",
                    artKind = BubbleArtKind.SCHOOL_TRANSPORT,
                    tone = BubbleTone.EMERALD,
                    sizeScale = 0.96f,
                    onOpen = { onNavigateSection(AppSection.SCHOOL_TRANSPORT) }
                )
            )

            isBusDriver -> listOf(
                InteractiveBubbleNode(
                    id = "driver_transport",
                    bubbleLabel = "النقل",
                    dockTitle = "إدارة خط النقل المدرسي",
                    dockDescription = "تسجيل صعود ونزول الطلبة وإرسال إشعارات تحرك المركبة للخط",
                    artKind = BubbleArtKind.SCHOOL_TRANSPORT,
                    tone = BubbleTone.EMERALD,
                    sizeScale = 1.05f,
                    onOpen = onGoToClassicHome
                ),
                InteractiveBubbleNode(
                    id = "driver_schedule",
                    bubbleLabel = "الجدول",
                    dockTitle = "جدول الرحلات اليومية",
                    dockDescription = "استعراض مواعيد الرحلات وقائمة الطلبة المشتركين في خطك",
                    artKind = BubbleArtKind.HOMEWORK_SCHEDULE,
                    tone = BubbleTone.GOLD,
                    sizeScale = 0.98f,
                    onOpen = onGoToClassicHome
                ),
                InteractiveBubbleNode(
                    id = "driver_alerts",
                    bubbleLabel = "الإشعارات",
                    dockTitle = "الإشعارات والتبليغات",
                    dockDescription = "متابعة تنبيهات الإدارة المدرسية ومستجدات خطوط النقل",
                    artKind = BubbleArtKind.INSTANT_ALERTS,
                    tone = BubbleTone.SKY,
                    sizeScale = 0.96f,
                    onOpen = onGoToClassicHome
                )
            )

            isCanteenStaffOnly -> listOf(
                InteractiveBubbleNode(
                    id = "canteen_main",
                    bubbleLabel = "الحانوت",
                    dockTitle = "الحانوت الذكي",
                    dockDescription = "تنفيذ المشتريات السريعة وشحن أرصدة الطلبة بالبطاقة الرقمية",
                    artKind = BubbleArtKind.CANTEEN_WALLET,
                    tone = BubbleTone.GOLD,
                    sizeScale = 1.05f,
                    onOpen = onOpenCanteenPos
                ),
                InteractiveBubbleNode(
                    id = "canteen_schedule",
                    bubbleLabel = "الجدول",
                    dockTitle = "الجدول والدوام اليومي",
                    dockDescription = "متابعة مواعيد الحصص والفرص والجدول المدرسي اليومي",
                    artKind = BubbleArtKind.HOMEWORK_SCHEDULE,
                    tone = BubbleTone.EMERALD,
                    sizeScale = 0.98f,
                    onOpen = { onNavigateSection(AppSection.SCHEDULE_HOMEWORK) }
                ),
                InteractiveBubbleNode(
                    id = "canteen_alerts",
                    bubbleLabel = "الإشعارات",
                    dockTitle = "الإشعارات الفورية",
                    dockDescription = "الاطلاع على التبليغات المدرسية والتنبيهات الحديثة",
                    artKind = BubbleArtKind.INSTANT_ALERTS,
                    tone = BubbleTone.SKY,
                    sizeScale = 0.96f,
                    onOpen = onOpenNotifications
                )
            )

            currentUser.role == UserRole.TEACHER -> listOf(
                InteractiveBubbleNode(
                    id = "teacher_grades",
                    bubbleLabel = "الصفوف والدرجات",
                    dockTitle = "الصفوف والدرجات",
                    dockDescription = "رصد درجات الامتحانات وتقييم مستوى الطلبة في صفوفك",
                    artKind = BubbleArtKind.EXAMS_GRADES,
                    tone = BubbleTone.GOLD,
                    sizeScale = 1.04f,
                    onOpen = { onNavigateSection(AppSection.EXAMS_GRADES) }
                ),
                InteractiveBubbleNode(
                    id = "teacher_homework",
                    bubbleLabel = "الواجبات",
                    dockTitle = "الواجبات والجدول",
                    dockDescription = "نشر الواجبات اليومية ومتابعة الجدول الأسبوعي والتحضير",
                    artKind = BubbleArtKind.HOMEWORK_SCHEDULE,
                    tone = BubbleTone.SKY,
                    sizeScale = 1.00f,
                    onOpen = { onNavigateSection(AppSection.SCHEDULE_HOMEWORK) }
                ),
                InteractiveBubbleNode(
                    id = "teacher_attendance",
                    bubbleLabel = "الحضور",
                    dockTitle = "سجل الحضور والغياب",
                    dockDescription = "رصد المواظبة اليومية لصفوفك وشعبك المسندة بسهولة",
                    artKind = BubbleArtKind.ATTENDANCE_CHECK,
                    tone = BubbleTone.EMERALD,
                    sizeScale = 1.00f,
                    onOpen = { onNavigateSection(AppSection.ATTENDANCE) }
                ),
                InteractiveBubbleNode(
                    id = "teacher_chat",
                    bubbleLabel = "التواصل",
                    dockTitle = "التواصل مع أولياء الأمور",
                    dockDescription = "إرسال التبليغات التربوية والتواصل المباشر مع العائلات",
                    artKind = BubbleArtKind.PARENT_CHAT,
                    tone = BubbleTone.SKY,
                    sizeScale = 0.98f,
                    onOpen = { onNavigateSection(AppSection.REPORTS_CHAT) }
                ),
                InteractiveBubbleNode(
                    id = "teacher_champions",
                    bubbleLabel = "أبطال القراءة",
                    dockTitle = "أبطال القراءة",
                    dockDescription = "متابعة تميّز طلبتك وتقدمهم في دوري أبطال القراءة بالصف",
                    artKind = BubbleArtKind.READING_CHAMPIONS,
                    tone = BubbleTone.GOLD,
                    sizeScale = 1.02f,
                    onOpen = onOpenReadingOasis
                )
            )

            else -> buildList {
                add(
                    InteractiveBubbleNode(
                        id = "stu_lessons",
                        bubbleLabel = "الدروس والواجبات",
                        dockTitle = "الدروس والواجبات",
                        dockDescription = "الجدول الأسبوعي والواجبات البيتية والمناهج الدراسية اليومية",
                        artKind = BubbleArtKind.HOMEWORK_SCHEDULE,
                        tone = BubbleTone.SKY,
                        sizeScale = 1.04f,
                        onOpen = { onNavigateSection(AppSection.SCHEDULE_HOMEWORK) }
                    )
                )
                add(
                    InteractiveBubbleNode(
                        id = "stu_grades",
                        bubbleLabel = "الدرجات والتفوق",
                        dockTitle = "الدرجات والتفوق",
                        dockDescription = "متابعة نتائج الامتحانات والشهادات ومستوى التقدم العلمي",
                        artKind = BubbleArtKind.EXAMS_GRADES,
                        tone = BubbleTone.GOLD,
                        sizeScale = 1.02f,
                        onOpen = { onNavigateSection(AppSection.EXAMS_GRADES) }
                    )
                )
                add(
                    InteractiveBubbleNode(
                        id = "stu_attendance",
                        bubbleLabel = "الحضور",
                        dockTitle = "الحضور والمواظبة",
                        dockDescription = "متابعة سجل الدوام اليومي وحركة الدخول والانصراف والإجازات",
                        artKind = BubbleArtKind.ATTENDANCE_CHECK,
                        tone = BubbleTone.EMERALD,
                        sizeScale = 0.98f,
                        onOpen = { onNavigateSection(AppSection.ATTENDANCE) }
                    )
                )
                add(
                    InteractiveBubbleNode(
                        id = "stu_canteen",
                        bubbleLabel = "البطاقة والحانوت",
                        dockTitle = "البطاقة والحانوت الذكي",
                        dockDescription = "عرض الهوية الرقمية ورصيد المحفظة وحركة المشتريات في الحانوت",
                        artKind = BubbleArtKind.CANTEEN_WALLET,
                        tone = BubbleTone.GOLD,
                        sizeScale = 1.00f,
                        onOpen = onOpenStudentWallet
                    )
                )
                if (isStudentSubscribedToTransport) {
                    add(
                        InteractiveBubbleNode(
                            id = "stu_transport",
                            bubbleLabel = "النقل المدرسي",
                            dockTitle = "النقل المدرسي",
                            dockDescription = "متابعة حالة الرحلة والمركبة المدرسية المشترك بها",
                            artKind = BubbleArtKind.SCHOOL_TRANSPORT,
                            tone = BubbleTone.EMERALD,
                            sizeScale = 0.96f,
                            onOpen = { onNavigateSection(AppSection.SCHOOL_TRANSPORT) }
                        )
                    )
                }
                add(
                    InteractiveBubbleNode(
                        id = "stu_alerts",
                        bubbleLabel = "الإشعارات",
                        dockTitle = "الإشعارات الفورية",
                        dockDescription = "جميع التنبيهات المدرسية والتبليغات الرسمية في مكان واحد",
                        artKind = BubbleArtKind.INSTANT_ALERTS,
                        tone = BubbleTone.SKY,
                        sizeScale = 0.96f,
                        onOpen = onOpenNotifications
                    )
                )
            }
        }
    }

    var selectedBubbleId by remember(currentUser.id) { mutableStateOf(HERO_READING_OASIS_ID) }
    var lastTappedId by remember { mutableStateOf("") }
    var lastTapTimestampMs by remember { mutableLongStateOf(0L) }
    var popTriggerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(popTriggerId) {
        if (popTriggerId != null) {
            delay(220)
            popTriggerId = null
        }
    }

    val selectedNode = remember(selectedBubbleId, heroOasisNode, outerBubbles) {
        if (selectedBubbleId == HERO_READING_OASIS_ID) {
            heroOasisNode
        } else {
            outerBubbles.firstOrNull { it.id == selectedBubbleId } ?: heroOasisNode
        }
    }

    fun handleBubbleTap(node: InteractiveBubbleNode) {
        val now = System.currentTimeMillis()
        val isDoubleTap = lastTappedId == node.id && (now - lastTapTimestampMs) <= 460L
        lastTappedId = node.id
        lastTapTimestampMs = now
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedBubbleId = node.id
        popTriggerId = node.id
        if (isDoubleTap) {
            node.onOpen()
        }
    }

    // Continuous smooth organic floating phase (independent per bubble via angle offsets)
    val floatTransition = rememberInfiniteTransition(label = "bubbles_organic_constellation")
    val phaseVertical by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_phase_y"
    )
    val phaseHorizontal by floatTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 8600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bubble_phase_x"
    )
    val heroHaloPulse by floatTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.09f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hero_bubble_halo_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .testTag("smart_welcome_hub_screen")
    ) {
        // Soft platform-native luminous atmospheric tints (bright & clean in Light mode, adaptive in Dark mode)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.50f, h * 0.44f),
                    radius = w * 0.78f
                ),
                radius = w * 0.78f,
                center = Offset(w * 0.50f, h * 0.44f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldAccent.copy(alpha = 0.07f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.14f, h * 0.16f),
                    radius = w * 0.50f
                ),
                radius = w * 0.50f,
                center = Offset(w * 0.14f, h * 0.16f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        InfoBlue.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.88f, h * 0.76f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.88f, h * 0.76f)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
            ) {
                // =========================================================================
                // 1. LIGHT, CLEAN TOP HEADER (NO GIANT BOX, NO EXTRA BADGES)
                // =========================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "أهلاً بك، $userFullName",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                lineHeight = 26.sp
                            ),
                            fontWeight = FontWeight.ExtraBold,
                            color = colorScheme.onBackground,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$displaySchoolName • بوابة $roleGateLabel",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.5.sp),
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Sleek Pill Button: "الرئيسية"
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .shadow(
                                elevation = 4.dp,
                                shape = RoundedCornerShape(50),
                                ambientColor = colorScheme.primary.copy(alpha = 0.14f)
                            )
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onGoToClassicHome)
                            .testTag("welcome_hub_header_classic_btn"),
                        shape = RoundedCornerShape(50),
                        color = colorScheme.surface,
                        border = BorderStroke(1.2.dp, colorScheme.primary.copy(alpha = 0.40f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            HubClassicGridVectorCanvas(
                                tint = colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "الرئيسية",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }

                // =========================================================================
                // 2. INTERACTIVE FLOATING BUBBLES CANVAS AREA (CENTERPIECE)
                // =========================================================================
                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    val areaW = maxWidth
                    val areaH = maxHeight
                    val minDim = minOf(areaW, areaH)

                    // Responsive, strictly non-overlapping diameters
                    val heroSize = (minDim * 0.415f).coerceIn(140.dp, 156.dp)
                    val outerBaseSize = (minDim * 0.258f).coerceIn(86.dp, 102.dp)

                    val areaWPx = with(density) { areaW.toPx() }
                    val areaHPx = with(density) { areaH.toPx() }
                    val centerXPx = areaWPx * 0.5f
                    val centerYPx = areaHPx * 0.5f

                    val heroSizePx = with(density) { heroSize.toPx() }
                    val outerMaxPx = with(density) { (outerBaseSize * 1.05f).toPx() }
                    val edgePadPx = with(density) { 10.dp.toPx() }

                    // Guarantee zero overlap between Hero Center Bubble and Outer Orbit Ring
                    val minSafeOrbitPx = (heroSizePx + outerMaxPx) * 0.5f + with(density) { 12.dp.toPx() }
                    val maxRxPx = ((areaWPx - outerMaxPx) * 0.5f - edgePadPx).coerceAtLeast(minSafeOrbitPx * 0.86f)
                    val maxRyPx = ((areaHPx - outerMaxPx) * 0.5f - edgePadPx).coerceAtLeast(minSafeOrbitPx * 0.96f)

                    // Smooth elliptical orbit fitting phone aspect ratios cleanly
                    val orbitRxPx = minOf(maxRxPx, maxOf(minSafeOrbitPx * 0.96f, areaWPx * 0.345f))
                    val orbitRyPx = minOf(maxRyPx, maxOf(minSafeOrbitPx * 1.06f, areaHPx * 0.365f))

                    // Hero organic floating displacement
                    val heroFloatXPx = with(density) { (sin(phaseHorizontal + 0.4f) * 3.5f).dp.toPx() }
                    val heroFloatYPx = with(density) { (sin(phaseVertical) * 5.2f).dp.toPx() }
                    val heroLiveCenter = Offset(centerXPx + heroFloatXPx, centerYPx + heroFloatYPx)

                    // Precompute exact angles & live centers for outer bubbles
                    val count = outerBubbles.size
                    val angleDegrees = remember(count) {
                        when (count) {
                            6 -> listOf(-90f, -30f, 30f, 90f, 150f, 210f)
                            5 -> listOf(-90f, -18f, 54f, 126f, 198f)
                            4 -> listOf(-45f, 45f, 135f, 225f)
                            3 -> listOf(-90f, 30f, 150f)
                            2 -> listOf(-90f, 90f)
                            else -> listOf(-90f)
                        }
                    }

                    val outerPositionsPx = outerBubbles.mapIndexed { idx, _ ->
                        val deg = angleDegrees.getOrElse(idx) { idx * (360f / count.coerceAtLeast(1)) }
                        val rad = deg * PI / 180.0
                        val phaseOffsetY = idx * 1.18f + 0.7f
                        val phaseOffsetX = idx * 1.45f + 0.3f
                        val ampY = (4.5f + (idx % 3) * 1.3f)
                        val ampX = (3.4f + ((idx + 1) % 3) * 1.1f)

                        val floatY = with(density) { (sin(phaseVertical + phaseOffsetY) * ampY).dp.toPx() }
                        val floatX = with(density) { (cos(phaseHorizontal + phaseOffsetX) * ampX).dp.toPx() }

                        val rawX = centerXPx + (orbitRxPx * cos(rad)).toFloat() + floatX
                        val rawY = centerYPx + (orbitRyPx * sin(rad)).toFloat() + floatY
                        Offset(
                            x = rawX.coerceIn(outerMaxPx * 0.5f + 4f, areaWPx - outerMaxPx * 0.5f - 4f),
                            y = rawY.coerceIn(outerMaxPx * 0.5f + 4f, areaHPx - outerMaxPx * 0.5f - 4f)
                        )
                    }

                    // Background Orbital Rings + Translucent Luminous Connectors to Center
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Subtle concentric orbital guide circles
                        drawOval(
                            color = colorScheme.primary.copy(alpha = 0.085f),
                            topLeft = Offset(centerXPx - orbitRxPx, centerYPx - orbitRyPx),
                            size = Size(orbitRxPx * 2f, orbitRyPx * 2f),
                            style = Stroke(width = 1.2.dp.toPx())
                        )
                        drawCircle(
                            color = GoldAccent.copy(alpha = 0.09f),
                            radius = heroSizePx * 0.68f * heroHaloPulse,
                            center = heroLiveCenter,
                            style = Stroke(width = 1.1.dp.toPx())
                        )

                        // Soft luminous connecting threads between center & each floating bubble
                        outerPositionsPx.forEachIndexed { idx, pos ->
                            val isSelected = outerBubbles[idx].id == selectedBubbleId
                            val threadColor = when (outerBubbles[idx].tone) {
                                BubbleTone.EMERALD -> colorScheme.primary
                                BubbleTone.GOLD -> GoldAccent
                                BubbleTone.SKY -> InfoBlue
                            }
                            drawLine(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        colorScheme.primary.copy(alpha = if (isSelected) 0.34f else 0.11f),
                                        threadColor.copy(alpha = if (isSelected) 0.42f else 0.13f)
                                    ),
                                    start = heroLiveCenter,
                                    end = pos
                                ),
                                start = heroLiveCenter,
                                end = pos,
                                strokeWidth = if (isSelected) 2.dp.toPx() else 1.1.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                    }

                    // Outer Surrounding Role-Based Floating Bubbles
                    outerBubbles.forEachIndexed { idx, node ->
                        val bubbleDiameter = (outerBaseSize * node.sizeScale).coerceIn(86.dp, 106.dp)
                        val bubblePx = with(density) { bubbleDiameter.toPx() }
                        val livePos = outerPositionsPx[idx]
                        val isSelected = selectedBubbleId == node.id
                        val isPopping = popTriggerId == node.id

                        SurroundingFloatingBubble(
                            node = node,
                            diameter = bubbleDiameter,
                            isSelected = isSelected,
                            isPopping = isPopping,
                            onTap = { handleBubbleTap(node) },
                            modifier = Modifier.offset {
                                IntOffset(
                                    x = (livePos.x - bubblePx * 0.5f).roundToInt(),
                                    y = (livePos.y - bubblePx * 0.5f).roundToInt()
                                )
                            }
                        )
                    }

                    // Hero Center Bubble ("واحة القراءة الذكية") featuring live ReadingMascotCanvas!
                    HeroReadingOasisCenterBubble(
                        diameter = heroSize,
                        isSelected = selectedBubbleId == HERO_READING_OASIS_ID,
                        isPopping = popTriggerId == HERO_READING_OASIS_ID,
                        haloPulse = heroHaloPulse,
                        onTap = { handleBubbleTap(heroOasisNode) },
                        modifier = Modifier.offset {
                            IntOffset(
                                x = (heroLiveCenter.x - heroSizePx * 0.5f).roundToInt(),
                                y = (heroLiveCenter.y - heroSizePx * 0.5f).roundToInt()
                            )
                        }
                    )
                }

                // =========================================================================
                // 3. INTERACTIVE BOTTOM FLOATING DOCK + LIGHTWEIGHT CONTROL ROW
                // =========================================================================
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 12.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    InteractiveSelectedBubbleDock(
                        node = selectedNode,
                        onEnterNow = selectedNode.onOpen
                    )

                    // Lightweight Control Row: Pin Switch + "الانتقال إلى الرئيسية"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onTogglePinAsDefault(!isSmartHubPinned) }
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .testTag("welcome_hub_pin_default_card"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Switch(
                                checked = isSmartHubPinned,
                                onCheckedChange = onTogglePinAsDefault,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colorScheme.primary
                                ),
                                modifier = Modifier
                                    .graphicsLayer {
                                        scaleX = 0.84f
                                        scaleY = 0.84f
                                    }
                                    .testTag("welcome_hub_pin_default_switch")
                            )
                            Text(
                                text = "تثبيت كواجهة رئيسية",
                                style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.Bold,
                                color = colorScheme.onSurface
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onGoToClassicHome)
                                .testTag("welcome_hub_go_classic_home_button"),
                            shape = RoundedCornerShape(50),
                            color = colorScheme.primaryContainer.copy(alpha = 0.55f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = "الانتقال إلى الرئيسية",
                                    style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colorScheme.onPrimaryContainer
                                )
                                HubChevronLeftVectorCanvas(
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Centerpiece Hero Bubble (~146-156dp) dedicated exclusively to "واحة القراءة الذكية",
 * reusing the live animated ReadingMascotCanvas ("فصيح") from the reading/ module.
 */
@Composable
private fun HeroReadingOasisCenterBubble(
    diameter: Dp,
    isSelected: Boolean,
    isPopping: Boolean,
    haloPulse: Float,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.91f
            isPopping -> 1.09f
            isSelected -> 1.03f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "hero_bubble_spring_scale"
    )

    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        // Soft pulsing ambient glow ring around the Hero Center Bubble
        Canvas(modifier = Modifier.size(diameter + 26.dp)) {
            val c = Offset(size.width / 2f, size.height / 2f)
            val baseR = size.minDimension * 0.48f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = if (isSelected) 0.28f else 0.16f),
                        GoldAccent.copy(alpha = if (isSelected) 0.16f else 0.08f),
                        Color.Transparent
                    ),
                    center = c,
                    radius = baseR * haloPulse
                ),
                radius = baseR * haloPulse,
                center = c
            )
        }

        Surface(
            modifier = Modifier
                .size(diameter)
                .shadow(
                    elevation = if (isSelected) 16.dp else 10.dp,
                    shape = CircleShape,
                    ambientColor = colorScheme.primary.copy(alpha = 0.28f),
                    spotColor = GoldAccent.copy(alpha = 0.24f)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onTap
                )
                .testTag("welcome_hub_reading_oasis_spotlight"),
            shape = CircleShape,
            color = colorScheme.surface,
            border = BorderStroke(
                width = if (isSelected) 2.6.dp else 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        colorScheme.primary,
                        GoldAccent,
                        InfoBlue,
                        colorScheme.primary
                    )
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                colorScheme.surface,
                                colorScheme.primaryContainer.copy(alpha = 0.45f),
                                InfoBlue.copy(alpha = 0.14f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Soft 3D Specular Glass Highlight along top arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.58f),
                                Color.White.copy(alpha = 0.0f)
                            ),
                            startY = h * 0.04f,
                            endY = h * 0.42f
                        ),
                        topLeft = Offset(w * 0.16f, h * 0.05f),
                        size = Size(w * 0.68f, h * 0.34f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 10.dp, bottom = 12.dp, start = 8.dp, end = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // Authentic Live Animation from reading/ module ("فصيح")
                    ReadingMascotCanvas(
                        modifier = Modifier.size(diameter * 0.58f),
                        isCelebrating = isSelected || isPopping
                    )

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colorScheme.primary.copy(alpha = 0.92f),
                        shadowElevation = 2.dp
                    ) {
                        Text(
                            text = "واحة القراءة",
                            style = MaterialTheme.typography.labelMedium.copy(fontSize = 11.5.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Glossy Surrounding Floating Bubble (86-106dp) with specular top sheen,
 * colored soft shadow, custom Canvas vector icon, short crisp Arabic title,
 * and glowing selection halo + spring pop on tap.
 */
@Composable
private fun SurroundingFloatingBubble(
    node: InteractiveBubbleNode,
    diameter: Dp,
    isSelected: Boolean,
    isPopping: Boolean,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()

    val accentColor = when (node.tone) {
        BubbleTone.EMERALD -> colorScheme.primary
        BubbleTone.GOLD -> GoldAccent
        BubbleTone.SKY -> InfoBlue
    }
    val softTint = when (node.tone) {
        BubbleTone.EMERALD -> colorScheme.primaryContainer
        BubbleTone.GOLD -> colorScheme.tertiaryContainer
        BubbleTone.SKY -> colorScheme.secondaryContainer
    }

    val scale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.89f
            isPopping -> 1.13f
            isSelected -> 1.07f
            else -> 1f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "outer_bubble_spring_${node.id}"
    )

    Box(
        modifier = modifier
            .size(diameter)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Canvas(modifier = Modifier.size(diameter + 16.dp)) {
                val c = Offset(size.width / 2f, size.height / 2f)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = c,
                        radius = size.minDimension * 0.50f
                    ),
                    radius = size.minDimension * 0.50f,
                    center = c
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(diameter)
                .shadow(
                    elevation = if (isSelected) 12.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = accentColor.copy(alpha = 0.24f),
                    spotColor = accentColor.copy(alpha = 0.24f)
                )
                .clip(CircleShape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onTap
                )
                .testTag("welcome_hub_feature_${node.id}"),
            shape = CircleShape,
            color = colorScheme.surface,
            border = BorderStroke(
                width = if (isSelected) 2.3.dp else 1.25.dp,
                brush = Brush.linearGradient(
                    colors = if (isSelected) {
                        listOf(accentColor, GoldAccent, accentColor)
                    } else {
                        listOf(accentColor.copy(alpha = 0.52f), accentColor.copy(alpha = 0.22f))
                    }
                )
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                colorScheme.surface,
                                softTint.copy(alpha = if (isSelected) 0.62f else 0.42f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Glossy Specular Highlight Arc
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    drawOval(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.62f),
                                Color.White.copy(alpha = 0f)
                            ),
                            startY = h * 0.05f,
                            endY = h * 0.42f
                        ),
                        topLeft = Offset(w * 0.18f, h * 0.06f),
                        size = Size(w * 0.64f, h * 0.32f)
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 7.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BubbleFeatureVectorCanvas(
                        kind = node.artKind,
                        primaryColor = accentColor,
                        secondaryColor = if (node.tone == BubbleTone.GOLD) colorScheme.primary else GoldAccent,
                        modifier = Modifier.size((diameter * 0.30f).coerceIn(26.dp, 32.dp))
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = node.bubbleLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.5.sp,
                            lineHeight = 13.sp
                        ),
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Interactive Floating Dock at the bottom of the screen.
 * Smoothly crossfades & slides via AnimatedContent whenever a bubble is tapped.
 */
@Composable
private fun InteractiveSelectedBubbleDock(
    node: InteractiveBubbleNode,
    onEnterNow: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = when (node.tone) {
        BubbleTone.EMERALD -> colorScheme.primary
        BubbleTone.GOLD -> GoldAccent
        BubbleTone.SKY -> InfoBlue
    }

    val ctaInteraction = remember { MutableInteractionSource() }
    val isCtaPressed by ctaInteraction.collectIsPressedAsState()
    val ctaScale by animateFloatAsState(
        targetValue = if (isCtaPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "dock_cta_spring"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = accentColor.copy(alpha = 0.16f),
                spotColor = colorScheme.primary.copy(alpha = 0.14f)
            )
            .testTag("welcome_hub_interactive_dock"),
        shape = RoundedCornerShape(24.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = 1.3.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    accentColor.copy(alpha = 0.48f),
                    GoldAccent.copy(alpha = 0.36f),
                    colorScheme.primary.copy(alpha = 0.32f)
                )
            )
        )
    ) {
        AnimatedContent(
            targetState = node,
            transitionSpec = {
                (fadeIn(tween(220)) + slideInVertically(tween(240)) { it / 5 })
                    .togetherWith(fadeOut(tween(150)) + slideOutVertically(tween(160)) { -it / 6 })
            },
            label = "selected_bubble_dock_content"
        ) { activeNode ->
            val activeAccent = when (activeNode.tone) {
                BubbleTone.EMERALD -> colorScheme.primary
                BubbleTone.GOLD -> GoldAccent
                BubbleTone.SKY -> InfoBlue
            }
            val activeTint = when (activeNode.tone) {
                BubbleTone.EMERALD -> colorScheme.primaryContainer
                BubbleTone.GOLD -> colorScheme.tertiaryContainer
                BubbleTone.SKY -> colorScheme.secondaryContainer
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = activeTint.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, activeAccent.copy(alpha = 0.30f)),
                    modifier = Modifier.size(46.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (activeNode.id == HERO_READING_OASIS_ID) {
                            ReadingMascotCanvas(
                                modifier = Modifier.size(36.dp),
                                isCelebrating = false
                            )
                        } else {
                            BubbleFeatureVectorCanvas(
                                kind = activeNode.artKind,
                                primaryColor = activeAccent,
                                secondaryColor = GoldAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = activeNode.dockTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontSize = 15.sp),
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = activeNode.dockDescription,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.8.sp,
                            lineHeight = 16.5.sp
                        ),
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    modifier = Modifier
                        .height(44.dp)
                        .graphicsLayer {
                            scaleX = ctaScale
                            scaleY = ctaScale
                        }
                        .clip(RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = ctaInteraction,
                            indication = null,
                            onClick = onEnterNow
                        )
                        .testTag("welcome_hub_dock_enter_now_btn"),
                    shape = RoundedCornerShape(14.dp),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(colorScheme.primary, DeepEmerald)
                                )
                            )
                            .padding(horizontal = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "دخول الآن",
                                style = MaterialTheme.typography.labelLarge.copy(fontSize = 12.5.sp),
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1
                            )
                            HubChevronLeftVectorCanvas(
                                tint = GoldLight,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// =========================================================================
// BESPOKE COMPOSE CANVAS VECTOR ARTWORK (ZERO EMOJIS / ZERO MATERIAL ICONS)
// =========================================================================

@Composable
private fun BubbleFeatureVectorCanvas(
    kind: BubbleArtKind,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stroke = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)

        when (kind) {
            BubbleArtKind.MANAGEMENT_CENTER -> {
                val barW = w * 0.16f
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.12f, h * 0.56f),
                    size = Size(barW, h * 0.32f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(w * 0.42f, h * 0.36f),
                    size = Size(barW, h * 0.52f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.72f, h * 0.20f),
                    size = Size(barW, h * 0.68f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
                drawArc(
                    color = secondaryColor,
                    startAngle = 195f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(w * 0.08f, h * 0.06f),
                    size = Size(w * 0.84f, h * 0.48f),
                    style = stroke
                )
            }

            BubbleArtKind.SMART_ADVISOR -> {
                val cx = w / 2f
                val cy = h / 2f
                drawCircle(color = primaryColor, radius = w * 0.42f, center = Offset(cx, cy), style = stroke)
                val wave = Path().apply {
                    moveTo(w * 0.18f, cy)
                    cubicTo(w * 0.35f, h * 0.22f, w * 0.65f, h * 0.78f, w * 0.82f, cy)
                }
                drawPath(path = wave, color = secondaryColor, style = stroke)
                drawCircle(color = primaryColor, radius = w * 0.10f, center = Offset(cx, cy))
            }

            BubbleArtKind.LIVE_PRESENCE -> {
                val cx = w / 2f
                val cy = h / 2f
                drawCircle(color = primaryColor.copy(alpha = 0.22f), radius = w * 0.44f, center = Offset(cx, cy))
                drawCircle(color = primaryColor, radius = w * 0.44f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = secondaryColor, radius = w * 0.26f, center = Offset(cx, cy), style = stroke)
                drawCircle(color = EmeraldAccent, radius = w * 0.12f, center = Offset(cx, cy))
            }

            BubbleArtKind.STAFF_STUDENTS -> {
                drawCircle(color = primaryColor, radius = w * 0.15f, center = Offset(w * 0.36f, h * 0.30f), style = stroke)
                val body1 = Path().apply {
                    moveTo(w * 0.12f, h * 0.82f)
                    cubicTo(w * 0.14f, h * 0.56f, w * 0.58f, h * 0.56f, w * 0.60f, h * 0.82f)
                }
                drawPath(path = body1, color = primaryColor, style = stroke)
                drawCircle(color = secondaryColor, radius = w * 0.12f, center = Offset(w * 0.70f, h * 0.36f), style = stroke)
                val body2 = Path().apply {
                    moveTo(w * 0.56f, h * 0.82f)
                    cubicTo(w * 0.60f, h * 0.62f, w * 0.86f, h * 0.62f, w * 0.90f, h * 0.82f)
                }
                drawPath(path = body2, color = secondaryColor, style = stroke)
            }

            BubbleArtKind.CANTEEN_WALLET -> {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.10f, h * 0.22f),
                    size = Size(w * 0.80f, h * 0.56f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = stroke
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.10f, h * 0.42f),
                    end = Offset(w * 0.90f, h * 0.42f),
                    strokeWidth = 2.dp.toPx()
                )
                drawRoundRect(
                    color = secondaryColor,
                    topLeft = Offset(w * 0.60f, h * 0.54f),
                    size = Size(w * 0.19f, h * 0.14f),
                    cornerRadius = CornerRadius(4f, 4f)
                )
            }

            BubbleArtKind.SCHOOL_TRANSPORT -> {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.14f, h * 0.16f),
                    size = Size(w * 0.72f, h * 0.58f),
                    cornerRadius = CornerRadius(w * 0.14f, w * 0.14f),
                    style = stroke
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(w * 0.14f, h * 0.46f),
                    end = Offset(w * 0.86f, h * 0.46f),
                    strokeWidth = 2.dp.toPx()
                )
                drawCircle(color = primaryColor, radius = w * 0.08f, center = Offset(w * 0.30f, h * 0.80f))
                drawCircle(color = primaryColor, radius = w * 0.08f, center = Offset(w * 0.70f, h * 0.80f))
            }

            BubbleArtKind.READING_OASIS,
            BubbleArtKind.READING_CHAMPIONS -> {
                // Golden crown / trophy star for Reading Champions
                val crown = Path().apply {
                    moveTo(w * 0.16f, h * 0.72f)
                    lineTo(w * 0.12f, h * 0.30f)
                    lineTo(w * 0.36f, h * 0.48f)
                    lineTo(w * 0.50f, h * 0.18f)
                    lineTo(w * 0.64f, h * 0.48f)
                    lineTo(w * 0.88f, h * 0.30f)
                    lineTo(w * 0.84f, h * 0.72f)
                    close()
                }
                drawPath(path = crown, color = secondaryColor.copy(alpha = 0.24f))
                drawPath(path = crown, color = primaryColor, style = stroke)
                drawLine(
                    color = secondaryColor,
                    start = Offset(w * 0.22f, h * 0.84f),
                    end = Offset(w * 0.78f, h * 0.84f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            BubbleArtKind.ATTENDANCE_CHECK -> {
                drawCircle(color = primaryColor, radius = w * 0.42f, center = Offset(w / 2f, h / 2f), style = stroke)
                val check = Path().apply {
                    moveTo(w * 0.28f, h * 0.52f)
                    lineTo(w * 0.44f, h * 0.68f)
                    lineTo(w * 0.74f, h * 0.34f)
                }
                drawPath(path = check, color = secondaryColor, style = stroke)
            }

            BubbleArtKind.HOMEWORK_SCHEDULE -> {
                drawRoundRect(
                    color = primaryColor,
                    topLeft = Offset(w * 0.14f, h * 0.18f),
                    size = Size(w * 0.72f, h * 0.68f),
                    cornerRadius = CornerRadius(w * 0.12f, w * 0.12f),
                    style = stroke
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.14f, h * 0.38f),
                    end = Offset(w * 0.86f, h * 0.38f),
                    strokeWidth = 2.dp.toPx()
                )
                drawLine(
                    color = secondaryColor,
                    start = Offset(w * 0.28f, h * 0.56f),
                    end = Offset(w * 0.72f, h * 0.56f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.28f, h * 0.70f),
                    end = Offset(w * 0.56f, h * 0.70f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            BubbleArtKind.EXAMS_GRADES -> {
                val cx = w / 2f
                val cy = h * 0.48f
                val outerR = w * 0.38f
                val innerR = w * 0.17f
                val star = Path().apply {
                    for (i in 0 until 10) {
                        val r = if (i % 2 == 0) outerR else innerR
                        val angle = (-90.0 + i * 36.0) * PI / 180.0
                        val px = cx + (r * cos(angle)).toFloat()
                        val py = cy + (r * sin(angle)).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                drawPath(path = star, color = secondaryColor.copy(alpha = 0.24f))
                drawPath(path = star, color = primaryColor, style = stroke)
            }

            BubbleArtKind.PARENT_CHAT -> {
                val bubble = Path().apply {
                    moveTo(w * 0.22f, h * 0.18f)
                    lineTo(w * 0.78f, h * 0.18f)
                    cubicTo(w * 0.88f, h * 0.18f, w * 0.88f, h * 0.68f, w * 0.78f, h * 0.68f)
                    lineTo(w * 0.46f, h * 0.68f)
                    lineTo(w * 0.24f, h * 0.86f)
                    lineTo(w * 0.28f, h * 0.68f)
                    lineTo(w * 0.22f, h * 0.68f)
                    cubicTo(w * 0.12f, h * 0.68f, w * 0.12f, h * 0.18f, w * 0.22f, h * 0.18f)
                    close()
                }
                drawPath(path = bubble, color = primaryColor, style = stroke)
                drawLine(
                    color = secondaryColor,
                    start = Offset(w * 0.30f, h * 0.38f),
                    end = Offset(w * 0.70f, h * 0.38f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = primaryColor,
                    start = Offset(w * 0.30f, h * 0.52f),
                    end = Offset(w * 0.56f, h * 0.52f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            BubbleArtKind.INSTANT_ALERTS -> {
                val bell = Path().apply {
                    moveTo(w * 0.50f, h * 0.14f)
                    cubicTo(w * 0.28f, h * 0.16f, w * 0.24f, h * 0.42f, w * 0.20f, h * 0.68f)
                    lineTo(w * 0.80f, h * 0.68f)
                    cubicTo(w * 0.76f, h * 0.42f, w * 0.72f, h * 0.16f, w * 0.50f, h * 0.14f)
                    close()
                }
                drawPath(path = bell, color = primaryColor, style = stroke)
                drawCircle(color = secondaryColor, radius = w * 0.08f, center = Offset(w * 0.50f, h * 0.80f))
            }
        }
    }
}

@Composable
private fun HubChevronLeftVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.64f, h * 0.20f)
            lineTo(w * 0.32f, h * 0.50f)
            lineTo(w * 0.64f, h * 0.80f)
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.1.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun HubPinStarVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val outerR = size.minDimension * 0.44f
        val innerR = size.minDimension * 0.20f
        val path = Path().apply {
            for (i in 0 until 10) {
                val r = if (i % 2 == 0) outerR else innerR
                val angle = (-90.0 + i * 36.0) * PI / 180.0
                val px = cx + (r * cos(angle)).toFloat()
                val py = cy + (r * sin(angle)).toFloat()
                if (i == 0) moveTo(px, py) else lineTo(px, py)
            }
            close()
        }
        drawPath(
            path = path,
            color = tint,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun HubClassicGridVectorCanvas(
    tint: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cell = w * 0.36f
        val gap = w * 0.14f
        val start = w * 0.07f
        val stroke = Stroke(width = 1.9.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        val positions = listOf(
            Offset(start, start),
            Offset(start + cell + gap, start),
            Offset(start, start + cell + gap),
            Offset(start + cell + gap, start + cell + gap)
        )
        positions.forEach { pos ->
            drawRoundRect(
                color = tint,
                topLeft = pos,
                size = Size(cell, cell),
                cornerRadius = CornerRadius(4f, 4f),
                style = stroke
            )
        }
    }
}

/**
 * Account Settings Card: "تخصيص واجهة المنصة"
 * Allows opening "الواجهة التفاعلية" anytime and toggling it as startup screen.
 */
@Composable
fun PlatformInterfaceCustomizationCard(
    isSmartHubDefault: Boolean,
    onOpenSmartWelcomeHub: () -> Unit,
    onToggleSmartHubDefault: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 5.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = colorScheme.primary.copy(alpha = 0.12f),
                spotColor = colorScheme.primary.copy(alpha = 0.10f)
            )
            .testTag("settings_platform_interface_customization_card"),
        shape = RoundedCornerShape(22.dp),
        color = colorScheme.surface,
        border = BorderStroke(
            width = 1.2.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    colorScheme.primary.copy(alpha = 0.40f),
                    colorScheme.tertiary.copy(alpha = 0.32f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.primaryContainer.copy(alpha = 0.65f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        HubPinStarVectorCanvas(
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "تخصيص واجهة المنصة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = "تحكّم بواجهة البداية المفضلة لحسابك على جميع أجهزتك",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Button: Open Interactive Interface anytime
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .clickable(onClick = onOpenSmartWelcomeHub)
                    .testTag("settings_open_smart_welcome_hub_button"),
                shape = RoundedCornerShape(15.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    colorScheme.primary,
                                    DeepEmerald,
                                    colorScheme.primary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HubClassicGridVectorCanvas(
                            tint = GoldLight,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "فتح الواجهة التفاعلية",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.5.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Switch: Set Interactive Interface as default startup screen
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onToggleSmartHubDefault(!isSmartHubDefault) },
                shape = RoundedCornerShape(16.dp),
                color = if (isSmartHubDefault) {
                    colorScheme.primaryContainer.copy(alpha = 0.42f)
                } else {
                    colorScheme.surfaceVariant.copy(alpha = 0.48f)
                },
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSmartHubDefault) colorScheme.primary.copy(alpha = 0.42f) else colorScheme.outline.copy(alpha = 0.30f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = "اعتماد الواجهة التفاعلية عند فتح التطبيق",
                            style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.5.sp),
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = if (isSmartHubDefault) {
                                "مفعّل — يفتح التطبيق على الواجهة التفاعلية مع زر انتقال سريع إلى الرئيسية"
                            } else {
                                "متوقف — يفتح التطبيق على الرئيسية مباشرة"
                            },
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = isSmartHubDefault,
                        onCheckedChange = onToggleSmartHubDefault,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = colorScheme.primary
                        ),
                        modifier = Modifier.testTag("settings_default_smart_hub_switch")
                    )
                }
            }
        }
    }
}
