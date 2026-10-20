package com.enad.enadmovil.ui.nav

import com.enad.enadmovil.ui.screens.teacher.ACCION_ASISTENCIA
import com.enad.enadmovil.ui.screens.teacher.ACCION_EVALUACION
import com.enad.enadmovil.ui.screens.teacher.EvaluacionMenuScreen
import com.enad.enadmovil.ui.screens.teacher.GestionarEstudiantesScreen
import com.enad.enadmovil.ui.screens.teacher.HistorialEstudianteScreen
import com.enad.enadmovil.ui.screens.teacher.PerfilEstudianteScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enad.enadmovil.analytics.RegistrarAperturaEffect
import com.enad.enadmovil.domain.model.Rol
import com.enad.enadmovil.ui.screens.admin.AdminHomeScreen
import com.enad.enadmovil.ui.screens.auth.LoginScreen
import com.enad.enadmovil.ui.screens.foundation.FoundationReportsScreen
import com.enad.enadmovil.ui.screens.teacher.AsistenciaScreen
import com.enad.enadmovil.ui.screens.teacher.ClasificacionScreen
import com.enad.enadmovil.ui.screens.teacher.MATERIA_LECTURA
import com.enad.enadmovil.ui.screens.teacher.MATERIA_MATEMATICAS
import com.enad.enadmovil.ui.screens.teacher.TeacherHomeScreen
import com.enad.enadmovil.ui.screens.teacher.TeacherTabs

private object Routes {
    const val LOGIN = "login"
    const val TEACHER_HOME = "teacher_home"
    const val ADMIN_HOME = "admin_home"
    const val FOUNDATION_REPORTS = "foundation_reports"
    const val ASISTENCIA = "asistencia"
    const val CLASIFICACION_LECTURA = "clasificacion_lectura"
    const val CLASIFICACION_MATEMATICAS = "clasificacion_matematicas"
    const val EVALUACION = "evaluacion"
    const val PERFIL_ESTUDIANTE = "estudiante/{id}"
    const val HISTORIAL_ESTUDIANTE = "estudiante/{id}/historial"
    const val ESTUDIANTES = "estudiantes"
    // Desde Asistencia: el código puede quedar vacío (provisional), como "unexpected" en Flutter.
    const val ESTUDIANTES_INESPERADO = "estudiantes/inesperado"

    fun perfil(id: String) = "estudiante/${android.net.Uri.encode(id)}"
    fun historial(id: String) = "estudiante/${android.net.Uri.encode(id)}/historial"
}

