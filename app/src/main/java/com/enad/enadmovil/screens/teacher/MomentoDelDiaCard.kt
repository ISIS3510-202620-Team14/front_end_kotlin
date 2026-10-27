package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.domain.model.AccionSugerida
import com.enad.enadmovil.domain.model.MomentoDelDia
import com.enad.enadmovil.domain.model.ResumenDelDia
import com.enad.enadmovil.feature.horas.formatearHoras
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadSuccessBg
import com.enad.enadmovil.ui.theme.EnadSuccessText
import com.enad.enadmovil.ui.theme.EnadSurface
import com.enad.enadmovil.ui.theme.PanelShape

/** Textos de la tarjeta para un momento del día. [boton] null = no hay nada pendiente. */
private data class TextosMomento(val etiqueta: String, val titulo: String, val detalle: String, val boton: String?)

private fun textosDe(momento: MomentoDelDia, resumen: ResumenDelDia): TextosMomento {
    val grupo = resumen.grupo ?: "tu grupo"
    val planeadas = "${formatearHoras(resumen.horasPlaneadas)} h planeadas hoy"
    return when (momento) {
        MomentoDelDia.SIN_CLASE -> TextosMomento(
            "Sin clase hoy", "Hoy no tienes clase en tu horario",
            "Aprovecha para planear las actividades de la semana.", "Planear actividades"
        )
        MomentoDelDia.INICIO -> TextosMomento(
            "Para empezar", "Toma la asistencia de $grupo",
            "Tienes $planeadas.", "Reportar asistencia"
        )
        MomentoDelDia.EN_CLASE -> TextosMomento(
            "En clase", "Clase en curso con $grupo",
            "Ya registraste ${resumen.marcasAsistencia} asistencia(s). Es buen momento para la evaluación formativa.",
            "Evaluación formativa"
        )
        MomentoDelDia.CIERRE -> TextosMomento(
            "Cierre del día", "Registra tus horas de hoy",
            listOfNotNull(
                planeadas.replaceFirstChar { it.uppercase() },
                resumen.horasEnSede?.let { "en la sede llevas ${formatearHoras(it)} h" }
            ).joinToString(" · ") + ".",
            "Reportar horas"
        )
        MomentoDelDia.SIN_ASISTENCIA -> TextosMomento(
            "Pendiente", "Hoy no registraste asistencia",
            "Si diste clase con $grupo, regístrala antes de terminar el día.", "Reportar asistencia"
        )
        MomentoDelDia.LISTO -> TextosMomento(
            "Listo", "Todo listo por hoy",
            "Asistencia y horas registradas. ¡Buen trabajo!", null
        )
    }
}

/**
 * Context-aware: "Hoy" propone el siguiente paso según la hora, el horario del grupo y lo que el
 * docente ya registró (ver [MomentoDelDia]). Sin momento calculado todavía no se muestra.
 */
@Composable
fun MomentoDelDiaCard(
    momento: MomentoDelDia?,
    resumen: ResumenDelDia?,
    onAccion: (AccionSugerida) -> Unit,
    modifier: Modifier = Modifier
) {
    if (momento == null || resumen == null) return
    val textos = textosDe(momento, resumen)
    val listo = momento == MomentoDelDia.LISTO

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(if (listo) EnadSuccessBg else EnadSurface, PanelShape)
            .border(1.dp, EnadLine, PanelShape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Etiqueta(
            texto = textos.etiqueta,
            fondo = if (listo) EnadSurface else EnadPendienteBg,
            color = if (listo) EnadSuccessText else EnadPendienteText
        )
        Text(textos.titulo, style = MaterialTheme.typography.titleMedium)
        Text(textos.detalle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        textos.boton?.let { boton ->
            EnadPrimaryButton(
                boton,
                onClick = { onAccion(momento.accion) },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}
