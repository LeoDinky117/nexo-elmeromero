package com.nexo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nexo.app.ui.viewModel.PerfilViewModel

@Composable
fun PerfilScreen(
    viewModel: PerfilViewModel = viewModel()
) {
    val perfil by viewModel.perfil.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeUI by viewModel.mensajeUI.collectAsState()
    val context = LocalContext.current


    LaunchedEffect(mensajeUI) {
        mensajeUI?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.mensajeMostrado()
        }
    }

    val fondo = Brush.verticalGradient(
        listOf(Color(0xFF0D1B2A), Color(0xFF081427), Color(0xFF050C1A))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(fondo)
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text("Mi perfil", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(24.dp))

        if (cargando) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFFFFA726))
            }
        } else if (perfil != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                ItemPerfil("Nombre", perfil!!.nombre)
                ItemPerfil("Correo", perfil!!.correo)
                ItemPerfil("Edad", perfil!!.edad.toString())
                ItemPerfil("Puntos", perfil!!.puntos.toString())
                ItemPerfil("Fecha de registro", perfil!!.fechaRegistro)
            }
        } else {
            Text("No hay datos del perfil", color = Color.White.copy(alpha = 0.75f))
        }
    }
}

@Composable
private fun ItemPerfil(titulo: String, valor: String) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = titulo, color = Color(0xFFFFA726), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = valor, color = Color.White, fontSize = 17.sp)
    }
}