// "Cambiar usuario" limpia todo el back stack y regresa a Login, sin importar
// desde qué pantalla se llame. Así no queda historial para volver con el botón
// atrás a una sesión anterior.
private fun volverALogin(navController: NavHostController) {
    navController.navigate(Routes.LOGIN) {
        popUpTo(navController.graph.id) { inclusive = true }
    }
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    var nombreUsuario by remember { mutableStateOf("") }
    // Vive aquí (no dentro de TeacherHomeScreen) para que sobreviva cuando navegas a
    // Asistencia/Clasificación y vuelves, y para poder saltar a un tab específico
    // (ej. Horas) desde otra pantalla en vez de siempre caer en "Hoy".
    var teacherTabSeleccionado by rememberSaveable { mutableStateOf(TeacherTabs.HOY) }

    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onLoginExitoso = { usuario ->
                    nombreUsuario = usuario.fullName.ifBlank { usuario.email }
                    val destino = when (usuario.rol) {
                        Rol.DOCENTE -> Routes.TEACHER_HOME
                        Rol.ADMINISTRADOR -> Routes.ADMIN_HOME
                        Rol.VOLUNTARIO -> Routes.FOUNDATION_REPORTS
                        else -> null // AuthViewModel already filters this out before calling here
                    }
                    if (destino != null) {
                        navController.navigate(destino) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.TEACHER_HOME) {
            RegistrarAperturaEffect()
            TeacherHomeScreen(
                profesorNombre = nombreUsuario,
                tabSeleccionado = teacherTabSeleccionado,
                onTabSeleccionadoChange = { teacherTabSeleccionado = it },
                onCambiarUsuario = { volverALogin(navController) },
                onGestionarEstudiantes = { navController.navigate(Routes.ESTUDIANTES) },
                onTabClick = { tab ->
                    if (tab.label == "Asistencia") {
                        navController.navigate(Routes.ASISTENCIA)
                    }
                },
                onProgresoClick = { item ->
                    when (item.title) {
                        "Lectura" -> navController.navigate(Routes.CLASIFICACION_LECTURA)
                        "Matemáticas" -> navController.navigate(Routes.CLASIFICACION_MATEMATICAS)
                    }
                },
                onAccionClick = { item ->
                    when (item.title) {
                        ACCION_ASISTENCIA -> navController.navigate(Routes.ASISTENCIA)
                        ACCION_EVALUACION -> navController.navigate(Routes.EVALUACION)
                    }
                }
            )
        }

        composable(Routes.EVALUACION) {
            EvaluacionMenuScreen(
                onBack = { navController.popBackStack() },
                onMateria = { titulo ->
                    when (titulo) {
                        "Lectura" -> navController.navigate(Routes.CLASIFICACION_LECTURA)
                        "Matemáticas" -> navController.navigate(Routes.CLASIFICACION_MATEMATICAS)
                    }
                }
            )
        }

        composable(Routes.CLASIFICACION_LECTURA) {
            ClasificacionScreen(
                materia = MATERIA_LECTURA,
                onBack = { navController.popBackStack() },
                onVerPerfil = { navController.navigate(Routes.perfil(it)) }
            )
        }

        composable(Routes.CLASIFICACION_MATEMATICAS) {
            ClasificacionScreen(
                materia = MATERIA_MATEMATICAS,
                onBack = { navController.popBackStack() },
                onVerPerfil = { navController.navigate(Routes.perfil(it)) }
            )
        }

        composable(Routes.ASISTENCIA) {
            AsistenciaScreen(
                profesorNombre = nombreUsuario,
                onCambiarUsuario = { volverALogin(navController) },
                onAbrirEstudiante = { navController.navigate(Routes.perfil(it)) },
                onGestionarEstudiantes = { navController.navigate(Routes.ESTUDIANTES_INESPERADO) },
                onTabClick = { label ->
                    // "Asistencia" es esta misma pantalla, no hace nada. Grupos y Horas
                    // viven como tabs dentro de TeacherHomeScreen, así que antes de
                    // regresar le decimos a cuál pestaña saltar.
                    when (label) {
                        "Asistencia" -> {}
                        "Grupos" -> {
                            teacherTabSeleccionado = TeacherTabs.GRUPOS
                            navController.popBackStack()
                        }
                        "Horas" -> {
                            teacherTabSeleccionado = TeacherTabs.HORAS
                            navController.popBackStack()
                        }
                        "Mis datos" -> {
                            teacherTabSeleccionado = TeacherTabs.MIS_DATOS
                            navController.popBackStack()
                        }
                        else -> {
                            teacherTabSeleccionado = TeacherTabs.HOY
                            navController.popBackStack()
                        }
                    }
                }
            )
        }

        composable(Routes.ESTUDIANTES) {
            GestionarEstudiantesScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ESTUDIANTES_INESPERADO) {
            GestionarEstudiantesScreen(onBack = { navController.popBackStack() }, inesperado = true)
        }

        composable(Routes.PERFIL_ESTUDIANTE) { entry ->
            val id = entry.arguments?.getString("id").orEmpty()
            PerfilEstudianteScreen(
                estudianteId = id,
                onBack = { navController.popBackStack() },
                onVerHistorial = { navController.navigate(Routes.historial(id)) }
            )
        }

        composable(Routes.HISTORIAL_ESTUDIANTE) { entry ->
            HistorialEstudianteScreen(
                estudianteId = entry.arguments?.getString("id").orEmpty(),
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
                nombreUsuario = nombreUsuario,
                onCambiarUsuario = { volverALogin(navController) }
            )
        }

        composable(Routes.FOUNDATION_REPORTS) {
            FoundationReportsScreen(
                onCambiarUsuario = { volverALogin(navController) }
            )
        }
    }
}