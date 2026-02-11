package com.example.tasalicool.models

import android.content.Context
import android.os.Handler
import android.os.Looper
import java.io.Serializable

object Game400Constants {
    const val CARDS_PER_PLAYER = 13
    const val WIN_SCORE = 41
    val TRUMP_SUIT = Suit.HEARTS
}

class Game400Engine(
    context: Context,
    val players: List<Player>
) : Serializable {

    @Transient
    private val appContext: Context = context.applicationContext

    @Transient
    private val handler = Handler(Looper.getMainLooper())

    val deck = Deck()

    var currentPlayerIndex = 0
    var trickNumber = 0
    val currentTrick = mutableListOf<Pair<Player, Card>>()

    var roundActive = false
    var gameWinner: Player? = null

    init {
        players.forEach { it.tricksWon = 0 }
    }

    /* ================= START ROUND ================= */

    fun startNewRound() {

        deck.reset()

        players.forEach {
            it.resetForNewRound()
            it.addCards(drawCards(Game400Constants.CARDS_PER_PLAYER))
        }

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()
    }

    /* ================= DRAW CARDS SAFE ================= */

    private fun drawCards(count: Int): List<Card> {
        val list = mutableListOf<Card>()
        repeat(count) {
            deck.drawCard()?.let { list.add(it) }
        }
        return list
    }

    /* ================= AI TURN ================= */

    fun playAITurnIfNeeded() {

        if (!roundActive) return

        val current = getCurrentPlayer()

        if (!current.isLocal) {

            val card = current.hand.randomOrNull() ?: return
            playCard(current, card)

            if (roundActive && !getCurrentPlayer().isLocal) {
                handler.postDelayed({
                    playAITurnIfNeeded()
                }, 400)
            }
        }
    }

    /* ================= GAME FLOW ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (!roundActive) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)
        currentTrick.add(player to card)

        if (currentTrick.size == 4)
            finishTrick()
        else
            nextPlayer()

        return true
    }

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    /* ================= TRICK ================= */

    private fun finishTrick() {

        val winner = determineTrickWinner() ?: return
        winner.first.incrementTrick()

        currentPlayerIndex =
            players.indexOf(winner.first)

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

        checkGameWinner()
        roundActive = false
    }

    private fun checkGameWinner() {
        players.forEach {
            if (it.score >= Game400Constants.WIN_SCORE)
                gameWinner = it
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

    fun isGameOver() =
        gameWinner != null
}
