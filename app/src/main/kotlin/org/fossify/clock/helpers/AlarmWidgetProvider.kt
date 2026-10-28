package org.fossify.clock.helpers

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import org.fossify.clock.R
import org.fossify.clock.activities.AlarmWidgetConfigureActivity
import org.fossify.clock.activities.MainActivity
import org.fossify.clock.extensions.config
import org.fossify.clock.extensions.dbHelper
import org.fossify.clock.models.Alarm
import org.fossify.clock.receivers.ToggleAlarmWidgetReceiver
import org.fossify.commons.extensions.getSelectedDaysString
import org.fossify.commons.helpers.ensureBackgroundThread
import java.util.Calendar

class AlarmWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, widgetIds: IntArray) {
        updateAll(context)
    }

    override fun onDeleted(context: Context, widgetIds: IntArray) {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        preferences.edit().apply {
            widgetIds.forEach { remove(alarmKey(it)) }
        }.apply()
    }

    companion object {
        private const val PREFERENCES = "alarm_widget_preferences"

        private fun alarmKey(widgetId: Int) = "alarm_$widgetId"

        fun assignAlarm(context: Context, widgetId: Int, alarmId: Int) {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .edit().putInt(alarmKey(widgetId), alarmId).apply()
            updateAll(context)
        }

        private fun updateWidget(context: Context, manager: AppWidgetManager, widgetId: Int) {
            val alarmId = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
                .getInt(alarmKey(widgetId), -1)
            val alarm = context.dbHelper.getAlarmWithId(alarmId)
            val views = RemoteViews(context.packageName, R.layout.widget_alarm)
            val textColor = context.config.widgetTextColor
            views.setTextColor(R.id.alarm_widget_time, textColor)
            views.setTextColor(R.id.alarm_widget_label, textColor)
            views.setTextColor(R.id.alarm_widget_toggle, textColor)
            views.setInt(R.id.alarm_widget_root, "setBackgroundColor", context.config.widgetBgColor)

            if (alarm == null) {
                views.setTextViewText(R.id.alarm_widget_time, context.getString(R.string.alarm_widget_name))
                views.setTextViewText(R.id.alarm_widget_label, context.getString(R.string.alarm_widget_unavailable))
                views.setTextViewText(R.id.alarm_widget_toggle, context.getString(R.string.alarm_widget_choose))
                views.setOnClickPendingIntent(
                    R.id.alarm_widget_root,
                    configurePendingIntent(context, widgetId)
                )
            } else {
                val time = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alarm.timeInMinutes / 60)
                    set(Calendar.MINUTE, alarm.timeInMinutes % 60)
                    set(Calendar.SECOND, 0)
                }.time
                val title = DateFormat.getTimeFormat(context).format(time)
                val label = alarm.label.ifBlank { context.getString(R.string.alarm_widget_name) }
                views.setTextViewText(R.id.alarm_widget_time, title)
                views.setTextViewText(R.id.alarm_widget_label, "$label · ${scheduleLabel(context, alarm)}")
                views.setTextViewText(
                    R.id.alarm_widget_toggle,
                    context.getString(if (alarm.isEnabled) R.string.alarm_widget_on else R.string.alarm_widget_off)
                )
                views.setContentDescription(
                    R.id.alarm_widget_toggle,
                    context.getString(if (alarm.isEnabled) R.string.alarm_widget_toggle_on else R.string.alarm_widget_toggle_off)
                )
                views.setOnClickPendingIntent(R.id.alarm_widget_toggle, togglePendingIntent(context, widgetId, alarm))
                views.setOnClickPendingIntent(R.id.alarm_widget_time, openAlarmsPendingIntent(context, widgetId))
                views.setOnClickPendingIntent(R.id.alarm_widget_label, openAlarmsPendingIntent(context, widgetId))
            }

            manager.updateAppWidget(widgetId, views)
        }

        private fun scheduleLabel(context: Context, alarm: Alarm): String = when {
            alarm.days > 0 -> context.getSelectedDaysString(alarm.days)
            alarm.isToday() -> context.getString(org.fossify.commons.R.string.today)
            alarm.isTomorrow() -> context.getString(org.fossify.commons.R.string.tomorrow)
            else -> context.getString(R.string.not_scheduled)
        }

        private fun configurePendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, AlarmWidgetConfigureActivity::class.java)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            return PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun togglePendingIntent(context: Context, widgetId: Int, alarm: Alarm): PendingIntent {
            val intent = Intent(context, ToggleAlarmWidgetReceiver::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
                putExtra(ALARM_ID, alarm.id)
                data = android.net.Uri.parse("clock-alarm-widget://${alarm.id}/$widgetId")
            }
            return PendingIntent.getBroadcast(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        private fun openAlarmsPendingIntent(context: Context, widgetId: Int): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(OPEN_TAB, TAB_ALARM)
                data = android.net.Uri.parse("clock-alarm-widget://open/$widgetId")
            }
            return PendingIntent.getActivity(
                context,
                widgetId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, AlarmWidgetProvider::class.java)
            val widgetIds = manager.getAppWidgetIds(component)
            if (widgetIds.isNotEmpty()) {
                ensureBackgroundThread {
                    widgetIds.forEach { updateWidget(context, manager, it) }
                }
            }
        }
    }
}
