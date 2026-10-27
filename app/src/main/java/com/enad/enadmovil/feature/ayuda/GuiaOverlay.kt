package com.enad.enadmovil.feature.ayuda

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSurface
import kotlin.math.roundToInt

fun alturaReservadaGuia(compacta: Boolean): Dp = if (compacta) 150.dp else 270.dp

/** Zona de la pantalla que la guía deja libre (el elemento del paso con un margen), o null si no hay. */
private fun recorteDe(guia: GuiaViewModel, pantalla: Rect, margen: Float): Rect? =
    if (guia.completa) null
    else guia.objetivo?.inflate(margen)?.intersect(guia.area ?: pantalla)?.takeUnless { it.isEmpty }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun GuiaOverlay(guia: GuiaViewModel, compacta: Boolean, modifier: Modifier = Modifier) {
    val flujo = guia.flujo ?: return
    val paso = guia.actual ?: return
    val density = LocalDensity.current
    val foco = LocalFocusManager.current
    val radio = with(density) { 18.dp.toPx() }
    val margen = with(density) { 6.dp.toPx() }

    BoxWithConstraints(modifier.fillMaxSize()) {
        val pantalla = Rect(0f, 0f, constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val objetivo = recorteDe(guia, pantalla, margen)

        Canvas(Modifier.fillMaxSize()) {
            // El rectángulo se lee aquí, en la fase de dibujo: así el recorte sigue al elemento en el mismo
            // cuadro en que se mueve, sin esperar a que la pantalla se recomponga (se veía atrasado).
            val destino = recorteDe(guia, pantalla, margen)
            val recorte = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(Rect(Offset.Zero, size))
                if (destino != null) addRoundRect(RoundRect(destino, CornerRadius(radio)))
            }
            drawPath(recorte, EnadBackground.copy(alpha = 0.82f))
            if (destino != null) {
                drawRoundRect(
                    color = EnadPrimary.copy(alpha= 0.6f),
                    topLeft = destino.topLeft,
                    size = destino.size,
                    cornerRadius = CornerRadius(radio),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }

        val zonas = if (objetivo == null) listOf(pantalla) else listOf(
            Rect(0f, 0f, pantalla.right, objetivo.top),
            Rect(0f, objetivo.bottom, pantalla.right, pantalla.bottom),
            Rect(0f, objetivo.top, objetivo.left, objetivo.bottom),
            Rect(objetivo.right, objetivo.top, pantalla.right, objetivo.bottom)
        )

        zonas.forEach { zona ->
            Box(
                Modifier.offset { IntOffset(zona.left.roundToInt(), zona.top.roundToInt())}
                    .size(width = with(density) { zona.width.coerceAtLeast(0f).toDp()},
                        height = with(density) { zona.height.coerceAtLeast(0f).toDp()}
                    )
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) awaitPointerEvent().changes.forEach { it.consume() }
                        }
                    }
            )

        }

        Surface(shape = RoundedCornerShape(24.dp),
            color = EnadSurface,
            shadowElevation = 3.dp,
            modifier = Modifier.align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
                .widthIn(max = 460.dp)
                .fillMaxWidth()
                .onSizeChanged { guia.fijarAlturaTarjeta(it.height) }
        ) {
            Column(Modifier.padding(20.dp)) {
                if (!compacta) {
                    Text("GUÍA · ${flujo.titulo.uppercase()}", style = MaterialTheme.typography.labelSmall)
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    paso.titulo,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (!compacta) {
                    Spacer(Modifier.height(8.dp))
                    // 12 sp: a 14 sp, en un teléfono de 360 dp el texto salta antes de llegar al borde de la tarjeta.
                    Text(paso.texto, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp))
                }
                val requisito = guia.requisito
                if (guia.mostrarContinuar && !paso.opcional && requisito != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(requisito, color = Color(0xFFB71C1C), style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = guia::salir) {
                        Text(if (guia.completa) "Cerrar" else "Salir", color = EnadInk)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (guia.puedeVolver) TextButton(onClick = guia::volver) { Text("Atrás", color = EnadInk) }
                        if (guia.mostrarContinuar) {
                            Button(
                                onClick = {
                                    foco.clearFocus()
                                    guia.siguiente()
                                },
                                enabled = guia.puedeContinuar,
                                colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary, contentColor = EnadSurface)
                            ) {
                                // El tema fija el color del texto en bodyLarge (negro); aquí se usa el del botón: blanco.
                                Text(
                                    if (paso.opcional) "Omitir" else "Continuar",
                                    color = LocalContentColor.current
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}