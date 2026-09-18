package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import com.enad.enadmovil.core.navigation.EnadDestination
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.enad.enadmovil.core.ui.theme.inkContainerLight
import com.enad.enadmovil.core.ui.theme.onInkContainerLight
import com.enad.enadmovil.core.ui.theme.onInkLight
import com.enad.enadmovil.core.ui.theme.pillBackgroundLight
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import com.enad.enadmovil.core.ui.theme.amberBackgroundContainerLight
import com.enad.enadmovil.domain.model.AreaMateria
import com.enad.enadmovil.domain.model.Grupo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposScreen(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable() { mutableStateOf(EnadDestination.GRUPOS)}
    var selectedTabIndex by rememberSaveable() {mutableStateOf(0) }
    val subjectAreas = listOf("Matemáticas", "Lectura")
    Scaffold(
        topBar = {
            Surface(
                color = inkContainerLight,
                contentColor = onInkLight,
                modifier = modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                    ) {
                    Column(){
                        Text(text = "ENAd", style = typography.titleLarge)
                        Text(text = "móvil", style = typography.titleLarge)
                    }
                    Column() {
                        Text(text = "Portal", style = typography.labelSmall, color = onInkContainerLight)
                        Text(text = "Docente", style = typography.labelSmall, color = onInkContainerLight)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Mateo", style = typography.labelSmall)
                    }
                    OutlinedButton(
                        onClick = { },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = onInkLight),
                        border = BorderStroke(1.dp, onInkLight)
                    ) {
                        Text("Cambiar\nusuario", style = typography.labelSmall, textAlign = TextAlign.Center)
                    }


                }
            }
        },
        bottomBar = {
            NavigationBar {
                EnadDestination.entries.forEach {
                    destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label)},
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = pillBackgroundLight,
                            selectedIconColor = colorScheme.primary,
                            selectedTextColor = colorScheme.primary
                        )
                    )
                }
            }

        }
    ) { innerPadding ->
        val areaSeleccionada = if (selectedTabIndex == 0) AreaMateria.MATEMATICAS else AreaMateria.LECTURA
        LazyColumn(
            modifier = Modifier.padding(innerPadding).padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Mis grupos",
                    style = typography.titleLarge
                )
            }
            item {
                Text(
                    text = "Un niño puede estar en un grupo de matemáticas y en otro de lectura.",
                    style = typography.bodyLarge
                )
            }
            item {
                SecondaryTabRow(selectedTabIndex = selectedTabIndex, containerColor = Color.Transparent) {
                    subjectAreas.forEachIndexed { index, label ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(label, style = typography.labelLarge) },
                            selectedContentColor = colorScheme.primary,
                            unselectedContentColor = colorScheme.onSurfaceVariant
                        )
                    }
                }
            }


            val gruposDeEjemplo = listOf(Grupo("Grupo Abejitas", 2, "Yo", AreaMateria.MATEMATICAS), Grupo("Grupo Colibríes", 2, "Prof. Nelson" ,
                AreaMateria.MATEMATICAS),
                Grupo("Grupo Tortugas", 1, "Prof. Marina", AreaMateria.MATEMATICAS))
            items(gruposDeEjemplo.filter { it.area == areaSeleccionada }) {
                grupo ->
                GrupoCard(nombre = grupo.nombre, cantidadNinos = grupo.cantidadNinos, docente = grupo.docente, onVer = {}, onEditar = {})
            }
            if (areaSeleccionada == AreaMateria.MATEMATICAS) {
                item {
                    Surface(color = amberBackgroundContainerLight, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(color = colorScheme.tertiary, shape = CircleShape, modifier = Modifier.size(28.dp)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text= "3", color=colorScheme.onTertiary, style = typography.labelLarge)
                                }
                            }
                            Text(text = "niños todavía sin grupo.", style = typography.bodyMedium)
                        }
                    }
                }
                item {
                    OutlinedCard(onClick = {}, modifier = Modifier.fillMaxWidth()) {
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