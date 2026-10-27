package com.enad.enadmovil.feature.ayuda

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay

val LocalGuia = staticCompositionLocalOf<GuiaViewModel?> { null }

@OptIn(ExperimentalFoundationApi::class)
fun Modifier.objetivoGuia(flujo: Guia, paso: Int): Modifier = composed {
    val guia = LocalGuia.current ?: return@composed this
    val clave = claveObjetivo(flujo, paso)
    val requester = remember { BringIntoViewRequester() }
    val esElActual = guia.flujo == flujo && guia.paso == paso

    // Se vuelve a pedir cada vez que el elemento cambia de lugar (la pantalla se achica al aparecer la tarjeta
    // de la guía, cargan otras tarjetas, etc.). Si ya se ve completo no desplaza nada, así que termina sola.
    val ubicacion = if (esElActual) guia.objetivo else null
    LaunchedEffect(esElActual, ubicacion) {
        if (esElActual) {
            delay(40)
            requester.bringIntoView()
        }
    }
    DisposableEffect(clave) { onDispose { guia.quitarObjetivo(clave)}}

    this.bringIntoViewRequester(requester)
        .onGloballyPositioned { guia.registrarObjetivo(clave, it.boundsInRoot())}
}