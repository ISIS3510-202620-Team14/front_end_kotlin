package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enad.enadmovil.data.local.VistaPreviaCsv
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadLine

@Composable
fun VistaPreviaCsvHoras(vista: VistaPreviaCsv, onCompartir: () -> Unit, onCerrar: () -> Unit) {
    var verTexto by rememberSaveable { mutableStateOf(false) }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(EnadBackground).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Vista previa del CSV", style = MaterialTheme.typography.titleLarge)
            Text(vista.archivo.name, style = MaterialTheme.typography.bodyMedium)
            Text(
                if (vista.externo) "Guardado en Documentos de la app" else "Guardado en el almacenamiento de la app",
                style = MaterialTheme.typography.bodySmall
            )
            val semanas = (vista.filas.size - 1).coerceAtLeast(0)
            Text(
                "${if (semanas == 1) "1 semana" else "$semanas semanas"} · ${vista.filas.firstOrNull()?.size ?: 0} columnas",
                style = MaterialTheme.typography.bodySmall
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !verTexto, onClick = { verTexto = false }, label = { Text("Tabla") })
                FilterChip(selected = verTexto, onClick = { verTexto = true }, label = { Text("Texto") })
            }
            EnadPanel(modifier = Modifier.weight(1f)) {
                Column(Modifier.verticalScroll(rememberScrollState()).horizontalScroll(rememberScrollState())) {
                    if (verTexto) {
                        SelectionContainer {
                            Text(vista.texto, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace))
                        }
                    } else {
                        vista.filas.forEachIndexed { i, fila ->
                            Row(
                                if (i == 0) Modifier.background(EnadLine) else Modifier
                            ) {
                                fila.forEach { celda ->
                                    Text(
                                        celda,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (i == 0) FontWeight.SemiBold else FontWeight.Normal,
                                        modifier = Modifier.width(132.dp).padding(horizontal = 6.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EnadSecondaryButton("Cerrar", onClick = onCerrar, modifier = Modifier.weight(1f))
                EnadPrimaryButton("Compartir", onClick = onCompartir, modifier = Modifier.weight(1f))
            }
        }
    }
}