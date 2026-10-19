package com.enad.enadmovil.feature.misdatos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SwitchAccount
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.feature.horas.formatearHoras
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.screens.teacher.MATERIA_LECTURA
import com.enad.enadmovil.ui.screens.teacher.MATERIA_MATEMATICAS
import com.enad.enadmovil.ui.theme.EnadAmberBg
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadNeutral
import com.enad.enadmovil.ui.theme.EnadPending
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSuccessBg
import com.enad.enadmovil.ui.theme.EnadSuccessText
import kotlin.math.roundToInt

@Composable
fun MisDatosScreen(
    onEnviar: () -> Unit = {},
    modifier: Modifier = Modifier,
    onCambiarUsuario: () -> Unit = {},
    onAbrirGrupo: (Grupo) -> Unit = {},
    onAbrirAsistencia: (() -> Unit)? = null,
    onGestionarEstudiantes: (() -> Unit)? = null,
    viewModel: MisDatosViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        EnadHeading("Mis datos", modifier = Modifier.padding(bottom = 8.dp))

        EnadPanel {
            EnadTextField(
                value = estado.nombre,
                onValueChange = viewModel::cambiarNombre,
                label = "Nombre",
                enabled = !estado.guardandoNombre
            )
            Spacer(Modifier.height(16.dp))
            Etiqueta("Correo")
            Spacer(Modifier.height(4.dp))
            Text(estado.correo, style = typography.bodyLarge, color = EnadInk)
            Spacer(Modifier.height(16.dp))
            Etiqueta("Instituciones")
            Spacer(Modifier.height(8.dp))
            estado.instituciones.forEach { i ->
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text(i.nombre, style = typography.bodyLarge, color = EnadInk)
                    Text("Municipio: ${i.municipio ?: "No informado"}", style = typography.bodyMedium)
                    i.sedes.forEach { Text("Sede: $it", style = typography.bodyMedium) }
                }
            }
            Spacer(Modifier.height(8.dp))
            EnadSecondaryButton(text = "Guardar nombre", onClick = viewModel::guardarNombre, enabled = !estado.guardandoNombre)
            estado.mensajeNombre?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = typography.bodyMedium)
            }
        }

        PanelDesplegable("Mis grupos (${estado.grupos.size})") {
            if (estado.grupos.isEmpty()) {
                Text("Todavía no tienes grupos.", style = typography.bodyMedium)
            }
            estado.grupos.forEach { g ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAbrirGrupo(g) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(g.nombre, style = typography.bodyLarge, color = EnadInk)
                        val materia = if (g.area == AreaMateria.MATEMATICAS) "Matemáticas" else "Lectura"
                        Text("$materia · ${g.ninos.size} estudiantes", style = typography.bodyMedium)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = EnadSoftInk)
                }
            }
        }

        if (onGestionarEstudiantes != null) {
            EnadSecondaryButton(text = "Gestionar estudiantes", onClick = onGestionarEstudiantes, icon = Icons.Outlined.PersonAddAlt)
        }

        PanelDesplegable("Reportes de horas") {
            Text("Últimos 30 días", style = typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            if (estado.reportes.isEmpty()) {
                Text("Sin reportes de horas en este periodo.", style = typography.bodyMedium)
            }
            estado.reportes.forEachIndexed { i, r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(r.fecha, style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = EnadInk, modifier = Modifier.weight(1f))
                    EstadoRegistro(if (MisDatosViewModel.enviado(r)) "Enviado" else "Pendiente")
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${formatearHoras(r.horasRealizadas)} h realizadas de ${formatearHoras(r.horasPlaneadas)} h programadas",
                    style = typography.bodyMedium
                )
                if (i != estado.reportes.lastIndex) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = EnadLine)
            }
        }

        PanelDesplegable("Mis indicadores", onAbrir = { if (!estado.indicadores.cargados) viewModel.cargarIndicadores() }) {
            val ind = estado.indicadores
            if (ind.cargando) LinearProgressIndicator(Modifier.fillMaxWidth(), color = EnadPrimary, trackColor = EnadLine)
            if (ind.error != null) {
                Text(ind.error, style = typography.bodyMedium)
                TextButton(onClick = viewModel::cargarIndicadores) { Text("Reintentar", color = EnadInk) }
            } else if (ind.cargados) {
                Indicador(
                    "Asistencia histórica registrada",
                    if (ind.asistenciaRegistrada == 0) "Sin sesiones registradas"
                    else "${(100.0 * ind.asistenciaPresente / ind.asistenciaRegistrada).roundToInt()}% · " +
                        "${ind.asistenciaPresente} de ${ind.asistenciaRegistrada} registros"
                )
            }
            val programadas = estado.reportes.sumOf { it.horasPlaneadas }
            val realizadas = estado.reportes.sumOf { it.horasRealizadas }
            Indicador(
                "Cumplimiento de horas · 30 días",
                if (programadas == 0.0) "Sin horas programadas registradas"
                else "${(100 * realizadas / programadas).roundToInt()}% · ${formatearHoras(realizadas)} de ${formatearHoras(programadas)} h reportadas"
            )
            if (ind.cargados) {
                Spacer(Modifier.height(16.dp))
                Text("Niveles actuales", style = typography.titleMedium)
                listOf(MATERIA_LECTURA, MATERIA_MATEMATICAS).forEach { materia ->
                    Spacer(Modifier.height(16.dp))
                    Text(materia.titulo, style = typography.labelLarge, color = EnadInk)
                    val conteo = ind.niveles[materia.clave].orEmpty()
                    if (ind.activos == 0) Text("Sin estudiantes activos", style = typography.bodyMedium)
                    else materia.niveles.forEach { nivel ->
                        val n = conteo[nivel.nombre] ?: 0
                        Column(Modifier.padding(vertical = 8.dp)) {
                            Text("${nivel.nombre} · $n", style = typography.bodyMedium)
                            Spacer(Modifier.height(6.dp))
                            EnadProgress(fraccion = n.toFloat() / ind.activos, color = nivel.color)
                        }
                    }
                }
            }
        }

        EnadPanel {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Asistencia por enviar", style = typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), color = EnadInk, modifier = Modifier.weight(1f))
                IconButton(onClick = viewModel::recargarPendientes) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar pendientes", tint = EnadInk)
                }
            }
            val pendientes = estado.pendientesAsistencia
            when {
                pendientes == null -> LinearProgressIndicator(Modifier.fillMaxWidth(), color = EnadPrimary, trackColor = EnadLine)
                pendientes.isEmpty() -> Text("Todo al día.", style = typography.bodyMedium)
                else -> pendientes.forEach { (fecha, n) -> Text("$fecha: $n pendientes", style = typography.bodyMedium) }
            }
            if (onAbrirAsistencia != null) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onAbrirAsistencia) { Text("Abrir Asistencia", color = EnadPrimary) }
            }
        }

        EnadPanel {
            AccionSesion(Icons.Outlined.SwitchAccount, "Cambiar usuario", onCambiarUsuario)
            AccionSesion(Icons.AutoMirrored.Outlined.Logout, "Cerrar sesión") {
                viewModel.cerrarSesion()
                onCambiarUsuario()
            }
        }
    }
}

