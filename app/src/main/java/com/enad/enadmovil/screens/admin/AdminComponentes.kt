package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Piezas del portal admin, iguales a las de admin_view en Flutter.

internal val LOCALE_CO: Locale = Locale.forLanguageTag("es-CO")
private val FORMATO_SEMANA = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_CO)

/** Fila de píldoras desplazable: la elegida en negro, como el selector de curso. */
@Composable
fun AdminChips(opciones: List<String>, seleccionada: Int, onSeleccionar: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEachIndexed { i, opcion ->
            val activa = i == seleccionada
            Box(
                modifier = Modifier
                    .heightIn(min = 40.dp)
                    .background(if (activa) EnadInk else EnadSurface, RoundedCornerShape(50))
                    .border(1.dp, if (activa) EnadInk else EnadSoftInk, RoundedCornerShape(50))
                    .clickable { onSeleccionar(i) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(opcion, style = MaterialTheme.typography.labelMedium, color = if (activa) EnadSurface else EnadInk)
            }
        }
    }
}

/** Título y descripción de un reporte, con el botón de actualizar. */
@Composable
fun EncabezadoReporte(titulo: String, descripcion: String = "", cargando: Boolean, onActualizar: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleLarge)
            if (descripcion.isNotBlank()) Text(descripcion, style = MaterialTheme.typography.bodyMedium)
        }
        IconButton(onClick = onActualizar, enabled = !cargando) {
            Icon(Icons.Filled.Refresh, contentDescription = "Actualizar", tint = EnadInk)
        }
    }
}

/** Muestra el spinner, el error o el contenido según el estado de la consulta. */
@Composable
fun <T> ContenidoCarga(
    carga: Carga<T>,
    vacio: (T) -> Boolean,
    textoVacio: String,
    contenido: @Composable (T) -> Unit
) {
    when (carga) {
        Carga.Cargando -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EnadInk)
        }
        is Carga.Fallo -> MensajePanel(carga.mensaje)
        is Carga.Lista -> if (vacio(carga.datos)) MensajePanel(textoVacio) else contenido(carga.datos)
    }
}

@Composable
fun MensajePanel(texto: String) {
    EnadPanel { Text(texto, style = MaterialTheme.typography.bodyMedium) }
}

/** Píldora ámbar: el dato de la semana en curso todavía puede cambiar. */
@Composable
fun PildoraProvisional(texto: String = "Semana en curso: el dato todavía puede cambiar") {
    Etiqueta(texto, EnadPendienteBg, EnadPendienteText)
}

@Composable
fun Etiqueta(texto: String, fondo: Color, color: Color) {
    Text(
        text = texto,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(fondo, RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

internal fun semanaLegible(lunes: String): String =
    runCatching { "Semana del " + LocalDate.parse(lunes).format(FORMATO_SEMANA) }.getOrDefault(lunes)

internal fun porcentajeTexto(porcentaje: Double?): String = when {
    porcentaje == null -> "—"
    porcentaje % 1.0 == 0.0 -> "${porcentaje.toLong()} %"
    else -> String.format(LOCALE_CO, "%.1f %%", porcentaje)
}

internal fun horasTexto(horas: Double): String =
    if (horas % 1.0 == 0.0) "${horas.toLong()} h" else String.format(LOCALE_CO, "%.1f h", horas)

internal fun materiaLegible(materia: String?): String? = when (materia) {
    null -> null
    "lectura" -> "Lectura"
    "matematicas" -> "Matemáticas"
    else -> materia.replaceFirstChar { it.uppercase() }
}
