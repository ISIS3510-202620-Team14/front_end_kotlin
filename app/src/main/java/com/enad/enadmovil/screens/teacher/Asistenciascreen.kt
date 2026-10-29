package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DocumentScanner
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.local.entity.EstadoRemoto
import com.enad.enadmovil.data.sync.ControlSincronizacion
import com.enad.enadmovil.ui.components.EnadBottomBar
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadSegmented
import com.enad.enadmovil.ui.components.EnadTopBar
import com.enad.enadmovil.ui.components.TeacherNavItems
import com.enad.enadmovil.ui.components.enadFieldColors
import com.enad.enadmovil.ui.screens.teacher.scan.CameraCapture
import com.enad.enadmovil.ui.screens.teacher.scan.EscanerListaSheet
import com.enad.enadmovil.ui.screens.teacher.scan.ScanViewModel
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBorder
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface
import com.enad.enadmovil.ui.theme.PanelShape
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.SideEffect
import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.feature.ayuda.LocalGuia
import com.enad.enadmovil.feature.ayuda.objetivoGuia
private val CURSOS_DISPONIBLES = listOf("Todos", "Grado 3", "Grado 4", "Grado 5")

data class DiaSemana(
    val abreviatura: String,
    val numero: Int,
    val nombreLargo: String,
    val fecha: LocalDate
)

enum class EstadoAsistencia { PENDIENTE, ASISTIO, NO_ASISTIO }

data class EstudianteAsistencia(
    val id: String,
    val numero: Int,
    val nombre: String,
    val etiqueta: String,
    val estado: EstadoAsistencia = EstadoAsistencia.PENDIENTE,
    val semana: List<EstadoAsistencia> = emptyList() // lunes a viernes
)

data class EstudianteSalon(val id: String, val nombre: String, val grado: String)

private val LOCALE_ES = Locale.forLanguageTag("es")

// "Sáb 17/10/2026", como AppDate.label en Flutter.
private fun etiquetaFecha(fecha: LocalDate): String {
    val dia = fecha.dayOfWeek.getDisplayName(TextStyle.SHORT, LOCALE_ES).removeSuffix(".")
        .replaceFirstChar { it.titlecase(LOCALE_ES) }
    return "$dia ${fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
}

