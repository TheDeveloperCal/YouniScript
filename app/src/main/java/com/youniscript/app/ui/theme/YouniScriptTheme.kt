package com.youniscript.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LibraryColors = lightColorScheme(
    primary = Color(0xFF59664F),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE4E9DE),
    onPrimaryContainer = Color(0xFF293326),
    secondary = Color(0xFF9A704F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF2E5D7),
    onSecondaryContainer = Color(0xFF382719),
    tertiary = Color(0xFF6E7771),
    background = Color(0xFFF5F2EA),
    onBackground = Color(0xFF292820),
    surface = Color(0xFFFBF9F3),
    onSurface = Color(0xFF292820),
    surfaceVariant = Color(0xFFECE8DE),
    onSurfaceVariant = Color(0xFF706F64),
    outline = Color(0xFFD5D0C4),
    error = Color(0xFF9A443A),
    onError = Color(0xFFFFFFFF),
)

private val LibraryDarkColors = darkColorScheme(
    primary = Color(0xFFA6B596), onPrimary = Color(0xFF1D281D),
    primaryContainer = Color(0xFF344034), onPrimaryContainer = Color(0xFFE0E8D9),
    secondary = Color(0xFFC1A182), onSecondary = Color(0xFF2E241A),
    secondaryContainer = Color(0xFF403426), onSecondaryContainer = Color(0xFFE7D7C4),
    tertiary = Color(0xFFAEB9AF), background = Color(0xFF171A17),
    onBackground = Color(0xFFF0EEE5), surface = Color(0xFF202520),
    onSurface = Color(0xFFF0EEE5), surfaceVariant = Color(0xFF303730),
    onSurfaceVariant = Color(0xFFB7BEB2), outline = Color(0xFF596258),
    error = Color(0xFFE7A49A), onError = Color(0xFF3C1714),
)

val YouniTypography = Typography().let { defaults ->
    defaults.copy(
        displayLarge = defaults.displayLarge.copy(fontFamily = FontFamily.Serif),
        displayMedium = defaults.displayMedium.copy(fontFamily = FontFamily.Serif),
        displaySmall = defaults.displaySmall.copy(fontFamily = FontFamily.Serif),
        headlineLarge = defaults.headlineLarge.copy(fontFamily = FontFamily.Serif),
        headlineMedium = defaults.headlineMedium.copy(fontFamily = FontFamily.Serif),
        headlineSmall = defaults.headlineSmall.copy(fontFamily = FontFamily.Serif),
        titleLarge = defaults.titleLarge.copy(fontFamily = FontFamily.Serif),
    )
}

@Composable
fun YouniScriptTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalYouniPalette provides if (darkTheme) DarkYouniPalette else LightYouniPalette,
    ) {
    MaterialTheme(
        colorScheme = if (darkTheme) LibraryDarkColors else LibraryColors,
        typography = YouniTypography,
        content = content,
    )
    }
}
