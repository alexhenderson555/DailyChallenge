package com.dailychallenge.app.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.dailychallenge.app.data.AppRepository
import com.dailychallenge.app.data.HistoryDataSource
import com.dailychallenge.app.data.PreferencesDataSource
import com.dailychallenge.app.util.AlarmHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MidnightReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val prefsDs = PreferencesDataSource(context)
                val historyDs = HistoryDataSource(context)
                val repo = AppRepository(prefsDs, historyDs)

                val prefs = prefsDs.preferences.first()
                repo.ensureTodayInPending(prefs.languageCode)

                val pending = prefsDs.getPendingChallenges()
                val todayKey = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                val todayChallenge = pending.find { it.date == todayKey }
                val records = historyDs.records.first()
                val streak = repo.getCurrentStreak(records)

                WidgetHelper.save(
                    context,
                    goalText = todayChallenge?.challengeText ?: "",
                    dateKey = todayKey,
                    isDone = false,
                    streak = streak,
                    theme = WidgetHelper.loadTheme(context)
                )
                DailyGoalWidget.updateAll(context)
            } catch (e: Exception) {
                timber.log.Timber.e(e, "MidnightReceiver failed")
            } finally {
                pendingResult.finish()
            }
        }
        scheduleMidnightRefresh(context)
    }

    companion object {
        private const val ACTION_MIDNIGHT = "com.dailychallenge.app.MIDNIGHT_REFRESH"

        fun scheduleMidnightRefresh(context: Context) {
            val cal = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 5)
                set(java.util.Calendar.MILLISECOND, 0)
                add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            AlarmHelper.scheduleExactAlarm(
                context, cal.timeInMillis, 9999, MidnightReceiver::class.java, ACTION_MIDNIGHT
            )
        }
    }
}
