package com.enad.enadmovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.enad.enadmovil.core.ui.theme.AppTheme
import com.enad.enadmovil.feature.grupos.GruposScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                GruposScreen()
            }
        }
    }
}
