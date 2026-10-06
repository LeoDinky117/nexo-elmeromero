package com.example

import com.example.com.nexo.app.model.MetaAhorroResponse
import com.example.com.nexo.app.model.Usuario
import com.example.com.nexo.app.model.UsuarioLogin
import com.example.com.nexo.app.model.Movimiento

import db.DatabaseFactory
import db.Movimientos
import db.MetasAhorro
import db.ProgresoMetas

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
                    }.map{it[Usuarios.id]}.singleOrNull() //Aqui se obtiene el ID del usuario o null si no existe
                }
                if (existe != null){
                    println("DEGUB: Login exitoso para ${loginReq.correo}, ID: $existe")
                    call.respond(HttpStatusCode.OK, com.example.com.nexo.app.model.UsuarioResponse(idUsuario = existe))
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

        routing {
            post("/movimientos") {
                try {
                    // 1. Recibimos el objeto Movimiento que viene de la App
                    val datosRecibidos = call.receive<Movimiento>()

                    // 2. Lo insertamos en SQL Server usando nuestra DatabaseFactory
                    DatabaseFactory.dbQuery {
                        Movimientos.insert {
                            it[idUsuario] = datosRecibidos.idUsuario
                            it[idCategoria] = datosRecibidos.idCategoria
                            it[monto] = datosRecibidos.monto.toBigDecimal()
                            it[tipo] = datosRecibidos.tipo
                            // Convertimos el String de la App a LocalDate de Java para la DB
                            it[fecha] = java.time.LocalDate.parse(datosRecibidos.fecha)
                            it[descripcion] = datosRecibidos.descripcion ?: ""
                        }
                    }
                    call.respond(HttpStatusCode.Created, "Movimiento guardado!")
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }

            // 2. ESTA ES LA QUE NECESITAMOS PARA EL SALDO (GET)
            get("/movimientos/usuario/{id}") {
                val idParam = call.parameters["id"]?.toIntOrNull()

                if (idParam == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID de usuario no válido")
                    return@get
                }

                try {
                    // Llamamos a la función que pusimos en verde en DatabaseFactory
                    val lista = DatabaseFactory.obtenerMovimientosPorUsuario(idParam)
                    call.respond(lista)
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }

            //POST PARA MetasAhorro
            // Crea una meta. SQL Server asigna IdMeta, FechaCreacion y Activa usa su default.
            post("/metas") {
                println("DEBUG POST /metas: solicitud recibida")

                try {
                    val solicitud = call.receive<com.example.com.nexo.app.model.MetaAhorro>()
                    println("DEBUG POST /metas: usuario=${solicitud.idUsuario}, meta=${solicitud.nombreMeta}")

                    newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) {
                        MetasAhorro.insert {
                            it[idUsuario] = solicitud.idUsuario
                            it[nombreMeta] = solicitud.nombreMeta
                            it[montoObjetivo] = solicitud.montoObjetivo.toBigDecimal()
                            it[fechaLimite] = java.time.LocalDate.parse(solicitud.fechaLimite)
                            // No se insertan IdMeta, FechaCreacion ni Activa:
                            // SQL Server genera el ID y aplica los valores predeterminados.
                        }
                    }

                    println("DEBUG POST /metas: respuesta 201 Created")
                    call.respond(HttpStatusCode.Created, "Meta creada correctamente")
                } catch (e: Exception) {
                    println("ERROR POST /metas: ${e.message}")
                    e.printStackTrace()
                    println("DEBUG POST /metas: respuesta 500 Internal Server Error")
                    call.respond(HttpStatusCode.InternalServerError, "No se pudo crear la meta: ${e.message}")
                }
            }

// Consulta las metas y suma sus aportaciones para incluir TotalAhorrado.
            get("/metas/usuario/{id}") {
                println("DEBUG GET metas: solicitud recibida")

                val idUsuario = call.parameters["id"]?.toIntOrNull()
                if (idUsuario == null) {
                    println("DEBUG GET metas: ID inválido; respuesta 400 Bad Request")
                    call.respond(HttpStatusCode.BadRequest, "ID de usuario no válido")
                    return@get
                }

                try {
                    // El tipo explícito ayuda a Kotlin a inferir el resultado de la transacción.
                    val metas: List<MetaAhorroResponse> =
                        newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) {
                            // Obtiene las metas del usuario con su fecha límite y estado actual.
                            val metasUsuario: List<MetaAhorroResponse> = MetasAhorro
                                .selectAll()
                                .where { MetasAhorro.idUsuario eq idUsuario }
                                .map { fila ->
                                    MetaAhorroResponse(
                                        idMeta = fila[MetasAhorro.idMeta],
                                        idUsuario = fila[MetasAhorro.idUsuario],
                                        nombreMeta = fila[MetasAhorro.nombreMeta],
                                        montoObjetivo = fila[MetasAhorro.montoObjetivo].toDouble(),
                                        fechaLimite = fila[MetasAhorro.fechaLimite].toString(),
                                        activa = fila[MetasAhorro.activa],
                                        totalAhorrado = 0.0
                                    )
                                }

                            // Agrupa las aportaciones por meta y suma sus montos.
                            val totalesPorMeta: Map<Int, Double> = ProgresoMetas
                                .selectAll()
                                .where { ProgresoMetas.idUsuario eq idUsuario }
                                .map { fila ->
                                    fila[ProgresoMetas.idMeta] to
                                            fila[ProgresoMetas.montoAhorrado].toDouble()
                                }
                                .groupBy({ aporte -> aporte.first }, { aporte -> aporte.second })
                                .mapValues { (_, aportaciones) -> aportaciones.sum() }

                            // Si no hay aportaciones para una meta, su total queda en 0.0.
                            metasUsuario.map { meta ->
                                meta.copy(
                                    totalAhorrado = totalesPorMeta[meta.idMeta] ?: 0.0
                                )
                            }
                        }

                    println("DEBUG GET metas: ${metas.size} meta(s); respuesta 200 OK")
                    call.respond(HttpStatusCode.OK, metas)
                } catch (e: Exception) {
                    println("ERROR GET metas: ${e.message}")
                    e.printStackTrace()
                    println("DEBUG GET metas: respuesta 500 Internal Server Error")
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        "No se pudieron cargar las metas: ${e.message}"
                    )
                }
            }




            get("/perfil/usuario/{id}") {
                val idParam = call.parameters["id"]?.toIntOrNull()
                if (idParam == null) {
                    call.respond(HttpStatusCode.BadRequest, "ID no válido")
                    return@get
                }

                try {
                    val perfil = newSuspendedTransaction(Dispatchers.IO, DatabaseFactory.database) {
                        // Usamos una consulta que traiga los puntos y el nombre del objeto Usuarios
                        Usuarios.select(Usuarios.id, Usuarios.nombre, Usuarios.correo, Usuarios.puntos, Usuarios.edad, Usuarios.fechaRegistro)
                            .where { Usuarios.id eq idParam }
                            .map {
                                // Aquí mapeas a tu PerfilUsuario model
                                com.example.com.nexo.app.model.PerfilUsuario(
                                    idUsuario = it[Usuarios.id],
                                    nombre = it[Usuarios.nombre],
                                    edad = it[Usuarios.edad],
                                    correo = it[Usuarios.correo],
                                    fechaRegistro = it[Usuarios.fechaRegistro]
                                        ?.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                                        ?: "Sin fecha registrada",
                                    puntos = it[Usuarios.puntos]
                                    // balanceTotal se puede calcular o traer de la vista
                                )
                            }.singleOrNull()
                    }

                    if (perfil != null) {
                        call.respond(perfil)
                    } else {
                        call.respond(HttpStatusCode.NotFound, "Usuario no encontrado")
                    }
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.InternalServerError, "Error: ${e.message}")
                }
            }


        } //Aqui termina el Routing jiji
    }
}