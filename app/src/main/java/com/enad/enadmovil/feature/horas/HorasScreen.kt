package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.enadFieldColors
import com.enad.enadmovil.ui.screens.teacher.Desplegable
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.round

private val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val etiquetasDia = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/** "Reportar horas" de Flutter: semana, día y las horas programadas y realizadas de ese día. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HorasScreen(
    nombreGrupo: String,
    onEnviarReporte: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HorasViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val hoy = remember { LocalDate.now() }
    var mostrarFecha by remember { mutableStateOf(false) }
    var confirmarReemplazo by remember { mutableStateOf(false) }
    val registro = estado.registro
    val inicioSemana = estado.fecha.minusDays((estado.fecha.dayOfWeek.value - 1).toLong())

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 32.dp)
    ) {
        EnadHeading("Reportar horas")
        Spacer(Modifier.height(24.dp))

        EnadPanel {
            Text(
                "${inicioSemana.format(formatoFecha)} al ${inicioSemana.plusDays(6).format(formatoFecha)}",
                style = typography.titleMedium
            )
            TextButton(onClick = { mostrarFecha = true }, enabled = !estado.guardando, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Cambiar semana", color = EnadInk)
            }
            SelectorDiaSemana(
                seleccionado = estado.fecha,
                ultimo = hoy,
                habilitado = !estado.guardando,
                onSeleccionar = viewModel::seleccionarDia
            )
        }
        Spacer(Modifier.height(16.dp))

        EnadPanel {
            if (registro == null) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EnadPrimary, trackColor = EnadLine)
            } else {
                CampoHoras("Programadas", registro.textoPlaneadas, !estado.guardando, viewModel::cambiarPlaneadas)
                Spacer(Modifier.height(20.dp))
                CampoHoras(
                    "Realizadas",
                    registro.texto,
                    !estado.guardando,
                    viewModel::cambiarHoras,
                    ayuda = if (registro.sugerido) "Sugerido, ajústalo" else null
                )
                if (registro.pideMotivo) {
                    Spacer(Modifier.height(20.dp))
                    Desplegable(
                        etiqueta = "Motivo",
                        valor = registro.categoria.orEmpty(),
                        opciones = MOTIVOS_HORAS.map { it to it },
                        onSeleccionar = viewModel::cambiarCategoria
                    )
                    if (registro.categoria != null) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = registro.detalle,
                            onValueChange = viewModel::cambiarDetalle,
                            enabled = !estado.guardando,
                            label = { Text(if (registro.categoria == "Otro") "Explica el motivo" else "Detalle (opcional)") },
                            minLines = 2,
                            maxLines = 4,
                            shape = ControlShape,
                            colors = enadFieldColors(),
                            textStyle = typography.bodyLarge,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    registro.errorMotivo?.takeIf { registro.categoria != null }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = typography.bodyMedium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                EnadPrimaryButton(
                    text = "Guardar",
                    onClick = { if (registro.existe) confirmarReemplazo = true else viewModel.guardar() },
                    enabled = registro.puedeGuardar && !estado.guardando
                )
                if (estado.guardando) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EnadPrimary, trackColor = EnadLine)
                }
                estado.mensaje?.let {
                    Spacer(Modifier.height(12.dp))
                    Text(it, style = typography.bodyMedium, color = EnadInk)
                }
            }
        }
    }

    if (mostrarFecha) {
        // Como en Flutter: cualquier día del año en curso, hasta hoy.
        val picker = rememberDatePickerState(
            initialSelectedDateMillis = estado.fecha.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                    val dia = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
                    return !dia.isAfter(hoy) && dia.year == hoy.year
                }
                override fun isSelectableYear(year: Int): Boolean = year == hoy.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { ms ->
                        viewModel.seleccionarDia(Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                    mostrarFecha = false
                }) { Text("Aceptar", color = EnadInk) }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar", color = EnadInk) } }
        ) { DatePicker(state = picker) }
    }

    if (confirmarReemplazo) {
        AlertDialog(
            onDismissRequest = { confirmarReemplazo = false },
            containerColor = EnadSurface,
            title = { Text("¿Actualizar las horas de este día?") },
            text = { Text("Guardar reemplazará el reporte anterior.") },
            dismissButton = { TextButton(onClick = { confirmarReemplazo = false }) { Text("Revisar", color = EnadInk) } },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarReemplazo = false
                        viewModel.guardar()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary)
                ) { Text("Guardar") }
            }
        )
    }
}

@Composable
private fun CampoHoras(etiqueta: String, valor: String, habilitado: Boolean, onCambio: (String) -> Unit, ayuda: String? = null) {
    OutlinedTextField(
        value = valor,
        onValueChange = onCambio,
        enabled = habilitado,
        label = { Text(etiqueta) },
        suffix = { Text("h") },
        supportingText = ayuda?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        singleLine = true,
        shape = ControlShape,
        colors = enadFieldColors(),
        textStyle = typography.bodyLarge,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Los siete días de la semana elegida; el día activo va en negro y centrado, como en Flutter. */
@Composable
private fun SelectorDiaSemana(
    seleccionado: LocalDate,
    ultimo: LocalDate,
    habilitado: Boolean,
    onSeleccionar: (LocalDate) -> Unit
) {
    val inicio = seleccionado.minusDays((seleccionado.dayOfWeek.value - 1).toLong())
    val scroll = rememberScrollState()
    val anchoDia = 72.dp
    val anchoPx = with(LocalDensity.current) { anchoDia.roundToPx() }
    LaunchedEffect(seleccionado, anchoPx) {
        scroll.animateScrollTo((seleccionado.dayOfWeek.value - 1) * anchoPx)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val margen = ((maxWidth - anchoDia) / 2).coerceAtLeast(0.dp)
        Row(
            modifier = Modifier
                .horizontalScroll(scroll)
                .padding(horizontal = margen)
        ) {
            for (i in 0 until 7) {
                val dia = inicio.plusDays(i.toLong())
                val activo = dia == seleccionado
                val permitido = habilitado && !dia.isAfter(ultimo) && dia.year == ultimo.year
                OutlinedButton(
                    onClick = { onSeleccionar(dia) },
                    enabled = permitido,
                    shape = ControlShape,
                    border = BorderStroke(1.dp, EnadLine),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (activo) EnadInk else EnadSurface,
                        contentColor = if (activo) EnadSurface else EnadInk,
                        disabledContainerColor = if (activo) EnadInk else EnadSurface,
                        disabledContentColor = if (activo) EnadSurface else EnadSoftInk
                    ),
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .width(anchoDia - 8.dp)
                        .heightIn(min = 60.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(etiquetasDia[i], style = typography.labelSmall)
                        Text("${dia.dayOfMonth}", style = typography.labelLarge)
                    }
                }
            }
        }
    }
}

fun formatearHoras(horas: Double): String {
    val redondeado = round(horas * 10) / 10
    return if (redondeado == redondeado.toInt().toDouble()) {
        redondeado.toInt().toString()
    } else {
        redondeado.toString()
    }
}
