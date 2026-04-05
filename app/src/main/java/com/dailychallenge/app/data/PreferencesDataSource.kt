package com.dailychallenge.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
private val gsonPending = Gson()
private val typePending = object : TypeToken<List<PendingChallenge>>() {}.type

class PreferencesDataSource(private val context: Context) {
    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            reminderHour = prefs[KEY_REMINDER_HOUR] ?: 9,
            reminderMinute = prefs[KEY_REMINDER_MINUTE] ?: 0,
            selectedCategoryIds = prefs[KEY_CATEGORIES] ?: setOf("health", "productivity"),
            onboardingDone = prefs[KEY_ONBOARDING_DONE] ?: false,
            theme = prefs[KEY_THEME] ?: "system",
            soundEnabled = prefs[KEY_SOUND] ?: true,
            vibrationEnabled = prefs[KEY_VIBRATION] ?: true,
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            secondReminderHour = prefs[KEY_SECOND_REMINDER_HOUR],
            secondReminderMinute = prefs[KEY_SECOND_REMINDER_MINUTE],
            remindWeekdaysOnly = prefs[KEY_REMIND_WEEKDAYS_ONLY] ?: false,
            difficulty = prefs[KEY_DIFFICULTY],
            languageCode = prefs[KEY_LANGUAGE],
        )
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        context.dataStore.edit { it[KEY_REMINDER_HOUR] = hour; it[KEY_REMINDER_MINUTE] = minute }
    }

    suspend fun setSelectedCategories(ids: Set<String>) {
        context.dataStore.edit { it[KEY_CATEGORIES] = ids }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_SOUND] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_VIBRATION] = enabled }
    }

    suspend fun setPremium(premium: Boolean) {
        context.dataStore.edit {
            val wasPremium = it[KEY_IS_PREMIUM] == true
            it[KEY_IS_PREMIUM] = premium
            if (premium && !wasPremium) it[KEY_FREEZE_COUNT] = 3
        }
    }

    /** Заморозки стрика (Premium): сколько осталось. */
    suspend fun getFreezeCount(): Int = context.dataStore.data.first()[KEY_FREEZE_COUNT] ?: 0
    suspend fun setFreezeCount(count: Int) {
        context.dataStore.edit { it[KEY_FREEZE_COUNT] = count.coerceAtLeast(0) }
    }

    /** Использованные на этом устройстве промокоды (чтобы один код не активировали дважды локально). */
    suspend fun getUsedPromoCodes(): Set<String> =
        context.dataStore.data.first()[KEY_USED_PROMO_CODES] ?: emptySet()

    /**
     * Активировать промокод. Если код верный и ещё не использован на этом устройстве —
     * выставляет Premium и запоминает код. Иначе возвращает false.
     * Позже: при наличии backend проверку делать на сервере и привязывать Premium к аккаунту.
     */
    suspend fun redeemPromoCode(code: String): Boolean {
        val normalized = code.uppercase().trim().ifBlank { return false }
        if (!PromoCodes.isValid(normalized)) return false
        val used = getUsedPromoCodes()
        if (normalized in used) return false
        context.dataStore.edit {
            it[KEY_USED_PROMO_CODES] = used + normalized
            it[KEY_IS_PREMIUM] = true
            it[KEY_FREEZE_COUNT] = 3
        }
        return true
    }

    suspend fun setSecondReminder(hour: Int?, minute: Int?) {
        context.dataStore.edit {
            if (hour != null) it[KEY_SECOND_REMINDER_HOUR] = hour else it.remove(KEY_SECOND_REMINDER_HOUR)
            if (minute != null) it[KEY_SECOND_REMINDER_MINUTE] = minute else it.remove(KEY_SECOND_REMINDER_MINUTE)
        }
    }

    suspend fun setRemindWeekdaysOnly(only: Boolean) {
        context.dataStore.edit { it[KEY_REMIND_WEEKDAYS_ONLY] = only }
    }

    suspend fun setDifficulty(diff: String?) {
        context.dataStore.edit {
            if (diff != null) it[KEY_DIFFICULTY] = diff else it.remove(KEY_DIFFICULTY)
        }
    }

    suspend fun setLanguage(code: String?) {
        context.dataStore.edit {
            if (code != null) it[KEY_LANGUAGE] = code else it.remove(KEY_LANGUAGE)
        }
        context.getSharedPreferences("locale_prefs", android.content.Context.MODE_PRIVATE)
            .edit().apply {
                if (code != null) putString("lang", code) else remove("lang")
            }.apply()
    }

    /** Очередь невыполненных целей (премиум до 3). Каждая с датой выдачи. */
    val pendingChallenges: Flow<List<PendingChallenge>> = context.dataStore.data.map { prefs ->
        val json = prefs[KEY_PENDING_CHALLENGES] ?: "[]"
        (gsonPending.fromJson(json, typePending) as? List<PendingChallenge>) ?: emptyList()
    }
    suspend fun getPendingChallenges(): List<PendingChallenge> = pendingChallenges.first()
    suspend fun setPendingChallenges(list: List<PendingChallenge>) {
        context.dataStore.edit { it[KEY_PENDING_CHALLENGES] = gsonPending.toJson(list) }
    }

    /** Кэш 4 целей на замену: в рамках дня одни и те же, пока пользователь не выберет одну. */
    suspend fun getCachedAlternativesDate(): String? = context.dataStore.data.first()[KEY_CACHED_ALTERNATIVES_DATE]
    suspend fun getCachedAlternativesList(): List<Pair<String, String>> {
        val raw = context.dataStore.data.first()[KEY_CACHED_ALTERNATIVES_LIST] ?: return emptyList()
        return raw.lines().mapNotNull { line ->
            val i = line.indexOf('|')
            if (i > 0) line.substring(0, i) to line.substring(i + 1) else null
        }
    }
    suspend fun setCachedAlternatives(date: String, list: List<Pair<String, String>>) {
        context.dataStore.edit {
            it[KEY_CACHED_ALTERNATIVES_DATE] = date
            it[KEY_CACHED_ALTERNATIVES_LIST] = list.joinToString("\n") { "${it.first}|${it.second}" }
        }
    }
    suspend fun clearCachedAlternatives() {
        context.dataStore.edit {
            it.remove(KEY_CACHED_ALTERNATIVES_DATE)
            it.remove(KEY_CACHED_ALTERNATIVES_LIST)
        }
    }

    /** Дата последней замены/выбора цели (ISO date). Один раз в день. */
    suspend fun getLastReplaceOrChoiceDate(): String? = context.dataStore.data.first()[KEY_LAST_REPLACE_OR_CHOICE_DATE]
    suspend fun setLastReplaceOrChoiceDate(date: String) {
        context.dataStore.edit { it[KEY_LAST_REPLACE_OR_CHOICE_DATE] = date }
    }

    suspend fun setLastReplaceWeek(weekKey: String) {
        context.dataStore.edit { it[KEY_LAST_REPLACE_WEEK] = weekKey }
    }

    suspend fun setReplaceCountThisWeek(count: Int) {
        context.dataStore.edit { it[KEY_REPLACE_COUNT_WEEK] = count }
    }

    suspend fun getLastReplaceWeek(): String? = context.dataStore.data.first()[KEY_LAST_REPLACE_WEEK]
    suspend fun getReplaceCountThisWeek(): Int = context.dataStore.data.first()[KEY_REPLACE_COUNT_WEEK] ?: 0

    companion object {
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        private val KEY_CATEGORIES = stringSetPreferencesKey("categories")
        private val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        private val KEY_THEME = stringPreferencesKey("theme")
        private val KEY_SOUND = booleanPreferencesKey("sound_enabled")
        private val KEY_VIBRATION = booleanPreferencesKey("vibration_enabled")
        private val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        private val KEY_USED_PROMO_CODES = stringSetPreferencesKey("used_promo_codes")
        private val KEY_SECOND_REMINDER_HOUR = intPreferencesKey("second_reminder_hour")
        private val KEY_SECOND_REMINDER_MINUTE = intPreferencesKey("second_reminder_minute")
        private val KEY_REMIND_WEEKDAYS_ONLY = booleanPreferencesKey("remind_weekdays_only")
        private val KEY_DIFFICULTY = stringPreferencesKey("difficulty")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_FREEZE_COUNT = intPreferencesKey("freeze_count")
        private val KEY_PENDING_CHALLENGES = stringPreferencesKey("pending_challenges")
        private val KEY_CACHED_ALTERNATIVES_DATE = stringPreferencesKey("cached_alternatives_date")
        private val KEY_CACHED_ALTERNATIVES_LIST = stringPreferencesKey("cached_alternatives_list")
        private val KEY_LAST_REPLACE_OR_CHOICE_DATE = stringPreferencesKey("last_replace_or_choice_date")
        private val KEY_LAST_REPLACE_WEEK = stringPreferencesKey("last_replace_week")
        private val KEY_REPLACE_COUNT_WEEK = intPreferencesKey("replace_count_week")
        val KEY_CUSTOM_GOALS = stringPreferencesKey("custom_goals")
    }

    /** Формат: "categoryId|text" по одной на строку */
    suspend fun getCustomGoals(): List<Pair<String, String>> {
        val raw = context.dataStore.data.first()[KEY_CUSTOM_GOALS] ?: return emptyList()
        return raw.lines().mapNotNull { line ->
            val idx = line.indexOf('|')
            if (idx > 0) line.substring(0, idx) to line.substring(idx + 1).trim() else null
        }.filter { it.second.isNotBlank() }
    }

    suspend fun setCustomGoals(goals: List<Pair<String, String>>) {
        val value = goals.joinToString("\n") { "${it.first}|${it.second}" }
        context.dataStore.edit { it[KEY_CUSTOM_GOALS] = value }
    }

    /** Применить настройки из облачного снимка (восстановление прогресса). */
    suspend fun applyFromCloud(data: com.dailychallenge.app.data.CloudPrefsData) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_HOUR] = data.reminderHour
            prefs[KEY_REMINDER_MINUTE] = data.reminderMinute
            prefs[KEY_CATEGORIES] = data.selectedCategoryIds
            prefs[KEY_ONBOARDING_DONE] = data.onboardingDone
            prefs[KEY_THEME] = data.theme
            prefs[KEY_SOUND] = data.soundEnabled
            prefs[KEY_VIBRATION] = data.vibrationEnabled
            prefs[KEY_IS_PREMIUM] = data.isPremium
            prefs[KEY_FREEZE_COUNT] = data.freezeCount.coerceAtLeast(0)
            prefs[KEY_REMIND_WEEKDAYS_ONLY] = data.remindWeekdaysOnly
            if (data.secondReminderHour != null) prefs[KEY_SECOND_REMINDER_HOUR] = data.secondReminderHour
            else prefs.remove(KEY_SECOND_REMINDER_HOUR)
            if (data.secondReminderMinute != null) prefs[KEY_SECOND_REMINDER_MINUTE] = data.secondReminderMinute
            else prefs.remove(KEY_SECOND_REMINDER_MINUTE)
            if (data.languageCode != null) prefs[KEY_LANGUAGE] = data.languageCode
            else prefs.remove(KEY_LANGUAGE)
            if (data.difficulty != null) prefs[KEY_DIFFICULTY] = data.difficulty
            else prefs.remove(KEY_DIFFICULTY)
        }
        context.getSharedPreferences("locale_prefs", android.content.Context.MODE_PRIVATE)
            .edit().apply {
                if (data.languageCode != null) putString("lang", data.languageCode) else remove("lang")
            }.apply()
        setCustomGoals(data.customGoalsAsPairs())
        setPendingChallenges(data.pendingChallenges)
    }
}
