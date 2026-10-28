package com.nexo.app.data.local

import android.content.Context
import com.nexo.app.model.ItemPresupuesto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Almacenamiento local por usuario. Se podrá sustituir por endpoints MariaDB más adelante. */
class PresupuestoRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("nexo_presupuesto", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val _items = MutableStateFlow<List<ItemPresupuesto>>(emptyList())
    val items: StateFlow<List<ItemPresupuesto>> = _items
    private var usuarioActivo = 0

    fun cargar(idUsuario: Int) {
        usuarioActivo = idUsuario
        _items.value = if (idUsuario <= 0) emptyList() else runCatching {
            json.decodeFromString<List<ItemPresupuesto>>(prefs.getString(claveItems(idUsuario), "[]") ?: "[]")
        }.getOrDefault(emptyList())
    }

    fun agregar(item: ItemPresupuesto) {
        if (item.idUsuario != usuarioActivo || usuarioActivo <= 0) return
        guardar(item.idUsuario, _items.value + item)
    }

    fun eliminar(id: String) {
        if (usuarioActivo <= 0) return
        guardar(usuarioActivo, _items.value.filterNot { it.id == id })
    }

    fun actualizarSaldoDeuda(id: String, saldoActual: Double) {
        if (usuarioActivo <= 0) return
        guardar(usuarioActivo, _items.value.map { item ->
            if (item.id == id && item.saldoActual != null) item.copy(saldoActual = saldoActual) else item
        })
    }

    fun necesidadesBasicas(idUsuario: Int): Double =
        prefs.getString(claveNecesidades(idUsuario), "0")?.toDoubleOrNull() ?: 0.0

    fun guardarNecesidadesBasicas(idUsuario: Int, monto: Double) {
        if (idUsuario <= 0) return
        prefs.edit().putString(claveNecesidades(idUsuario), monto.toString()).apply()
    }

    private fun guardar(idUsuario: Int, items: List<ItemPresupuesto>) {
        _items.value = items
        prefs.edit().putString(claveItems(idUsuario), json.encodeToString(items)).apply()
    }

    private fun claveItems(id: Int) = "items_usuario_$id"
    private fun claveNecesidades(id: Int) = "necesidades_usuario_$id"
}
