package com.enad.enadmovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.enad.enadmovil.data.telemetria.RegistroFallos
import com.enad.enadmovil.ui.nav.AppNavigation
import com.enad.enadmovil.ui.theme.EnadMovilTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        RegistroFallos.instalar(this)
        lifecycleScope.launch(Dispatchers.IO) { RegistroFallos.subirPendientes() }
        enableEdgeToEdge()
        setContent {
            EnadMovilTheme {
                AppNavigation()
            }
        }
    }
}