@Composable
private fun Etiqueta(texto: String) {
    Text(texto, style = typography.labelMedium, color = EnadSoftInk)
}

@Composable
private fun Indicador(titulo: String, valor: String) {
    Column(Modifier.padding(vertical = 8.dp)) {
        Text(titulo, style = typography.bodyLarge, color = EnadInk)
        Text(valor, style = typography.bodyMedium)
    }
}

@Composable
private fun EstadoRegistro(etiqueta: String) {
    val enviado = etiqueta == "Enviado"
    Surface(color = if (enviado) EnadSuccessBg else EnadAmberBg, shape = RoundedCornerShape(24.dp)) {
        Text(
            etiqueta,
            style = typography.labelMedium,
            color = if (enviado) EnadSuccessText else EnadPending,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun AccionSesion(icon: ImageVector, texto: String, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(texto, color = EnadInk)
    }
}

/** Panel que se abre y cierra, como los ExpansionTile de profile_view. */
@Composable
private fun PanelDesplegable(titulo: String, onAbrir: () -> Unit = {}, contenido: @Composable ColumnScope.() -> Unit) {
    var abierto by rememberSaveable(titulo.substringBefore(" (")) { mutableStateOf(false) }
    EnadPanel {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    abierto = !abierto
                    if (abierto) onAbrir()
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(titulo, style = typography.titleMedium, modifier = Modifier.weight(1f))
            Icon(if (abierto) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = EnadInk)
        }
        AnimatedVisibility(visible = abierto) {
            Column(Modifier.padding(top = 12.dp), content = contenido)
        }
    }
}
