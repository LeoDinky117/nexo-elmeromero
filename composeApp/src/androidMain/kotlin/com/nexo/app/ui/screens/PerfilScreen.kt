package com.nexo.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.nexo.app.ui.viewModel.PerfilViewModel
import com.nexo.app.model.Recompensa
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.components.RewardVisual

@Composable
fun PerfilScreen(
    navController: NavController,
    viewModel: PerfilViewModel,
    onCerrarSesion: () -> Unit
) {
    val perfil by viewModel.perfil.collectAsState()
    val cargando by viewModel.cargando.collectAsState()
    val mensajeUI by viewModel.mensajeUI.collectAsState()
    val tienda by viewModel.tienda.collectAsState()
    val tiendaCargando by viewModel.tiendaCargando.collectAsState()
    val accionRecompensaEnCurso by viewModel.accionRecompensaEnCurso.collectAsState()
    val guardandoPerfil by viewModel.guardandoPerfil.collectAsState()
    val paleta = LocalNexoPalette.current
    val context = LocalContext.current
    var campoEditado by remember { mutableStateOf<String?>(null) }
    var confirmarCerrarSesion by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.recargarPerfil()
    }

    LaunchedEffect(viewModel.modoEdicion) {
        if (!viewModel.modoEdicion) campoEditado = null
    }


    LaunchedEffect(mensajeUI) {
        mensajeUI?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.mensajeMostrado()
        }
    }

    val fondo = Brush.verticalGradient(listOf(paleta.background, paleta.background.copy(alpha = 0.92f), Color(0xFF050C1A)))
    Scaffold(
        bottomBar = { MyBottomBar(navController) }, //barra navegable

        ) { paddingValues ->

        NexoPullToRefresh(
            onRefresh = { viewModel.recargarPerfil().join() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(fondo)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text("Mi perfil", color = paleta.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.objetivoGuia("profile_title"))
            Spacer(modifier = Modifier.height(24.dp))

            if (cargando) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = paleta.accent)
                }
            } else if (perfil != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(paleta.surface, RoundedCornerShape(20.dp))
                        .padding(20.dp)
                ) {
                    tienda?.iconoVisualActivo?.let { RewardVisual(it, modifier = Modifier.size(48.dp).padding(bottom = 12.dp), emojiFontSize = 36.sp) }
                    ItemPerfilEditable("Nombre", perfil!!.nombre, onEditar = {
                        viewModel.iniciarEdicion()
                        campoEditado = "nombre"
                    })
                    ItemPerfilEditable("Correo", perfil!!.correo, onEditar = {
                        viewModel.iniciarEdicion()
                        campoEditado = "correo"
                    })
                    ItemPerfil("Edad", perfil!!.edad.toString())
                    ItemPerfil("Puntos", perfil!!.puntos.toString())
                    ItemPerfil("Fecha de registro", perfil!!.fechaRegistro)
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text("Recompensas", color = paleta.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(6.dp))
                Text("Desbloquea temas e iconos con los puntos que ganas al registrar movimientos.", color = paleta.secondaryOnBackground, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .background(paleta.surface, RoundedCornerShape(18.dp))
                        .padding(18.dp)
                ) {
                    Text(tienda?.let { "${it.puntosDisponibles} puntos disponibles" } ?: "Consultando saldo de puntos…", color = paleta.highlight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Los puntos no tienen valor monetario; canjéalos por artículos digitales.", color = paleta.secondaryOnSurface, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(14.dp))
                if (tiendaCargando && tienda == null) {
                    CircularProgressIndicator(color = paleta.accent, modifier = Modifier.align(Alignment.CenterHorizontally))
                } else if (tienda?.recompensas.isNullOrEmpty()) {
                    Text("El catálogo no está disponible por ahora.", color = paleta.secondaryOnBackground)
                } else {
                    tienda!!.recompensas.forEach { premio ->
                        RecompensaPerfilCard(
                            premio = premio,
                            puntos = tienda!!.puntosDisponibles,
                            ocupada = accionRecompensaEnCurso,
                            accent = paleta.accent,
                            onDesbloquear = { viewModel.desbloquearRecompensa(premio.id) },
                            onEquipar = { viewModel.equiparRecompensa(premio.tipo, premio.codigo) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("No se pudieron mostrar los datos del perfil.", color = paleta.secondaryOnBackground)
                    Button(
                        onClick = viewModel::recargarPerfil,
                        colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)
                    ) {
                        Text("Volver a intentar", color = Color(0xFF111A27))
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { confirmarCerrarSesion = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)
            ) {
                Text("Cerrar sesión", color = paleta.onAccent)
            }
        }
        }
    }

    campoEditado?.let { campo ->
        val esCorreo = campo == "correo"
        AlertDialog(
            onDismissRequest = { if (!guardandoPerfil) { campoEditado = null; viewModel.cancelarEdicion() } },
            title = { Text(if (esCorreo) "Editar correo" else "Editar nombre") },
            text = {
                OutlinedTextField(
                    value = if (esCorreo) viewModel.correoEditado else viewModel.nombreEditado,
                    onValueChange = if (esCorreo) viewModel::cambiarCorreo else viewModel::cambiarNombre,
                    label = { Text(if (esCorreo) "Correo electrónico" else "Nombre") },
                    singleLine = true,
                    keyboardOptions = if (esCorreo) androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Email) else androidx.compose.foundation.text.KeyboardOptions.Default,
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = paleta.onBackground, unfocusedTextColor = paleta.onBackground,
                        focusedContainerColor = paleta.background, unfocusedContainerColor = paleta.background,
                        focusedIndicatorColor = paleta.accent, unfocusedIndicatorColor = paleta.accent.copy(alpha = .5f),
                        focusedLabelColor = paleta.accentTextOnBackground, unfocusedLabelColor = paleta.secondaryOnBackground
                    )
                )
            },
            confirmButton = {
                Button(onClick = viewModel::guardarPerfil, enabled = !guardandoPerfil,
                    colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)) {
                    Text(if (guardandoPerfil) "Guardando…" else "Guardar", color = paleta.onAccent)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { campoEditado = null; viewModel.cancelarEdicion() }, enabled = !guardandoPerfil) { Text("Cancelar") }
            },
            containerColor = paleta.surface,
            titleContentColor = paleta.onSurface,
            textContentColor = paleta.onSurface
        )
    }

    if (confirmarCerrarSesion) {
        AlertDialog(
            onDismissRequest = { confirmarCerrarSesion = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Volverás al inicio de sesión. Tus datos guardados seguirán en tu cuenta.") },
            confirmButton = {
                Button(
                    onClick = {
                        confirmarCerrarSesion = false
                        onCerrarSesion()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)
                ) { Text("Cerrar sesión", color = paleta.onAccent) }
            },
            dismissButton = {
                OutlinedButton(onClick = { confirmarCerrarSesion = false }) { Text("Cancelar") }
            },
            containerColor = paleta.surface,
            titleContentColor = paleta.onSurface,
            textContentColor = paleta.onSurface
        )
    }

}

