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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.aistudio.micrhema.AccentColor
import com.aistudio.micrhema.XpRewardManager
import com.aistudio.micrhema.currentSettingsState
import com.aistudio.micrhema.loggedInMemberState
import com.aistudio.micrhema.xpEntitlementsState

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
    background = Color(0xFF090E17),
    surface = Color(0xFF131B2E),
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFFD4AF37),
    surfaceVariant = Color(0xFF1E293B),
    onSurfaceVariant = Color(0xFF94A3B8),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF)
)

/**
 * Tema premium da Loja XP. O dourado comum continua disponível normalmente;
 * este esquema só entra quando o membro possui o entitlement e seleciona Dourado.
 */
private val GoldPlusLightColorScheme = lightColorScheme(
    primary = Color(0xFFB47B00),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFEDB0),
    onPrimaryContainer = Color(0xFF332200),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF3A2A00),
    secondaryContainer = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFF3A2A00),
    tertiary = Color(0xFFE3B83F),
    onTertiary = Color(0xFF2A1B00),
    tertiaryContainer = Color(0xFFFFF7DC),
    onTertiaryContainer = Color(0xFF332200),
    background = Color(0xFFFFFCF3),
    onBackground = Color(0xFF211A0D),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF211A0D),
    surfaceVariant = Color(0xFFFFF6DA),
    onSurfaceVariant = Color(0xFF5B4816),
    outline = Color(0xFF9C7A20),
    outlineVariant = Color(0xFFE8D79D)
)

private val GoldPlusDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFD86A),
    onPrimary = Color(0xFF3A2800),
    primaryContainer = Color(0xFF5A4000),
    onPrimaryContainer = Color(0xFFFFF4CF),
    secondary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFF17130A),
    secondaryContainer = Color(0xFF2A2417),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = Color(0xFFFFF0A8),
    onTertiary = Color(0xFF302300),
    tertiaryContainer = Color(0xFF443500),
    onTertiaryContainer = Color(0xFFFFFFFF),
    background = Color(0xFF0D0A04),
    onBackground = Color(0xFFFFFBEE),
    surface = Color(0xFF171207),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF2B2312),
    onSurfaceVariant = Color(0xFFFFE9A8),
    outline = Color(0xFFD8B84E),
    outlineVariant = Color(0xFF6F5A21)
)

@Composable
fun MICRhemaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val accent = currentSettingsState.value.accentColor
    val memberId = loggedInMemberState.value?.id
    val entitlementOwned = memberId != null && (
        xpEntitlementsState.value
            ?.takeIf { it.memberId == memberId }
            ?.entitlements
            ?.any { it.itemId == XpRewardManager.GOLD_THEME } == true ||
            XpRewardManager.isOwned(context, XpRewardManager.GOLD_THEME, memberId)
        )
    val goldPlusActive = accent == AccentColor.GOLD && entitlementOwned

    val baseColorScheme = when {
        goldPlusActive && darkTheme -> GoldPlusDarkColorScheme
        goldPlusActive -> GoldPlusLightColorScheme
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

    val colorScheme = if (goldPlusActive) {
        baseColorScheme
    } else {
        baseColorScheme.copy(
            primary = primaryColor,
            secondary = primaryColor,
            onPrimary = if (accent == AccentColor.WHITE && darkTheme) Color.Black else Color.White
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            var ctx = view.context
            while (ctx is android.content.ContextWrapper) {
                if (ctx is Activity) break
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
