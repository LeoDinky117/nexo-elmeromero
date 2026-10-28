package com.nexo.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nexo.app.model.Movimiento
import com.nexo.app.ui.LocalNexoPalette
import com.nexo.app.ui.components.NexoPullToRefresh
import com.nexo.app.ui.viewModel.MovimientosViewModel
import nexo.composeapp.generated.resources.Res
import nexo.composeapp.generated.resources.icon_comida
import nexo.composeapp.generated.resources.icon_gastos
import nexo.composeapp.generated.resources.icon_ocio
import nexo.composeapp.generated.resources.icon_trabajo
import nexo.composeapp.generated.resources.icon_transporte
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

@Composable
fun GastosScreen(navController: NavController, movimientosVM: MovimientosViewModel) {
    val paleta = LocalNexoPalette.current
    val idioma = Locale.forLanguageTag("es-MX")
    val meses = remember(movimientosVM.listaDeMovimientos.toList()) {
        movimientosVM.listaDeMovimientos
            .mapNotNull { movimiento -> fechaMovimiento(movimiento.fecha)?.let { YearMonth.from(it) to movimiento } }
            .groupBy({ it.first }, { it.second })
            .toSortedMap(compareByDescending { it })
            .mapValues { (_, movimientos) -> movimientos.sortedByDescending { fechaMovimiento(it.fecha) } }
    }
    val sinFecha = remember(movimientosVM.listaDeMovimientos.toList()) {
        movimientosVM.listaDeMovimientos.filter { fechaMovimiento(it.fecha) == null }
    }
    val ingresos = movimientosVM.listaDeMovimientos.filter { it.monto > 0 }.sumOf { it.monto }
    val gastos = movimientosVM.listaDeMovimientos.filter { it.monto < 0 }.sumOf { abs(it.monto) }

    Scaffold(
        containerColor = paleta.background,
        bottomBar = { MyBottomBar(navController) }
    ) { paddingValues ->
        NexoPullToRefresh(
            onRefresh = { movimientosVM.refrescarDatos().join() },
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Column {
                        Text("Mis movimientos", color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.Bold)
                        Text("Ingresos y gastos organizados por mes", color = Color(0xFFABB6C6), fontSize = 14.sp)
                        Spacer(Modifier.height(18.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .background(paleta.surface, RoundedCornerShape(20.dp))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ResumenImporte("Ingresos", ingresos, Color(0xFF65CF83))
                            Box(Modifier.height(36.dp).width(1.dp).background(Color(0xFF354154)))
                            ResumenImporte("Gastos", gastos, paleta.highlight)
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }

                if (movimientosVM.listaDeMovimientos.isEmpty()) {
                    item {
                        Text(
                            "Todavía no hay movimientos. Cuando registres uno, aparecerá aquí.",
                            color = Color(0xFFABB6C6), modifier = Modifier.padding(vertical = 28.dp)
                        )
                    }
                } else {
                    meses.forEach { (mes, movimientos) ->
                        item(key = "month-$mes") {
                            Column(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(top = 12.dp, bottom = 2.dp)
                                    .background(paleta.surface.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 15.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    mes.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", idioma))
                                        .replaceFirstChar { it.titlecase(idioma) },
                                    color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold
                                )
                                val totalIngresos = movimientos.filter { it.monto > 0 }.sumOf { it.monto }
                                val totalGastos = movimientos.filter { it.monto < 0 }.sumOf { abs(it.monto) }
                                Text(
                                    "${movimientos.size} movimientos  ·  Entró ${dinero(totalIngresos)}  ·  Salió ${dinero(totalGastos)}",
                                    color = Color(0xFFB8C2D1), fontSize = 11.sp
                                )
                            }
                        }
                        items(movimientos, key = { "movement-${it.idMovimiento}" }) { movimiento ->
                            val categoria = movimientosVM.nombreCategoria(movimiento.idCategoria)
                            val gasto = movimiento.monto < 0
                            val fecha = fechaMovimiento(movimiento.fecha)?.format(
                                DateTimeFormatter.ofPattern("EEE d MMM", idioma)
                            )?.replaceFirstChar { it.titlecase(idioma) } ?: movimiento.fecha
                            ItemMovimiento(
                                titulo = movimiento.descripcion?.takeIf { it.isNotBlank() } ?: "Movimiento",
                                sub = categoria,
                                fecha = fecha,
                                monto = "${if (gasto) "−" else "+"}${dinero(abs(movimiento.monto))}",
                                colorMonto = if (gasto) paleta.highlight else Color(0xFF65CF83),
                                icono = when (movimiento.idCategoria) {
                                    1 -> Res.drawable.icon_trabajo
                                    2 -> Res.drawable.icon_comida
                                    3 -> Res.drawable.icon_ocio
                                    4 -> Res.drawable.icon_transporte
                                    else -> Res.drawable.icon_gastos
                                },
                                iconoEmoji = emojiMovimiento(categoria)
                            )
                        }
                    }
                    if (sinFecha.isNotEmpty()) {
                        item(key = "month-no-date") {
                            Text("Fecha no disponible", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                        }
                        items(sinFecha, key = { "movement-no-date-${it.idMovimiento}" }) { movimiento ->
                            val categoria = movimientosVM.nombreCategoria(movimiento.idCategoria)
                            val gasto = movimiento.monto < 0
                            ItemMovimiento(
                                titulo = movimiento.descripcion?.takeIf { it.isNotBlank() } ?: "Movimiento",
                                sub = categoria,
                                monto = "${if (gasto) "−" else "+"}${dinero(abs(movimiento.monto))}",
                                colorMonto = if (gasto) paleta.highlight else Color(0xFF65CF83),
                                icono = Res.drawable.icon_gastos,
                                iconoEmoji = emojiMovimiento(categoria),
                                fecha = movimiento.fecha
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenImporte(titulo: String, monto: Double, color: Color) {
    Column {
        Text(titulo, color = Color(0xFFB8C2D1), fontSize = 12.sp)
        Text(dinero(monto), color = color, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

private fun fechaMovimiento(valor: String): LocalDate? =
    runCatching { LocalDate.parse(valor.take(10)) }.getOrNull()

private fun dinero(valor: Double): String = "$" + String.format(Locale.forLanguageTag("es-MX"), "%,.2f", valor)

private fun emojiMovimiento(categoria: String): String? = when (categoria.lowercase(Locale.forLanguageTag("es-MX"))) {
    "trabajo" -> "💼"
    "comida" -> "🍽️"
    "ocio" -> "⭐"
    "trans.", "transporte" -> "🚘"
    "salud" -> "🩺"
    "educación" -> "📚"
    "productos", "compras" -> "🛍️"
    "hogar" -> "🏠"
    "servicios" -> "💡"
    "suscripciones" -> "📱"
    "deudas" -> "💳"
    "mascotas" -> "🐾"
    "viajes" -> "✈️"
    "ventas" -> "🧾"
    "rendimientos", "inversiones" -> "📈"
    "regalo" -> "🎁"
    else -> null
}
