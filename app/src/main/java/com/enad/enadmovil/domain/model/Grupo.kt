package com.enad.enadmovil.domain.model

enum class AreaMateria {
    MATEMATICAS, LECTURA
}
data class Grupo(val id: Int, val nombre: String, val ninos: List<Nino>, val docente: String, val area: AreaMateria)