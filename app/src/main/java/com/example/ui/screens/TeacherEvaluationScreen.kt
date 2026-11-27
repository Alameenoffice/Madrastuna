package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeacherEvaluationSummary
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.EliteTeacherExcellenceMedalCanvas
import com.example.ui.components.GlobalImageLightboxController
import com.example.ui.components.SmartWelcomeProfileAvatar
import com.example.ui.components.TeacherEvaluationSpotlightCard
import com.example.ui.components.rankAvatarBadgeSpec
import com.example.ui.components.resolveSmartAvatarPersona
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin

private val SeamlessEvaluationHeroWaveShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h * 0.93f)
    cubicTo(
        w * 0.68f, h,
        w * 0.32f, h * 0.87f,
        0f, h * 0.95f
    )
    close()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherEvaluationScreen(
    viewModel: SchoolViewModel
) {
    val featureSettings by viewModel.featureSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val teacherRatings by viewModel.teacherRatings.collectAsState()
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val isAdmin = currentUser?.role == UserRole.ADMIN
    val isParent = currentUser?.role == UserRole.PARENT

    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val pureTeacherAccounts = remember(accounts, schoolConfig) {
        accounts.filterNot { acc ->
            acc.isDriverInSchoolConfig(schoolConfig) ||
                acc.assignedClasses.contains("route_", ignoreCase = true) ||
                acc.specialty.contains("سائق", ignoreCase = true) ||
                acc.username.contains("driver", ignoreCase = true)
        }
    }
    val summaries = remember(pureTeacherAccounts, teacherRatings, currentUser) {
        viewModel.getTeacherEvaluationSummaries(pureTeacherAccounts, teacherRatings, currentUser)
    }
    val topSummary = remember(summaries) {
        summaries.firstOrNull { it.totalRatingsCount > 0 }
    }

    var ratingTargetTeacher by remember { mutableStateOf<UserAccount?>(null) }
    var selectedStars by remember { mutableIntStateOf(5) }
    var lastTappedStar by remember { mutableIntStateOf(5) }
    var starRippleTrigger by remember { mutableIntStateOf(0) }
    var selectedPraiseTag by remember { mutableStateOf("شرح ممتاز ومبسط") }
    var customParentNote by remember { mutableStateOf("") }
    var showCelebrationState by remember { mutableStateOf(false) }

    val quickPraiseOptions = remember {
        listOf(
            "شرح ممتاز ومبسط",
            "متعاون جداً مع الطلبة",
            "قدوة تربوية رائعة",
            "متابعة يومية دقيقة"
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("teacher_evaluation_lazy_column"),
        contentPadding = PaddingValues(top = 0.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) Full-Width Seamless Deep Emerald Wave Header + Dynamic Spotlight + #1 Top Teacher Podium
        item {
            SeamlessEvaluationConnectedWaveHeader(
                totalTeachersCount = summaries.size,
                totalRatingsCount = teacherRatings.size,
                topSummary = topSummary,
                schoolGenderType = schoolConfig.schoolGenderType
            )
        }

        // 2) Principal Cloud Evaluation Controls (Admin Only: Switch 1 & Switch 2)
        if (isAdmin) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp))
                        .testTag("principal_teacher_evaluation_controls_card"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.2.dp, Color(0xFFA7F3D0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFD1FAE5),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.AdminPanelSettings,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(19.dp)
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تحكم المدير الكامل بنظام التقييم السحابي",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF064E3B)
                                )
                                Text(
                                    text = "تُحفظ الإعدادات فوراً في السحابة وتنعكس لدى جميع أولياء الأمور والمنتسبين",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF475569)
                                )
                            }
                        }

                        // Switch 1: تفعيل / إيقاف تقييم الكادر التدريسي لأولياء الأمور
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (featureSettings.teacherEvaluationEnabled) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (featureSettings.teacherEvaluationEnabled) Color(0xFF10B981) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "تفعيل / إيقاف تقييم الكادر التدريسي لأولياء الأمور",
                                        fontSize = 12.8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (featureSettings.teacherEvaluationEnabled) {
                                            "الحالة: مفتوح لأولياء الأمور (تقييم واحد فقط لكل أستاذ)"
                                        } else {
                                            "الحالة: مغلق حالياً من قِبل الإدارة"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featureSettings.teacherEvaluationEnabled) Color(0xFF047857) else Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = featureSettings.teacherEvaluationEnabled,
                                    onCheckedChange = { viewModel.toggleTeacherEvaluationEnabled(it) },
                                    modifier = Modifier.testTag("teacher_evaluation_enabled_switch")
                                )
                            }
                        }

                        // Switch 2: إظهار / إزالة المعلم الأعلى تقييماً من الشاشة الرئيسية
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (featureSettings.showTopTeacherOnHome) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                            border = BorderStroke(
                                1.dp,
                                if (featureSettings.showTopTeacherOnHome) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "إظهار / إزالة نخبة الأساتذة الأعلى تقييماً من الشاشة الرئيسية",
                                        fontSize = 12.8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (featureSettings.showTopTeacherOnHome) {
                                            "الحالة: يظهر المقطع المتموج لأفضل 3 أساتذة في الشاشة الرئيسية"
                                        } else {
                                            "الحالة: مخفي من الشاشة الرئيسية"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (featureSettings.showTopTeacherOnHome) Color(0xFFB45309) else Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = featureSettings.showTopTeacherOnHome,
                                    onCheckedChange = { viewModel.toggleShowTopTeacherOnHome(it) },
                                    modifier = Modifier.testTag("show_top_teacher_home_switch")
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notice Banner when Evaluation is Paused by Principal
        if (!featureSettings.teacherEvaluationEnabled) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.2.dp, Color(0xFFF59E0B).copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("teacher_evaluation_paused_notice")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PauseCircleFilled,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "تقييم الكادر التدريسي متوقف حالياً من قِبل إدارة المدرسة. يمكنك الاطلاع على لوحة التميز والتقييمات السابقة.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF92400E),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // 3) Clean Full-Width Section Title (Deleted cramped vertical side badge)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.WorkspacePremium,
                    contentDescription = null,
                    tint = Color(0xFF059669),
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = "قائمة الكادر التدريسي حسب التقييم",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        if (summaries.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EliteTeacherExcellenceMedalCanvas(
                            modifier = Modifier.size(84.dp)
                        )
                        Text(
                            text = "لا يوجد معلمون مسجلون حالياً في المدرسة",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "عند إضافة حسابات الأساتذة من قسم إدارة الحسابات، ستظهر بطاقاتهم التفاعلية هنا للتقييم والتكريم.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            itemsIndexed(summaries, key = { _, item -> item.teacher.id }) { index, summary ->
                ThreeDWaveTeacherEvaluationCard(
                    rank = index + 1,
                    summary = summary,
                    schoolGenderType = schoolConfig.schoolGenderType,
                    isParent = isParent,
                    isEvaluationEnabled = featureSettings.teacherEvaluationEnabled,
                    onClickRate = {
                        selectedStars = 5
                        lastTappedStar = 5
                        selectedPraiseTag = quickPraiseOptions.first()
                        customParentNote = ""
                        showCelebrationState = false
                        ratingTargetTeacher = summary.teacher
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // 4) Interactive Animated 3D Rating ModalBottomSheet with Written Note & 1.5s Sweet Celebration
    val targetTeacher = ratingTargetTeacher
    if (targetTeacher != null) {
        ModalBottomSheet(
            onDismissRequest = {
                if (!showCelebrationState) {
                    ratingTargetTeacher = null
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            val rippleProgress by animateFloatAsState(
                targetValue = if (starRippleTrigger > 0) 1f else 0f,
                animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
                label = "star_ripple_halo"
            )

            AnimatedContent(
                targetState = showCelebrationState,
                transitionSpec = {
                    (fadeIn(tween(320)) + scaleIn(initialScale = 0.90f, animationSpec = tween(320))) togetherWith
                        (fadeOut(tween(220)) + scaleOut(targetScale = 0.95f, animationSpec = tween(220)))
                },
                label = "rating_sheet_celebration_transition"
            ) { isCelebrating ->
                if (isCelebrating) {
                    SweetRatingCelebrationView(teacherName = targetTeacher.fullName)
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        EliteTeacherExcellenceMedalCanvas(
                            modifier = Modifier.size(82.dp)
                        )

                        Text(
                            text = "تقييم ${targetTeacher.honorificTeacherTitle}: ${targetTeacher.fullName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF064E3B),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = targetTeacher.specialty.ifBlank { "الكادر التدريسي" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857)
                        )

                        // Animated Expression Badge above the 5 Stars
                        AnimatedContent(
                            targetState = selectedStars,
                            transitionSpec = {
                                (scaleIn(
                                    initialScale = 0.85f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
                                ) + fadeIn(tween(220))) togetherWith (fadeOut(tween(150)))
                            },
                            label = "star_expression_badge_anim"
                        ) { starVal ->
                            val badgeText = when (starVal) {
                                5 -> "5 من 5 — عطاء تربوي استثنائي ومُلهم"
                                4 -> "4 من 5 — أداء متميز وجهد مشكور"
                                3 -> "3 من 5 — أداء جيد ومقدر"
                                2 -> "2 من 5 — يحتاج لمزيد من المتابعة"
                                else -> "1 من 5 — بحاجة لتحسين التواصل التربوي"
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFFFBEB),
                                border = BorderStroke(1.2.dp, Color(0xFFF59E0B))
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFB45309),
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }

                        // Interactive 5-Star 3D Bouncing Selector with Golden Ripple Halo Canvas Effect
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            for (star in 1..5) {
                                val isFilled = star <= selectedStars
                                val isRecentlyTapped = star == lastTappedStar && starRippleTrigger > 0
                                val starScale by animateFloatAsState(
                                    targetValue = if (isFilled) 1.18f else 0.92f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "star_scale_$star"
                                )
                                val starTilt by animateFloatAsState(
                                    targetValue = if (isFilled) 0f else -8f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "star_tilt_$star"
                                )

                                Box(
                                    modifier = Modifier
                                        .padding(horizontal = 5.dp)
                                        .size(54.dp)
                                        .graphicsLayer {
                                            scaleX = starScale
                                            scaleY = starScale
                                            rotationZ = starTilt
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            if (isFilled) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                                        )
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            selectedStars = star
                                            lastTappedStar = star
                                            starRippleTrigger++
                                        }
                                        .testTag("select_rating_star_$star"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isFilled) {
                                        Canvas(modifier = Modifier.matchParentSize()) {
                                            val r = (size.minDimension * 0.48f) * (0.7f + 0.3f * rippleProgress)
                                            drawCircle(
                                                brush = Brush.radialGradient(
                                                    listOf(
                                                        Color(0xFFFBBF24).copy(alpha = if (isRecentlyTapped) 0.42f else 0.22f),
                                                        Color.Transparent
                                                    )
                                                ),
                                                radius = r
                                            )
                                            if (isRecentlyTapped) {
                                                drawCircle(
                                                    color = Color(0xFFF59E0B).copy(alpha = (1f - rippleProgress) * 0.65f),
                                                    radius = size.minDimension * 0.46f,
                                                    style = Stroke(width = 2.dp.toPx())
                                                )
                                            }
                                        }
                                    }
                                    Icon(
                                        imageVector = if (isFilled) Icons.Filled.Star else Icons.Filled.StarBorder,
                                        contentDescription = "$star نجوم",
                                        tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                        }

                        // Selectable Quick Praise Capsules (Strictly WITHOUT ✨ or AutoAwesome)
                        Text(
                            text = "اختر وسام الإشادة التربوية للأستاذ:",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth()
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            maxItemsInEachRow = 2
                        ) {
                            quickPraiseOptions.forEach { tag ->
                                val isTagSelected = selectedPraiseTag == tag
                                val pillScale by animateFloatAsState(
                                    targetValue = if (isTagSelected) 1.03f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                    label = "praise_pill_scale_$tag"
                                )
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .scale(pillScale)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable {
                                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            selectedPraiseTag = tag
                                        },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (isTagSelected) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                                    border = BorderStroke(
                                        width = if (isTagSelected) 1.8.dp else 1.dp,
                                        color = if (isTagSelected) Color(0xFF059669) else Color(0xFFCBD5E1)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isTagSelected) Icons.Filled.CheckCircle else Icons.Filled.Verified,
                                            contentDescription = null,
                                            tint = if (isTagSelected) Color(0xFF059669) else Color(0xFF64748B),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = tag,
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isTagSelected) Color(0xFF065F46) else Color(0xFF334155),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        // Mandatory Written Note Field (note: String)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "اكتب ملاحظتك أو رسالتك التربوية حول الأستاذ (اختياري):",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            OutlinedTextField(
                                value = customParentNote,
                                onValueChange = { customParentNote = it },
                                placeholder = {
                                    Text(
                                        text = "مثال: نشكر الأستاذ على متابعته الحريصة وتواصله المستمر...",
                                        fontSize = 12.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                },
                                minLines = 3,
                                maxLines = 4,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF059669),
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("teacher_rating_written_note_input")
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "تنبيه: يحق لولي الأمر تقييم كل أستاذ مرة واحدة فقط لضمان النزاهة والعدالة.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(10.dp)
                            )
                        }

                        Button(
                            onClick = {
                                val ok = viewModel.submitTeacherRatingOnce(
                                    teacherId = targetTeacher.id,
                                    stars = selectedStars,
                                    praiseTag = selectedPraiseTag,
                                    note = customParentNote
                                )
                                if (ok) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    showCelebrationState = true
                                    scope.launch {
                                        delay(1500L)
                                        showCelebrationState = false
                                        ratingTargetTeacher = null
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("submit_teacher_rating_confirm_btn"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.Verified, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "اعتماد وإرسال التقييم",
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Full-Width Deep Harmonious Emerald Wave Header + Dynamic Spotlight + Frosted #1 Podium
 * Matching the home screen aesthetic: soft bottom wave + thin luminous golden thread (no brownish/olive gradient).
 */
@Composable
private fun SeamlessEvaluationConnectedWaveHeader(
    totalTeachersCount: Int,
    totalRatingsCount: Int,
    topSummary: TeacherEvaluationSummary?,
    schoolGenderType: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eval_wave_header_aura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.14f,
        targetValue = 0.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eval_aura_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(SeamlessEvaluationHeroWaveShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF047857),
                        Color(0xFF065F46),
                        Color(0xFF022C22)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(900f, 950f)
                )
            )
            .testTag("top_teacher_spotlight_podium_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Soft emerald-mint ambient highlight behind spotlight widget
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(
                        Color(0xFF34D399).copy(alpha = auraAlpha),
                        Color(0xFFFEF08A).copy(alpha = auraAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * 0.40f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.22f, h * 0.40f)
            )

            // Calm deep emerald bottom wave (zero brownish/olive tone)
            val bottomEmeraldWave = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(w * 0.34f, h * 0.62f, w * 0.68f, h * 0.86f, w, h * 0.70f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomEmeraldWave,
                color = Color(0xFF022C22).copy(alpha = 0.34f)
            )

            // Thin luminous golden-emerald thread along the bottom wave contour (like Home screen)
            val goldenThread = Path().apply {
                moveTo(0f, h * 0.945f)
                cubicTo(w * 0.32f, h * 0.865f, w * 0.68f, h * 0.995f, w, h * 0.925f)
            }
            drawPath(
                path = goldenThread,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF34D399).copy(alpha = 0.35f),
                        Color(0xFFFEF08A).copy(alpha = 0.85f),
                        Color(0xFFF59E0B).copy(alpha = 0.65f),
                        Color(0xFF34D399).copy(alpha = 0.35f)
                    )
                ),
                style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Pills Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.School,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$totalTeachersCount أستاذ مسجل",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(0.8.dp, Color.White.copy(alpha = 0.24f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFFFEF08A),
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "$totalRatingsCount تقييم معتمد",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                    }
                }
            }

            // Main Hero Title + Dynamic Section Spotlight Card on Left (Trailing in RTL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "تقييم وتميّز الكادر التدريسي",
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 20.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "منصة شفافة لتقدير جهود الأساتذة المتميزين بتقييم مباشر وموثوق من أولياء الأمور",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFD1FAE5)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TeacherEvaluationSpotlightCard()
            }

            // Refined Frosted-Glass #1 Top Teacher Card with SmartWelcomeProfileAvatar & Thin Golden Vertical Accent
            if (topSummary != null) {
                val topPersona = remember(topSummary.teacher.gender, schoolGenderType, topSummary.teacher.fullName) {
                    resolveSmartAvatarPersona(
                        role = UserRole.TEACHER,
                        gender = topSummary.teacher.gender,
                        schoolGenderType = schoolGenderType,
                        fullName = topSummary.teacher.fullName
                    )
                }
                val topRankBadge = remember { rankAvatarBadgeSpec(1) }
                val cleanTopRating = remember(topSummary.averageRating, topSummary.totalRatingsCount) {
                    "${String.format(Locale.US, "%.1f", topSummary.averageRating)} من 5.0 • ${topSummary.totalRatingsCount} تقييم"
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Min)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Thin vertical golden accent strip
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFF6EE7B7))
                                    )
                                )
                        )

                        SmartWelcomeProfileAvatar(
                            photoSource = topSummary.teacher.photoBase64,
                            contentDescription = topSummary.teacher.fullName,
                            persona = topPersona,
                            statusBadge = topRankBadge,
                            onClick = {
                                if (topSummary.teacher.photoBase64.isNotBlank()) {
                                    GlobalImageLightboxController.open(
                                        topSummary.teacher.photoBase64,
                                        topSummary.teacher.fullName
                                    )
                                }
                            },
                            avatarSize = 58.dp,
                            showStatusBadge = true
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFEF08A).copy(alpha = 0.20f),
                                border = BorderStroke(0.8.dp, Color(0xFFFEF08A).copy(alpha = 0.50f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.WorkspacePremium,
                                        contentDescription = null,
                                        tint = Color(0xFFFEF08A),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = if (topSummary.teacher.isFemale) "المتصدرة الأولى • وسام التميز" else "المتصدر الأول • وسام التميز",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFEF08A),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Text(
                                text = "${topSummary.teacher.honorificTeacherTitle}: ${topSummary.teacher.fullName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = topSummary.teacher.specialty.ifBlank { "الكادر التدريسي المتميز" },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFD1FAE5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFFEF08A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = cleanTopRating,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFFFBEB),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
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
 * 1.5-second Sweet Submission Celebration Animation inside the Rating ModalBottomSheet
 */
@Composable
private fun SweetRatingCelebrationView(teacherName: String) {
    val progressAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    LaunchedEffect(Unit) {
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing)
        )
    }
    val p = progressAnim.value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(150.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val maxR = size.minDimension * 0.48f

                // Gentle Golden & Emerald Particles Bursting Outward
                for (i in 0 until 14) {
                    val angle = Math.toRadians((i * (360.0 / 14.0)) + (p * 35.0))
                    val dist = maxR * (0.35f + 0.65f * p)
                    val px = cx + (dist * cos(angle)).toFloat()
                    val py = cy + (dist * sin(angle)).toFloat() - (p * 12.dp.toPx())
                    val particleColor = if (i % 2 == 0) Color(0xFFFBBF24) else Color(0xFF10B981)
                    drawCircle(
                        color = particleColor.copy(alpha = (1f - p * 0.55f).coerceIn(0f, 1f)),
                        radius = (5.5f - p * 2f).dp.toPx(),
                        center = Offset(px, py)
                    )
                }

                // Glowing 3D Emerald Circle
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B))
                    ),
                    radius = maxR * 0.54f,
                    center = Offset(cx, cy)
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = maxR * 0.54f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Animated Checkmark inside the 3D Circle
                val checkPath = Path().apply {
                    moveTo(cx - maxR * 0.22f, cy + maxR * 0.02f)
                    lineTo(cx - maxR * 0.05f, cy + maxR * 0.18f)
                    lineTo(cx + maxR * 0.24f, cy - maxR * 0.14f)
                }
                drawPath(
                    path = checkPath,
                    color = Color.White,
                    style = Stroke(
                        width = 4.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )
            }
        }

        Text(
            text = "شكراً لك! تم توثيق تقييمك وملاحظتك بنجاح",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF065F46),
            textAlign = TextAlign.Center
        )

        Text(
            text = "تم تحديث سجل التميز التربوي للأستاذ(ة) $teacherName في السحابة فوراً",
            fontSize = 12.5.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF475569),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThreeDWaveTeacherEvaluationCard(
    rank: Int,
    summary: TeacherEvaluationSummary,
    schoolGenderType: String,
    isParent: Boolean,
    isEvaluationEnabled: Boolean,
    onClickRate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val teacher = summary.teacher
    val myRating = summary.myParentRating
    val ratingFraction = (summary.averageRating / 5f).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = ratingFraction,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "teacher_rating_progress_${teacher.id}"
    )
    val teacherPersona = remember(teacher.gender, schoolGenderType, teacher.fullName) {
        resolveSmartAvatarPersona(
            role = UserRole.TEACHER,
            gender = teacher.gender,
            schoolGenderType = schoolGenderType,
            fullName = teacher.fullName
        )
    }
    val rankBadge = remember(rank) { rankAvatarBadgeSpec(rank) }
    val isTopRanked = rank == 1 && summary.totalRatingsCount > 0

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isTopRanked) 6.dp else 3.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF064E3B).copy(alpha = 0.09f),
                spotColor = Color(0xFF064E3B).copy(alpha = 0.13f)
            )
            .testTag("teacher_eval_card_${teacher.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (isTopRanked) 1.4.dp else 1.dp,
            color = if (isTopRanked) Color(0xFFD97706).copy(alpha = 0.36f) else Color(0xFFA7F3D0).copy(alpha = 0.85f)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Very soft subtle mint corner sheen
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.84f)
                    cubicTo(w * 0.35f, h * 0.76f, w * 0.70f, h * 0.94f, w, h * 0.82f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wavePath,
                    color = Color(0xFFECFDF5).copy(alpha = 0.55f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header Row: SmartWelcomeProfileAvatar (with refined rank badge) + Teacher Details + Score Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SmartWelcomeProfileAvatar(
                        photoSource = teacher.photoBase64,
                        contentDescription = teacher.fullName,
                        persona = teacherPersona,
                        statusBadge = rankBadge,
                        onClick = {
                            if (teacher.photoBase64.isNotBlank()) {
                                GlobalImageLightboxController.open(teacher.photoBase64, teacher.fullName)
                            }
                        },
                        avatarSize = 58.dp,
                        showStatusBadge = true
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = teacher.fullName,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 15.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = teacher.specialty.ifBlank { "الكادر التدريسي" },
                            fontSize = 12.2.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (teacher.assignedClasses.isNotBlank()) {
                            Text(
                                text = "الصفوف: ${teacher.assignedClasses}",
                                fontSize = 11.2.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Natural Arabic Score Capsule ("2.0 من 5.0")
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (summary.totalRatingsCount > 0) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            1.dp,
                            if (summary.totalRatingsCount > 0) Color(0xFFF59E0B).copy(alpha = 0.42f) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = if (summary.totalRatingsCount > 0) Color(0xFFD97706) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (summary.totalRatingsCount > 0) {
                                        String.format(Locale.US, "%.1f من 5.0", summary.averageRating)
                                    } else {
                                        "جديد"
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (summary.totalRatingsCount > 0) Color(0xFF92400E) else Color(0xFF64748B),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                            Text(
                                text = "${summary.totalRatingsCount} تقييم",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Slim Platform-Gradient Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFFF1F5F9))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgress)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF059669), Color(0xFF10B981), Color(0xFFD97706))
                                )
                            )
                    )
                }

                // Clean Praise Chips
                if (summary.topPraiseTags.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        summary.topPraiseTags.forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFECFDF5),
                                border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.5.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Text(
                                        text = tag,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                            }
                        }
                    }
                }

                // Recent Parent Written Notes (if any)
                if (summary.recentParentNotes.isNotEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "رسائل وملاحظات أولياء الأمور الموثقة:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857)
                        )
                        summary.recentParentNotes.take(2).forEach { (parentName, noteText) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FormatQuote,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$noteText — ($parentName)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF334155),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Parent One-Time Rating Status Strip or Rate CTA
                if (myRating != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFF0FDF4),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_already_rated_badge_${teacher.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = "تم تقييم الأستاذ • تقييمك: ${myRating.stars}.0 من 5.0",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = Color(0xFFD97706),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (myRating.note.isNotBlank()) {
                                Text(
                                    text = "ملاحظتك: \"${myRating.note}\"",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF047857)
                                )
                            }
                        }
                    }
                } else if (isParent) {
                    if (isEvaluationEnabled) {
                        Button(
                            onClick = onClickRate,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("rate_teacher_btn_${teacher.id}"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = Color(0xFFFEF08A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تقييم الأستاذ",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "التقييم متوقف حالياً من قِبل الإدارة",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
