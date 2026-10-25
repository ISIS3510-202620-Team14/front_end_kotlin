package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.remote.CloudFunctionsApi.FilaReporte
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary

/** Reporte "Sincronización" (BQ tipo 1): sesiones de agrupación que no llegaron en 24 h, semana por semana. */
@Composable
fun ReporteSincronizacionSection(viewModel: ReporteSincronizacionViewModel = viewModel()) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EncabezadoReporte(
            titulo = "Sincronización de agrupaciones",
            descripcion = "Sesiones que no llegaron al servidor en 24 h o terminaron en error (últimas 8 semanas).",
            cargando = estado.cargando,
            onActualizar = viewModel::actualizar
        )
        val carga: Carga<CloudFunctionsApi.ReporteSincronizacion?> = when {
            estado.cargando -> Carga.Cargando
            estado.error != null -> Carga.Fallo(estado.error.orEmpty())
            else -> Carga.Lista(estado.reporte)
        }
        ContenidoCarga(
            carga = carga,
            vacio = { it == null || it.semanas.isEmpty() },
            textoVacio = "Todavía no hay sesiones de agrupación en las últimas 8 semanas."
        ) { reporte ->
            reporte!!.semanas.sortedByDescending { it.semana }.forEach { semana ->
                TarjetaSemana(semana, reporte.filas.filter { it.semana == semana.semana })
            }
        }
    }
}

@Composable
private fun TarjetaSemana(semana: FilaReporte, filas: List<FilaReporte>) {
    EnadPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(semanaLegible(semana.semana), style = MaterialTheme.typography.titleMedium)
                Text(
                    "${semana.tarde + semana.errores} de ${semana.total} no llegaron a tiempo",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(porcentajeTexto(semana.porcentaje), style = MaterialTheme.typography.headlineSmall, color = EnadPrimary)
        }
        Spacer(Modifier.height(10.dp))
        EnadProgress(((semana.porcentaje ?: 0.0) / 100).toFloat(), color = EnadPrimary)
        Spacer(Modifier.height(10.dp))
        if (semana.provisional) {
            PildoraProvisional()
            Spacer(Modifier.height(8.dp))
        }
        Text(
            "A tiempo ${semana.aTiempo} · Tarde ${semana.tarde} · Con error ${semana.errores}",
            style = MaterialTheme.typography.bodyMedium
        )
        filas.forEach {
            Spacer(Modifier.height(8.dp))
            FilaDesglose(it)
        }
    }
}

@Composable
private fun FilaDesglose(fila: FilaReporte) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadBackground, ControlShape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("${nombrePlataforma(fila.plataforma)} · ${fila.version.orEmpty()}", style = MaterialTheme.typography.labelMedium)
            Text(
                if (fila.conectividad == "offline") "Creadas sin conexión" else "Creadas con conexión",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(porcentajeTexto(fila.porcentaje), style = MaterialTheme.typography.labelLarge)
            Text("${fila.tarde + fila.errores} de ${fila.total}", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun nombrePlataforma(plataforma: String?): String = when (plataforma) {
    "kotlin" -> "Kotlin"
    "flutter" -> "Flutter"
    else -> plataforma ?: "?"
}
