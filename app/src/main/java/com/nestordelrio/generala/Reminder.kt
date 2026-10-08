package com.nestordelrio.generala

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Recordatorio diario opcional ("¿una partida rápida?"), a las 19 h, solo si ese día no jugó. */
object Reminder {
    private const val WORK_NAME = "daily_reminder"
    const val CHANNEL_ID = "daily_reminder"
    private val REMINDER_TIME: LocalTime = LocalTime.of(19, 0)

    fun enable(context: Context) {
        val now = LocalDateTime.now()
        var next = now.toLocalDate().atTime(REMINDER_TIME)
        if (!next.isAfter(now)) next = next.plusDays(1)
        val delayMinutes = Duration.between(now, next).toMinutes()
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMinutes, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun disable(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun ensureChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = context.getString(R.string.reminder_channel_description) }
        nm.createNotificationChannel(channel)
    }
}

class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val ctx = applicationContext
        val prefs = Prefs(ctx)
        if (!prefs.reminderEnabled) return Result.success()
        if (prefs.lastOpenDay == LocalDate.now().toEpochDay()) return Result.success()
        if (!Reminder.canNotify(ctx)) return Result.success()

        Reminder.ensureChannel(ctx)
        val texts = ctx.resources.getStringArray(R.array.reminder_texts)
        val text = texts[(LocalDate.now().toEpochDay() % texts.size).toInt()]
        val intent = Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(ctx, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(ctx, Reminder.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_dice)
            .setContentTitle(ctx.getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(ctx).notify(1001, notification)
        } catch (_: SecurityException) {
            // El usuario revocó el permiso de notificaciones: no hacemos nada.
        }
        return Result.success()
    }
}
