package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.border
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSurface
import com.enad.enadmovil.ui.theme.PanelShape
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
        estado.confirmado != null -> TextButton(onClick = onCambiar, modifier = modifier) { Text("Cambiar grupo", color = EnadPrimary) }
    }
}

@Composable
private fun TarjetaSugerencia(grupo: GrupoDelDia, onAceptar: () -> Unit, onCambiar: () -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Iniciar sesión con ${grupo.nombre}?",
            style = MaterialTheme.typography.titleMedium
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
            EnadPrimaryButton("Sí, iniciar", onClick = onAceptar, modifier = Modifier.weight(1f))
            EnadSecondaryButton("Cambiar", onClick = onCambiar, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun TarjetaElegir(grupos: List<GrupoDelDia>, onElegir: (GrupoDelDia) -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Con qué grupo trabajas hoy?",
            style = MaterialTheme.typography.titleMedium
        )
        grupos.forEach { grupo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EnadBackground, ControlShape)
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
    // Mismo panel de Flutter: blanco, borde suave y radio de 20 dp.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(EnadSurface, PanelShape)
            .border(1.dp, EnadLine, PanelShape)
            .padding(20.dp),
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
