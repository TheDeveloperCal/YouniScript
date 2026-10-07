package com.youniscript.app.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

/** Shared shell colors. Page-specific colors are defined by PageStyle. */
data class YouniPalette(
    val library: Color, val paper: Color, val elevatedPaper: Color,
    val ink: Color, val secondaryInk: Color, val mutedInk: Color,
    val sage: Color, val sageLight: Color, val naturalBrown: Color,
    val border: Color, val divider: Color, val error: Color,
    val success: Color, val selection: Color,
)

val LightYouniPalette = YouniPalette(
    Color(0xFFF5F2EA), Color(0xFFFBF9F3), Color(0xFFFFFFFF),
    Color(0xFF292820), Color(0xFF56564C), Color(0xFF706F64),
    Color(0xFF59664F), Color(0xFFE4E9DE), Color(0xFF9A704F),
    Color(0xFFE5E0D6), Color(0xFFD5D0C4), Color(0xFF9A443A),
    Color(0xFF47634A), Color(0xFFDEE7D6),
)

val DarkYouniPalette = YouniPalette(
    Color(0xFF171A17), Color(0xFF202520), Color(0xFF292F29),
    Color(0xFFF0EEE5), Color(0xFFD1D3C9), Color(0xFFA8ADA2),
    Color(0xFFA6B596), Color(0xFF344034), Color(0xFFC1A182),
    Color(0xFF3A413A), Color(0xFF444B43), Color(0xFFE7A49A),
    Color(0xFF9DBA9B), Color(0xFF354235),
)

val LocalYouniPalette = staticCompositionLocalOf { LightYouniPalette }

/** Relative interface text scale; page and book body typography can opt out. */
val LocalYouniInterfaceScale = staticCompositionLocalOf { 1f }

/** Keeps authored page and book text at its chosen page style size. */
@Composable
fun YouniContentTypography(content: @Composable () -> Unit) {
    val interfaceScale = LocalYouniInterfaceScale.current
    val density = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(density.density, density.fontScale / interfaceScale),
        content = content,
    )
}

object YouniColors {
    val library: Color @Composable get() = LocalYouniPalette.current.library
    val paper: Color @Composable get() = LocalYouniPalette.current.paper
    val elevatedPaper: Color @Composable get() = LocalYouniPalette.current.elevatedPaper
    val ink: Color @Composable get() = LocalYouniPalette.current.ink
    val secondaryInk: Color @Composable get() = LocalYouniPalette.current.secondaryInk
    val mutedInk: Color @Composable get() = LocalYouniPalette.current.mutedInk
    val sage: Color @Composable get() = LocalYouniPalette.current.sage
    val sageLight: Color @Composable get() = LocalYouniPalette.current.sageLight
    val naturalBrown: Color @Composable get() = LocalYouniPalette.current.naturalBrown
    val border: Color @Composable get() = LocalYouniPalette.current.border
    val divider: Color @Composable get() = LocalYouniPalette.current.divider
    val error: Color @Composable get() = LocalYouniPalette.current.error
    val success: Color @Composable get() = LocalYouniPalette.current.success
    val selection: Color @Composable get() = LocalYouniPalette.current.selection
}

object YouniSpacing {
    val xSmall = 4.dp
    val small = 8.dp
    val medium = 16.dp
    val large = 24.dp
    val xLarge = 32.dp
}
