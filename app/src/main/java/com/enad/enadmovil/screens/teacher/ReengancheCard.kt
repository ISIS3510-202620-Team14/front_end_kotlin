package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.theme.EnadAmber
import com.enad.enadmovil.ui.theme.EnadAmberBg
import java.util.Locale

@Composable
fun ReengancheCard(
    estado: AperturasUiState,
    onReportarAsistencia: () -> Unit,
    onAhoraNo: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!estado.mostrarRiesgo) return
    EnadPanel(modifier = modifier, color = EnadAmberBg) {
        Text("Has entrado menos que antes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "La semana pasada abriste la app ${veces(estado.riesgo.semanaPasada.toDouble())}; " +
                "antes lo hacías unas ${veces(estado.riesgo.semanaHabitual)} por semana. " +
                "Con unos minutos hoy dejas al día la asistencia de tus estudiantes.",
            style = MaterialTheme.typography.bodyMedium,
            color = EnadAmber
        )
        Spacer(Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EnadPrimaryButton("Reportar asistencia", onReportarAsistencia, Modifier.weight(1f))
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = onAhoraNo) { Text("Ahora no") }
        }
    }
}

internal fun veces(valor: Double): String {
    val numero = if (valor % 1.0 == 0.0) valor.toInt().toString()
    else String.format(Locale.US, "%.1f", valor).replace('.', ',')
    return if (valor == 1.0) "$numero vez" else "$numero veces"
}