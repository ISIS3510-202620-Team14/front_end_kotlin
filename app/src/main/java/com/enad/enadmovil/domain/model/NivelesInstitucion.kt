package com.enad.enadmovil.domain.model

/**
 * BQ (tipo 4): % de estudiantes evaluados que siguen en Principiante, por institución, en
 * Lectura y Matemáticas. Una institución cabe en 42 conteos: 2 materias x 3 grados x 7 niveles
 * (nivel 0 = todavía sin evaluar).
 */
object NivelesInstitucion {
    val MATERIAS = listOf("lectura", "matematicas")
    val GRADOS = listOf(3, 4, 5)
    const val NIVELES = 7
    const val TAMANO = 2 * 3 * NIVELES

    /** Posición de una celda dentro del IntArray de conteos. */
    fun indice(materia: Int, grado: Int, nivel: Int): Int = (materia * GRADOS.size + grado) * NIVELES + nivel
}

/** Una institución en pantalla, para la materia y el grado elegidos. */
class FilaNiveles(
    val schoolId: String,
    val nombre: String,
    val guardadoEn: Long, // epoch millis
    /** niveles[0] = sin evaluar, niveles[1..6] = estudiantes en cada nivel ENAd. */
    val niveles: IntArray = IntArray(NivelesInstitucion.NIVELES)
) {
    val pendientes: Int get() = niveles[0]

    val evaluados: Int
        get() {
            var total = 0
            for (nivel in 1 until NivelesInstitucion.NIVELES) total += niveles[nivel]
            return total
        }

    val principiantes: Int get() = niveles[1]

    /** Porcentaje de evaluados que siguen en Principiante; null si nadie ha sido evaluado. */
    val porcentajePrincipiante: Double?
        get() {
            val total = evaluados
            return if (total == 0) null else principiantes * 100.0 / total
        }

    companion object {
        /** La institución que necesita más apoyo va primero; con el mismo porcentaje, por nombre. */
        val porApoyoNecesario: Comparator<FilaNiveles> = Comparator { a, b ->
            val porPorcentaje = (b.porcentajePrincipiante ?: -1.0).compareTo(a.porcentajePrincipiante ?: -1.0)
            if (porPorcentaje != 0) porPorcentaje else a.nombre.compareTo(b.nombre)
        }
    }
}
