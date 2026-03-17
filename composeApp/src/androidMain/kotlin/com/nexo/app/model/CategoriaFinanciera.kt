package com.nexo.app.model

data class CategoriaFinanciera(
    val idCategoria: Int = 0,
    val nombre: String,
    val tipo: String // "Ingreso" o "Gasto"
)