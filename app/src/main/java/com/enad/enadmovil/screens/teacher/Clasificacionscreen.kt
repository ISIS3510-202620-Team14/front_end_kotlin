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
import androidx.compose.runtime.Composable
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
data class MateriaConfig(val titulo: String, val niveles: List<NivelConfig> = emptyList())

private const val NIVEL_RETIRADO = "Retirado"
private val COLOR_RETIRADO = Color(0xFF8A8378)

val MATERIA_LECTURA = MateriaConfig(
    titulo = "Lectura",
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
    niveles = listOf(
        NivelConfig("Principiante", Color(0xFF3D6FEF)),
        NivelConfig("1 dígito", Color(0xFF45B8D6)),
        NivelConfig("2 dígitos", Color(0xFF43A047)),
        NivelConfig("Resta", Color(0xFFE08A3B)),
        NivelConfig("División", Color(0xFFD9553F)),
        NivelConfig("Problema escrito", Color(0xFF8E6FD6))
    )
)

// Paso 2: chips "CURSO DE ORIGEN" (mismo patrón visual que en Asistencia). Todavía
// no filtran ninguna lista de estudiantes, porque esa lista no existe aún.
private val CLASIFICACION_CURSOS = listOf("Todos", "Grado 3", "Grado 4", "Grado 5")

// Paso 3: lista de estudiantes en modo plano. Mismos 8 estudiantes del salón usados
// en Asistencia, para que se sienta el mismo grupo.
// Paso 5: se agregan "sexo" y "edad", editables desde la tarjeta expandida.
// Paso 6: se agrega "nivelActual" (null = Pendiente, si no = Evaluado en ese nivel).
data class EstudianteClasificacion(
    val numero: Int,
    val nombre: String,
    val sexo: String = "F",
    val edad: String = "",
    val nivelActual: String? = null
)

private fun estudiantesClasificacionDemo(): List<EstudianteClasificacion> = listOf(
    EstudianteClasificacion(1, "María López Quintero", sexo = "F"),
    EstudianteClasificacion(2, "Juan Carlos Cruz", sexo = "M"),
    EstudianteClasificacion(3, "Juan Carlos Cruz", sexo = "M"),
    EstudianteClasificacion(4, "Lucía Restrepo", sexo = "F"),
    EstudianteClasificacion(5, "Valentina Ríos Peña", sexo = "F"),
    EstudianteClasificacion(6, "Sofía Betancur", sexo = "F"),
    EstudianteClasificacion(7, "Andrés Mejía", sexo = "M"),
    EstudianteClasificacion(8, "Nicolás Pardo Salazar", sexo = "M")
)

@Composable
fun ClasificacionScreen(
    materia: MateriaConfig,
    onBack: () -> Unit = {}
) {
    var cursoSeleccionado by remember { mutableStateOf(CLASIFICACION_CURSOS.first()) }
    var estudiantes by remember { mutableStateOf(estudiantesClasificacionDemo()) }
    var expandidoNumero by remember { mutableStateOf(estudiantes.firstOrNull()?.numero) }

    fun actualizarSexo(numero: Int, sexo: String) {
        estudiantes = estudiantes.map { e -> if (e.numero == numero) e.copy(sexo = sexo) else e }
    }

    fun actualizarEdad(numero: Int, edad: String) {
        estudiantes = estudiantes.map { e -> if (e.numero == numero) e.copy(edad = edad) else e }
    }

    fun actualizarNivel(numero: Int, nivel: String) {
        estudiantes = estudiantes.map { e -> if (e.numero == numero) e.copy(nivelActual = nivel) else e }
    }

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
                text = "${estudiantes.size} de ${estudiantes.size} estudiantes activos",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "CURSO DE ORIGEN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ClasificacionCursoChips(
                seleccionado = cursoSeleccionado,
                onSeleccionar = { cursoSeleccionado = it }
            )

            grupos.forEach { (nombreGrupo, lista) ->
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
                        expandido = expandidoNumero == estudiante.numero,
                        onToggleExpand = {
                            expandidoNumero = if (expandidoNumero == estudiante.numero) null else estudiante.numero
                        },
                        onSexoChange = { actualizarSexo(estudiante.numero, it) },
                        onEdadChange = { actualizarEdad(estudiante.numero, it) },
                        onNivelSeleccionado = { nivel -> actualizarNivel(estudiante.numero, nivel) }
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
private fun ClasificacionCursoChips(seleccionado: String, onSeleccionar: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CLASIFICACION_CURSOS.forEach { opcion ->
            val estaSeleccionado = opcion == seleccionado
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
                    .clickable { onSeleccionar(opcion) }
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
                    text = opcion,
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

@Preview(showBackground = true)
@Composable
private fun ClasificacionLecturaPreview() {
    EnadMovilTheme {
        ClasificacionScreen(materia = MATERIA_LECTURA)
    }
}

@Preview(showBackground = true)
@Composable
private fun ClasificacionMatematicasPreview() {
    EnadMovilTheme {
        ClasificacionScreen(materia = MATERIA_MATEMATICAS)
    }
}
