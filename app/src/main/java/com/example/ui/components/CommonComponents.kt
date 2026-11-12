package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import com.example.model.OccasionIconKey
import com.example.model.TeacherEvaluationSummary
import kotlinx.coroutines.delay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import kotlin.math.abs
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
    tint: Color = Color.White
) {
    val safeData = base64Data.orEmpty()
    val bitmap = remember(safeData) {
        ImageCompressor.decodeBase64ToImageBitmap(safeData, maxDimension = 240)
    }
    Box(
        modifier = Modifier
            .requiredSize(size)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF065F46),
                        Color(0xFF042F2E)
                    )
                )
            )
            .border(
                width = 1.5.dp,
                brush = Brush.sweepGradient(
                    colors = listOf(
                        Color(0xFFFBBF24),
                        Color(0xFF34D399),
                        Color(0xFFF59E0B),
                        Color(0xFFFBBF24)
                    )
                ),
                shape = CircleShape
            )
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
                tint = Color.White,
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
            .clip(RoundedCornerShape(38.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0F705B),
                        Color(0xFF0A4F40),
                        Color(0xFF03221B)
                    )
                )
            )
            .border(
                width = 1.8.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFFF3B0),
                        Color(0xFFD4AF37),
                        Color(0xFF9A6F18)
                    )
                ),
                shape = RoundedCornerShape(38.dp)
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

            // Ambient Royal Emerald & Golden Glow
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.24f * bookProgress),
                        Color(0xFFD4AF37).copy(alpha = 0.12f * bookProgress),
                        Color.Transparent
                    ),
                    center = Offset(cx, cy),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(cx, cy)
            )

            // Outer Thin Glowing Golden Ring
            drawCircle(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0x99FFF3B0),
                        Color(0x55D4AF37),
                        Color(0x889A6F18)
                    ),
                    start = Offset(w * 0.12f, h * 0.12f),
                    end = Offset(w * 0.88f, h * 0.88f)
                ),
                radius = w * 0.42f,
                center = Offset(cx, cy),
                style = Stroke(width = 1.6f * unit)
            )

            // =====================================================================
            // 1. 3D ROYAL ACADEMIC SHIELD (Golden Frame #FFF3B0 -> #D4AF37 -> #9A6F18 + Emerald Glass Core)
            // =====================================================================
            val shieldScale = (0.86f + 0.14f * bookProgress).coerceIn(0.1f, 1f)
            withTransform({
                scale(scaleX = shieldScale, scaleY = shieldScale, pivot = Offset(cx, cy))
            }) {
                val outerShieldPath = Path().apply {
                    moveTo(cx, cy - 78f * unit)
                    lineTo(cx + 64f * unit, cy - 58f * unit)
                    lineTo(cx + 64f * unit, cy - 2f * unit)
                    cubicTo(
                        cx + 64f * unit, cy + 38f * unit,
                        cx + 36f * unit, cy + 65f * unit,
                        cx, cy + 80f * unit
                    )
                    cubicTo(
                        cx - 36f * unit, cy + 65f * unit,
                        cx - 64f * unit, cy + 38f * unit,
                        cx - 64f * unit, cy - 2f * unit
                    )
                    lineTo(cx - 64f * unit, cy - 58f * unit)
                    close()
                }

                // 3D Golden Shield Frame
                drawPath(
                    path = outerShieldPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFFFF3B0),
                            Color(0xFFF4D068),
                            Color(0xFFD4AF37),
                            Color(0xFF9A6F18)
                        ),
                        start = Offset(cx - 64f * unit, cy - 78f * unit),
                        end = Offset(cx + 64f * unit, cy + 80f * unit)
                    )
                )

                // Inner Shield Deep Emerald Core
                val innerShieldPath = Path().apply {
                    moveTo(cx, cy - 69f * unit)
                    lineTo(cx + 55f * unit, cy - 51f * unit)
                    lineTo(cx + 55f * unit, cy - 3f * unit)
                    cubicTo(
                        cx + 55f * unit, cy + 32f * unit,
                        cx + 31f * unit, cy + 56f * unit,
                        cx, cy + 70f * unit
                    )
                    cubicTo(
                        cx - 31f * unit, cy + 56f * unit,
                        cx - 55f * unit, cy + 32f * unit,
                        cx - 55f * unit, cy - 3f * unit
                    )
                    lineTo(cx - 55f * unit, cy - 51f * unit)
                    close()
                }

                drawPath(
                    path = innerShieldPath,
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF117A63),
                            Color(0xFF084C3D),
                            Color(0xFF03261E)
                        ),
                        center = Offset(cx, cy - 8f * unit),
                        radius = 78f * unit
                    )
                )

                // Glassmorphic Diagonal Shield Reflection
                val glassReflectionPath = Path().apply {
                    moveTo(cx, cy - 69f * unit)
                    lineTo(cx - 55f * unit, cy - 51f * unit)
                    lineTo(cx - 55f * unit, cy - 3f * unit)
                    cubicTo(
                        cx - 55f * unit, cy + 24f * unit,
                        cx - 36f * unit, cy + 46f * unit,
                        cx - 10f * unit, cy + 62f * unit
                    )
                    lineTo(cx + 8f * unit, cy - 12f * unit)
                    close()
                }
                drawPath(
                    path = glassReflectionPath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.20f),
                            Color(0xFFA7F3D0).copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        start = Offset(cx - 55f * unit, cy - 65f * unit),
                        end = Offset(cx + 10f * unit, cy + 40f * unit)
                    )
                )
            }

            // =====================================================================
            // 2. OPEN BOOK OF KNOWLEDGE (Pearl-White & Gold Pages + Rising Illumination)
            // =====================================================================
            if (bookProgress > 0.01f) {
                val spineBottomY = cy + 44f * unit
                val spineTopY = cy + 12f * unit

                // Soft Golden Illumination Rays Rising from the Open Book Toward the Cap
                val lightConePath = Path().apply {
                    moveTo(cx, spineTopY + 4f * unit)
                    lineTo(cx - 40f * unit, cy - 26f * unit)
                    lineTo(cx + 40f * unit, cy - 26f * unit)
                    close()
                }
                drawPath(
                    path = lightConePath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFDE68A).copy(alpha = 0.16f * bookProgress),
                            Color(0xFFFFF3B0).copy(alpha = 0.36f * bookProgress)
                        ),
                        startY = cy - 26f * unit,
                        endY = spineTopY + 4f * unit
                    )
                )

                // Golden Book Base Cover
                val coverBasePath = Path().apply {
                    moveTo(cx, spineTopY + 5f * unit)
                    cubicTo(
                        cx - 16f * unit, spineTopY - 3f * unit,
                        cx - 32f * unit, spineTopY - 4f * unit,
                        cx - 44f * unit, spineTopY
                    )
                    lineTo(cx - 41f * unit, spineBottomY + 4f * unit)
                    cubicTo(
                        cx - 28f * unit, spineBottomY,
                        cx - 14f * unit, spineBottomY + 1f * unit,
                        cx, spineBottomY + 9f * unit
                    )
                    cubicTo(
                        cx + 14f * unit, spineBottomY + 1f * unit,
                        cx + 28f * unit, spineBottomY,
                        cx + 41f * unit, spineBottomY + 4f * unit
                    )
                    lineTo(cx + 44f * unit, spineTopY)
                    cubicTo(
                        cx + 32f * unit, spineTopY - 4f * unit,
                        cx + 16f * unit, spineTopY - 3f * unit,
                        cx, spineTopY + 5f * unit
                    )
                    close()
                }
                drawPath(
                    path = coverBasePath,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFFB8860B),
                            Color(0xFFFFF3B0),
                            Color(0xFF9A6F18)
                        ),
                        start = Offset(cx - 44f * unit, spineTopY),
                        end = Offset(cx + 44f * unit, spineBottomY)
                    )
                )

                val pageScaleX = 0.25f + 0.75f * bookProgress
                val leftPageRot = (1f - bookProgress) * 14f
                val rightPageRot = -(1f - bookProgress) * 14f

                // Left Pearl-White Page
                withTransform({
                    rotate(degrees = leftPageRot, pivot = Offset(cx, spineBottomY))
                    scale(scaleX = pageScaleX, scaleY = bookProgress, pivot = Offset(cx, spineBottomY))
                }) {
                    val leftPage = Path().apply {
                        moveTo(cx - 1f * unit, spineTopY)
                        cubicTo(
                            cx - 15f * unit, spineTopY - 8f * unit,
                            cx - 30f * unit, spineTopY - 9f * unit,
                            cx - 41f * unit, spineTopY - 4f * unit
                        )
                        lineTo(cx - 39f * unit, spineBottomY - 2f * unit)
                        cubicTo(
                            cx - 27f * unit, spineBottomY - 6f * unit,
                            cx - 14f * unit, spineBottomY - 5f * unit,
                            cx - 1f * unit, spineBottomY + 3f * unit
                        )
                        close()
                    }
                    drawPath(
                        path = leftPage,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFFFFF),
                                Color(0xFFFFFBEB),
                                Color(0xFFFDE68A)
                            ),
                            start = Offset(cx - 41f * unit, spineTopY - 6f * unit),
                            end = Offset(cx, spineBottomY)
                        )
                    )
                    for (row in 0..2) {
                        val ly = spineTopY + (6f + row * 7.5f) * unit
                        drawLine(
                            color = Color(0xFFC8961E).copy(alpha = 0.78f * bookProgress),
                            start = Offset(cx - 8f * unit, ly - 1f * unit),
                            end = Offset(cx - 32f * unit, ly - 3.5f * unit),
                            strokeWidth = 2.0f * unit,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Right Pearl-White Page
                withTransform({
                    rotate(degrees = rightPageRot, pivot = Offset(cx, spineBottomY))
                    scale(scaleX = pageScaleX, scaleY = bookProgress, pivot = Offset(cx, spineBottomY))
                }) {
                    val rightPage = Path().apply {
                        moveTo(cx + 1f * unit, spineTopY)
                        cubicTo(
                            cx + 15f * unit, spineTopY - 8f * unit,
                            cx + 30f * unit, spineTopY - 9f * unit,
                            cx + 41f * unit, spineTopY - 4f * unit
                        )
                        lineTo(cx + 39f * unit, spineBottomY - 2f * unit)
                        cubicTo(
                            cx + 27f * unit, spineBottomY - 6f * unit,
                            cx + 14f * unit, spineBottomY - 5f * unit,
                            cx + 1f * unit, spineBottomY + 3f * unit
                        )
                        close()
                    }
                    drawPath(
                        path = rightPage,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFFBEB),
                                Color(0xFFFEF3C7),
                                Color(0xFFF4D068)
                            ),
                            start = Offset(cx + 41f * unit, spineTopY - 6f * unit),
                            end = Offset(cx, spineBottomY)
                        )
                    )
                    for (row in 0..2) {
                        val ly = spineTopY + (6f + row * 7.5f) * unit
                        drawLine(
                            color = Color(0xFFC8961E).copy(alpha = 0.78f * bookProgress),
                            start = Offset(cx + 8f * unit, ly - 1f * unit),
                            end = Offset(cx + 32f * unit, ly - 3.5f * unit),
                            strokeWidth = 2.0f * unit,
                            cap = StrokeCap.Round
                        )
                    }
                }

                // Center Golden Book Spine
                drawLine(
                    color = Color(0xFFD4AF37),
                    start = Offset(cx, spineTopY - 1f * unit),
                    end = Offset(cx, spineBottomY + 7f * unit),
                    strokeWidth = 3.2f * unit,
                    cap = StrokeCap.Round
                )
            }

            // =====================================================================
            // 3. LUXURY 3D GRADUATION CAP / MORTARBOARD & GOLDEN TASSEL
            // =====================================================================
            if (capAlpha > 0.01f) {
                val capTargetCy = cy - 26f * unit
                val dropOffsetY = (1f - capLandingEase) * (-36f * unit)
                val capCy = capTargetCy + dropOffsetY
                val capScale = (0.78f + 0.22f * capLandingEase).coerceAtLeast(0.1f)

                withTransform({
                    scale(scaleX = capScale, scaleY = capScale, pivot = Offset(cx, capCy))
                }) {
                    // 3D Skullcap Base (Deep Royal Navy-Emerald with Golden Rim)
                    val capBasePath = Path().apply {
                        moveTo(cx - 28f * unit, capCy + 5f * unit)
                        lineTo(cx - 26f * unit, capCy + 21f * unit)
                        cubicTo(
                            cx - 14f * unit, capCy + 29f * unit,
                            cx + 14f * unit, capCy + 29f * unit,
                            cx + 26f * unit, capCy + 21f * unit
                        )
                        lineTo(cx + 28f * unit, capCy + 5f * unit)
                        close()
                    }
                    drawPath(
                        path = capBasePath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0D3B2E).copy(alpha = capAlpha),
                                Color(0xFF06231B).copy(alpha = capAlpha),
                                Color(0xFF0F3D5E).copy(alpha = capAlpha)
                            ),
                            start = Offset(cx - 28f * unit, capCy + 5f * unit),
                            end = Offset(cx + 28f * unit, capCy + 26f * unit)
                        )
                    )
                    drawPath(
                        path = capBasePath,
                        color = Color(0xFFF4D068).copy(alpha = capAlpha),
                        style = Stroke(width = 2.2f * unit, join = StrokeJoin.Round)
                    )

                    // 3D Mortarboard Lower Golden Edge Rim
                    val rimEdgePath = Path().apply {
                        moveTo(cx - 52f * unit, capCy - 2f * unit)
                        lineTo(cx, capCy + 16f * unit)
                        lineTo(cx + 52f * unit, capCy - 2f * unit)
                        lineTo(cx + 52f * unit, capCy + 2.5f * unit)
                        lineTo(cx, capCy + 20.5f * unit)
                        lineTo(cx - 52f * unit, capCy + 2.5f * unit)
                        close()
                    }
                    drawPath(
                        path = rimEdgePath,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF9A6F18).copy(alpha = capAlpha),
                                Color(0xFFFFF3B0).copy(alpha = capAlpha),
                                Color(0xFF855E12).copy(alpha = capAlpha)
                            ),
                            startX = cx - 52f * unit,
                            endX = cx + 52f * unit
                        )
                    )

                    // 3D Mortarboard Diamond Top Surface
                    val diamondPath = Path().apply {
                        moveTo(cx, capCy - 20f * unit)
                        lineTo(cx + 52f * unit, capCy - 2f * unit)
                        lineTo(cx, capCy + 16f * unit)
                        lineTo(cx - 52f * unit, capCy - 2f * unit)
                        close()
                    }
                    drawPath(
                        path = diamondPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF0A3A2E).copy(alpha = capAlpha),
                                Color(0xFF062820).copy(alpha = capAlpha),
                                Color(0xFF0A2E46).copy(alpha = capAlpha)
                            ),
                            start = Offset(cx - 52f * unit, capCy - 20f * unit),
                            end = Offset(cx + 52f * unit, capCy + 16f * unit)
                        )
                    )

                    // Inner Golden Bevel Frame on Mortarboard Top
                    val innerDiamondPath = Path().apply {
                        moveTo(cx, capCy - 16.5f * unit)
                        lineTo(cx + 45f * unit, capCy - 2f * unit)
                        lineTo(cx, capCy + 12.5f * unit)
                        lineTo(cx - 45f * unit, capCy - 2f * unit)
                        close()
                    }
                    drawPath(
                        path = innerDiamondPath,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFFF3B0).copy(alpha = capAlpha),
                                Color(0xFFD4AF37).copy(alpha = capAlpha),
                                Color(0xFF9A6F18).copy(alpha = capAlpha)
                            ),
                            start = Offset(cx - 45f * unit, capCy - 16f * unit),
                            end = Offset(cx + 45f * unit, capCy + 12f * unit)
                        ),
                        style = Stroke(width = 2.0f * unit, join = StrokeJoin.Round)
                    )

                    // Center Golden Medallion Button on Mortarboard
                    val pivotX = cx
                    val pivotY = capCy - 2f * unit
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBEB).copy(alpha = capAlpha),
                                Color(0xFFD4AF37).copy(alpha = capAlpha),
                                Color(0xFF9A6F18).copy(alpha = capAlpha)
                            ),
                            center = Offset(pivotX, pivotY),
                            radius = 5.2f * unit
                        ),
                        radius = 4.8f * unit,
                        center = Offset(pivotX, pivotY)
                    )

                    // Golden Tassel Cord & Pendulum Fringe
                    val tasselAnchorX = cx + 36f * unit
                    val tasselAnchorY = capCy + 5f * unit
                    drawLine(
                        color = Color(0xFFFFF3B0).copy(alpha = capAlpha),
                        start = Offset(pivotX, pivotY),
                        end = Offset(tasselAnchorX, tasselAnchorY),
                        strokeWidth = 2.6f * unit,
                        cap = StrokeCap.Round
                    )

                    withTransform({
                        rotate(
                            degrees = totalTasselSwingDeg,
                            pivot = Offset(tasselAnchorX, tasselAnchorY)
                        )
                    }) {
                        val tasselEndY = tasselAnchorY + 22f * unit
                        drawLine(
                            color = Color(0xFFFFF3B0).copy(alpha = capAlpha),
                            start = Offset(tasselAnchorX, tasselAnchorY),
                            end = Offset(tasselAnchorX, tasselEndY),
                            strokeWidth = 2.6f * unit,
                            cap = StrokeCap.Round
                        )
                        val fringePath = Path().apply {
                            moveTo(tasselAnchorX - 5.2f * unit, tasselEndY + 11f * unit)
                            lineTo(tasselAnchorX + 5.2f * unit, tasselEndY + 11f * unit)
                            lineTo(tasselAnchorX + 2.6f * unit, tasselEndY)
                            lineTo(tasselAnchorX - 2.6f * unit, tasselEndY)
                            close()
                        }
                        drawPath(
                            path = fringePath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFFFF3B0).copy(alpha = capAlpha),
                                    Color(0xFFD4AF37).copy(alpha = capAlpha),
                                    Color(0xFF9A6F18).copy(alpha = capAlpha)
                                ),
                                startY = tasselEndY,
                                endY = tasselEndY + 11f * unit
                            )
                        )
                        drawCircle(
                            color = Color(0xFFD4AF37).copy(alpha = capAlpha),
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
                            Color.White.copy(alpha = 0.22f),
                            Color(0xFFFFF3B0).copy(alpha = 0.28f),
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
 * Razor-Sharp Native Compose Vector Logo + Official Live Brand Typography:
 * Renders the 3D Royal Academic Shield + 3D Graduation Cap + Open Book of Knowledge
 * at full screen vector resolution (never scaling up a bitmap), followed by live text:
 * - "منصة مدرستي الذكية"
 * - "MADRASATI"
 */
@Composable
fun RoyalAcademicCrestBrandHeader(
    isDarkMode: Boolean,
    logoSize: Dp = 104.dp,
    subtitleText: String? = null,
    onLogoClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = if (onLogoClick != null) {
                Modifier
                    .clip(RoundedCornerShape(38.dp))
                    .clickable { onLogoClick() }
            } else {
                Modifier
            }
        ) {
            ChoreographedRoyalEmblemAnimation(size = logoSize)
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = "منصة مدرستي الذكية",
            fontSize = 21.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (isDarkMode) Color.White else Color(0xFF03221B),
            textAlign = TextAlign.Center
        )

        Text(
            text = "MADRASATI",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 3.5.sp,
            color = if (isDarkMode) Color(0xFFF4D068) else Color(0xFFB8860B),
            textAlign = TextAlign.Center
        )

        if (!subtitleText.isNullOrBlank()) {
            Text(
                text = subtitleText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF065F46),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/**
 * Full-Screen Choreographed Splash Screen (شاشة تشغيل التطبيق الترحيبية):
 * - Renders the razor-sharp 3D Royal Academic Shield + Graduation Cap + Open Book of Knowledge
 *   directly via Compose Canvas at full screen vector resolution (Zero Bitmap Scaling).
 * - Writes underneath in live crisp Text: "منصة مدرستي الذكية" and "MADRASATI".
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
                            Color(0xFF03221B),
                            Color(0xFF0A4F40),
                            Color(0xFF03221B)
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
                        GoldAccent.copy(alpha = if (isDarkMode) 0.16f else 0.10f),
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
            // Razor-Sharp Full-Resolution Native Compose Vector Crest
            ChoreographedRoyalEmblemAnimation(size = 216.dp)

            Spacer(modifier = Modifier.height(22.dp))

            // Live Razor-Sharp Typography: "منصة مدرستي الذكية" & "MADRASATI"
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
                    fontSize = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDarkMode) Color.White else Color(0xFF03221B),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "MADRASATI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 4.sp,
                    color = if (isDarkMode) Color(0xFFFFF3B0) else Color(0xFFB8860B),
                    textAlign = TextAlign.Center
                )

                Box(
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .width(56.dp)
                        .height(2.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color(0xFFD4AF37),
                                    Color.Transparent
                                )
                            )
                        )
                )

                if (hasDiscoveredSchool) {
                    Text(
                        text = discoveredSchoolName.orEmpty(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
                        textAlign = TextAlign.Center
                    )
                } else {
                    Text(
                        text = "منصة مدرستي الذكية المتكاملة",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color(0xFFA7F3D0) else Color(0xFF047857),
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
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(toastAlert?.timestamp) {
        if (toastAlert != null) {
            runCatching { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        }
    }

    androidx.compose.animation.AnimatedVisibility(
        visible = toastAlert != null,
        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandVertically(),
        exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkVertically()
    ) {
        val alert = toastAlert ?: return@AnimatedVisibility
        val bgColor = if (alert.isSuccess) Color(0xFFECFDF5) else Color(0xFFFDE8E8)
        val borderColor = if (alert.isSuccess) Color(0xFFA7F3D0) else Color(0xFFF8B4B4)
        val contentColor = if (alert.isSuccess) Color(0xFF065F46) else Color(0xFF9B1C1C)

        var dragOffsetX by remember(alert.id) { mutableFloatStateOf(0f) }
        var dragOffsetY by remember(alert.id) { mutableFloatStateOf(0f) }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
                .graphicsLayer {
                    translationX = dragOffsetX
                    translationY = dragOffsetY.coerceAtMost(0f)
                    alpha = (1f - (abs(dragOffsetX) / 500f) - (abs(dragOffsetY.coerceAtMost(0f)) / 220f)).coerceIn(0.2f, 1f)
                }
                .pointerInput(alert.id) {
                    detectDragGestures(
                        onDragEnd = {
                            if (dragOffsetY < -35f || abs(dragOffsetX) > 90f) {
                                onDismiss()
                            } else {
                                dragOffsetX = 0f
                                dragOffsetY = 0f
                            }
                        },
                        onDragCancel = {
                            dragOffsetX = 0f
                            dragOffsetY = 0f
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            dragOffsetX += dragAmount.x
                            dragOffsetY = (dragOffsetY + dragAmount.y).coerceAtMost(16f)
                            if (dragOffsetY < -75f || abs(dragOffsetX) > 190f) {
                                onDismiss()
                            }
                        }
                    )
                }
                .shadow(
                    elevation = 4.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = borderColor.copy(alpha = 0.25f),
                    spotColor = borderColor.copy(alpha = 0.30f)
                )
                .clip(RoundedCornerShape(16.dp))
                .clickable { onDismiss() },
            shape = RoundedCornerShape(16.dp),
            color = bgColor,
            border = BorderStroke(1.dp, borderColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = borderColor.copy(alpha = 0.45f),
                    modifier = Modifier.size(30.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = if (alert.isSuccess) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                            contentDescription = "إشعار الإجراء",
                            tint = contentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = contentColor,
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
    WELCOME_ACADEMIC_CHARACTER,
    BOOKS_GRADUATION_CAP,
    MEGAPHONE,
    DIRECT_CHAT_ENVELOPE_3D,
    SOCIAL_COMMUNITY_3D,
    STUDENT_CREATIVITY_SHOWCASE_3D,
    SYSTEM_BROADCAST_3D,
    GLOWING_CHART_SPHERE,
    ATTENDANCE_GATE_CLOCK,
    EXAM_CERTIFICATE_PEN,
    TROPHY_STARS,
    ROYAL_HONOR_SHIELD,
    SCHEDULE_BOOKS_3D,
    SMART_WALLET_CARD,
    SECURITY_SHIELD,
    USER_MANAGEMENT_AVATARS_3D,
    SETTINGS_ROTATING_GEARS_3D,
    TEACHER_EXCELLENCE_CROWN_3D,
    CRESCENT_LANTERN_3D,
    UPDATE_SPARK_3D,
    CALENDAR_HOLIDAYS,
    CLOUD_OFFLINE_3D,
    DIGITAL_LIBRARY_ILLUMINATED_BOOK_3D
}

fun OccasionIconKey.toThreeDIllustrationType(): ThreeDIllustrationType {
    return when (this) {
        OccasionIconKey.BELL -> ThreeDIllustrationType.MEGAPHONE
        OccasionIconKey.GRADUATION_CAP,
        OccasionIconKey.BOOK,
        OccasionIconKey.ROSE -> ThreeDIllustrationType.BOOKS_GRADUATION_CAP
        OccasionIconKey.CRESCENT -> ThreeDIllustrationType.CRESCENT_LANTERN_3D
        OccasionIconKey.TROPHY,
        OccasionIconKey.CROWN -> ThreeDIllustrationType.TROPHY_STARS
        OccasionIconKey.CALENDAR -> ThreeDIllustrationType.CALENDAR_HOLIDAYS
        OccasionIconKey.STAR -> ThreeDIllustrationType.UPDATE_SPARK_3D
        OccasionIconKey.SHIELD,
        OccasionIconKey.FLAG -> ThreeDIllustrationType.SYSTEM_BROADCAST_3D
    }
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
    // Lottie-Style Infinite Smooth Canvas Animation Engine
    val infiniteTransition = rememberInfiniteTransition(label = "three_d_illustration_${type.name}")
    val floatOffsetY by infiniteTransition.animateFloat(
        initialValue = -4.5f,
        targetValue = 4.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset_y"
    )
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breath_scale"
    )
    val glowPulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.58f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse_alpha"
    )
    val waveExpandFactor by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave_expand_factor"
    )
    val continuousRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "continuous_rotation"
    )

    Canvas(
        modifier = modifier.graphicsLayer {
            translationY = floatOffsetY
            scaleX = breathScale
            scaleY = breathScale
        }
    ) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f

        when (type) {
            ThreeDIllustrationType.WELCOME_ACADEMIC_CHARACTER -> {
                // 3D Isometric Emerald & Gold Crystal Shield/Cube + Academic Crown + 3-Layer Light Pedestal + 3D Floating Glass Orbs
                val phaseRad = (continuousRotation * (Math.PI / 180.0) * 2.0).toFloat()
                val crystalHoverY = sin(phaseRad) * (h * 0.032f)
                val crownHoverY = sin(phaseRad + 0.7f) * (h * 0.038f)

                // 1) Volumetric Radial Aura & Holographic Upward Light Beam
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.18f).coerceAtMost(0.78f)),
                            Color(0xFFFDE68A).copy(alpha = glowPulseAlpha * 0.36f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy * 0.96f),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy * 0.96f)
                )

                val beamPath = Path().apply {
                    moveTo(w * 0.24f, h * 0.80f)
                    lineTo(w * 0.14f, h * 0.16f)
                    lineTo(w * 0.86f, h * 0.16f)
                    lineTo(w * 0.76f, h * 0.80f)
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFF6EE7B7).copy(alpha = 0.12f),
                            Color(0xFFFDE68A).copy(alpha = 0.22f)
                        ),
                        startY = h * 0.14f,
                        endY = h * 0.82f
                    )
                )

                // 2) 3-Layer Isometric Glowing Light Pedestal (Bottom Platform)
                for (layer in 2 downTo 0) {
                    val ly = h * (0.76f + layer * 0.048f)
                    val rx = w * (0.31f + layer * 0.055f)
                    val ry = h * (0.068f + layer * 0.012f)

                    val isoPlatform = Path().apply {
                        moveTo(cx, ly - ry)
                        lineTo(cx + rx, ly)
                        lineTo(cx, ly + ry)
                        lineTo(cx - rx, ly)
                        close()
                    }
                    drawPath(
                        path = isoPlatform,
                        brush = Brush.linearGradient(
                            colors = when (layer) {
                                0 -> listOf(Color(0xFF34D399).copy(alpha = 0.88f), Color(0xFF059669).copy(alpha = 0.92f))
                                1 -> listOf(Color(0xFF10B981).copy(alpha = 0.68f), Color(0xFF047857).copy(alpha = 0.80f))
                                else -> listOf(Color(0xFF065F46).copy(alpha = 0.55f), Color(0xFF022C22).copy(alpha = 0.72f))
                            },
                            start = Offset(cx - rx, ly - ry),
                            end = Offset(cx + rx, ly + ry)
                        )
                    )
                    drawPath(
                        path = isoPlatform,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFFEF08A).copy(alpha = if (layer == 0) 0.95f else 0.55f),
                                Color(0xFF6EE7B7).copy(alpha = 0.80f),
                                Color(0xFFF59E0B).copy(alpha = 0.70f)
                            )
                        ),
                        style = Stroke(width = (1.8f - layer * 0.35f).dp.toPx(), join = StrokeJoin.Round)
                    )
                }

                // 3) 3D Isometric Emerald & Gold Crystal Cube / Shield Core
                val cubeCenterY = h * 0.51f + crystalHoverY
                val cubeR = w * 0.23f
                val dx = cubeR * 0.866f
                val dy = cubeR * 0.50f

                val topVertex = Offset(cx, cubeCenterY - cubeR)
                val rightVertex = Offset(cx + dx, cubeCenterY - dy)
                val centerVertex = Offset(cx, cubeCenterY)
                val leftVertex = Offset(cx - dx, cubeCenterY - dy)
                val bottomRightVertex = Offset(cx + dx, cubeCenterY + dy)
                val bottomVertex = Offset(cx, cubeCenterY + cubeR)
                val bottomLeftVertex = Offset(cx - dx, cubeCenterY + dy)

                // Top Isometric Crystal Facet (Brightest Glass Highlight)
                val topFace = Path().apply {
                    moveTo(topVertex.x, topVertex.y)
                    lineTo(rightVertex.x, rightVertex.y)
                    lineTo(centerVertex.x, centerVertex.y)
                    lineTo(leftVertex.x, leftVertex.y)
                    close()
                }
                drawPath(
                    path = topFace,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFA7F3D0), Color(0xFF34D399), Color(0xFF10B981)),
                        start = topVertex,
                        end = centerVertex
                    )
                )

                // Left Isometric Crystal Facet (Rich Emerald Midtone)
                val leftFace = Path().apply {
                    moveTo(leftVertex.x, leftVertex.y)
                    lineTo(centerVertex.x, centerVertex.y)
                    lineTo(bottomVertex.x, bottomVertex.y)
                    lineTo(bottomLeftVertex.x, bottomLeftVertex.y)
                    close()
                }
                drawPath(
                    path = leftFace,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
                        start = leftVertex,
                        end = bottomVertex
                    )
                )

                // Right Isometric Crystal Facet (Deep Volumetric Emerald-Gold Shadow)
                val rightFace = Path().apply {
                    moveTo(centerVertex.x, centerVertex.y)
                    lineTo(rightVertex.x, rightVertex.y)
                    lineTo(bottomRightVertex.x, bottomRightVertex.y)
                    lineTo(bottomVertex.x, bottomVertex.y)
                    close()
                }
                drawPath(
                    path = rightFace,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF047857), Color(0xFF065F46), Color(0xFF022C22)),
                        start = centerVertex,
                        end = bottomRightVertex
                    )
                )

                // Inner Golden Isometric Shield Crest on Front Facets
                val innerShield = Path().apply {
                    moveTo(cx, cubeCenterY - cubeR * 0.55f)
                    lineTo(cx + dx * 0.58f, cubeCenterY - dy * 0.25f)
                    lineTo(cx + dx * 0.46f, cubeCenterY + dy * 0.55f)
                    lineTo(cx, cubeCenterY + cubeR * 0.68f)
                    lineTo(cx - dx * 0.46f, cubeCenterY + dy * 0.55f)
                    lineTo(cx - dx * 0.58f, cubeCenterY - dy * 0.25f)
                    close()
                }
                drawPath(
                    path = innerShield,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.32f),
                            Color(0xFFFEF08A).copy(alpha = 0.24f),
                            Color(0xFFF59E0B).copy(alpha = 0.30f)
                        )
                    )
                )
                drawPath(
                    path = innerShield,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFBBF24), Color(0xFFD97706))
                    ),
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )

                // Specular Glass Facet Borders
                val outerHex = Path().apply {
                    moveTo(topVertex.x, topVertex.y)
                    lineTo(rightVertex.x, rightVertex.y)
                    lineTo(bottomRightVertex.x, bottomRightVertex.y)
                    lineTo(bottomVertex.x, bottomVertex.y)
                    lineTo(bottomLeftVertex.x, bottomLeftVertex.y)
                    lineTo(leftVertex.x, leftVertex.y)
                    close()
                }
                drawPath(
                    path = outerHex,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFEF08A), Color(0xFF34D399), Color(0xFFF59E0B))
                    ),
                    style = Stroke(width = 1.8.dp.toPx(), join = StrokeJoin.Round)
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.78f),
                    start = leftVertex,
                    end = centerVertex,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.78f),
                    start = rightVertex,
                    end = centerVertex,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFFEF08A).copy(alpha = 0.82f),
                    start = centerVertex,
                    end = bottomVertex,
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 4) 3D Isometric Royal Golden Academic Crown Floating Above the Crystal Cube
                val crownBaseY = h * 0.25f + crownHoverY
                val crownW = w * 0.34f
                val crownPath = Path().apply {
                    moveTo(cx - crownW * 0.44f, crownBaseY)
                    lineTo(cx - crownW * 0.52f, crownBaseY - h * 0.10f)
                    lineTo(cx - crownW * 0.22f, crownBaseY - h * 0.04f)
                    lineTo(cx, crownBaseY - h * 0.14f)
                    lineTo(cx + crownW * 0.22f, crownBaseY - h * 0.04f)
                    lineTo(cx + crownW * 0.52f, crownBaseY - h * 0.10f)
                    lineTo(cx + crownW * 0.44f, crownBaseY)
                    quadraticBezierTo(cx, crownBaseY + h * 0.03f, cx - crownW * 0.44f, crownBaseY)
                    close()
                }
                drawPath(
                    path = crownPath,
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309)),
                        start = Offset(cx - crownW * 0.5f, crownBaseY - h * 0.14f),
                        end = Offset(cx + crownW * 0.5f, crownBaseY)
                    )
                )
                drawPath(
                    path = crownPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 1.3.dp.toPx(), join = StrokeJoin.Round)
                )
                // Glowing Jewel Orbs on the 3 Crown Peaks
                val crownJewels = listOf(
                    Offset(cx - crownW * 0.52f, crownBaseY - h * 0.11f),
                    Offset(cx, crownBaseY - h * 0.155f),
                    Offset(cx + crownW * 0.52f, crownBaseY - h * 0.11f)
                )
                crownJewels.forEachIndexed { idx, jewelPos ->
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (idx == 1) {
                                listOf(Color.White, Color(0xFF6EE7B7), Color(0xFF059669))
                            } else {
                                listOf(Color.White, Color(0xFFFEF08A), Color(0xFFF59E0B))
                            },
                            center = Offset(jewelPos.x - 1.5f, jewelPos.y - 1.5f),
                            radius = w * 0.035f
                        ),
                        radius = if (idx == 1) w * 0.030f else w * 0.024f,
                        center = jewelPos
                    )
                }

                // 5) 3D Floating Glassmorphic Orbs (Golden & Emerald Spheres Orbiting in 3D Space)
                val orbConfigs = listOf(
                    Triple(0.16f, 0.34f, true),
                    Triple(0.85f, 0.30f, false),
                    Triple(0.14f, 0.64f, false),
                    Triple(0.86f, 0.62f, true)
                )
                orbConfigs.forEachIndexed { index, (relX, relY, isGoldOrb) ->
                    val orbFloatY = sin(phaseRad + index * 1.4f) * (h * 0.036f)
                    val orbFloatX = cos(phaseRad * 0.8f + index * 1.1f) * (w * 0.018f)
                    val orbCenter = Offset(w * relX + orbFloatX, h * relY + orbFloatY)
                    val orbRadius = if (index % 2 == 0) w * 0.068f else w * 0.056f

                    // Outer soft orb glow
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                (if (isGoldOrb) Color(0xFFFDE68A) else Color(0xFF6EE7B7)).copy(alpha = 0.45f),
                                Color.Transparent
                            ),
                            center = orbCenter,
                            radius = orbRadius * 1.75f
                        ),
                        radius = orbRadius * 1.75f,
                        center = orbCenter
                    )

                    // 3D Volumetric Sphere Body
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = if (isGoldOrb) {
                                listOf(
                                    Color.White,
                                    Color(0xFFFEF08A),
                                    Color(0xFFF59E0B),
                                    Color(0xFF92400E)
                                )
                            } else {
                                listOf(
                                    Color.White,
                                    Color(0xFFA7F3D0),
                                    Color(0xFF10B981),
                                    Color(0xFF064E3B)
                                )
                            },
                            center = Offset(orbCenter.x - orbRadius * 0.32f, orbCenter.y - orbRadius * 0.32f),
                            radius = orbRadius * 1.35f
                        ),
                        radius = orbRadius,
                        center = orbCenter
                    )

                    // Glassmorphic Rim & Specular Highlight Dot
                    drawCircle(
                        color = Color.White.copy(alpha = 0.65f),
                        radius = orbRadius,
                        center = orbCenter,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.90f),
                        radius = orbRadius * 0.24f,
                        center = Offset(orbCenter.x - orbRadius * 0.30f, orbCenter.y - orbRadius * 0.30f)
                    )
                }
            }

            ThreeDIllustrationType.BOOKS_GRADUATION_CAP -> {
                // 1) Glowing emerald aura & neon base ring (pulsing)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = glowPulseAlpha),
                            Color(0xFF10B981).copy(alpha = glowPulseAlpha * 0.35f),
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
                    color = Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.32f).coerceAtMost(0.95f)),
                    topLeft = Offset(w * 0.06f, h * 0.66f),
                    size = Size(w * 0.88f, h * 0.24f),
                    style = Stroke(width = 2.6.dp.toPx())
                )
                drawOval(
                    color = Color(0xFFA7F3D0).copy(alpha = glowPulseAlpha),
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
                // 3D News Photo Frame Card in background + 3D News Megaphone in foreground
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // Back 3D Tilted News/Photo Gallery Frame Card
                rotate(degrees = -9f, pivot = Offset(w * 0.42f, h * 0.40f)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                        topLeft = Offset(w * 0.10f, h * 0.14f),
                        size = Size(w * 0.56f, h * 0.48f),
                        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                        topLeft = Offset(w * 0.10f, h * 0.14f),
                        size = Size(w * 0.56f, h * 0.48f),
                        cornerRadius = CornerRadius(9.dp.toPx(), 9.dp.toPx()),
                        style = Stroke(width = 1.8.dp.toPx())
                    )
                    // Inner photo landscape & sun inside the news frame
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color(0xFF0F3D5E), Color(0xFF059669))),
                        topLeft = Offset(w * 0.15f, h * 0.19f),
                        size = Size(w * 0.46f, h * 0.28f),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = w * 0.045f,
                        center = Offset(w * 0.24f, h * 0.27f)
                    )
                    val hillPath = Path().apply {
                        moveTo(w * 0.15f, h * 0.47f)
                        lineTo(w * 0.30f, h * 0.33f)
                        lineTo(w * 0.44f, h * 0.42f)
                        lineTo(w * 0.54f, h * 0.35f)
                        lineTo(w * 0.61f, h * 0.47f)
                        close()
                    }
                    drawPath(hillPath, color = Color(0xFF34D399).copy(alpha = 0.85f))
                }

                // Handle (Emerald 3D grip)
                val handlePath = Path().apply {
                    moveTo(w * 0.34f, h * 0.60f)
                    lineTo(w * 0.44f, h * 0.57f)
                    lineTo(w * 0.50f, h * 0.84f)
                    lineTo(w * 0.39f, h * 0.87f)
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
                    topLeft = Offset(w * 0.20f, h * 0.46f),
                    size = Size(w * 0.20f, h * 0.24f)
                )

                // White/Silver 3D Cone Body
                val coneBody = Path().apply {
                    moveTo(w * 0.30f, h * 0.47f)
                    lineTo(w * 0.70f, h * 0.24f)
                    lineTo(w * 0.78f, h * 0.66f)
                    lineTo(w * 0.34f, h * 0.68f)
                    close()
                }
                drawPath(
                    path = coneBody,
                    brush = Brush.verticalGradient(
                        listOf(Color.White, Color(0xFFE2E8F0), Color(0xFFCBD5E1))
                    )
                )

                // Golden & Emerald middle band on cone
                val emeraldBand = Path().apply {
                    moveTo(w * 0.28f, h * 0.48f)
                    lineTo(w * 0.42f, h * 0.41f)
                    lineTo(w * 0.47f, h * 0.67f)
                    lineTo(w * 0.32f, h * 0.68f)
                    close()
                }
                drawPath(
                    path = emeraldBand,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                    )
                )

                // Front Mouth Oval (3D shaded interior)
                rotate(degrees = -12f, pivot = Offset(w * 0.74f, h * 0.45f)) {
                    drawOval(
                        color = Color(0xFFF8FAFC),
                        topLeft = Offset(w * 0.64f, h * 0.22f),
                        size = Size(w * 0.20f, h * 0.46f)
                    )
                    drawOval(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFF0F3D5E), Color(0xFF047857), Color(0xFF34D399))
                        ),
                        topLeft = Offset(w * 0.67f, h * 0.26f),
                        size = Size(w * 0.14f, h * 0.38f)
                    )
                    drawCircle(
                        color = Color(0xFFFBBF24),
                        radius = w * 0.045f,
                        center = Offset(w * 0.72f, h * 0.45f)
                    )
                }

                // 3 Animated Golden/Emerald Sound Waves on the right
                val waveColor = Color(0xFFFEF08A).copy(alpha = (glowPulseAlpha + 0.38f).coerceAtMost(1f))
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.84f, h * 0.30f),
                    end = Offset(w * (0.84f + 0.11f * waveExpandFactor), h * (0.30f - 0.08f * waveExpandFactor)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.86f, h * 0.44f),
                    end = Offset(w * (0.86f + 0.12f * waveExpandFactor), h * 0.42f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = waveColor,
                    start = Offset(w * 0.84f, h * 0.57f),
                    end = Offset(w * (0.84f + 0.11f * waveExpandFactor), h * (0.57f + 0.07f * waveExpandFactor)),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            ThreeDIllustrationType.DIRECT_CHAT_ENVELOPE_3D -> {
                // Unique 3D Educational Direct Messaging: 2 Overlapping 3D Chat Bubbles (White, Gold & Emerald) + Certified Envelope + Glowing Dots
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.15f).coerceAtMost(0.75f)), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // Back 3D Emerald/Navy Chat Bubble (Top-Right)
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF0F3D5E))),
                    topLeft = Offset(w * 0.32f, h * 0.12f),
                    size = Size(w * 0.56f, h * 0.38f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                val backTail = Path().apply {
                    moveTo(w * 0.72f, h * 0.48f)
                    lineTo(w * 0.84f, h * 0.58f)
                    lineTo(w * 0.62f, h * 0.48f)
                    close()
                }
                drawPath(backTail, color = Color(0xFF047857))
                // 3 Glowing Golden Communication Dots inside Back Bubble
                val dotAlpha = (glowPulseAlpha + 0.35f).coerceAtMost(1f)
                drawCircle(color = Color(0xFFFEF08A).copy(alpha = dotAlpha), radius = w * 0.036f, center = Offset(w * 0.48f, h * 0.31f))
                drawCircle(color = Color.White.copy(alpha = dotAlpha), radius = w * 0.036f, center = Offset(w * 0.60f, h * 0.31f))
                drawCircle(color = Color(0xFFFEF08A).copy(alpha = dotAlpha), radius = w * 0.036f, center = Offset(w * 0.72f, h * 0.31f))

                // Front 3D Pure White & Gold Chat Bubble (Center-Left)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
                    topLeft = Offset(w * 0.10f, h * 0.34f),
                    size = Size(w * 0.58f, h * 0.40f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                    topLeft = Offset(w * 0.10f, h * 0.34f),
                    size = Size(w * 0.58f, h * 0.40f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                val frontTail = Path().apply {
                    moveTo(w * 0.22f, h * 0.72f)
                    lineTo(w * 0.14f, h * 0.84f)
                    lineTo(w * 0.34f, h * 0.72f)
                    close()
                }
                drawPath(frontTail, color = Color(0xFFF1F5F9))
                // Message lines inside white chat bubble
                drawRoundRect(
                    color = Color(0xFF059669),
                    topLeft = Offset(w * 0.18f, h * 0.45f),
                    size = Size(w * 0.40f, h * 0.045f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF64748B).copy(alpha = 0.65f),
                    topLeft = Offset(w * 0.18f, h * 0.54f),
                    size = Size(w * 0.30f, h * 0.042f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )

                // Certified Golden Official Envelope on Bottom-Right
                rotate(degrees = 6f, pivot = Offset(w * 0.68f, h * 0.72f)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))),
                        topLeft = Offset(w * 0.46f, h * 0.58f),
                        size = Size(w * 0.44f, h * 0.30f),
                        cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                    )
                    val flap = Path().apply {
                        moveTo(w * 0.46f, h * 0.59f)
                        lineTo(w * 0.68f, h * 0.74f)
                        lineTo(w * 0.90f, h * 0.59f)
                        close()
                    }
                    drawPath(flap, color = Color(0xFFFFFBEB).copy(alpha = 0.88f))
                    // Emerald official seal on envelope flap
                    drawCircle(
                        color = Color(0xFF059669),
                        radius = w * 0.055f,
                        center = Offset(w * 0.68f, h * 0.73f)
                    )
                }
            }

            ThreeDIllustrationType.SOCIAL_COMMUNITY_3D -> {
                // Original Elegant 3D Onboarding Communication & School Community Illustration (preserved for Onboarding / Intro Screen)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.2f).coerceAtMost(0.78f)), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // Back 3D Glassmorphic Card / Bubble (Royal Emerald)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF047857))
                    ),
                    topLeft = Offset(w * 0.10f, h * 0.18f),
                    size = Size(w * 0.58f, h * 0.42f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.45f),
                    topLeft = Offset(w * 0.10f, h * 0.18f),
                    size = Size(w * 0.58f, h * 0.42f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.85f),
                    start = Offset(w * 0.20f, h * 0.32f),
                    end = Offset(w * 0.56f, h * 0.32f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFFA7F3D0),
                    start = Offset(w * 0.20f, h * 0.43f),
                    end = Offset(w * 0.46f, h * 0.43f),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Front 3D Golden-White Glassmorphic Card / Bubble
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFFEF9C3), Color(0xFFFDE68A))
                    ),
                    topLeft = Offset(w * 0.32f, h * 0.42f),
                    size = Size(w * 0.58f, h * 0.42f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFFF59E0B).copy(alpha = 0.65f),
                    topLeft = Offset(w * 0.32f, h * 0.42f),
                    size = Size(w * 0.58f, h * 0.42f),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // Golden Star Badge inside front card
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFBBF24), Color(0xFFD97706))
                    ),
                    radius = w * 0.08f,
                    center = Offset(w * 0.46f, h * 0.63f)
                )
                drawLine(
                    color = Color(0xFF065F46),
                    start = Offset(w * 0.58f, h * 0.57f),
                    end = Offset(w * 0.80f, h * 0.57f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF059669),
                    start = Offset(w * 0.58f, h * 0.68f),
                    end = Offset(w * 0.74f, h * 0.68f),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            ThreeDIllustrationType.STUDENT_CREATIVITY_SHOWCASE_3D -> {
                // أنيميشن سوشيال ميديا وتفاعل مجتمع الطلبة ثلاثي الأبعاد (Pure 3D Social Media & Community Feed Animation)
                val pulseScale = 0.94f + (glowPulseAlpha * 0.22f)

                // 1) Soft Emerald, Rose & Golden Ambient Aura + Pedestal Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.16f).coerceAtMost(0.70f)),
                            Color(0xFFF43F5E).copy(alpha = glowPulseAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy * 0.95f),
                        radius = w * 0.54f
                    ),
                    radius = w * 0.54f,
                    center = Offset(cx, cy * 0.95f)
                )

                drawOval(
                    color = Color(0xFF042F2E).copy(alpha = 0.38f),
                    topLeft = Offset(w * 0.16f, h * 0.80f),
                    size = Size(w * 0.68f, h * 0.11f)
                )
                drawOval(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFFFEF08A), Color(0xFF10B981))
                    ),
                    topLeft = Offset(w * 0.12f, h * 0.78f),
                    size = Size(w * 0.76f, h * 0.14f),
                    style = Stroke(width = 2.dp.toPx())
                )

                // 2) Secondary Floating 3D Social Feed Card in the Back (بطاقة منشور خلفي عائم)
                rotate(degrees = -9f, pivot = Offset(w * 0.44f, h * 0.44f)) {
                    drawRoundRect(
                        color = Color(0xFF042F2E).copy(alpha = 0.32f),
                        topLeft = Offset(w * 0.15f, h * 0.20f - floatOffsetY * 0.25f),
                        size = Size(w * 0.56f, h * 0.44f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFFD1FAE5).copy(alpha = 0.88f),
                                Color(0xFFA7F3D0).copy(alpha = 0.82f)
                            )
                        ),
                        topLeft = Offset(w * 0.13f, h * 0.18f - floatOffsetY * 0.25f),
                        size = Size(w * 0.56f, h * 0.44f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.75f),
                        topLeft = Offset(w * 0.13f, h * 0.18f - floatOffsetY * 0.25f),
                        size = Size(w * 0.56f, h * 0.44f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                }

                // 3) Primary 3D Glassmorphic Social Post Card in the Front (بطاقة منشور طلابي تفاعلي ثلاثية الأبعاد)
                val cardLeft = w * 0.20f
                val cardTop = h * 0.24f + floatOffsetY * 0.35f
                val cardW = w * 0.62f
                val cardH = h * 0.52f

                // Drop shadow
                drawRoundRect(
                    color = Color(0xFF022C22).copy(alpha = 0.36f),
                    topLeft = Offset(cardLeft + w * 0.025f, cardTop + h * 0.03f),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                // Glass card body
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.White,
                            Color(0xFFECFDF5),
                            Color(0xFFD1FAE5)
                        ),
                        start = Offset(cardLeft, cardTop),
                        end = Offset(cardLeft + cardW, cardTop + cardH)
                    ),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color.White, Color(0xFFFEF08A), Color(0xFF10B981))
                    ),
                    topLeft = Offset(cardLeft, cardTop),
                    size = Size(cardW, cardH),
                    cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                    style = Stroke(width = 2.dp.toPx())
                )

                // Author Avatar Circle + Post Header Lines inside Front Social Card
                val avCx = cardLeft + cardW * 0.80f
                val avCy = cardTop + cardH * 0.22f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399), Color(0xFF047857))
                    ),
                    radius = w * 0.062f,
                    center = Offset(avCx, avCy)
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = w * 0.062f,
                    center = Offset(avCx, avCy),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawLine(
                    color = Color(0xFF065F46),
                    start = Offset(cardLeft + cardW * 0.32f, avCy - h * 0.025f),
                    end = Offset(cardLeft + cardW * 0.66f, avCy - h * 0.025f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = Color(0xFF10B981).copy(alpha = 0.65f),
                    start = Offset(cardLeft + cardW * 0.44f, avCy + h * 0.03f),
                    end = Offset(cardLeft + cardW * 0.66f, avCy + h * 0.03f),
                    strokeWidth = 2.2.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Media Preview Frame inside the Social Post Card
                val mediaTop = cardTop + cardH * 0.38f
                val mediaH = cardH * 0.36f
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF059669), Color(0xFF047857), Color(0xFF064E3B))
                    ),
                    topLeft = Offset(cardLeft + cardW * 0.10f, mediaTop),
                    size = Size(cardW * 0.80f, mediaH),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                // Sun/landscape accent inside media preview
                drawCircle(
                    color = Color(0xFFFDE047),
                    radius = w * 0.035f,
                    center = Offset(cardLeft + cardW * 0.26f, mediaTop + mediaH * 0.36f)
                )
                val hillsPath = Path().apply {
                    moveTo(cardLeft + cardW * 0.12f, mediaTop + mediaH * 0.88f)
                    lineTo(cardLeft + cardW * 0.38f, mediaTop + mediaH * 0.48f)
                    lineTo(cardLeft + cardW * 0.56f, mediaTop + mediaH * 0.72f)
                    lineTo(cardLeft + cardW * 0.72f, mediaTop + mediaH * 0.42f)
                    lineTo(cardLeft + cardW * 0.88f, mediaTop + mediaH * 0.88f)
                    close()
                }
                drawPath(
                    path = hillsPath,
                    color = Color(0xFF34D399).copy(alpha = 0.55f)
                )

                // Interaction bar dots at bottom of card
                drawCircle(
                    color = Color(0xFFF43F5E),
                    radius = w * 0.022f,
                    center = Offset(cardLeft + cardW * 0.18f, cardTop + cardH * 0.86f)
                )
                drawCircle(
                    color = Color(0xFF059669),
                    radius = w * 0.022f,
                    center = Offset(cardLeft + cardW * 0.30f, cardTop + cardH * 0.86f)
                )
                drawLine(
                    color = Color(0xFF64748B).copy(alpha = 0.55f),
                    start = Offset(cardLeft + cardW * 0.52f, cardTop + cardH * 0.86f),
                    end = Offset(cardLeft + cardW * 0.84f, cardTop + cardH * 0.86f),
                    strokeWidth = 2.4.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 4) 3D Pulsing Red/Pink Like Heart Badge (أيقونة إعجاب ❤️ ثلاثية الأبعاد نابضة)
                val heartCx = w * 0.79f
                val heartCy = h * 0.25f - floatOffsetY * 0.55f
                val heartRadius = w * 0.155f * pulseScale

                drawCircle(
                    color = Color(0xFF881337).copy(alpha = 0.32f),
                    radius = heartRadius,
                    center = Offset(heartCx + 2.dp.toPx(), heartCy + 3.dp.toPx())
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFFE4E6),
                            Color(0xFFFB7185),
                            Color(0xFFE11D48)
                        ),
                        center = Offset(heartCx - heartRadius * 0.25f, heartCy - heartRadius * 0.25f),
                        radius = heartRadius * 1.3f
                    ),
                    radius = heartRadius,
                    center = Offset(heartCx, heartCy)
                )
                drawCircle(
                    color = Color.White,
                    radius = heartRadius,
                    center = Offset(heartCx, heartCy),
                    style = Stroke(width = 1.8.dp.toPx())
                )

                // Sculpted 3D Heart Shape inside the badge
                val hs = heartRadius * 0.58f
                val heartPath = Path().apply {
                    moveTo(heartCx, heartCy + hs * 0.78f)
                    cubicTo(
                        heartCx - hs * 1.25f, heartCy + hs * 0.08f,
                        heartCx - hs * 0.95f, heartCy - hs * 0.92f,
                        heartCx, heartCy - hs * 0.30f
                    )
                    cubicTo(
                        heartCx + hs * 0.95f, heartCy - hs * 0.92f,
                        heartCx + hs * 1.25f, heartCy + hs * 0.08f,
                        heartCx, heartCy + hs * 0.78f
                    )
                    close()
                }
                drawPath(
                    path = heartPath,
                    color = Color.White
                )

                // 5) 3D Floating Comment Bubble on the Left (فقاعة تعليقات 💬 ثلاثية الأبعاد)
                val commCx = w * 0.18f
                val commCy = h * 0.58f + floatOffsetY * 0.45f
                val commW = w * 0.26f
                val commH = h * 0.20f

                drawRoundRect(
                    color = Color(0xFF042F2E).copy(alpha = 0.32f),
                    topLeft = Offset(commCx - commW * 0.5f + 2.dp.toPx(), commCy - commH * 0.5f + 3.dp.toPx()),
                    size = Size(commW, commH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))
                    ),
                    topLeft = Offset(commCx - commW * 0.5f, commCy - commH * 0.5f),
                    size = Size(commW, commH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(commCx - commW * 0.5f, commCy - commH * 0.5f),
                    size = Size(commW, commH),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // 3 typing/comment dots inside bubble
                listOf(-0.24f, 0f, 0.24f).forEach { dxFactor ->
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = w * 0.019f,
                        center = Offset(commCx + commW * dxFactor, commCy)
                    )
                }

                // 6) 3D Floating Share Node Badge on the Bottom Right (أيقونة مشاركة ثلاثية الأبعاد)
                val shareCx = w * 0.80f
                val shareCy = h * 0.66f - floatOffsetY * 0.35f
                val shareR = w * 0.115f
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B))
                    ),
                    radius = shareR,
                    center = Offset(shareCx, shareCy)
                )
                drawCircle(
                    color = Color(0xFFFEF08A),
                    radius = shareR,
                    center = Offset(shareCx, shareCy),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                val nLeft = Offset(shareCx - shareR * 0.38f, shareCy)
                val nTop = Offset(shareCx + shareR * 0.32f, shareCy - shareR * 0.36f)
                val nBot = Offset(shareCx + shareR * 0.32f, shareCy + shareR * 0.36f)
                drawLine(Color.White, nLeft, nTop, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(Color.White, nLeft, nBot, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(Color(0xFFFEF08A), radius = shareR * 0.18f, center = nLeft)
                drawCircle(Color.White, radius = shareR * 0.18f, center = nTop)
                drawCircle(Color.White, radius = shareR * 0.18f, center = nBot)
            }

            ThreeDIllustrationType.SYSTEM_BROADCAST_3D -> {
                // 3D Royal Indigo/Cyan System Radar & Global Signal Sphere for Developer System Broadcast
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF38BDF8).copy(alpha = (glowPulseAlpha + 0.22f).coerceAtMost(0.85f)), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )
                // Outer glowing cyan radar rings
                drawCircle(
                    color = Color(0xFF38BDF8).copy(alpha = 0.55f),
                    radius = w * 0.42f * waveExpandFactor.coerceIn(0.88f, 1.08f),
                    center = Offset(cx, cy),
                    style = Stroke(width = 2.dp.toPx())
                )
                drawOval(
                    color = Color(0xFF818CF8).copy(alpha = 0.75f),
                    topLeft = Offset(w * 0.12f, h * 0.34f),
                    size = Size(w * 0.76f, h * 0.32f),
                    style = Stroke(width = 2.2.dp.toPx())
                )
                // Core 3D Indigo/Cyan Sphere
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFBAE6FD), Color(0xFF0284C7), Color(0xFF1E1B4B)),
                        center = Offset(cx - w * 0.06f, cy - h * 0.06f),
                        radius = w * 0.28f
                    ),
                    radius = w * 0.24f,
                    center = Offset(cx, cy)
                )
                // Orbiting golden node
                drawCircle(
                    color = Color(0xFFFBBF24),
                    radius = w * 0.055f,
                    center = Offset(w * 0.78f, h * 0.36f)
                )
            }

            ThreeDIllustrationType.GLOWING_CHART_SPHERE,
            ThreeDIllustrationType.ATTENDANCE_GATE_CLOCK -> {
                // 3D Smart Gate & Digital ID Card (بوابة المدرسة الذكية والبطاقة الرقمية بدون ساعة برتقالية نهائياً)
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.18f).coerceAtMost(0.78f)),
                            Color(0xFF10B981).copy(alpha = glowPulseAlpha * 0.45f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // 1) 3D Smart Gate Portal Frame (Emerald & Royal Navy architectural portal)
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF047857), Color(0xFF0F3D5E))),
                    topLeft = Offset(w * 0.12f, h * 0.12f),
                    size = Size(w * 0.52f, h * 0.74f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Inner portal passage with glowing cyan/emerald scanner grid
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF042F2E), Color(0xFF064E3B))),
                    topLeft = Offset(w * 0.19f, h * 0.21f),
                    size = Size(w * 0.38f, h * 0.60f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                // Animated horizontal laser scan beam inside the Smart Gate
                val beamY = h * (0.30f + 0.35f * (glowPulseAlpha - 0.25f))
                drawLine(
                    color = Color(0xFF6EE7B7),
                    start = Offset(w * 0.21f, beamY),
                    end = Offset(w * 0.55f, beamY),
                    strokeWidth = 2.6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 2) Floating 3D Tilted Digital Student ID Card (Front-Right)
                rotate(degrees = 7f, pivot = Offset(w * 0.62f, h * 0.54f)) {
                    drawRoundRect(
                        brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                        topLeft = Offset(w * 0.36f, h * 0.28f),
                        size = Size(w * 0.54f, h * 0.52f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Golden Top Header Strip of Digital ID Card
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                        topLeft = Offset(w * 0.36f, h * 0.28f),
                        size = Size(w * 0.54f, h * 0.13f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                    // Student avatar circle on ID card
                    drawCircle(
                        color = Color(0xFF059669),
                        radius = w * 0.075f,
                        center = Offset(w * 0.48f, h * 0.51f)
                    )
                    // ID Card text & QR/barcode stripes
                    drawRoundRect(
                        color = Color(0xFF0F3D5E),
                        topLeft = Offset(w * 0.59f, h * 0.46f),
                        size = Size(w * 0.24f, h * 0.042f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFF059669).copy(alpha = 0.7f),
                        topLeft = Offset(w * 0.59f, h * 0.53f),
                        size = Size(w * 0.18f, h * 0.038f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    // Barcode stripes at bottom of ID card
                    for (b in 0..5) {
                        drawRect(
                            color = Color(0xFF0F172A).copy(alpha = 0.82f),
                            topLeft = Offset(w * (0.44f + b * 0.065f), h * 0.65f),
                            size = Size(w * 0.032f, h * 0.09f)
                        )
                    }
                }
            }

            ThreeDIllustrationType.EXAM_CERTIFICATE_PEN -> {
                // 3D Certified Exam Certificate Scroll + A+/100 Seal + Floating 3D Golden Pen
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.12f).coerceAtMost(0.7f)), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // Back 3D Folder / Exam Board (Navy & Emerald)
                rotate(-6f, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFF0F3D5E), Color(0xFF047857))),
                        topLeft = Offset(w * 0.18f, h * 0.16f),
                        size = Size(w * 0.58f, h * 0.68f),
                        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                    )
                }

                // Front 3D White Certificate Sheet with Golden Header Bar
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFF1F5F9))),
                    topLeft = Offset(w * 0.22f, h * 0.18f),
                    size = Size(w * 0.56f, h * 0.66f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFDE68A), Color(0xFFF59E0B))),
                    topLeft = Offset(w * 0.22f, h * 0.18f),
                    size = Size(w * 0.56f, h * 0.12f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )

                // Certificate horizontal text lines
                for (i in 0..2) {
                    val lineY = h * (0.38f + i * 0.09f)
                    drawRoundRect(
                        color = if (i == 0) Color(0xFF059669).copy(alpha = 0.65f) else Color(0xFF94A3B8).copy(alpha = 0.55f),
                        topLeft = Offset(w * 0.29f, lineY),
                        size = Size(w * (if (i == 2) 0.28f else 0.42f), h * 0.038f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }

                // 3D Golden Excellence Seal & Ribbons on Bottom-Left of Certificate
                val sealCenter = Offset(w * 0.38f, h * 0.70f)
                val ribbonLeft = Path().apply {
                    moveTo(w * 0.34f, h * 0.74f)
                    lineTo(w * 0.29f, h * 0.88f)
                    lineTo(w * 0.37f, h * 0.85f)
                    close()
                }
                val ribbonRight = Path().apply {
                    moveTo(w * 0.42f, h * 0.74f)
                    lineTo(w * 0.47f, h * 0.88f)
                    lineTo(w * 0.39f, h * 0.85f)
                    close()
                }
                drawPath(ribbonLeft, color = Color(0xFF059669))
                drawPath(ribbonRight, color = Color(0xFF047857))
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    radius = w * 0.11f,
                    center = sealCenter
                )
                // Star/Check inside Golden Seal
                val sealCheck = Path().apply {
                    moveTo(w * 0.34f, h * 0.70f)
                    lineTo(w * 0.37f, h * 0.73f)
                    lineTo(w * 0.43f, h * 0.66f)
                }
                drawPath(
                    path = sealCheck,
                    color = Color.White,
                    style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Floating 3D Golden & Emerald Fountain Pen on the Right
                rotate(-32f, pivot = Offset(w * 0.74f, h * 0.52f)) {
                    drawRoundRect(
                        brush = Brush.horizontalGradient(listOf(Color(0xFF10B981), Color(0xFF0F3D5E))),
                        topLeft = Offset(w * 0.69f, h * 0.24f),
                        size = Size(w * 0.10f, h * 0.44f),
                        cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                    )
                    // Golden ring & nib
                    drawRect(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(w * 0.69f, h * 0.62f),
                        size = Size(w * 0.10f, h * 0.05f)
                    )
                    val nibPath = Path().apply {
                        moveTo(w * 0.69f, h * 0.68f)
                        lineTo(w * 0.79f, h * 0.68f)
                        lineTo(w * 0.74f, h * 0.80f)
                        close()
                    }
                    drawPath(
                        path = nibPath,
                        brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFD97706)))
                    )
                }
            }

            ThreeDIllustrationType.TROPHY_STARS,
            ThreeDIllustrationType.ROYAL_HONOR_SHIELD -> {
                // Luxury 3D Crystal & Gold Excellence Trophy on a Royal Pedestal (كأس تفوق كريستالي/ذهبي ثلاثي الأبعاد فخم)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFDE68A).copy(alpha = (glowPulseAlpha + 0.22f).coerceAtMost(0.82f)),
                            Color(0xFF34D399).copy(alpha = glowPulseAlpha * 0.35f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy)
                )

                // 1) Royal Multi-Tiered Pedestal Base (Deep Navy & Gold)
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF0F172A))),
                    topLeft = Offset(w * 0.24f, h * 0.77f),
                    size = Size(w * 0.52f, h * 0.12f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Golden Nameplate on Pedestal
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))),
                    topLeft = Offset(w * 0.31f, h * 0.80f),
                    size = Size(w * 0.38f, h * 0.06f),
                    cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                )
                // Upper Gold Pedestal Collar
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFEF08A), Color(0xFFB45309))),
                    topLeft = Offset(w * 0.30f, h * 0.71f),
                    size = Size(w * 0.40f, h * 0.07f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // 2) Sculpted 3D Golden Stem
                val stemPath = Path().apply {
                    moveTo(w * 0.44f, h * 0.54f)
                    lineTo(w * 0.56f, h * 0.54f)
                    lineTo(w * 0.62f, h * 0.72f)
                    lineTo(w * 0.38f, h * 0.72f)
                    close()
                }
                drawPath(
                    path = stemPath,
                    brush = Brush.horizontalGradient(listOf(Color(0xFFD97706), Color(0xFFFEF08A), Color(0xFFB45309)))
                )

                // 3) Left & Right Ornate 3D Golden Trophy Handles
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    startAngle = 95f,
                    sweepAngle = 175f,
                    useCenter = false,
                    topLeft = Offset(w * 0.14f, h * 0.22f),
                    size = Size(w * 0.28f, h * 0.28f),
                    style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    startAngle = -90f,
                    sweepAngle = 175f,
                    useCenter = false,
                    topLeft = Offset(w * 0.58f, h * 0.22f),
                    size = Size(w * 0.28f, h * 0.28f),
                    style = Stroke(width = 4.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 4) 3D Crystal & Gold Cup Body
                val cupBody = Path().apply {
                    moveTo(w * 0.25f, h * 0.18f)
                    lineTo(w * 0.75f, h * 0.18f)
                    quadraticBezierTo(w * 0.73f, h * 0.52f, cx, h * 0.57f)
                    quadraticBezierTo(w * 0.27f, h * 0.52f, w * 0.25f, h * 0.18f)
                    close()
                }
                drawPath(
                    path = cupBody,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309))
                    )
                )
                // Inner Crystal Facet Highlight (gives luxury 3D crystal & gold sheen)
                val crystalFacet = Path().apply {
                    moveTo(w * 0.33f, h * 0.21f)
                    lineTo(w * 0.50f, h * 0.21f)
                    quadraticBezierTo(w * 0.48f, h * 0.46f, w * 0.42f, h * 0.50f)
                    quadraticBezierTo(w * 0.34f, h * 0.44f, w * 0.33f, h * 0.21f)
                    close()
                }
                drawPath(
                    path = crystalFacet,
                    brush = Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.82f), Color(0xFFFEF08A).copy(alpha = 0.25f)))
                )
                // Cup Top Golden Rim
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFFF59E0B), Color(0xFFFFFBEB), Color(0xFFD97706))),
                    topLeft = Offset(w * 0.23f, h * 0.15f),
                    size = Size(w * 0.54f, h * 0.055f),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // 5) Center 3D Crystal Diamond / Crown Emblem on the Cup
                val gemPath = Path().apply {
                    moveTo(cx, h * 0.26f)
                    lineTo(w * 0.59f, h * 0.36f)
                    lineTo(cx, h * 0.46f)
                    lineTo(w * 0.41f, h * 0.36f)
                    close()
                }
                drawPath(
                    path = gemPath,
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFFA7F3D0), Color(0xFF059669)))
                )
            }

            ThreeDIllustrationType.SCHEDULE_BOOKS_3D -> {
                // 3D Weekly Timetable Board + Stacked Study Books + Bookmark Ribbon
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )

                // 3D Timetable Card on Top-Right
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFE2E8F0))),
                    topLeft = Offset(w * 0.24f, h * 0.14f),
                    size = Size(w * 0.60f, h * 0.48f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(listOf(Color(0xFF059669), Color(0xFF0F3D5E))),
                    topLeft = Offset(w * 0.24f, h * 0.14f),
                    size = Size(w * 0.60f, h * 0.13f),
                    cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
                )
                for (r in 0..1) {
                    for (c in 0..2) {
                        drawRoundRect(
                            color = if ((r + c) % 2 == 0) Color(0xFF10B981).copy(alpha = 0.75f) else Color(0xFF94A3B8).copy(alpha = 0.45f),
                            topLeft = Offset(w * (0.30f + c * 0.17f), h * (0.32f + r * 0.12f)),
                            size = Size(w * 0.12f, h * 0.07f),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }
                }

                // Foreground 3D Study Book with Golden Bookmark
                drawRoundRect(
                    brush = Brush.linearGradient(listOf(Color(0xFF10B981), Color(0xFF047857))),
                    topLeft = Offset(w * 0.14f, h * 0.56f),
                    size = Size(w * 0.64f, h * 0.24f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(w * 0.18f, h * 0.60f),
                    size = Size(w * 0.56f, h * 0.16f),
                    cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
                // Golden Bookmark Ribbon
                drawRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.32f, h * 0.56f),
                    size = Size(w * 0.07f, h * 0.28f)
                )
            }

            ThreeDIllustrationType.CLOUD_OFFLINE_3D -> {
                // 3D Glassmorphic Cloud + Pulsing Signal Arcs for Offline Screen
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )
                // Pulsing Wi-Fi / Signal waves above the cloud
                val waveCol = Color(0xFF10B981).copy(alpha = (glowPulseAlpha + 0.25f).coerceAtMost(0.95f))
                drawArc(
                    color = waveCol,
                    startAngle = -140f,
                    sweepAngle = 100f,
                    useCenter = false,
                    topLeft = Offset(w * 0.26f, h * 0.10f),
                    size = Size(w * 0.48f, h * 0.42f),
                    style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                )
                drawArc(
                    color = waveCol.copy(alpha = waveCol.alpha * 0.75f),
                    startAngle = -135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(w * 0.34f, h * 0.18f),
                    size = Size(w * 0.32f, h * 0.30f),
                    style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3D Layered Cloud Body
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFD1FAE5))),
                    radius = w * 0.18f,
                    center = Offset(w * 0.36f, h * 0.56f)
                )
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFA7F3D0))),
                    radius = w * 0.22f,
                    center = Offset(w * 0.52f, h * 0.48f)
                )
                drawCircle(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFD1FAE5))),
                    radius = w * 0.16f,
                    center = Offset(w * 0.67f, h * 0.58f)
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color.White, Color(0xFFA7F3D0))),
                    topLeft = Offset(w * 0.22f, h * 0.52f),
                    size = Size(w * 0.58f, h * 0.22f),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
                // Golden sync badge on cloud
                drawCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
                    radius = w * 0.11f,
                    center = Offset(w * 0.68f, h * 0.68f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.045f,
                    center = Offset(w * 0.68f, h * 0.68f)
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
                // Exclusive 3D Secure Digital Portal & Floating Golden Key with Smart Scan Waves (UnifiedLoginScreen ONLY)
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.18f).coerceAtMost(0.80f)),
                            Color(0xFF059669).copy(alpha = 0.18f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy)
                )

                // 1) Animated Smart Scan Waves radiating from the portal
                for (ring in 1..2) {
                    val rScale = (waveExpandFactor * (0.34f + ring * 0.10f)).coerceIn(0.28f, 0.49f)
                    drawOval(
                        color = Color(0xFF6EE7B7).copy(alpha = (0.62f - ring * 0.18f).coerceAtLeast(0.15f)),
                        topLeft = Offset(cx - w * rScale, h * 0.68f - h * 0.08f * ring),
                        size = Size(w * rScale * 2f, h * 0.22f),
                        style = Stroke(width = 2.dp.toPx())
                    )
                }

                // 2) 3D Glowing Digital Portal Arch (Outer Emerald/Navy Frame + Inner Luminous Core)
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF0F3D5E), Color(0xFF064E3B), Color(0xFF022C22))
                    ),
                    topLeft = Offset(w * 0.20f, h * 0.14f),
                    size = Size(w * 0.54f, h * 0.68f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF34D399), Color(0xFFFBBF24), Color(0xFF10B981))
                    ),
                    topLeft = Offset(w * 0.20f, h * 0.14f),
                    size = Size(w * 0.54f, h * 0.68f),
                    cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx()),
                    style = Stroke(width = 2.5.dp.toPx())
                )
                // Inner glowing portal doorway
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF6EE7B7).copy(alpha = (glowPulseAlpha + 0.30f).coerceAtMost(0.9f)),
                            Color(0xFF10B981).copy(alpha = 0.45f),
                            Color(0xFF0F172A).copy(alpha = 0.85f)
                        )
                    ),
                    topLeft = Offset(w * 0.26f, h * 0.20f),
                    size = Size(w * 0.42f, h * 0.56f),
                    cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
                )
                // Animated horizontal laser scan line inside portal
                val scanY = h * (0.26f + 0.40f * ((floatOffsetY + 4.5f) / 9f).coerceIn(0f, 1f))
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, Color(0xFFFEF08A), Color(0xFF34D399), Color.Transparent)
                    ),
                    start = Offset(w * 0.24f, scanY),
                    end = Offset(w * 0.70f, scanY),
                    strokeWidth = 2.8.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3) Floating 3D Royal Golden Key unlocking the portal
                rotate(degrees = -18f + floatOffsetY * 1.4f, pivot = Offset(w * 0.66f, h * 0.52f)) {
                    // Key Bow (3D Golden Ring)
                    drawCircle(
                        brush = Brush.linearGradient(
                            listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))
                        ),
                        radius = w * 0.14f,
                        center = Offset(w * 0.74f, h * 0.46f)
                    )
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = w * 0.055f,
                        center = Offset(w * 0.74f, h * 0.46f)
                    )
                    // Key Shaft
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFD97706))
                        ),
                        topLeft = Offset(w * 0.36f, h * 0.43f),
                        size = Size(w * 0.28f, h * 0.065f),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                    )
                    // Key Teeth
                    drawRoundRect(
                        color = Color(0xFFFBBF24),
                        topLeft = Offset(w * 0.38f, h * 0.49f),
                        size = Size(w * 0.045f, h * 0.075f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                    drawRoundRect(
                        color = Color(0xFFF59E0B),
                        topLeft = Offset(w * 0.45f, h * 0.49f),
                        size = Size(w * 0.045f, h * 0.055f),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }

            ThreeDIllustrationType.USER_MANAGEMENT_AVATARS_3D -> {
                // Exclusive 3D User Management Illustration: Overlapping 3D Staff/Student/Parent Avatars + Rotating Orbital Ring
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy)
                )

                // Rotating orbital communication ring around the avatars
                rotate(degrees = continuousRotation, pivot = Offset(cx, cy * 1.05f)) {
                    drawOval(
                        brush = Brush.sweepGradient(
                            listOf(
                                Color(0xFF34D399).copy(alpha = 0.85f),
                                Color(0xFFFBBF24).copy(alpha = 0.85f),
                                Color(0xFF38BDF8).copy(alpha = 0.45f),
                                Color(0xFF34D399).copy(alpha = 0.85f)
                            )
                        ),
                        topLeft = Offset(w * 0.06f, h * 0.34f),
                        size = Size(w * 0.88f, h * 0.42f),
                        style = Stroke(width = 2.4.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFFEF08A),
                        radius = w * 0.038f,
                        center = Offset(w * 0.94f, h * 0.55f)
                    )
                    drawCircle(
                        color = Color(0xFF6EE7B7),
                        radius = w * 0.034f,
                        center = Offset(w * 0.06f, h * 0.55f)
                    )
                }

                // Back Left Avatar (Teacher - Royal Blue/Cyan 3D Bust)
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFF38BDF8), Color(0xFF0284C7))),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.10f, h * 0.48f),
                    size = Size(w * 0.36f, h * 0.34f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFE0F2FE), Color(0xFF38BDF8))),
                    radius = w * 0.095f,
                    center = Offset(w * 0.28f, h * 0.36f)
                )

                // Back Right Avatar (Student/Parent - Golden Amber 3D Bust)
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFBBF24), Color(0xFFD97706))),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.54f, h * 0.48f),
                    size = Size(w * 0.36f, h * 0.34f)
                )
                drawCircle(
                    brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                    radius = w * 0.095f,
                    center = Offset(w * 0.72f, h * 0.36f)
                )

                // Center Foreground Avatar (Principal/Lead - Emerald & White 3D Bust with Verified Badge)
                drawArc(
                    brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B))),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(w * 0.26f, h * 0.44f),
                    size = Size(w * 0.48f, h * 0.44f)
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color.White, Color(0xFFD1FAE5), Color(0xFF10B981)),
                        center = Offset(w * 0.47f, h * 0.27f)
                    ),
                    radius = w * 0.125f,
                    center = Offset(w * 0.50f, h * 0.30f)
                )
                // Golden Star Badge on Center Avatar
                drawCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                    radius = w * 0.065f,
                    center = Offset(w * 0.64f, h * 0.58f)
                )
            }

            ThreeDIllustrationType.SETTINGS_ROTATING_GEARS_3D -> {
                // Exclusive 3D Settings Illustration: Interlocking Rotating 3D Mechanical Gears + Floating 3D Control Sliders
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF34D399).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.52f
                    ),
                    radius = w * 0.52f,
                    center = Offset(cx, cy)
                )

                // 1) Floating 3D Glass Control Sliders Panel in Background
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF0F3D5E), Color(0xFF064E3B))),
                    topLeft = Offset(w * 0.12f, h * 0.18f),
                    size = Size(w * 0.58f, h * 0.54f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx())
                )
                drawRoundRect(
                    color = Color(0xFF6EE7B7).copy(alpha = 0.55f),
                    topLeft = Offset(w * 0.12f, h * 0.18f),
                    size = Size(w * 0.58f, h * 0.54f),
                    cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // Slider tracks & animated knobs
                for (row in 0..1) {
                    val yPos = h * (0.32f + row * 0.18f)
                    drawLine(
                        color = Color.White.copy(alpha = 0.35f),
                        start = Offset(w * 0.20f, yPos),
                        end = Offset(w * 0.60f, yPos),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    val knobX = if (row == 0) w * (0.44f + floatOffsetY * 0.008f) else w * (0.30f - floatOffsetY * 0.008f)
                    drawCircle(
                        brush = Brush.radialGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                        radius = w * 0.045f,
                        center = Offset(knobX, yPos)
                    )
                }

                // 2) Main Primary 3D Rotating Emerald/Silver Gear (Bottom-Right)
                val gear1Center = Offset(w * 0.64f, h * 0.58f)
                rotate(degrees = continuousRotation, pivot = gear1Center) {
                    for (tooth in 0 until 8) {
                        rotate(degrees = tooth * 45f, pivot = gear1Center) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(Color(0xFFA7F3D0), Color(0xFF10B981))),
                                topLeft = Offset(gear1Center.x - w * 0.045f, gear1Center.y - w * 0.25f),
                                size = Size(w * 0.09f, w * 0.12f),
                                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                            )
                        }
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFD1FAE5), Color(0xFF10B981), Color(0xFF047857)),
                            center = gear1Center
                        ),
                        radius = w * 0.185f,
                        center = gear1Center
                    )
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = w * 0.075f,
                        center = gear1Center
                    )
                }

                // 3) Secondary Interlocking 3D Golden Gear (Top-Right, Counter-Rotating)
                val gear2Center = Offset(w * 0.76f, h * 0.26f)
                rotate(degrees = -continuousRotation * 1.3f, pivot = gear2Center) {
                    for (tooth in 0 until 6) {
                        rotate(degrees = tooth * 60f, pivot = gear2Center) {
                            drawRoundRect(
                                brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                                topLeft = Offset(gear2Center.x - w * 0.035f, gear2Center.y - w * 0.16f),
                                size = Size(w * 0.07f, w * 0.08f),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                        }
                    }
                    drawCircle(
                        brush = Brush.radialGradient(
                            listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309)),
                            center = gear2Center
                        ),
                        radius = w * 0.115f,
                        center = gear2Center
                    )
                    drawCircle(
                        color = Color(0xFF0F172A),
                        radius = w * 0.045f,
                        center = gear2Center
                    )
                }
            }

            ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D -> {
                // Luxury 3D Academic Excellence Podium & Golden Star Award:
                // Multi-tiered crystal & polished emerald podium topped with an illuminated 3D open book
                // and an engraved 5-pointed golden star award, surrounded by 5 orbiting 3D 5-pointed golden stars.
                fun buildFivePointStarPath(centerX: Float, centerY: Float, outerR: Float, innerR: Float): Path {
                    return Path().apply {
                        for (i in 0 until 10) {
                            val r = if (i % 2 == 0) outerR else innerR
                            val angleRad = Math.toRadians((i * 36.0) - 90.0)
                            val px = centerX + (r * cos(angleRad)).toFloat()
                            val py = centerY + (r * sin(angleRad)).toFloat()
                            if (i == 0) moveTo(px, py) else lineTo(px, py)
                        }
                        close()
                    }
                }

                // 1) Radiant Golden & Emerald Ambient Halo
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFFFBBF24).copy(alpha = (glowPulseAlpha + 0.25f).coerceAtMost(0.88f)),
                            Color(0xFF10B981).copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy * 0.92f),
                        radius = w * 0.54f
                    ),
                    radius = w * 0.54f,
                    center = Offset(cx, cy * 0.92f)
                )

                // 2) Multi-Tiered 3D Crystal & Polished Emerald Podium Base
                // Lower wide crystal step
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF34D399), Color(0xFF059669), Color(0xFF064E3B))
                    ),
                    topLeft = Offset(w * 0.16f, h * 0.78f),
                    size = Size(w * 0.68f, h * 0.11f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFFEF08A))
                    ),
                    topLeft = Offset(w * 0.16f, h * 0.78f),
                    size = Size(w * 0.68f, h * 0.11f),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
                    style = Stroke(width = 1.6.dp.toPx())
                )
                // Upper polished emerald & gold pedestal step
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFF6EE7B7), Color(0xFF10B981), Color(0xFF047857))
                    ),
                    topLeft = Offset(w * 0.25f, h * 0.67f),
                    size = Size(w * 0.50f, h * 0.12f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
                )
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFFFDE68A), Color(0xFFD97706))
                    ),
                    topLeft = Offset(w * 0.25f, h * 0.67f),
                    size = Size(w * 0.50f, h * 0.12f),
                    cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx()),
                    style = Stroke(width = 1.4.dp.toPx())
                )

                // 3) Illuminated 3D Open Book resting on the Podium
                val leftPagePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.66f)
                    quadraticBezierTo(w * 0.36f, h * 0.62f, w * 0.22f, h * 0.65f)
                    lineTo(w * 0.25f, h * 0.50f)
                    quadraticBezierTo(w * 0.38f, h * 0.48f, w * 0.50f, h * 0.53f)
                    close()
                }
                val rightPagePath = Path().apply {
                    moveTo(w * 0.50f, h * 0.66f)
                    quadraticBezierTo(w * 0.64f, h * 0.62f, w * 0.78f, h * 0.65f)
                    lineTo(w * 0.75f, h * 0.50f)
                    quadraticBezierTo(w * 0.62f, h * 0.48f, w * 0.50f, h * 0.53f)
                    close()
                }
                drawPath(
                    path = leftPagePath,
                    brush = Brush.linearGradient(listOf(Color.White, Color(0xFFFEF9C3), Color(0xFFFDE68A)))
                )
                drawPath(
                    path = rightPagePath,
                    brush = Brush.linearGradient(listOf(Color(0xFFFEF9C3), Color.White, Color(0xFFFDE68A)))
                )
                drawPath(
                    path = leftPagePath,
                    color = Color(0xFFD97706),
                    style = Stroke(width = 1.4.dp.toPx())
                )
                drawPath(
                    path = rightPagePath,
                    color = Color(0xFFD97706),
                    style = Stroke(width = 1.4.dp.toPx())
                )

                // 4) Central 3D Engraved Golden 5-Pointed Star Award rising above the Open Book
                val centralStarOuter = w * 0.21f
                val centralStarInner = w * 0.095f
                val centralStarCy = h * 0.34f
                val centralStarShadowPath = buildFivePointStarPath(cx, centralStarCy + 3.dp.toPx(), centralStarOuter, centralStarInner)
                drawPath(
                    path = centralStarShadowPath,
                    color = Color(0xFF92400E).copy(alpha = 0.55f)
                )
                val centralStarPath = buildFivePointStarPath(cx, centralStarCy, centralStarOuter, centralStarInner)
                drawPath(
                    path = centralStarPath,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFFBEB), Color(0xFFFDE047), Color(0xFFF59E0B), Color(0xFFB45309))
                    )
                )
                drawPath(
                    path = centralStarPath,
                    color = Color.White.copy(alpha = 0.85f),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                // Inner engraved star core
                val innerCoreStar = buildFivePointStarPath(cx, centralStarCy, centralStarOuter * 0.52f, centralStarInner * 0.52f)
                drawPath(
                    path = innerCoreStar,
                    brush = Brush.radialGradient(
                        listOf(Color.White, Color(0xFFFEF08A), Color(0xFFD97706)),
                        center = Offset(cx, centralStarCy),
                        radius = centralStarOuter * 0.55f
                    )
                )

                // 5) 5 Orbiting & Sparkling 3D 5-Pointed Golden Stars around the Podium
                for (starIdx in 0 until 5) {
                    val baseAngleDeg = (starIdx * 72f) + (continuousRotation * 0.65f)
                    val rad = Math.toRadians(baseAngleDeg.toDouble())
                    val orbitX = cx + (w * 0.39f * cos(rad)).toFloat()
                    val orbitY = (h * 0.42f) + (h * 0.24f * sin(rad)).toFloat()
                    val miniStarScale = 0.85f + 0.20f * sin(rad + Math.toRadians(continuousRotation.toDouble())).toFloat()
                    val miniOuter = w * 0.068f * miniStarScale
                    val miniInner = miniOuter * 0.44f
                    val miniStarPath = buildFivePointStarPath(orbitX, orbitY, miniOuter, miniInner)
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = 0.38f),
                        radius = miniOuter * 1.35f,
                        center = Offset(orbitX, orbitY)
                    )
                    drawPath(
                        path = miniStarPath,
                        brush = Brush.verticalGradient(
                            listOf(Color.White, Color(0xFFFDE047), Color(0xFFF59E0B))
                        )
                    )
                }
            }

            ThreeDIllustrationType.CRESCENT_LANTERN_3D -> {
                // 3D Religious Occasion Illustration: Golden Crescent Moon & Glowing Lantern
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFFFBBF24).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )
                // Golden Crescent Moon
                drawCircle(
                    brush = Brush.linearGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B), Color(0xFFB45309))),
                    radius = w * 0.32f,
                    center = Offset(w * 0.46f, h * 0.50f)
                )
                drawCircle(
                    color = Color(0xFF064E3B),
                    radius = w * 0.25f,
                    center = Offset(w * 0.56f, h * 0.44f)
                )
                // Hanging 3D Glowing Star/Lantern
                drawLine(
                    color = Color(0xFFFEF08A),
                    start = Offset(w * 0.68f, h * 0.16f),
                    end = Offset(w * 0.68f, h * 0.42f),
                    strokeWidth = 2.dp.toPx()
                )
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                    topLeft = Offset(w * 0.60f, h * 0.42f),
                    size = Size(w * 0.16f, h * 0.22f),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            ThreeDIllustrationType.UPDATE_SPARK_3D -> {
                // 3D App Update & New Feature Illustration: Glowing 3D Cube / Rocket Sparkles
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(Color(0xFF38BDF8).copy(alpha = glowPulseAlpha), Color.Transparent),
                        center = Offset(cx, cy),
                        radius = w * 0.50f
                    ),
                    radius = w * 0.50f,
                    center = Offset(cx, cy)
                )
                rotate(degrees = continuousRotation * 0.5f, pivot = Offset(cx, cy)) {
                    drawRoundRect(
                        brush = Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF34D399), Color(0xFFFBBF24))),
                        topLeft = Offset(w * 0.24f, h * 0.24f),
                        size = Size(w * 0.52f, h * 0.52f),
                        cornerRadius = CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                        style = Stroke(width = 2.6.dp.toPx())
                    )
                }
                drawRoundRect(
                    brush = Brush.verticalGradient(listOf(Color(0xFF34D399), Color(0xFF059669))),
                    topLeft = Offset(w * 0.30f, h * 0.30f),
                    size = Size(w * 0.40f, h * 0.40f),
                    cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
                )
                // Upward 3D Boost Arrow inside cube
                val arrowPath = Path().apply {
                    moveTo(w * 0.50f, h * 0.36f)
                    lineTo(w * 0.62f, h * 0.50f)
                    lineTo(w * 0.54f, h * 0.50f)
                    lineTo(w * 0.54f, h * 0.64f)
                    lineTo(w * 0.46f, h * 0.64f)
                    lineTo(w * 0.46f, h * 0.50f)
                    lineTo(w * 0.38f, h * 0.50f)
                    close()
                }
                drawPath(path = arrowPath, color = Color(0xFFFEF08A))
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

            ThreeDIllustrationType.DIGITAL_LIBRARY_ILLUMINATED_BOOK_3D -> {
                // 1) Radiant Emerald & Gold Aura Behind the Open 3D Book
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFBBF24).copy(alpha = glowPulseAlpha * 0.85f),
                            Color(0xFF34D399).copy(alpha = glowPulseAlpha * 0.55f),
                            Color.Transparent
                        ),
                        center = Offset(cx, cy * 0.88f),
                        radius = w * 0.54f
                    ),
                    radius = w * 0.54f,
                    center = Offset(cx, cy * 0.88f)
                )

                // 2) Glowing Holographic Pedestal Rings Under the Book
                drawOval(
                    color = Color(0xFF34D399).copy(alpha = (glowPulseAlpha + 0.25f).coerceAtMost(0.92f)),
                    topLeft = Offset(w * 0.08f, h * 0.72f),
                    size = Size(w * 0.84f, h * 0.20f),
                    style = Stroke(width = 2.4.dp.toPx())
                )
                drawOval(
                    color = Color(0xFFFDE68A).copy(alpha = glowPulseAlpha * 0.9f),
                    topLeft = Offset(w * 0.14f, h * 0.75f),
                    size = Size(w * 0.72f, h * 0.14f),
                    style = Stroke(width = 1.6.dp.toPx())
                )

                // 3) Smart Luminous Beams / Light Paths Rising from the Open Book Pages
                val beamLeft = Path().apply {
                    moveTo(w * 0.48f, h * 0.62f)
                    cubicTo(
                        w * 0.36f, h * 0.44f,
                        w * 0.24f, h * 0.28f,
                        w * 0.20f, h * (0.14f - (waveExpandFactor - 1f) * 0.08f)
                    )
                }
                val beamCenter = Path().apply {
                    moveTo(w * 0.50f, h * 0.60f)
                    cubicTo(
                        w * 0.48f, h * 0.38f,
                        w * 0.52f, h * 0.24f,
                        w * 0.50f, h * (0.08f - (waveExpandFactor - 1f) * 0.10f)
                    )
                }
                val beamRight = Path().apply {
                    moveTo(w * 0.52f, h * 0.62f)
                    cubicTo(
                        w * 0.64f, h * 0.44f,
                        w * 0.76f, h * 0.28f,
                        w * 0.80f, h * (0.14f - (waveExpandFactor - 1f) * 0.08f)
                    )
                }

                // Wide soft light cone from open pages
                val lightCone = Path().apply {
                    moveTo(w * 0.26f, h * 0.62f)
                    lineTo(w * 0.14f, h * 0.14f)
                    lineTo(w * 0.86f, h * 0.14f)
                    lineTo(w * 0.74f, h * 0.62f)
                    close()
                }
                drawPath(
                    path = lightCone,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0xFFFEF08A).copy(alpha = glowPulseAlpha * 0.36f),
                            Color(0xFF6EE7B7).copy(alpha = glowPulseAlpha * 0.48f)
                        ),
                        startY = h * 0.12f,
                        endY = h * 0.64f
                    )
                )

                val beamBrush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFEF08A),
                        Color(0xFF34D399),
                        Color(0xFFF59E0B)
                    )
                )
                drawPath(beamLeft, brush = beamBrush, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))
                drawPath(beamCenter, brush = beamBrush, style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round))
                drawPath(beamRight, brush = beamBrush, style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round))

                // Floating Geometric Knowledge Nodes along the Light Beams
                val nodePositions = listOf(
                    Offset(w * 0.22f, h * 0.18f),
                    Offset(w * 0.50f, h * 0.11f),
                    Offset(w * 0.78f, h * 0.18f),
                    Offset(w * 0.35f, h * 0.30f),
                    Offset(w * 0.65f, h * 0.30f)
                )
                nodePositions.forEachIndexed { idx, pos ->
                    val pulse = if (idx % 2 == 0) waveExpandFactor else (2f - waveExpandFactor)
                    drawCircle(
                        color = Color(0xFFFEF08A).copy(alpha = 0.38f),
                        radius = 6.5.dp.toPx() * pulse,
                        center = pos
                    )
                    drawCircle(
                        color = if (idx == 1) Color(0xFFFBBF24) else Color(0xFFA7F3D0),
                        radius = 3.4.dp.toPx(),
                        center = pos
                    )
                }

                // 4) 3D Open Hardcover Book Base (Emerald & Royal Gold)
                val coverLeft = Path().apply {
                    moveTo(w * 0.50f, h * 0.76f)
                    lineTo(w * 0.10f, h * 0.66f)
                    lineTo(w * 0.14f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.58f)
                    close()
                }
                val coverRight = Path().apply {
                    moveTo(w * 0.50f, h * 0.76f)
                    lineTo(w * 0.90f, h * 0.66f)
                    lineTo(w * 0.86f, h * 0.46f)
                    lineTo(w * 0.50f, h * 0.58f)
                    close()
                }
                drawPath(
                    path = coverLeft,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF065F46), Color(0xFF047857), Color(0xFF0F3D5E))
                    )
                )
                drawPath(
                    path = coverRight,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF047857), Color(0xFF065F46), Color(0xFF0F3D5E))
                    )
                )

                // Golden Trim along Book Cover Edges
                drawPath(
                    path = coverLeft,
                    color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )
                drawPath(
                    path = coverRight,
                    color = Color(0xFFFBBF24).copy(alpha = 0.75f),
                    style = Stroke(width = 1.6.dp.toPx(), join = StrokeJoin.Round)
                )

                // 5) 3D Curved Illuminated Open Pages (Left & Right)
                val pageLeft = Path().apply {
                    moveTo(w * 0.50f, h * 0.72f)
                    quadraticBezierTo(w * 0.30f, h * 0.60f, w * 0.14f, h * 0.62f)
                    lineTo(w * 0.18f, h * 0.42f)
                    quadraticBezierTo(w * 0.34f, h * 0.42f, w * 0.50f, h * 0.54f)
                    close()
                }
                val pageRight = Path().apply {
                    moveTo(w * 0.50f, h * 0.72f)
                    quadraticBezierTo(w * 0.70f, h * 0.60f, w * 0.86f, h * 0.62f)
                    lineTo(w * 0.82f, h * 0.42f)
                    quadraticBezierTo(w * 0.66f, h * 0.42f, w * 0.50f, h * 0.54f)
                    close()
                }
                drawPath(
                    path = pageLeft,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFFFFFF), Color(0xFFECFDF5), Color(0xFFFEF9C3))
                    )
                )
                drawPath(
                    path = pageRight,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFEF9C3), Color(0xFFECFDF5), Color(0xFFFFFFFF))
                    )
                )

                // Subtle Luminous Text Lines on Both Open Pages
                for (lineIdx in 0..2) {
                    val yRatio = 0.49f + lineIdx * 0.055f
                    drawLine(
                        color = Color(0xFF059669).copy(alpha = 0.55f),
                        start = Offset(w * 0.23f, h * yRatio),
                        end = Offset(w * 0.44f, h * (yRatio + 0.04f)),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color(0xFFD97706).copy(alpha = 0.55f),
                        start = Offset(w * 0.77f, h * yRatio),
                        end = Offset(w * 0.56f, h * (yRatio + 0.04f)),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // Center Golden Spine & Bookmark Ribbon
                drawLine(
                    brush = Brush.verticalGradient(listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))),
                    start = Offset(w * 0.50f, h * 0.53f),
                    end = Offset(w * 0.50f, h * 0.82f),
                    strokeWidth = 3.5.dp.toPx(),
                    cap = StrokeCap.Round
                )
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
/**
 * شريط الإعلان المدمج بانسيابية موجية مع جسم الصفحة بدون إطار صندوقي تقليدي
 * (Seamless Wave Announcement Section with 3D Animated Megaphone)
 * يطابق الصورة المرجعية حرفياً:
 * - ممتد بانسيابية موجية ناعمة بدون حدود صندوقية (Zero Box BorderStroke)
 * - أقصى اليمين: دائرة خضراء داكنة بداخلها سهم أبيض يتبعها خط فاصل عمودي زمردي
 * - المنتصف: عنوان الإعلان بالزمردي الغامق العريض + النص الفرعي بالكحلي الداكن
 * - أقصى اليسار: مجسم مكبر الصوت ثلاثي الأبعاد المتحرك (3D Animated Megaphone)
 * - يدعم أزرار التحكم السريع للمدير عند وجود تبليغ عاجل مفعّل لمنع تكرار البطاقات نهائياً.
 */
/**
 * 1) شريط تنبيه المطور العام (Developer System Broadcast Banner):
 * - يظهر بأسلوب تقني ملكي مميز ومستقل كلياً عن إعلانات مدير المدرسة:
 * - تدرج كحلي/بنفسجي عميق (#1E1B4B إلى #312E81) مع إطار سماوي متوهج (#38BDF8)
 * - أيقونة رادار/نظام 3D متحركة (SYSTEM_BROADCAST_3D) وشارة "إشعار النظام العام".
 */
/**
 * مسار القص الموجي الانسيابي بعرض الشاشة بدون أي إطار صندوقي (Seamless Edge-to-Edge Wave Section Shape)
 */
private val SeamlessFullWidthWaveShape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h * 0.94f)
    cubicTo(
        w * 0.68f, h,
        w * 0.32f, h * 0.88f,
        0f, h * 0.96f
    )
    close()
}

