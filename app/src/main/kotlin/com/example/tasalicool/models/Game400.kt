package com.example.tasalicool.models

import java.io.Serializable

// قواعد لعبة 400
object Game400Rules {
    const val TARGET_SCORE = 400
    const val CARDS_PER_HAND = 7
    const val MAX_PLAYERS = 4
}

data class Game400Round(
    val players: List<Player>,
    val deck: Deck = Deck(),
    val discardPile: MutableList<Card> = mutableListOf(),
    var currentPlayerIndex: Int = 0,
    var roundInProgress: Boolean = true
) : Serializable {

    private var initialized = false

    fun initialize() {
        if (initialized) return
        initialized = true

        deck.shuffle()

        players.forEach { player ->
            player.clearHand()
            player.addCards(deck.drawCards(Game400Rules.CARDS_PER_HAND))
        }

        deck.drawCard()?.let { discardPile.add(it) }

        currentPlayerIndex = 0
        roundInProgress = true
    }

    fun getCurrentPlayer(): Player = players[currentPlayerIndex]

    fun nextTurn() {
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size
    }

    fun canPlay(card: Card): Boolean {
        val lastCard = discardPile.lastOrNull() ?: return true
        return card.rank == lastCard.rank || card.suit == lastCard.suit
    }

    fun playCard(card: Card): Boolean {

        if (!roundInProgress) return false

        val player = getCurrentPlayer()

        if (!canPlay(card)) return false

        val removed = player.removeCard(card)
        if (!removed) return false

        discardPile.add(card)

        // ✅ فحص انتهاء الجولة
        if (player.hand.isEmpty()) {
            endRound(winner = player)
        } else {
            nextTurn()
        }

        return true
    }

    fun drawFromDeck() {

        if (!roundInProgress) return

        if (deck.isEmpty()) {
            reshuffleDiscardIntoDeck()
        }

        val player = getCurrentPlayer()
        deck.drawCard()?.let { player.addCards(listOf(it)) }
    }

    private fun reshuffleDiscardIntoDeck() {

        if (discardPile.size <= 1) return

        val lastCard = discardPile.last()
        val cardsToReshuffle = discardPile.dropLast(1)

        discardPile.clear()
        discardPile.add(lastCard)

        deck.addCards(cardsToReshuffle)
        deck.shuffle()
    }

    private fun endRound(winner: Player) {

        roundInProgress = false

        // حساب نقاط الخاسرين
        players.forEach { player ->
            if (player != winner) {
                val penalty = calculateHandValue(player)
                player.score += penalty
            }
        }
    }

    private fun calculateHandValue(player: Player): Int {
        var score = 0

        player.hand.forEach { card ->
            score += when (card.rank) {
                Rank.ACE -> 1
                Rank.TEN, Rank.JACK, Rank.QUEEN, Rank.KING -> 10
                else -> card.rank.value
            }
        }

        return score
    }

    fun isRoundOver(): Boolean = !roundInProgress

    fun isGameOver(): Boolean {
        return players.any { it.score >= Game400Rules.TARGET_SCORE }
    }

    fun getWinner(): Player? {
        return players.find { it.score >= Game400Rules.TARGET_SCORE }
    }
}
