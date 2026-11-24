package com.example

import com.example.com.nexo.app.model.*
import db.DatabaseFactory
import db.Usuarios
import db.ActividadDiaria
import db.MetasAhorro
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.io.readByteArray
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URI
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import kotlin.system.measureTimeMillis

object CatalogoRecompensas : Table("Recompensas") {
    val id = integer("IdRecompensa").autoIncrement()
    val codigo = varchar("Codigo", 48).uniqueIndex()
    val nombre = varchar("Nombre", 80)
    val descripcion = varchar("Descripcion", 255).default("")
    val tipo = varchar("Tipo", 12)
    val costo = integer("CostoPuntos")
    val valorVisual = varchar("ValorVisual", 96)
    val activa = bool("Activa").default(true)
    val fechaAlta = datetime("FechaAlta").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(id)
}

object RecompensasDesbloqueadas : Table("RecompensasDesbloqueadas") {
    val usuario = integer("IdUsuario").references(Usuarios.id)
    val recompensa = integer("IdRecompensa").references(CatalogoRecompensas.id)
    val fecha = datetime("FechaDesbloqueo").defaultExpression(CurrentDateTime)
    override val primaryKey = PrimaryKey(usuario, recompensa)
}

object SaldoPuntosRecompensas : Table("SaldoPuntosRecompensas") {
    val usuario = integer("IdUsuario").references(Usuarios.id)
    val gastados = integer("PuntosGastados").default(0)
    override val primaryKey = PrimaryKey(usuario)
}

object EstiloUsuario : Table("EstiloUsuario") {
    val usuario = integer("IdUsuario").references(Usuarios.id)
    val tema = varchar("TemaActivo", 48).default("tema_clasico")
    val icono = varchar("IconoActivo", 48).default("icono_avatar")
    override val primaryKey = PrimaryKey(usuario)
}

private val recompensasIniciales = listOf(
    NuevaRecompensa("tema_clasico", "Atardecer", "El estilo original de NEXO.", "TEMA", 0, "#C837AB|#0B1019|#161C28|#FF508E"),
    NuevaRecompensa("tema_oceano", "Océano", "Un acento turquesa para tu espacio.", "TEMA", 100, "#35C6C8|#071820|#102A35|#75E0D3"),
    NuevaRecompensa("tema_bosque", "Bosque", "Verde tranquilo para tus finanzas.", "TEMA", 200, "#52B788|#071A16|#122820|#8BD3A8"),
    NuevaRecompensa("icono_avatar", "Avatar NEXO", "Tu icono original.", "ICONO", 0, "👤"),
    NuevaRecompensa("icono_zorro", "Zorro financiero", "Un nuevo compañero para tu perfil.", "ICONO", 150, "🦊"),
    NuevaRecompensa("icono_lobo", "Lobo NEXO", "Un icono especial para tu constancia.", "ICONO", 300, "🐺")
)

fun crearTablasRecompensas() = transaction(DatabaseFactory.database) {
    SchemaUtils.create(CatalogoRecompensas, RecompensasDesbloqueadas, EstiloUsuario, SaldoPuntosRecompensas)
    recompensasIniciales.forEach { premio ->
        if (CatalogoRecompensas.selectAll().where { CatalogoRecompensas.codigo eq premio.codigo }.empty()) {
            CatalogoRecompensas.insert {
                it[codigo] = premio.codigo
                it[nombre] = premio.nombre
                it[descripcion] = premio.descripcion
                it[tipo] = premio.tipo
                it[costo] = premio.costoPuntos
                it[valorVisual] = premio.valorVisual
                it[activa] = true
            }
        }
    }
}

