package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
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
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.DeepEmerald
import com.example.ui.theme.DeepForestGreen
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.GoldFoil
import com.example.ui.theme.WhatsAppGreen
import com.example.util.DocumentAndIdExporter
import com.example.util.ExternalIntents
import com.example.util.ImageCompressor
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

/**
 * Global controller so any image across the entire platform (chat, events, honor roll,
 * homework, study materials, student photos, school logo, lost & found) opens in a smooth
 * full-screen interactive Lightbox with pinch-to-zoom, zoom buttons, and 1-tap Save to Device.
 */
object GlobalImageLightboxController {
    var activeImageState by mutableStateOf<Pair<String, String>?>(null)
        private set

    fun open(base64Data: String, title: String = "معاينة الصورة") {
        if (base64Data.isNotBlank()) {
            activeImageState = base64Data to title
        }
    }

    fun close() {
        activeImageState = null
    }
}

/**
 * Global Smooth Interactive Image Lightbox Dialog hosted at the root of the app.
 */
@Composable
fun GlobalImageLightboxDialog() {
    val active = GlobalImageLightboxController.activeImageState ?: return
    val (base64Data, title) = active
    val context = LocalContext.current
    val bitmap = remember(base64Data) {
        ImageCompressor.decodeBase64ToImageBitmap(base64Data)
    }
    var scale by remember(base64Data) { mutableFloatStateOf(1f) }
    var offsetX by remember(base64Data) { mutableFloatStateOf(0f) }
    var offsetY by remember(base64Data) { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = { GlobalImageLightboxController.close() },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .pointerInput(base64Data) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 4.5f)
                        scale = newScale
                        if (newScale > 1f) {
                            offsetX = (offsetX + pan.x).coerceIn(-900f, 900f)
                            offsetY = (offsetY + pan.y).coerceIn(-900f, 900f)
                        } else {
                            offsetX = 0f
                            offsetY = 0f
                        }
                    }
                }
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 72.dp)
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                )
            }

            // Top Bar: Title + Close Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .background(Color.Black.copy(alpha = 0.65f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title.ifBlank { "معاينة الصورة" },
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { GlobalImageLightboxController.close() },
                    modifier = Modifier.testTag("lightbox_close_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "إغلاق",
                        tint = Color.White
                    )
                }
            }

            // Bottom Control Bar: Zoom In / Out / Reset + Save Image to Device
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp),
                shape = RoundedCornerShape(50),
                color = Color(0xFF064E3B).copy(alpha = 0.94f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = {
                            scale = (scale - 0.4f).coerceAtLeast(1f)
                            if (scale <= 1f) {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomOut,
                            contentDescription = "تصغير",
                            tint = Color.White
                        )
                    }
                    Text(
                        text = "${(scale * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { scale = (scale + 0.4f).coerceAtMost(4.5f) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ZoomIn,
                            contentDescription = "تكبير",
                            tint = Color.White
                        )
                    }
                    if (scale > 1.05f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CenterFocusStrong,
                                contentDescription = "إعادة ضبط الحجم",
                                tint = GoldAccent
                            )
                        }
                    }

                    Button(
                        onClick = {
                            DocumentAndIdExporter.saveBase64ImageToGallery(
                                context = context,
                                base64Data = base64Data,
                                titlePrefix = title.ifBlank { "Madrasati_Image" }
                            )
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldAccent,
                            contentColor = Color(0xFF052E16)
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.testTag("lightbox_save_image_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "حفظ الصورة في الجهاز",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single-line Platform Footer Bar at the bottom of Onboarding, Login, and Settings
 * (Cleaned of any hardcoded phone number).
 */
@Composable
fun DeveloperCreditFooter(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("developer_credit_footer"),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = "الدعم الفني للمنصة",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "الدعم الفني للمنصة التعليمية — نظام الإدارة السحابية المتكاملة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Smart 11-Digit Iraqi Telecom Carrier Validator & Interactive Phone Input Field
 * (078 Zain Iraq, 077 Asiacell, 075 Korek with explicit Override Toggle).
 */
object IraqiPhoneCarrierValidator {
    fun sanitizeDigits(raw: String): String {
        val cleaned = raw.trim().let {
            if (com.example.util.SecurityShieldHelper.isLegacyPlaceholderPhone(it)) "" else it
        }
        return cleaned.filter { it.isDigit() }.take(11)
    }

    fun isKorekNumber(phone: String): Boolean {
        return sanitizeDigits(phone).startsWith("075")
    }

    /**
     * Validates a phone number before saving.
     * - Blocks saving if non-empty and not 11 digits.
     * - Blocks saving if starting with 075 (Korek) unless [korekOverrideApproved] is true.
     */
    fun validateForSave(
        phone: String,
        korekOverrideApproved: Boolean,
        allowEmpty: Boolean = true
    ): Pair<Boolean, String> {
        val digits = sanitizeDigits(phone)
        if (digits.isEmpty()) {
            return if (allowEmpty) {
                true to ""
            } else {
                false to "يرجى إدخال رقم الهاتف المكون من 11 رقماً (مثال: 078XXXXXXXX)"
            }
        }
        if (digits.length != 11) {
            return false to "يجب أن يتكون رقم الهاتف من 11 رقماً بالضبط (المدخل حالياً: ${digits.length} أرقام)"
        }
        if (!digits.startsWith("07")) {
            return false to "يجب أن يبدأ رقم الهاتف العراقي بـ 078 أو 077 أو 075 ويتكون من 11 رقماً"
        }
        if (digits.startsWith("075") && !korekOverrideApproved) {
            return false to "تنبيه: هذا الرقم تابع لشركة كورك (075) ولا يمكن اعتماده إلا بالضغط على زر (تجاوز المنع واعتماد الرقم استثنائياً)"
        }
        return true to ""
    }
}

@Composable
fun SmartIraqiPhoneInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String = "رقم الهاتف (11 رقماً)",
    modifier: Modifier = Modifier,
    korekOverrideApproved: Boolean = false,
    onKorekOverrideChange: ((Boolean) -> Unit)? = null,
    testTag: String = "smart_iraqi_phone_input"
) {
    val cleanDigits = remember(value) { IraqiPhoneCarrierValidator.sanitizeDigits(value) }
    var internalKorekOverride by remember { mutableStateOf(korekOverrideApproved) }
    val effectiveKorekOverride = if (onKorekOverrideChange != null) korekOverrideApproved else internalKorekOverride

    val prefix3 = cleanDigits.take(3)
    val isZain = prefix3 == "078"
    val isAsiacell = prefix3 == "077"
    val isKorek = prefix3 == "075"
    val isComplete11 = cleanDigits.length == 11

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = cleanDigits,
            onValueChange = { rawInput ->
                val filtered = rawInput.filter { it.isDigit() }.take(11)
                if (!filtered.startsWith("075")) {
                    internalKorekOverride = false
                    onKorekOverrideChange?.invoke(false)
                }
                onValueChange(filtered)
            },
            label = { Text(label) },
            placeholder = {
                Text(
                    text = "يجب أن يتكون رقم الهاتف من 11 رقماً (مثال: 078XXXXXXXX)",
                    style = MaterialTheme.typography.labelSmall
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.PhoneIphone,
                    contentDescription = null,
                    tint = when {
                        isKorek && !effectiveKorekOverride -> Color(0xFFDC2626)
                        isZain || isAsiacell -> DeepEmerald
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
            },
            trailingIcon = {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isComplete11) DeepEmerald.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = "${cleanDigits.length}/11",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isComplete11) DeepEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            },
            isError = (cleanDigits.isNotEmpty() && cleanDigits.length != 11) || (isKorek && !effectiveKorekOverride),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )

        // رسالة التوجيه الأساسية لـ 11 رقماً
        Text(
            text = if (cleanDigits.isEmpty() || cleanDigits.length == 11) {
                "يجب أن يتكون رقم الهاتف من 11 رقماً (مثال: 078XXXXXXXX)"
            } else {
                "يجب أن يتكون رقم الهاتف من 11 رقماً — متبقي ${11 - cleanDigits.length} أرقام (مثال: 078XXXXXXXX)"
            },
            style = MaterialTheme.typography.labelSmall,
            color = if (cleanDigits.isNotEmpty() && cleanDigits.length != 11) {
                Color(0xFFB45309)
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            fontWeight = FontWeight.SemiBold
        )

        // الفحص اللحظي لشركات الاتصالات العراقية (أول 3 أرقام: 078 / 077 / 075)
        when {
            isZain -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTag}_zain_badge"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFECFDF5),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Verified,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "خط شركة زين العراق (Zain) — معتمد (11 رقماً)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }

            isAsiacell -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTag}_asiacell_badge"),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFF0FDFA),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.55f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF0D9488),
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "خط شركة آسياسيل (Asiacell) — معتمد (11 رقماً)",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF115E59)
                        )
                    }
                }
            }

            isKorek -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("${testTag}_korek_warning"),
                    shape = RoundedCornerShape(12.dp),
                    color = if (effectiveKorekOverride) Color(0xFFFFFBEB) else Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        if (effectiveKorekOverride) Color(0xFFF59E0B) else Color(0xFFEF4444)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (effectiveKorekOverride) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber,
                                contentDescription = null,
                                tint = if (effectiveKorekOverride) Color(0xFFD97706) else Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (effectiveKorekOverride) {
                                    "تم تفعيل الاستثناء اليدوي لاعتماد رقم شركة كورك (Korek - 075)"
                                } else {
                                    "تنبيه: هذا الرقم تابع لشركة كورك (Korek) ولا يمكن اعتماد خطها حالياً في النظام"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (effectiveKorekOverride) Color(0xFF92400E) else Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    val nextState = !effectiveKorekOverride
                                    internalKorekOverride = nextState
                                    onKorekOverrideChange?.invoke(nextState)
                                }
                                .testTag("${testTag}_korek_override_btn"),
                            shape = RoundedCornerShape(8.dp),
                            color = if (effectiveKorekOverride) Color(0xFF059669) else Color(0xFFB45309)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (effectiveKorekOverride) Icons.Filled.VerifiedUser else Icons.Filled.LockOpen,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (effectiveKorekOverride) {
                                        "تم تجاوز المنع واعتماد الرقم استثنائياً (اضغط لإلغاء التجاوز)"
                                    } else {
                                        "تجاوز المنع واعتماد الرقم استثنائياً"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
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

/**
 * Vector Badge with white-space: nowrap equivalent (maxLines = 1, softWrap = false).
 */
@Composable
fun NoWrapBadge(
    text: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = containerColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = text,
                tint = contentColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                color = contentColor,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Vector Action Button with white-space: nowrap equivalent (maxLines = 1, softWrap = false).
 */
@Composable
fun VectorActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    testTag: String = "",
    isLoading: Boolean = false
) {
    val cloudWriteInFlight by com.example.data.FirebaseCloudSync.isCloudWriteInFlight.collectAsState()
    var triggeredByThisButton by remember { mutableStateOf(false) }

    LaunchedEffect(cloudWriteInFlight, triggeredByThisButton) {
        if (triggeredByThisButton) {
            if (!cloudWriteInFlight) {
                kotlinx.coroutines.delay(220)
                if (!com.example.data.FirebaseCloudSync.isCloudWriteInFlight.value) {
                    triggeredByThisButton = false
                }
            }
        }
    }

    val showSpinner = isLoading || (triggeredByThisButton && cloudWriteInFlight)

    Button(
        onClick = {
            if (!showSpinner) {
                triggeredByThisButton = true
                onClick()
            }
        },
        enabled = enabled && !showSpinner,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = if (showSpinner) containerColor.copy(alpha = 0.85f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
            disabledContentColor = if (showSpinner) contentColor else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        if (showSpinner) {
            LuxuryVectorButtonSpinner(tint = contentColor, modifier = Modifier.size(18.dp))
        } else {
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (showSpinner) "جاري الحفظ..." else text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LuxuryVectorButtonSpinner(
    tint: Color = Color.White,
    modifier: Modifier = Modifier.size(18.dp)
) {
    val transition = androidx.compose.animation.core.rememberInfiniteTransition(label = "btn_spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = androidx.compose.animation.core.tween(
                durationMillis = 850,
                easing = androidx.compose.animation.core.LinearEasing
            )
        ),
        label = "btn_spinner_angle"
    )
    androidx.compose.foundation.Canvas(modifier = modifier) {
        val stroke = 2.2.dp.toPx()
        drawCircle(
            color = tint.copy(alpha = 0.25f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        )
        drawArc(
            color = tint,
            startAngle = angle,
            sweepAngle = 250f,
            useCenter = false,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = stroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

/**
 * Photo Picker Button that automatically compresses any chosen image via Canvas to
 * max 800px (or 400px for avatars) and 0.75 JPEG quality Base64.
 */
@Composable
fun CompressedPhotoPickerButton(
    label: String,
    currentBase64: String,
    onImageCompressed: (String) -> Unit,
    modifier: Modifier = Modifier,
    isAvatar: Boolean = label.contains("شخصية") || label.contains("شعار") || label.contains("الطالب") || label.contains("المعلم")
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val compressed = ImageCompressor.compressUriToBase64Jpeg(
                context = context,
                uri = uri,
                isAvatar = isAvatar
            )
            if (compressed != null) {
                onImageCompressed(compressed)
            }
        }
    }

    OutlinedButton(
        onClick = {
            launcher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(
            imageVector = if (currentBase64.isNotEmpty()) Icons.Filled.CheckCircle else Icons.Filled.AddPhotoAlternate,
            contentDescription = label,
            tint = if (currentBase64.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (currentBase64.isNotEmpty()) "تم اختيار الصورة • $label" else label,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun CompressedPhotoPickerButton(
    label: String,
    hasImage: Boolean,
    onPhotoSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isAvatar: Boolean = label.contains("شخصية") || label.contains("شعار") || label.contains("الطالب") || label.contains("المعلم")
) {
    CompressedPhotoPickerButton(
        label = label,
        currentBase64 = if (hasImage) "selected" else "",
        onImageCompressed = onPhotoSelected,
        modifier = modifier,
        isAvatar = isAvatar
    )
}

/**
 * Renders a Base64 compressed image or a clean vector fallback avatar/icon.
 * Tapping any non-empty Base64 image opens it in the Global Smooth Image Lightbox.
 */
@Composable
fun Base64AvatarImage(
    base64Data: String?,
    fallbackIcon: ImageVector,
    contentDescription: String,
    size: Dp = 52.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val safeData = base64Data.orEmpty()
    val bitmap = remember(safeData) {
        ImageCompressor.decodeBase64ToImageBitmap(safeData, maxDimension = 240)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.5.dp, tint.copy(alpha = 0.4f), CircleShape)
            .then(
                if (bitmap != null && safeData.isNotBlank()) {
                    Modifier.clickable {
                        GlobalImageLightboxController.open(safeData, contentDescription)
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(size * 0.54f)
            )
        }
    }
}

/**
 * Real Choreographed Multi-Element Sequential Vector Morphing Animation (3-Stage Staggered Choreography):
 * - Stage 1 (0.0s - 0.5s): Hands of Giving & Emerald Book of Knowledge pages draw via stroke segments
 *   and unfold independently from the spine with page rotation, horizontal scaling, and knowledge lines.
 * - Stage 2 (0.4s - 0.9s): Golden Laurel Wreath stems grow upward and 12 golden leaves (6 right, 6 left)
 *   sprout and unfold one leaf after another in an organic wave.
 * - Stage 3 (0.7s - 1.3s): Royal Golden Graduation Cap descends from above into the heart of the wreath
 *   with a real damped pendulum swing on the golden tassel and a diagonal light shimmer pass.
 */
@Composable
fun ChoreographedRoyalEmblemAnimation(
    modifier: Modifier = Modifier,
    size: Dp = 228.dp,
    replayKey: Any = Unit
) {
    val elapsedSec = remember(replayKey) { Animatable(0f) }
    LaunchedEffect(replayKey) {
        elapsedSec.snapTo(0f)
        elapsedSec.animateTo(
            targetValue = 1.45f,
            animationSpec = tween(durationMillis = 1450, easing = LinearEasing)
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "emblem_continuous_physics")
    val continuousWave by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_wave"
    )
    val shimmerSweepLoop by infiniteTransition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_sweep_loop"
    )

    val t = elapsedSec.value

    // Stage 1 (0.0s - 0.5s): Book pages & Hands of Giving stroke draw + page unfolding
    val stage1Raw = (t / 0.50f).coerceIn(0f, 1f)
    val bookProgress = FastOutSlowInEasing.transform(stage1Raw)

    // Stage 2 (0.4s - 0.9s): Golden Laurel stems + staggered 6 pairs of leaves
    val stemRaw = ((t - 0.38f) / 0.36f).coerceIn(0f, 1f)
    val stemProgress = FastOutSlowInEasing.transform(stemRaw)

    // Stage 3 (0.7s - 1.3s): Graduation Cap drop + Pendulum Tassel Swing + Shimmer Pass
    val capRaw = ((t - 0.68f) / 0.42f).coerceIn(0f, 1f)
    // Spring overshoot curve for the cap landing
    val capLandingEase = if (capRaw < 1f) {
        val u = 1f - capRaw
        1f - u * u * (2.6f * u - 1.6f)
    } else 1f
    val capAlpha = ((t - 0.68f) / 0.18f).coerceIn(0f, 1f)

    // Real Damped Pendulum Physics for the Golden Tassel (starts swinging as cap lands at t=0.72s)
    val tasselTime = (t - 0.72f).coerceAtLeast(0f)
    val dampedImpulseDeg = if (tasselTime > 0f) {
        (34f * exp(-2.4f * tasselTime) * sin(10.5f * tasselTime))
    } else 0f
    val ambientPendulumDeg = if (t >= 1.25f) {
        (5.5f * sin(continuousWave.toDouble())).toFloat()
    } else 0f
    val totalTasselSwingDeg = dampedImpulseDeg + ambientPendulumDeg

    // Initial shimmer pass (0.82s - 1.38s), then continuous soft shimmer pass
    val activeShimmerPos = if (t < 1.42f) {
        ((t - 0.80f) / 0.58f).coerceIn(-0.4f, 1.5f)
    } else {
        shimmerSweepLoop
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(36.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF032B22),
                        DeepEmerald,
                        Color(0xFF0D9488)
                    )
                )
            )
            .border(
                width = 1.6.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        GoldAccent.copy(alpha = 0.75f),
                        Color(0xFFA7F3D0).copy(alpha = 0.35f),
                        GoldFoil.copy(alpha = 0.75f)
                    )
                ),
                shape = RoundedCornerShape(36.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = this.size.width
            val h = this.size.height
            val cx = w / 2f
            val cy = h / 2f
            val unit = this.size.minDimension / 240f
            val pathMeasure = PathMeasure()

            // Subtle ambient royal halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.20f * bookProgress),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.46f
                ),
                radius = w * 0.46f,
                center = Offset(cx, cy)
            )

            // =====================================================================
            // STAGE 1 (0.0s - 0.5s): Hands of Giving & Open Emerald Book of Knowledge
            // =====================================================================
            if (bookProgress > 0.01f) {
                val spineBottomY = cy + 44f * unit
                val spineTopY = cy + 6f * unit

                // Cupped Hands of Giving below the book (drawn progressively with PathMeasure)
                val leftHandPath = Path().apply {
                    moveTo(cx - 4f * unit, spineBottomY + 18f * unit)
                    cubicTo(
                        cx - 28f * unit, spineBottomY + 20f * unit,
                        cx - 56f * unit, spineBottomY + 6f * unit,
                        cx - 66f * unit, spineBottomY - 12f * unit
                    )
                    cubicTo(
                        cx - 48f * unit, spineBottomY - 2f * unit,
                        cx - 26f * unit, spineBottomY + 8f * unit,
                        cx - 4f * unit, spineBottomY + 10f * unit
                    )
                }
                val rightHandPath = Path().apply {
                    moveTo(cx + 4f * unit, spineBottomY + 18f * unit)
                    cubicTo(
                        cx + 28f * unit, spineBottomY + 20f * unit,
                        cx + 56f * unit, spineBottomY + 6f * unit,
                        cx + 66f * unit, spineBottomY - 12f * unit
                    )
                    cubicTo(
                        cx + 48f * unit, spineBottomY - 2f * unit,
                        cx + 26f * unit, spineBottomY + 8f * unit,
                        cx + 4f * unit, spineBottomY + 10f * unit
                    )
                }

                val leftHandSeg = Path()
                pathMeasure.setPath(leftHandPath, false)
                pathMeasure.getSegment(0f, pathMeasure.length * bookProgress, leftHandSeg, true)
                drawPath(
                    path = leftHandSeg,
                    color = Color(0xFFA7F3D0),
                    style = Stroke(width = 3.2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                val rightHandSeg = Path()
                pathMeasure.setPath(rightHandPath, false)
                pathMeasure.getSegment(0f, pathMeasure.length * bookProgress, rightHandSeg, true)
                drawPath(
                    path = rightHandSeg,
                    color = Color(0xFFA7F3D0),
                    style = Stroke(width = 3.2f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Left & Right Book Pages unfolding from the center spine
                val pageScaleX = 0.22f + 0.78f * bookProgress
                val leftPageRot = (1f - bookProgress) * 18f
                val rightPageRot = -(1f - bookProgress) * 18f

                // Left Page Transform (Unfolding)
                withTransform({
                    rotate(degrees = leftPageRot, pivot = Offset(cx, spineBottomY))
                    scale(scaleX = pageScaleX, scaleY = bookProgress, pivot = Offset(cx, spineBottomY))
                }) {
                    val leftPage = Path().apply {
                        moveTo(cx, spineTopY)
                        cubicTo(
                            cx - 22f * unit, spineTopY - 12f * unit,
                            cx - 46f * unit, spineTopY - 10f * unit,
                            cx - 60f * unit, spineTopY - 2f * unit
                        )
                        lineTo(cx - 56f * unit, spineBottomY - 6f * unit)
                        cubicTo(
                            cx - 42f * unit, spineBottomY - 12f * unit,
                            cx - 20f * unit, spineBottomY - 10f * unit,
                            cx, spineBottomY
                        )
                        close()
                    }
                    drawPath(
                        path = leftPage,
                        color = Color(0xFF065F46).copy(alpha = 0.85f * bookProgress)
                    )
                    val leftStrokeSeg = Path()
                    pathMeasure.setPath(leftPage, true)
                    pathMeasure.getSegment(0f, pathMeasure.length * bookProgress, leftStrokeSeg, true)
                    drawPath(
                        path = leftStrokeSeg,
                        color = Color(0xFFFEF3C7),
                        style = Stroke(width = 2.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Inner knowledge lines on left page
                    val lineProg = ((bookProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    if (lineProg > 0f) {
                        for (row in 0..2) {
                            val ly = spineTopY + (8f + row * 9f) * unit
                            drawLine(
                                color = GoldAccent.copy(alpha = 0.85f * lineProg),
                                start = Offset(cx - 10f * unit, ly - 3f * unit),
                                end = Offset(cx - (10f + 34f * lineProg) * unit, ly - (3f + 2f * lineProg) * unit),
                                strokeWidth = 2f * unit,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }

                // Right Page Transform (Unfolding)
                withTransform({
                    rotate(degrees = rightPageRot, pivot = Offset(cx, spineBottomY))
                    scale(scaleX = pageScaleX, scaleY = bookProgress, pivot = Offset(cx, spineBottomY))
                }) {
                    val rightPage = Path().apply {
                        moveTo(cx, spineTopY)
                        cubicTo(
                            cx + 22f * unit, spineTopY - 12f * unit,
                            cx + 46f * unit, spineTopY - 10f * unit,
                            cx + 60f * unit, spineTopY - 2f * unit
                        )
                        lineTo(cx + 56f * unit, spineBottomY - 6f * unit)
                        cubicTo(
                            cx + 42f * unit, spineBottomY - 12f * unit,
                            cx + 20f * unit, spineBottomY - 10f * unit,
                            cx, spineBottomY
                        )
                        close()
                    }
                    drawPath(
                        path = rightPage,
                        color = Color(0xFF047857).copy(alpha = 0.85f * bookProgress)
                    )
                    val rightStrokeSeg = Path()
                    pathMeasure.setPath(rightPage, true)
                    pathMeasure.getSegment(0f, pathMeasure.length * bookProgress, rightStrokeSeg, true)
                    drawPath(
                        path = rightStrokeSeg,
                        color = Color(0xFFFEF3C7),
                        style = Stroke(width = 2.6f * unit, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Inner knowledge lines on right page
                    val lineProg = ((bookProgress - 0.35f) / 0.65f).coerceIn(0f, 1f)
                    if (lineProg > 0f) {
                        for (row in 0..2) {
                            val ly = spineTopY + (8f + row * 9f) * unit
                            drawLine(
                                color = GoldAccent.copy(alpha = 0.85f * lineProg),
                                start = Offset(cx + 10f * unit, ly - 3f * unit),
                                end = Offset(cx + (10f + 34f * lineProg) * unit, ly - (3f + 2f * lineProg) * unit),
                                strokeWidth = 2f * unit,
                                cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // STAGE 2 (0.4s - 0.9s): Golden Laurel Branches & Leaf-by-Leaf Unfolding
            // =====================================================================
            if (stemProgress > 0.01f) {
                val leftStemPath = Path().apply {
                    moveTo(cx - 10f * unit, cy + 74f * unit)
                    cubicTo(
                        cx - 78f * unit, cy + 62f * unit,
                        cx - 98f * unit, cy - 10f * unit,
                        cx - 48f * unit, cy - 72f * unit
                    )
                }
                val rightStemPath = Path().apply {
                    moveTo(cx + 10f * unit, cy + 74f * unit)
                    cubicTo(
                        cx + 78f * unit, cy + 62f * unit,
                        cx + 98f * unit, cy - 10f * unit,
                        cx + 48f * unit, cy - 72f * unit
                    )
                }

                val leftStemSeg = Path()
                pathMeasure.setPath(leftStemPath, false)
                val leftStemLen = pathMeasure.length
                pathMeasure.getSegment(0f, leftStemLen * stemProgress, leftStemSeg, true)
                drawPath(
                    path = leftStemSeg,
                    color = GoldAccent,
                    style = Stroke(width = 3.2f * unit, cap = StrokeCap.Round)
                )

                val rightStemSeg = Path()
                pathMeasure.setPath(rightStemPath, false)
                val rightStemLen = pathMeasure.length
                pathMeasure.getSegment(0f, rightStemLen * stemProgress, rightStemSeg, true)
                drawPath(
                    path = rightStemSeg,
                    color = GoldAccent,
                    style = Stroke(width = 3.2f * unit, cap = StrokeCap.Round)
                )

                // 6 Pairs of Golden Leaves unfolding sequentially from bottom to top (0.42s .. 0.90s)
                val leafCount = 6
                for (i in 0 until leafCount) {
                    val leafStartT = 0.42f + i * 0.065f
                    val rawLeaf = ((t - leafStartT) / 0.22f).coerceIn(0f, 1f)
                    if (rawLeaf > 0.01f) {
                        // Overshoot organic leaf unfolding
                        val u = 1f - rawLeaf
                        val leafScale = (1f - u * u * (2.3f * u - 1.3f)).coerceAtLeast(0f)
                        val breezeDeg = if (t >= 0.95f) {
                            (3.2f * sin(continuousWave.toDouble() + i * 0.7)).toFloat()
                        } else 0f
                        val unfoldRot = (1f - rawLeaf) * 42f + breezeDeg

                        val fraction = (i + 1) / (leafCount + 1f)

                        // Left branch leaf position
                        pathMeasure.setPath(leftStemPath, false)
                        val leftPos = pathMeasure.getPosition(leftStemLen * fraction)
                        withTransform({
                            rotate(
                                degrees = -35f - i * 12f - unfoldRot,
                                pivot = leftPos
                            )
                            scale(scaleX = leafScale, scaleY = leafScale, pivot = leftPos)
                        }) {
                            val leafPath = Path().apply {
                                moveTo(leftPos.x, leftPos.y)
                                quadraticTo(
                                    leftPos.x - 14f * unit, leftPos.y - 10f * unit,
                                    leftPos.x - 4f * unit, leftPos.y - 24f * unit
                                )
                                quadraticTo(
                                    leftPos.x + 8f * unit, leftPos.y - 10f * unit,
                                    leftPos.x, leftPos.y
                                )
                                close()
                            }
                            drawPath(
                                path = leafPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFDE68A), GoldAccent),
                                    start = leftPos,
                                    end = Offset(leftPos.x - 10f * unit, leftPos.y - 24f * unit)
                                )
                            )
                        }

                        // Right branch leaf position
                        pathMeasure.setPath(rightStemPath, false)
                        val rightPos = pathMeasure.getPosition(rightStemLen * fraction)
                        withTransform({
                            rotate(
                                degrees = 35f + i * 12f + unfoldRot,
                                pivot = rightPos
                            )
                            scale(scaleX = leafScale, scaleY = leafScale, pivot = rightPos)
                        }) {
                            val leafPath = Path().apply {
                                moveTo(rightPos.x, rightPos.y)
                                quadraticTo(
                                    rightPos.x + 14f * unit, rightPos.y - 10f * unit,
                                    rightPos.x + 4f * unit, rightPos.y - 24f * unit
                                )
                                quadraticTo(
                                    rightPos.x - 8f * unit, rightPos.y - 10f * unit,
                                    rightPos.x, rightPos.y
                                )
                                close()
                            }
                            drawPath(
                                path = leafPath,
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFFDE68A), GoldAccent),
                                    start = rightPos,
                                    end = Offset(rightPos.x + 10f * unit, rightPos.y - 24f * unit)
                                )
                            )
                        }
                    }
                }
            }

            // =====================================================================
            // STAGE 3 (0.7s - 1.3s): Descending Golden Graduation Cap & Pendulum Tassel
            // =====================================================================
            if (capAlpha > 0.01f) {
                val capTargetCy = cy - 34f * unit
                val dropOffsetY = (1f - capLandingEase) * (-68f * unit)
                val capCy = capTargetCy + dropOffsetY
                val capScale = (0.72f + 0.28f * capLandingEase).coerceAtLeast(0.1f)

                withTransform({
                    scale(scaleX = capScale, scaleY = capScale, pivot = Offset(cx, capCy))
                }) {
                    // Royal Graduation Cap Skullcap Base
                    val capBasePath = Path().apply {
                        moveTo(cx - 28f * unit, capCy + 4f * unit)
                        lineTo(cx - 24f * unit, capCy + 20f * unit)
                        quadraticTo(
                            cx, capCy + 28f * unit,
                            cx + 24f * unit, capCy + 20f * unit
                        )
                        lineTo(cx + 28f * unit, capCy + 4f * unit)
                        close()
                    }
                    drawPath(
                        path = capBasePath,
                        color = GoldDeep.copy(alpha = capAlpha)
                    )
                    drawPath(
                        path = capBasePath,
                        color = Color(0xFFFEF3C7).copy(alpha = capAlpha),
                        style = Stroke(width = 1.8f * unit, join = StrokeJoin.Round)
                    )

                    // Mortarboard Diamond Top
                    val diamondPath = Path().apply {
                        moveTo(cx, capCy - 18f * unit)
                        lineTo(cx + 50f * unit, capCy - 2f * unit)
                        lineTo(cx, capCy + 14f * unit)
                        lineTo(cx - 50f * unit, capCy - 2f * unit)
                        close()
                    }
                    drawPath(
                        path = diamondPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFEF3C7).copy(alpha = capAlpha),
                                GoldAccent.copy(alpha = capAlpha),
                                GoldDeep.copy(alpha = capAlpha)
                            ),
                            start = Offset(cx - 50f * unit, capCy - 18f * unit),
                            end = Offset(cx + 50f * unit, capCy + 14f * unit)
                        )
                    )
                    drawPath(
                        path = diamondPath,
                        color = Color.White.copy(alpha = 0.85f * capAlpha),
                        style = Stroke(width = 2.2f * unit, join = StrokeJoin.Round)
                    )

                    // Center Golden Button Pivot on Mortarboard
                    val pivotX = cx
                    val pivotY = capCy - 2f * unit
                    drawCircle(
                        color = Color(0xFFFEF3C7).copy(alpha = capAlpha),
                        radius = 4.2f * unit,
                        center = Offset(pivotX, pivotY)
                    )

                    // Real Physical Pendulum Tassel Swing
                    val tasselAnchorX = cx + 34f * unit
                    val tasselAnchorY = capCy + 4f * unit
                    // Cord from center pivot to right edge of cap
                    drawLine(
                        color = Color(0xFFFEF3C7).copy(alpha = capAlpha),
                        start = Offset(pivotX, pivotY),
                        end = Offset(tasselAnchorX, tasselAnchorY),
                        strokeWidth = 2.2f * unit,
                        cap = StrokeCap.Round
                    )

                    // Hanging Pendulum Tassel swinging around tasselAnchor
                    withTransform({
                        rotate(
                            degrees = totalTasselSwingDeg,
                            pivot = Offset(tasselAnchorX, tasselAnchorY)
                        )
                    }) {
                        val tasselEndY = tasselAnchorY + 26f * unit
                        drawLine(
                            color = GoldAccent.copy(alpha = capAlpha),
                            start = Offset(tasselAnchorX, tasselAnchorY),
                            end = Offset(tasselAnchorX, tasselEndY),
                            strokeWidth = 2.5f * unit,
                            cap = StrokeCap.Round
                        )
                        // Golden Tassel Fringe Bell
                        val fringePath = Path().apply {
                            moveTo(tasselAnchorX - 5f * unit, tasselEndY + 9f * unit)
                            lineTo(tasselAnchorX + 5f * unit, tasselEndY + 9f * unit)
                            lineTo(tasselAnchorX + 2.5f * unit, tasselEndY - 1f * unit)
                            lineTo(tasselAnchorX - 2.5f * unit, tasselEndY - 1f * unit)
                            close()
                        }
                        drawPath(
                            path = fringePath,
                            color = Color(0xFFFDE68A).copy(alpha = capAlpha)
                        )
                        drawCircle(
                            color = GoldAccent.copy(alpha = capAlpha),
                            radius = 3.2f * unit,
                            center = Offset(tasselAnchorX, tasselEndY)
                        )
                    }
                }
            }

            // =====================================================================
            // SHIMMER PASS: Diagonal Light Sweep Across the Formed Emblem
            // =====================================================================
            if (activeShimmerPos in -0.3f..1.4f) {
                val bandCenter = w * activeShimmerPos
                val bandWidth = w * 0.35f
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.White.copy(alpha = 0.24f),
                            Color(0xFFFEF3C7).copy(alpha = 0.32f),
                            Color.Transparent
                        ),
                        start = Offset(bandCenter - bandWidth, 0f),
                        end = Offset(bandCenter + bandWidth, h)
                    ),
                    size = this.size
                )
            }
        }
    }
}

/**
 * Full-Screen Choreographed Splash Screen (شاشة تشغيل التطبيق الترحيبية):
 * - Plays the 3-stage multi-part vector formation animation immediately on launch.
 * - Automatically displays the discovered school name & logo beneath the animation if a school
 *   is active or saved in the user's session; otherwise displays only the official platform title.
 */
@Composable
fun ChoreographedSplashScreen(
    discoveredSchoolName: String?,
    schoolLogoBase64: String,
    isDarkMode: Boolean,
    onSplashFinished: () -> Unit
) {
    val textRevealProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(520)
        textRevealProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
        kotlinx.coroutines.delay(1050)
        onSplashFinished()
    }

    val reveal = textRevealProgress.value
    val hasDiscoveredSchool = !discoveredSchoolName.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (isDarkMode) {
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
            .clickable { onSplashFinished() }
            .testTag("choreographed_splash_screen"),
        contentAlignment = Alignment.Center
    ) {
        // Soft ambient radial glows
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF10B981).copy(alpha = if (isDarkMode) 0.18f else 0.12f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.82f, size.height * 0.18f),
                    radius = size.minDimension * 0.65f
                ),
                center = Offset(size.width * 0.82f, size.height * 0.18f),
                radius = size.minDimension * 0.65f
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldAccent.copy(alpha = if (isDarkMode) 0.14f else 0.10f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.18f, size.height * 0.82f),
                    radius = size.minDimension * 0.60f
                ),
                center = Offset(size.width * 0.18f, size.height * 0.82f),
                radius = size.minDimension * 0.60f
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
        ) {
            // 3-Stage Choreographed Multi-Element Vector Emblem Animation
            ChoreographedRoyalEmblemAnimation(size = 224.dp)

            Spacer(modifier = Modifier.height(24.dp))

            // Minimalist Luxury Typography for Platform Title & Discovered School Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .graphicsLayer(
                        alpha = reveal.coerceIn(0f, 1f),
                        scaleX = 0.88f + 0.12f * reveal,
                        scaleY = 0.88f + 0.12f * reveal,
                        translationY = (1f - reveal) * 28f
                    )
                    .testTag("splash_discovered_school_card")
            ) {
                Text(
                    text = "منصة مدرستي الذكية",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = if (isDarkMode) Color(0xFF6EE7B7) else Color(0xFF047857),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .padding(vertical = 6.dp)
                        .width(48.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFF59E0B),
                                    Color.Transparent
                                )
                            )
                        )
                )

                if (hasDiscoveredSchool) {
                    Text(
                        text = discoveredSchoolName.orEmpty(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF022C22),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "منصة مدرستي الذكية المتكاملة",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else Color(0xFF022C22),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "بيئة تعليمية وإدارية سحابية فائقة الفخامة",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isDarkMode) Color.White.copy(alpha = 0.80f) else MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Interactive Choreographed Vector Scene for the 3 Onboarding Slides.
 * Uses the real 3-stage multi-element ChoreographedRoyalEmblemAnimation instead of a rigid up/down block.
 */
@Composable
fun OnboardingAnimatedScene(
    slideIndex: Int,
    modifier: Modifier = Modifier
) {
    ChoreographedRoyalEmblemAnimation(
        modifier = modifier,
        size = 220.dp,
        replayKey = slideIndex
    )
}

/**
 * Custom Golden Vector Medal Emblem for the Honor Roll (لوحة الشرف).
 */
@Composable
fun GoldenVectorMedal(
    rank: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.minDimension * 0.44f

            // Ribbons at bottom
            val ribbonPath = Path().apply {
                moveTo(cx - 12f, cy + outerR * 0.5f)
                lineTo(cx - 20f, size.height)
                lineTo(cx - 6f, size.height - 8f)
                lineTo(cx - 2f, cy + outerR * 0.7f)
                close()

                moveTo(cx + 12f, cy + outerR * 0.5f)
                lineTo(cx + 20f, size.height)
                lineTo(cx + 6f, size.height - 8f)
                lineTo(cx + 2f, cy + outerR * 0.7f)
                close()
            }
            drawPath(ribbonPath, color = Color(0xFF047857))

            // Starburst Laurel Points
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val sx = cx + (outerR * 1.06f * cos(angle)).toFloat()
                val sy = cy + (outerR * 1.06f * sin(angle)).toFloat()
                drawCircle(
                    color = Color(0xFF10B981),
                    radius = 5f,
                    center = Offset(sx, sy)
                )
            }

            // Main Emerald Medallion
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857))
                ),
                radius = outerR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.75f),
                radius = outerR * 0.78f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Interactive Heart Reaction Button for Golden Honor Roll (لوحة الشرف الذهبية) and Events Gallery (معرض الفعاليات)
 * with single-reaction-per-user toggle (`isLikedByMe`), live counter, elastic heart pulse, and floating mini-hearts & golden sparkles animation.
 */
