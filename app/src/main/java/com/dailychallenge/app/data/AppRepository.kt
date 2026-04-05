package com.dailychallenge.app.data

import android.app.Activity
import com.dailychallenge.app.sync.PlayGamesSync
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

class AppRepository(
    internal val prefs: PreferencesDataSource,
    internal val history: HistoryDataSource,
) {

    fun createPlayGamesSync(activity: Activity) = PlayGamesSync(activity, history, prefs)
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    fun todayKey() = LocalDate.now().format(formatter)

    val preferences: Flow<UserPreferences> = prefs.preferences
    val records: Flow<List<DayRecord>> = history.records
    /** Очередь невыполненных целей (премиум до 3). При выполнении засчитывается день выдачи. */
    val pendingChallenges: Flow<List<PendingChallenge>> = prefs.pendingChallenges

    suspend fun getRecordsList(): List<DayRecord> = history.getRecords()

    suspend fun getSelectedCategories(): Set<String> = prefs.preferences.first().selectedCategoryIds

    suspend fun getTodaysRecord(): DayRecord? {
        val list = history.getRecords()
        return list.find { it.date == todayKey() }
    }

    suspend fun getRecordByDate(dateStr: String): DayRecord? =
        history.getRecords().find { it.date == dateStr }

    /**
     * Добавить сегодня в очередь невыполненных, если нужно: премиум — до 3 целей, бесплатно — только сегодня.
     * resolvedLanguageCode: при "Системный" передать язык устройства (en/ru), иначе цели не совпадут с интерфейсом.
     */
    suspend fun ensureTodayInPending(resolvedLanguageCode: String? = null) {
        val userPrefs = prefs.preferences.first()
        val lang = resolvedLanguageCode ?: userPrefs.languageCode
        val cats = userPrefs.selectedCategoryIds
        if (cats.isEmpty()) return
        val records = history.getRecords()
        var pending = prefs.getPendingChallenges().filter { p -> records.none { it.date == p.date } }
        if (!userPrefs.isPremium) pending = pending.filter { it.date == todayKey() }
        if (pending.any { it.date == todayKey() }) {
            if (pending != prefs.getPendingChallenges()) prefs.setPendingChallenges(pending)
            return
        }
        if (records.any { it.date == todayKey() }) return
        if (userPrefs.isPremium && pending.size >= 3) return
        val picked = pickChallengeForToday(cats, lang) ?: return
        prefs.setPendingChallenges(pending + PendingChallenge(todayKey(), picked.first, picked.second))
    }

    /** Отметить результат по дате выдачи цели; день выдачи засчитывается в календаре. Удаляет цель из очереди. */
    suspend fun markResultForDate(date: String, completed: Boolean) {
        val pending = prefs.getPendingChallenges()
        val entry = pending.find { it.date == date } ?: return
        val existing = getRecordByDate(date)
        history.addOrUpdateRecord(
            DayRecord(
                date = date,
                challengeText = entry.challengeText,
                categoryId = entry.categoryId,
                completed = completed,
                usedFreeze = existing?.usedFreeze ?: false
            )
        )
        prefs.setPendingChallenges(pending.filter { it.date != date })
    }

    /** Добавить заметку к записи за дату. */
    suspend fun addNoteToRecord(date: String, note: String) {
        val record = getRecordByDate(date) ?: return
        history.addOrUpdateRecord(record.copy(note = note))
    }

    /** Отменить отметку: удалить запись из истории и вернуть цель в очередь. */
    suspend fun undoMarkResult(date: String, entry: PendingChallenge) {
        history.removeRecord(date)
        val pending = prefs.getPendingChallenges()
        if (pending.none { it.date == date }) {
            prefs.setPendingChallenges(pending + entry)
        }
    }

    /** Текущая цель на сегодня (обратная совместимость). Предпочитает запись, иначе первая в очереди на сегодня. */
    suspend fun getOrCreateTodaysChallenge(): Pair<String, String>? {
        ensureTodayInPending()
        val existing = getTodaysRecord()
        if (existing != null) return existing.categoryId to existing.challengeText
        val pending = prefs.getPendingChallenges()
        val todayEntry = pending.find { it.date == todayKey() }
        return todayEntry?.let { it.categoryId to it.challengeText }
    }

    /** Выбрать новую цель на сегодня из выбранных категорий + свои (Premium), избегая недавних. */
    suspend fun pickChallengeForToday(categoryIds: Set<String>, languageCode: String? = null): Pair<String, String>? {
        val difficulty = prefs.preferences.first().difficulty
        var pool = Challenges.getAllChallengesForCategories(categoryIds, languageCode, difficulty).toMutableList()
        val custom = prefs.getCustomGoals().filter { it.first in categoryIds }
        pool.addAll(custom)
        if (pool.isEmpty()) return null
        val recent = history.getRecords().takeLast(14).map { it.challengeText }.toSet()
        val available = pool.filter { it.second !in recent }
        val list = if (available.isEmpty()) pool else available
        return list.random()
    }

    /** До 4 альтернативных целей на выбор (для премиума). В рамках дня одни и те же 4, пока не выберут — без читерства. */
    suspend fun pickAlternativeChallenges(count: Int, resolvedLanguageCode: String? = null): List<Pair<String, String>> {
        val userPrefs = prefs.preferences.first()
        val lang = resolvedLanguageCode ?: userPrefs.languageCode
        val cachedDate = prefs.getCachedAlternativesDate()
        if (cachedDate == todayKey()) {
            val cached = prefs.getCachedAlternativesList()
            if (cached.size == count) return cached
        }
        val cats = userPrefs.selectedCategoryIds
        if (cats.isEmpty()) return emptyList()
        var pool = Challenges.getAllChallengesForCategories(cats, lang).toMutableList()
        val custom = prefs.getCustomGoals().filter { it.first in cats }
        pool.addAll(custom)
        if (pool.isEmpty()) return emptyList()
        val pendingTexts = prefs.getPendingChallenges().map { it.challengeText }.toSet()
        val recent = history.getRecords().takeLast(14).map { it.challengeText }.toSet()
        val exclude = recent + pendingTexts
        val available = pool.filter { it.second !in exclude }.distinctBy { it.second }
        val list = if (available.size <= count) available.shuffled() else available.shuffled().take(count)
        prefs.setCachedAlternatives(todayKey(), list)
        return list
    }

    /**
     * При смене языка: перевести тексты в очереди через параллельные индексы.
     * Если перевод невозможен (пользовательская цель / списки разной длины) — перегенерировать случайно.
     */
    suspend fun retranslateOrRepickPending(newLanguageCode: String) {
        val pending = prefs.getPendingChallenges()
        if (pending.isEmpty()) return
        val cats = prefs.preferences.first().selectedCategoryIds
        val newPending = pending.map { p ->
            val translated = Challenges.translateChallenge(p.challengeText, p.categoryId, newLanguageCode)
            if (translated != null) {
                p.copy(challengeText = translated)
            } else {
                val picked = pickChallengeForToday(cats, newLanguageCode)
                if (picked != null) PendingChallenge(p.date, picked.first, picked.second) else p
            }
        }
        prefs.setPendingChallenges(newPending)
        prefs.clearCachedAlternatives()
    }

    /** Записать цель на дату (для обратной совместимости и заморозки). */
    suspend fun setTodaysChallenge(categoryId: String, text: String, completed: Boolean? = null) {
        val keepCompleted = completed ?: getTodaysRecord()?.completed ?: false
        history.addOrUpdateRecord(
            DayRecord(date = todayKey(), challengeText = text, categoryId = categoryId, completed = keepCompleted)
        )
    }

    /** Отметить результат за сегодня. Если запись уже есть (например «не выполнил») — обновляем её. */
    suspend fun markTodaysResult(completed: Boolean) {
        val r = getTodaysRecord()
        if (r != null) {
            history.addOrUpdateRecord(r.copy(completed = completed))
            val pending = prefs.getPendingChallenges().filter { it.date != todayKey() }
            prefs.setPendingChallenges(pending)
            return
        }
        markResultForDate(todayKey(), completed)
    }

    /** Один раз в день: бесплатно — 1 замена (случайная), премиум — 1 выбор из 4. */
    suspend fun canReplaceToday(): Boolean {
        val lastDate = prefs.getLastReplaceOrChoiceDate()
        return lastDate != todayKey()
    }

    /** Заменить цель на одну случайную. Только для бесплатных; премиум всегда идёт через выбор из 4 (choosePendingChallenge). */
    suspend fun replacePendingChallenge(date: String, resolvedLanguageCode: String? = null): Boolean {
        val userPrefs = prefs.preferences.first()
        if (userPrefs.isPremium) return false
        if (date != todayKey()) return false
        val cats = userPrefs.selectedCategoryIds
        if (cats.isEmpty()) return false
        val pending = prefs.getPendingChallenges()
        if (pending.none { it.date == date }) return false
        val lang = resolvedLanguageCode ?: userPrefs.languageCode
        val (cat, text) = pickChallengeForToday(cats, lang) ?: return false
        val newList = pending.map { if (it.date == date) PendingChallenge(date, cat, text) else it }
        prefs.setPendingChallenges(newList)
        prefs.setLastReplaceOrChoiceDate(todayKey())
        return true
    }

    /** Установить выбранную цель для даты (премиум: выбор из 4, 1 раз в день — любую цель в очереди). */
    suspend fun choosePendingChallenge(date: String, categoryId: String, text: String) {
        val userPrefs = prefs.preferences.first()
        if (!userPrefs.isPremium && date != todayKey()) return
        val pending = prefs.getPendingChallenges()
        if (pending.none { it.date == date }) return
        val newList = pending.map { if (it.date == date) PendingChallenge(date, categoryId, text) else it }
        prefs.setPendingChallenges(newList)
        prefs.setLastReplaceOrChoiceDate(todayKey())
        prefs.clearCachedAlternatives()
    }

    fun getCurrentStreak(records: List<DayRecord>): Int {
        val byDate = records.associateBy { it.date }
        var streak = 0
        var date = todayKey()
        while (true) {
            val r = byDate[date] ?: break
            if (!r.completed && !r.usedFreeze) break
            streak++
            date = previousDay(date)
        }
        return streak
    }

    fun getBestStreak(records: List<DayRecord>): Int {
        if (records.isEmpty()) return 0
        val keeping = records.filter { it.completed || it.usedFreeze }.sortedBy { it.date }
        if (keeping.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until keeping.size) {
            val prev = LocalDate.parse(keeping[i - 1].date)
            val curr = LocalDate.parse(keeping[i].date)
            if (ChronoUnit.DAYS.between(prev, curr) == 1L) {
                current++
                best = maxOf(best, current)
            } else {
                current = 1
            }
        }
        return best
    }

    private fun yesterdayKey() = LocalDate.now().minusDays(1).format(formatter)
    private fun previousDay(dateStr: String) = LocalDate.parse(dateStr).minusDays(1).format(formatter)

    fun monthStats(records: List<DayRecord>): Pair<Int, Int> {
        val start = LocalDate.now().withDayOfMonth(1)
        val end = LocalDate.now()
        val inMonth = records.filter { r ->
            val d = LocalDate.parse(r.date)
            !d.isBefore(start) && !d.isAfter(end)
        }
        val daysInMonth = end.lengthOfMonth()
        val completed = inMonth.count { it.completed }
        return completed to daysInMonth
    }

    /** За текущую неделю (пн–вс): выполнено из 7 */
    fun weekStats(records: List<DayRecord>): Pair<Int, Int> {
        val now = LocalDate.now()
        val monday = now.minusDays(now.dayOfWeek.value - 1L)
        val sunday = monday.plusDays(6)
        val inWeek = records.filter { r ->
            val d = LocalDate.parse(r.date)
            !d.isBefore(monday) && !d.isAfter(sunday)
        }
        return inWeek.count { it.completed } to 7
    }

    /** По категориям: сколько целей выполнено в каждой. */
    fun completedByCategory(records: List<DayRecord>): Map<String, Int> =
        records.filter { it.completed }.groupBy { it.categoryId }.mapValues { it.value.size }

    /** За текущий год: дата (ISO) → выполнено ли. Для «год в пикселях». */
    fun yearCompletionMap(records: List<DayRecord>): Map<String, Boolean> {
        val year = LocalDate.now().year
        return records
            .filter { LocalDate.parse(it.date).year == year }
            .associate { it.date to it.completed }
    }

    /** Выполнено по дням недели (1=Пн..7=Вс). */
    fun completionByDayOfWeek(records: List<DayRecord>): Map<Int, Int> {
        return records.filter { it.completed }.groupBy {
            LocalDate.parse(it.date).dayOfWeek.value
        }.mapValues { it.value.size }
    }

    /** Прогресс по неделям (последние 12 недель): список (weekLabel, count). */
    fun weeklyProgress(records: List<DayRecord>): List<Pair<String, Int>> {
        val now = LocalDate.now()
        val weekFormatter = DateTimeFormatter.ofPattern("dd.MM")
        return (11 downTo 0).map { weeksAgo ->
            val monday = now.minusWeeks(weeksAgo.toLong()).let { it.minusDays(it.dayOfWeek.value - 1L) }
            val sunday = monday.plusDays(6)
            val count = records.count { r ->
                val d = LocalDate.parse(r.date)
                r.completed && !d.isBefore(monday) && !d.isAfter(sunday)
            }
            monday.format(weekFormatter) to count
        }
    }

    /** Осталось замен/выборов сегодня: 0 или 1. */
    suspend fun getReplaceOrChoiceRemainingToday(): Int = if (canReplaceToday()) 1 else 0

    /** Можно ли использовать заморозку сегодня (Premium, есть заморозки, сегодня отмечено «не выполнил» и ещё не заморожено). */
    suspend fun canUseFreezeToday(): Boolean {
        val userPrefs = prefs.preferences.first()
        if (!userPrefs.isPremium) return false
        if (prefs.getFreezeCount() <= 0) return false
        val r = getTodaysRecord() ?: return false
        return !r.completed && !r.usedFreeze
    }

    suspend fun getFreezeCount(): Int = prefs.getFreezeCount()

    /** Использовать заморозку за сегодня: стрик не сгорает. */
    suspend fun useFreezeForToday(): Boolean {
        val r = getTodaysRecord() ?: return false
        if (r.usedFreeze) return false
        val count = prefs.getFreezeCount()
        if (count <= 0) return false
        prefs.setFreezeCount(count - 1)
        history.addOrUpdateRecord(r.copy(usedFreeze = true))
        return true
    }
}
