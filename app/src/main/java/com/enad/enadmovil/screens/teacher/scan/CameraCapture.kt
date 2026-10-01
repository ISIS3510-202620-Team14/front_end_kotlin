package com.enad.enadmovil.ui.screens.teacher.scan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

// Misma paleta del Login.
private val Rojo = Color(0xFFA6241F)
private val TextoOscuro = Color(0xFF2B2320)
private val TextoSuave = Color(0xFF7A7168)

/**
 * Sensor: cámara del teléfono (CameraX). Antes de pedir el permiso muestra una
 * tarjeta blanca que explica para qué se usa; después muestra la vista previa y,
 * al tocar el botón, guarda una foto en cacheDir y devuelve su Uri.
 *
 * Configurada para leer texto: máxima calidad, mayor resolución disponible,
 * enfoque al tocar la pantalla y linterna.
 */
@Composable
fun CameraCapture(
    onFotoCapturada: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val notificarError = onError

    fun tienePermiso() =
        ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    var permisoConcedido by remember { mutableStateOf(tienePermiso()) }
    var denegado by remember { mutableStateOf(false) }

    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { concedido ->
        permisoConcedido = concedido
        denegado = !concedido
    }

    // Si el docente activa el permiso desde Ajustes y vuelve a la app, se detecta solo.
    DisposableEffect(lifecycleOwner) {
        val observador = LifecycleEventObserver { _, evento ->
            if (evento == Lifecycle.Event.ON_RESUME) permisoConcedido = tienePermiso()
        }
        lifecycleOwner.lifecycle.addObserver(observador)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observador) }
    }

    if (!permisoConcedido) {
        TarjetaPermiso(
            denegado = denegado,
            onPermitir = { pedirPermiso.launch(Manifest.permission.CAMERA) },
            onAbrirAjustes = {
                context.startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", context.packageName, null)
                    )
                )
            },
            onCancelar = { notificarError("Permiso de cámara cancelado") },
            modifier = modifier
        )
        return
    }

    // Foto: la mayor resolución disponible en 4:3 y calidad máxima (lo que el OCR necesita).
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                    .setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY)
                    .build()
            )
            .setJpegQuality(95)
            .build()
    }
    val camara = remember { mutableStateOf<Camera?>(null) }
    var linterna by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    // Muestra el encuadre completo, igual a lo que se guarda.
                    scaleType = PreviewView.ScaleType.FIT_CENTER
                }

                // Tocar la pantalla = enfocar y medir la luz en ese punto.
                previewView.setOnTouchListener { vista, evento ->
                    if (evento.action == MotionEvent.ACTION_UP) {
                        val punto = previewView.meteringPointFactory.createPoint(evento.x, evento.y)
                        camara.value?.cameraControl?.startFocusAndMetering(
                            FocusMeteringAction.Builder(punto).build()
                        )
                        vista.performClick()
                    }
                    true
                }

                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    val provider = futuro.get()
                    val preview = Preview.Builder()
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                                .build()
                        )
                        .build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                    try {
                        provider.unbindAll()
                        camara.value = provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        notificarError("No se pudo abrir la cámara.")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Text(
            text = "Toca la pantalla para enfocar",
            color = Color.White,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 24.dp)
        )

        Button(
            onClick = {
                linterna = !linterna
                camara.value?.cameraControl?.enableTorch(linterna)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0x99000000),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 48.dp, end = 16.dp)
        ) { Text(if (linterna) "Linterna: sí" else "Linterna: no", fontSize = 12.sp) }

        Button(
            onClick = {
                val archivo = File(context.cacheDir, "lista_${System.currentTimeMillis()}.jpg")
                val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
                imageCapture.takePicture(
                    opciones,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            camara.value?.cameraControl?.enableTorch(false)
                            onFotoCapturada(Uri.fromFile(archivo))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            notificarError("No se pudo tomar la foto.")
                        }
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Color.White),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Tomar foto") }
    }
}

/** Tarjeta blanca sobre fondo oscuro: no se confunde con la UI de la app. */
@Composable
private fun TarjetaPermiso(
    denegado: Boolean,
    onPermitir: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.background(Color(0xB3000000)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = if (denegado) "Falta el permiso de la cámara" else "Permitir el uso de la cámara",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextoOscuro
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (denegado) {
                        "Sin el permiso no podemos escanear tu lista. Actívalo en Ajustes → " +
                                "Permisos → Cámara y vuelve a la app."
                    } else {
                        "ENAd Móvil usa la cámara solo para tomar una foto de tu lista de asistencia " +
                                "y leerla. La foto se procesa en tu teléfono."
                    },
                    fontSize = 14.sp,
                    color = TextoSuave
                )
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = if (denegado) onAbrirAjustes else onPermitir,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Rojo, contentColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (denegado) "Abrir ajustes" else "Permitir",
                        fontWeight = FontWeight.Bold
                    )
                }
                TextButton(onClick = onCancelar, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (denegado) "Cancelar" else "Ahora no", color = TextoSuave)
                }
            }
        }
    }
}