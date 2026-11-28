package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.border
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sugerencia = estado.sugerencia
    when {
        estado.cargando -> Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(Modifier.size(16.dp), color = EnadInk, strokeWidth = 2.dp)
            Text(
                text = "Buscando tu institución…",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        estado.eligiendo -> TarjetaElegir(estado, onElegir, onCancelar, modifier)
        sugerencia != null -> TarjetaSugerencia(sugerencia, estado, onAceptar, onCambiar, modifier)
        estado.confirmado != null -> Column(modifier) {
            TextButton(onClick = onCambiar) { Text("Cambiar grupo", color = EnadPrimary) }
            estado.error?.let { TextoError(it) }
        }
        estado.grupos.isNotEmpty() -> TextButton(onClick = onCambiar, modifier = modifier) { Text("Elegir grupo de hoy", color = EnadPrimary) }
        else -> Text(
            text = estado.error ?: "No encontramos un grupo para hoy en tu horario. Cuando tengas uno, aparecerá aquí.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
        )
    }
}

@Composable
private fun TextoError(texto: String) {
    Text(text = texto, fontSize = 13.sp, color = EnadPrimary)
}

@Composable
private fun TarjetaSugerencia(grupo: GrupoDelDia, estado: ContextoHoyUiState, onAceptar: () -> Unit, onCambiar: () -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Iniciar sesión con ${grupo.nombre}?",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = listOfNotNull(
                grupo.escuela?.let { "Estás en $it" },
                grupo.horasPlaneadas?.let { "hoy ${formatearHoras(it)} h planeadas" }
            ).joinToString(" · "),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            EnadPrimaryButton(if (estado.guardando) "Guardando…" else "Sí, iniciar", onClick = onAceptar, modifier = Modifier.weight(1f), enabled = !estado.guardando)
            EnadSecondaryButton("Cambiar", onClick = onCambiar, modifier = Modifier.weight(1f), enabled = !estado.guardando)
        }
        estado.error?.let { TextoError(it) }
        if (estado.grupos.isEmpty()) {
            Text(
                text = "No encontramos grupos en tu horario para hoy.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TarjetaElegir(estado: ContextoHoyUiState, onElegir: (GrupoDelDia) -> Unit, onCancelar: () -> Unit, modifier: Modifier) {
    Tarjeta(modifier) {
        Text(
            text = "¿Con qué grupo trabajas hoy?",
            style = MaterialTheme.typography.titleMedium
        )
        estado.grupos.forEach { grupo ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(EnadBackground, ControlShape)
                    .clickable(enabled = !estado.guardando) { onElegir(grupo) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Text(text = grupo.nombre, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = listOfNotNull(
                        nombreMateria(grupo.materia),
                        grupo.escuela,
                        grupo.horasPlaneadas?.let { "hoy ${formatearHoras(it)} h" }
                    ).joinToString(" · "),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        estado.error?.let { TextoError(it) }
        TextButton(onClick = onCancelar, enabled = !estado.guardando) { Text("Cancelar", color = EnadPrimary) }
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
