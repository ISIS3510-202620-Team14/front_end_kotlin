package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi.FilaReporte
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadPrimary
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val LOCALE_CO: Locale = Locale.forLanguageTag("es-CO")
private val FORMATO_SEMANA: DateTimeFormatter = DateTimeFormatter.ofPattern("d 'de' MMMM", LOCALE_CO)

/** Pestaña "Reportes" del admin: responde la BQ tipo 1 semana por semana. */
@Composable
fun ReporteSincronizacionSection(
    modifier: Modifier = Modifier,
    viewModel: ReporteSincronizacionViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val reporte = estado.reporte

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Sincronización de agrupaciones",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sesiones que no llegaron al servidor en 24 h o terminaron en error.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = viewModel::cargar, enabled = !estado.cargando) {
                Text("Actualizar")
            }
        }

        when {
            estado.cargando -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            estado.error != null -> TarjetaMensaje(estado.error.orEmpty())
            reporte == null || reporte.semanas.isEmpty() ->
                TarjetaMensaje("Todavía no hay sesiones de agrupación en las últimas 8 semanas.")
            else -> reporte.semanas.forEach { semana ->
                TarjetaSemana(semana, reporte.filas.filter { it.semana == semana.semana })
            }
        }
    }
}

@Composable
private fun TarjetaSemana(semana: FilaReporte, filas: List<FilaReporte>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Semana del ${fechaLegible(semana.semana)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "${semana.tarde + semana.errores} de ${semana.total} no llegaron a tiempo",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = porcentajeTexto(semana.porcentaje),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = EnadPrimary
            )
        }
        if (semana.provisional) {
            Text(
                text = "Semana en curso: el dato todavía puede cambiar",
                fontSize = 12.sp,
                color = EnadPendienteText,
                modifier = Modifier
                    .background(EnadPendienteBg, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
        Text(
            text = "A tiempo ${semana.aTiempo} · Tarde ${semana.tarde} · Con error ${semana.errores}",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        filas.forEach { FilaDesglose(it) }
    }
}

@Composable
private fun FilaDesglose(fila: FilaReporte) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${nombrePlataforma(fila.plataforma)} · ${fila.version.orEmpty()}",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = if (fila.conectividad == "offline") "Creadas sin conexión" else "Creadas con conexión",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = porcentajeTexto(fila.porcentaje),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${fila.tarde + fila.errores} de ${fila.total}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaMensaje(texto: String) {
    Text(
        text = texto,
        fontSize = 14.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp)
    )
}

private fun fechaLegible(semana: String): String =
    runCatching { LocalDate.parse(semana).format(FORMATO_SEMANA) }.getOrDefault(semana)

private fun porcentajeTexto(porcentaje: Double?): String = when {
    porcentaje == null -> "—"
    porcentaje % 1.0 == 0.0 -> "${porcentaje.toLong()} %"
    else -> String.format(LOCALE_CO, "%.1f %%", porcentaje)
}

private fun nombrePlataforma(plataforma: String?): String = when (plataforma) {
    "kotlin" -> "Kotlin"
    "flutter" -> "Flutter"
    else -> plataforma ?: "?"
}
