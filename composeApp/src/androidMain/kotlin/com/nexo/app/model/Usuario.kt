package com.nexo.app.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class Usuario(
    @SerialName("IdUsuario")
    val idUsuario: Int = 0,
    @SerialName("Nombre")
    val nombre: String,
    @SerialName("Correo")
    val correo: String,
    @SerialName("Edad")
    val edad: Int,
    @SerialName("Password")
    val password: String,

    @Contextual
    @SerialName("FechaRegistro")
    val fechaRegistro: Instant? = null,
    @SerialName("Puntos")
    val puntos: Int = 0
)