@Composable
fun HonorHeartReactionButton(
    heartCount: Int,
    isLikedByMe: Boolean,
    onReact: () -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = "تشجيع",
    testTag: String = ""
) {
    var burstTrigger by remember { mutableIntStateOf(0) }
    val heartScale = remember { Animatable(1f) }
    val burstProgress = remember { Animatable(1f) }

    LaunchedEffect(burstTrigger) {
        if (burstTrigger > 0) {
            launch {
                heartScale.snapTo(0.8f)
                heartScale.animateTo(
                    targetValue = 1.38f,
                    animationSpec = tween(durationMillis = 160, easing = FastOutSlowInEasing)
                )
                heartScale.animateTo(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
            }
            launch {
                burstProgress.snapTo(0f)
                burstProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    val crimsonHeart = Color(0xFFE11D48)
    val softRoseBg = Color(0xFFFFE4E6)

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Floating mini-hearts & golden sparkles particle burst around the heart button when liking
        if (burstTrigger > 0 && burstProgress.value < 0.99f) {
            val p = burstProgress.value
            val alpha = (1f - p).coerceIn(0f, 1f)
            Canvas(
                modifier = Modifier
                    .matchParentSize()
                    .height(52.dp)
            ) {
                val cx = size.width * 0.28f
                val cy = size.height * 0.5f
                val angles = listOf(-135.0, -105.0, -75.0, -45.0, -15.0, 20.0, 160.0, -90.0)
                angles.forEachIndexed { idx, deg ->
                    val rad = Math.toRadians(deg)
                    val dist = (18f + (idx % 3) * 12f) * p
                    val px = cx + (dist * cos(rad)).toFloat()
                    val py = cy + (dist * sin(rad)).toFloat() - (26f * p)
                    if (idx % 2 == 0) {
                        // Mini floating heart dot
                        drawCircle(
                            color = crimsonHeart.copy(alpha = alpha),
                            radius = (5.5f - 2f * p).coerceAtLeast(2f),
                            center = Offset(px, py)
                        )
                    } else {
                        // Golden sparkle dot
                        drawCircle(
                            color = GoldAccent.copy(alpha = alpha),
                            radius = (4.5f - 1.5f * p).coerceAtLeast(1.5f),
                            center = Offset(px, py)
                        )
                    }
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(50),
            color = if (isLikedByMe) softRoseBg else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .border(
                    width = 1.dp,
                    color = if (isLikedByMe) crimsonHeart.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(50)
                )
                .clickable {
                    if (!isLikedByMe) {
                        burstTrigger++
                    }
                    onReact()
                }
                .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = if (isLikedByMe) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "تفاعل القلب",
                    tint = if (isLikedByMe) crimsonHeart else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size((20f * heartScale.value).dp)
                )
                Text(
                    text = "$heartCount",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLikedByMe) Color(0xFF9F1239) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * Dual-Guarantee Inline Notification Banner rendered directly inside open Dialogs/Modals
 * alongside the root system Popup window so the user immediately sees feedback on any action.
 */
@Composable
fun InlineDialogToastBanner(
    toastAlert: com.example.model.ToastAlertMessage?,
    onDismiss: () -> Unit = {}
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = toastAlert != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        val alert = toastAlert ?: return@AnimatedVisibility
        val bgColor = if (alert.isSuccess) Color(0xFF059669) else Color(0xFFDC2626)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .clickable { onDismiss() },
            shape = RoundedCornerShape(12.dp),
            color = bgColor,
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (alert.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = "إشعار الإجراء",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Refined, ultra-compact capsule badge integrated at the bottom edge of the student's avatar circle.
 * Strips the prefix "وسام " and omits the icon so only the pure badge name (e.g., "الالتزام الذهبي") appears.
 */
@Composable
fun CompactStudentBadgeCapsule(
    badges: List<com.example.model.StudentAutoBadge> = emptyList(),
    badge: com.example.model.StudentAutoBadge? = null,
    fallbackHonorTitle: String = "",
    isOnHonorRoll: Boolean = false,
    onLightBackground: Boolean = true,
    onDarkBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val effectiveOnLight = if (onDarkBackground) false else onLightBackground
    val primaryBadge = badge ?: badges.firstOrNull()
    val rawBadgeTitle = when {
        isOnHonorRoll && fallbackHonorTitle.isNotBlank() -> fallbackHonorTitle
        primaryBadge != null -> primaryBadge.title
        else -> "وسام الالتزام الذهبي"
    }
    val cleanBadgeText = rawBadgeTitle
        .trim()
        .removePrefix("وسام ")
        .removePrefix("وسام")
        .trim()
        .ifBlank { "الالتزام الذهبي" }

    val badgeBg = if (effectiveOnLight) Color(0xFF059669) else Color.White
    val badgeBorder = if (effectiveOnLight) Color.White else Color(0xFF059669).copy(alpha = 0.45f)
    val badgeTextColor = if (effectiveOnLight) Color.White else Color(0xFF047857)

    Surface(
        shape = RoundedCornerShape(50),
        color = badgeBg,
        shadowElevation = 1.5.dp,
        modifier = modifier
            .border(0.8.dp, badgeBorder, RoundedCornerShape(50))
    ) {
        Text(
            text = cleanBadgeText,
            fontSize = 7.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.Bold,
            color = badgeTextColor,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
        )
    }
}

/**
 * Unified Avatar (54.dp) + Outer Bottom-Edge Micro Badge Box.
 * Keeps the circular avatar completely uncovered while displaying the full micro badge below its bottom rim.
 */
@Composable
fun StudentAvatarWithOverlayBadge(
    base64Data: String,
    contentDescription: String,
    badge: com.example.model.StudentAutoBadge? = null,
    badges: List<com.example.model.StudentAutoBadge> = emptyList(),
    fallbackHonorTitle: String = "",
    isOnHonorRoll: Boolean = false,
    fallbackIcon: ImageVector = Icons.Filled.Person,
    avatarSize: Dp = 54.dp,
    boxWidth: Dp = 62.dp,
    tint: Color? = null,
    onDarkBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .widthIn(min = boxWidth)
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Base64AvatarImage(
            base64Data = base64Data,
            fallbackIcon = fallbackIcon,
            contentDescription = contentDescription,
            size = avatarSize,
            tint = tint ?: if (onDarkBackground) Color.White else MaterialTheme.colorScheme.primary
        )
        CompactStudentBadgeCapsule(
            badges = badges,
            badge = badge,
            fallbackHonorTitle = fallbackHonorTitle,
            isOnHonorRoll = isOnHonorRoll,
            onDarkBackground = onDarkBackground,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 6.dp)
        )
    }
}

// =========================================================================================
// 3D DEPTH, GLASSMORPHISM & LAYERED CANVAS UI ENGINE
// =========================================================================================

enum class ThreeDIllustrationType {
    BOOKS_GRADUATION_CAP,
    MEGAPHONE,
    GLOWING_CHART_SPHERE,
    TROPHY_STARS,
    SMART_WALLET_CARD,
    SECURITY_SHIELD,
    CALENDAR_HOLIDAYS
}

/**
 * الخلفية المتموجة الناعمة (Soft Wave Mesh Background) لكامل التطبيق:
 * في الوضع النهاري: لون ثلجي فاتح جداً مائل للنعناع (0xFFF4FBF8) مع تموجات مائية ناعمة قطرية شفافة (0xFFE0F7EE و 0xFFD1FAE5)
 * في الوضع الليلي: كحلي داكن عميق عالي التباين (0xFF0F172A) مع إضاءة زمردية محيطية هادئة.
 */
@Composable
fun SoftWaveMeshBackground(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val baseBg = if (isDarkMode) Color(0xFF0F172A) else Color(0xFFF4FBF8)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(baseBg)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            if (!isDarkMode) {
                // Top diagonal soft mint waves
                val topWave1 = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(w, 0f)
                    lineTo(w, h * 0.24f)
                    cubicTo(
                        w * 0.72f, h * 0.16f,
                        w * 0.32f, h * 0.29f,
                        0f, h * 0.19f
                    )
                    close()
                }
                drawPath(
                    path = topWave1,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFD1FAE5).copy(alpha = 0.65f),
                            Color(0xFFE0F7EE).copy(alpha = 0.45f),
                            Color(0xFFF4FBF8).copy(alpha = 0.1f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(w, h * 0.28f)
                    )
                )

                val topWave2 = Path().apply {
                    moveTo(0f, h * 0.06f)
                    cubicTo(
                        w * 0.35f, h * 0.14f,
                        w * 0.65f, h * 0.03f,
                        w, h * 0.12f
                    )
                    lineTo(w, h * 0.32f)
                    cubicTo(
                        w * 0.60f, h * 0.22f,
                        w * 0.25f, h * 0.34f,
                        0f, h * 0.26f
                    )
                    close()
                }
                drawPath(
                    path = topWave2,
                    color = Color(0xFFE0F7EE).copy(alpha = 0.50f)
                )

                // Bottom soft mint ribbons
                val bottomWave1 = Path().apply {
                    moveTo(0f, h * 0.78f)
                    cubicTo(
                        w * 0.34f, h * 0.72f,
                        w * 0.68f, h * 0.86f,
                        w, h * 0.76f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = bottomWave1,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFE0F7EE).copy(alpha = 0.40f),
                            Color(0xFFD1FAE5).copy(alpha = 0.70f)
                        ),
                        startY = h * 0.74f,
                        endY = h
                    )
                )

                val bottomWave2 = Path().apply {
                    moveTo(0f, h * 0.86f)
                    cubicTo(
                        w * 0.40f, h * 0.94f,
                        w * 0.75f, h * 0.81f,
                        w, h * 0.89f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = bottomWave2,
                    color = Color(0xFFA7F3D0).copy(alpha = 0.32f)
                )
            } else {
                // Dark mode ambient emerald & navy mesh glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF059669).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.85f, h * 0.12f),
                        radius = w * 0.65f
                    ),
                    radius = w * 0.65f,
                    center = Offset(w * 0.85f, h * 0.12f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF0F3D5E).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.18f, h * 0.82f),
                        radius = w * 0.7f
                    ),
                    radius = w * 0.7f,
                    center = Offset(w * 0.18f, h * 0.82f)
                )
            }
        }
        content()
    }
}

