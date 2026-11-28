package com.enad.enadmovil.data.repository

import com.enad.enadmovil.data.cache.CacheArchivosNiveles
import com.enad.enadmovil.data.local.niveles.ConteoNivelEntity
import com.enad.enadmovil.data.local.niveles.NivelesDao
import com.enad.enadmovil.data.local.niveles.SumaNivel
import com.enad.enadmovil.data.niveles.EscuelaNivel
import com.enad.enadmovil.data.niveles.FuenteNiveles
import com.enad.enadmovil.domain.model.NivelesInstitucion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NivelesRepositoryTest {

    @get:Rule val carpeta = TemporaryFolder()

    /** La tabla en memoria, para no necesitar un emulador. */
    private class DaoEnMemoria : NivelesDao() {
        val filas = mutableListOf<ConteoNivelEntity>()
        var borradoDesde: Long? = null
        override suspend fun borrarInstitucion(uid: String, schoolId: String) { filas.removeAll { it.docenteUid == uid && it.schoolId == schoolId } }
        override suspend fun insertar(conteos: List<ConteoNivelEntity>) { filas += conteos }
        override suspend fun sumasPorNivel(uid: String, materia: String, grado: Int): List<SumaNivel> = emptyList()
        override suspend fun borrarAnterioresA(uid: String, desde: Long) { borradoDesde = desde }
    }

    private class RedFalsa(private val falla: String? = null) : FuenteNiveles {
        var pedidas = 0
        override suspend fun escuelasJson(): String { pedidas++; return "escuelas" }
        override suspend fun estudiantesJson(schoolId: String): String {
            pedidas++
            if (schoolId == falla) error("sin señal")
            return "estudiantes-$schoolId"
        }
    }

    private val escuelas = listOf(EscuelaNivel("a", "Escuela A"), EscuelaNivel("b", "Escuela B"))
    private var ahora = 1_800_000_000_000L

    private fun repo(dao: NivelesDao, red: FuenteNiveles) = NivelesRepository(
        dao = dao,
        cache = CacheArchivosNiveles(carpeta.root, reloj = { ahora }, registro = {}),
        red = red,
        aEscuelas = { escuelas },
        contar = { IntArray(NivelesInstitucion.TAMANO).also { c -> c[NivelesInstitucion.indice(0, 0, 1)] = 1 } },
        reloj = { ahora },
        registro = {}
    )

    @Test
    fun laSegundaDescargaSaleDeLaCache() = runBlocking {
        val red = RedFalsa()
        val repo = repo(DaoEnMemoria(), red)

        val primera = repo.descargar("uid", forzar = false)
        ahora += 10 * 60 * 1000L
        val segunda = repo.descargar("uid", forzar = false)

        assertEquals(3, primera.peticiones)  // la lista + 2 instituciones
        assertEquals(0, segunda.peticiones)
        assertEquals(3, segunda.desdeCache)
        assertEquals(3, red.pedidas)
    }

    @Test
    fun forzarIgnoraLaCache() = runBlocking {
        val red = RedFalsa()
        val repo = repo(DaoEnMemoria(), red)
        repo.descargar("uid", forzar = false)

        val forzada = repo.descargar("uid", forzar = true)

        assertEquals(3, forzada.peticiones)
        assertEquals(0, forzada.desdeCache)
    }

    @Test
    fun siUnaFallaSigueLaSiguienteYNoBorraLoViejo() = runBlocking {
        val dao = DaoEnMemoria()
        val progreso = mutableListOf<Pair<Int, Int>>()

        val resultado = repo(dao, RedFalsa(falla = "a")).descargar("uid", forzar = false) { h, t -> progreso += h to t }

        assertEquals(1, resultado.fallidas)
        assertEquals(setOf("b"), dao.filas.map { it.schoolId }.toSet())
        assertNull(dao.borradoDesde) // con una fallida no se sabe qué instituciones ya no existen
        assertEquals(listOf(0 to 2, 1 to 2, 2 to 2), progreso)
    }

    @Test
    fun conLaDescargaCompletaBorraLasInstitucionesViejas() = runBlocking {
        val dao = DaoEnMemoria()
        val resultado = repo(dao, RedFalsa()).descargar("uid", forzar = false)

        assertEquals(0, resultado.fallidas)
        assertEquals(resultado.iniciadaEn, dao.borradoDesde)
        assertEquals(setOf("a", "b"), dao.filas.map { it.schoolId }.toSet())
    }
}
