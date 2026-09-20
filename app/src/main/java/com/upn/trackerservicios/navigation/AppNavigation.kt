package com.upn.trackerservicios.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController


import com.upn.trackerservicios.ui.auth.AuthViewModel
import com.upn.trackerservicios.ui.auth.LoginScreen
import com.upn.trackerservicios.ui.cisterna.PanelCisternaScreen
import com.upn.trackerservicios.ui.recolector.PanelRecolectorScreen
import com.upn.trackerservicios.ui.vecino.CajaTrackerScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()

    NavHost(navController = navController, startDestination = "login") {

        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onLoginExitoso = { rol ->
                    when (rol) {
                        "CISTERNA" -> navController.navigate("panel_cisterna") { popUpTo(0) }
                        "RECOLECTOR" -> navController.navigate("panel_recolector") { popUpTo(0) }
                        else -> navController.navigate("panel_vecino") { popUpTo(0) }
                    }
                }
            )
        }

        composable("panel_cisterna") {
            PanelCisternaScreen(
                mensaje = "Cisterna 💧",
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }

        composable("panel_recolector") {
            PanelRecolectorScreen(
                mensaje = "Recolector 🗑️",
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }

        composable("panel_vecino") {
            CajaTrackerScreen(
                onCerrarSesion = {
                    authViewModel.cerrarSesion()
                    navController.navigate("login") { popUpTo(0) }
                }
            )
        }
    }
}