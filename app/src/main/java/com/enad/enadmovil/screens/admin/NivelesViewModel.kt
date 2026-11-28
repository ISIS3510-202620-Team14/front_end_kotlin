package com.enad.enadmovil.ui.screens.admin

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.data.repository.AuthRepository
import com.enad.enadmovil.data.repository.NivelesRepository
import com.enad.enadmovil.data.telemetria.InfoDispositivo
import com.enad.enadmovil.domain.model.EstadoNiveles
import com.enad.enadmovil.domain.model.FilaNiveles
import com.enad.enadmovil.domain.model.ResumenNiveles
import com.enad.enadmovil.domain.model.estadoSinConexion
import com.enad.enadmovil.domain.model.estadoTrasDescarga
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NivelesUiState(
    val materia: String = "lectura",
    val grado: Int = 0, // 0 = todos los grados
    val filas: List<FilaNiveles> = emptyList(),
    val estado: EstadoNiveles = EstadoNiveles.CARGANDO,
    val hechas: Int = 0,
    val total: Int = 0,
    val fallidas: Int = 0,
    val volvioConexion: Boolean = false,
    val ultimoIntento: Long? = null
) {
    val resumen: ResumenNiveles get() = ResumenNiveles(filas)
}

/**
 * EVENTUAL CONNECTIVITY (Sprint 4): caché y si no red, más un respaldo genérico. Primero se muestra
 * lo guardado en SQLite; la red solo se usa con archivos de caché vencidos (30 min) o al tocar
 * Actualizar. Revisa la conexión antes de pedir, marca las instituciones que no se actualizaron y,
 * cuando vuelve el internet, ofrece Actualizar para que el admin decida cuándo gastar datos.
 */
class NivelesViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = NivelesRepository(application)
    private val prefs = application.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val _uiState = MutableStateFlow(NivelesUiState())
    val uiState: StateFlow<NivelesUiState> = _uiState.asStateFlow()
    private val uid get() = AuthRepository().uidActual() ?: "sin-sesion"

    private val conectividad = application.getSystemService(ConnectivityManager::class.java)
    private val alVolverRed = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            val estado = _uiState.value.estado
            if (estado == EstadoNiveles.SIN_CONEXION || estado == EstadoNiveles.VACIO) {
                Log.d(TAG, "Conectividad: volvió la conexión")
                _uiState.update { it.copy(volvioConexion = true) }
            }
        }
    }

    init {
        viewModelScope.launch { iniciar() }
        // En Android 6 no existe este aviso; ahí solo se actualiza con el botón
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            runCatching { conectividad?.registerDefaultNetworkCallback(alVolverRed) }
        }
    }

    private suspend fun iniciar() {
        // LOCAL STORAGE (Sprint 4): SharedPreferences guarda el último filtro del admin
        val materia = prefs.getString(claveMateria(), "lectura") ?: "lectura"
        val grado = prefs.getInt(claveGrado(), 0)
        Log.d(TAG, "SharedPreferences: filtro guardado = $materia, grado $grado")
        _uiState.update { it.copy(materia = materia, grado = grado) }
        mostrar()
        if (enLinea()) {
            actualizar()
        } else {
            Log.d(TAG, "Sin conexión: se muestra lo guardado en SQLite")
            _uiState.update { it.copy(estado = estadoSinConexion(it.filas.isNotEmpty())) }
        }
    }

    fun elegir(materia: String? = null, grado: Int? = null) {
        _uiState.update { it.copy(materia = materia ?: it.materia, grado = grado ?: it.grado) }
        val estado = _uiState.value
        prefs.edit().putString(claveMateria(), estado.materia).putInt(claveGrado(), estado.grado).apply()
        viewModelScope.launch { mostrar() }
    }

    /** [forzar] = true se salta los archivos de caché (botón Actualizar del encabezado). */
    fun actualizar(forzar: Boolean = false) {
        if (_uiState.value.estado == EstadoNiveles.ACTUALIZANDO) return
        if (!enLinea()) {
            Log.d(TAG, "Actualizar sin conexión: no se lanza ninguna petición")
            _uiState.update { it.copy(estado = estadoSinConexion(it.filas.isNotEmpty())) }
            return
        }
        Log.d(TAG, if (forzar) "Actualizar: se salta la caché" else "Actualizar")
        _uiState.update { it.copy(estado = EstadoNiveles.ACTUALIZANDO, volvioConexion = false, hechas = 0, fallidas = 0) }
        viewModelScope.launch {
            val estado = try {
                val descarga = repo.descargar(uid, forzar) { hechas, total ->
                    _uiState.update { it.copy(hechas = hechas, total = total) }
                    viewModelScope.launch { mostrar() } // cada institución aparece apenas está lista
                }
                _uiState.update { it.copy(fallidas = descarga.fallidas, ultimoIntento = descarga.iniciadaEn) }
                mostrar()
                estadoTrasDescarga(_uiState.value.filas.isNotEmpty(), descarga.total, descarga.fallidas)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.d(TAG, "No se pudo actualizar: ${e.message}")
                estadoSinConexion(_uiState.value.filas.isNotEmpty())
            }
            _uiState.update { it.copy(estado = estado) }
        }
    }

    private suspend fun mostrar() {
        val estado = _uiState.value
        val filas = runCatching { repo.filas(uid, estado.materia, estado.grado) }.getOrDefault(estado.filas)
        _uiState.update { if (it.materia == estado.materia && it.grado == estado.grado) it.copy(filas = filas) else it }
    }

    private fun enLinea(): Boolean = InfoDispositivo.conectividad(getApplication()) == InfoDispositivo.ONLINE

    private fun claveMateria() = "niveles_materia_$uid"
    private fun claveGrado() = "niveles_grado_$uid"

    override fun onCleared() {
        // Ningún listener queda vivo después de la pantalla
        runCatching { conectividad?.unregisterNetworkCallback(alVolverRed) }
    }

    private companion object {
        const val PREFS = "enad_niveles"
        const val TAG = "niveles"
    }
}
