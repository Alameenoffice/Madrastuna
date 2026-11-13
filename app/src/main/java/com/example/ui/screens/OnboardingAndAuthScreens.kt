package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.example.model.*
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.ExternalIntents
import com.example.util.SecurityShieldHelper
import com.example.viewmodel.SchoolViewModel

data class OnboardingSlideData(
    val badge: String,
    val title: String,
    val description: String,
    val highlights: List<String>
)

@Composable
fun OnboardingScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onFinishOnboarding: () -> Unit,
    occasionThemeConfig: OccasionThemeConfig = OccasionThemeConfig(),
    discoveredSchoolName: String? = null,
    schoolLogoBase64: String = ""
) {
    val slides = remember {
        listOf(
            OnboardingSlideData(
                badge = "الشريحة 1 من 3 • منصة مدرستي المتكاملة",
                title = "منصة مدرستي الذكية بهوية عصرية",
                description = "منصة رقمية شاملة تربط الأسرة التربوية، الكادر التدريسي، أولياء الأمور، والطلبة في بيئة تعليمية موحدة وآمنة.",
                highlights = listOf(
                    "بيئة تعليمية وتربوية تفاعلية تجمع الطلبة والأساتذة وأولياء الأمور",
                    "متابعة متكاملة لكافة المراحل والصفوف الدراسية",
                    "إشعارات فورية وتواصل مباشر مع أولياء الأمور"
                )
            ),
            OnboardingSlideData(
                badge = "الشريحة 2 من 3 • المتابعة اليومية الدقيقة",
                title = "رصد الحضور والواجبات والدرجات لحظياً",
                description = "متابعة يومية دقيقة لسجل الحضور والغياب، جدول الحصص الأسبوعي، التحضير اليومي، وكشف الدرجات التفصيلي مع حساب السعي والمعدل تلقائياً.",
                highlights = listOf(
                    "تنبيه فوري لولي الأمر عند تسجيل غياب أو تأخر",
                    "نشر الواجبات البيتية والتحضير اليومي لكل صف وشعبة",
                    "كشف درجات متكامل يحسب السعي السنوي والتقدير آلياً"
                )
            ),
            OnboardingSlideData(
                badge = "الشريحة 3 من 3 • مجتمع تفاعلي ومنصة تواصل",
                title = "مجتمع تفاعلي ومنصة تواصل اجتماعي تربوية",
                description = "مساحة اجتماعية آمنة وراقية تجمع الطلبة والأساتذة لمشاركة الإبداعات العلمية والفنية، التفاعل الإيجابي، ومتابعة الفعاليات والأخبار المدرسية لحظة بلحظة.",
                highlights = listOf(
                    "منصة تواصل اجتماعي طلابية لمشاركة المواهب والمشاريع العلمية والفنية",
                    "تفاعل حي ومباشر (إعجاب، تعليقات، ومتابعة للإبداعات المدرسية)",
                    "متابعة فورية لليوم الدراسي، الجداول، الواجبات، والفعاليات المصورة"
                )
            )
        )
    }

    var currentSlide by remember { mutableIntStateOf(0) }

    SoftWaveMeshBackground(isDarkMode = isDarkMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Bar: School Platform Capsule + Theme Toggle + Skip Capsule
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF059669).copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.School,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "منصة مدرستي الذكية",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            onClick = onToggleDarkMode,
                            shape = CircleShape,
                            color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                            border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("onboarding_theme_toggle")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "تبديل الوضع الليلي والنهاري",
                                    tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        if (currentSlide < slides.lastIndex) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onFinishOnboarding() }
                                    .testTag("skip_onboarding_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.FastForward,
                                        contentDescription = "تخطي",
                                        tint = Color(0xFF0F3D5E),
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "تخطي",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F3D5E),
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                AnimatedContent(
                    targetState = currentSlide,
                    label = "onboarding_slide_transition"
                ) { index ->
                    val slide = slides[index]
                    val slideIllustration = when (index) {
                        0 -> ThreeDIllustrationType.BOOKS_GRADUATION_CAP
                        1 -> ThreeDIllustrationType.ATTENDANCE_GATE_CLOCK
                        else -> ThreeDIllustrationType.SOCIAL_COMMUNITY_3D
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // 3D Seamless Wave Hero Section for the active Onboarding Slide (without "الشريحة X من 3" badge or duplicate middle box)
                        ThreeDHeroHeaderCard(
                            title = slide.title,
                            subtitle = slide.description,
                            rightPillText = "",
                            leftPillText = "منصة مدرستي الذكية",
                            leftPillIcon = Icons.Filled.VerifiedUser,
                            illustrationType = slideIllustration
                        )

                        // 3D Glassmorphic Highlights Card
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(
                                    elevation = 10.dp,
                                    shape = RoundedCornerShape(24.dp),
                                    ambientColor = Color(0xFF059669).copy(alpha = 0.16f),
                                    spotColor = Color(0xFF0F3D5E).copy(alpha = 0.18f)
                                ),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                            ),
                            border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                slide.highlights.forEach { point ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color(0xFFD1FAE5),
                                            modifier = Modifier.size(30.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                Icon(
                                                    imageVector = Icons.Filled.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = point,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slides.indices.forEach { idx ->
                            val active = idx == currentSlide
                            Box(
                                modifier = Modifier
                                    .height(9.dp)
                                    .width(if (active) 32.dp else 10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (active) Color(0xFF059669)
                                        else Color(0xFF94A3B8).copy(alpha = 0.45f)
                                    )
                                    .clickable { currentSlide = idx }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (currentSlide > 0) {
                            OutlinedButton(
                                onClick = { currentSlide-- },
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 50.dp),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.4.dp, Color(0xFF059669))
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "السابق",
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "السابق",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF047857),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Button(
                            onClick = {
                                if (currentSlide < slides.lastIndex) {
                                    currentSlide++
                                } else {
                                    onFinishOnboarding()
                                }
                            },
                            modifier = Modifier
                                .weight(if (currentSlide > 0) 1.5f else 1f)
                                .heightIn(min = 50.dp)
                                .testTag("start_now_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "البدء الآن",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (currentSlide == slides.lastIndex) "البدء الآن" else "التالي",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

/**
 * First-time Platform Initialization Screen:
 * "تأسيس منصة المدرسة وإنشاء حساب المدير"
 * Includes SchoolType (أهلية / حكومية) AND SchoolStage (ابتدائية / متوسطة / إعدادية / ثانوية)
 * with automatic generation of Registered Classes & Sections.
 */
@Composable
fun SchoolSetupScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onCancelSetup: () -> Unit = {},
    onCompleteSetupWithAffiliation: ((
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String,
        schoolStage: SchoolStage,
        ministryAffiliation: String,
        schoolGenderType: String
    ) -> Unit)? = null,
    onCompleteSetup: (
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String,
        schoolStage: SchoolStage
    ) -> Unit = { _, _, _, _, _, _, _, _ -> }
) {
    BackHandler { onCancelSetup() }

    var schoolName by remember { mutableStateOf("مدرستي النموذجية") }
    var address by remember { mutableStateOf("") }
    var selectedSchoolType by remember { mutableStateOf(SchoolType.PRIVATE) }
    var selectedSchoolStage by remember { mutableStateOf(SchoolStage.SECONDARY) }
    var selectedMinistryAffiliation by remember { mutableStateOf("وزارة التربية") }
    var selectedSchoolGenderType by remember { mutableStateOf("مدرسة بنين") }
    var logoBase64 by remember { mutableStateOf("") }
    var adminFullName by remember { mutableStateOf("المدير") }
    var adminGender by remember { mutableStateOf("") }
    var adminAgeYears by remember { mutableStateOf(0) }
    var adminPhone by remember { mutableStateOf("") }
    var adminPhoneKorekOverride by remember { mutableStateOf(false) }
    var adminUsername by remember { mutableStateOf("admin") }
    var adminPassword by remember { mutableStateOf("123456") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onCancelSetup,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("cancel_school_setup_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    NoWrapBadge(
                        text = "تأسيس مدرسة جديدة • إعداد المدير",
                        icon = Icons.Filled.AdminPanelSettings,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "تبديل المظهر",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Base64AvatarImage(
                base64Data = logoBase64,
                fallbackIcon = Icons.Filled.School,
                contentDescription = "شعار المدرسة",
                size = 76.dp,
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "تأسيس منصة المدرسة وإنشاء حساب المدير",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "أدخل البيانات الرسمية للمدرسة وحدد المرحلة الدراسية لتوليد الصفوف والشعب تلقائياً",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "1. البيانات الأساسية للمدرسة",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = schoolName,
                        onValueChange = { schoolName = it },
                        label = { Text("اسم المدرسة الرسمي") },
                        placeholder = { Text("مثال: مدرسة النور الأهلية") },
                        leadingIcon = { Icon(Icons.Filled.School, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_school_name_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("العنوان الجغرافي للمدرسة") },
                        placeholder = { Text("أدخل العنوان الجغرافي (اختياري)") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_school_address_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // School Type Selector (مدرسة أهلية / مدرسة حكومية)
                    Text(
                        text = "نوع المدرسة (يحدد تفعيل أو إخفاء نظام الأقساط تلقائياً):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SchoolType.entries.forEach { type ->
                            val isSelected = selectedSchoolType == type
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedSchoolType = type }
                                    .testTag("school_type_${type.name}"),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (type == SchoolType.PRIVATE) Icons.Filled.AccountBalance else Icons.Filled.AssuredWorkload,
                                        contentDescription = type.arabicLabel,
                                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = type.arabicLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // School Stage Selector (مدرسة ابتدائية / متوسطة / إعدادية / ثانوية)
                    Text(
                        text = "المرحلة الدراسية (توليد الصفوف والشعب المسجلة تلقائياً):",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SchoolStage.entries.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                pair.forEach { stage ->
                                    val isSelected = selectedSchoolStage == stage
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(14.dp))
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline,
                                                shape = RoundedCornerShape(14.dp)
                                            )
                                            .clickable { selectedSchoolStage = stage }
                                            .testTag("school_stage_${stage.name}"),
                                        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Class,
                                                contentDescription = stage.arabicLabel,
                                                tint = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = stage.arabicLabel,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                                maxLines = 1,
                                                softWrap = false,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Official School Affiliation Selector (الجهة الرسمية التابعة لها المدرسة)
                    Text(
                        text = "الجهة الرسمية التابعة لها المدرسة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("وزارة التربية", "التعليم الإسلامي - الوقف السني").forEach { affiliation ->
                            val isSelected = selectedMinistryAffiliation == affiliation
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedMinistryAffiliation = affiliation }
                                    .testTag("school_affiliation_$affiliation"),
                                color = if (isSelected) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccountBalance,
                                        contentDescription = affiliation,
                                        tint = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = affiliation,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }

                    // School Gender Type Selector (جنس المدرسة: بنين / بنات / مختلطة)
                    Text(
                        text = "جنس المدرسة:",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("مدرسة بنين", "مدرسة بنات", "مدرسة مختلطة").forEach { genderOption ->
                            val isSelected = selectedSchoolGenderType == genderOption
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) Color(0xFF059669) else MaterialTheme.colorScheme.outline,
                                        shape = RoundedCornerShape(14.dp)
                                    )
                                    .clickable { selectedSchoolGenderType = genderOption }
                                    .testTag("school_gender_$genderOption"),
                                color = if (isSelected) Color(0xFFECFDF5) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = genderOption,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Preview of Auto-Generated Classes & Sections
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "الصفوف المسجلة المتولدة تلقائياً (الشعب: أ، ب، ج):",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(selectedSchoolStage.defaultClasses) { clsName ->
                                    NoWrapBadge(
                                        text = clsName,
                                        icon = Icons.Filled.CheckCircle,
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    CompressedPhotoPickerButton(
                        label = "رفع شعار المدرسة",
                        currentBase64 = logoBase64,
                        onImageCompressed = { logoBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "2. بيانات حساب المدير",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = adminFullName,
                        onValueChange = { adminFullName = it },
                        label = { Text("اسم المدير") },
                        placeholder = { Text("أدخل الاسم الثلاثي للمدير") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    GenderAndAgeSegmentedSelectorCard(
                        selectedGender = adminGender,
                        onGenderChange = { newGender: String ->
                            adminGender = newGender
                        },
                        ageYears = adminAgeYears,
                        onAgeYearsChange = { newAge: Int ->
                            adminAgeYears = newAge
                        },
                        title = "الجنس والعمر"
                    )

                    SmartIraqiPhoneInputField(
                        value = adminPhone,
                        onValueChange = { adminPhone = it },
                        label = "رقم هاتف المدير / الإدارة (11 رقماً)",
                        korekOverrideApproved = adminPhoneKorekOverride,
                        onKorekOverrideChange = { adminPhoneKorekOverride = it },
                        testTag = "setup_admin_phone_input"
                    )

                    OutlinedTextField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        label = { Text("اسم المستخدم لحساب المدير (يُضاف كود المدرسة الثلاثي تلقائياً)") },
                        placeholder = { Text("admin أو 1") },
                        leadingIcon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
                        supportingText = {
                            Text(
                                text = "سيتم توليد كود مدرسة ثلاثي فريد (مثال: 105) ليصبح اسم المستخدم تلقائياً: 105-${adminUsername.ifBlank { "admin" }}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_admin_username_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("كلمة المرور لحساب الإدارة") },
                        leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "إظهار كلمة المرور"
                                )
                            }
                        },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("setup_admin_password_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    if (errorMessage != null) {
                        Surface(
                            color = Color(0xFFFDE8E8),
                            border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFF9B1C1C)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = Color(0xFF9B1C1C),
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    VectorActionButton(
                        text = "تأسيس المنصة واعتماد الصفوف وحساب المدير",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            val (phoneOk, phoneErr) = IraqiPhoneCarrierValidator.validateForSave(
                                phone = adminPhone,
                                korekOverrideApproved = adminPhoneKorekOverride,
                                allowEmpty = true
                            )
                            if (schoolName.isBlank() || adminUsername.isBlank() || adminPassword.isBlank()) {
                                errorMessage = "يرجى إدخال اسم المدرسة واسم المستخدم وكلمة المرور للمدير"
                            } else if (!phoneOk) {
                                errorMessage = phoneErr
                            } else {
                                if (onCompleteSetupWithAffiliation != null) {
                                    onCompleteSetupWithAffiliation(
                                        schoolName,
                                        address.trim(),
                                        selectedSchoolType,
                                        logoBase64,
                                        adminFullName,
                                        adminUsername,
                                        adminPassword,
                                        selectedSchoolStage,
                                        selectedMinistryAffiliation,
                                        selectedSchoolGenderType
                                    )
                                } else {
                                    onCompleteSetup(
                                        schoolName,
                                        address.trim(),
                                        selectedSchoolType,
                                        logoBase64,
                                        adminFullName,
                                        adminUsername,
                                        adminPassword,
                                        selectedSchoolStage
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "complete_school_setup_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

/**
 * School Activation & Access Gate Screen (بوابة رمز المدرسة والترحيب الأكاديمي)
 * Mandatory screen between Splash/Onboarding and UnifiedLoginScreen.
 */
@Composable
fun SchoolActivationGateScreen(
    schoolConfig: SchoolConfig,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onActivateSchoolAsAdmin: (schoolCode: String, adminPassword: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onVerifyActivatedSchoolCode: (schoolCode: String, onResult: (Boolean, String) -> Unit) -> Unit,
    onGateUnlockedSuccess: () -> Unit = {}
) {
    val coroutineScope = rememberCoroutineScope()

    var isAdminActivationMode by remember(schoolConfig.isSchoolOfficiallyActivated) {
        mutableStateOf(!schoolConfig.isSchoolOfficiallyActivated)
    }

    var schoolCodeInput by remember {
        mutableStateOf(
            schoolConfig.schoolActivationCode.takeIf { it.isNotBlank() && schoolConfig.isSetupCompleted }.orEmpty()
        )
    }
    var adminPasswordInput by remember { mutableStateOf("") }
    var showAdminPassword by remember { mutableStateOf(false) }
    var isVerifying by remember { mutableStateOf(false) }
    var isGateUnlockedAnimating by remember { mutableStateOf(false) }
    var statusFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var isStatusSuccess by remember { mutableStateOf(false) }

    val isTypingActive = schoolCodeInput.isNotBlank() || adminPasswordInput.isNotBlank()

    // Smooth physics-based animations for the Modern Flat Vector Academic Character
    val infiniteTransition = rememberInfiniteTransition(label = "pro_vector_welcome_character")
    val breathCycle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_breath_cycle"
    )
    val elbowWaveDeg by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_elbow_wave_deg"
    )
    val wristWaveDeg by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 22f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_wrist_wave_deg"
    )
    val tasselPendulumDeg by infiniteTransition.animateFloat(
        initialValue = -11f,
        targetValue = 13f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "char_tassel_pendulum_deg"
    )
    val particleFloatPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "char_particles_phase"
    )

    val unlockBadgeProgress by animateFloatAsState(
        targetValue = if (isGateUnlockedAnimating) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "char_unlock_badge_progress"
    )

    val typingBoost by animateFloatAsState(
        targetValue = when {
            isGateUnlockedAnimating -> 1f
            isVerifying -> 0.7f
            isTypingActive -> 0.35f
            else -> 0f
        },
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "char_typing_boost"
    )

    SoftWaveMeshBackground(isDarkMode = isDarkMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0, 0, 0, 0)
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Safe top spacing (24.dp after statusBarsPadding) so the header is never clipped
                Spacer(modifier = Modifier.height(24.dp))

                // Subtle Theme Toggle aligned to the end while keeping the main title centered
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        onClick = onToggleDarkMode,
                        shape = CircleShape,
                        color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(38.dp)
                            .testTag("gate_theme_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل المظهر",
                                tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 44.dp)
                    ) {
                        Text(
                            text = "مرحباً بك في منصتك المدرسية",
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color.White else Color(0xFF064E3B),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        Text(
                            text = "أدخل رمز المدرسة للمتابعة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Modern Flat Vector Illustration — Pro Academic Character with Smooth Waving Hand & Bezier Curves
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val bodyScaleY = 1f + (breathCycle - 0.5f) * 0.024f
                    val bodyTranslateY = (0.5f - breathCycle) * 7f

                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleY = bodyScaleY
                                translationY = bodyTranslateY
                            }
                            .testTag("school_security_gate_canvas")
                    ) {
                        val w = size.width
                        val h = size.height
                        val cx = w * 0.5f
                        val cy = h * 0.54f
                        val unit = minOf(w, h) / 250f

                        // 1. Soft Emerald & Mint Glow Circular Backdrop
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF34D399).copy(alpha = if (isDarkMode) 0.28f else 0.34f),
                                    Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.14f else 0.18f),
                                    Color(0xFFD97706).copy(alpha = 0.07f * (1f + unlockBadgeProgress)),
                                    Color.Transparent
                                ),
                                center = Offset(cx, cy - 6f * unit),
                                radius = 118f * unit
                            ),
                            radius = 118f * unit,
                            center = Offset(cx, cy - 6f * unit)
                        )

                        // Soft inner mint halo disc
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = if (isDarkMode) {
                                    listOf(Color(0xFF064E3B).copy(alpha = 0.65f), Color(0xFF042F24).copy(alpha = 0.40f))
                                } else {
                                    listOf(Color(0xFFD1FAE5).copy(alpha = 0.85f), Color(0xFFECFDF5).copy(alpha = 0.55f))
                                },
                                start = Offset(cx - 80f * unit, cy - 80f * unit),
                                end = Offset(cx + 80f * unit, cy + 80f * unit)
                            ),
                            radius = 84f * unit,
                            center = Offset(cx, cy + 4f * unit)
                        )
                        drawCircle(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF10B981).copy(alpha = 0.55f),
                                    Color(0xFFF59E0B).copy(alpha = 0.45f)
                                )
                            ),
                            radius = 84f * unit,
                            center = Offset(cx, cy + 4f * unit),
                            style = Stroke(width = 2f * unit)
                        )

                        // 2. Floating Geometric Glowing Particles (Diamonds, Rings, Soft Orbs)
                        val particleAnchors = listOf(
                            Triple(-92f, -52f, 7f),
                            Triple(94f, -46f, 6.5f),
                            Triple(-84f, 38f, 5.5f),
                            Triple(92f, 36f, 6f),
                            Triple(-52f, -88f, 5f),
                            Triple(58f, -84f, 5.5f)
                        )
                        particleAnchors.forEachIndexed { idx, (ox, oy, rad) ->
                            val driftY = sin(particleFloatPhase + idx * 1.15f) * 6.5f * unit
                            val driftX = cos(particleFloatPhase * 0.8f + idx * 0.9f) * 4f * unit
                            val px = cx + ox * unit + driftX
                            val py = cy + oy * unit + driftY
                            val pSize = rad * unit * (1f + typingBoost * 0.25f)

                            if (idx % 2 == 0) {
                                // Glowing 4-point geometric star/diamond via Bezier curves
                                val starPath = Path().apply {
                                    moveTo(px, py - pSize * 1.35f)
                                    cubicTo(
                                        px + pSize * 0.25f, py - pSize * 0.25f,
                                        px + pSize * 0.25f, py - pSize * 0.25f,
                                        px + pSize * 1.35f, py
                                    )
                                    cubicTo(
                                        px + pSize * 0.25f, py + pSize * 0.25f,
                                        px + pSize * 0.25f, py + pSize * 0.25f,
                                        px, py + pSize * 1.35f
                                    )
                                    cubicTo(
                                        px - pSize * 0.25f, py + pSize * 0.25f,
                                        px - pSize * 0.25f, py + pSize * 0.25f,
                                        px - pSize * 1.35f, py
                                    )
                                    cubicTo(
                                        px - pSize * 0.25f, py - pSize * 0.25f,
                                        px - pSize * 0.25f, py - pSize * 0.25f,
                                        px, py - pSize * 1.35f
                                    )
                                    close()
                                }
                                drawPath(
                                    path = starPath,
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFFBBF24), Color(0xFFF59E0B).copy(alpha = 0.2f)),
                                        center = Offset(px, py),
                                        radius = pSize * 1.5f
                                    )
                                )
                            } else {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF34D399).copy(alpha = 0.85f),
                                            Color(0xFF10B981).copy(alpha = 0.1f)
                                        ),
                                        center = Offset(px, py),
                                        radius = pSize * 1.4f
                                    ),
                                    radius = pSize,
                                    center = Offset(px, py)
                                )
                            }
                        }

                        // 3. Sculpted Left Arm (resting gracefully on hip/side in smooth Bezier curves)
                        val torsoTopY = cy + 6f * unit
                        val torsoBottomY = cy + 88f * unit
                        val leftArmPath = Path().apply {
                            moveTo(cx - 42f * unit, torsoTopY + 8f * unit)
                            cubicTo(
                                cx - 68f * unit, torsoTopY + 24f * unit,
                                cx - 72f * unit, torsoTopY + 56f * unit,
                                cx - 50f * unit, torsoBottomY - 4f * unit
                            )
                            cubicTo(
                                cx - 42f * unit, torsoBottomY - 2f * unit,
                                cx - 36f * unit, torsoTopY + 52f * unit,
                                cx - 34f * unit, torsoTopY + 24f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = leftArmPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF047857), Color(0xFF064E3B)),
                                start = Offset(cx - 70f * unit, torsoTopY),
                                end = Offset(cx - 34f * unit, torsoBottomY)
                            )
                        )

                        // 4. Anatomically Proportioned Academic Blazer & Shoulders (Smooth Cubic Bezier)
                        val blazerPath = Path().apply {
                            moveTo(cx - 56f * unit, torsoBottomY)
                            cubicTo(
                                cx - 58f * unit, torsoTopY + 42f * unit,
                                cx - 54f * unit, torsoTopY + 12f * unit,
                                cx - 38f * unit, torsoTopY + 4f * unit
                            )
                            cubicTo(
                                cx - 22f * unit, torsoTopY - 2f * unit,
                                cx + 22f * unit, torsoTopY - 2f * unit,
                                cx + 38f * unit, torsoTopY + 4f * unit
                            )
                            cubicTo(
                                cx + 54f * unit, torsoTopY + 12f * unit,
                                cx + 58f * unit, torsoTopY + 42f * unit,
                                cx + 56f * unit, torsoBottomY
                            )
                            cubicTo(
                                cx + 22f * unit, torsoBottomY + 10f * unit,
                                cx - 22f * unit, torsoBottomY + 10f * unit,
                                cx - 56f * unit, torsoBottomY
                            )
                            close()
                        }
                        drawPath(
                            path = blazerPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF059669),
                                    Color(0xFF047857),
                                    Color(0xFF064E3B)
                                ),
                                start = Offset(cx - 40f * unit, torsoTopY),
                                end = Offset(cx + 40f * unit, torsoBottomY)
                            )
                        )

                        // Crisp White Dress Shirt V-Insert & Emerald-Gold Tie
                        val shirtPath = Path().apply {
                            moveTo(cx - 18f * unit, torsoTopY + 2f * unit)
                            cubicTo(
                                cx - 12f * unit, torsoTopY + 24f * unit,
                                cx - 5f * unit, torsoTopY + 40f * unit,
                                cx, torsoTopY + 48f * unit
                            )
                            cubicTo(
                                cx + 5f * unit, torsoTopY + 40f * unit,
                                cx + 12f * unit, torsoTopY + 24f * unit,
                                cx + 18f * unit, torsoTopY + 2f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = shirtPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFFFFFFF), Color(0xFFE2E8F0)),
                                startY = torsoTopY,
                                endY = torsoTopY + 48f * unit
                            )
                        )

                        // Academic Gold-Trimmed Tie
                        val tiePath = Path().apply {
                            moveTo(cx - 4.5f * unit, torsoTopY + 8f * unit)
                            lineTo(cx + 4.5f * unit, torsoTopY + 8f * unit)
                            lineTo(cx + 6.5f * unit, torsoTopY + 34f * unit)
                            lineTo(cx, torsoTopY + 44f * unit)
                            lineTo(cx - 6.5f * unit, torsoTopY + 34f * unit)
                            close()
                        }
                        drawPath(
                            path = tiePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF59E0B), Color(0xFFB45309))
                            )
                        )

                        // Royal Academic Stole / Sash (وشاح أكاديمي زمردي وذهبي)
                        val leftSashPath = Path().apply {
                            moveTo(cx - 24f * unit, torsoTopY + 2f * unit)
                            cubicTo(
                                cx - 30f * unit, torsoTopY + 30f * unit,
                                cx - 28f * unit, torsoTopY + 58f * unit,
                                cx - 22f * unit, torsoBottomY + 4f * unit
                            )
                            lineTo(cx - 11f * unit, torsoBottomY + 5f * unit)
                            cubicTo(
                                cx - 15f * unit, torsoTopY + 54f * unit,
                                cx - 16f * unit, torsoTopY + 26f * unit,
                                cx - 14f * unit, torsoTopY + 3f * unit
                            )
                            close()
                        }
                        val rightSashPath = Path().apply {
                            moveTo(cx + 24f * unit, torsoTopY + 2f * unit)
                            cubicTo(
                                cx + 30f * unit, torsoTopY + 30f * unit,
                                cx + 28f * unit, torsoTopY + 58f * unit,
                                cx + 22f * unit, torsoBottomY + 4f * unit
                            )
                            lineTo(cx + 11f * unit, torsoBottomY + 5f * unit)
                            cubicTo(
                                cx + 15f * unit, torsoTopY + 54f * unit,
                                cx + 16f * unit, torsoTopY + 26f * unit,
                                cx + 14f * unit, torsoTopY + 3f * unit
                            )
                            close()
                        }
                        val sashBrush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFF10B981))
                        )
                        drawPath(path = leftSashPath, brush = sashBrush)
                        drawPath(path = rightSashPath, brush = sashBrush)

                        // Sculpted Blazer Collar Lapels
                        val lapelsPath = Path().apply {
                            moveTo(cx - 18f * unit, torsoTopY + 2f * unit)
                            lineTo(cx - 28f * unit, torsoTopY + 18f * unit)
                            lineTo(cx - 14f * unit, torsoTopY + 26f * unit)
                            lineTo(cx, torsoTopY + 48f * unit)
                            lineTo(cx + 14f * unit, torsoTopY + 26f * unit)
                            lineTo(cx + 28f * unit, torsoTopY + 18f * unit)
                            lineTo(cx + 18f * unit, torsoTopY + 2f * unit)
                        }
                        drawPath(
                            path = lapelsPath,
                            color = Color(0xFF064E3B),
                            style = Stroke(width = 2.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // 5. Smooth Waving Right Arm & Sculpted Hand (Rotating naturally from Elbow & Wrist joints)
                        val shoulderPivot = Offset(cx + 40f * unit, torsoTopY + 12f * unit)
                        val elbowPivot = Offset(cx + 68f * unit, torsoTopY + 18f * unit)

                        // Upper Arm bridging shoulder to elbow
                        val upperArmPath = Path().apply {
                            moveTo(shoulderPivot.x - 8f * unit, shoulderPivot.y - 6f * unit)
                            cubicTo(
                                shoulderPivot.x + 12f * unit, shoulderPivot.y - 8f * unit,
                                elbowPivot.x - 4f * unit, elbowPivot.y - 8f * unit,
                                elbowPivot.x + 6f * unit, elbowPivot.y - 2f * unit
                            )
                            cubicTo(
                                elbowPivot.x + 8f * unit, elbowPivot.y + 10f * unit,
                                shoulderPivot.x + 12f * unit, shoulderPivot.y + 14f * unit,
                                shoulderPivot.x - 6f * unit, shoulderPivot.y + 12f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = upperArmPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF059669), Color(0xFF047857)),
                                start = shoulderPivot,
                                end = elbowPivot
                            )
                        )

                        // Forearm + Waving Hand rotating smoothly around the Elbow Joint
                        val activeElbowWave = elbowWaveDeg * (1f + typingBoost * 0.35f) - unlockBadgeProgress * 18f
                        rotate(degrees = activeElbowWave, pivot = elbowPivot) {
                            val wristPivot = Offset(elbowPivot.x + 16f * unit, elbowPivot.y - 42f * unit)

                            // Curved Forearm Sleeve
                            val forearmPath = Path().apply {
                                moveTo(elbowPivot.x - 9f * unit, elbowPivot.y + 4f * unit)
                                cubicTo(
                                    elbowPivot.x - 6f * unit, elbowPivot.y - 18f * unit,
                                    wristPivot.x - 11f * unit, wristPivot.y + 8f * unit,
                                    wristPivot.x - 8f * unit, wristPivot.y
                                )
                                lineTo(wristPivot.x + 8f * unit, wristPivot.y + 3f * unit)
                                cubicTo(
                                    wristPivot.x + 10f * unit, wristPivot.y + 18f * unit,
                                    elbowPivot.x + 11f * unit, elbowPivot.y - 12f * unit,
                                    elbowPivot.x + 8f * unit, elbowPivot.y + 6f * unit
                                )
                                close()
                            }
                            drawPath(
                                path = forearmPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFF059669), Color(0xFF065F46)),
                                    start = elbowPivot,
                                    end = wristPivot
                                )
                            )
                            // Gold cuff trim at wrist
                            drawLine(
                                color = Color(0xFFFBBF24),
                                start = Offset(wristPivot.x - 8f * unit, wristPivot.y + 2f * unit),
                                end = Offset(wristPivot.x + 8f * unit, wristPivot.y + 5f * unit),
                                strokeWidth = 3f * unit,
                                cap = StrokeCap.Round
                            )

                            // Sculpted Vector Hand rotating at the Wrist Joint
                            rotate(degrees = wristWaveDeg, pivot = wristPivot) {
                                val handPalmPath = Path().apply {
                                    moveTo(wristPivot.x - 7f * unit, wristPivot.y)
                                    // Thumb curve
                                    cubicTo(
                                        wristPivot.x - 16f * unit, wristPivot.y - 6f * unit,
                                        wristPivot.x - 18f * unit, wristPivot.y - 16f * unit,
                                        wristPivot.x - 10f * unit, wristPivot.y - 18f * unit
                                    )
                                    cubicTo(
                                        wristPivot.x - 7f * unit, wristPivot.y - 17f * unit,
                                        wristPivot.x - 6f * unit, wristPivot.y - 12f * unit,
                                        wristPivot.x - 5f * unit, wristPivot.y - 14f * unit
                                    )
                                    // Sculpted fingers arch
                                    cubicTo(
                                        wristPivot.x - 6f * unit, wristPivot.y - 28f * unit,
                                        wristPivot.x + 8f * unit, wristPivot.y - 30f * unit,
                                        wristPivot.x + 11f * unit, wristPivot.y - 16f * unit
                                    )
                                    cubicTo(
                                        wristPivot.x + 13f * unit, wristPivot.y - 8f * unit,
                                        wristPivot.x + 10f * unit, wristPivot.y - 2f * unit,
                                        wristPivot.x + 6f * unit, wristPivot.y + 2f * unit
                                    )
                                    close()
                                }
                                drawPath(
                                    path = handPalmPath,
                                    brush = Brush.linearGradient(
                                        colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B).copy(alpha = 0.55f)),
                                        start = Offset(wristPivot.x - 12f * unit, wristPivot.y - 28f * unit),
                                        end = Offset(wristPivot.x + 10f * unit, wristPivot.y + 4f * unit)
                                    )
                                )

                                // Subtle waving motion arc lines beside the hand
                                val waveArcRadius = 22f * unit
                                drawArc(
                                    color = Color(0xFF10B981).copy(alpha = 0.55f),
                                    startAngle = -55f,
                                    sweepAngle = 65f,
                                    useCenter = false,
                                    topLeft = Offset(wristPivot.x - waveArcRadius + 4f * unit, wristPivot.y - 34f * unit),
                                    size = Size(waveArcRadius * 2f, waveArcRadius * 2f),
                                    style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round)
                                )

                                // Glowing Golden Confirmation Badge raised when school code is verified
                                if (unlockBadgeProgress > 0.02f) {
                                    val badgeCenter = Offset(
                                        wristPivot.x + 2f * unit,
                                        wristPivot.y - (36f + unlockBadgeProgress * 12f) * unit
                                    )
                                    val badgeRadius = 18f * unit * unlockBadgeProgress
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                Color(0xFFFDE68A),
                                                Color(0xFFF59E0B),
                                                Color.Transparent
                                            ),
                                            center = badgeCenter,
                                            radius = badgeRadius * 1.9f
                                        ),
                                        radius = badgeRadius * 1.9f,
                                        center = badgeCenter
                                    )
                                    drawCircle(
                                        brush = Brush.linearGradient(
                                            colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                                        ),
                                        radius = badgeRadius,
                                        center = badgeCenter
                                    )
                                    val checkPath = Path().apply {
                                        moveTo(badgeCenter.x - badgeRadius * 0.42f, badgeCenter.y + badgeRadius * 0.02f)
                                        lineTo(badgeCenter.x - badgeRadius * 0.10f, badgeCenter.y + badgeRadius * 0.34f)
                                        lineTo(badgeCenter.x + badgeRadius * 0.46f, badgeCenter.y - badgeRadius * 0.30f)
                                    }
                                    drawPath(
                                        path = checkPath,
                                        color = Color.White,
                                        style = Stroke(width = 3f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                }
                            }
                        }

                        // 6. Neck & Sculpted Flat-Vector Face (With Warm Shading & Modern Vector Features)
                        val neckTopY = cy - 12f * unit
                        val neckPath = Path().apply {
                            moveTo(cx - 11f * unit, neckTopY)
                            lineTo(cx - 13f * unit, torsoTopY + 8f * unit)
                            cubicTo(
                                cx - 5f * unit, torsoTopY + 15f * unit,
                                cx + 5f * unit, torsoTopY + 15f * unit,
                                cx + 13f * unit, torsoTopY + 8f * unit
                            )
                            lineTo(cx + 11f * unit, neckTopY)
                            close()
                        }
                        drawPath(
                            path = neckPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF5CBA7), Color(0xFFE59866)),
                                startY = neckTopY,
                                endY = torsoTopY + 14f * unit
                            )
                        )

                        val headCenterY = cy - 42f * unit
                        // Soft Ears
                        drawOval(
                            color = Color(0xFFF5CBA7),
                            topLeft = Offset(cx - 33f * unit, headCenterY - 6f * unit),
                            size = Size(10f * unit, 16f * unit)
                        )
                        drawOval(
                            color = Color(0xFFF5CBA7),
                            topLeft = Offset(cx + 23f * unit, headCenterY - 6f * unit),
                            size = Size(10f * unit, 16f * unit)
                        )

                        // Sculpted Jawline & Head Contour via Cubic Bezier Curves
                        val facePath = Path().apply {
                            moveTo(cx, headCenterY - 30f * unit)
                            cubicTo(
                                cx + 24f * unit, headCenterY - 30f * unit,
                                cx + 29f * unit, headCenterY - 12f * unit,
                                cx + 26f * unit, headCenterY + 6f * unit
                            )
                            cubicTo(
                                cx + 23f * unit, headCenterY + 22f * unit,
                                cx + 11f * unit, headCenterY + 31f * unit,
                                cx, headCenterY + 31f * unit
                            )
                            cubicTo(
                                cx - 11f * unit, headCenterY + 31f * unit,
                                cx - 23f * unit, headCenterY + 22f * unit,
                                cx - 26f * unit, headCenterY + 6f * unit
                            )
                            cubicTo(
                                cx - 29f * unit, headCenterY - 12f * unit,
                                cx - 24f * unit, headCenterY - 30f * unit,
                                cx, headCenterY - 30f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = facePath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFFFFF3E0),
                                    Color(0xFFFAD7A0),
                                    Color(0xFFF5CBA7)
                                ),
                                start = Offset(cx - 20f * unit, headCenterY - 28f * unit),
                                end = Offset(cx + 22f * unit, headCenterY + 30f * unit)
                            )
                        )

                        // Modern Styled Hair Under Graduation Cap
                        val hairPath = Path().apply {
                            moveTo(cx - 26f * unit, headCenterY - 6f * unit)
                            cubicTo(
                                cx - 28f * unit, headCenterY - 24f * unit,
                                cx - 14f * unit, headCenterY - 32f * unit,
                                cx, headCenterY - 30f * unit
                            )
                            cubicTo(
                                cx + 14f * unit, headCenterY - 32f * unit,
                                cx + 28f * unit, headCenterY - 24f * unit,
                                cx + 26f * unit, headCenterY - 6f * unit
                            )
                            cubicTo(
                                cx + 22f * unit, headCenterY - 18f * unit,
                                cx + 8f * unit, headCenterY - 16f * unit,
                                cx - 4f * unit, headCenterY - 19f * unit
                            )
                            cubicTo(
                                cx - 14f * unit, headCenterY - 21f * unit,
                                cx - 22f * unit, headCenterY - 16f * unit,
                                cx - 26f * unit, headCenterY - 6f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = hairPath,
                            brush = Brush.linearGradient(
                                colors = listOf(Color(0xFF1E293B), Color(0xFF0F172A)),
                                start = Offset(cx - 26f * unit, headCenterY - 30f * unit),
                                end = Offset(cx + 26f * unit, headCenterY)
                            )
                        )

                        // Soft Vector Eyebrows & Curved Friendly Eyelids (No primitive black dots)
                        val leftBrowPath = Path().apply {
                            moveTo(cx - 17f * unit, headCenterY - 7f * unit)
                            quadraticBezierTo(
                                cx - 11f * unit, headCenterY - 11f * unit,
                                cx - 5f * unit, headCenterY - 8f * unit
                            )
                        }
                        val rightBrowPath = Path().apply {
                            moveTo(cx + 5f * unit, headCenterY - 8f * unit)
                            quadraticBezierTo(
                                cx + 11f * unit, headCenterY - 11f * unit,
                                cx + 17f * unit, headCenterY - 7f * unit
                            )
                        }
                        drawPath(
                            path = leftBrowPath,
                            color = Color(0xFF334155),
                            style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = rightBrowPath,
                            color = Color(0xFF334155),
                            style = Stroke(width = 2.2f * unit, cap = StrokeCap.Round)
                        )

                        // Warm Curved Vector Eyes (Almond/Smiling Arcs with Emerald Iris Shading)
                        val leftEyeArch = Path().apply {
                            moveTo(cx - 16f * unit, headCenterY - 1f * unit)
                            quadraticBezierTo(
                                cx - 11f * unit, headCenterY - 5.5f * unit,
                                cx - 6f * unit, headCenterY - 1f * unit
                            )
                        }
                        val rightEyeArch = Path().apply {
                            moveTo(cx + 6f * unit, headCenterY - 1f * unit)
                            quadraticBezierTo(
                                cx + 11f * unit, headCenterY - 5.5f * unit,
                                cx + 16f * unit, headCenterY - 1f * unit
                            )
                        }
                        drawPath(
                            path = leftEyeArch,
                            color = Color(0xFF064E3B),
                            style = Stroke(width = 2.4f * unit, cap = StrokeCap.Round)
                        )
                        drawPath(
                            path = rightEyeArch,
                            color = Color(0xFF064E3B),
                            style = Stroke(width = 2.4f * unit, cap = StrokeCap.Round)
                        )

                        // Subtle Sculpted Nose Bridge
                        val nosePath = Path().apply {
                            moveTo(cx, headCenterY - 2f * unit)
                            cubicTo(
                                cx - 2.5f * unit, headCenterY + 5f * unit,
                                cx - 2f * unit, headCenterY + 8f * unit,
                                cx + 1.5f * unit, headCenterY + 8.5f * unit
                            )
                        }
                        drawPath(
                            path = nosePath,
                            color = Color(0xFFD98880).copy(alpha = 0.75f),
                            style = Stroke(width = 1.8f * unit, cap = StrokeCap.Round)
                        )

                        // Confident Warm Vector Smile
                        val smilePath = Path().apply {
                            moveTo(cx - 9.5f * unit, headCenterY + 14f * unit)
                            cubicTo(
                                cx - 4f * unit, headCenterY + 21f * unit,
                                cx + 4f * unit, headCenterY + 21f * unit,
                                cx + 9.5f * unit, headCenterY + 14f * unit
                            )
                        }
                        drawPath(
                            path = smilePath,
                            color = Color(0xFF065F46),
                            style = Stroke(width = 2.5f * unit, cap = StrokeCap.Round)
                        )

                        // 7. 3D Perspective Graduation Cap (Mortarboard) + Swinging Pendulum Gold Tassel
                        val capCrownY = headCenterY - 24f * unit
                        // Cap Skull Band
                        val capBandPath = Path().apply {
                            moveTo(cx - 24f * unit, capCrownY + 4f * unit)
                            cubicTo(
                                cx - 22f * unit, capCrownY - 8f * unit,
                                cx + 22f * unit, capCrownY - 8f * unit,
                                cx + 24f * unit, capCrownY + 4f * unit
                            )
                            cubicTo(
                                cx + 12f * unit, capCrownY + 1f * unit,
                                cx - 12f * unit, capCrownY + 1f * unit,
                                cx - 24f * unit, capCrownY + 4f * unit
                            )
                            close()
                        }
                        drawPath(
                            path = capBandPath,
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color(0xFF042F24), Color(0xFF065F46), Color(0xFF042F24))
                            )
                        )

                        // 3D Perspective Mortarboard Top Rhombus
                        val capTopCenterY = capCrownY - 8f * unit
                        val capBoardPath = Path().apply {
                            moveTo(cx, capTopCenterY - 15f * unit)
                            lineTo(cx + 46f * unit, capTopCenterY - 1f * unit)
                            lineTo(cx, capTopCenterY + 12f * unit)
                            lineTo(cx - 46f * unit, capTopCenterY - 1f * unit)
                            close()
                        }
                        drawPath(
                            path = capBoardPath,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF065F46),
                                    Color(0xFF047857),
                                    Color(0xFF064E3B)
                                ),
                                start = Offset(cx - 46f * unit, capTopCenterY - 15f * unit),
                                end = Offset(cx + 46f * unit, capTopCenterY + 12f * unit)
                            )
                        )
                        drawPath(
                            path = capBoardPath,
                            color = Color(0xFFFBBF24),
                            style = Stroke(width = 2f * unit, join = StrokeJoin.Round)
                        )

                        // Gold Button on Center of Mortarboard + Pendulum Swinging Tassel
                        val buttonCenter = Offset(cx, capTopCenterY - 1.5f * unit)
                        val tasselAnchor = Offset(cx - 32f * unit, capTopCenterY + 2f * unit)
                        drawLine(
                            color = Color(0xFFFBBF24),
                            start = buttonCenter,
                            end = tasselAnchor,
                            strokeWidth = 2.2f * unit,
                            cap = StrokeCap.Round
                        )
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = 3.5f * unit,
                            center = buttonCenter
                        )

                        rotate(degrees = tasselPendulumDeg, pivot = tasselAnchor) {
                            val tasselDropPath = Path().apply {
                                moveTo(tasselAnchor.x, tasselAnchor.y)
                                quadraticBezierTo(
                                    tasselAnchor.x - 3f * unit,
                                    tasselAnchor.y + 12f * unit,
                                    tasselAnchor.x - 1f * unit,
                                    tasselAnchor.y + 24f * unit
                                )
                            }
                            drawPath(
                                path = tasselDropPath,
                                color = Color(0xFFF59E0B),
                                style = Stroke(width = 2.6f * unit, cap = StrokeCap.Round)
                            )
                            val plumePath = Path().apply {
                                moveTo(tasselAnchor.x - 1f * unit, tasselAnchor.y + 22f * unit)
                                lineTo(tasselAnchor.x - 5.5f * unit, tasselAnchor.y + 32f * unit)
                                lineTo(tasselAnchor.x + 3.5f * unit, tasselAnchor.y + 32f * unit)
                                close()
                            }
                            drawPath(
                                path = plumePath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Clean, Minimal Luxury Activation Card (Zero Clutter)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = Color(0xFF059669).copy(alpha = 0.18f),
                            spotColor = Color(0xFF064E3B).copy(alpha = 0.22f)
                        ),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                    ),
                    border = BorderStroke(1.2.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFD1FAE5))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Field 1: رمز المدرسة
                        OutlinedTextField(
                            value = schoolCodeInput,
                            onValueChange = { newVal: String ->
                                schoolCodeInput = newVal
                                statusFeedbackMessage = null
                            },
                            label = { Text("رمز المدرسة") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFF059669)
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gate_school_code_input"),
                            shape = RoundedCornerShape(14.dp)
                        )

                        // Field 2 (Only in Principal Activation Mode): كلمة مرور الإدارة
                        AnimatedVisibility(visible = isAdminActivationMode) {
                            OutlinedTextField(
                                value = adminPasswordInput,
                                onValueChange = { newPass: String ->
                                    adminPasswordInput = newPass
                                    statusFeedbackMessage = null
                                },
                                label = { Text("كلمة مرور الإدارة") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Filled.Lock,
                                        contentDescription = null,
                                        tint = Color(0xFFD97706)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showAdminPassword = !showAdminPassword }) {
                                        Icon(
                                            imageVector = if (showAdminPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "إظهار كلمة المرور"
                                        )
                                    }
                                },
                                visualTransformation = if (showAdminPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("gate_admin_password_input"),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }

                        // Concise Feedback Banner
                        if (statusFeedbackMessage != null) {
                            Surface(
                                color = if (isStatusSuccess) Color(0xFFECFDF5) else Color(0xFFFDE8E8),
                                border = BorderStroke(
                                    1.dp,
                                    if (isStatusSuccess) Color(0xFF10B981) else Color(0xFFF8B4B4)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isStatusSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isStatusSuccess) Color(0xFF059669) else Color(0xFF9B1C1C)
                                    )
                                    Text(
                                        text = statusFeedbackMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isStatusSuccess) Color(0xFF065F46) else Color(0xFF9B1C1C)
                                    )
                                }
                            }
                        }

                        // Primary Action Button: "متابعة" or "تفعيل المدرسة"
                        Button(
                            onClick = {
                                if (isVerifying || isGateUnlockedAnimating) return@Button
                                isVerifying = true
                                statusFeedbackMessage = null
                                if (isAdminActivationMode) {
                                    onActivateSchoolAsAdmin(schoolCodeInput, adminPasswordInput) { ok: Boolean, msg: String ->
                                        isVerifying = false
                                        isStatusSuccess = ok
                                        statusFeedbackMessage = msg
                                        if (ok) {
                                            isGateUnlockedAnimating = true
                                            coroutineScope.launch {
                                                delay(550L)
                                                onGateUnlockedSuccess()
                                            }
                                        }
                                    }
                                } else {
                                    onVerifyActivatedSchoolCode(schoolCodeInput) { ok: Boolean, msg: String ->
                                        isVerifying = false
                                        isStatusSuccess = ok
                                        statusFeedbackMessage = msg
                                        if (ok) {
                                            isGateUnlockedAnimating = true
                                            coroutineScope.launch {
                                                delay(550L)
                                                onGateUnlockedSuccess()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isVerifying && !isGateUnlockedAnimating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("gate_submit_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF059669),
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                        ) {
                            if (isVerifying) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.2.dp,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "جاري التحقق...",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            } else {
                                Text(
                                    text = if (isAdminActivationMode) "تفعيل المدرسة" else "متابعة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }

                        // Bottom Short Toggle Button: "تفعيل مدرسة جديدة" or "لدي رمز مدرسة مفعلة"
                        TextButton(
                            onClick = {
                                isAdminActivationMode = !isAdminActivationMode
                                statusFeedbackMessage = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("gate_toggle_mode_button")
                        ) {
                            Text(
                                text = if (isAdminActivationMode) {
                                    "لدي رمز مدرسة مفعلة"
                                } else {
                                    "تفعيل مدرسة جديدة"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF047857)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Unified Login Screen (بوابة تسجيل الدخول الموحدة)
 * - Default screen after passing SchoolActivationGateScreen.
 * - Contains ONLY Username field, Password field, and Login button (no public "+ تأسيس مدرسة جديدة" button).
 * - Tapping 5 times consecutively on the platform logo at the top opens the one-time Activation Code dialog.
 */
@Composable
fun UnifiedLoginScreen(
    schoolConfig: SchoolConfig,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLoginAttempt: (String, String) -> Result<*>,
    onVerifyActivationCode: (String, (Boolean) -> Unit) -> Unit = { _, _ -> },
    discoveredSchoolName: String? = null,
    loginConnectingMessage: String? = null,
    onUsernameChanged: (String) -> Unit = {},
    occasionThemeConfig: OccasionThemeConfig = OccasionThemeConfig(),
    loginLockoutUntilMs: Long = 0L,
    failedLoginAttemptsCount: Int = 0,
    biometricCredentials: BiometricSavedCredentials? = null,
    onBiometricLogin: () -> Result<*> = { Result.success(null) },
    onShowErrorToast: (String) -> Unit = {},
    hasActiveTopToast: Boolean = false,
    onSwitchSchoolCodeGate: () -> Unit = {},
    studentsForQrLogin: List<StudentRecord> = emptyList(),
    onStudentBarcodeLogin: (String) -> Result<*> = {
        Result.failure<Any>(IllegalArgumentException("عفواً، باركود الهوية غير مسجل في هذه المدرسة أو غير صالح."))
    }
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }
    var showStudentBarcodeScannerModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (username.isBlank()) {
            onUsernameChanged("")
        }
    }

    val extractedSchoolCode: String? = remember(username) {
        val trimmed = username.trim()
        if (trimmed.contains("-")) {
            trimmed.substringBefore("-").trim().takeIf { it.isNotEmpty() }
        } else {
            null
        }
    }
    val isSchoolVerified = !extractedSchoolCode.isNullOrBlank() && !discoveredSchoolName.isNullOrBlank()

    var nowTickMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(loginLockoutUntilMs) {
        while (loginLockoutUntilMs > System.currentTimeMillis()) {
            nowTickMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000L)
        }
        nowTickMs = System.currentTimeMillis()
    }
    val remainingLockoutSec = ((loginLockoutUntilMs - nowTickMs) / 1000L).coerceAtLeast(0L)

    // 5-Tap Consecutive Logo Gesture State for New School Activation Code
    var logoTapCount by remember { mutableIntStateOf(0) }
    var lastLogoTapTime by remember { mutableLongStateOf(0L) }
    var showActivationCodeDialog by remember { mutableStateOf(false) }
    var activationCodeInput by remember { mutableStateOf("") }

    if (showActivationCodeDialog) {
        AlertDialog(
            onDismissRequest = { showActivationCodeDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.VpnKey,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "أدخل كود تفعيل تأسيس المدرسة",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أدخل كود التفعيل لمرة واحدة (مثال: MAD-XXXX) المخصص لفتح شاشة تأسيس مدرسة جديدة:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = activationCodeInput,
                        onValueChange = { activationCodeInput = it.uppercase() },
                        label = { Text("كود تفعيل تأسيس المدرسة") },
                        placeholder = { Text("MAD-XXXX") },
                        leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("activation_code_input"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "تحقق وفتح التأسيس",
                    icon = Icons.Filled.Verified,
                    onClick = {
                        onVerifyActivationCode(activationCodeInput) { valid ->
                            if (valid) {
                                showActivationCodeDialog = false
                                activationCodeInput = ""
                            }
                        }
                    },
                    testTag = "verify_activation_code_button"
                )
            },
            dismissButton = {
                TextButton(onClick = { showActivationCodeDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }

    SoftWaveMeshBackground(isDarkMode = isDarkMode) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 18.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: School Capsule + Switch Security Gate Button + Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF059669).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    val now = System.currentTimeMillis()
                                    if (now - lastLogoTapTime > 2200L) {
                                        logoTapCount = 1
                                    } else {
                                        logoTapCount++
                                    }
                                    lastLogoTapTime = now
                                    if (logoTapCount >= 5) {
                                        logoTapCount = 0
                                        showActivationCodeDialog = true
                                    }
                                }
                                .testTag("platform_logo_five_taps")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = Color(0xFF047857),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isSchoolVerified) {
                                        "منصة: $discoveredSchoolName"
                                    } else {
                                        "منصة مدرستي الذكية"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF047857),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Surface(
                            onClick = onSwitchSchoolCodeGate,
                            shape = RoundedCornerShape(50),
                            color = if (isDarkMode) Color(0xFF0F291E) else Color(0xFFFEF3C7).copy(alpha = 0.75f),
                            border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.55f)),
                            modifier = Modifier.testTag("switch_school_security_gate_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = "تبديل رمز المدرسة",
                                    tint = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFFB45309),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "تبديل رمز المدرسة",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFFFBBF24) else Color(0xFF92400E),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onToggleDarkMode,
                        shape = CircleShape,
                        color = if (isDarkMode) Color(0xFF1E293B) else Color.White,
                        border = BorderStroke(1.dp, if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("login_theme_toggle")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل الوضع الليلي والنهاري",
                                tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Razor-Sharp Full-Resolution Native Compose Vector Crest + Live "منصة مدرستي الذكية" & "MADRASATI"
                    RoyalAcademicCrestBrandHeader(
                        isDarkMode = isDarkMode,
                        logoSize = 108.dp,
                        subtitleText = "بوابة تسجيل الدخول الموحدة والآمنة للهيئة الإدارية والتدريسية والطلبة وأولياء الأمور",
                        onLogoClick = {
                            val now = System.currentTimeMillis()
                            if (now - lastLogoTapTime > 2200L) {
                                logoTapCount = 1
                            } else {
                                logoTapCount++
                            }
                            lastLogoTapTime = now
                            if (logoTapCount >= 5) {
                                logoTapCount = 0
                                showActivationCodeDialog = true
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3D Glassmorphic Login Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(
                                elevation = 12.dp,
                                shape = RoundedCornerShape(26.dp),
                                ambientColor = Color(0xFF059669).copy(alpha = 0.18f),
                                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.22f)
                            ),
                        shape = RoundedCornerShape(26.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDarkMode) Color(0xFF1E293B) else Color.White
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFD1FAE5),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        Icon(
                                            imageVector = Icons.Filled.Security,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = "تسجيل الدخول ببيانات الحساب المعتمدة",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else Color(0xFF0F172A)
                                )
                            }

                            // Smart Frameless Animated Transition between Credentials Notice (State 1)
                            // and Verified School Logo + Official Name + Ministry Affiliation & Gender (State 2)
                            val showRecognizedSchoolHeader = isSchoolVerified ||
                                (schoolConfig.isSetupCompleted && schoolConfig.schoolName.isNotBlank() &&
                                    (extractedSchoolCode == schoolConfig.schoolId ||
                                        (biometricCredentials != null && biometricCredentials.isValid && username.isBlank())))

                            androidx.compose.animation.AnimatedContent(
                                targetState = showRecognizedSchoolHeader,
                                label = "frameless_login_school_identity_transition"
                            ) { recognized ->
                                if (!recognized) {
                                    // State 1: Clean Frameless Credentials Notice (Zero Box Border, Zero Rounded Container)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .testTag("login_school_admin_credentials_notice"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Campaign,
                                            contentDescription = null,
                                            tint = Color(0xFF059669),
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "ملاحظة: تُستلم بيانات الدخول لجميع الحسابات مباشرةً من إدارة المدرسة.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                } else {
                                    // State 2: Clean Frameless School Identity (52.dp Logo + Official Name + Ministry Affiliation, Type & Gender)
                                    val resolvedSchoolTitle = discoveredSchoolName?.takeIf { it.isNotBlank() }
                                        ?: schoolConfig.schoolName.ifBlank { "المدرسة المعتمدة" }
                                    val matchedLocalConfig = schoolConfig.isSetupCompleted &&
                                        schoolConfig.schoolId.isNotBlank() &&
                                        (extractedSchoolCode == null || extractedSchoolCode == schoolConfig.schoolId)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .testTag("verified_school_identity_card"),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFECFDF5)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (matchedLocalConfig && schoolConfig.logoBase64.isNotBlank()) {
                                                Base64AvatarImage(
                                                    base64Data = schoolConfig.logoBase64,
                                                    fallbackIcon = Icons.Filled.School,
                                                    contentDescription = resolvedSchoolTitle,
                                                    size = 52.dp,
                                                    tint = Color(0xFF059669)
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Filled.School,
                                                    contentDescription = resolvedSchoolTitle,
                                                    tint = Color(0xFF059669),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }

                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            Text(
                                                text = resolvedSchoolTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontSize = 15.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF065F46),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (matchedLocalConfig) {
                                                Text(
                                                    text = "تتبع لـ: ${schoolConfig.ministryAffiliation} • ${schoolConfig.schoolType.arabicLabel} • ${schoolConfig.schoolGenderType} (${schoolConfig.schoolStage.arabicLabel})",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            } else if (!extractedSchoolCode.isNullOrBlank()) {
                                                Text(
                                                    text = "كود المدرسة المعرّف: $extractedSchoolCode",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF047857)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = username,
                                onValueChange = {
                                    username = it
                                    loginError = null
                                    onUsernameChanged(it)
                                },
                                label = { Text("اسم المستخدم") },
                                placeholder = { Text("مثال: 105-1 أو 105-ali") },
                                leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null, tint = Color(0xFF059669)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_username_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = {
                                    password = it
                                    loginError = null
                                },
                                label = { Text("كلمة المرور") },
                                placeholder = { Text("أدخل كلمة المرور") },
                                leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null, tint = Color(0xFF059669)) },
                                trailingIcon = {
                                    IconButton(onClick = { showPassword = !showPassword }) {
                                        Icon(
                                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            contentDescription = "إظهار كلمة المرور"
                                        )
                                    }
                                },
                                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_password_input"),
                                shape = RoundedCornerShape(14.dp)
                            )

                            val activeConnectingBanner = loginConnectingMessage
                                ?: loginError?.takeIf { it.startsWith("جاري الاتصال") }

                            if (!activeConnectingBanner.isNullOrBlank()) {
                                val resolvedBannerText = if (isSchoolVerified) {
                                    "جاري الاتصال بـ $discoveredSchoolName..."
                                } else {
                                    activeConnectingBanner
                                }
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF059669)
                                        )
                                        Text(
                                            text = resolvedBannerText,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF047857)
                                        )
                                    }
                                }
                            } else if (loginError != null && !hasActiveTopToast) {
                                Surface(
                                    color = Color(0xFFFDE8E8),
                                    border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.GppBad,
                                            contentDescription = null,
                                            tint = Color(0xFF9B1C1C)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = loginError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9B1C1C)
                                        )
                                    }
                                }
                            }

                            if (remainingLockoutSec > 0L) {
                                Surface(
                                    color = Color(0xFFFDE8E8),
                                    border = BorderStroke(1.dp, Color(0xFFF8B4B4)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.LockClock,
                                            contentDescription = null,
                                            tint = Color(0xFF9B1C1C)
                                        )
                                        Text(
                                            text = "الدرع الأمني مفعل: يرجى الانتظار ${remainingLockoutSec} ثانية قبل المحاولة مجدداً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF9B1C1C)
                                        )
                                    }
                                }
                            }

                            VectorActionButton(
                                text = if (remainingLockoutSec > 0L) "مقفل مؤقتاً (${remainingLockoutSec}ث)" else "تسجيل الدخول",
                                icon = Icons.AutoMirrored.Filled.Login,
                                enabled = remainingLockoutSec == 0L,
                                onClick = {
                                    loginError = null
                                    val res = onLoginAttempt(username, password)
                                    if (res.isFailure) {
                                        val msg = res.exceptionOrNull()?.message ?: "خطأ في تسجيل الدخول"
                                        if (msg.startsWith("جاري الاتصال")) {
                                            loginError = msg
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "login_submit_button"
                            )

                            // فاصل أنيق للدخول السريع بدون كلمة مرور عبر باركود الهوية الطلابية
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = if (isDarkMode) Color(0xFF1E3A2F) else Color(0xFFD1FAE5)
                                )
                                Text(
                                    text = "أو الدخول السريع بدون كلمة مرور",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.weight(1f),
                                    thickness = 1.dp,
                                    color = if (isDarkMode) Color(0xFF1E3A2F) else Color(0xFFD1FAE5)
                                )
                            }

                            // بطاقة/زر زمردية ملكية بحواف ذهبية لتسجيل الدخول بباركود الهوية الطلابية
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable(enabled = remainingLockoutSec == 0L) {
                                        loginError = null
                                        showStudentBarcodeScannerModal = true
                                    }
                                    .testTag("student_qr_barcode_quick_login_button"),
                                shape = RoundedCornerShape(16.dp),
                                color = Color.Transparent,
                                border = BorderStroke(1.5.dp, Color(0xFFFBBF24)),
                                shadowElevation = 4.dp
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(
                                                    Color(0xFF042F24),
                                                    Color(0xFF065F46),
                                                    Color(0xFF047857)
                                                )
                                            )
                                        )
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        modifier = Modifier.size(46.dp),
                                        shape = RoundedCornerShape(13.dp),
                                        color = Color(0xFF022C22).copy(alpha = 0.78f),
                                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.85f))
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            StudentIdBarcodeVectorIcon(
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "تسجيل الدخول بباركود الهوية الطلابية",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color(0xFFFEF3C7)
                                        )
                                        Text(
                                            text = "امسح باركود بطاقة الطالب للوصول الفوري للحساب",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFFD1FAE5)
                                        )
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFFBBF24).copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.65f)),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.QrCodeScanner,
                                                contentDescription = "مسح باركود الهوية الطلابية",
                                                tint = Color(0xFFFBBF24),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (biometricCredentials != null && biometricCredentials.isValid && remainingLockoutSec == 0L) {
                                BiometricQuickLoginButton(
                                    savedCredentials = biometricCredentials,
                                    onBiometricAuthenticated = {
                                        loginError = null
                                        val res = onBiometricLogin()
                                        if (res.isFailure) {
                                            val msg = res.exceptionOrNull()?.message ?: "تعذر تسجيل الدخول بالبصمة المحفوظة"
                                            if (msg.startsWith("جاري الاتصال")) {
                                                loginError = msg
                                            }
                                        }
                                    },
                                    onBiometricError = { errMsg ->
                                        loginError = null
                                        onShowErrorToast(errMsg)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showStudentBarcodeScannerModal) {
            StudentIdQrLoginDialog(
                schoolConfig = schoolConfig,
                students = studentsForQrLogin,
                onDismiss = { showStudentBarcodeScannerModal = false },
                onBarcodeScannedOrSubmitted = { barcodePayload ->
                    onStudentBarcodeLogin(barcodePayload)
                }
            )
        }
    }
}

