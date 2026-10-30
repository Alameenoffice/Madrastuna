package com.example.ui.components

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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.model.OccasionThemeConfig
import com.example.model.OccasionThemeType

fun parseHexColorSafe(hex: String, fallback: Color = Color(0xFF064E3B)): Color {
    return try {
        val cleaned = hex.trim().let { if (it.startsWith("#")) it else "#$it" }
        Color(android.graphics.Color.parseColor(cleaned))
    } catch (_: Exception) {
        fallback
    }
}

fun occasionThemeBrush(
    primaryHex: String,
    gradientHex: String
): Brush {
    val c1 = parseHexColorSafe(primaryHex, Color(0xFF064E3B))
    val c2 = parseHexColorSafe(gradientHex, Color(0xFF059669))
    return Brush.linearGradient(colors = listOf(c1, c2))
}

/**
 * Bespoke Vector / SVG Canvas Emblem for each Occasion Theme + Custom Uploaded Image Support.
 */
@Composable
fun OccasionVectorEmblem(
    themeType: OccasionThemeType,
    customIconBase64: String = "",
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    if (customIconBase64.isNotBlank()) {
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFFBBF24), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Base64AvatarImage(
                base64Data = customIconBase64,
                fallbackIcon = Icons.Filled.Palette,
                contentDescription = themeType.arabicLabel,
                size = size,
                tint = Color(0xFFFDE68A)
            )
        }
        return
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.Black.copy(alpha = 0.22f))
            .border(1.dp, Color(0xFFFBBF24).copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size * 0.72f)) {
            val w = this.size.width
            val h = this.size.height
            val gold = Color(0xFFFBBF24)
            val lightGold = Color(0xFFFDE68A)
            val emerald = Color(0xFF10B981)

            when (themeType) {
                OccasionThemeType.OFFICIAL -> {
                    // Royal Shield + Crown Emblem
                    val shield = Path().apply {
                        moveTo(w * 0.5f, h * 0.08f)
                        lineTo(w * 0.88f, h * 0.24f)
                        lineTo(w * 0.82f, h * 0.64f)
                        quadraticBezierTo(w * 0.5f, h * 0.94f, w * 0.5f, h * 0.94f)
                        quadraticBezierTo(w * 0.5f, h * 0.94f, w * 0.18f, h * 0.64f)
                        lineTo(w * 0.12f, h * 0.24f)
                        close()
                    }
                    drawPath(shield, color = emerald.copy(alpha = 0.35f))
                    drawPath(shield, color = gold, style = Stroke(width = w * 0.07f))
                    // Inner Royal Crown
                    val crown = Path().apply {
                        moveTo(w * 0.28f, h * 0.60f)
                        lineTo(w * 0.24f, h * 0.36f)
                        lineTo(w * 0.40f, h * 0.48f)
                        lineTo(w * 0.50f, h * 0.30f)
                        lineTo(w * 0.60f, h * 0.48f)
                        lineTo(w * 0.76f, h * 0.36f)
                        lineTo(w * 0.72f, h * 0.60f)
                        close()
                    }
                    drawPath(crown, color = lightGold)
                }

                OccasionThemeType.RAMADAN_EID -> {
                    // Golden Crescent Moon (Hilal) + Hanging Lantern (Fanoos)
                    drawCircle(
                        color = gold,
                        radius = w * 0.34f,
                        center = Offset(w * 0.56f, h * 0.54f)
                    )
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = w * 0.27f,
                        center = Offset(w * 0.67f, h * 0.48f)
                    )
                    // Hanging Lantern on Left
                    drawLine(
                        color = lightGold,
                        start = Offset(w * 0.28f, h * 0.08f),
                        end = Offset(w * 0.28f, h * 0.28f),
                        strokeWidth = w * 0.05f
                    )
                    val lantern = Path().apply {
                        moveTo(w * 0.28f, h * 0.26f)
                        lineTo(w * 0.40f, h * 0.36f)
                        lineTo(w * 0.36f, h * 0.58f)
                        lineTo(w * 0.28f, h * 0.66f)
                        lineTo(w * 0.20f, h * 0.58f)
                        lineTo(w * 0.16f, h * 0.36f)
                        close()
                    }
                    drawPath(lantern, color = lightGold)
                    // Sparkling Star
                    drawCircle(
                        color = lightGold,
                        radius = w * 0.06f,
                        center = Offset(w * 0.74f, h * 0.26f)
                    )
                }

                OccasionThemeType.NEW_YEAR -> {
                    // Festive Calendar Card + Golden Starburst
                    drawRoundRect(
                        color = lightGold,
                        topLeft = Offset(w * 0.14f, h * 0.22f),
                        size = Size(w * 0.72f, h * 0.68f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                    )
                    drawRoundRect(
                        color = Color(0xFFD97706),
                        topLeft = Offset(w * 0.14f, h * 0.22f),
                        size = Size(w * 0.72f, h * 0.22f),
                        cornerRadius = CornerRadius(w * 0.12f, w * 0.12f)
                    )
                    // Rings
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(w * 0.28f, h * 0.10f),
                        size = Size(w * 0.10f, h * 0.20f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                    drawRoundRect(
                        color = gold,
                        topLeft = Offset(w * 0.62f, h * 0.10f),
                        size = Size(w * 0.10f, h * 0.20f),
                        cornerRadius = CornerRadius(w * 0.04f, w * 0.04f)
                    )
                    // Center Star inside Calendar
                    val star = Path().apply {
                        moveTo(w * 0.50f, h * 0.48f)
                        lineTo(w * 0.56f, h * 0.60f)
                        lineTo(w * 0.69f, h * 0.60f)
                        lineTo(w * 0.59f, h * 0.68f)
                        lineTo(w * 0.63f, h * 0.80f)
                        lineTo(w * 0.50f, h * 0.72f)
                        lineTo(w * 0.37f, h * 0.80f)
                        lineTo(w * 0.41f, h * 0.68f)
                        lineTo(w * 0.31f, h * 0.60f)
                        lineTo(w * 0.44f, h * 0.60f)
                        close()
                    }
                    drawPath(star, color = Color(0xFFB45309))
                }

                OccasionThemeType.ACADEMIC_EXCELLENCE -> {
                    // Graduation Cap (Mortarboard) + Golden Trophy
                    val cap = Path().apply {
                        moveTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.92f, h * 0.28f)
                        lineTo(w * 0.50f, h * 0.46f)
                        lineTo(w * 0.08f, h * 0.28f)
                        close()
                    }
                    drawPath(cap, color = lightGold)
                    // Tassel
                    drawLine(
                        color = gold,
                        start = Offset(w * 0.82f, h * 0.30f),
                        end = Offset(w * 0.82f, h * 0.56f),
                        strokeWidth = w * 0.06f
                    )
                    // Trophy Cup
                    val cup = Path().apply {
                        moveTo(w * 0.28f, h * 0.48f)
                        lineTo(w * 0.72f, h * 0.48f)
                        lineTo(w * 0.64f, h * 0.74f)
                        lineTo(w * 0.36f, h * 0.74f)
                        close()
                    }
                    drawPath(cup, color = gold)
                    drawRect(
                        color = lightGold,
                        topLeft = Offset(w * 0.44f, h * 0.74f),
                        size = Size(w * 0.12f, h * 0.10f)
                    )
                    drawRoundRect(
                        color = lightGold,
                        topLeft = Offset(w * 0.32f, h * 0.84f),
                        size = Size(w * 0.36f, h * 0.08f),
                        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                    )
                }

                OccasionThemeType.NATIONAL_DAY -> {
                    // Sovereign Flag Pole & Waving Royal Banner
                    drawRoundRect(
                        color = lightGold,
                        topLeft = Offset(w * 0.18f, h * 0.10f),
                        size = Size(w * 0.08f, h * 0.82f),
                        cornerRadius = CornerRadius(w * 0.03f, w * 0.03f)
                    )
                    val flag = Path().apply {
                        moveTo(w * 0.26f, h * 0.14f)
                        cubicTo(w * 0.46f, h * 0.06f, w * 0.64f, h * 0.24f, w * 0.88f, h * 0.16f)
                        lineTo(w * 0.88f, h * 0.56f)
                        cubicTo(w * 0.64f, h * 0.64f, w * 0.46f, h * 0.46f, w * 0.26f, h * 0.54f)
                        close()
                    }
                    drawPath(flag, color = gold)
                    drawCircle(
                        color = Color(0xFF064E3B),
                        radius = w * 0.09f,
                        center = Offset(w * 0.57f, h * 0.35f)
                    )
                }

                OccasionThemeType.CUSTOM_MANUAL -> {
                    // Royal Custom Diamond & Palette Emblem
                    val diamond = Path().apply {
                        moveTo(w * 0.50f, h * 0.10f)
                        lineTo(w * 0.88f, h * 0.50f)
                        lineTo(w * 0.50f, h * 0.90f)
                        lineTo(w * 0.12f, h * 0.50f)
                        close()
                    }
                    drawPath(diamond, color = gold.copy(alpha = 0.30f))
                    drawPath(diamond, color = lightGold, style = Stroke(width = w * 0.07f))
                    drawCircle(
                        color = lightGold,
                        radius = w * 0.16f,
                        center = Offset(w * 0.50f, h * 0.50f)
                    )
                }
            }
        }
    }
}

