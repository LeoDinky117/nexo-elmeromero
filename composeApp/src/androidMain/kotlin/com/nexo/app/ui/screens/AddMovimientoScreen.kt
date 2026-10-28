package com.nexo.app.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.core.content.ContextCompat
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.ui.LocalNexoPalette
import nexo.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource


@Composable
fun AddMovimientoScreen(navController: NavController,
                        movimientosVM: MovimientosViewModel
                         ) {

    val context = LocalContext.current
    LaunchedEffect(movimientosVM.esIngresoInput) {
        movimientosVM.cargarCategorias(if (movimientosVM.esIngresoInput) "Ingreso" else "Gasto")
    }
    var vozEstado by remember { mutableStateOf("") }
    var escuchando by remember { mutableStateOf(false) }
    val viewModelActual by rememberUpdatedState(movimientosVM)
    val reconocedor = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else null
    }

    val iniciarDictado: () -> Unit = {
        if (reconocedor == null) {
            vozEstado = "No hay un servicio de reconocimiento de voz disponible."
        } else {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-MX")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Di el tipo, monto y concepto del movimiento")
            }
            try {
                vozEstado = "Escuchando..."
                escuchando = true
                reconocedor.startListening(intent)
            } catch (_: Exception) {
                vozEstado = "No se pudo iniciar el reconocimiento. Intenta de nuevo."
                escuchando = false
            }
        }
    }

    val permisoMicrofono = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido ->
        if (concedido) iniciarDictado()
        else vozEstado = "Activa el permiso del micrófono para dictar el movimiento."
    }

    DisposableEffect(reconocedor) {
        reconocedor?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { vozEstado = "Escuchando..." }
            override fun onBeginningOfSpeech() { vozEstado = "Te estoy escuchando..." }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { vozEstado = "Procesando voz..." }
            override fun onError(error: Int) {
                escuchando = false
                vozEstado = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No entendí lo que dijiste. Intenta otra vez."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Falta el permiso del micrófono."
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "El servicio de voz necesita conexión o no respondió."
                    else -> "Falló el reconocimiento de voz. Intenta otra vez."
                }
            }
            override fun onResults(results: Bundle?) {
                escuchando = false
                val transcripcion = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                if (transcripcion.isNullOrBlank()) {
                    vozEstado = "No se reconoció una frase. Intenta otra vez."
                } else {
                    val borradorAgregado = viewModelActual.aplicarComandoVoz(transcripcion)
                    if (borradorAgregado) {
                        navController.navigate(Routes.MAIN_HOME) {
                            popUpTo(Routes.ADD_MOVIMIENTO) { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        vozEstado = "Dictado reconocido. Di el tipo, monto y concepto para crear un borrador; revisa los campos."
                    }
                }
            }
            override fun onPartialResults(partialResults: Bundle?) = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { reconocedor?.destroy() }
    }

    val solicitarOVerificarMicrofono: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            iniciarDictado()
        } else {
            permisoMicrofono.launch(Manifest.permission.RECORD_AUDIO)
        }
    }


    val paleta = LocalNexoPalette.current
    val colorFondo = paleta.background
    val colorContenedor = paleta.surface
    val degradadoRosa = Brush.horizontalGradient(listOf(paleta.accent, paleta.highlight))
    val colorMoradoNexo = paleta.accent

    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorFondo)
                .padding(horizontal = 24.dp)
        ) {
            // --- HEADER
            Row(
                modifier = Modifier.padding(top = 40.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Image(
                        painter = painterResource(Res.drawable.icon_back),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    "Agregar movimiento",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- SECCIÓN DEL MONTO  ---
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("MONTO", color = Color.Gray, fontSize = 12.sp, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "$",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold

                    )
                    BasicTextField(
                        value = movimientosVM.montoInput,
                        onValueChange = { movimientosVM.onMontoChange(it) },
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Start
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number //teclado de num.
                        ),
                        cursorBrush = SolidColor(paleta.accent),
                        modifier = Modifier
                            .width(IntrinsicSize.Min)
                            .padding(start = 4.dp),
                        decorationBox = { innerTextField ->
                            if(movimientosVM.montoInput.isEmpty()){
                                Text("0", color = Color.DarkGray,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // SELECTOR TIPO (Ingreso / Gasto con Degradado)

            Text("Tipo", color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(colorContenedor, RoundedCornerShape(18.dp))
                    .padding(4.dp)
            ) {
                // Botón Ingreso (Con Degradado Rosa)
                Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            if (movimientosVM.esIngresoInput) degradadoRosa
                            else Brush.linearGradient(
                                listOf(Color.Transparent, Color.Transparent)
                            ),
                            RoundedCornerShape(14.dp)
                        )
                        .clickable { movimientosVM.onEsIngresoChange(true) }, contentAlignment = Alignment.Center) {
                    Text("+ Ingreso", color = if (movimientosVM.esIngresoInput) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold)
                }
                // Botón Gasto (Oscuro)
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight().background(
                        if (!movimientosVM.esIngresoInput) degradadoRosa
                        else Brush.linearGradient(
                            listOf(Color.Transparent, Color.Transparent)
                        ),
                        RoundedCornerShape(14.dp)
                    )
                        .clickable { movimientosVM.onEsIngresoChange(false)},
                    contentAlignment = Alignment.Center
                ) {
                    Text("- Gasto", color = if (!movimientosVM.esIngresoInput) Color.White else Color.Gray,
                        fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SECCIÓN DE CATEGORÍAS
            Text("Categoría",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(movimientosVM.categorias) { cat ->

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .size(height = 90.dp, width = 75.dp) // Tamaño del cuadrito
                            .background(colorContenedor, RoundedCornerShape(16.dp))
                            // Si está seleccionada, le ponemos el borde morado
                            .border(
                                if (movimientosVM.categoriaSeleccionada == cat.nombre) 2.dp else 0.dp,
                                if (movimientosVM.categoriaSeleccionada == cat.nombre) colorMoradoNexo else Color.Transparent,
                                RoundedCornerShape(16.dp)
                            )
                            .clickable { movimientosVM.onCategoriaChange(cat) }
                            .padding(top = 16.dp)
                    ) {
                        Text(text = iconoCategoria(cat.nombre), fontSize = 24.sp)
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = cat.nombre,
                            color = if (movimientosVM.categoriaSeleccionada == cat.nombre) colorMoradoNexo else Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                }
            }


            Spacer(modifier = Modifier.height(24.dp))

        // --- CAMPO DE FECHA
        Text("Fecha", color = Color.Gray,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        TextField(
            value = movimientosVM.fechaInput,
            onValueChange = { movimientosVM.onFechaChange(it)},
            leadingIcon = {
                Image(
                    painter = painterResource(Res.drawable.icon_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = paleta.surface,
                unfocusedContainerColor = paleta.surface,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMPO DE CONCEPTO (TextField Dark Mode con Icono) ---

            Text("Concepto",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = movimientosVM.descripcionInput,
                onValueChange = { movimientosVM.onDescripcionChanged(it)},
                placeholder = { Text("Ej. Cena...", color = Color.DarkGray) },
                leadingIcon = { Image(painter = painterResource(Res.drawable.icon_concepto),
                    contentDescription = null, modifier = Modifier.size(20.dp)) },
                trailingIcon = {
                    TextButton(
                        onClick = solicitarOVerificarMicrofono,
                        enabled = !escuchando
                    ) {
                        Text(if (escuchando) "Escuchando" else "🎙 Dictar", color = paleta.highlight, fontSize = 12.sp)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                shape = RoundedCornerShape(18.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colorContenedor,
                    unfocusedContainerColor = colorContenedor,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            if (vozEstado.isNotBlank()) {
                Text(
                    text = vozEstado,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 8.dp)
                )
            }

            // --- BOTÓN DE GUARDAR (Abajo, con degradado rosa completo) ---

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    movimientosVM.registrarMovimiento {
                        navController.navigate(Routes.SUCCESS){
                            popUpTo(Routes.ADD_MOVIMIENTO){inclusive = true}
                        }

                    }
                          },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(degradadoRosa),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Guardar", color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold)
                }
            }
        }
    }
private fun iconoCategoria(nombre: String): String = when (nombre) {
    "Trabajo" -> "💼"
    "Comida" -> "🍽️"
    "Ocio" -> "🎮"
    "Trans." -> "🚗"
    "Ventas" -> "🛍️"
    "Freelance" -> "🧑‍💻"
    "Inversiones" -> "📈"
    "Regalo" -> "🎁"
    "Otros ingresos" -> "💵"
    "Hogar" -> "🏠"
    "Servicios" -> "💡"
    "Salud" -> "🩺"
    "Educación" -> "📚"
    "Compras" -> "🛒"
    "Suscripciones" -> "📱"
    "Deudas" -> "💳"
    "Viajes" -> "✈️"
    "Mascotas" -> "🐾"
    else -> "📦"
}
