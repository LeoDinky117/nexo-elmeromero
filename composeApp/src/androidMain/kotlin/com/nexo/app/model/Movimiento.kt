package com.nexo.app.model

import java.util.Date

data class Movimiento(
    val idMovimiento: Int = 0,
    val idUsuario: Int,
    val idCategoria: Int,
    val monto: Double,
    val fecha: Date? = null,
    val descripcion: String?
)