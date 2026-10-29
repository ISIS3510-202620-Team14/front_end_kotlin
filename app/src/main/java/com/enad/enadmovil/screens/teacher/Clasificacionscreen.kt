package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PersonAddAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.components.enadFieldColors
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLevel1
import com.enad.enadmovil.ui.theme.EnadLevel2
import com.enad.enadmovil.ui.theme.EnadLevel3
import com.enad.enadmovil.ui.theme.EnadLevel4
import com.enad.enadmovil.ui.theme.EnadLevel5
import com.enad.enadmovil.ui.theme.EnadLevel6
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadNeutral
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface
import com.enad.enadmovil.ui.theme.PanelShape
import kotlinx.coroutines.launch
import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.feature.ayuda.LocalGuia
import com.enad.enadmovil.feature.ayuda.objetivoGuia
import com.enad.enadmovil.domain.model.UltimaEvaluacion
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import java.time.LocalDate
import java.time.ZoneId

// "materia" parametriza la pantalla para que Lectura y Matemáticas la compartan
// (título + niveles propios). Los colores de nivel son los de LevelPalette en Flutter.
data class NivelConfig(val nombre: String, val color: Color)
// "clave" es la materia tal como la nombra el backend en levels (lectura | matematicas).
data class MateriaConfig(val titulo: String, val clave: String, val niveles: List<NivelConfig> = emptyList())

val MATERIA_LECTURA = MateriaConfig(
    titulo = "Lectura",
    clave = "lectura",
    niveles = listOf(
        NivelConfig("Principiante", EnadLevel1),
        NivelConfig("Letra", EnadLevel2),
        NivelConfig("Palabra", EnadLevel3),
        NivelConfig("Párrafo", EnadLevel4),
        NivelConfig("Cuento", EnadLevel5),
        NivelConfig("Comprensión", EnadLevel6)
    )
)

val MATERIA_MATEMATICAS = MateriaConfig(
    titulo = "Matemáticas",
    clave = "matematicas",
    niveles = listOf(
        NivelConfig("Principiante", EnadLevel1),
        NivelConfig("1 dígito", EnadLevel2),
        NivelConfig("2 dígitos", EnadLevel3),
        NivelConfig("Resta", EnadLevel4),
        NivelConfig("División", EnadLevel5),
        NivelConfig("Problema escrito", EnadLevel6)
    )
)

/** Texto sobre un color de nivel: blanco solo sobre el primero (el más oscuro). */
internal fun textoSobreNivel(fondo: Color): Color = if (fondo == EnadLevel1) Color.White else EnadInk

// Chips "CURSO": grado -> etiqueta (null = Todos). Son los grados que maneja ENAd (3 a 5).
private val CLASIFICACION_CURSOS = listOf<Pair<Int?, String>>(
    null to "Todos", 3 to "Grado 3", 4 to "Grado 4", 5 to "Grado 5"
)

// Estudiante de la institución elegida. "sexo", "edad" y "muestra" se completan en la ficha;
// "nivelActual" null = Sin evaluar.
data class EstudianteClasificacion(
    val id: String,
    val numero: Int,
    val nombre: String,
    val grado: Int,
    val sexo: String? = null,
    val edad: String = "",
    val muestra: Int? = null,
    val nivelActual: String? = null,
    val ultimaEvaluacion: String? = null
)

private fun edadValida(edad: String): Boolean = edad.toIntOrNull()?.let { it in 5..18 } == true

private fun estudiantesClasificacionDemo(): List<EstudianteClasificacion> = listOf(
    EstudianteClasificacion("demo-1", 1, "María López Quintero", grado = 3, sexo = "F"),
    EstudianteClasificacion("demo-2", 2, "Juan Carlos Cruz", grado = 3, sexo = "M", edad = "9", muestra = 1, nivelActual = "Palabra"),
    EstudianteClasificacion("demo-3", 3, "Lucía Restrepo", grado = 4, sexo = "F"),
)

@Composable
fun ClasificacionScreen(
    materia: MateriaConfig,
    onBack: () -> Unit = {},
    onVerPerfil: (id: String) -> Unit = {},
    viewModel: ClasificacionViewModel = viewModel(key = "clasificacion-${materia.clave}")
) {
    LaunchedEffect(materia) { viewModel.iniciar(materia) }
    // BQ 9: el cronómetro de clasificación solo corre mientras la pantalla está visible.
    LifecycleResumeEffect(viewModel) {
        viewModel.alReanudar()
        onPauseOrDispose { viewModel.alPausar() }
    }
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    ClasificacionContenido(
        materia = materia,
        estado = estado,
        onBack = onBack,
        onVerPerfil = onVerPerfil,
        onSeleccionarInstitucion = viewModel::seleccionarInstitucion,
        onSeleccionarGrado = viewModel::seleccionarGrado,
        onSeleccionarMomento = viewModel::seleccionarMomento,
        onReintentar = viewModel::reintentar,
        onEvaluar = viewModel::evaluar,
        onRetirar = viewModel::retirar
    )
}

