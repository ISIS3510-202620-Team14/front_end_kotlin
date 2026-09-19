package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditarGrupoScreen(
    grupo: Grupo,
    materia: String,
    onBack: () -> Unit,
    onNombreChange: (String) -> Unit,
    onDocenteChange: (String) -> Unit,
    onQuitarNino: (Nino) -> Unit,
    onAgregarNinos: (List<Nino>) -> Unit,
    onEliminarGrupo: () -> Unit,
    modifier: Modifier = Modifier
) {
    var menuDocenteAbierto by remember { mutableStateOf(false) }
    var mostrarMeterNinos by remember { mutableStateOf(false) }
    var ninosSeleccionados by remember { mutableStateOf(setOf<Nino>()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver")
            }
            Column {
                Text(text = "Editar grupo", style = typography.titleLarge)
                Text(text = materia, style = typography.bodyLarge, color = colorScheme.onSurfaceVariant)
            }
        }

        Text(text = "DATOS DEL GRUPO", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = grupo.nombre,
                    onValueChange = onNombreChange,
                    label = { Text("Nombre del grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { menuDocenteAbierto = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Docente", style = typography.bodyLarge)
                        Text(
                            text = grupo.docente,
                            style = typography.titleMedium,
                            modifier = Modifier.weight(1f).padding(start = 8.dp)
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuDocenteAbierto,
                        onDismissRequest = { menuDocenteAbierto = false },
                        containerColor = colorScheme.surface,
                        offset = DpOffset(x = 140.dp, y = 0.dp),
                        modifier = Modifier.width(240.dp)
                    ) {
                        docentesDisponibles.forEach { docente ->
                            val seleccionado = docente == grupo.docente
                            DropdownMenuItem(
                                text = { Text(docente, style = typography.titleMedium) },
                                onClick = {
                                    onDocenteChange(docente)
                                    menuDocenteAbierto = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                modifier = Modifier.background(
                                    if (seleccionado) colorScheme.surfaceVariant else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(text = "NIÑOS · ${grupo.ninos.size}", style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
        grupo.ninos.forEach { nino ->
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = nino.nombre, style = typography.titleMedium)
                        Text(
                            text = "${nino.nivel} · Grado ${nino.grado}",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { onQuitarNino(nino) }) {
                        Icon(Icons.Default.Close, contentDescription = "Quitar a ${nino.nombre}")
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = { mostrarMeterNinos = true },
                colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Text("Meter niños")
            }
            IconButton(onClick = onEliminarGrupo) {
                Icon(Icons.Default.DeleteOutline, contentDescription = "Eliminar grupo", tint = colorScheme.primary)
            }
        }
    }

    if (mostrarMeterNinos) {
        val disponibles = ninosDeEjemplo.filter { it !in grupo.ninos }
        MeterNinosDialog(
            titulo = "Meter niños a ${grupo.nombre}",
            ninos = disponibles,
            seleccionados = ninosSeleccionados,
            onSeleccionadosChange = { ninosSeleccionados = it },
            onAgregar = {
                onAgregarNinos(ninosSeleccionados.toList())
                ninosSeleccionados = emptySet()
                mostrarMeterNinos = false
            },
            onDismiss = {
                ninosSeleccionados = emptySet()
                mostrarMeterNinos = false
            }
        )
    }
}
