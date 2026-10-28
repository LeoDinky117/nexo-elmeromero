package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexo.app.model.CatalogoConceptosPresupuesto
import com.nexo.app.model.ConceptoPresupuesto
import com.nexo.app.model.EventosPresupuesto
import com.nexo.app.model.EventoCalendario
import com.nexo.app.model.FlujoPresupuesto
import com.nexo.app.model.GrupoPresupuesto
import com.nexo.app.model.ItemPresupuesto
import com.nexo.app.model.PeriodicidadPresupuesto
import com.nexo.app.model.TipoEventoCalendario
import com.nexo.app.ui.viewModel.PresupuestoViewModel
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.LocalNexoPalette
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PresupuestoScreen(
    navController: NavController,
    viewModel: PresupuestoViewModel,
    movimientosViewModel: MovimientosViewModel
) {
    val paleta = LocalNexoPalette.current
    val items by viewModel.items.collectAsState()
    val resumen = viewModel.resumen(movimientosViewModel.saldoTotal)
    val fechaHoy = LocalDate.now()
    val compromisosProximos = remember(items, fechaHoy) {
        EventosPresupuesto.proximos(items, desde = fechaHoy, limite = 100)
            .filter { it.tipo == TipoEventoCalendario.PAGO }
            .filter { runCatching { LocalDate.parse(it.fecha) }.getOrNull()?.isBefore(fechaHoy.plusDays(31)) == true }
    }
    val totalCompromisosProximos = compromisosProximos.sumOf { it.monto }
    androidx.compose.runtime.LaunchedEffect(viewModel.userId) {
        if (viewModel.userId > 0) movimientosViewModel.cargarDatosDelServidor()
    }
    var conceptoSeleccionado by remember { mutableStateOf<ConceptoPresupuesto?>(null) }
    var mostrarConceptos by remember { mutableStateOf(false) }
    var monto by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var corte by remember { mutableStateOf("") }
    var saldo by remember { mutableStateOf("") }
    var plazo by remember { mutableStateOf("") }
    var periodicidad by remember { mutableStateOf(PeriodicidadPresupuesto.MENSUAL) }
    var mostrarPeriodicidades by remember { mutableStateOf(false) }
    var editarSaldo by remember { mutableStateOf<ItemPresupuesto?>(null) }
    var saldoEditado by remember { mutableStateOf("") }
    var saldoEditadoError by remember { mutableStateOf<String?>(null) }
    var montoError by remember { mutableStateOf<String?>(null) }
    var diaError by remember { mutableStateOf<String?>(null) }
    var corteError by remember { mutableStateOf<String?>(null) }
    var saldoError by remember { mutableStateOf<String?>(null) }
    var plazoError by remember { mutableStateOf<String?>(null) }
    var compromisosDesplegados by remember { mutableStateOf(false) }
    val compromisosRequester = remember { BringIntoViewRequester() }
    val mensaje = viewModel.mensaje

    androidx.compose.runtime.LaunchedEffect(compromisosDesplegados) {
        if (compromisosDesplegados) compromisosRequester.bringIntoView()
    }

    Scaffold(
        containerColor = paleta.background,
        bottomBar = { MyBottomBar(navController) }
    ) { padding ->
        NexoPullToRefresh(
            onRefresh = {
                viewModel.refrescar()
                movimientosViewModel.cargarDatosDelServidor().join()
            },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Mi presupuesto", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Una vista clara de tu saldo y tus próximos compromisos.", color = Color(0xFFB8C2D1), fontSize = 14.sp)

            SaldoRegistradoCard(resumen.saldoCuenta)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResumenCompactoCard(
                    titulo = "Compromisos próximos",
                    monto = totalCompromisosProximos,
                    modifier = Modifier.weight(1f),
                    mostrarBoton = true,
                    nota = "Suma en 30 días",
                    onClick = { compromisosDesplegados = true }
                )
                ResumenCompactoCard(
                    titulo = "Saldo después de compromisos",
                    monto = resumen.saldoCuenta - totalCompromisosProximos,
                    modifier = Modifier.weight(1f),
                    nota = "Saldo actual menos pagos"
                )
            }

            BalanceSaldoDeudaCard(saldo = resumen.saldoCuenta, deuda = resumen.deudaActual)

            Card(
                colors = CardDefaults.cardColors(containerColor = paleta.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth().bringIntoViewRequester(compromisosRequester)
                    .clickable { compromisosDesplegados = !compromisosDesplegados }
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Desglose de compromisos", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("${compromisosProximos.size} próximos · siguientes 30 días", color = Color(0xFFB8C2D1), fontSize = 12.sp)
                    }
                    Text(if (compromisosDesplegados) "⌃" else "⌄", color = Color(0xFFFF5E7E), fontSize = 24.sp)
                }
            }
            if (compromisosDesplegados) {
                if (compromisosProximos.isEmpty()) {
                    Text("No tienes compromisos próximos registrados.", color = Color(0xFFB8C2D1), fontSize = 13.sp)
                } else {
                    compromisosProximos.forEach { evento ->
                        val periodicidadEvento = items.firstOrNull { it.id == evento.idItem }?.periodicidad ?: PeriodicidadPresupuesto.MENSUAL
                        CompromisoProximoRow(evento, periodicidadEvento)
                    }
                }
            }

            Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Necesidades básicas al mes", color = Color.White, fontWeight = FontWeight.SemiBold)
                        Text("Se calcula con los gastos que marques como esenciales.", color = Color(0xFFB8C2D1), fontSize = 12.sp)
                    }
                    Text(dinero(resumen.necesidadesBasicas), color = paleta.accent, fontWeight = FontWeight.Bold)
                }
            }

            Text("Agregar compromiso o ingreso", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            Text("Elige el concepto y cada cuánto ocurre.", color = Color(0xFFB8C2D1), fontSize = 13.sp)
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(paleta.accent.copy(alpha = 0.24f), paleta.highlight.copy(alpha = 0.18f))), RoundedCornerShape(17.dp))
                    .border(1.dp, paleta.accent, RoundedCornerShape(17.dp))
                    .clickable { mostrarConceptos = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("CONCEPTO · TOCA PARA ELEGIR", color = paleta.accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(conceptoSeleccionado?.nombre ?: "Seleccionar concepto", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Text("▾", color = paleta.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }

            if (conceptoSeleccionado != null) {
                val concepto = conceptoSeleccionado!!
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Periodicidad", color = Color(0xFFB8C2D1), fontSize = 13.sp)
                    Box {
                        TextButton(onClick = { mostrarPeriodicidades = true }) {
                            Text(periodicidadTexto(periodicidad) + "  ▾", color = paleta.accent, fontWeight = FontWeight.Bold)
                        }
                        androidx.compose.material3.DropdownMenu(
                            expanded = mostrarPeriodicidades,
                            onDismissRequest = { mostrarPeriodicidades = false }
                        ) {
                            PeriodicidadPresupuesto.entries.forEach { opcion ->
                                androidx.compose.material3.DropdownMenuItem(
                                    text = { Text(periodicidadTexto(opcion)) },
                                    onClick = { periodicidad = opcion; monto = ""; mostrarPeriodicidades = false }
                                )
                            }
                        }
                    }
                }
                if (periodicidad == PeriodicidadPresupuesto.VARIABLE) {
                    val estimado = viewModel.estimarMontoVariablePublico(concepto, movimientosViewModel.listaDeMovimientos.toList())
                    Text(
                        if (estimado != null) "Promedio estimado al mes: ${dinero(estimado)}" else "El estimado aparecerá cuando existan movimientos relacionados con este concepto.",
                        color = if (estimado != null) paleta.accent else Color(0xFFB8C2D1), fontSize = 13.sp
                    )
                } else OutlinedTextField(
                    value = monto,
                    onValueChange = { value ->
                        if (value.matches(Regex("\\d*([.,]\\d{0,2})?"))) {
                            monto = value
                            montoError = null
                        } else montoError = "Ingresa solo números y hasta 2 decimales."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(montoEtiqueta(periodicidad, concepto.flujo == FlujoPresupuesto.INGRESO)) },
                    prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = montoError != null,
                    supportingText = montoError?.let { error -> ({ Text(error) }) },
                    colors = presupuestoFieldColors()
                )
                OutlinedTextField(
                    value = dia,
                    onValueChange = { value ->
                        if (value.all(Char::isDigit) && value.length <= 2) {
                            dia = value
                            diaError = null
                        } else diaError = "Ingresa un día con números del 1 al 31."
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (concepto.flujo == FlujoPresupuesto.INGRESO) "Día esperado de ingreso (1–31)" else "Día de pago (1–31)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = diaError != null,
                    supportingText = diaError?.let { error -> ({ Text(error) }) },
                    colors = presupuestoFieldColors()
                )
                if (concepto.tieneFechaCorte) {
                    OutlinedTextField(
                        value = corte,
                        onValueChange = { value ->
                            if (value.all(Char::isDigit) && value.length <= 2) {
                                corte = value
                                corteError = null
                            } else corteError = "Ingresa un día de corte del 1 al 31."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Día de corte (1–31)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = corteError != null,
                        supportingText = corteError?.let { error -> ({ Text(error) }) },
                        colors = presupuestoFieldColors()
                    )
                }
                if (concepto.registraDeuda) {
                    OutlinedTextField(
                        value = saldo,
                        onValueChange = { value ->
                            if (value.matches(Regex("\\d*([.,]\\d{0,2})?"))) {
                                saldo = value
                                saldoError = null
                            } else saldoError = "Ingresa solo números y hasta 2 decimales."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Saldo pendiente actual") },
                        prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        isError = saldoError != null,
                        supportingText = saldoError?.let { error -> ({ Text(error) }) },
                        colors = presupuestoFieldColors()
                    )
                }
                if (concepto.requierePlazo) {
                    OutlinedTextField(
                        value = plazo,
                        onValueChange = { value ->
                            if (value.all(Char::isDigit) && value.length <= 3) {
                                plazo = value
                                plazoError = null
                            } else plazoError = "Ingresa un plazo en meses usando solo números."
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Meses restantes del plan") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = plazoError != null,
                        supportingText = plazoError?.let { error -> ({ Text(error) }) },
                        colors = presupuestoFieldColors()
                    )
                }
                Text(
                    if (concepto.esencial) "Se contará como esencial para calcular el ingreso mínimo." else "Se incluirá en tus compromisos mensuales.",
                    color = Color(0xFFB8C2D1), fontSize = 12.sp
                )
                Button(
                    onClick = {
                        viewModel.agregar(concepto, monto, dia, corte, saldo, plazo, periodicidad, movimientosViewModel.listaDeMovimientos.toList())
                        if (viewModel.mensaje?.startsWith("Se agregó") == true) {
                    conceptoSeleccionado = null; monto = ""; dia = ""; corte = ""; saldo = ""; plazo = ""
                            periodicidad = PeriodicidadPresupuesto.MENSUAL
                            montoError = null; diaError = null; corteError = null; saldoError = null; plazoError = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)
                ) { Text("Agregar al presupuesto", color = Color(0xFF111A27), fontWeight = FontWeight.Bold) }
            }

            Text("Mis conceptos", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 19.sp)
            if (items.isEmpty()) {
                Text("Aún no has agregado ingresos ni compromisos.", color = Color(0xFFB8C2D1))
            } else {
                items.forEach { item ->
                    ItemPresupuestoCard(
                        item = item,
                        onEliminar = { viewModel.eliminar(item.id) },
                        onEditarSaldo = {
                            editarSaldo = item
                            saldoEditado = (item.saldoActual ?: 0.0).toString()
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        }
    }

    if (mostrarConceptos) {
        ModalBottomSheet(
            onDismissRequest = { mostrarConceptos = false },
            containerColor = Color(0xFF151E2C)
        ) {
            Column(Modifier.fillMaxWidth().height(520.dp).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
                Text("Selecciona un concepto", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                GrupoPresupuesto.entries.forEach { grupo ->
                    val opciones = CatalogoConceptosPresupuesto.conceptos.filter { it.grupo == grupo }
                    if (opciones.isNotEmpty()) {
                        Text(tituloGrupo(grupo), color = paleta.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        opciones.forEach { concepto ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    conceptoSeleccionado = concepto
                                    monto = ""
                                    dia = ""
                                    corte = ""
                                    saldo = ""
                                    plazo = ""
                                    periodicidad = PeriodicidadPresupuesto.MENSUAL
                                    montoError = null; diaError = null; corteError = null; saldoError = null; plazoError = null
                                    mostrarConceptos = false
                                }.padding(vertical = 11.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(concepto.nombre, color = Color.White)
                                Text(if (concepto.flujo == FlujoPresupuesto.INGRESO) "Ingreso" else if (concepto.esencial) "Esencial" else "Compromiso", color = Color(0xFFB8C2D1), fontSize = 12.sp)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    editarSaldo?.let { item ->
        AlertDialog(
            onDismissRequest = { editarSaldo = null },
            title = { Text("Actualizar saldo de ${item.concepto}") },
            text = {
                OutlinedTextField(
                    value = saldoEditado,
                    onValueChange = { value ->
                        if (value.matches(Regex("\\d*([.,]\\d{0,2})?"))) {
                            saldoEditado = value
                            saldoEditadoError = null
                        } else saldoEditadoError = "Ingresa solo números y hasta 2 decimales."
                    },
                    label = { Text("Saldo pendiente") }, prefix = { Text("$ ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = saldoEditadoError != null,
                    supportingText = saldoEditadoError?.let { error -> ({ Text(error) }) },
                    colors = presupuestoFieldColors()
                )
            },
            confirmButton = { TextButton(onClick = { viewModel.actualizarSaldoDeuda(item.id, saldoEditado); editarSaldo = null }) { Text("Guardar") } },
            dismissButton = { TextButton(onClick = { editarSaldo = null }) { Text("Cancelar") } }
        )
    }

    if (mensaje != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::limpiarMensaje,
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = viewModel::limpiarMensaje) { Text("Entendido") } }
        )
    }
}

@Composable
private fun presupuestoFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LocalNexoPalette.current.surface,
    unfocusedContainerColor = LocalNexoPalette.current.surface,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    cursorColor = LocalNexoPalette.current.accent,
    focusedBorderColor = LocalNexoPalette.current.accent,
    unfocusedBorderColor = Color(0xFF526176),
    focusedLabelColor = LocalNexoPalette.current.accent,
    unfocusedLabelColor = Color(0xFFB8C2D1),
    focusedPrefixColor = Color.White,
    unfocusedPrefixColor = Color(0xFFB8C2D1),
    errorBorderColor = Color(0xFFFF7585),
    errorLabelColor = Color(0xFFFF7585),
    errorSupportingTextColor = Color(0xFFFF9BA5)
)

@Composable
private fun SaldoRegistradoCard(saldo: Double) {
    val paleta = LocalNexoPalette.current
    Box(
        Modifier.fillMaxWidth()
            .background(
                Brush.horizontalGradient(listOf(paleta.surface, paleta.background)),
                RoundedCornerShape(22.dp)
            )
            .padding(20.dp)
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Saldo registrado", color = Color(0xFFD7C7F2), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(dinero(saldo), color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                Text("Dinero disponible en tu cuenta", color = Color(0xFFB8C2D1), fontSize = 12.sp)
            }
            Box(
                Modifier.size(54.dp).background(paleta.accent.copy(alpha = .2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = paleta.highlight, fontSize = 27.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResumenCompactoCard(
    titulo: String,
    monto: Double,
    modifier: Modifier = Modifier,
    mostrarBoton: Boolean = false,
    nota: String,
    onClick: () -> Unit = {}
) {
    val paleta = LocalNexoPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = paleta.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier.heightIn(min = 137.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(titulo, color = Color(0xFFB8C2D1), fontSize = 12.sp, lineHeight = 16.sp, minLines = 2)
            Text(
                dinero(monto),
                color = if (monto < 0) Color(0xFFFF7899) else Color.White,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (mostrarBoton) {
                TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                    Text("Ver detalle  ›", color = paleta.highlight, fontSize = 12.sp)
                }
            } else {
                Text(nota, color = Color(0xFF8995A7), fontSize = 11.sp, maxLines = 1)
            }
        }
    }
}

@Composable
private fun BalanceSaldoDeudaCard(saldo: Double, deuda: Double) {
    val paleta = LocalNexoPalette.current
    val saldoPositivo = saldo.coerceAtLeast(0.0)
    val deudaPositiva = deuda.coerceAtLeast(0.0)
    val total = saldoPositivo + deudaPositiva
    val proporcionSaldo = if (total > 0.0) (saldoPositivo / total).toFloat().coerceIn(0f, 1f) else 0f
    Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Tu panorama · saldo vs. deuda", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Una vista rápida de lo que tienes registrado y tus deudas pendientes.", color = Color(0xFFB8C2D1), fontSize = 12.sp)
            Row(
                Modifier.fillMaxWidth().height(13.dp).background(Color(0xFF303A4A), CircleShape),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (total > 0.0) {
                    if (saldoPositivo > 0.0) Box(Modifier.weight(proporcionSaldo.coerceAtLeast(0.025f)).height(13.dp).background(Color(0xFF55D6A2), CircleShape))
                    if (deudaPositiva > 0.0) Box(Modifier.weight((1f - proporcionSaldo).coerceAtLeast(0.025f)).height(13.dp).background(Color(0xFFFF668E), CircleShape))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Saldo registrado", color = Color(0xFF72DCAF), fontSize = 11.sp)
                    Text(dinero(saldo), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Deuda pendiente", color = Color(0xFFFF829E), fontSize = 11.sp)
                    Text(dinero(deuda), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun CompromisoProximoRow(evento: EventoCalendario, periodicidad: PeriodicidadPresupuesto) {
    val paleta = LocalNexoPalette.current
    val fecha = runCatching {
        LocalDate.parse(evento.fecha).format(DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("es-MX")))
    }.getOrDefault(evento.fecha)
    Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(15.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(evento.concepto, color = Color.White, fontWeight = FontWeight.SemiBold)
                    Text("Pago · $fecha · ${periodicidadTexto(periodicidad)}", color = Color(0xFFB8C2D1), fontSize = 12.sp)
            }
            Text(dinero(evento.monto), color = Color(0xFFFF7899), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ItemPresupuestoCard(item: ItemPresupuesto, onEliminar: () -> Unit, onEditarSaldo: () -> Unit) {
    val paleta = LocalNexoPalette.current
    Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.concepto, color = Color.White, fontWeight = FontWeight.Bold)
                    val tipo = if (item.flujo == FlujoPresupuesto.INGRESO) "Ingreso" else if (item.esencial) "Esencial" else "Compromiso"
                    Text("$tipo · ${periodicidadTexto(item.periodicidad)} · día ${item.diaPago}${item.diaCorte?.let { " · corte $it" } ?: ""}${item.mesesPlazo?.let { " · $it meses" } ?: ""}", color = Color(0xFFB8C2D1), fontSize = 12.sp)
                }
                Text(dinero(item.montoMensual), color = if (item.flujo == FlujoPresupuesto.INGRESO) Color(0xFF7AD68A) else Color(0xFFFF7A9E), fontWeight = FontWeight.Bold)
            }
            item.saldoActual?.let { actual ->
                Text("Saldo: ${dinero(actual)} / inicial ${dinero(item.saldoInicial ?: 0.0)}", color = Color(0xFFB8C2D1), fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = onEditarSaldo) { Text("Actualizar saldo pendiente") }
            }
            TextButton(onClick = onEliminar, modifier = Modifier.align(Alignment.End)) { Text("Quitar", color = Color(0xFFFF8A8A)) }
        }
    }
}

private fun dinero(valor: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(valor)

private fun periodicidadTexto(periodicidad: PeriodicidadPresupuesto): String = when (periodicidad) {
    PeriodicidadPresupuesto.DIARIO -> "Diario"
    PeriodicidadPresupuesto.SEMANAL -> "Semanal"
    PeriodicidadPresupuesto.QUINCENAL -> "Quincenal"
    PeriodicidadPresupuesto.MENSUAL -> "Mensual"
    PeriodicidadPresupuesto.VARIABLE -> "Variable · promedio"
}

private fun montoEtiqueta(periodicidad: PeriodicidadPresupuesto, ingreso: Boolean): String {
    val concepto = if (ingreso) "Ingreso" else "Gasto"
    return when (periodicidad) {
        PeriodicidadPresupuesto.DIARIO -> "$concepto por día"
        PeriodicidadPresupuesto.SEMANAL -> "$concepto por semana"
        PeriodicidadPresupuesto.QUINCENAL -> "$concepto por quincena"
        PeriodicidadPresupuesto.MENSUAL -> "$concepto por mes"
        PeriodicidadPresupuesto.VARIABLE -> "$concepto variable"
    }
}

private fun tituloGrupo(grupo: GrupoPresupuesto) = when (grupo) {
    GrupoPresupuesto.INGRESO -> "INGRESOS"
    GrupoPresupuesto.BASICO -> "NECESIDADES Y HOGAR"
    GrupoPresupuesto.DEUDA -> "DEUDAS Y CRÉDITOS"
    GrupoPresupuesto.SUSCRIPCION -> "SUSCRIPCIONES"
    GrupoPresupuesto.TANDA -> "TANDAS"
    GrupoPresupuesto.DIFERIDO -> "COMPRAS DIFERIDAS"
    GrupoPresupuesto.OTRO -> "OTROS COMPROMISOS"
}
