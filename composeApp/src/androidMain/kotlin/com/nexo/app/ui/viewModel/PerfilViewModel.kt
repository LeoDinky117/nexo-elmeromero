package com.nexo.app.ui.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.PerfilUsuario
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PerfilViewModel (private val sessionManager: SessionManager): ViewModel() {
    private var userIdReal: Int = 0

    init {
        //Buscamos el ID automáticamente al iniciar VM
        viewModelScope.launch{
            sessionManager.userIdFlow.collect { id ->
                if (id != 0){
                    userIdReal = id
                    cargarPerfil(id)
                }
            }
        }
    }
    private val _perfil = MutableStateFlow<PerfilUsuario?>(null)
    val perfil: StateFlow<PerfilUsuario?> = _perfil

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI

    fun mensajeMostrado() {
        _mensajeUI.value = null
    }

    fun cargarPerfil(idUsuario: Int) {
        viewModelScope.launch {
            _cargando.value = true
            try {
                _perfil.value = client.get("${ApiConfig.PERFIL_URL}/$idUsuario").body()
            } catch (e: Exception) {
                _mensajeUI.value = "No se pudo cargar el perfil"
                println("Error cargarPerfil: ${e.message}")
            } finally {
                _cargando.value = false
            }
        }
    }
}
