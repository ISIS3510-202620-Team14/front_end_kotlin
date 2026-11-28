package com.enad.enadmovil.data.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CacheArchivosNivelesTest {

    @get:Rule val carpeta = TemporaryFolder()

    private var ahora = 1_800_000_000_000L
    private fun cache() = CacheArchivosNiveles(carpeta.root, reloj = { ahora }, registro = {})

    @Test
    fun dentroDeLos30MinutosSaleDeLaCache() {
        val cache = cache()
        cache.guardar("uid_estudiantes_s1", """{"students":[]}""")
        ahora += 29 * 60 * 1000L

        assertEquals("""{"students":[]}""", cache.fresco("uid_estudiantes_s1"))
        assertEquals(1, cache.desdeCache)
        assertEquals(0, cache.desdeRed)
    }

    @Test
    fun vencidaOSinArchivoVaALaRed() {
        val cache = cache()
        assertNull(cache.fresco("nunca_guardada"))
        cache.guardar("uid_instituciones", "{}")
        ahora += CacheArchivosNiveles.VIGENCIA_MS

        assertNull(cache.fresco("uid_instituciones"))
        assertEquals(2, cache.desdeRed)
    }

    @Test
    fun borraLosViejosYDejaComoMaximo60() {
        val cache = cache()
        cache.guardar("vieja", "{}")
        ahora += CacheArchivosNiveles.SIN_USO_MS
        repeat(65) { i ->
            ahora += 1000
            cache.guardar("escuela_$i", "{}")
        }

        val nombres = carpeta.root.listFiles()!!.map { it.name }.toSet()
        assertEquals(CacheArchivosNiveles.MAX_ARCHIVOS, nombres.size)
        assertEquals(false, "vieja.json" in nombres)
        assertEquals(false, "escuela_0.json" in nombres) // la menos reciente sale primero
        assertEquals(true, "escuela_64.json" in nombres)
    }
}
