package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.ui.theme.EnadBorder
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadPillBg
import com.enad.enadmovil.ui.theme.EnadPillText

// Paso 1: esqueleto (flecha atrás + título + subtítulo). "materia" parametriza la
// pantalla para que Lectura y Matemáticas la compartan (título + niveles propios).
// Paso 6: se agregan los niveles con su color, y el nivel especial "Retirado".
data class NivelConfig(val nombre: String, val color: Color)
// "clave" es la materia tal como la nombra el backend en levels (lectura | matematicas).
data class MateriaConfig(val titulo: String, val clave: String, val niveles: List<NivelConfig> = emptyList())

private const val NIVEL_RETIRADO = "Retirado"
private val COLOR_RETIRADO = Color(0xFF8A8378)

val MATERIA_LECTURA = MateriaConfig(
    titulo = "Lectura",
    clave = "lectura",
    niveles = listOf(
        NivelConfig("Principiante", Color(0xFF3D6FEF)),
        NivelConfig("Letra", Color(0xFF45B8D6)),
        NivelConfig("Palabra", Color(0xFF43A047)),
        NivelConfig("Párrafo", Color(0xFFE08A3B)),
        NivelConfig("Cuento", Color(0xFFD9553F)),
        NivelConfig("Comprensión", Color(0xFF8E6FD6))
    )
)

val MATERIA_MATEMATICAS = MateriaConfig(
    titulo = "Matemáticas",
    clave = "matematicas",
    niveles = listOf(
        NivelConfig("Principiante", Color(0xFF3D6FEF)),
        NivelConfig("1 dígito", Color(0xFF45B8D6)),
        NivelConfig("2 dígitos", Color(0xFF43A047)),
        NivelConfig("Resta", Color(0xFFE08A3B)),
        NivelConfig("División", Color(0xFFD9553F)),
        NivelConfig("Problema escrito", Color(0xFF8E6FD6))
    )
)

// Chips "CURSO DE ORIGEN": grado -> etiqueta (null = Todos). Son los grados que maneja ENAd (3 a 5).
private val CLASIFICACION_CURSOS = listOf<Pair<Int?, String>>(
    null to "Todos", 3 to "Grado 3", 4 to "Grado 4", 5 to "Grado 5"
)

// Estudiante de la institución elegida. "sexo" y "edad" son editables desde la tarjeta expandida;
// "nivelActual" null = Pendiente, si no = Evaluado en ese nivel.
data class EstudianteClasificacion(
    val id: String,
    val numero: Int,
    val nombre: String,
    val grado: Int,
    val sexo: String = "F",
    val edad: String = "",
    val nivelActual: String? = null
)

private fun estudiantesClasificacionDemo(): List<EstudianteClasificacion> = listOf(
    EstudianteClasificacion("demo-1", 1, "María López Quintero", grado = 3, sexo = "F"),
    EstudianteClasificacion("demo-2", 2, "Juan Carlos Cruz", grado = 3, sexo = "M"),
    EstudianteClasificacion("demo-3", 3, "Lucía Restrepo", grado = 4, sexo = "F"),
    EstudianteClasificacion("demo-4", 4, "Valentina Ríos Peña", grado = 4, sexo = "F"),
    EstudianteClasificacion("demo-5", 5, "Andrés Mejía", grado = 5, sexo = "M")
)

@Composable
fun ClasificacionScreen(
    materia: MateriaConfig,
    onBack: () -> Unit = {},
    viewModel: ClasificacionViewModel = viewModel(key = "clasificacion-${materia.clave}")
) {
    LaunchedEffect(materia) { viewModel.iniciar(materia) }
    val estado by viewModel.uiState.collectAsStateWithLifecycle()

    ClasificacionContenido(
        materia = materia,
        estado = estado,
        onBack = onBack,
        onSeleccionarInstitucion = viewModel::seleccionarInstitucion,
        onSeleccionarGrado = viewModel::seleccionarGrado,
        onReintentar = viewModel::reintentar,
        onActualizar = viewModel::actualizar
    )
}

