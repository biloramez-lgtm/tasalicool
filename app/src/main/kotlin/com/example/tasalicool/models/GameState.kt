package com.example.tasalicool.models

import com.google.gson.Gson
import java.io.Serializable

data class GameState(

    val players: MutableList<Player> = mutableListOf(),

    var currentPlayerIndex: Int = 0,

    val deck: Deck = Deck(),

    // نخزن playerId بدل Player object
    val currentTrick: MutableList<Pair<String, Card>> = mutableListOf(),

    var roundNumber: Int = 1,

    var gameInProgress: Boolean = true,

    var winnerId: String? = null

) : Serializable {

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
    /* ================= ROUND CONTROL ===================== */
    /* ===================================================== */

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

    /* ===================================================== */
    /* ================= PLAY CARD (LOCAL) ================= */
    /* ===================================================== */

    /**
     * يستخدمه الجهاز المحلي
     * يرجع payload جاهز للإرسال للشبكة
     */
    fun playCardAndCreateNetworkPayload(
        playerId: String,
        card: Card
    ): Map<String, String>? {

        val player = players.find { it.id == playerId }
            ?: return null

        if (!gameInProgress) return null
        if (player != getCurrentPlayer()) return null
        if (!player.hand.contains(card)) return null

        player.removeCard(card)

        currentTrick.add(player.id to card)

        val payload = mapOf(
            "playerId" to player.id,
            "cardSuit" to card.suit.name,
            "cardRank" to card.rank.name
        )

        if (currentTrick.size == players.size) {
            evaluateTrick()
        } else {
            nextPlayer()
        }

        return payload
    }

    /* ===================================================== */
    /* ================= APPLY NETWORK MOVE ================= */
    /* ===================================================== */

    /**
     * يستخدمه جميع الأجهزة عند استلام CARD_PLAYED
     */
    fun applyNetworkCardPlayed(payload: Map<String, String>) {

        val playerId = payload["playerId"] ?: return
        val suitName = payload["cardSuit"] ?: return
        val rankName = payload["cardRank"] ?: return

        val player = players.find { it.id == playerId } ?: return

        val card = Card(
            suit = Suit.valueOf(suitName),
            rank = Rank.valueOf(rankName)
        )

        player.removeCard(card)

        currentTrick.add(player.id to card)

        if (currentTrick.size == players.size) {
            evaluateTrick()
        } else {
            nextPlayer()
        }
    }

    /* ===================================================== */
    /* ================= TRICK EVALUATION ================== */
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

    /* ===================================================== */
    /* ================= ROUND FINISH ====================== */
    /* ===================================================== */

    private fun finishRound() {

        players.forEach {
            it.applyRoundScore()
        }

        checkGameWinner()
    }

    /* ===================================================== */
    /* ================= DEAL CARDS ======================== */
    /* ===================================================== */

    fun dealCards(cardsPerPlayer: Int = 5) {

        players.forEach { player ->
            val drawn = deck.drawCards(cardsPerPlayer)
            player.addCards(drawn)
        }
    }

    /* ===================================================== */
    /* ================= TEAM SCORES ======================= */
    /* ===================================================== */

    fun getTeamScores(): Map<Int, Int> {

        return players
            .groupBy { it.teamId }
            .mapValues { entry ->
                entry.value.sumOf { it.score }
            }
    }

    /* ===================================================== */
    /* ================= GAME END ========================== */
    /* ===================================================== */

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

    /* ===================================================== */
    /* ================= NETWORK SAFE COPY ================= */
    /* ===================================================== */

    fun toNetworkSafeCopy(): GameState {

        val safePlayers =
            players.map { it.toNetworkSafeCopy() }
                .toMutableList()

        return copy(
            players = safePlayers,
            deck = Deck(mutableListOf())
        )
    }

    fun updateFromNetwork(networkState: GameState) {

        currentPlayerIndex = networkState.currentPlayerIndex
        roundNumber = networkState.roundNumber
        gameInProgress = networkState.gameInProgress
        winnerId = networkState.winnerId

        networkState.players.forEach { netPlayer ->
            players.find { it.id == netPlayer.id }
                ?.updateFromNetwork(netPlayer)
        }

        currentTrick.clear()
        currentTrick.addAll(networkState.currentTrick)
    }
}
