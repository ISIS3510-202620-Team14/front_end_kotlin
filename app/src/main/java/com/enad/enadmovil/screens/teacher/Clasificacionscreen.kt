package com.enad.enadmovil.ui.screens.teacher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.ui.theme.EnadMovilTheme

// Paso 1: solo el esqueleto (flecha atrás + título + subtítulo), sin chips ni lista
// todavía. "materia" ya queda parametrizada para que Lectura y Matemáticas compartan
// esta misma pantalla más adelante (solo cambia el título por ahora).
data class MateriaConfig(val titulo: String)

val MATERIA_LECTURA = MateriaConfig(titulo = "Lectura")
val MATERIA_MATEMATICAS = MateriaConfig(titulo = "Matemáticas")

@Composable
fun ClasificacionScreen(
    materia: MateriaConfig,
    totalEstudiantes: Int = 8,
    onBack: () -> Unit = {}
) {
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
