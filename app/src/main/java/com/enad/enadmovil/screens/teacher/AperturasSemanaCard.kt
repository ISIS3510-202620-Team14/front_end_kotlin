package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.theme.EnadPrimary

@Composable
fun AperturasSemanaCard(state: AperturasUiState, modifier: Modifier = Modifier) {
    val fraccion = (state.aperturas.toFloat() / state.meta).coerceIn(0f, 1f)
    EnadPanel(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Uso de la app esta semana", style = MaterialTheme.typography.titleMedium)
            Text(
                "${state.aperturas}/${state.meta}",
                style = MaterialTheme.typography.titleLarge,
                color = EnadPrimary
            )
        }
        Spacer(Modifier.height(10.dp))
        EnadProgress(fraccion = fraccion, color = EnadPrimary)
        if (state.mostrarRecordatorio) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Vuelve a abrir la app esta semana para mantener al día el progreso de tus estudiantes.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
