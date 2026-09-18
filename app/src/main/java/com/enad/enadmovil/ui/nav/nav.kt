package com.enad.enadmovil.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enad.enadmovil.ui.screens.admin.AdminHomeScreen
import com.enad.enadmovil.ui.screens.auth.LoginScreen
import com.enad.enadmovil.ui.screens.foundation.FoundationReportsScreen
import com.enad.enadmovil.ui.screens.teacher.AsistenciaScreen
import com.enad.enadmovil.ui.screens.teacher.TeacherHomeScreen

private object Routes {
    const val LOGIN = "login"
    const val TEACHER_HOME = "teacher_home"
    const val ADMIN_HOME = "admin_home"
    const val FOUNDATION_REPORTS = "foundation_reports"
    const val ASISTENCIA = "asistencia"
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
                onCambiarUsuario = { volverALogin(navController) },
                onTabClick = { tab ->
                    if (tab.label == "Mi lista") {
                        navController.navigate(Routes.ASISTENCIA)
                    }
                }
            )
        }

        composable(Routes.ASISTENCIA) {
            AsistenciaScreen(
                profesorNombre = nombreUsuario,
                onCambiarUsuario = { volverALogin(navController) }
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