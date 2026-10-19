package com.enad.enadmovil.ui.screens.auth

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.enad.enadmovil.R
import com.enad.enadmovil.data.remote.CloudFunctionsApi
import com.enad.enadmovil.domain.model.Usuario
import com.enad.enadmovil.ui.components.EnadHeading
import com.enad.enadmovil.ui.components.EnadPrimaryButton
import com.enad.enadmovil.ui.components.EnadTextField
import com.enad.enadmovil.ui.theme.EnadBackground
import com.enad.enadmovil.ui.theme.EnadInk
import com.enad.enadmovil.ui.theme.EnadPrimary
import com.enad.enadmovil.ui.theme.EnadSoftInk
import com.enad.enadmovil.ui.theme.EnadSurface

// Acceso y registro con el diseño de Flutter (login_view y register_view).
// La lógica sigue en AuthViewModel; aquí solo cambia la presentación.
@Composable
fun LoginScreen(
    viewModel: AuthViewModel = viewModel(),
    onLoginExitoso: (Usuario) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // En registro, "atrás" vuelve al login en vez de cerrar la app.
    BackHandler(enabled = uiState.modoRegistro && !uiState.cargando) {
        viewModel.onToggleModoRegistro()
    }

    val enviar = {
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
    }

    val ojo: @Composable () -> Unit = {
        IconButton(onClick = viewModel::onMostrarContrasenaToggle) {
            Icon(
                imageVector = if (uiState.mostrarContrasena) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                contentDescription = if (uiState.mostrarContrasena) "Ocultar contraseña" else "Mostrar contraseña",
                tint = EnadSoftInk
            )
        }
    }
    val transformacion =
        if (uiState.mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation()

    Surface(color = EnadBackground, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                // Con varias instituciones y sedes el registro ya no cabe en una pantalla.
                .verticalScroll(rememberScrollState())
                .padding(horizontal = if (uiState.modoRegistro) 20.dp else 24.dp)
                .padding(bottom = 24.dp)
        ) {
            if (uiState.modoRegistro) {
                IconButton(
                    onClick = viewModel::onToggleModoRegistro,
                    enabled = !uiState.cargando,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver", tint = EnadInk)
                }
                Spacer(Modifier.height(8.dp))
                EnadHeading("Crear cuenta")
                Spacer(Modifier.height(32.dp))

                EnadTextField(
                    value = uiState.nombreCompleto,
                    onValueChange = viewModel::onNombreCompletoChange,
                    label = "Nombre completo",
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                )
                Spacer(Modifier.height(16.dp))
                EnadTextField(
                    value = uiState.usuario,
                    onValueChange = viewModel::onUsuarioChange,
                    label = "Correo",
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(Modifier.height(16.dp))
                EnadTextField(
                    value = uiState.contrasena,
                    onValueChange = viewModel::onContrasenaChange,
                    label = "Contraseña",
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = transformacion,
                    trailingIcon = ojo,
                )
                Spacer(Modifier.height(32.dp))
                SelectorInstituciones(
                    instituciones = uiState.instituciones,
                    cargando = uiState.cargandoInstituciones,
                    sedesElegidas = uiState.sedesElegidas,
                    habilitado = !uiState.cargando,
                    onInstitucionToggle = viewModel::onInstitucionToggle,
                    onSedeToggle = viewModel::onSedeToggle
                )
                Spacer(Modifier.height(16.dp))
            } else {
                Spacer(Modifier.height(60.dp))
                Image(
                    painter = painterResource(R.drawable.enad_logo),
                    contentDescription = "ENAd, Enseñar al nivel adecuado",
                    contentScale = ContentScale.Fit,
                    alignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().height(90.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text("Ingresa con tu correo y contraseña.", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(40.dp))

                EnadTextField(
                    value = uiState.usuario,
                    onValueChange = viewModel::onUsuarioChange,
                    label = "Usuario (correo)",
                    floatingLabel = false,
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                )
                Spacer(Modifier.height(16.dp))
                EnadTextField(
                    value = uiState.contrasena,
                    onValueChange = viewModel::onContrasenaChange,
                    label = "Contraseña",
                    floatingLabel = false,
                    enabled = !uiState.cargando,
                    isError = uiState.error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = transformacion,
                    trailingIcon = ojo,
                )
                Spacer(Modifier.height(32.dp))
            }

            if (uiState.error != null) {
                Text(uiState.error!!, style = MaterialTheme.typography.bodyMedium, color = EnadPrimary)
                Spacer(Modifier.height(16.dp))
            }

            if (uiState.cargando) {
                Row(
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = EnadPrimary, strokeWidth = 2.dp)
                }
            } else {
                EnadPrimaryButton(
                    text = if (uiState.modoRegistro) "Crear cuenta" else "Entrar",
                    onClick = enviar
                )
            }

            if (!uiState.modoRegistro) {
                Spacer(Modifier.height(16.dp))
                TextButton(
                    onClick = viewModel::onToggleModoRegistro,
                    enabled = !uiState.cargando,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("¿No tienes cuenta? Regístrate", style = MaterialTheme.typography.labelMedium, color = EnadPrimary)
                }
            }
        }
    }
}

/**
 * "Instituciones y sedes": una casilla por institución (puede marcar varias) y, debajo de
 * cada institución marcada, una casilla por sede.
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
    Text("Instituciones y sedes", style = MaterialTheme.typography.bodyMedium)
    if (cargando) {
        Spacer(Modifier.height(8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = EnadInk)
    }
    instituciones.forEach { inst ->
        FilaCasilla(
            texto = inst.municipality?.let { "${inst.name} · $it" } ?: inst.name,
            marcada = inst.id in sedesElegidas,
            habilitado = habilitado,
            sangria = 0,
            onClick = { onInstitucionToggle(inst.id) }
        )
        if (inst.id in sedesElegidas) {
            inst.campuses.forEach { sede ->
                FilaCasilla(
                    texto = sede.name,
                    marcada = sedesElegidas[inst.id]?.contains(sede.id) == true,
                    habilitado = habilitado,
                    sangria = 16,
                    onClick = { onSedeToggle(inst.id, sede.id) }
                )
            }
        }
    }
}

// Equivale a CheckboxListTile: texto a la izquierda y casilla a la derecha.
@Composable
private fun FilaCasilla(texto: String, marcada: Boolean, habilitado: Boolean, sangria: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(enabled = habilitado, onClick = onClick)
            .padding(start = (16 + sangria).dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(texto, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Checkbox(
            checked = marcada,
            onCheckedChange = { onClick() },
            enabled = habilitado,
            colors = CheckboxDefaults.colors(
                checkedColor = EnadInk,
                uncheckedColor = EnadSoftInk,
                checkmarkColor = EnadSurface
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    val authViewModel: AuthViewModel = viewModel()
    LoginScreen(viewModel = authViewModel)
}