/**
 * Momentos de evaluación (classification_view en Flutter): panel con el momento, "Agregar
 * estudiante a la sesión" y los cursos; después, fichas agrupadas por nivel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClasificacionContenido(
    materia: MateriaConfig,
    estado: ClasificacionUiState,
    onBack: () -> Unit,
    onVerPerfil: (String) -> Unit,
    onSeleccionarInstitucion: (String) -> Unit,
    onSeleccionarGrado: (Int?) -> Unit,
    onSeleccionarMomento: (String) -> Unit,
    onReintentar: () -> Unit,
    onEvaluar: (String, String, String, Int, Int, (String) -> Unit) -> Unit,
    onRetirar: (String, (String) -> Unit) -> Unit
) {
    val guia = LocalGuia.current
    val estudiantes = estado.visibles
    var expandidoId by remember { mutableStateOf<String?>(null) }
    var retiradosAbiertos by remember { mutableStateOf(false) }
    var mostrarAgregar by remember { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val avisar: (String) -> Unit = { texto -> scope.launch { snackbar.showSnackbar(texto) } }
    val avisarEvaluacion: (String) -> Unit = { texto ->
        avisar(texto)
        if (texto == "Evaluación guardada.") guia?.avanzar(Guia.EVALUACION, 3)
    }

    val activos = estudiantes.filter { it.nivelActual != NIVEL_RETIRADO }
    val retirados = estudiantes.filter { it.nivelActual == NIVEL_RETIRADO }
    val hoy = remember { LocalDate.now(ZoneId.of("America/Bogota"))}
    val diasDesde = { e: EstudianteClasificacion -> UltimaEvaluacion.diasDesde(e.ultimaEvaluacion, hoy) }
    val grupos = buildList {
        add("Sin evaluar" to activos.filter { it.nivelActual == null })
        materia.niveles.forEach { nivel -> add(nivel.nombre to activos.filter { it.nivelActual == nivel.nombre }.sortedByDescending { diasDesde(it) ?: 0 }) }
    }.filter { it.second.isNotEmpty() }

    val tarjeta: @Composable (EstudianteClasificacion) -> Unit = { e ->
        FichaEvaluacion(
            estudiante = e,
            materia = materia,
            dias = if (e.nivelActual == NIVEL_RETIRADO) null else diasDesde(e),
            expandida = expandidoId == e.id,
            onAlternar = { expandidoId = if (expandidoId == e.id) null else e.id },
            onVerPerfil = { if (guia?.activa != true) onVerPerfil(e.id) },
            onGuardar = { nivel, sexo, edad, muestra -> onEvaluar(e.id, nivel, sexo, edad, muestra, avisarEvaluacion) },
            onRetirar = { onRetirar(e.id, avisar) }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            BarraConVolver(
                titulo = materia.titulo,
                subtitulo = "${activos.size} de ${estado.estudiantes.size} estudiantes activos",
                onBack = onBack
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Como en Flutter, el paso 3 resalta toda la pantalla: así se puede cambiar el curso o agregar estudiantes.
                .objetivoGuia(Guia.EVALUACION, 3)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp)
        ) {
            EnadPanel {
                Desplegable(
                    etiqueta = "Momento de evaluación *",
                    valor = estado.momento?.let { MOMENTOS_EVALUACION[it] }.orEmpty(),
                    opciones = MOMENTOS_EVALUACION.entries.map { it.key to it.value },
                    onSeleccionar = { clave ->
                        onSeleccionarMomento(clave)
                        guia?.avanzar(Guia.EVALUACION, 2)
                    },
                    modifier = Modifier.objetivoGuia(Guia.EVALUACION, 2)
                )
                if (estado.instituciones.size > 1) {
                    Spacer(Modifier.height(16.dp))
                    Desplegable(
                        etiqueta = "Institución",
                        valor = estado.instituciones.find { it.id == estado.institucionId }?.name.orEmpty(),
                        opciones = estado.instituciones.map { it.id to it.name },
                        onSeleccionar = onSeleccionarInstitucion
                    )
                }
                Spacer(Modifier.height(20.dp))
                EnadSecondaryButton(
                    "Agregar estudiante a la sesión",
                    onClick = { mostrarAgregar = true },
                    icon = Icons.Outlined.PersonAddAlt,
                    enabled = estado.estudiantes.isNotEmpty()
                )
                Spacer(Modifier.height(16.dp))
                EtiquetaSeccion("CURSO")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CLASIFICACION_CURSOS.forEach { (grado, etiqueta) ->
                        ChipCurso(etiqueta, seleccionado = estado.grado == grado) { onSeleccionarGrado(grado) }
                    }
                }
            }

            when {
                estado.cargando -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = EnadPrimary)
                }

                estado.mensaje != null -> Column(Modifier.padding(top = 24.dp)) {
                    Text(estado.mensaje, style = MaterialTheme.typography.bodyMedium, color = EnadPrimary)
                    TextButton(onClick = onReintentar) { Text("Reintentar", color = EnadInk) }
                }

                estado.momento == null -> Text(
                    "Selecciona el momento antes de registrar una evaluación.",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 24.dp)
                )

                estudiantes.isEmpty() -> Text(
                    if (estado.grado == null) "Esta institución aún no tiene estudiantes." else "No hay estudiantes en este curso.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 24.dp)
                )

                else -> {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Las fichas muestran el último nivel registrado. Los nuevos resultados se guardan en el momento seleccionado.",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp)
                    )
                    grupos.forEach { (nombre, lista) ->
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            InsigniaNivel(nombre, materia)
                            Spacer(Modifier.width(8.dp))
                            Text("${lista.size}", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(10.dp))
                        lista.forEach { e ->
                            tarjeta(e)
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    if (retirados.isNotEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        EnadPanel {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { retiradosAbiertos = !retiradosAbiertos },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Retirados · ${retirados.size}",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    if (retiradosAbiertos) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = if (retiradosAbiertos) EnadPrimary else EnadInk
                                )
                            }
                            if (retiradosAbiertos) {
                                Spacer(Modifier.height(12.dp))
                                retirados.forEach { e ->
                                    tarjeta(e)
                                    Spacer(Modifier.height(12.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (mostrarAgregar) {
        AgregarEstudianteDialog(
            estudiantes = estado.estudiantes.filter { it.nivelActual != NIVEL_RETIRADO },
            onElegir = { e ->
                mostrarAgregar = false
                onSeleccionarGrado(e.grado)
                expandidoId = e.id
                avisar("${e.nombre}: completa la ficha y guarda la evaluación.")
            },
            onCerrar = { mostrarAgregar = false }
        )
    }
}

/** Ficha de evaluación de un estudiante (StudentCard en Flutter). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FichaEvaluacion(
    estudiante: EstudianteClasificacion,
    materia: MateriaConfig,
    dias: Int?,
    expandida: Boolean,
    onAlternar: () -> Unit,
    onVerPerfil: () -> Unit,
    onGuardar: (nivel: String, sexo: String, edad: Int, muestra: Int) -> Unit,
    onRetirar: () -> Unit
) {
    // Lo que el docente edita en la ficha; vuelve a los datos guardados si cambia el estudiante.
    var sexo by remember(estudiante) { mutableStateOf(estudiante.sexo) }
    var edad by remember(estudiante) { mutableStateOf(estudiante.edad) }
    var muestra by remember(estudiante) { mutableStateOf(estudiante.muestra) }
    var nivelElegido by remember(estudiante) { mutableStateOf<String?>(null) }
    var confirmarRetiro by remember { mutableStateOf(false) }

    val guardado = estudiante.nivelActual != null && estudiante.nivelActual != NIVEL_RETIRADO &&
        estudiante.sexo != null && estudiante.muestra != null && edadValida(estudiante.edad) &&
        sexo == estudiante.sexo && edad.trim() == estudiante.edad.trim() && muestra == estudiante.muestra &&
        nivelElegido == null
    val puedeGuardar = nivelElegido != null && sexo != null && edadValida(edad) && muestra != null

    val borde by animateColorAsState(if (guardado) EnadInk.copy(alpha = 0.33f) else EnadLine, tween(450), label = "borde")
    val fondo by animateColorAsState(
        if (guardado) EnadInk.copy(alpha = 0.03f).compositeOver(EnadSurface) else EnadSurface, tween(450), label = "fondo"
    )

    Surface(shape = PanelShape, color = fondo, border = BorderStroke(2.dp, borde), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable(onClick = onAlternar)) {
                Text(
                    "${estudiante.numero}  ${estudiante.nombre}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                AnimatedVisibility(visible = guardado, enter = scaleIn(tween(450))) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = EnadInk)
                }
                IconButton(onClick = onAlternar) {
                    Icon(
                        if (expandida) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Mostrar datos del estudiante",
                        tint = if (expandida) EnadPrimary else EnadInk
                    )
                }
            }
            estudiante.nivelActual?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
            // BQ tipo 2: cuántos días lleva sin evaluarse; si pasó el límite se resalta
            dias?.let { d ->
                val atrasado = UltimaEvaluacion.pendiente(d)
                Text(
                    text = if (d <= 1) UltimaEvaluacion.texto(d).replaceFirstChar { it.uppercase() }
                    else "Última evaluación ${UltimaEvaluacion.texto(d)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (atrasado) EnadPendienteText else EnadSoftInk,
                    modifier = if (atrasado) {
                        Modifier.padding(top = 8.dp).background(EnadPendienteBg, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)
                    } else {
                        Modifier.padding(top = 8.dp)
                    }
                )
            }
            TextButton(onClick = onVerPerfil) {
                Icon(Icons.Outlined.History, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ver perfil", style = MaterialTheme.typography.labelMedium, color = EnadInk)
            }

            if (expandida) {
                Spacer(Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Sexo *",
                    valor = when (sexo) { "F" -> "Femenino"; "M" -> "Masculino"; else -> "" },
                    opciones = listOf("F" to "Femenino", "M" to "Masculino"),
                    onSeleccionar = { sexo = it }
                )
                Spacer(Modifier.height(16.dp))
                EnadTextField(
                    value = edad,
                    onValueChange = { nuevo -> edad = nuevo.filter(Char::isDigit).take(2) },
                    label = "Edad (5–18 años) *",
                    isError = edad.isNotEmpty() && !edadValida(edad),
                    supportingText = if (edad.isNotEmpty() && !edadValida(edad)) "La edad debe estar entre 5 y 18 años." else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(Modifier.height(16.dp))
                Desplegable(
                    etiqueta = "Muestra *",
                    valor = muestra?.let { "Muestra $it" }.orEmpty(),
                    opciones = (1..4).map { it.toString() to "Muestra $it" },
                    onSeleccionar = { muestra = it.toInt() }
                )
                Spacer(Modifier.height(18.dp))
                EtiquetaSeccion("NIVEL ALCANZADO")
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    materia.niveles.forEach { nivel ->
                        ChipNivel(nivel, seleccionado = nivelElegido == nivel.nombre) {
                            nivelElegido = if (nivelElegido == nivel.nombre) null else nivel.nombre
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                if (nivelElegido != null) {
                    TextButton(onClick = { nivelElegido = null }) { Text("Quitar selección", color = EnadInk) }
                }
                EnadPrimaryButton(
                    text = "Guardar evaluación",
                    enabled = puedeGuardar,
                    onClick = {
                        val nivel = nivelElegido ?: return@EnadPrimaryButton
                        onGuardar(nivel, sexo ?: return@EnadPrimaryButton, edad.trim().toInt(), muestra ?: return@EnadPrimaryButton)
                        nivelElegido = null
                    }
                )
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = { confirmarRetiro = true }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Retirar estudiante", style = MaterialTheme.typography.labelMedium, color = EnadInk)
                }
            }
        }
    }

    if (confirmarRetiro) {
        AlertDialog(
            onDismissRequest = { confirmarRetiro = false },
            containerColor = EnadSurface,
            shape = PanelShape,
            title = { Text("¿Retirar al estudiante?", style = MaterialTheme.typography.titleLarge) },
            text = {
                Text(
                    "${estudiante.nombre} dejará de aparecer en las listas activas de todas las áreas. Su historial se conserva.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            dismissButton = { TextButton(onClick = { confirmarRetiro = false }) { Text("Cancelar", color = EnadInk) } },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarRetiro = false
                        onRetirar()
                    },
                    shape = ControlShape,
                    colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary, contentColor = EnadSurface)
                ) { Text("Retirar estudiante") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun Desplegable(
    etiqueta: String,
    valor: String,
    opciones: List<Pair<String, String>>,
    onSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var abierto by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = abierto, onExpandedChange = { abierto = it }, modifier = modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            label = { Text(etiqueta) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = abierto) },
            shape = ControlShape,
            colors = enadFieldColors(),
            textStyle = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(expanded = abierto, onDismissRequest = { abierto = false }) {
            opciones.forEach { (clave, texto) ->
                DropdownMenuItem(text = { Text(texto) }, onClick = {
                    onSeleccionar(clave)
                    abierto = false
                })
            }
        }
    }
}

@Composable
internal fun EtiquetaSeccion(texto: String) {
    Text(texto, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp))
}

/** Chip de curso: el seleccionado lleva fondo gris y check, como ChoiceChip en Flutter. */
@Composable
internal fun ChipCurso(texto: String, seleccionado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.bodyMedium.copy(color = EnadInk)) },
        leadingIcon = if (seleccionado) ({ Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }) else null,
        shape = RoundedCornerShape(50),
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = seleccionado, borderColor = EnadLine, selectedBorderColor = EnadNeutral),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = EnadSurface,
            selectedContainerColor = EnadNeutral,
            selectedLeadingIconColor = EnadInk
        ),
        modifier = Modifier.heightIn(min = 40.dp)
    )
}

