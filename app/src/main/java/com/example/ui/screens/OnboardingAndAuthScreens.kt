package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SchoolConfig
import com.example.model.SchoolStage
import com.example.model.SchoolType
import com.example.ui.components.*
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.util.ExternalIntents
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
    onFinishOnboarding: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlideData(
                badge = "الشريحة 1 من 3 • منصة مدرستي المتكاملة",
                title = "إدارة مدرسية ذكية بهوية عصرية",
                description = "منصة رقمية شاملة تربط إدارة المدرسة، الكادر التدريسي، أولياء الأمور، والطلبة في بيئة تعليمية موحدة وآمنة في الموصل - زمار.",
                highlights = listOf(
                    "هيكلة إدارية صارمة وصلاحيات مخصصة لكل حساب",
                    "دعم المدارس الأهلية والحكومية وكافة المراحل الدراسية",
                    "إشعارات فورية وتكامل مباشر مع واتساب"
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
                badge = "الشريحة 3 من 3 • التميز والتواصل المباشر",
                title = "لوحة الشرف الذهبية والتواصل الفعال",
                description = "احتفِ بأوائل الطلبة والمجتهدين في لوحة الشرف الذهبية، وتابع الأقساط الدراسية، البلاغات، ومعرض الفعاليات المدرسية المصور.",
                highlights = listOf(
                    "لوحة شرف احتفالية بأوسمة فيكتور ذهبية للطلبة الأوائل",
                    "نظام أقساط ذكي يتفعل تلقائياً للمدارس الأهلية",
                    "صندوق مراسلة مباشر وبلاغات رسمية مع أولياء الأمور"
                )
            )
        )
    }

    var currentSlide by remember { mutableIntStateOf(0) }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoWrapBadge(
                    text = "منصة مدرستي • الموصل - زمار",
                    icon = Icons.Filled.School,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("onboarding_theme_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل الوضع الليلي والنهاري",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (currentSlide < slides.lastIndex) {
                        TextButton(
                            onClick = onFinishOnboarding,
                            modifier = Modifier.testTag("skip_onboarding_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FastForward,
                                contentDescription = "تخطي",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تخطي",
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            AnimatedContent(
                targetState = currentSlide,
                label = "onboarding_slide_transition"
            ) { index ->
                val slide = slides[index]
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OnboardingAnimatedScene(slideIndex = index)

                    Spacer(modifier = Modifier.height(20.dp))

                    NoWrapBadge(
                        text = slide.badge,
                        icon = Icons.Filled.AutoAwesome,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = slide.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = slide.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            slide.highlights.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = point,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

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
                                .height(8.dp)
                                .width(if (active) 28.dp else 10.dp)
                                .clip(CircleShape)
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
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
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "السابق",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "السابق",
                                style = MaterialTheme.typography.labelLarge,
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
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (currentSlide == slides.lastIndex) Icons.Filled.RocketLaunch else Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "البدء الآن",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (currentSlide == slides.lastIndex) "البدء الآن" else "التالي",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                DeveloperCreditFooter()
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
    onCompleteSetup: (
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String,
        schoolStage: SchoolStage
    ) -> Unit
) {
    BackHandler { onCancelSetup() }

    var schoolName by remember { mutableStateOf("مدرستي النموذجية") }
    var address by remember { mutableStateOf("الموصل - زمار") }
    var selectedSchoolType by remember { mutableStateOf(SchoolType.PRIVATE) }
    var selectedSchoolStage by remember { mutableStateOf(SchoolStage.SECONDARY) }
    var logoBase64 by remember { mutableStateOf("") }
    var adminFullName by remember { mutableStateOf("المدير") }
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
                        placeholder = { Text("مثال: ثانوية زمار النموذجية") },
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
                        placeholder = { Text("الموصل - زمار") },
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
                            if (schoolName.isBlank() || adminUsername.isBlank() || adminPassword.isBlank()) {
                                errorMessage = "يرجى إدخال اسم المدرسة واسم المستخدم وكلمة المرور للمدير"
                            } else {
                                onCompleteSetup(
                                    schoolName,
                                    address.ifBlank { "الموصل - زمار" },
                                    selectedSchoolType,
                                    logoBase64,
                                    adminFullName,
                                    adminUsername,
                                    adminPassword,
                                    selectedSchoolStage
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "complete_school_setup_button"
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            DeveloperCreditFooter()
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
    onVerifyActivationCode: (String, (Boolean) -> Unit) -> Unit = { _, _ -> }
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

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
                        text = "أدخل كود التفعيل لمرة واحدة (مثال: MAD-XXXX) المرسل إليك من المطور أحمد أبو رسلان لفتح شاشة تأسيس مدرسة جديدة:",
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoWrapBadge(
                    text = if (schoolConfig.isSetupCompleted && schoolConfig.schoolId.isNotBlank()) {
                        "كود المدرسة: ${schoolConfig.schoolId} • ${schoolConfig.schoolStage.arabicLabel}"
                    } else {
                        "منصة مدرستي الذكية • الموصل - زمار"
                    },
                    icon = Icons.Filled.LocationOn,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )

                IconButton(
                    onClick = onToggleDarkMode,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .testTag("login_theme_toggle")
                ) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                        contentDescription = "تبديل الوضع الليلي والنهاري",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tapping 5 consecutive times on the top platform logo opens the Activation Code dialog
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
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
                    Base64AvatarImage(
                        base64Data = schoolConfig.logoBase64,
                        fallbackIcon = Icons.Filled.School,
                        contentDescription = schoolConfig.schoolName,
                        size = 84.dp,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (schoolConfig.isSetupCompleted) schoolConfig.schoolName else "منصة مدرستي المتكاملة",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "بوابة تسجيل الدخول الموحدة",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "تسجيل الدخول ببيانات الحساب المعتمدة",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = {
                                username = it
                                loginError = null
                            },
                            label = { Text("اسم المستخدم") },
                            placeholder = { Text("مثال: 105-1 أو 105-ali") },
                            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("login_username_input"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                loginError = null
                            },
                            label = { Text("كلمة المرور") },
                            placeholder = { Text("أدخل كلمة المرور") },
                            leadingIcon = { Icon(Icons.Filled.Key, contentDescription = null) },
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
                            shape = RoundedCornerShape(12.dp)
                        )

                        if (loginError != null) {
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

                        VectorActionButton(
                            text = "تسجيل الدخول",
                            icon = Icons.AutoMirrored.Filled.Login,
                            onClick = {
                                val res = onLoginAttempt(username, password)
                                if (res.isFailure) {
                                    loginError = res.exceptionOrNull()?.message ?: "خطأ في تسجيل الدخول"
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            testTag = "login_submit_button"
                        )

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "يتم إنشاء وتفعيل حسابات المعلمين وأولياء الأمور والطلبة حصرياً من قبل إدارة المدرسة وإرسالها عبر واتساب.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            DeveloperCreditFooter()
        }
    }
}

/**
 * Internal Developer Super-Admin Control Panel:
 * "لوحة تحكم المطور - أحمد أبو رسلان"
 * Opened exclusively when logging in with ABU-RASLAN / 07512362567.
 */
@Composable
fun DeveloperControlPanelScreen(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val licenses by viewModel.schoolLicenses.collectAsState()
    val registeredSchools by viewModel.registeredSchools.collectAsState()
    var directorPhoneForWhatsApp by remember { mutableStateOf("") }

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
            // Top Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Terminal,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(30.dp)
                            )
                            Column {
                                Text(
                                    text = "لوحة تحكم المطور - أحمد أبو رسلان",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "إدارة التراخيص، تأسيس المدارس، واشتراكات السحابة (Firestore)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            IconButton(
                                onClick = {
                                    viewModel.refreshDeveloperCloudData()
                                    viewModel.showToastAlert("تمت مزامنة التراخيص والمدارس مع السحابة", isSuccess = true)
                                },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.18f))
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Sync,
                                    contentDescription = "مزامنة السحابة",
                                    tint = Color.White
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
                                    tint = Color.White
                                )
                            }
                            IconButton(
                                onClick = { viewModel.exitDeveloperPanel() },
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(DangerRed.copy(alpha = 0.85f))
                                    .testTag("exit_developer_panel_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = "خروج",
                                    tint = Color.White
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        NoWrapBadge(
                            text = "الأكواد المتاحة: ${licenses.count { !it.used }}",
                            icon = Icons.Filled.VpnKey,
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                        NoWrapBadge(
                            text = "الأكواد المستخدمة: ${licenses.count { it.used }}",
                            icon = Icons.Filled.Verified,
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                        NoWrapBadge(
                            text = "المدارس المشتركة: ${registeredSchools.size}",
                            icon = Icons.Filled.School,
                            containerColor = Color.White.copy(alpha = 0.18f),
                            contentColor = Color.White
                        )
                    }
                }
            }

            // 1 & 3: Generate License Code & Direct School Setup Card
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
                                text = "توليد أكواد التفعيل وتأسيس المدارس",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "حفظ فوري في مجموعة school_licenses على Firebase Firestore",
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

                    OutlinedTextField(
                        value = directorPhoneForWhatsApp,
                        onValueChange = { directorPhoneForWhatsApp = it },
                        label = { Text("رقم واتساب مدير المدرسة الجديدة (اختياري قبل الإرسال)") },
                        placeholder = { Text("مثال: 0751XXXXXXX") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
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
                                                        append("مرحباً بك في *منصة مدرستي الذكية* 🏫\n\n")
                                                        append("🔑 *كود تفعيل تأسيس المدرسة لمرة واحدة:*\n")
                                                        append("*${lic.code}*\n\n")
                                                        append("📌 *طريقة تأسيس المدرسة:*\n")
                                                        append("1. افتح تطبيق مدرستي على هاتفك.\n")
                                                        append("2. في شاشة تسجيل الدخول، اضغط *5 ضغطات متتالية* على شعار المنصة في الأعلى.\n")
                                                        append("3. أدخل كود التفعيل (*${lic.code}*) لتفتح لك شاشة تأسيس المدرسة وإنشاء حساب المدير.\n\n")
                                                        append("مع تحيات المطور: *المهندس أحمد أبو رسلان*")
                                                    }
                                                    ExternalIntents.sendWhatsAppMessage(
                                                        context = context,
                                                        rawPhone = directorPhoneForWhatsApp.ifBlank { "07512362567" },
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
                                text = "قائمة إدارة المدارس المشتركة (schools)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "عرض جميع المدارس المسجلة مع التحكم بتفعيل أو إيقاف اشتراك كل مدرسة",
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
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
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
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = school.schoolName,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "كود المدرسة المميز: ${school.schoolId} • تاريخ التأسيس: ${school.createdAt}",
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${school.schoolType} • ${school.schoolStage} • ${school.address}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            if (school.adminUsername.isNotBlank()) {
                                                Text(
                                                    text = "حساب المدير: ${school.adminUsername}",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.secondary
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

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        NoWrapBadge(
                                            text = if (school.isActive) "الاشتراك مفعّل (isActive: true)" else "الاشتراك متوقف (isActive: false)",
                                            icon = if (school.isActive) Icons.Filled.CheckCircle else Icons.Filled.Block,
                                            containerColor = if (school.isActive) SuccessGreen.copy(alpha = 0.16f) else DangerRed.copy(alpha = 0.16f),
                                            contentColor = if (school.isActive) SuccessGreen else DangerRed
                                        )

                                        TextButton(
                                            onClick = { viewModel.toggleSchoolSubscriptionActive(school.schoolId) }
                                        ) {
                                            Text(
                                                text = if (school.isActive) "إيقاف اشتراك المدرسة" else "تفعيل اشتراك المدرسة",
                                                color = if (school.isActive) DangerRed else SuccessGreen,
                                                fontWeight = FontWeight.Bold
                                            )
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
}
