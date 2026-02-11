package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

data class Player(
    val id: String,
    val name: String,

    val hand: MutableList<Card> = mutableListOf(),

    var score: Int = 0,
    var bid: Int = 0,
    var tricksWon: Int = 0,
    var teamId: Int = 0,

    val isLocal: Boolean = false,

    var difficulty: AIDifficulty = AIDifficulty.NORMAL,
    var rating: Int = 1200

) : Serializable {

    /* ===================================================== */
    /* ================= CARD MANAGEMENT =================== */
    /* ===================================================== */

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
        sortHand()
    }

    fun removeCard(card: Card): Boolean {
        val removed = hand.remove(card)
        if (removed) sortHand()
        return removed
    }

    fun clearHand() {
        hand.clear()
    }

    fun sortHand() {
        hand.sortWith(
            compareByDescending<Card> { it.isTrump() }
                .thenByDescending { it.strength() }
        )
    }

    fun hasCard(card: Card): Boolean {
        return hand.contains(card)
    }

    fun resetForNewRound() {
        bid = 0
        tricksWon = 0
        clearHand()
    }

    /* ===================================================== */
    /* ================= GAME INFO HELPERS ================= */
    /* ===================================================== */

    fun isOutOfCards(): Boolean = hand.isEmpty()

    fun isWinning(): Boolean = tricksWon >= bid

    /* ===================================================== */
    /* ================= TRICKS ============================ */
    /* ===================================================== */

    fun incrementTrick() {
        tricksWon++
    }

    /* ===================================================== */
    /* ================= ROUND SCORE ======================= */
    /* ===================================================== */

    fun applyRoundScore(): Int {

        val points = when {

            // كبوت 13
            bid == 13 ->
                if (tricksWon == 13) 400 else -52

            // نجح
            tricksWon >= bid ->
                if (bid >= 7) bid * 2 else bid

            // فشل
            else ->
                if (bid >= 7) -(bid * 2) else -bid
        }

        score += points
        return points
    }

    /* ===================================================== */
    /* ================= AI BEHAVIOR ======================= */
    /* ===================================================== */

    fun aggressionFactor(): Double =
        when (difficulty) {
            AIDifficulty.EASY -> 0.8
            AIDifficulty.NORMAL -> 1.0
            AIDifficulty.HARD -> 1.2
            AIDifficulty.ELITE -> 1.4
        }

    /* ===================================================== */
    /* ================= ELO RATING ======================== */
    /* ===================================================== */

    fun updateRating(opponentRating: Int, won: Boolean) {

        val kFactor = when (difficulty) {
            AIDifficulty.EASY -> 16
            AIDifficulty.NORMAL -> 24
            AIDifficulty.HARD -> 32
            AIDifficulty.ELITE -> 40
        }

        val expected =
            1.0 / (1 + 10.0.pow(
                (opponentRating - rating) / 400.0
            ))

        val scoreValue = if (won) 1.0 else 0.0

        rating =
            (rating + kFactor *
                    (scoreValue - expected)).toInt()

        rating = max(800, min(3000, rating))
    }

    /* ===================================================== */
    /* ================= NETWORK COPIES ==================== */
    /* ===================================================== */

    /**
     * نسخة كاملة للشبكة (Host يرسلها)
     */
    fun toNetworkFullCopy(): Player {
        return copy(
            hand = hand.toMutableList()
        )
    }

    /**
     * نسخة آمنة مستقبلية (حالياً نفس الكاملة)
     * جاهزة إذا أردت إخفاء أوراق الآخرين لاحقاً
     */
    fun toNetworkSafeCopy(): Player {
        return copy(
            hand = hand.toMutableList()
        )
    }

    /**
     * تحديث كامل من السيرفر (CLIENT يستخدمها)
     */
    fun updateFromNetwork(networkPlayer: Player) {

        score = networkPlayer.score
        bid = networkPlayer.bid
        tricksWon = networkPlayer.tricksWon
        teamId = networkPlayer.teamId
        rating = networkPlayer.rating
        difficulty = networkPlayer.difficulty

        // تحديث اليد بالكامل لضمان التزامن
        hand.clear()
        hand.addAll(networkPlayer.hand)
        sortHand()
    }

    /* ===================================================== */
    /* ================= UTILITIES ========================= */
    /* ===================================================== */

    fun displayScore(): String {
        return "$score pts"
    }

    fun shortInfo(): String {
        return "$name | Score: $score | Tricks: $tricksWon"
    }

    override fun toString(): String {
        return "Player(id=$id, name=$name, score=$score)"
    }
}
