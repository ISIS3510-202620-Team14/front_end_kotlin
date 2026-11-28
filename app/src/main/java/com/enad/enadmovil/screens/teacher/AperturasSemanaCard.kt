package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk

@Composable
fun AperturasSemanaCard(state: AperturasUiState, onEnviar: () -> Unit, modifier: Modifier = Modifier) {
    val fraccion = if (state.cargado) (state.aperturas.toFloat() / state.meta).coerceIn(0f, 1f) else 0f
    EnadPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Uso de la app esta semana", style = MaterialTheme.typography.titleMedium)
            Text(
                if (state.cargado) "${state.aperturas}/${state.meta}" else "…",
                style = MaterialTheme.typography.titleLarge,
                color = EnadPrimary
            )
        }
        Spacer(Modifier.height(10.dp))
        EnadProgress(fraccion = fraccion, color = EnadPrimary)
        Spacer(Modifier.height(8.dp))
        Text(
            if (state.cargado) state.mensaje else "Contando tus aperturas…",
            style = MaterialTheme.typography.bodyMedium
        )
        if (state.cargado && state.pendientes > 0) {
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.pendientes == 1) "1 apertura está guardada en tu teléfono y se enviará cuando haya conexión."
                else "${state.pendientes} aperturas están guardadas en tu teléfono y se enviarán cuando haya conexión.",
                style = MaterialTheme.typography.bodyMedium,
                color = EnadSoftInk
            )
            TextButton(onClick = onEnviar) { Text("Enviar ahora", color = EnadPrimary) }
        }
    }
}
