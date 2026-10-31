package com.enad.enadmovil.feature.grupos

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.enad.enadmovil.domain.model.MetodoAgrupacion
import com.enad.enadmovil.domain.model.Nino
import com.enad.enadmovil.domain.model.Recomendacion
import com.enad.enadmovil.domain.model.armarGrupoAutomatico
import com.enad.enadmovil.ui.components.EnadSecondaryButton
import com.enad.enadmovil.ui.components.enadFieldColors
import com.enad.enadmovil.ui.theme.ControlShape
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadLine
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.PanelShape

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

    Dialog(onDismissRequest = onBack, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = PanelShape,
            color = EnadBackground,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
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
                CampoNombreGrupo(
                    valor = nombreGrupo,
                    onCambiar = { nombreGrupo = it; errorNombre = null },
                    error = errorNombre
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
                    Button(
                        onClick = {
                            val nombre = nombreGrupo.trim()
                            if (nombre.isEmpty()) {
                                errorNombre = "Escribe un nombre para el grupo"
                                return@Button
                            }
                            val ninos = if (metodo == MetodoAgrupacion.AUTOMATICO) {
                                armarGrupoAutomatico(sinGrupo, sinGrupo.size)
                            } else {
                                emptyList()
                            }
                            onGuardar(nombre, ninos, docentesDisponibles.first(), tamanoSalon, 1, metodo)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        shape = ControlShape,
                        colors = ButtonDefaults.buttonColors(containerColor = EnadPrimary, contentColor = EnadInk)
                    ) {
                        Text(
                            text = "Crear grupo",
                            style = typography.labelLarge.copy(fontSize = 14.sp),
                            color = EnadInk
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CampoNombreGrupo(valor: String, onCambiar: (String) -> Unit, error: String?) {
    val interaccion = remember { MutableInteractionSource() }
    val colores = enadFieldColors(bordeSinFoco = EnadLine)
    val estilo = typography.bodyLarge.copy(fontSize = 14.sp, color = EnadInk)
    BasicTextField(
        value = valor,
        onValueChange = onCambiar,
        modifier = Modifier.fillMaxWidth(),
        textStyle = estilo,
        singleLine = true,
        cursorBrush = SolidColor(EnadInk),
        interactionSource = interaccion,
        decorationBox = { campo ->
            OutlinedTextFieldDefaults.DecorationBox(
                value = valor,
                innerTextField = campo,
                enabled = true,
                singleLine = true,
                visualTransformation = VisualTransformation.None,
                interactionSource = interaccion,
                isError = error != null,
                placeholder = { Text("Ej: Grupo Mariposas", style = estilo) },
                supportingText = error?.let { { Text(it) } },
                colors = colores,
                contentPadding = PaddingValues(12.dp),
                container = {
                    OutlinedTextFieldDefaults.Container(
                        enabled = true,
                        isError = error != null,
                        interactionSource = interaccion,
                        colors = colores,
                        shape = ControlShape
                    )
                }
            )
        }
    )
}