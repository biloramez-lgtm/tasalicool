package com.example.tasalicool.models

import java.io.Serializable
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

enum class PlayerType {
    HUMAN,
    AI
}

data class Player(

    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: PlayerType = PlayerType.HUMAN,

    val hand: MutableList<Card> = mutableListOf(),

    var score: Int = 0,
    var bid: Int = 0,
    var tricksWon: Int = 0,
    var teamId: Int = 0,
    var rating: Int = 1200

) : Serializable {

    fun isAI(): Boolean = type == PlayerType.AI
    fun isHuman(): Boolean = type == PlayerType.HUMAN

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

    fun hasCard(card: Card): Boolean = hand.contains(card)

    fun resetForNewRound() {
        bid = 0
        tricksWon = 0
        clearHand()
    }

    fun isOutOfCards(): Boolean = hand.isEmpty()

    fun isWinning(): Boolean = tricksWon >= bid

    fun incrementTrick() {
        tricksWon++
    }

    /* ================= ROUND SCORE ======================= */

    fun applyRoundScore(): Int {

        val basePoints = when (bid) {
            2 -> 2
            3 -> 3
            4 -> 4
            5 -> 10
            6 -> 12
            7 -> 14
            8 -> 16
            9 -> 27
            10 -> 40
            11 -> 11
            12 -> 12
            13 -> 41   // 🔥 عدلنا هون
            else -> 0
        }

        val points = if (tricksWon >= bid) {
            basePoints
        } else {
            -basePoints
        }

        score += points
        return points
    }

    /* ================= NETWORK SUPPORT =================== */

    fun updateFromNetwork(serverPlayer: Player) {
        score = serverPlayer.score
        bid = serverPlayer.bid
        tricksWon = serverPlayer.tricksWon
        teamId = serverPlayer.teamId
        rating = serverPlayer.rating

        hand.clear()
        hand.addAll(serverPlayer.hand)
        sortHand()
    }

    fun cloneState(): Player {
        return copy(
            hand = hand.toMutableList()
        )
    }

    /* ================= ELO RATING ======================== */

    fun updateRating(opponentRating: Int, won: Boolean) {

        val kFactor = 32

        val expected =
            1.0 / (1 + 10.0.pow(
                (opponentRating - rating) / 400.0
            ))

        val scoreValue = if (won) 1.0 else 0.0

        rating =
            (rating + kFactor * (scoreValue - expected)).toInt()

        rating = max(800, min(3000, rating))
    }

    fun displayScore(): String = "$score pts"

    fun shortInfo(): String =
        "$name | Score: $score | Tricks: $tricksWon"

    override fun toString(): String {
        return "Player(id=$id, name=$name, type=$type, score=$score)"
    }
}
