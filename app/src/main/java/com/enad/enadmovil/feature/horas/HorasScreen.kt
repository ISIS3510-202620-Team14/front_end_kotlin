package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.core.ui.theme.amberBackgroundContainerLight
import com.enad.enadmovil.core.ui.theme.pendingLight
import com.enad.enadmovil.core.ui.theme.pillBackgroundLight
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.round

private val meses = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)
@Composable
fun HorasScreen(nombreGrupo: String,onEnviarReporte: () -> Unit, modifier: Modifier = Modifier) {
    val hoy = remember { LocalDate.now() }
    var anio by remember { mutableIntStateOf(hoy.year) }
    var mesIndex by remember { mutableIntStateOf(hoy.monthValue - 1) }
    var semana by remember { mutableIntStateOf(1) }
    var horasProgramadas by remember { mutableStateOf("6") }
    var horasRealizadas by remember { mutableStateOf("5") }
    var quedaronPendientes by remember { mutableStateOf(false) }
    var motivo by remember { mutableStateOf("") }

    val anios = remember(anio) { listOf(anio - 1, anio, anio + 1) }
    val diasEnMes = remember(anio, mesIndex) { YearMonth.of(anio, mesIndex + 1).lengthOfMonth() }
    val totalSemanas = remember(diasEnMes) { (diasEnMes + 6) / 7 }
    val opcionesSemana = remember(totalSemanas, diasEnMes) {
        (1..totalSemanas).map { s ->
            val inicio = (s - 1) * 7 + 1
            val fin = if (s * 7 > diasEnMes) diasEnMes else s * 7
            "Semana $s · $inicio–$fin"
        }
    }
    val diferencia = ((horasProgramadas.toDoubleOrNull() ?: 0.0) - (horasRealizadas.toDoubleOrNull() ?: 0.0))
        .let { if (it > 0) it else 0.0 }

    LazyColumn(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Reporte Interno ENAd", style = typography.titleLarge)
        }
        item {
            Text(text = nombreGrupo, style = typography.bodyLarge, color = colorScheme.onSurfaceVariant)
        }
        item {
            Text(
                text = "Planea y registra tu semana. Los cambios duran durante esta sesión demo.",
                style = typography.bodyLarge
            )
        }
        item {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        DropdownCampo(
                            label = "Año",
                            valor = anio.toString(),
                            opciones = anios.map { it.toString() },
                            onSeleccionar = { index -> anio = anios[index] },
                            modifier = Modifier.weight(1f)
                        )
                        DropdownCampo(
                            label = "Mes",
                            valor = meses[mesIndex],
                            opciones = meses,
                            onSeleccionar = { index ->
                                mesIndex = index
                                semana = 1
                            },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    DropdownCampo(
                        label = "Semana",
                        valor = opcionesSemana.getOrElse(semana - 1) { opcionesSemana.first() },
                        opciones = opcionesSemana,
                        onSeleccionar = { index -> semana = index + 1 }
                    )
                    OutlinedTextField(
                        value = horasProgramadas,
                        onValueChange = { horasProgramadas = it },
                        label = { Text("Horas programadas en la semana") },
                        suffix = { Text("h") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = horasRealizadas,
                        onValueChange = { horasRealizadas = it },
                        label = { Text("Horas realizadas en la semana") },
                        suffix = { Text("h") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "¿Quedaron horas pendientes de semanas anteriores?",
                        style = typography.labelLarge
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(true, false).forEach { opcion ->
                            val seleccionado = quedaronPendientes == opcion
                            FilterChip(
                                selected = seleccionado,
                                onClick = { quedaronPendientes = opcion },
                                label = { Text(if (opcion) "Sí" else "No") },
                                leadingIcon = if (seleccionado) {
                                    { Icon(Icons.Filled.Check, contentDescription = null) }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = pillBackgroundLight,
                                    selectedLabelColor = colorScheme.primary,
                                    selectedLeadingIconColor = colorScheme.primary
                                )
                            )
                        }
                    }
                    if (diferencia > 0) {
                        Surface(
                            color = amberBackgroundContainerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Diferencia: ${formatearHoras(diferencia)} h no realizadas.",
                                color = pendingLight,
                                style = typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                        OutlinedTextField(
                            value = motivo,
                            onValueChange = { motivo = it },
                            label = { Text("Motivo de horas no realizadas") },
                            minLines = 3,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Button(
                        onClick = onEnviarReporte,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Enviar reporte")
                    }
                }
            }
        }
    }
}

@Composable
private fun DropdownCampo(
    label: String,
    valor: String,
    opciones: List<String>,
    onSeleccionar: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, colorScheme.outlineVariant), RoundedCornerShape(4.dp))
                .clickable { abierto = true }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(text = label, style = typography.labelSmall, color = colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = valor, style = typography.titleMedium, modifier = Modifier.weight(1f))
                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
            }
        }
        DropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEachIndexed { index, opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccionar(index)
                        abierto = false
                    }
                )
            }
        }
    }
}

private fun formatearHoras(horas: Double): String {
    val redondeado = round(horas * 10) / 10
    return if (redondeado == redondeado.toInt().toDouble()) {
        redondeado.toInt().toString()
    } else {
        redondeado.toString()
    }
}
