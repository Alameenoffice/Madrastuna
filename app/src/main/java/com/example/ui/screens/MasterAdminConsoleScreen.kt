package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import com.example.util.ExternalIntents
import com.example.viewmodel.SchoolViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

// Executive Dark Navy & Amber Gold Palette
private val ExecDeepNavy = Color(0xFF0A1128)
private val ExecCardNavy = Color(0xFF111D3B)
private val ExecElevatedNavy = Color(0xFF17264A)
private val ExecBorderNavy = Color(0xFF233868)
private val ExecAmberGold = Color(0xFFF59E0B)
private val ExecAmberLight = Color(0xFFFBBF24)
private val ExecSkyBlue = Color(0xFF38BDF8)
private val ExecEmerald = Color(0xFF10B981)
private val ExecCrimson = Color(0xFFEF4444)
private val ExecTextPrimary = Color(0xFFF8FAFC)
private val ExecTextSecondary = Color(0xFFCBD5E1)
private val ExecTextMuted = Color(0xFF94A3B8)

enum class MasterConsoleTab(
    val index: Int,
    val arabicTitle: String,
    val shortBadge: String
) {
    SCHOOLS_AND_CODES(0, "وحدة إدارة المدارس وأكواد التفعيل", "المدارس والتفعيل"),
    ANALYTICS_INSPECTOR(1, "وحدة الرقابة الشاملة والإحصائيات الحية", "الرقابة والإحصائيات"),
    CURRICULUM_HUB(2, "وحدة إدارة وتحديث روابط المناهج المركزية", "روابط المناهج"),
    SEASONAL_IDENTITY(3, "وحدة الهوية الموسمية وأيقونات المناسبات", "الهوية والمناسبات"),
    BROADCASTS_AND_CLOUD(4, "وحدة التعميمات المركزية وحالة السحابة", "التعميمات والسحابة")
}