@Composable
private fun ClasificacionContenido(
    materia: MateriaConfig,
    estado: ClasificacionUiState,
    onBack: () -> Unit,
    onSeleccionarInstitucion: (String) -> Unit,
    onSeleccionarGrado: (Int?) -> Unit,
    onReintentar: () -> Unit,
    onActualizar: (String, (EstudianteClasificacion) -> EstudianteClasificacion) -> Unit
) {
    val estudiantes = estado.visibles
    var expandidoId by remember { mutableStateOf<String?>(null) }

    val grupos = buildList {
        val sinEvaluar = estudiantes.filter { it.nivelActual == null }
        if (sinEvaluar.isNotEmpty()) add("Sin evaluar" to sinEvaluar)
        (materia.niveles.map { it.nombre } + NIVEL_RETIRADO).forEach { nombreNivel ->
            val delNivel = estudiantes.filter { it.nivelActual == nombreNivel }
            if (delNivel.isNotEmpty()) add(nombreNivel to delNivel)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(horizontal = 20.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = materia.titulo,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${estudiantes.size} de ${estado.estudiantes.size} estudiantes activos",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (estado.instituciones.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                TituloSeccion("INSTITUCIÓN")
                Spacer(modifier = Modifier.height(10.dp))
                FiltroChips<String?>(
                    opciones = estado.instituciones.map { it.id to it.name },
                    seleccionado = estado.institucionId,
                    onSeleccionar = { id -> id?.let(onSeleccionarInstitucion) }
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            TituloSeccion("CURSO DE ORIGEN")
            Spacer(modifier = Modifier.height(10.dp))
            FiltroChips(
                opciones = CLASIFICACION_CURSOS,
                seleccionado = estado.grado,
                onSeleccionar = onSeleccionarGrado
            )

            when {
                estado.cargando -> Box(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }

                estado.mensaje != null -> Column(modifier = Modifier.padding(top = 32.dp)) {
                    Text(text = estado.mensaje, fontSize = 14.sp, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onReintentar) { Text("Reintentar") }
                }

                estudiantes.isEmpty() -> Text(
                    text = if (estado.grado == null) "Esta institución aún no tiene estudiantes."
                    else "No hay estudiantes en este curso.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 32.dp)
                )
            }

            if (!estado.cargando && estado.mensaje == null) grupos.forEach { (nombreGrupo, lista) ->
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "${nombreGrupo.uppercase()} · ${lista.size}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                lista.forEach { estudiante ->
                    EstudianteClasificacionCard(
                        estudiante = estudiante,
                        materia = materia,
                        expandido = expandidoId == estudiante.id,
                        onToggleExpand = {
                            expandidoId = if (expandidoId == estudiante.id) null else estudiante.id
                        },
                        onSexoChange = { sexo -> onActualizar(estudiante.id) { it.copy(sexo = sexo) } },
                        onEdadChange = { edad -> onActualizar(estudiante.id) { it.copy(edad = edad) } },
                        onNivelSeleccionado = { nivel -> onActualizar(estudiante.id) { it.copy(nivelActual = nivel) } }
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
    }
}

// Paso 4: acordeón (solo una tarjeta abierta a la vez).
// Paso 5: dentro de la tarjeta expandida, dropdown "Sexo" + campo "Edad".
// Paso 6: pill "Evaluado" + nivel, y chips de "NIVEL ALCANZADO" (marcan al instante).
@Composable
private fun EstudianteClasificacionCard(
    estudiante: EstudianteClasificacion,
    materia: MateriaConfig,
    expandido: Boolean,
    onToggleExpand: () -> Unit,
    onSexoChange: (String) -> Unit,
    onEdadChange: (String) -> Unit,
    onNivelSeleccionado: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggleExpand),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${estudiante.numero}  ${estudiante.nombre}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expandido) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = null,
                tint = if (expandido) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val nivelActual = estudiante.nivelActual
            if (nivelActual == null) {
                PillGenerico("Pendiente", EnadPendienteBg, EnadPendienteText)
            } else {
                PillGenerico("Evaluado", EnadPillBg, EnadPillText)
                PillGenerico(nivelActual, nivelColorDe(materia, nivelActual), Color.White)
            }
        }

        if (expandido) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                SexoDropdown(
                    valor = estudiante.sexo,
                    onSeleccionar = onSexoChange,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = estudiante.edad,
                    onValueChange = { nuevo -> onEdadChange(nuevo.filter { it.isDigit() }) },
                    label = { Text("Edad") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "NIVEL ALCANZADO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                materia.niveles.forEach { nivel ->
                    NivelChip(
                        nivel = nivel,
                        seleccionado = estudiante.nivelActual == nivel.nombre,
                        onClick = { onNivelSeleccionado(nivel.nombre) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            NivelChip(
                nivel = NivelConfig(NIVEL_RETIRADO, COLOR_RETIRADO),
                seleccionado = estudiante.nivelActual == NIVEL_RETIRADO,
                onClick = { onNivelSeleccionado(NIVEL_RETIRADO) }
            )
        }
    }
}

private fun nivelColorDe(materia: MateriaConfig, nombreNivel: String): Color =
    materia.niveles.find { it.nombre == nombreNivel }?.color ?: COLOR_RETIRADO

@Composable
private fun NivelChip(nivel: NivelConfig, seleccionado: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(
                color = if (seleccionado) nivel.color else nivel.color.copy(alpha = 0.16f),
                shape = RoundedCornerShape(20.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = nivel.nombre,
            fontSize = 13.sp,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Medium,
            color = if (seleccionado) Color.White else nivel.color
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SexoDropdown(valor: String, onSeleccionar: (String) -> Unit, modifier: Modifier = Modifier) {
    var expandido by remember { mutableStateOf(false) }
    val opciones = listOf("F", "M")

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text("Sexo") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccionar(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PillGenerico(texto: String, fondo: Color, textoColor: Color) {
    Box(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = texto, fontSize = 12.sp, color = textoColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TituloSeccion(texto: String) {
    Text(
        text = texto,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Chips de filtro (mismo patrón visual que en Asistencia). [opciones]: valor -> etiqueta.
@Composable
private fun <T> FiltroChips(opciones: List<Pair<T, String>>, seleccionado: T, onSeleccionar: (T) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { (valor, etiqueta) ->
            val estaSeleccionado = valor == seleccionado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = if (estaSeleccionado) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .let {
                        if (estaSeleccionado) it else it.border(1.dp, EnadBorder, RoundedCornerShape(20.dp))
                    }
                    .clickable { onSeleccionar(valor) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (estaSeleccionado) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = etiqueta,
                    fontSize = 13.sp,
                    fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Normal,
                    color = if (estaSeleccionado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        }
    }
}

@Composable
private fun ClasificacionPreview(materia: MateriaConfig) {
    EnadMovilTheme {
        ClasificacionContenido(
            materia = materia,
            estado = ClasificacionUiState(
                cargando = false,
                instituciones = listOf(
                    CloudFunctionsApi.Institucion("ie-1", "IE Rural El Carmen", null, emptyList()),
                    CloudFunctionsApi.Institucion("ie-2", "IE San José", null, emptyList())
                ),
                institucionId = "ie-1",
                estudiantes = estudiantesClasificacionDemo()
            ),
            onBack = {},
            onSeleccionarInstitucion = {},
            onSeleccionarGrado = {},
            onReintentar = {},
            onActualizar = { _, _ -> }
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ClasificacionLecturaPreview() = ClasificacionPreview(MATERIA_LECTURA)

@Preview(showBackground = true)
@Composable
private fun ClasificacionMatematicasPreview() = ClasificacionPreview(MATERIA_MATEMATICAS)
