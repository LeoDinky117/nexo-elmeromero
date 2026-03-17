package com.nexo.app.model

import java.util.Date

data class ProgresoMeta(
    val idProgreso: Int = 0,
    val idMeta: Int,
    val montoAhorrado: Double,
    val fechaRegistro: Date? = null
)