/**
 * 1) مقطع تنبيه المطور العام المدمج بأمواج انسيابية بعرض الشاشة (Seamless Wave Developer Broadcast Section):
 * - بدون أي صندوق مستطيل أو RoundedCornerShape أو BorderStroke نهائياً.
 * - ممتد من الحافة للحافة (Edge-to-Edge) بتموجات مائية ناعمة ومجسم أنيميشن 3D ديناميكي حسب اختيار المطور.
 */
@Composable
fun DeveloperSystemBroadcastBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    badgeText: String = "إشعار النظام العام",
    iconKey: OccasionIconKey = OccasionIconKey.BELL,
    illustrationType: ThreeDIllustrationType = iconKey.toThreeDIllustrationType(),
    topOverlapPadding: Dp = 0.dp,
    onDismiss: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SeamlessFullWidthWaveShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD1FAE5),
                        Color(0xFFE0F7EE),
                        Color(0xFFA7F3D0)
                    )
                )
            )
    ) {
        // Subtle inner wave ribbon drawn on Canvas (zero box border)
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val ribbonPath = Path().apply {
                moveTo(0f, h * 0.65f)
                cubicTo(
                    w * 0.35f, h * 0.42f,
                    w * 0.65f, h * 0.88f,
                    w, h * 0.58f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = ribbonPath,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF10B981).copy(alpha = 0.14f),
                        Color(0xFF059669).copy(alpha = 0.08f),
                        Color(0xFF34D399).copy(alpha = 0.16f)
                    )
                )
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp + topOverlapPadding,
                    bottom = 18.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Right side in RTL: Circular dark-emerald action/dismiss icon + vertical emerald bar + text
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF047857))
                        .then(if (onDismiss != null) Modifier.clickable { onDismiss() } else Modifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (onDismiss != null) Icons.Filled.Close else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "إشعار النظام العام",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(2.5.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF059669).copy(alpha = 0.75f))
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = badgeText,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF047857),
                        maxLines = 1
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF065F46),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 12.8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Left side in RTL: Dynamic Animated Occasion Bezier Vector (No Trophy)
            AnimatedOccasionVectorCanvas(
                iconKey = iconKey,
                titleHint = "$badgeText $title $subtitle",
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

/**
 * 2) مقطع إعلان إدارة المدرسة المدمج بأمواج انسيابية بعرض الشاشة بدون أي صندوق أو إطار
 * (Seamless Full-Width Wave Principal Announcement Section - Matches Reference Image Strictly)
 */
@Composable
fun ThreeDMegaphoneAnnouncementBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    topOverlapPadding: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    isUrgentActive: Boolean = false,
    isAdmin: Boolean = false,
    onEditUrgent: (() -> Unit)? = null,
    onStopUrgent: (() -> Unit)? = null,
    onDismissUrgent: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SeamlessFullWidthWaveShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFFD1FAE5),
                        Color(0xFFE0F7EE),
                        Color(0xFFA7F3D0)
                    )
                )
            )
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val waveOverlay = Path().apply {
                moveTo(0f, h * 0.62f)
                cubicTo(
                    w * 0.30f, h * 0.38f,
                    w * 0.70f, h * 0.85f,
                    w, h * 0.55f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = waveOverlay,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFF10B981).copy(alpha = 0.14f),
                        Color(0xFF34D399).copy(alpha = 0.08f),
                        Color(0xFF059669).copy(alpha = 0.14f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 16.dp + topOverlapPadding,
                    bottom = 16.dp
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    // Right circular dark-emerald button with white arrow (matching reference image)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF047857)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Emerald vertical divider line
                    Box(
                        modifier = Modifier
                            .width(2.5.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color(0xFF059669).copy(alpha = 0.78f))
                    )

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.5.sp,
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

                // Left side (in RTL): 3D Animated Megaphone Illustration
                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.MEGAPHONE,
                    modifier = Modifier.size(72.dp)
                )
            }

            // Compact inline control chips when an urgent school alert is active
            if (isUrgentActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAdmin && onEditUrgent != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF047857).copy(alpha = 0.14f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onEditUrgent() }
                                .testTag("urgent_alert_banner_edit_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.Edit, contentDescription = null, tint = Color(0xFF047857), modifier = Modifier.size(13.dp))
                                Text("تعديل التبليغ", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                            }
                        }
                    }
                    if (isAdmin && onStopUrgent != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFFEF4444).copy(alpha = 0.16f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onStopUrgent() }
                                .testTag("urgent_alert_banner_delete_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.StopCircle, contentDescription = null, tint = Color(0xFFB91C1C), modifier = Modifier.size(13.dp))
                                Text("إيقاف البث", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF991B1B))
                            }
                        }
                    }
                    if (onDismissUrgent != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color(0xFF0F172A).copy(alpha = 0.08f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable { onDismissUrgent() }
                                .testTag("urgent_alert_banner_dismiss_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Filled.DoneAll, contentDescription = null, tint = Color(0xFF0F172A), modifier = Modifier.size(13.dp))
                                Text("تم الاطلاع", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 3) المقطع المتموج لتكريم أفضل 3 أساتذة تقييماً بالتناوب التلقائي في الشاشة الرئيسية بدون أي صندوق أو إطار
 * (Top 3 Auto-Rotating Rated Teachers Seamless Full-Width Wave Spotlight Section - Zero Box BorderStroke)
 */
@Composable
fun SeamlessWaveTeacherSpotlightSection(
    teacherName: String = "",
    teacherSubjects: String = "",
    averageRating: Float = 0f,
    totalRatingsCount: Int = 0,
    topTeachers: List<TeacherEvaluationSummary> = emptyList(),
    isAdmin: Boolean = false,
    topOverlapPadding: Dp = 0.dp,
    onOpenEvaluationSection: () -> Unit,
    onHideFromHome: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    data class SpotlightSlide(
        val rankIndex: Int,
        val rankLabel: String,
        val honorificTitle: String,
        val fullName: String,
        val subjects: String,
        val avgRating: Float,
        val count: Int
    )

    val slides = remember(topTeachers, teacherName, teacherSubjects, averageRating, totalRatingsCount) {
        if (topTeachers.isNotEmpty()) {
            topTeachers.take(3).mapIndexed { index, summary ->
                val rankTitle = when (index) {
                    0 -> if (summary.teacher.isFemale) "المتصدرة الأولى #1" else "المتصدر الأول #1"
                    1 -> "المركز الثاني #2"
                    else -> "المركز الثالث #3"
                }
                SpotlightSlide(
                    rankIndex = index,
                    rankLabel = rankTitle,
                    honorificTitle = summary.teacher.honorificTeacherTitle,
                    fullName = summary.teacher.fullName,
                    subjects = summary.teacher.specialty.ifBlank {
                        summary.teacher.teacherSubjectsList.joinToString(" • ").ifBlank { "الكادر التدريسي المتميز" }
                    },
                    avgRating = summary.averageRating,
                    count = summary.totalRatingsCount
                )
            }
        } else if (teacherName.isNotBlank()) {
            listOf(
                SpotlightSlide(
                    rankIndex = 0,
                    rankLabel = "المتصدر الأول #1",
                    honorificTitle = "الأستاذ",
                    fullName = teacherName,
                    subjects = teacherSubjects.ifBlank { "الكادر التدريسي المتميز" },
                    avgRating = averageRating,
                    count = totalRatingsCount
                )
            )
        } else {
            emptyList()
        }
    }

    if (slides.isEmpty()) return

    var activeIndex by remember(slides.size) { mutableIntStateOf(0) }
    LaunchedEffect(slides.size) {
        if (slides.size > 1) {
            while (true) {
                delay(4000L)
                activeIndex = (activeIndex + 1) % slides.size
            }
        }
    }
    val currentSlide = slides[activeIndex.coerceIn(0, slides.lastIndex)]

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(SeamlessFullWidthWaveShape)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF064E3B),
                        Color(0xFF047857),
                        Color(0xFF0F3D5E)
                    )
                )
            )
            .clickable { onOpenEvaluationSection() }
            .testTag("home_top_teacher_wave_spotlight")
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height
            val goldenRibbon = Path().apply {
                moveTo(0f, h * 0.66f)
                cubicTo(
                    w * 0.32f, h * 0.40f,
                    w * 0.68f, h * 0.90f,
                    w, h * 0.54f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = goldenRibbon,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color(0xFFFBBF24).copy(alpha = 0.22f),
                        Color(0xFF34D399).copy(alpha = 0.14f),
                        Color(0xFFF59E0B).copy(alpha = 0.24f)
                    )
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 18.dp,
                    end = 18.dp,
                    top = 18.dp + topOverlapPadding,
                    bottom = 18.dp
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AnimatedContent(
                    targetState = currentSlide,
                    transitionSpec = {
                        (slideInHorizontally { fullWidth -> fullWidth / 3 } + fadeIn(tween(380))) togetherWith
                            (slideOutHorizontally { fullWidth -> -fullWidth / 3 } + fadeOut(tween(280)))
                    },
                    label = "top_3_teachers_spotlight_anim",
                    modifier = Modifier.weight(1f)
                ) { slide ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.5.dp)
                                .height(72.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))
                                    )
                                )
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "نخبة الكادر التدريسي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFA7F3D0),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFBBF24).copy(alpha = 0.24f)
                            ) {
                                Text(
                                    text = slide.rankLabel,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF08A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }

                            Text(
                                text = "${slide.honorificTitle}: ${slide.fullName}",
                                style = MaterialTheme.typography.titleMedium,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Text(
                                text = slide.subjects,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFD1FAE5),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.18f)
                            ) {
                                Text(
                                    text = String.format(
                                        java.util.Locale.US,
                                        "%.1f / 5.0 (%d تقييم)",
                                        slide.avgRating,
                                        slide.count
                                    ),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFFFEF08A),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                ThreeDIllustrationBox(
                    type = ThreeDIllustrationType.TEACHER_EXCELLENCE_CROWN_3D,
                    modifier = Modifier.requiredSize(74.dp)
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.16f),
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(50))
                            .clickable { onOpenEvaluationSection() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFEF08A), modifier = Modifier.size(14.dp))
                            Text(
                                text = "عرض تقييمات الكادر التدريسي",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // 3 Glowing Indicator Dots for the Top 3 Teachers
                    if (slides.size > 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            slides.forEachIndexed { idx, _ ->
                                val isSelected = idx == activeIndex
                                Box(
                                    modifier = Modifier
                                        .height(6.dp)
                                        .width(if (isSelected) 18.dp else 6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            if (isSelected) Color(0xFFFEF08A) else Color.White.copy(alpha = 0.35f)
                                        )
                                        .clickable { activeIndex = idx }
                                )
                            }
                        }
                    }
                }

                if (isAdmin && onHideFromHome != null) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEF4444).copy(alpha = 0.24f),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onHideFromHome() }
                            .testTag("hide_top_teacher_from_home_btn")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null, tint = Color(0xFFFEE2E2), modifier = Modifier.size(13.dp))
                            Text(
                                text = "إزالة من الرئيسية",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
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

/**
 * القسم الترحيبي / الترويسي الملكي ثلاثي الأبعاد المدمج بأمواج انسيابية مع جسم الصفحة
 * (Seamless Wave-Curved 3D Royal Hero Section)
 * يطابق الصورة المرجعية حرفياً:
 * - حافة علوية وسفلية منحنية بموجة Bezier ناعمة بدلاً من الصندوق المستطيل ذي الإطار
 * - تدرج زمردي إلى كحلي عميق + شبكة نقاط مضيئة (Halftone Dot Matrix)
 * - كبسولات زجاجية علوية + مجسم ثلاثي الأبعاد متحرك على اليسار + شريط زر زجاجي سفلي اختياري.
 */
/**
 * 3D Isometric Academic Crown & Crystal Shield/Cube with Glass Orbs & 3-Layer Holographic Light Pedestal
 * for the Emerald Welcome Hero Banner (strictly no cartoon human faces).
 */
@Composable
fun WelcomeBannerBezierCharacter(
    modifier: Modifier = Modifier.size(108.dp)
) {
    ThreeDIllustrationBox(
        type = ThreeDIllustrationType.WELCOME_ACADEMIC_CHARACTER,
        modifier = modifier
    )
}

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
    // Custom Bezier Wave Shape: curved wave top & curved wave bottom matching reference image
    val waveHeroShape = remember {
        GenericShape { size, _ ->
            val w = size.width
            val h = size.height
            val topWaveDepth = (h * 0.065f).coerceIn(10f, 22f)
            val bottomWaveDepth = (h * 0.095f).coerceIn(16f, 30f)

            moveTo(0f, topWaveDepth * 0.45f)
            cubicTo(
                w * 0.32f, -topWaveDepth * 0.35f,
                w * 0.68f, topWaveDepth * 1.35f,
                w, topWaveDepth * 0.25f
            )
            lineTo(w, h - bottomWaveDepth)
            quadraticBezierTo(
                w * 0.50f, h + bottomWaveDepth * 0.85f,
                0f, h - bottomWaveDepth * 0.85f
            )
            close()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = waveHeroShape,
                ambientColor = Color(0xFF047857).copy(alpha = 0.32f),
                spotColor = Color(0xFF0F3D5E).copy(alpha = 0.38f)
            )
            .clip(waveHeroShape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF059669),
                        Color(0xFF047857),
                        Color(0xFF0F3D5E)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(950f, 680f)
                )
            )
    ) {
        // Halftone Dot Matrix, Wave Highlights & Radial Glow Overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            val w = size.width
            val h = size.height

            // Soft internal wave sheen
            val innerWave = Path().apply {
                moveTo(0f, h * 0.72f)
                cubicTo(
                    w * 0.35f, h * 0.60f,
                    w * 0.70f, h * 0.88f,
                    w, h * 0.74f
                )
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(
                path = innerWave,
                color = Color(0xFF0F3D5E).copy(alpha = 0.28f)
            )

            // Radial emerald highlight behind the 3D illustration (left side in RTL)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF34D399).copy(alpha = 0.32f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.22f, h * 0.48f),
                    radius = w * 0.44f
                ),
                radius = w * 0.44f,
                center = Offset(w * 0.22f, h * 0.48f)
            )

            // Halftone dot matrix in top-left and bottom-right corners
            val step = 11.dp.toPx()
            val dotRadius = 1.6.dp.toPx()
            for (row in 0..6) {
                for (col in 0..7) {
                    val alpha = ((1f - (row + col) / 14f) * 0.22f).coerceIn(0.03f, 0.22f)
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.04f + col * step, h * 0.10f + row * step)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = alpha),
                        radius = dotRadius,
                        center = Offset(w * 0.96f - col * step, h * 0.88f - row * step)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Row: Two Frosted Glass Pills (Guaranteed non-clipping for academic year & role badges)
            if (rightPillText.isNotBlank() || leftPillText.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (rightPillText.isNotBlank()) {
                        Surface(
                            modifier = Modifier.weight(1f, fill = false),
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = rightPillIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = rightPillText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(0.01f))

                    if (leftPillText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Icon(
                                    imageVector = leftPillIcon,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = leftPillText,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontSize = 11.sp,
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

            // Middle Row: Welcome Title & Subtitle on Right + 3D Animated Illustration on Left
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

                if (illustrationType == ThreeDIllustrationType.WELCOME_ACADEMIC_CHARACTER) {
                    WelcomeBannerBezierCharacter(
                        modifier = Modifier.size(108.dp)
                    )
                } else {
                    ThreeDIllustrationBox(
                        type = illustrationType,
                        modifier = Modifier.size(108.dp)
                    )
                }
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
                            fontSize = 13.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
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
            // 3D Royal Emerald & Gold Diamond Emblem (replaces mini bar chart)
            Canvas(modifier = Modifier.size(22.dp)) {
                val w = size.width
                val h = size.height
                val outerDiamond = Path().apply {
                    moveTo(w * 0.50f, h * 0.06f)
                    lineTo(w * 0.94f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.94f)
                    lineTo(w * 0.06f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = outerDiamond,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFF10B981), Color(0xFF047857), Color(0xFF0F3D5E))
                    )
                )
                val innerDiamond = Path().apply {
                    moveTo(w * 0.50f, h * 0.24f)
                    lineTo(w * 0.76f, h * 0.50f)
                    lineTo(w * 0.50f, h * 0.76f)
                    lineTo(w * 0.24f, h * 0.50f)
                    close()
                }
                drawPath(
                    path = innerDiamond,
                    brush = Brush.linearGradient(
                        listOf(Color(0xFFFEF08A), Color(0xFFF59E0B))
                    )
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
                // Top Row: Wide Comfortable Title & Unclipped Description on Right + 3D Smart Gate & ID Card on Left
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.Start,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontSize = 16.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 2
                        )
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.94f),
                            lineHeight = 17.sp,
                            maxLines = 3
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Vertical translucent white divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(58.dp)
                            .background(Color.White.copy(alpha = 0.30f))
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    // Left side (in RTL): 3D Smart Gate & Digital ID Card (76.dp)
                    ThreeDIllustrationBox(
                        type = ThreeDIllustrationType.ATTENDANCE_GATE_CLOCK,
                        modifier = Modifier.size(76.dp)
                    )
                }

                // Middle Row: 5 Frosted Glass Emerald Capsules in a single balanced row (Zero truncation on "متواجدون: 1")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val capsules = listOf(
                        "حاضرون: $presentCount" to 0.96f,
                        "غائبون: $absentCount" to 0.94f,
                        "دخلوا: $checkedInCount" to 0.92f,
                        "غادروا: $checkedOutCount" to 0.94f,
                        "متواجدون: $insideNowCount" to 1.24f
                    )
                    capsules.forEach { (pillLabel, pillWeight) ->
                        Surface(
                            modifier = Modifier.weight(pillWeight),
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.36f))
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = pillLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                // Bottom Row: Wide Sleek Pure White & Emerald Action Button ("عرض التفاصيل <") below the 5 capsules
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(50))
                        .clickable { onClick() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonText,
                            style = MaterialTheme.typography.labelLarge,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF047857),
                            maxLines = 1,
                            softWrap = false
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = Color(0xFF047857),
                            modifier = Modifier.size(18.dp)
                        )
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

