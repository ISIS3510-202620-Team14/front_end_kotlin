package com.enad.enadmovil.ui.screens.auth

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.domain.model.Usuario

// Same palette as the high-fidelity mockup. Kept local to this file on purpose.
private val BgCream = Color(0xFFF7F1E8)
private val TextDark = Color(0xFF2B2320)
private val TextMuted = Color(0xFF7A7168)
private val PrimaryRed = Color(0xFFA6241F)
private val FieldBorder = Color(0xFFDCD2C0)
private val ChipSelected = Color(0xFFF6DAD6)

// Generic serif family (maps to the system serif font, no extra font files
// needed) — gives the title the same display-serif look as the mockup without
// touching the rest of the app's typography.
private val TitleFont = FontFamily.Serif

@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginExitoso: (Usuario) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Surface(color = BgCream, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // Con varias instituciones y sedes el registro ya no cabe en una pantalla.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp, bottom = 24.dp)
        ) {
            Text(
                text = "ENAd Móvil",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = TitleFont,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (uiState.modoRegistro) {
                    "Crea tu cuenta para comenzar."
                } else {
                    "Ingresa con tu usuario y contraseña."
                },
                fontSize = 15.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            if (uiState.modoRegistro) {
                OutlinedTextField(
                    value = uiState.nombreCompleto,
                    onValueChange = viewModel::onNombreCompletoChange,
                    placeholder = { Text("Nombre completo", color = TextMuted) },
                    singleLine = true,
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                    shape = RoundedCornerShape(10.dp),
                    colors = campoColors(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            OutlinedTextField(
                value = uiState.usuario,
                onValueChange = viewModel::onUsuarioChange,
                placeholder = { Text("Usuario", color = TextMuted) },
                singleLine = true,
                enabled = !uiState.cargando,
                isError = uiState.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape = RoundedCornerShape(10.dp),
                colors = campoColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = uiState.contrasena,
                onValueChange = viewModel::onContrasenaChange,
                placeholder = { Text("Contraseña", color = TextMuted) },
                singleLine = true,
                enabled = !uiState.cargando,
                isError = uiState.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (uiState.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = viewModel::onMostrarContrasenaToggle) {
                        Icon(
                            imageVector = if (uiState.mostrarContrasena) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (uiState.mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                            tint = TextMuted
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = campoColors(),
                modifier = Modifier.fillMaxWidth()
            )

            if (uiState.modoRegistro) {
                Spacer(modifier = Modifier.height(20.dp))
                SelectorInstituciones(
                    instituciones = uiState.instituciones,
                    cargando = uiState.cargandoInstituciones,
                    sedesElegidas = uiState.sedesElegidas,
                    habilitado = !uiState.cargando,
                    onInstitucionToggle = viewModel::onInstitucionToggle,
                    onSedeToggle = viewModel::onSedeToggle
                )
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.error!!,
                    fontSize = 13.sp,
                    color = PrimaryRed,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    viewModel.onSubmitClick { usuario ->
                        // El Toast sobrevive a la navegación, así que se ve ya dentro de la app.
                        if (viewModel.uiState.value.correoBienvenidaEnviado) {
                            Toast.makeText(
                                context,
                                "Te enviamos un correo de bienvenida a ${usuario.email}.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        onLoginExitoso(usuario)
                    }
                },
                enabled = !uiState.cargando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed,
                    contentColor = Color.White
                )
            ) {
                if (uiState.cargando) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = if (uiState.modoRegistro) "Registrarme" else "Entrar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (uiState.modoRegistro) {
                    "¿Ya tienes cuenta? Inicia sesión"
                } else {
                    "¿No tienes cuenta? Regístrate"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = PrimaryRed,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(enabled = !uiState.cargando) { viewModel.onToggleModoRegistro() }
            )
        }
    }
}

/**
 * Instituciones donde trabaja el docente (puede marcar varias) y, debajo de cada
 * institución marcada que tenga sedes, sus sedes para marcar.
 */
@Composable
private fun SelectorInstituciones(
    instituciones: List<CloudFunctionsApi.Institucion>,
    cargando: Boolean,
    sedesElegidas: Map<String, Set<String>>,
    habilitado: Boolean,
    onInstitucionToggle: (String) -> Unit,
    onSedeToggle: (String, String) -> Unit
) {
    Text(
        text = if (cargando) "Cargando instituciones..." else "Instituciones donde trabajas (puedes elegir varias)",
        fontSize = 14.sp,
        color = TextMuted
    )
    Spacer(modifier = Modifier.height(8.dp))
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        instituciones.forEach { inst ->
            ChipSeleccion(
                texto = inst.municipality?.let { "${inst.name} · $it" } ?: inst.name,
                seleccionado = inst.id in sedesElegidas,
                habilitado = habilitado,
                onClick = { onInstitucionToggle(inst.id) }
            )
        }
    }

    instituciones
        .filter { it.id in sedesElegidas && it.campuses.isNotEmpty() }
        .forEach { inst ->
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = "Sedes de ${inst.name}", fontSize = 14.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                inst.campuses.forEach { sede ->
                    ChipSeleccion(
                        texto = sede.name,
                        seleccionado = sedesElegidas[inst.id]?.contains(sede.id) == true,
                        habilitado = habilitado,
                        onClick = { onSedeToggle(inst.id, sede.id) }
                    )
                }
            }
        }
}

@Composable
private fun ChipSeleccion(texto: String, seleccionado: Boolean, habilitado: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        enabled = habilitado,
        label = { Text(texto) },
        leadingIcon = if (seleccionado) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = ChipSelected,
            selectedLabelColor = PrimaryRed,
            selectedLeadingIconColor = PrimaryRed
        )
    )
}

@Composable
private fun campoColors() = OutlinedTextFieldDefaults.colors(
    unfocusedBorderColor = FieldBorder,
    focusedBorderColor = PrimaryRed,
    unfocusedTextColor = TextDark,
    focusedTextColor = TextDark,
    cursorColor = PrimaryRed
)

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    val authViewModel: AuthViewModel = viewModel()
    LoginScreen(viewModel = authViewModel)
}