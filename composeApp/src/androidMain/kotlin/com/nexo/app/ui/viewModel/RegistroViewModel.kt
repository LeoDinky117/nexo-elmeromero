package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
//--------------------------------
import androidx.lifecycle.viewModelScope
import com.nexo.app.model.Usuario
import com.nexo.app.util.client
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RegistroViewModel : ViewModel() {

    private val _registroExitoso = MutableStateFlow(false)
    val registroExitoso: StateFlow<Boolean> = _registroExitoso

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
    fun onNombreChange(value: String) {
        nombre.value = value
    }
    //Le agregué esta función de edad
    fun onEdadChange(value: String) {
        if (value.all { it.isDigit() })
        edad.value = value
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
        if (nombre.value.isEmpty() || correo.value.isEmpty() || password.value.isEmpty()){
            println("Rellenar campos vacíos")
            return
        }
        if (password.value != confirmar.value) {
            println("Las contraseñas no coinciden")
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
                val respuesta = client.post("http://192.168.1.4:8080/register") {
                    contentType(ContentType.Application.Json)
                    setBody(nuevoUsuario)
                }
                if (respuesta.status.value in 200..299) {
                    println("Usuario registrado en el servidor: ${nombre.value}")
                    //Estado de registro exitoso:
                    _registroExitoso.value = true
                }else{
                    println("Error del servidor: ${respuesta.status}")
                }
            }catch (e: Exception){
                //Esto atrapará errores de internet (ej. sin conexion)
                println("Error de red: ${e.message}")
            }
        }
        //Función para "limpiar" el estado después de navegar


        // aquí irá la API
    }
    fun resetRegistroEstado(){
        _registroExitoso.value = false
    }
}