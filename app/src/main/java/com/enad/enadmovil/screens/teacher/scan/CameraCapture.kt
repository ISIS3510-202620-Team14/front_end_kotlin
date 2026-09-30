package com.enad.enadmovil.ui.screens.teacher.scan

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File

/**
 * Sensor: cámara del teléfono (CameraX). Pide el permiso, muestra la vista
 * previa y, al tocar el botón, guarda una foto en cacheDir y devuelve su Uri.
 */
@Composable
fun CameraCapture(
    onFotoCapturada: (Uri) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var permisoConcedido by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }
    val pedirPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permisoConcedido = it }

    LaunchedEffect(Unit) {
        if (!permisoConcedido) pedirPermiso.launch(Manifest.permission.CAMERA)
    }

    if (!permisoConcedido) {
        Column(
            modifier = modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Necesitamos la cámara para escanear la lista de asistencia.")
            Button(
                onClick = { pedirPermiso.launch(Manifest.permission.CAMERA) },
                modifier = Modifier.padding(top = 16.dp)
            ) { Text("Dar permiso") }
        }
        return
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val futuro = ProcessCameraProvider.getInstance(ctx)
                futuro.addListener({
                    val provider = futuro.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    try {
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture
                        )
                    } catch (e: Exception) {
                        onError("No se pudo abrir la cámara.")
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        Button(
            onClick = {
                val archivo = File(context.cacheDir, "lista_${System.currentTimeMillis()}.jpg")
                val opciones = ImageCapture.OutputFileOptions.Builder(archivo).build()
                imageCapture.takePicture(
                    opciones,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onFotoCapturada(Uri.fromFile(archivo))
                        }

                        override fun onError(exception: ImageCaptureException) {
                            onError("No se pudo tomar la foto.")
                        }
                    }
                )
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFA6241F),
                contentColor = Color.White
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp)
                .fillMaxWidth()
                .height(52.dp)
        ) { Text("Tomar foto") }
    }
}