/**
 * مجسمات ثلاثية الأبعاد مرسومة بـ Canvas متعدد الطبقات (Multi-layer 3D Canvas Illustrations)
 * مع تدرجات الإضاءة والظل العميق والحلقات المضيئة.
 */
@Composable
fun ThreeDIllustrationBox(
    type: ThreeDIllustrationType,
    modifier: Modifier = Modifier.size(110.dp)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        when (type) {
            ThreeDIllustrationType.BOOKS_GRADUATION_CAP -> {
                // 1) Glowing emerald aura & neon base ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = 0.42f),
                            Color(0xFF10B981).copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy * 1.05f),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy * 1.05f)
                )

                // Neon glowing ring at the base
                drawOval(
                    color = Color(0xFF34D399).copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.06f, h * 0.66f),
                    size = Size(w * 0.88f, h * 0.24f),
                    style = Stroke(width = 2.6.dp.toPx())
                )
                drawOval(
                    color = Color(0xFFA7F3D0).copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.03f, h * 0.64f),
                    size = Size(w * 0.94f, h * 0.28f),
                    style = Stroke(width = 5.dp.toPx())
                )

                // Drop shadow under bottom book
                drawOval(
                    color = Color(0xFF042F2E).copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.14f, h * 0.72f),
                    size = Size(w * 0.72f, h * 0.14f)
                )

                // 2) Bottom 3D Book (Royal Blue Cover + White 3D Pages)
                val b1CoverBottom = Path().apply {
                    moveTo(w * 0.15f, h * 0.68f)
                    lineTo(w * 0.54f, h * 0.80f)
                    lineTo(w * 0.87f, h * 0.68f)
                    lineTo(w * 0.48f, h * 0.57f)
                    close()
                }
                drawPath(b1CoverBottom, color = Color(0xFF1D4ED8))

                // Pages block of bottom book
                val b1Pages = Path().apply {
                    moveTo(w * 0.17f, h * 0.60f)
                    lineTo(w * 0.54f, h * 0.71f)
                    lineTo(w * 0.85f, h * 0.60f)
                    lineTo(w * 0.85f, h * 0.67f)
                    lineTo(w * 0.54f, h * 0.78f)
                    lineTo(w * 0.17f, h * 0.67f)
                    close()
                }
                drawPath(
                    path = b1Pages,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    )
                )

                // Left spine of bottom book
                val b1Spine = Path().apply {
                    moveTo(w * 0.15f, h * 0.59f)
                    lineTo(w * 0.54f, h * 0.71f)
                    lineTo(w * 0.54f, h * 0.79f)
                    lineTo(w * 0.15f, h * 0.67f)
                    close()
                }
                drawPath(
                    path = b1Spine,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF2563EB), Color(0xFF1E40AF))
                    )
                )

                // Top cover of bottom book
                val b1CoverTop = Path().apply {
                    moveTo(w * 0.15f, h * 0.59f)
                    lineTo(w * 0.54f, h * 0.71f)
                    lineTo(w * 0.87f, h * 0.59f)
                    lineTo(w * 0.48f, h * 0.48f)
                    close()
                }
                drawPath(
                    path = b1CoverTop,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF60A5FA), Color(0xFF2563EB))
                    )
                )

                // 3) Upper 3D Book (Bright Cyan/Blue + White Pages)
                val b2Pages = Path().apply {
                    moveTo(w * 0.19f, h * 0.48f)
                    lineTo(w * 0.53f, h * 0.58f)
                    lineTo(w * 0.83f, h * 0.48f)
                    lineTo(w * 0.83f, h * 0.55f)
                    lineTo(w * 0.53f, h * 0.65f)
                    lineTo(w * 0.19f, h * 0.55f)
                    close()
                }
                drawPath(
                    path = b2Pages,
                    brush = Brush.horizontalGradient(
                        listOf(Color.White, Color(0xFFF1F5F9), Color(0xFFCBD5E1))
                    )
                )

                val b2Spine = Path().apply {
                    moveTo(w * 0.18f, h * 0.47f)
                    lineTo(w * 0.53f, h * 0.58f)
                    lineTo(w * 0.53f, h * 0.65f)
                    lineTo(w * 0.18f, h * 0.54f)
                    close()
                }
                drawPath(
                    path = b2Spine,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF3B82F6), Color(0xFF1D4ED8))
                    )
                )

                val b2CoverTop = Path().apply {
                    moveTo(w * 0.18f, h * 0.47f)
                    lineTo(w * 0.53f, h * 0.58f)
                    lineTo(w * 0.84f, h * 0.47f)
                    lineTo(w * 0.49f, h * 0.37f)
                    close()
                }
                drawPath(
                    path = b2CoverTop,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF93C5FD), Color(0xFF3B82F6))
                    )
                )

                // 4) 3D Graduation Cap on top (Deep Navy/Charcoal Skullcap + Mortarboard + Golden Tassel)
                val capSkull = Path().apply {
                    moveTo(w * 0.33f, h * 0.30f)
                    lineTo(w * 0.69f, h * 0.30f)
                    lineTo(w * 0.66f, h * 0.44f)
                    quadraticBezierTo(w * 0.51f, h * 0.50f, w * 0.36f, h * 0.44f)
                    close()
                }
                drawPath(
                    path = capSkull,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF1E293B), Color(0xFF334155), Color(0xFF0F172A))
                    )
                )

                // Mortarboard 3D thickness
                val capRim = Path().apply {
                    moveTo(w * 0.12f, h * 0.25f)
                    lineTo(w * 0.51f, h * 0.36f)
                    lineTo(w * 0.90f, h * 0.25f)
                    lineTo(w * 0.90f, h * 0.28f)
                    lineTo(w * 0.51f, h * 0.39f)
                    lineTo(w * 0.12f, h * 0.28f)
                    close()
                }
                drawPath(capRim, color = Color(0xFF0F172A))

                // Mortarboard top diamond
                val capTop = Path().apply {
                    moveTo(w * 0.12f, h * 0.25f)
                    lineTo(w * 0.51f, h * 0.36f)
                    lineTo(w * 0.90f, h * 0.25f)
                    lineTo(w * 0.51f, h * 0.14f)
                    close()
                }
                drawPath(
                    path = capTop,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF475569), Color(0xFF1E293B), Color(0xFF0F172A))
                    )
                )

                // Center button & Golden Tassel
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = 3.2.dp.toPx(),
                    center = Offset(w * 0.51f, h * 0.25f)
                )
                val tasselCord = Path().apply {
                    moveTo(w * 0.51f, h * 0.25f)
                    quadraticBezierTo(w * 0.74f, h * 0.28f, w * 0.80f, h * 0.34f)
                    lineTo(w * 0.80f, h * 0.50f)
                }
                drawPath(
                    path = tasselCord,
                    color = Color(0xFFF59E0B),
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
                )
                val tasselFringe = Path().apply {
                    moveTo(w * 0.80f, h * 0.46f)
                    lineTo(w * 0.76f, h * 0.57f)
                    lineTo(w * 0.84f, h * 0.57f)
                    close()
                }
                drawPath(
                    path = tasselFringe,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                    )
                )
            }

            ThreeDIllustrationType.MEGAPHONE -> {
                // Soft emerald glow behind megaphone
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy)
                )

                // Handle (Emerald 3D grip)
                val handlePath = Path().apply {
                    moveTo(w * 0.30f, h * 0.58f)
                    lineTo(w * 0.40f, h * 0.55f)
                    lineTo(w * 0.46f, h * 0.82f)
                    lineTo(w * 0.35f, h * 0.85f)
                    close()
                }
                drawPath(
                    path = handlePath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF047857))
                    )
                )

                // Back emerald cap of megaphone
                drawOval(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669))
                    ),
                    topLeft = Offset(w * 0.16f, h * 0.44f),
                    size = Size(w * 0.20f, h * 0.24f)
                )

                // White/Silver 3D Cone Body
                val coneBody = Path().apply {
                    moveTo(w * 0.26f, h * 0.45f)
                    lineTo(w * 0.66f, h * 0.22f)
                    lineTo(w * 0.74f, h * 0.64f)
                    lineTo(w * 0.30f, h * 0.66f)
                    close()
                }
                drawPath(
                    path = coneBody,
                    brush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    )
                )

                // Emerald middle band on cone
                val emeraldBand = Path().apply {
                    moveTo(w * 0.24f, h * 0.46f)
                    lineTo(w * 0.38f, h * 0.39f)
                    lineTo(w * 0.43f, h * 0.65f)
                    lineTo(w * 0.28f, h * 0.66f)
                    close()
                }
                drawPath(
                    path = emeraldBand,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF10B981), Color(0xFF047857))
                    )
                )

                // Front Mouth Oval (3D shaded interior)
                rotate(degrees = -12f, pivot = Offset(w * 0.70f, h * 0.43f)) {
                    drawOval(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(w * 0.60f, h * 0.20f),
                        size = Size(w * 0.20f, h * 0.46f)
                    )
                    drawOval(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF64748B), Color(0xFF94A3B8), Color(0xFFE2E8F0))
                        ),
                        topLeft = Offset(w * 0.63f, h * 0.24f),
                        size = Size(w * 0.14f, h * 0.38f)
                    )
                    // Inner speaker dome
                    drawCircle(
                        color = Color(0xFF059669),
                        radius = w * 0.045f,
                        center = Offset(w * 0.68f, h * 0.43f)
                    )
                }

                // 3 Emerald Sound Waves on the right
                val waveColor = Color(0xFF059669)
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.83f, h * 0.28f),
                    end = Offset(w * 0.94f, h * 0.20f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.85f, h * 0.42f),
                    end = Offset(w * 0.97f, h * 0.40f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.83f, h * 0.55f),
                    end = Offset(w * 0.94f, h * 0.61f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            ThreeDIllustrationType.GLOWING_CHART_SPHERE -> {
                // Outer neon halo
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF4ADE80).copy(alpha = 0.55f),
                            Color(0xFF10B981).copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // 3D Glass Sphere body
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF10B981),
                            Color(0xFF059669),
                            Color(0xFF065F46)
                        ),
                        start = Offset(w * 0.2f, h * 0.2f),
                        end = Offset(w * 0.8f, h * 0.8f)
                    ),
                    radius = w * 0.39f,
                    center = Offset(cx, cy)
                )

                // Neon outer ring
                drawCircle(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFA7F3D0), Color(0xFF34D399), Color(0xFF10B981))
                    ),
                    radius = w * 0.39f,
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.5.dp.toPx())
                )

                // Top specular glass highlight
                drawOval(
                    brush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.45f), Color.Transparent)
                    ),
                    topLeft = Offset(w * 0.26f, h * 0.16f),
                    size = Size(w * 0.48f, h * 0.22f)
                )

                // 4 White 3D Bar Columns inside sphere
                val barWidth = w * 0.065f
                val baseLineY = h * 0.68f
                val bars = listOf(
                    w * 0.31f to h * 0.56f,
                    w * 0.41f to h * 0.49f,
                    w * 0.51f to h * 0.42f,
                    w * 0.61f to h * 0.35f
                )
                bars.forEach { (bx, topY) ->
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(bx, topY),
                        size = Size(barWidth, baseLineY - topY),
                        cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                    )
                }

                // Rising arrow above the bars
                val arrowPath = Path().apply {
                    moveTo(w * 0.30f, h * 0.48f)
                    quadraticBezierTo(w * 0.45f, h * 0.38f, w * 0.63f, h * 0.26f)
                }
                drawPath(
                    path = arrowPath,
                    color = Color.White,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
                val arrowHead = Path().apply {
                    moveTo(w * 0.55f, h * 0.25f)
                    lineTo(w * 0.65f, h * 0.24f)
                    lineTo(w * 0.63f, h * 0.34f)
                }
                drawPath(
                    path = arrowHead,
                    color = Color.White,
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            ThreeDIllustrationType.TROPHY_STARS -> {
                // Radiant aura
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFBBF24).copy(alpha = 0.38f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy)
                )
                // Pedestal base
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF047857), Color(0xFF065F46))),
                    topLeft = Offset(w * 0.28f, h * 0.72f),
                    size = Size(w * 0.44f, h * 0.12f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Golden stem
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFD97706))),
                    topLeft = Offset(w * 0.45f, h * 0.56f),
                    size = Size(w * 0.10f, h * 0.17f)
                )
                // Cup body
                val cupPath = Path().apply {
                    moveTo(w * 0.28f, h * 0.22f)
                    lineTo(w * 0.72f, h * 0.22f)
                    quadraticBezierTo(w * 0.70f, h * 0.58f, w * 0.50f, h * 0.58f)
                    quadraticBezierTo(w * 0.30f, h * 0.58f, w * 0.28f, h * 0.22f)
                    close()
                }
                drawPath(
                    path = cupPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                    )
                )
                // Handles
                drawArc(
                    color = Color(0xFFFBBF24),
                    startAngle = 90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.18f, h * 0.26f),
                    size = Size(w * 0.20f, h * 0.22f),
                    style = Stroke(width = 3.dp.toPx())
                )
                drawArc(
                    color = Color(0xFFFBBF24),
                    startAngle = -90f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(w * 0.62f, h * 0.26f),
                    size = Size(w * 0.20f, h * 0.22f),
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            ThreeDIllustrationType.SMART_WALLET_CARD -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy)
                )
                rotate(-8f, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF0F3D5E), Color(0xFF047857))
                        ),
                        topLeft = Offset(w * 0.14f, h * 0.22f),
                        size = Size(w * 0.72f, h * 0.48f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                }
                rotate(4f, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF065F46))
                        ),
                        topLeft = Offset(w * 0.14f, h * 0.28f),
                        size = Size(w * 0.74f, h * 0.48f),
                        cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                    )
                    // Smart Gold Chip
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                        topLeft = Offset(w * 0.22f, h * 0.40f),
                        size = Size(w * 0.16f, h * 0.12f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Barcode stripes
                    for (i in 0..5) {
                        drawRect(
                            color = Color.White.copy(alpha = 0.85f),
                            topLeft = Offset(w * (0.52f + i * 0.045f), h * 0.56f),
                            size = Size(w * 0.022f, h * 0.12f)
                        )
                    }
                }
            }

            ThreeDIllustrationType.SECURITY_SHIELD -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = 0.40f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy)
                )
                val shield = Path().apply {
                    moveTo(cx, h * 0.14f)
                    lineTo(w * 0.82f, h * 0.26f)
                    lineTo(w * 0.82f, h * 0.52f)
                    quadraticBezierTo(w * 0.82f, h * 0.76f, cx, h * 0.88f)
                    quadraticBezierTo(w * 0.18f, h * 0.76f, w * 0.18f, h * 0.52f)
                    lineTo(w * 0.18f, h * 0.26f)
                    close()
                }
                drawPath(
                    path = shield,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF0F3D5E))
                    )
                )
                drawPath(
                    path = shield,
                    color = Color.White.copy(alpha = 0.7f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Checkmark inside shield
                val check = Path().apply {
                    moveTo(w * 0.36f, h * 0.52f)
                    lineTo(w * 0.47f, h * 0.62f)
                    lineTo(w * 0.66f, h * 0.40f)
                }
                drawPath(
                    path = check,
                    color = Color.White,
                    style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }

            ThreeDIllustrationType.CALENDAR_HOLIDAYS -> {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = 0.35f), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.48f
                    ),
                    radius = w * 0.48f,
                    center = Offset(cx, cy)
                )
                // Calendar body
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.64f, h * 0.60f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Calendar top bar
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF047857))),
                    topLeft = Offset(w * 0.18f, h * 0.22f),
                    size = Size(w * 0.64f, h * 0.18f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Rings
                drawRoundRect(
                    color = Color(0xFF0F3D5E),
                    topLeft = Offset(w * 0.30f, h * 0.16f),
                    size = Size(w * 0.06f, h * 0.12f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF0F3D5E),
                    topLeft = Offset(w * 0.64f, h * 0.16f),
                    size = Size(w * 0.06f, h * 0.12f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // Grid dots
                for (r in 0..1) {
                    for (c in 0..2) {
                        drawRoundRect(
                            color = if (r == 0 && c == 1) Color(0xFF059669) else Color(0xFFCBD5E1),
                            topLeft = Offset(w * (0.28f + c * 0.17f), h * (0.48f + r * 0.14f)),
                            size = Size(w * 0.10f, h * 0.08f),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }
            }
        }
    }
}

/**
 * بطاقة الإعلان البانورامية ثلاثية الأبعاد مع مكبر الصوت (3D Megaphone Announcement Banner)
 * تطابق التصميم المرجعي حرفياً:
 * - أقصى اليمين: دائرة خضراء داكنة بداخلها سهم أبيض يتبعها خط فاصل عمودي زمردي
 * - المنتصف: عنوان الإعلان بالزمردي الغامق العريض + النص الفرعي بالكحلي الداكن
 * - أقصى اليسار: مجسم مكبر الصوت ثلاثي الأبعاد (3D Megaphone) مع 3 موجات صوتية
 */
@Composable
fun ThreeDMegaphoneAnnouncementBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.16f),
                spotColor = Color(0xFF059669).copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(22.dp))
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(22.dp),
        color = Color.Transparent,
        border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.90f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFE6F7F0),
                            Color(0xFFF2FBF7),
                            Color(0xFFE0F7EE)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Right side (in RTL): Dark emerald circle with white arrow + vertical emerald line + text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF047857),
                        modifier = Modifier.size(28.dp),
                        shadowElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .width(2.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF059669).copy(alpha = 0.55f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Left side (in RTL): 3D Megaphone Illustration
                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.MEGAPHONE,
                    modifier = Modifier.size(74.dp)
                )
            }
        }
    }
}

