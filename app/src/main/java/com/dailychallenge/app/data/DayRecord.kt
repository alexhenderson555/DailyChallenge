package com.dailychallenge.app.data

/** Невыполненная цель с датой выдачи (для премиум-очереди до 3 шт.). */
data class PendingChallenge(
    val date: String,
    val categoryId: String,
    val challengeText: String
)

data class DayRecord(
    val date: String, // yyyy-MM-dd
    val challengeText: String,
    val categoryId: String,
    val completed: Boolean,
    val usedFreeze: Boolean = false,
    val note: String = ""
)
