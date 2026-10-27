package com.enad.enadmovil.feature.ayuda

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.PanelShape
import com.enad.enadmovil.ui.theme.EnadSurface

private fun iconoDe(guia: Guia): ImageVector = when (guia) {
    Guia.HORAS -> Icons.Outlined.AccessTime
    Guia.ASISTENCIA -> Icons.Outlined.FactCheck
    Guia.EVALUACION -> Icons.Outlined.Assessment
    Guia.ACTIVIDADES -> Icons.AutoMirrored.Outlined.MenuBook
}

/**
 * Botón "Ayuda" de Hoy. No usa ExtendedFloatingActionButton: con una elevación tonal de 6 dp, Material 3
 * cambia un fondo igual al surface por surfaceContainerHigh, que en este tema es gris. Flutter lo usa blanco.
 */
@Composable
fun BotonAyuda(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = PanelShape,
        color = EnadSurface,
        contentColor = EnadInk,
        tonalElevation = 0.dp,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier.padding(start = 20.dp, end = 24.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null)
            Text("Ayuda", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyudaSheet(onElegir: (Guia) -> Unit, onCerrar: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onCerrar, containerColor = EnadSurface) {
        Column(Modifier.navigationBarsPadding().padding(start=24.dp, end=24.dp, bottom = 24.dp)) {
            Text("¿Qué puedes hacer?", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(12.dp))
            Guia.entries.forEach { guia ->
                ListItem(
                    headlineContent = { Text(guia.titulo)},
                    leadingContent = { Icon(iconoDe(guia), contentDescription = null, tint = EnadPrimary )},
                    trailingContent = { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)},
                    colors = ListItemDefaults.colors(containerColor = EnadSurface),
                    modifier = Modifier.clickable { onElegir(guia)}
                )

            }
        }
    }
}