/**
 * البطاقة الترحيبية / الترويسية الملكية ثلاثية الأبعاد (3D Royal Hero Card)
 * مع تدرج زمردي إلى كحلي عميق، شبكة نقاط مضيئة (Halftone Dot Matrix)، كبسولات زجاجية علوية،
 * مجسم ثلاثي الأبعاد على اليسار، وشريط زر زجاجي سفلي اختياري.
 */
@Composable
fun ThreeDHeroHeaderCard(
    title: String,
    subtitle: String,
    rightPillText: String,
    rightPillIcon: ImageVector = Icons.Filled.School,
    leftPillText: String,
    leftPillIcon: ImageVector = Icons.Filled.VerifiedUser,
    illustrationType: ThreeDIllustrationType = ThreeDIllustrationType.BOOKS_GRADUATION_CAP,
    bottomActionText: String? = null,
    bottomActionIcon: ImageVector = Icons.Filled.ManageAccounts,
    bottomActionTestTag: String = "hero_bottom_action_btn",
    onBottomActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    extraBottomContent: (@Composable ColumnScope.() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0xFF047857).copy(alpha = 0.35f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.40f)
            ),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.40f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF059669),
                            Color(0xFF047857),
                            Color(0xFF0F3D5E)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 650f)
                    )
                )
        ) {
            // Halftone Dot Matrix & Radial Glow Overlay
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Radial emerald highlight behind the 3D illustration (left side in RTL)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.22f, h * 0.48f),
                        radius = w * 0.42f
                    ),
                    radius = w * 0.42f,
                    center = Offset(w * 0.22f, h * 0.48f)
                )

                // Halftone dot matrix in top-left and bottom-right corners
                val step = 11.dp.toPx()
                val dotRadius = 1.6.dp.toPx()
                for (row in 0..6) {
                    for (col in 0..7) {
                        val alpha = ((1f - (row + col) / 14f) * 0.22f).coerceIn(0.03f, 0.22f)
                        // Top-left dots
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = dotRadius,
                            center = Offset(w * 0.04f + col * step, h * 0.08f + row * step)
                        )
                        // Bottom-right dots
                        drawCircle(
                            color = Color.White.copy(alpha = alpha),
                            radius = dotRadius,
                            center = Offset(w * 0.96f - col * step, h * 0.92f - row * step)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Row: Two Frosted Glass Pills
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Right Pill (School Type & Stage)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = rightPillIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = rightPillText,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Left Pill (Role Badge)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = leftPillIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = leftPillText,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }

                // Middle Row: Welcome Title & Subtitle on Right + 3D Illustration on Left
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
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.92f)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    ThreeDIllustrationBox(
                        type = illustrationType,
                        modifier = Modifier.size(106.dp)
                    )
                }

                // Optional Bottom Glassmorphic Capsule Action Bar
                if (bottomActionText != null && onBottomActionClick != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(50))
                            .clickable { onBottomActionClick() }
                            .testTag(bottomActionTestTag),
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f),
                        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.38f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Icon(
                                imageVector = bottomActionIcon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                text = bottomActionText,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                if (extraBottomContent != null) {
                    extraBottomContent()
                }
            }
        }
    }
}

