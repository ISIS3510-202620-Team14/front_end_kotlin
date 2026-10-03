package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.domain.model.GrupoDelDia
import com.enad.enadmovil.feature.horas.formatearHoras

/** Confirmación ligera del contexto del día: sugerencia, selección manual o el grupo ya elegido. */
@Composable
fun ContextoSesionCard(
    estado: ContextoHoyUiState,
    onAceptar: () -> Unit,
    onCambiar: () -> Unit,
    onElegir: (GrupoDelDia) -> Unit,
    modifier: Modifier = Modifier
) {
    val sugerencia = estado.sugerencia
    when {
        estado.cargando -> Text(
            text = "Buscando tu sede…",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
        estado.eligiendo -> TarjetaElegir(estado.grupos, onElegir, modifier)
        sugerencia != null -> TarjetaSugerencia(sugerencia, onAceptar, onCambiar, modifier)
        estado.confirmado != null -> TextButton(onClick = onCambiar, modifier = modifier) { Text("Cambiar grupo") }
    }
}

@Composable
private fun TarjetaSugerencia(grupo: GrupoDelDia, onAceptar: () -> Unit, onCambiar: () -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Iniciar sesión con ${grupo.nombre}?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = listOfNotNull(
                grupo.sede?.let { "Estás en la sede $it" },
                grupo.horasPlaneadas?.let { "hoy ${formatearHoras(it)} h planeadas" }
            ).joinToString(" · "),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onAceptar) { Text("Sí, iniciar") }
            OutlinedButton(onClick = onCambiar) { Text("Cambiar") }
        }
    }
}

@Composable
private fun TarjetaElegir(grupos: List<GrupoDelDia>, onElegir: (GrupoDelDia) -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Con qué grupo trabajas hoy?",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        grupos.forEach { grupo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(10.dp))
                    .clickable { onElegir(grupo) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = grupo.nombre, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listOfNotNull(
                        nombreMateria(grupo.materia),
                        grupo.sede,
                        grupo.horasPlaneadas?.let { "hoy ${formatearHoras(it)} h" }
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Tarjeta(modifier: Modifier, contenido: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        contenido()
    }
}

internal fun nombreMateria(materia: String): String = when (materia) {
    "matematicas" -> "Matemáticas"
    "lectura" -> "Lectura"
    else -> materia
}
