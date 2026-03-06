package com.omniagent.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// === TYPOGRAPHY ===
val OmniTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        letterSpacing = 0.15.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
    )
)

// === DARK COLOR SCHEME ===
private val OmniDarkColorScheme = darkColorScheme(
    primary = OmniColors.Primary,
    onPrimary = OmniColors.Background,
    primaryContainer = OmniColors.PrimaryDim,
    secondary = OmniColors.Secondary,
    onSecondary = OmniColors.Background,
    secondaryContainer = OmniColors.SecondaryDim,
    tertiary = OmniColors.Accent,
    background = OmniColors.Background,
    onBackground = OmniColors.TextPrimary,
    surface = OmniColors.Surface,
    onSurface = OmniColors.TextPrimary,
    surfaceVariant = OmniColors.SurfaceElevated,
    onSurfaceVariant = OmniColors.TextSecondary,
    outline = OmniColors.Border,
    error = OmniColors.Danger
)

@Composable
fun OmniAgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OmniDarkColorScheme,
        typography = OmniTypography,
        content = content
    )
}
