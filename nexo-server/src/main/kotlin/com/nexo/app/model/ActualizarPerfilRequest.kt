package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ActualizarPerfilRequest(
    @SerialName("Nombre") val nombre: String,
    @SerialName("Correo") val correo: String
)

@Serializable
data class AportacionMetaRequest(
    @SerialName("IdUsuario") val idUsuario: Int,
    @SerialName("MontoAhorrado") val montoAhorrado: Double
)
