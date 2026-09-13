package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = MutedBlueLight,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF1E3A5F),
    onPrimaryContainer = Color(0xFFBAE6FD),

    secondary = MutedBlueSecondary,
    onSecondary = Color(0xFF0F172A),
    secondaryContainer = Color(0xFF162D4A),
    onSecondaryContainer = Color(0xFFCBD5E1),

    tertiary = AmberLight,
    onTertiary = Color(0xFF451A03),
    tertiaryContainer = Color(0xFF78350F),
    onTertiaryContainer = Color(0xFFFDE68A),

    error = RedCritical,
    onError = Color.White,
    errorContainer = RedCriticalContainer,
    onErrorContainer = Color(0xFFFCA5A5),

    background = BackgroundDark,
    onBackground = Color(0xFFF1F5F9),
    surface = SurfaceDark,
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = SurfaceElevatedDark,
    onSurfaceVariant = Color(0xFFCBD5E1),
    outline = SurfaceBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = MutedBlueDark,
    onPrimary = Color.White,
    primaryContainer = MutedBlueContainer,
    onPrimaryContainer = MutedBlueDark,

    secondary = MutedBlueSecondary,
    onSecondary = Color.White,
    secondaryContainer = MutedBlueContainer,
    onSecondaryContainer = MutedBlueDark,

    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AmberTertiary,

    error = RestrainedRed,
    onError = Color.White,
    errorContainer = RestrainedRedContainer,
    onErrorContainer = RestrainedRed,

    background = PaperBackground,
    onBackground = TextPrimary,
    surface = PaperCard,
    onSurface = TextPrimary,
    surfaceVariant = PaperCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = PaperBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Clean, low eye-fatigue light theme by default
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
