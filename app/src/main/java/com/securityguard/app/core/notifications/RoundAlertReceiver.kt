package com.securityguard.app.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.securityguard.app.MainActivity
import com.securityguard.app.R
import com.securityguard.app.SecurityGuardApplication
import com.securityguard.app.core.database.RoundEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RoundAlertReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val roundId = intent.getLongExtra(EXTRA_ROUND_ID, -1L)
        if (roundId <= 0L) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SecurityGuardApplication
                val round = app.database.roundDao().get(roundId) ?: return@launch
                if (round.status != "SCHEDULED" && round.status != "LATE") return@launch
                val now = System.currentTimeMillis()
                val session = app.database.shiftSessionDao().get(round.shiftSessionId) ?: return@launch
                val shift = app.database.shiftDao().get(session.shiftId) ?: return@launch
                val lateAfter = shift.lateAfterMinutes
                if (now < round.scheduledTime + lateAfter.coerceAtLeast(0) * 60_000L) {
                    RoundAlertScheduler(context).schedule(round, shift)
                    return@launch
                }
                app.database.roundDao().update(round.copy(status = "LATE", updatedAt = now))
                postNotification(context, round)
                if (shift.vibrationEnabled) vibrate(context)
                scheduleRepeat(context, round)
            } finally { pending.finish() }
        }
    }

    private fun postNotification(context: Context, round: RoundEntity) {
        val open = PendingIntent.getActivity(
            context, round.id.toInt(), Intent(context, MainActivity::class.java).apply {
                putExtra("open_round_id", round.id)
            }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, RoundAlertScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("Round is late")
            .setContentText("Patrolling round is waiting to be started.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        if (Build.VERSION.SDK_INT < 33 || NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            NotificationManagerCompat.from(context).notify(round.id.toInt(), notification)
        }
    }

    private fun vibrate(context: Context) {
        val vibrator = context.getSystemService(Vibrator::class.java) ?: return
        if (!vibrator.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 350, 180, 350), -1))
        else @Suppress("DEPRECATION") vibrator.vibrate(longArrayOf(0, 350, 180, 350), -1)
    }

    private fun scheduleRepeat(context: Context, round: RoundEntity) {
        val app = context.applicationContext as SecurityGuardApplication
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            val session = app.database.shiftSessionDao().get(round.shiftSessionId) ?: return@launch
            val shift = app.database.shiftDao().get(session.shiftId) ?: return@launch
            if (shift.alertRepeatMinutes <= 0) return@launch
            val triggerRound = round.copy(scheduledTime = System.currentTimeMillis() + shift.alertRepeatMinutes * 60_000L, status = "LATE")
            RoundAlertScheduler(context).schedule(triggerRound, shift)
        }
    }

    companion object {
        private const val EXTRA_ROUND_ID = "round_id"
        fun intent(context: Context, roundId: Long) = Intent(context, RoundAlertReceiver::class.java).putExtra(EXTRA_ROUND_ID, roundId)
    }
}
