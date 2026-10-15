package com.goreecloud.markdown

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Product-specific native presentation mapping, under Glaze V1.7 Stable
 * consumer-adoption review. Colors communicate intent; labels carry meaning.
 * These palettes do not imply formal Glaze consumer acceptance.
 */
internal data class MarkdownWorkspaceColors(
    val canvas: Color,
    val panel: Color,
    val editor: Color,
    val copper: Color,
    val copperSoft: Color,
    val copperInk: Color,
    val forest: Color,
    val forestSoft: Color,
    val forestInk: Color,
    val ochre: Color,
    val ochreSoft: Color,
    val ochreInk: Color,
)

internal fun markdownWorkspaceColors(dark: Boolean): MarkdownWorkspaceColors =
    if (dark) MarkdownWorkspaceColors(
        canvas = Color(0xFF161C1A),
        panel = Color(0xFF222B27),
        editor = Color(0xFF1D2522),
        copper = Color(0xFFEAB69F),
        copperSoft = Color(0xFF503329),
        copperInk = Color(0xFFFFD8C9),
        forest = Color(0xFFA1D8B3),
        forestSoft = Color(0xFF223D30),
        forestInk = Color(0xFFC4F0D0),
        ochre = Color(0xFFF5D18A),
        ochreSoft = Color(0xFF493B23),
        ochreInk = Color(0xFFFDE2AC),
    ) else MarkdownWorkspaceColors(
        canvas = Color(0xFFF7F5F0),
        panel = Color(0xFFFFFEFB),
        editor = Color(0xFFFFFEFB),
        copper = Color(0xFF8F4430),
        copperSoft = Color(0xFFFFE6DA),
        copperInk = Color(0xFF713321),
        forest = Color(0xFF286B4B),
        forestSoft = Color(0xFFDDF1E1),
        forestInk = Color(0xFF24573E),
        ochre = Color(0xFF8E6416),
        ochreSoft = Color(0xFFFFF0C9),
        ochreInk = Color(0xFF70501C),
    )

@Composable
internal fun MarkdownWorkspaceTheme(
    content: @Composable (MarkdownWorkspaceColors) -> Unit,
) {
    val dark = isSystemInDarkTheme()
    val tones = markdownWorkspaceColors(dark)
    val scheme = if (dark) darkColorScheme(
        primary = tones.copper,
        onPrimary = Color(0xFF40241C),
        primaryContainer = tones.copperSoft,
        onPrimaryContainer = tones.copperInk,
        secondary = tones.forest,
        onSecondary = Color(0xFF183927),
        secondaryContainer = tones.forestSoft,
        onSecondaryContainer = tones.forestInk,
        tertiary = tones.ochre,
        onTertiary = Color(0xFF3B2B10),
        tertiaryContainer = tones.ochreSoft,
        onTertiaryContainer = tones.ochreInk,
        background = tones.canvas,
        onBackground = Color(0xFFF0F2ED),
        surface = tones.panel,
        onSurface = Color(0xFFF0F2ED),
        surfaceVariant = Color(0xFF34413A),
        onSurfaceVariant = Color(0xFFCDD6CC),
        outline = Color(0xFF809387),
        error = Color(0xFFFFB4AB),
    ) else lightColorScheme(
        primary = tones.copper,
        onPrimary = Color.White,
        primaryContainer = tones.copperSoft,
        onPrimaryContainer = tones.copperInk,
        secondary = tones.forest,
        onSecondary = Color.White,
        secondaryContainer = tones.forestSoft,
        onSecondaryContainer = tones.forestInk,
        tertiary = tones.ochre,
        onTertiary = Color.White,
        tertiaryContainer = tones.ochreSoft,
        onTertiaryContainer = tones.ochreInk,
        background = tones.canvas,
        onBackground = Color(0xFF262E29),
        surface = tones.panel,
        onSurface = Color(0xFF262E29),
        surfaceVariant = Color(0xFFE8EDE6),
        onSurfaceVariant = Color(0xFF58675D),
        outline = Color(0xFF849488),
        error = Color(0xFFB3261E),
    )
    MaterialTheme(
        colorScheme = scheme,
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(22.dp),
            extraLarge = RoundedCornerShape(28.dp),
        ),
    ) { content(tones) }
}
