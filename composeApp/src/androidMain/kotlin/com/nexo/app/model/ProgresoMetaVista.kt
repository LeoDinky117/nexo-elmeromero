package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProgresoMetaVista(
    @SerialName("NombreMeta")
    val nombreMeta: String,
    @SerialName("MontoObjetivo")
    val montoObjetivo: Double,
    @SerialName("TotalAhorrado")
    val totalAhorrado: Double,
    @SerialName("FechaLimite")
    val fechaLimite: String,
    @SerialName("Activa")
    val activa: Boolean
)