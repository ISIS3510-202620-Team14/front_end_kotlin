package com.enad.enadmovil.feature.horas

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun HorasScreen(nombreGrupo: String, modifier: Modifier = Modifier){
    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text(text = "Reporte Interno ENAd", style = typography.titleLarge)
        }
        item {
            Text(text = nombreGrupo, style = typography.bodyLarge, color = colorScheme.onSurfaceVariant)
        }
        item {
            Text(text = "Planea y registra tu semana. Los cambios duran durante esta sesión demo.", style = typography.bodyLarge)
        }
    }
}