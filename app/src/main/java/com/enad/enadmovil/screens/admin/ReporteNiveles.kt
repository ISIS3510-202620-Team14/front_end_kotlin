package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.domain.model.DatosGuardados
import com.enad.enadmovil.domain.model.EstadoNiveles
import com.enad.enadmovil.domain.model.FilaNiveles
import com.enad.enadmovil.domain.model.NivelesInstitucion
import com.enad.enadmovil.domain.model.sinActualizar
import com.enad.enadmovil.screens.teacher.AvisoDatosGuardados
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.screens.teacher.MATERIA_LECTURA
import com.enad.enadmovil.ui.screens.teacher.MATERIA_MATEMATICAS
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLevel1
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSuccessBg
import com.enad.enadmovil.ui.theme.EnadSuccessText

private val MATERIAS = listOf("lectura" to "Lectura", "matematicas" to "Matemáticas")
private val GRADOS = listOf(0 to "Todos", 3 to "3°", 4 to "4°", 5 to "5°")

/** Sprint 4, vista nueva (BQ de niveles): niveles ENAd de cada institución, Principiante primero. */
@Composable
fun ReporteNivelesSection(viewModel: NivelesViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val niveles = (if (estado.materia == "matematicas") MATERIA_MATEMATICAS else MATERIA_LECTURA).niveles
    val colores = niveles.map { it.color }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Niveles por institución",
            descripcion = "Estudiantes evaluados que siguen en Principiante, por institución.",
            cargando = estado.estado == EstadoNiveles.ACTUALIZANDO,
            onActualizar = { viewModel.actualizar(forzar = true) }
        )
        EstadoNivelesAviso(estado, onActualizar = { viewModel.actualizar() })
        AdminChips(MATERIAS.map { it.second }, MATERIAS.indexOfFirst { it.first == estado.materia }.coerceAtLeast(0), onSeleccionar = { viewModel.elegir(materia = MATERIAS[it].first) })
        AdminChips(GRADOS.map { it.second }, GRADOS.indexOfFirst { it.first == estado.grado }.coerceAtLeast(0), onSeleccionar = { viewModel.elegir(grado = GRADOS[it].first) })

        when {
            estado.estado == EstadoNiveles.CARGANDO && estado.filas.isEmpty() ->
                Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EnadInk)
                }
            estado.filas.isEmpty() && estado.estado != EstadoNiveles.ACTUALIZANDO -> SinNivelesGuardados()
            estado.filas.isNotEmpty() -> ResumenNivelesPanel(estado, niveles.map { it.nombre }, colores)
        }
        estado.filas.forEach { fila ->
            FilaInstitucion(fila, colores, sinActualizar(fila, estado.ultimoIntento))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EstadoNivelesAviso(estado: NivelesUiState, onActualizar: () -> Unit) {
    val guardadoEn = estado.resumen.guardadoEn
    when (estado.estado) {
        EstadoNiveles.CARGANDO -> Unit
        EstadoNiveles.ACTUALIZANDO -> Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = EnadInk, strokeWidth = 2.dp, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(8.dp))
            Text("Actualizando… ${estado.hechas} de ${estado.total} instituciones", style = MaterialTheme.typography.bodyMedium)
        }
        EstadoNiveles.SIN_CONEXION, EstadoNiveles.VACIO -> when {
            estado.volvioConexion -> Row(
                modifier = Modifier.fillMaxWidth()
                    .background(EnadSuccessBg, RoundedCornerShape(12.dp))
                    .padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Volvió la conexión. Toca Actualizar para traer los niveles nuevos.",
                    style = MaterialTheme.typography.bodySmall,
                    color = EnadSuccessText,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onActualizar) { Text("Actualizar", color = EnadSuccessText) }
            }
            estado.estado == EstadoNiveles.SIN_CONEXION && guardadoEn != null ->
                AvisoDatosGuardados(guardadoEn, onActualizar, titulo = "Niveles guardados")
        }
        EstadoNiveles.EN_LINEA -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val cuando = estado.ultimoIntento?.let { " ${DatosGuardados.cuando(it)}" } ?: ""
            Etiqueta("Actualizado$cuando", EnadSuccessBg, EnadSuccessText)
            if (estado.fallidas > 0) {
                Etiqueta("${estado.fallidas} sin actualizar · se muestran sus datos guardados", EnadPendienteBg, EnadPendienteText)
            }
        }
    }
}

/** Respaldo genérico: nunca una pantalla en blanco. */
@Composable
private fun SinNivelesGuardados() {
    EnadPanel {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, tint = EnadSoftInk, modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(8.dp))
            Text(
                "Aún no hay niveles guardados en este celular. Conéctate a internet y toca Actualizar para " +
                    "descargarlos; después podrás verlos sin conexión.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResumenNivelesPanel(estado: NivelesUiState, nombres: List<String>, colores: List<Color>) {
    val resumen = estado.resumen
    val peor = resumen.necesitaMasApoyo
    EnadPanel {
        Text(porcentaje(resumen.porcentajePrincipiante), style = MaterialTheme.typography.headlineLarge, color = EnadLevel1)
        Text("de ${resumen.evaluados} estudiantes evaluados sigue en Principiante", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            if (peor == null) "Todavía no hay evaluaciones en este filtro."
            else "Necesita más apoyo: ${peor.nombre} (${porcentaje(peor.porcentajePrincipiante)})",
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            nombres.forEachIndexed { i, nombre ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).background(colores[i]))
                    Spacer(Modifier.width(4.dp))
                    Text(nombre, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

/** Una institución: barra apilada de los 6 niveles y su porcentaje en Principiante. */
@Composable
private fun FilaInstitucion(fila: FilaNiveles, colores: List<Color>, desactualizada: Boolean) {
    val porcentaje = fila.porcentajePrincipiante
    val alerta = porcentaje != null && porcentaje >= 50
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                fila.nombre,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Etiqueta(
                if (porcentaje == null) "Sin evaluar" else "${Math.round(porcentaje)} % Principiante",
                if (alerta) EnadLevel1 else EnadLine,
                if (alerta) Color.White else EnadInk
            )
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(6.dp)).background(EnadLine)) {
            for (nivel in 1 until NivelesInstitucion.NIVELES) {
                val estudiantes = fila.niveles[nivel]
                if (estudiantes > 0) Box(Modifier.weight(estudiantes.toFloat()).fillMaxHeight().background(colores[nivel - 1]))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            "${fila.evaluados} evaluados · ${fila.pendientes} sin evaluar" + if (desactualizada) " · sin actualizar" else "",
            style = MaterialTheme.typography.bodySmall,
            color = if (desactualizada) EnadPendienteText else EnadSoftInk
        )
    }
}

private fun porcentaje(valor: Double?): String = if (valor == null) "—" else "${Math.round(valor)} %"
