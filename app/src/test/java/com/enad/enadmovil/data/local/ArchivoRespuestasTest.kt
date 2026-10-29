package com.enad.enadmovil.data.local

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.IOException
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

    private class Rechazo : Exception("el backend respondió con error")

    private suspend fun leerConRespaldo(uid: String?, pedir: suspend () -> String) =
        archivo.conRespaldo(uid, "estudiantes", esRechazo = { it is Rechazo }, pedir = pedir, convertir = { it.length })

    @Test
    fun `con red devuelve lo nuevo, sin hora, y lo guarda`() = runBlocking {
        val lectura = leerConRespaldo("docente1") { """{"a":1}""" }
        assertEquals(7, lectura.datos)
        assertNull(lectura.guardadoEn)
        assertEquals("""{"a":1}""", archivo.leer("docente1", "estudiantes")?.json)
    }

    @Test
    fun `sin red devuelve lo guardado con la hora en que se guardo`() = runBlocking {
        archivo.guardar("docente1", "estudiantes", """{"a":1,"b":2}""")
        val lectura = leerConRespaldo("docente1") { throw IOException("sin red") }
        assertEquals(13, lectura.datos)
        assertEquals(archivo.leer("docente1", "estudiantes")?.guardadoEn, lectura.guardadoEn)
    }

    @Test(expected = IOException::class)
    fun `sin red y sin archivo sigue el error de red`(): Unit = runBlocking {
        leerConRespaldo("docente1") { throw IOException("sin red") }
    }

    @Test(expected = Rechazo::class)
    fun `si el backend rechaza no se muestra lo viejo`(): Unit = runBlocking {
        archivo.guardar("docente1", "estudiantes", "{}")
        leerConRespaldo("docente1") { throw Rechazo() }
    }

    @Test
    fun `un archivo dañado no se muestra`() = runBlocking {
        archivo.guardar("docente1", "estudiantes", "{}")
        val lectura = runCatching {
            archivo.conRespaldo("docente1", "estudiantes", pedir = { throw IOException("sin red") },
                convertir = { error("JSON dañado") })
        }
        assertEquals("sin red", lectura.exceptionOrNull()?.message)
    }

    @Test
    fun `sin sesion no guarda nada`() = runBlocking {
        val lectura = leerConRespaldo(null) { "{}" }
        assertEquals(2, lectura.datos)
        assertFalse(File(raiz, ArchivoRespuestas.CARPETA).exists())
    }

    @Test
    fun `un id con barras no sale de la carpeta`() {
        assertEquals("docente1_estudiantes____x.json", ArchivoRespuestas.nombreArchivo("docente1", "estudiantes_../x"))
    }
}