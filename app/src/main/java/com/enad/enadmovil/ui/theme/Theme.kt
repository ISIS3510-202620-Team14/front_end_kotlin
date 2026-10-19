package com.enad.enadmovil.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Radios compartidos con Flutter: 20 dp para paneles y 16 dp para controles.
val PanelRadius = 20.dp
val ControlRadius = 16.dp
val PageInset = 20.dp
val PanelShape = RoundedCornerShape(PanelRadius)
val ControlShape = RoundedCornerShape(ControlRadius)

// Rojo para acciones (primary: Button y FilledButton) y negro para selección y confirmación
// (secondary: chips, toggles y snackbars).
private val LightColorScheme = lightColorScheme(
    primary = EnadPrimary,
    onPrimary = Color.White,
    secondary = EnadInk,
    onSecondary = EnadSurface,
    secondaryContainer = EnadNeutral,
    onSecondaryContainer = EnadInk,
    tertiary = EnadAmber,
    onTertiary = EnadSurface,
    tertiaryContainer = EnadAmberBg,
    onTertiaryContainer = EnadPending,
    background = EnadBackground,
    onBackground = EnadInk,
    surface = EnadSurface,
    onSurface = EnadInk,
    surfaceVariant = EnadNeutral,
    onSurfaceVariant = EnadSoftInk,
    surfaceContainerLowest = EnadSurface,
    surfaceContainerLow = EnadBackground,
    surfaceContainer = EnadNeutral,
    surfaceContainerHigh = EnadNeutralHigh,
    surfaceTint = Color.Transparent,
    outline = EnadSoftInk,
    outlineVariant = EnadLine,
    error = EnadPrimary,
    onError = EnadSurface,
    inverseSurface = EnadInk,
    inverseOnSurface = EnadSurface,
)

private val EnadShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = ControlShape,
    medium = ControlShape,
    large = PanelShape,
    extraLarge = PanelShape,
)

// La app solo tiene tema claro, igual que Flutter. Los parámetros se conservan
// para no romper las llamadas existentes.
@Composable
fun EnadMovilTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = false,
    @Suppress("UNUSED_PARAMETER") dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        shapes = EnadShapes,
        content = content
    )
}
