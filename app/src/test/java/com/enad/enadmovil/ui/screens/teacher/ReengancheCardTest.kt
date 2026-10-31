package com.enad.enadmovil.ui.screens.teacher

import org.junit.Assert.assertEquals
import org.junit.Test

class ReengancheCardTest {

    @Test
    fun `una sola vez va en singular`() {
        assertEquals("1 vez", veces(1.0))
    }

    @Test
    fun `los enteros no llevan decimales`() {
        assertEquals("6 veces", veces(6.0))
        assertEquals("0 veces", veces(0.0))
    }

    @Test
    fun `los promedios llevan un decimal con coma`() {
        assertEquals("2,5 veces", veces(2.5))
        assertEquals("6,3 veces", veces(6.333333))
    }
}