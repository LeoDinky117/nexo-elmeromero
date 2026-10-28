package com.nexo.app.ui.viewModel

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.Movimiento
import com.nexo.app.model.MovimientoBorrador
import com.nexo.app.model.CategoriaFinanciera
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.BorradoresNotificacion
import com.nexo.app.widgets.MovimientoWidgetProvider
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

class MovimientosViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private var userIdReal : Int = 0
    var idCategoria by mutableStateOf(0)
    var categorias by mutableStateOf<List<CategoriaFinanciera>>(emptyList())
        private set
    var categoriaIdSeleccionada by mutableStateOf(0)
        private set
    var puntos by mutableStateOf(0)
        private set
    var racha by mutableStateOf(0)
        private set

    // Inputs de la interfaz y ESTADOS para la UI
    var descripcionInput by mutableStateOf("")
        private set
    var montoInput by mutableStateOf("")
        private set
    var montoDouble by mutableStateOf(0.0)
    var categoriaSeleccionada by mutableStateOf("")
        private set
    var esIngresoInput by mutableStateOf(false)
        private set
    var saldoTotal by mutableStateOf(0.0)
        private set
    var fechaInput by mutableStateOf(LocalDate.now().toString())
        private set

    // Estado de la UI --- lista observable
    val listaDeMovimientos = mutableStateListOf<Movimiento>()
    val borradoresPendientes = mutableStateListOf<MovimientoBorrador>()
    var guardandoBorradores by mutableStateOf(false)
        private set
    var mensajeBorradores by mutableStateOf<String?>(null)
        private set


    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    // Cliente Ktor (Igual al de tu Login)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    private val borradoresPreferences = sessionManager.applicationContext
        .getSharedPreferences("nexo_borradores", android.content.Context.MODE_PRIVATE)
    private val borradoresListener = SharedPreferences.OnSharedPreferenceChangeListener { _, clave ->
        val usuarioActual = userIdReal
        if (usuarioActual > 0 && clave == "usuario_$usuarioActual") {
            viewModelScope.launch {
                val guardados = BorradoresNotificacion.cargarBorradores(
                    sessionManager.applicationContext,
                    usuarioActual
                )
                if (borradoresPendientes.toList() != guardados) {
                    borradoresPendientes.clear()
                    borradoresPendientes.addAll(guardados)
                }
            }
        }
    }

    init {
        borradoresPreferences.registerOnSharedPreferenceChangeListener(borradoresListener)
        viewModelScope.launch {
            //Leemos el ID desde el SessionManager(DataSource)
            sessionManager.userIdFlow.collect { id ->
                if (id!= 0){
                    userIdReal = id
                    println("DEBUG: ID de usuario recuperado en Movimientos: $userIdReal")
                    cargarDatosDelServidor()
                    actualizarRachaPorConexion(id)
                    borradoresPendientes.clear()
                    borradoresPendientes.addAll(
                        BorradoresNotificacion.cargarBorradores(sessionManager.applicationContext, id)
                    )
                    if (borradoresPendientes.isNotEmpty()) {
                        BorradoresNotificacion.programarRecordatorio(sessionManager.applicationContext, id)
                    }

                }
            }
        }
        viewModelScope.launch {
            sessionManager.puntosFlow.collect { puntos = it }
        }
        viewModelScope.launch {
            sessionManager.rachaFlow.collect { racha = it }
        }
    }

    private fun actualizarRachaPorConexion(id: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val respuesta = client.post("${ApiConfig.BASE_URL}/actividad/usuario/$id/conexion")
                if (respuesta.status == HttpStatusCode.OK) {
                    val datos: com.nexo.app.model.RachaResponse = respuesta.body()
                    racha = datos.racha
                    sessionManager.saveStreak(racha)
                }
            } catch (e: Exception) {
                println("No se pudo actualizar la racha de conexión: ${e.message}")
            }
        }
    }

    fun cargarCategorias(tipo: String? = null): Job = viewModelScope.launch(Dispatchers.IO) {
            try {
                val respuesta = client.get(ApiConfig.CATEGORIAS_URL) {
                    if (!tipo.isNullOrBlank()) url { parameters.append("tipo", tipo) }
                }
                if (respuesta.status == HttpStatusCode.OK) {
                    val recibidas = respuesta.body<List<CategoriaFinanciera>>()
                    withContext(Dispatchers.Main) { categorias = recibidas }
                }
            } catch (e: Exception) {
                println("No se pudieron cargar categorías: ${e.message}")
            }
        }

    fun nombreCategoria(id: Int): String = categorias.firstOrNull { it.idCategoria == id }?.nombre ?: "Otros"
    // Funciones de actualización de UI
    fun cargarDatosDelServidor(): Job {
        println("DEBUG: [PASO 1] Entrando a cargarDatosDelServidor: $userIdReal")
        if(userIdReal == 0){
            println("DEBUG: Abortando carga, userIdReal sigue en 0")
            return viewModelScope.launch { }
        }
        return viewModelScope.launch(Dispatchers.IO) {
            _cargando.value = true
            println("DEBUG: [PASO 2] Lanzando petición a: ${ApiConfig.MOVIMIENTOS_URL}/$userIdReal  ")
            try {
                // 1. Llamada para obtener movimientos de este usuario específico
                // Ajusta la URL según tu API, ej: ApiConfig.MOVIMIENTOS_URL + "/$userIdReal"
                val respuesta = client.get("${ApiConfig.MOVIMIENTOS_URL}/usuario/$userIdReal") {
                    contentType(ContentType.Application.Json)
            }
                println("DEBUG: [PASO 3] Respuesta del servidor recibida. Status: ${respuesta.status}")
                if (respuesta.status == HttpStatusCode.OK) {
                    val movimientosRemotos: List<Movimiento> = respuesta.body<List<Movimiento>>()
                    println("DEBUG: [PASO4] Movimientos parseados: ${movimientosRemotos.size} items")


                    // Limpiamos la lista actual y agregamos los de la base de datos
                    withContext(Dispatchers.Main) {
                        listaDeMovimientos.clear()
                        listaDeMovimientos.addAll(movimientosRemotos)


                        saldoTotal = movimientosRemotos.sumOf { it.monto }
                        println("DEBUG: [EXITO] UI actualizada. Saldo calculado: $saldoTotal")
                    }
                    // Calculamos el saldo real basado en los movimientos recibidos
                    // O si tienes un endpoint de saldo, úsalo aquí.

                    println("DEBUG: Datos cargados. Movimientos: ${movimientosRemotos.size}, Saldo: $saldoTotal")
                }else{
                    println("DEBUG: [ERROR] El servidor respondió algo distinto a OK: ${respuesta.status}")
                }
            } catch (e: Exception) {
                println("DEBUG: Error cargando datos: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }

    fun refrescarDatos(): Job = viewModelScope.launch {
        val categoriasJob = cargarCategorias()
        val movimientosJob = cargarDatosDelServidor()
        categoriasJob.join()
        movimientosJob.join()
        val userId = userIdReal
        if (userId > 0) {
            borradoresPendientes.clear()
            borradoresPendientes.addAll(
                BorradoresNotificacion.cargarBorradores(sessionManager.applicationContext, userId)
            )
        }
    }

    fun onDescripcionChanged(valor: String) { descripcionInput = valor }
    fun onMontoChange(valor: String) {
        if (valor.isEmpty() || valor.matches("^\\d*\\.?\\d*$".toRegex())){
            montoInput = valor
            montoDouble = valor.toDoubleOrNull() ?: 0.0
        }
    }
    fun onCategoriaChange(valor: CategoriaFinanciera) {
        categoriaSeleccionada = valor.nombre
        categoriaIdSeleccionada = valor.idCategoria
    }
    fun onEsIngresoChange(valor: Boolean) {
        if (esIngresoInput != valor) {
            categoriaSeleccionada = ""
            categoriaIdSeleccionada = 0
        }
        esIngresoInput = valor
    }
    fun onFechaChange(valor: String) { fechaInput = valor }

    /** Aplica al formulario los datos que se puedan reconocer en una frase dictada. */
    fun aplicarComandoVoz(transcripcion: String): Boolean {
        val texto = transcripcion.trim()
        if (texto.isBlank()) return false

        val normalizado = texto.lowercase(Locale.forLanguageTag("es-MX"))
        val indicaIngreso = Regex("\\b(ingreso|ingres[eé]|recib[ií]|dep[oó]sito|depositaron|me pagaron|me transfirieron|transferencia recibida|transferencia entrante|sueldo|n[oó]mina|venta|vend[ií]|rendimientos?|intereses?)\\b")
            .containsMatchIn(normalizado)
        val indicaGasto = Regex("\\b(gasto|gast[eé]|compr[eé]|pagos?|pagu[eé]|pag[uú]e|pag[oó]|transfer[ií]|transferencia enviada|transferencia saliente|envi[eé]|mand[eé])\\b")
            .containsMatchIn(normalizado)
        val tipoReconocido = when {
            indicaIngreso -> "Ingreso"
            indicaGasto -> "Gasto"
            else -> null
        }
        if (indicaIngreso) esIngresoInput = true
        else if (indicaGasto) esIngresoInput = false

        val cantidadRegex = Regex("(?<![\\w])(?:\\d{1,3}(?:[.,]\\d{3})+(?:[.,]\\d{1,2})?|\\d+(?:[.,]\\d{1,2})?)(?![\\w])")
        val cantidadMatch = cantidadRegex.find(normalizado)
        val montoReconocido = cantidadMatch?.value?.let { cantidad ->
            val monto = normalizarMontoDictado(cantidad)
            if (monto.toDoubleOrNull() != null) {
                montoInput = monto
                montoDouble = monto.toDouble()
                monto.toDouble()
            } else null
        }

        val nombreCategoria = when {
            Regex("\\b(trabajo|sueldo|n[oó]mina)\\b").containsMatchIn(normalizado) -> "Trabajo"
            Regex("\\b(comida|comer|restaurante|desayuno|cena|almuerzo)\\b").containsMatchIn(normalizado) -> "Comida"
            Regex("\\b(ocio|cine|juego|entretenimiento)\\b").containsMatchIn(normalizado) -> "Ocio"
            Regex("\\b(transporte|trans|gasolina|taxi|uber|cam[ií]on)\\b").containsMatchIn(normalizado) -> "Trans."
            Regex("\\b(renta|hipoteca|muebles|hogar)\\b").containsMatchIn(normalizado) -> "Hogar"
            Regex("\\b(luz|agua|internet|tel[eé]fono|servicios)\\b").containsMatchIn(normalizado) -> "Servicios"
            Regex("\\b(consulta(s)?|m[eé]dic[oa]s?|doctor(a)?|hospital|cl[ií]nica|dentista|dental|terapia|farmacia|medicina|salud|an[aá]lisis cl[ií]nico(s)?)\\b").containsMatchIn(normalizado) -> "Salud"
            Regex("\\b(escuela|colegiatura|curso|libros|educaci[oó]n)\\b").containsMatchIn(normalizado) -> "Educación"
            Regex("\\b(ropa|compras|supermercado| tienda)\\b").containsMatchIn(normalizado) -> "Compras"
            Regex("\\b(productos?|bolsa|bolsas|art[ií]culo|art[ií]culos)\\b").containsMatchIn(normalizado) -> "Productos"
            Regex("\\b(netflix|spotify|suscripci[oó]n)\\b").containsMatchIn(normalizado) -> "Suscripciones"
            Regex("\\b(pr[eé]stamo|deuda|tarjeta de cr[eé]dito)\\b").containsMatchIn(normalizado) -> "Deudas"
            Regex("\\b(viaje|hotel|vacaciones)\\b").containsMatchIn(normalizado) -> "Viajes"
            Regex("\\b(mascota|veterinario|perro|gato)\\b").containsMatchIn(normalizado) -> "Mascotas"
            Regex("\\b(venta|vend[ií])\\b").containsMatchIn(normalizado) -> "Ventas"
            Regex("\\b(freelance|trabajo independiente)\\b").containsMatchIn(normalizado) -> "Freelance"
            Regex("\\b(rendimiento|rendimientos)\\b").containsMatchIn(normalizado) -> "Rendimientos"
            Regex("\\b(inter[eé]s|intereses)\\b").containsMatchIn(normalizado) -> "Intereses"
            Regex("\\b(inversi[oó]n|dividendo)\\b").containsMatchIn(normalizado) -> "Inversiones"
            Regex("\\b(regalo|obsequio)\\b").containsMatchIn(normalizado) -> "Regalo"
            else -> null
        }
        val categoria = nombreCategoria?.let { nombre -> categorias.firstOrNull { it.nombre.equals(nombre, true) } }
            ?: categorias.firstOrNull { it.nombre == if (esIngresoInput) "Otros ingresos" else "Otros" }
        categoriaSeleccionada = categoria?.nombre.orEmpty()
        categoriaIdSeleccionada = categoria?.idCategoria ?: 0

        val descripcion = normalizado
            .replace(Regex("^(oye[, ]*)?(anota|registra|agrega|guarda)\\s+"), "")
            .replace(Regex("\\b(ingreso|ingres[eé]|gasto|gast[eé]|compr[eé]|pagos?|pagu[eé]|pag[uú]e|pag[oó]|recib[ií]|dep[oó]sito|depositaron|me pagaron|me transfirieron|transfer[ií]|transferencia|envi[eé]|mand[eé])\\b"), " ")
            .replace(cantidadRegex, " ")
            .replace(Regex("\\b(pesos?|mxn|d[oó]lares?)\\b|\\$"), " ")
            .replace(Regex("\\b(en|de|por|con|para)\\b"), " ")
            // Conserva conceptos concretos como "sueldo", "gasolina" o "cena";
            // solo quita el nombre genérico de la categoría para dejar limpia la descripción.
            .replace(Regex("\\b(trabajo|comida|ocio|transporte|trans)\\b"), " ")
            .replace(Regex("\\s+"), " ")
            .trim(' ', ',', '.', ';', ':')

        descripcionInput = descripcion.ifBlank { texto }

        // El dictado nunca envía el movimiento inmediatamente: lo deja en el teléfono
        // para que Leonardo lo revise y confirme desde Inicio.
        if (tipoReconocido == null || montoReconocido == null || montoReconocido <= 0.0 ||
            descripcion.isBlank() || userIdReal <= 0
        ) return false

        val categoriaId = categoriaIdSeleccionada.takeIf { it > 0 } ?: return false
        val montoConSigno = if (tipoReconocido == "Ingreso") montoReconocido else -montoReconocido
        val borrador = MovimientoBorrador(
            id = UUID.randomUUID().toString(),
            idUsuario = userIdReal,
            idCategoria = categoriaId,
            monto = montoConSigno,
            fecha = LocalDate.now().toString(),
            descripcion = descripcion,
            tipo = tipoReconocido,
            transcripcion = texto,
            creadoEn = System.currentTimeMillis()
        )
        borradoresPendientes.add(0, borrador)
        guardarBorradoresLocales()
        BorradoresNotificacion.programarRecordatorio(sessionManager.applicationContext, userIdReal)
        montoInput = ""
        montoDouble = 0.0
        descripcionInput = ""
        return true
    }

    fun quitarBorrador(id: String) {
        borradoresPendientes.removeAll { it.id == id }
        guardarBorradoresLocales()
        if (borradoresPendientes.isEmpty() && userIdReal > 0) {
            BorradoresNotificacion.cancelarRecordatorio(sessionManager.applicationContext, userIdReal)
        }
    }

    /** Guarda un borrador individual y lo elimina del almacenamiento local solo si el servidor lo acepta. */
    fun confirmarBorrador(id: String) {
        if (guardandoBorradores) return
        val borrador = borradoresPendientes.firstOrNull { it.id == id } ?: return
        guardandoBorradores = true
        viewModelScope.launch {
            mensajeBorradores = null
            try {
                val movimiento = Movimiento(
                    idMovimiento = 0,
                    idUsuario = borrador.idUsuario,
                    idCategoria = borrador.idCategoria,
                    monto = borrador.monto,
                    fecha = borrador.fecha,
                    descripcion = borrador.descripcion,
                    tipo = borrador.tipo
                )
                val respuesta = client.post(ApiConfig.MOVIMIENTOS_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(movimiento)
                }
                if (respuesta.status == HttpStatusCode.Created || respuesta.status == HttpStatusCode.OK) {
                    sincronizarBorradoresGuardados(id)
                    listaDeMovimientos.add(0, movimiento)
                    saldoTotal += borrador.monto
                    puntos += 10
                    sessionManager.saveGamification(puntos, racha)
                    guardarBorradoresLocales()
                    if (borradoresPendientes.isEmpty() && userIdReal > 0) {
                        BorradoresNotificacion.cancelarRecordatorio(sessionManager.applicationContext, userIdReal)
                    }
                    mensajeBorradores = "Movimiento guardado."
                } else {
                    mensajeBorradores = "No se pudo guardar. El movimiento sigue en borradores."
                }
            } catch (e: Exception) {
                mensajeBorradores = "No se pudo guardar. Revisa tu conexión e intenta de nuevo."
                println("ERROR guardando borrador individual: ${e.message}")
            } finally {
                guardandoBorradores = false
            }
        }
    }

    fun ocultarMensajeBorradores() { mensajeBorradores = null }

    fun confirmarBorradores() {
        if (guardandoBorradores || borradoresPendientes.isEmpty()) return
        guardandoBorradores = true
        viewModelScope.launch {
            mensajeBorradores = null
            var guardados = 0
            try {
                for (borrador in borradoresPendientes.toList()) {
                    val movimiento = Movimiento(
                        idMovimiento = 0,
                        idUsuario = borrador.idUsuario,
                        idCategoria = borrador.idCategoria,
                        monto = borrador.monto,
                        fecha = borrador.fecha,
                        descripcion = borrador.descripcion,
                        tipo = borrador.tipo
                    )
                    val respuesta = client.post(ApiConfig.MOVIMIENTOS_URL) {
                        contentType(ContentType.Application.Json)
                        setBody(movimiento)
                    }
                    if (respuesta.status != HttpStatusCode.Created && respuesta.status != HttpStatusCode.OK) break

                    sincronizarBorradoresGuardados(borrador.id)
                    listaDeMovimientos.add(0, movimiento)
                    saldoTotal += borrador.monto
                    puntos += 10
                    sessionManager.saveGamification(puntos, racha)
                    guardarBorradoresLocales()
                    guardados++
                }
                mensajeBorradores = when {
                    guardados == 0 -> "No se pudieron guardar los movimientos. Siguen en borradores."
                    borradoresPendientes.isEmpty() -> "Se guardaron $guardados movimiento(s)."
                    else -> "Se guardaron $guardados. Quedan ${borradoresPendientes.size} por confirmar."
                }
                if (borradoresPendientes.isEmpty() && userIdReal > 0) {
                    BorradoresNotificacion.cancelarRecordatorio(sessionManager.applicationContext, userIdReal)
                }
            } catch (e: Exception) {
                mensajeBorradores = if (guardados == 0) {
                    "No se pudieron guardar. Revisa tu conexión e intenta de nuevo."
                } else {
                    "Se guardaron $guardados. Los demás siguen como borradores."
                }
                println("ERROR guardando borradores de movimientos: ${e.message}")
            } finally {
                guardandoBorradores = false
            }
        }
    }

    private fun sincronizarBorradoresGuardados(id: String) {
        val restantes = BorradoresNotificacion.eliminarBorrador(
            sessionManager.applicationContext,
            userIdReal,
            id
        )
        borradoresPendientes.removeAll { it.id == id }
        // Mantiene el estado en memoria igual al almacenamiento, incluidas altas concurrentes del widget.
        if (borradoresPendientes.toList() != restantes) {
            borradoresPendientes.clear()
            borradoresPendientes.addAll(restantes)
        }
    }

    private fun guardarBorradoresLocales() {
        if (userIdReal > 0) {
            BorradoresNotificacion.guardarBorradores(
                sessionManager.applicationContext,
                userIdReal,
                borradoresPendientes.toList()
            )
            MovimientoWidgetProvider.actualizarTodos(sessionManager.applicationContext)
        }
    }

    override fun onCleared() {
        borradoresPreferences.unregisterOnSharedPreferenceChangeListener(borradoresListener)
        client.close()
        super.onCleared()
    }

    private fun normalizarMontoDictado(cantidad: String): String {
        val ultimoPunto = cantidad.lastIndexOf('.')
        val ultimaComa = cantidad.lastIndexOf(',')
        return when {
            ultimoPunto >= 0 && ultimaComa >= 0 -> {
                val separadorDecimal = maxOf(ultimoPunto, ultimaComa)
                cantidad.filterIndexed { index, char -> char.isDigit() || index == separadorDecimal }
                    .replace(',', '.')
            }
            ultimaComa >= 0 && cantidad.length - ultimaComa - 1 in 1..2 -> cantidad.replace(".", "").replace(',', '.')
            ultimaComa >= 0 -> cantidad.replace(",", "")
            ultimoPunto >= 0 && cantidad.length - ultimoPunto - 1 == 3 -> cantidad.replace(".", "")
            else -> cantidad.replace(",", "")
        }
    }

    fun resetFormulario(){
        montoInput = ""
        descripcionInput = ""
        idCategoria = 0
        categoriaSeleccionada = ""
        categoriaIdSeleccionada = 0
    }

    //Funcion para registrar
    fun registrarMovimiento(onSuccess: () -> Unit) {
        val montoDouble = montoInput.toDoubleOrNull() ?: 0.0
        println("DEBUG: Intentando registrar -> Descripción: $descripcionInput, Monto: $montoDouble, User: $userIdReal")

        if (descripcionInput.isNotEmpty() && montoDouble > 0) {
            val montoFinal = if(esIngresoInput) montoDouble else -montoDouble

            val idReal = categoriaIdSeleccionada
            if (idReal <= 0) {
                println("DEBUG: Selecciona una categoría antes de guardar")
                return
            }


            val nuevo = Movimiento(
                idMovimiento = 0,
                idUsuario = userIdReal, // Aquí después usarás el ID del usuario logueado
                idCategoria = idReal,
                monto = montoFinal,
                fecha = fechaInput, // Ya viene como "yyyy-MM-dd"
                descripcion = descripcionInput,
                tipo = if(esIngresoInput) "Ingreso" else "Gasto"
            )

            // Iniciamos la petición al servidor
            viewModelScope.launch {
                _cargando.value = true
                try {
                    val respuesta = client.post(ApiConfig.MOVIMIENTOS_URL) { // Asegúrate de tener esta URL en ApiConfig
                        contentType(ContentType.Application.Json)
                        setBody(nuevo)
                    }
                    //LOG 2: Ver qué respondió el servidor
                    println("DEBUG: Respuesta Servidor Movimiento: ${respuesta.status}")

                    if (respuesta.status == HttpStatusCode.Created || respuesta.status == HttpStatusCode.OK) {
                        // Solo si el servidor confirma, actualizamos la lista visual
                        listaDeMovimientos.add(0, nuevo)
                        //Se actualiza el saldo
                        saldoTotal += montoFinal
                        puntos += 10
                        sessionManager.saveGamification(puntos, racha)

                        // Limpiar campos
                        resetFormulario()
                        onSuccess()
                    }
                } catch (e: Exception) {
                    //LOG 3: Ver si hubo error de conexión
                    println("DEBUG: ERROR DE RED:  ${e.message}")
                    e.printStackTrace()
                } finally {
                    _cargando.value = false
                }
            }
        }else{
            println("DEBUG: Validación fallida - Campos vacíos o monto 0")
        }
    }
}
