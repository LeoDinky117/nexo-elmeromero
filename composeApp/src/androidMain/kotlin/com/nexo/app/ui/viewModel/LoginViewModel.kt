package com.nexo.app.ui.viewModel


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
import io.ktor.client.call.*
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.UsuarioResponse
import com.nexo.app.util.ApiConfig
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LoginViewModel(private val sessionManager: SessionManager) : ViewModel() {
    var correo by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    //Estado para manejar el exito del login
    private val _loginExitoso = MutableStateFlow(false)
    val loginExitoso: MutableStateFlow<Boolean> = _loginExitoso
    //Estado para controlar el indicador de carga
    private val _cargando = MutableStateFlow(false)
    val cargando : StateFlow<Boolean> = _cargando

    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {json(Json{ignoreUnknownKeys = true})}
    }
    //__________________________________________
    //FUNCIONES PARA ACTUALIZAR EL ESTADO DESDE LA SCREEN
    fun onCorreoChange(value: String) {
        correo = value
    }
    fun onPasswordChange(value: String) {
        password = value
    }
    fun resetLoginState() {
        _loginExitoso.value = false
    }
    fun login() {
        if (correo.isEmpty() || password.isEmpty()) {
            println("Campos vacíos")
            return
        }
        viewModelScope.launch{
            _cargando.value = true
            try {
                println("DEBUG: Iniciando peticion")
                val respuesta = client.post(ApiConfig.LOGIN_URL){
                    contentType(ContentType.Application.Json)
                    setBody(UsuarioLogin(correo, password))
                }
                println("DEBUG: Respuesta del servidor: ${respuesta.status}")

                if (respuesta.status == HttpStatusCode.OK) {
                    //1.- Se lee el contenido tal cual está
                    val usuarioResponse: UsuarioResponse = respuesta.body()
                    val id = usuarioResponse.idUsuario
                    if(id > 0) {
                        sessionManager.saveUserId(id)
                        println("DEBUG: ID $id guardado exitosamente")
                        //Forzamos el cambio al hilo principal para que la UI se entere
                        withContext(Dispatchers.Main){
                            _loginExitoso.value = true
                        }
                    }

                }else{
                    _loginExitoso.value = false //Estado de error en login
                    println("Credenciales Incorrectas (Status: ${respuesta.status})")
                }
            }catch(e: Exception){
                //println("Error de red: ${e.localizedMessage}")
                println("Error: ${e.message}")
                e.printStackTrace()//Esto imprime toda la ruta del error en rojo
                _loginExitoso.value = false
            }finally {
                _cargando.value = false
            }
        }
    }
}