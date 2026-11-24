package com.nexo.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.nexo.app.data.local.SessionManager
import com.nexo.app.navigation.App

private object SesionDelProceso {
    @Volatile var preparada: Boolean = false
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(11, 16, 25)),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.rgb(11, 16, 25))
        )

        // 1. Inicializamos el SessionManager aquí
        val sessionManager = SessionManager(this)
        val prepararSesion = savedInstanceState == null || !SesionDelProceso.preparada

        setContent {
            var lista by remember { mutableStateOf(!prepararSesion) }
            LaunchedEffect(prepararSesion) {
                if (prepararSesion) {
                    sessionManager.cerrarSesionLocal()
                    SesionDelProceso.preparada = true
                }
                lista = true
            }

            if (lista) {
                App(sessionManager)
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFFFA726))
                }
            }
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    //App()
}
