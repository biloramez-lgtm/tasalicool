package com.example.tasalicool.models

import java.io.Serializable
import kotlin.random.Random

data class Deck(
    val cards: MutableList<Card> = mutableListOf()
) : Serializable {

    private var lastSeed: Long = 0L

    init {
        if (cards.isEmpty()) {
            reset()
        }
    }

    /* ================= RESET ================= */

    /**
     * السيرفر يمكنه تمرير seed محدد
     * لضمان نفس التوزيع على جميع الأجهزة
     */
    fun reset(seed: Long = System.currentTimeMillis()) {

        lastSeed = seed

        cards.clear()

        for (suit in Suit.values()) {
            for (rank in Rank.values()) {
                cards.add(Card(suit, rank))
            }
        }

        shuffle(seed)
    }

    /* ================= SHUFFLE ================= */

    private fun shuffle(seed: Long) {
        cards.shuffle(Random(seed))
    }

    /**
     * يسمح بمعرفة seed المستخدم
     * لإعادة بناء الجولة عند الحاجة
     */
    fun getLastSeed(): Long = lastSeed

    /* ================= DRAW ================= */

    // 🔥 نسخة صارمة - لا تسمح بإرجاع null
    fun drawCard(): Card {
        require(cards.isNotEmpty()) { "Deck is empty!" }
        return cards.removeAt(0)
    }

    fun drawCards(count: Int): List<Card> {

        val safeCount = count.coerceAtMost(cards.size)
        val drawnCards = mutableListOf<Card>()

        repeat(safeCount) {
            drawnCards.add(drawCard())
        }

        return drawnCards
    }

    /* ================= INFO ================= */

    fun remainingCards(): Int = cards.size

    fun isEmpty(): Boolean = cards.isEmpty()
}
