package com.nexo.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.nexo.app.data.local.SessionManager
import com.nexo.app.ui.screens.AddMovimientoScreen
import com.nexo.app.ui.screens.GastosScreen
import com.nexo.app.ui.screens.HomeScreen
import com.nexo.app.ui.screens.LoginScreen
import com.nexo.app.ui.screens.MainHome
import com.nexo.app.ui.screens.RegistroScreen
import com.nexo.app.ui.screens.SuccessScreen
import com.nexo.app.ui.viewModel.MovimientosViewModel

@Composable
fun AppNavigation(sessionManager: SessionManager) {
    val navController = rememberNavController()

    val  movimientosVM : MovimientosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory{
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MovimientosViewModel(sessionManager) as T
            }
        }
    )

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {



        //SE INICIA AQUI AL INICIAR LA APP GRACIAS AL "startDestination"
        composable(Routes.LOGIN) {
            LoginScreen(
                navController = navController,
                onIrARegistro = {
                    navController.navigate(Routes.REGISTRO)
                },
                sessionManager = sessionManager
            )
        }
        //Se dirige aquí en el caso de que el usuario no tenga una cuenta creada

        composable(Routes.REGISTRO) {
            RegistroScreen(
                navController = navController, //Aqui se le pasa como parametro el navController
                onVolverLogin = {
                    navController.navigate(Routes.LOGIN)
                }
            )
        }

        //Se dirige aquí al crear cuenta, posterior iniciar sesión

        composable(Routes.HOME) {
            HomeScreen(
                onIrLogin = {
                    navController.navigate(Routes.HOME)
                },
                onIrRegistro = {
                    navController.navigate(Routes.REGISTRO)
                }
            )
        }

        //Se deben agregar las rutas de las demás pantallas
        composable(Routes.MAIN_HOME) {
            MainHome(navController, movimientosVM
            )
        }

        composable(Routes.MOVIMIENTOS){
            GastosScreen(navController, movimientosVM)
        }

        composable(Routes.METAS){
            Text("Pantalla de metas")//paq no tuene //terminar de copy and paste

        }


        composable(Routes.PERFIL){
            Text("Pantalla de perfil")

        }

        composable(Routes.ADD_MOVIMIENTO){
            AddMovimientoScreen(navController, movimientosVM
            )
        }

        composable(Routes.SUCCESS){
            SuccessScreen(navController, movimientosVM
            )
        }
    }
}