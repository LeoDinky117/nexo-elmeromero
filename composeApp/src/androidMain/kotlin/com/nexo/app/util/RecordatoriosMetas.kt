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
import com.nexo.app.model.PeriodicidadAportacionMeta
import com.nexo.app.model.ProgresoMetaVista
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** Recordatorios locales de aportación, programados un día antes de cada fecha prevista. */
object RecordatoriosMetas {
    private const val CHANNEL = "aportaciones_metas"
    private const val PREFS = "nexo_recordatorios_metas"
    private const val PREFIX = "meta_"
    private const val ACTION_REMINDER = "com.nexo.app.action.META_CONTRIBUTION_REMINDER"
    private const val EXTRA_USER = "meta_reminder_user"
    private const val EXTRA_META = "meta_reminder_id"

    private data class Recordatorio(
        val userId: Int,
        val metaId: Int,
        val nombre: String,
        val fechaObjetivo: LocalDate,
        val siguienteAportacion: LocalDate,
        val periodicidad: PeriodicidadAportacionMeta
    )

    fun programar(
        context: Context,
        userId: Int,
        metaId: Int,
        nombre: String,
        fechaObjetivo: String,
        periodicidad: PeriodicidadAportacionMeta
    ) {
        if (userId <= 0 || metaId <= 0) return
        val objetivo = runCatching { LocalDate.parse(fechaObjetivo) }.getOrNull() ?: return
        val ahora = LocalDateTime.now()
        if (!objetivo.isAfter(ahora.toLocalDate())) return

        // Ancla la frecuencia a la fecha objetivo y encuentra la primera aportación futura.
        var siguiente = objetivo
        while (true) {
            val anterior = periodicidad.anterior(siguiente)
            if (!anterior.isAfter(ahora.toLocalDate()) || horaAviso(anterior) <= ahora) break
            siguiente = anterior
        }
        val recordatorio = Recordatorio(userId, metaId, nombre, objetivo, siguiente, periodicidad)
        guardar(context, recordatorio)
        programarAlarma(context, recordatorio)
    }

    /** Conserva los recordatorios de metas activas y elimina los de metas completadas. */
    fun sincronizar(context: Context, userId: Int, metas: List<ProgresoMetaVista>) {
        val activas = metas.filter { it.activa && it.totalAhorrado < it.montoObjetivo }.map { it.idMeta }.toSet()
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.all.keys.filter { it.startsWith(PREFIX) }.forEach { key ->
            val recordatorio = leer(prefs.getString(key, null)) ?: return@forEach
            if (recordatorio.userId == userId && recordatorio.metaId !in activas) {
                cancelarAlarma(context, recordatorio)
                prefs.edit().remove(key).apply()
            } else if (recordatorio.userId == userId) {
                programarAlarma(context, recordatorio)
            }
        }
    }

    internal fun recibir(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.all.values.mapNotNull { leer(it as? String) }.forEach { programarAlarma(context, it) }
            return
        }

        val userId = intent.getIntExtra(EXTRA_USER, 0)
        val metaId = intent.getIntExtra(EXTRA_META, 0)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val key = clave(userId, metaId)
        val actual = leer(prefs.getString(key, null)) ?: return
        mostrar(context, actual)

        var siguiente = actual.periodicidad.siguiente(actual.siguienteAportacion)
        val ahora = LocalDateTime.now()
        while (siguiente <= actual.fechaObjetivo && horaAviso(siguiente) <= ahora) {
            siguiente = actual.periodicidad.siguiente(siguiente)
        }
        if (siguiente <= actual.fechaObjetivo) {
            val actualizado = actual.copy(siguienteAportacion = siguiente)
            guardar(context, actualizado)
            programarAlarma(context, actualizado)
        } else {
            cancelarAlarma(context, actual)
            prefs.edit().remove(key).apply()
        }
    }

    private fun horaAviso(fechaAportacion: LocalDate): LocalDateTime = fechaAportacion.minusDays(1).atTime(19, 0)

    private fun programarAlarma(context: Context, recordatorio: Recordatorio) {
        if (recordatorio.siguienteAportacion > recordatorio.fechaObjetivo) return
        val horaAviso = horaAviso(recordatorio.siguienteAportacion)
            .atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (horaAviso <= System.currentTimeMillis()) return
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, recordatorio) ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, horaAviso, pending)
    }

    private fun cancelarAlarma(context: Context, recordatorio: Recordatorio) {
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pending = pendingIntent(context, recordatorio, PendingIntent.FLAG_NO_CREATE) ?: return
        alarm.cancel(pending)
    }

    private fun pendingIntent(context: Context, recordatorio: Recordatorio, flag: Int = PendingIntent.FLAG_UPDATE_CURRENT): PendingIntent? {
        val identity = "${recordatorio.userId}|${recordatorio.metaId}"
        val intent = Intent(context, RecordatorioMetaReceiver::class.java).apply {
            action = ACTION_REMINDER
            data = Uri.parse("nexo://metas/${Uri.encode(identity)}")
            putExtra(EXTRA_USER, recordatorio.userId)
            putExtra(EXTRA_META, recordatorio.metaId)
        }
        return PendingIntent.getBroadcast(
            context,
            identity.hashCode(),
            intent,
            flag or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun mostrar(context: Context, recordatorio: Recordatorio) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, "Aportaciones a metas", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val openApp = PendingIntent.getActivity(
            context,
            recordatorio.userId + recordatorio.metaId,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val aviso = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Mañana toca aportar")
            .setContentText("Recuerda tu aportación para ${recordatorio.nombre}.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Mañana corresponde tu aportación ${recordatorio.periodicidad.etiqueta.lowercase()} para la meta “${recordatorio.nombre}”."))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) NotificationManagerCompat.from(context).notify(recordatorio.metaId, aviso)
    }

    private fun guardar(context: Context, recordatorio: Recordatorio) {
        val json = JSONObject().apply {
            put("userId", recordatorio.userId)
            put("metaId", recordatorio.metaId)
            put("nombre", recordatorio.nombre)
            put("fechaObjetivo", recordatorio.fechaObjetivo.toString())
            put("siguienteAportacion", recordatorio.siguienteAportacion.toString())
            put("periodicidad", recordatorio.periodicidad.name)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(clave(recordatorio.userId, recordatorio.metaId), json.toString()).apply()
    }

    private fun leer(valor: String?): Recordatorio? = runCatching {
        val json = JSONObject(valor ?: return null)
        Recordatorio(
            userId = json.getInt("userId"),
            metaId = json.getInt("metaId"),
            nombre = json.getString("nombre"),
            fechaObjetivo = LocalDate.parse(json.getString("fechaObjetivo")),
            siguienteAportacion = LocalDate.parse(json.getString("siguienteAportacion")),
            periodicidad = PeriodicidadAportacionMeta.valueOf(json.getString("periodicidad"))
        )
    }.getOrNull()

    private fun clave(userId: Int, metaId: Int) = "$PREFIX${userId}_$metaId"
}

class RecordatorioMetaReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = RecordatoriosMetas.recibir(context, intent)
}
