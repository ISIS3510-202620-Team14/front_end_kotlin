package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Nino
import com.enad.enadmovil.domain.model.Recomendacion
import com.enad.enadmovil.domain.model.armarGrupoAutomatico
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk

val docentesDisponibles = listOf("Yo", "Prof. Nelson", "Prof. Marina", "Sin asignar")

/**
 * "Nuevo grupo de <materia>", igual que el diálogo de Flutter: nombre y método. Con el método
 * automático la app mete a los estudiantes que siguen sin grupo, ordenados por nivel; con el
 * manual el grupo nace vacío y los integrantes se marcan después en el detalle.
 */
@Composable
fun CrearGrupoScreen(
    materia: String,
    recomendacion: Recomendacion?,
    onPedirRecomendacion: (tamano: Int) -> Unit,
    onBack: () -> Unit,
    onGuardar: (nombre: String, ninos: List<Nino>, docente: String, cantidadNinos: Int, cantidadDocentes: Int, metodo: MetodoAgrupacion) -> Unit,
    modifier: Modifier = Modifier,
    sinGrupo: List<Nino> = ninosDeEjemplo
) {
    var nombreGrupo by rememberSaveable { mutableStateOf("") }
    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    // null = el docente todavía no eligió: se muestra el recomendado si hay datos suficientes, o el automático
    var metodoElegido by rememberSaveable { mutableStateOf<String?>(null) }
    val metodo = metodoElegido?.let { MetodoAgrupacion.deClave(it) }
        ?: recomendacion?.takeIf { it.tieneDatosSuficientes }?.metodo
        ?: MetodoAgrupacion.AUTOMATICO
    val tamanoSalon = ninosDeEjemplo.size

    LaunchedEffect(tamanoSalon) { onPedirRecomendacion(tamanoSalon) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(EnadBackground)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp)
    ) {
        Text(
            text = "Nuevo grupo de $materia",
            style = typography.headlineLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Bold)
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Nombre del grupo",
            style = typography.bodyLarge.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = EnadInk
        )
        Spacer(Modifier.height(6.dp))
        EnadTextField(
            value = nombreGrupo,
            onValueChange = { nombreGrupo = it; errorNombre = null },
            label = "Ej: Grupo Mariposas",
            floatingLabel = false,
            isError = errorNombre != null,
            supportingText = errorNombre
        )
        Spacer(Modifier.height(20.dp))
        MetodoAgrupacionSelector(
            seleccionado = metodo,
            recomendacion = recomendacion,
            onCambiar = { metodoElegido = it.clave }
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            EnadSecondaryButton(text = "Cancelar", onClick = onBack, modifier = Modifier.weight(1f))
            EnadPrimaryButton(
                text = "Crear grupo",
                onClick = {
                    val nombre = nombreGrupo.trim()
                    if (nombre.isEmpty()) {
                        errorNombre = "Escribe un nombre para el grupo"
                        return@EnadPrimaryButton
                    }
                    val ninos = if (metodo == MetodoAgrupacion.AUTOMATICO) {
                        armarGrupoAutomatico(sinGrupo, sinGrupo.size)
                    } else {
                        emptyList()
                    }
                    onGuardar(nombre, ninos, docentesDisponibles.first(), tamanoSalon, 1, metodo)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}
