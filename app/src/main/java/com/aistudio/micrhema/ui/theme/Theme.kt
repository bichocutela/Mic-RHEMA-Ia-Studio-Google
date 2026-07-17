package com.aistudio.micrhema.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF3B82F6), // Elegant premium blue
    secondary = Color(0xFFD4AF37), // Pure gold accent
    tertiary = Color(0xFFE5B842), // Bright gold accent
    background = Color(0xFFFFFFFF), // White background
    surface = Color(0xFFFFFFFF), // Pure white surface
    primaryContainer = Color(0xFFE2E8F0), // Light slate container
    onPrimaryContainer = Color(0xFF0F172A),
    secondaryContainer = Color(0xFFFEF08A),
    onSecondaryContainer = Color(0xFF854D0E),
    surfaceVariant = Color(0xFFFFFFFF), // Pure white surface variant
    onSurfaceVariant = Color(0xFF475569), // Dark slate text
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF3B82F6), // Elegant premium blue
    secondary = Color(0xFFD4AF37), // Pure gold accent
    tertiary = Color(0xFFE5B842), // Bright gold accent
    background = Color(0xFF090E17), // Premium midnight deep navy
    surface = Color(0xFF131B2E), // Premium dark ocean surface
    primaryContainer = Color(0xFF1E293B), // Elegant slate container
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFD4AF37),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8), // Clear secondary slate text
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun MICRhemaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var context = view.context
            while (context is android.content.ContextWrapper) {
                if (context is Activity) {
                    break
                }
                context = context.baseContext
            }
            if (context is Activity) {
                val window = context.window
                val statusBarColor = if (darkTheme) Color(0xFF090E17) else Color(0xFFFFFFFF)
                window.statusBarColor = statusBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        content = content
    )
}
