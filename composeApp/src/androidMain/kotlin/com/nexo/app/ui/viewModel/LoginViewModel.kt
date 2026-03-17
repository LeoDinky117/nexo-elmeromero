package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel

class LoginViewModel : ViewModel() {

    var correo = mutableStateOf("")
        private set

    var password = mutableStateOf("")
        private set

    fun onCorreoChange(value: String) {
        correo.value = value
    }

    fun onPasswordChange(value: String) {
        password.value = value
    }

    fun login() {

        if (correo.value.isEmpty() || password.value.isEmpty()) {
            println("Campos vacíos")
            return
        }

        println("Login con ${correo.value}")

        // aquí luego irá la API
    }
}