package com.enad.enadmovil.domain.model

import org.json.JSONArray
import org.json.JSONObject

data class AperturasDocente(val uid: String, val nombre: String?, val aperturas: Int)

data class DocenteEnRiesgo(val uid: String, val nombre: String?, val semanaPasada: Int, val semanaHabitual: Double)

data class DocentesEnRiesgo(val ultimaSemana: String, val docentes: List<DocenteEnRiesgo>) {

    fun aJson(): String = JSONObject()
        .put("lastWeek", ultimaSemana)
        .put("teachers", JSONArray().apply {
            docentes.forEach {
                put(
                    JSONObject().put("uid", it.uid).put("name", it.nombre ?: JSONObject.NULL)
                        .put("lastWeek", it.semanaPasada).put("usualWeek", it.semanaHabitual)
                )
            }
        })
        .toString()

    companion object {
        fun deJson(texto: String): DocentesEnRiesgo {
            val o = JSONObject(texto)
            val arreglo = o.getJSONArray("teachers")
            return DocentesEnRiesgo(o.getString("lastWeek"), List(arreglo.length()) {
                val d = arreglo.getJSONObject(it)
                DocenteEnRiesgo(
                    uid = d.getString("uid"),
                    nombre = if (d.isNull("name")) null else d.getString("name"),
                    semanaPasada = d.getInt("lastWeek"),
                    semanaHabitual = d.getDouble("usualWeek")
                )
            })
        }

        fun calcular(ultimaSemana: String, semanas: List<List<AperturasDocente>>): DocentesEnRiesgo {
            val aperturas = LinkedHashMap<String, IntArray>()
            val nombres = HashMap<String, String>()
            semanas.forEachIndexed { i, semana ->
                semana.forEach { d ->
                    aperturas.getOrPut(d.uid) { IntArray(semanas.size) }[i] = d.aperturas
                    if (d.nombre != null) nombres.putIfAbsent(d.uid, d.nombre)
                }
            }
            val docentes = aperturas.mapNotNull { (uid, porSemana) ->
                val riesgo = RiesgoAbandono.desdeAperturas(porSemana.toList())
                if (riesgo.enRiesgo) DocenteEnRiesgo(uid, nombres[uid], riesgo.semanaPasada, riesgo.semanaHabitual) else null
            }
            return DocentesEnRiesgo(ultimaSemana, docentes.sortedByDescending { it.semanaHabitual - it.semanaPasada })
        }
    }
}