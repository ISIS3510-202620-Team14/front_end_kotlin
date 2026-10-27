package com.enad.enadmovil.ui.screens.teacher.scan

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.domain.model.EstudianteEscaneado
import com.enad.enadmovil.domain.parser.FusionFilas
import com.enad.enadmovil.ui.components.enadFieldColors
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface

/**
 * "Escanear lista de estudiantes" de Flutter: abre la cámara, lee la lista con OCR en el
 * teléfono y deja corregir nombre y código de cada fila antes de importar. Se puede escanear
 * más de una página; las filas se van sumando.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EscanerListaSheet(
    onImportar: (List<EstudianteEscaneado>) -> Unit,
    onCancelar: () -> Unit,
    // Desde Asistencia (SF #3) una fila sin código entra como provisional; Gestionar estudiantes sí lo exige.
    exigirCodigo: Boolean = false,
    scanViewModel: ScanViewModel = viewModel()
) {
    val estado by scanViewModel.uiState.collectAsStateWithLifecycle()
    val filas = remember { mutableStateListOf<EstudianteEscaneado>() }
    var mostrarCamara by remember { mutableStateOf(false) }

    LaunchedEffect(estado.filas) {
        if (estado.filas.isNotEmpty()) {
            filas.addAll(FusionFilas.nuevas(filas, estado.filas))
            scanViewModel.tomarFilas()
        }
    }

    val validas = filas.filter { it.nombre.isNotBlank() && (!exigirCodigo || !it.codigo.isNullOrBlank()) }
    val cerrar = {
        scanViewModel.limpiar()
        onCancelar()
    }

    ModalBottomSheet(
        onDismissRequest = cerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = EnadSurface
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding()
        ) {
            Text("Escanear lista de estudiantes", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            // La cámara no se bloquea mientras se leen las páginas anteriores
            CajaEscaneo(
                leyendo = estado.procesando,
                conFilas = filas.isNotEmpty() || estado.paginas.isNotEmpty(),
                onClick = { mostrarCamara = true }
            )
            if (estado.paginas.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                ProgresoPaginas(estado.paginas, miniatura = scanViewModel::miniatura)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    estado.procesando -> "Leyendo ${estado.paginas.count { it.estado == EstadoPagina.EnCola || it.estado == EstadoPagina.Leyendo }} " +
                        "página(s). Puedes seguir tomando fotos."
                    estado.error != null -> estado.error.orEmpty()
                    filas.isNotEmpty() && exigirCodigo -> "Completa los códigos reales y revisa los nombres antes de importar. " +
                        "Toca uno para corregirlo. Puedes escanear otra página."
                    filas.isNotEmpty() -> "Revisa los nombres antes de importar; los que no tengan código quedan con uno provisional. " +
                        "Los que no tienen grado aparecen en rojo. Puedes escanear otra página."
                    else -> "Toma una foto de la lista impresa o escrita a mano (un estudiante por renglón: nombre y apellido)."
                },
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 14.sp),
                color = if (estado.error != null) EnadPrimary else EnadSoftInk
            )
            if (filas.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    filas.forEachIndexed { i, fila ->
                        FilaEscaneada(
                            fila = fila,
                            onNombre = { filas[i] = fila.copy(nombre = it) },
                            onCodigo = { filas[i] = fila.copy(codigo = it) },
                            onQuitar = { filas.removeAt(i) }
                        )
                        if (i != filas.lastIndex) HorizontalDivider(color = EnadLine)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    onImportar(validas.map { it.copy(nombre = it.nombre.trim(), codigo = it.codigo?.trim()?.ifBlank { null }) })
                    scanViewModel.limpiar()
                },
                enabled = validas.isNotEmpty() && validas.size == filas.size,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary, contentColor = EnadSurface, disabledContainerColor = EnadLine),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                Text(if (validas.isEmpty()) "Importar lista" else "Importar ${validas.size} estudiantes")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = cerrar,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, EnadLine),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EnadInk),
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) { Text("Cancelar") }
        }
    }

    // Cámara a pantalla completa; al tomar la foto se cierra y el OCR corre en el teléfono, sin internet.
    if (mostrarCamara) {
        Dialog(
            onDismissRequest = { mostrarCamara = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            CameraCapture(
                onFotoCapturada = { uri ->
                    mostrarCamara = false
                    scanViewModel.onFotoCapturada(uri)
                },
                onError = { mostrarCamara = false },
                paginas = estado.paginas,
                miniatura = scanViewModel::miniatura,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun CajaEscaneo(leyendo: Boolean, conFilas: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = EnadBackground,
        border = BorderStroke(1.dp, EnadLine),
        modifier = Modifier.fillMaxWidth().height(if (conFilas) 72.dp else 150.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.DocumentScanner,
                    contentDescription = null,
                    tint = EnadPrimary,
                    modifier = Modifier.size(if (conFilas) 28.dp else 48.dp)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (conFilas) "ESCANEAR OTRA PÁGINA" else "ABRIR CÁMARA",
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.5.sp,
                    color = EnadInk
                )
                if (leyendo) {
                    Spacer(Modifier.width(12.dp))
                    CircularProgressIndicator(color = EnadPrimary, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Una línea por foto con su miniatura: en cola, leyéndose, cuántos estudiantes salieron o si falló. */
