package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.feature.grupos.CrearGrupoScreen
import com.enad.enadmovil.feature.grupos.EditarGrupoScreen
import com.enad.enadmovil.feature.grupos.GruposScreen
import com.enad.enadmovil.feature.grupos.GruposViewModel
import com.enad.enadmovil.feature.grupos.VerGrupoScreen
import com.enad.enadmovil.ui.theme.EnadHeader
import com.enad.enadmovil.ui.theme.EnadHeaderChip
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadTrack

data class ProgressItem(val title: String, val percent: Int, val evaluatedText: String)
data class ActionItem(val title: String, val subtitle: String)
data class BottomTab(val label: String, val icon: ImageVector, val selected: Boolean)

private sealed class SubPantallaGrupos {
    data object Lista : SubPantallaGrupos()
    data object Crear : SubPantallaGrupos()
    data class Ver(val grupoId: Int) : SubPantallaGrupos()
    data class Editar(val grupoId: Int) : SubPantallaGrupos()
}

@Composable
fun TeacherHomeScreen(
    profesorNombre: String = "Mateo",
    grupoTitulo: String = "Educación formativa",
    grupoSubtitulo: String = "Antonia Santos · Grado 5",
    progreso: List<ProgressItem> = listOf(
        ProgressItem("Lectura", 0, "0 de 8 estudiantes evaluados"),
        ProgressItem("Matemáticas", 63, "5 de 8 estudiantes evaluados")
    ),
    acciones: List<ActionItem> = listOf(
        ActionItem("Planear horas", "Organiza tu reporte semanal"),
        ActionItem("Tomar asistencia", "0 de 8 registrados")
    ),
    tabs: List<BottomTab> = listOf(
        BottomTab("Hoy", Icons.Filled.CalendarToday, true),
        BottomTab("Mi lista", Icons.Filled.ListAlt, false),
        BottomTab("Grupos", Icons.Filled.Groups, false),
        BottomTab("Horas", Icons.Filled.Schedule, false),
        BottomTab("Mis datos", Icons.Filled.Person, false)
    ),
    onCambiarUsuario: () -> Unit = {},
    onAccionClick: (ActionItem) -> Unit = {},
    onTabClick: (BottomTab) -> Unit = {},
    onProgresoClick: (ProgressItem) -> Unit = {}
) {
    val gruposViewModel: GruposViewModel = viewModel()
    val gruposUiState by gruposViewModel.uiState.collectAsStateWithLifecycle()
    var tabSeleccionado by remember { mutableStateOf(tabs.indexOfFirst { it.selected }.coerceAtLeast(0)) }
    var subPantallaGrupos by remember { mutableStateOf<SubPantallaGrupos>(SubPantallaGrupos.Lista) }

    when (val actual = subPantallaGrupos) {
        is SubPantallaGrupos.Crear -> {
            CrearGrupoScreen(
                materia = gruposUiState.subjectAreas[gruposUiState.selectedTabIndex],
                onBack = { subPantallaGrupos = SubPantallaGrupos.Lista },
                onGuardar = { nombre, ninos, docente ->
                    gruposViewModel.agregarGrupo(nombre, ninos, docente)
                    subPantallaGrupos = SubPantallaGrupos.Lista
                }
            )
            return
        }
        is SubPantallaGrupos.Ver -> {
            val grupo = gruposUiState.grupos.find { it.id == actual.grupoId }
            if (grupo != null) {
                VerGrupoScreen(
                    grupo = grupo,
                    materia = gruposUiState.subjectAreas[gruposUiState.selectedTabIndex],
                    onBack = { subPantallaGrupos = SubPantallaGrupos.Lista }
                )
                return
            } else {
                subPantallaGrupos = SubPantallaGrupos.Lista
            }
        }
        is SubPantallaGrupos.Editar -> {
            val grupo = gruposUiState.grupos.find { it.id == actual.grupoId }
            if (grupo != null) {
                EditarGrupoScreen(
                    grupo = grupo,
                    materia = gruposUiState.subjectAreas[gruposUiState.selectedTabIndex],
                    onBack = { subPantallaGrupos = SubPantallaGrupos.Lista },
                    onNombreChange = { nuevoNombre -> gruposViewModel.actualizarNombre(grupo.id, nuevoNombre) },
                    onDocenteChange = { nuevoDocente -> gruposViewModel.actualizarDocente(grupo.id, nuevoDocente) },
                    onQuitarNino = { nino -> gruposViewModel.quitarNino(grupo.id, nino) },
                    onAgregarNinos = { ninos -> gruposViewModel.agregarNinos(grupo.id, ninos) },
                    onEliminarGrupo = {
                        gruposViewModel.eliminarGrupo(grupo.id)
                        subPantallaGrupos = SubPantallaGrupos.Lista
                    }
                )
                return
            } else {
                subPantallaGrupos = SubPantallaGrupos.Lista
            }
        }
        SubPantallaGrupos.Lista -> { }
    }

    val tabsConSeleccion = tabs.mapIndexed { index, tab -> tab.copy(selected = index == tabSeleccionado) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { TeacherTopBar(profesorNombre, onCambiarUsuario) },
        bottomBar = {
            TeacherBottomBar(
                tabs = tabsConSeleccion,
                onTabClick = { tab ->
                    val index = tabsConSeleccion.indexOf(tab)
                    if (index >= 0) tabSeleccionado = index
                    onTabClick(tab)
                }
            )
        }
    ) { padding ->
        if (tabSeleccionado == 2) {
            GruposScreen(
                viewModel = gruposViewModel,
                onVerGrupo = { id -> subPantallaGrupos = SubPantallaGrupos.Ver(id) },
                onEditarGrupo = { id -> subPantallaGrupos = SubPantallaGrupos.Editar(id) },
                onCrearGrupo = { subPantallaGrupos = SubPantallaGrupos.Crear },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp)
            ) {
                Text(
                    text = grupoTitulo,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = grupoSubtitulo, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Spacer(modifier = Modifier.height(20.dp))

                progreso.forEach { item ->
                    ProgressCard(item, onClick = { onProgresoClick(item) })
                    Spacer(modifier = Modifier.height(14.dp))
                }

                acciones.forEach { accion ->
                    ActionRow(accion) { onAccionClick(accion) }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sesión de demostración · Los cambios se reinician al cambiar de usuario o cerrar la app.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TeacherTopBar(profesorNombre: String, onCambiarUsuario: () -> Unit) {
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
private fun ProgressCard(item: ProgressItem, onClick: () -> Unit = {}) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "${item.percent}%",
                fontSize = 20.sp,
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
                    .fillMaxWidth(fraction = (item.percent / 100f).coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(3.dp))
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = item.evaluatedText, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ActionRow(item: ActionItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = item.subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(text = ">", fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TeacherBottomBar(tabs: List<BottomTab>, onTabClick: (BottomTab) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = tab.selected,
                onClick = { onTabClick(tab) },
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
private fun TeacherHomeScreenPreview() {
    EnadMovilTheme {
        TeacherHomeScreen()
    }
}