/**
 * عنوان قسم الإحصائيات الفاصل مع الخطوط الجانبية وأيقونة الأعمدة الملونة
 * (Section Divider Header: ── 📊 لوحة الإحصائيات العامة للمدرسة ──)
 */
@Composable
fun SectionDividerHeaderWithLines(
    title: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFF10B981))
                    )
                )
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Colored 3D mini bar chart icon matching reference screenshot
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                drawRoundRect(
                    color = Color(0xFF0F3D5E),
                    topLeft = Offset(w * 0.08f, h * 0.46f),
                    size = Size(w * 0.22f, h * 0.48f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF059669),
                    topLeft = Offset(w * 0.39f, h * 0.14f),
                    size = Size(w * 0.22f, h * 0.80f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF10B981),
                    topLeft = Offset(w * 0.70f, h * 0.32f),
                    size = Size(w * 0.22f, h * 0.62f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(2.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF10B981), Color.Transparent)
                    )
                )
        )
    }
}

/**
 * بطاقة "موجز الدوام وحركة البوابة اليومية" الزمردية ثلاثية الأبعاد (مع الكرة المضيئة و 5 كبسولات زجاجية)
 * تطابق الصورة المرجعية مع الحفاظ الصارم على المصطلحات التربوية المعتمدة (موجز الدوام وحركة البوابة اليومية / عرض التفاصيل).
 */
