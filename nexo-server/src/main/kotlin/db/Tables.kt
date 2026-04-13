package db


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

//import org.jetbrains.exposed.sql.javatime.date

// Tabla Usuarios
object Usuarios : Table("Usuarios") {
    val id = integer("IdUsuario").autoIncrement()

    val nombre = varchar("Nombre", 100)
    val edad = integer("Edad")
    val correo = varchar("Correo", 100).uniqueIndex()
    val password = varchar("Contrasena", 100)
    //val edad1 = integer("Edad").nullable()

    // El error 'primaryKey overrides nothing' se quita usando override val
    override val primaryKey = PrimaryKey(id)
}

//Tabla Movimiento
object Movimientos : Table("Movimientos") {
    val id = integer("IdMovimiento").autoIncrement()
    val idUsuario = integer("IdUsuario") references Usuarios.id
    val idCategoria = integer("IdCategoria") // Ingreso o Gasto
    val monto = decimal("Monto", 18, 2)
    val tipo = varchar("Tipo", 30)
    val fecha = date("Fecha")
    val descripcion = varchar("Descripcion", 200)

    override val primaryKey = PrimaryKey(id)
}