package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.enad.enadmovil.ui.theme.EnadBorder
import com.enad.enadmovil.ui.theme.EnadHeader
import com.enad.enadmovil.ui.theme.EnadHeaderChip
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadNoAsistioBg
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText
import com.enad.enadmovil.ui.theme.EnadPillBg
import com.enad.enadmovil.ui.theme.EnadPillText
import com.enad.enadmovil.ui.theme.EnadTrack

private val CURSOS_DISPONIBLES = listOf("Todos", "Grado 3", "Grado 4", "Grado 5")

data class DiaSemana(val abreviatura: String, val numero: Int, val nombreLargo: String)

private val DIAS_SEMANA = listOf(
    DiaSemana("Lun", 1, "Lunes"),
    DiaSemana("Mar", 2, "Martes"),
    DiaSemana("Mié", 3, "Miércoles"),
    DiaSemana("Jue", 4, "Jueves"),
    DiaSemana("Vie", 5, "Viernes")
)

enum class EstadoAsistencia { PENDIENTE, ASISTIO, NO_ASISTIO }

data class EstudianteAsistencia(
    val numero: Int,
    val nombre: String,
    val etiqueta: String,
    val estado: EstadoAsistencia = EstadoAsistencia.PENDIENTE
)

// Datos de ejemplo, en memoria. Sin conexión a datos reales todavía.
private fun estudiantesDemo(): List<EstudianteAsistencia> = listOf(
    EstudianteAsistencia(1, "María López Quintero", "F"),
    EstudianteAsistencia(2, "Juan Carlos Cruz", "M"),
    EstudianteAsistencia(3, "Sofía Ramírez Toro", "F"),
    EstudianteAsistencia(4, "Andrés Felipe Gómez", "M"),
    EstudianteAsistencia(5, "Valentina Ríos Peña", "F"),
    EstudianteAsistencia(6, "Santiago Herrera Vargas", "M"),
    EstudianteAsistencia(7, "Camila Torres Duarte", "F"),
    EstudianteAsistencia(8, "Nicolás Pardo Salazar", "M")
)

// Paso 8: lista ficticia que simula el resultado de un "escaneo". No usa cámara ni OCR real.
private val LISTA_ESCANEADA_DEMO = listOf(
    "Laura Jiménez Rico",
    "Diego Alejandro Mora",
    "Isabella Castro Niño",
    "Samuel Rojas Beltrán",
    "Mariana Gil Escobar",
    "Tomás Restrepo Silva"
)

// Paso 9: roster completo del salón (varios grados) para buscar un "estudiante inesperado".
data class EstudianteSalon(val id: Int, val nombre: String, val grado: String)

private val SALON_COMPLETO = listOf(
    EstudianteSalon(1, "María López Quintero", "Grado 3"),
    EstudianteSalon(2, "Juan Carlos Cruz", "Grado 3"),
    EstudianteSalon(3, "Juan Carlos Cruz", "Grado 4"),
    EstudianteSalon(4, "Lucía Restrepo", "Grado 4"),
    EstudianteSalon(5, "Mateo Salcedo Rendón", "Grado 3"),
    EstudianteSalon(6, "Daniela Peña Ochoa", "Grado 4"),
    EstudianteSalon(7, "Emmanuel Cárdenas Ruiz", "Grado 5"),
    EstudianteSalon(8, "Gabriela Muñoz Serna", "Grado 5")
)

