package com.enad.enadmovil.core.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today

enum class EnadDestination(val label: String, val icon: ImageVector) {
    HOY("Hoy", Icons.Default.Today),
    MI_LISTA("Mi lista", Icons.Default.ListAlt),
    GRUPOS("Grupos", Icons.Default.Groups),
    HORAS("Horas", Icons.Default.CalendarToday),
    MIS_DATOS("Mis datos", Icons.Default.Person),

}