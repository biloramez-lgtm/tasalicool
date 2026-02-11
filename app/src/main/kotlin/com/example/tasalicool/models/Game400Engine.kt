package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max
import kotlin.math.min

object Game400Constants {
    const val CARDS_PER_PLAYER = 13
    const val WIN_SCORE = 41
    val TRUMP_SUIT = Suit.HEARTS
}

class Game400Engine(
    val players: List<Player>
) : Serializable {

    val deck = Deck()

    var currentPlayerIndex = 0
    var trickNumber = 0

    val currentTrick: MutableList<Pair<Player, Card>> = mutableListOf()

    var roundActive = false
    var gameWinner: Player? = null

    /* =====================================================
       🧠 ذاكرة الأوراق الملعوبة
       ===================================================== */

    private val playedCards: MutableList<Card> = mutableListOf()

    private val remainingCardsBySuit: MutableMap<Suit, MutableList<Card>> =
        mutableMapOf()

    private fun initializeMemory() {
        playedCards.clear()
        remainingCardsBySuit.clear()

        Suit.values().forEach { suit ->
            remainingCardsBySuit[suit] =
                Rank.values().map { Card(suit, it) }.toMutableList()
        }
    }

    private fun registerPlayedCard(card: Card) {
        playedCards.add(card)
        remainingCardsBySuit[card.suit]?.removeIf {
            it.rank == card.rank
        }
    }

    fun getRemainingCardsOfSuit(suit: Suit): List<Card> {
        return remainingCardsBySuit[suit] ?: emptyList()
    }

    /* =====================================================
       بدء جولة جديدة
       ===================================================== */

    fun startNewRound() {

        deck.reset()
        initializeMemory()

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(Game400Constants.CARDS_PER_PLAYER))
        }

        calculateAdvancedAIBids()

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
    }

    /* ===================================================== */

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /* =====================================================
       🤖 Bid احترافي
       ===================================================== */

    private fun calculateAdvancedAIBids() {
        players.forEach { player ->
            if (!player.isLocal) {
                player.bid = calculateProfessionalBid(player)
            }
        }
    }

    private fun calculateProfessionalBid(player: Player): Int {

        var score = 0.0

        val trumpCount =
            player.hand.count { it.suit == Game400Constants.TRUMP_SUIT }

        val highCards =
            player.hand.count { it.rank.value >= 11 }

        score += trumpCount * 2.8
        score += highCards * 1.4

        player.hand.forEach {
            if (it.suit == Game400Constants.TRUMP_SUIT &&
                it.rank == Rank.ACE
            ) score += 2
        }

        var bid = (score / 3).toInt()
        bid = max(2, bid)
        bid = min(13, bid)

        return bid
    }

    /* =====================================================
       اللعب داخل الأكلة
       ===================================================== */

    fun playCard(player: Player, card: Card): Boolean {

        if (!roundActive) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)
        currentTrick.add(player to card)

        // 🔥 تسجيل في الذاكرة
        registerPlayedCard(card)

        if (currentTrick.size == 4) {
            finishTrick()
        } else {
            nextPlayer()
        }

        return true
    }

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasLeadSuit = player.hand.any { it.suit == leadSuit }

        return if (hasLeadSuit) card.suit == leadSuit else true
    }

    /* =====================================================
       🤖 AI بذاكرة فعلية
       ===================================================== */

    fun playAITurnIfNeeded() {

        val current = getCurrentPlayer()

        if (!current.isLocal && roundActive) {

            val card = chooseProfessionalCard(current)
            playCard(current, card)

            playAITurnIfNeeded()
        }
    }

    private fun chooseProfessionalCard(player: Player): Card {

        val validCards = player.hand.filter { isValidPlay(player, it) }
        val leadSuit = currentTrick.firstOrNull()?.second?.suit

        // إذا يبدأ الأكلة
        if (leadSuit == null) {

            // إذا بقي طرنيب قوي عند الخصوم → لا تحرق الآص
            val remainingTrumps =
                getRemainingCardsOfSuit(Game400Constants.TRUMP_SUIT)

            val opponentStrongTrumpExists =
                remainingTrumps.any { it.rank.value >= 13 }

            if (!opponentStrongTrumpExists) {
                return validCards.maxBy { it.rank.value }
            }

            return validCards.minBy { it.rank.value }
        }

        val sameSuit = validCards.filter { it.suit == leadSuit }

        if (sameSuit.isNotEmpty()) {

            val highestOnTable = currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
                .second

            val winningCard = sameSuit
                .filter { it.rank.value > highestOnTable.rank.value }
                .minByOrNull { it.rank.value }

            return winningCard ?: sameSuit.minBy { it.rank.value }
        }

        val trumps = validCards
            .filter { it.suit == Game400Constants.TRUMP_SUIT }

        if (trumps.isNotEmpty()) {

            val remainingTrumps =
                getRemainingCardsOfSuit(Game400Constants.TRUMP_SUIT)

            val strongestRemaining =
                remainingTrumps.maxByOrNull { it.rank.value }

            val safeTrump = trumps
                .filter {
                    strongestRemaining == null ||
                            it.rank.value >= strongestRemaining.rank.value
                }
                .minByOrNull { it.rank.value }

            return safeTrump ?: trumps.minBy { it.rank.value }
        }

        return validCards.minBy { it.rank.value }
    }

    /* ===================================================== */

    private fun finishTrick() {

        val winnerPair = determineTrickWinner()
        winnerPair.first.incrementTrick()

        currentPlayerIndex = players.indexOf(winnerPair.first)

        currentTrick.clear()
        trickNumber++

        if (trickNumber == 13) {
            finishRound()
        }
    }

    private fun determineTrickWinner(): Pair<Player, Card> {

        val leadSuit = currentTrick.first().second.suit

        val trumpCards = currentTrick.filter {
            it.second.suit == Game400Constants.TRUMP_SUIT
        }

        return if (trumpCards.isNotEmpty()) {
            trumpCards.maxBy { it.second.rank.value }
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
        }
    }

    /* ===================================================== */

    private fun finishRound() {

        players.forEach { it.applyRoundScore() }

        checkGameWinner()
        roundActive = false
    }

    private fun checkGameWinner() {

        players.forEach { player ->
            if (player.score >= Game400Constants.WIN_SCORE) {

                val partner = players.first {
                    it.teamId == player.teamId && it != player
                }

                if (partner.score > 0) {
                    gameWinner = player
                }
            }
        }
    }

    fun isGameOver(): Boolean = gameWinner != null
}
