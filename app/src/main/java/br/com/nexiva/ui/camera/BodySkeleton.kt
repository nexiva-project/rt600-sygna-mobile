package br.com.nexiva.ui.camera

/**
 * Quais pontos ligar no desenho.
 * Não decide se a parte foi detectada. Isso já vem pronto em [br.com.nexiva.detection.BodyTrack].
 */
internal object BodySkeleton {
    /**
     * Linhas do rosto. Cada par, como `61 to 146`, liga o ponto 61 ao ponto 146.
     * Os números são fixos do MediaPipe Face Landmarker: 478 pontos no total,
     * de 0 a 477. Esquerda e direita são as da pessoa, não as da tela.
     *
     * Mapa e explicação:
     * https://developers.google.com/edge/mediapipe/solutions/vision/face_landmarker
     * Lista original, classe FaceLandmarksConnections:
     * https://github.com/google-ai-edge/mediapipe/blob/master/mediapipe/tasks/python/vision/face_landmarker.py
     */
    val face = listOf(
        // Lábios, contorno de fora e de dentro. FACE_LANDMARKS_LIPS.
        61 to 146, 146 to 91, 91 to 181, 181 to 84, 84 to 17, 17 to 314, 314 to 405,
        405 to 321, 321 to 375, 375 to 291, 61 to 185, 185 to 40, 40 to 39, 39 to 37,
        37 to 0, 0 to 267, 267 to 269, 269 to 270, 270 to 409, 409 to 291, 78 to 95,
        95 to 88, 88 to 178, 178 to 87, 87 to 14, 14 to 317, 317 to 402, 402 to 318,
        318 to 324, 324 to 308, 78 to 191, 191 to 80, 80 to 81, 81 to 82, 82 to 13,
        13 to 312, 312 to 311, 311 to 310, 310 to 415, 415 to 308,
        // Olho esquerdo da pessoa. FACE_LANDMARKS_LEFT_EYE.
        263 to 249, 249 to 390, 390 to 373, 373 to 374, 374 to 380, 380 to 381,
        381 to 382, 382 to 362, 263 to 466, 466 to 388, 388 to 387, 387 to 386,
        386 to 385, 385 to 384, 384 to 398, 398 to 362,
        // Sobrancelha esquerda. FACE_LANDMARKS_LEFT_EYEBROW.
        276 to 283, 283 to 282, 282 to 295, 295 to 285, 300 to 293, 293 to 334,
        334 to 296, 296 to 336,
        // Íris esquerda, pontos 474 a 477. FACE_LANDMARKS_LEFT_IRIS.
        474 to 475, 475 to 476, 476 to 477, 477 to 474,
        // Olho direito da pessoa. FACE_LANDMARKS_RIGHT_EYE.
        33 to 7, 7 to 163, 163 to 144, 144 to 145, 145 to 153, 153 to 154, 154 to 155,
        155 to 133, 33 to 246, 246 to 161, 161 to 160, 160 to 159, 159 to 158,
        158 to 157, 157 to 173, 173 to 133,
        // Sobrancelha direita. FACE_LANDMARKS_RIGHT_EYEBROW.
        46 to 53, 53 to 52, 52 to 65, 65 to 55, 70 to 63, 63 to 105, 105 to 66, 66 to 107,
        // Íris direita, pontos 469 a 472. FACE_LANDMARKS_RIGHT_IRIS.
        469 to 470, 470 to 471, 471 to 472, 472 to 469,
        // Contorno do rosto, da testa ao queixo. FACE_LANDMARKS_FACE_OVAL.
        10 to 338, 338 to 297, 297 to 332, 332 to 284, 284 to 251, 251 to 389,
        389 to 356, 356 to 454, 454 to 323, 323 to 361, 361 to 288, 288 to 397,
        397 to 365, 365 to 379, 379 to 378, 378 to 400, 400 to 377, 377 to 152,
        152 to 148, 148 to 176, 176 to 149, 149 to 150, 150 to 136, 136 to 172,
        172 to 58, 58 to 132, 132 to 93, 93 to 234, 234 to 127, 127 to 162, 162 to 21,
        21 to 54, 54 to 103, 103 to 67, 67 to 109, 109 to 10,
        // Nariz, ponte e base. FACE_LANDMARKS_NOSE.
        168 to 6, 6 to 197, 197 to 195, 195 to 5, 5 to 4, 4 to 1, 1 to 19, 19 to 94,
        94 to 2, 98 to 97, 97 to 2, 2 to 326, 326 to 327, 327 to 294, 294 to 278,
        278 to 344, 344 to 440, 440 to 275, 275 to 4, 4 to 45, 45 to 220, 220 to 115,
        115 to 48, 48 to 64, 64 to 98,
    )

    /** Linha entre o ombro esquerdo (11) e o direito (12). */
    val shoulders = listOf(11 to 12)

    val arms = listOf(
        11 to 13,
        13 to 15,
        12 to 14,
        14 to 16,
    )

    val hand = listOf(
        0 to 1, 1 to 2, 2 to 3, 3 to 4,
        0 to 5, 5 to 6, 6 to 7, 7 to 8,
        0 to 9, 9 to 10, 10 to 11, 11 to 12,
        0 to 13, 13 to 14, 14 to 15, 15 to 16,
        0 to 17, 17 to 18, 18 to 19, 19 to 20,
        5 to 9, 9 to 13, 13 to 17,
    )
}
