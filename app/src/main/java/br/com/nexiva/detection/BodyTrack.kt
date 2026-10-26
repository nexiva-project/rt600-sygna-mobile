package br.com.nexiva.detection

/**
 * Resultado de um quadro: onde está cada parte e se ela foi aceita.
 * Quem desenha e quem mostra os indicadores só lê este objeto.
 */
data class BodyTrack(
    val facePoints: List<NormPoint>,
    val posePoints: List<NormPoint>,
    val leftHand: List<NormPoint>,
    val rightHand: List<NormPoint>,
    val faceDetected: Boolean,
    val torsoDetected: Boolean,
    val leftHandDetected: Boolean,
    val rightHandDetected: Boolean,
    /** Tempo que o modelo levou neste quadro, em milissegundos. */
    val inferenceMs: Long,
)
