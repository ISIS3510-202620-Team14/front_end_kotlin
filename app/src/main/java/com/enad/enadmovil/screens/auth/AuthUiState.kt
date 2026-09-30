package com.enad.enadmovil.ui.screens.auth

data class AuthUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val nombreCompleto: String = "",
    val modoRegistro: Boolean = false,
    val mostrarContrasena: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null
)