/**
 * أيقونة فيكتور مخصصة لباركود الهوية الطلابية مرسومة بـ Canvas ومنحنيات بيزيير
 */
@Composable
fun StudentIdBarcodeVectorIcon(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "student_id_barcode_icon")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "barcode_icon_laser"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cornerLen = w * 0.22f
        val strokeW = (w * 0.075f).coerceAtLeast(1.8f)
        val goldColor = Color(0xFFFBBF24)
        val emeraldLight = Color(0xFF6EE7B7)

        // زوايا إطار الماسح الضوئي الأربع
        val cornersPath = Path().apply {
            // Top-left
            moveTo(w * 0.08f, h * 0.28f)
            lineTo(w * 0.08f, h * 0.14f)
            quadraticTo(w * 0.08f, h * 0.08f, w * 0.14f, h * 0.08f)
            lineTo(w * 0.08f + cornerLen, h * 0.08f)

            // Top-right
            moveTo(w * 0.92f - cornerLen, h * 0.08f)
            lineTo(w * 0.86f, h * 0.08f)
            quadraticTo(w * 0.92f, h * 0.08f, w * 0.92f, h * 0.14f)
            lineTo(w * 0.92f, h * 0.28f)

            // Bottom-left
            moveTo(w * 0.08f, h * 0.72f)
            lineTo(w * 0.08f, h * 0.86f)
            quadraticTo(w * 0.08f, h * 0.92f, w * 0.14f, h * 0.92f)
            lineTo(w * 0.08f + cornerLen, h * 0.92f)

            // Bottom-right
            moveTo(w * 0.92f - cornerLen, h * 0.92f)
            lineTo(w * 0.86f, h * 0.92f)
            quadraticTo(w * 0.92f, h * 0.92f, w * 0.92f, h * 0.86f)
            lineTo(w * 0.92f, h * 0.72f)
        }
        drawPath(
            path = cornersPath,
            color = goldColor,
            style = Stroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // خطوط الباركود العمودية داخل الإطار
        val barPositions = listOf(0.24f, 0.35f, 0.44f, 0.55f, 0.65f, 0.76f)
        val barWidths = listOf(0.055f, 0.035f, 0.065f, 0.04f, 0.06f, 0.045f)
        barPositions.forEachIndexed { idx, xRatio ->
            val barW = w * barWidths[idx]
            drawRoundRect(
                color = if (idx % 2 == 0) Color.White else emeraldLight,
                topLeft = Offset(w * xRatio - barW / 2f, h * 0.25f),
                size = Size(barW, h * 0.50f),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }

        // شعاع ليزر بيزيير متحرك
        val laserY = h * laserProgress
        val laserPath = Path().apply {
            moveTo(w * 0.12f, laserY)
            cubicTo(
                w * 0.36f, laserY - h * 0.05f,
                w * 0.64f, laserY + h * 0.05f,
                w * 0.88f, laserY
            )
        }
        drawPath(
            path = laserPath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    Color.Transparent,
                    goldColor,
                    Color(0xFF34D399),
                    goldColor,
                    Color.Transparent
                )
            ),
            style = Stroke(width = strokeW * 1.15f, cap = StrokeCap.Round)
        )
    }
}

