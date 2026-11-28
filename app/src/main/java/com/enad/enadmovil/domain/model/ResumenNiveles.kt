package com.enad.enadmovil.domain.model

/** En qué está la vista de niveles por institución. */
enum class EstadoNiveles { CARGANDO, ACTUALIZANDO, EN_LINEA, SIN_CONEXION, VACIO }

/** Lo que muestra el resumen de la vista para las filas de un filtro (materia y grado). */
class ResumenNiveles(val filas: List<FilaNiveles>) {

    val evaluados: Int
        get() {
            var total = 0
            for (i in filas.indices) total += filas[i].evaluados
            return total
        }

    val principiantes: Int
        get() {
            var total = 0
            for (i in filas.indices) total += filas[i].principiantes
            return total
        }

    /** % de evaluados que siguen en Principiante en todas las instituciones; null sin evaluados. */
    val porcentajePrincipiante: Double?
        get() {
            val total = evaluados
            return if (total == 0) null else principiantes * 100.0 / total
        }

    /** Las filas llegan ordenadas por apoyo necesario, así que la primera responde la BQ. */
    val necesitaMasApoyo: FilaNiveles?
        get() = filas.firstOrNull()?.takeIf { it.porcentajePrincipiante != null }

    /** La institución guardada hace más tiempo: la fecha del aviso sin conexión. */
    val guardadoEn: Long?
        get() = filas.minOfOrNull { it.guardadoEn }
}

/** Una institución con datos de antes del último intento de descarga no se actualizó. */
fun sinActualizar(fila: FilaNiveles, ultimoIntento: Long?): Boolean =
    ultimoIntento != null && fila.guardadoEn < ultimoIntento

/** Sin red: lo guardado en SQLite o, si no hay nada, la explicación de qué hacer. */
fun estadoSinConexion(hayFilas: Boolean): EstadoNiveles =
    if (hayFilas) EstadoNiveles.SIN_CONEXION else EstadoNiveles.VACIO

/** Cómo queda la vista al terminar una descarga. */
fun estadoTrasDescarga(hayFilas: Boolean, total: Int, fallidas: Int): EstadoNiveles = when {
    !hayFilas -> EstadoNiveles.VACIO
    total > 0 && fallidas == total -> EstadoNiveles.SIN_CONEXION
    else -> EstadoNiveles.EN_LINEA
}
