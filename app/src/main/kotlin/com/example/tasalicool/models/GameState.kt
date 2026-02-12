package com.example.tasalicool.models

import com.example.tasalicool.network.GameAction
import com.example.tasalicool.network.NetworkMessage
import com.google.gson.Gson
import java.io.Serializable

data class GameState(

    val players: MutableList<Player> = mutableListOf(),
    var currentPlayerIndex: Int = 0,
    val deck: Deck = Deck(),
    val currentTrick: MutableList<Pair<String, Card>> = mutableListOf(),
    var roundNumber: Int = 1,
    var gameInProgress: Boolean = false,
    var winnerId: String? = null

) : Serializable {

    @Transient
    private val gson = Gson()

    /* ===================================================== */
    /* ================= CURRENT PLAYER ==================== */
    /* ===================================================== */

    fun getCurrentPlayer(): Player? =
        players.getOrNull(currentPlayerIndex)

    fun nextPlayer() {
        if (players.isEmpty()) return
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    /* ===================================================== */
    /* ================= START GAME (HOST) ================= */
    /* ===================================================== */

    fun startGameAsHost(cardsPerPlayer: Int = 5): NetworkMessage {

        if (players.isEmpty()) {
            return createMessage(GameAction.MESSAGE, "No players")
        }

        roundNumber = 1
        currentPlayerIndex = 0
        winnerId = null
        gameInProgress = true

        deck.reset()
        deck.shuffle()

        players.forEach {
            it.score = 0
            it.resetForNewRound()
        }

        dealCards(cardsPerPlayer)

        return createStateUpdateMessage()
    }

    /* ===================================================== */
    /* ================= NEW ROUND (HOST) ================== */
    /* ===================================================== */

    fun startNewRoundAsHost(cardsPerPlayer: Int = 5): NetworkMessage {

        roundNumber++
        currentTrick.clear()

        deck.reset()
        deck.shuffle()

        players.forEach { it.resetForNewRound() }

        dealCards(cardsPerPlayer)

        currentPlayerIndex = 0

        return createStateUpdateMessage()
    }

    /* ===================================================== */
    /* ================= PLAY CARD (HOST) ================== */
    /* ===================================================== */

    fun playCardAsHost(
        playerId: String,
        card: Card
    ): NetworkMessage? {

        val player = players.find { it.id == playerId }
            ?: return null

        if (!gameInProgress) return null
        if (player != getCurrentPlayer()) return null
        if (!player.hand.contains(card)) return null

        player.removeCard(card)
        currentTrick.add(player.id to card)

        if (currentTrick.size == players.size) {
            evaluateTrick()
        } else {
            nextPlayer()
        }

        return createStateUpdateMessage()
    }

    /* ===================================================== */
    /* ================= APPLY NETWORK ===================== */
    /* ===================================================== */

    fun applyNetworkMessage(message: NetworkMessage) {

        when (message.action) {

            GameAction.UPDATE_GAME_STATE,
            GameAction.SYNC_STATE -> {

                message.payload?.let { json ->
                    applyFullNetworkState(json)
                }
            }

            else -> {}
        }
    }

    private fun applyFullNetworkState(json: String) {

        val networkState =
            gson.fromJson(json, GameState::class.java)

        currentPlayerIndex = networkState.currentPlayerIndex
        roundNumber = networkState.roundNumber
        gameInProgress = networkState.gameInProgress
        winnerId = networkState.winnerId

        currentTrick.clear()
        currentTrick.addAll(networkState.currentTrick)

        networkState.players.forEach { netPlayer ->

            val localPlayer =
                players.find { it.id == netPlayer.id }

            if (localPlayer != null) {
                localPlayer.updateFromNetwork(netPlayer)
            } else {
                players.add(netPlayer)
            }
        }
    }

    /* ===================================================== */
    /* ================= TRICK ============================= */
    /* ===================================================== */

    private fun evaluateTrick() {

        val winningPair =
            currentTrick.maxByOrNull { it.second.strength() }

        winningPair?.let { pair ->

            val winnerPlayer =
                players.find { it.id == pair.first }

            winnerPlayer?.incrementTrick()

            winnerPlayer?.let {
                currentPlayerIndex = players.indexOf(it)
            }
        }

        currentTrick.clear()

        if (isRoundFinished()) {
            finishRound()
        }
    }

    private fun finishRound() {

        players.forEach {
            it.applyRoundScore()
        }

        checkGameWinner()
    }

    /* ===================================================== */
    /* ================= DEAL CARDS ======================== */
    /* ===================================================== */

    private fun dealCards(cardsPerPlayer: Int = 5) {

        players.forEach { player ->
            val drawn = deck.drawCards(cardsPerPlayer)
            player.addCards(drawn)
        }
    }

    /* ===================================================== */
    /* ================= HELPERS =========================== */
    /* ===================================================== */

    fun isRoundFinished(): Boolean {
        if (players.isEmpty()) return true
        return players.all { it.hand.isEmpty() }
    }

    fun getTeamScores(): Map<Int, Int> {
        return players
            .groupBy { it.teamId }
            .mapValues { entry ->
                entry.value.sumOf { it.score }
            }
    }

    private fun checkGameWinner(maxScore: Int = 400) {

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
        gameInProgress = false
        currentTrick.clear()
        deck.reset()
    }

    /* ===================================================== */
    /* ================= NETWORK MESSAGES ================== */
    /* ===================================================== */

    private fun createStateUpdateMessage(): NetworkMessage {
        return NetworkMessage(
            playerId = "HOST",
            gameType = "TASALI",
            action = GameAction.UPDATE_GAME_STATE,
            payload = gson.toJson(toNetworkSafeCopy()),
            round = roundNumber
        )
    }

    private fun createMessage(
        action: GameAction,
        text: String
    ): NetworkMessage {
        return NetworkMessage(
            playerId = "HOST",
            gameType = "TASALI",
            action = action,
            payload = text,
            round = roundNumber
        )
    }

    fun toNetworkSafeCopy(): GameState {

        val safePlayers =
            players.map { it.toNetworkSafeCopy() }
                .toMutableList()

        return copy(
            players = safePlayers,
            deck = Deck(mutableListOf())
        )
    }
}
