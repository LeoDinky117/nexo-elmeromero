package db

//Conexion a SQL Server (JDBC)
import com.example.com.nexo.app.model.Movimiento
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {

    lateinit var database: Database //Linea agregada
    suspend fun <T> dbQuery(block: suspend () -> T): T =
        org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction(
            kotlinx.coroutines.Dispatchers.IO,
            db = database
        ) {block()}

    suspend fun obtenerMovimientosPorUsuario(userId: Int): List<Movimiento> = dbQuery{
        Movimientos
            .selectAll()
            .where { Movimientos.idUsuario eq userId }
            .map { row ->
                Movimiento(
                    idMovimiento = row[Movimientos.id],
                    idUsuario = row[Movimientos.idUsuario],
                    idCategoria = row[Movimientos.idCategoria],
                    monto = row[Movimientos.monto].toDouble(), // Convertimos BigDecimal a Double para la API
                    tipo = row[Movimientos.tipo],
                    fecha = row[Movimientos.fecha].toString(), // Convertimos la fecha a String
                    descripcion = row[Movimientos.descripcion]
                )
            }
    }
    fun init() {
        try {
            database = Database.connect(createHikariDataSource())
            // Crea las tablas en la BD si aún no existen
            transaction(database) {
                println("DEBUG: Conectado a: ${connection.metadata { databaseProductVersion }}")
                SchemaUtils.create(Usuarios, Movimientos, CategoriasFinancieras, ActividadDiaria)
                sembrarCategorias()
            }
        }catch (e: Exception){
            println("ERROR DE CONEXIÓN: No se pudo conectar a la base de datos...")
            println("Detalle:  ${e.message}")
        }
    }

    private fun sembrarCategorias() {
        val categorias = listOf(
            "Trabajo" to "Ingreso", "Comida" to "Gasto", "Ocio" to "Gasto",
            "Trans." to "Gasto", "Otros" to "Gasto", "Ventas" to "Ingreso",
            "Freelance" to "Ingreso", "Inversiones" to "Ingreso", "Regalo" to "Ingreso",
            "Otros ingresos" to "Ingreso", "Hogar" to "Gasto", "Servicios" to "Gasto",
            "Salud" to "Gasto", "Educación" to "Gasto", "Compras" to "Gasto",
            "Suscripciones" to "Gasto", "Deudas" to "Gasto", "Viajes" to "Gasto",
            "Mascotas" to "Gasto", "Productos" to "Gasto",
            "Rendimientos" to "Ingreso", "Intereses" to "Ingreso"
        )
        categorias.forEach { (nombreCategoria, tipoCategoria) ->
            val existe = CategoriasFinancieras.selectAll()
                .where { (CategoriasFinancieras.nombre eq nombreCategoria) and (CategoriasFinancieras.tipo eq tipoCategoria) }
                .any()
            if (!existe) {
                CategoriasFinancieras.insert {
                    it[nombre] = nombreCategoria
                    it[tipo] = tipoCategoria
                }
            }
        }
    }
    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {

            //Driver de mariaDB
            driverClassName = "org.mariadb.jdbc.Driver"




            //jdbcUrl de mariaDB
            jdbcUrl = "jdbc:mariadb://localhost:3306/Nexo"
            username = "root"
            password = "root117"

            //conexion con sqlServer
            //driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            //jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=Nexo;encrypt=false;trustServerCertificate=true"
            //username = "sa"
            //password = "Adidas_117"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}
