package com.nexo.app.widgets

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.MovimientoBorrador
import com.nexo.app.util.BorradoresNotificacion
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class CapturaMovimientoActivity : ComponentActivity() {
    private var userId by mutableStateOf(0)
    private var tipo by mutableStateOf("Gasto")
    private var monto by mutableStateOf("")
    private var concepto by mutableStateOf("")
    private var categoria by mutableStateOf("Otros")
    private var categoriaId by mutableStateOf(5)
    private var error by mutableStateOf<String?>(null)
    private var menuCategoria by mutableStateOf(false)
    private var transcripcion by mutableStateOf("")

    private val categorias = listOf(
        WidgetCategory("Trabajo", "Ingreso", 1), WidgetCategory("Comida", "Gasto", 2),
        WidgetCategory("Ocio", "Gasto", 3), WidgetCategory("Trans.", "Gasto", 4),
        WidgetCategory("Otros", "Gasto", 5), WidgetCategory("Ventas", "Ingreso", 6),
        WidgetCategory("Freelance", "Ingreso", 7), WidgetCategory("Inversiones", "Ingreso", 8),
        WidgetCategory("Regalo", "Ingreso", 9), WidgetCategory("Otros ingresos", "Ingreso", 10),
        WidgetCategory("Hogar", "Gasto", 11), WidgetCategory("Servicios", "Gasto", 12),
        WidgetCategory("Salud", "Gasto", 13), WidgetCategory("Educación", "Gasto", 14),
        WidgetCategory("Compras", "Gasto", 15), WidgetCategory("Suscripciones", "Gasto", 16),
        WidgetCategory("Deudas", "Gasto", 17), WidgetCategory("Viajes", "Gasto", 18),
        WidgetCategory("Mascotas", "Gasto", 19), WidgetCategory("Productos", "Gasto", 20),
        WidgetCategory("Rendimientos", "Ingreso", 21), WidgetCategory("Intereses", "Ingreso", 22)
    )

    private val voiceLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val texto = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!texto.isNullOrBlank()) aplicarDictado(texto)
        else error = "No se reconoció el dictado. Puedes escribirlo manualmente."
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            userId = runCatching { SessionManager(this@CapturaMovimientoActivity).getUserId() }.getOrDefault(0)
            if (userId <= 0) {
                error = "Abre NEXO e inicia sesión para vincular tus movimientos con tu cuenta."
            } else if (intent.getBooleanExtra(EXTRA_DICTAR, false)) {
                window.decorView.post { iniciarDictado() }
            }
        }
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize(), color = Color(0xFF0D1726)) {
                    Column(
                        Modifier.fillMaxSize().padding(22.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Movimiento rápido", color = Color.White, fontSize = 23.sp)
                        Text("Se guardará como borrador y no afectará tu saldo hasta que lo confirmes.", color = Color(0xFFB8C3D1))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Gasto", "Ingreso", "Pago").forEach { opcion ->
                                val seleccionado = if (opcion == "Pago") tipo == "Pago" else tipo == opcion
                                TextButton(
                                    onClick = { seleccionarTipo(opcion) },
                                    colors = ButtonDefaults.textButtonColors(
                                        containerColor = if (seleccionado) Color(0xFFFF9F2E) else Color(0xFF1C293A),
                                        contentColor = if (seleccionado) Color(0xFF111A27) else Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) { Text(opcion) }
                            }
                        }
                        OutlinedTextField(
                            value = monto,
                            onValueChange = { valor -> if (valor.matches(Regex("\\d*(?:[.,]\\d{0,2})?"))) monto = valor },
                            label = { Text("Monto") },
                            prefix = { Text("$ ") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFFFFB04A),
                                focusedContainerColor = Color(0xFF1C293A),
                                unfocusedContainerColor = Color(0xFF1C293A),
                                focusedBorderColor = Color(0xFFFFB04A),
                                unfocusedBorderColor = Color(0xFF526176),
                                focusedLabelColor = Color(0xFFFFB04A),
                                unfocusedLabelColor = Color(0xFFB8C3D1),
                                focusedPrefixColor = Color.White,
                                unfocusedPrefixColor = Color(0xFFB8C3D1)
                            )
                        )
                        OutlinedTextField(
                            value = concepto,
                            onValueChange = { concepto = it },
                            label = { Text("Concepto") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Color(0xFFFFB04A),
                                focusedContainerColor = Color(0xFF1C293A),
                                unfocusedContainerColor = Color(0xFF1C293A),
                                focusedBorderColor = Color(0xFFFFB04A),
                                unfocusedBorderColor = Color(0xFF526176),
                                focusedLabelColor = Color(0xFFFFB04A),
                                unfocusedLabelColor = Color(0xFFB8C3D1)
                            )
                        )
                        TextButton(onClick = { menuCategoria = true }) {
                            Text("Categoría: $categoria  ▾", color = Color.White)
                        }
                        DropdownMenu(
                            expanded = menuCategoria,
                            onDismissRequest = { menuCategoria = false },
                            containerColor = Color(0xFF1C293A)
                        ) {
                            categorias.filter { it.tipo == tipoReal() }.forEach { opcion ->
                                DropdownMenuItem(
                                    text = { Text(opcion.nombre, color = Color.White) },
                                    onClick = { categoria = opcion.nombre; categoriaId = opcion.id; menuCategoria = false }
                                )
                            }
                        }
                        TextButton(onClick = ::iniciarDictado, modifier = Modifier.fillMaxWidth()) {
                            Text("🎙  Dictar movimiento", color = Color(0xFFFFB04A))
                        }
                        error?.let { Text(it, color = Color(0xFFFF8A8A)) }
                        Spacer(Modifier.height(4.dp))
                        Button(
                            onClick = ::guardarBorrador,
                            enabled = userId > 0,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9F2E)),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("Guardar borrador", color = Color(0xFF111A27)) }
                        TextButton(onClick = ::finish, modifier = Modifier.fillMaxWidth()) {
                            Text("Cancelar", color = Color(0xFFB8C3D1))
                        }
                    }
                }
            }
        }
    }

    private fun seleccionarTipo(seleccion: String) {
        tipo = seleccion
        val sugerida = categoriaPorTexto(concepto, tipoReal())
            ?: categorias.first { it.tipo == tipoReal() }
        categoria = sugerida.nombre
        categoriaId = sugerida.id
    }

    private fun tipoReal() = if (tipo == "Ingreso") "Ingreso" else "Gasto"

    private fun iniciarDictado() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Di, por ejemplo: pagué 150 pesos de luz")
        }
        runCatching { voiceLauncher.launch(intent) }
            .onFailure { error = "No hay reconocimiento de voz disponible en este dispositivo." }
    }

    private fun aplicarDictado(texto: String) {
        transcripcion = texto
        val normalizado = texto.lowercase(Locale.forLanguageTag("es-MX"))
        tipo = when {
            Regex("\\b(ingreso|recib[ií]|dep[oó]sito|me pagaron|me transfirieron|sueldo|n[oó]mina|venta|vend[ií]|rendimientos?|intereses?)\\b").containsMatchIn(normalizado) -> "Ingreso"
            Regex("\\b(pago|pagu[eé]|pag[oó])\\b").containsMatchIn(normalizado) -> "Pago"
            else -> "Gasto"
        }
        val montoRegex = Regex("(?<![\\w])\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?|(?<![\\w])\\d+(?:[.,]\\d{1,2})?(?![\\w])")
        val montoTexto = montoRegex.find(normalizado)?.value
        monto = montoTexto?.let(::normalizarMonto).orEmpty()
        val encontrada = categoriaPorTexto(normalizado, tipoReal())
        if (encontrada != null) {
            categoria = encontrada.nombre
            categoriaId = encontrada.id
        } else {
            val default = categorias.first { it.tipo == tipoReal() }
            categoria = default.nombre
            categoriaId = default.id
        }
        concepto = texto
            .replace(montoRegex, " ")
            .replace(Regex("\\b(oye|anota|registra|agrega|guarda|ingreso|gasto|gast[eé]|compr[eé]|pago|pag[oó]|pagu[eé]|recib[ií]|dep[oó]sito|pesos?|mxn|d[oó]lares?|de|por|en)\\b", RegexOption.IGNORE_CASE), " ")
            .replace(Regex("\\s+"), " ").trim(' ', ',', '.', ';', ':')
        error = if (monto.isBlank() || concepto.isBlank()) "Revisa el monto y el concepto antes de guardar." else null
    }

    private fun categoriaPorTexto(texto: String, tipoEsperado: String): WidgetCategory? {
        val lower = texto.lowercase(Locale.forLanguageTag("es-MX"))
        val nombre = when {
            Regex("\\b(sueldo|n[oó]mina|trabajo)\\b").containsMatchIn(lower) -> "Trabajo"
            Regex("\\b(venta|vend[ií])\\b").containsMatchIn(lower) -> "Ventas"
            Regex("\\b(rendimiento|rendimientos)\\b").containsMatchIn(lower) -> "Rendimientos"
            Regex("\\b(inter[eé]s|intereses)\\b").containsMatchIn(lower) -> "Intereses"
            Regex("freelance|independiente").containsMatchIn(lower) -> "Freelance"
            Regex("inversi[oó]n|dividendo").containsMatchIn(lower) -> "Inversiones"
            Regex("\\b(regalo|obsequio)\\b").containsMatchIn(lower) -> "Regalo"
            Regex("comida|restaurante|desayuno|cena|almuerzo|caf[eé]").containsMatchIn(lower) -> "Comida"
            Regex("ocio|cine|juego|entretenimiento").containsMatchIn(lower) -> "Ocio"
            Regex("transporte|gasolina|taxi|uber|cam[ií]on").containsMatchIn(lower) -> "Trans."
            Regex("renta|hipoteca|hogar").containsMatchIn(lower) -> "Hogar"
            Regex("luz|agua|internet|tel[eé]fono|servicios").containsMatchIn(lower) -> "Servicios"
            Regex("\\b(consulta(s)?|m[eé]dic[oa]s?|doctor(a)?|hospital|cl[ií]nica|dentista|dental|terapia|farmacia|medicina|salud|an[aá]lisis cl[ií]nico(s)?)\\b").containsMatchIn(lower) -> "Salud"
            Regex("escuela|colegiatura|curso|libros|educaci[oó]n").containsMatchIn(lower) -> "Educación"
            Regex("ropa|compras|supermercado").containsMatchIn(lower) -> "Compras"
            Regex("\\b(productos?|bolsa|bolsas|art[ií]culo|art[ií]culos)\\b").containsMatchIn(lower) -> "Productos"
            Regex("netflix|spotify|suscripci[oó]n").containsMatchIn(lower) -> "Suscripciones"
            Regex("pr[eé]stamo|deuda|tarjeta de cr[eé]dito").containsMatchIn(lower) -> "Deudas"
            Regex("viaje|hotel|vacaciones").containsMatchIn(lower) -> "Viajes"
            Regex("mascota|veterinario|perro|gato").containsMatchIn(lower) -> "Mascotas"
            else -> null
        }
        return categorias.firstOrNull { it.nombre == nombre && it.tipo == tipoEsperado }
    }

    private fun normalizarMonto(valor: String): String {
        val punto = valor.lastIndexOf('.')
        val coma = valor.lastIndexOf(',')
        return when {
            punto >= 0 && coma >= 0 -> {
                val decimal = maxOf(punto, coma)
                valor.filterIndexed { i, c -> c.isDigit() || i == decimal }.replace(',', '.')
            }
            coma >= 0 && valor.length - coma - 1 in 1..2 -> valor.replace(".", "").replace(',', '.')
            coma >= 0 -> valor.replace(",", "")
            punto >= 0 && valor.length - punto - 1 == 3 -> valor.replace(".", "")
            else -> valor
        }
    }

    private fun guardarBorrador() {
        val montoNum = monto.replace(',', '.').toDoubleOrNull()
        if (userId <= 0 || montoNum == null || montoNum <= 0 || concepto.isBlank()) {
            error = "Escribe un monto mayor que cero y un concepto."
            return
        }
        val borrador = MovimientoBorrador(
            id = UUID.randomUUID().toString(),
            idUsuario = userId,
            idCategoria = categoriaId,
            monto = if (tipoReal() == "Ingreso") montoNum else -montoNum,
            fecha = LocalDate.now().toString(),
            descripcion = concepto.trim(),
            tipo = tipoReal(),
            transcripcion = transcripcion,
            creadoEn = System.currentTimeMillis()
        )
        val actuales = BorradoresNotificacion.cargarBorradores(this, userId)
        BorradoresNotificacion.guardarBorradores(this, userId, listOf(borrador) + actuales)
        BorradoresNotificacion.programarRecordatorio(this, userId)
        MovimientoWidgetProvider.actualizarTodos(this)
        finish()
    }

    companion object { const val EXTRA_DICTAR = "dictar_movimiento" }
}

private data class WidgetCategory(val nombre: String, val tipo: String, val id: Int)
