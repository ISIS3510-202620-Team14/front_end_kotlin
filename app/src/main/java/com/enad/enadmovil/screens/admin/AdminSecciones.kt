package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadNeutral
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk

// ---------- Resumen ----------

/** Pestaña "Resumen": un indicador por reporte; al tocarlo abre ese reporte. */
@Composable
fun AdminResumenSection(estado: AdminUiState, onAbrirReporte: (Int) -> Unit, onAbrirInstituciones: () -> Unit) {
    val aperturas = estado.aperturas.datos
    val horas = estado.horas.datos?.firstOrNull()
    val top = estado.actividades.datos?.actividades?.firstOrNull()
    val instituciones = estado.instituciones.datos

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Indicador(
                etiqueta = "Instituciones",
                valor = if (estado.busqueda.isBlank()) instituciones?.size?.toString() else null,
                detalle = "activas",
                carga = estado.instituciones,
                onClick = onAbrirInstituciones,
                modifier = Modifier.weight(1f)
            )
            Indicador(
                etiqueta = "Uso semanal",
                valor = aperturas?.let { if (it.docentes == 0) "—" else porcentajeTexto(Math.round(it.docentesMasDeUna * 1000.0 / it.docentes) / 10.0) },
                detalle = aperturas?.let { "${it.docentesMasDeUna} de ${it.docentes} docentes" } ?: "",
                carga = estado.aperturas,
                onClick = { onAbrirReporte(REPORTE_APERTURAS) },
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Indicador(
                etiqueta = "Horas",
                valor = horas?.let { horasTexto(it.realizadas) } ?: if (estado.horas.datos != null) "—" else null,
                detalle = horas?.let { "de ${horasTexto(it.planeadas)} planeadas" } ?: "sin reportes",
                carga = estado.horas,
                onClick = { onAbrirReporte(REPORTE_HORAS) },
                modifier = Modifier.weight(1f)
            )
            Indicador(
                etiqueta = "Actividad top",
                valor = top?.let { porcentajeTexto(it.porcentaje) } ?: if (estado.actividades.datos != null) "—" else null,
                detalle = top?.titulo ?: "sin datos",
                carga = estado.actividades,
                onClick = { onAbrirReporte(REPORTE_ACTIVIDADES) },
                modifier = Modifier.weight(1f)
            )
        }
        EnadPanel(onClick = { onAbrirReporte(REPORTE_SINCRONIZACION) }) {
            Text("Sincronización y clasificación", style = MaterialTheme.typography.titleMedium)
            Text("Revisa las sesiones que no llegaron a tiempo y cuánto tarda cada docente en clasificar.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Indicador(
    etiqueta: String,
    valor: String?,
    detalle: String,
    carga: Carga<*>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    EnadPanel(modifier = modifier, onClick = onClick) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
        when {
            carga is Carga.Cargando -> CircularProgressIndicator(color = EnadInk, strokeWidth = 2.dp, modifier = Modifier.height(28.dp).width(28.dp))
            carga is Carga.Fallo -> Text("Sin datos", style = MaterialTheme.typography.titleMedium, color = EnadSoftInk)
            else -> Text(valor ?: "—", style = MaterialTheme.typography.headlineSmall, color = EnadPrimary)
        }
        Spacer(Modifier.height(4.dp))
        Text(detalle, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

// ---------- Instituciones ----------

/** Pestaña "Instituciones": buscador y lista; al tocar una institución se ven sus docentes. */
@Composable
fun AdminInstitucionesSection(estado: AdminUiState, onBuscar: (String) -> Unit, onAlternarDocentes: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EnadTextField(
            value = estado.busqueda,
            onValueChange = onBuscar,
            label = "Buscar por nombre o municipio",
            floatingLabel = false,
            trailingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = EnadSoftInk) }
        )
        ContenidoCarga(
            estado.instituciones,
            vacio = { it.isEmpty() },
            textoVacio = if (estado.busqueda.isBlank()) "Todavía no hay instituciones registradas."
            else "Ninguna institución coincide con «${estado.busqueda}»."
        ) { lista ->
            Text("${lista.size} institución(es)", style = MaterialTheme.typography.bodyMedium)
            lista.forEach { escuela ->
                TarjetaInstitucionAdmin(escuela, estado.docentes[escuela.id]) { onAlternarDocentes(escuela.id) }
            }
        }
    }
}

@Composable
private fun TarjetaInstitucionAdmin(
    escuela: CloudFunctionsApi.EscuelaAdmin,
    docentes: Carga<List<CloudFunctionsApi.DocenteAdmin>>?,
    onClick: () -> Unit
) {
    EnadPanel(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(escuela.nombre, style = MaterialTheme.typography.titleMedium)
                val lugar = listOfNotNull(escuela.municipio, escuela.departamento).joinToString(", ")
                if (lugar.isNotEmpty()) Text(lugar, style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                if (docentes != null) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (docentes != null) "Ocultar docentes" else "Ver docentes",
                tint = EnadSoftInk
            )
        }
        val etiquetas = listOfNotNull(
            escuela.zona?.replaceFirstChar { it.uppercase() },
            escuela.etapa?.let { "Etapa $it" },
            "${escuela.sedes.size} sede(s)"
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            etiquetas.forEach { Etiqueta(it, EnadNeutral, EnadInk) }
        }
        if (docentes != null) {
            Spacer(Modifier.height(12.dp))
            Text("Docentes", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(6.dp))
            ContenidoCarga(docentes, vacio = { it.isEmpty() }, textoVacio = "Esta institución todavía no tiene docentes.") { lista ->
                lista.forEach { d ->
                    Column(Modifier.padding(vertical = 6.dp)) {
                        Text(d.nombre ?: "Docente sin nombre", style = MaterialTheme.typography.bodyLarge)
                        d.correo?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    }
                }
            }
        }
    }
}
