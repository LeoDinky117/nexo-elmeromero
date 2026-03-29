package com.nexo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
//import com.nexo.app.ui.screens.InputField

import androidx.compose.material3.Text

import androidx.compose.runtime.*
import androidx.compose.runtime.getValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
//import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.navigation.Routes
//import androidx.navigation.compose.rememberNavController
import com.nexo.app.ui.components.InputField
import com.nexo.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.logo_nexo


@Composable
fun LoginScreen(
    navController: NavController,
    onIrARegistro: () -> Unit,
    viewModel: LoginViewModel = viewModel()

) {

    //validacion de usuario existente y navegacion de login exitoso a home
    val loginExitoso by viewModel.loginExitoso.collectAsState()
    val context = LocalContext.current
    val estaCargando by viewModel.cargando.collectAsState()

    LaunchedEffect(loginExitoso){
        println("DEBUG: LaunchedEffect disparado. Valor de loginExitoso: $loginExitoso")
        if (loginExitoso){ //Verificación explícita al loginExitoso
            println("DEBUG: Intentando navegar a home...")
            //Navegar a pantalla HOME
            navController.navigate(Routes.HOME){
            Toast.makeText(context, "¡Bienvenido a tu mundo financiero!", Toast.LENGTH_LONG).show()
                //Se borra el login del historial para que no se regrese al picar atrás
                //Es decir si ya está en HOME y le da al boton atras, ya no lo va a regresar al login
                popUpTo(Routes.LOGIN){inclusive = true}
            }
            //Hacemos un delay para que navegue a home y posterior realice el reset
            //kotlinx.coroutines.delay(500)
            //viewModel.resetLoginState()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        val fondo = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0D1B2A),
                Color(0xFF081427),
                Color(0xFF050C1A)
            )
        )
        val botonGradiente = Brush.horizontalGradient(
            listOf(
                Color(0xFFFFA726),
                Color(0xFFFF5E7E)
            )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(60.dp))
            Image(
                painter = painterResource(Res.drawable.logo_nexo),
                contentDescription = "logo",
                modifier = Modifier.size(140.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "¡Hola! Entra a tu mundo financiero",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(40.dp))
            InputField(
                value = viewModel.correo,
                placeholder = "Correo electrónico",
                onValueChange = { viewModel.onCorreoChange(it) }
            )
            Spacer(modifier = Modifier.height(16.dp))
            InputField(
                value = viewModel.password,
                placeholder = "Contraseña",
                isPassword = true,
                onValueChange = { viewModel.onPasswordChange(it) }
            )
            Spacer(modifier = Modifier.height(30.dp))

            //_________________________ BOTON LOGIN ________________________________________
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .shadow(12.dp, RoundedCornerShape(40.dp))
                    .background(
                        brush = if (estaCargando) {
                            // Degradado gris
                            Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFFFFA726), Color(0xFFFF5E7E)))
                        },
                        shape = RoundedCornerShape(40.dp)
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.login() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (estaCargando) "Cargando" else "Iniciar sesión",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "¿No tienes cuenta? Regístrate",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.clickable { onIrARegistro() }
            )

        }
        if (estaCargando) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)) // Oscurece la pantalla
                    .clickable(enabled = false) { }, // Evita que se toque lo de atrás
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color(0xFFFFA726) // El naranja de Nexo
                )
            }
        }



    }
}