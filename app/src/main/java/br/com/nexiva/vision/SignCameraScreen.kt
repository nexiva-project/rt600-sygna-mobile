package br.com.nexiva.vision

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.util.Rational
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import br.com.nexiva.R
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

private val FaceColor = Color(0xFF4FC3F7)
private val TorsoColor = Color(0xFFFFD54F)
private val LeftHandColor = Color(0xFF81C784)
private val RightHandColor = Color(0xFFCE93D8)
private val PanelColor = Color(0xCC101418)

private val FaceOval = intArrayOf(
    10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288,
    397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136,
    172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109,
)

private val UpperBodyConnections = listOf(
    11 to 12,
    11 to 23,
    12 to 24,
    23 to 24,
    11 to 13,
    13 to 15,
    12 to 14,
    14 to 16,
)

private val HandConnections = listOf(
    0 to 1, 1 to 2, 2 to 3, 3 to 4,
    0 to 5, 5 to 6, 6 to 7, 7 to 8,
    0 to 9, 9 to 10, 10 to 11, 11 to 12,
    0 to 13, 13 to 14, 14 to 15, 15 to 16,
    0 to 17, 17 to 18, 18 to 19, 19 to 20,
    5 to 9, 9 to 13, 13 to 17,
)

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
        PermissionRequest(
            showSettings = askedOnce,
            onAllow = { permissionLauncher.launch(Manifest.permission.CAMERA) },
            onOpenSettings = {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            },
        )
        return
    }

    CameraDetection()
}

@Composable
private fun PermissionRequest(
    showSettings: Boolean,
    onAllow: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101418))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.camera_permission_title),
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.camera_permission_body),
            modifier = Modifier.padding(top = 12.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color(0xFFD0D7DE),
            textAlign = TextAlign.Center,
        )
        Button(onClick = if (showSettings) onOpenSettings else onAllow) {
            Text(
                text = stringResource(
                    if (showSettings) {
                        R.string.camera_permission_settings
                    } else {
                        R.string.camera_permission_button
                    },
                ),
            )
        }
    }
}

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
        val disposed = AtomicBoolean(false)
        val trackerRef = AtomicReference<HolisticTracker?>(null)
        val executor = Executors.newSingleThreadExecutor()
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                if (disposed.get()) return@addListener
                val cameraProvider = cameraProviderFuture.get()
                executor.execute {
                    val tracker = try {
                        HolisticTracker(context.applicationContext)
                    } catch (error: Exception) {
                        mainExecutor.execute {
                            status = error.message ?: "Não foi possível carregar o modelo"
                        }
                        return@execute
                    }
                    trackerRef.set(tracker)
                    if (disposed.get()) {
                        tracker.close()
                        return@execute
                    }
                    mainExecutor.execute {
                        boundPreview.post {
                            if (disposed.get()) {
                                tracker.close()
                                return@post
                            }
                            status = "Posicione rosto, tronco e mãos na câmera"
                            bindCamera(
                                previewView = boundPreview,
                                lifecycleOwner = lifecycleOwner,
                                cameraProvider = cameraProvider,
                                analysisExecutor = executor,
                                tracker = tracker,
                                disposed = disposed,
                                onTrack = { detected ->
                                    mainExecutor.execute {
                                        if (!disposed.get()) track = detected
                                    }
                                },
                                onMirror = { mirrorPreview = it },
                                onStatus = { status = it },
                            )
                        }
                    }
                }
            },
            mainExecutor,
        )
        onDispose {
            disposed.set(true)
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
            executor.execute { trackerRef.get()?.close() }
            executor.shutdown()
        }
    }
}

private fun bindCamera(
    previewView: PreviewView,
    lifecycleOwner: LifecycleOwner,
    cameraProvider: ProcessCameraProvider,
    analysisExecutor: java.util.concurrent.ExecutorService,
    tracker: HolisticTracker,
    disposed: AtomicBoolean,
    onTrack: (BodyTrack) -> Unit,
    onMirror: (Boolean) -> Unit,
    onStatus: (String) -> Unit,
) {
    val selector = when {
        cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
            CameraSelector.DEFAULT_FRONT_CAMERA
        cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
            CameraSelector.DEFAULT_BACK_CAMERA
        else -> {
            onStatus("Nenhuma câmera encontrada")
            tracker.close()
            return
        }
    }
    val usingFrontCamera = selector == CameraSelector.DEFAULT_FRONT_CAMERA
    onMirror(usingFrontCamera)

    val preview = Preview.Builder().build().also {
        it.surfaceProvider = previewView.surfaceProvider
    }
    val analysis = ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
        .build()
    analysis.setAnalyzer(analysisExecutor) { imageProxy ->
        if (disposed.get()) {
            imageProxy.close()
            return@setAnalyzer
        }
        try {
            val bitmap = imageProxy.toUprightBitmap()
            try {
                onTrack(tracker.detect(bitmap))
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            // Um quadro ruim não deve derrubar a câmera.
        } finally {
            imageProxy.close()
        }
    }

    val rotation = previewView.display?.rotation ?: preview.targetRotation
    val viewPort = ViewPort.Builder(
        Rational(previewView.width.coerceAtLeast(1), previewView.height.coerceAtLeast(1)),
        rotation,
    ).setScaleType(ViewPort.FILL_CENTER).build()
    val group = UseCaseGroup.Builder()
        .addUseCase(preview)
        .addUseCase(analysis)
        .setViewPort(viewPort)
        .build()

    try {
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, selector, group)
    } catch (error: Exception) {
        onStatus(error.message ?: "Não foi possível abrir a câmera")
        tracker.close()
    }
}

