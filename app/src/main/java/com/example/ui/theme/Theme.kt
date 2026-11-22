package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.model.TubeThemePreset

private fun buildDarkScheme(preset: TubeThemePreset): ColorScheme {
    val (primary, secondary, tertiary) = when (preset) {
        TubeThemePreset.CRIMSON -> Triple(CrimsonPrimaryDark, Color(0xFF00D2D3), Color(0xFFFF9F43))
        TubeThemePreset.CYBER -> Triple(CyberPrimaryDark, CyberSecondaryDark, Color(0xFFF472B6))
        TubeThemePreset.SAFFRON -> Triple(SaffronPrimaryDark, SaffronSecondaryDark, Color(0xFFFFD54F))
        TubeThemePreset.EMERALD -> Triple(EmeraldPrimaryDark, EmeraldSecondaryDark, Color(0xFFA3E635))
    }
    return darkColorScheme(
        primary = primary,
        onPrimary = Color(0xFF090D16),
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color(0xFF06131F),
        secondaryContainer = secondary.copy(alpha = 0.20f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color(0xFF140D05),
        background = NightBg,
        onBackground = NightOnSurface,
        surface = NightBg,
        onSurface = NightOnSurface,
        surfaceVariant = NightSurfaceHigh,
        onSurfaceVariant = NightOnSurfaceVar,
        surfaceContainerLow = NightSurfaceLow,
        surfaceContainer = NightSurface,
        surfaceContainerHigh = NightSurfaceHigh,
        outline = Color(0xFF334155)
    )
}

private fun buildLightScheme(preset: TubeThemePreset): ColorScheme {
    val (primary, secondary, tertiary) = when (preset) {
        TubeThemePreset.CRIMSON -> Triple(CrimsonPrimary, Color(0xFF0284C7), Color(0xFFEA580C))
        TubeThemePreset.CYBER -> Triple(CyberPrimary, CyberSecondary, Color(0xFFDB2777))
        TubeThemePreset.SAFFRON -> Triple(SaffronPrimary, SaffronSecondary, Color(0xFFD97706))
        TubeThemePreset.EMERALD -> Triple(EmeraldPrimary, EmeraldSecondary, Color(0xFF16A34A))
    }
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.14f),
        onPrimaryContainer = primary,
        secondary = secondary,
        onSecondary = Color.White,
        secondaryContainer = secondary.copy(alpha = 0.14f),
        onSecondaryContainer = secondary,
        tertiary = tertiary,
        onTertiary = Color.White,
        background = DayBg,
        onBackground = DayOnSurface,
        surface = DayBg,
        onSurface = DayOnSurface,
        surfaceVariant = DaySurfaceHigh,
        onSurfaceVariant = DayOnSurfaceVar,
        surfaceContainerLow = DaySurfaceLow,
        surfaceContainer = DaySurface,
        surfaceContainerHigh = DaySurfaceHigh,
        outline = Color(0xFFCBD5E1)
    )
}

val TubeStoreShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun TubeStoreTheme(
    preset: TubeThemePreset = TubeThemePreset.CRIMSON,
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) buildDarkScheme(preset) else buildLightScheme(preset)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TubeStoreTypography,
        shapes = TubeStoreShapes,
        content = content
    )
}
