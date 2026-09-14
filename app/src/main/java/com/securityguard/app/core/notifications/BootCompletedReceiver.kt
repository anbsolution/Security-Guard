package com.securityguard.app.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.securityguard.app.SecurityGuardApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId

/** Rebuilds future late-round alarms after reboot or package replacement. */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as SecurityGuardApplication
                val date = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate().toString()
                val scheduler = RoundAlertScheduler(context)
                val shiftsById = app.database.shiftDao().active().associateBy { it.id }
                app.database.shiftSessionDao().forDate(date).forEach { session ->
                    val rounds = app.database.roundDao().forSession(session.id)
                    scheduler.scheduleAll(rounds, shiftsById)
                }
            } finally { pending.finish() }
        }
    }
}
