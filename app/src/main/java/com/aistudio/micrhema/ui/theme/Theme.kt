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
import com.aistudio.micrhema.AccentColor
import com.aistudio.micrhema.currentSettingsState
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8A6500),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE8A3),
    onPrimaryContainer = Color(0xFF3C2F00),

    secondary = Color(0xFF765600),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF0C2),
    onSecondaryContainer = Color(0xFF3C2F00),

    tertiary = Color(0xFF8A6500),
    onTertiary = Color.White,

    background = Color(0xFFFFFDF7),
    onBackground = Color(0xFF1C1917),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1917),

    surfaceVariant = Color(0xFFFFF8E7),
    onSurfaceVariant = Color(0xFF514000),

    outline = Color(0xFF6B6252),
    outlineVariant = Color(0xFFD4C8AC)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF7DB3FF),
    onPrimary = Color(0xFF06234A),
    secondary = Color(0xFFE5B842),
    onSecondary = Color(0xFF2D2100),
    tertiary = Color(0xFFFFD66B),
    onTertiary = Color(0xFF2D2100),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = currentSettingsState.value.accentColor
    val baseColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val primaryColor = when (accent) {
        AccentColor.BLUE -> Color(0xFF3B82F6)
        AccentColor.GREEN -> Color(0xFF10B981)
        AccentColor.PURPLE -> Color(0xFF8B5CF6)
        AccentColor.GOLD -> Color(0xFF8A6500)
        AccentColor.WHITE -> if (darkTheme) Color.White else Color.Black
    }
    
    val colorScheme = baseColorScheme.copy(
        primary = primaryColor,
        secondary = primaryColor,
        onPrimary = if (accent == AccentColor.WHITE && darkTheme) Color.Black else Color.White
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) {
                    break
                }
                ctx = ctx.baseContext
            }
            if (ctx is Activity) {
                val window = ctx.window
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT
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
