package com.example.com.nexo.app.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class MetaAhorroResponse (
    @SerialName("IdMeta")
    val idMeta: Int,
    @SerialName("IdUsuario")
    val idUsuario: Int,
    @SerialName("NombreMeta")
    val nombreMeta: String,
    @SerialName("MontoObjetivo")
    val montoObjetivo: Double,
    @SerialName("FechaLimite")
    val fechaLimite: String,
    @SerialName("Activa")
    val activa: Boolean,
    @SerialName("TotalAhorrado")
    val totalAhorrado: Double
)
