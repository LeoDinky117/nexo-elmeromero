package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.SearchBarDefaults.InputField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow

@Composable
fun RegistroScreen(
    onVolverLogin: () -> Unit
) {

    var nombre by remember { mutableStateOf("") }
    var correo by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

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
                ) {},
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