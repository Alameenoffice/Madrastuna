package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.*
import com.example.ui.components.SoftWaveMeshBackground
import com.example.ui.components.ThreeDHeroHeaderCard
import com.example.ui.components.ThreeDIllustrationType
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.viewmodel.SchoolViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

enum class MasterAdminSectionTab(
    val title: String,
    val testTag: String
) {
    SCHOOLS("المدارس", "master_tab_schools"),
    STATISTICS("الإحصائيات", "master_tab_statistics"),
    CURRICULUM("المناهج", "master_tab_curriculum"),
    OCCASIONS("المناسبات", "master_tab_occasions"),
    ALERTS("التنبيهات", "master_tab_alerts")
}

/**
 * Root Coordinator for the Standalone "إدارة نظام مدرستي (Master System Admin Console)" Application.
 * Smoothly transitions via AnimatedContent (fadeIn + scaleIn) between:
 * 1. Pro Flat-Vector Animated Developer Welcome Gate (`DeveloperWelcomeGateScreen`)
 * 2. The 5-Section Master System Admin Console (`MasterSystemAdminConsoleScreen`)
 */
@Composable
fun MasterAdminStandaloneRootFlow(
    viewModel: SchoolViewModel
) {
    val isDeveloperAuthenticated by viewModel.isDeveloperAuthenticated.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshDeveloperCloudData()
    }

    AnimatedContent(
        targetState = isDeveloperAuthenticated,
        transitionSpec = {
            (fadeIn(animationSpec = tween(420, easing = FastOutSlowInEasing)) +
                scaleIn(initialScale = 0.94f, animationSpec = tween(420, easing = FastOutSlowInEasing)))
                .togetherWith(
                    fadeOut(animationSpec = tween(260)) +
                        scaleOut(targetScale = 0.96f, animationSpec = tween(260))
                )
        },
        label = "master_admin_gate_to_console_transition"
    ) { authenticated ->
        if (!authenticated) {
            DeveloperWelcomeGateScreen(viewModel = viewModel)
        } else {
            MasterSystemAdminConsoleScreen(viewModel = viewModel)
        }
    }
}

/**
 * =========================================================================
 * ثانياً: بوابة الترحيب التفاعلية بالمطور (Pro Flat-Vector Animated Developer Welcome Gate)
 * =========================================================================
 */
