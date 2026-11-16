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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.ui.components.InputField
import com.nexo.app.viewmodel.MetasViewModel
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.model.ProgresoMetaVista
import com.nexo.app.ui.viewModel.MovimientosViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun MetasScreen(
    navController: NavController,
    viewModel: MetasViewModel,
    movimientosViewModel: MovimientosViewModel
) {
    val metas by viewModel.metas.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeUI by viewModel.mensajeUI.collectAsState()
    val context = LocalContext.current
    var metaParaAbonar by remember { mutableStateOf<ProgresoMetaVista?>(null) }
    var montoAbono by remember { mutableStateOf("") }
    val abonoGuardando by viewModel.abonoGuardando.collectAsState()


    LaunchedEffect(mensajeUI) {
        mensajeUI?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.mensajeMostrado()
        }
    }

    val paleta = LocalNexoPalette.current
    val fondo = Brush.verticalGradient(listOf(paleta.background, paleta.background.copy(alpha = 0.92f), Color(0xFF050C1A)))
    val botonGradiente = Brush.horizontalGradient(
        listOf(paleta.accent, paleta.highlight)
    )

    Scaffold(
        bottomBar = { MyBottomBar(navController) }, // barra navegable
    ) { paddingValues ->


        NexoPullToRefresh(
            onRefresh = { viewModel.refrescarMetas().join() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Text("Mis metas", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Crea y visualiza tus objetivos de ahorro", color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
            Spacer(modifier = Modifier.height(28.dp))

            InputField(viewModel.nombreMeta, "Nombre de la meta", containerColor = paleta.surface) { viewModel.onNombreMetaChange(it) }
            Spacer(modifier = Modifier.height(14.dp))
            InputField(viewModel.montoObjetivo, "Monto objetivo", keyboardType = KeyboardType.Decimal, leadingText = "$", containerColor = paleta.surface) { viewModel.onMontoObjetivoChange(it) }
            Spacer(modifier = Modifier.height(14.dp))
            InputField(viewModel.fechaLimite, "Fecha límite (YYYY-MM-DD)", keyboardType = KeyboardType.Number, formatDate = true, containerColor = paleta.surface) { viewModel.onFechaLimiteChange(it) }
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
                CircularProgressIndicator(color = paleta.accent)
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Objetivo: $${meta.montoObjetivo}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                                Text("Ahorrado: $${meta.totalAhorrado}", color = Color.White.copy(alpha = 0.9f), fontSize = 13.sp)
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Fecha objetivo: ${meta.fechaLimite}", color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
                                Text("Creada: ${meta.fechaCreacion.take(10)}", color = Color.White.copy(alpha = 0.65f), fontSize = 12.sp)
                            }
                        }
                        val completada = !meta.activa || progreso >= 1f
                        val estado = when {
                            completada -> "Completada"
                            meta.diasSinAbonos >= 60 -> "Sin abonos desde hace ${meta.diasSinAbonos} días · retómala cuando quieras"
                            meta.diasSinAbonos >= 30 -> "Sin abonos desde hace ${meta.diasSinAbonos} días"
                            else -> "Activa"
                        }
                        Row(
                            modifier = Modifier.padding(top = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                estado,
                                color = if (completada) Color(0xFF55D68B) else paleta.accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp
                            )
                            if (completada) {
                                Text("✓", color = Color(0xFF55D68B), fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(progress = { progreso }, color = paleta.accent, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "${(progreso * 100).toInt()}% completado",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                        if (!completada) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .background(paleta.accent, RoundedCornerShape(24.dp))
                                    .clickable {
                                        montoAbono = ""
                                        metaParaAbonar = meta
                                    }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("＋ Abonar", color = Color(0xFF101722), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(30.dp))
        }
        }
    }

    metaParaAbonar?.let { meta ->
        AlertDialog(
            onDismissRequest = { if (!abonoGuardando) metaParaAbonar = null },
            title = { Text("Abonar a ${meta.nombreMeta}") },
            containerColor = paleta.surface,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB8C2D1),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Falta $${"%.2f".format(Locale.US, (meta.montoObjetivo - meta.totalAhorrado).coerceAtLeast(0.0))} para completar la meta.")
                    OutlinedTextField(
                        value = montoAbono,
                        onValueChange = { value ->
                            val limpio = value.filter { it.isDigit() || it == '.' || it == ',' }
                            val separador = limpio.indexOfFirst { it == '.' || it == ',' }
                            montoAbono = if (separador < 0) limpio else limpio.take(separador + 1) + limpio.drop(separador + 1).filter(Char::isDigit).take(2)
                        },
                        label = { Text("Monto del abono") },
                        prefix = { Text("$ ") },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = paleta.background,
                            unfocusedContainerColor = paleta.background,
                            focusedIndicatorColor = paleta.accent,
                            unfocusedIndicatorColor = paleta.accent.copy(alpha = .45f),
                            focusedLabelColor = paleta.accent,
                            unfocusedLabelColor = Color.White.copy(alpha = .65f)
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = !abonoGuardando, onClick = {
                    viewModel.registrarAbono(meta.idMeta, montoAbono) { concepto, monto ->
                        movimientosViewModel.registrarMovimientoPresupuesto(concepto, monto)
                    }
                    metaParaAbonar = null
                }) { Text(if (abonoGuardando) "Guardando…" else "Guardar abono", color = paleta.accent) }
            },
            dismissButton = {
                TextButton(enabled = !abonoGuardando, onClick = { metaParaAbonar = null }) { Text("Cancelar") }
            }
        )
    }
}
