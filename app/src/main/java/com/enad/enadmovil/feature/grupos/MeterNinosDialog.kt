package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.Nino

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeterNinosDialog(
    ninos: List<Nino>,
    seleccionados: Set<Nino>,
    onSeleccionadosChange: (Set<Nino>) -> Unit,
    onAgregar: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "Meter niños a este grupo",
                style = typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            LazyColumn {
                items(ninos) { nino ->
                    val marcado = nino in seleccionados
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Checkbox(
                            checked = marcado,
                            onCheckedChange = { checked ->
                                onSeleccionadosChange(
                                    if (checked) seleccionados + nino else seleccionados - nino
                                )
                            }
                        )
                        Column {
                            Text(text = nino.nombre, style = typography.titleMedium)
                            Text(
                                text = "${nino.nivel} · Grado ${nino.grado}",
                                style = typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Button(
                onClick = onAgregar,
                enabled = seleccionados.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Text("Agregar")
            }
        }
    }
}