/**
 * Asistencia de la sesión con el diseño de Flutter (students_view): buscador, curso compacto
 * con edición, resumen con porcentaje, secciones "Sin registrar" y "Registrados" (plegable)
 * y la barra "Faltan N / Guardar".
 *
 * Cada marca se guarda en el teléfono al tocarla (Room); Guardar pide subir lo pendiente.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AsistenciaScreen(
    profesorNombre: String = "Mateo",
    @Suppress("UNUSED_PARAMETER") grupoSubtitulo: String = "",
    onCambiarUsuario: () -> Unit = {},
    onTabClick: (String) -> Unit = {},
    onAbrirEstudiante: (id: String) -> Unit = {},
    onGestionarEstudiantes: (() -> Unit)? = null,
    viewModel: AsistenciaViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsStateWithLifecycle()

    val dias = remember { diasDeLaSemana() }
    var cursoSeleccionado by remember { mutableStateOf(CURSOS_DISPONIBLES.first()) }
    var fechaSeleccionada by remember { mutableStateOf(diaInicial(dias).fecha) }
    var busqueda by remember { mutableStateOf("") }
    var filtrosAbiertos by remember { mutableStateOf(false) }
    var registradosAbiertos by remember { mutableStateOf(true) }
    var mostrarFecha by remember { mutableStateOf(false) }
    var mostrarEscaner by remember { mutableStateOf(false) }
    var mostrarInesperado by remember { mutableStateOf(false) }
    var guardando by remember { mutableStateOf(false) }

    // Orden en que se marcó cada estudiante: los registrados se listan en ese orden.
    val ordenMarcas = remember { mutableStateMapOf<String, Int>() }
    var secuencia by remember { mutableIntStateOf(0) }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val guia = LocalGuia.current
    val lista = rememberLazyListState()

    val estudiantes = ui.estudiantes
    val total = estudiantes.size
    val registrados = estudiantes.count { it.estado != EstadoAsistencia.PENDIENTE }
    val asistieron = estudiantes.count { it.estado == EstadoAsistencia.ASISTIO }
    val faltan = total - registrados
    val porcentaje = if (total == 0) 0 else asistieron * 100 / total

    // Cuando la subida termina, la barra de guardado desaparece y se confirma el envío.
    LaunchedEffect(guardando, ui.pendientesSync) {
        if (guardando && ui.pendientesSync == 0) {
            guardando = false
            guia?.avanzar(Guia.ASISTENCIA, 5)
            snackbar.showSnackbar("Asistencia enviada.")
        }
    }

    val q = busqueda.trim().lowercase()
    val filtrados = estudiantes.filter { q.isEmpty() || it.nombre.lowercase().contains(q) || it.etiqueta.lowercase().contains(q) }
    val sinRegistrar = filtrados.filter { it.estado == EstadoAsistencia.PENDIENTE }.sortedBy { it.nombre.lowercase() }
    val yaRegistrados = filtrados.filter { it.estado != EstadoAsistencia.PENDIENTE }
        .sortedWith(compareBy({ ordenMarcas[it.id] ?: 0 }, { it.nombre.lowercase() }))

    val marcar = { id: String, estado: EstadoAsistencia ->
        if (id !in ordenMarcas) ordenMarcas[id] = ++secuencia
        viewModel.marcar(id, estado)
    }

    val mostrarBarraGuardar = guardando || faltan > 0 || ui.pendientesSync > 0

    if (guia != null && guia.flujo == Guia.ASISTENCIA) {
        SideEffect {
            guia.fijarRequisito(requisitoAsistencia(guia.paso, ui.cargando || guardando, faltan))
        }
        // Paso 4: el botón "Agregar estudiante" es el último de la lista; se baja hasta allí para que exista y se resalte.
        LaunchedEffect(guia.paso) {
            if (guia.paso == 4) lista.animateScrollToItem((lista.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
        }
        LaunchedEffect(guia.paso, ui.cargando, guardando, faltan, ui.pendientesSync) {
            if (guia.paso == 5 && !ui.cargando && !guardando && total > 0 && faltan == 0 && ui.pendientesSync == 0) {
                guia.avanzar(Guia.ASISTENCIA, 5)
            }
        }
    }


    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { EnadTopBar(nombre = profesorNombre, onCambiarUsuario = onCambiarUsuario) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Column {
                if (mostrarBarraGuardar && total > 0) {
                    BarraGuardar(
                        texto = when {
                            guardando -> "Guardando…"
                            faltan == 0 -> "Lista para guardar"
                            else -> "Faltan $faltan"
                        },
                        habilitado = faltan == 0 && !guardando,
                        onGuardar = {
                            guardando = true
                            viewModel.guardar(alQuedarPendiente = { guardando = false })
                        },
                        modifier = Modifier.objetivoGuia(Guia.ASISTENCIA, 5)
                    )
                }
                if (guia?.activa != true) {
                    EnadBottomBar(
                        items = TeacherNavItems.mapIndexed { i, item ->
                            if (i == TeacherTabs.MI_LISTA) item.copy(badge = ui.pendientesSync) else item
                        },
                        seleccionado = TeacherTabs.MI_LISTA,
                        onSeleccionar = { i -> onTabClick(TeacherNavItems[i].label) }
                    )
                }

            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 12.dp)) {
                EnadHeading("Asistencia")
                Spacer(Modifier.height(20.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = {
                        busqueda = it
                        registradosAbiertos = true
                    },
                    placeholder = { Text("Buscar", color = EnadSoftInk) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = EnadPrimary) },
                    singleLine = true,
                    shape = ControlShape,
                    colors = enadFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            LazyColumn(
                state = lista,
                // Paso 3: toda la lista es el objetivo (las fichas lejanas todavía no existen en pantalla).
                modifier = Modifier.fillMaxSize().objetivoGuia(Guia.ASISTENCIA, 3),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Column(Modifier.objetivoGuia(Guia.ASISTENCIA, 1).objetivoGuia(Guia.ASISTENCIA, 2)) {
                        EnadPanel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Curso · ${cursoSeleccionado}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                                TextButton(onClick = { filtrosAbiertos = !filtrosAbiertos }) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(if (filtrosAbiertos) "Cerrar" else "Editar", style = MaterialTheme.typography.labelMedium, color = EnadInk)
                                }
                            }
                        }
                        if (filtrosAbiertos) {
                            Spacer(Modifier.height(12.dp))
                            EnadPanel {
                                SelectorCurso(
                                    seleccionado = cursoSeleccionado,
                                    onSeleccionar = {
                                        cursoSeleccionado = it
                                        viewModel.seleccionarGrado(it.removePrefix("Grado ").toIntOrNull()) // "Todos" -> null
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                                AccionFiltro("Elegir fecha", Icons.Outlined.CalendarMonth) { mostrarFecha = true }
                                AccionFiltro("Descartar cambios") { viewModel.limpiar() }
                                AccionFiltro("Estudiantes") {
                                    filtrosAbiertos = false
                                    // Abrir Estudiantes cambia de pantalla: la guía se cierra.
                                    guia?.salir()
                                    if (onGestionarEstudiantes != null) onGestionarEstudiantes() else mostrarEscaner = true
                                }
                            }
                        }
                    }
                }

                item {
                    ResumenAsistencia(total = total, fecha = fechaSeleccionada, porcentaje = porcentaje)
                }

                if (ui.reemplazadas.isNotEmpty()) {
                    item {
                        PanelMarcasReemplazadas(
                            marcas = ui.reemplazadas,
                            onRevisada = { id, fecha -> viewModel.revisarReemplazada(id, fecha) }
                        )
                    }
                }

                if (estudiantes.isEmpty() || filtrados.isEmpty()) {
                    item {
                        EnadPanel {
                            Text(
                                text = when {
                                    ui.cargando -> "Cargando…"
                                    ui.sinColegio -> "Tu cuenta todavía no tiene un colegio asignado. Pide a un administrador que lo asigne."
                                    estudiantes.isNotEmpty() -> "Ningún estudiante coincide con «$busqueda»."
                                    else -> "Aún no hay estudiantes en este curso. Añádelos desde Estudiantes o con el escáner."
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (estudiantes.isEmpty() && !ui.cargando && !ui.sinColegio) {
                                Spacer(Modifier.height(12.dp))
                                EnadSecondaryButton("Añadir estudiantes con escáner", onClick = { mostrarEscaner = true }, icon = Icons.Outlined.DocumentScanner)
                            }
                        }
                    }
                }

                if (sinRegistrar.isNotEmpty()) {
                    item { Text("Sin registrar", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 4.dp, top = 4.dp)) }
                    itemsIndexed(sinRegistrar, key = { _, e -> e.id }) { indice, e ->
                        TarjetaAsistencia(e, onAbrir = { if (guia?.activa != true) onAbrirEstudiante(e.id) }, onMarcar = { marcar(e.id, it) })
                    }
                }

                if (yaRegistrados.isNotEmpty()) {
                    item {
                        OutlinedButton(
                            onClick = { registradosAbiertos = !registradosAbiertos },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                            shape = ControlShape,
                            border = BorderStroke(1.dp, EnadLine),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = EnadSurface, contentColor = EnadInk)
                        ) {
                            Icon(if (registradosAbiertos) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = EnadPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                (if (registradosAbiertos) "Ocultar" else "Mostrar") + " registrados (${yaRegistrados.size})",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                    if (registradosAbiertos) {
                        items(yaRegistrados, key = { it.id }) { e ->
                            TarjetaAsistencia(e, onAbrir = { onAbrirEstudiante(e.id) }, onMarcar = { marcar(e.id, it) })
                        }
                    }
                }

                if (estudiantes.isNotEmpty()) {
                    item {
                        EnadSecondaryButton(
                            "Agregar estudiante a la sesión",
                            onClick = { mostrarInesperado = true },
                            icon = Icons.Outlined.PersonAddAlt,
                            modifier = Modifier.objetivoGuia(Guia.ASISTENCIA, 4)
                        )
                    }
                }
            }
        }
    }

    if (mostrarFecha) {
        // La asistencia se descarga por semana: se puede elegir de lunes a viernes de esta semana, hasta hoy.
        val hoy = LocalDate.now()
        val permitidas = dias.map { it.fecha }.filter { !it.isAfter(hoy) }.toSet()
        val estado = rememberDatePickerState(
            initialSelectedDateMillis = fechaSeleccionada.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate() in permitidas
                override fun isSelectableYear(year: Int): Boolean = year == hoy.year
            }
        )
        DatePickerDialog(
            onDismissRequest = { mostrarFecha = false },
            confirmButton = {
                TextButton(onClick = {
                    estado.selectedDateMillis?.let { ms ->
                        val fecha = Instant.ofEpochMilli(ms).atZone(ZoneOffset.UTC).toLocalDate()
                        fechaSeleccionada = fecha
                        viewModel.seleccionarFecha(fecha)
                    }
                    mostrarFecha = false
                }) { Text("Aceptar", color = EnadInk) }
            },
            dismissButton = { TextButton(onClick = { mostrarFecha = false }) { Text("Cancelar", color = EnadInk) } }
        ) { DatePicker(state = estado) }
    }

    if (mostrarInesperado) {
        EstudianteInesperadoDialog(
            roster = ui.roster,
            onSeleccionar = { alumno ->
                marcar(alumno.id, EstadoAsistencia.ASISTIO)
                mostrarInesperado = false
            },
            onCrear = { nombre, edad ->
                viewModel.crearInesperado(nombre, edad)
                mostrarInesperado = false
            },
            onDismiss = { mostrarInesperado = false }
        )
    }

    if (mostrarEscaner) {
        EscanerListaSheet(
            onImportar = { filas ->
                viewModel.importar(filas) // guarda en Room y programa la subida
                mostrarEscaner = false
            },
            onCancelar = { mostrarEscaner = false }
        )
    }

    ui.mensaje?.let { mensaje ->
        AlertDialog(
            onDismissRequest = viewModel::consumirMensaje,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = PanelShape,
            title = { Text(text = "Aviso", style = MaterialTheme.typography.titleLarge) },
            text = { Text(text = mensaje, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = viewModel::consumirMensaje) {
                    Text(text = "Entendido", color = EnadInk, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorCurso(seleccionado: String, onSeleccionar: (String) -> Unit) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }) {
        OutlinedTextField(
            value = seleccionado,
            onValueChange = {},
            readOnly = true,
            label = { Text("Curso") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            shape = ControlShape,
            colors = enadFieldColors(),
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            CURSOS_DISPONIBLES.forEach { curso ->
                DropdownMenuItem(text = { Text(curso) }, onClick = {
                    onSeleccionar(curso)
                    abierto = false
                })
            }
        }
    }
}

@Composable
private fun AccionFiltro(texto: String, icono: androidx.compose.ui.graphics.vector.ImageVector? = null, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        if (icono != null) {
            Icon(icono, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(texto, style = MaterialTheme.typography.labelMedium, color = EnadInk)
    }
}

@Composable
private fun ResumenAsistencia(total: Int, fecha: LocalDate, porcentaje: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$total", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.width(16.dp))
        Column {
            Text("Asistencia por estudiante", style = MaterialTheme.typography.titleMedium)
            Text(etiquetaFecha(fecha), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Asistió el $porcentaje% de estudiantes", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            EnadProgress(porcentaje / 100f, modifier = Modifier.width(220.dp))
        }
    }
}
private fun requisitoAsistencia(paso: Int, cargando: Boolean, faltan: Int): String? = when {
    paso !in 1..4 -> null
    cargando -> "Espera a que termine de cargar la sesión."
    paso == 3 && faltan > 0 -> "Falta marcar a $faltan estudiantes. Revisa la lista."
    else -> null
}
/** Tarjeta de estudiante (AttendanceStudentCard): al marcar, borde negro, fondo tenue y check animado. */
@Composable
private fun TarjetaAsistencia(
    estudiante: EstudianteAsistencia,
    onAbrir: () -> Unit,
    onMarcar: (EstadoAsistencia) -> Unit,
    modifier: Modifier = Modifier
) {
    val marcado = estudiante.estado != EstadoAsistencia.PENDIENTE
    val duracion = tween<Color>(450)
    val borde by animateColorAsState(if (marcado) EnadInk.copy(alpha = 0.33f) else Color.Transparent, duracion, label = "borde")
    val fondo by animateColorAsState(
        if (marcado) EnadInk.copy(alpha = 0.03f).compositeOver(EnadSurface) else EnadSurface, duracion, label = "fondo"
    )
    Surface(
        shape = PanelShape,
        color = fondo,
        border = BorderStroke(2.dp, borde),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    estudiante.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f).clickable(onClick = onAbrir).padding(vertical = 12.dp)
                )
                Spacer(Modifier.width(8.dp))
                Box(Modifier.size(24.dp)) {
                    androidx.compose.animation.AnimatedVisibility(visible = marcado, enter = scaleIn(tween(450)), exit = scaleOut(tween(450))) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EnadInk, modifier = Modifier.size(24.dp))
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(estudiante.etiqueta, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(12.dp))
            EnadSegmented(
                opciones = listOf("Asistió", "No asistió"),
                seleccionada = when (estudiante.estado) {
                    EstadoAsistencia.ASISTIO -> 0
                    EstadoAsistencia.NO_ASISTIO -> 1
                    EstadoAsistencia.PENDIENTE -> null
                },
                onSeleccionar = { i -> onMarcar(if (i == 0) EstadoAsistencia.ASISTIO else EstadoAsistencia.NO_ASISTIO) }
            )
        }
    }
}

