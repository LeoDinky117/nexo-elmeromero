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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
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
import com.nexo.app.model.VisualConceptoCalendario
import com.nexo.app.ui.viewModel.PresupuestoViewModel
import com.nexo.app.ui.viewModel.MovimientosViewModel
import com.nexo.app.navigation.Routes
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.readableForeground
import nexo.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneOffset
import kotlinx.coroutines.launch

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
    var mostrarFormulario by remember { mutableStateOf(false) }
    var seccionRegistro by remember { mutableStateOf(SeccionRegistroPresupuesto.ESENCIALES) }
    var seccionExpandida by remember { mutableStateOf<SeccionRegistroPresupuesto?>(null) }
    var monto by remember { mutableStateOf("") }
    var dia by remember { mutableStateOf("") }
    var fechaPagoSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var corte by remember { mutableStateOf("") }
    var fechaCorteSeleccionada by remember { mutableStateOf(LocalDate.now()) }
    var saldo by remember { mutableStateOf("") }
    var plazo by remember { mutableStateOf("") }
    var periodicidad by remember { mutableStateOf(PeriodicidadPresupuesto.MENSUAL) }
    var mostrarPeriodicidades by remember { mutableStateOf(false) }
    var editarSaldo by remember { mutableStateOf<ItemPresupuesto?>(null) }
    var itemEditando by remember { mutableStateOf<ItemPresupuesto?>(null) }
    var saldoEditado by remember { mutableStateOf("") }
    var saldoEditadoError by remember { mutableStateOf<String?>(null) }
    var guardandoPagoDeuda by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var montoError by remember { mutableStateOf<String?>(null) }
    var diaError by remember { mutableStateOf<String?>(null) }
    var corteError by remember { mutableStateOf<String?>(null) }
    var saldoError by remember { mutableStateOf<String?>(null) }
    var plazoError by remember { mutableStateOf<String?>(null) }
    val mensaje = viewModel.mensaje

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
            Text("Mi presupuesto", color = paleta.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Una vista clara de tu saldo y tus próximos compromisos.", color = paleta.secondaryOnBackground, fontSize = 14.sp)

            SaldoRegistradoCard(resumen.saldoCuenta)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ResumenCompactoCard(
                    titulo = "Compromisos próximos",
                    monto = totalCompromisosProximos,
                    modifier = Modifier.weight(1f),
                    mostrarBoton = true,
                    nota = "Suma en 30 días",
                    onClick = { navController.navigate(Routes.CALENDARIO) }
                )
                ResumenCompactoCard(
                    titulo = "Saldo después de compromisos",
                    monto = resumen.saldoCuenta - totalCompromisosProximos,
                    modifier = Modifier.weight(1f),
                    colorPositivo = Color(0xFF55D6A2),
                    nota = "Saldo actual menos pagos"
                )
            }

            BalanceSaldoDeudaCard(saldo = resumen.saldoCuenta, deuda = resumen.deudaActual)

            Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Necesidades básicas al mes", color = paleta.onSurface, fontWeight = FontWeight.SemiBold)
                        Text("Se calcula con los gastos que marques como esenciales.", color = paleta.secondaryOnSurface, fontSize = 12.sp)
                    }
                    Text(dinero(resumen.necesidadesBasicas), color = paleta.accent, fontWeight = FontWeight.Bold)
                }
            }

            Text("Registro de presupuesto", color = paleta.onBackground, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.objetivoGuia("budget_title"))
            Text("Organiza ingresos y gastos por tipo. Toca una sección para ver su contenido.", color = paleta.secondaryOnBackground, fontSize = 13.sp)
            val esenciales = items.filter { (it.esencial && it.grupo !in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO)) || it.flujo == FlujoPresupuesto.INGRESO }
            val conceptosEsencialesPendientes = CatalogoConceptosPresupuesto.conceptos.filter { concepto ->
                concepto.flujo == FlujoPresupuesto.EGRESO && concepto.esencial && concepto.grupo !in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO) &&
                    esenciales.none { it.concepto.equals(concepto.nombre, ignoreCase = true) }
            }
            val deudas = items.filter { it.grupo == GrupoPresupuesto.DEUDA || it.grupo == GrupoPresupuesto.DIFERIDO }
            val otros = items.filter { it.flujo == FlujoPresupuesto.EGRESO && !it.esencial && it.grupo !in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO) }
            val prepararEdicion: (ItemPresupuesto) -> Unit = { item ->
                itemEditando = item
                conceptoSeleccionado = CatalogoConceptosPresupuesto.conceptos.firstOrNull { it.nombre == item.concepto }
                    ?: ConceptoPresupuesto(item.concepto, item.grupo, item.flujo, item.esencial, item.diaCorte != null, item.saldoActual != null, item.mesesPlazo != null)
                periodicidad = item.periodicidad
                monto = montoCapturadoParaEditar(item)
                fechaPagoSeleccionada = LocalDate.now().withDayOfMonth(item.diaPago.coerceAtMost(LocalDate.now().lengthOfMonth()))
                dia = item.diaPago.toString()
                fechaCorteSeleccionada = item.diaCorte?.let { LocalDate.now().withDayOfMonth(it.coerceAtMost(LocalDate.now().lengthOfMonth())) } ?: LocalDate.now()
                corte = item.diaCorte?.toString().orEmpty()
                saldo = item.saldoActual?.toString().orEmpty()
                plazo = item.mesesPlazo?.toString().orEmpty()
                mostrarFormulario = true
            }
            SeccionRegistroCard(
                titulo = "Gastos esenciales", subtitulo = "Pagos básicos e ingresos previstos",
                items = esenciales, pendientes = conceptosEsencialesPendientes, expandida = seccionExpandida == SeccionRegistroPresupuesto.ESENCIALES,
                onToggle = { seccionExpandida = if (seccionExpandida == SeccionRegistroPresupuesto.ESENCIALES) null else SeccionRegistroPresupuesto.ESENCIALES },
                onAgregar = { itemEditando = null; seccionRegistro = SeccionRegistroPresupuesto.ESENCIALES; conceptoSeleccionado = null; mostrarConceptos = true },
                onEliminar = { viewModel.eliminar(it.id) },
                onEditar = prepararEdicion,
                onEditarSaldo = { editarSaldo = it; saldoEditado = (it.saldoActual ?: 0.0).toString() },
                onAgregarPendiente = { concepto ->
                    itemEditando = null
                    conceptoSeleccionado = concepto
                    periodicidad = PeriodicidadPresupuesto.MENSUAL
                    monto = ""; dia = LocalDate.now().dayOfMonth.toString(); corte = ""; saldo = ""; plazo = ""
                    fechaPagoSeleccionada = LocalDate.now(); fechaCorteSeleccionada = LocalDate.now()
                    mostrarFormulario = true
                }
            )
            SeccionRegistroCard(
                titulo = "Deudas", subtitulo = "Tarjetas, préstamos y compras a meses",
                items = deudas, expandida = seccionExpandida == SeccionRegistroPresupuesto.DEUDAS,
                onToggle = { seccionExpandida = if (seccionExpandida == SeccionRegistroPresupuesto.DEUDAS) null else SeccionRegistroPresupuesto.DEUDAS },
                onAgregar = { itemEditando = null; seccionRegistro = SeccionRegistroPresupuesto.DEUDAS; conceptoSeleccionado = null; mostrarConceptos = true },
                onEliminar = { viewModel.eliminar(it.id) },
                onEditar = prepararEdicion,
                onEditarSaldo = { editarSaldo = it; saldoEditado = (it.saldoActual ?: 0.0).toString() }
            )
            SeccionRegistroCard(
                titulo = "Otros gastos", subtitulo = "Suscripciones, tandas y gastos flexibles",
                items = otros, expandida = seccionExpandida == SeccionRegistroPresupuesto.OTROS,
                onToggle = { seccionExpandida = if (seccionExpandida == SeccionRegistroPresupuesto.OTROS) null else SeccionRegistroPresupuesto.OTROS },
                onAgregar = { itemEditando = null; seccionRegistro = SeccionRegistroPresupuesto.OTROS; conceptoSeleccionado = null; mostrarConceptos = true },
                onEliminar = { viewModel.eliminar(it.id) },
                onEditar = prepararEdicion,
                onEditarSaldo = { editarSaldo = it; saldoEditado = (it.saldoActual ?: 0.0).toString() }
            )

            if (conceptoSeleccionado != null && mostrarFormulario) {
                val concepto = conceptoSeleccionado!!
                AlertDialog(
                    onDismissRequest = { mostrarFormulario = false; conceptoSeleccionado = null; itemEditando = null },
                    title = { Text(if (itemEditando == null) "Agregar ${concepto.nombre}" else "Editar ${concepto.nombre}") },
                    containerColor = paleta.surface,
                    titleContentColor = Color.White,
                    textContentColor = Color(0xFFB8C2D1),
                    text = {
                    Column(Modifier.heightIn(max = 500.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            periodicidadesPara(concepto).forEach { opcion ->
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
                SelectorFechaPresupuesto(
                    etiqueta = if (concepto.flujo == FlujoPresupuesto.INGRESO) "Fecha esperada de ingreso" else "Fecha de pago",
                    fecha = fechaPagoSeleccionada,
                    onFechaSeleccionada = { fechaPagoSeleccionada = it; dia = it.dayOfMonth.toString(); diaError = null }
                )
                if (concepto.tieneFechaCorte) {
                    SelectorFechaPresupuesto("Fecha de corte", fechaCorteSeleccionada) {
                        fechaCorteSeleccionada = it; corte = it.dayOfMonth.toString(); corteError = null
                    }
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
                    }
                    },
                    confirmButton = { Button(
                    onClick = {
                        viewModel.agregar(concepto, monto, dia, corte, saldo, plazo, periodicidad, movimientosViewModel.listaDeMovimientos.toList(), itemEditando)
                        if (viewModel.mensaje?.startsWith("Se agregó") == true || viewModel.mensaje?.startsWith("Se actualizó") == true) {
                            conceptoSeleccionado = null; itemEditando = null; mostrarFormulario = false; monto = ""; dia = ""; corte = ""; saldo = ""; plazo = ""
                            periodicidad = PeriodicidadPresupuesto.MENSUAL
                            montoError = null; diaError = null; corteError = null; saldoError = null; plazoError = null
                        }
                    },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = paleta.accent)
                ) { Text(if (itemEditando == null) "Agregar" else "Guardar cambios", color = Color(0xFF111A27), fontWeight = FontWeight.Bold) } },
                    dismissButton = { TextButton(onClick = { mostrarFormulario = false; conceptoSeleccionado = null; itemEditando = null }) { Text("Cancelar") } }
                )
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
                val conceptosVisibles = CatalogoConceptosPresupuesto.conceptos.filter { concepto ->
                    when (seccionRegistro) {
                        SeccionRegistroPresupuesto.ESENCIALES -> concepto.grupo in setOf(GrupoPresupuesto.INGRESO, GrupoPresupuesto.BASICO) || (concepto.esencial && concepto.grupo !in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO))
                        SeccionRegistroPresupuesto.DEUDAS -> concepto.grupo in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO)
                        SeccionRegistroPresupuesto.OTROS -> concepto.flujo == FlujoPresupuesto.EGRESO && !concepto.esencial && concepto.grupo !in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO)
                    }
                }
                GrupoPresupuesto.entries.forEach { grupo ->
                    val opciones = conceptosVisibles.filter { it.grupo == grupo }
                    if (opciones.isNotEmpty()) {
                        Text(tituloGrupo(grupo), color = paleta.accent, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                        opciones.forEach { concepto ->
                            Row(
                                Modifier.fillMaxWidth().clickable {
                                    itemEditando = null
                                    conceptoSeleccionado = concepto
                                    mostrarFormulario = true
                                    monto = ""
                                    fechaPagoSeleccionada = LocalDate.now()
                                    dia = fechaPagoSeleccionada.dayOfMonth.toString()
                                    corte = ""
                                    fechaCorteSeleccionada = LocalDate.now()
                                    corte = fechaCorteSeleccionada.dayOfMonth.toString()
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
            title = { Text("Registrar pago de ${item.concepto}") },
            containerColor = LocalNexoPalette.current.surface,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB8C2D1),
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = saldoEditado,
                        onValueChange = { value ->
                            if (value.matches(Regex("\\d*([.,]\\d{0,2})?"))) {
                                saldoEditado = value
                                saldoEditadoError = null
                            } else saldoEditadoError = "Ingresa solo números y hasta 2 decimales."
                        },
                        label = { Text("Nuevo saldo pendiente") }, prefix = { Text("$ ") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = saldoEditadoError != null,
                        supportingText = saldoEditadoError?.let { error -> ({ Text(error) }) },
                        colors = presupuestoFieldColors()
                    )
                    val nuevoSaldo = saldoEditado.replace(',', '.').toDoubleOrNull()
                    val montoPago = nuevoSaldo?.let { ((item.saldoActual ?: 0.0) - it).coerceAtLeast(0.0) } ?: 0.0
                    if (montoPago > 0.0) {
                        Text("Se registrará un pago de ${dinero(montoPago)} y se descontará del saldo disponible.", color = LocalNexoPalette.current.highlight, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(enabled = !guardandoPagoDeuda, onClick = {
                    val nuevoSaldo = saldoEditado.replace(',', '.').toDoubleOrNull()
                    if (nuevoSaldo == null || nuevoSaldo < 0.0) {
                        saldoEditadoError = "Ingresa un saldo válido de cero o más."
                    } else {
                        val pago = ((item.saldoActual ?: 0.0) - nuevoSaldo).coerceAtLeast(0.0)
                        if (pago > 0.0) {
                            guardandoPagoDeuda = true
                            scope.launch {
                                val registrado = movimientosViewModel.registrarMovimientoPresupuesto("Pago de ${item.concepto}", pago)
                                if (registrado) {
                                    viewModel.actualizarSaldoDeuda(item.id, nuevoSaldo.toString())
                                    editarSaldo = null
                                } else saldoEditadoError = "No se pudo registrar el pago. No se actualizó la deuda."
                                guardandoPagoDeuda = false
                            }
                        } else {
                            viewModel.actualizarSaldoDeuda(item.id, nuevoSaldo.toString())
                            editarSaldo = null
                        }
                    }
                }) { Text(if (guardandoPagoDeuda) "Guardando…" else "Guardar") }
            },
            dismissButton = { TextButton(enabled = !guardandoPagoDeuda, onClick = { editarSaldo = null }) { Text("Cancelar") } }
        )
    }

    if (mensaje != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = viewModel::limpiarMensaje,
            containerColor = LocalNexoPalette.current.surface,
            titleContentColor = Color.White,
            textContentColor = Color(0xFFB8C2D1),
            text = { Text(mensaje) },
            confirmButton = { TextButton(onClick = viewModel::limpiarMensaje) { Text("Entendido") } }
        )
    }
}

private enum class SeccionRegistroPresupuesto { ESENCIALES, DEUDAS, OTROS }

private fun periodicidadesPara(concepto: ConceptoPresupuesto): List<PeriodicidadPresupuesto> =
    if (concepto.grupo in setOf(GrupoPresupuesto.DEUDA, GrupoPresupuesto.DIFERIDO)) {
        listOf(PeriodicidadPresupuesto.DIARIO, PeriodicidadPresupuesto.SEMANAL, PeriodicidadPresupuesto.QUINCENAL, PeriodicidadPresupuesto.MENSUAL)
    } else PeriodicidadPresupuesto.entries.toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorFechaPresupuesto(
    etiqueta: String,
    fecha: LocalDate,
    onFechaSeleccionada: (LocalDate) -> Unit
) {
    val palette = LocalNexoPalette.current
    var mostrarCalendario by remember { mutableStateOf(false) }
    val picker = rememberDatePickerState(initialSelectedDateMillis = fecha.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    OutlinedTextField(
        value = fecha.format(DateTimeFormatter.ofPattern("d 'de' MMMM yyyy", Locale.forLanguageTag("es-MX"))),
        onValueChange = {}, readOnly = true, modifier = Modifier.fillMaxWidth(),
        label = { Text(etiqueta) }, trailingIcon = {
            IconButton(onClick = { mostrarCalendario = true }) {
                Icon(
                    painter = painterResource(Res.drawable.icon_calendar),
                    contentDescription = "Abrir calendario",
                    tint = palette.accent
                )
            }
        },
        colors = presupuestoFieldColors()
    )
    if (mostrarCalendario) {
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { millis -> onFechaSeleccionada(LocalDate.ofEpochDay(millis / 86_400_000L)) }
                    mostrarCalendario = false
                }) { Text("Seleccionar") }
            },
            dismissButton = { TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") } }
        ) { DatePicker(state = picker) }
    }
}

