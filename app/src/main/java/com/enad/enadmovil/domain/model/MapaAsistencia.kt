package com.enad.enadmovil.domain.model

import org.json.JSONArray
import org.json.JSONObject

/** Asistencia de una institución: % de lunes a viernes, de la semana y de la semana pasada. */
data class AsistenciaInstitucion(
    val nombre: String,
    val dias: List<Double?>,
    val semana: Double?,
    val semanaPasada: Double?
) {
    /** Puntos que subió (positivo) o bajó (negativo) frente a la semana pasada. */
    val cambio: Double? get() = if (semana == null || semanaPasada == null) null else semana - semanaPasada
}

/** Mapa de asistencia de la semana: una fila por institución, de menor a mayor asistencia. */
data class MapaAsistencia(
    val dias: List<String>,
    val instituciones: List<AsistenciaInstitucion>,
    val semana: Double?,
    val guardadoEn: Long
) {
    /** La institución que más puntos bajó, o null si ninguna bajó. */
    val caidaMayor: AsistenciaInstitucion?
        get() = instituciones.filter { (it.cambio ?: 0.0) < 0 }.minByOrNull { it.cambio ?: 0.0 }
}

/** Marcas de una institución por día (vino, no_vino, sin_registro), tal como llegan del backend. */
data class MarcasInstitucion(
    val nombre: String,
    val estaSemana: List<List<String?>>,
    val semanaPasada: List<List<String?>>
)

/** Arma el mapa a partir de las marcas. Es puro para poder correrlo fuera del hilo principal. */
fun armarMapaAsistencia(dias: List<String>, marcas: List<MarcasInstitucion>, ahora: Long): MapaAsistencia {
    var vinieron = 0
    var marcadas = 0
    val instituciones = marcas.map { escuela ->
        val porDia = escuela.estaSemana.map { contar(it) }
        val pasada = escuela.semanaPasada.map { contar(it) }
        val vinoSemana = porDia.sumOf { it.first }
        val marcadasSemana = porDia.sumOf { it.second }
        vinieron += vinoSemana
        marcadas += marcadasSemana
        AsistenciaInstitucion(
            nombre = escuela.nombre,
            dias = porDia.map { porcentaje(it.first, it.second) },
            semana = porcentaje(vinoSemana, marcadasSemana),
            semanaPasada = porcentaje(pasada.sumOf { it.first }, pasada.sumOf { it.second })
        )
    }.sortedBy { it.semana ?: 101.0 }
    return MapaAsistencia(dias, instituciones, porcentaje(vinieron, marcadas), ahora)
}

// Cuenta los que vinieron y los que tienen marca; sin_registro no cuenta como marcado
private fun contar(estados: List<String?>): Pair<Int, Int> {
    val marcados = estados.filter { it != null && it != "sin_registro" }
    return marcados.count { it == "vino" } to marcados.size
}

// Porcentaje con un decimal, null si no hay marcas
private fun porcentaje(parte: Int, total: Int): Double? =
    if (total == 0) null else Math.round(parte * 1000.0 / total) / 10.0

fun MapaAsistencia.aJson(): JSONObject = JSONObject()
    .put("dias", JSONArray(dias))
    .put("semana", semana ?: JSONObject.NULL)
    .put("guardadoEn", guardadoEn)
    .put("instituciones", JSONArray(instituciones.map { i ->
        JSONObject()
            .put("nombre", i.nombre)
            .put("dias", JSONArray(i.dias.map { it ?: JSONObject.NULL }))
            .put("semana", i.semana ?: JSONObject.NULL)
            .put("semanaPasada", i.semanaPasada ?: JSONObject.NULL)
    }))

fun mapaAsistenciaDeJson(json: JSONObject): MapaAsistencia {
    val dias = json.getJSONArray("dias")
    val instituciones = json.getJSONArray("instituciones")
    return MapaAsistencia(
        dias = List(dias.length()) { dias.getString(it) },
        instituciones = List(instituciones.length()) { n ->
            val i = instituciones.getJSONObject(n)
            val porDia = i.getJSONArray("dias")
            AsistenciaInstitucion(
                nombre = i.getString("nombre"),
                dias = List(porDia.length()) { numero(porDia.opt(it)) },
                semana = numero(i.opt("semana")),
                semanaPasada = numero(i.opt("semanaPasada"))
            )
        },
        semana = numero(json.opt("semana")),
        guardadoEn = json.getLong("guardadoEn")
    )
}

private fun numero(valor: Any?): Double? = (valor as? Number)?.toDouble()
