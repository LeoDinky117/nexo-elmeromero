package com.example

import com.example.com.nexo.app.model.Usuario
import com.example.com.nexo.app.model.UsuarioLogin
import db.DatabaseFactory
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.insert
import db.Usuarios // Asegúrate de que este sea el nombre de tu objeto Table
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq

fun Application.configureRouting() {
    routing {
        // Esta es la ruta que llamaremos desde Android
        post("/register") {
            try {
                // 1. Se recibe el JSON y lo convertimos automáticamente a la clase Usuario
                val userRequest = call.receive<Usuario>()
                println("Recibido correctamente: $userRequest")

                // 2. Se abre una transacción para guardar en SQL Server
                newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) {
                    Usuarios.insert {
                        //exec("EXEC RegistrarUsuario @Nombre='${userRequest.nombre}', @Correo='${userRequest.correo}', @Contrasena='${userRequest.password}', @Edad=${userRequest.edad}")
                        it[nombre] = userRequest.nombre
                        it[edad ] = userRequest.edad
                        it[correo] = userRequest.correo
                        it[password] = userRequest.password
                        //it[edad] = userRequest.edad
                        //it[fecha] = LocalDateTime.now() //Esto es para agregar la fecha de registro
                    }
                }

                // 3. Respondemos a la App que todo salió bien
                call.respond(HttpStatusCode.Created, "Usuario ${userRequest.nombre} registrado correctamente")
            } catch (e: Exception) {
                println("Error en register: ${e.localizedMessage}")
                e.printStackTrace()
                println("Error en SQL: ${e.message}") //Avisa error en la terminal
                // Si algo falla (ej. correo duplicado), avisamos del error
                call.respond(HttpStatusCode.InternalServerError, "Error en el servidor: ${e.message}")
            }
        }

        post("/login") {
            try {
                val loginReq = call.receive<UsuarioLogin>()
                println("DEBUG: Intento de login para ${loginReq.correo}")
                //Se ejecuta la búsqueda y sacamos solo el resultado (Si existe o no)
                val existe = newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) {
                    //Ejecutamos si hay alguna fila que coincida
                    Usuarios.selectAll().where {
                        (Usuarios.correo eq loginReq.correo) and (Usuarios.password eq loginReq.password)
                    }.count() > 0 //Si el conteo es mayor a 0, el usuario existe
                }
                if (existe){
                    println("DEGUB: Login exitoso para ${loginReq.correo}")
                    call.respond(HttpStatusCode.OK, "Login correcto")
                }else{
                    println("DEGUB: Credenciales incorrectas")
                    call.respond(HttpStatusCode.Unauthorized, "Correo o Contraseña incorrectos")
                }
            }catch (e: Exception){
                println("ERROR CRÍTICO EN LOGIN: ${e.localizedMessage}")
                e.printStackTrace()
                call.respond(HttpStatusCode.InternalServerError, "Error interno: ${e.message}")
            }

        }

        // Ruta de prueba para saber si el servidor responde
        get("/") {
            call.respondText("Servidor Nexo funcionando!")
        }
    }
}