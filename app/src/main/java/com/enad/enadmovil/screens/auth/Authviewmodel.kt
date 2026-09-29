package com.enad.enadmovil.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.domain.model.Rol
import com.enad.enadmovil.domain.model.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Roles que ya tienen una pantalla a la que navegar. "sin_privilegios" (el rol
// con el que nace toda cuenta nueva) queda fuera a propósito.
private val ROLES_CON_PANTALLA = setOf(Rol.DOCENTE, Rol.ADMINISTRADOR, Rol.VOLUNTARIO)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onEmailChange(valor: String) {
        _uiState.update { it.copy(email = valor, error = null) }
    }

    fun onPasswordChange(valor: String) {
        _uiState.update { it.copy(password = valor, error = null) }
    }

    fun onMostrarContrasenaToggle() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    /**
     * Login real: Cloud Function + Firebase Auth (vía AuthRepository). Si el
     * rol que devuelve el backend no tiene pantalla asignada (ej. una cuenta
     * recién registrada, que siempre nace "sin_privilegios"), NO llama a
     * [onExito] — deja un mensaje explicando por qué en vez de navegar a un
     * lugar que no existe.
     */
    fun onEntrarClick(onExito: (Usuario) -> Unit) {
        val estado = _uiState.value
        if (estado.email.isBlank() || estado.password.isBlank()) {
            _uiState.update { it.copy(error = "Ingresa tu correo y tu contraseña.") }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            try {
                val usuario = authRepository.iniciarSesion(estado.email.trim(), estado.password)

                if (usuario.rol !in ROLES_CON_PANTALLA) {
                    _uiState.update {
                        it.copy(
                            cargando = false,
                            error = "Tu cuenta todavía no tiene un rol asignado " +
                                    "(rol actual: \"${usuario.rol}\"). Pide a un administrador " +
                                    "que te lo asigne en Firestore."
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(cargando = false) }
                onExito(usuario)
            } catch (e: CloudFunctionsApi.ApiException) {
                // Mensaje que ya viene de la Cloud Function (ej. credenciales inválidas).
                _uiState.update { it.copy(cargando = false, error = e.message ?: "Correo o contraseña incorrectos.") }
            } catch (e: Exception) {
                // Sin internet, timeout, URL caída, etc.
                _uiState.update {
                    it.copy(
                        cargando = false,
                        error = "No se pudo conectar con el servidor. Revisa tu internet e intenta de nuevo."
                    )
                }
            }
        }
    }
}