package com.gorib.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PremiumLightColorScheme = lightColorScheme(
    primary = Color(0xFF4F46E5),              // Premium Vibrant Indigo
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEEF2FF),     // Glassy slate-indigo container
    onPrimaryContainer = Color(0xFF3730A3),
    secondary = Color(0xFF0891B2),            // Teal secondary
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFECFEFF),
    onSecondaryContainer = Color(0xFF155E75),
    background = Color(0xFFF8FAFC),           // Clean soft off-white/slate
    onBackground = Color(0xFF0F172A),         // Deep Slate body text
    surface = Color(0xFFFFFFFF),              // Pure White surfaces
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),       // Elevated Card Backgrounds
    onSurfaceVariant = Color(0xFF64748B),     // Muted secondary text
    outline = Color(0xFFE2E8F0),              // Clean thin outline dividers
    error = Color(0xFFE11D48),                // Vibrant Rose
    errorContainer = Color(0xFFFFE4E6)
)

private val PremiumDarkColorScheme = darkColorScheme(
    primary = Primary,
    onPrimary = OnPrimary,
    primaryContainer = PrimaryContainer,
    onPrimaryContainer = OnPrimaryContainer,
    secondary = Secondary,
    onSecondary = OnSecondary,
    secondaryContainer = SecondaryContainer,
    onSecondaryContainer = OnSecondaryContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    surfaceVariant = SurfaceVariant,
    onSurfaceVariant = OnSurfaceVariant,
    outline = Outline,
    error = Error,
    errorContainer = ErrorContainer
)

private val LightColorScheme = PremiumLightColorScheme
private val DarkColorScheme = PremiumDarkColorScheme

/**
 * Main visual theme wrapper for the GORIB application.
 * Configured with premium teal/amber light mode as the default layout.
 */
@Composable
fun GoribTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            
            // Adjust light/dark status bar text appearance according to selected theme
            val windowInsetsController = WindowCompat.getInsetsController(window, view)
            windowInsetsController.isAppearanceLightStatusBars = !darkTheme
            windowInsetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
