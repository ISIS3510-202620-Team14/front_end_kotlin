package com.enad.enadmovil.ui.screens.teacher.scan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.domain.model.EstudianteEscaneado

/** Pantalla de revisión: el docente ve lo que leyó el OCR antes de importar. */
@Composable
fun ScanReviewDialog(
    estado: ScanUiState,
    onImportar: (List<EstudianteEscaneado>) -> Unit,
    onCancelar: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        title = { Text("Lista escaneada", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
        text = {
            when {
                estado.procesando -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Leyendo la lista…", fontSize = 14.sp)
                }
                estado.error != null -> Text(
                    text = estado.error,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                else -> Column {
                    Text(
                        text = "Se detectaron ${estado.filas.size} estudiantes. Revisa antes de importar. " +
                                "Los que no tienen grado aparecen en rojo.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .heightIn(max = 320.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        estado.filas.forEach { fila ->
                            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                Text(
                                    text = fila.nombre,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = listOf(
                                        fila.codigo?.let { "Código $it" } ?: "sin código",
                                        fila.grado?.let { "Grado $it" } ?: "sin grado",
                                        fila.edad?.let { "$it años" } ?: "sin edad"
                                    ).joinToString(" · "),
                                    fontSize = 12.sp,
                                    color = if (fila.grado == null) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onImportar(estado.filas) },
                enabled = !estado.procesando && estado.filas.isNotEmpty()
            ) {
                Text("Importar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )
}