package com.dailychallenge.app.data

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class StreakAndStatsTest {

    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    private fun record(daysAgo: Long, completed: Boolean = true, frozen: Boolean = false) =
        DayRecord(
            date = LocalDate.now().minusDays(daysAgo).format(formatter),
            challengeText = "Test",
            categoryId = "health",
            completed = completed,
            usedFreeze = frozen
        )

    private fun todayKey() = LocalDate.now().format(formatter)
    private fun previousDay(dateStr: String) = LocalDate.parse(dateStr).minusDays(1).format(formatter)

    private fun getCurrentStreak(records: List<DayRecord>): Int {
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

    private fun getBestStreak(records: List<DayRecord>): Int {
        if (records.isEmpty()) return 0
        val keeping = records.filter { it.completed || it.usedFreeze }.sortedBy { it.date }
        if (keeping.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until keeping.size) {
            val prev = LocalDate.parse(keeping[i - 1].date)
            val curr = LocalDate.parse(keeping[i].date)
            if (java.time.temporal.ChronoUnit.DAYS.between(prev, curr) == 1L) {
                current++
                best = maxOf(best, current)
            } else {
                current = 1
            }
        }
        return best
    }

    @Test
    fun `streak is 0 for empty records`() {
        assertEquals(0, getCurrentStreak(emptyList()))
    }

    @Test
    fun `streak counts consecutive completed days from today`() {
        val records = listOf(record(0), record(1), record(2))
        assertEquals(3, getCurrentStreak(records))
    }

    @Test
    fun `streak breaks on gap day`() {
        val records = listOf(record(0), record(1), record(3))
        assertEquals(2, getCurrentStreak(records))
    }

    @Test
    fun `streak includes frozen days`() {
        val records = listOf(record(0), record(1, completed = false, frozen = true), record(2))
        assertEquals(3, getCurrentStreak(records))
    }

    @Test
    fun `streak breaks on not completed and not frozen`() {
        val records = listOf(record(0), record(1, completed = false), record(2))
        assertEquals(1, getCurrentStreak(records))
    }

    @Test
    fun `streak is 0 if today has no record`() {
        val records = listOf(record(1), record(2))
        assertEquals(0, getCurrentStreak(records))
    }

    @Test
    fun `best streak finds the longest run`() {
        val records = listOf(
            record(10), record(9), record(8), record(7), record(6),
            record(3), record(2)
        )
        assertEquals(5, getBestStreak(records))
    }

    @Test
    fun `best streak returns 0 for empty`() {
        assertEquals(0, getBestStreak(emptyList()))
    }

    @Test
    fun `best streak returns 1 for single record`() {
        assertEquals(1, getBestStreak(listOf(record(5))))
    }

    @Test
    fun `best streak ignores not completed records`() {
        val records = listOf(record(3), record(2, completed = false), record(1))
        assertEquals(1, getBestStreak(records))
    }

    @Test
    fun `difficulty filter returns subsets`() {
        val all = Challenges.getAllChallengesForCategories(setOf("health"), "en", null)
        val easy = Challenges.getAllChallengesForCategories(setOf("health"), "en", "easy")
        val medium = Challenges.getAllChallengesForCategories(setOf("health"), "en", "medium")
        val hard = Challenges.getAllChallengesForCategories(setOf("health"), "en", "hard")
        assertTrue("Easy should be non-empty", easy.isNotEmpty())
        assertTrue("Medium should be non-empty", medium.isNotEmpty())
        assertTrue("Hard should be non-empty", hard.isNotEmpty())
        assertTrue("Easy should be subset of all", easy.size < all.size)
    }

    @Test
    fun `translateChallenge works for known challenges`() {
        val ruHealth = Challenges.getAllChallengesForCategories(setOf("health"), "ru", null)
        if (ruHealth.isNotEmpty()) {
            val (cat, text) = ruHealth.first()
            val translated = Challenges.translateChallenge(text, cat, "en")
            assertNotNull("Translation should exist", translated)
        }
    }

    @Test
    fun `weeklyChallenge returns non-empty string`() {
        assertTrue(Challenges.getWeeklyChallenge("en").isNotEmpty())
        assertTrue(Challenges.getWeeklyChallenge("ru").isNotEmpty())
    }

    @Test
    fun `monthlyChallenge returns non-empty string`() {
        assertTrue(Challenges.getMonthlyChallenge("en").isNotEmpty())
        assertTrue(Challenges.getMonthlyChallenge("ru").isNotEmpty())
    }
}
