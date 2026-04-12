package com.nexo.app.util
 object ApiConfig {

     //Se almacena la IP en una variable
     private const val CASA_IP = "http://192.168.1.6:8080"
     private const val CELULAR_IP = "http://10.12.202.49:8080"
     private const val ESCUELA_IP = "http://10.10.198.61:8080"

     // VARIABLE GLOBAL DE LA IP/API // Se asigna aqui la IP
     const val BASE_URL = CASA_IP

     //Endpoints automáticos
     const val LOGIN_URL = "$BASE_URL/login"
     const val REGISTER_URL = "$BASE_URL/register"
     const val MOVIMIENTOS_URL = "$BASE_URL/movimientos/registrar"
     const val PERFIL_URL = "$BASE_URL/perfil"


}