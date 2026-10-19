package br.com.nexiva.vision

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

data class NormPoint(
    val x: Float,
    val y: Float,
    val visible: Boolean = true,
)

data class BodyTrack(
    val facePoints: List<NormPoint>,
    val posePoints: List<NormPoint>,
    val leftHand: List<NormPoint>,
    val rightHand: List<NormPoint>,
    val faceDetected: Boolean,
    val torsoDetected: Boolean,
    val leftHandDetected: Boolean,
    val rightHandDetected: Boolean,
    val inferenceMs: Long,
)

/**
 * Passo 1 do sign-to-text: um único modelo marca rosto, pose (tronco) e as duas mãos.
 * Ainda não traduz o sinal.
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
            .setMinPoseDetectionConfidence(0.5f)
            .setMinHandLandmarksConfidence(0.5f)
            .build()
        landmarker = HolisticLandmarker.createFromOptions(context, options)
    }

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
            val visibility = landmark.visibility().orElse(1f)
            val presence = landmark.presence().orElse(1f)
            NormPoint(
                x = landmark.x(),
                y = landmark.y(),
                visible = visibility >= VISIBILITY_THRESHOLD && presence >= VISIBILITY_THRESHOLD,
            )
        }
        val left = leftHandLandmarks().map { NormPoint(it.x(), it.y()) }
        val right = rightHandLandmarks().map { NormPoint(it.x(), it.y()) }
        val torsoDetected = pose.isTorsoVisible()
        return BodyTrack(
            facePoints = face,
            posePoints = pose,
            leftHand = left,
            rightHand = right,
            faceDetected = face.isNotEmpty(),
            torsoDetected = torsoDetected,
            leftHandDetected = left.size >= HAND_LANDMARK_COUNT,
            rightHandDetected = right.size >= HAND_LANDMARK_COUNT,
            inferenceMs = inferenceMs,
        )
    }

    private fun List<NormPoint>.isTorsoVisible(): Boolean {
        if (size <= RIGHT_HIP) return false
        return this[LEFT_SHOULDER].visible &&
            this[RIGHT_SHOULDER].visible &&
            this[LEFT_HIP].visible &&
            this[RIGHT_HIP].visible
    }

    private companion object {
        const val MODEL_ASSET = "holistic_landmarker.task"
        const val VISIBILITY_THRESHOLD = 0.5f
        const val HAND_LANDMARK_COUNT = 21
        const val LEFT_SHOULDER = 11
        const val RIGHT_SHOULDER = 12
        const val LEFT_HIP = 23
        const val RIGHT_HIP = 24
    }
}
