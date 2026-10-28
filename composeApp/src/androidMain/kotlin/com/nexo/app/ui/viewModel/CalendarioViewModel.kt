package com.nexo.app.ui.viewModel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexo.app.data.local.PresupuestoRepository
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.EventoCalendario
import com.nexo.app.model.EventosPresupuesto
import com.nexo.app.model.ItemPresupuesto
import com.nexo.app.model.TipoEventoCalendario
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

class CalendarioViewModel(
    sessionManager: SessionManager,
    private val repository: PresupuestoRepository
) : ViewModel() {
    private var userId = 0
    var mesVisible by mutableStateOf(YearMonth.now())
        private set
    var fechaSeleccionada by mutableStateOf(LocalDate.now())
        private set
    val items = repository.items

    init {
        viewModelScope.launch {
            sessionManager.userIdFlow.collectLatest {
                userId = it
                repository.cargar(it)
            }
        }
    }

    fun refrescar() {
        if (userId > 0) repository.cargar(userId)
    }

    fun mesAnterior() { mesVisible = mesVisible.minusMonths(1); fechaSeleccionada = mesVisible.atDay(1) }
    fun mesSiguiente() { mesVisible = mesVisible.plusMonths(1); fechaSeleccionada = mesVisible.atDay(1) }
    fun seleccionarFecha(fecha: LocalDate) { fechaSeleccionada = fecha; mesVisible = YearMonth.from(fecha) }
    fun irAHoy() { fechaSeleccionada = LocalDate.now(); mesVisible = YearMonth.now() }

    fun eventosDelDia(fecha: LocalDate): List<EventoCalendario> = eventosDelMes(fecha.year, fecha.monthValue)
        .filter { it.fecha == fecha.toString() }
        .sortedBy { it.tipo.ordinal }

    fun proximosEventos(desde: LocalDate = LocalDate.now(), limite: Int = 1000): List<EventoCalendario> {
        return EventosPresupuesto.proximos(items.value, desde, limite)
    }

    fun eventosDelMes(year: Int = mesVisible.year, month: Int = mesVisible.monthValue): List<EventoCalendario> =
        eventosDelMes(YearMonth.of(year, month))

    private fun eventosDelMes(mes: YearMonth): List<EventoCalendario> = EventosPresupuesto.delMes(items.value, mes)
}
