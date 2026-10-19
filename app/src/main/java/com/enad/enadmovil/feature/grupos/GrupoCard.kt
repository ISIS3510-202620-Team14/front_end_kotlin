package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.EnadPrimary

/** Tarjeta de un grupo: toda la tarjeta abre el detalle, como en Flutter. */
@Composable
fun GrupoCard(nombre: String, cantidadNinos: Int, docente: String, onVer: () -> Unit, onEditar: () -> Unit, modifier: Modifier = Modifier) {
    EnadPanel(modifier = modifier, onClick = onEditar) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = nombre, style = typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = EnadPrimary)
        }
        Spacer(Modifier.height(16.dp))
        DatoGrupo(Icons.Outlined.PersonOutline, if (cantidadNinos == 1) "1 estudiante" else "$cantidadNinos estudiantes")
        Spacer(Modifier.height(10.dp))
        DatoGrupo(Icons.Outlined.School, docente)
    }
}

@Composable
private fun DatoGrupo(icon: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text = texto, style = typography.bodyMedium)
    }
}