@Composable
private fun SeccionRegistroCard(
    titulo: String,
    subtitulo: String,
    items: List<ItemPresupuesto>,
    pendientes: List<ConceptoPresupuesto> = emptyList(),
    expandida: Boolean,
    onToggle: () -> Unit,
    onAgregar: () -> Unit,
    onEliminar: (ItemPresupuesto) -> Unit,
    onEditar: (ItemPresupuesto) -> Unit,
    onEditarSaldo: (ItemPresupuesto) -> Unit,
    onAgregarPendiente: (ConceptoPresupuesto) -> Unit = {}
) {
    val palette = LocalNexoPalette.current
    val total = items.sumOf { if (it.flujo == FlujoPresupuesto.INGRESO) it.montoMensual else -it.montoMensual }
    Card(colors = CardDefaults.cardColors(containerColor = palette.surface), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onToggle), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(titulo, color = palette.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("$subtitulo · ${items.size} registrados${if (pendientes.isNotEmpty()) " · ${pendientes.size} por configurar" else ""}", color = palette.secondaryOnSurface, fontSize = 12.sp)
                }
                Text(dinero(total), color = palette.accentTextOnSurface, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Box(
                    Modifier.padding(start = 8.dp).size(34.dp).background(palette.accent.copy(alpha = 0.18f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (expandida) "⌃" else "⌄", color = palette.highlightTextOnSurface, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (expandida) {
                if (items.isEmpty() && pendientes.isEmpty()) Text("Todavía no hay registros en esta sección.", color = palette.secondaryOnSurface, fontSize = 13.sp)
                items.forEach { item ->
                    ItemPresupuestoCard(item, onEliminar = { onEliminar(item) }, onEditar = { onEditar(item) }, onEditarSaldo = { onEditarSaldo(item) })
                }
                pendientes.forEach { concepto ->
                    Row(
                        Modifier.fillMaxWidth().background(palette.background, RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(VisualConceptoCalendario.icono(concepto.nombre), fontSize = 22.sp)
                        Column(Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(concepto.nombre, color = palette.onBackground, fontWeight = FontWeight.SemiBold)
                            Text("Esencial · sin configurar", color = palette.secondaryOnBackground, fontSize = 11.sp)
                        }
                        TextButton(onClick = { onAgregarPendiente(concepto) }) { Text("Configurar +", color = palette.highlightTextOnSurface) }
                    }
                }
                Button(
                    onClick = onAgregar,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accent)
                ) { Text("Agregar otro", color = palette.onAccent, fontWeight = FontWeight.Bold) }
            } else {
                TextButton(onClick = onAgregar, modifier = Modifier.align(Alignment.End)) { Text("Agregar +", color = palette.highlightTextOnSurface, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun presupuestoFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedContainerColor = LocalNexoPalette.current.surface,
    unfocusedContainerColor = LocalNexoPalette.current.surface,
    focusedTextColor = LocalNexoPalette.current.onSurface,
    unfocusedTextColor = LocalNexoPalette.current.onSurface,
    cursorColor = LocalNexoPalette.current.accent,
    focusedBorderColor = LocalNexoPalette.current.accent,
    unfocusedBorderColor = Color(0xFF526176),
    focusedLabelColor = LocalNexoPalette.current.accentTextOnSurface,
    unfocusedLabelColor = LocalNexoPalette.current.secondaryOnSurface,
    focusedPrefixColor = LocalNexoPalette.current.onSurface,
    unfocusedPrefixColor = LocalNexoPalette.current.secondaryOnSurface,
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text("Saldo registrado", color = paleta.secondaryOnSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = paleta.highlightTextOnSurface)) { append("$") }
                        withStyle(SpanStyle(color = paleta.onSurface)) {
                            append(NumberFormat.getNumberInstance(Locale.forLanguageTag("es-MX")).apply {
                                minimumFractionDigits = 2
                                maximumFractionDigits = 2
                            }.format(saldo))
                        }
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                Text("Dinero disponible en tu cuenta", color = paleta.secondaryOnSurface, fontSize = 12.sp)
            }
            Spacer(Modifier.size(16.dp))
            Box(
                Modifier.size(58.dp).background(paleta.accent.copy(alpha = 0.16f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$", color = paleta.highlightTextOnSurface, fontSize = 34.sp, fontWeight = FontWeight.Bold)
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
    colorPositivo: Color = Color.White,
    nota: String,
    onClick: () -> Unit = {}
) {
    val paleta = LocalNexoPalette.current
    Card(
        colors = CardDefaults.cardColors(containerColor = paleta.surface),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(titulo, color = paleta.secondaryOnSurface, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2)
            Text(
                dinero(monto),
                color = if (monto < 0) readableForeground(Color(0xFFFF526F), paleta.surface) else if (colorPositivo == Color.White) paleta.onSurface else readableForeground(colorPositivo, paleta.surface),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            if (mostrarBoton) {
                TextButton(onClick = onClick, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 0.dp, vertical = 0.dp)) {
                    Text("Ver detalle  ›", color = paleta.highlightTextOnSurface, fontSize = 12.sp)
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
            Text("Tu panorama · saldo vs. deuda", color = paleta.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text("Una vista rápida de lo que tienes registrado y tus deudas pendientes.", color = paleta.secondaryOnSurface, fontSize = 12.sp)
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
                    Text("Saldo registrado", color = readableForeground(Color(0xFF72DCAF), paleta.surface), fontSize = 11.sp)
                    Text(dinero(saldo), color = paleta.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Deuda pendiente", color = readableForeground(Color(0xFFFF829E), paleta.surface), fontSize = 11.sp)
                    Text(dinero(deuda), color = paleta.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                Text(evento.concepto, color = paleta.onSurface, fontWeight = FontWeight.SemiBold)
                    Text("Pago · $fecha · ${periodicidadTexto(periodicidad)}", color = paleta.secondaryOnSurface, fontSize = 12.sp)
            }
            Text(dinero(evento.monto), color = Color(0xFFFF7899), fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
private fun ItemPresupuestoCard(item: ItemPresupuesto, onEliminar: () -> Unit, onEditar: () -> Unit, onEditarSaldo: () -> Unit) {
    val paleta = LocalNexoPalette.current
    Card(colors = CardDefaults.cardColors(containerColor = paleta.surface), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(VisualConceptoCalendario.icono(item.concepto), fontSize = 20.sp)
                        Text(item.concepto, color = paleta.onSurface, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
                    }
                    val tipo = if (item.flujo == FlujoPresupuesto.INGRESO) "Ingreso" else if (item.esencial) "Esencial" else "Compromiso"
                    Text("$tipo · ${periodicidadTexto(item.periodicidad)} · día ${item.diaPago}${item.diaCorte?.let { " · corte $it" } ?: ""}${item.mesesPlazo?.let { " · $it meses" } ?: ""}", color = paleta.secondaryOnSurface, fontSize = 12.sp)
                }
                val colorMonto = if (item.flujo == FlujoPresupuesto.INGRESO) Color(0xFF7AD68A) else Color(0xFFFF7A9E)
                Text(dinero(item.montoMensual), color = readableForeground(colorMonto, paleta.surface), fontWeight = FontWeight.Bold)
            }
            item.saldoActual?.let { actual ->
                Text("Saldo: ${dinero(actual)} / inicial ${dinero(item.saldoInicial ?: 0.0)}", color = paleta.secondaryOnSurface, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                TextButton(onClick = onEditarSaldo) { Text("Registrar pago / ajustar saldo", color = paleta.accentTextOnSurface) }
            }
            Row(Modifier.align(Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEditar) { Text("Editar", color = paleta.highlightTextOnSurface) }
                androidx.compose.material3.IconButton(onClick = onEliminar) {
                    Text("🗑", color = Color(0xFFFF8A8A), fontSize = 20.sp)
                }
            }
        }
    }
}

private fun montoCapturadoParaEditar(item: ItemPresupuesto): String {
    val monto = when (item.periodicidad) {
        PeriodicidadPresupuesto.DIARIO -> item.montoMensual / YearMonth.now().lengthOfMonth()
        PeriodicidadPresupuesto.SEMANAL -> item.montoMensual / (52.0 / 12.0)
        PeriodicidadPresupuesto.QUINCENAL -> item.montoMensual / 2.0
        PeriodicidadPresupuesto.MENSUAL -> item.montoMensual
        PeriodicidadPresupuesto.VARIABLE -> return ""
    }
    return monto.toString()
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
