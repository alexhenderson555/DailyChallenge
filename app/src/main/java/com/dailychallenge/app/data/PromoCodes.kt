package com.dailychallenge.app.data

/**
 * Список действующих промокодов на Premium.
 * Ты выдаёшь пользователям код — они вводят его в приложении.
 * Позже можно заменить на проверку через backend (тогда код привяжется к аккаунту и будет работать на всех устройствах).
 */
object PromoCodes {
    /** Действующие коды (без учёта регистра). Добавляй сюда новые при выдаче. */
    val validCodes: Set<String> = setOf(
        "PREMIUM",
        "BETA",
        "PROMO2024",
        "PROMO2025",
    ).map { it.uppercase().trim() }.toSet()

    fun isValid(code: String): Boolean =
        code.uppercase().trim() in validCodes
}
