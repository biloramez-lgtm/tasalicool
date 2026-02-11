package com.example.tasalicool.models

import java.io.Serializable

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

    // أوراق الأكلة الحالية
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

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
    }

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun nextPlayer() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    /* =====================================================
       المزايدة
       ===================================================== */

    fun setPlayerBid(player: Player, bid: Int) {
        player.bid = bid
    }

    fun allPlayersBid(): Boolean {
        return players.all { it.bid > 0 }
    }

    fun totalBids(): Int {
        return players.sumOf { it.bid }
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

        return if (hasLeadSuit) {
            card.suit == leadSuit
        } else {
            true
        }
    }

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

        val trumpCards = currentTrick.filter { it.second.suit == Game400Constants.TRUMP_SUIT }

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