internal fun validarPremio(p: NuevaRecompensa) {
    require(p.codigo.matches(Regex("[a-z][a-z0-9_]{1,47}"))) { "El código debe usar minúsculas, números y guion bajo." }
    require(p.nombre.trim().length in 1..80) { "El nombre debe tener entre 1 y 80 caracteres." }
    require(p.descripcion.length <= 255) { "La descripción no puede superar 255 caracteres." }
    require(p.tipo in setOf("TEMA", "ICONO")) { "El tipo debe ser TEMA o ICONO." }
    require(p.costoPuntos in 0..100000) { "El costo debe estar entre 0 y 100000 puntos." }
    if (p.tipo == "TEMA") {
        require(p.valorVisual.split('|').size == 4 && p.valorVisual.split('|').all { it.matches(Regex("#[0-9A-Fa-f]{6}")) }) {
            "Un tema requiere cuatro colores hexadecimales separados por |."
        }
    } else {
        val esImagen = Regex("^/admin/icons/[0-9a-f-]{36}\\.(png|jpg|webp)$", RegexOption.IGNORE_CASE).matches(p.valorVisual)
        require(esImagen || (p.valorVisual.isNotBlank() && p.valorVisual.length <= 12)) {
            "El icono debe ser un emoji o una imagen importada desde el panel."
        }
    }
}

private const val TAMANO_MAXIMO_ICONO = 2 * 1024 * 1024

private fun directorioIconosAdmin(): File {
    val configurado = System.getenv("NEXO_ADMIN_ICON_DIR")?.trim()?.takeIf(String::isNotEmpty)
    val ruta = configurado ?: "${System.getProperty("user.home")}/.nexo/admin-icons"
    return File(ruta).canonicalFile
}

internal fun archivoIconoAdmin(nombre: String): File? {
    if (!Regex("^[0-9a-f-]{36}\\.(png|jpg|webp)$", RegexOption.IGNORE_CASE).matches(nombre)) return null
    return runCatching {
        val dir = directorioIconosAdmin()
        val archivo = File(dir, nombre).canonicalFile
        archivo.takeIf { it.parentFile == dir && it.isFile }
    }.getOrNull()
}

private fun extensionImagenValida(bytes: ByteArray): String? = when {
    bytes.size >= 8 && bytes.take(8).map { it.toInt() and 0xff } == listOf(137, 80, 78, 71, 13, 10, 26, 10) -> "png"
    bytes.size >= 3 && bytes[0] == 0xff.toByte() && bytes[1] == 0xd8.toByte() && bytes[2] == 0xff.toByte() -> "jpg"
    bytes.size >= 12 && bytes.decodeToString(0, 4) == "RIFF" && bytes.decodeToString(8, 12) == "WEBP" -> "webp"
    else -> null
}

private fun guardarIconoAdmin(bytes: ByteArray): String {
    require(bytes.size in 1..TAMANO_MAXIMO_ICONO) { "La imagen debe pesar como máximo 2 MB." }
    val extension = extensionImagenValida(bytes)
        ?: throw IllegalArgumentException("El archivo no es un PNG, JPG/JPEG o WebP válido.")
    val directorio = directorioIconosAdmin()
    if (!directorio.isDirectory && !directorio.mkdirs()) {
        throw IOException("No se pudo crear el directorio para los iconos.")
    }
    val nombre = "${UUID.randomUUID()}.$extension"
    val archivo = File(directorio, nombre)
    if (!archivo.createNewFile()) throw IOException("No se pudo reservar el archivo del icono.")
    try {
        archivo.writeBytes(bytes)
    } catch (error: Exception) {
        archivo.delete()
        throw error
    }
    return "/admin/icons/$nombre"
}

private const val COOKIE_SESION_ADMIN = "nexo_admin_session"
private const val DURACION_SESION_ADMIN_MS = 30L * 60L * 1000L
private val sesionesAdmin = ConcurrentHashMap<String, Long>()
private val randomAdmin = SecureRandom()

