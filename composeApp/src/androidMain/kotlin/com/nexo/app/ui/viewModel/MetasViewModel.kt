package com.nexo.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.AportacionMeta
import com.nexo.app.model.MetaAhorro
import com.nexo.app.model.ProgresoMetaVista
import com.nexo.app.model.MetaCreadaResponse
import com.nexo.app.model.PeriodicidadAportacionMeta
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.RecordatoriosMetas
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

class MetasViewModel (private val sessionManager: SessionManager) : ViewModel() {
    private var userIdReal: Int = 0
    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collect {id ->
                if (id > 0){
                    userIdReal = id
                    cargarMetas(id)
                } else {
                    userIdReal = 0
                    _metas.value = emptyList()
                    _cargando.value = false
                    _abonoGuardando.value = false
                    limpiarFormulario()
                }
            }
        }
    }

    var nombreMeta by mutableStateOf("")
        private set
    var montoObjetivo by mutableStateOf("")
        private set
    var fechaLimite by mutableStateOf("")
        private set
    var periodicidadAportacion by mutableStateOf(PeriodicidadAportacionMeta.SEMANAL)
        private set

    private val _metas = MutableStateFlow<List<ProgresoMetaVista>>(emptyList())
    val metas: StateFlow<List<ProgresoMetaVista>> = _metas

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    private val _abonoGuardando = MutableStateFlow(false)
    val abonoGuardando: StateFlow<Boolean> = _abonoGuardando

    fun onNombreMetaChange(value: String) { nombreMeta = value }
    fun onPeriodicidadAportacionChange(value: PeriodicidadAportacionMeta) { periodicidadAportacion = value }
    // Conserva los dígitos y permite un solo punto decimal con hasta dos decimales.
    fun onMontoObjetivoChange(value: String) {
        val numero = value.filter { it.isDigit() || it == '.' }
        val punto = numero.indexOf('.')

        montoObjetivo = if (punto == -1) {
            numero
        } else {
            val parteEntera = numero.substring(0, punto)
            val parteDecimal = numero.substring(punto + 1)
                .replace(".", "")
                .take(2)

            "$parteEntera.$parteDecimal"
        }
    }

    // Acepta dígitos y agrega automáticamente los guiones del formato AAAA-MM-DD.
    fun onFechaLimiteChange(value: String) {
        val digitos = value.filter { it.isDigit() }.take(8)

        fechaLimite = when {
            digitos.length <= 4 -> digitos
            digitos.length <= 6 ->
                "${digitos.substring(0, 4)}-${digitos.substring(4)}"
            else ->
                "${digitos.substring(0, 4)}-${digitos.substring(4, 6)}-${digitos.substring(6)}"
        }
    }
    fun mensajeMostrado() { _mensajeUI.value = null }

    private fun limpiarFormulario() {
        nombreMeta = ""
        montoObjetivo = ""
        fechaLimite = ""
        periodicidadAportacion = PeriodicidadAportacionMeta.SEMANAL
    }

    fun cargarMetas(idUsuario: Int): Job = viewModelScope.launch {
            if (idUsuario <= 0 || idUsuario != userIdReal) return@launch
            _cargando.value = true
            try {
                val recibidas: List<ProgresoMetaVista> = client.get("${ApiConfig.METAS_URL}/usuario/$idUsuario").body()
                if (userIdReal == idUsuario) {
                    _metas.value = recibidas
                    RecordatoriosMetas.sincronizar(sessionManager.applicationContext, idUsuario, recibidas)
                }
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudieron cargar las metas"
                println("Error al cargar metas: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }

    fun refrescarMetas(): Job = viewModelScope.launch {
        if (userIdReal > 0) cargarMetas(userIdReal).join()
    }

    fun registrarAbono(idMeta: Int, montoTexto: String, registrarSalida: suspend (String, Double) -> Boolean) = viewModelScope.launch {
        val monto = montoTexto.trim().replace(',', '.').toDoubleOrNull()
        if (userIdReal <= 0) {
            _mensajeUI.value = "Tu sesión no está disponible."
            return@launch
        }
        if (monto == null || !monto.isFinite() || monto <= 0.0) {
            _mensajeUI.value = "Escribe un monto válido mayor que cero."
            return@launch
        }
        if (_abonoGuardando.value) return@launch
        _abonoGuardando.value = true
        try {
            val response = client.post("${ApiConfig.METAS_URL}/$idMeta/abonos") {
                contentType(ContentType.Application.Json)
                setBody(AportacionMeta(idUsuario = userIdReal, montoAhorrado = monto))
            }
            if (response.status == HttpStatusCode.Created) {
                val meta = _metas.value.firstOrNull { it.idMeta == idMeta }
                val salidaRegistrada = registrarSalida("Abono a meta: ${meta?.nombreMeta ?: "ahorro"}", monto)
                _mensajeUI.value = if (salidaRegistrada) "Abono guardado y descontado del saldo. ¡Buen avance!"
                    else "El abono quedó guardado, pero no se pudo descontar del saldo. Revisa tu conexión."
                cargarMetas(userIdReal).join()
            } else {
                _mensajeUI.value = "No se pudo guardar. Revisa el monto que falta para completar la meta."
            }
        } catch (e: Exception) {
            _mensajeUI.value = "No se pudo guardar el abono. Revisa tu conexión."
        } finally {
            _abonoGuardando.value = false
        }
    }

    fun registrarMeta() {
        println("---- DEBUG METAS: clic en Guardar meta ----")
        println(
            "DEBUG METAS: userId:$userIdReal," +
            "nombre='${nombreMeta.trim()}'," +
            "monto='$montoObjetivo', fecha='${fechaLimite.trim()}'"
        )
        if (userIdReal == 0){
            _mensajeUI.value = "Error de sesión"
            return
        }
        if (nombreMeta.isBlank() || montoObjetivo.isBlank() || fechaLimite.isBlank()) {
            _mensajeUI.value = "Completa todos los campos"
            return
        }

        val monto = montoObjetivo.toDoubleOrNull()
        if (monto == null || monto <= 0.0) {
            _mensajeUI.value = "Monto objetivo inválido"
            return
        }

        viewModelScope.launch {
            _cargando.value = true
            try {
                val fechaObjetivo = runCatching { java.time.LocalDate.parse(fechaLimite.trim()) }.getOrNull()
                if (fechaObjetivo == null || !fechaObjetivo.isAfter(java.time.LocalDate.now())) {
                    _mensajeUI.value = "Selecciona una fecha objetivo futura válida"
                    return@launch
                }

                val nuevaMeta = MetaAhorro(
                    idUsuario = userIdReal,
                    nombreMeta = nombreMeta.trim(),
                    montoObjetivo = monto,
                    fechaLimite = fechaLimite.trim(),
                    activa = true,
                    totalAhorrado = 0.0
                )
                println("DEBUG METAS: validación correcta; enviando POST a ${ApiConfig.METAS_URL}")
                println("DEBUG METAS: cuerpo a enviar=$nuevaMeta")



                val response = client.post(ApiConfig.METAS_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(nuevaMeta)
                }
                println("DEBUG METAS: respuesta HTTP ${response.status}")

                if (response.status == HttpStatusCode.Created) {
                    println("DEBUG METAS: guardado confirmado por el servidor")
                    val metaCreada = response.body<MetaCreadaResponse>()
                    RecordatoriosMetas.programar(
                        context = sessionManager.applicationContext,
                        userId = userIdReal,
                        metaId = metaCreada.idMeta,
                        nombre = nuevaMeta.nombreMeta,
                        fechaObjetivo = nuevaMeta.fechaLimite,
                        periodicidad = periodicidadAportacion
                    )
                    _mensajeUI.value = "Meta registrada con éxito"
                    limpiarFormulario()
                    cargarMetas(userIdReal).join()
                } else {
                    println("DEBUG METAS: el servidor rechazó el guardado; estado=${response.status}")
                    _mensajeUI.value = "Error al registrar la meta"
                }
            } catch (e: Exception) {
                println("DEBUG METAS: excepción al registrar — ${e::class.simpleName}: ${e.message}")
                _mensajeUI.value = "Error de red al registrar meta"
                println("Error registrarMeta: ${e.message}")
            } finally {
                _cargando.value = false
                println("DEBUG METAS: terminó el intento de guardado")
            }
        }
    }
}
