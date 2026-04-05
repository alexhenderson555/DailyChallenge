package com.dailychallenge.app.widget

import android.appwidget.AppWidgetManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.core.app.NotificationManagerCompat
import com.dailychallenge.app.R
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.HistoryDataSource
import com.dailychallenge.app.data.PreferencesDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

const val ACTION_WIDGET_DONE = "com.dailychallenge.app.WIDGET_DONE"
const val ACTION_WIDGET_SKIP = "com.dailychallenge.app.WIDGET_SKIP"
const val ACTION_NOTIF_DONE = "com.dailychallenge.app.NOTIF_DONE"
const val ACTION_NOTIF_SKIP = "com.dailychallenge.app.NOTIF_SKIP"
const val EXTRA_APPWIDGET_ID = "appWidgetId"
const val NOTIFICATION_ID_DAILY = 1001

class WidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_WIDGET_DONE -> markAndUpdate(context, completed = true)
            ACTION_WIDGET_SKIP -> markAndUpdate(context, completed = false)
            ACTION_NOTIF_DONE -> markAndUpdate(context, completed = true, dismissNotification = true)
            ACTION_NOTIF_SKIP -> markAndUpdate(context, completed = false, dismissNotification = true)
        }
    }

    private fun markAndUpdate(context: Context, completed: Boolean, dismissNotification: Boolean = false) {
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefs = PreferencesDataSource(appContext)
                val history = HistoryDataSource(appContext)
                val repo = AppRepository(prefs, history)
                repo.markTodaysResult(completed)
                val records = repo.getRecordsList()
                val todayKey = repo.todayKey()
                val todayRecord = records.find { it.date == todayKey }
                val goalText = todayRecord?.challengeText ?: run {
                    val pending = prefs.getPendingChallenges()
                    pending.find { it.date == todayKey }?.challengeText ?: ""
                }
                val streak = repo.getCurrentStreak(records)
                val theme = prefs.preferences.first().theme
                WidgetHelper.save(appContext, goalText, todayKey, completed, streak, theme)
                DailyGoalWidget.updateAll(appContext)
                if (dismissNotification) {
                    NotificationManagerCompat.from(appContext).cancel(NOTIFICATION_ID_DAILY)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        fun setPendingIntents(context: Context, views: RemoteViews, appWidgetId: Int) {
            val doneIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = ACTION_WIDGET_DONE
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val skipIntent = Intent(context, WidgetActionReceiver::class.java).apply {
                action = ACTION_WIDGET_SKIP
                putExtra(EXTRA_APPWIDGET_ID, appWidgetId)
            }
            val donePending = android.app.PendingIntent.getBroadcast(
                context, 10000 + appWidgetId * 2, doneIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            val skipPending = android.app.PendingIntent.getBroadcast(
                context, 10000 + appWidgetId * 2 + 1, skipIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_done, donePending)
            views.setOnClickPendingIntent(R.id.widget_btn_skip, skipPending)
        }
    }
}
