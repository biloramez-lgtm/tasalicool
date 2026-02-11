package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

/* =====================================================
   أنواع الأوراق
   ===================================================== */

enum class Suit {
    HEARTS, DIAMONDS, CLUBS, SPADES
}

/* =====================================================
   رتب الأوراق
   ===================================================== */

enum class Rank(val displayName: String, val value: Int) {
    ACE("A", 14),
    KING("K", 13),
    QUEEN("Q", 12),
    JACK("J", 11),
    TEN("10", 10),
    NINE("9", 9),
    EIGHT("8", 8),
    SEVEN("7", 7),
    SIX("6", 6),
    FIVE("5", 5),
    FOUR("4", 4),
    THREE("3", 3),
    TWO("2", 2)
}

/* =====================================================
   Card
   ===================================================== */

data class Card(
    val suit: Suit,
    val rank: Rank
) : Serializable {

    fun isTrump(): Boolean = suit == Suit.HEARTS

    override fun toString(): String =
        "${rank.displayName}${suit.name.first()}"

    fun getResourceName(): String =
        "${rank.displayName.lowercase()}_of_${suit.name.lowercase()}"

    fun strength(): Int =
        if (isTrump()) rank.value + 20 else rank.value
}

/* =====================================================
   Deck
   ===================================================== */

data class Deck(
    val cards: MutableList<Card> = mutableListOf()
) : Serializable {

    init {
        if (cards.isEmpty()) reset()
    }

    fun reset() {
        cards.clear()
        Suit.values().forEach { suit ->
            Rank.values().forEach { rank ->
                cards.add(Card(suit, rank))
            }
        }
        shuffle()
    }

    fun shuffle() = cards.shuffle()

    fun drawCard(): Card? =
        if (cards.isNotEmpty()) cards.removeAt(0) else null

    fun drawCards(count: Int): List<Card> {
        val drawn = mutableListOf<Card>()
        repeat(count) {
            drawCard()?.let { drawn.add(it) }
        }
        return drawn
    }

    fun size(): Int = cards.size
    fun isEmpty(): Boolean = cards.isEmpty()
}

/* =====================================================
   AI Difficulty
   ===================================================== */

enum class AIDifficulty {
    EASY,
    NORMAL,
    HARD,
    ELITE
}

/* =====================================================
   Player (Elite Adaptive Version)
   ===================================================== */

data class Player(
    val id: String,
    val name: String,
    val hand: MutableList<Card> = mutableListOf(),

    var score: Int = 0,
    var bid: Int = 0,
    var tricksWon: Int = 0,
    var teamId: Int = 0,
    val isLocal: Boolean = false,

    // 🔥 جديد
    var difficulty: AIDifficulty = AIDifficulty.NORMAL,
    var rating: Int = 1200

) : Serializable {

    /* ================= Hand Management ================= */

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
        sortHand()
    }

    fun removeCard(card: Card): Boolean =
        hand.remove(card)

    fun clearHand() = hand.clear()

    fun handSize(): Int = hand.size

    private fun sortHand() {
        hand.sortWith(
            compareBy<Card> { it.suit.ordinal }
                .thenByDescending { it.strength() }
        )
    }

    /* ================= Round Reset ================= */

    fun resetForNewRound() {
        bid = 0
        tricksWon = 0
        hand.clear()
    }

    fun incrementTrick() {
        tricksWon++
    }

    /* ================= Scoring ================= */

    fun applyRoundScore(): Int {

        val points = when {
            bid == 13 ->
                if (tricksWon == 13) 400 else -52

            tricksWon >= bid ->
                if (bid >= 7) bid * 2 else bid

            else ->
                if (bid >= 7) -(bid * 2) else -bid
        }

        score += points
        return points
    }

    fun isPositiveScore(): Boolean = score > 0

    /* ================= AI Aggression ================= */

    fun aggressionFactor(): Double {
        return when (difficulty) {
            AIDifficulty.EASY -> 0.8
            AIDifficulty.NORMAL -> 1.0
            AIDifficulty.HARD -> 1.2
            AIDifficulty.ELITE -> 1.4
        }
    }

    /* ================= ELO SYSTEM ================= */

    fun updateRating(opponentRating: Int, won: Boolean) {

        val kFactor = when (difficulty) {
            AIDifficulty.EASY -> 16
            AIDifficulty.NORMAL -> 24
            AIDifficulty.HARD -> 32
            AIDifficulty.ELITE -> 40
        }

        val expected =
            1.0 / (1 + Math.pow(10.0,
                (opponentRating - rating) / 400.0))

        val scoreValue = if (won) 1.0 else 0.0

        rating =
            (rating + kFactor *
                    (scoreValue - expected)).toInt()

        rating = max(800, min(3000, rating))
    }
}

/* =====================================================
   Round History
   ===================================================== */

data class RoundResult(
    val roundNumber: Int,
    val teamScores: Map<Int, Int>
) : Serializable

/* =====================================================
   GameState (Ultimate Stable)
   ===================================================== */

data class GameState(

    val players: List<Player>,
    var currentPlayerIndex: Int = 0,
    val deck: Deck = Deck(),
    val currentTrick: MutableList<Pair<Player, Card>> = mutableListOf(),

    var roundNumber: Int = 1,
    var gameInProgress: Boolean = true,
    var winner: Player? = null,

    val matchHistory: MutableList<RoundResult> = mutableListOf()

) : Serializable {

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    fun totalTricksPlayed(): Int =
        players.sumOf { it.tricksWon }

    fun isStateValid(): Boolean {

        if (players.size != 4) return false
        if (currentPlayerIndex !in players.indices) return false

        val allCards =
            players.flatMap { it.hand } +
                    deck.cards +
                    currentTrick.map { it.second }

        if (allCards.distinct().size != allCards.size)
            return false

        return true
    }

    fun recordRoundResult() {

        val teamScores =
            players.groupBy { it.teamId }
                .mapValues { entry ->
                    entry.value.sumOf { it.score }
                }

        matchHistory.add(
            RoundResult(roundNumber, teamScores)
        )

        roundNumber++
    }
}

/* =====================================================
   Game Type
   ===================================================== */

enum class GameType {
    GAME_400
}
