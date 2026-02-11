package com.example.tasalicool.models

import java.io.Serializable

data class GameState(

    val players: MutableList<Player> = mutableListOf(),

    var currentPlayerIndex: Int = 0,

    val deck: Deck = Deck(),

    val currentTrick: MutableList<Pair<String, Card>> = mutableListOf(),
    // نخزن playerId بدل Player object لتفادي مشاكل الشبكة

    var roundNumber: Int = 1,

    var gameInProgress: Boolean = true,

    var winnerId: String? = null

) : Serializable {

    /* ================= CURRENT PLAYER ================= */

    fun getCurrentPlayer(): Player? =
        players.getOrNull(currentPlayerIndex)

    fun nextPlayer() {
        if (players.isEmpty()) return
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    /* ================= ROUND CONTROL ================= */

    fun isRoundFinished(): Boolean {
        if (players.isEmpty()) return true
        return players.all { it.hand.isEmpty() }
    }

    fun startNewRound(cardsPerPlayer: Int = 5) {

        roundNumber++
        currentTrick.clear()
        deck.reset()

        players.forEach { it.resetForNewRound() }

        dealCards(cardsPerPlayer)

        currentPlayerIndex = 0
    }

    /* ================= PLAY CARD ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (!gameInProgress) return false
        if (player != getCurrentPlayer()) return false
        if (!player.hand.contains(card)) return false

        player.removeCard(card)

        currentTrick.add(player.id to card)

        if (currentTrick.size == players.size) {
            evaluateTrick()
        } else {
            nextPlayer()
        }

        return true
    }

    /* ================= TRICK EVALUATION ================= */

    private fun evaluateTrick() {

        val winningPair =
            currentTrick.maxByOrNull { it.second.strength() }

        winningPair?.let { pair ->

            val winnerPlayer =
                players.find { it.id == pair.first }

            winnerPlayer?.incrementTrick()

            // الفائز يبدأ الجولة التالية
            winnerPlayer?.let {
                currentPlayerIndex = players.indexOf(it)
            }
        }

        currentTrick.clear()

        if (isRoundFinished()) {
            finishRound()
        }
    }

    /* ================= ROUND FINISH ================= */

    private fun finishRound() {

        players.forEach {
            it.applyRoundScore()
        }

        checkGameWinner()
    }

    /* ================= DEAL CARDS ================= */

    fun dealCards(cardsPerPlayer: Int = 5) {

        players.forEach { player ->
            val drawn = deck.drawCards(cardsPerPlayer)
            player.addCards(drawn)
        }
    }

    /* ================= TEAM SCORES ================= */

    fun getTeamScores(): Map<Int, Int> {

        return players
            .groupBy { it.teamId }
            .mapValues { entry ->
                entry.value.sumOf { it.score }
            }
    }

    /* ================= GAME END ================= */

    fun checkGameWinner(maxScore: Int = 400) {

        players.find { it.score >= maxScore }?.let {
            winnerId = it.id
            gameInProgress = false
        }
    }

    fun resetGame() {

        players.forEach {
            it.score = 0
            it.resetForNewRound()
        }

        currentPlayerIndex = 0
        roundNumber = 1
        winnerId = null
        gameInProgress = true
        currentTrick.clear()
        deck.reset()
    }

    /* ================= NETWORK SAFE COPY ================= */

    /**
     * نسخة آمنة للشبكة (لا ترسل أوراق اللاعبين)
     */
    fun toNetworkSafeCopy(): GameState {

        val safePlayers =
            players.map { it.toNetworkSafeCopy() }
                .toMutableList()

        return copy(
            players = safePlayers,
            deck = Deck(mutableListOf()), // لا نرسل deck
        )
    }

    /**
     * تحديث الحالة من نسخة شبكة
     */
    fun updateFromNetwork(networkState: GameState) {

        currentPlayerIndex = networkState.currentPlayerIndex
        roundNumber = networkState.roundNumber
        gameInProgress = networkState.gameInProgress
        winnerId = networkState.winnerId

        // تحديث اللاعبين
        networkState.players.forEach { netPlayer ->
            players.find { it.id == netPlayer.id }
                ?.updateFromNetwork(netPlayer)
        }

        currentTrick.clear()
        currentTrick.addAll(networkState.currentTrick)
    }
}
