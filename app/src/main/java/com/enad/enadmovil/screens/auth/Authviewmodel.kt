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

// Roles that already have a screen to navigate to. New accounts are born "docente";
// old ones may still be "sin_privilegios", which is deliberately left out.
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
        val estado = _uiState.value
        if (estado.modoRegistro && estado.instituciones.isEmpty() && !estado.cargandoInstituciones) {
            cargarInstituciones()
        }
    }

    /** Trae del back las instituciones (con sus sedes) que se pueden elegir al registrarse. */
    fun cargarInstituciones() {
        _uiState.update { it.copy(cargandoInstituciones = true) }
        viewModelScope.launch {
            try {
                val lista = CloudFunctionsApi.institucionesRegistro()
                _uiState.update { it.copy(instituciones = lista, cargandoInstituciones = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        cargandoInstituciones = false,
                        error = (e as? CloudFunctionsApi.ApiException)?.message
                            ?: "No pudimos cargar las instituciones. Revisa tu internet."
                    )
                }
            }
        }
    }

    /** Marca o desmarca una institución. Al desmarcarla se olvidan sus sedes. */
    fun onInstitucionToggle(institucionId: String) {
        _uiState.update {
            val elegidas = it.sedesElegidas.toMutableMap()
            if (elegidas.remove(institucionId) == null) elegidas[institucionId] = emptySet()
            it.copy(sedesElegidas = elegidas, error = null)
        }
    }

    /** Marca o desmarca una sede de una institución ya marcada. */
    fun onSedeToggle(institucionId: String, sedeId: String) {
        _uiState.update {
            val sedes = it.sedesElegidas[institucionId] ?: return@update it
            val nuevas = if (sedeId in sedes) sedes - sedeId else sedes + sedeId
            it.copy(sedesElegidas = it.sedesElegidas + (institucionId to nuevas), error = null)
        }
    }

    /** Mensaje si falta elegir institución o sede, o null si la selección está completa. */
    private fun faltaSeleccion(estado: AuthUiState): String? {
        if (estado.sedesElegidas.isEmpty()) return "Elige al menos una institución."
        val sinSede = estado.instituciones.firstOrNull { inst ->
            val sedes = estado.sedesElegidas[inst.id]
            sedes != null && inst.campuses.isNotEmpty() && sedes.isEmpty()
        }
        return sinSede?.let { "Elige al menos una sede de ${it.name}." }
    }

    /**
     * Real login or register (depending on modoRegistro), through AuthRepository.
     * If the role the backend returns has no screen assigned yet (e.g. an old
     * account still marked "sin_privilegios"), [onExito] is
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

        if (estado.modoRegistro) {
            faltaSeleccion(estado)?.let { mensaje ->
                _uiState.update { it.copy(error = mensaje) }
                return
            }
        }

        _uiState.update { it.copy(cargando = true, error = null, correoBienvenidaEnviado = false) }

        viewModelScope.launch {
            try {
                val usuario = if (estado.modoRegistro) {
                    val registro = authRepository.registrarse(
                        email = estado.usuario.trim(),
                        password = estado.contrasena,
                        fullName = estado.nombreCompleto.trim(),
                        sedesPorInstitucion = estado.sedesElegidas.mapValues { it.value.toList() }
                    )
                    _uiState.update { it.copy(correoBienvenidaEnviado = registro.correoBienvenidaEnviado) }
                    registro.usuario
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