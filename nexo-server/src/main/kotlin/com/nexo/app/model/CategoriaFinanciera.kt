package com.example.com.nexo.app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoriaFinanciera(
    @SerialName("IdCategoria") val idCategoria: Int,
    @SerialName("Nombre") val nombre: String,
    @SerialName("Tipo") val tipo: String
)
