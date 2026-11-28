package com.enad.enadmovil.data.cache

import androidx.collection.SparseArrayCompat
import com.enad.enadmovil.domain.model.Recomendacion

class CacheRecomendaciones {
    private val porMateria = HashMap<String, SparseArrayCompat<Recomendacion>>(2)

    var aciertos = 0
        private set
    var fallos = 0
        private set

    @Synchronized
    fun obtener(materia: String, tamano: Int): Recomendacion? {
        val recomendacion = porMateria[materia]?.get(tamano)
        if (recomendacion != null) aciertos++ else fallos++
        return recomendacion
    }

    @Synchronized
    fun guardar(materia: String, tamano: Int, recomendacion: Recomendacion) {
        porMateria.getOrPut(materia) { SparseArrayCompat() }.put(tamano, recomendacion)
    }

    @Synchronized
    fun tamanos(materia: String): Int = porMateria[materia]?.size() ?: 0

    @Synchronized
    fun invalidar(materia: String) {
        porMateria.remove(materia)
    }

    @Synchronized
    fun limpiar() {
        porMateria.clear()
    }
}