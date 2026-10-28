package com.nexo.app.ui.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.PresupuestoRepository
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.ConceptoPresupuesto
import com.nexo.app.model.FlujoPresupuesto
import com.nexo.app.model.GrupoPresupuesto
import com.nexo.app.model.ItemPresupuesto
import com.nexo.app.model.Movimiento
import com.nexo.app.model.PeriodicidadPresupuesto
import com.nexo.app.model.ResumenPresupuesto
import com.nexo.app.widgets.CalendarioWidgetProvider
import com.nexo.app.util.RecordatoriosPresupuesto
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.UUID
import java.time.YearMonth
import java.text.Normalizer

class PresupuestoViewModel(
    private val sessionManager: SessionManager,
    private val repository: PresupuestoRepository
) : ViewModel() {
    var userId by mutableStateOf(0)
        private set
    var necesidadesBasicas by mutableStateOf(0.0)
        private set
    var mensaje by mutableStateOf<String?>(null)
        private set

    val items = repository.items

    fun refrescar() {
        if (userId > 0) {
            repository.cargar(userId)
            necesidadesBasicas = repository.necesidadesBasicas(userId)
            RecordatoriosPresupuesto.actualizar(sessionManager.applicationContext, userId, repository.items.value)
        }
    }

    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collectLatest { id ->
                userId = id
                repository.cargar(id)
                necesidadesBasicas = repository.necesidadesBasicas(id)
                RecordatoriosPresupuesto.actualizar(sessionManager.applicationContext, id, repository.items.value)
            }
        }
    }

    fun agregar(
        concepto: ConceptoPresupuesto,
        montoTexto: String,
        diaTexto: String,
        diaCorteTexto: String,
        deudaTexto: String,
        plazoTexto: String,
        periodicidad: PeriodicidadPresupuesto = PeriodicidadPresupuesto.MENSUAL,
        movimientos: List<Movimiento> = emptyList()
    ) {
        val montoCapturado = montoTexto.replace(",", ".").toDoubleOrNull()
        val dia = diaTexto.toIntOrNull()
        val corte = if (concepto.tieneFechaCorte) diaCorteTexto.toIntOrNull() else null
        val saldo = if (concepto.registraDeuda) deudaTexto.replace(",", ".").toDoubleOrNull() else null
        val plazo = if (concepto.requierePlazo) plazoTexto.toIntOrNull() else null
        val montoMensual = when (periodicidad) {
            PeriodicidadPresupuesto.DIARIO -> montoCapturado?.times(YearMonth.now().lengthOfMonth())
            PeriodicidadPresupuesto.SEMANAL -> montoCapturado?.times(52.0 / 12.0)
            PeriodicidadPresupuesto.QUINCENAL -> montoCapturado?.times(2.0)
            PeriodicidadPresupuesto.MENSUAL -> montoCapturado
            PeriodicidadPresupuesto.VARIABLE -> estimarMontoVariable(concepto, movimientos)
        }
        when {
            userId <= 0 -> mensaje = "Inicia sesión para guardar tu presupuesto."
            montoMensual == null || montoMensual <= 0 -> mensaje = if (periodicidad == PeriodicidadPresupuesto.VARIABLE)
                "Aún no hay movimientos de ${concepto.nombre.lowercase()} para calcular un promedio. Registra algunos y vuelve a intentarlo."
            else "Escribe un monto mayor que cero."
            dia == null || dia !in 1..31 -> mensaje = "El día debe estar entre 1 y 31."
            concepto.tieneFechaCorte && (corte == null || corte !in 1..31) -> mensaje = "Indica un día de corte entre 1 y 31."
            concepto.registraDeuda && (saldo == null || saldo < 0) -> mensaje = "Indica el saldo pendiente de la deuda."
            concepto.requierePlazo && (plazo == null || plazo !in 1..120) -> mensaje = "Indica un plazo entre 1 y 120 meses."
            else -> {
                repository.agregar(
                    ItemPresupuesto(
                        id = UUID.randomUUID().toString(),
                        idUsuario = userId,
                        concepto = concepto.nombre,
                        grupo = concepto.grupo,
                        flujo = concepto.flujo,
                        montoMensual = montoMensual,
                        diaPago = dia,
                        diaCorte = corte,
                        saldoInicial = saldo,
                        saldoActual = saldo,
                        mesInicio = if (plazo != null) YearMonth.now().toString() else null,
                        mesesPlazo = plazo,
                        esencial = concepto.esencial,
                        periodicidad = periodicidad
                    )
                )
                CalendarioWidgetProvider.actualizarTodos(sessionManager.applicationContext)
                RecordatoriosPresupuesto.actualizar(sessionManager.applicationContext, userId, items.value)
                mensaje = "Se agregó ${concepto.nombre} al presupuesto."
            }
        }
    }

    private fun estimarMontoVariable(concepto: ConceptoPresupuesto, movimientos: List<Movimiento>): Double? {
        val mes = YearMonth.now()
        val palabrasClave = normalizar(concepto.nombre).split(' ')
            .filter { it.length > 2 && it !in setOf("pago", "gasto", "ingreso", "para", "meses") }
        val delConcepto = movimientos.filter { movimiento ->
            val fecha = movimiento.fecha.take(10).let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() }
            val tipo = normalizar(movimiento.tipo.orEmpty())
            val mismoFlujo = if (concepto.flujo == FlujoPresupuesto.INGRESO) {
                "ingreso" in tipo || "deposito" in tipo || "abono" in tipo
            } else {
                "gasto" in tipo || "pago" in tipo || "egreso" in tipo
            }
            val descripcion = normalizar(movimiento.descripcion.orEmpty())
            val coincideConcepto = palabrasClave.any { it in descripcion }
            fecha != null && YearMonth.from(fecha) == mes && mismoFlujo && coincideConcepto
        }
        if (delConcepto.isEmpty()) return null
        val diasTranscurridos = java.time.LocalDate.now().dayOfMonth.coerceAtLeast(1)
        return delConcepto.sumOf { kotlin.math.abs(it.monto) } / diasTranscurridos * mes.lengthOfMonth()
    }

    fun estimarMontoVariablePublico(concepto: ConceptoPresupuesto, movimientos: List<Movimiento>): Double? =
        estimarMontoVariable(concepto, movimientos)

    private fun normalizar(texto: String): String = Normalizer
        .normalize(texto.lowercase(), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")

    fun eliminar(id: String) {
        items.value.firstOrNull { it.id == id }?.let {
            RecordatoriosPresupuesto.cancelarItem(sessionManager.applicationContext, userId, it)
        }
        repository.eliminar(id)
        CalendarioWidgetProvider.actualizarTodos(sessionManager.applicationContext)
        RecordatoriosPresupuesto.actualizar(sessionManager.applicationContext, userId, items.value)
    }

    fun guardarNecesidadesBasicas(texto: String) {
        val monto = texto.replace(",", ".").toDoubleOrNull()
        if (userId <= 0 || monto == null || monto < 0) {
            mensaje = "Revisa el monto de tus necesidades básicas."
            return
        }
        necesidadesBasicas = monto
        repository.guardarNecesidadesBasicas(userId, monto)
        mensaje = "Se actualizó tu punto de partida."
    }

    fun actualizarSaldoDeuda(id: String, texto: String) {
        val saldo = texto.replace(",", ".").toDoubleOrNull()
        if (saldo == null || saldo < 0) {
            mensaje = "El saldo debe ser cero o mayor."
            return
        }
        repository.actualizarSaldoDeuda(id, saldo)
        CalendarioWidgetProvider.actualizarTodos(sessionManager.applicationContext)
        RecordatoriosPresupuesto.actualizar(sessionManager.applicationContext, userId, items.value)
        mensaje = "Se actualizó el saldo pendiente."
    }

    fun limpiarMensaje() { mensaje = null }

    fun resumen(saldoCuenta: Double): ResumenPresupuesto {
        val mesActual = YearMonth.now()
        val planesVigentes = items.value.filter { item ->
            if (!item.activo) return@filter false
            val inicio = item.mesInicio?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
            if (inicio != null && mesActual < inicio) return@filter false
            val plazo = item.mesesPlazo
            if (inicio != null && plazo != null && mesActual >= inicio.plusMonths(plazo.toLong())) return@filter false
            true
        }
        val activos = planesVigentes.filter {
            it.flujo == FlujoPresupuesto.INGRESO || it.saldoActual == null || it.saldoActual > 0.0
        }
        val ingresos = activos.filter { it.flujo == FlujoPresupuesto.INGRESO }.sumOf { it.montoMensual }
        // El saldo del libro de movimientos representa dinero ya disponible;
        // los ingresos presupuestados representan dinero que todavía se espera recibir.
        val recursosDisponibles = saldoCuenta + ingresos
        val gastos = activos.filter { it.flujo == FlujoPresupuesto.EGRESO }.sumOf { it.montoMensual }
        val esenciales = activos.filter { it.flujo == FlujoPresupuesto.EGRESO && it.esencial }.sumOf { it.montoMensual }
        val inicial = planesVigentes.sumOf { it.saldoInicial ?: 0.0 }
        val actual = planesVigentes.sumOf { it.saldoActual ?: 0.0 }
        // Las necesidades básicas se obtienen de los conceptos esenciales registrados.
        val sueldoMinimo = esenciales
        val progreso = if (inicial > 0.0) ((inicial - actual) / inicial).toFloat().coerceIn(0f, 1f) else 0f
        return ResumenPresupuesto(
            ingresosMensuales = ingresos,
            saldoCuenta = saldoCuenta,
            recursosDisponibles = recursosDisponibles,
            gastosMensuales = gastos,
            necesidadesBasicas = esenciales,
            sueldoMinimoNecesario = sueldoMinimo,
            faltanteParaMinimo = (sueldoMinimo - recursosDisponibles).coerceAtLeast(0.0),
            balanceProyectado = saldoCuenta + ingresos - gastos,
            deudaInicial = inicial,
            deudaActual = actual,
            progresoDeuda = progreso,
            gastosFlexibles = activos.filter { it.flujo == FlujoPresupuesto.EGRESO && !it.esencial }.sumOf { it.montoMensual }
        )
    }
}
