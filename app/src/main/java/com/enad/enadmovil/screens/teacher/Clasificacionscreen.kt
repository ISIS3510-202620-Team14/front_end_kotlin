package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.theme.EnadBorder
import com.enad.enadmovil.ui.theme.EnadMovilTheme

// Paso 1: solo el esqueleto (flecha atrás + título + subtítulo), sin chips ni lista
// todavía. "materia" ya queda parametrizada para que Lectura y Matemáticas compartan
// esta misma pantalla más adelante (solo cambia el título por ahora).
data class MateriaConfig(val titulo: String)

val MATERIA_LECTURA = MateriaConfig(titulo = "Lectura")
val MATERIA_MATEMATICAS = MateriaConfig(titulo = "Matemáticas")

// Paso 2: chips "CURSO DE ORIGEN" (mismo patrón visual que en Asistencia). Todavía
// no filtran ninguna lista de estudiantes, porque esa lista no existe aún.
private val CLASIFICACION_CURSOS = listOf("Todos", "Grado 3", "Grado 4", "Grado 5")

@Composable
fun ClasificacionScreen(
    materia: MateriaConfig,
    totalEstudiantes: Int = 8,
    onBack: () -> Unit = {}
) {
    var cursoSeleccionado by remember { mutableStateOf(CLASIFICACION_CURSOS.first()) }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
        ) {
            IconButton(onClick = onBack, modifier = Modifier.padding(top = 4.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = materia.titulo,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "$totalEstudiantes de $totalEstudiantes estudiantes activos",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "CURSO DE ORIGEN",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            ClasificacionCursoChips(
                seleccionado = cursoSeleccionado,
                onSeleccionar = { cursoSeleccionado = it }
            )
        }
    }
}

@Composable
private fun ClasificacionCursoChips(seleccionado: String, onSeleccionar: (String) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CLASIFICACION_CURSOS.forEach { opcion ->
            val estaSeleccionado = opcion == seleccionado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .background(
                        color = if (estaSeleccionado) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = RoundedCornerShape(20.dp)
                    )
                    .let {
                        if (estaSeleccionado) it else it.border(1.dp, EnadBorder, RoundedCornerShape(20.dp))
                    }
                    .clickable { onSeleccionar(opcion) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                if (estaSeleccionado) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = opcion,
                    fontSize = 13.sp,
                    fontWeight = if (estaSeleccionado) FontWeight.Bold else FontWeight.Normal,
                    color = if (estaSeleccionado) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ClasificacionLecturaPreview() {
    EnadMovilTheme {
        ClasificacionScreen(materia = MATERIA_LECTURA)
    }
}

@Preview(showBackground = true)
@Composable
private fun ClasificacionMatematicasPreview() {
    EnadMovilTheme {
        ClasificacionScreen(materia = MATERIA_MATEMATICAS)
    }
}
