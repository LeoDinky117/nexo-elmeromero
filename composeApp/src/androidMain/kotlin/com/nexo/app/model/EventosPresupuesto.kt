package com.nexo.app.model

import java.time.LocalDate
import java.time.YearMonth

object EventosPresupuesto {
    fun delMes(items: List<ItemPresupuesto>, mes: YearMonth): List<EventoCalendario> =
        items.filter { item ->
            if (!item.activo) return@filter false
            val inicio = item.mesInicio?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            if (inicio != null && mes < inicio) return@filter false
            val plazo = item.mesesPlazo
            if (inicio != null && plazo != null && mes >= inicio.plusMonths(plazo.toLong())) return@filter false
            true
        }.flatMap { item ->
            val eventos = mutableListOf<EventoCalendario>()
            if (item.flujo == FlujoPresupuesto.INGRESO || item.saldoActual == null || item.saldoActual > 0.0) {
                val primerDia = item.diaPago.coerceAtMost(mes.lengthOfMonth())
                val diasEvento = when (item.periodicidad) {
                    PeriodicidadPresupuesto.DIARIO, PeriodicidadPresupuesto.VARIABLE -> (1..mes.lengthOfMonth()).toList()
                    PeriodicidadPresupuesto.SEMANAL -> generateSequence(primerDia) { it + 7 }.takeWhile { it <= mes.lengthOfMonth() }.toList()
                    PeriodicidadPresupuesto.QUINCENAL -> generateSequence(primerDia) { it + 15 }.takeWhile { it <= mes.lengthOfMonth() }.toList()
                    PeriodicidadPresupuesto.MENSUAL -> listOf(primerDia)
                }
                val montoPorEvento = when (item.periodicidad) {
                    PeriodicidadPresupuesto.DIARIO, PeriodicidadPresupuesto.VARIABLE -> item.montoMensual / mes.lengthOfMonth()
                    PeriodicidadPresupuesto.SEMANAL -> item.montoMensual / (52.0 / 12.0)
                    PeriodicidadPresupuesto.QUINCENAL -> item.montoMensual / 2.0
                    PeriodicidadPresupuesto.MENSUAL -> item.montoMensual
                }
                diasEvento.forEach { dia ->
                    eventos += EventoCalendario(
                        idItem = item.id,
                        fecha = mes.atDay(dia).toString(),
                        concepto = item.concepto,
                        monto = montoPorEvento,
                        tipo = if (item.flujo == FlujoPresupuesto.INGRESO) TipoEventoCalendario.INGRESO else TipoEventoCalendario.PAGO,
                        esencial = item.esencial
                    )
                }
            }
            item.diaCorte?.let { dia ->
                eventos += EventoCalendario(
                    idItem = item.id,
                    fecha = mes.atDay(dia.coerceAtMost(mes.lengthOfMonth())).toString(),
                    concepto = "Corte · ${item.concepto}",
                    monto = 0.0,
                    tipo = TipoEventoCalendario.CORTE,
                    esencial = true
                )
            }
            eventos
        }

    fun proximos(items: List<ItemPresupuesto>, desde: LocalDate = LocalDate.now(), limite: Int = 5): List<EventoCalendario> {
        val mes = YearMonth.from(desde)
        return (0..2).flatMap { delMes(items, mes.plusMonths(it.toLong())) }
            .filter { LocalDate.parse(it.fecha) >= desde }
            .sortedBy { it.fecha }
            .take(limite)
    }
}
