package com.example

import com.example.com.nexo.app.model.*
import db.DatabaseFactory
import db.Usuarios
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.lessEq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.plus
import org.jetbrains.exposed.sql.javatime.CurrentDateTime
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import java.security.MessageDigest

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
        require(p.valorVisual.isNotBlank() && p.valorVisual.length <= 12) { "El icono debe ser un emoji o símbolo de hasta 12 caracteres." }
    }
}

private suspend fun ApplicationCall.esAdministrador(): Boolean {
    val esperado = System.getenv("NEXO_ADMIN_API_KEY")?.takeIf { it.isNotBlank() }
    val recibido = request.headers["X-Admin-Key"]
    val valido = esperado != null && recibido != null && MessageDigest.isEqual(esperado.toByteArray(), recibido.toByteArray())
    if (!valido) respond(HttpStatusCode.Unauthorized, mapOf("mensaje" to "Acceso administrativo no autorizado."))
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
            get("/metricas") {
                if (!call.esAdministrador()) return@get
                val metricas = DatabaseFactory.dbQuery {
                    MetricasRecompensas(
                        usuarios = Usuarios.selectAll().count(),
                        premiosActivos = CatalogoRecompensas.selectAll().where { CatalogoRecompensas.activa eq true }.count(),
                        desbloqueos = RecompensasDesbloqueadas.selectAll().count(),
                        puntosAcumulados = Usuarios.selectAll().sumOf { it[Usuarios.puntos].toLong() }
                    )
                }
                call.respond(metricas)
            }
        }
    }
}
