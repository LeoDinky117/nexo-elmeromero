package db

//Conexion a SQL Server (JDBC)

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseFactory {
    lateinit var database: Database //Linea agregada
    fun init() {
        try {
            database = Database.connect(createHikariDataSource())
            // Crea las tablas en la BD si aún no existen
            transaction {
                //SchemaUtils.create(Usuarios, Movimientos)
            }
        }catch (e: Exception){
            println("ERROR DE CONEXIÓN: No se pudo conectar a la base de datos...")
            println("Detalle:  ${e.message}")
        }
    }
    private fun createHikariDataSource(): HikariDataSource {
        val config = HikariConfig().apply {
            driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
            // Reemplaza con tus datos reales de DBeaver
            jdbcUrl = "jdbc:sqlserver://localhost:1433;databaseName=Nexo;encrypt=false;trustServerCertificate=true"
            username = "sa"
            password = "Adidas_117"
            maximumPoolSize = 3
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        }
        return HikariDataSource(config)
    }
}