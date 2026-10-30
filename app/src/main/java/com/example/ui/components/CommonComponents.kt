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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GoldAccent
import com.example.ui.theme.GoldDeep
import com.example.ui.theme.WhatsAppGreen
import com.example.util.ExternalIntents
import com.example.util.ImageCompressor
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

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
                    contentDescription = "مطور المنصة",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "تصميم وتطوير: أحمد أبو رسلان | 07512362567",
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
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = Color.White,
    testTag: String = ""
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = 48.dp)
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = text,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Photo Picker Button that automatically compresses any chosen image via Canvas to
 * max 800px width and 0.7 JPEG quality Base64.
 */
@Composable
fun CompressedPhotoPickerButton(
    label: String,
    currentBase64: String,
    onImageCompressed: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val compressed = ImageCompressor.compressUriToBase64Jpeg(context, uri)
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

/**
 * Renders a Base64 compressed image or a clean vector fallback avatar/icon.
 */
@Composable
fun Base64AvatarImage(
    base64Data: String,
    fallbackIcon: ImageVector,
    contentDescription: String,
    size: Dp = 52.dp,
    tint: Color = MaterialTheme.colorScheme.primary
) {
    val bitmap = remember(base64Data) {
        ImageCompressor.decodeBase64ToImageBitmap(base64Data)
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.14f))
            .border(1.5.dp, tint.copy(alpha = 0.4f), CircleShape),
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
 * Interactive Lottie-style Vector Keyframe Scene for the 3 Onboarding Slides.
 */
@Composable
fun OnboardingAnimatedScene(
    slideIndex: Int,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "onboarding_lottie_scene")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_offset"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val rotationDeg by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(14000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation_deg"
    )

    Box(
        modifier = modifier
            .size(220.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(
                Brush.linearGradient(
                    colors = when (slideIndex) {
                        0 -> listOf(Color(0xFF064E3B), Color(0xFF059669))
                        1 -> listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
                        else -> listOf(Color(0xFF78350F), Color(0xFFD97706))
                    }
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            // Animated Orbiting Rings (Lottie JSON keyframe style)
            rotate(rotationDeg, pivot = Offset(cx, cy)) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.40f * pulseScale,
                    center = Offset(cx, cy),
                    style = Stroke(width = 4f)
                )
                for (i in 0 until 8) {
                    val angle = Math.toRadians((i * 45).toDouble())
                    val px = cx + (size.minDimension * 0.40f * cos(angle)).toFloat()
                    val py = cy + (size.minDimension * 0.40f * sin(angle)).toFloat()
                    drawCircle(
                        color = GoldAccent.copy(alpha = 0.85f),
                        radius = 6f,
                        center = Offset(px, py)
                    )
                }
            }

            // Inner glowing disc
            drawCircle(
                color = Color.White.copy(alpha = 0.16f),
                radius = size.minDimension * 0.28f * pulseScale,
                center = Offset(cx, cy + floatOffset)
            )

            // Floating decorative cards inside the scene
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(cx - 72f, cy - 68f + floatOffset),
                size = Size(144f, 136f),
                cornerRadius = CornerRadius(24f, 24f)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (floatOffset * 0.4f).dp)
        ) {
            Icon(
                imageVector = when (slideIndex) {
                    0 -> Icons.Filled.School
                    1 -> Icons.Filled.FactCheck
                    else -> Icons.Filled.EmojiEvents
                },
                contentDescription = "مشهد تفاعلي",
                tint = if (slideIndex == 2) Color(0xFFFDE68A) else Color.White,
                modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.28f)
            ) {
                Text(
                    text = when (slideIndex) {
                        0 -> "منصة مدرستي الذكية"
                        1 -> "متابعة فورية لحظة بلحظة"
                        else -> "لوحة الشرف والأوائل"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
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
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
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
                    text = if (heartCount > 0) "$heartCount" else emptyLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (isLikedByMe) Color(0xFF9F1239) else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    softWrap = false
                )
                if (heartCount > 0) {
                    Text(
                        text = "قلب",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLikedByMe) Color(0xFFBE123C) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}
