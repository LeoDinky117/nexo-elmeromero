package com.nexo.app.ui.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.Movimiento
import com.nexo.app.util.ApiConfig
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.time.LocalDate

class MovimientosViewModel(private val sessionManager: SessionManager) : ViewModel() {

    private var userIdReal : Int = 0
    var idCategoria by mutableStateOf(0)

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


    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    // Cliente Ktor (Igual al de tu Login)
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    init {
        viewModelScope.launch {
            //Leemos el ID desde el SessionManager(DataSource)
            sessionManager.userIdFlow.collect { id ->
                if (id!= 0){
                    userIdReal = id
                    println("DEBUG: ID de usuario recuperado en Movimientos: $userIdReal")

                }
            }
        }
    }
    // Funciones de actualización de UI
    fun cargarDatosDelServidor() {
        println("DEBUG: [PASO 1] Entrando a cargarDatosDelServidor: $userIdReal")
        if(userIdReal == 0){
            println("DEBUG: Abortando carga, userIdReal sigue en 0")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
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

    fun onDescripcionChanged(valor: String) { descripcionInput = valor }
    fun onMontoChange(valor: String) {
        if (valor.isEmpty() || valor.matches("^\\d*\\.?\\d*$".toRegex())){
            montoInput = valor
            montoDouble = valor.toDoubleOrNull() ?: 0.0
        }
    }
    fun onCategoriaChange(valor: String) { categoriaSeleccionada = valor }
    fun onEsIngresoChange(valor: Boolean) { esIngresoInput = valor }
    fun onFechaChange(valor: String) { fechaInput = valor }
    fun resetFormulario(){
        montoInput = ""
        descripcionInput = ""
        idCategoria = 0
    }

    //Funcion para registrar
    fun registrarMovimiento(onSuccess: () -> Unit) {
        val montoDouble = montoInput.toDoubleOrNull() ?: 0.0
        println("DEBUG: Intentando registrar -> Descripción: $descripcionInput, Monto: $montoDouble, User: $userIdReal")

        if (descripcionInput.isNotEmpty() && montoDouble > 0) {
            val montoFinal = if(esIngresoInput) montoDouble else -montoDouble

            val idReal = when (categoriaSeleccionada) {
                "Trabajo" -> 1
                "Comida" -> 2
                "Ocio" -> 3
                "Trans." -> 4
                else -> 5 // Categoría por defecto
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