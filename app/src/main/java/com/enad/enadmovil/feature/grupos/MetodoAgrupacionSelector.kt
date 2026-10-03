package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Recomendacion
import kotlin.math.roundToInt

/** BQ 14: las dos formas de armar un grupo, con la insignia "Recomendado" en la que más se usa en salones parecidos. */
@Composable
fun MetodoAgrupacionSelector(
    seleccionado: MetodoAgrupacion,
    recomendacion: Recomendacion?,
    onCambiar: (MetodoAgrupacion) -> Unit,
    modifier: Modifier = Modifier
) {
    fun confianzaDe(metodo: MetodoAgrupacion): Double? =
        recomendacion?.takeIf { it.tieneDatosSuficientes && it.metodo == metodo }?.confianza

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "MÉTODO DE AGRUPACIÓN",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )
        OpcionMetodo(
            titulo = "Automática",
            descripcion = "La app distribuye los niños según su nivel",
            seleccionada = seleccionado == MetodoAgrupacion.AUTOMATICO,
            confianza = confianzaDe(MetodoAgrupacion.AUTOMATICO),
            onClick = { onCambiar(MetodoAgrupacion.AUTOMATICO) }
        )
        OpcionMetodo(
            titulo = "Manual",
            descripcion = "Tú eliges qué niños van en cada grupo",
            seleccionada = seleccionado == MetodoAgrupacion.MANUAL,
            confianza = confianzaDe(MetodoAgrupacion.MANUAL),
            onClick = { onCambiar(MetodoAgrupacion.MANUAL) }
        )
    }
}

@Composable
private fun OpcionMetodo(
    titulo: String,
    descripcion: String,
    seleccionada: Boolean,
    confianza: Double?,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        border = BorderStroke(
            width = if (seleccionada) 2.dp else 1.dp,
            color = if (seleccionada) colorScheme.primary else colorScheme.outlineVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            RadioButton(selected = seleccionada, onClick = null)
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(text = titulo, style = typography.titleMedium)
                    if (confianza != null) InsigniaRecomendado(confianza)
                }
                Text(text = descripcion, style = typography.bodyMedium, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InsigniaRecomendado(confianza: Double) {
    Surface(color = colorScheme.primaryContainer, shape = RoundedCornerShape(50)) {
        Text(
            text = "Recomendado · ${(confianza * 100).roundToInt()} %",
            style = typography.labelSmall,
            color = colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}
