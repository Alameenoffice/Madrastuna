package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.*
import com.example.ui.components.AdminSubscriptionAndBroadcastBanner
import com.example.ui.components.Base64AvatarImage
import com.example.ui.components.DigitalStudentIdDialog
import com.example.ui.components.GlobalImageLightboxDialog
import com.example.ui.components.ParentMultiChildSwitcherBar
import com.example.ui.components.ParentStudentCanteenWalletDialog
import com.example.ui.components.SmartCanteenPosAndWalletDialog
import com.example.ui.components.ProgressiveInfiniteScrollFooter
import com.example.ui.components.SchoolHolidaysManagementModalDialog
import com.example.ui.components.StudentActivitiesLinearVector
import com.example.ui.components.UrgentSchoolAlertModalDialog
import com.example.ui.components.VectorActionButton
import com.example.ui.screens.*
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldFoil
import com.example.ui.theme.GoldLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarmGoldLight
import com.example.util.DocumentAndIdExporter
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    @android.webkit.JavascriptInterface
    fun setAppIcon(aliasName: String) {
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(this, aliasName)
    }

    @android.webkit.JavascriptInterface
    fun changeLauncherIcon(themeKey: String) {
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(this, themeKey)
    }

    @android.webkit.JavascriptInterface
    fun isBiometricAvailable(): Boolean {
        return com.example.util.BiometricAuthHelper.isBiometricAvailable(this)
    }

    @android.webkit.JavascriptInterface
    fun authenticateBiometric() {
        runOnUiThread {
            com.example.util.BiometricAuthHelper.authenticateBiometric(
                context = this,
                title = com.example.util.BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
                subtitle = com.example.util.BiometricAuthHelper.DEFAULT_PROMPT_SUBTITLE,
                onSuccess = {},
                onError = {}
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val schoolViewModel: SchoolViewModel = viewModel()
            val isDarkMode by schoolViewModel.isDarkMode.collectAsState()

            MyApplicationTheme(darkTheme = isDarkMode) {
                MadrasatiRootApp(viewModel = schoolViewModel)
            }
        }
    }
}