/**
 * إطار الماسح الضوئي التفاعلي بأنميشن ليزر بيزيير (Bezier Laser Scanner Animation)
 */
@Composable
fun StudentIdBezierLaserScannerFrame(
    modifier: Modifier = Modifier,
    statusLabel: String = "وجّه باركود بطاقة الهوية داخل الإطار الزمردي"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bezier_laser_scanner_frame")
    val scanYRatio by infiniteTransition.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bezier_laser_y"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bezier_laser_glow"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(175.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF021F18),
                        Color(0xFF042F24),
                        Color(0xFF021F18)
                    )
                )
            )
            .border(1.5.dp, Color(0xFFFBBF24).copy(alpha = 0.65f), RoundedCornerShape(20.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val padX = w * 0.12f
            val padY = h * 0.14f
            val boxW = w - padX * 2f
            val boxH = h - padY * 2f
            val corner = 26.dp.toPx()

            // شبكة خلفية خفيفة للهوية الرقمية
            val step = 24.dp.toPx()
            var gx = padX
            while (gx <= w - padX) {
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    start = Offset(gx, padY),
                    end = Offset(gx, h - padY),
                    strokeWidth = 1.dp.toPx()
                )
                gx += step
            }
            var gy = padY
            while (gy <= h - padY) {
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.08f),
                    start = Offset(padX, gy),
                    end = Offset(w - padX, gy),
                    strokeWidth = 1.dp.toPx()
                )
                gy += step
            }

            // رسم أركان الماسح الذهبية والزمردية
            val cornerStroke = 3.5.dp.toPx()
            val corners = Path().apply {
                moveTo(padX, padY + corner)
                lineTo(padX, padY)
                lineTo(padX + corner, padY)

                moveTo(w - padX - corner, padY)
                lineTo(w - padX, padY)
                lineTo(w - padX, padY + corner)

                moveTo(padX, h - padY - corner)
                lineTo(padX, h - padY)
                lineTo(padX + corner, h - padY)

                moveTo(w - padX - corner, h - padY)
                lineTo(w - padX, h - padY)
                lineTo(w - padX, h - padY - corner)
            }
            drawPath(
                path = corners,
                color = Color(0xFFFBBF24),
                style = Stroke(width = cornerStroke, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // محاكاة بطاقة الطالب والباركود في وسط الماسح
            val cardW = boxW * 0.56f
            val cardH = boxH * 0.62f
            val cardLeft = (w - cardW) / 2f
            val cardTop = (h - cardH) / 2f
            drawRoundRect(
                color = Color(0xFF064E3B).copy(alpha = 0.55f),
                topLeft = Offset(cardLeft, cardTop),
                size = Size(cardW, cardH),
                cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx()),
                style = Stroke(width = 1.4.dp.toPx())
            )
            // خطوط الباركود داخل البطاقة
            val barsCount = 13
            val barsStartX = cardLeft + cardW * 0.14f
            val barsSpanW = cardW * 0.72f
            for (i in 0 until barsCount) {
                val bx = barsStartX + (barsSpanW / barsCount) * i
                val bw = if (i % 3 == 0) 3.5.dp.toPx() else 2.dp.toPx()
                drawRoundRect(
                    color = if (i % 2 == 0) Color(0xFFD1FAE5).copy(alpha = 0.7f) else Color(0xFFFDE68A).copy(alpha = 0.7f),
                    topLeft = Offset(bx, cardTop + cardH * 0.24f),
                    size = Size(bw, cardH * 0.52f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            // شعاع الليزر الزمردي-الذهبي المرسوم بمنحنيات بيزيير (Bezier Laser Beam)
            val laserY = padY + boxH * scanYRatio
            val waveAmp = 7.dp.toPx() * (0.5f - kotlin.math.abs(scanYRatio - 0.5f))
            val laserCurve = Path().apply {
                moveTo(padX + 6.dp.toPx(), laserY)
                cubicTo(
                    padX + boxW * 0.33f, laserY - waveAmp,
                    padX + boxW * 0.66f, laserY + waveAmp,
                    w - padX - 6.dp.toPx(), laserY
                )
            }
            // هالة الليزر
            drawPath(
                path = laserCurve,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF10B981).copy(alpha = glowAlpha * 0.45f),
                        Color(0xFFFBBF24).copy(alpha = glowAlpha * 0.65f),
                        Color(0xFF10B981).copy(alpha = glowAlpha * 0.45f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
            )
            // خيط الليزر الحاد
            drawPath(
                path = laserCurve,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color(0xFF34D399),
                        Color(0xFFFEF08A),
                        Color(0xFF34D399),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            shape = RoundedCornerShape(50),
            color = Color(0xFF022C22).copy(alpha = 0.88f),
            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f))
        ) {
            Text(
                text = statusLabel,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD1FAE5)
            )
        }
    }
}

