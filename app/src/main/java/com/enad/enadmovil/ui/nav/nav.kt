package com.enad.enadmovil.ui.nav

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
}

// Mapeo temporal usando las mismas credenciales de la tarjeta "Credenciales de
// prueba" de LoginScreen. Todavía no hay autenticación real: esto solo decide
// a qué pantalla navegar en esta entrega visual.
private fun rolParaUsuario(usuario: String): String? = when (usuario.trim().lowercase()) {
    "mateo" -> Routes.TEACHER_HOME
    "sofia" -> Routes.FOUNDATION_REPORTS
    "aifos" -> Routes.ADMIN_HOME // voluntario sigue en el layout de admin por ahora
    else -> null
}

// "Cambiar usuario" limpia todo el back stack y regresa a Login, sin importar
// desde qué pantalla se llame. Así no queda historial para volver con el botón
// atrás a una sesión anterior. mujejejejeeee locuraaaa
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
                onEntrarClick = { usuario, _ ->
                    val destino = rolParaUsuario(usuario)
                    if (destino != null) {
                        nombreUsuario = usuario.trim().replaceFirstChar { it.uppercase() }
                        navController.navigate(destino) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen(
                profesorNombre = nombreUsuario,
                tabSeleccionado = teacherTabSeleccionado,
                onTabSeleccionadoChange = { teacherTabSeleccionado = it },
                onCambiarUsuario = { volverALogin(navController) },
                onTabClick = { tab ->
                    if (tab.label == "Mi lista") {
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
                    if (item.title == "Tomar asistencia") {
                        navController.navigate(Routes.ASISTENCIA)
                    }
                }
            )
        }

        composable(Routes.CLASIFICACION_LECTURA) {
            ClasificacionScreen(
                materia = MATERIA_LECTURA,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CLASIFICACION_MATEMATICAS) {
            ClasificacionScreen(
                materia = MATERIA_MATEMATICAS,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.ASISTENCIA) {
            AsistenciaScreen(
                profesorNombre = nombreUsuario,
                onCambiarUsuario = { volverALogin(navController) },
                onTabClick = { label ->
                    // "Mi lista" es esta misma pantalla, no hace nada. Grupos y Horas
                    // viven como tabs dentro de TeacherHomeScreen, así que antes de
                    // regresar le decimos a cuál pestaña saltar.
                    when (label) {
                        "Mi lista" -> {}
                        "Grupos" -> {
                            teacherTabSeleccionado = TeacherTabs.GRUPOS
                            navController.popBackStack()
                        }
                        "Horas" -> {
                            teacherTabSeleccionado = TeacherTabs.HORAS
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

        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen(
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