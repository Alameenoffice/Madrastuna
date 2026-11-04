package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TeacherEvaluationSummary
import com.example.model.UserAccount
import com.example.model.UserRole
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.ThreeDIllustrationBox
import com.example.ui.components.ThreeDIllustrationType
import com.example.viewmodel.SchoolViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TeacherEvaluationScreen(
    viewModel: SchoolViewModel
) {
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val featureSettings by viewModel.featureSettings.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val accounts by viewModel.accounts.collectAsState()
    val teacherRatings by viewModel.teacherRatings.collectAsState()

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
    var selectedPraiseTag by remember { mutableStateOf("شرح ممتاز ومبسط") }

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
        contentPadding = PaddingValues(top = 10.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1) 3D Wave Hero Header with Exclusive TEACHER_EXCELLENCE_CROWN_3D Animation
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ThreeDHeroHeaderCard(
                    title = "تقييم وتميّز الكادر التدريسي",
                    subtitle = "منصة شفافة لتقدير جهود الأساتذة المتميزين بتقييم مباشر وموثوق من أولياء الأمور",
                    rightPillText = "${summaries.size} أستاذ مسجل",
                    rightPillIcon = Icons.Filled.School,
                    leftPillText = "${teacherRatings.size} تقييم معتمد",
                    leftPillIcon = Icons.Filled.Stars,
                    illustrationType = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D
                )
            }
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
                                        text = "إظهار / إزالة المعلم الأعلى تقييماً من الشاشة الرئيسية",
                                        fontSize = 12.8.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = if (featureSettings.showTopTeacherOnHome) {
                                            "الحالة: يظهر المقطع المتموج للمعلم الأول في الشاشة الرئيسية"
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

        // 3) #1 Top Teacher Spotlight Podium (منصة نجم الكادر التدريسي الأول)
        if (topSummary != null) {
            item {
                TopTeacherPodiumCard(
                    summary = topSummary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // 4) Section Title for Ranked Teachers
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
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
                        color = Color(0xFF0F172A)
                    )
                }
                if (isParent) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFD1FAE5)
                    ) {
                        Text(
                            text = "تقييم واحد لكل أستاذ",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
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
                    isParent = isParent,
                    isEvaluationEnabled = featureSettings.teacherEvaluationEnabled,
                    onClickRate = {
                        selectedStars = 5
                        selectedPraiseTag = quickPraiseOptions.first()
                        ratingTargetTeacher = summary.teacher
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    // 5) Interactive Animated 3D Rating ModalBottomSheet
    val targetTeacher = ratingTargetTeacher
    if (targetTeacher != null) {
        ModalBottomSheet(
            onDismissRequest = { ratingTargetTeacher = null },
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                    modifier = Modifier.size(86.dp)
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

                // Interactive 5-Star 3D Bouncing Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (star in 1..5) {
                        val isFilled = star <= selectedStars
                        val starScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.18f else 0.94f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessLow
                            ),
                            label = "star_scale_$star"
                        )
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 6.dp)
                                .size(50.dp)
                                .scale(starScale)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) Color(0xFFFEF3C7) else Color(0xFFF1F5F9)
                                )
                                .clickable { selectedStars = star }
                                .testTag("select_rating_star_$star"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFilled) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star نجوم",
                                tint = if (isFilled) Color(0xFFF59E0B) else Color(0xFF94A3B8),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                Text(
                    text = when (selectedStars) {
                        5 -> "٥ نجوم — أداء تربوي وعلمي استثنائي ⭐"
                        4 -> "٤ نجوم — أداء متميز جداً وجهد مشكور"
                        3 -> "٣ نجوم — أداء جيد"
                        2 -> "نجمتان — يحتاج إلى مزيد من المتابعة"
                        else -> "نجمة واحدة — بحاجة إلى تحسين"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFB45309)
                )

                // Selectable Quick Praise Capsules
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
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { selectedPraiseTag = tag },
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
                                    imageVector = if (isTagSelected) Icons.Filled.CheckCircle else Icons.Filled.AutoAwesome,
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
                            praiseTag = selectedPraiseTag
                        )
                        if (ok) {
                            ratingTargetTeacher = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
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
                        text = "اعتماد التقييم النهائي",
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun TopTeacherPodiumCard(
    summary: TeacherEvaluationSummary,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "top_teacher_podium_aura")
    val auraAlpha by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura_alpha"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(26.dp))
            .testTag("top_teacher_spotlight_podium_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.8.dp, Color(0xFFFBBF24))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF064E3B),
                            Color(0xFF047857),
                            Color(0xFF0F3D5E)
                        )
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFBBF24).copy(alpha = auraAlpha), Color.Transparent),
                        center = Offset(w * 0.22f, h * 0.5f),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(w * 0.22f, h * 0.5f)
                )
                val wave = Path().apply {
                    moveTo(0f, h * 0.72f)
                    cubicTo(w * 0.35f, h * 0.52f, w * 0.68f, h * 0.92f, w, h * 0.64f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = wave, color = Color.White.copy(alpha = 0.07f))
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFFBBF24).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFFEF08A))
                    ) {
                        Text(
                            text = "المركز الأول #1 • درع التميز التربوي ⭐",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFFEF08A),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Text(
                        text = "الأستاذ(ة): ${summary.teacher.fullName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )

                    Text(
                        text = summary.teacher.specialty.ifBlank { "الكادر التدريسي المتميز" },
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD1FAE5)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = String.format(Locale.US, "⭐ %.1f / 5.0", summary.averageRating),
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFEF08A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "(${summary.totalRatingsCount} تقييم من أولياء الأمور)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.90f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                    modifier = Modifier.size(84.dp)
                )
            }
        }
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
                        // 3D Rank & Initials Badge
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

                    // Score Pill
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
                                    String.format(Locale.US, "⭐ %.1f", summary.averageRating)
                                } else {
                                    "جديد"
                                },
                                fontSize = 13.sp,
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

                // Top Praise Tags
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
                                Text(
                                    text = "✨ $tag",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF065F46),
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 9.dp),
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
                                text = "تقييمك: ${"★".repeat(myRating.stars)}",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFD97706)
                            )
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
                                text = "تقييم الأستاذ ⭐",
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