@Composable
private fun ChipNivel(nivel: NivelConfig, seleccionado: Boolean, onClick: () -> Unit) {
    val texto = textoSobreNivel(nivel.color)
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(nivel.nombre, style = MaterialTheme.typography.labelMedium, color = texto) },
        leadingIcon = if (seleccionado) ({ Icon(Icons.Filled.Check, contentDescription = null, tint = texto, modifier = Modifier.size(18.dp)) }) else null,
        shape = RoundedCornerShape(50),
        border = BorderStroke(if (seleccionado) 2.dp else 1.dp, if (seleccionado) EnadInk else nivel.color),
        colors = FilterChipDefaults.filterChipColors(containerColor = nivel.color, selectedContainerColor = nivel.color)
    )
}

/** Insignia de grupo ("Sin evaluar", "Palabra"...) con el color de su nivel. */
@Composable
internal fun InsigniaNivel(nombre: String, materia: MateriaConfig) {
    val color = materia.niveles.find { it.nombre == nombre }?.color ?: EnadNeutral
    Box(Modifier.background(color, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(nombre, style = MaterialTheme.typography.labelMedium, color = textoSobreNivel(color))
    }
}

@Composable
private fun AgregarEstudianteDialog(
    estudiantes: List<EstudianteClasificacion>,
    onElegir: (EstudianteClasificacion) -> Unit,
    onCerrar: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val resultados = estudiantes.filter { busqueda.isBlank() || it.nombre.contains(busqueda.trim(), ignoreCase = true) }
    Dialog(onDismissRequest = onCerrar) {
        Surface(shape = PanelShape, color = EnadSurface) {
            Column(Modifier.padding(24.dp)) {
                Text("Agregar estudiante a la sesión", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    placeholder = { Text("Buscar", color = EnadSoftInk) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = EnadPrimary) },
                    singleLine = true,
                    shape = ControlShape,
                    colors = enadFieldColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Column(Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                    if (resultados.isEmpty()) {
                        Text("Ningún estudiante coincide.", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    resultados.forEach { e ->
                        Column(Modifier.fillMaxWidth().clickable { onElegir(e) }.padding(vertical = 10.dp)) {
                            Text(e.nombre, style = MaterialTheme.typography.bodyLarge)
                            Text("Grado ${e.grado}", style = MaterialTheme.typography.bodyMedium)
                        }
                        HorizontalDivider(color = EnadLine)
                    }
                }
                Spacer(Modifier.height(16.dp))
                EnadSecondaryButton("Cancelar", onClick = onCerrar)
            }
        }
    }
}

@Composable
private fun ClasificacionPreview(materia: MateriaConfig) {
    EnadMovilTheme {
        ClasificacionContenido(
            materia = materia,
            estado = ClasificacionUiState(cargando = false, momento = "inicial", estudiantes = estudiantesClasificacionDemo()),
            onBack = {},
            onVerPerfil = {},
            onSeleccionarInstitucion = {},
            onSeleccionarGrado = {},
            onSeleccionarMomento = {},
            onReintentar = {},
            onEvaluar = { _, _, _, _, _, _ -> },
            onRetirar = { _, _ -> }
        )
    }
}

@Preview(showBackground = true, name = "Lectura")
@Composable
private fun ClasificacionLecturaPreview() = ClasificacionPreview(MATERIA_LECTURA)

@Preview(showBackground = true, name = "Matemáticas")
@Composable
private fun ClasificacionMatematicasPreview() = ClasificacionPreview(MATERIA_MATEMATICAS)
