package com.nexo.app.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexo.app.MainActivity
import com.nexo.app.data.local.PresupuestoRepository
import com.nexo.app.model.EventoCalendario
import com.nexo.app.model.EventosPresupuesto
import com.nexo.app.model.ItemPresupuesto
import com.nexo.app.model.TipoEventoCalendario
import java.time.LocalDate
import java.time.YearMonth
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

object RecordatoriosPresupuesto {
    const val ACTION_REMINDER = "com.nexo.app.action.BUDGET_REMINDER"
    private const val CHANNEL = "recordatorios_presupuesto"
    private const val EXTRA_USER = "budget_user_id"
    private const val EXTRA_ITEM = "budget_item_id"
    private const val EXTRA_DATE = "budget_event_date"
    private const val EXTRA_TYPE = "budget_event_type"
    private const val EXTRA_WHEN = "budget_reminder_when"

    fun actualizar(context: Context, userId: Int, items: List<ItemPresupuesto>) {
        if (userId <= 0) return
        items.forEach { cancelarItem(context, userId, it) }
        val inicio = YearMonth.now()
        (0..2).forEach { offset ->
            val mes = inicio.plusMonths(offset.toLong())
            EventosPresupuesto.delMes(items, mes).forEach { evento -> programarEvento(context, userId, evento) }
        }
    }

    fun cancelarItem(context: Context, userId: Int, item: ItemPresupuesto) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val inicio = YearMonth.now().minusMonths(1)
        (0..14).forEach { offset ->
            val mes = inicio.plusMonths(offset.toLong())
            val fechaPago = mes.atDay(item.diaPago.coerceAtMost(mes.lengthOfMonth())).toString()
            val tipos = buildList {
                add("INGRESO" to fechaPago)
                add("PAGO" to fechaPago)
                item.diaCorte?.let { add("CORTE" to mes.atDay(it.coerceAtMost(mes.lengthOfMonth())).toString()) }
            }
            tipos.forEach { (tipo, fechaEvento) ->
                listOf("ANTES", "HOY").forEach { cuando ->
                    val base = "${item.id}|$tipo|$cuando|$fechaEvento"
                    val token = PendingIntent.getBroadcast(
                        context, base.hashCode(), crearIntent(context, userId, item.id, fechaEvento, tipo, cuando, base),
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (token != null) alarm.cancel(token)
                }
            }
        }
    }

    private fun programarEvento(context: Context, userId: Int, evento: EventoCalendario) {
        val itemId = evento.idItem
        val fecha = LocalDate.parse(evento.fecha)
        listOf("ANTES", "HOY").forEach { cuando ->
            val fechaAviso = if (cuando == "ANTES") fecha.minusDays(1) else fecha
            val hora = if (cuando == "ANTES") 19 else 8
            val cuandoAviso = fechaAviso.atTime(hora, 0)
            val millis = Calendar.getInstance().apply {
                set(Calendar.YEAR, cuandoAviso.year)
                set(Calendar.MONTH, cuandoAviso.monthValue - 1)
                set(Calendar.DAY_OF_MONTH, cuandoAviso.dayOfMonth)
                set(Calendar.HOUR_OF_DAY, cuandoAviso.hour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            if (millis <= System.currentTimeMillis()) return@forEach
            val tipo = evento.tipo.name
            val base = "$itemId|$tipo|$cuando|${evento.fecha}"
            val pendingIntent = PendingIntent.getBroadcast(
                context, base.hashCode(), crearIntent(context, userId, itemId, evento.fecha, tipo, cuando, base),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, pendingIntent)
        }
    }

    private fun crearIntent(context: Context, userId: Int, itemId: String, fecha: String, tipo: String, cuando: String, identidad: String) =
        Intent(context, RecordatorioPresupuestoReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("nexo://presupuesto/${Uri.encode(identidad)}")
            putExtra(EXTRA_USER, userId)
            putExtra(EXTRA_ITEM, itemId)
            putExtra(EXTRA_DATE, fecha)
            putExtra(EXTRA_TYPE, tipo)
            putExtra(EXTRA_WHEN, cuando)
        }

    internal fun recibir(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences("nexo_presupuesto", Context.MODE_PRIVATE)
            prefs.all.keys.mapNotNull { key ->
                key.removePrefix("items_usuario_").takeIf { it != key }?.toIntOrNull()
            }.forEach { id ->
                val repository = PresupuestoRepository(context)
                repository.cargar(id)
                actualizar(context, id, repository.items.value)
            }
            return
        }
        val userId = intent.getIntExtra(EXTRA_USER, 0)
        if (userId <= 0) return

        val itemId = intent.getStringExtra(EXTRA_ITEM).orEmpty()
        val fecha = intent.getStringExtra(EXTRA_DATE).orEmpty()
        val tipo = runCatching { TipoEventoCalendario.valueOf(intent.getStringExtra(EXTRA_TYPE).orEmpty()) }.getOrNull() ?: return
        val cuando = intent.getStringExtra(EXTRA_WHEN).orEmpty()
        val repository = PresupuestoRepository(context)
        repository.cargar(userId)
        val item = repository.items.value.firstOrNull { it.id == itemId } ?: return
        val evento = EventosPresupuesto.delMes(repository.items.value, YearMonth.from(LocalDate.parse(fecha)))
            .firstOrNull { it.fecha == fecha && it.tipo == tipo && (it.concepto == item.concepto || it.concepto == "Corte · ${item.concepto}") }
            ?: return
        mostrar(context, userId, evento, cuando)
        actualizar(context, userId, repository.items.value)
    }

    private fun mostrar(context: Context, userId: Int, evento: EventoCalendario, cuando: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(NotificationChannel(CHANNEL, "Pagos e ingresos programados", NotificationManager.IMPORTANCE_DEFAULT))
        }
        val fecha = LocalDate.parse(evento.fecha)
        val accion = if (cuando == "ANTES") "Mañana" else "Hoy"
        val tipoTexto = when (evento.tipo) {
            TipoEventoCalendario.INGRESO -> "recibes"
            TipoEventoCalendario.PAGO -> "vence el pago de"
            TipoEventoCalendario.CORTE -> "es el corte de"
        }
        val cantidad = if (evento.monto > 0) " · ${NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-MX")).format(evento.monto)}" else ""
        val openApp = PendingIntent.getActivity(
            context, userId + fecha.dayOfMonth,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("$accion: ${evento.concepto}")
            .setContentText("$tipoTexto ${evento.concepto.lowercase(Locale.forLanguageTag("es-MX"))}$cantidad")
            .setStyle(NotificationCompat.BigTextStyle().bigText("$accion ${fecha.dayOfMonth}: $tipoTexto ${evento.concepto.lowercase(Locale.forLanguageTag("es-MX"))}$cantidad"))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) NotificationManagerCompat.from(context).notify((evento.concepto + evento.fecha + evento.tipo).hashCode(), notification)
    }
}

class RecordatorioPresupuestoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RecordatoriosPresupuesto.recibir(context, intent)
}
