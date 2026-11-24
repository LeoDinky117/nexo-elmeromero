package com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MetaCreadaResponse(
    @SerialName("IdMeta") val idMeta: Int
)
