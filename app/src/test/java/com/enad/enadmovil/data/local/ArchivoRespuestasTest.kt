package com.enad.enadmovil.data.local

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ArchivoRespuestasTest {

    private lateinit var raiz: File
    private lateinit var archivo: ArchivoRespuestas

    @Before
    fun crear() {
        raiz = Files.createTempDirectory("respuestas").toFile()
        archivo = ArchivoRespuestas(File(raiz, ArchivoRespuestas.CARPETA))
    }

    @After
    fun borrar() {
        raiz.deleteRecursively()
    }

    @Test
    fun `sin archivo guardado no hay respaldo`() = runBlocking {
        assertNull(archivo.leer("docente1", "estudiantes"))
    }

    @Test
    fun `guardar reemplaza el contenido anterior`() = runBlocking {
        archivo.guardar("docente1", "estudiantes", """{"students":[1]}""")
        archivo.guardar("docente1", "estudiantes", """{"students":[1,2]}""")
        assertEquals("""{"students":[1,2]}""", archivo.leer("docente1", "estudiantes")?.json)
    }

    @Test
    fun `cada docente lee solo lo suyo`() = runBlocking {
        archivo.guardar("docente1", "estudiantes", "{}")
        assertNull(archivo.leer("docente2", "estudiantes"))
    }

    @Test
    fun `no quedan temporales despues de guardar`() = runBlocking {
        archivo.guardar("docente1", "instituciones", "{}")
        val nombres = File(raiz, ArchivoRespuestas.CARPETA).list().orEmpty().toList()
        assertEquals(listOf("docente1_instituciones.json"), nombres)
    }

    @Test
    fun `borrarTodo deja el telefono sin datos de estudiantes`() = runBlocking {
        archivo.guardar("docente1", "estudiantes", "{}")
        archivo.borrarTodo()
        assertNull(archivo.leer("docente1", "estudiantes"))
        assertFalse(File(raiz, ArchivoRespuestas.CARPETA).exists())
    }

    @Test
    fun `un id con barras no sale de la carpeta`() {
        assertEquals("docente1_estudiantes____x.json", ArchivoRespuestas.nombreArchivo("docente1", "estudiantes_../x"))
    }
}