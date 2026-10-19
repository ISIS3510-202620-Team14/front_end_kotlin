package com.enad.enadmovil.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta ENAd del Sprint 3, igual a AppTheme de la app Flutter
// (rama feature/enad-feedback-review, lib/theme/app_theme/app_theme.dart).

val EnadBackground = Color(0xFFFAF6F3)   // fondo cálido de todas las pantallas
val EnadSurface = Color(0xFFFFFFFF)      // tarjetas / inputs
val EnadInk = Color(0xFF1D1D1B)          // texto principal, selección y confirmación
val EnadSoftInk = Color(0xFF50504D)      // texto secundario y bordes de inputs
val EnadLine = Color(0xFFE2E2E2)         // bordes suaves y botones deshabilitados
val EnadPrimary = Color(0xFFE52228)      // rojo de acciones (botón principal, iconos)
val EnadNeutral = Color(0xFFE9E9E6)      // chip seleccionado, nivel "Sin evaluar"
val EnadNeutralHigh = Color(0xFFDEDEDA)

val EnadHeader = EnadInk                 // barra superior negra
val EnadHeaderChip = Color(0xFF3A3835)   // botón sobre el header
val EnadHeaderMuted = Color(0xFFAAA6A3)  // texto secundario y borde sobre el header ("Portal docente")
val EnadOnBackground = EnadInk
val EnadMuted = EnadSoftInk
val EnadBorder = EnadLine
val EnadTrack = EnadLine                 // fondo de las barras de progreso
val EnadCardAlt = EnadNeutral

// Estados (mismos valores que Flutter).
val EnadPillBg = Color(0xFFD7ECDD)       // "Asistió" / evaluado
val EnadPillText = Color(0xFF2E6B45)
val EnadPendienteBg = Color(0xFFF4E3C8)  // "Pendiente"
val EnadPendienteText = Color(0xFF8A5A22)
val EnadNoAsistioBg = Color(0xFFF6D9D6)
val EnadAmber = Color(0xFFB4700F)
val EnadAmberBg = Color(0xFFFBF0DE)
val EnadPending = Color(0xFF955D0C)
val EnadSuccessBg = Color(0xFFEAF3EC)
val EnadSuccessText = Color(0xFF3E7C59)
val EnadChipPink = Color(0xFFF6DAD6)

// Escala de seis niveles (LevelPalette en Flutter). Lectura y Matemáticas comparten colores.
val EnadLevel1 = Color(0xFFB72619)       // Principiante
val EnadLevel2 = Color(0xFFCC6B68)       // Letra · 1 dígito
val EnadLevel3 = Color(0xFFF0BC40)       // Palabra · 2 dígitos
val EnadLevel4 = Color(0xFFF9D876)       // Párrafo · Resta
val EnadLevel5 = Color(0xFF9BC184)       // Cuento · División
val EnadLevel6 = Color(0xFF56A359)       // Comprensión · Problema escrito

// Heatmap de horas: se elimina en la Fase 7; mientras tanto usa la escala neutra/roja.
val EnadHeatmapEmpty = EnadNeutral
val EnadHeatmapStep2 = EnadLevel2
val EnadHeatmapStep3 = EnadLevel1
