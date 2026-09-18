package com.enad.enadmovil

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.enad.enadmovil.ui.screens.auth.LoginScreen
import com.enad.enadmovil.ui.theme.EnadMovilTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EnadMovilTheme {
                LoginScreen()
            }
        }
    }
}