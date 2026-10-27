package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CondicionCapturaTest {

    private val gravedad = 9.81f

    @Test
    fun elNivelDeLuzSigueLosUmbrales() {
        assertEquals(NivelLuz.DESCONOCIDA, NivelLuz.de(null))
        assertEquals(NivelLuz.OSCURO, NivelLuz.de(3f))
        assertEquals(NivelLuz.BAJA, NivelLuz.de(40f))
        assertEquals(NivelLuz.BUENA, NivelLuz.de(300f))
        assertEquals(NivelLuz.EXCESIVA, NivelLuz.de(50_000f))
    }

    @Test
    fun quietoSobreLaMesaSoloCuentaDespuesDeLaVentana() {
        val detector = DetectorQuietud(ventanaMs = 600)
        assertFalse(detector.agregar(0f, 0f, gravedad, 0))
        assertFalse(detector.agregar(0f, 0f, gravedad, 300))
        assertTrue(detector.agregar(0f, 0f, gravedad, 600))
    }

    @Test
    fun unSacudonReiniciaLaQuietud() {
        val detector = DetectorQuietud(ventanaMs = 600)
        (0..700 step 50).forEach { detector.agregar(0f, 0f, gravedad, it.toLong()) }
        assertFalse(detector.agregar(3f, -2f, gravedad + 4f, 750))
        // Después del movimiento vuelve a esperar la ventana completa
        var quieto = false
        (800..1300 step 50).forEach { quieto = detector.agregar(0f, 0f, gravedad, it.toLong()) }
        assertFalse(quieto)
        (1350..2000 step 50).forEach { quieto = detector.agregar(0f, 0f, gravedad, it.toLong()) }
        assertTrue(quieto)
    }

    @Test
    fun inclinadoPeroQuietoTambienEsEstable() {
        // La gravedad repartida en los ejes (celular inclinado) no es movimiento
        val detector = DetectorQuietud(ventanaMs = 200)
        var quieto = false
        (0..400 step 50).forEach { quieto = detector.agregar(4f, 3f, 8.4f, it.toLong()) }
        assertTrue(quieto)
    }
}
