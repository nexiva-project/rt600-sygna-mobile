package br.com.nexiva.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import br.com.nexiva.detection.BodyTrack
import br.com.nexiva.detection.NormPoint

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
        drawConnections(
            current.posePoints,
            BodySkeleton.shoulders,
            BodyColors.torso,
            ::map,
            requireVisible = true,
        )
        drawConnections(
            current.posePoints,
            BodySkeleton.arms,
            BodyColors.torso,
            ::map,
            requireVisible = true,
        )
        drawConnections(current.leftHand, BodySkeleton.hand, BodyColors.leftHand, ::map)
        drawConnections(current.rightHand, BodySkeleton.hand, BodyColors.rightHand, ::map)
    }
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
