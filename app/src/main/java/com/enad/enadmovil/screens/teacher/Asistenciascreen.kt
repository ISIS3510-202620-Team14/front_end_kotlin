package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

// Paso 5/6: tarjetas de estudiante con botones "Asistió" / "No asistió".
// El resumen y el porcentaje ahora se calculan a partir del estado real de la lista.
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

@Composable
fun AsistenciaScreen(
    profesorNombre: String = "Mateo",
    grupoSubtitulo: String = "Antonia Santos · Grado 5",
    onCambiarUsuario: () -> Unit = {}
) {
    var cursoSeleccionado by remember { mutableStateOf(CURSOS_DISPONIBLES.first()) }
    var diaSeleccionado by remember { mutableStateOf(DIAS_SEMANA[1]) }
    var estudiantes by remember { mutableStateOf(estudiantesDemo()) }

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
        bottomBar = { AsistenciaBottomBar() }
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
                fechaLarga = "${diaSeleccionado.nombreLargo} ${diaSeleccionado.numero} de septiembre"
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
        }
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
            OutlinedButton(
                onClick = onAsistioClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EnadBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(text = "✓ Asistió", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
            OutlinedButton(
                onClick = onNoAsistioClick,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, EnadBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onBackground)
            ) {
                Text(text = "✗ No asistió", fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun EstadoPill(estado: EstadoAsistencia) {
    val (texto, fondo, textoColor) = when (estado) {
        EstadoAsistencia.PENDIENTE -> Triple("Pendiente", EnadPendienteBg, EnadPendienteText)
        EstadoAsistencia.ASISTIO -> Triple("Asistió", EnadPillBg, EnadPillText)
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
        Text(
            text = "+ Inesperado",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onInesperadoClick)
        )
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
private fun AsistenciaBottomBar() {
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
                onClick = {},
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
