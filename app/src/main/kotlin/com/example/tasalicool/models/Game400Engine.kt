package com.example.tasalicool.models

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import java.io.Serializable
import kotlin.math.*

object Game400Constants {
    const val CARDS_PER_PLAYER = 13
    const val WIN_SCORE = 41
    val TRUMP_SUIT = Suit.HEARTS
}

class Game400Engine(
    context: Context,
    val players: List<Player>
) : Serializable {

    private val appContext = context.applicationContext
    val deck = Deck()

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(
            "FINAL_ELITE_AI",
            Context.MODE_PRIVATE
        )

    private val elo = EloRating()
    private val learning = PlayerLearningSystem()

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
        Game400AI.resetMemory()

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(Game400Constants.CARDS_PER_PLAYER))
        }

        runHybridBidding()

        trickNumber = 0
        currentPlayerIndex = 0
        roundActive = true
        currentTrick.clear()

        // ✅ FIXED
        GameSaveManager.saveGame(appContext, buildGameState())
    }

    private fun runHybridBidding() {
        players.forEach {
            if (!it.isLocal) {
                it.bid = Game400AI.calculateBid(it)
            }
        }
    }

    /* ================= AI TURN ================= */

    fun playAITurnIfNeeded() {

        if (!roundActive) return

        val current = getCurrentPlayer()

        if (!current.isLocal) {

            val state = buildGameState()
            val card = Game400AI.chooseCard(current, state)

            playCard(current, card)

            if (roundActive && !getCurrentPlayer().isLocal) {
                Handler(Looper.getMainLooper()).postDelayed({
                    playAITurnIfNeeded()
                }, 300)
            }
        }
    }

    private fun buildGameState(): GameState {

        return GameState(
            players = players,
            currentPlayerIndex = currentPlayerIndex,
            deck = deck,
            currentTrick = currentTrick.toMutableList(),
            roundNumber = trickNumber + 1,
            gameInProgress = roundActive,
            winner = gameWinner
        )
    }

    /* ================= GAME FLOW ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (!roundActive) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        if (player.isLocal) {
            learning.updateSkillLevel(1, 50)
        }

        player.removeCard(card)
        currentTrick.add(player to card)

        Game400AI.rememberCard(player, card)

        if (currentTrick.size == 4)
            finishTrick()
        else
            nextPlayer()

        // ✅ FIXED
        GameSaveManager.saveGame(appContext, buildGameState())

        return true
    }

    fun getCurrentPlayer() =
        players[currentPlayerIndex]

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    private fun finishTrick() {

        val winner = determineTrickWinner()

        winner.first.incrementTrick()

        currentPlayerIndex =
            players.indexOf(winner.first)

        currentTrick.clear()
        trickNumber++

        if (trickNumber == 13)
            finishRound()
    }

    private fun determineTrickWinner():
            Pair<Player, Card> {

        val leadSuit =
            currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter {
                it.second.suit ==
                        Game400Constants.TRUMP_SUIT
            }

        return if (trumpCards.isNotEmpty())
            trumpCards.maxBy { it.second.strength() }
        else
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.strength() }
    }

    private fun finishRound() {

        val winningTeam =
            players.groupBy { it.teamId }
                .maxBy {
                    it.value.sumOf { p -> p.tricksWon }
                }.key

        val aiWon =
            winningTeam != players.first().teamId

        players.forEach { it.applyRoundScore() }

        checkGameWinner()

        roundActive = false

        // ✅ FIXED
        GameSaveManager.saveGame(appContext, buildGameState())
    }

    private fun checkGameWinner() {
        players.forEach {
            if (it.score >= Game400Constants.WIN_SCORE)
                gameWinner = it
        }
    }

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
