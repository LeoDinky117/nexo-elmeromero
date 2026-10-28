package com.nexo.app.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.widget.RemoteViews
import com.nexo.app.MainActivity
import com.nexo.app.nexo.R
import com.nexo.app.data.local.PresupuestoRepository
import com.nexo.app.data.local.SessionManager
import com.nexo.app.model.EventosPresupuesto
import com.nexo.app.model.TipoEventoCalendario
import com.nexo.app.util.BorradoresNotificacion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class MovimientoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> actualizar(context, manager, id) }
    }

    companion object {
        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, MovimientoWidgetProvider::class.java))
            ids.forEach { actualizar(context, manager, it) }
        }

        private fun actualizar(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val views = RemoteViews(context.packageName, R.layout.widget_movimiento)
            views.setOnClickPendingIntent(R.id.widget_add_button, capturaIntent(context, false))
            views.setOnClickPendingIntent(R.id.widget_voice_button, capturaIntent(context, true))
            widgetScope.launch {
                val userId = runCatching { SessionManager(context).getUserId() }.getOrDefault(0)
                val cantidad = if (userId > 0) BorradoresNotificacion.cargarBorradores(context, userId).size else 0
                val mensaje = when {
                    userId <= 0 -> "Inicia sesión una vez en NEXO para usar la captura rápida"
                    cantidad == 0 -> "Tus movimientos se guardarán como borradores"
                    else -> "$cantidad movimiento(s) esperando la confirmación"
                }
                views.setTextViewText(R.id.widget_drafts_count, mensaje)
                manager.updateAppWidget(widgetId, views)
            }
            manager.updateAppWidget(widgetId, views)
        }

        private fun capturaIntent(context: Context, dictar: Boolean): PendingIntent {
            val intent = Intent(context, CapturaMovimientoActivity::class.java)
                .putExtra(CapturaMovimientoActivity.EXTRA_DICTAR, dictar)
            val requestCode = if (dictar) 2 else 1
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}

class CalendarioWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        actualizarConRecepcionActiva(context, manager, ids)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_MONTH_PREVIOUS || intent.action == ACTION_MONTH_NEXT) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val key = OFFSET_KEY_PREFIX + widgetId
                val delta = if (intent.action == ACTION_MONTH_PREVIOUS) -1 else 1
                prefs.edit().putInt(key, (prefs.getInt(key, 0) + delta).coerceIn(-24, 24)).apply()
                actualizarConRecepcionActiva(context, AppWidgetManager.getInstance(context), intArrayOf(widgetId))
            }
        } else if (intent.action in setOf(Intent.ACTION_DATE_CHANGED, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED)) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalendarioWidgetProvider::class.java))
            actualizarConRecepcionActiva(context, manager, ids)
        }
    }

    private fun actualizarConRecepcionActiva(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val pendingResult = goAsync()
        if (ids.isEmpty()) {
            pendingResult.finish()
            return
        }
        val pendientes = java.util.concurrent.atomic.AtomicInteger(ids.size)
        ids.forEach { widgetId ->
            actualizar(context, manager, widgetId) {
                if (pendientes.decrementAndGet() == 0) pendingResult.finish()
            }
        }
    }

    companion object {
        fun actualizarTodos(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, CalendarioWidgetProvider::class.java))
            ids.forEach { actualizar(context, manager, it) }
        }

        private fun actualizar(context: Context, manager: AppWidgetManager, widgetId: Int, alTerminar: (() -> Unit)? = null) {
            val views = RemoteViews(context.packageName, R.layout.widget_calendario)
            val locale = Locale.forLanguageTag("es-MX")
            val offset = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(OFFSET_KEY_PREFIX + widgetId, 0)
            val mes = YearMonth.now().plusMonths(offset.toLong())
            views.setTextViewText(
                R.id.widget_month_year,
                mes.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale)).replaceFirstChar { it.titlecase(locale) }
            )

            val openApp = PendingIntent.getActivity(
                context,
                30,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_calendar_root, openApp)
            views.setOnClickPendingIntent(R.id.widget_month_previous, monthIntent(context, widgetId, ACTION_MONTH_PREVIOUS, 1))
            views.setOnClickPendingIntent(R.id.widget_month_next, monthIntent(context, widgetId, ACTION_MONTH_NEXT, 2))
            views.removeAllViews(R.id.widget_calendar_grid)
            manager.updateAppWidget(widgetId, views)
            widgetScope.launch {
                try {
                    val userId = runCatching { SessionManager(context).getUserId() }.getOrDefault(0)
                    val repository = PresupuestoRepository(context)
                    if (userId > 0) repository.cargar(userId)
                    val items = repository.items.value
                    val hoy = LocalDate.now()
                    val eventosPorDia = EventosPresupuesto.delMes(items, mes)
                        .groupBy { LocalDate.parse(it.fecha).dayOfMonth }
                    val espaciosAntes = mes.atDay(1).dayOfWeek.value - 1
                    val filas = (espaciosAntes + mes.lengthOfMonth() + 6) / 7
                    repeat(filas) { fila ->
                        val semana = RemoteViews(context.packageName, R.layout.widget_calendar_week)
                        repeat(7) { columna ->
                            val dia = fila * 7 + columna - espaciosAntes + 1
                            val textoId = dayViewId(columna)
                            val puntoId = dotViewId(columna)
                            if (dia in 1..mes.lengthOfMonth()) {
                                semana.setTextViewText(textoId, dia.toString())
                                val fecha = mes.atDay(dia)
                                val esHoy = offset == 0 && fecha == hoy
                                semana.setTextColor(textoId, if (esHoy) Color.rgb(21, 32, 48) else Color.rgb(231, 236, 244))
                                if (esHoy) semana.setInt(textoId, "setBackgroundColor", Color.rgb(255, 159, 46))
                                when (eventosPorDia[dia]?.firstOrNull()?.tipo) {
                                    TipoEventoCalendario.INGRESO -> {
                                        semana.setViewVisibility(puntoId, android.view.View.VISIBLE)
                                        semana.setTextViewText(puntoId, "•")
                                        semana.setTextColor(puntoId, Color.rgb(88, 190, 112))
                                    }
                                    TipoEventoCalendario.PAGO -> {
                                        semana.setViewVisibility(puntoId, android.view.View.VISIBLE)
                                        semana.setTextViewText(puntoId, "•")
                                        semana.setTextColor(puntoId, Color.rgb(234, 77, 155))
                                    }
                                    TipoEventoCalendario.CORTE -> {
                                        semana.setViewVisibility(puntoId, android.view.View.VISIBLE)
                                        semana.setTextViewText(puntoId, "•")
                                        semana.setTextColor(puntoId, Color.rgb(177, 117, 255))
                                    }
                                    null -> semana.setViewVisibility(puntoId, android.view.View.INVISIBLE)
                                }
                            } else {
                                semana.setTextViewText(textoId, "")
                                semana.setViewVisibility(puntoId, android.view.View.INVISIBLE)
                            }
                        }
                        views.addView(R.id.widget_calendar_grid, semana)
                    }

                    val siguiente = EventosPresupuesto.proximos(items, desde = hoy, limite = 1).firstOrNull()
                    val texto = if (siguiente == null) "Sin eventos próximos" else {
                        val fecha = LocalDate.parse(siguiente.fecha).format(DateTimeFormatter.ofPattern("d MMM", locale))
                        val marca = when (siguiente.tipo) {
                            TipoEventoCalendario.INGRESO -> "+"
                            TipoEventoCalendario.PAGO -> "−"
                            TipoEventoCalendario.CORTE -> "•"
                        }
                        "$fecha  $marca ${siguiente.concepto}"
                    }
                    views.setTextViewText(R.id.widget_upcoming, texto)
                    manager.updateAppWidget(widgetId, views)
                } catch (error: Exception) {
                    views.setTextViewText(R.id.widget_upcoming, "Abre NEXO para actualizar el calendario")
                    runCatching { manager.updateAppWidget(widgetId, views) }
                } finally {
                    alTerminar?.invoke()
                }
            }
        }

        private fun monthIntent(context: Context, widgetId: Int, action: String, actionCode: Int): PendingIntent {
            val intent = Intent(context, CalendarioWidgetProvider::class.java)
                .setAction(action)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            return PendingIntent.getBroadcast(
                context, widgetId * 10 + actionCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun dayViewId(day: Int): Int = when (day) {
            0 -> R.id.calendar_day_0
            1 -> R.id.calendar_day_1
            2 -> R.id.calendar_day_2
            3 -> R.id.calendar_day_3
            4 -> R.id.calendar_day_4
            5 -> R.id.calendar_day_5
            else -> R.id.calendar_day_6
        }

        private fun dotViewId(day: Int): Int = when (day) {
            0 -> R.id.calendar_dot_0
            1 -> R.id.calendar_dot_1
            2 -> R.id.calendar_dot_2
            3 -> R.id.calendar_dot_3
            4 -> R.id.calendar_dot_4
            5 -> R.id.calendar_dot_5
            else -> R.id.calendar_dot_6
        }

        private const val PREFS = "nexo_calendar_widget"
        private const val OFFSET_KEY_PREFIX = "month_offset_"
        private const val ACTION_MONTH_PREVIOUS = "com.nexo.app.widgets.CALENDAR_MONTH_PREVIOUS"
        private const val ACTION_MONTH_NEXT = "com.nexo.app.widgets.CALENDAR_MONTH_NEXT"
    }
}
