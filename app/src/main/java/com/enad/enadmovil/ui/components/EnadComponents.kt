package com.enad.enadmovil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.R
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadHeader
import com.enad.enadmovil.ui.theme.EnadHeaderMuted
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface
import com.enad.enadmovil.ui.theme.PanelShape

// Piezas compartidas por todas las pantallas, equivalentes a los widgets de Flutter
// (home_top_bar, home_bottom_nav, enad_panel, quick_action_card, brand_heading).

/** Barra superior negra: isotipo, "Portal docente", nombre y "Cambiar usuario". */
@Composable
fun EnadTopBar(
    nombre: String,
    onCambiarUsuario: () -> Unit,
    portalLabel: String = "Portal\ndocente",
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.enad_isotipo_white),
            contentDescription = "ENAd",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 28.dp, height = 40.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(portalLabel, style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp, lineHeight = 16.sp), color = EnadSurface)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.width(1.dp).height(30.dp).background(Color(0xFF747470)))
        Spacer(Modifier.width(10.dp))
        Text(
            text = nombre,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = EnadSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(12.dp))
        OutlinedButton(
            onClick = onCambiarUsuario,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, EnadHeaderMuted),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = EnadSurface),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                "Cambiar\nusuario",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(lineHeight = 14.sp),
            )
        }
    }
}

data class EnadNavItem(val label: String, val icon: ImageVector, val badge: Int = 0)

// Pestañas del docente, en el mismo orden que TeacherTabs.
val TeacherNavItems = listOf(
    EnadNavItem("Hoy", Icons.Outlined.CalendarToday),
    EnadNavItem("Asistencia", Icons.AutoMirrored.Outlined.ListAlt),
    EnadNavItem("Grupos", Icons.Outlined.Groups),
    EnadNavItem("Horas", Icons.Outlined.CalendarMonth),
    EnadNavItem("Mis datos", Icons.Outlined.Person),
)

/** Barra inferior: la pestaña activa lleva una píldora negra detrás del icono. */
@Composable
fun EnadBottomBar(
    items: List<EnadNavItem>,
    seleccionado: Int,
    onSeleccionar: (Int) -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(EnadSurface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(EnadLine))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEachIndexed { i, item ->
                val activo = i == seleccionado
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSeleccionar(i) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 52.dp, height = 32.dp)
                            .background(if (activo) EnadInk else Color.Transparent, RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        BadgedBox(badge = {
                            if (item.badge > 0) Badge(containerColor = EnadInk, contentColor = EnadSurface) { Text("${item.badge}") }
                        }) {
                            Icon(item.icon, contentDescription = null, tint = if (activo) EnadSurface else EnadInk, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(item.label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp), textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
    }
}

/** Título de sección en Lexend ("Hoy", "Asistencia", "Mis grupos"...). */
@Composable
fun EnadHeading(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.headlineLarge, modifier = modifier)
}

/** Panel blanco con borde suave y radio de 20 dp. */
@Composable
fun EnadPanel(
    modifier: Modifier = Modifier,
    color: Color = EnadSurface,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val border = BorderStroke(1.dp, EnadLine)
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier.fillMaxWidth(), shape = PanelShape, color = color, border = border) {
            Column(Modifier.padding(20.dp), content = content)
        }
    } else {
        Surface(modifier = modifier.fillMaxWidth(), shape = PanelShape, color = color, border = border) {
            Column(Modifier.padding(20.dp), content = content)
        }
    }
}

/** Tarjeta de acceso de "Hoy": icono rojo en un cuadro cálido, título y chevron. */
@Composable
fun EnadActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = PanelShape,
        color = EnadSurface,
        border = BorderStroke(1.dp, EnadLine),
    ) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(48.dp).background(EnadBackground, ControlShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = EnadSoftInk)
        }
    }
}

/** Botón principal rojo; deshabilitado queda gris, como en Flutter. */
@Composable
fun EnadPrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ControlShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = EnadPrimary,
            contentColor = EnadSurface,
            disabledContainerColor = EnadLine,
            disabledContentColor = EnadSoftInk,
        ),
    ) { Text(text, style = MaterialTheme.typography.labelLarge) }
}

/** Botón secundario blanco con borde suave e icono rojo opcional ("Crear grupo", "Gestionar estudiantes"). */
@Composable
fun EnadSecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 48.dp),
        shape = ControlShape,
        border = BorderStroke(1.dp, EnadLine),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = EnadSurface, contentColor = EnadInk),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

/** Toggle de dos o más opciones: la activa en negro con texto blanco (materia, Asistió/No asistió). */
@Composable
fun EnadSegmented(
    opciones: List<String>,
    seleccionada: Int?,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp)
            .border(1.dp, EnadSoftInk, RoundedCornerShape(50))
            .padding(0.dp),
    ) {
        opciones.forEachIndexed { i, opcion ->
            val activa = i == seleccionada
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp)
                    .background(if (activa) EnadInk else Color.Transparent, RoundedCornerShape(50))
                    .clickable { onSeleccionar(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(opcion, style = MaterialTheme.typography.labelMedium, color = if (activa) EnadSurface else EnadInk)
            }
        }
    }
}

/** Barra de progreso negra sobre pista gris. */
@Composable
fun EnadProgress(fraccion: Float, modifier: Modifier = Modifier, color: Color = EnadInk) {
    LinearProgressIndicator(
        progress = { fraccion.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(4.dp),
        color = color,
        trackColor = EnadLine,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Campo de texto con borde gris, foco negro de 2 dp y radio de 16 dp (appInputDecoration en Flutter). */
@Composable
fun EnadTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    floatingLabel: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        // Sin etiqueta flotante (login) el texto queda como placeholder, como en Flutter.
        label = if (floatingLabel) ({ Text(label) }) else null,
        placeholder = if (floatingLabel) null else ({ Text(label, color = EnadSoftInk) }),
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        isError = isError,
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        supportingText = supportingText?.let { { Text(it) } },
        shape = ControlShape,
        textStyle = MaterialTheme.typography.bodyLarge,
        colors = enadFieldColors(),
    )
}

@Composable
fun enadFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = EnadSurface,
    unfocusedContainerColor = EnadSurface,
    disabledContainerColor = EnadSurface,
    unfocusedBorderColor = EnadSoftInk,
    focusedBorderColor = EnadInk,
    errorBorderColor = EnadPrimary,
    focusedLabelColor = EnadSoftInk,
    unfocusedLabelColor = EnadSoftInk,
    cursorColor = EnadInk,
    focusedTextColor = EnadInk,
    unfocusedTextColor = EnadInk,
)
