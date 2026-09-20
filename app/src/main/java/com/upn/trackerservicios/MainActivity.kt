package com.upn.trackerservicios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.upn.trackerservicios.navigation.AppNavigation
import com.upn.trackerservicios.ui.theme.TrackerserviciosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TrackerserviciosTheme {
                // Toda la magia inicia aquí de forma limpia
                AppNavigation()
            }
        }
    }
}