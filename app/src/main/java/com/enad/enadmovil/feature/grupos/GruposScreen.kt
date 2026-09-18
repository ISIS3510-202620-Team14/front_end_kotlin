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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GruposScreen(modifier: Modifier = Modifier) {
    var selectedDestination by rememberSaveable() { mutableStateOf(EnadDestination.GRUPOS)}

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
        Column(
            modifier = Modifier.padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                modifier = Modifier.padding(8.dp),
                text = "Here go the content." +
                        ""
            )
        }

    }
}