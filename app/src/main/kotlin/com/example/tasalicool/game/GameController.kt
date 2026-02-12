package com.example.tasalicool.game

import com.example.tasalicool.models.*

class GameController(
    val players: MutableList<Player>
) {

    var roundNumber: Int = 1
    var currentPlayerIndex: Int = 0

    val currentTrick: MutableList<Pair<Player, Card>> = mutableListOf()

    /* =========================================================
       🎮 Start New Round
       ========================================================= */

    fun startNewRound() {

        roundNumber = 1
        currentPlayerIndex = 0
        currentTrick.clear()

        // Reset AI Memory
        AdvancedAI.resetMemory()

        players.forEach {
            it.tricksWon = 0
            it.bid = 0
            it.hand.clear()
        }

        dealCards()
    }

    /* =========================================================
       🃏 Deal Cards
       ========================================================= */

    private fun dealCards() {

        val deck = Suit.values().flatMap { suit ->
            Rank.values().map { rank ->
                Card(suit, rank)
            }
        }.shuffled()

        var index = 0

        repeat(13) {
            for (player in players) {
                player.hand.add(deck[index])
                index++
            }
        }
    }

    /* =========================================================
       🎯 Play Card
       ========================================================= */

    fun playCard(player: Player, card: Card) {

        if (!player.hand.contains(card)) return

        player.hand.remove(card)
        currentTrick.add(player to card)

        // Inform AI memory
        AdvancedAI.rememberCard(player, card)

        if (currentTrick.size == players.size) {
            finishTrick()
        } else {
            moveToNextPlayer()
        }
    }

    /* =========================================================
       🤖 AI Turn
       ========================================================= */

    fun playAITurn() {

        val player = players[currentPlayerIndex]

        if (!player.isHuman) {
            val chosenCard =
                AdvancedAI.chooseCard(player, this)

            playCard(player, chosenCard)
        }
    }

    /* =========================================================
       🏆 Finish Trick
       ========================================================= */

    private fun finishTrick() {

        val winner = determineTrickWinner()

        winner?.let {
            it.tricksWon += 1
            currentPlayerIndex = players.indexOf(it)
        }

        currentTrick.clear()
        roundNumber++
    }

    private fun determineTrickWinner(): Player? {

        if (currentTrick.isEmpty()) return null

        val leadSuit = currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter { it.second.isTrump() }

        return if (trumpCards.isNotEmpty()) {
            trumpCards.maxBy { it.second.rank.value }.first
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
                .first
        }
    }

    /* =========================================================
       🔄 Turn Rotation
       ========================================================= */

    private fun moveToNextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }
}
