package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarHorasSheet(
    registro: RegistroDiaUi,
    onHorasChange: (String) -> Unit,
    onMotivoChange: (String) -> Unit,
    onGuardar: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.padding(16.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Horas del ${registro.fecha.dayOfMonth}/${registro.fecha.monthValue}", style = typography.titleLarge)
            Text(text = "Programadas: ${formatearHoras(registro.planeadas)} h", style = typography.bodyMedium)
            OutlinedTextField(
                value = registro.texto,
                onValueChange = onHorasChange,
                label = { Text("Horas realizadas") },
                suffix = { Text("h") },
                supportingText = if (registro.sugerido) {
                    { Text("Sugerido, ajústalo") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0).forEach { horas ->
                    AssistChip(
                        onClick = { onHorasChange(formatearHoras(horas)) },
                        label = { Text("${formatearHoras(horas)} h") }
                    )
                }
            }
            if (registro.pideMotivo) {
                OutlinedTextField(
                    value = registro.motivo,
                    onValueChange = onMotivoChange,
                    label = { Text("Motivo de horas no realizadas") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(onClick = onGuardar, enabled = registro.puedeGuardar, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }
        }
    }
}
