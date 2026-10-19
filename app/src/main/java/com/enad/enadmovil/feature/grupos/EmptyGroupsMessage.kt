package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.theme.EnadPrimary

@Composable
fun EmptyGroupsMessage(title: String, onCrearGrupo: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Icon(Icons.Outlined.Groups, contentDescription = null, tint = EnadPrimary, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = typography.headlineLarge.copy(fontSize = 17.sp, fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Crea el primero cuando tengas estudiantes para repartir.",
            style = typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))
        EnadSecondaryButton(text = "Crear grupo", onClick = onCrearGrupo)
    }
}
