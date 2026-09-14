package com.securityguard.app.core.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.securityguard.app.core.database.RoundEntity
import com.securityguard.app.core.database.ShiftEntity

/** Schedules only the LATE threshold, never the normal round due time. */
class RoundAlertScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    init { createChannel() }

    fun schedule(round: RoundEntity, shift: ShiftEntity) {
        if (!shift.roundsEnabled || shift.lateAfterMinutes < 0) return
        val trigger = round.scheduledTime + shift.lateAfterMinutes.coerceAtLeast(0) * 60_000L
        val intent = RoundAlertReceiver.intent(context, round.id)
        val pi = PendingIntent.getBroadcast(
            context, requestCode(round.id), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (Build.VERSION.SDK_INT >= 23) {
            if (Build.VERSION.SDK_INT >= 31 && !alarmManager.canScheduleExactAlarms()) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi)
            }
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, trigger, pi)
        }
    }

    fun cancel(roundId: Long) {
        val pi = PendingIntent.getBroadcast(
            context, requestCode(roundId), RoundAlertReceiver.intent(context, roundId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pi)
        pi.cancel()
    }

    fun scheduleAll(rounds: List<RoundEntity>, shiftsById: Map<Long, ShiftEntity>) {
        rounds.filter { it.status == "SCHEDULED" || it.status == "LATE" }.forEach { round ->
            shiftsById[round.shiftSessionId]?.let { schedule(round, it) }
        }
    }

    private fun requestCode(roundId: Long): Int = (roundId xor (roundId ushr 32)).toInt()

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Late Round Alerts", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a scheduled patrol round has passed its late threshold."
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object { const val CHANNEL_ID = "round_late_alerts" }
}
