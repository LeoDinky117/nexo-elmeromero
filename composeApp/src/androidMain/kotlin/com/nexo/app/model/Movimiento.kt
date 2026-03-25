package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Date

@Serializable
data class Movimientos(
    @SerialName("IdMovimiento")
    val idMovimiento: Int = 0,
    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("IdCategoria")
    val idCategoria: Int,
    @SerialName("Monto")
    val monto: Double,
    @SerialName("Fecha")
    val fecha: String,
    @SerialName("Descripcion")
    val descripcion: String?
)