package com.enad.enadmovil.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Colores fijos, tomados directo del prototipo. Todo el archivo es independiente
// del Theme.kt del proyecto: no hace falta tocar nada más para que se vea bien.
private val BgCream = Color(0xFFF7F1E8)
private val TextDark = Color(0xFF2B2320)
private val TextMuted = Color(0xFF7A7168)
private val PrimaryRed = Color(0xFFA6241F)
private val FieldBorder = Color(0xFFDCD2C0)
private val CardBg = Color(0xFFEFE7D8)

@Composable
fun LoginScreen(
    onEntrarClick: (usuario: String, contrasena: String) -> Unit = { _, _ -> }
) {
    var usuario by remember { mutableStateOf("") }
    var contrasena by remember { mutableStateOf("") }
    var mostrarContrasena by remember { mutableStateOf(false) }

    Surface(color = BgCream, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(top = 56.dp)
        ) {
            Text(
                text = "ENAd Móvil",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Ingresa con tu usuario y contraseña.",
                fontSize = 15.sp,
                color = TextMuted
            )

            Spacer(modifier = Modifier.height(28.dp))

            OutlinedTextField(
                value = usuario,
                onValueChange = { usuario = it },
                placeholder = { Text("Usuario", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = FieldBorder,
                    focusedBorderColor = PrimaryRed,
                    unfocusedTextColor = TextDark,
                    focusedTextColor = TextDark,
                    cursorColor = PrimaryRed
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = contrasena,
                onValueChange = { contrasena = it },
                placeholder = { Text("Contraseña", color = TextMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                visualTransformation = if (mostrarContrasena) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { mostrarContrasena = !mostrarContrasena }) {
                        Text(
                            text = if (mostrarContrasena) "Ocultar" else "Ver",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                },
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = FieldBorder,
                    focusedBorderColor = PrimaryRed,
                    unfocusedTextColor = TextDark,
                    focusedTextColor = TextDark,
                    cursorColor = PrimaryRed
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = { onEntrarClick(usuario, contrasena) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryRed,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))

            CredencialesDePruebaCard()

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "DEMOSTRACIÓN · Datos ficticios en memoria. Cada acceso reinicia el ejemplo.",
                fontSize = 12.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
private fun CredencialesDePruebaCard() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "Credenciales de prueba",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(12.dp))
        CredencialRow(rol = "Profesor", usuario = "mateo", contrasena = "1234")
        Spacer(modifier = Modifier.height(10.dp))
        CredencialRow(rol = "Administrador", usuario = "sofia", contrasena = "1234")
        Spacer(modifier = Modifier.height(10.dp))
        CredencialRow(rol = "Miembro de fundación", usuario = "aifos", contrasena = "1234")
    }
}

@Composable
private fun CredencialRow(rol: String, usuario: String, contrasena: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(text = rol, color = TextMuted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(
            text = "usuario: $usuario · contraseña: $contrasena",
            color = TextDark,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen()
}