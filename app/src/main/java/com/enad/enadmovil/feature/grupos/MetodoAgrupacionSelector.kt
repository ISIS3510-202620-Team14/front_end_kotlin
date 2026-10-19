package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.PanTool
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Recomendacion
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSuccessBg
import com.enad.enadmovil.ui.theme.EnadSuccessText
import com.enad.enadmovil.ui.theme.EnadSurface
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

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Método de agrupación",
            style = typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = EnadInk
        )
        OpcionMetodo(
            icono = Icons.Outlined.AutoAwesome,
            titulo = "Automática",
            descripcion = "La app distribuye los estudiantes según su nivel",
            seleccionada = seleccionado == MetodoAgrupacion.AUTOMATICO,
            confianza = confianzaDe(MetodoAgrupacion.AUTOMATICO),
            onClick = { onCambiar(MetodoAgrupacion.AUTOMATICO) }
        )
        OpcionMetodo(
            icono = Icons.Outlined.PanTool,
            titulo = "Manual",
            descripcion = "Tú eliges qué estudiantes van en cada grupo",
            seleccionada = seleccionado == MetodoAgrupacion.MANUAL,
            confianza = confianzaDe(MetodoAgrupacion.MANUAL),
            onClick = { onCambiar(MetodoAgrupacion.MANUAL) }
        )
    }
}

@Composable
private fun OpcionMetodo(
    icono: ImageVector,
    titulo: String,
    descripcion: String,
    seleccionada: Boolean,
    confianza: Double?,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (seleccionada) EnadSuccessBg else EnadSurface,
        border = BorderStroke(if (seleccionada) 2.dp else 1.dp, if (seleccionada) EnadSuccessText else EnadLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Icon(icono, contentDescription = null, tint = if (seleccionada) EnadSuccessText else EnadSoftInk, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = titulo,
                        style = typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                        color = EnadInk
                    )
                    if (confianza != null) InsigniaRecomendado(confianza)
                }
                Text(text = descripcion, style = typography.bodySmall.copy(fontSize = 12.sp))
            }
            Icon(
                if (seleccionada) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (seleccionada) EnadSuccessText else EnadLine,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun InsigniaRecomendado(confianza: Double) {
    Surface(color = EnadSuccessText.copy(alpha = 0.12f), shape = RoundedCornerShape(10.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Icon(Icons.Outlined.TipsAndUpdates, contentDescription = null, tint = EnadSuccessText, modifier = Modifier.size(12.dp))
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Recomendado · ${(confianza * 100).roundToInt()}%",
                style = typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold),
                color = EnadSuccessText
            )
        }
    }
}
