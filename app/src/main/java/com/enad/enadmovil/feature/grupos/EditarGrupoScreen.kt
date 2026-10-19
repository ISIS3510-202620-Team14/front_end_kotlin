package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino
import com.enad.enadmovil.ui.screens.teacher.BarraConVolver
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadNeutral
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSurface

/** "Detalle del grupo" de Flutter: cambiar el nombre, marcar integrantes y eliminar el grupo. */
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
    var nombre by rememberSaveable(grupo.id) { mutableStateOf(grupo.nombre) }
    var confirmarEliminar by remember { mutableStateOf(false) }
    val integrantes = grupo.ninos.map { it.id }.toSet()

    Column(modifier = modifier.fillMaxSize().background(EnadBackground)) {
        BarraConVolver(titulo = "Detalle del grupo", onBack = onBack, subtitulo = materia)
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            EnadPanel {
                EnadTextField(value = nombre, onValueChange = { nombre = it }, label = "Nombre del grupo")
                Spacer(Modifier.height(20.dp))
                EnadPrimaryButton(
                    text = "Guardar nombre",
                    onClick = { onNombreChange(nombre.trim()) },
                    enabled = nombre.isNotBlank() && nombre.trim() != grupo.nombre
                )
            }
            Spacer(Modifier.height(12.dp))
            Text("Integrantes", style = typography.titleLarge)
            ninosDeEjemplo.forEach { nino ->
                val marcado = nino.id in integrantes
                EnadPanel(onClick = { if (marcado) onQuitarNino(nino) else onAgregarNinos(listOf(nino)) }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(nino.nombre, style = typography.titleMedium)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Grado ${nino.grado}", style = typography.bodyMedium)
                                Spacer(Modifier.width(8.dp))
                                Surface(color = EnadNeutral, shape = RoundedCornerShape(50)) {
                                    Text(
                                        nino.nivel,
                                        style = typography.labelSmall,
                                        color = EnadInk,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                        }
                        Checkbox(
                            checked = marcado,
                            onCheckedChange = { if (it) onAgregarNinos(listOf(nino)) else onQuitarNino(nino) },
                            colors = CheckboxDefaults.colors(checkedColor = EnadInk, checkmarkColor = EnadSurface)
                        )
                    }
                }
            }
            TextButton(onClick = { confirmarEliminar = true }) {
                Text("Eliminar grupo", color = EnadPrimary)
            }
        }
    }

    if (confirmarEliminar) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            containerColor = EnadSurface,
            title = { Text("Eliminar grupo") },
            text = { Text("Se eliminará este grupo; los estudiantes se conservarán.") },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text("Cancelar", color = EnadInk) }
            },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarEliminar = false
                        onEliminarGrupo()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary)
                ) { Text("Eliminar") }
            }
        )
    }
}
