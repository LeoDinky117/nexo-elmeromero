package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class RegistroViewModel : ViewModel() {

    var nombre = mutableStateOf("")
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

        // aquí irá la API
    }
}