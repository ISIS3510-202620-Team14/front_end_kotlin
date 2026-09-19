package com.enad.enadmovil.feature.misdatos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.core.ui.theme.greenBackgroundContainerLight
import com.enad.enadmovil.core.ui.theme.pendingLight
import com.enad.enadmovil.domain.model.EnvioPendiente

private val enviosEjemplo = listOf(
    EnvioPendiente("Niveles de matemáticas", "Hoy, 9:40 a. m.", 12),
    EnvioPendiente("Niveles de lectura", "Hoy, 10:15 a. m.", 9),
    EnvioPendiente("Asistencia Grupo Abejitas", "Ayer, 2:30 p. m.", 3),
)

@Composable
fun MisDatosScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Mis datos", style = typography.displaySmall)
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(greenBackgroundContainerLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = colorScheme.secondary
                        )
                    }
                    Column {
                        Text(text = "Guardado en esta sesión demo", style = typography.titleMedium)
                        Text(
                            text = "Los datos de prueba se reinician al cerrar la app.",
                            style = typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Pendiente de envío simulado",
                            style = typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = enviosEjemplo.sumOf { it.cantidad }.toString(),
                            style = typography.displaySmall,
                            color = pendingLight
                        )
                    }
                    enviosEjemplo.forEachIndexed { index, envio ->
                        FilaEnvioPendiente(envio)
                        if (index != enviosEjemplo.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ÚLTIMO ENVÍO SIMULADO",
                        style = typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    Text(text = "Ayer, 4:12 p. m. · 31 registros", style = typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun FilaEnvioPendiente(envio: EnvioPendiente) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = envio.etiqueta, style = typography.bodyLarge)
            Text(text = envio.horaTexto, style = typography.bodyMedium, color = colorScheme.onSurfaceVariant)
        }
        Text(text = envio.cantidad.toString(), style = typography.titleLarge)
    }
}