@Composable
fun AsistenciaScreen(
    profesorNombre: String = "Mateo",
    grupoSubtitulo: String = "Antonia Santos · Grado 5",
    onCambiarUsuario: () -> Unit = {},
    onTabClick: (String) -> Unit = {}
) {
    var cursoSeleccionado by remember { mutableStateOf(CURSOS_DISPONIBLES.first()) }
    var diaSeleccionado by remember { mutableStateOf(DIAS_SEMANA[1]) }
    var estudiantes by remember { mutableStateOf(estudiantesDemo()) }
    var mostrarConfirmacionGuardado by remember { mutableStateOf(false) }
    var mostrarEscaner by remember { mutableStateOf(false) }
    var mostrarInesperado by remember { mutableStateOf(false) }

    fun actualizarEstado(numero: Int, nuevoEstado: EstadoAsistencia) {
        estudiantes = estudiantes.map { estudiante ->
            if (estudiante.numero == numero) estudiante.copy(estado = nuevoEstado) else estudiante
        }
    }

    val asistieron = estudiantes.count { it.estado == EstadoAsistencia.ASISTIO }
    val noAsistieron = estudiantes.count { it.estado == EstadoAsistencia.NO_ASISTIO }
    val sinRegistrar = estudiantes.count { it.estado == EstadoAsistencia.PENDIENTE }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AsistenciaTopBar(profesorNombre, onCambiarUsuario) },
        bottomBar = { AsistenciaBottomBar(onTabClick) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 20.dp)
        ) {
            Text(
                text = "Asistencia",
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = grupoSubtitulo, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CURSO",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            CursoFiltroChips(
                opciones = CURSOS_DISPONIBLES,
                seleccionado = cursoSeleccionado,
                onSeleccionar = { cursoSeleccionado = it }
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DÍA",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            DiaSelector(
                dias = DIAS_SEMANA,
                seleccionado = diaSeleccionado,
                onSeleccionar = { diaSeleccionado = it }
            )

            Spacer(modifier = Modifier.height(24.dp))

            AsistenciaListaHeader(
                totalEstudiantes = estudiantes.size,
                fechaLarga = "${diaSeleccionado.nombreLargo} ${diaSeleccionado.numero} de septiembre",
                onInesperadoClick = { mostrarInesperado = true }
            )

            Spacer(modifier = Modifier.height(14.dp))

            ResumenAsistenciaBar(
                asistieron = asistieron,
                noAsistieron = noAsistieron,
                sinRegistrar = sinRegistrar
            )

            Spacer(modifier = Modifier.height(18.dp))

            estudiantes.forEach { estudiante ->
                EstudianteCard(
                    estudiante = estudiante,
                    onAsistioClick = { actualizarEstado(estudiante.numero, EstadoAsistencia.ASISTIO) },
                    onNoAsistioClick = { actualizarEstado(estudiante.numero, EstadoAsistencia.NO_ASISTIO) }
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = {
                        estudiantes = estudiantes.map { it.copy(estado = EstadoAsistencia.PENDIENTE) }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, EnadBorder),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
                ) {
                    Text(text = "Limpiar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { mostrarConfirmacionGuardado = true },
                    enabled = sinRegistrar == 0,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(text = "Guardar asistencia", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { mostrarEscaner = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EnadBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(text = "Cargar lista con escáner", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Escáner de demostración: permite revisar e importar una lista ficticia. No usa cámara ni OCR.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (mostrarInesperado) {
        EstudianteInesperadoDialog(
            roster = SALON_COMPLETO,
            onSeleccionar = { alumno ->
                estudiantes = estudiantes + EstudianteAsistencia(
                    numero = estudiantes.size + 1,
                    nombre = alumno.nombre,
                    etiqueta = "—"
                )
                mostrarInesperado = false
            },
            onCrear = { nombre ->
                estudiantes = estudiantes + EstudianteAsistencia(
                    numero = estudiantes.size + 1,
                    nombre = nombre,
                    etiqueta = "—"
                )
                mostrarInesperado = false
            },
            onDismiss = { mostrarInesperado = false }
        )
    }

    if (mostrarEscaner) {
        AlertDialog(
            onDismissRequest = { mostrarEscaner = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = "Lista escaneada (demo)", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Se \"detectaron\" ${LISTA_ESCANEADA_DEMO.size} estudiantes. Revisa antes de importar.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LISTA_ESCANEADA_DEMO.forEach { nombre ->
                        Text(
                            text = "• $nombre",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    estudiantes = LISTA_ESCANEADA_DEMO.mapIndexed { index, nombre ->
                        EstudianteAsistencia(numero = index + 1, nombre = nombre, etiqueta = "—")
                    }
                    mostrarEscaner = false
                }) {
                    Text(text = "Importar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarEscaner = false }) {
                    Text(text = "Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (mostrarConfirmacionGuardado) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmacionGuardado = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp),
            title = {
                Text(text = "Asistencia guardada", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Demostración: los datos no se envían a ningún servidor todavía.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { mostrarConfirmacionGuardado = false }) {
                    Text(text = "Cerrar", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun EstudianteCard(
    estudiante: EstudianteAsistencia,
    onAsistioClick: () -> Unit,
    onNoAsistioClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            Text(text = estudiante.etiqueta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(modifier = Modifier.height(8.dp))

        EstadoPill(estudiante.estado)

        Spacer(modifier = Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            val asistioSeleccionado = estudiante.estado == EstadoAsistencia.ASISTIO
            val noAsistioSeleccionado = estudiante.estado == EstadoAsistencia.NO_ASISTIO

            OutlinedButton(
                onClick = onAsistioClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (asistioSeleccionado) Color.Transparent else EnadBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (asistioSeleccionado) EnadPillBg else Color.Transparent,
                    contentColor = if (asistioSeleccionado) EnadPillText else MaterialTheme.colorScheme.onBackground
                )
            ) {
                Text(text = "✓ Asistió", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onNoAsistioClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, if (noAsistioSeleccionado) Color.Transparent else EnadBorder),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (noAsistioSeleccionado) EnadNoAsistioBg else Color.Transparent,
                    contentColor = if (noAsistioSeleccionado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            ) {
                Text(text = "✗ No asistió", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EstudianteInesperadoDialog(
    roster: List<EstudianteSalon>,
    onSeleccionar: (EstudianteSalon) -> Unit,
    onCrear: (nombre: String) -> Unit,
    onDismiss: () -> Unit
) {
    var busqueda by remember { mutableStateOf("") }
    val resultados = remember(busqueda) {
        if (busqueda.isBlank()) roster else roster.filter { it.nombre.contains(busqueda, ignoreCase = true) }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Estudiante inesperado",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                    shape = RoundedCornerShape(10.dp),
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
                        shape = RoundedCornerShape(10.dp),
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

// El campo "Edad" se captura por fidelidad visual con el mockup, pero todavía no se
// guarda en ningún lado (EstudianteAsistencia no tiene ese campo). El "código
// provisional" es solo texto explicativo, no genera un código real todavía.
@Composable
private fun CrearEstudianteInesperadoForm(
    busqueda: String,
    onCancelar: () -> Unit,
    onCrear: (nombre: String) -> Unit
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
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(10.dp))
    OutlinedTextField(
        value = edad,
        onValueChange = { nuevo -> edad = nuevo.filter { it.isDigit() } },
        label = { Text("Edad") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    )
    Spacer(modifier = Modifier.height(16.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = onCancelar,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, EnadBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
        ) {
            Text(text = "Cancelar", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { onCrear(nombreCompleto) },
            enabled = nombreCompleto.isNotBlank(),
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
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

@Composable
private fun EstadoPill(estado: EstadoAsistencia) {
    val (texto, fondo, textoColor) = when (estado) {
        EstadoAsistencia.PENDIENTE -> Triple("Pendiente", EnadPendienteBg, EnadPendienteText)
        EstadoAsistencia.ASISTIO -> Triple("Presente", EnadPillBg, EnadPillText)
        EstadoAsistencia.NO_ASISTIO -> Triple("No asistió", EnadNoAsistioBg, MaterialTheme.colorScheme.primary)
    }
    Box(
        modifier = Modifier
            .background(fondo, RoundedCornerShape(20.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(text = texto, fontSize = 12.sp, color = textoColor, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun AsistenciaListaHeader(
    totalEstudiantes: Int,
    fechaLarga: String,
    onInesperadoClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = totalEstudiantes.toString(),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "Asistencia por estudiante",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(text = fechaLarga, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                .clickable(onClick = onInesperadoClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "+ Inesperado",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ResumenAsistenciaBar(asistieron: Int, noAsistieron: Int, sinRegistrar: Int) {
    val total = asistieron + noAsistieron + sinRegistrar
    val porcentaje = if (total > 0) asistieron * 100 / total else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$asistieron asistieron · $noAsistieron no · $sinRegistrar sin registrar",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "$porcentaje%",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(EnadTrack, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (porcentaje / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
private fun DiaSelector(dias: List<DiaSemana>, seleccionado: DiaSemana, onSeleccionar: (DiaSemana) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        dias.forEach { dia ->
            val estaSeleccionado = dia == seleccionado
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .background(
                        color = if (estaSeleccionado) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(12.dp)
                    )
                    .let {
                        if (estaSeleccionado) it else it.border(1.dp, EnadBorder, RoundedCornerShape(12.dp))
                    }
                    .clickable { onSeleccionar(dia) }
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = dia.abreviatura,
                    fontSize = 11.sp,
                    color = if (estaSeleccionado) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = dia.numero.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (estaSeleccionado) Color.White else MaterialTheme.colorScheme.onBackground
                )
            }
        }
    }
}

@Composable
private fun CursoFiltroChips(opciones: List<String>, seleccionado: String, onSeleccionar: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        opciones.forEach { opcion ->
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

@Composable
private fun AsistenciaTopBar(profesorNombre: String, onCambiarUsuario: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(EnadHeader)
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = "ENAd", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "Móvil", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
        Text(
            text = "Portal\ndocente",
            fontSize = 11.sp,
            color = Color(0xFFCFC7BB),
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(text = profesorNombre, fontSize = 13.sp, color = Color.White, modifier = Modifier.padding(end = 12.dp))
        Box(
            modifier = Modifier
                .background(EnadHeaderChip, RoundedCornerShape(8.dp))
                .clickable(onClick = onCambiarUsuario)
                .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
            Text(text = "Cambiar\nusuario", fontSize = 11.sp, color = Color.White)
        }
    }
}

@Composable
private fun AsistenciaBottomBar(onTabClick: (String) -> Unit = {}) {
    data class Tab(val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector, val selected: Boolean)

    val tabs = listOf(
        Tab("Hoy", Icons.Filled.CalendarToday, false),
        Tab("Mi lista", Icons.Filled.ListAlt, true),
        Tab("Grupos", Icons.Filled.Groups, false),
        Tab("Horas", Icons.Filled.Schedule, false),
        Tab("Mis datos", Icons.Filled.Person, false)
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.selected,
                onClick = { onTabClick(tab.label) },
                icon = { Icon(imageVector = tab.icon, contentDescription = tab.label) },
                label = { Text(text = tab.label, fontSize = 11.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AsistenciaScreenPreview() {
    EnadMovilTheme {
        AsistenciaScreen()
    }
}