/**
 * Marcas que no se aplicaron porque otro celular registró primero: el docente ve la suya y la que
 * quedó en el servidor, para corregirla en la lista si hace falta.
 */
@Composable
private fun PanelMarcasReemplazadas(
    marcas: List<ControlSincronizacion.MarcaReemplazada>,
    onRevisada: (String, String) -> Unit
) {
    EnadPanel {
        Text("Marcas reemplazadas", style = MaterialTheme.typography.titleMedium)
        Text(
            "Otro celular registró primero la asistencia de ${marcas.size} estudiante(s). " +
                "Estas son tus marcas y las que quedaron guardadas.",
            style = MaterialTheme.typography.bodyMedium
        )
        marcas.forEach { marca ->
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(marca.nombre.ifBlank { "Estudiante" }, style = MaterialTheme.typography.labelMedium)
                    Text(
                        "${fechaLegible(marca.fecha)} · tú marcaste ${estadoLegible(marca.enviada)}, " +
                            "quedó ${estadoLegible(marca.conservada)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                TextButton(onClick = { onRevisada(marca.estudianteId, marca.fecha) }) {
                    Text("Revisada", style = MaterialTheme.typography.labelMedium, color = EnadPrimary)
                }
            }
        }
    }
}

private fun fechaLegible(fecha: String): String =
    runCatching { etiquetaFecha(LocalDate.parse(fecha)) }.getOrDefault(fecha)

