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
    var tricksWon: Int = 0,
    var teamId: Int = 0,
    var rating: Int = 1200

) : Serializable {

    /* ================= BID ================= */

    var bid: Int = 0
        private set

    fun setBid(value: Int) {
        bid = value.coerceIn(0, 13)
    }

    fun clearBid() {
        bid = 0
    }

    fun hasPlacedBid(): Boolean = bid > 0

    fun hasWonBid(): Boolean =
        hasPlacedBid() && tricksWon >= bid

    fun bidDifference(): Int = tricksWon - bid

    /* ================= BASIC INFO ================= */

    fun isAI(): Boolean = type == PlayerType.AI
    fun isHuman(): Boolean = type == PlayerType.HUMAN

    /* ================= HAND MANAGEMENT ================= */

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

    fun hasCard(card: Card): Boolean = hand.contains(card)

    fun isOutOfCards(): Boolean = hand.isEmpty()

    fun sortHand(trump: Suit = Suit.HEARTS) {
        hand.sortWith(
            compareByDescending<Card> { it.isTrump(trump) }
                .thenByDescending { it.strength(trump) }
        )
    }

    /* ================= ROUND ================= */

    fun incrementTrick() {
        tricksWon++
    }

    fun resetForNewRound() {
        clearBid()
        tricksWon = 0
        clearHand()
    }

    /* ================= NETWORK SUPPORT ================= */

    fun updateFromNetwork(serverPlayer: Player) {
        score = serverPlayer.score
        setBid(serverPlayer.bid)
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

    /* ================= ELO RATING ================= */

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

    /* ================= UI HELPERS ================= */

    fun displayScore(): String = "$score pts"

    fun displayBid(): String =
        if (hasPlacedBid()) bid.toString() else ""

    fun bidStatus(): BidStatus =
        when {
            !hasPlacedBid() -> BidStatus.NONE
            hasWonBid() -> BidStatus.SUCCESS
            else -> BidStatus.FAILED
        }

    fun shortInfo(): String =
        "$name | Score: $score | Tricks: $tricksWon"

    override fun toString(): String {
        return "Player(id=$id, name=$name, score=$score, bid=$bid)"
    }
}

/* ================= BID STATUS ENUM ================= */

enum class BidStatus {
    NONE,
    SUCCESS,
    FAILED
}
