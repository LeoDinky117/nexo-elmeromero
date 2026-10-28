package com.nexo.app.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.nexo.app.MainActivity
import com.nexo.app.model.MovimientoBorrador
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Calendar

object BorradoresNotificacion {
    const val CHANNEL_ID = "borradores_movimientos"
    const val EXTRA_USER_ID = "borradores_user_id"
    private const val PREFS = "nexo_borradores"
    private const val HORA_RECORDATORIO = 20
    private val borradoresLock = Any()

    fun cargarBorradores(context: Context, userId: Int): List<MovimientoBorrador> {
        val contenido = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString("usuario_$userId", null) ?: return emptyList()
        return runCatching { Json.decodeFromString<List<MovimientoBorrador>>(contenido) }
            .getOrDefault(emptyList())
    }

    fun guardarBorradores(context: Context, userId: Int, borradores: List<MovimientoBorrador>) {
        synchronized(borradoresLock) {
            val preferencias = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (borradores.isEmpty()) preferencias.edit().remove("usuario_$userId").commit()
            else preferencias.edit().putString("usuario_$userId", Json.encodeToString(borradores)).commit()
        }
    }

    /** Quita por ID desde la versión más reciente guardada, evitando reescribir una lista vieja. */
    fun eliminarBorrador(context: Context, userId: Int, borradorId: String): List<MovimientoBorrador> = synchronized(borradoresLock) {
        val restantes = cargarBorradores(context, userId).filterNot { it.id == borradorId }
        guardarBorradores(context, userId, restantes)
        restantes
    }

    fun programarRecordatorio(context: Context, userId: Int) {
        if (userId <= 0) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = pendingIntent(context, userId)
        val hora = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, HORA_RECORDATORIO)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, hora.timeInMillis, pendingIntent)
    }

    fun cancelarRecordatorio(context: Context, userId: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(pendingIntent(context, userId))
    }

    fun mostrarNotificacion(context: Context, userId: Int) {
        val borradores = cargarBorradores(context, userId)
        if (borradores.isEmpty()) return
        // Si todavía quedan borradores, recordar de nuevo al siguiente cierre del día.
        programarRecordatorio(context, userId)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                CHANNEL_ID,
                "Movimientos por confirmar",
                android.app.NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Avisos para revisar movimientos dictados" }
            manager.createNotificationChannel(channel)
        }

        val openApp = PendingIntent.getActivity(
            context,
            userId,
            Intent(context, MainActivity::class.java).putExtra(EXTRA_USER_ID, userId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacion = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Movimientos por confirmar")
            .setContentText("Tienes ${borradores.size} movimiento(s) pendiente(s). Abre NEXO para revisarlos.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(userId, notificacion)
        }
    }

    private fun pendingIntent(context: Context, userId: Int): PendingIntent {
        val intent = Intent(context, BorradoresReminderReceiver::class.java)
            .putExtra(EXTRA_USER_ID, userId)
            .setData(Uri.parse("nexo://recordatorio-borradores/$userId"))
        return PendingIntent.getBroadcast(
            context,
            userId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class BorradoresReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val preferencias = context.getSharedPreferences("nexo_borradores", Context.MODE_PRIVATE)
            preferencias.all.keys.mapNotNull { clave ->
                clave.removePrefix("usuario_").takeIf { it != clave }?.toIntOrNull()
            }.forEach { userId ->
                if (BorradoresNotificacion.cargarBorradores(context, userId).isNotEmpty()) {
                    BorradoresNotificacion.programarRecordatorio(context, userId)
                }
            }
            return
        }
        val userId = intent.getIntExtra(BorradoresNotificacion.EXTRA_USER_ID, 0)
        BorradoresNotificacion.mostrarNotificacion(context, userId)
    }
}
