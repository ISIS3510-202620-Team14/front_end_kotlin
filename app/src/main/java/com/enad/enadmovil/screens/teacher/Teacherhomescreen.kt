package com.enad.enadmovil.ui.screens.teacher

import com.enad.enadmovil.ui.components.EnadActionCard
import com.enad.enadmovil.ui.components.EnadHeading
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FactCheck
import androidx.compose.material.icons.outlined.Assessment
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import com.enad.enadmovil.ui.components.EnadBottomBar
import com.enad.enadmovil.ui.components.EnadTopBar
import com.enad.enadmovil.ui.components.TeacherNavItems
import android.Manifest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.feature.actividades.ActividadesScreen
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.feature.grupos.CrearGrupoScreen
import com.enad.enadmovil.feature.grupos.EditarGrupoScreen
import com.enad.enadmovil.feature.grupos.GruposScreen
import com.enad.enadmovil.feature.grupos.GruposViewModel
import com.enad.enadmovil.feature.grupos.VerGrupoScreen
import com.enad.enadmovil.feature.horas.HorasScreen
import com.enad.enadmovil.feature.misdatos.MisDatosScreen
import com.enad.enadmovil.ui.theme.EnadHeader
import com.enad.enadmovil.ui.theme.EnadHeaderChip
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import com.enad.enadmovil.ui.theme.EnadTrack
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.ExtendedFloatingActionButton
import com.enad.enadmovil.feature.ayuda.AyudaSheet
import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.feature.ayuda.LocalGuia
import com.enad.enadmovil.feature.ayuda.objetivoGuia
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadSurface

data class ProgressItem(
    val title: String,
    val percent: Int,
    val evaluatedText: String
)

// Títulos de los accesos de Hoy que resuelve AppNavigation.
const val ACCION_ASISTENCIA = "Reportar asistencia"
const val ACCION_EVALUACION = "Evaluación formativa"

data class ActionItem(
    val title: String,
    val subtitle: String
)

data class BottomTab(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean
)

// Índices del tab por defecto de TeacherHomeScreen. nav.kt los usa para poder
// mandarte directo a una pestaña específica (ej. desde Asistencia a Horas), ya que
// "tabSeleccionado" ahora vive afuera de esta pantalla (ver onTabSeleccionadoChange).
object TeacherTabs {
    const val HOY = 0
    const val MI_LISTA = 1
    const val GRUPOS = 2
    const val HORAS = 3
    const val MIS_DATOS = 4
}

