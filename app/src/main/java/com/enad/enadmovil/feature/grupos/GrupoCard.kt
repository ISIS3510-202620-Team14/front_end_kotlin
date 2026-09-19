package com.enad.enadmovil.feature.grupos

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme.colorScheme

@Composable
fun GrupoCard(nombre: String, cantidadNinos: Int, docente: String, onVer: () -> Unit, onEditar: () -> Unit, modifier: Modifier = Modifier) {
    OutlinedCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = nombre, style = typography.titleMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onVer) {
                    Icon(Icons.Outlined.Visibility, contentDescription = "Ver $nombre")
                }
                IconButton(onClick = onEditar) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Editar $nombre")
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                Text(
                    text = if (cantidadNinos == 1) "1 niño" else "$cantidadNinos niños",
                    style = typography.bodyMedium
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.School, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                Text(
                    text = docente, style = typography.bodyMedium
                )
            }
        }

    }

}