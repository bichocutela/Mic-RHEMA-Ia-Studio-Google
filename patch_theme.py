import re

with open('app/src/main/java/com/aistudio/micrhema/ui/theme/Theme.kt', 'r') as f:
    content = f.read()

target = """private val LightColorScheme = lightColorScheme(
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
)"""

replacement = """private val LightColorScheme = lightColorScheme(
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
)"""

content = content.replace(target, replacement)

target_theme = """@Composable
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
    }"""

replacement_theme = """@Composable
fun MICRhemaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false, // Disabled dynamic color to force our harmonized theme
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme // Force Light Scheme as requested
"""

content = content.replace(target_theme, replacement_theme)

target_status_bar = """                val statusBarColor = if (darkTheme) Color(0xFF090E17) else Color(0xFFFFFFFF)
                window.statusBarColor = statusBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme"""

replacement_status_bar = """                val statusBarColor = Color(0xFFFFFDF5)
                window.statusBarColor = statusBarColor.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true"""

content = content.replace(target_status_bar, replacement_status_bar)

with open('app/src/main/java/com/aistudio/micrhema/ui/theme/Theme.kt', 'w') as f:
    f.write(content)
