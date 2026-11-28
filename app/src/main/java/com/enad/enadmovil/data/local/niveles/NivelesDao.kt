package com.enad.enadmovil.data.local.niveles

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.enad.enadmovil.domain.model.FilaNiveles
import com.enad.enadmovil.domain.model.NivelesInstitucion

/**
 * LOCAL STORAGE (Sprint 4): los conteos están organizados por institución, materia, grado y nivel,
 * y la vista los filtra por materia y grado, así que cada filtro es una sola consulta GROUP BY,
 * también sin internet.
 */
@Dao
abstract class NivelesDao {

    @Query("DELETE FROM conteos_nivel WHERE docenteUid = :uid AND schoolId = :schoolId")
    abstract suspend fun borrarInstitucion(uid: String, schoolId: String)

    @Insert
    abstract suspend fun insertar(conteos: List<ConteoNivelEntity>)

    /** Reemplaza los conteos de una institución en una sola transacción. */
    @Transaction
    open suspend fun guardarInstitucion(uid: String, schoolId: String, nombre: String, conteos: IntArray, guardadoEn: Long) {
        borrarInstitucion(uid, schoolId)
        val filas = aConteos(uid, schoolId, nombre, conteos, guardadoEn)
        if (filas.isNotEmpty()) insertar(filas)
    }

    /** grado = 0 suma los tres grados. */
    @Query(
        "SELECT schoolId, nombreInstitucion, nivel, SUM(estudiantes) AS estudiantes, MIN(guardadoEn) AS guardadoEn " +
            "FROM conteos_nivel " +
            "WHERE docenteUid = :uid AND materia = :materia AND (:grado = 0 OR grado = :grado) " +
            "GROUP BY schoolId, nombreInstitucion, nivel"
    )
    abstract suspend fun sumasPorNivel(uid: String, materia: String, grado: Int): List<SumaNivel>

    /** Filas de una materia (grado 0 = todos), la institución que necesita más apoyo primero. */
    open suspend fun filas(uid: String, materia: String, grado: Int): List<FilaNiveles> =
        agruparPorInstitucion(sumasPorNivel(uid, materia, grado))

    /** Después de una descarga completa, borra las instituciones que ya no existen. */
    @Query("DELETE FROM conteos_nivel WHERE docenteUid = :uid AND guardadoEn < :desde")
    abstract suspend fun borrarAnterioresA(uid: String, desde: Long)
}

/** Pasa los 42 conteos de una institución a filas de la tabla, solo las celdas con estudiantes. */
internal fun aConteos(uid: String, schoolId: String, nombre: String, conteos: IntArray, guardadoEn: Long): List<ConteoNivelEntity> {
    require(conteos.size == NivelesInstitucion.TAMANO) { "Se esperaban ${NivelesInstitucion.TAMANO} conteos" }
    val filas = mutableListOf<ConteoNivelEntity>()
    NivelesInstitucion.MATERIAS.forEachIndexed { m, materia ->
        NivelesInstitucion.GRADOS.forEachIndexed { g, grado ->
            for (nivel in 0 until NivelesInstitucion.NIVELES) {
                val estudiantes = conteos[NivelesInstitucion.indice(m, g, nivel)]
                if (estudiantes == 0) continue
                filas += ConteoNivelEntity(uid, schoolId, nombre, materia, grado, nivel, estudiantes, guardadoEn)
            }
        }
    }
    return filas
}

/** Junta las sumas del GROUP BY en una fila por institución, ordenadas por apoyo necesario. */
internal fun agruparPorInstitucion(sumas: List<SumaNivel>): List<FilaNiveles> {
    val porInstitucion = LinkedHashMap<String, FilaNiveles>()
    for (s in sumas) {
        if (s.nivel !in 0 until NivelesInstitucion.NIVELES) continue
        val fila = porInstitucion.getOrPut(s.schoolId) { FilaNiveles(s.schoolId, s.nombreInstitucion, s.guardadoEn) }
        fila.niveles[s.nivel] = s.estudiantes
    }
    return porInstitucion.values.sortedWith(FilaNiveles.porApoyoNecesario)
}
