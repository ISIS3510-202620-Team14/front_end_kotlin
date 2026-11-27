package com.enad.enadmovil.data.local

import android.content.Context
import com.enad.enadmovil.domain.model.MapaAsistencia
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reporte de asistencia guardado como .txt en el celular. */
data class ReporteGuardado(val archivo: File, val guardadoEn: Long, val bytes: Long)

/** Guarda los reportes del mapa de asistencia en filesDir/reportes_asistencia/<uid>. */
class ReportesAsistencia(private val carpeta: File) {
    constructor(context: Context) : this(File(context.applicationContext.filesDir, CARPETA))

    private fun carpetaDe(uid: String) = File(carpeta, uid.replace(Regex("[^A-Za-z0-9_-]"), "_"))

    suspend fun guardar(uid: String, mapa: MapaAsistencia, ahora: Long = System.currentTimeMillis()): ReporteGuardado =
        withContext(Dispatchers.IO) {
            val destino = carpetaDe(uid).apply { mkdirs() }
            val archivo = File(destino, "reporte_asistencia_${SELLO.format(Instant.ofEpochMilli(ahora))}.txt")
            archivo.writeText(textoReporte(mapa, ahora), Charsets.UTF_8)
            ReporteGuardado(archivo, archivo.lastModified(), archivo.length())
        }

    suspend fun listar(uid: String): List<ReporteGuardado> = withContext(Dispatchers.IO) {
        (carpetaDe(uid).listFiles() ?: emptyArray())
            .filter { it.isFile && it.name.endsWith(".txt") }
            .map { ReporteGuardado(it, it.lastModified(), it.length()) }
            .sortedByDescending { it.guardadoEn }
    }

    suspend fun leer(reporte: ReporteGuardado): String = withContext(Dispatchers.IO) { reporte.archivo.readText(Charsets.UTF_8) }

    suspend fun borrar(reporte: ReporteGuardado) {
        withContext(Dispatchers.IO) { reporte.archivo.delete() }
    }

    companion object {
        const val CARPETA = "reportes_asistencia"
        private val ZONA = ZoneId.of("America/Bogota")
        private val SELLO = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss").withZone(ZONA)
    }
}

private val DIAS = listOf("Lun", "Mar", "Mié", "Jue", "Vie")
private val ZONA_CO = ZoneId.of("America/Bogota")
private val FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZONA_CO)

/** Texto del reporte, el mismo que arma Flutter. */
fun textoReporte(mapa: MapaAsistencia, creadoEn: Long): String = buildString {
    val caida = mapa.caidaMayor
    appendLine("REPORTE DE ASISTENCIA")
    appendLine("Semana del ${fecha(mapa.dias.first())} al ${fecha(mapa.dias.last())}")
    appendLine("Generado: ${FECHA_HORA.format(Instant.ofEpochMilli(creadoEn))}")
    appendLine()
    appendLine("Asistencia total de la semana: ${porcentaje(mapa.semana)}")
    appendLine(
        if (caida == null) "Institución que más bajó: ninguna"
        else "Institución que más bajó: ${caida.nombre} (${decimal(Math.abs(caida.cambio ?: 0.0))} puntos)"
    )
    appendLine()
    appendLine("INSTITUCIONES (de menor a mayor asistencia)")
    mapa.instituciones.forEach { institucion ->
        appendLine()
        appendLine(institucion.nombre)
        institucion.dias.forEachIndexed { i, dia ->
            appendLine("  ${DIAS[i]} ${fecha(mapa.dias[i])}: ${porcentaje(dia)}")
        }
        appendLine("  Semana: ${porcentaje(institucion.semana)} · Semana pasada: ${porcentaje(institucion.semanaPasada)}")
    }
}

private fun fecha(dia: String) = LocalDate.parse(dia).format(FECHA)

private fun decimal(valor: Double) = String.format(Locale.ROOT, "%.1f", valor).replace('.', ',')

private fun porcentaje(valor: Double?) = if (valor == null) "—" else "${decimal(valor)} %"
