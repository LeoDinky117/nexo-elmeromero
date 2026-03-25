package db


import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date

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
    val monto = decimal("Monto", 10, 2)
    val fecha = date("Fecha")
    val descripcion = varchar("Descripcion", 200)
    val tipo = varchar("Tipo", 20) // "Ingreso" o "Gasto"

    override val primaryKey = PrimaryKey(id)
}