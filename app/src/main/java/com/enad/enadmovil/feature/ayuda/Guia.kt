package com.enad.enadmovil.feature.ayuda

enum class Guia(val titulo: String) {
    HORAS("Reportar horas"),
    ASISTENCIA("Reportar asistencia"),
    EVALUACION("Evaluación formativa"),
    ACTIVIDADES("Planear actividades")
}

data class PasoGuia(val titulo: String, val texto: String, val continuar: Boolean = false, val opcional: Boolean = false)

val PASOS_GUIA: Map<Guia, List<PasoGuia>> = mapOf(
    Guia.HORAS to listOf(
        PasoGuia("Abre Reportar horas", "Toca el botón resaltado para empezar."),
        PasoGuia("Revisa la fecha", "Elige el día de la jornada. Si la fecha ya es correcta, continúa.", continuar = true),
        PasoGuia("Horas programadas", "Escribe cuántas horas tenías programadas para ese día.", continuar = true),
        PasoGuia("Horas realizadas", "Escribe cuántas horas realizaste. Usa los datos reales de tu jornada.", continuar = true),
        PasoGuia("Cuéntanos el motivo", "Selecciona por qué realizaste menos horas. Si eliges Otro, escribe una explicación.", continuar = true),
        PasoGuia("Envía tu reporte", "Pulsa Guardar. Si ya existe un reporte de ese día, tendrás que confirmar su actualización."),
        PasoGuia("¡Reporte guardado!", "Terminaste la guía de Horas. Puedes volver a consultarla desde Ayuda.")
    ),
    Guia.ASISTENCIA to listOf(
        PasoGuia("Abre Reportar asistencia", "Toca el botón resaltado."),
        PasoGuia("Revisa el curso y la fecha", "Usa Editar para elegir el curso. Ahí también puedes cambiar la fecha.", continuar = true),
        PasoGuia("Añade estudiantes (opcional)", "Si necesitas registrar estudiantes desde una lista, abre Editar, luego Estudiantes, y usa el escáner. Puedes omitirlo.", continuar = true, opcional = true),
        PasoGuia("Marca la asistencia", "En cada ficha elige Asistió o No asistió. Revisa también los registrados.", continuar = true),
        PasoGuia("Agrega a quien falte (opcional)", "Si alguien no aparece, pulsa Agregar estudiante a la sesión. Si están todos, omite este paso.", continuar = true, opcional = true),
        PasoGuia("Guarda la sesión", "Pulsa Guardar para enviar las marcas. La guía termina cuando se confirma el envío."),
        PasoGuia("¡Asistencia enviada!", "La asistencia de esta sesión se envió correctamente.")
    ),
    Guia.EVALUACION to listOf(
        PasoGuia("Abre Evaluación formativa", "Toca el botón resaltado."),
        PasoGuia("Elige el área", "Selecciona Lectura o Matemáticas."),
        PasoGuia("Elige el momento", "Selecciona el momento de evaluación que vas a registrar."),
        PasoGuia("Evalúa a un estudiante", "Abre la ficha con la flecha, completa los datos, elige el nivel observado y pulsa Guardar evaluación. Si no hay estudiantes, revisa el curso."),
        PasoGuia("¡Evaluación guardada!", "El resultado del estudiante quedó guardado.")
    ),
    Guia.ACTIVIDADES to listOf(
        PasoGuia("Abre Planear actividades", "Toca el botón resaltado."),
        PasoGuia("Elige el área", "Selecciona Matemáticas o Lectura. Después continúa.", continuar = true),
        PasoGuia("Añade una actividad", "Elige una actividad de la biblioteca y pulsa Usar para añadirla al plan de hoy. Si está vacía, cambia de área."),
        PasoGuia("¡Actividad agregada!", "La actividad quedó agregada a tu plan de hoy.")
    )
)