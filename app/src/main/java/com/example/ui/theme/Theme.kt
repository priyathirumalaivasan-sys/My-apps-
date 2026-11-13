package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val VasanDarkColorScheme = darkColorScheme(
    primary = StudioYellow,
    onPrimary = Color(0xFF141100),
    primaryContainer = Color(0xFF3A3000),
    onPrimaryContainer = StudioYellowSoft,
    secondary = StudioPurple,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF2B1845),
    onSecondaryContainer = StudioPurpleGlow,
    tertiary = StudioAmber,
    onTertiary = Color(0xFF180F00),
    background = StudioBlack,
    onBackground = StudioTextPrimary,
    surface = StudioSurface,
    onSurface = StudioTextPrimary,
    surfaceVariant = StudioSurfaceElevated,
    onSurfaceVariant = StudioTextSecondary,
    outline = StudioBorderSubtle,
    error = StudioError,
    onError = Color.White
)

private val VasanShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun VasanAiStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VasanDarkColorScheme,
        typography = VasanTypography,
        shapes = VasanShapes,
        content = content
    )
}
