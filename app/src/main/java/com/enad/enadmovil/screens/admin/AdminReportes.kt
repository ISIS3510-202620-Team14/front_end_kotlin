package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSuccessBg
import com.enad.enadmovil.ui.theme.EnadSuccessText

/** Reporte "Aperturas" (BQ #6): docentes que abrieron la app más de una vez en la semana. */
@Composable
fun ReporteAperturasSection(estado: AdminUiState, onCambiarSemana: (Long) -> Unit, onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Uso semanal de la app",
            descripcion = "Docentes que abrieron la app más de una vez en la semana.",
            cargando = estado.aperturas is Carga.Cargando,
            onActualizar = onActualizar
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { onCambiarSemana(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior", tint = EnadInk)
            }
            Text(
                semanaLegible(estado.semanaAperturas.toString()),
                style = MaterialTheme.typography.labelMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            val esActual = estado.semanaAperturas == lunesActual()
            IconButton(onClick = { onCambiarSemana(1) }, enabled = !esActual) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Semana siguiente",
                    tint = if (esActual) EnadSoftInk.copy(alpha = 0.4f) else EnadInk
                )
            }
        }
        ContenidoCarga(estado.aperturas, vacio = { false }, textoVacio = "") { r ->
            val fraccion = if (r.docentes == 0) 0.0 else r.docentesMasDeUna.toDouble() / r.docentes
            EnadPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Abrieron la app más de una vez", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (r.docentes == 0) "Ningún docente abrió la app esta semana."
                            else "${r.docentesMasDeUna} de ${r.docentes} docentes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    Text(
                        if (r.docentes == 0) "—" else porcentajeTexto(Math.round(fraccion * 1000) / 10.0),
                        style = MaterialTheme.typography.headlineSmall,
                        color = EnadPrimary
                    )
                }
                Spacer(Modifier.height(10.dp))
                EnadProgress(fraccion.toFloat(), color = EnadPrimary)
                if (r.provisional) {
                    Spacer(Modifier.height(10.dp))
                    PildoraProvisional()
                }
            }
        }
    }
}

/** Reporte "Horas": horas planeadas y realizadas por semana, y cuántas veces se aceptó la sugerencia. */
@Composable
fun ReporteHorasSection(estado: AdminUiState, onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Horas reportadas",
            descripcion = "Horas planeadas y realizadas por semana, y cuántos días se aceptó la sugerencia (últimas 8 semanas).",
            cargando = estado.horas is Carga.Cargando,
            onActualizar = onActualizar
        )
        ContenidoCarga(
            estado.horas,
            vacio = { it.isEmpty() },
            textoVacio = "Todavía no hay horas reportadas en las últimas 8 semanas."
        ) { semanas ->
            semanas.forEach { TarjetaHoras(it) }
        }
    }
}

@Composable
private fun TarjetaHoras(s: CloudFunctionsApi.SemanaHoras) {
    val fraccion = if (s.planeadas <= 0.0) 0.0 else s.realizadas / s.planeadas
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(semanaLegible(s.semana), style = MaterialTheme.typography.titleMedium)
                Text("${s.docentes} docente(s) · ${s.dias} día(s) reportados", style = MaterialTheme.typography.bodyMedium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(horasTexto(s.realizadas), style = MaterialTheme.typography.headlineSmall, color = EnadPrimary)
                Text("de ${horasTexto(s.planeadas)}", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Spacer(Modifier.height(10.dp))
        EnadProgress(fraccion.toFloat(), color = EnadPrimary)
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Etiqueta("Sugerencia aceptada ${porcentajeTexto(s.porcentajeSugeridas)}", EnadSuccessBg, EnadSuccessText)
            Spacer(Modifier.width(8.dp))
            Text("${s.manuales} a mano · ${s.conMotivo} con motivo", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** Reporte "Actividades" (BQ #11): actividades más elegidas por los docentes. */
@Composable
fun ReporteActividadesSection(estado: AdminUiState, onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Actividades más usadas",
            descripcion = "Actividades que más eligen los docentes en todas las instituciones.",
            cargando = estado.actividades is Carga.Cargando,
            onActualizar = onActualizar
        )
        ContenidoCarga(
            estado.actividades,
            vacio = { it.actividades.isEmpty() },
            textoVacio = "Todavía no hay actividades elegidas."
        ) { top ->
            Text("${top.total} selecciones en total", style = MaterialTheme.typography.bodyMedium)
            EnadPanel {
                top.actividades.forEachIndexed { i, a ->
                    if (i > 0) Spacer(Modifier.height(14.dp))
                    FilaActividad(i + 1, a)
                }
            }
        }
    }
}

@Composable
private fun FilaActividad(puesto: Int, a: CloudFunctionsApi.ActividadUso) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("$puesto", style = MaterialTheme.typography.titleMedium, color = if (puesto == 1) EnadPrimary else EnadInk)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(a.titulo, style = MaterialTheme.typography.labelMedium)
            val detalle = listOfNotNull(materiaLegible(a.materia), "${a.veces} ${if (a.veces == 1) "vez" else "veces"}")
            Text(detalle.joinToString(" · "), style = MaterialTheme.typography.bodyMedium)
        }
        Text(porcentajeTexto(a.porcentaje), style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    EnadProgress(((a.porcentaje ?: 0.0) / 100).toFloat(), color = if (puesto == 1) EnadPrimary else EnadInk)
}
