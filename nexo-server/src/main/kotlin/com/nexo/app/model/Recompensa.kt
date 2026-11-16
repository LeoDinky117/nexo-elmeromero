package com.example.com.nexo.app.model

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

@Serializable
data class NuevaRecompensa(
    @SerialName("Codigo") val codigo: String,
    @SerialName("Nombre") val nombre: String,
    @SerialName("Descripcion") val descripcion: String = "",
    @SerialName("Tipo") val tipo: String,
    @SerialName("CostoPuntos") val costoPuntos: Int,
    @SerialName("ValorVisual") val valorVisual: String
)

@Serializable
data class MetricasRecompensas(
    @SerialName("Usuarios") val usuarios: Long,
    @SerialName("PremiosActivos") val premiosActivos: Long,
    @SerialName("Desbloqueos") val desbloqueos: Long,
    @SerialName("PuntosAcumulados") val puntosAcumulados: Long,
    @SerialName("UsuariosActivos") val usuariosActivos: Long = 0,
    @SerialName("UsuariosInactivos") val usuariosInactivos: Long = 0,
    @SerialName("MetasCompletadas") val metasCompletadas: Long = 0,
    @SerialName("PromedioDiasMeta") val promedioDiasMeta: Double = 0.0,
    @SerialName("RegistrosMensuales") val registrosMensuales: List<RegistroMensual> = emptyList(),
    @SerialName("Retencion30Dias") val retencion30Dias: Double = 0.0,
    @SerialName("CohorteRetencion30Dias") val cohorteRetencion30Dias: Long = 0,
    @SerialName("UsuariosConRacha") val usuariosConRacha: Long = 0,
    @SerialName("RachaPromedio") val rachaPromedio: Double = 0.0,
    @SerialName("TiempoRespuestaMs") val tiempoRespuestaMs: Double = 0.0
)

@Serializable
data class RegistroMensual(
    @SerialName("Mes") val mes: String,
    @SerialName("Cantidad") val cantidad: Long
)