private fun ImageProxy.toUprightBitmap(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowPadding = plane.rowStride - plane.pixelStride * width
    val paddedWidth = width + rowPadding / plane.pixelStride
    val raw = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    buffer.rewind()
    raw.copyPixelsFromBuffer(buffer)

    val crop = cropRect
    val left = crop.left.coerceIn(0, width - 1)
    val top = crop.top.coerceIn(0, height - 1)
    val cropWidth = crop.width().coerceIn(1, width - left)
    val cropHeight = crop.height().coerceIn(1, height - top)
    val cropped = Bitmap.createBitmap(raw, left, top, cropWidth, cropHeight)
    if (cropped != raw) raw.recycle()

    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return cropped
    val upright = Bitmap.createBitmap(
        cropped,
        0,
        0,
        cropped.width,
        cropped.height,
        Matrix().apply { postRotate(rotation.toFloat()) },
        true,
    )
    if (upright != cropped) cropped.recycle()
    return upright
}

@Composable
private fun LandmarkOverlay(
    track: BodyTrack?,
    mirrorX: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val current = track ?: return@Canvas
        fun map(point: NormPoint): Offset {
            val x = if (mirrorX) 1f - point.x else point.x
            return Offset(x * size.width, point.y * size.height)
        }
        drawLoop(current.facePoints, FaceOval, FaceColor, ::map)
        drawConnections(current.posePoints, UpperBodyConnections, TorsoColor, ::map, requireVisible = true)
        drawConnections(current.leftHand, HandConnections, LeftHandColor, ::map)
        drawConnections(current.rightHand, HandConnections, RightHandColor, ::map)
        current.posePoints.forEach { point ->
            if (point.visible) {
                drawCircle(color = TorsoColor, radius = 7f, center = map(point))
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLoop(
    points: List<NormPoint>,
    indices: IntArray,
    color: Color,
    map: (NormPoint) -> Offset,
) {
    if (indices.any { it !in points.indices }) return
    for (index in indices.indices) {
        val start = map(points[indices[index]])
        val end = map(points[indices[(index + 1) % indices.size]])
        drawLine(color = color, start = start, end = end, strokeWidth = 3f)
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConnections(
    points: List<NormPoint>,
    connections: List<Pair<Int, Int>>,
    color: Color,
    map: (NormPoint) -> Offset,
    requireVisible: Boolean = false,
) {
    connections.forEach { (startIndex, endIndex) ->
        val start = points.getOrNull(startIndex) ?: return@forEach
        val end = points.getOrNull(endIndex) ?: return@forEach
        if (requireVisible && (!start.visible || !end.visible)) return@forEach
        drawLine(color = color, start = map(start), end = map(end), strokeWidth = 5f)
        drawCircle(color = color, radius = 6f, center = map(start))
        drawCircle(color = color, radius = 6f, center = map(end))
    }
}

@Composable
private fun DetectionPanel(
    track: BodyTrack?,
    status: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(PanelColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Passo 1 — ver o corpo",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
        )
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFFD0D7DE),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Rosto", track?.faceDetected == true, FaceColor)
            StatusChip("Tronco", track?.torsoDetected == true, TorsoColor)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Mão esquerda", track?.leftHandDetected == true, LeftHandColor)
            StatusChip("Mão direita", track?.rightHandDetected == true, RightHandColor)
        }
        if (track != null) {
            Text(
                text = "Leitura em ${track.inferenceMs} ms. Sem tradução ainda.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9AA4AF),
            )
        }
    }
}

@Composable
private fun StatusChip(label: String, detected: Boolean, color: Color) {
    Text(
        text = if (detected) "$label ok" else label,
        modifier = Modifier
            .border(1.dp, color, RoundedCornerShape(50))
            .background(if (detected) color else Color.Transparent, RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        color = if (detected) Color(0xFF101418) else color,
        style = MaterialTheme.typography.labelLarge,
    )
}
