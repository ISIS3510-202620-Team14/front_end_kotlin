package com.enad.enadmovil.domain.model

data class Usuario(
    val uid: String = "",
    val email: String = "",
    val fullName: String = "",
    val rol: String = "sin_privilegios"
)