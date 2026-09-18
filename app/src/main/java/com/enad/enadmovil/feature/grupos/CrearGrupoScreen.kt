package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.core.ui.theme.inkContainerLight
import com.enad.enadmovil.core.ui.theme.onInkLight
import com.enad.enadmovil.domain.model.Nino
import kotlin.math.ceil

val docentesDisponibles = listOf("Yo", "Prof. Nelson", "Prof. Marina", "Sin asignar")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrearGrupoScreen(
    materia: String,
    onBack: () -> Unit,
    onGuardar: (nombre: String, ninos: List<Nino>, docente: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var cantidadNinos by rememberSaveable { mutableIntStateOf(8) }
    var cantidadDocentes by rememberSaveable { mutableIntStateOf(3) }
    var nombreGrupo by rememberSaveable { mutableStateOf("") }
    var docenteSeleccionado by rememberSaveable { mutableStateOf(docentesDisponibles.first()) }
    var menuDocenteAbierto by remember { mutableStateOf(false) }
    var mostrarMeterNinos by remember { mutableStateOf(false) }
    var ninosSeleccionados by remember { mutableStateOf(setOf<Nino>()) }

    val porGrupo = if (cantidadDocentes > 0) {
        ceil(cantidadNinos.toDouble() / cantidadDocentes).toInt()
    } else 0

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Volver")
            }
            Column {
                Text(text = "Crear grupo", style = typography.titleLarge)
                Text(
                    text = materia,
                    style = typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "CUÁNTOS NIÑOS POR GRUPO",
                    style = typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CalculadoraCaja(
                        valor = cantidadNinos.toString(),
                        etiqueta = "niños",
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "÷", style = typography.titleLarge)
                    CalculadoraCaja(
                        valor = cantidadDocentes.toString(),
                        etiqueta = "docentes",
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = "=", style = typography.titleLarge)
                    CalculadoraCaja(
                        valor = porGrupo.toString(),
                        etiqueta = "por grupo",
                        fondo = inkContainerLight,
                        contenido = onInkLight,
                        modifier = Modifier.weight(1f)
                    )
                }

                ContadorFila(
                    pregunta = "¿Cuántos niños hay?",
                    valor = cantidadNinos,
                    onDecrementar = { if (cantidadNinos > 0) cantidadNinos-- },
                    onIncrementar = { if (cantidadNinos < 30) cantidadNinos++ }
                )
                ContadorFila(
                    pregunta = "¿Cuántos docentes somos?",
                    valor = cantidadDocentes,
                    onDecrementar = { if (cantidadDocentes > 1) cantidadDocentes-- },
                    onIncrementar = { cantidadDocentes++ }
                )

                Text(
                    text = "El salón aguanta máximo 30. Ese es el límite, no la cuenta.",
                    style = typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "DATOS DEL GRUPO",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = nombreGrupo,
                    onValueChange = { nombreGrupo = it },
                    label = { Text("Nombre del grupo") },
                    modifier = Modifier.fillMaxWidth()
                )
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { menuDocenteAbierto = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Docente", style = typography.bodyLarge)
                        Text(
                            text = docenteSeleccionado,
                            style = typography.titleMedium,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuDocenteAbierto,
                        onDismissRequest = { menuDocenteAbierto = false },
                        containerColor = colorScheme.surface,
                        offset = DpOffset(x = 140.dp, y = 0.dp),
                        modifier = Modifier.width(240.dp)
                    ) {
                        docentesDisponibles.forEach { docente ->
                            val seleccionado = docente == docenteSeleccionado
                            DropdownMenuItem(
                                text = { Text(docente, style = typography.titleMedium) },
                                onClick = {
                                    docenteSeleccionado = docente
                                    menuDocenteAbierto = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                                modifier = Modifier.background(
                                    if (seleccionado) colorScheme.surfaceVariant else Color.Transparent
                                )
                            )
                        }
                    }
                }
            }
        }

        Text(
            text = "NIÑOS · ${ninosSeleccionados.size}",
            style = typography.labelSmall,
            color = colorScheme.onSurfaceVariant
        )
        OutlinedCard(
            onClick = { mostrarMeterNinos = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Meter niños",
                style = typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp)
            )
        }

        Button(
            onClick = {
                onGuardar(nombreGrupo, ninosSeleccionados.toList(), docenteSeleccionado)
                onBack()
            },
            enabled = nombreGrupo.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Text("Guardar grupo")
        }
    }

    if (mostrarMeterNinos) {
        MeterNinosDialog(
            titulo = "Meter niños a este grupo",
            ninos = ninosDeEjemplo,
            seleccionados = ninosSeleccionados,
            onSeleccionadosChange = { ninosSeleccionados = it },
            onAgregar = { mostrarMeterNinos = false },
            onDismiss = { mostrarMeterNinos = false }
        )
    }
}

@Composable
private fun CalculadoraCaja(
    valor: String,
    etiqueta: String,
    modifier: Modifier = Modifier,
    fondo: Color = colorScheme.surfaceVariant,
    contenido: Color = colorScheme.onSurface
) {
    Surface(
        color = fondo,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text(text = valor, style = typography.titleLarge, color = contenido)
            Text(text = etiqueta, style = typography.bodyMedium, color = contenido)
        }
    }
}

@Composable
private fun ContadorFila(
    pregunta: String,
    valor: Int,
    onDecrementar: () -> Unit,
    onIncrementar: () -> Unit
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = pregunta, style = typography.bodyLarge, modifier = Modifier.weight(1f))
        BotonContador(texto = "-", onClick = onDecrementar)
        Text(
            text = valor.toString(),
            style = typography.titleMedium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        BotonContador(texto = "+", onClick = onIncrementar)
    }
}

@Composable
private fun BotonContador(texto: String, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.size(36.dp)) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = texto, style = typography.titleMedium)
        }
    }
}