@Composable
fun MadrasatiRootApp(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val onboardingSeen by viewModel.onboardingSeen.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()
    val isSchoolSetupUnlocked by viewModel.isSchoolSetupUnlocked.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()
    val discoveredSchoolName by viewModel.discoveredSchoolName.collectAsState()
    val loginConnectingMessage by viewModel.loginConnectingMessage.collectAsState()
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsState()
    val dismissedUpdateVersion by viewModel.dismissedUpdateVersion.collectAsState()
    val occasionThemeConfig by viewModel.occasionThemeConfig.collectAsState()
    val biometricCredentials by viewModel.biometricCredentials.collectAsState()
    val loginLockoutUntilMs by viewModel.loginLockoutUntilMs.collectAsState()
    val failedLoginAttemptsCount by viewModel.failedLoginAttemptsCount.collectAsState()

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(toastAlert?.id) {
        if (toastAlert != null) {
            delay(3600)
            viewModel.dismissToastAlert()
        }
    }

    LaunchedEffect(occasionThemeConfig.themeType, occasionThemeConfig.launcherAliasName) {
        com.example.util.AppIconSwitcherHelper.changeLauncherIcon(
            context,
            occasionThemeConfig.effectiveAliasName.ifBlank { occasionThemeConfig.themeType.name }
        )
    }

    val activeDiscoveredSchoolName = remember(
        discoveredSchoolName,
        schoolConfig.isSetupCompleted,
        schoolConfig.schoolId,
        schoolConfig.schoolName,
        biometricCredentials?.schoolName
    ) {
        when {
            !discoveredSchoolName.isNullOrBlank() -> discoveredSchoolName
            schoolConfig.isSetupCompleted && schoolConfig.schoolId.isNotBlank() && schoolConfig.schoolName.isNotBlank() -> schoolConfig.schoolName
            !biometricCredentials?.schoolName.isNullOrBlank() -> biometricCredentials?.schoolName
            else -> null
        }
    }
    val activeSchoolLogoBase64 = remember(schoolConfig.isSetupCompleted, schoolConfig.schoolId, schoolConfig.logoBase64) {
        if (schoolConfig.isSetupCompleted && schoolConfig.schoolId.isNotBlank()) schoolConfig.logoBase64 else ""
    }

    var showSplashScreen by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !onboardingSeen -> {
                OnboardingScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onFinishOnboarding = { viewModel.completeOnboarding() },
                    occasionThemeConfig = occasionThemeConfig,
                    discoveredSchoolName = activeDiscoveredSchoolName,
                    schoolLogoBase64 = activeSchoolLogoBase64
                )
            }
            isDeveloperAuthenticated -> {
                DeveloperControlPanelScreen(
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() }
                )
            }
            isSchoolSetupUnlocked -> {
                SchoolSetupScreen(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onCancelSetup = { viewModel.cancelSchoolSetupAndReturn() },
                    onCompleteSetup = { schoolName, address, schoolType, logoBase64, adminFullName, adminUsername, adminPassword, schoolStage ->
                        viewModel.setupSchoolAndAdmin(
                            schoolName = schoolName,
                            address = address,
                            schoolType = schoolType,
                            logoBase64 = logoBase64,
                            adminFullName = adminFullName,
                            adminUsername = adminUsername,
                            adminPassword = adminPassword,
                            schoolStage = schoolStage
                        )
                    }
                )
            }
            currentUser == null -> {
                UnifiedLoginScreen(
                    schoolConfig = schoolConfig,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { viewModel.toggleDarkMode() },
                    onLoginAttempt = { u, p -> viewModel.login(u, p) },
                    onVerifyActivationCode = { code, callback ->
                        viewModel.verifyActivationCodeAndUnlockSetup(code, callback)
                    },
                    discoveredSchoolName = activeDiscoveredSchoolName,
                    loginConnectingMessage = loginConnectingMessage,
                    onUsernameChanged = { u -> viewModel.previewSchoolNameByUsername(u) },
                    occasionThemeConfig = occasionThemeConfig,
                    loginLockoutUntilMs = loginLockoutUntilMs,
                    failedLoginAttemptsCount = failedLoginAttemptsCount,
                    biometricCredentials = biometricCredentials,
                    onBiometricLogin = { viewModel.loginWithSavedBiometrics() }
                )
            }
            else -> {
                AuthenticatedMainPlatform(viewModel = viewModel)
            }
        }

        // Choreographed Multi-Element Vector Splash Screen on Launch
        AnimatedVisibility(
            visible = showSplashScreen,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(420)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(9999f)
        ) {
            com.example.ui.components.ChoreographedSplashScreen(
                discoveredSchoolName = activeDiscoveredSchoolName,
                schoolLogoBase64 = activeSchoolLogoBase64,
                isDarkMode = isDarkMode,
                onSplashFinished = { showSplashScreen = false }
            )
        }

        // Global Smooth Interactive Image Lightbox Dialog
        GlobalImageLightboxDialog()

        // Global Remote App Update Notification Modal / Banner (Managed by System Developer)
        val shouldShowUpdateModal = appUpdateConfig.isUpdateActive &&
            appUpdateConfig.apkDownloadUrl.isNotBlank() &&
            !isDeveloperAuthenticated &&
            (appUpdateConfig.isForceUpdate || dismissedUpdateVersion != appUpdateConfig.latestVersionName)

        if (shouldShowUpdateModal) {
            AlertDialog(
                onDismissRequest = {
                    if (!appUpdateConfig.isForceUpdate) {
                        viewModel.dismissAppUpdateBannerLocally()
                    }
                },
                icon = {
                    Icon(
                        imageVector = Icons.Filled.SystemUpdate,
                        contentDescription = null,
                        tint = DeepEmerald,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "تحديث جديد متوفر للمنصة (${appUpdateConfig.latestVersionName})",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = appUpdateConfig.releaseNotes.ifBlank {
                                "يتوفر إصدار محدث من منصة مدرستي الذكية يتضمن مزايا وتحسينات جديدة."
                            },
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (appUpdateConfig.isForceUpdate) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DangerRed.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = "هذا التحديث إلزامي لضمان عمل كافة الخدمات السحابية بسلاسة.",
                                    modifier = Modifier.padding(8.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DangerRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            runCatching {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse(appUpdateConfig.apkDownloadUrl)
                                )
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DeepEmerald)
                    ) {
                        Icon(Icons.Filled.Download, contentDescription = null, tint = Color(0xFFA7F3D0))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تحديث الآن", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    if (!appUpdateConfig.isForceUpdate) {
                        TextButton(onClick = { viewModel.dismissAppUpdateBannerLocally() }) {
                            Text("لاحقاً")
                        }
                    }
                }
            )
        }

        // Global Toast / Snackbar Completion Alert Banner (Green on Success, Red with Reason on Error)
        // Rendered both in the root Box (zIndex MAX_VALUE) AND inside a top-level Popup window so it ALWAYS floats above any open Dialog or Modal!
        AnimatedVisibility(
            visible = toastAlert != null,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .zIndex(Float.MAX_VALUE)
        ) {
            val alert = toastAlert
            if (alert != null) {
                Surface(
                    color = if (alert.isSuccess) SuccessGreen else DangerRed,
                    shape = RoundedCornerShape(16.dp),
                    shadowElevation = 12.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("global_toast_alert_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = if (alert.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = alert.message,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        IconButton(
                            onClick = { viewModel.dismissToastAlert() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "إغلاق التنبيه",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Top-most Window Popup Layer so Toast / Action Banner ALWAYS appears above open Dialogs / Modals
        val activeToastForPopup = toastAlert
        if (activeToastForPopup != null) {
            androidx.compose.ui.window.Popup(
                alignment = Alignment.TopCenter,
                offset = androidx.compose.ui.unit.IntOffset(0, 24),
                properties = androidx.compose.ui.window.PopupProperties(
                    focusable = false,
                    dismissOnBackPress = false,
                    dismissOnClickOutside = false,
                    clippingEnabled = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .zIndex(Float.MAX_VALUE),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Surface(
                        color = if (activeToastForPopup.isSuccess) SuccessGreen else DangerRed,
                        shape = RoundedCornerShape(16.dp),
                        shadowElevation = 16.dp,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color.White.copy(alpha = 0.28f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (activeToastForPopup.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = activeToastForPopup.message,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.dismissToastAlert() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق التنبيه",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Full-Screen Offline Experience (بتصميم مطابق لفخامة شاشة الترحيب Onboarding/Splash بدون صندوق قاتم أو لون برتقالي)
        val isNetworkOnline by viewModel.isNetworkOnline.collectAsState()
        val isDarkOffline by viewModel.isDarkMode.collectAsState()
        AnimatedVisibility(
            visible = !isNetworkOnline,
            enter = fadeIn(animationSpec = tween(280)),
            exit = fadeOut(animationSpec = tween(220)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(99999f)
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "offline_splash_lottie_scene")
            val floatOffset by infiniteTransition.animateFloat(
                initialValue = -9f,
                targetValue = 9f,
                animationSpec = infiniteRepeatable(
                    animation = tween(2200, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offline_float_offset"
            )
            val pulseWave by infiniteTransition.animateFloat(
                initialValue = 0.86f,
                targetValue = 1.22f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1900, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offline_pulse_wave"
            )
            val orbitAngle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(12000, easing = androidx.compose.animation.core.LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "offline_orbit_angle"
            )
            val signalSweep by infiniteTransition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1500, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "offline_signal_sweep"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = if (isDarkOffline) {
                                listOf(
                                    Color(0xFF031F18),
                                    Color(0xFF063729),
                                    Color(0xFF04261D)
                                )
                            } else {
                                listOf(
                                    Color(0xFFEFFBF5),
                                    Color(0xFFF7FDF9),
                                    Color(0xFFFFFFFF)
                                )
                            }
                        )
                    )
                    .pointerInput(Unit) {
                        detectTapGestures { /* Block all underlying clicks/touches while offline */ }
                    }
                    .testTag("network_connectivity_guard_banner")
            ) {
                // Soft Ambient Glows (إضاءة محيطية ناعمة مطابقة لشاشة الترحيب)
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF10B981).copy(alpha = if (isDarkOffline) 0.16f else 0.12f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.85f, size.height * 0.15f),
                            radius = size.minDimension * 0.65f
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.15f),
                        radius = size.minDimension * 0.65f
                    )
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                GoldAccent.copy(alpha = if (isDarkOffline) 0.12f else 0.10f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.15f, size.height * 0.82f),
                            radius = size.minDimension * 0.60f
                        ),
                        center = Offset(size.width * 0.15f, size.height * 0.82f),
                        radius = size.minDimension * 0.60f
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Spacer(modifier = Modifier.height(8.dp))

                    // Center Multi-Layer JSON/Lottie-Style Animated Scene + Concise Typography
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(228.dp)
                                .clip(RoundedCornerShape(36.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFF042F2E),
                                            DeepEmerald,
                                            Color(0xFF0D9488)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cx = size.width / 2f
                                val cy = size.height / 2f

                                // Multi-layer animated radar waves searching for signal
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.07f),
                                    radius = size.minDimension * 0.44f * pulseWave,
                                    center = Offset(cx, cy)
                                )
                                drawCircle(
                                    color = Color(0xFF34D399).copy(alpha = 0.22f * signalSweep),
                                    radius = size.minDimension * 0.38f * pulseWave,
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2.2.dp.toPx())
                                )
                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.32f * signalSweep),
                                    radius = size.minDimension * 0.30f * (2f - pulseWave * 0.75f),
                                    center = Offset(cx, cy),
                                    style = Stroke(width = 2.dp.toPx())
                                )

                                // Orbiting golden & emerald smart nodes
                                val rad1 = Math.toRadians(orbitAngle.toDouble())
                                val rad2 = Math.toRadians((orbitAngle + 120f).toDouble())
                                val rad3 = Math.toRadians((orbitAngle + 240f).toDouble())
                                val orbitR = size.minDimension * 0.37f

                                drawCircle(
                                    color = GoldAccent.copy(alpha = 0.85f),
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(
                                        cx + (kotlin.math.cos(rad1) * orbitR).toFloat(),
                                        cy + (kotlin.math.sin(rad1) * orbitR).toFloat()
                                    )
                                )
                                drawCircle(
                                    color = Color(0xFF6EE7B7).copy(alpha = 0.80f),
                                    radius = 4.5.dp.toPx(),
                                    center = Offset(
                                        cx + (kotlin.math.cos(rad2) * orbitR).toFloat(),
                                        cy + (kotlin.math.sin(rad2) * orbitR).toFloat()
                                    )
                                )
                                drawCircle(
                                    color = Color.White.copy(alpha = 0.70f),
                                    radius = 4.dp.toPx(),
                                    center = Offset(
                                        cx + (kotlin.math.cos(rad3) * orbitR).toFloat(),
                                        cy + (kotlin.math.sin(rad3) * orbitR).toFloat()
                                    )
                                )

                                // Floating glassmorphic card backdrop
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.11f),
                                    topLeft = Offset(cx - 64.dp.toPx(), cy - 64.dp.toPx() + floatOffset),
                                    size = androidx.compose.ui.geometry.Size(128.dp.toPx(), 128.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx())
                                )
                                drawRoundRect(
                                    color = GoldAccent.copy(alpha = 0.42f),
                                    topLeft = Offset(cx - 64.dp.toPx(), cy - 64.dp.toPx() + floatOffset),
                                    size = androidx.compose.ui.geometry.Size(128.dp.toPx(), 128.dp.toPx()),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                                    style = Stroke(width = 1.4.dp.toPx())
                                )

                                // Animated 3D-style Signal Waves & Smart Hub Node
                                val waveCenterY = cy + floatOffset + 12.dp.toPx()
                                drawArc(
                                    color = Color.White.copy(alpha = 0.35f + 0.55f * signalSweep),
                                    startAngle = 215f,
                                    sweepAngle = 110f,
                                    useCenter = false,
                                    topLeft = Offset(cx - 38.dp.toPx(), waveCenterY - 38.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(76.dp.toPx(), 76.dp.toPx()),
                                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFFA7F3D0).copy(alpha = 0.45f + 0.50f * (1f - signalSweep * 0.5f)),
                                    startAngle = 220f,
                                    sweepAngle = 100f,
                                    useCenter = false,
                                    topLeft = Offset(cx - 25.dp.toPx(), waveCenterY - 25.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(50.dp.toPx(), 50.dp.toPx()),
                                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = GoldAccent,
                                    startAngle = 225f,
                                    sweepAngle = 90f,
                                    useCenter = false,
                                    topLeft = Offset(cx - 13.dp.toPx(), waveCenterY - 13.dp.toPx()),
                                    size = androidx.compose.ui.geometry.Size(26.dp.toPx(), 26.dp.toPx()),
                                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                                )
                                // Glowing Hub Core Dot
                                drawCircle(
                                    color = GoldAccent,
                                    radius = 5.5.dp.toPx(),
                                    center = Offset(cx, waveCenterY + 4.dp.toPx())
                                )
                                drawCircle(
                                    color = Color.White,
                                    radius = 2.4.dp.toPx(),
                                    center = Offset(cx, waveCenterY + 4.dp.toPx())
                                )
                            }
                        }

                        Text(
                            text = "انقطع الاتصال بالشبكة مؤقتاً",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkOffline) Color.White else DeepForestGreen,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "يرجى التحقق من اتصال جهازك بالإنترنت لاستئناف المزامنة السحابية الفورية.",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (isDarkOffline) Color.White.copy(alpha = 0.82f) else MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 25.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    // Bottom Luxury Capsule Emerald/Gold Action Button
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(DeepEmerald)
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(GoldAccent.copy(alpha = 0.65f))
                            )
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(DeepEmerald.copy(alpha = 0.30f))
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { viewModel.triggerImmediateCloudResync() }
                                .testTag("network_retry_sync_button"),
                            shape = RoundedCornerShape(50),
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.2.dp, GoldAccent.copy(alpha = 0.65f)),
                            shadowElevation = 8.dp
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                Color(0xFF042F2E),
                                                DeepEmerald,
                                                Color(0xFF0D9488)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Refresh,
                                        contentDescription = "إعادة الفحص الآن",
                                        tint = GoldAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "إعادة الفحص الآن",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthenticatedMainPlatform(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    val isSideDrawerEnabled by viewModel.isSideDrawerEnabled.collectAsState()
    val schoolConfig by viewModel.schoolConfig.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val activeSection by viewModel.activeSection.collectAsState()
    val allNotifications by viewModel.notifications.collectAsState()
    val latestBannerAlert by viewModel.latestBannerAlert.collectAsState()
    val allStudents by viewModel.students.collectAsState()
    val selectedParentChildId by viewModel.selectedParentChildId.collectAsState()
    val developerBroadcasts by viewModel.developerBroadcasts.collectAsState()
    val dismissedBroadcastIds by viewModel.dismissedBroadcastIds.collectAsState()
    val occasionThemeConfig by viewModel.occasionThemeConfig.collectAsState()

    val featureSettings by viewModel.featureSettings.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val notifications = remember(allNotifications, currentUser, allStudents) {
        viewModel.getScopedNotificationsForUser(currentUser)
    }

    val parentChildren = remember(allStudents, currentUser) {
        viewModel.getChildrenForParent(currentUser)
    }
    val selectedParentChild = remember(parentChildren, selectedParentChildId, currentUser) {
        parentChildren.firstOrNull { it.id == selectedParentChildId }
            ?: parentChildren.firstOrNull { it.id == currentUser?.linkedStudentId }
            ?: parentChildren.firstOrNull()
    }

    val loggedInStudentRecord = remember(allStudents, currentUser) {
        if (currentUser?.role == UserRole.STUDENT) {
            allStudents.firstOrNull {
                it.id == currentUser?.linkedStudentId ||
                    it.studentUsername.equals(currentUser?.username ?: "", ignoreCase = true)
            }
        } else null
    }

    val showUrgentAlertModal by viewModel.showUrgentAlertModal.collectAsState()
    val showHolidaysModal by viewModel.showHolidaysModal.collectAsState()
    val holidaysConfig by viewModel.holidaysConfig.collectAsState()
    val showIndexedLeaveRequestsModal by viewModel.showIndexedLeaveRequestsModal.collectAsState()
    val leaveRequests by viewModel.leaveRequests.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()

    val pendingLeaveRequestsCount = remember(leaveRequests, schoolConfig.schoolId) {
        leaveRequests.count {
            (it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId) &&
                it.status == LeaveRequestStatus.PENDING
        }
    }

    var idCardDialogStudent by remember { mutableStateOf<StudentRecord?>(null) }
    var showNotificationsSheet by remember { mutableStateOf(false) }
    var showCanteenPosDialogFromDrawer by remember { mutableStateOf(false) }
    var showStudentWalletDialogFromDrawer by remember { mutableStateOf(false) }
    val canManageCanteen = remember(currentUser) {
        viewModel.canUserManageCanteen(currentUser)
    }

    LaunchedEffect(featureSettings, activeSection, schoolConfig.schoolType) {
        if (!featureSettings.qrAttendanceEnabled && idCardDialogStudent != null) {
            idCardDialogStudent = null
        }
        if (activeSection == AppSection.HONOR_ROLL && !featureSettings.honorRollEnabled) {
            viewModel.navigateToSection(AppSection.DASHBOARD)
        } else if (activeSection == AppSection.TUITION_FEES &&
            (schoolConfig.schoolType == SchoolType.PUBLIC || !featureSettings.tuitionEnabled)
        ) {
            viewModel.navigateToSection(AppSection.DASHBOARD)
        } else if (activeSection == AppSection.STUDENT_ACTIVITIES && !featureSettings.studentActivitiesEnabled) {
            viewModel.navigateToSection(AppSection.DASHBOARD)
        } else if (activeSection == AppSection.EVENTS_NEWS && !featureSettings.eventsGalleryEnabled) {
            viewModel.navigateToSection(AppSection.DASHBOARD)
        }
    }

    val unreadCount = remember(notifications) { notifications.count { !it.isRead } }

    // BackHandler to return to Dashboard from any secondary section
    BackHandler(enabled = activeSection != AppSection.DASHBOARD) {
        viewModel.navigateToSection(AppSection.DASHBOARD)
    }

    val allSections = remember(schoolConfig.schoolType, featureSettings, currentUser) {
        buildList {
            add(AppSection.DASHBOARD to Icons.Filled.Dashboard)
            if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER) {
                add(AppSection.LIVE_OPERATIONS_RADAR to Icons.Filled.FactCheck)
            }
            add(AppSection.ATTENDANCE to Icons.Filled.HowToReg)
            add(AppSection.STUDENT_COMPREHENSIVE_ARCHIVE to Icons.Filled.FolderShared)
            add(AppSection.SCHEDULE_HOMEWORK to Icons.Filled.CalendarMonth)
            add(AppSection.EXAMS_GRADES to Icons.Filled.Assessment)
            if (featureSettings.studentActivitiesEnabled) {
                add(AppSection.STUDENT_ACTIVITIES to StudentActivitiesLinearVector)
            }
            if (featureSettings.honorRollEnabled) {
                add(AppSection.HONOR_ROLL to Icons.Filled.EmojiEvents)
            }
            if (schoolConfig.schoolType == SchoolType.PRIVATE &&
                featureSettings.tuitionEnabled &&
                currentUser?.role != UserRole.TEACHER
            ) {
                add(AppSection.TUITION_FEES to Icons.Filled.AccountBalanceWallet)
            }
            add(AppSection.REPORTS_CHAT to Icons.Filled.Forum)
            if (featureSettings.eventsGalleryEnabled) {
                add(AppSection.EVENTS_NEWS to Icons.Filled.PhotoLibrary)
            }
            if (currentUser?.role == UserRole.ADMIN) {
                add(AppSection.ADMIN_ACCOUNTS to Icons.Filled.ManageAccounts)
            }
            add(AppSection.SETTINGS to Icons.Filled.Settings)
        }
    }

    val bottomNavItems = remember(schoolConfig.schoolType, featureSettings) {
        buildList {
            add(AppSection.DASHBOARD to Icons.Filled.Dashboard)
            add(AppSection.ATTENDANCE to Icons.Filled.HowToReg)
            add(AppSection.SCHEDULE_HOMEWORK to Icons.Filled.MenuBook)
            add(AppSection.EXAMS_GRADES to Icons.Filled.Assessment)
            if (featureSettings.studentActivitiesEnabled) {
                add(AppSection.STUDENT_ACTIVITIES to StudentActivitiesLinearVector)
            } else if (featureSettings.honorRollEnabled) {
                add(AppSection.HONOR_ROLL to Icons.Filled.EmojiEvents)
            } else {
                add(AppSection.REPORTS_CHAT to Icons.Filled.Forum)
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isSideDrawerEnabled,
        drawerContent = {
            if (isSideDrawerEnabled) {
                val academicGroupSections = remember(currentUser) {
                    buildList {
                        add(AppSection.DASHBOARD to Icons.Filled.Dashboard)
                        if (currentUser?.role == UserRole.ADMIN || currentUser?.role == UserRole.TEACHER) {
                            add(AppSection.LIVE_OPERATIONS_RADAR to Icons.Filled.FactCheck)
                        }
                        add(AppSection.ATTENDANCE to Icons.Filled.HowToReg)
                        add(AppSection.STUDENT_COMPREHENSIVE_ARCHIVE to Icons.Filled.FolderShared)
                        add(AppSection.SCHEDULE_HOMEWORK to Icons.Filled.CalendarMonth)
                        add(AppSection.EXAMS_GRADES to Icons.Filled.Assessment)
                    }
                }
                val communityGroupSections = remember(schoolConfig.schoolType, featureSettings, currentUser) {
                    buildList {
                        if (featureSettings.studentActivitiesEnabled) {
                            add(AppSection.STUDENT_ACTIVITIES to StudentActivitiesLinearVector)
                        }
                        if (featureSettings.honorRollEnabled) {
                            add(AppSection.HONOR_ROLL to Icons.Filled.EmojiEvents)
                        }
                        add(AppSection.REPORTS_CHAT to Icons.Filled.Forum)
                        if (featureSettings.eventsGalleryEnabled) {
                            add(AppSection.EVENTS_NEWS to Icons.Filled.PhotoLibrary)
                        }
                        if (schoolConfig.schoolType == SchoolType.PRIVATE &&
                            featureSettings.tuitionEnabled &&
                            currentUser?.role != UserRole.TEACHER
                        ) {
                            add(AppSection.TUITION_FEES to Icons.Filled.AccountBalanceWallet)
                        }
                    }
                }

                ModalDrawerSheet(
                    drawerContainerColor = if (isDarkMode) Color(0xFF0F172A) else Color.White,
                    drawerContentColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A),
                    drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
                    modifier = Modifier.width(308.dp)
                ) {
                    val drawerBg = if (isDarkMode) Color(0xFF0F172A) else Color.White
                    val headerCardBg = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                    val headerBorderColor = if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                    val primaryTextColor = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF0F172A)
                    val secondaryTextColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(drawerBg)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // ب) رأس القائمة الأنيق والنظيف (Clean Profile & School Header)
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = headerCardBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, headerBorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFFECFDF5),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f)),
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Base64AvatarImage(
                                                base64Data = currentUser?.photoBase64?.ifBlank { schoolConfig.logoBase64 }.orEmpty(),
                                                fallbackIcon = Icons.Filled.School,
                                                contentDescription = schoolConfig.schoolName,
                                                size = 44.dp,
                                                tint = Color(0xFF059669)
                                            )
                                        }
                                    }

                                    val userFullName = currentUser?.fullName?.trim().orEmpty()
                                    val userRoleTitle = currentUser?.role?.arabicTitle?.trim().orEmpty()
                                    val displayUserName = userFullName.ifBlank { userRoleTitle }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = displayUserName,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = primaryTextColor,
                                                fontWeight = FontWeight.ExtraBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                modifier = Modifier.weight(1f, fill = false)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(50),
                                                color = Color(0xFFECFDF5),
                                                border = androidx.compose.foundation.BorderStroke(0.8.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = userRoleTitle,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF065F46),
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = schoolConfig.schoolName,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = secondaryTextColor,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .size(30.dp)
                                            .clip(CircleShape)
                                            .clickable { coroutineScope.launch { drawerState.close() } }
                                            .testTag("drawer_close_btn"),
                                        shape = CircleShape,
                                        color = if (isDarkMode) Color(0xFF334155) else Color(0xFFF1F5F9)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Close,
                                                contentDescription = "إغلاق القائمة الجانبية",
                                                tint = secondaryTextColor,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // ج) تنظيم العناصر في مجموعات نظيفة بدون صناديق أو إطارات صفراء
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                // المجموعة الأولى: الأقسام التعليمية
                                item(key = "drawer_group_academic") {
                                    LuxuryDrawerGroupCard(
                                        groupTitle = "الأقسام التعليمية",
                                        isDarkMode = isDarkMode
                                    ) {
                                        academicGroupSections.forEach { (section, icon) ->
                                            LuxuryDrawerMenuItem(
                                                title = section.arabicTitle,
                                                subtitle = when (section) {
                                                    AppSection.LIVE_OPERATIONS_RADAR ->
                                                        "إحصائية اليوم لحضور الطلبة وحركة بوابة المدرسة"
                                                    AppSection.STUDENT_COMPREHENSIVE_ARCHIVE ->
                                                        "تقارير الأداء، المواظبة، والنتائج الشهرية والسنوية"
                                                    else -> null
                                                },
                                                icon = icon,
                                                selected = activeSection == section,
                                                isDarkMode = isDarkMode,
                                                statusDotColor = null,
                                                testTag = "drawer_nav_${section.name}",
                                                onClick = {
                                                    viewModel.navigateToSection(section)
                                                    coroutineScope.launch { drawerState.close() }
                                                }
                                            )
                                        }
                                        LuxuryDrawerMenuItem(
                                            title = "طلبات الإجازة الإلكترونية",
                                            subtitle = if (pendingLeaveRequestsCount > 0) {
                                                "طلبات قيد المراجعة: $pendingLeaveRequestsCount (مفهرسة بالصفوف)"
                                            } else {
                                                "سجل الإجازات المفهرس حسب الصفوف والحالة"
                                            },
                                            icon = Icons.Filled.EventNote,
                                            selected = showIndexedLeaveRequestsModal,
                                            isDarkMode = isDarkMode,
                                            statusDotColor = if (pendingLeaveRequestsCount > 0) DangerRed else null,
                                            testTag = "drawer_leave_requests_modal_button",
                                            onClick = {
                                                coroutineScope.launch { drawerState.close() }
                                                viewModel.openIndexedLeaveRequestsModal()
                                            }
                                        )
                                    }
                                }

                                // المجموعة الثانية: المجتمع والتواصل
                                item(key = "drawer_group_community") {
                                    LuxuryDrawerGroupCard(
                                        groupTitle = "المجتمع والتواصل",
                                        isDarkMode = isDarkMode
                                    ) {
                                        communityGroupSections.forEach { (section, icon) ->
                                            LuxuryDrawerMenuItem(
                                                title = section.arabicTitle,
                                                icon = icon,
                                                selected = activeSection == section,
                                                isDarkMode = isDarkMode,
                                                testTag = "drawer_nav_${section.name}",
                                                onClick = {
                                                    viewModel.navigateToSection(section)
                                                    coroutineScope.launch { drawerState.close() }
                                                }
                                            )
                                        }
                                    }
                                }

                                // قسم محفظة وحانوت المدرسة الذكي (للمدير، المعلم المخول، ولي الأمر، والطالب)
                                if (canManageCanteen || currentUser?.role == UserRole.PARENT || currentUser?.role == UserRole.STUDENT) {
                                    item(key = "drawer_group_canteen_wallet") {
                                        LuxuryDrawerGroupCard(
                                            groupTitle = "محفظة وحانوت المدرسة",
                                            isDarkMode = isDarkMode
                                        ) {
                                            if (canManageCanteen) {
                                                LuxuryDrawerMenuItem(
                                                    title = "محفظة وحانوت المدرسة",
                                                    subtitle = "نظام كاشير الحانوت الذكية وشحن الرصيد",
                                                    icon = Icons.Filled.Storefront,
                                                    selected = showCanteenPosDialogFromDrawer,
                                                    isDarkMode = isDarkMode,
                                                    testTag = "drawer_canteen_pos_wallet_button",
                                                    onClick = {
                                                        coroutineScope.launch { drawerState.close() }
                                                        showCanteenPosDialogFromDrawer = true
                                                    }
                                                )
                                            } else {
                                                LuxuryDrawerMenuItem(
                                                    title = "محفظتي والحانوت",
                                                    subtitle = "محفظة الطالب الذكية وسجل الحركات والحد اليومي",
                                                    icon = Icons.Filled.AccountBalanceWallet,
                                                    selected = showStudentWalletDialogFromDrawer,
                                                    isDarkMode = isDarkMode,
                                                    testTag = "drawer_student_canteen_wallet_button",
                                                    onClick = {
                                                        coroutineScope.launch { drawerState.close() }
                                                        showStudentWalletDialogFromDrawer = true
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // المجموعة الثالثة: أدوات الإدارة السريعة (للمدير)
                                if (currentUser?.role == UserRole.ADMIN) {
                                    item(key = "drawer_group_admin_tools") {
                                        val hasActiveUrgent = schoolConfig.urgentAlert.isActive &&
                                            schoolConfig.urgentAlert.message.isNotBlank()
                                        LuxuryDrawerGroupCard(
                                            groupTitle = "أدوات الإدارة السريعة",
                                            isDarkMode = isDarkMode
                                        ) {
                                            LuxuryDrawerMenuItem(
                                                title = "بث تنبيه عاجل",
                                                subtitle = if (hasActiveUrgent) "تنبيه عاجل مفعّل حالياً" else "نشر أو إيقاف تنبيه للمدرسة",
                                                icon = Icons.Filled.Campaign,
                                                selected = hasActiveUrgent,
                                                isDarkMode = isDarkMode,
                                                statusDotColor = if (hasActiveUrgent) DangerRed else null,
                                                testTag = "drawer_urgent_school_alert_button",
                                                onClick = {
                                                    coroutineScope.launch { drawerState.close() }
                                                    viewModel.openUrgentSchoolAlertModal()
                                                }
                                            )
                                            LuxuryDrawerMenuItem(
                                                title = "إدارة العطل الرسمية",
                                                subtitle = "العطل الأسبوعية والرسمية وتوثيق السجل",
                                                icon = Icons.Filled.EventAvailable,
                                                selected = false,
                                                isDarkMode = isDarkMode,
                                                testTag = "drawer_holidays_management_button",
                                                onClick = {
                                                    coroutineScope.launch { drawerState.close() }
                                                    viewModel.openHolidaysManagementModal()
                                                }
                                            )
                                            LuxuryDrawerMenuItem(
                                                title = AppSection.ADMIN_ACCOUNTS.arabicTitle,
                                                subtitle = "إدارة الأساتذة والطلبة وأولياء الأمور",
                                                icon = Icons.Filled.ManageAccounts,
                                                selected = activeSection == AppSection.ADMIN_ACCOUNTS,
                                                isDarkMode = isDarkMode,
                                                testTag = "drawer_nav_${AppSection.ADMIN_ACCOUNTS.name}",
                                                onClick = {
                                                    viewModel.navigateToSection(AppSection.ADMIN_ACCOUNTS)
                                                    coroutineScope.launch { drawerState.close() }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // د) تذييل القائمة الجانبية (Sticky Drawer Footer)
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HorizontalDivider(color = headerBorderColor)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val isSettingsSelected = activeSection == AppSection.SETTINGS
                                    // زر الإعدادات الكبسولي
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                viewModel.navigateToSection(AppSection.SETTINGS)
                                                coroutineScope.launch { drawerState.close() }
                                            }
                                            .testTag("drawer_nav_${AppSection.SETTINGS.name}"),
                                        shape = RoundedCornerShape(50),
                                        color = if (isSettingsSelected) {
                                            if (isDarkMode) Color(0xFF065F46).copy(alpha = 0.40f) else Color(0xFFECFDF5)
                                        } else {
                                            headerCardBg
                                        },
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (isSettingsSelected) Color(0xFF059669).copy(alpha = 0.45f)
                                            else headerBorderColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Settings,
                                                contentDescription = "الإعدادات",
                                                tint = if (isSettingsSelected) Color(0xFF059669) else secondaryTextColor,
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "الإعدادات",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSettingsSelected) {
                                                    if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
                                                } else {
                                                    primaryTextColor
                                                },
                                                maxLines = 1
                                            )
                                        }
                                    }

                                    // زر تسجيل الخروج الكبسولي
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(42.dp)
                                            .clip(RoundedCornerShape(50))
                                            .clickable {
                                                coroutineScope.launch { drawerState.close() }
                                                viewModel.logout()
                                            }
                                            .testTag("drawer_logout_btn"),
                                        shape = RoundedCornerShape(50),
                                        color = if (isDarkMode) Color(0xFF7F1D1D).copy(alpha = 0.30f) else Color(0xFFFEF2F2),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            Color(0xFFEF4444).copy(alpha = 0.35f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.Logout,
                                                contentDescription = "تسجيل الخروج",
                                                tint = Color(0xFFDC2626),
                                                modifier = Modifier.size(17.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "تسجيل الخروج",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isDarkMode) Color(0xFFFCA5A5) else Color(0xFFB91C1C),
                                                maxLines = 1
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
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .statusBarsPadding()
                ) {
                    // Main Header Bar (Compact Action Icons on Left + Wide User Name & Role Section on Right)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp)
                        ) {
                            if (isSideDrawerEnabled) {
                                Surface(
                                    onClick = {
                                        coroutineScope.launch {
                                            if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                        }
                                    },
                                    shape = CircleShape,
                                    color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFECFDF5),
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("top_hamburger_menu_button")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Filled.Menu,
                                            contentDescription = "القائمة الجانبية السريعة",
                                            tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            Base64AvatarImage(
                                base64Data = currentUser?.photoBase64?.ifBlank { schoolConfig.logoBase64 }
                                    ?: schoolConfig.logoBase64,
                                fallbackIcon = Icons.Filled.Person,
                                contentDescription = currentUser?.fullName ?: schoolConfig.schoolName,
                                size = 38.dp
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                Text(
                                    text = currentUser?.fullName?.ifBlank { schoolConfig.schoolName }
                                        ?: schoolConfig.schoolName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = schoolConfig.schoolName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Notification Bell with Numeric Badge (32.dp compact circular button, 18.dp icon)
                            Surface(
                                onClick = {
                                    showNotificationsSheet = true
                                    viewModel.markAllNotificationsRead()
                                },
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("top_notifications_bell")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    BadgedBox(
                                        badge = {
                                            if (unreadCount > 0) {
                                                Badge(
                                                    containerColor = DangerRed,
                                                    contentColor = Color.White
                                                ) {
                                                    Text(
                                                        text = "$unreadCount",
                                                        fontSize = 8.sp,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            }
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Notifications,
                                            contentDescription = "مركز الإشعارات الفورية",
                                            tint = if (isDarkMode) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }

                            // Dark / Light Mode Toggle (32.dp compact circular button, 18.dp icon)
                            Surface(
                                onClick = { viewModel.toggleDarkMode() },
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("top_theme_toggle")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                        contentDescription = "الوضع الليلي والنهاري",
                                        tint = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Settings Shortcut (32.dp compact circular button, 18.dp icon)
                            Surface(
                                onClick = { viewModel.navigateToSection(AppSection.SETTINGS) },
                                shape = CircleShape,
                                color = if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                border = androidx.compose.foundation.BorderStroke(
                                    0.8.dp,
                                    if (isDarkMode) Color(0xFF334155) else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("top_settings_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.Settings,
                                        contentDescription = "الإعدادات",
                                        tint = if (isDarkMode) Color(0xFFE2E8F0) else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Compact Top Bar Logout Button (horizontal = 8.dp, vertical = 4.dp, fontSize = 11.sp)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = DangerRed.copy(alpha = if (isDarkMode) 0.22f else 0.14f),
                                modifier = Modifier
                                    .clickable { viewModel.logout() }
                                    .testTag("top_logout_button")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = "تسجيل الخروج",
                                        tint = if (isDarkMode) Color(0xFFFCA5A5) else DangerRed,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "خروج",
                                        color = if (isDarkMode) Color(0xFFFCA5A5) else DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Scrollable Section Pills Strip for instant access to all active sections (with Dark Mode Contrast & Live Radar Dot)
                    val pulseTransition = rememberInfiniteTransition(label = "live_radar_pill_pulse")
                    val liveDotAlpha by pulseTransition.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(750, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "live_dot_alpha"
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allSections, key = { it.first.name }) { (section, icon) ->
                            val selected = activeSection == section
                            val pillBg = when {
                                selected -> Color(0xFF059669)
                                isDarkMode -> Color(0xFF1E293B)
                                else -> Color(0xFFF1F5F9)
                            }
                            val pillBorder = when {
                                selected -> Color(0xFF059669)
                                isDarkMode -> Color(0xFF334155)
                                else -> Color(0xFFE2E8F0)
                            }
                            val pillContentColor = when {
                                selected -> Color.White
                                isDarkMode -> Color(0xFFE2E8F0)
                                else -> Color(0xFF1E293B)
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = pillBg,
                                border = androidx.compose.foundation.BorderStroke(1.dp, pillBorder),
                                modifier = Modifier
                                    .clickable { viewModel.navigateToSection(section) }
                                    .testTag("nav_pill_${section.name}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = section.arabicTitle,
                                        tint = pillContentColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = section.arabicTitle,
                                        color = pillContentColor,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                }
            },
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { (section, icon) ->
                        val selected = activeSection == section
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.navigateToSection(section) },
                            icon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = section.arabicTitle
                                )
                            },
                            label = {
                                Text(
                                    text = section.arabicTitle,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier.testTag("bottom_nav_${section.name}")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Admin 14-day Subscription Expiry Warning & Developer Broadcasts Banner (STRICTLY ADMIN ONLY)
                if (currentUser?.role == UserRole.ADMIN) {
                    AdminSubscriptionAndBroadcastBanner(
                        schoolConfig = schoolConfig,
                        developerBroadcasts = developerBroadcasts,
                        dismissedBroadcastIds = dismissedBroadcastIds,
                        onDismissBroadcast = { bcId ->
                            viewModel.dismissDeveloperBroadcastForAdmin(bcId)
                        }
                    )
                }

                // Compact Calm Single-Row Top Bar for Parent & Student Accounts
                if (currentUser?.role == UserRole.PARENT && parentChildren.isNotEmpty()) {
                    ParentMultiChildSwitcherBar(
                        children = parentChildren,
                        selectedStudent = selectedParentChild,
                        onSelectStudent = { stuId -> viewModel.selectParentChild(stuId) },
                        onOpenStudentIdCard = { stu -> idCardDialogStudent = stu },
                        isParentRole = true,
                        showDigitalIdButton = featureSettings.qrAttendanceEnabled
                    )
                } else if (currentUser?.role == UserRole.STUDENT && loggedInStudentRecord != null) {
                    ParentMultiChildSwitcherBar(
                        children = listOf(loggedInStudentRecord),
                        selectedStudent = loggedInStudentRecord,
                        onSelectStudent = {},
                        onOpenStudentIdCard = { stu -> idCardDialogStudent = stu },
                        isParentRole = false,
                        showDigitalIdButton = featureSettings.qrAttendanceEnabled
                    )
                }

                // Instant Alert Banner when a new notification arrives
                AnimatedVisibility(visible = latestBannerAlert != null) {
                    val alert = latestBannerAlert
                    if (alert != null) {
                        val cleanAlertTitle = com.example.util.InputSanitizer.stripEmojis(alert.title)
                        val cleanAlertBody = com.example.util.InputSanitizer.stripEmojis(alert.body)
                        val isSecretSuggestion = cleanAlertTitle.contains("صندوق المقترحات") || cleanAlertTitle.contains("سرية")
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clickable {
                                    viewModel.dismissBannerAlert()
                                    viewModel.openNotificationDeepLink(alert)
                                },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (isSecretSuggestion) Icons.Filled.Lock else Icons.Filled.NotificationsActive,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = cleanAlertTitle,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = Color.White
                                        )
                                        Text(
                                            text = cleanAlertBody,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.9f),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = { viewModel.dismissBannerAlert() },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Active Section Screen Content
                Box(modifier = Modifier.weight(1f)) {
                    when (activeSection) {
                        AppSection.DASHBOARD -> DashboardScreen(
                            viewModel = viewModel,
                            onOpenNotificationsDialog = {
                                showNotificationsSheet = true
                                viewModel.markAllNotificationsRead()
                            }
                        )
                        AppSection.LIVE_OPERATIONS_RADAR -> LiveOperationsRadarScreen(viewModel = viewModel)
                        AppSection.ATTENDANCE -> DailyAttendanceScreen(viewModel = viewModel)
                        AppSection.STUDENT_COMPREHENSIVE_ARCHIVE -> StudentComprehensiveArchiveScreen(viewModel = viewModel)
                        AppSection.SCHEDULE_HOMEWORK -> ScheduleAndHomeworkScreen(viewModel = viewModel)
                        AppSection.EXAMS_GRADES -> ExamsAndGradesScreen(viewModel = viewModel)
                        AppSection.STUDENT_ACTIVITIES -> StudentActivitiesScreen(viewModel = viewModel)
                        AppSection.HONOR_ROLL -> HonorRollScreen(viewModel = viewModel)
                        AppSection.TUITION_FEES -> TuitionFeesScreen(viewModel = viewModel)
                        AppSection.REPORTS_CHAT -> ReportsAndChatScreen(viewModel = viewModel)
                        AppSection.EVENTS_NEWS -> EventsAndNewsScreen(viewModel = viewModel)
                        AppSection.ADMIN_ACCOUNTS -> AdminAccountsScreen(viewModel = viewModel)
                        AppSection.SETTINGS -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // Indexed Electronic Leave Requests Modal Dialog (launched from Side Drawer or Dashboard)
    if (showIndexedLeaveRequestsModal) {
        IndexedLeaveRequestsModalDialog(
            viewModel = viewModel,
            onDismiss = { viewModel.dismissIndexedLeaveRequestsModal() }
        )
    }

    // Principal's Urgent School Alert Modal Dialog (launched from Side Drawer)
    if (showUrgentAlertModal && currentUser?.role == UserRole.ADMIN) {
        UrgentSchoolAlertModalDialog(
            currentAlert = schoolConfig.urgentAlert,
            onDismiss = { viewModel.closeUrgentSchoolAlertModal() },
            onPublishOrUpdate = { msg -> viewModel.publishOrUpdateUrgentSchoolAlert(msg) },
            onStopOrDelete = { viewModel.deleteOrStopUrgentSchoolAlert() }
        )
    }

    // Principal's Weekly & Official Holidays Management Modal Dialog
    if (showHolidaysModal && currentUser?.role == UserRole.ADMIN) {
        SchoolHolidaysManagementModalDialog(
            holidaysConfig = holidaysConfig,
            onToggleWeeklyDay = { day -> viewModel.toggleWeeklyHolidayDay(day) },
            onSaveOfficialHoliday = { id, dateIso, title ->
                viewModel.addOrUpdateOfficialHoliday(id, dateIso, title)
            },
            onDeleteOfficialHoliday = { id -> viewModel.deleteOfficialHoliday(id) },
            onDismiss = { viewModel.closeHolidaysManagementModal() },
            toastAlert = toastAlert,
            onDismissToast = { viewModel.dismissToastAlert() }
        )
    }

    // Full Notification History Dialog (سجل الإشعارات الفورية الكامل)
    if (showNotificationsSheet) {
        AlertDialog(
            onDismissRequest = { showNotificationsSheet = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("سجل الإشعارات الفورية الكامل")
                }
            },
            text = {
                val allDistinctNotifications = remember(notifications) {
                    notifications.distinctBy { it.id }
                }
                var notificationsVisibleCount by remember(allDistinctNotifications.size) {
                    mutableStateOf(6)
                }
                val displayedNotifications = remember(allDistinctNotifications, notificationsVisibleCount) {
                    allDistinctNotifications.take(notificationsVisibleCount)
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 420.dp)
                ) {
                    items(displayedNotifications, key = { it.id }) { notif ->
                        val cleanTitle = com.example.util.InputSanitizer.stripEmojis(notif.title)
                        val cleanBody = com.example.util.InputSanitizer.stripEmojis(notif.body)
                        val isSecretSuggestion = cleanTitle.contains("صندوق المقترحات") || cleanTitle.contains("سرية")
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .clickable {
                                    showNotificationsSheet = false
                                    viewModel.openNotificationDeepLink(notif)
                                }
                                .testTag("sheet_notif_item_${notif.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSecretSuggestion) {
                                        Icons.Filled.Lock
                                    } else {
                                        Icons.Filled.Notifications
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = cleanTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = notif.createdAt,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = cleanBody,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteNotification(notif.id) },
                                    modifier = Modifier
                                        .size(26.dp)
                                        .testTag("delete_sheet_notif_${notif.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "حذف الإشعار",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (displayedNotifications.size < allDistinctNotifications.size) {
                        item {
                            ProgressiveInfiniteScrollFooter(
                                visibleCount = displayedNotifications.size,
                                totalCount = allDistinctNotifications.size,
                                step = 6,
                                itemLabel = "إشعار",
                                onLoadMore = {
                                    notificationsVisibleCount = (notificationsVisibleCount + 6).coerceAtMost(allDistinctNotifications.size)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                VectorActionButton(
                    text = "إغلاق السجل",
                    icon = Icons.Filled.Check,
                    onClick = { showNotificationsSheet = false }
                )
            }
        )
    }

    idCardDialogStudent?.let { targetStu ->
        val stuBadges = remember(targetStu.id, allStudents) {
            viewModel.getStudentAutoBadges(targetStu.id)
        }
        val todayIsoStr: String = remember { com.example.ui.components.ArabicCalendarDateHelper.todayIsoDate() }
        val todayStatusLabel = viewModel.getStudentStatusForDate(targetStu, todayIsoStr).arabicLabel
        DigitalStudentIdDialog(
            student = targetStu,
            schoolConfig = schoolConfig,
            badges = stuBadges,
            todayAttendanceLabel = todayStatusLabel,
            todayIso = todayIsoStr,
            toastAlert = toastAlert,
            onDismissToast = { viewModel.dismissToastAlert() },
            onSavePng = {
                val res = DocumentAndIdExporter.saveStudentIdCardAsHighResPng(
                    context = context,
                    student = targetStu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم حفظ الهوية المدرسية بدقة عالية (PNG) في المعرض جاهزة للطباعة أو الكبس الحراري",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر حفظ الصورة: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onExportPdf = {
                val res = DocumentAndIdExporter.exportStudentIdCardPdf(
                    context = context,
                    student = targetStu,
                    schoolConfig = schoolConfig,
                    badges = stuBadges
                )
                if (res.isSuccess) {
                    viewModel.showToastAlert(
                        "تم تصدير وفتح نافذة طباعة الهوية المدرسية بصيغة PDF بنجاح",
                        isSuccess = true
                    )
                } else {
                    viewModel.showToastAlert("تعذر تصدير PDF: ${res.exceptionOrNull()?.message}", isSuccess = false)
                }
            },
            onDismiss = { idCardDialogStudent = null }
        )
    }

    if (showCanteenPosDialogFromDrawer && canManageCanteen) {
        val todayIsoStr = remember { com.example.ui.components.ArabicCalendarDateHelper.todayIsoDate() }
        val activeSchoolStudents = remember(allStudents, schoolConfig.schoolId) {
            allStudents.filter { it.schoolId.isEmpty() || it.schoolId == schoolConfig.schoolId }
        }
        SmartCanteenPosAndWalletDialog(
            students = activeSchoolStudents,
            todayIso = todayIsoStr,
            isCanteenWalletEnabled = schoolConfig.isCanteenWalletEnabled,
            isAdminRole = currentUser?.role == UserRole.ADMIN,
            onToggleSchoolCanteenWallet = { enabled ->
                viewModel.setSchoolCanteenWalletEnabled(enabled)
            },
            onToggleStudentWalletSuspended = { stuId, suspended ->
                viewModel.setStudentWalletSuspended(stuId, suspended)
            },
            toastAlert = toastAlert,
            onDismissToast = { viewModel.dismissToastAlert() },
            onTopUpWallet = { stuId, amountIqd, note, onComplete ->
                viewModel.topUpStudentWallet(stuId, amountIqd, note, onComplete)
            },
            onProcessPurchase = { stuId, amountIqd, itemSummary, onComplete ->
                viewModel.processCanteenPurchase(stuId, amountIqd, itemSummary) { ok, msg, _ ->
                    onComplete(ok, msg)
                }
            },
            onFindStudentByQr = { rawQr ->
                viewModel.findStudentByWalletOrQrCode(rawQr)
            },
            onDismiss = { showCanteenPosDialogFromDrawer = false }
        )
    }

    if (showStudentWalletDialogFromDrawer) {
        val walletTargetStudent = if (currentUser?.role == UserRole.PARENT) {
            selectedParentChild
        } else {
            loggedInStudentRecord
        }
        if (walletTargetStudent != null) {
            val todayIsoStr = remember { com.example.ui.components.ArabicCalendarDateHelper.todayIsoDate() }
            ParentStudentCanteenWalletDialog(
                student = walletTargetStudent,
                isParentRole = currentUser?.role == UserRole.PARENT,
                todayIso = todayIsoStr,
                isSchoolWalletEnabled = schoolConfig.isCanteenWalletEnabled,
                toastAlert = toastAlert,
                onDismissToast = { viewModel.dismissToastAlert() },
                onUpdateDailyLimit = { newLimit, onComplete ->
                    viewModel.updateStudentDailySpendLimit(walletTargetStudent.id, newLimit)
                    onComplete(true, "تم حفظ السقف اليومي بنجاح")
                },
                onOpenStudentIdCard = {
                    idCardDialogStudent = walletTargetStudent
                },
                onDismiss = { showStudentWalletDialogFromDrawer = false }
            )
        } else {
            LaunchedEffect(Unit) {
                showStudentWalletDialogFromDrawer = false
                viewModel.showToastAlert("يرجى اختيار أو ربط ملف الطالب لعرض محفظة الحانوت", isSuccess = false)
            }
        }
    }
}

@Composable
private fun LuxuryDrawerGroupCard(
    groupTitle: String,
    isDarkMode: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = groupTitle,
            style = MaterialTheme.typography.labelSmall,
            color = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B),
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
        content()
    }
}

@Composable
private fun LuxuryDrawerMenuItem(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    testTag: String,
    subtitle: String? = null,
    isDarkMode: Boolean = false,
    statusDotColor: Color? = null,
    onClick: () -> Unit
) {
    val activeBg = if (isDarkMode) Color(0xFF065F46).copy(alpha = 0.35f) else Color(0xFFECFDF5)
    val activeTextColor = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46)
    val inactiveTextColor = if (isDarkMode) Color(0xFFE2E8F0) else Color(0xFF1E293B)
    val subtitleColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)
    val activeIconColor = if (isDarkMode) Color(0xFF34D399) else Color(0xFF059669)
    val inactiveIconColor = if (isDarkMode) Color(0xFF94A3B8) else Color(0xFF475569)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) activeBg else Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (selected) {
                    if (isDarkMode) Color(0xFF059669).copy(alpha = 0.25f) else Color(0xFFD1FAE5)
                } else {
                    if (isDarkMode) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                },
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = if (selected) activeIconColor else inactiveIconColor,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    color = if (selected) activeTextColor else inactiveTextColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = subtitleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (statusDotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusDotColor)
                )
            } else if (selected) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF059669))
                )
            }
        }
    }
}


