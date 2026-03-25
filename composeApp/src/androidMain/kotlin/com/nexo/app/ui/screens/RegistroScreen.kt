package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexo.app.model.Usuario
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.nexo.app.viewmodel.RegistroViewModel

@Composable
fun RegistroScreen(
    navController: NavController,
    viewModel: RegistroViewModel = viewModel(),
    onVolverLogin: () -> Unit
) {

    var nombre by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    //-------------------
    val context = LocalContext.current
    //Se usa by para que registroExitoso sea un Boolean directo
    val registroExitoso by viewModel.registroExitoso.collectAsState()
    //-------------------------------------------------
    LaunchedEffect(registroExitoso){
        if (registroExitoso){
            Toast.makeText(context, "¡Registro exitoso!", Toast.LENGTH_SHORT).show()
            navController.navigate("login"){
                popUpTo("registro") { inclusive = true}
            }
            viewModel.resetRegistroEstado()
        }
    }

    val client = remember {
        HttpClient(OkHttp) {
            install (ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true //Por si el server manda datos de mas
                })
            }
        }
    }

    val fondo = Brush.verticalGradient(
        listOf(
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

        Spacer(modifier = Modifier.height(80.dp))

        Text(
            "Regístrate y únete",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(40.dp))

        InputField(nombre, "Nombre completo") { nombre = it }
        Spacer(modifier = Modifier.height(16.dp))

        InputField(edad, "Edad")
            //onValueChange = {if (it.all{char -> char.isDigit()})}) //Esto es para aceptar solo datos numericos
            { edad = it}
        Spacer(modifier = Modifier.height(16.dp))

        InputField(correo, "Correo electrónico") { correo = it }
        Spacer(modifier = Modifier.height(16.dp))

        InputField(password, "Contraseña", true) { password = it }
        Spacer(modifier = Modifier.height(16.dp))

        InputField(confirmPassword, "Confirmar contraseña", true) { confirmPassword = it }
        Spacer(modifier = Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .shadow(12.dp, RoundedCornerShape(40.dp))
                .background(botonGradiente, RoundedCornerShape(40.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    scope.launch {
                        //Validacion de edad jiji
                        val edad = edad.toInt()
                        if (edad !in 15..25){
                            println("La edad debe estar entre 15 y 25 años")
                            return@launch
                        }
                        try {
                            val response = client.post("http://192.168.1.4:8080/register") {
                                contentType(ContentType.Application.Json)
                                setBody(Usuario(
                                    nombre = nombre,
                                    edad = edad,
                                    correo = correo,
                                    password = password,

                                ))
                            }
                            println("Registro exitoso: ${response.status}")
                        }catch (e: Exception){
                            println("Error de red: ${e.message}")

                        }                    }
                    },


            contentAlignment = Alignment.Center
        ) {

            Text(
                "Crear cuenta",
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "¿Ya tienes cuenta? Inicia sesión",
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.clickable { onVolverLogin() }
        )
    }
}