package com.jupgi.origami

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.jupgi.origami.core.designsystem.theme.JupgiTheme
import com.jupgi.origami.ui.JupgiApp
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JupgiTheme {
                JupgiApp()
            }
        }
    }
}
