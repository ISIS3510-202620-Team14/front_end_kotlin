package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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

// Paso 2/6: chips de filtro "CURSO" (Todos / Grado 3 / Grado 4 / Grado 5).
// Todavía sin selector de día ni lista de estudiantes.
private val CURSOS_DISPONIBLES = listOf("Todos", "Grado 3", "Grado 4", "Grado 5")

@Composable
fun AsistenciaScreen(
    profesorNombre: String = "Mateo",
    grupoSubtitulo: String = "Antonia Santos · Grado 5",
    onCambiarUsuario: () -> Unit = {}
) {
    var cursoSeleccionado by remember { mutableStateOf(CURSOS_DISPONIBLES.first()) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AsistenciaTopBar(profesorNombre, onCambiarUsuario) },
        bottomBar = { AsistenciaBottomBar() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp)
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
