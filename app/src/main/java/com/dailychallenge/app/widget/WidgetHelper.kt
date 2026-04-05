package com.dailychallenge.app.widget

import android.content.Context
import android.content.SharedPreferences

/**
 * Данные для виджета «Цель дня». Приложение записывает их при открытии главного экрана и при смене цели/отметке.
 */
data class WidgetData(
    val goalText: String,
    val dateKey: String,
    val isDone: Boolean,
    val streak: Int
)

object WidgetHelper {
    private const val PREFS_NAME = "widget_daily_goal"
    private const val KEY_GOAL_TEXT = "goal_text"
    private const val KEY_DATE = "date"
    private const val KEY_IS_DONE = "is_done"
    private const val KEY_STREAK = "streak"
    private const val KEY_THEME = "theme"

    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context, goalText: String, dateKey: String, isDone: Boolean, streak: Int, theme: String = "system") {
        prefs(context).edit()
            .putString(KEY_GOAL_TEXT, goalText)
            .putString(KEY_DATE, dateKey)
            .putBoolean(KEY_IS_DONE, isDone)
            .putInt(KEY_STREAK, streak)
            .putString(KEY_THEME, theme)
            .apply()
    }

    fun load(context: Context): WidgetData {
        val p = prefs(context)
        return WidgetData(
            goalText = p.getString(KEY_GOAL_TEXT, "").orEmpty(),
            dateKey = p.getString(KEY_DATE, "").orEmpty(),
            isDone = p.getBoolean(KEY_IS_DONE, false),
            streak = p.getInt(KEY_STREAK, 0)
        )
    }

    fun loadTheme(context: Context): String =
        prefs(context).getString(KEY_THEME, "system").orEmpty()
}