/**
 * نافذة الماسح الذكي لباركود الهوية الطلابية (QR / Barcode Scanner Modal)
 * تدعم:
 * 1) مسح الكاميرا المباشر / الماسح الضوئي الذكي بأنميشن ليزر بيزيير (Bezier Laser Scanner Animation)
 * 2) رفع وقراءة صورة الهوية الطلابية من المعرض + إدخال رمز الباركود السريع (ID Barcode Token / Student ID)
 */
@Composable
fun StudentIdQrLoginDialog(
    schoolConfig: SchoolConfig,
    students: List<StudentRecord>,
    onDismiss: () -> Unit,
    onBarcodeScannedOrSubmitted: (String) -> Result<*>
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var selectedMethodTab by remember { mutableIntStateOf(0) } // 0 = الماسح الضوئي والكاميرا، 1 = صورة المعرض أو رمز الباركود
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var useLiveCameraPreview by remember { mutableStateOf(hasCameraPermission) }
    var barcodeTokenInput by remember { mutableStateOf("") }
    var scannerError by remember { mutableStateOf<String?>(null) }
    var verifiedWelcomeBanner by remember { mutableStateOf<String?>(null) }
    var isProcessingScan by remember { mutableStateOf(false) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        useLiveCameraPreview = granted
        if (!granted) {
            scannerError = "لم يتم منح إذن الكاميرا؛ يمكنك قراءة صورة الهوية من المعرض أو إدخال رمز الباركود مباشرة."
        } else {
            scannerError = null
        }
    }

    fun executeBarcodeLoginFlow(rawPayload: String) {
        val trimmed = rawPayload.trim()
        if (trimmed.isBlank() || isProcessingScan) {
            if (trimmed.isBlank()) {
                scannerError = "يرجى مسح باركود الهوية أو إدخال رمز الباركود المطبوع على بطاقة الطالب."
            }
            return
        }
        scannerError = null
        isProcessingScan = true

        // التحقق المسبق من اسم الطالب لعرض رسالة الترحيب الزمردية الفورية قبل الانتقال للوحة التحكم
        val matchedPreviewStudent = com.example.util.StandardQrCodeHelper.matchStudentFromQrPayloadOrStudents(
            rawInput = trimmed,
            students = students
        ) ?: students.firstOrNull { it.matchesBarcodePayload(trimmed) }

        if (matchedPreviewStudent != null && !schoolConfig.isSuspended) {
            verifiedWelcomeBanner = "تم التعرف على الهوية الطلابية: مرحباً بك ${matchedPreviewStudent.fullNameWithFather}"
            coroutineScope.launch {
                delay(650L)
                val result = onBarcodeScannedOrSubmitted(trimmed)
                isProcessingScan = false
                if (result.isSuccess) {
                    onDismiss()
                } else {
                    verifiedWelcomeBanner = null
                    scannerError = result.exceptionOrNull()?.message
                        ?: "عفواً، باركود الهوية غير مسجل في هذه المدرسة أو غير صالح."
                }
            }
        } else {
            val result = onBarcodeScannedOrSubmitted(trimmed)
            isProcessingScan = false
            if (result.isSuccess) {
                val loggedAccount = result.getOrNull() as? UserAccount
                val welcomeName = loggedAccount?.fullName ?: matchedPreviewStudent?.fullNameWithFather ?: "طالبنا العزيز"
                verifiedWelcomeBanner = "تم التعرف على الهوية الطلابية: مرحباً بك $welcomeName"
                onDismiss()
            } else {
                verifiedWelcomeBanner = null
                scannerError = result.exceptionOrNull()?.message
                    ?: "عفواً، باركود الهوية غير مسجل في هذه المدرسة أو غير صالح."
            }
        }
    }

    // قارئ صورة بطاقة الهوية الطلابية من المعرض (Zero-permission Android Photo Picker)
    val galleryPhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scannerError = null
            runCatching {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    android.graphics.BitmapFactory.decodeStream(stream)
                }
            }.onSuccess { bitmap ->
                if (bitmap != null) {
                    val detectedPayload = com.example.util.StandardQrCodeHelper.detectQrCodeFromBitmap(
                        bitmap = bitmap,
                        students = students
                    )
                    if (!detectedPayload.isNullOrBlank()) {
                        barcodeTokenInput = detectedPayload
                        executeBarcodeLoginFlow(detectedPayload)
                    } else {
                        // إذا كان هناك طالب واحد مطابق أو تعذر استخراج مصفوفة البكسل من لقطة شاشة مقصوصة، نوجه المستخدم لإدخال الكود المطبوع أسفل الباركود
                        selectedMethodTab = 1
                        scannerError = "تعذر قراءة مربعات QR تلقائياً من هذه الصورة؛ يرجى كتابة أو لصق كود الباركود المطبوع أسفل بطاقة الهوية."
                    }
                } else {
                    scannerError = "تعذر فتح صورة الهوية المختارة."
                }
            }.onFailure {
                scannerError = "حدث خطأ أثناء قراءة صورة الهوية من المعرض."
            }
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { if (!isProcessingScan) onDismiss() },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 520.dp)
                .clip(RoundedCornerShape(26.dp)),
            shape = RoundedCornerShape(26.dp),
            color = Color(0xFF042F24),
            border = BorderStroke(1.8.dp, Color(0xFFFBBF24)),
            shadowElevation = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // ترويسة نافذة الماسح الذكي لباركود الهوية الطلابية
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF022C22),
                        border = BorderStroke(1.2.dp, Color(0xFFFBBF24))
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            StudentIdBarcodeVectorIcon(modifier = Modifier.size(28.dp))
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تسجيل الدخول بباركود الهوية الطلابية",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFEF3C7)
                        )
                        Text(
                            text = if (schoolConfig.schoolName.isNotBlank()) {
                                "${schoolConfig.schoolName} • بوابة الهوية الرقمية الذكية"
                            } else {
                                "امسح باركود بطاقة الطالب للوصول الفوري للحساب"
                            },
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFA7F3D0)
                        )
                    }

                    IconButton(
                        onClick = { if (!isProcessingScan) onDismiss() },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "إغلاق الماسح",
                            tint = Color(0xFFFEF3C7),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // رسالة الترحيب الزمردية الفورية عند نجاح التعرف على الهوية الطلابية
                AnimatedVisibility(visible = !verifiedWelcomeBanner.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_barcode_verified_welcome_banner"),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF065F46),
                        border = BorderStroke(1.5.dp, Color(0xFFFBBF24))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(26.dp)
                            )
                            Text(
                                text = verifiedWelcomeBanner.orEmpty(),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFEF3C7)
                            )
                        }
                    }
                }

                // أزرار التبديل بين الطريقتين العمليتين
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF022C22), RoundedCornerShape(14.dp))
                        .border(1.dp, Color(0xFF047857), RoundedCornerShape(14.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val tab1Selected = selectedMethodTab == 0
                    val tab2Selected = selectedMethodTab == 1

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedMethodTab = 0
                                scannerError = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (tab1Selected) Color(0xFF065F46) else Color.Transparent,
                        border = if (tab1Selected) BorderStroke(1.dp, Color(0xFFFBBF24)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                tint = if (tab1Selected) Color(0xFFFBBF24) else Color(0xFFA7F3D0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مسح الكاميرا الذكي",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tab1Selected) Color(0xFFFEF3C7) else Color(0xFFA7F3D0)
                            )
                        }
                    }

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                selectedMethodTab = 1
                                scannerError = null
                            },
                        shape = RoundedCornerShape(10.dp),
                        color = if (tab2Selected) Color(0xFF065F46) else Color.Transparent,
                        border = if (tab2Selected) BorderStroke(1.dp, Color(0xFFFBBF24)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 9.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                tint = if (tab2Selected) Color(0xFFFBBF24) else Color(0xFFA7F3D0),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "صورة الهوية / الكود",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (tab2Selected) Color(0xFFFEF3C7) else Color(0xFFA7F3D0)
                            )
                        }
                    }
                }

                // الطريقة الأولى: مسح الكاميرا / الماسح الضوئي الذكي (Bezier Laser Scanner Animation)
                if (selectedMethodTab == 0) {
                    if (hasCameraPermission && useLiveCameraPreview) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(215.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(20.dp))
                        ) {
                            com.example.util.StandardQrCodeHelper.LiveCameraQrScannerBox(
                                students = students,
                                onQrCodeScanned = { detectedCode: String ->
                                    barcodeTokenInput = detectedCode
                                    executeBarcodeLoginFlow(detectedCode)
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    } else {
                        StudentIdBezierLaserScannerFrame(
                            statusLabel = "شعاع الليزر الذكي جاهز لمسح باركود بطاقة الطالب"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (hasCameraPermission) {
                                    useLiveCameraPreview = !useLiveCameraPreview
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.8f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFFEF3C7)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (hasCameraPermission && useLiveCameraPreview) "إطار الليزر الرقمي" else "تشغيل الكاميرا المباشرة",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Button(
                            onClick = {
                                galleryPhotoPickerLauncher.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("pick_student_id_gallery_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF065F46),
                                contentColor = Color(0xFFFEF3C7)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF34D399))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ImageSearch,
                                contentDescription = null,
                                tint = Color(0xFFFBBF24),
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "رفع صورة الهوية",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // الطريقة الثانية: رفع صورة الهوية من المعرض مباشرة
                    StudentIdBezierLaserScannerFrame(
                        statusLabel = "اختر صورة بطاقة الهوية من هاتفك أو أدخل رمز الباركود"
                    )

                    Button(
                        onClick = {
                            galleryPhotoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF065F46),
                            contentColor = Color(0xFFFEF3C7)
                        ),
                        border = BorderStroke(1.2.dp, Color(0xFFFBBF24))
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اختيار صورة بطاقة الهوية الطلابية من المعرض",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }

                // قسم إدخال/لصق رمز الباركود السريع (يعمل في كلتا الحالتين لتمكين الدخول الفوري 100% على الهواتف والمحاكيات)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF022C22).copy(alpha = 0.85f),
                    border = BorderStroke(1.dp, Color(0xFF047857))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                    imageVector = Icons.Filled.Badge,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24),
                                    modifier = Modifier.size(17.dp)
                                )
                                Text(
                                    text = "رمز باركود الهوية السريع (ID Barcode Token)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF3C7)
                                )
                            }

                            val clipboardManager = remember(context) {
                                context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            }
                            TextButton(
                                onClick = {
                                    val pasted = clipboardManager?.primaryClip
                                        ?.takeIf { it.itemCount > 0 }
                                        ?.getItemAt(0)
                                        ?.coerceToText(context)
                                        ?.toString()
                                        ?.trim()
                                        .orEmpty()
                                    if (pasted.isNotBlank()) {
                                        barcodeTokenInput = pasted
                                        scannerError = null
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentPaste,
                                    contentDescription = null,
                                    tint = Color(0xFF6EE7B7),
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "لصق الكود",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = barcodeTokenInput,
                            onValueChange = {
                                barcodeTokenInput = it
                                scannerError = null
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("student_barcode_token_input"),
                            singleLine = true,
                            label = {
                                Text(
                                    text = "MADRASATI_ID:${schoolConfig.schoolId.ifBlank { "100" }}:ST... أو معرف الطالب",
                                    fontSize = 11.5.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "امسح البطاقة أو الصق كود الباركود المطبوع أسفل QR",
                                    fontSize = 11.sp
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.QrCode2,
                                    contentDescription = null,
                                    tint = Color(0xFFFBBF24)
                                )
                            },
                            trailingIcon = {
                                if (barcodeTokenInput.isNotBlank()) {
                                    IconButton(onClick = { barcodeTokenInput = "" }) {
                                        Icon(
                                            imageVector = Icons.Filled.Clear,
                                            contentDescription = "مسح",
                                            tint = Color(0xFFA7F3D0)
                                        )
                                    }
                                }
                            },
                            shape = RoundedCornerShape(13.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color(0xFFECFDF5),
                                focusedBorderColor = Color(0xFFFBBF24),
                                unfocusedBorderColor = Color(0xFF10B981).copy(alpha = 0.55f),
                                focusedLabelColor = Color(0xFFFBBF24),
                                unfocusedLabelColor = Color(0xFFA7F3D0),
                                cursorColor = Color(0xFFFBBF24)
                            )
                        )

                        // شرائح اختيار سريع لبطاقات الهوية المسجلة في المدرسة لتسهيل الاختبار الفوري على المحاكيات
                        if (students.isNotEmpty()) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "بطاقات الهوية الطلابية المتاحة للمسح السريع (${students.size}):",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFA7F3D0)
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(students.take(12), key = { it.id }) { stu ->
                                        Surface(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    barcodeTokenInput = stu.qrCodeValue
                                                    executeBarcodeLoginFlow(stu.qrCodeValue)
                                                },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color(0xFF064E3B),
                                            border = BorderStroke(1.dp, Color(0xFFFBBF24).copy(alpha = 0.65f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.QrCode,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFBBF24),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Column {
                                                    Text(
                                                        text = stu.fullNameWithFather,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFFFEF3C7),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = stu.qrCodeValue,
                                                        fontSize = 9.5.sp,
                                                        color = Color(0xFFA7F3D0),
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
                }

                // رسالة التنبيه الأمنية عند وجود باركود غير صالح أو غير مسجل
                AnimatedVisibility(visible = !scannerError.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_barcode_error_banner"),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.92f),
                        border = BorderStroke(1.2.dp, Color(0xFFF87171))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.GppBad,
                                contentDescription = null,
                                tint = Color(0xFFFCA5A5),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = scannerError.orEmpty(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFEE2E2)
                            )
                        }
                    }
                }

                // زر التحقق والدخول الفوري بالباركود
                Button(
                    onClick = { executeBarcodeLoginFlow(barcodeTokenInput) },
                    enabled = !isProcessingScan,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("verify_student_barcode_login_button"),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFBBF24),
                        contentColor = Color(0xFF022C22)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.QrCodeScanner,
                        contentDescription = null,
                        tint = Color(0xFF022C22),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isProcessingScan) "جاري التحقق من الهوية الطلابية..." else "تحقق ودخول فوري بالباركود",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF022C22)
                    )
                }
            }
        }
    }
}

