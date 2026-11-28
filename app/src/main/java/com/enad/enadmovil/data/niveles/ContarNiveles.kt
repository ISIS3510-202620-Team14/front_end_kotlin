package com.enad.enadmovil.data.niveles

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/*
 * MULTITHREADING (Sprint 4): decodificar el JSON de cientos de estudiantes y contarlos es trabajo de
 * CPU. Con una corrutina normal seguiría en el hilo principal y congelaría la pantalla, así que cada
 * institución se cuenta en Dispatchers.Default, el pool de hilos para trabajo de CPU, mientras la UI
 * sigue dibujando. La función solo recibe el texto del JSON y devuelve un IntArray nuevo, así que no
 * comparte estado con otros hilos.
 */

/** Decodifica y cuenta una institución: los 42 conteos de NivelesInstitucion. */
fun contarNiveles(estudiantesJson: String): IntArray = contarEstudiantes(leerEstudiantes(estudiantesJson))

/**
 * Cuenta una institución fuera del hilo principal. Quien descarga varias instituciones puede lanzar
 * una por cada descarga con async, y se cuentan en paralelo.
 */
suspend fun contarEnSegundoPlano(
    estudiantesJson: String,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    contar: (String) -> IntArray = ::contarNiveles
): IntArray = withContext(dispatcher) { contar(estudiantesJson) }
