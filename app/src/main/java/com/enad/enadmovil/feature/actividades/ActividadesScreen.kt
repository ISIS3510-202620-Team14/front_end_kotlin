package com.enad.enadmovil.feature.actividades

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.domain.model.ActivitySource
import com.enad.enadmovil.domain.model.ActivityTemplate

@Composable
fun ActividadesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActividadesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var mostrarDialogoPropia by remember {
        mutableStateOf(false)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        item {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Volver"
                    )
                }

                Column {
                    Text(
                        text = "Actividades",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Text(
                        text = "Planea desde la biblioteca o crea las tuyas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            UsageCard(state)
        }

        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("Matemáticas", "Lectura").forEach { subject ->
                    FilterChip(
                        selected = state.selectedSubject == subject,
                        onClick = {
                            viewModel.seleccionarMateria(subject)
                        },
                        label = {
                            Text(subject)
                        }
                    )
                }
            }
        }

        item {
            Text(
                text = "BIBLIOTECA",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(
            items = viewModel.library,
            key = { it.id }
        ) { template ->
            ActivityTemplateCard(
                template = template,
                onUse = {
                    viewModel.usarActividadDeBiblioteca(template)
                }
            )
        }

        item {
            Button(
                onClick = {
                    mostrarDialogoPropia = true
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null
                )

                Spacer(modifier = Modifier.padding(4.dp))

                Text("Crear propia")
            }
        }

        item {
            Text(
                text = "PLANEADAS HOY · ${state.planned.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (state.planned.isEmpty()) {
            item {
                Text(
                    text = "Todavía no has planeado actividades.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(
                items = state.planned,
                key = { it.id }
            ) { activity ->
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (activity.source == ActivitySource.LIBRARY) {
                                Icons.Filled.MenuBook
                            } else {
                                Icons.Filled.Edit
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Column {
                            Text(
                                text = activity.title,
                                style = MaterialTheme.typography.titleMedium
                            )

                            Text(
                                text = "${activity.subject} · ${activity.minutes} min · " +
                                        if (activity.source == ActivitySource.LIBRARY) {
                                            "Biblioteca"
                                        } else {
                                            "Propia"
                                        },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    if (mostrarDialogoPropia) {
        CrearActividadDialogo(
            subject = state.selectedSubject,
            onDismiss = {
                mostrarDialogoPropia = false
            },
            onCreate = { title, minutes ->
                val error = viewModel.crearActividadPropia(
                    title = title,
                    minutes = minutes
                )

                if (error == null) {
                    mostrarDialogoPropia = false
                }

                error
            }
        )
    }
}

@Composable
private fun UsageCard(
    state: ActividadesUiState
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Text(
                text = "TUS ACTIVIDADES",
                style = MaterialTheme.typography.labelLarge
            )

            when {
                state.loadingUsage && state.usage.total == 0 -> {
                    Text("Cargando historial…")
                }

                state.usage.total == 0 -> {
                    Text(
                        text = state.usageError
                            ?: "Aún no has planeado actividades. Aquí verás cuántas tomas de la biblioteca y cuántas creas tú.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                else -> {
                    Text(
                        text = "${state.usage.libraryPercent}% de la biblioteca",
                        style = MaterialTheme.typography.headlineSmall
                    )

                    Text(
                        text = "De tus ${state.usage.total} actividades, " +
                                "${state.usage.libraryCount} salieron de la biblioteca y " +
                                "${state.usage.customCount} las creaste tú " +
                                "(${state.usage.customPercent}%).",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ActivityTemplateCard(
    template: ActivityTemplate,
    onUse: () -> Unit
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.MenuBook,
                    contentDescription = null
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium
                    )

                    Text(
                        text = "${template.minutes} min · ${template.description}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = onUse,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Usar")
            }
        }
    }
}

@Composable
private fun CrearActividadDialogo(
    subject: String,
    onDismiss: () -> Unit,
    onCreate: (String, Int?) -> String?
) {
    var title by remember {
        mutableStateOf("")
    }

    var minutesText by remember {
        mutableStateOf("")
    }

    var error by remember {
        mutableStateOf<String?>(null)
    }

    AlertDialog(
        onDismissRequest = onDismiss,

        title = {
            Text("Nueva actividad · $subject")
        },

        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                    },
                    label = {
                        Text("Nombre")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = minutesText,
                    onValueChange = {
                        minutesText = it
                    },
                    label = {
                        Text("Minutos")
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                error?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },

        confirmButton = {
            Button(
                onClick = {
                    error = onCreate(
                        title,
                        minutesText.toIntOrNull()
                    )
                }
            ) {
                Text("Crear")
            }
        },

        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Cancelar")
            }
        }
    )
}