@Composable
fun ThreeDDailyAttendanceGateSummaryCard(
    title: String = "موجز الدوام وحركة البوابة اليومية",
    subtitle: String = "متابعة حضور وغياب وإجازات كل صف وحركة الدخول والانصراف في المدرسة",
    buttonText: String = "عرض التفاصيل",
    presentCount: Int,
    absentCount: Int,
    checkedInCount: Int,
    checkedOutCount: Int,
    insideNowCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "dashboard_live_radar_card"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0xFF059669).copy(alpha = 0.32f),
                spotColor = Color(0xFF047857).copy(alpha = 0.36f)
            )
            .clip(RoundedCornerShape(26.dp))
            .clickable { onClick() }
            .testTag(testTag),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.2.dp, Color.White.copy(alpha = 0.42f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF059669),
                            Color(0xFF047857),
                            Color(0xFF065F46)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 14.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Right side (in RTL): Pure White Capsule Button ("عرض التفاصيل <")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onClick() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = buttonText,
                                style = MaterialTheme.typography.labelMedium,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF047857),
                                maxLines = 1,
                                softWrap = false
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = Color(0xFF047857),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Center: Title & Educational Description
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Vertical translucent white divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(54.dp)
                            .background(Color.White.copy(alpha = 0.30f))
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Left side (in RTL): 3D Glowing Chart Sphere (72.dp)
                    ThreeDIllustrationBox(
                        type = ThreeDIllustrationType.GLOWING_CHART_SPHERE,
                        modifier = Modifier.size(72.dp)
                    )
                }

                // Bottom Row: 5 Frosted Glass Emerald Capsules in a single balanced row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        "حاضرون: $presentCount",
                        "غائبون: $absentCount",
                        "دخلوا: $checkedInCount",
                        "غادروا: $checkedOutCount",
                        "متواجدون: $insideNowCount"
                    ).forEach { pillLabel ->
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 3.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pillLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
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
    }
}

