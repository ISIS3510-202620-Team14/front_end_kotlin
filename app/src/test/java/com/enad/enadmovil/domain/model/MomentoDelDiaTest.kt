package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalTime

class MomentoDelDiaTest {

    private val manana = LocalTime.of(8, 30)
    private val tarde = LocalTime.of(18, 0)

    private fun resumen(
        planeadas: Double = 2.0,
        marcas: Int = 0,
        horas: Boolean = false,
        enSede: Double? = null
    ) = ResumenDelDia("Lectura 3A", planeadas, marcas, horas, enSede)

    @Test
    fun sinHorasEnElHorarioNoHayClase() {
        assertEquals(MomentoDelDia.SIN_CLASE, MomentoDelDia.de(resumen(planeadas = 0.0), manana))
    }

    @Test
    fun alEmpezarElDiaPideLaAsistencia() {
        val momento = MomentoDelDia.de(resumen(), manana)
        assertEquals(MomentoDelDia.INICIO, momento)
        assertEquals(AccionSugerida.ASISTENCIA, momento.accion)
    }

    @Test
    fun conAsistenciaYTiempoPorDelanteEstaEnClase() {
        assertEquals(MomentoDelDia.EN_CLASE, MomentoDelDia.de(resumen(marcas = 20, enSede = 0.5), manana))
    }

    @Test
    fun cuandoCumplioLasHorasEnLaSedePideRegistrarlas() {
        val momento = MomentoDelDia.de(resumen(marcas = 20, enSede = 2.0), manana)
        assertEquals(MomentoDelDia.CIERRE, momento)
        assertEquals(AccionSugerida.HORAS, momento.accion)
    }

    @Test
    fun alFinDeLaJornadaPideLasHorasAunqueNoHayaDatoDeLaSede() {
        assertEquals(MomentoDelDia.CIERRE, MomentoDelDia.de(resumen(marcas = 20), tarde))
    }

    @Test
    fun alFinDeLaJornadaSinAsistenciaAvisaQueFalto() {
        assertEquals(MomentoDelDia.SIN_ASISTENCIA, MomentoDelDia.de(resumen(), tarde))
    }

    @Test
    fun conAsistenciaYHorasElDiaEstaListo() {
        assertEquals(MomentoDelDia.LISTO, MomentoDelDia.de(resumen(marcas = 20, horas = true), tarde))
    }

    @Test
    fun siReportoHorasEnUnDiaSinClaseTambienEstaListo() {
        assertEquals(MomentoDelDia.LISTO, MomentoDelDia.de(resumen(planeadas = 0.0, horas = true), manana))
    }
}
