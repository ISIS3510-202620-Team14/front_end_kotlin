package com.enad.enadmovil.core.navigation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.core.ui.theme.inkContainerLight
import com.enad.enadmovil.core.ui.theme.onInkContainerLight
import com.enad.enadmovil.core.ui.theme.onInkLight
import com.enad.enadmovil.core.ui.theme.pillBackgroundLight

@Composable
fun AppShell(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable { mutableStateOf(EnadDestination.HORAS) }

    Scaffold(
        topBar = {
            Surface(
                color = inkContainerLight,
                contentColor = onInkLight,
                modifier = modifier.fillMaxWidth().statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "ENAd", style = typography.titleLarge)
                        Text(text = "móvil", style = typography.titleLarge)
                    }
                    Column {
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
                EnadDestination.entries.forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = { selectedDestination = destination },
                        icon = {
                            Icon(
                                imageVector = destination.icon,
                                contentDescription = destination.label
                            )
                        },
                        label = { Text(destination.label) },
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
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when (selectedDestination) {
                EnadDestination.HOY -> PlaceholderScreen("Hoy")
                EnadDestination.MI_LISTA -> PlaceholderScreen("Mi lista")
                EnadDestination.GRUPOS -> PlaceholderScreen("Grupos")
                EnadDestination.HORAS -> PlaceholderScreen("Horas")
                EnadDestination.MIS_DATOS -> PlaceholderScreen("Mis datos")
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(nombre: String) {
    Text(
        text = "$nombre — próximamente",
        style = typography.bodyLarge,
        modifier = Modifier.padding(16.dp)
    )
}
