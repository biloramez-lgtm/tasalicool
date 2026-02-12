package com.example.tasalicool.models

import java.io.Serializable

/* ================= CONSTANTS ================= */

object Game400Constants {
    const val CARDS_PER_PLAYER = 13
    const val WIN_SCORE = 41
    val TRUMP_SUIT = Suit.HEARTS
}

/* ================= GAME ENGINE ================= */

class Game400Engine(
    val players: List<Player>
) : Serializable {

    private val deck = Deck()

    /* ================= STATE ================= */

    var currentPlayerIndex = 0
        private set

    var trickNumber = 0
        private set

    var currentRound = 1
        private set

    val currentTrick = mutableListOf<Pair<Player, Card>>()

    var roundActive = false
        private set

    var gameWinner: Player? = null
        private set

    init {
        players.forEach { it.tricksWon = 0 }
    }

    /* ================= START ROUND ================= */

    fun startNewRound() {

        if (isGameOver()) return

        deck.reset()

        players.forEach {
            it.resetForNewRound()
            it.addCards(
                deck.drawCards(Game400Constants.CARDS_PER_PLAYER)
            )
        }

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
        gameWinner = null
    }

    /* ================= GAME FLOW ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (!roundActive) return false
        if (isGameOver()) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)
        currentTrick.add(player to card)

        if (currentTrick.size == players.size)
            finishTrick()
        else
            nextPlayer()

        return true
    }

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    fun isCurrentPlayerAI(): Boolean =
        getCurrentPlayer().isAI()

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    /* ================= TRICK ================= */

    private fun finishTrick() {

        val winnerPair = determineTrickWinner() ?: return
        val winner = winnerPair.first

        winner.incrementTrick()

        currentPlayerIndex =
            players.indexOf(winner)

        currentTrick.clear()
        trickNumber++

        if (trickNumber >= Game400Constants.CARDS_PER_PLAYER)
            finishRound()
    }

    private fun determineTrickWinner(): Pair<Player, Card>? {

        if (currentTrick.isEmpty()) return null

        val leadSuit = currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter {
                it.second.suit == Game400Constants.TRUMP_SUIT
            }

        return if (trumpCards.isNotEmpty())
            trumpCards.maxByOrNull { it.second.strength() }
        else
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxByOrNull { it.second.strength() }
    }

    /* ================= ROUND END ================= */

    private fun finishRound() {

        players.forEach { it.applyRoundScore() }

        roundActive = false
        currentRound++

        checkGameWinner()
    }

    private fun checkGameWinner() {
        players.forEach {
            if (it.score >= Game400Constants.WIN_SCORE) {
                gameWinner = it
            }
        }
    }

    /* ================= VALID PLAY ================= */

    private fun isValidPlay(
        player: Player,
        card: Card
    ): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit =
            currentTrick.first().second.suit

        val hasSuit =
            player.hand.any { it.suit == leadSuit }

        return if (hasSuit)
            card.suit == leadSuit
        else true
    }

    /* ================= NETWORK SUPPORT ================= */

    fun forceSyncFromServer(serverEngine: Game400Engine) {

        currentPlayerIndex = serverEngine.currentPlayerIndex
        trickNumber = serverEngine.trickNumber
        currentRound = serverEngine.currentRound
        roundActive = serverEngine.roundActive
        gameWinner = serverEngine.gameWinner

        currentTrick.clear()
        currentTrick.addAll(serverEngine.currentTrick)

        serverEngine.players.forEach { serverPlayer ->

            val localPlayer =
                players.find { it.id == serverPlayer.id }

            localPlayer?.updateFromNetwork(serverPlayer)
        }
    }

    /* ================= STATUS ================= */

    fun isGameOver(): Boolean =
        gameWinner != null
}