/**
 * بطاقة الأقسام التعليمية والإدارية ثلاثية الأبعاد ذات الأمواج السفلية
 * (3D Wave Quick-Access Section Card)
 * تمنع اقتصاص العناوين الطويلة (مثل: "السجل الأكاديمي الشامل") وتمنح كل قسم عمقاً ثلاثي الأبعاد.
 */
@Composable
fun SectionWaveQuickCard3D(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isBlueAccent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    val waveColor1 = if (isBlueAccent) Color(0xFFDBEAFE) else Color(0xFFCCFBF1)
    val waveColor2 = if (isBlueAccent) Color(0xFFBFDBFE) else Color(0xFFA7F3D0)
    val iconCircleBg = if (isBlueAccent) Color(0xFFDBEAFE) else Color(0xFFD1FAE5)
    val iconTint = if (isBlueAccent) Color(0xFF1D4ED8) else Color(0xFF059669)

    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = iconTint.copy(alpha = 0.14f),
                spotColor = iconTint.copy(alpha = 0.18f)
            )
            .clip(RoundedCornerShape(22.dp))
            .clickable { onClick() }
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.2.dp, Color(0xFFE2E8F0))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 132.dp)
        ) {
            // Double smooth water wave drawn at the bottom of the card
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                val backWave = Path().apply {
                    moveTo(0f, h * 0.74f)
                    cubicTo(
                        w * 0.32f, h * 0.64f,
                        w * 0.68f, h * 0.86f,
                        w, h * 0.72f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = backWave, color = waveColor1.copy(alpha = 0.72f))

                val frontWave = Path().apply {
                    moveTo(0f, h * 0.82f)
                    cubicTo(
                        w * 0.38f, h * 0.92f,
                        w * 0.72f, h * 0.72f,
                        w, h * 0.84f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = frontWave, color = waveColor2.copy(alpha = 0.62f))
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = iconCircleBg,
                        border = BorderStroke(1.5.dp, Color.White),
                        shadowElevation = 3.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Surface(
                        shape = CircleShape,
                        color = iconCircleBg.copy(alpha = 0.65f),
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A),
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF475569),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * سطح ترويسة متموج ثلاثي الأبعاد متعدد الطبقات (WaveLayeredHeaderSurface)
 * يمنح ترويسة موحدة فاخرة بتدرج زمردي ملكي مع أمواج مائية وشبكة نقاط مضيئة
 * لاحتواء العنوان والمجسم الثلاثي الأبعاد وأزرار التبويبات الفرعية بسلاسة تامة.
 */
@Composable
fun WaveLayeredHeaderSurface(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 26.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = Color(0xFF047857).copy(alpha = 0.28f),
                spotColor = Color(0xFF047857).copy(alpha = 0.34f)
            )
            .clip(RoundedCornerShape(cornerRadius)),
        shape = RoundedCornerShape(cornerRadius),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF047857)),
        border = BorderStroke(1.2.dp, Color(0xFF34D399).copy(alpha = 0.45f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF059669),
                            Color(0xFF047857),
                            Color(0xFF065F46),
                            Color(0xFF0F3D5E)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(900f, 700f)
                    )
                )
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val w = size.width
                val h = size.height

                // Ambient emerald-gold glow
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF34D399).copy(alpha = 0.25f),
                            Color.Transparent
                        ),
                        center = Offset(w * 0.22f, h * 0.28f),
                        radius = w * 0.55f
                    ),
                    radius = w * 0.55f,
                    center = Offset(w * 0.22f, h * 0.28f)
                )

                // Subtle 3D dot matrix on top right & left corners
                val step = 16.dp.toPx()
                for (row in 0..5) {
                    for (col in 0..6) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.08f),
                            radius = 1.6.dp.toPx(),
                            center = Offset(w - 18.dp.toPx() - col * step, 18.dp.toPx() + row * step)
                        )
                    }
                }

                // Multi-layer flowing wave ribbons along the bottom
                val waveBack = Path().apply {
                    moveTo(0f, h * 0.68f)
                    cubicTo(
                        w * 0.32f, h * 0.56f,
                        w * 0.68f, h * 0.82f,
                        w, h * 0.64f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = waveBack,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.20f),
                            Color(0xFF042F2E).copy(alpha = 0.42f)
                        ),
                        startY = h * 0.56f,
                        endY = h
                    )
                )

                val waveFront = Path().apply {
                    moveTo(0f, h * 0.80f)
                    cubicTo(
                        w * 0.38f, h * 0.92f,
                        w * 0.72f, h * 0.70f,
                        w, h * 0.84f
                    )
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = waveFront,
                    color = Color.White.copy(alpha = 0.07f)
                )
            }

            content()
        }
    }
}





