package com.enad.enadmovil.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.enad.enadmovil.domain.model.DatosGuardados
import com.enad.enadmovil.ui.theme.EnadPendienteBg
import com.enad.enadmovil.ui.theme.EnadPendienteText

@Composable
fun AvisoDatosGuardados(guardadoEn: Long, onActualizar: () -> Unit, modifier:  Modifier = Modifier){
    Row(
        modifier = modifier.fillMaxWidth()
            .background(EnadPendienteBg, RoundedCornerShape(12.dp))
            .padding(start = 14.dp, top = 4.dp, end = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Sin conexión. Lista guardada ${DatosGuardados.cuando(guardadoEn)}",
            style = MaterialTheme.typography.bodySmall,
            color = EnadPendienteText,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onActualizar) {
            Text("Actualizar", color = EnadPendienteText)
        }
    }
}