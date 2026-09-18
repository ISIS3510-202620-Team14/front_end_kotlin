package com.enad.enadmovil.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.enad.enadmovil.ui.screens.admin.AdminHomeScreen
import com.enad.enadmovil.ui.screens.auth.LoginScreen
import com.enad.enadmovil.ui.screens.teacher.TeacherHomeScreen

private object Routes {
    const val LOGIN = "login"
    const val TEACHER_HOME = "teacher_home"
    const val ADMIN_HOME = "admin_home"
}

// Mapeo temporal usando las mismas credenciales de la tarjeta "Credenciales de
// prueba" de LoginScreen. Todavía no hay autenticación real: esto solo decide
// a qué pantalla navegar en esta entrega visual.
private fun rolParaUsuario(usuario: String): String? = when (usuario.trim().lowercase()) {
    "mateo" -> Routes.TEACHER_HOME
    "sofia" -> Routes.ADMIN_HOME
    "aifos" -> Routes.ADMIN_HOME // voluntario reutiliza el layout de admin por ahora
    else -> null
}

@Composable
fun AppNavigation(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = Routes.LOGIN) {

        composable(Routes.LOGIN) {
            LoginScreen(
                onEntrarClick = { usuario, _ ->
                    val destino = rolParaUsuario(usuario)
                    if (destino != null) {
                        navController.navigate(destino) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                    // TODO: si el usuario no coincide con ninguna credencial demo,
                    // por ahora simplemente no navega. Falta mostrar error visual.
                }
            )
        }

        composable(Routes.TEACHER_HOME) {
            TeacherHomeScreen()
        }

        composable(Routes.ADMIN_HOME) {
            AdminHomeScreen()
        }
    }
}