package com.enad.enadmovil.data.local

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class VistaPreviaCsvTest {

    private lateinit var raiz: File
    private lateinit var csv: ReporteHorasCsv

    @Before
    fun crear() {
        raiz = Files.createTempDirectory("vista").toFile()
        csv = ReporteHorasCsv(File(raiz, ReporteHorasCsv.CARPETA)) { File(raiz, "Documents") }
    }

    @After
    fun borrar() {
        raiz.deleteRecursively()
    }

    @Test
    fun `cada linea es una fila y cada coma separa una columna`() {
        val filas = filasCsv("semana,horas\r\n2026-11-16,7.5,\r\n\r\n")
        assertEquals(listOf(listOf("semana", "horas"), listOf("2026-11-16", "7.5", "")), filas)
    }

    @Test
    fun `la vista previa muestra lo que quedo en el archivo exportado`() = runBlocking {
        csv.guardar("admin1", listOf(CloudFunctionsApi.SemanaHoras("2026-11-23", 5, 2, 10.0, 7.5, 3, 2, 60.0, 1)))
        val exportado = csv.exportar("admin1")!!
        val vista = csv.vistaPrevia(exportado)
        assertEquals(exportado.archivo.readText(), vista.texto)
        assertEquals(2, vista.filas.size)
        assertEquals(10, vista.filas[0].size)
        assertEquals("2026-11-23", vista.filas[1][0])
        assertTrue(vista.externo)
    }
}