package com.example.tasalicool.models

import com.example.tasalicool.game.AdvancedAI
import com.example.tasalicool.game.GameMode
import com.google.gson.Gson
import java.io.Serializable
import java.util.UUID

class Game400Engine(
    var gameMode: GameMode = GameMode.SINGLE_PLAYER,
    humanCount: Int = 1,
    val players: MutableList<Player> = initializePlayers(gameMode, humanCount),
    val onClientConnected: ((Player) -> Unit)? = null,
    val onClientDisconnected: ((Player) -> Unit)? = null,
    val onGameUpdated: (() -> Unit)? = null
) : Serializable {

    var isNetworkClient = false

    private val deck = Deck()
    private val gson = Gson()

    var phase =
        if (gameMode == GameMode.WIFI_MULTIPLAYER)
            GamePhase.WAITING_FOR_PLAYERS
        else
            GamePhase.BIDDING

    var currentPlayerIndex = 0

    var dealerIndex = -1

    var trickNumber = 0

    val currentTrick = mutableListOf<Pair<Player, Card>>()

    var lastTrickWinner: Player? = null

    var winner: Player? = null

    /* ================= START ================= */

    fun startGame() {
        if (isNetworkClient) return
        if (gameMode != GameMode.WIFI_MULTIPLAYER) {
            startNewRound()
        }
    }

    fun startNewRound() {

        if (isNetworkClient) return

        deck.reset()
        AdvancedAI.resetMemory()

        dealerIndex = (dealerIndex + 1) % players.size

        players.forEach {
            it.resetForNewRound()
            it.addCards(deck.drawCards(13))
        }

        trickNumber = 0
        currentTrick.clear()
        lastTrickWinner = null
        winner = null

        currentPlayerIndex = (dealerIndex + 1) % players.size
        phase = GamePhase.BIDDING

        onGameUpdated?.invoke()
        processAIBidding()
    }

    /* ================= NETWORK SYNC ================= */

    fun applyNetworkState(stateJson: String) {

        val serverEngine =
            gson.fromJson(
                stateJson,
                Game400Engine::class.java
            )

        synchronized(this) {

            players.clear()
            players.addAll(serverEngine.players)

            currentTrick.clear()
            currentTrick.addAll(serverEngine.currentTrick)

            phase = serverEngine.phase
            trickNumber = serverEngine.trickNumber
            winner = serverEngine.winner
            currentPlayerIndex = serverEngine.currentPlayerIndex
            dealerIndex = serverEngine.dealerIndex
        }

        onGameUpdated?.invoke()
    }

    /* ================= BIDDING ================= */

    fun placeBid(player: Player, bid: Int): Boolean {

        if (isNetworkClient) return false
        if (phase != GamePhase.BIDDING) return false
        if (player != getCurrentPlayer()) return false

        val minBid = when {
            player.score < 30 -> 2
            player.score < 40 -> 3
            else -> 4
        }

        if (bid < minBid || bid > 13) return false

        player.bid = bid
        nextPlayer()

        processAIBidding()

        if (players.all { it.bid > 0 }) {
            phase = GamePhase.PLAYING
            currentPlayerIndex = (dealerIndex + 1) % players.size
        }

        onGameUpdated?.invoke()
        processAITurns()
        return true
    }

    private fun processAIBidding() {

        if (isNetworkClient) return

        while (
            phase == GamePhase.BIDDING &&
            getCurrentPlayer().type == PlayerType.AI
        ) {
            val ai = getCurrentPlayer()

            val minBid = when {
                ai.score < 30 -> 2
                ai.score < 40 -> 3
                else -> 4
            }

            val bid = AdvancedAI.chooseBid(ai, this, minBid)
            ai.bid = bid
            nextPlayer()
        }
    }

    /* ================= PLAY ================= */

    fun playCard(player: Player, card: Card): Boolean {

        if (isNetworkClient) return false
        if (phase != GamePhase.PLAYING) return false
        if (player != getCurrentPlayer()) return false
        if (!isValidPlay(player, card)) return false

        player.removeCard(card)
        currentTrick.add(player to card)

        if (player.type == PlayerType.AI) {
            AdvancedAI.rememberCard(player, card)
        }

        if (currentTrick.size == players.size) {
            finishTrick()
        } else {
            nextPlayer()
        }

        onGameUpdated?.invoke()
        processAITurns()
        return true
    }

    private fun processAITurns() {

        if (isNetworkClient) return

        while (isAITurn()) {
            val ai = getCurrentPlayer()
            val card = AdvancedAI.chooseCard(ai, this)
            playCard(ai, card)
        }
    }

    private fun finishTrick() {

        val trickWinner = determineTrickWinner()
        trickWinner.incrementTrick()

        lastTrickWinner = trickWinner
        trickNumber++

        currentPlayerIndex = players.indexOf(trickWinner)
    }

    fun clearTrickAfterDelay() {

        if (isNetworkClient) return
        if (currentTrick.isEmpty()) return

        currentTrick.clear()

        if (trickNumber >= 13) {
            finishRound()
        }

        onGameUpdated?.invoke()
        processAITurns()
    }

    /* ================= RULES ================= */

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasSameSuit = player.hand.any { it.suit == leadSuit }

        return if (hasSameSuit) {
            card.suit == leadSuit
        } else true
    }

    private fun determineTrickWinner(): Player {

        val leadSuit = currentTrick.first().second.suit

        val winningPlay =
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxByOrNull { it.second.rank.ordinal }!!

        return winningPlay.first
    }

    /* ================= SCORING ================= */

    private fun finishRound() {

        players.forEach { it.applyRoundScore() }

        val team1Score =
            players.filter { it.teamId == 1 }.sumOf { it.score }

        val team2Score =
            players.filter { it.teamId == 2 }.sumOf { it.score }

        if (team1Score >= 41 || team2Score >= 41) {

            val winningTeamId =
                if (team1Score >= 41) 1 else 2

            winner =
                players.first { it.teamId == winningTeamId }

            phase = GamePhase.GAME_OVER
            onGameUpdated?.invoke()
            return
        }

        phase = GamePhase.ROUND_END
        onGameUpdated?.invoke()
    }

    /* ================= HELPERS ================= */

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    fun isAITurn(): Boolean {
        return phase == GamePhase.PLAYING &&
                getCurrentPlayer().type == PlayerType.AI
    }

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    companion object {

        fun initializePlayers(
            mode: GameMode,
            humanCount: Int = 1
        ): MutableList<Player> {

            val players = mutableListOf<Player>()
            val totalPlayers = 4

            val humans = when (mode) {
                GameMode.SINGLE_PLAYER -> 1
                GameMode.LOCAL_MULTIPLAYER -> humanCount
                GameMode.WIFI_MULTIPLAYER -> humanCount
            }

            val aiCount = totalPlayers - humans
            var index = 0

            repeat(humans) {
                players.add(
                    Player(
                        id = UUID.randomUUID().toString(),
                        name = "Player ${it + 1}",
                        type = PlayerType.HUMAN,
                        teamId = if (index % 2 == 0) 1 else 2
                    )
                )
                index++
            }

            repeat(aiCount) {
                players.add(
                    Player(
                        id = UUID.randomUUID().toString(),
                        name = "AI ${it + 1}",
                        type = PlayerType.AI,
                        teamId = if (index % 2 == 0) 1 else 2
                    )
                )
                index++
            }

            return players
        }
    }
}
