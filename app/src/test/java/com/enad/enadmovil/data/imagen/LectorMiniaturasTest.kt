package com.enad.enadmovil.data.imagen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LectorMiniaturasTest {

    @Test
    fun unaFotoDeLaCamaraSeAbreSaltandoPixeles() {
        // 4000 x 3000 entre 16 queda en 250 x 187, lo más cerca de 200 sin pasar por debajo
        assertEquals(16, LectorMiniaturas.calcularMuestra(4000, 3000, 200))
    }

    @Test
    fun unaFotoVerticalUsaSuLadoLargo() {
        assertEquals(16, LectorMiniaturas.calcularMuestra(3000, 4000, 200))
    }

    @Test
    fun unaFotoPequenaNoSeReduce() {
        assertEquals(1, LectorMiniaturas.calcularMuestra(300, 200, 200))
    }

    @Test
    fun laMuestraNuncaDejaElLadoLargoPorDebajoDelPedido() {
        listOf(400 to 300, 1280 to 720, 1920 to 1080, 4032 to 3024, 8000 to 6000).forEach { (ancho, alto) ->
            val muestra = LectorMiniaturas.calcularMuestra(ancho, alto, 200)
            assertTrue(maxOf(ancho, alto) / muestra >= 200)
            assertTrue(maxOf(ancho, alto) / (muestra * 2) < 200)
        }
    }
}