@Composable
private fun ItemPerfil(titulo: String, valor: String) {
    val paleta = LocalNexoPalette.current
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(text = titulo, color = paleta.accentTextOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = valor, color = paleta.onSurface, fontSize = 17.sp)
    }
}

@Composable
private fun ItemPerfilEditable(titulo: String, valor: String, onEditar: () -> Unit) {
    val paleta = LocalNexoPalette.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(titulo, color = paleta.accentTextOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(4.dp))
            Text(valor, color = paleta.onSurface, fontSize = 17.sp)
        }
        IconButton(onClick = onEditar) {
            Text("✎", color = paleta.highlight, fontSize = 22.sp)
        }
    }
}

@Composable
private fun RecompensaPerfilCard(
    premio: Recompensa,
    puntos: Int,
    ocupada: Boolean,
    accent: Color,
    onDesbloquear: () -> Unit,
    onEquipar: () -> Unit
) {
    val paleta = LocalNexoPalette.current
    val coloresTema = if (premio.tipo == "TEMA") premio.valorVisual.split('|').mapNotNull {
        runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull()
    } else emptyList()
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(paleta.surface, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (premio.tipo == "ICONO") {
            RewardVisual(premio.valorVisual, modifier = Modifier.size(44.dp).padding(end = 10.dp), emojiFontSize = 30.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.padding(end = 12.dp)) {
                coloresTema.take(4).forEach { color ->
                    Box(Modifier.size(14.dp).background(color, RoundedCornerShape(50.dp)))
                }
            }
        }
        Column(Modifier.weight(1f)) {
            Text(premio.nombre, color = paleta.onSurface, fontWeight = FontWeight.SemiBold)
            Text(premio.descripcion, color = paleta.secondaryOnSurface, fontSize = 12.sp)
            Text(if (premio.desbloqueada) "Desbloqueada" else "${premio.costoPuntos} puntos", color = paleta.accentTextOnSurface, fontSize = 12.sp)
        }
        when {
            premio.equipada -> Text("En uso", color = paleta.accentTextOnSurface, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            premio.desbloqueada -> OutlinedButton(onClick = onEquipar, enabled = !ocupada) { Text("Usar") }
            else -> Button(
                onClick = onDesbloquear,
                enabled = !ocupada && puntos >= premio.costoPuntos,
                colors = ButtonDefaults.buttonColors(containerColor = accent)
            ) { Text("Canjear", color = paleta.onAccent) }
        }
    }
}
