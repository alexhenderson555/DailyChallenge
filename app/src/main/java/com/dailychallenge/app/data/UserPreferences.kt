package com.dailychallenge.app.data

data class UserPreferences(
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val selectedCategoryIds: Set<String> = setOf("health", "productivity"),
    val onboardingDone: Boolean = false,
    val theme: String = "system",
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val isPremium: Boolean = false,
    val secondReminderHour: Int? = null,
    val secondReminderMinute: Int? = null,
    val remindWeekdaysOnly: Boolean = false,
    val difficulty: String? = null,
    val languageCode: String? = null, // null = system, "ru", "en"
)
