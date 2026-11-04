package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
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
 * Unified Login Screen (بوابة تسجيل الدخول الموحدة)
 * - Default screen on any device installing the app.
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
    onBiometricLogin: () -> Result<*> = { Result.success(null) }
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

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
                // Top Row: School Capsule + Theme Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
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
                                text = if (isSchoolVerified) {
                                    "منصة: $discoveredSchoolName"
                                } else {
                                    "منصة مدرستي الذكية"
                                },
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                softWrap = false
                            )
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
                    // Seamless 3D Wave Hero Header for Unified Login Screen (Exclusive 3D Digital Portal & Key Animation)
                    ThreeDHeroHeaderCard(
                        title = "منصة مدرستي الذكية",
                        subtitle = "بوابة تسجيل الدخول الموحدة والآمنة للهيئة الإدارية والتدريسية والطلبة وأولياء الأمور",
                        rightPillText = "",
                        rightPillIcon = Icons.Filled.School,
                        leftPillText = "",
                        leftPillIcon = Icons.Filled.VerifiedUser,
                        illustrationType = ThreeDIllustrationType.SECURITY_SHIELD
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

                            // Mandatory Prominent Emerald Notice Banner for Students, Parents & Teachers
                            Surface(
                                color = Color(0xFFECFDF5),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.55f)),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("login_school_admin_credentials_notice")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF059669),
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                            Icon(
                                                imageVector = Icons.Filled.Campaign,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "ملاحظة: تُستلم بيانات الدخول لجميع الحسابات مباشرةً من إدارة المدرسة.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF065F46),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            // Single Verified School Identity Card (Revealed smoothly ONLY after typing a valid school code prefix)
                            AnimatedVisibility(visible = isSchoolVerified) {
                                val matchedLocalConfig = schoolConfig.isSetupCompleted &&
                                    schoolConfig.schoolId.isNotBlank() &&
                                    extractedSchoolCode == schoolConfig.schoolId
                                Surface(
                                    color = Color(0xFFECFDF5),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.2.dp, Color(0xFF10B981).copy(alpha = 0.65f)),
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("verified_school_identity_card")
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = Color.White,
                                            border = BorderStroke(1.5.dp, Color(0xFF10B981)),
                                            modifier = Modifier.size(44.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                                if (matchedLocalConfig && schoolConfig.logoBase64.isNotBlank()) {
                                                    Base64AvatarImage(
                                                        base64Data = schoolConfig.logoBase64,
                                                        fallbackIcon = Icons.Filled.Verified,
                                                        contentDescription = discoveredSchoolName.orEmpty(),
                                                        size = 38.dp,
                                                        tint = Color(0xFF059669)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = Icons.Filled.Verified,
                                                        contentDescription = null,
                                                        tint = Color(0xFF059669),
                                                        modifier = Modifier.size(24.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = discoveredSchoolName.orEmpty(),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF065F46),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(
                                                    shape = RoundedCornerShape(50),
                                                    color = Color(0xFFD1FAE5)
                                                ) {
                                                    Text(
                                                        text = "كود المدرسة: ${extractedSchoolCode.orEmpty()}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = Color(0xFF047857),
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                    )
                                                }
                                                if (matchedLocalConfig) {
                                                    Surface(
                                                        shape = RoundedCornerShape(50),
                                                        color = Color(0xFFFEF3C7)
                                                    ) {
                                                        Text(
                                                            text = "${schoolConfig.ministryAffiliation} • ${schoolConfig.schoolGenderType} • ${schoolConfig.schoolStage.arabicLabel}",
                                                            fontSize = 9.5.sp,
                                                            fontWeight = FontWeight.ExtraBold,
                                                            color = Color(0xFF92400E),
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                        )
                                                    }
                                                }
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
                            } else if (loginError != null) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
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
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = loginError!!,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            if (remainingLockoutSec > 0L) {
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
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
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = "الدرع الأمني مفعل: يرجى الانتظار ${remainingLockoutSec} ثانية قبل المحاولة مجدداً.",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }

                            VectorActionButton(
                                text = if (remainingLockoutSec > 0L) "مقفل مؤقتاً (${remainingLockoutSec}ث)" else "تسجيل الدخول",
                                icon = Icons.AutoMirrored.Filled.Login,
                                enabled = remainingLockoutSec == 0L,
                                onClick = {
                                    val res = onLoginAttempt(username, password)
                                    if (res.isFailure) {
                                        loginError = res.exceptionOrNull()?.message ?: "خطأ في تسجيل الدخول"
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                testTag = "login_submit_button"
                            )

                            if (biometricCredentials != null && biometricCredentials.isValid && remainingLockoutSec == 0L) {
                                BiometricQuickLoginButton(
                                    savedCredentials = biometricCredentials,
                                    onBiometricAuthenticated = {
                                        val res = onBiometricLogin()
                                        if (res.isFailure) {
                                            loginError = res.exceptionOrNull()?.message ?: "تعذر تسجيل الدخول بالبصمة المحفوظة"
                                        }
                                    },
                                    onBiometricError = { errMsg ->
                                        loginError = errMsg
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
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

