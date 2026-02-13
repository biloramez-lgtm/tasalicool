package com.example.tasalicool.game

import com.example.tasalicool.models.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

object AdvancedAI {

    /* ================= MEMORY ================= */

    private val playedCards = mutableSetOf<Card>()
    private val playerCardHistory = mutableMapOf<String, MutableList<Card>>()

    fun rememberCard(player: Player, card: Card) {
        playedCards.add(card)
        val history = playerCardHistory.getOrPut(player.id) { mutableListOf() }
        history.add(card)
    }

    fun resetMemory() {
        playedCards.clear()
        playerCardHistory.clear()
    }

    /* ================= HAND EVALUATION ================= */

    fun evaluateHandStrength(player: Player): Double {

        var score = 0.0
        val trumpSuit = Suit.HEARTS

        val trumpCards = player.hand.filter { it.isTrump(trumpSuit) }
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
            if (!it.isTrump(trumpSuit)) score += 2.0
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

    /* ================= NEW SMART BID ================= */

    fun chooseBid(
        player: Player,
        engine: Game400Engine,
        minBid: Int
    ): Int {

        val baseBid = calculateBid(player)

        // ضغط الجولة
        val needed = player.bid - player.tricksWon
        val roundPressure =
            if (engine.trickNumber < 3) 0
            else if (engine.trickNumber < 8) 1
            else 2

        var finalBid = baseBid + roundPressure

        // لا ينزل تحت الحد الأدنى
        finalBid = max(minBid, finalBid)

        // لا يتجاوز 13
        finalBid = min(13, finalBid)

        return finalBid
    }

    /* ================= DECISION ENGINE ================= */

    fun chooseCard(
        player: Player,
        engine: Game400Engine
    ): Card {

        val trick = engine.currentTrick
        val trumpSuit = Suit.HEARTS

        val validCards = getValidCards(player, trick)

        var bestCard = validCards.first()
        var bestScore = Double.NEGATIVE_INFINITY

        for (card in validCards) {

            val monteCarlo = simulateFuture(player, card, trumpSuit)
            val tactical = tacticalEvaluation(player, card, trumpSuit)
            val pressure = pressureFactor(player, engine)
            val partner = partnerFactor(player, trick, trumpSuit)
            val stage = stageFactor(engine)
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

    /* ================= MONTE CARLO ================= */

    private fun simulateFuture(
        player: Player,
        card: Card,
        trumpSuit: Suit
    ): Double {

        var wins = 0
        val simulations = 20

        repeat(simulations) {
            val probability = calculateWinProbability(player, card, trumpSuit)
            val randomFactor = Random.nextDouble(0.8, 1.2)

            if (probability * randomFactor > 0.6)
                wins++
        }

        return wins.toDouble() / simulations
    }

    private fun calculateWinProbability(
        player: Player,
        card: Card,
        trumpSuit: Suit
    ): Double {

        val remaining = buildRemainingDeck(player, card)

        val higherSameSuit =
            remaining.count {
                it.suit == card.suit &&
                        it.rank.value > card.rank.value
            }

        val trumpThreat =
            remaining.count {
                it.isTrump(trumpSuit) && !card.isTrump(trumpSuit)
            }

        val total = remaining.size.toDouble()
        if (total == 0.0) return 1.0

        val risk = (higherSameSuit + trumpThreat) / total
        return 1.0 - risk
    }

    private fun buildRemainingDeck(player: Player, card: Card): List<Card> {

        val all =
            Suit.values().flatMap { suit ->
                Rank.values().map { rank ->
                    Card(suit, rank)
                }
            }

        return all
            .filterNot { playedCards.contains(it) }
            .filterNot { player.hand.contains(it) }
            .filterNot { it == card }
    }

    /* ================= TACTICAL ================= */

    private fun tacticalEvaluation(
        player: Player,
        card: Card,
        trumpSuit: Suit
    ): Double {

        var score = card.rank.value / 14.0

        if (card.isTrump(trumpSuit))
            score += 1.0

        val needed = player.bid - player.tricksWon

        if (needed > 0)
            score += 0.7
        else
            score -= 0.5

        return score
    }

    private fun stageFactor(engine: Game400Engine): Double {
        return when {
            engine.trickNumber < 4 -> 0.3
            engine.trickNumber < 9 -> 0.7
            else -> 1.1
        }
    }

    private fun partnerFactor(
        player: Player,
        trick: List<Pair<Player, Card>>,
        trumpSuit: Suit
    ): Double {

        if (trick.isEmpty()) return 0.0

        val currentWinner = determineCurrentWinner(trick, trumpSuit)

        return if (currentWinner?.teamId == player.teamId)
            -0.6
        else 0.5
    }

    private fun pressureFactor(
        player: Player,
        engine: Game400Engine
    ): Double {

        val needed = player.bid - player.tricksWon
        val remaining =
            13 - engine.players.sumOf { it.tricksWon }

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
        trick: List<Pair<Player, Card>>,
        trumpSuit: Suit
    ): Player? {

        if (trick.isEmpty()) return null

        val leadSuit = trick.first().second.suit

        val trumpCards =
            trick.filter { it.second.isTrump(trumpSuit) }

        return if (trumpCards.isNotEmpty())
            trumpCards.maxBy { it.second.rank.value }.first
        else
            trick.filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
                .first
    }

    private fun getValidCards(
        player: Player,
        trick: List<Pair<Player, Card>>
    ): List<Card> {

        if (trick.isEmpty())
            return player.hand

        val leadSuit = trick.first().second.suit
        val hasSuit = player.hand.any { it.suit == leadSuit }

        return if (hasSuit)
            player.hand.filter { it.suit == leadSuit }
        else player.hand
    }
}
