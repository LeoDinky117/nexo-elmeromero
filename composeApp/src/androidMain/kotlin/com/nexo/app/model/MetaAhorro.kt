package com.nexo.app.model

import java.util.Date

data class MetaAhorro(
    val idMeta: Int = 0,
    val idUsuario: Int,
    val nombreMeta: String,
    val montoObjetivo: Double,
    val fechaLimite: Date,
    val activa: Boolean = true
)