@Composable
private fun ProgresoPaginas(paginas: List<PaginaEscaneada>, miniatura: suspend (PaginaEscaneada) -> Bitmap?) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        paginas.forEach { pagina ->
            val (texto, error) = when (val estado = pagina.estado) {
                EstadoPagina.EnCola -> "en cola" to false
                EstadoPagina.Leyendo -> "leyendo…" to false
                is EstadoPagina.Lista -> (if (estado.filas == 1) "1 estudiante" else "${estado.filas} estudiantes") to (estado.filas == 0)
                EstadoPagina.Fallo -> "no se pudo leer" to true
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniaturaPagina(pagina, miniatura)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Página ${pagina.numero} · $texto",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (error) EnadPrimary else EnadSoftInk
                )
            }
        }
    }
}

/** Mientras la miniatura no está lista se ve un cuadro gris del mismo tamaño. */
@Composable
internal fun MiniaturaPagina(pagina: PaginaEscaneada, miniatura: suspend (PaginaEscaneada) -> Bitmap?) {
    val imagen by produceState<ImageBitmap?>(null, pagina.foto) {
        value = miniatura(pagina)?.asImageBitmap()
    }
    val forma = RoundedCornerShape(6.dp)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(forma)
            .background(EnadLine, forma)
    ) {
        imagen?.let {
            Image(
                bitmap = it,
                contentDescription = "Foto de la página ${pagina.numero}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun FilaEscaneada(
    fila: EstudianteEscaneado,
    onNombre: (String) -> Unit,
    onCodigo: (String) -> Unit,
    onQuitar: () -> Unit
) {
    Row(modifier = Modifier.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextField(
                value = fila.nombre,
                onValueChange = onNombre,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = fila.codigo.orEmpty(),
                onValueChange = onCodigo,
                singleLine = true,
                label = { Text("Código del estudiante") },
                shape = ControlShape,
                colors = enadFieldColors(),
                textStyle = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.fillMaxWidth()
            )
            // Lo demás que leyó el OCR de la fila; sin grado se usa el curso elegido en la pantalla.
            Text(
                text = listOf(
                    fila.grado?.let { "Grado $it" } ?: "sin grado",
                    fila.edad?.let { "$it años" } ?: "sin edad"
                ).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = if (fila.grado == null) EnadPrimary else EnadSoftInk
            )
        }
        IconButton(onClick = onQuitar) {
            Icon(Icons.Outlined.Close, contentDescription = "Quitar", tint = EnadSoftInk)
        }
    }
}
