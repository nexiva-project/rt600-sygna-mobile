package br.com.nexiva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.nexiva.ui.camera.SignCameraScreen
import br.com.nexiva.ui.theme.Rt600sygnamobileTheme

/**
 * Abre o app e mostra a tela da câmera.
 * Não lê a câmera, não detecta o corpo e não traduz LIBRAS.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Rt600sygnamobileTheme {
                SignCameraScreen()
            }
        }
    }
}
