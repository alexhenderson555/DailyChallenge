package com.dailychallenge.app.reminder

import android.app.PendingIntent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.dailychallenge.app.DailyChallengeApp
import com.dailychallenge.app.R
import com.dailychallenge.app.data.PreferencesDataSource
import com.dailychallenge.app.widget.ACTION_NOTIF_DONE
import com.dailychallenge.app.widget.ACTION_NOTIF_SKIP
import com.dailychallenge.app.widget.NOTIFICATION_ID_DAILY
import com.dailychallenge.app.widget.MidnightReceiver
import com.dailychallenge.app.widget.WidgetActionReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = PreferencesDataSource(context).preferences.first()
                        scheduleAllReminders(context, prefs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            ACTION_DAILY -> {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val prefs = PreferencesDataSource(context)
                        val userPrefs = prefs.preferences.first()
                        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK)
                        val isWeekend = day == java.util.Calendar.SATURDAY || day == java.util.Calendar.SUNDAY
                        if (!userPrefs.remindWeekdaysOnly || !isWeekend) {
                            val todayKey = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                            val pending = prefs.getPendingChallenges()
                            val goalText = pending.find { it.date == todayKey }?.challengeText
                            showDailyNotification(context, goalText)
                        }
                        scheduleAllReminders(context, userPrefs)
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }

    private fun showDailyNotification(context: Context, goalText: String?) {
        val title = context.getString(R.string.notification_daily_title)
        val text = goalText ?: context.getString(R.string.notification_daily_text)

        val doneIntent = Intent(context, WidgetActionReceiver::class.java).apply { action = ACTION_NOTIF_DONE }
        val donePi = PendingIntent.getBroadcast(context, 2000, doneIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val skipIntent = Intent(context, WidgetActionReceiver::class.java).apply { action = ACTION_NOTIF_SKIP }
        val skipPi = PendingIntent.getBroadcast(context, 2001, skipIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            putExtra("open_tab", 0)
        }
        val openPi = if (openIntent != null) PendingIntent.getActivity(context, 2002, openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) else null

        val builder = NotificationCompat.Builder(context, DailyChallengeApp.CHANNEL_DAILY)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .addAction(0, context.getString(R.string.notification_action_done), donePi)
            .addAction(0, context.getString(R.string.notification_action_skip), skipPi)

        if (openPi != null) builder.setContentIntent(openPi)

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_DAILY, builder.build())
        }
    }

    companion object {
        private const val ACTION_DAILY = "com.dailychallenge.app.DAILY_REMINDER"

        suspend fun scheduleFromContext(context: Context) {
            scheduleAllReminders(context, PreferencesDataSource(context).preferences.first())
        }

        fun scheduleAllReminders(context: Context, prefs: com.dailychallenge.app.data.UserPreferences) {
            scheduleDailyReminder(context, prefs.reminderHour, prefs.reminderMinute, requestCode = 0, weekdaysOnly = prefs.remindWeekdaysOnly)
            val h2 = prefs.secondReminderHour
            val m2 = prefs.secondReminderMinute
            if (prefs.isPremium && h2 != null && m2 != null) {
                scheduleDailyReminder(context, h2, m2, requestCode = 1, weekdaysOnly = prefs.remindWeekdaysOnly)
            }
            MidnightReceiver.scheduleMidnightRefresh(context)
        }

        fun scheduleDailyReminder(context: Context, hour: Int, minute: Int, requestCode: Int = 0, weekdaysOnly: Boolean = false) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
            }
            if (weekdaysOnly) {
                while (true) {
                    val day = cal.get(Calendar.DAY_OF_WEEK)
                    if (day != Calendar.SATURDAY && day != Calendar.SUNDAY) break
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            com.dailychallenge.app.util.AlarmHelper.scheduleExactAlarm(
                context, cal.timeInMillis, requestCode, ReminderReceiver::class.java, ACTION_DAILY
            )
        }
    }
}
