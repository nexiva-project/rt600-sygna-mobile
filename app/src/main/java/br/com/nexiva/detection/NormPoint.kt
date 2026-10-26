package br.com.nexiva.detection

/**
 * Um ponto do corpo, com x e y entre 0 e 1 em relação à imagem.
 * [score] é a confiança do modelo, de 0 a 1.
 */
data class NormPoint(
    val x: Float,
    val y: Float,
    val score: Float = 1f,
) {
    /** O ponto cai dentro da imagem, e não numa borda inventada. */
    val inFrame: Boolean
        get() = x in 0f..1f && y in 0f..1f

    /** Confiança mínima para desenhar o ponto. */
    val visible: Boolean
        get() = inFrame && score >= DRAW_SCORE

    private companion object {
        const val DRAW_SCORE = 0.15f
    }
}
