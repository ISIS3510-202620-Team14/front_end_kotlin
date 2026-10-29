package com.enad.enadmovil.ui.screens.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.components.EnadBottomBar
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadNavItem
import com.enad.enadmovil.ui.components.EnadTopBar
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadPrimary

// Portal admin, igual al de Flutter: Resumen, Instituciones y Reportes.
private val AdminNavItems = listOf(
    EnadNavItem("Resumen", Icons.Outlined.GridView),
    EnadNavItem("Instituciones", Icons.Outlined.School),
    EnadNavItem("Reportes", Icons.Outlined.Description),
)
private const val TAB_RESUMEN = 0
private const val TAB_INSTITUCIONES = 1
private const val TAB_REPORTES = 2

// Reportes de la pestaña "Reportes", en el mismo orden que en Flutter.
internal const val REPORTE_APERTURAS = 0
internal const val REPORTE_SINCRONIZACION = 1
internal const val REPORTE_CLASIFICACION = 2
internal const val REPORTE_HORAS = 3
internal const val REPORTE_ACTIVIDADES = 4
internal const val REPORTE_CONFLICTOS = 5

internal const val REPORTE_EVALUACIONES = 6
private val REPORTES = listOf("Uso semanal", "Sincronización", "Clasificación", "Horas", "Actividades", "Conflictos", "Evaluaciones")


@Composable
fun AdminHomeScreen(
    nombreUsuario: String = "",
    portalLabel: String = "Portal\nadmin",
    onCambiarUsuario: () -> Unit = {},
    onVerCatalogoAlianzas: (() -> Unit)? = null,
    viewModel: AdminViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(TAB_RESUMEN) }
    var reporte by rememberSaveable { mutableIntStateOf(REPORTE_APERTURAS) }

    Scaffold(
        containerColor = EnadBackground,
        topBar = { EnadTopBar(nombreUsuario.ifBlank { "Administrador" }, onCambiarUsuario, portalLabel = portalLabel) },
        bottomBar = { EnadBottomBar(AdminNavItems, tab) { tab = it } }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                EnadHeading(AdminNavItems[tab].label)
                Text("Fundación ENAd", style = MaterialTheme.typography.bodyMedium)
            }
            when (tab) {
                TAB_RESUMEN -> {
                    AdminResumenSection(
                        estado = estado,
                        onAbrirReporte = { reporte = it; tab = TAB_REPORTES },
                        onAbrirInstituciones = { tab = TAB_INSTITUCIONES }
                    )
                    if (onVerCatalogoAlianzas != null) {
                        TextButton(onClick = onVerCatalogoAlianzas) {
                            Text("Ver catálogo de alianzas", style = MaterialTheme.typography.labelMedium, color = EnadPrimary)
                        }
                    }
                }
                TAB_INSTITUCIONES -> AdminInstitucionesSection(
                    estado = estado,
                    onBuscar = viewModel::buscar,
                    onAlternarDocentes = viewModel::alternarDocentes
                )
                else -> {
                    AdminChips(REPORTES, reporte, onSeleccionar = { reporte = it })
                    when (reporte) {
                        REPORTE_APERTURAS -> ReporteAperturasSection(estado, viewModel::cambiarSemanaAperturas, viewModel::actualizarAperturas)
                        REPORTE_SINCRONIZACION -> ReporteSincronizacionSection()
                        REPORTE_CLASIFICACION -> ReporteClasificacionSection()
                        REPORTE_HORAS -> ReporteHorasSection(estado, viewModel::actualizarHoras)
                        REPORTE_ACTIVIDADES -> ReporteActividadesSection(estado, viewModel::actualizarActividades)
                        REPORTE_EVALUACIONES -> ReporteEvaluacionesSection(estado, viewModel::actualizarEvaluaciones)
                        else -> ReporteConflictosSection(estado, viewModel::cambiarSemanaConflictos, viewModel::actualizarConflictos)
                    }
                }
            }
        }
    }
}
