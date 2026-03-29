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
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

import androidx.navigation.NavController
import androidx.compose.runtime.LaunchedEffect
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.nexo.app.ui.components.InputField
import com.nexo.app.viewmodel.RegistroViewModel

@Composable
fun RegistroScreen(
    //Parametros
    navController: NavController,
    viewModel: RegistroViewModel = viewModel(),
    onVolverLogin: () -> Unit
) {

    //-------------------

    //Se usa by para que registroExitoso sea un Boolean directo
    val registroExitoso by viewModel.registroExitoso.collectAsState()
    val context = LocalContext.current
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

        InputField(
            viewModel.nombre.value,
            "Nombre completo")
        { viewModel.onNombreChange(it)}
        Spacer(modifier = Modifier.height(16.dp))

        InputField(viewModel.edad.value,
            "Edad")
        //onValueChange = {if (it.all{char -> char.isDigit()})}) //Esto es para aceptar solo datos numericos
        { viewModel.onEdadChange(it) }
        Spacer(modifier = Modifier.height(16.dp))

        InputField(viewModel.correo.value,
            "Correo electrónico") { viewModel.onCorreoChange(it)}
        Spacer(modifier = Modifier.height(16.dp))

        InputField(viewModel.password.value,
            "Contraseña",
            true) { viewModel.onPasswordChange(it)}
        Spacer(modifier = Modifier.height(16.dp))

        InputField(viewModel.confirmar.value,
            "Confirmar contraseña",
            true) { viewModel.onConfirmarChange(it) }
        Spacer(modifier = Modifier.height(30.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .shadow(12.dp, RoundedCornerShape(40.dp))
                .background(botonGradiente, RoundedCornerShape(40.dp))
                .clickable (
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                    ) {
                   viewModel.registrar()
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