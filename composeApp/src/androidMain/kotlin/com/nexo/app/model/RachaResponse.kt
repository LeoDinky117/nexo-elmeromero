package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RachaResponse(@SerialName("Racha") val racha: Int)
