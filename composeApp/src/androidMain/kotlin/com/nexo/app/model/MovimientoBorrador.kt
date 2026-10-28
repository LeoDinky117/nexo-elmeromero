package com.nexo.app.model

import kotlinx.serialization.Serializable

/** Movimiento guardado localmente, todavía pendiente de confirmación y envío al servidor. */
@Serializable
data class MovimientoBorrador(
    val id: String,
    val idUsuario: Int,
    val idCategoria: Int,
    val monto: Double,
    val fecha: String,
    val descripcion: String,
    val tipo: String,
    val transcripcion: String,
    val creadoEn: Long
)
