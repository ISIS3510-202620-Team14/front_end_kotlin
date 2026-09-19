package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.DiaSesion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistrarHorasSheet(dia: DiaSesion, onRegistrar: (Double) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp).padding(bottom = 24.dp)) {
            Text(text = "Horas del ${dia.fecha.dayOfMonth}/${dia.fecha.monthValue}", style = typography.titleLarge)
            Text(text = "Programadas: ${formatearHoras(dia.horasProgramadas)} h", style = typography.bodyMedium)
            Row(
                modifier = Modifier.padding(top = 12.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0.0, 0.5, 1.0, 1.5, 2.0, 2.5, 3.0).forEach { horas ->
                    AssistChip(
                        onClick = { onRegistrar(horas) },
                        label = { Text("${formatearHoras(horas)} h") }
                    )
                }
            }
        }
    }
}