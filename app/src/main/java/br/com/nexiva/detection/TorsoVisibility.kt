package br.com.nexiva.detection

import kotlin.math.hypot

/**
 * Decide se o tronco entrou no quadro.
 *
 * Olha só os dois ombros. Perto da câmera pede mais confiança. Longe, os ombros
 * ocupam pouco da imagem e a confiança do modelo cai, então a exigência cai junto.
 * O quadril não participa: de longe ele some antes do peito.
 */
fun List<NormPoint>.isTorsoVisible(): Boolean {
    if (size <= PoseLandmark.RIGHT_SHOULDER) return false
    val left = this[PoseLandmark.LEFT_SHOULDER]
    val right = this[PoseLandmark.RIGHT_SHOULDER]
    if (!left.inFrame || !right.inFrame) return false
    val span = hypot(left.x - right.x, left.y - right.y)
    if (span < MIN_SHOULDER_SPAN) return false
    val minScore = if (span < FAR_SHOULDER_SPAN) FAR_SHOULDER_SCORE else NEAR_SHOULDER_SCORE
    return left.score >= minScore && right.score >= minScore
}

private const val MIN_SHOULDER_SPAN = 0.012f
private const val FAR_SHOULDER_SPAN = 0.16f
private const val FAR_SHOULDER_SCORE = 0.15f
private const val NEAR_SHOULDER_SCORE = 0.3f
