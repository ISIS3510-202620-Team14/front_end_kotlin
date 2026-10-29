package com.enad.enadmovil.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RevisionEscaneoTest {

    private fun fila(nombre: String, codigo: String? = null) = EstudianteEscaneado(nombre = nombre, codigo = codigo)

    @Test
    fun sinTocarNadaTodasQuedanSinCambios() {
        val filas = listOf(FilaRevisada(fila("Laura Gómez", "1001")), FilaRevisada(fila("Pedro Ruiz")))
        assertEquals(ResumenRevision(2, 2, 0, 0, 0, 0), ResumenRevision.de(filas, borradas = 0))
    }

    @Test
    fun cuentaLasEdicionesPorCampoYLasBorradas() {
        val filas = listOf(
            FilaRevisada(fila("Laura Gomes", "1001"), fila("Laura Gómez", "1001")),   // nombre
            FilaRevisada(fila("Pedro Ruiz", "1O02"), fila("Pedro Ruíz", "1002")),     // nombre y código
            FilaRevisada(fila("Ana Torres"), fila("Ana Torres", "1003")),             // código
            FilaRevisada(fila("Juan Díaz", "1004"))
        )
        assertEquals(ResumenRevision(6, 1, 3, 2, 2, 2), ResumenRevision.de(filas, borradas = 2))
    }

    @Test
    fun espaciosDeSobraYCodigoVacioNoSonEdiciones() {
        val filas = listOf(FilaRevisada(fila("Laura Gómez", null), fila("  Laura Gómez ", "  ")))
        assertEquals(ResumenRevision(1, 1, 0, 0, 0, 0), ResumenRevision.de(filas, borradas = 0))
    }
}
