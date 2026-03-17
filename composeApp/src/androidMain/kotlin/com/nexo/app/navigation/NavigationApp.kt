package com.nexo.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.*
import com.nexo.app.ui.screens.HomeScreen
import com.nexo.app.ui.screens.LoginScreen
import com.nexo.app.ui.screens.RegistroScreen

@Composable
fun AppNavigation() {

    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {

        composable(Routes.HOME) {
            HomeScreen(
                onIrLogin = {
                    navController.navigate(Routes.LOGIN)
                },
                onIrRegistro = {
                    navController.navigate(Routes.REGISTRO)
                }
            )
        }

        composable(Routes.LOGIN) {
            LoginScreen(
                onIrARegistro = {
                    navController.navigate(Routes.REGISTRO)
                }
            )
        }

        composable(Routes.REGISTRO) {
            RegistroScreen(
                onVolverLogin = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }
    }
}