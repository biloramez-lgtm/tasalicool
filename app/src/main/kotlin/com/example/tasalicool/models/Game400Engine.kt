package com.example.tasalicool.models

import com.example.tasalicool.game.AdvancedAI
import com.example.tasalicool.game.GameMode
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

    private val deck = Deck()

    var phase =
        if (gameMode == GameMode.WIFI_MULTIPLAYER)
            GamePhase.WAITING_FOR_PLAYERS
        else
            GamePhase.BIDDING
        private set

    var currentPlayerIndex = 0
        private set

    var dealerIndex = -1
        private set

    var trickNumber = 0
        private set

    val currentTrick = mutableListOf<Pair<Player, Card>>()

    var lastTrickWinner: Player? = null
        private set

    var winner: Player? = null
        private set

    init {
        if (gameMode != GameMode.WIFI_MULTIPLAYER) {
            startNewRound()
        }
    }

    /* ================= START FROM LOBBY ================= */

    fun startGameFromLobby() {
        if (phase != GamePhase.WAITING_FOR_PLAYERS) return
        dealerIndex = -1
        winner = null
        startNewRound()
        onGameUpdated?.invoke()
    }

    /* ================= ROUND ================= */

    fun startNewRound() {

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

        processAIBidding()

        onGameUpdated?.invoke()
    }

    /* ================= BIDDING ================= */

    fun placeBid(player: Player, bid: Int): Boolean {

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
            processAITurns()
        }

        onGameUpdated?.invoke()
        return true
    }

    private fun processAIBidding() {

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

            val bid = minBid + (0..2).random()
            placeBid(ai, bid.coerceAtMost(13))
        }
    }

    /* ================= PLAY ================= */

    fun playCard(player: Player, card: Card): Boolean {

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

        processAITurns()

        onGameUpdated?.invoke()
        return true
    }

    private fun processAITurns() {

        while (
            phase == GamePhase.PLAYING &&
            getCurrentPlayer().type == PlayerType.AI
        ) {

            val ai = getCurrentPlayer()
            val card = AdvancedAI.chooseCard(ai, this)
            playCard(ai, card)
        }
    }

    private fun finishTrick() {

        val trickWinner = determineTrickWinner()
        trickWinner.incrementTrick()

        lastTrickWinner = trickWinner
        currentPlayerIndex = players.indexOf(trickWinner)

        trickNumber++
    }

    fun clearTrickAfterDelay() {

        if (currentTrick.isEmpty()) return

        currentTrick.clear()

        if (trickNumber >= 13) {
            finishRound()
        }

        onGameUpdated?.invoke()
    }

    /* ================= TRICK LOGIC ================= */

    private fun determineTrickWinner(): Player {

        val leadSuit = currentTrick.first().second.suit

        val trumpCards =
            currentTrick.filter { it.second.suit == Suit.HEARTS }

        return if (trumpCards.isNotEmpty()) {
            trumpCards.maxBy { it.second.rank.value }.first
        } else {
            currentTrick
                .filter { it.second.suit == leadSuit }
                .maxBy { it.second.rank.value }
                .first
        }
    }

    private fun isValidPlay(player: Player, card: Card): Boolean {

        if (currentTrick.isEmpty()) return true

        val leadSuit = currentTrick.first().second.suit
        val hasSuit = player.hand.any { it.suit == leadSuit }

        return if (hasSuit) card.suit == leadSuit else true
    }

    /* ================= SCORING ================= */

    private fun finishRound() {

        players.forEach { player ->
            if (player.bid == 13 && player.tricksWon == 13) {
                winner = player
                phase = GamePhase.GAME_OVER
                onGameUpdated?.invoke()
                return
            }
        }

        players.forEach { it.applyRoundScore() }

        players.forEach { player ->
            val partner = getPartner(player)
            if (player.score >= 41 && partner.score > 0) {
                winner = player
                phase = GamePhase.GAME_OVER
                onGameUpdated?.invoke()
                return
            }
        }

        if (phase != GamePhase.GAME_OVER) {
            phase = GamePhase.ROUND_END
        }

        onGameUpdated?.invoke()
    }

    /* ================= WIFI SYNC ================= */

    fun forceSyncFromServer(server: Game400Engine) {

        this.phase = server.phase
        this.currentPlayerIndex = server.currentPlayerIndex
        this.dealerIndex = server.dealerIndex
        this.trickNumber = server.trickNumber

        this.currentTrick.clear()
        this.currentTrick.addAll(server.currentTrick)

        this.lastTrickWinner = server.lastTrickWinner
        this.winner = server.winner

        this.players.clear()
        server.players.forEach {
            this.players.add(it)
        }

        onGameUpdated?.invoke()
    }

    /* ================= HELPERS ================= */

    fun getCurrentPlayer(): Player =
        players[currentPlayerIndex]

    private fun nextPlayer() {
        currentPlayerIndex =
            (currentPlayerIndex + 1) % players.size
    }

    private fun getPartner(player: Player): Player {
        val index = players.indexOf(player)
        return players[(index + 2) % players.size]
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

            for (i in 1..humans) {
                players.add(
                    Player(
                        id = UUID.randomUUID().toString(),
                        name = "Player $i",
                        type = PlayerType.HUMAN,
                        teamId = if (index % 2 == 0) 1 else 2
                    )
                )
                index++
            }

            for (i in 1..aiCount) {
                players.add(
                    Player(
                        id = UUID.randomUUID().toString(),
                        name = "AI $i",
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
