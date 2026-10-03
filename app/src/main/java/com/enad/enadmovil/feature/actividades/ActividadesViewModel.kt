package com.enad.enadmovil.feature.actividades

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.enad.enadmovil.domain.model.Actividad
import com.enad.enadmovil.domain.model.ActivitySource
import com.enad.enadmovil.domain.model.ActivityTemplate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
class ActividadesViewModel : ViewModel() {

    private val repository = ActividadesRepository()

    private val _uiState = MutableStateFlow(ActividadesUiState())
    val uiState: StateFlow<ActividadesUiState> = _uiState.asStateFlow()

    val library: List<ActivityTemplate>
        get() = ACTIVITY_LIBRARY.filter {
            it.subject == _uiState.value.selectedSubject
        }

    init {
        cargarDatos()
    }

    fun seleccionarMateria(subject: String) {
        _uiState.value = _uiState.value.copy(
            selectedSubject = subject
        )
    }

    fun cargarUso() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingUsage = true,
                usageError = null
            )

            runCatching {
                repository.cargarUso()
            }.onSuccess { usage ->
                _uiState.value = _uiState.value.copy(
                    usage = usage,
                    loadingUsage = false
                )
            }.onFailure {
                _uiState.value = _uiState.value.copy(
                    loadingUsage = false,
                    usageError = "No pudimos cargar tu historial. Revisa tu conexión."
                )
            }
        }
    }

    private fun cargarDatos() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                loadingUsage = true,
                usageError = null
            )

            val usageResult = runCatching {
                repository.cargarUso()
            }

            val plannedResult = runCatching {
                repository.cargarActividadesPlaneadas()
            }

            usageResult
                .onSuccess { usage ->
                    _uiState.value = _uiState.value.copy(
                        usage = usage
                    )
                }
                .onFailure { error ->
                    android.util.Log.e(
                        "ActividadesVM",
                        "ERROR cargarUso",
                        error
                    )
                }

            plannedResult
                .onSuccess { planned ->
                    _uiState.value = _uiState.value.copy(
                        planned = planned
                    )
                }
                .onFailure { error ->
                    android.util.Log.e(
                        "ActividadesVM",
                        "ERROR cargarActividadesPlaneadas",
                        error
                    )
                }

            val error = usageResult.exceptionOrNull()
                ?: plannedResult.exceptionOrNull()

            _uiState.value = _uiState.value.copy(
                loadingUsage = false,
                usageError = error?.let {
                    "${it::class.simpleName}: ${it.message}"
                }
            )
        }
    }
    fun usarActividadDeBiblioteca(template: ActivityTemplate) {
        val activity = Actividad(
            id = "${template.id}-${System.currentTimeMillis()}",
            title = template.title,
            subject = template.subject,
            minutes = template.minutes,
            source = ActivitySource.LIBRARY,
            templateId = template.id
        )

        registrarYAgregar(activity)
    }

    fun crearActividadPropia(title: String, minutes: Int?): String? {
        val cleanTitle = title.trim()

        if (cleanTitle.length < 3) {
            return "Escribe un nombre para la actividad."
        }

        if (minutes == null || minutes <= 0) {
            return "Indica la duración en minutos."
        }

        val activity = Actividad(
            id = "custom-${System.currentTimeMillis()}",
            title = cleanTitle,
            subject = _uiState.value.selectedSubject,
            minutes = minutes,
            source = ActivitySource.CUSTOM
        )

        registrarYAgregar(activity)
        return null
    }

    private fun registrarYAgregar(activity: Actividad) {
        val current = _uiState.value

        val usage = when (activity.source) {
            ActivitySource.LIBRARY -> current.usage.copy(
                libraryCount = current.usage.libraryCount + 1
            )

            ActivitySource.CUSTOM -> current.usage.copy(
                customCount = current.usage.customCount + 1
            )
        }

        _uiState.value = current.copy(
            usage = usage,
            planned = listOf(activity) + current.planned
        )

        viewModelScope.launch {
            runCatching {
                repository.registrarActividadSeleccionada(activity)
            }
        }
    }

    companion object {
        val ACTIVITY_LIBRARY = listOf(
            ActivityTemplate(
                id = "lec-01",
                title = "Lectura en voz alta por turnos",
                subject = "Lectura",
                minutes = 20,
                description = "Cada estudiante lee un párrafo y el grupo comenta."
            ),
            ActivityTemplate(
                id = "lec-02",
                title = "Palabras nuevas del cuento",
                subject = "Lectura",
                minutes = 15,
                description = "Subrayar palabras desconocidas y adivinar su significado."
            ),
            ActivityTemplate(
                id = "lec-03",
                title = "Cambia el final",
                subject = "Lectura",
                minutes = 30,
                description = "Escribir un final distinto para la historia leída."
            ),
            ActivityTemplate(
                id = "mat-01",
                title = "Tienda del salón",
                subject = "Matemáticas",
                minutes = 30,
                description = "Comprar y dar vueltas con billetes de papel."
            ),
            ActivityTemplate(
                id = "mat-02",
                title = "Cálculo mental en parejas",
                subject = "Matemáticas",
                minutes = 15,
                description = "Sumas y restas rápidas con tarjetas."
            ),
            ActivityTemplate(
                id = "mat-03",
                title = "Medir el salón",
                subject = "Matemáticas",
                minutes = 25,
                description = "Medir objetos con pasos, cuartas y metro."
            )
        )
    }
}

data class ActividadesUiState(
    val selectedSubject: String = "Matemáticas",
    val usage: ActivityUsage = ActivityUsage(),
    val planned: List<Actividad> = emptyList(),
    val loadingUsage: Boolean = false,
    val usageError: String? = null
)