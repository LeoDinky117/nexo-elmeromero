package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
//--------------------------------
import androidx.lifecycle.viewModelScope
import com.nexo.app.model.Usuario
import com.nexo.app.util.ApiConfig
import com.nexo.app.util.client
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel : ViewModel() {

    private val _mensajeUI = MutableStateFlow<String?>(null)
    val mensajeUI: StateFlow<String?> = _mensajeUI
    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

    //Estados para el formulario
    var nombre = mutableStateOf("")
        private set
    var edad = mutableStateOf("")
        private set
    var correo = mutableStateOf("")
        private set
    var password = mutableStateOf("")
        private set
    var confirmar = mutableStateOf("")
        private set

    //Función para limpiar el mensaje después de mostrarlo
    fun mensajeMostrado(){
        _mensajeUI.value = null
    }
    fun onNombreChange(value: String) {
        nombre.value = value
    }
    //Le agregué esta función de edad
    fun onEdadChange(value: String) {
        val edadLimpia = value.trim()
        if (edadLimpia.isEmpty() || edadLimpia.all { it.isDigit() }){
            edad.value = edadLimpia
        }
    }
    fun onCorreoChange(value: String) {
        correo.value = value
    }
    fun onPasswordChange(value: String) {
        password.value = value
    }
    fun onConfirmarChange(value: String) {
        confirmar.value = value
    }
    fun registrar() {
        //Validación de campos vacíos
        if (nombre.value.isEmpty() || correo.value.isEmpty() || password.value.isEmpty()){
            println("Rellenar campos vacíos")
            return
        }
        //Validación de coincidencia de contraseñas
        if (password.value != confirmar.value) {
            println("Las contraseñas no coinciden")
            return
        }
        val edadNumerica = edad.value.toIntOrNull() ?: 0
        if (edadNumerica <15 || edadNumerica > 25){
            println("La edad debe permitida es de 15 a 25 años...")
            println("Por favor ingrese una edad valida")
            return
        }

        println("Usuario registrado: ${nombre.value}")
        //2.- Llamada a la API
        viewModelScope.launch {
            try {
                val nuevoUsuario = Usuario(
                    nombre = nombre.value,
                    edad = edad.value.toIntOrNull() ?: 0,
                    correo = correo.value,
                    password = password.value,
                )
                //Se usa el cliente que configuramos antes
                //Cambiar URL por la del servidor

                println("Enviando JSON de usuario: $nuevoUsuario")
                //Requests==
                val respuesta = client.post(ApiConfig.REGISTER_URL) {
                    contentType(ContentType.Application.Json)
                    setBody(nuevoUsuario)
                }
                //Mensaje en Consola
                println("--RESPUESTA DEL SERVIDOR--")
                println("Status Code: ${respuesta.status}")

                if (respuesta.status.value in 200..299) {
                    println("Usuario registrado en el servidor: ${nombre.value}")
                    //Estado de registro exitoso:
                    _registroExitoso.value = true
                    _mensajeUI.value = "¡Cuenta creada con éxito!"
                }else{
                    _mensajeUI.value = "Error del servidor: ${respuesta.status}"
                }
            }catch (e: Exception){
                //Esto atrapará errores de internet (ej. sin conexion)
                _mensajeUI.value = "Error de red: no se pudo conectar"
                println("Error de red: ${e.message}")
            }
        }
    }
    //Función para "limpiar" el estado después de navegar
    fun resetRegistroEstado(){
        _registroExitoso.value = false
    }
}