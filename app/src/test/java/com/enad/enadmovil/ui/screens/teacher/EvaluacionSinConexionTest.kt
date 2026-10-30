package com.enad.enadmovil.ui.screens.teacher

import com.enad.enadmovil.data.local.EvaluacionPendiente
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class EvaluacionSinConexionTest {

    private fun estudiante(id: String, nivel: String? = null) =
        EstudianteClasificacion(id = id, numero = 1, nombre = "Estudiante $id", grado = 3, nivelActual = nivel)

    private fun pendiente(id: String, etiqueta: String, fecha: String) =
        EvaluacionPendiente("c-$id-$fecha", id, "lectura", "palabra", etiqueta, "inicial", fecha, "F", 9, 1)

    @Test
    fun sinRedOServidorCaidoSeGuardaParaDespues() {
        assertTrue(esFallaDeConexion(IOException("sin red")))
        assertTrue(esFallaDeConexion(CloudFunctionsApi.ApiException("internal", "caído", 503)))
        assertTrue(esFallaDeConexion(CloudFunctionsApi.ApiException("timeout", "lento", 408)))
    }

    @Test
    fun unRechazoDelBackendNoSeGuarda() {
        assertFalse(esFallaDeConexion(CloudFunctionsApi.ApiException("invalid-argument", "edad", 400)))
        assertFalse(esFallaDeConexion(CloudFunctionsApi.ApiException("permission-denied", "ajeno", 403)))
        assertFalse(esFallaDeConexion(CloudFunctionsApi.ApiException("unauthenticated", "vencida", 401)))
    }

    @Test
    fun laListaMuestraLaEvaluacionPendienteMasReciente() {
        val lista = listOf(estudiante("a", nivel = "Letra"), estudiante("b", nivel = "Cuento"))
        val pendientes = listOf(
            pendiente("a", "Palabra", "2026-10-30T10:00:00Z"),
            pendiente("a", "Párrafo", "2026-10-30T11:00:00Z")
        )
        val resultado = aplicarPendientes(lista, pendientes)
        assertEquals("Párrafo", resultado[0].nivelActual)
        assertEquals("9", resultado[0].edad)
        assertEquals("Cuento", resultado[1].nivelActual) // sin pendientes no cambia
    }
}
