package org.fossify.clock.activities

import android.app.AlertDialog
import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.text.format.DateFormat
import android.widget.Toast
import org.fossify.clock.R
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.helpers.AlarmWidgetProvider
import org.fossify.clock.models.Alarm
import org.fossify.commons.extensions.getSelectedDaysString
import java.util.Calendar

class AlarmWidgetConfigureActivity : SimpleActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        val widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        )
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        val alarms = dbHelper.getAlarms()
        if (alarms.isEmpty()) {
            Toast.makeText(this, R.string.alarm_widget_no_alarms, Toast.LENGTH_LONG).show()
            finish()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.alarm_widget_choose)
            .setItems(alarms.map(::getAlarmDescription).toTypedArray()) { _, index ->
                AlarmWidgetProvider.assignAlarm(this, widgetId, alarms[index].id)
                setResult(RESULT_OK, intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
                finish()
            }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun getAlarmDescription(alarm: Alarm): String {
        val time = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, alarm.timeInMinutes / 60)
            set(Calendar.MINUTE, alarm.timeInMinutes % 60)
            set(Calendar.SECOND, 0)
        }.time
        val schedule = when {
            alarm.days > 0 -> getSelectedDaysString(alarm.days)
            alarm.isToday() -> getString(org.fossify.commons.R.string.today)
            alarm.isTomorrow() -> getString(org.fossify.commons.R.string.tomorrow)
            else -> getString(R.string.not_scheduled)
        }
        val label = alarm.label.ifBlank { getString(R.string.alarm_widget_name) }
        val state = getString(if (alarm.isEnabled) R.string.alarm_widget_on else R.string.alarm_widget_off)
        return "${DateFormat.getTimeFormat(this).format(time)}  $label  $schedule  $state"
    }
}
