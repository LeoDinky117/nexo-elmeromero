package com.nexo.app.util
 object ApiConfig {

     //Se almacena la IP en una variable
     // La app se conecta al servidor Ktor por HTTP. MariaDB usa 3306 entre el servidor y la BD.
     private const val CASA_IP ="http://192.168.1.3:8080"
     private const val CELULAR_IP="http://10.81.149.148:8080"
     private const val ESCUELA_IP="http://10.39.189.148:8080"
     private const val Internet_Compartida= "http://127.0.0.1:8080"
     private const val EMULADOR_IP = "http://10.0.2.2:8080"

     // VARIABLE GLOBAL DE LA IP/API // Se asigna aquí la IP
     const val BASE_URL = ESCUELA_IP


     //Endpoints automáticos
     const val LOGIN_URL = "$BASE_URL/login"
     const val REGISTER_URL = "$BASE_URL/register"
     const val MOVIMIENTOS_URL = "$BASE_URL/movimientos"
     const val PERFIL_URL = "$BASE_URL/perfil"
     const val METAS_URL = "$BASE_URL/metas"
     const val CATEGORIAS_URL = "$BASE_URL/categorias"
     const val RECOMPENSAS_URL = "$BASE_URL/recompensas"


}
