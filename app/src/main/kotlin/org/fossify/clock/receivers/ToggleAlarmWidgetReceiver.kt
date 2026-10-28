package org.fossify.clock.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import org.fossify.clock.extensions.alarmController
import org.fossify.clock.extensions.cancelAlarmClock
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.extensions.updateWidgets
import org.fossify.clock.helpers.ALARM_ID
import org.fossify.commons.helpers.ensureBackgroundThread

class ToggleAlarmWidgetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(ALARM_ID, -1)
        if (alarmId < 0) return

        val pendingResult = goAsync()
        ensureBackgroundThread {
            try {
                val alarm = context.dbHelper.getAlarmWithId(alarmId) ?: return@ensureBackgroundThread
                val enabled = !alarm.isEnabled
                if (!context.dbHelper.updateAlarmEnabledState(alarmId, enabled)) return@ensureBackgroundThread

                if (enabled) {
                    alarm.isEnabled = true
                    context.alarmController.scheduleNextOccurrence(alarm)
                } else {
                    context.cancelAlarmClock(alarm)
                }

                context.updateWidgets()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
