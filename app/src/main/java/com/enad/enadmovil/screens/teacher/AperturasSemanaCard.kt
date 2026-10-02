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
import com.enad.enadmovil.ui.theme.EnadTrack

@Composable
fun AperturasSemanaCard(state: AperturasUiState, modifier: Modifier = Modifier) {
    val fraccion = (state.aperturas.toFloat() / state.meta).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Uso de la app esta semana",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                "${state.aperturas}/${state.meta}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).background(EnadTrack, RoundedCornerShape(3.dp))) {
            Box(
                Modifier.fillMaxWidth(fraccion).height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
        }
        if (state.mostrarRecordatorio) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Vuelve a abrir la app esta semana para mantener al día el progreso de tus estudiantes.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}