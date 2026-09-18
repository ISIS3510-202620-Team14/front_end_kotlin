package com.enad.enadmovil.data.mapper

import com.enad.enadmovil.data.local.GrupoConNinos
import com.enad.enadmovil.domain.model.Grupo
import com.enad.enadmovil.domain.model.Nino

fun GrupoConNinos.aGrupo(): Grupo = Grupo(
    id = grupo.id,
    nombre = grupo.nombre,
    ninos = ninos.map { Nino(it.id, it.nombre, it.nivel, it.grado) },
    docente = grupo.docente,
    area = grupo.area
)
