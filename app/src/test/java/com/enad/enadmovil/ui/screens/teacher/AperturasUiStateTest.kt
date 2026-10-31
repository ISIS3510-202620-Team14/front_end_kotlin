package com.enad.enadmovil.ui.screens.teacher

import com.enad.enadmovil.domain.model.RiesgoAbandono
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AperturasUiStateTest {

    private val enRiesgo = RiesgoAbandono.desdeAperturas(listOf(6, 5, 7, 2))

    @Test
    fun `por defecto no se muestra la tarjeta de riesgo`() {
        assertFalse(AperturasUiState().mostrarRiesgo)
    }

    @Test
    fun `se muestra cuando hay riesgo y no se ha descartado`() {
        assertTrue(AperturasUiState(riesgo = enRiesgo).mostrarRiesgo)
    }

    @Test
    fun `se oculta una vez descartada aunque siga el riesgo`() {
        assertFalse(AperturasUiState(riesgo = enRiesgo, riesgoDescartado = true).mostrarRiesgo)
    }

    @Test
    fun `sin riesgo no se muestra`() {
        val estable = RiesgoAbandono.desdeAperturas(listOf(6, 5, 7, 6))
        assertFalse(AperturasUiState(riesgo = estable).mostrarRiesgo)
    }
}