package com.enad.enadmovil.ui.screens.auth

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val mostrarContrasena: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null
)