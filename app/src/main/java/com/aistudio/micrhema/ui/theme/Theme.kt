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
    primary = Color(0xFFD4AF37), // Pure gold
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFEF08A), // Light yellow
    onPrimaryContainer = Color(0xFF713F12),
    
    secondary = Color(0xFFEAB308), // Yellow
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFDE68A), // Light amber
    onSecondaryContainer = Color(0xFF78350F),
    
    tertiary = Color(0xFFFBBF24), // Amber
    onTertiary = Color.White,
    
    background = Color(0xFFFFFDF5), // Off-white with yellow tint
    onBackground = Color(0xFF1C1917),
    
    surface = Color(0xFFFFFFFF), // Pure white
    onSurface = Color(0xFF1C1917),
    
    surfaceVariant = Color(0xFFFFFBEB), // Very light yellow surface variant
    onSurfaceVariant = Color(0xFF78350F), // Dark yellow/brown text
    
    outline = Color(0xFFFDE68A)
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
    dynamicColor: Boolean = false, // Disabled dynamic color to force our harmonized theme
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Force Light Scheme as requested

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
                val statusBarColor = Color(0xFFFFFDF5)
                window.statusBarColor = statusBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ExpressiveTypography,
        content = content
    )
}
