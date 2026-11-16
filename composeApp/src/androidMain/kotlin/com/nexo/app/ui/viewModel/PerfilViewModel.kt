package com.nexo.app.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.PerfilUsuario
import com.nexo.app.model.PerfilEditable
import com.nexo.app.model.EstadoRecompensas
import com.nexo.app.model.SeleccionRecompensa
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

class PerfilViewModel(private val sessionManager: SessionManager) : ViewModel() {
    private var userIdReal: Int = 0

    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil

    private val _cargando = MutableStateFlow(true)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    private val _tienda = MutableStateFlow<EstadoRecompensas?>(null)
    val tienda: StateFlow<EstadoRecompensas?> = _tienda

    private val _tiendaCargando = MutableStateFlow(false)
    val tiendaCargando: StateFlow<Boolean> = _tiendaCargando

    private val _accionRecompensaEnCurso = MutableStateFlow(false)
    val accionRecompensaEnCurso: StateFlow<Boolean> = _accionRecompensaEnCurso

    var modoEdicion by mutableStateOf(false)
        private set
    var nombreEditado by mutableStateOf("")
        private set
    var correoEditado by mutableStateOf("")
        private set
    private val _guardandoPerfil = MutableStateFlow(false)
    val guardandoPerfil: StateFlow<Boolean> = _guardandoPerfil

    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collectLatest { id ->
                userIdReal = id
                if (id > 0) cargarPerfil(id)
                else {
                    _perfil.value = null
                    _cargando.value = false
                }
            }
        }
    }

    fun recargarPerfil(): Job = viewModelScope.launch {
            val id = sessionManager.userIdFlow.first { it > 0 }
            userIdReal = id
            cargarPerfil(id).join()
    }

    fun mensajeMostrado() {
        _mensajeUI.value = null
    }

    fun iniciarEdicion() {
        val actual = _perfil.value ?: return
        nombreEditado = actual.nombre
        correoEditado = actual.correo
        modoEdicion = true
    }

    fun cambiarNombre(value: String) { nombreEditado = value }
    fun cambiarCorreo(value: String) { correoEditado = value }
    fun cancelarEdicion() { modoEdicion = false }

    fun guardarPerfil() = viewModelScope.launch {
        val nombre = nombreEditado.trim()
        val correo = correoEditado.trim().lowercase()
        if (nombre.isBlank() || nombre.length > 100) {
            _mensajeUI.value = "El nombre debe tener entre 1 y 100 caracteres."
            return@launch
        }
        if (!Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(correo) || correo.length > 100) {
            _mensajeUI.value = "Escribe un correo electrónico válido."
            return@launch
        }
        if (userIdReal <= 0 || _guardandoPerfil.value) return@launch
        _guardandoPerfil.value = true
        try {
            val respuesta = client.put("${ApiConfig.PERFIL_URL}/usuario/$userIdReal") {
                contentType(ContentType.Application.Json)
                setBody(PerfilEditable(nombre, correo))
            }
            if (respuesta.status == HttpStatusCode.OK) {
                _perfil.value = _perfil.value?.copy(nombre = nombre, correo = correo)
                modoEdicion = false
                _mensajeUI.value = "Perfil actualizado."
            } else {
                val detalle = respuesta.bodyAsText()
                val mensaje = runCatching {
                    Json.parseToJsonElement(detalle).jsonObject["mensaje"]?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                _mensajeUI.value = mensaje ?: "No se pudo actualizar el perfil."
            }
        } catch (e: Exception) {
            _mensajeUI.value = "No se pudo actualizar el perfil. Revisa tu conexión."
        } finally {
            _guardandoPerfil.value = false
        }
    }

    fun cargarPerfil(idUsuario: Int): Job = viewModelScope.launch {
            if (idUsuario <= 0) return@launch

            _cargando.value = true
            println("DEBUG ANDROID: Iniciando carga para ID: $idUsuario")
            println("DEBUG ANDROID: URL destino: ${ApiConfig.PERFIL_URL}/usuario/$idUsuario")

            try {
                // El delay para dar tiempo a la conexión
                delay(500)

                val respuesta = client.get("${ApiConfig.PERFIL_URL}/usuario/$idUsuario")
                if (respuesta.status != HttpStatusCode.OK) {
                    val detalle = respuesta.bodyAsText()
                    println("DEBUG ANDROID: Perfil respondió HTTP ${respuesta.status.value}: $detalle")
                    _mensajeUI.value = "No se pudieron cargar los datos del perfil (HTTP ${respuesta.status.value})."
                    return@launch
                }
                _perfil.value = respuesta.body()
                println("DEBUG ANDROID: Respuesta exitosa: ${_perfil.value?.nombre}")
                cargarTienda(idUsuario)
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudo conectar con el servidor para cargar el perfil."
                println("DEBUG ANDROID: FALLÓ LA PETICIÓN - Mensaje: ${e.message}")
                e.printStackTrace()
            } finally {
                _cargando.value = false
            }
        }

    private suspend fun cargarTienda(idUsuario: Int) {
        _tiendaCargando.value = true
        try {
            val respuesta = client.get("${ApiConfig.RECOMPENSAS_URL}/usuario/$idUsuario")
            if (respuesta.status == HttpStatusCode.OK) {
                aplicarEstadoTienda(respuesta.body())
            } else {
                _mensajeUI.value = "No se pudo cargar el catálogo de recompensas."
            }
        } catch (e: Exception) {
            _mensajeUI.value = "No se pudo conectar con el catálogo de recompensas."
        } finally {
            _tiendaCargando.value = false
        }
    }

    private suspend fun aplicarEstadoTienda(estado: EstadoRecompensas) {
        _tienda.value = estado
        _perfil.value = _perfil.value?.copy(puntos = estado.puntos)
        val racha = sessionManager.rachaFlow.first()
        sessionManager.saveGamification(estado.puntos, racha)
        sessionManager.saveRewardStyle(estado.temaVisualActivo, estado.iconoVisualActivo)
    }

    fun desbloquearRecompensa(id: Int) = enviarAccionRecompensa("${ApiConfig.RECOMPENSAS_URL}/usuario/$userIdReal/desbloquear/$id")

    fun equiparRecompensa(tipo: String, codigo: String) = enviarAccionRecompensa(
        "${ApiConfig.RECOMPENSAS_URL}/usuario/$userIdReal/equipar",
        SeleccionRecompensa(tipo, codigo)
    )

    private fun enviarAccionRecompensa(url: String, body: SeleccionRecompensa? = null) = viewModelScope.launch {
        if (userIdReal <= 0 || _accionRecompensaEnCurso.value) return@launch
        _accionRecompensaEnCurso.value = true
        try {
            val respuesta = client.post(url) {
                if (body != null) {
                    contentType(ContentType.Application.Json)
                    setBody(body)
                }
            }
            if (respuesta.status == HttpStatusCode.OK) {
                aplicarEstadoTienda(respuesta.body())
                _mensajeUI.value = if (body == null) "Premio desbloqueado. ¡Ya puedes equiparlo!" else "Tu recompensa ya está activa."
            } else {
                val detalle = respuesta.bodyAsText()
                val mensaje = runCatching { Json.parseToJsonElement(detalle).jsonObject["mensaje"]?.jsonPrimitive?.contentOrNull }.getOrNull()
                _mensajeUI.value = mensaje ?: "No se pudo completar la acción."
            }
        } catch (e: Exception) {
            _mensajeUI.value = "No se pudo guardar el cambio. Revisa tu conexión e inténtalo de nuevo."
        } finally {
            _accionRecompensaEnCurso.value = false
        }
    }
} // <--- ESTA llave debe ser la última del archivo
