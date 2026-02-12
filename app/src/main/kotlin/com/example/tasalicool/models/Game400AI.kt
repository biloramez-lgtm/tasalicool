package com.example.tasalicool.models

import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object Game400AI {

    /* =========================================================
       🧠 Memory System (Elite)
       ========================================================= */

    private val playedCards = mutableSetOf<Card>()
    private val playerCardHistory = mutableMapOf<String, MutableList<Card>>()

    fun rememberCard(player: Player, card: Card) {
        playedCards.add(card)

        val history = playerCardHistory.getOrPut(player.id) {
            mutableListOf()
        }
        history.add(card)
    }

    fun resetMemory() {
        playedCards.clear()
        playerCardHistory.clear()
    }

    /* =========================================================
       🧠 Hand Evaluation
       ========================================================= */

    fun evaluateHandStrength(player: Player): Double {

        var score = 0.0

        val trumpCards = player.hand.filter { it.isTrump() }
        val highCards = player.hand.filter { it.rank.value >= 11 }

        score += trumpCards.size * 4.5

        trumpCards.forEach {
            score += when (it.rank) {
                Rank.ACE -> 6.0
                Rank.KING -> 4.5
                Rank.QUEEN -> 3.5
                Rank.JACK -> 2.5
                else -> 1.2
            }
        }

        highCards.forEach {
            if (!it.isTrump()) score += 2.0
        }

        val suitCounts = player.hand.groupBy { it.suit }
        suitCounts.forEach { (_, cards) ->
            if (cards.size <= 2) score += 2.0
        }

        return score
    }

    fun calculateBid(player: Player): Int {

        val strength = evaluateHandStrength(player)

        var bid = (strength / 4).toInt()

        if (strength > 26) bid++
        if (strength > 32) bid++

        return min(max(2, bid), 13)
    }

    /* =========================================================
       🧠 Hybrid Elite Decision
       ========================================================= */

    fun chooseCard(
        player: Player,
        gameState: GameState
    ): Card {

        val trick: List<Pair<Player, Card>> = gameState.currentTrick
        val validCards = getValidCards(player, trick)

        var bestCard = validCards.first()
        var bestScore = Double.NEGATIVE_INFINITY

        for (card in validCards) {

            val monteCarlo = simulateFuture(player, card)
            val tactical = tacticalEvaluation(player, card)
            val pressure = pressureFactor(player, gameState)
            val partner = partnerFactor(player, trick)
            val stage = stageFactor(gameState)
            val memoryImpact = memoryFactor(card)

            val score =
                monteCarlo * 0.35 +
                tactical * 0.20 +
                pressure * 0.15 +
                partner * 0.10 +
                stage * 0.10 +
                memoryImpact * 0.10

            if (score > bestScore) {
                bestScore = score
                bestCard = card
            }
        }

        return bestCard
    }

    /* =========================================================
       🎲 Monte Carlo Light
       ========================================================= */

    private fun simulateFuture(
        player: Player,
        card: Card
    ): Double {

        var wins = 0
        val simulations = 20

        repeat(simulations) {

            val probability =
                calculateWinProbability(player, card)

            val randomFactor =
                Random.nextDouble(0.8, 1.2)

            if (probability * randomFactor > 0.6)
                wins++
        }

        return wins.toDouble() / simulations
    }

    private fun calculateWinProbability(
        player: Player,
        card: Card
    ): Double {

        val remaining = buildRemainingDeck(player, card)

        val higherSameSuit =
            remaining.count {
                it.suit == card.suit &&
                        it.rank.value > card.rank.value
            }

        val trumpThreat =
            remaining.count {
                it.isTrump() && !card.isTrump()
            }

        val total = remaining.size.toDouble()
        if (total == 0.0) return 1.0

        val risk =
            (higherSameSuit + trumpThreat) / total

        return 1.0 - risk
    }

    private fun buildRemainingDeck(
        player: Player,
        card: Card
    ): List<Card> {

        val all =
            Suit.values().flatMap { s ->
                Rank.values().map { r ->
                    Card(s, r)
                }
            }

        return all
            .filterNot { playedCards.contains(it) }
            .filterNot { player.hand.contains(it) }
            .filterNot { it == card }
    }

    /* =========================================================
       🎯 Tactical Layers
       ========================================================= */

    private fun tacticalEvaluation(
        player: Player,
        card: Card
    ): Double {

        var score = card.rank.value / 14.0

        if (card.isTrump())
            score += 1.0

        val needed =
            player.bid - player.tricksWon

        if (needed > 0)
            score += 0.7
        else
            score -= 0.5

        return score
    }

    private fun stageFactor(
        gameState: GameState
    ): Double {

        return when {
            gameState.roundNumber < 4 -> 0.3
            gameState.roundNumber < 9 -> 0.7
            else -> 1.1
        }
    }

    private fun partnerFactor(
        player: Player,
        trick: List<Pair<Player, Card>>
    ): Double {

        if (trick.isEmpty()) return 0.0

        val currentWinner =
            determineCurrentWinner(trick)

        return if (currentWinner?.teamId == player.teamId)
            -0.6
        else 0.5
    }

    private fun pressureFactor(
        player: Player,
        gameState: GameState
    ): Double {

        val needed = player.bid - player.tricksWon
        val remaining =
            13 - gameState.players.sumOf { it.tricksWon }

        return when {
            needed >= remaining -> 1.0
            needed > remaining / 2 -> 0.7
            else -> 0.3
        }
    }

    private fun memoryFactor(card: Card): Double {

        val sameSuitPlayed =
            playedCards.count { it.suit == card.suit }

        return sameSuitPlayed / 13.0
    }

    private fun determineCurrentWinner(
        trick: List<Pair<Player, Card>>
    ): Player? {

        if (trick.isEmpty()) return null

        val leadSuit = trick.first().second.suit

        val trumpCards =
            trick.filter { it.second.isTrump() }

        return if (trumpCards.isNotEmpty())
            trumpCards.maxBy { it.second.rank.value }.first
        else
            trick.filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }.first
    }

    private fun getValidCards(
        player: Player,
        trick: List<Pair<Player, Card>>
    ): List<Card> {

        if (trick.isEmpty())
            return player.hand

        val leadSuit =
            trick.first().second.suit

        val hasSuit =
            player.hand.any { it.suit == leadSuit }

        return if (hasSuit)
            player.hand.filter { it.suit == leadSuit }
        else player.hand
    }
}
