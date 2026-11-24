package com.nexo.app.model

import java.time.LocalDate

enum class PeriodicidadAportacionMeta(val etiqueta: String) {
    SEMANAL("Semanal"),
    QUINCENAL("Quincenal"),
    MENSUAL("Mensual");

    fun anterior(fecha: LocalDate): LocalDate = when (this) {
        SEMANAL -> fecha.minusWeeks(1)
        QUINCENAL -> fecha.minusWeeks(2)
        MENSUAL -> fecha.minusMonths(1)
    }

    fun siguiente(fecha: LocalDate): LocalDate = when (this) {
        SEMANAL -> fecha.plusWeeks(1)
        QUINCENAL -> fecha.plusWeeks(2)
        MENSUAL -> fecha.plusMonths(1)
    }
}
