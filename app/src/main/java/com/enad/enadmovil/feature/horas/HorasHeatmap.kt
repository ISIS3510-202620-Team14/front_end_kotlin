package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.ui.theme.EnadChipPink
import com.enad.enadmovil.ui.theme.EnadHeatmapEmpty
import com.enad.enadmovil.ui.theme.EnadHeatmapStep2
import com.enad.enadmovil.ui.theme.EnadHeatmapStep3
import com.enad.enadmovil.domain.model.DiaSesion

@Composable
fun HorasHeatmap(
    dias: List<DiaSesion>,
    maxHorasReales: Double,
    onDiaClick: (DiaSesion) -> Unit,
    modifier: Modifier = Modifier
) {
    val semanas = remember(dias) { dias.chunked(5) }
    val etiquetasDias = listOf("L", "M", "M", "J", "V")

    Column(modifier = modifier) {
        Row(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            Column {
                etiquetasDias.forEach { etiqueta ->
                    Box(
                        modifier = Modifier.size(16.dp).padding(bottom = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = etiqueta, style = typography.labelSmall)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            semanas.forEach { semana ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    semana.forEach { dia ->
                        CeldaHeatmap(
                            dia = dia,
                            maxHorasReales = maxHorasReales,
                            onClick = { onDiaClick(dia) }
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                }
                Spacer(Modifier.width(4.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        LeyendaHeatmap()
    }
}

@Composable
private fun CeldaHeatmap(
    dia: DiaSesion,
    maxHorasReales: Double,
    onClick: () -> Unit
) {
    val nivel = dia.nivelIntensidad(maxHorasReales)
    val color = when (nivel) {
        0 -> EnadHeatmapEmpty
        1 -> EnadChipPink
        2 -> EnadHeatmapStep2
        3 -> EnadHeatmapStep3
        else -> colorScheme.primary
    }
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color)
            .clickable(onClick = onClick)
    )
}

@Composable
private fun LeyendaHeatmap() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = "Menos", style = typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(6.dp))
        listOf(EnadHeatmapEmpty, EnadChipPink, EnadHeatmapStep2, EnadHeatmapStep3, colorScheme.primary)
            .forEach { color ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Spacer(Modifier.width(4.dp))
            }
        Text(text = "Más", style = typography.bodyMedium, color = colorScheme.onSurfaceVariant)
    }
}
