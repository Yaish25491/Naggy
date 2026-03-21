package com.yaish.naggy.ui.theme

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

// --- Dark Mode: Glassmorphic Minimalist (Updated to Deep Space) ---
val DeepSpace = Color(0xFF080A10)
val TwilightGrey = Color(0xFF14171F)
val CloudWhiteDark = Color(0xFFF1F3F5)
val SilverGrey = Color(0xFFA0A8B0)
val IceBlue = Color(0xFFA5D8FF)

// --- Light Mode: Ethereal Glass ---
val PristineBackground = Color(0xFFF8F9FF)
val OffBlack = Color(0xFF202020)
val SkyBlueAccent = Color(0xFF40AFFF)

private val DarkColorScheme = darkColorScheme(
    primary = IceBlue,
    onPrimary = DeepSpace,
    primaryContainer = TwilightGrey,
    onPrimaryContainer = CloudWhiteDark,
    secondary = SilverGrey,
    onSecondary = DeepSpace,
    background = DeepSpace,
    onBackground = CloudWhiteDark,
    surface = TwilightGrey,
    onSurface = CloudWhiteDark,
    surfaceVariant = TwilightGrey.copy(alpha = 0.7f),
    onSurfaceVariant = SilverGrey,
    error = Color(0xFFFF5252),
    outline = SilverGrey.copy(alpha = 0.2f)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlueAccent,
    onPrimary = Color.White,
    primaryContainer = SkyBlueAccent.copy(alpha = 0.1f),
    onPrimaryContainer = Color(0xFF004080),
    secondary = Color(0xFF004080),
    onSecondary = Color.White,
    background = PristineBackground,
    onBackground = OffBlack,
    surface = Color.White,
    onSurface = OffBlack,
    surfaceVariant = Color.White.copy(alpha = 0.5f),
    onSurfaceVariant = OffBlack.copy(alpha = 0.6f),
    error = Color(0xFFFF3B30),
    outline = Color.White
)

@Composable
fun TodoAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
