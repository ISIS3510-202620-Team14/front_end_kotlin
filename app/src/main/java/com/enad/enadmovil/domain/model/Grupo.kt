package com.enad.enadmovil.domain.model

enum class AreaMateria {
    MATEMATICAS, LECTURA
}
data class Grupo(val nombre: String, val cantidadNinos: Int, val docente: String, val area: AreaMateria)