@Composable
fun DeveloperWelcomeGateScreen(
    viewModel: SchoolViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    var secretCodeInput by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifyingBadgeRaised by remember { mutableStateOf(false) }

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.35f) else Color(0xFF0D8A6A).copy(alpha = 0.22f)

    val isTypingActive = secretCodeInput.isNotBlank()

    fun triggerGateLogin() {
        focusManager.clearFocus()
        val trimmed = secretCodeInput.trim()
        if (trimmed.isBlank()) {
            viewModel.authenticateMasterDeveloperGate("")
            return
        }
        scope.launch {
            isVerifyingBadgeRaised = true
            delay(380)
            viewModel.authenticateMasterDeveloperGate(trimmed) { success, _ ->
                if (!success) {
                    isVerifyingBadgeRaised = false
                }
            }
        }
    }

    SoftWaveMeshBackground(isDarkMode = isDarkMode) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Header with App Identity & Dark/Light Mode Toggle
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.28f)),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "إدارة نظام مدرستي",
                                    tint = Color(0xFF0D8A6A),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "إدارة نظام مدرستي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false
                            )
                            Text(
                                text = "بوابة المطور الرئيسي للمنظومة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleDarkMode() },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0D8A6A).copy(alpha = 0.10f))
                            .testTag("dev_gate_theme_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                            contentDescription = "تبديل الوضع الليلي والنهاري",
                            tint = if (isDarkMode) GoldAccent else Color(0xFF0D8A6A)
                        )
                    }
                }
            }

            // Welcome Title above the Character
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF0D8A6A).copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = null,
                            tint = Color(0xFF0D8A6A),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "بوابة المطور الرئيسي",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                Text(
                    text = "مرحباً بك في منصة القيادة المركزية",
                    style = MaterialTheme.typography.headlineSmall,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "أدخل رمز المطور الرئيسي لإدارة المنظومة",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = secondaryText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Multi-Layered Flat-Vector Animated Character Canvas
            ProFlatVectorDeveloperCharacterCanvas(
                isTypingOrActive = isTypingActive,
                isBadgeRaised = isVerifyingBadgeRaised,
                isDarkMode = isDarkMode,
                modifier = Modifier
                    .size(230.dp)
                    .testTag("developer_welcome_vector_character")
            )

            // Luxury White Card for Developer Secret Code Input
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .testTag("developer_welcome_gate_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Key,
                            contentDescription = null,
                            tint = Color(0xFF0D8A6A),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "رمز الدخول السري للمطور",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText
                        )
                    }

                    OutlinedTextField(
                        value = secretCodeInput,
                        onValueChange = { secretCodeInput = it },
                        label = {
                            Text(
                                text = "رمز الدخول السري للمطور",
                                fontWeight = FontWeight.Bold
                            )
                        },
                        placeholder = {
                            Text("مثال: DEV-2026")
                        },
                        singleLine = true,
                        visualTransformation = if (isPasswordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = Color(0xFF0D8A6A)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { isPasswordVisible = !isPasswordVisible }
                            ) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = "إظهار أو إخفاء الرمز",
                                    tint = secondaryText
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { triggerGateLogin() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF0D8A6A),
                            unfocusedBorderColor = borderColor,
                            focusedLabelColor = Color(0xFF0D8A6A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("developer_secret_code_input")
                    )

                    // Quick Fill Chip for Default Developer Code (DEV-2026)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "الرمز الافتراضي المعتمد: DEV-2026",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = secondaryText
                        )
                        Surface(
                            onClick = { secretCodeInput = "DEV-2026" },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0D8A6A).copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.30f)),
                            modifier = Modifier.testTag("dev_quick_fill_code_chip")
                        ) {
                            Text(
                                text = "تعبئة DEV-2026",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }

                    Button(
                        onClick = { triggerGateLogin() },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D8A6A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("developer_enter_console_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "دخول لوحة القيادة",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }
    }
}

/**
 * Multi-Layered Flat-Vector Animated Leadership & Academic Character (`Canvas` with `Path.cubicTo`,
 * `LinearGradient`, `RadialGradient`, breathing physics, waving hand, and golden verification badge).
 */
@Composable
fun ProFlatVectorDeveloperCharacterCanvas(
    isTypingOrActive: Boolean,
    isBadgeRaised: Boolean,
    isDarkMode: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "dev_character_infinite")

    val breathPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "breath_phase"
    )

    val waveAngle by infiniteTransition.animateFloat(
        initialValue = -14f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "hand_wave_angle"
    )

    val orbitAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 11000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbit_angle"
    )

    val auraGlowScale by animateFloatAsState(
        targetValue = if (isBadgeRaised) 1.15f else if (isTypingOrActive) 1.07f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "aura_glow_scale"
    )

    val badgeLiftFraction by animateFloatAsState(
        targetValue = if (isBadgeRaised || isTypingOrActive) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "badge_lift_fraction"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.52f
        val breathOffset = sin(breathPhase) * (h * 0.014f)

        // 1. Background Radiant Emerald & Gold Aura
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    if (isTypingOrActive || isBadgeRaised) Color(0xFFD97706).copy(alpha = 0.28f)
                    else Color(0xFF10B981).copy(alpha = 0.24f),
                    Color(0xFF0D8A6A).copy(alpha = 0.10f),
                    Color.Transparent
                ),
                center = Offset(cx, cy),
                radius = w * 0.48f * auraGlowScale
            ),
            radius = w * 0.48f * auraGlowScale,
            center = Offset(cx, cy)
        )

        // Concentric Smart Control Orbits
        drawCircle(
            color = Color(0xFF0D8A6A).copy(alpha = if (isDarkMode) 0.32f else 0.18f),
            radius = w * 0.41f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.6.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFD97706).copy(alpha = 0.26f),
            radius = w * 0.34f,
            center = Offset(cx, cy),
            style = Stroke(width = 1.2.dp.toPx())
        )

        // Floating Orbiting Smart Nodes
        for (i in 0 until 4) {
            val rad = Math.toRadians((orbitAngle + i * 90f).toDouble())
            val orbitR = if (i % 2 == 0) w * 0.41f else w * 0.34f
            val nx = cx + cos(rad).toFloat() * orbitR
            val ny = cy + sin(rad).toFloat() * orbitR
            drawCircle(
                color = if (i % 2 == 0) Color(0xFF0D8A6A) else Color(0xFFF59E0B),
                radius = 4.5.dp.toPx(),
                center = Offset(nx, ny)
            )
            drawCircle(
                color = Color.White,
                radius = 2.dp.toPx(),
                center = Offset(nx, ny)
            )
        }

        // 2. Pedestal Base Ring
        drawOval(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF064E3B).copy(alpha = 0.28f),
                    Color(0xFF0D8A6A).copy(alpha = 0.12f)
                )
            ),
            topLeft = Offset(cx - w * 0.28f, h * 0.84f),
            size = Size(w * 0.56f, h * 0.08f)
        )

        // 3. Royal Emerald Academic / Executive Suit Torso (Cubic Bezier Path)
        val torsoTopY = cy + h * 0.02f + breathOffset
        val torsoBottomY = h * 0.86f
        val torsoPath = Path().apply {
            moveTo(cx - w * 0.23f, torsoBottomY)
            cubicTo(
                cx - w * 0.25f, torsoTopY + h * 0.10f,
                cx - w * 0.17f, torsoTopY,
                cx - w * 0.09f, torsoTopY
            )
            lineTo(cx + w * 0.09f, torsoTopY)
            cubicTo(
                cx + w * 0.17f, torsoTopY,
                cx + w * 0.25f, torsoTopY + h * 0.10f,
                cx + w * 0.23f, torsoBottomY
            )
            close()
        }
        drawPath(
            path = torsoPath,
            brush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF0D8A6A),
                    Color(0xFF065F46),
                    Color(0xFF064E3B)
                ),
                start = Offset(cx, torsoTopY),
                end = Offset(cx, torsoBottomY)
            )
        )

        // Crisp White Executive Shirt V-Neck & Gold Tie
        val shirtPath = Path().apply {
            moveTo(cx - w * 0.065f, torsoTopY)
            lineTo(cx + w * 0.065f, torsoTopY)
            lineTo(cx, torsoTopY + h * 0.18f)
            close()
        }
        drawPath(path = shirtPath, color = Color(0xFFF8FAFC))

        val tiePath = Path().apply {
            moveTo(cx - w * 0.016f, torsoTopY + h * 0.02f)
            lineTo(cx + w * 0.016f, torsoTopY + h * 0.02f)
            lineTo(cx + w * 0.024f, torsoTopY + h * 0.14f)
            lineTo(cx, torsoTopY + h * 0.18f)
            lineTo(cx - w * 0.024f, torsoTopY + h * 0.14f)
            close()
        }
        drawPath(
            path = tiePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFBBF24), Color(0xFFD97706))
            )
        )

        // Gold Lapel Trim Lines
        val leftLapel = Path().apply {
            moveTo(cx - w * 0.07f, torsoTopY)
            lineTo(cx - w * 0.11f, torsoTopY + h * 0.12f)
            lineTo(cx, torsoTopY + h * 0.19f)
        }
        drawPath(
            path = leftLapel,
            color = Color(0xFFFBBF24).copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        val rightLapel = Path().apply {
            moveTo(cx + w * 0.07f, torsoTopY)
            lineTo(cx + w * 0.11f, torsoTopY + h * 0.12f)
            lineTo(cx, torsoTopY + h * 0.19f)
        }
        drawPath(
            path = rightLapel,
            color = Color(0xFFFBBF24).copy(alpha = 0.85f),
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 4. Left Arm Holding Leadership Verification Shield / Badge
        val leftShoulderX = cx - w * 0.19f
        val leftShoulderY = torsoTopY + h * 0.06f
        val badgeLiftY = -badgeLiftFraction * (h * 0.06f)
        val badgeCenterX = cx - w * 0.28f
        val badgeCenterY = torsoTopY + h * 0.08f + badgeLiftY

        val leftArmPath = Path().apply {
            moveTo(leftShoulderX, leftShoulderY)
            cubicTo(
                leftShoulderX - w * 0.08f, leftShoulderY + h * 0.08f,
                badgeCenterX + w * 0.02f, badgeCenterY + h * 0.06f,
                badgeCenterX, badgeCenterY
            )
        }
        drawPath(
            path = leftArmPath,
            color = Color(0xFF065F46),
            style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
        )

        // Golden Leadership Verification Shield Badge
        val shieldW = w * (0.14f + 0.02f * badgeLiftFraction)
        val shieldH = h * (0.16f + 0.02f * badgeLiftFraction)
        val shieldPath = Path().apply {
            moveTo(badgeCenterX, badgeCenterY - shieldH * 0.5f)
            lineTo(badgeCenterX + shieldW * 0.5f, badgeCenterY - shieldH * 0.28f)
            cubicTo(
                badgeCenterX + shieldW * 0.5f, badgeCenterY + shieldH * 0.18f,
                badgeCenterX + shieldW * 0.22f, badgeCenterY + shieldH * 0.42f,
                badgeCenterX, badgeCenterY + shieldH * 0.55f
            )
            cubicTo(
                badgeCenterX - shieldW * 0.22f, badgeCenterY + shieldH * 0.42f,
                badgeCenterX - shieldW * 0.5f, badgeCenterY + shieldH * 0.18f,
                badgeCenterX - shieldW * 0.5f, badgeCenterY - shieldH * 0.28f
            )
            close()
        }
        drawPath(
            path = shieldPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFDE68A), Color(0xFFF59E0B), Color(0xFFB45309))
            )
        )
        drawPath(
            path = shieldPath,
            color = Color.White,
            style = Stroke(width = 1.8.dp.toPx())
        )
        // Checkmark inside Shield
        val checkPath = Path().apply {
            moveTo(badgeCenterX - shieldW * 0.18f, badgeCenterY)
            lineTo(badgeCenterX - shieldW * 0.04f, badgeCenterY + shieldH * 0.14f)
            lineTo(badgeCenterX + shieldW * 0.22f, badgeCenterY - shieldH * 0.14f)
        }
        drawPath(
            path = checkPath,
            color = Color(0xFF064E3B),
            style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // 5. Right Waving Hand (Interactive Physics)
        val rightShoulderX = cx + w * 0.19f
        val rightShoulderY = torsoTopY + h * 0.05f
        rotate(degrees = waveAngle, pivot = Offset(rightShoulderX, rightShoulderY)) {
            val rightArmPath = Path().apply {
                moveTo(rightShoulderX, rightShoulderY)
                cubicTo(
                    rightShoulderX + w * 0.10f, rightShoulderY - h * 0.03f,
                    rightShoulderX + w * 0.14f, rightShoulderY - h * 0.14f,
                    rightShoulderX + w * 0.12f, rightShoulderY - h * 0.22f
                )
            }
            drawPath(
                path = rightArmPath,
                color = Color(0xFF0D8A6A),
                style = Stroke(width = 13.dp.toPx(), cap = StrokeCap.Round)
            )
            // Waving Palm
            val handX = rightShoulderX + w * 0.12f
            val handY = rightShoulderY - h * 0.24f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE6C4), Color(0xFFF3C69F)),
                    center = Offset(handX, handY),
                    radius = 10.dp.toPx()
                ),
                radius = 9.dp.toPx(),
                center = Offset(handX, handY)
            )
        }

        // 6. Neck & Sculpted Flat-Vector Head with Warm Skin Gradient
        val headCenterY = cy - h * 0.14f + breathOffset
        val headW = w * 0.22f
        val headH = h * 0.25f

        drawRoundRect(
            color = Color(0xFFEBC098),
            topLeft = Offset(cx - w * 0.045f, torsoTopY - h * 0.05f),
            size = Size(w * 0.09f, h * 0.07f),
            cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
        )

        val facePath = Path().apply {
            moveTo(cx - headW * 0.5f, headCenterY - headH * 0.2f)
            cubicTo(
                cx - headW * 0.52f, headCenterY + headH * 0.35f,
                cx - headW * 0.26f, headCenterY + headH * 0.55f,
                cx, headCenterY + headH * 0.55f
            )
            cubicTo(
                cx + headW * 0.26f, headCenterY + headH * 0.55f,
                cx + headW * 0.52f, headCenterY + headH * 0.35f,
                cx + headW * 0.5f, headCenterY - headH * 0.2f
            )
            cubicTo(
                cx + headW * 0.48f, headCenterY - headH * 0.55f,
                cx - headW * 0.48f, headCenterY - headH * 0.55f,
                cx - headW * 0.5f, headCenterY - headH * 0.2f
            )
            close()
        }
        drawPath(
            path = facePath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFFFCE3C8), Color(0xFFF3C89E)),
                start = Offset(cx, headCenterY - headH * 0.5f),
                end = Offset(cx, headCenterY + headH * 0.5f)
            )
        )

        // Sculpted Hair & Royal Emerald Leadership Cap/Crest
        val hairPath = Path().apply {
            moveTo(cx - headW * 0.54f, headCenterY - headH * 0.12f)
            cubicTo(
                cx - headW * 0.56f, headCenterY - headH * 0.65f,
                cx + headW * 0.56f, headCenterY - headH * 0.65f,
                cx + headW * 0.54f, headCenterY - headH * 0.12f
            )
            cubicTo(
                cx + headW * 0.30f, headCenterY - headH * 0.38f,
                cx - headW * 0.30f, headCenterY - headH * 0.38f,
                cx - headW * 0.54f, headCenterY - headH * 0.12f
            )
            close()
        }
        drawPath(
            path = hairPath,
            brush = Brush.linearGradient(
                colors = listOf(Color(0xFF064E3B), Color(0xFF042F2E))
            )
        )

        // Modern Executive Glasses Frame (Emerald & Gold)
        val eyeY = headCenterY - headH * 0.02f
        val leftEyeX = cx - headW * 0.21f
        val rightEyeX = cx + headW * 0.21f
        val lensW = headW * 0.34f
        val lensH = headH * 0.20f

        drawRoundRect(
            color = Color(0xFF064E3B),
            topLeft = Offset(leftEyeX - lensW * 0.5f, eyeY - lensH * 0.5f),
            size = Size(lensW, lensH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawRoundRect(
            color = Color(0xFF064E3B),
            topLeft = Offset(rightEyeX - lensW * 0.5f, eyeY - lensH * 0.5f),
            size = Size(lensW, lensH),
            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
            style = Stroke(width = 2.dp.toPx())
        )
        drawLine(
            color = Color(0xFFD97706),
            start = Offset(leftEyeX + lensW * 0.5f, eyeY),
            end = Offset(rightEyeX - lensW * 0.5f, eyeY),
            strokeWidth = 2.dp.toPx()
        )

        // Expressive Eyes & Confident Smile
        drawCircle(
            color = Color(0xFF064E3B),
            radius = 2.6.dp.toPx(),
            center = Offset(leftEyeX, eyeY)
        )
        drawCircle(
            color = Color(0xFF064E3B),
            radius = 2.6.dp.toPx(),
            center = Offset(rightEyeX, eyeY)
        )

        val smilePath = Path().apply {
            moveTo(cx - headW * 0.16f, headCenterY + headH * 0.24f)
            cubicTo(
                cx - headW * 0.06f, headCenterY + headH * 0.34f,
                cx + headW * 0.06f, headCenterY + headH * 0.34f,
                cx + headW * 0.16f, headCenterY + headH * 0.24f
            )
        }
        drawPath(
            path = smilePath,
            color = Color(0xFF064E3B),
            style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

/**
 * =========================================================================
 * رابعاً: الأقسام الخمسة في تطبيق "إدارة نظام مدرستي" المستقل
 * =========================================================================
 */
@Composable
fun MasterSystemAdminConsoleScreen(
    viewModel: SchoolViewModel
) {
    val isDarkMode by viewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentSecretCode by viewModel.masterDeveloperSecretCode.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableStateOf(MasterAdminSectionTab.SCHOOLS) }
    var showChangeSecretDialog by remember { mutableStateOf(false) }
    var newSecretCodeDraft by remember(currentSecretCode) { mutableStateOf(currentSecretCode) }

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.32f) else Color(0xFF0D8A6A).copy(alpha = 0.20f)

    if (showChangeSecretDialog) {
        AlertDialog(
            onDismissRequest = { showChangeSecretDialog = false },
            containerColor = cardBg,
            title = {
                Text(
                    text = "تغيير رمز الدخول السري للمطور",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "الرمز الحالي: $currentSecretCode (يمكنك أيضاً استخدام الرمز القياسي DEV-2026 في أي وقت)",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText
                    )
                    OutlinedTextField(
                        value = newSecretCodeDraft,
                        onValueChange = { newSecretCodeDraft = it },
                        label = { Text("الرمز السري الجديد") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_new_secret_code_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMasterDeveloperSecretCode(newSecretCodeDraft)
                        showChangeSecretDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D8A6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ الرمز الجديد", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangeSecretDialog = false }) {
                    Text("إلغاء", color = secondaryText, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                border = BorderStroke(1.dp, borderColor),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.28f)),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AdminPanelSettings,
                                    contentDescription = "إدارة نظام مدرستي",
                                    tint = Color(0xFF0D8A6A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "إدارة نظام مدرستي",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "منصة القيادة المركزية للمطور",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.refreshDeveloperCloudData()
                                viewModel.showToastAlert("تم تحديث واكتشاف أحدث البيانات بنجاح", isSuccess = true)
                            },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D8A6A).copy(alpha = 0.10f))
                                .testTag("master_console_sync_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Sync,
                                contentDescription = "مزامنة البيانات",
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { showChangeSecretDialog = true },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D8A6A).copy(alpha = 0.10f))
                                .testTag("master_console_secret_settings_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Key,
                                contentDescription = "إعدادات رمز المطور",
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleDarkMode() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0D8A6A).copy(alpha = 0.10f))
                                .testTag("master_console_theme_btn")
                        ) {
                            Icon(
                                imageVector = if (isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل المظهر",
                                tint = if (isDarkMode) GoldAccent else Color(0xFF0D8A6A),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.exitDeveloperPanel() },
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626).copy(alpha = 0.10f))
                                .testTag("master_console_lock_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "قفل لوحة القيادة",
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            NavigationBar(
                containerColor = cardBg,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .testTag("master_admin_bottom_nav")
            ) {
                MasterAdminSectionTab.entries.forEach { tab ->
                    val selected = selectedTab == tab
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedTab = tab },
                        icon = {
                            Icon(
                                imageVector = when (tab) {
                                    MasterAdminSectionTab.SCHOOLS -> Icons.Filled.Domain
                                    MasterAdminSectionTab.STATISTICS -> Icons.Filled.Analytics
                                    MasterAdminSectionTab.CURRICULUM -> Icons.AutoMirrored.Filled.MenuBook
                                    MasterAdminSectionTab.OCCASIONS -> Icons.Filled.AutoAwesome
                                    MasterAdminSectionTab.ALERTS -> Icons.Filled.Campaign
                                },
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 11.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Visible
                            )
                        },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.White,
                            selectedTextColor = primaryText,
                            indicatorColor = Color(0xFF0D8A6A),
                            unselectedIconColor = secondaryText,
                            unselectedTextColor = secondaryText
                        ),
                        modifier = Modifier.testTag(tab.testTag)
                    )
                }
            }
        }
    ) { innerPadding ->
        SoftWaveMeshBackground(isDarkMode = isDarkMode) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    MasterAdminSectionTab.SCHOOLS -> MasterSchoolsManagementTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    MasterAdminSectionTab.STATISTICS -> MasterStatisticsInspectionTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    MasterAdminSectionTab.CURRICULUM -> MasterCurriculumAutoDiscoveryTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    MasterAdminSectionTab.OCCASIONS -> MasterOccasionsRemoteThemeTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    MasterAdminSectionTab.ALERTS -> MasterAlertsAndUpdatesTab(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

/**
 * =========================================================================
 * 1. قسم "المدارس" (إدارة المدارس وبوابة التفعيل السحابية)
 * =========================================================================
 */
@Composable
private fun MasterSchoolsManagementTab(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    val registeredSchools by viewModel.registeredSchools.collectAsStateWithLifecycle()
    val cloudRevision by viewModel.cloudDiscoveryRevision.collectAsStateWithLifecycle()

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.32f) else Color(0xFF0D8A6A).copy(alpha = 0.20f)

    var newSchoolName by remember { mutableStateOf("") }
    var newSchoolStage by remember { mutableStateOf("ثانوية") }
    var newActivationCode by remember { mutableStateOf("") }
    var newAdminPassword by remember { mutableStateOf("123456") }
    var newPrincipalName by remember { mutableStateOf("") }

    var editingSchool by remember { mutableStateOf<RegisteredSchoolSummary?>(null) }
    var editCodeDraft by remember { mutableStateOf("") }
    var editPassDraft by remember { mutableStateOf("") }

    editingSchool?.let { target ->
        AlertDialog(
            onDismissRequest = { editingSchool = null },
            containerColor = cardBg,
            title = {
                Text(
                    text = "تعديل الكود أو كلمة سر المدير",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = target.schoolName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0D8A6A)
                    )
                    OutlinedTextField(
                        value = editCodeDraft,
                        onValueChange = { editCodeDraft = it },
                        label = { Text("كود تفعيل المدرسة") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editPassDraft,
                        onValueChange = { editPassDraft = it },
                        label = { Text("كلمة سر المدير") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateSchoolActivationCodeAndAdminPasswordRemote(
                            schoolId = target.schoolId,
                            newActivationCode = editCodeDraft,
                            newAdminPassword = editPassDraft
                        )
                        editingSchool = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D8A6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التعديلات", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingSchool = null }) {
                    Text("إلغاء", color = secondaryText, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ThreeDHeroHeaderCard(
                title = "إدارة المدارس وبوابة التفعيل",
                subtitle = "اكتشاف تلقائي للمدارس المسجلة والتحكم الفوري عن بُعد بالتفعيل والأكواد",
                rightPillText = "إدارة المنظومة",
                leftPillText = "المدارس: ${registeredSchools.size}",
                illustrationType = ThreeDIllustrationType.SECURITY_SHIELD
            )
        }

        // Card to Add New School & Generate Activation Code
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_add_school_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AddBusiness,
                                contentDescription = null,
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "إضافة مدرسة جديدة وتوليد كود التفعيل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                        }

                        Surface(
                            onClick = {
                                val generated = viewModel.generateNewSchoolLicense()
                                newActivationCode = generated.code
                            },
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.30f)),
                            modifier = Modifier.testTag("master_generate_code_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.VpnKey,
                                    contentDescription = null,
                                    tint = Color(0xFF0D8A6A),
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "توليد كود تلقائي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0D8A6A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newSchoolName,
                        onValueChange = { newSchoolName = it },
                        label = { Text("اسم المدرسة (مثال: ثانوية النخبة الأهلية)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("master_new_school_name_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("ابتدائية", "متوسطة", "إعدادية", "ثانوية").forEach { stageLabel ->
                            val selected = newSchoolStage == stageLabel
                            Surface(
                                onClick = { newSchoolStage = stageLabel },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0xFF0D8A6A) else Color(0xFF0D8A6A).copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) GoldAccent else Color(0xFF0D8A6A).copy(alpha = 0.22f)
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = stageLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else primaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = newActivationCode,
                            onValueChange = { newActivationCode = it },
                            label = { Text("كود تفعيل المدرسة") },
                            placeholder = { Text("مثال: 1005") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("master_new_school_code_input")
                        )

                        OutlinedTextField(
                            value = newAdminPassword,
                            onValueChange = { newAdminPassword = it },
                            label = { Text("كلمة سر المدير") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("master_new_school_pass_input")
                        )
                    }

                    OutlinedTextField(
                        value = newPrincipalName,
                        onValueChange = { newPrincipalName = it },
                        label = { Text("اسم مدير المدرسة (اختياري)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            viewModel.addNewSchoolDirectlyFromMasterConsole(
                                schoolName = newSchoolName,
                                schoolStageLabel = newSchoolStage,
                                activationCode = newActivationCode,
                                adminPassword = newAdminPassword,
                                principalName = newPrincipalName.ifBlank { "مدير المدرسة" }
                            )
                            newSchoolName = ""
                            newActivationCode = ""
                            newPrincipalName = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D8A6A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("master_confirm_add_school_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إضافة المدرسة وتفعيل كودها فوراً",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // Section Header for Auto-Discovered Schools
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "المدارس المكتشفة في المنظومة (${registeredSchools.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText
                )
                Text(
                    text = "تحكم مباشر عن بُعد",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D8A6A)
                )
            }
        }

        items(registeredSchools, key = { it.schoolId }) { school ->
            val stats = remember(school, cloudRevision) {
                viewModel.getSchoolDetailedStats(school.schoolId)
            }
            val effectiveCode = school.schoolActivationCode.ifBlank { school.schoolId }
            val effectivePass = stats.adminPasswordDisplay
            val effectiveAdminUser = stats.principalAccount?.username?.ifBlank { "${school.schoolId}-admin" } ?: "${school.schoolId}-admin"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_school_card_${school.schoolId}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header Row: School Name + Status Badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = school.schoolName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                            Text(
                                text = "${school.schoolStage} • المدير: ${stats.principalAccount?.fullName ?: school.principalFullName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = secondaryText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = when {
                                !school.isActive -> Color(0xFFDC2626).copy(alpha = 0.12f)
                                !school.isSchoolOfficiallyActivated -> Color(0xFFD97706).copy(alpha = 0.14f)
                                else -> Color(0xFF0D8A6A).copy(alpha = 0.12f)
                            },
                            border = BorderStroke(
                                1.dp,
                                when {
                                    !school.isActive -> Color(0xFFDC2626).copy(alpha = 0.4f)
                                    !school.isSchoolOfficiallyActivated -> Color(0xFFD97706).copy(alpha = 0.4f)
                                    else -> Color(0xFF0D8A6A).copy(alpha = 0.4f)
                                }
                            )
                        ) {
                            Text(
                                text = when {
                                    !school.isActive -> "مجمّدة مؤقتاً"
                                    !school.isSchoolOfficiallyActivated -> "بوابة مقفلة"
                                    else -> "مفعّلة رسمياً"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = when {
                                    !school.isActive -> Color(0xFFDC2626)
                                    !school.isSchoolOfficiallyActivated -> Color(0xFFD97706)
                                    else -> Color(0xFF0D8A6A)
                                },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Activation & Credentials Summary Box
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF0D8A6A).copy(alpha = 0.06f),
                        border = BorderStroke(1.dp, borderColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "كود تفعيل المدرسة: $effectiveCode",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "حساب المدير: $effectiveAdminUser | كلمة السر: $effectivePass",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText
                                )
                            }

                            Text(
                                text = "${stats.students.size} طالب • ${stats.teachers.size} معلم",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A)
                            )
                        }
                    }

                    // Remote Control Action Buttons Grid (All 5 Requested Controls)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. تفعيل رسمي
                        Button(
                            onClick = {
                                viewModel.setSchoolOfficialActivationRemote(school.schoolId, true)
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D8A6A),
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("school_activate_btn_${school.schoolId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تفعيل رسمي",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 2. إيقاف/تجميد مؤقت
                        OutlinedButton(
                            onClick = {
                                viewModel.toggleSchoolSubscriptionActive(school.schoolId)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                1.dp,
                                if (school.isActive) Color(0xFFD97706) else Color(0xFF0D8A6A)
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp)
                                .testTag("school_freeze_btn_${school.schoolId}")
                        ) {
                            Icon(
                                imageVector = if (school.isActive) Icons.Filled.PauseCircle else Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = if (school.isActive) Color(0xFFD97706) else Color(0xFF0D8A6A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (school.isActive) "إيقاف/تجميد مؤقت" else "إلغاء التجميد",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (school.isActive) Color(0xFFD97706) else Color(0xFF0D8A6A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 3. إعادة قفل البوابة لمطالبة المدير بالتفعيل
                        OutlinedButton(
                            onClick = {
                                viewModel.setSchoolOfficialActivationRemote(school.schoolId, false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("school_relock_gate_btn_${school.schoolId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LockReset,
                                contentDescription = null,
                                tint = primaryText,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إعادة قفل البوابة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        // 4. تعديل الكود أو كلمة سر المدير
                        OutlinedButton(
                            onClick = {
                                editCodeDraft = effectiveCode
                                editPassDraft = effectivePass
                                editingSchool = school
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .testTag("school_edit_creds_btn_${school.schoolId}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Edit,
                                contentDescription = null,
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "تعديل الكود/كلمة السر",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // 5. نسخ بيانات التفعيل
                    Surface(
                        onClick = {
                            val clipText = "المدرسة: ${school.schoolName}\nكود التفعيل: $effectiveCode\nاسم مستخدم المدير: $effectiveAdminUser\nكلمة سر المدير: $effectivePass"
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cm?.setPrimaryClip(ClipData.newPlainText("بيانات تفعيل المدرسة", clipText))
                            viewModel.showToastAlert("تم نسخ بيانات التفعيل لمدرسة (${school.schoolName})", isSuccess = true)
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0D8A6A).copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.30f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .testTag("school_copy_creds_btn_${school.schoolId}")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = null,
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نسخ بيانات التفعيل",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A),
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

/**
 * =========================================================================
 * 2. قسم "الإحصائيات" (الرقابة الشاملة وفحص السجلات)
 * =========================================================================
 */
@Composable
private fun MasterStatisticsInspectionTab(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean
) {
    val registeredSchools by viewModel.registeredSchools.collectAsStateWithLifecycle()
    val cloudRevision by viewModel.cloudDiscoveryRevision.collectAsStateWithLifecycle()

    val allStats = remember(registeredSchools, cloudRevision) {
        viewModel.getAllDiscoveredSchoolsDetailedStats()
    }

    val totalSchoolsCount = allStats.size
    val totalStudentsCount = allStats.sumOf { it.students.size }
    val totalTeachersCount = allStats.sumOf { it.teachers.size }
    val overallAttendanceAvg = if (allStats.isNotEmpty()) {
        allStats.map { it.attendanceRatePercent }.average().toInt()
    } else {
        96
    }

    var selectedSchoolId by remember(allStats) {
        mutableStateOf(allStats.firstOrNull()?.school?.schoolId ?: "1001")
    }
    val selectedStats = allStats.firstOrNull {
        it.school.schoolId.equals(selectedSchoolId, ignoreCase = true)
    } ?: allStats.firstOrNull()

    var inspectionSubTab by remember { mutableIntStateOf(0) } // 0 = الطلاب والشعب, 1 = الكادر التدريسي, 2 = الحضور والنشاط

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.32f) else Color(0xFF0D8A6A).copy(alpha = 0.20f)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ThreeDHeroHeaderCard(
                title = "الرقابة الشاملة والإحصائيات الفعلية",
                subtitle = "عدادات حية وفحص تفصيلي لسجلات الطلاب والكادر التدريسي ونسب الحضور لكل مدرسة",
                rightPillText = "الطلاب: $totalStudentsCount",
                leftPillText = "المعلمون: $totalTeachersCount",
                illustrationType = ThreeDIllustrationType.GLOWING_CHART_SPHERE
            )
        }

        // System-wide Summary Stat Cards (Never 0)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                EmeraldMetricSummaryCard(
                    title = "إجمالي المدارس",
                    value = "$totalSchoolsCount",
                    subtitle = "مدرسة مسجلة",
                    icon = Icons.Filled.Domain,
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor,
                    modifier = Modifier.weight(1f)
                )
                EmeraldMetricSummaryCard(
                    title = "الطلاب الفعليون",
                    value = "$totalStudentsCount",
                    subtitle = "طالب وطالبة",
                    icon = Icons.Filled.Groups,
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor,
                    modifier = Modifier.weight(1f)
                )
                EmeraldMetricSummaryCard(
                    title = "الكادر التدريسي",
                    value = "$totalTeachersCount",
                    subtitle = "معلم ومدرس",
                    icon = Icons.Filled.School,
                    cardBg = cardBg,
                    primaryText = primaryText,
                    secondaryText = secondaryText,
                    borderColor = borderColor,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // School Selector for Deep Record Inspection
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "اختر المدرسة لفحص سجلاتها التفصيلية:",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(allStats, key = { it.school.schoolId }) { item ->
                            val isSelected = item.school.schoolId.equals(selectedSchoolId, ignoreCase = true)
                            Surface(
                                onClick = { selectedSchoolId = item.school.schoolId },
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF0D8A6A) else Color(0xFF0D8A6A).copy(alpha = 0.08f),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) GoldAccent else borderColor
                                ),
                                modifier = Modifier.testTag("stats_select_school_${item.school.schoolId}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = item.school.schoolName,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) Color.White else primaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                    Text(
                                        text = "${item.students.size} طالب • ${item.teachers.size} معلم • حضور ${item.attendanceRatePercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White.copy(alpha = 0.9f) else secondaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    // Sub-tabs for the selected school's records
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            0 to "الطلاب والشعب (${selectedStats?.students?.size ?: 0})",
                            1 to "المعلمون (${selectedStats?.teachers?.size ?: 0})",
                            2 to "الحضور والنشاط (${selectedStats?.attendanceRatePercent ?: overallAttendanceAvg}%)"
                        ).forEach { (idx, label) ->
                            val active = inspectionSubTab == idx
                            Surface(
                                onClick = { inspectionSubTab = idx },
                                shape = RoundedCornerShape(12.dp),
                                color = if (active) Color(0xFF065F46) else Color(0xFF0D8A6A).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (active) GoldAccent else borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (active) Color.White else primaryText,
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

        if (selectedStats != null) {
            when (inspectionSubTab) {
                0 -> {
                    // Sections summary chips
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "توزيع الصفوف والشعب الدراسية في ${selectedStats.school.schoolName}:",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(selectedStats.sectionsMap.entries.toList()) { (secLabel, count) ->
                                        Surface(
                                            shape = RoundedCornerShape(50),
                                            color = Color(0xFF0D8A6A).copy(alpha = 0.10f),
                                            border = BorderStroke(1.dp, Color(0xFF0D8A6A).copy(alpha = 0.30f))
                                        ) {
                                            Text(
                                                text = "$secLabel ($count طالب)",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = primaryText,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    items(selectedStats.students, key = { it.id }) { student ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Person,
                                                contentDescription = null,
                                                tint = Color(0xFF0D8A6A)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = student.studentName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = "${student.gradeClass} - شعبة ${student.sectionName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryText
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "المعدل: ${student.averageScore}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D8A6A)
                                    )
                                    Text(
                                        text = "الحضور: ${student.attendanceRate}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = secondaryText
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    items(selectedStats.teachers, key = { it.id }) { teacher ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.dp, borderColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.School,
                                                contentDescription = null,
                                                tint = Color(0xFF0D8A6A)
                                            )
                                        }
                                    }
                                    Column {
                                        Text(
                                            text = teacher.fullName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = primaryText
                                        )
                                        Text(
                                            text = "الاختصاص: ${teacher.specialty.ifBlank { "تدريسي" }} • الصفوف: ${teacher.assignedClasses.ifBlank { teacher.gradeClass }}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = secondaryText
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF0D8A6A).copy(alpha = 0.10f)
                                ) {
                                    Text(
                                        text = teacher.username,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0D8A6A),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            border = BorderStroke(1.2.dp, borderColor)
                        ) {
                            Column(
                                modifier = Modifier.padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "تقرير الحضور والنشاط المدرسي — ${selectedStats.school.schoolName}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                LinearProgressIndicator(
                                    progress = { (selectedStats.attendanceRatePercent / 100f).coerceIn(0f, 1f) },
                                    color = Color(0xFF0D8A6A),
                                    trackColor = Color(0xFF0D8A6A).copy(alpha = 0.14f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(50))
                                )
                                Text(
                                    text = "نسبة الالتزام بالحضور اليومي: ${selectedStats.attendanceRatePercent}%",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0D8A6A)
                                )
                                Text(
                                    text = "المدير المسؤول: ${selectedStats.principalAccount?.fullName ?: selectedStats.school.principalFullName} (${selectedStats.principalAccount?.username ?: "${selectedStats.school.schoolId}-admin"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText
                                )
                                Text(
                                    text = "إجمالي الشعب الدراسية النشطة: ${selectedStats.sectionsMap.size} شعبة • الأنشطة المنشورة: ${selectedStats.activityPostsCount}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText
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
private fun EmeraldMetricSummaryCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    cardBg: Color,
    primaryText: Color,
    secondaryText: Color,
    borderColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.2.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF0D8A6A).copy(alpha = 0.12f),
                modifier = Modifier.size(34.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color(0xFF0D8A6A),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF0D8A6A),
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText,
                maxLines = 1,
                softWrap = false
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                fontSize = 10.sp,
                color = secondaryText,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

/**
 * =========================================================================
 * 3. قسم "المناهج" (الاكتشاف التلقائي والتحكم بروابط الكتب)
 * =========================================================================
 */
@Composable
private fun MasterCurriculumAutoDiscoveryTab(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        IraqiCurriculumCatalog.init(context)
    }
    val allBooks by IraqiCurriculumCatalog.booksFlow.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            CurriculumHeroBannerWithOpenBookAnimation(
                title = "المناهج الدراسية وروابط الكتب",
                subtitle = "اكتشاف تلقائي لجميع المراحل والصفوف والكتب (${allBooks.size} كتاباً) مع تحويل فوري لروابط Google Drive",
                rightPillText = "المناهج العراقية المعتمدة",
                leftPillText = "${allBooks.size} كتاب مكتشف"
            )
        }

        item {
            DeveloperCurriculumManagerSection(viewModel = viewModel)
        }
    }
}

/**
 * =========================================================================
 * 4. قسم "المناسبات" (الهوية الموسمية عن بُعد)
 * =========================================================================
 */
@Composable
private fun MasterOccasionsRemoteThemeTab(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean
) {
    val occasionConfig by viewModel.occasionThemeConfig.collectAsStateWithLifecycle()

    var selectedTheme by remember(occasionConfig.themeType) {
        mutableStateOf(occasionConfig.themeType)
    }
    var customGreetingText by remember(occasionConfig.customBannerText) {
        mutableStateOf(
            occasionConfig.customBannerText.ifBlank {
                "نتمنى لجميع إدارات المدارس والكادر التدريسي والطلاب دوام التفوق والنجاح"
            }
        )
    }

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.32f) else Color(0xFF0D8A6A).copy(alpha = 0.20f)

    val supportedOccasions = remember {
        listOf(
            OccasionThemeType.OFFICIAL,
            OccasionThemeType.RAMADAN,
            OccasionThemeType.EID,
            OccasionThemeType.NEW_YEAR,
            OccasionThemeType.MAWLID_NABAWI,
            OccasionThemeType.IRAQI_NATIONAL_DAY
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ThreeDHeroHeaderCard(
                title = "الهوية الموسمية والمناسبات عن بُعد",
                subtitle = "تفعيل شريط وأيقونة المناسبة وتخصيص نص التهنئة في جميع المدارس بضغطة زر",
                rightPillText = "الهوية الموسمية",
                leftPillText = if (occasionConfig.isActive) "نشط: ${occasionConfig.effectiveTitle}" else "الوضع الرسمي",
                illustrationType = ThreeDIllustrationType.CRESCENT_LANTERN_3D
            )
        }

        // Occasion Cards Grid
        items(supportedOccasions, key = { it.name }) { themeType ->
            val isSelected = selectedTheme == themeType
            val isCurrentlyLive = occasionConfig.isActive && occasionConfig.themeType == themeType

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedTheme = themeType
                        customGreetingText = themeType.bannerGreeting
                    }
                    .testTag("occasion_card_${themeType.name}"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(
                    if (isSelected) 2.dp else 1.dp,
                    if (isSelected) Color(0xFF0D8A6A) else borderColor
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 5.dp else 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFF0D8A6A) else Color(0xFF0D8A6A).copy(alpha = 0.10f),
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (isSelected) GoldAccent else Color(0xFF0D8A6A),
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = themeType.arabicLabel,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                            Text(
                                text = themeType.bannerGreeting,
                                style = MaterialTheme.typography.bodySmall,
                                color = secondaryText,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (isCurrentlyLive) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0D8A6A).copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A))
                        ) {
                            Text(
                                text = "مفعّل حالياً",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // Custom Greeting & Remote Activation Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تخصيص نص التهنئة للمناسبة المختارة (${selectedTheme.arabicLabel}):",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = primaryText
                    )

                    OutlinedTextField(
                        value = customGreetingText,
                        onValueChange = { customGreetingText = it },
                        label = { Text("نص التهنئة الظاهر في شريط جميع المدارس") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("occasion_custom_greeting_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                if (selectedTheme == OccasionThemeType.OFFICIAL) {
                                    viewModel.resetGlobalOccasionThemeToOfficial()
                                } else {
                                    viewModel.updateGlobalOccasionTheme(
                                        occasionTheme = selectedTheme,
                                        customGreeting = customGreetingText
                                    )
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D8A6A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .testTag("occasion_activate_remote_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "تفعيل في جميع المدارس",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.resetGlobalOccasionThemeToOfficial() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFF0D8A6A)),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("occasion_reset_official_btn")
                        ) {
                            Text(
                                text = "الهوية الرسمية",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0D8A6A),
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

/**
 * =========================================================================
 * 5. قسم "التنبيهات" (إشعارات التحديث الجديد + إدارة التنبيهات المرسلة)
 * =========================================================================
 */
@Composable
private fun MasterAlertsAndUpdatesTab(
    viewModel: SchoolViewModel,
    isDarkMode: Boolean
) {
    val appUpdateConfig by viewModel.appUpdateConfig.collectAsStateWithLifecycle()
    val developerBroadcasts by viewModel.developerBroadcasts.collectAsStateWithLifecycle()

    var versionName by remember(appUpdateConfig.latestVersionName) {
        mutableStateOf(appUpdateConfig.latestVersionName.ifBlank { "2.5.0" })
    }
    var apkDownloadUrl by remember(appUpdateConfig.apkDownloadUrl) {
        mutableStateOf(appUpdateConfig.apkDownloadUrl)
    }
    var releaseNotes by remember(appUpdateConfig.releaseNotes) {
        mutableStateOf(appUpdateConfig.releaseNotes)
    }
    var isForceUpdate by remember(appUpdateConfig.isForceUpdate) {
        mutableStateOf(appUpdateConfig.isForceUpdate)
    }

    var broadcastTitle by remember { mutableStateOf("") }
    var broadcastMessage by remember { mutableStateOf("") }
    var targetAudienceRole by remember { mutableStateOf("ALL") } // ALL, ADMIN, TEACHER, STUDENT

    var editingBroadcast by remember { mutableStateOf<DeveloperBroadcastItem?>(null) }
    var editBroadcastTitle by remember { mutableStateOf("") }
    var editBroadcastBody by remember { mutableStateOf("") }

    val cardBg = if (isDarkMode) Color(0xFF112620) else Color.White
    val primaryText = if (isDarkMode) Color(0xFFF8FAFC) else Color(0xFF064E3B)
    val secondaryText = if (isDarkMode) Color(0xFFCBD5E1) else Color(0xFF334155)
    val borderColor = if (isDarkMode) Color(0xFF10B981).copy(alpha = 0.32f) else Color(0xFF0D8A6A).copy(alpha = 0.20f)

    editingBroadcast?.let { target ->
        AlertDialog(
            onDismissRequest = { editingBroadcast = null },
            containerColor = cardBg,
            title = {
                Text(
                    text = "تعديل نص التنبيه المرسل",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = primaryText
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editBroadcastTitle,
                        onValueChange = { editBroadcastTitle = it },
                        label = { Text("عنوان التنبيه") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editBroadcastBody,
                        onValueChange = { editBroadcastBody = it },
                        label = { Text("نص التنبيه") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateDeveloperBroadcast(
                            broadcastId = target.id,
                            newTitle = editBroadcastTitle,
                            newMessage = editBroadcastBody
                        )
                        editingBroadcast = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0D8A6A),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("حفظ التعديل", fontWeight = FontWeight.ExtraBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBroadcast = null }) {
                    Text("إلغاء", color = secondaryText, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ThreeDHeroHeaderCard(
                title = "إشعارات التحديث والتنبيهات المركزية",
                subtitle = "إرسال إشعارات التحديث الجديد للتطبيق والتعاميم الفورية مع التحكم الكامل بسجل التنبيهات",
                rightPillText = "مركز التنبيهات",
                leftPillText = "النشطة: ${developerBroadcasts.count { it.isActive }}",
                illustrationType = ThreeDIllustrationType.SYSTEM_BROADCAST_3D
            )
        }

        // 1. بطاقة إشعار التحديث الجديد للتطبيق
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_app_update_alert_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SystemUpdate,
                                contentDescription = null,
                                tint = Color(0xFF0D8A6A),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = "بطاقة إشعار التحديث الجديد للتطبيق",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = primaryText
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (appUpdateConfig.isUpdateBroadcastActive) {
                                Color(0xFF0D8A6A).copy(alpha = 0.14f)
                            } else {
                                secondaryText.copy(alpha = 0.12f)
                            }
                        ) {
                            Text(
                                text = if (appUpdateConfig.isUpdateBroadcastActive) "التحديث منشور" else "متوقف حالياً",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (appUpdateConfig.isUpdateBroadcastActive) Color(0xFF0D8A6A) else secondaryText,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = versionName,
                            onValueChange = { versionName = it },
                            label = { Text("رقم الإصدار") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.4f)
                                .testTag("update_version_input")
                        )

                        OutlinedTextField(
                            value = apkDownloadUrl,
                            onValueChange = { apkDownloadUrl = it },
                            label = { Text("رابط التحميل المباشر (APK)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .weight(0.6f)
                                .testTag("update_url_input")
                        )
                    }

                    OutlinedTextField(
                        value = releaseNotes,
                        onValueChange = { releaseNotes = it },
                        label = { Text("ملاحظات ومزايا التحديث الجديد") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("update_notes_input")
                    )

                    // Optional vs Force Update Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            false to "تحديث اختياري",
                            true to "تحديث إجباري (Force Update)"
                        ).forEach { (forceVal, label) ->
                            val selected = isForceUpdate == forceVal
                            Surface(
                                onClick = { isForceUpdate = forceVal },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0xFF0D8A6A) else Color(0xFF0D8A6A).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (selected) GoldAccent else borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else primaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateGlobalAppUpdateConfig(
                                    versionName = versionName,
                                    apkUrl = apkDownloadUrl,
                                    releaseNotes = releaseNotes,
                                    isForceUpdate = isForceUpdate,
                                    isBroadcastActive = true
                                )
                            },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF0D8A6A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("publish_update_alert_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudUpload,
                                contentDescription = null,
                                tint = GoldAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "نشر إشعار التحديث",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }

                        OutlinedButton(
                            onClick = { viewModel.stopAndWithdrawGlobalAppUpdate() },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, Color(0xFFDC2626)),
                            modifier = Modifier
                                .height(46.dp)
                                .testTag("withdraw_update_alert_btn")
                        ) {
                            Text(
                                text = "إيقاف وسحب التحديث",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFDC2626),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }
        }

        // 2. بطاقة إرسال تعميم إداري فوري
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("master_send_broadcast_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.2.dp, borderColor),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF0D8A6A),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "بطاقة إرسال تعميم إداري فوري",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = primaryText
                        )
                    }

                    Text(
                        text = "الفئة المستهدفة بالتعميم:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = secondaryText
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            "ALL" to "الجميع",
                            "ADMIN" to "المدراء",
                            "TEACHER" to "المعلمون",
                            "STUDENT" to "الطلاب"
                        ).forEach { (roleKey, roleLabel) ->
                            val selected = targetAudienceRole == roleKey
                            Surface(
                                onClick = { targetAudienceRole = roleKey },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) Color(0xFF0D8A6A) else Color(0xFF0D8A6A).copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, if (selected) GoldAccent else borderColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("broadcast_target_$roleKey")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = roleLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (selected) Color.White else primaryText,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = broadcastTitle,
                        onValueChange = { broadcastTitle = it },
                        label = { Text("عنوان التعميم الإداري") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_title_input")
                    )

                    OutlinedTextField(
                        value = broadcastMessage,
                        onValueChange = { broadcastMessage = it },
                        label = { Text("نص التعميم والتنبيه الفوري") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("broadcast_message_input")
                    )

                    Button(
                        onClick = {
                            viewModel.sendTargetedDeveloperBroadcast(
                                title = broadcastTitle,
                                message = broadcastMessage,
                                targetAudienceRole = targetAudienceRole
                            )
                            broadcastTitle = ""
                            broadcastMessage = ""
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D8A6A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("broadcast_send_submit_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Send,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "إرسال التعميم الفوري الآن",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
        }

        // 3. سجل التنبيهات المرسلة والنشطة
        item {
            Text(
                text = "سجل التنبيهات المرسلة والنشطة (${developerBroadcasts.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = primaryText
            )
        }

        if (developerBroadcasts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.dp, borderColor)
                ) {
                    Text(
                        text = "لا توجد تعاميم مرسلة حالياً. يمكنك إرسال تعميم جديد من البطاقة أعلاه ليظهر هنا مع خيارات التعديل والإيقاف والحذف.",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = secondaryText,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(developerBroadcasts, key = { it.id }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sent_broadcast_card_${item.id}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    border = BorderStroke(1.2.dp, borderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = primaryText
                                )
                                Text(
                                    text = "الفئة: ${item.targetAudienceArabicLabel} • ${item.createdAt}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = if (item.isActive) Color(0xFF0D8A6A).copy(alpha = 0.14f) else Color(0xFFD97706).copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (item.isActive) "نشط ومعروض" else "متوقف العرض",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (item.isActive) Color(0xFF0D8A6A) else Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }

                        Text(
                            text = item.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = primaryText
                        )

                        // Action Buttons in front of each alert: (تعديل النص، إيقاف العرض، حذف نهائي)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    editBroadcastTitle = item.title
                                    editBroadcastBody = item.message
                                    editingBroadcast = item
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFF0D8A6A)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("broadcast_edit_btn_${item.id}")
                            ) {
                                Text(
                                    text = "تعديل النص",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0D8A6A),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.toggleDeveloperBroadcastActive(item.id) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("broadcast_toggle_btn_${item.id}")
                            ) {
                                Text(
                                    text = if (item.isActive) "إيقاف العرض" else "إعادة العرض",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFD97706),
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            OutlinedButton(
                                onClick = { viewModel.deleteDeveloperBroadcast(item.id) },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFDC2626)),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("broadcast_delete_btn_${item.id}")
                            ) {
                                Text(
                                    text = "حذف نهائي",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFDC2626),
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
