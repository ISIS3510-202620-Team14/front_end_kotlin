package com.enad.enadmovil.data.local

import android.content.Context
import android.os.Environment
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

data class CsvGuardado(val archivo: File, val guardadoEn: Long)

data class CsvExportado(val archivo: File, val externo: Boolean)

data class VistaPreviaCsv(val archivo: File, val externo: Boolean, val texto: String, val filas: List<List<String>>)

class ReporteHorasCsv(
    private val carpeta: File,
    private val carpetaExterna: () -> File?
) {
    constructor(context: Context) : this(
        File(context.applicationContext.filesDir, CARPETA),
        { documentosExternos(context.applicationContext) }
    )

    private fun archivoDe(uid: String) = File(carpeta, "reporte_horas_" + uid.replace(Regex("[^A-Za-z0-9_-]"), "_") + ".csv")

    suspend fun guardar(uid: String, semanas: List<CloudFunctionsApi.SemanaHoras>): CsvGuardado = withContext(Dispatchers.IO) {
        carpeta.mkdirs()
        val destino = archivoDe(uid)
        val temporal = File(carpeta, "${destino.name}.tmp")
        temporal.writeText(csvHoras(semanas), Charsets.UTF_8)
        if (!temporal.renameTo(destino)) {
            destino.delete()
            if (!temporal.renameTo(destino)) {
                temporal.delete()
                throw IOException("No se pudo guardar ${destino.name}")
            }
        }
        CsvGuardado(destino, destino.lastModified())
    }

    suspend fun ultimo(uid: String): CsvGuardado? = withContext(Dispatchers.IO) {
        archivoDe(uid).takeIf { it.isFile }?.let { CsvGuardado(it, it.lastModified()) }
    }

    suspend fun exportar(uid: String, ahora: Long = System.currentTimeMillis()): CsvExportado? = withContext(Dispatchers.IO) {
        val origen = archivoDe(uid).takeIf { it.isFile } ?: return@withContext null
        val externa = carpetaExterna()?.takeIf { it.isDirectory || it.mkdirs() }
            ?: return@withContext CsvExportado(origen, externo = false)
        val copia = origen.copyTo(File(externa, "reporte_horas_${DIA.format(Instant.ofEpochMilli(ahora))}.csv"), overwrite = true)
        CsvExportado(copia, externo = true)
    }

    suspend fun vistaPrevia(exportado: CsvExportado): VistaPreviaCsv = withContext(Dispatchers.IO) {
        val texto = exportado.archivo.readText(Charsets.UTF_8)
        VistaPreviaCsv(exportado.archivo, exportado.externo, texto, filasCsv(texto))
    }

    companion object {
        const val CARPETA = "reporte_horas"
        private val DIA = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.of("America/Bogota"))

        private fun documentosExternos(context: Context): File? =
            if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED) {
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            } else {
                null
            }
    }
}

internal fun csvHoras(semanas: List<CloudFunctionsApi.SemanaHoras>): String = buildString {
    append("semana,docentes,dias_reportados,horas_planeadas,horas_realizadas,cumplimiento_pct,")
    append("dias_sugerencia_aceptada,dias_a_mano,sugerencia_aceptada_pct,dias_con_motivo\r\n")
    semanas.sortedBy { it.semana }.forEach { s ->
        val cumplimiento = if (s.planeadas > 0.0) numero(100 * s.realizadas / s.planeadas) else ""
        append(
            listOf(
                s.semana, s.docentes, s.dias, numero(s.planeadas), numero(s.realizadas), cumplimiento,
                s.sugeridas, s.manuales, numero(s.porcentajeSugeridas), s.conMotivo
            ).joinToString(",")
        )
        append("\r\n")
    }
}

internal fun filasCsv(texto: String): List<List<String>> =
    texto.lineSequence().filter { it.isNotBlank() }.map { it.split(",") }.toList()

private fun numero(valor: Double) = String.format(Locale.ROOT, "%.1f", valor)