package com.nexo.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import com.nexo.app.viewmodel.LoginViewModel
import org.jetbrains.compose.resources.painterResource
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.logo_nexo

@Composable
fun LoginScreen(
    onIrARegistro: () -> Unit,
    viewModel: LoginViewModel = LoginViewModel()
) {

    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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
            value = viewModel.correo.value,            placeholder = "Correo electrónico",
            onValueChange = { viewModel.onCorreoChange(it) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        InputField(
            value = password,
            placeholder = "Contraseña",
            isPassword = true
        ) { password = it }

        Spacer(modifier = Modifier.height(30.dp))

        // BOTON LOGIN
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(55.dp)
                .shadow(12.dp, RoundedCornerShape(40.dp))
                .background(botonGradiente, RoundedCornerShape(40.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {},
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Iniciar sesión",
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
}