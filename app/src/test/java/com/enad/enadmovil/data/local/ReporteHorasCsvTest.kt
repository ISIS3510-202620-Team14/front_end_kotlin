package com.enad.enadmovil.data.local

import com.enad.enadmovil.data.remote.CloudFunctionsApi
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ReporteHorasCsvTest {

    private lateinit var raiz: File
    private var externa: File? = null
    private lateinit var csv: ReporteHorasCsv

    @Before
    fun crear() {
        raiz = Files.createTempDirectory("horas").toFile()
        externa = File(raiz, "Documents")
        csv = ReporteHorasCsv(File(raiz, ReporteHorasCsv.CARPETA)) { externa }
    }

    @After
    fun borrar() {
        raiz.deleteRecursively()
    }

    private fun semana(semana: String, planeadas: Double, realizadas: Double) =
        CloudFunctionsApi.SemanaHoras(semana, 5, 2, planeadas, realizadas, 3, 2, 60.0, 1)

    @Test
    fun `el csv tiene encabezado y una fila por semana en orden de fecha`() {
        val texto = csvHoras(listOf(semana("2026-11-23", 10.0, 7.5), semana("2026-11-16", 0.0, 0.0)))
        assertEquals(
            "semana,docentes,dias_reportados,horas_planeadas,horas_realizadas,cumplimiento_pct," +
                "dias_sugerencia_aceptada,dias_a_mano,sugerencia_aceptada_pct,dias_con_motivo\r\n" +
                "2026-11-16,2,5,0.0,0.0,,3,2,60.0,1\r\n" +
                "2026-11-23,2,5,10.0,7.5,75.0,3,2,60.0,1\r\n",
            texto
        )
    }

    @Test
    fun `sin guardar no hay csv ni nada que exportar`() = runBlocking {
        assertNull(csv.ultimo("admin1"))
        assertNull(csv.exportar("admin1"))
    }

    @Test
    fun `guardar de nuevo actualiza el mismo archivo`() = runBlocking {
        csv.guardar("admin1", listOf(semana("2026-11-16", 10.0, 5.0)))
        val guardado = csv.guardar("admin1", listOf(semana("2026-11-16", 10.0, 5.0), semana("2026-11-23", 8.0, 8.0)))
        assertEquals(guardado.archivo, csv.ultimo("admin1")?.archivo)
        assertEquals(3, guardado.archivo.readLines().size)
        assertEquals(listOf(guardado.archivo.name), File(raiz, ReporteHorasCsv.CARPETA).list()?.toList())
    }

    @Test
    fun `exportar copia el csv a la carpeta externa con la fecha del dia`() = runBlocking {
        val guardado = csv.guardar("admin1", listOf(semana("2026-11-23", 10.0, 7.5)))
        val exportado = csv.exportar("admin1", ahora = 1_795_000_000_000L)!!
        assertTrue(exportado.externo)
        assertEquals(File(externa, "reporte_horas_2026-11-18.csv"), exportado.archivo)
        assertEquals(guardado.archivo.readText(), exportado.archivo.readText())
    }

    @Test
    fun `sin almacenamiento externo exporta el csv interno`() = runBlocking {
        externa = null
        val guardado = csv.guardar("admin1", listOf(semana("2026-11-23", 10.0, 7.5)))
        val exportado = csv.exportar("admin1")!!
        assertFalse(exportado.externo)
        assertEquals(guardado.archivo, exportado.archivo)
    }
}