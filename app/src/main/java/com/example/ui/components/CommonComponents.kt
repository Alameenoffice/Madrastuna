package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
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
 * Single-line Developer Credit Bar required at the bottom of Onboarding, Login, and Settings:
 * "تصميم وتطوير: أحمد أبو رسلان | 07512362567" with direct Call and WhatsApp buttons.
 */
@Composable
fun DeveloperCreditFooter(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = "الدعم الفني للمنصة",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الدعم الفني للمنصة التعليمية | 07512362567",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Direct Phone Call Button (tel:07512362567)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { ExternalIntents.dialPhoneNumber(context, "07512362567") }
                        .testTag("dev_call_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Call,
                            contentDescription = "اتصال بالمطور",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "اتصال",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }

                // Direct WhatsApp Button (https://wa.me/9647512362567)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WhatsAppGreen,
                    modifier = Modifier
                        .height(32.dp)
                        .clickable { ExternalIntents.openDeveloperWhatsApp(context) }
                        .testTag("dev_whatsapp_button")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Chat,
                            contentDescription = "واتساب المطور",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "واتساب",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
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

            // Animated Platform Badge & Automatic Discovered School Name
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.graphicsLayer(
                    alpha = reveal.coerceIn(0f, 1f),
                    scaleX = 0.88f + 0.12f * reveal,
                    scaleY = 0.88f + 0.12f * reveal,
                    translationY = (1f - reveal) * 28f
                )
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (isDarkMode) Color(0xFF064E3B) else DeepEmerald.copy(alpha = 0.10f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.2.dp,
                        GoldAccent.copy(alpha = 0.70f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = GoldAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "منصة مدرستي الذكية",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDarkMode) Color(0xFFFEF3C7) else DeepForestGreen
                        )
                    }
                }

                if (hasDiscoveredSchool) {
                    // Display the actual discovered school name & logo with a luxury Emerald/Gold badge card
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = if (isDarkMode) Color(0xFF064E3B).copy(alpha = 0.85f) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, GoldAccent),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag("splash_discovered_school_card")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (schoolLogoBase64.isNotBlank()) {
                                Base64AvatarImage(
                                    base64Data = schoolLogoBase64,
                                    fallbackIcon = Icons.Filled.School,
                                    contentDescription = discoveredSchoolName ?: "",
                                    size = 44.dp,
                                    tint = DeepEmerald
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.linearGradient(
                                                colors = listOf(DeepEmerald, Color(0xFF0D9488))
                                            )
                                        )
                                        .border(1.5.dp, GoldAccent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.School,
                                        contentDescription = null,
                                        tint = Color(0xFFFEF3C7),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.Start) {
                                Text(
                                    text = "المدرسة المكتشفة",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GoldDeep,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = discoveredSchoolName.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isDarkMode) Color.White else DeepForestGreen,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    // First launch before any school login: show official platform title only
                    Text(
                        text = "منصة مدرستي الذكية المتكاملة",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDarkMode) Color.White else DeepForestGreen,
                        textAlign = TextAlign.Center
                    )
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
            drawPath(ribbonPath, color = GoldDeep)

            // Starburst Laurel Points
            for (i in 0 until 12) {
                val angle = Math.toRadians((i * 30).toDouble())
                val sx = cx + (outerR * 1.06f * cos(angle)).toFloat()
                val sy = cy + (outerR * 1.06f * sin(angle)).toFloat()
                drawCircle(
                    color = GoldAccent,
                    radius = 5f,
                    center = Offset(sx, sy)
                )
            }

            // Main Gold Medallion
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFDE68A), GoldAccent, GoldDeep)
                ),
                radius = outerR,
                center = Offset(cx, cy)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.6f),
                radius = outerR * 0.78f,
                center = Offset(cx, cy),
                style = Stroke(width = 2.5f, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "#$rank",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF451A03),
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
