package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.ui.text.style.TextAlign

@Composable
fun EmptyGroupsMessage(title: String, onCrearGrupo: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = Icons.Default.Groups,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Text(text = title, style = typography.titleLarge, textAlign = TextAlign.Center)
        Text(text = "Crea el primero cuando tengas niños para repartir.", style = typography.bodyLarge, color = colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        OutlinedCard(onClick = onCrearGrupo, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Crear grupo",
                style = typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)
            )
        }
    }
}