private fun estadoLegible(estado: String): String = when (estado) {
    EstadoRemoto.VINO -> "asistió"
    EstadoRemoto.NO_VINO -> "no asistió"
    else -> "sin registrar"
}

@Composable
private fun BarraGuardar(texto: String, habilitado: Boolean, onGuardar: () -> Unit, modifier: Modifier = Modifier) {
    Surface(color = EnadSurface, modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(texto, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            Button(
                onClick = onGuardar,
                enabled = habilitado,
                shape = ControlShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = EnadPrimary,
                    contentColor = EnadSurface,
                    disabledContainerColor = EnadLine,
                    disabledContentColor = EnadSoftInk
                ),
                modifier = Modifier.heightIn(min = 48.dp)
            ) { Text("Guardar", style = MaterialTheme.typography.labelLarge) }
        }
    }
}

@Composable
private fun EstudianteInesperadoDialog(
    roster: List<EstudianteSalon>,
    onSeleccionar: (EstudianteSalon) -> Unit,
    onCrear: (nombre: String, edad: Int?) -> Unit,
    onDismiss: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val resultados = remember(busqueda) {
        if (busqueda.isBlank()) roster else roster.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = PanelShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(text = "Agregar estudiante a la sesión", style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Busca en la lista completa del salón antes de crear uno nuevo.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = { Text("Buscar por nombre") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = ControlShape,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (resultados.isEmpty()) {
                    CrearEstudianteInesperadoForm(
                        busqueda = busqueda,
                        onCancelar = onDismiss,
                        onCrear = onCrear
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        resultados.forEach { alumno ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onSeleccionar(alumno) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(text = alumno.nombre, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
                                Text(text = alumno.grado, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            HorizontalDivider(color = EnadBorder)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        shape = ControlShape,
                        border = BorderStroke(1.dp, EnadBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                    ) {
                        Text(text = "Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// El estudiante se crea con código provisional: el servidor asigna PROV-XXXX y un
// mentor lo reemplaza después. Se guarda primero en el teléfono y se sube con internet.
@Composable
private fun CrearEstudianteInesperadoForm(
    busqueda: String,
    onCancelar: () -> Unit,
    onCrear: (nombre: String, edad: Int?) -> Unit
) {
    var nombreCompleto by remember { mutableStateOf(busqueda) }
    var edad by remember { mutableStateOf("") }

    Text(
        text = "\"$busqueda\" no está en la lista. Se creará con código provisional.",
        fontSize = 12.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = nombreCompleto,
        onValueChange = { nombreCompleto = it },
        label = { Text("Apellidos y nombre") },
        singleLine = true,
        shape = ControlShape,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = edad,
        onValueChange = { nuevo -> edad = nuevo.filter { it.isDigit() } },
        label = { Text("Edad") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = ControlShape,
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onCancelar,
            modifier = Modifier.weight(1f),
            shape = ControlShape,
            border = BorderStroke(1.dp, EnadBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
        ) {
            Text(text = "Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onCrear(nombreCompleto.trim(), edad.toIntOrNull()) },
            enabled = nombreCompleto.isNotBlank(),
            modifier = Modifier.weight(1f),
            shape = ControlShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        ) {
            Text(text = "Crear", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}

