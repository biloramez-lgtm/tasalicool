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
    val onClientDisconnected: ((Player) -> Unit)? = null
) : Serializable {

    var onGameUpdated: (() -> Unit)? = null
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
        if (players.size < 4) return

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

        currentPlayerIndex = (dealerIndex + 1) % players.size
        phase = GamePhase.BIDDING

        onGameUpdated?.invoke()
        processAIBidding()
    }

    /* ================= BIDDING ================= */

    fun placeBid(player: Player, bid: Int): Boolean {

        if (isNetworkClient) return false
        if (phase != GamePhase.BIDDING) return false
        if (player != getCurrentPlayer()) return false

        val minBid = when {
            player.score < 30 -> 2
            player.score < 40 -> 3
            player.score < 50 -> 4
            else -> 5
        }

        if (bid < minBid || bid > 13) return false

        player.setBid(bid)
        nextPlayer()
        processAIBidding()

        if (players.all { it.hasPlacedBid() }) {

            val totalBids = players.sumOf { it.bid }

            val minTotal = when {
                players.any { it.score >= 50 } -> 14
                players.any { it.score >= 40 } -> 13
                players.any { it.score >= 30 } -> 12
                else -> 11
            }

            if (totalBids < minTotal) {
                startNewRound()
                return true
            }

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
                ai.score < 50 -> 4
                else -> 5
            }

            val bid = AdvancedAI.chooseBid(ai, this, minBid)
            ai.setBid(bid)
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

        if (trickNumber >= 13) {

            players.forEach { player ->
                val bidValue = getBidValue(player.bid, player.score)

                if (player.tricks >= player.bid) {
                    player.score += bidValue
                } else {
                    player.score -= bidValue
                }
            }

            checkGameWinner()

            if (winner == null) {
                startNewRound()
            } else {
                phase = GamePhase.GAME_OVER
            }
        }

        currentTrick.clear()
        onGameUpdated?.invoke()
    }

    /* ================= RULES ================= */

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasSameSuit = player.hand.any { it.suit == leadSuit }

        return if (hasSameSuit) card.suit == leadSuit else true
    }

    private fun determineTrickWinner(): Player {

        val leadSuit = currentTrick.first().second.suit

        val heartsPlays =
            currentTrick.filter { it.second.suit.name == "HEARTS" }

        val winningPlay = if (heartsPlays.isNotEmpty()) {
            heartsPlays.maxByOrNull { it.second.rank.ordinal }
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxByOrNull { it.second.rank.ordinal }
        }!!

        return winningPlay.first
    }

    private fun getBidValue(bid: Int, currentScore: Int): Int {

        val normalTable = mapOf(
            2 to 2, 3 to 3, 4 to 4,
            5 to 10, 6 to 12, 7 to 14,
            8 to 16, 9 to 27,
            10 to 40, 11 to 40, 12 to 40, 13 to 40
        )

        val after30Table = mapOf(
            2 to 2, 3 to 3, 4 to 4,
            5 to 5, 6 to 6, 7 to 14,
            8 to 16, 9 to 27,
            10 to 40, 11 to 40, 12 to 40, 13 to 40
        )

        return if (currentScore >= 30)
            after30Table[bid] ?: bid
        else
            normalTable[bid] ?: bid
    }

    private fun checkGameWinner() {

        players.forEach { player ->

            val partner = players.first {
                it.teamId == player.teamId && it != player
            }

            if (player.score >= 41 && partner.score > 0) {
                winner = player
            }
        }
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
