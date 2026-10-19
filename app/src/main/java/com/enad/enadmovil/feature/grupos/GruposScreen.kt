package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.PeopleOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadPanel
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadSegmented
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary

@Composable
fun GruposScreen(
    onVerGrupo: (Int) -> Unit,
    onEditarGrupo: (Int) -> Unit,
    onCrearGrupo: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GruposViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val materia = if (uiState.selectedTabIndex == 0) "matemáticas" else "lectura"

    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                EnadHeading("Mis grupos", modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.recargar() }) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Actualizar grupos", tint = EnadInk)
                }
            }
            Text(
                text = "Un estudiante puede estar en un grupo de matemáticas y en otro de lectura.",
                style = typography.bodyMedium
            )
            Spacer(Modifier.height(12.dp))
        }
        item {
            EnadPanel {
                EnadSegmented(
                    opciones = uiState.subjectAreas,
                    seleccionada = uiState.selectedTabIndex,
                    onSeleccionar = viewModel::onTabSelected
                )
            }
            Spacer(Modifier.height(12.dp))
        }

        if (uiState.grupos.isEmpty()) {
            item {
                EmptyGroupsMessage(title = "Todavía no hay grupos de $materia", onCrearGrupo = onCrearGrupo)
            }
        } else {
            items(uiState.grupos, key = { it.id }) { grupo ->
                GrupoCard(
                    nombre = grupo.nombre,
                    cantidadNinos = grupo.ninos.size,
                    docente = grupo.docente,
                    onVer = { onVerGrupo(grupo.id) },
                    onEditar = { onEditarGrupo(grupo.id) }
                )
            }
            if (uiState.ninosSinGrupo > 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PeopleOutline, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = if (uiState.ninosSinGrupo == 1) "1 estudiante todavía sin grupo."
                            else "${uiState.ninosSinGrupo} estudiantes todavía sin grupo.",
                            style = typography.bodyMedium
                        )
                    }
                }
            }
            item {
                EnadSecondaryButton(text = "Crear grupo", onClick = onCrearGrupo, icon = Icons.Outlined.GroupAdd)
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
