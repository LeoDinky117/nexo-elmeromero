package com.nexo.app.util
 object ApiConfig {

     //EN ESTA CLASE SE CON
     private const val CASA_IP = "http://192.168.1.10:8080"
     private const val CELULAR_IP = "http://10.213.83.49:8080"
     private const val ESCUELA_IP = "http://10.10.198.61:8080"

     // VARIABLE GLOBAL DE LA IP/API
     const val BASE_URL = CASA_IP

     //Endpoints automáticos
     const val LOGIN_URL = "$BASE_URL/login"
     const val REGISTER_URL = "$BASE_URL/register"
     const val MOVIMIENTOS_URL = "$BASE_URL/movimientos"
     const val PERFIL_URL = "$BASE_URL/perfil"


}