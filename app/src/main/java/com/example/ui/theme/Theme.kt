package com.example.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isFeminineTheme: Boolean = GenderThemeState.isFeminine,
    content: @Composable () -> Unit
) {
    // Synchronize global reactive state immediately so every screen and component reads the active gender palette
    GenderThemeState.isFeminine = isFeminineTheme
    SideEffect {
        GenderThemeState.isFeminine = isFeminineTheme
    }

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

    val animSpec = tween<Color>(durationMillis = 420)
    val animatedPrimary by animateColorAsState(
        targetValue = if (darkTheme) PrimaryDark else PrimaryLight,
        animationSpec = animSpec,
        label = "theme_primary"
    )
    val animatedPrimaryContainer by animateColorAsState(
        targetValue = if (darkTheme) PrimaryContainerDark else PrimaryContainerLight,
        animationSpec = animSpec,
        label = "theme_primary_container"
    )
    val animatedOnPrimaryContainer by animateColorAsState(
        targetValue = if (darkTheme) OnPrimaryContainerDark else OnPrimaryContainerLight,
        animationSpec = animSpec,
        label = "theme_on_primary_container"
    )
    val animatedSecondary by animateColorAsState(
        targetValue = if (darkTheme) SecondaryDark else SecondaryLight,
        animationSpec = animSpec,
        label = "theme_secondary"
    )
    val animatedSecondaryContainer by animateColorAsState(
        targetValue = if (darkTheme) SecondaryContainerDark else SecondaryContainerLight,
        animationSpec = animSpec,
        label = "theme_secondary_container"
    )
    val animatedBackground by animateColorAsState(
        targetValue = if (darkTheme) BackgroundDark else BackgroundLight,
        animationSpec = animSpec,
        label = "theme_background"
    )
    val animatedSurfaceVariant by animateColorAsState(
        targetValue = if (darkTheme) SurfaceVariantDark else SurfaceVariantLight,
        animationSpec = animSpec,
        label = "theme_surface_variant"
    )

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = animatedPrimary,
            onPrimary = OnPrimaryDark,
            primaryContainer = animatedPrimaryContainer,
            onPrimaryContainer = animatedOnPrimaryContainer,
            secondary = animatedSecondary,
            onSecondary = OnSecondaryDark,
            secondaryContainer = animatedSecondaryContainer,
            onSecondaryContainer = OnSecondaryContainerDark,
            tertiary = TertiaryDark,
            onTertiary = OnTertiaryDark,
            tertiaryContainer = TertiaryContainerDark,
            onTertiaryContainer = OnTertiaryContainerDark,
            background = animatedBackground,
            onBackground = OnBackgroundDark,
            surface = SurfaceDark,
            onSurface = OnSurfaceDark,
            surfaceVariant = animatedSurfaceVariant,
            onSurfaceVariant = OnSurfaceVariantDark,
            outline = OutlineDark
        )
    } else {
        lightColorScheme(
            primary = animatedPrimary,
            onPrimary = OnPrimaryLight,
            primaryContainer = animatedPrimaryContainer,
            onPrimaryContainer = animatedOnPrimaryContainer,
            secondary = animatedSecondary,
            onSecondary = OnSecondaryLight,
            secondaryContainer = animatedSecondaryContainer,
            onSecondaryContainer = OnSecondaryContainerLight,
            tertiary = TertiaryLight,
            onTertiary = OnTertiaryLight,
            tertiaryContainer = TertiaryContainerLight,
            onTertiaryContainer = OnTertiaryContainerLight,
            background = animatedBackground,
            onBackground = OnBackgroundLight,
            surface = SurfaceLight,
            onSurface = OnSurfaceLight,
            surfaceVariant = animatedSurfaceVariant,
            onSurfaceVariant = OnSurfaceVariantLight,
            outline = OutlineLight
        )
    }

    val adaptivePalette = remember(isFeminineTheme) {
        if (isFeminineTheme) {
            GenderAdaptivePalette(
                isFeminine = true,
                primary = Color(0xFFE11D48),
                primaryDeep = Color(0xFFBE123C),
                primaryDarkest = Color(0xFF881337),
                accentBright = Color(0xFFF43F5E),
                secondaryLavenderOrNavy = Color(0xFF6B21A8),
                softTintBg = Color(0xFFFFF1F2),
                softContainerBg = Color(0xFFFFE4E6),
                softBorderAccent = Color(0xFFFECDD3),
                waveRibbon1 = Color(0xFFFFE4E6),
                waveRibbon2 = Color(0xFFFECDD3)
            )
        } else {
            GenderAdaptivePalette()
        }
    }

    CompositionLocalProvider(
        LocalLayoutDirection provides LayoutDirection.Rtl,
        com.example.util.LocalAppLanguage provides com.example.util.AppLanguage.ARABIC,
        LocalGenderAdaptivePalette provides adaptivePalette
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
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