@Composable
fun MasterSystemAdminRootApp(
    viewModel: SchoolViewModel
) {
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsState()
    val toastAlert by viewModel.toastAlert.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.refreshDeveloperCloudData()
        viewModel.refreshEcosystemLiveAnalytics(syncFromCloudFirst = true)
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ExecDeepNavy)
        ) {
            AnimatedContent(
                targetState = isDeveloperAuthenticated,
                transitionSpec = {
                    fadeIn(tween(320)) togetherWith fadeOut(tween(240))
                },
                label = "master_admin_auth_switch"
            ) { authenticated: Boolean ->
                if (!authenticated) {
                    MasterDeveloperAuthGateScreen(viewModel = viewModel)
                } else {
                    MasterSystemAdminConsoleScreen(viewModel = viewModel)
                }
            }

            // Executive Floating Toast Banner
            AnimatedVisibility(
                visible = toastAlert != null,
                enter = fadeIn(tween(220)) + expandVertically(),
                exit = fadeOut(tween(220)) + shrinkVertically(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                toastAlert?.let { alert: ToastAlertMessage ->
                    val accent = if (alert.isSuccess) ExecEmerald else ExecCrimson
                    Surface(
                        color = ExecCardNavy,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.5.dp, accent.copy(alpha = 0.85f)),
                        shadowElevation = 14.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Canvas(modifier = Modifier.size(22.dp)) {
                                val r = size.minDimension * 0.44f
                                drawCircle(
                                    color = accent.copy(alpha = 0.22f),
                                    radius = r
                                )
                                drawCircle(
                                    color = accent,
                                    radius = r,
                                    style = Stroke(width = 2.dp.toPx())
                                )
                                if (alert.isSuccess) {
                                    val check = Path().apply {
                                        moveTo(size.width * 0.28f, size.height * 0.52f)
                                        lineTo(size.width * 0.45f, size.height * 0.68f)
                                        lineTo(size.width * 0.74f, size.height * 0.34f)
                                    }
                                    drawPath(
                                        path = check,
                                        color = accent,
                                        style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                    )
                                } else {
                                    drawLine(
                                        color = accent,
                                        start = Offset(size.width * 0.34f, size.height * 0.34f),
                                        end = Offset(size.width * 0.66f, size.height * 0.66f),
                                        strokeWidth = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                    drawLine(
                                        color = accent,
                                        start = Offset(size.width * 0.66f, size.height * 0.34f),
                                        end = Offset(size.width * 0.34f, size.height * 0.66f),
                                        strokeWidth = 2.2.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            Text(
                                text = alert.message,
                                color = ExecTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 19.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 1. MASTER DEVELOPER AUTH GATE SCREEN (بوابة المطور الرئيسي)
// ============================================================================
@Composable
fun MasterDeveloperAuthGateScreen(
    viewModel: SchoolViewModel
) {
    var secretCodeInput by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }
    val registeredSchools by viewModel.registeredSchools.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "exec_gate_anim")
    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.78f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        ExecDeepNavy,
                        Color(0xFF0E1836),
                        ExecDeepNavy
                    )
                )
            )
            .systemBarsPadding()
    ) {
        // Subtle Ambient Architectural Grid & Glow Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ExecAmberGold.copy(alpha = 0.10f * pulseGlow),
                        ExecSkyBlue.copy(alpha = 0.04f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.5f, h * 0.22f),
                    radius = w * 0.72f
                ),
                center = Offset(w * 0.5f, h * 0.22f),
                radius = w * 0.72f
            )
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            contentPadding = PaddingValues(vertical = 28.dp)
        ) {
            item {
                // Bespoke Executive Shield & Orbital Crest (Canvas Vector)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(118.dp)
                        .padding(bottom = 14.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val outerR = size.minDimension * 0.46f

                        // Outer rotating dashed/segmented executive ring
                        rotate(degrees = orbitAngle, pivot = Offset(cx, cy)) {
                            for (i in 0 until 12) {
                                val startAngle = i * 30f
                                drawArc(
                                    color = if (i % 2 == 0) ExecAmberGold.copy(alpha = 0.75f) else ExecSkyBlue.copy(alpha = 0.45f),
                                    startAngle = startAngle,
                                    sweepAngle = 16f,
                                    useCenter = false,
                                    topLeft = Offset(cx - outerR, cy - outerR),
                                    size = Size(outerR * 2f, outerR * 2f),
                                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                                )
                            }
                        }

                        // Inner glowing medallion
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    ExecElevatedNavy,
                                    ExecCardNavy
                                ),
                                center = Offset(cx, cy),
                                radius = outerR * 0.80f
                            ),
                            radius = outerR * 0.80f,
                            center = Offset(cx, cy)
                        )
                        drawCircle(
                            color = ExecAmberGold.copy(alpha = 0.85f),
                            radius = outerR * 0.80f,
                            center = Offset(cx, cy),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Executive Sovereign Shield Path inside medallion
                        val sW = outerR * 0.78f
                        val sH = outerR * 0.88f
                        val topY = cy - sH * 0.48f
                        val botY = cy + sH * 0.52f
                        val shieldPath = Path().apply {
                            moveTo(cx, topY)
                            lineTo(cx + sW * 0.48f, topY + sH * 0.18f)
                            lineTo(cx + sW * 0.40f, cy + sH * 0.14f)
                            quadraticTo(cx + sW * 0.22f, botY - sH * 0.08f, cx, botY)
                            quadraticTo(cx - sW * 0.22f, botY - sH * 0.08f, cx - sW * 0.40f, cy + sH * 0.14f)
                            lineTo(cx - sW * 0.48f, topY + sH * 0.18f)
                            close()
                        }
                        drawPath(
                            path = shieldPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    ExecAmberGold.copy(alpha = 0.28f),
                                    ExecSkyBlue.copy(alpha = 0.14f)
                                )
                            )
                        )
                        drawPath(
                            path = shieldPath,
                            color = ExecAmberLight,
                            style = Stroke(width = 2.4.dp.toPx(), join = StrokeJoin.Round)
                        )

                        // Central Command Keyhole / Core Node inside Shield
                        drawCircle(
                            color = ExecAmberLight,
                            radius = 4.5.dp.toPx(),
                            center = Offset(cx, cy - 3.dp.toPx())
                        )
                        drawLine(
                            color = ExecAmberLight,
                            start = Offset(cx, cy + 1.dp.toPx()),
                            end = Offset(cx, cy + 11.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Official Executive Badge
                Surface(
                    color = ExecAmberGold.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, ExecAmberGold.copy(alpha = 0.55f))
                ) {
                    Text(
                        text = "MASTER SYSTEM ADMIN CONSOLE",
                        color = ExecAmberLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "إدارة نظام مدرستي",
                    color = ExecTextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "بوابة المطور الرئيسي والتحكم السحابي المركزي بجميع المدارس",
                    color = ExecTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(22.dp))

                // Main Executive Auth Card
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.2.dp, ExecBorderNavy),
                    shadowElevation = 18.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "رمز الدخول السري للمطور",
                                color = ExecTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Surface(
                                color = if (isCloudConnected) ExecEmerald.copy(alpha = 0.16f) else ExecAmberGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, if (isCloudConnected) ExecEmerald.copy(alpha = 0.5f) else ExecAmberGold.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (isCloudConnected) ExecEmerald else ExecAmberGold)
                                    )
                                    Text(
                                        text = "متصل بالسحابة (${registeredSchools.size} مدرسة)",
                                        color = if (isCloudConnected) ExecEmerald else ExecAmberLight,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        OutlinedTextField(
                            value = secretCodeInput,
                            onValueChange = {
                                secretCodeInput = it
                                if (errorText.isNotEmpty()) errorText = ""
                            },
                            label = {
                                Text(
                                    text = "أدخل الكود السري الخاص بالمطور (مثال: DEV-2026)",
                                    fontSize = 13.sp
                                )
                            },
                            placeholder = {
                                Text(
                                    text = "DEV-2026",
                                    color = ExecTextMuted
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    val ok = viewModel.verifyMasterDeveloperAccessCode(secretCodeInput)
                                    if (!ok) {
                                        errorText = "رمز المطور غير صحيح — الرمز القياسي هو DEV-2026"
                                    }
                                }
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = ExecTextPrimary,
                                unfocusedTextColor = ExecTextPrimary,
                                focusedBorderColor = ExecAmberGold,
                                unfocusedBorderColor = ExecBorderNavy,
                                focusedLabelColor = ExecAmberLight,
                                unfocusedLabelColor = ExecTextSecondary,
                                cursorColor = ExecAmberGold,
                                focusedContainerColor = ExecDeepNavy.copy(alpha = 0.65f),
                                unfocusedContainerColor = ExecDeepNavy.copy(alpha = 0.45f)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("master_developer_code_input")
                        )

                        if (errorText.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = errorText,
                                color = ExecCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                val ok = viewModel.verifyMasterDeveloperAccessCode(secretCodeInput)
                                if (!ok) {
                                    errorText = "رمز المطور غير صحيح — الرمز القياسي هو DEV-2026"
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExecAmberGold,
                                contentColor = ExecDeepNavy
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("master_developer_unlock_button")
                        ) {
                            Text(
                                text = "فتح غرفة القيادة والتحكم المركزي",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// 2. MASTER SYSTEM ADMIN CONSOLE MAIN SCREEN (5 CLOUD CONTROL MODULES)
// ============================================================================
@Composable
fun MasterSystemAdminConsoleScreen(
    viewModel: SchoolViewModel
) {
    var selectedTab by remember { mutableStateOf(MasterConsoleTab.SCHOOLS_AND_CODES) }
    val registeredSchools by viewModel.registeredSchools.collectAsState()
    val ecosystemAnalytics by viewModel.ecosystemAnalytics.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isCloudSyncInProgress by viewModel.isCloudSyncInProgress.collectAsState()

    Scaffold(
        containerColor = ExecDeepNavy,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            MasterConsoleExecutiveHeader(
                selectedTab = selectedTab,
                schoolsCount = registeredSchools.size,
                studentsCount = ecosystemAnalytics.totalActiveStudentsCount,
                isCloudConnected = isCloudConnected,
                isSyncing = isCloudSyncInProgress,
                onQuickSync = { viewModel.performComprehensiveMasterCloudSync() },
                onLockConsole = { viewModel.lockMasterDeveloperConsole() }
            )
        },
        bottomBar = {
            MasterConsoleBottomNavigationBar(
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    fadeIn(tween(220)) togetherWith fadeOut(tween(180))
                },
                label = "master_console_tab_content"
            ) { tab: MasterConsoleTab ->
                when (tab) {
                    MasterConsoleTab.SCHOOLS_AND_CODES ->
                        SchoolsAndActivationGateModule(viewModel = viewModel)
                    MasterConsoleTab.ANALYTICS_INSPECTOR ->
                        LiveEcosystemAnalyticsAndInspectorModule(viewModel = viewModel)
                    MasterConsoleTab.CURRICULUM_HUB ->
                        CentralCurriculumLinksHubModule(viewModel = viewModel)
                    MasterConsoleTab.SEASONAL_IDENTITY ->
                        RemoteSeasonalThemeControllerModule(viewModel = viewModel)
                    MasterConsoleTab.BROADCASTS_AND_CLOUD ->
                        GlobalBroadcastsAndCloudSyncModule(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun MasterConsoleExecutiveHeader(
    selectedTab: MasterConsoleTab,
    schoolsCount: Int,
    studentsCount: Int,
    isCloudConnected: Boolean,
    isSyncing: Boolean,
    onQuickSync: () -> Unit,
    onLockConsole: () -> Unit
) {
    Surface(
        color = ExecCardNavy,
        border = BorderStroke(1.dp, ExecBorderNavy),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                    // Executive Crest Mini Icon
                    Canvas(modifier = Modifier.size(36.dp)) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = size.minDimension * 0.45f
                        drawCircle(
                            color = ExecAmberGold.copy(alpha = 0.16f),
                            radius = r
                        )
                        drawCircle(
                            color = ExecAmberGold,
                            radius = r,
                            style = Stroke(width = 1.8.dp.toPx())
                        )
                        val shield = Path().apply {
                            moveTo(cx, cy - r * 0.55f)
                            lineTo(cx + r * 0.48f, cy - r * 0.25f)
                            lineTo(cx + r * 0.36f, cy + r * 0.25f)
                            lineTo(cx, cy + r * 0.60f)
                            lineTo(cx - r * 0.36f, cy + r * 0.25f)
                            lineTo(cx - r * 0.48f, cy - r * 0.25f)
                            close()
                        }
                        drawPath(
                            path = shield,
                            color = ExecAmberLight,
                            style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
                        )
                    }

                    Column {
                        Text(
                            text = "إدارة نظام مدرستي • لوحة المطور",
                            color = ExecTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = selectedTab.arabicTitle,
                            color = ExecAmberLight,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Instant Cloud Sync Button
                    Surface(
                        color = ExecSkyBlue.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExecSkyBlue.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(enabled = !isSyncing) { onQuickSync() }
                            .testTag("header_quick_cloud_sync_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isCloudConnected) ExecEmerald else ExecAmberGold)
                            )
                            Text(
                                text = if (isSyncing) "جارٍ المزامنة..." else "مزامنة",
                                color = ExecSkyBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Lock Console Button
                    Surface(
                        color = ExecCrimson.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, ExecCrimson.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onLockConsole() }
                            .testTag("header_lock_console_button")
                    ) {
                        Text(
                            text = "قفل",
                            color = Color(0xFFFCA5A5),
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterConsoleBottomNavigationBar(
    selectedTab: MasterConsoleTab,
    onSelectTab: (MasterConsoleTab) -> Unit
) {
    Surface(
        color = ExecCardNavy,
        border = BorderStroke(1.dp, ExecBorderNavy),
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            MasterConsoleTab.entries.forEach { tab ->
                val isSelected = tab == selectedTab
                val bg = if (isSelected) ExecAmberGold.copy(alpha = 0.18f) else Color.Transparent
                val borderCol = if (isSelected) ExecAmberGold.copy(alpha = 0.75f) else Color.Transparent
                val textCol = if (isSelected) ExecAmberLight else ExecTextSecondary

                Surface(
                    color = bg,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderCol),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 2.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectTab(tab) }
                        .testTag("master_tab_${tab.name.lowercase()}")
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        MasterTabVectorIcon(
                            tab = tab,
                            tint = if (isSelected) ExecAmberGold else ExecTextMuted,
                            size = 20.dp
                        )
                        Text(
                            text = tab.shortBadge,
                            color = textCol,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MasterTabVectorIcon(
    tab: MasterConsoleTab,
    tint: Color,
    size: Dp
) {
    Canvas(modifier = Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tab) {
            MasterConsoleTab.SCHOOLS_AND_CODES -> {
                // Architectural School Gate & Key Vector
                val roof = Path().apply {
                    moveTo(w * 0.12f, h * 0.42f)
                    lineTo(w * 0.50f, h * 0.14f)
                    lineTo(w * 0.88f, h * 0.42f)
                }
                drawPath(roof, color = tint, style = stroke)
                drawRoundRect(
                    color = tint,
                    topLeft = Offset(w * 0.22f, h * 0.42f),
                    size = Size(w * 0.56f, h * 0.44f),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.58f),
                    end = Offset(w * 0.50f, h * 0.86f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            MasterConsoleTab.ANALYTICS_INSPECTOR -> {
                // Radar Ring & Analytics Bars Vector
                drawCircle(
                    color = tint,
                    radius = w * 0.40f,
                    center = Offset(w * 0.5f, h * 0.5f),
                    style = stroke
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.34f, h * 0.65f),
                    end = Offset(w * 0.34f, h * 0.48f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.65f),
                    end = Offset(w * 0.50f, h * 0.34f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = tint,
                    start = Offset(w * 0.66f, h * 0.65f),
                    end = Offset(w * 0.66f, h * 0.42f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            MasterConsoleTab.CURRICULUM_HUB -> {
                // Open Book Vector
                val book = Path().apply {
                    moveTo(w * 0.50f, h * 0.26f)
                    quadraticTo(w * 0.30f, h * 0.20f, w * 0.14f, h * 0.28f)
                    lineTo(w * 0.14f, h * 0.76f)
                    quadraticTo(w * 0.30f, h * 0.68f, w * 0.50f, h * 0.76f)
                    quadraticTo(w * 0.70f, h * 0.68f, w * 0.86f, h * 0.76f)
                    lineTo(w * 0.86f, h * 0.28f)
                    quadraticTo(w * 0.70f, h * 0.20f, w * 0.50f, h * 0.26f)
                }
                drawPath(book, color = tint, style = stroke)
                drawLine(
                    color = tint,
                    start = Offset(w * 0.50f, h * 0.26f),
                    end = Offset(w * 0.50f, h * 0.76f),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            MasterConsoleTab.SEASONAL_IDENTITY -> {
                // Crescent & Star / Crest Vector
                drawArc(
                    color = tint,
                    startAngle = 35f,
                    sweepAngle = 260f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.18f),
                    size = Size(w * 0.64f, h * 0.64f),
                    style = stroke
                )
                drawCircle(
                    color = tint,
                    radius = 2.4.dp.toPx(),
                    center = Offset(w * 0.68f, h * 0.34f)
                )
            }
            MasterConsoleTab.BROADCASTS_AND_CLOUD -> {
                // Broadcast Waves & Cloud Signal Vector
                drawCircle(
                    color = tint,
                    radius = 2.6.dp.toPx(),
                    center = Offset(w * 0.50f, h * 0.62f)
                )
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.28f, h * 0.38f),
                    size = Size(w * 0.44f, h * 0.44f),
                    style = stroke
                )
                drawArc(
                    color = tint,
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.72f, h * 0.72f),
                    style = stroke
                )
            }
        }
    }
}

// ============================================================================
// TAB 1: SCHOOLS & ACTIVATION GATE MANAGER (وحدة إدارة المدارس وأكواد التفعيل السحابية)
// ============================================================================
@Composable
private fun SchoolsAndActivationGateModule(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val registeredSchools by viewModel.registeredSchools.collectAsState()

    var showCreateSchoolCard by remember { mutableStateOf(true) }
    var newSchoolName by remember { mutableStateOf("") }
    var selectedStageLabel by remember { mutableStateOf("ابتدائية") }
    var selectedGenderLabel by remember { mutableStateOf("بنين") }
    var newPrincipalName by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("123456") }
    var newActivationCode by remember {
        mutableStateOf(viewModel.generateNextAvailableSchoolActivationCode())
    }
    var activateImmediately by remember { mutableStateOf(true) }

    var expandedSchoolId by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var schoolPendingDelete by remember { mutableStateOf<RegisteredSchoolSummary?>(null) }

    val filteredSchools = remember(registeredSchools, searchQuery) {
        val q = searchQuery.trim()
        if (q.isEmpty()) {
            registeredSchools
        } else {
            registeredSchools.filter {
                it.schoolName.contains(q, ignoreCase = true) ||
                    it.schoolId.contains(q, ignoreCase = true) ||
                    it.schoolActivationCode.contains(q, ignoreCase = true) ||
                    it.schoolStage.contains(q, ignoreCase = true)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Create New School & Generate Official Activation Code Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecAmberGold.copy(alpha = 0.55f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreateSchoolCard = !showCreateSchoolCard },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "إنشاء مدرسة جديدة وتوليد كود تفعيلها الرسمي",
                                color = ExecAmberLight,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "تأسيس سحابي فوري للمدرسة وحساب المدير وكود البوابة",
                                color = ExecTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            color = ExecAmberGold.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ExecAmberGold.copy(alpha = 0.5f))
                        ) {
                            Text(
                                text = if (showCreateSchoolCard) "طي النموذج" else "فتح النموذج",
                                color = ExecAmberLight,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    AnimatedVisibility(visible = showCreateSchoolCard) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // School Name
                            ExecutiveOutlinedField(
                                value = newSchoolName,
                                onValueChange = { newSchoolName = it },
                                label = "اسم المدرسة الرسمية (مثال: مدرسة الفراهيدي النموذجية)",
                                testTag = "create_school_name_input"
                            )

                            // Stage Selector (ابتدائية / متوسطة / إعدادية / ثانوية)
                            Text(
                                text = "المرحلة الدراسية:",
                                color = ExecTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("ابتدائية", "متوسطة", "إعدادية", "ثانوية").forEach { stageOpt ->
                                    val selected = selectedStageLabel == stageOpt
                                    Surface(
                                        color = if (selected) ExecAmberGold else ExecElevatedNavy,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) ExecAmberLight else ExecBorderNavy
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedStageLabel = stageOpt }
                                    ) {
                                        Text(
                                            text = stageOpt,
                                            color = if (selected) ExecDeepNavy else ExecTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            // Gender Type Selector (بنين / بنات / مختلط)
                            Text(
                                text = "نوع المدرسة:",
                                color = ExecTextSecondary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("بنين", "بنات", "مختلط").forEach { genderOpt ->
                                    val selected = selectedGenderLabel == genderOpt
                                    Surface(
                                        color = if (selected) ExecSkyBlue else ExecElevatedNavy,
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) ExecSkyBlue else ExecBorderNavy
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable { selectedGenderLabel = genderOpt }
                                    ) {
                                        Text(
                                            text = genderOpt,
                                            color = if (selected) ExecDeepNavy else ExecTextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.padding(vertical = 9.dp)
                                        )
                                    }
                                }
                            }

                            // Principal Name & Initial Password
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveOutlinedField(
                                        value = newPrincipalName,
                                        onValueChange = { newPrincipalName = it },
                                        label = "اسم المدير الكامل",
                                        testTag = "create_school_principal_input"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveOutlinedField(
                                        value = newAdminPassword,
                                        onValueChange = { newAdminPassword = it },
                                        label = "كلمة المرور الابتدائية للمدير",
                                        testTag = "create_school_admin_pass_input"
                                    )
                                }
                            }

                            // Activation Code Field + Auto-Generate Button
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveOutlinedField(
                                        value = newActivationCode,
                                        onValueChange = { newActivationCode = it },
                                        label = "كود تفعيل المدرسة الرسمي (مثال: 1001)",
                                        testTag = "create_school_activation_code_input"
                                    )
                                }
                                Button(
                                    onClick = {
                                        newActivationCode = viewModel.generateNextAvailableSchoolActivationCode()
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = ExecElevatedNavy,
                                        contentColor = ExecAmberLight
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, ExecAmberGold.copy(alpha = 0.6f)),
                                    modifier = Modifier.height(54.dp)
                                ) {
                                    Text(
                                        text = "توليد تلقائي",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Option to activate gate immediately or require principal first-time activation
                            Surface(
                                color = ExecElevatedNavy.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, ExecBorderNavy),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { activateImmediately = !activateImmediately }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (activateImmediately) {
                                                "حالة البوابة الابتدائية: مفعلة رسمياً عن بُعد"
                                            } else {
                                                "حالة البوابة الابتدائية: بانتظار تفعيل المدير بالكود"
                                            },
                                            color = if (activateImmediately) ExecEmerald else ExecAmberLight,
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "يمكنك تبديل حالة البوابة أو قفلها عن بُعد في أي لحظة",
                                            color = ExecTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                    Switch(
                                        checked = activateImmediately,
                                        onCheckedChange = { activateImmediately = it },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = ExecDeepNavy,
                                            checkedTrackColor = ExecEmerald,
                                            uncheckedThumbColor = ExecTextSecondary,
                                            uncheckedTrackColor = ExecDeepNavy
                                        )
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    viewModel.createSchoolFromMasterConsole(
                                        schoolName = newSchoolName,
                                        stageLabel = selectedStageLabel,
                                        genderLabel = selectedGenderLabel,
                                        principalName = newPrincipalName,
                                        initialAdminPassword = newAdminPassword,
                                        activationCodeInput = newActivationCode,
                                        officiallyActivateImmediately = activateImmediately
                                    ) { createdSummary, _, _ ->
                                        newSchoolName = ""
                                        newPrincipalName = ""
                                        newActivationCode = viewModel.generateNextAvailableSchoolActivationCode()
                                        expandedSchoolId = createdSummary.schoolId
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExecAmberGold,
                                    contentColor = ExecDeepNavy
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("create_school_submit_button")
                            ) {
                                Text(
                                    text = "حفظ وتأسيس المدرسة ومزامنة كود التفعيل في السحابة",
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Search & Filter Bar for Registered Schools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "سجل المدارس المسجلة في السحابة (${filteredSchools.size})",
                    color = ExecTextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "اضغط على أي مدرسة للتحكم الفوري عن بُعد",
                    color = ExecSkyBlue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            ExecutiveOutlinedField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = "ابحث باسم المدرسة أو كود التفعيل (مثال: 1001)...",
                testTag = "schools_search_input"
            )
        }

        if (filteredSchools.isEmpty()) {
            item {
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ExecBorderNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "لا توجد مدارس مطابقة للبحث حالياً",
                            color = ExecTextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "استخدم نموذج الإنشاء أعلاه لتأسيس مدرسة جديدة وتوليد كود تفعيلها في السحابة",
                            color = ExecTextMuted,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(filteredSchools, key = { it.schoolId }) { school: RegisteredSchoolSummary ->
                val isExpanded = expandedSchoolId == school.schoolId
                SchoolRemoteControlCard(
                    school = school,
                    isExpanded = isExpanded,
                    plainAdminPass = viewModel.getPlainAdminPasswordForSchool(school.schoolId),
                    onToggleExpand = {
                        expandedSchoolId = if (isExpanded) null else school.schoolId
                    },
                    onActivateGateRemotely = {
                        viewModel.setSchoolOfficialGateStatusRemotely(school.schoolId, true)
                    },
                    onRelockGateRemotely = {
                        viewModel.setSchoolOfficialGateStatusRemotely(school.schoolId, false)
                    },
                    onToggleFreezeKillSwitch = {
                        viewModel.setSchoolFreezeKillSwitchRemotely(school.schoolId, !school.isActive)
                    },
                    onUpdateCredentials = { newCode, newPass ->
                        viewModel.updateSchoolCredentialsRemotely(school.schoolId, newCode, newPass)
                    },
                    onCopyActivationCard = {
                        val cardText = viewModel.buildSchoolActivationShareCardText(school)
                        copyToClipboard(context, "بطاقة تفعيل ${school.schoolName}", cardText)
                        viewModel.showBannerMessage("تم نسخ بطاقة التفعيل الكاملة لمدرسة (${school.schoolName}) إلى الحافظة")
                    },
                    onShareWhatsApp = {
                        val cardText = viewModel.buildSchoolActivationShareCardText(school)
                        ExternalIntents.sendWhatsAppMessage(context, "", cardText)
                    },
                    onRequestDelete = {
                        schoolPendingDelete = school
                    }
                )
            }
        }
    }

    // Confirm Complete School Deletion Dialog
    schoolPendingDelete?.let { targetSchool ->
        AlertDialog(
            onDismissRequest = { schoolPendingDelete = null },
            containerColor = ExecCardNavy,
            titleContentColor = ExecTextPrimary,
            textContentColor = ExecTextSecondary,
            title = {
                Text(
                    text = "تأكيد الحذف الجذري للمدرسة من السحابة",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
            },
            text = {
                Text(
                    text = "هل أنت متأكد من حذف مدرسة (${targetSchool.schoolName} - كود ${targetSchool.schoolActivationCode.ifBlank { targetSchool.schoolId }}) وجميع حساباتها وطلابها وسجلاتها نهائياً من قاعدة البيانات السحابية؟",
                    fontSize = 13.5.sp,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSchoolCompletelyFromCloud(targetSchool.schoolId)
                        schoolPendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ExecCrimson,
                        contentColor = Color.White
                    )
                ) {
                    Text("نعم، حذف نهائي من السحابة", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { schoolPendingDelete = null }) {
                    Text("إلغاء", color = ExecTextSecondary)
                }
            }
        )
    }
}

@Composable
private fun SchoolRemoteControlCard(
    school: RegisteredSchoolSummary,
    isExpanded: Boolean,
    plainAdminPass: String,
    onToggleExpand: () -> Unit,
    onActivateGateRemotely: () -> Unit,
    onRelockGateRemotely: () -> Unit,
    onToggleFreezeKillSwitch: () -> Unit,
    onUpdateCredentials: (String, String) -> Unit,
    onCopyActivationCard: () -> Unit,
    onShareWhatsApp: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val statusLabel: String
    val statusColor: Color
    when {
        !school.isActive -> {
            statusLabel = "موقوفة من المطور"
            statusColor = ExecCrimson
        }
        school.isSchoolOfficiallyActivated -> {
            statusLabel = "مفعلة رسمياً"
            statusColor = ExecEmerald
        }
        else -> {
            statusLabel = "بانتظار تفعيل المدير"
            statusColor = ExecAmberGold
        }
    }

    val effectiveCode = school.schoolActivationCode.ifBlank { school.schoolId }
    val effectiveAdminUser = school.adminUsername.ifBlank {
        FirebaseConfigConstants.applySchoolPrefix(school.schoolId, "admin")
    }

    var editActivationCode by remember(school.schoolId, school.schoolActivationCode) {
        mutableStateOf(effectiveCode)
    }
    var editAdminPassword by remember(school.schoolId, plainAdminPass) {
        mutableStateOf(plainAdminPass)
    }

    Surface(
        color = ExecCardNavy,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(
            width = if (isExpanded) 1.5.dp else 1.dp,
            color = if (isExpanded) ExecSkyBlue else statusColor.copy(alpha = 0.45f)
        ),
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("school_card_${school.schoolId}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Top Row: School Identity + Live Status Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = school.schoolName,
                        color = ExecTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${school.schoolStage} • ${school.schoolGenderType} • كود التفعيل: $effectiveCode",
                        color = ExecSkyBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.16f),
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.65f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = statusLabel,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Summary Pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = ExecElevatedNavy,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ExecBorderNavy)
                ) {
                    Text(
                        text = "حساب المدير: $effectiveAdminUser",
                        color = ExecTextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Surface(
                    color = ExecElevatedNavy,
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, ExecBorderNavy)
                ) {
                    Text(
                        text = "كلمة سر المدير: $plainAdminPass",
                        color = ExecAmberLight,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (isExpanded) "إخفاء التحكم ▲" else "التحكم عن بُعد ▼",
                    color = ExecSkyBlue,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Remote Kill-Switch & Activation Panel
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    HorizontalDivider(color = ExecBorderNavy)

                    Text(
                        text = "لوحة التحكم الفوري بالمدرسة عن بُعد (Remote Kill-Switch & Gate Panel)",
                        color = ExecAmberLight,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    // 1 & 2. Remote Official Gate Activation / Re-lock Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onActivateGateRemotely,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExecEmerald,
                                contentColor = ExecDeepNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "تفعيل بوابة المدرسة رسمياً عن بُعد",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center
                            )
                        }

                        OutlinedButton(
                            onClick = onRelockGateRemotely,
                            border = BorderStroke(1.2.dp, ExecAmberGold),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExecAmberLight),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                        ) {
                            Text(
                                text = "إعادة قفل البوابة لمطالبة المدير",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // 3. Remote Freeze / Kill-Switch Button
                    Button(
                        onClick = onToggleFreezeKillSwitch,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (school.isActive) ExecCrimson else ExecSkyBlue,
                            contentColor = if (school.isActive) Color.White else ExecDeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Text(
                            text = if (school.isActive) {
                                "إيقاف / تجميد المدرسة الفوري عن بُعد (Remote Kill-Switch)"
                            } else {
                                "إلغاء التجميد وإعادة تشغيل المدرسة فوراً في السحابة"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // 4. Edit School Activation Code or Principal Password in Cloud
                    Surface(
                        color = ExecElevatedNavy.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ExecBorderNavy),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "تعديل كود التفعيل أو كلمة سر المدير في السحابة:",
                                color = ExecTextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveOutlinedField(
                                        value = editActivationCode,
                                        onValueChange = { editActivationCode = it },
                                        label = "كود التفعيل الرسمي"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    ExecutiveOutlinedField(
                                        value = editAdminPassword,
                                        onValueChange = { editAdminPassword = it },
                                        label = "كلمة سر المدير"
                                    )
                                }
                            }
                            Button(
                                onClick = {
                                    onUpdateCredentials(editActivationCode, editAdminPassword)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExecSkyBlue,
                                    contentColor = ExecDeepNavy
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Text(
                                    text = "حفظ ومزامنة الكود وكلمة سر المدير في السحابة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }

                    // 5. Copy Activation Card & Share / Delete Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onCopyActivationCard,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExecAmberGold,
                                contentColor = ExecDeepNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1.3f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "نسخ بطاقة التفعيل للمدير",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        OutlinedButton(
                            onClick = onShareWhatsApp,
                            border = BorderStroke(1.dp, ExecEmerald),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExecEmerald),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(0.9f)
                                .height(44.dp)
                        ) {
                            Text(
                                text = "إرسال واتساب",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        OutlinedButton(
                            onClick = onRequestDelete,
                            border = BorderStroke(1.dp, ExecCrimson.copy(alpha = 0.7f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFCA5A5)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text(
                                text = "حذف",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 2: LIVE ECOSYSTEM ANALYTICS & INSPECTOR (وحدة الرقابة الشاملة والإحصائيات الحية)
// ============================================================================
@Composable
private fun LiveEcosystemAnalyticsAndInspectorModule(
    viewModel: SchoolViewModel
) {
    val registeredSchools by viewModel.registeredSchools.collectAsState()
    val analytics by viewModel.ecosystemAnalytics.collectAsState()
    val inspectedData by viewModel.inspectedSchoolData.collectAsState()
    val isSyncingInspector by viewModel.isSyncingInspector.collectAsState()

    var selectedInspectorSubTab by remember { mutableIntStateOf(0) } // 0: المعلمين, 1: الطلاب, 2: الشعب والحضور

    LaunchedEffect(Unit) {
        viewModel.refreshEcosystemLiveAnalytics(syncFromCloudFirst = true)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Animated Canvas Progress Rings Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecBorderNavy),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "مؤشرات المنظومة السحابية الحية",
                                color = ExecTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "إحصائيات فورية شاملة لكافة المدارس والطلاب والكوادر",
                                color = ExecTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Surface(
                            color = ExecSkyBlue.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, ExecSkyBlue.copy(alpha = 0.5f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { viewModel.refreshEcosystemLiveAnalytics(syncFromCloudFirst = true) }
                        ) {
                            Text(
                                text = "تحديث الأرقام",
                                color = ExecSkyBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AnimatedExecutiveGaugeRing(
                            label = "إجمالي المدارس",
                            valueText = analytics.totalSchoolsCount.toString(),
                            subtitle = "${analytics.officiallyActivatedSchoolsCount} مفعلة",
                            progress = if (analytics.totalSchoolsCount > 0) {
                                (analytics.officiallyActivatedSchoolsCount.toFloat() / analytics.totalSchoolsCount.toFloat()).coerceIn(0.15f, 1f)
                            } else 0.75f,
                            ringColor = ExecAmberGold
                        )
                        AnimatedExecutiveGaugeRing(
                            label = "إجمالي الطلاب الفعليين",
                            valueText = analytics.totalActiveStudentsCount.toString(),
                            subtitle = "حضور ${analytics.overallAttendanceRatePercent}%",
                            progress = (analytics.overallAttendanceRatePercent / 100f).coerceIn(0.2f, 1f),
                            ringColor = ExecSkyBlue
                        )
                        AnimatedExecutiveGaugeRing(
                            label = "الكادر التدريسي",
                            valueText = analytics.totalTeachingStaffCount.toString(),
                            subtitle = "معلم ومدرس",
                            progress = if (analytics.totalTeachingStaffCount > 0) 0.88f else 0.35f,
                            ringColor = ExecEmerald
                        )
                    }
                }
            }
        }

        // 2. Interactive School Selector for Deep Live Inspection
        item {
            Text(
                text = "تصفح وفحص سجلات أي مدرسة مسجلة مباشرة:",
                color = ExecAmberLight,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (registeredSchools.isEmpty()) {
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ExecBorderNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "لا توجد مدارس مسجلة حالياً لفحصها — قم بإضافة مدرسة من التبويب الأول.",
                        color = ExecTextSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(registeredSchools, key = { it.schoolId }) { school ->
                        val isSelected = inspectedSchoolIdMatches(inspectedData, school.schoolId)
                        Surface(
                            color = if (isSelected) ExecAmberGold else ExecCardNavy,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(
                                1.2.dp,
                                if (isSelected) ExecAmberLight else ExecBorderNavy
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable {
                                    viewModel.inspectSchoolLiveRecords(school.schoolId)
                                }
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = school.schoolName,
                                    color = if (isSelected) ExecDeepNavy else ExecTextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "كود: ${school.schoolActivationCode.ifBlank { school.schoolId }} • ${school.schoolStage}",
                                    color = if (isSelected) ExecDeepNavy.copy(alpha = 0.8f) else ExecSkyBlue,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Inspected School Live Records Card
        item {
            if (isSyncingInspector) {
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, ExecSkyBlue.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = ExecAmberGold,
                            strokeWidth = 2.5.dp,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "جارٍ جلب ومزامنة سجلات المدرسة الحية من السحابة...",
                            color = ExecTextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (inspectedData != null) {
                val data = inspectedData!!
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.2.dp, ExecSkyBlue.copy(alpha = 0.6f)),
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = data.school.schoolName,
                                    color = ExecTextPrimary,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = "المدير: ${data.config.principalFullName} (${data.adminUsername}) • كلمة السر: ${data.adminPasswordPlain}",
                                    color = ExecAmberLight,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Surface(
                                color = ExecEmerald.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, ExecEmerald.copy(alpha = 0.5f))
                            ) {
                                Text(
                                    text = "حضور ${data.attendanceRatePercent}%",
                                    color = ExecEmerald,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        // Sub-tabs: قائمة المعلمين | سجل الطلاب | الشعب والنشاط
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val subTabs = listOf(
                                0 to "قائمة المعلمين (${data.teachers.size})",
                                1 to "سجل الطلاب (${data.students.size})",
                                2 to "الشعب (${data.sections.size})"
                            )
                            subTabs.forEach { (idx, title) ->
                                val active = selectedInspectorSubTab == idx
                                Surface(
                                    color = if (active) ExecSkyBlue else ExecElevatedNavy,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (active) ExecSkyBlue else ExecBorderNavy),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable { selectedInspectorSubTab = idx }
                                ) {
                                    Text(
                                        text = title,
                                        color = if (active) ExecDeepNavy else ExecTextPrimary,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }

                        when (selectedInspectorSubTab) {
                            0 -> {
                                if (data.teachers.isEmpty()) {
                                    Text(
                                        text = "لم يتم تسجيل كادر تدريسي في هذه المدرسة بعد.",
                                        color = ExecTextMuted,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    data.teachers.forEach { teacher ->
                                        Surface(
                                            color = ExecElevatedNavy,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, ExecBorderNavy),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Text(
                                                    text = teacher.fullName,
                                                    color = ExecTextPrimary,
                                                    fontSize = 13.5.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "الاختصاص: ${teacher.specialty.ifBlank { "عام" }} • الصفوف: ${teacher.formattedAssignedClassesForDisplay}",
                                                    color = ExecSkyBlue,
                                                    fontSize = 11.5.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            1 -> {
                                if (data.students.isEmpty()) {
                                    Text(
                                        text = "لم يتم تسجيل طلاب في هذه المدرسة بعد.",
                                        color = ExecTextMuted,
                                        fontSize = 12.5.sp,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    data.students.take(40).forEach { stu ->
                                        Surface(
                                            color = ExecElevatedNavy,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, ExecBorderNavy),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = stu.fullNameWithFather,
                                                        color = ExecTextPrimary,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "${stu.gradeClass} - شعبة ${stu.sectionName} • حساب الطالب: ${stu.studentUsername}",
                                                        color = ExecTextSecondary,
                                                        fontSize = 11.5.sp
                                                    )
                                                }
                                                Text(
                                                    text = stu.enrollmentStatus.arabicLabel,
                                                    color = ExecEmerald,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "الصفوف الدراسية المسجلة: ${data.config.registeredClasses.joinToString(" ، ")}",
                                        color = ExecTextPrimary,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "الشعب الفعالة: ${data.sections.joinToString(" ، ") { "شعبة $it" }}",
                                        color = ExecAmberLight,
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "إجمالي سجلات الرصد اليومي للحضور: ${data.totalAttendanceRecords} سجل (نسبة الالتزام ${data.attendanceRatePercent}%)",
                                        color = ExecEmerald,
                                        fontSize = 12.5.sp,
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
}

private fun inspectedSchoolIdMatches(
    inspected: EcosystemSchoolInspectionData?,
    schoolId: String
): Boolean {
    return inspected != null && inspected.school.schoolId == schoolId
}

@Composable
private fun AnimatedExecutiveGaugeRing(
    label: String,
    valueText: String,
    subtitle: String,
    progress: Float,
    ringColor: Color
) {
    val animatedSweep by animateFloatAsState(
        targetValue = (progress.coerceIn(0f, 1f) * 300f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "gauge_sweep_$label"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(102.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(86.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeW = 7.dp.toPx()
                val pad = strokeW / 2f + 3.dp.toPx()
                val arcSize = Size(size.width - pad * 2f, size.height - pad * 2f)
                drawArc(
                    color = ExecBorderNavy,
                    startAngle = 120f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
                drawArc(
                    color = ringColor,
                    startAngle = 120f,
                    sweepAngle = animatedSweep,
                    useCenter = false,
                    topLeft = Offset(pad, pad),
                    size = arcSize,
                    style = Stroke(width = strokeW, cap = StrokeCap.Round)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = valueText,
                    color = ExecTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = ExecTextPrimary,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = ringColor,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

// ============================================================================
// TAB 3: CENTRAL CURRICULUM LINKS HUB (وحدة إدارة وتحديث روابط المناهج الدراسية المركزية)
// ============================================================================
@Composable
private fun CentralCurriculumLinksHubModule(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IraqiCurriculumCatalog.init(context)
    }

    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()
    var selectedStage by remember { mutableStateOf(CurriculumAcademicStage.PRIMARY) }
    var selectedGrade by remember(selectedStage) {
        mutableStateOf(selectedStage.gradeClasses.first())
    }

    var newBookTitle by remember { mutableStateOf("") }
    var newBookDriveUrl by remember { mutableStateOf("") }
    var editingBookId by remember { mutableStateOf<String?>(null) }
    var editingBookTitle by remember { mutableStateOf("") }
    var editingBookUrl by remember { mutableStateOf("") }

    val gradeBooks = remember(allBooks, selectedStage, selectedGrade) {
        allBooks.filter { it.stage == selectedStage && it.gradeClass == selectedGrade }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Stage & Grade Selector Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecBorderNavy),
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إدارة وتحديث روابط المناهج الدراسية المركزية",
                        color = ExecAmberLight,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "أي رابط Google Drive تضعه هنا يتحول تلقائياً إلى صيغة المعاينة المباشرة (/preview) ويتحدث فوراً لدى جميع المدارس.",
                        color = ExecTextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    // Stage Tabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CurriculumAcademicStage.entries.forEach { stage ->
                            val selected = stage == selectedStage
                            Surface(
                                color = if (selected) ExecAmberGold else ExecElevatedNavy,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (selected) ExecAmberLight else ExecBorderNavy),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        selectedStage = stage
                                        selectedGrade = stage.gradeClasses.first()
                                    }
                            ) {
                                Text(
                                    text = stage.shortBadge,
                                    color = if (selected) ExecDeepNavy else ExecTextPrimary,
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    // Grade Classes Horizontal Row
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(selectedStage.gradeClasses) { gradeCls ->
                            val isSelected = gradeCls == selectedGrade
                            Surface(
                                color = if (isSelected) ExecSkyBlue else ExecElevatedNavy,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (isSelected) ExecSkyBlue else ExecBorderNavy),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedGrade = gradeCls }
                            ) {
                                Text(
                                    text = gradeCls,
                                    color = if (isSelected) ExecDeepNavy else ExecTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Add New Book to Selected Grade Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ExecEmerald.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إضافة كتاب منهجي جديد لصف ($selectedGrade)",
                        color = ExecEmerald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    ExecutiveOutlinedField(
                        value = newBookTitle,
                        onValueChange = { newBookTitle = it },
                        label = "اسم الكتاب المنهجي (مثال: كتاب الرياضيات)",
                        testTag = "curriculum_new_book_title_input"
                    )

                    ExecutiveOutlinedField(
                        value = newBookDriveUrl,
                        onValueChange = { newBookDriveUrl = it },
                        label = "رابط Google Drive للكتاب (يُحوّل تلقائياً إلى /preview)",
                        testTag = "curriculum_new_book_url_input"
                    )

                    Button(
                        onClick = {
                            if (newBookTitle.isBlank() || newBookDriveUrl.isBlank()) {
                                viewModel.showBannerMessage("يرجى إدخال اسم الكتاب ورابط Google Drive أولاً")
                            } else {
                                val added = IraqiCurriculumCatalog.addBookToGrade(
                                    context = context,
                                    stage = selectedStage,
                                    gradeClass = selectedGrade,
                                    subjectTitle = newBookTitle,
                                    rawGoogleDriveUrl = newBookDriveUrl
                                )
                                newBookTitle = ""
                                newBookDriveUrl = ""
                                viewModel.showBannerMessage("تمت إضافة (${added.subjectTitle}) لصف ($selectedGrade) ومزامنته في السحابة فوراً")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecEmerald,
                            contentColor = ExecDeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("curriculum_add_book_button")
                    ) {
                        Text(
                            text = "إضافة الكتاب ومزامنة الرابط مع جميع المدارس",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // Current Books List for Selected Grade
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الكتب المعتمدة في ($selectedGrade) — العدد: ${gradeBooks.size}",
                    color = ExecTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                TextButton(
                    onClick = {
                        IraqiCurriculumCatalog.resetPrimaryToDefaults(context)
                        viewModel.showBannerMessage("تمت استعادة الروابط القياسية للمناهج ومزامنتها")
                    }
                ) {
                    Text(
                        text = "استعادة الافتراضي",
                        color = ExecAmberLight,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        items(gradeBooks, key = { it.id }) { book: CurriculumBookItem ->
            val isEditing = editingBookId == book.id
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isEditing) ExecAmberGold else ExecBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = book.subjectTitle,
                                color = ExecTextPrimary,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = book.previewUrl,
                                color = ExecSkyBlue,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = ExecAmberGold.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExecAmberGold.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (isEditing) {
                                            editingBookId = null
                                        } else {
                                            editingBookId = book.id
                                            editingBookTitle = book.subjectTitle
                                            editingBookUrl = book.previewUrl
                                        }
                                    }
                            ) {
                                Text(
                                    text = if (isEditing) "إغلاق" else "تعديل الرابط",
                                    color = ExecAmberLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }

                            Surface(
                                color = ExecCrimson.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExecCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        IraqiCurriculumCatalog.deleteBook(context, book.id)
                                        viewModel.showBannerMessage("تم حذف (${book.subjectTitle}) من السحابة")
                                    }
                            ) {
                                Text(
                                    text = "حذف",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isEditing) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ExecutiveOutlinedField(
                                value = editingBookTitle,
                                onValueChange = { editingBookTitle = it },
                                label = "عنوان الكتاب المنهجي"
                            )
                            ExecutiveOutlinedField(
                                value = editingBookUrl,
                                onValueChange = { editingBookUrl = it },
                                label = "رابط Google Drive الجديد"
                            )
                            Button(
                                onClick = {
                                    val formatted = IraqiCurriculumCatalog.updateBookLink(
                                        context = context,
                                        bookId = book.id,
                                        newRawUrl = editingBookUrl,
                                        newTitle = editingBookTitle
                                    )
                                    editingBookId = null
                                    viewModel.showBannerMessage("تم تحديث رابط الكتاب إلى ($formatted) ومزامنته فوراً")
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExecAmberGold,
                                    contentColor = ExecDeepNavy
                                ),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                            ) {
                                Text(
                                    text = "اعتماد الرابط الجديد ومزامنته مع السحابة",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// TAB 4: REMOTE SEASONAL THEME CONTROLLER (وحدة الهوية الموسمية وأيقونات المناسبات عن بُعد)
// ============================================================================
private data class SeasonalPresetCardModel(
    val presetKey: String,
    val arabicTitle: String,
    val subtitle: String,
    val accentColor: Color
)

@Composable
private fun RemoteSeasonalThemeControllerModule(
    viewModel: SchoolViewModel
) {
    val occasionConfig by viewModel.occasionThemeConfig.collectAsState()
    var customTitleInput by remember(occasionConfig.customTitle) {
        mutableStateOf(occasionConfig.customTitle)
    }
    var customGreetingInput by remember(occasionConfig.customBannerText) {
        mutableStateOf(occasionConfig.customBannerText)
    }

    val presets = remember {
        listOf(
            SeasonalPresetCardModel(
                presetKey = "OFFICIAL",
                arabicTitle = "الهوية الأكاديمية الرسمية",
                subtitle = "الثيم الزمردي الملكي الرسمي للمنصة",
                accentColor = ExecEmerald
            ),
            SeasonalPresetCardModel(
                presetKey = "RAMADAN",
                arabicTitle = "موسم شهر رمضان المبارك",
                subtitle = "هلال وفانوس رمضان الذهبي المضيء",
                accentColor = ExecAmberGold
            ),
            SeasonalPresetCardModel(
                presetKey = "EID",
                arabicTitle = "عيد الفطر والأضحى",
                subtitle = "إشراقة العيد السعيد وتهنئة الأسرة التربوية",
                accentColor = Color(0xFFFBBF24)
            ),
            SeasonalPresetCardModel(
                presetKey = "NEW_YEAR",
                arabicTitle = "رأس السنة الميلادية",
                subtitle = "احتفالية العام الجديد والتفوق العلمي",
                accentColor = ExecSkyBlue
            ),
            SeasonalPresetCardModel(
                presetKey = "MAWLID",
                arabicTitle = "المولد النبوي الشريف",
                subtitle = "ذكرى المولد النبوي الشريف العطرة",
                accentColor = Color(0xFF34D399)
            ),
            SeasonalPresetCardModel(
                presetKey = "NATIONAL_DAY",
                arabicTitle = "العيد الوطني ويوم العلم العراقي",
                subtitle = "احتفالية الوطن ورفعة العلم العراقي",
                accentColor = Color(0xFF2DD4BF)
            )
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "الهوية الموسمية وأيقونات المناسبات عن بُعد",
                                color = ExecAmberLight,
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "تبديل الثيم وشريط التهنئة في تطبيق المدرسة العام بضغطة زر",
                                color = ExecTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = occasionConfig.isActive,
                            onCheckedChange = { viewModel.toggleGlobalOccasionThemeActive(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ExecDeepNavy,
                                checkedTrackColor = ExecEmerald,
                                uncheckedThumbColor = ExecTextSecondary,
                                uncheckedTrackColor = ExecDeepNavy
                            )
                        )
                    }

                    Text(
                        text = if (occasionConfig.isActive) {
                            "الحالة الحالية في المدارس: نشط (${occasionConfig.effectiveTitle})"
                        } else {
                            "الحالة الحالية في المدارس: الهوية الأكاديمية الرسمية (شريط المناسبات مخفي)"
                        },
                        color = if (occasionConfig.isActive) ExecEmerald else ExecSkyBlue,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Grid of 6 Seasonal Preset Cards (2 per row)
        items(presets.chunked(2)) { rowPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowPair.forEach { preset ->
                    val isCurrentlySelected = (preset.presetKey == "OFFICIAL" && !occasionConfig.isActive) ||
                        (occasionConfig.isActive && occasionConfig.customTitle.contains(preset.arabicTitle.take(10)))

                    Surface(
                        color = ExecCardNavy,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            width = if (isCurrentlySelected) 1.8.dp else 1.dp,
                            color = if (isCurrentlySelected) preset.accentColor else ExecBorderNavy
                        ),
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable {
                                viewModel.applyMasterSeasonalPresetRemotely(preset.presetKey)
                            }
                            .testTag("seasonal_preset_${preset.presetKey.lowercase()}")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SeasonalPresetCanvasIllustration(
                                presetKey = preset.presetKey,
                                accentColor = preset.accentColor
                            )

                            Text(
                                text = preset.arabicTitle,
                                color = ExecTextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                                minLines = 2
                            )

                            Text(
                                text = preset.subtitle,
                                color = ExecTextSecondary,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                color = if (isCurrentlySelected) preset.accentColor else ExecElevatedNavy,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isCurrentlySelected) "مفعّل حالياً" else "تفعيل فوري",
                                    color = if (isCurrentlySelected) ExecDeepNavy else preset.accentColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Custom Title & Greeting Editor Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ExecBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "تخصيص عنوان وعبارة التهنئة المباشرة في أعلى شاشات المدارس:",
                        color = ExecTextPrimary,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    ExecutiveOutlinedField(
                        value = customTitleInput,
                        onValueChange = { customTitleInput = it },
                        label = "عنوان المناسبة (مثال: موسم شهر رمضان المبارك)"
                    )
                    ExecutiveOutlinedField(
                        value = customGreetingInput,
                        onValueChange = { customGreetingInput = it },
                        label = "رسالة التهنئة الظاهرة لجميع المدارس"
                    )
                    Button(
                        onClick = {
                            viewModel.updateGlobalOccasionTheme(
                                themeType = occasionConfig.themeType,
                                customTitle = customTitleInput,
                                customBannerText = customGreetingInput,
                                isActive = true
                            )
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecAmberGold,
                            contentColor = ExecDeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(
                            text = "نشر وتحديث الهوية الموسمية في السحابة فوراً",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SeasonalPresetCanvasIllustration(
    presetKey: String,
    accentColor: Color
) {
    Canvas(modifier = Modifier.size(54.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = size.minDimension * 0.44f

        drawCircle(
            color = accentColor.copy(alpha = 0.16f),
            radius = r,
            center = Offset(cx, cy)
        )
        drawCircle(
            color = accentColor.copy(alpha = 0.65f),
            radius = r,
            center = Offset(cx, cy),
            style = Stroke(width = 1.8.dp.toPx())
        )

        when (presetKey) {
            "OFFICIAL" -> {
                // Academic Shield & Crown Vector
                val shield = Path().apply {
                    moveTo(cx, cy - r * 0.52f)
                    lineTo(cx + r * 0.44f, cy - r * 0.24f)
                    lineTo(cx + r * 0.34f, cy + r * 0.24f)
                    lineTo(cx, cy + r * 0.56f)
                    lineTo(cx - r * 0.34f, cy + r * 0.24f)
                    lineTo(cx - r * 0.44f, cy - r * 0.24f)
                    close()
                }
                drawPath(shield, color = accentColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
            }
            "RAMADAN", "MAWLID" -> {
                // Crescent Moon & Lantern Star Vector
                drawArc(
                    color = accentColor,
                    startAngle = 30f,
                    sweepAngle = 265f,
                    useCenter = false,
                    topLeft = Offset(cx - r * 0.48f, cy - r * 0.48f),
                    size = Size(r * 0.96f, r * 0.96f),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )
                drawCircle(
                    color = accentColor,
                    radius = 3.dp.toPx(),
                    center = Offset(cx + r * 0.22f, cy - r * 0.18f)
                )
            }
            "EID" -> {
                // 8-Ray Celebration Starburst Vector
                for (i in 0 until 8) {
                    val angleRad = (i * 45.0) * PI / 180.0
                    val x1 = cx + (r * 0.18f * cos(angleRad)).toFloat()
                    val y1 = cy + (r * 0.18f * sin(angleRad)).toFloat()
                    val x2 = cx + (r * 0.52f * cos(angleRad)).toFloat()
                    val y2 = cy + (r * 0.52f * sin(angleRad)).toFloat()
                    drawLine(
                        color = accentColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
            "NEW_YEAR" -> {
                // Graduation Cap / New Year Beacon Vector
                val diamond = Path().apply {
                    moveTo(cx, cy - r * 0.45f)
                    lineTo(cx + r * 0.52f, cy - r * 0.10f)
                    lineTo(cx, cy + r * 0.22f)
                    lineTo(cx - r * 0.52f, cy - r * 0.10f)
                    close()
                }
                drawPath(diamond, color = accentColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
                drawLine(
                    color = accentColor,
                    start = Offset(cx + r * 0.42f, cy - r * 0.04f),
                    end = Offset(cx + r * 0.42f, cy + r * 0.42f),
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            "NATIONAL_DAY" -> {
                // Flag Banner & Pole Vector
                drawLine(
                    color = accentColor,
                    start = Offset(cx - r * 0.36f, cy - r * 0.50f),
                    end = Offset(cx - r * 0.36f, cy + r * 0.52f),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )
                val flag = Path().apply {
                    moveTo(cx - r * 0.36f, cy - r * 0.46f)
                    lineTo(cx + r * 0.44f, cy - r * 0.32f)
                    lineTo(cx + r * 0.32f, cy + r * 0.08f)
                    lineTo(cx - r * 0.36f, cy + r * 0.02f)
                    close()
                }
                drawPath(flag, color = accentColor, style = Stroke(width = 2.dp.toPx(), join = StrokeJoin.Round))
            }
        }
    }
}

// ============================================================================
// TAB 5: GLOBAL SYSTEM BROADCASTS & CLOUD SYNC (وحدة التعميمات المركزية وحالة السحابة)
// ============================================================================
@Composable
private fun GlobalBroadcastsAndCloudSyncModule(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val broadcasts by viewModel.developerBroadcasts.collectAsState()
    val isCloudConnected by viewModel.isCloudConnected.collectAsState()
    val isSyncing by viewModel.isCloudSyncInProgress.collectAsState()
    val lastSyncStamp by viewModel.lastCloudSyncTimestamp.collectAsState()
    val currentSecretCode by viewModel.masterDeveloperSecretCode.collectAsState()

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var targetScope by remember { mutableStateOf("ALL_PRINCIPALS") } // ALL_PRINCIPALS | ALL_USERS

    var newSecretCodeInput by remember(currentSecretCode) { mutableStateOf(currentSecretCode) }
    var backupJsonPreview by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Live Firebase Cloud Connection Status & Instant Comprehensive Sync Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecSkyBlue.copy(alpha = 0.65f)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حالة الربط الحي بقاعدة بيانات السحابة المركزية",
                                color = ExecTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = FirebaseConfigConstants.DATABASE_URL,
                                color = ExecSkyBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Surface(
                            color = if (isCloudConnected) ExecEmerald.copy(alpha = 0.16f) else ExecCrimson.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, if (isCloudConnected) ExecEmerald else ExecCrimson)
                        ) {
                            Text(
                                text = if (isCloudConnected) "متصل نشط" else "غير متصل",
                                color = if (isCloudConnected) ExecEmerald else ExecCrimson,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Text(
                        text = "آخر مزامنة شاملة: $lastSyncStamp",
                        color = ExecTextSecondary,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = { viewModel.performComprehensiveMasterCloudSync() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecSkyBlue,
                            contentColor = ExecDeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("comprehensive_cloud_sync_button")
                    ) {
                        Text(
                            text = if (isSyncing) {
                                "جارٍ تنفيذ المزامنة الفورية الشاملة مع السحابة..."
                            } else {
                                "مزامنة فورية شاملة لجميع المدارس والمناهج والتعميمات"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 2. Send Global Administrative Broadcast Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, ExecAmberGold.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "إرسال تعميم إداري مركزي فوري",
                        color = ExecAmberLight,
                        fontSize = 15.5.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val scopes = listOf(
                            "ALL_PRINCIPALS" to "توجيه لمدراء المدارس",
                            "ALL_USERS" to "توجيه لكافة المستخدمين"
                        )
                        scopes.forEach { (key, label) ->
                            val selected = targetScope == key
                            Surface(
                                color = if (selected) ExecAmberGold else ExecElevatedNavy,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, if (selected) ExecAmberLight else ExecBorderNavy),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { targetScope = key }
                            ) {
                                Text(
                                    text = label,
                                    color = if (selected) ExecDeepNavy else ExecTextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 9.dp)
                                )
                            }
                        }
                    }

                    ExecutiveOutlinedField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = "عنوان التعميم الإداري المركزي",
                        testTag = "broadcast_title_input"
                    )

                    ExecutiveOutlinedField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        label = "نص التعميم والتوجيه الرسمي",
                        singleLine = false,
                        testTag = "broadcast_message_input"
                    )

                    Button(
                        onClick = {
                            viewModel.sendGlobalSystemBroadcastFromConsole(
                                title = broadcastTitle,
                                message = broadcastMessage,
                                targetScope = targetScope
                            )
                            if (broadcastTitle.isNotBlank() && broadcastMessage.isNotBlank()) {
                                broadcastTitle = ""
                                broadcastMessage = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ExecAmberGold,
                            contentColor = ExecDeepNavy
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("send_global_broadcast_button")
                    ) {
                        Text(
                            text = "بث التعميم المركزي فوراً عبر السحابة",
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }

        // 3. Active Broadcasts List
        if (broadcasts.isNotEmpty()) {
            item {
                Text(
                    text = "التعميمات المركزية المنشورة حالياً (${broadcasts.size})",
                    color = ExecTextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            items(broadcasts, key = { it.id }) { bc: DeveloperBroadcastItem ->
                Surface(
                    color = ExecCardNavy,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ExecBorderNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = bc.title,
                                color = ExecAmberLight,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.weight(1f)
                            )
                            Surface(
                                color = ExecCrimson.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ExecCrimson.copy(alpha = 0.5f)),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { viewModel.deleteDeveloperBroadcast(bc.id) }
                            ) {
                                Text(
                                    text = "إيقاف وحذف",
                                    color = Color(0xFFFCA5A5),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Text(
                            text = bc.message,
                            color = ExecTextPrimary,
                            fontSize = 12.5.sp,
                            lineHeight = 19.sp
                        )
                        Text(
                            text = "تاريخ النشر: ${bc.createdAt}",
                            color = ExecTextMuted,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // 4. Master Developer Secret Access Code & System Backup Card
        item {
            Surface(
                color = ExecCardNavy,
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, ExecBorderNavy),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إعدادات أمان بوابة المطور والنسخ الاحتياطي الشامل",
                        color = ExecTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ExecutiveOutlinedField(
                                value = newSecretCodeInput,
                                onValueChange = { newSecretCodeInput = it },
                                label = "رمز الدخول السري لبوابة المطور"
                            )
                        }
                        Button(
                            onClick = {
                                viewModel.updateMasterDeveloperSecretCode(newSecretCodeInput)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = ExecEmerald,
                                contentColor = ExecDeepNavy
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(52.dp)
                        ) {
                            Text(
                                text = "حفظ الرمز",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val json = viewModel.exportFullSystemCloudBackupJson()
                                backupJsonPreview = json
                                copyToClipboard(context, "نسخة مدرستي الشاملة", json)
                                viewModel.showBannerMessage("تم تصدير ونسخ النسخة الاحتياطية الشاملة للنظام إلى الحافظة")
                            },
                            border = BorderStroke(1.dp, ExecSkyBlue),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ExecSkyBlue),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = "نسخ احتياطي شامل (JSON)",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (backupJsonPreview.isNotBlank()) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.restoreFullSystemCloudBackupJson(backupJsonPreview)
                                },
                                border = BorderStroke(1.dp, ExecAmberGold),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ExecAmberLight),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "استعادة النسخة",
                                    fontSize = 11.5.sp,
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

@Composable
private fun ExecutiveOutlinedField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp
            )
        },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 3,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = ExecTextPrimary,
            unfocusedTextColor = ExecTextPrimary,
            focusedBorderColor = ExecAmberGold,
            unfocusedBorderColor = ExecBorderNavy,
            focusedLabelColor = ExecAmberLight,
            unfocusedLabelColor = ExecTextSecondary,
            cursorColor = ExecAmberGold,
            focusedContainerColor = ExecDeepNavy.copy(alpha = 0.65f),
            unfocusedContainerColor = ExecDeepNavy.copy(alpha = 0.45f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    )
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText(label, text))
    } catch (_: Exception) {
    }
}
