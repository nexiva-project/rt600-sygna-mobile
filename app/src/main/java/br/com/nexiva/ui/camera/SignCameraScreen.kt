package br.com.nexiva.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import br.com.nexiva.camera.CameraSession
import br.com.nexiva.detection.BodyTrack
import br.com.nexiva.detection.HolisticTracker
import java.util.concurrent.atomic.AtomicReference

/**
 * Junta as partes da tela: permissão, câmera, desenho e indicadores.
 * Não contém a regra do tronco nem a leitura do hardware. Só liga uma coisa na outra.
 */
@Composable
fun SignCameraScreen() {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }
    var askedOnce by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        askedOnce = true
        permissionGranted = granted
    }

    if (!permissionGranted) {
        CameraPermissionScreen(
            showSettings = askedOnce,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
        )
        return
    }

    CameraDetection()
}

/** Liga [CameraSession] em [HolisticTracker] e mostra o resultado. */
@Composable
private fun CameraDetection() {
    val context = LocalContext.current
    val lifecycleOwner = context as LifecycleOwner
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var track by remember { mutableStateOf<BodyTrack?>(null) }
    var status by remember { mutableStateOf("Carregando modelo…") }
    var mirrorPreview by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { viewContext ->
                PreviewView(viewContext).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                }
            },
        )
        LandmarkOverlay(
            track = track,
            mirrorX = mirrorPreview,
            modifier = Modifier.fillMaxSize(),
        )
        DetectionPanel(
            track = track,
            status = status,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }

    val boundPreview = previewView
    DisposableEffect(boundPreview) {
        if (boundPreview == null) {
            return@DisposableEffect onDispose {}
        }
        val session = CameraSession(context, lifecycleOwner)
        val trackerRef = AtomicReference<HolisticTracker?>(null)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        session.runOnCameraThread {
            val tracker = try {
                HolisticTracker(context.applicationContext)
            } catch (error: Exception) {
                mainExecutor.execute {
                    status = error.message ?: "Não foi possível carregar o modelo"
                }
                return@runOnCameraThread
            }
            trackerRef.set(tracker)
            if (session.isClosed) {
                tracker.close()
                return@runOnCameraThread
            }
            mainExecutor.execute {
                status = "Posicione rosto, tronco e mãos na câmera"
                session.open(
                    previewView = boundPreview,
                    onFrame = { bitmap ->
                        val detected = tracker.detect(bitmap)
                        mainExecutor.execute {
                            if (!session.isClosed) track = detected
                        }
                    },
                    onFrontCamera = { mirrorPreview = it },
                    onFailed = { status = it },
                )
            }
        }
        onDispose {
            session.close { trackerRef.get()?.close() }
        }
    }
}
