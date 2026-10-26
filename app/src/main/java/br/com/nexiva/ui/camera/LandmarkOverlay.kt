package br.com.nexiva.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import br.com.nexiva.detection.BodyTrack
import br.com.nexiva.detection.NormPoint
import br.com.nexiva.detection.PoseLandmark
import kotlin.math.hypot

/**
 * Desenha rosto, tronco e mãos por cima do vídeo.
 * Só pinta o [BodyTrack]. Não abre a câmera e não roda o modelo.
 */
@Composable
fun LandmarkOverlay(
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
        drawConnections(
            current.facePoints,
            BodySkeleton.face,
            BodyColors.face,
            ::map,
            strokeWidth = 2.5f,
            pointRadius = 3.5f,
            cap = StrokeCap.Round,
        )
        if (current.torsoDetected) {
            drawTorso(current.posePoints, ::map)
        }
        drawConnections(
            current.posePoints,
            BodySkeleton.upperBody,
            BodyColors.torso,
            ::map,
            requireVisible = true,
        )
        drawConnections(current.leftHand, BodySkeleton.hand, BodyColors.leftHand, ::map)
        drawConnections(current.rightHand, BodySkeleton.hand, BodyColors.rightHand, ::map)
        current.posePoints.forEach { point ->
            if (point.visible) {
                drawCircle(color = BodyColors.torso, radius = 7f, center = map(point))
            }
        }
    }
}

/**
 * Pinta a área do tronco. Se o quadril não apareceu, estima a altura pelos ombros
 * para a área continuar visível quando a pessoa está longe.
 */
private fun DrawScope.drawTorso(
    points: List<NormPoint>,
    map: (NormPoint) -> Offset,
) {
    val leftShoulder = points.getOrNull(PoseLandmark.LEFT_SHOULDER) ?: return
    val rightShoulder = points.getOrNull(PoseLandmark.RIGHT_SHOULDER) ?: return
    val leftHip = points.getOrNull(PoseLandmark.LEFT_HIP)
    val rightHip = points.getOrNull(PoseLandmark.RIGHT_HIP)
    val bottomLeft: NormPoint
    val bottomRight: NormPoint
    if (leftHip != null && rightHip != null && leftHip.visible && rightHip.visible) {
        bottomLeft = leftHip
        bottomRight = rightHip
    } else {
        bottomLeft = estimatedHip(leftShoulder, rightShoulder, fromLeft = true)
        bottomRight = estimatedHip(leftShoulder, rightShoulder, fromLeft = false)
    }
    val path = Path().apply {
        val topLeft = map(leftShoulder)
        val topRight = map(rightShoulder)
        val lowerRight = map(bottomRight)
        val lowerLeft = map(bottomLeft)
        moveTo(topLeft.x, topLeft.y)
        lineTo(topRight.x, topRight.y)
        lineTo(lowerRight.x, lowerRight.y)
        lineTo(lowerLeft.x, lowerLeft.y)
        close()
    }
    drawPath(path, BodyColors.torso.copy(alpha = 0.28f))
}

private fun estimatedHip(leftShoulder: NormPoint, rightShoulder: NormPoint, fromLeft: Boolean): NormPoint {
    val dx = rightShoulder.x - leftShoulder.x
    val dy = rightShoulder.y - leftShoulder.y
    val span = hypot(dx, dy).coerceAtLeast(0.02f)
    var downX = -dy / span
    var downY = dx / span
    if (downY < 0f) {
        downX = -downX
        downY = -downY
    }
    val length = span * 1.35f
    val origin = if (fromLeft) leftShoulder else rightShoulder
    return NormPoint(
        x = origin.x + downX * length,
        y = origin.y + downY * length,
    )
}

private fun DrawScope.drawConnections(
    points: List<NormPoint>,
    connections: List<Pair<Int, Int>>,
    color: Color,
    map: (NormPoint) -> Offset,
    requireVisible: Boolean = false,
    strokeWidth: Float = 5f,
    pointRadius: Float = 6f,
    cap: StrokeCap = StrokeCap.Butt,
) {
    connections.forEach { (startIndex, endIndex) ->
        val start = points.getOrNull(startIndex) ?: return@forEach
        val end = points.getOrNull(endIndex) ?: return@forEach
        if (requireVisible && (!start.visible || !end.visible)) return@forEach
        drawLine(
            color = color,
            start = map(start),
            end = map(end),
            strokeWidth = strokeWidth,
            cap = cap,
        )
        drawCircle(color = color, radius = pointRadius, center = map(start))
        drawCircle(color = color, radius = pointRadius, center = map(end))
    }
}
