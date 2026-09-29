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

// Roles that already have a screen to navigate to. "sin_privilegios" (the role
// every new account is born with) is deliberately left out.
private val ROLES_CON_PANTALLA = setOf(Rol.DOCENTE, Rol.ADMINISTRADOR, Rol.VOLUNTARIO)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun onUsuarioChange(valor: String) {
        _uiState.update { it.copy(usuario = valor, error = null) }
    }

    fun onContrasenaChange(valor: String) {
        _uiState.update { it.copy(contrasena = valor, error = null) }
    }

    fun onNombreCompletoChange(valor: String) {
        _uiState.update { it.copy(nombreCompleto = valor, error = null) }
    }

    fun onMostrarContrasenaToggle() {
        _uiState.update { it.copy(mostrarContrasena = !it.mostrarContrasena) }
    }

    fun onToggleModoRegistro() {
        _uiState.update { it.copy(modoRegistro = !it.modoRegistro, error = null) }
    }

    /**
     * Real login or register (depending on modoRegistro), through AuthRepository.
     * If the role the backend returns has no screen assigned yet (e.g. a
     * brand-new account, which is always born "sin_privilegios"), [onExito] is
     * NOT called — the state is left with a message explaining why instead of
     * navigating somewhere that doesn't exist.
     */
    fun onSubmitClick(onExito: (Usuario) -> Unit) {
        val estado = _uiState.value

        if (estado.usuario.isBlank() || estado.contrasena.isBlank() ||
            (estado.modoRegistro && estado.nombreCompleto.isBlank())
        ) {
            _uiState.update {
                it.copy(
                    error = if (estado.modoRegistro) {
                        "Completa tu nombre, usuario y contraseña."
                    } else {
                        "Ingresa tu usuario y tu contraseña."
                    }
                )
            }
            return
        }

        _uiState.update { it.copy(cargando = true, error = null) }

        viewModelScope.launch {
            try {
                val usuario = if (estado.modoRegistro) {
                    authRepository.registrarse(
                        email = estado.usuario.trim(),
                        password = estado.contrasena,
                        fullName = estado.nombreCompleto.trim()
                    )
                } else {
                    authRepository.iniciarSesion(
                        email = estado.usuario.trim(),
                        password = estado.contrasena
                    )
                }

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
                // Message already comes from the Cloud Function (e.g. invalid credentials,
                // email already registered).
                _uiState.update { it.copy(cargando = false, error = e.message ?: "Ocurrió un error. Intenta de nuevo.") }
            } catch (e: Exception) {
                // No internet, timeout, URL down, etc.
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