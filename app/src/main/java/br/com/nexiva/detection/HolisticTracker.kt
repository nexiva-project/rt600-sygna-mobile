package br.com.nexiva.detection

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarker
import com.google.mediapipe.tasks.vision.holisticlandmarker.HolisticLandmarkerResult
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Roda o modelo que marca rosto, pose e as duas mãos.
 *
 * Recebe um bitmap e devolve um [BodyTrack]. Não abre a câmera e não desenha.
 * Ainda não traduz LIBRAS para texto.
 */
class HolisticTracker(context: Context) : AutoCloseable {
    private val landmarker: HolisticLandmarker
    private val timestamps = AtomicLong(0L)
    private val closed = AtomicBoolean(false)

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath(MODEL_ASSET)
            .setDelegate(Delegate.CPU)
            .build()
        val options = HolisticLandmarker.HolisticLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.VIDEO)
            .setMinFaceDetectionConfidence(0.5f)
            .setMinPoseDetectionConfidence(0.3f)
            .setMinHandLandmarksConfidence(0.5f)
            .build()
        landmarker = HolisticLandmarker.createFromOptions(context, options)
    }

    /** Marca o corpo neste quadro. Chamar sempre na mesma thread. */
    fun detect(bitmap: Bitmap): BodyTrack {
        val started = SystemClock.elapsedRealtime()
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = landmarker.detectForVideo(mpImage, nextTimestamp())
        return result.toBodyTrack(SystemClock.elapsedRealtime() - started)
    }

    override fun close() {
        if (closed.compareAndSet(false, true)) {
            landmarker.close()
        }
    }

    private fun nextTimestamp(): Long {
        val now = SystemClock.uptimeMillis()
        while (true) {
            val previous = timestamps.get()
            val next = if (now > previous) now else previous + 1L
            if (timestamps.compareAndSet(previous, next)) return next
        }
    }

    private fun HolisticLandmarkerResult.toBodyTrack(inferenceMs: Long): BodyTrack {
        val face = faceLandmarks().map { NormPoint(it.x(), it.y()) }
        val pose = poseLandmarks().map { landmark ->
            NormPoint(
                x = landmark.x(),
                y = landmark.y(),
                score = minOf(
                    landmark.visibility().orElse(1f),
                    landmark.presence().orElse(1f),
                ),
            )
        }
        val left = leftHandLandmarks().map { NormPoint(it.x(), it.y()) }
        val right = rightHandLandmarks().map { NormPoint(it.x(), it.y()) }
        return BodyTrack(
            facePoints = face,
            posePoints = pose,
            leftHand = left,
            rightHand = right,
            faceDetected = face.isNotEmpty(),
            torsoDetected = pose.isTorsoVisible(),
            leftHandDetected = left.size >= HAND_LANDMARK_COUNT,
            rightHandDetected = right.size >= HAND_LANDMARK_COUNT,
            inferenceMs = inferenceMs,
        )
    }

    private companion object {
        const val MODEL_ASSET = "holistic_landmarker.task"
        const val HAND_LANDMARK_COUNT = 21
    }
}
