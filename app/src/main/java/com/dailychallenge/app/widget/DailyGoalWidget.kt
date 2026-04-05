package com.dailychallenge.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import com.dailychallenge.app.MainActivity
import com.dailychallenge.app.R

class DailyGoalWidget : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, DailyGoalWidget::class.java)
            )
            if (ids.isNotEmpty()) {
                DailyGoalWidget().onUpdate(context, manager, ids)
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_daily_goal)
            val data = WidgetHelper.load(context)

            views.setTextViewText(R.id.widget_title, context.getString(R.string.widget_title))
            if (data.streak > 0) {
                views.setTextViewText(R.id.widget_streak, context.getString(R.string.widget_streak, data.streak))
                views.setViewVisibility(R.id.widget_streak, android.view.View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_streak, android.view.View.GONE)
            }
            val goalDisplay = when {
                data.goalText.isNotEmpty() && data.isDone -> data.goalText + "\n" + context.getString(R.string.home_completed)
                data.goalText.isNotEmpty() -> data.goalText
                data.isDone -> context.getString(R.string.home_completed)
                else -> context.getString(R.string.widget_goal_placeholder)
            }
            views.setTextViewText(R.id.widget_goal, goalDisplay)
            views.setViewVisibility(R.id.widget_btn_done, if (data.isDone) android.view.View.GONE else android.view.View.VISIBLE)
            views.setViewVisibility(R.id.widget_btn_skip, if (data.isDone) android.view.View.GONE else android.view.View.VISIBLE)

            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPending = PendingIntent.getActivity(
                context, 0, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPending)

            applyTheme(context, views)
            WidgetActionReceiver.setPendingIntents(context, views, appWidgetId)
            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun applyTheme(context: Context, views: RemoteViews) {
            var theme = WidgetHelper.loadTheme(context)
            if (theme == "system") {
                val isDark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
                theme = if (isDark) "dark" else "light"
            }
            val (bgRes, textPrimaryRes, textSecondaryRes) = when (theme) {
                "amoled" -> Triple(R.color.widget_bg_amoled, R.color.widget_text_primary_amoled, R.color.widget_text_secondary_amoled)
                "dark" -> Triple(R.color.widget_bg_dark, R.color.widget_text_primary_dark, R.color.widget_text_secondary_dark)
                else -> Triple(R.color.widget_bg_light, R.color.widget_text_primary_light, R.color.widget_text_secondary_light)
            }
            val bgColor = ContextCompat.getColor(context, bgRes)
            val textPrimary = ContextCompat.getColor(context, textPrimaryRes)
            val textSecondary = ContextCompat.getColor(context, textSecondaryRes)
            views.setInt(R.id.widget_root, "setBackgroundColor", bgColor)
            views.setTextColor(R.id.widget_goal, textPrimary)
            views.setTextColor(R.id.widget_title, textSecondary)
            views.setTextColor(R.id.widget_streak, textSecondary)
        }
    }
}
