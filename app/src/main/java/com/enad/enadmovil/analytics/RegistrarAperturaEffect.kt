package com.enad.enadmovil.analytics

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.enad.enadmovil.data.repository.AperturasRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RegistrarAperturaEffect() {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    // ON_START fires when the screen first appears and every time the app returns to the
    // foreground. The repository debounces opens that are too close together.
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        scope.launch(Dispatchers.IO) { AperturasRepository(context).registrar() }
    }
}