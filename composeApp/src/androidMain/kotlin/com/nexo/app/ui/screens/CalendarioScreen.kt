package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexo.app.model.EventoCalendario
import com.nexo.app.model.TipoEventoCalendario
import com.nexo.app.ui.viewModel.CalendarioViewModel
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.LocalNexoPalette
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarioScreen(navController: NavController, viewModel: CalendarioViewModel) {
    val paleta = LocalNexoPalette.current
    val fondoCalendario = paleta.background
    val tarjetaCalendario = paleta.surface
    val acentoCalendario = paleta.accent
    val items by viewModel.items.collectAsState()
    val mes = viewModel.mesVisible
    val seleccionado = viewModel.fechaSeleccionada
    val eventosSeleccionados = viewModel.eventosDelDia(seleccionado)
    val diasConEventos = viewModel.eventosDelMes().map { it.fecha }.toSet()
    val idioma = Locale.forLanguageTag("es-MX")
    val mesLabel = mes.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idioma))
        .replaceFirstChar { it.titlecase(idioma) }
    val hoyLabel = LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", idioma))
        .replaceFirstChar { it.titlecase(idioma) }
    val offset = mes.atDay(1).dayOfWeek.value - 1
    val dias = List(offset) { null } + (1..mes.lengthOfMonth()).map { it }
    val semanas = dias.chunked(7).map { semana -> semana + List(7 - semana.size) { null } }

    Scaffold(containerColor = fondoCalendario, bottomBar = { MyBottomBar(navController) }) { padding ->
        NexoPullToRefresh(
            onRefresh = { viewModel.refrescar() },
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            Text("Calendario", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("Pagos e ingresos de tu presupuesto", color = Color(0xFFB8C2D1), fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            Spacer(Modifier.height(18.dp))

            Card(colors = CardDefaults.cardColors(containerColor = tarjetaCalendario), shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = viewModel::mesAnterior) { Text("‹", color = Color.White, fontSize = 30.sp) }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(mesLabel, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(hoyLabel, color = acentoCalendario, fontSize = 12.sp)
                        }
                        IconButton(onClick = viewModel::mesSiguiente) { Text("›", color = Color.White, fontSize = 30.sp) }
                    }
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp)) {
                        listOf("L", "M", "M", "J", "V", "S", "D").forEach { dia ->
                            Text(dia, modifier = Modifier.weight(1f), color = Color(0xFF8995A7), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        }
                    }
                    semanas.forEach { semana ->
                        Row(Modifier.fillMaxWidth()) {
                            semana.forEach { dia ->
                                if (dia == null) {
                                    Box(Modifier.weight(1f).height(47.dp))
                                } else {
                                    val fecha = mes.atDay(dia)
                                    val esSeleccionado = fecha == seleccionado
                                    val esHoy = fecha == LocalDate.now()
                                    val tieneEvento = fecha.toString() in diasConEventos
                                    Column(
                                        Modifier.weight(1f).height(47.dp).clickable { viewModel.seleccionarFecha(fecha) },
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            Modifier.size(32.dp).background(
                                                when {
                                                    esSeleccionado -> acentoCalendario
                                                    esHoy -> acentoCalendario.copy(alpha = .2f)
                                                    else -> Color.Transparent
                                                }, CircleShape
                                            ), contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                dia.toString(),
                                                color = if (esSeleccionado) Color(0xFF111A27) else Color.White,
                                                fontWeight = if (esHoy || esSeleccionado) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Box(Modifier.padding(top = 1.dp).size(4.dp).background(if (tieneEvento) Color(0xFFC837AB) else Color.Transparent, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                    Text("• Ingreso     • Pago o corte", color = Color(0xFFB8C2D1), fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    seleccionado.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", idioma)).replaceFirstChar { it.titlecase(idioma) },
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                if (seleccionado != LocalDate.now()) {
                    Text("Hoy", color = acentoCalendario, modifier = Modifier.clickable { viewModel.irAHoy() }.padding(8.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            if (eventosSeleccionados.isEmpty()) {
                Card(colors = CardDefaults.cardColors(containerColor = tarjetaCalendario), shape = RoundedCornerShape(18.dp)) {
                    Text(
                        if (items.isEmpty()) "Agrega tus ingresos y compromisos desde Presupuesto para verlos aquí."
                        else "No tienes pagos ni ingresos programados para este día.",
                        color = Color(0xFFB8C2D1), modifier = Modifier.fillMaxWidth().padding(18.dp)
                    )
                }
            } else {
                eventosSeleccionados.forEach { EventoCard(it) }
            }

            Spacer(Modifier.height(20.dp))
            Text("Próximos", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(9.dp))
            val hoy = LocalDate.now()
            val mesActual = YearMonth.from(hoy)
            val porMes = viewModel.proximosEventos(desde = hoy)
                .filter { LocalDate.parse(it.fecha) >= hoy }
                .filter { YearMonth.from(LocalDate.parse(it.fecha)) <= mesActual.plusMonths(1) }
                .groupBy { YearMonth.from(LocalDate.parse(it.fecha)) }
            if (porMes.values.all { it.isEmpty() }) {
                Text("Tus próximos pagos e ingresos aparecerán aquí al completar el presupuesto.", color = Color(0xFFB8C2D1), fontSize = 13.sp)
            } else {
                listOf(mesActual, mesActual.plusMonths(1)).forEachIndexed { index, mesSeccion ->
                    val eventosMes = porMes[mesSeccion].orEmpty().sortedBy { it.fecha }
                    Text(
                        if (index == 0) "Mes actual · ${mesSeccion.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idioma)).replaceFirstChar { it.titlecase(idioma) }}"
                        else "Mes siguiente · ${mesSeccion.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idioma)).replaceFirstChar { it.titlecase(idioma) }}",
                        color = acentoCalendario,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                    if (eventosMes.isEmpty()) {
                        Text("Sin pagos ni ingresos programados.", color = Color(0xFF8995A7), fontSize = 12.sp, modifier = Modifier.padding(vertical = 5.dp))
                    } else eventosMes.forEach { evento ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(9.dp).background(colorEvento(evento.tipo), CircleShape))
                            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                                Text(evento.concepto, color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text(LocalDate.parse(evento.fecha).format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", idioma)), color = Color(0xFFB8C2D1), fontSize = 12.sp)
                            }
                            if (evento.monto > 0) Text(
                                (if (evento.tipo == TipoEventoCalendario.INGRESO) "+" else "−") + dineroCalendario(evento.monto),
                                color = colorEvento(evento.tipo), fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        }
    }
}

@Composable
private fun EventoCard(evento: EventoCalendario) {
    val tarjetaCalendario = LocalNexoPalette.current.surface
    Card(
        colors = CardDefaults.cardColors(containerColor = tarjetaCalendario),
        shape = RoundedCornerShape(17.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(colorEvento(evento.tipo), CircleShape))
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(evento.concepto, color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    when (evento.tipo) {
                        TipoEventoCalendario.INGRESO -> "Ingreso esperado"
                        TipoEventoCalendario.PAGO -> if (evento.esencial) "Pago esencial" else "Pago programado"
                        TipoEventoCalendario.CORTE -> "Fecha de corte"
                    }, color = Color(0xFFB8C2D1), fontSize = 12.sp
                )
            }
            if (evento.monto > 0) Text(
                (if (evento.tipo == TipoEventoCalendario.INGRESO) "+" else "−") + dineroCalendario(evento.monto),
                color = colorEvento(evento.tipo), fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun colorEvento(tipo: TipoEventoCalendario) = when (tipo) {
    TipoEventoCalendario.INGRESO -> Color(0xFF71D58B)
    TipoEventoCalendario.PAGO -> Color(0xFFFF7599)
    TipoEventoCalendario.CORTE -> Color(0xFFB985E8)
}

private fun dineroCalendario(valor: Double): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(valor)
