package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProgresoMetaVista(
    @SerialName("IdMeta")
    val idMeta: Int,
    @SerialName("NombreMeta")
    val nombreMeta: String,
    @SerialName("MontoObjetivo")
    val montoObjetivo: Double,
    @SerialName("TotalAhorrado")
    val totalAhorrado: Double,
    @SerialName("FechaLimite")
    val fechaLimite: String,
    @SerialName("FechaCreacion")
    val fechaCreacion: String,
    @SerialName("DiasSinAbonos")
    val diasSinAbonos: Long = 0,
    @SerialName("Activa")
    val activa: Boolean
)
