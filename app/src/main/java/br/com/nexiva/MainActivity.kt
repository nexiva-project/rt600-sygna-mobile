package br.com.nexiva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import br.com.nexiva.ui.theme.Rt600sygnamobileTheme
import br.com.nexiva.vision.SignCameraScreen

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