private fun origenAdminConfigurado(): String? {
    val origen = System.getenv("NEXO_ADMIN_PUBLIC_ORIGIN")?.trim()?.trimEnd('/') ?: return null
    val uri = runCatching { URI(origen) }.getOrNull() ?: return null
    val localHttp = uri.scheme == "http" && uri.host in setOf("localhost", "127.0.0.1", "::1")
    val https = uri.scheme == "https"
    if ((!https && !localHttp) || uri.rawPath.orEmpty().isNotEmpty() || uri.rawQuery != null || uri.rawFragment != null || uri.userInfo != null) return null
    return origen
}

private fun ApplicationCall.origenAdminValido(): Boolean {
    val esperado = origenAdminConfigurado()
    val recibido = request.headers["Origin"]?.trim()?.trimEnd('/')
    val valido = esperado != null && recibido != null && MessageDigest.isEqual(esperado.toByteArray(), recibido.toByteArray())
    if (!valido) {
        application.log.warn(
            "Origen administrativo rechazado: recibido='{}', configurado='{}'",
            recibido ?: "(ausente)",
            esperado ?: "(no configurado o no válido)"
        )
    }
    return valido
}

private fun nuevaSesionAdmin(): String {
    val bytes = ByteArray(32)
    randomAdmin.nextBytes(bytes)
    return bytes.joinToString(separator = "") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

private fun ApplicationCall.establecerCookieAdmin(token: String, maxAge: Int) {
    val secure = origenAdminConfigurado()?.startsWith("https://") == true
    val secureFlag = if (secure) "; Secure" else ""
    response.headers.append(
        "Set-Cookie",
        "$COOKIE_SESION_ADMIN=$token; Path=/admin; HttpOnly; SameSite=Strict; Max-Age=$maxAge$secureFlag"
    )
    response.headers.append("Cache-Control", "no-store")
    response.headers.append("X-Content-Type-Options", "nosniff")
}

private suspend fun ApplicationCall.esAdministrador(): Boolean {
    val token = request.cookies[COOKIE_SESION_ADMIN]
    val vence = token?.let(sesionesAdmin::get)
    val ahora = System.currentTimeMillis()
    val valido = token != null && vence != null && vence > ahora
    sesionesAdmin.entries.removeIf { it.value <= ahora }
    if (!valido) {
        if (token != null) sesionesAdmin.remove(token)
        establecerCookieAdmin("", 0)
        respond(HttpStatusCode.Unauthorized, mapOf("mensaje" to "La sesión administrativa venció. Vuelve a iniciar sesión."))
    }
    return valido
}

private fun leerEstadoRecompensas(usuarioId: Int): EstadoRecompensas {
    val puntos = Usuarios.selectAll().where { Usuarios.id eq usuarioId }.single()[Usuarios.puntos]
    val desbloqueadas = RecompensasDesbloqueadas.selectAll()
        .where { RecompensasDesbloqueadas.usuario eq usuarioId }
        .map { it[RecompensasDesbloqueadas.recompensa] }.toSet()
    val estilo = EstiloUsuario.selectAll().where { EstiloUsuario.usuario eq usuarioId }.singleOrNull()
    val gastados = SaldoPuntosRecompensas.selectAll().where { SaldoPuntosRecompensas.usuario eq usuarioId }.singleOrNull()?.get(SaldoPuntosRecompensas.gastados) ?: 0
    val tema = estilo?.get(EstiloUsuario.tema) ?: "tema_clasico"
    val icono = estilo?.get(EstiloUsuario.icono) ?: "icono_avatar"
    val temas = CatalogoRecompensas.selectAll().where { (CatalogoRecompensas.tipo eq "TEMA") and (CatalogoRecompensas.activa eq true) }.toList()
    val iconos = CatalogoRecompensas.selectAll().where { (CatalogoRecompensas.tipo eq "ICONO") and (CatalogoRecompensas.activa eq true) }.toList()
    val filas = temas + iconos
    val visualTema = filas.firstOrNull { it[CatalogoRecompensas.codigo] == tema }?.get(CatalogoRecompensas.valorVisual) ?: recompensasIniciales.first().valorVisual
    val visualIcono = filas.firstOrNull { it[CatalogoRecompensas.codigo] == icono }?.get(CatalogoRecompensas.valorVisual) ?: "👤"
    val articulos = filas.map { row ->
        val id = row[CatalogoRecompensas.id]
        val codigo = row[CatalogoRecompensas.codigo]
        Recompensa(
            id = id,
            codigo = codigo,
            nombre = row[CatalogoRecompensas.nombre],
            descripcion = row[CatalogoRecompensas.descripcion],
            tipo = row[CatalogoRecompensas.tipo],
            costoPuntos = row[CatalogoRecompensas.costo],
            valorVisual = row[CatalogoRecompensas.valorVisual],
            desbloqueada = id in desbloqueadas || row[CatalogoRecompensas.costo] == 0,
            equipada = codigo == if (row[CatalogoRecompensas.tipo] == "TEMA") tema else icono
        )
    }
    return EstadoRecompensas(puntos, (puntos - gastados).coerceAtLeast(0), tema, icono, visualTema, visualIcono, articulos)
}

fun Application.configureRecompensas() {
    routing {
        route("/admin/sesion") {
            post {
                if (!call.origenAdminValido()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("mensaje" to "Origen no autorizado."))
                    return@post
                }
                val esperado = System.getenv("NEXO_ADMIN_API_KEY")?.takeIf { it.length >= 32 }
                if (esperado == null) {
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("mensaje" to "El acceso administrativo no está configurado correctamente en el servidor."))
                    return@post
                }
                val body = runCatching { call.receive<Map<String, String>>() }.getOrNull()
                val recibido = body?.get("clave")
                if (recibido == null || !MessageDigest.isEqual(esperado.toByteArray(), recibido.toByteArray())) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("mensaje" to "Clave administrativa incorrecta."))
                    return@post
                }
                val ahora = System.currentTimeMillis()
                sesionesAdmin.entries.removeIf { it.value <= ahora }
                val token = nuevaSesionAdmin()
                sesionesAdmin[token] = ahora + DURACION_SESION_ADMIN_MS
                call.establecerCookieAdmin(token, 1800)
                call.respond(HttpStatusCode.OK, mapOf("mensaje" to "Sesión administrativa iniciada."))
            }
            get {
                if (!call.esAdministrador()) return@get
                call.response.headers.append("Cache-Control", "no-store")
                call.respond(HttpStatusCode.OK, mapOf("autenticado" to true))
            }
            post("/salir") {
                if (!call.origenAdminValido()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("mensaje" to "Origen no autorizado."))
                    return@post
                }
                call.request.cookies[COOKIE_SESION_ADMIN]?.let(sesionesAdmin::remove)
                call.establecerCookieAdmin("", 0)
                call.respond(HttpStatusCode.OK, mapOf("mensaje" to "Sesión finalizada."))
            }
        }
        route("/recompensas") {
            get("/usuario/{id}") {
                val usuarioId = call.parameters["id"]?.toIntOrNull()
                if (usuarioId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("mensaje" to "ID de usuario no válido.")); return@get }
                try { call.respond(DatabaseFactory.dbQuery { leerEstadoRecompensas(usuarioId) }) }
                catch (_: NoSuchElementException) { call.respond(HttpStatusCode.NotFound, mapOf("mensaje" to "No se encontró la cuenta.")) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable, mapOf("mensaje" to "No se pudieron cargar las recompensas.")) }
            }
            post("/usuario/{id}/desbloquear/{premioId}") {
                val uid = call.parameters["id"]?.toIntOrNull()
                val premioId = call.parameters["premioId"]?.toIntOrNull()
                if (uid == null || premioId == null) { call.respond(HttpStatusCode.BadRequest, mapOf("mensaje" to "Datos de recompensa no válidos.")); return@post }
                try {
                    val estado = DatabaseFactory.dbQuery {
                        val premio = CatalogoRecompensas.selectAll().where { (CatalogoRecompensas.id eq premioId) and (CatalogoRecompensas.activa eq true) }.singleOrNull()
                            ?: throw NoSuchElementException("No encontramos ese premio.")
                        val costo = premio[CatalogoRecompensas.costo]
                        if (costo > 0) {
                            require(RecompensasDesbloqueadas.selectAll().where { (RecompensasDesbloqueadas.usuario eq uid) and (RecompensasDesbloqueadas.recompensa eq premioId) }.empty()) { "Ya desbloqueaste este premio." }
                            val total = Usuarios.selectAll().where { Usuarios.id eq uid }.singleOrNull()?.get(Usuarios.puntos)
                                ?: throw NoSuchElementException("No se encontró la cuenta.")
                            if (SaldoPuntosRecompensas.selectAll().where { SaldoPuntosRecompensas.usuario eq uid }.empty()) {
                                SaldoPuntosRecompensas.insert { it[usuario] = uid; it[gastados] = 0 }
                            }
                            val descontados = SaldoPuntosRecompensas.update({ (SaldoPuntosRecompensas.usuario eq uid) and (SaldoPuntosRecompensas.gastados lessEq (total - costo)) }) {
                                it.update(SaldoPuntosRecompensas.gastados, SaldoPuntosRecompensas.gastados + costo)
                            }
                            require(descontados == 1) { "No tienes puntos suficientes para este premio." }
                            RecompensasDesbloqueadas.insert { it[usuario] = uid; it[recompensa] = premioId }
                        }
                        leerEstadoRecompensas(uid)
                    }
                    call.respond(estado)
                } catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.Conflict, mapOf("mensaje" to (e.message ?: "No se pudo desbloquear."))) }
                catch (_: NoSuchElementException) { call.respond(HttpStatusCode.NotFound, mapOf("mensaje" to "No se encontró el usuario o el premio.")) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable, mapOf("mensaje" to "No se pudo guardar el desbloqueo.")) }
            }
            post("/usuario/{id}/equipar") {
                val uid = call.parameters["id"]?.toIntOrNull()
                if (uid == null) { call.respond(HttpStatusCode.BadRequest, mapOf("mensaje" to "ID de usuario no válido.")); return@post }
                try {
                    val seleccion = call.receive<SeleccionRecompensa>()
                    require(seleccion.tipo in setOf("TEMA", "ICONO")) { "Tipo de premio no válido." }
                    val estado = DatabaseFactory.dbQuery {
                        val item = CatalogoRecompensas.selectAll().where { (CatalogoRecompensas.codigo eq seleccion.codigo) and (CatalogoRecompensas.tipo eq seleccion.tipo) and (CatalogoRecompensas.activa eq true) }.singleOrNull()
                            ?: throw NoSuchElementException()
                        val id = item[CatalogoRecompensas.id]
                        if (item[CatalogoRecompensas.costo] > 0) require(!RecompensasDesbloqueadas.selectAll().where { (RecompensasDesbloqueadas.usuario eq uid) and (RecompensasDesbloqueadas.recompensa eq id) }.empty()) { "Desbloquea el premio antes de equiparlo." }
                        if (EstiloUsuario.selectAll().where { EstiloUsuario.usuario eq uid }.empty()) EstiloUsuario.insert { it[usuario] = uid }
                        EstiloUsuario.update({ EstiloUsuario.usuario eq uid }) { row -> if (seleccion.tipo == "TEMA") row[tema] = seleccion.codigo else row[icono] = seleccion.codigo }
                        leerEstadoRecompensas(uid)
                    }
                    call.respond(estado)
                } catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.Conflict, mapOf("mensaje" to (e.message ?: "No se pudo equipar."))) }
                catch (_: NoSuchElementException) { call.respond(HttpStatusCode.NotFound, mapOf("mensaje" to "No se encontró el premio o el usuario.")) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.ServiceUnavailable, mapOf("mensaje" to "No se pudo guardar el estilo.")) }
            }
        }
        route("/admin/recompensas") {
            get {
                if (!call.esAdministrador()) return@get
                val catalogo = DatabaseFactory.dbQuery {
                    CatalogoRecompensas.selectAll().orderBy(CatalogoRecompensas.id to SortOrder.ASC).map { row ->
                        Recompensa(row[CatalogoRecompensas.id], row[CatalogoRecompensas.codigo], row[CatalogoRecompensas.nombre], row[CatalogoRecompensas.descripcion], row[CatalogoRecompensas.tipo], row[CatalogoRecompensas.costo], row[CatalogoRecompensas.valorVisual])
                    }
                }
                call.respond(catalogo)
            }
            post {
                if (!call.esAdministrador()) return@post
                if (!call.origenAdminValido()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("mensaje" to "Origen no autorizado."))
                    return@post
                }
                try {
                    val p = call.receive<NuevaRecompensa>()
                    validarPremio(p)
                    DatabaseFactory.dbQuery {
                        require(CatalogoRecompensas.selectAll().where { CatalogoRecompensas.codigo eq p.codigo }.empty()) { "Ya existe un premio con ese código." }
                        CatalogoRecompensas.insert { it[codigo] = p.codigo; it[nombre] = p.nombre.trim(); it[descripcion] = p.descripcion.trim(); it[tipo] = p.tipo; it[costo] = p.costoPuntos; it[valorVisual] = p.valorVisual; it[activa] = true }
                    }
                    call.respond(HttpStatusCode.Created, mapOf("mensaje" to "Premio agregado al catálogo."))
                } catch (e: IllegalArgumentException) { call.respond(HttpStatusCode.BadRequest, mapOf("mensaje" to (e.message ?: "Datos no válidos."))) }
                catch (e: Exception) { if (e is kotlinx.coroutines.CancellationException) throw e; call.respond(HttpStatusCode.Conflict, mapOf("mensaje" to "No se pudo agregar el premio.")) }
            }
            post("/iconos") {
                if (!call.esAdministrador()) return@post
                if (!call.origenAdminValido()) {
                    call.respond(HttpStatusCode.Forbidden, mapOf("mensaje" to "Origen no autorizado."))
                    return@post
                }
                try {
                    var imagen: ByteArray? = null
                    val multipart = call.receiveMultipart(formFieldLimit = 16 * 1024)
                    multipart.forEachPart { parte ->
                        try {
                            if (parte is PartData.FileItem && parte.name == "icono" && imagen == null) {
                                val contenido = parte.provider().readRemaining(TAMANO_MAXIMO_ICONO + 1L).readByteArray()
                                require(contenido.size <= TAMANO_MAXIMO_ICONO) { "La imagen debe pesar como máximo 2 MB." }
                                imagen = contenido
                            }
                        } finally {
                            parte.dispose()
                        }
                    }
                    val bytes = imagen ?: throw IllegalArgumentException("Selecciona una imagen para subir.")
                    call.respond(HttpStatusCode.Created, mapOf("ruta" to guardarIconoAdmin(bytes)))
                } catch (e: IllegalArgumentException) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("mensaje" to (e.message ?: "Imagen no válida.")))
                } catch (e: Exception) {
                    if (e is kotlinx.coroutines.CancellationException) throw e
                    call.application.log.warn("No se pudo guardar la imagen del premio", e)
                    call.respond(HttpStatusCode.ServiceUnavailable, mapOf("mensaje" to "No se pudo guardar la imagen."))
                }
            }
            get("/metricas") {
                if (!call.esAdministrador()) return@get
                lateinit var metricas: MetricasRecompensas
                val tiempo = measureTimeMillis {
                    metricas = DatabaseFactory.dbQuery {
                        val hoy = LocalDate.now()
                        // Leer solo las columnas agregadas necesarias; no cargar correos ni contraseñas al panel.
                        val usuarios = Usuarios.select(Usuarios.id, Usuarios.fechaRegistro, Usuarios.puntos).toList()
                        val fechasActividad = ActividadDiaria.selectAll().map {
                            it[ActividadDiaria.idUsuario] to it[ActividadDiaria.fecha]
                        }.groupBy({ it.first }, { it.second })
                        val activosDesde = hoy.minusDays(29)
                        // Actividad diaria registrada al usar la app; limita el conteo a cuentas existentes.
                        val activos = usuarios.count { usuario ->
                            val idUsuario = usuario[Usuarios.id]
                            fechasActividad[idUsuario].orEmpty().any { fecha -> fecha in activosDesde..hoy }
                        }
                        val metas = MetasAhorro.select(MetasAhorro.fechaCreacion, MetasAhorro.fechaCumplimiento).toList()
                        val metasCompletadas = metas.mapNotNull { fila ->
                            val creacion = fila[MetasAhorro.fechaCreacion].toLocalDate()
                            fila[MetasAhorro.fechaCumplimiento]?.let { ChronoUnit.DAYS.between(creacion, it.toLocalDate()).coerceAtLeast(0) }
                        }
                        val inicioCohorte = hoy.minusDays(60)
                        val finCohorte = hoy.minusDays(30)
                        val cohorte = usuarios.filter {
                            val registro = it[Usuarios.fechaRegistro].toLocalDate()
                            registro in inicioCohorte..finCohorte
                        }
                        val retenidos = cohorte.count { usuario ->
                            val registro = usuario[Usuarios.fechaRegistro].toLocalDate()
                            fechasActividad[usuario[Usuarios.id]].orEmpty().any { fecha -> fecha in registro.plusDays(30)..hoy }
                        }
                        val mesActual = YearMonth.from(hoy)
                        val registrosMensuales = (5L downTo 0L).map { offset ->
                            val mes = mesActual.minusMonths(offset)
                            RegistroMensual(
                                mes = mes.toString(),
                                cantidad = usuarios.count { YearMonth.from(it[Usuarios.fechaRegistro]) == mes }.toLong()
                            )
                        }
                        val rachasActivas = fechasActividad.values.mapNotNull { fechas ->
                            val ordenadas = fechas.distinct().sortedDescending()
                            var esperado = if (hoy in ordenadas) hoy else hoy.minusDays(1)
                            var racha = 0
                            for (fecha in ordenadas) {
                                if (fecha != esperado) break
                                racha++
                                esperado = esperado.minusDays(1)
                            }
                            racha.takeIf { it > 0 }
                        }
                        MetricasRecompensas(
                            usuarios = usuarios.size.toLong(),
                            premiosActivos = CatalogoRecompensas.selectAll().where { CatalogoRecompensas.activa eq true }.count(),
                            desbloqueos = RecompensasDesbloqueadas.selectAll().count(),
                            puntosAcumulados = usuarios.sumOf { it[Usuarios.puntos].toLong() },
                            usuariosActivos = activos.toLong(),
                            usuariosInactivos = (usuarios.size - activos).toLong(),
                            metasCompletadas = metasCompletadas.size.toLong(),
                            promedioDiasMeta = metasCompletadas.average().takeIf { it.isFinite() } ?: 0.0,
                            registrosMensuales = registrosMensuales,
                            retencion30Dias = if (cohorte.isEmpty()) 0.0 else retenidos * 100.0 / cohorte.size,
                            cohorteRetencion30Dias = cohorte.size.toLong(),
                            usuariosConRacha = rachasActivas.size.toLong(),
                            rachaPromedio = rachasActivas.average().takeIf { it.isFinite() } ?: 0.0
                        )
                    }
                }
                call.respond(metricas.copy(tiempoRespuestaMs = tiempo.toDouble()))
            }
        }
    }
}