/**
 * Internal Developer Super-Admin Control Panel:
 * "لوحة تحكم النظام العام"
 * Opened exclusively via encrypted cloud vault authentication.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DeveloperControlPanelScreen(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val licenses by viewModel.schoolLicenses.collectAsState()
    val registeredSchools by viewModel.registeredSchools.collectAsState()
    val developerBroadcasts by viewModel.developerBroadcasts.collectAsState()
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsState()
    val occasionThemeConfig by viewModel.occasionThemeConfig.collectAsState()
    val biometricCredentials by viewModel.biometricCredentials.collectAsState()
    var directorPhoneForWhatsApp by remember { mutableStateOf("") }
    var showBroadcastToPrincipalsDialog by remember { mutableStateOf(false) }
    var editingExpirySchool by remember { mutableStateOf<RegisteredSchoolSummary?>(null) }
    var schoolPendingFullDelete by remember { mutableStateOf<RegisteredSchoolSummary?>(null) }
    var deleteSchoolConfirmInput by remember { mutableStateOf("") }

    var devNewUsername by remember { mutableStateOf("") }
    var devNewPassword by remember { mutableStateOf("") }
    var showDevPasswordPlain by remember { mutableStateOf(false) }
    var pendingBackupJsonToWrite by remember { mutableStateOf("") }
    var isBackupBusy by remember { mutableStateOf(false) }

    val saveBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && pendingBackupJsonToWrite.isNotBlank()) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(pendingBackupJsonToWrite.toByteArray(Charsets.UTF_8))
                }
                viewModel.showToastAlert("تم حفظ ملف النسخة الاحتياطية الشاملة (JSON) على جهازك بنجاح", isSuccess = true)
            }.onFailure { err ->
                viewModel.showToastAlert("تعذر كتابة ملف النسخة الاحتياطية: ${err.message}", isSuccess = false)
            }
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                val rawJson = context.contentResolver.openInputStream(uri)?.use { input ->
                    input.bufferedReader(Charsets.UTF_8).readText()
                }.orEmpty()
                if (rawJson.isNotBlank()) {
                    isBackupBusy = true
                    viewModel.restoreFullSystemCloudBackupJson(rawJson)
                    isBackupBusy = false
                } else {
                    viewModel.showToastAlert("ملف النسخة الاحتياطية المختار فارغ", isSuccess = false)
                }
            }.onFailure { err ->
                isBackupBusy = false
                viewModel.showToastAlert("تعذر قراءة ملف النسخة الاحتياطية: ${err.message}", isSuccess = false)
            }
        }
    }

    var updateVersionCodeStr by remember(appUpdateConfig) { mutableStateOf(appUpdateConfig.latestVersionCode.toString()) }
    var updateVersionName by remember(appUpdateConfig) { mutableStateOf(appUpdateConfig.latestVersionName) }
    var updateNotes by remember(appUpdateConfig) { mutableStateOf(appUpdateConfig.updateNotes) }
    var updateDownloadUrl by remember(appUpdateConfig) { mutableStateOf(appUpdateConfig.downloadUrl) }
    var isForceUpdate by remember(appUpdateConfig) { mutableStateOf(appUpdateConfig.isForceUpdate) }

    var isOccasionBannerActive by remember(occasionThemeConfig) { mutableStateOf(occasionThemeConfig.isActive) }
    var selectedOccasionIconKey by remember(occasionThemeConfig) {
        mutableStateOf(OccasionIconKey.fromKey(occasionThemeConfig.selectedIconKey))
    }
    var customOccasionTitle by remember(occasionThemeConfig) {
        mutableStateOf(occasionThemeConfig.customTitle)
    }
    var customOccasionGreeting by remember(occasionThemeConfig) {
        mutableStateOf(occasionThemeConfig.customBannerText)
    }
    var customOccasionIconBase64 by remember(occasionThemeConfig) { mutableStateOf(occasionThemeConfig.customIconBase64) }
    var customOccasionIconUrl by remember(occasionThemeConfig) { mutableStateOf(occasionThemeConfig.customIconUrl) }
    var customPrimaryHex by remember(occasionThemeConfig) { mutableStateOf(occasionThemeConfig.effectivePrimaryHex) }
    var customGradientHex by remember(occasionThemeConfig) { mutableStateOf(occasionThemeConfig.effectiveGradientHex) }

    BackHandler {
        viewModel.exitDeveloperPanel()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 18.dp, vertical = 14.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Selected Developer Hub Section (-1 = All sections visible, 0..3 = Focused section)
            var selectedDevSection by remember { mutableStateOf(-1) }

            // 3D Royal Wave Hero Header for Developer Control Panel
            ThreeDHeroHeaderCard(
                title = "لوحة تحكم النظام العام",
                subtitle = "إدارة المدارس والاشتراكات وتأسيس المنصات وبث التنبيهات والنسخ الاحتياطي",
                rightPillText = "المدارس المشتركة: ${registeredSchools.size}",
                rightPillIcon = Icons.Filled.School,
                leftPillText = "الأكواد المتاحة: ${licenses.count { !it.used }}",
                leftPillIcon = Icons.Filled.VpnKey,
                illustrationType = ThreeDIllustrationType.SECURITY_SHIELD,
                extraBottomContent = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NoWrapBadge(
                            text = "الأكواد المستخدمة: ${licenses.count { it.used }}",
                            icon = Icons.Filled.Verified,
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    viewModel.refreshDeveloperCloudData()
                                    viewModel.showToastAlert("تمت مزامنة التراخيص والمدارس بنجاح", isSuccess = true)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "مزامنة",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleDarkMode,
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Icon(
                                    imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = "تبديل المظهر",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            IconButton(
                                onClick = { viewModel.exitDeveloperPanel() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed)
                                    .testTag("exit_developer_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "خروج",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            )

            // 4-Section 3D Categorized Navigation Bar for Developer Control Panel
            val devSections = remember {
                listOf(
                    Triple(0, "إدارة المدارس والاشتراكات", Icons.Filled.Domain),
                    Triple(1, "تأسيس مدرسة جديدة", Icons.Filled.AddBusiness),
                    Triple(2, "بث تنبيهات المطور الحية", Icons.Filled.Campaign),
                    Triple(3, "النسخ الاحتياطي والأمان", Icons.Filled.EnhancedEncryption)
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                devSections.chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowItems.forEach { (secIdx, secTitle, secIcon) ->
                            val isSelected = selectedDevSection == secIdx
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) Color(0xFF059669) else Color.White,
                                border = BorderStroke(
                                    1.2.dp,
                                    if (isSelected) Color(0xFFA7F3D0) else Color(0xFFCBD5E1)
                                ),
                                shadowElevation = if (isSelected) 6.dp else 2.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable {
                                        selectedDevSection = if (selectedDevSection == secIdx) -1 else secIdx
                                    }
                                    .testTag("dev_hub_tab_$secIdx")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = secIcon,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF059669),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = secTitle,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A),
                                        maxLines = 1,
                                        softWrap = false,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Biometric Fingerprint Quick Login Toggle for Developer Account
            BiometricSettingsSectionCard(
                currentAccountUsername = SecurityShieldHelper.defaultDeveloperIdentifierToken(),
                currentAccountFullName = "إدارة النظام العام",
                currentAccountRoleLabel = "مطور النظام العام",
                savedBiometricCredentials = biometricCredentials,
                onEnableBiometricVerified = {
                    viewModel.enableBiometricForDeveloper()
                },
                onDisableBiometric = {
                    viewModel.disableBiometricLogin()
                },
                onErrorToast = { err ->
                    viewModel.showToastAlert(err, isSuccess = false)
                }
            )

            // 7.1: Encrypted Cloud Developer Credentials Vault Card (تشفير وأرشفة بيانات المطور سحابياً)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EnhancedEncryption,
                            contentDescription = null,
                            tint = DeepEmerald
                        )
                        Column {
                            Text(
                                text = "خزنة تشفير وأرشفة بيانات دخول المطور سحابياً",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "محفوظة بتشفير SHA-256 + AES داخل (system_secure_config/developer_vault) دون أي نص صريح في الكود",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = devNewUsername,
                            onValueChange = { devNewUsername = it },
                            label = { Text("معرّف المطور الجديد") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_vault_new_username_input")
                        )
                        OutlinedTextField(
                            value = devNewPassword,
                            onValueChange = { devNewPassword = it },
                            label = { Text("كلمة المرور السحابية الجديدة") },
                            singleLine = true,
                            visualTransformation = if (showDevPasswordPlain) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                            trailingIcon = {
                                IconButton(onClick = { showDevPasswordPlain = !showDevPasswordPlain }) {
                                    Icon(
                                        imageVector = if (showDevPasswordPlain) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = null
                                    )
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("dev_vault_new_password_input")
                        )
                    }

                    VectorActionButton(
                        text = "تشفير وحفظ بيانات دخول المطور في السحابة",
                        icon = Icons.Filled.LockReset,
                        containerColor = DeepEmerald,
                        onClick = {
                            viewModel.updateDeveloperCredentialsInCloud(
                                newUsername = devNewUsername,
                                newPassword = devNewPassword
                            )
                            if (devNewUsername.trim().length >= 3 && devNewPassword.trim().length >= 4) {
                                devNewUsername = ""
                                devNewPassword = ""
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_vault_save_button"
                    )
                }
            }

            // 7.2: Full System Cloud Backup & Restore Card (النسخ الاحتياطي الشامل واستعادة النظام سحابياً)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudSync,
                            contentDescription = null,
                            tint = DeepEmerald
                        )
                        Column {
                            Text(
                                text = "النسخ الاحتياطي الشامل واستعادة النظام سحابياً (Backup & Restore)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "تصدير أو استعادة كافة المدارس والحسابات والطلبة والحضور والدرجات والمحافظ والتراخيص بملف JSON موحّد",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VectorActionButton(
                            text = if (isBackupBusy) "جاري المعالجة..." else "تنزيل نسخة احتياطية شاملة (JSON)",
                            icon = Icons.Filled.CloudDownload,
                            containerColor = DeepEmerald,
                            onClick = {
                                if (!isBackupBusy) {
                                    isBackupBusy = true
                                    val jsonOut = viewModel.exportFullSystemCloudBackupJson()
                                    isBackupBusy = false
                                    if (jsonOut.isNotBlank()) {
                                        pendingBackupJsonToWrite = jsonOut
                                        val dateStamp = com.example.ui.components.ArabicCalendarDateHelper.todayIsoDate()
                                        saveBackupLauncher.launch("madrasati_cloud_backup_$dateStamp.json")
                                    }
                                }
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_export_full_backup_button"
                        )

                        OutlinedButton(
                            onClick = {
                                if (!isBackupBusy) {
                                    restoreBackupLauncher.launch(arrayOf("application/json", "text/plain", "*/*"))
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .testTag("dev_restore_full_backup_button"),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepEmerald)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "رفع واستعادة نسخة احتياطية",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            // Developer-Only Curriculum Manager (global_curriculum_config)
            DeveloperCurriculumManagerSection(viewModel = viewModel)

            // 1 & 3: Generate License Code, Direct School Setup & Broadcast to School Principals Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "توليد أكواد التفعيل، تأسيس المدارس، والتعميم للمدراء",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حفظ فوري في مجموعة school_licenses و developer_broadcasts على Firebase Firestore",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    VectorActionButton(
                        text = "توليد كود تفعيل لمدرسة جديدة",
                        icon = Icons.Filled.AddModerator,
                        onClick = { viewModel.generateNewSchoolLicense() },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_generate_license_button"
                    )

                    VectorActionButton(
                        text = "إرسال تعميم لمدراء المدارس (إشعار خاص للمدراء فقط)",
                        icon = Icons.Filled.Campaign,
                        containerColor = DeepEmerald,
                        onClick = { showBroadcastToPrincipalsDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "dev_broadcast_to_principals_button"
                    )

                    OutlinedButton(
                        onClick = { viewModel.openSchoolSetupFromDeveloper() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("dev_direct_school_setup_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AddBusiness,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "+ تأسيس مدرسة جديدة الآن",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    var editingBroadcast by remember { mutableStateOf<DeveloperBroadcastItem?>(null) }

                    if (developerBroadcasts.isNotEmpty()) {
                        HorizontalDivider(color = DeepEmerald.copy(alpha = 0.15f))
                        Text(
                            text = "التعاميم المرسلة لمدراء المدارس (${developerBroadcasts.size}):",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepForestGreen,
                            fontWeight = FontWeight.ExtraBold
                        )
                        developerBroadcasts.forEach { bc ->
                            Surface(
                                color = Color(0xFFECFDF5),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            DeveloperSelectedIconBox(
                                                iconKey = OccasionIconKey.fromKey(bc.iconKey),
                                                size = 36.dp
                                            )
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${bc.title} (${bc.createdAt})",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = DeepForestGreen,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Text(
                                                    text = bc.message,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = DeepEmerald
                                                )
                                            }
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            TextButton(
                                                onClick = { editingBroadcast = bc },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("dev_edit_broadcast_btn_${bc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Edit,
                                                    contentDescription = "تعديل التعميم",
                                                    tint = DeepEmerald,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تعديل", style = MaterialTheme.typography.labelSmall, color = DeepEmerald, fontWeight = FontWeight.Bold)
                                            }
                                            TextButton(
                                                onClick = { viewModel.deleteDeveloperBroadcast(bc.id) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.testTag("dev_delete_broadcast_btn_${bc.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = "حذف التعميم",
                                                    tint = DangerRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("حذف", style = MaterialTheme.typography.labelSmall, color = DangerRed, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                    if (bc.imageBase64.isNotBlank() || bc.imageUrl.isNotBlank()) {
                                        DeveloperAttachedImageBannerSection(
                                            imageBase64 = bc.imageBase64,
                                            imageUrl = bc.imageUrl
                                        )
                                    }
                                }
                            }
                        }
                    }

                    editingBroadcast?.let { targetBc ->
                        var editedTitle by remember(targetBc.id) { mutableStateOf(targetBc.title) }
                        var editedMsg by remember(targetBc.id) { mutableStateOf(targetBc.message) }
                        var editedIconKey by remember(targetBc.id) {
                            mutableStateOf(OccasionIconKey.fromKey(targetBc.iconKey))
                        }
                        var editedImageBase64 by remember(targetBc.id) { mutableStateOf(targetBc.imageBase64) }
                        var editedImageUrl by remember(targetBc.id) { mutableStateOf(targetBc.imageUrl) }
                        AlertDialog(
                            onDismissRequest = { editingBroadcast = null },
                            title = {
                                Text("تعديل تعميم مدراء المدارس", fontWeight = FontWeight.ExtraBold)
                            },
                            text = {
                                Column(
                                    modifier = Modifier.verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = editedTitle,
                                        onValueChange = { editedTitle = it },
                                        label = { Text("عنوان التعميم") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    OutlinedTextField(
                                        value = editedMsg,
                                        onValueChange = { editedMsg = it },
                                        label = { Text("نص التعميم") },
                                        minLines = 3,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Text(
                                        text = "اختيار أيقونة مخصصة للتعميم:",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                    OccasionVectorIconSelectorGrid(
                                        selectedIconKey = editedIconKey,
                                        hasCustomImage = false,
                                        onSelectIcon = { editedIconKey = it }
                                    )
                                    Text(
                                        text = "إرفاق صورة مدمجة مع التعميم (اختياري):",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = DeepEmerald
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CompressedPhotoPickerButton(
                                            label = if (editedImageBase64.isBlank()) {
                                                "رفع صورة مرفقة من الجهاز"
                                            } else {
                                                "تغيير الصورة المرفقة"
                                            },
                                            currentBase64 = editedImageBase64,
                                            isAvatar = false,
                                            onImageCompressed = { editedImageBase64 = it },
                                            modifier = Modifier.weight(1f)
                                        )
                                        if (editedImageBase64.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = { editedImageBase64 = "" },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                                            ) {
                                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = editedImageUrl,
                                        onValueChange = { editedImageUrl = it },
                                        label = { Text("أو رابط صورة مرفقة (Image URL)") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    if (editedImageBase64.isNotBlank() || editedImageUrl.isNotBlank()) {
                                        DeveloperAttachedImageBannerSection(
                                            imageBase64 = editedImageBase64,
                                            imageUrl = editedImageUrl
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        viewModel.updateDeveloperBroadcast(
                                            broadcastId = targetBc.id,
                                            newTitle = editedTitle,
                                            newMessage = editedMsg,
                                            newIconKey = editedIconKey.name,
                                            newImageBase64 = editedImageBase64,
                                            newImageUrl = editedImageUrl
                                        )
                                        editingBroadcast = null
                                    },
                                    enabled = editedTitle.isNotBlank() && editedMsg.isNotBlank(),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald, contentColor = Color.White)
                                ) {
                                    Text("حفظ التعديل", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { editingBroadcast = null }) {
                                    Text("إلغاء")
                                }
                            }
                        )
                    }
                }
            }

            // Developer Upgrade A: Remote App Update Control Card (إدارة تحديثات التطبيق المباشرة + إيقاف وسحب التحديث)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = DeepEmerald)
                            Column {
                                Text(
                                    text = "إدارة تحديثات التطبيق عن بُعد (Remote App Update)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "نشر التحديث لجميع الهواتف أو إيقافه وسحبه فوراً بضغطة زر",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Live Active / Stopped Status Indicator & Instant Toggle Switch
                    val isCurrentlyActive = appUpdateConfig.isUpdateBroadcastActive
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isCurrentlyActive) Color(0xFFECFDF5) else Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isCurrentlyActive) EmeraldAccent else DangerRed.copy(alpha = 0.45f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isCurrentlyActive) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (isCurrentlyActive) DeepEmerald else DangerRed,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = if (isCurrentlyActive) {
                                            "حالة التحديث الحالية: مفعّل ويظهر للمستخدمين (${appUpdateConfig.latestVersionName})"
                                        } else {
                                            "حالة التحديث الحالية: متوقف / مسحوب عن المستخدمين"
                                        },
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (isCurrentlyActive) DeepEmerald else DangerRed,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "تفعيل أو إيقاف ظهور نافذة التحديث للمستخدمين لحظياً",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                            Switch(
                                checked = isCurrentlyActive,
                                onCheckedChange = { nextActive ->
                                    viewModel.toggleGlobalAppUpdateActive(nextActive)
                                },
                                modifier = Modifier.testTag("dev_toggle_update_active_switch")
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = updateVersionCodeStr,
                            onValueChange = { updateVersionCodeStr = it.filter { ch -> ch.isDigit() } },
                            label = { Text("رقم الإصدار (مثال: 2)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = updateVersionName,
                            onValueChange = { updateVersionName = it },
                            label = { Text("اسم الإصدار (2.5.0)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = updateDownloadUrl,
                        onValueChange = { updateDownloadUrl = it },
                        label = { Text("رابط تحميل التحديث المباشر (GitHub Release APK URL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = updateNotes,
                        onValueChange = { updateNotes = it },
                        label = { Text("ملاحظات ومزايا التحديث الجديد") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "تحديث إجباري (Force Update - يمنع الإغلاق)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(checked = isForceUpdate, onCheckedChange = { isForceUpdate = it })
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VectorActionButton(
                            text = "نشر التحديث فوراً لجميع المستخدمين",
                            icon = Icons.Filled.CloudUpload,
                            containerColor = DeepEmerald,
                            onClick = {
                                viewModel.updateGlobalAppUpdateConfig(
                                    latestVersionCode = updateVersionCodeStr.toIntOrNull() ?: 2,
                                    latestVersionName = updateVersionName.ifBlank { "2.5.0" },
                                    updateNotes = updateNotes,
                                    downloadUrl = updateDownloadUrl,
                                    isForceUpdate = isForceUpdate,
                                    isUpdateBroadcastActive = true
                                )
                            },
                            modifier = Modifier.weight(1.15f),
                            testTag = "dev_publish_app_update_btn"
                        )

                        VectorActionButton(
                            text = "إيقاف / سحب التحديث الحالي",
                            icon = Icons.Filled.Block,
                            containerColor = DangerRed,
                            onClick = {
                                isForceUpdate = false
                                viewModel.stopAndWithdrawGlobalAppUpdate()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_stop_app_update_btn"
                        )
                    }
                }
            }

            // Developer Upgrade B: Manual Occasion Theme + Vector SVG Icon Library + Default Hidden Toggle
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Filled.Celebration, contentDescription = null, tint = DeepEmerald)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ثيم المناسبات الموحد (إدخال يدوي + مكتبة أيقونات Vector)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "مخفي افتراضياً عن المستخدمين ولا يظهر فوق بطاقة الترحيب إلا عند تفعيله يدوياً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Toggle Switch: إظهار / إخفاء شريط المناسبة في المدارس
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isOccasionBannerActive) Color(0xFFECFDF5) else Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isOccasionBannerActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "إظهار / إخفاء شريط المناسبة في المدارس",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isOccasionBannerActive) Color(0xFF064E3B) else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (isOccasionBannerActive) {
                                        "الحالة الحالية: الشريط ظاهر ومفعل في جميع المدارس"
                                    } else {
                                        "الحالة الحالية: الشريط مخفي افتراضياً (الوضع الرسمي النظيف)"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isOccasionBannerActive) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = isOccasionBannerActive,
                                onCheckedChange = { checked ->
                                    isOccasionBannerActive = checked
                                    if (checked) {
                                        viewModel.updateGlobalOccasionTheme(
                                            themeType = OccasionThemeType.CUSTOM_MANUAL,
                                            customTitle = customOccasionTitle.ifBlank { "مناسبة خاصة" },
                                            customBannerText = customOccasionGreeting.ifBlank { "أطيب التهاني والتبريكات لجميع منتسبي المدرسة" },
                                            customIconBase64 = customOccasionIconBase64,
                                            customIconUrl = customOccasionIconUrl,
                                            primaryColorHex = customPrimaryHex,
                                            gradientColorHex = customGradientHex,
                                            launcherAliasName = selectedOccasionIconKey.launcherAliasName,
                                            selectedIconKey = selectedOccasionIconKey.name,
                                            isActive = true
                                        )
                                    } else {
                                        viewModel.toggleGlobalOccasionThemeActive(false)
                                    }
                                },
                                modifier = Modifier.testTag("dev_toggle_occasion_active_switch")
                            )
                        }
                    }

                    HorizontalDivider()

                    // Live Banner Preview inside Developer Panel
                    val previewConfig = OccasionThemeConfig(
                        isActive = isOccasionBannerActive,
                        themeType = OccasionThemeType.CUSTOM_MANUAL,
                        selectedIconKey = selectedOccasionIconKey.name,
                        customTitle = customOccasionTitle.ifBlank { "عنوان المناسبة (معاينة حية)" },
                        customBannerText = customOccasionGreeting.ifBlank { "عبارة التهنئة الترحيبية لجميع منتسبي المدارس" },
                        customIconBase64 = customOccasionIconBase64,
                        customIconUrl = customOccasionIconUrl,
                        primaryColorHex = customPrimaryHex,
                        gradientColorHex = customGradientHex,
                        launcherAliasName = selectedOccasionIconKey.launcherAliasName
                    )
                    Text(
                        text = "معاينة فورية لشكل شريط المناسبة عند التفعيل:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                    LiveOccasionBannerCard(
                        config = previewConfig,
                        forcePreview = true
                    )

                    // 1) حقل نصي: عنوان المناسبة
                    OutlinedTextField(
                        value = customOccasionTitle,
                        onValueChange = { customOccasionTitle = it },
                        label = { Text("1. عنوان المناسبة (مثال: عيد الفطر السعيد / يوم المعلم / تفوق دراسي)") },
                        placeholder = { Text("اكتب عنوان المناسبة هنا...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_occasion_title_input")
                    )

                    // 2) حقل نصي: عبارة التهنئة
                    OutlinedTextField(
                        value = customOccasionGreeting,
                        onValueChange = { customOccasionGreeting = it },
                        label = { Text("2. عبارة التهنئة الترحيبية") },
                        placeholder = { Text("اكتب عبارة التهنئة التي ستظهر تحت عنوان المناسبة...") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_occasion_greeting_input")
                    )

                    // 3) اختيار مجسم الأنيميشن ثلاثي الأبعاد المتحرك للتنبيه (7 مجسمات 3D متحركة)
                    Text(
                        text = "3. اختيار مجسم الأنيميشن ثلاثي الأبعاد المتحرك للتنبيه:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OccasionVectorIconSelectorGrid(
                        selectedIconKey = selectedOccasionIconKey,
                        hasCustomImage = false,
                        onSelectIcon = { iconKey ->
                            selectedOccasionIconKey = iconKey
                        }
                    )

                    Text(
                        text = "4. إرفاق/رفع صورة مدمجة تظهر مع التبليغ (من الجهاز أو عبر رابط):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompressedPhotoPickerButton(
                            label = if (customOccasionIconBase64.isBlank()) {
                                "إرفاق/رفع صورة مدمجة مع التنبيه من الجهاز"
                            } else {
                                "تغيير الصورة المرفقة مع التنبيه"
                            },
                            currentBase64 = customOccasionIconBase64,
                            isAvatar = false,
                            onImageCompressed = { b64: String ->
                                customOccasionIconBase64 = b64
                            },
                            modifier = Modifier.weight(1f)
                        )
                        if (customOccasionIconBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = { customOccasionIconBase64 = "" },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("إزالة الصورة")
                            }
                        }
                    }

                    OutlinedTextField(
                        value = customOccasionIconUrl,
                        onValueChange = { customOccasionIconUrl = it },
                        label = { Text("أو وضع رابط صورة مرفقة مع التنبيه (Image URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dev_occasion_image_url_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VectorActionButton(
                            text = "نشر التبليغ الآن",
                            icon = Icons.Filled.CheckCircle,
                            containerColor = DeepEmerald,
                            onClick = {
                                isOccasionBannerActive = true
                                viewModel.updateGlobalOccasionTheme(
                                    themeType = OccasionThemeType.CUSTOM_MANUAL,
                                    customTitle = customOccasionTitle.ifBlank { "مناسبة خاصة" },
                                    customBannerText = customOccasionGreeting.ifBlank { "أطيب التهاني والتبريكات لجميع منتسبي المدرسة" },
                                    customIconBase64 = customOccasionIconBase64,
                                    customIconUrl = customOccasionIconUrl,
                                    primaryColorHex = customPrimaryHex,
                                    gradientColorHex = customGradientHex,
                                    launcherAliasName = selectedOccasionIconKey.launcherAliasName,
                                    selectedIconKey = selectedOccasionIconKey.name,
                                    isActive = true
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_apply_occasion_theme_btn"
                        )

                        VectorActionButton(
                            text = "إيقاف التبليغ",
                            icon = Icons.Filled.Restore,
                            containerColor = DangerRed,
                            onClick = {
                                isOccasionBannerActive = false
                                customOccasionTitle = ""
                                customOccasionGreeting = ""
                                customOccasionIconBase64 = ""
                                customOccasionIconUrl = ""
                                selectedOccasionIconKey = OccasionIconKey.BELL
                                customPrimaryHex = OccasionThemeType.OFFICIAL.defaultPrimaryHex
                                customGradientHex = OccasionThemeType.OFFICIAL.defaultGradientHex
                                viewModel.resetGlobalOccasionThemeToOfficial()
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "dev_reset_occasion_theme_btn"
                        )
                    }
                }
            }

            // 2: Activation Codes List (قائمة أكواد التفعيل)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "قائمة أكواد التفعيل (school_licenses)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أكواد التفعيل المتاحة والمستخدمة لتأسيس المدارس لمرة واحدة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    SmartIraqiPhoneInputField(
                        value = directorPhoneForWhatsApp,
                        onValueChange = { directorPhoneForWhatsApp = it },
                        label = "رقم واتساب مدير المدرسة الجديدة (11 رقماً — اختياري قبل الإرسال)",
                        testTag = "dev_license_director_phone_input"
                    )

                    if (licenses.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "لا توجد أكواد تفعيل مولدة بعد",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "اضغط على زر (توليد كود تفعيل لمدرسة جديدة) أعلاه لإنشاء كود MAD-XXXX",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        licenses.forEach { lic ->
                            Surface(
                                color = if (lic.used) {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (lic.used) Icons.Filled.Lock else Icons.Filled.VpnKey,
                                                contentDescription = null,
                                                tint = if (lic.used) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary
                                            )
                                            Column {
                                                Text(
                                                    text = lic.code,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = "تاريخ الإنشاء: ${lic.createdAt}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }

                                        NoWrapBadge(
                                            text = if (lic.used) "مستخدم (Used)" else "متاح للتفعيل",
                                            icon = if (lic.used) Icons.Filled.CheckCircle else Icons.Filled.FiberNew,
                                            containerColor = if (lic.used) MaterialTheme.colorScheme.surfaceVariant else SuccessGreen.copy(alpha = 0.16f),
                                            contentColor = if (lic.used) MaterialTheme.colorScheme.onSurfaceVariant else SuccessGreen
                                        )
                                    }

                                    if (lic.used) {
                                        Text(
                                            text = "تم الاستخدام لتأسيس مدرسة: ${lic.usedBySchoolName.ifBlank { "مدرسة" }} (كود المدرسة: ${lic.usedBySchoolId})",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    } else {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                    clipboard?.setPrimaryClip(ClipData.newPlainText("Madrasati Activation Code", lic.code))
                                                    viewModel.showToastAlert("تم نسخ كود التفعيل (${lic.code}) إلى الحافظة", isSuccess = true)
                                                },
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .heightIn(min = 48.dp),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.ContentCopy,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "نسخ الكود",
                                                    style = MaterialTheme.typography.labelLarge
                                                )
                                            }

                                            VectorActionButton(
                                                text = "إرسال عبر واتساب",
                                                icon = Icons.AutoMirrored.Filled.Send,
                                                containerColor = Color(0xFF25D366),
                                                onClick = {
                                                    val msg = buildString {
                                                        append("السلام عليكم ورحمة الله وبركاته\n")
                                                        append("مرحباً بك في *منصة مدرستي الذكية*\n\n")
                                                        append("*كود تفعيل تأسيس المدرسة لمرة واحدة:*\n")
                                                        append("*${lic.code}*\n\n")
                                                        append("*طريقة تأسيس المدرسة:*\n")
                                                        append("1. افتح تطبيق مدرستي على هاتفك.\n")
                                                        append("2. في شاشة تسجيل الدخول، اضغط *5 ضغطات متتالية* على شعار المنصة في الأعلى.\n")
                                                        append("3. أدخل كود التفعيل (*${lic.code}*) لتفتح لك شاشة تأسيس المدرسة وإنشاء حساب المدير.\n\n")
                                                        append("مع تحيات إدارة *منصة مدرستي الذكية*")
                                                    }
                                                    ExternalIntents.sendWhatsAppMessage(
                                                        context = context,
                                                        rawPhone = directorPhoneForWhatsApp.trim(),
                                                        message = msg
                                                    )
                                                },
                                                modifier = Modifier.weight(1.3f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4: Subscribed Schools Management List (قائمة إدارة المدارس المشتركة - schools)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Domain,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = "قائمة إدارة المدارس المشتركة واشتراكاتها (schools)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "التحكم بتفعيل الاشتراك، تحديد تاريخ الانتهاء، وعداد الأيام المتبقية",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (registeredSchools.isEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "لا توجد مدارس مؤسسة بعد",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "عند تأسيس أي مدرسة عبر كود التفعيل أو مباشرة ستظهر هنا مع كودها الثلاثي",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        registeredSchools.forEach { school ->
                            val daysLeft = school.daysUntilExpiry() ?: 365L
                            val expiryBadgeColor = when {
                                daysLeft <= 0L -> DangerRed
                                daysLeft <= 14L -> WarningAmber
                                else -> SuccessGreen
                            }
                            val expiryBadgeText = when {
                                daysLeft < 0L -> "منتهي منذ ${-daysLeft} يوم"
                                daysLeft == 0L -> "ينتهي اليوم!"
                                else -> "متبقي $daysLeft يوم"
                            }

                            Surface(
                                color = Color.White,
                                border = BorderStroke(1.2.dp, Color(0xFFE2E8F0)),
                                shadowElevation = 4.dp,
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                        .padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = school.schoolName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF0F172A)
                                            )
                                            Text(
                                                text = "كود المدرسة المميز: ${school.schoolId} • تاريخ التأسيس: ${school.createdAt}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF059669)
                                            )
                                            Text(
                                                text = "${school.ministryAffiliation} • ${school.schoolGenderType} • ${school.schoolType} • ${school.schoolStage}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFF475569)
                                            )
                                            if (school.adminUsername.isNotBlank()) {
                                                Text(
                                                    text = "حساب المدير: ${school.adminUsername}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F3D5E)
                                                )
                                            }
                                        }

                                        Switch(
                                            checked = school.isActive,
                                            onCheckedChange = {
                                                viewModel.toggleSchoolSubscriptionActive(school.schoolId)
                                            },
                                            modifier = Modifier.testTag("toggle_school_active_${school.schoolId}")
                                        )
                                    }

                                    // Subscription Expiry Row with Remaining Days Badge & Edit Button
                                    Surface(
                                        color = Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "تاريخ انتهاء الاشتراك: ${school.subscriptionExpiryDate}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                NoWrapBadge(
                                                    text = expiryBadgeText,
                                                    icon = Icons.Filled.EventBusy,
                                                    containerColor = expiryBadgeColor.copy(alpha = 0.16f),
                                                    contentColor = expiryBadgeColor
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { editingExpirySchool = school },
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.testTag("edit_school_expiry_${school.schoolId}")
                                            ) {
                                                Icon(Icons.Filled.EditCalendar, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تحديد الانتهاء", style = MaterialTheme.typography.labelMedium, maxLines = 1, softWrap = false)
                                            }
                                        }
                                    }

                                    // Clean Horizontal Action Row: Status Badge + Horizontal Capsule Toggle + Delete Button (Zero vertical character wrapping)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NoWrapBadge(
                                            text = if (school.isActive) "الاشتراك مفعل ونشط" else "الاشتراك متوقف مؤقتاً",
                                            icon = if (school.isActive) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                            containerColor = if (school.isActive) SuccessGreen.copy(alpha = 0.16f) else DangerRed.copy(alpha = 0.16f),
                                            contentColor = if (school.isActive) SuccessGreen else DangerRed
                                        )

                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = if (school.isActive) DangerRed.copy(alpha = 0.10f) else SuccessGreen.copy(alpha = 0.14f),
                                                border = BorderStroke(
                                                    1.dp,
                                                    if (school.isActive) DangerRed.copy(alpha = 0.45f) else SuccessGreen.copy(alpha = 0.45f)
                                                ),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable { viewModel.toggleSchoolSubscriptionActive(school.schoolId) }
                                            ) {
                                                Text(
                                                    text = if (school.isActive) "إيقاف الاشتراك" else "تفعيل الاشتراك",
                                                    fontSize = 11.sp,
                                                    color = if (school.isActive) DangerRed else SuccessGreen,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = DangerRed.copy(alpha = 0.10f),
                                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.45f)),
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(50))
                                                    .clickable {
                                                        deleteSchoolConfirmInput = ""
                                                        schoolPendingFullDelete = school
                                                    }
                                                    .testTag("delete_school_cloud_btn_${school.schoolId}")
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.DeleteForever,
                                                        contentDescription = null,
                                                        tint = DangerRed,
                                                        modifier = Modifier.size(15.dp)
                                                    )
                                                    Text(
                                                        text = "حذف المدرسة",
                                                        fontSize = 11.sp,
                                                        color = DangerRed,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            DeveloperCreditFooter()
        }
    }

    if (showBroadcastToPrincipalsDialog) {
        var broadcastTitle by remember { mutableStateOf("تعميم وتحديث إداري هام لمدراء المدارس") }
        var broadcastMessage by remember { mutableStateOf("") }
        var broadcastIconKey by remember { mutableStateOf(OccasionIconKey.BELL) }
        var broadcastImageBase64 by remember { mutableStateOf("") }
        var broadcastImageUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showBroadcastToPrincipalsDialog = false },
            title = { Text("إرسال تعميم لمدراء المدارس المشتركة", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "سيصل هذا التعميم كإشعار فوري يظهر لحسابات (المدير ADMIN) فقط في جميع المدارس المشتركة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("عنوان التعميم أو التحديث") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        label = { Text("نص التعميم الموجه لمدراء المدارس") },
                        placeholder = { Text("اكتب تفاصيل التحديث أو التنبيه الإداري هنا...") },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "اختيار أيقونة مخصصة للتعميم:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                    OccasionVectorIconSelectorGrid(
                        selectedIconKey = broadcastIconKey,
                        hasCustomImage = false,
                        onSelectIcon = { broadcastIconKey = it }
                    )
                    Text(
                        text = "إرفاق/رفع صورة مدمجة مع التعميم (اختياري):",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = DeepEmerald
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CompressedPhotoPickerButton(
                            label = if (broadcastImageBase64.isBlank()) {
                                "رفع صورة مرفقة من الجهاز"
                            } else {
                                "تغيير الصورة المرفقة"
                            },
                            currentBase64 = broadcastImageBase64,
                            isAvatar = false,
                            onImageCompressed = { broadcastImageBase64 = it },
                            modifier = Modifier.weight(1f)
                        )
                        if (broadcastImageBase64.isNotBlank()) {
                            OutlinedButton(
                                onClick = { broadcastImageBase64 = "" },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    OutlinedTextField(
                        value = broadcastImageUrl,
                        onValueChange = { broadcastImageUrl = it },
                        label = { Text("أو رابط صورة مرفقة (Image URL)") },
                        placeholder = { Text("https://...") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (broadcastImageBase64.isNotBlank() || broadcastImageUrl.isNotBlank()) {
                        DeveloperAttachedImageBannerSection(
                            imageBase64 = broadcastImageBase64,
                            imageUrl = broadcastImageUrl
                        )
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إرسال التعميم لجميع المدراء",
                    icon = Icons.Filled.Send,
                    onClick = {
                        if (broadcastMessage.isNotBlank()) {
                            viewModel.sendDeveloperBroadcastToPrincipals(
                                title = broadcastTitle,
                                message = broadcastMessage,
                                iconKey = broadcastIconKey.name,
                                imageBase64 = broadcastImageBase64,
                                imageUrl = broadcastImageUrl
                            )
                            showBroadcastToPrincipalsDialog = false
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { showBroadcastToPrincipalsDialog = false }) { Text("إلغاء") }
            }
        )
    }

    editingExpirySchool?.let { school ->
        var expiryDateInput by remember(school) { mutableStateOf(school.subscriptionExpiryDate) }
        AlertDialog(
            onDismissRequest = { editingExpirySchool = null },
            title = { Text("تحديد تاريخ انتهاء اشتراك: ${school.schoolName}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "اختر تاريخ انتهاء الاشتراك من التقويم (يظهر تنبيه التجديد لمدير المدرسة تلقائياً قبل 14 يوماً من الانتهاء):",
                        style = MaterialTheme.typography.bodySmall
                    )
                    ModernCalendarDatePickerField(
                        selectedDateIso = expiryDateInput,
                        label = "تاريخ انتهاء الاشتراك",
                        onDateSelected = { selectedIso, _ -> expiryDateInput = selectedIso }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { expiryDateInput = DateCountdownHelper.defaultExpiryDatePlusDays(10) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("تجربة (بعد 10 أيام)", style = MaterialTheme.typography.labelSmall)
                        }
                        OutlinedButton(
                            onClick = { expiryDateInput = DateCountdownHelper.defaultExpiryDatePlusDays(365) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("سنة كاملة (+365 يوم)", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "حفظ ومزامنة تاريخ الانتهاء",
                    icon = Icons.Filled.Save,
                    onClick = {
                        if (expiryDateInput.isNotBlank()) {
                            viewModel.updateSchoolSubscriptionExpiryDate(
                                schoolId = school.schoolId,
                                newExpiryDate = expiryDateInput.trim()
                            )
                            editingExpirySchool = null
                        }
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingExpirySchool = null }) { Text("إلغاء") }
            }
        )
    }

    schoolPendingFullDelete?.let { targetSchool ->
        val isConfirmed = deleteSchoolConfirmInput.trim() == "حذف" ||
            deleteSchoolConfirmInput.trim().equals(targetSchool.schoolId, ignoreCase = true)
        AlertDialog(
            onDismissRequest = { schoolPendingFullDelete = null },
            icon = {
                Icon(
                    imageVector = Icons.Filled.WarningAmber,
                    contentDescription = null,
                    tint = DangerRed,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "تأكيد الحذف الجذري والنهائي للمدرسة من السحابة",
                    fontWeight = FontWeight.ExtraBold,
                    color = DangerRed
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "أنت على وشك حذف مدرسة (${targetSchool.schoolName}) ذات الكود (${targetSchool.schoolId}) جذرياً من قاعدة البيانات السحابية، بما يشمل جميع حساباتها وطلبتها وسجلات الحضور والدرجات والمحافظ.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "لتأكيد الحذف النهائي غير القابل للتراجع، اكتب كلمة (حذف) أو كود المدرسة (${targetSchool.schoolId}) في الحقل أدناه:",
                        style = MaterialTheme.typography.labelMedium,
                        color = DangerRed,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = deleteSchoolConfirmInput,
                        onValueChange = { deleteSchoolConfirmInput = it },
                        label = { Text("اكتب (حذف) أو (${targetSchool.schoolId}) للتأكيد") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("confirm_delete_school_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isConfirmed) {
                            val schoolIdToDelete = targetSchool.schoolId
                            schoolPendingFullDelete = null
                            deleteSchoolConfirmInput = ""
                            viewModel.deleteSchoolCompletelyFromCloud(schoolIdToDelete)
                        }
                    },
                    enabled = isConfirmed,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.testTag("confirm_delete_school_submit_btn")
                ) {
                    Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حذف المدرسة وبياناتها نهائياً", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolPendingFullDelete = null }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

