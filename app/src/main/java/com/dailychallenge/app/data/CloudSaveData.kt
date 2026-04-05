package com.dailychallenge.app.data

import com.google.gson.annotations.SerializedName

/**
 * Данные для облачного сохранения (Google Play Saved Games).
 * Сериализуется в JSON и сохраняется в снимок.
 */
data class CloudSaveData(
    @SerializedName("v") val version: Int = 1,
    @SerializedName("records") val records: List<DayRecord> = emptyList(),
    @SerializedName("prefs") val prefs: CloudPrefsData = CloudPrefsData()
)

data class CloudPrefsData(
    @SerializedName("reminder_hour") val reminderHour: Int = 9,
    @SerializedName("reminder_minute") val reminderMinute: Int = 0,
    @SerializedName("categories") val selectedCategoryIds: Set<String> = setOf("health", "productivity"),
    @SerializedName("onboarding_done") val onboardingDone: Boolean = false,
    @SerializedName("theme") val theme: String = "system",
    @SerializedName("sound") val soundEnabled: Boolean = true,
    @SerializedName("vibration") val vibrationEnabled: Boolean = true,
    @SerializedName("premium") val isPremium: Boolean = false,
    @SerializedName("second_reminder_hour") val secondReminderHour: Int? = null,
    @SerializedName("second_reminder_minute") val secondReminderMinute: Int? = null,
    @SerializedName("weekdays_only") val remindWeekdaysOnly: Boolean = false,
    @SerializedName("language") val languageCode: String? = null,
    @SerializedName("difficulty") val difficulty: String? = null,
    @SerializedName("custom_goals") val customGoals: List<CustomGoalJson> = emptyList(),
    @SerializedName("pending") val pendingChallenges: List<PendingChallenge> = emptyList(),
    @SerializedName("freeze_count") val freezeCount: Int = 0
)

data class CustomGoalJson(
    @SerializedName("cat") val catId: String = "",
    @SerializedName("text") val text: String = ""
)

fun CloudPrefsData.customGoalsAsPairs(): List<Pair<String, String>> =
    customGoals.map { it.catId to it.text }
