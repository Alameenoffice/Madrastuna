package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.LayoutDirection

/**
 * Global composition local indicating whether the Gender-Adaptive Theme Engine
 * is currently displaying the Royal Feminine Palette ("ثيم الجنس اللطيف").
 */
val LocalIsFeminineTheme = compositionLocalOf { false }

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
)

private val FeminineDarkColorScheme = darkColorScheme(
    primary = FemininePrimaryDark,
    onPrimary = FeminineOnPrimaryDark,
    primaryContainer = FemininePrimaryContainerDark,
    onPrimaryContainer = FeminineOnPrimaryContainerDark,
    secondary = FeminineSecondaryDark,
    onSecondary = FeminineOnSecondaryDark,
    secondaryContainer = FeminineSecondaryContainerDark,
    onSecondaryContainer = FeminineOnSecondaryContainerDark,
    tertiary = FeminineTertiaryDark,
    onTertiary = FeminineOnTertiaryDark,
    tertiaryContainer = FeminineTertiaryContainerDark,
    onTertiaryContainer = FeminineOnTertiaryContainerDark,
    background = FeminineBackgroundDark,
    onBackground = FeminineOnBackgroundDark,
    surface = FeminineSurfaceDark,
    onSurface = FeminineOnSurfaceDark,
    surfaceVariant = FeminineSurfaceVariantDark,
    onSurfaceVariant = FeminineOnSurfaceVariantDark,
    outline = FeminineOutlineDark
)

private val FeminineLightColorScheme = lightColorScheme(
    primary = FemininePrimaryLight,
    onPrimary = FeminineOnPrimaryLight,
    primaryContainer = FemininePrimaryContainerLight,
    onPrimaryContainer = FeminineOnPrimaryContainerLight,
    secondary = FeminineSecondaryLight,
    onSecondary = FeminineOnSecondaryLight,
    secondaryContainer = FeminineSecondaryContainerLight,
    onSecondaryContainer = FeminineOnSecondaryContainerLight,
    tertiary = FeminineTertiaryLight,
    onTertiary = FeminineOnTertiaryLight,
    tertiaryContainer = FeminineTertiaryContainerLight,
    onTertiaryContainer = FeminineOnTertiaryContainerLight,
    background = FeminineBackgroundLight,
    onBackground = FeminineOnBackgroundLight,
    surface = FeminineSurfaceLight,
    onSurface = FeminineOnSurfaceLight,
    surfaceVariant = FeminineSurfaceVariantLight,
    onSurfaceVariant = FeminineOnSurfaceVariantLight,
    outline = FeminineOutlineLight
)

@Composable
private fun animateAdaptiveColorScheme(target: ColorScheme): ColorScheme {
    val spec = tween<Color>(durationMillis = 420)
    val primary by animateColorAsState(target.primary, spec, label = "primary")
    val onPrimary by animateColorAsState(target.onPrimary, spec, label = "onPrimary")
    val primaryContainer by animateColorAsState(target.primaryContainer, spec, label = "primaryContainer")
    val onPrimaryContainer by animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer")
    val secondary by animateColorAsState(target.secondary, spec, label = "secondary")
    val onSecondary by animateColorAsState(target.onSecondary, spec, label = "onSecondary")
    val secondaryContainer by animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer")
    val onSecondaryContainer by animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer")
    val tertiary by animateColorAsState(target.tertiary, spec, label = "tertiary")
    val onTertiary by animateColorAsState(target.onTertiary, spec, label = "onTertiary")
    val tertiaryContainer by animateColorAsState(target.tertiaryContainer, spec, label = "tertiaryContainer")
    val onTertiaryContainer by animateColorAsState(target.onTertiaryContainer, spec, label = "onTertiaryContainer")
    val background by animateColorAsState(target.background, spec, label = "background")
    val onBackground by animateColorAsState(target.onBackground, spec, label = "onBackground")
    val surface by animateColorAsState(target.surface, spec, label = "surface")
    val onSurface by animateColorAsState(target.onSurface, spec, label = "onSurface")
    val surfaceVariant by animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant")
    val onSurfaceVariant by animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant")
    val outline by animateColorAsState(target.outline, spec, label = "outline")

    return target.copy(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = secondary,
        onSecondary = onSecondary,
        secondaryContainer = secondaryContainer,
        onSecondaryContainer = onSecondaryContainer,
        tertiary = tertiary,
        onTertiary = onTertiary,
        tertiaryContainer = tertiaryContainer,
        onTertiaryContainer = onTertiaryContainer,
        background = background,
        onBackground = onBackground,
        surface = surface,
        onSurface = onSurface,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = onSurfaceVariant,
        outline = outline
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFeminineTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var activeFontFamily by remember {
        mutableStateOf<FontFamily>(ArabicFontLoader.getInitialFontFamily(context))
    }

    LaunchedEffect(Unit) {
        val loadedFontFamily = ArabicFontLoader.loadArabicFontFamily(context)
        activeFontFamily = loadedFontFamily
    }

    val dynamicTypography = remember(activeFontFamily) {
        buildAppTypography(activeFontFamily)
    }

    val targetColorScheme = when {
        darkTheme && isFeminineTheme -> FeminineDarkColorScheme
        darkTheme && !isFeminineTheme -> DarkColorScheme
        !darkTheme && isFeminineTheme -> FeminineLightColorScheme
        else -> LightColorScheme
    }
    val animatedColorScheme = animateAdaptiveColorScheme(targetColorScheme)

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        LocalIsFeminineTheme provides isFeminineTheme,
        com.example.util.LocalAppLanguage provides com.example.util.AppLanguage.ARABIC
    ) {
        MaterialTheme(
            colorScheme = animatedColorScheme,
            typography = dynamicTypography
        ) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(fontFamily = activeFontFamily))
            ) {
                ProvideTextStyle(
                    value = TextStyle(fontFamily = activeFontFamily),
                    content = content
                )
            }
        }
    }
}
