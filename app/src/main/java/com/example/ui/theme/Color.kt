package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Global Reactive Gender-Adaptive Theme Engine State.
 * - `isFeminine = false`: Official Royal Emerald & Gold Identity (طالب / معلم / مدير / مدرسة بنين)
 * - `isFeminine = true`: Official Royal Feminine Identity — Rose Quartz, Soft Lavender & Rose Gold
 *   (طالبة / معلمة أو أستاذة / ولي أمر يتصفح حساب طالبة / مدرسة بنات)
 */
object GenderThemeState {
    var isFeminine by mutableStateOf(false)
}

data class GenderAdaptivePalette(
    val isFeminine: Boolean = false,
    val primary: Color = Color(0xFF059669),
    val primaryDeep: Color = Color(0xFF047857),
    val primaryDarkest: Color = Color(0xFF064E3B),
    val accentBright: Color = Color(0xFF10B981),
    val secondaryLavenderOrNavy: Color = Color(0xFF0F3D5E),
    val softTintBg: Color = Color(0xFFECFDF5),
    val softContainerBg: Color = Color(0xFFD1FAE5),
    val softBorderAccent: Color = Color(0xFFA7F3D0),
    val waveRibbon1: Color = Color(0xFFCCFBF1),
    val waveRibbon2: Color = Color(0xFFA7F3D0)
)

val LocalGenderAdaptivePalette = staticCompositionLocalOf { GenderAdaptivePalette() }

// Light Mode Palette - Unified Royal Emerald & Gold Identity
val PrimaryLight: Color
    get() = Color(0xFF059669)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight: Color
    get() = Color(0xFFD1FAE5)
val OnPrimaryContainerLight: Color
    get() = Color(0xFF064E3B)

val SecondaryLight: Color
    get() = Color(0xFF1E3A8A)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight: Color
    get() = Color(0xFFDBEAFE)
val OnSecondaryContainerLight: Color
    get() = Color(0xFF1E3A8A)

val TertiaryLight: Color
    get() = Color(0xFFD97706)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight: Color
    get() = Color(0xFFFEF3C7)
val OnTertiaryContainerLight: Color
    get() = Color(0xFF78350F)

val BackgroundLight: Color
    get() = Color(0xFFF8FAFC)
val OnBackgroundLight = Color(0xFF0F172A)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF0F172A)
val SurfaceVariantLight: Color
    get() = Color(0xFFF1F5F9)
val OnSurfaceVariantLight = Color(0xFF475569)
val OutlineLight: Color
    get() = Color(0xFFCBD5E1)

// Dark Mode Palette - Luxury Deep Slate & Vibrant Emerald
val PrimaryDark: Color
    get() = Color(0xFF10B981)
val OnPrimaryDark: Color
    get() = Color(0xFF022C22)
val PrimaryContainerDark: Color
    get() = Color(0xFF064E3B)
val OnPrimaryContainerDark: Color
    get() = Color(0xFFA7F3D0)

val SecondaryDark: Color
    get() = Color(0xFF60A5FA)
val OnSecondaryDark = Color(0xFF0F172A)
val SecondaryContainerDark = Color(0xFF1E293B)
val OnSecondaryContainerDark: Color
    get() = Color(0xFFDBEAFE)

val TertiaryDark = Color(0xFFFBBF24)       // Gold 400
val OnTertiaryDark = Color(0xFF451A03)
val TertiaryContainerDark = Color(0xFF78350F)
val OnTertiaryContainerDark = Color(0xFFFDE68A)

val BackgroundDark = Color(0xFF0B1120)
val OnBackgroundDark = Color(0xFFF8FAFC)
val SurfaceDark = Color(0xFF111827)
val OnSurfaceDark = Color(0xFFF8FAFC)
val SurfaceVariantDark = Color(0xFF1E293B)
val OnSurfaceVariantDark = Color(0xFF94A3B8)
val OutlineDark = Color(0xFF334155)

// Semantic Accents
val GoldAccent = Color(0xFFF59E0B)
val GoldDeep = Color(0xFFB45309)
val GoldLightBg = Color(0xFFFFFBEB)
val WhatsAppGreen = Color(0xFF25D366)
val DangerRed = Color(0xFFEF4444)
val WarningAmber = Color(0xFFF59E0B)
val InfoBlue = Color(0xFF3B82F6)
val SuccessGreen = Color(0xFF10B981)

// Luxury Identity Aliases (Unified Royal Emerald across the entire system)
val DeepForestGreen: Color
    get() = Color(0xFF064E3B)
val DeepEmerald: Color
    get() = Color(0xFF047857)
val RoyalGreen: Color
    get() = Color(0xFF059669)
val EmeraldAccent: Color
    get() = Color(0xFF10B981)

// Additional Adaptive Surface & Hero Gradient Tokens (Unified Royal Emerald)
val AdaptiveDarkHeader: Color
    get() = Color(0xFF042F24)
val AdaptiveHeroStart: Color
    get() = Color(0xFF059669)
val AdaptiveHeroMid: Color
    get() = Color(0xFF047857)
val AdaptiveHeroEnd: Color
    get() = Color(0xFF0F3D5E)
val AdaptiveSoftSurface: Color
    get() = Color(0xFFECFDF5)
val AdaptiveSoftContainer: Color
    get() = Color(0xFFD1FAE5)
val AdaptiveSoftBorder: Color
    get() = Color(0xFFA7F3D0)
val AdaptiveWaveRibbon1: Color
    get() = Color(0xFFCCFBF1)
val AdaptiveWaveRibbon2: Color
    get() = Color(0xFFA7F3D0)
val AdaptiveHighlightGlow: Color
    get() = Color(0xFF34D399)

val GoldFoil = Color(0xFFD97706)
val GoldLight = Color(0xFFFDE68A)
val WarmGoldLight = Color(0xFFFEF3C7)
val CardSurfaceWhite = Color(0xFFFFFFFF)
val TextMutedGreen = Color(0xFF334155)