private sealed class SubPantallaGrupos {
    data object Lista : SubPantallaGrupos()
    data object Crear : SubPantallaGrupos()
    data class Ver(val grupoId: Int) : SubPantallaGrupos()
    // area: pestaña de materia que debe estar cargada (cuando se abre desde Mis datos).
    data class Editar(val grupoId: Int, val area: Int? = null) : SubPantallaGrupos()
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
        ActionItem(
            "Planear actividades",
            "Usa la biblioteca o crea las tuyas"
        ),
        ActionItem(
            "Planear horas",
            "Organiza tu reporte semanal"
        ),
        ActionItem(
            "Tomar asistencia",
            "0 de 8 registrados"
        )
    ),
    tabs: List<BottomTab> = listOf(
        BottomTab("Hoy", Icons.Filled.CalendarToday, true),
        BottomTab("Asistencia", Icons.Filled.ListAlt, false),
        BottomTab("Grupos", Icons.Filled.Groups, false),
        BottomTab("Horas", Icons.Filled.Schedule, false),
        BottomTab("Mis datos", Icons.Filled.Person, false)
    ),
    tabSeleccionado: Int = tabs.indexOfFirst { it.selected }.coerceAtLeast(0),
    onTabSeleccionadoChange: (Int) -> Unit = {},
    onCambiarUsuario: () -> Unit = {},
    onGestionarEstudiantes: () -> Unit = {},
    onAccionClick: (ActionItem) -> Unit = {},
    onTabClick: (BottomTab) -> Unit = {},
    onProgresoClick: (ProgressItem) -> Unit = {}
) {
    val gruposViewModel: GruposViewModel = viewModel()
    val gruposUiState by gruposViewModel.uiState.collectAsStateWithLifecycle()
    val recomendacionAgrupacion by gruposViewModel.recomendacion.collectAsStateWithLifecycle()

    val aperturasViewModel: AperturasViewModel = viewModel()
    val aperturasState by aperturasViewModel.uiState.collectAsStateWithLifecycle()

    val contextoViewModel: ContextoHoyViewModel = viewModel()
    val contextoState by contextoViewModel.uiState.collectAsStateWithLifecycle()

    val progresoViewModel: TeacherProgressViewModel = viewModel()
    val progresoState by progresoViewModel.uiState.collectAsStateWithLifecycle()

    val pedirUbicacion =
        rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) {
            contextoViewModel.iniciar()
        }

    LaunchedEffect(Unit) {
        if (contextoViewModel.tienePermisoUbicacion()) {
            contextoViewModel.iniciar()
        } else {
            pedirUbicacion.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LifecycleResumeEffect(progresoViewModel) {
        progresoViewModel.refrescar()

        onPauseOrDispose {
        }
    }

    val tituloHoy =
        contextoState.confirmado?.nombre ?: grupoTitulo

    val subtituloHoy =
        contextoState.confirmado
            ?.let { grupo ->
                listOfNotNull(
                    grupo.sede,
                    nombreMateria(grupo.materia)
                ).joinToString(" · ")
            }
            ?: grupoSubtitulo

    var subPantallaGrupos by remember {
        mutableStateOf<SubPantallaGrupos>(
            SubPantallaGrupos.Lista
        )
    }

    var mostrarActividades by remember {
        mutableStateOf(false)
    }

    val snackbarHostState =
        remember { SnackbarHostState() }

    val scope = rememberCoroutineScope()

    val guia = LocalGuia.current
    var mostrarAyuda by remember { mutableStateOf(false) }

    if (mostrarActividades) {
        BackHandler {
            mostrarActividades = false
        }

        ActividadesScreen(
            onBack = {
                mostrarActividades = false
            },
            modifier = Modifier.fillMaxSize()
        )

        return
    }

    when (val actual = subPantallaGrupos) {
        is SubPantallaGrupos.Crear -> {
            BackHandler {
                subPantallaGrupos =
                    SubPantallaGrupos.Lista
            }

            CrearGrupoScreen(
                materia = gruposUiState
                    .subjectAreas[gruposUiState.selectedTabIndex],
                recomendacion = recomendacionAgrupacion,
                sinGrupo = gruposUiState.sinGrupo,
                onPedirRecomendacion =
                    gruposViewModel::pedirRecomendacion,
                onBack = {
                    subPantallaGrupos =
                        SubPantallaGrupos.Lista
                },
                onGuardar = {
                        nombre,
                        ninos,
                        docente,
                        cantidadNinos,
                        cantidadDocentes,
                        metodo
                    ->
                    gruposViewModel.agregarGrupo(
                        nombre,
                        ninos,
                        docente,
                        cantidadNinos,
                        cantidadDocentes,
                        metodo
                    )

                    subPantallaGrupos =
                        SubPantallaGrupos.Lista
                }
            )

            return
        }

        is SubPantallaGrupos.Ver -> {
            val grupo =
                gruposUiState.grupos.find {
                    it.id == actual.grupoId
                }

            if (grupo != null) {
                BackHandler {
                    subPantallaGrupos =
                        SubPantallaGrupos.Lista
                }

                VerGrupoScreen(
                    grupo = grupo,
                    materia = gruposUiState
                        .subjectAreas[gruposUiState.selectedTabIndex],
                    onBack = {
                        subPantallaGrupos =
                            SubPantallaGrupos.Lista
                    }
                )

                return
            } else {
                subPantallaGrupos =
                    SubPantallaGrupos.Lista
            }
        }

        is SubPantallaGrupos.Editar -> {
            val grupo =
                gruposUiState.grupos.find {
                    it.id == actual.grupoId
                }

            if (grupo != null) {
                BackHandler {
                    subPantallaGrupos =
                        SubPantallaGrupos.Lista
                }

                EditarGrupoScreen(
                    grupo = grupo,
                    materia = gruposUiState
                        .subjectAreas[gruposUiState.selectedTabIndex],
                    onBack = {
                        subPantallaGrupos =
                            SubPantallaGrupos.Lista
                    },
                    onNombreChange = { nuevoNombre ->
                        gruposViewModel.actualizarNombre(
                            grupo.id,
                            nuevoNombre
                        )
                    },
                    onDocenteChange = { nuevoDocente ->
                        gruposViewModel.actualizarDocente(
                            grupo.id,
                            nuevoDocente
                        )
                    },
                    onQuitarNino = { nino ->
                        gruposViewModel.quitarNino(
                            grupo.id,
                            nino
                        )
                    },
                    onAgregarNinos = { ninos ->
                        gruposViewModel.agregarNinos(
                            grupo.id,
                            ninos
                        )
                    },
                    onEliminarGrupo = {
                        gruposViewModel.eliminarGrupo(
                            grupo.id
                        )

                        subPantallaGrupos =
                            SubPantallaGrupos.Lista
                    }
                )

                return
            } else if (actual.area == null || gruposUiState.selectedTabIndex == actual.area) {
                // Si se pidió otra materia, se espera a que lleguen sus grupos antes de rendirse.
                subPantallaGrupos =
                    SubPantallaGrupos.Lista
            }
        }

        SubPantallaGrupos.Lista -> Unit
    }

    val tabsConSeleccion =
        tabs.mapIndexed { index, tab ->
            tab.copy(
                selected = index == tabSeleccionado
            )
        }

    Scaffold(
        containerColor =
            MaterialTheme.colorScheme.background,

        snackbarHost = {
            SnackbarHost(snackbarHostState)
        },

        topBar = {
            TeacherTopBar(
                profesorNombre,
                onCambiarUsuario
            )
        },

        bottomBar = {
            if (guia?.activa != true) {
                TeacherBottomBar(
                    tabs = tabsConSeleccion,
                    onTabClick = { tab ->
                        val index =
                            tabsConSeleccion.indexOf(tab)

                        if (index >= 0) {
                            onTabSeleccionadoChange(index)
                        }

                        onTabClick(tab)
                    }
                )
            }
        },

        floatingActionButton = {
            if (tabSeleccionado == TeacherTabs.HOY && guia?.activa != true) {
                ExtendedFloatingActionButton(
                    onClick = { mostrarAyuda = true },
                    containerColor = EnadSurface,
                    contentColor = EnadInk,
                    icon = { Icon(Icons.AutoMirrored.Outlined.HelpOutline, contentDescription = null) },
                    text = { Text("Ayuda") }
                )
            }
        }
    ) { padding ->

        if (tabSeleccionado == TeacherTabs.GRUPOS) {

            GruposScreen(
                viewModel = gruposViewModel,

                onVerGrupo = { id ->
                    subPantallaGrupos =
                        SubPantallaGrupos.Ver(id)
                },

                onEditarGrupo = { id ->
                    subPantallaGrupos =
                        SubPantallaGrupos.Editar(id)
                },

                onCrearGrupo = {
                    subPantallaGrupos =
                        SubPantallaGrupos.Crear
                },

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

        } else if (tabSeleccionado == TeacherTabs.HORAS) {

            HorasScreen(
                nombreGrupo = tituloHoy,

                onEnviarReporte = {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            "Reporte enviado."
                        )
                    }
                },

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

        } else if (tabSeleccionado == TeacherTabs.MIS_DATOS) {

            MisDatosScreen(
                onCambiarUsuario = onCambiarUsuario,
                onAbrirGrupo = { grupo ->
                    val area = if (grupo.area == AreaMateria.MATEMATICAS) 0 else 1
                    gruposViewModel.onTabSelected(area)
                    subPantallaGrupos = SubPantallaGrupos.Editar(grupo.id, area)
                },
                onAbrirAsistencia = { onAccionClick(ActionItem(ACCION_ASISTENCIA, "")) },
                onGestionarEstudiantes = onGestionarEstudiantes,

                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )

        } else {
            // Hoy reúne los accesos del docente, igual que hoy_view en Flutter. El avance de
            // Lectura y Matemáticas vive ahora en la pantalla de Evaluación formativa.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                EnadHeading("Hoy", modifier = Modifier.padding(bottom = 8.dp))
                // SF #1: sugerencia de sede y grupo según ubicación y horario.
                ContextoSesionCard(
                    estado = contextoState,
                    onAceptar = contextoViewModel::aceptarSugerencia,
                    onCambiar = contextoViewModel::cambiar,
                    onElegir = contextoViewModel::elegir
                )
                // BQ #6: aperturas de la app en la semana.
                AperturasSemanaCard(aperturasState)
                EnadActionCard(
                    "Reportar horas",
                    Icons.Outlined.AccessTime,
                    onClick = {
                        onTabSeleccionadoChange(TeacherTabs.HORAS)
                        guia?.avanzar(Guia.HORAS, 0)
                    },
                    modifier = Modifier.objetivoGuia(Guia.HORAS, 0)
                )
                EnadActionCard("Reportar asistencia", Icons.Outlined.FactCheck, onClick = {
                    onAccionClick(ActionItem(ACCION_ASISTENCIA, ""))
                    guia?.avanzar(Guia.ASISTENCIA, 0)
                }, modifier = Modifier.objetivoGuia(Guia.ASISTENCIA, 0))
                EnadActionCard("Evaluación formativa", Icons.Outlined.Assessment, onClick = {
                    onAccionClick(ActionItem(ACCION_EVALUACION, ""))
                })
                EnadActionCard("Planear actividades", Icons.AutoMirrored.Outlined.MenuBook, onClick = {
                    mostrarActividades = true
                })
            }
        }
    }

    if (mostrarAyuda) {
        AyudaSheet(
            onElegir = { elegida ->
                mostrarAyuda = false
                guia?.iniciar(elegida)
            },
            onCerrar = { mostrarAyuda = false }
        )
    }
}

