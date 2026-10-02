package com.enad.enadmovil.ui.screens.auth

import com.enad.enadmovil.data.remote.CloudFunctionsApi

data class AuthUiState(
    val usuario: String = "",
    val contrasena: String = "",
    val nombreCompleto: String = "",
    val modoRegistro: Boolean = false,
    val mostrarContrasena: Boolean = false,
    val cargando: Boolean = false,
    val error: String? = null,
    // Registro: instituciones que vienen del back y lo que el docente marcó.
    val instituciones: List<CloudFunctionsApi.Institucion> = emptyList(),
    val cargandoInstituciones: Boolean = false,
    // id de institución -> ids de sedes marcadas en ella. Estar en el mapa = institución marcada.
    val sedesElegidas: Map<String, Set<String>> = emptyMap(),
    // Si el back alcanzó a enviar el correo de bienvenida al registrarse.
    val correoBienvenidaEnviado: Boolean = false
)
