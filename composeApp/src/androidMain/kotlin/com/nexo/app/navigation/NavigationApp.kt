package com.nexo.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.*
import com.nexo.app.data.local.SessionManager
import com.nexo.app.data.local.PresupuestoRepository
import com.nexo.app.ui.screens.AddMovimientoScreen
import com.nexo.app.ui.screens.CalendarioScreen
import com.nexo.app.ui.screens.GastosScreen
import com.nexo.app.ui.screens.HomeScreen
import com.nexo.app.ui.screens.LoginScreen
import com.nexo.app.ui.screens.MainHome
import com.nexo.app.ui.screens.MetasScreen
import com.nexo.app.ui.screens.PerfilScreen
import com.nexo.app.ui.screens.PresupuestoScreen
import com.nexo.app.ui.screens.RegistroScreen
import com.nexo.app.ui.screens.GuiaBienvenidaOverlay
import com.nexo.app.ui.screens.GuiaFocoState
import com.nexo.app.ui.screens.LocalGuiaFocoState
import com.nexo.app.ui.screens.pasosGuiaFoco
import com.nexo.app.ui.screens.SuccessScreen
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.ui.viewModel.CalendarioViewModel
import com.nexo.app.ui.viewModel.PresupuestoViewModel
import com.nexo.app.viewmodel.MetasViewModel
import com.nexo.app.ui.viewModel.PerfilViewModel
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.LocalNexoProfileIcon
import com.nexo.app.ui.parseNexoPalette
import kotlinx.coroutines.launch

@Composable
fun AppNavigation(sessionManager: SessionManager) {
    val navController = rememberNavController()
    val paletteData by sessionManager.temaPaletteFlow.collectAsState(initial = "#C837AB|#0B1019|#161C28|#FF508E")
    val profileIcon by sessionManager.profileIconFlow.collectAsState(initial = "👤")
    val estadoFoco = remember { GuiaFocoState() }
    var guiaActiva by remember { mutableStateOf(false) }
    var pasoGuia by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    // 1. ViewModel de Movimientos con SessionManager
    val  movimientosVM : MovimientosViewModel = viewModel(
        factory = object : ViewModelProvider.Factory{
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MovimientosViewModel(sessionManager) as T
            }
        }
    )

    // 2. ViewModel de Perfil con SessionManager
    val perfilVM: PerfilViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PerfilViewModel(sessionManager) as T
            }
        }
    )

    // 3. ViewModel de Metas con SessionManager
    val metasVM: MetasViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MetasViewModel(sessionManager) as T
            }
        }
    )

    val presupuestoRepository = remember(sessionManager) {
        PresupuestoRepository(sessionManager.applicationContext)
    }
    val presupuestoVM: PresupuestoViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PresupuestoViewModel(sessionManager, presupuestoRepository) as T
        }
    )
    val calendarioVM: CalendarioViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                CalendarioViewModel(sessionManager, presupuestoRepository) as T
        }
    )

    CompositionLocalProvider(
      LocalNexoPalette provides parseNexoPalette(paletteData),
      LocalNexoProfileIcon provides profileIcon,
      LocalGuiaFocoState provides estadoFoco
    ) {
      Box(Modifier.fillMaxSize()) {
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
                onIniciarGuia = {
                    estadoFoco.limpiar()
                    pasoGuia = 0
                    guiaActiva = true
                },
                sessionManager = sessionManager
            )
        }
        //Se dirige aquí en el caso de que el usuario no tenga una cuenta creada

        composable(Routes.REGISTRO) {
            RegistroScreen(
                navController = navController, //Aqui se le pasa como parametro el navController
                sessionManager = sessionManager,
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

        composable(Routes.CALENDARIO) {
            CalendarioScreen(navController, calendarioVM)
        }

        composable(Routes.PRESUPUESTO) {
            PresupuestoScreen(navController, presupuestoVM, movimientosVM)
        }

        composable(Routes.METAS){
            MetasScreen(navController, viewModel = metasVM, movimientosViewModel = movimientosVM)

        }


        composable(Routes.PERFIL){

            PerfilScreen(navController,
                perfilVM,
                onCerrarSesion = {
                    scope.launch {
                        guiaActiva = false
                        sessionManager.cerrarSesionLocal()
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.MAIN_HOME) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            )

        }

        composable(Routes.ADD_MOVIMIENTO){
            AddMovimientoScreen(navController, movimientosVM, metasVM, presupuestoVM
            )
        }

        composable(Routes.SUCCESS){
            SuccessScreen(navController, movimientosVM
            )
        }
    }
      if (guiaActiva) {
          GuiaBienvenidaOverlay(
              pasoActual = pasoGuia,
              estadoFoco = estadoFoco,
              onCambiarPaso = { nuevoPaso ->
                  val indice = nuevoPaso.coerceIn(pasosGuiaFoco.indices)
                  pasoGuia = indice
                  val ruta = pasosGuiaFoco[indice].ruta
                  if (navController.currentDestination?.route != ruta) {
                      navController.navigate(ruta) {
                          popUpTo(Routes.MAIN_HOME) { inclusive = false }
                          launchSingleTop = true
                      }
                  }
              },
              onTerminar = {
                  scope.launch {
                      sessionManager.completeOnboarding(sessionManager.getUserId())
                      guiaActiva = false
                      navController.navigate(Routes.MAIN_HOME) {
                          popUpTo(Routes.MAIN_HOME) { inclusive = false }
                          launchSingleTop = true
                      }
                  }
              },
              onConfigurarPresupuesto = {
                  scope.launch {
                      sessionManager.completeOnboarding(sessionManager.getUserId())
                      guiaActiva = false
                      navController.navigate(Routes.PRESUPUESTO) {
                          popUpTo(Routes.MAIN_HOME) { inclusive = false }
                          launchSingleTop = true
                      }
                  }
              }
          )
      }
      }
    }
}
