package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.SchoolConfig
import com.example.model.SchoolType
import com.example.ui.components.*

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
                    "دعم المدارس الأهلية والحكومية بمرونة كاملة",
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
                description = "احتفِ بأوائل الطلبة والمجتهدين في لوحة الشرف الذهبية، وتابع الأقساط الدراسية، البلاغات، ومعرض الفعاليات المدرسية بضغط صور ذكي.",
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
            // Top bar with brand & theme toggle
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

            // Animated Lottie-style Vector Scene
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

            // Slide indicators & Action Buttons
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
 * "تأسيس منصة المدرسة وإنشاء حساب المدير العام"
 * Strictly NO exposed role switcher buttons.
 */
@Composable
fun SchoolSetupScreen(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onCompleteSetup: (
        schoolName: String,
        address: String,
        schoolType: SchoolType,
        logoBase64: String,
        adminFullName: String,
        adminUsername: String,
        adminPassword: String
    ) -> Unit
) {
    var schoolName by remember { mutableStateOf("مدرستي النموذجية") }
    var address by remember { mutableStateOf("الموصل - زمار") }
    var selectedSchoolType by remember { mutableStateOf(SchoolType.PRIVATE) }
    var logoBase64 by remember { mutableStateOf("") }
    var adminFullName by remember { mutableStateOf("المدير العام") }
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
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoWrapBadge(
                    text = "التأسيس لأول مرة • إعداد الإدارة",
                    icon = Icons.Filled.AdminPanelSettings,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
                text = "تأسيس منصة المدرسة وإنشاء حساب المدير العام",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Text(
                text = "أدخل البيانات الرسمية للمدرسة وعيّن بيانات الدخول لحساب الإدارة العامة",
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

                    CompressedPhotoPickerButton(
                        label = "رفع شعار المدرسة (ضغط تلقائي 800px)",
                        currentBase64 = logoBase64,
                        onImageCompressed = { logoBase64 = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                    Text(
                        text = "2. بيانات حساب المدير العام",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = adminFullName,
                        onValueChange = { adminFullName = it },
                        label = { Text("اسم المدير العام") },
                        placeholder = { Text("أدخل الاسم الثلاثي للمدير") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = adminUsername,
                        onValueChange = { adminUsername = it },
                        label = { Text("اسم المستخدم لحساب الإدارة") },
                        placeholder = { Text("admin") },
                        leadingIcon = { Icon(Icons.Filled.VerifiedUser, contentDescription = null) },
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
                        text = "تأسيس المنصة واعتماد حساب المدير العام",
                        icon = Icons.Filled.CheckCircle,
                        onClick = {
                            if (schoolName.isBlank() || adminUsername.isBlank() || adminPassword.isBlank()) {
                                errorMessage = "يرجى إدخال اسم المدرسة واسم المستخدم وكلمة المرور للمدير العام"
                            } else {
                                onCompleteSetup(
                                    schoolName,
                                    address.ifBlank { "الموصل - زمار" },
                                    selectedSchoolType,
                                    logoBase64,
                                    adminFullName,
                                    adminUsername,
                                    adminPassword
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
 * Unified Login Screen (شاشة تسجيل الدخول الموحدة):
 * Strictly NO exposed role-switching buttons.
 * Users (مدير، معلم، ولي أمر، طالب) log in exclusively with their credentials approved by Administration.
 */
@Composable
fun UnifiedLoginScreen(
    schoolConfig: SchoolConfig,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onLoginAttempt: (String, String) -> Result<*>
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loginError by remember { mutableStateOf<String?>(null) }

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
            // Top Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NoWrapBadge(
                    text = "${schoolConfig.schoolType.arabicLabel} • ${schoolConfig.address}",
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

            // Center Login Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Base64AvatarImage(
                    base64Data = schoolConfig.logoBase64,
                    fallbackIcon = Icons.Filled.School,
                    contentDescription = schoolConfig.schoolName,
                    size = 84.dp,
                    tint = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = schoolConfig.schoolName,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "بوابة تسجيل الدخول الموحدة (إدارة • معلمين • أولياء أمور • طلبة)",
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
                            placeholder = { Text("أدخل اسم المستخدم المعتمد من الإدارة") },
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
                            text = "دخول إلى المنصة",
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
