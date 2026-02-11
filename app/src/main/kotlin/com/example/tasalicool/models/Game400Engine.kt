package com.example.tasalicool.models

import java.io.Serializable
import kotlin.math.max

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
       بدء جولة جديدة
       ===================================================== */

    fun startNewRound() {

        deck.reset()

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(Game400Constants.CARDS_PER_PLAYER))
        }

        calculateAIBids()

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
    }

    /* =====================================================
       اللاعب الحالي
       ===================================================== */

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /* =====================================================
       🤖 حساب Bid للذكاء الصناعي
       ===================================================== */

    private fun calculateAIBids() {
        players.forEach { player ->
            if (!player.isLocal) {
                player.bid = calculateSmartBid(player)
            }
        }
    }

    private fun calculateSmartBid(player: Player): Int {

        var strength = 0

        player.hand.forEach { card ->
            if (card.suit == Game400Constants.TRUMP_SUIT)
                strength += 2

            if (card.rank.value >= 11)
                strength += 1
        }

        return max(1, strength / 3)
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
       🤖 AI يلعب تلقائياً
       ===================================================== */

    fun playAITurnIfNeeded() {

        val current = getCurrentPlayer()

        if (!current.isLocal && roundActive) {

            val card = chooseBestCard(current)
            playCard(current, card)

            // إذا التالي AI كمان
            playAITurnIfNeeded()
        }
    }

    private fun chooseBestCard(player: Player): Card {

        val leadSuit = currentTrick.firstOrNull()?.second?.suit

        val validCards = player.hand.filter { isValidPlay(player, it) }

        if (leadSuit == null) {
            // يبدأ الأكلة → يرمي أقوى ورقة
            return validCards.maxBy { it.rank.value }
        }

        val sameSuit = validCards.filter { it.suit == leadSuit }

        if (sameSuit.isNotEmpty()) {
            return sameSuit.maxBy { it.rank.value }
        }

        val trumps = validCards.filter { it.suit == Game400Constants.TRUMP_SUIT }

        if (trumps.isNotEmpty()) {
            return trumps.maxBy { it.rank.value }
        }

        return validCards.minBy { it.rank.value }
    }

    /* =====================================================
       إنهاء الأكلة
       ===================================================== */

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

    /* =====================================================
       نهاية الجولة
       ===================================================== */

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
