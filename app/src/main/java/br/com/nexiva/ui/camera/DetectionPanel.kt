package br.com.nexiva.ui.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import br.com.nexiva.detection.BodyTrack

/**
 * Mostra se rosto, tronco e mãos foram encontrados neste quadro.
 * Não desenha os pontos e não decide a detecção: só lê [BodyTrack].
 */
@Composable
fun DetectionPanel(
    track: BodyTrack?,
    status: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(BodyColors.panel, RoundedCornerShape(20.dp))
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
            StatusChip("Rosto", track?.faceDetected == true, BodyColors.face)
            StatusChip("Tronco", track?.torsoDetected == true, BodyColors.torso)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusChip("Mão esquerda", track?.leftHandDetected == true, BodyColors.leftHand)
            StatusChip("Mão direita", track?.rightHandDetected == true, BodyColors.rightHand)
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

/** Um indicador: apagado enquanto a parte não entra, preenchido quando entra. */
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
