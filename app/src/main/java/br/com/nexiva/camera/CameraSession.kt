package br.com.nexiva.camera

import android.content.Context
import android.graphics.Bitmap
import android.util.Rational
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Abre a câmera e entrega cada quadro.
 *
 * Prefere a câmera frontal. [onFrame] roda na thread da câmera e o bitmap
 * só vale durante essa chamada. Não sabe o que é rosto, tronco ou mão.
 */
class CameraSession(
    context: Context,
    private val lifecycleOwner: LifecycleOwner,
) {
    private val appContext = context.applicationContext
    private val disposed = AtomicBoolean(false)
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    private val mainExecutor = ContextCompat.getMainExecutor(appContext)
    private var cameraProvider: ProcessCameraProvider? = null

    val isClosed: Boolean
        get() = disposed.get()

    /** Roda [block] na mesma thread que lê os quadros, sem cruzar com a detecção. */
    fun runOnCameraThread(block: () -> Unit) {
        executor.execute(block)
    }

    fun open(
        previewView: PreviewView,
        onFrame: (Bitmap) -> Unit,
        onFrontCamera: (Boolean) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener(
            {
                if (disposed.get()) return@addListener
                val provider = future.get()
                cameraProvider = provider
                previewView.post {
                    if (disposed.get()) return@post
                    bind(previewView, provider, onFrame, onFrontCamera, onFailed)
                }
            },
            mainExecutor,
        )
    }

    /**
     * Solta a câmera e, ainda na thread dela, executa [onCameraThread] antes de encerrar.
     * Use isso para fechar o modelo sem corrida com [onFrame].
     */
    fun close(onCameraThread: () -> Unit = {}) {
        disposed.set(true)
        runCatching { cameraProvider?.unbindAll() }
        executor.execute {
            onCameraThread()
            executor.shutdown()
        }
    }

    private fun bind(
        previewView: PreviewView,
        provider: ProcessCameraProvider,
        onFrame: (Bitmap) -> Unit,
        onFrontCamera: (Boolean) -> Unit,
        onFailed: (String) -> Unit,
    ) {
        val selector = when {
            provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ->
                CameraSelector.DEFAULT_FRONT_CAMERA
            provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ->
                CameraSelector.DEFAULT_BACK_CAMERA
            else -> {
                onFailed("Nenhuma câmera encontrada")
                return
            }
        }
        onFrontCamera(selector == CameraSelector.DEFAULT_FRONT_CAMERA)

        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .setResolutionSelector(
                ResolutionSelector.Builder()
                    .setResolutionStrategy(
                        ResolutionStrategy(
                            Size(1280, 720),
                            ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                        ),
                    )
                    .build(),
            )
            .build()
        analysis.setAnalyzer(executor) { imageProxy ->
            if (disposed.get()) {
                imageProxy.close()
                return@setAnalyzer
            }
            try {
                val bitmap = imageProxy.toUprightBitmap()
                try {
                    onFrame(bitmap)
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
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, selector, group)
        } catch (error: Exception) {
            onFailed(error.message ?: "Não foi possível abrir a câmera")
        }
    }
}
