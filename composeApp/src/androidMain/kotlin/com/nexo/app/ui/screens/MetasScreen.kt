package com.nexo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.ui.components.InputField
import com.nexo.app.viewmodel.MetasViewModel

@Composable
fun MetasScreen(
    navController: NavController,
    viewModel: MetasViewModel = viewModel()
) {
    val metas by viewModel.metas.collectAsState()
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
    val botonGradiente = Brush.horizontalGradient(
        listOf(Color(0xFFFFA726), Color(0xFFFF5E7E))
    )

    Scaffold(
        bottomBar = { MyBottomBar(navController) }, // barra navegable
    ) { paddingValues ->


        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mis metas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Crea y visualiza tus objetivos de ahorro", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(28.dp))

            InputField(viewModel.nombreMeta, "Nombre de la meta") { viewModel.onNombreMetaChange(it) }
            Spacer(modifier = Modifier.height(14.dp))
            InputField(viewModel.montoObjetivo, "Monto objetivo") { viewModel.onMontoObjetivoChange(it) }
            Spacer(modifier = Modifier.height(14.dp))
            InputField(viewModel.fechaLimite, "Fecha límite (YYYY-MM-DD)") { viewModel.onFechaLimiteChange(it) }
            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .shadow(12.dp, RoundedCornerShape(40.dp))
                    .background(botonGradiente, RoundedCornerShape(40.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { viewModel.registrarMeta() },
                contentAlignment = Alignment.Center
            ) {
                Text("Guardar meta", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text("Progreso de metas", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            if (cargando) {
                CircularProgressIndicator(color = Color(0xFFFFA726))
            } else if (metas.isEmpty()) {
                Text("Aún no hay metas registradas", color = Color.White.copy(alpha = 0.7f))
            } else {
                metas.forEach { meta ->
                    val progreso = if (meta.montoObjetivo > 0) {
                        (meta.totalAhorrado / meta.montoObjetivo).toFloat().coerceIn(0f, 1f)
                    } else 0f

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp)
                            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Text(meta.nombreMeta, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Objetivo: $${meta.montoObjetivo}", color = Color.White.copy(alpha = 0.85f))
                        Text("Ahorrado: $${meta.totalAhorrado}", color = Color.White.copy(alpha = 0.85f))
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(progress = { progreso }, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${(progreso * 100).toInt()}% completado",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
