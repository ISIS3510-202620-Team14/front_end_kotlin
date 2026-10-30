package com.enad.enadmovil.ui.screens.admin

import com.enad.enadmovil.feature.ayuda.Guia
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LecturaPasosGuiasTest {

    @Test
    fun losIdsDeLosEventosSonLasGuiasDeAyuda() {
        assertEquals(Guia.HORAS, guiaDeId("hours"))
        assertEquals(Guia.ASISTENCIA, guiaDeId("attendance"))
        assertEquals(Guia.EVALUACION, guiaDeId("evaluation"))
        assertEquals(Guia.ACTIVIDADES, guiaDeId("activities"))
        assertNull(guiaDeId("otra"))
    }

    @Test
    fun elPasoSeMuestraDesdeUnoConSuTitulo() {
        assertEquals("Paso 1: Abre Reportar asistencia", nombrePaso(Guia.ASISTENCIA, 0))
        assertEquals("Paso 3: Añade estudiantes (opcional)", nombrePaso(Guia.ASISTENCIA, 2))
    }

    @Test
    fun unPasoQueNoExisteSoloLlevaElNumero() {
        assertEquals("Paso 41", nombrePaso(Guia.HORAS, 40))
        assertEquals("Paso 2", nombrePaso(null, 1))
    }
}
