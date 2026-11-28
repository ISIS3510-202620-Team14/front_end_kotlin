package com.enad.enadmovil.data.cache

import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Recomendacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CacheRecomendacionesTest {

    private val cache = CacheRecomendaciones()
    private val automatico = Recomendacion(MetodoAgrupacion.AUTOMATICO, 0.75, 8)
    private val manual = Recomendacion(MetodoAgrupacion.MANUAL, 0.6, 5)

    @Test
    fun `la primera vez es un fallo y despues de guardar es un acierto`() {
        assertNull(cache.obtener("matematicas", 25))
        cache.guardar("matematicas", 25, automatico)
        assertEquals(automatico, cache.obtener("matematicas", 25))
        assertEquals(1, cache.aciertos)
        assertEquals(1, cache.fallos)
    }

    @Test
    fun `cada materia y cada tamano tienen su propia entrada`() {
        cache.guardar("matematicas", 25, automatico)
        cache.guardar("lectura", 25, manual)
        cache.guardar("matematicas", 8, manual)
        assertEquals(automatico, cache.obtener("matematicas", 25))
        assertEquals(manual, cache.obtener("lectura", 25))
        assertEquals(manual, cache.obtener("matematicas", 8))
        assertNull(cache.obtener("lectura", 8))
        assertEquals(2, cache.tamanos("matematicas"))
    }

    @Test
    fun `invalidar una materia no toca la otra`() {
        cache.guardar("matematicas", 25, automatico)
        cache.guardar("lectura", 25, manual)
        cache.invalidar("matematicas")
        assertNull(cache.obtener("matematicas", 25))
        assertEquals(manual, cache.obtener("lectura", 25))
    }

    @Test
    fun `limpiar borra todas las materias`() {
        cache.guardar("matematicas", 25, automatico)
        cache.guardar("lectura", 25, manual)
        cache.limpiar()
        assertEquals(0, cache.tamanos("matematicas"))
        assertEquals(0, cache.tamanos("lectura"))
    }
}