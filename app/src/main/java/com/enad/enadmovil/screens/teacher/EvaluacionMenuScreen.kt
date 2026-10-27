package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadProgress
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.feature.ayuda.Guia
import com.enad.enadmovil.feature.ayuda.LocalGuia
import com.enad.enadmovil.feature.ayuda.objetivoGuia
/**
 * Entrada a la evaluación formativa (evaluation_menu_view en Flutter): una tarjeta por
 * materia con el porcentaje de estudiantes evaluados y el acceso a la lista.
 */
@Composable
fun EvaluacionMenuScreen(
    onBack: () -> Unit,
    onMateria: (String) -> Unit,
    viewModel: TeacherProgressViewModel = viewModel(),
) {
    val estado by viewModel.uiState.collectAsStateWithLifecycle()
    val guia = LocalGuia.current

    LifecycleResumeEffect(viewModel) {
        viewModel.refrescar()
        onPauseOrDispose { }
    }

    // Mientras carga se muestran las dos materias en cero, como en Flutter.
    val materias = estado.materias.ifEmpty {
        listOf(MateriaProgreso("Lectura", 0, 0), MateriaProgreso("Matemáticas", 0, 0))
    }

    Scaffold(
        containerColor = EnadBackground,
        topBar = { BarraConVolver("Evaluación formativa", onBack) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 24.dp, end = 20.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                "Selecciona Lectura o Matemáticas. Después elige el momento de evaluación.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Column(Modifier.objetivoGuia(Guia.EVALUACION, 1), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                materias.forEach { materia ->
                    EnadPanel(onClick = { guia?.avanzar(Guia.EVALUACION, 1)
                        onMateria(materia.titulo) }) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(materia.titulo, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                            Spacer(Modifier.width(12.dp))
                            Text("${materia.porcentaje}%", style = MaterialTheme.typography.titleLarge)
                        }
                        Spacer(Modifier.height(12.dp))
                        EnadProgress(materia.porcentaje / 100f, color = EnadInk)
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${materia.evaluados} de ${materia.total} estudiantes evaluados",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Ver estudiantes", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

        }
    }
}

/** Barra de pantalla secundaria: flecha atrás y título en Lexend sobre el fondo cálido. */
@Composable
fun BarraConVolver(titulo: String, onBack: () -> Unit, subtitulo: String? = null) {
    Row(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.statusBars)
            .height(72.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = EnadInk)
        }
        Column {
            Text(titulo, style = MaterialTheme.typography.headlineMedium)
            if (subtitulo != null) Text(subtitulo, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
