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
import java.time.LocalDate
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.screens.teacher.AvisoDatosGuardados
/** Flechas para moverse entre semanas; la semana en curso es el tope. */
@Composable
private fun SelectorSemana(semana: LocalDate, onCambiarSemana: (Long) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { onCambiarSemana(-1) }) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Semana anterior", tint = EnadInk)
        }
        Text(
            semanaLegible(semana.toString()),
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        val esActual = semana == lunesActual()
        IconButton(onClick = { onCambiarSemana(1) }, enabled = !esActual) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Semana siguiente",
                tint = if (esActual) EnadSoftInk.copy(alpha = 0.4f) else EnadInk
            )
        }
    }
}

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
        SelectorSemana(estado.semanaAperturas, onCambiarSemana)
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

/**
 * Reporte "Evaluaciones" (BQ #12, tipo 4): qué materia concentra más evaluaciones en cada grado.
 * La materia menos usada es la que la fundación puede priorizar con contenido.
 */
@Composable
fun ReporteEvaluacionesSection(estado: AdminUiState, onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Evaluaciones por materia",
            descripcion = "Cuántas evaluaciones tiene cada materia en cada grado. La menos usada es la que conviene reforzar con contenido.",
            cargando = estado.evaluaciones is Carga.Cargando,
            onActualizar = onActualizar
        )
        val guardadoEn = estado.evaluacionesGuardadoEn
        if (guardadoEn != null && estado.evaluaciones is Carga.Lista) {
            AvisoDatosGuardados(guardadoEn, onActualizar = onActualizar, titulo = "Reporte guardado")
        }
        ContenidoCarga(
            estado.evaluaciones,
            vacio = { it.total == 0 },
            textoVacio = "Todavía no hay evaluaciones registradas."
        ) { r ->
            TarjetaEvaluaciones("Todos los grados", r.general)
            r.grados.forEach { TarjetaEvaluaciones("Grado ${it.grado}", it) }
        }
    }
}

@Composable
private fun TarjetaEvaluaciones(titulo: String, r: CloudFunctionsApi.ResumenEvaluaciones) {
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    if (r.total == 0) "Sin evaluaciones" else "${r.total} ${if (r.total == 1) "evaluación" else "evaluaciones"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (r.total > 0 && r.menosUsada == null) Etiqueta("Mismo número", EnadSuccessBg, EnadSuccessText)
        }
        if (r.total > 0) {
            FilaMateria("matematicas", r.matematicas, r.porcentajeMatematicas, r.menosUsada == "matematicas")
            FilaMateria("lectura", r.lectura, r.porcentajeLectura, r.menosUsada == "lectura")
        }
    }
}

@Composable
private fun FilaMateria(materia: String, cantidad: Int, porcentaje: Double?, menosUsada: Boolean) {
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(materiaLegible(materia).orEmpty(), style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
        if (menosUsada) {
            Etiqueta("Menos usada", EnadPendienteBg, EnadPendienteText)
            Spacer(Modifier.width(8.dp))
        }
        Text("$cantidad · ${porcentajeTexto(porcentaje)}", style = MaterialTheme.typography.labelLarge)
    }
    Spacer(Modifier.height(6.dp))
    EnadProgress(((porcentaje ?: 0.0) / 100).toFloat(), color = if (menosUsada) EnadPrimary else EnadInk)
}

/**
 * Reporte "Conflictos" (BQ Sprint 3 tipo 1): marcas de asistencia que no se aplicaron porque otro
 * celular ya había registrado otro valor para el mismo estudiante y día.
 */
@Composable
fun ReporteConflictosSection(estado: AdminUiState, onCambiarSemana: (Long) -> Unit, onActualizar: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Conflictos de asistencia",
            cargando = estado.conflictos is Carga.Cargando,
            onActualizar = onActualizar
        )
        SelectorSemana(estado.semanaConflictos, onCambiarSemana)
        ContenidoCarga(
            estado.conflictos,
            vacio = { it.total == 0 },
            textoVacio = "Esta semana no se enviaron marcas de asistencia."
        ) { r ->
            EnadPanel {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Marcas reemplazadas", style = MaterialTheme.typography.titleMedium)
                        Text("${r.conflictos} de ${r.total} marcas enviadas", style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(porcentajeTexto(r.porcentaje), style = MaterialTheme.typography.headlineSmall, color = EnadPrimary)
                }
                Spacer(Modifier.height(10.dp))
                EnadProgress(((r.porcentaje ?: 0.0) / 100).toFloat(), color = EnadPrimary)
                if (r.provisional) {
                    Spacer(Modifier.height(10.dp))
                    PildoraProvisional()
                }
            }
            GrupoConflictos("Por institución", r.porInstitucion)
            GrupoConflictos("Por conectividad al marcar", r.porConectividad.map { it.copy(etiqueta = conectividadLegible(it.etiqueta)) })
            GrupoConflictos("Por plataforma", r.porPlataforma.map { it.copy(etiqueta = plataformaLegible(it.etiqueta)) })
        }
    }
}

@Composable
private fun GrupoConflictos(titulo: String, filas: List<CloudFunctionsApi.FilaConflictos>) {
    if (filas.isEmpty()) return
    EnadPanel {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        filas.forEach { fila ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(fila.etiqueta, style = MaterialTheme.typography.labelMedium)
                    Text("${fila.conflictos} de ${fila.total} marcas", style = MaterialTheme.typography.bodyMedium)
                }
                Text(porcentajeTexto(fila.porcentaje), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.height(6.dp))
            EnadProgress(((fila.porcentaje ?: 0.0) / 100).toFloat(), color = EnadPrimary)
        }
    }
}

private fun conectividadLegible(valor: String): String = when (valor) {
    "online" -> "Con conexión"
    "offline" -> "Sin conexión"
    else -> "Sin dato"
}

private fun plataformaLegible(valor: String): String = when (valor) {
    "kotlin" -> "Kotlin"
    "flutter" -> "Flutter"
    else -> "Sin dato"
}
