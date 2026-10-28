package com.nexo.app.model

import kotlinx.serialization.Serializable

@Serializable
enum class FlujoPresupuesto { INGRESO, EGRESO }

@Serializable
enum class GrupoPresupuesto { INGRESO, BASICO, DEUDA, SUSCRIPCION, TANDA, DIFERIDO, OTRO }

@Serializable
enum class PeriodicidadPresupuesto { DIARIO, SEMANAL, QUINCENAL, MENSUAL, VARIABLE }

/** Compromiso o percepción mensual que alimenta el presupuesto y el calendario. */
@Serializable
data class ItemPresupuesto(
    val id: String,
    val idUsuario: Int,
    val concepto: String,
    val grupo: GrupoPresupuesto,
    val flujo: FlujoPresupuesto,
    val montoMensual: Double,
    val diaPago: Int,
    val diaCorte: Int? = null,
    val saldoInicial: Double? = null,
    val saldoActual: Double? = null,
    val mesInicio: String? = null,
    val mesesPlazo: Int? = null,
    val esencial: Boolean = false,
    val activo: Boolean = true,
    val periodicidad: PeriodicidadPresupuesto = PeriodicidadPresupuesto.MENSUAL
)

enum class TipoEventoCalendario { INGRESO, PAGO, CORTE }

data class EventoCalendario(
    val idItem: String,
    val fecha: String,
    val concepto: String,
    val monto: Double,
    val tipo: TipoEventoCalendario,
    val esencial: Boolean
)

data class ResumenPresupuesto(
    val ingresosMensuales: Double,
    val saldoCuenta: Double,
    val recursosDisponibles: Double,
    val gastosMensuales: Double,
    val necesidadesBasicas: Double,
    val sueldoMinimoNecesario: Double,
    val faltanteParaMinimo: Double,
    val balanceProyectado: Double,
    val deudaInicial: Double,
    val deudaActual: Double,
    val progresoDeuda: Float,
    val gastosFlexibles: Double
)

data class ConceptoPresupuesto(
    val nombre: String,
    val grupo: GrupoPresupuesto,
    val flujo: FlujoPresupuesto,
    val esencial: Boolean = false,
    val tieneFechaCorte: Boolean = false,
    val registraDeuda: Boolean = false,
    val requierePlazo: Boolean = false
)

object CatalogoConceptosPresupuesto {
    val conceptos = listOf(
        ConceptoPresupuesto("Sueldo", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO, esencial = true),
        ConceptoPresupuesto("Ingreso extra", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO),
        ConceptoPresupuesto("Freelance", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO),
        ConceptoPresupuesto("Ventas", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO),
        ConceptoPresupuesto("Pensión o apoyo", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO),
        ConceptoPresupuesto("Tanda recibida", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO),
        ConceptoPresupuesto("Renta", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Hipoteca", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Luz", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Agua", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Gas", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Internet y teléfono", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Despensa", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Transporte", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Salud y medicinas", GrupoPresupuesto.BASICO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Tarjeta de crédito", GrupoPresupuesto.DEUDA, FlujoPresupuesto.EGRESO, esencial = true, tieneFechaCorte = true, registraDeuda = true),
        ConceptoPresupuesto("Préstamo", GrupoPresupuesto.DEUDA, FlujoPresupuesto.EGRESO, esencial = true, registraDeuda = true),
        ConceptoPresupuesto("Crédito automotriz", GrupoPresupuesto.DEUDA, FlujoPresupuesto.EGRESO, esencial = true, registraDeuda = true),
        ConceptoPresupuesto("Tanda", GrupoPresupuesto.TANDA, FlujoPresupuesto.EGRESO),
        ConceptoPresupuesto("Suscripción", GrupoPresupuesto.SUSCRIPCION, FlujoPresupuesto.EGRESO),
        ConceptoPresupuesto("Pago diferido a meses", GrupoPresupuesto.DIFERIDO, FlujoPresupuesto.EGRESO, esencial = true, registraDeuda = true, requierePlazo = true),
        ConceptoPresupuesto("Gastos hormiga", GrupoPresupuesto.OTRO, FlujoPresupuesto.EGRESO),
        ConceptoPresupuesto("Seguro", GrupoPresupuesto.OTRO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Colegiatura", GrupoPresupuesto.OTRO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Impuestos", GrupoPresupuesto.OTRO, FlujoPresupuesto.EGRESO, esencial = true),
        ConceptoPresupuesto("Otro gasto", GrupoPresupuesto.OTRO, FlujoPresupuesto.EGRESO),
        ConceptoPresupuesto("Otro ingreso", GrupoPresupuesto.INGRESO, FlujoPresupuesto.INGRESO)
    )
}
