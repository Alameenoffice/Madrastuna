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
import com.example.ui.components.ThreeDIllustrationBox
import com.example.ui.components.ThreeDIllustrationType
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

    val summaries = remember(accounts, teacherRatings, currentUser) {
        viewModel.getTeacherEvaluationSummaries(accounts, teacherRatings, currentUser)
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
        // 1) Full-Width Seamless Connected Wave Header + #1 Top Teacher Podium (Zero Box Border, Zero White Gap)
        item {
            SeamlessEvaluationConnectedWaveHeader(
                totalTeachersCount = summaries.size,
                totalRatingsCount = teacherRatings.size,
                topSummary = topSummary
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
                        ThreeDIllustrationBox(
                            type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                            modifier = Modifier.size(88.dp)
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
                        ThreeDIllustrationBox(
                            type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                            modifier = Modifier.size(88.dp)
                        )

                        Text(
                            text = "تقييم الأستاذ(ة): ${targetTeacher.fullName}",
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

                        // Animated 3D Expression Badge above the 5 Stars
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
                                5 -> "٥ نجوم — عطاء تربوي استثنائي ومُلهم ★"
                                4 -> "٤ نجوم — أداء متميز وجهد مشكور ★"
                                3 -> "٣ نجوم — أداء جيد ومقدر ★"
                                2 -> "نجمتان — يحتاج لمزيد من المتابعة"
                                else -> "نجمة واحدة — بحاجة لتحسين التواصل التربوي"
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
 * Full-Width Seamless Connected Wave Header + #1 Top Teacher Podium
 * Zero yellow box border, zero white gap, edge-to-edge luxury 3D wave design.
 */
@Composable
private fun SeamlessEvaluationConnectedWaveHeader(
    totalTeachersCount: Int,
    totalRatingsCount: Int,
    topSummary: TeacherEvaluationSummary?
) {
    val infiniteTransition = rememberInfiniteTransition(label = "eval_wave_header_aura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.20f,
        targetValue = 0.48f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
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
                        Color(0xFF0F3D5E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 800f)
                )
            )
            .testTag("top_teacher_spotlight_podium_card")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFFFBBF24).copy(alpha = auraAlpha), Color.Transparent),
                    center = Offset(w * 0.20f, h * 0.45f),
                    radius = w * 0.52f
                ),
                radius = w * 0.52f,
                center = Offset(w * 0.20f, h * 0.45f)
            )

            val middleRibbon = Path().apply {
                moveTo(0f, h * 0.52f)
                cubicTo(w * 0.35f, h * 0.40f, w * 0.68f, h * 0.68f, w, h * 0.48f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = middleRibbon,
                color = Color(0xFF064E3B).copy(alpha = 0.38f)
            )

            val bottomGoldWave = Path().apply {
                moveTo(0f, h * 0.78f)
                cubicTo(w * 0.30f, h * 0.64f, w * 0.70f, h * 0.94f, w, h * 0.72f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = bottomGoldWave,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFBBF24).copy(alpha = 0.20f),
                        Color(0xFF34D399).copy(alpha = 0.14f),
                        Color(0xFFF59E0B).copy(alpha = 0.22f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 28.dp),
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
                    color = Color.White.copy(alpha = 0.18f)
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
                    color = Color.White.copy(alpha = 0.18f)
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

            // Main Hero Title + 3D Podium & Golden Star Award Illustration
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
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = "منصة شفافة لتقدير جهود الأساتذة المتميزين بتقييم مباشر وموثوق من أولياء الأمور",
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD1FAE5)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                    modifier = Modifier.size(96.dp)
                )
            }

            // Integrated #1 Top Teacher Podium Wave Strip (Frameless inside the connected wave)
            if (topSummary != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.16f),
                                    Color(0xFFFBBF24).copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFBBF24).copy(alpha = 0.28f)
                            ) {
                                Text(
                                    text = "المركز الأول #1 • وسام التميز التربوي ★",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF08A),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = "الأستاذ(ة): ${topSummary.teacher.fullName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 17.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = topSummary.teacher.specialty.ifBlank { "الكادر التدريسي المتميز" },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD1FAE5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color.White.copy(alpha = 0.20f)
                                ) {
                                    Text(
                                        text = String.format(Locale.US, "%.1f / 5.0 ★", topSummary.averageRating),
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFEF08A),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                                Text(
                                    text = "(${topSummary.totalRatingsCount} تقييم من أولياء الأمور)",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White.copy(alpha = 0.92f)
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
            text = "تم تحديث سجل التميز التربوي للأستاذ(ة) $teacherName في السحابة فوراً ★",
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

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(7.dp, RoundedCornerShape(22.dp))
            .testTag("teacher_eval_card_${teacher.id}"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(
            width = if (rank == 1 && summary.totalRatingsCount > 0) 1.6.dp else 1.dp,
            color = if (rank == 1 && summary.totalRatingsCount > 0) Color(0xFFF59E0B) else Color(0xFFE2E8F0)
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle bottom mint wave inside card
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                val wavePath = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(w * 0.35f, h * 0.68f, w * 0.70f, h * 0.92f, w, h * 0.75f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = wavePath,
                    color = Color(0xFFD1FAE5).copy(alpha = 0.35f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // 3D Rank Badge
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        if (rank == 1 && summary.totalRatingsCount > 0) {
                                            listOf(Color(0xFFF59E0B), Color(0xFFD97706))
                                        } else {
                                            listOf(Color(0xFF059669), Color(0xFF0F3D5E))
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rank",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF059669),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (teacher.assignedClasses.isNotBlank()) {
                                Text(
                                    text = "الصفوف: ${teacher.assignedClasses}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF64748B),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    // Score Pill formatted strictly as "4.8 / 5.0 ★"
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (summary.totalRatingsCount > 0) Color(0xFFFEF3C7) else Color(0xFFF1F5F9),
                        border = BorderStroke(
                            1.dp,
                            if (summary.totalRatingsCount > 0) Color(0xFFF59E0B) else Color(0xFFCBD5E1)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (summary.totalRatingsCount > 0) {
                                    String.format(Locale.US, "%.1f / 5.0 ★", summary.averageRating)
                                } else {
                                    "جديد"
                                },
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (summary.totalRatingsCount > 0) Color(0xFFB45309) else Color(0xFF64748B)
                            )
                            Text(
                                text = "${summary.totalRatingsCount} تقييم",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                // Animated Rating Bar
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
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
                                        listOf(Color(0xFF10B981), Color(0xFFF59E0B))
                                    )
                                )
                        )
                    }
                }

                // Top Praise Tags (Clean without ✨)
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
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
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
                                        fontSize = 10.5.sp,
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
                                    tint = Color(0xFFF59E0B),
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

                // Parent One-Time Rating Action / Locked State
                if (myRating != null) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.2.dp, Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("teacher_already_rated_badge_${teacher.id}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "تم تقييم الأستاذ ✓",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46)
                                    )
                                }
                                Text(
                                    text = "تقييمك: ${myRating.stars}.0 / 5.0 ★",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706)
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
                                .height(44.dp)
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
                                text = "تقييم الأستاذ ★",
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