/**
 * بطاقات الإحصائيات البيضاء ذات الأمواج السفلية ثلاثية الأبعاد (3D Wave Stat Cards)
 * تطابق الصورة المرجعية:
 * - خلفية بيضاء نقية بحواف دائرية 24.dp وظل ثلاثي الأبعاد ناعم
 * - في الجزء السفلي: رسم موجة مائية مزدوجة ناعمة عبر Canvas (نعناعي فاتح أو أزرق سماوي فاتح)
 * - الأعلى يميناً: دائرة ناعمة بداخلها الأيقونة، الأعلى يساراً: دائرة صغيرة بداخلها سهم صغير
 * - المنتصف: الرقم العريض + العنوان الملون + الوصف المختصر
 */
@Composable
fun WaveStatCard3D(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    isBlueTheme: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val waveColor1 = if (isBlueTheme) Color(0xFFDBEAFE) else Color(0xFFCCFBF1)
    val waveColor2 = if (isBlueTheme) Color(0xFFBFDBFE) else Color(0xFFA7F3D0)
    val circleBg = if (isBlueTheme) Color(0xFFDBEAFE) else Color(0xFFD1FAE5)
    val iconTint = if (isBlueTheme) Color(0xFF2563EB) else Color(0xFF059669)

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = iconTint.copy(alpha = 0.14f),
                spotColor = iconTint.copy(alpha = 0.16f)
            )
            .clip(RoundedCornerShape(24.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
        ) {
            // Double smooth water wave drawn at the bottom of the card
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                val backWave = Path().apply {
                    moveTo(0f, h * 0.72f)
                    cubicTo(
                        w * 0.30f, h * 0.62f,
                        w * 0.68f, h * 0.84f,
                        w, h * 0.70f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = backWave,
                    color = waveColor1.copy(alpha = 0.75f)
                )

                val frontWave = Path().apply {
                    moveTo(0f, h * 0.80f)
                    cubicTo(
                        w * 0.36f, h * 0.90f,
                        w * 0.72f, h * 0.70f,
                        w, h * 0.82f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = frontWave,
                    color = waveColor2.copy(alpha = 0.65f)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: Large tinted icon circle on Right (in RTL) + small chevron circle on Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Surface(
                        shape = CircleShape,
                        color = circleBg,
                        border = BorderStroke(2.dp, Color.White),
                        shadowElevation = 2.dp,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = iconTint,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = circleBg.copy(alpha = 0.65f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Value & Titles aligned cleanly above the bottom wave
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = iconTint,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}