/**
 * Luxury Visual Occasion Card for the Developer Control Panel.
 */
@Composable
fun LuxuryOccasionThemeCard(
    themeType: OccasionThemeType,
    isSelected: Boolean,
    customIconBase64: String = "",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val brush = occasionThemeBrush(themeType.defaultPrimaryHex, themeType.defaultGradientHex)
    val borderColor = if (isSelected) Color(0xFFFBBF24) else Color.White.copy(alpha = 0.22f)
    val borderWidth = if (isSelected) 2.2.dp else 1.dp

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .testTag("dev_occasion_card_${themeType.name}"),
        shape = RoundedCornerShape(16.dp),
        color = Color.Transparent,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isSelected) 6.dp else 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(horizontal = 12.dp, vertical = 11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OccasionVectorEmblem(
                    themeType = themeType,
                    customIconBase64 = if (themeType == OccasionThemeType.CUSTOM_MANUAL) customIconBase64 else "",
                    size = 46.dp
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = themeType.arabicLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isSelected) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color(0xFFFBBF24)
                            ) {
                                Text(
                                    text = "نشط",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF064E3B),
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                    Text(
                        text = themeType.badgeTitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = themeType.bannerGreeting,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFFFBBF24),
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Live Occasion Banner Card displayed on Dashboard, Login, and Onboarding screens
 * when an occasion theme is active.
 */
@Composable
fun LiveOccasionBannerCard(
    config: OccasionThemeConfig,
    modifier: Modifier = Modifier
) {
    if (config.themeType == OccasionThemeType.OFFICIAL &&
        config.customBannerText.isBlank() &&
        config.customIconBase64.isBlank()
    ) {
        return
    }

    val brush = occasionThemeBrush(config.effectivePrimaryHex, config.effectiveGradientHex)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_occasion_theme_banner"),
        shape = RoundedCornerShape(18.dp),
        color = Color.Transparent,
        border = BorderStroke(1.2.dp, Color(0xFFFBBF24).copy(alpha = 0.65f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(brush)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OccasionVectorEmblem(
                    themeType = config.themeType,
                    customIconBase64 = config.customIconBase64,
                    size = 42.dp
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = config.effectiveTitle,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFFDE68A),
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = config.effectiveBannerText,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
