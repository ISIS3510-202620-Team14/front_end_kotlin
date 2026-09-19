package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.theme.EnadAmber
import com.enad.enadmovil.ui.theme.EnadAmberBg

@Composable
fun GruposScreen(
    onVerGrupo: (Int) -> Unit,
    onEditarGrupo: (Int) -> Unit,
    onCrearGrupo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GruposViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(text = "Mis grupos", style = typography.titleLarge)
        }
        item {
            Text(
                text = "Un niño puede estar en un grupo de matemáticas y en otro de lectura.",
                style = typography.bodyLarge
            )
        }
        item {
            SecondaryTabRow(selectedTabIndex = uiState.selectedTabIndex, containerColor = Color.Transparent) {
                uiState.subjectAreas.forEachIndexed { index, label ->
                    Tab(
                        selected = uiState.selectedTabIndex == index,
                        onClick = { viewModel.onTabSelected(index) },
                        text = { Text(label, style = typography.labelLarge) },
                        selectedContentColor = colorScheme.primary,
                        unselectedContentColor = colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (uiState.grupos.isEmpty()) {
            item {
                EmptyGroupsMessage(
                    title = if (uiState.selectedTabIndex == 0) "Todavía no hay grupos de matemáticas" else "Todavía no hay grupos de lectura",
                    onCrearGrupo = onCrearGrupo
                )
            }
        } else {
            items(uiState.grupos) { grupo ->
                GrupoCard(
                    nombre = grupo.nombre,
                    cantidadNinos = grupo.ninos.size,
                    docente = grupo.docente,
                    onVer = { onVerGrupo(grupo.id) },
                    onEditar = { onEditarGrupo(grupo.id) }
                )
            }
            if (uiState.selectedTabIndex == 0) {
                item {
                    Surface(color = EnadAmberBg, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(color = EnadAmber, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = uiState.ninosSinGrupo.toString(), color = Color.White, style = typography.labelLarge)
                                }
                            }
                            Text(text = "niños todavía sin grupo.", style = typography.bodyMedium)
                        }
                    }
                }
                item {
                    OutlinedCard(onClick = onCrearGrupo, modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "Crear grupo", style = typography.titleMedium)
                                Text(text = "Un grupo a la vez: nombre, docente y niños", style = typography.bodyMedium)
                            }
                            Icon(Icons.Default.ChevronRight, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}
