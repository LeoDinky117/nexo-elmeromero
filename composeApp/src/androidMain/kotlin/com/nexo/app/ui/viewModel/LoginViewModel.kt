package com.nexo.app.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.model.UsuarioLogin
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
import io.ktor.client.request.*
import io.ktor.http.contentType
import io.ktor.http.*
import io.ktor.client.call.*
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {
    var correo = mutableStateOf("")
        private set
    var password = mutableStateOf("")
        private set
    //Estado para manejar el exito del login
    private val _loginExitoso = MutableStateFlow(false)
    val loginExitoso: StateFlow<Boolean> = _loginExitoso

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {json(Json{ignoreUnknownKeys = true})}
    }
    //__________________________________________
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
        viewModelScope.launch{
            try {
                val respuesta = client.post("http://192.168.1.4:8080/login"){
                    contentType(ContentType.Application.Json)
                    setBody(UsuarioLogin(correo.value, password.value))
                }
                if (respuesta.status == HttpStatusCode.OK) {
                    println("Login exitoso para: ${correo.value}")
                    _loginExitoso.value = true
                }else{
                    println("Credenciales Incorrectas")
                }
            }catch(e: Exception){
                //println("Error de red: ${e.localizedMessage}")
                println("Error de red: ${e.message}")
            }
        }
        println("Login con ${correo.value}")
        // aquí luego irá la API
    }
}