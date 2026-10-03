package com.enad.enadmovil.domain.model

/** Método automático: arma el grupo con niños del mismo nivel y grado juntos, sin que el docente tenga que elegirlos. */
fun armarGrupoAutomatico(disponibles: List<Nino>, cantidad: Int): List<Nino> =
    disponibles.sortedWith(compareBy({ it.nivel }, { it.grado }, { it.id })).take(cantidad.coerceAtLeast(0))
