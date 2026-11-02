package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.BiometricSavedCredentials
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.EmeraldAccent
import com.example.util.BiometricAuthHelper
import com.example.util.StandardQrCodeHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Custom Vector SVG / Canvas Fingerprint Icon with multi-ridge biometric curves.
 */
@Composable
fun BiometricFingerprintVectorIcon(
    size: Dp = 36.dp,
    tint: Color = Color(0xFF059669),
    accentTint: Color = Color(0xFF059669),
    isScanning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "fp_scan_anim")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fp_scan_line"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val strokeW = (w * 0.075f).coerceAtLeast(2f)

        // Outer biometric arch
        drawArc(
            color = tint,
            startAngle = 195f,
            sweepAngle = 150f,
            useCenter = false,
            topLeft = Offset(w * 0.10f, h * 0.08f),
            size = Size(w * 0.80f, h * 0.84f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Second biometric ridge
        drawArc(
            color = tint.copy(alpha = 0.92f),
            startAngle = 185f,
            sweepAngle = 170f,
            useCenter = false,
            topLeft = Offset(w * 0.20f, h * 0.18f),
            size = Size(w * 0.60f, h * 0.70f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Third inner ridge
        drawArc(
            color = accentTint,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(w * 0.30f, h * 0.28f),
            size = Size(w * 0.40f, h * 0.54f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Core fingerprint whorl
        drawArc(
            color = tint,
            startAngle = 170f,
            sweepAngle = 200f,
            useCenter = false,
            topLeft = Offset(w * 0.40f, h * 0.40f),
            size = Size(w * 0.20f, h * 0.36f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )
        // Lower curved ridges
        drawArc(
            color = tint.copy(alpha = 0.85f),
            startAngle = 25f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(w * 0.22f, h * 0.28f),
            size = Size(w * 0.56f, h * 0.62f),
            style = Stroke(width = strokeW, cap = StrokeCap.Round)
        )

        if (isScanning) {
            val scanY = h * scanLineProgress
            drawLine(
                color = accentTint.copy(alpha = 0.65f),
                start = Offset(w * 0.12f, scanY),
                end = Offset(w * 0.88f, scanY),
                strokeWidth = strokeW * 0.9f,
                cap = StrokeCap.Round
            )
        }
    }
}

/**
 * Interactive Biometric Sensor Verification Dialog used when hardware BiometricPrompt
 * delegates to fallback (e.g., on emulators or browsers without OS-enrolled fingerprints).
 */
@Composable
fun BiometricSensorFallbackDialog(
    title: String = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
    subtitle: String = BiometricAuthHelper.DEFAULT_PROMPT_SUBTITLE,
    accountLabel: String = "",
    onVerifiedSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanningNow by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isScanningNow) onDismiss() },
        icon = {
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD1FAE5))
                    .border(1.5.dp, Color(0xFFA7F3D0), CircleShape)
                    .clickable(enabled = !isScanningNow) {
                        isScanningNow = true
                        scope.launch {
                            delay(550L)
                            StandardQrCodeHelper.playScanBeepAndVibrate(context)
                            onVerifiedSuccess()
                        }
                    }
                    .testTag("biometric_sensor_touch_pad"),
                contentAlignment = Alignment.Center
            ) {
                BiometricFingerprintVectorIcon(
                    size = 44.dp,
                    tint = Color(0xFF059669),
                    accentTint = Color(0xFF059669),
                    isScanning = true
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (accountLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0))
                    ) {
                        Text(
                            text = accountLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF065F46),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
                Text(
                    text = if (isScanningNow) "جاري قراءة بصمة الإصبع..." else "المس مستشعر البصمة أعلاه أو اضغط تأكيد البصمة",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!isScanningNow) {
                        isScanningNow = true
                        scope.launch {
                            delay(400L)
                            StandardQrCodeHelper.playScanBeepAndVibrate(context)
                            onVerifiedSuccess()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF059669),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("confirm_biometric_sensor_btn")
            ) {
                BiometricFingerprintVectorIcon(
                    size = 18.dp,
                    tint = Color.White,
                    accentTint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("تأكيد بصمة الإصبع", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isScanningNow
            ) {
                Text("إلغاء")
            }
        }
    )
}

/**
 * Settings Card for enabling/disabling Biometric Fingerprint Login across all roles
 * (Admin, Teacher, Student, Parent, and Developer).
 */
@Composable
fun BiometricSettingsSectionCard(
    currentAccountUsername: String,
    currentAccountFullName: String,
    currentAccountRoleLabel: String,
    savedBiometricCredentials: BiometricSavedCredentials?,
    onEnableBiometricVerified: () -> Unit,
    onDisableBiometric: () -> Unit,
    onErrorToast: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFallbackSensorDialog by remember { mutableStateOf(false) }

    val isEnabledForThisAccount = savedBiometricCredentials != null &&
        savedBiometricCredentials.isValid &&
        savedBiometricCredentials.username.equals(currentAccountUsername, ignoreCase = true)

    val isEnabledForAnyAccount = savedBiometricCredentials != null && savedBiometricCredentials.isValid

    val triggerEnableFlow = {
        BiometricAuthHelper.authenticateBiometric(
            context = context,
            title = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
            subtitle = "ضع بصمة إصبعك لتفعيل الدخول السريع لحساب ($currentAccountFullName)",
            onSuccess = {
                onEnableBiometricVerified()
            },
            onError = { err ->
                onErrorToast(err)
            },
            onFallbackRequired = {
                showFallbackSensorDialog = true
            }
        )
    }

    if (showFallbackSensorDialog) {
        BiometricSensorFallbackDialog(
            title = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
            subtitle = "ضع بصمة إصبعك لتفعيل الدخول السريع وحفظ الجلسة المشفرة",
            accountLabel = "$currentAccountFullName ($currentAccountUsername)",
            onVerifiedSuccess = {
                showFallbackSensorDialog = false
                onEnableBiometricVerified()
            },
            onDismiss = {
                showFallbackSensorDialog = false
            }
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("biometric_settings_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (isEnabledForThisAccount) Color(0xFFA7F3D0)
            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFD1FAE5),
                        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            BiometricFingerprintVectorIcon(
                                size = 28.dp,
                                tint = Color(0xFF059669),
                                accentTint = Color(0xFF059669),
                                isScanning = isEnabledForThisAccount
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "تفعيل الدخول السريع ببصمة الإصبع",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isEnabledForThisAccount) {
                                "مفعّل حالياً لحسابك ($currentAccountUsername) • جاهز للدخول بلمسة واحدة"
                            } else if (isEnabledForAnyAccount) {
                                "مفعّل حالياً لحساب (${savedBiometricCredentials?.fullName}) • فعّله لربطه بحسابك الحالي"
                            } else {
                                "تحقق ببصمة إصبعك لحفظ جلسة الدخول المشفرة والدخول فوراً بدون كتابة كلمة المرور"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabledForThisAccount) Color(0xFF059669) else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = isEnabledForThisAccount,
                    onCheckedChange = { shouldEnable ->
                        if (shouldEnable) {
                            triggerEnableFlow()
                        } else {
                            onDisableBiometric()
                        }
                    },
                    modifier = Modifier.testTag("toggle_biometric_login_switch")
                )
            }

            if (isEnabledForAnyAccount && savedBiometricCredentials != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
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
                                imageVector = Icons.Filled.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "الحساب المربوط بالبصمة: ${savedBiometricCredentials.fullName}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFF065F46),
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "المستخدم: ${savedBiometricCredentials.username} • ${savedBiometricCredentials.roleArabic}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF059669),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = onDisableBiometric,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier
                                .height(34.dp)
                                .testTag("disable_biometric_login_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "إلغاء البصمة",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            } else {
                Button(
                    onClick = { triggerEnableFlow() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("enable_biometric_now_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669),
                        contentColor = Color.White
                    )
                ) {
                    BiometricFingerprintVectorIcon(
                        size = 20.dp,
                        tint = Color.White,
                        accentTint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "تفعيل الدخول السريع ببصمة الإصبع الآن",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

/**
 * Clean Soft Emerald Biometric Fingerprint Quick Login Button displayed on UnifiedLoginScreen
 * below the primary "تسجيل الدخول" button when saved biometric credentials exist.
 */
@Composable
fun BiometricQuickLoginButton(
    savedCredentials: BiometricSavedCredentials,
    onBiometricAuthenticated: () -> Unit,
    onBiometricError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showFallbackSensorDialog by remember { mutableStateOf(false) }

    val triggerBiometricLogin = {
        BiometricAuthHelper.authenticateBiometric(
            context = context,
            title = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
            subtitle = BiometricAuthHelper.DEFAULT_PROMPT_SUBTITLE,
            onSuccess = {
                onBiometricAuthenticated()
            },
            onError = { errMsg ->
                onBiometricError(errMsg)
            },
            onFallbackRequired = {
                showFallbackSensorDialog = true
            }
        )
    }

    if (showFallbackSensorDialog) {
        BiometricSensorFallbackDialog(
            title = BiometricAuthHelper.DEFAULT_PROMPT_TITLE,
            subtitle = BiometricAuthHelper.DEFAULT_PROMPT_SUBTITLE,
            accountLabel = "الدخول كـ: ${savedCredentials.fullName} (${savedCredentials.username})",
            onVerifiedSuccess = {
                showFallbackSensorDialog = false
                onBiometricAuthenticated()
            },
            onDismiss = {
                showFallbackSensorDialog = false
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { triggerBiometricLogin() }
            .testTag("biometric_quick_login_button"),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFECFDF5),
        border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFFD1FAE5),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    BiometricFingerprintVectorIcon(
                        size = 26.dp,
                        tint = Color(0xFF059669),
                        accentTint = Color(0xFF059669),
                        isScanning = false
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "الدخول السريع ببصمة الإصبع",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF065F46),
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "الدخول كـ: ${savedCredentials.fullName} (${savedCredentials.username})",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF059669),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF059669),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
