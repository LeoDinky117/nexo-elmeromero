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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
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
import com.nexo.app.ui.viewModel.PresupuestoViewModel
import com.nexo.app.viewmodel.MetasViewModel
import com.nexo.app.model.GrupoPresupuesto
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.nexo.app.ui.LocalNexoPalette
import nexo.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource


@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddMovimientoScreen(navController: NavController,
                        movimientosVM: MovimientosViewModel,
                        metasVM: MetasViewModel,
                        presupuestoVM: PresupuestoViewModel
                         ) {

    val context = LocalContext.current
    LaunchedEffect(Unit) {
        movimientosVM.onTipoMovimientoChange("Gasto")
        movimientosVM.onFechaChange(LocalDate.now().toString())
        metasVM.refrescarMetas()
        presupuestoVM.refrescar()
    }
    val metas by metasVM.metas.collectAsState()
    val presupuestos by presupuestoVM.items.collectAsState()
    val scope = rememberCoroutineScope()
    var destinoPago by remember { mutableStateOf("Meta") }
    var idMetaPago by remember { mutableStateOf<Int?>(null) }
    var idDeudaPago by remember { mutableStateOf<String?>(null) }
    var errorPago by remember { mutableStateOf<String?>(null) }
    var guardandoPago by remember { mutableStateOf(false) }
    var mostrarCalendario by remember { mutableStateOf(false) }
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
    val colorMoradoNexo = paleta.accentTextOnSurface

    Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colorFondo)
                .verticalScroll(rememberScrollState())
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
                    color = paleta.onBackground,
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
                Text("MONTO", color = paleta.secondaryOnBackground, fontSize = 12.sp, letterSpacing = 2.sp,
                    fontWeight = FontWeight.Medium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ){
                    Text(
                        text = "$",
                        color = paleta.onBackground,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.ExtraBold

                    )
                    BasicTextField(
                        value = movimientosVM.montoInput,
                        onValueChange = { movimientosVM.onMontoChange(it) },
                        textStyle = TextStyle(
                            color = paleta.onBackground,
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
                                Text("0", color = paleta.secondaryOnBackground,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.ExtraBold)
                            }
                            innerTextField()
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Selector de ingreso, gasto o pago.

            Text("Tipo", color = paleta.secondaryOnBackground,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(colorContenedor, RoundedCornerShape(18.dp))
                    .padding(4.dp)
            ) {
                listOf("Ingreso", "Gasto", "Pago").forEach { tipo ->
                    val seleccionado = when (tipo) {
                        "Ingreso" -> movimientosVM.esIngresoInput
                        "Pago" -> movimientosVM.esPagoInput
                        else -> !movimientosVM.esIngresoInput && !movimientosVM.esPagoInput
                    }
                    Box(
                        modifier = Modifier.weight(1f).fillMaxHeight()
                            .background(if (seleccionado) degradadoRosa else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)), RoundedCornerShape(14.dp))
                            .clickable { movimientosVM.onTipoMovimientoChange(tipo); errorPago = null },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(tipo, color = if (seleccionado) paleta.onAccent else paleta.secondaryOnSurface, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (movimientosVM.esPagoInput) {
                Text("Pagar a", color = paleta.secondaryOnBackground, fontSize = 14.sp, modifier = Modifier.padding(bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Meta", "Deuda").forEach { destino ->
                        FilterChip(
                            selected = destinoPago == destino,
                            onClick = { destinoPago = destino; errorPago = null; idMetaPago = null; idDeudaPago = null },
                            label = { Text(destino) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = paleta.accent,
                                selectedLabelColor = paleta.onAccent,
                                labelColor = paleta.onBackground
                            )
                        )
                    }
                }
                val opcionesMeta = metas.filter { it.activa && it.totalAhorrado < it.montoObjetivo }
                val opcionesDeuda = presupuestos.filter {
                    it.activo && (it.grupo == GrupoPresupuesto.DEUDA || it.grupo == GrupoPresupuesto.DIFERIDO) && (it.saldoActual ?: 0.0) > 0.0
                }
                if (destinoPago == "Meta") {
                    if (opcionesMeta.isEmpty()) Text("No tienes metas activas con saldo pendiente.", color = paleta.secondaryOnBackground)
                    opcionesMeta.forEach { meta ->
                        val seleccionado = idMetaPago == meta.idMeta
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { idMetaPago = meta.idMeta; errorPago = null },
                            colors = CardDefaults.cardColors(containerColor = if (seleccionado) paleta.surface.copy(alpha = .75f) else colorContenedor),
                            border = if (seleccionado) androidx.compose.foundation.BorderStroke(1.dp, paleta.accent) else null
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(meta.nombreMeta, color = paleta.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Falta ${"$"}${"%.2f".format(Locale.US, (meta.montoObjetivo - meta.totalAhorrado).coerceAtLeast(0.0))}", color = paleta.highlightTextOnSurface)
                            }
                        }
                    }
                } else {
                    if (opcionesDeuda.isEmpty()) Text("No tienes deudas con saldo pendiente.", color = paleta.secondaryOnBackground)
                    opcionesDeuda.forEach { deuda ->
                        val seleccionado = idDeudaPago == deuda.id
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { idDeudaPago = deuda.id; errorPago = null },
                            colors = CardDefaults.cardColors(containerColor = if (seleccionado) paleta.surface.copy(alpha = .75f) else colorContenedor),
                            border = if (seleccionado) androidx.compose.foundation.BorderStroke(1.dp, paleta.accent) else null
                        ) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(deuda.concepto, color = paleta.onSurface, fontWeight = FontWeight.SemiBold)
                                Text("Pendiente ${"$"}${"%.2f".format(Locale.US, deuda.saldoActual ?: 0.0)}", color = paleta.highlightTextOnSurface)
                            }
                        }
                    }
                }
                errorPago?.let { Text(it, color = Color(0xFFFF7085), modifier = Modifier.padding(top = 8.dp)) }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // SECCIÓN DE CATEGORÍAS
            if (!movimientosVM.esPagoInput) {
                Text("Categoría",
                    color = paleta.secondaryOnBackground,
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
                            color = if (movimientosVM.categoriaSeleccionada == cat.nombre) colorMoradoNexo else paleta.secondaryOnSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }

                }
                }
            }


            Spacer(modifier = Modifier.height(24.dp))

        // --- CAMPO DE FECHA
        Text("Fecha", color = paleta.secondaryOnBackground,
            fontSize = 14.sp,
            modifier = Modifier.padding(bottom = 8.dp))
        val fechaActual = runCatching { LocalDate.parse(movimientosVM.fechaInput) }.getOrDefault(LocalDate.now())
        val fechaTexto = if (fechaActual == LocalDate.now()) "Hoy · ${fechaActual.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX")))}"
            else fechaActual.format(DateTimeFormatter.ofPattern("d MMM yyyy", Locale("es", "MX")))
        OutlinedTextField(
            value = fechaTexto,
            onValueChange = {},
            readOnly = true,
            leadingIcon = {
                Image(
                    painter = painterResource(Res.drawable.icon_calendar),
                    contentDescription = null,
                    modifier = Modifier.size(22.dp)
                )
            },
            trailingIcon = {
                IconButton(onClick = { mostrarCalendario = true }) {
                    Icon(
                        painter = painterResource(Res.drawable.icon_calendar),
                        contentDescription = "Abrir calendario",
                        tint = paleta.highlightTextOnSurface
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            shape = RoundedCornerShape(18.dp),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = paleta.surface,
                unfocusedContainerColor = paleta.surface,
                focusedTextColor = paleta.onSurface,
                unfocusedTextColor = paleta.onSurface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            singleLine = true
        )
        if (mostrarCalendario) {
            val pickerState = rememberDatePickerState(
                initialSelectedDateMillis = fechaActual.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            )
            DatePickerDialog(
                onDismissRequest = { mostrarCalendario = false },
                confirmButton = {
                    TextButton(onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            movimientosVM.onFechaChange(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString())
                        }
                        mostrarCalendario = false
                    }) { Text("Aceptar") }
                },
                dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
            ) { DatePicker(state = pickerState) }
        }

            Spacer(modifier = Modifier.height(24.dp))

            // --- CAMPO DE CONCEPTO (TextField Dark Mode con Icono) ---

            Text("Concepto",
                color = paleta.secondaryOnBackground,
                fontSize = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp))
            TextField(
                value = movimientosVM.descripcionInput,
                onValueChange = { movimientosVM.onDescripcionChanged(it)},
                placeholder = { Text("Ej. Cena...", color = paleta.secondaryOnSurface) },
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
                    focusedTextColor = paleta.onSurface,
                    unfocusedTextColor = paleta.onSurface,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                singleLine = true
            )
            if (vozEstado.isNotBlank()) {
                Text(
                    text = vozEstado,
                    color = paleta.secondaryOnBackground,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 6.dp, start = 8.dp)
                )
            }

            // --- BOTÓN DE GUARDAR (Abajo, con degradado rosa completo) ---

            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = {
                    if (!movimientosVM.esPagoInput) {
                        movimientosVM.registrarMovimiento {
                            navController.navigate(Routes.SUCCESS) { popUpTo(Routes.ADD_MOVIMIENTO) { inclusive = true } }
                        }
                    } else if (guardandoPago) {
                        Unit
                    } else {
                        val monto = movimientosVM.montoInput.replace(",", ".").toDoubleOrNull()
                        val meta = metas.firstOrNull { it.idMeta == idMetaPago && it.activa }
                        val deuda = presupuestos.firstOrNull {
                            it.id == idDeudaPago && it.activo &&
                                (it.grupo == GrupoPresupuesto.DEUDA || it.grupo == GrupoPresupuesto.DIFERIDO) &&
                                (it.saldoActual ?: 0.0) > 0.0
                        }
                        val categoriaPreferida = movimientosVM.categorias.firstOrNull {
                            it.nombre == if (destinoPago == "Meta") "Otros" else "Deudas"
                        } ?: movimientosVM.categorias.firstOrNull()
                        when {
                            monto == null || monto <= 0.0 -> errorPago = "Escribe un monto mayor que cero."
                            categoriaPreferida == null -> errorPago = "No hay una categoría de gasto disponible."
                            destinoPago == "Meta" && meta == null -> errorPago = "Selecciona una meta activa."
                            destinoPago == "Deuda" && deuda == null -> errorPago = "Selecciona una deuda pendiente."
                            destinoPago == "Meta" && meta != null && monto > meta.montoObjetivo - meta.totalAhorrado + 0.000001 -> errorPago = "El monto supera lo que falta para completar la meta."
                            destinoPago == "Deuda" && deuda != null && monto > (deuda.saldoActual ?: 0.0) + 0.000001 -> errorPago = "El monto supera el saldo pendiente de la deuda."
                            else -> scope.launch {
                                guardandoPago = true
                                val exito = if (destinoPago == "Meta" && meta != null) {
                                    movimientosVM.registrarPagoMeta(meta.idMeta, monto, movimientosVM.fechaInput, "Pago a meta: ${meta.nombreMeta}", categoriaPreferida.idCategoria).also {
                                        if (it) metasVM.refrescarMetas().join()
                                    }
                                } else if (deuda != null) {
                                    movimientosVM.registrarPagoDeuda("Pago de deuda: ${deuda.concepto}", monto, movimientosVM.fechaInput, categoriaPreferida.idCategoria).also {
                                        if (it) presupuestoVM.actualizarSaldoDeuda(deuda.id, ((deuda.saldoActual ?: 0.0) - monto).coerceAtLeast(0.0).toString())
                                    }
                                } else false
                                guardandoPago = false
                                if (exito) navController.navigate(Routes.SUCCESS) { popUpTo(Routes.ADD_MOVIMIENTO) { inclusive = true } }
                                else errorPago = "No se pudo guardar el pago. Revisa tu conexión e inténtalo de nuevo."
                            }
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
                    Text(if (guardandoPago) "Guardando..." else if (movimientosVM.esPagoInput) "Registrar pago" else "Guardar", color = paleta.onAccent,
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