@Composable
fun TeacherTopBar(
    profesorNombre: String,
    onCambiarUsuario: () -> Unit,
    portalLabel: String = "Portal\ndocente"
) {
    EnadTopBar(nombre = profesorNombre, onCambiarUsuario = onCambiarUsuario, portalLabel = portalLabel)
}

@Composable
private fun ProgressCard(
    item: ProgressItem,
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(18.dp)
    ) {

        Row(
            modifier = Modifier.fillMaxWidth(),

            horizontalArrangement =
                Arrangement.SpaceBetween,

            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Text(
                text = item.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "${item.percent}%",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.primary
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(
                    EnadTrack,
                    RoundedCornerShape(3.dp)
                )
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction =
                            (item.percent / 100f)
                                .coerceIn(0f, 1f)
                    )
                    .height(6.dp)
                    .background(
                        MaterialTheme.colorScheme.primary,
                        RoundedCornerShape(3.dp)
                    )
            )
        }

        Spacer(
            modifier = Modifier.height(10.dp)
        )

        Text(
            text = item.evaluatedText,
            fontSize = 13.sp,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun ActionRow(
    item: ActionItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surface,
                RoundedCornerShape(14.dp)
            )
            .clickable(
                onClick = onClick
            )
            .padding(
                horizontal = 18.dp,
                vertical = 16.dp
            ),

        verticalAlignment =
            Alignment.CenterVertically
    ) {

        Column(
            modifier = Modifier.weight(1f)
        ) {

            Text(
                text = item.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color =
                    MaterialTheme.colorScheme.onBackground
            )

            Spacer(
                modifier = Modifier.height(2.dp)
            )

            Text(
                text = item.subtitle,
                fontSize = 13.sp,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant
            )
        }

        Text(
            text = ">",
            fontSize = 18.sp,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant
        )
    }
}

@Composable
private fun TeacherBottomBar(
    tabs: List<BottomTab>,
    onTabClick: (BottomTab) -> Unit
) {
    EnadBottomBar(
        items = TeacherNavItems,
        seleccionado = tabs.indexOfFirst { it.selected },
        onSeleccionar = { i -> tabs.getOrNull(i)?.let(onTabClick) }
    )
}

@Preview(showBackground = true)
@Composable
private fun TeacherHomeScreenPreview() {
    EnadMovilTheme {

        var tab by remember {
            mutableStateOf(
                TeacherTabs.HOY
            )
        }

        TeacherHomeScreen(
            tabSeleccionado = tab,
            onTabSeleccionadoChange = {
                tab = it
            }
        )
    }
}