package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Recompensa(
    @SerialName("IdRecompensa") val id: Int,
    @SerialName("Codigo") val codigo: String,
    @SerialName("Nombre") val nombre: String,
    @SerialName("Descripcion") val descripcion: String,
    @SerialName("Tipo") val tipo: String,
    @SerialName("CostoPuntos") val costoPuntos: Int,
    @SerialName("ValorVisual") val valorVisual: String,
    @SerialName("Desbloqueada") val desbloqueada: Boolean = false,
    @SerialName("Equipada") val equipada: Boolean = false
)

@Serializable
data class EstadoRecompensas(
    @SerialName("Puntos") val puntos: Int,
    @SerialName("PuntosDisponibles") val puntosDisponibles: Int,
    @SerialName("TemaActivo") val temaActivo: String,
    @SerialName("IconoActivo") val iconoActivo: String,
    @SerialName("TemaVisualActivo") val temaVisualActivo: String,
    @SerialName("IconoVisualActivo") val iconoVisualActivo: String,
    @SerialName("Recompensas") val recompensas: List<Recompensa>
)

@Serializable
data class SeleccionRecompensa(
    @SerialName("Tipo") val tipo: String,
    @SerialName("Codigo") val codigo: String
)
