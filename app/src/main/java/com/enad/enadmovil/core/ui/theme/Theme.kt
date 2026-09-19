package com.enad.enadmovil.core.ui.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = EnadPrimary,
    secondary = PurpleGrey80,
    tertiary = Pink80,
    background = Color(0xFF1C1B19),
    surface = Color(0xFF2B2320),
    onBackground = Color(0xFFF7F1E8),
    onSurface = Color(0xFFF7F1E8)
)

private val LightColorScheme = lightColorScheme(
    primary = EnadPrimary,
    secondary = PurpleGrey40,
    tertiary = Pink40,
    background = EnadBackground,
    surface = EnadSurface,
    surfaceVariant = EnadCardAlt,
    onPrimary = Color.White,
    onBackground = EnadOnBackground,
    onSurface = EnadOnBackground,
    onSurfaceVariant = EnadMuted,
    outline = EnadBorder
)

@Composable
fun AppTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
