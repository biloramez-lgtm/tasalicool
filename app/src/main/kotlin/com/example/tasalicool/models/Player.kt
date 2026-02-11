package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

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

    fun addCards(cards: List<Card>) {
        hand.addAll(cards)
        hand.sortByDescending { it.strength() }
    }

    fun removeCard(card: Card) = hand.remove(card)

    fun resetForNewRound() {
        bid = 0
        tricksWon = 0
        hand.clear()
    }

    fun incrementTrick() {
        tricksWon++
    }

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

    fun aggressionFactor(): Double =
        when (difficulty) {
            AIDifficulty.EASY -> 0.8
            AIDifficulty.NORMAL -> 1.0
            AIDifficulty.HARD -> 1.2
            AIDifficulty.ELITE -> 1.4
        }

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
